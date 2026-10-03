# 19. Subqueries vs Joins

Now we move to an important SQL design question:

> **When should I use a subquery, and when should I use a JOIN?**

Both can sometimes solve the same problem, but they express **different kinds of thinking**.

---

## 1. What is a Subquery?

A **subquery** is a query written inside another query.

Think of it as:

```text
Outer Query
    |
    +---- Inner Query (Subquery)
              |
              +---- produces a value / rows
```

Example:

```sql
SELECT *
FROM customers
WHERE id IN (
    SELECT customer_id
    FROM orders
);
```

The inner query:

```sql
SELECT customer_id
FROM orders;
```

produces customer IDs.

The outer query then asks:

> "Give me customers whose ID is in that result."

---

# 2. What is a JOIN?

A JOIN combines rows from multiple tables based on a relationship.

```sql
SELECT
    c.id,
    c.name,
    o.id AS order_id
FROM customers AS c
JOIN orders AS o
    ON c.id = o.customer_id;
```

Mental model:

```text
customers
     |
     | JOIN
     ↓
orders
     |
     ↓
combined rows
```

So:

* **JOIN** → combine related rows
* **Subquery** → use the result of another query

---

# 3. A Simple Example

Suppose we have:

### customers

| id | name    |
| -: | ------- |
|  1 | Alice   |
|  2 | Bob     |
|  3 | Charlie |

### orders

|  id | customer_id | amount |
| --: | ----------: | -----: |
| 101 |           1 |    500 |
| 102 |           1 |    200 |
| 103 |           2 |    900 |

We want:

> Find customers who have placed at least one order.

There are several ways.

---

## 4. Using JOIN

```sql
SELECT DISTINCT
    c.id,
    c.name
FROM customers AS c
JOIN orders AS o
    ON c.id = o.customer_id;
```

Result:

| id | name  |
| -: | ----- |
|  1 | Alice |
|  2 | Bob   |

But notice something.

Alice has two orders.

Therefore the JOIN initially produces:

```text
Alice + Order 101
Alice + Order 102
Bob   + Order 103
```

Then `DISTINCT` removes the repeated Alice.

---

# 5. Using EXISTS

We could instead say:

> "I don't need the orders. I only need to know whether an order exists."

```sql
SELECT
    c.id,
    c.name
FROM customers AS c
WHERE EXISTS (
    SELECT 1
    FROM orders AS o
    WHERE o.customer_id = c.id
);
```

Result:

| id | name  |
| -: | ----- |
|  1 | Alice |
|  2 | Bob   |

This is a **semi-join**.

The important difference is:

```text
JOIN

Customer
   |
   +---- Order 101
   |
   +---- Order 102

        ↓

2 result rows
```

Whereas:

```text
EXISTS

Customer
   |
   +---- Does an order exist?
              |
              YES
              ↓
        Keep customer once
```

---

# 6. The Most Important Question

When deciding between JOIN and a subquery, ask:

> **What am I trying to get from the other table?**

There are two very different situations.

### Situation A — I need data from the other table

Use a JOIN.

```sql
SELECT
    c.name,
    o.id,
    o.amount
FROM customers AS c
JOIN orders AS o
    ON c.id = o.customer_id;
```

We need:

* customer name
* order ID
* order amount

So we actually need the related rows.

---

### Situation B — I only need to know whether something exists

Use `EXISTS`.

```sql
SELECT
    c.name
FROM customers AS c
WHERE EXISTS (
    SELECT 1
    FROM orders AS o
    WHERE o.customer_id = c.id
);
```

We don't need:

* order ID
* order amount
* order date

We only need:

```text
Does at least one order exist?
```

---

# 7. Subqueries Are More Than EXISTS

There are several types of subqueries.

A useful classification is:

| Type                | Example                | Produces                  |
| ------------------- | ---------------------- | ------------------------- |
| Scalar subquery     | `(SELECT AVG(...))`    | One value                 |
| `IN` subquery       | `WHERE id IN (...)`    | Multiple values           |
| `EXISTS` subquery   | `WHERE EXISTS (...)`   | Boolean existence         |
| Correlated subquery | references outer query | Depends on outer row      |
| Derived table       | `FROM (SELECT ...)`    | Temporary result relation |

Let's understand them one by one.

---

# 8. Scalar Subquery

A scalar subquery returns **one value**.

Example:

> Find orders whose amount is greater than the average order amount.

```sql
SELECT
    id,
    amount
FROM orders
WHERE amount > (
    SELECT AVG(amount)
    FROM orders
);
```

The subquery:

```sql
SELECT AVG(amount)
FROM orders;
```

might produce:

```text
533.33
```

Conceptually:

```text
             ┌──────────────────────┐
             │ SELECT AVG(amount)   │
             │ FROM orders          │
             └──────────┬───────────┘
                        │
                        ↓
                    533.33
                        │
                        ↓
             amount > 533.33
```

This is a very natural use of a scalar subquery.

---

# 9. Could We Use a JOIN Instead?

Yes, but it becomes more complicated.

For example:

```sql
SELECT
    o.id,
    o.amount
FROM orders AS o
CROSS JOIN (
    SELECT AVG(amount) AS average_amount
    FROM orders
) AS avg_orders
WHERE o.amount > avg_orders.average_amount;
```

This works.

But conceptually:

```text
"I need one calculated value"
```

is naturally represented by:

```sql
(
    SELECT AVG(amount)
    FROM orders
)
```

So a subquery can make the intent clearer.

---

# 10. `IN` Subquery

Suppose:

> Find customers who have orders.

```sql
SELECT
    id,
    name
FROM customers
WHERE id IN (
    SELECT customer_id
    FROM orders
);
```

The subquery produces:

```text
1
2
```

Then the outer query effectively asks:

```text
customer.id ∈ {1, 2}
```

Result:

```text
Alice
Bob
```

---

# 11. `IN` vs `EXISTS`

These can express similar requirements.

### IN

```sql
SELECT *
FROM customers AS c
WHERE c.id IN (
    SELECT o.customer_id
    FROM orders AS o
);
```

### EXISTS

```sql
SELECT *
FROM customers AS c
WHERE EXISTS (
    SELECT 1
    FROM orders AS o
    WHERE o.customer_id = c.id
);
```

Conceptually:

```text
IN
 ↓
"Is this value contained in that result?"

EXISTS
 ↓
"Does at least one matching row exist?"
```

Both can be useful.

For relationship/existence logic, `EXISTS` often communicates the intention particularly clearly.

---

# 12. A Very Important Difference: `JOIN` Can Multiply Rows

Consider:

```text
customers

Alice
Bob
```

and:

```text
orders

Alice → Order 101
Alice → Order 102
Alice → Order 103
Bob   → Order 104
```

JOIN:

```sql
SELECT c.name
FROM customers AS c
JOIN orders AS o
    ON c.id = o.customer_id;
```

Produces:

```text
Alice
Alice
Alice
Bob
```

Why?

Because JOIN returns matching **row combinations**.

---

But:

```sql
SELECT c.name
FROM customers AS c
WHERE EXISTS (
    SELECT 1
    FROM orders AS o
    WHERE o.customer_id = c.id
);
```

Produces:

```text
Alice
Bob
```

Because EXISTS answers an existence question.

---

# 13. `JOIN + DISTINCT` vs `EXISTS`

You may see:

```sql
SELECT DISTINCT c.id, c.name
FROM customers AS c
JOIN orders AS o
    ON c.id = o.customer_id;
```

This can produce the same final result as:

```sql
SELECT c.id, c.name
FROM customers AS c
WHERE EXISTS (
    SELECT 1
    FROM orders AS o
    WHERE o.customer_id = c.id
);
```

But they express different ideas.

### JOIN version

```text
Find matching rows
       ↓
Create combinations
       ↓
Remove duplicates
```

### EXISTS version

```text
Check whether a match exists
       ↓
Keep the customer once
```

Therefore, if your requirement is explicitly:

> "Return customers who have at least one order."

`EXISTS` is often the more direct expression.

---

# 14. Correlated Subquery

This is an important concept.

Consider:

```sql
SELECT
    c.name
FROM customers AS c
WHERE EXISTS (
    SELECT 1
    FROM orders AS o
    WHERE o.customer_id = c.id
);
```

Notice:

```sql
c.id
```

inside the subquery.

`c` belongs to the **outer query**.

Therefore the inner query is correlated with the current outer row.

Conceptually:

```text
Outer query
───────────

Alice
  |
  | c.id = 1
  ↓
Inner query
  |
  | Find orders where customer_id = 1
  ↓
  EXISTS?


Bob
  |
  | c.id = 2
  ↓
Inner query
  |
  | Find orders where customer_id = 2
  ↓
  EXISTS?
```

That's called a **correlated subquery**.

---

# 15. Correlated Does Not Automatically Mean Slow

A common misconception is:

> "Correlated subquery means PostgreSQL runs the inner query from scratch for every row."

You should **not** assume that.

PostgreSQL's optimizer can transform queries and choose different execution strategies.

For example, an `EXISTS` query may appear in an execution plan as:

```text
Hash Semi Join
```

or:

```text
Nested Loop Semi Join
```

or:

```text
Merge Semi Join
```

So:

```text
SQL syntax
    ↓
PostgreSQL optimizer
    ↓
Execution plan
    ↓
Actual execution
```

The SQL you write does not necessarily dictate the physical algorithm.

---

# 16. Derived Table Subquery

A subquery can also appear inside `FROM`.

Example:

```sql
SELECT
    customer_id,
    order_count
FROM (
    SELECT
        customer_id,
        COUNT(*) AS order_count
    FROM orders
    GROUP BY customer_id
) AS customer_orders;
```

The inner query produces:

| customer_id | order_count |
| ----------: | ----------: |
|           1 |           2 |
|           2 |           1 |

The outer query then works with that result.

Think of it as:

```text
orders
   |
   ↓
GROUP BY customer_id
   |
   ↓
┌──────────────────────┐
│ customer_id | count  │
├──────────────────────┤
│ 1           | 2      │
│ 2           | 1      │
└──────────────────────┘
   |
   ↓
Outer query
```

This becomes especially useful when you need to **aggregate something before joining it**.

---

# 17. Example: Avoiding Join Explosion

Suppose:

```text
customers
    |
    +---- orders
    |
    +---- support_tickets
```

Alice has:

```text
3 orders
2 tickets
```

If you directly join both:

```sql
SELECT
    c.id,
    COUNT(o.id),
    COUNT(t.id)
FROM customers AS c
LEFT JOIN orders AS o
    ON c.id = o.customer_id
LEFT JOIN support_tickets AS t
    ON c.id = t.customer_id
GROUP BY c.id;
```

The intermediate result can contain:

```text
3 orders × 2 tickets = 6 rows
```

So naive counts can become incorrect.

Instead, aggregate first:

```sql
SELECT
    c.id,
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

Now:

```text
orders
  ↓
aggregate
  ↓
1 row/customer
  ↓
JOIN


tickets
  ↓
aggregate
  ↓
1 row/customer
  ↓
JOIN
```

This is an extremely useful production SQL pattern.

---

# 18. JOIN vs Subquery: They Are Not Always Opposites

This is important.

You don't have to think:

```text
JOIN OR subquery
```

Sometimes the correct query uses **both**.

For example:

```sql
SELECT
    c.id,
    c.name,
    o.order_count
FROM customers AS c
LEFT JOIN (
    SELECT
        customer_id,
        COUNT(*) AS order_count
    FROM orders
    GROUP BY customer_id
) AS o
    ON c.id = o.customer_id
WHERE c.status = 'ACTIVE';
```

Here:

* outer query uses a JOIN
* inner query performs aggregation
* outer query filters customers

SQL allows you to compose these techniques.

---

# 19. Subquery vs JOIN: Practical Comparison

| Requirement                                          | Natural approach       |
| ---------------------------------------------------- | ---------------------- |
| Need columns from both tables                        | `JOIN`                 |
| Need to know whether a related row exists            | `EXISTS`               |
| Need to know whether no related row exists           | `NOT EXISTS`           |
| Compare against one calculated value                 | Scalar subquery        |
| Check membership in a result set                     | `IN`                   |
| Aggregate a table before joining                     | Derived table/subquery |
| Need matching rows themselves                        | `JOIN`                 |
| Need one parent row regardless of number of children | `EXISTS`               |
| Need to combine two datasets                         | `JOIN`                 |

---

# 20. Example: Three Different Requirements

Imagine:

```text
customers
orders
```

### Requirement 1

> Give me customers and their orders.

Use JOIN:

```sql
SELECT
    c.name,
    o.id,
    o.amount
FROM customers AS c
JOIN orders AS o
    ON c.id = o.customer_id;
```

---

### Requirement 2

> Give me customers who have at least one order.

Use EXISTS:

```sql
SELECT
    c.name
FROM customers AS c
WHERE EXISTS (
    SELECT 1
    FROM orders AS o
    WHERE o.customer_id = c.id
);
```

---

### Requirement 3

> Give me orders above the average order amount.

Use scalar subquery:

```sql
SELECT
    id,
    amount
FROM orders
WHERE amount > (
    SELECT AVG(amount)
    FROM orders
);
```

Notice how the **business question determines the SQL structure**.

---

# 21. A Common Mistake

Suppose the requirement is:

> Find customers who have placed at least one order.

Someone writes:

```sql
SELECT
    c.id,
    c.name
FROM customers AS c
JOIN orders AS o
    ON c.id = o.customer_id;
```

They might later discover:

```text
Alice
Alice
Bob
```

Then they add:

```sql
DISTINCT
```

```sql
SELECT DISTINCT
    c.id,
    c.name
FROM customers AS c
JOIN orders AS o
    ON c.id = o.customer_id;
```

This may work.

But first ask:

> **Why am I joining orders at all?**

If the only requirement is existence, then:

```sql
WHERE EXISTS (...)
```

directly expresses the requirement.

---

# 22. Another Common Mistake: Using JOIN When You Need a Single Value

Suppose:

> Find products whose price is greater than the average product price.

A subquery is natural:

```sql
SELECT
    id,
    name,
    price
FROM products
WHERE price > (
    SELECT AVG(price)
    FROM products
);
```

The question is:

```text
price > one calculated value
```

That's exactly what a scalar subquery represents.

---

# 23. Performance: Don't Use Rules Like "JOIN Is Faster"

You may hear statements such as:

> "JOIN is always faster than a subquery."

or:

> "EXISTS is always faster."

These are unreliable rules.

PostgreSQL can transform and optimize many logically equivalent queries.

Actual performance depends on things such as:

* table size
* indexes
* statistics
* selectivity
* data distribution
* PostgreSQL version
* query structure
* chosen execution plan

When performance matters:

```sql
EXPLAIN ANALYZE
```

is the appropriate way to investigate.

We'll study execution plans later in the course.

---

# 24. The Deeper Mental Model

Think about the question you're asking.

### JOIN

```text
"I want to combine these related rows."
```

```text
A ───────── JOIN ───────── B
             │
             ↓
       combined rows
```

### EXISTS

```text
"I only care whether B exists."
```

```text
A
│
├── Does matching B exist?
│
├── YES → keep A
│
└── NO  → discard A
```

### NOT EXISTS

```text
"I only care whether B does NOT exist."
```

```text
A
│
├── Does matching B exist?
│
├── YES → discard A
│
└── NO  → keep A
```

### Scalar subquery

```text
"I need one calculated value."
```

```text
Subquery
   │
   ↓
 one value
   │
   ↓
Outer query comparison
```

---

# 25. Decision Tree

When writing a query, you can use this:

```text
                  What do I need?
                        |
          ┌─────────────┴─────────────┐
          |                           |
   Related row data?            Only existence?
          |                           |
         YES                          YES
          |                           |
        JOIN                  ┌────────┴────────┐
                              |                 |
                         Must exist?       Must NOT exist?
                              |                 |
                           EXISTS           NOT EXISTS
```

If you need a calculated value:

```text
Need one calculated value?
          |
         YES
          |
    Scalar subquery
```

If you need to transform/aggregate a dataset first:

```text
Need to aggregate/filter
before combining?
          |
         YES
          |
   Derived table
   / subquery
          |
          ↓
        JOIN
```

---

# 26. Interview Perspective

### Question

> When would you prefer `EXISTS` over `JOIN`?

A good answer:

> When I only need to determine whether a related row exists and don't need columns from the related table. `EXISTS` expresses the existence requirement directly and avoids producing multiple result rows when the related table contains multiple matches.

---

### Question

> Does a correlated subquery always execute once per outer row?

No.

The SQL is logically correlated, but PostgreSQL's optimizer can transform it into an appropriate execution strategy, such as a semi join.

---

### Question

> Is JOIN always faster than a subquery?

No.

You should compare the actual execution plans and runtime rather than relying on a blanket rule.

---

### Question

> Why can JOIN produce duplicate-looking rows while EXISTS doesn't?

Because JOIN returns matching row combinations. If one row in A matches multiple rows in B, A can appear multiple times.

`EXISTS` only asks whether at least one match exists, so the outer row is retained once.

---

# 27. Golden Rules

Remember these:

1. **JOIN = combine related rows.**
2. **EXISTS = at least one matching row exists.**
3. **NOT EXISTS = no matching row exists.**
4. **Scalar subquery = one calculated value.**
5. **IN = membership in a result set.**
6. JOIN can multiply rows.
7. EXISTS does not multiply the outer result because of multiple matches.
8. Don't use `DISTINCT` blindly to hide JOIN multiplication.
9. Correlated subquery does not automatically mean poor performance.
10. Don't assume JOIN or subquery is universally faster.
11. Choose the construct that most directly expresses the question.
12. Use `EXPLAIN ANALYZE` when performance actually matters.

---

# 28. The One Sentence to Remember

> **If you need the related data, think `JOIN`; if you only need to know whether related data exists, think `EXISTS`; if you need a calculated value, think subquery.**

### Next concept: **20. CTEs and Joins**

We'll look at how `WITH` queries let us break complex JOIN logic into named, readable steps, and when CTEs are useful versus regular subqueries.

