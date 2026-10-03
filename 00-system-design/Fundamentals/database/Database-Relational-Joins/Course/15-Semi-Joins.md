# 15. Semi Joins

A **semi join** answers the opposite question of an anti join:

> **"Give me rows from table A where at least one matching row exists in table B."**

The important part is:

> We only want rows from **A**. We don't need any columns from B.

---

## 1. Simple Example

Suppose we have:

### `customers`

| id | name    |
| -: | ------- |
|  1 | Alice   |
|  2 | Bob     |
|  3 | Charlie |

### `orders`

|  id | customer_id | amount |
| --: | ----------: | -----: |
| 101 |           1 |    100 |
| 102 |           1 |    200 |
| 103 |           2 |     50 |

We want:

> Find customers who have placed **at least one order**.

Expected result:

| id | name  |
| -: | ----- |
|  1 | Alice |
|  2 | Bob   |

Charlie should not appear.

---

# 2. The Semi-Join Mental Model

Think of it as an existence test:

```text
Customer
   │
   ▼
Does at least one matching order exist?
   │
   ├── YES → KEEP customer
   │
   └── NO  → DISCARD customer
```

Notice something important:

```text
We don't care HOW MANY orders exist.
We only care whether AT LEAST ONE exists.
```

That distinction is what makes a semi join different from a normal `JOIN`.

---

# 3. The Most Natural SQL: EXISTS

The most common way to express a semi join is `EXISTS`.

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

```text
Alice
Bob
```

### What happens for Alice?

PostgreSQL checks:

```text
Does an order exist where order.customer_id = 1?

101 → customer_id = 1 → YES
```

Once a matching row exists, the condition is satisfied.

It doesn't matter that Alice has another order:

```text
102 → customer_id = 1
```

We already know:

```text
EXISTS = TRUE
```

---

# 4. Why `SELECT 1`?

You will commonly see:

```sql
WHERE EXISTS (
    SELECT 1
    FROM orders AS o
    WHERE o.customer_id = c.id
);
```

You might wonder:

> Why `SELECT 1` instead of `SELECT *`?

Because `EXISTS` doesn't care about the actual values returned.

It only asks:

```text
Did the subquery produce at least one row?
```

So these are conceptually equivalent for `EXISTS`:

```sql
SELECT 1
```

```sql
SELECT *
```

```sql
SELECT o.id
```

But:

```sql
SELECT 1
```

communicates the intention clearly:

> "I'm checking existence, not retrieving data."

---

# 5. Semi Join vs Normal INNER JOIN

This is one of the most important concepts.

You might write:

```sql
SELECT
    c.id,
    c.name
FROM customers AS c
JOIN orders AS o
    ON c.id = o.customer_id;
```

At first glance, this seems like it should return customers who have orders.

But look at Alice.

Alice has:

```text
Order 101
Order 102
```

So the JOIN produces:

```text
Alice + Order 101
Alice + Order 102
```

Result:

| customer | order |
| -------- | ----: |
| Alice    |   101 |
| Alice    |   102 |
| Bob      |   103 |

Alice appears **twice**.

---

## 6. The Difference Visually

### INNER JOIN

```text
Customer Alice
      │
      ├──── Order 101
      │
      └──── Order 102

Result:
Alice + 101
Alice + 102
```

### SEMI JOIN

```text
Customer Alice
      │
      ├──── Order 101
      │
      └──── Order 102

       ↓

At least one exists?

       ↓

      YES

       ↓

Keep Alice ONCE
```

That's the key difference.

---

# 7. `JOIN` Returns Matches

A normal join says:

> "Give me the combinations of matching rows."

```sql
SELECT
    c.name,
    o.id
FROM customers AS c
JOIN orders AS o
    ON c.id = o.customer_id;
```

For Alice:

```text
Alice + 101
Alice + 102
```

---

# 8. `EXISTS` Returns the Left Row

A semi join says:

> "Give me the customer if a matching order exists."

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

For Alice:

```text
Order 101 exists
        ↓
      TRUE
        ↓
Alice
```

The actual order rows are not returned.

---

# 9. Semi Join vs Anti Join

You just learned anti joins.

They are almost mirror images.

### Semi Join

> Keep A if B exists.

```sql
WHERE EXISTS (...)
```

### Anti Join

> Keep A if B does NOT exist.

```sql
WHERE NOT EXISTS (...)
```

Visual:

```text
                 Matching B exists?
                       │
              ┌────────┴────────┐
              │                 │
             YES                NO
              │                 │
          SEMI JOIN         ANTI JOIN
              │                 │
           KEEP A           KEEP A
```

Example:

### Customers who have orders

```sql
SELECT c.*
FROM customers AS c
WHERE EXISTS (
    SELECT 1
    FROM orders AS o
    WHERE o.customer_id = c.id
);
```

### Customers who don't have orders

```sql
SELECT c.*
FROM customers AS c
WHERE NOT EXISTS (
    SELECT 1
    FROM orders AS o
    WHERE o.customer_id = c.id
);
```

Very useful pair to remember:

```text
EXISTS
   ↓
"at least one exists"

NOT EXISTS
   ↓
"none exist"
```

---

# 10. Why `DISTINCT` Is Sometimes Used With JOIN

You might see:

```sql
SELECT DISTINCT
    c.id,
    c.name
FROM customers AS c
JOIN orders AS o
    ON c.id = o.customer_id;
```

This gives:

| id | name  |
| -: | ----- |
|  1 | Alice |
|  2 | Bob   |

So it appears to solve the problem.

But notice what happened:

```text
JOIN
 ↓
multiple Alice rows
 ↓
DISTINCT
 ↓
collapse them
```

Whereas:

```sql
EXISTS
```

expresses the requirement directly:

```text
Does a matching order exist?
        ↓
      YES
        ↓
      Alice
```

---

# 11. `JOIN + DISTINCT` vs `EXISTS`

Compare the intent.

### JOIN + DISTINCT

```sql
SELECT DISTINCT c.id, c.name
FROM customers AS c
JOIN orders AS o
    ON c.id = o.customer_id;
```

Meaning:

> Join customers with orders, then remove duplicate customers.

### EXISTS

```sql
SELECT c.id, c.name
FROM customers AS c
WHERE EXISTS (
    SELECT 1
    FROM orders AS o
    WHERE o.customer_id = c.id
);
```

Meaning:

> Return customers for whom an order exists.

The second query directly expresses the business question.

---

# 12. Another Example: Products That Were Purchased

Suppose:

### `products`

```text
products
---------
id
name
```

### `order_items`

```text
order_items
-----------
id
order_id
product_id
quantity
```

Question:

> Which products have been purchased at least once?

Use:

```sql
SELECT
    p.id,
    p.name
FROM products AS p
WHERE EXISTS (
    SELECT 1
    FROM order_items AS oi
    WHERE oi.product_id = p.id
);
```

The query doesn't care:

```text
How many orders?
How many customers?
How many units?
```

It only asks:

```text
Does at least one order_item exist?
```

---

# 13. Semi Join With Conditions

The existence test can contain additional conditions.

Suppose we want:

> Customers who placed an order over $500.

```sql
SELECT
    c.id,
    c.name
FROM customers AS c
WHERE EXISTS (
    SELECT 1
    FROM orders AS o
    WHERE o.customer_id = c.id
      AND o.amount > 500
);
```

This means:

```text
For each customer:

Does there exist an order
    belonging to this customer
    AND amount > 500?

       YES → keep customer
       NO  → discard customer
```

---

# 14. "At Least One" Is a Strong Signal for EXISTS

When you see requirements such as:

> Customers who have at least one order

```sql
EXISTS
```

> Students enrolled in at least one course

```sql
EXISTS
```

> Employees assigned to an active project

```sql
EXISTS
```

> Products that have been purchased

```sql
EXISTS
```

> Users who have logged in at least once

```sql
EXISTS
```

These are all existence questions.

---

# 15. Example: Students With Enrollments

Schema:

```text
students
   │
   │ 1
   │
   │ N
   ▼
enrollments
   │
   │ N
   │
   │ 1
   ▼
courses
```

Question:

> Find students who are enrolled in at least one course.

```sql
SELECT
    s.id,
    s.name
FROM students AS s
WHERE EXISTS (
    SELECT 1
    FROM enrollments AS e
    WHERE e.student_id = s.id
);
```

We don't even need to join `courses`.

Why?

Because the question is:

```text
Does an enrollment exist?
```

not:

```text
Which courses?
```

If we need the course name, that's a different requirement.

---

# 16. When a JOIN Is Actually Better

Semi join isn't automatically better just because `EXISTS` exists.

Suppose the requirement is:

> Show every customer together with their order ID and order amount.

Then we need columns from `orders`:

```sql
SELECT
    c.name,
    o.id AS order_id,
    o.amount
FROM customers AS c
JOIN orders AS o
    ON c.id = o.customer_id;
```

That's a normal join.

Because we actually need:

```text
customer data
+
order data
```

But if the requirement is:

> Which customers have at least one order?

Then:

```sql
WHERE EXISTS (...)
```

is a natural expression.

---

# 17. Semi Join and Many-to-Many Relationships

Suppose:

```text
students
   │
   │
   ▼
enrollments
   │
   │
   ▼
courses
```

Question:

> Find students enrolled in at least one course.

```sql
SELECT
    s.id,
    s.name
FROM students AS s
WHERE EXISTS (
    SELECT 1
    FROM enrollments AS e
    WHERE e.student_id = s.id
);
```

Notice that we don't create:

```text
student × enrollment × course
```

result rows.

We only perform an existence test.

---

# 18. Semi Join With Multiple Conditions

Suppose we want:

> Customers who have at least one successful order in the last 30 days.

```sql
SELECT
    c.id,
    c.name
FROM customers AS c
WHERE EXISTS (
    SELECT 1
    FROM orders AS o
    WHERE o.customer_id = c.id
      AND o.status = 'SUCCESS'
      AND o.created_at >= CURRENT_DATE - INTERVAL '30 days'
);
```

The business rule becomes almost readable as English:

```text
SELECT customers
WHERE EXISTS
    a successful order
    for this customer
    within the last 30 days
```

---

# 19. Semi Join and `IN`

Another way to express some semi-join logic is `IN`.

For example:

```sql
SELECT
    c.id,
    c.name
FROM customers AS c
WHERE c.id IN (
    SELECT o.customer_id
    FROM orders AS o
);
```

This asks:

```text
Is customer.id present among order.customer_id values?
```

Conceptually:

```text
customers.id
     │
     ▼
Is it in the set of order.customer_id?
     │
   YES → keep
   NO  → discard
```

For simple existence checks, you will often see both:

```sql
WHERE EXISTS (...)
```

and

```sql
WHERE id IN (...)
```

---

# 20. `EXISTS` vs `IN`

For example:

```sql
-- EXISTS
SELECT c.*
FROM customers AS c
WHERE EXISTS (
    SELECT 1
    FROM orders AS o
    WHERE o.customer_id = c.id
);
```

versus:

```sql
-- IN
SELECT c.*
FROM customers AS c
WHERE c.id IN (
    SELECT o.customer_id
    FROM orders AS o
);
```

They can express similar positive-existence logic.

But there is an important distinction in SQL semantics around `NULL`, especially when using `NOT IN`.

For positive existence checks, `EXISTS` is often very clear because it directly expresses:

> Does a matching row exist?

For negative existence, remember the important lesson from anti joins:

```text
NOT EXISTS
```

is safer conceptually than blindly using:

```text
NOT IN
```

because of SQL's `NULL` behavior.

---

# 21. PostgreSQL's Execution Perspective

Conceptually, PostgreSQL can recognize that an `EXISTS` query represents a **semi join**.

You might see something like this in an execution plan:

```text
Nested Loop Semi Join
```

or:

```text
Hash Semi Join
```

or:

```text
Merge Semi Join
```

We'll study these execution concepts later in the course.

For now, the important distinction is:

```text
SQL meaning:

EXISTS
  ↓
Semi Join
```

You don't need to manually write something like:

```sql
SEMI JOIN
```

There is no ordinary SQL syntax:

```sql
SEMI JOIN
```

Instead, you normally express it using:

```sql
EXISTS
```

or sometimes `IN`.

---

# 22. Important Difference: JOIN Can Multiply, EXISTS Doesn't

Suppose Alice has:

```text
Order 101
Order 102
Order 103
```

### JOIN

```sql
SELECT c.id
FROM customers c
JOIN orders o
  ON c.id = o.customer_id;
```

Conceptually:

```text
Alice + 101
Alice + 102
Alice + 103
```

Alice appears 3 times.

### EXISTS

```sql
SELECT c.id
FROM customers c
WHERE EXISTS (
    SELECT 1
    FROM orders o
    WHERE o.customer_id = c.id
);
```

Conceptually:

```text
Alice
```

once.

This is one of the biggest reasons to recognize semi-join requirements.

---

# 23. A Practical Decision Rule

When writing a query, ask:

### Do I need columns from B?

```text
YES
 ↓
JOIN may be appropriate
```

### No?

Then ask:

> Do I only need to know whether B exists?

```text
YES
 ↓
EXISTS / semi join
```

Visual:

```text
                 Need data from B?
                       │
              ┌────────┴────────┐
              │                 │
             YES                NO
              │                 │
             JOIN         Need to know
                          whether B exists?
                               │
                          ┌────┴────┐
                          │         │
                         YES       NO
                          │
                       EXISTS
```

---

# 24. Semi Join vs Anti Join Cheat Sheet

| Requirement                                   | Pattern      |
| --------------------------------------------- | ------------ |
| A where B exists                              | `EXISTS`     |
| A where B doesn't exist                       | `NOT EXISTS` |
| A plus B's data                               | `JOIN`       |
| A plus B's data, preserve unmatched A         | `LEFT JOIN`  |
| A where matching B exists, but don't return B | `EXISTS`     |
| A where no matching B exists                  | `NOT EXISTS` |

---

# 25. Real-World Examples

### Customers who have placed an order

```sql
WHERE EXISTS (
    SELECT 1
    FROM orders o
    WHERE o.customer_id = c.id
)
```

### Customers who have never placed an order

```sql
WHERE NOT EXISTS (
    SELECT 1
    FROM orders o
    WHERE o.customer_id = c.id
)
```

### Employees assigned to a project

```sql
WHERE EXISTS (
    SELECT 1
    FROM employee_projects ep
    WHERE ep.employee_id = e.id
)
```

### Employees not assigned to any project

```sql
WHERE NOT EXISTS (
    SELECT 1
    FROM employee_projects ep
    WHERE ep.employee_id = e.id
)
```

### Products that have been purchased

```sql
WHERE EXISTS (
    SELECT 1
    FROM order_items oi
    WHERE oi.product_id = p.id
)
```

### Products never purchased

```sql
WHERE NOT EXISTS (
    SELECT 1
    FROM order_items oi
    WHERE oi.product_id = p.id
)
```

Notice the symmetry:

```text
          EXISTS       NOT EXISTS
             │              │
             ▼              ▼
        Semi Join       Anti Join
             │              │
        matching B      no matching B
             │              │
          KEEP A          KEEP A
```

---

# 26. Common Mistakes

## Mistake 1: Using JOIN when you only need existence

```sql
SELECT DISTINCT c.id
FROM customers c
JOIN orders o
  ON c.id = o.customer_id;
```

This can work, but you're generating matching rows and then removing repeated customers.

For an existence requirement, consider:

```sql
SELECT c.id
FROM customers c
WHERE EXISTS (
    SELECT 1
    FROM orders o
    WHERE o.customer_id = c.id
);
```

---

## Mistake 2: Forgetting that JOIN multiplies rows

If one customer has 100 orders:

```text
JOIN → potentially 100 result rows
EXISTS → one customer row
```

Always think about cardinality.

---

## Mistake 3: Using `NOT IN` without considering NULL

Be particularly careful with:

```sql
WHERE c.id NOT IN (
    SELECT o.customer_id
    FROM orders o
);
```

If `o.customer_id` can contain `NULL`, SQL's three-valued logic can produce surprising results.

For "no matching row exists", prefer understanding and usually expressing the requirement as:

```sql
WHERE NOT EXISTS (...)
```

---

# 27. The Core Mental Model

A normal join asks:

```text
A ─────── matches ─────── B

Return the matching combinations.
```

A semi join asks:

```text
A
│
│ Does B have at least one match?
│
├── YES ──→ Return A
│
└── NO  ──→ Don't return A
```

An anti join asks:

```text
A
│
│ Does B have at least one match?
│
├── YES ──→ Don't return A
│
└── NO  ──→ Return A
```

So:

```text
              Does B exist?
                   │
            ┌──────┴──────┐
            │             │
           YES            NO
            │             │
         SEMI          ANTI
       (EXISTS)    (NOT EXISTS)
            │             │
         KEEP A         KEEP A
```

---

# 28. Golden Rules

Remember these:

1. **Semi join = rows from A where at least one B exists.**
2. The most common SQL expression is **`EXISTS`**.
3. `EXISTS` doesn't return B's data.
4. `JOIN` can multiply A's rows.
5. `EXISTS` gives you A rows based on existence, without that multiplication in the result.
6. `SELECT 1` inside `EXISTS` is conventional because the actual value doesn't matter.
7. `JOIN + DISTINCT` can sometimes produce the same result, but `EXISTS` expresses existence directly.
8. `NOT EXISTS` is the negative counterpart: an **anti join**.
9. If you actually need columns from B, a normal `JOIN` is usually the appropriate concept.
10. **"At least one exists" is a strong signal to think `EXISTS`.**

---

## Quick Comparison

```text
INNER JOIN

A ─────── B
│         │
└─ match ─┘
    ↓
return matching combinations


SEMI JOIN

A ─────── B
│         │
└─ exists?
    ↓
return A once


ANTI JOIN

A ─────── B
│         │
└─ doesn't exist?
    ↓
return A
```

### One sentence to remember

> **JOIN asks "what matches?" — `EXISTS` asks "does a match exist?" — `NOT EXISTS` asks "does no match exist?"**

---

## Next: 16. `EXISTS` vs `JOIN`

We'll go deeper into **why `EXISTS` and `JOIN` are not interchangeable**, especially around row multiplication, `DISTINCT`, `NULL`, correlated subqueries, and how PostgreSQL can optimize both into different join strategies.

