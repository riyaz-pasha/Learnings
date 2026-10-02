# 18. PostgreSQL-Specific Advanced Constraints

Now let's move from the standard SQL constraints into some features that are particularly useful in **PostgreSQL**.

The goal is not to memorize every PostgreSQL feature. It's to understand:

> **What kinds of integrity rules can PostgreSQL enforce beyond basic column constraints?**

---

## 1. PostgreSQL gives us more than the basic constraints

So far we've seen:

```text
NOT NULL
UNIQUE
PRIMARY KEY
FOREIGN KEY
CHECK
DEFAULT
```

PostgreSQL additionally gives us powerful mechanisms such as:

* `EXCLUDE` / exclusion constraints
* partial unique indexes
* expression indexes
* `NULLS NOT DISTINCT`
* generated columns
* domains
* custom data types
* range types
* identity columns
* deferrable unique/primary-key constraints

Some are technically constraints, while others are **index/type/schema features that can enforce integrity rules**.

That distinction is important.

---

# 2. Partial unique indexes

Suppose we have users:

```sql
CREATE TABLE users (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    email TEXT NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE
);
```

Business rule:

> An email must be unique among active users.

A normal constraint:

```sql
UNIQUE (email)
```

would make the email unique across **all** users.

But perhaps we want:

```text
Active users:
john@example.com   ← allowed

Inactive users:
john@example.com   ← also allowed
```

We can use a partial unique index:

```sql
CREATE UNIQUE INDEX uq_active_users_email
ON users (email)
WHERE is_active = TRUE;
```

Now uniqueness applies only to rows satisfying:

```sql
is_active = TRUE
```

---

## 3. Why is this useful?

This pattern appears frequently in real systems.

### Example: one active subscription

```sql
CREATE UNIQUE INDEX uq_active_subscription
ON subscriptions (user_id)
WHERE status = 'ACTIVE';
```

This means:

```text
User 10 → ACTIVE subscription
User 10 → ACTIVE subscription
```

is not allowed.

But:

```text
User 10 → CANCELLED
User 10 → CANCELLED
User 10 → ACTIVE
```

is allowed.

The database is enforcing a **conditional uniqueness rule**.

---

# 4. Expression-based uniqueness

PostgreSQL can also enforce uniqueness on an expression.

Suppose emails should be treated case-insensitively:

```text
John@example.com
john@example.com
JOHN@EXAMPLE.COM
```

should all represent the same email.

One approach is:

```sql
CREATE UNIQUE INDEX uq_users_email_lower
ON users (LOWER(email));
```

Now PostgreSQL effectively compares:

```text
LOWER(email)
```

rather than the raw value.

So:

```text
John@example.com
john@example.com
```

produce the same indexed value.

The second insert conflicts.

---

# 5. `UNIQUE NULLS NOT DISTINCT`

We previously saw that PostgreSQL's normal `UNIQUE` behavior allows multiple `NULL`s.

For example:

```sql
CREATE TABLE users (
    id BIGINT PRIMARY KEY,
    phone TEXT UNIQUE
);
```

These are normally allowed:

```text
id   phone
1    NULL
2    NULL
3    NULL
```

Because SQL normally treats `NULL` as "unknown".

PostgreSQL provides:

```sql
UNIQUE NULLS NOT DISTINCT (phone)
```

Now:

```text
NULL
NULL
```

are treated as duplicates for uniqueness purposes.

So only one `NULL` is allowed.

This is useful when your business rule says:

> There can be at most one row with no value.

---

# 6. Generated columns

Generated columns aren't constraints themselves, but they're an important PostgreSQL integrity feature.

Suppose we have:

```sql
CREATE TABLE products (
    id BIGINT PRIMARY KEY,
    price NUMERIC(10,2),
    quantity INTEGER,
    total NUMERIC(12,2)
        GENERATED ALWAYS AS (price * quantity) STORED
);
```

Now the database calculates:

```text
total = price × quantity
```

The application doesn't need to manually maintain it.

For example:

```text
price     = 100
quantity  = 3
total     = 300
```

If the application tries to supply:

```sql
INSERT INTO products(price, quantity, total)
VALUES (100, 3, 999);
```

PostgreSQL won't allow manually overriding the generated value.

This prevents inconsistent derived data.

---

# 7. Domains

A **domain** lets you create a reusable data type with constraints.

For example:

```sql
CREATE DOMAIN positive_price AS NUMERIC(10,2)
CHECK (VALUE >= 0);
```

Now we can use it:

```sql
CREATE TABLE products (
    id BIGINT PRIMARY KEY,
    price positive_price NOT NULL
);
```

The rule:

```text
price >= 0
```

is now associated with the domain.

You can reuse it across many tables:

```sql
CREATE TABLE products (
    price positive_price
);

CREATE TABLE order_items (
    price positive_price
);

CREATE TABLE invoices (
    amount positive_price
);
```

This is useful when the **same validation rule represents a reusable data concept**.

---

# 8. Range types

PostgreSQL has built-in range types.

For example:

```sql
daterange
```

represents a range of dates.

Instead of storing:

```text
start_date
end_date
```

you can represent:

```text
[2026-10-01, 2026-10-10)
```

as a single range value.

PostgreSQL provides range types such as:

```text
int4range
int8range
numrange
tsrange
tstzrange
daterange
```

This becomes especially powerful when combined with exclusion constraints.

---

# 9. Exclusion constraints

This is our next major concept and deserves its own lesson.

Imagine hotel bookings.

We don't want:

```text
Room 101
Booking A → Oct 1 - Oct 5

Room 101
Booking B → Oct 3 - Oct 7
```

because the date ranges overlap.

A normal:

```sql
UNIQUE(room_id, start_date, end_date)
```

doesn't solve this.

Why?

Because:

```text
(room 101, Oct 1, Oct 5)
(room 101, Oct 3, Oct 7)
```

are different combinations.

We need a different kind of constraint:

> **For the same room, booking periods must not overlap.**

PostgreSQL's `EXCLUDE` constraint is designed for this type of rule.

We'll cover it separately in the next concept.

---

# 10. Identity columns

We've already used:

```sql
id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY
```

The important distinction is:

```text
IDENTITY
    ↓
Automatically generates a value

PRIMARY KEY
    ↓
Ensures that value identifies a row uniquely
```

These are separate concepts.

For example:

```sql
id BIGINT GENERATED ALWAYS AS IDENTITY
```

does **not** automatically mean:

```text
PRIMARY KEY
```

You can combine them:

```sql
id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY
```

which is the common design.

---

# 11. `CHECK` + PostgreSQL types

PostgreSQL's rich types can sometimes reduce the need for complicated checks.

For example, instead of storing a status as arbitrary text:

```sql
status TEXT
CHECK (status IN ('PENDING', 'PAID', 'CANCELLED'))
```

PostgreSQL also supports enumerated types:

```sql
CREATE TYPE order_status AS ENUM (
    'PENDING',
    'PAID',
    'CANCELLED'
);
```

Then:

```sql
CREATE TABLE orders (
    id BIGINT PRIMARY KEY,
    status order_status NOT NULL
);
```

Now PostgreSQL itself restricts the value to the defined enum values.

However, enums have schema-evolution considerations, so `CHECK` constraints are often preferred when statuses change frequently.

---

# 12. Constraint vs index

This distinction is important.

Consider:

```sql
UNIQUE (email)
```

versus:

```sql
CREATE UNIQUE INDEX ...
```

They can sometimes enforce similar uniqueness behavior, but conceptually:

### Constraint

Represents a **data-integrity rule**.

```sql
CONSTRAINT uq_users_email
UNIQUE (email)
```

### Index

Primarily represents a **data-access structure**, though a unique index can enforce uniqueness.

```sql
CREATE UNIQUE INDEX ...
```

For straightforward business uniqueness, prefer a `UNIQUE` constraint.

Use a unique index when you need capabilities such as:

```text
partial uniqueness
expression-based uniqueness
```

that aren't directly expressed as a normal table constraint.

---

# 13. A useful design hierarchy

When designing a PostgreSQL schema, think roughly like this:

```text
Can this be represented as NOT NULL?
        ↓
Can this be represented as UNIQUE?
        ↓
Can this be represented as FOREIGN KEY?
        ↓
Can this be represented as CHECK?
        ↓
Can a partial/expression index enforce it?
        ↓
Can an EXCLUDE constraint enforce it?
        ↓
Do we need triggers/application/domain logic?
```

The exact choice depends on the rule, but this is a useful way to think.

---

# 14. Real-world example

Suppose we're designing a subscription system:

```sql
CREATE TABLE subscriptions (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,

    user_id BIGINT NOT NULL,

    email TEXT NOT NULL,

    status TEXT NOT NULL
        CHECK (status IN ('ACTIVE', 'CANCELLED')),

    created_at TIMESTAMPTZ NOT NULL
        DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_subscription_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
);
```

Then perhaps:

```sql
CREATE UNIQUE INDEX uq_active_subscription_user
ON subscriptions (user_id)
WHERE status = 'ACTIVE';
```

We now have several layers:

```text
PRIMARY KEY
    ↓
subscription identity

NOT NULL
    ↓
required values

CHECK
    ↓
valid status

FOREIGN KEY
    ↓
valid user relationship

DEFAULT
    ↓
automatic timestamp

PARTIAL UNIQUE INDEX
    ↓
only one active subscription per user
```

This is a good example of using different database mechanisms for different invariants.

---

# 15. Important PostgreSQL-specific lesson

Don't ask:

> "Which constraint should I use?"

Instead ask:

> **"What invariant does the business require?"**

For example:

```text
Email must exist
        ↓
NOT NULL

Email must be unique
        ↓
UNIQUE

User must exist
        ↓
FOREIGN KEY

Price cannot be negative
        ↓
CHECK

Only active record can be unique
        ↓
Partial UNIQUE INDEX

Values must not overlap
        ↓
EXCLUDE
```

This way, schema design becomes much easier.

---

# Mental model

Think of PostgreSQL as giving you increasingly powerful ways to describe invariants:

```text
Simple value rule
       ↓
NOT NULL / CHECK
       ↓
Uniqueness
       ↓
UNIQUE / partial UNIQUE
       ↓
Relationships
       ↓
FOREIGN KEY
       ↓
Complex spatial/time relationship
       ↓
EXCLUDE
```

The key idea for this lesson:

> **PostgreSQL provides advanced integrity mechanisms beyond the basic SQL constraints, allowing you to express conditional uniqueness, expression-based rules, reusable constrained types, generated values, ranges, and non-overlapping relationships.**

---

## Next

**19. Exclusion Constraints (`EXCLUDE`)**

We'll go deep into the particularly powerful PostgreSQL rule:

> **"These rows must not conflict with each other."**

We'll use **room bookings, employee schedules, reservations, and time ranges** to understand why `UNIQUE` isn't enough and when `EXCLUDE` is the right tool.

