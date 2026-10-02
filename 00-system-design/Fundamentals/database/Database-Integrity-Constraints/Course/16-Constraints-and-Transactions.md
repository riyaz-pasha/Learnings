# 16. Constraints and Transactions

Now we connect two concepts we've already learned:

* **Integrity constraints** → define what database states are valid.
* **Transactions** → group multiple database operations into one atomic unit.

The key question is:

> **What happens to constraints when multiple statements execute inside a transaction?**

---

# 1. Quick transaction refresher

A transaction groups operations together:

```sql
BEGIN;

-- operation 1
-- operation 2
-- operation 3

COMMIT;
```

If everything succeeds:

```text
BEGIN
  ↓
Operation 1
  ↓
Operation 2
  ↓
Operation 3
  ↓
COMMIT
  ↓
Changes become permanent
```

If something goes wrong:

```sql
ROLLBACK;
```

Then the transaction's changes are undone.

---

# 2. Constraints are checked during transactions

Suppose:

```sql
CREATE TABLE products (
    id BIGINT PRIMARY KEY,

    price NUMERIC(10,2) NOT NULL,

    CONSTRAINT chk_products_price
        CHECK (price >= 0)
);
```

Now:

```sql
BEGIN;

INSERT INTO products (id, price)
VALUES (1, 100);

INSERT INTO products (id, price)
VALUES (2, -50);

COMMIT;
```

The second `INSERT` violates:

```text
price >= 0
```

So PostgreSQL raises an error.

The important question becomes:

> Does the first insert remain?

---

# 3. PostgreSQL transaction behavior after an error

For a normal PostgreSQL transaction, once a statement produces an error, the transaction enters an **aborted state**.

Conceptually:

```text
BEGIN
  ↓
INSERT 1 → success
  ↓
INSERT 2 → constraint violation ❌
  ↓
Transaction is aborted
  ↓
Further commands fail
  ↓
ROLLBACK
```

For example:

```sql
BEGIN;

INSERT INTO products (id, price)
VALUES (1, 100);

INSERT INTO products (id, price)
VALUES (2, -50);

INSERT INTO products (id, price)
VALUES (3, 200);
```

The third statement cannot simply continue as if nothing happened.

You need:

```sql
ROLLBACK;
```

to end the failed transaction.

---

# 4. Why does PostgreSQL do this?

Because the transaction represents one logical unit of work.

Suppose we're transferring money:

```text
Account A: -100
Account B: +100
```

We don't want:

```text
Debit succeeds
Credit fails
```

and then accidentally commit the partial operation.

Instead:

```text
BEGIN
   ↓
Debit A
   ↓
Credit B
   ↓
COMMIT
```

If something fails:

```text
BEGIN
   ↓
Debit A
   ↓
Credit B → ERROR
   ↓
ROLLBACK
   ↓
Debit A is undone
```

This is the combination of:

> **Transactions + constraints + atomicity**

---

# 5. Example with a foreign key

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

Now:

```sql
BEGIN;

INSERT INTO orders (id, customer_id)
VALUES (100, 999);

COMMIT;
```

If customer `999` doesn't exist:

```text
FOREIGN KEY violation
```

The transaction fails.

The database does not store an order that references a nonexistent customer.

---

# 6. Transaction with multiple valid operations

Now:

```sql
BEGIN;

INSERT INTO customers (id)
VALUES (999);

INSERT INTO orders (id, customer_id)
VALUES (100, 999);

COMMIT;
```

The sequence is:

```text
Insert customer 999
        ↓
customer exists
        ↓
Insert order referencing 999
        ↓
FK satisfied
        ↓
COMMIT
```

Everything succeeds.

---

# 7. Transaction gives you atomicity

Suppose we're creating an order.

We need:

```text
1. Create order
2. Create order items
3. Reduce inventory
4. Create payment record
```

We might do:

```sql
BEGIN;

INSERT INTO orders (...);

INSERT INTO order_items (...);

UPDATE inventory
SET quantity = quantity - 1
WHERE product_id = 10;

INSERT INTO payments (...);

COMMIT;
```

If step 4 fails:

```text
Payment insert
      ↓
ERROR
      ↓
ROLLBACK
```

Then steps 1–3 are also rolled back.

This is why transactions and constraints work well together.

---

# 8. Constraint violations protect atomicity

Suppose:

```sql
CREATE TABLE order_items (
    id BIGINT PRIMARY KEY,

    quantity INT NOT NULL,

    CONSTRAINT chk_quantity_positive
        CHECK (quantity > 0)
);
```

Now:

```sql
BEGIN;

INSERT INTO orders (id)
VALUES (100);

INSERT INTO order_items (id, quantity)
VALUES (1, 2);

INSERT INTO order_items (id, quantity)
VALUES (2, -1);

COMMIT;
```

The final insert violates:

```text
quantity > 0
```

The transaction cannot successfully commit in its current state.

If we roll it back:

```sql
ROLLBACK;
```

the entire transaction's changes disappear.

So we don't end up with:

```text
Order exists
Some items exist
Some other operation failed
```

Instead:

```text
Nothing from the failed transaction remains.
```

---

# 9. Important distinction: statement vs transaction

These are different concepts.

### Statement

One SQL command:

```sql
INSERT ...
```

### Transaction

A group of SQL commands:

```sql
BEGIN;

INSERT ...
UPDATE ...
DELETE ...

COMMIT;
```

Constraints normally apply to statements as they execute.

But the transaction determines whether the overall set of changes becomes permanent.

---

# 10. Immediate constraints inside transactions

Most constraints are checked immediately.

Example:

```sql
BEGIN;

INSERT INTO products (id, price)
VALUES (1, -10);
```

The `CHECK` violation happens at the `INSERT`.

You don't wait for:

```sql
COMMIT;
```

The statement itself fails.

This is:

```text
Immediate constraint
        ↓
Statement executes
        ↓
Constraint checked
```

---

# 11. Deferred constraints

This connects directly to our previous lesson.

A deferrable constraint can instead be checked later.

For example:

```sql
CREATE TABLE employees (
    id INT PRIMARY KEY,

    manager_id INT,

    CONSTRAINT fk_manager
        FOREIGN KEY (manager_id)
        REFERENCES employees(id)
        DEFERRABLE INITIALLY DEFERRED
);
```

Then:

```sql
BEGIN;

INSERT INTO employees (id, manager_id)
VALUES (1, 2);

INSERT INTO employees (id, manager_id)
VALUES (2, 1);

COMMIT;
```

The FK can temporarily be violated during the transaction.

At the appropriate checking point:

```text
COMMIT
  ↓
Check FK
  ↓
Both employees exist
  ↓
Success
```

So transactions provide the boundary within which deferred constraints can operate.

---

# 12. Immediate vs deferred constraints

Visualize them like this.

### Immediate

```text
BEGIN
  ↓
INSERT
  ↓
CHECK NOW
  ↓
Pass / Error
```

### Deferred

```text
BEGIN
  ↓
INSERT
  ↓
Don't check yet
  ↓
More operations
  ↓
COMMIT
  ↓
CHECK
  ↓
Pass / Error
```

This is why `DEFERRABLE` is closely related to transactions.

---

# 13. What happens after a constraint error?

Consider:

```sql
BEGIN;

INSERT INTO products (id, price)
VALUES (1, 100);

INSERT INTO products (id, price)
VALUES (2, -50);

SELECT * FROM products;
```

After the constraint violation, PostgreSQL considers the transaction failed.

The subsequent `SELECT` doesn't simply continue normally.

You typically see an error indicating that the current transaction is aborted and commands are ignored until the transaction ends.

You then need:

```sql
ROLLBACK;
```

---

# 14. `ROLLBACK` restores the previous state

Suppose before the transaction:

```text
products

id | price
---+------
10 | 100
```

Then:

```sql
BEGIN;

INSERT INTO products (id, price)
VALUES (20, 200);

INSERT INTO products (id, price)
VALUES (30, -50);

ROLLBACK;
```

After rollback:

```text
id | price
---+------
10 | 100
```

The successful insert of `20` is also gone.

That's atomicity.

---

# 15. `SAVEPOINT`: recovering from an error

There is an important exception to the "transaction is aborted" story.

You can use a **savepoint**.

Example:

```sql
BEGIN;

INSERT INTO products (id, price)
VALUES (1, 100);

SAVEPOINT before_second_insert;

INSERT INTO products (id, price)
VALUES (2, -50);
```

The second insert fails.

Instead of rolling back the entire transaction:

```sql
ROLLBACK TO SAVEPOINT before_second_insert;
```

Now you can continue:

```sql
INSERT INTO products (id, price)
VALUES (3, 200);

COMMIT;
```

Final state:

```text
id | price
---+------
1  | 100
3  | 200
```

The failed operation was discarded, but the earlier successful work was preserved.

---

# 16. Savepoint mental model

Think of:

```text
BEGIN
  │
  ├── Operation A
  │
  ├── SAVEPOINT
  │
  ├── Operation B → ❌
  │
  ├── ROLLBACK TO SAVEPOINT
  │
  ├── Operation C
  │
  └── COMMIT
```

Instead of:

```text
Operation B fails
      ↓
Entire transaction lost
```

you can sometimes do:

```text
Operation B fails
      ↓
Rollback to savepoint
      ↓
Continue transaction
```

---

# 17. Why this matters in applications

Database libraries often manage transactions for you.

For example, a Java/Spring application might conceptually do:

```java
@Transactional
public void createOrder() {
    createOrder();
    createOrderItems();
    updateInventory();
    createPayment();
}
```

If a database constraint violation occurs:

```text
Constraint violation
        ↓
Transaction fails
        ↓
Rollback
```

The application doesn't manually execute:

```sql
ROLLBACK;
```

in every method.

The transaction framework handles it according to its configuration and exception semantics.

---

# 18. Constraint + transaction example

Imagine:

```sql
CREATE TABLE accounts (
    id BIGINT PRIMARY KEY,
    balance NUMERIC(12,2) NOT NULL,
    
    CONSTRAINT chk_balance
        CHECK (balance >= 0)
);
```

Now:

```sql
BEGIN;

UPDATE accounts
SET balance = balance - 500
WHERE id = 1;

UPDATE accounts
SET balance = balance + 500
WHERE id = 2;

COMMIT;
```

If account 1 would become negative and the constraint rejects the first update:

```text
UPDATE account 1
       ↓
CHECK fails
       ↓
Transaction fails
       ↓
Account 2 is NOT credited
```

That's exactly what we want.

We don't want:

```text
Account 1: debit failed
Account 2: credit succeeded
```

because that would create money out of nowhere.

---

# 19. Constraints don't replace transactions

This is another important distinction.

Suppose you have:

```text
Account A
Account B
```

and want to transfer money.

A `CHECK` constraint can enforce something like:

```text
balance >= 0
```

But it doesn't automatically make these two operations atomic:

```sql
UPDATE accounts SET balance = balance - 100 WHERE id = 1;

UPDATE accounts SET balance = balance + 100 WHERE id = 2;
```

You need a transaction:

```sql
BEGIN;

UPDATE accounts
SET balance = balance - 100
WHERE id = 1;

UPDATE accounts
SET balance = balance + 100
WHERE id = 2;

COMMIT;
```

So:

```text
Constraint
→ protects validity

Transaction
→ protects atomicity
```

---

# 20. Constraints don't replace concurrency control either

Imagine two transactions both try to modify the same account.

Transactions alone don't automatically mean:

> "Nobody else can interfere."

Isolation levels, row locks, MVCC, `SELECT ... FOR UPDATE`, unique constraints, and other mechanisms can matter depending on the problem.

We'll cover concurrency separately in a later lesson.

For now, remember:

```text
Constraints
    ↓
Is the state valid?

Transactions
    ↓
Are these changes atomic?

Concurrency control
    ↓
What happens when operations overlap?
```

These are related but different concerns.

---

# 21. A complete example

Consider:

```sql
CREATE TABLE customers (
    id BIGINT PRIMARY KEY
);

CREATE TABLE orders (
    id BIGINT PRIMARY KEY,

    customer_id BIGINT NOT NULL,

    total NUMERIC(12,2) NOT NULL,

    CONSTRAINT fk_orders_customer
        FOREIGN KEY (customer_id)
        REFERENCES customers(id),

    CONSTRAINT chk_orders_total
        CHECK (total >= 0)
);
```

Now create a customer and order atomically:

```sql
BEGIN;

INSERT INTO customers (id)
VALUES (1);

INSERT INTO orders (id, customer_id, total)
VALUES (100, 1, 500);

COMMIT;
```

Everything succeeds.

But:

```sql
BEGIN;

INSERT INTO customers (id)
VALUES (1);

INSERT INTO orders (id, customer_id, total)
VALUES (100, 999);

COMMIT;
```

The order violates:

```text
fk_orders_customer
```

The transaction fails.

If rolled back:

```sql
ROLLBACK;
```

the customer inserted in that transaction is also removed.

---

# 22. The deeper relationship

We've now built a useful hierarchy:

```text
              Database
                 │
        ┌────────┴────────┐
        │                 │
   Constraints       Transactions
        │                 │
   Valid state        Atomic changes
        │                 │
        └────────┬────────┘
                 │
          Reliable data
```

Constraints answer:

> **"What data state is allowed?"**

Transactions answer:

> **"Which collection of changes should become permanent together?"**

---

# 23. Interview questions

### Q1. What happens when a normal constraint is violated inside a transaction?

The statement fails, and PostgreSQL marks the transaction as aborted. You normally need `ROLLBACK` or a savepoint rollback before continuing.

---

### Q2. Does a successful earlier statement remain after a later constraint violation?

Not if the entire transaction is rolled back.

```text
Statement 1 → success
Statement 2 → constraint violation
ROLLBACK
       ↓
Statement 1 also undone
```

---

### Q3. Can you continue after an error inside a transaction?

Not directly after a normal error.

You need to end the failed transaction or recover using a savepoint:

```sql
ROLLBACK TO SAVEPOINT ...
```

---

### Q4. What is the difference between a constraint and a transaction?

| Constraint              | Transaction                      |
| ----------------------- | -------------------------------- |
| Defines valid data      | Groups changes                   |
| Protects invariants     | Provides atomicity               |
| Prevents invalid state  | Determines what commits together |
| Examples: PK, FK, CHECK | `BEGIN`, `COMMIT`, `ROLLBACK`    |

---

### Q5. Why are constraints useful inside transactions?

They ensure that the transaction cannot successfully commit an invalid database state when those constraints apply.

---

### Q6. Why are deferred constraints related to transactions?

Because deferred constraints intentionally allow certain temporary states and validate them later within the transaction lifecycle.

---

# Mental Model

Think of a transaction as a **unit of work**:

```text
BEGIN
   ↓
Make several changes
   ↓
Constraints protect validity
   ↓
Something fails?
   ├── Yes → ROLLBACK
   └── No  → COMMIT
```

And remember these three concepts separately:

```text
CONSTRAINT
"What states are valid?"

TRANSACTION
"Which changes commit together?"

CONCURRENCY CONTROL
"What happens when changes happen simultaneously?"
```

The combination is what makes relational databases reliable.

### Next concept

**17. Constraints Under Concurrency** — we'll look at the difficult case where two transactions perform operations at the same time, including race conditions, unique constraints, foreign keys, locking, and why application-side checks can fail.

