# 4. `PRIMARY KEY` Constraint in PostgreSQL

A **primary key** is the constraint that defines how a row is **uniquely identified within a table**.

The simplest mental model is:

> **A primary key is the official identity of a row.**

---

# 1. The problem

Consider:

```sql
CREATE TABLE users (
    id BIGINT,
    name TEXT,
    email TEXT
);
```

We have:

```text
id    name     email
---   ------   -------------------
1     Alice    alice@example.com
2     Bob      bob@example.com
3     Charlie  charlie@example.com
```

How do we identify Alice?

We could say:

```text
email = alice@example.com
```

But perhaps email can change.

The database needs a reliable way to say:

> "This exact row is user 1."

That's the purpose of a primary key.

---

# 2. Basic syntax

```sql
CREATE TABLE users (
    id BIGINT PRIMARY KEY,
    name TEXT NOT NULL,
    email TEXT
);
```

Now `id` is the primary key.

PostgreSQL guarantees that:

```text
id
 |
 +--> cannot be NULL
 |
 +--> cannot be duplicated
```

So this is invalid:

```sql
INSERT INTO users (id, name)
VALUES (1, 'Alice');

INSERT INTO users (id, name)
VALUES (1, 'Bob');
```

The second insert fails because `id = 1` already exists.

And this is also invalid:

```sql
INSERT INTO users (id, name)
VALUES (NULL, 'Charlie');
```

because a primary key cannot be `NULL`.

---

# 3. Primary key = uniqueness + non-null

Conceptually:

```text
PRIMARY KEY
     |
     +---- UNIQUE
     |
     +---- NOT NULL
```

So:

```sql
id BIGINT PRIMARY KEY
```

provides both:

```text
id must exist
+
id must be unique
```

However, there is an important semantic difference:

> A primary key isn't merely a column that happens to be unique. It represents the table's chosen identity for its rows.

---

# 4. Why can't a primary key be `NULL`?

Suppose:

```text
id    name
---   -----
1     Alice
2     Bob
NULL  Charlie
```

How would we refer to Charlie's row using the primary key?

We can't.

A primary key must provide a definite identity.

That's why PostgreSQL doesn't allow:

```sql
id BIGINT PRIMARY KEY
```

to contain `NULL`.

---

# 5. Only one primary key per table

This is an important rule.

You cannot do:

```sql
CREATE TABLE users (
    id BIGINT PRIMARY KEY,
    email TEXT PRIMARY KEY
);
```

A table can have **only one primary key constraint**.

But that does **not** mean a table can only have one unique field.

You can have:

```sql
CREATE TABLE users (
    id BIGINT PRIMARY KEY,
    email TEXT UNIQUE,
    username TEXT UNIQUE
);
```

Here:

```text
PRIMARY KEY
    ↓
id

UNIQUE
    ↓
email

UNIQUE
    ↓
username
```

There is one primary key but multiple unique constraints.

---

# 6. Primary key vs `UNIQUE`

This is a very common interview question.

| Property                  | `PRIMARY KEY` | `UNIQUE`        |
| ------------------------- | ------------- | --------------- |
| Prevents duplicate values | Yes           | Yes             |
| Allows `NULL`             | No            | Normally yes    |
| Number per table          | One           | Multiple        |
| Represents row identity   | Yes           | Not necessarily |
| Automatically `NOT NULL`  | Yes           | No              |
| Can be composite          | Yes           | Yes             |

Example:

```sql
CREATE TABLE users (
    id BIGINT PRIMARY KEY,
    email TEXT UNIQUE
);
```

Here:

```text
id
→ official identity

email
→ another value that happens to be unique
```

That distinction becomes especially important when designing relationships.

---

# 7. Primary keys and foreign keys

Consider two tables:

```text
users
----------------
id       PRIMARY KEY
name
```

and:

```text
orders
----------------
id
user_id
```

We want:

```text
orders.user_id
        |
        v
users.id
```

We can define:

```sql
CREATE TABLE users (
    id BIGINT PRIMARY KEY,
    name TEXT NOT NULL
);

CREATE TABLE orders (
    id BIGINT PRIMARY KEY,
    user_id BIGINT NOT NULL,

    FOREIGN KEY (user_id)
        REFERENCES users(id)
);
```

Now the relationship is:

```text
users
+---------+
| id      | <------+
+---------+        |
                    |
                 references
                    |
                    |
+---------+         |
| orders  |         |
| user_id | --------+
+---------+
```

The `PRIMARY KEY` gives the parent row its identity.

The `FOREIGN KEY` allows another table to refer to that identity.

We'll study foreign keys in detail later.

---

# 8. Single-column primary key

The most common case is one column:

```sql
CREATE TABLE products (
    product_id BIGINT PRIMARY KEY,
    name TEXT NOT NULL
);
```

Here:

```text
product_id
```

uniquely identifies every product.

---

# 9. Composite primary key

A primary key doesn't have to consist of one column.

You can have:

```sql
CREATE TABLE order_items (
    order_id BIGINT,
    product_id BIGINT,
    quantity INT NOT NULL,

    PRIMARY KEY (order_id, product_id)
);
```

Now the primary key is:

```text
(order_id, product_id)
```

The **combination** must be unique.

For example:

```text
order_id    product_id
--------    ----------
100         10
100         20
101         10
101         20
```

All are valid.

But:

```text
order_id    product_id
--------    ----------
100         10
100         10   <-- duplicate combination
```

is invalid.

Notice that:

```text
order_id
```

doesn't have to be unique.

A single order can contain many products.

Likewise:

```text
product_id
```

doesn't have to be unique.

A product can appear in many orders.

The combination identifies the row.

---

# 10. Why composite primary keys are useful

Consider a course enrollment system:

```text
student
course
```

A student can enroll in multiple courses.

A course can have multiple students.

So:

```sql
CREATE TABLE enrollments (
    student_id BIGINT,
    course_id BIGINT,

    PRIMARY KEY (student_id, course_id)
);
```

This says:

> A particular student can be enrolled in a particular course at most once.

The primary key represents the identity of the **relationship row**.

---

# 11. Naming the primary key constraint

You can explicitly name it:

```sql
CREATE TABLE users (
    id BIGINT,
    name TEXT,

    CONSTRAINT pk_users
        PRIMARY KEY (id)
);
```

This can make migrations and database administration easier.

For example:

```sql
ALTER TABLE users
DROP CONSTRAINT pk_users;
```

---

# 12. Adding a primary key to an existing table

Suppose:

```sql
CREATE TABLE users (
    id BIGINT,
    name TEXT
);
```

You can later add:

```sql
ALTER TABLE users
ADD CONSTRAINT pk_users
PRIMARY KEY (id);
```

But PostgreSQL must verify that the existing data satisfies the primary-key rules.

For example, this data is invalid:

```text
id
---
1
2
2
NULL
```

There is both:

```text
duplicate 2
```

and:

```text
NULL
```

You must fix the existing data before PostgreSQL can establish the primary key.

---

# 13. Primary key and indexes

Like `UNIQUE`, PostgreSQL creates an index to enforce a primary key.

For:

```sql
CREATE TABLE users (
    id BIGINT PRIMARY KEY
);
```

PostgreSQL creates a corresponding unique B-tree index by default.

Conceptually:

```text
PRIMARY KEY
     |
     v
Unique index
     |
     +---- fast lookup
     |
     +---- uniqueness enforcement
```

This is useful for queries such as:

```sql
SELECT *
FROM users
WHERE id = 123;
```

The primary-key index can efficiently locate the row.

---

# 14. Does the primary key have to be an `id` column?

No.

This is perfectly valid:

```sql
CREATE TABLE countries (
    country_code CHAR(2) PRIMARY KEY,
    name TEXT NOT NULL
);
```

For example:

```text
country_code    name
------------    -----
IN              India
US              United States
GB              United Kingdom
```

The primary key is:

```text
country_code
```

not an automatically generated numeric ID.

So:

> A primary key is a **concept**, not a specific column name or data type.

---

# 15. Natural key vs surrogate key

This leads to an important database-design concept.

## Natural key

A real-world attribute is used as the primary key.

Example:

```sql
country_code CHAR(2) PRIMARY KEY
```

The value already has meaning in the real world.

---

## Surrogate key

A separate artificial identifier is created specifically for database identity.

For example:

```sql
id BIGINT PRIMARY KEY
```

while:

```text
email
username
employee_number
```

are business attributes.

A common design is:

```sql
CREATE TABLE employees (
    id BIGINT PRIMARY KEY,
    employee_number TEXT NOT NULL UNIQUE,
    name TEXT NOT NULL
);
```

Here:

```text
id
→ database identity

employee_number
→ business identifier
```

These are different concepts.

---

# 16. Generated primary keys

In real PostgreSQL applications, you often don't want the application to manually generate IDs.

PostgreSQL can generate them.

One modern option is:

```sql
CREATE TABLE users (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name TEXT NOT NULL
);
```

Then:

```sql
INSERT INTO users (name)
VALUES ('Alice');
```

PostgreSQL generates the ID.

For example:

```text
id    name
---   -----
1     Alice
2     Bob
3     Charlie
```

`IDENTITY` columns are PostgreSQL's SQL-standard-oriented mechanism for generated values.

You may also encounter older PostgreSQL schemas using:

```sql
SERIAL
```

such as:

```sql
id BIGSERIAL PRIMARY KEY
```

`SERIAL` is legacy shorthand built around sequences; for new schemas, `IDENTITY` is generally the clearer modern choice.

---

# 17. A primary key doesn't mean "business uniqueness"

This distinction is very important.

Suppose:

```sql
CREATE TABLE users (
    id BIGINT PRIMARY KEY,
    email TEXT NOT NULL,
    username TEXT NOT NULL
);
```

The primary key guarantees:

```text
id is unique
```

But it does **not** guarantee:

```text
email is unique
username is unique
```

If those are business rules, explicitly enforce them:

```sql
CREATE TABLE users (
    id BIGINT PRIMARY KEY,
    email TEXT NOT NULL UNIQUE,
    username TEXT NOT NULL UNIQUE
);
```

So we can have:

```text
                  users
                    |
        +-----------+-----------+
        |           |           |
        v           v           v
       id         email      username
        |           |           |
   PRIMARY KEY    UNIQUE      UNIQUE
```

---

# 18. A complete example

Consider an ecommerce system:

```sql
CREATE TABLE products (
    product_id BIGINT GENERATED ALWAYS AS IDENTITY,
    sku TEXT NOT NULL,
    name TEXT NOT NULL,
    price NUMERIC(12, 2) NOT NULL,

    CONSTRAINT pk_products
        PRIMARY KEY (product_id),

    CONSTRAINT uq_products_sku
        UNIQUE (sku)
);
```

We now have two different guarantees:

```text
product_id
    ↓
PRIMARY KEY
    ↓
Identifies the database row
```

and:

```text
sku
    ↓
UNIQUE
    ↓
Prevents duplicate product SKUs
```

For example:

```text
product_id    sku          name
-----------   ----------   ----------
1             IPHONE15     iPhone 15
2             MACBOOK01    MacBook
3             AIRPODS01    AirPods
```

If somebody tries:

```sql
INSERT INTO products (sku, name)
VALUES ('IPHONE15', 'Another Product');
```

the `UNIQUE` constraint rejects it.

If somebody tries to create another row with:

```text
product_id = 1
```

the primary key rejects it.

---

# 19. The mental model

Think about the difference this way:

```text
PRIMARY KEY
    ↓
"Which row is this?"
```

while:

```text
UNIQUE
    ↓
"Can another row have this value?"
```

For example:

```text
users
--------------------------------
id          email
--------------------------------
101         alice@example.com
102         bob@example.com
```

The database says:

```text
id = 101
    ↓
This identifies the row.
```

while:

```text
email = alice@example.com
    ↓
No other user can have this email.
```

Those are related but different responsibilities.

---

# Key takeaways

1. A `PRIMARY KEY` uniquely identifies each row in a table.
2. A primary key is implicitly `NOT NULL` and unique.
3. A table can have only **one primary key constraint**.
4. A table can have multiple `UNIQUE` constraints.
5. A primary key can consist of one column or multiple columns.
6. `PRIMARY KEY (a, b)` makes the **combination** unique.
7. Primary keys are commonly referenced by foreign keys.
8. PostgreSQL creates a unique index to support primary-key enforcement.
9. A primary key doesn't have to be called `id`.
10. `GENERATED ... AS IDENTITY` can automatically generate numeric primary-key values.
11. A primary key represents **row identity**; other business attributes may need separate `UNIQUE` constraints.

## Next concept

**`FOREIGN KEY`** — how PostgreSQL maintains relationships between tables, what referential integrity actually means, what happens when a referenced row is deleted or updated, and why `ON DELETE CASCADE` can be dangerous if misunderstood.

