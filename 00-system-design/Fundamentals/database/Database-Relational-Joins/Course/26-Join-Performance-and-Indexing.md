# 26. Join Performance & Indexing

Now we connect the concepts we've learned so far:

```text
JOIN
 ↓
Cardinality
 ↓
Join algorithm
 ↓
Nested Loop / Hash Join / Merge Join
 ↓
Indexes
 ↓
Execution plan
 ↓
EXPLAIN ANALYZE
```

The big question is:

> **How do indexes affect JOIN performance, and which columns should we index?**

---

# 1. First: What is an index?

An index is a data structure PostgreSQL maintains to find rows more efficiently.

Imagine:

```text
customers

id
---
1
2
3
4
5
...
1,000,000
```

Without an index, finding:

```sql
SELECT *
FROM customers
WHERE id = 500000;
```

may require scanning many rows.

With an index:

```text
Query
  |
  v
Index
  |
  v
id = 500000
  |
  v
Table row
```

The index acts somewhat like a book's index.

Instead of:

```text
Read every page
```

you can:

```text
Find the indexed value
      ↓
Find where the row is
      ↓
Read the row
```

---

# 2. The most important join-index example

Consider:

```text
customers
---------
id
name

orders
------
id
customer_id
amount
```

Relationship:

```text
customers.id
     |
     | 1:N
     v
orders.customer_id
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

The obvious question is:

> Should `orders.customer_id` have an index?

Often, yes.

For example:

```sql
CREATE INDEX idx_orders_customer_id
ON orders(customer_id);
```

---

# 3. Why index the foreign-key column?

Consider:

```text
customers
1 Alice
2 Bob
3 Charlie

orders
101 → 1
102 → 1
103 → 1
104 → 2
105 → 3
```

Suppose PostgreSQL has one customer:

```text
customer.id = 1
```

and needs to find:

```text
orders.customer_id = 1
```

With an index:

```text
customer 1
    |
    v
orders.customer_id index
    |
    v
101, 102, 103
```

Without an index, PostgreSQL may need to scan the orders table to find those rows.

This is particularly relevant to **Nested Loop + Index Scan**.

---

# 4. Nested Loop + index

Recall Nested Loop:

```text
Outer row
   ↓
Find matching inner rows
   ↓
Repeat
```

Now add an index:

```text
customers
   |
   v
customer 1
   |
   v
Index lookup
   |
   v
orders.customer_id = 1
```

Conceptually:

```text
        Nested Loop
             |
       +-----+-----+
       |           |
       v           v
 customers     orders index
       |           |
       +-----------+
             |
          matches
```

This can be extremely effective when the outer side is small.

---

# 5. Why indexes don't always help

This is one of the most important database concepts:

> **An index existing does not mean PostgreSQL should use it.**

Suppose:

```text
orders = 100 million rows
```

and your query needs:

```text
90 million rows
```

Using the index to retrieve almost every row may be more expensive than:

```text
Sequentially read the table
```

So PostgreSQL might choose:

```text
Seq Scan
```

instead of:

```text
Index Scan
```

This can be completely correct.

---

# 6. Selectivity

A key concept is **selectivity**.

Suppose:

```text
orders = 10,000,000
```

Query:

```sql
WHERE customer_id = 42
```

If customer 42 has:

```text
10 orders
```

that's highly selective.

```text
10 / 10,000,000
```

Very small fraction.

An index can be useful.

But suppose:

```text
customer_id = 42
```

matches:

```text
5,000,000 orders
```

That's much less selective.

Reading a large portion of the table through the index may not be worthwhile.

---

# 7. Indexes and Hash Join

A common misconception:

> "Hash Join needs an index."

It does not.

For:

```sql
SELECT *
FROM customers AS c
JOIN orders AS o
    ON c.id = o.customer_id;
```

Hash Join can do:

```text
customers
    |
    v
Seq Scan
    |
    v
Build Hash
    |
    v
Hash Table

orders
    |
    v
Seq Scan
    |
    v
Probe
```

No index is required.

So:

```text
Hash Join
    ≠
Requires index
```

---

# 8. Indexes and Merge Join

Indexes can be particularly interesting for Merge Join because an index can provide ordering.

Suppose:

```sql
CREATE INDEX idx_orders_customer_id
ON orders(customer_id);
```

The index provides access to values in key order.

Conceptually:

```text
orders index

1
1
1
2
2
3
4
4
5
...
```

This ordering can potentially help a Merge Join.

```text
customers ordered
        +
orders ordered
        |
        v
   Merge Join
```

So:

```text
Hash Join
→ index not required

Merge Join
→ indexes can help provide ordering

Nested Loop
→ indexes can make repeated lookups efficient
```

---

# 9. The foreign-key indexing rule

Suppose:

```sql
CREATE TABLE customers (
    id BIGINT PRIMARY KEY
);

CREATE TABLE orders (
    id BIGINT PRIMARY KEY,
    customer_id BIGINT REFERENCES customers(id)
);
```

A very common design is:

```sql
CREATE INDEX idx_orders_customer_id
ON orders(customer_id);
```

Why?

Because the foreign key establishes the relationship:

```text
orders.customer_id
        ↓
customers.id
```

but:

> **A foreign-key constraint does not automatically mean you have the index you want on the referencing column.**

The primary key on:

```text
customers.id
```

already has an index because PostgreSQL creates an index to enforce the primary-key constraint.

But the referencing column:

```text
orders.customer_id
```

does not automatically get a corresponding index merely because it is a foreign key.

---

# 10. Why foreign-key indexes matter beyond SELECT JOINs

An index on:

```text
orders.customer_id
```

can also help operations involving the relationship.

For example:

```sql
DELETE FROM customers
WHERE id = 42;
```

PostgreSQL must consider whether orders reference customer 42 when enforcing the foreign-key constraint.

An index on:

```text
orders.customer_id
```

can make finding referencing rows more efficient.

So indexing foreign-key columns is often useful for:

```text
JOINs
+
foreign-key checks
+
queries filtering by the foreign key
```

But again, actual indexing decisions depend on workload.

---

# 11. Primary key vs foreign key indexes

Consider:

```text
customers
---------
id PRIMARY KEY

orders
------
id PRIMARY KEY
customer_id FOREIGN KEY
```

Think:

```text
customers.id
    ↓
PRIMARY KEY
    ↓
index exists

orders.customer_id
    ↓
FOREIGN KEY
    ↓
index NOT automatically guaranteed
```

So a common setup is:

```sql
CREATE INDEX idx_orders_customer_id
ON orders(customer_id);
```

---

# 12. A simple join performance example

Without an index:

```sql
SELECT
    c.name,
    o.amount
FROM customers AS c
JOIN orders AS o
    ON c.id = o.customer_id
WHERE c.id = 42;
```

A possible strategy could be:

```text
Find customer 42
       ↓
scan orders
       ↓
check every order
       ↓
keep customer_id = 42
```

With:

```sql
CREATE INDEX idx_orders_customer_id
ON orders(customer_id);
```

a possible strategy becomes:

```text
Find customer 42
       ↓
Index lookup
       ↓
orders.customer_id = 42
       ↓
only matching orders
```

This is exactly the kind of situation where:

```text
Nested Loop
+
Index Scan
```

can be powerful.

---

# 13. But PostgreSQL may still choose Hash Join

Even after creating:

```sql
CREATE INDEX idx_orders_customer_id
ON orders(customer_id);
```

PostgreSQL might still choose:

```text
Hash Join
```

That's not necessarily a problem.

Suppose the query joins:

```text
millions of customers
+
millions of orders
```

and retrieves a large portion of both.

The planner might conclude:

```text
Sequential scan
+
Hash Join
```

is cheaper than:

```text
Index scan
+
millions of random/index accesses
```

This is why:

> **Indexes provide options. The planner chooses whether those options are worthwhile.**

---

# 14. Composite indexes

Now suppose your join condition is:

```sql
ON o.customer_id = c.id
AND o.status = 'SUCCESS'
```

You might have:

```sql
CREATE INDEX idx_orders_customer_id_status
ON orders(customer_id, status);
```

This is a **composite index**.

It indexes multiple columns together.

Conceptually:

```text
(customer_id, status)
```

rather than:

```text
customer_id
```

alone.

---

# 15. Why column order matters

Consider:

```sql
CREATE INDEX idx_orders_customer_status
ON orders(customer_id, status);
```

Think of it roughly as:

```text
customer_id
    ↓
  status
```

The ordering is primarily by:

```text
customer_id
```

then within that:

```text
status
```

So this index is naturally useful for queries involving:

```sql
WHERE customer_id = 42
```

and:

```sql
WHERE customer_id = 42
  AND status = 'SUCCESS'
```

But it is not equivalent to having:

```sql
(status, customer_id)
```

Those two indexes have different ordering properties and usefulness.

---

# 16. Example

Suppose:

```sql
CREATE INDEX idx_orders_customer_status
ON orders(customer_id, status);
```

Query:

```sql
SELECT *
FROM orders
WHERE customer_id = 42
  AND status = 'SUCCESS';
```

The index aligns well with the predicates.

Conceptually:

```text
Index
(customer_id, status)

42
 ├── FAILED
 ├── PENDING
 └── SUCCESS
```

The exact physical representation is more complex, but the mental model is enough for now.

---

# 17. Composite index for joins

Suppose:

```sql
SELECT *
FROM customers AS c
JOIN orders AS o
    ON c.id = o.customer_id
   AND o.status = 'SUCCESS';
```

A potentially useful index is:

```sql
CREATE INDEX idx_orders_customer_status
ON orders(customer_id, status);
```

because the query uses:

```text
customer_id
+
status
```

But whether PostgreSQL actually uses it depends on the query, table size, selectivity, statistics, and cost.

---

# 18. Join condition + filtering

Consider:

```sql
SELECT *
FROM customers AS c
JOIN orders AS o
    ON c.id = o.customer_id
WHERE o.status = 'SUCCESS';
```

A potentially useful index could be:

```text
orders(customer_id, status)
```

or in some workloads:

```text
orders(status, customer_id)
```

Which ordering is better depends on how the query workload filters and joins.

There is no universal rule:

> "Always put the join column first."

Index design is workload-dependent.

---

# 19. Don't create indexes on everything

It is tempting to think:

```text
More indexes
    =
Faster queries
```

That's false.

Indexes have costs.

Every index can add overhead to:

```text
INSERT
UPDATE
DELETE
```

because PostgreSQL may need to maintain the index.

They also consume storage.

Conceptually:

```text
More indexes
     |
     +---- SELECT can benefit
     |
     +---- INSERT/UPDATE/DELETE have more maintenance
     |
     +---- More disk usage
```

So indexes are a trade-off.

---

# 20. Indexes and UPDATE

Suppose:

```sql
UPDATE orders
SET customer_id = 50
WHERE id = 100;
```

If:

```text
customer_id
```

is indexed, changing its value may require index maintenance.

So:

```text
Read performance
      ↕
Write/maintenance cost
```

is part of index design.

---

# 21. The "index every foreign key" rule

You may hear:

> "Always index every foreign key."

It's a useful practical guideline, especially for frequently joined relationships, but don't treat it as an absolute law.

Ask:

```text
Is this relationship frequently queried?
Is the column used in joins?
Is it used in filtering?
Are foreign-key checks important for this workload?
Is the table heavily written?
```

Then make the indexing decision based on actual workload.

For many OLTP schemas, indexing foreign-key columns is common and useful.

---

# 22. Indexing and join direction

Consider:

```sql
FROM customers AS c
JOIN orders AS o
    ON c.id = o.customer_id
```

and:

```sql
FROM orders AS o
JOIN customers AS c
    ON o.customer_id = c.id
```

For an `INNER JOIN`, these express the same logical relationship.

The planner can choose an execution order independently.

So don't think:

```text
customers written first
    ↓
must be processed first
```

That is not necessarily true.

The planner may reorder joins.

---

# 23. Join order matters

Consider:

```text
A
 |
 +---- B
 |
 +---- C
```

There can be multiple ways to execute it.

For example:

```text
(A JOIN B) JOIN C
```

or:

```text
A JOIN (B JOIN C)
```

The planner evaluates possible strategies within its planning limits.

Indexes can influence the cost of different access paths, but they don't dictate the entire plan.

---

# 24. Selective filters can make indexes powerful

Consider:

```sql
SELECT
    c.name,
    o.amount
FROM customers AS c
JOIN orders AS o
    ON c.id = o.customer_id
WHERE c.email = 'alice@example.com';
```

Suppose:

```text
customers = 10 million
```

and:

```text
email is unique
```

Then:

```text
email index
    ↓
1 customer
    ↓
orders.customer_id index
    ↓
matching orders
```

This could make an indexed Nested Loop very efficient.

Conceptually:

```text
email
 ↓
1 customer
 ↓
customer_id index
 ↓
few orders
```

This is a classic highly selective access pattern.

---

# 25. Why `UNIQUE` matters

Suppose:

```sql
CREATE UNIQUE INDEX idx_customers_email
ON customers(email);
```

Now PostgreSQL knows:

```text
one email → at most one customer
```

That is useful information for both:

* data integrity
* query planning

It tells PostgreSQL something about cardinality.

This connects directly to our earlier lesson:

> **Cardinality affects join performance.**

---

# 26. Indexes and cardinality

Suppose PostgreSQL knows:

```text
customers.email
```

is unique.

Then:

```sql
WHERE email = 'alice@example.com'
```

can be estimated as approximately:

```text
0 or 1 row
```

That's very different from a column where:

```text
email
```

could match millions of rows.

So constraints and indexes can provide important information about the data model.

---

# 27. Index-only scans and joins

Sometimes an index contains all the columns required for part of a query.

For example:

```sql
CREATE INDEX idx_orders_customer_amount
ON orders(customer_id, amount);
```

If the query only needs:

```text
customer_id
amount
```

PostgreSQL may potentially use an:

```text
Index Only Scan
```

instead of fetching table rows in the usual way.

Conceptually:

```text
Index
+----------------------+
| customer_id | amount |
+----------------------+
          |
          v
      Query result
```

This can reduce table access.

But whether it actually becomes an Index Only Scan depends on visibility information and other details.

---

# 28. Don't assume an index fixes a bad join

Suppose your query accidentally says:

```sql
JOIN orders AS o
    ON c.id = o.id;
```

instead of:

```sql
JOIN orders AS o
    ON c.id = o.customer_id;
```

Adding an index doesn't fix the logical error.

This is a critical principle:

> **Correctness comes before performance.**

First establish:

```text
Correct relationship
      ↓
Correct grain
      ↓
Correct result
      ↓
Then optimize
```

---

# 29. Don't use `DISTINCT` to fix join explosion

Suppose:

```text
customers
    |
    +---- orders
    |
    +---- tickets
```

and the query creates:

```text
3 orders × 4 tickets = 12 rows
```

Adding:

```sql
SELECT DISTINCT ...
```

may hide some repeated output, but it doesn't necessarily fix the underlying join structure.

Instead ask:

```text
Why did the join produce 12 combinations?
What is the intended grain?
Should I aggregate orders first?
Should I aggregate tickets first?
```

This is often much more important than adding an index.

---

# 30. How to investigate a join

Suppose this query is slow:

```sql
SELECT
    c.name,
    o.amount
FROM customers AS c
JOIN orders AS o
    ON c.id = o.customer_id
WHERE c.id = 42;
```

Don't immediately create indexes randomly.

Start with:

```sql
EXPLAIN (ANALYZE, BUFFERS)
SELECT
    c.name,
    o.amount
FROM customers AS c
JOIN orders AS o
    ON c.id = o.customer_id
WHERE c.id = 42;
```

Then ask:

```text
1. What join algorithm was chosen?
2. What scan was used?
3. How many rows were estimated?
4. How many rows actually appeared?
5. How many loops?
6. How much time?
7. How much I/O?
8. Is an index being used?
9. If not, why might Seq Scan be cheaper?
```

---

# 31. Example diagnosis

Imagine you get:

```text
Nested Loop
├── Index Scan on customers
│    actual rows=1
│
└── Index Scan on orders
     Index Cond: (customer_id = c.id)
     actual rows=5
```

This is conceptually:

```text
1 customer
   ↓
index lookup
   ↓
5 orders
```

That's a very sensible shape for this query.

---

# 32. Another diagnosis

Imagine instead:

```text
Hash Join
├── Seq Scan on orders
│    actual rows=50,000,000
│
└── Hash
     └── Seq Scan on customers
          actual rows=10,000,000
```

If the query really needs a huge fraction of both tables, this might be reasonable.

Don't say:

> "There are indexes, so why aren't they used?"

Instead ask:

> "Would using those indexes actually be cheaper than scanning these huge portions of the tables?"

---

# 33. Index Scan vs Bitmap Heap Scan

There's another important access strategy you'll frequently encounter:

```text
Bitmap Index Scan
Bitmap Heap Scan
```

Conceptually:

```text
Index
  |
  v
Bitmap of matching locations
  |
  v
Table pages
  |
  v
Rows
```

This can be useful when a query needs more rows than a typical highly selective Index Scan, but not enough rows to justify a full sequential scan.

So PostgreSQL has more choices than simply:

```text
Index Scan
vs
Seq Scan
```

There is also:

```text
Bitmap Index Scan
+
Bitmap Heap Scan
```

---

# 34. Three rough levels of selectivity

Think conceptually:

```text
Very few rows
     ↓
Index Scan
```

```text
Moderate number of rows
     ↓
Bitmap Scan
```

```text
Large fraction of table
     ↓
Seq Scan
```

This is only a mental model.

The planner uses a much more detailed cost model.

---

# 35. Join performance is about the whole plan

This is the key lesson.

Don't think:

```text
JOIN performance
=
index on join column
```

Instead:

```text
JOIN performance
=
query shape
+
cardinality
+
selectivity
+
indexes
+
statistics
+
join algorithm
+
access paths
+
memory
+
I/O
+
data distribution
```

Everything interacts.

---

# 36. A practical indexing checklist

For a join like:

```sql
A
JOIN B
    ON A.id = B.a_id
```

ask:

### 1. Is `A.id` indexed?

If it's a primary key, normally yes.

### 2. Is `B.a_id` indexed?

Consider whether the relationship is frequently queried.

### 3. Is the join selective?

Does the query access a small portion of the data?

### 4. Are there additional filters?

For example:

```sql
B.status = 'ACTIVE'
```

### 5. Would a composite index help?

Potentially:

```text
(B.a_id, B.status)
```

### 6. What does `EXPLAIN ANALYZE` say?

Don't guess.

---

# 37. Common indexing mistakes

## Mistake 1: Index every column

```text
❌ More indexes = always faster
```

No.

Indexes have storage and write-maintenance costs.

---

## Mistake 2: Assume PostgreSQL must use an index

```text
Index exists
    ≠
Index will be used
```

---

## Mistake 3: Index the wrong column

For:

```sql
ON c.id = o.customer_id
```

the commonly useful foreign-key-side index is:

```text
orders.customer_id
```

not some unrelated column.

---

## Mistake 4: Ignore selectivity

An index is more attractive when it can substantially reduce the rows PostgreSQL must process.

---

## Mistake 5: Ignore cardinality

If one customer has:

```text
10 million orders
```

an index lookup still produces 10 million rows.

The index can't magically make a huge result small.

---

## Mistake 6: Fix logical problems with indexes

An incorrect JOIN remains incorrect regardless of indexing.

---

# 38. The relationship between our lessons

We've now built this chain:

```text
                 JOIN
                  |
                  v
             Cardinality
                  |
                  v
         How many rows match?
                  |
                  v
          Join Algorithm
         /       |       \
        /        |        \
Nested Loop   Hash Join  Merge Join
     |            |          |
     v            v          v
  indexes       memory     ordering
     |                       |
     +-----------+-----------+
                 |
                 v
             EXPLAIN
                 |
                 v
        EXPLAIN ANALYZE
                 |
                 v
          Actual behavior
```

This is the foundation for real PostgreSQL performance analysis.

---

# 39. The most important mental model

When you see:

```sql
SELECT ...
FROM customers AS c
JOIN orders AS o
    ON c.id = o.customer_id;
```

don't immediately think:

> "I need an index."

Think:

```text
1. How many customers?
        ↓
2. How many orders?
        ↓
3. How many rows will match?
        ↓
4. Is the query selective?
        ↓
5. What join algorithm is appropriate?
        ↓
6. What access path is appropriate?
        ↓
7. What does EXPLAIN ANALYZE show?
        ↓
8. Only then: should I change indexing?
```

---

# 40. Final summary

### Indexes can help Nested Loop

```text
Small outer input
      ↓
Index lookup
      ↓
Matching inner rows
```

### Hash Join doesn't require indexes

```text
Build hash
      +
Sequential scan/probe
```

### Indexes can help Merge Join

```text
Index
  ↓
Ordered rows
  ↓
Merge Join
```

### Foreign-key columns are common index candidates

```text
orders.customer_id
        ↓
frequent JOIN
        ↓
often useful index
```

### But indexes aren't automatically used

```text
Index exists
     ↓
Planner estimates cost
     ↓
Maybe Index Scan
Maybe Bitmap Scan
Maybe Seq Scan
```

### And performance isn't just about indexes

```text
Correct query
    +
Correct cardinality
    +
Correct grain
    +
Good statistics
    +
Appropriate join algorithm
    +
Appropriate indexes
    +
Reasonable memory/I/O
```

> **An index gives PostgreSQL another access path. `EXPLAIN ANALYZE` tells you whether PostgreSQL actually benefits from using it.**

---

## Next

**27. Advanced Join Patterns**

We'll move beyond the basic join types and learn practical patterns such as **latest-row joins, greatest-per-group, joining against ranges, conditional joins, deduplication strategies, joining aggregated data, and other patterns that appear frequently in real PostgreSQL applications.**

