# The Double Booking Problem — A Complete Reference

*Hotels, tickets, seats: why it happens, how it's solved, and why different companies solve it differently.*

---

## Part 1 — Where the Problem Comes From

**The single-clerk era.** One hotel front desk, one paper ledger. Booking a room is: (1) look at the ledger, (2) see it's empty, (3) write the guest's name. Because there's one clerk, these three steps always happen back-to-back, with nothing squeezing in between — the ledger is *serialized* purely because a human can only do one thing at a time.

**Where it breaks.** Add more clerks (or servers, or website visitors) all looking at what's *supposed to* be the same ledger, and this gap appears:

| Time | Clerk A | Clerk B |
|---|---|---|
| 10:00:00.000 | Checks Room 101, Friday → empty | |
| 10:00:00.050 | | Also checks → empty |
| 10:00:00.400 | Writes "Alice" | |
| 10:00:00.600 | | Writes "Bob" |

Both clerks followed the procedure correctly. The procedure itself stopped being safe once more than one actor could act on the same record at the same time. This gap — between *"I looked and it was free"* and *"I claimed it"* — is a **race condition**. The double booking problem is the booking-system flavor of a general problem: what happens when independent actors read shared state, decide, and act, without knowing about each other.

---

## Part 2 — The First "Fix" (And Why It Wasn't One)

**Naive code**, translating the clerk's steps directly:

```python
def book_room(room_id, date, guest_name):
    booking = db.query(
        "SELECT * FROM bookings WHERE room_id=? AND date=?", room_id, date
    )
    if booking is None:
        db.execute(
            "INSERT INTO bookings (room_id, date, guest) VALUES (?, ?, ?)",
            room_id, date, guest_name
        )
        return "Success"
    return "Room already booked"
```

Looks correct — but "check" and "act" are two separate operations, and anything that isn't a single atomic operation can be interleaved by another process. Under concurrent load (many requests per second, possibly across multiple servers), both requests can pass the check before either writes — same race as Part 1, just faster and more frequent.

**Second attempt: an in-memory lock.**

```python
room_locks = {}  # (room_id, date) -> Lock

def book_room(room_id, date, guest_name):
    key = (room_id, date)
    room_locks.setdefault(key, Lock())
    with room_locks[key]:
        booking = db.query(...)
        if booking is None:
            db.execute(...)
            return "Success"
        return "Room already booked"
```

Works perfectly **on one server**. Fails the moment there's more than one process — `room_locks` lives in one process's memory; a second server has never heard of it. **Lesson: a lock is only as strong as the scope everyone agrees to respect it in.**

---

## Part 3 — The One Thing Every Server Shares: The Database

All servers share exactly one thing: the database. Two real strategies follow from that.

### Strategy 1 — Pessimistic locking (`SELECT ... FOR UPDATE`)

```sql
BEGIN TRANSACTION;

SELECT * FROM bookings
WHERE room_id = 101 AND date = '2026-09-04'
FOR UPDATE;                      -- locks the row for writers

-- if nothing was returned:
INSERT INTO bookings (room_id, date, guest) VALUES (101, '2026-09-04', 'Alice');

COMMIT;
```

A second request's `FOR UPDATE` **blocks** until the first commits, then re-reads and correctly sees the row exists. This recreates single-clerk serialization, enforced by the database.

- **Cost 1 — throughput**: every other request for that same row sits and waits, holding a connection open. Fine for low contention; brutal at Ticketmaster-scale demand.
- **Cost 2 — deadlocks**: locking two rows in different orders across two transactions can deadlock; databases detect and kill one, so the app still needs retry logic.

### Strategy 2 — Let a unique constraint be the lock

```sql
CREATE TABLE bookings (
    room_id INT,
    date DATE,
    guest TEXT,
    UNIQUE (room_id, date)
);
```

```python
def book_room(room_id, date, guest_name):
    try:
        db.execute(
            "INSERT INTO bookings (room_id, date, guest) VALUES (?, ?, ?)",
            room_id, date, guest_name
        )
        return "Success"
    except UniqueConstraintViolation:
        return "Room already booked"
```

No `SELECT`, no explicit lock — just attempt the insert and let the storage engine's uniqueness guarantee reject the loser. Cheaper than `FOR UPDATE`, but only applies to the *creation* case (there's no "duplicate" to catch when you're updating an existing row).

---

## Part 4 — Optimistic Locking, and What ACID Actually Promises

### The lost-update problem (the update-not-insert version of the race)

```python
def book_deluxe_room():
    row = db.query("SELECT available FROM inventory WHERE room_type='Deluxe' AND date='2026-09-04'")
    if row.available > 0:
        db.execute(
            "UPDATE inventory SET available = ? WHERE room_type='Deluxe' AND date='2026-09-04'",
            row.available - 1
        )
        return "Booked"
    return "Sold out"
```

Two concurrent requests both read `available=3`, both compute `2`, both write `2` — one decrement is silently lost. Same disease as double booking (read-then-write gap), different costume.

### Optimistic locking — verify at write time instead of blocking up front

```sql
ALTER TABLE inventory ADD COLUMN version INT DEFAULT 0;
```

```python
def book_deluxe_room():
    row = db.query(
        "SELECT available, version FROM inventory WHERE room_type='Deluxe' AND date='2026-09-04'"
    )
    if row.available <= 0:
        return "Sold out"

    result = db.execute(
        """UPDATE inventory
           SET available = ?, version = version + 1
           WHERE room_type='Deluxe' AND date='2026-09-04' AND version = ?""",
        row.available - 1, row.version
    )
    if result.rows_affected == 0:
        return "Retry"   # version didn't match — someone else updated first
    return "Booked"
```

The write is conditional on nothing having changed since you read it. The loser's `UPDATE` matches zero rows; the app decides whether to retry or surface a "just got taken" message to the user. Optimistic locking bets collisions are rare and pays a retry cost only when wrong — the opposite bet from pessimistic locking's "always pay, never collide."

### What ACID actually guarantees (and doesn't)

- **Atomicity** — all-or-nothing writes within one transaction. Says nothing about *timing between* two transactions — doesn't stop lost updates.
- **Consistency** — enforces constraints you defined (e.g., `UNIQUE`). Only protects rules the database knows about — add `CHECK (available >= 0)` yourself if you need it.
- **Isolation** — the one that actually governs races, and it's a *dial*:
  - `READ COMMITTED` (Postgres default): never reads uncommitted data, but does **not** prevent lost updates.
  - `REPEATABLE READ`: consistent snapshot for the transaction's duration — still doesn't prevent lost updates in all databases (notable Postgres gotcha).
  - `SERIALIZABLE`: outcome is *as if* transactions ran one after another — fully closes the gap, but slowest, often implemented via conflict detection + forced retries (conceptually similar to the manual `version` column).
- **Durability** — survives a crash once committed. Real, but unrelated to races.

**Takeaway: ACID doesn't save you by default.** Most production databases run `READ COMMITTED` because `SERIALIZABLE` is too slow for general traffic — so the developer is responsible for explicitly reaching for `FOR UPDATE`, a unique constraint, or a version column, depending on the operation.

---

## Part 5 — How Real Companies Chose Differently

The right strategy is a function of: (a) how many people contend for the *same* resource at the *same instant*, (b) how cheap a rare failure is to absorb, (c) whether you even have transactional access to the source of truth.

### Ticketmaster — pessimistic locking, time-boxed (not a DB lock)

Holding a real `FOR UPDATE` row lock for the *minutes* a human takes to check out would grind the system to a halt. Instead:

```
1. User clicks seat → write a "hold" record:
   seat_id=A12, status='held', held_by=user123, expires_at=now()+120s
   (uses a unique constraint, so only one hold can exist per seat)
2. Seat shows unavailable to everyone else
3. User has 120s to complete checkout
4. Pay in time → hold converts to a real booking
5. Timer expires → background job deletes the hold, seat reopens
```

Pessimistic *in effect* (blocks others immediately), implemented without an actual long-held database lock. **New problem introduced: the zombie cart** — if the user abandons checkout or the expiry job lags/crashes, the seat stays falsely "held," losing inventory during the highest-demand seconds. Solved with redundant, independent expiry mechanisms (e.g., a delayed message queue entry, not reliant on one background job's uptime).

### Airlines — overbooking as a deliberate business decision, not a bug

Airlines prevent literally selling the same seat number twice (strong consistency internally), but routinely sell *more tickets than seats*, on purpose — based on statistical no-show models, because an empty seat at departure is unrecoverable revenue. This isn't a race condition tolerated by accident; it's a **known, bounded, chosen** rate of overbooking, handled by a separate system entirely: bumping, compensation, rebooking. Not every "two people, one seat" situation is a bug to engineer away — sometimes it's cheaper to occasionally pay for a bumped passenger than to leave that revenue on the table every flight.

### Booking.com / aggregators — optimistic at internet scale, with compensation

Booking.com doesn't own the hotel's inventory system — it's one of several channels (hotel front desk, other OTAs, phone line) selling from availability that's only *synced*, not live-shared. No single database to put `FOR UPDATE` on. So:

```
1. Show availability from the last sync (possibly minutes stale)
2. Accept the booking request optimistically
3. Fire a confirmation request to the hotel's system
4. If the hotel says "that room's gone" → a real double booking happened
5. Compensating workflow: refund, and often rebook the guest at an
   equal-or-better room at Booking.com's own cost
```

Optimistic locking's version-mismatch idea, except the "retry" is a customer-service/refund pipeline, because the two systems are separated by network calls and independent databases, not microseconds inside one transaction.

**Pattern:** each choice traded one problem for a new one — zombie holds, angry bumped passengers, refund pipelines. None are "solved" in a final sense; each is a different point on the correctness/speed/ownership trade-off curve.

---

## Part 6 — When There's No Single Database Left (Microservices)

Booking now spans separate services with separate databases that can't share one SQL transaction:

```
Inventory Service   — "is this room available"
Pricing Service      — "what does this cost"
Payment Service      — "charge the card"
```

Two new failure shapes appear:

1. **Distributed race** — two requests can both pass Inventory's check near-simultaneously, same as Part 2, now across a network with its own latency and possible replica lag.
2. **Partial failure** — Inventory and Pricing succeed, Payment fails (card declined). No single `ROLLBACK` can undo this — it was three independent commits in three independent systems, not one transaction.

### Attempt 1 — Distributed locks (e.g., Redlock)

```
1. Acquire lock "room:101:2026-09-04" in Redis, TTL = 10s
2. If acquired: call Inventory → Pricing → Payment in sequence
3. Release the lock (TTL as a safety net)
```

Works in the common case, but inherits Part 3's throughput cost, plus a new failure: if the lock holder crashes mid-sequence before the TTL expires, the room is stuck half-reserved and blocked from everyone else. If the TTL is too short, it can expire while work is still legitimately in progress, and a second process grabs a "lock" the first still believes it holds — meaning the lock wasn't truly mutually exclusive. (This is the substance of the well-known Redlock correctness debate: locks with timers are a compromise, not a hard guarantee, when the machines involved don't share a clock or scheduling guarantee.)

### Attempt 2 — Sagas: no cross-service lock at all, just undo steps

```
Step 1: Inventory reserves room 101      → succeeds
Step 2: Pricing locks in the price        → succeeds
Step 3: Payment charges the card          → FAILS

→ Compensate in reverse:
  Undo Step 2: Pricing releases the locked-in price
  Undo Step 1: Inventory un-reserves room 101
```

Each step is a normal local transaction (still using Part 3/4's guarantees within its own database). What's new: every state-changing step must define its own undo action, since there's no global rollback. Conceptually the same idea as Booking.com's "detect and pay to fix" (Part 5), but automated and code-defined. **Sharp edge**: the window between Step 1 succeeding and Step 3 failing is a window where the room *looks* reserved to everyone else even though the booking will ultimately fail — a milder, automated cousin of the zombie-hold problem.

---

## The Pattern Underneath Everything

| Stage | How correctness was achieved |
|---|---|
| Single clerk | Perfect, because concurrency didn't exist |
| Naive code | Broken — copied the old procedure without noticing concurrency now existed |
| Shared database | Regained via blocking (pessimistic), mechanical guarantees (constraints), or check-on-write (optimistic) |
| Multiple companies / partial sharing | Traded for speed or business economics; failures handled *after* the fact |
| Multiple services | Either faked a shared lock (distributed lock, with crash/clock caveats) or gave up atomicity entirely and made every failure reversible (sagas) |

There's no final fix that makes the problem disappear — every layer of scale just moves the system to a different point on the trade-off between **strict correctness, throughput, and how expensive a rare mistake is to clean up afterward.**
