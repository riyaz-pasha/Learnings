# The Story of Database Locking — Chapter 1

Let's start from the very beginning. Before we even talk about `SELECT FOR UPDATE`, we need to understand the *pain* that made people invent it.

---

## 🏦 The Bank Story — Where It All Begins

Imagine it's 2005. You're building an online banking system. Simple enough. Users can transfer money between accounts.

You write this code:

```sql
-- Step 1: Check the balance
SELECT balance FROM accounts WHERE id = 42;
-- Returns: 1000

-- Step 2: If balance >= amount, deduct it
UPDATE accounts SET balance = balance - 500 WHERE id = 42;
```

Works perfectly. You ship it. Everyone's happy.

Then one Monday morning, your phone blows up.

---

## 😱 The Problem: Two People, One Account, One Disaster

**Alice and Bob** both try to withdraw **₹800** from an account that has **₹1000**, *at the exact same millisecond*.

Here's what happens inside your database:

```
Time →

Alice's Transaction:               Bob's Transaction:
SELECT balance FROM accounts        SELECT balance FROM accounts
WHERE id = 42;                      WHERE id = 42;
-- Sees: 1000 ✅                    -- Sees: 1000 ✅

-- Both think: "1000 >= 800, okay!"

UPDATE accounts SET balance = 200   UPDATE accounts SET balance = 200
WHERE id = 42;                      WHERE id = 42;

COMMIT;                             COMMIT;
```

Both transactions **read 1000**, both think there's enough money, both withdraw ₹800. The account goes to ₹200 (not -₹600) because the second UPDATE just overwrites the first.

**₹600 has vanished into thin air.** Your bank just lost money.

This is called the **Lost Update Problem** or more specifically here, a **Race Condition**.

---

## 🤔 The Naive First Attempt — Application-Level Locking

The first instinct people had was: *"Let's handle this in the application code."*

```python
# Pseudo-code
acquire_lock("account_42")   # Some Redis/in-memory lock

balance = db.query("SELECT balance FROM accounts WHERE id = 42")
if balance >= 800:
    db.execute("UPDATE accounts SET balance = balance - 800 WHERE id = 42")

release_lock("account_42")
```

**This sounds reasonable. But it's a nightmare.**

- What if your app server crashes *between* acquiring the lock and releasing it? Lock is stuck forever.
- What if you have 10 app servers? You need a distributed lock system. Now you're managing Redis + your DB + lock expiry logic.
- What if the lock expires but your DB transaction is still running?
- What if two operations deadlock waiting on each other's Redis locks?

You've now built a second, fragile locking system *on top* of a database that already has a perfectly good locking system built in.

This is when smart engineers said:

> *"Wait. The database already knows about transactions. Why are we reinventing this? Let the database handle it."*

---

## ✅ The Solution: `SELECT FOR UPDATE`

The idea is beautifully simple:

> **"When I read a row, I want to LOCK it immediately — so nobody else can touch it until I'm done."**

```sql
BEGIN;

-- Read AND lock the row at the same time
SELECT balance FROM accounts WHERE id = 42 FOR UPDATE;
-- Returns: 1000
-- 🔒 Row is now LOCKED

-- Safe to proceed — nobody else can read-to-modify this row
UPDATE accounts SET balance = balance - 800 WHERE id = 42;

COMMIT; -- 🔓 Lock is released
```

Now let's replay Alice and Bob:

```
Alice's Transaction:                Bob's Transaction:

BEGIN;                              BEGIN;

SELECT balance FROM accounts        SELECT balance FROM accounts
WHERE id = 42 FOR UPDATE;           WHERE id = 42 FOR UPDATE;
-- Gets lock ✅                     -- BLOCKED 🚫 (Alice has the lock)
-- Sees: 1000                       -- Waiting...
                                    -- Waiting...
UPDATE ... SET balance = 200;
                                    -- Waiting...
COMMIT; (lock released)
                                    -- NOW gets the lock ✅
                                    -- Sees: 200
                                    -- 200 < 800, INSUFFICIENT FUNDS ✅
                                    ROLLBACK;
```

**Problem solved.** The database itself serializes access to that row. No Redis. No external locks. No app-level complexity.

---

## 🔍 How Does It Work Under the Hood?

`SELECT FOR UPDATE` acquires an **Exclusive Lock** (also called a Write Lock or X-Lock) on the selected rows.

The lock rules are:

| Lock Holder | New Request: Read (plain SELECT) | New Request: FOR UPDATE | New Request: FOR SHARE |
|---|---|---|---|
| Exclusive Lock (FOR UPDATE) | ✅ Allowed* | ❌ Blocked | ❌ Blocked |
| Shared Lock (FOR SHARE) | ✅ Allowed | ❌ Blocked | ✅ Allowed |

*Plain `SELECT` without any locking clause doesn't block on exclusive locks in most modern databases (due to MVCC — we'll touch on this). But `SELECT FOR UPDATE` from another transaction will block.

---

## 📦 Real Example with PostgreSQL

```sql
-- Terminal 1 (Alice)
BEGIN;
SELECT * FROM accounts WHERE id = 42 FOR UPDATE;
-- Gets the lock, sees balance = 1000

-- Terminal 2 (Bob) — run this WHILE Terminal 1 is open
BEGIN;
SELECT * FROM accounts WHERE id = 42 FOR UPDATE;
-- ⏳ Just hangs here, waiting for Alice to COMMIT or ROLLBACK

-- Back to Terminal 1
UPDATE accounts SET balance = 200 WHERE id = 42;
COMMIT;

-- Now Terminal 2 immediately unblocks
-- Sees the UPDATED balance = 200
```

This is the database doing exactly what you want — enforcing one-at-a-time access.

---

## 🎯 When Should You Use `SELECT FOR UPDATE`?

Use it when your logic follows this pattern:

1. **Read** a value
2. **Make a decision** based on that value
3. **Write** based on that decision

Without the lock, steps 1 and 3 are not atomic. With `FOR UPDATE`, you're saying "I'm reading this with the *intent to update*, so lock it now."

Classic use cases:
- **Inventory management** — "Is item in stock? Reserve it."
- **Seat booking** — "Is seat available? Book it."
- **Wallet/Banking** — "Is balance sufficient? Deduct it."
- **Job queues** — "Is this job unclaimed? Claim it."

---

## ⚠️ New Problem Introduced: Deadlocks

`SELECT FOR UPDATE` solves race conditions, but introduces a new villain: **Deadlocks**.

```
Alice locks Row A, then tries to lock Row B.
Bob   locks Row B, then tries to lock Row A.

Neither can proceed. Both wait forever.
```

```sql
-- Alice's transaction
BEGIN;
SELECT * FROM accounts WHERE id = 1 FOR UPDATE; -- Locks account 1
-- Now tries to lock account 2...
SELECT * FROM accounts WHERE id = 2 FOR UPDATE; -- WAITING for Bob

-- Bob's transaction (running simultaneously)
BEGIN;
SELECT * FROM accounts WHERE id = 2 FOR UPDATE; -- Locks account 2
-- Now tries to lock account 1...
SELECT * FROM accounts WHERE id = 1 FOR UPDATE; -- WAITING for Alice

-- 💀 Deadlock. Both wait forever.
```

**How databases solve it:** They have a **deadlock detector** that periodically looks for circular wait chains. When found, it picks one transaction as the "victim" and kills it with an error, letting the other proceed. The killed transaction gets an error and must retry.

**How you solve it in code:** Always acquire locks in a **consistent order**. If you always lock the lower account ID first, Alice and Bob will never deadlock:

```sql
-- Both Alice and Bob: always lock lower ID first
SELECT * FROM accounts WHERE id IN (1, 2) ORDER BY id FOR UPDATE;
-- id=1 locked first, then id=2. No circular dependency. ✅
```

---

## 📝 Summary of Chapter 1

The **problem**: Race conditions when multiple transactions read-then-write the same data.

The **solution**: `SELECT FOR UPDATE` — locks the row at read time, preventing others from modifying it until your transaction completes.

The **trade-off**: Contention. If many transactions compete for the same row, they queue up. And if you're not careful, you get deadlocks.

The **deadlock solution**: Consistent lock ordering + database's automatic deadlock detection.

---

**Ready for Chapter 2?**

Next up is `SELECT FOR SHARE` — which solves a different problem: *"What if multiple people need to READ the same data and ensure it doesn't change, but they don't need to block each other from reading?"* It's a more nuanced lock that enables more concurrency while still providing guarantees. 

Just say **"next"** when you're ready, or ask any questions about what we covered! 🚀
---
# Chapter 2: `SELECT FOR SHARE` — The Kinder, Gentler Lock

---

## 🔁 Quick Recap of Where We Are

In Chapter 1, we learned that `SELECT FOR UPDATE` puts an **Exclusive Lock** on rows — meaning:

> "I'm reading this row with intent to modify it. **Nobody else** can lock it until I'm done."

It's powerful. But it's also *aggressive*. And that aggression causes a real-world problem that pushed engineers to invent something softer.

---

## 🏗️ A New Scenario — The Order & Inventory System

Let's change our story. You're now building an **e-commerce platform**. You have two tables:

```sql
CREATE TABLE products (
    id SERIAL PRIMARY KEY,
    name TEXT,
    price NUMERIC,
    stock INT
);

CREATE TABLE order_items (
    id SERIAL PRIMARY KEY,
    order_id INT,
    product_id INT REFERENCES products(id),
    quantity INT,
    price_at_purchase NUMERIC  -- snapshot of price when ordered
);
```

You have **two kinds of operations** running constantly:

**Operation A — Place an Order:**
```
1. Read the product's current price
2. Check stock
3. Insert into order_items with that price
4. Decrease stock
```

**Operation B — Apply a Discount:**
```
1. Read the product's current price
2. Calculate discounted price
3. Update the product's price
```

---

## 😰 The Problem That Emerges

Imagine this sequence:

```
Time →

Order Transaction (Alice):          Discount Transaction (Admin):

SELECT price FROM products          
WHERE id = 5;                       
-- Sees price = 1000                

                                    UPDATE products 
                                    SET price = 700   ← discount applied
                                    WHERE id = 5;
                                    COMMIT;

INSERT INTO order_items (
  product_id, price_at_purchase
) VALUES (5, 1000);  ← STALE PRICE!
```

Alice read the price as **₹1000**, but by the time she inserted the order, the price had been changed to **₹700**. Now you have an order recorded at the old price. Depending on your business logic, this could be:

- A customer getting overcharged (angry customer)
- A customer getting something cheaper than current price (revenue loss)
- A legal/audit problem

**Alice needed the price to be stable for the duration of her transaction.** She needed to say:

> "I'm reading this row. Please make sure **nobody changes it** while I'm working, but I'm totally fine with **other people reading it** too."

This is a fundamentally different need from `FOR UPDATE`. She doesn't want to *modify* the product row herself — she just wants to **read it and trust that it won't change**.

---

## 🤔 Why Not Just Use `SELECT FOR UPDATE` Here?

You might think: *"Just use FOR UPDATE, problem solved."*

But watch what happens:

```sql
-- Alice placing an order
BEGIN;
SELECT * FROM products WHERE id = 5 FOR UPDATE; -- 🔒 Exclusive lock

-- Meanwhile, Bob is ALSO placing an order for the same product
BEGIN;
SELECT * FROM products WHERE id = 5 FOR UPDATE; -- ❌ BLOCKED. Waiting for Alice.
```

**Bob gets blocked even though he's also just reading the price.** Two customers trying to buy the same product simultaneously — a very common, totally harmless situation — are now forced to go one at a time.

On a flash sale with 10,000 concurrent users? **Your system grinds to a halt.**

`FOR UPDATE` is too possessive. It blocks even harmless readers.

---

## ✅ The Solution: `SELECT FOR SHARE`

`FOR SHARE` acquires a **Shared Lock** (also called a Read Lock or S-Lock):

> "I'm reading this row and I need it to stay stable. Others can **also read and lock it with FOR SHARE**, but **nobody can modify it** until I'm done."

```sql
-- Alice placing an order
BEGIN;
SELECT price, stock FROM products WHERE id = 5 FOR SHARE;
-- 🔒 Shared lock acquired. Price = 1000. Stock = 50.

-- Bob ALSO placing an order simultaneously
BEGIN;
SELECT price, stock FROM products WHERE id = 5 FOR SHARE;
-- ✅ ALSO gets a shared lock! Not blocked at all.
-- Price = 1000. Stock = 50.

-- Admin tries to apply a discount
BEGIN;
UPDATE products SET price = 700 WHERE id = 5;
-- ❌ BLOCKED. Cannot modify — Alice AND Bob hold shared locks.
```

Now Alice and Bob proceed in parallel without blocking each other. The admin's discount update is blocked until *both* finish their orders. Once both commit, the admin's update goes through.

This is the key insight:

| | `SELECT FOR UPDATE` | `SELECT FOR SHARE` |
|---|---|---|
| **Lock type** | Exclusive (X) | Shared (S) |
| **Blocks other FOR UPDATE?** | ✅ Yes | ✅ Yes |
| **Blocks other FOR SHARE?** | ✅ Yes | ❌ No |
| **Blocks plain SELECT?** | ❌ No (MVCC) | ❌ No (MVCC) |
| **Blocks UPDATE/DELETE?** | ✅ Yes | ✅ Yes |
| **Intent** | "I will modify this" | "I'm reading this, keep it stable" |

---

## 🔍 Under The Hood — The Lock Compatibility Matrix

Here's the full picture of how these locks interact:

```
                    Requested Lock →
                    | None  | FOR SHARE | FOR UPDATE | UPDATE/DELETE |
Held Lock ↓         |       |           |            |               |
--------------------|-------|-----------|------------|---------------|
None                |  ✅   |    ✅     |     ✅     |      ✅       |
FOR SHARE           |  ✅   |    ✅     |     ❌     |      ❌       |
FOR UPDATE          |  ✅   |    ❌     |     ❌     |      ❌       |
UPDATE/DELETE       |  ✅   |    ❌     |     ❌     |      ❌       |
```

Shared locks are **compatible with each other** but incompatible with anything that wants to write.

---

## 📦 Full Realistic Example

Let's walk through the order placement scenario properly:

```sql
-- ============================================
-- ALICE placing an order (Terminal 1)
-- ============================================
BEGIN;

-- Lock the product row for reading (shared — other readers welcome)
SELECT id, price, stock 
FROM products 
WHERE id = 5 
FOR SHARE;
-- Returns: id=5, price=1000, stock=50
-- 🔒 Shared lock on product 5

-- Business logic: validate stock, compute total
-- stock=50 >= quantity=2, so we're good

-- Insert the order item with the LOCKED-IN price
INSERT INTO order_items (order_id, product_id, quantity, price_at_purchase)
VALUES (101, 5, 2, 1000);

-- Now update stock (this is a write, so we'd want FOR UPDATE for the stock check)
UPDATE products SET stock = stock - 2 WHERE id = 5;

COMMIT;
-- 🔓 Lock released

-- ============================================
-- ADMIN applying discount (Terminal 2, runs WHILE Alice's transaction is open)
-- ============================================
BEGIN;

UPDATE products SET price = 700 WHERE id = 5;
-- ❌ BLOCKED — Alice holds a shared lock
-- Waiting...
-- (Once Alice commits, this proceeds)

COMMIT;
```

Alice's price is protected. The admin's discount takes effect *after* Alice's order is safely recorded at the original price. Clean, correct, auditable.

---

## ⚠️ New Problem Introduced: Lock Upgrades & Deadlocks

`FOR SHARE` introduces a subtle new deadlock scenario that's easy to miss.

**The Lock Upgrade Deadlock:**

Imagine Alice and Bob both start with `FOR SHARE`, then both *try to upgrade* to `FOR UPDATE`:

```sql
-- Alice                              -- Bob
BEGIN;                                BEGIN;

SELECT * FROM products                SELECT * FROM products  
WHERE id = 5 FOR SHARE;              WHERE id = 5 FOR SHARE;
-- ✅ Shared lock                     -- ✅ Shared lock (compatible!)

-- Both now decide they need to write
UPDATE products                       UPDATE products
SET stock = stock - 2                 SET stock = stock - 3
WHERE id = 5;                         WHERE id = 5;
-- ❌ BLOCKED waiting for Bob's        -- ❌ BLOCKED waiting for Alice's
--    shared lock to release               shared lock to release

-- 💀 DEADLOCK
```

Both hold shared locks. Both want to upgrade to exclusive. Neither can proceed because the other's shared lock is in the way.

**How to solve it:** If you *know* you'll eventually write to a row, **use `FOR UPDATE` from the beginning**, not `FOR SHARE`. Don't start with a shared lock hoping to upgrade — the database doesn't have a native "upgrade" operation and you'll deadlock.

```sql
-- Better approach: if you know you'll write, just use FOR UPDATE from the start
BEGIN;
SELECT * FROM products WHERE id = 5 FOR UPDATE;  -- Committed from the beginning
-- Now safe to read AND write
UPDATE products SET stock = stock - 2 WHERE id = 5;
COMMIT;
```

---

## 🎯 The Rule of Thumb

Ask yourself one question before choosing:

> **"Will I write to this row in this transaction?"**

- **Yes → `FOR UPDATE`** (Exclusive. "I own this row.")
- **No, but I need it stable → `FOR SHARE`** (Shared. "I'm just reading, but protect me.")
- **No, and I don't care if it changes → plain `SELECT`** (No lock. Fastest.)

---

## 🧩 Real-World Use Cases for `FOR SHARE`

**1. Foreign Key Validation**
When you insert a row that references another table, databases internally use `FOR SHARE` on the parent row. They're saying "I'm about to create a child referencing this parent — make sure nobody deletes the parent while I'm doing this."

**2. Price Snapshot Orders** (our example above)
Read price → lock it shared → insert order at that price → update stock.

**3. Multi-step Report Generation**
A report reads data across multiple tables and needs them consistent across all reads, but doesn't need to block other readers.

**4. Validation Before Bulk Insert**
"Validate that category 5 exists, then insert 1000 products under it" — lock category 5 with FOR SHARE while inserting.

---

## 📝 Summary of Chapter 2

The **problem**: `FOR UPDATE` is too aggressive — it blocks other readers unnecessarily, killing concurrency on read-heavy workloads.

The **solution**: `SELECT FOR SHARE` — a softer lock that says "keep this row stable, but let others read it too."

The **key insight**: Multiple shared locks coexist peacefully. Only writes get blocked.

The **new trade-off**: If you start with `FOR SHARE` and then decide to write, you're in deadlock territory. Commit to `FOR UPDATE` early if you know a write is coming.

---

**Ready for Chapter 3?**

Next up is `SELECT FOR UPDATE NOWAIT` — which solves a completely different frustration: *"What if I don't want to wait at all? What if I'd rather immediately fail and try something else than sit there blocked?"* It changes the entire error-handling philosophy of locking. 

---

# Chapter 3: `SELECT FOR UPDATE NOWAIT` — "Don't Make Me Wait"

---

## 🔁 Quick Recap

So far we have:
- **`FOR UPDATE`** — Exclusive lock. Block and wait if someone else has it.
- **`FOR SHARE`** — Shared lock. Block and wait only if someone is writing.

Both of these **wait**. If the row is locked, your transaction just sits there, frozen, until the other transaction finishes. And that waiting is about to become a very serious problem.

---

## 🎮 A New Scenario — The Flash Sale

You're building a ticketing platform. Ed Sheeran is performing. 50,000 people hit "Buy Ticket" at the exact same millisecond the sale opens.

Your code does this:

```sql
BEGIN;

SELECT * FROM tickets 
WHERE event_id = 99 
AND status = 'available' 
LIMIT 1 
FOR UPDATE;  -- Lock one available ticket

UPDATE tickets SET status = 'sold', buyer_id = ? WHERE id = ?;

COMMIT;
```

This is correct and safe. Only one person gets each ticket. No double-booking.

But here's what actually happens at scale:

```
50,000 requests arrive simultaneously.

Transaction 1 gets the lock.       → Processing ticket #1
Transactions 2–49,999 are WAITING. → All frozen. Staring at the locked row.

Transaction 1 commits.
Transaction 2 gets the lock.       → Processing ticket #1 (now sold!)
Transactions 3–49,999 are WAITING. → Still frozen.

...and so on.
```

**Every single user is waiting in a queue they didn't sign up for.**

The average user sits there for seconds — maybe tens of seconds — while 49,999 other transactions drain ahead of them. Your API response time goes from milliseconds to *minutes*. Users think the site crashed. They spam-click. They make it worse. Your servers pile up with thousands of open database connections, each one just... waiting.

And here's the darkest part: **most of them are waiting for a ticket that's already been sold.**

By the time Transaction #51 gets its turn, the last ticket (#50) was sold long ago. But Transaction #51 didn't know that. It waited patiently in line for 30 seconds just to be told "sorry, sold out."

This is an **experience disaster** and a **resource disaster** at the same time.

---

## 🤔 What Engineers Really Wanted

The engineers sat down and said:

> *"The waiting is the problem. If a row is locked, we don't want to wait for it — because by the time the lock releases, the data will probably have changed in a way that makes our work irrelevant anyway. We'd rather **know immediately** that the row is busy and **do something smarter** right now."*

This is a fundamentally different philosophy:

- **Old philosophy (FOR UPDATE):** "Wait your turn. Eventually you'll get in."
- **New philosophy (NOWAIT):** "If the door is locked, don't knock — immediately try the next door."

---

## ✅ The Solution: `SELECT FOR UPDATE NOWAIT`

`NOWAIT` tells the database:

> "Try to acquire the lock. If you can't get it **immediately**, don't wait — throw an **error** right now."

```sql
BEGIN;

SELECT * FROM tickets 
WHERE event_id = 99 
AND status = 'available' 
LIMIT 1 
FOR UPDATE NOWAIT;  -- Get the lock NOW or fail immediately

-- If lock was available: proceeds normally
-- If lock was NOT available: raises an error instantly
--   PostgreSQL: ERROR 55P03: could not obtain lock on row in relation "tickets"
--   MySQL:      ERROR 3572: Statement aborted because lock(s) could not be acquired immediately
--   Oracle:     ORA-00054: resource busy and acquire with NOWAIT specified

UPDATE tickets SET status = 'sold', buyer_id = ? WHERE id = ?;

COMMIT;
```

Now your application code can handle the error meaningfully:

```python
def buy_ticket(user_id, event_id):
    for attempt in range(3):  # Try up to 3 times
        try:
            with db.transaction():
                ticket = db.query("""
                    SELECT * FROM tickets 
                    WHERE event_id = %s AND status = 'available'
                    LIMIT 1
                    FOR UPDATE NOWAIT
                """, [event_id])
                
                if not ticket:
                    return {"error": "Sold out"}  # No tickets left at all
                
                db.execute("""
                    UPDATE tickets SET status = 'sold', buyer_id = %s 
                    WHERE id = %s
                """, [user_id, ticket.id])
                
                return {"success": True, "ticket_id": ticket.id}
        
        except LockNotAvailableError:
            # This specific ticket was being processed by someone else
            # Try again immediately — pick a DIFFERENT available ticket
            continue
    
    return {"error": "System busy, please try again"}
```

See the difference? Instead of freezing for 30 seconds, the application **immediately retries** with a fresh attempt to grab *any* available ticket — not the same locked one. This is dramatically faster and smarter.

---

## 🔍 The Timeline Comparison

**With `FOR UPDATE` (waiting):**
```
User clicks "Buy"
    │
    ▼
Transaction starts
    │
    ▼
SELECT FOR UPDATE ──── locked ──── waiting ──── waiting ──── waiting (30s) ────▶ Gets lock
                                                                                       │
                                                                                       ▼
                                                                               Ticket already sold
                                                                                       │
                                                                                       ▼
                                                                               ROLLBACK
                                                                               "Sold out" (30s later)
```

**With `FOR UPDATE NOWAIT`:**
```
User clicks "Buy"
    │
    ▼
Transaction starts
    │
    ▼
SELECT FOR UPDATE NOWAIT ──── locked ──── IMMEDIATE ERROR
    │
    ▼ (retry: pick different ticket)
SELECT FOR UPDATE NOWAIT ──── got it! ──── UPDATE ──── COMMIT ──── "Success!" (50ms total)
```

The user experience goes from **30 seconds of anxiety** to **50 milliseconds of success**.

---

## 📦 A Deeper Real Example — Job Queue

`NOWAIT` is absolutely beloved for **job queues** — systems where workers pick up tasks to process.

```sql
CREATE TABLE jobs (
    id SERIAL PRIMARY KEY,
    status TEXT DEFAULT 'pending',  -- pending, processing, done
    payload JSONB,
    created_at TIMESTAMP DEFAULT NOW()
);
```

You have 10 worker processes all running simultaneously, each trying to grab a job:

```sql
-- Each worker runs this
BEGIN;

SELECT * FROM jobs
WHERE status = 'pending'
ORDER BY created_at ASC
LIMIT 1
FOR UPDATE NOWAIT;  -- Critical: don't wait, try another

UPDATE jobs SET status = 'processing' WHERE id = ?;

COMMIT;
```

**Without NOWAIT:** Worker 2 through 10 all try to grab the same oldest job, and 9 of them freeze waiting for Worker 1 to release it. They're all staring at the same job instead of going and grabbing *different* pending jobs.

**With NOWAIT:** Worker 2 immediately gets an error that job #1 is taken, skips it, and grabs job #2. Worker 3 grabs job #3. All 10 workers are processing different jobs simultaneously. **10x throughput.**

---

## ⚠️ New Problems Introduced

### Problem 1: Retry Logic Complexity

`NOWAIT` shifts the responsibility of "what to do when busy" from the database to *your application*. Now you have to write retry logic, and doing it badly creates new problems:

**Bad retry — thundering herd:**
```python
except LockNotAvailableError:
    time.sleep(0)  # Instantly retry
    # Result: all 50,000 transactions immediately retry simultaneously
    # They all collide again. And again. And again. Infinite storm.
```

**Good retry — exponential backoff with jitter:**
```python
except LockNotAvailableError:
    sleep_time = (2 ** attempt) * 0.1 + random.uniform(0, 0.1)
    # Attempt 1: ~100ms, Attempt 2: ~200ms, Attempt 3: ~400ms
    # Jitter prevents synchronized retries
    time.sleep(sleep_time)
```

The jitter is crucial. Without it, all retrying threads sync up and hammer the database in waves.

### Problem 2: Starvation

With `NOWAIT`, there's no guaranteed queue. A transaction might fail to get a lock 10 times in a row just because it keeps hitting busy rows, while others breeze through. In a high-contention system, some transactions might **never** succeed.

**Solution:** Track retry count. After N failures, either: give up gracefully with a user-friendly message, or fall back to `FOR UPDATE` (with a timeout) to ensure eventual progress.

### Problem 3: False "Sold Out" Signals

In our ticket example, if `NOWAIT` fails 3 times and you return "sold out" — but there actually *were* available tickets, just contested — you've incorrectly told the user tickets are unavailable.

**Solution:** Distinguish between "no rows returned" (truly none available) and "lock error" (busy but maybe available). Your retry logic should only return "sold out" after a successful `SELECT` with no rows, not after a lock failure.

```python
except LockNotAvailableError:
    continue  # Don't give up yet — try again
else:
    if not ticket:
        return {"error": "Sold out"}  # Only say sold out if query returned nothing
```

---

## 🧩 When to Use `NOWAIT` vs Waiting `FOR UPDATE`

| Situation | Use |
|---|---|
| High concurrency, many available rows to choose from | `NOWAIT` — skip contested rows, grab others |
| Single specific row that MUST be processed | `FOR UPDATE` — wait your turn |
| Job/task queues with multiple workers | `NOWAIT` — workers spread out across jobs |
| Financial: deducting from a specific account | `FOR UPDATE` — there's only one account to wait for |
| Flash sales, ticket booking | `NOWAIT` — with smart retry |
| Admin operations, rare updates | Either — doesn't matter much |

The core question: **"Are there other rows I could work on instead?"**
- Yes → `NOWAIT` and retry with a different row
- No, it must be this row → `FOR UPDATE` and wait

---

## 📝 Summary of Chapter 3

The **problem**: `FOR UPDATE` makes transactions wait in a queue — disastrous under high concurrency when most of the wait is pointless.

The **solution**: `FOR UPDATE NOWAIT` — if you can't get the lock immediately, fail fast with an error so the application can do something smarter right now.

The **key shift**: Waiting is now the application's choice, not the database's default. You control the retry strategy.

The **new trade-offs**:
- You must write retry logic (with jitter)
- Risk of starvation under extreme contention
- Must carefully distinguish "lock busy" from "no rows"

---

**Ready for Chapter 4?**

Next is `SELECT FOR UPDATE SKIP LOCKED` — which is arguably the most *elegant* solution in this entire story. Instead of failing with an error when a row is locked, it just **silently skips** locked rows and returns the next available unlocked ones. It's like `NOWAIT`'s smarter sibling, and it completely transforms how job queues and task distribution systems are built.

---

# Chapter 4: `SELECT FOR UPDATE SKIP LOCKED` — "Skip the Queue, Grab What's Free"

---

## 🔁 Quick Recap

So far our locking toolkit:
- **`FOR UPDATE`** — Lock and **wait** if busy
- **`FOR SHARE`** — Shared lock and **wait** if someone's writing
- **`FOR UPDATE NOWAIT`** — Try to lock, **fail immediately** if busy

`NOWAIT` was a huge step forward — fail fast, retry smart. But engineers still weren't fully satisfied. Here's why.

---

## 😤 The Remaining Frustration With NOWAIT

Remember our job queue from Chapter 3?

```python
# Worker trying to grab a job
try:
    SELECT * FROM jobs WHERE status = 'pending' LIMIT 1 FOR UPDATE NOWAIT
except LockNotAvailableError:
    # Retry... but retry WHAT exactly?
    # We just asked for ANY pending job.
    # The database gave us job #1 (which was locked).
    # We failed. We retry. Database gives us job #1 AGAIN.
    # Still locked. We fail again. We retry again...
```

Here's the subtle problem: when you say `LIMIT 1` and the database picks job #1 — if that's locked and `NOWAIT` throws an error — **your retry will likely get job #1 again**. The database doesn't know you want to *skip* that one. You have to tell it which IDs to exclude, which means tracking failed IDs in your application, which gets messy fast:

```python
failed_ids = []
for attempt in range(10):
    try:
        job = db.query(f"""
            SELECT * FROM jobs 
            WHERE status = 'pending'
            AND id NOT IN ({','.join(failed_ids) or 'NULL'})
            LIMIT 1 
            FOR UPDATE NOWAIT
        """)
        break
    except LockNotAvailableError:
        failed_ids.append(last_tried_id)  # How do you even know this reliably?
```

This is ugly. You're building application-level skip logic that the database could handle for you. Engineers looked at this and said:

> *"What if the database just... automatically skipped locked rows and returned the next available unlocked one? No error. No retry loop. Just silently hop over anything that's busy."*

---

## ✅ The Solution: `SELECT FOR UPDATE SKIP LOCKED`

`SKIP LOCKED` changes the behavior completely:

> "When scanning rows, **silently skip** any row that is currently locked. Return only rows that are **immediately lockable**. Lock those and return them."

```sql
SELECT * FROM jobs
WHERE status = 'pending'
ORDER BY created_at ASC
LIMIT 1
FOR UPDATE SKIP LOCKED;
```

No error. No waiting. The database just glides past locked rows as if they don't exist, grabs the first unlocked one, locks it, and returns it to you.

---

## 🎬 The Magic in Slow Motion

Let's put 5 workers and 5 jobs on stage and watch what happens:

```
Jobs table:
id | status  | payload
---|---------|--------
1  | pending | "send email to Alice"
2  | pending | "resize image #44"
3  | pending | "generate report"
4  | pending | "send SMS to Bob"
5  | pending | "sync to S3"
```

All 5 workers start simultaneously:

```
Worker 1: SELECT ... LIMIT 1 FOR UPDATE SKIP LOCKED
          → Scans from top, job #1 is free → Locks #1 → Returns job #1

Worker 2: SELECT ... LIMIT 1 FOR UPDATE SKIP LOCKED
          → Scans from top, job #1 is LOCKED → Skips it
          → job #2 is free → Locks #2 → Returns job #2

Worker 3: SELECT ... LIMIT 1 FOR UPDATE SKIP LOCKED
          → job #1 locked → skip, job #2 locked → skip
          → job #3 is free → Locks #3 → Returns job #3

Worker 4: → Skips #1, #2, #3 → Locks #4 → Returns job #4

Worker 5: → Skips #1, #2, #3, #4 → Locks #5 → Returns job #5
```

**All 5 workers are processing different jobs simultaneously. Zero waiting. Zero errors. Zero retry logic needed.**

When a worker finishes, it commits, the lock releases, and next time it runs the query it'll pick up the next available pending job.

---

## 🔍 What "Skipping" Really Means Internally

This is important to understand deeply — `SKIP LOCKED` does **not** give you a consistent view of the table.

```sql
-- The actual table has 10 pending jobs: ids 1–10
-- Workers 1-4 have locked jobs 1, 3, 5, 7

SELECT id FROM jobs WHERE status = 'pending' FOR UPDATE SKIP LOCKED;

-- Returns: 2, 4, 6, 8, 9, 10
-- (Skipped 1, 3, 5, 7 because they're locked)
```

From the perspective of this query, jobs 1, 3, 5, 7 **don't exist right now**. They're invisible. This is intentional — but it means `SKIP LOCKED` queries are **not repeatable and not consistent**. The same query run twice in the same transaction might return different rows.

This is fine for task queues. It's catastrophic for financial reporting. **Never use `SKIP LOCKED` when you need a consistent view of data.** It's purpose-built for "grab and go" patterns only.

---

## 📦 The Full Production Job Queue Pattern

Here's how a real, production-grade job queue is built with `SKIP LOCKED`:

```sql
-- Jobs table (production-ready)
CREATE TABLE jobs (
    id          SERIAL PRIMARY KEY,
    status      TEXT NOT NULL DEFAULT 'pending',
    queue_name  TEXT NOT NULL DEFAULT 'default',
    payload     JSONB NOT NULL,
    attempts    INT NOT NULL DEFAULT 0,
    max_attempts INT NOT NULL DEFAULT 3,
    scheduled_at TIMESTAMP DEFAULT NOW(),
    locked_at   TIMESTAMP,
    locked_by   TEXT,  -- worker identifier
    created_at  TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_jobs_queue ON jobs (queue_name, status, scheduled_at)
WHERE status = 'pending';  -- Partial index — only index pending jobs
```

```python
import socket
import os

WORKER_ID = f"{socket.gethostname()}-{os.getpid()}"  # Unique worker identifier

def fetch_next_job(queue_name="default"):
    with db.transaction():
        # Grab the next available job — database handles all the skipping
        job = db.query_one("""
            SELECT * FROM jobs
            WHERE queue_name = %s
              AND status = 'pending'
              AND attempts < max_attempts
              AND scheduled_at <= NOW()
            ORDER BY scheduled_at ASC
            LIMIT 1
            FOR UPDATE SKIP LOCKED
        """, [queue_name])
        
        if not job:
            return None  # Truly no work available right now
        
        # Mark it as processing (within the same transaction)
        db.execute("""
            UPDATE jobs 
            SET status = 'processing',
                locked_at = NOW(),
                locked_by = %s,
                attempts = attempts + 1
            WHERE id = %s
        """, [WORKER_ID, job.id])
        
        return job  # Transaction commits here, lock becomes the status change
```

```python
def worker_loop(queue_name="default"):
    print(f"Worker {WORKER_ID} starting...")
    
    while True:
        job = fetch_next_job(queue_name)
        
        if not job:
            time.sleep(1)  # Nothing to do, poll again in 1 second
            continue
        
        try:
            process_job(job)  # Your actual business logic
            
            db.execute("""
                UPDATE jobs SET status = 'done' WHERE id = %s
            """, [job.id])
            
        except Exception as e:
            # Job failed — mark it, maybe retry later
            db.execute("""
                UPDATE jobs 
                SET status = CASE 
                    WHEN attempts >= max_attempts THEN 'failed'
                    ELSE 'pending'
                END,
                scheduled_at = NOW() + INTERVAL '5 minutes'  -- backoff
                WHERE id = %s
            """, [job.id])
```

Notice what's **completely absent**: no try/except for lock errors, no retry loops, no failed ID tracking. The database handles all the coordination. This is clean, production-quality code.

---

## ⚠️ New Problems Introduced

### Problem 1: The Phantom Job / Stale Lock Problem

Here's a scary scenario. A worker grabs a job, starts processing it, and then **crashes mid-way**:

```
Worker 1: Fetches job #5 with SKIP LOCKED ✅
Worker 1: Begins processing (calling external API...)
Worker 1: CRASHES 💀

Job #5: Still in status = 'processing'
        Nobody is working on it.
        Other workers skip it because... wait, they can't.
```

Actually, here's the key thing: `SKIP LOCKED` only skips rows with **active transaction locks**. Once Worker 1 crashes, its transaction is rolled back or its connection is dropped, and the **row-level lock is released automatically**. The row goes back to being a regular "processing" row.

But the *status* column still says `processing`. Other workers filter by `status = 'pending'`, so they skip it. **The job is stuck forever.**

**Solution: Heartbeat + Timeout pattern**

```sql
-- Add a heartbeat column
ALTER TABLE jobs ADD COLUMN heartbeat_at TIMESTAMP;

-- Worker updates heartbeat every 30 seconds while processing
UPDATE jobs SET heartbeat_at = NOW() WHERE id = ?;

-- A separate "reaper" process runs periodically:
-- Find jobs that are "processing" but whose worker died
UPDATE jobs 
SET status = 'pending', locked_by = NULL, locked_at = NULL
WHERE status = 'processing'
AND heartbeat_at < NOW() - INTERVAL '2 minutes';  -- Worker hasn't checked in
```

Or alternatively, use the `locked_at` timestamp and never trust a lock older than N minutes.

---

### Problem 2: Queue Starvation / Priority Inversion

With `SKIP LOCKED` and no priority system, a flood of low-priority jobs can prevent high-priority ones from being picked up:

```
Jobs (ordered by created_at):
#1 - LOW priority - locked by Worker 1
#2 - LOW priority - locked by Worker 2  
#3 - LOW priority - locked by Worker 3
#4 - HIGH priority - pending ← never reached if all workers are busy with 1,2,3
#5 - HIGH priority - pending
```

**Solution: Priority queues + multiple query strategies**

```sql
-- Add priority column
ALTER TABLE jobs ADD COLUMN priority INT DEFAULT 5;  -- 1=highest, 10=lowest

-- Workers query by priority first, then time
SELECT * FROM jobs
WHERE status = 'pending'
ORDER BY priority ASC, scheduled_at ASC  -- High priority first
LIMIT 1
FOR UPDATE SKIP LOCKED;
```

Or use separate queues per priority level and have some workers dedicated to high-priority queues:

```python
# High-priority worker
fetch_next_job(queue_name="critical")

# General worker: try critical first, fall back to normal
job = fetch_next_job("critical") or fetch_next_job("default")
```

---

### Problem 3: Hot Rows at the Top of the Queue

If all workers always `ORDER BY created_at ASC` and grab `LIMIT 1`, they all scan the same rows every time, even if those rows get skipped. Under very high concurrency, this index scanning itself becomes a bottleneck.

**Solution: Batch fetching**

Instead of grabbing 1 job at a time, grab a small batch:

```sql
-- Grab 5 jobs at once per worker
SELECT * FROM jobs
WHERE status = 'pending'
ORDER BY scheduled_at ASC
LIMIT 5
FOR UPDATE SKIP LOCKED;
```

Now each worker processes 5 jobs before going back to the queue. Fewer round trips to the database, less index contention, better throughput.

---

## 🆚 `SKIP LOCKED` vs `NOWAIT` — Which One When?

These two are often confused since both avoid waiting. Here's the real distinction:

| | `NOWAIT` | `SKIP LOCKED` |
|---|---|---|
| **When locked row found** | Throws error | Silently skips it |
| **You get back** | Error to handle | Clean result set |
| **Multiple matching rows** | Fails on first locked | Returns first unlocked |
| **Retry logic needed?** | Yes, in your code | No |
| **Use case** | "I need THIS specific row or fail" | "Give me ANY available row" |
| **Example** | Locking a specific user's account for a transfer | Grabbing any pending job from the queue |

The distinction in one sentence:

> **`NOWAIT`** is for when you care about a **specific row**.
> **`SKIP LOCKED`** is for when you care about **any available row**.

---

## 🧩 Real World Systems Built on SKIP LOCKED

Many famous systems are essentially `SKIP LOCKED` over a database table:

- **Sidekiq** (Ruby job queue) — Uses Redis, but same concept
- **Django-Q, Celery with DB broker** — Literally use `SKIP LOCKED`
- **Delayed::Job** (Rails) — Uses `SKIP LOCKED` on the jobs table
- **pg-boss** (Node.js) — PostgreSQL job queue built entirely on `SKIP LOCKED`
- **River** (Go) — Modern PostgreSQL job queue using `SKIP LOCKED`

The pattern is so well-proven that people now say: *"If you're already on PostgreSQL, you might not need Redis or RabbitMQ for your job queue."*

---

## 📝 Summary of Chapter 4

The **problem**: `NOWAIT` requires application-level retry logic to find unlocked rows. It's error-prone and messy at scale.

The **solution**: `SKIP LOCKED` — let the database silently hop over locked rows and return only immediately available ones. Zero errors, zero retry loops, maximum throughput.

The **key insight**: `SKIP LOCKED` is not a general-purpose locking tool. It deliberately gives you an **inconsistent, non-repeatable view** of the table — and that's exactly what makes it perfect for task distribution.

The **new trade-offs introduced and their solutions**:

- **Stale locks after crashes** → Heartbeat + timeout-based reaper
- **Priority starvation** → Priority column + ordered queries or dedicated queues
- **Index hotspot at queue head** → Batch fetching (LIMIT N instead of LIMIT 1)

---

## 🗺️ The Complete Story So Far

| Tool | Philosophy | Blocks? | Returns locked rows? | Use when |
|---|---|---|---|---|
| `FOR UPDATE` | Wait your turn | Yes, waits | Yes (after wait) | You need THIS row, willing to queue |
| `FOR SHARE` | Read together, write alone | Only for writes | Yes (after wait) | Reading with stability guarantee |
| `FOR UPDATE NOWAIT` | Try or fail | No | Error | You need THIS row, can't afford to wait |
| `FOR UPDATE SKIP LOCKED` | Skip the busy, grab the free | No | No (skips them) | Any row will do, maximize throughput |

---

**Ready for Chapter 5?**

You mentioned I should also cover something you missed — and there are actually **two powerful but lesser-known additions** to this story:

1. **`OF table_name`** — When you're joining multiple tables, how do you control *which table's rows* get locked? This is where most developers silently introduce bugs.

2. **`FOR UPDATE` with `LIMIT` and `ORDER BY` — The subtle trap** — Why the combination of these clauses can give you surprising and wrong results, and how to write them correctly.

---

# Chapter 5: The Hidden Traps — `OF table_name`, `LIMIT`, and `ORDER BY`

---

## 🔁 Quick Recap

We now have the full locking toolkit:
- **`FOR UPDATE`** — Wait and lock exclusively
- **`FOR SHARE`** — Wait and lock shared
- **`FOR UPDATE NOWAIT`** — Lock or fail immediately
- **`FOR UPDATE SKIP LOCKED`** — Lock what's free, skip the rest

You might think you're ready to go build production systems. And you almost are. But there are two silent killers hiding in plain sight that experienced engineers get wrong all the time — and they don't throw errors. They just **silently do the wrong thing**.

---

## 🔫 Silent Killer #1 — Locking the Wrong Table in a JOIN

### The Setup

You're building an order management system. You want to lock an order *and* its associated customer row when processing a refund — because you don't want the customer's account to be deleted or modified while you're issuing a refund to it.

You write this:

```sql
BEGIN;

SELECT o.id, o.total, c.email, c.account_status
FROM orders o
JOIN customers c ON o.customer_id = c.id
WHERE o.id = 500
FOR UPDATE;

-- Process refund...
COMMIT;
```

Looks reasonable. You're joining orders and customers, and slapping `FOR UPDATE` at the end.

**Question: Which rows got locked?**

Most developers assume: *"Both the order row AND the customer row."*

**The actual answer in PostgreSQL:** Both. But is that always what you want?

Now consider the inverse — what if you're processing a batch of orders and you only want to lock the **order rows**, not the customer rows? Maybe another system is doing customer analytics simultaneously and you don't want to block it.

Or worse — what if you have a complex join across 4 tables and `FOR UPDATE` locks **all of them**, creating massive unnecessary contention?

---

### The Real Problem — A Concrete Example

```sql
CREATE TABLE warehouses (
    id SERIAL PRIMARY KEY,
    name TEXT,
    region TEXT
);

CREATE TABLE inventory (
    id SERIAL PRIMARY KEY,
    warehouse_id INT REFERENCES warehouses(id),
    product_id INT,
    quantity INT
);
```

You have a restocking system. You want to:
- Lock the **inventory row** (because you'll update the quantity)
- But only **read** the warehouse row (just need its region for logging)

```sql
BEGIN;

SELECT i.quantity, w.region
FROM inventory i
JOIN warehouses w ON i.warehouse_id = w.id
WHERE i.product_id = 77
AND w.region = 'north'
FOR UPDATE;  -- Locks BOTH inventory AND warehouse rows
```

Now a completely unrelated operation — say, renaming a warehouse — is blocked:

```sql
-- Runs simultaneously, completely unrelated operation
UPDATE warehouses SET name = 'North Hub' WHERE id = 3;
-- ❌ BLOCKED — your refund query locked this warehouse row unnecessarily
```

You've created contention on a table you had no business locking. In a large system with many joins, this cascades into a web of unnecessary blocking.

---

### ✅ The Solution: `OF table_name`

The `OF` clause lets you specify **exactly which table's rows** should be locked:

```sql
BEGIN;

SELECT i.quantity, w.region
FROM inventory i
JOIN warehouses w ON i.warehouse_id = w.id
WHERE i.product_id = 77
AND w.region = 'north'
FOR UPDATE OF i;  -- ONLY lock inventory rows, NOT warehouse rows
```

Now the warehouse rename runs freely. You locked precisely what you needed and nothing more.

You can even mix lock types across tables:

```sql
SELECT o.total, c.email, c.credit_limit
FROM orders o
JOIN customers c ON o.customer_id = c.id
WHERE o.id = 500
FOR UPDATE OF o    -- Exclusive lock on the order (we'll write to it)
FOR SHARE OF c;    -- Shared lock on the customer (we'll only read it, but need stability)
```

This is surgical precision locking. You're telling the database exactly your intentions for each table.

---

### 📦 Full `OF` Example — The Refund System

```sql
BEGIN;

-- Lock the order exclusively (we're going to update it)
-- Lock the customer shared (we need their email/status to be stable, but won't change it)
-- Don't lock the payment_methods table at all (just reading for display)
SELECT 
    o.id,
    o.total,
    o.status,
    c.email,
    c.account_status,
    pm.last_four
FROM orders o
JOIN customers c ON o.customer_id = c.id
JOIN payment_methods pm ON o.payment_method_id = pm.id
WHERE o.id = 500
FOR UPDATE OF o
FOR SHARE OF c;
-- pm is not mentioned — not locked at all

-- Now safely update the order
UPDATE orders SET status = 'refunded', refunded_at = NOW() WHERE id = 500;

-- Send refund to c.email — knowing it won't change mid-transaction

COMMIT;
```

**Lock scope: exactly right. No more, no less.**

---

### ⚠️ The `OF` Gotcha — Ambiguous Column Names

`OF` requires the **alias or table name** you used in the query, not the original table name if you aliased it:

```sql
-- This WORKS
SELECT * FROM orders o JOIN customers c ON o.customer_id = c.id
FOR UPDATE OF o;  -- uses alias 'o'

-- This FAILS in PostgreSQL
SELECT * FROM orders o JOIN customers c ON o.customer_id = c.id
FOR UPDATE OF orders;  -- ERROR: relation "orders" in FOR UPDATE clause not found
                       -- because you aliased it as 'o'
```

Always use the **alias** when you've defined one. Simple rule, easy to forget, irritating to debug.

---

## 🔫 Silent Killer #2 — The `LIMIT` + `FOR UPDATE` Trap

This one is subtle enough that it has caused real production bugs in major companies. No errors thrown. Just silently wrong behavior.

### The Setup

You're building that job queue again. You want to fetch 3 pending jobs:

```sql
SELECT * FROM jobs
WHERE status = 'pending'
ORDER BY created_at ASC
LIMIT 3
FOR UPDATE SKIP LOCKED;
```

Seems straightforward. But there's a question hiding here:

**Does the database apply `LIMIT` before or after `FOR UPDATE SKIP LOCKED`?**

---

### The Trap Explained

Imagine the jobs table looks like this:

```
id | status     | created_at
---|------------|------------
1  | pending    | 10:00:01   ← locked by another worker
2  | pending    | 10:00:02   ← locked by another worker
3  | pending    | 10:00:03   ← locked by another worker
4  | pending    | 10:00:04   ← FREE
5  | pending    | 10:00:05   ← FREE
6  | pending    | 10:00:06   ← FREE
7  | pending    | 10:00:07   ← FREE
```

You run: `LIMIT 3 FOR UPDATE SKIP LOCKED`

**What you expect:** "Give me 3 free jobs" → Returns jobs 4, 5, 6 ✅

**What actually happens in some databases/scenarios:**

The database might:
1. Apply `LIMIT 3` first → Gets rows 1, 2, 3
2. Then try to apply `FOR UPDATE SKIP LOCKED` → All 3 are locked, skip all
3. Returns **0 rows** ❌

Or worse — it might return only 1 or 2 rows even though 4 free rows exist, because it hit the LIMIT before collecting enough unlocked rows.

---

### Does PostgreSQL Have This Problem?

Let's be precise. **PostgreSQL's behavior:**

PostgreSQL evaluates `SKIP LOCKED` during the scan — it skips locked rows *as it scans*, then applies `LIMIT` to the **skippable results**. So in PostgreSQL, `LIMIT 3 FOR UPDATE SKIP LOCKED` correctly returns 3 unlocked rows (4, 5, 6 in our example).

But here's where it gets dangerous — **`ORDER BY` interacts with this in a non-obvious way:**

```sql
-- This query in PostgreSQL
SELECT * FROM jobs
WHERE status = 'pending'
ORDER BY created_at ASC
LIMIT 3
FOR UPDATE SKIP LOCKED;
```

PostgreSQL must sort ALL qualifying rows first (to determine the ORDER), THEN apply LIMIT + SKIP LOCKED. Under high concurrency, by the time it's done sorting and tries to lock the top 3 results, some may have just been locked by another transaction. It handles this correctly — but it means the sort happens on a potentially large result set before any skipping, which is a **performance** trap, not a correctness trap.

The correctness trap appears in **MySQL before 8.0** and some other databases that apply LIMIT before SKIP LOCKED. Always check your specific database's documentation.

---

### The Subtler ORDER BY Trap — Wrong Rows Locked

Here's the trap that hits PostgreSQL users too. Consider:

```sql
-- You want the 3 highest-priority pending jobs
SELECT * FROM jobs
WHERE status = 'pending'
ORDER BY priority DESC, created_at ASC
LIMIT 3
FOR UPDATE SKIP LOCKED;
```

Jobs table:
```
id | priority | status  | locked?
---|----------|---------|--------
1  |    10    | pending | YES (locked)
2  |    10    | pending | NO  (free)
3  |    9     | pending | NO  (free)
4  |    8     | pending | NO  (free)
```

You get back: jobs 2, 3, 4 ✅ — correct, the top 3 *available* by priority.

But now imagine:

```
id | priority | status  | locked?
---|----------|---------|--------
1  |    10    | pending | YES
2  |    10    | pending | YES
3  |    10    | pending | YES
4  |    9     | pending | NO
5  |    8     | pending | NO
6  |    7     | pending | NO
```

You run `LIMIT 3 FOR UPDATE SKIP LOCKED` ordered by priority DESC.

You get: jobs 4, 5, 6 — but these are **lower priority** than what you wanted. The system is now processing low-priority work while 3 workers are grinding on high-priority items.

**This is correct behavior — but it's a design problem you must be aware of.** `SKIP LOCKED` doesn't guarantee you get the globally highest-priority items. It guarantees you get the highest-priority *available* items. Under high concurrency those are different things.

**Solution: Priority queues, not priority columns**

```sql
-- Instead of one table with a priority column,
-- use separate tables (or queue_name values) per priority tier

-- Worker logic:
job = (
    fetch_from_queue('critical')   -- Try critical first
    or fetch_from_queue('high')    -- Then high
    or fetch_from_queue('normal')  -- Then normal
)
```

This guarantees a critical job is never skipped in favor of a normal one, regardless of locking state.

---

### The `COUNT` Trap

One more `LIMIT` variant that burns people:

```sql
-- WRONG: This does NOT tell you how many unlocked pending jobs exist
SELECT COUNT(*) FROM jobs WHERE status = 'pending' FOR UPDATE SKIP LOCKED;

-- COUNT with SKIP LOCKED counts only the rows it could lock RIGHT NOW
-- This number changes every millisecond under concurrency
-- It's meaningless as a "how many jobs are pending" metric
```

**Never use `COUNT` with `SKIP LOCKED`** for monitoring or reporting. Use a separate plain `SELECT COUNT(*)` without any locking clause for that.

---

## 🔍 Putting It All Together — The Correct Production Pattern

Here's a query that combines everything correctly:

```sql
-- Production job queue fetch: correct, performant, precise
BEGIN;

SELECT 
    j.id,
    j.payload,
    j.attempts,
    q.max_concurrency  -- just reading queue config, don't lock it
FROM jobs j
JOIN queues q ON j.queue_name = q.name
WHERE j.status = 'pending'
  AND j.queue_name = 'email'
  AND j.scheduled_at <= NOW()
  AND j.attempts < j.max_attempts
ORDER BY j.priority DESC, j.scheduled_at ASC
LIMIT 5
FOR UPDATE OF j          -- ONLY lock job rows
SKIP LOCKED;             -- Skip any job already being processed

-- Update status in same transaction
UPDATE jobs 
SET status = 'processing', locked_by = 'worker-7', locked_at = NOW()
WHERE id = ANY(ARRAY[...ids from above...]);

COMMIT;
```

Every clause has a reason:
- `OF j` → Don't lock the queues config table
- `SKIP LOCKED` → Don't wait, grab what's free
- `LIMIT 5` → Batch for throughput
- `ORDER BY priority DESC` → Best-effort priority (knowing high-priority might be taken)
- Status update in same transaction → Atomic claim

---

## 📝 Summary of Chapter 5

### Silent Killer #1: Locking the Wrong Tables in JOINs
- **Problem:** `FOR UPDATE` on a JOIN locks ALL joined tables by default
- **Solution:** `FOR UPDATE OF table_alias` to lock only what you intend
- **Gotcha:** Use the alias, not the original table name
- **Best practice:** Always explicitly specify `OF` in multi-table queries

### Silent Killer #2: `LIMIT` and `ORDER BY` Interactions
- **Problem:** `LIMIT` + `SKIP LOCKED` can return fewer rows than LIMIT or interact with ORDER BY in performance-costly ways
- **Solution:** Understand your database's specific execution order; test under concurrency
- **`COUNT` trap:** Never count with `SKIP LOCKED` for reporting — it's meaningless
- **Priority trap:** `SKIP LOCKED` gives best-effort priority, not global priority — use separate queues for hard priority guarantees

---

## 🗺️ The Complete Map — Everything We've Covered

```
The Problem                          The Solution              The Trade-off
─────────────────────────────────────────────────────────────────────────────
Race conditions on read-then-write → FOR UPDATE            → Deadlocks, contention
Too much blocking on reads         → FOR SHARE             → Lock upgrade deadlocks  
Waiting is too expensive           → FOR UPDATE NOWAIT     → Retry complexity
Retry logic is too messy           → SKIP LOCKED           → Stale locks, priority gaps
Locking too many joined tables     → OF table_name         → Must remember aliases
LIMIT returns wrong rows           → Understand exec order → Test under concurrency
```

---

**And That's the Full Story.**

You now understand not just *what* these tools do, but *why* they were invented, *what problems they introduced*, and *how those were solved in turn*. That chain of problem → solution → new problem → better solution is exactly how database internals evolved over decades.

A few things you could explore next if you want to go even deeper:

- **Advisory Locks** (`pg_advisory_lock`) — application-level locks *inside* the database, for things that don't map to specific rows
- **Optimistic Locking** — the philosophical opposite: don't lock at all, detect conflicts at write time using version columns
- **MVCC (Multi-Version Concurrency Control)** — the underlying engine that makes plain `SELECT` not block on locked rows, which is what makes all of this work without full table freezes
- **Isolation Levels** (`READ COMMITTED`, `REPEATABLE READ`, `SERIALIZABLE`) — the broader framework that `FOR UPDATE` sits inside

Want to dive into any of these? 🚀
