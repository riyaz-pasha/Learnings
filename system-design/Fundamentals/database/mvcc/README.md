## The Story of MVCC — Multi-Version Concurrency Control

Let's start with the problem that made MVCC inevitable. Not the theoretical problem — the visceral, painful, production problem.

---

## Chapter 1: The World Before MVCC — The Lock Nightmare

It's 1975. You've built a database with proper isolation. You use locks. The rules are simple:

- **Reading a row** → acquire a shared lock (S-lock). Multiple readers can hold S-locks simultaneously.
- **Writing a row** → acquire an exclusive lock (X-lock). Nobody else can read OR write while you hold it.

```
S-lock + S-lock = compatible  (two readers, fine)
S-lock + X-lock = incompatible (reader blocks writer, writer blocks reader)
X-lock + X-lock = incompatible (writers block each other)
```

This is correct. Perfectly serializable. And it created a catastrophe at scale.

---

### The Symptom: Everything Blocks Everything

Imagine a busy e-commerce site. Thousands of users are browsing products (reads). Occasionally, someone places an order (write).

```
User buys item:
  TXN-WRITE acquires X-lock on product row
  [processing payment... 200ms]
  
Meanwhile:
  1000 users want to read that product page
  All 1000 are BLOCKED waiting for TXN-WRITE to release its X-lock
  
TXN-WRITE commits, releases lock
  1000 reads unblock simultaneously
  They all pile onto the database at once
  Database grinds under the sudden load spike
```

A single write to one product row blocks a thousand concurrent readers. On a popular product during a sale, this is catastrophic. The more popular your site, the worse it gets.

Engineers measured this and found something depressing: **most database time in read-heavy workloads was spent waiting for locks, not doing actual work.**

The core problem is philosophical:

> *Why should reading block writing? A reader isn't changing anything. Why does it need to lock out writers?*

The answer — under the locking model — is: because without a lock, a writer might change the data mid-read, giving you an inconsistent view. But this feels like a fundamental limitation of the locking approach, not an inescapable truth.

In 1979, researchers at Berkeley — David Reed in his PhD dissertation — proposed a radical alternative:

> *"What if instead of blocking, we simply gave each transaction its own consistent **snapshot** of the database? Readers see the version of data that existed when they started. Writers create new versions. Nobody blocks anybody."*

This is the core idea of MVCC. It took another decade to implement well, but the insight was there in 1979.

---

## Chapter 2: The Core Insight — Versions Instead of Locks

Let's establish the fundamental mental model before going into any implementation.

In a locking database, there is **one version** of each row. Everyone fights over it.

```
Locking model:
  Row: Alice, balance=$1000
  
  Reader: "I want to read this" → must wait for any writer
  Writer: "I want to write this" → must wait for all readers AND writers
```

In an MVCC database, there are **multiple versions** of each row — one for each time it was modified. Each transaction is assigned a **timestamp** (or transaction ID) when it starts. Each transaction sees only the versions that existed as of its timestamp.

```
MVCC model:
  Row versions:
    v1: Alice, balance=$1000  (created at time T=100)
    v2: Alice, balance=$800   (created at time T=150)
    v3: Alice, balance=$750   (created at time T=200)
  
  TXN starting at T=120 sees: v1 ($1000)  ← only version before T=120
  TXN starting at T=160 sees: v2 ($800)   ← latest version before T=160
  TXN starting at T=210 sees: v3 ($750)   ← latest version before T=210
```

Each transaction lives in its own consistent bubble of time. Readers see a past snapshot. Writers create new versions without disturbing old ones. **Nobody blocks anybody — for reads.**

This one idea — versions instead of locks — changed the entire field.

Now let's go deep on exactly how PostgreSQL implements this. Because the implementation details reveal layers of subtlety that the high-level idea hides.

---

## Chapter 3: PostgreSQL's MVCC — Every Byte Explained

### The Hidden Columns

Open any PostgreSQL table and you'll see your columns — `id`, `name`, `balance`, etc. But every row in PostgreSQL has additional **system columns** that are normally invisible. You can query them explicitly:

```sql
SELECT xmin, xmax, ctid, id, name, balance 
FROM accounts;
```

```
 xmin │ xmax │  ctid  │ id │ name  │ balance
──────┼──────┼────────┼────┼───────┼────────
 1001 │    0 │ (0,1)  │  1 │ Alice │ 1000
 1001 │    0 │ (0,2)  │  2 │ Bob   │  500
```

Let's understand every field:

---

**`xmin` — Transaction that Created This Version**

When a row version is born — via INSERT or UPDATE — PostgreSQL stamps it with the transaction ID (XID) of the transaction that created it.

Transaction IDs in PostgreSQL are 32-bit integers, incrementing from 3 (1 and 2 are reserved). Every new transaction gets the next available ID.

```
xmin = 1001 means:
"This row version was created by transaction #1001"
```

---

**`xmax` — Transaction that Killed This Version**

When a row version is "deleted" — either by DELETE or by UPDATE (which creates a new version) — PostgreSQL stamps the old version with the XID of the transaction that killed it.

```
xmax = 0    means: "Nobody has deleted/updated this version. It's alive."
xmax = 1042 means: "Transaction #1042 deleted or updated this version."
```

Critical subtlety: `xmax` being set doesn't mean the version is invisible. If transaction #1042 is still running (not yet committed), then maybe it rolled back, and this row is still alive. The visibility rules handle this.

---

**`ctid` — Physical Location**

`ctid` is `(page_number, row_number_within_page)`. It's the physical address of this row version on disk.

```
ctid = (0,1) means: "Page 0, row slot 1"
ctid = (3,7) means: "Page 3, row slot 7"
```

This is how PostgreSQL builds **version chains** — but in a different way than you might expect. More on this soon.

---

### A Transaction's Lifecycle — xmin States

When a transaction starts, it gets an XID. When it commits or rolls back, PostgreSQL records this in a special data structure called the **commit log** (clog, also called pg_xact in newer versions).

The commit log is a bitmap that records, for each transaction ID, one of:
- `IN_PROGRESS` — transaction is still running
- `COMMITTED` — transaction successfully committed
- `ABORTED` — transaction rolled back

So when PostgreSQL evaluates whether to show a row version, it doesn't just look at the xmin/xmax numbers — it looks up their status in the commit log.

```
Row version: xmin=1042, xmax=0

Is transaction 1042 committed?
  Commit log lookup: 1042 → COMMITTED
  → Yes, this row was created by a committed transaction
  → Potentially visible (still need to check snapshot)
```

---

### The Snapshot — A Transaction's View of the World

When a transaction starts (or when a statement executes, depending on isolation level), PostgreSQL takes a **snapshot**. A snapshot is not a copy of the data — it's a lightweight data structure that encodes which transactions were active at the moment the snapshot was taken.

A PostgreSQL snapshot contains exactly three things:

```
Snapshot = {
  xmin:   lowest XID of any active transaction at snapshot time
  xmax:   highest XID assigned at snapshot time + 1
  xip:    list of XIDs that were in-progress at snapshot time
}
```

Let's make this concrete. At snapshot time, suppose:
- Transactions 1001-1040 have all committed or aborted
- Transaction 1041 is still running
- Transaction 1042 is still running
- Transaction 1043 was just assigned (about to start)

The snapshot looks like:

```
Snapshot = {
  xmin: 1041       ← lowest in-progress XID
  xmax: 1044       ← next XID to be assigned (exclusive upper bound)
  xip:  [1041, 1042]  ← transactions in flight at snapshot time
}
```

---

### The Visibility Rules — The Heart of MVCC

Now for the most important part. Given a row version with `(xmin, xmax)` and a snapshot, is this row version **visible** to the transaction holding that snapshot?

PostgreSQL applies this logic (simplified slightly for clarity):

---

**Rule 1: Is the row version's creator visible to me?**

A row version created by transaction `xmin` is visible if:

```
xmin is COMMITTED
AND
xmin < snapshot.xmax   (created before my snapshot's upper bound)
AND
xmin NOT IN snapshot.xip  (not in-progress when I took my snapshot)
```

In plain English: **the creating transaction committed, AND it committed before I started, AND it wasn't still running when I started.**

Let's trace some cases:

```
Case A: xmin = 900 (committed long ago)
  snapshot.xmin = 1041, snapshot.xmax = 1044, xip = [1041, 1042]
  
  Is 900 committed? Yes.
  Is 900 < 1044? Yes.
  Is 900 in [1041, 1042]? No.
  → VISIBLE ✓

Case B: xmin = 1042 (in-progress when snapshot taken)
  Is 1042 committed? Maybe yes (it committed after our snapshot).
  Is 1042 < 1044? Yes.
  Is 1042 in [1041, 1042]? YES.
  → NOT VISIBLE ✗ (even if it committed later)
  
Case C: xmin = 1050 (created AFTER our snapshot)
  Is 1050 < 1044? No.
  → NOT VISIBLE ✗
```

Case B is subtle and important. Transaction 1042 was running when you took your snapshot. Even if it commits before your transaction ends, you don't see its changes. Your snapshot captured "the world as it was before 1042 committed." This is how Repeatable Read works — your view of the world is frozen at snapshot time.

---

**Rule 2: Has this version been deleted (is xmax relevant)?**

A visible row version is still alive (not deleted) if:

```
xmax = 0   (never deleted)
OR
xmax is ABORTED   (the deleting transaction rolled back)
OR
xmax >= snapshot.xmax   (deleted after my snapshot)
OR
xmax IN snapshot.xip   (deleting transaction was in-progress at my snapshot time)
```

In plain English: **the row hasn't been deleted, OR the deletion happened after my snapshot, OR the deleting transaction was in-progress (might roll back).**

---

### Putting the Visibility Rules Together — Full Example

Let me walk through a complete scenario with four concurrent transactions.

```
Timeline:
  T=1 : TXN-1000 starts, inserts Alice ($1000), commits
  T=2 : TXN-1001 starts, inserts Bob ($500), commits
  T=3 : TXN-1002 starts (read-only report)
  T=4 : TXN-1003 starts, updates Alice to $800
  T=5 : TXN-1004 starts (another read)
  T=6 : TXN-1003 commits
  T=7 : TXN-1002 reads Alice and Bob
  T=8 : TXN-1004 reads Alice
```

Physical state of accounts table (all row versions on disk):

```
  xmin  │ xmax │ name  │ balance
 ───────┼──────┼───────┼────────
  1000  │ 1003 │ Alice │ 1000     ← original Alice, "deleted" by TXN-1003
  1001  │    0 │ Bob   │  500     ← Bob, still alive
  1003  │    0 │ Alice │  800     ← new Alice version from TXN-1003
```

**TXN-1002's snapshot** (taken at T=3, before 1003 started):
```
{xmin: 1002, xmax: 1003, xip: [1002]}
```

**TXN-1004's snapshot** (taken at T=5, 1003 in-progress):
```
{xmin: 1003, xmax: 1005, xip: [1003, 1004]}
```

---

**TXN-1002 reads at T=7 (after 1003 committed):**

Evaluating original Alice row `(xmin=1000, xmax=1003)`:
- Is xmin=1000 committed? Yes.
- Is 1000 < xmax(1003)? Yes.
- Is 1000 in xip([1002])? No.
- → **Creator is visible ✓**

- Is xmax=1003 = 0? No.
- Is xmax=1003 aborted? No (committed).
- Is 1003 >= snapshot.xmax(1003)? Yes (1003 >= 1003)!
- → **Deletion is invisible to me ✓** — TXN-1003 deleted this row AFTER my snapshot

Result: TXN-1002 **sees the original Alice ($1000).** Even though TXN-1003 committed at T=6, it was created after TXN-1002's snapshot. TXN-1002 is living in a frozen past.

---

**TXN-1004 reads at T=8 (after 1003 committed):**

Evaluating original Alice row `(xmin=1000, xmax=1003)`:
- Creator visible? Yes (1000 committed, before snapshot).
- Is xmax=1003 in xip([1003, 1004])? YES.
- → **Deletion was in-progress at my snapshot time** → **row appears alive... wait**

Actually let me re-examine. TXN-1004's snapshot was taken at T=5, when 1003 was in-progress. By T=8 when TXN-1004 reads, 1003 has committed. But TXN-1004 still uses its T=5 snapshot.

- xmax=1003 was in xip when snapshot taken → treat deletion as invisible → **this version appears alive**

But ALSO evaluate new Alice row `(xmin=1003, xmax=0)`:
- Is xmin=1003 committed? Yes (committed at T=6).
- Is 1003 < snapshot.xmax(1005)? Yes.
- Is 1003 in snapshot.xip([1003, 1004])? YES.
- → **Creator was in-progress at my snapshot time → NOT VISIBLE ✗**

So TXN-1004 sees the original Alice ($1000) from xmin=1000 version, because TXN-1003 was in-progress when TXN-1004's snapshot was taken.

This is **Repeatable Read** in action — TXN-1004 is isolated from changes made by transactions that were in-progress when it started.

---

### Read Committed vs Repeatable Read — The Snapshot Timing Difference

Here's the elegant thing. PostgreSQL implements both isolation levels using the exact same MVCC machinery. The only difference is **when the snapshot is taken:**

```
Read Committed:
  → New snapshot taken at the start of EACH STATEMENT
  → Each query sees all commits that happened before THAT QUERY ran

Repeatable Read / Serializable:
  → Snapshot taken ONCE at the start of the TRANSACTION
  → Every query in the transaction uses the SAME snapshot
  → Changes committed after transaction start are invisible
```

```sql
-- Under READ COMMITTED:
BEGIN;
SELECT balance FROM accounts WHERE id=1; -- snapshot A taken here
-- [TXN-B commits, updates balance]
SELECT balance FROM accounts WHERE id=1; -- snapshot B taken here
-- sees TXN-B's change! Different result. Non-repeatable read possible.
COMMIT;

-- Under REPEATABLE READ:
BEGIN;
SET TRANSACTION ISOLATION LEVEL REPEATABLE READ;
SELECT balance FROM accounts WHERE id=1; -- snapshot taken here
-- [TXN-B commits, updates balance]
SELECT balance FROM accounts WHERE id=1; -- SAME snapshot used
-- does NOT see TXN-B's change. Stable read guaranteed.
COMMIT;
```

One mechanism. Two behaviors. Just different snapshot timing. Beautiful.

---

## Chapter 4: Version Chains — The Physical Reality

Now let's talk about how multiple versions of the same row are physically connected on disk. This gets into PostgreSQL's heap storage format.

PostgreSQL's storage is organized into **8KB pages**. Each page holds multiple row versions (called **tuples** in PostgreSQL). When you UPDATE a row, the new version is written into whatever page has free space — not necessarily the same page as the old version.

So the "version chain" in PostgreSQL is not a linked list of pointers in the traditional sense. Instead:

```
Old tuple at (page 3, slot 2):
  xmin=1001, xmax=1042, ctid=(7,4)  ← ctid points to NEW version

New tuple at (page 7, slot 4):
  xmin=1042, xmax=0, ctid=(7,4)     ← ctid points to itself (latest version)
```

The `ctid` of an old tuple points to the newer version. This creates a forward chain:

```
(page 3, slot 2) ──ctid──▶ (page 7, slot 4) ──ctid──▶ itself (latest)
xmin=1001, xmax=1042        xmin=1042, xmax=0
Alice $1000 (dead)          Alice $800  (alive)
```

To find the right version for a given snapshot, PostgreSQL:
1. Starts at the latest version (from the index)
2. Checks visibility. If not visible, follows ctid backward... actually wait.

Here's a subtlety: the chain goes **forward** via ctid. But indexes always point to the **latest** version's ctid. When you do an index scan and land on a tuple, you check if it's visible. If it isn't (too new), you need to find an older version — but how?

PostgreSQL follows the chain **backwards** using a structure called the **HOT chain** (Heap-Only Tuple). For updates to rows that don't change indexed columns, PostgreSQL can keep the chain within the same page, and the old version's ctid points forward to the new version. Traversal starts from the index entry pointing to the page root, then follows the chain.

For updates that DO change indexed columns or land on a different page, new index entries are created for the new version. The old version is eventually vacuumed away.

---

### The HOT Optimization — Heap-Only Tuples

This is an important performance optimization worth understanding. Consider a table:

```sql
CREATE TABLE accounts (id INT, name TEXT, balance INT);
CREATE INDEX ON accounts(id);
```

When you UPDATE only the `balance` (not the indexed `id`), PostgreSQL can use a **HOT update**:

```
Page 0:
  Slot 1: xmin=1001, xmax=1042, ctid=(0,2), id=1, name=Alice, balance=1000
  Slot 2: xmin=1042, xmax=0,    ctid=(0,2), id=1, name=Alice, balance=800

Index entry for id=1: points to (0,1)  ← still points to OLD version
```

When you index-scan for `id=1`, you land on slot 1 (old version). PostgreSQL sees it's a HOT chain (a flag is set on the tuple), follows ctid to slot 2, checks visibility — sees new version is visible — returns it.

**Why is this a win?** Because the index doesn't need to be updated. No index bloat. No extra index writes. For workloads that heavily update non-indexed columns (like `balance`), this is a massive performance win.

---

## Chapter 5: The Visibility Edge Cases PostgreSQL Must Handle

Real production databases have edge cases that test the visibility rules to their limits. Let's go through the important ones.

---

### Edge Case 1: The Current Transaction's Own Writes

A transaction should always see its own writes — even though they haven't committed yet.

```sql
BEGIN; -- TXN-1050
INSERT INTO accounts (id, name, balance) VALUES (3, 'Carol', 300);
SELECT * FROM accounts WHERE id = 3; -- should see Carol!
COMMIT;
```

The xmin of Carol's row is 1050 (our own transaction). The commit log says 1050 is IN_PROGRESS — not committed yet.

The visibility rule for your own writes:

```
If xmin == current_transaction_id:
  → ALWAYS VISIBLE (you can see your own changes)
```

Simple exception. Hardcoded. Your own writes are always visible to you.

---

### Edge Case 2: Sub-transactions and Savepoints

PostgreSQL supports savepoints — partial rollbacks within a transaction:

```sql
BEGIN; -- TXN-1060
UPDATE accounts SET balance = 900 WHERE id = 1; -- change 1
SAVEPOINT my_savepoint;
UPDATE accounts SET balance = 850 WHERE id = 1; -- change 2
ROLLBACK TO SAVEPOINT my_savepoint;             -- undo change 2
-- change 1 should still be visible
COMMIT;
```

PostgreSQL handles this with **sub-transaction IDs**. Each savepoint gets its own sub-XID. The commit log tracks sub-transaction status separately. Visibility rules check both the main XID and sub-XIDs.

```
Row versions:
  xmin=1060,   balance=1000  (original)
  xmin=1060/1, balance=900   (sub-txn 1, committed to savepoint)
  xmin=1060/2, balance=850   (sub-txn 2, rolled back)

After ROLLBACK TO SAVEPOINT:
  sub-txn 2 is marked aborted
  Row with balance=850 becomes invisible
  Row with balance=900 is visible (sub-txn 1 committed)
```

---

### Edge Case 3: The Wraparound Problem — PostgreSQL's Achilles Heel

Transaction IDs in PostgreSQL are 32-bit integers. They increment forever. With enough transactions, the counter wraps around from 2³² back to 0.

This is catastrophic. If the counter wraps around, old XIDs become "newer" than current XIDs. Visibility rules break completely. Old data might suddenly appear invisible (as if from the future). The database corrupts.

PostgreSQL's solution: **transaction ID wraparound protection via VACUUM FREEZE.**

Every row has an `xmin`. VACUUM periodically walks through tables and, for rows old enough that every running transaction can definitely see them, **freezes** them — replacing their xmin with a special frozen XID that is always considered "committed in the past."

```
Normal xmin: 1042  (specific transaction ID)
Frozen xmin: 2     (FrozenTransactionId — always visible)
```

Once frozen, the row's visibility no longer depends on XID arithmetic. It's permanently visible to all future transactions.

PostgreSQL tracks `relfrozenxid` (per-table freeze horizon) and `datfrozenxid` (per-database). If a table isn't vacuumed for ~2 billion transactions (about 1-2 years at high transaction rates), PostgreSQL will:

1. **Warn loudly** in logs
2. Eventually **refuse to accept new transactions** until freeze VACUUM runs
3. In extreme cases, **shut down** rather than risk wraparound corruption

This is the XID wraparound emergency — one of the most dramatic PostgreSQL operational failures. There are war stories of production databases with billions of transactions suddenly refusing writes because nobody remembered to vacuum a rarely-updated table.

The lesson: **autovacuum is not optional. It's a fundamental safety mechanism for MVCC correctness.**

---

### Edge Case 4: The Commit Log Itself Can Become Stale

The clog (pg_xact) records transaction status. But it can only hold so many entries before it's also truncated (for space). How does PostgreSQL handle visibility for very old transactions whose clog entries have been removed?

Answer: if an xmin is older than the database's `relfrozenxid`, it's assumed to be committed (because VACUUM should have frozen it by then). If VACUUM froze the row, xmin was replaced with FrozenXID. If somehow the row wasn't frozen and clog was truncated, it's treated as committed under the assumption that anything that old must have committed.

This is a pragmatic approximation. It works in practice because VACUUM freeze always runs before clog truncation by design.

---

## Chapter 6: Dead Tuples, VACUUM, and the Storage Cost of MVCC

Every UPDATE creates a new row version. Every DELETE marks a row as dead. Over time, your tables fill with dead versions. Let's quantify this.

Suppose you have a `sessions` table with 1 million rows, and sessions are updated 100 times each before expiring:

```
Total row versions on disk:
  1,000,000 live rows
  × 100 updates each
  = 100,000,000 row versions physically on disk

But only 1,000,000 are visible to any transaction.
99% of your storage is dead weight.
```

This is table bloat in the extreme. Without VACUUM, your 1GB table becomes 100GB.

---

### How VACUUM Works — Step by Step

```
VACUUM accounts;
```

VACUUM does NOT lock the table. It runs concurrently with normal reads and writes. Here's what it does:

**Phase 1: Heap Scan**

VACUUM scans every page of the table. For each row version, it checks: *"Is any currently running transaction old enough that it might need this dead version?"*

The oldest running transaction's snapshot defines the **oldest XID of interest** (`OldestXmin`). Any dead row version with `xmax < OldestXmin` is truly dead — no running transaction can ever see it.

```
OldestXmin = 1800  (the snapshot XID of the oldest running transaction)

Dead row: xmax=1042 (deleted by committed TXN-1042)
  Is 1042 < 1800? Yes.
  Is any running transaction using a snapshot older than 1042? 
  → Check OldestXmin. 1042 < 1800 means no.
  → This dead row is safe to remove. ✓
```

**Phase 2: Index Cleanup**

For each dead row that will be removed, VACUUM first removes its entries from all indexes. This is the slow part — indexes must be updated.

**Phase 3: Heap Cleanup**

VACUUM marks the freed space in each page as reusable. It updates the **free space map** (FSM) — a data structure tracking free space per page — so future inserts and updates can fill these gaps.

Importantly: **VACUUM does not move rows or compact pages by default.** It just marks space as reusable. If a page has 10 dead rows and 2 live rows, the dead space is marked free but the 2 live rows stay exactly where they are.

`VACUUM FULL` does compact — it rewrites the entire table — but holds an exclusive lock for the entire duration. Typically avoided in production.

**Phase 4: Freeze**

VACUUM also freezes old-enough rows — replacing xmin with FrozenXID for rows that are older than `vacuum_freeze_min_age` transactions ago. This keeps XID wraparound at bay.

---

### The Bloat Equation — When VACUUM Can't Keep Up

VACUUM is a background process. If your write rate exceeds VACUUM's cleaning speed, bloat accumulates. Several factors make this worse:

**Long-running transactions** — if any transaction stays open for a long time (even just reading), it pins `OldestXmin` at its snapshot time. VACUUM cannot clean any dead rows newer than that snapshot. A 2-hour reporting query can prevent VACUUM from cleaning 2 hours of updates.

```
Long-running TXN opened at T=1000:
  OldestXmin = 1000
  
2 hours later: database has generated XIDs 1000 through 500,000
  VACUUM cannot clean ANY dead rows from those 500,000 transactions
  Table bloat grows continuously for 2 hours
  
TXN finishes:
  OldestXmin jumps to 500,000
  VACUUM suddenly has 500,000 transactions of dead rows to clean
  Massive VACUUM spike hits the disk
```

This is why **long-running transactions are dangerous in PostgreSQL** — not just for locking reasons, but because they cause MVCC bloat accumulation.

---

## Chapter 7: CockroachDB — Distributed MVCC

Now we move from a single machine to a distributed system. CockroachDB took PostgreSQL's MVCC ideas and reimagined them for a distributed, multi-node, globally-replicated database.

The challenge: PostgreSQL's MVCC works because a single machine has a single, authoritative transaction ID counter. In a distributed system:
- Multiple nodes are accepting writes simultaneously
- Each node could increment its own counter
- Coordinating a global counter adds latency

CockroachDB's solution changes the fundamental unit of time.

---

### The Timestamp Revolution — Replacing XIDs with Hybrid Logical Clocks

PostgreSQL uses **transaction IDs** (integers) to order events. CockroachDB uses **timestamps** — but not simple wall-clock timestamps (which can't be trusted across machines due to clock skew, as we learned in the Spanner chapter).

CockroachDB uses **Hybrid Logical Clocks (HLC)** — a clever combination of wall-clock time and logical counters.

```
HLC timestamp = (physical_time, logical_counter)

Examples:
  (1704067200.000, 0)   ← Jan 1 2024, 00:00:00.000, counter 0
  (1704067200.000, 1)   ← same millisecond, next event
  (1704067200.001, 0)   ← next millisecond
```

The rules of HLC:
1. Physical time advances as the wall clock advances (within bounded drift)
2. When two events happen in the same physical millisecond, the logical counter breaks the tie
3. When a node receives a message with a higher timestamp than its own clock, it advances its clock to that timestamp + 1

```
Node A's HLC: (T=1000, L=0)
Node A receives message from Node B: (T=1005, L=3)
Node A advances: max(1000, 1005) = 1005, counter = 4
Node A's new HLC: (T=1005, L=4)
```

This guarantees: **if event A causally precedes event B (A happened-before B), then A's HLC timestamp is strictly less than B's HLC timestamp.** Causality is preserved across the entire distributed system without a central counter.

---

### CockroachDB's MVCC Storage — RocksDB Key-Value

CockroachDB doesn't use PostgreSQL's heap storage. It stores all data in **RocksDB** (now called Pebble, CockroachDB's own fork) — a key-value store based on **LSM trees** (Log-Structured Merge trees).

Every row version is stored as a key-value pair where the key encodes both the row key AND the timestamp:

```
Key format: [table_id][primary_key_columns][timestamp]

Examples for Alice's account (account_id=1):
  /accounts/1/T=1704067200.000  →  {balance: 1000}   ← version at T=1000
  /accounts/1/T=1704067200.150  →  {balance: 800}    ← version at T=1000.150
  /accounts/1/T=1704067200.300  →  {balance: 750}    ← version at T=1000.300
```

All versions of a row are stored **sorted by timestamp** in the same key range. Finding the right version for a given timestamp is just a range scan — "give me the latest key for `/accounts/1/` with timestamp ≤ my_snapshot_timestamp."

```
My snapshot: T=1704067200.200

Scan: /accounts/1/T=???
  /accounts/1/T=1704067200.300  → too new (> 200), skip
  /accounts/1/T=1704067200.150  → visible! (≤ 200)
  → return {balance: 800}
```

This is dramatically different from PostgreSQL's heap approach. There are no dead tuples scattered through pages. Old versions are sorted together in the LSM tree. Cleanup (analogous to VACUUM) happens through LSM **compaction** — a natural background process of the storage engine.

---

### Distributed Transactions — The Timestamp Ordering Protocol

Now the hard part: CockroachDB allows **any node** to initiate a transaction. How do you prevent write conflicts and maintain serializability without a central coordinator?

CockroachDB uses a protocol called **Timestamp Ordering with Uncertainty Windows.**

When a transaction starts, it gets a start timestamp from its local HLC. When it reads data, it wants to see all commits with timestamp ≤ its start timestamp.

But here's the problem: due to clock skew, a write might have happened on another node at "timestamp 1000" but that node's clock is 50ms ahead of our node. From our perspective, that write looks like it happened "in the future." Do we see it or not?

CockroachDB's solution: **uncertainty intervals.**

Every transaction has:
- `read_timestamp` — "I want to see data as of this time"
- `max_timestamp` — `read_timestamp + max_clock_skew` (typically ~500ms)

When a transaction encounters a value with timestamp `T` where `read_timestamp < T < max_timestamp`:

```
Data could have been written before or after my actual start time.
I'm uncertain whether I should see it.
→ Restart the transaction with read_timestamp advanced to T.
```

This restart propagates the transaction's timestamp forward until it's definitively past the uncertain write. It's called a **transaction restart** and is transparent to the application — CockroachDB automatically retries.

```
TXN starts with read_ts=1000, max_ts=1500

Reads key X: finds version at timestamp=1100
  1000 < 1100 < 1500 → uncertain!
  Restart with read_ts=1101

Reads key X again: finds version at timestamp=1100
  1100 ≥ 1100, not uncertain anymore
  Sees version at 1100 → visible ✓
```

This is elegant because it's **correct under any clock skew within the max bound** without requiring globally synchronized clocks (unlike Spanner's TrueTime). The cost is occasional transaction restarts — but these are automatic and rare in practice.

---

### Write Intents — CockroachDB's Uncommitted Data Marker

In PostgreSQL, uncommitted row versions are stored directly in the heap with their xmin pointing to an in-progress transaction. The commit log tells you whether that transaction committed.

CockroachDB does something different: **write intents.**

When a transaction writes a value, it stores a **write intent** — a special marker that says "this value is tentative, written by transaction X":

```
/accounts/1/T=1704067200.150  →  INTENT: {balance: 800, txn_id: "abc-123"}
```

Write intents are stored in the same RocksDB keyspace as committed values. When another transaction encounters an intent:

1. It checks the **transaction record** for transaction "abc-123" — a separate key that stores the transaction's commit status.
2. If the transaction has committed, the intent is treated as a regular committed value.
3. If the transaction is still in progress, the encountering transaction must decide: wait, push the timestamp, or abort.

```
TXN-B reads /accounts/1/ and encounters INTENT from TXN-A:
  Look up TXN-A's transaction record:
    Status: COMMITTED, commit_timestamp=T=1704067200.200
  → Treat intent as committed at T=200
  → Is T=200 ≤ TXN-B's read_timestamp? 
    If yes: visible
    If no: not visible
```

This is how CockroachDB implements MVCC's core promise — uncommitted writes are visible only as intents, which are resolved lazily when encountered.

---

### CockroachDB's Serializable Isolation — SSI Distributed

CockroachDB runs at **Serializable** isolation by default — unlike PostgreSQL's default of Read Committed. This was a deliberate product decision: correctness over raw performance.

It implements SSI (Serializable Snapshot Isolation) distributedly, tracking read and write timestamps across nodes. The dependency graph detection we discussed in the isolation levels chapter is implemented using **timestamp intervals** — if a transaction's reads and writes don't form a dangerous pattern, it commits. If they do, one is restarted with a new timestamp.

The distributed nature means the SSI tracking data is stored in the transaction records, accessible to any node. When a transaction commits, it validates its reads haven't been invalidated by concurrent writes. This validation happens at the Raft layer — ensuring it's atomic and consistent across replicas.

---

## Chapter 8: How MVCC Powers Each Isolation Level — The Complete Picture

Now let's tie everything together. Here's exactly how MVCC implements each isolation level — end to end.

---

### Read Committed via MVCC

```
Mechanism: Fresh snapshot per statement

TXN starts
  Statement 1 executes:
    Take snapshot S1 = {current committed state}
    Read using S1
    Return results
    
  [Other transactions commit changes]
  
  Statement 2 executes:
    Take NEW snapshot S2 = {current committed state, includes new commits}
    Read using S2
    Return results
    
TXN ends
```

Result: Each statement sees the latest committed state. Dirty reads prevented (uncommitted data invisible). Non-repeatable reads possible (S1 ≠ S2).

---

### Repeatable Read via MVCC

```
Mechanism: Single snapshot for entire transaction

TXN starts
  Take snapshot S1 = {committed state at transaction start}
  
  Statement 1: use S1 → returns results R1
  
  [Other transactions commit changes]
  
  Statement 2: use S1 → returns same results R1
  (new commits invisible because they're outside S1)
  
TXN ends
```

Result: Same snapshot throughout. Non-repeatable reads prevented. Phantom reads possible in theory (new rows from concurrent INSERTs). 

Note: MySQL's InnoDB Repeatable Read additionally uses **gap locks** on range queries to prevent phantom inserts — making it effectively stronger than the SQL standard's Repeatable Read definition.

---

### Serializable (SSI) via MVCC

```
Mechanism: Single snapshot + dependency tracking

TXN starts
  Take snapshot S1
  Initialize read-set and write-set tracking
  
  Each read: record which rows/ranges were read and their timestamps
  Each write: record which rows were written
  
  At commit time: validate
    For each row I read: has it been written by a concurrent transaction?
    Do my reads and writes form a dangerous cycle with concurrent transactions?
    
    If no dangerous cycle: COMMIT ✓
    If dangerous cycle detected: ABORT, retry with new snapshot
    
TXN ends
```

Result: Execution equivalent to some serial order. All anomalies prevented.

---

### The Unified View

```
Isolation Level  │ Snapshot Timing   │ Extra Mechanism      │ Prevents
─────────────────┼───────────────────┼──────────────────────┼──────────────────
Read Uncommitted │ None (no MVCC)    │ None                 │ Nothing
Read Committed   │ Per statement     │ None                 │ Dirty reads
Repeatable Read  │ Per transaction   │ None (or gap locks)  │ + Non-repeatable
Snapshot Isolation│ Per transaction  │ None                 │ + Phantoms
Serializable SSI │ Per transaction   │ Dependency tracking  │ + Write skew
```

The beautiful insight: **MVCC is the foundation that makes all levels above Read Committed possible without heavy locking.** The snapshot mechanism is reused at every level — only the timing and supplementary tracking changes.

---

## Chapter 9: The Hidden Costs of MVCC — What Nobody Tells You

MVCC is not free. Let's be honest about the full cost:

---

**Cost 1: Storage Amplification**
Every update creates a new version. Heavy-update workloads can have 10-50x storage overhead during peak activity before VACUUM cleans up. Financial systems updating account balances thousands of times per second need careful VACUUM tuning.

**Cost 2: Read Performance Degradation Under Bloat**
When tables have heavy bloat, sequential scans read through pages that are mostly dead rows. A 10x bloated table means 10x more I/O for full table scans. Index scans suffer too — they land on many dead rows before finding live ones.

**Cost 3: VACUUM is Unpredictable**
VACUUM is a background process competing for I/O with your actual queries. Poorly tuned autovacuum causes either insufficient cleaning (bloat) or excessive I/O (query latency spikes). Getting VACUUM right is a specialist skill.

**Cost 4: Long Transaction Danger**
A single long-running transaction pins OldestXmin, preventing all dead row cleanup. In PostgreSQL, a forgotten idle transaction in `BEGIN` state can cause unbounded bloat accumulation. Production PostgreSQL monitoring MUST include tracking long-running transactions.

**Cost 5: Transaction ID / Timestamp Exhaustion**
PostgreSQL's XID wraparound is a real operational risk. CockroachDB's HLC timestamps can theoretically overflow (though at 2⁶⁴ nanoseconds ≈ 584 years, this is less pressing).

---

## The Full Story, End to End

```
Problem                         → MVCC Solution              → Remaining Challenge
────────────────────────────────────────────────────────────────────────────────
Locks cause readers to block    → Multiple versions,         → Storage bloat,
writers, killing performance      readers use old versions     VACUUM needed

Stale versions accumulate       → VACUUM background          → Long transactions
forever, wasting storage          process cleans dead rows     block VACUUM

Single-machine XIDs don't       → Hybrid Logical Clocks      → Clock uncertainty,
work across distributed nodes     (HLC) in CockroachDB         transaction restarts

Uncommitted data handling       → Write intents +            → Intent resolution
across distributed nodes          transaction records          coordination cost

Need different isolation        → Same MVCC engine,          → SSI tracking
levels with different            different snapshot            overhead,
performance tradeoffs            timing strategies             abort rates

Read Committed (default)        → Fresh snapshot per         → Dirty reads gone,
                                  statement                    non-repeatable stay

Repeatable Read                 → Transaction-lifetime        → Phantom reads
                                  snapshot                     possible (no locks)

Serializable (SSI)              → Snapshot + dependency      → Occasional aborts,
                                  cycle detection              slight overhead
```

---

**Where would you like to go next?** Natural threads from here:

- **LSM Trees vs B-Trees** — the two competing storage engines underneath all of this, and why CockroachDB chose LSM while PostgreSQL uses B-Trees
- **Distributed transactions in depth** — 2PC, Percolator (Google's distributed transaction protocol that CockroachDB is based on), Spanner's wound-wait
- **PostgreSQL internals deeper** — buffer pool management, WAL interaction with MVCC, checkpoint mechanics
- **Locking in depth** — deadlocks, lock queues, how MVCC and locks coexist (SELECT FOR UPDATE, advisory locks)
