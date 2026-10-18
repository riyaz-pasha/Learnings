# Chapter 9: Locking — Interview Questions and HLD Design Trade-offs

This is the final chapter of the course. We'll connect the concepts from PostgreSQL, Redis, optimistic and pessimistic locking, and distributed systems into answers you can confidently explain in a system design or backend engineering interview.

The goal is not just to memorize definitions. For each question, understand what problem the technique solves, how it works, what guarantees it provides, and when you would choose it in production.

## 1. Pessimistic locking vs. optimistic locking

Interview question: What is the difference between optimistic and pessimistic locking? When would you use each?

### Model answer

Pessimistic locking assumes concurrent operations are likely to conflict. It acquires a lock before modifying the shared resource, making competing transactions wait or fail.

Optimistic locking assumes conflicts are relatively uncommon. Multiple transactions can read and work with the same data, but a write succeeds only if the data has not changed since it was read.

| Aspect                                   | Pessimistic                              | Optimistic                                   |
| ---------------------------------------- | ---------------------------------------- | -------------------------------------------- |
| Basic approach                           | Lock before modification                 | Detect conflicts during modification         |
| Conflict handling                        | Wait, fail, or skip                      | Reject stale update, then resolve            |
| Typical implementation                   | PostgreSQL `SELECT ... FOR UPDATE`       | Version column, `@Version`, CAS              |
| Best suited for                          | High contention, short critical sections | Low or moderate contention                   |
| Main cost                                | Lock waits and possible deadlocks        | Failed writes and possible retries           |
| Common failure                           | Deadlock, timeout                        | Optimistic lock conflict                     |
| External API calls while protecting data | Avoid holding DB locks across calls      | Still requires safe handling of side effects |

### Pessimistic locking example

Suppose two users try to reserve the same seat.

```
BEGIN;

SELECT status
FROM seats
WHERE show_id = 100
  AND seat_id = 25
FOR UPDATE;

-- Check availability.
-- Update the seat and create the booking.

COMMIT;
```

The second transaction must wait for the first transaction's conflicting row lock to be released, unless configured to fail or use a different strategy.

### Optimistic locking example

```
UPDATE products
SET price = 120,
    version = version + 1
WHERE product_id = 10
  AND version = 5;
```

If one row is updated, the operation succeeded. If zero rows are updated, the version did not match, or the row does not exist.

The application must determine whether to reject the operation, reload the latest data, or safely retry.

Important interview point: Optimistic locking does not eliminate database locks. PostgreSQL still coordinates concurrent writes internally. The difference is that optimistic locking does not deliberately acquire an explicit row lock in advance to reserve the resource.

How I would choose: For a frequently contested seat that must be reserved consistently, I would consider pessimistic locking or an atomic conditional update. For an ordinary profile edit, I would generally prefer optimistic locking.

## 2. Does PostgreSQL use locks if it supports MVCC?

Interview question: PostgreSQL supports MVCC. Why do we still need locks?

### Model answer

MVCC stands for Multi-Version Concurrency Control. PostgreSQL maintains multiple row versions so that ordinary readers can access a consistent snapshot without necessarily blocking concurrent writers.

However, MVCC does not eliminate locks.

PostgreSQL still needs coordination for operations such as:

- Updating or deleting the same row concurrently.
- Preventing conflicting modifications to a resource.
- Enforcing unique constraints.
- Performing schema changes that require stronger table locks.
- Implementing explicit pessimistic locking using `SELECT ... FOR UPDATE`.

Consider two transactions updating the same account balance.

```
UPDATE accounts
SET balance = balance - 100
WHERE account_id = 1;
```

PostgreSQL coordinates the conflicting updates. One transaction may need to wait for the other, and the result depends on the transaction isolation level and whether the row still qualifies for the update.

Meanwhile, a normal `SELECT` can often read an appropriate visible row version without waiting for the update to finish.

### What exactly does `SELECT FOR UPDATE` lock?

```
SELECT *
FROM accounts
WHERE account_id = 1
FOR UPDATE;
```

It acquires a row-level lock on the selected row and a table-level `ROW SHARE` lock on the table. The row lock prevents conflicting operations from modifying or locking that row incompatibly until the transaction ends.

It does not lock every row in the table.

The lock is normally held until the transaction commits or rolls back.

### A useful distinction

| Concept               | Purpose                                                            |
| --------------------- | ------------------------------------------------------------------ |
| MVCC                  | Controls which row versions transactions can see                   |
| Row locks             | Coordinate conflicting access to particular rows                   |
| Table locks           | Coordinate operations at the table level                           |
| Transaction isolation | Defines the visibility and concurrency guarantees of a transaction |

Interview takeaway: MVCC controls visibility; locks coordinate conflicting operations. They work together rather than replace each other.

## 3. Explain PostgreSQL isolation levels and write skew

Interview question: What is the difference between `READ COMMITTED`, `REPEATABLE READ`, and `SERIALIZABLE`?

### Model answer

Transaction isolation determines what a transaction can observe when other transactions are concurrently reading and writing data.

| Isolation level   | PostgreSQL behavior                                                       | Important limitation                                         |
| ----------------- | ------------------------------------------------------------------------- | ------------------------------------------------------------ |
| `READ COMMITTED`  | Each statement generally gets a fresh snapshot                            | Repeated reads can see different committed data              |
| `REPEATABLE READ` | Transaction uses a consistent snapshot                                    | Write skew and other serialization anomalies can still occur |
| `SERIALIZABLE`    | Detects unsafe dependency patterns and aborts transactions when necessary | Transactions may need to be retried                          |

PostgreSQL's `READ UNCOMMITTED` behaves like `READ COMMITTED`; PostgreSQL does not allow dirty reads.

### What is write skew?

Imagine a hospital needs at least one doctor on call.

Initially:

| Doctor | On call |
| ------ | ------- |
| Alice  | Yes     |
| Bob    | Yes     |

Two transactions run concurrently:

- Transaction A checks that two doctors are on call, then marks Alice off call.
- Transaction B checks the same snapshot, then marks Bob off call.

Each transaction sees another doctor on call, so each believes its update is safe.

The final state could be:

| Doctor | On call |
| ------ | ------- |
| Alice  | No      |
| Bob    | No      |

The business invariant has been violated.

### How can we prevent it?

Depending on the design:

1. Use `SERIALIZABLE` and retry transactions that fail with a serialization error.
2. Lock a shared coordination row that represents the on-call roster.
3. Redesign the data model so the invariant can be enforced by a constraint or atomic state transition.

A row lock on Alice and a separate row lock on Bob would not necessarily solve the problem: the transactions are modifying different rows. The coordination must protect the shared invariant.

Interview takeaway: `REPEATABLE READ` provides a stable snapshot in PostgreSQL, but that does not guarantee every business invariant is preserved. Use `SERIALIZABLE`, appropriate locking, or a stronger data model when the invariant requires it.

## 4. How do you prevent deadlocks?

Interview question: What is a deadlock, and how would you prevent it in a production application?

### Model answer

A deadlock occurs when transactions wait for resources held by one another, creating a cycle in the wait-for graph.

For example:

\#chatgpt-mermaid-\_r_4gt\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_4gt\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_4gt\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_4gt\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_4gt\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4gt\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_4gt\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_4gt\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_4gt\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_4gt\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_4gt\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_4gt\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4gt\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4gt\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_4gt\_ p{margin:0;}#chatgpt-mermaid-\_r_4gt\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4gt\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4gt\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4gt\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_4gt\_ .label text,#chatgpt-mermaid-\_r_4gt\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4gt\_ .node rect,#chatgpt-mermaid-\_r_4gt\_ .node circle,#chatgpt-mermaid-\_r_4gt\_ .node ellipse,#chatgpt-mermaid-\_r_4gt\_ .node polygon,#chatgpt-mermaid-\_r_4gt\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_4gt\_ .rough-node .label text,#chatgpt-mermaid-\_r_4gt\_ .node .label text,#chatgpt-mermaid-\_r_4gt\_ .image-shape .label,#chatgpt-mermaid-\_r_4gt\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_4gt\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_4gt\_ .rough-node .label,#chatgpt-mermaid-\_r_4gt\_ .node .label,#chatgpt-mermaid-\_r_4gt\_ .image-shape .label,#chatgpt-mermaid-\_r_4gt\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_4gt\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_4gt\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4gt\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4gt\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_4gt\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_4gt\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_4gt\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_4gt\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_4gt\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_4gt\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_4gt\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4gt\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4gt\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_4gt\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4gt\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_4gt\_ .icon-shape,#chatgpt-mermaid-\_r_4gt\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_4gt\_ .icon-shape p,#chatgpt-mermaid-\_r_4gt\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_4gt\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_4gt\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_4gt\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_4gt\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_4gt\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_4gt\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_4gt\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_4gt\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_4gt\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4gt\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_4gt\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_4gt\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_4gt\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4gt\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_4gt\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_4gt\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4gt\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_4gt\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_4gt\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4gt\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_4gt\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4gt\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_4gt\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_4gt\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_4gt\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_4gt\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_4gt\_ .node rect,#chatgpt-mermaid-\_r_4gt\_ .node circle,#chatgpt-mermaid-\_r_4gt\_ .node ellipse,#chatgpt-mermaid-\_r_4gt\_ .node polygon,#chatgpt-mermaid-\_r_4gt\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_4gt\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_4gt\_ .node.mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_4gt\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_4gt\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4gt\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}Transaction A holds Row 1Transaction BWaits for Row 2Waits for Row 1

Transaction A holds Row 1 and waits for Row 2. Transaction B holds Row 2 and waits for Row 1. Neither can proceed.

PostgreSQL detects deadlocks and aborts one of the transactions to break the cycle.

### Prevention strategies

1\. Acquire locks in a consistent order.

For example, if transactions need accounts 10 and 20, always lock the smaller account ID first.

```
BEGIN;

SELECT *
FROM accounts
WHERE account_id IN (10, 20)
ORDER BY account_id
FOR UPDATE;

-- Perform transfers.

COMMIT;
```

Consistent ordering reduces deadlocks, although complex queries and other lock types can still introduce them.

2\. Keep transactions short.

Do not wait for user input or call a slow external API while holding database locks.

3\. Lock only what you need.

Avoid unnecessarily locking large numbers of rows.

4\. Retry safely.

PostgreSQL reports a deadlock with SQLSTATE `40P01`. The application can retry the entire transaction in a fresh transaction, using a bounded retry policy and backoff.

For `SERIALIZABLE` transactions, the common serialization failure code is `40001`.

Critical point: Retrying is not a replacement for deadlock prevention. Also, do not blindly retry external side effects such as sending a payment or email. Use idempotency, transactional outbox patterns, or other mechanisms to make those effects safe.

## 5. What is the difference between `NOWAIT` and `SKIP LOCKED`?

Interview question: When would you use each in PostgreSQL?

Both change how a query behaves when it encounters rows with conflicting locks.

### `NOWAIT`

```
SELECT *
FROM seats
WHERE show_id = 100
  AND seat_id = 25
FOR UPDATE NOWAIT;
```

If the required row lock cannot be acquired immediately, PostgreSQL raises an error instead of waiting.

Use it when the application prefers an immediate response, such as telling a user that a seat is currently being processed.

### `SKIP LOCKED`

```
SELECT *
FROM jobs
WHERE status = 'PENDING'
ORDER BY job_id
FOR UPDATE SKIP LOCKED
LIMIT 1;
```

Instead of waiting for locked rows, PostgreSQL skips them and tries to return other eligible rows.

This is useful when multiple workers claim jobs from a database queue.

| Feature                               | `NOWAIT`                    | `SKIP LOCKED`          |
| ------------------------------------- | --------------------------- | ---------------------- |
| Encounters a conflicting row lock     | Fails immediately           | Skips the locked row   |
| Typical use                           | Immediate conflict response | Concurrent job workers |
| Guarantees the oldest job is claimed? | No                          | No                     |
| Guarantees exactly-once processing?   | No                          | No                     |

Important: `SKIP LOCKED` intentionally gives an inconsistent view of the rows, making it suitable for work distribution rather than general-purpose business queries.

It also does not solve abandoned jobs. If a worker crashes after claiming a job, your application needs a recovery policy, such as a lease expiry, claim timeout, or retryable job state.

## 6. How do you implement a safe Redis lock?

Interview question: How would you implement a distributed lock using Redis?

### Model answer

For a basic Redis lock, use an atomic `SET` command with a unique ownership token and an expiry:

```
SET lock:inventory:101 unique-token NX PX 30000
```

- `NX`: Set the key only if it does not already exist.
- `PX 30000`: Expire the key after 30,000 milliseconds.
- `unique-token`: Identifies the specific lock owner.

If the command succeeds, the client acquired the lock. If the key already exists, it did not.

### Why do we need a unique token?

Imagine this sequence:

1. Client A acquires the lock.
2. A pauses for too long and its lock expires.
3. Client B acquires the same lock.
4. Client A resumes and tries to release the lock.

If A simply executes `DEL lock:inventory:101`, it may delete B's lock.

Instead, release the lock only if the stored token matches the owner's token. Perform the comparison and deletion atomically using a Lua script.

```
if redis.call("GET", KEYS[1]) == ARGV[1] then
    return redis.call("DEL", KEYS[1])
else
    return 0
end
```

### Is a TTL sufficient to make the lock safe?

No.

A TTL helps recover from abandoned locks, but it introduces a lease-expiration problem.

Suppose A holds a lock for 30 seconds but pauses for 40 seconds. The lock expires, and B acquires it. When A resumes, both clients may execute their protected operations.

The Redis key prevents a second acquisition only while the key remains valid. It cannot automatically stop a process that previously acquired the lock from continuing to execute code.

### How do fencing tokens help?

A fencing token is a monotonically increasing number attached to each ownership generation.

For example:

- A receives token 41.
- A pauses and its lease expires.
- B receives token 42.
- B writes to the protected resource.
- A resumes and attempts to write using token 41.

The protected resource rejects A's stale operation because it has already accepted token 42.

The important requirement is that the protected resource must enforce fencing tokens. Merely generating tokens does not provide protection.

The token-generation mechanism must also preserve the necessary monotonicity across failures. A simple Redis counter is not automatically a durable, globally safe fencing-token source under every failover configuration.

## 7. Is Redis Redlock always safe?

Interview question: What is Redlock, and what are its limitations?

Redlock is an algorithm designed to acquire a lock across multiple independent Redis instances.

At a high level, a client:

1. Attempts to acquire the lock on multiple independent Redis masters.
2. Requires a majority of instances to grant the lock.
3. Checks whether acquisition completed within the allowed time.
4. Accounts for elapsed time and clock-drift assumptions when calculating the remaining lease.
5. Releases acquired locks if the attempt fails.

For \\(N\\) independent instances, the majority is:

\\[ \left\lfloor \frac{N}{2} \right\rfloor + 1 \\]

For five instances, the majority is three.

### What is the limitation?

Redlock's guarantees depend on assumptions about timing, lease validity, process pauses, and the behavior of the independent instances. Distributed lock safety is subtle, and the algorithm's guarantees have been debated.

A lock that was valid when acquired can expire while the owner is paused. Another client may then acquire the lock, while the original client continues working.

A majority lock does not, by itself, prevent a stale owner from writing to an external resource.

For correctness-critical operations, such as financial ledger updates or inventory invariants, I would prefer to enforce correctness at the authoritative resource using database transactions, constraints, conditional updates, or fencing tokens where appropriate.

Redis locking can still be useful for coordinating work, preventing duplicate expensive computation, or reducing cache stampedes.

## 8. Distributed locks vs. consensus systems

Interview question: Why would you use etcd or ZooKeeper instead of Redis for distributed coordination?

A distributed lock coordinates processes that do not share a single memory space. But the strength of the coordination depends on how the system handles failures, leases, leadership, and network partitions.

| Property                                      | Redis lock                                        | etcd / ZooKeeper-style coordination                          |
| --------------------------------------------- | ------------------------------------------------- | ------------------------------------------------------------ |
| Primary strength                              | Fast, lightweight coordination                    | Stronger coordination semantics using quorum-based protocols |
| Failure handling                              | Depends on configuration and replication behavior | Uses consensus or quorum-based coordination mechanisms       |
| Lease/session support                         | TTL-based expiration                              | Lease or session mechanisms                                  |
| Leader election                               | Can be built with locks                           | Common built-in coordination use case                        |
| Prevents stale external writes automatically? | No                                                | No                                                           |
| Operational complexity                        | Often simpler                                     | Usually more operationally involved                          |

Consensus-backed systems can provide stronger guarantees about authoritative coordination state. For example, a consensus system may prevent two leaders from both being accepted as the current leader by the coordination service.

However, a former leader can still be alive and execute code after losing leadership.

Therefore, for a protected resource, you may also need:

- An ownership epoch or fencing token.
- Resource-side validation of the current ownership generation.
- Idempotency and recovery mechanisms.

Interview takeaway: Stronger coordination helps determine who is allowed to act, but the protected resource must still defend itself against stale operations when correctness requires it.

## 9. Why not use a lock for everything?

Interview question: If locks solve concurrency problems, why do we need atomic updates, optimistic locking, constraints, and idempotency?

Because a lock is a coordination mechanism, not a universal correctness guarantee.

Different problems have simpler and more reliable solutions.

### Example: Inventory

Instead of reading the stock, locking the row, checking the value in application code, and then updating it, use an atomic conditional update:

```
UPDATE inventory
SET stock = stock - 1
WHERE product_id = 101
  AND stock > 0
RETURNING stock;
```

If a row is returned, the decrement succeeded. If no row is returned, no eligible inventory row was updated.

This combines the condition and modification in one database statement.

### Example: Prevent duplicate bookings

Use a unique constraint:

```
ALTER TABLE bookings
ADD CONSTRAINT unique_seat_per_show
UNIQUE (show_id, seat_id);
```

Even if two application instances race, the database will not permit two committed bookings with the same `(show_id, seat_id)` pair.

The application must still handle the uniqueness violation correctly.

### Example: Prevent duplicate payment requests

Use an idempotency key tied to a durable request record and the payment workflow.

A lock alone cannot guarantee that a payment will not be charged twice after timeouts, retries, crashes, or provider responses being lost.

### Decision table

| Requirement                                            | Preferred starting point                                          |
| ------------------------------------------------------ | ----------------------------------------------------------------- |
| Decrement stock if positive                            | Atomic conditional SQL update                                     |
| Prevent duplicate seat bookings                        | Unique constraint, often with a transaction                       |
| Detect stale profile edits                             | Optimistic locking                                                |
| Protect a multi-step DB operation with high contention | Pessimistic row locking                                           |
| Prevent a write-skew invariant violation               | Serializable isolation, coordination lock, or data-model redesign |
| Deduplicate API retries                                | Durable idempotency mechanism                                     |
| Distribute ordered work                                | Queue with partitioning and idempotent consumers                  |
| Coordinate a cluster-wide leader                       | Appropriate distributed coordination system                       |
| Reduce duplicate cache refreshes                       | Single-flight or a distributed lease                              |

The best design often combines multiple mechanisms. For example, a seat-booking system may use a transaction, a unique constraint, a reservation expiry, and payment idempotency. Each solves a different failure mode.

## 10. HLD scenario: Design a movie ticket booking system

Interview question: Multiple users are trying to book the same seat. How would you prevent double booking without holding locks during payment?

### Step 1: Define the invariant

A seat can have at most one active confirmed booking for a given show.

The database must enforce this invariant, not merely the application code.

### Step 2: Introduce a reservation state

A simplified state machine might look like this:

\#chatgpt-mermaid-\_r_4im\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_4im\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_4im\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_4im\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_4im\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4im\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_4im\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_4im\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_4im\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_4im\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_4im\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_4im\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4im\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4im\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_4im\_ p{margin:0;}#chatgpt-mermaid-\_r_4im\_ defs [id$="-barbEnd"]{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4im\_ g.stateGroup text{fill:rgb(31, 78, 148);stroke:none;font-size:10px;}#chatgpt-mermaid-\_r_4im\_ g.stateGroup text{fill:rgb(237, 237, 237);stroke:none;font-size:10px;}#chatgpt-mermaid-\_r_4im\_ g.stateGroup .state-title{font-weight:bolder;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4im\_ g.stateGroup rect{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_4im\_ g.stateGroup line{stroke:rgb(175, 175, 175);stroke-width:1;}#chatgpt-mermaid-\_r_4im\_ .transition{stroke:rgb(175, 175, 175);stroke-width:1;fill:none;}#chatgpt-mermaid-\_r_4im\_ .stateGroup .composit{fill:transparent;border-bottom:1px;}#chatgpt-mermaid-\_r_4im\_ .stateGroup .alt-composit{fill:#e0e0e0;border-bottom:1px;}#chatgpt-mermaid-\_r_4im\_ .state-note{stroke:rgb(58, 132, 63);fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_4im\_ .state-note text{fill:rgb(237, 237, 237);stroke:none;font-size:10px;}#chatgpt-mermaid-\_r_4im\_ .stateLabel .box{stroke:none;stroke-width:0;fill:rgb(9, 23, 44);opacity:0.5;}#chatgpt-mermaid-\_r_4im\_ .edgeLabel .label rect{fill:rgb(9, 23, 44);opacity:0.5;}#chatgpt-mermaid-\_r_4im\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_4im\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_4im\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_4im\_ .edgeLabel .label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4im\_ .label div .edgeLabel{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4im\_ .stateLabel text{fill:rgb(237, 237, 237);font-size:10px;font-weight:bold;}#chatgpt-mermaid-\_r_4im\_ .node circle.state-start{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4im\_ .node .fork-join{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4im\_ .node circle.state-end{fill:rgb(31, 78, 148);stroke:transparent;stroke-width:1.5;}#chatgpt-mermaid-\_r_4im\_ .end-state-inner{fill:transparent;stroke-width:1.5;}#chatgpt-mermaid-\_r_4im\_ .node rect{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_4im\_ .node polygon{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_4im\_ [id$="-barbEnd"]{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4im\_ .statediagram-cluster rect{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_4im\_ .cluster-label,#chatgpt-mermaid-\_r_4im\_ .nodeLabel{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4im\_ .statediagram-cluster rect.outer{rx:5px;ry:5px;}#chatgpt-mermaid-\_r_4im\_ .statediagram-state .divider{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_4im\_ .statediagram-state .title-state{rx:5px;ry:5px;}#chatgpt-mermaid-\_r_4im\_ .statediagram-cluster.statediagram-cluster .inner{fill:transparent;}#chatgpt-mermaid-\_r_4im\_ .statediagram-cluster.statediagram-cluster-alt .inner{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_4im\_ .statediagram-cluster .inner{rx:0;ry:0;}#chatgpt-mermaid-\_r_4im\_ .statediagram-state rect.basic{rx:5px;ry:5px;}#chatgpt-mermaid-\_r_4im\_ .statediagram-state rect.divider{stroke-dasharray:10,10;fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_4im\_ .note-edge{stroke-dasharray:5;}#chatgpt-mermaid-\_r_4im\_ .statediagram-note rect{fill:rgb(48, 48, 48);stroke:rgb(58, 132, 63);stroke-width:1px;rx:0;ry:0;}#chatgpt-mermaid-\_r_4im\_ .statediagram-note rect{fill:rgb(48, 48, 48);stroke:rgb(58, 132, 63);stroke-width:1px;rx:0;ry:0;}#chatgpt-mermaid-\_r_4im\_ .statediagram-note text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4im\_ .statediagram-note .nodeLabel{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4im\_ .statediagram .edgeLabel{color:red;}#chatgpt-mermaid-\_r_4im\_ [id$="-dependencyStart"],#chatgpt-mermaid-\_r_4im\_ [id$="-dependencyEnd"]{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);stroke-width:1;}#chatgpt-mermaid-\_r_4im\_ .statediagramTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4im\_ [data-look="neo"].statediagram-cluster rect{fill:rgb(9, 23, 44);stroke:url(#chatgpt-mermaid-\_r_4im\_-gradient);stroke-width:1;}#chatgpt-mermaid-\_r_4im\_ [data-look="neo"].statediagram-cluster rect.outer{rx:5px;ry:5px;filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4im\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_4im\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_4im\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_4im\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_4im\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4im\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_4im\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_4im\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_4im\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4im\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_4im\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_4im\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4im\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_4im\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_4im\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4im\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_4im\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4im\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}AVAILABLERESERVEDCONFIRMEDPAYMENT_PENDINGReserve seatPayment succeedsReservation expiresPayment initiatedPayment succeedsFailure or expiry handledsafely

Real payment workflows need additional states for late success, refunds, cancellation, and reconciliation. A timeout alone does not prove that a payment failed.

### Step 3: Reserve the seat in a short transaction

One approach is to conditionally change the seat state:

```
UPDATE show_seats
SET status = 'RESERVED',
    reservation_id = :reservation_id,
    reserved_until = now() + interval '5 minutes'
WHERE show_id = :show_id
  AND seat_id = :seat_id
  AND status = 'AVAILABLE'
RETURNING seat_id;
```

If no row is returned, the seat was not available for reservation.

In practice, reservation creation and any related booking records should be coordinated in a transaction. Expiry handling must be designed carefully so an expired reservation cannot later overwrite a newer reservation.

### Step 4: Call the payment provider outside the DB transaction

Do not keep a database row lock open while waiting for a payment provider.

Instead:

1. Commit the reservation.
2. Initiate payment using a stable idempotency key.
3. Record the result through a durable workflow.
4. Confirm the booking if payment succeeds and the reservation is still valid.
5. Handle failures, late responses, and expiration through explicit state transitions and reconciliation.

### Step 5: Enforce the final invariant

A database constraint should prevent duplicate confirmed bookings, and state transitions must verify that the operation still belongs to the relevant reservation.

The exact schema depends on whether expired reservations remain in the same table and whether the system allows booking history. A simple unique constraint over all historical booking rows may be too restrictive; the database model should express the intended active-booking invariant.

### Why not use only Redis locking?

Because Redis lock ownership and payment state are separate concerns. A lock can expire, an application can crash, and a payment provider can respond late. The database and payment workflow need their own durable correctness rules.

Strong interview answer: I would use short database transactions for reservation state transitions, atomic conditional updates and constraints for seat uniqueness, and idempotent payment processing outside the database transaction. I would treat expiration and late payment as explicit workflow states rather than assuming a lock or timeout solves them.

## 11. What metrics would you monitor in production?

Interview question: How would you diagnose a locking problem in a production system?

I would monitor both the database and the application-level coordination mechanism.

### PostgreSQL

| Metric or signal             | What it tells you                                          |
| ---------------------------- | ---------------------------------------------------------- |
| Lock wait duration           | How long transactions spend waiting                        |
| Blocked and blocking queries | Which sessions are involved in contention                  |
| Deadlock count               | Whether transactions are acquiring conflicting locks       |
| Serialization failures       | Whether concurrent transactions are being aborted          |
| Long-running transactions    | Potential lock retention and MVCC cleanup problems         |
| Idle-in-transaction sessions | Connections that may hold locks while doing no useful work |
| Transaction duration         | Whether critical sections are unnecessarily long           |

Useful tools include `pg_stat_activity`, `pg_blocking_pids()`, `pg_locks`, database logs, and query-performance monitoring.

### Redis or distributed coordination

Monitor:

- Lock acquisition success and failure rates.
- Acquisition latency and wait time.
- Lease-renewal failures.
- Lock hold duration relative to lease duration.
- Ownership-token mismatches during release.
- Expired jobs and recovery attempts.
- Failover events and suspected overlapping ownership.

The most important production question is not simply “Are locks working?” It is: Are concurrency conflicts delaying the system, or are failures allowing incorrect state transitions?

## 12. Rapid-fire interview questions

Use these to test whether you can explain the concepts without relying on memorized definitions.

### Q1. Does acquiring a row lock guarantee that my entire business operation is correct?

No. It protects against specified conflicting operations on the locked row. The operation may involve other rows, tables, external services, or business invariants that require additional protection.

### Q2. Does a PostgreSQL row lock survive a transaction commit?

No. The row lock is normally released when the transaction commits or rolls back.

### Q3. Can ordinary PostgreSQL `SELECT` statements block updates?

Usually, ordinary reads and updates do not block each other because of MVCC. However, queries can still interact with table-level locks, DDL, and other operations in ways that cause waits.

### Q4. Does optimistic locking mean no locks are used?

No. It means the application detects stale writes instead of explicitly locking the resource in advance. PostgreSQL still uses internal locking to coordinate writes.

### Q5. Does `SKIP LOCKED` guarantee exactly-once job execution?

No. It helps workers claim different available rows concurrently. Crashes, retries, and duplicate delivery still require recovery and idempotency.

### Q6. Is a Redis lock safe as long as it has a TTL?

Not necessarily. A client can continue executing after its lease expires. Use resource-side safeguards, such as fencing tokens, when stale writes must be rejected.

### Q7. What is the difference between a lock and a lease?

A lock represents exclusive ownership according to a coordination mechanism. A lease grants ownership for a limited time. Expiry enables recovery, but it does not necessarily stop the former owner from executing code.

### Q8. Can `SERIALIZABLE` transactions fail even when the application logic is correct?

Yes. PostgreSQL may abort a transaction to preserve serializable execution. The application must be prepared to retry the entire transaction safely.

### Q9. Why are database constraints important if the application already locks rows?

Constraints enforce invariants at the database boundary. They protect against application bugs, alternative code paths, and concurrent requests that the application did not coordinate correctly.

### Q10. When should I choose a distributed lock?

When multiple independent processes need coordination that cannot be handled adequately by a single database transaction, atomic operation, or queue. Before introducing one, identify the authoritative resource and decide how it will reject stale owners.

### Q11. Can I hold a lock while calling an external API?

Technically, yes, but it is usually a poor design for database transactions. Network calls can be slow or time out, extending lock duration and increasing contention. Prefer durable workflow states, short transactions, and idempotent external operations.

### Q12. What is your first choice for preventing overselling inventory?

I would start with an atomic conditional update or another database-enforced invariant. I would introduce more elaborate locking or distributed coordination only if the actual workflow and contention requirements justified it.

## 13. Final decision framework

When designing a concurrent system, work through these questions in order.

\#chatgpt-mermaid-\_r_4j6\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_4j6\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_4j6\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_4j6\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_4j6\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4j6\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_4j6\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_4j6\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_4j6\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_4j6\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_4j6\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_4j6\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4j6\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4j6\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_4j6\_ p{margin:0;}#chatgpt-mermaid-\_r_4j6\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4j6\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4j6\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4j6\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_4j6\_ .label text,#chatgpt-mermaid-\_r_4j6\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4j6\_ .node rect,#chatgpt-mermaid-\_r_4j6\_ .node circle,#chatgpt-mermaid-\_r_4j6\_ .node ellipse,#chatgpt-mermaid-\_r_4j6\_ .node polygon,#chatgpt-mermaid-\_r_4j6\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_4j6\_ .rough-node .label text,#chatgpt-mermaid-\_r_4j6\_ .node .label text,#chatgpt-mermaid-\_r_4j6\_ .image-shape .label,#chatgpt-mermaid-\_r_4j6\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_4j6\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_4j6\_ .rough-node .label,#chatgpt-mermaid-\_r_4j6\_ .node .label,#chatgpt-mermaid-\_r_4j6\_ .image-shape .label,#chatgpt-mermaid-\_r_4j6\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_4j6\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_4j6\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4j6\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4j6\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_4j6\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_4j6\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_4j6\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_4j6\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_4j6\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_4j6\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_4j6\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4j6\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4j6\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_4j6\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4j6\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_4j6\_ .icon-shape,#chatgpt-mermaid-\_r_4j6\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_4j6\_ .icon-shape p,#chatgpt-mermaid-\_r_4j6\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_4j6\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_4j6\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_4j6\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_4j6\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_4j6\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_4j6\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_4j6\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_4j6\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_4j6\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4j6\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_4j6\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_4j6\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_4j6\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4j6\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_4j6\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_4j6\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4j6\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_4j6\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_4j6\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4j6\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_4j6\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4j6\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_4j6\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_4j6\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_4j6\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_4j6\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_4j6\_ .node rect,#chatgpt-mermaid-\_r_4j6\_ .node circle,#chatgpt-mermaid-\_r_4j6\_ .node ellipse,#chatgpt-mermaid-\_r_4j6\_ .node polygon,#chatgpt-mermaid-\_r_4j6\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_4j6\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_4j6\_ .node.mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_4j6\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_4j6\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4j6\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}Identify the businessinvariantCan one atomic DB operationenforce it?Use conditional update orCASCan a constraint enforce it?Add constraint and handleviolationsWhat concurrency guaranteeis needed?Optimistic locking for stalewritesPessimistic locking forcontested resourcesSerializable isolation fortransaction-wide invariantsQueue or coordinator fordistributed workflowsAdd idempotency, recovery,monitoringYesNoYesNo

Use the diagram as a starting point, not a rigid decision tree. Many production systems combine these techniques.

## Final interview summary

If you remember only these principles, you will have a strong foundation for locking questions in backend and HLD interviews:

1. Start with the invariant. Define exactly what must never become invalid.
2. Prefer the simplest correct mechanism. Atomic SQL updates and database constraints are often better than introducing a distributed lock.
3. Use pessimistic locking when contention justifies early coordination. Keep transactions short and acquire locks consistently.
4. Use optimistic locking when stale writes are the main concern. Detect conflicts and resolve them deliberately.
5. Understand PostgreSQL MVCC and isolation levels. A stable snapshot is not the same as preventing every serialization anomaly.
6. Treat Redis locks as leases, not magical ownership guarantees. Expiration does not stop a stale client from continuing to work.
7. Protect the authoritative resource. Use constraints, conditional state transitions, or fencing validation where necessary.
8. Design for retries and failures. Idempotency, durable state transitions, and reconciliation are essential for payments and distributed jobs.
9. Monitor contention and correctness. Lock waits, deadlocks, serialization failures, expired leases, and abandoned jobs all matter.

A strong closing statement for an interview:

> I choose a concurrency-control strategy based on the invariant I need to protect, the expected contention, and the failure model. I first consider atomic database operations and constraints. If the operation needs more coordination, I choose optimistic or pessimistic locking based on conflict frequency and cost. For distributed workflows, I account for lease expiry, stale owners, retries, and idempotency, and I make sure the authoritative resource can enforce correctness rather than relying on the lock alone.

That completes the course on pessimistic locking, optimistic locking, PostgreSQL concurrency, Redis locks, distributed coordination, and locking trade-offs in high-level system design.

