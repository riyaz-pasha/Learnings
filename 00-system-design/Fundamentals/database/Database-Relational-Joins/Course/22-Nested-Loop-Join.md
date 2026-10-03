# 22. Nested Loop Join

Now we'll go deeper into the first physical join algorithm:

> **Nested Loop Join**

The name sounds complicated, but the basic idea is very simple:

> Take one row from the outer input, look for matching rows in the inner input, then repeat.

---

# 1. The Basic Idea

Suppose we have:

```text
customers

id
---
1
2
3
```

and:

```text
orders

id   customer_id
---  -----------
101      1
102      1
103      2
104      3
```

The JOIN is:

```sql
SELECT
    c.name,
    o.id
FROM customers AS c
JOIN orders AS o
    ON c.id = o.customer_id;
```

A Nested Loop conceptually does:

```text
Take customer 1
    ↓
Find matching orders
    ↓
Order 101
Order 102

Take customer 2
    ↓
Find matching orders
    ↓
Order 103

Take customer 3
    ↓
Find matching orders
    ↓
Order 104
```

So:

```text
customers
    |
    ↓
┌─────────────┐
│ customer 1  │ ─────→ search orders
│ customer 2  │ ─────→ search orders
│ customer 3  │ ─────→ search orders
└─────────────┘
```

---

# 2. Why "Nested Loop"?

Because conceptually it resembles two loops:

```text
for each row in customers:
    for each row in orders:
        if customer.id == order.customer_id:
            produce result
```

In pseudocode:

```text
for customer in customers:
    for order in orders:
        if customer.id == order.customer_id:
            output(customer, order)
```

That's the simplest mental model.

However, **PostgreSQL does not necessarily literally scan the entire inner table for every outer row**.

That's where indexes become important.

---

# 3. Two Versions of Nested Loop

There are two useful mental models.

### Version 1 — Inner scan

```text
for each outer row
    scan inner input
```

### Version 2 — Inner index lookup

```text
for each outer row
    use index to find matching inner rows
```

The second version can be dramatically faster.

---

# 4. Nested Loop Without an Index

Suppose:

```text
customers = 3 rows
orders = 4 rows
```

Conceptually:

```text
Customer 1 → scan orders 1,2,3,4
Customer 2 → scan orders 1,2,3,4
Customer 3 → scan orders 1,2,3,4
```

Potential comparisons:

```text
3 × 4 = 12
```

If we had:

```text
customers = 100,000
orders = 10,000,000
```

a naïve full scan for every outer row would be enormous.

That is why the access method for the inner side matters.

---

# 5. Nested Loop With an Index

Suppose:

```sql
CREATE INDEX idx_orders_customer_id
ON orders(customer_id);
```

Now imagine:

```text
customers = 10 rows
orders = 10,000,000 rows
```

PostgreSQL can potentially do:

```text
customer 1
    ↓
index lookup: customer_id = 1
    ↓
matching orders

customer 2
    ↓
index lookup: customer_id = 2
    ↓
matching orders

customer 3
    ↓
index lookup: customer_id = 3
    ↓
matching orders
```

Instead of:

```text
customer 1 → scan 10 million rows
customer 2 → scan 10 million rows
customer 3 → scan 10 million rows
...
```

This is the idea behind an **Index Nested Loop Join**.

---

# 6. The Most Important Insight

A Nested Loop is not inherently slow.

This:

```text
small outer input
        +
cheap inner lookup
        =
potentially excellent Nested Loop
```

For example:

```text
10 customers
      ↓
10 index lookups
      ↓
large orders table
```

can be very efficient.

---

# 7. Outer vs Inner

When PostgreSQL uses a Nested Loop, there are two inputs:

```text
Outer
  |
  ↓
Nested Loop
  |
  ↓
Inner
```

For every row from the **outer** side, PostgreSQL processes the **inner** side.

Conceptually:

```text
Outer row 1
    ↓
Inner
    ↓
matches

Outer row 2
    ↓
Inner
    ↓
matches

Outer row 3
    ↓
Inner
    ↓
matches
```

The choice of outer and inner side matters.

---

# 8. Why Does Outer Side Size Matter?

Imagine:

```text
A = 10 rows
B = 10,000,000 rows
```

Nested Loop:

```text
10 outer rows
   ↓
10 inner lookups
```

Potentially reasonable.

But reverse the situation:

```text
A = 10,000,000 rows
B = 10 rows
```

Now you might conceptually have:

```text
10,000,000 outer rows
   ↓
10,000,000 inner operations
```

That could be expensive.

This is why the planner doesn't simply say:

> "Nested Loop is good."

It evaluates the estimated cost.

---

# 9. The Cost Intuition

A simplified mental model for Nested Loop is:

```text
Cost ≈
cost of reading outer
+
(number of outer rows × cost of processing inner)
```

Not PostgreSQL's actual cost formula, but useful intuition.

The critical multiplication is:

```text
outer rows × inner work
```

If the outer side is tiny:

```text
10 × cheap lookup
```

great.

If the outer side is huge:

```text
10,000,000 × lookup
```

potentially expensive.

---

# 10. Index Changes the Equation

Without an index:

```text
outer row
   ↓
scan entire inner table
```

With an index:

```text
outer row
   ↓
index lookup
   ↓
matching rows
```

So:

```text
Nested Loop
     |
     +── Sequential Scan inner
     |
     OR
     |
     +── Index Scan inner
```

This is an important distinction.

---

# 11. Example

Consider:

```sql
SELECT
    c.id,
    c.name,
    o.id AS order_id
FROM customers AS c
JOIN orders AS o
    ON c.id = o.customer_id
WHERE c.id = 42;
```

Suppose:

```text
customers
    ↓
WHERE id = 42
    ↓
1 row
```

Then:

```text
1 customer
    ↓
index lookup on orders.customer_id
    ↓
matching orders
```

A Nested Loop can be a very sensible plan.

---

# 12. What If We Remove the Filter?

Now:

```sql
SELECT
    c.id,
    c.name,
    o.id AS order_id
FROM customers AS c
JOIN orders AS o
    ON c.id = o.customer_id;
```

Suppose:

```text
customers = 5 million
orders = 20 million
```

Now the planner has a very different problem.

Repeated indexed lookups for millions of customers may not be the cheapest approach.

A Hash Join could be preferable.

So:

```text
Query with selective filter
        ↓
small outer input
        ↓
Nested Loop may be attractive
```

while:

```text
Query without selective filter
        ↓
huge inputs
        ↓
Hash/Merge Join may be more attractive
```

Again, these are tendencies, not guarantees.

---

# 13. Nested Loop Can Still Use a Filter

Suppose:

```sql
SELECT
    c.name,
    o.id
FROM customers AS c
JOIN orders AS o
    ON c.id = o.customer_id
WHERE c.country = 'IN';
```

If only a small number of customers are in India:

```text
customers
   ↓
country = 'IN'
   ↓
small result
   ↓
Nested Loop
   ↓
index lookups into orders
```

The filter can dramatically change the economics of the join.

---

# 14. Nested Loop With a Composite Condition

Consider:

```sql
SELECT *
FROM orders AS o
JOIN product_prices AS p
    ON o.product_id = p.product_id
   AND o.created_at >= p.valid_from
   AND o.created_at < p.valid_to;
```

This is more complicated than a simple equality join.

A Nested Loop can be useful when:

```text
outer order
   ↓
find candidate prices
   ↓
check date range
```

This is one reason Nested Loop is more flexible than Hash Join for certain join conditions.

---

# 15. Nested Loop and Non-Equality Conditions

Recall our range join:

```sql
SELECT
    e.name,
    r.level
FROM employees AS e
JOIN salary_ranges AS r
    ON e.salary >= r.min_salary
   AND e.salary < r.max_salary;
```

This isn't a simple equality:

```text
employee.salary = range.key
```

Instead:

```text
employee.salary >= min
AND
employee.salary < max
```

A Nested Loop can evaluate such predicates.

This doesn't mean PostgreSQL will always choose Nested Loop, but it is an algorithm that can handle general join conditions.

---

# 16. Nested Loop + Index Scan

A very common execution-plan pattern looks conceptually like:

```text
Nested Loop
├── Seq Scan / Index Scan on customers
└── Index Scan on orders
```

For example:

```text
Nested Loop
├── Index Scan using customers_pkey
│
└── Index Scan using idx_orders_customer_id
```

Read it as:

```text
Find customer(s)
      ↓
for each customer
      ↓
lookup matching orders through index
```

---

# 17. Why You Might See `loops=` in EXPLAIN ANALYZE

This is an important practical concept.

Suppose the plan looks roughly like:

```text
Nested Loop
  -> Index Scan on customers
  -> Index Scan on orders
       loops=100
```

The:

```text
loops=100
```

means the inner operation was executed repeatedly as part of the Nested Loop.

Conceptually:

```text
outer rows ≈ 100
        ↓
inner operation
        ↓
executed repeatedly
```

This is one of the first things to look at when diagnosing an expensive Nested Loop.

---

# 18. Example of a Potentially Bad Nested Loop

Imagine PostgreSQL estimates:

```text
outer rows = 1,000,000
inner lookup cost = significant
```

Then:

```text
1,000,000
     ×
inner work
```

can become expensive.

You might see something like:

```text
Nested Loop
├── Seq Scan on A
│      rows=1,000,000
│
└── Index Scan on B
       loops=1,000,000
```

That doesn't automatically mean the query is wrong.

But it is a strong signal to investigate:

* Is the row estimate correct?
* Is the index appropriate?
* Is the join selective?
* Could another join algorithm be cheaper?
* Is there a missing filter?
* Are statistics outdated?

---

# 19. Estimated Rows vs Actual Rows

This is extremely important for understanding PostgreSQL plans.

You might see:

```text
rows=10
```

but with `EXPLAIN ANALYZE`:

```text
actual rows=10000
```

That's a huge estimation error.

The planner thought:

```text
10 rows
```

but reality was:

```text
10,000 rows
```

That can cause a poor join choice.

For example:

```text
Planner thinks:
10 outer rows
     ↓
Nested Loop is cheap

Reality:
10,000 outer rows
     ↓
Nested Loop is expensive
```

---

# 20. Why Can Estimates Be Wrong?

Possible reasons include:

* stale statistics
* unusual data distribution
* correlated columns
* insufficient statistics
* complex predicates
* parameterized queries
* skewed values

PostgreSQL uses statistics to estimate cardinality.

You can refresh normal table statistics with:

```sql
ANALYZE customers;
ANALYZE orders;
```

Or:

```sql
ANALYZE;
```

for broader analysis.

We'll go deeper into statistics later when we study execution plans.

---

# 21. Nested Loop and Join Order

Consider:

```text
A JOIN B JOIN C
```

A possible plan:

```text
       Nested Loop
       /         \
   A JOIN B      C
```

Or:

```text
       Nested Loop
       /         \
       A       B JOIN C
```

The optimizer considers different possibilities.

This matters because the number of rows entering each Nested Loop affects its cost.

---

# 22. A Small Example

Suppose:

```text
A = 100 rows
B = 1,000 rows
C = 1,000,000 rows
```

If:

```text
A JOIN B
```

produces only:

```text
20 rows
```

then:

```text
A JOIN B
    ↓
20 rows
    ↓
JOIN C
```

might be much better than:

```text
B JOIN C
    ↓
huge intermediate result
    ↓
JOIN A
```

This connects directly to our earlier discussion of **join explosion**.

---

# 23. Nested Loop vs Hash Join

Let's compare them.

### Nested Loop

```text
Outer row
   ↓
lookup/search inner
   ↓
next outer row
   ↓
lookup/search inner
```

### Hash Join

```text
Build hash table
       ↓
scan other input
       ↓
hash lookup
       ↓
matches
```

---

# 24. When Nested Loop Is Attractive

A simplified checklist:

```text
Small outer input?
      |
     YES
      ↓
Cheap inner access?
      |
     YES
      ↓
Index available?
      |
     YES
      ↓
Nested Loop can be attractive
```

Typical situations:

* highly selective WHERE condition
* small result from outer table
* index on inner join key
* lookup-style joins
* certain non-equality joins

---

# 25. When Nested Loop Can Become Expensive

Potential warning signs:

```text
Huge outer input
      +
Expensive inner operation
      +
Many loops
      =
Potentially expensive query
```

For example:

```text
1 million outer rows
       ↓
1 million inner scans/lookups
```

Especially problematic if the inner operation is a full table scan.

---

# 26. Nested Loop Does Not Mean "Bad Query"

This is worth emphasizing.

Seeing:

```text
Nested Loop
```

in `EXPLAIN` does **not** mean:

> "I found the performance problem."

You need to ask:

```text
How many outer rows?
How many loops?
What is the inner operation?
Is it indexed?
How many rows does each lookup return?
What is actual execution time?
Were planner estimates accurate?
```

A Nested Loop over 5 rows can be perfectly fine.

A Nested Loop with millions of expensive loops may be problematic.

---

# 27. Practical Example

Suppose:

```sql
CREATE INDEX idx_orders_customer_id
ON orders(customer_id);
```

Query:

```sql
SELECT
    c.id,
    c.name,
    o.id AS order_id,
    o.amount
FROM customers AS c
JOIN orders AS o
    ON c.id = o.customer_id
WHERE c.id IN (10, 20, 30);
```

Conceptually:

```text
customers
    |
    ↓
3 customers
    |
    ↓
Nested Loop
    |
    ├── customer 10
    │      ↓
    │   index lookup
    │
    ├── customer 20
    │      ↓
    │   index lookup
    │
    └── customer 30
           ↓
        index lookup
```

This is exactly the kind of situation where Nested Loop can make sense.

---

# 28. What Happens If the Inner Side Has Many Matches?

Suppose:

```text
customer 10
   ↓
500,000 orders
```

Even with an index:

```text
index lookup
     ↓
500,000 matching rows
```

The JOIN still needs to produce those rows.

The index doesn't magically make the output small.

This is important:

> **An efficient lookup doesn't mean the final operation is cheap if the lookup returns a huge number of rows.**

---

# 29. Nested Loop and Result Cardinality

Suppose:

```text
Outer = 100 customers
Average matching orders = 20
```

Expected output:

```text
100 × 20 = 2,000 rows
```

The Nested Loop has to produce those 2,000 rows.

If:

```text
Outer = 100,000 customers
Average matching orders = 100
```

you potentially have:

```text
100,000 × 100
=
10,000,000 result rows
```

No join algorithm can make the cost of materializing 10 million required output rows disappear.

This reinforces a key principle from earlier:

> **Cardinality matters.**

---

# 30. The `EXPLAIN` Mental Model

When you eventually see:

```text
Nested Loop
  -> ...
  -> ...
```

read it as:

```text
1. Execute the outer child.
2. For each outer row:
       execute/process the inner child.
3. Produce matching rows.
```

For example:

```text
Nested Loop
├── Index Scan on customers
└── Index Scan on orders
```

means approximately:

```text
customers
   ↓
outer rows
   ↓
for each row
   ↓
orders index lookup
   ↓
joined result
```

---

# 31. The Critical Difference: Algorithm vs Access Method

Another important distinction.

These are **join algorithms**:

```text
Nested Loop
Hash Join
Merge Join
```

These are **scan/access methods**:

```text
Sequential Scan
Index Scan
Index Only Scan
Bitmap Heap Scan
Bitmap Index Scan
```

For example:

```text
Nested Loop
├── Seq Scan on customers
└── Index Scan on orders
```

means:

```text
Join algorithm:
    Nested Loop

Outer access method:
    Sequential Scan

Inner access method:
    Index Scan
```

Don't mix these concepts.

---

# 32. A More Accurate Mental Model

Think of a PostgreSQL plan as layers:

```text
                 Query
                   |
                   ↓
              Join Algorithm
                   |
          ┌────────┴────────┐
          ↓                 ↓
       Input A           Input B
          |                 |
      Scan method       Scan method
          |                 |
          ↓                 ↓
       Storage           Storage
```

For Nested Loop:

```text
                Nested Loop
                /         \
               /           \
        Outer input      Inner input
             |                |
        Seq/Index/etc.    Seq/Index/etc.
```

This will become extremely useful when we study `EXPLAIN`.

---

# 33. Interview Questions

### Q1. What is a Nested Loop Join?

A Nested Loop Join processes rows from an outer input and searches/processes the inner input for each outer row to find matching rows.

---

### Q2. Is Nested Loop always slow?

No.

It can be very efficient when the outer input is small and the inner side can be accessed cheaply, particularly through an appropriate index.

---

### Q3. Why is an index useful for Nested Loop?

Because instead of scanning the entire inner table for each outer row, PostgreSQL can use the index to directly locate candidate matching rows.

---

### Q4. What does `loops=1000` indicate in an EXPLAIN plan?

It indicates that the corresponding plan node was executed repeatedly as part of the surrounding plan, with the node reporting 1,000 loops.

---

### Q5. Does an index guarantee Nested Loop?

No.

The optimizer considers the overall cost and can choose Hash Join, Merge Join, or another plan.

---

### Q6. Can Nested Loop handle non-equality joins?

Yes.

Unlike Hash Join's natural equality-based lookup model, Nested Loop can evaluate general join predicates, although whether it is efficient depends on the access path and data.

---

### Q7. What is the biggest performance risk with Nested Loop?

A large number of outer rows combined with expensive repeated inner work.

---

# 34. Golden Rules

1. **Nested Loop = process outer rows and repeatedly access the inner side.**
2. The outer side is processed first.
3. The inner side is accessed for each outer row.
4. A small outer input can make Nested Loop very efficient.
5. An index can make inner lookups very cheap.
6. Nested Loop does not require an index.
7. Without a suitable index, the inner operation can become expensive.
8. Large outer inputs can make repeated inner work expensive.
9. `loops=` helps reveal repeated execution in `EXPLAIN ANALYZE`.
10. Always examine the inner operation, not just the words `Nested Loop`.
11. A Nested Loop is not automatically a performance problem.
12. Indexes don't guarantee Nested Loop.
13. Result cardinality still matters.
14. Join algorithm and scan/access method are different concepts.
15. Actual execution plans matter more than blanket rules.

---

# 35. One Sentence to Remember

> **Nested Loop Join means: take an outer row, find its matching inner rows, repeat for every outer row.**

The most useful mental picture is:

```text
             Outer rows
                 |
                 ↓
        ┌────────────────┐
        │  Nested Loop   │
        └───────┬────────┘
                |
       ┌────────┼────────┐
       ↓        ↓        ↓
   lookup B  lookup B  lookup B
       ↓        ↓        ↓
   matches   matches   matches
```

And with an index:

```text
Outer row
    |
    ↓
Index lookup
    |
    ↓
Matching inner rows
```

## Next: 23. Hash Join

We'll go deeper into **Hash Join**, including:

* build side vs probe side
* hash tables and buckets
* why equality joins are important
* memory and `work_mem`
* what happens when the hash doesn't fit in memory
* `Hash`, `Hash Join`, and `Batches` in `EXPLAIN ANALYZE`
* why Hash Join can be excellent for large tables.

