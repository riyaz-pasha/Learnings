# 12. `CHECK` Constraints with Real Business Rules

We already learned the basic idea of `CHECK`:

> A `CHECK` constraint ensures that a condition is not `FALSE` for a row.

Now let's go deeper into how `CHECK` is used to enforce **real business rules**.

---

## 1. What is a business rule?

A business rule is a condition that must always be true for your data to make sense.

For example, in an ecommerce system:

* Product price cannot be negative.
* Order quantity must be greater than zero.
* Discount must be between 0 and 100%.
* An order's shipped date cannot be before its order date.
* A booking's end time must be after its start time.

These are excellent candidates for `CHECK`.

```sql
CREATE TABLE products (
    id BIGINT PRIMARY KEY,
    name TEXT NOT NULL,
    price NUMERIC(10,2) CHECK (price >= 0)
);
```

The database now guarantees:

```text
price >= 0
```

regardless of which application or service inserts the data.

---

# 2. Simple business rule

Suppose we have employees:

```sql
CREATE TABLE employees (
    id BIGINT PRIMARY KEY,
    name TEXT NOT NULL,
    age INT CHECK (age >= 18)
);
```

Valid:

```sql
INSERT INTO employees (id, name, age)
VALUES (1, 'Alice', 25);
```

Invalid:

```sql
INSERT INTO employees (id, name, age)
VALUES (2, 'Bob', 15);
```

PostgreSQL rejects it because:

```text
age >= 18
15 >= 18
FALSE
```

---

# 3. Multiple business rules

A table can have multiple `CHECK` constraints.

```sql
CREATE TABLE products (
    id BIGINT PRIMARY KEY,

    name TEXT NOT NULL,

    price NUMERIC(10,2)
        CHECK (price >= 0),

    quantity INT
        CHECK (quantity >= 0),

    discount_percent NUMERIC(5,2)
        CHECK (discount_percent BETWEEN 0 AND 100)
);
```

Conceptually:

```text
Product
 │
 ├── name must exist
 │
 ├── price >= 0
 │
 ├── quantity >= 0
 │
 └── discount between 0 and 100
```

Each rule protects a different invariant.

---

# 4. `CHECK` for allowed values

Suppose an order has a status:

```text
PENDING
PAID
SHIPPED
DELIVERED
CANCELLED
```

You can enforce the allowed values:

```sql
CREATE TABLE orders (
    id BIGINT PRIMARY KEY,

    status TEXT NOT NULL
        CHECK (
            status IN (
                'PENDING',
                'PAID',
                'SHIPPED',
                'DELIVERED',
                'CANCELLED'
            )
        )
);
```

This prevents accidental values such as:

```text
"pendinggg"
"SHIP"
"COMPLETE"
"random"
```

### Why is this useful?

Without the constraint:

```sql
INSERT INTO orders (id, status)
VALUES (1, 'PAIDD');
```

The database might happily store it.

Now your application has to deal with invalid state.

With the `CHECK`:

```text
Database
    ↓
Is status one of the allowed values?
    ↓
     NO
    ↓
Reject
```

---

# 5. `CHECK` across multiple columns

This is where table-level `CHECK` constraints become especially useful.

Consider an event:

```text
start_time
end_time
```

The rule is:

```text
start_time < end_time
```

You can write:

```sql
CREATE TABLE events (
    id BIGINT PRIMARY KEY,
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP NOT NULL,

    CHECK (start_time < end_time)
);
```

Now:

```text
10:00 → 11:00   ✅
10:00 → 10:30   ✅
10:00 → 10:00   ❌
11:00 → 10:00   ❌
```

Notice that this rule involves **two columns**.

That's why a table-level `CHECK` is appropriate.

---

# 6. Real example: bank account

Suppose we have:

```sql
CREATE TABLE bank_accounts (
    id BIGINT PRIMARY KEY,

    account_type TEXT NOT NULL,

    balance NUMERIC(15,2) NOT NULL,

    overdraft_limit NUMERIC(15,2) NOT NULL
);
```

Business rules:

### Rule 1

Balance cannot be below the allowed overdraft.

```text
balance >= -overdraft_limit
```

### Rule 2

Overdraft limit cannot be negative.

```text
overdraft_limit >= 0
```

We can encode both:

```sql
CREATE TABLE bank_accounts (
    id BIGINT PRIMARY KEY,

    account_type TEXT NOT NULL,

    balance NUMERIC(15,2) NOT NULL,

    overdraft_limit NUMERIC(15,2) NOT NULL,

    CHECK (overdraft_limit >= 0),

    CHECK (balance >= -overdraft_limit)
);
```

Example:

```text
balance = -500
overdraft_limit = 1000

-500 >= -1000
TRUE
```

Allowed.

But:

```text
balance = -1500
overdraft_limit = 1000

-1500 >= -1000
FALSE
```

Rejected.

This is a good example of a **cross-column business invariant**.

---

# 7. Real example: ecommerce order item

Suppose:

```sql
CREATE TABLE order_items (
    id BIGINT PRIMARY KEY,

    quantity INT NOT NULL,

    unit_price NUMERIC(10,2) NOT NULL,

    discount_percent NUMERIC(5,2) NOT NULL
);
```

Business rules:

```text
quantity > 0
unit_price >= 0
discount between 0 and 100
```

So:

```sql
CREATE TABLE order_items (
    id BIGINT PRIMARY KEY,

    quantity INT NOT NULL
        CHECK (quantity > 0),

    unit_price NUMERIC(10,2) NOT NULL
        CHECK (unit_price >= 0),

    discount_percent NUMERIC(5,2) NOT NULL
        CHECK (discount_percent BETWEEN 0 AND 100)
);
```

---

# 8. More interesting: start/end ranges

Consider hotel bookings:

```sql
CREATE TABLE bookings (
    id BIGINT PRIMARY KEY,

    check_in DATE NOT NULL,

    check_out DATE NOT NULL,

    CHECK (check_in < check_out)
);
```

This guarantees:

```text
check_in < check_out
```

So:

```text
2026-10-10 → 2026-10-15   ✅
2026-10-10 → 2026-10-10   ❌
2026-10-15 → 2026-10-10   ❌
```

However, notice something important.

This constraint does **not** prevent two different bookings from overlapping.

For example:

```text
Booking A
Oct 10 → Oct 15

Booking B
Oct 12 → Oct 18
```

Both individually satisfy:

```text
check_in < check_out
```

So both pass.

Preventing overlap is a **cross-row** rule, which is a different problem.

We'll eventually see PostgreSQL's **exclusion constraints**, which are designed for cases like this.

---

# 9. The most important `NULL` behavior

Remember this from our earlier lesson:

`CHECK` rejects only when the condition evaluates to:

```text
FALSE
```

It does **not** reject:

```text
UNKNOWN
```

This matters because of `NULL`.

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

But it doesn't.

Why?

SQL evaluates:

```text
NULL >= 18
```

as:

```text
UNKNOWN
```

And `CHECK` allows `UNKNOWN`.

Therefore:

```sql
CHECK (age >= 18)
```

does **not** mean:

> age must exist and be at least 18.

It means:

> If age is present, it cannot violate `age >= 18`.

If age is mandatory:

```sql
CREATE TABLE employees (
    id BIGINT PRIMARY KEY,

    age INT NOT NULL
        CHECK (age >= 18)
);
```

Now:

```text
NULL          ❌
17            ❌
18            ✅
25            ✅
```

This distinction is extremely important in PostgreSQL interviews.

---

# 10. `CHECK` vs `NOT NULL`

Compare:

```sql
age INT NOT NULL
```

with:

```sql
age INT CHECK (age >= 18)
```

They solve different problems.

| Constraint          | Protects against        |
| ------------------- | ----------------------- |
| `NOT NULL`          | Missing value           |
| `CHECK (age >= 18)` | Invalid value           |
| Both                | Missing + invalid value |

Think:

```text
NOT NULL
    ↓
"Must there be a value?"

CHECK
    ↓
"If there is a value, is it valid?"
```

---

# 11. Naming business rules

For production schemas, meaningful constraint names are useful.

Instead of:

```sql
CHECK (price >= 0)
```

you can write:

```sql
CONSTRAINT chk_product_price_non_negative
CHECK (price >= 0)
```

Example:

```sql
CREATE TABLE products (
    id BIGINT PRIMARY KEY,

    price NUMERIC(10,2) NOT NULL,

    discount_percent NUMERIC(5,2) NOT NULL,

    CONSTRAINT chk_product_price_non_negative
        CHECK (price >= 0),

    CONSTRAINT chk_product_discount_valid
        CHECK (discount_percent BETWEEN 0 AND 100)
);
```

Now when PostgreSQL reports a violation, the constraint name communicates what rule was broken.

---

# 12. A subtle design question: one big `CHECK` or multiple?

You could write:

```sql
CHECK (
    price >= 0
    AND quantity >= 0
    AND discount_percent BETWEEN 0 AND 100
)
```

Or:

```sql
CHECK (price >= 0),
CHECK (quantity >= 0),
CHECK (discount_percent BETWEEN 0 AND 100)
```

Generally, separate constraints are easier to understand and maintain:

```sql
CONSTRAINT chk_price_non_negative
CHECK (price >= 0),

CONSTRAINT chk_quantity_non_negative
CHECK (quantity >= 0),

CONSTRAINT chk_discount_valid
CHECK (discount_percent BETWEEN 0 AND 100)
```

Each constraint represents one business invariant.

---

# 13. What `CHECK` is good at

A `CHECK` is excellent when the rule is primarily about the **current row**.

Examples:

```text
price >= 0
quantity > 0
age >= 18
start_time < end_time
discount BETWEEN 0 AND 100
status IN (...)
balance >= -overdraft_limit
```

These are all row-level invariants.

---

# 14. What `CHECK` is NOT good at

Suppose we want:

> A customer cannot have more than 3 active orders.

That's a **cross-row** rule.

A single row cannot determine how many other rows exist.

Similarly:

> A room cannot have two bookings that overlap.

Again, this involves multiple rows.

Or:

> An employee's manager must belong to the same department.

That involves relationships between rows.

These rules require other database mechanisms such as:

* `UNIQUE`
* foreign keys
* exclusion constraints
* triggers
* transactions
* sometimes application logic

The important skill is knowing **which constraint matches which invariant**.

---

# 15. A useful classification

When designing a table, ask:

### Rule about one value?

```text
price >= 0
```

→ `CHECK`

### Rule about whether a value exists?

```text
email must exist
```

→ `NOT NULL`

### Rule about duplication?

```text
email must be unique
```

→ `UNIQUE`

### Rule about identity?

```text
user uniquely identifies a row
```

→ `PRIMARY KEY`

### Rule about relationship?

```text
order.customer_id must reference a customer
```

→ `FOREIGN KEY`

### Rule about multiple rows overlapping?

```text
two bookings cannot overlap
```

→ potentially `EXCLUDE`

This is a very useful database-design mental model.

---

# 16. `CHECK` and application validation

Suppose your API does:

```text
POST /products

price = -10
```

Your application can validate:

```text
price >= 0
```

and return:

```text
400 Bad Request
```

That's good for user experience.

But the database should often enforce the invariant too:

```sql
CHECK (price >= 0)
```

Why?

Because data can enter the database through many paths:

```text
Web API
   │
   ├── Admin API
   ├── Background job
   ├── Migration
   ├── Script
   ├── ETL
   └── Another service
          │
          ▼
       Database
```

The database constraint protects the final boundary.

So a common production approach is:

```text
Application validation
        +
Database constraint
```

Application validation gives a friendly error early.

Database constraints provide authoritative data integrity.

---

# 17. `CHECK` is about invariants

This is the deeper concept.

An **invariant** is something that should remain true for valid database state.

For example:

```text
quantity > 0
```

is an invariant.

Whenever someone attempts:

```sql
INSERT
```

or:

```sql
UPDATE
```

the database checks whether the invariant still holds.

```text
INSERT / UPDATE
       ↓
   CHECK rule
       ↓
   ┌───┴───┐
 TRUE    FALSE
  ↓        ↓
Allow    Reject
```

That's the fundamental purpose of a `CHECK` constraint.

---

# 18. Interview questions

### Q1. Does `CHECK` reject `NULL`?

Not necessarily.

```sql
CHECK (age >= 18)
```

allows `NULL` because:

```text
NULL >= 18 → UNKNOWN
```

and `CHECK` does not reject `UNKNOWN`.

Use:

```sql
age INT NOT NULL CHECK (age >= 18)
```

if `NULL` must also be rejected.

---

### Q2. Can a `CHECK` reference multiple columns?

Yes.

```sql
CHECK (start_time < end_time)
```

is a common example.

---

### Q3. Can a `CHECK` enforce that two rows don't overlap?

A normal `CHECK` is not designed for cross-row constraints.

PostgreSQL has **exclusion constraints** for important cases such as non-overlapping ranges.

---

### Q4. Is `CHECK` better than application validation?

They solve different parts of the problem.

A useful architecture is:

```text
Application validation
        ↓
Better user/API errors

Database CHECK
        ↓
Guaranteed invariant at database boundary
```

---

### Q5. Can I use `CHECK` for enum-like values?

Yes:

```sql
CHECK (status IN ('PENDING', 'PAID', 'CANCELLED'))
```

For larger or more dynamic sets, other modeling approaches may be more appropriate.

---

# Mental Model

Remember `CHECK` as:

> **"For every row, this condition must never be FALSE."**

For example:

```sql
CHECK (price >= 0)
```

means:

```text
price = 100  → TRUE     → ✅
price = 0    → TRUE     → ✅
price = -10  → FALSE    → ❌
price = NULL → UNKNOWN  → ✅
```

And if you need:

```text
value must exist
AND
value must be valid
```

use:

```sql
NOT NULL + CHECK
```

### Next concept

**13. `DEFERRABLE` Constraints** — we'll learn how PostgreSQL can postpone constraint checking until the end of a transaction, and why that is useful for complex inserts/updates.

