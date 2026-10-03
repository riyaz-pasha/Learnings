# 27. Advanced Join Patterns

We've covered the core JOIN concepts and PostgreSQL's execution strategies.

Now we move into patterns you'll encounter frequently in **real applications and production SQL**.

The important shift is:

```text
Basic JOIN
    ↓
"I know how JOIN works"
    ↓
Advanced JOIN pattern
    ↓
"I know how to solve a real query problem with JOIN"
```

We'll start with one of the most common:

> **Finding the latest related row for each parent.**

---

# 1. The "latest row per group" problem

Suppose we have:

```text
customers
---------
id
name
```

and:

```text
orders
------
id
customer_id
amount
created_at
```

A common requirement is:

> "Give me every customer and their latest order."

For example:

```text
customers

1 Alice
2 Bob
3 Charlie
```

```text
orders

id   customer_id   amount   created_at
---  -----------   ------   ----------
101      1          500     Jan 10
102      1          700     Jan 20
103      2          300     Jan 15
104      1          900     Feb 01
```

Desired result:

```text
Alice   104   900
Bob     103   300
Charlie NULL  NULL
```

The challenge is:

> We don't want **all** orders. We want **one particular order per customer**.

---

# 2. The naive JOIN doesn't solve it

You might start with:

```sql id="k0fq6v"
SELECT
    c.name,
    o.id,
    o.amount,
    o.created_at
FROM customers AS c
LEFT JOIN orders AS o
    ON c.id = o.customer_id;
```

Result:

```text
Alice    101    500
Alice    102    700
Alice    104    900
Bob      103    300
Charlie  NULL   NULL
```

But we need:

```text
Alice    104    900
Bob      103    300
Charlie  NULL   NULL
```

So we need another technique.

---

# 3. Pattern 1: `ROW_NUMBER()`

One of the most useful solutions is a window function.

```sql id="w2h3j8"
SELECT
    c.name,
    o.id,
    o.amount,
    o.created_at
FROM customers AS c
LEFT JOIN (
    SELECT
        id,
        customer_id,
        amount,
        created_at,
        ROW_NUMBER() OVER (
            PARTITION BY customer_id
            ORDER BY created_at DESC
        ) AS rn
    FROM orders
) AS o
    ON c.id = o.customer_id
   AND o.rn = 1;
```

Let's break this down.

---

# 4. What does `ROW_NUMBER()` do?

Inside the subquery:

```sql id="e6ut7x"
ROW_NUMBER() OVER (
    PARTITION BY customer_id
    ORDER BY created_at DESC
)
```

means:

> Number orders separately for each customer, with the newest order getting number 1.

For example:

```text id="3m0lqk"
customer_id   created_at   row_number
-----------   ----------   ----------
1             Feb 01       1
1             Jan 20       2
1             Jan 10       3

2             Jan 15       1
```

So:

```text id="4m0n7d"
rn = 1
```

means:

> This is the latest order for that customer.

---

# 5. Why `PARTITION BY`?

Without:

```sql id="k0xk2b"
PARTITION BY customer_id
```

PostgreSQL would number the entire result set:

```text
1
2
3
4
5
...
```

But we want numbering to restart for every customer.

Therefore:

```text id="zjv5xc"
PARTITION BY customer_id
```

creates independent groups:

```text id="9r7g8a"
Customer 1
---------
1
2
3

Customer 2
---------
1
2

Customer 3
---------
1
```

---

# 6. Why `ORDER BY created_at DESC`?

We want the newest order first:

```text id="1m8f2n"
Feb 01
Jan 20
Jan 10
```

Therefore:

```sql id="f8d5c7"
ORDER BY created_at DESC
```

gives:

```text id="l2wq1j"
Feb 01 → rn = 1
Jan 20 → rn = 2
Jan 10 → rn = 3
```

Then:

```sql id="3h8d1p"
WHERE rn = 1
```

or, in our JOIN:

```sql id="f5k0zs"
AND o.rn = 1
```

keeps only the latest row.

---

# 7. Why put `rn = 1` in `ON`?

Notice:

```sql id="7yk4g3"
LEFT JOIN (...) AS o
    ON c.id = o.customer_id
   AND o.rn = 1;
```

rather than:

```sql id="x7k3n2"
LEFT JOIN (...) AS o
    ON c.id = o.customer_id
WHERE o.rn = 1;
```

This connects directly to our earlier lesson about:

> **ON vs WHERE with LEFT JOIN**

The first version:

```sql id="z2x6a8"
ON c.id = o.customer_id
AND o.rn = 1
```

means:

> Match the customer with their latest order.

Customers with no orders remain:

```text
Charlie | NULL | NULL
```

The second version:

```sql id="k1w3u7"
WHERE o.rn = 1
```

would remove rows where:

```text
o.rn IS NULL
```

and therefore could eliminate customers without orders.

---

# 8. Visualizing the query

```text id="x1q7e2"
                 customers
                     |
                     v
                 LEFT JOIN
                     |
                     |
        +------------+-------------+
        |                          |
        v                          v
   customer.id              ranked orders
                                  |
                                  v
                        PARTITION BY customer
                                  |
                                  v
                        ORDER BY newest first
                                  |
                                  v
                           rn = 1
                                  |
                                  v
                           latest order
```

The result:

```text id="f6z0n4"
Customer
   |
   +---- latest order
   |
   +---- or NULL if none exists
```

---

# 9. What if two orders have the same timestamp?

This is an important real-world problem.

Suppose:

```text id="j5m9qr"
id   customer_id   created_at
---  -----------   ----------
101      1         2026-10-01 10:00
102      1         2026-10-01 10:00
```

Both have the same timestamp.

Then:

```sql id="9l8rj5"
ORDER BY created_at DESC
```

doesn't completely specify which one should be first.

So use a deterministic tie-breaker:

```sql id="m5j9sp"
ROW_NUMBER() OVER (
    PARTITION BY customer_id
    ORDER BY created_at DESC, id DESC
)
```

Now:

```text id="bdk4p6"
created_at equal
       ↓
higher id wins
```

This is a very useful production habit.

---

# 10. General pattern

The pattern is:

```sql id="g0p4rz"
ROW_NUMBER() OVER (
    PARTITION BY grouping_column
    ORDER BY ranking_column DESC
)
```

then:

```text id="n3bq9w"
rn = 1
```

means:

> Give me the top row for each group.

Examples:

```text id="6wq9c3"
latest order per customer
latest payment per account
latest status per order
highest salary per department
most recent login per user
latest price per product
```

---

# 11. Example: latest payment per account

Tables:

```text id="2w5q4j"
accounts
--------
id
name

payments
--------
id
account_id
amount
created_at
```

Query:

```sql id="1p7s4f"
SELECT
    a.id,
    a.name,
    p.id AS payment_id,
    p.amount,
    p.created_at
FROM accounts AS a
LEFT JOIN (
    SELECT
        id,
        account_id,
        amount,
        created_at,
        ROW_NUMBER() OVER (
            PARTITION BY account_id
            ORDER BY created_at DESC, id DESC
        ) AS rn
    FROM payments
) AS p
    ON a.id = p.account_id
   AND p.rn = 1;
```

Same pattern.

---

# 12. Pattern 2: PostgreSQL `DISTINCT ON`

Because we're learning **PostgreSQL**, there's another very useful technique.

PostgreSQL supports:

```sql id="b2z1t6"
DISTINCT ON
```

For example:

```sql id="9z6x5p"
SELECT DISTINCT ON (customer_id)
    id,
    customer_id,
    amount,
    created_at
FROM orders
ORDER BY customer_id, created_at DESC, id DESC;
```

This returns one order per customer.

The important part is:

```sql id="2d5y5x"
DISTINCT ON (customer_id)
```

combined with:

```sql id="4b6r7c"
ORDER BY customer_id, created_at DESC, id DESC
```

---

# 13. Why does `ORDER BY` matter with DISTINCT ON?

Consider:

```sql id="x0g8s4"
SELECT DISTINCT ON (customer_id)
    ...
FROM orders
ORDER BY customer_id, created_at DESC;
```

PostgreSQL sees:

```text id="qf8p7j"
customer 1
  newest
  older
  older

customer 2
  newest
  older
```

`DISTINCT ON` keeps the **first row encountered for each distinct group**.

Therefore:

```text id="d8f1c5"
ORDER BY customer_id,
         created_at DESC
```

makes the newest row appear first within each customer.

---

# 14. Important PostgreSQL rule

With:

```sql id="2kq5nx"
DISTINCT ON (customer_id)
```

the `ORDER BY` must begin with:

```text id="7d6v1c"
customer_id
```

For example:

```sql id="u9v3o6"
ORDER BY customer_id, created_at DESC;
```

is appropriate.

Whereas:

```sql id="r3j6q1"
ORDER BY created_at DESC, customer_id;
```

doesn't follow the required ordering relationship for `DISTINCT ON (customer_id)`.

---

# 15. Joining `DISTINCT ON` back to customers

We can combine it with our original requirement:

```sql id="0x4h7m"
SELECT
    c.name,
    o.id,
    o.amount,
    o.created_at
FROM customers AS c
LEFT JOIN (
    SELECT DISTINCT ON (customer_id)
        id,
        customer_id,
        amount,
        created_at
    FROM orders
    ORDER BY customer_id, created_at DESC, id DESC
) AS o
    ON c.id = o.customer_id;
```

This gives:

```text id="z9v4g6"
Alice    latest order
Bob      latest order
Charlie  NULL
```

---

# 16. `ROW_NUMBER()` vs `DISTINCT ON`

Both can solve the problem.

| Technique      | Strength                                |
| -------------- | --------------------------------------- |
| `ROW_NUMBER()` | Standard SQL-style, flexible            |
| `DISTINCT ON`  | PostgreSQL-specific and concise         |
| `DISTINCT ON`  | Very convenient for top row per group   |
| `ROW_NUMBER()` | Better when you need more ranking logic |

For example, if you need:

```text id="8k6tq3"
latest
second latest
third latest
```

then:

```sql id="v0f8c2"
ROW_NUMBER()
```

is naturally suited.

If you simply need:

```text id="y8r1fd"
one latest row per customer
```

PostgreSQL's:

```sql id="l7n2q4"
DISTINCT ON
```

can be very convenient.

---

# 17. Pattern 3: Join against an aggregated result

Another extremely common pattern is:

> "Calculate something per group, then join that result back."

Example:

> Give me every customer and their total spending.

First aggregate:

```sql id="8m3q5f"
SELECT
    customer_id,
    SUM(amount) AS total_spent
FROM orders
GROUP BY customer_id;
```

This produces:

```text
customer_id | total_spent
------------+------------
1           | 1200
2           | 300
```

Now join it:

```sql id="d2v6q1"
SELECT
    c.id,
    c.name,
    COALESCE(o.total_spent, 0) AS total_spent
FROM customers AS c
LEFT JOIN (
    SELECT
        customer_id,
        SUM(amount) AS total_spent
    FROM orders
    GROUP BY customer_id
) AS o
    ON c.id = o.customer_id;
```

---

# 18. Why aggregate before joining?

This is connected to our earlier lesson about **join explosion**.

Suppose:

```text id="f7n4z2"
customers
    |
    +---- orders
    |
    +---- support_tickets
```

If Alice has:

```text id="q6z2sx"
3 orders
4 tickets
```

joining both directly produces:

```text id="4b8m3w"
3 × 4 = 12 rows
```

But if we first aggregate:

```text id="0v8w6s"
orders
   ↓
GROUP BY customer_id
   ↓
1 row per customer

tickets
   ↓
GROUP BY customer_id
   ↓
1 row per customer
```

then:

```text id="z4r6s8"
customers
   +
order summary
   +
ticket summary
```

produces:

```text
1 customer → 1 row
```

This is one of the most useful advanced JOIN patterns.

---

# 19. Pattern 4: Conditional JOIN

Suppose the requirement is:

> Give me every customer, but only their successful orders.

Use:

```sql id="r8v5c1"
SELECT
    c.id,
    c.name,
    o.id AS order_id,
    o.amount
FROM customers AS c
LEFT JOIN orders AS o
    ON c.id = o.customer_id
   AND o.status = 'SUCCESS';
```

The important part is:

```sql id="9y2w5g"
AND o.status = 'SUCCESS'
```

inside `ON`.

Result:

```text id="7w1p4z"
Alice   successful order
Bob     successful order
Charlie NULL
```

---

# 20. Why not WHERE?

If you write:

```sql id="g9n4w2"
SELECT
    c.id,
    c.name,
    o.id
FROM customers AS c
LEFT JOIN orders AS o
    ON c.id = o.customer_id
WHERE o.status = 'SUCCESS';
```

then customers without orders don't satisfy:

```text id="q1s6b8"
o.status = 'SUCCESS'
```

because:

```text id="t7d3x9"
NULL = 'SUCCESS'
```

is UNKNOWN.

So they disappear.

Again:

```text id="5q8c3j"
ON
→ controls which related rows attach

WHERE
→ controls which final rows survive
```

---

# 21. Pattern 5: Range Join

Not every relationship uses equality.

Suppose:

```text id="p8k4v2"
salary_ranges

level   min_salary   max_salary
------  ----------   ----------
Junior  0            50000
Mid     50000        100000
Senior  100000      200000
```

and:

```text id="w1c7x4"
employees

id   name    salary
---  ------  ------
1    Alice   75000
2    Bob     120000
```

We want the employee's salary level.

```sql id="6j8r3m"
SELECT
    e.name,
    e.salary,
    r.level
FROM employees AS e
JOIN salary_ranges AS r
    ON e.salary >= r.min_salary
   AND e.salary < r.max_salary;
```

Result:

```text
Alice   75000    Mid
Bob     120000   Senior
```

This is called a **range join**.

---

# 22. Why `< max_salary` instead of `<=`?

Notice:

```sql id="p3q7f1"
e.salary >= r.min_salary
AND e.salary < r.max_salary
```

rather than:

```sql id="s6m2x9"
e.salary >= r.min_salary
AND e.salary <= r.max_salary
```

Using:

```text
[minimum, maximum)
```

creates non-overlapping ranges.

For example:

```text id="n0v6p4"
Junior:  [0, 50000)
Mid:     [50000, 100000)
Senior:  [100000, 200000)
```

Then:

```text
50000
```

belongs to Mid, not both Junior and Mid.

This is a very useful technique for avoiding overlapping matches.

---

# 23. Pattern 6: Temporal / effective-date JOIN

A very common production problem is:

> "Which price was valid when the order was placed?"

Suppose:

```text
product_prices

product_id
price
valid_from
valid_to
```

and:

```text
orders

id
product_id
created_at
```

Query:

```sql id="j6f1r9"
SELECT
    o.id AS order_id,
    o.created_at,
    p.price
FROM orders AS o
JOIN product_prices AS p
    ON o.product_id = p.product_id
   AND o.created_at >= p.valid_from
   AND o.created_at < p.valid_to;
```

Conceptually:

```text id="7c4m9x"
Order timestamp
       |
       v
Which price range contains this timestamp?
       |
       v
valid_from <= timestamp < valid_to
```

This pattern appears in:

* pricing
* subscriptions
* employment history
* exchange rates
* tax rules
* configuration versions
* feature flags
* contracts

---

# 24. Pattern 7: Join using `EXISTS`

Sometimes you don't actually need columns from the related table.

Requirement:

> "Find customers who have placed at least one successful order."

Use:

```sql id="w4f2k7"
SELECT
    c.id,
    c.name
FROM customers AS c
WHERE EXISTS (
    SELECT 1
    FROM orders AS o
    WHERE o.customer_id = c.id
      AND o.status = 'SUCCESS'
);
```

This is the **semi-join** pattern we learned earlier.

It avoids producing multiple customer rows when a customer has many matching orders.

---

# 25. Pattern 8: Join using `NOT EXISTS`

Requirement:

> "Find customers who have never placed a successful order."

```sql id="j3m8v1"
SELECT
    c.id,
    c.name
FROM customers AS c
WHERE NOT EXISTS (
    SELECT 1
    FROM orders AS o
    WHERE o.customer_id = c.id
      AND o.status = 'SUCCESS'
);
```

Mental model:

```text id="8q2x5w"
Customer
   |
   v
Successful order exists?
   |
   +---- YES → discard
   |
   +---- NO  → keep
```

This is the **anti-join** pattern.

---

# 26. Pattern 9: Self-join for hierarchical relationships

Suppose:

```text
employees

id   name      manager_id
---  --------  ----------
1    Alice     NULL
2    Bob       1
3    Charlie   1
4    David     2
```

Requirement:

> "Show employee and manager."

```sql id="f8w2m6"
SELECT
    e.name AS employee,
    m.name AS manager
FROM employees AS e
LEFT JOIN employees AS m
    ON e.manager_id = m.id;
```

Result:

```text
Alice    NULL
Bob      Alice
Charlie  Alice
David    Bob
```

This is a **self-join** because:

```text
employees
    |
    +---- employee role
    |
    +---- manager role
```

---

# 27. Pattern 10: Multiple independent aggregates

Suppose we need:

```text
customer
total orders
total tickets
total successful payments
```

Don't blindly do:

```text
customers
  JOIN orders
  JOIN tickets
  JOIN payments
```

because you can create:

```text
orders × tickets × payments
```

for each customer.

Instead:

```text id="c8z4n6"
orders
   ↓
aggregate per customer
   ↓
order_summary

tickets
   ↓
aggregate per customer
   ↓
ticket_summary

payments
   ↓
aggregate per customer
   ↓
payment_summary

          ↓
       JOIN summaries
          ↓
       customers
```

For example:

```sql id="v2s8q1"
WITH order_summary AS (
    SELECT
        customer_id,
        COUNT(*) AS order_count
    FROM orders
    GROUP BY customer_id
),
ticket_summary AS (
    SELECT
        customer_id,
        COUNT(*) AS ticket_count
    FROM support_tickets
    GROUP BY customer_id
),
payment_summary AS (
    SELECT
        customer_id,
        COALESCE(SUM(amount), 0) AS total_paid
    FROM payments
    GROUP BY customer_id
)
SELECT
    c.id,
    c.name,
    COALESCE(os.order_count, 0) AS order_count,
    COALESCE(ts.ticket_count, 0) AS ticket_count,
    COALESCE(ps.total_paid, 0) AS total_paid
FROM customers AS c
LEFT JOIN order_summary AS os
    ON c.id = os.customer_id
LEFT JOIN ticket_summary AS ts
    ON c.id = ts.customer_id
LEFT JOIN payment_summary AS ps
    ON c.id = ps.customer_id;
```

Notice the grain:

```text id="1r5w7k"
order_summary
→ one row per customer

ticket_summary
→ one row per customer

payment_summary
→ one row per customer
```

Therefore:

```text id="x6v8c2"
customer
  1 row
    +
order summary
  1 row
    +
ticket summary
  1 row
    +
payment summary
  1 row
```

No multiplication.

---

# 28. Pattern 11: Join to a filtered subquery

Suppose we only care about orders from the last 30 days.

We can isolate them:

```sql id="8p6v4n"
WITH recent_orders AS (
    SELECT *
    FROM orders
    WHERE created_at >= CURRENT_DATE - INTERVAL '30 days'
)
SELECT
    c.name,
    ro.id,
    ro.amount
FROM customers AS c
LEFT JOIN recent_orders AS ro
    ON c.id = ro.customer_id;
```

This makes the intent explicit:

```text id="3h8zq5"
orders
   |
   | last 30 days
   v
recent_orders
   |
   v
LEFT JOIN customers
```

The same logic could often be written directly in the `ON` clause:

```sql id="8g2r4s"
SELECT
    c.name,
    o.id,
    o.amount
FROM customers AS c
LEFT JOIN orders AS o
    ON c.id = o.customer_id
   AND o.created_at >= CURRENT_DATE - INTERVAL '30 days';
```

The choice is often about clarity and query structure rather than automatically assuming one is faster.

---

# 29. Pattern 12: Deduplicating before joining

Suppose a table unexpectedly contains multiple records for the same logical entity.

For example:

```text
customer_contacts

customer_id   email
-----------   ----------------
1             alice@example.com
1             alice@example.com
1             alice@new.com
```

If you join directly:

```sql id="m5j8q2"
JOIN customer_contacts cc
    ON c.id = cc.customer_id
```

you get multiple rows.

Sometimes the requirement is:

> "Use the latest contact record."

Then first identify the desired row:

```sql id="z8w1k4"
WITH ranked_contacts AS (
    SELECT
        customer_id,
        email,
        ROW_NUMBER() OVER (
            PARTITION BY customer_id
            ORDER BY updated_at DESC, id DESC
        ) AS rn
    FROM customer_contacts
)
SELECT
    c.name,
    rc.email
FROM customers AS c
LEFT JOIN ranked_contacts AS rc
    ON c.id = rc.customer_id
   AND rc.rn = 1;
```

Again:

```text id="7x5j2m"
Raw many rows
     ↓
Rank
     ↓
Keep desired row
     ↓
JOIN
```

---

# 30. The "one row per X" question

This is one of the most valuable questions you can ask before writing an advanced JOIN:

> **What should one output row represent?**

For example:

```text id="3v6h2p"
one row per customer
```

versus:

```text id="6n1k5c"
one row per order
```

versus:

```text id="0x7m3q"
one row per order item
```

versus:

```text id="z4b8p1"
one row per customer + month
```

Once you know the grain, the query structure becomes much clearer.

---

# 31. Advanced JOIN decision tree

When facing a real query, use this mental decision tree:

```text id="q1n8c6"
What am I trying to retrieve?
          |
          v
   Need related rows?
      /          \
    YES           NO
     |             |
     v             v
   JOIN        Need existence?
                  /       \
                YES        NO
                 |          |
                 v          v
              EXISTS     other pattern
```

Then:

```text id="m7v4z2"
Need one specific row?
        |
        +---- latest → ROW_NUMBER / DISTINCT ON
        |
        +---- highest → ROW_NUMBER / DISTINCT ON
        |
        +---- earliest → ROW_NUMBER / DISTINCT ON
```

And:

```text id="k9x3p5"
Need summary data?
        |
        v
Aggregate first
        |
        v
JOIN summary
```

And:

```text id="f6w1r9"
Relationship based on range?
        |
        v
Range JOIN
```

---

# 32. Advanced JOIN patterns cheat sheet

| Requirement                       | Common pattern                       |
| --------------------------------- | ------------------------------------ |
| All matching rows                 | `JOIN`                               |
| All parent rows, optional child   | `LEFT JOIN`                          |
| At least one child exists         | `EXISTS`                             |
| No child exists                   | `NOT EXISTS`                         |
| Latest child                      | `ROW_NUMBER()` / `DISTINCT ON`       |
| Highest child                     | `ROW_NUMBER()` / `DISTINCT ON`       |
| Aggregate child data              | `GROUP BY` then `JOIN`               |
| Multiple independent aggregates   | Aggregate separately, then JOIN      |
| Range relationship                | Range JOIN                           |
| Historical/effective record       | Temporal/range JOIN                  |
| Parent/manager relationship       | Self JOIN                            |
| Remove unwanted multiplicity      | Rank/filter or aggregate before JOIN |
| PostgreSQL-specific top-per-group | `DISTINCT ON`                        |

---

# 33. A critical principle: reduce the data before joining when appropriate

One of the strongest patterns we've learned is:

```text id="1h7xq9"
Huge child table
       |
       v
Filter / aggregate / rank
       |
       v
Smaller, correct-grain result
       |
       v
JOIN parent
```

Instead of:

```text id="c8r4z2"
Parent
   +
Huge child
   +
Another huge child
   |
   v
Potential row explosion
```

This can improve both:

* correctness
* performance

But don't blindly push every operation into a subquery or CTE. The right structure depends on the query.

---

# 34. Advanced JOINs are mostly about grain

Notice how many of these patterns come back to the same question:

```text id="p4v9c1"
What is the grain?
```

### Latest order

```text
one row per customer
```

### Order summary

```text
one row per customer
```

### Ticket summary

```text
one row per customer
```

### Customer + order details

```text
one row per order
```

### Customer + order item details

```text
one row per order item
```

Once you know the grain, you can reason about whether a JOIN will multiply rows.

---

# 35. The production SQL mindset

Instead of starting with:

> "Which JOIN syntax should I use?"

start with:

```text id="8g4y2z"
1. What does one output row represent?
          ↓
2. Which table naturally has that grain?
          ↓
3. What relationships do I need?
          ↓
4. Which relationships are 1:1, 1:N, or N:N?
          ↓
5. Will any JOIN multiply rows?
          ↓
6. Do I need all related rows or only existence?
          ↓
7. Do I need the latest/highest/specific related row?
          ↓
8. Should I aggregate/rank/filter before joining?
          ↓
9. What should happen when no related row exists?
          ↓
10. Then write the JOIN
```

This is much more reliable than memorizing SQL patterns.

---

# 36. Final mental model

```text id="s4z8n2"
                    REAL QUERY
                        |
                        v
                  Define the grain
                        |
                        v
                 Understand cardinality
                        |
             +----------+----------+
             |          |          |
             v          v          v
          JOIN       EXISTS     NOT EXISTS
             |
             v
       Need special row?
             |
       +-----+------+
       |            |
      YES           NO
       |
       +---- latest/highest
       |       ↓
       |   ROW_NUMBER
       |   DISTINCT ON
       |
       +---- summary
       |       ↓
       |   GROUP BY
       |       ↓
       |      JOIN
       |
       +---- range
               ↓
           Range JOIN
```

> **Advanced JOINs are usually not about learning a new JOIN keyword. They're about controlling cardinality, grain, filtering, ranking, and aggregation before the JOIN creates unwanted combinations.**

---

## Next

**28. Real-World Query Design**

We'll finish the course by putting everything together into realistic PostgreSQL query-design scenarios: **starting from a business requirement, identifying the grain and relationships, choosing JOIN/EXISTS/aggregation, preventing row explosion, and validating the final query with `EXPLAIN ANALYZE`.**

