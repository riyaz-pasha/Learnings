# 3. `UNIQUE` Constraint in PostgreSQL

The `UNIQUE` constraint ensures that **no two rows have the same value for a column or combination of columns**.

## 1. The problem

Suppose we have:

```sql
CREATE TABLE users (
    id BIGINT,
    email TEXT
);
```

Nothing prevents this:

```sql
INSERT INTO users VALUES (1, 'alice@example.com');
INSERT INTO users VALUES (2, 'alice@example.com');
INSERT INTO users VALUES (3, 'alice@example.com');
```

We now have three users with the same email.

If email is supposed to uniquely identify a user, this is invalid data.

We can enforce the rule with:

```sql
CREATE TABLE users (
    id BIGINT,
    email TEXT UNIQUE
);
```

Now:

```sql
INSERT INTO users VALUES (1, 'alice@example.com');
```

works.

But:

```sql
INSERT INTO users VALUES (2, 'alice@example.com');
```

fails because that email already exists.

---

# 2. Basic definition

The actual rule is:

> A `UNIQUE` constraint requires the values in a column, or combination of columns, to be unique among rows.

For a single column:

```sql
CREATE TABLE users (
    id BIGINT,
    email TEXT UNIQUE
);
```

For multiple columns:

```sql
CREATE TABLE user_roles (
    user_id BIGINT,
    role_id BIGINT,
    UNIQUE (user_id, role_id)
);
```

The second example means:

```text
(user_id, role_id)
```

as a pair must be unique.

It does **not** mean that `user_id` must be unique by itself or `role_id` must be unique by itself.

---

# 3. Column-level vs table-level syntax

Both of these are valid.

### Column-level

```sql
CREATE TABLE users (
    id BIGINT,
    email TEXT UNIQUE
);
```

### Table-level

```sql
CREATE TABLE users (
    id BIGINT,
    email TEXT,
    UNIQUE (email)
);
```

For a single column, they are effectively expressing the same rule.

Table-level syntax becomes particularly useful for composite uniqueness:

```sql
CREATE TABLE memberships (
    user_id BIGINT,
    organization_id BIGINT,

    UNIQUE (user_id, organization_id)
);
```

---

# 4. `UNIQUE` with multiple columns

This is an important concept.

Consider:

```sql
CREATE TABLE enrollments (
    student_id BIGINT,
    course_id BIGINT,

    UNIQUE (student_id, course_id)
);
```

Suppose we have:

```text
student_id    course_id
-----------   ---------
1             101
1             102
2             101
2             102
```

All of these are valid.

Why?

Because each **combination** is different.

But this is invalid:

```text
student_id    course_id
-----------   ---------
1             101
1             101   <-- duplicate combination
```

The rule is:

```text
(student_id, course_id) must be unique
```

not:

```text
student_id must be unique
course_id must be unique
```

This pattern is extremely common in many-to-many relationships.

For example:

```text
users
  |
  | many-to-many
  |
roles
```

The join table might have:

```sql
CREATE TABLE user_roles (
    user_id BIGINT,
    role_id BIGINT,

    UNIQUE (user_id, role_id)
);
```

This prevents assigning the same role to the same user twice.

---

# 5. `UNIQUE` and `NULL`

This is one of the most important PostgreSQL details.

Consider:

```sql
CREATE TABLE users (
    id BIGINT,
    email TEXT UNIQUE
);
```

Now:

```sql
INSERT INTO users VALUES (1, NULL);
INSERT INTO users VALUES (2, NULL);
```

Are both allowed?

Yes.

This surprises many people.

Why?

Because `NULL` represents an unknown/missing value, and PostgreSQL's traditional `UNIQUE` behavior allows multiple `NULL` values.

For example:

```text
id    email
---   -------------------
1     alice@example.com
2     bob@example.com
3     NULL
4     NULL
```

This is valid under the normal PostgreSQL `UNIQUE` constraint.

---

# 6. Why does this happen?

Remember what we discussed with `NULL`:

```sql
NULL = NULL
```

does not evaluate to `TRUE`.

It evaluates to `UNKNOWN`.

Therefore, two `NULL` values aren't treated as ordinary equal values for a traditional unique constraint.

This leads to an important distinction:

```sql
email TEXT UNIQUE
```

means roughly:

> Non-null email values must be unique.

It does not mean:

> There can only be one row where email is missing.

---

# 7. `UNIQUE` + `NOT NULL`

If your business rule is:

> Every user must have an email, and every email must be unique.

Use:

```sql
CREATE TABLE users (
    id BIGINT PRIMARY KEY,
    email TEXT NOT NULL UNIQUE
);
```

Now:

```text
                 email
                   |
             +-----+-----+
             |           |
          NOT NULL     UNIQUE
             |           |
        must exist    no duplicates
```

This is a very common combination.

---

# 8. Naming a `UNIQUE` constraint

You can explicitly name the constraint:

```sql
CREATE TABLE users (
    id BIGINT,
    email TEXT,

    CONSTRAINT uq_users_email UNIQUE (email)
);
```

This is often preferable in production schemas because the constraint has a meaningful name.

Instead of a database-generated name, you get:

```text
uq_users_email
```

This makes migrations and debugging easier.

For example:

```sql
ALTER TABLE users
DROP CONSTRAINT uq_users_email;
```

---

# 9. Adding `UNIQUE` to an existing table

Suppose:

```sql
CREATE TABLE users (
    id BIGINT,
    email TEXT
);
```

You can add a constraint:

```sql
ALTER TABLE users
ADD CONSTRAINT uq_users_email UNIQUE (email);
```

But PostgreSQL first has to verify that the existing data doesn't violate the rule.

Suppose the table already contains:

```text
id    email
---   -------------------
1     alice@example.com
2     bob@example.com
3     alice@example.com
```

Adding the constraint fails because:

```text
alice@example.com
```

appears twice.

You need to fix the existing data first.

---

# 10. What happens during `UPDATE`?

Constraints apply to updates too.

Suppose:

```sql
CREATE TABLE users (
    id BIGINT PRIMARY KEY,
    email TEXT UNIQUE
);
```

Data:

```text
id    email
---   -------------------
1     alice@example.com
2     bob@example.com
```

Now:

```sql
UPDATE users
SET email = 'alice@example.com'
WHERE id = 2;
```

PostgreSQL rejects the update because it would create a duplicate.

So again:

```text
INSERT ──┐
         ├──> UNIQUE constraint
UPDATE ──┘
```

Both are protected.

---

# 11. `UNIQUE` vs `PRIMARY KEY`

These are closely related but **not the same**.

| Feature                  | `PRIMARY KEY` | `UNIQUE`         |
| ------------------------ | ------------- | ---------------- |
| Prevents duplicates      | Yes           | Yes              |
| Allows `NULL`            | No            | Yes, normally    |
| One per table            | Yes           | Multiple allowed |
| Identifies the row       | Yes           | Not necessarily  |
| Automatically `NOT NULL` | Yes           | No               |

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
↓
PRIMARY KEY
↓
unique + not null
```

while:

```text
email
↓
UNIQUE
↓
unique non-null values, but NULL is normally allowed
```

We'll study `PRIMARY KEY` in detail next.

---

# 12. `UNIQUE` creates an index

This is an important PostgreSQL implementation detail.

When you create:

```sql
CREATE TABLE users (
    email TEXT UNIQUE
);
```

PostgreSQL creates a unique index to enforce the uniqueness rule.

Conceptually:

```text
UNIQUE constraint
       |
       v
Unique index
       |
       v
Fast lookup + duplicate prevention
```

You don't normally need to manually create another index just because you have a `UNIQUE` constraint.

For example:

```sql
CREATE TABLE users (
    id BIGINT PRIMARY KEY,
    email TEXT UNIQUE
);
```

PostgreSQL has indexes associated with both uniqueness constraints.

This is one reason `UNIQUE` is more than just application-level validation.

---

# 13. Why application checks aren't enough

You might write:

```java
if (!userRepository.existsByEmail(email)) {
    userRepository.save(user);
}
```

But two requests can arrive simultaneously:

```text
Request A                  Request B
---------                  ---------
email doesn't exist       email doesn't exist
        |                         |
        v                         v
     INSERT                    INSERT
```

Both applications could observe that the email doesn't exist.

A database `UNIQUE` constraint gives PostgreSQL the final authority:

```text
Request A ──┐
            |
Request B ──┼──> PostgreSQL
            |        |
            |      UNIQUE
            |        |
            └──> one conflicting write fails
```

Application validation is still useful for a friendly error message, but the database constraint protects the invariant.

---

# 14. A real-world example

Consider an ecommerce system:

```sql
CREATE TABLE products (
    id BIGINT PRIMARY KEY,
    sku TEXT NOT NULL UNIQUE,
    name TEXT NOT NULL,
    price NUMERIC(12, 2) NOT NULL
);
```

Here:

```text
id
→ identifies the database row

sku
→ identifies the product's business/catalog code

name
→ must exist

price
→ must exist
```

The important point is that `id` and `sku` serve different purposes.

For example:

```text
id     sku
----   -----------
101    IPHONE15
102    MACBOOK01
103    AIRPODS02
```

The database can use the generated/internal `id` as the primary key while enforcing that the business-facing `sku` is unique.

---

# 15. PostgreSQL-specific: `NULLS NOT DISTINCT`

Modern PostgreSQL also provides a way to change the normal `NULL` behavior.

Normally:

```sql
UNIQUE (email)
```

allows multiple `NULL`s.

But you can explicitly say:

```sql
CREATE TABLE users (
    id BIGINT,
    email TEXT,

    CONSTRAINT uq_users_email
        UNIQUE NULLS NOT DISTINCT (email)
);
```

Now PostgreSQL treats `NULL` values as equal for this uniqueness constraint.

So:

```sql
INSERT INTO users VALUES (1, NULL);
```

works.

But:

```sql
INSERT INTO users VALUES (2, NULL);
```

fails.

This is useful when the business rule is:

> There can be at most one row with a missing value.

It's a more advanced PostgreSQL feature, so remember the normal behavior first.

---

# 16. Another useful pattern: conditional uniqueness

Sometimes the requirement is:

> Active users must have unique email addresses, but inactive users can share them.

This can be implemented using a **partial unique index**:

```sql
CREATE UNIQUE INDEX uq_active_users_email
ON users (email)
WHERE active = TRUE;
```

Now uniqueness is enforced only for rows where:

```sql
active = TRUE
```

For example:

```text
id    email                active
---   ------------------   ------
1     a@example.com        TRUE
2     a@example.com        FALSE
3     a@example.com        FALSE
```

This can be valid because only active rows participate in the unique index.

This is an advanced pattern we'll revisit when discussing PostgreSQL indexing and constraints.

---

# Mental model

Think of `UNIQUE` as:

```text
             UNIQUE
                |
       "Can another row
        have this value?"
                |
        +-------+-------+
        |               |
       YES              NO
        |               |
      allow             ERROR
```

For composite uniqueness:

```text
UNIQUE (A, B)
```

means:

```text
The combination (A, B) cannot repeat.
```

Not:

```text
A cannot repeat
B cannot repeat
```

---

# Key takeaways

1. `UNIQUE` prevents duplicate values.
2. It can apply to one column or multiple columns.
3. `UNIQUE (a, b)` makes the **combination** unique.
4. Normal PostgreSQL `UNIQUE` constraints allow multiple `NULL` values.
5. `NOT NULL + UNIQUE` is a common combination when a value is mandatory and unique.
6. `UNIQUE` is enforced during both `INSERT` and `UPDATE`.
7. PostgreSQL uses a unique index to enforce uniqueness.
8. A database constraint is important even when the application performs validation.
9. Multiple `UNIQUE` constraints can exist in one table.
10. `PRIMARY KEY` and `UNIQUE` are related, but they have different semantics.

## Next concept

**`PRIMARY KEY`** — why it is more than just `UNIQUE + NOT NULL`, how primary keys identify rows, single vs composite primary keys, and why a table can have only one primary key.

