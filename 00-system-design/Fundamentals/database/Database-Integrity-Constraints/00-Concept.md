**Referential integrity** is a relational database rule that ensures relationships between tables remain consistent and valid. It requires that every value in a child table’s **foreign key** must correspond to an existing **primary key** in the parent table, or else be set to `NULL`.

---

---

In the diagram above, `PROJECT EMPLOYEE` references `EMPLOYEE` via `employee id` and `PROJECT` via `project id`. Referential integrity guarantees you cannot create an assignment in `PROJECT EMPLOYEE` using an `employee id` that does not exist in the `EMPLOYEE` table.

## Why It Matters: Preventing Orphan Records

Without referential integrity, databases risk creating **orphaned records**—child rows with invalid or dangling references:

* An order in an `Orders` table referencing a `customer_id` that was never created or was accidentally deleted.
* Line items in an invoice pointing to a deleted product ID.
* Timecard logs linked to a nonexistent employee ID.

Enforcing these constraints at the database level ensures data consistency cannot be broken by application bugs, manual edits, or race conditions.

## How the Database Enforces It

When a foreign key constraint is enabled, the database engine validates every write operation:

* **INSERT:** Rejects any new row in the child table whose foreign key value does not match an existing parent primary key.
* **UPDATE:** Rejects updates that introduce non-existent foreign keys, as well as changes to a parent's primary key if dependent child rows exist.
* **DELETE:** Blocks the removal of a parent row if child rows still reference it, unless an automated deletion policy is defined.

## Common Referential Actions

When parent records are updated or deleted, databases use explicit rules to maintain integrity:

| Action | Behavior When Parent Record is Deleted or Changed |
| --- | --- |
| **RESTRICT / NO ACTION** | Prevents the delete or update on the parent if dependent child rows exist. |
| **CASCADE** | Automatically deletes or updates the corresponding child rows to match the parent. |
| **SET NULL** | Sets the foreign key in child rows to `NULL` (requires the foreign key column to be nullable). |
| **SET DEFAULT** | Replaces the foreign key in child rows with a defined default value. |

---

---

To understand how referential integrity behaves in practice, consider two tables: a parent table (`authors`) and a child table (`books`).

```sql
CREATE TABLE authors (
    author_id INT PRIMARY KEY,
    name VARCHAR(100) NOT NULL
);

```

Every referential action determines what happens to `books` when a row in `authors` is inserted, updated, or deleted.

---

### Scenario 1: Preventing Invalid Child Inserts (Baseline Foreign Key Rule)

Before addressing deletes or updates, the core foreign key rule ensures no child record can reference a non-existent parent.

```sql
CREATE TABLE books (
    book_id INT PRIMARY KEY,
    title VARCHAR(100) NOT NULL,
    author_id INT REFERENCES authors(author_id)
);

```

* **Existing Data:** `authors` contains only `author_id = 1` ("Frank Herbert").
* **Attempted Action:**
```sql
INSERT INTO books (book_id, title, author_id) 
VALUES (101, 'Foundation', 99);

```


* **Result:** **Blocked (Error).** The engine checks `authors` for `author_id = 99`. Because it does not exist, the insert fails with a foreign key violation error.

---

### Scenario 2: `ON DELETE CASCADE` (Automatic Cleanup)

With `CASCADE`, deleting a parent record automatically deletes all child records linked to it.

```sql
CREATE TABLE books (
    book_id INT PRIMARY KEY,
    title VARCHAR(100) NOT NULL,
    author_id INT,
    CONSTRAINT fk_author FOREIGN KEY (author_id) 
        REFERENCES authors(author_id) 
        ON DELETE CASCADE
        ON UPDATE CASCADE
);

```

#### Walkthrough

* **Initial State:**
* `authors`: `(1, 'Frank Herbert')`
* `books`: `(101, 'Dune', 1)`, `(102, 'Dune Messiah', 1)`


* **Action:**
```sql
DELETE FROM authors WHERE author_id = 1;

```


* **Result:** Both books `101` and `102` are **automatically deleted** from `books` alongside the author.
* **On Update:** If you run `UPDATE authors SET author_id = 5 WHERE author_id = 1;`, both books in `books` automatically have their `author_id` updated to `5`.
* **Best Used For:** Dependent child entities that have no independent purpose without the parent (e.g., `order_items` belonging to an `order`, or `comments` on a `post`).

---

### Scenario 3: `ON DELETE RESTRICT` (Strict Protection)

`RESTRICT` forbids the deletion or update of a parent record as long as any child record references it.

```sql
CREATE TABLE books (
    book_id INT PRIMARY KEY,
    title VARCHAR(100) NOT NULL,
    author_id INT,
    CONSTRAINT fk_author FOREIGN KEY (author_id) 
        REFERENCES authors(author_id) 
        ON DELETE RESTRICT
);

```

#### Walkthrough

* **Initial State:**
* `authors`: `(1, 'Frank Herbert')`
* `books`: `(101, 'Dune', 1)`


* **Action:**
```sql
DELETE FROM authors WHERE author_id = 1;

```


* **Result:** **Blocked (Error).** The database raises an error: `update or delete on table "authors" violates foreign key constraint`. The parent row remains untouched.
* **How to delete Herbert:** You must first reassign or delete the child record manually before deleting the author:
```sql
DELETE FROM books WHERE author_id = 1;
DELETE FROM authors WHERE author_id = 1;

```


* **Best Used For:** Critical records that must never be deleted accidentally (e.g., deleting a `customer` who has active `invoices` or `orders`).

---

### Scenario 4: `ON DELETE NO ACTION` (Immediate vs. Deferred Checks)

`NO ACTION` is the default standard SQL behavior. In most databases (like MySQL), it behaves identically to `RESTRICT`. In PostgreSQL and Oracle, `NO ACTION` allows **deferred constraint checks** within a transaction.

* **Standard `RESTRICT`:** Checks the constraint immediately when the `DELETE` statement executes.
* **`NO ACTION` (with `DEFERRABLE`):** Postpones the constraint check until the entire transaction commits.

#### Walkthrough (PostgreSQL Example)

```sql
CREATE TABLE books (
    book_id INT PRIMARY KEY,
    title VARCHAR(100) NOT NULL,
    author_id INT REFERENCES authors(author_id) 
        ON DELETE NO ACTION 
        DEFERRABLE INITIALLY DEFERRED
);

```

Inside a transaction, you can delete the author, insert a new author with the same ID, and commit without triggering an error—as long as the reference is valid by the time the transaction finishes:

```sql
BEGIN;
DELETE FROM authors WHERE author_id = 1;
-- The check is deferred, so the transaction doesn't fail here yet.
INSERT INTO authors (author_id, name) VALUES (1, 'Herbert Estate');
COMMIT; -- Integrity check runs now: PASSES.

```

---

### Scenario 5: `ON DELETE SET NULL` (Preserve Child, Detach Parent)

When the parent is removed, the child row stays in the table, but its foreign key column is updated to `NULL`.

> **Prerequisite:** The child foreign key column **must not** have a `NOT NULL` constraint, or the delete will fail at runtime.

```sql
CREATE TABLE books (
    book_id INT PRIMARY KEY,
    title VARCHAR(100) NOT NULL,
    author_id INT NULL, -- Must allow NULLs
    CONSTRAINT fk_author FOREIGN KEY (author_id) 
        REFERENCES authors(author_id) 
        ON DELETE SET NULL
);

```

#### Walkthrough

* **Initial State:**
* `authors`: `(1, 'Frank Herbert')`
* `books`: `(101, 'Dune', 1)`


* **Action:**
```sql
DELETE FROM authors WHERE author_id = 1;

```


* **Result:** Author `1` is deleted. Book `101` remains in the database with `author_id = NULL`:
* `books`: `(101, 'Dune', NULL)`


* **Best Used For:** Optional relationships where child records have independent value (e.g., deleting an `employee` account should detach them from historical `tickets` without deleting the tickets themselves).

---

### Scenario 6: `ON DELETE SET DEFAULT` (Fallback Assignment)

When the parent record is deleted, the foreign key column in the child table resets to its predefined column default.

> **Prerequisite:** The default value specified **must already exist** in the parent table; otherwise, the delete violates referential integrity when trying to apply the default.

```sql
-- Ensure default fallback author exists first
INSERT INTO authors (author_id, name) VALUES (0, 'Unknown / Anonymous');

CREATE TABLE books (
    book_id INT PRIMARY KEY,
    title VARCHAR(100) NOT NULL,
    author_id INT DEFAULT 0,
    CONSTRAINT fk_author FOREIGN KEY (author_id) 
        REFERENCES authors(author_id) 
        ON DELETE SET DEFAULT
);

```

#### Walkthrough

* **Initial State:**
* `authors`: `(0, 'Unknown / Anonymous')`, `(1, 'Frank Herbert')`
* `books`: `(101, 'Dune', 1)`


* **Action:**
```sql
DELETE FROM authors WHERE author_id = 1;

```


* **Result:** Author `1` is removed. Book `101` automatically updates its `author_id` to `0`:
* `books`: `(101, 'Dune', 0)`


* **Best Used For:** Systems that disallow `NULL` values and require orphaned records to be owned by a designated system account or generic queue.

---

### Summary Matrix

| Action | What Happens to Child When Parent is Deleted | Child Column Requirements | Common Use Case |
| --- | --- | --- | --- |
| **CASCADE** | Child rows are deleted | None | Parent-child ownership (`Order` → `LineItems`) |
| **RESTRICT** | Deletion is blocked immediately | None | Preventing data loss (`Customer` → `Invoices`) |
| **NO ACTION** | Blocked immediately or at commit time | None | Complex transactions with deferred checks |
| **SET NULL** | Foreign key becomes `NULL` | Must be nullable (`NULL`) | Optional links (`Author` deleted, keep `Post`) |
| **SET DEFAULT** | Foreign key resets to default value | Default ID must exist in parent | System fallbacks (`Task` assigned to `Unassigned`) |

