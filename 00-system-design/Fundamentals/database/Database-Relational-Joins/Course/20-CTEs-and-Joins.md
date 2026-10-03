# 20. CTEs and Joins

Now we move to **CTEs — Common Table Expressions**.

CTEs are extremely useful when SQL queries become complicated, especially when multiple JOINs, aggregations, filtering, or intermediate results are involved.

The main idea is simple:

> A CTE lets you give a name to an intermediate query result and then use that result in the main query.

---

# 1. The Problem CTEs Solve

Imagine this query:

```sql
SELECT
    c.id,
    c.name,
    COALESCE(o.order_count, 0) AS order_count,
    COALESCE(t.ticket_count, 0) AS ticket_count
FROM customers AS c
LEFT JOIN (
    SELECT
        customer_id,
        COUNT(*) AS order_count
    FROM orders
    GROUP BY customer_id
) AS o
    ON c.id = o.customer_id
LEFT JOIN (
    SELECT
        customer_id,
        COUNT(*) AS ticket_count
    FROM support_tickets
    GROUP BY customer_id
) AS t
    ON c.id = t.customer_id;
```

This is valid.

But as the query grows, nested subqueries can become difficult to read.

CTEs allow us to write:

```sql
WITH order_counts AS (
    ...
),
ticket_counts AS (
    ...
)
SELECT ...
```

Now each intermediate result has a meaningful name.

---

# 2. Basic CTE Syntax

The basic syntax is:

```sql
WITH cte_name AS (
    SELECT ...
)
SELECT ...
FROM cte_name;
```

Think:

```text
WITH
  ↓
create temporary named result
  ↓
main query uses it
```

For example:

```sql
WITH customer_orders AS (
    SELECT
        customer_id,
        COUNT(*) AS order_count
    FROM orders
    GROUP BY customer_id
)
SELECT *
FROM customer_orders;
```

The CTE is named:

```text
customer_orders
```

and contains:

| customer_id | order_count |
| ----------: | ----------: |
|           1 |           2 |
|           2 |           1 |

---

# 3. CTE + JOIN

Now we can JOIN the CTE.

```sql
WITH customer_orders AS (
    SELECT
        customer_id,
        COUNT(*) AS order_count
    FROM orders
    GROUP BY customer_id
)
SELECT
    c.id,
    c.name,
    COALESCE(co.order_count, 0) AS order_count
FROM customers AS c
LEFT JOIN customer_orders AS co
    ON c.id = co.customer_id;
```

Visualize it:

```text
             orders
                |
                ↓
       ┌─────────────────┐
       │ GROUP BY        │
       │ customer_id     │
       └────────┬────────┘
                |
                ↓
       customer_orders
                |
                | JOIN
                ↓
customers ────────────────
                |
                ↓
          final result
```

---

# 4. Why Is This Useful?

Without a CTE:

```text
customers
   |
   +── LEFT JOIN (subquery)
   |
   +── LEFT JOIN (subquery)
   |
   +── WHERE
   |
   +── GROUP BY
   |
   +── ...
```

With CTEs:

```text
order_counts
      |
      |
ticket_counts
      |
      ↓
   main query
      ↑
      |
customers
```

The logic becomes easier to reason about.

---

# 5. Multiple CTEs

A `WITH` clause can contain multiple CTEs.

Example:

```sql
WITH order_counts AS (
    SELECT
        customer_id,
        COUNT(*) AS order_count
    FROM orders
    GROUP BY customer_id
),
ticket_counts AS (
    SELECT
        customer_id,
        COUNT(*) AS ticket_count
    FROM support_tickets
    GROUP BY customer_id
)
SELECT
    c.id,
    c.name,
    COALESCE(oc.order_count, 0) AS order_count,
    COALESCE(tc.ticket_count, 0) AS ticket_count
FROM customers AS c
LEFT JOIN order_counts AS oc
    ON c.id = oc.customer_id
LEFT JOIN ticket_counts AS tc
    ON c.id = tc.customer_id;
```

This is much easier to understand.

---

# 6. CTEs as Named Intermediate Results

Think of each CTE as a named logical table.

```text
orders
   |
   ↓
order_counts
   |
   ↓
       ┌─────────────┐
       │             │
       ↓             ↓
   customers    ticket_counts
       │             │
       └──────┬──────┘
              ↓
         final result
```

The names communicate intent:

```text
order_counts
ticket_counts
customer_revenue
active_customers
recent_orders
```

This is one of the biggest advantages of CTEs.

---

# 7. CTE vs Subquery

Consider this subquery:

```sql
SELECT
    c.id,
    c.name,
    oc.order_count
FROM customers AS c
LEFT JOIN (
    SELECT
        customer_id,
        COUNT(*) AS order_count
    FROM orders
    GROUP BY customer_id
) AS oc
    ON c.id = oc.customer_id;
```

We can rewrite it using a CTE:

```sql
WITH order_counts AS (
    SELECT
        customer_id,
        COUNT(*) AS order_count
    FROM orders
    GROUP BY customer_id
)
SELECT
    c.id,
    c.name,
    oc.order_count
FROM customers AS c
LEFT JOIN order_counts AS oc
    ON c.id = oc.customer_id;
```

The result can be the same.

The major difference is **organization and readability**.

---

# 8. Mental Model

### Subquery

The intermediate logic is embedded inside the query:

```text
Main Query
    |
    +---- Subquery
```

### CTE

The intermediate logic is named first:

```text
CTE
 |
 ↓
Main Query
```

So:

```text
Subquery:

SELECT ...
FROM (
    SELECT ...
) x;


CTE:

WITH x AS (
    SELECT ...
)
SELECT ...
FROM x;
```

Both give you an intermediate result.

---

# 9. Multiple CTEs Can Build a Pipeline

This is where CTEs become especially powerful.

Suppose we want:

> Customers who spent more than ₹10,000 on successful orders in the last 30 days.

We could break the problem into stages.

```sql
WITH recent_orders AS (
    SELECT
        customer_id,
        amount
    FROM orders
    WHERE status = 'SUCCESS'
      AND created_at >= CURRENT_DATE - INTERVAL '30 days'
),
customer_totals AS (
    SELECT
        customer_id,
        SUM(amount) AS total_spent
    FROM recent_orders
    GROUP BY customer_id
)
SELECT
    c.id,
    c.name,
    ct.total_spent
FROM customers AS c
JOIN customer_totals AS ct
    ON c.id = ct.customer_id
WHERE ct.total_spent > 10000;
```

Visual:

```text
orders
  |
  | filter successful + recent
  ↓
recent_orders
  |
  | GROUP BY customer
  | SUM(amount)
  ↓
customer_totals
  |
  | JOIN
  ↓
customers
  |
  | filter total > 10000
  ↓
final result
```

This is much easier to debug than one giant query.

---

# 10. CTEs Can Depend on Earlier CTEs

This is important.

For example:

```sql
WITH recent_orders AS (
    SELECT *
    FROM orders
    WHERE created_at >= CURRENT_DATE - INTERVAL '30 days'
),
successful_orders AS (
    SELECT *
    FROM recent_orders
    WHERE status = 'SUCCESS'
),
customer_totals AS (
    SELECT
        customer_id,
        SUM(amount) AS total_spent
    FROM successful_orders
    GROUP BY customer_id
)
SELECT *
FROM customer_totals;
```

The dependency is:

```text
orders
  ↓
recent_orders
  ↓
successful_orders
  ↓
customer_totals
  ↓
final query
```

This creates a logical processing pipeline.

---

# 11. Recursive CTEs

CTEs also support recursion.

This is particularly useful for hierarchical data such as:

```text
CEO
 |
 +-- Engineering Manager
 |      |
 |      +-- Developer
 |      +-- Developer
 |
 +-- Sales Manager
        |
        +-- Salesperson
```

For example:

```sql
WITH RECURSIVE employee_tree AS (
    SELECT
        id,
        name,
        manager_id,
        0 AS level
    FROM employees
    WHERE manager_id IS NULL

    UNION ALL

    SELECT
        e.id,
        e.name,
        e.manager_id,
        et.level + 1
    FROM employees AS e
    JOIN employee_tree AS et
        ON e.manager_id = et.id
)
SELECT *
FROM employee_tree;
```

Conceptually:

```text
Level 0
   CEO
    |
    ↓
Level 1
 Managers
    |
    ↓
Level 2
 Employees
    |
    ↓
Level 3
 ...
```

You don't need to master recursive CTEs yet; the important point is:

> `WITH RECURSIVE` allows a CTE to reference itself.

---

# 12. CTEs and JOIN Cardinality

CTEs do **not** magically prevent JOIN multiplication.

Suppose:

```sql
WITH customer_orders AS (
    SELECT
        customer_id,
        COUNT(*) AS order_count
    FROM orders
    GROUP BY customer_id
)
SELECT ...
```

Here:

```text
orders
   ↓
GROUP BY customer_id
   ↓
one row per customer
```

So `customer_orders` has a controlled grain:

> **one row per customer**

That makes joining it to `customers` safe from order-level multiplication.

This is a powerful pattern.

---

# 13. Grain Still Matters

Suppose this CTE:

```sql
WITH order_items AS (
    SELECT *
    FROM orders
    JOIN order_items
        ON orders.id = order_items.order_id
)
```

might have:

```text
one row per order item
```

If you then join it to another one-to-many dataset, multiplication can still occur.

So always ask:

> **What does one row in this CTE represent?**

For example:

| CTE               | Grain                  |
| ----------------- | ---------------------- |
| `recent_orders`   | one row per order      |
| `order_counts`    | one row per customer   |
| `customer_totals` | one row per customer   |
| `order_items`     | one row per order item |

This concept remains just as important with CTEs as with normal JOINs.

---

# 14. CTEs for Avoiding Join Explosion

Let's revisit the earlier problem.

We have:

```text
customers
   |
   +---- orders
   |
   +---- tickets
```

Alice:

```text
3 orders
2 tickets
```

Direct JOIN:

```text
3 × 2 = 6 rows
```

Instead:

```sql
WITH order_counts AS (
    SELECT
        customer_id,
        COUNT(*) AS order_count
    FROM orders
    GROUP BY customer_id
),
ticket_counts AS (
    SELECT
        customer_id,
        COUNT(*) AS ticket_count
    FROM support_tickets
    GROUP BY customer_id
)
SELECT
    c.id,
    c.name,
    COALESCE(oc.order_count, 0) AS order_count,
    COALESCE(tc.ticket_count, 0) AS ticket_count
FROM customers AS c
LEFT JOIN order_counts AS oc
    ON c.id = oc.customer_id
LEFT JOIN ticket_counts AS tc
    ON c.id = tc.customer_id;
```

Now:

```text
orders
   ↓
one row/customer
   ↓
order_counts
   ↓
      JOIN
         \
          customers
         /
      JOIN
   ticket_counts
   ↑
one row/customer
   ↑
tickets
```

Each child has been reduced to the desired grain before the JOIN.

---

# 15. CTEs Are Not Temporary Tables

This distinction is important.

A CTE:

```sql
WITH x AS (
    SELECT ...
)
SELECT *
FROM x;
```

is part of a **single SQL statement**.

It is not automatically a permanent database object.

It doesn't create:

```text
database table
```

that remains available after the query finishes.

Compare:

| Feature                   | CTE                | Temporary Table                             |
| ------------------------- | ------------------ | ------------------------------------------- |
| Lifetime                  | Statement          | Session/transaction depending on definition |
| Named intermediate result | Yes                | Yes                                         |
| Permanent object          | No                 | No                                          |
| Can be indexed separately | No                 | Yes                                         |
| Used in one statement     | Yes                | Can be reused across statements             |
| Good for                  | Query organization | Larger multi-step workflows                 |

---

# 16. CTEs and PostgreSQL Optimization

Here's an important PostgreSQL-specific detail.

A CTE is not necessarily a physical temporary table.

PostgreSQL can sometimes **inline** a CTE into the surrounding query.

Conceptually:

```text
SQL
 ↓
CTE
 ↓
Optimizer
 ↓
Execution plan
```

So don't think:

```text
WITH x AS (...)
```

automatically means:

```text
"execute x completely and store it somewhere."
```

Modern PostgreSQL can optimize CTEs depending on the query.

---

# 17. `MATERIALIZED`

PostgreSQL allows you to explicitly request materialization:

```sql
WITH expensive_calculation AS MATERIALIZED (
    SELECT ...
)
SELECT ...
FROM expensive_calculation;
```

Conceptually:

```text
CTE
 ↓
materialize result
 ↓
use result
```

You can also explicitly request inlining:

```sql
WITH calculated AS NOT MATERIALIZED (
    SELECT ...
)
SELECT ...
FROM calculated;
```

These are advanced optimization controls.

The important takeaway for now:

> **A CTE is primarily a query-structuring mechanism; don't automatically equate it with a temporary table.**

---

# 18. CTEs Improve Debugging

This is one of their practical benefits.

Suppose we have:

```sql
WITH recent_orders AS (
    ...
),
successful_orders AS (
    ...
),
customer_totals AS (
    ...
)
SELECT ...
```

During debugging, you can temporarily replace the final query with:

```sql
WITH recent_orders AS (
    ...
),
successful_orders AS (
    ...
),
customer_totals AS (
    ...
)
SELECT *
FROM recent_orders;
```

Then:

```sql
SELECT *
FROM successful_orders;
```

Then:

```sql
SELECT *
FROM customer_totals;
```

You can inspect each stage independently.

Visual:

```text
orders
  ↓
[Stage 1]
recent_orders
  ↓
[Stage 2]
successful_orders
  ↓
[Stage 3]
customer_totals
  ↓
final query
```

This makes complicated SQL much easier to troubleshoot.

---

# 19. CTEs for Business Logic

CTEs can also make business rules readable.

For example:

```sql
WITH active_customers AS (
    SELECT
        id,
        name
    FROM customers
    WHERE status = 'ACTIVE'
),
successful_orders AS (
    SELECT
        customer_id,
        amount
    FROM orders
    WHERE status = 'SUCCESS'
)
SELECT
    ac.id,
    ac.name,
    COALESCE(SUM(so.amount), 0) AS total_spent
FROM active_customers AS ac
LEFT JOIN successful_orders AS so
    ON ac.id = so.customer_id
GROUP BY
    ac.id,
    ac.name;
```

The query almost reads like English:

```text
Get active customers
       ↓
Get successful orders
       ↓
Connect them
       ↓
Calculate total spending
```

---

# 20. CTE vs Derived Table

These are closely related.

### Derived table

```sql
SELECT *
FROM (
    SELECT
        customer_id,
        COUNT(*) AS order_count
    FROM orders
    GROUP BY customer_id
) AS order_counts;
```

### CTE

```sql
WITH order_counts AS (
    SELECT
        customer_id,
        COUNT(*) AS order_count
    FROM orders
    GROUP BY customer_id
)
SELECT *
FROM order_counts;
```

The CTE is often easier to read, especially when the intermediate query is large.

---

# 21. CTE vs JOIN

These aren't alternatives.

This is a common misconception.

```text
CTE
and
JOIN
```

solve different problems.

A CTE answers:

> "How can I organize an intermediate query result?"

A JOIN answers:

> "How do I combine related rows?"

Therefore:

```sql
WITH order_counts AS (
    SELECT ...
)
SELECT ...
FROM customers AS c
JOIN order_counts AS oc
    ON ...
```

is completely normal.

You can have:

```text
CTE
 ↓
JOIN
 ↓
another JOIN
 ↓
GROUP BY
 ↓
final result
```

---

# 22. When Should You Use a CTE?

CTEs are particularly useful when:

### 1. Query is becoming difficult to read

Instead of:

```text
giant nested query
```

use:

```text
named stages
```

---

### 2. You need an intermediate aggregation

```sql
WITH order_counts AS (
    SELECT customer_id, COUNT(*)
    FROM orders
    GROUP BY customer_id
)
...
```

---

### 3. You want to separate business logic

```text
recent_orders
successful_orders
high_value_customers
```

Each stage has a clear purpose.

---

### 4. You need to reuse an intermediate result within the statement

For example:

```sql
WITH customer_totals AS (
    ...
)
SELECT ...
FROM customer_totals ...
```

You can reference the CTE from multiple parts of the statement where appropriate.

---

### 5. You need recursion

Use:

```sql
WITH RECURSIVE
```

for hierarchical/tree-like problems.

---

# 23. When Should You Not Add a CTE?

Don't create CTEs simply because you can.

This:

```sql
WITH x AS (
    SELECT *
    FROM customers
)
SELECT *
FROM x;
```

doesn't add much value.

You could simply write:

```sql
SELECT *
FROM customers;
```

A CTE should generally make the query:

* clearer
* easier to reason about
* easier to maintain
* easier to debug
* structurally appropriate

---

# 24. A Useful Query Design Pattern

For complex reporting queries, think:

```text
Raw tables
    ↓
Filter
    ↓
Aggregate
    ↓
Normalize grain
    ↓
JOIN
    ↓
Final filtering
    ↓
Final output
```

CTEs can represent these stages:

```sql
WITH filtered_orders AS (
    ...
),
customer_totals AS (
    ...
),
active_customers AS (
    ...
)
SELECT ...
FROM active_customers
JOIN customer_totals
    ON ...;
```

This gives you a clean mental pipeline.

---

# 25. Debugging a CTE Query

Suppose you have:

```sql
WITH
recent_orders AS (...),
customer_totals AS (...),
high_value_customers AS (...)
SELECT ...
```

and the final result is wrong.

Don't immediately stare at the entire query.

Test each stage:

```sql
SELECT *
FROM recent_orders;
```

Check:

```text
Are the right orders present?
```

Then:

```sql
SELECT *
FROM customer_totals;
```

Check:

```text
Are totals correct?
```

Then:

```sql
SELECT *
FROM high_value_customers;
```

Check:

```text
Are customers correctly classified?
```

Finally inspect the JOIN.

This gives you a debugging pipeline:

```text
Stage 1
  ↓
correct?
  ↓ YES
Stage 2
  ↓
correct?
  ↓ YES
Stage 3
  ↓
correct?
  ↓ YES
JOIN
  ↓
correct?
```

---

# 26. CTEs + Grain: The Critical Connection

This is probably the most important concept from today's lesson.

Before joining a CTE, ask:

> **What is the grain of this CTE?**

Example:

```sql
WITH order_counts AS (
    SELECT
        customer_id,
        COUNT(*) AS order_count
    FROM orders
    GROUP BY customer_id
)
```

Its grain is:

```text
ONE ROW PER CUSTOMER
```

Therefore:

```text
customers
   1
   |
   | JOIN
   |
   1
order_counts
```

This is effectively a one-to-one relationship at the CTE level.

That is much safer than joining raw:

```text
customers
   1
   |
   |
   N
orders
```

when your final output needs one row per customer.

---

# 27. The Big Picture

We've now covered:

```text
JOIN
 ↓
combine rows

GROUP BY
 ↓
summarize rows

EXISTS
 ↓
test existence

CTE
 ↓
organize intermediate results
```

And they can be combined:

```text
                    ┌───────────────┐
                    │   customers   │
                    └───────┬───────┘
                            │
                            │ JOIN
                            │
             ┌──────────────▼─────────────┐
             │       order_counts         │
             │       (CTE)                │
             └──────────────┬─────────────┘
                            │
                            │
                       final result
```

---

# 28. Interview Questions

### Q1. What is a CTE?

A Common Table Expression is a named query result defined using `WITH` and available to the statement that follows it.

---

### Q2. Is a CTE a temporary table?

No.

A CTE is part of a SQL statement and is not automatically a persistent or independently indexed table.

---

### Q3. Why use a CTE?

Common reasons include:

* readability
* breaking complex queries into stages
* intermediate aggregation
* expressing business logic
* debugging
* recursive queries

---

### Q4. Can you JOIN a CTE?

Yes.

A CTE can be referenced similarly to a table in the main query.

```sql
WITH order_counts AS (
    ...
)
SELECT ...
FROM customers AS c
JOIN order_counts AS oc
    ON c.id = oc.customer_id;
```

---

### Q5. Does a CTE prevent JOIN explosion?

**Not automatically.**

It can help prevent explosion if you use it to aggregate data to the correct grain before joining.

For example:

```text
orders
  ↓
GROUP BY customer
  ↓
one row/customer
  ↓
JOIN
```

But a CTE containing multiple rows per customer can still multiply rows.

---

### Q6. Does CTE always improve performance?

No.

CTEs primarily help with query organization and clarity. PostgreSQL's optimizer determines how the query is actually executed, and materialization/inlining behavior matters.

---

# 29. Golden Rules

Remember these:

1. `WITH` creates a **named intermediate query result**.
2. A CTE is available to the statement that follows it.
3. CTEs and JOINs are complementary, not competing features.
4. Use CTEs to break complex SQL into understandable stages.
5. Multiple CTEs can form a logical pipeline.
6. A CTE can depend on an earlier CTE.
7. `WITH RECURSIVE` supports hierarchical/recursive queries.
8. A CTE is not automatically a temporary table.
9. A CTE does **not** automatically prevent JOIN multiplication.
10. Always know the **grain** of your CTE before joining it.
11. Aggregate to the required grain before joining when appropriate.
12. Use CTEs for clarity, maintainability, and debugging rather than adding them unnecessarily.
13. Don't assume CTE syntax dictates physical execution.

---

# 30. One Sentence to Remember

> **A CTE lets you turn a complicated query into named, understandable stages that you can then JOIN, aggregate, filter, or otherwise use in the main query.**

```text
WITH
  ↓
named intermediate result
  ↓
another CTE if needed
  ↓
JOIN / GROUP BY / WHERE
  ↓
final result
```

## Next: PostgreSQL Join Algorithms

We'll move from **how we write JOINs** to **how PostgreSQL actually executes them**, introducing:

* Nested Loop Join
* Hash Join
* Merge Join
* why PostgreSQL chooses one over another
* how indexes affect the choice
* how to see the chosen algorithm with `EXPLAIN`

