# 2. `NOT NULL` Constraint in PostgreSQL

The `NOT NULL` constraint ensures that a column cannot contain a `NULL` value.

Let's understand this from the basics, including how PostgreSQL treats `NULL`, how to define the constraint, and what happens during `INSERT` and `UPDATE`.

## 1. The problem: Missing values

Imagine you're designing a users table.

SQL

```
CREATE TABLE users (
    id BIGINT,
    name TEXT,
    email TEXT
);
```

You insert a user:

SQL

```
INSERT INTO users (id, name, email)
VALUES (1, 'Riyaz', 'riyaz@example.com');
```

No problem.

But what about this?

SQL

```
INSERT INTO users (id, name, email)
VALUES (2, NULL, NULL);
```

PostgreSQL accepts it.

Why? Because, by default, PostgreSQL allows `NULL` values in columns unless a constraint prevents them.

However, perhaps your application requires every user to have a name and email.

That's where `NOT NULL` comes in.

## 2. Basic syntax

SQL

```
CREATE TABLE users (
    id BIGINT,
    name TEXT NOT NULL,
    email TEXT NOT NULL
);
```

Now let's try some inserts.

Accepted

SQL

```
INSERT INTO users (id, name, email)
VALUES (1, 'Riyaz', 'riyaz@example.com');
```

Rejected

SQL

```
INSERT INTO users (id, name, email)
VALUES (2, NULL, 'riyaz@example.com');
```

Error: null value in column "name" violates not-null constraint.

Rejected

SQL

```
INSERT INTO users (id, name, email)
VALUES (3, 'Riyaz', NULL);
```

Error: null value in column "email" violates not-null constraint.

The database rejects the entire statement when a `NOT NULL` constraint is violated.

## 3. What exactly is `NULL`?

This is an important SQL concept.

`NULL` represents an unknown or missing value. It is not the same as:

| Value   | Meaning                       |
| ------- | ----------------------------- |
| `NULL`  | Missing or unknown value      |
| `0`     | A numeric value equal to zero |
| `''`    | An empty string               |
| `' '`   | A string containing a space   |
| `FALSE` | A boolean false value         |

For example:

SQL

```
INSERT INTO users (id, name, email)
VALUES
    (1, NULL, 'a@example.com'),
    (2, '', 'b@example.com'),
    (3, ' ', 'c@example.com');
```

If `name` has no `NOT NULL` constraint, all three rows are valid.

Even if you add `NOT NULL`, the empty string and whitespace string are still allowed.

This is because `NOT NULL` only checks whether the value is `NULL`. It doesn't check whether the value is meaningful.

If you also want to prevent empty names, you can use a `CHECK` constraint:

SQL

```
CREATE TABLE users (
    id BIGINT,
    name TEXT NOT NULL CHECK (length(trim(name)) > 0)
);
```

We'll explore `CHECK` constraints in a later lesson.

## 4. `NULL` comparisons: A common SQL trap

Consider this query:

SQL

```
SELECT *
FROM users
WHERE name = NULL;
```

You might expect it to return users whose names are missing.

But it doesn't work that way.

In SQL, comparisons involving `NULL` generally produce `UNKNOWN`, rather than `TRUE` or `FALSE`.

### Three-valued logic

Unlike ordinary boolean logic, SQL uses three possible outcomes.

| Expression    | Result    |
| ------------- | --------- |
| `5 = 5`       | `TRUE`    |
| `5 = 10`      | `FALSE`   |
| `NULL = 5`    | `UNKNOWN` |
| `NULL = NULL` | `UNKNOWN` |
| `NULL <> 5`   | `UNKNOWN` |

To check for missing values, use:

SQL

```
SELECT *
FROM users
WHERE name IS NULL;
```

To find rows with a present name:

SQL

```
SELECT *
FROM users
WHERE name IS NOT NULL;
```

Remember this distinction because it becomes particularly important when working with `CHECK`, `UNIQUE`, and `FOREIGN KEY` constraints.

## 5. Adding `NOT NULL` to an existing table

Suppose you already have a table:

SQL

```
CREATE TABLE employees (
    id BIGINT PRIMARY KEY,
    name TEXT
);
```

And you have existing records:

SQL

```
INSERT INTO employees VALUES
    (1, 'Alice'),
    (2, 'Bob'),
    (3, NULL);
```

Now you want to make `name` mandatory.

You might try:

SQL

```
ALTER TABLE employees
ALTER COLUMN name SET NOT NULL;
```

PostgreSQL rejects this because an existing row contains `NULL`.

First, find the problematic records:

SQL

```
SELECT *
FROM employees
WHERE name IS NULL;
```

Fix the data:

SQL

```
UPDATE employees
SET name = 'Unknown'
WHERE name IS NULL;
```

Then add the constraint:

SQL

```
ALTER TABLE employees
ALTER COLUMN name SET NOT NULL;
```

To remove the constraint later:

SQL

```
ALTER TABLE employees
ALTER COLUMN name DROP NOT NULL;
```

Note that dropping `NOT NULL` does not delete or modify any existing values.

## 6. What happens during `UPDATE`?

Constraints are not just checked during inserts.

They also apply to updates.

Consider:

SQL

```
CREATE TABLE accounts (
    id BIGINT PRIMARY KEY,
    account_holder TEXT NOT NULL
);
```

Insert:

SQL

```
INSERT INTO accounts
VALUES (1, 'Alice');
```

This works.

But:

SQL

```
UPDATE accounts
SET account_holder = NULL
WHERE id = 1;
```

PostgreSQL rejects the update.

The row remains unchanged because the update violates the constraint.

This is one of the most important properties of database constraints: they protect the data regardless of whether the change comes from an `INSERT` or an `UPDATE`.

## 7. `NOT NULL` vs `DEFAULT`

These two features are often confused.

Consider:

SQL

```
CREATE TABLE orders (
    id BIGINT PRIMARY KEY,
    status TEXT NOT NULL DEFAULT 'PENDING'
);
```

What happens in each situation?

| SQL operation                    | Result                |
| -------------------------------- | --------------------- |
| Omit `status`                    | `'PENDING'`           |
| Explicitly provide `'COMPLETED'` | `'COMPLETED'`         |
| Explicitly provide `NULL`        | Error                 |
| Explicitly provide `''`          | Empty string accepted |

The key distinction:

* `DEFAULT` supplies a value when the column is omitted or the `DEFAULT` keyword is used.

* `NOT NULL` rejects an explicitly supplied `NULL` value.

For example:

SQL

```
INSERT INTO orders (id)
VALUES (1);
```

The default applies.

SQL

```
INSERT INTO orders (id, status)
VALUES (2, NULL);
```

This fails.

A default value does not automatically make a column `NOT NULL`.

## 8. Real-world example: Banking system

Consider a simplified bank account table:

SQL

```
CREATE TABLE bank_accounts (
    account_id BIGINT PRIMARY KEY,
    customer_id BIGINT NOT NULL,
    account_number TEXT NOT NULL,
    currency CHAR(3) NOT NULL DEFAULT 'INR',
    balance NUMERIC(15, 2) NOT NULL DEFAULT 0.00
);
```

Here:

* `account_id` must identify an account.

* `customer_id` must be present.

* `account_number` must be present.

* `currency` defaults to INR when omitted.

* `balance` defaults to zero and cannot be `NULL`.

Notice that this schema doesn't yet prevent negative balances or duplicate account numbers. Those require additional constraints, which we'll study in upcoming lessons.

## 9. Quick practice

1. What happens when you insert NULL into a NOT NULL column?

PostgreSQL converts it to an empty string

PostgreSQL rejects the operation

PostgreSQL uses the column's default value

PostgreSQL ignores the column

2. A column has NOT NULL DEFAULT 100. What happens if you omit that column from an INSERT?

The insert fails

The column gets NULL

The column gets 100

The column gets 0

3. Does NOT NULL reject an empty string ('')?

Yes, always

No, it only rejects NULL

Only for TEXT columns

Only when a DEFAULT is defined

Check answers

### Key takeaways

* `NOT NULL` prevents missing values in a column.

* `NULL` is different from zero, an empty string, and `FALSE`.

* Use `IS NULL` and `IS NOT NULL` instead of equality comparisons with `NULL`.

* `NOT NULL` is enforced during both inserts and updates.

* `DEFAULT` supplies omitted values; `NOT NULL` rejects null values.

* You can add or remove `NOT NULL` on existing columns using `ALTER TABLE`.

Next concept: `UNIQUE` constraint — how PostgreSQL prevents duplicate values, what makes `NULL` particularly interesting with `UNIQUE`, and how unique indexes work behind the scenes.

