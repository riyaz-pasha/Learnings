# 14. Constraint Naming and Database Design Practices

So far, we've learned what the different constraints do.

Now let's talk about something that looks small but becomes **very important in real production databases**:

> **Giving constraints meaningful names and designing them consistently.**

---

# 1. Why name constraints?

Consider:

```sql
CREATE TABLE products (
    id BIGINT PRIMARY KEY,
    price NUMERIC(10,2) CHECK (price >= 0),
    sku TEXT UNIQUE
);
```

This works perfectly.

But PostgreSQL has to give the constraints system-generated names.

For example, you may end up seeing errors involving names such as:

```text
products_pkey
products_price_check
products_sku_key
```

Sometimes those are understandable.

But as schemas become larger, automatically generated names can become less useful.

Explicit names make the schema much easier to understand and maintain.

---

# 2. Naming a `PRIMARY KEY`

Instead of:

```sql
CREATE TABLE users (
    id BIGINT PRIMARY KEY
);
```

you can write:

```sql
CREATE TABLE users (
    id BIGINT,

    CONSTRAINT pk_users
        PRIMARY KEY (id)
);
```

Now the constraint has an intentional name:

```text
pk_users
```

---

# 3. Naming a `FOREIGN KEY`

Instead of:

```sql
CREATE TABLE orders (
    id BIGINT PRIMARY KEY,
    customer_id BIGINT REFERENCES customers(id)
);
```

use:

```sql
CREATE TABLE orders (
    id BIGINT PRIMARY KEY,
    customer_id BIGINT,

    CONSTRAINT fk_orders_customer
        FOREIGN KEY (customer_id)
        REFERENCES customers(id)
);
```

Now the relationship is obvious:

```text
fk_orders_customer
       │
       ├── FK
       ├── orders
       └── customer
```

This becomes particularly useful when there are multiple foreign keys.

---

# 4. Multiple foreign keys

Consider:

```sql
CREATE TABLE payments (
    id BIGINT PRIMARY KEY,

    customer_id BIGINT,
    processed_by BIGINT,

    CONSTRAINT fk_payments_customer
        FOREIGN KEY (customer_id)
        REFERENCES customers(id),

    CONSTRAINT fk_payments_processor
        FOREIGN KEY (processed_by)
        REFERENCES employees(id)
);
```

Now imagine a foreign-key violation.

Knowing:

```text
fk_payments_processor
```

immediately tells us which relationship is involved.

Without meaningful names, debugging can be more difficult.

---

# 5. Naming `UNIQUE` constraints

Example:

```sql
CREATE TABLE users (
    id BIGINT PRIMARY KEY,

    email TEXT NOT NULL,

    CONSTRAINT uq_users_email
        UNIQUE (email)
);
```

A common convention is:

```text
uq_<table>_<column(s)>
```

For example:

```text
uq_users_email
uq_users_username
uq_orders_external_id
```

---

# 6. Naming composite `UNIQUE`

Suppose usernames are unique within an organization:

```sql
CREATE TABLE users (
    id BIGINT PRIMARY KEY,

    organization_id BIGINT NOT NULL,
    username TEXT NOT NULL,

    CONSTRAINT uq_users_organization_username
        UNIQUE (organization_id, username)
);
```

The name describes the actual business rule:

```text
organization + username must be unique
```

That's much more useful than something generic like:

```text
unique_constraint_1
```

---

# 7. Naming `CHECK` constraints

Suppose:

```sql
CREATE TABLE products (
    id BIGINT PRIMARY KEY,

    price NUMERIC(10,2),
    discount_percent NUMERIC(5,2),

    CONSTRAINT chk_products_price_non_negative
        CHECK (price >= 0),

    CONSTRAINT chk_products_discount_valid
        CHECK (discount_percent BETWEEN 0 AND 100)
);
```

The names explain the invariant:

```text
chk_products_price_non_negative

chk_products_discount_valid
```

When something fails, you can quickly understand what rule was violated.

---

# 8. Naming conventions

There isn't one universal PostgreSQL naming convention.

The important thing is:

> **Pick a convention and use it consistently.**

A common convention is:

| Constraint  | Prefix  | Example                           |
| ----------- | ------- | --------------------------------- |
| Primary key | `pk_`   | `pk_users`                        |
| Foreign key | `fk_`   | `fk_orders_customer`              |
| Unique      | `uq_`   | `uq_users_email`                  |
| Check       | `chk_`  | `chk_products_price_non_negative` |
| Exclusion   | `excl_` | `excl_room_booking`               |

For example:

```sql
CONSTRAINT pk_users
    PRIMARY KEY (id)

CONSTRAINT uq_users_email
    UNIQUE (email)

CONSTRAINT fk_orders_customer
    FOREIGN KEY (customer_id)
    REFERENCES customers(id)

CONSTRAINT chk_products_price_non_negative
    CHECK (price >= 0)
```

---

# 9. Why this matters for migrations

Suppose later you want to remove a constraint.

If you know its name:

```sql
ALTER TABLE products
DROP CONSTRAINT chk_products_price_non_negative;
```

That's straightforward.

If the constraint was generated automatically and you don't know its name, you first have to inspect the database to discover it.

Explicit names make migrations more predictable.

---

# 10. Adding named constraints later

You don't have to define constraints during table creation.

For example:

```sql
ALTER TABLE users
ADD CONSTRAINT uq_users_email
UNIQUE (email);
```

Or:

```sql
ALTER TABLE products
ADD CONSTRAINT chk_products_price_non_negative
CHECK (price >= 0);
```

Or:

```sql
ALTER TABLE orders
ADD CONSTRAINT fk_orders_customer
FOREIGN KEY (customer_id)
REFERENCES customers(id);
```

---

# 11. Dropping constraints

Because we know the name:

```sql
ALTER TABLE products
DROP CONSTRAINT chk_products_price_non_negative;
```

Similarly:

```sql
ALTER TABLE users
DROP CONSTRAINT uq_users_email;
```

And:

```sql
ALTER TABLE orders
DROP CONSTRAINT fk_orders_customer;
```

The pattern is:

```sql
ALTER TABLE table_name
DROP CONSTRAINT constraint_name;
```

---

# 12. Don't confuse constraint names with column names

Consider:

```sql
CREATE TABLE users (
    id BIGINT,

    CONSTRAINT pk_users
        PRIMARY KEY (id)
);
```

There are two different things:

```text
Column:
id

Constraint:
pk_users
```

The constraint operates on the column, but it has its own name.

Similarly:

```sql
CONSTRAINT uq_users_email
UNIQUE (email)
```

means:

```text
Column     → email
Constraint → uq_users_email
```

---

# 13. Constraint names should describe the rule

Compare:

```text
constraint_1
```

with:

```text
chk_orders_total_non_negative
```

The second is much more informative.

A good constraint name should answer:

> "What rule does this constraint represent?"

For example:

```text
fk_orders_customer
```

means:

```text
orders.customer_id → customers
```

And:

```text
uq_users_organization_username
```

means:

```text
organization_id + username → unique combination
```

---

# 14. Naming can become especially important with self-references

Consider an employee hierarchy:

```sql
CREATE TABLE employees (
    id BIGINT PRIMARY KEY,
    manager_id BIGINT
);
```

The FK references the same table:

```sql
CONSTRAINT fk_employees_manager
    FOREIGN KEY (manager_id)
    REFERENCES employees(id)
```

The name makes the relationship clear:

```text
employees.manager_id
        │
        ▼
employees.id
```

Without the name, a self-referencing relationship can be harder to understand when inspecting the schema.

---

# 15. Don't over-name everything blindly

Explicit names are useful, but names should remain readable.

Good:

```text
fk_orders_customer
```

Less useful:

```text
fk_orders_customer_id_references_customers_id_primary_key_constraint
```

The goal is:

```text
clear + predictable + reasonably short
```

---

# 16. Database design practice: name the business rule

Here's an important design mindset.

Don't just think:

```text
"I need a CHECK."
```

Think:

```text
"What invariant am I protecting?"
```

For example:

Business requirement:

> Product price cannot be negative.

Database design:

```sql
CONSTRAINT chk_products_price_non_negative
CHECK (price >= 0)
```

Business requirement:

> An employee code must be unique within a department.

Database design:

```sql
CONSTRAINT uq_employees_department_code
UNIQUE (department_id, employee_code)
```

Business requirement:

> Every order must belong to an existing customer.

Database design:

```sql
CONSTRAINT fk_orders_customer
FOREIGN KEY (customer_id)
REFERENCES customers(id)
```

This connection between:

```text
Business rule
      ↓
Database invariant
      ↓
Constraint
```

is an important database-design skill.

---

# 17. A production-style example

Let's combine what we've learned.

```sql
CREATE TABLE customers (
    id BIGINT GENERATED ALWAYS AS IDENTITY,

    email TEXT NOT NULL,

    CONSTRAINT pk_customers
        PRIMARY KEY (id),

    CONSTRAINT uq_customers_email
        UNIQUE (email)
);
```

Then:

```sql
CREATE TABLE orders (
    id BIGINT GENERATED ALWAYS AS IDENTITY,

    customer_id BIGINT NOT NULL,

    total_amount NUMERIC(12,2) NOT NULL,

    status TEXT NOT NULL,

    CONSTRAINT pk_orders
        PRIMARY KEY (id),

    CONSTRAINT fk_orders_customer
        FOREIGN KEY (customer_id)
        REFERENCES customers(id),

    CONSTRAINT chk_orders_total_non_negative
        CHECK (total_amount >= 0),

    CONSTRAINT chk_orders_status_valid
        CHECK (
            status IN (
                'PENDING',
                'PAID',
                'SHIPPED',
                'CANCELLED'
            )
        )
);
```

Now the schema communicates its rules very clearly.

```text
customers
│
├── pk_customers
└── uq_customers_email

orders
│
├── pk_orders
├── fk_orders_customer
├── chk_orders_total_non_negative
└── chk_orders_status_valid
```

---

# 18. One important PostgreSQL detail

Constraint names are scoped to the relevant database object, but you should still aim for names that are easy to identify globally.

For example, if your organization has many schemas and migration tooling, this is clearer:

```text
fk_orders_customer
```

than:

```text
fk_customer
```

because the table involved is immediately apparent.

---

# 19. Naming and error handling

Suppose an insert violates:

```sql
CONSTRAINT chk_orders_total_non_negative
CHECK (total_amount >= 0)
```

PostgreSQL can report the constraint name.

Your application can use that information when translating database errors into meaningful API errors.

Conceptually:

```text
Database error
      ↓
chk_orders_total_non_negative
      ↓
Application recognizes constraint
      ↓
Meaningful API error
```

You should be careful not to expose raw database errors directly to end users, but meaningful constraint names are very useful internally.

---

# 20. Should every business rule become a constraint?

Not necessarily.

The key question is:

> Can the database reliably enforce this rule using an appropriate constraint?

For example:

### Good constraint candidates

```text
price >= 0
quantity > 0
email unique
order must reference customer
start_time < end_time
```

### More complex rules

```text
Customer cannot have more than 3 active orders.
A room cannot have overlapping bookings.
Only one active subscription per customer.
A payment cannot exceed the total amount of the order.
```

Some of these require:

* unique/partial indexes
* exclusion constraints
* triggers
* transactions
* application logic
* or a combination

We'll get into the appropriate mechanisms later.

---

# 21. Common interview questions

### Q1. Why should constraints be explicitly named?

For:

* readability
* debugging
* migrations
* maintenance
* identifying violated business rules

---

### Q2. Can you rename a constraint?

Yes.

PostgreSQL supports:

```sql
ALTER TABLE users
RENAME CONSTRAINT old_name TO new_name;
```

For example:

```sql
ALTER TABLE users
RENAME CONSTRAINT users_email_key
TO uq_users_email;
```

---

### Q3. Does renaming a constraint change the data?

No.

It changes only the constraint's name.

---

### Q4. Does renaming a constraint change what it enforces?

No.

For example:

```text
Before:
uq_users_email

After:
unique_user_email

Rule:
email must be unique
```

The rule remains the same.

---

### Q5. Is there one mandatory PostgreSQL naming convention?

No.

Consistency within your project is more important than a particular prefix convention.

---

# 22. Recommended mental model

When designing a schema, think in this order:

```text
Business requirement
        ↓
What must ALWAYS be true?
        ↓
Database invariant
        ↓
Which constraint enforces it?
        ↓
Give that constraint a meaningful name
```

For example:

```text
"Every order must belong to a customer"

        ↓

order.customer_id must reference customers.id

        ↓

FOREIGN KEY

        ↓

fk_orders_customer
```

Or:

```text
"Username is unique within a tenant"

        ↓

(tenant_id, username) must be unique

        ↓

UNIQUE

        ↓

uq_users_tenant_username
```

This is the mindset you want to develop rather than simply memorizing SQL syntax.

---

# Mental Model

A constraint is not just a database object.

It represents a **business invariant**.

So ideally:

```text
Constraint name
      ↓
tells you
      ↓
what rule is being protected
```

A practical convention is:

```text
PK    → pk_<table>
FK    → fk_<table>_<referenced_entity>
UNIQUE → uq_<table>_<columns>
CHECK → chk_<table>_<rule>
```

For example:

```sql
CONSTRAINT pk_orders
    PRIMARY KEY (id),

CONSTRAINT fk_orders_customer
    FOREIGN KEY (customer_id)
    REFERENCES customers(id),

CONSTRAINT uq_users_tenant_username
    UNIQUE (tenant_id, username),

CONSTRAINT chk_orders_total_non_negative
    CHECK (total_amount >= 0)
```

### Next concept

**15. Constraints vs Application Validation** — we'll look at exactly what should be validated in the application, what must be enforced by PostgreSQL, why doing only application-side checks can fail under concurrency, and how production systems usually use both.

