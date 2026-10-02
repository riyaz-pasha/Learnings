# Database Integrity Constraints — PostgreSQL

We'll do this as a **concept-by-concept course**, similar to your other learning tracks.

### Course roadmap

1. **What are Integrity Constraints?** ← Start here
2. `NOT NULL`
3. `UNIQUE`
4. `PRIMARY KEY`
5. `FOREIGN KEY`
6. `CHECK`
7. `DEFAULT`
8. `PRIMARY KEY` vs `UNIQUE`
9. Foreign key actions: `CASCADE`, `RESTRICT`, `NO ACTION`, `SET NULL`, `SET DEFAULT`
10. Composite keys and composite constraints
11. Multi-column `UNIQUE`
12. `CHECK` constraints with real business rules
13. `DEFERRABLE` constraints
14. Constraint naming and database design practices
15. Constraints vs application validation
16. Constraints and transactions
17. Constraints under concurrency
18. PostgreSQL-specific advanced constraints
19. Exclusion constraints
20. Real-world schema design + interview questions

We'll move to the next concept only when you say **`next`**.

---

# 1. What are Database Integrity Constraints?

Let's start with the actual problem.

Imagine we have:

```sql
CREATE TABLE users (
    id BIGINT,
    email TEXT,
    age INT
);
```

We can insert:

```sql
INSERT INTO users VALUES (1, 'alice@example.com', 25);
```

Everything looks fine.

But PostgreSQL will also allow:

```sql
INSERT INTO users VALUES (NULL, NULL, -500);
```

And:

```sql
INSERT INTO users VALUES (1, 'alice@example.com', 25);
INSERT INTO users VALUES (1, 'alice@example.com', 25);
INSERT INTO users VALUES (1, 'alice@example.com', 25);
```

The database doesn't inherently know that these values are supposed to follow certain rules.

That's where **integrity constraints** come in.

---

## Actual definition

> **An integrity constraint is a rule enforced by the database that restricts what data can be stored or how rows can relate to each other.**

In simple terms:

> **Constraints are rules that protect the correctness of your database data.**

Think of them as **guards at the database boundary**.

```text
Application
    |
    | INSERT / UPDATE
    v
+-----------------------+
|      PostgreSQL       |
|                       |
|  Constraints          |
|  ├── NOT NULL         |
|  ├── UNIQUE           |
|  ├── PRIMARY KEY      |
|  ├── FOREIGN KEY      |
|  ├── CHECK            |
|  └── DEFAULT          |
|                       |
+-----------------------+
    |
    v
Correct / valid data
```

---

# Why do we need them?

Suppose your application has:

```text
User
-------------------------
id
email
age
```

Business rules might say:

```text
id       → must exist
email    → must be unique
age      → must be >= 18
```

You could enforce these rules only in Java:

```java
if (age < 18) {
    throw new IllegalArgumentException();
}
```

But there is a problem.

What if another application accesses the database?

```text
Spring Boot API
       \
        \
Admin Tool -----> PostgreSQL
        /
Batch Job
       /
Migration Script
```

If the rules exist only in the application, another database client can bypass them.

Database constraints provide a **last line of defense**.

---

# Example

Instead of:

```sql
CREATE TABLE users (
    id BIGINT,
    email TEXT,
    age INT
);
```

we can define:

```sql
CREATE TABLE users (
    id BIGINT PRIMARY KEY,
    email TEXT UNIQUE NOT NULL,
    age INT CHECK (age >= 18)
);
```

Now PostgreSQL itself understands:

```text
id
 ↓
PRIMARY KEY
 ↓
must uniquely identify a row
```

```text
email
 ↓
NOT NULL + UNIQUE
 ↓
must exist and be unique
```

```text
age
 ↓
CHECK
 ↓
must satisfy age >= 18
```

---

# What happens when a constraint is violated?

Suppose:

```sql
INSERT INTO users (id, email, age)
VALUES (1, 'alice@example.com', 25);
```

Works.

Now:

```sql
INSERT INTO users (id, email, age)
VALUES (1, 'bob@example.com', 30);
```

The `PRIMARY KEY` constraint rejects it because:

```text
id = 1
```

already exists.

PostgreSQL doesn't silently fix the data.

It produces an error.

Conceptually:

```text
INSERT
  |
  v
Check constraints
  |
  +---- valid ------> Store row
  |
  +---- invalid -----> ERROR
```

This is important:

> **A constraint is not merely documentation. PostgreSQL actively enforces it.**

---

# The major PostgreSQL integrity constraints

There are several important types.

| Constraint    | Main purpose                             |
| ------------- | ---------------------------------------- |
| `NOT NULL`    | Value must exist                         |
| `UNIQUE`      | Values must not duplicate                |
| `PRIMARY KEY` | Uniquely identifies a row                |
| `FOREIGN KEY` | Maintains relationship between tables    |
| `CHECK`       | Value must satisfy a condition           |
| `DEFAULT`     | Supplies a value when one isn't provided |
| `EXCLUSION`   | Prevents conflicting combinations/ranges |

We'll study each carefully.

---

# A very important distinction

Consider this:

```sql
CREATE TABLE users (
    id BIGINT PRIMARY KEY,
    email TEXT UNIQUE
);
```

There are actually **two different kinds of rules**:

### Entity integrity

Concerned with identifying a row.

```text
PRIMARY KEY
```

Example:

```text
User #1
User #2
User #3
```

Each must have a unique identity.

---

### Referential integrity

Concerned with relationships between tables.

For example:

```text
users
---------
id
  |
  | referenced by
  v
orders
---------
user_id
```

We don't want:

```text
orders.user_id = 999999
```

when user `999999` doesn't exist.

A `FOREIGN KEY` prevents this.

---

# Database constraints vs application validation

This is a very important interview and system-design concept.

Suppose:

```text
Rule:

email must be unique
```

Application:

```java
if (!repository.existsByEmail(email)) {
    repository.save(user);
}
```

This **looks** safe.

But consider two concurrent requests:

```text
Request A                 Request B
---------                 ---------
check email               check email
"not found"               "not found"
save                      save
```

Both requests can potentially pass the check.

This is a classic **race condition**.

A database-level:

```sql
UNIQUE (email)
```

provides the authoritative enforcement at the point where the data is actually written.

So a common production approach is:

```text
Application validation
        +
Database constraints
```

not:

```text
Application validation
        OR
Database constraints
```

Application validation is useful for user experience.

Database constraints protect **data integrity**.

---

# One mental model to remember

Think of your database as having a contract:

```text
                DATABASE
                    |
       +------------+------------+
       |            |            |
    Identity    Relationships   Values
       |            |            |
   PRIMARY KEY  FOREIGN KEY    CHECK
       |                         |
     UNIQUE                   NOT NULL
```

The database says:

> "I will not allow data that violates these rules."

---

# First important principle

### Constraints should represent invariants.

An **invariant** is something that must remain true about your data.

For example:

```text
Every user must have an ID.
```

```text
Every email must be unique.
```

```text
Every order must belong to an existing user.
```

```text
An account balance cannot be negative.
```

```text
An employee's salary must be >= 0.
```

These are excellent candidates for database constraints.

---

## One thing to be careful about

Not every business rule belongs naturally in a simple constraint.

For example:

> "A user can place at most 5 orders per day."

That's a more complex cross-row/business rule and may require additional database mechanisms such as triggers, transactions, locking, or application logic.

We'll get to those distinctions later.

---

### The core idea

If you remember only one thing from this lesson:

> **Integrity constraints are database-enforced rules that prevent invalid data from entering or remaining in the database.**

Next we'll look at **`NOT NULL`**, starting from what `NULL` actually means in PostgreSQL and why `NULL` is more subtle than simply "no value."


