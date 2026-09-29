# Topic 18 — JPA Entity Lifecycle & Persistence Context

This is the topic that makes the previous mapping concepts really click.

Knowing:

```java
@Entity
@ManyToOne
@OneToMany
```

is only the surface.

For an interview, you should understand what happens to an entity from the moment you create it until the transaction commits:

```text
new object
   ↓
Transient
   ↓ persist
Managed
   ↓
Dirty checking
   ↓
Flush
   ↓
Database
```

and also:

```text
Managed
   ↓ detach / clear / persistence context ends
Detached
   ↓ merge
Managed copy
```

The Jakarta Persistence specification defines four lifecycle states: **new, managed, detached, and removed**. It also defines `persist`, `merge`, `remove`, `refresh`, `detach`, `clear`, and `flush` as operations that affect entity state or the persistence context. ([Jakarta EE][1])

---

# 1. First: What is a Persistence Context?

This is the most important concept in this entire topic.

A **persistence context** is the set of entity instances that are currently being managed by a JPA `EntityManager`.

Think of it as:

```text
Persistence Context
─────────────────────────
User#1
User#2
Order#10
Order#11
Invoice#100
```

JPA knows about those objects and can track their state.

The `EntityManager` is the API through which you interact with that persistence context. ([Jakarta EE][1])

A useful mental model is:

> **Persistence context = JPA's managed world for entity objects.**

---

# 2. Why do we need a Persistence Context?

Suppose we didn't have one.

You load:

```java
User user = repository.findById(1L).orElseThrow();
```

Then:

```java
user.setName("Alice");
```

How would JPA know that you changed the object?

It needs some way to keep track of the entity.

That's what the persistence context provides.

Conceptually:

```text
Database
   ↑
   |
Persistence Context
   |
   +---- User#1
```

When the managed entity changes:

```text
User#1
  |
  +-- name: "John" → "Alice"
```

JPA can detect the difference when the persistence context is flushed.

The Jakarta Persistence specification explicitly states that modifications to managed entities are automatically detected; there is no separate `update()` operation. ([Jakarta EE][1])

---

# 3. The Four Entity States

Memorize these:

```text
NEW / TRANSIENT
MANAGED
DETACHED
REMOVED
```

The formal terminology in Jakarta Persistence uses **new**, **managed**, **detached**, and **removed**. ([Jakarta EE][1])

Let's understand each rather than just memorizing definitions.

---

# 4. State 1 — Transient / New

Suppose:

```java
User user = new User();
user.setName("John");
```

At this point:

```text
User object
    |
    X
not associated with persistence context
```

It is a:

```text
NEW / TRANSIENT entity
```

The database doesn't know about it.

There is no persistent identity associated with it in the persistence context.

The specification describes a new entity as one that has no persistent identity and is not associated with a persistence context. ([Jakarta EE][1])

---

# 5. `persist()`

Now:

```java
entityManager.persist(user);
```

The entity becomes:

```text
NEW
 ↓ persist()
MANAGED
```

Conceptually:

```text
Before:

Java Heap
   |
   +--- User
          |
          X
       unmanaged


After persist():

Java Heap
   |
   +--- User
          |
          ↓
   Persistence Context
```

The key thing is:

> `persist()` makes a new entity managed.

The database insertion is associated with persistence synchronization; it is not necessarily equivalent to "execute INSERT immediately at this exact line." ([Jakarta EE][1])

---

# 6. Does `persist()` immediately execute INSERT?

This is a classic interview trap.

Many people say:

```java
entityManager.persist(user);
```

means:

```sql
INSERT INTO users ...
```

**immediately.**

That's too simplistic.

`persist()` makes the entity managed and schedules it for persistence. Database synchronization happens during **flush**.

Depending on the ID-generation strategy, provider, and circumstances, SQL can sometimes be issued earlier than the eventual transaction commit, but conceptually the JPA lifecycle is:

```text
persist()
   ↓
managed entity
   ↓
flush
   ↓
INSERT
```

The persistence specification states that `persist()` results in insertion when the persistence context is synchronized with the database. ([Jakarta EE][1])

Hibernate's documentation likewise describes INSERT actions as being scheduled by `persist`, with SQL generated as part of flushing. ([Hibernate Documentation][2])

---

# 7. State 2 — Managed

This is the most important state.

Suppose:

```java
User user = entityManager.find(User.class, 1L);
```

The returned entity is associated with the persistence context.

Conceptually:

```text
Persistence Context
        |
        +---- User#1
```

Now:

```java
user.setName("Alice");
```

JPA is tracking that object.

You don't need an explicit:

```java
entityManager.update(user);
```

There is no standard JPA `update()` operation.

The managed entity's changed state is detected automatically. ([Jakarta EE][1])

---

# 8. Dirty Checking

This is one of the most important JPA concepts.

Suppose:

```java
@Transactional
public void updateUser(Long id) {

    User user = entityManager.find(User.class, id);

    user.setName("Alice");
    user.setAge(30);
}
```

We didn't call:

```java
entityManager.update(user);
```

JPA can still persist the changes.

Why?

### Dirty checking.

Conceptually, JPA/Hibernate knows:

```text
Original managed state:

name = "John"
age  = 25
```

Later:

```text
Current state:

name = "Alice"
age  = 30
```

During flush, Hibernate detects that the managed entity has been modified and generates the appropriate `UPDATE`. Hibernate's current documentation explicitly describes dirty checking as determining whether a managed entity was modified and `EntityUpdateAction` generating the update during flush. ([Hibernate Documentation][2])

---

# 9. The Most Important JPA Rule

Memorize this:

> **Managed entities are automatically dirty-checked.**

Therefore:

```java
@Transactional
public void update(Long id) {

    User user = repository.findById(id)
            .orElseThrow();

    user.setName("Alice");
}
```

can be enough to update the database at transaction completion.

You don't necessarily need:

```java
repository.save(user);
```

for the change itself to become persistent.

Spring Data JPA's own documentation notes that calling `save` is not strictly necessary from a JPA perspective for a managed entity, although keeping the call can be consistent with the repository abstraction. ([Home][3])

---

# 10. Why does `save()` still appear everywhere?

You'll commonly see:

```java
User user = repository.findById(id).orElseThrow();

user.setName("Alice");

repository.save(user);
```

This is fine.

But:

```text
save()
```

isn't what makes a managed entity's changed state detectable.

The entity is already managed.

The real mechanism is:

```text
Managed entity
    ↓
property modification
    ↓
dirty checking
    ↓
flush
```

Spring Data's `save()` abstraction is useful for deciding between `persist` and `merge`, especially when the entity may be new or detached. ([Home][4])

---

# 11. State 3 — Detached

Now suppose the entity was managed:

```text
Persistence Context
       |
       +--- User#1
```

and then we do:

```java
entityManager.detach(user);
```

Now:

```text
Persistence Context
       |
       X
     User#1
```

The Java object still exists.

But JPA is no longer managing it.

Therefore:

```text
User
  ↓
DETACHED
```

The specification defines detached as an entity with persistent identity that is no longer associated with a persistence context. ([Jakarta EE][1])

---

# 12. What happens to changes after detach?

Suppose:

```java
User user = entityManager.find(User.class, 1L);

entityManager.detach(user);

user.setName("Alice");
```

The entity is detached when the modification occurs.

Therefore JPA isn't tracking that change as part of the old persistence context.

It won't simply dirty-check the detached object and generate an update.

That's one of the most important differences between:

```text
Managed
```

and:

```text
Detached
```

---

# 13. State 4 — Removed

Suppose:

```java
User user = entityManager.find(User.class, 1L);

entityManager.remove(user);
```

The entity becomes:

```text
MANAGED
   ↓ remove()
REMOVED
```

It is associated with the persistence context but scheduled for deletion from the database when the persistence context is synchronized and, normally, the transaction completes successfully.

The Jakarta Persistence specification defines removed entities as managed instances scheduled for removal from the database. ([Jakarta EE][1])

---

# 14. The Complete State Diagram

This is the diagram to memorize:

```text
                  +----------------+
                  |     NEW        |
                  |  (Transient)   |
                  +-------+--------+
                          |
                       persist
                          |
                          v
                  +----------------+
                  |    MANAGED     |
                  +---+---+----+---+
                      |   |    |
                detach   |   remove
                      |   |    |
                      v   |    v
               +----------+   +----------+
               | DETACHED |   | REMOVED  |
               +----+-----+   +----------+
                    |
                  merge
                    |
                    v
                 MANAGED
```

And conceptually:

```text
MANAGED
   |
   +--> dirty checking
   |
   +--> flush
           |
           v
       Database
```

---

# 15. `merge()` — probably the most misunderstood JPA method

Consider:

```java
User detachedUser = ...;

User managedUser =
        entityManager.merge(detachedUser);
```

A very common incorrect explanation is:

> "`merge()` makes the object managed."

Not quite.

More accurately:

> `merge()` copies the state of the given entity into a managed entity and returns the managed instance.

The returned managed entity may be a **different Java object** from the one passed into `merge()`. The Jakarta Persistence API explicitly specifies that the returned managed instance has the same persistent state but distinct Java object identity in the relevant merge cases. ([Jakarta EE][1])

---

# 16. Example of `merge()`

Suppose:

```java
User detachedUser = new User();
detachedUser.setId(1L);
detachedUser.setName("Alice");
```

Then:

```java
User managedUser =
        entityManager.merge(detachedUser);
```

Conceptually:

```text
detachedUser
      |
      | state copied
      v
managedUser
      |
      v
Persistence Context
```

Not:

```text
detachedUser
      ↓
magically becomes managed
```

That's a very important distinction.

---

# 17. `merge()` does not manage the original object

Suppose:

```java
User detachedUser = ...;

User managedUser =
        entityManager.merge(detachedUser);
```

Then:

```java
detachedUser == managedUser
```

is not generally true.

You should use:

```java
managedUser
```

as the managed entity.

This is a frequent interview trick.

---

# 18. `merge()` can insert too

Another misconception:

> "`merge()` is only for updates."

Not necessarily.

The JPA API states that merging a **new** entity can result in a new managed instance and eventually an insert; merging a detached entity can result in an update. ([Jakarta EE][1])

Conceptually:

```text
merge(new entity)
      ↓
new managed copy
      ↓
INSERT
```

or:

```text
merge(detached entity)
      ↓
managed copy
      ↓
UPDATE
```

This is why Spring Data's `save()` can choose between `persist()` and `merge()` based on entity-newness detection. ([Home][4])

---

# 19. `persist()` vs `merge()`

This table is worth memorizing:

| `persist()`                      | `merge()`                                           |
| -------------------------------- | --------------------------------------------------- |
| Primarily for new entity         | Used for new or detached entity state               |
| Makes the given instance managed | Returns a managed instance                          |
| Original object becomes managed  | Original object remains detached if it was detached |
| May lead to INSERT               | May lead to INSERT or UPDATE                        |
| Returns `void`                   | Returns managed entity                              |

The JPA `EntityManager` contract explicitly defines these semantics. ([Jakarta EE][1])

---

# 20. `detach()`

You can explicitly detach an entity:

```java
entityManager.detach(user);
```

Result:

```text
MANAGED
   ↓ detach()
DETACHED
```

Any unflushed changes on that entity will no longer be synchronized through that persistence context.

The JPA API specifies that `detach()` evicts the entity from the persistence context and that unflushed changes will not be made persistent through that context. ([Jakarta EE][1])

---

# 21. `clear()`

Instead of detaching one entity:

```java
entityManager.detach(user);
```

you can clear the entire persistence context:

```java
entityManager.clear();
```

Conceptually:

```text
Before:

Persistence Context
--------------------
User#1
User#2
Order#10
Invoice#100

clear()

After:

Persistence Context
--------------------
(empty)
```

All managed entities become detached. ([Jakarta EE][1])

This is particularly useful to understand in large batch-processing operations.

---

# 22. `detach()` vs `clear()`

Think:

```text
detach(entity)
    ↓
one entity leaves persistence context
```

versus:

```text
clear()
    ↓
all entities leave persistence context
```

Easy interview question.

---

# 23. `refresh()`

Suppose:

```java
User user = entityManager.find(User.class, 1L);

user.setName("Temporary Name");
```

Now suppose you want to discard your in-memory unflushed change and reload the database state:

```java
entityManager.refresh(user);
```

Conceptually:

```text
Managed User
    |
    | refresh()
    v
Database state copied back
```

The JPA API says `refresh()` reloads the state of a managed entity from the database and overwrites unflushed changes. ([Jakarta EE][1])

---

# 24. `refresh()` vs `merge()`

These are easy to confuse.

### `merge()`

```text
Detached/new entity
       ↓
copy state INTO persistence context
```

### `refresh()`

```text
Database
    ↓
overwrite managed entity state
```

Think:

```text
merge
    → Java → Persistence Context

refresh
    → Database → Java
```

That's a very useful mental shortcut.

---

# 25. `flush()`

Now one of the most important concepts:

```java
entityManager.flush();
```

What does it mean?

> Synchronize pending changes in the persistence context with the database.

For example:

```java
user.setName("Alice");

entityManager.flush();
```

can result in:

```sql
UPDATE users
SET name = 'Alice'
WHERE id = 1;
```

The JPA specification states that modifications aren't necessarily immediately synchronized with the database; synchronization occurs during flush. ([Jakarta EE][1])

---

# 26. `flush()` does NOT mean `commit()`

This is probably the #1 `flush()` interview trap.

Suppose:

```java
@Transactional
public void updateUser() {

    user.setName("Alice");

    entityManager.flush();

    // more operations...
}
```

After:

```java
flush();
```

SQL may have been sent to the database.

But the transaction can still:

```text
commit
```

or:

```text
rollback
```

later.

Conceptually:

```text
Java changes
     ↓
Persistence Context
     ↓
flush()
     ↓
SQL synchronization
     ↓
transaction still active
     ↓
commit / rollback
```

So:

> **Flush ≠ commit**

Memorize this.

---

# 27. Why would you flush manually?

Normally you don't need:

```java
entityManager.flush();
```

everywhere.

JPA/Hibernate handles flushing according to the configured flush mode and transaction behavior.

But there are situations where you want to force synchronization before continuing.

For example:

```java
repository.save(entity);
entityManager.flush();

runSomethingThatDependsOnDatabaseState();
```

Possible reasons include:

* detect database constraints earlier
* ensure SQL is issued before a subsequent operation
* coordinate with database-side effects
* control when persistence-context changes are synchronized

Don't interpret that as "flush is always better."

It has a cost and should be deliberate.

---

# 28. Flush modes

The standard JPA flush modes include:

```text
AUTO
COMMIT
```

With `AUTO`, the persistence context must also be flushed before certain queries whose results could be affected by unflushed changes. With `COMMIT`, flushing occurs before transaction commit, while the provider may have more flexibility around earlier flushing. ([Jakarta EE][1])

For most application code, you generally don't manually manipulate flush mode.

But you should know it exists.

---

# 29. A very important sequence

Consider:

```java
@Transactional
public void example() {

    User user = repository.findById(1L).orElseThrow();

    user.setName("Alice");

    repository.findByEmail("alice@example.com");
}
```

At this point, the persistence context contains:

```text
User#1
name = Alice
```

but the database may still have:

```text
name = John
```

Under the default `AUTO` flush behavior, JPA may flush before executing a query whose result could be affected by those changes. ([Jakarta EE][1])

This is why people sometimes observe SQL appearing before transaction commit.

---

# 30. Flush vs SQL timing

A useful mental model is:

```text
Java change
    ↓
managed entity dirty
    ↓
flush triggered
    ↓
SQL generated/executed
```

Possible flush triggers include:

```text
transaction completion
explicit flush()
provider-required synchronization before relevant queries
```

The exact SQL timing can also depend on the provider, ID generation strategy, and other conditions.

Therefore don't tell an interviewer:

> "JPA always executes SQL only at commit."

That's not strictly correct.

---

# 31. Transaction lifecycle

Let's put transactions into the picture.

Typical Spring service method:

```java
@Transactional
public void updateUser(Long id) {

    User user = repository.findById(id)
            .orElseThrow();

    user.setName("Alice");
}
```

Think:

```text
Transaction starts
       ↓
Persistence Context associated
       ↓
find()
       ↓
User becomes managed
       ↓
modify User
       ↓
dirty checking
       ↓
flush
       ↓
commit
```

That is the standard unit-of-work mental model.

Spring Data JPA's repository methods themselves have transactional defaults, but an outer service-level transaction determines the transaction boundary for a composed business operation. ([Home][3])

---

# 32. Why the service layer should define the transaction

Suppose a business operation requires:

```text
1. Create Order
2. Reduce inventory
3. Create payment record
```

You don't want three unrelated transaction boundaries.

Instead:

```java
@Transactional
public void placeOrder(...) {

    orderRepository.save(order);

    inventoryService.reduce(...);

    paymentRepository.save(payment);
}
```

Conceptually:

```text
                ONE TRANSACTION
┌──────────────────────────────────┐
│                                  │
│ save Order                       │
│      ↓                           │
│ update Inventory                 │
│      ↓                           │
│ save Payment                     │
│                                  │
└───────────────┬──────────────────┘
                ↓
              commit
```

If an error occurs:

```text
Order saved
Inventory updated
Payment fails
        ↓
rollback
```

We'll have a dedicated deep dive into transactions later.

---

# 33. What happens when the transaction commits?

Conceptually:

```text
transaction
    |
    +--> persistence context
             |
             +--> managed entity changes
                         |
                         v
                       flush
                         |
                         v
                        SQL
                         |
                         v
                      COMMIT
```

At the end of a typical transaction-scoped persistence context, the managed entities become detached when the persistence context is no longer associated with the transaction/context. The exact lifecycle depends on how the `EntityManager` is configured; Spring applications commonly use transaction-scoped behavior. ([Jakarta EE][5])

---

# 34. Why detached entities matter in web applications

Consider:

```java
@GetMapping("/{id}")
public User getUser(@PathVariable Long id) {
    return repository.findById(id).orElseThrow();
}
```

The repository retrieves a managed entity inside its transaction.

But then you serialize it outside the persistence context.

Now relationships like:

```java
user.getOrders()
```

may be problematic if those associations are lazy and weren't initialized.

That's one reason entity → DTO mapping is often preferable in REST APIs.

It also helps prevent exposing your entire entity graph.

We'll later combine:

```text
persistence context
+
lazy loading
+
DTOs
+
N+1
```

into one topic.

---

# 35. The first-level cache

The persistence context also acts as a first-level cache in the standard JPA mental model.

Suppose:

```java
User u1 = entityManager.find(User.class, 1L);
User u2 = entityManager.find(User.class, 1L);
```

within the same persistence context.

The persistence context maintains identity for managed entities, so JPA can return the managed instance associated with that identity rather than creating multiple managed instances representing the same database identity.

Conceptually:

```text
Persistence Context

User#1
  ↑
  |
u1 + u2
```

instead of:

```text
u1 → different managed User#1
u2 → another managed User#1
```

This identity guarantee is an important part of the persistence-context model.

---

# 36. First-level cache vs second-level cache

Don't confuse:

```text
Persistence Context
```

with:

```text
Second-level cache
```

### First-level cache

Associated with:

```text
EntityManager / persistence context
```

and is intrinsic to the JPA persistence-context model.

### Second-level cache

Provider-level/shared cache across persistence contexts, if enabled.

Conceptually:

```text
Application
   |
EntityManager A ── Persistence Context A
EntityManager B ── Persistence Context B
                         |
                         v
                 Second-level cache
                         |
                         v
                     Database
```

Second-level caching is provider/configuration dependent.

For now, the important interview point is:

> **Every persistence context has its own managed entity set; a second-level cache, when enabled, is shared at a broader provider scope.**

---

# 37. `find()` vs JPQL query

This is a useful persistence-context nuance.

Consider:

```java
entityManager.find(User.class, 1L);
```

versus:

```java
entityManager.createQuery(
    "select u from User u where u.id = :id",
    User.class
)
```

The persistence context still matters in both cases, but query execution and SQL behavior differ.

One thing you should never assume is:

> "Every repository call always causes a database hit."

The persistence context can avoid redundant database access for already-managed entities in appropriate situations.

This is part of why JPA behaves more like a unit-of-work system than a thin SQL wrapper.

---

# 38. What happens to an entity after `clear()`?

Suppose:

```java
User user = repository.findById(1L).orElseThrow();
```

Then:

```java
entityManager.clear();
```

Now:

```text
user
  ↓
DETACHED
```

Then:

```java
user.setName("Alice");
```

That modification isn't automatically dirty-checked by the cleared persistence context.

If you want the state associated with a new/current persistence context, you can merge it:

```java
User managed =
        entityManager.merge(user);
```

Remember:

```text
merge() returns managed copy
```

Use the returned reference.

---

# 39. Why `merge()` can be dangerous

Suppose:

```java
User detachedUser = ...;
User managedUser = entityManager.merge(detachedUser);
```

Now:

```java
detachedUser.setName("Bob");
```

after the merge.

Does the persistence context automatically track that new change?

No.

Because:

```text
detachedUser
```

is still detached.

You would modify:

```java
managedUser.setName("Bob");
```

if you want the managed entity's state changed.

This is a classic interview scenario.

---

# 40. The `save()` connection

Now connect this with the previous topic.

Spring Data's:

```java
repository.save(entity);
```

uses the underlying JPA `EntityManager`.

Current Spring Data JPA documentation describes the basic behavior as:

```text
entity considered new
    ↓
entityManager.persist(entity)

entity considered existing
    ↓
entityManager.merge(entity)
```

and explains the default new-entity detection using version/id inspection, with `Persistable` as another strategy. ([Home][4])

So:

```java
repository.save(entity);
```

is essentially a higher-level Spring Data abstraction over this lifecycle decision.

---

# 41. How Spring Data determines "new"

Suppose:

```java
@Entity
public class User {

    @Id
    @GeneratedValue
    private Long id;
}
```

For default entity-state detection, Spring Data JPA can inspect the identifier:

```text
id == null
    ↓
new
    ↓
persist()
```

while a non-null ID generally indicates existing under that strategy.

If there's a suitable non-primitive `@Version` property, Spring Data checks that first. It also supports implementing `Persistable` when custom new-state detection is needed. ([Home][4])

---

# 42. Why manually assigned IDs complicate this

Suppose:

```java
@Id
private UUID id;
```

and your application assigns it immediately:

```java
user.setId(UUID.randomUUID());
```

The ID is already non-null.

But the object could still be brand new.

Therefore default ID-based newness detection may not be enough.

Spring Data JPA provides:

```java
Persistable<ID>
```

for this situation. ([Home][4])

This is an excellent advanced interview point.

---

# 43. Cascade and Entity Lifecycle

Remember the previous topic's:

```java
cascade = CascadeType.ALL
```

Now we can understand it properly.

Cascade applies lifecycle operations across relationships.

The Jakarta Persistence specification defines:

```text
PERSIST
MERGE
REMOVE
REFRESH
DETACH
ALL
```

and states that `ALL` is equivalent to all of them. ([Jakarta EE][6])

So:

```java
@OneToMany(cascade = CascadeType.PERSIST)
```

means:

```text
persist(parent)
      ↓
persist(child)
```

while:

```java
@OneToMany(cascade = CascadeType.MERGE)
```

means:

```text
merge(parent)
      ↓
merge(child)
```

This is why cascade is really about **lifecycle propagation**.

---

# 44. Lifecycle callbacks

JPA also supports lifecycle callback annotations such as:

```java
@PrePersist
@PostPersist

@PreUpdate
@PostUpdate

@PreRemove
@PostRemove

@PostLoad
```

For example:

```java
@PrePersist
void beforeInsert() {
    createdAt = LocalDateTime.now();
}
```

Conceptually:

```text
persist
   ↓
@PrePersist
   ↓
INSERT
   ↓
@PostPersist
```

These callbacks are useful for things such as:

```text
audit fields
timestamps
derived state
```

but you should avoid hiding complicated business logic inside entity lifecycle callbacks.

---

# 45. `@PreUpdate` isn't a generic "setter changed" callback

Another interview nuance.

```java
@PreUpdate
```

is related to the entity update lifecycle during persistence synchronization.

It isn't simply:

> "This method runs every time any Java setter is called."

For example:

```java
user.setName("Alice");
```

doesn't itself mean:

```text
@PreUpdate immediately executes
```

The change becomes relevant during the persistence lifecycle and eventual update synchronization.

This is another consequence of understanding dirty checking and flush.

---

# 46. Bulk update is different

Suppose you use JPQL:

```java
@Modifying
@Query("""
    update User u
    set u.status = :status
""")
void updateAllStatus(Status status);
```

This is a bulk update.

It operates directly at the query/database level rather than updating each managed entity one by one through normal dirty checking.

This creates an important issue:

```text
Persistence Context
       +
Database
```

can temporarily become out of sync.

For example:

```text
Persistence Context:
User#1 status = ACTIVE

Bulk UPDATE:
database says status = INACTIVE
```

But the persistence context may still hold:

```text
ACTIVE
```

until refreshed/cleared appropriately.

This is why bulk operations need special care.

We'll cover this in the query/transactions topics.

---

# 47. Why `clear()` can be useful in batch processing

Imagine processing:

```text
1,000,000 invoices
```

and every entity remains managed:

```text
Persistence Context
-------------------
Invoice 1
Invoice 2
Invoice 3
...
Invoice 1,000,000
```

Memory usage can grow significantly.

A common batch pattern is conceptually:

```text
process 100
flush
clear

process next 100
flush
clear
```

For example:

```java
for (...) {

    entityManager.persist(entity);

    if (count % 100 == 0) {
        entityManager.flush();
        entityManager.clear();
    }
}
```

The exact batch strategy depends on the provider and workload, but the lifecycle concepts are the reason this pattern works.

---

# 48. Entity state example from beginning to end

Let's trace one entity.

```java
User user = new User();
```

State:

```text
NEW
```

Then:

```java
repository.save(user);
```

Spring Data determines it is new and uses:

```text
persist()
```

State:

```text
MANAGED
```

Then:

```java
user.setName("Alice");
```

State:

```text
MANAGED + DIRTY
```

Then:

```text
flush()
```

Hibernate detects the dirty entity.

```text
DIRTY
  ↓
UPDATE SQL
```

Then:

```text
transaction commit
```

Database transaction completes.

Then, for the usual transaction-scoped persistence context:

```text
MANAGED
  ↓
context ends
  ↓
DETACHED
```

That's the complete lifecycle story.

---

# 49. Another example with `remove()`

Suppose:

```java
User user =
    entityManager.find(User.class, 1L);
```

State:

```text
MANAGED
```

Then:

```java
entityManager.remove(user);
```

State:

```text
REMOVED
```

On synchronization:

```sql
DELETE FROM users
WHERE id = 1;
```

After successful transaction completion, the entity is no longer managed in the old persistence context.

---

# 50. What if you call `persist()` on a managed entity?

This is a subtle API question.

Suppose:

```java
User user =
    entityManager.find(User.class, 1L);

entityManager.persist(user);
```

The entity is already managed.

`persist()` does not mean:

```text
INSERT another row
```

JPA's lifecycle semantics are based on the current entity state and the operation's defined behavior.

This is another reason you should think in terms of lifecycle states rather than "method = SQL statement."

---

# 51. What if you call `remove()` and then `persist()`?

An interesting JPA state transition is:

```text
MANAGED
   ↓ remove()
REMOVED
   ↓ persist()
MANAGED
```

The specification allows `persist()` on a removed entity to make it managed again, undoing the effect of the previous `remove()` under the defined lifecycle semantics. ([Jakarta EE][1])

You won't use this every day, but it's useful for demonstrating that:

> JPA operations work on entity states, not just SQL verbs.

---

# 52. The "entity is just a Java object" misconception

A JPA entity is still a Java object.

But once it becomes managed, it has an additional relationship with:

```text
Persistence Context
```

So think:

```text
Java object
     +
JPA managed state
     +
Persistence Context
```

This is why two otherwise identical Java objects can behave very differently:

```text
managed entity
```

versus:

```text
detached entity
```

because JPA tracks only the managed one through that persistence context.

---

# 53. Why entities shouldn't be passed blindly everywhere

Suppose your controller returns:

```java
@GetMapping("/{id}")
public User getUser(...) {
    return service.getUser(...);
}
```

You're exposing a JPA entity directly.

Now things such as:

```text
lazy relationships
bidirectional references
serialization
```

can cause problems.

For example:

```text
User
  ↓
orders
  ↓
customer
  ↓
orders
  ↓
...
```

You could get:

* lazy initialization problems
* huge response graphs
* recursive serialization
* accidental data exposure

This is one reason DTOs are common in REST APIs.

---

# 54. Persistence Context and DTOs

A common service pattern:

```java
@Transactional
public UserDto getUser(Long id) {

    User user = repository.findById(id)
            .orElseThrow();

    return new UserDto(
            user.getId(),
            user.getName()
    );
}
```

Now the entity is converted while you're in the transaction.

Conceptually:

```text
Database
   ↓
Persistence Context
   ↓
Managed Entity
   ↓
DTO
   ↓
Controller
   ↓
JSON
```

This makes the API boundary cleaner.

---

# 55. A major interview question

### "What is the difference between `save()` and `persist()`?"

Answer:

> `persist()` is a JPA `EntityManager` operation that makes a new entity managed. `save()` is a Spring Data repository abstraction that decides whether to call JPA `persist()` or `merge()` based on whether Spring Data considers the entity new. ([Home][4])

Excellent answer.

---

# 56. "What is the difference between `save()` and `saveAndFlush()`?"

```text
save()
   ↓
persist / merge

saveAndFlush()
   ↓
persist / merge
   ↓
flush
```

But:

```text
flush != commit
```

Spring Data JPA exposes both operations, and `saveAndFlush()` explicitly saves and flushes. ([Home][3])

---

# 57. "What is dirty checking?"

A strong interview answer:

> Dirty checking is JPA's mechanism for detecting modifications made to managed entities and synchronizing those changes with the database during flush. There is no explicit `update()` operation for ordinary managed-entity changes. ([Jakarta EE][1])

---

# 58. "What is the difference between managed and detached?"

### Managed

```text
associated with persistence context
↓
changes tracked
↓
dirty checking
```

### Detached

```text
not associated with persistence context
↓
ordinary changes aren't tracked by that context
```

That's the essential difference.

---

# 59. "What does merge() do?"

A strong answer:

> `merge()` takes the state of a new or detached entity and copies it into a managed entity associated with the current persistence context. It returns the managed instance, which may have a different Java object identity from the instance passed in. ([Jakarta EE][1])

This is much better than:

> "`merge()` updates the entity."

---

# 60. "What happens if I modify an entity without calling save()?"

If the entity is managed inside an appropriate transaction:

```java
user.setName("Alice");
```

the change can be detected by dirty checking and synchronized during flush.

Spring Data JPA explicitly notes that calling `save()` is not strictly necessary from JPA's perspective for such a managed entity change. ([Home][3])

---

# 61. "When exactly is SQL generated?"

Don't answer:

> "At `save()`."

or:

> "Only at commit."

A better answer:

> Entity operations modify the persistence context. During flush, the provider synchronizes pending changes with the database and generates the necessary SQL. Flush may occur at transaction completion, explicitly via `flush()`, or earlier when required by the configured flush mode and query execution. ([Jakarta EE][1])

That's a strong interview answer.

---

# 62. `persist` vs `merge` — visual comparison

```text
persist()
────────────

new User
   |
   v
persist()
   |
   v
same object becomes managed
   |
   v
INSERT during flush
```

versus:

```text
merge()
────────────

detached User
      |
      v
    merge()
      |
      v
managed copy
      |
      v
UPDATE/INSERT during flush
```

The second diagram is the one people often get wrong.

---

# 63. Entity lifecycle + Spring Data

Now combine everything:

```text
                    Spring Data JPA
                           |
                     repository.save()
                           |
                  +--------+--------+
                  |                 |
                new              existing
                  |                 |
                  v                 v
              persist()          merge()
                  |                 |
                  +--------+--------+
                           |
                           v
                      Managed entity
                           |
                     property change
                           |
                           v
                     Dirty checking
                           |
                           v
                         flush
                           |
                           v
                          SQL
                           |
                           v
                        commit
```

Spring Data JPA's `save()` semantics and entity-newness detection are documented directly by the project. ([Home][4])

---

# 64. The most important distinction of all

Think of these as three separate layers:

### Layer 1 — Java state

```text
User object
```

### Layer 2 — Persistence-context state

```text
NEW
MANAGED
DETACHED
REMOVED
```

### Layer 3 — Database state

```text
INSERT
UPDATE
DELETE
```

JPA sits between those concepts.

For example:

```text
user.setName("Alice")
        ↓
managed entity changed
        ↓
dirty checking
        ↓
flush
        ↓
UPDATE SQL
```

This is far more accurate than thinking:

```text
setter → UPDATE SQL
```

---

# 65. A realistic Spring Boot example

```java
@Service
public class UserService {

    private final UserRepository repository;

    public UserService(UserRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void renameUser(Long id, String name) {

        User user = repository.findById(id)
                .orElseThrow(() ->
                        new UserNotFoundException(id));

        user.setName(name);
    }
}
```

What happens?

### Step 1

Transaction begins.

```text
@Transactional
```

### Step 2

Repository loads user.

```text
findById()
```

### Step 3

Entity becomes managed by the transaction's persistence context.

### Step 4

You change:

```java
user.setName(name);
```

### Step 5

Dirty checking detects the change.

### Step 6

Flush synchronizes the change:

```sql
UPDATE users
SET name = ?
WHERE id = ?
```

### Step 7

Transaction commits.

That's the entire mechanism.

---

# 66. A common trick question

Consider:

```java
@Transactional
public void updateUser(Long id) {

    User user = repository.findById(id)
            .orElseThrow();

    user.setName("Alice");

    entityManager.detach(user);
}
```

Will the update definitely be persisted?

No.

Why?

Because you've detached the entity before the change has necessarily been synchronized through that persistence context.

`detach()` removes the entity from the context, and unflushed changes aren't subsequently made persistent through that context. ([Jakarta EE][1])

---

# 67. Another trick question

```java
@Transactional
public User update(User user) {

    User managed = entityManager.merge(user);

    user.setName("Bob");

    return managed;
}
```

Does `"Bob"` automatically get persisted?

Not necessarily.

The object that is managed is:

```java
managed
```

not necessarily:

```java
user
```

So the correct pattern is:

```java
User managed = entityManager.merge(user);

managed.setName("Bob");
```

This is why understanding `merge()` rather than memorizing "merge = update" matters.

---

# 68. Another trick question: `flush()` then exception

```java
@Transactional
public void test() {

    user.setName("Alice");

    entityManager.flush();

    throw new RuntimeException();
}
```

Could the transaction still roll back?

Yes.

Because:

```text
flush
```

is not:

```text
commit
```

SQL may already have reached the database, but if the transaction subsequently rolls back, the database transaction can undo those changes.

The exact rollback outcome depends on transaction configuration, exception classification, and database transaction behavior, which we'll cover in the transaction topic.

---

# 69. JPA lifecycle operations cheat sheet

| Operation   | Main effect                             |
| ----------- | --------------------------------------- |
| `persist()` | Make new entity managed                 |
| `merge()`   | Copy state into managed instance        |
| `remove()`  | Mark managed entity for removal         |
| `detach()`  | Remove one entity from context          |
| `clear()`   | Detach all managed entities             |
| `refresh()` | Reload managed state from DB            |
| `flush()`   | Synchronize persistence context with DB |

These semantics come directly from the Jakarta Persistence `EntityManager` contract. ([Jakarta EE][1])

---

# 70. The state transition cheat sheet

```text
new object
    ↓
  NEW
    |
 persist()
    ↓
 MANAGED
    |
    +---- modify ----> dirty
    |                    |
    |                  flush
    |                    ↓
    |                   SQL
    |
    +---- detach() ---> DETACHED
    |                       |
    |                     merge()
    |                       |
    |                       v
    +-------------------- MANAGED
    |
    +---- remove() ----> REMOVED
```

And:

```text
clear()
  ↓
all managed entities → DETACHED
```

---

# 71. What you should know cold for EPAM

You should be able to answer these without hesitation:

### Persistence Context

> What is it?
> Why does JPA need it?
> What does it track?

### Entity states

```text
NEW
MANAGED
DETACHED
REMOVED
```

### Operations

```text
persist
merge
remove
detach
clear
refresh
flush
```

### Dirty checking

You need to explain why:

```java
entity.setName("Alice");
```

can generate:

```sql
UPDATE
```

without:

```java
save()
```

for a managed entity.

### `merge()`

You must know:

> **The returned object is the managed instance.**

### Flush

You must know:

> **flush ≠ commit**

### Spring Data

You must know:

```text
repository.save()
    ↓
new? persist()
existing? merge()
```

based on Spring Data's entity-newness detection. ([Home][4])

---

# The mental model to permanently remember

When dealing with JPA, never think only:

```text
Java object ↔ Database row
```

Think:

```text
          Java Object
               |
               v
      +-------------------+
      | Persistence       |
      | Context           |
      |                   |
      | managed entities  |
      | dirty checking    |
      +---------+---------+
                |
              flush
                |
                v
             Database
```

And lifecycle:

```text
NEW
 ↓ persist
MANAGED
 ↓
modify
 ↓
dirty checking
 ↓
flush
 ↓
SQL
 ↓
commit
 ↓
DETACHED
```

The most important interview sentence from this topic is:

> **JPA manages entities through the persistence context. Managed entities are automatically tracked by dirty checking, and their changes are synchronized with the database during flush. `persist()` makes a new entity managed, while `merge()` copies state into a managed instance and returns that managed instance; flush is synchronization, not transaction commit.** ([Jakarta EE][1])

---

## Topic 18 revision questions

Try answering these mentally before moving on:

```text
1. What exactly is a persistence context?
2. What are the four JPA entity states?
3. What happens when persist() is called?
4. Does persist() immediately execute INSERT?
5. What is dirty checking?
6. Why can a managed entity be updated without save()?
7. What does merge() actually do?
8. Is the object passed to merge() managed afterward?
9. Why does merge() return an object?
10. What is the difference between detach() and clear()?
11. What does refresh() do?
12. What does flush() do?
13. Why is flush() different from commit?
14. When can JPA flush automatically?
15. How does Spring Data save() decide between persist() and merge()?
16. What happens to lazy relationships after an entity becomes detached?
17. Why can bulk UPDATE queries create persistence-context inconsistencies?
18. Why is @Transactional usually placed at the service/use-case boundary?
```

**Next topic: Spring Transactions in depth — `@Transactional`, transaction proxying/AOP, propagation (`REQUIRED`, `REQUIRES_NEW`, `NESTED`), isolation levels, rollback rules, `readOnly`, self-invocation, and the classic transaction interview traps.**

[1]: https://jakarta.ee/specifications/persistence/4.0/apidocs/jakarta.persistence/jakarta/persistence/entitymanager?utm_source=chatgpt.com "EntityManager (Jakarta Persistence API documentation)"
[2]: https://docs.hibernate.org/stable/orm/userguide/html_single/?utm_source=chatgpt.com "Hibernate ORM User Guide"
[3]: https://docs.spring.io/spring-data/jpa/reference/jpa/transactions.html?utm_source=chatgpt.com "Transactionality :: Spring Data JPA"
[4]: https://docs.spring.io/spring-data/jpa/reference/4.2/jpa/entity-persistence.html?utm_source=chatgpt.com "Persisting Entities :: Spring Data JPA"
[5]: https://jakarta.ee/specifications/persistence/4.0/jakarta-persistence-spec-4.0-m4?utm_source=chatgpt.com "Jakarta Persistence"
[6]: https://jakarta.ee/specifications/persistence/4.0/jakarta-persistence-spec-4.0-M4.pdf?utm_source=chatgpt.com "Jakarta Persistence"

