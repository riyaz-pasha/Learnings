# 11. Multi-column `UNIQUE` Constraints

We just learned about composite constraints. Now let's focus specifically on one of the most useful ones:

```sql
UNIQUE (column1, column2)
```

A multi-column `UNIQUE` constraint is used when:

> **A combination of columns must be unique, even though the individual columns don't need to be unique.**

---

# 1. Simple example

Suppose we have employees working in different departments:

```sql
CREATE TABLE employees (
    id BIGINT PRIMARY KEY,
    department_id BIGINT NOT NULL,
    employee_code TEXT NOT NULL,

    UNIQUE (department_id, employee_code)
);
```

The rule is:

> An employee code must be unique **within a department**.

This is allowed:

```text
department_id    employee_code
--------------------------------
10               E001
10               E002
20               E001
20               E002
```

Why?

Because the combinations are different:

```text
(10, E001)
(10, E002)
(20, E001)
(20, E002)
```

But this is not allowed:

```text
department_id    employee_code
--------------------------------
10               E001
10               E001   ← duplicate combination
```

---

# 2. Individual uniqueness vs combination uniqueness

This is the most important concept.

Consider:

```sql
UNIQUE (department_id, employee_code)
```

It does **not** mean:

```text
department_id must be unique
```

It does **not** mean:

```text
employee_code must be unique
```

It means:

```text
(department_id, employee_code)
must be unique
```

Think of it as a tuple:

```text
department_id + employee_code
              ↓
          unique key
```

---

# 3. Real-world example: usernames within organizations

Imagine a SaaS platform where each organization has its own users.

The same username can exist in different organizations:

```text
Organization A → admin
Organization B → admin
Organization C → admin
```

But within one organization:

```text
Organization A → admin
Organization A → admin
```

should not be allowed.

We can model this as:

```sql
CREATE TABLE users (
    id BIGINT PRIMARY KEY,
    organization_id BIGINT NOT NULL,
    username TEXT NOT NULL,

    UNIQUE (organization_id, username)
);
```

Now:

```text
organization_id    username
---------------------------
1                  admin     ✓
1                  john      ✓
2                  admin     ✓
2                  john      ✓
```

But:

```text
1    admin
1    admin
```

fails.

This pattern is extremely common in multi-tenant systems.

---

# 4. Another common example: seat booking

Suppose a movie theater has multiple shows.

A seat can be booked once **per show**.

You might have:

```sql
CREATE TABLE bookings (
    id BIGINT PRIMARY KEY,
    show_id BIGINT NOT NULL,
    seat_id BIGINT NOT NULL,

    UNIQUE (show_id, seat_id)
);
```

This means:

> A particular seat cannot be booked twice for the same show.

For example:

```text
show_id    seat_id
------------------
100        A1       ✓
100        A2       ✓
100        A3       ✓
101        A1       ✓
101        A2       ✓
```

Notice:

```text
seat_id = A1
```

appears multiple times.

That's fine because the show is different.

The combination:

```text
(show_id, seat_id)
```

is unique.

---

# 5. Why this is better than application validation

Imagine your application does:

```text
1. Check whether seat A1 is available.
2. If available, insert booking.
```

Two requests arrive simultaneously:

```text
Request A → Is A1 available? → YES
Request B → Is A1 available? → YES
```

Both applications may then try:

```text
INSERT booking
```

Without a database uniqueness constraint, both could succeed.

But with:

```sql
UNIQUE (show_id, seat_id)
```

the database guarantees:

```text
Only one row with this combination can exist.
```

So the database becomes the final enforcement point.

This is especially important for:

* bookings
* inventory
* usernames
* tenant-scoped resources
* schedules
* assignments
* memberships

---

# 6. Multi-column `UNIQUE` and `NULL`

Now we reach an important PostgreSQL/SQL detail.

Consider:

```sql
CREATE TABLE employees (
    department_id BIGINT,
    employee_code TEXT,

    UNIQUE (department_id, employee_code)
);
```

Because `UNIQUE` normally allows `NULL`, combinations involving `NULL` can behave differently from what beginners expect.

For example:

```text
department_id    employee_code
--------------------------------
10               NULL
10               NULL
```

can normally coexist under PostgreSQL's standard unique semantics.

Why?

Because `NULL` represents an unknown/missing value and isn't considered equal to another `NULL` in ordinary SQL uniqueness semantics.

---

# 7. If both columns are required

If your business rule is:

> Every department and employee code must be present, and the combination must be unique.

Use:

```sql
CREATE TABLE employees (
    department_id BIGINT NOT NULL,
    employee_code TEXT NOT NULL,

    UNIQUE (department_id, employee_code)
);
```

Now:

```text
NULL + E001
```

is rejected because of `NOT NULL`.

And:

```text
10 + E001
10 + E001
```

is rejected because of `UNIQUE`.

This is a common combination:

```text
NOT NULL
+
UNIQUE (a, b)
```

---

# 8. `UNIQUE NULLS NOT DISTINCT`

PostgreSQL provides:

```sql
UNIQUE NULLS NOT DISTINCT (a, b)
```

This changes the handling of `NULL`.

Example:

```sql
CREATE TABLE example (
    a INT,
    b INT,

    UNIQUE NULLS NOT DISTINCT (a, b)
);
```

Now PostgreSQL treats `NULL` values as equivalent for uniqueness purposes.

So combinations such as:

```text
a     b
---------
1     NULL
1     NULL
```

would violate the unique constraint.

This is useful when your business meaning says:

> Missing values should still participate in uniqueness as though `NULL` represents one common value.

But don't use it automatically. First determine what `NULL` means in your data model.

---

# 9. Three different designs

Suppose you have:

```text
organization_id
username
```

and want to control duplicates.

### Design A — username globally unique

```sql
username TEXT UNIQUE
```

Then:

```text
Org 1 → admin
Org 2 → admin   ✗
```

---

### Design B — username unique within organization

```sql
UNIQUE (organization_id, username)
```

Then:

```text
Org 1 → admin   ✓
Org 2 → admin   ✓
Org 1 → admin   ✗
```

---

### Design C — username not required to be unique

No unique constraint:

```text
Org 1 → admin   ✓
Org 1 → admin   ✓
```

So the constraint should reflect the **actual business rule**.

---

# 10. Order of columns

Consider:

```sql
UNIQUE (organization_id, username)
```

versus:

```sql
UNIQUE (username, organization_id)
```

From a pure uniqueness perspective, both enforce the same set of combinations.

For example, both prevent:

```text
(1, admin)
(1, admin)
```

But the order matters for the **supporting index and query performance**.

For example:

```sql
UNIQUE (organization_id, username)
```

creates an index ordered conceptually like:

```text
organization_id → username
```

This is particularly useful for queries such as:

```sql
SELECT *
FROM users
WHERE organization_id = 10;
```

and:

```sql
SELECT *
FROM users
WHERE organization_id = 10
  AND username = 'admin';
```

But a query only filtering by:

```sql
WHERE username = 'admin'
```

doesn't benefit in the same way from the leading `organization_id` column.

So when designing composite unique constraints, think about both:

```text
1. Data integrity
2. Query/index usage
```

---

# 11. Leftmost-prefix idea

Suppose you have:

```sql
UNIQUE (A, B, C)
```

The supporting index is ordered approximately as:

```text
A → B → C
```

It can efficiently support lookups involving the leading portion, such as:

```text
A
A + B
A + B + C
```

But a query involving only:

```text
B
```

doesn't get the same benefit from this index.

This is commonly referred to as the **leftmost-prefix** principle for B-tree indexes.

Don't confuse this with the uniqueness rule itself.

The uniqueness rule is:

```text
(A, B, C) must be unique
```

The column ordering is additionally relevant to how the supporting index is used.

---

# 12. Multi-column unique constraint vs unique index

You can enforce uniqueness with:

```sql
UNIQUE (organization_id, username)
```

or create a unique index:

```sql
CREATE UNIQUE INDEX ...
```

They are related, but conceptually different.

### Constraint

```sql
ALTER TABLE users
ADD CONSTRAINT uq_users_org_username
UNIQUE (organization_id, username);
```

You are explicitly expressing:

> This is a data-integrity rule.

### Unique index

```sql
CREATE UNIQUE INDEX ...
```

You are explicitly creating an index that enforces uniqueness.

For ordinary business uniqueness, a `UNIQUE` constraint is generally the clearer expression of intent.

Unique indexes become especially useful for more advanced cases, such as **partial uniqueness**.

---

# 13. Conditional uniqueness

Here's an interesting real-world case.

Suppose a system allows multiple historical usernames, but only one active username assignment.

You could use a partial unique index:

```sql
CREATE UNIQUE INDEX uq_active_username
ON users (organization_id, username)
WHERE is_active = TRUE;
```

This means:

> Among active rows, `(organization_id, username)` must be unique.

For example:

```text
organization    username    active
-----------------------------------
1               john        TRUE
1               john        FALSE
1               john        FALSE
```

can be valid.

But:

```text
1    john    TRUE
1    john    TRUE
```

is rejected.

This is technically a **unique index**, rather than a regular table-level `UNIQUE` constraint.

It demonstrates how PostgreSQL can express more specialized uniqueness rules.

---

# 14. Another real-world example: employee assignments

Suppose employees can be assigned to projects:

```sql
CREATE TABLE project_assignments (
    project_id BIGINT NOT NULL,
    employee_id BIGINT NOT NULL,
    role TEXT NOT NULL,

    UNIQUE (project_id, employee_id)
);
```

This means:

> An employee can be assigned to a project only once.

So:

```text
project    employee
-------------------
1          101      ✓
1          102      ✓
2          101      ✓
```

is valid.

But:

```text
1    101
1    101
```

is not.

If the employee can have multiple roles on the same project, then this rule might instead be:

```sql
UNIQUE (project_id, employee_id, role)
```

Now the business meaning has changed to:

> The same employee can have multiple roles on a project, but cannot have the same role twice.

For example:

```text
project    employee    role
---------------------------
1          101         Developer
1          101         Reviewer
```

is valid.

But:

```text
1    101    Developer
1    101    Developer
```

is invalid.

This illustrates a powerful design principle:

> **The columns inside a composite unique constraint define exactly what constitutes a duplicate.**

---

# 15. Multi-column `UNIQUE` and foreign keys

A composite unique constraint can also provide a key that another table references.

For example:

```sql
CREATE TABLE products (
    tenant_id BIGINT NOT NULL,
    sku TEXT NOT NULL,

    UNIQUE (tenant_id, sku)
);
```

Then another table can reference that combination:

```sql
CREATE TABLE order_items (
    id BIGINT PRIMARY KEY,

    tenant_id BIGINT NOT NULL,
    sku TEXT NOT NULL,

    FOREIGN KEY (tenant_id, sku)
        REFERENCES products(tenant_id, sku)
);
```

Relationship:

```text
order_items
(tenant_id, sku)
       │
       ▼
products
(tenant_id, sku)
```

This is useful in multi-tenant databases where identifiers are only unique within a tenant.

---

# 16. Naming the constraint

For production schemas, naming composite constraints can make migrations and debugging easier.

Instead of:

```sql
UNIQUE (organization_id, username)
```

you can write:

```sql
CONSTRAINT uq_users_organization_username
UNIQUE (organization_id, username)
```

Example:

```sql
CREATE TABLE users (
    id BIGINT PRIMARY KEY,

    organization_id BIGINT NOT NULL,
    username TEXT NOT NULL,

    CONSTRAINT uq_users_organization_username
        UNIQUE (organization_id, username)
);
```

Now if the constraint is violated, the database error can identify the meaningful constraint name.

A naming convention might be:

```text
uq_<table>_<columns>
```

For example:

```text
uq_users_organization_username
uq_order_items_order_product
uq_employee_department_code
```

---

# 17. Important distinction: `UNIQUE` doesn't find "the closest duplicate"

Suppose:

```sql
UNIQUE (organization_id, username)
```

and you insert:

```text
organization_id = 10
username = 'john'
```

If `(10, 'john')` already exists, the database rejects the operation.

It doesn't:

* update the existing row
* merge the rows
* choose one row
* automatically rename the username

It simply enforces the integrity rule.

Your application can then decide what to do with the resulting conflict.

---

# 18. `ON CONFLICT` works nicely with composite uniqueness

PostgreSQL's `INSERT ... ON CONFLICT` can use a unique constraint as the conflict target.

Suppose:

```sql
CREATE TABLE users (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    organization_id BIGINT NOT NULL,
    username TEXT NOT NULL,

    UNIQUE (organization_id, username)
);
```

You can write:

```sql
INSERT INTO users (organization_id, username)
VALUES (10, 'john')
ON CONFLICT (organization_id, username)
DO NOTHING;
```

The database recognizes:

```text
(organization_id, username)
```

as the unique conflict key.

This is particularly useful for idempotent operations and concurrency-safe inserts.

---

# 19. Common mistake

Requirement:

> A user can use the same username in different organizations, but not twice in the same organization.

Someone writes:

```sql
username TEXT UNIQUE
```

That's too restrictive.

It produces:

```text
Org 1 → john ✓
Org 2 → john ✗
```

But the requirement says that should be allowed.

Correct:

```sql
UNIQUE (organization_id, username)
```

Now:

```text
Org 1 → john ✓
Org 2 → john ✓
Org 1 → john ✗
```

---

# 20. Another common mistake

Requirement:

> A seat can be booked once for each show.

Someone writes:

```sql
seat_id BIGINT UNIQUE
```

This means a seat can only ever appear once in the entire table.

That's wrong if the same physical seat is reused for different shows.

Correct:

```sql
UNIQUE (show_id, seat_id)
```

Now:

```text
Show 1 → Seat A1 ✓
Show 2 → Seat A1 ✓
Show 3 → Seat A1 ✓

Show 1 → Seat A1 again ✗
```

---

# 21. How to identify the correct columns

When designing a multi-column unique constraint, ask:

> **What exactly constitutes a duplicate in the business domain?**

For example:

### Seat booking

```text
Duplicate = same show + same seat
```

Therefore:

```sql
UNIQUE (show_id, seat_id)
```

### Tenant username

```text
Duplicate = same tenant + same username
```

Therefore:

```sql
UNIQUE (tenant_id, username)
```

### Employee department code

```text
Duplicate = same department + same employee code
```

Therefore:

```sql
UNIQUE (department_id, employee_code)
```

### Product SKU within tenant

```text
Duplicate = same tenant + same SKU
```

Therefore:

```sql
UNIQUE (tenant_id, sku)
```

This way of thinking is more important than memorizing SQL syntax.

---

# 22. Mental model

Think of a unique constraint as defining a **duplicate detector**.

For:

```sql
UNIQUE (A, B, C)
```

PostgreSQL asks:

```text
Does another row already have:

A = same value
AND
B = same value
AND
C = same value?
```

If yes:

```text
❌ duplicate
```

If no:

```text
✅ allowed
```

For example:

```text
Existing:

tenant    username
------------------
10        alice
20        alice
```

New:

```text
10        alice
```

Comparison:

```text
tenant:   10 = 10     ✓
username: alice = alice ✓
```

Therefore:

```text
❌ duplicate
```

But:

```text
20        bob
```

doesn't match an existing combination:

```text
tenant:   20
username: bob
```

so:

```text
✅ allowed
```

---

# Key takeaways

### 1. Multi-column uniqueness applies to the combination

```sql
UNIQUE (A, B)
```

means:

```text
(A, B) must be unique
```

not:

```text
A must be unique
B must be unique
```

### 2. Individual columns can repeat

```text
A = 1, B = X
A = 1, B = Y
A = 2, B = X
```

can all be valid.

### 3. `NOT NULL` is separate

If both values are mandatory:

```sql
A NOT NULL,
B NOT NULL,
UNIQUE (A, B)
```

### 4. Column order matters for the supporting index

```sql
UNIQUE (A, B)
```

and:

```sql
UNIQUE (B, A)
```

enforce the same combinations but can have different query-performance characteristics.

### 5. The constraint should represent the definition of a duplicate

Ask:

> **"When should two rows be considered duplicates?"**

Then put exactly those columns in the `UNIQUE` constraint.

---

## Next concept

**12. `CHECK` Constraints with Real Business Rules**


