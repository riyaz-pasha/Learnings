# 20. Real-World Schema Design + Interview Questions

This is the final concept in our roadmap.

Now we'll put everything together and use a **real-world system** to answer the most important question:

> **Given a business rule, which database integrity mechanism should enforce it?**

We'll use an **e-commerce/order system** because it gives us examples of almost every constraint we've learned.

---

# 1. Start with the business requirements

Suppose our system has:

* Customers
* Products
* Orders
* Order items
* Payments

Business rules:

1. Every customer has an identity.
2. Customer email is required.
3. Customer email must be unique.
4. Every order belongs to an existing customer.
5. An order must have a valid status.
6. Product price cannot be negative.
7. Order quantity must be positive.
8. The same product shouldn't appear twice in one order.
9. Product prices at the time of purchase must be stored.
10. An order item must reference an existing product.
11. An order should have a creation timestamp automatically.
12. A product SKU must be unique.
13. Certain records may have conditional uniqueness.

Now let's translate those requirements into database constraints.

---

# 2. Customers

```sql
CREATE TABLE customers (
    id BIGINT GENERATED ALWAYS AS IDENTITY,

    email TEXT NOT NULL,

    name TEXT NOT NULL,

    created_at TIMESTAMPTZ NOT NULL
        DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_customers
        PRIMARY KEY (id),

    CONSTRAINT uq_customers_email
        UNIQUE (email)
);
```

Let's identify what each constraint does.

### `PRIMARY KEY`

```sql
PRIMARY KEY (id)
```

Rule:

> Every customer must have a unique identity.

---

### `NOT NULL`

```sql
email TEXT NOT NULL
```

Rule:

> A customer must have an email value.

---

### `UNIQUE`

```sql
UNIQUE (email)
```

Rule:

> Two customers cannot have the same email.

---

### `DEFAULT`

```sql
created_at TIMESTAMPTZ NOT NULL
    DEFAULT CURRENT_TIMESTAMP
```

Rule:

> If the application doesn't provide a creation timestamp, PostgreSQL supplies one.

---

# 3. Products

```sql
CREATE TABLE products (
    id BIGINT GENERATED ALWAYS AS IDENTITY,

    sku TEXT NOT NULL,

    name TEXT NOT NULL,

    price NUMERIC(12,2) NOT NULL,

    CONSTRAINT pk_products
        PRIMARY KEY (id),

    CONSTRAINT uq_products_sku
        UNIQUE (sku),

    CONSTRAINT chk_products_price
        CHECK (price >= 0)
);
```

Now we have:

```text
id
 ↓
PRIMARY KEY
 ↓
product identity

sku
 ↓
UNIQUE
 ↓
business uniqueness

price
 ↓
NOT NULL + CHECK
 ↓
required and non-negative
```

Notice something important:

```sql
CHECK (price >= 0)
```

doesn't replace:

```sql
NOT NULL
```

Because `CHECK` can allow `UNKNOWN` when the value is `NULL`.

Therefore:

```sql
price NUMERIC(12,2) NOT NULL
CHECK (price >= 0)
```

is the correct combination when price is mandatory.

---

# 4. Orders

```sql
CREATE TABLE orders (
    id BIGINT GENERATED ALWAYS AS IDENTITY,

    customer_id BIGINT NOT NULL,

    status TEXT NOT NULL
        DEFAULT 'PENDING',

    created_at TIMESTAMPTZ NOT NULL
        DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_orders
        PRIMARY KEY (id),

    CONSTRAINT fk_orders_customer
        FOREIGN KEY (customer_id)
        REFERENCES customers(id),

    CONSTRAINT chk_orders_status
        CHECK (
            status IN (
                'PENDING',
                'PAID',
                'CANCELLED'
            )
        )
);
```

Now we have three important concepts.

### Foreign key

```sql
FOREIGN KEY (customer_id)
REFERENCES customers(id)
```

Means:

> The customer referenced by an order must exist.

### `NOT NULL`

```sql
customer_id BIGINT NOT NULL
```

Means:

> Every order must have a customer.

Without `NOT NULL`, this would be possible:

```text
order.customer_id = NULL
```

The FK wouldn't reject it.

---

# 5. Order items

Now the interesting part.

An order can contain multiple products:

```text
Order 100
    ├── Product 10 × 2
    ├── Product 20 × 1
    └── Product 30 × 5
```

We can create:

```sql
CREATE TABLE order_items (
    order_id BIGINT NOT NULL,

    product_id BIGINT NOT NULL,

    quantity INTEGER NOT NULL,

    unit_price NUMERIC(12,2) NOT NULL,

    CONSTRAINT pk_order_items
        PRIMARY KEY (order_id, product_id),

    CONSTRAINT fk_order_items_order
        FOREIGN KEY (order_id)
        REFERENCES orders(id),

    CONSTRAINT fk_order_items_product
        FOREIGN KEY (product_id)
        REFERENCES products(id),

    CONSTRAINT chk_order_items_quantity
        CHECK (quantity > 0),

    CONSTRAINT chk_order_items_unit_price
        CHECK (unit_price >= 0)
);
```

Now we've used a **composite primary key**:

```sql
PRIMARY KEY (order_id, product_id)
```

This means:

> A product can appear only once within a particular order.

For example:

```text
(order 100, product 10) → allowed
(order 100, product 20) → allowed
(order 101, product 10) → allowed
```

But:

```text
(order 100, product 10)
(order 100, product 10)
```

is prohibited.

---

# 6. Why store `unit_price`?

Suppose the current product price is:

```text
$100
```

Customer buys it.

Later:

```text
Product price → $150
```

If the order only stores:

```text
product_id = 10
```

we don't know what the customer originally paid.

Therefore the order item stores:

```text
unit_price = 100
```

This isn't primarily an integrity-constraint issue.

It's a **data-modeling decision**.

This is an important lesson:

> Not every business requirement should be solved with a constraint.

---

# 7. Let's map every rule

Here's the complete mapping:

| Business rule                         | Database mechanism       |
| ------------------------------------- | ------------------------ |
| Customer needs identity               | `PRIMARY KEY`            |
| Email required                        | `NOT NULL`               |
| Email unique                          | `UNIQUE`                 |
| Order must reference customer         | `FOREIGN KEY`            |
| Customer relationship mandatory       | `NOT NULL + FOREIGN KEY` |
| Product price non-negative            | `CHECK`                  |
| Order quantity positive               | `CHECK`                  |
| Valid order status                    | `CHECK`                  |
| Automatic creation timestamp          | `DEFAULT`                |
| One product per order                 | Composite `PRIMARY KEY`  |
| Order must reference existing product | `FOREIGN KEY`            |
| Only one active subscription          | Partial `UNIQUE INDEX`   |
| Booking periods cannot overlap        | `EXCLUDE`                |

This is the core skill we're building.

---

# 8. Constraints are about invariants

An **invariant** is something that must remain true for valid database state.

For example:

```text
Customer ID is unique
```

is an invariant.

```text
Order must belong to an existing customer
```

is an invariant.

```text
Quantity > 0
```

is an invariant.

```text
Room bookings must not overlap
```

is an invariant.

Then we choose the appropriate database mechanism.

---

# 9. The constraint decision tree

When designing a schema, ask:

### Question 1

> Can this value be missing?

If no:

```sql
NOT NULL
```

---

### Question 2

> Must this value or combination be unique?

If yes:

```sql
UNIQUE
```

or possibly:

```sql
PRIMARY KEY
```

if it represents row identity.

---

### Question 3

> Must this row reference another row?

If yes:

```sql
FOREIGN KEY
```

---

### Question 4

> Must values satisfy a logical condition?

If yes:

```sql
CHECK
```

---

### Question 5

> Should a missing value receive an automatic value?

If yes:

```sql
DEFAULT
```

---

### Question 6

> Is uniqueness conditional?

Consider:

```sql
CREATE UNIQUE INDEX ...
WHERE ...
```

---

### Question 7

> Must rows not conflict or overlap?

Consider:

```sql
EXCLUDE
```

---

# 10. Constraint vs application logic

This distinction is critical.

Suppose the requirement is:

> "A customer cannot place more than 10 orders per day."

That's not necessarily a simple row-level constraint.

You might need:

* transaction logic
* locking
* application/domain logic
* triggers
* aggregation
* specialized database mechanisms

Similarly:

> "A customer must complete KYC before placing an order."

That's primarily a **workflow/business process** rather than a simple integrity constraint.

Don't force every business rule into `CHECK`.

---

# 11. Constraints vs transactions

Remember our earlier distinction:

```text
Constraint
    ↓
What states are valid?

Transaction
    ↓
Which changes commit together?

Concurrency control
    ↓
What happens when transactions overlap?
```

Example:

```text
Bank transfer

Account A -100
Account B +100
```

A transaction ensures these changes happen atomically.

A constraint might ensure:

```text
balance >= -1000
```

And concurrency control ensures simultaneous transfers are coordinated correctly.

These are different responsibilities.

---

# 12. Constraints vs indexes

Another common interview question.

### Constraint

Represents an integrity rule.

```sql
UNIQUE (email)
```

### Index

Primarily supports efficient access.

```sql
CREATE INDEX idx_users_created_at
ON users(created_at);
```

A unique index can also enforce uniqueness, but a normal index does not.

For example:

```sql
CREATE INDEX idx_users_email
ON users(email);
```

does **not** prevent:

```text
john@example.com
john@example.com
```

A unique index does:

```sql
CREATE UNIQUE INDEX ...
```

But for straightforward business uniqueness, expressing it as:

```sql
UNIQUE
```

usually communicates the intent better.

---

# 13. Important interview question: `PRIMARY KEY` vs `UNIQUE`

### Primary key

```text
Defines row identity.
```

Properties:

* unique
* not null
* one primary-key constraint per table
* commonly referenced by foreign keys

### Unique

```text
Defines additional uniqueness.
```

Properties:

* multiple unique constraints allowed
* normal PostgreSQL unique semantics allow multiple `NULL`s
* can represent business-level uniqueness

Example:

```text
users
-----
id       → PRIMARY KEY
email    → UNIQUE
username → UNIQUE
```

There is one identity:

```text
id
```

but multiple business uniqueness rules.

---

# 14. Important interview question: Why `FOREIGN KEY` doesn't imply `NOT NULL`?

Consider:

```sql
customer_id BIGINT
    REFERENCES customers(id)
```

This means:

> If `customer_id` contains a value, that value must reference an existing customer.

It does **not** mean:

> `customer_id` must contain a value.

Therefore:

```sql
customer_id BIGINT NOT NULL
    REFERENCES customers(id)
```

means:

```text
customer must exist
+
customer_id cannot be NULL
```

---

# 15. Important interview question: Why is `CHECK(age >= 18)` not enough?

Because:

```sql
CHECK (age >= 18)
```

doesn't reject `NULL`.

Conceptually:

```text
age = 20
20 >= 18
→ TRUE
→ allowed

age = 15
15 >= 18
→ FALSE
→ rejected

age = NULL
NULL >= 18
→ UNKNOWN
→ allowed by CHECK
```

If age is required:

```sql
age INTEGER NOT NULL
    CHECK (age >= 18)
```

---

# 16. Important interview question: Why isn't application validation enough?

Suppose:

```text
Application:
    SELECT email
    WHERE email = 'john@example.com'

    if not found:
        INSERT
```

Two concurrent requests can both observe:

```text
not found
```

and both try to insert.

Therefore:

```sql
UNIQUE (email)
```

is the authoritative protection.

The application should still provide friendly error handling.

---

# 17. Important interview question: `NO ACTION` vs `RESTRICT`

They can appear similar:

```sql
ON DELETE NO ACTION
```

and:

```sql
ON DELETE RESTRICT
```

But they differ around deferred constraint checking.

`RESTRICT` prevents the operation immediately and cannot be deferred.

`NO ACTION` checks that referential integrity is satisfied at the appropriate constraint-checking point and can participate in deferred constraints.

The default is:

```sql
ON DELETE NO ACTION
```

---

# 18. Important interview question: Composite key

Suppose:

```sql
PRIMARY KEY (student_id, course_id)
```

Does this mean:

```text
student_id is unique?
```

No.

It means:

```text
(student_id, course_id)
```

as a **combination** is unique.

So this is allowed:

```text
student_id    course_id
-----------   ----------
1             10
1             20
2             10
```

But this isn't:

```text
1    10
1    10
```

---

# 19. Important interview question: Can a foreign key reference `UNIQUE`?

Yes.

A foreign key doesn't necessarily have to reference a primary key.

For example:

```sql
CREATE TABLE users (
    id BIGINT PRIMARY KEY,
    email TEXT UNIQUE
);
```

Another table can reference the unique email:

```sql
CREATE TABLE contacts (
    user_email TEXT
        REFERENCES users(email)
);
```

The referenced column must have appropriate uniqueness semantics.

However, using a stable primary/surrogate identifier is often preferable for relationships because business attributes such as email can change.

---

# 20. Important interview question: What does `CASCADE` actually do?

Consider:

```sql
FOREIGN KEY (customer_id)
REFERENCES customers(id)
ON DELETE CASCADE
```

If:

```text
Customer 10
    ↓
Order 100
    ↓
Order item 500
```

is deleted through cascading relationships, dependent rows can also be deleted according to those configured relationships.

Therefore:

> `CASCADE` is not merely "cleanup."

It changes the lifecycle of related data.

Use it when the child truly has a dependent lifecycle.

---

# 21. Important interview question: Is `DEFAULT` a constraint?

Technically, PostgreSQL treats a default as a **column default**, rather than a constraint in the same category as:

```text
PRIMARY KEY
UNIQUE
FOREIGN KEY
CHECK
```

Its job is different.

```text
NOT NULL
    → prevents missing value

DEFAULT
    → supplies value when omitted
```

For example:

```sql
status TEXT NOT NULL DEFAULT 'PENDING'
```

means:

```text
omitted
   ↓
PENDING

explicit 'PAID'
   ↓
PAID

explicit NULL
   ↓
NOT NULL violation
```

---

# 22. Important interview question: What is `DEFERRABLE`?

It means:

> The database can postpone checking certain constraints until later, commonly until transaction commit.

Example:

```sql
CONSTRAINT fk_employee_manager
FOREIGN KEY (manager_id)
REFERENCES employees(id)
DEFERRABLE INITIALLY DEFERRED
```

Useful when intermediate transaction states may temporarily violate the constraint but the final state is valid.

Remember:

```text
DEFERRABLE ≠ disabled
```

The final valid state is still required.

---

# 23. Important interview question: When would you use `EXCLUDE`?

When the rule is about **conflicts rather than exact duplicates**.

For example:

```text
Same room + overlapping booking period
```

or:

```text
Same employee + overlapping shift
```

Conceptually:

```sql
EXCLUDE USING gist (
    room_id WITH =,
    booking_period WITH &&
)
```

Think:

```text
UNIQUE → same

EXCLUDE → conflict
```

---

# 24. The most important design lesson

Don't start database design by asking:

> "Should I use `CHECK` or `UNIQUE`?"

Start with:

> **"What must always be true about my data?"**

For example:

```text
Business requirement
        ↓
"In every order, quantity must be positive"
        ↓
Invariant
        ↓
quantity > 0
        ↓
CHECK
```

Another:

```text
Business requirement
        ↓
"Email cannot belong to two customers"
        ↓
Invariant
        ↓
email unique
        ↓
UNIQUE
```

Another:

```text
Business requirement
        ↓
"Order customer must exist"
        ↓
Invariant
        ↓
referential integrity
        ↓
FOREIGN KEY
```

This is the fundamental database-design mindset.

---

# 25. Complete mental map of the course

We've now covered:

```text
                    DATABASE INTEGRITY
                           │
       ┌───────────────────┼────────────────────┐
       ↓                   ↓                    ↓
   Value rules         Identity rules      Relationship rules
       │                   │                    │
   NOT NULL          PRIMARY KEY           FOREIGN KEY
   CHECK             UNIQUE
       │
       ↓
   Defaults
       │
    DEFAULT
       │
       ↓
   Advanced rules
       │
   ┌───┼───────────────┐
   ↓   ↓               ↓
Partial  Expression   EXCLUDE
UNIQUE   uniqueness
   │
   ↓
Transactions + concurrency
   │
   ↓
Database remains valid
```

---

# 26. The one-page cheat sheet

| Feature                 | Question it answers                                  |
| ----------------------- | ---------------------------------------------------- |
| `NOT NULL`              | Can this value be missing?                           |
| `DEFAULT`               | What should happen when the value is omitted?        |
| `CHECK`                 | Does this row satisfy a condition?                   |
| `UNIQUE`                | Can this value/combo be duplicated?                  |
| `PRIMARY KEY`           | What uniquely identifies this row?                   |
| `FOREIGN KEY`           | Does this relationship point to an existing row?     |
| `ON DELETE/UPDATE`      | What happens to related rows?                        |
| Composite key           | Does a combination identify/enforce uniqueness?      |
| `DEFERRABLE`            | Can this constraint be checked later?                |
| Partial unique index    | Is uniqueness conditional?                           |
| Expression unique index | Should uniqueness be based on a computed expression? |
| `EXCLUDE`               | Can two rows conflict/overlap?                       |

---

# 27. The most important interview mental model

If an interviewer gives you a requirement, translate it like this:

```text
Requirement
    ↓
Invariant
    ↓
Constraint/mechanism
    ↓
Database-enforced guarantee
```

For example:

```text
"Username cannot repeat"
        ↓
username must be unique
        ↓
UNIQUE(username)
```

```text
"Every order belongs to a customer"
        ↓
customer must exist
        ↓
FOREIGN KEY
```

```text
"Quantity must be positive"
        ↓
quantity > 0
        ↓
CHECK(quantity > 0)
```

```text
"Booking periods cannot overlap"
        ↓
same resource + overlapping ranges forbidden
        ↓
EXCLUDE
```

That is the core skill you should take away from this entire course.

---

# Final takeaway

Database integrity constraints are not just SQL syntax to memorize.

They are a way to **formally encode invariants directly into the database**.

A good schema asks:

> **What invalid states must be impossible?**

Then expresses those rules as close to the data as possible.

```text
Application
    ↓
Business logic / validation
    ↓
Database constraints
    ↓
Persistent valid state
```

The application can make mistakes.

Multiple applications can write concurrently.

Different services can access the database.

Someone can run SQL directly.

But when an invariant truly matters, the database can enforce it.

That is the central idea behind **Database Integrity Constraints**.

