# 17. Constraints Under Concurrency

This is where database constraints become **really important**.

So far, we've seen:

> A constraint guarantees that invalid data cannot be stored.

But what happens when **two requests try to modify the database at the same time?**

That's concurrency.

---

## 1. What is concurrency?

Concurrency simply means:

> Multiple transactions are executing at the same time.

For example, two users try to register the same username:

```text
User A                         User B
------                         ------
Check username "john"          Check username "john"
        ↓                             ↓
   Doesn't exist                 Doesn't exist
        ↓                             ↓
     INSERT                       INSERT
```

Both requests can observe that `"john"` doesn't exist.

If we rely only on application validation, both might attempt the insert.

---

# 2. The dangerous pattern

Suppose we have:

```sql
CREATE TABLE users (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    username TEXT NOT NULL
);
```

Application code might do:

```sql
SELECT 1
FROM users
WHERE username = 'john';
```

If nothing is returned:

```sql
INSERT INTO users(username)
VALUES ('john');
```

Seems reasonable.

But now imagine:

```text
Transaction A                  Transaction B

SELECT username='john'
→ no row                     

                               SELECT username='john'
                               → no row

INSERT 'john'
→ succeeds

                               INSERT 'john'
                               → succeeds
```

Now we have:

```text
john
john
```

The application check didn't protect us.

This is called a **race condition**.

---

# 3. The database constraint solves this

Add:

```sql
CREATE TABLE users (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,

    username TEXT NOT NULL,

    CONSTRAINT uq_users_username
        UNIQUE (username)
);
```

Now the database itself guarantees:

```text
There can never be two rows
with the same username.
```

Consider the same concurrent requests:

```text
Transaction A                  Transaction B

INSERT 'john'
        ↓
   UNIQUE check
        ↓
    succeeds

                               INSERT 'john'
                                      ↓
                                 UNIQUE check
                                      ↓
                                  rejected
```

The exact concurrency behavior involves PostgreSQL's locking and unique-index machinery, but the important result is:

> **The database guarantees the invariant even when multiple transactions race.**

---

# 4. Why application checks are insufficient

This is one of the most important database concepts.

### Application check

```text
"Does this username exist?"
```

This is a **question about the current state**.

Another transaction can change that state immediately afterward.

### Database constraint

```text
"At all times when this transaction is allowed to commit,
the username must be unique."
```

This is an **invariant enforced by the database**.

That's a fundamental difference.

---

# 5. Another example: seat booking

Imagine:

```sql
CREATE TABLE bookings (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    show_id BIGINT NOT NULL,
    seat_id BIGINT NOT NULL,

    CONSTRAINT uq_booking_show_seat
        UNIQUE (show_id, seat_id)
);
```

We want:

```text
One seat
+
One show
=
One booking
```

Suppose two users simultaneously try:

```text
User A → Show 10, Seat 25

User B → Show 10, Seat 25
```

Both applications might initially see:

```text
Seat 25 is available
```

But only one transaction can successfully create:

```text
(show_id = 10, seat_id = 25)
```

The other gets a unique-constraint violation.

This is much safer than:

```text
SELECT → check availability → INSERT
```

without a database constraint.

---

# 6. `SELECT` followed by `INSERT` is a classic race

This pattern is dangerous when used for uniqueness:

```sql
SELECT ...
```

then:

```sql
INSERT ...
```

because there is a gap between the two operations.

Think of it as:

```text
        Time →

A:  SELECT ─────────────── INSERT
          ↑
          |
          B can interfere here
          |
B:        SELECT ─────────────── INSERT
```

The database constraint closes the correctness gap.

---

# 7. What about transactions?

You might think:

> "Okay, I'll just put the SELECT and INSERT inside a transaction."

For example:

```sql
BEGIN;

SELECT 1
FROM users
WHERE username = 'john';

INSERT INTO users(username)
VALUES ('john');

COMMIT;
```

A transaction provides atomicity, but **a transaction by itself doesn't automatically make this uniqueness check safe**.

Two concurrent transactions can still both observe the username as absent depending on the isolation/concurrency situation.

You need the appropriate database mechanism:

```sql
UNIQUE
```

and potentially:

* appropriate locking
* appropriate transaction isolation
* `ON CONFLICT`
* domain-specific concurrency control

depending on the problem.

---

# 8. `INSERT ... ON CONFLICT`

PostgreSQL gives us a very useful way to handle uniqueness races.

For example:

```sql
INSERT INTO users(username)
VALUES ('john')
ON CONFLICT (username)
DO NOTHING;
```

Now:

```text
Transaction A
     ↓
INSERT john
     ↓
 succeeds
```

and another concurrent attempt:

```text
Transaction B
     ↓
INSERT john
     ↓
conflict
     ↓
DO NOTHING
```

Instead of treating the conflict as an unexpected application error, we can intentionally handle it.

---

# 9. `ON CONFLICT DO UPDATE`

We can also perform an upsert:

```sql
INSERT INTO users(username)
VALUES ('john')
ON CONFLICT (username)
DO UPDATE
SET username = EXCLUDED.username;
```

`EXCLUDED` represents the row we attempted to insert.

The important idea is:

```text
UNIQUE constraint
       +
ON CONFLICT
       ↓
Concurrency-safe uniqueness handling
```

The exact behavior you want depends on the business operation.

---

# 10. Foreign keys and concurrency

Concurrency isn't only about `UNIQUE`.

Consider:

```sql
CREATE TABLE customers (
    id BIGINT PRIMARY KEY
);

CREATE TABLE orders (
    id BIGINT PRIMARY KEY,
    customer_id BIGINT NOT NULL,

    CONSTRAINT fk_orders_customer
        FOREIGN KEY (customer_id)
        REFERENCES customers(id)
);
```

Now one transaction tries:

```sql
INSERT INTO orders(customer_id)
VALUES (100);
```

while another transaction is modifying customer `100`.

PostgreSQL's foreign-key enforcement and locking mechanisms coordinate these operations so that the database doesn't simply allow an invalid committed relationship.

The broader lesson is:

> Constraints aren't just static rules checked in isolation. PostgreSQL has to enforce them correctly while transactions interact.

---

# 11. Why indexes matter for concurrency

Remember that PostgreSQL uses an index to efficiently enforce a `UNIQUE` constraint.

For:

```sql
UNIQUE (username)
```

PostgreSQL creates a unique index behind the scenes.

Conceptually:

```text
users table
     │
     ↓
unique index
     │
     ↓
enforces uniqueness
```

This isn't merely a performance optimization.

The unique index is part of how PostgreSQL efficiently determines whether a conflicting value already exists while concurrent transactions are operating.

---

# 12. A very important distinction

There are three separate concepts:

| Concept             | Main responsibility                   |
| ------------------- | ------------------------------------- |
| Constraint          | Defines what data is valid            |
| Transaction         | Groups changes atomically             |
| Concurrency control | Coordinates simultaneous transactions |

For example:

```text
Two users book the same seat
          ↓
Concurrency
          ↓
Both transactions run
          ↓
UNIQUE(show_id, seat_id)
          ↓
Only one booking can be committed
```

All three concepts work together.

---

# 13. What if both transactions update the same row?

Consider:

```sql
UPDATE accounts
SET balance = balance - 100
WHERE id = 1;
```

Two transactions could execute this concurrently.

PostgreSQL uses row-level locking and MVCC mechanisms to coordinate the updates.

This is different from a uniqueness constraint.

The database has several mechanisms for concurrency control, including:

* row locks
* MVCC
* transaction isolation levels
* unique indexes
* foreign-key enforcement
* explicit locking

Constraints define **validity**, while PostgreSQL's concurrency mechanisms determine **how concurrent changes interact while preserving correctness**.

---

# 14. The big production lesson

Suppose you're designing a booking system.

Bad:

```text
Application:
    Check whether seat is available
        ↓
    If available
        ↓
    Insert booking
```

Better:

```text
Database:
    UNIQUE(show_id, seat_id)

Application:
    Try to insert
        ↓
    If conflict
        ↓
    Tell user seat is already booked
```

The application can still check availability first for a better user experience.

But the **database constraint remains the final authority**.

---

# 15. Interview question

### Q: Why isn't this safe?

```java
if (!userRepository.existsByUsername(username)) {
    userRepository.save(user);
}
```

Because two concurrent requests can execute:

```text
Request A                  Request B

exists? → false           exists? → false

save()                    save()
```

The correct database design is:

```sql
username TEXT NOT NULL UNIQUE
```

and the application should handle the resulting conflict appropriately.

---

# 16. Mental model

Think about concurrency like this:

```text
                Multiple transactions
                       │
                       ↓
                Can they race?
                       │
                       ↓
              Database constraints
                       │
                       ↓
          Invalid final state rejected
```

The most important sentence to remember:

> **Application validation can check the current state, but database constraints protect the invariant when multiple transactions change that state concurrently.**

And this is why database constraints are not just "extra validation."

They are part of the **correctness model of a concurrent system**.

---

## Next

**18. PostgreSQL-Specific Advanced Constraints**

We'll look at constraints/features that go beyond the standard `NOT NULL`, `UNIQUE`, `PRIMARY KEY`, `FOREIGN KEY`, and `CHECK` set, including PostgreSQL-specific behavior and useful advanced patterns.

