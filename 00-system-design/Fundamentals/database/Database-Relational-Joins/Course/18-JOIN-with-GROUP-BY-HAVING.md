# 18. JOIN with `GROUP BY` / `HAVING`

Now we will go deeper into how **`GROUP BY` and `HAVING` work together with JOINs**.

This is especially important for reporting queries such as:

* Customers with more than 5 orders
* Customers who spent more than ₹10,000
* Products sold more than 1,000 times
* Departments with more than 10 employees
* Categories whose average price exceeds a threshold

The key distinction is:

```text
WHERE
→ filters individual rows

GROUP BY
→ creates groups

HAVING
→ filters groups
```

---

# 1. Start With a Simple JOIN

Consider:

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
| 103 |           1 |    300 |
| 104 |           2 |    500 |

We want:

> Total amount spent by each customer.

First, the JOIN produces:

| customer | order | amount |
| -------- | ----: | -----: |
| Alice    |   101 |    100 |
| Alice    |   102 |    200 |
| Alice    |   103 |    300 |
| Bob      |   104 |    500 |

Now we need to turn this into:

| customer | total |
| -------- | ----: |
| Alice    |   600 |
| Bob      |   500 |

That's what `GROUP BY` does.

---

# 2. Basic `GROUP BY` With JOIN

```sql
SELECT
    c.id,
    c.name,
    SUM(o.amount) AS total_spent
FROM customers AS c
JOIN orders AS o
    ON c.id = o.customer_id
GROUP BY
    c.id,
    c.name;
```

Result:

| id | name  | total_spent |
| -: | ----- | ----------: |
|  1 | Alice |         600 |
|  2 | Bob   |         500 |

---

# 3. What Does GROUP BY Actually Do?

Think of the joined rows:

```text
Alice  100
Alice  200
Alice  300
Bob    500
```

Then:

```text
GROUP BY customer
```

creates:

```text
Alice
├── 100
├── 200
└── 300

Bob
└── 500
```

Then:

```text
SUM()
```

operates independently on each group:

```text
Alice → 100 + 200 + 300 = 600

Bob → 500 = 500
```

Visual:

```text
Joined rows
     │
     ▼
┌───────────────┐
│ Alice  100    │
│ Alice  200    │
│ Alice  300    │
│ Bob    500    │
└───────────────┘
     │
     │ GROUP BY customer
     ▼
┌───────────────┐
│ Alice         │
│  100          │
│  200          │
│  300          │
├───────────────┤
│ Bob           │
│  500          │
└───────────────┘
     │
     │ SUM()
     ▼
Alice → 600
Bob   → 500
```

---

# 4. Why Do We Need GROUP BY?

Suppose we write:

```sql
SELECT
    c.name,
    SUM(o.amount)
FROM customers AS c
JOIN orders AS o
    ON c.id = o.customer_id;
```

This is problematic because we're asking for:

```text
c.name
```

and also:

```text
SUM(o.amount)
```

But SQL needs to know:

> Which customer's rows should be summarized together?

We need:

```sql
GROUP BY c.name
```

or, more safely when using customer identity:

```sql
GROUP BY c.id, c.name
```

---

# 5. The Core Rule of GROUP BY

When using aggregation, every selected expression generally needs to be one of:

1. An aggregate expression
2. A grouped expression

For example:

```sql
SELECT
    c.name,
    COUNT(o.id)
FROM customers c
JOIN orders o
    ON c.id = o.customer_id
GROUP BY c.name;
```

`c.name` is grouped.

`COUNT(o.id)` is aggregated.

Good.

---

# 6. Why Grouping by the Primary Key Is Often Better

Suppose:

```text
customers
---------
id
name
```

and `id` is the primary key.

You might write:

```sql
GROUP BY c.id, c.name
```

This is very common.

Why not just:

```sql
GROUP BY c.id
```

and select:

```sql
c.name
```

PostgreSQL has rules around functional dependencies that can allow certain cases where a non-grouped column is functionally determined by a grouped primary key.

However, for learning and portability, this is a good pattern:

```sql
GROUP BY
    c.id,
    c.name
```

It makes the grouping intention explicit.

---

# 7. GROUP BY Defines the Result Grain

This connects directly to our earlier discussion of **grain**.

If you write:

```sql
GROUP BY c.id, c.name
```

your result grain is:

```text
one row = one customer
```

If instead you write:

```sql
GROUP BY c.id, o.status
```

your grain becomes:

```text
one row = one customer + one order status
```

For example:

| customer | status  | count |
| -------- | ------- | ----: |
| Alice    | SUCCESS |     4 |
| Alice    | FAILED  |     1 |
| Bob      | SUCCESS |     2 |

So:

> **`GROUP BY` defines what a result row represents.**

This is one of the most important SQL concepts.

---

# 8. GROUP BY Multiple Columns

Suppose we want:

> Order count per customer per status.

```sql
SELECT
    c.id,
    c.name,
    o.status,
    COUNT(o.id) AS order_count
FROM customers AS c
JOIN orders AS o
    ON c.id = o.customer_id
GROUP BY
    c.id,
    c.name,
    o.status;
```

Now the grouping key is:

```text
(customer, status)
```

Not simply:

```text
customer
```

Visual:

```text
Alice
├── SUCCESS
│    ├── order
│    ├── order
│    └── order
│
└── FAILED
     └── order
```

---

# 9. WHERE vs GROUP BY vs HAVING

This is the central topic.

Suppose the requirement is:

> Find customers who have at least 3 successful orders.

We have three logical stages:

```text
orders
   │
   ▼
filter successful orders
   │
   ▼
group by customer
   │
   ▼
count orders
   │
   ▼
keep groups with count >= 3
```

SQL:

```sql
SELECT
    c.id,
    c.name,
    COUNT(o.id) AS successful_orders
FROM customers AS c
JOIN orders AS o
    ON c.id = o.customer_id
WHERE o.status = 'SUCCESS'
GROUP BY
    c.id,
    c.name
HAVING COUNT(o.id) >= 3;
```

---

# 10. What WHERE Does

```sql
WHERE o.status = 'SUCCESS'
```

filters **individual joined rows**.

Suppose the joined data is:

| customer | status  |
| -------- | ------- |
| Alice    | SUCCESS |
| Alice    | SUCCESS |
| Alice    | FAILED  |
| Alice    | SUCCESS |
| Bob      | SUCCESS |
| Bob      | FAILED  |

After `WHERE`:

| customer | status  |
| -------- | ------- |
| Alice    | SUCCESS |
| Alice    | SUCCESS |
| Alice    | SUCCESS |
| Bob      | SUCCESS |

The failed orders are removed before grouping.

---

# 11. What GROUP BY Does

Now:

```sql
GROUP BY c.id, c.name
```

creates:

```text
Alice
├── SUCCESS
├── SUCCESS
└── SUCCESS

Bob
└── SUCCESS
```

Then:

```sql
COUNT(o.id)
```

produces:

```text
Alice → 3
Bob   → 1
```

---

# 12. What HAVING Does

Now:

```sql
HAVING COUNT(o.id) >= 3
```

filters the **groups**.

Before HAVING:

| customer | count |
| -------- | ----: |
| Alice    |     3 |
| Bob      |     1 |

After HAVING:

| customer | count |
| -------- | ----: |
| Alice    |     3 |

So:

```text
WHERE
→ filters rows

HAVING
→ filters groups
```

---

# 13. The Most Important Visual

```text
FROM
  │
  ▼
JOIN
  │
  ▼
Joined rows
  │
  ▼
WHERE
  │
  │ remove individual rows
  ▼
Filtered rows
  │
  ▼
GROUP BY
  │
  │ create groups
  ▼
Groups
  │
  ▼
Aggregate
  │
  │ COUNT / SUM / AVG / ...
  ▼
Aggregated groups
  │
  ▼
HAVING
  │
  │ remove groups
  ▼
Final groups
```

This is the mental model you should use.

---

# 14. WHERE Cannot Normally Use Aggregate Results

This is wrong:

```sql
SELECT
    c.id,
    c.name,
    COUNT(o.id)
FROM customers c
JOIN orders o
    ON c.id = o.customer_id
WHERE COUNT(o.id) >= 3
GROUP BY c.id, c.name;
```

Why?

Because `WHERE` operates before the groups have been formed.

At that point:

```text
COUNT(o.id)
```

doesn't represent a completed group yet.

Use:

```sql
HAVING COUNT(o.id) >= 3
```

instead.

---

# 15. HAVING Without WHERE

You can have:

```sql
SELECT
    c.id,
    c.name,
    COUNT(o.id) AS order_count
FROM customers AS c
JOIN orders AS o
    ON c.id = o.customer_id
GROUP BY
    c.id,
    c.name
HAVING COUNT(o.id) >= 3;
```

No `WHERE` is required.

This means:

```text
all joined orders
    ↓
group by customer
    ↓
count
    ↓
keep groups with count >= 3
```

---

# 16. WHERE and HAVING Together

This is very common.

Requirement:

> Find customers with at least 3 successful orders from the last 30 days.

```sql
SELECT
    c.id,
    c.name,
    COUNT(o.id) AS successful_orders
FROM customers AS c
JOIN orders AS o
    ON c.id = o.customer_id
WHERE o.status = 'SUCCESS'
  AND o.created_at >= CURRENT_DATE - INTERVAL '30 days'
GROUP BY
    c.id,
    c.name
HAVING COUNT(o.id) >= 3;
```

Think:

```text
WHERE
↓
Which orders qualify?

GROUP BY
↓
Which customer does each order belong to?

COUNT
↓
How many qualifying orders?

HAVING
↓
Which customers have at least 3?
```

---

# 17. JOIN Condition vs WHERE vs HAVING

Now we have three different places where conditions can go.

Consider:

```text
ON
↓
Which rows match across tables?


WHERE
↓
Which joined rows survive?


GROUP BY
↓
How are surviving rows grouped?


HAVING
↓
Which groups survive?
```

Example:

```sql
SELECT
    c.id,
    c.name,
    COUNT(o.id) AS order_count
FROM customers AS c
JOIN orders AS o
    ON c.id = o.customer_id
WHERE o.status = 'SUCCESS'
GROUP BY c.id, c.name
HAVING COUNT(o.id) >= 3;
```

Each clause has a distinct job.

---

# 18. A Real Example

Suppose we have:

### Orders

|  id | customer_id | status  | amount |
| --: | ----------: | ------- | -----: |
| 101 |           1 | SUCCESS |    100 |
| 102 |           1 | SUCCESS |    200 |
| 103 |           1 | FAILED  |    300 |
| 104 |           1 | SUCCESS |    400 |
| 105 |           2 | SUCCESS |     50 |
| 106 |           2 | SUCCESS |     60 |

Requirement:

> Customers with at least 3 successful orders.

Query:

```sql
SELECT
    c.id,
    c.name,
    COUNT(o.id) AS successful_orders
FROM customers AS c
JOIN orders AS o
    ON c.id = o.customer_id
WHERE o.status = 'SUCCESS'
GROUP BY
    c.id,
    c.name
HAVING COUNT(o.id) >= 3;
```

Process:

```text
Alice:
SUCCESS
SUCCESS
FAILED ← removed by WHERE
SUCCESS

→ 3 successful orders
→ passes HAVING


Bob:
SUCCESS
SUCCESS

→ 2 successful orders
→ fails HAVING
```

Result:

| customer | successful_orders |
| -------- | ----------------: |
| Alice    |                 3 |

---

# 19. HAVING With SUM

Requirement:

> Customers whose total order value exceeds ₹10,000.

```sql
SELECT
    c.id,
    c.name,
    SUM(o.amount) AS total_spent
FROM customers AS c
JOIN orders AS o
    ON c.id = o.customer_id
GROUP BY
    c.id,
    c.name
HAVING SUM(o.amount) > 10000;
```

Here:

```text
SUM(o.amount)
```

is calculated per customer.

Then:

```text
HAVING SUM(o.amount) > 10000
```

filters the customer groups.

---

# 20. HAVING With AVG

Requirement:

> Customers whose average order value exceeds ₹2,000.

```sql
SELECT
    c.id,
    c.name,
    AVG(o.amount) AS average_order
FROM customers AS c
JOIN orders AS o
    ON c.id = o.customer_id
GROUP BY
    c.id,
    c.name
HAVING AVG(o.amount) > 2000;
```

Again:

```text
GROUP BY customer
        ↓
AVG per customer
        ↓
HAVING
        ↓
keep qualifying customers
```

---

# 21. HAVING With Multiple Conditions

You can combine conditions:

```sql
HAVING COUNT(o.id) >= 3
   AND SUM(o.amount) > 10000
```

Example:

```sql
SELECT
    c.id,
    c.name,
    COUNT(o.id) AS order_count,
    SUM(o.amount) AS total_spent
FROM customers AS c
JOIN orders AS o
    ON c.id = o.customer_id
GROUP BY
    c.id,
    c.name
HAVING COUNT(o.id) >= 3
   AND SUM(o.amount) > 10000;
```

The customer must satisfy **both** group-level conditions.

---

# 22. HAVING With OR

You can also write:

```sql
HAVING COUNT(o.id) >= 10
    OR SUM(o.amount) > 50000;
```

Meaning:

```text
10+ orders
       OR
₹50,000+ total
```

The condition is evaluated against each group.

---

# 23. LEFT JOIN + GROUP BY + HAVING

Now let's bring back customers with zero orders.

Suppose:

> Show customers with at least 1 order.

```sql
SELECT
    c.id,
    c.name,
    COUNT(o.id) AS order_count
FROM customers AS c
LEFT JOIN orders AS o
    ON c.id = o.customer_id
GROUP BY
    c.id,
    c.name
HAVING COUNT(o.id) >= 1;
```

Because:

```text
Charlie → COUNT(o.id) = 0
```

Charlie is removed by:

```sql
HAVING COUNT(o.id) >= 1
```

This is perfectly valid.

---

# 24. Why Not Just Use INNER JOIN?

You could use:

```sql
FROM customers c
JOIN orders o
    ON c.id = o.customer_id
```

and then group.

That naturally removes customers with no orders.

So:

```text
LEFT JOIN + HAVING COUNT(...) >= 1
```

can sometimes produce the same customer set as:

```text
INNER JOIN
```

But `LEFT JOIN` becomes useful when your aggregation or other logic needs to retain zero-count groups.

For example:

```sql
HAVING COUNT(o.id) >= 0
```

would preserve everyone.

---

# 25. A Subtle Difference With HAVING

Suppose we want:

> Show every customer, including those with zero orders.

Use:

```sql
SELECT
    c.id,
    c.name,
    COUNT(o.id) AS order_count
FROM customers AS c
LEFT JOIN orders AS o
    ON c.id = o.customer_id
GROUP BY
    c.id,
    c.name;
```

Don't add:

```sql
HAVING COUNT(o.id) > 0
```

because that explicitly removes zero-order groups.

---

# 26. GROUP BY and NULL

Suppose:

```text
orders.status

SUCCESS
SUCCESS
NULL
FAILED
```

If we group by status:

```sql
GROUP BY o.status
```

the `NULL` values belong to their own group.

Conceptually:

```text
SUCCESS → 2
FAILED  → 1
NULL    → 1
```

Remember:

```text
NULL ≠ NULL
```

for normal comparison, but grouping treats rows with NULL grouping values as belonging to the same group.

This distinction is important.

---

# 27. JOIN + GROUP BY Can Change the Meaning of a Query

Compare:

```sql
GROUP BY c.id, c.name
```

with:

```sql
GROUP BY c.id, c.name, o.status
```

First:

```text
one row = one customer
```

Second:

```text
one row = one customer + one status
```

For example:

```text
Customer
   │
   ├── SUCCESS → 5
   │
   └── FAILED  → 2
```

Therefore:

> Adding a column to `GROUP BY` changes the grain.

Always ask:

```text
"What should one output row represent?"
```

---

# 28. The Most Common GROUP BY Mistake

Suppose the requirement is:

> Total spending per customer.

Someone writes:

```sql
GROUP BY
    c.id,
    c.name,
    o.id;
```

Now you're grouping by the order itself.

Result:

```text
Alice + Order 101 → 100
Alice + Order 102 → 200
Alice + Order 103 → 300
```

Instead of:

```text
Alice → 600
```

Why?

Because:

```text
GROUP BY customer + order
```

has a much finer grain than:

```text
GROUP BY customer
```

---

# 29. Another Common Mistake: GROUP BY Too Little

Suppose you want:

```text
customer + status
```

but write:

```sql
GROUP BY c.id, c.name;
```

Then you cannot meaningfully return:

```sql
o.status
```

as an ordinary non-aggregate column.

You need:

```sql
GROUP BY
    c.id,
    c.name,
    o.status;
```

Again:

```text
Selected non-aggregate column
        ↓
usually needs to participate in grouping
```

---

# 30. JOIN + GROUP BY + HAVING Pattern

Memorize this general shape:

```sql
SELECT
    grouping_columns,
    aggregate_function(...)
FROM table_a
JOIN table_b
    ON relationship
WHERE row_level_conditions
GROUP BY
    grouping_columns
HAVING aggregate_condition;
```

Example:

```sql
SELECT
    c.id,
    c.name,
    COUNT(o.id) AS order_count,
    SUM(o.amount) AS total_spent
FROM customers AS c
JOIN orders AS o
    ON c.id = o.customer_id
WHERE o.status = 'SUCCESS'
GROUP BY
    c.id,
    c.name
HAVING COUNT(o.id) >= 3
   AND SUM(o.amount) > 10000;
```

This pattern appears constantly in production SQL.

---

# 31. A Four-Level Mental Model

When designing a query, think:

```text
1. JOIN
   "Which rows are related?"

2. WHERE
   "Which individual rows do I want?"

3. GROUP BY
   "How should those rows be grouped?"

4. HAVING
   "Which groups do I want?"
```

For example:

```text
Customers + Orders
       ↓
JOIN
       ↓
only SUCCESS orders
       ↓
WHERE
       ↓
group by customer
       ↓
GROUP BY
       ↓
count orders
       ↓
HAVING count >= 3
```

---

# 32. WHERE vs HAVING Cheat Sheet

| Question                                   | Use        |
| ------------------------------------------ | ---------- |
| Is this individual order successful?       | `WHERE`    |
| Is this order over ₹1,000?                 | `WHERE`    |
| Does this customer have at least 5 orders? | `HAVING`   |
| Did this customer spend more than ₹10,000? | `HAVING`   |
| Is average order value > ₹2,000?           | `HAVING`   |
| Should this order match the customer?      | `ON`       |
| What defines one group?                    | `GROUP BY` |

---

# 33. Performance Consideration

Suppose you have:

```text
10 million orders
```

and only:

```text
100,000 SUCCESS orders
```

If you have a condition:

```sql
WHERE o.status = 'SUCCESS'
```

the database can potentially reduce the rows before grouping.

Conceptually:

```text
10,000,000 orders
        ↓
WHERE SUCCESS
        ↓
100,000 rows
        ↓
GROUP BY
```

rather than grouping all 10 million rows.

This is one reason to distinguish row-level filtering from group-level filtering.

Of course, PostgreSQL's actual execution depends on its optimizer, indexes, statistics, and execution plan.

---

# 34. Conditional Aggregation vs HAVING

These are different concepts.

### Conditional aggregation

```sql
COUNT(*) FILTER (
    WHERE o.status = 'SUCCESS'
)
```

asks:

> How many successful orders are in this group?

### HAVING

```sql
HAVING COUNT(*) >= 3
```

asks:

> Should this group remain in the result?

You can combine them:

```sql
SELECT
    c.id,
    c.name,

    COUNT(o.id) AS total_orders,

    COUNT(o.id) FILTER (
        WHERE o.status = 'SUCCESS'
    ) AS successful_orders

FROM customers AS c
LEFT JOIN orders AS o
    ON c.id = o.customer_id

GROUP BY
    c.id,
    c.name

HAVING COUNT(o.id) >= 3;
```

---

# 35. A Production-Style Example

Requirement:

> Find customers who placed at least 5 successful orders during the last 90 days and spent more than ₹25,000 on those orders.

```sql
SELECT
    c.id,
    c.name,
    COUNT(o.id) AS order_count,
    SUM(o.amount) AS total_spent
FROM customers AS c
JOIN orders AS o
    ON c.id = o.customer_id
WHERE o.status = 'SUCCESS'
  AND o.created_at >= CURRENT_DATE - INTERVAL '90 days'
GROUP BY
    c.id,
    c.name
HAVING COUNT(o.id) >= 5
   AND SUM(o.amount) > 25000;
```

Read it in English:

```text
Start with customers and their orders
        ↓
Keep successful orders
        ↓
Keep orders from the last 90 days
        ↓
Group them by customer
        ↓
Count and sum each customer's orders
        ↓
Keep customers with:
    at least 5 orders
    AND more than ₹25,000 spent
```

This is exactly how you should mentally construct complex aggregation queries.

---

# 36. The Golden Construction Method

Don't try to write the entire query at once.

Build it incrementally.

### Step 1

```sql
SELECT *
FROM customers c
JOIN orders o
    ON c.id = o.customer_id;
```

Check the relationship.

### Step 2

Add row filtering:

```sql
WHERE o.status = 'SUCCESS'
```

### Step 3

Decide the grain:

```text
one row = one customer
```

### Step 4

Add:

```sql
GROUP BY c.id, c.name
```

### Step 5

Add:

```sql
COUNT(o.id)
SUM(o.amount)
```

### Step 6

Add group filtering:

```sql
HAVING COUNT(o.id) >= 5
```

This incremental approach makes debugging much easier.

---

# 37. Debugging Questions

When a JOIN + GROUP BY query gives unexpected results, ask:

### 1. What is my intended grain?

```text
one row = customer?
one row = customer + status?
one row = order?
```

### 2. Did the JOIN multiply rows?

```text
customer
   ↓
orders
   ↓
payments
```

Could create:

```text
orders × payments
```

### 3. Is my filter a row filter or group filter?

```text
row → WHERE
group → HAVING
```

### 4. Am I using the correct COUNT?

```text
COUNT(*)
COUNT(child.id)
COUNT(DISTINCT child.id)
```

### 5. Did LEFT JOIN preserve zero-count groups?

### 6. Did I accidentally put a right-side condition in WHERE?

These questions catch a large percentage of SQL aggregation bugs.

---

# 38. Complete Mental Model

```text
                TABLE A
                   │
                   │
                JOIN
                   │
                   ▼
                TABLE B
                   │
                   ▼
             joined rows
                   │
                   ▼
                WHERE
             row filtering
                   │
                   ▼
             filtered rows
                   │
                   ▼
               GROUP BY
             create groups
                   │
                   ▼
            COUNT / SUM /
            AVG / MIN / MAX
                   │
                   ▼
            aggregated groups
                   │
                   ▼
                HAVING
             group filtering
                   │
                   ▼
              final result
```

The key is to understand what each stage is operating on.

---

# 39. Golden Rules

1. **`GROUP BY` defines the result grain.**
2. `COUNT`, `SUM`, `AVG`, `MIN`, and `MAX` summarize each group.
3. `WHERE` filters individual rows.
4. `HAVING` filters groups after aggregation.
5. Don't use aggregate conditions in `WHERE`; use `HAVING`.
6. Adding a column to `GROUP BY` can change the meaning and grain of the result.
7. `LEFT JOIN + COUNT(child.id)` is a common pattern for including zero-count parents.
8. `COUNT(*)` and `COUNT(child.id)` behave differently with `LEFT JOIN`.
9. Always check whether earlier JOINs multiplied rows before aggregating.
10. For multiple independent one-to-many relationships, consider aggregating each side before joining.
11. Build complex queries incrementally.
12. Before writing `GROUP BY`, answer:

> **"What should one output row represent?"**

---

# One Sentence to Remember

> **`WHERE` decides which rows participate; `GROUP BY` decides how they are grouped; `HAVING` decides which groups survive.**

---

## Next: 19. Subqueries vs Joins

We'll compare **subqueries and JOINs** using real examples, including:

* scalar subqueries
* correlated subqueries
* `IN`
* `EXISTS`
* derived tables
* when a JOIN is more natural
* when a subquery is more natural
* avoiding unnecessary nested queries
* and how PostgreSQL can optimize these different forms.

