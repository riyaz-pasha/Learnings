# 9. Foreign Key Actions

We already learned that a foreign key maintains a relationship between two tables.

Now we need to answer an important question:

> **What should happen to child rows when the referenced parent row is updated or deleted?**

For example:

```sql
CREATE TABLE customers (
    id BIGINT PRIMARY KEY,
    name TEXT NOT NULL
);

CREATE TABLE orders (
    id BIGINT PRIMARY KEY,
    customer_id BIGINT NOT NULL
        REFERENCES customers(id)
);
```

Relationship:

```text
customers
    │
    │ id
    ▼
orders.customer_id
```

Suppose:

```text
customers

id    name
------------
1     Alice
2     Bob
```

and:

```text
orders

id    customer_id
-----------------
101   1
102   1
103   2
```

What happens if we execute:

```sql
DELETE FROM customers
WHERE id = 1;
```

There are orders referring to customer `1`.

PostgreSQL needs a rule telling it what to do.

That's where foreign key actions come in.

---

# 1. The five important actions

PostgreSQL provides:

```text
NO ACTION
RESTRICT
CASCADE
SET NULL
SET DEFAULT
```

They can be used for both:

```text
ON DELETE
ON UPDATE
```

For example:

```sql
FOREIGN KEY (customer_id)
REFERENCES customers(id)
ON DELETE CASCADE
ON UPDATE CASCADE
```

---

# 2. `NO ACTION`

This is the **default behavior**.

Example:

```sql
CREATE TABLE orders (
    id BIGINT PRIMARY KEY,
    customer_id BIGINT NOT NULL,

    FOREIGN KEY (customer_id)
        REFERENCES customers(id)
        ON DELETE NO ACTION
);
```

Suppose:

```text
customers

id
--
1
```

and:

```text
orders

id    customer_id
-----------------
101   1
102   1
```

Now:

```sql
DELETE FROM customers
WHERE id = 1;
```

PostgreSQL says:

> You cannot delete customer `1` because orders still reference it.

The delete fails.

This protects referential integrity.

---

# 3. Why does PostgreSQL reject it?

Without the foreign key rule, we'd end up with:

```text
customers

id
--
2
```

while:

```text
orders

id    customer_id
-----------------
101   1
102   1
```

Now `customer_id = 1` points to something that doesn't exist.

That's called an **orphaned reference**.

The foreign key prevents this.

---

# 4. `RESTRICT`

`RESTRICT` also prevents the parent row from being deleted or updated when dependent rows exist.

```sql
CREATE TABLE orders (
    id BIGINT PRIMARY KEY,
    customer_id BIGINT NOT NULL,

    FOREIGN KEY (customer_id)
        REFERENCES customers(id)
        ON DELETE RESTRICT
);
```

If orders reference customer `1`:

```sql
DELETE FROM customers
WHERE id = 1;
```

fails.

So at first glance:

```text
NO ACTION
RESTRICT
```

look almost identical.

There is, however, an important difference.

---

# 5. `NO ACTION` vs `RESTRICT`

This is an interview favorite.

### `RESTRICT`

> Check the restriction immediately.

### `NO ACTION`

> Normally reject the operation if the constraint would be violated, but a **deferrable** foreign key can postpone that check until the constraint's check time, typically transaction commit.

This distinction becomes important when using:

```sql
DEFERRABLE
```

We'll study deferrable constraints later.

For normal, non-deferrable foreign keys, you can generally think:

```text
NO ACTION ≈ RESTRICT
```

But technically:

```text
RESTRICT
→ cannot be deferred

NO ACTION
→ can participate in deferred constraint checking
```

---

# 6. `CASCADE`

Now we get to the powerful one.

```sql
ON DELETE CASCADE
```

means:

> If the parent row is deleted, automatically delete the referencing child rows.

Example:

```sql
CREATE TABLE orders (
    id BIGINT PRIMARY KEY,
    customer_id BIGINT NOT NULL,

    FOREIGN KEY (customer_id)
        REFERENCES customers(id)
        ON DELETE CASCADE
);
```

Data:

```text
customers

id    name
------------
1     Alice
2     Bob
```

```text
orders

id    customer_id
-----------------
101   1
102   1
103   2
```

Now:

```sql
DELETE FROM customers
WHERE id = 1;
```

PostgreSQL deletes:

```text
customers
1
```

and automatically deletes:

```text
orders
101
102
```

Order `103` remains because it belongs to customer `2`.

---

# 7. Visualizing `CASCADE`

Before:

```text
Customer 1
   │
   ├── Order 101
   └── Order 102
```

After:

```sql
DELETE FROM customers WHERE id = 1;
```

with:

```sql
ON DELETE CASCADE
```

we get:

```text
Customer 1
   │
   ├── Order 101
   └── Order 102

        ↓ DELETE

everything removed
```

So:

```text
Parent deleted
      ↓
Children automatically deleted
```

---

# 8. When is `CASCADE` useful?

A good use case is **dependent data that has no meaningful existence without the parent**.

For example:

```text
order
 ├── order_items
 ├── order_addresses
 └── order_metadata
```

If the database model says these records exist only as part of the order, cascading deletion can be appropriate.

Another example:

```text
user
 └── user_preferences
```

If `user_preferences` has no meaning without the user, `CASCADE` may make sense.

---

# 9. When should you be careful with `CASCADE`?

Imagine:

```text
customer
   │
   └── orders
         │
         └── payments
```

If you use cascading relationships everywhere:

```text
DELETE customer
       ↓
DELETE orders
       ↓
DELETE payments
```

One delete can cause a large amount of data to disappear.

Therefore:

> `CASCADE` should be chosen based on the ownership/lifecycle relationship, not simply because it is convenient.

For financial or audit data, you may deliberately avoid cascading deletes because historical records need to remain.

---

# 10. `SET NULL`

Another option is:

```sql
ON DELETE SET NULL
```

This means:

> Delete the parent, but keep the child row and set its foreign key to `NULL`.

Example:

```sql
CREATE TABLE orders (
    id BIGINT PRIMARY KEY,

    customer_id BIGINT,

    FOREIGN KEY (customer_id)
        REFERENCES customers(id)
        ON DELETE SET NULL
);
```

Notice:

```sql
customer_id BIGINT
```

is nullable.

Suppose:

```text
customers

id
--
1
```

and:

```text
orders

id    customer_id
-----------------
101   1
102   1
```

Now:

```sql
DELETE FROM customers
WHERE id = 1;
```

Result:

```text
customers

id
--
(empty)
```

and:

```text
orders

id    customer_id
-----------------
101   NULL
102   NULL
```

The orders remain.

---

# 11. Why would we use `SET NULL`?

Suppose we have:

```text
employees
    │
    │ manager_id
    ▼
projects
```

Or perhaps:

```text
orders
    │
    │ sales_rep_id
    ▼
employees
```

If an employee leaves the company, you might want to keep the order:

```text
order_id = 1001
sales_rep_id = NULL
```

because the order still exists, but the employee relationship no longer does.

That's a different lifecycle relationship from:

```text
order
   └── order_item
```

where the child might genuinely depend on the parent.

---

# 12. `SET NULL` requires nullable columns

This will cause a problem:

```sql
CREATE TABLE orders (
    id BIGINT PRIMARY KEY,

    customer_id BIGINT NOT NULL,

    FOREIGN KEY (customer_id)
        REFERENCES customers(id)
        ON DELETE SET NULL
);
```

Why?

Because PostgreSQL would need to do:

```text
customer_id = NULL
```

but:

```sql
NOT NULL
```

prohibits that.

So:

```text
ON DELETE SET NULL
        ↓
FK column must be able to contain NULL
```

---

# 13. `SET DEFAULT`

Another option is:

```sql
ON DELETE SET DEFAULT
```

This means:

> When the referenced parent is deleted, set the child foreign key to its default value.

Example:

```sql
CREATE TABLE customers (
    id BIGINT PRIMARY KEY,
    name TEXT NOT NULL
);

CREATE TABLE orders (
    id BIGINT PRIMARY KEY,

    customer_id BIGINT DEFAULT 0,

    FOREIGN KEY (customer_id)
        REFERENCES customers(id)
        ON DELETE SET DEFAULT
);
```

Conceptually:

```text
customer 1 deleted
       ↓
order.customer_id
       ↓
DEFAULT
       ↓
0
```

But there is an important catch.

---

# 14. The default must still satisfy the foreign key

Suppose:

```text
customers

id
--
1
2
```

and:

```sql
customer_id BIGINT DEFAULT 0
```

Now customer `1` is deleted.

PostgreSQL tries:

```text
customer_id = 0
```

But:

```text
customers.id = 0
```

doesn't exist.

The foreign key is still violated.

Therefore the operation fails.

If you want:

```sql
ON DELETE SET DEFAULT
```

you generally need a valid default referenced row.

For example:

```text
customers

id    name
----------------
0     Unknown
1     Alice
2     Bob
```

Then:

```text
customer 1 deleted
       ↓
customer_id = 0
       ↓
points to "Unknown"
```

This is sometimes useful, although `SET NULL` is often simpler when "no longer associated" is the desired meaning.

---

# 15. Summary of `ON DELETE`

Suppose:

```text
Parent
  ↑
  │
Child
```

and we delete the parent.

| Action        | What happens to child?                  |
| ------------- | --------------------------------------- |
| `NO ACTION`   | Delete fails if child references parent |
| `RESTRICT`    | Delete fails if child references parent |
| `CASCADE`     | Child is automatically deleted          |
| `SET NULL`    | Child remains, FK becomes `NULL`        |
| `SET DEFAULT` | Child remains, FK becomes its default   |

Visualized:

```text
NO ACTION
Parent ✗
Child  → remains


RESTRICT
Parent ✗
Child  → remains


CASCADE
Parent ✓ deleted
Child  → deleted


SET NULL
Parent ✓ deleted
Child  → FK = NULL


SET DEFAULT
Parent ✓ deleted
Child  → FK = DEFAULT
```

---

# 16. `ON UPDATE`

Everything we've discussed so far was about:

```sql
ON DELETE
```

But the same concepts can apply when the referenced key is updated.

For example:

```sql
CREATE TABLE orders (
    id BIGINT PRIMARY KEY,
    customer_id BIGINT,

    FOREIGN KEY (customer_id)
        REFERENCES customers(id)
        ON UPDATE CASCADE
);
```

Suppose:

```text
customers

id
--
100
```

and:

```text
orders

id    customer_id
-----------------
1     100
2     100
```

If:

```sql
UPDATE customers
SET id = 200
WHERE id = 100;
```

with:

```sql
ON UPDATE CASCADE
```

PostgreSQL updates:

```text
orders

id    customer_id
-----------------
1     200
2     200
```

The relationship remains intact.

---

# 17. Why don't we see `ON UPDATE CASCADE` as often?

In many modern schemas, primary keys are designed to be **stable identifiers**.

For example:

```text
user_id = 12345
```

is generally not changed.

Instead of:

```text
user_id 12345 → 99999
```

we keep the identity stable.

Therefore:

```sql
ON UPDATE CASCADE
```

is less commonly needed in many designs.

But it is still important to understand.

---

# 18. A complete example

Let's create:

```text
customers
orders
```

with cascading deletion.

```sql
CREATE TABLE customers (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name TEXT NOT NULL
);

CREATE TABLE orders (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,

    customer_id BIGINT NOT NULL,

    FOREIGN KEY (customer_id)
        REFERENCES customers(id)
        ON DELETE CASCADE
        ON UPDATE CASCADE
);
```

Insert:

```sql
INSERT INTO customers (name)
VALUES ('Alice');

INSERT INTO orders (customer_id)
VALUES (1);

INSERT INTO orders (customer_id)
VALUES (1);
```

Data:

```text
customers

id    name
------------
1     Alice


orders

id    customer_id
-----------------
1     1
2     1
```

Now:

```sql
DELETE FROM customers
WHERE id = 1;
```

Result:

```text
customers
(empty)

orders
(empty)
```

because of:

```sql
ON DELETE CASCADE
```

---

# 19. Foreign key actions are about lifecycle

This is probably the most useful way to think about them.

Ask:

> **What is the lifecycle relationship between parent and child?**

### Case 1: Child cannot exist without parent

```text
order
 └── order_item
```

Potentially:

```sql
ON DELETE CASCADE
```

### Case 2: Child should survive without parent

```text
order
 └── sales_rep
```

Potentially:

```sql
ON DELETE SET NULL
```

### Case 3: Parent must not disappear while children exist

```text
customer
 └── financial_account
```

Potentially:

```sql
ON DELETE RESTRICT
```

### Case 4: Deleted parent should map to a special fallback

```text
customer
 └── "Unknown customer"
```

Potentially:

```sql
ON DELETE SET DEFAULT
```

The correct action depends on the domain semantics.

---

# 20. A subtle but important point about `NO ACTION`

You might see:

```sql
ON DELETE NO ACTION
```

and think:

> "Nothing happens."

That's not what it means.

It means:

> PostgreSQL does not automatically modify the child rows, but it also won't allow the operation to leave the foreign key violated.

So:

```text
NO ACTION
```

does **not** mean:

```text
"Ignore the foreign key."
```

It means:

```text
"Don't automatically take corrective action;
make sure the final state still satisfies the constraint."
```

---

# 21. Interview question

### What is the difference between `CASCADE` and `SET NULL`?

```text
CASCADE
→ parent deleted
→ child deleted

SET NULL
→ parent deleted
→ child remains
→ foreign key becomes NULL
```

Example:

```text
Before:

Customer 1
   │
   ├── Order 101
   └── Order 102
```

`CASCADE`:

```text
Customer 1 deleted
   ↓
Orders 101, 102 deleted
```

`SET NULL`:

```text
Customer 1 deleted
   ↓
Orders 101, 102 remain
   ↓
customer_id = NULL
```

---

# 22. Interview question

### `NO ACTION` vs `RESTRICT`?

For ordinary immediate constraints:

```text
Both prevent deleting/updating a referenced parent
while dependent rows exist.
```

The important technical difference is:

```text
RESTRICT
→ cannot be deferred

NO ACTION
→ can be deferred when the foreign key is DEFERRABLE
```

We'll explore this properly when we reach `DEFERRABLE`.

---

# 23. Interview question

### What happens if I use `SET NULL` with `NOT NULL`?

Example:

```sql
customer_id BIGINT NOT NULL
    REFERENCES customers(id)
    ON DELETE SET NULL
```

The delete cannot successfully set:

```text
customer_id = NULL
```

because `NOT NULL` prohibits it.

Therefore the schema is contradictory for that deletion behavior.

---

# 24. Mental model

Remember the parent-child relationship:

```text
             PARENT
               │
               │ FK
               ▼
             CHILD
```

When the parent is deleted:

```text
NO ACTION
    │
    └── "Don't let me break the relationship."

RESTRICT
    │
    └── "Don't let me delete this while children exist."

CASCADE
    │
    └── "Delete the children too."

SET NULL
    │
    └── "Keep the children, remove the relationship."

SET DEFAULT
    │
    └── "Keep the children, point them to the default."
```

That's the core idea.

---

# Key takeaways

1. `NO ACTION` is the default.
2. `NO ACTION` and `RESTRICT` usually behave similarly for immediate constraints.
3. `CASCADE` deletes dependent rows automatically.
4. `SET NULL` keeps the child but removes the relationship.
5. `SET DEFAULT` replaces the FK with its default value.
6. `SET NULL` requires a nullable FK.
7. `SET DEFAULT` requires the resulting default value to satisfy the FK.
8. The same actions can be used with `ON UPDATE`.
9. Choose the action based on the **lifecycle relationship** between parent and child.
10. Be particularly careful with cascading deletes across multiple levels.

## Next concept

**10. Composite Keys and Composite Constraints**

