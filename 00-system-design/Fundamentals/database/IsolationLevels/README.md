## The Story of Isolation Levels

Let's start with a crime scene.

---

## Chapter 1: The Problem Nobody Saw Coming — Concurrency

It's the early 1970s. You've built a database. WAL solves crashes. Transactions are atomic. Life is good.

Then your database gets popular. Instead of one user at a time, you have **thousands of users hitting the database simultaneously.** And something strange starts happening.

Calculations come out wrong. Balances don't add up. Reports show numbers that never existed. Nobody can reproduce the bugs reliably — they happen randomly, under load, and disappear when you look for them.

You add logging everywhere. You stare at the data. And slowly you realize:

> *"The transactions aren't interfering with the disk. They're interfering with **each other**."*

This was a new class of problem nobody had fully thought through. A single transaction is clean and atomic. But when thousands of transactions run **at the same time**, their reads and writes **interleave** in time in ways that produce completely wrong results — even when each individual transaction is perfectly correct.

This is the **concurrency problem**. And solving it is one of the deepest, most subtle challenges in all of database engineering.

---

### The Naive Solution — And Why It Fails

The obvious fix is simple: **don't let transactions run at the same time.** Run them one at a time, strictly serially.

```
TXN-1 starts → TXN-1 finishes → TXN-2 starts → TXN-2 finishes → ...
```

This is called **serial execution** and it is, by definition, perfectly correct. No interleaving, no interference.

And it's also **completely unusable** at any real scale.

Your database has thousands of users. Each transaction takes 10ms. If they run serially:

```
1000 users × 10ms = 10 seconds wait time for the last user
```

In the time it takes user #1000 to get a response, they've already given up and called customer support.

The entire point of a database is to serve many users simultaneously. You need concurrency. But concurrency causes bugs.

This is the fundamental tension:

> **Maximum correctness = serial execution = terrible performance.**
> **Maximum performance = fully concurrent = correctness nightmares.**

The question becomes: *is there a middle ground? Can we identify exactly which kinds of interference matter, quantify them, and offer different levels of protection depending on what the application actually needs?*

This question consumed database researchers for a decade. The answer they arrived at is **Isolation Levels** — a formal taxonomy of concurrency anomalies, ordered from least to most protected.

Let's discover each anomaly the way engineers did — by watching it cause a real problem.

---

## Chapter 2: The First Anomaly — Dirty Reads

It's 1974. You're running a bank. Two transactions are running concurrently:

**Transaction A** — Alice is transferring $200 to Bob:
```
TXN-A Step 1: Alice.balance = 1000 - 200 = 800   (written to buffer pool)
TXN-A Step 2: Bob.balance = 500 + 200 = 700       (written to buffer pool)
TXN-A Step 3: ... (still processing, not committed yet)
```

**Transaction B** — A report is calculating the total money in the bank:
```
TXN-B: SELECT SUM(balance) FROM accounts;
```

Transaction B runs RIGHT in between Transaction A's two steps. It reads Alice's balance ($800 — already updated) and Bob's balance ($500 — not yet updated).

```
TXN-B sees:
  Alice: $800
  Bob:   $500
  Total: $1,300
```

But the real total should be $1,500 (Alice's $1,000 + Bob's $500). Transaction B has read a total that **never existed in any consistent state of the database.** $200 has temporarily vanished.

Now suppose Transaction A then **fails and rolls back**:
```
TXN-A: ERROR — network failure, rolling back
  Alice.balance = 1000  (restored)
  Bob.balance = 500     (restored)
```

Transaction B's report — already sent to the manager — shows $1,300 total. The real total is $1,500. The report is wrong. Based on data that was written but never committed — data that doesn't exist anymore.

This is a **Dirty Read** — reading data written by a transaction that hasn't committed yet. "Dirty" because the data is provisional — it might vanish.

---

### The Fix: Read Committed

The solution is simple in concept:

> *"A transaction is only allowed to read data that has been **committed.** Uncommitted writes are invisible."*

This is the **Read Committed** isolation level. It's the default in Oracle, SQL Server, and many others.

How does the database implement this? Two main approaches:

**Approach 1: Shared Locks**

When Transaction A writes a row, it acquires an **exclusive lock** on it. When Transaction B tries to read that row, it tries to acquire a **shared lock** — but the exclusive lock blocks it. Transaction B waits until Transaction A commits or rolls back and releases the lock.

```
TXN-A writes Alice's row → acquires exclusive lock
TXN-B tries to read Alice's row → blocked (waiting for lock)
TXN-A commits → releases exclusive lock
TXN-B can now read Alice's row → sees committed value $800
```

**Approach 2: MVCC (Multi-Version Concurrency Control)**

More modern databases use a different approach: instead of blocking, they keep **multiple versions** of each row. Transaction B doesn't wait — it reads the **last committed version** of the row, which is still $1,000. Transaction A's $800 is a newer version that's invisible until it commits.

```
Row versions for Alice:
  Version 1: balance=$1,000, committed by TXN-1  ← TXN-B reads this
  Version 2: balance=$800,   written by TXN-A, UNCOMMITTED  ← invisible to TXN-B
```

MVCC is dramatically faster than locking for reads — readers never block writers, and writers never block readers. We'll go deeper on MVCC in a moment. For now: Read Committed prevents dirty reads.

Problem solved? Not quite. Engineers deployed Read Committed everywhere and then discovered a new class of bugs.

---

## Chapter 3: The Second Anomaly — Non-Repeatable Reads

Here's a scenario that Read Committed doesn't protect against.

Transaction A is running a complex bank audit. It reads Alice's balance at the start:

```
TXN-A Step 1: SELECT balance FROM accounts WHERE id = 'alice';
              → returns $1,000

[TXN-A pauses to do some calculations...]
```

While TXN-A is paused, Transaction B runs and commits:
```
TXN-B: UPDATE accounts SET balance = 800 WHERE id = 'alice';
TXN-B: COMMIT;  ← fully committed, legitimate
```

Now TXN-A continues and reads Alice's balance again for verification:
```
TXN-A Step 2: SELECT balance FROM accounts WHERE id = 'alice';
              → returns $800  ← 😱
```

Within the same transaction, the same query returned **two different values**. TXN-A's calculations are now internally inconsistent — it made decisions based on $1,000 but now sees $800. The audit report will be wrong.

This is a **Non-Repeatable Read** — within a single transaction, reading the same row twice gives different results because another transaction committed a change in between.

Note that the second read is perfectly valid under Read Committed — $800 IS the committed value. Read Committed only prevents reading uncommitted data. It says nothing about reads being **stable** over time.

For the audit transaction, this is a disaster. It needs a consistent snapshot of the database as it existed at the start of the transaction. The world should not change under its feet while it's working.

---

### The Fix: Repeatable Read

> *"Once a transaction reads a row, that row will return the same value for the entire duration of the transaction — even if other transactions commit changes to it."*

This is **Repeatable Read** isolation.

With locking: Transaction A acquires and **holds** shared locks on every row it reads, until the transaction ends. Other transactions that want to write those rows are blocked until A finishes.

With MVCC (more elegantly): Transaction A gets a **snapshot timestamp** when it starts. Every read sees the database as it existed at that timestamp — ignoring any commits that happen after the snapshot was taken.

```
TXN-A starts at time T=100. Gets snapshot of database at T=100.

TXN-B commits at T=105, writes Alice=$800.

TXN-A reads Alice at T=110:
  "What was Alice's balance at T=100?"
  → $1,000  (the snapshot version, ignoring TXN-B's commit)
```

Same query, same result, every time, for the entire transaction. Non-repeatable reads eliminated.

But engineers deployed this and found *another* class of bug that Repeatable Read still doesn't handle.

---

## Chapter 4: The Third Anomaly — Phantom Reads

This one is subtle. Let me show you exactly where Repeatable Read breaks.

Transaction A is a payroll system calculating bonuses. It queries all employees with salary over $50,000:

```
TXN-A Step 1: SELECT * FROM employees WHERE salary > 50000;
              → returns 10 rows: [Alice, Bob, Charlie, ...]
              
[TXN-A is now doing calculations on these 10 employees...]
```

Meanwhile Transaction B runs and commits:
```
TXN-B: INSERT INTO employees (name, salary) VALUES ('Dave', 75000);
TXN-B: COMMIT;
```

TXN-A, still running, queries again for verification:
```
TXN-A Step 2: SELECT * FROM employees WHERE salary > 50000;
              → returns 11 rows: [Alice, Bob, Charlie, ..., Dave]  ← 😱
```

But wait — TXN-A is using Repeatable Read! It should see a consistent snapshot!

Here's the subtlety: Repeatable Read guarantees that **rows you've already read** don't change. It says nothing about **new rows appearing** that match your query.

Dave didn't exist when TXN-A first ran the query. He's not a row TXN-A read and "locked." He's a completely new row — a **phantom** — that materialized between two identical queries.

TXN-A calculated bonuses for 10 people. Now it sees 11. Dave gets no bonus. That's a payroll error.

This is a **Phantom Read** — a transaction re-runs a query and gets different rows because another transaction inserted (or deleted) matching rows in between.

---

### The Fix: Serializable

> *"The transaction should behave as if it's the only transaction running. No anomalies. No phantoms. No surprises. The outcome should be identical to some serial execution of all transactions."*

This is **Serializable** isolation — the highest level. The gold standard.

With locking, phantoms are prevented with **predicate locks** (also called **range locks**). When TXN-A queries `WHERE salary > 50000`, it locks not just the rows it found, but the **entire predicate** — the concept of "any row with salary > 50000." If TXN-B tries to insert Dave with salary 75000, it tries to acquire a lock on a row that matches TXN-A's predicate — and blocks until TXN-A finishes.

With MVCC, the modern approach is **Serializable Snapshot Isolation (SSI)** — which deserves its own section because it's a beautiful algorithm.

---

## Chapter 5: There's a Fourth Anomaly They Initially Missed — Write Skew

The original 1992 ANSI SQL standard defined the three anomalies above. Researchers thought that was complete. Then in 1995, researchers Hal Berenson, Phil Bernstein, Jim Gray and others published a paper showing the standard had missed something.

Even under Serializable isolation (as originally defined), there was a subtle anomaly that could occur. They called it **Write Skew.**

---

### The On-Call Doctor Problem

A hospital has a rule: **at least one doctor must be on call at all times.** Currently, both Dr. Alice and Dr. Bob are on call.

```
doctors table:
  id  │ name  │ on_call
  ────┼───────┼────────
  1   │ Alice │ true
  2   │ Bob   │ true
```

Dr. Alice feels sick and wants to take herself off call. She writes a transaction:

```sql
-- TXN-A (Alice going off call):
BEGIN;
SELECT COUNT(*) FROM doctors WHERE on_call = true;
-- returns 2. Two doctors on call. Safe to remove one.
UPDATE doctors SET on_call = false WHERE id = 1; -- Alice goes off call
COMMIT;
```

Simultaneously, Dr. Bob also feels sick and does the same thing:

```sql
-- TXN-B (Bob going off call):
BEGIN;
SELECT COUNT(*) FROM doctors WHERE on_call = true;
-- returns 2. Two doctors on call. Safe to remove one.
UPDATE doctors SET on_call = false WHERE id = 2; -- Bob goes off call
COMMIT;
```

Both transactions run concurrently. Both read the count as 2. Both conclude it's safe to remove one doctor. Both commit successfully.

```
Result:
  Alice: on_call = false
  Bob:   on_call = false
  
Doctors on call: 0  ← nobody on call. Hospital rule violated. 😱
```

Here's what makes this insidious: **neither transaction has a dirty read, non-repeatable read, or phantom read.** Each transaction's reads are perfectly consistent with the data at the time they started. Each transaction's write is to a **different row** — Alice writes to her row, Bob writes to his. There's no write conflict in the traditional sense.

The problem is that the **decision each transaction made was based on a premise that the other transaction invalidated.**

Alice's decision: "There are 2 doctors on call, so I can leave."
Bob's decision: "There are 2 doctors on call, so I can leave."

Both premises were true when read. But when both acted on those premises simultaneously, the combined effect violated the invariant. Neither transaction alone is wrong. Their **interaction** is wrong.

This is **Write Skew** — two transactions read overlapping data, each makes a decision based on what they read, each writes to a disjoint part of the data, but the combined writes violate an invariant that their reads were supposed to protect.

---

### Why This Breaks Snapshot Isolation

Snapshot Isolation — which most databases marketed as "Serializable" in the 1990s and early 2000s — **does not prevent write skew.**

Under Snapshot Isolation:
- Both transactions get a snapshot at the start
- Both see the same data (2 doctors on call)
- Both write to different rows (no conflict detected)
- Both commit successfully

The database sees no conflict because it only checks: *"Did two transactions write to the same row?"* They didn't — Alice wrote to row 1, Bob wrote to row 2.

But a truly serial execution would never produce this result. If they ran serially:
- Alice's TXN runs first → sees 2 doctors → goes off call (1 remains: Bob)
- Bob's TXN runs second → sees 1 doctor → **decides it's unsafe to go off call** → aborts

Or vice versa. Either order, one of them can't proceed. The constraint is maintained.

This was a bombshell. Major databases had been claiming "Serializable" isolation for years while quietly shipping Snapshot Isolation — which has the write skew hole.

The researchers listed databases that had this discrepancy. It was embarrassing. And it forced a rethinking of what "Serializable" actually needed to guarantee.

---

## Chapter 6: The Elegant Modern Solution — Serializable Snapshot Isolation

For years, the only way to get true Serializable isolation was with heavy locking — predicate locks everywhere, readers blocking writers, writers blocking readers. The performance cost was brutal. Many applications simply accepted the weaker isolation to get acceptable performance.

Then in 2008, Michael Cahill published his PhD dissertation at the University of Sydney introducing **Serializable Snapshot Isolation (SSI)**. It was implemented in PostgreSQL in 2011 (version 9.1). It's one of the most elegant algorithms in database engineering.

The key insight was:

> *"Instead of preventing dangerous patterns upfront with locks, let transactions run freely with snapshot isolation — but watch for patterns of reads and writes that COULD produce write skew. If we detect those patterns, abort one of the conflicting transactions."*

This is an **optimistic** approach. Assume things will be fine. Proceed without blocking. But monitor carefully. If you detect a situation that could lead to write skew, intervene.

---

### How SSI Detects Write Skew — The Dependency Graph

SSI tracks two types of dependencies between concurrent transactions:

**Read-Write (rw) anti-dependency:** Transaction A reads a row, then Transaction B writes that row. A's read is now "stale" relative to B's write. A read something that B later changed.

```
TXN-A reads row X at version 1
TXN-B writes row X, creating version 2
→ rw anti-dependency: A→B (A's read is affected by B's write)
```

**Write-Read (wr) dependency:** Transaction A writes a row, then Transaction B reads it. B might be making decisions based on A's write.

```
TXN-A writes row X
TXN-B reads row X (A's version)
→ wr dependency: A→B (B's read depends on A's write)
```

SSI builds a **dependency graph** — a graph where nodes are transactions and edges are dependencies. Theoretical research proved:

> *Write skew is possible if and only if there is a cycle in the dependency graph where there are at least **two consecutive rw anti-dependency edges.***

This is called a **dangerous structure** or **pivot.**

Let's trace it through the doctor example:

```
TXN-A reads doctors table        → TXN-B writes doctors table
  rw anti-dependency: TXN-A → TXN-B

TXN-B reads doctors table        → TXN-A writes doctors table  
  rw anti-dependency: TXN-B → TXN-A
```

```
Dependency graph:
  TXN-A ──rw──▶ TXN-B
  TXN-B ──rw──▶ TXN-A
  
  → CYCLE with two consecutive rw edges → dangerous!
```

SSI detects this cycle. It aborts one of the transactions (say TXN-B). TXN-B retries, now sees only 1 doctor on call, decides it can't go off call, and the constraint is maintained.

---

### The Performance Win

The beauty of SSI is that in the **common case** — when there are no dangerous patterns — transactions run with zero additional overhead compared to Snapshot Isolation. No extra locking. No blocking. Readers and writers don't block each other.

Only when a dangerous pattern is detected does SSI intervene — and even then, only one transaction is aborted and retried. Most workloads see SSI abort rates of less than 1%.

This gave the database world something it had been searching for since the 1970s: **truly correct serializable execution at near-snapshot-isolation performance.**

---

## Chapter 7: Putting It All Together — The Isolation Level Spectrum

Now we can see the full picture. Let's map out exactly which anomalies each level prevents:

```
                   │ Dirty  │ Non-Rep │ Phantom │ Write  │
Isolation Level    │ Read   │ Read    │ Read    │ Skew   │ Performance
───────────────────┼────────┼─────────┼─────────┼────────┼────────────
Read Uncommitted   │  ✗     │  ✗      │  ✗      │  ✗     │ Fastest
Read Committed     │  ✓     │  ✗      │  ✗      │  ✗     │ Fast
Repeatable Read    │  ✓     │  ✓      │  ✗*     │  ✗     │ Medium
Snapshot Isolation │  ✓     │  ✓      │  ✓      │  ✗     │ Medium
Serializable (SSI) │  ✓     │  ✓      │  ✓      │  ✓     │ Medium+

* MySQL's Repeatable Read prevents phantoms via gap locks — a special case
```

---

### What Each Database Actually Ships By Default

And here's the uncomfortable truth about defaults:

```
Database          │ Default Isolation    │ Notes
──────────────────┼─────────────────────┼────────────────────────────
PostgreSQL        │ Read Committed       │ SSI available, not default
MySQL (InnoDB)    │ Repeatable Read      │ With gap locks for phantoms
Oracle            │ Read Committed       │ Snapshot available
SQL Server        │ Read Committed       │ Snapshot available
SQLite            │ Serializable         │ (single writer anyway)
CockroachDB       │ Serializable         │ SSI, always
Google Spanner    │ Serializable         │ External consistency, always
```

Most databases default to something weaker than Serializable for performance reasons. This means most applications in production are silently vulnerable to some class of concurrency anomaly — and most developers don't know it.

---

## Chapter 8: MVCC — The Engine Under the Hood

We've mentioned MVCC several times. Now let's go deep on how it actually works, because it's the mechanism that makes modern isolation levels possible without crippling performance.

The core idea:

> *"Instead of one version of each row, keep **multiple versions** — one for each point in time. Let each transaction see the version that existed when it started, regardless of what's happened since."*

---

### How PostgreSQL Implements MVCC

In PostgreSQL, every row has two hidden system columns:

```
accounts table (what you see):
  id  │ name  │ balance
  ────┼───────┼────────
  1   │ Alice │ $800

accounts table (what's actually stored):
  xmin  │ xmax  │ id │ name  │ balance
  ──────┼───────┼────┼───────┼────────
  1001  │ 0     │ 1  │ Alice │ $1,000   ← original row, written by TXN-1001
  1042  │ 0     │ 1  │ Alice │ $800     ← updated row, written by TXN-1042
```

**xmin** — the transaction ID that created this row version.
**xmax** — the transaction ID that deleted/updated this row version (0 means still alive).

When TXN-1042 updates Alice's balance:
1. The old row gets `xmax = 1042` (marked as "deleted by TXN-1042")
2. A new row version is inserted with `xmin = 1042, balance = $800`

Both versions exist on disk simultaneously.

---

### The Visibility Rule

When any transaction reads a row, PostgreSQL evaluates visibility rules:

> *"This row version is visible to me if:*
> - *Its xmin committed BEFORE my transaction started (it was created by a committed transaction I should see)*
> - *AND its xmax is either 0 (not deleted) or committed AFTER my transaction started (it was deleted after my snapshot)"*

Let's trace this with the doctor example:

```
TXN-A starts at snapshot time T=1000
TXN-B starts at snapshot time T=1001

Both read doctors table.

TXN-A updates row 1 (Alice): 
  old version: xmin=500, xmax=1000 (now deleted)
  new version: xmin=1000, xmax=0

TXN-B updates row 2 (Bob):
  old version: xmin=500, xmax=1001 (now deleted)
  new version: xmin=1001, xmax=0

TXN-A commits (TXN-A = 1000)
TXN-B commits (TXN-B = 1001)
```

Final state — both new versions are committed. The hospital has no doctors on call. Without SSI, both would commit. With SSI, the dependency cycle is detected and one is aborted.

---

### The MVCC Storage Problem — Dead Tuples

Here's the dark side of MVCC. Every update creates a new row version. The old version isn't deleted immediately — other transactions might still be reading it.

Over time, your table fills up with **dead tuples** — old row versions that no transaction can see anymore but are still taking up space on disk.

```
accounts table physical storage after 1000 updates to Alice:
  [Alice $1,000 - dead]
  [Alice $950  - dead]
  [Alice $920  - dead]
  ...998 more dead versions...
  [Alice $800  - alive]  ← only this one is visible
```

This is called **table bloat** — your table physically grows even if the logical data size stays the same.

PostgreSQL's solution is **VACUUM** — a background process that scans tables, identifies dead tuples that no running transaction can see, and marks their space as reusable.

```
VACUUM runs:
  Scans accounts table
  Finds 999 dead versions of Alice's row
  Checks: is any running transaction old enough to need these?
  → No. All dead.
  Marks space as free for reuse.
```

But VACUUM has to run continuously, consume I/O, and keep up with the pace of updates. If your application updates rows faster than VACUUM can clean up, you get bloat, degraded performance, and eventually disk space exhaustion.

This is one of PostgreSQL's famous operational challenges. Managing VACUUM — tuning `autovacuum_vacuum_scale_factor`, `autovacuum_cost_delay`, understanding when to manually run VACUUM ANALYZE — is a core skill for serious PostgreSQL operators.

---

## Chapter 9: The Real-World Consequences — Bugs You've Probably Shipped

Let me make this concrete. Here are real-world bugs caused by wrong isolation level assumptions — the kind that are almost certainly in production systems right now.

---

### Bug 1: The Double-Spend (Write Skew)

A user has $500 in their account. They open two browser tabs simultaneously and make two purchases — $400 each.

```
TXN-A (Tab 1): SELECT balance FROM accounts WHERE id=1; → $500
               Is $500 >= $400? Yes. Proceed.
               UPDATE accounts SET balance = 100 WHERE id=1;
               COMMIT;

TXN-B (Tab 2): SELECT balance FROM accounts WHERE id=1; → $500
               Is $500 >= $400? Yes. Proceed.
               UPDATE accounts SET balance = 100 WHERE id=1;
               COMMIT;
```

Under Snapshot Isolation: both see $500, both proceed, both commit. Account balance ends at $100. User spent $800 they don't have.

Wait — these two transactions write to the SAME row. Isn't that a conflict?

It depends on timing. If TXN-B reads before TXN-A commits, TXN-B sees the old $500 and both commit. The final balance is $100, not -$300, because both wrote the same value. But the user successfully made two $400 purchases with only $500 — spending $800 total.

The fix: either use Serializable isolation, or use `SELECT FOR UPDATE` — which acquires a write lock on the balance row during the read, forcing TXN-B to wait for TXN-A to commit before proceeding.

```sql
-- Correct:
BEGIN;
SELECT balance FROM accounts WHERE id=1 FOR UPDATE; -- acquires lock
-- TXN-B blocks here until TXN-A commits
UPDATE accounts SET balance = balance - 400 WHERE id=1;
COMMIT;
```

---

### Bug 2: The Phantom Insert Race (Phantom Read)

You're building a unique username system. Before inserting a new user, you check if the username exists:

```python
# TXN-A (User 1 tries to register "cooldev"):
result = db.execute("SELECT * FROM users WHERE username='cooldev'")
if result.rowcount == 0:
    db.execute("INSERT INTO users (username) VALUES ('cooldev')")
    db.commit()

# TXN-B (User 2 simultaneously tries to register "cooldev"):
result = db.execute("SELECT * FROM users WHERE username='cooldev'")
if result.rowcount == 0:  # also sees 0 rows!
    db.execute("INSERT INTO users (username) VALUES ('cooldev')")
    db.commit()
```

Both transactions check, both see no existing "cooldev", both insert. You now have two users with the same username. Your application-level uniqueness check failed because of a phantom read.

The correct fix isn't isolation levels — it's a **database-level unique constraint:**

```sql
CREATE UNIQUE INDEX ON users(username);
```

Now the second INSERT will fail with a unique violation error, regardless of isolation level. Database constraints are enforced atomically by the database engine — they can't have race conditions the way application-level checks can.

This is a universal lesson: **don't enforce uniqueness in application code. Always use database constraints.**

---

### Bug 3: The Stale Read Aggregation (Non-Repeatable Read)

An analytics job runs a multi-step report under Read Committed:

```sql
-- Step 1: Get total revenue
SELECT SUM(amount) FROM orders WHERE date = '2024-01-01';
→ $1,000,000

-- [Meanwhile, 50 new orders are inserted and committed by other transactions]

-- Step 2: Get order count  
SELECT COUNT(*) FROM orders WHERE date = '2024-01-01';
→ 1,050  ← includes the 50 new orders

-- Step 3: Calculate average order value
average = $1,000,000 / 1,050 = $952.38  ← wrong denominator
```

The average is calculated using total revenue from before the new orders, divided by a count that includes the new orders. The result is meaningless.

The fix: run the report under Repeatable Read or Snapshot Isolation, or wrap it in a single query:

```sql
SELECT SUM(amount), COUNT(*), SUM(amount)/COUNT(*) as avg
FROM orders 
WHERE date = '2024-01-01';
-- All computed atomically from the same snapshot
```

---

## Chapter 10: The Philosophical Layer — What Is "Correct" in a Concurrent World?

Here's the deepest question this story raises:

When two users simultaneously try to do something that together creates an inconsistency — **whose fault is it?**

The doctor example: both doctors simultaneously chose to go off call. Each made a locally rational decision. The system allowed it. Is this a database bug? An application bug? A protocol bug?

The answer is: it depends on what guarantee you asked for.

If you said "Read Committed" — you asked for a specific, weak set of guarantees. Write skew is outside those guarantees. You got exactly what you asked for.

If you said "Serializable" — you asked for the database to guarantee outcomes match serial execution. Write skew is prevented. You got exactly what you asked for.

The database is not wrong. The application developer made a choice — often unconsciously, by accepting whatever default the database shipped with — and that choice had consequences they didn't understand.

This is the meta-lesson of isolation levels:

> **Isolation levels are not a safety net. They are a contract. The database will honor exactly the contract you choose. If you choose a weaker contract for performance and then reason as if you have a stronger one, you will have bugs — bugs that are subtle, rare, and catastrophic.**

Most developers don't know which isolation level their database is using. Most don't know which anomalies their application is actually vulnerable to. This is why concurrency bugs are some of the hardest to find — they only appear under concurrent load, they're non-deterministic, and they leave no trace in logs.

---

## The Full Story, End to End

```
Observation                    → Anomaly Found      → Isolation Level Created
────────────────────────────────────────────────────────────────────────────
Reading uncommitted writes     → Dirty Read         → Read Committed
causes wrong results

Same query in same transaction → Non-Repeatable     → Repeatable Read  
returns different rows           Read

New rows appear between        → Phantom Read       → Serializable (1992)
identical range queries                               (via predicate locks)

Concurrent transactions with   → Write Skew         → True Serializable
disjoint writes corrupt                               (redefined, 1995)
shared invariant                                      SSI (implemented 2008)

Locking kills performance      → MVCC invented      → Readers don't block
                                                      writers, vice versa

MVCC creates dead tuples       → Table bloat        → VACUUM / autovacuum

All of this is still           → Most apps ship     → Production bugs
confusing, defaults are weak     Read Committed        nobody knows exist
```

---

**Where would you like to go next?** Natural threads from here:

- **MVCC deep dive** — exactly how version chains work, how PostgreSQL's visibility rules handle every edge case, how CockroachDB implements distributed MVCC
- **Locking in depth** — deadlocks, lock queues, gap locks in MySQL, how lock managers work
- **Distributed isolation** — what Serializable means when your database is spread across continents (Spanner's approach, CockroachDB's approach)
- **The application side** — how to actually structure transactions correctly, SELECT FOR UPDATE, advisory locks, optimistic locking patterns
