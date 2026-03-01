# The Story of Database Locking 🔒

Let's start at the very beginning. Before we even talk about locks, we need to understand the world that made them necessary.

---

## Chapter 1: The Wild West — Life Without Locks

Imagine it's the early days of databases. You have a bank database. Simple. Just a table with account balances.

```
Accounts Table:
| account_id | name    | balance |
|------------|---------|---------|
| 101        | Alice   | $1000   |
| 102        | Bob     | $500    |
```

Now imagine **two bank tellers** working at the same time, both serving customers. Both of them are connected to the same database.

**Teller 1** is helping Alice transfer $200 to Bob.
**Teller 2** is also helping Alice withdraw $300 at the same time.

Here's what happens at the database level, millisecond by millisecond:

```
TIME →

Teller 1: READ Alice's balance → gets $1000
Teller 2: READ Alice's balance → gets $1000

Teller 1: Calculates new balance → $1000 - $200 = $800
Teller 2: Calculates new balance → $1000 - $300 = $700

Teller 1: WRITE Alice's balance → $800
Teller 2: WRITE Alice's balance → $700  ← 💥 OVERWRITES Teller 1's write!
```

**The result?** Alice's balance is $700. But she transferred $200 AND withdrew $300. She should have $500. She just got **$200 for free** because Teller 2 never saw Teller 1's write — it just obliterated it.

This is called the **Lost Update Problem**. One transaction's work got completely lost.

---

### But wait, it gets worse. Meet the Dirty Read.

Now imagine **Teller 1** is in the middle of a transfer — they've deducted $200 from Alice but haven't yet added it to Bob. Right at this exact moment:

```
Teller 1: Deducted $200 from Alice → Alice now has $800  (not committed yet)
                                                           ↑
Teller 2: READ Alice's balance right here → sees $800   (dirty! uncommitted data)

Teller 1: Something goes WRONG → transaction ROLLS BACK
          Alice goes back to $1000

Teller 2: Already made a decision based on $800 → WRONG!
```

Teller 2 read data that **never officially existed**. This is called a **Dirty Read** — reading uncommitted, possibly-about-to-be-rolled-back data.

---

### And there's one more ghost: The Phantom Read.

A manager runs a report: *"Show me all accounts with balance > $500"* — gets 3 accounts.

While they're looking at the report, someone opens a new account with $800.

The manager runs the report again — now gets 4 accounts.

**Same query, same transaction, different results.** The new row "appeared like a phantom." This is called a **Phantom Read**.

---

So by now, the engineers in the 1970s are pulling their hair out. They have **three serious problems:**

1. **Lost Updates** — concurrent writes obliterate each other
2. **Dirty Reads** — reading uncommitted, ghost data
3. **Phantom Reads** — results change mid-transaction because rows appear/disappear

They needed a way to make transactions feel like they're the **only one running**, even when thousands are happening simultaneously.

Their solution? **Locks.**

---

**The core insight was simple:** *Before you touch data, claim it. Make others wait.*

---

That's where Chapter 2 begins — the first generation of locks, how they worked, and the new nightmare they created called **Deadlock**.

---
# Chapter 2: The First Solution — Locks Are Born

So the engineers had a clear goal: **prevent two transactions from interfering with each other.** The idea they came up with was beautifully simple.

> *"Before you read or write data, put a flag on it. If someone else's flag is already there — wait."*

That flag is a **lock.**

But very quickly they realized — not all operations are equal. A **read** and a **write** are fundamentally different in terms of danger. So they didn't make just one type of lock. They made two.

---

## The Two Fundamental Locks

### 🔵 Shared Lock (S-Lock) — The "I'm Reading This" Lock

When a transaction wants to **read** data, it acquires a Shared Lock.

The key insight: **multiple transactions can read the same data at the same time safely.** Reading doesn't change anything. So shared locks are *compatible with each other* — many transactions can hold a shared lock on the same row simultaneously.

```
Transaction A: Shared Lock on Row 101 ✅
Transaction B: Shared Lock on Row 101 ✅  ← Also allowed! They're just reading.
Transaction C: Shared Lock on Row 101 ✅  ← Fine too!
```

But — if you hold a shared lock, **nobody can write** to that data while you're reading it.

---

### 🔴 Exclusive Lock (X-Lock) — The "I'm Writing This" Lock

When a transaction wants to **write** data, it acquires an Exclusive Lock.

This one is serious. **Nobody else can read OR write** to that data while you hold an exclusive lock. You have full, exclusive ownership.

```
Transaction A: Exclusive Lock on Row 101 ✅
Transaction B: Shared Lock on Row 101   ❌  ← Must WAIT
Transaction C: Exclusive Lock on Row 101 ❌  ← Must WAIT
```

---

### The Compatibility Matrix

This is the single most important table in locking theory. Memorize the logic, not the table:

```
                  Existing Lock →
                  None    Shared    Exclusive
Requested Lock ↓
Shared            ✅       ✅         ❌
Exclusive         ✅       ❌         ❌
```

The rule is simple: **Exclusive locks don't play with anyone. Shared locks play with other shared locks.**

---

## How This Solves Our Three Problems

Let's go back to our bank and replay those disasters — this time with locks.

### ✅ Lost Update — SOLVED

```
Teller 1 wants to UPDATE Alice's balance:
→ Requests EXCLUSIVE lock on Row 101
→ Gets it ✅

Teller 2 also wants to UPDATE Alice's balance:
→ Requests EXCLUSIVE lock on Row 101
→ ❌ Already held by Teller 1 → WAITS...

Teller 1 finishes, commits, releases lock.
Teller 2 wakes up, reads the UPDATED balance ($800), does its work correctly.
```

No more lost updates. They're **serialized** — forced to happen one after another.

---

### ✅ Dirty Read — SOLVED

```
Teller 1 is mid-transfer, has EXCLUSIVE lock on Alice's row (uncommitted):

Teller 2 wants to READ Alice's balance:
→ Requests SHARED lock on Row 101
→ ❌ Teller 1 holds EXCLUSIVE lock → WAITS...

Teller 1 either commits or rolls back, releases lock.
Teller 2 NOW reads — sees only committed data.
```

Dirty reads are impossible. You can't read data that's being written because the writer holds an exclusive lock.

---

### ✅ Phantom Reads — Partially addressed (we'll revisit this)

Range locks can be placed on a set of rows matching a condition. This is more complex — we'll come back to it when we talk about gap locks. For now, just know basic row locks don't fully solve phantoms.

---

## When Are Locks Released?

This question seems obvious but it caused a HUGE debate. The engineers tried the intuitive approach first:

### Approach 1: Release the Lock Right After You're Done With That Data

```
Transaction:
1. Lock Row 101, Read balance ($1000)     → Unlock Row 101 immediately
2. Lock Row 101, Write balance ($800)     → Unlock Row 101 immediately
3. Lock Row 102, Write balance (+$200)    → Unlock Row 102 immediately
4. Commit
```

Seems reasonable right? You're done with the row, let others use it.

**But this is catastrophically wrong.** Here's why:

```
Transaction A (Transfer $200 Alice→Bob):
Step 1: Lock 101, Read Alice = $1000, UNLOCK 101

                    ← RIGHT HERE, Transaction B sneaks in →
                    Transaction B: Lock 101, Read Alice = $1000, UNLOCK 101
                    Transaction B: Lock 101, Write Alice = $700, UNLOCK 101
                    Transaction B: Commit

Step 2: Transaction A: Lock 101, Write Alice = $800, UNLOCK 101
```

Transaction A read $1000, but by the time it writes, Transaction B already changed it to $700. Transaction A just **overwrote** B's committed work based on a **stale read**. We're back to lost updates even with locks!

---

### Approach 2: Two-Phase Locking (2PL) — The Real Solution

The engineers realized: **you cannot release a lock until your entire transaction is done.**

This gave birth to the most important locking protocol in database history: **Two-Phase Locking.**

The rule is:

> **Phase 1 — Growing Phase:** You can only ACQUIRE locks. Never release.
> **Phase 2 — Shrinking Phase:** You can only RELEASE locks. Never acquire.

```
Transaction Timeline:

GROWING PHASE             |  SHRINKING PHASE
                          |
Acquire Lock A ✅         |
Acquire Lock B ✅         |
Acquire Lock C ✅         |  ← Commit point (usually here)
                          |  Release Lock A
                          |  Release Lock B
                          |  Release Lock C
```

In practice, most databases implement **Strict 2PL** — an even stronger version where **all locks are held until commit or rollback.** This is cleaner and easier to reason about.

```
BEGIN TRANSACTION
  Lock Row 101 (Exclusive) ← Acquired
  Read Alice's balance
  Lock Row 102 (Exclusive) ← Acquired
  Update Alice's balance
  Update Bob's balance
COMMIT ← Only NOW are ALL locks released simultaneously
```

This guarantees that nobody sees your partial work, and you never make decisions based on data that someone else might change underneath you.

---

## So... Is Everything Solved?

The engineers were happy. 2PL with shared and exclusive locks seemed bulletproof.

Then two developers walked into the office one morning and found their database **completely frozen.** No queries running. Everything waiting. The database was **alive but doing absolutely nothing.**

They had just discovered **Deadlock** — and it's not a bug. It's a logical inevitability of locking.

---

## The Deadlock Nightmare

Here's the simplest possible deadlock:

```
Transaction A wants to transfer money: Alice → Bob
Transaction B wants to transfer money: Bob → Alice

Transaction A: Locks Row 101 (Alice) ✅
Transaction B: Locks Row 102 (Bob)   ✅

Transaction A: Tries to Lock Row 102 (Bob)   ❌ → WAITS for B
Transaction B: Tries to Lock Row 101 (Alice) ❌ → WAITS for A

A is waiting for B.
B is waiting for A.
Both wait.
Forever.
```

Visually:

```
    Transaction A ──── waiting for ────→ Transaction B
          ↑                                    │
          └──────── waiting for ───────────────┘
```

This is a **circular wait** — the defining characteristic of deadlock. Neither transaction can proceed. Neither will ever release its locks. The database is frozen on these two transactions.

And the scary part? **This is not a programming error.** Both transactions are doing exactly the right thing. They're just doing it in the wrong order relative to each other.

---

## How Databases Detect and Break Deadlocks

The database can't just wait forever. It needs to detect this cycle and break it.

### Detection: The Wait-For Graph

The database constantly maintains a graph of who is waiting for whom:

```
Node = Transaction
Edge = "is waiting for"

A → B → A   ← This cycle = DEADLOCK
```

When the database detects a cycle in this graph, it knows there's a deadlock. It then has to make a brutal decision — **pick one transaction as the victim and kill it.**

The killed transaction gets rolled back, its locks are released, and the other transaction can now proceed. The victim transaction gets an error back to the application, which typically retries.

**How does it pick the victim?** Different databases do this differently, but common strategies are: kill the transaction that's done the least work (cheapest to redo), kill the youngest transaction, or kill the one with the fewest locks.

```
Database detects: A → B → A  (cycle!)
Picks victim: Transaction B (did less work)
Rolls back B → releases Lock on Row 102
Transaction A can now proceed ✅
Transaction B gets error → application retries
```

---

### Prevention: Lock Ordering

Some systems try to **prevent** deadlocks entirely rather than detect and recover. The approach is elegant:

> **If every transaction always acquires locks in the same global order, circular waits become impossible.**

```
Rule: Always lock accounts in ascending order of account_id.

Transaction A (Alice→Bob): Lock 101 first, then 102 ✅
Transaction B (Bob→Alice): Lock 101 first, then 102 ✅  ← same order!

Now:
Transaction A: Locks 101 ✅
Transaction B: Tries to Lock 101 ❌ → WAITS (no deadlock, just waiting)
Transaction A: Locks 102 ✅, commits, releases both
Transaction B: Gets 101 ✅, then 102 ✅, proceeds

No cycle. No deadlock.
```

This works beautifully in theory. In practice it's very hard because application code doesn't always know in advance which rows it will need.

---

## Where We Are Now

Let's take stock of the journey so far:

```
Problem                  → Solution              → New Problem
─────────────────────────────────────────────────────────────
Concurrent writes        → Exclusive Locks        → 
Dirty Reads              → Shared Locks           → Deadlock
Lost Updates             → Two-Phase Locking      → Performance
                         → Strict 2PL             
```

And that performance problem is **massive.** Think about what strict locking means for a heavily-read database like a news website or a product catalog. Every read locks the row. Every lock makes writers wait. Every writer makes readers wait. The database becomes a **single-file queue** even when most operations don't actually conflict.

The engineers started to ask: *"Do reads really need to block writes? What if we could let readers see a consistent snapshot of the data without locking anything at all?"*

That question leads us to one of the most elegant ideas in computer science — **MVCC: Multi-Version Concurrency Control.**

---

Ready to go into Chapter 3? This is where things get really interesting — the database stops being a single truth and starts maintaining **multiple versions of reality simultaneously.**

---
# Chapter 3: Multiple Realities — MVCC

Let's start with the core frustration that led here.

After 2PL was deployed everywhere, database administrators started noticing something painful on their monitoring dashboards:

```
Active Queries:    1,247
Waiting Queries:   1,891  ← More waiting than working!
```

The problem was reads and writes blocking each other constantly. And when they looked at the actual workload, the reality was brutal:

> In most real-world databases, **80-95% of operations are READS.**

So the system was essentially making the vast majority of operations — harmless reads that change nothing — wait in line behind writes. A user just trying to view their account balance had to wait because someone else was updating a completely unrelated field on the same row.

The engineers asked a fundamental question:

> *"Why does a reader need to block a writer at all? Reading doesn't change anything. And why does a writer need to block readers? Can't they just... read the old version while the new one is being written?"*

This was the insight that changed everything.

---

## The Library Analogy

Imagine a library with one copy of a book. Old system (locking):

```
Person A wants to READ the book.
Person B wants to EDIT the book.

→ They fight over the one copy. One must wait.
```

Now imagine the library makes a **photocopy of the book** before Person B starts editing. Now:

```
Person A reads the photocopy (old version) ✅
Person B edits the original (new version) ✅

Both work simultaneously. Nobody waits. Nobody blocks.
```

When Person B is done, the edited version becomes the new "real" book. Person A, still reading the photocopy, sees a perfectly consistent version of the book as it existed when they started — even as Person B is changing it.

**This is MVCC.** Instead of one version of each row, the database keeps **multiple versions**, and gives each transaction the version that was current when *that transaction started.*

---

## How MVCC Actually Works Inside the Database

Every row in an MVCC database has hidden metadata columns that users never see:

```
Accounts Table (what you see):
| account_id | name  | balance |

Accounts Table (what actually exists):
| account_id | name  | balance | created_by_txn | deleted_by_txn |
|------------|-------|---------|----------------|----------------|
| 101        | Alice | $1000   | txn_001        | NULL           |
```

`created_by_txn` = which transaction created this version of the row
`deleted_by_txn` = which transaction deleted/replaced this version (NULL = still current)

Now let's walk through what happens during an update. This is the key moment.

---

### An Update Is Not An Update — It's a Delete + Insert

In MVCC, when you update a row, the database does **not** modify it in place. Instead:

1. It **marks the old version as deleted** by writing the current transaction ID into `deleted_by_txn`
2. It **inserts a brand new version** of the row with the new values

```
Transaction 500 updates Alice's balance from $1000 to $800:

BEFORE:
| 101 | Alice | $1000 | created: txn_001 | deleted: NULL    |

AFTER (two rows now exist):
| 101 | Alice | $1000 | created: txn_001 | deleted: txn_500 |  ← old version
| 101 | Alice | $800  | created: txn_500 | deleted: NULL    |  ← new version
```

Both versions exist simultaneously on disk. The old one isn't gone — it's just marked as "replaced by transaction 500."

---

### The Snapshot — Each Transaction Sees Its Own Reality

When a transaction begins, the database hands it a **snapshot** — essentially a list of which transactions were committed at the moment this transaction started.

```
Transaction 600 starts.
Snapshot given to txn_600: {txn_001, txn_002, ..., txn_499 committed}
                           {txn_500 is still in progress — NOT in snapshot}
```

Now when Transaction 600 reads Alice's row, it sees two versions:

```
| 101 | Alice | $1000 | created: txn_001 | deleted: txn_500 |
| 101 | Alice | $800  | created: txn_500 | deleted: NULL    |
```

The database applies the visibility rule:

> **A row version is visible to you if:**
> - The transaction that CREATED it is in your snapshot (it was committed before you started)
> - AND the transaction that DELETED it is NOT in your snapshot (either not committed, or committed after you started)

Let's apply this:

```
Version 1: $1000
  Created by txn_001 → In snapshot ✅
  Deleted by txn_500 → NOT in snapshot ✅ (txn_500 was running when we started)
  → VISIBLE! Transaction 600 sees $1000

Version 2: $800
  Created by txn_500 → NOT in snapshot ❌ (txn_500 was running when we started)
  → INVISIBLE. Transaction 600 cannot see this.
```

Transaction 600 sees **$1000** — the value as it existed when txn_600 started. Even though txn_500 has since changed it to $800. Even if txn_500 committed 3 seconds ago. Transaction 600 lives in **its own consistent snapshot of the past.**

---

### The Full Movie — Watching It Happen in Real Time

Let's watch two transactions run simultaneously and see exactly what each one sees:

```
Timeline →

txn_500: BEGIN
txn_600: BEGIN

txn_500: UPDATE Alice: $1000 → $800
         (old version marked deleted:txn_500, new version created:txn_500)

txn_600: SELECT Alice's balance
         → Applies visibility rules
         → Sees $1000  (txn_500 not in its snapshot, not committed yet)

txn_500: COMMIT

txn_600: SELECT Alice's balance AGAIN
         → Still sees $1000! (snapshot was taken at txn_600's start time)
         → txn_500 committing AFTER txn_600 started doesn't matter

txn_700: BEGIN  (after txn_500 committed)
txn_700: SELECT Alice's balance
         → txn_500 IS in its snapshot (committed before txn_700 started)
         → Sees $800 ✅

txn_600: COMMIT (or ROLLBACK, doesn't matter)
```

This is the superpower of MVCC: **Readers never block writers. Writers never block readers.** They live in parallel realities, each consistent and isolated.

---

## MVCC Solves Dirty Reads and Non-Repeatable Reads Elegantly

**Dirty Reads:** Impossible by design. You can only see versions created by *committed* transactions. An in-progress transaction's writes have a transaction ID that's not in your snapshot — so they're invisible to you. Even if that transaction rolls back, you never saw its data.

**Non-Repeatable Reads:** Also gone. Your snapshot is fixed at transaction start. No matter how many times you read the same row, you always see the same version — the one current when you started.

```
txn_600: SELECT Alice → $1000  (snapshot frozen at start)
txn_500: commits, Alice is now $800 in "reality"
txn_600: SELECT Alice → $1000  (still! snapshot hasn't changed)
```

You get **total consistency within your transaction** without holding a single read lock.

---

## The Garbage Collector — MVCC's Silent Janitor

At this point you might be thinking: *"Wait, if old row versions are never deleted, the database will grow forever. Every update adds a new row. The disk will fill up!"*

You're absolutely right. This is a real operational concern. Databases that use MVCC have a background process whose job is to clean up old versions that no living transaction can ever see anymore.

In PostgreSQL this is called **VACUUM.** In other databases it has different names, but the job is the same.

```
Old version: | 101 | Alice | $1000 | created: txn_001 | deleted: txn_500 |

VACUUM asks: "Is there any active transaction whose snapshot predates txn_500?"
             → If NO: this old version is invisible to everyone alive. DELETE IT.
             → If YES: keep it, someone might still need it.
```

This works great normally. But consider this scenario: a developer starts a transaction, gets distracted, and leaves it open for 4 hours without committing or rolling back. The database has to keep **every old version created in those 4 hours** because that long-running transaction's snapshot predates all of them. The table bloats enormously. This is called **table bloat** or **MVCC bloat**, and it's a real production problem that has taken down databases.

In PostgreSQL, this situation even has a famous worst-case: the **transaction ID wraparound problem** — but that's a deep operational rabbit hole. The point is: MVCC is not free. It trades lock contention for storage and cleanup complexity.

---

## But MVCC Doesn't Eliminate All Locks — The Write-Write Problem

Here's the thing MVCC cannot solve on its own:

```
txn_A: BEGIN (snapshot: sees Alice = $1000)
txn_B: BEGIN (snapshot: sees Alice = $1000)

txn_A: UPDATE Alice → $800  ✅ (creates new version)
txn_B: UPDATE Alice → $700  ❓
```

What should happen here? txn_B's update was based on seeing $1000. But txn_A has already changed it to $800. If txn_B just writes $700, txn_A's change is lost. We're back to the Lost Update problem.

**MVCC alone cannot resolve two concurrent writers on the same row.** The database still needs a mechanism here.

The solution: **When two transactions try to write the same row, one of them must wait (or be killed).** Writes still use locks. The breakthrough of MVCC is only about reads — reads no longer need to lock anything.

```
MVCC Rule for Writes:
"If you want to update a row and someone else is currently updating it too,
either wait for them to finish, or abort and let the application retry."
```

Different databases handle this differently:

**PostgreSQL:** Uses "first-writer-wins." If txn_B tries to update a row that txn_A is currently updating, txn_B waits. When txn_A commits, PostgreSQL says *"the data you based your update on has changed — you're aborted, retry."*

**MySQL InnoDB:** Similar but with some nuances in how it detects the conflict.

This "first writer wins" rule is important because it means MVCC-based systems can still have serialization failures that applications need to handle.

---

## The New Isolation Level Menu — MVCC Made This Possible

Because MVCC gives you such fine-grained control over what a transaction can see, it enabled databases to offer a **menu of isolation levels.** The SQL standard defines four:

```
Isolation Level      │ Dirty  │ Non-Repeatable │ Phantom
                     │ Reads  │ Reads          │ Reads
─────────────────────┼────────┼────────────────┼────────
READ UNCOMMITTED     │  ✅    │  ✅            │  ✅    ← sees everything, dangerous
READ COMMITTED       │  ❌    │  ✅            │  ✅    ← safe from dirty reads only
REPEATABLE READ      │  ❌    │  ❌            │  ✅    ← stable rows, but new rows appear
SERIALIZABLE         │  ❌    │  ❌            │  ❌    ← perfect isolation, most expensive
```

✅ = problem CAN occur, ❌ = problem prevented

In MVCC terms:

**READ COMMITTED** → Your snapshot refreshes **every statement.** You always see the latest committed data, but within one transaction, two reads of the same row might see different values.

**REPEATABLE READ** → Snapshot is taken **once at transaction start.** Every read in your transaction sees the same data. MySQL's default.

**SERIALIZABLE** → Full isolation. More on this later.

Most production databases run on **READ COMMITTED** as their default (PostgreSQL, Oracle, SQL Server) because it gives good performance with reasonable safety. MySQL defaults to **REPEATABLE READ.**

---

## Where We Are Now

```
Era 1: No concurrency control  → Chaos (dirty reads, lost updates, phantoms)
Era 2: Locks (2PL)             → Safe but slow (readers block writers, deadlocks)
Era 3: MVCC                    → Readers never block writers ✅
                                  Writers still coordinate with each other
                                  New cost: storage (old versions), vacuum complexity
```

Now we have a system that handles the common case — mostly-read workloads — beautifully. But there's still one class of problems that neither basic MVCC nor basic locking fully solves.

It's subtle. It's nasty. And it trips up even experienced developers.

It's called **Write Skew**, and it's the reason the SERIALIZABLE isolation level exists and is so hard to implement.

---

**Ready for Chapter 4?** This is where we encounter bugs that are almost impossible to debug — transactions that each look perfectly correct in isolation but together produce an impossible result.

---

# Chapter 4: The Subtle Killer — Write Skew & Serializable Isolation

Everything we've built so far feels solid. MVCC handles reads beautifully. Locks handle concurrent writes. Deadlock detection handles the circular wait problem.

But there's a class of bug that slips through **all of it.** No dirty reads. No lost updates. No phantom rows. Every transaction reads committed data. Every transaction writes correctly. And yet — the final state of the database is **impossible.** It could never have happened if the transactions ran one at a time.

This bug has a name: **Write Skew.**

---

## The Hospital On-Call System

Let's build a real scenario. A hospital has a rule:

> **At least one doctor must be on call at all times.**

The database:

```
Doctors Table:
| doctor_id | name    | on_call |
|-----------|---------|---------|
| 1         | Alice   | true    |
| 2         | Bob     | true    |
```

Both Alice and Bob are on call. The rule is satisfied — 2 doctors on call ✅

Now, simultaneously, both doctors decide to take themselves off call (maybe they're both sick):

```
Transaction A (Alice going off call):
  1. SELECT COUNT(*) FROM doctors WHERE on_call = true
     → Returns 2  ✅ (safe to go off call, someone else is still on)
  2. UPDATE doctors SET on_call = false WHERE doctor_id = 1

Transaction B (Bob going off call):
  1. SELECT COUNT(*) FROM doctors WHERE on_call = true
     → Returns 2  ✅ (safe to go off call, someone else is still on)
  2. UPDATE doctors SET on_call = false WHERE doctor_id = 2
```

Both transactions read 2 doctors on call. Both conclude it's safe to go off call. Both write to **different rows** — no write conflict, no waiting, no deadlock.

Both commit successfully.

```
Final state:
| 1  | Alice | false |
| 2  | Bob   | false |

Doctors on call: 0  💀
```

The hospital has zero doctors on call. A patient could die. And yet — from the database's perspective, **nothing went wrong.** No rules were violated at the row level. Both transactions read committed data. Both wrote to different rows.

---

## Why This Slips Through Everything

Let's understand precisely why MVCC + locks failed here:

**No Dirty Read:** Both transactions read committed data ✅

**No Lost Update:** They wrote to *different* rows — Alice's row and Bob's row. Neither overwrote the other ✅

**No write conflict:** Since they touched different rows, there was no row-level lock contention ✅

The problem is something more subtle. Each transaction made a decision based on a **premise** — *"there are 2 doctors on call."* Then each acted on that premise by changing data. But the combined effect of both actions **invalidated the premise that both of them relied on.**

Neither transaction's individual action was wrong. Together, they created an impossible state.

This is **Write Skew:** a transaction reads some data, makes a decision, and writes based on that decision — but another concurrent transaction has also read the same data and made a conflicting decision, and the two writes together violate a constraint that neither write alone would have violated.

---

## More Write Skew Examples So The Pattern Is Clear

Write skew is everywhere once you know how to see it. The pattern is always:

```
1. Read a set of rows to check a condition
2. Write to some row(s) based on that condition
3. Another transaction does the same
4. Together, the writes violate the condition
```

**Meeting Room Booking:**
```
Two people book the same room at the same time.
Both read: "Is room 101 free at 2pm?" → Both see: Yes
Both write: "Book room 101 at 2pm for me"
Both succeed. Room is double-booked.
```

**Username Uniqueness:**
```
Two people register with username "alice" simultaneously.
Both read: "Does username 'alice' exist?" → Both see: No
Both write: INSERT user with username 'alice'
Both succeed (if no unique constraint). Duplicate usernames.
```

*(Note: unique constraints at the DB level solve this specific case — but write skew appears in business logic constraints that can't always be expressed as DB constraints)*

**Inventory Oversell:**
```
100 customers try to buy the last item simultaneously.
All read: "Is quantity > 0?" → All see: 1
All write: quantity = quantity - 1
Result: quantity = -99. Item oversold 99 times.
```

---

## The Real Solution: Serializable Isolation

The engineers realized: the fundamental guarantee people actually want is this:

> **"My transaction should behave as if it ran completely alone — as if no other transaction existed while mine was running."**

This is called **Serializability.** The result of running N concurrent transactions should be equivalent to running them in *some* sequential order, one at a time.

```
Transactions A and B run concurrently.
Result must be equivalent to: (A then B) OR (B then A)
Either order is fine — but it must be equivalent to SOME serial order.
```

If A then B would give result X, and B then A would give result Y — the concurrent run must give either X or Y. Not some third impossible result Z.

The hospital scenario gives result Z — a state that can never arise from any serial ordering of those two transactions. That's the violation.

---

## How Do You Implement Serializability?

This is where it gets fascinating. There have been two major approaches.

---

### Approach 1: Predicate Locking — Lock What You READ, Not Just What You Write

The insight: Write skew happens because transactions make decisions based on **what they read** but only lock **what they write.** The fix: also lock what you read.

In the hospital example:

```
Transaction A reads: "doctors WHERE on_call = true" (2 rows)
→ Acquires a SHARED LOCK on this entire set of rows

Transaction B tries to read: "doctors WHERE on_call = true"
→ Also acquires a SHARED LOCK ✅ (shared locks are compatible)

Transaction A writes: doctor_id = 1, sets on_call = false
→ Acquires EXCLUSIVE LOCK on row 1

Transaction B writes: doctor_id = 2, sets on_call = false
→ Acquires EXCLUSIVE LOCK on row 2

BUT NOW — Transaction A's write changes the set of rows that match
"on_call = true." This overlaps with Transaction B's read set.
The predicate lock on B's read conflicts with A's write.
→ Transaction B must WAIT until A commits.

A commits. B re-reads: now only 1 doctor on call (Bob).
B checks the condition... only 1 doctor left.
B's logic says: "I cannot go off call." ✅
```

Predicate locking says: **lock the predicate (the WHERE condition), not just the rows.** Any write that would affect the result of your WHERE clause must wait for your transaction to finish.

**The problem with predicate locking:** It's brutally expensive. Checking whether a new write conflicts with some other transaction's WHERE clause requires checking every active predicate against every write. With thousands of concurrent transactions each with complex WHERE clauses, this becomes an enormous computational overhead.

In practice, databases approximate predicate locks with **index range locks** (also called gap locks) — which are simpler to check but slightly more conservative (lock a bit more than strictly necessary).

---

### Approach 2: Serializable Snapshot Isolation (SSI) — Optimistic & Brilliant

This is the modern approach, pioneered in a landmark 2008 research paper and implemented in PostgreSQL 9.1 (2011). It's elegant because it combines MVCC with a lightweight conflict detector.

The core idea is optimistic:

> **"Let everyone run freely. Don't block anything. But quietly track dependencies between transactions. If we detect that a cycle of dependencies formed — meaning serial execution would have been impossible — abort one of the conflicting transactions."**

Instead of preventing conflicts upfront (pessimistic), SSI detects them after the fact (optimistic).

Here's how the tracking works. SSI watches for a specific pattern called an **rw-antidependency**:

```
Transaction A READS data that Transaction B later WRITES
→ A has a "read-write antidependency" on B (written as A →rw B)

If we also see:
Transaction B READS data that Transaction A later WRITES
→ B has a "read-write antidependency" on A (written as B →rw A)

Two rw-antidependencies forming a cycle = SERIALIZATION FAILURE
→ Abort one transaction
```

Let's trace the hospital example through SSI:

```
txn_A reads: "WHERE on_call = true" (sees Alice, Bob)
txn_B reads: "WHERE on_call = true" (sees Alice, Bob)

txn_A writes: Alice → on_call = false
txn_B writes: Bob → on_call = false

SSI tracks:
→ txn_B's read (on_call = true) is affected by txn_A's write (Alice off call)
   txn_A →rw txn_B

→ txn_A's read (on_call = true) is affected by txn_B's write (Bob off call)
   txn_B →rw txn_A

CYCLE DETECTED: txn_A →rw txn_B →rw txn_A
→ ABORT one of them. Say txn_B gets aborted.

txn_A commits: Alice is off call, Bob is on call.
txn_B retries: now only 1 doctor on call → cannot go off call ✅
```

The beauty of SSI: **no blocking upfront.** Transactions run at full MVCC speed. Only when a genuine conflict is detected does one get aborted. In workloads where conflicts are rare (most real-world workloads), SSI performs nearly as well as REPEATABLE READ while giving full serializability.

---

## The Cost of Serializability

Nothing is free. Serializable isolation — whether predicate locks or SSI — has real costs:

**Throughput:** Genuinely conflicting workloads will see more aborts and retries. Your application must be prepared to retry transactions.

**False positives in SSI:** SSI's cycle detection is conservative. It sometimes aborts transactions that *would have been fine* — because the theoretical conflict existed even if the actual data didn't matter in that case. These are called "false positive serialization failures." They're safe (just wasteful retries) but they happen.

**Operational complexity:** Your application now needs retry logic for serialization failures. A transaction that was working fine for months might suddenly get a serialization error in production under load. Developers unfamiliar with this are blindsided.

---

## The Isolation Level Decision in Real Life

So which isolation level should you use? Here's how engineers actually think about it:

```
READ COMMITTED (default in PostgreSQL, Oracle):
  Use when: High throughput matters, transactions are simple,
            business logic doesn't have complex cross-row invariants.
  Risk:     Non-repeatable reads, write skew possible.

REPEATABLE READ (default in MySQL):
  Use when: Transactions read the same rows multiple times and need
            consistency. Reporting queries.
  Risk:     Write skew still possible. Phantom reads possible in some DBs.

SERIALIZABLE:
  Use when: Business constraints span multiple rows and cannot be expressed
            as DB constraints. Financial systems. Booking systems. Inventory.
  Cost:     Performance overhead, retry logic required.
```

The sad truth of the industry: most applications run on READ COMMITTED and just have subtle write skew bugs sitting silently in production. They manifest as occasional double-bookings, occasional oversells, occasional data anomalies that get blamed on "a bug" and fixed with a manual data patch — without anyone realizing the root cause is an isolation level mismatch.

---

## Where We Are Now

```
Problem                    → Solution                    → Cost/Tradeoff
───────────────────────────────────────────────────────────────────────
Dirty Reads                → MVCC Snapshots              → Storage bloat
Lost Updates               → Row-level write locks       → Write contention  
Non-repeatable Reads       → Snapshot at txn start       → Stale reads
Write Skew                 → Serializable Isolation      → Abort/retry overhead
                             (Predicate Locks or SSI)
```

We've now covered the entire correctness story of database locking. Every major problem and every major solution.

But there's one dimension we haven't talked about yet — **scale.**

Everything we've discussed assumes a single database machine. What happens when your data is so large, or your traffic so high, that you need to split it across multiple servers? Locking across a network is a completely different beast — and it's where distributed databases live.

But before we go there, there's one more topic in single-node locking that's extremely practical and affects every developer daily: **Lock Granularity.** Right now we've been casually saying "lock the row" — but real databases have a whole hierarchy of lock levels, from the entire table down to a single row, and choosing the wrong one has massive performance consequences.

---

**Two directions we can go:**

**Option A — Lock Granularity:** Table locks vs page locks vs row locks vs gap locks. How databases decide what level to lock, and how this affects your queries in MySQL and PostgreSQL right now.

**Option B — Distributed Locking:** What happens when your database spans multiple machines? Two-phase commit, distributed deadlocks, and why Google's Spanner needed atomic clocks.

Which world do you want to enter next?

---

# Chapter 5: The Lock Hierarchy — Granularity Matters Enormously

Let's start with a simple question that exposes the entire problem.

You run an e-commerce site. You have an `orders` table with **50 million rows.** A customer updates their single order. Meanwhile, 10,000 other customers are also updating their own orders simultaneously.

If you lock the **entire table** for each update — only one customer can update at a time. 9,999 customers are waiting. Your site is effectively single-threaded.

If you lock only the **single row** being updated — all 10,000 customers can update simultaneously, since they're all touching different rows.

The difference is **lock granularity** — how big a piece of data you claim when you lock.

But here's the twist: finer granularity isn't always better. It comes with its own costs. And real databases don't just choose one level — they use **multiple levels simultaneously**, managed through a hierarchy that is one of the most clever designs in database engineering.

---

## The Granularity Spectrum

Think of your database as a set of nested containers:

```
Database
└── Table
    └── Page  (physical disk page, typically 8KB, holds ~100-200 rows)
        └── Row
            └── Column (some DBs support this, rare)
```

Each level can potentially be locked. The tradeoff is always the same:

```
Coarser Lock (Table)          Finer Lock (Row)
─────────────────────────     ─────────────────────────
✅ Low overhead               ✅ High concurrency
   (1 lock to manage)            (only affects 1 row)
✅ Easy to manage             ✅ Other rows unaffected
❌ Blocks everything          ❌ High overhead
❌ Terrible concurrency          (1000 rows = 1000 locks)
                              ❌ Expensive to manage
                              ❌ Lock table memory usage
```

So there's a fundamental tension: the more precise your lock, the more locks you need to manage, and the more memory and CPU you spend on lock management itself.

---

## The Problem With Naive Fine-Grained Locking

Imagine you want to do a full table operation:

```sql
DELETE FROM orders WHERE status = 'cancelled';
-- This might affect 2 million rows
```

With row-level locking, the database would need to:
1. Acquire 2,000,000 individual row locks
2. Track all 2,000,000 in memory
3. Release all 2,000,000 at commit

Meanwhile, if another transaction tries to do:
```sql
ALTER TABLE orders ADD COLUMN shipped_at TIMESTAMP;
```

This needs to touch the entire table. How does it know if it's safe? It would have to **check every single row** to see if any row lock conflicts with its table-level operation. With 2 million row locks active, that check is impossibly expensive.

The engineers needed a way to efficiently answer the question: *"Is any part of this table currently locked?"* without scanning every row.

Their solution is elegant: **Intention Locks.**

---

## Intention Locks — The Announcement System

Before you lock a row, you must first announce your intention at every level above it in the hierarchy.

Three new lock types:

**IS — Intention Shared:** "I intend to place Shared locks on some rows below this level."

**IX — Intention Exclusive:** "I intend to place Exclusive locks on some rows below this level."

**SIX — Shared + Intention Exclusive:** "I have a Shared lock on this entire level AND intend to place Exclusive locks on some rows below." (Used for operations that read everything but update some rows.)

So when Transaction A updates a single row, it actually acquires:

```
1. IX lock on the DATABASE
2. IX lock on the TABLE
3. IX lock on the PAGE containing the row
4. X  lock on the ROW itself
```

Now when Transaction B wants to do a full table operation:

```
Transaction B wants to lock the entire TABLE with X lock.

It checks: does the TABLE have any IX or IS locks? → YES (Transaction A's IX)
X lock is incompatible with IX lock → Transaction B WAITS.

No row scanning needed. One check at the table level answers the question instantly.
```

---

### The Full Compatibility Matrix With Intention Locks

This is what real database lock managers implement:

```
          IS    IX    S     SIX   X
IS        ✅    ✅    ✅    ✅    ❌
IX        ✅    ✅    ❌    ❌    ❌
S         ✅    ❌    ✅    ❌    ❌
SIX       ✅    ❌    ❌    ❌    ❌
X         ❌    ❌    ❌    ❌    ❌
```

The logic behind this:
- IS and IX are compatible with each other — many transactions can simultaneously be "intending" to do things on different rows
- S and IX conflict — "I'm reading everything" conflicts with "someone is exclusively writing some rows"
- X conflicts with everything — full table exclusive means nobody else can touch anything

---

## Now Let's Talk About Gap Locks — The Phantom Killer

We left a loose thread in Chapter 2: **Phantom Reads.** Basic row locks can't prevent new rows from appearing. Let's fix that now.

Recall the phantom scenario:

```sql
-- Transaction A, running under REPEATABLE READ
SELECT * FROM orders WHERE amount > 1000;
-- Returns 5 rows. Makes a decision based on this.

-- Transaction B (concurrent)
INSERT INTO orders (amount) VALUES (1500);
-- Commits.

-- Transaction A, same transaction
SELECT * FROM orders WHERE amount > 1000;
-- Returns 6 rows! A new phantom appeared.
```

The problem: Transaction A locked 5 existing rows, but couldn't lock the *gap* where new rows could be inserted.

The solution: **Gap Locks.** Lock not just the row, but the **gap before it** in the index.

---

### Understanding the Index Structure

To understand gap locks, you need to picture how a B-tree index looks. For an index on `amount`:

```
Index on 'amount' column:

... | 800 | 900 | 1000 | 1200 | 1500 | 2000 | ...
     gap0   gap1  gap2   gap3   gap4   gap5
```

Each value in the index has a **gap before it** — the space where new values could be inserted.

A gap lock on a gap means: **no new rows can be inserted into this gap.**

```
Transaction A: SELECT WHERE amount > 1000

Rows returned: 1200, 1500, 2000

Gap locks acquired:
- Gap before 1200 (the gap between 1000 and 1200)
- Gap before 1500
- Gap before 2000
- Gap after 2000 (the "supremum" — open-ended upper bound)
```

Now if Transaction B tries to insert amount = 1300:

```
INSERT INTO orders (amount) VALUES (1300)
→ 1300 would go in the gap between 1200 and 1500
→ That gap is LOCKED by Transaction A
→ Transaction B WAITS (or gets an error if it's a lock-nowait scenario)
```

Phantom prevented. ✅

---

### Next Record Locks = Row Lock + Gap Lock

MySQL InnoDB has a compound lock type called a **Next-Key Lock** that combines a row lock and the gap lock before it into a single unit. It's the default for REPEATABLE READ in MySQL.

```
Next-Key Lock on value 1200:
= Row lock on the record 1200
+ Gap lock on the gap (1000, 1200)
```

When MySQL does `SELECT WHERE amount > 1000 FOR UPDATE`, it places next-key locks on every row in the result set, which effectively locks both the rows and all the gaps between them, preventing any inserts into that range.

---

### The Gap Lock Pain Point

Gap locks are conservative. They lock a range, not just specific rows. This causes surprising blocking behavior:

```sql
-- Table has rows with id: 1, 5, 10, 20

-- Transaction A
SELECT * FROM orders WHERE id BETWEEN 6 AND 9 FOR UPDATE;
-- Returns NO ROWS (none exist in this range)
-- But acquires gap lock on (5, 10)

-- Transaction B
INSERT INTO orders (id) VALUES (7);
-- BLOCKED! Even though Transaction A found nothing,
-- its gap lock prevents insertion in that range.
```

This confuses developers endlessly. "Why is my INSERT blocked when no rows match the SELECT?" — because the gap itself is locked.

---

## Table-Level Locks — When Row Locks Are Too Expensive

Sometimes you actually *want* a table lock. Specifically, DDL operations (schema changes) need them:

```sql
ALTER TABLE orders ADD COLUMN notes TEXT;
-- Needs to modify every row. Must have exclusive table access.
```

But also, MySQL uses table-level locks in one important scenario: **when it can't use an index.**

```sql
-- No index on 'notes' column
UPDATE orders SET status = 'reviewed' WHERE notes LIKE '%urgent%';
```

Without an index, MySQL must scan every row. If it used row locks, it would need to lock every row anyway — that's millions of row locks. MySQL's optimizer sometimes decides: *"I'm touching so much of this table anyway, just take a table lock."* This is called **lock escalation.**

---

## Lock Escalation — When Fine-Grained Locks Become Coarse

SQL Server does this explicitly and automatically. If a transaction acquires too many row locks (default threshold: 5,000 locks, or >20% of a table), SQL Server **escalates** them to a single table lock:

```
Transaction acquires:
Row lock 1, row lock 2, ... row lock 4,999, row lock 5,000 → ESCALATION
All 5,000 row locks replaced by 1 table lock.

Memory: 5,000 lock objects → 1 lock object ✅
Concurrency: Many rows blocked → Entire table blocked ❌
```

This is a major operational concern. Unexpected lock escalation can suddenly block an entire table when you expected only row-level contention. DBAs tune escalation thresholds carefully.

PostgreSQL takes a different philosophy: **it never escalates.** It will happily hold millions of row locks if needed. This avoids the surprise blocking, but can exhaust lock memory (`max_locks_per_transaction` limit).

---

## Practical Reality: What MySQL InnoDB and PostgreSQL Actually Do

Let's ground all this theory in what actually happens when you write SQL.

### MySQL InnoDB Lock Behavior

```sql
-- Plain SELECT: No locks acquired (MVCC snapshot read)
SELECT * FROM orders WHERE id = 42;

-- SELECT with explicit lock: Shared lock on row
SELECT * FROM orders WHERE id = 42 LOCK IN SHARE MODE;

-- SELECT with exclusive lock: Exclusive lock on row + gap locks
SELECT * FROM orders WHERE id = 42 FOR UPDATE;

-- UPDATE/DELETE: Exclusive lock on matching rows + gap locks on ranges
UPDATE orders SET status = 'shipped' WHERE id = 42;
DELETE FROM orders WHERE amount > 1000;  -- locks rows + all gaps in range
```

MySQL's default isolation (REPEATABLE READ) uses next-key locks aggressively to prevent phantoms. This is why MySQL deadlocks are so common — gap locks block insertions even when there's no actual data conflict.

---

### PostgreSQL Lock Behavior

```sql
-- Plain SELECT: No locks at all (pure MVCC)
SELECT * FROM orders WHERE id = 42;

-- SELECT FOR SHARE: Row-level shared lock
SELECT * FROM orders WHERE id = 42 FOR SHARE;

-- SELECT FOR UPDATE: Row-level exclusive lock
SELECT * FROM orders WHERE id = 42 FOR UPDATE;

-- UPDATE/DELETE: Row-level exclusive lock on affected rows
-- No gap locks! PostgreSQL relies on SSI for phantom prevention
-- at SERIALIZABLE level, not gap locks.
UPDATE orders SET status = 'shipped' WHERE id = 42;
```

PostgreSQL is more relaxed about gap locks because it uses SSI (from Chapter 4) for serializable isolation rather than gap locks. Under REPEATABLE READ, PostgreSQL doesn't prevent phantoms with gap locks — it just doesn't see them due to its snapshot. Under SERIALIZABLE, SSI catches the conflict.

---

## The Advisory Lock — A Lock For Your Application Logic

There's one more type of lock that doesn't fit the row/table hierarchy at all, but is incredibly useful: **Advisory Locks.**

These are locks you create manually, with arbitrary names, for coordinating application logic that has no corresponding database rows.

```sql
-- PostgreSQL advisory lock
SELECT pg_advisory_lock(12345);
-- Now do some work that needs exclusivity
-- Another connection trying pg_advisory_lock(12345) will WAIT
SELECT pg_advisory_unlock(12345);
```

Real use case: You have a background job that runs every 5 minutes. If two servers start it simultaneously, bad things happen (duplicate emails sent, double billing, etc.). Row locks don't help because there's no "running job" row to lock.

```sql
-- Background job starts
SELECT pg_try_advisory_lock(99999);  -- returns TRUE if got lock, FALSE if already taken

-- If FALSE: another server is running it → exit immediately
-- If TRUE:  we have exclusive access → run the job
--           Lock is automatically released when session ends
```

Advisory locks are named by integer keys. The application decides what the numbers mean — the database just enforces mutual exclusion. It's essentially a distributed mutex living inside your database.

---

## Putting It All Together — The Lock Hierarchy In One Picture

```
When you run: UPDATE orders SET status='shipped' WHERE customer_id = 42

Lock acquisition sequence:
┌─────────────────────────────────────────────────┐
│  IX lock on DATABASE                            │  ← "I intend to exclusively
├─────────────────────────────────────────────────┤     modify something below"
│  IX lock on TABLE 'orders'                      │
├─────────────────────────────────────────────────┤
│  IX lock on PAGE containing the row             │
├─────────────────────────────────────────────────┤
│  X lock on ROW (customer_id = 42)               │  ← Actual data lock
├─────────────────────────────────────────────────┤
│  Gap lock on index gap around customer_id = 42  │  ← Prevents phantom inserts
│  (MySQL REPEATABLE READ only)                   │     near this key value
└─────────────────────────────────────────────────┘
```

A concurrent `ALTER TABLE orders ADD COLUMN ...` just checks: *"Does TABLE 'orders' have an IX lock?"* → Yes → Wait. One check. No row scanning. The hierarchy makes it efficient.

---

## The Practical Advice That Comes From All This

Understanding granularity has immediate, practical consequences for how you write SQL:

**Always filter on indexed columns in write queries.** An UPDATE or DELETE without an index forces a full table scan, which acquires far more locks than necessary and can escalate to a table lock.

```sql
-- BAD: No index on 'notes', locks entire table scan
UPDATE orders SET priority = 'high' WHERE notes LIKE '%urgent%';

-- GOOD: Index on customer_id, locks only matching rows  
UPDATE orders SET priority = 'high' WHERE customer_id = 42;
```

**Keep transactions short.** Locks are held until commit. A transaction that takes 10 seconds holds every lock it acquired for 10 seconds. Other transactions pile up waiting.

```sql
-- BAD: Locks held while application does slow external work
BEGIN;
  SELECT * FROM orders WHERE id = 42 FOR UPDATE;
  -- application calls external payment API (takes 3 seconds)
  UPDATE orders SET status = 'paid' WHERE id = 42;
COMMIT;

-- GOOD: Do external work first, then do a short transaction
-- [external payment API call happens here, outside transaction]
BEGIN;
  UPDATE orders SET status = 'paid' WHERE id = 42;
COMMIT;
```

**Use `SELECT FOR UPDATE` deliberately, not accidentally.** Many ORM frameworks issue `SELECT FOR UPDATE` when you call "find and lock" methods. Know when your ORM is doing this — it holds an exclusive lock until your transaction ends.

**Watch out for gap lock deadlocks in MySQL.** Two transactions inserting rows near the same index range can deadlock through gap locks even though they're inserting completely different rows.

```sql
-- Both transactions run concurrently in MySQL (REPEATABLE READ)
-- Table has rows with id 10 and 20

-- Transaction A: gap lock on (10, 20), then tries to insert 15
-- Transaction B: gap lock on (10, 20), then tries to insert 15
-- Deadlock: each holds a gap lock the other needs
```

---

## Where We Are Now — The Complete Single-Node Picture

```
Concept               What it solves               What it costs
──────────────────────────────────────────────────────────────────
Shared/Exclusive Locks  Basic read/write isolation   Contention
Two-Phase Locking       Correctness across ops       Deadlock risk
MVCC                    Readers don't block writers  Storage, vacuum
Intention Locks         Efficient hierarchy checks   Complexity
Gap Locks               Phantom prevention           Over-blocking
SSI                     Write skew, serializability  Abort/retry
Lock Granularity        Concurrency vs overhead      Tuning needed
```

This is the complete picture of single-node database locking. Every major database — PostgreSQL, MySQL, Oracle, SQL Server — is built on these exact foundations. The implementations differ in details but the concepts are identical.

---

Now — we have one major frontier left: **Option B, Distributed Locking.**

Everything above assumes one machine. One lock manager. One source of truth about who holds what lock. When you add a second machine — and a network between them — every single assumption breaks. Clocks disagree. Messages get lost. Nodes crash mid-operation. And yet the same correctness guarantees must hold.

This is where Google needed atomic clocks, where Amazon built DynamoDB's design around giving up consistency, and where the famous CAP theorem lives.

**Ready to go distributed?**
