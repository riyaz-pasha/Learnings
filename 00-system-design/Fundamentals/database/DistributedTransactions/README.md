## The Story of Distributed Transactions

Let's start with why this problem exists at all. Because if you understand the *why* deeply enough, every solution we cover will feel inevitable rather than arbitrary.

---

## Chapter 1: The Moment Single-Machine Transactions Weren't Enough

It's 2004. You're an engineer at a bank. Your database is a single PostgreSQL instance. A user transfers $500 from checking to savings:

```sql
BEGIN;
UPDATE accounts SET balance = balance - 500 WHERE id = 'checking_alice';
UPDATE accounts SET balance = balance + 500 WHERE id = 'savings_alice';
COMMIT;
```

Both accounts are on the same machine. PostgreSQL's transaction engine handles this atomically. Either both happen or neither does. You sleep well.

Then your bank grows. 50 million customers. 10 billion dollars in daily transactions. One machine can't hold all the accounts. You shard — checking accounts on Shard 1, savings accounts on Shard 2, loans on Shard 3.

Now Alice's transfer looks like this:

```
Shard 1 (checking): UPDATE accounts SET balance = balance - 500 WHERE id = 'checking_alice';
Shard 2 (savings):  UPDATE accounts SET balance = balance + 500 WHERE id = 'savings_alice';
```

These are two separate machines. Two separate database engines. Two separate transaction systems that know absolutely nothing about each other.

The question that will consume the next 40 years of distributed systems research:

> *"How do you make two independent machines either BOTH commit or BOTH abort — with no coordinator they can trust, on an unreliable network, where any machine can die at any moment?"*

This is the **distributed transaction problem.** And it's genuinely hard — not "hard to engineer" hard, but "there are fundamental theoretical limits on what's possible" hard.

Let's discover those limits the way engineers did — by trying the obvious thing first and watching it fail.

---

## Chapter 2: The Obvious First Attempt — Just Send Both Writes

The most naive approach: send both writes, hope for the best.

```
Coordinator:
  → Send to Shard 1: "Subtract $500 from checking_alice"
  → Send to Shard 2: "Add $500 to savings_alice"
  → Done!
```

What could go wrong?

**Scenario A:** Shard 1 succeeds, network drops, Shard 2 never receives its message.
```
Result: Alice loses $500 into void. Bank lawsuit incoming.
```

**Scenario B:** Both writes succeed, but Shard 2's confirmation is lost.
```
Coordinator: "Did Shard 2 get it? I'm not sure..."
Result: Coordinator doesn't know if it should retry (double credit?) or not.
```

**Scenario C:** Coordinator crashes after sending to Shard 1 but before sending to Shard 2.
```
Result: $500 gone. Nobody left to send the second message.
```

Every scenario is a disaster. The core problem is:

> **On an unreliable network, you can never be certain that a message you sent was received, processed, and committed — unless you get an explicit acknowledgment. And acknowledgments can also be lost.**

This is related to a famous theoretical result called the **Two Generals Problem** — two armies trying to coordinate an attack by sending messengers through enemy territory (where messengers can be captured). It's mathematically proven that they can never achieve guaranteed coordination through an unreliable channel.

The distributed transaction problem is the database version of Two Generals. And the field's response was: *"We can't achieve perfect guarantees — but we can get arbitrarily close if we're careful about what each machine records before taking action."*

This insight led to the first real protocol.

---

## Chapter 3: Two-Phase Commit — The Classic Solution

Two-Phase Commit (2PC) was developed in the late 1970s, formalized by Jim Gray (one of the most important figures in database history) in his 1978 paper "Notes on Data Base Operating Systems."

The key insight is beautifully simple:

> *"Before anyone commits, everyone must first PROMISE they can commit. Only after everyone has promised does anyone actually commit."*

The protocol has two phases (obviously):

---

### Phase 1 — The Prepare Phase (Voting)

The coordinator sends a "prepare" message to all participants. This is not "commit." It's "can you commit?"

```
Coordinator → Shard 1: "PREPARE to commit: subtract $500 from checking_alice"
Coordinator → Shard 2: "PREPARE to commit: add $500 to savings_alice"
```

Each participant:
1. Acquires all locks needed for its part of the transaction
2. Writes a PREPARE record to its own WAL — durably, to disk
3. Responds YES or NO

```
Shard 1:
  Acquires exclusive lock on checking_alice row ✓
  Writes to WAL: "PREPARED for TXN-789, subtract $500" ✓
  → Coordinator: "YES, I can commit"

Shard 2:
  Acquires exclusive lock on savings_alice row ✓
  Writes to WAL: "PREPARED for TXN-789, add $500" ✓
  → Coordinator: "YES, I can commit"
```

The PREPARE WAL record is the critical step. Once a participant writes PREPARE to disk and votes YES, it has made an **irrevocable promise** — it will commit this transaction if the coordinator says COMMIT, no matter what. Even if it crashes and restarts, it will find the PREPARE record in its WAL and honor the promise.

This is what makes 2PC work: the promise is durable.

---

### Phase 2 — The Commit Phase (Decision)

The coordinator collects votes. If ALL participants voted YES, send COMMIT. If ANY participant voted NO (or timed out), send ABORT.

```
All votes: YES
Coordinator writes to its own WAL: "COMMIT TXN-789" ✓ (durable!)
Coordinator → Shard 1: "COMMIT"
Coordinator → Shard 2: "COMMIT"
```

Each participant commits, releases locks, confirms:

```
Shard 1:
  Applies transaction: checking_alice.balance -= 500 ✓
  Releases lock on checking_alice ✓
  Writes to WAL: "COMMITTED TXN-789" ✓
  → Coordinator: "Done"

Shard 2:
  Applies transaction: savings_alice.balance += 500 ✓
  Releases lock on savings_alice ✓
  Writes to WAL: "COMMITTED TXN-789" ✓
  → Coordinator: "Done"
```

Alice's transfer is complete. $500 atomically moved from checking to savings across two separate machines.

---

### Why 2PC Is Correct — The Proof

Let's verify it handles every failure scenario:

**Failure 1: Participant crashes before voting**
```
Shard 1 crashes during Prepare phase, never votes.
Coordinator times out waiting for Shard 1's vote.
→ Coordinator sends ABORT to all.
→ No money moved anywhere. Consistent. ✓
```

**Failure 2: Participant crashes after voting YES**
```
Shard 1 votes YES, writes PREPARE to WAL, then crashes.
Coordinator still gets Shard 2's YES, but Shard 1 is silent.
→ Coordinator sends ABORT (can't get all votes).
→ Shard 1 restarts, finds PREPARE in WAL, asks coordinator: "What happened?"
→ Coordinator: "ABORT." Shard 1 rolls back. ✓
```

**Failure 3: Coordinator crashes after writing COMMIT to its WAL but before sending to participants**
```
Coordinator crashes. Participants are in PREPARED state, holding locks.
Coordinator restarts, finds "COMMIT TXN-789" in its WAL.
→ Resends COMMIT to all participants.
→ Participants commit. ✓
```

**Failure 4: Network partition — commit sent to Shard 1, not Shard 2**
```
Coordinator sends COMMIT to Shard 1 (succeeds), COMMIT to Shard 2 (network drops).
Shard 1 commits. Shard 2 still in PREPARED state.
Coordinator retries sending COMMIT to Shard 2. Eventually succeeds.
→ Both commit. ✓
```

2PC handles every failure — **as long as the coordinator eventually recovers.**

---

### The Fatal Flaw — The Blocking Problem

Here's the scenario 2PC cannot handle:

```
Shard 1 votes YES. Writes PREPARE to WAL. Releases nothing.
Shard 2 votes YES. Writes PREPARE to WAL. Releases nothing.
Coordinator has received both YES votes.
Coordinator writes COMMIT to its WAL.
Coordinator crashes.  ← RIGHT HERE
```

Both shards are now in the **PREPARED state** — they've voted YES, they're holding their locks, and they're waiting for the coordinator's decision. But the coordinator is dead.

What do the shards do?

- They can't commit on their own — maybe the coordinator actually aborted (what if it wrote ABORT to WAL before the crash? They don't know).
- They can't abort on their own — maybe the coordinator wrote COMMIT and other shards have already committed. If they abort, money disappears.
- They can't contact each other — they don't know who else is involved in this transaction.

They are **stuck.** Holding locks. Waiting. For a coordinator that might never come back.

```
checking_alice row: LOCKED 🔒 (waiting for coordinator)
savings_alice row: LOCKED 🔒 (waiting for coordinator)

Any other transaction trying to read Alice's balance: BLOCKED
Any attempt to send Alice money: BLOCKED
Any attempt for Alice to transfer money: BLOCKED
```

This is called the **blocking problem** — 2PC can block indefinitely if the coordinator fails at exactly the wrong moment. And "indefinitely" in production means: until an operator manually intervenes. Which could be hours.

In a banking system handling millions of transactions per second, having rows locked indefinitely because a coordinator machine crashed is a severity-1 outage.

Worse: the more participants you have, the more likely at least one coordinator failure occurs. 2PC doesn't scale well — the probability of blocking increases with the number of nodes and transactions.

Engineers knew this and tried to fix it. They invented **Three-Phase Commit (3PC)** — adding a phase between Prepare and Commit to allow participants to make progress even if the coordinator dies. But 3PC assumes a synchronous network — if message delivery can be arbitrarily delayed (which it can, in real networks), 3PC still blocks. It solved a theoretical problem while leaving the practical problem untouched.

The field was stuck with 2PC for decades — using it, suffering its blocking problem, and working around it operationally. Until Google had a problem so large they had to invent something better.

---

## Chapter 4: Google's Percolator — Rethinking Distributed Transactions From Scratch

In 2010, Google published a paper titled **"Large-scale Incremental Processing Using Distributed Transactions and Notifications."** The system was called **Percolator**, built to incrementally update the Google Search index.

The problem: Google's web crawlers constantly find new pages and updates to existing pages. The index needed to be updated incrementally — small changes to a distributed key-value store (Bigtable) that could affect millions of interconnected documents. These updates needed to be transactional.

The scale: thousands of machines, petabytes of data, millions of transactions per second. Classic 2PC couldn't handle it — too much coordinator bottleneck, too much blocking risk.

Google's insight was radical:

> *"What if there is no coordinator machine? What if the coordination data lives IN the database itself, as regular key-value entries?"*

This is Percolator's core idea. Instead of a separate coordinator process that can crash and cause blocking, coordination state is stored as data — durably, in the same distributed database the data lives in. Any machine can be a "coordinator" because all the coordination state is accessible to any machine.

Let's walk through Percolator step by step, because this is the foundation CockroachDB is built on.

---

### Percolator's Data Model — Three Columns Per Cell

Percolator runs on top of Bigtable, which stores data as a multi-dimensional map: `(row, column, timestamp) → value`.

For each piece of user data, Percolator maintains **three special columns**:

```
For a cell with key K:

K:data     → the actual value, stored at various timestamps
K:lock     → indicates if this cell has an uncommitted write (the write intent)
K:write    → a pointer from a committed timestamp to the data timestamp
```

Let's make this concrete with Alice's checking account:

```
"checking_alice":data   at T=10  →  {balance: 1000}  ← committed value
"checking_alice":write  at T=10  →  "data@T=10"      ← proof it's committed
"checking_alice":lock   at T=10  →  (empty)          ← no ongoing transaction
```

The `:write` column is the commit record. If `:write` exists at timestamp T, the corresponding `:data` at that timestamp is committed and visible. If `:data` exists but `:write` doesn't, the write is uncommitted — a write intent.

This is a physical implementation of MVCC, stored directly in the key-value store.

---

### Percolator's Transaction — Step by Step

Let's transfer $500 from Alice's checking to savings. Alice's checking is at key K1, savings at K2.

**Timestamp Oracle — Getting a Start Timestamp**

Every Percolator transaction starts by getting a timestamp from a **Timestamp Oracle** — a single server that hands out monotonically increasing timestamps. This is a lightweight service, not a bottleneck for data — it just hands out numbers.

```
Transaction begins:
  → Timestamp Oracle: "Give me a start timestamp"
  ← start_ts = 15
```

Our transaction sees the database as of T=15. It will read the latest committed version ≤ T=15 for each key.

---

**Phase 1 — Reads**

Read checking_alice's balance as of T=15:

```
Scan "checking_alice":write for timestamps ≤ 15
  → finds "data@T=10" at T=10
  → Read "checking_alice":data at T=10
  → balance = $1000
```

Read savings_alice's balance similarly. Suppose it's $500.

---

**Phase 2 — Prewrite (Percolator's equivalent of Prepare)**

The client picks one key to be the **primary lock** — the anchor of the entire transaction. This choice is arbitrary but must be consistent. Let's say K1 (checking) is primary.

For EACH key being written, the client does a **Prewrite**:

**Prewrite K1 (checking_alice) — the primary:**

1. Check for conflicts: Is there any lock on K1 (from another concurrent transaction)? Is there any write to K1 at timestamp > start_ts (meaning someone committed after we started)?

```
Check "checking_alice":lock → empty ✓ (no concurrent lock)
Check "checking_alice":write for timestamps > 15 → none ✓ (no newer commit)
```

2. Write the data and the lock:

```
Write "checking_alice":data  at T=15  →  {balance: 500}     ← new value
Write "checking_alice":lock  at T=15  →  {primary: K1, start_ts: 15}  ← primary lock marker
```

**Prewrite K2 (savings_alice) — a secondary:**

Same conflict check. Then:

```
Write "savings_alice":data  at T=15  →  {balance: 1000}    ← new value  
Write "savings_alice":lock  at T=15  →  {primary: K1, start_ts: 15}  ← points to PRIMARY
```

Notice: **K2's lock points to K1 (the primary).** This is the critical design. Every secondary lock knows where the primary is. The primary lock is the **single source of truth** for whether this transaction committed or aborted.

---

**Get Commit Timestamp**

After all prewrites succeed, get a commit timestamp from the oracle:

```
→ Timestamp Oracle: "Give me a commit timestamp"
← commit_ts = 20
```

commit_ts must be > start_ts. This is guaranteed by the oracle.

---

**Phase 3 — Commit (the point of no return)**

**Commit the PRIMARY only:**

1. Write the `:write` column for K1, pointing to the data:

```
Write "checking_alice":write at T=20  →  {data_ts: 15}   ← "committed at T=20, data at T=15"
```

2. Delete K1's lock:

```
Delete "checking_alice":lock at T=15
```

**THIS IS THE COMMIT POINT.** The moment the primary's `:write` column is written and `:lock` is deleted, the transaction is committed. This is a single key-value write — atomic in Bigtable/Paxos. Either it happened or it didn't. No ambiguity.

This is the genius of Percolator: **the commit decision is reduced to a single atomic write on a single key.** No coordinator machine needs to be alive. The data itself records the commit.

---

**Phase 4 — Cleanup (asynchronous)**

After the primary is committed, the secondary locks need to be cleaned up. But this doesn't need to happen immediately or atomically:

```
Write "savings_alice":write at T=20  →  {data_ts: 15}    ← committed
Delete "savings_alice":lock at T=15
```

If the client crashes after committing the primary but before cleaning secondaries — no problem. The secondary locks still exist, but any transaction that encounters them can look at the primary lock's status:

```
Transaction C reads savings_alice, finds lock pointing to "checking_alice":
  → Check "checking_alice" for `:write` at T=20
  → Found! Primary is committed.
  → Clean up the secondary lock ourselves (help the crashed transaction)
  → Proceed to read the committed value
```

Any transaction that finds a stale secondary lock becomes a **helper** — it cleans up the secondary lock on behalf of the crashed transaction. This is called **lazy cleanup** and it's what eliminates the blocking problem of 2PC.

---

### Why Percolator Eliminates Blocking

In 2PC, blocking happens because participants voted YES but the coordinator crashed — they can't determine the outcome without the coordinator.

In Percolator, the commit outcome is determined by the **primary lock's state in the database** — which is replicated (via Paxos/Raft) and always accessible. The coordinator can crash at any point:

```
Crash BEFORE prewrite: nothing was written. Transaction never happened. ✓
Crash AFTER prewrite, BEFORE primary commit: 
  Locks exist. Any reader that encounters them checks primary.
  Primary has no :write record → transaction aborted.
  Reader cleans up locks and proceeds. ✓

Crash AFTER primary commit, BEFORE secondary cleanup:
  Primary has :write record → transaction committed.
  Reader encountering secondary lock sees primary committed.
  Reader cleans up and returns committed value. ✓
```

In every case, the outcome is **deterministic from the database state** — no need for a live coordinator to answer questions. Any machine can determine the transaction's outcome by reading the primary lock.

This is the fundamental insight Percolator contributed:

> *"Move coordination state from a coordinator process into the database itself. Make the commit decision a single atomic database write. Let cleanup be lazy and distributed."*

---

## Chapter 5: CockroachDB — Percolator on Raft

CockroachDB's distributed transaction protocol is directly inspired by Percolator, adapted for its own storage model (RocksDB/Pebble) and consensus layer (Raft).

Let's walk through CockroachDB's implementation. There are important differences from Percolator that reveal deeper engineering tradeoffs.

---

### Transaction Records — CockroachDB's Coordinator State

Instead of using the primary lock as the commit indicator (Percolator's approach), CockroachDB stores a dedicated **transaction record** — a special key in the database that represents the transaction's state.

```
Transaction Record for TXN-abc123:
  key:    /txn/abc123
  state:  PENDING | COMMITTED | ABORTED
  timestamp: commit_ts
  heartbeat: last_heartbeat_time
```

Every write intent (uncommitted write) includes a pointer to its transaction record:

```
Write intent for checking_alice:
  value: {balance: 500}
  txn_id: abc123    ← points to /txn/abc123
  ts: T=15
```

When a transaction commits, it:
1. Writes COMMITTED status to its transaction record (single Raft-consensus write)
2. Asynchronously cleans up write intents

When any reader encounters a write intent:
1. Look up the transaction record at `/txn/{txn_id}`
2. If COMMITTED → treat intent as committed value
3. If ABORTED → treat intent as non-existent, clean it up
4. If PENDING → check heartbeat time. If recent → wait. If stale → push or abort.

---

### The Heartbeat Mechanism — Detecting Dead Coordinators

In Percolator, if a transaction crashes after prewrites but before commit, the coordinator is just... gone. There's no timeout mechanism built into the protocol — readers who encounter stale locks must decide whether to wait or abort.

CockroachDB adds **heartbeats** to transaction records. The transaction coordinator (the gateway node handling the client connection) continuously updates the transaction record's heartbeat timestamp while the transaction is active:

```
Every 2 seconds:
  Coordinator writes: /txn/abc123.heartbeat = current_time
```

If a reader encounters a write intent from TXN-abc123 and the transaction is PENDING:

```
Check /txn/abc123:
  state = PENDING
  heartbeat = 5 minutes ago  ← too old!
  → The coordinator is dead.
  → Abort this transaction.
  → Write ABORTED to /txn/abc123.
  → Clean up write intents.
  → Proceed.
```

This is crucial: it bounds the time any reader waits due to a dead coordinator. No more indefinite blocking. The heartbeat timeout (configurable, default ~10 seconds) is the maximum wait.

---

### Write Conflicts in CockroachDB — The Timestamp Dance

Here's where CockroachDB's MVCC timestamp approach shines. Consider two concurrent transactions trying to write the same key:

```
TXN-A: start_ts=100, trying to write checking_alice
TXN-B: start_ts=110, also trying to write checking_alice
```

TXN-A prewrites first:

```
checking_alice: INTENT {balance: 500, txn_id: A, ts: 100}
```

TXN-B tries to prewrite checking_alice, encounters TXN-A's intent.

In 2PC, TXN-B would block waiting for TXN-A. CockroachDB has a more nuanced approach — it tries to resolve conflicts without blocking using the **timestamp ordering** approach:

**Option 1 — TXN-B has a higher timestamp than TXN-A:**

```
TXN-A intent at ts=100.
TXN-B starts at ts=110.
TXN-B can simply write its intent at ts=110.
No conflict — they're at different timestamps!
The committed version ordering will resolve which one "wins."
```

Wait — can two versions of the same key coexist? Yes, in MVCC! They're at different timestamps. When a reader comes along, they see the version appropriate for their snapshot timestamp.

**Option 2 — Write-write conflict at same timestamp (rare):**

CockroachDB uses a **priority system**. Higher-priority transactions can **push** lower-priority transactions — advance the lower-priority transaction's timestamp to clear the way:

```
TXN-B wants to write at ts=110, but TXN-A has an intent at ts=100.
TXN-B "pushes" TXN-A: "Move your intent to ts=111 or higher."
TXN-A's coordinator receives the push.
TXN-A can either: accept the new timestamp (restart with ts=111)
                 or: abort if it can't move forward.
```

This timestamp-pushing mechanism prevents most blocking scenarios. Instead of waiting, transactions negotiate timestamps and reorganize in MVCC order.

---

### Read Refreshes — Maintaining Snapshot Integrity

Remember from the MVCC chapter: CockroachDB transactions have a start timestamp and read all data as of that timestamp. But what if, during a long transaction, data you READ early on has since been overwritten by a committed transaction?

```
TXN-A starts at ts=100.
TXN-A reads checking_alice → $1000 (committed at ts=50)
TXN-A does some work...
TXN-B commits at ts=105, writes checking_alice → $900

TXN-A tries to commit at ts=110.
TXN-A wants to be serializable — did its read at ts=100 reflect reality?
checking_alice changed between ts=100 and ts=110!
```

CockroachDB must validate: **are all keys I read still at the same version as when I read them?**

This is called a **read refresh**. Before committing, CockroachDB checks every key in the transaction's read set:

```
For each key K in read_set:
  Is there a version of K committed between my_start_ts and my_commit_ts?
  → If NO: read is still valid ✓
  → If YES: read is stale, need to refresh
```

If a read is stale, CockroachDB tries to advance the transaction's read timestamp to the commit timestamp and validate that the newer version is still consistent with the transaction's logic. If it can't be validated cleanly, the transaction aborts and retries.

This refresh mechanism is what enables true serializable isolation — detecting read-write conflicts that would constitute write skew.

---

### The Full CockroachDB Transaction Flow

Let's put it all together for Alice's transfer:

```
T=0: Client connects to CockroachDB gateway node (node 3).
     Node 3 becomes the transaction coordinator.
     
T=1: Get start timestamp from HLC: start_ts = (T=1704067200.000, L=0)
     Create transaction record:
       /txn/abc123: {state: PENDING, ts: start_ts, heartbeat: now}
     
T=2: Read checking_alice from Shard 1 (via Raft).
     See version at ts=..., balance=$1000.
     Add checking_alice to read_set.
     
T=3: Read savings_alice from Shard 2 (via Raft).
     See version at ts=..., balance=$500.
     Add savings_alice to read_set.
     
T=4: Write intent on Shard 1:
       checking_alice: INTENT {balance=500, txn_id=abc123, ts=start_ts}
     Write intent on Shard 2:
       savings_alice: INTENT {balance=1000, txn_id=abc123, ts=start_ts}
     (both writes go through Raft for their respective shards)
     
[While waiting, coordinator sends heartbeats every 2s to /txn/abc123]

T=5: Validate read set (read refresh):
       checking_alice: any commits between start_ts and now? No ✓
       savings_alice: any commits between start_ts and now? No ✓
     
T=6: Get commit timestamp from HLC: commit_ts = (T=1704067200.010, L=0)

T=7: COMMIT POINT:
     Update transaction record via Raft:
       /txn/abc123: {state: COMMITTED, commit_ts: commit_ts}
     Tell client: "Transaction committed!" ✓

T=8: ASYNC CLEANUP (can happen lazily):
     Shard 1: Replace intent with committed value at commit_ts.
     Shard 2: Replace intent with committed value at commit_ts.
     Delete transaction record /txn/abc123.
```

The commit happens at T=7 — a single Raft write to the transaction record's shard. Everything after is cleanup.

---

## Chapter 6: Spanner — When You Need Nanosecond Precision Globally

CockroachDB's HLC approach handles uncertainty by sometimes restarting transactions. Google Spanner took a different approach: **eliminate the uncertainty entirely by knowing what time it actually is, globally.**

We touched on this in Chapter 6 of the replication series. Now let's go deep on how TrueTime interacts with distributed transactions.

---

### The Clock Skew Problem — Why It Matters For Transactions

In any distributed system, machines have clocks that drift apart. Typical NTP synchronization keeps clocks within ~100ms of each other. Without special hardware, you fundamentally don't know if your clock is ahead or behind by up to 100ms.

For distributed transactions, this creates a problem:

```
Spanner node in Virginia: clock says T=1000
Spanner node in Tokyo: clock says T=1050  ← 50ms ahead

Transaction committed at Virginia: "committed at T=1000"
Transaction reads at Tokyo: "reading as of T=1020"
  → Should Tokyo see Virginia's T=1000 commit? 
  → Yes! T=1000 < T=1020 logically.
  → But Tokyo's clock said T=1020 when the commit "happened at T=1000" 
     by Virginia's clock. From Tokyo's perspective, the commit happened 
     in the past relative to T=1020.
  → EXCEPT: What if Virginia's clock is actually 50ms behind real time?
     Then "T=1000 Virginia" = "T=1050 real" = "T=1000 Tokyo" actually...
  
This is maddening. Without knowing which clock is right, 
you can't determine the true ordering of events.
```

CockroachDB's HLC solution: track uncertainty, sometimes restart transactions to push past the uncertain window.

Spanner's solution: **measure and bound the uncertainty, then wait it out.**

---

### TrueTime — GPS + Atomic Clocks = Known Uncertainty

Every Google datacenter has **Time Master** servers equipped with GPS receivers and atomic clocks. These are synchronized to real UTC time with known, bounded error — typically within 7 milliseconds (ε, the epsilon).

TrueTime API gives you not a single timestamp but an **interval**:

```c
TrueTime.now() returns {
  earliest:  T - ε,   // real time is definitely after this
  latest:    T + ε,   // real time is definitely before this
}
// Guarantee: real time is somewhere in [T-ε, T+ε]
```

With GPS/atomic clocks, ε is typically 1-7ms. With just NTP, ε could be 100ms+.

This interval is the key. You know real time is definitely within this range. You can use this knowledge to make hard guarantees.

---

### Spanner's Commit Wait — The Price of External Consistency

When Spanner commits a transaction, it picks a commit timestamp `s`. It wants to guarantee:

> *"If any other transaction starts after this one committed, it will receive a start timestamp that is definitely greater than s."*

This is **external consistency** — transactions see reality in the same order they actually happened in real time.

To guarantee this, Spanner uses **commit wait**:

```
1. Transaction is ready to commit.
2. Get TrueTime interval: TT = {earliest: T-ε, latest: T+ε}
3. Pick commit timestamp: s = TT.latest  (the UPPER BOUND of uncertainty)
4. Wait until TrueTime.now().earliest > s
   (i.e., wait until we're CERTAIN real time has passed s)
5. Apply the commit.
```

Why wait? Because another machine reading at the same real-time moment might have its clock slightly behind. If we commit at "s" but a reader's clock says "s-3ms", they might miss our commit. By waiting until TrueTime.earliest > s — meaning EVEN accounting for maximum clock error, real time has definitely passed s — we guarantee any subsequent reader's timestamp is after s.

```
TrueTime: ε = 7ms
Commit at s = T.latest = 1000.007

Wait until TrueTime.now().earliest > 1000.007
  TrueTime keeps updating...
  At real time ~1000.014, TrueTime reports: {earliest: 1000.009, latest: 1000.016}
  1000.009 > 1000.007 → done waiting ✓

Commit applied. Any future transaction gets start_ts > 1000.007.
```

The commit wait is typically **2ε** — twice the clock uncertainty — which at 7ms ε means ~14ms of waiting. Every write, every time.

This seems like a lot. But consider what you get: **true external consistency, globally, across continents.** No transaction restarts. No uncertainty windows. Perfect serializable ordering that matches real time.

---

### Spanner's Distributed Transaction Protocol

Spanner uses Paxos groups for each shard (similar to CockroachDB's Raft). Its transaction protocol is a refined 2PC built on top of these Paxos groups, made non-blocking by:

1. **The participant state is replicated via Paxos** — coordinator crashes don't lose the prepare votes; they're replicated across Paxos replicas.
2. **The coordinator is itself a Paxos group** — if one coordinator replica fails, another takes over instantly.

```
Traditional 2PC:
  Coordinator is single machine → crashes → blocking

Spanner 2PC:
  Coordinator is a Paxos group (replicated across 5 machines)
  → one coordinator machine dies → Paxos elects new leader instantly
  → transaction proceeds without blocking
```

This is the key insight: replicate the coordinator, and blocking disappears. 2PC's blocking problem fundamentally stems from the coordinator being a single point of failure. Make the coordinator itself highly available via consensus, and the problem dissolves.

```
Spanner Transaction:

1. Transaction coordinator is chosen (a Paxos group).
2. Reads happen across relevant Paxos groups with TrueTime snapshot.
3. Prepare: coordinator sends PREPARE to all participant Paxos groups.
   Each participant Paxos group runs consensus to durably record PREPARE.
   → Even if coordinator dies, all prepares are durably committed via Paxos.
4. Coordinator receives all YES votes.
5. Coordinator Paxos group runs consensus to record COMMIT decision.
6. Commit wait (2ε).
7. Coordinator sends COMMIT to participants.
8. Each participant Paxos group applies commit.
```

If the coordinator dies between steps 4 and 5, a new Paxos leader is elected with all the prepare votes visible (replicated). Transaction continues. **Zero blocking.**

---

### The Spanner vs CockroachDB Tradeoff

Both solve distributed transactions correctly. Both achieve serializable isolation. But differently:

```
                    │ Spanner              │ CockroachDB
────────────────────┼──────────────────────┼──────────────────────
Clock mechanism     │ TrueTime (GPS +      │ HLC (software, no
                    │ atomic clocks)        │ special hardware)
                    │                      │
Write latency       │ 2ε (~14ms) always   │ Occasional restarts,
                    │                      │ but often faster
                    │                      │
Cross-region write  │ ~100ms (commit       │ ~100ms (Raft round
latency             │ wait + Paxos)        │ trip + HLC)
                    │                      │
Uncertainty handling│ Wait out the         │ Restart transactions
                    │ uncertainty          │ that hit uncertainty
                    │                      │
Hardware req        │ GPS + atomic clocks  │ None beyond servers
                    │ in every DC          │
                    │                      │
Consistency model   │ External consistency │ Serializable
                    │ (stronger)           │
                    │                      │
Operational cost    │ Very high (Google-   │ High (self-host)
                    │ grade infra)         │ Medium (Cloud)
```

Spanner's external consistency is strictly stronger than CockroachDB's serializable. But the difference matters only in very specific cases — most applications can't observe the difference.

---

## Chapter 7: The SAGA Pattern — When You Can't Use Distributed Transactions

Not all problems can be solved with 2PC or Percolator. Sometimes the operations span systems you don't control — third-party APIs, legacy databases, external services. You can't enroll them in a 2PC protocol.

Also: even when you technically could use distributed transactions, sometimes the performance cost is unacceptable. A checkout process that touches an inventory service, a payment processor, a shipping calculator, and a notification service — running that as a single distributed transaction would be agonizingly slow and fragile.

In 1987, Hector Garcia-Molina and Kenneth Salem published a paper proposing **SAGAs** — a pattern for long-running business transactions that can't be atomic in the traditional sense.

The insight:

> *"Instead of making the entire operation atomic, break it into a sequence of smaller transactions, each with a corresponding **compensating transaction** that undoes it."*

---

### The Saga Model — Forward and Backward Paths

A saga for Alice's order looks like this:

```
Forward path (happy path):
  T1: Reserve inventory          → can be compensated by C1: Release inventory
  T2: Charge payment             → can be compensated by C2: Refund payment
  T3: Schedule shipping          → can be compensated by C3: Cancel shipping
  T4: Send confirmation email    → can be compensated by C4: Send cancellation email

If T3 fails:
  Run compensations in reverse: C2 → C1
  (T4 hasn't happened, C3 not needed)
```

Each step is its own local transaction. If any step fails, the saga runs compensation transactions in reverse to undo the completed steps.

This is **eventual atomicity** — the system might be temporarily inconsistent during saga execution (inventory reserved but payment not yet charged), but eventually either all steps complete or all are compensated.

---

### Two Saga Implementations

**Choreography** — each step publishes an event, the next step listens for it:

```
T1 commits → publishes "InventoryReserved" event
T2 listens for "InventoryReserved" → charges payment → publishes "PaymentCharged"
T3 listens for "PaymentCharged" → schedules shipping → publishes "ShippingScheduled"
...

If T2 fails → publishes "PaymentFailed"
T1 listens for "PaymentFailed" → runs C1: releases inventory
```

No central coordinator. Each service knows what to do when it hears certain events. Decentralized and resilient.

**Orchestration** — a central orchestrator service directs each step:

```
Orchestrator:
  → Inventory Service: "Reserve items for order 789"
  ← "Reserved ✓"
  → Payment Service: "Charge $150"
  ← "Charged ✓"
  → Shipping Service: "Schedule delivery"
  ← ERROR: "Out of delivery slots"
  → Payment Service: "Refund $150"  ← compensate
  → Inventory Service: "Release items"  ← compensate
  ← All compensated ✓
  → Client: "Order failed, you've been refunded"
```

Orchestration is easier to reason about and debug. Choreography is more decoupled and scalable.

---

### The Fundamental Limitation of Sagas — No Isolation

Sagas solve the availability and coordination problem. But they explicitly sacrifice isolation:

```
During saga execution:
  T1 committed: inventory reserved (visible to other transactions!)
  T2 not yet committed: payment pending
  
Another user's query: "How much inventory is available?"
  Sees: inventory reserved (even though order might fail and release it)
```

Between saga steps, other transactions can observe **intermediate states** — states that might be compensated away later. This is called **dirty reads at the saga level** (not to be confused with database-level dirty reads).

For some business processes this is fine. For others — where intermediate states could trigger other actions — it's dangerous.

Sagas are not a replacement for distributed transactions. They're a complementary tool for when atomicity is too expensive or impossible, and eventual consistency is acceptable.

---

## Chapter 8: The Theoretical Limits — What's Actually Impossible

After all these solutions, it's worth stepping back and understanding what's fundamentally impossible. Because several results in distributed systems are not engineering limitations — they're mathematical proofs about what cannot be built.

---

### FLP Impossibility — Consensus Is Impossible With One Faulty Process

In 1985, Fischer, Lynch, and Paterson proved something shocking:

> *In an asynchronous distributed system where messages can be delayed arbitrarily, consensus is IMPOSSIBLE if even ONE process can fail — even if it just stops responding.*

This is the **FLP Impossibility** result. It proves that there's no algorithm that can guarantee:
1. All non-faulty processes agree on a value
2. The agreed value was proposed by some process
3. The algorithm always terminates in finite time

... given that any process might fail and you can't tell if it failed or is just slow.

This sounds like it invalidates everything we've built. Raft, Paxos, 2PC — they all solve consensus. Are they wrong?

No — they sidestep FLP by relaxing "always terminates." Raft and Paxos guarantee progress only when a majority of nodes are available. They can block (temporarily) if too many nodes fail. FLP says this is unavoidable. Raft and Paxos don't claim to make progress under all failure scenarios — they make progress under specific (majority-alive) scenarios.

2PC violates FLP by claiming to always terminate — and pays for this with the blocking problem. It can block indefinitely when the coordinator fails.

Every real system that claims to solve consensus either:
- Blocks under some failure scenarios (Raft, Paxos — won't elect leader without majority)
- Loses correctness under some scenarios (last-write-wins, eventual consistency systems)

FLP proves you must choose one.

---

### The CAP Theorem — Revisited With New Depth

We covered CAP earlier, but now we can understand it more precisely.

Eric Brewer's CAP theorem (2000), proved formally by Gilbert and Lynch (2002):

> *A distributed system cannot simultaneously provide all three: Consistency, Availability, and Partition Tolerance.*

But there's a subtlety that often gets lost:

**Partition Tolerance is not optional.** If your system runs across a network, network partitions WILL happen. Packets get dropped, cables get cut, data centers lose connectivity. You cannot build a "partition-free" distributed system because partitions are a property of networks, not your software.

So the real choice is: **when a partition happens, do you preserve Consistency or Availability?**

- **CP systems** (Consistent + Partition Tolerant): During a partition, refuse writes (or reads) to preserve consistency. Example: Raft-based systems like CockroachDB, etcd, Zookeeper. Minority partition goes down, refusing requests. Majority partition keeps working correctly.

- **AP systems** (Available + Partition Tolerant): During a partition, keep accepting writes on both sides of the partition. Risk inconsistency. Example: Cassandra, DynamoDB in eventual consistency mode. Both sides keep working but might diverge.

But here's what CAP doesn't capture: **latency.** Even in a non-partition scenario (normal operation), you face a tradeoff between consistency and latency. This led to the PACELC theorem.

---

### PACELC — The More Complete Model

Daniel Abadi extended CAP in 2012 with **PACELC**:

> *If there is a **P**artition: choose between **A**vailability and **C**onsistency.*
> *Else (normal operation): choose between **L**atency and **C**onsistency.*

```
System        │ Partition behavior │ Normal behavior
──────────────┼────────────────────┼────────────────────────
DynamoDB      │ PA (available)     │ EL (low latency)
              │                    │ (or EC with strong read)
Cassandra     │ PA (available)     │ EL (low latency)
Spanner       │ PC (consistent)    │ EC (consistent, higher
              │                    │    latency due to commit wait)
CockroachDB   │ PC (consistent)    │ EC (serializable, some
              │                    │    latency from HLC/Raft)
Zookeeper     │ PC (consistent)    │ EC (consistent reads)
```

In normal operation with no partitions — which is 99.99% of the time — the question is: how much latency are you willing to accept for how much consistency?

Spanner says: 14ms commit wait (2ε) for perfect external consistency. CockroachDB says: occasional restarts for serializable consistency. Cassandra says: sub-millisecond writes for eventual consistency.

These are business decisions expressed as technical choices.

---

## Chapter 9: The Mental Model for Picking the Right Approach

After everything we've covered, how do you actually choose?

Here's the decision tree experienced engineers use:

---

**Question 1: Do all the operations touch data you control?**

```
YES → Can use distributed transactions (2PC, Percolator, Spanner)
NO  → Must use Sagas (can't enroll external APIs in 2PC)
```

---

**Question 2: What is the cost of inconsistency?**

```
Money, inventory, medical records → cannot tolerate inconsistency
  → Use distributed transactions with Serializable isolation

User preferences, feed rankings, analytics → can tolerate brief inconsistency
  → Eventual consistency is fine, use Cassandra/DynamoDB-style
```

---

**Question 3: How frequently do transactions span multiple shards?**

```
Rarely (by design, data co-located) → 
  → Single-shard transactions are fast, use occasional 2PC for cross-shard
  
Frequently (unavoidable by data model) →
  → Distributed transactions are in your critical path
  → Choose a system optimized for this (Spanner, CockroachDB)
  → Or redesign your data model to minimize cross-shard transactions
```

---

**Question 4: What is your geographic distribution?**

```
Single datacenter →
  → Raft/Paxos with 2PC, ~1-2ms commit latency achievable
  
Multi-datacenter, same region →
  → ~10-20ms commit latency with 2PC + consensus
  
Global (multiple continents) →
  → 100-200ms commit latency unavoidable (speed of light)
  → Strongly consider if you actually need global strong consistency
  → For most operations: route writes to nearest region, accept eventual consistency
  → For critical operations: accept the latency, use Spanner or CockroachDB
```

---

**Question 5: Can you tolerate blocking during failures?**

```
YES (rare failures, operations team handles it) →
  → Classic 2PC is simpler to implement and understand
  
NO (must be always available) →
  → Percolator-style (commit state in database) or replicated coordinator (Spanner)
```

---

## The Full Story, End to End

```
Problem                          → Solution                → Limitation
──────────────────────────────────────────────────────────────────────────────
Two machines must agree,         → Two-Phase Commit        → Blocks if
naive "just send" loses money      (2PC)                     coordinator dies

2PC coordinator is single        → Three-Phase Commit      → Doesn't work
point of failure                   (3PC)                     with async networks

Need non-blocking at scale       → Percolator              → Requires timestamp
with no coordinator machine        (commit state            oracle, async cleanup
                                   in DB itself)            complexity

Percolator needs Google's        → CockroachDB             → Occasional transaction
infrastructure                    (Percolator +             restarts from HLC
                                   Raft + HLC)              uncertainty

Need perfect global ordering,    → Spanner                 → 14ms commit wait,
no restarts                        (TrueTime +               atomic clocks in
                                   Paxos + 2PC)              every datacenter

Operations span external         → Sagas                   → No isolation,
services you don't control         (compensating             intermediate states
                                   transactions)             visible to others

Formal limits: what's possible   → FLP: consensus          → Every system must
at all in distributed systems      impossible with           choose: block under
                                   one fault + async         failure OR sacrifice
                                   network                   correctness
                                 
                                 → PACELC: always           → Consistency always
                                   a tradeoff between        costs latency
                                   consistency and           somewhere
                                   latency/availability
```

---

**The meta-lesson of distributed transactions:**

> *There is no free lunch. Every solution to the distributed transaction problem pays a cost somewhere — in latency (Spanner's commit wait), in complexity (Percolator's lazy cleanup), in availability (2PC's blocking), in consistency (Sagas' isolation sacrifice). The engineer's job is not to find the costless solution — it doesn't exist — but to choose which cost their specific system can most afford to pay.*

---

**Where would you like to go next?**

- **LSM Trees vs B-Trees** — the storage engines underneath all of this, and why they matter for transaction performance
- **Consensus deep dive** — Raft's leader election edge cases, log compaction, membership changes, the subtle bugs in naive implementations
- **The Saga pattern in practice** — event sourcing, outbox pattern, idempotency keys, exactly-once semantics in message queues
- **Lock-free data structures** — how databases implement concurrent access without traditional locks using CAS operations and memory ordering
