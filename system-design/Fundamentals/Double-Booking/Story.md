Good — this fits the same "one concept at a time, story-first" style you've been using for the distributed systems stuff. Let's start.

## Part 1: Where This Problem Actually Comes From

Before we talk about databases and code, picture a small hotel in the 1960s. One front desk. One ledger — a big paper book with rooms down the side and dates across the top. When someone calls to book Room 101 for Friday night, the clerk does something so natural nobody would've called it a "protocol":

1. Look at the ledger, find Room 101, Friday's column.
2. See it's empty.
3. Write the guest's name in it.

Because there's **one clerk and one ledger**, these three steps always happen one after another, with nothing squeezing in between. Even if the phone rings again the instant she hangs up, she finishes writing the name before she even picks it up. The ledger is *serialized* by nature — not because anyone designed it that way, but because a human can only do one thing at a time.

This is the quiet assumption that every booking system — hotels, cinemas, airlines — was built on for decades: **check-then-write, with nothing else allowed to happen in between.**

### Where it broke

Now fast-forward. The hotel grows. It's not one clerk anymore — it's three people at three counters, a call center in another city, and (later) a website. All of them are looking at what is *supposed to* be the same ledger. But now, physically, there is no single ledger anyone is staring at — there's a shared record sitting on a server, and everyone's *reading a copy of it* into their own head or their own screen before acting.

Here's the moment it goes wrong, laid out step by step:

| Time | Clerk A (front desk) | Clerk B (phone) |
|------|----------------------|------------------|
| 10:00:00.000 | Checks Room 101, Friday → sees "empty" | |
| 10:00:00.050 | | Checks Room 101, Friday → *also* sees "empty" |
| 10:00:00.400 | Writes "Alice" into Room 101, Friday | |
| 10:00:00.600 | | Writes "Bob" into Room 101, Friday |

Both clerks did *exactly* the right thing according to the old procedure. Both saw an empty room. Both booked it. Nobody made a mistake — the procedure itself stopped being safe the moment more than one person could act on the same record at the same time. Room 101 is now double-booked, and nobody will find out until Alice and Bob both show up on Friday night.

This gap — between **"I looked and it was free"** and **"I claimed it"** — is called a **race condition**, and the double booking problem is really just the booking-system flavor of a much older, much more general problem in computing: *what happens when two independent actors read shared state, decide based on what they read, and then both act on that decision, without knowing about each other?*

That's the seed of everything we're about to unpack: guarantees, locking, optimistic vs pessimistic strategies, how Ticketmaster does it differently from how Booking.com does it, and why.

---

Before I move to Part 2 (the first attempts people made to fix this, and why those attempts *also* broke) — does this framing of the root cause make sense? Specifically the "check-then-write gap" as the actual source of the problem, not just "computers are bad at this"?

---

## Part 2: The First "Fix" — And Why It Wasn't One

So the hotel chain hires developers. They build a website. Someone points out the race condition from Part 1, and the fix seems obvious: **just make the code check before it writes.** That's literally what the clerk did — so let's translate it directly into code.

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
    else:
        return "Room already booked"
```

This looks *exactly* like what the clerk did: check, then act. And on paper it's correct. The bug isn't in the logic — it's in something the code doesn't show you at all: **how long each line takes, and what else can happen while it's running.**

### Watch it fail

A `SELECT` isn't instant. A network hop to the database, query planning, disk/cache lookup — even a few milliseconds is an eternity to a CPU. And now imagine this code isn't running once — it's running on a web server handling hundreds of requests per second, possibly across *multiple* server instances behind a load balancer.

| Time | Request A (Alice) | Request B (Bob) |
|------|--------------------|--------------------|
| t0 | `SELECT` → no booking found | |
| t1 | | `SELECT` → no booking found (A hasn't inserted yet) |
| t2 | `INSERT` Alice succeeds | |
| t3 | | `INSERT` Bob succeeds |

Both requests ran the *exact same "correct" logic*. Both passed the check. Both inserted. If the database doesn't stop them, you now have two rows: Room 101, Friday, Alice — and Room 101, Friday, Bob. The bug wasn't a typo or a missing `if`. It's that **"check" and "act" are two separate operations, and anything that is not a single atomic operation can be interleaved by another process.** Writing it in code didn't remove the gap — it actually made the gap *more dangerous*, because computers can interleave operations at a timescale and frequency no two human clerks ever could.

### The second attempt: "let's just lock it"

The next instinct — a very reasonable one — is: *fine, let's make sure only one request can touch this room+date at a time.* So a junior dev adds an in-memory lock:

```python
room_locks = {}  # key: (room_id, date) -> lock

def book_room(room_id, date, guest_name):
    key = (room_id, date)
    if key not in room_locks:
        room_locks[key] = Lock()

    with room_locks[key]:
        booking = db.query(...)
        if booking is None:
            db.execute(...)
            return "Success"
        return "Room already booked"
```

This actually works... **on one server.** Requests hitting *that one process* now queue up nicely for the same room+date, just like the single clerk with the single ledger.

But the hotel's website doesn't run on one server — it runs on ten, behind a load balancer, for redundancy and scale. `room_locks` is just a variable sitting in the memory of *one* of those ten processes. Server #3 has never heard of the lock Server #7 is holding. Alice's request lands on Server #3, Bob's lands on Server #7 — both sail through their own local locks, completely unaware the other exists, and you're right back to the double booking, just with extra, useless code in between.

This is the core lesson of Part 2: **a lock is only as strong as the scope everyone agrees to respect it in.** A lock that lives inside one process's memory can't coordinate across processes — and a real booking system is never just one process.

---

That's the trap most people fall into on their first attempt: check-then-act in code, then "just add a lock" without realizing *where* that lock needs to live to actually mean anything.

Does the failure mode of the in-memory lock make sense — specifically *why* scaling to multiple servers is what breaks it? Once that's solid, Part 3 is where we bring in the database itself as the one thing *all* those servers actually share, and how it can be used to close this gap for real (pessimistic locking, `SELECT FOR UPDATE`, and the trade-offs that come with it).

---

## Part 3: The One Thing Everyone Actually Shares — The Database

Here's the insight that has to click before anything else makes sense: those ten web servers don't agree on anything — except one thing. They all talk to the **same database**. That's not an implementation detail, that's the whole point of having a database instead of ten separate spreadsheets. So if you want a lock that means something across every server, it can't live in any server's memory — it has to live in the one place they all touch.

This gives us two genuinely different philosophies for closing the gap. Let's take them one at a time, because they solve the problem in opposite ways.

### Strategy 1: Pessimistic locking — "assume a collision is coming, so block it"

The idea: when Alice's request goes to check Room 101, make the *database itself* lock that row so nobody else can even *read* it for updating until Alice is done.

```sql
BEGIN TRANSACTION;

SELECT * FROM bookings
WHERE room_id = 101 AND date = '2026-09-04'
FOR UPDATE;                      -- ← this is the whole trick

-- if the SELECT returned nothing:
INSERT INTO bookings (room_id, date, guest) VALUES (101, '2026-09-04', 'Alice');

COMMIT;
```

`FOR UPDATE` tells the database: "I'm about to act on what I just read — don't let anyone else read this row for writing purposes until I commit or rollback." Now replay the race:

| Time | Request A (Alice) | Request B (Bob) |
|------|--------------------|--------------------|
| t0 | `SELECT ... FOR UPDATE` → gets the lock, sees nothing | |
| t1 | | `SELECT ... FOR UPDATE` → **blocks**, waits for A's lock |
| t2 | `INSERT`, `COMMIT` → lock released | |
| t3 | | Now unblocks, `SELECT` re-runs → sees Alice's row exists |
| t4 | | Returns "Room already booked" |

Bob's request doesn't fail — it just *waits its turn*, exactly like the phone ringing while the single clerk finishes writing in the ledger. We've recreated the old single-clerk serialization, except now it's enforced by the database instead of by there only being one human. This is genuinely correct. It fully closes the gap from Part 1 and Part 2.

**So why isn't this just "the answer" and end of story?** Two costs:

1. **Throughput.** Every other request wanting *that same room+date* has to literally sit and wait, holding a database connection open, doing nothing. Fine for a boutique hotel. Very not-fine for something like Ticketmaster, where 50,000 people might hit "buy" on the same concert's front-row seat within the same second — you'd have a queue 50,000 deep all blocked on one lock.
2. **Deadlocks.** If a request needs to lock *two* rows (say, a bundled hotel+flight booking) and another request locks them in the opposite order, both can end up waiting on each other forever. Databases detect this and kill one transaction, but now your app needs retry logic anyway.

### Strategy 2: Let the database's own uniqueness rules be the lock

There's a cheaper trick that solves the *exact same race* for the common case, using something you probably already think of as unrelated: a **unique constraint**.

```sql
CREATE TABLE bookings (
    room_id INT,
    date DATE,
    guest TEXT,
    UNIQUE (room_id, date)          -- ← this line does the work
);
```

Now the code doesn't check-then-insert at all. It just *tries* to insert, and lets the database reject the second one:

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

Replay the race one more time:

| Time | Request A | Request B |
|------|-----------|-----------|
| t0 | `INSERT` Alice → succeeds | |
| t1 | | `INSERT` Bob → **rejected** by the unique constraint |

Notice what disappeared: there's no `SELECT` at all anymore, no explicit lock, no waiting. Both requests just *try* the write, and the database — which enforces uniqueness at the storage-engine level, far below SQL — guarantees only one can win. This sidesteps the whole check-then-act gap by never checking in the first place. You just attempt, and the database is the single source of truth on who was first.

This is quietly one of the most important ideas in this whole topic: **the safest lock is often not a lock you asked for — it's a guarantee the storage engine gives you for free if you shape your schema right.**

---

Two real strategies now on the table: **pessimistic locking** (block and wait, `FOR UPDATE`) and **constraint-based locking** (attempt and let uniqueness reject). Next up in Part 4 is the third major strategy — **optimistic locking** (version numbers, "check nobody changed this while I was thinking" — used heavily for things like *changing* a booking, not just creating one) — plus the real guarantees word: what "ACID" and "isolation levels" actually promise you here, and where they quietly *don't* protect you even when you think they do.

Make sense so far — particularly why the unique-constraint approach is cheaper than `FOR UPDATE` but only works for the "create" case, not for later modifications?

---

## Part 4: Optimistic Locking, and What "ACID" Actually Promises You

Quick reason we need a *third* strategy: the unique constraint trick from Part 3 is brilliant for *creating* a booking — but it only works because "two rows with the same room+date" is something the database can mechanically detect as a duplicate. What about when there's no duplicate to catch? Say, Alice is *editing* an existing reservation — changing her checkout date, or a hotel admin is decrementing "rooms available" from 3 to 2. There's no INSERT to collide on. Both people are *updating* the same existing row. Let's see the race show up in this new shape.

### The "lost update" problem

Say a room-inventory row looks like this: `room_type='Deluxe', date='2026-09-04', available=3`. Two booking requests come in at nearly the same instant, each trying to book one Deluxe room.

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

| Time | Request A | Request B |
|------|-----------|-----------|
| t0 | Reads `available = 3` | |
| t1 | | Reads `available = 3` |
| t2 | Computes `3 - 1 = 2`, writes `available = 2` | |
| t3 | | Computes `3 - 1 = 2`, writes `available = 2` |

Both requests wrote `2`. But two rooms were actually sold — the real value should be `1`. One of those decrements just vanished into thin air. This is called a **lost update**, and notice it's the *same disease* as double booking (read-then-write gap), just wearing a different costume — instead of two guests in one room, it's inventory silently drifting wrong until, weeks later, the hotel has sold more Deluxe rooms than exist.

### Optimistic locking: "assume no collision, but verify before committing"

The idea, in contrast to pessimistic locking (which blocks *up front*): let everyone read and compute freely, but attach a **version number** or the last-known value to the update, and make the write itself conditional on nothing having changed since you read it.

```sql
-- Add a version column
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
        return "Retry"   # someone else updated it first — version didn't match
    return "Booked"
```

Replay it:

| Time | Request A | Request B |
|------|-----------|-----------|
| t0 | Reads `available=3, version=5` | |
| t1 | | Reads `available=3, version=5` |
| t2 | `UPDATE ... WHERE version=5` → matches, succeeds. Row is now `available=2, version=6` | |
| t3 | | `UPDATE ... WHERE version=5` → **no rows match** (version is now 6) → 0 rows affected |
| t4 | | App sees `rows_affected == 0`, returns "Retry" |

Bob's request doesn't block like it would with `FOR UPDATE` — it just runs, fails cheaply, and the *application* decides what to do (retry the whole read-compute-write cycle, or tell the user "seat just got taken, please pick again"). This is why it's called *optimistic*: you bet that collisions are rare, so you don't pay the cost of locking every time — you only pay a retry cost on the rare occasions you were wrong. That trade-off is exactly the opposite bet that pessimistic locking makes, and which one is "correct" depends entirely on how contended the resource is — more on that when we get to Ticketmaster.

---

### Now — what does "ACID" actually guarantee you here?

You'll hear "just use a transaction, it's ACID" thrown around like it magically fixes all of this. It doesn't, automatically — and here's exactly where it does and doesn't help.

- **Atomicity** — a transaction's writes either *all* happen or *none* happen. Great, but this says nothing about *timing* between two separate transactions. Atomicity alone does not stop the lost update above — both transactions were individually atomic, and both still lost data.
- **Consistency** — the database won't let a transaction violate constraints you've defined (like `UNIQUE`). This is *exactly* what saved us in Part 3 — but only because we defined a constraint that happened to encode our business rule. Consistency doesn't protect rules the database doesn't know about, like "available can't go negative" unless you add a `CHECK (available >= 0)` constraint yourself.
- **Isolation** — this is the one that actually governs races, and it's the trickiest, because **isolation is not one fixed guarantee — it's a *dial*, called the isolation level**, and most databases default to a *weak* setting:
  - `READ COMMITTED` (Postgres's default): you never read uncommitted data from someone else, but two separate reads in your own transaction can still see different values if someone else committed in between. This does **not** prevent the lost-update race above.
  - `REPEATABLE READ`: your transaction sees a consistent snapshot for its whole duration — but depending on the database, this *still* doesn't stop lost updates in all cases (this is a genuinely famous gotcha in Postgres specifically).
  - `SERIALIZABLE`: the database guarantees the outcome is *as if* all transactions ran one after another, never interleaved. This is the only level that fully closes the gap without you doing anything manual — and it's also the slowest, because under the hood it's often just detecting conflicts and forcing retries, similar in spirit to what we did by hand with the `version` column.
- **Durability** — once committed, it survives a crash. Real, important, but unrelated to races entirely.

So the honest summary: **"ACID" doesn't save you by default.** Most production databases run at `READ COMMITTED` because `SERIALIZABLE` is too slow for general traffic — which means *you*, the developer, are responsible for explicitly reaching for `FOR UPDATE`, a unique constraint, or a version column, depending on the shape of the operation. ACID gives you the *tools* to be safe; it doesn't make you safe by default.

---

That's the theory core: three strategies (pessimistic / constraint-based / optimistic), and what ACID's isolation levels actually do and don't hand you. Next, Part 5 is where this gets genuinely interesting: **how real companies chose differently** — why Ticketmaster/ticketing systems lean toward short-lived pessimistic "holds," why airlines historically *overbook on purpose* as a business decision rather than a bug, and why systems like Booking.com lean optimistic with compensating cancellations. That's also where new problems this introduces show up — like the "zombie cart" problem (someone locks a seat and then abandons checkout).

Does the ACID/isolation-level piece land — especially that `READ COMMITTED` (the common default) does *not* save you from the lost update by itself?

---

## Part 5: How Real Companies Actually Chose Differently — And the New Problems Each Choice Created

Here's where theory meets business reality. Every company we're about to look at *knows* all three strategies from Parts 3–4. The one they picked wasn't about which is "more correct" — it was about their specific shape of contention, and each choice dragged in a brand new problem nobody saw coming until it hit them.

### Ticketmaster / event ticketing: pessimistic locking, but *time-boxed*

Picture a stadium-seating map for a huge concert. 50,000 people are refreshing the seat map the second tickets go live. If Ticketmaster used the constraint-based approach from Part 3 (let everyone try, reject the losers), you'd get 49,999 people simultaneously slamming `INSERT` at the database for the same handful of good seats — brutal write contention, and worse, a *terrible user experience*: you click a seat, fill in your card details for two minutes, hit submit, and only *then* find out it's gone. Nobody wants that.

So ticketing systems reach for pessimistic locking — but with a twist the textbook version doesn't have: **the lock isn't held for the length of one SQL transaction, it's held for the length of a human filling out a checkout form.** That's minutes, not milliseconds. You can't hold a database row lock (`FOR UPDATE`) open for two minutes — it would grind the whole system to a halt (remember the throughput cost from Part 3). So instead:

```
1. User clicks seat → system writes a "hold" record:
   seat_id=A12, status='held', held_by=user123, expires_at=now()+120s
   (this write itself uses a unique constraint, like Part 3, so only
   one hold can exist per seat)
2. Seat now shows as unavailable to everyone else
3. User has 120 seconds to complete checkout
4. If they pay in time → hold converts to a real booking
5. If the timer expires → a background job deletes the hold, seat reopens
```

This is pessimistic locking *conceptually* (block others out immediately) implemented *without* an actual database lock (use a row + a TTL instead, so it doesn't choke the database). It's a really elegant reframing — but look what it just introduced:

**The zombie cart / abandoned hold problem.** What happens if the user closes the tab instead of letting the timer run out? What if the background expiry job itself crashes or lags? Now a seat sits "held" by nobody, unavailable to the 49,999 other people, for longer than intended — pure lost inventory during the highest-demand seconds of the sale. This is a genuinely new problem that *pessimistic locking with humans in the loop* creates, and it's why real implementations pair the hold with aggressive, redundant expiry mechanisms (e.g., a message queue with a delayed message that deletes the hold, *independent* of any single background job's uptime) — trading one race condition for a much smaller, much less catastrophic one (a seat being unavailable for an extra few seconds vs. being sold twice).

### Airlines: overbooking is not a bug — it's a deliberate business decision

This one surprises people the first time they hear it. Airlines *do* use strong consistency internally to prevent literally selling seat 14C to two different people. But they routinely sell **more tickets than seats exist** on the plane — on purpose. Why? Because historically, a predictable percentage of passengers don't show up (no-shows, missed connections), and an empty seat is pure lost revenue that can never be recovered once the door closes. So airlines run statistical models predicting the no-show rate for a given route and time, and deliberately oversell by that margin.

Notice this is a *completely different category of solution* from everything in Parts 1–4. It's not a race condition being tolerated by accident — it's the airline choosing to accept a *known, bounded* rate of "double booking" as a business trade-off, then handling the rare case where it goes wrong (everyone *does* show up) with a completely separate system: compensation, bumping passengers, rebooking. This is worth sitting with, because it reframes the whole topic — **not every instance of "two people, one seat" is a bug to be engineered away. Sometimes it's cheaper to occasionally pay $800 and a hotel voucher to a bumped passenger than to leave that revenue on the table on every single flight.**

### Booking.com / hotel aggregators: optimistic, with compensating cancellations

Now the hardest case. Booking.com doesn't own the hotel's inventory system — it's showing you a hotel's availability that's *also* being sold through five other channels (the hotel's own front desk, Expedia, Airbnb-for-hotels, a phone reservation line) simultaneously. There is no single database Booking.com can put a `FOR UPDATE` lock on, because the actual source of truth is the hotel's own property-management system, which Booking.com only talks to through periodic sync calls (webhooks, polling APIs) — not a live shared transaction.

This means pessimistic locking is architecturally *impossible* here — you cannot lock a resource you don't control and don't have a live connection to at the moment of decision. So the entire industry defaults to something you might call **"optimistic at internet scale"**:

```
1. Show availability based on the last sync (could be minutes stale)
2. Accept the booking request optimistically
3. Immediately fire a confirmation request to the hotel's system
4. If the hotel's system says "actually, no, that room's gone"
   → this is now a REAL double booking that happened in production
5. Trigger a compensating workflow: apologize, refund, and often
   rebook the guest at an equal-or-better room at Booking.com's own
   cost, to protect the customer relationship
```

This is a genuinely new category compared to everything before it: instead of *preventing* the race with a database mechanism, the system **accepts that the race will sometimes be lost, detects it after the fact, and pays to make it right.** It's optimistic locking's version-mismatch idea (Part 4), except the "retry" isn't a quick re-read of a row — it's a customer service and refund pipeline, because the two "transactions" involved (Booking.com's system and the hotel's front desk) are separated by network calls, not by microseconds inside one database.

---

**The pattern across all three**, if you zoom out: the "right" strategy is a function of

- how many people are contending for the *same* resource at the *same instant* (Ticketmaster: extreme → pessimistic hold)
- whether the "cost" of a rare failure is cheap to absorb (airlines: yes, a voucher → deliberately allow it)
- whether you even have transactional access to the source of truth (Booking.com: no → optimistic + compensate)

Every one of these choices then created its *own* new problem: zombie holds, angry bumped passengers, and refund/compensation pipelines respectively. None of them are "solved" in some final sense — they're each a different point on the same trade-off curve between *correctness*, *speed*, and *who owns the source of truth*.

---

That covers the "why did company X do it differently" angle. There's one more piece worth going deep on if you want it: **distributed locks** — what happens when the "shared database" from Part 3 isn't even one database anymore, but a sharded/microservices system where inventory for one hotel chain might live across multiple services (pricing, inventory, payments), and how things like Redis-based locks (Redlock), or event-driven **sagas** with compensating transactions, get used when there's no single database to rely on at all.

Want to go there next, or is this a good place to consolidate what we've covered so far?

---

## Part 6: When There's No Single Database Left — Sagas, Redlock, and the Microservices Version of This Problem

Everything so far assumed one crucial thing: somewhere, a *single* database was the ultimate referee — either directly (Parts 3–4) or as a source of truth Booking.com syncs against (Part 5). Now let's break that assumption on purpose, because this is exactly what happens once a booking system grows into microservices.

### How the problem re-appears in a new shape

Imagine the hotel chain's engineering has grown up. Booking a room is no longer "one API hitting one database" — it's now three separate services, each owning its own database, that all have to agree:

```
Inventory Service   — owns "is this room available"
Pricing Service      — owns "what does this cost, apply promo codes"
Payment Service      — owns "charge the customer's card"
```

A booking now looks like: check inventory → reserve it → calculate price → charge card → confirm. That's five steps across three separate databases that **cannot share a single SQL transaction**, because a transaction can't span two different database servers (not without something exotic like two-phase commit, which most companies avoid — more on why in a second).

So immediately, two new failure shapes appear that didn't exist before:

1. **The distributed race**: two requests could still both pass the Inventory Service's check at nearly the same time, same as Part 2 — except now "the database" isn't one thing, it's a service you call over the network, with its own latency, and possibly its own replica lag.
2. **The partial failure**: Inventory Service successfully reserves the room, Pricing Service succeeds, but Payment Service's card charge *fails* (card declined, timeout). Now the room is marked reserved forever, for a booking that never actually completed. No database `ROLLBACK` can undo this, because the "transaction" was never a single transaction — it was three independent commits in three independent systems.

### Attempt 1: Distributed locks (Redlock) — pessimistic locking, stretched across services

The direct instinct: "we used `FOR UPDATE` inside one database — let's build the equivalent that works *across* multiple services." This is what tools like Redis-based **Redlock** try to do: acquire a lock (e.g., `lock:room:101:2026-09-04`) in a shared, fast, external system (Redis) *before* touching any of the three services, do all the work, then release it.

```
1. Try to acquire lock "room:101:2026-09-04" in Redis, TTL = 10s
2. If acquired: call Inventory → Pricing → Payment in sequence
3. Release the lock (or let the TTL expire as a safety net)
```

This works — *in the common case*. But it inherits Part 3's throughput cost (everyone else waits) plus a genuinely new problem: **what if the process holding the lock crashes mid-way**, after reserving inventory but before charging payment, and the TTL hasn't expired yet? Now nobody else can even *try* to book that room for up to 10 seconds, and the room is stuck half-reserved. Worse, if your TTL is too short, the lock can expire *while you're still legitimately working* (e.g., a slow payment gateway response), and a second process grabs the same "lock" you still think you're holding — meaning your "safe" lock wasn't actually mutually exclusive at all. This exact flaw is famous enough that it has a name in distributed-systems circles (it's the core of the well-known Redlock correctness debate between Redis's creator and distributed-systems researcher Martin Kleppmann) — locks with timers are inherently a compromise, not a hard guarantee, because you're trying to reason about mutual exclusion across machines that don't share a clock or a guarantee of when the other one will resume running.

### Attempt 2: Don't lock across services at all — use a Saga instead

The alternative philosophy: stop trying to fake one big transaction across three databases. Instead, treat it as a **sequence of local transactions, each with a defined "undo" step**, and if any step fails, walk backward undoing what already succeeded. This pattern is called a **saga**.

```
Step 1: Inventory Service reserves room 101       → succeeds
Step 2: Pricing Service locks in the price          → succeeds
Step 3: Payment Service charges the card            → FAILS (declined)

→ Saga now runs compensating actions in reverse:
  Undo Step 2: Pricing Service releases the locked-in price
  Undo Step 1: Inventory Service un-reserves room 101
```

Each service only ever runs a normal local transaction against its own database (so all the guarantees from Part 4 — ACID, version columns, `FOR UPDATE` — still apply *within* each step). What's new is the **compensating action** — every step that changes state must also define how to undo itself, because there is no global rollback anymore. This is conceptually the exact same idea as Booking.com's "detect the failure after the fact and pay to fix it" from Part 5 — except instead of a human customer-service workflow, it's an automated, code-defined undo chain.

The saga pattern has its own sharp edge though: **the window between Step 1 succeeding and Step 3 failing is a window where the room *looks* reserved to everyone else, even though the booking will ultimately fail.** If that window is long (payment gateways can be slow), you're temporarily "locking out" other customers from a room that will end up free again seconds later — a milder, automated cousin of the zombie-hold problem from Ticketmaster in Part 5.

### The real pattern underneath all of this

By now you can probably feel the shape repeating across every part of this story:

- Single clerk (Part 1): perfect correctness, because concurrency literally didn't exist.
- Naive code (Part 2): tried to copy that correctness without noticing concurrency now existed underneath.
- One shared database (Part 3–4): regained correctness by pushing the coordination into the one thing everyone truly shares — using either blocking (pessimistic), a mechanical guarantee (constraints), or check-on-write (optimistic).
- Multiple companies (Part 5): once even the database wasn't fully shared or fully live, correctness got traded for *speed* or *business economics*, and failures got handled after the fact instead of prevented before the fact.
- Multiple services (Part 6): once there wasn't *any* single shared thing left, the strategies became either "build a fake shared thing" (distributed lock, with its own crash/clock problems) or "give up on atomicity entirely and make every failure reversible" (sagas).

There's no final answer that makes this problem "go away" — every layer of scale just moves you to a different point on the same trade-off between **strict correctness, system throughput, and how expensive a rare mistake is to clean up afterward.** That's genuinely the whole concept, top to bottom.

---

That's the full arc — root cause through microservices. Want me to pull this into a single consolidated reference doc (all six parts, examples included) as an artifact you can keep and reference later, or is holding it in the conversation enough for now?

---

Great question — this is the piece that ties Parts 3 and 4 together properly, because isolation levels and locks aren't two separate mechanisms, they're two views of the same thing. Let me unpack how.

## The core idea: isolation levels are what *decide* whether a lock gets taken at all

Every SQL statement runs *inside* a transaction, and every transaction runs at some isolation level. That level isn't just a label — it directly controls **what locks the database takes, how long it holds them, and what a concurrent transaction is allowed to see while those locks are held.** So "use `FOR UPDATE`" from Part 3 wasn't independent of isolation levels — it was one specific tool that behaves differently depending on which level you're running under.

Databases implement this two different ways, and it matters which one you're using:

- **Lock-based (2PL — two-phase locking)**: used by SQL Server, MySQL/InnoDB in some modes. Reads can take *shared locks*, writes take *exclusive locks*, and higher isolation = more locks held for longer.
- **MVCC (multi-version concurrency control)**: used by Postgres, Oracle, InnoDB by default. Readers don't block writers and vice versa — instead, each transaction sees a *snapshot* of the data as of some point in time, and the database keeps multiple versions of each row around. Locks still exist here too (for writes), but reads mostly avoid them.

Let's go level by level, because each one changes the actual mechanics.

## Read Uncommitted — almost no locking, almost no safety

```sql
SET TRANSACTION ISOLATION LEVEL READ UNCOMMITTED;
```

Reads don't take locks, and they don't respect *other* transactions' locks either — you can read a row another transaction has modified but not yet committed. This is called a **dirty read**.

Booking example: imagine Request A starts inserting Alice's booking but hasn't committed yet. Under `READ UNCOMMITTED`, Request B's `SELECT` could see Alice's row *before* it's confirmed — and act on data that might get rolled back a moment later (say, A's transaction fails and rolls back). B just made a decision based on a booking that never actually happened. Almost nobody uses this level for anything transactional; it exists mainly for read-only analytics where a little staleness doesn't matter.

## Read Committed — the common default (Postgres, SQL Server)

```sql
SET TRANSACTION ISOLATION LEVEL READ COMMITTED;
```

Now reads only ever see committed data — dirty reads are gone. Mechanically:

- **Lock-based engines**: a `SELECT` takes a brief shared lock just long enough to read, then releases it immediately — it does *not* hold the lock for the rest of the transaction.
- **MVCC engines (Postgres)**: each individual statement gets its *own* fresh snapshot of "everything committed as of right now." Two `SELECT`s in the same transaction, seconds apart, can see different data if someone else committed in between.

This is exactly why, in Part 4, `READ COMMITTED` didn't save you from the lost update: your `SELECT available FROM inventory` read a value, but by the time your `UPDATE` runs, nothing forced the database to check *whether that value is still true* — it just overwrites blindly. `READ COMMITTED` guarantees you never read garbage; it says nothing about whether that data is still accurate by the time you act on it.

**Where `FOR UPDATE` becomes essential here**: under `READ COMMITTED`, `SELECT ... FOR UPDATE` is what forces a *real, held* exclusive lock on the row, blocking anyone else's `FOR UPDATE` (or write) on that same row until your transaction ends. That's the mechanism from Part 3 — you're manually asking for stronger locking than the isolation level gives you by default.

## Repeatable Read — a consistent snapshot for the whole transaction

```sql
SET TRANSACTION ISOLATION LEVEL REPEATABLE READ;
```

Now every read *within one transaction* sees the same snapshot — as if the transaction took a photograph of the database the moment it started, and kept looking at that photo for every subsequent read, no matter what anyone else commits in the meantime.

- **Lock-based engines**: typically hold shared locks on every row you've read until the transaction ends, preventing others from modifying rows you've touched.
- **Postgres (MVCC)**: doesn't lock rows on plain reads at all — it just serves you your snapshot. This is a meaningfully different mechanism from the lock-based version, and it produces a famous, specific gotcha:

**The Postgres `REPEATABLE READ` lost-update gotcha**, replaying our inventory example:

| Time | Txn A (REPEATABLE READ) | Txn B (REPEATABLE READ) |
|---|---|---|
| t0 | `SELECT available` → sees `3` (snapshot taken) | |
| t1 | | `SELECT available` → sees `3` (own snapshot) |
| t2 | `UPDATE ... SET available=2` → commits fine | |
| t3 | | `UPDATE ... SET available=2` → **here Postgres actually detects this and throws a serialization error**, forcing B to retry |

Postgres is smart enough at `REPEATABLE READ` to detect that B's `UPDATE` is based on a row that's since changed, and it aborts B rather than silently losing the update — but notice **the correctness now depends on your app catching that error and retrying**, it's not silent safety, it's a thrown exception you must handle. And on other databases (or with certain query shapes even in Postgres, like updating based on an *aggregate* rather than a single row), this protection doesn't kick in the same way — this is why it has a reputation as "the isolation level that looks safer than it is." The reliable fix inside a `REPEATABLE READ`/`READ COMMITTED` world is still what Part 4 showed you by hand: `FOR UPDATE`, or the explicit `version` column check.

## Serializable — the only level that fully closes the gap, and how it's actually built

```sql
SET TRANSACTION ISOLATION LEVEL SERIALIZABLE;
```

The guarantee: the outcome of running transactions concurrently must be *identical* to some possible order of running them one at a time. This is the only level that, by itself and without you writing any manual locking code, prevents the lost update from Part 4 and the double booking from Part 1–2.

Two very different engines get you here:

- **Strict two-phase locking (2PL)** — the "pessimistic" implementation. Every read takes a lock, every write takes a lock, and *no lock is released until the transaction commits* (that's the "two-phase" part: phase one only acquires locks, phase two — commit — only releases them). This is essentially "treat every `SELECT` as if it were `FOR UPDATE`, automatically." Correct, but this is the throughput cost from Part 3, now applied to *every single read* in the system, not just the ones you explicitly locked.
- **Serializable Snapshot Isolation (SSI)** — what Postgres actually uses. This is optimistic, structurally identical in spirit to Part 4's version-column trick, but done automatically by the engine: transactions run against MVCC snapshots with *no* extra locking, and the database tracks read/write dependencies between concurrent transactions in the background. If it detects a pattern that *could* have produced a non-serializable outcome, it aborts one of the transactions at commit time with a serialization failure — same as the `REPEATABLE READ` example above, but now covering cases that level misses (like updates based on aggregates, or multi-row invariants).

So concretely, for our booking system: if you ran the naive check-then-insert code from Part 2 under `SERIALIZABLE`, the database itself would catch the race — one of the two transactions would get a serialization failure and be forced to retry, rather than silently succeeding twice. You get Part 3's correctness *without* writing `FOR UPDATE` yourself — but you inherit either 2PL's throughput cost, or SSI's need for your app to catch and retry serialization failures (which, notably, is the exact same retry-logic requirement optimistic locking already demanded of you in Part 4 — `SERIALIZABLE`/SSI is essentially optimistic locking generalized and automated across your whole query, instead of one column you defined by hand).

## Tying it back into one table

| Isolation level | Dirty reads | Lost updates | Mechanism (Postgres/MVCC) | Mechanism (lock-based) |
|---|---|---|---|---|
| Read Uncommitted | Possible | Possible | rarely implemented as truly uncommitted in MVCC engines | no locks on read |
| Read Committed | Prevented | Possible | fresh snapshot per statement | shared lock per statement, released immediately |
| Repeatable Read | Prevented | Sometimes caught (Postgres), often not (generally) | one snapshot for whole transaction | shared locks held for whole transaction |
| Serializable | Prevented | Prevented | SSI — snapshot + dependency tracking + abort-on-conflict | strict 2PL — lock everything, hold until commit |

**The one-sentence version to keep**: isolation levels are the *policy*, locks (or MVCC snapshot/conflict-tracking) are the *mechanism* that enforces that policy — and `FOR UPDATE`, unique constraints, and version columns from Parts 3–4 are all ways of manually buying yourself stronger guarantees than your database's *default* isolation level gives you, without paying the full cost of running everything at `SERIALIZABLE`.

Want to go deeper on any one piece of this — e.g., actually watching write skew happen (a `SERIALIZABLE`-only anomaly that `REPEATABLE READ` completely misses, and a classic one for booking-adjacent systems like overlapping reservation ranges), or move on?

