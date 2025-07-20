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


---


In relational databases, **transaction isolation levels** define how concurrent transactions interact with each other, specifically in terms of **visibility of data changes**. The SQL standard defines **four main isolation levels**, each with increasing levels of strictness (and usually cost). They are:

---

### 🔹 1. **Read Uncommitted** (Lowest isolation)

* ✅ **Allows:** Dirty Reads, Non-repeatable Reads, Phantom Reads
* ❌ **Prevents:** Nothing
* 📌 Description: Transactions can read data that has been modified but not yet committed by other transactions (i.e., *dirty reads*).
* ⚠️ Use Case: Rarely used due to data inconsistency risks.

* Postgres doesn't support **Read Uncommitted**.

```SQL
SHOW TRANSACTION ISOLATION LEVEL;
```

---

### 🔹 2. **Read Committed** (Default in many databases like Oracle, PostgreSQL)

* ❌ **Prevents:** Dirty Reads
* ✅ **Allows:** Non-repeatable Reads, Phantom Reads
* 📌 Description: A transaction only reads committed data; it waits if the data is being modified by another transaction.
* 🔁 If you read the same row twice, the value might change (non-repeatable reads).

| Time Stamp | 🖥️ Session A (Transaction A)             | 🖥️ Session B (Transaction B)                              |
| ---------- | --------------------------------------- | -------------------------------------------------------- |
| t0         | ```SHOW TRANSACTION ISOLATION LEVEL;``` | ```SHOW TRANSACTION ISOLATION LEVEL;```                  |
| t1         | ```BEGIN;```                            | ```BEGIN```                                              |
| t2         | ```SELECT * FROM accounts;```           |                                                          |
| t3         |                                         | ```UPDATE accounts SET balance=balance+99 WHERE id=1;``` |
| t4         | ```SELECT * FROM accounts;```           |                                                          |
| t5         |                                         | ```COMMIT;```                                            |
| t6         | ```SELECT * FROM accounts;```           |                                                          |
| t7         | ```ROLLBACK;```                         |                                                          |

---

### 🔹 3. **Repeatable Read** (Default in MySQL InnoDB)

* ❌ **Prevents:** Dirty Reads, Non-repeatable Reads
* ✅ **Allows:** Phantom Reads
* 📌 Description: Ensures that if a row is read twice, the result is the same; however, new rows (phantoms) might appear in range queries.
* 🧠 Uses **shared locks** on read rows until the transaction ends.

* All SELECT queries in a transaction see the same snapshot of the database taken at the start of the transaction.

| Time Stamp | 🖥️ Session A (Transaction A)                            | 🖥️ Session B (Transaction B)                               |
| ---------- | ------------------------------------------------------ | --------------------------------------------------------- |
| t0         | ```BEGIN;```                                           | ```BEGIN```                                               |
| t1         | ```SET TRANSACTION ISOLATION LEVEL REPEATABLE READ;``` | ```SET TRANSACTION ISOLATION LEVEL REPEATABLE READ;```    |
| t2         | ```SHOW TRANSACTION ISOLATION LEVEL;```                | ```SHOW TRANSACTION ISOLATION LEVEL;```                   |
| t3         | ```SELECT * FROM accounts;```                          |                                                           |
| t4         |                                                        | ```UPDATE accounts SET balance=balance+100 WHERE id=1;``` |
| t5         | ```SELECT * FROM accounts;```                          |                                                           |
| t6         |                                                        | ```COMMIT;```                                             |
| t7         | ```SELECT * FROM accounts;```                          |                                                           |
| t8         | ```COMMIT;```                                          |                                                           |

* At t7 we can still see the account balance same as t3 & t5;
  
| Time Stamp | 🖥️ Session A (Transaction A)                            | 🖥️ Session B (Transaction B)                            |
| ---------- | ------------------------------------------------------ | ------------------------------------------------------ |
| t0         | ```BEGIN;```                                           | ```BEGIN```                                            |
| t1         | ```SET TRANSACTION ISOLATION LEVEL REPEATABLE READ;``` | ```SET TRANSACTION ISOLATION LEVEL REPEATABLE READ;``` |
| t2         | ```SHOW TRANSACTION ISOLATION LEVEL;```                | ```SHOW TRANSACTION ISOLATION LEVEL;```                |
| t3         | ```SELECT * FROM accounts WHERE balance>=1000;```      |                                                        |
| t4         |                                                        | ```INSERT INTO accounts(balance) VALUES (1001);```     |
| t5         | ```SELECT * FROM accounts WHERE balance>=1000;```      | ```SELECT * FROM accounts WHERE balance>=1000;```      |
| t6         |                                                        | ```COMMIT;```                                          |
| t7         | ```SELECT * FROM accounts WHERE balance>=1000;```      |                                                        |
| t8         | ```COMMIT;```                                          |                                                        |


### 🔍 Why Phantom Reads Don’t Happen in PostgreSQL `REPEATABLE READ`

* In **the SQL standard**, `REPEATABLE READ` **allows phantom reads** (new rows that match a query condition appear in the same transaction if executed again).
* But in **PostgreSQL**, `REPEATABLE READ` provides a **stronger guarantee**:

  * It **takes a consistent snapshot** at the beginning of the transaction.
  * All `SELECT`s use that snapshot — so **you won’t see any new inserts** or updates made by other transactions after your transaction started.
  * This **prevents phantom reads**, even though the SQL standard allows them at this level.

---

### 🧠 Summary

| Isolation Level | SQL Standard Phantom Read | PostgreSQL Behavior         |
| --------------- | ------------------------- | --------------------------- |
| Read Committed  | ✅ Allowed                 | ✅ Happens                   |
| Repeatable Read | ✅ Allowed                 | ❌ Prevented (MVCC snapshot) |
| Serializable    | ❌ Not allowed             | ❌ Prevented                 |


---

### 🔹 4. **Serializable** (Highest isolation)

* ❌ **Prevents:** Dirty Reads, Non-repeatable Reads, Phantom Reads
* 📌 Description: Transactions are executed with full isolation. It's as if the transactions were run sequentially rather than concurrently.
* ⚖️ Uses **range locks** and/or **predicate locking**.
* ⚠️ Most expensive in terms of performance and locking.

  
| Time Stamp | 🖥️ Session A (Transaction A)                         | 🖥️ Session B (Transaction B)                        |
| ---------- | --------------------------------------------------- | -------------------------------------------------- |
| t0         | ```BEGIN;```                                        | ```BEGIN```                                        |
| t1         | ```SET TRANSACTION ISOLATION LEVEL SERIALIZABLE;``` | ```SET TRANSACTION ISOLATION LEVEL SERIALIZABLE``` |
| t2         | ```SHOW TRANSACTION ISOLATION LEVEL;```             | ```SHOW TRANSACTION ISOLATION LEVEL;```            |
| t3         | ```SELECT * FROM accounts WHERE balance>=1000;```   |                                                    |
| t4         |                                                     | ```INSERT INTO accounts(balance) VALUES (2000);``` |
| t5         | ```SELECT * FROM accounts WHERE balance>=1000;```   | ```SELECT * FROM accounts WHERE balance>=1000;```  |
| t6         |                                                     | ```COMMIT;```                                      |
| t7         | ```SELECT * FROM accounts WHERE balance>=1000;```   |                                                    |
| t8         | ```COMMIT;```                                       |                                                    |

---

### 💡 Comparison Table

| Isolation Level  | Dirty Read | Non-repeatable Read | Phantom Read |
| ---------------- | ---------- | ------------------- | ------------ |
| Read Uncommitted | ✅          | ✅                   | ✅            |
| Read Committed   | ❌          | ✅                   | ✅            |
| Repeatable Read  | ❌          | ❌                   | ✅            |
| Serializable     | ❌          | ❌                   | ❌            |

---

### 🗒️ Notes

* **Dirty Read:** Reading uncommitted changes from another transaction.
* **Non-repeatable Read:** Reading the same row twice gives different results due to another committed transaction.
* **Phantom Read:** A query returns different sets of rows when re-executed, due to inserts/deletes by other transactions.


---

* [transaction-isolation-postgres](https://www.thenile.dev/blog/transaction-isolation-postgres)
