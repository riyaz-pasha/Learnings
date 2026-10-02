# 7. `DEFAULT` Constraint in PostgreSQL

## 1. What is a `DEFAULT` constraint?

A `DEFAULT` specifies **what value PostgreSQL should automatically use when an `INSERT` does not provide a value for that column**.

Simple definition:

> `DEFAULT` provides an automatic value when a column is omitted from an `INSERT`.

For example:

```sql
CREATE TABLE users (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name TEXT NOT NULL,
    is_active BOOLEAN DEFAULT TRUE
);
```

Now:

```sql
INSERT INTO users (name)
VALUES ('Riyaz');
```

PostgreSQL automatically stores:

```text
id        → generated automatically
name      → Riyaz
is_active → TRUE
```

---

# 2. Why do we need `DEFAULT`?

Imagine a table:

```sql
CREATE TABLE orders (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    status TEXT,
    created_at TIMESTAMP,
    quantity INT
);
```

Every time you insert an order, you might have to explicitly provide:

```sql
INSERT INTO orders (status, created_at, quantity)
VALUES ('PENDING', CURRENT_TIMESTAMP, 1);
```

But `status` and `created_at` usually have sensible initial values.

We can encode those rules in the database:

```sql
CREATE TABLE orders (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    status TEXT DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    quantity INT DEFAULT 1
);
```

Now:

```sql
INSERT INTO orders DEFAULT VALUES;
```

can produce something conceptually like:

```text
id          status      created_at              quantity
--------------------------------------------------------
1           PENDING     2026-10-02 15:30:00     1
```

The database supplies the missing values.

---

# 3. The most important distinction: omitted vs `NULL`

This is one of the most important things to understand.

Consider:

```sql
CREATE TABLE users (
    name TEXT,
    is_active BOOLEAN DEFAULT TRUE
);
```

### Case 1: Column is omitted

```sql
INSERT INTO users (name)
VALUES ('Alice');
```

Result:

```text
name   = Alice
is_active = TRUE
```

The `DEFAULT` is used.

---

### Case 2: Explicitly provide `NULL`

```sql
INSERT INTO users (name, is_active)
VALUES ('Bob', NULL);
```

Result:

```text
name   = Bob
is_active = NULL
```

The default is **not** used.

So:

```text
column omitted
       ↓
DEFAULT may be applied

column explicitly given NULL
       ↓
NULL is stored
```

This is a very important distinction.

---

# 4. `DEFAULT` does NOT mean `NOT NULL`

Consider:

```sql
CREATE TABLE users (
    is_active BOOLEAN DEFAULT TRUE
);
```

This allows:

```sql
INSERT INTO users DEFAULT VALUES;
```

Result:

```text
is_active = TRUE
```

But this is also allowed:

```sql
INSERT INTO users (is_active)
VALUES (NULL);
```

because the column is nullable.

If you want:

> Every user must have a value, and if the application doesn't provide one, use `TRUE`.

Use:

```sql
CREATE TABLE users (
    is_active BOOLEAN NOT NULL DEFAULT TRUE
);
```

Now:

```sql
INSERT INTO users DEFAULT VALUES;
```

works:

```text
is_active = TRUE
```

But:

```sql
INSERT INTO users (is_active)
VALUES (NULL);
```

fails.

So:

```text
DEFAULT
   ↓
What should happen when the value is omitted?

NOT NULL
   ↓
Is NULL allowed?

NOT NULL + DEFAULT
   ↓
Value is required, but database can automatically provide it
```

---

# 5. `DEFAULT` is not a validation rule

This is another important distinction.

Suppose:

```sql
CREATE TABLE products (
    price NUMERIC DEFAULT 0
);
```

This does **not** mean:

> Price must be >= 0.

It only means:

> If price is omitted, use `0`.

You could still do:

```sql
INSERT INTO products (price)
VALUES (-100);
```

That is allowed.

If you want to enforce the business rule:

```sql
CREATE TABLE products (
    price NUMERIC NOT NULL DEFAULT 0
        CHECK (price >= 0)
);
```

Now we have three separate responsibilities:

```text
DEFAULT
→ supplies a value

NOT NULL
→ prevents absence

CHECK
→ validates the value
```

This separation is extremely useful when designing schemas.

---

# 6. Common types of defaults

Defaults can be constants.

### String

```sql
status TEXT DEFAULT 'PENDING'
```

### Number

```sql
quantity INT DEFAULT 1
```

### Boolean

```sql
is_active BOOLEAN DEFAULT TRUE
```

### Date/time

```sql
created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
```

### Numeric expression

```sql
total NUMERIC DEFAULT 0
```

Defaults can also use PostgreSQL functions and expressions where appropriate.

For example:

```sql
created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
```

---

# 7. `CURRENT_TIMESTAMP`

A very common production use case is timestamps.

```sql
CREATE TABLE orders (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

Then:

```sql
INSERT INTO orders DEFAULT VALUES;
```

PostgreSQL generates the creation timestamp.

This is often preferable to relying entirely on application code because the database itself knows when the row was inserted.

---

# 8. `DEFAULT` does not continuously update the value

This is a common misconception.

Suppose:

```sql
created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
```

You insert:

```sql
INSERT INTO orders DEFAULT VALUES;
```

The default is evaluated for that insert.

Later:

```sql
UPDATE orders
SET status = 'COMPLETED'
WHERE id = 1;
```

`created_at` does **not** automatically change.

It remains the original creation time.

So:

```text
DEFAULT
   ↓
Applied when a value is omitted during INSERT
```

It is not:

```text
DEFAULT
   ↓
Keep updating the column automatically
```

If you need `updated_at` to change automatically whenever a row is updated, that requires a different mechanism, commonly a trigger or application logic.

---

# 9. `DEFAULT` during `UPDATE`

This is interesting.

Suppose:

```sql
CREATE TABLE users (
    name TEXT,
    status TEXT DEFAULT 'ACTIVE'
);
```

You can explicitly request the default during an update:

```sql
UPDATE users
SET status = DEFAULT
WHERE name = 'Alice';
```

PostgreSQL replaces the current value with the column's default.

So `DEFAULT` can appear in SQL statements explicitly.

For example:

```sql
INSERT INTO users (name, status)
VALUES ('Alice', DEFAULT);
```

And:

```sql
UPDATE users
SET status = DEFAULT
WHERE name = 'Alice';
```

---

# 10. `INSERT ... DEFAULT VALUES`

PostgreSQL also supports:

```sql
INSERT INTO table_name DEFAULT VALUES;
```

Example:

```sql
CREATE TABLE system_settings (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

Then:

```sql
INSERT INTO system_settings DEFAULT VALUES;
```

PostgreSQL fills the columns using their defaults.

Result:

```text
id   enabled   created_at
-------------------------------
1    TRUE      current timestamp
```

This is useful when almost all initial values are database-defined.

---

# 11. Adding a `DEFAULT` to an existing column

Suppose we already have:

```sql
CREATE TABLE users (
    id BIGINT PRIMARY KEY,
    is_active BOOLEAN
);
```

We can add a default:

```sql
ALTER TABLE users
ALTER COLUMN is_active
SET DEFAULT TRUE;
```

Now future inserts that omit `is_active` receive:

```text
TRUE
```

---

# 12. Removing a `DEFAULT`

You can remove it:

```sql
ALTER TABLE users
ALTER COLUMN is_active
DROP DEFAULT;
```

Important:

> Dropping a default does not modify existing rows.

Suppose we have:

```text
id   is_active
--------------
1    TRUE
2    TRUE
```

After:

```sql
ALTER TABLE users
ALTER COLUMN is_active
DROP DEFAULT;
```

the existing values remain:

```text
id   is_active
--------------
1    TRUE
2    TRUE
```

Only future inserts are affected.

---

# 13. `DEFAULT` and existing data

This is another important distinction.

Suppose:

```sql
CREATE TABLE users (
    id BIGINT PRIMARY KEY,
    is_active BOOLEAN
);
```

Existing data:

```text
id   is_active
--------------
1    NULL
2    NULL
3    FALSE
```

Now:

```sql
ALTER TABLE users
ALTER COLUMN is_active
SET DEFAULT TRUE;
```

The existing rows **do not become `TRUE`**.

They remain:

```text
id   is_active
--------------
1    NULL
2    NULL
3    FALSE
```

The default only applies when a future operation needs the default.

If you want existing rows changed:

```sql
UPDATE users
SET is_active = TRUE
WHERE is_active IS NULL;
```

Then add:

```sql
ALTER TABLE users
ALTER COLUMN is_active SET DEFAULT TRUE;
```

And if the column should never be null:

```sql
ALTER TABLE users
ALTER COLUMN is_active SET NOT NULL;
```

---

# 14. `DEFAULT` + `NOT NULL` + `CHECK`

Let's build a realistic example.

```sql
CREATE TABLE products (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,

    name TEXT NOT NULL,

    price NUMERIC(10,2)
        NOT NULL
        DEFAULT 0
        CHECK (price >= 0),

    quantity INT
        NOT NULL
        DEFAULT 1
        CHECK (quantity > 0),

    status TEXT
        NOT NULL
        DEFAULT 'ACTIVE'
        CHECK (status IN ('ACTIVE', 'INACTIVE'))
);
```

Each constraint has a different job.

### `name`

```sql
name TEXT NOT NULL
```

Means:

> Product must have a name.

### `price`

```sql
price NUMERIC(10,2)
    NOT NULL
    DEFAULT 0
    CHECK (price >= 0)
```

Means:

```text
omitted?
    → 0

NULL?
    → rejected

negative?
    → rejected
```

### `quantity`

```sql
quantity INT
    NOT NULL
    DEFAULT 1
    CHECK (quantity > 0)
```

Means:

```text
omitted?
    → 1

NULL?
    → rejected

0 or negative?
    → rejected
```

### `status`

```sql
status TEXT
    NOT NULL
    DEFAULT 'ACTIVE'
    CHECK (status IN ('ACTIVE', 'INACTIVE'))
```

Means:

```text
omitted?
    → ACTIVE

NULL?
    → rejected

UNKNOWN status?
    → rejected
```

This is how multiple integrity constraints work together.

---

# 15. Application default vs database default

Suppose your Java application does this:

```java
user.setActive(true);
```

That is an **application-level default**.

But:

```sql
is_active BOOLEAN DEFAULT TRUE
```

is a **database-level default**.

Why might the database default matter?

Imagine there are multiple ways data can enter your database:

```text
                    ┌── Java API
                    │
                    ├── Admin script
                    │
                    ├── Background job
                    │
                    ├── Data migration
                    │
                    └── Another service
                           ↓
                       DATABASE
```

If only the Java application knows:

```text
is_active = TRUE
```

another path could forget to provide it.

With:

```sql
DEFAULT TRUE
```

the database provides the fallback regardless of which application writes the row.

---

# 16. But don't put every business rule into `DEFAULT`

A default should represent a **sensible automatic initial value**.

Good:

```sql
status TEXT DEFAULT 'PENDING'
```

Good:

```sql
is_active BOOLEAN DEFAULT TRUE
```

Good:

```sql
created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
```

Potentially problematic:

```sql
discount NUMERIC DEFAULT 17.35
```

unless `17.35` genuinely represents the business rule.

The question should be:

> "If the caller doesn't provide this value, is there an objectively sensible value the database should use?"

If yes, `DEFAULT` may be appropriate.

---

# 17. `DEFAULT` and generated columns are different

Don't confuse:

```sql
DEFAULT
```

with:

```sql
GENERATED ALWAYS AS
```

For example:

```sql
price NUMERIC DEFAULT 100
```

means:

> If price isn't supplied, use 100.

Whereas a generated column might be:

```sql
total NUMERIC GENERATED ALWAYS AS (price * quantity) STORED
```

means:

> Calculate `total` from other columns.

Conceptually:

```text
DEFAULT
   ↓
Fallback value

GENERATED COLUMN
   ↓
Calculated value
```

---

# 18. `DEFAULT` and identity columns

You will often see:

```sql
id BIGINT GENERATED ALWAYS AS IDENTITY
```

This is related to automatic value generation, but identity columns are not simply the same thing as a normal `DEFAULT`.

For example:

```sql
CREATE TABLE users (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name TEXT NOT NULL
);
```

You don't normally write:

```sql
INSERT INTO users (id, name)
VALUES (1, 'Alice');
```

Instead:

```sql
INSERT INTO users (name)
VALUES ('Alice');
```

PostgreSQL generates the ID.

Think of them as different concepts:

```text
DEFAULT
→ use a fallback expression when omitted

IDENTITY
→ generate a unique sequence-backed value for the column
```

---

# 19. A subtle point: `DEFAULT` is not a constraint in exactly the same sense

You'll often hear "`DEFAULT` constraint," but technically PostgreSQL treats a default as a **column default**, rather than a constraint like `PRIMARY KEY`, `FOREIGN KEY`, `CHECK`, or `UNIQUE`.

Why does this matter?

Because constraints generally **reject invalid data**.

A default doesn't reject anything.

Instead, it says:

> "If you don't provide a value, here's what PostgreSQL should use."

This distinction helps you build the correct mental model.

---

# 20. Interview question

### Question

What is the difference between:

```sql
status TEXT DEFAULT 'ACTIVE'
```

and:

```sql
status TEXT NOT NULL DEFAULT 'ACTIVE'
```

### Answer

The first allows `NULL`:

```sql
INSERT INTO users (status)
VALUES (NULL);
```

is valid.

The second rejects `NULL`:

```sql
INSERT INTO users (status)
VALUES (NULL);
```

fails because of `NOT NULL`.

But both use `'ACTIVE'` when the column is omitted:

```sql
INSERT INTO users DEFAULT VALUES;
```

assuming other columns permit it.

---

# 21. The mental model

Remember this:

```text
                 INSERT
                   │
                   ▼
          Did caller provide value?
             /             \
           YES              NO
            │                │
            ▼                ▼
       use provided       use DEFAULT
            │                │
            └───────┬────────┘
                    ▼
             Other constraints
             validate the row
```

For example:

```sql
price NUMERIC
    NOT NULL
    DEFAULT 0
    CHECK (price >= 0)
```

Think:

```text
omitted
   ↓
DEFAULT 0
   ↓
NOT NULL ✓
   ↓
CHECK 0 >= 0 ✓
   ↓
INSERT succeeds
```

But:

```sql
price = -10
```

means:

```text
provided -10
   ↓
DEFAULT not used
   ↓
NOT NULL ✓
   ↓
CHECK -10 >= 0 ✗
   ↓
INSERT fails
```

And:

```sql
price = NULL
```

means:

```text
provided NULL
   ↓
DEFAULT not used
   ↓
NOT NULL ✗
   ↓
INSERT fails
```

---

# Key takeaways

| Feature       | Purpose                       |
| ------------- | ----------------------------- |
| `DEFAULT`     | Supplies a value when omitted |
| `NOT NULL`    | Prevents `NULL`               |
| `CHECK`       | Validates a condition         |
| `UNIQUE`      | Prevents duplicate values     |
| `PRIMARY KEY` | Identifies a row              |
| `FOREIGN KEY` | Maintains relationships       |

The most important rule:

> **`DEFAULT` applies to an omitted value, not to an explicitly supplied `NULL`.**

And:

> **`DEFAULT` provides a value; it does not validate the value.**

## Next concept

**8. `PRIMARY KEY` vs `UNIQUE`**

