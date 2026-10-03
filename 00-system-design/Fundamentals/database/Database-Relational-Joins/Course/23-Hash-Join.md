# 23. Hash Join

We now move from **Nested Loop Join** to the second major PostgreSQL join algorithm:

> **Hash Join**

The simplest mental model is:

> **Build a hash table from one side, then use it to quickly find matching rows from the other side.**

---

## 1. Why do we need Hash Join?

Suppose we have:

```text
customers
+----+---------+
| id | name    |
+----+---------+
| 1  | Alice   |
| 2  | Bob     |
| 3  | Charlie |
+----+---------+

orders
+----+-------------+--------+
| id | customer_id | amount |
+----+-------------+--------+
| 101| 1           | 500    |
| 102| 3           | 200    |
| 103| 1           | 700    |
| 104| 2           | 300    |
+----+-------------+--------+
```

Query:

```sql
SELECT
    c.name,
    o.amount
FROM customers AS c
JOIN orders AS o
    ON c.id = o.customer_id;
```

Conceptually, we want:

```text
customer.id = order.customer_id
```

A Nested Loop could repeatedly search for matching orders.

But when both tables are large, repeatedly searching can become expensive.

Hash Join takes a different approach.

---

# 2. Core idea

Hash Join works roughly like this:

```text
                HASH JOIN

        Build phase
        ------------
        customers
            |
            v
      hash(customer.id)
            |
            v
       Hash Table
       +---------+
       | bucket  |
       | bucket  |
       | bucket  |
       | bucket  |
       +---------+

        Probe phase
        -----------
        orders
           |
           v
    hash(order.customer_id)
           |
           v
      find bucket
           |
           v
       matching
       customer
```

Instead of repeatedly scanning/searching the entire `customers` table, PostgreSQL builds a structure that allows matching rows to be found efficiently.

---

# 3. Two important sides: Build and Probe

Hash Join has two conceptual inputs:

```text
          Hash Join
         /         \
        /           \
   Build side     Probe side
```

### Build side

PostgreSQL reads one input and builds a **hash table** using the join key.

For example:

```text
customers.id
```

### Probe side

PostgreSQL reads the other input and uses its join key to look into the hash table.

For example:

```text
orders.customer_id
```

So:

```text
customers
   |
   | build
   v
Hash Table

orders
   |
   | probe
   v
Hash Table
```

---

# 4. What is a hash table?

A hash table is a data structure that lets us associate a value with a location.

Imagine:

```text
customer.id

1
2
3
4
5
```

A hash function can turn those values into bucket locations.

Conceptually:

```text
hash(1) → bucket 3
hash(2) → bucket 7
hash(3) → bucket 1
hash(4) → bucket 5
hash(5) → bucket 2
```

The actual PostgreSQL implementation is more sophisticated, but this is the important mental model.

We can visualize:

```text
Customer ID
     |
     v
   hash()
     |
     v
+----------------+
| Bucket 0       |
| Bucket 1 ---> 3|
| Bucket 2 ---> 5|
| Bucket 3 ---> 1|
| Bucket 4       |
| Bucket 5 ---> 4|
| Bucket 6       |
| Bucket 7 ---> 2|
+----------------+
```

---

# 5. Build phase

Suppose:

```text
customers

id
--
1
2
3
4
```

PostgreSQL conceptually does:

```text
Read customer 1
    ↓
hash(1)
    ↓
put customer 1 into bucket

Read customer 2
    ↓
hash(2)
    ↓
put customer 2 into bucket

Read customer 3
    ↓
hash(3)
    ↓
put customer 3 into bucket

Read customer 4
    ↓
hash(4)
    ↓
put customer 4 into bucket
```

Eventually:

```text
             Hash Table

bucket 0
bucket 1 → customer 3
bucket 2 → customer 1
bucket 3
bucket 4 → customer 4
bucket 5 → customer 2
```

The exact bucket assignment isn't important.

The important idea is:

> PostgreSQL has created a lookup structure based on the join key.

---

# 6. Probe phase

Now PostgreSQL starts reading orders:

```text
orders

customer_id
-----------
1
3
1
2
```

For the first order:

```text
order.customer_id = 1
```

PostgreSQL calculates:

```text
hash(1)
```

Then goes directly to the corresponding bucket.

```text
orders.customer_id
        |
        v
      hash(1)
        |
        v
     bucket
        |
        v
   customer id = 1
```

Match found.

Then:

```text
order.customer_id = 3
        |
        v
      hash(3)
        |
        v
     bucket
        |
        v
   customer id = 3
```

Match found.

And so on.

---

# 7. Complete picture

The whole process can be visualized as:

```text
                  HASH JOIN
                     |
          +----------+----------+
          |                     |
          v                     v
      customers               orders
          |                     |
          | BUILD               | PROBE
          v                     v
      hash(id)            hash(customer_id)
          |                     |
          v                     |
    +-------------+             |
    | Hash Table  |<------------+
    +-------------+
          |
          v
       Matches
```

This is the core of Hash Join.

---

# 8. Why is this useful?

Consider:

```text
customers = 1,000,000 rows
orders    = 10,000,000 rows
```

A naive Nested Loop approach could conceptually involve enormous amounts of repeated work.

Hash Join can instead:

```text
Build hash table
      +
Scan/probe other table
```

So the work is much more like:

```text
Read table A
      ↓
Build hash structure
      ↓
Read table B
      ↓
Probe hash structure
```

This is particularly attractive for **large equality joins**.

---

# 9. Hash Join and equality

This is extremely important.

Hash Join naturally works with equality:

```sql
ON c.id = o.customer_id
```

because hashing is based on equality.

For example:

```text
hash(42)
```

can find values that are equal to:

```text
42
```

But consider:

```sql
ON a.salary > b.minimum_salary
```

That's not a straightforward equality lookup.

There isn't a simple:

```text
hash(salary)
```

operation that answers:

> "Find all rows where salary is greater than this value."

Therefore Hash Join is primarily useful for **equality join conditions**.

---

# 10. Equi-join

A join based on equality is called an **equi-join**.

Example:

```sql
SELECT *
FROM customers AS c
JOIN orders AS o
    ON c.id = o.customer_id;
```

The important part is:

```sql
c.id = o.customer_id
```

Hash Join is particularly well suited to this.

Another example:

```sql
SELECT *
FROM employees AS e
JOIN departments AS d
    ON e.department_id = d.id;
```

Again:

```text
employee.department_id
          =
department.id
```

Perfect candidate for hashing.

---

# 11. Hash collisions

You might wonder:

> What if two values produce the same bucket?

That's called a **hash collision**.

For example:

```text
hash(10) → bucket 3
hash(25) → bucket 3
```

The hash table can contain multiple entries in the same bucket.

Conceptually:

```text
Bucket 3
   |
   +---- customer 10
   |
   +---- customer 25
```

When probing:

```text
hash(25)
   ↓
bucket 3
   ↓
check candidates
   ↓
25 = 25
   ↓
MATCH
```

So a hash collision does not mean the join is incorrect.

The hash value narrows down the candidates; PostgreSQL still verifies the actual join condition.

---

# 12. Build side vs smaller side

You may hear:

> "Hash Join builds the hash table on the smaller table."

This is a useful rule of thumb, but don't treat it as an absolute SQL rule.

The PostgreSQL planner estimates costs and chooses the build side.

Why is a smaller build side attractive?

Because the hash table needs memory.

For example:

```text
Table A = 100 MB
Table B = 5 GB
```

Building the hash table from:

```text
100 MB
```

is generally much more manageable than building one from:

```text
5 GB
```

So conceptually:

```text
        Smaller input
             |
             v
       Build Hash Table
             |
             v
        Larger input
             |
             v
           Probe
```

The actual planner decision depends on statistics, costs, filters, available memory, and the overall plan.

---

# 13. Filters can change the build side

Consider:

```sql
SELECT *
FROM customers AS c
JOIN orders AS o
    ON c.id = o.customer_id
WHERE c.country = 'India';
```

Suppose:

```text
customers = 10 million rows
```

but after filtering:

```text
country = 'India'
```

only:

```text
100,000 rows
```

remain.

The effective input to the join may therefore be much smaller.

Conceptually:

```text
10,000,000 customers
        |
        | WHERE country = 'India'
        v
100,000 customers
        |
        | build
        v
   Hash Table
```

This is one reason query optimization is about **intermediate result sizes**, not just base table sizes.

---

# 14. What PostgreSQL EXPLAIN looks like

Eventually, you'll learn `EXPLAIN` in detail.

For now, recognize this shape:

```text
Hash Join
  Hash Cond: (o.customer_id = c.id)
  -> Seq Scan on orders o
  -> Hash
       -> Seq Scan on customers c
```

The structure is important.

```text
Hash Join
├── orders
└── Hash
     └── customers
```

This means conceptually:

```text
customers
   ↓
build hash
   ↓
Hash Table
   ↑
probe
   ↑
orders
```

---

# 15. `Hash`

In an execution plan, you may see:

```text
Hash
```

Don't confuse:

```text
Hash
```

with:

```text
Hash Join
```

They represent different parts of the plan.

### Hash

This node builds the hash table.

```text
Hash
└── Seq Scan on customers
```

means roughly:

```text
scan customers
     ↓
build hash table
```

### Hash Join

This performs the actual matching:

```text
Hash Join
├── orders
└── Hash
     └── customers
```

So:

```text
Hash
    = build the hash structure

Hash Join
    = probe + match using that structure
```

---

# 16. Memory matters

Hash tables consume memory.

PostgreSQL has a setting called:

```sql
work_mem
```

It controls the amount of memory available for certain operations, including hash-based operations.

For example:

```sql
SHOW work_mem;
```

might return:

```text
4MB
```

The exact value depends on your PostgreSQL configuration.

---

# 17. What if the hash table doesn't fit in memory?

This is an important real-world issue.

Suppose PostgreSQL wants to build a hash table containing a huge amount of data.

But:

```text
Hash table size
     >
available memory
```

PostgreSQL can't simply assume unlimited RAM.

It can divide the work into **batches**.

Conceptually:

```text
Huge input
    |
    v
+-------------------+
| Batch 1            |
| Batch 2            |
| Batch 3            |
| Batch 4            |
+-------------------+
```

Then process them separately.

This allows PostgreSQL to perform the join without requiring the entire hash table to fit in memory simultaneously.

---

# 18. Batches in EXPLAIN ANALYZE

You may see something like:

```text
Hash
  Buckets: 131072
  Batches: 4
  Memory Usage: 5000kB
```

For now, understand these at a high level:

| Term           | Meaning                                               |
| -------------- | ----------------------------------------------------- |
| `Buckets`      | Hash table bucket structure                           |
| `Batches`      | Number of partitions/batches used to process the hash |
| `Memory Usage` | Memory used by the hash structure                     |

The particularly important one is:

```text
Batches: 1
```

versus:

```text
Batches: 8
```

Conceptually:

```text
Batches: 1

Everything fits into the relevant in-memory hash structure.
```

Whereas:

```text
Batches: 8

The operation has been divided into multiple batches.
```

Multiple batches can involve additional work and potentially disk I/O, so it can make the operation more expensive.

---

# 19. Why `work_mem` matters

Imagine:

```text
Hash table needs: 500 MB
```

but the operation effectively has only:

```text
100 MB
```

available.

PostgreSQL may need to batch the operation.

Increasing `work_mem` can sometimes allow more of the hash operation to stay in memory.

But **don't blindly increase `work_mem` globally**.

Why?

Because `work_mem` applies to operations, and a query can contain multiple memory-consuming operations.

For example:

```text
Query
 |
 +-- Hash Join
 |
 +-- Sort
 |
 +-- Hash Aggregate
 |
 +-- another Hash Join
```

If every operation gets a large amount of memory, concurrent queries can consume substantial RAM.

So:

> `work_mem` is an important tuning parameter, but increasing it is not automatically a performance fix.

---

# 20. Hash Join doesn't require an index

This is one of the biggest differences from the common Indexed Nested Loop scenario.

Consider:

```sql
SELECT *
FROM customers AS c
JOIN orders AS o
    ON c.id = o.customer_id;
```

A Hash Join can work even if:

```text
orders.customer_id
```

has no index.

Why?

Because Hash Join can do:

```text
Sequentially read customers
        ↓
Build hash table

Sequentially read orders
        ↓
Probe hash table
```

It doesn't need an index lookup for every order.

---

# 21. Hash Join vs Index Nested Loop

Suppose:

```text
customers = 10 rows
orders    = 100 million rows
```

and we filter to one customer:

```sql
WHERE c.id = 42
```

An index on:

```sql
orders(customer_id)
```

could make a Nested Loop with index lookups very attractive:

```text
customer 42
    |
    v
Index lookup
    |
    v
matching orders
```

But suppose instead:

```text
customers = 10 million
orders    = 100 million
```

and we're joining most of both tables.

Doing millions of individual index lookups may not be attractive.

A Hash Join can instead do:

```text
Build hash
     +
Sequentially scan/probe
```

This is why PostgreSQL chooses algorithms based on the **shape of the query and data**.

---

# 22. A useful comparison

| Characteristic       | Nested Loop                          | Hash Join                    |
| -------------------- | ------------------------------------ | ---------------------------- |
| Main idea            | Repeated inner lookup                | Build hash + probe           |
| Great for            | Small/selective outer input          | Large equality joins         |
| Requires index?      | No                                   | No                           |
| Benefits from index? | Often                                | Not required                 |
| Equality joins       | Yes                                  | Excellent fit                |
| Range joins          | Yes                                  | Not naturally suited         |
| Memory usage         | Usually lower                        | Hash table requires memory   |
| Large inputs         | Can be expensive depending on access | Often effective for equality |
| Main concern         | Repeated inner work                  | Hash memory/batching         |

Remember:

> This is intuition, not a guarantee.

---

# 23. Hash Join and duplicate keys

Suppose:

```text
customers

id
--
1
```

and:

```text
orders

id   customer_id
---  -----------
101  1
102  1
103  1
```

The hash table contains:

```text
hash(1)
   |
   +-- customer Alice
```

Now three orders probe the same hash location:

```text
order 101 → customer 1 → Alice
order 102 → customer 1 → Alice
order 103 → customer 1 → Alice
```

Result:

```text
Alice | 101
Alice | 102
Alice | 103
```

Hash Join does **not eliminate duplicates**.

This connects directly to the earlier concept of:

> **Join cardinality and join explosion.**

The algorithm used to find matches does not change the logical result.

---

# 24. Hash Join does not change JOIN semantics

This is extremely important.

Suppose your SQL says:

```sql
INNER JOIN
```

PostgreSQL might execute it using:

```text
Hash Join
```

If you change the plan to:

```text
Nested Loop
```

the intended result is still the same.

The difference is **how PostgreSQL finds the matches**.

```text
SQL semantics
      |
      v
"What rows should match?"
      |
      v
JOIN condition


Physical algorithm
      |
      v
"How should PostgreSQL find them?"
      |
      +---- Nested Loop
      +---- Hash Join
      +---- Merge Join
```

So:

> Hash Join is an execution strategy, not a different type of SQL JOIN.

---

# 25. A practical example

Consider:

```sql
CREATE TABLE departments (
    id BIGINT PRIMARY KEY,
    name TEXT
);

CREATE TABLE employees (
    id BIGINT PRIMARY KEY,
    name TEXT,
    department_id BIGINT
);
```

Query:

```sql
SELECT
    e.name AS employee,
    d.name AS department
FROM employees AS e
JOIN departments AS d
    ON e.department_id = d.id;
```

Conceptually PostgreSQL could do:

```text
departments
     |
     | build
     v
 Hash Table
     ^
     |
     | probe
     |
employees
```

If the planner chooses Hash Join, you might see:

```text
Hash Join
├── Seq Scan on employees
└── Hash
     └── Seq Scan on departments
```

The exact plan depends on the actual statistics and costs.

---

# 26. When Hash Join is a strong candidate

Think about Hash Join when you have:

### Large tables

```text
A = millions of rows
B = millions of rows
```

### Equality relationship

```sql
ON A.id = B.a_id
```

### Sequential processing is reasonable

```text
scan A
scan B
```

### Building a hash table is affordable

```text
hash table fits reasonably in memory
```

A simplified mental picture:

```text
Large A
   |
   | build
   v
Hash Table
   ^
   |
   | probe
   |
Large B
```

---

# 27. When Hash Join may be less attractive

It may not be ideal when:

### The join is a range condition

```sql
ON a.value >= b.min_value
AND a.value < b.max_value
```

Hashing doesn't naturally solve this.

### The build side is huge

```text
Huge table
    ↓
Huge hash table
    ↓
memory pressure
    ↓
possible batching
```

### A highly selective indexed lookup exists

For example:

```text
1 customer
     ↓
indexed lookup
     ↓
5 matching orders
```

A Nested Loop with an index may be very efficient.

---

# 28. Don't force Hash Join blindly

PostgreSQL has planner settings that can influence join strategies, but normally you should **not start by forcing a particular algorithm**.

The better workflow is:

```text
Query
  ↓
EXPLAIN ANALYZE
  ↓
Understand chosen plan
  ↓
Look at estimates vs actuals
  ↓
Check indexes/statistics/query shape
  ↓
Tune if necessary
```

The fact that you see:

```text
Hash Join
```

doesn't mean:

> "Something is wrong."

Likewise:

```text
Nested Loop
```

doesn't automatically mean:

> "Something is wrong."

The important question is whether the chosen plan performs well for the actual workload.

---

# 29. Very important: estimated vs actual rows

Suppose PostgreSQL expects:

```text
rows=100
```

but actually gets:

```text
actual rows=1,000,000
```

That is a huge estimation error.

The planner may have selected Hash Join based on incorrect assumptions.

Or it might select Nested Loop when the actual data is much larger than expected.

Conceptually:

```text
Statistics
    ↓
Planner estimates
    ↓
Choose algorithm
    ↓
Actual execution
```

If the statistics are inaccurate:

```text
bad estimate
    ↓
potentially bad plan
```

This is one reason PostgreSQL statistics are so important.

---

# 30. Hash Join mental model

When you see:

```sql
JOIN ... ON a.id = b.a_id
```

think:

```text
        HASH JOIN

      BUILD              PROBE
        |                   |
        v                   v
    table A              table B
        |                   |
        v                   v
      hash()             hash()
        |                   |
        +-------> Hash <----+
                  Table
                    |
                    v
                 matches
```

The two words to remember are:

> **BUILD → PROBE**

---

# 31. Interview-style explanation

If asked:

> What is a Hash Join?

A good answer is:

> A Hash Join is a PostgreSQL join algorithm that builds a hash table from one input using the join key, then scans the other input and probes that hash table to find matching rows. It is particularly effective for large equality joins and does not require an index on the join column, but it requires memory for the hash table and may use batching when the hash operation cannot fit entirely in memory.

---

# 32. Hash Join vs Nested Loop — mental shortcut

Use this mental model:

```text
Nested Loop

Small/selective outer
        |
        v
   repeated lookup
        |
        v
      inner
```

versus:

```text
Hash Join

Build hash
    +
scan/probe
    |
    v
matches
```

Or even shorter:

```text
Nested Loop → "Look up again and again"

Hash Join   → "Build once, probe many times"
```

That's the key intuition.

---

# 33. What to look for in a plan

When you eventually inspect:

```sql
EXPLAIN ANALYZE
```

and see:

```text
Hash Join
  Hash Cond: (...)
  ...
  Hash
    ...
```

ask yourself:

1. **Which side is being hashed?**
2. **What is the join key?**
3. **How many rows are entering the hash?**
4. **How many rows are being probed?**
5. **How much memory is used?**
6. **How many batches are used?**
7. **Are estimated rows close to actual rows?**
8. **Is the actual execution time reasonable?**

For now, you don't need to memorize every EXPLAIN field.

Just recognize:

```text
Hash Join
   |
   +-- Build a hash structure
   |
   +-- Probe it with the other input
   |
   +-- Equality joins are the natural use case
   |
   +-- Memory matters
   |
   +-- Batches can appear when memory is insufficient
```

---

# 34. Final mental model

```text
                  HASH JOIN
                      |
            +---------+---------+
            |                   |
        BUILD SIDE          PROBE SIDE
            |                   |
            v                   v
        hash(key)           hash(key)
            |                   |
            v                   |
      +-------------+            |
      | Hash Table  |<-----------+
      +-------------+
            |
            v
         verify
         matches
            |
            v
          output
```

### Remember these 5 things

1. **Hash Join = Build + Probe**
2. **Build side creates the hash table**
3. **Probe side searches that hash table**
4. **Equality joins are the natural fit**
5. **Memory matters; multiple batches can occur**

> **Nested Loop:** repeatedly look for matches.
> **Hash Join:** build a lookup structure once, then probe it.

The next concept is **24. Merge Join**, where PostgreSQL takes a very different approach: **sort both inputs by the join key and then walk through them together**.

