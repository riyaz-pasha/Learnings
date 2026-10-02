# 5. `FOREIGN KEY` Constraint in PostgreSQL

A `FOREIGN KEY` maintains a **relationship between rows in two tables**.

The simplest mental model is:

> **A foreign key says: "If this column contains an ID, that ID must exist in the referenced table."**

This is the foundation of **referential integrity**.

---

# 1. The problem

Suppose we have users:

```sql
CREATE TABLE users (
    id BIGINT PRIMARY KEY,
    name TEXT NOT NULL
);
```

Data:

```text
id    name
---   -----
1     Alice
2     Bob
3     Charlie
```

Now we create orders:

```sql
CREATE TABLE orders (
    id BIGINT PRIMARY KEY,
    user_id BIGINT
);
```

We might insert:

```sql
INSERT INTO orders (id, user_id)
VALUES (101, 1);
```

That's fine.

Order `101` belongs to user `1`.

But without a foreign key, PostgreSQL also allows:

```sql
INSERT INTO orders (id, user_id)
VALUES (102, 999);
```

But user `999` doesn't exist.

We now have:

```text
users
---------
1 Alice
2 Bob
3 Charlie

orders
---------
101 → user 1
102 → user 999   ❌
```

This is **referentially invalid data**.

---

# 2. The foreign key fixes this

Define the relationship:

```sql
CREATE TABLE orders (
    id BIGINT PRIMARY KEY,
    user_id BIGINT,

    FOREIGN KEY (user_id)
        REFERENCES users(id)
);
```

Now PostgreSQL understands:

```text
orders.user_id
      |
      | references
      v
users.id
```

This means:

> Every non-NULL `orders.user_id` must correspond to an existing `users.id`.

Now:

```sql
INSERT INTO orders (id, user_id)
VALUES (101, 1);
```

works.

But:

```sql
INSERT INTO orders (id, user_id)
VALUES (102, 999);
```

fails.

PostgreSQL rejects the operation because user `999` doesn't exist.

---

# 3. What is referential integrity?

This is the central concept.

> **Referential integrity means relationships between tables remain valid.**

Consider:

```text
users
+----+---------+
| id | name    |
+----+---------+
| 1  | Alice   |
| 2  | Bob     |
+----+---------+
      ^
      |
      |
orders
+-----+---------+
| id  | user_id |
+-----+---------+
| 101 | 1       |
| 102 | 2       |
+-----+---------+
```

The relationships are valid.

But:

```text
orders
+-----+---------+
| id  | user_id |
+-----+---------+
| 101 | 1       |
| 102 | 999     |  ❌
+-----+---------+
```

is invalid because:

```text
999 ∉ users.id
```

The foreign key prevents this.

---

# 4. Basic syntax

There are two common ways to write it.

### Column-level

```sql
CREATE TABLE orders (
    id BIGINT PRIMARY KEY,

    user_id BIGINT
        REFERENCES users(id)
);
```

### Table-level

```sql
CREATE TABLE orders (
    id BIGINT PRIMARY KEY,
    user_id BIGINT,

    FOREIGN KEY (user_id)
        REFERENCES users(id)
);
```

Both express the same relationship.

For more complicated foreign keys, table-level syntax is often clearer.

---

# 5. Parent and child tables

You'll frequently hear these terms.

Given:

```text
users
  ↑
  |
orders
```

`users` is the **parent** or referenced table.

`orders` is the **child** or referencing table.

More specifically:

```text
users.id
    ↑
    |
    | referenced key
    |
orders.user_id
       ↑
       |
       referencing key
```

The terminology:

| Concept            | Example          |
| ------------------ | ---------------- |
| Referenced table   | `users`          |
| Referenced column  | `users.id`       |
| Referencing table  | `orders`         |
| Referencing column | `orders.user_id` |

---

# 6. The referenced column must be unique

This is important.

Suppose:

```sql
CREATE TABLE users (
    id BIGINT,
    name TEXT
);
```

You generally cannot create:

```sql
FOREIGN KEY (user_id)
REFERENCES users(id)
```

unless `users.id` is backed by a suitable unique constraint/index.

Typically you'd have:

```sql
CREATE TABLE users (
    id BIGINT PRIMARY KEY,
    name TEXT
);
```

Then:

```text
users.id
    ↓
PRIMARY KEY
    ↓
unique
    ↓
can be referenced
```

A `UNIQUE` column can also be referenced.

For example:

```sql
CREATE TABLE users (
    id BIGINT PRIMARY KEY,
    email TEXT UNIQUE
);
```

Another table could reference:

```sql
REFERENCES users(email)
```

Although referencing a surrogate primary key such as `id` is often more common.

---

# 7. `FOREIGN KEY` does not automatically mean `NOT NULL`

This is a very important distinction.

Consider:

```sql
CREATE TABLE orders (
    id BIGINT PRIMARY KEY,
    user_id BIGINT REFERENCES users(id)
);
```

Can `user_id` be `NULL`?

Yes.

So:

```sql
INSERT INTO orders (id, user_id)
VALUES (101, NULL);
```

is allowed.

Why?

Because the foreign key rule says:

> If a value is provided, it must reference a valid user.

It doesn't necessarily say:

> A value must be provided.

If every order must belong to a user, use:

```sql
CREATE TABLE orders (
    id BIGINT PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id)
);
```

Now you have two separate rules:

```text
NOT NULL
    ↓
user_id must exist

FOREIGN KEY
    ↓
user_id must reference an existing user
```

---

# 8. Insert order matters

Consider:

```sql
CREATE TABLE users (
    id BIGINT PRIMARY KEY
);

CREATE TABLE orders (
    id BIGINT PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id)
);
```

This works:

```sql
INSERT INTO users (id)
VALUES (1);

INSERT INTO orders (id, user_id)
VALUES (101, 1);
```

But this fails:

```sql
INSERT INTO orders (id, user_id)
VALUES (101, 999);
```

because user `999` doesn't exist.

So generally:

```text
Create parent
      ↓
Create child referencing parent
```

---

# 9. What happens when we delete the parent?

This is where foreign keys become particularly interesting.

Suppose:

```text
users
---------
id
1

orders
---------
id    user_id
101   1
102   1
```

Now we execute:

```sql
DELETE FROM users
WHERE id = 1;
```

What should happen to orders `101` and `102`?

PostgreSQL needs a rule.

By default, the foreign key uses:

```text
NO ACTION
```

So PostgreSQL prevents the deletion if dependent rows still exist.

Conceptually:

```text
DELETE user 1
      |
      v
Are orders referencing user 1?
      |
     YES
      |
      v
Reject DELETE
```

This protects referential integrity.

---

# 10. `ON DELETE CASCADE`

Sometimes you actually want dependent rows to be deleted automatically.

Example:

```sql
CREATE TABLE orders (
    id BIGINT PRIMARY KEY,
    user_id BIGINT NOT NULL,

    FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE
);
```

Now:

```sql
DELETE FROM users
WHERE id = 1;
```

causes PostgreSQL to delete the referencing orders as well.

Conceptually:

```text
DELETE user 1
      |
      v
Find orders where user_id = 1
      |
      v
Delete those orders
      |
      v
Delete user
```

This can be useful for true dependent/owned data.

But you should understand the consequence carefully.

---

# 11. Why `CASCADE` can be dangerous

Imagine:

```text
User
 |
 +-- Orders
      |
      +-- Order Items
            |
            +-- Product Reviews
```

If cascading deletes are configured throughout the relationship graph, deleting one parent can cause many dependent records to disappear.

For example:

```text
DELETE user
     ↓
orders deleted
     ↓
order_items deleted
     ↓
other dependent data deleted
```

Therefore:

> `ON DELETE CASCADE` should represent an intentional ownership/dependency relationship, not simply be added everywhere for convenience.

---

# 12. `ON DELETE SET NULL`

Another option is:

> When the parent is deleted, keep the child row but set the foreign key to `NULL`.

Example:

```sql
CREATE TABLE orders (
    id BIGINT PRIMARY KEY,
    user_id BIGINT,

    FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE SET NULL
);
```

Suppose:

```text
Before:

users
---------
id
1

orders
---------
id    user_id
101   1
```

Delete user `1`.

After:

```text
users
---------
(empty)

orders
---------
id    user_id
101   NULL
```

This only makes sense when the relationship is optional.

Therefore:

```sql
user_id BIGINT
```

can work.

But:

```sql
user_id BIGINT NOT NULL
```

cannot be used with `ON DELETE SET NULL` in a way that would allow the resulting row to remain valid, because PostgreSQL can't set a `NOT NULL` column to `NULL`.

---

# 13. `ON DELETE SET DEFAULT`

Another option is:

```sql
ON DELETE SET DEFAULT
```

When the referenced row is deleted, PostgreSQL sets the foreign key to its column default.

For example:

```sql
CREATE TABLE orders (
    id BIGINT PRIMARY KEY,
    user_id BIGINT DEFAULT 0,

    FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE SET DEFAULT
);
```

However, the resulting default value must still satisfy the foreign key relationship.

If `user_id` becomes `0`, there must be a referenced row with:

```text
users.id = 0
```

Otherwise the referential-integrity rule is still violated.

---

# 14. `ON DELETE RESTRICT`

You can explicitly specify:

```sql
ON DELETE RESTRICT
```

This means:

> Don't allow deletion of the referenced row while dependent rows exist.

Example:

```sql
FOREIGN KEY (user_id)
REFERENCES users(id)
ON DELETE RESTRICT
```

This is useful when deleting the parent should require the application to deal with its dependencies explicitly.

---

# 15. `NO ACTION` vs `RESTRICT`

You'll often encounter both:

```text
NO ACTION
RESTRICT
```

They are similar but have an important difference around **deferrable constraints**.

In the normal, immediate-constraint case, both prevent a parent deletion that would leave an invalid reference.

Conceptually:

```text
RESTRICT
→ reject the operation when the conflict is encountered

NO ACTION
→ allow the check to be deferred if the constraint is configured as deferrable
```

We'll cover `DEFERRABLE` constraints later.

For now, the practical distinction is:

```text
NO ACTION
    ↓
default behavior

RESTRICT
    ↓
explicitly reject conflicting delete/update
```

---

# 16. All major `ON DELETE` behaviors

You should know this table well.

| Action        | What happens when parent is deleted?  |
| ------------- | ------------------------------------- |
| `NO ACTION`   | Reject if dependent rows remain       |
| `RESTRICT`    | Reject deletion when dependents exist |
| `CASCADE`     | Delete dependent rows                 |
| `SET NULL`    | Set foreign key to `NULL`             |
| `SET DEFAULT` | Set foreign key to its default        |

Example:

```sql
FOREIGN KEY (user_id)
REFERENCES users(id)
ON DELETE CASCADE
```

---

# 17. Foreign keys also matter for `UPDATE`

Foreign keys aren't only about deletion.

Suppose:

```text
users
---------
id
100

orders
---------
user_id
100
```

What if we execute:

```sql
UPDATE users
SET id = 200
WHERE id = 100;
```

Now the child row contains:

```text
orders.user_id = 100
```

but user `100` no longer exists.

PostgreSQL therefore needs an `ON UPDATE` rule.

For example:

```sql
FOREIGN KEY (user_id)
REFERENCES users(id)
ON UPDATE CASCADE
```

Then:

```text
users.id
100 → 200

orders.user_id
100 → 200
```

automatically.

Available actions are similar:

```text
NO ACTION
RESTRICT
CASCADE
SET NULL
SET DEFAULT
```

`NO ACTION` is the default.

---

# 18. A complete example

Let's build a simple ecommerce relationship.

```sql
CREATE TABLE customers (
    customer_id BIGINT GENERATED ALWAYS AS IDENTITY,
    name TEXT NOT NULL,

    CONSTRAINT pk_customers
        PRIMARY KEY (customer_id)
);
```

Then:

```sql
CREATE TABLE orders (
    order_id BIGINT GENERATED ALWAYS AS IDENTITY,
    customer_id BIGINT NOT NULL,

    CONSTRAINT pk_orders
        PRIMARY KEY (order_id),

    CONSTRAINT fk_orders_customer
        FOREIGN KEY (customer_id)
        REFERENCES customers(customer_id)
);
```

The relationship is:

```text
customers
+-------------+
| customer_id |
+-------------+
       ^
       |
       | FK
       |
+-------------+
| customer_id |
|   orders    |
+-------------+
```

Now:

```sql
INSERT INTO customers (name)
VALUES ('Alice');
```

Suppose PostgreSQL generates:

```text
customer_id = 1
```

Then:

```sql
INSERT INTO orders (customer_id)
VALUES (1);
```

works.

But:

```sql
INSERT INTO orders (customer_id)
VALUES (999);
```

fails.

---

# 19. One-to-many relationship

This is the most common use of foreign keys.

For example:

```text
One customer
     |
     +------ Order 1
     |
     +------ Order 2
     |
     +------ Order 3
```

Database representation:

```text
customers
-----------
id = 1

orders
-----------
id    customer_id
101   1
102   1
103   1
```

Notice:

```text
customers.id
```

is unique because it is a primary key.

But:

```text
orders.customer_id
```

is **not** unique.

That's what allows one customer to have many orders.

---

# 20. Foreign key does NOT mean one-to-one

This is a common misconception.

If you write:

```sql
customer_id BIGINT REFERENCES customers(id)
```

you have not said:

> One customer can have only one order.

You've only said:

> Every non-null `customer_id` must refer to an existing customer.

To enforce one-to-one, you'd additionally need:

```sql
customer_id BIGINT UNIQUE REFERENCES customers(id)
```

For example:

```sql
CREATE TABLE customer_profiles (
    profile_id BIGINT PRIMARY KEY,
    customer_id BIGINT UNIQUE NOT NULL
        REFERENCES customers(customer_id)
);
```

Now one customer can appear at most once in `customer_profiles`.

---

# 21. Foreign key and application validation

You might be tempted to write:

```java
if (userRepository.existsById(userId)) {
    orderRepository.save(order);
}
```

This can improve the user experience.

But it isn't sufficient as the database's integrity mechanism.

Consider concurrent operations:

```text
Request A                    Request B
---------                    ---------
check user 10 exists
                             delete user 10
create order for user 10
```

Application-level checking alone cannot guarantee that the relationship remains valid.

The foreign key provides database-level enforcement.

---

# 22. Foreign key mental model

Think of a foreign key as:

```text
                FOREIGN KEY
                     |
                     v
          "Does this referenced
             row actually exist?"
                     |
             +-------+-------+
             |               |
            YES              NO
             |               |
           allow            ERROR
```

For deletion:

```text
Parent row
    |
    v
Are child rows referencing it?
    |
    +---- No ----> Delete
    |
    +---- Yes ---> Apply ON DELETE rule
```

---

# 23. Important production detail: index the child foreign key

PostgreSQL does **not automatically create an index on the referencing foreign-key column**.

For example:

```sql
CREATE TABLE orders (
    id BIGINT PRIMARY KEY,
    customer_id BIGINT NOT NULL
        REFERENCES customers(id)
);
```

The foreign key is enforced, but PostgreSQL does not automatically create:

```text
INDEX ON orders(customer_id)
```

You may want one for performance, particularly when:

* querying all orders for a customer
* deleting/updating referenced parent rows
* joining parent and child tables frequently

For example:

```sql
CREATE INDEX idx_orders_customer_id
ON orders(customer_id);
```

This is an important distinction:

```text
PRIMARY KEY / UNIQUE
→ PostgreSQL creates supporting unique indexes

FOREIGN KEY
→ PostgreSQL does not automatically index the referencing column
```

---

# 24. The big picture

We now have three important constraints:

```text
PRIMARY KEY
     ↓
"What uniquely identifies this row?"

UNIQUE
     ↓
"Can another row have this value?"

FOREIGN KEY
     ↓
"Does this referenced row actually exist?"
```

Together:

```text
customers
+----------------+
| id PK          |
+----------------+
       ^
       |
       | FK
       |
orders
+----------------+
| id PK          |
| customer_id FK |
+----------------+
```

This gives us both:

* **Entity integrity** → primary key
* **Referential integrity** → foreign key

---

# Key takeaways

1. A foreign key maintains a relationship between tables.
2. It ensures a referencing value corresponds to a valid referenced value.
3. The referenced column must have appropriate uniqueness, typically a `PRIMARY KEY` or `UNIQUE` constraint.
4. A foreign key does not automatically imply `NOT NULL`.
5. `NOT NULL + FOREIGN KEY` means the relationship is mandatory.
6. A foreign key usually represents a one-to-many relationship.
7. Add `UNIQUE` to the foreign-key column if you want one-to-one semantics.
8. `ON DELETE` controls what happens when the referenced row is deleted.
9. `ON UPDATE` controls what happens when the referenced key changes.
10. `CASCADE` should be used deliberately because it can delete dependent data.
11. PostgreSQL does not automatically create an index on the referencing foreign-key column.
12. Foreign keys provide database-level referential integrity, not merely application-level validation.

## Next concept

**`CHECK` constraint** — how to enforce rules such as `price >= 0`, valid status values, date relationships, and more complex row-level business invariants.

