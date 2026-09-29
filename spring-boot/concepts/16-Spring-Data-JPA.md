# Topic 16 — Spring Data JPA: Repository Architecture and How It Actually Works

Now we move into one of the most important Spring Boot areas for interviews:

> **How does `JpaRepository` actually work behind the scenes?**

A lot of developers use:

```java
public interface UserRepository extends JpaRepository<User, Long> {
}
```

every day but cannot explain what happens after that.

For an EPAM-level interview, you should be able to explain:

```text
JpaRepository
   ↓
Spring Data repository proxy
   ↓
generated/selected repository implementation
   ↓
EntityManager
   ↓
JPA provider (commonly Hibernate)
   ↓
SQL
   ↓
Database
```

Spring Data JPA's goal is to reduce the boilerplate required for JPA data access through a repository abstraction. As of the current Spring Data JPA 4.1.1 documentation, `JpaRepository` builds on the Spring Data repository interfaces and JPA-specific functionality. ([Home][1])

---

# 1. What problem does Spring Data JPA solve?

Without Spring Data JPA, imagine implementing:

```java
public User findById(Long id) {
    EntityManager em = ...;

    return em.find(User.class, id);
}
```

Then:

```java
public void save(User user) {
    entityManager.persist(user);
}
```

Then:

```java
public void delete(User user) {
    entityManager.remove(user);
}
```

Then queries:

```java
TypedQuery<User> query =
    entityManager.createQuery(
        "select u from User u where u.email = :email",
        User.class
    );

query.setParameter("email", email);

return query.getResultList();
```

A large application would contain lots of repetitive data-access code.

Spring Data gives you:

```java
public interface UserRepository
        extends JpaRepository<User, Long> {
}
```

and you immediately get repository operations.

The repository abstraction exists specifically to reduce this data-access boilerplate. ([Home][2])

---

# 2. What is a Repository?

At the root of Spring Data is:

```java
Repository<T, ID>
```

For example:

```java
Repository<User, Long>
```

means:

```text
T  = User
ID = Long
```

Conceptually:

```text
Repository<T, ID>
        |
        +---- CrudRepository<T, ID>
                    |
                    +---- ListCrudRepository
                    |
                    +---- ...
                             
                 JpaRepository<T, ID>
```

Current Spring Data JPA also has separate list-returning and paging/sorting repository interfaces. `JpaRepository` combines these capabilities and also extends `QueryByExampleExecutor`. ([Home][3])

---

# 3. Repository hierarchy

The hierarchy is important enough to memorize.

Conceptually:

```text
Repository<T, ID>
        |
        +----------------------+
        |                      |
        v                      v
CrudRepository       PagingAndSortingRepository
        |                      |
        v                      v
ListCrudRepository    ListPagingAndSortingRepository
        \                      /
         \                    /
          +------ JpaRepository
```

And `JpaRepository` additionally gives JPA-specific operations. ([Home][4])

One subtle version-related point:

Since Spring Data 3.0, `PagingAndSortingRepository` is no longer itself a child of `CrudRepository`; if you want both capabilities in a plain repository definition, both interfaces need to be combined. `JpaRepository` already combines the relevant interfaces for you. ([Home][3])

---

# 4. `CrudRepository`

At its simplest:

```java
public interface UserRepository
        extends CrudRepository<User, Long> {
}
```

You get operations such as:

```java
save(...)
findById(...)
findAll(...)
count()
delete(...)
existsById(...)
```

The exact return types of collection methods are one reason `ListCrudRepository` was introduced: it returns `List` instead of `Iterable` for applicable multi-result methods. ([Home][5])

---

# 5. `JpaRepository`

Most Spring Boot applications use:

```java
public interface UserRepository
        extends JpaRepository<User, Long> {
}
```

Why?

Because `JpaRepository` provides:

```text
CRUD
+
paging/sorting
+
JPA-specific functionality
+
query-by-example functionality
```

Its current API includes inherited CRUD methods as well as JPA-specific methods such as:

```java
flush()
saveAndFlush(...)
deleteAllInBatch(...)
deleteAllByIdInBatch(...)
```

among others. ([Home][4])

---

# 6. The most important question

### Where is the implementation?

Look at this:

```java
public interface UserRepository
        extends JpaRepository<User, Long> {
}
```

There is no:

```java
UserRepositoryImpl
```

that you wrote.

So who implements it?

Spring Data creates/configures a repository implementation for you.

The standard implementation for JPA is:

```java
SimpleJpaRepository
```

The current API identifies `SimpleJpaRepository` as the default implementation of the repository functionality. ([Home][6])

This is one of the most important things to know.

---

# 7. What is `SimpleJpaRepository`?

Think of:

```java
SimpleJpaRepository<User, Long>
```

as the actual class providing the standard JPA repository behavior.

Very roughly:

```text
Your code

UserRepository repository
        |
        v
Spring-generated proxy
        |
        v
SimpleJpaRepository
        |
        v
EntityManager
        |
        v
JPA provider
        |
        v
Database
```

The proxy layer is important because the object injected into your service is normally not simply an object you instantiated with:

```java
new SimpleJpaRepository<>(...)
```

Spring Data infrastructure creates repository beans/proxies around the repository interface and routes method calls to the appropriate implementation/query mechanism. Spring Data documentation describes repository interfaces being backed by generated proxy instances. ([Home][7])

---

# 8. What happens when Spring starts?

Suppose:

```java
@Repository
public interface UserRepository
        extends JpaRepository<User, Long> {
}
```

Spring Boot sees the repository configuration.

Spring Data JPA identifies repository interfaces and creates repository beans for them. Repository configuration is backed by the `EntityManagerFactory` and transaction infrastructure. ([Home][8])

Conceptually:

```text
Application startup
        ↓
Repository scanning
        ↓
Find UserRepository
        ↓
Determine:
    Domain = User
    ID     = Long
        ↓
Create repository infrastructure
        ↓
Create repository bean/proxy
        ↓
Inject into UserService
```

So when you later write:

```java
@Service
public class UserService {

    private final UserRepository repository;

    public UserService(UserRepository repository) {
        this.repository = repository;
    }
}
```

Spring is injecting the repository bean it created.

---

# 9. Repository interfaces are declarations

This:

```java
public interface UserRepository
        extends JpaRepository<User, Long> {
}
```

is largely a **declaration of what repository behavior you want**.

You're telling Spring Data:

```text
I need a repository for User entities
whose ID is Long.
```

You don't need to manually implement all standard CRUD operations.

That's the central value of Spring Data.

---

# 10. What does `save()` actually do?

This is a very important interview question.

You write:

```java
User saved = repository.save(user);
```

Spring Data JPA eventually delegates persistence to the underlying JPA `EntityManager`.

The current Spring Data JPA documentation states that `save(...)` uses:

```java
entityManager.persist(...)
```

for a new entity and:

```java
entityManager.merge(...)
```

for an existing entity. ([Home][9])

Conceptually:

```text
repository.save(entity)
        |
        v
determine whether entity is new
        |
        +---- new ------> EntityManager.persist()
        |
        +---- existing -> EntityManager.merge()
```

This distinction is extremely important.

---

# 11. `persist()` vs `merge()`

Suppose:

```java
User user = new User();
user.setName("John");
```

and it is determined to be a new entity.

Then:

```java
entityManager.persist(user);
```

means:

> Make this entity managed/persistent in the current persistence context.

Now consider an existing/detached entity.

Spring Data may use:

```java
entityManager.merge(user);
```

`merge()` has different semantics from `persist()`.

This is why the statement:

> "`save()` always calls `persist()`"

is incorrect.

And:

> "`save()` always calls `merge()`"

is also incorrect.

The current entity-state detection strategy determines which operation is used. ([Home][9])

---

# 12. How does Spring know whether an entity is new?

This is a slightly deeper Spring Data JPA topic.

The default strategy first considers a non-primitive `@Version` property when available.

Without such a version property, it inspects the identifier.

For example:

```java
@Id
@GeneratedValue
private Long id;
```

A `null` identifier generally indicates a new entity under the default strategy. ([Home][9])

Conceptually:

```text
Is there a suitable Version property?
       |
       +-- yes --> version == null ?
       |
       +-- no --> id == null ?
```

There are additional strategies, including implementing:

```java
Persistable
```

when the default detection isn't appropriate, especially for manually assigned IDs. ([Home][9])

---

# 13. Why is `Persistable` useful?

Consider an entity with an ID assigned by your application:

```java
User user = new User();
user.setId(UUID.randomUUID());
```

The ID is already non-null.

But the entity could still be new.

The default identifier-based detection can therefore be unsuitable.

You can implement:

```java
Persistable<ID>
```

and explicitly tell Spring Data:

```java
@Override
public boolean isNew() {
    return isNew;
}
```

Spring Data uses that information when deciding between `persist()` and `merge()`. ([Home][9])

This is not something you need in every application, but it is good interview depth.

---

# 14. `findById()`

Now:

```java
repository.findById(10L);
```

returns:

```java
Optional<User>
```

rather than:

```java
User
```

So:

```java
Optional<User> user =
        repository.findById(id);
```

You can write:

```java
User user = repository.findById(id)
        .orElseThrow(() ->
                new UserNotFoundException(id));
```

This avoids the old pattern:

```java
if (user == null) {
    ...
}
```

The repository abstraction defines `findById` as a reserved CRUD method targeting the entity identifier. ([Home][10])

---

# 15. `findAll()`

With:

```java
repository.findAll();
```

Spring Data executes the corresponding data-access operation.

Conceptually:

```text
findAll()
   ↓
repository implementation/query mechanism
   ↓
JPA query
   ↓
EntityManager / provider
   ↓
SQL
   ↓
database
```

The important thing is:

> You didn't write the SQL.

Spring Data generated or supplied the necessary repository behavior for you.

---

# 16. Derived query methods

This is one of the most powerful features.

Suppose:

```java
public interface UserRepository
        extends JpaRepository<User, Long> {

    List<User> findByEmail(String email);
}
```

You didn't write:

```sql
SELECT *
FROM users
WHERE email = ?
```

Instead, Spring Data derives the query from the method name.

The documented mechanism supports subject keywords such as:

```text
find...By
read...By
get...By
query...By
search...By
exists...By
count...By
delete...By
```

and predicates such as:

```text
And
Or
Between
LessThan
GreaterThan
In
Containing
StartingWith
EndingWith
IsNull
NotNull
```

among others. ([Home][10])

---

# 17. More examples

```java
List<User> findByName(String name);
```

Conceptually:

```sql
WHERE name = ?
```

---

```java
List<User> findByNameAndAge(
        String name,
        int age
);
```

Conceptually:

```sql
WHERE name = ?
AND age = ?
```

---

```java
List<User> findByAgeGreaterThan(int age);
```

Conceptually:

```sql
WHERE age > ?
```

---

```java
List<User> findByAgeBetween(
        int min,
        int max
);
```

Conceptually:

```sql
WHERE age BETWEEN ? AND ?
```

---

```java
List<User> findByNameContaining(String text);
```

Conceptually:

```sql
WHERE name LIKE '%text%'
```

The exact generated SQL depends on the JPA provider and database.

The important thing is the method-name parsing.

---

# 18. How does Spring parse that method name?

Take:

```java
findByFirstNameAndAgeGreaterThan(
        String firstName,
        int age
)
```

Spring Data roughly interprets:

```text
find
 ↓
By
 ↓
FirstName
 ↓
And
 ↓
Age
 ↓
GreaterThan
```

giving a logical query structure like:

```text
firstName = ?
AND age > ?
```

This is called:

> **Query derivation from method names.**

Spring Data documents the query method keyword system centrally in Spring Data Commons. ([Home][10])

---

# 19. When derived queries become ugly

Imagine this:

```java
findByFirstNameAndLastNameAndStatusAndDepartmentAndCreatedDateBetweenOrderByCreatedDateDesc(...)
```

Technically possible.

But now the method name is becoming unreadable.

At some point, use:

```java
@Query
```

or:

```java
Specification
```

or a custom repository implementation, depending on the problem.

For example:

```java
@Query("""
    select u
    from User u
    where u.email = :email
    and u.status = :status
""")
List<User> findUsers(
        @Param("email") String email,
        @Param("status") Status status
);
```

The Spring Data JPA module supports declared queries in addition to derived queries. ([Home][7])

---

# 20. Derived Query vs `@Query`

Think:

### Simple query

```java
findByEmail(...)
```

Use query derivation.

### Complex but fixed query

```java
@Query("""
    select ...
    from ...
    where ...
""")
```

Use `@Query`.

### Highly dynamic filtering

```text
filter A optional
filter B optional
filter C optional
date range optional
status optional
...
```

Often use:

```java
Specification
```

or another dynamic-query mechanism.

This connects directly to the invoice `Specification` work you've done before.

---

# 21. `JpaSpecificationExecutor`

A repository can be:

```java
public interface UserRepository
        extends JpaRepository<User, Long>,
                JpaSpecificationExecutor<User> {
}
```

Then:

```java
repository.findAll(specification);
```

This lets you dynamically compose predicates.

For example:

```java
Specification<User> spec =
        Specification.where(hasStatus(status))
                     .and(hasDepartment(department))
                     .and(createdBetween(start, end));
```

Then:

```java
repository.findAll(spec);
```

This is precisely why `JpaSpecificationExecutor` is useful for APIs with many optional filters.

The current Spring Data JPA API explicitly provides `JpaSpecificationExecutor` alongside `JpaRepository`. ([Home][11])

---

# 22. This explains your earlier repository question

You previously worked with something like:

```java
invoiceRepository.findAll(
    InvoiceSpecifications.withFilters(filter, jobNumbers)
);
```

That works because your repository can extend:

```java
JpaSpecificationExecutor<Invoice>
```

For example:

```java
public interface InvoiceRepository
        extends JpaRepository<Invoice, Long>,
                JpaSpecificationExecutor<Invoice> {
}
```

So you do **not** necessarily need a custom repository just because the query is dynamic.

That's an important distinction:

```text
Standard CRUD
    ↓
JpaRepository

Dynamic predicates
    ↓
JpaSpecificationExecutor

Custom query
    ↓
@Query

Highly specialized repository behavior
    ↓
Custom repository implementation
```

This is exactly the kind of architectural decision an interviewer may ask you to make.

---

# 23. What happens when you call a repository method?

Let's trace:

```java
repository.findByEmail("john@example.com");
```

Conceptually:

```text
Your service
    |
    | findByEmail(...)
    v
Repository proxy
    |
    | inspect repository method
    v
Query creation/execution infrastructure
    |
    v
JPA provider / EntityManager
    |
    v
SQL
    |
    v
Database
```

For:

```java
findByEmail(...)
```

Spring Data can derive the query from the method name.

For:

```java
@Query(...)
```

it uses the declared query.

For:

```java
findAll(specification)
```

it builds the query from the specification.

This is the key mental model.

---

# 24. Important: Spring Data is not JPA

Interviewers often test this.

These are different layers:

```text
Spring Boot
      ↓
Spring Data JPA
      ↓
JPA / Jakarta Persistence API
      ↓
Hibernate
      ↓
JDBC
      ↓
Database
```

More accurately, Hibernate is a JPA implementation/provider.

### JPA

Defines APIs/contracts such as:

```java
EntityManager
@Entity
@Id
@OneToMany
```

### Hibernate

Implements the JPA specification and provides additional features.

### Spring Data JPA

Builds a repository abstraction on top of JPA.

### Spring Boot

Provides auto-configuration and application setup around these technologies.

This distinction is extremely important.

---

# 25. `JpaRepository` does not directly execute SQL itself

Don't say:

> "`JpaRepository` executes SQL."

A better explanation:

> "`JpaRepository` is a Spring Data repository abstraction. Its implementation delegates persistence operations to JPA infrastructure, which is then handled by the JPA provider such as Hibernate and ultimately JDBC/database access."

The current `SimpleJpaRepository` implementation is built around JPA's `EntityManager`. ([Home][6])

---

# 26. `EntityManager`

Now we reach JPA itself.

The central API is:

```java
EntityManager
```

It provides operations such as:

```java
persist()
find()
merge()
remove()
flush()
```

Conceptually:

```text
JpaRepository
       ↓
SimpleJpaRepository
       ↓
EntityManager
       ↓
Hibernate
       ↓
JDBC
       ↓
Database
```

This is one of the most important diagrams in JPA.

---

# 27. Persistence Context

Now we reach the core JPA concept.

The **persistence context** is essentially the context in which JPA manages entity instances.

Think of it as:

```text
Persistence Context
-------------------------
User#1 → Java object
User#2 → Java object
Order#10 → Java object
...
```

For the current persistence context, JPA tracks managed entities and their state.

This enables important features such as:

> **Dirty checking**

---

# 28. Dirty checking

Suppose:

```java
@Transactional
public void updateUser(Long id) {

    User user = repository.findById(id)
            .orElseThrow();

    user.setName("John");
}
```

Notice something fascinating.

We did **not** write:

```java
repository.save(user);
```

after:

```java
user.setName("John");
```

Yet, under the appropriate transactional managed-entity conditions, JPA can detect the change and synchronize it to the database.

Conceptually:

```text
find()
  ↓
managed entity
  ↓
user.setName(...)
  ↓
JPA tracks change
  ↓
flush
  ↓
UPDATE SQL
```

This is called:

> **Dirty checking**

This concept is absolutely essential for JPA interviews.

---

# 29. Entity states

You should know four major states.

```text
Transient
    ↓ persist
Managed
    ↓ detach / transaction ends
Detached

Managed
    ↓ remove
Removed
```

Let's understand each.

---

## Transient

A normal Java object that JPA isn't managing:

```java
User user = new User();
```

At this point:

```text
User object
   |
   X
not managed
```

---

## Managed

After:

```java
entityManager.persist(user);
```

or after loading an entity into the persistence context:

```java
User user = entityManager.find(User.class, id);
```

the entity can be managed.

```text
Persistence Context
      |
      +--> user
```

---

## Detached

An entity that was previously managed but is no longer associated with the current persistence context.

Conceptually:

```text
Persistence Context
      |
      X
    user
```

The Java object still exists.

It's just not currently managed.

---

## Removed

An entity scheduled for deletion:

```java
entityManager.remove(user);
```

Conceptually:

```text
Managed
   ↓ remove
Removed
```

These four states are standard JPA interview material.

---

# 30. Why persistence context matters

Consider:

```java
User u1 = repository.findById(1L).orElseThrow();
User u2 = repository.findById(1L).orElseThrow();
```

Within the same persistence context, JPA's identity semantics matter: the persistence context is designed around managed entity instances, and repeated access to the same database identity can reuse the managed instance rather than creating unrelated managed objects.

The practical result is that the persistence context acts somewhat like a first-level cache.

Think:

```text
Database
   ↑
   |
Persistence Context
   |
   +---- User#1
```

rather than:

```text
find #1 → always hit DB
find #1 → always hit DB
find #1 → always hit DB
```

The exact SQL behavior depends on state and query type, so don't oversimplify this into "JPA never queries the database twice."

---

# 31. `save()` and dirty checking — common confusion

Suppose:

```java
@Transactional
public void update(Long id) {

    User user = repository.findById(id).orElseThrow();

    user.setName("Alice");

    repository.save(user);
}
```

For an already-managed entity, calling `save()` may not be necessary just to make the property change persistent; dirty checking at flush/commit can detect the modification.

This is one reason:

```java
repository.save(entity)
```

doesn't mean:

> "Without this line, the entity can never be updated."

The exact behavior depends on entity state and transaction boundaries.

---

# 32. What is `flush()`?

`flush()` means:

> synchronize pending changes in the persistence context with the database.

It does **not necessarily mean transaction commit**.

Think:

```text
Java changes
    ↓
Persistence Context
    ↓
flush()
    ↓
SQL sent/synchronized
    ↓
transaction
    ↓
commit
```

These are separate concepts.

`JpaRepository` exposes `flush()` and `saveAndFlush(...)`. ([Home][4])

---

# 33. `saveAndFlush()`

You can write:

```java
repository.saveAndFlush(user);
```

This means:

```text
save entity
+
flush pending changes
```

But don't translate it as:

> "Immediately commit the transaction."

Flush and commit are different.

An uncommitted transaction can still be rolled back.

This distinction is frequently tested.

---

# 34. `save()` vs `saveAndFlush()`

### `save()`

```java
repository.save(user);
```

Persist/merge the entity as appropriate; synchronization with the database typically happens at flush/commit according to JPA transaction behavior.

### `saveAndFlush()`

```java
repository.saveAndFlush(user);
```

Also flushes immediately as part of the repository operation.

Think:

```text
save
  ↓
persistence context

saveAndFlush
  ↓
persistence context
  ↓
flush
```

Not:

```text
saveAndFlush
  ↓
automatic commit
```

---

# 35. Why transactions matter

Now connect this with:

```java
@Transactional
```

A typical service method:

```java
@Transactional
public void updateUser(Long id) {

    User user = repository.findById(id)
            .orElseThrow();

    user.setName("Alice");
}
```

During the transaction:

```text
Transaction starts
       ↓
Persistence Context
       ↓
load entity
       ↓
modify entity
       ↓
dirty checking
       ↓
flush
       ↓
commit
```

This is the normal JPA lifecycle that you should visualize.

We'll later spend an entire topic on **Spring transactions**, because there are many subtle rules around proxies, propagation, isolation, rollback, and `readOnly`.

---

# 36. Why repository methods can work without writing `@Transactional`

Another common confusion:

```java
repository.findById(...)
```

works even when you didn't put:

```java
@Transactional
```

on your controller.

Why?

Spring Data JPA provides transactional behavior for repository operations according to its repository implementation/configuration. The current `SimpleJpaRepository` API is itself annotated as transactional, with class-level read-only semantics and method-specific write transactions. ([Home][6])

But your **service-level transaction boundary** is still extremely important for operations involving multiple repository calls and a unit of work.

For example:

```java
@Transactional
public void transferMoney(...) {

    accountRepository...
    accountRepository...
    transactionRepository...
}
```

The three operations should participate in one transaction.

We'll dive much deeper into this later.

---

# 37. Why service-level transactions are usually preferred

Compare:

```java
controller
   |
   +--> repository A
   |
   +--> repository B
```

versus:

```text
controller
   |
   v
service @Transactional
   |
   +--> repository A
   |
   +--> repository B
```

The second structure allows the service method to define the logical unit of work.

For example:

```java
@Transactional
public void transfer(
        Long fromId,
        Long toId,
        BigDecimal amount) {

    debit(fromId, amount);
    credit(toId, amount);
}
```

If something fails:

```text
debit succeeds
credit fails
```

the transaction can roll back the entire unit.

That's much more meaningful than independently transactional repository calls.

---

# 38. Repository query return types

Spring Data repositories support many return types.

Common ones:

```java
Optional<User>
User
List<User>
Page<User>
Slice<User>
long
boolean
```

For example:

```java
Optional<User> findByEmail(String email);
```

or:

```java
List<User> findByStatus(Status status);
```

or:

```java
Page<User> findByStatus(
        Status status,
        Pageable pageable);
```

The repository infrastructure supports different result wrappers depending on the query and repository module. ([Home][7])

---

# 39. `Page` vs `Slice`

This is important for REST APIs.

### `Page<T>`

Contains:

```text
content
total elements
total pages
page number
page size
...
```

So obtaining a `Page` generally involves count information in addition to fetching the requested content.

### `Slice<T>`

Primarily tells you:

```text
content
is there another slice?
```

This can avoid the need for a total-count query in use cases that don't need total counts.

We'll cover pagination in greater depth later.

---

# 40. Repository method naming — reserved methods

Some names have special meaning.

For example:

```java
findById(...)
```

is a reserved repository method.

Spring Data treats methods such as:

```text
findById
existsById
deleteById
findAllById
deleteAllById
```

as predefined repository functionality. ([Home][10])

This matters because query derivation isn't always interpreted purely as "parse any method name mechanically."

---

# 41. Why method naming can cause surprises

Suppose your entity contains:

```java
@Id
private Long userId;

private Long id;
```

Then:

```java
findById(...)
```

has special repository semantics around the identifier.

The Spring Data documentation specifically calls out reserved repository methods and notes that `findById` targets the entity's identifier property. ([Home][10])

Interview takeaway:

> Not every `findBy...` method is just a simple property parser; some repository method names are reserved.

---

# 42. Repository implementation architecture

Now let's zoom in.

You write:

```java
public interface UserRepository
        extends JpaRepository<User, Long> {

    List<User> findByEmail(String email);
}
```

At startup, Spring Data analyzes:

```text
UserRepository
    |
    +--> repository metadata
    |
    +--> domain type = User
    |
    +--> ID type = Long
    |
    +--> inherited CRUD methods
    |
    +--> findByEmail query method
```

Then repository infrastructure creates the necessary implementation/proxy.

At invocation time:

```text
repository.findByEmail(...)
```

is routed to query execution infrastructure that creates/executes the appropriate JPA query.

This architecture is why interfaces alone can produce fully functional Spring beans.

---

# 43. `SimpleJpaRepository` is not the implementation of every custom query method

This distinction is subtle.

`SimpleJpaRepository` provides standard repository functionality.

For:

```java
findById()
save()
delete()
findAll()
```

it provides the standard implementation.

But for:

```java
findByEmail()
```

Spring Data's query infrastructure handles the declared query method.

Conceptually:

```text
UserRepository
       |
       +---- standard method
       |        ↓
       |   SimpleJpaRepository
       |
       +---- derived query
       |        ↓
       |   query execution infrastructure
       |
       +---- @Query
       |        ↓
       |   declared query
       |
       +---- custom fragment
                ↓
           custom implementation
```

This distinction helps explain how Spring Data can support both framework-provided and application-defined methods.

---

# 44. Custom repository implementation

Sometimes you actually need custom repository code.

For example:

```java
public interface UserRepositoryCustom {

    List<User> searchUsers(...);
}
```

and:

```java
public interface UserRepository
        extends JpaRepository<User, Long>,
                UserRepositoryCustom {
}
```

Then provide the corresponding custom implementation/fragment.

Spring Data JPA explicitly supports custom repository implementations through repository fragments. ([Home][12])

This is appropriate when:

```text
standard repository methods
      +
derived queries
      +
@Query
```

are not enough.

---

# 45. When should you NOT create a custom repository?

Don't create:

```text
UserRepositoryCustom
UserRepositoryImpl
```

for every query.

For:

```java
findByEmail(...)
```

you don't need one.

For:

```java
findByStatusAndCreatedDateBetween(...)
```

you probably don't need one.

For moderate dynamic conditions:

```java
JpaSpecificationExecutor
```

may be enough.

Use custom implementations when you genuinely need custom data-access behavior.

This keeps your repository architecture simpler.

---

# 46. `@Query`

For a fixed custom query:

```java
@Query("""
    select u
    from User u
    where u.email = :email
""")
Optional<User> findUser(
        @Param("email") String email);
```

This is generally easier to understand than a massive method name.

You can write JPQL:

```java
select u from User u
```

rather than SQL against physical tables:

```sql
SELECT *
FROM users
```

That's an important JPA distinction.

---

# 47. JPQL vs SQL

Suppose:

```java
@Query("""
    select u
    from User u
    where u.email = :email
""")
```

Here:

```text
User
```

is the entity name.

And:

```text
u.email
```

is an entity property.

You're querying the **object model**, not necessarily the physical table structure.

The JPA provider then translates this into database-specific SQL.

Think:

```text
JPQL
 ↓
JPA provider
 ↓
SQL
```

This is one of the major benefits of JPA.

---

# 48. Native queries

You can also use database-specific SQL when needed.

Conceptually:

```java
@Query(value = """
    SELECT *
    FROM users
    WHERE email = :email
    """,
    nativeQuery = true)
```

Now you're operating much closer to the database.

Use this when appropriate, but understand the tradeoff:

```text
JPQL
+
portable entity-oriented model

Native SQL
+
database-specific power/control
```

We'll cover query strategies separately later.

---

# 49. Why JPA doesn't simply execute SQL for every method immediately

Consider:

```java
User user = repository.findById(id)
        .orElseThrow();

user.setName("John");
```

The persistence context tracks:

```text
User object
```

and JPA can defer synchronization until flush.

This enables:

```text
dirty checking
+
transactional batching
+
unit-of-work behavior
```

So JPA is not simply:

```text
Java method
   ↓
immediate SQL
```

It's more like:

```text
Java operations
      ↓
Persistence Context
      ↓
Unit of Work
      ↓
Flush
      ↓
SQL
```

This distinction is crucial.

---

# 50. The full architecture

You should be able to draw this in an interview:

```text
                 Spring Boot Application
                          |
                          v
                    Service Layer
                          |
                          v
                 Spring Data Repository
                          |
                  UserRepository
                          |
                          v
               Repository Proxy/Infrastructure
                          |
                          v
                  SimpleJpaRepository
                  / Query Execution
                          |
                          v
                     EntityManager
                          |
                          v
                    JPA Provider
                      (Hibernate)
                          |
                          v
                        JDBC
                          |
                          v
                      Database
```

And alongside it:

```text
               Persistence Context
                       |
             +---------+---------+
             |                   |
          Entity A            Entity B
             |                   |
             +------ dirty ------+
                    checking
                       |
                       v
                     flush
                       |
                       v
                       SQL
```

That is the mental model you want.

---

# 51. Very common interview traps

### Trap 1

> Is `JpaRepository` a class?

No.

It's an interface.

---

### Trap 2

> Does `JpaRepository` directly access the database?

Not directly.

Its repository infrastructure works through JPA/`EntityManager`, with the JPA provider handling persistence and SQL generation/execution. ([Home][6])

---

### Trap 3

> Who implements `JpaRepository`?

For standard JPA repository behavior, `SimpleJpaRepository` is the default implementation, with Spring Data's repository infrastructure creating the repository bean/proxy. ([Home][6])

---

### Trap 4

> Does `save()` always mean INSERT?

No.

It may result in:

```text
persist()
```

for a new entity or:

```text
merge()
```

for an existing entity based on entity-state detection. ([Home][9])

---

### Trap 5

> Does `saveAndFlush()` commit?

No.

`flush()` and transaction commit are different concepts.

---

### Trap 6

> Do I need `repository.save()` after every field modification?

Not necessarily for a managed entity inside an appropriate transaction because JPA dirty checking can detect changes.

---

### Trap 7

> Is Spring Data JPA the same thing as Hibernate?

No.

```text
Spring Data JPA
    ↓
repository abstraction

JPA
    ↓
persistence API/specification

Hibernate
    ↓
JPA provider
```

---

# 52. Interview question: "What happens when you inject JpaRepository?"

A strong answer:

> Spring Data JPA discovers the repository interface during repository configuration and creates a Spring-managed repository bean/proxy. Standard repository operations are backed by the JPA repository implementation such as `SimpleJpaRepository`, while query methods are handled by Spring Data's query infrastructure. Repository operations ultimately use JPA's `EntityManager` and the configured JPA provider to interact with the database. ([Home][7])

That's the level of answer an interviewer wants.

---

# 53. Interview question: "How does `findByEmail()` work?"

Say:

> Spring Data recognizes the method as a query method. It parses the method name according to its query-derivation rules, builds the corresponding query, and executes it through the JPA infrastructure. ([Home][10])

Example:

```java
findByEmail(String email)
```

becomes conceptually:

```text
email = ?
```

---

# 54. Interview question: "What is the difference between `JpaRepository` and `CrudRepository`?"

A good answer:

> `CrudRepository` provides generic CRUD functionality. `JpaRepository` is the JPA-specific repository abstraction and combines CRUD, paging/sorting, query-by-example support, and additional JPA-oriented operations such as flush and batch deletion. ([Home][5])

---

# 55. Interview question: "Why use JpaRepository instead of directly using EntityManager?"

`EntityManager` gives you lower-level JPA control.

`JpaRepository` gives you:

```text
less boilerplate
+
standard CRUD
+
derived queries
+
pagination
+
sorting
+
Spring Data integration
```

So:

```text
EntityManager
    ↓
lower-level control

JpaRepository
    ↓
higher-level repository abstraction
```

You can still inject `EntityManager` when you need lower-level or highly customized operations.

---

# 56. What you should remember from this topic

The entire topic can be compressed into this:

```text
JpaRepository
     |
     v
Spring Data creates repository infrastructure/proxy
     |
     +---------------------------+
     |                           |
standard methods             query methods
     |                           |
SimpleJpaRepository        derived/@Query/etc.
     |                           |
     +------------+--------------+
                  |
                  v
             EntityManager
                  |
                  v
               Hibernate
                  |
                  v
                JDBC
                  |
                  v
              Database
```

And:

```text
Entity loaded
    ↓
Managed by Persistence Context
    ↓
Java object modified
    ↓
Dirty checking
    ↓
Flush
    ↓
SQL UPDATE
```

And:

```text
save(entity)
    ↓
new entity?
   / \
 yes  no
  |    |
persist merge
```

Spring Data JPA 4.1.1 documents this `save()` behavior explicitly. ([Home][9])

---

# What you need to know cold for EPAM

You should now be able to explain, from memory:

```text
1. What is Spring Data JPA?
2. What is Repository?
3. CrudRepository vs JpaRepository
4. Where does the JpaRepository implementation come from?
5. What is SimpleJpaRepository?
6. How does a repository proxy work conceptually?
7. How does findByEmail() work?
8. What is query derivation?
9. When do you use @Query?
10. When do you use Specification?
11. What is EntityManager?
12. What is the persistence context?
13. Entity states: transient, managed, detached, removed
14. What is dirty checking?
15. save() vs saveAndFlush()
16. persist() vs merge()
17. flush vs commit
18. Spring Data JPA vs JPA vs Hibernate
19. When is JpaSpecificationExecutor useful?
20. When would you create a custom repository implementation?
```

The **next topic** should go deeper into the thing that makes JPA interviews difficult: **Entity Mapping and Relationships — `@Entity`, `@Id`, `@OneToMany`, `@ManyToOne`, owning side, `mappedBy`, foreign keys, cascade types, `orphanRemoval`, and bidirectional relationships.**

[1]: https://docs.spring.io/spring-data/jpa/reference/?utm_source=chatgpt.com "Spring Data JPA :: Spring Data JPA"
[2]: https://docs.spring.io/spring-data/commons/reference/repositories.html?utm_source=chatgpt.com "Working with Spring Data Repositories :: Spring Data Commons"
[3]: https://docs.spring.io/spring-data/jpa/reference/repositories/definition.html?utm_source=chatgpt.com "Defining Repository Interfaces :: Spring Data JPA"
[4]: https://docs.spring.io/spring-data/jpa/reference/4.0/api/java/org/springframework/data/jpa/repository/JpaRepository.html?utm_source=chatgpt.com "JpaRepository (Spring Data JPA 4.0.7 API)"
[5]: https://docs.spring.io/spring-data/jpa/reference/4.0/repositories/core-concepts.html?utm_source=chatgpt.com "Core concepts :: Spring Data JPA"
[6]: https://docs.spring.io/spring-data/jpa/reference/api/java/org/springframework/data/jpa/repository/support/SimpleJpaRepository.html?utm_source=chatgpt.com "SimpleJpaRepository (Spring Data JPA 4.1.1 API)"
[7]: https://docs.spring.io/spring-data/commons/reference/repositories/query-methods.html?utm_source=chatgpt.com "Query Methods :: Spring Data Commons"
[8]: https://docs.spring.io/spring-data/jpa/reference/repositories/create-instances.html?utm_source=chatgpt.com "Configuration :: Spring Data JPA"
[9]: https://docs.spring.io/spring-data/jpa/reference/jpa/entity-persistence.html?utm_source=chatgpt.com "Persisting Entities :: Spring Data JPA"
[10]: https://docs.spring.io/spring-data/commons/reference/repositories/query-keywords-reference.html?utm_source=chatgpt.com "Repository query keywords :: Spring Data Commons"
[11]: https://docs.spring.io/spring-data/jpa/docs/current/api/org/springframework/data/jpa/repository/support/JpaRepositoryImplementation.html?utm_source=chatgpt.com "JpaRepositoryImplementation (Spring Data JPA Parent 4.1.0 API)"
[12]: https://docs.spring.io/spring-data/jpa/reference/repositories/custom-implementations.html?utm_source=chatgpt.com "Custom Repository Implementations :: Spring Data JPA"

