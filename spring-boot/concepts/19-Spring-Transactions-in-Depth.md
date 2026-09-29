# Topic 19 — Spring Transactions in Depth

This is one of the **most important Spring Boot interview topics**.

You already learned:

```text
Entity
   ↓
Persistence Context
   ↓
Dirty Checking
   ↓
Flush
   ↓
Database
```

Now we add the transaction boundary around that whole process:

```text
                 TRANSACTION
┌─────────────────────────────────────┐
│                                     │
│   Service                           │
│      ↓                              │
│   Repository                        │
│      ↓                              │
│   Persistence Context               │
│      ↓                              │
│   Dirty Checking                    │
│      ↓                              │
│   Flush                             │
│      ↓                              │
│   Database                          │
│                                     │
└─────────────────────────────────────┘
                    ↓
                 COMMIT
```

Spring's transaction abstraction provides a consistent programming model over JPA, JDBC, Hibernate, JTA, and other transaction APIs. For imperative applications, `PlatformTransactionManager` is the core abstraction. ([Home][1])

The annotation you'll use most often is:

```java
@Transactional
```

---

# 1. First: What is a transaction?

A transaction is a logical unit of work that should obey the familiar ACID properties:

```text
A → Atomicity
C → Consistency
I → Isolation
D → Durability
```

Imagine transferring money:

```text
Account A: -100
Account B: +100
```

These two changes belong to one logical operation.

We don't want:

```text
A → -100
B → FAIL
```

with the first change permanently committed.

We want:

```text
both succeed
       OR
both roll back
```

That is **atomicity**.

---

# 2. Why do we need transactions in Spring?

Consider:

```java
public void placeOrder(Order order) {

    orderRepository.save(order);

    inventoryRepository.reduceStock(
        order.getProductId(),
        order.getQuantity()
    );

    paymentRepository.createPayment(...);
}
```

What happens if:

```text
save Order           ✅
reduce Inventory     ✅
create Payment       ❌
```

Without one transaction:

```text
Order exists
Inventory reduced
Payment missing
```

The system is inconsistent.

With:

```java
@Transactional
public void placeOrder(Order order) {
    ...
}
```

we conceptually get:

```text
START TRANSACTION

save Order
reduce Inventory
create Payment

        ↓

if all successful → COMMIT

if failure → ROLLBACK
```

That's why transactions are usually defined around a **business/use-case operation**, often at the service layer.

---

# 3. What does `@Transactional` actually do?

This is where Spring becomes interesting.

A beginner explanation is:

> "`@Transactional` starts a transaction."

That is directionally correct, but incomplete.

Spring's declarative transaction management is implemented through **AOP**. The transactional metadata is interpreted by Spring infrastructure, which creates transactional proxy behavior; `TransactionInterceptor` works with a transaction manager to begin, commit, or roll back transactions around method invocations. ([Home][2])

So:

```java
@Transactional
public void placeOrder() {
    ...
}
```

is conceptually transformed into something like:

```text
Caller
  |
  v
Spring Proxy
  |
  +--> begin transaction
  |
  +--> call real method
  |
  +--> success? commit
  |
  +--> exception? rollback according to rules
```

This is **absolutely critical** for understanding Spring transactions.

---

# 4. The transaction proxy

Suppose:

```java
@Service
public class OrderService {

    @Transactional
    public void placeOrder() {
        ...
    }
}
```

Spring may create:

```text
Caller
   |
   v
OrderService Proxy
   |
   v
Real OrderService
```

So when another Spring bean calls:

```java
orderService.placeOrder();
```

the call normally goes through the proxy.

The proxy invokes transaction infrastructure around the actual method.

Spring's documentation explicitly states that proxy-based transaction management is the default mode and that only calls coming **through the proxy** are intercepted. ([Home][3])

---

# 5. The famous self-invocation problem

Suppose:

```java
@Service
public class OrderService {

    public void outer() {
        inner();
    }

    @Transactional
    public void inner() {
        ...
    }
}
```

You might think:

```text
outer()
   ↓
inner()
   ↓
START TRANSACTION
```

But in normal proxy mode, that isn't what happens.

Why?

Because:

```java
inner();
```

is effectively:

```java
this.inner();
```

The call happens **inside the target object itself**.

It does not go through the Spring proxy.

Therefore the transaction interceptor doesn't get a chance to intercept it.

Spring explicitly documents that self-invocation does not trigger transactional interception in the default proxy mode. ([Home][3])

This is one of the most frequently asked Spring interview questions.

---

# 6. Visualizing self-invocation

Normal external call:

```text
Controller
    |
    v
Proxy
    |
    +--> Transaction Interceptor
    |
    v
OrderService.placeOrder()
```

Self-invocation:

```text
OrderService.outer()
      |
      +--> this.inner()
               |
               v
         direct method call

         ❌ no proxy
         ❌ no transaction interception
```

So:

> **`@Transactional` doesn't magically modify the Java method itself. The call must normally pass through Spring's transactional proxy.**

---

# 7. How do you fix self-invocation?

### Preferred solution: move the transactional method to another bean

Instead of:

```java
class OrderService {
    outer() {
        inner();
    }

    @Transactional
    inner() {}
}
```

use:

```text
OrderService
     |
     v
TransactionalOrderService
```

Then:

```java
orderService.outer();
```

calls another Spring bean, so the call passes through its proxy.

Spring's AOP documentation recommends refactoring to avoid self-invocation as the cleanest solution. ([Home][4])

---

# 8. Another solution: self-injection

You can inject the proxied bean itself:

```java
@Service
public class OrderService {

    private final OrderService self;

    public OrderService(OrderService self) {
        this.self = self;
    }

    public void outer() {
        self.inner();
    }

    @Transactional
    public void inner() {
        ...
    }
}
```

Then:

```text
outer()
   ↓
self.inner()
   ↓
proxy
   ↓
transaction
```

This can work, but it's usually less clean than restructuring the beans.

---

# 9. `AopContext.currentProxy()`

You may also encounter:

```java
((OrderService) AopContext.currentProxy()).inner();
```

Spring's documentation explicitly calls this approach highly discouraged because it couples the code directly to Spring AOP. ([Home][4])

So in an interview:

> "How do you solve self-invocation?"

Prefer:

> "Refactor the transactional method behind another Spring bean so the call crosses the proxy."

That's the cleanest answer.

---

# 10. `@Transactional` defaults

The standard Spring `@Transactional` defaults include:

```text
propagation = REQUIRED
isolation = DEFAULT
readOnly = false
timeout = underlying default
rollback:
    RuntimeException → rollback
    Error            → rollback
    checked Exception → normally no rollback
```

Spring documents these defaults explicitly. ([Home][3])

So:

```java
@Transactional
public void doSomething() {
}
```

is effectively conceptually close to:

```java
@Transactional(
    propagation = Propagation.REQUIRED,
    isolation = Isolation.DEFAULT,
    readOnly = false
)
```

with the default rollback behavior described above.

---

# 11. `Propagation`

Propagation answers this question:

> **What should happen if a transactional method calls another transactional method while a transaction already exists?**

This is one of the most important Spring transaction topics.

Spring provides:

```text
REQUIRED
REQUIRES_NEW
NESTED
SUPPORTS
NOT_SUPPORTED
MANDATORY
NEVER
```

The propagation enum defines these behaviors. ([Home][5])

We'll focus heavily on the first three.

---

# 12. `REQUIRED`

This is the default.

```java
@Transactional(
    propagation = Propagation.REQUIRED
)
```

Meaning:

> Join the current transaction if one exists; otherwise create a new transaction.

Spring describes `REQUIRED` as creating a physical transaction when needed, while participating methods join an existing outer transaction. ([Home][6])

---

# 13. `REQUIRED` example

Suppose:

```java
@Transactional
public void outer() {
    methodA();
    methodB();
}
```

where both A and B are:

```java
@Transactional(propagation = Propagation.REQUIRED)
```

Conceptually:

```text
outer
  |
  +--> BEGIN TRANSACTION
  |
  +--> methodA
  |       |
  |       +--> joins existing TX
  |
  +--> methodB
          |
          +--> joins existing TX
  |
  +--> COMMIT
```

There is generally **one physical transaction**.

That's why `REQUIRED` is the normal default for service-layer operations. ([Home][6])

---

# 14. `REQUIRED` and logical transactions

Here's a subtle concept.

With nested `REQUIRED` calls:

```text
outer() REQUIRED
    |
    v
inner() REQUIRED
```

Spring can have:

```text
logical transaction scope A
    |
    +--- logical transaction scope B
```

but both participate in the same:

```text
physical database transaction
```

Spring explicitly distinguishes logical transaction scopes from the underlying physical transaction. ([Home][6])

This explains a famous issue:

```text
UnexpectedRollbackException
```

---

# 15. `UnexpectedRollbackException`

Suppose:

```java
@Transactional
public void outer() {

    try {
        inner();
    } catch (Exception e) {
        // swallow exception
    }
}
```

and:

```java
@Transactional
public void inner() {
    throw new RuntimeException();
}
```

Because `inner()` joins the outer transaction, the failure can mark the shared transaction as:

```text
ROLLBACK_ONLY
```

Even if `outer()` catches the exception, the transaction has already been marked rollback-only.

Then the outer method reaches:

```text
commit
```

but Spring discovers:

```text
this transaction cannot commit
```

and can throw:

```text
UnexpectedRollbackException
```

Spring documents this behavior explicitly: an inner participating scope can mark the shared transaction rollback-only, and the outer caller then gets `UnexpectedRollbackException` so it cannot incorrectly assume that a commit occurred. ([Home][6])

This is an extremely good interview scenario.

---

# 16. `REQUIRES_NEW`

Now:

```java
@Transactional(
    propagation = Propagation.REQUIRES_NEW
)
```

means:

> Always create a new physical transaction, suspending the current transaction if one exists.

Spring explicitly states that `REQUIRES_NEW` uses an independent physical transaction and suspends the existing transaction. ([Home][6])

Visualize:

```text
Outer Transaction
┌──────────────────────────────┐
│                              │
│   call inner()               │
│       ↓                      │
│   suspend outer TX           │
│                              │
└──────────────────────────────┘

          Inner Transaction
       ┌─────────────────┐
       │                 │
       │   execute       │
       │   commit/       │
       │   rollback      │
       │                 │
       └─────────────────┘

          resume outer TX
```

---

# 17. Why would we use `REQUIRES_NEW`?

Classic example:

```text
Main business transaction
        +
Audit transaction
```

Suppose:

```java
@Transactional
public void processOrder() {

    updateOrder();

    auditService.recordAudit();
}
```

and:

```java
@Transactional(propagation = Propagation.REQUIRES_NEW)
public void recordAudit() {
    ...
}
```

Now:

```text
TX #1
update order
   |
   +---- suspend TX #1
             |
             v
           TX #2
         save audit
             |
           COMMIT
             |
             v
       resume TX #1
   |
   v
continue
```

If TX #1 later rolls back:

```text
TX #1 → ROLLBACK
TX #2 → already COMMITTED
```

This can be exactly what you want for certain independent records.

But it should be deliberate.

---

# 18. `REQUIRES_NEW` has a major cost

It requires another physical transaction.

With JDBC-style resources, that often means another connection/resource allocation while the outer transaction's connection remains held.

Spring warns that excessive use can exhaust the connection pool and can even contribute to deadlocks if the pool isn't sized appropriately. ([Home][6])

So don't say:

> "Use `REQUIRES_NEW` whenever you want isolation."

That's too vague.

Use it when you explicitly need an independent transaction boundary.

---

# 19. `NESTED`

Now:

```java
Propagation.NESTED
```

has different semantics.

Conceptually:

```text
ONE PHYSICAL TRANSACTION
        |
        +---- SAVEPOINT
                 |
              inner work
                 |
        rollback to savepoint
```

Spring describes `NESTED` as using a single physical transaction with multiple savepoints, allowing an inner scope to roll back partially while the outer transaction continues. It is typically mapped to JDBC savepoints and therefore relies on JDBC resource transactions. ([Home][6])

---

# 20. `NESTED` vs `REQUIRES_NEW`

This is a classic interview comparison.

### `REQUIRES_NEW`

```text
Outer TX
   ↓ suspend
Inner TX
   ↓ independent commit/rollback
resume Outer TX
```

Two physical transactions.

### `NESTED`

```text
Outer TX
   ↓
SAVEPOINT
   ↓
Inner work
   ↓
rollback to SAVEPOINT if necessary
   ↓
continue Outer TX
```

One physical transaction.

So:

```text
REQUIRES_NEW → independent transaction

NESTED       → savepoint inside same transaction
```

---

# 21. Other propagation modes

You should know what these mean conceptually.

### `SUPPORTS`

```text
transaction exists?
    ↓
yes → join it
no  → run without one
```

### `MANDATORY`

```text
transaction exists?
    ↓
yes → join
no  → exception
```

### `NOT_SUPPORTED`

```text
transaction exists?
    ↓
yes → suspend it
no  → execute normally
```

### `NEVER`

```text
transaction exists?
    ↓
yes → exception
no  → execute normally
```

The propagation enum defines these semantics. ([Home][5])

For interviews, know:

```text
REQUIRED
REQUIRES_NEW
NESTED
```

very well, and know the remaining four conceptually.

---

# 22. The propagation table

| Propagation     | Existing transaction?      |     New physical transaction? |
| --------------- | -------------------------- | ----------------------------: |
| `REQUIRED`      | Join                       |           Only if none exists |
| `REQUIRES_NEW`  | Suspend existing           |                           Yes |
| `NESTED`        | Use nested scope/savepoint | No, normally same physical TX |
| `SUPPORTS`      | Join                       |                            No |
| `NOT_SUPPORTED` | Suspend                    |                            No |
| `MANDATORY`     | Must exist                 |                            No |
| `NEVER`         | Must not exist             |                            No |

Spring's propagation semantics are defined by its `Propagation` API and transaction reference documentation. ([Home][5])

---

# 23. Isolation

Now we move to another major concept.

Propagation answers:

> **What happens when transactions call each other?**

Isolation answers:

> **How isolated is one transaction's database work from other concurrent transactions?**

Spring's `Isolation` maps to JDBC isolation levels, with `DEFAULT` meaning the database's default isolation level. ([Home][7])

The standard levels are:

```text
DEFAULT
READ_UNCOMMITTED
READ_COMMITTED
REPEATABLE_READ
SERIALIZABLE
```

---

# 24. Dirty read

Suppose Transaction A changes:

```text
balance = 500
```

but hasn't committed.

Transaction B reads:

```text
balance = 500
```

Then A rolls back.

B has read a value that never actually committed.

That's a:

> **Dirty read**

`READ_UNCOMMITTED` allows dirty reads. ([Home][7])

---

# 25. Non-repeatable read

Transaction A:

```text
SELECT balance
→ 1000
```

Transaction B changes the row:

```text
1000 → 500
COMMIT
```

Transaction A reads again:

```text
SELECT balance
→ 500
```

Same transaction, same row, different result.

That's:

> **Non-repeatable read**

`READ_COMMITTED` prevents dirty reads, while non-repeatable reads can still occur.

---

# 26. Phantom read

Suppose Transaction A does:

```sql
SELECT *
FROM orders
WHERE amount > 1000;
```

It gets:

```text
10 rows
```

Meanwhile Transaction B inserts another matching row and commits.

Transaction A executes the query again:

```text
11 rows
```

A new matching row appeared.

That's:

> **Phantom read**

The standard isolation descriptions identify the progression of anomalies: `REPEATABLE_READ` prevents dirty and non-repeatable reads while phantoms can still occur, whereas `SERIALIZABLE` prevents dirty, non-repeatable, and phantom reads. ([Home][7])

---

# 27. Isolation level summary

| Isolation          | Dirty read | Non-repeatable read | Phantom read |
| ------------------ | ---------: | ------------------: | -----------: |
| `READ_UNCOMMITTED` |   Possible |            Possible |     Possible |
| `READ_COMMITTED`   |  Prevented |            Possible |     Possible |
| `REPEATABLE_READ`  |  Prevented |           Prevented |     Possible |
| `SERIALIZABLE`     |  Prevented |           Prevented |    Prevented |

This is the interview table you should know.

One caveat: exact concurrency behavior can depend on the database implementation, but these are the standard JDBC/JPA isolation semantics represented by Spring. ([Home][7])

---

# 28. What is `Isolation.DEFAULT`?

By default:

```java
@Transactional
```

uses:

```java
Isolation.DEFAULT
```

That means:

> Let the underlying database determine the isolation behavior.

Spring's documentation explicitly defines `DEFAULT` this way. ([Home][7])

This is why you shouldn't assume:

> "Every Spring application runs with READ_COMMITTED."

The database/configuration matters.

---

# 29. Isolation applies to `REQUIRED` and `REQUIRES_NEW`

Spring documents the `isolation` attribute as applying only to propagation values:

```text
REQUIRED
REQUIRES_NEW
```

for declarative transaction management. ([Home][3])

A subtle point:

If an inner `REQUIRED` method joins an existing transaction, its local isolation declaration doesn't simply create a new isolation level. By default, it participates in the outer transaction's characteristics. Spring documents this behavior and mentions `validateExistingTransaction` for stricter validation. ([Home][6])

---

# 30. `readOnly`

You can write:

```java
@Transactional(readOnly = true)
public List<User> getUsers() {
    ...
}
```

This communicates:

> This transaction is intended for read-only work.

It can allow the transaction manager or persistence provider to apply read-only optimizations.

But do not interpret it as:

> "The database is physically incapable of accepting writes."

The exact behavior depends on the transaction manager/database/provider.

Spring describes read-only as a transaction attribute and notes it can be useful as an optimization, especially with Hibernate. ([Home][8])

---

# 31. Common usage

For a query service:

```java
@Transactional(readOnly = true)
public UserDto getUser(Long id) {
    ...
}
```

For a write operation:

```java
@Transactional
public void updateUser(...) {
    ...
}
```

But don't blindly annotate every method with:

```java
readOnly = true
```

without understanding what resources/providers are doing.

---

# 32. Timeout

You can specify:

```java
@Transactional(timeout = 5)
```

Conceptually:

> The transaction should not be allowed to run indefinitely; the transaction infrastructure/underlying system handles timeout behavior.

Spring's `@Transactional` defines timeout attributes in seconds and notes that the actual enforcement depends on the underlying transaction system. ([Home][3])

This is useful for preventing:

```text
transaction held for minutes
        ↓
connection occupied
        ↓
locks held
        ↓
pool exhaustion
```

Timeouts should be chosen based on actual workload, not arbitrarily.

---

# 33. Rollback rules

This is another huge interview topic.

By default:

```text
RuntimeException → rollback
Error           → rollback
checked Exception → no automatic rollback
```

Spring documents this default behavior. ([Home][3])

Example:

```java
@Transactional
public void process() throws Exception {

    saveSomething();

    throw new IOException();
}
```

By default, a checked `IOException` does not trigger the same automatic rollback rule that an unchecked exception does.

---

# 34. How do we roll back for a checked exception?

Use:

```java
@Transactional(
    rollbackFor = IOException.class
)
```

Example:

```java
@Transactional(rollbackFor = IOException.class)
public void process() throws IOException {
    ...
}
```

Now:

```text
IOException
    ↓
rollback
```

Spring supports `rollbackFor` and `rollbackForClassName` for this purpose. ([Home][3])

---

# 35. `noRollbackFor`

You can also explicitly say:

```java
@Transactional(
    noRollbackFor = SomeException.class
)
```

meaning:

> Don't roll back for this exception type.

Spring supports both positive and negative rollback rules. ([Home][9])

---

# 36. Which rollback rule wins?

Suppose:

```java
rollbackFor = Throwable.class
```

and:

```java
noRollbackFor = SomeSpecificException.class
```

If:

```text
SomeSpecificException
```

is thrown, the stronger/specific matching rule wins.

Spring documents that when multiple rollback rules apply, the strongest matching rule wins. ([Home][9])

This is useful advanced interview knowledge.

---

# 37. A major transaction trap: catching exceptions

Consider:

```java
@Transactional
public void process() {

    try {
        repository.save(...);
        throw new RuntimeException();
    } catch (RuntimeException e) {
        System.out.println("handled");
    }
}
```

What happens?

You caught the exception.

From Spring's transaction interceptor's perspective, the method may return normally.

So you should not assume:

```text
exception happened inside method
    ↓
automatically rollback
```

The transaction infrastructure needs the exception to escape the transactional invocation or otherwise be explicitly marked rollback-only according to your configuration.

This is why:

```java
catch (Exception e) {
    // ignore
}
```

inside a transactional method can be dangerous.

---

# 38. Catching and rethrowing

This is different:

```java
@Transactional
public void process() {

    try {
        repository.save(...);
    } catch (RuntimeException e) {
        log.error("Failed", e);
        throw e;
    }
}
```

Now the exception escapes.

Default rollback rules can apply.

Conceptually:

```text
Exception
   ↓
catch
   ↓
log
   ↓
throw again
   ↓
transaction interceptor
   ↓
rollback
```

That's a common pattern.

---

# 39. `setRollbackOnly()`

Spring also allows explicit programmatic rollback:

```java
TransactionAspectSupport
    .currentTransactionStatus()
    .setRollbackOnly();
```

But this couples your code to Spring transaction infrastructure.

Spring explicitly recommends declarative rollback rules where possible and presents programmatic rollback as a more invasive option. ([Home][9])

So:

```text
Preferred:
@Transactional(rollbackFor = ...)

Less preferred:
setRollbackOnly()
```

---

# 40. The `UnexpectedRollbackException` scenario again

This becomes easier now.

```java
@Transactional
public void outer() {

    try {
        inner();
    } catch (RuntimeException e) {
        // catch it
    }

    // outer completes
}
```

and:

```java
@Transactional
public void inner() {
    throw new RuntimeException();
}
```

If `inner()` is `REQUIRED`, it's part of the same physical transaction.

The inner failure can mark the shared transaction:

```text
ROLLBACK_ONLY
```

Outer catches the exception, but that doesn't magically clear the rollback-only flag.

At outer commit:

```text
commit()
   ↓
transaction says ROLLBACK_ONLY
   ↓
UnexpectedRollbackException
```

Spring documents this exact reason for the exception. ([Home][6])

---

# 41. `REQUIRES_NEW` changes that example

Suppose:

```java
@Transactional
public void outer() {
    try {
        inner();
    } catch (Exception e) {
        // continue
    }
}
```

and:

```java
@Transactional(propagation = Propagation.REQUIRES_NEW)
public void inner() {
    throw new RuntimeException();
}
```

Now:

```text
Outer TX #1
    |
    | suspend
    v
Inner TX #2
    |
    X rollback
    |
    v
resume TX #1
    |
    v
continue
    |
    v
commit TX #1
```

The inner rollback does not automatically roll back the outer physical transaction because they are independent. ([Home][6])

---

# 42. Where should `@Transactional` usually go?

Typically:

```text
Controller
    ↓
Service   ← transaction boundary
    ↓
Repository
```

For example:

```java
@Service
public class OrderService {

    @Transactional
    public void placeOrder(...) {
        ...
    }
}
```

Why service?

Because the service method generally represents a business/use-case unit.

You usually don't want:

```text
Controller
  ↓
repository.save()  → TX #1

repository.update() → TX #2

repository.save()   → TX #3
```

when those operations logically belong to one business action.

Instead:

```text
Service @Transactional
        |
        +--> Repository A
        +--> Repository B
        +--> Repository C
              |
              v
         ONE transaction
```

---

# 43. Should controllers be transactional?

They can be, but it's generally better to put business transaction boundaries in the service layer.

Why?

Because:

```text
Controller
```

represents:

```text
HTTP transport
```

while:

```text
Service
```

represents:

```text
business operation
```

You don't want your transaction boundary to depend unnecessarily on the transport layer.

This is also why transaction management works on arbitrary Spring beans rather than only controllers or special classes. ([Home][10])

---

# 44. What if a service calls a repository?

Example:

```java
@Transactional
public void transfer() {

    accountRepository.debit(...);

    accountRepository.credit(...);
}
```

The repository operations participate in the existing transaction.

Conceptually:

```text
Service TX
    |
    +--> repository.debit
    |
    +--> repository.credit
    |
    v
commit
```

This is the standard service/facade transaction pattern described in Spring's transaction documentation. ([Home][6])

---

# 45. Thread-bound transactions

In imperative Spring transaction management, transaction resources are commonly bound to the current execution thread by the transaction infrastructure. ([Home][2])

So conceptually:

```text
Thread
  |
  +--> Transaction
  |
  +--> EntityManager/session resources
  |
  +--> JDBC connection
```

This is why code running on the same thread can participate in the current transaction.

But:

```java
new Thread(() -> {
    repository.save(...);
}).start();
```

does **not** mean the new thread automatically inherits the original transaction.

Spring explicitly documents that thread-bound imperative transactions do not propagate to newly started threads. ([Home][2])

---

# 46. `@Async` + `@Transactional`

This is consequently dangerous to misunderstand.

Suppose:

```java
@Transactional
public void process() {

    asyncService.doSomething();
}
```

with:

```java
@Async
@Transactional
public void doSomething() {
}
```

The asynchronous method runs on another thread.

Therefore it doesn't participate in the caller's thread-bound transaction.

It can have its **own** transaction if its invocation goes through the appropriate Spring proxy and transaction configuration.

This is a common production interview scenario.

---

# 47. Final methods and proxies

Spring's AOP proxy mechanism has constraints.

With class-based proxying, a:

```java
final class
```

cannot be subclassed for proxying, and:

```java
final method
```

cannot be overridden/advised in the usual class-based proxy mechanism. Private methods also cannot be advised because they cannot be overridden. ([Home][4])

So don't assume:

```java
@Transactional
private void doSomething()
```

will behave like a public proxy-intercepted service method.

---

# 48. Interface-based vs class-based proxies

Spring AOP can use:

```text
JDK dynamic proxy
```

or:

```text
CGLIB/class-based proxy
```

Spring's current proxy documentation states that JDK proxies are used when appropriate interfaces are present, while class-based proxies can be used otherwise or when configured. ([Home][4])

For interview purposes:

```text
JDK proxy
    ↓
interface-based

CGLIB
    ↓
subclass-based
```

The important transaction lesson is:

> **The proxy is what intercepts the external invocation.**

---

# 49. `@Transactional` on interface vs implementation

Spring can recognize transactional metadata in several locations, but Spring recommends annotating concrete class methods rather than relying on interface declarations, especially because annotations on interfaces have limitations with different proxy/weaving approaches. ([Home][3])

For clean application code, prefer:

```java
@Service
public class OrderService {

    @Transactional
    public void placeOrder() {
        ...
    }
}
```

rather than making transaction semantics depend on an interface annotation.

---

# 50. Transaction manager

Underneath:

```java
@Transactional
```

Spring needs a transaction manager.

The central imperative abstraction is:

```java
PlatformTransactionManager
```

with operations conceptually like:

```text
getTransaction()
commit()
rollback()
```

Spring provides different implementations for different technologies. ([Home][8])

For example:

```text
JPA
 ↓
JpaTransactionManager

JDBC
 ↓
DataSourceTransactionManager

JTA
 ↓
JtaTransactionManager
```

The exact configuration depends on your application.

---

# 51. Spring Boot and transaction manager

In a typical Spring Boot JPA application, the JPA infrastructure is configured so that repository/service operations can participate in transactions.

You usually just write:

```java
@Transactional
```

rather than manually doing:

```java
transactionManager.begin();
...
transactionManager.commit();
```

That's the power of declarative transaction management.

---

# 52. Declarative vs programmatic transaction management

### Declarative

```java
@Transactional
public void placeOrder() {
    ...
}
```

Spring handles transaction boundaries.

### Programmatic

```java
transactionTemplate.execute(status -> {
    ...
    return result;
});
```

or work directly with a transaction manager.

Spring's documentation generally recommends `TransactionTemplate` for imperative programmatic transaction management when you need explicit control. ([Home][11])

So:

```text
Declarative
    ↓
simple / common

Programmatic
    ↓
more explicit control
```

---

# 53. Why declarative transactions are preferred

Compare:

```java
transactionManager.begin();

try {
    ...
    transactionManager.commit();
} catch (...) {
    transactionManager.rollback();
}
```

with:

```java
@Transactional
public void placeOrder() {
    ...
}
```

The second keeps business logic focused on business behavior.

Spring explicitly highlights declarative transaction management as the commonly preferred model because of its low impact on application code. ([Home][10])

---

# 54. A complete transaction example

Imagine an e-commerce service:

```java
@Service
public class OrderService {

    @Transactional
    public void placeOrder(OrderRequest request) {

        Order order =
                createOrder(request);

        inventoryService.reserve(
                request.productId(),
                request.quantity()
        );

        paymentService.charge(
                request.paymentDetails()
        );

        orderRepository.save(order);
    }
}
```

Conceptually:

```text
                     @Transactional
                           |
                           v
                    BEGIN TRANSACTION
                           |
              +------------+------------+
              |            |            |
              v            v            v
          createOrder   reserve      charge
              |        inventory     payment
              |            |            |
              +------------+------------+
                           |
                           v
                         SAVE
                           |
                           v
                         FLUSH
                           |
                           v
                         COMMIT
```

If payment fails:

```text
payment → exception
       ↓
transaction rollback
       ↓
order + inventory changes rolled back
```

assuming all operations participate in the same transaction and the exception triggers rollback.

---

# 55. Transaction boundaries and external APIs

Now something important.

Suppose:

```java
@Transactional
public void placeOrder() {

    saveOrder();

    callPaymentGateway();

    updateInventory();
}
```

The transaction potentially stays open while you call an external service.

That's often undesirable.

Why?

Because while waiting for the network:

```text
DB transaction still open
DB connection possibly occupied
locks may remain
```

A common architectural principle is:

> **Don't unnecessarily keep database transactions open across slow remote calls.**

Spring itself does not propagate transaction contexts across remote calls. ([Home][10])

This becomes especially important in microservice architectures.

---

# 56. Transaction + remote service ≠ distributed transaction automatically

Suppose:

```text
Service A
   |
   +--> DB transaction
   |
   +--> HTTP call
         |
         v
      Service B
          |
          +--> its own DB
```

A local Spring transaction does not magically make the entire operation one atomic transaction across both services.

You now have:

```text
Service A transaction
Service B transaction
```

and need distributed-systems patterns for cross-service consistency.

We'll later cover things like:

```text
Saga
Outbox
idempotency
eventual consistency
```

when we reach microservices.

---

# 57. `readOnly` isn't a security boundary

This is worth repeating.

Don't assume:

```java
@Transactional(readOnly = true)
```

means:

```text
writes are impossible
```

It communicates intent and may enable optimizations.

The actual semantics are provider/transaction-manager/database dependent. Spring documents `readOnly` as a transaction characteristic and optimization hint rather than a general-purpose access-control mechanism. ([Home][8])

---

# 58. Class-level `@Transactional`

You can put:

```java
@Transactional
@Service
public class UserService {
}
```

Now the transaction configuration acts as a default for methods in that class.

Then override a specific method:

```java
@Transactional(readOnly = true)
public User getUser(...) {
}
```

or:

```java
@Transactional(
    propagation = Propagation.REQUIRES_NEW
)
public void audit(...) {
}
```

The more specific method-level declaration takes precedence over the class-level settings. ([Home][3])

---

# 59. AOP proxy means `new` can break transaction management

Consider:

```java
OrderService service = new OrderService(...);
service.placeOrder();
```

You bypass the Spring container.

Therefore:

```text
no Spring proxy
no transaction interceptor
```

So:

```java
@Transactional
```

is not magic on a plain object instantiated with `new`.

The class needs to be managed by Spring and the call needs to reach it through the transactional infrastructure.

This is the same proxy mental model you've seen with other Spring AOP features.

---

# 60. Transaction method flow

A useful internal flow is:

```text
Caller
   |
   v
Transactional Proxy
   |
   v
TransactionInterceptor
   |
   +--> inspect @Transactional metadata
   |
   +--> TransactionManager.getTransaction(...)
   |
   v
Target method
   |
   +--> repository calls
   |
   +--> JPA EntityManager
   |
   +--> flush
   |
   v
TransactionInterceptor
   |
   +--> success → commit
   |
   +--> rollback rule matches → rollback
```

Spring's declarative transaction implementation is built around precisely this AOP + interceptor + transaction manager model. ([Home][2])

---

# 61. A transaction does not mean every operation immediately hits the database

Connect this with the previous topic.

Inside:

```java
@Transactional
public void updateUser(Long id) {

    User user = repository.findById(id).orElseThrow();

    user.setName("Alice");
}
```

you might have:

```text
Transaction
    |
    +--> Persistence Context
             |
             +--> User managed
             |
             +--> name changed
             |
             +--> dirty checking
```

Then:

```text
flush
    ↓
UPDATE SQL
    ↓
commit
```

So transaction management and persistence-context management work together.

---

# 62. `flush()` vs rollback

Suppose:

```java
@Transactional
public void process() {

    user.setName("Alice");

    entityManager.flush();

    throw new RuntimeException();
}
```

You might see:

```sql
UPDATE users ...
```

before the method ends.

Then:

```text
RuntimeException
     ↓
rollback
```

The transaction can still roll back because:

```text
flush ≠ commit
```

That's why seeing SQL in the logs does **not** mean the transaction has successfully committed.

---

# 63. Isolation vs propagation — don't mix them up

This is one of the easiest interview questions to get wrong.

### Propagation

Deals with:

```text
transaction A calls transaction B
```

Question:

> Join? Suspend? Create new? Savepoint?

### Isolation

Deals with:

```text
transaction A ↔ transaction B
```

Question:

> How much can concurrent transactions see of one another's work?

Think:

```text
Propagation = transaction structure
Isolation   = transaction visibility/concurrency
```

Memorize that.

---

# 64. Rollback vs isolation

Another distinction:

```text
Rollback
    ↓
What happens after failure?

Isolation
    ↓
How concurrent transactions interact?
```

Don't mix these.

---

# 65. Classic interview scenario #1

```java
@Transactional
public void methodA() {
    methodB();
}
```

and:

```java
@Transactional
public void methodB() {
    ...
}
```

Assuming both are called through the same transactional proxy structure and use the default propagation:

```text
methodA → starts TX
methodB → joins TX
```

One physical transaction.

Answer:

> `REQUIRED` joins the existing transaction by default.

---

# 66. Classic interview scenario #2

```java
@Transactional
public void methodA() {
    methodB();
}
```

where B uses:

```java
@Transactional(propagation = Propagation.REQUIRES_NEW)
```

Answer:

```text
A starts TX #1
B suspends TX #1
B starts TX #2
B completes
TX #1 resumes
```

Two physical transactions.

---

# 67. Classic interview scenario #3

```java
@Transactional
public void methodA() {
    try {
        methodB();
    } catch (Exception e) {
        // ignore
    }
}
```

B is `REQUIRED` and throws a runtime exception.

What can happen?

```text
B failure
   ↓
shared transaction marked rollback-only
   ↓
A catches exception
   ↓
A reaches commit
   ↓
UnexpectedRollbackException
```

Exactly why? Because the inner scope participated in the same transaction and marked it rollback-only. ([Home][6])

---

# 68. Classic interview scenario #4

```java
@Transactional
public void outer() {

    updateUser();

    new Thread(() -> {
        updateAudit();
    }).start();
}
```

Does the new thread automatically participate in the same transaction?

No.

Thread-bound imperative Spring transactions don't automatically propagate to newly started threads. ([Home][2])

---

# 69. Classic interview scenario #5

```java
@Service
public class PaymentService {

    public void outer() {
        inner();
    }

    @Transactional
    public void inner() {
        ...
    }
}
```

Does `inner()` get transactional behavior?

Not in default proxy mode, because this is self-invocation.

Spring explicitly documents that only external calls through the proxy are intercepted in proxy mode. ([Home][3])

---

# 70. Classic interview scenario #6

```java
@Transactional
public void method() throws IOException {
    save();
    throw new IOException();
}
```

Will it roll back?

By default:

```text
checked IOException
    ↓
not automatic rollback
```

Unless you configure:

```java
@Transactional(rollbackFor = IOException.class)
```

Spring's default rollback rules distinguish unchecked exceptions/errors from checked exceptions. ([Home][3])

---

# 71. Classic interview scenario #7

```java
@Transactional(readOnly = true)
public void method() {
    ...
}
```

Does this guarantee writes will fail?

No.

It's transactional metadata expressing read-only intent and may enable optimizations. Exact behavior depends on the underlying infrastructure. ([Home][8])

---

# 72. Classic interview scenario #8

```java
@Transactional
public void method() {
    ...
}

method();
```

from inside a manually instantiated object:

```java
new SomeService().method();
```

Will Spring transaction handling happen?

No.

You bypassed the Spring-managed proxy/container.

---

# 73. A strong production-style service

```java
@Service
public class TransferService {

    private final AccountRepository accountRepository;

    public TransferService(
            AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Transactional
    public void transfer(
            Long sourceId,
            Long targetId,
            BigDecimal amount) {

        Account source =
                accountRepository.findById(sourceId)
                        .orElseThrow();

        Account target =
                accountRepository.findById(targetId)
                        .orElseThrow();

        source.debit(amount);
        target.credit(amount);
    }
}
```

Notice:

```text
No explicit begin
No explicit commit
No explicit rollback
No manual EntityManager update
```

Spring + JPA handle the transactional/persistence infrastructure.

Conceptually:

```text
transfer()
    ↓
BEGIN
    ↓
load source
load target
modify source
modify target
    ↓
dirty checking
    ↓
flush
    ↓
COMMIT
```

---

# 74. What if `target.credit()` throws?

Suppose:

```java
target.credit(amount);
```

throws:

```java
InsufficientBalanceException
```

If it's a runtime exception and no rollback rule overrides it:

```text
exception
   ↓
transaction interceptor
   ↓
rollback
```

No partial commit.

The entire service transaction is rolled back.

---

# 75. Transaction boundary and checked business exceptions

Suppose you use:

```java
public class BusinessException extends Exception {
}
```

and:

```java
@Transactional
public void process() throws BusinessException {
    ...
    throw new BusinessException();
}
```

With Spring's default rollback rules:

```text
BusinessException
    ↓
checked
    ↓
no automatic rollback
```

So if your business exception is checked and the desired behavior is rollback, you may need:

```java
@Transactional(rollbackFor = BusinessException.class)
```

or a broader configured rollback rule.

---

# 76. Current Spring note

The current stable Spring Framework line is **7.0.9**. Spring also provides a newer configurable default rollback mechanism in which applications can opt into rollback for all exceptions, including checked exceptions, through transaction-management configuration such as `rollbackOn=ALL_EXCEPTIONS`; however, the ordinary `@Transactional` defaults remain important interview knowledge unless the application has deliberately changed them. ([Home][3])

For interview questions, answer the default behavior first, then mention that rollback rules can be customized.

---

# 77. Multiple transaction managers

Some applications have:

```text
Order DB
Payment DB
Audit DB
```

with separate transaction managers.

Spring allows selecting a transaction manager through:

```java
@Transactional("order")
```

or:

```java
@Transactional(transactionManager = "orderTransactionManager")
```

Spring documents the `value`/`transactionManager` attribute for choosing among multiple managers. ([Home][3])

This does not mean one local transaction automatically spans multiple independent transaction managers.

That's a much deeper distributed-transaction problem.

---

# 78. Transaction name

An advanced detail:

Declarative transaction names are generated from the fully qualified class and method name, such as:

```text
com.example.OrderService.placeOrder
```

Spring notes that explicit control over this transaction name is not currently exposed in declarative transaction configuration. ([Home][3])

You probably won't be asked this, but it's useful advanced knowledge.

---

# 79. `@Transactional` on tests

Spring's TestContext framework also supports transactions on test methods, and transactional tests roll back by default in the Spring test environment unless configured otherwise. ([Home][12])

That's why you often see:

```java
@Transactional
@SpringBootTest
class UserRepositoryTest {
}
```

and the database changes disappear after the test.

This is useful to know when you start writing integration tests.

---

# 80. The full picture: Spring + JPA

Now let's combine the last two topics.

Suppose:

```java
@Transactional
public void updateInvoice(Long id) {

    Invoice invoice =
            invoiceRepository.findById(id)
                    .orElseThrow();

    invoice.setStatus(PAID);
}
```

Internally:

```text
             Spring Proxy
                  |
                  v
          TransactionInterceptor
                  |
                  v
          BEGIN TRANSACTION
                  |
                  v
        InvoiceService.updateInvoice()
                  |
                  v
        invoiceRepository.findById()
                  |
                  v
             EntityManager
                  |
                  v
          Persistence Context
                  |
                  v
         Managed Invoice entity
                  |
                  v
         invoice.setStatus(PAID)
                  |
                  v
            Dirty Checking
                  |
                  v
                flush
                  |
                  v
              UPDATE SQL
                  |
                  v
               COMMIT
```

This is the architecture you should now be able to explain from end to end.

---

# 81. The three concepts you must never confuse

### Persistence Context

```text
Which entities are being managed?
```

### Transaction

```text
Which operations should succeed/fail as one unit?
```

### Flush

```text
When are persistence-context changes synchronized with DB?
```

So:

```text
Persistence Context
        +
Transaction
        +
Flush
```

work together, but they are **not the same thing**.

---

# 82. The interview cheat sheet

### `@Transactional`

```text
Declarative transaction metadata
        ↓
Spring AOP proxy
        ↓
TransactionInterceptor
        ↓
TransactionManager
```

([Home][2])

### Default propagation

```text
REQUIRED
```

([Home][3])

### Default isolation

```text
DEFAULT
```

([Home][3])

### Default read/write mode

```text
read-write
```

### Default rollback

```text
RuntimeException / Error → rollback
checked Exception        → normally no rollback
```

([Home][3])

---

# 83. Propagation cheat sheet

```text
REQUIRED
    ↓
join existing
or create

REQUIRES_NEW
    ↓
suspend existing
create independent

NESTED
    ↓
same physical transaction
use savepoint
```

Then:

```text
SUPPORTS
    ↓
join if present

MANDATORY
    ↓
must already exist

NOT_SUPPORTED
    ↓
suspend transaction

NEVER
    ↓
must not have transaction
```

([Home][5])

---

# 84. Isolation cheat sheet

```text
READ_UNCOMMITTED
    ↓
dirty reads possible

READ_COMMITTED
    ↓
dirty reads prevented

REPEATABLE_READ
    ↓
dirty + non-repeatable prevented

SERIALIZABLE
    ↓
dirty + non-repeatable + phantom prevented
```

([Home][7])

---

# 85. The five Spring transaction traps you absolutely need to know

### Trap 1 — Self invocation

```java
this.inner();
```

doesn't normally go through the transactional proxy.

([Home][3])

### Trap 2 — `flush()` isn't commit

```text
flush → synchronize SQL
commit → complete transaction
```

### Trap 3 — `save()` isn't required for every managed update

```text
managed entity
   ↓
modify
   ↓
dirty checking
```

### Trap 4 — `REQUIRES_NEW` isn't "nested"

```text
REQUIRES_NEW → separate physical transaction
NESTED       → savepoint in same physical transaction
```

([Home][6])

### Trap 5 — checked exception doesn't automatically mean rollback

```text
checked Exception
   ↓
no rollback by default
```

unless rollback rules say otherwise.

([Home][3])

---

# 86. EPAM-style interview questions

You should be able to answer these clearly now:

### Basic

> What is a transaction?

> Why do we need transactions?

> What is `@Transactional`?

> Where should `@Transactional` normally be placed?

### Internals

> How does Spring implement `@Transactional`?

> What is a transaction proxy?

> What is `TransactionInterceptor`?

> What is `PlatformTransactionManager`?

### Propagation

> What is `REQUIRED`?

> What is `REQUIRES_NEW`?

> What is `NESTED`?

> Difference between `REQUIRED` and `REQUIRES_NEW`?

> Difference between `REQUIRES_NEW` and `NESTED`?

> What happens when an inner `REQUIRED` transaction fails?

> What is `UnexpectedRollbackException`?

### Rollback

> Which exceptions trigger rollback by default?

> How do you roll back for a checked exception?

> What is `rollbackFor`?

> What is `noRollbackFor`?

### Isolation

> What is transaction isolation?

> Explain dirty read.

> Explain non-repeatable read.

> Explain phantom read.

> Compare `READ_COMMITTED`, `REPEATABLE_READ`, and `SERIALIZABLE`.

### Proxy traps

> Why doesn't `@Transactional` work during self-invocation?

> What happens when you create a service using `new` instead of Spring?

> Do private/final methods work with proxy-based transactions?

### Practical

> Why put the transaction at the service layer?

> What happens if a transaction calls another microservice?

> Does a transaction propagate to another thread?

> What does `readOnly=true` actually mean?

---

# 87. The answer I'd expect you to give in an interview

If they ask:

### "Explain `@Transactional`."

A strong answer is:

> `@Transactional` provides declarative transaction management in Spring. In the default proxy-based mode, Spring creates transactional proxy behavior around the bean and uses a `TransactionInterceptor` together with a transaction manager to start or join a transaction, then commit or roll it back according to the outcome and rollback rules. The default propagation is `REQUIRED`, the default isolation is `DEFAULT`, and by default runtime exceptions and errors trigger rollback while checked exceptions do not. Because proxy interception is involved, self-invocation does not trigger transactional advice. ([Home][3])

That's an excellent EPAM-level response.

---

# 88. The ultimate mental model

Put everything you've learned together:

```text
                   HTTP REQUEST
                         |
                         v
                    Controller
                         |
                         v
                Spring Transactional
                       Proxy
                         |
                         v
              TransactionInterceptor
                         |
                         v
                 BEGIN / JOIN TX
                         |
                         v
                     Service
                         |
              +----------+----------+
              |                     |
              v                     v
        Repository A          Repository B
              |                     |
              +----------+----------+
                         |
                         v
                  EntityManager
                         |
                         v
                Persistence Context
                         |
                  managed entities
                         |
                  dirty checking
                         |
                         v
                       FLUSH
                         |
                         v
                        SQL
                         |
                         v
                      Database
                         |
                         v
                 COMMIT / ROLLBACK
```

And when another transaction is involved:

```text
            TRANSACTION A
                 |
        propagation rules
                 |
       +---------+---------+
       |                   |
    REQUIRED          REQUIRES_NEW
       |                   |
   join A             suspend A
                           |
                         TX B
                           |
                     commit/rollback
                           |
                        resume A
```

That is the core architecture.

---

# What you should know cold

Before moving on, make sure these are genuinely clear:

```text
@Transactional
AOP proxy
TransactionInterceptor
PlatformTransactionManager

transaction boundary
service-layer transaction

Propagation
    REQUIRED
    REQUIRES_NEW
    NESTED
    SUPPORTS
    MANDATORY
    NOT_SUPPORTED
    NEVER

Isolation
    DEFAULT
    READ_UNCOMMITTED
    READ_COMMITTED
    REPEATABLE_READ
    SERIALIZABLE

Dirty read
Non-repeatable read
Phantom read

rollback rules
rollbackFor
noRollbackFor

UnexpectedRollbackException

readOnly
timeout

self-invocation
proxy bypass
final/private methods
new object vs Spring bean

flush vs commit

thread-bound transaction
transaction + async
transaction + remote call
```

### The one sentence to permanently remember

> **Spring's `@Transactional` is declarative transaction metadata implemented through AOP/proxy infrastructure; the transaction manager establishes the transaction boundary, propagation controls how nested transactional calls participate, isolation controls concurrent visibility, rollback rules determine what happens on failure, and JPA's persistence context/dirty checking operates inside that transaction to synchronize entity changes with the database.** ([Home][2])

**Next topic: JPA Querying in depth — JPQL vs native SQL, derived queries, `@Query`, projections, pagination, sorting, `Specification`, Criteria API, bulk updates, and how Hibernate turns these into SQL.**

[1]: https://docs.spring.io/spring-framework/reference/data-access/transaction.html?utm_source=chatgpt.com "Transaction Management :: Spring Framework"
[2]: https://docs.spring.io/spring/reference/7.0-SNAPSHOT/data-access/transaction/declarative/tx-decl-explained.html?utm_source=chatgpt.com "Understanding the Spring Framework’s Declarative Transaction Implementation :: Spring Framework"
[3]: https://docs.spring.io/spring/reference/7.0-SNAPSHOT/data-access/transaction/declarative/annotations.html?utm_source=chatgpt.com "Using @Transactional :: Spring Framework"
[4]: https://docs.spring.io/spring-framework/reference/core/aop/proxying.html?utm_source=chatgpt.com "Proxying Mechanisms :: Spring Framework"
[5]: https://docs.spring.io/spring-framework/docs/7.0.0-M7/javadoc-api/org/springframework/transaction/annotation/Propagation.html?utm_source=chatgpt.com "Propagation (Spring Framework 7.0.0-M7 API)"
[6]: https://docs.spring.io/spring/reference/7.1/data-access/transaction/declarative/tx-propagation.html?utm_source=chatgpt.com "Transaction Propagation :: Spring Framework"
[7]: https://docs.spring.io/spring-framework/docs/7.0.0-M6/javadoc-api/org/springframework/transaction/annotation/Isolation.html?utm_source=chatgpt.com "Isolation (Spring Framework 7.0.0-M6 API)"
[8]: https://docs.spring.io/spring-framework/reference/7.0-SNAPSHOT/data-access/transaction/strategies.html?utm_source=chatgpt.com "Understanding the Spring Framework Transaction Abstraction :: Spring Framework"
[9]: https://docs.spring.io/spring/reference/7.0-SNAPSHOT/data-access/transaction/declarative/rolling-back.html?utm_source=chatgpt.com "Rolling Back a Declarative Transaction :: Spring Framework"
[10]: https://docs.spring.io/spring-framework/reference/7.1/data-access/transaction/declarative.html?utm_source=chatgpt.com "Declarative Transaction Management :: Spring Framework"
[11]: https://docs.spring.io/spring-framework/reference/7.0-SNAPSHOT/data-access/transaction/programmatic.html?utm_source=chatgpt.com "Programmatic Transaction Management :: Spring Framework"
[12]: https://docs.spring.io/spring/reference/7.0-SNAPSHOT/testing/annotations/integration-spring/annotation-rollback.html?utm_source=chatgpt.com "@Rollback :: Spring Framework"

