# Course: Locking in High-Level System Design (HLD)

PostgreSQL, Redis, distributed systems, concurrency control, and production implementation

This course will go from the fundamentals to database internals and distributed locking, with a focus on understanding why a locking mechanism exists, how it works internally, when to use it, and how to explain it in system design interviews.

We'll proceed chapter by chapter. I'll cover one chapter at a time, and you can type `next` to continue.

## Course roadmap

Chapter 1 — Foundations of locking

Concurrency, race conditions, critical sections, lost updates, and why locking is necessary.

Starting now

Chapter 2 — Pessimistic locking

Shared and exclusive locks, PostgreSQL row locks, table locks, deadlocks, MVCC, and SELECT FOR UPDATE.

Chapter 3 — Optimistic locking

Version columns, compare-and-swap, conditional updates, retries, and conflict detection.

Chapter 4 — PostgreSQL concurrency internals

MVCC, snapshots, isolation levels, row-lock internals, Serializable transactions, predicate locks, and lock monitoring.

Chapter 5 — Redis locking

SET NX, expiration, atomic Lua scripts, token ownership, lease expiry, Redlock, and failure scenarios.

Chapter 6 — Distributed locking

Multiple application instances, leases, fencing tokens, split-brain risks, ZooKeeper, etcd, and coordination services.

Chapter 7 — Locking alternatives

Atomic operations, conditional writes, queues, partitioning, idempotency, and lock-free approaches.

Chapter 8 — HLD and production use cases

Seat booking, inventory, bank transfers, job scheduling, payment processing, and choosing the right strategy.

Chapter 9 — Interview questions and trade-offs

Common misconceptions, failure scenarios, scalability trade-offs, and interview-ready answers.

# Chapter 1 — Foundations of locking

## 1. What is locking?

A lock is a coordination mechanism that controls which operations can access or modify a shared resource concurrently.

The resource could be:

- A row in a PostgreSQL table.
- An inventory item.
- A Redis key.
- A file.
- A scheduled job.
- A shared object in memory.

Consider an online store with one item left in stock.

Two customers try to purchase it at almost the same time.

[5.4. Exclusive Control — TERASOLUNA Server Framework for Java (5.x) Development Guideline 5.0.0.RELEASE documentation](https://images.openai.com/static-rsc-4/BoYL9IHnEzqmvstqsn5kB8lZSovmpIl6hfFB-dTA2ecEOwNPinZNKnOCuqzVXEAEN-ExFnSuV_DrZE2gYn7LqwHEXJLs1T5X7eZnr2oFXZfGcNtRtghRpkPWjuMLrDq-DCQE_KhfI_c0Xc-pYtKUFZumXMBl2u4xSQhW8Xyn9qg?purpose=inline)

[terasolunaorg.github.io](https://terasolunaorg.github.io/guideline/5.0.0.RELEASE/en/ArchitectureInDetail/ExclusionControl.html?utm_source=chatgpt.com)

The desired business rule is:

> If there is only one item in stock, at most one customer should successfully purchase it.

The challenge is not that either request is incorrect. Both requests can be individually valid. The problem is that their operations overlap in time.

A locking mechanism can coordinate those operations so that they don't both make conflicting decisions based on the same shared state.

However, locking is only one way to solve concurrency problems. Atomic updates, database constraints, optimistic concurrency control, and serializable transactions can often solve them with less contention.

That distinction is important throughout this course.

## 2. Why do we need locking?

Let's start with a concrete example.

Suppose the database contains this inventory record:

| product_id | stock |
| ---------- | ----- |
| 101        | 1     |

Two requests arrive:

- Request A: Customer Alice purchases one item.
- Request B: Customer Bob purchases one item.

Both requests execute the following conceptual logic:

```
1. Read current stock.
2. If stock > 0:
      Reduce stock by 1.
      Confirm purchase.
```

The code appears correct when executed sequentially. But concurrent execution can produce a race condition.

### 2.1 What is a race condition?

A race condition occurs when the correctness of a program depends on the timing or interleaving of concurrent operations.

Consider this execution:

Request A

Request B

Reads stock = 1

—

—

Reads stock = 1

Checks stock > 0

Checks stock > 0

Writes stock = 0

Writes stock = 0

Confirms purchase

Confirms purchase

Incorrect business outcome

Both customers receive confirmation even though only one item existed.

The final stock value is zero, which looks correct. But two purchases were confirmed for one item.

This is a subtle and important point:

The database value can look correct while the business outcome is incorrect.

Concurrency bugs are not always visible by inspecting the final row.

### 2.2 The underlying problem: read, decide, write

Many concurrency problems follow this pattern:

```
READ shared state
      ↓
MAKE a decision
      ↓
WRITE updated state
```

The problem is that another request can change the shared state between your read and your write.

Examples include:

| System         | Shared state       | Possible bug                     |
| -------------- | ------------------ | -------------------------------- |
| E-commerce     | Inventory quantity | Overselling                      |
| Ticket booking | Seat availability  | Two bookings for one seat        |
| Banking        | Account balance    | Spending the same funds twice    |
| Job scheduler  | Job status         | Two workers process the same job |
| Coupon service | Redemption count   | Exceeding the redemption limit   |
| API quota      | Request count      | Exceeding a limit                |

Not every example needs a lock. Each needs a concurrency-control strategy appropriate to its correctness requirements.

## 3. A second problem: lost updates

Suppose an account has a balance of ₹1,000.

Two independent requests arrive:

- Request A adds ₹100.
- Request B adds ₹200.

Both use application-side read-modify-write logic.

| Step | Request A         | Request B         |
| ---- | ----------------- | ----------------- |
| 1    | Reads ₹1,000      |                   |
| 2    |                   | Reads ₹1,000      |
| 3    | Calculates ₹1,100 |                   |
| 4    |                   | Calculates ₹1,200 |
| 5    | Writes ₹1,100     |                   |
| 6    |                   | Writes ₹1,200     |

Final balance: ₹1,200.

Expected balance: ₹1,300.

Request A's update was lost because Request B overwrote its result.

This is called a lost update.

Notice the difference from the inventory example:

- Inventory: two operations incorrectly succeed.
- Lost update: one operation's effect disappears.

Both arise from unsafe concurrent access to shared state.

## 4. What exactly are we protecting?

Before selecting a locking strategy, identify three things.

### 4.1 The shared resource

What is being accessed or changed?

Examples:

- A particular inventory row.
- One seat for one show.
- An account balance.
- A Redis key representing a scheduled job.

### 4.2 The invariant

An invariant is a rule that must remain true for the system to be correct.

Examples:

\\[ \text{stock} \geq 0 \\]

\\[ \text{successful bookings for a seat} \leq 1 \\]

\\[ \text{balance} \geq 0 \\]

\\[ \text{successful redemptions} \leq \text{coupon limit} \\]

Some invariants are local to one row. Others span multiple rows or even multiple services.

This matters because the scope of the invariant influences the correct solution. A lock on one inventory row cannot automatically protect a business rule involving several independent rows or services.

### 4.3 The critical section

A critical section is the part of an operation that must be coordinated with other operations to preserve correctness.

For example:

```
Check inventory
      +
Reserve inventory
      +
Record the reservation
```

If checking inventory and reserving it must behave as one logical operation, protecting only the final write may be insufficient.

The right solution must coordinate the relevant state transition, not merely one line of code.

## 5. What does a lock guarantee?

A lock generally provides some form of exclusion or coordination among operations that follow its rules.

For example, with an exclusive lock:

```
Request A acquires lock
        ↓
Request A performs protected operation
        ↓
Request B waits
        ↓
Request A releases lock
        ↓
Request B proceeds
```

The exact guarantee depends on the lock mechanism.

A lock does not automatically guarantee all of the following:

- The entire business transaction is atomic.
- Changes survive a crash.
- Every process respects the lock.
- A lock cannot expire while its owner is still running.
- Deadlocks are impossible.
- The system is globally consistent across databases and services.

Those are separate properties that must be considered in system design.

For example, a PostgreSQL transaction can combine row locking with atomic database changes. A Redis lock does not automatically make a separate PostgreSQL transaction safe.

## 6. Locking versus atomic operations

This is one of the most useful distinctions for HLD interviews.

Sometimes you don't need to lock a resource explicitly. You can let the database perform the check and update as one atomic operation.

Consider this PostgreSQL query:

```
UPDATE inventory
SET stock = stock - 1
WHERE product_id = 101
  AND stock > 0
RETURNING stock;
```

The database performs the conditional update as a single statement.

If two requests compete for the last item, PostgreSQL coordinates conflicting row updates. Under the usual `READ COMMITTED` isolation level, a waiting update rechecks the applicable condition against the updated row.

Only one request can decrement stock from one to zero. The other finds that `stock > 0` is no longer true and updates zero rows.

Your application can interpret the result as:

- One returned row: reservation succeeded.
- No returned row: insufficient stock or no matching product.

This is often simpler and safer than performing a separate read, checking the value in application code, and then writing a new value.

Important: This statement atomically updates the inventory row. If the operation also inserts an order, writes a payment record, or changes other database rows, you may still need a transaction to make those database changes succeed or fail together.

### When to prefer an atomic operation

Prefer it when:

- The business rule can be expressed in one database statement.
- You only need to update a specific row or a small, well-defined set of rows.
- You want to minimize lock duration and application-side coordination.

Use explicit locking or another transaction-level strategy when the decision requires more complex reads, multiple steps, or coordination that cannot be expressed safely as a conditional write.

## 7. The main families of concurrency control

## Pessimistic locking

Assume conflicts are possible and acquire a lock before performing the protected work.

Example: PostgreSQL `SELECT ... FOR UPDATE`.

## Optimistic locking

Allow concurrent work, detect whether the data changed, and reject or retry conflicting updates.

Example: a version column and conditional `UPDATE`.

## Atomic operations

Express the state transition as one indivisible operation rather than splitting it into application-side steps.

Example: decrement inventory only if stock is positive.

## Serializable transactions

Require concurrent transactions to behave as though they ran in some serial order. The database may abort transactions that cannot safely be serialized.

Example: complex database invariants spanning multiple rows.

## Distributed locking

Coordinate independent processes that need exclusive access to a resource, typically using a shared coordination service.

Example: Redis locks or coordination using etcd/ZooKeeper.

These categories overlap. For example, PostgreSQL can use pessimistic row locks inside a serializable transaction. A conditional update can also be used as an optimistic concurrency-control technique.

The important question is not "Which lock is best?" It is:

What correctness guarantee do I need, and what is the simplest mechanism that provides it?

## 8. Locking has a cost

Locks solve concurrency problems, but they can introduce other problems.

### Contention

Many requests need the same resource, so they spend time waiting.

### Deadlocks

Transaction A holds resource X and waits for Y, while Transaction B holds Y and waits for X.

### Reduced throughput

A heavily contended lock can serialize requests that could otherwise run concurrently.

### Operational complexity

Distributed locks introduce ownership, expiration, crash recovery, and stale-owner concerns.

A good system design therefore tries to:

- Lock the smallest necessary resource.
- Hold locks for the shortest necessary time.
- Avoid network calls while holding database locks.
- Use database constraints for invariants they can enforce.
- Retry only errors that are genuinely transient.
- Design operations to be idempotent where appropriate.

## 9. Interview-ready answer: What is locking and why do we need it?

A strong answer would be:

> Locking is a concurrency-control mechanism used to coordinate access to shared resources when multiple operations can execute at the same time. Without appropriate coordination, concurrent operations can produce race conditions, lost updates, double bookings, or inconsistent business outcomes.
>
> For example, in an inventory system, two requests might both read that one item is available and then both confirm a purchase. We can prevent this with pessimistic locking, optimistic concurrency control, or an atomic conditional update, depending on the workflow.
>
> In system design, I first identify the invariant that must be preserved, then choose the simplest mechanism that enforces it. I also consider contention, transaction boundaries, deadlocks, retries, and whether the resource is confined to one database or shared across distributed services.

This answer is stronger than simply saying, "We use locks to prevent multiple threads from accessing a resource," because concurrency problems can occur across multiple application instances, not just threads in one process.

## 10. Check your understanding

Scenario: An inventory row has `stock = 1`. Two application instances concurrently execute a read, check, and write sequence. Each reads `stock = 1` before either writes.

Which statement is correct?

Both requests may confirm a purchase, even if the final stock value is zero.

The database will always detect that the application confirmed two purchases.

Using two application instances automatically prevents the race condition.

A lock is always required, even if the database can perform an atomic conditional update.

Check answer

### Correct!

Both requests can act on stale reads unless the operation is properly coordinated. A final stock value of zero does not prove that only one purchase was confirmed. An atomic conditional update is one way to solve this without an explicit application-managed lock.

When you're ready, type `next` for Chapter 2: Pessimistic locking — shared vs exclusive locks, PostgreSQL row-level locking, `SELECT FOR UPDATE`, internal behavior, deadlocks, and production implementation patterns.

