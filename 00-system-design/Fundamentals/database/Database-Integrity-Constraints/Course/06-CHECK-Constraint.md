# 6. `CHECK` Constraint in PostgreSQL

A `CHECK` constraint allows you to tell PostgreSQL:

> **"This condition must be true for every row."**

It is one of the most flexible basic integrity constraints.

For example:

```sql
CREATE TABLE products (
    id BIGINT PRIMARY KEY,
    name TEXT NOT NULL,
    price NUMERIC(10, 2) CHECK (price >= 0)
);
```

Now PostgreSQL won't allow a product with a negative price.

---

# 1. The problem

Consider:

```sql
CREATE TABLE products (
    id BIGINT PRIMARY KEY,
    name TEXT NOT NULL,
    price NUMERIC(10, 2)
);
```

Nothing prevents:

```sql
INSERT INTO products (id, name, price)
VALUES (1, 'Laptop', -50000);
```

But a negative product price probably violates the application's business rules.

We can encode that rule in the database:

```sql
price NUMERIC(10, 2) CHECK (price >= 0)
```

Now:

```sql
INSERT INTO products (id, name, price)
VALUES (1, 'Laptop', -50000);
```

is rejected.

---

# 2. Basic syntax

The general form is:

```sql
column_name data_type CHECK (condition)
```

For example:

```sql
CREATE TABLE employees (
    id BIGINT PRIMARY KEY,
    age INT CHECK (age >= 18)
);
```

The condition:

```sql
age >= 18
```

must hold for every inserted or updated row.

---

# 3. `CHECK` is about conditions

Some common examples:

```sql
CHECK (age >= 18)
```

```sql
CHECK (price >= 0)
```

```sql
CHECK (quantity > 0)
```

```sql
CHECK (discount >= 0 AND discount <= 100)
```

```sql
CHECK (salary > 0)
```

```sql
CHECK (start_date <= end_date)
```

```sql
CHECK (status IN ('PENDING', 'PAID', 'CANCELLED'))
```

So the basic idea is:

```text
                    CHECK
                      |
                      v
              Is condition valid?
                 /          \
               YES           NO
                |             |
              allow          ERROR
```

---

# 4. `CHECK` is evaluated during `INSERT`

Consider:

```sql
CREATE TABLE products (
    id BIGINT PRIMARY KEY,
    price NUMERIC(10, 2)
        CHECK (price >= 0)
);
```

Valid:

```sql
INSERT INTO products
VALUES (1, 100);
```

Invalid:

```sql
INSERT INTO products
VALUES (2, -100);
```

PostgreSQL rejects the second row.

---

# 5. `CHECK` is also evaluated during `UPDATE`

Suppose:

```text
id    price
---   -----
1     100
2     200
```

Then:

```sql
UPDATE products
SET price = -50
WHERE id = 1;
```

is rejected.

The constraint isn't just protecting the initial data.

It protects the invariant throughout the row's lifetime:

```text
INSERT ──┐
         │
UPDATE ──┼──> CHECK constraint
         │
         └──> condition must remain valid
```

---

# 6. `CHECK` with multiple conditions

You can combine conditions with:

* `AND`
* `OR`
* `NOT`
* comparison operators
* `IN`
* `BETWEEN`
* etc.

For example:

```sql
CREATE TABLE employees (
    id BIGINT PRIMARY KEY,
    age INT CHECK (age >= 18 AND age <= 65)
);
```

Now:

```text
18 <= age <= 65
```

must hold.

Valid:

```text
18
25
40
65
```

Invalid:

```text
17
66
100
```

---

# 7. `IN`

`IN` is useful for restricting a value to a predefined set.

For example:

```sql
CREATE TABLE orders (
    id BIGINT PRIMARY KEY,
    status TEXT CHECK (
        status IN ('PENDING', 'PAID', 'CANCELLED')
    )
);
```

Allowed:

```text
PENDING
PAID
CANCELLED
```

Not allowed:

```text
SHIPPED
UNKNOWN
COMPLETED
```

This is useful for small, stable sets of values.

---

# 8. `BETWEEN`

You can also write:

```sql
CREATE TABLE products (
    id BIGINT PRIMARY KEY,
    discount NUMERIC(5, 2)
        CHECK (discount BETWEEN 0 AND 100)
);
```

So:

```text
0 <= discount <= 100
```

is required.

For example:

```text
0       → valid
10      → valid
50      → valid
100     → valid
-1      → invalid
101     → invalid
```

---

# 9. Multiple `CHECK` constraints

You don't have to put everything into one huge expression.

For example:

```sql
CREATE TABLE employees (
    id BIGINT PRIMARY KEY,

    age INT,
    salary NUMERIC(12, 2),

    CHECK (age >= 18),
    CHECK (salary >= 0)
);
```

This is perfectly valid.

You could also write:

```sql
CREATE TABLE employees (
    id BIGINT PRIMARY KEY,
    age INT,
    salary NUMERIC(12, 2),

    CHECK (age >= 18 AND salary >= 0)
);
```

Both are possible.

Separate constraints can be easier to understand and name individually.

---

# 10. Naming a `CHECK` constraint

You can give it a meaningful name:

```sql
CREATE TABLE products (
    id BIGINT PRIMARY KEY,
    price NUMERIC(10, 2),

    CONSTRAINT chk_products_price_non_negative
        CHECK (price >= 0)
);
```

Now PostgreSQL identifies the constraint as:

```text
chk_products_price_non_negative
```

This is useful when debugging or managing migrations.

For example:

```sql
ALTER TABLE products
DROP CONSTRAINT chk_products_price_non_negative;
```

---

# 11. Column-level vs table-level `CHECK`

You can write:

```sql
CREATE TABLE products (
    id BIGINT PRIMARY KEY,
    price NUMERIC(10, 2)
        CHECK (price >= 0)
);
```

This is a column-level check.

But you can also write:

```sql
CREATE TABLE products (
    id BIGINT PRIMARY KEY,
    price NUMERIC(10, 2),

    CHECK (price >= 0)
);
```

This is a table-level check.

The distinction becomes important when the condition involves **multiple columns**.

---

# 12. Multi-column `CHECK`

Suppose we have an event:

```sql
CREATE TABLE events (
    id BIGINT PRIMARY KEY,
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP NOT NULL,

    CHECK (start_time < end_time)
);
```

Now PostgreSQL ensures:

```text
start_time < end_time
```

For example:

```text
start                 end
-------------------   -------------------
10:00                 11:00       valid
10:00                 10:30       valid
11:00                 10:00       invalid
```

This is a great example of why table-level checks are useful.

The rule involves **two columns**.

---

# 13. Another multi-column example

Suppose an employee has a minimum and maximum salary range:

```sql
CREATE TABLE salary_ranges (
    id BIGINT PRIMARY KEY,
    minimum_salary NUMERIC(12, 2),
    maximum_salary NUMERIC(12, 2),

    CHECK (minimum_salary <= maximum_salary)
);
```

This prevents:

```text
minimum_salary = 100000
maximum_salary = 50000
```

because:

```text
100000 <= 50000
```

is false.

---

# 14. The important `NULL` behavior

This is where `CHECK` becomes subtle.

Consider:

```sql
CREATE TABLE employees (
    id BIGINT PRIMARY KEY,
    age INT CHECK (age >= 18)
);
```

You might expect:

```sql
INSERT INTO employees (id, age)
VALUES (1, NULL);
```

to fail.

But it does **not**.

Why?

Because SQL's `CHECK` constraint doesn't require the expression to evaluate specifically to `TRUE`.

The row is rejected when the `CHECK` expression evaluates to `FALSE`.

If it evaluates to `TRUE` or `UNKNOWN`, the check does not reject the row.

And:

```sql
NULL >= 18
```

evaluates to:

```text
UNKNOWN
```

not `FALSE`.

Therefore:

```sql
age INT CHECK (age >= 18)
```

does not mean:

> age must be present and at least 18.

It means:

> If the expression evaluates to `FALSE`, reject the row.

---

# 15. `CHECK` + `NOT NULL`

If age must actually exist:

```sql
CREATE TABLE employees (
    id BIGINT PRIMARY KEY,

    age INT NOT NULL
        CHECK (age >= 18)
);
```

Now there are two independent rules:

```text
NOT NULL
   ↓
age must exist

CHECK (age >= 18)
   ↓
age must be at least 18
```

This is an important pattern.

For example:

```sql
INSERT INTO employees (id, age)
VALUES (1, NULL);
```

fails because of `NOT NULL`.

And:

```sql
INSERT INTO employees (id, age)
VALUES (2, 17);
```

fails because of `CHECK`.

While:

```sql
INSERT INTO employees (id, age)
VALUES (3, 25);
```

works.

---

# 16. `CHECK` is not a replacement for `NOT NULL`

Don't write:

```sql
age INT CHECK (age >= 18)
```

if your requirement is:

> Age must exist and be at least 18.

Use:

```sql
age INT NOT NULL CHECK (age >= 18)
```

This distinction is frequently tested in interviews.

---

# 17. `CHECK` and empty strings

Suppose:

```sql
CREATE TABLE users (
    name TEXT CHECK (length(name) > 0)
);
```

This prevents:

```text
''
```

but because `name` can still be `NULL`, this may still be allowed:

```sql
INSERT INTO users (name)
VALUES (NULL);
```

If you want:

> Name must exist and cannot be empty.

Use:

```sql
CREATE TABLE users (
    name TEXT NOT NULL
        CHECK (length(trim(name)) > 0)
);
```

Now:

```text
NULL       → rejected
''         → rejected
'   '      → rejected
'Alice'    → accepted
```

---

# 18. `CHECK` and business rules

This is where `CHECK` becomes very useful.

Suppose you're designing a banking system:

```sql
CREATE TABLE accounts (
    id BIGINT PRIMARY KEY,
    balance NUMERIC(15, 2) NOT NULL,
    interest_rate NUMERIC(5, 2) NOT NULL,

    CHECK (balance >= 0),
    CHECK (interest_rate >= 0 AND interest_rate <= 100)
);
```

You are encoding database invariants directly:

```text
balance >= 0

0 <= interest_rate <= 100
```

Another example:

```sql
CREATE TABLE transactions (
    id BIGINT PRIMARY KEY,
    amount NUMERIC(15, 2) NOT NULL,
    transaction_type TEXT NOT NULL,

    CHECK (amount > 0),
    CHECK (
        transaction_type IN ('CREDIT', 'DEBIT')
    )
);
```

---

# 19. What `CHECK` cannot easily enforce

This is just as important as understanding what it can do.

A `CHECK` constraint is primarily intended to enforce conditions based on the **current row**.

For example:

```sql
CHECK (price >= 0)
```

is straightforward.

But suppose you want:

> A customer's total order amount must never exceed their credit limit.

That involves other rows/tables.

A simple `CHECK` constraint isn't the right mechanism.

Similarly:

> An employee's manager must belong to the same department.

That involves relationships between rows and potentially another table.

For such rules, you may need:

* `FOREIGN KEY`
* `UNIQUE`
* transactions
* triggers
* application logic
* or other database mechanisms

We'll discuss where to draw this boundary later.

---

# 20. `CHECK` vs application validation

Suppose your Java application has:

```java
if (price < 0) {
    throw new IllegalArgumentException("Price cannot be negative");
}
```

That's useful.

But another client could execute:

```sql
INSERT INTO products (price)
VALUES (-100);
```

directly.

A database constraint:

```sql
CHECK (price >= 0)
```

protects the database regardless of which application writes the data.

A common architecture is therefore:

```text
                    Request
                       |
                       v
              Application validation
                       |
                       v
                  PostgreSQL
                       |
                       v
                DB constraints
```

Both layers can have value.

---

# 21. Changing a `CHECK` constraint

Suppose:

```sql
CREATE TABLE products (
    id BIGINT PRIMARY KEY,
    price NUMERIC(10, 2),

    CONSTRAINT chk_price
        CHECK (price >= 0)
);
```

To change the rule, you normally remove and recreate the constraint:

```sql
ALTER TABLE products
DROP CONSTRAINT chk_price;

ALTER TABLE products
ADD CONSTRAINT chk_price
CHECK (price >= 1);
```

The new constraint applies to subsequent changes and PostgreSQL also verifies existing rows when adding it normally.

---

# 22. A useful real-world example

Imagine a movie booking system:

```sql
CREATE TABLE shows (
    id BIGINT PRIMARY KEY,
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP NOT NULL,
    capacity INT NOT NULL,

    CHECK (start_time < end_time),
    CHECK (capacity > 0)
);
```

This protects two invariants:

```text
start_time < end_time
```

and:

```text
capacity > 0
```

You could then have:

```sql
CREATE TABLE bookings (
    id BIGINT PRIMARY KEY,
    show_id BIGINT NOT NULL,
    seat_number INT NOT NULL,

    CHECK (seat_number > 0),

    FOREIGN KEY (show_id)
        REFERENCES shows(id)
);
```

Now different constraints protect different aspects:

```text
PRIMARY KEY
    ↓
row identity

NOT NULL
    ↓
required values

CHECK
    ↓
valid values / relationships within the row

FOREIGN KEY
    ↓
valid relationship to another table
```

---

# 23. The mental model

Think of `CHECK` as a question PostgreSQL asks for every row:

```text
                 New/updated row
                       |
                       v
              +----------------+
              | CHECK condition |
              +----------------+
                       |
              +--------+--------+
              |                 |
            TRUE            FALSE
              |                 |
            Allow             ERROR
```

With one important SQL detail:

```text
UNKNOWN
   |
   v
not rejected by CHECK
```

That's why `NULL` often needs a separate `NOT NULL` constraint.

---

# Key takeaways

1. `CHECK` enforces a condition on row values.
2. It is checked during both `INSERT` and `UPDATE`.
3. It can validate a single column or multiple columns.
4. Common examples are ranges, allowed values, positive numbers, and date relationships.
5. `CHECK` does not automatically imply `NOT NULL`.
6. A `CHECK` expression evaluating to `FALSE` causes rejection; `UNKNOWN` does not.
7. Use `NOT NULL + CHECK` when a value must both exist and satisfy a condition.
8. `CHECK` is primarily suited to rules based on the row being checked.
9. Cross-row or cross-table rules generally require other mechanisms.
10. Named constraints make schema management and debugging easier.

## Next concept

**`DEFAULT` constraint** — how PostgreSQL automatically supplies values, when defaults are evaluated, `DEFAULT` vs `NOT NULL`, timestamps, identity columns, and some subtle cases where developers assume a default will be applied but it won't.

