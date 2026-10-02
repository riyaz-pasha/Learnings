# 15. Constraints vs Application Validation

This is a very important production concept.

A common question is:

> **"If my backend already validates the data, why do I need database constraints?"**

And the opposite question:

> **"If the database validates everything, why do I need application validation?"**

The answer is:

> **They solve different problems, and production systems commonly use both.**

---

# 1. Two layers of validation

Imagine this request:

```text
Client
  ↓
Backend
  ↓
Database
```

We can validate at both levels:

```text
Client
  ↓
Application validation
  ↓
Database constraints
  ↓
Stored data
```

Each layer has a different responsibility.

---

# 2. Application validation

Application validation is validation performed by your backend/application code.

For example, a REST API receives:

```json
{
  "price": -100
}
```

Your Java application might check:

```java
if (price.compareTo(BigDecimal.ZERO) < 0) {
    throw new IllegalArgumentException("Price cannot be negative");
}
```

The API can then return:

```text
HTTP 400 Bad Request
```

This is useful because the application can provide a **friendly, domain-specific error**.

---

# 3. Database validation

The database can independently enforce:

```sql
CREATE TABLE products (
    id BIGINT PRIMARY KEY,

    price NUMERIC(10,2) NOT NULL,

    CONSTRAINT chk_products_price_non_negative
        CHECK (price >= 0)
);
```

Now even if some application code accidentally tries:

```sql
INSERT INTO products (price)
VALUES (-100);
```

PostgreSQL rejects it.

---

# 4. Why application validation alone is dangerous

Suppose your backend does:

```text
if email doesn't already exist
    INSERT user
```

Conceptually:

```text
Request
  ↓
SELECT whether email exists
  ↓
No
  ↓
INSERT
```

This looks correct.

But now imagine two requests arrive at almost the same time.

```text
Request A                 Request B
    │                         │
    │── check email ─────────>│
    │                         │
    │     "doesn't exist"     │
    │                         │
    │                         │
    │                  check email
    │                         │
    │                  "doesn't exist"
    │                         │
    ▼                         ▼
 INSERT                    INSERT
```

Both applications can conclude:

```text
"Email is available."
```

Now both try to insert it.

Without a database constraint:

```text
email
----------------
alice@example.com
alice@example.com
```

You've got duplicate data.

---

# 5. Database `UNIQUE` solves the race

Add:

```sql
CONSTRAINT uq_users_email
UNIQUE (email)
```

Now:

```text
Request A                 Request B
    │                         │
    ├── application check    │
    │                         ├── application check
    │                         │
    ▼                         ▼
 INSERT                    INSERT
    │                         │
    │                         ├── database detects conflict
    │                         │
    ▼                         ▼
 SUCCESS                   REJECT
```

The database becomes the final authority.

Only one row can satisfy:

```text
email = 'alice@example.com'
```

---

# 6. Application validation is still useful

You might wonder:

> "Then why not remove the application validation?"

Because application validation provides a better experience.

Suppose the user sends:

```json
{
  "age": 15
}
```

and the business rule is:

```text
age >= 18
```

The backend can immediately respond:

```text
400 Bad Request

Age must be at least 18.
```

Instead of allowing the request to travel all the way to the database and then translating a database error.

So:

```text
Application validation
        ↓
Good user/API experience

Database constraint
        ↓
Data integrity guarantee
```

---

# 7. Think of the database as the final authority

Imagine multiple systems write to the same database:

```text
                ┌── Web API
                │
                ├── Admin API
                │
                ├── Background worker
                │
                ├── Batch job
                │
                ├── Data migration
                │
                └── Another service
                        │
                        ▼
                    Database
```

If only the main API validates:

```text
Web API → validates
Worker → maybe doesn't
Batch job → maybe doesn't
Migration → maybe doesn't
```

Then invalid data can still enter.

A database constraint applies at the common boundary:

```text
Every writer
     ↓
  Database
     ↓
Constraint
     ↓
Valid state
```

---

# 8. Example: `NOT NULL`

Suppose:

```sql
email TEXT NOT NULL
```

Application:

```java
if (email == null) {
    throw new ValidationException("Email is required");
}
```

Database:

```sql
email TEXT NOT NULL
```

Why both?

### Application

Provides:

```text
400
"Email is required"
```

### Database

Guarantees:

```text
No committed row can have email = NULL
```

---

# 9. Example: `CHECK`

Business rule:

```text
quantity > 0
```

Application:

```java
if (quantity <= 0) {
    throw new ValidationException(
        "Quantity must be greater than zero"
    );
}
```

Database:

```sql
CONSTRAINT chk_order_items_quantity_positive
CHECK (quantity > 0)
```

Again:

```text
Application → friendly validation

Database → authoritative invariant
```

---

# 10. Example: `FOREIGN KEY`

Suppose:

```text
orders.customer_id
        ↓
customers.id
```

Application might do:

```text
SELECT id
FROM customers
WHERE id = ?
```

Then:

```text
if customer doesn't exist
    return 400
```

But this still isn't enough.

Between the application check and the actual insert, database state can change.

The database should enforce:

```sql
CONSTRAINT fk_orders_customer
FOREIGN KEY (customer_id)
REFERENCES customers(id)
```

Now the database guarantees:

```text
orders.customer_id
        ↓
must reference
        ↓
existing customers.id
```

---

# 11. Application validation and business rules

Not every business rule belongs in a database constraint.

For example:

> "A customer can create at most 3 orders per day."

This may involve:

* multiple rows
* time
* user identity
* current state
* business policy

The application may be responsible for this logic.

But some rules can still have database enforcement mechanisms.

For example:

> "Only one active subscription per customer."

A partial unique index could enforce this:

```sql
CREATE UNIQUE INDEX uq_one_active_subscription
ON subscriptions (customer_id)
WHERE status = 'ACTIVE';
```

The general principle is:

> Put rules in the strongest appropriate layer that can reliably enforce them.

---

# 12. Validation vs constraint

These terms are related but not identical.

### Application validation

Usually means:

> "Should I accept this request?"

Example:

```text
Is the request body valid?
Is this field present?
Is this string properly formatted?
Does this user have permission?
```

### Database constraint

Means:

> "Can this database state exist?"

Example:

```text
Can two users have the same email?
Can an order reference a nonexistent customer?
Can a product have a negative price?
Can a required column be NULL?
```

This distinction is extremely useful.

---

# 13. Validation can change; invariants should be protected

Suppose your application has a UI:

```text
Frontend
   ↓
Backend
   ↓
Database
```

Today the frontend validates:

```text
price >= 0
```

Tomorrow someone creates:

```text
Admin script
   ↓
Database
```

The frontend validation is irrelevant.

But:

```sql
CHECK (price >= 0)
```

still protects the database.

This is why constraints are particularly valuable for **invariants**.

---

# 14. Don't put everything into the database

There's also an opposite mistake:

> "Let's put every business rule into database constraints."

That can make the schema unnecessarily complicated.

For example:

```text
"If the customer is a premium customer,
and the order is above $500,
and the customer has been active for 6 months,
then apply promotion X unless..."
```

This kind of evolving business logic is often better handled in application/domain logic.

The database should primarily protect **fundamental data integrity**.

Examples:

```text
NOT NULL
UNIQUE
PRIMARY KEY
FOREIGN KEY
CHECK
EXCLUSION
```

---

# 15. A useful three-layer model

A production system can look like:

```text
┌─────────────────────────────┐
│          API layer          │
│                             │
│ Request shape               │
│ Required fields             │
│ Format validation           │
└──────────────┬──────────────┘
               ↓
┌─────────────────────────────┐
│       Domain/application    │
│                             │
│ Business workflows          │
│ Authorization               │
│ Complex business rules      │
└──────────────┬──────────────┘
               ↓
┌─────────────────────────────┐
│          Database           │
│                             │
│ Data invariants             │
│ Uniqueness                  │
│ Referential integrity       │
│ Basic value constraints     │
└─────────────────────────────┘
```

The layers complement each other.

---

# 16. Example: user registration

Suppose we're creating:

```text
User
----
email
password
age
```

### API/application validation

Could check:

```text
email format is valid
password meets password policy
age is supplied
request isn't malformed
```

### Database constraints

Could enforce:

```sql
CREATE TABLE users (
    id BIGINT GENERATED ALWAYS AS IDENTITY,

    email TEXT NOT NULL,

    password_hash TEXT NOT NULL,

    age INT NOT NULL,

    CONSTRAINT pk_users
        PRIMARY KEY (id),

    CONSTRAINT uq_users_email
        UNIQUE (email),

    CONSTRAINT chk_users_age
        CHECK (age >= 18)
);
```

Now each layer has a clear responsibility.

---

# 17. What happens when a database constraint fails?

Suppose:

```sql
INSERT INTO users (email, password_hash, age)
VALUES ('alice@example.com', '...', 15);
```

The application might already have checked:

```text
age >= 18
```

But suppose it didn't.

PostgreSQL rejects the transaction because:

```text
chk_users_age
```

was violated.

The backend can catch the database exception and translate it into an appropriate API response.

Conceptually:

```text
Database constraint violation
           ↓
Application catches error
           ↓
Maps known constraint
           ↓
API response
```

This is a common production pattern.

---

# 18. The concurrency lesson

This is probably the most important part of today's concept.

Consider:

```text
Application check:

SELECT COUNT(*)
FROM users
WHERE email = 'a@example.com';
```

returns:

```text
0
```

You might think:

```text
"Safe to insert."
```

But another transaction can insert the same email before yours.

Therefore:

```text
SELECT first
+
INSERT later
```

does **not** guarantee uniqueness.

Instead:

```sql
UNIQUE (email)
```

provides the database-level guarantee.

This is why:

> **"Check before insert" is not a replacement for `UNIQUE`.**

---

# 19. `INSERT ... ON CONFLICT`

PostgreSQL also provides a very useful mechanism for handling uniqueness conflicts.

Suppose:

```sql
CREATE TABLE users (
    id BIGINT PRIMARY KEY,
    email TEXT UNIQUE
);
```

You can write:

```sql
INSERT INTO users (id, email)
VALUES (1, 'alice@example.com')
ON CONFLICT (email)
DO NOTHING;
```

Now PostgreSQL handles the race safely.

Another possibility:

```sql
INSERT INTO users (id, email)
VALUES (1, 'alice@example.com')
ON CONFLICT (email)
DO UPDATE
SET id = EXCLUDED.id;
```

The exact conflict behavior depends on your business requirement.

The important point is that the database's uniqueness mechanism participates directly in concurrency-safe writes.

---

# 20. What should usually be a database constraint?

Good candidates include:

### Identity

```text
PRIMARY KEY
```

### Required data

```text
NOT NULL
```

### Uniqueness

```text
UNIQUE
```

### Relationships

```text
FOREIGN KEY
```

### Simple invariants

```text
CHECK
```

### Certain cross-row/range rules

```text
EXCLUDE
```

These are properties that should remain true regardless of which application writes the data.

---

# 21. What usually belongs in application logic?

Examples include:

```text
Authorization
Complex workflows
External API calls
Notifications
Business process orchestration
User-facing validation
Complex calculations
Policy decisions
```

For example:

```text
"Can this employee approve this expense?"
```

is usually an application/domain authorization decision.

Whereas:

```text
"Does this expense reference an existing employee?"
```

is a database integrity rule.

---

# 22. Interview questions

### Q1. Why isn't application validation enough?

Because multiple writers and concurrent requests can bypass or race application checks.

Database constraints provide the authoritative integrity guarantee.

---

### Q2. Why not put all validation in the database?

Because application validation is useful for request-level validation, user-friendly errors, workflows, authorization, and complex business logic.

---

### Q3. Is database validation a replacement for application validation?

No.

A common architecture is:

```text
Application validation
        +
Database constraints
```

---

### Q4. Why is `SELECT` followed by `INSERT` unsafe for uniqueness?

Because another transaction can modify the data between the two statements.

A `UNIQUE` constraint provides the database-level guarantee.

---

### Q5. What is the database's role?

A useful mental model:

> **The application decides whether a request should be accepted; the database guarantees that invalid database states cannot be committed when the relevant constraints prohibit them.**

---

# Mental Model

Think of the two layers like this:

```text
Application
    ↓
"Is this request acceptable?"
    ↓
Database
    ↓
"Can this state exist?"
```

For example:

```text
User sends age = 15
        ↓
Application
        ↓
"Age must be >= 18"
        ↓
400 Bad Request
```

But even if the application makes a mistake:

```text
Some writer
    ↓
age = 15
    ↓
Database
    ↓
CHECK (age >= 18)
    ↓
REJECT
```

And for concurrency:

```text
Application pre-check
        ↓
Helpful
        +
Database UNIQUE
        ↓
Guarantee
```

### Key takeaway

> **Application validation improves correctness and user experience at the request level. Database constraints protect the integrity of the stored data at the system boundary.**

### Next concept

**16. Constraints and Transactions** — we'll see how constraints interact with `BEGIN`, `COMMIT`, `ROLLBACK`, atomicity, and what happens when one statement in a transaction violates a constraint.

