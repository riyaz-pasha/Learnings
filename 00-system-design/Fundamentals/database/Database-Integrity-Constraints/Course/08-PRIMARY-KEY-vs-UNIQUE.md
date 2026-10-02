# 8. `PRIMARY KEY` vs `UNIQUE`

This is one of the most important distinctions in database design and interviews.

At first glance, they look very similar:

```sql
PRIMARY KEY
```

and

```sql
UNIQUE
```

Both prevent duplicate values.

But their **purpose is different**.

---

# 1. Simple definition

### `PRIMARY KEY`

> Defines the **official identity of a row** in a table.

### `UNIQUE`

> Ensures that a value or combination of values **does not appear more than once**.

Think:

```text
PRIMARY KEY
    ↓
"Which column identifies this row?"

UNIQUE
    ↓
"Which values must not be duplicated?"
```

---

# 2. Simple example

Consider a users table:

```sql
CREATE TABLE users (
    id BIGINT PRIMARY KEY,
    email TEXT UNIQUE,
    name TEXT
);
```

Suppose:

```text
id    email                 name
---------------------------------------
101   alice@example.com     Alice
102   bob@example.com       Bob
103   charlie@example.com   Charlie
```

Here:

```text
id
↓
PRIMARY KEY
↓
Official identity of the user
```

while:

```text
email
↓
UNIQUE
↓
No two users can have the same email
```

Both are unique, but they communicate different meanings.

---

# 3. Primary key automatically means `UNIQUE + NOT NULL`

When you write:

```sql
id BIGINT PRIMARY KEY
```

PostgreSQL effectively enforces:

```text
UNIQUE
+
NOT NULL
```

Therefore:

```sql
INSERT INTO users (id, email)
VALUES (101, 'new@example.com');
```

fails if `101` already exists.

And:

```sql
INSERT INTO users (id, email)
VALUES (NULL, 'new@example.com');
```

also fails.

---

# 4. `UNIQUE` does NOT automatically mean `NOT NULL`

This is a major difference.

Consider:

```sql
CREATE TABLE users (
    id BIGINT PRIMARY KEY,
    email TEXT UNIQUE
);
```

PostgreSQL normally allows:

```sql
INSERT INTO users (id, email)
VALUES (1, NULL);

INSERT INTO users (id, email)
VALUES (2, NULL);
```

So you can have:

```text
id    email
------------
1     NULL
2     NULL
3     NULL
```

Why?

Because PostgreSQL's normal `UNIQUE` constraint treats `NULL`s as distinct.

Therefore:

```text
PRIMARY KEY
→ unique + NOT NULL

UNIQUE
→ unique, but NULL is normally allowed
```

---

# 5. If email must be present and unique

Use:

```sql
email TEXT NOT NULL UNIQUE
```

Now:

```text
NULL
 ↓
rejected

duplicate email
 ↓
rejected
```

This is extremely common for things like:

```sql
email TEXT NOT NULL UNIQUE
```

---

# 6. Why not make everything a `PRIMARY KEY`?

Suppose:

```sql
CREATE TABLE users (
    id BIGINT PRIMARY KEY,
    email TEXT UNIQUE,
    username TEXT UNIQUE
);
```

There are three uniqueness rules:

```text
id
↓
PRIMARY KEY

email
↓
UNIQUE

username
↓
UNIQUE
```

Why not:

```sql
id PRIMARY KEY
email PRIMARY KEY
username PRIMARY KEY
```

Because a table can have **only one primary key constraint**.

The database needs one designated identity for the row.

But a table can have **multiple `UNIQUE` constraints**.

For example:

```sql
CREATE TABLE users (
    id BIGINT PRIMARY KEY,
    email TEXT UNIQUE,
    username TEXT UNIQUE,
    phone TEXT UNIQUE
);
```

This is perfectly normal.

---

# 7. Think about the meaning

Imagine a banking system:

```sql
CREATE TABLE accounts (
    account_id BIGINT PRIMARY KEY,
    account_number TEXT NOT NULL UNIQUE,
    iban TEXT UNIQUE,
    customer_id BIGINT NOT NULL
);
```

### `account_id`

```text
PRIMARY KEY
```

This is the database's official identity for the row.

### `account_number`

```text
UNIQUE
```

Two accounts should not have the same account number.

### `iban`

```text
UNIQUE
```

Two accounts should not have the same IBAN.

They are all unique, but only one is the table's designated primary identity.

---

# 8. Primary key is commonly referenced by foreign keys

Suppose:

```sql
CREATE TABLE users (
    id BIGINT PRIMARY KEY,
    email TEXT NOT NULL UNIQUE
);
```

Then:

```sql
CREATE TABLE orders (
    id BIGINT PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id)
);
```

Here:

```text
users.id
   ↑
   │
FOREIGN KEY
   │
orders.user_id
```

The primary key commonly serves as the target of relationships.

A `UNIQUE` column can also be referenced by a foreign key when it satisfies PostgreSQL's requirements for a referenced unique key.

For example:

```sql
CREATE TABLE users (
    id BIGINT PRIMARY KEY,
    email TEXT NOT NULL UNIQUE
);

CREATE TABLE notifications (
    id BIGINT PRIMARY KEY,
    user_email TEXT REFERENCES users(email)
);
```

This can be valid because `users.email` is unique.

But using a primary key as the normal relational identity is usually much clearer.

---

# 9. Composite `PRIMARY KEY` vs composite `UNIQUE`

Both can involve multiple columns.

### Composite primary key

```sql
PRIMARY KEY (student_id, course_id)
```

means:

> The combination of `student_id + course_id` identifies a row.

For example:

```text
student_id    course_id
-----------------------
1             101
1             102
2             101
```

All combinations are unique.

---

### Composite unique

```sql
UNIQUE (country_code, phone_number)
```

means:

> The combination of country code and phone number cannot be duplicated.

The table can still have a completely different primary key:

```sql
CREATE TABLE users (
    id BIGINT PRIMARY KEY,
    country_code TEXT,
    phone_number TEXT,

    UNIQUE (country_code, phone_number)
);
```

So:

```text
PRIMARY KEY
→ row identity

UNIQUE
→ additional uniqueness rule
```

---

# 10. Can a primary key be nullable?

No.

This:

```sql
id BIGINT PRIMARY KEY
```

cannot contain `NULL`.

A primary key must identify a row, and `NULL` essentially means "unknown/missing value."

Therefore:

```sql
INSERT INTO users (id)
VALUES (NULL);
```

fails.

---

# 11. Can a unique column contain NULL?

Yes, normally in PostgreSQL.

```sql
email TEXT UNIQUE
```

can contain multiple `NULL`s.

This is because:

```text
NULL ≠ NULL
```

in ordinary SQL equality semantics.

More precisely, comparing `NULL` using `=` produces `UNKNOWN`, rather than `TRUE`.

PostgreSQL also provides an advanced option if you want `NULL`s to be treated as duplicates:

```sql
email TEXT UNIQUE NULLS NOT DISTINCT
```

Then only one `NULL` is allowed.

This is useful, but the normal behavior to remember is:

```text
UNIQUE
→ multiple NULLs allowed

UNIQUE NULLS NOT DISTINCT
→ NULLs treated as duplicates
```

---

# 12. Can there be multiple `UNIQUE` constraints?

Absolutely.

For example:

```sql
CREATE TABLE employees (
    employee_id BIGINT PRIMARY KEY,

    email TEXT NOT NULL UNIQUE,

    employee_number TEXT NOT NULL UNIQUE,

    national_identifier TEXT UNIQUE
);
```

There is:

```text
1 PRIMARY KEY
3 UNIQUE constraints
```

That's completely normal.

---

# 13. Primary key doesn't necessarily mean "business unique"

This is an important design concept.

Consider:

```sql
CREATE TABLE users (
    id BIGINT PRIMARY KEY,
    email TEXT UNIQUE
);
```

The database identity is:

```text
id
```

But the business rule says:

```text
email must also be unique
```

These are two different concepts.

For example:

```text
id = 1001
email = alice@example.com
```

The ID might be an internal surrogate key, while email is a business-level uniqueness rule.

You should not assume:

> "If something is unique, it should be the primary key."

Uniqueness and identity are related but different concepts.

---

# 14. A real-world example

Consider an ecommerce system:

```sql
CREATE TABLE products (
    product_id BIGINT PRIMARY KEY,
    sku TEXT NOT NULL UNIQUE,
    barcode TEXT UNIQUE,
    name TEXT NOT NULL
);
```

Here:

### `product_id`

```text
PRIMARY KEY
```

Database identity.

### `sku`

```text
UNIQUE
```

Business rule:

> Two products cannot have the same SKU.

### `barcode`

```text
UNIQUE
```

Business rule:

> Two products cannot have the same barcode.

So:

```text
                 PRODUCTS
                    │
        ┌───────────┼────────────┐
        ↓           ↓            ↓
    product_id      sku       barcode
    PRIMARY KEY    UNIQUE      UNIQUE
        │
        │
     identity     business uniqueness
```

---

# 15. PostgreSQL indexes

There is another important technical difference worth knowing.

When PostgreSQL creates:

```sql
PRIMARY KEY
```

it creates a supporting unique index.

Similarly, a normal:

```sql
UNIQUE
```

constraint is backed by a unique index.

For example:

```sql
CREATE TABLE users (
    id BIGINT PRIMARY KEY,
    email TEXT UNIQUE
);
```

PostgreSQL needs efficient structures to enforce:

```text
id uniqueness
email uniqueness
```

So indexes are involved.

However, you should think of:

```text
PRIMARY KEY / UNIQUE
```

as **constraints expressing database rules**, rather than simply thinking "they are indexes."

The constraint represents the business/data-integrity rule; PostgreSQL uses an index to enforce it efficiently.

---

# 16. Important interview comparison

| Feature                           | `PRIMARY KEY`    | `UNIQUE`                  |
| --------------------------------- | ---------------- | ------------------------- |
| Purpose                           | Identifies a row | Prevents duplicate values |
| Duplicate values                  | Not allowed      | Not allowed               |
| `NULL`                            | Not allowed      | Normally allowed          |
| Number per table                  | One              | Multiple                  |
| Can be composite?                 | Yes              | Yes                       |
| Can be referenced by FK?          | Yes              | Yes, when suitable        |
| Creates supporting unique index?  | Yes              | Yes                       |
| Automatically `NOT NULL`?         | Yes              | No                        |
| Represents official row identity? | Yes              | Not necessarily           |

---

# 17. A very common interview question

### Question

Can a table have:

```sql
id PRIMARY KEY
```

and:

```sql
email UNIQUE
```

at the same time?

### Answer

Yes.

In fact, this is extremely common:

```sql
CREATE TABLE users (
    id BIGINT PRIMARY KEY,
    email TEXT NOT NULL UNIQUE,
    name TEXT NOT NULL
);
```

Here:

```text
id
→ identifies the row

email
→ must be unique
```

---

# 18. Another interview question

### Question

Can a table have two primary keys?

No.

This is invalid conceptually:

```sql
CREATE TABLE users (
    id BIGINT PRIMARY KEY,
    email TEXT PRIMARY KEY
);
```

A table can have **one primary key constraint**.

But you can have:

```sql
CREATE TABLE users (
    id BIGINT PRIMARY KEY,
    email TEXT UNIQUE,
    username TEXT UNIQUE
);
```

One primary key, multiple unique constraints.

---

# 19. One subtle point: composite primary key

Consider:

```sql
CREATE TABLE enrollments (
    student_id BIGINT,
    course_id BIGINT,

    PRIMARY KEY (student_id, course_id)
);
```

This does **not** mean:

```text
student_id must be globally unique
```

and it does **not** mean:

```text
course_id must be globally unique
```

It means:

```text
(student_id, course_id)
must be unique
```

Therefore:

```text
student_id    course_id
-----------------------
1             101       ✓
1             102       ✓
2             101       ✓
2             102       ✓
1             101       ✗ duplicate
```

The same principle applies to:

```sql
UNIQUE (student_id, course_id)
```

The difference is that a composite primary key is the table's designated identity, while a composite unique constraint is an additional uniqueness rule.

---

# 20. Mental model

When deciding between them, ask two questions.

### Question 1

> "What identifies this row?"

Use:

```sql
PRIMARY KEY
```

### Question 2

> "What values/combinations must not be duplicated?"

Use:

```sql
UNIQUE
```

For example:

```sql
CREATE TABLE customers (
    customer_id BIGINT PRIMARY KEY,

    email TEXT NOT NULL UNIQUE,

    phone TEXT UNIQUE,

    username TEXT NOT NULL UNIQUE
);
```

Think:

```text
customer_id
    ↓
WHO IS THIS ROW?
    ↓
PRIMARY KEY


email / phone / username
    ↓
CAN TWO ROWS HAVE THE SAME VALUE?
    ↓
UNIQUE
```

That's the fundamental difference.

---

## Key takeaway

> **`PRIMARY KEY` defines identity. `UNIQUE` defines additional uniqueness.**

A useful schema often looks like:

```sql
CREATE TABLE users (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    email TEXT NOT NULL UNIQUE,
    username TEXT NOT NULL UNIQUE,
    phone TEXT UNIQUE
);
```

One row identity:

```text
id → PRIMARY KEY
```

Multiple uniqueness rules:

```text
email    → UNIQUE
username → UNIQUE
phone    → UNIQUE
```

---

## Next concept

**9. Foreign Key Actions: `CASCADE`, `RESTRICT`, `NO ACTION`, `SET NULL`, `SET DEFAULT`**

