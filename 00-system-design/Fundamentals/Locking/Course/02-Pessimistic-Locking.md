# Chapter 2 — Pessimistic Locking

Focus: PostgreSQL, shared and exclusive locks, `SELECT ... FOR UPDATE`, transaction boundaries, internal behavior, deadlocks, and production implementation.

Pessimistic locking is one of the most important concepts in system design because it directly addresses a common production problem: multiple requests trying to modify the same resource at the same time.

By the end of this chapter, you should understand not just how to write a locking query, but why it works, what PostgreSQL does internally, and when locking becomes a scalability bottleneck.

## 1. What is pessimistic locking?

Pessimistic locking assumes that concurrent operations may conflict, so a transaction acquires a lock before performing the protected operation.

Imagine a movie-ticket booking system. Seat A10 is available, and two customers attempt to book it simultaneously.

Without coordination:

```
Customer A                   Customer B
    |                            |
    | Read A10 = AVAILABLE       |
    |                            | Read A10 = AVAILABLE
    | Book A10                   |
    |                            | Book A10
    |                            |
    +-------- Conflict ----------+
```

With pessimistic locking:

```
Customer A                   Customer B
    |                            |
    | Acquire lock on A10        |
    |                            | Try to acquire lock
    | Read availability          | Wait
    | Reserve A10                | Wait
    | Commit transaction         | Wait
    | Release lock               |
    |                            | Acquire lock
    |                            | Read latest state
    |                            | Discover A10 is booked
    |                            | Reject booking
```

The second transaction cannot acquire a conflicting lock while the first transaction holds it. Once the first transaction finishes, the second can proceed and evaluate the current state.

This is the key idea:

Acquire the lock before making the decision that must be protected, and hold it until the transaction finishes.

One important qualification: a lock only coordinates operations that conflict under the database's locking rules. A separate application that ignores a Redis lock, for example, is not automatically coordinated by it. Database constraints are still useful as a final line of defense.

## 2. What exactly is being locked?

The term lock is overloaded. Different systems lock different resources at different granularities.

| Lock scope  | What it protects                                         | Example                      |
| ----------- | -------------------------------------------------------- | ---------------------------- |
| Row-level   | A particular database row against conflicting operations | One inventory item           |
| Table-level | Operations on a table, depending on lock mode            | Schema changes               |
| Page-level  | A database storage page in systems that use such locks   | Some database internals      |
| Advisory    | A logical resource identified by the application         | One scheduled report         |
| Distributed | A resource coordinated across independent processes      | A shared job across services |

PostgreSQL supports several of these mechanisms, but its normal row-level locking is particularly important for application development.

A row lock lets unrelated rows be processed concurrently.

For example, if Alice purchases product 101 and Bob purchases product 202, PostgreSQL can allow both transactions to hold row locks simultaneously, assuming there are no other conflicting operations.

This is much more scalable than locking the entire inventory table for every purchase.

## 3. Shared locks vs exclusive locks

Before understanding PostgreSQL's locking queries, let's understand the two fundamental lock types.

### 3.1 Shared lock (read lock)

A shared lock generally allows multiple transactions to read the same resource concurrently while preventing conflicting modifications.

```
Transaction A          Transaction B          Transaction C
     |                       |                      |
     | Acquire shared        |                      |
     |-----------------------|                      |
     |                       | Acquire shared       |
     |                       |----------------------|
     | Read                  | Read                 | Wait to modify
```

Multiple readers can coexist, but a conflicting writer must wait.

This is useful when several transactions need to inspect a resource and you need to prevent it from changing in a way that invalidates their work.

However, an ordinary SQL `SELECT` in PostgreSQL does not acquire a shared row lock. PostgreSQL's MVCC mechanism normally allows reads without blocking concurrent updates.

### 3.2 Exclusive lock (write lock)

An exclusive lock prevents other transactions from acquiring incompatible locks on the same resource.

```
Transaction A          Transaction B
     |                       |
     | Acquire exclusive     |
     |                       | Try conflicting lock
     |                       | Wait
     | Update row            | Wait
     | Commit                 |
     | Release                |
     |                       | Continue
```

Exclusive locking is useful when a transaction needs to inspect and modify a resource while ensuring that another transaction cannot make a conflicting change before the transaction finishes.

### 3.3 Lock compatibility

Conceptually, shared and exclusive locks behave like this:

| Existing lock | New shared lock | New exclusive lock |
| ------------- | --------------- | ------------------ |
| Shared        | Allowed         | Wait               |
| Exclusive     | Wait            | Wait               |

This is a useful introductory model, but it is not the complete PostgreSQL lock-compatibility matrix. PostgreSQL has several row-lock modes, each with different compatibility rules.

For HLD interviews, understand the general principle first:

- Compatible locks can coexist.
- Conflicting locks require coordination.
- A waiting transaction can reduce throughput if many requests compete for the same resource.

## 4. PostgreSQL `SELECT ... FOR UPDATE`

This is the most important practical pessimistic-locking mechanism to learn first.

Consider this inventory table:

```
CREATE TABLE inventory (
    product_id BIGINT PRIMARY KEY,
    stock INTEGER NOT NULL CHECK (stock >= 0)
);

INSERT INTO inventory (product_id, stock)
VALUES (101, 1);
```

Suppose two requests want to purchase product 101.

### 4.1 The locking query

```
BEGIN;

SELECT product_id, stock
FROM inventory
WHERE product_id = 101
FOR UPDATE;
```

PostgreSQL locates the matching row and acquires a row-level lock that conflicts with other operations trying to lock or modify that row.

The transaction can now inspect the stock and make its reservation decision while holding the lock.

For example:

```
UPDATE inventory
SET stock = stock - 1
WHERE product_id = 101;

COMMIT;
```

In a real application, you would first check that the stock is greater than zero, or use a conditional update. The example above illustrates locking, not a complete safe inventory implementation.

### 4.2 What happens internally?

At a high level, PostgreSQL performs these steps:

1. It identifies the row matching the query.
2. It acquires the appropriate row-level lock.
3. If a conflicting transaction already holds a lock, the requesting transaction waits, unless a nonblocking option or timeout changes that behavior.
4. Once the lock is acquired, the transaction can perform its protected operations.
5. The lock is held until the transaction ends, usually by `COMMIT` or `ROLLBACK`.
6. Waiting transactions can then proceed, subject to remaining conflicts and transaction isolation rules.

PostgreSQL also takes a table-level `ROW SHARE` lock for a `SELECT ... FOR UPDATE` statement. This table lock does not mean the entire table is exclusively locked; ordinary operations on unrelated rows can continue.

Important internal detail: PostgreSQL uses MVCC (Multi-Version Concurrency Control). A row lock is not simply a mutex stored in an application process. PostgreSQL coordinates transactions through database-managed tuple and transaction state, and its lock manager handles relevant lock requests and waits.

Some row-lock information is represented in tuple headers and can cause disk-page writes. PostgreSQL may also use transaction-ID locks to coordinate waits. Consequently, inspecting `pg_locks` does not always give you a simple, direct listing of every locked row.

### 4.3 Why must we use a transaction?

Consider this incorrect approach:

```
SELECT *
FROM inventory
WHERE product_id = 101
FOR UPDATE;

-- Application performs other work.

UPDATE inventory
SET stock = stock - 1
WHERE product_id = 101;
```

If the application executes the `SELECT` in autocommit mode, the transaction ends when the statement finishes. The lock is released before the subsequent update.

The intended protection is lost.

The correct structure is:

```
BEGIN;

SELECT *
FROM inventory
WHERE product_id = 101
FOR UPDATE;

-- Check the current stock.
-- Perform the reservation.

UPDATE inventory
SET stock = stock - 1
WHERE product_id = 101;

COMMIT;
```

The lock protects the row throughout the transaction.

This leads to an important rule:

A pessimistic row lock is useful only if the lock remains held throughout the critical section it is supposed to protect.

### 4.4 A production-safe inventory implementation

For simple inventory decrements, a conditional update is usually preferable to a separate locking query.

```
UPDATE inventory
SET stock = stock - 1
WHERE product_id = 101
  AND stock > 0
RETURNING product_id, stock;
```

The update is atomic, and the `CHECK (stock >= 0)` constraint provides an additional database-level safeguard against negative stock.

If a successful reservation also requires inserting an order, combine those changes in a transaction:

```
BEGIN;

UPDATE inventory
SET stock = stock - 1
WHERE product_id = 101
  AND stock > 0
RETURNING product_id;

-- If no row was returned, reject the reservation.
-- Otherwise, insert the order.

COMMIT;
```

The application must check the result and roll back if the reservation failed. It must also ensure that order creation and the stock decrement succeed or fail together.

For a workflow that requires reading several rows, evaluating business rules, and then making coordinated changes, `SELECT ... FOR UPDATE` can be more appropriate.

## 5. The four PostgreSQL row-lock modes

PostgreSQL provides four row-level locking clauses. They differ in which concurrent operations they permit.

| SQL clause          | What it protects                                                                                          |
| ------------------- | --------------------------------------------------------------------------------------------------------- |
| `FOR UPDATE`        | Strongest row-lock mode; conflicts with all other row-lock modes and ordinary updates/deletes of that row |
| `FOR NO KEY UPDATE` | Allows more concurrency with operations that only need a key-share lock                                   |
| `FOR SHARE`         | Prevents conflicting updates and deletes while allowing compatible readers                                |
| `FOR KEY SHARE`     | Protects key-related aspects of a row against conflicting changes                                         |

Let's examine their practical meaning.

### 5.1 `FOR UPDATE`

```
SELECT *
FROM inventory
WHERE product_id = 101
FOR UPDATE;
```

Use it when your transaction intends to modify the selected row and wants to prevent conflicting changes before it does so.

Typical examples:

- Reserving a ticket.
- Updating a wallet balance.
- Changing an order's state after checking its current state.

It is the clearest starting point for pessimistic locking.

### 5.2 `FOR NO KEY UPDATE`

```
SELECT *
FROM accounts
WHERE account_id = 500
FOR NO KEY UPDATE;
```

This is a weaker row lock than `FOR UPDATE`.

It is useful when changing a row without changing the key values that determine its identity or participate in relevant foreign-key relationships.

PostgreSQL's ordinary `UPDATE` generally takes a `FOR NO KEY UPDATE`-equivalent row lock when it does not modify key columns in the relevant sense. When key columns are modified, it can take the stronger `FOR UPDATE` mode.

The distinction matters because `FOR NO KEY UPDATE` is compatible with `FOR KEY SHARE`, allowing some foreign-key-related operations to proceed without unnecessary blocking.

You do not need to choose this mode manually in every application. Use it when its weaker locking semantics are appropriate and you understand the interactions.

### 5.3 `FOR SHARE`

```
SELECT *
FROM products
WHERE product_id = 101
FOR SHARE;
```

This allows compatible shared row locks but prevents conflicting modifications while the lock is held.

Unlike an ordinary `SELECT`, this is an explicit locking read.

One subtle point: a shared row lock does not freeze every value or every related row in the database. It protects the selected rows according to PostgreSQL's lock compatibility rules.

### 5.4 `FOR KEY SHARE`

```
SELECT *
FROM products
WHERE product_id = 101
FOR KEY SHARE;
```

This is the weakest of the four row-lock modes.

It prevents deletion of the selected row and key-changing updates that conflict with the lock, but permits certain non-key updates.

PostgreSQL uses this mode in relevant foreign-key checks. It helps prevent a referenced row from disappearing or having its referenced key changed while a dependent operation relies on it.

### 5.5 Lock-mode compatibility

Here is the actual compatibility matrix for PostgreSQL row-level locks.

A check mark means the new lock is compatible with the existing lock; a cross means it conflicts.

| Existing ↓ / Requested → | Key Share | Share | No Key Update | Update |
| ------------------------ | --------- | ----- | ------------- | ------ |
| Key Share                | ✓         | ✓     | ✓             | ✗      |
| Share                    | ✓         | ✓     | ✗             | ✗      |
| No Key Update            | ✓         | ✗     | ✗             | ✗      |
| Update                   | ✗         | ✗     | ✗             | ✗      |

The practical takeaway is that PostgreSQL does not treat every row lock as identical.

Interview tip: If asked which mode to use for ordinary pessimistic locking, start with `FOR UPDATE`. Explain the weaker modes when concurrency optimization or foreign-key behavior makes the distinction relevant.

## 6. What happens when two transactions lock the same row?

Let's work through a concrete example.

Assume `inventory(product_id, stock)` contains product 101 with stock 5.

Transaction A

```
BEGIN;

SELECT stock
FROM inventory
WHERE product_id = 101
FOR UPDATE;

-- Returns 5.
-- Transaction A keeps running.
```

Transaction B, executed from another database connection:

```
BEGIN;

SELECT stock
FROM inventory
WHERE product_id = 101
FOR UPDATE;
```

Transaction B attempts to acquire a conflicting lock on the same row. It waits.

Now Transaction A executes:

```
UPDATE inventory
SET stock = 4
WHERE product_id = 101;

COMMIT;
```

Transaction A's lock is released when the transaction commits.

Under PostgreSQL's default `READ COMMITTED` isolation level, Transaction B can then lock and return the updated row, observing `stock = 4`.

This behavior is crucial: the second transaction does not simply acquire a lock and blindly continue using its original assumption.

The precise behavior can differ under other isolation levels. In particular, `REPEATABLE READ` and `SERIALIZABLE` may produce serialization errors when a transaction attempts to lock or update a row that changed after its snapshot was established. Applications using these isolation levels must be prepared to retry eligible transactions.

### Why this is safe

The lock serializes the conflicting operations on that row.

Instead of allowing both transactions to make a decision based on an unchanged assumption, PostgreSQL coordinates their access so that the later operation can account for the preceding change.

However, the lock alone does not validate your business rules. Transaction B must still check whether stock remains available before making a reservation.

## 7. Deadlocks: the biggest pessimistic-locking hazard

Locks can prevent race conditions, but they can also cause transactions to wait for one another indefinitely unless the database detects the problem or the application intervenes.

A deadlock occurs when transactions form a cycle of dependencies: each is waiting for a resource held by another transaction in the cycle.

### 7.1 A classic deadlock

Suppose a bank transfer touches two account rows.

Transaction A transfers money from account 1 to account 2.

Transaction B transfers money from account 2 to account 1.

They acquire their locks in opposite orders.

\#chatgpt-mermaid-\_r_2mc\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_2mc\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_2mc\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_2mc\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_2mc\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2mc\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_2mc\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_2mc\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_2mc\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_2mc\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_2mc\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_2mc\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2mc\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2mc\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_2mc\_ p{margin:0;}#chatgpt-mermaid-\_r_2mc\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2mc\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2mc\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2mc\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_2mc\_ .label text,#chatgpt-mermaid-\_r_2mc\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2mc\_ .node rect,#chatgpt-mermaid-\_r_2mc\_ .node circle,#chatgpt-mermaid-\_r_2mc\_ .node ellipse,#chatgpt-mermaid-\_r_2mc\_ .node polygon,#chatgpt-mermaid-\_r_2mc\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_2mc\_ .rough-node .label text,#chatgpt-mermaid-\_r_2mc\_ .node .label text,#chatgpt-mermaid-\_r_2mc\_ .image-shape .label,#chatgpt-mermaid-\_r_2mc\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_2mc\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_2mc\_ .rough-node .label,#chatgpt-mermaid-\_r_2mc\_ .node .label,#chatgpt-mermaid-\_r_2mc\_ .image-shape .label,#chatgpt-mermaid-\_r_2mc\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_2mc\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_2mc\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2mc\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2mc\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_2mc\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_2mc\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_2mc\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_2mc\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_2mc\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_2mc\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_2mc\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2mc\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2mc\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_2mc\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2mc\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_2mc\_ .icon-shape,#chatgpt-mermaid-\_r_2mc\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_2mc\_ .icon-shape p,#chatgpt-mermaid-\_r_2mc\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_2mc\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_2mc\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_2mc\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_2mc\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_2mc\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_2mc\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_2mc\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_2mc\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_2mc\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2mc\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_2mc\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_2mc\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_2mc\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2mc\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_2mc\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_2mc\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2mc\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_2mc\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_2mc\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2mc\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_2mc\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2mc\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_2mc\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_2mc\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_2mc\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_2mc\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_2mc\_ .node rect,#chatgpt-mermaid-\_r_2mc\_ .node circle,#chatgpt-mermaid-\_r_2mc\_ .node ellipse,#chatgpt-mermaid-\_r_2mc\_ .node polygon,#chatgpt-mermaid-\_r_2mc\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_2mc\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_2mc\_ .node.mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_2mc\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_2mc\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2mc\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}Transaction AHolds lock on account 1Transaction BHolds lock on account 2Waits for account 2Waits for account 1

Neither transaction can proceed because each needs the other's lock.

### 7.2 Reproducing the deadlock in PostgreSQL

Start with two account rows.

Connection A

```
BEGIN;

SELECT *
FROM accounts
WHERE account_id = 1
FOR UPDATE;
```

Connection B

```
BEGIN;

SELECT *
FROM accounts
WHERE account_id = 2
FOR UPDATE;
```

Now, in Connection A:

```
SELECT *
FROM accounts
WHERE account_id = 2
FOR UPDATE;
```

Connection A waits for Connection B.

Then, in Connection B:

```
SELECT *
FROM accounts
WHERE account_id = 1
FOR UPDATE;
```

Now Connection B waits for Connection A.

A deadlock exists.

PostgreSQL detects the cycle and aborts one of the transactions, allowing the other to proceed. Which transaction is chosen as the victim is not something an application should depend on.

The application must handle the error, roll back the failed transaction, and retry the complete transaction when appropriate.

### 7.3 How do we prevent deadlocks?

Strategy 1: Acquire locks in a consistent order.

For transfers, always lock the lower account ID first, then the higher ID.

```
BEGIN;

SELECT *
FROM accounts
WHERE account_id IN (1, 2)
ORDER BY account_id
FOR UPDATE;

-- Validate balances.
-- Perform both balance updates.

COMMIT;
```

This illustrates the intended ordering, but production code should ensure that it acquires the required locks in the intended order, especially for complex queries and multiple locking statements. An explicit sequence of individual locking queries in sorted order can make the ordering easier to reason about.

Strategy 2: Keep transactions short.

Avoid unnecessary work while holding locks.

Do not make an external HTTP request, wait for a human action, or perform slow computational work inside a transaction that is holding important locks.

Strategy 3: Lock only what is necessary.

Avoid broad locking queries when a specific row or a narrow set of rows is sufficient.

Strategy 4: Retry correctly.

Retry the entire transaction after a deadlock, not merely the last SQL statement. The failed transaction's earlier work has been rolled back.

Retries should be bounded and use appropriate backoff. They should also account for whether external side effects may already have occurred.

### 7.4 Deadlocks versus lock timeouts

These are different problems.

- Deadlock: A cycle of waits exists. PostgreSQL can detect the cycle and abort a transaction.
- Lock timeout: A lock request waited longer than its configured timeout. The waiting statement is canceled with an error.

A timeout does not prove that a deadlock occurred.

For production systems, configure sensible transaction and lock timeouts based on the application's latency requirements, and monitor the errors they generate.

## 8. `NOWAIT` and `SKIP LOCKED`

These are two important options for changing how PostgreSQL handles rows that are already locked.

### 8.1 `NOWAIT`: fail instead of waiting

```
SELECT *
FROM inventory
WHERE product_id = 101
FOR UPDATE NOWAIT;
```

If a conflicting lock prevents the query from acquiring the requested lock, PostgreSQL returns an error immediately instead of waiting.

Use it when waiting is not acceptable and the application can handle an immediate conflict response.

Examples:

- A user is editing a record that must not be modified concurrently.
- An operation has a strict latency budget.
- A request should fail fast rather than join a long queue.

Be careful: a lock conflict is not necessarily a permanent business failure. The application may return a conflict response or retry later.

Also, PostgreSQL errors normally put the current transaction into an aborted state. If you catch the error in application code, you generally need to roll back the transaction before reusing that connection for further work.

### 8.2 `SKIP LOCKED`: skip rows that are locked

Suppose you have a table of background jobs:

```
CREATE TABLE jobs (
    job_id BIGSERIAL PRIMARY KEY,
    status TEXT NOT NULL DEFAULT 'pending',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
```

Several workers want to claim jobs concurrently.

If every worker selects the oldest pending job and waits for its lock, they can become unnecessarily serialized.

Instead:

```
BEGIN;

SELECT job_id
FROM jobs
WHERE status = 'pending'
ORDER BY created_at, job_id
FOR UPDATE SKIP LOCKED
LIMIT 1;

-- Update the selected job to mark it as claimed.

COMMIT;
```

A worker skips rows that cannot currently be locked and attempts to claim another available row.

\#chatgpt-mermaid-\_r_2od\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_2od\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_2od\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_2od\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_2od\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2od\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_2od\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_2od\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_2od\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_2od\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_2od\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_2od\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2od\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2od\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_2od\_ p{margin:0;}#chatgpt-mermaid-\_r_2od\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2od\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2od\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2od\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_2od\_ .label text,#chatgpt-mermaid-\_r_2od\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2od\_ .node rect,#chatgpt-mermaid-\_r_2od\_ .node circle,#chatgpt-mermaid-\_r_2od\_ .node ellipse,#chatgpt-mermaid-\_r_2od\_ .node polygon,#chatgpt-mermaid-\_r_2od\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_2od\_ .rough-node .label text,#chatgpt-mermaid-\_r_2od\_ .node .label text,#chatgpt-mermaid-\_r_2od\_ .image-shape .label,#chatgpt-mermaid-\_r_2od\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_2od\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_2od\_ .rough-node .label,#chatgpt-mermaid-\_r_2od\_ .node .label,#chatgpt-mermaid-\_r_2od\_ .image-shape .label,#chatgpt-mermaid-\_r_2od\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_2od\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_2od\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2od\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2od\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_2od\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_2od\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_2od\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_2od\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_2od\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_2od\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_2od\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2od\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2od\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_2od\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2od\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_2od\_ .icon-shape,#chatgpt-mermaid-\_r_2od\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_2od\_ .icon-shape p,#chatgpt-mermaid-\_r_2od\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_2od\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_2od\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_2od\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_2od\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_2od\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_2od\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_2od\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_2od\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_2od\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2od\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_2od\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_2od\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_2od\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2od\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_2od\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_2od\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2od\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_2od\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_2od\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2od\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_2od\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2od\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_2od\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_2od\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_2od\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_2od\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_2od\_ .node rect,#chatgpt-mermaid-\_r_2od\_ .node circle,#chatgpt-mermaid-\_r_2od\_ .node ellipse,#chatgpt-mermaid-\_r_2od\_ .node polygon,#chatgpt-mermaid-\_r_2od\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_2od\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_2od\_ .node.mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_2od\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_2od\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2od\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}Worker requests a jobFind oldest pending jobCan the row be locked?Wait for lockSkip locked rowClaim selected jobCommitYesNo, ordinary lockingNo, SKIP LOCKED

This pattern is especially useful for database-backed work queues.

But `SKIP LOCKED` comes with a trade-off: it deliberately gives an inconsistent view of which rows are available. It can skip an older job that another worker is processing, allowing a newer job to run first.

That means:

- It is useful for competing work consumers.
- It is not appropriate when every row must be considered in a consistent order.
- It does not guarantee exactly-once job processing.
- A worker crash, retry, or lease-expiry policy still needs to be handled.

### 8.3 A production-minded job-claiming query

The following pattern claims a job within a short transaction and returns the claimed row.

```
BEGIN;

WITH next_job AS (
    SELECT job_id
    FROM jobs
    WHERE status = 'pending'
    ORDER BY created_at, job_id
    FOR UPDATE SKIP LOCKED
    LIMIT 1
)
UPDATE jobs AS j
SET status = 'processing'
FROM next_job
WHERE j.job_id = next_job.job_id
RETURNING j.*;

COMMIT;
```

The conditional update and row lock coordinate concurrent claim attempts. The status change is committed before the worker begins processing the job.

A complete production implementation also needs a recovery mechanism for jobs left in `processing` after a worker crashes. Common approaches include claim timestamps, lease expiry, worker identifiers, and retry counts.

If processing involves an external service, use idempotency keys or another deduplication strategy because the external action and the database transaction are not automatically atomic.

## 9. When should you use pessimistic locking?

Pessimistic locking is useful when conflicts are relatively common, the cost of allowing concurrent decisions to proceed incorrectly is high, and the critical section can be kept short.

| Scenario                              | Typical approach                       | Why                                             |
| ------------------------------------- | -------------------------------------- | ----------------------------------------------- |
| Booking a particular seat             | Conditional update or row lock         | Prevent two bookings from winning               |
| Transferring money between accounts   | Transaction with appropriate row locks | Protect balances and coordinate both accounts   |
| Claiming a database-backed job        | `FOR UPDATE SKIP LOCKED`               | Allow workers to claim different jobs           |
| Updating a heavily contested resource | Atomic update or pessimistic lock      | Coordinate changes safely                       |
| Editing a low-contention profile      | Optimistic locking                     | Avoid unnecessary waiting                       |
| Incrementing a counter                | Atomic SQL operation                   | Avoid application-side read-modify-write        |
| Enforcing uniqueness                  | Unique constraint                      | Let the database enforce the invariant directly |

There is no universal rule that high contention always means pessimistic locking is best. Under extreme contention, an atomic update, queue, partitioning strategy, or different data model may provide better throughput.

### When not to use it

Avoid unnecessary pessimistic locking when:

- Conflicts are rare and optimistic concurrency control is sufficient.
- A single atomic SQL statement can safely express the operation.
- The transaction would hold locks while waiting on slow external systems.
- You are locking broad sets of rows for a narrow operation.
- You expect to hold a lock for a long time.
- Your actual problem spans several independent systems that PostgreSQL cannot coordinate through one local transaction.

A database lock is not a substitute for a distributed transaction or a well-designed workflow.

## 10. PostgreSQL locking in production: monitoring and configuration

Understanding the SQL syntax is only half the job. In production, you also need to identify blocked queries, investigate long transactions, and determine whether locking is affecting latency.

### 10.1 Find blocked queries

PostgreSQL exposes current activity through `pg_stat_activity` and lock information through `pg_locks`.

You can use a query like this to inspect sessions that are waiting for other transactions:

```
SELECT
    blocked.pid AS blocked_pid,
    blocked.usename AS blocked_user,
    blocked.query AS blocked_query,
    blocked.wait_event_type,
    blocked.wait_event,
    blocker.pid AS blocker_pid,
    blocker.query AS blocker_query,
    now() - blocker.xact_start AS blocker_transaction_age
FROM pg_stat_activity AS blocked
CROSS JOIN LATERAL
    unnest(pg_blocking_pids(blocked.pid)) AS blocking(pid)
JOIN pg_stat_activity AS blocker
    ON blocker.pid = blocking.pid
ORDER BY blocker.xact_start NULLS LAST;
```

This helps you identify:

- Which session is waiting.
- Which session is blocking it.
- What the two sessions are executing.
- How long the blocking transaction has been open.

A long-running transaction is not automatically a problem, but one holding locks that many requests need can cause a significant production incident.

### 10.2 Set a lock timeout

For a transaction that should not wait indefinitely for a lock:

```
BEGIN;

SET LOCAL lock_timeout = '2s';

SELECT *
FROM inventory
WHERE product_id = 101
FOR UPDATE;

-- Perform the required work.

COMMIT;
```

Here, PostgreSQL cancels a lock-acquisition attempt if it waits longer than two seconds.

`SET LOCAL` applies the setting for the current transaction. Choose the timeout based on your latency requirements; two seconds is an example, not a universal recommendation.

If the statement fails because of the timeout, roll back the transaction before attempting further work on that transaction.

You should distinguish between:

- `lock_timeout`: maximum time spent waiting to acquire a lock.
- `statement_timeout`: maximum duration of a statement.
- `idle_in_transaction_session_timeout`: maximum time a session can remain idle while holding an open transaction.

These settings address different operational risks.

### 10.3 What should you monitor?

For a production application, useful metrics include:

- Lock wait duration and frequency.
- Deadlock count.
- Transaction duration.
- Number of active and waiting database sessions.
- Query latency and connection-pool saturation.
- Lock timeout and serialization failure rates.

The goal is not to eliminate all waiting. Some waiting is normal. The goal is to identify unnecessary contention and ensure that transactions complete within acceptable latency bounds.

## 11. Common interview questions

### Q1. Does `SELECT` lock a row in PostgreSQL?

An ordinary `SELECT` normally does not acquire a row-level lock. It reads a snapshot using MVCC.

A locking read such as `SELECT ... FOR UPDATE` explicitly requests a row lock.

### Q2. Does `SELECT FOR UPDATE` lock the whole table?

No. It locks the selected rows against conflicting operations, although the query also acquires a table-level lock with a compatible lock mode.

Other transactions can usually lock or modify unrelated rows.

### Q3. When is the lock released?

For ordinary PostgreSQL transaction-level row locks, the lock is released when the transaction ends, by committing or rolling back.

A long-running transaction can therefore hold locks much longer than the individual SQL statement that acquired them.

### Q4. Can two transactions update different rows simultaneously?

Yes. PostgreSQL's row-level locking allows concurrency on different rows, provided other locks, constraints, and transaction dependencies do not introduce conflicts.

### Q5. Can pessimistic locking cause deadlocks?

Yes. Deadlocks occur when transactions wait for resources held by one another in a cycle. PostgreSQL detects deadlocks and aborts a transaction to break the cycle.

### Q6. Is `SELECT FOR UPDATE` always better than optimistic locking?

No. Pessimistic locking can be effective when conflicts are frequent, but it introduces waiting and potential deadlocks. Optimistic locking can be better when conflicts are rare.

The correct choice depends on contention, transaction duration, correctness requirements, and retry behavior.

### Q7. Can a PostgreSQL row lock protect a Redis key?

No. A PostgreSQL row lock coordinates transactions through PostgreSQL. It does not automatically coordinate access to Redis or another independent service.

You need an appropriate coordination strategy for each shared resource.

## 12. Chapter 2 summary

Remember these principles:

1. Pessimistic locking acquires a lock before performing protected work.
2. `SELECT ... FOR UPDATE` is a common way to lock selected PostgreSQL rows.
3. The transaction must remain open for the duration of the critical section.
4. PostgreSQL's MVCC and row-locking mechanisms work together; ordinary reads generally do not block writers.
5. `NOWAIT` fails immediately on a lock conflict, while `SKIP LOCKED` skips rows that cannot currently be locked.
6. Deadlocks are possible. Consistent lock ordering, short transactions, monitoring, and safe retries help manage them.
7. For simple state transitions, an atomic conditional `UPDATE` is often simpler than an explicit locking read.
8. Database locks do not automatically coordinate independent systems or guarantee exactly-once external side effects.

The most important production lesson is this: use the smallest coordination mechanism that preserves your business invariant, and hold locks for the shortest time necessary.

## Next chapter

Type `next` for Chapter 3 — Optimistic Locking.

We'll cover version columns, compare-and-swap, SQL conditional updates, JPA's `@Version`, concurrent update conflicts, retry strategies, and how optimistic and pessimistic locking compare under real workloads.


