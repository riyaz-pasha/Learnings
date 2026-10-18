# Chapter 4 — PostgreSQL Concurrency Internals

Focus: MVCC, transaction snapshots, isolation levels, row-lock internals, predicate locks, write skew, deadlocks, and production debugging.

So far, we have studied pessimistic locking and optimistic locking from an application developer's perspective.

Now let's look inside PostgreSQL.

The most important concept to understand is this:

PostgreSQL does not rely on locking every read against every write. It uses MVCC to let transactions work with appropriate versions of data while coordinating conflicting changes.

Understanding this explains why PostgreSQL can handle many concurrent requests without forcing every query to wait for every other query.

## 1. What problem does MVCC solve?

MVCC stands for Multi-Version Concurrency Control.

Imagine a product table containing:

| product_id | price |
| ---------- | ----- |
| 101        | 100   |

Transaction A starts reading the product price.

At almost the same time, Transaction B updates the price to 120.

Should Transaction A wait until Transaction B finishes? Not necessarily.

PostgreSQL can allow Transaction A to read a version of the row that is visible to its transaction, while Transaction B creates a newer version.

Conceptually:

\#chatgpt-mermaid-\_r_37m\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_37m\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_37m\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_37m\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_37m\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_37m\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_37m\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_37m\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_37m\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_37m\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_37m\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_37m\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_37m\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_37m\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_37m\_ p{margin:0;}#chatgpt-mermaid-\_r_37m\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_37m\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_37m\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_37m\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_37m\_ .label text,#chatgpt-mermaid-\_r_37m\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_37m\_ .node rect,#chatgpt-mermaid-\_r_37m\_ .node circle,#chatgpt-mermaid-\_r_37m\_ .node ellipse,#chatgpt-mermaid-\_r_37m\_ .node polygon,#chatgpt-mermaid-\_r_37m\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_37m\_ .rough-node .label text,#chatgpt-mermaid-\_r_37m\_ .node .label text,#chatgpt-mermaid-\_r_37m\_ .image-shape .label,#chatgpt-mermaid-\_r_37m\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_37m\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_37m\_ .rough-node .label,#chatgpt-mermaid-\_r_37m\_ .node .label,#chatgpt-mermaid-\_r_37m\_ .image-shape .label,#chatgpt-mermaid-\_r_37m\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_37m\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_37m\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_37m\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_37m\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_37m\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_37m\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_37m\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_37m\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_37m\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_37m\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_37m\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_37m\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_37m\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_37m\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_37m\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_37m\_ .icon-shape,#chatgpt-mermaid-\_r_37m\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_37m\_ .icon-shape p,#chatgpt-mermaid-\_r_37m\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_37m\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_37m\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_37m\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_37m\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_37m\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_37m\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_37m\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_37m\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_37m\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_37m\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_37m\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_37m\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_37m\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_37m\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_37m\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_37m\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_37m\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_37m\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_37m\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_37m\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_37m\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_37m\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_37m\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_37m\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_37m\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_37m\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_37m\_ .node rect,#chatgpt-mermaid-\_r_37m\_ .node circle,#chatgpt-mermaid-\_r_37m\_ .node ellipse,#chatgpt-mermaid-\_r_37m\_ .node polygon,#chatgpt-mermaid-\_r_37m\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_37m\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_37m\_ .node.mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_37m\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_37m\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_37m\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}Existing row versionprice = 100Transaction A readsprice = 100Transaction B updatesprice = 120New row versionprice = 120Can continue readingits visible version

This is a conceptual illustration, not a promise that PostgreSQL always keeps both versions indefinitely. PostgreSQL retains old row versions as needed for visibility and cleanup, and eventually reclaims obsolete versions through vacuuming.

The key benefit is that ordinary readers usually do not block writers, and writers usually do not block ordinary readers.

That improves concurrency significantly.

### What MVCC does not mean

MVCC does not mean:

- Every transaction sees the latest committed value at all times.
- Writers never wait for other writers.
- PostgreSQL never acquires locks.
- Every old version remains available forever.
- All transactions automatically satisfy every business invariant.

MVCC controls which row versions are visible to a transaction. Locks and transaction isolation rules handle additional aspects of concurrency.

## 2. How PostgreSQL stores row versions

PostgreSQL uses a technique called tuple versioning.

A tuple is PostgreSQL's term for a row stored in a table.

Suppose a row begins as:

```
product_id = 101
price      = 100
```

A transaction updates the price:

```
UPDATE products
SET price = 120
WHERE product_id = 101;
```

Conceptually, PostgreSQL creates a new row version rather than simply overwriting all the old row contents in place.

The table's physical storage may contain information resembling:

```
Version 1:
price = 100
xmin = transaction that created this version
xmax = transaction that superseded or deleted it

Version 2:
price = 120
xmin = transaction that created this version
xmax = not yet superseded or deleted
```

`xmin` and `xmax` are system columns that represent transaction IDs associated with a tuple's creation and deletion or replacement. Their exact interpretation involves transaction status and visibility rules, so they should not be treated as simple timestamps.

An update can create a new tuple version and mark the old version as no longer current. The old version may still be visible to transactions whose snapshots require it.

For example:

| Transaction                    | Relevant visible price |
| ------------------------------ | ---------------------- |
| A, with an earlier snapshot    | 100                    |
| B, after committing the update | 120                    |

The precise result depends on the transaction's isolation level and snapshot timing.

### Why retain old versions?

Without old versions, a long-running reader could suddenly observe a value that changed halfway through its transaction, even when its isolation semantics require a consistent view.

MVCC allows PostgreSQL to preserve the versions needed by active snapshots.

### The cost: dead tuples

Old versions consume storage until PostgreSQL determines that they are no longer needed.

This creates a maintenance requirement:

- Updates and deletes generate obsolete tuple versions.
- `VACUUM` reclaims space for reuse when versions are no longer needed.
- Long-running transactions can prevent cleanup of versions still needed by their snapshots.
- Poor vacuum behavior and excessive updates can cause table bloat.

This is one reason why transaction duration matters even when a transaction is not holding an obvious row lock.

## 3. What is a transaction snapshot?

A snapshot determines which database changes a transaction can see.

Imagine this sequence:

```
T1: Transaction A begins
T2: Transaction A reads price = 100
T3: Transaction B updates price to 120
T4: Transaction B commits
T5: Transaction A reads price again
```

What does Transaction A see at T5?

It depends on the isolation level.

Under PostgreSQL's default `READ COMMITTED` isolation, each SQL statement generally gets a new snapshot. If Transaction A executes a new `SELECT` at T5, it can see the committed price of 120.

Under `REPEATABLE READ`, Transaction A generally continues to see the data from its transaction snapshot, so it can continue seeing 100.

This distinction is fundamental.

A transaction is not necessarily one fixed snapshot under every isolation level.

### 3.1 `READ COMMITTED`

PostgreSQL's default isolation level.

Each statement sees data committed before that statement's snapshot was taken, subject to the isolation and visibility rules.

```
BEGIN ISOLATION LEVEL READ COMMITTED;

SELECT price
FROM products
WHERE product_id = 101;

-- Another transaction updates and commits the price.

SELECT price
FROM products
WHERE product_id = 101;

COMMIT;
```

The two queries can return different prices.

This is often suitable for typical CRUD applications, but it does not automatically make a sequence of separate reads and writes safe.

### 3.2 `REPEATABLE READ`

PostgreSQL establishes a transaction snapshot that is generally retained throughout the transaction.

```
BEGIN ISOLATION LEVEL REPEATABLE READ;

SELECT price
FROM products
WHERE product_id = 101;

-- Another transaction updates and commits the price.

SELECT price
FROM products
WHERE product_id = 101;

COMMIT;
```

The second query generally sees the same committed row version visible to the first query.

PostgreSQL implements `REPEATABLE READ` using snapshot isolation. It prevents non-repeatable reads and phantoms in the usual SQL sense, but it does not prevent every possible serialization anomaly.

That last point is especially important for system design.

### 3.3 `SERIALIZABLE`

This isolation level aims to ensure that successfully committed transactions behave as though they executed in some serial order.

PostgreSQL uses Serializable Snapshot Isolation (SSI), which detects dangerous dependency patterns and aborts transactions when necessary.

The application may receive a serialization failure and must retry the entire transaction when appropriate.

We will examine this mechanism in detail below.

## 4. The four isolation levels and their practical meaning

SQL defines four standard isolation levels. PostgreSQL supports all four, but its implementation details matter.

| Isolation level    | Non-repeatable reads     | Phantoms                 | Serialization anomalies                           |
| ------------------ | ------------------------ | ------------------------ | ------------------------------------------------- |
| `READ UNCOMMITTED` | Possible in the standard | Possible in the standard | Possible                                          |
| `READ COMMITTED`   | Possible                 | Possible                 | Possible                                          |
| `REPEATABLE READ`  | Prevented                | Prevented in PostgreSQL  | Still possible                                    |
| `SERIALIZABLE`     | Prevented                | Prevented                | Prevented for successfully committed transactions |

PostgreSQL treats `READ UNCOMMITTED` as `READ COMMITTED`, because its MVCC implementation does not expose uncommitted changes to ordinary queries.

Let's clarify the three most important anomalies.

### 4.1 Dirty read

A dirty read means reading a value written by a transaction that has not committed.

PostgreSQL does not permit dirty reads, even at `READ COMMITTED`.

### 4.2 Non-repeatable read

A transaction reads the same row twice but sees different committed values.

Example:

```
Transaction A: Read balance = 1000
Transaction B: Change balance to 1200 and commit
Transaction A: Read balance = 1200
```

This can occur under `READ COMMITTED`.

### 4.3 Phantom read

A transaction repeats a query and sees a different set of rows because another transaction committed inserts, deletes, or changes that affect the predicate.

Example:

```
SELECT *
FROM reservations
WHERE show_id = 10;
```

Another transaction inserts a reservation for show 10 and commits. Repeating the query at `READ COMMITTED` can return an additional row.

PostgreSQL's `REPEATABLE READ` also prevents this kind of phantom visibility by retaining the transaction snapshot. However, that does not mean it prevents every anomaly involving multiple transactions.

That brings us to the next concept.

## 5. Write skew: the subtle concurrency bug

Write skew is one of the most important reasons to understand the difference between snapshot isolation and serializable isolation.

Consider a hospital where at least one doctor must remain on call.

The database contains:

| doctor_id | on_call |
| --------- | ------- |
| 1         | true    |
| 2         | true    |

The business invariant is:

\\[ \text{Number of on-call doctors} \geq 1 \\]

Two doctors independently request to go off call.

- Transaction A checks that two doctors are on call.
- Transaction B checks that two doctors are on call.
- Transaction A sets doctor 1 to off call.
- Transaction B sets doctor 2 to off call.

Each transaction modifies a different row.

Under snapshot isolation, both transactions can potentially commit because neither transaction updates the same row as the other.

The resulting state is:

| doctor_id | on_call |
| --------- | ------- |
| 1         | false   |
| 2         | false   |

The invariant has been violated even though each transaction performed a valid check against its own snapshot.

### Why ordinary row locking might not be enough

If each transaction locks only the row it intends to update, the transactions can lock different rows and proceed.

The conflict is not simply "two transactions are updating the same row."

The real conflict is that both transactions make decisions based on a shared invariant involving multiple rows.

### How can we prevent write skew?

Possible approaches include:

1. Use a `SERIALIZABLE` transaction and retry serialization failures.
2. Lock a shared coordination row that every relevant transaction must acquire before checking the invariant.
3. Redesign the schema so a database constraint can enforce the rule.
4. Use a carefully designed atomic operation that makes the invariant enforceable.

For example, with PostgreSQL `SERIALIZABLE`:

```
BEGIN ISOLATION LEVEL SERIALIZABLE;

SELECT count(*)
FROM doctors
WHERE on_call = true;

-- Validate that another doctor remains on call.
-- Update the selected doctor's status.

COMMIT;
```

Concurrent executions that create a dangerous dependency pattern may result in a serialization failure. The application must be prepared to retry the entire transaction.

Interview takeaway: `REPEATABLE READ` gives a stable snapshot, but a stable snapshot is not the same thing as serializable execution.

## 6. PostgreSQL row locks internally

Let's connect the SQL behavior to the implementation.

When you execute:

```
SELECT *
FROM inventory
WHERE product_id = 101
FOR UPDATE;
```

PostgreSQL does not simply place a traditional mutex around the entire table.

Several mechanisms cooperate.

### 6.1 The lock manager

PostgreSQL has a lock manager that coordinates lock requests and records lock ownership and waiting relationships.

Many heavyweight locks, including table-level locks and transaction-ID locks, are managed through shared lock-management structures.

When a transaction requests an incompatible lock, PostgreSQL determines whether it can be granted immediately or must wait.

The lock manager also participates in deadlock detection.

### 6.2 Tuple-level locking

Row-level locking is associated with individual tuple versions.

PostgreSQL records information in tuple headers and uses transaction state to coordinate access to those tuples. In some cases, row-lock information can be represented using MultiXact structures when multiple transactions hold compatible row locks.

An important implementation detail: not every row lock appears as a separate, straightforward entry in `pg_locks`.

If you inspect `pg_locks`, you may see transaction-ID locks associated with transactions waiting for row updates. The tuple's internal state and the transaction-locking machinery together explain the actual behavior.

### 6.3 Why can acquiring a row lock modify storage?

It may seem surprising that a locking query can cause a disk-page write.

PostgreSQL sometimes needs to record row-lock information in the tuple header. This can mark a page as dirty and generate write-ahead log (WAL) activity, even if the application did not update a business column.

The exact work depends on the row's state and whether the requested lock is already held or can be represented without additional tuple changes.

This means that locking reads are not always as operationally cheap as ordinary MVCC reads.

### 6.4 Transaction-ID waits

Suppose Transaction A updates a row and has not yet committed.

Transaction B attempts to update that same row.

Transaction B may need to wait for Transaction A's transaction outcome before it can safely determine whether it can proceed.

This can appear as a wait on a transaction ID in PostgreSQL's lock and wait-event information.

The underlying logic is not merely "wait for a mutex." PostgreSQL must establish which row version is valid, whether the other transaction committed, and whether the requested operation is compatible with the resulting state.

## 7. PostgreSQL table locks versus row locks

A common interview question is whether PostgreSQL locks a table whenever it locks a row.

The precise answer is that PostgreSQL locking statements generally acquire both the relevant table-level lock mode and, where applicable, row-level locks.

For example:

```
SELECT *
FROM inventory
WHERE product_id = 101
FOR UPDATE;
```

This statement acquires a table-level `ROW SHARE` lock and row-level locks on the selected rows.

The table-level `ROW SHARE` lock is compatible with many ordinary operations, including normal inserts, updates, and deletes. Therefore, it does not mean that the entire table becomes unavailable for modification.

An `UPDATE` statement, on the other hand, typically acquires a table-level `ROW EXCLUSIVE` lock in addition to row-level locks on the affected rows.

These names are easy to misinterpret:

- `ROW SHARE` is a table-level lock mode.
- `ROW EXCLUSIVE` is also a table-level lock mode.
- Neither name means that the lock applies only to a single row.

The table-level lock modes coordinate operations that need different kinds of access to the table, particularly with respect to operations such as schema changes.

### Why this matters

A transaction holding a row lock on product 101 generally does not prevent another transaction from updating product 202.

However, a conflicting table-level lock can still block otherwise unrelated operations. For example, certain schema changes require strong table-level locks.

For production debugging, it is therefore important to distinguish between a row-level conflict and a table-level lock conflict.

## 8. PostgreSQL predicate locks and Serializable isolation

We have seen that row locks protect existing rows.

But what if the business rule concerns rows that match a condition, including rows that do not exist yet?

For example:

```
SELECT *
FROM reservations
WHERE show_id = 10;
```

Suppose there are no reservations for show 10 yet. A row lock cannot lock a row that does not exist.

PostgreSQL's Serializable isolation needs a way to detect dependencies involving reads and concurrent writes, including cases where one transaction inserts a row that another transaction's predicate would have matched.

### 8.1 What is a predicate lock?

In PostgreSQL's SSI implementation, predicate-lock information is represented by `SIReadLock` entries.

These are not ordinary blocking locks. They are used to track read dependencies so that PostgreSQL can detect potentially unsafe concurrent transaction patterns.

Depending on the query and execution plan, the tracked read information may be associated with tuples, pages, or relations.

PostgreSQL may abort a transaction if the dependency pattern means the transactions cannot safely be allowed to commit together while preserving serializable behavior.

### 8.2 Example: limiting reservations

Suppose a show can have at most 100 reservations.

Two transactions each read the current reservation count:

```
SELECT count(*)
FROM reservations
WHERE show_id = 10;
```

Both see 99.

Each then inserts a reservation.

A naive implementation might allow the count to reach 101.

Serializable isolation can detect dangerous dependencies and abort a transaction if the executions cannot be serialized safely.

But this does not eliminate the need for application-level error handling. The application must retry eligible serialization failures and re-evaluate the business condition.

Also, for a straightforward capacity limit, a stronger schema or data model can be easier to reason about. For example, reserving capacity from a single inventory row with an atomic conditional update may be simpler than repeatedly counting reservations.

Design lesson: Serializable isolation is a powerful correctness tool, but a carefully chosen atomic data model can be simpler and more efficient.

## 9. Monitoring PostgreSQL locks in production

When a production endpoint becomes slow, a common mistake is to assume the SQL query itself is expensive.

Sometimes the query is simple but spends most of its time waiting for another transaction.

### 9.1 Inspect lock waits

Use PostgreSQL's `pg_stat_activity` and `pg_blocking_pids()`:

```
SELECT
    blocked.pid AS blocked_pid,
    blocked.state AS blocked_state,
    blocked.wait_event_type,
    blocked.wait_event,
    now() - blocked.query_start AS blocked_query_age,
    blocked.query AS blocked_query,
    blocker.pid AS blocker_pid,
    blocker.state AS blocker_state,
    now() - blocker.xact_start AS blocker_transaction_age,
    blocker.query AS blocker_query
FROM pg_stat_activity AS blocked
CROSS JOIN LATERAL
    unnest(pg_blocking_pids(blocked.pid)) AS blocking(pid)
JOIN pg_stat_activity AS blocker
    ON blocker.pid = blocking.pid
ORDER BY blocker.xact_start NULLS LAST;
```

This shows waiting sessions alongside the sessions blocking them.

Look for patterns such as:

- Many requests waiting on one transaction.
- Transactions that have remained open for a long time.
- Application code holding a transaction open while performing unrelated work.
- A common resource that is repeatedly updated by many requests.

The query reports current blocking relationships; it is not a complete historical record of every lock wait.

### 9.2 Inspect lock entries

You can also inspect `pg_locks`:

```
SELECT
    pid,
    locktype,
    mode,
    granted,
    relation::regclass AS relation,
    transactionid,
    waitstart
FROM pg_locks
WHERE NOT granted
ORDER BY waitstart NULLS LAST;
```

This lists outstanding lock requests that have not been granted.

Remember that `pg_locks` does not expose every row lock as a simple row identifier. For many investigations, combining it with `pg_stat_activity`, `pg_blocking_pids()`, and the actual query text is more useful than examining `pg_locks` alone.

### 9.3 What should we optimize first?

If lock waits are causing latency, investigate in this order:

1. Identify the transaction holding the conflicting lock.
2. Find out why that transaction is taking so long.
3. Reduce the duration of the critical section.
4. Ensure transactions acquire multiple locks in a consistent order.
5. Consider whether an atomic update or a different data model can eliminate unnecessary coordination.
6. Tune timeouts and retry behavior as appropriate.

Increasing the connection pool size does not fix a lock-contention problem. It can actually increase the number of sessions waiting for the same resource.

## 10. Choosing an isolation level for production

Isolation level is a correctness decision, not merely a performance setting.

| Use case                                                      | Possible starting point                                  | Important qualification                                       |
| ------------------------------------------------------------- | -------------------------------------------------------- | ------------------------------------------------------------- |
| Typical CRUD endpoints                                        | `READ COMMITTED`                                         | Use constraints and atomic operations for business invariants |
| Updating a single inventory row                               | `READ COMMITTED` with conditional `UPDATE`               | Check the affected-row count                                  |
| Editing a profile                                             | `READ COMMITTED` with optimistic versioning              | Reject or resolve stale updates                               |
| Reading a consistent snapshot across multiple queries         | `REPEATABLE READ`                                        | Understand snapshot isolation and write skew                  |
| Complex invariants involving multiple concurrent transactions | `SERIALIZABLE`                                           | Handle serialization failures and retry transactions          |
| Transferring money between two accounts                       | Transaction with appropriate row locks or atomic updates | Enforce balance rules and update both accounts atomically     |

There is no universally best isolation level.

For many ordinary web applications, `READ COMMITTED` plus well-designed SQL, constraints, and explicit concurrency control is sufficient.

For more complex business invariants, stronger isolation or explicit coordination may be needed.

### A critical production detail: retries

With `SERIALIZABLE`, PostgreSQL may reject a transaction because of a serialization conflict.

The correct response is generally to retry the entire transaction, not just the last statement.

For PostgreSQL, the SQLSTATE codes most relevant here include:

- `40001`: serialization failure.
- `40P01`: deadlock detected.

Both can be candidates for bounded transaction retries, provided the application can safely repeat the operation.

The retry must start a fresh transaction and repeat the relevant reads and business validation. It must not reuse decisions made from a stale snapshot.

## 11. Putting it together: movie-ticket booking

Let's combine MVCC, row locking, constraints, and transactions in a realistic system.

Suppose two customers attempt to book seat A10 for the same show.

A useful schema is:

```
CREATE TABLE show_seats (
    show_id BIGINT NOT NULL,
    seat_id BIGINT NOT NULL,
    status TEXT NOT NULL DEFAULT 'available'
        CHECK (status IN ('available', 'booked')),
    PRIMARY KEY (show_id, seat_id)
);
```

The primary key ensures there is only one seat record for a given show and seat.

To book the seat, use a conditional update:

```
UPDATE show_seats
SET status = 'booked'
WHERE show_id = 10
  AND seat_id = 101
  AND status = 'available'
RETURNING show_id, seat_id;
```

The result determines whether the reservation succeeded.

- One returned row: this request changed the seat from available to booked.
- No returned row: the seat was already booked or the specified seat did not exist.

If the booking also inserts an order record, perform the seat update and order insertion in the same database transaction.

```
BEGIN;

UPDATE show_seats
SET status = 'booked'
WHERE show_id = 10
  AND seat_id = 101
  AND status = 'available'
RETURNING show_id, seat_id;

-- If the update returned a row, insert the booking.
-- Otherwise, roll back or handle the unsuccessful booking.

COMMIT;
```

The application must check the returned rows and only insert the booking when the seat update succeeds.

What does PostgreSQL provide here?

1. MVCC allows ordinary reads to operate without blocking every writer.
2. PostgreSQL coordinates concurrent updates to the same seat row.
3. The conditional update ensures that only a request matching the available state can claim the seat.
4. The primary key prevents duplicate seat records.
5. The transaction ensures the seat update and booking insertion succeed or fail together.

No Redis distributed lock is required for this database-local invariant.

If the workflow also involves payment processing, the payment system introduces a separate consistency boundary. A PostgreSQL transaction cannot atomically commit an independent payment provider's operation. That requires a suitable workflow, idempotency, and failure-recovery strategy.

## 12. Interview questions and answers

### Q1. Why does PostgreSQL use MVCC?

MVCC allows readers to see appropriate versions of data without requiring every read to block concurrent writes. It improves concurrency while supporting transaction isolation.

### Q2. Does PostgreSQL overwrite a row directly when an update occurs?

Conceptually, PostgreSQL creates a new tuple version and marks the previous version as superseded. Old versions remain until they are no longer needed and can be reclaimed.

### Q3. Does MVCC eliminate locks?

No. PostgreSQL still uses locks to coordinate conflicting writes, protect certain operations, and manage table-level and other resources. MVCC primarily changes how concurrent reads and writes interact.

### Q4. What is the difference between `READ COMMITTED` and `REPEATABLE READ`?

Under `READ COMMITTED`, each statement normally receives a new snapshot. Under PostgreSQL's `REPEATABLE READ`, the transaction generally retains a stable snapshot. PostgreSQL's `REPEATABLE READ` prevents phantom visibility but does not prevent every serialization anomaly.

### Q5. Does `REPEATABLE READ` prevent write skew?

No. PostgreSQL's `REPEATABLE READ` uses snapshot isolation, under which write skew can occur when concurrent transactions read overlapping data but modify different rows.

Use serializable transactions or another mechanism that protects the full invariant when required.

### Q6. What is PostgreSQL Serializable Snapshot Isolation?

SSI is PostgreSQL's approach to implementing serializable isolation on top of snapshot-based execution. It tracks relevant read/write dependencies and aborts transactions when necessary to prevent unsafe serialization patterns.

### Q7. What is a predicate lock in PostgreSQL?

Predicate-lock information, exposed through `SIReadLock` entries, helps PostgreSQL track dependencies for serializable transactions. These are not ordinary blocking locks; they support anomaly detection.

### Q8. What happens if a transaction holds a row lock for a long time?

Other transactions needing conflicting access may wait, increasing latency and connection-pool pressure. Long-running transactions can also interfere with MVCC cleanup and contribute to table bloat.

### Q9. Should we use `SERIALIZABLE` for every transaction?

No. It can provide stronger correctness guarantees, but applications must handle serialization failures and may incur additional overhead. Use it where the business invariants justify it.

## 13. Chapter 4 summary

The key concepts to remember are:

1. MVCC lets PostgreSQL manage concurrent readers and writers using multiple row versions.
2. Snapshots determine which committed changes a transaction can see.
3. `READ COMMITTED` generally takes a new snapshot for each statement.
4. PostgreSQL's `REPEATABLE READ` maintains a stable transaction snapshot but can permit write skew.
5. `SERIALIZABLE` aims to ensure that successfully committed transactions behave as though they executed in a serial order.
6. Row locks, tuple metadata, transaction state, and the lock manager cooperate to coordinate conflicting operations.
7. Old row versions require cleanup, which is why long-running transactions and vacuum behavior matter.
8. `pg_stat_activity`, `pg_locks`, and `pg_blocking_pids()` help diagnose locking problems.
9. Database-local correctness is not the same as atomicity across independent services.

The big picture: MVCC explains how PostgreSQL achieves concurrency; locking explains how it coordinates conflicts; isolation levels define which concurrent outcomes are permitted.

## Next chapter — Redis locking

Type `next` to continue with Chapter 5: Redis locking, from basic to advanced.

We'll cover:

- Why Redis locks differ from PostgreSQL row locks.
- `SET key value NX PX` and atomic lock acquisition.
- Why `DEL lock_key` can be dangerous.
- Unique ownership tokens and safe release using Lua scripts.
- TTL expiration, crashes, pauses, and stale lock owners.
- Lock renewal and lease management.
- Redlock: what it guarantees and what it does not.
- Fencing tokens and how to protect the actual resource.
- When Redis locks are appropriate and when a database constraint or atomic operation is safer.


