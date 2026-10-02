# 13. `DEFERRABLE` Constraints

This is one of the more advanced PostgreSQL integrity concepts.

The basic idea is:

> A `DEFERRABLE` constraint can have its validation postponed until later, usually until the end of the transaction.

Normally, PostgreSQL checks constraints **immediately** when an `INSERT`, `UPDATE`, or `DELETE` statement runs.

With a deferrable constraint, we can say:

> "Don't check this constraint right now. Check it when the transaction is committed."

---

# 1. Why would we need this?

Consider two tables:

```sql
CREATE TABLE employees (
    id INT PRIMARY KEY,
    name TEXT NOT NULL,
    manager_id INT
);
```

Suppose we want:

```text
employee.manager_id → employees.id
```

So an employee can reference another employee as their manager.

```sql
ALTER TABLE employees
ADD CONSTRAINT fk_manager
FOREIGN KEY (manager_id)
REFERENCES employees(id);
```

Now imagine we want to insert:

```text
Alice → manager = Bob
Bob   → manager = Alice
```

We have a circular relationship.

---

# 2. The normal problem

Suppose the table is initially empty.

We try:

```sql
INSERT INTO employees (id, name, manager_id)
VALUES (1, 'Alice', 2);
```

PostgreSQL immediately checks:

```text
manager_id = 2
        ↓
Does employee 2 exist?
        ↓
       NO
        ↓
     REJECT
```

But we haven't had a chance to insert Bob yet.

We could insert Bob first:

```sql
INSERT INTO employees (id, name, manager_id)
VALUES (2, 'Bob', 1);
```

But now Alice doesn't exist.

So:

```text
Insert Alice first → Bob doesn't exist ❌
Insert Bob first   → Alice doesn't exist ❌
```

Yet the **final state** we want is perfectly valid:

```text
Alice → Bob
Bob   → Alice
```

This is where deferred constraint checking becomes useful.

---

# 3. `DEFERRABLE`

We can declare the foreign key:

```sql
CREATE TABLE employees (
    id INT PRIMARY KEY,
    name TEXT NOT NULL,
    manager_id INT,

    CONSTRAINT fk_manager
        FOREIGN KEY (manager_id)
        REFERENCES employees(id)
        DEFERRABLE
);
```

Now the FK is allowed to be deferred.

But there's an important distinction.

`DEFERRABLE` means:

> This constraint **can** be deferred.

It doesn't necessarily mean:

> Always defer it.

---

# 4. `INITIALLY IMMEDIATE`

By default, a deferrable constraint is still checked immediately.

```sql
CONSTRAINT fk_manager
    FOREIGN KEY (manager_id)
    REFERENCES employees(id)
    DEFERRABLE INITIALLY IMMEDIATE
```

Behavior:

```text
INSERT
  ↓
Check constraint immediately
```

But within a transaction, we can explicitly tell PostgreSQL:

```sql
SET CONSTRAINTS fk_manager DEFERRED;
```

Then:

```text
INSERT
  ↓
Don't check FK yet
  ↓
Continue transaction
  ↓
COMMIT
  ↓
Check FK
```

---

# 5. `INITIALLY DEFERRED`

We can instead define:

```sql
CONSTRAINT fk_manager
    FOREIGN KEY (manager_id)
    REFERENCES employees(id)
    DEFERRABLE INITIALLY DEFERRED
```

Now the constraint starts each transaction in deferred mode.

Conceptually:

```text
BEGIN
  ↓
Constraint is deferred
  ↓
INSERT / UPDATE
  ↓
Constraint isn't checked immediately
  ↓
COMMIT
  ↓
Constraint checked
```

---

# 6. Complete example

Let's create the table:

```sql
CREATE TABLE employees (
    id INT PRIMARY KEY,
    name TEXT NOT NULL,
    manager_id INT,

    CONSTRAINT fk_manager
        FOREIGN KEY (manager_id)
        REFERENCES employees(id)
        DEFERRABLE INITIALLY DEFERRED
);
```

Now:

```sql
BEGIN;

INSERT INTO employees (id, name, manager_id)
VALUES (1, 'Alice', 2);

INSERT INTO employees (id, name, manager_id)
VALUES (2, 'Bob', 1);

COMMIT;
```

During the inserts:

```text
Alice → Bob
```

Bob doesn't exist yet.

Then:

```text
Bob → Alice
```

Alice already exists.

At `COMMIT`, both rows exist:

```text
1 Alice → 2
2 Bob   → 1
```

The FK is satisfied.

Therefore the transaction can commit.

---

# 7. What if the final state is invalid?

Suppose:

```sql
BEGIN;

INSERT INTO employees (id, name, manager_id)
VALUES (1, 'Alice', 999);

COMMIT;
```

At commit:

```text
manager_id = 999
        ↓
Does employee 999 exist?
        ↓
       NO
        ↓
Constraint violation
        ↓
Transaction cannot commit
```

So deferred checking does **not** remove the integrity rule.

It only changes **when** the rule is checked.

This is the most important idea.

---

# 8. Immediate vs deferred

Compare:

### Immediate

```text
INSERT
  ↓
Check constraint
  ↓
Pass → continue
Fail → error
```

### Deferred

```text
INSERT
  ↓
Continue
  ↓
More INSERT/UPDATE/DELETE
  ↓
COMMIT
  ↓
Check constraint
  ↓
Pass → commit
Fail → rollback/fail
```

So:

> `DEFERRABLE` changes the timing of validation, not the rule itself.

---

# 9. `SET CONSTRAINTS`

You can change the checking mode inside a transaction.

Example:

```sql
BEGIN;

SET CONSTRAINTS fk_manager DEFERRED;

INSERT INTO employees (id, name, manager_id)
VALUES (1, 'Alice', 2);

INSERT INTO employees (id, name, manager_id)
VALUES (2, 'Bob', 1);

COMMIT;
```

You can also switch it back:

```sql
SET CONSTRAINTS fk_manager IMMEDIATE;
```

When switching from `DEFERRED` to `IMMEDIATE`, PostgreSQL checks outstanding violations at that point.

For example:

```sql
BEGIN;

SET CONSTRAINTS fk_manager DEFERRED;

-- temporary inconsistent state
INSERT INTO employees (id, name, manager_id)
VALUES (1, 'Alice', 2);

-- Now force the check
SET CONSTRAINTS fk_manager IMMEDIATE;
```

If employee `2` doesn't exist, the `SET CONSTRAINTS` statement can fail.

---

# 10. `DEFERRABLE` vs `INITIALLY DEFERRED`

These names are easy to confuse.

### `DEFERRABLE`

Means:

> You are allowed to defer this constraint.

Example:

```sql
DEFERRABLE INITIALLY IMMEDIATE
```

Starts immediate, but you can do:

```sql
SET CONSTRAINTS fk_manager DEFERRED;
```

---

### `INITIALLY DEFERRED`

Means:

> Start the transaction with this constraint deferred.

Example:

```sql
DEFERRABLE INITIALLY DEFERRED
```

You don't need to explicitly call:

```sql
SET CONSTRAINTS ... DEFERRED
```

---

# 11. Three important modes

Think of these as:

| Definition                       | Can defer? | Initial behavior |
| -------------------------------- | ---------: | ---------------- |
| `NOT DEFERRABLE`                 |         No | Immediate        |
| `DEFERRABLE INITIALLY IMMEDIATE` |        Yes | Immediate        |
| `DEFERRABLE INITIALLY DEFERRED`  |        Yes | Deferred         |

---

# 12. Which constraints support `DEFERRABLE`?

In PostgreSQL, deferrability is relevant to certain constraint types, notably:

* `UNIQUE`
* `PRIMARY KEY`
* `FOREIGN KEY`
* `EXCLUDE`

For example:

```sql
CREATE TABLE users (
    id INT,

    CONSTRAINT pk_users
        PRIMARY KEY (id)
        DEFERRABLE
);
```

And:

```sql
CREATE TABLE child (
    id INT PRIMARY KEY,
    parent_id INT,

    CONSTRAINT fk_parent
        FOREIGN KEY (parent_id)
        REFERENCES parent(id)
        DEFERRABLE
);
```

A regular `CHECK` constraint is not made deferrable in PostgreSQL.

Likewise, `NOT NULL` isn't something you can defer.

---

# 13. A surprising use case: swapping unique values

Here's another interesting example.

Suppose:

```sql
CREATE TABLE users (
    id INT PRIMARY KEY,
    username TEXT UNIQUE
);
```

We have:

```text
id | username
---+---------
1  | alice
2  | bob
```

Now we want:

```text
1 → bob
2 → alice
```

If we try:

```sql
UPDATE users
SET username = 'bob'
WHERE id = 1;
```

PostgreSQL sees:

```text
bob already belongs to id = 2
```

So the unique constraint is violated.

We could solve this with multiple updates or a temporary value.

But a deferrable unique constraint gives another approach.

```sql
CREATE TABLE users (
    id INT PRIMARY KEY,

    username TEXT,

    CONSTRAINT uq_username
        UNIQUE (username)
        DEFERRABLE INITIALLY DEFERRED
);
```

Then:

```sql
BEGIN;

UPDATE users
SET username = CASE id
    WHEN 1 THEN 'bob'
    WHEN 2 THEN 'alice'
END
WHERE id IN (1, 2);

COMMIT;
```

During the statement/transaction there can temporarily be a duplicate.

At the final constraint check:

```text
alice
bob
```

are unique again.

Therefore the transaction can succeed.

This illustrates a powerful concept:

> A transaction can temporarily pass through an invalid intermediate state as long as the final state satisfies deferred constraints.

---

# 14. Why transactions matter here

`DEFERRABLE` makes sense primarily in combination with transactions.

Think about:

```text
BEGIN
   ↓
Step 1
   ↓
Temporary inconsistency
   ↓
Step 2
   ↓
Temporary inconsistency
   ↓
Step 3
   ↓
Valid final state
   ↓
COMMIT
```

The database only needs the invariant to be satisfied at the required checking point.

Without a transaction, there's no useful "temporary state" across multiple operations.

---

# 15. Very important: not every constraint can be deferred

Don't think:

> "I can make any PostgreSQL constraint deferred."

You cannot.

For example:

```sql
name TEXT NOT NULL
```

is always enforced immediately.

You cannot say:

```sql
NOT NULL DEFERRABLE
```

Similarly, normal `CHECK` constraints aren't deferred.

So if you have:

```sql
price NUMERIC NOT NULL CHECK (price >= 0)
```

you cannot temporarily have:

```text
price = NULL
```

or:

```text
price = -10
```

and expect the constraint to wait until `COMMIT`.

---

# 16. Why would a database allow temporary inconsistency?

At first this may seem dangerous.

But consider a transaction:

```text
Initial state:

A → B

We want:

B → A
```

There might be no way to move directly from the first valid state to the second valid state while satisfying an immediate constraint after every individual statement.

The transaction provides an atomic boundary:

```text
Before transaction
        ↓
temporary states
        ↓
desired valid state
        ↓
COMMIT
```

If the transaction fails:

```text
ROLLBACK
```

and the temporary states disappear.

So other transactions don't simply see your uncommitted intermediate state.

---

# 17. Real-world situations

Deferrable constraints can be useful for:

### Circular relationships

```text
Employee A → Employee B
Employee B → Employee A
```

### Complex data migrations

You may need to temporarily violate an FK while restructuring data.

### Reordering unique values

For example:

```text
1 → position 1
2 → position 2
3 → position 3
```

and you want to rotate/reorder them without temporary uniqueness conflicts.

### Complex transactional workflows

Where several related rows need to be changed together and only the final state needs to satisfy certain constraints.

---

# 18. `NO ACTION` and `DEFERRABLE`

This connects directly to the foreign-key actions we learned earlier.

Remember:

```sql
ON DELETE NO ACTION
```

and:

```sql
ON DELETE RESTRICT
```

are similar under immediate checking.

But there's an important difference:

> `NO ACTION` can work with deferred constraint checking; `RESTRICT` cannot be deferred.

For example:

```sql
FOREIGN KEY (parent_id)
REFERENCES parent(id)
ON DELETE NO ACTION
DEFERRABLE INITIALLY DEFERRED
```

This allows PostgreSQL to wait until the appropriate checking point.

This is one reason the distinction between `NO ACTION` and `RESTRICT` matters.

---

# 19. The key distinction

Don't confuse:

```text
DEFERRABLE
```

with:

```text
DISABLED
```

A deferred constraint is **not disabled**.

Suppose:

```text
Constraint:
order.customer_id must reference customers.id
```

During the transaction:

```text
customer 100 doesn't exist yet
```

That's temporarily tolerated.

But at the checking point:

```text
customer 100 still doesn't exist
```

→ transaction fails.

So:

```text
DEFERRABLE
    =
"Check later"

NOT

"Don't check"
```

---

# 20. Interview questions

### Q1. What does `DEFERRABLE` mean?

It means the constraint can have its validation postponed until a later point, typically the end of the transaction.

---

### Q2. What is the difference between `DEFERRABLE` and `INITIALLY DEFERRED`?

`DEFERRABLE` means the constraint is allowed to be deferred.

`INITIALLY DEFERRED` means it starts the transaction in deferred mode.

---

### Q3. What does `INITIALLY IMMEDIATE` mean?

The constraint is checked immediately by default, but because it's deferrable, you can change it to deferred:

```sql
SET CONSTRAINTS constraint_name DEFERRED;
```

---

### Q4. Does deferred checking mean the constraint is disabled?

No.

The final state must still satisfy the constraint.

---

### Q5. Can `NOT NULL` be deferred?

No.

---

### Q6. Can a `CHECK` constraint be deferred?

Not as a normal PostgreSQL `CHECK` constraint.

---

### Q7. Why are deferrable constraints useful?

They allow a transaction to temporarily pass through states that violate certain constraints while ensuring that the final state satisfies the integrity rules.

---

# Mental Model

Think of normal constraints as:

```text
Statement
   ↓
CHECK NOW
   ↓
Pass / Fail
```

Deferrable constraints can be:

```text
Statement
   ↓
Don't check yet
   ↓
More statements
   ↓
COMMIT
   ↓
CHECK
   ↓
Pass / Fail
```

The simplest definition to remember is:

> **`DEFERRABLE` controls when a constraint must be satisfied, not whether it must be satisfied.**

### Next concept

**14. Constraint Naming and Database Design Practices** — how to name constraints consistently, why names matter for debugging/migrations, and practical PostgreSQL naming conventions.

