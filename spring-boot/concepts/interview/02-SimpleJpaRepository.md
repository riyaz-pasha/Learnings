`SimpleJpaRepository` is Spring Data JPA's default implementation of the repository interfaces (`JpaRepository`, `CrudRepository`, `PagingAndSortingRepository`, etc.). When you declare an interface like `interface UserRepository extends JpaRepository<User, Long>`, you never write the implementation. Spring creates a proxy at startup, and the actual CRUD logic behind it lives in `SimpleJpaRepository`.

## How it fits together

1. At startup, Spring Data scans for repository interfaces.
2. For each one, `JpaRepositoryFactory` builds a **JDK dynamic proxy**.
3. The proxy's **target** (the object that actually does the work) is a `SimpleJpaRepository<User, Long>` instance, constructed with the entity's `JpaEntityInformation` and an `EntityManager`.
4. Calls to methods like `save()`, `findById()`, `deleteById()`, `findAll()` are routed to `SimpleJpaRepository`. Derived query methods (`findByEmail`) and `@Query` methods are handled by separate query-execution interceptors in the proxy chain instead.

## What it does internally

It's a thin wrapper over the JPA `EntityManager`:

- **`save(entity)`**: checks `entityInformation.isNew(entity)`. If new, calls `em.persist(entity)`; otherwise `em.merge(entity)`.
- **`findById(id)`**: `em.find(domainClass, id)` wrapped in an `Optional`.
- **`deleteById(id)`**: loads the entity, then `em.remove(...)`.
- **`findAll(Pageable)` / `findAll(Specification)`**: builds a Criteria API query.
- **`existsById`, `count`, `flush`, `saveAndFlush`, `deleteAllInBatch`**: similar direct delegation.

## Transactions

The class is annotated with `@Transactional(readOnly = true)` at the class level, and write methods (`save`, `delete`, etc.) override it with a plain `@Transactional`. That's why repository calls work without you opening a transaction, and why reads get the `readOnly` hint (which can let Hibernate skip dirty checking and flush).

## Why it matters in practice

- **Interview point:** `save()` on an entity with an assigned ID (not generated) causes `isNew` to return false, so you get a `merge` (extra SELECT first) instead of a `persist`. A common performance gotcha. Implementing `Persistable<ID>` fixes this.
- **Customizing:** to add behavior to *all* repositories, extend `SimpleJpaRepository` and register it with `@EnableJpaRepositories(repositoryBaseClass = MyBaseRepository.class)`.
- **Debugging:** if you see `SimpleJpaRepository.save` or `.findById` in a stack trace, that's the layer between your repository interface and Hibernate.

Want me to go deeper on any part, such as the proxy creation flow, the `persist` vs `merge` behavior, or custom base repositories?

---
---
---


# `SimpleJpaRepository`, concept by concept

I'll start from the problem it solves and build up one piece at a time. Each concept depends on the previous one, so read in order.

---

## Concept 1: The problem

Without Spring Data, every entity needs the same boring code:

```java
public class UserDao {
    private final EntityManager em;

    public User findById(Long id)   { return em.find(User.class, id); }
    public void save(User u)        { em.persist(u); }
    public void delete(User u)      { em.remove(u); }
    // ...and the same again for Order, Product, Payment...
}
```

This is identical for every entity, so Spring Data lets you write only this:

```java
public interface UserRepository extends JpaRepository<User, Long> { }
```

That is an interface, so it has no code inside it. **Something has to supply the real code.** That something is `SimpleJpaRepository`.

---

## Concept 2: `EntityManager`

`EntityManager` is part of plain JPA, not Spring. It is the object that talks to the database for you.

| Method | What it does |
|---|---|
| `em.find(User.class, 5L)` | SELECT by primary key |
| `em.persist(user)` | INSERT a **brand-new** object |
| `em.merge(user)` | "Here's an object, make the DB match it" (insert or update) |
| `em.remove(user)` | DELETE |

Everything `SimpleJpaRepository` does boils down to calling these four.

---

## Concept 3: What `SimpleJpaRepository` is

It is an ordinary Java class that implements `JpaRepository` by calling `EntityManager`. In simplified form:

```java
public class SimpleJpaRepository<T, ID> implements JpaRepository<T, ID> {

    private final EntityManager em;
    private final Class<T> domainClass;   // e.g. User.class

    public Optional<T> findById(ID id) {
        return Optional.ofNullable(em.find(domainClass, id));
    }

    public void deleteById(ID id) {
        findById(id).ifPresent(em::remove);
    }
    // ...
}
```

The `T` and `ID` generics mean one class works for every entity. For `UserRepository` it becomes `SimpleJpaRepository<User, Long>`.

**Key takeaway:** `SimpleJpaRepository` is the *real code*. Your interface is only a *contract*.

---

## Concept 4: Connecting your interface to that class

You call `userRepository.findById(5L)`, but `UserRepository` is an interface, so who runs the code? Spring uses a **proxy**.

A proxy is a runtime-generated object that pretends to be your interface. Every method call lands on it first, and it decides where to forward the call.

```
Your code
   │  userRepository.findById(5L)
   ▼
┌─────────────────────────────┐
│  PROXY (looks like          │
│  UserRepository)            │
│                             │
│  "Is this a CRUD method?"   │
└──────┬───────────────┬──────┘
       │ yes           │ no (e.g. findByEmail)
       ▼               ▼
SimpleJpaRepository   Query machinery
(the target)          (parses method name / @Query)
       │
       ▼
  EntityManager → Database
```

At startup, Spring does this for each repository interface:

1. Finds `UserRepository`.
2. Creates a `SimpleJpaRepository<User, Long>` (the **target**), giving it an `EntityManager`.
3. Wraps it in a proxy that implements `UserRepository`.
4. Registers the proxy as a bean, so `@Autowired UserRepository` gives you that proxy.

---

## Concept 5: Who handles which method?

| Method kind | Example | Handled by |
|---|---|---|
| Built-in CRUD | `save`, `findById`, `findAll`, `delete`, `count` | `SimpleJpaRepository` |
| Derived query | `findByEmail(String e)` | Query machinery (method name parsed into a query) |
| `@Query` | `@Query("select u from User u where ...")` | Query machinery |

`SimpleJpaRepository` only owns the built-in methods that came from the parent interfaces.

---

## Concept 6: Transactions

A database write must happen inside a transaction. You never open one when calling `save()`, because `SimpleJpaRepository` is annotated for it:

```java
@Repository
@Transactional(readOnly = true)          // default for the whole class: reads
public class SimpleJpaRepository<T, ID> {

    @Transactional                       // overrides: writes get a real transaction
    public <S extends T> S save(S entity) { ... }

    @Transactional
    public void deleteById(ID id) { ... }

    public Optional<T> findById(ID id) { ... }   // inherits readOnly = true
}
```

- Reads run with `readOnly = true`, which lets Hibernate skip some work, such as dirty checking.
- Writes get a full transaction.
- If your service method is already `@Transactional`, these calls **join** your transaction instead of starting a new one.

---

## Concept 7: `save()`, the one method that really matters

`save()` doesn't know whether you are inserting or updating, so it decides:

```java
public <S extends T> S save(S entity) {
    if (entityInformation.isNew(entity)) {
        em.persist(entity);      // INSERT
        return entity;
    } else {
        return em.merge(entity); // UPDATE (or insert if missing)
    }
}
```

**How does it decide "new"?** The default rule is whether the `@Id` is `null`.

```java
// Case A: generated ID (the normal case)
@Id @GeneratedValue
private Long id;

new User();            // id == null → isNew = true  → persist → 1 INSERT ✅
```

```java
// Case B: you assign the ID yourself
@Id
private String code;

user.setCode("U-100"); // id != null → isNew = false → merge
userRepository.save(user);
```

In case B, `merge` doesn't trust that the row is missing. It first runs a **SELECT** to check, then does the INSERT. That is **2 queries instead of 1**, and it is the classic interview gotcha.

**The fix** is to tell Spring when the entity is new by implementing `Persistable`:

```java
@Entity
public class User implements Persistable<String> {
    @Id private String code;

    @Transient private boolean isNew = true;

    @Override public String getId()  { return code; }
    @Override public boolean isNew() { return isNew; }

    @PostLoad @PostPersist
    void markNotNew() { this.isNew = false; }
}
```

Now `save()` calls `persist` directly and skips the extra SELECT.

---

## Concept 8: Customizing it

To add behavior to **all** repositories, such as soft delete, you extend the class and register it:

```java
public class MyBaseRepository<T, ID> extends SimpleJpaRepository<T, ID> {
    public MyBaseRepository(JpaEntityInformation<T, ?> info, EntityManager em) {
        super(info, em);
    }
    // override or add methods
}

@EnableJpaRepositories(repositoryBaseClass = MyBaseRepository.class)
```

Spring then uses your class as the target instead of the default `SimpleJpaRepository`.

---

## The whole picture in 5 lines

1. Your repository is an **interface** with no code.
2. Spring creates a **proxy** that implements it.
3. The proxy forwards built-in CRUD calls to **`SimpleJpaRepository`** and query methods to the query machinery.
4. `SimpleJpaRepository` calls **`EntityManager`** (`find`, `persist`, `merge`, `remove`) inside **`@Transactional`**.
5. `save()` picks `persist` vs `merge` using `isNew()`, and assigned IDs trigger an extra SELECT.

## How to say it in an interview

> "`SimpleJpaRepository` is Spring Data JPA's default implementation of `JpaRepository`. Spring generates a proxy for my repository interface, and CRUD calls are delegated to a `SimpleJpaRepository` instance that wraps the `EntityManager`. It's `@Transactional(readOnly = true)` by default with write methods overridden. `save()` uses `isNew()`: persist if new, merge otherwise, which is why assigned IDs cause an extra SELECT unless the entity implements `Persistable`."

---

Want a quick check on whether this stuck? I can give you 5 interview-style questions on it, or go deeper on `persist` vs `merge` (the persistence context), which is the next concept that makes this click fully.

---
---
---


# Who implements derived queries?

Not `SimpleJpaRepository`. A separate set of classes handles them, and they are wired into the same proxy from before.

---

## Concept 1: Recall the proxy

Your repository proxy has a **chain of interceptors** in front of the `SimpleJpaRepository` target. One interceptor in that chain, `QueryExecutorMethodInterceptor`, is the router.

```
userRepository.findByEmail("a@b.com")
        │
        ▼
┌──────────────────────────────────────────┐
│ PROXY                                    │
│  QueryExecutorMethodInterceptor          │
│   "Is findByEmail a query method?"       │
│        │ yes                  │ no       │
│        ▼                      ▼          │
│   RepositoryQuery       SimpleJpaRepository
│   (prebuilt at startup)  (save, findById...)
└──────────────────────────────────────────┘
```

---

## Concept 2: The work happens at startup, not per call

When the app boots, for every method in your interface that is **not** a built-in CRUD method, Spring builds a `RepositoryQuery` object and stores it in a map:

```
Map<Method, RepositoryQuery>
  findByEmail          → PartTreeJpaQuery
  findByStatusAndAge   → PartTreeJpaQuery
  searchActive (@Query)→ SimpleJpaQuery
```

At call time the interceptor just looks up your method in this map and runs the stored query. Nothing is parsed per call.

---

## Concept 3: Who decides which kind of query to build?

A `QueryLookupStrategy` makes the decision. The default is `CREATE_IF_NOT_FOUND`, which checks in this order:

| Order | Check | Query object created |
|---|---|---|
| 1 | Does the method have `@Query`? | `SimpleJpaQuery` (or native variant) |
| 2 | Is there a named query (`User.findByEmail`) in your entity or XML? | `NamedQuery` |
| 3 | Otherwise, derive it from the method name | `PartTreeJpaQuery` |

So **derived queries (rule 3) are implemented by `PartTreeJpaQuery`**.

---

## Concept 4: How `PartTreeJpaQuery` turns a name into a query

Take this method:

```java
List<User> findByEmailAndStatus(String email, Status status);
```

**Step 1: parse the name with `PartTree`.**

```
findBy  Email  And  Status
  │       │     │     │
subject  part  op   part
```

- `find...By` means this is a SELECT.
- `Email` and `Status` must match **property names** on `User`.
- `And` combines them.

Other keywords it understands: `Or`, `OrderBy`, `GreaterThan`, `Like`, `In`, `IsNull`, `First`, `Top3`, `Distinct`, `Count...By`, `Exists...By`, `Delete...By`.

**Step 2: build a Criteria API query** (via `JpaQueryCreator`). Conceptually:

```java
CriteriaBuilder cb = em.getCriteriaBuilder();
CriteriaQuery<User> q = cb.createQuery(User.class);
Root<User> root = q.from(User.class);
q.where(cb.and(
    cb.equal(root.get("email"),  emailParam),
    cb.equal(root.get("status"), statusParam)
));
```

**Step 3: execute at call time.** When you call the method, the arguments are bound to the parameters, the query runs through the `EntityManager`, and the result is shaped according to your return type:

| Return type | Behavior |
|---|---|
| `List<User>` | collects all rows |
| `User` / `Optional<User>` | expects at most one row (throws if more) |
| `Page<User>` | runs the query plus a count query |
| `long` (`countBy...`) | count query |
| `boolean` (`existsBy...`) | exists check |

---

## Concept 5: A consequence worth knowing, fail-fast startup

Because parsing happens at boot, a wrong name breaks the app **immediately**, not at runtime:

```java
List<User> findByEmial(String e);   // typo
```

```
PropertyReferenceException: No property 'emial' found for type 'User'
```

The application context fails to start. That is a useful safety net and a common interview talking point.

---

## Summary

| Method type | Implemented by | Created when |
|---|---|---|
| `save`, `findById`, `findAll`, `delete`... | `SimpleJpaRepository` | startup (as proxy target) |
| `@Query` methods | `SimpleJpaQuery` | startup |
| Named queries | `NamedQuery` | startup |
| Derived (`findByEmail...`) | **`PartTreeJpaQuery`** | startup |
| Routing between all of these | `QueryExecutorMethodInterceptor` (in the proxy) | per call |

**Interview answer:**

> "Derived queries aren't handled by `SimpleJpaRepository`. At startup, Spring Data's `QueryLookupStrategy` creates a `RepositoryQuery` for each non-CRUD method. For derived queries that's `PartTreeJpaQuery`, which parses the method name with `PartTree`, builds a Criteria query, and caches it. At runtime, `QueryExecutorMethodInterceptor` in the proxy routes the call to it. A bad property name fails the app at startup."

Want me to quiz you on the whole repository stack so far, or go into how `Page<User>` and `Pageable` generate the extra count query?

---
---
---

# Named query vs derived query

Both answer "how does Spring get a query for this method?", but **who writes the query** differs.

| | Named query | Derived query |
|---|---|---|
| **Who writes the query?** | **You**, in JPQL or SQL | **Spring**, generated from the method name |
| **Where does the query text live?** | On the entity (`@NamedQuery`) or in XML/properties | Nowhere. It's built from the name |
| **Implemented by** | `NamedQuery` | `PartTreeJpaQuery` |
| **Linked to the method by** | A **matching name string** | **Parsing** the method name |

---

## Concept 1: Derived query (Spring writes it)

You write only the method name, and Spring figures out the query:

```java
public interface UserRepository extends JpaRepository<User, Long> {
    List<User> findByEmailAndStatus(String email, Status status);
}
```

Spring parses `findBy` + `Email` + `And` + `Status` into a Criteria query. **You never see or write the query text.**

The limit is that the method name is your only tool. Complex logic gives you names like `findByStatusAndCreatedAtBetweenAndAgeGreaterThanOrderByNameAsc`, which is unreadable and still can't express joins or functions well.

---

## Concept 2: Named query (you write it, and give it a name)

You write the query yourself, **give it a name**, and store it somewhere other than the repository:

```java
@Entity
@NamedQuery(
    name  = "User.findByEmail",
    query = "select u from User u where lower(u.email) = lower(?1)"
)
public class User { ... }
```

Then, in the repository, you declare a method **with no query on it**:

```java
User findByEmail(String email);
```

The link between the two is purely the **naming convention**:

```
Entity simple name  +  "."  +  method name
      "User"        +  "."  +  "findByEmail"   =  "User.findByEmail"
```

When Spring prepares `findByEmail`, it asks: *"Is there a named query called `User.findByEmail`?"* If yes, it uses that. The method name is now just a lookup key, and it is **not parsed** for `By`, `And`, etc.

Named queries can also live in `META-INF/jpa-named-queries.properties` or `orm.xml`, and native SQL versions exist (`@NamedNativeQuery`).

---

## Concept 3: Why the order matters

Spring checks in this order:

```
@Query on method?   → use it (SimpleJpaQuery)
        │ no
        ▼
Named query "User.findByEmail" exists?   → use it (NamedQuery)
        │ no
        ▼
Parse method name   → PartTreeJpaQuery
```

**A named query wins over derivation.** This produces a gotcha: if someone adds a `@NamedQuery` called `User.findByEmail` on the entity, your `findByEmail` method silently **stops being derived** and starts running that JPQL, even though the method name looks self-explanatory.

---

## Concept 4: Side-by-side on one problem

"Find users by email, case-insensitive."

**Derived** (Spring writes it):
```java
User findByEmailIgnoreCase(String email);
```

**Named** (you write it):
```java
// on entity
@NamedQuery(name = "User.findByEmail",
            query = "select u from User u where lower(u.email) = lower(?1)")

// in repository
User findByEmail(String email);
```

**`@Query`** (for comparison, you write it directly on the method):
```java
@Query("select u from User u where lower(u.email) = lower(:email)")
User findByEmail(@Param("email") String email);
```

---

## When to use which

| Situation | Choice |
|---|---|
| Simple filters, 1 to 3 conditions | **Derived** |
| Joins, aggregates, complex logic | `@Query` |
| Query shared by multiple callers or defined centrally with the entity | **Named query** |
| Everyday modern Spring code | Derived + `@Query`. Named queries are **rare** today |

Named queries are mostly legacy or JPA-standard style. They cost you the readability of keeping the query next to the method, which is why `@Query` took over.

---

## One more difference: validation timing

- **Named queries** are validated by the **JPA provider (Hibernate)** at startup, as it builds the entity metadata.
- **Derived queries** are validated by **Spring Data** at startup, when `PartTree` checks property names.

Both fail fast, but the error comes from a different layer.

---

## Interview answer

> "A derived query is generated by Spring Data from the method name via `PartTree`, so I never write the query. A named query is one I define myself, in JPQL or SQL, under a name like `User.findByEmail` using `@NamedQuery` or XML. Spring looks it up by the entity name plus the method name. The lookup order is `@Query`, then named query, then derivation, so a matching named query takes priority over parsing the method name."

Want me to move on to how `Pageable` creates the extra count query, or quiz you on the full stack (proxy → interceptor → `SimpleJpaRepository` / query types)?

---
---
---



