# Topic 22 — JPA Locking & Concurrency

This is one of the topics that separates someone who **uses JPA** from someone who **understands what happens when 100 users hit the same record at the same time**.

The central problem is:

> **What happens when two transactions read the same data, both modify it, and both try to save it?**

Without concurrency control, you can get a **lost update**.

JPA/Jakarta Persistence supports both **optimistic** and **pessimistic** locking, and Hibernate provides implementations of both. ([jakarta.ee](https://jakarta.ee/specifications/persistence/4.0/jakarta-persistence-spec-4.0-m4), [docs.hibernate.org](https://docs.hibernate.org/orm/7.1/userguide/html_single/?utm_source=chatgpt.com))

---

# 1. The problem: concurrent updates

Imagine:

```text
Product
---------
id       = 10
stock    = 10
```

Two requests arrive simultaneously:

```text
Request A                Request B
---------                ---------
read stock = 10         read stock = 10

buy 6                    buy 5

stock = 4                stock = 5

save                     save
```

Potentially the final database value becomes:

```text
stock = 5
```

But logically:

```text
10 - 6 - 5 = -1
```

The second transaction overwrote the first transaction's update.

That's a:

> **Lost update**

And this is a concurrency problem.

---

# 2. Why `@Transactional` alone doesn't solve everything

You might think:

```java
@Transactional
public void purchase(...) {
    ...
}
```

solves concurrency.

It doesn't necessarily.

A transaction gives you a transaction boundary:

```text
BEGIN
...
COMMIT / ROLLBACK
```

but concurrent transactions can still interact with the same rows.

For example:

```text
Transaction A
    |
    +--> read stock = 10

Transaction B
    |
    +--> read stock = 10
```

Both are inside valid transactions.

The issue is:

> **How do we prevent stale data from overwriting newer data?**

That's what locking strategies address.

---

# 3. Two major strategies

JPA/Hibernate gives us:

```text
OPTIMISTIC LOCKING
        vs
PESSIMISTIC LOCKING
```

Think of them as:

### Optimistic

> "Collisions are relatively uncommon. Let transactions proceed, then detect conflicts."

### Pessimistic

> "Collisions are likely. Lock the database resource so competing transactions must wait/fail."

Hibernate describes exactly this distinction: optimistic locking assumes transactions can proceed without locking resources and verifies that another transaction hasn't modified the data before completion; pessimistic locking assumes conflicts are likely and requires database resources to be locked while used. ([docs.hibernate.org](https://docs.hibernate.org/orm/7.1/userguide/html_single/?utm_source=chatgpt.com))

---

# 4. Optimistic locking

Optimistic locking is the most important one to understand first.

Suppose:

```java
@Entity
public class Product {

    @Id
    private Long id;

    private String name;

    private int stock;

    @Version
    private Long version;
}
```

Now the database might contain:

```text
id      stock      version
--------------------------
10      10         3
```

The important column is:

```text
version = 3
```

Jakarta Persistence defines `@Version` as the mechanism for detecting optimistic-lock conflicts. The provider reads the version, verifies it when updating/deleting, and increments the version when the entity is written. ([jakarta.ee](https://jakarta.ee/specifications/persistence/4.0/jakarta-persistence-spec-4.0-m4), [docs.hibernate.org](https://docs.hibernate.org/orm/7.1/userguide/html_single/?utm_source=chatgpt.com))

---

# 5. How `@Version` works

Suppose Transaction A loads:

```text
stock = 10
version = 3
```

Transaction B also loads:

```text
stock = 10
version = 3
```

Now A changes:

```text
stock = 4
```

Hibernate can conceptually execute:

```sql
UPDATE product
SET stock = 4,
    version = 4
WHERE id = 10
  AND version = 3;
```

One row is updated.

So:

```text
Database:

stock   = 4
version = 4
```

---

# 6. Transaction B now has stale data

B still has:

```text
stock = 10
version = 3
```

B tries:

```sql
UPDATE product
SET stock = 5,
    version = 4
WHERE id = 10
  AND version = 3;
```

But the database now contains:

```text
version = 4
```

So:

```text
WHERE version = 3
```

matches nothing.

Therefore:

```text
rows updated = 0
```

Hibernate concludes:

> Somebody else changed this entity after I read it.

and raises:

```text
OptimisticLockException
```

Hibernate documents that versioned entity updates include the version in the `WHERE` clause; when the version check fails, an `OptimisticLockException` indicates the current state is stale. ([docs.hibernate.org](https://docs.hibernate.org/orm/7.1/introduction/html_single/?utm_source=chatgpt.com))

---

# 7. This is the most important optimistic-locking diagram

```text
Initial DB

Product
----------------
id       = 10
stock    = 10
version  = 3
```

Two transactions:

```text
T1                     T2
 |                      |
 | read version 3       | read version 3
 |                      |
 | stock = 4            | stock = 5
 |                      |
 | UPDATE ...           |
 | WHERE version = 3   |
 |--------------------->|
 |                      |
 | version → 4          |
 |                      |
 |                      | UPDATE ...
 |                      | WHERE version = 3
 |                      |
 |                      X
 |                OptimisticLockException
```

That is optimistic locking.

---

# 8. Why is this called "optimistic"?

Because we don't initially lock the row.

We're assuming:

```text
"Probably nobody else will change this."
```

So:

```text
read
 ↓
work
 ↓
update if still unchanged
```

If a conflict occurs:

```text
detect
 ↓
reject
```

rather than:

```text
lock immediately
 ↓
make everyone wait
```

Hibernate notes that optimistic locking is especially suitable for read-often/write-sometimes workloads and scales well because resources aren't locked while users are thinking. ([docs.hibernate.org](https://docs.hibernate.org/orm/7.1/userguide/html_single/?utm_source=chatgpt.com))

---

# 9. Why `@Version` is so valuable

Without `@Version`:

```text
T1 reads 10
T2 reads 10

T1 writes 4
T2 writes 5

final = 5
```

No one knows that T2 used stale data.

With `@Version`:

```text
T1 writes successfully
T2 detects version mismatch
```

Now the application can:

```text
reject
retry
inform user
recalculate
```

instead of silently losing an update.

Jakarta Persistence strongly encourages optimistic locking for entities that may be concurrently accessed or merged from detached state because otherwise applications can experience inconsistent state and lost updates. ([jakarta.ee](https://jakarta.ee/specifications/persistence/4.0/jakarta-persistence-spec-4.0-m4))

---

# 10. `@Version` is updated by the provider

Suppose:

```java
@Version
private Long version;
```

Don't do:

```java
product.setVersion(product.getVersion() + 1);
```

Hibernate/JPA manages the version value as part of optimistic locking. Hibernate explicitly states that application code must not alter the version number maintained by Hibernate. ([docs.hibernate.org](https://docs.hibernate.org/orm/7.1/userguide/html_single/?utm_source=chatgpt.com))

Think:

```text
application
    |
    +--> modify business fields

Hibernate
    |
    +--> manage version
```

---

# 11. What does the SQL look like?

Conceptually:

```sql
UPDATE product
SET stock = ?,
    version = ?
WHERE id = ?
  AND version = ?;
```

The key is:

```sql
AND version = ?
```

That's the optimistic-lock check.

This is arguably the single most important SQL pattern to remember.

---

# 12. What happens if the version check fails?

Usually:

```text
UPDATE returns 0 rows
        ↓
optimistic conflict
        ↓
OptimisticLockException
```

Jakarta Persistence specifies that an optimistic locking conflict causes `OptimisticLockException`, and the transaction is marked for rollback when the exception is thrown by the persistence provider in an active transaction. ([jakarta.ee](https://jakarta.ee/specifications/persistence/4.0/jakarta-persistence-spec-4.0-m4))

This has an important consequence.

---

# 13. Don't casually catch `OptimisticLockException` and continue

Suppose:

```java
@Transactional
public void updateProduct(...) {
    try {
        ...
    }
    catch (OptimisticLockException e) {
        // retry here?
    }
}
```

The exception can mark the current transaction rollback-only.

So simply catching it doesn't make the current transaction safe to continue.

Jakarta Persistence explicitly requires the transaction to be marked for rollback when an optimistic conflict is detected in an active transaction. ([jakarta.ee](https://jakarta.ee/specifications/persistence/4.0/jakarta-persistence-spec-4.0-m4))

A retry generally needs a **fresh transaction** and freshly loaded state.

---

# 14. Retry pattern

Conceptually:

```text
Transaction 1
   ↓
load version 3
   ↓
conflict
   ↓
rollback
   ↓
start NEW transaction
   ↓
reload version 4
   ↓
recalculate
   ↓
update
   ↓
commit
```

Don't retry blindly.

You need to decide whether the business operation is safe to repeat.

For example:

```text
increment counter
```

may be retried differently from:

```text
charge credit card
```

because external side effects introduce another layer of idempotency concerns.

---

# 15. Optimistic locking isn't the same thing as preventing all concurrency

It doesn't make concurrent access impossible.

Instead:

```text
concurrent access
    ↓
allowed
    ↓
conflict detected
    ↓
one transaction rejected
```

That's the point.

---

# 16. `@Version` and detached entities

This is another important use case.

Suppose:

```text
Request 1
   ↓
GET Product
   ↓
Product version = 3

User thinks for 10 minutes

Request 2
   ↓
updates Product
   ↓
version = 4
```

Request 1 later submits:

```text
Product version = 3
```

The application merges/updates that stale entity.

JPA can detect:

```text
expected version = 3
database version = 4
```

and throw an optimistic locking exception. Jakarta Persistence explicitly defines version checking for stale detached entities during merge. ([jakarta.ee](https://jakarta.ee/specifications/persistence/4.0/jakarta-persistence-spec-4.0-m4))

This is a major reason optimistic locking is useful in web applications.

---

# 17. Optimistic locking with REST

A common API model is:

```json
{
  "id": 10,
  "name": "Laptop",
  "stock": 10,
  "version": 3
}
```

Client edits it.

Another client updates it first:

```text
version 3 → version 4
```

First client sends:

```text
version 3
```

Server detects:

```text
client version = 3
database version = 4
```

and can return a conflict.

This can be represented as:

```http
409 Conflict
```

depending on the API's chosen contract.

The key point is:

> **The client isn't allowed to blindly overwrite a newer version.**

---

# 18. Optimistic locking and HTTP `ETag`

There's an interesting connection here.

HTTP supports:

```text
ETag
If-Match
```

which can implement HTTP-level conditional updates.

Conceptually:

```text
Database version
      ↓
ETag
      ↓
If-Match
      ↓
conditional update
```

For example:

```http
If-Match: "v3"
```

The API can reject the request if the current representation is no longer version 3.

This is conceptually similar to optimistic concurrency control, although HTTP ETags and JPA `@Version` operate at different layers.

That's a powerful architecture-level connection.

---

# 19. Pessimistic locking

Now the other strategy.

Suppose:

```text
Inventory
---------
product_id = 10
stock = 10
```

and concurrent updates are highly likely.

Instead of:

```text
read freely
 ↓
detect conflict later
```

we can say:

> **Lock the database row while this transaction is working with it.**

That's pessimistic locking.

JPA provides:

```text
PESSIMISTIC_READ
PESSIMISTIC_WRITE
PESSIMISTIC_FORCE_INCREMENT
```

through `LockModeType`. ([jakarta.ee](https://jakarta.ee/specifications/persistence/4.0/apidocs/jakarta.persistence/jakarta/persistence/lockmodetype))

---

# 20. `PESSIMISTIC_WRITE`

This is the one you'll most commonly discuss.

Conceptually:

```text
T1
 ↓
read row + obtain write lock
 ↓
modify
 ↓
commit
```

Meanwhile:

```text
T2
 ↓
tries to acquire conflicting lock
 ↓
wait / timeout / fail
```

Jakarta Persistence defines `PESSIMISTIC_WRITE` as an immediate pessimistic database-level lock intended to force serialization among transactions attempting to update the entity. ([jakarta.ee](https://jakarta.ee/specifications/persistence/4.0/apidocs/jakarta.persistence/jakarta/persistence/lockmodetype))

---

# 21. The database is doing the locking

A very important point:

> **Hibernate does not simply lock a Java object in memory.**

Hibernate's current documentation states that pessimistic locking uses the database's locking mechanism rather than an in-memory object lock. ([docs.hibernate.org](https://docs.hibernate.org/orm/7.1/userguide/html_single/?utm_source=chatgpt.com))

So don't think:

```text
Java object
   ↓
synchronized
```

Think:

```text
Database row
   ↓
database lock
```

---

# 22. `SELECT ... FOR UPDATE`

Depending on the database and provider, a pessimistic write lock is commonly represented by SQL resembling:

```sql
SELECT *
FROM product
WHERE id = ?
FOR UPDATE;
```

The exact SQL syntax is database/provider dependent; Jakarta Persistence deliberately does not require a particular database locking implementation. ([jakarta.ee](https://jakarta.ee/specifications/persistence/4.0/jakarta-persistence-spec-4.0-m4), [docs.hibernate.org](https://docs.hibernate.org/orm/7.1/userguide/html_single/?utm_source=chatgpt.com))

So in an interview, say:

> "`PESSIMISTIC_WRITE` typically results in a database-level row lock, often implemented with a `SELECT ... FOR UPDATE`-style statement, depending on the database."

Don't say:

> "JPA always generates SELECT FOR UPDATE."

---

# 23. Spring Data JPA `@Lock`

Spring Data JPA gives you a convenient annotation:

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
Optional<Product> findById(Long id);
```

This tells Spring Data to apply that lock mode to the query.

Spring Data JPA documents `@Lock` specifically for assigning a `LockModeType` to repository query methods and even allows redeclaring CRUD methods to add lock metadata. ([docs.spring.io](https://docs.spring.io/spring-data/jpa/reference/4.2/jpa/locking.html?utm_source=chatgpt.com))

---

# 24. Example: inventory reservation

```java
public interface ProductRepository
        extends JpaRepository<Product, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Product> findById(Long id);
}
```

Service:

```java
@Transactional
public void reserveStock(Long productId, int quantity) {

    Product product =
            repository.findById(productId)
                    .orElseThrow();

    if (product.getStock() < quantity) {
        throw new InsufficientStockException();
    }

    product.setStock(
        product.getStock() - quantity
    );
}
```

Conceptually:

```text
T1
 ↓
lock product row
 ↓
read stock
 ↓
reduce stock
 ↓
commit
 ↓
release lock
```

If T2 arrives during T1's critical section:

```text
T2
 ↓
try lock same row
 ↓
wait
```

This prevents both transactions from simultaneously performing the critical operation on that row.

---

# 25. When should pessimistic locking be considered?

It's appropriate when:

```text
high contention
+
short critical section
+
conflicting updates are expensive
```

For example:

```text
limited inventory
seat reservation
resource allocation
highly contended counters
financial/resource reservation
```

But it's not automatically better than optimistic locking.

Hibernate's documentation notes that optimistic locking is generally more scalable, while pessimistic locking has a place when conflicts are frequent and you want to reduce rollback/update failures. ([docs.hibernate.org](https://docs.hibernate.org/orm/7.1/userguide/html_single/?utm_source=chatgpt.com))

---

# 26. Why pessimistic locking can hurt performance

Suppose:

```text
T1 acquires lock
   ↓
T1 performs HTTP call
   ↓
waits 3 seconds
   ↓
commit
```

During those 3 seconds:

```text
other transactions
        ↓
blocked
```

Now imagine 100 requests.

You can end up with:

```text
lock contention
connection usage
wait time
timeouts
throughput reduction
```

This is why:

> **Keep pessimistic-lock critical sections short.**

Hibernate specifically advises avoiding long-held pessimistic locks across user interaction. ([docs.hibernate.org](https://docs.hibernate.org/orm/7.1/introduction/html_single/?utm_source=chatgpt.com))

---

# 27. Never hold a database lock while waiting for a user

Bad architecture:

```text
BEGIN
 ↓
lock row
 ↓
show confirmation page to user
 ↓
wait 30 seconds
 ↓
user clicks confirm
 ↓
commit
```

That's a terrible use of pessimistic locking.

The user interaction should generally happen outside the transaction.

Instead:

```text
Request 1
 ↓
read data
 ↓
return response

user thinks

Request 2
 ↓
new transaction
 ↓
lock/recheck
 ↓
commit
```

Optimistic locking is often a better fit for this kind of workflow. Hibernate explicitly recommends optimistic locking for multiuser applications where conversations span time. ([docs.hibernate.org](https://docs.hibernate.org/orm/7.1/introduction/html_single/?utm_source=chatgpt.com))

---

# 28. `PESSIMISTIC_READ`

Conceptually:

```text
read row
+
obtain read lock
```

It can be used when you need stronger repeatable-read semantics for the locked entity while allowing other transactions to read it where the database supports shared locks.

The Jakarta Persistence specification defines `PESSIMISTIC_READ` as preventing dirty and non-repeatable reads for locked data and describes it as a database-level pessimistic lock. ([jakarta.ee](https://jakarta.ee/specifications/persistence/4.0/apidocs/jakarta.persistence/jakarta/persistence/lockmodetype))

In practice, database support and translation can vary.

---

# 29. `PESSIMISTIC_FORCE_INCREMENT`

This is more advanced.

It combines:

```text
pessimistic locking
+
version increment
```

So you get:

```text
lock the entity
+
force its version to advance
```

Jakarta Persistence defines this mode specifically as a pessimistic write lock that also forces a version increment on versioned entities. ([jakarta.ee](https://jakarta.ee/specifications/persistence/4.0/apidocs/jakarta.persistence/jakarta/persistence/lockmodetype))

You generally won't use this in ordinary CRUD code.

But know what it means.

---

# 30. Optimistic lock modes

JPA also supports:

```text
OPTIMISTIC
OPTIMISTIC_FORCE_INCREMENT
```

The older:

```text
READ
WRITE
```

names are synonyms for these optimistic modes, with the newer names preferred for new applications. ([jakarta.ee](https://jakarta.ee/specifications/persistence/4.0/apidocs/jakarta.persistence/jakarta/persistence/lockmodetype))

### `OPTIMISTIC`

Perform version checking.

### `OPTIMISTIC_FORCE_INCREMENT`

Perform optimistic locking and force a version increment even without an ordinary entity update.

---

# 31. `@Version` vs `OPTIMISTIC`

This distinction is subtle.

If an entity is versioned:

```java
@Version
private Long version;
```

normal updates already involve version checking.

So you don't usually need to explicitly write:

```java
entityManager.lock(
    entity,
    LockModeType.OPTIMISTIC
);
```

for every ordinary modification.

Hibernate notes that an explicit optimistic lock isn't necessary on an entity that is already being modified because its version will be checked during update. ([docs.hibernate.org](https://docs.hibernate.org/orm/7.1/introduction/html_single/?utm_source=chatgpt.com))

---

# 32. Why use `OPTIMISTIC_FORCE_INCREMENT`?

Suppose you want:

```text
"I've logically touched/claimed this entity,
so I want its version to change even if no ordinary field changed."
```

Then:

```java
LockModeType.OPTIMISTIC_FORCE_INCREMENT
```

can force the version to advance.

This can be useful in more advanced domain workflows.

---

# 33. Spring Data `@Lock` example

You can write:

```java
public interface OrderRepository
        extends JpaRepository<Order, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select o
        from Order o
        where o.id = :id
    """)
    Optional<Order> findForUpdate(
            @Param("id") Long id
    );
}
```

Then:

```java
@Transactional
public void updateOrder(Long id) {

    Order order =
            repository.findForUpdate(id)
                    .orElseThrow();

    ...
}
```

The transaction boundary matters.

A pessimistic lock is intended to be retained until the current transaction completes. Jakarta Persistence explicitly defines that pessimistic locks remain until transaction completion. ([jakarta.ee](https://jakarta.ee/specifications/persistence/4.0/apidocs/jakarta.persistence/jakarta/persistence/lockmodetype))

---

# 34. Pessimistic locking requires a transaction

Think:

```text
@Transactional
      +
PESSIMISTIC_WRITE
```

not:

```text
repository.findForUpdate()
   ↓
lock forever
```

The lock belongs to a transaction.

When the transaction completes:

```text
COMMIT / ROLLBACK
       ↓
lock released
```

The exact database behavior is implementation-specific, but the JPA semantics require the pessimistic lock to be maintained until transaction completion. ([jakarta.ee](https://jakarta.ee/specifications/persistence/4.0/jakarta-persistence-spec-4.0-m4))

---

# 35. Pessimistic lock timeout

What if T2 tries to lock a row currently locked by T1?

It may:

```text
wait
```

and eventually:

```text
timeout
```

or:

```text
fail immediately
```

depending on configuration/database support.

JPA defines:

```text
LockTimeoutException
PessimisticLockException
```

depending on whether the locking failure causes statement-level or transaction-level rollback. ([jakarta.ee](https://jakarta.ee/specifications/persistence/4.0/apidocs/jakarta.persistence/jakarta/persistence/lockmodetype))

Hibernate also supports lock timeout settings and dialect-specific handling. ([docs.hibernate.org](https://docs.hibernate.org/orm/7.1/userguide/html_single/?utm_source=chatgpt.com))

---

# 36. Optimistic vs pessimistic

This is the table you should know cold:

| Optimistic                           | Pessimistic                                  |
| ------------------------------------ | -------------------------------------------- |
| No database lock during normal read  | Database lock acquired                       |
| Assumes conflicts are uncommon       | Assumes conflicts are likely                 |
| Detects conflict later               | Prevents/serializes conflict earlier         |
| `@Version` commonly used             | `PESSIMISTIC_WRITE` / `PESSIMISTIC_READ`     |
| Better scalability in many workloads | Can reduce concurrency                       |
| Conflict causes rollback/failure     | Other transactions may wait                  |
| Good for read-heavy workloads        | Useful for high-contention critical sections |

Hibernate explicitly describes optimistic locking as scaling well for read-often/write-sometimes scenarios, while pessimistic locking is useful when concurrent transactions are expected to conflict. ([docs.hibernate.org](https://docs.hibernate.org/orm/7.1/userguide/html_single/?utm_source=chatgpt.com))

---

# 37. A very common interview question

### Which is better?

Don't answer:

> Optimistic.

or:

> Pessimistic.

The correct answer is:

> It depends on contention and business requirements.

If conflicts are rare:

```text
optimistic
```

can be attractive.

If conflicts are frequent and the critical section is short:

```text
pessimistic
```

may be appropriate.

---

# 38. Race condition

Now let's separate **locking** from **race conditions**.

Suppose:

```java
if (account.getBalance() >= amount) {
    account.setBalance(
        account.getBalance().subtract(amount)
    );
}
```

This looks fine.

But concurrent requests can both observe:

```text
balance = 100
```

and both pass:

```text
balance >= amount
```

This is a race condition.

The problem isn't the Java syntax.

It's:

```text
check
   +
act
```

being performed concurrently.

---

# 39. Check-then-act race

Classic example:

```text
T1:
check stock >= 1 → true

T2:
check stock >= 1 → true

T1:
decrement

T2:
decrement
```

Both saw the old state.

This is:

> **check-then-act race**

You need some form of concurrency control.

---

# 40. Solution 1 — optimistic locking

```text
T1 reads version 5
T2 reads version 5

T1 updates → version 6

T2 updates WHERE version = 5
               ↓
             fails
```

Then T2 can:

```text
retry
re-read
reject
```

---

# 41. Solution 2 — pessimistic lock

```text
T1
 ↓
PESSIMISTIC_WRITE
 ↓
lock row
 ↓
check stock
 ↓
decrement
 ↓
commit

T2
 ↓
waits for lock
 ↓
reads updated stock
```

Now only one transaction performs the critical operation at a time.

---

# 42. Solution 3 — atomic database operation

Sometimes you don't even need entity locking.

Suppose:

```sql
UPDATE product
SET stock = stock - :quantity
WHERE id = :id
  AND stock >= :quantity
```

Then check:

```text
rows updated == 1 ?
```

If yes:

```text
reservation succeeded
```

If zero:

```text
not enough stock / concurrent update lost race
```

This is an extremely useful database concurrency pattern.

The operation itself is atomic from the database's perspective.

For high-throughput counters/reservations, carefully designed atomic SQL can be preferable to repeatedly loading an entity.

---

# 43. `@Version` isn't always the best concurrency mechanism

Suppose:

```text
100,000 users
```

and every request updates:

```text
lastAccessedAt
```

A version increment on every access may create a lot of unnecessary contention.

Or suppose you need:

```text
"decrement stock only if stock >= quantity"
```

An atomic conditional update may express the business invariant more directly.

This is why experienced developers consider:

```text
optimistic locking
pessimistic locking
atomic SQL
database constraints
```

rather than automatically putting `@Version` everywhere and calling it done.

---

# 44. Database constraints as concurrency protection

Some business rules belong directly in the database.

Suppose:

```text
email must be unique
```

Don't rely only on:

```java
if (!repository.existsByEmail(email)) {
    repository.save(...);
}
```

because:

```text
T1: exists → false
T2: exists → false

T1: insert
T2: insert
```

Both can pass the check.

Instead, also have:

```text
UNIQUE(email)
```

in the database.

Then one insert succeeds and the other fails.

This is a powerful rule:

> **Application-level checks and database constraints solve different parts of concurrency.**

---

# 45. `existsBy...` doesn't guarantee uniqueness

This is a common interview trick.

Code:

```java
if (repository.existsByEmail(email)) {
    throw new DuplicateEmailException();
}
repository.save(user);
```

looks safe.

It isn't sufficient under concurrency.

The correct design is:

```text
application validation
+
database UNIQUE constraint
```

and handle:

```text
DataIntegrityViolationException
```

appropriately.

---

# 46. Deadlocks

Now we reach an important consequence of pessimistic/concurrent access.

Suppose:

```text
Transaction A:
lock Row 1
wait for Row 2

Transaction B:
lock Row 2
wait for Row 1
```

Diagram:

```text
T1
 |
 +--> lock A
 |
 +--> wait for B

T2
 |
 +--> lock B
 |
 +--> wait for A
```

Neither can continue.

This is a:

> **Deadlock**

The database can detect the cycle and abort one transaction.

---

# 47. How do we reduce deadlocks?

A major technique is:

> **Acquire locks in a consistent order.**

Bad:

```text
Transaction A:
lock Account 1
lock Account 2

Transaction B:
lock Account 2
lock Account 1
```

Potential deadlock.

Better:

```text
always lock the lower account ID first
```

So both do:

```text
Account 1
   ↓
Account 2
```

Consistent ordering significantly reduces lock-order deadlocks.

---

# 48. Another cause of deadlocks

Long transactions increase the window during which locks are held.

Bad:

```text
BEGIN
 ↓
lock row
 ↓
HTTP call
 ↓
expensive calculation
 ↓
another DB operation
 ↓
COMMIT
```

Better:

```text
short transaction
 ↓
lock
 ↓
critical DB work
 ↓
commit
```

Pessimistic locking + long transaction duration is a particularly dangerous combination.

---

# 49. Deadlock vs lock timeout

Don't confuse them.

### Lock timeout

```text
T2 waits for lock
   ↓
timeout
```

### Deadlock

```text
T1 waits for T2
T2 waits for T1
```

The database detects the cycle and typically aborts one transaction.

They are different concurrency failures.

---

# 50. Isolation vs locking

You learned transaction isolation in the previous topic.

Do not confuse:

```text
Isolation
```

with:

```text
Locking
```

Isolation answers:

> **What visibility guarantees do concurrent transactions have?**

Locking answers:

> **How do we explicitly control concurrent access to particular data?**

They interact, but they're not identical.

---

# 51. Does `SERIALIZABLE` eliminate the need for explicit locks?

Not necessarily.

`SERIALIZABLE` provides stronger transaction isolation, but it can have serious performance/concurrency implications, and exact database behavior varies.

Explicit pessimistic locking can still be appropriate when the use case needs a specific resource locked.

So don't answer:

> "Just use SERIALIZABLE."

as a universal concurrency solution.

---

# 52. Optimistic locking vs database isolation

Another important point:

You can have:

```text
READ_COMMITTED
+
@Version
```

and safely detect lost updates.

You don't necessarily need:

```text
SERIALIZABLE
```

for every concurrency problem.

Optimistic locking and transaction isolation solve overlapping but different concerns.

---

# 53. `@Version` and partial updates

Suppose:

```text
Product
---------
price
description
stock
version
```

T1 changes:

```text
price
```

T2 changes:

```text
description
```

With standard version-based optimistic locking:

```text
T1 version 1 → 2
T2 version 1 → conflict
```

Even though they're changing different fields.

That's expected with ordinary `@Version` semantics.

Hibernate offers more specialized optimistic-locking options such as `OptimisticLockType.DIRTY`, but those are Hibernate-specific and require careful modeling. ([docs.hibernate.org](https://docs.hibernate.org/orm/7.1/userguide/html_single/?utm_source=chatgpt.com))

For most applications:

```text
@Version
```

is the straightforward, portable choice.

---

# 54. Versionless optimistic locking

Hibernate also supports optimistic locking without an explicit `@Version` field through:

```java
@OptimisticLocking
```

with strategies such as:

```text
ALL
DIRTY
```

Hibernate expands the SQL `WHERE` clause using entity attributes instead of a dedicated version column. ([docs.hibernate.org](https://docs.hibernate.org/orm/7.1/userguide/html_single/?utm_source=chatgpt.com))

This is useful mainly for specific legacy-schema cases.

For normal applications:

```text
@Version
```

is much easier to reason about.

---

# 55. `@Version` with deletes

Optimistic locking isn't only for updates.

Suppose:

```text
T1 reads Product version 3
T2 deletes Product
T1 attempts update/delete
```

The version check can detect that the entity's current database state no longer matches what T1 read.

Jakarta Persistence includes version verification for both updates and deletes. ([jakarta.ee](https://jakarta.ee/specifications/persistence/4.0/jakarta-persistence-spec-4.0-m4))

---

# 56. Pessimistic locking and versioned entities

You can combine:

```text
@Version
+
PESSIMISTIC_WRITE
```

Jakarta Persistence requires appropriate interaction between pessimistic locking and version checks for versioned entities, and `PESSIMISTIC_FORCE_INCREMENT` additionally increments the version. ([jakarta.ee](https://jakarta.ee/specifications/persistence/4.0/jakarta-persistence-spec-4.0-m4))

So optimistic and pessimistic locking aren't always mutually exclusive concepts.

---

# 57. Spring Data locking

Spring Data makes locking very convenient:

```java
public interface AccountRepository
        extends JpaRepository<Account, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Account> findById(Long id);
}
```

The important thing is:

```text
@Lock
   ↓
LockModeType
   ↓
JPA query
   ↓
database locking
```

Spring Data JPA explicitly documents using `@Lock` on query methods and redeclared CRUD methods. ([docs.spring.io](https://docs.spring.io/spring-data/jpa/reference/4.2/jpa/locking.html?utm_source=chatgpt.com))

---

# 58. `@Lock` isn't itself a transaction

Another interview trap:

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
```

doesn't mean:

```text
"Start and complete a transaction automatically in the way I expect."
```

It's lock metadata.

You still need appropriate transactional context for a pessimistic lock to have meaningful transaction-scoped semantics.

Think:

```text
@Transactional
    +
@Lock
```

not:

```text
@Lock alone = entire concurrency strategy
```

---

# 59. A realistic seat-reservation example

Suppose:

```text
Flight seat A1
status = AVAILABLE
```

Two users try to book it.

### Option 1: Optimistic

```text
T1 reads A1, version 5
T2 reads A1, version 5

T1 → BOOKED, version 6
T2 → WHERE version=5 → conflict
```

Then T2 receives:

```text
seat already booked
```

### Option 2: Pessimistic

```text
T1 locks A1
T2 waits

T1 books
T1 commits

T2 reads BOOKED
T2 rejects
```

Both can be valid designs depending on contention and requirements.

---

# 60. Another realistic example: bank transfer

Suppose:

```text
Account A = 1000
Account B = 1000
```

Transfer:

```text
A -= 100
B += 100
```

You usually want:

```text
ONE transaction
```

and concurrency control around the accounts.

One danger:

```text
Transfer 1:
lock A
lock B

Transfer 2:
lock B
lock A
```

Potential deadlock.

Better:

```text
always lock accounts in a deterministic order
```

for example:

```text
lower ID first
higher ID second
```

Now both transactions obtain locks consistently.

---

# 61. Atomic SQL can be even simpler for counters

Suppose:

```text
remaining = 100
```

You need to decrement safely.

Instead of:

```text
SELECT remaining
Java calculation
UPDATE remaining
```

you could use:

```sql
UPDATE inventory
SET remaining = remaining - 1
WHERE id = ?
  AND remaining > 0;
```

Then:

```text
rows affected = 1
    ↓
success

rows affected = 0
    ↓
no inventory
```

This is an important database technique.

Not every concurrency problem requires an entity-level lock.

---

# 62. Database uniqueness is another concurrency tool

Likewise:

```sql
CREATE UNIQUE INDEX ...
```

is often the correct solution to:

```text
"Only one row may have this value."
```

For example:

```text
unique order number
unique email
unique username
unique external reference
```

The database is the final authority.

Application checks are useful for friendly validation, but the database constraint enforces the invariant under concurrency.

---

# 63. Optimistic locking and retry architecture

A common production structure:

```text
Controller
    ↓
Service
    ↓
@Transactional
    ↓
attempt update
    ↓
OptimisticLockException
    ↓
transaction rolls back
    ↓
retry in fresh transaction
```

You could have a retry mechanism around the transactional operation.

The important rule:

> **The retry should encompass the entire transaction, not just the failing `save()` call inside the same doomed transaction.**

This follows from the fact that an optimistic locking failure marks the active transaction rollback-only. ([jakarta.ee](https://jakarta.ee/specifications/persistence/4.0/jakarta-persistence-spec-4.0-m4))

---

# 64. Don't blindly retry everything

Imagine:

```text
charge card
 ↓
database optimistic conflict
 ↓
retry
```

The database operation might be retryable, but an external payment side effect may already have happened.

This is why concurrency + external systems leads to:

```text
idempotency
outbox
sagas
```

which we'll cover later in the microservices portion.

---

# 65. Pessimistic locking and deadlock retry

Even pessimistic locking can fail because of:

```text
deadlock
lock timeout
database conflict
```

A robust application may treat certain database errors as retryable.

But again:

```text
retry entire transaction
```

rather than continuing a corrupted/failed transaction context.

---

# 66. What happens when a lock cannot be acquired?

JPA distinguishes:

```text
PessimisticLockException
LockTimeoutException
```

depending on whether the database locking failure causes transaction-level or statement-level rollback. ([jakarta.ee](https://jakarta.ee/specifications/persistence/4.0/apidocs/jakarta.persistence/jakarta/persistence/lockmodetype))

In an interview, you don't need to memorize every provider-specific exception path, but knowing these two names is valuable.

---

# 67. The most important SQL patterns

### Optimistic

```sql
UPDATE product
SET stock = ?,
    version = ?
WHERE id = ?
  AND version = ?;
```

Key idea:

```text
conditional update
+
version check
```

### Pessimistic

Conceptually:

```sql
SELECT *
FROM product
WHERE id = ?
FOR UPDATE;
```

Key idea:

```text
lock row
+
hold until transaction completes
```

Exact pessimistic SQL is provider/database dependent. ([jakarta.ee](https://jakarta.ee/specifications/persistence/4.0/jakarta-persistence-spec-4.0-m4))

---

# 68. Interview question: "What is optimistic locking?"

Strong answer:

> Optimistic locking assumes concurrent conflicts are relatively uncommon, so transactions proceed without holding database locks for the whole unit of work. With JPA, it is commonly implemented using `@Version`; the provider includes the expected version in update/delete checks and throws `OptimisticLockException` when another transaction has already changed the entity. ([jakarta.ee](https://jakarta.ee/specifications/persistence/4.0/jakarta-persistence-spec-4.0-m4))

---

# 69. Interview question: "What is pessimistic locking?"

Strong answer:

> Pessimistic locking acquires a database-level lock on the data while the transaction uses it, so conflicting transactions are blocked or fail rather than both proceeding. JPA exposes modes such as `PESSIMISTIC_READ` and `PESSIMISTIC_WRITE`; the actual locking SQL is database/provider dependent. ([jakarta.ee](https://jakarta.ee/specifications/persistence/4.0/apidocs/jakarta.persistence/jakarta/persistence/lockmodetype))

---

# 70. Interview question: "What does `@Version` do?"

Strong answer:

> `@Version` marks an entity attribute used for optimistic concurrency control. The provider stores the current version and verifies it when updating or deleting; if the database version no longer matches the version originally read, the operation fails with an optimistic locking conflict instead of silently overwriting another transaction's update. ([jakarta.ee](https://jakarta.ee/specifications/persistence/4.0/jakarta-persistence-spec-4.0-m4))

---

# 71. Interview question: "What happens internally with `@Version`?"

Say:

```text
Read:
version = 5

Update:
WHERE id = ?
AND version = 5

success:
version becomes 6

if zero rows updated:
OptimisticLockException
```

That explanation is much stronger than just:

> "`@Version` prevents concurrent updates."

---

# 72. Interview question: "Optimistic or pessimistic?"

A strong answer:

> I choose based on contention and business requirements. Optimistic locking is attractive when conflicts are relatively rare and scalability matters; pessimistic locking is useful when conflicts are frequent and the critical section is short enough that blocking competing transactions is acceptable. ([docs.hibernate.org](https://docs.hibernate.org/orm/7.1/userguide/html_single/?utm_source=chatgpt.com))

---

# 73. Interview question: "Can `@Transactional` prevent lost updates?"

Answer:

> Not by itself. A transaction provides atomic transaction boundaries, but concurrent transactions can still read the same state and overwrite each other. Lost-update prevention may require optimistic locking, pessimistic locking, an atomic conditional database operation, or an appropriate database constraint depending on the use case.

Excellent answer.

---

# 74. Interview question: "What is the difference between optimistic locking and isolation?"

Say:

> Isolation defines the visibility and concurrency guarantees between transactions. Optimistic locking is an explicit concurrency-control mechanism that detects conflicting updates, commonly through a version field. They solve related but different problems and can be used together.

---

# 75. Interview question: "Why not use SERIALIZABLE everywhere?"

Because:

```text
stronger isolation
    ↓
more coordination
    ↓
less concurrency
    ↓
potentially lower throughput
```

and it doesn't necessarily express the business concurrency rule as directly as optimistic/pessimistic locking or an atomic update.

---

# 76. Interview question: "What is a deadlock?"

Strong answer:

> A deadlock occurs when transactions wait on each other's locks in a cycle. For example, T1 locks row A and waits for B while T2 locks B and waits for A. A common mitigation is to acquire multiple locks in a consistent order and keep transactions short.

---

# 77. Interview question: "Why can `existsBy...` still produce duplicates?"

Because:

```text
T1 checks
T2 checks
```

can happen before either inserts.

Therefore:

```text
application check
```

is not sufficient.

Use:

```text
UNIQUE DB constraint
```

and handle the resulting constraint violation.

---

# 78. Interview question: "Can optimistic locking fail at commit instead of save?"

Yes.

Depending on the persistence provider's flush strategy, the version check may occur during `flush()` or at transaction commit. Jakarta Persistence explicitly says the timing is provider-dependent; if the application needs to catch the optimistic lock failure earlier, an explicit `flush()` can force synchronization. ([jakarta.ee](https://jakarta.ee/specifications/persistence/4.0/jakarta-persistence-spec-4.0-m4))

This is a very good advanced answer.

---

# 79. Interview question: "Is `SELECT FOR UPDATE` standard JPA?"

No.

```text
SELECT FOR UPDATE
```

is database SQL syntax/behavior.

JPA gives you:

```text
LockModeType.PESSIMISTIC_WRITE
```

and the provider/database translates that into the appropriate locking mechanism.

Jakarta Persistence deliberately does not standardize the exact database locking implementation. ([jakarta.ee](https://jakarta.ee/specifications/persistence/4.0/jakarta-persistence-spec-4.0-m4))

---

# 80. Spring Data JPA's locking abstraction

You should remember:

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
```

is Spring Data JPA.

While:

```java
LockModeType.PESSIMISTIC_WRITE
```

is Jakarta Persistence.

So:

```text
Spring Data JPA
    ↓
@Lock

Jakarta Persistence
    ↓
LockModeType
```

Spring Data applies the chosen JPA lock mode to the repository query. ([docs.spring.io](https://docs.spring.io/spring-data/jpa/reference/4.2/jpa/locking.html?utm_source=chatgpt.com))

---

# 81. A production-style optimistic example

Entity:

```java
@Entity
public class Account {

    @Id
    @GeneratedValue
    private Long id;

    private BigDecimal balance;

    @Version
    private Long version;

    public void debit(BigDecimal amount) {
        if (balance.compareTo(amount) < 0) {
            throw new InsufficientBalanceException();
        }

        balance = balance.subtract(amount);
    }
}
```

Service:

```java
@Transactional
public void debit(
        Long accountId,
        BigDecimal amount) {

    Account account =
            repository.findById(accountId)
                    .orElseThrow();

    account.debit(amount);
}
```

If two transactions modify the same account:

```text
version check
    ↓
one succeeds
one conflicts
```

No explicit lock required.

---

# 82. A production-style pessimistic example

Repository:

```java
public interface AccountRepository
        extends JpaRepository<Account, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select a
        from Account a
        where a.id = :id
    """)
    Optional<Account> findForUpdate(
            @Param("id") Long id
    );
}
```

Service:

```java
@Transactional
public void debit(
        Long accountId,
        BigDecimal amount) {

    Account account =
            repository.findForUpdate(accountId)
                    .orElseThrow();

    account.debit(amount);
}
```

Now the critical account row is locked for the transaction.

---

# 83. The decision tree

Use this mental model:

```text
Concurrent updates?
       |
       v
Do conflicts happen rarely?
       |
      YES
       ↓
 @Version / optimistic
       |
       +--> conflict
              ↓
          rollback/retry/reject
```

If:

```text
conflicts frequently
       |
       v
short critical section?
       |
      YES
       ↓
pessimistic lock
```

But also ask:

```text
Can business rule be expressed atomically?
       |
      YES
       ↓
conditional UPDATE
```

And:

```text
Is invariant database-enforceable?
       |
      YES
       ↓
UNIQUE / CHECK / FK constraint
```

This is a much more sophisticated concurrency decision process.

---

# 84. The four concurrency tools

Think:

```text
1. Optimistic locking
   @Version

2. Pessimistic locking
   PESSIMISTIC_WRITE / READ

3. Atomic DB operation
   UPDATE ... WHERE ...

4. Database constraint
   UNIQUE / FK / CHECK
```

Experienced applications frequently use a combination.

---

# 85. The most important concurrency diagram

```text
                    CONCURRENT REQUESTS
                           |
                    +------+------+
                    |             |
                    v             v
                   T1             T2
                    |             |
                 read X         read X
                    |             |
              +-----+-------------+-----+
              |                         |
              v                         v
         Optimistic                Pessimistic
           @Version                   Lock
              |                         |
      no lock while working       DB lock acquired
              |                         |
              v                         v
       check version             other TX waits/fails
              |                         |
          +---+---+                     |
          |       |                     |
        match   mismatch                |
          |       |                     |
       update   conflict             update
          |       |                     |
          v       v                     v
       commit   rollback              commit
```

---

# 86. What you should know cold for EPAM

These are the questions I'd expect you to handle now:

```text
1. What is concurrency control?
2. What is a lost update?
3. What is optimistic locking?
4. What is pessimistic locking?
5. How does @Version work?
6. What SQL does optimistic locking conceptually produce?
7. Why is version included in the WHERE clause?
8. What happens when the version check fails?
9. What is OptimisticLockException?
10. Why can't you casually continue the same transaction after it?
11. What is PESSIMISTIC_WRITE?
12. What is PESSIMISTIC_READ?
13. What is PESSIMISTIC_FORCE_INCREMENT?
14. What is @Lock in Spring Data JPA?
15. Is SELECT FOR UPDATE standard JPA?
16. When would you choose optimistic vs pessimistic?
17. What is a race condition?
18. What is check-then-act?
19. How can atomic UPDATE solve concurrency?
20. Why is existsBy... insufficient for uniqueness?
21. Why do we need database UNIQUE constraints?
22. What is a deadlock?
23. How do you reduce deadlocks?
24. How does isolation differ from locking?
25. Can optimistic locking happen at flush/commit?
26. Why shouldn't you hold pessimistic locks during user interaction?
27. Why is @Version useful for detached entities?
28. How does optimistic locking connect with REST ETags?
29. How would you safely reserve inventory under high concurrency?
30. How would you make a money-transfer operation safe when two transfers touch the same accounts?
```

---

# The one mental model to remember

Never think:

```text
@Transactional = concurrency solved
```

Think:

```text
                 CONCURRENT TRANSACTIONS
                          |
              +-----------+-----------+
              |           |           |
              v           v           v
          Optimistic   Pessimistic  Atomic DB
           locking       locking    operation
              |           |           |
           @Version    @Lock(...)   UPDATE ...
              |           |        WHERE ...
              |           |           |
              +-----------+-----------+
                          |
                          v
                 Database constraints
                          |
                          v
                 final data integrity
```

And the most important optimistic-locking mechanism:

```text
Database version = 5

Client loads version 5
        ↓
works on entity
        ↓
UPDATE ...
WHERE id = ?
AND version = 5
        ↓
success → version 6

or

0 rows updated
        ↓
OptimisticLockException
```

The most important pessimistic mechanism:

```text
@Transactional
      ↓
PESSIMISTIC_WRITE
      ↓
database lock
      ↓
critical section
      ↓
COMMIT
      ↓
lock released
```

### The interview sentence to memorize

> **Optimistic locking allows concurrent work and detects stale updates, commonly using `@Version`; pessimistic locking acquires database-level locks to serialize conflicting access. Optimistic locking is usually attractive when conflicts are rare, while pessimistic locking is useful for short, highly contended critical sections. Neither replaces proper transaction design, atomic database operations, or database constraints where those are the right tools.** ([jakarta.ee](https://jakarta.ee/specifications/persistence/4.0/jakarta-persistence-spec-4.0-m4), [docs.hibernate.org](https://docs.hibernate.org/orm/7.1/userguide/html_single/?utm_source=chatgpt.com))

---

## Topic 22 revision drill

Before moving on, make sure you can explain this scenario without notes:

```text
Product stock = 10
version = 3

T1 reads stock 10 / version 3
T2 reads stock 10 / version 3

T1 buys 6
T2 buys 5

→ What SQL does T1 conceptually execute?
→ What SQL does T2 execute?
→ Why does one fail?
→ Which exception is raised?
→ What happens to the transaction?
→ How would you retry?
→ How would PESSIMISTIC_WRITE change the flow?
→ How could one atomic UPDATE solve the same problem?
→ What happens if there is no @Version or lock?
```

That single scenario tests a surprisingly large portion of JPA concurrency knowledge.

**Next topic: JPA Entity Equality & Identity — `==` vs `equals()`, entity IDs, `hashCode()`, Hibernate proxies, `Set`/`Map` behavior, generated IDs, and the infamous `equals/hashCode` pitfalls with JPA entities.**

