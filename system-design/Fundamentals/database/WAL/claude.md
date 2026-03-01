## The Story of Databases and the Write-Ahead Log

Let's go back. Way back. Before replication, before sharding, before any of the distributed complexity we just covered.

Back to a single machine. A single database. And a deceptively simple question that took decades to fully answer:

> *"How do we make sure data isn't lost or corrupted when the power goes out?"*

---

## Chapter 1: The Naive Beginning — "Just Write It To Disk"

It's the 1970s. You're building one of the first database systems. You have a table of bank accounts in memory:

```
Memory (RAM):
┌─────────────────────────────────┐
│ account_id │ name  │ balance    │
│ 001        │ Alice │ $1,000     │
│ 002        │ Bob   │ $500       │
└─────────────────────────────────┘
```

A user says: *"Transfer $200 from Alice to Bob."*

You do the math in memory:
```
Alice: $1,000 → $800
Bob:   $500   → $700
```

Then you write those new values to disk. Done. Simple.

Until — halfway through writing Alice's row to disk — **the power goes out.**

You come back online. Alice's row on disk shows `$800` (already written). Bob's row on disk still shows `$500` (not yet written). **$200 just vanished from the universe.**

```
Disk after crash:
Alice: $800   ← updated
Bob:   $500   ← not updated
              ← $200 is gone
```

This is called a **partial write** — and it's not a theoretical edge case. Disks are slow. Writing a database page takes milliseconds. A lot can happen in milliseconds. Power failures, kernel panics, process crashes — any of them can interrupt a write exactly halfway through.

The early database builders had to answer a question that sounds simple but isn't:

> *"How do you make a multi-step operation either fully happen or fully not happen — even if the machine dies at any point during it?"*

This property has a name: **Atomicity**. It's the "A" in ACID (Atomic, Consistent, Isolated, Durable) — the four properties that define what a real database must guarantee.

The first attempts to solve this were... creative. And mostly wrong.

---

## Chapter 2: The First Attempts — Shadow Paging

The earliest serious attempt at solving this was called **Shadow Paging**, used in IBM's System R in the 1970s — one of the first relational database prototypes.

The idea was clever:

> *"Never modify data in place. Instead, write to a shadow copy. Only swap in the new copy when you're completely done."*

Here's how it worked. Your database file is divided into fixed-size **pages** (think of them as pages in a book — maybe 4KB or 8KB each).

```
Database Pages on Disk:
┌──────────┐ ┌──────────┐ ┌──────────┐
│  Page 1  │ │  Page 2  │ │  Page 3  │
│  Alice   │ │  Bob     │ │  orders  │
│  $1,000  │ │  $500    │ │  ...     │
└──────────┘ └──────────┘ └──────────┘
       ↑
  Page Table (a map of which pages are "live")
```

When you want to update Alice's balance, you don't touch Page 1. Instead:

**Step 1** — Copy Page 1 to a new location. Call it Page 1-Shadow:
```
┌──────────┐         ┌──────────────┐
│  Page 1  │         │ Page 1-Shadow│
│  Alice   │ ──copy──│  Alice       │
│  $1,000  │         │  $800        │ ← updated here
└──────────┘         └──────────────┘
```

**Step 2** — Modify the shadow copy with the new balance.

**Step 3** — Update Bob's page the same way.

**Step 4** — Only after ALL changes are safely on disk in shadow pages — do one final atomic operation: **flip the page table** to point to the new pages.

```
Page Table flips atomically:
  Page 1 pointer → now points to Page 1-Shadow ✓
  Page 2 pointer → now points to Page 2-Shadow ✓
```

If the system crashes before Step 4, the page table still points to the old pages. Old data is intact. Nothing is lost. The transaction never happened — which is exactly what you want.

If the system crashes after Step 4, the new data is live. Transaction is committed. Also correct.

This is called an **atomic pointer swap** — one single operation that's either done or not done, with nothing in between.

---

### Shadow Paging's Dirty Secret

Shadow paging worked. It was provably correct. And it had a performance problem so severe it was eventually abandoned for almost all use cases.

**Problem 1: Write Amplification**

When you modify one row in a page, you copy the entire page (4KB or 8KB) to a new location. But that page is part of a tree structure (a B-Tree — we'll get to this). Changing a leaf page means you have to create a shadow of its parent page too, and its parent's parent, all the way to the root.

Changing one row → copying 4-6 pages minimum. Every single write.

**Problem 2: Fragmentation**

The old shadow pages become garbage. They're scattered across disk in random locations. You need a garbage collector to clean them up. Meanwhile your data is no longer stored sequentially on disk — it's fragmented everywhere.

This matters enormously because disks (especially the spinning magnetic disks of the 1970s-2000s) are fastest when reading sequentially. Random access — jumping around to different locations — is orders of magnitude slower. Shadow paging turned every database into a random-access nightmare.

**Problem 3: Commit is Slow**

The final page table flip has to be atomic. On the hardware of the era, guaranteeing that a multi-word update to the page table itself was atomic required complex locking and careful hardware interaction.

Engineers looked at shadow paging and said: *"There has to be a better way."*

There was. But it required a completely different mental model.

---

## Chapter 3: The Insight That Changed Everything — "What If We Wrote Our Intentions First?"

Somewhere in the late 1970s, engineers started asking a different question. Instead of:

> *"How do we make writes to the actual data safe?"*

They asked:

> *"What if, before we touch the actual data at all, we wrote down exactly what we're going to do — in a simple, append-only record — and made sure THAT was safely on disk first?"*

The idea is almost psychological. Imagine you're a surgeon about to perform a complex operation. Before you start, you write in a logbook: *"10:00am: beginning procedure on patient Alice. Steps: 1, 2, 3, 4."* After each step, you log it. If you get interrupted in the middle, anyone who reads your logbook knows exactly what state things were left in and what needs to be completed or undone.

This is the **Write-Ahead Log** — WAL. The fundamental rule is:

> **Before you change any data on disk, you must first write what you're about to do to the log. The log entry must be safely on disk before the data change happens.**

"Write-Ahead" literally means: *write to the log first, ahead of writing to actual data.*

Let's see exactly what this looks like.

---

## Chapter 4: WAL in Detail — Walking Through Every Step

Let's go back to Alice sending $200 to Bob.

Your database has two structures:

```
1. The WAL (log file)      — append-only, lives on disk
2. The Data Pages          — actual table data, lives on disk
3. The Buffer Pool         — pages currently in memory (RAM)
```

The **buffer pool** is critical and often misunderstood. Databases don't read and write directly to disk for every operation — disk is too slow. Instead, they load pages into RAM, work on them there, and flush them to disk later. This is why crashes are dangerous — RAM is volatile. Power goes out, RAM is gone.

---

### The Transaction Begins

Alice initiates the transfer. The database starts a transaction and assigns it an ID: **Transaction #42**.

```
Active transactions: {TXN-42: in progress}
```

---

### Step 1: Read the data into the Buffer Pool

The database loads Alice's page and Bob's page from disk into RAM:

```
Buffer Pool (RAM):
┌─────────────────────────────────────┐
│ Page 7: Alice │ balance: $1,000     │  ← "clean" (matches disk)
│ Page 12: Bob  │ balance: $500       │  ← "clean" (matches disk)
└─────────────────────────────────────┘
```

---

### Step 2: Write to the WAL FIRST — Before touching data pages

Before changing anything in the buffer pool, the database writes a log record:

```
WAL on disk:
┌──────────────────────────────────────────────────────────────────┐
│ LSN 1001 | TXN-42 | UPDATE accounts | alice_id=001 | old=$1,000 │
│                                                    | new=$800    │
├──────────────────────────────────────────────────────────────────┤
│ LSN 1002 | TXN-42 | UPDATE accounts | bob_id=002   | old=$500   │
│                                                    | new=$700    │
└──────────────────────────────────────────────────────────────────┘
```

Every log record has a **LSN — Log Sequence Number**. This is a monotonically increasing number. Every record written to the WAL gets the next LSN. This ordering is critical — it tells you exactly what happened in what order.

Each log record contains:
- Which transaction made this change
- Which page was modified
- The **old value** (called the "before image" — needed to undo)
- The **new value** (called the "after image" — needed to redo)

This log is written sequentially — just appending to the end of a file. Sequential writes are the fastest possible disk operation. This is a crucial performance advantage over shadow paging's random writes.

---

### Step 3: The Critical fsync

Writing to a file doesn't mean it's on disk. Operating systems buffer writes in their own memory (the **OS page cache**). If the process crashes, the OS can flush the buffer to disk. But if the entire machine loses power, the OS buffer is gone too.

To guarantee data is truly on disk, the database calls **fsync()** — a system call that forces the OS to flush all buffered writes for that file to the physical storage medium.

```
database calls fsync() on WAL file
         │
         ▼
OS flushes WAL buffer → physical disk platters
         │
         ▼
Disk controller confirms write
         │
         ▼
fsync() returns ← only NOW does the database proceed
```

This is the moment of truth. Once fsync() returns successfully, the log record is guaranteed to survive a power failure. The machine could die a microsecond later and you'd still be able to recover.

Only after this point does the database proceed to modify the actual data.

---

### Step 4: Modify the Buffer Pool

NOW — and only now — the database modifies the pages in RAM:

```
Buffer Pool (RAM):
┌─────────────────────────────────────┐
│ Page 7:  Alice │ balance: $800      │  ← "dirty" (different from disk)
│ Page 12: Bob   │ balance: $700      │  ← "dirty" (different from disk)
└─────────────────────────────────────┘
```

These pages are now called **dirty pages** — they've been modified in memory but not yet written to disk. The data on disk still shows Alice: $1,000 and Bob: $500.

This is fine. The WAL has the truth. Disk pages are temporarily stale.

---

### Step 5: Write the Commit Record

The transaction is done. Write one final log record:

```
WAL on disk:
┌──────────────────────────────────────────────────────────────────┐
│ LSN 1001 | TXN-42 | UPDATE accounts | alice | old=$1,000 new=$800│
├──────────────────────────────────────────────────────────────────┤
│ LSN 1002 | TXN-42 | UPDATE accounts | bob   | old=$500  new=$700 │
├──────────────────────────────────────────────────────────────────┤
│ LSN 1003 | TXN-42 | COMMIT                                       │ ← this
└──────────────────────────────────────────────────────────────────┘
```

fsync() again. Once the COMMIT record is on disk, **the transaction is durably committed.** Tell the user: "Transfer successful."

---

### Step 6: Eventually Write Dirty Pages to Disk

At some later point — maybe seconds, maybe minutes later — a background process called the **checkpointer** flushes dirty pages from the buffer pool to their actual locations on disk.

```
Checkpointer wakes up:
  Page 7 is dirty → write Alice's $800 to disk ✓
  Page 12 is dirty → write Bob's $700 to disk ✓

Data pages on disk now:
  Alice: $800
  Bob:   $700
```

Once the data pages are on disk, the WAL records for that transaction are no longer needed for recovery. The log can eventually be cleaned up (truncated) to save space.

---

## Chapter 5: The Crash — Seeing WAL Earn Its Pay

Now the most important question: **what happens when the machine crashes?**

The database comes back online and runs **crash recovery**. It reads the WAL from the beginning (or from the last checkpoint — we'll get to this) and asks for each record:

> *"Did this transaction commit? If yes — are its changes in the data pages? If no, apply them (REDO). Did this transaction NOT commit? If it was in progress when we crashed — undo its partial changes."*

This is called **REDO/UNDO recovery**.

---

### Scenario A: Crash After Commit Record, Before Checkpointer Flushed Pages

```
WAL:
  LSN 1001 | TXN-42 | UPDATE alice | new=$800   ✓
  LSN 1002 | TXN-42 | UPDATE bob   | new=$700   ✓
  LSN 1003 | TXN-42 | COMMIT                    ✓  ← committed!

Data pages on disk:
  Alice: $1,000  ← stale! checkpointer hadn't run yet
  Bob:   $500    ← stale!
```

Recovery sees: TXN-42 has a COMMIT record. It was committed. But the data pages don't reflect it. **REDO** — replay the changes:

```
Apply LSN 1001: set Alice = $800  ✓
Apply LSN 1002: set Bob = $700    ✓
```

Data is now consistent. No data loss. The user's "Transfer successful" message was correct.

---

### Scenario B: Crash After Some Log Records, But No Commit Record

```
WAL:
  LSN 1001 | TXN-42 | UPDATE alice | new=$800   ✓
  LSN 1002 | TXN-42 | UPDATE bob   | new=$700   ✓
  [CRASH — commit record never written]

Data pages on disk:
  Alice: $800   ← was flushed mid-transaction (bad luck)
  Bob:   $500   ← not flushed yet
```

Recovery sees: TXN-42 has no COMMIT record. It was in progress when the crash happened. **UNDO** — reverse any changes that made it to disk:

```
Reverse LSN 1001: set Alice back to $1,000  ✓
(Bob's page was never written to disk anyway)
```

Data is back to its pre-transaction state. Clean. Consistent. The user's transfer failed — they'll get an error and need to retry. But no money was lost.

---

### The Deep Insight Here

Notice what WAL gave us:

**The log is the source of truth. The data pages are just a cache.**

This is a profound mental shift. When you write to a WAL-based database, the thing that *makes your data real* is not the data pages — it's the log entry. The data pages are an optimization, a materialized view of the log. If they get corrupted or lost, you can always reconstruct them by replaying the log.

This insight — that a log is the fundamental, authoritative record of state — turns out to be one of the most generative ideas in all of computer science. We'll come back to why this matters beyond just crash recovery.

---

## Chapter 6: The Performance Problem WAL Created — And The Solutions

WAL solved crash recovery brilliantly. But it created a new performance problem that tortured database engineers for years.

**The problem:** Every transaction now requires writing to the WAL *and* calling fsync() before telling the user "success." And fsync() is *slow*.

On a traditional spinning hard disk, an fsync() takes about **5-10 milliseconds** — the time for the disk head to rotate to the right position and the OS to confirm the write. That means your database can only commit **100-200 transactions per second**.

For a small blog, that's fine. For a bank processing thousands of transactions per second? Catastrophic.

Engineers developed several techniques to push through this bottleneck.

---

### Technique 1: Group Commit — "Let's Batch Our fsyncs"

Instead of calling fsync() after every single transaction, what if you let several transactions accumulate in the log buffer and fsync() them all at once?

```
Without group commit:
  TXN-42 writes → fsync() → confirm → 10ms
  TXN-43 writes → fsync() → confirm → 10ms
  TXN-44 writes → fsync() → confirm → 10ms
  Total: 30ms, 3 transactions
  Throughput: 100 TPS

With group commit:
  TXN-42 writes ─┐
  TXN-43 writes ─┼──→ single fsync() → confirm → 10ms
  TXN-44 writes ─┘
  Total: 10ms, 3 transactions
  Throughput: 300 TPS (3x improvement)
```

The trick is timing. You want to accumulate enough transactions to make the batching worthwhile, but not wait so long that individual transactions suffer high latency. PostgreSQL and most modern databases implement this with careful tuning of how long the log buffer waits before flushing.

---

### Technique 2: Asynchronous Commit — "Trust the User to Handle Failures"

Some applications don't actually need durability for every single transaction. A logging system writing millions of events per second doesn't care if it loses the last few milliseconds of logs on a crash.

For these use cases, databases offer **async commit** — write to the WAL buffer in memory, tell the user "success" immediately, and fsync() in the background.

```
Async commit:
  TXN-42 writes to memory buffer → immediately tell user "success"
  [background thread fsyncs periodically]
```

Risk: if the machine crashes between the "success" message and the fsync, that transaction is lost. The user was lied to — told "success" when the data didn't survive.

But for non-critical data, this tradeoff is often worth it. PostgreSQL's `synchronous_commit = off` gives 5-10x higher throughput at the cost of potentially losing the last ~200ms of committed transactions on a crash.

---

### Technique 3: SSDs Changed the Math

Traditional spinning disks were slow for random writes because the physical head had to move. SSDs have no moving parts — random writes are nearly as fast as sequential writes, and fsync() latency dropped from ~10ms to ~0.1ms.

This alone gave WAL-based databases a 50-100x throughput improvement without changing any code. Modern NVMe SSDs push this even further.

This is a lesson that repeats in computer science: **sometimes the hardware catches up to the algorithm**, and problems that seemed fundamental turn out to be incidental to the technology of the era.

---

## Chapter 7: Checkpoints — "The Log Can't Grow Forever"

Here's a problem we glossed over. The WAL is append-only. Every change ever made is recorded there. Left unchecked, it would grow infinitely — filling your disk in hours on a busy system.

Also — if the log is the source of truth for crash recovery, and the log has billions of records going back years, crash recovery would take days. That's not acceptable.

The solution is **checkpointing**.

Periodically, the database does the following:

**Step 1** — Write a "checkpoint begin" record to the WAL.

**Step 2** — Flush ALL dirty pages from the buffer pool to disk. Every modified page gets written to its actual location on disk.

**Step 3** — Write a "checkpoint complete" record to the WAL.

```
WAL:
  [billions of old records]
  LSN 8291 | CHECKPOINT BEGIN
  [all dirty pages flushed to disk...]
  LSN 8292 | CHECKPOINT COMPLETE
  LSN 8293 | TXN-99 | UPDATE ...
  LSN 8294 | TXN-99 | COMMIT
```

Now here's the key: everything before LSN 8291 is **guaranteed to be in the data pages on disk**. The checkpoint proves it. So if a crash happens after LSN 8292, recovery only needs to replay from LSN 8292 onward.

```
Recovery after crash:
  Find last CHECKPOINT COMPLETE record (LSN 8292)
  Only replay WAL from LSN 8292 onward
  Everything before is already in data pages ✓
```

And all WAL records before the checkpoint? They can be **archived or deleted** — they're no longer needed for recovery.

This keeps recovery time bounded (minutes, not hours) and WAL size manageable.

---

### The Checkpoint Performance Problem

Checkpointing sounds easy. It isn't. During a checkpoint, you're flushing potentially gigabytes of dirty pages to disk. This causes a massive spike in disk I/O — which means regular queries suddenly get slower because the disk is busy with checkpoint writes.

Users see this as **periodic latency spikes**. "The database gets slow every 5 minutes!" — a classic complaint that usually points to checkpoint tuning.

Modern databases solve this with **incremental checkpointing** — instead of flushing everything at once in a checkpoint, spread the flushing continuously over time, limiting the I/O rate so regular queries don't suffer. PostgreSQL calls this `checkpoint_completion_target` — a setting that controls how smoothly checkpoint I/O is spread out.

---

## Chapter 8: WAL Is Not Just For Recovery — The Realization That Changed Everything

Here's where the story gets really interesting. And this connects back directly to our replication story.

Engineers were using WAL purely for crash recovery. Then someone noticed something:

> *"The WAL is a complete, ordered record of every change ever made to the database. If we ship the WAL to another machine... that machine could replay it and become an exact copy."*

This is **WAL-based replication** — and it became the dominant form of database replication.

Instead of replication being a separate system with its own sync mechanisms, you just **stream the WAL** to replica nodes.

```
PRIMARY:
  Transaction commits → WAL record written → fsync → success

  [WAL Sender process]
       │
       │ streams WAL records over network
       ▼
REPLICA:
  [WAL Receiver process]
       │
       │ receives WAL records
       ▼
  [WAL Replay process]
       │
       │ applies changes to replica's data pages
       ▼
  Replica is now in sync with primary ✓
```

PostgreSQL calls this **Streaming Replication**. MySQL calls it **Binary Log (binlog) Replication**. The idea is identical — the log that exists for crash recovery becomes the source of truth for replication.

This is elegant for a deep reason: you don't need two systems. Crash recovery and replication are the same mechanism, applied in different contexts.

---

### Logical vs Physical WAL Replication

But here's a subtlety that caused real problems in production.

The WAL records we've been describing are **physical** — they describe specific byte changes to specific pages:

```
Physical WAL record:
"At page 7, offset 342, change bytes 0x7E 0x2F to 0x3A 0x08"
```

This is perfectly precise but completely opaque to anything except the exact same database version on the exact same hardware architecture. You can't send a physical WAL record to a Postgres 14 replica from a Postgres 15 primary — the page formats might be different. You can't send it to a different type of database at all.

This created painful operational constraints. Want to upgrade your PostgreSQL version? You have to take replication down, upgrade, restart — risky and requires downtime.

So databases developed **logical replication** — WAL records that describe changes in terms of rows and SQL operations rather than raw bytes:

```
Logical WAL record:
"Table: accounts, Operation: UPDATE, Row: {id=001, balance: 1000→800}"
```

This is human-readable, version-agnostic, and can be consumed by:
- A replica running a different database version
- A completely different database type (send Postgres changes to MySQL)
- A data warehouse
- An application that wants to react to database changes in real time

---

## Chapter 9: Change Data Capture — WAL Escapes the Database

And this brings us to one of the most powerful modern uses of WAL that most people don't know about: **Change Data Capture (CDC)**.

The idea: your WAL is not just for your database. It's a real-time stream of everything that happened to your data. What if other systems could subscribe to that stream?

```
┌─────────────────────────────────────────────────────────────┐
│                      PostgreSQL                              │
│  [Tables]  ──writes──▶  [WAL]  ──CDC──▶  [Logical Decoder] │
└──────────────────────────────────────────────────────────────┘
                                                │
                         ┌──────────────────────┼─────────────────────┐
                         ▼                      ▼                     ▼
                  [ Elasticsearch ]      [ Kafka ]           [ Redis Cache ]
                  "index new orders      "stream all         "invalidate cache
                   for search"            changes for         when user data
                                          event processing"   changes"
```

Tools like **Debezium** sit on top of the database WAL and turn it into a Kafka stream. Every INSERT, UPDATE, DELETE becomes an event that any downstream system can consume.

This solves a problem that previously required ugly workarounds:

**Old way:** Application writes to database, then manually sends events to Elasticsearch, updates Redis cache, publishes to Kafka. If any of these fail, your systems are out of sync. This is called the **dual-write problem**.

**CDC way:** Application writes ONLY to the database. CDC reads the WAL and propagates changes to all downstream systems automatically. The database is the single source of truth. Everything else follows.

```
Old way (dual write):
  App ──▶ Database  (write 1)
  App ──▶ Kafka     (write 2)  ← what if this fails after write 1?
  App ──▶ Redis     (write 3)  ← what if this fails?
  Result: systems get out of sync

CDC way:
  App ──▶ Database only
            │
            └──▶ WAL ──▶ CDC ──▶ Kafka  (guaranteed, in order)
                              └──▶ Redis (guaranteed, in order)
  Result: everything in sync, always
```

---

## Chapter 10: The Philosophical Conclusion — The Log Is Everything

In 2013, Jay Kreps (one of the creators of Kafka) wrote an essay called **"The Log: What every software engineer should know about real-time data's unifying abstraction."**

His argument was sweeping:

> *The humble append-only log — the thing databases invented just to survive power failures — is actually the central abstraction of distributed systems.*

Think about it:

- **WAL** — a log of database changes
- **Kafka** — a distributed log of events
- **Git** — a log of code changes
- **Blockchain** — a log of transactions
- **Raft/Paxos** — algorithms for agreeing on the contents of a distributed log
- **Event Sourcing** — an application architecture where the log of events IS the database

All of them are the same idea: **an ordered, append-only sequence of records that serves as the authoritative source of truth, from which current state can be derived at any time.**

The database engineers of the 1970s who invented WAL just to not lose data on a power failure had accidentally discovered one of the most fundamental primitives in computing.

---

## The Full Story, End to End

```
Problem                        → Solution              → Insight Unlocked
──────────────────────────────────────────────────────────────────────────
Power failure corrupts writes  → Shadow Paging         → Atomic pointer swaps
                                                          work but are slow

Shadow paging too slow,        → Write-Ahead Log       → Log first, data pages
too much write amplification                              are just a cache

WAL fsync bottleneck           → Group Commit,         → Batch I/O, async for
limits throughput                Async Commit, SSDs      non-critical data

WAL grows forever,             → Checkpointing         → Bound recovery time,
recovery takes too long                                   truncate old log

WAL is a complete              → WAL-based             → Crash recovery and
change history...                Replication             replication are the
                                                          same mechanism

Physical WAL is fragile,       → Logical Replication   → Database changes as
version-dependent                                         row-level events

Downstream systems out of      → Change Data Capture   → WAL as a real-time
sync with database              (Debezium, etc.)         event stream

The log as a pattern           → Kafka, Event          → The append-only log
repeats everywhere               Sourcing, Git,          is the universal
                                 Blockchain              abstraction
```

---

**Ready to go deeper?** We can explore any thread from this story:

- **B-Trees and how data pages are actually structured** — what those pages the WAL is protecting actually look like inside
- **MVCC (Multi-Version Concurrency Control)** — how databases let thousands of transactions read and write simultaneously without locking each other out
- **Isolation levels** — what "read committed" and "serializable" actually mean at the WAL level
- **Kafka's architecture** — WAL as a first-class distributed system, not just a database implementation detail

What pulls you in?
