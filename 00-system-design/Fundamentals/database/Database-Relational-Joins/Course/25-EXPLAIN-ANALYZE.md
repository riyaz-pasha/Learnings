# 25. EXPLAIN / EXPLAIN ANALYZE

So far, we learned **what PostgreSQL can do** when executing joins:

```text
Nested Loop
Hash Join
Merge Join
```

But there is an important question:

> **How do we know which algorithm PostgreSQL actually chose, and why?**

That's what `EXPLAIN` helps us understand.

---

# 1. What is EXPLAIN?

`EXPLAIN` asks PostgreSQL:

> "Show me the execution plan you intend to use for this query."

Example:

```sql
EXPLAIN
SELECT
    c.name,
    o.amount
FROM customers AS c
JOIN orders AS o
    ON c.id = o.customer_id;
```

PostgreSQL might return something like:

```text
Hash Join
  Hash Cond: (o.customer_id = c.id)
  -> Seq Scan on orders o
  -> Hash
       -> Seq Scan on customers c
```

This tells us **how PostgreSQL plans to execute the query**.

---

# 2. EXPLAIN does not normally execute the query

This is an important distinction.

```sql
EXPLAIN
SELECT ...
```

normally gives you the plan without actually executing the query.

Think:

```text
EXPLAIN
   |
   v
Planner
   |
   v
Execution plan
   |
   X
Doesn't normally execute the SELECT
```

This makes `EXPLAIN` useful for investigating queries without actually running the expensive operation.

---

# 3. What is EXPLAIN ANALYZE?

Now:

```sql
EXPLAIN ANALYZE
SELECT ...
```

means:

> **Actually execute the query and show me what happened.**

For example:

```sql
EXPLAIN ANALYZE
SELECT
    c.name,
    o.amount
FROM customers AS c
JOIN orders AS o
    ON c.id = o.customer_id;
```

You may see:

```text
Hash Join  (cost=... rows=... width=...)
           (actual time=... rows=... loops=...)
```

The crucial difference:

```text
EXPLAIN
→ estimated plan

EXPLAIN ANALYZE
→ estimated plan + actual execution
```

---

# 4. The most important distinction

Think of it like this:

```text
EXPLAIN

"What does PostgreSQL THINK will happen?"
```

versus:

```text
EXPLAIN ANALYZE

"What did PostgreSQL THINK would happen,
and what ACTUALLY happened?"
```

This distinction is fundamental to database performance debugging.

---

# 5. A simple example

Suppose:

```sql
EXPLAIN
SELECT *
FROM customers
WHERE id = 42;
```

You might get:

```text
Index Scan using customers_pkey on customers
  Index Cond: (id = 42)
```

PostgreSQL is saying:

```text
I'll use the primary-key index
to find customer 42.
```

---

# 6. EXPLAIN ANALYZE

Now:

```sql
EXPLAIN ANALYZE
SELECT *
FROM customers
WHERE id = 42;
```

You might see something conceptually like:

```text
Index Scan using customers_pkey on customers
  (cost=0.29..8.30 rows=1 width=100)
  (actual time=0.020..0.021 rows=1 loops=1)
  Index Cond: (id = 42)
```

There are now two sets of information.

### Estimated

```text
cost=...
rows=1
```

### Actual

```text
actual time=...
rows=1
loops=1
```

This lets us compare PostgreSQL's expectations with reality.

---

# 7. The execution-plan tree

One of the most important things to understand:

> An execution plan is a **tree**.

For example:

```text
Hash Join
├── Seq Scan on orders
└── Hash
     └── Seq Scan on customers
```

Read it roughly from the leaves upward:

```text
customers
   ↓
build hash
   ↓
Hash Join
   ↑
orders
   ↑
scan orders
```

The indentation represents parent/child relationships between operations.

---

# 8. Example with a join

Query:

```sql
EXPLAIN
SELECT
    c.name,
    o.amount
FROM customers AS c
JOIN orders AS o
    ON c.id = o.customer_id;
```

Potential plan:

```text
Hash Join
  Hash Cond: (o.customer_id = c.id)
  -> Seq Scan on orders o
  -> Hash
       -> Seq Scan on customers c
```

Let's decode it.

---

# 9. `Hash Join`

The root node:

```text
Hash Join
```

tells us:

> PostgreSQL chose Hash Join as the physical join algorithm.

This connects directly to our previous lesson.

---

# 10. `Hash Cond`

You may see:

```text
Hash Cond: (o.customer_id = c.id)
```

This tells us the join condition used by the hash operation.

Conceptually:

```text
orders.customer_id
        =
customers.id
```

So:

```text
Hash Join
    |
    └── Hash Cond
```

is the part that tells us **what keys are being matched**.

---

# 11. `Seq Scan`

You may see:

```text
Seq Scan on orders
```

This means PostgreSQL is scanning the table sequentially.

Conceptually:

```text
orders
  |
  v
row 1
row 2
row 3
row 4
...
```

It is not necessarily a bad thing.

For a large query that needs a substantial portion of a table:

```text
Sequential Scan
```

can be exactly what PostgreSQL wants.

---

# 12. Sequential Scan does NOT mean slow

This is an important beginner misconception.

Seeing:

```text
Seq Scan
```

doesn't automatically mean:

> "PostgreSQL made a bad decision."

Imagine:

```text
table = 1,000,000 rows
query needs = 900,000 rows
```

Using an index to find 900,000 individual rows may be less efficient than simply reading the table sequentially.

So:

```text
Seq Scan
≠
Bad
```

It means:

> "PostgreSQL chose to read the table sequentially."

Whether that is good depends on the workload.

---

# 13. `Index Scan`

You may instead see:

```text
Index Scan using idx_orders_customer_id on orders
```

This means PostgreSQL is using an index to locate rows.

Conceptually:

```text
Query condition
      |
      v
    Index
      |
      v
Matching table rows
```

Indexes are particularly useful when the query is selective.

For example:

```sql
SELECT *
FROM orders
WHERE customer_id = 42;
```

If only 20 orders belong to customer 42 among millions of orders, an index can be very useful.

---

# 14. `Index Only Scan`

You may also see:

```text
Index Only Scan
```

This means PostgreSQL can potentially obtain the required information directly from the index without fetching the corresponding table rows in the usual way.

Conceptually:

```text
Query
  |
  v
Index
  |
  v
Required data
```

This can be efficient when the index contains everything needed and PostgreSQL's visibility information allows it.

For now, remember:

```text
Seq Scan       → read table
Index Scan     → use index to find rows
Index Only Scan → potentially satisfy query from index
```

---

# 15. Cost

You may see:

```text
(cost=0.29..100.50 rows=500 width=64)
```

The two cost values are:

```text
startup cost .. total cost
```

For example:

```text
cost=10.00..500.00
```

means roughly:

```text
startup cost = 10
total cost   = 500
```

These are **planner cost units**, not milliseconds.

This is extremely important.

```text
cost=500
```

does **not** mean:

```text
500 milliseconds
```

or:

```text
500 dollars
```

It's an internal relative cost used by PostgreSQL's planner.

---

# 16. Cost is for comparison

Suppose PostgreSQL considers:

```text
Plan A
cost = 100

Plan B
cost = 500
```

The planner generally prefers the lower estimated cost.

The important thing is:

> Cost is primarily useful for comparing plans within PostgreSQL's cost model.

Don't interpret it as actual execution time.

---

# 17. Estimated rows

You may see:

```text
rows=1000
```

This means PostgreSQL estimates that this operation will produce approximately:

```text
1,000 rows
```

For example:

```text
Hash Join
  (cost=... rows=10000 ...)
```

means the planner expects approximately 10,000 output rows from that node.

---

# 18. Actual rows

With:

```sql
EXPLAIN ANALYZE
```

you may see:

```text
actual ... rows=9500
```

Now we can compare:

```text
estimated rows = 10,000
actual rows    = 9,500
```

That's reasonably close.

But imagine:

```text
estimated rows = 10
actual rows    = 1,000,000
```

That's a huge estimation error.

And that can matter enormously for join selection.

---

# 19. Why row estimates matter so much

Remember our three algorithms:

```text
Nested Loop
Hash Join
Merge Join
```

Suppose PostgreSQL estimates:

```text
outer rows = 10
```

A Nested Loop might look very attractive.

But suppose reality is:

```text
outer rows = 10,000,000
```

Now the actual workload could be dramatically different.

Conceptually:

```text
Bad estimate
     |
     v
Wrong expectation about workload
     |
     v
Potentially poor plan choice
```

This is one of the most important reasons to inspect:

```text
estimated rows
vs
actual rows
```

---

# 20. `actual time`

With `EXPLAIN ANALYZE`, you may see:

```text
(actual time=0.020..10.500 rows=1000 loops=1)
```

The two time values represent approximately:

```text
startup time .. total time
```

So:

```text
actual time=0.020..10.500
```

means the node began producing output around 0.020 ms and finished around 10.500 ms.

These are actual measured execution times for that node.

---

# 21. `loops`

This is especially important for Nested Loop.

Suppose:

```text
Nested Loop
├── Seq Scan on customers
└── Index Scan on orders
```

You might see:

```text
Index Scan on orders
  (actual time=0.010..0.050 rows=3 loops=100)
```

That means the inner operation was executed repeatedly.

Conceptually:

```text
customer 1 → index lookup
customer 2 → index lookup
customer 3 → index lookup
...
customer 100 → index lookup
```

So:

```text
loops=100
```

means approximately 100 executions of that node.

---

# 22. Why `loops` matters

Remember Nested Loop:

```text
outer row
   ↓
inner operation
   ↓
repeat
```

If:

```text
outer rows = 10
```

you may have:

```text
inner loops ≈ 10
```

If:

```text
outer rows = 1,000,000
```

you may have:

```text
inner loops ≈ 1,000,000
```

That can completely change the performance picture.

---

# 23. A complete example

Consider:

```sql
EXPLAIN ANALYZE
SELECT
    c.name,
    o.amount
FROM customers AS c
JOIN orders AS o
    ON c.id = o.customer_id
WHERE c.id = 42;
```

A conceptual plan could look like:

```text
Nested Loop
  (cost=... rows=... )
  (actual time=... rows=... loops=1)

  -> Index Scan using customers_pkey on customers c
       (actual ... rows=1 loops=1)

  -> Index Scan using idx_orders_customer_id on orders o
       (actual ... rows=5 loops=1)
```

Read it as:

```text
Find customer 42
       ↓
1 customer
       ↓
lookup orders for customer 42
       ↓
5 orders
       ↓
produce 5 joined rows
```

This is a perfect example of why Nested Loop can be excellent for a very selective query.

---

# 24. Another example: Hash Join

Suppose:

```sql
EXPLAIN ANALYZE
SELECT
    c.name,
    o.amount
FROM customers AS c
JOIN orders AS o
    ON c.id = o.customer_id;
```

You might see:

```text
Hash Join
  Hash Cond: (o.customer_id = c.id)

  -> Seq Scan on orders o
       (actual ... rows=10000000 loops=1)

  -> Hash
       (actual ... rows=1000000 loops=1)
       Buckets: ...
       Batches: ...
       Memory Usage: ...

       -> Seq Scan on customers c
            (actual ... rows=1000000 loops=1)
```

Conceptually:

```text
customers
   ↓
Seq Scan
   ↓
Hash
   ↓
Hash Table
   ↑
   |
Hash Join
   ↑
   |
orders
   ↑
Seq Scan
```

---

# 25. `Batches` revisited

From the previous Hash Join lesson, you saw:

```text
Batches: 1
```

or:

```text
Batches: 8
```

Now `EXPLAIN ANALYZE` gives you a way to observe this.

For example:

```text
Hash
  Buckets: 262144
  Batches: 1
  Memory Usage: 45000kB
```

means conceptually:

```text
Hash operation
    |
    +-- 262144 buckets
    +-- 1 batch
    +-- ~45 MB memory
```

If you see:

```text
Batches: 16
```

the hash operation has been divided into multiple batches.

That can indicate more work than a single in-memory batch.

---

# 26. `Buffers`

Another very useful option is:

```sql
EXPLAIN (ANALYZE, BUFFERS)
SELECT ...
```

This adds information about PostgreSQL's buffer/cache activity.

For example:

```text
Buffers: shared hit=5000 read=100
```

Conceptually:

```text
shared hit
    ↓
data was already available in PostgreSQL's shared buffers

read
    ↓
data had to be read into the buffers
```

This becomes very useful when diagnosing I/O-heavy queries.

---

# 27. Why `BUFFERS` is useful

Suppose two queries have similar execution times.

Query A:

```text
Buffers:
shared hit=10000
read=10
```

Query B:

```text
Buffers:
shared hit=100
read=10000
```

The second query is doing much more physical reading.

That can help explain performance behavior.

So a common diagnostic command is:

```sql
EXPLAIN (ANALYZE, BUFFERS)
SELECT ...
```

---

# 28. Important warning about EXPLAIN ANALYZE

`EXPLAIN ANALYZE` **actually executes the query**.

Therefore:

```sql
EXPLAIN ANALYZE
DELETE FROM orders
WHERE ...;
```

will actually perform the `DELETE`.

Likewise:

```sql
EXPLAIN ANALYZE
UPDATE accounts
SET ...
WHERE ...;
```

actually performs the update.

So be careful.

For read-only queries:

```sql
EXPLAIN ANALYZE
SELECT ...
```

is usually straightforward.

For modifying statements, understand that you're executing the statement.

---

# 29. Transaction safety for experiments

When experimenting with modifying queries, you can sometimes use a transaction:

```sql
BEGIN;

EXPLAIN ANALYZE
DELETE FROM orders
WHERE customer_id = 42;

ROLLBACK;
```

The statement executes, but the transaction is rolled back afterward.

However, don't treat this as a universal safety mechanism for every production scenario.

For learning with local/test databases, it is a useful technique.

---

# 30. EXPLAIN output is a tree

This is perhaps the most important skill to develop.

Consider:

```text
Hash Join
├── Seq Scan on orders
└── Hash
     └── Seq Scan on customers
```

Don't read it as a flat list.

Read it as:

```text
                    Hash Join
                   /         \
                  /           \
        Seq Scan orders       Hash
                               |
                               v
                         Seq Scan customers
```

The leaves produce data.

The parent consumes that data.

The root produces the final result.

---

# 31. Bottom-up thinking

When reading a plan, start from the bottom.

For example:

```text
Hash Join
├── Seq Scan on orders
└── Hash
     └── Seq Scan on customers
```

Read:

### Step 1

```text
Seq Scan on customers
```

Read customers.

### Step 2

```text
Hash
```

Build a hash table.

### Step 3

```text
Seq Scan on orders
```

Read orders.

### Step 4

```text
Hash Join
```

Probe and match.

Conceptually:

```text
customers
   ↓
hash

orders
   ↓
probe

matches
```

---

# 32. Parent nodes depend on child nodes

Consider:

```text
Aggregate
└── Hash Join
    ├── Seq Scan A
    └── Hash
         └── Seq Scan B
```

The flow is:

```text
A ───────────┐
             ↓
          Hash Join
             ↑
B → Hash ────┘
             ↓
         Aggregate
             ↓
          Result
```

This becomes extremely useful once you start reading complex production query plans.

---

# 33. Common EXPLAIN nodes you should recognize

You don't need to memorize everything yet.

Start with these:

| Node                | Meaning                                |
| ------------------- | -------------------------------------- |
| `Seq Scan`          | Sequentially scan a table              |
| `Index Scan`        | Access rows through an index           |
| `Index Only Scan`   | Potentially satisfy query from index   |
| `Bitmap Index Scan` | Build bitmap of matching index entries |
| `Bitmap Heap Scan`  | Fetch table pages using bitmap         |
| `Nested Loop`       | Nested Loop join                       |
| `Hash Join`         | Hash-based join                        |
| `Hash`              | Build hash table                       |
| `Merge Join`        | Merge ordered inputs                   |
| `Sort`              | Sort rows                              |
| `Aggregate`         | Aggregate rows                         |
| `HashAggregate`     | Hash-based aggregation                 |
| `GroupAggregate`    | Grouped aggregation                    |
| `Limit`             | Limit number of rows                   |
| `Materialize`       | Materialize/reuse intermediate data    |

Don't worry about mastering all of these yet.

The important ones for our current course are:

```text
Nested Loop
Hash Join
Merge Join
```

and their supporting scan/sort/hash nodes.

---

# 34. Estimated vs actual: the most important diagnostic

Suppose you see:

```text
Hash Join
  (cost=100..500 rows=1000)
  (actual time=20..200 rows=100000 loops=1)
```

The important discrepancy is:

```text
Estimated rows = 1,000
Actual rows    = 100,000
```

That's a **100× difference**.

This should immediately make you curious.

Why did PostgreSQL expect only 1,000 rows?

Potential causes include:

* stale statistics
* data distribution that statistics don't represent well
* correlated columns
* highly selective filters that are estimated incorrectly
* skewed data
* complex query predicates

The next step isn't automatically "change the join algorithm."

First understand the estimation problem.

---

# 35. `ANALYZE`

PostgreSQL maintains statistics about table data.

You can manually refresh them with:

```sql
ANALYZE customers;
ANALYZE orders;
```

Or:

```sql
ANALYZE;
```

for the relevant database objects according to PostgreSQL's normal statistics collection behavior.

The statistics help PostgreSQL estimate:

```text
How many rows?
How selective is this condition?
How many rows will this join produce?
```

Better estimates can lead to better plans.

---

# 36. Don't optimize based on one number

Suppose you see:

```text
cost=500
```

Don't immediately say:

> "The cost is too high."

Instead look at:

```text
estimated rows
actual rows
actual time
loops
buffers
scan type
join algorithm
```

A plan should be understood as a whole.

---

# 37. A practical debugging workflow

When a query is slow:

```text
1. Run EXPLAIN
        ↓
2. Understand the chosen plan
        ↓
3. Run EXPLAIN ANALYZE
        ↓
4. Compare estimated vs actual rows
        ↓
5. Look at actual execution time
        ↓
6. Look at loops
        ↓
7. Look at scans
        ↓
8. Look at buffers if needed
        ↓
9. Find the expensive node
        ↓
10. Investigate why
```

For example:

```sql
EXPLAIN (ANALYZE, BUFFERS)
SELECT ...;
```

is a very useful starting point for read-only performance investigation.

---

# 38. A visual example

Imagine:

```text
EXPLAIN ANALYZE

Hash Join
(actual time=50..500 rows=200000 loops=1)
├── Seq Scan on orders
│   (actual time=0.1..200 rows=1000000 loops=1)
│
└── Hash
    (actual time=40..40 rows=100000 loops=1)
    └── Seq Scan on customers
        (actual time=0.1..20 rows=100000 loops=1)
```

You can reason:

```text
customers
   |
   | 100k rows
   v
 Hash
   |
   +------------------+
                      |
orders                |
1m rows               |
   |                  |
   +-------> Hash Join
                    |
                    v
                200k rows
```

Now you can start asking:

* Why did the planner choose Hash Join?
* Is 1 million orders expected?
* Is the hash side appropriately sized?
* Are estimated rows close to actual?
* Is the sequential scan reasonable?
* How much time is spent in each node?

That is the beginning of real query-plan analysis.

---

# 39. `EXPLAIN` does not tell you "the query is slow"

It tells you **what PostgreSQL plans or did**.

You have to interpret the plan.

For example:

```text
Seq Scan
```

is not automatically bad.

```text
Nested Loop
```

is not automatically bad.

```text
Hash Join
```

is not automatically good.

```text
Index Scan
```

is not automatically good.

The correct question is:

> **Does this plan efficiently process this workload?**

---

# 40. The planner's basic decision process

A simplified mental model:

```text
                 SQL Query
                     |
                     v
                PostgreSQL
                 Planner
                     |
        +------------+------------+
        |            |            |
        v            v            v
   Nested Loop   Hash Join   Merge Join
        |            |            |
        +------------+------------+
                     |
                     v
              Estimate costs
                     |
                     v
              Choose a plan
```

Then with `EXPLAIN ANALYZE`:

```text
Chosen plan
     |
     v
Actually execute
     |
     v
Measure what happened
     |
     v
Compare estimate vs actual
```

---

# 41. EXPLAIN vs EXPLAIN ANALYZE

Keep this table handy:

| Command                                 | What happens                              |
| --------------------------------------- | ----------------------------------------- |
| `EXPLAIN SELECT ...`                    | Shows estimated execution plan            |
| `EXPLAIN ANALYZE SELECT ...`            | Executes query and shows actual execution |
| `EXPLAIN (ANALYZE, BUFFERS) SELECT ...` | Actual execution + buffer/I/O information |

The simplest memory trick:

```text
EXPLAIN
→ "What do you PLAN to do?"

EXPLAIN ANALYZE
→ "Do it, then tell me what ACTUALLY happened."
```

---

# 42. Your first plan-reading checklist

When you encounter a plan, read it in this order:

### 1. What is the root operation?

```text
Hash Join?
Nested Loop?
Merge Join?
Aggregate?
```

### 2. What are the children?

```text
Seq Scan?
Index Scan?
Hash?
Sort?
```

### 3. What are the estimated rows?

```text
rows=...
```

### 4. What are the actual rows?

```text
actual ... rows=...
```

### 5. Are they wildly different?

```text
estimate ≈ actual
```

is generally more reassuring than:

```text
estimate << actual
```

### 6. How many loops?

```text
loops=...
```

### 7. How much actual time?

```text
actual time=...
```

### 8. If needed, what do buffers show?

```text
Buffers: ...
```

---

# 43. The most important concepts so far

You have now learned the conceptual foundation for PostgreSQL join execution:

```text
SQL JOIN
   |
   v
PostgreSQL Planner
   |
   +----------------+
   |                |
   v                v
Logical semantics   Physical algorithm
   |                |
   |          +-----+-----+
   |          |     |     |
   |          v     v     v
   |       Nested Hash  Merge
   |       Loop   Join  Join
   |
   v
What rows should exist?
```

And `EXPLAIN` lets you inspect the physical side:

```text
EXPLAIN
   ↓
Estimated plan

EXPLAIN ANALYZE
   ↓
Actual execution
   +
Estimated vs actual
```

---

# 44. Final mental model

```text
                 YOUR SQL
                    |
                    v
              PostgreSQL Planner
                    |
                    v
              Execution Plan
                    |
        +-----------+-----------+
        |           |           |
        v           v           v
   Nested Loop   Hash Join   Merge Join
        |           |           |
        v           v           v
      Access      Hash +      Sort/order
      rows        Probe       + walk
        |           |           |
        +-----------+-----------+
                    |
                    v
                 Result
```

And:

```text
EXPLAIN
→ planned execution

EXPLAIN ANALYZE
→ actual execution + measurements
```

> **The most valuable skill isn't memorizing EXPLAIN output. It's learning to compare what PostgreSQL expected with what actually happened.**

---

## Next

**26. Join Performance & Indexing**

We'll connect everything together and learn **when indexes help joins, which columns to index, composite indexes, foreign-key indexes, why an index may not be used, and how indexing affects Nested Loop, Hash Join, and Merge Join.**

