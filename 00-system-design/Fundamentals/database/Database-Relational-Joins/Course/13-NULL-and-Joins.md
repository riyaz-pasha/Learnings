# 13. NULL and Joins

`NULL` is one of the biggest sources of confusion when working with JOINs.

The important thing to understand is:

> **`NULL` does not mean a value is empty, zero, false, or equal to another `NULL`.**

It means:

> **The value is unknown, missing, or not applicable.**

This becomes especially important with `LEFT`, `RIGHT`, and `FULL` JOINs.

---

# 1. What Is `NULL`?

Suppose we have:

```text
customers
-------------------------
id | name    | phone
---+---------+----------
1  | Alice   | 1111
2  | Bob     | NULL
3  | Charlie | 3333
```

Bob's phone is `NULL`.

That does **not** necessarily mean:

```text
phone = ''
```

or:

```text
phone = 0
```

or:

```text
phone = 'NULL'
```

It means:

```text
The database has no known value for Bob's phone.
```

---

# 2. The Most Important Rule: `NULL` Is Not a Value

Consider:

```sql
SELECT NULL = NULL;
```

You might expect:

```text
TRUE
```

But SQL returns:

```text
NULL
```

More precisely, the comparison evaluates to **UNKNOWN**.

Why?

Because SQL treats `NULL` as an unknown value.

Imagine:

```text
unknown = unknown
```

You cannot conclude that they're equal.

---

# 3. SQL Has Three-Valued Logic

Most programming languages think in terms of:

```text
TRUE
FALSE
```

SQL has:

```text
TRUE
FALSE
UNKNOWN
```

For example:

```sql
NULL = 10
```

→ `UNKNOWN`

```sql
NULL = NULL
```

→ `UNKNOWN`

```sql
NULL > 100
```

→ `UNKNOWN`

```sql
NULL <> 10
```

→ `UNKNOWN`

This is extremely important for JOINs.

---

# 4. `WHERE` Keeps Only TRUE

Suppose:

```sql
SELECT *
FROM customers
WHERE phone = '1111';
```

For Bob:

```text
phone = NULL
```

The condition:

```text
NULL = '1111'
```

is:

```text
UNKNOWN
```

And `WHERE` keeps only rows where the condition is:

```text
TRUE
```

Therefore Bob is excluded.

---

# 5. How Do We Check for NULL?

Never write:

```sql
WHERE phone = NULL
```

Instead use:

```sql
WHERE phone IS NULL
```

And:

```sql
WHERE phone IS NOT NULL
```

Example:

```sql
SELECT *
FROM customers
WHERE phone IS NULL;
```

Result:

```text
Bob
```

---

# 6. NULL in JOIN Columns

Consider:

```text
customers
----------------
id | name
1  | Alice
2  | Bob
3  | Charlie
```

and:

```text
orders
----------------------
id  | customer_id
101 | 1
102 | NULL
103 | 3
```

Order `102` doesn't have a customer ID.

Now:

```sql
SELECT
    c.name,
    o.id AS order_id
FROM customers AS c
JOIN orders AS o
    ON c.id = o.customer_id;
```

What happens?

For order 102:

```text
c.id = o.customer_id
```

becomes:

```text
? = NULL
```

which is:

```text
UNKNOWN
```

Therefore it doesn't match.

Result:

| name    | order_id |
| ------- | -------: |
| Alice   |      101 |
| Charlie |      103 |

Order 102 disappears.

---

# 7. INNER JOIN and NULL

Remember:

```text
INNER JOIN
```

keeps only matching rows.

If the JOIN condition evaluates to:

```text
TRUE
```

→ match.

If it evaluates to:

```text
FALSE
UNKNOWN
```

→ no match.

So:

```sql
ON c.id = o.customer_id
```

will not match:

```text
c.id = NULL
```

---

# 8. LEFT JOIN Changes the Story

Now use:

```sql
SELECT
    c.name,
    o.id AS order_id
FROM customers AS c
LEFT JOIN orders AS o
    ON c.id = o.customer_id;
```

Customers:

```text
Alice
Bob
Charlie
```

Orders:

```text
101 → Alice
102 → NULL customer
103 → Charlie
```

Result:

| name    | order_id |
| ------- | -------: |
| Alice   |      101 |
| Bob     |     NULL |
| Charlie |      103 |

Notice:

```text
Bob | NULL
```

Bob had no matching order.

The `NULL` in the result was **created by the LEFT JOIN**.

It doesn't necessarily come from the original `orders` table.

---

# 9. Two Different Sources of NULL

This distinction is very important.

A `NULL` in a JOIN result can come from:

### Case 1 — NULL already existed in the table

```text
orders.customer_id = NULL
```

### Case 2 — JOIN couldn't find a matching row

```text
customer has no order
```

Both can appear as `NULL` in the output, but they mean different things.

---

# 10. Visualizing LEFT JOIN

```text
customers                 orders

Alice ────────────────→ Order 101
Bob   ────────────────→ no match
Charlie ───────────────→ Order 103
```

LEFT JOIN says:

```text
"Keep Alice, Bob, and Charlie no matter what."
```

Therefore:

```text
Bob | NULL
```

is generated for the missing right-side row.

---

# 11. NULL on the Left Side

Suppose:

```text
customers
----------------
id | name
1  | Alice
NULL | Unknown
3  | Charlie
```

and:

```text
orders
----------------
id | customer_id
101 | 1
102 | NULL
103 | 3
```

Then:

```sql
ON c.id = o.customer_id
```

does **not** match:

```text
NULL = NULL
```

Remember:

```text
NULL = NULL
```

→ `UNKNOWN`

So order 102 does not match the customer whose ID is also NULL.

---

# 12. This Surprises Many Developers

You might think:

```text
customers.id = NULL
orders.customer_id = NULL
```

Therefore:

```text
they should match
```

But standard SQL equality doesn't work that way.

```sql
NULL = NULL
```

is not `TRUE`.

It's `UNKNOWN`.

Therefore:

```sql
JOIN ... ON a.value = b.value
```

doesn't match two NULL values.

---

# 13. What If We Actually Want NULLs to Match?

PostgreSQL provides:

```sql
IS NOT DISTINCT FROM
```

This treats two `NULL`s as equal for comparison purposes.

Example:

```sql
SELECT *
FROM table_a AS a
JOIN table_b AS b
    ON a.code IS NOT DISTINCT FROM b.code;
```

Conceptually:

```text
a.code | b.code | match?
-------+--------+--------
A      | A      | YES
A      | B      | NO
NULL   | A      | NO
NULL   | NULL   | YES
```

This is different from:

```sql
a.code = b.code
```

where:

```text
NULL | NULL
```

does not match.

---

# 14. `IS DISTINCT FROM`

PostgreSQL also provides:

```sql
IS DISTINCT FROM
```

It gives a normal TRUE/FALSE result even when NULL is involved.

Example:

```sql
SELECT
    NULL IS DISTINCT FROM NULL;
```

Result:

```text
FALSE
```

Because the two values are considered equivalent for this comparison.

And:

```sql
SELECT
    NULL IS DISTINCT FROM 10;
```

Result:

```text
TRUE
```

This is useful when you need **NULL-safe comparisons**.

---

# 15. LEFT JOIN + WHERE Is the Big Trap

Consider:

```sql
SELECT
    c.name,
    o.amount
FROM customers AS c
LEFT JOIN orders AS o
    ON c.id = o.customer_id
WHERE o.amount > 100;
```

Suppose:

```text
Alice → 200
Bob   → no order
Charlie → 50
```

After the LEFT JOIN:

| name    | amount |
| ------- | -----: |
| Alice   |    200 |
| Bob     |   NULL |
| Charlie |     50 |

Now apply:

```sql
WHERE o.amount > 100
```

For Bob:

```text
NULL > 100
```

→ `UNKNOWN`

`WHERE` removes UNKNOWN.

Final result:

| name  | amount |
| ----- | -----: |
| Alice |    200 |

Bob disappears.

---

# 16. Why This Can Effectively Turn a LEFT JOIN Into an INNER JOIN

This query:

```sql
SELECT
    c.name,
    o.amount
FROM customers AS c
LEFT JOIN orders AS o
    ON c.id = o.customer_id
WHERE o.amount > 100;
```

often behaves like:

```sql
SELECT
    c.name,
    o.amount
FROM customers AS c
INNER JOIN orders AS o
    ON c.id = o.customer_id
WHERE o.amount > 100;
```

because the `WHERE` condition requires a non-NULL `o.amount`.

The important point isn't:

> "LEFT JOIN + WHERE always equals INNER JOIN."

That's too simplistic.

The correct reasoning is:

> **The WHERE condition removes rows whose right-side value is NULL.**

---

# 17. Put the Condition in `ON` When Appropriate

Suppose the requirement is:

> "Show every customer, but only attach orders greater than 100."

Then:

```sql
SELECT
    c.name,
    o.amount
FROM customers AS c
LEFT JOIN orders AS o
    ON c.id = o.customer_id
   AND o.amount > 100;
```

Result:

| name    | amount |
| ------- | -----: |
| Alice   |    200 |
| Bob     |   NULL |
| Charlie |   NULL |

Charlie may have an order, but if his order is only 50, it doesn't qualify as a match.

Yet Charlie remains because the LEFT JOIN preserves customers.

---

# 18. Compare the Two Queries

### Condition in WHERE

```sql
FROM customers AS c
LEFT JOIN orders AS o
    ON c.id = o.customer_id
WHERE o.amount > 100;
```

Meaning:

```text
Join customers with orders
↓
Then keep only final rows where amount > 100
```

Unmatched customers disappear.

---

### Condition in ON

```sql
FROM customers AS c
LEFT JOIN orders AS o
    ON c.id = o.customer_id
   AND o.amount > 100;
```

Meaning:

```text
Keep every customer
↓
Only consider orders > 100 as matching orders
```

Unmatched customers remain.

---

# 19. NULL and `COUNT`

This is another important JOIN interaction.

Suppose:

```text
customers

Alice
Bob
Charlie
```

Orders:

```text
Alice → 2 orders
Bob   → 1 order
Charlie → 0 orders
```

Query:

```sql
SELECT
    c.name,
    COUNT(*) AS count_star,
    COUNT(o.id) AS count_id
FROM customers AS c
LEFT JOIN orders AS o
    ON c.id = o.customer_id
GROUP BY c.name;
```

You might get:

| name    | count_star | count_id |
| ------- | ---------: | -------: |
| Alice   |          2 |        2 |
| Bob     |          1 |        1 |
| Charlie |          1 |        0 |

Why?

---

# 20. Why `COUNT(*)` Is 1 for Charlie

LEFT JOIN produces:

```text
Charlie | NULL
```

There is still one result row.

Therefore:

```sql
COUNT(*)
```

counts that row.

So:

```text
Charlie → 1
```

---

# 21. Why `COUNT(o.id)` Is 0

`o.id` is:

```text
NULL
```

for Charlie.

`COUNT(column)` ignores NULL values.

Therefore:

```text
COUNT(o.id)
```

returns:

```text
0
```

This is why the common pattern is:

```sql
COUNT(child.id)
```

when counting child records after a LEFT JOIN.

---

# 22. `SUM` and NULL

Consider:

```sql
SELECT
    c.name,
    SUM(o.amount)
FROM customers AS c
LEFT JOIN orders AS o
    ON c.id = o.customer_id
GROUP BY c.name;
```

For a customer with no orders:

```text
SUM(NULL)
```

produces:

```text
NULL
```

If your business requirement is:

> "No orders should display as 0."

Use:

```sql
COALESCE(SUM(o.amount), 0)
```

Example:

```sql
SELECT
    c.name,
    COALESCE(SUM(o.amount), 0) AS total_amount
FROM customers AS c
LEFT JOIN orders AS o
    ON c.id = o.customer_id
GROUP BY c.name;
```

Now:

```text
No orders → 0
```

instead of:

```text
No orders → NULL
```

---

# 23. `COALESCE`

`COALESCE` returns the first non-NULL value.

```sql
COALESCE(value, fallback)
```

Example:

```sql
COALESCE(o.amount, 0)
```

means:

```text
If o.amount is not NULL:
    use o.amount

Otherwise:
    use 0
```

Example:

```sql
SELECT
    c.name,
    COALESCE(o.amount, 0) AS amount
FROM customers AS c
LEFT JOIN orders AS o
    ON c.id = o.customer_id;
```

---

# 24. NULL in FULL OUTER JOIN

FULL JOIN makes NULL behavior particularly visible.

Suppose:

### customers

| id | name  |
| -: | ----- |
|  1 | Alice |
|  2 | Bob   |

### orders

|  id | customer_id |
| --: | ----------: |
| 101 |           1 |
| 102 |           3 |

JOIN:

```sql
SELECT
    c.id AS customer_id,
    c.name,
    o.id AS order_id,
    o.customer_id
FROM customers AS c
FULL OUTER JOIN orders AS o
    ON c.id = o.customer_id;
```

Result conceptually:

| customer_id | name  | order_id | order_customer_id |
| ----------: | ----- | -------: | ----------------: |
|           1 | Alice |      101 |                 1 |
|           2 | Bob   |     NULL |              NULL |
|        NULL | NULL  |      102 |                 3 |

There are two different unmatched cases:

```text
Bob
↓
customer exists
order doesn't
```

and:

```text
Order 102
↓
order exists
customer doesn't
```

FULL JOIN uses NULLs to represent the missing side.

---

# 25. Finding Unmatched Rows

This leads to a powerful pattern.

### Customers without orders

```sql
SELECT
    c.id,
    c.name
FROM customers AS c
LEFT JOIN orders AS o
    ON c.id = o.customer_id
WHERE o.id IS NULL;
```

The important part is:

```sql
WHERE o.id IS NULL
```

This means:

> "Keep customers for whom no matching order row exists."

---

# 26. Why `WHERE o.id IS NULL` Works

Suppose:

```text
Alice → Order 101
Bob   → NULL
```

After LEFT JOIN:

```text
Alice | 101
Bob   | NULL
```

Then:

```sql
WHERE o.id IS NULL
```

keeps:

```text
Bob
```

This is called an **anti-join pattern**.

We'll study anti joins in detail later.

---

# 27. Don't Write `= NULL`

This is wrong:

```sql
WHERE o.id = NULL
```

Because:

```text
o.id = NULL
```

evaluates to:

```text
UNKNOWN
```

Therefore it doesn't find NULLs.

Use:

```sql
WHERE o.id IS NULL
```

Similarly:

```sql
WHERE o.id IS NOT NULL
```

---

# 28. NULL and JOIN Conditions

Consider:

```sql
FROM A
JOIN B
    ON A.code = B.code
```

There are four important cases:

| A.code | B.code | `A.code = B.code` |
| ------ | ------ | ----------------- |
| `A`    | `A`    | TRUE              |
| `A`    | `B`    | FALSE             |
| `NULL` | `A`    | UNKNOWN           |
| `NULL` | `NULL` | UNKNOWN           |

Therefore normal equality JOINs don't match NULLs.

If you specifically need NULL-safe equality in PostgreSQL:

```sql
ON A.code IS NOT DISTINCT FROM B.code
```

Then:

| A.code | B.code | Match? |
| ------ | ------ | ------ |
| `A`    | `A`    | Yes    |
| `A`    | `B`    | No     |
| `NULL` | `A`    | No     |
| `NULL` | `NULL` | Yes    |

---

# 29. NULL vs Empty String vs Zero

Don't confuse these:

```text
NULL
''
0
FALSE
```

They represent different things.

For example:

```text
phone = NULL
```

means:

```text
phone is unknown/missing
```

while:

```text
phone = ''
```

means:

```text
phone contains an empty string
```

And:

```text
amount = 0
```

means:

```text
amount is explicitly zero
```

They behave differently in JOINs and conditions.

---

# 30. A Useful Visual Model

Think of a LEFT JOIN as creating a placeholder when no match exists:

```text
Customers              Orders

Alice ──────────────── Order 101
Bob   ──────────────── [NO MATCH]
Charlie ────────────── Order 102
                           ↓
                         NULL
```

The result becomes:

```text
Alice   | 101
Bob     | NULL
Charlie | 102
```

That `NULL` represents:

> **There was no matching row on the right side.**

---

# 31. NULL Can Propagate Through Expressions

Suppose:

```sql
SELECT
    c.name,
    o.amount * 2 AS double_amount
FROM customers AS c
LEFT JOIN orders AS o
    ON c.id = o.customer_id;
```

For a customer without an order:

```text
o.amount = NULL
```

Therefore:

```text
NULL * 2
```

is:

```text
NULL
```

Similarly:

```text
NULL + 10     → NULL
NULL * 10     → NULL
NULL || 'abc' → NULL
```

depending on the expression/operator semantics.

If you need a fallback:

```sql
COALESCE(o.amount, 0) * 2
```

---

# 32. A Very Important Debugging Question

Whenever you see unexpected NULLs after a JOIN, ask:

```text
1. Was the original column already NULL?
```

or:

```text
2. Did the JOIN fail to find a matching row?
```

For example:

```text
LEFT JOIN result:

Alice | 100
Bob   | NULL
```

Bob's NULL might mean:

```text
No order exists
```

But:

```text
Alice | NULL
```

might mean:

```text
An order exists, but its amount itself is NULL
```

These are different situations.

---

# 33. Summary Table

| Situation                          | Result                                            |
| ---------------------------------- | ------------------------------------------------- |
| `NULL = NULL`                      | UNKNOWN                                           |
| `NULL = 10`                        | UNKNOWN                                           |
| `NULL > 10`                        | UNKNOWN                                           |
| `WHERE condition`                  | Keeps only TRUE                                   |
| Check NULL                         | `IS NULL`                                         |
| Check non-NULL                     | `IS NOT NULL`                                     |
| NULL-safe equality in PostgreSQL   | `IS NOT DISTINCT FROM`                            |
| LEFT JOIN unmatched right side     | Right columns become NULL                         |
| RIGHT JOIN unmatched left side     | Left columns become NULL                          |
| FULL JOIN unmatched side           | Missing side becomes NULL                         |
| `COUNT(*)`                         | Counts result rows, including NULL-generated rows |
| `COUNT(column)`                    | Ignores NULL                                      |
| `SUM(...)` with no matching values | Often NULL                                        |
| Replace NULL with fallback         | `COALESCE(...)`                                   |

---

# 34. The Three Rules to Remember

If you remember only three things from this lesson:

### Rule 1

```sql
NULL = NULL
```

is **not TRUE**.

Use:

```sql
IS NULL
```

or, for NULL-safe comparison:

```sql
IS NOT DISTINCT FROM
```

---

### Rule 2

A `LEFT JOIN` can create NULLs:

```text
left row exists
+
right row doesn't exist
=
right columns become NULL
```

---

### Rule 3

Be extremely careful with:

```sql
LEFT JOIN ...
WHERE right_table.column ...
```

because the `WHERE` condition may remove the NULL-extended rows you intended to preserve.

---

# Next

**14. Anti Joins**

We'll learn how to answer questions like:

* "Find customers who have **no orders**."
* "Find products that were **never purchased**."
* "Find employees who **don't have a manager**."
* "Find records in A that **do not exist in B**."

We'll compare the classic `LEFT JOIN ... IS NULL` pattern with `NOT EXISTS` and understand when each approach makes sense.

