# Isolation

- Isolations ensures that concurrent transactions do not interfere with each other.
- For example, if one transactions making serveral writes then other transaction should see either all updates or none, but not some subset.


- This prevents problems like
  
### Dirty Reads
- Imagine there are two txns running in parallel and 1 is updating a record and haven't commited it yet. And other one reading the same record and read the uncommited changes. This is called **Dirty Reads**

Example:

- T1 writes to a Row.
- T2 reads the same row before T1 commits.
- If T1 rolls back, T2 read invalid data.

### Non-Repeatable Reads
- Reading the same row twice in a transaction and getting different Results.

Example:
- T1 reads a row.
- T2 modifies the same row and commits the data.
- T1 reds the row again and sees the new data.

### Phantom Read
- New rows appear in a subsequent query in the same transaction
  
Example:
- T1 queries for rows with `WHERE age>30`.
- T2 insert a row with `age = 35` and commits.
- T1 runs the same query again and sees a new row.

### Lost update
- Two transactions read the same row and both updates it - one overwrites the other's work.

Example:
- T1 reads salary = 5000
- T2 reads salary = 5000
- T1 sets salary = salary + 500 → 5500
- T2 sets salary = salary + 1000 → 6000
- Final: salary = 6000 ❌ (T1's update lost)


---

### Read Committed

- prevents dirty reads


```sql
SET TRANSACTION ISOLATION LEVEL READ COMMITTED;
BEGIN;
SELECT * FROM accounts WHERE id = 1;  -- safe
COMMIT;
```

- This can be solved in two ways

1. version control (PostgreSQL, oracle)
   1. when a txn updates a row , old version is not overwritten.
   2. A new version of the row is created.
   3. Other txns sees only commited data
2. Read locks or Row level locking (SQL server. pessimistic concurrency)


---

### Repeatable Read


---

### Serializable
