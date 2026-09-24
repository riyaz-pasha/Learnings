# 🎬 Designing a Ticket Booking System — The Story Way

Let's begin at the very beginning. Before any system, there was a **problem.**

---

## Chapter 1: The Problem — "The Queue Outside the Theatre"

### 🕰️ 1990s — The Real World Problem

Imagine it's 1995. A new Shah Rukh Khan movie is releasing. You want a ticket.

What do you do?

You **wake up at 5 AM**, stand in a serpentine queue outside the cinema, wait 3 hours, and maybe — *just maybe* — get a ticket. Sometimes you'd reach the counter and hear:

> *"Sorry, houseful."*

3 hours. Wasted.

**The problems were clear:**
- You had no idea how many seats were left
- Multiple people were racing for the same seat
- No way to book in advance from home
- Scalpers would buy in bulk and resell at 3x price

---

### 💻 Early 2000s — "Let's put it on the Internet!"

Someone smart said: *"Why not let people book online?"*

So they built the first version. Simple. Naive. But it worked... until it didn't.

Here's what the first version looked like:

```
User → Web Server → Database
```

A single web server. A single database. User picks a seat, it gets saved. Done.

**This worked fine for... maybe 50 users a day.**

Then came the first big movie release online. 10,000 people tried to book at 10 AM.

The server **crashed.**

But even before the crash — there was a scarier bug.

---

### 😱 The "Double Booking" Nightmare

Two users — Rahul and Priya — both open the app at the same time. Both see **Seat A5 is available.** Both click "Book." Both get a confirmation.

Now two people show up at the theatre with tickets for the **same seat.**

This is the **core problem** of a ticket booking system, and everything we design from here is essentially solving this one problem at scale:

> **How do you ensure that one and only one person gets a seat, even when thousands are trying simultaneously?**

---

### 🧠 So what's the fundamental challenge?

Let's name the enemies clearly:

| Enemy | What it means |
|---|---|
| **Concurrency** | 1000s of users hitting the same seat at the same millisecond |
| **Scale** | Crore+ users, thousands of shows, millions of seats |
| **Availability** | System should never go down, especially during peak load |
| **Consistency** | A seat booked by Person A should NEVER also go to Person B |
| **Latency** | Booking should feel fast. Nobody waits 10 seconds |

---

### ✅ Before we move forward — let's lock in the scope

In an interview, the **first thing you do** is define scope. You ask:

- Are we building booking + payments + notifications?
- How many cities? How many concurrent users?
- Do we need a waiting list?
- Mobile + Web both?

For our system, let's define:

**Functional Requirements:**
1. Users can browse movies, shows, theatres
2. Users can select seats and book tickets
3. Payment processing
4. Booking confirmation via email/SMS

**Non-Functional Requirements:**
1. 10 million users/day, with **spikes of 1 lakh concurrent users** during big releases
2. A seat, once booked, must **never be double-booked** (strong consistency here)
3. System should be available **99.99%** of the time
4. Booking confirmation within **3 seconds**

---

### 🗺️ The 10,000 foot view (before going deep)

At the highest level, the system needs these building blocks:

```
[User] 
  ↓
[Load Balancer]
  ↓
[Web/App Servers]
  ↓              ↓
[Seat Service]  [Payment Service]  [Notification Service]
  ↓
[Database]
```

We'll zoom into every single box. But we'll do it **one story at a time.**

---

## 🎯 Where do you want to go next?

We've established *why* the problem is hard. Now we can go deeper. Here's what's coming next, in order:

1. **The Seat Locking Problem** — How do we prevent double booking? (Optimistic vs Pessimistic locking)
2. **The Database Design** — What tables do we need? How do we model seats, shows, bookings?
3. **Scaling the reads** — Caching with Redis (why browsing is easy, booking is hard)
4. **Scaling the writes** — Queues, rate limiting during Avengers-level traffic
5. **Sharding the Database** — When one DB isn't enough
6. **Replication** — Keeping data safe and reads fast
7. **Payment & Consistency** — The saga pattern
8. **Notifications** — Async architecture
9. **Failure handling** — What if the server dies mid-booking?

---

**Shall we go to Chapter 2: The Seat Locking Problem?** 

This is the most important and most commonly asked concept in booking system interviews. It's where the real engineering begins. 🚀

---

# Chapter 2: The Seat Locking Problem

## 🔁 Recap of where we are

Rahul and Priya both see Seat A5 as available. Both click Book. Both get confirmed. Two people, one seat. **Chaos.**

The engineers sat down and asked: *"How do we make sure only ONE person can claim a seat at a time?"*

They came up with approaches. Each one taught them something. Let's walk through each.

---

## Attempt 1 — "Just check before saving" 🤦

The first instinct was obvious. Before saving a booking, **check if the seat is already taken.**

```
1. User clicks "Book Seat A5"
2. Server checks DB → "Is A5 booked?" → NO
3. Server saves → "A5 is now booked by Rahul"
```

Simple. Clean. **Completely broken.**

Here's why. Imagine this happening at the **exact same millisecond:**

```
Time ──────────────────────────────────────────────►

Rahul's request:   [Check A5? → FREE]......[Save A5 = Rahul] ✅
Priya's request:        [Check A5? → FREE]......[Save A5 = Priya] ✅

                         ↑
              Both checked BEFORE either saved.
              Both saw FREE. Both saved. 💥 Double booking.
```

This is called a **Race Condition.** The check and the save are two separate operations. In between those two operations, the world can change.

The engineers realized:

> *"The check and the save must be ONE atomic operation. Nothing should be able to sneak in between."*

This led them to the concept of **Locking.**

---

## Attempt 2 — Pessimistic Locking 🔒

### The Story

Think of it like a **fitting room in a clothing store.**

When you enter the fitting room, you lock the door. Nobody else can come in. You try the outfit. You either buy it or you leave. Only then does the next person get in.

**Pessimistic Locking works the same way:**

> *"I'm going to assume the worst — someone WILL try to steal this seat. So I'll lock it the moment I touch it."*

### How it works in the DB

```sql
BEGIN TRANSACTION;

-- This locks the row. Nobody else can read/write it.
SELECT * FROM seats 
WHERE seat_id = 'A5' AND show_id = '123' 
FOR UPDATE;          -- ← This is the lock

-- If status is AVAILABLE, mark it as BOOKED
UPDATE seats 
SET status = 'BOOKED', user_id = 'rahul_id'
WHERE seat_id = 'A5' AND show_id = '123';

COMMIT;
```

`SELECT ... FOR UPDATE` tells the database:

> *"I'm about to update this row. Lock it. If anyone else tries to touch it, make them wait."*

### What Priya sees

```
Rahul:  [Locks A5] → [Checks → FREE] → [Books A5] → [Releases Lock]
Priya:       [Tries to Lock A5] → ⏳ WAITING ⏳ → [Gets Lock] → [Checks → BOOKED] → ❌ Sorry, unavailable
```

**No double booking. Problem solved!** 🎉

...or is it?

---

### 😬 The Problem with Pessimistic Locking

Pessimistic locking works perfectly for **small scale.** But remember our requirement: **1 lakh concurrent users** during a big movie release.

Imagine a stadium. 50,000 seats. And 5 lakh people all trying to book at 10 AM.

Every user who touches a seat **locks a DB row.** While that row is locked, everyone else **waits.**

Here's what happens:

```
User 1 locks Seat A1    → Takes 2 seconds to complete payment
User 2 locks Seat A2    → Takes 2 seconds
...
User 1000 tries Seat A1 → WAITING 2 seconds
User 1001 tries Seat A1 → WAITING 2 seconds
User 1002 tries Seat A1 → WAITING 2 seconds
```

**Problems that emerge:**
- 🐢 The system becomes slow under load — everyone is waiting for locks
- 💀 **Deadlocks** — User A locks Seat 1 and wants Seat 2. User B locks Seat 2 and wants Seat 1. Both wait forever.
- 📉 DB connections pile up waiting for locks — eventually the DB **dies**
- 🔥 During peak traffic (Avengers release), the system **falls over exactly when you need it most**

The engineers thought:

> *"Locks are expensive. What if we only lock when there's actually a conflict? Most of the time, two people aren't going for the exact same seat."*

This led to **Optimistic Locking.**

---

## Attempt 3 — Optimistic Locking 🤞

### The Story

Think of it like **Google Docs.**

Two people open the same document. Both edit it. When Person B tries to save, Google Docs says:

> *"Wait — Person A already changed this. Here's the conflict. Resolve it."*

Nobody was locked out. They just **detect the conflict at the end** and handle it then.

**Optimistic Locking works the same way:**

> *"I'm going to assume the best — probably nobody else is touching this seat. I'll go ahead, and only at the last moment will I check if someone beat me to it."*

### How it works — The Version Number Trick

Every row in the DB has a **version number.**

```sql
seats table:
| seat_id | show_id | status    | version |
|---------|---------|-----------|---------|
| A5      | 123     | AVAILABLE | 7       |
```

Here's the flow:

```
1. Rahul reads Seat A5 → gets status=AVAILABLE, version=7
2. Priya reads Seat A5 → gets status=AVAILABLE, version=7

3. Rahul tries to update:
   UPDATE seats 
   SET status='BOOKED', version=8 
   WHERE seat_id='A5' AND version=7  ← "Only update if nobody changed it"
   
   → version was still 7 → UPDATE succeeds → 1 row affected ✅

4. Priya tries to update:
   UPDATE seats 
   SET status='BOOKED', version=8 
   WHERE seat_id='A5' AND version=7  ← "Only update if nobody changed it"
   
   → version is now 8, not 7 → UPDATE fails → 0 rows affected ❌
   → Priya gets: "Sorry, seat just got taken. Please choose another."
```

### The key insight

```
No waiting. No locking. Just:
"I'll try. If someone beat me, I'll know immediately and react."
```

### Optimistic vs Pessimistic — When to use which?

| Scenario | Use This | Why |
|---|---|---|
| **High contention** (same seat, many users) | Pessimistic | Too many retries with optimistic |
| **Low contention** (different seats, many users) | Optimistic | No need to lock, faster |
| **Long transactions** (payment takes 5 sec) | Optimistic | Don't hold DB lock during payment |
| **Short transactions** (< 100ms) | Pessimistic | Lock duration is tiny, no big deal |

For a ticket booking system — **Optimistic locking wins** for most cases, because most users are picking **different seats.** The chance of two people fighting for the exact same seat at the exact same millisecond is low.

But there's still one big problem neither approach solves...

---

## Attempt 4 — The "Temporary Hold" System ⏱️

### The Real World Problem Optimistic Locking Doesn't Solve

Think about what actually happens when you book on BookMyShow:

1. You pick Seat A5
2. You fill in your details
3. You enter payment info
4. Payment processes...

**This whole flow takes 3-5 minutes.**

During those 3-5 minutes, **what should happen to Seat A5?**

- If it stays **AVAILABLE** → someone else might book it → you pay → conflict 💥
- If it becomes **BOOKED** → but you haven't paid yet → what if you abandon the page? → seat is gone forever 💀

Neither is acceptable. You need a **third state.**

```
AVAILABLE → HELD (for 10 minutes) → BOOKED
                    ↓
              (if timer expires)
                    ↓
              AVAILABLE again
```

### How BookMyShow actually does this

```
User picks Seat A5
      ↓
System creates a "HOLD" on A5 for THIS user, for 10 minutes
      ↓
Seat shows as UNAVAILABLE to everyone else
      ↓
User completes payment within 10 minutes
      ↓
HOLD → BOOKED ✅

OR

User abandons / payment fails / 10 min timer expires
      ↓
HOLD is released → Seat goes back to AVAILABLE ✅
```

You've seen this countdown timer yourself:

```
⚠️ "Your seats are held for 09:47. Complete payment before time runs out."
```

**That's the HOLD system in action.**

---

### Implementing the Hold — Where does the timer live?

This is a great interview discussion point. You have options:

**Option A — Timer in the Database**
```sql
| seat_id | status | held_by    | held_until          |
|---------|--------|------------|---------------------|
| A5      | HELD   | rahul_id   | 2024-01-01 10:10:00 |
```
A background job runs every minute, finds expired holds, resets them to AVAILABLE.

- ✅ Simple, persistent across server restarts
- ❌ Background job has latency (up to 1 min delay to release)
- ❌ DB gets hammered with polling queries

**Option B — Timer in Redis (The industry approach)**
```
Redis Key:  "hold:show123:seatA5"
Value:      "rahul_id"
TTL:        600 seconds  ← Redis auto-deletes this after 10 mins!
```

When the key expires, a **Redis keyspace notification** fires → your service listens → resets the seat.

- ✅ Sub-millisecond check: "Is this seat held?"
- ✅ Auto-expiry built into Redis, no background job needed
- ✅ Extremely fast reads — checking availability hits Redis, not DB
- ❌ Redis can lose data if it crashes (but we handle this with persistence + DB as source of truth)

---

### The Full Picture Now

```
User clicks Seat A5
      ↓
Check Redis → "hold:show123:seatA5" exists? 
      → YES → "Seat unavailable" ❌
      → NO  → Set Redis key with 10min TTL → "Seat held for you" ✅
                      ↓
              User fills details + pays
                      ↓
              Payment success → Write BOOKED to DB → Delete Redis key
              Payment fail    → Delete Redis key → Seat available again
```

**DB is the source of truth. Redis is the fast, temporary gatekeeper.**

---

## 🧵 The Full Locking Story in One Diagram

```
1000 users hit Seat A5 simultaneously
          ↓
    Redis Check (nanoseconds)
    "Is there a hold?" 
          ↓
   999 users → "YES, held" → redirected to pick other seats
          ↓
    1 user gets through
          ↓
    Optimistic Lock on DB
    "UPDATE where version=X"
          ↓
    Success → Hold created in Redis (10 min TTL)
          ↓
    User goes to payment
          ↓
    Pay → Confirmed → BOOKED in DB ✅
```

---

## 🎯 Interview Cheat Sheet for This Chapter

If an interviewer asks *"How do you prevent double booking?"* — here's the **progression to walk them through:**

1. **Naive approach** — Check then save → Race condition, broken ❌
2. **Pessimistic locking** → Works but kills performance at scale ⚠️
3. **Optimistic locking** → Better, handles most cases ✅
4. **Temporary Hold with Redis TTL** → The real production answer ✅✅

Always mention **why** you moved from one to the next. That's what interviewers love — the *reasoning*, not just the answer.

---

## ✅ Concept Check Before We Move On

Make sure you can answer these confidently:
- What is a race condition and why does "check then save" fail?
- What's the difference between pessimistic and optimistic locking?
- What problem does the "HOLD" state solve that locking alone doesn't?
- Why is Redis better than DB for storing holds?

---

**Ready for Chapter 3: Database Design?** 🗄️

We'll design the actual tables, talk about what columns you need, and then hit the first big scaling question — *"Your DB is getting 10,000 writes/second. What do you do?"*

---

# Chapter 3: Database Design — "The Foundation"

## 🏗️ Why Database Design Matters in Interviews

Before you can talk about sharding, replication, or caching — an interviewer will often ask:

> *"Walk me through your schema."*

If your schema is weak, everything built on top of it is weak. A good schema also **reveals that you understand the domain deeply.** Let's build it the story way.

---

## The Naive First Attempt — "One Big Table"

The first engineer on the team, in a hurry, created this:

```sql
bookings table:
| booking_id | user_name | movie_name | theatre_name | seat | show_time           | amount |
|------------|-----------|------------|--------------|------|---------------------|--------|
| 1          | Rahul     | Pathaan    | PVR Andheri  | A5   | 2024-01-01 10:00:00 | 250    |
| 2          | Priya     | Pathaan    | PVR Andheri  | A6   | 2024-01-01 10:00:00 | 250    |
| 3          | Rahul     | Jawan      | INOX BKC     | B3   | 2024-01-02 14:00:00 | 300    |
```

Looks fine at first glance. But watch what happens when:

- PVR Andheri **changes its name** to PVR Cinemas Andheri → You have to update **every single row** that mentions it 😱
- Pathaan's ticket price changes → Update thousands of rows
- You want to find **all shows of Pathaan across all theatres** → Full table scan 🐢
- The movie name has a **typo** in some rows, different spelling in others → Data inconsistency 💥

This is called **data anomalies** — update anomaly, insertion anomaly, deletion anomaly.

The engineer learned about **Normalization.**

> *"Every piece of information should live in exactly ONE place. Everything else should reference it."*

---

## Building the Schema Right — Entity by Entity

Let's think about the **real world objects** in a ticket booking system and model them one by one.

---

### Entity 1 — Cities 🏙️

```sql
cities
| city_id | name      | timezone        |
|---------|-----------|-----------------|
| 1       | Mumbai    | Asia/Kolkata    |
| 2       | Delhi     | Asia/Kolkata    |
| 3       | Bangalore | Asia/Kolkata    |
```

Simple. Every theatre belongs to a city.

---

### Entity 2 — Movies 🎬

```sql
movies
| movie_id | title    | duration_mins | language | genre   | release_date | rating |
|----------|----------|---------------|----------|---------|--------------|--------|
| 101      | Pathaan  | 146           | Hindi    | Action  | 2023-01-25   | UA     |
| 102      | RRR      | 187           | Telugu   | Drama   | 2022-03-25   | UA     |
| 103      | Jawan    | 169           | Hindi    | Action  | 2023-09-07   | UA     |
```

Movie info lives here **once.** Title changes? Update one row. Done.

---

### Entity 3 — Theatres 🏟️

```sql
theatres
| theatre_id | name           | city_id | address          | total_screens |
|------------|----------------|---------|------------------|---------------|
| 201        | PVR Andheri    | 1       | Andheri West...  | 5             |
| 202        | INOX BKC       | 1       | BKC Complex...   | 8             |
| 203        | PVR Select     | 3       | Orion Mall...    | 6             |
```

Notice `city_id` is a **foreign key** — it references the cities table. No duplication of city info.

---

### Entity 4 — Screens 📽️

A theatre has multiple screens. Each screen has its own seating layout.

```sql
screens
| screen_id | theatre_id | name     | total_seats | screen_type |
|-----------|------------|----------|-------------|-------------|
| 301       | 201        | Screen 1 | 200         | 2D          |
| 302       | 201        | Screen 2 | 150         | IMAX        |
| 303       | 202        | Screen 1 | 300         | 4DX         |
```

---

### Entity 5 — Seats 💺

This is a critical table. Every physical seat in every screen.

```sql
seats
| seat_id | screen_id | row_label | seat_number | seat_type | base_price |
|---------|-----------|-----------|-------------|-----------|------------|
| 4001    | 301       | A         | 1           | REGULAR   | 200        |
| 4002    | 301       | A         | 2           | REGULAR   | 200        |
| 4003    | 301       | A         | 3           | REGULAR   | 200        |
| 4050    | 301       | F         | 5           | RECLINER  | 500        |
```

**"Why store seats as individual rows? That's 200 rows per screen!"**

Great question. Because:
- You need to track the status of **each individual seat per show**
- Different seats have different prices
- Some seats are wheelchair accessible
- You need to know **exactly** which seat is booked, not just "how many"

A screen with 200 seats × 1000 screens = 200,000 rows. That's tiny for a database.

---

### Entity 6 — Shows 🎭

A show = a specific movie, on a specific screen, at a specific date & time.

```sql
shows
| show_id | movie_id | screen_id | show_date  | start_time | end_time | status  | language | format |
|---------|----------|-----------|------------|------------|----------|---------|----------|--------|
| 5001    | 101      | 301       | 2024-01-25 | 10:00      | 12:26    | ACTIVE  | Hindi    | 2D     |
| 5002    | 101      | 301       | 2024-01-25 | 14:00      | 16:26    | ACTIVE  | Hindi    | 2D     |
| 5003    | 102      | 302       | 2024-01-25 | 18:00      | 21:07    | ACTIVE  | Telugu   | IMAX   |
```

**Why store `end_time`?** 
So you can prevent scheduling conflicts — you can't start a new show on Screen 301 at 11:00 if another show ends at 12:26.

```sql
-- Find scheduling conflicts
SELECT * FROM shows 
WHERE screen_id = 301 
AND show_date = '2024-01-25'
AND start_time < '12:26'   -- new show starts before current ends
AND end_time > '10:00'     -- new show ends after current starts
```

---

### Entity 7 — Show Seats 🎯

This is the **most important table** in the whole system.

Here's the problem it solves: The `seats` table stores **physical seats** (seat A5 in Screen 301). But the **availability of that seat changes for every show.**

Seat A5 might be:
- Available for the 10 AM show
- Booked for the 2 PM show
- Held for the 6 PM show

So you need a table that maps **each seat to each show** with its own status:

```sql
show_seats
| show_seat_id | show_id | seat_id | status    | locked_by  | locked_until        | version |
|--------------|---------|---------|-----------|------------|---------------------|---------|
| 1            | 5001    | 4001    | AVAILABLE | NULL       | NULL                | 1       |
| 2            | 5001    | 4002    | HELD      | rahul_uuid | 2024-01-25 10:10:00 | 2       |
| 3            | 5001    | 4003    | BOOKED    | priya_uuid | NULL                | 4       |
| 4            | 5002    | 4001    | AVAILABLE | NULL       | NULL                | 1       |  ← Same seat, different show
```

**See what's happening?**
- `show_id: 5001, seat_id: 4001` = Seat A1 for 10 AM show → AVAILABLE
- `show_id: 5002, seat_id: 4001` = Seat A1 for 2 PM show → AVAILABLE

Same physical seat, tracked **independently per show.**

The `version` column is your **optimistic lock** from Chapter 2. The `locked_by` and `locked_until` are your **HOLD system.**

---

### Entity 8 — Users 👤

```sql
users
| user_id | name  | email           | phone      | created_at |
|---------|-------|-----------------|------------|------------|
| 6001    | Rahul | rahul@email.com | 9876543210 | 2023-01-01 |
| 6002    | Priya | priya@email.com | 9876543211 | 2023-01-02 |
```

---

### Entity 9 — Bookings 🎟️

When payment is complete, a booking record is created.

```sql
bookings
| booking_id | user_id | show_id | total_amount | status    | booked_at           | payment_id |
|------------|---------|---------|--------------|-----------|---------------------|------------|
| 7001       | 6001    | 5001    | 450          | CONFIRMED | 2024-01-25 09:45:00 | pay_abc123 |
| 7002       | 6002    | 5001    | 200          | CONFIRMED | 2024-01-25 09:46:00 | pay_def456 |
```

---

### Entity 10 — Booking Seats 🪑

One booking can have **multiple seats.** A linking table:

```sql
booking_seats
| id  | booking_id | show_seat_id | price_paid |
|-----|------------|--------------|------------|
| 1   | 7001       | 2            | 200        |  ← Seat A5
| 2   | 7001       | 3            | 250        |  ← Seat A6 (recliner)
| 3   | 7002       | 4            | 200        |  ← Seat A7
```

This allows Rahul to book 2 seats in one transaction, each with its own price.

---

## The Complete Schema Map

```
cities
  └── theatres (city_id)
        └── screens (theatre_id)
              └── seats (screen_id)
              └── shows (screen_id + movie_id)
                    └── show_seats (show_id + seat_id) ← THE HEART
                          └── booking_seats (show_seat_id)
                                └── bookings (booking_id + user_id)

movies ──────────────────────────────► shows
users  ──────────────────────────────► bookings
```

---

## Indexing — "Why your queries are slow and how to fix it"

You have the schema. Now 10 million users hit your DB. Queries are taking 3 seconds. Why?

**Without indexes, every query does a full table scan:**

```
"Find all shows for movie Pathaan in Mumbai on Jan 25"

Database: "Let me read all 10 million rows in the shows table 
           and check each one..." 🐢
```

**With indexes:**

```
Database: "Let me jump directly to the relevant rows 
           using the index tree" → milliseconds ✅
```

### Which indexes do you need?

```sql
-- Most common query: "Show me all shows for a movie in a city on a date"
CREATE INDEX idx_shows_movie_date 
ON shows(movie_id, show_date);

-- "Is this seat available for this show?" (Hit millions of times/second)
CREATE INDEX idx_show_seats_show_status 
ON show_seats(show_id, status);

-- "Find all bookings for a user"
CREATE INDEX idx_bookings_user 
ON bookings(user_id, booked_at DESC);

-- "Find all held seats that have expired" (background cleanup job)
CREATE INDEX idx_show_seats_locked_until 
ON show_seats(locked_until) 
WHERE status = 'HELD';    ← Partial index, only indexes HELD rows
```

### The Composite Index Trick

```sql
-- This query:
SELECT * FROM show_seats 
WHERE show_id = 5001 AND status = 'AVAILABLE';

-- Benefits from: INDEX on (show_id, status)
-- NOT from: INDEX on (status, show_id)  ← order matters!
```

The rule: **Put the most selective / most filtered column first.**

---

## The First Scaling Problem Appears 📈

Everything is beautiful. Schema is clean. Indexes are sharp. System launches.

Day 1: 1,000 users. Fine.  
Week 1: 100,000 users. Fine.  
Month 3: 1,000,000 users. Starting to sweat.  
Pathaan releases: 10,000,000 users in one day. **DB on fire. 🔥**

The team looks at the metrics:

```
DB CPU:        98% 🔴
DB Connections: 9,847 / 10,000 max 🔴
Query latency:  4.2 seconds avg 🔴
Read queries:   94% of total load
Write queries:  6% of total load
```

That last stat is the key insight:

> *"94% of our load is people just BROWSING — searching movies, viewing seats, checking showtimes. Only 6% are actually BOOKING."*

An engineer asks: **"Why is our precious DB serving seat-browsing requests? Those don't need to be real-time to the millisecond."**

This sets up our next chapter — **Caching with Redis.**

---

## 🎯 Interview Cheat Sheet for This Chapter

| Question | Answer |
|---|---|
| Why separate `seats` and `show_seats`? | Physical seat is permanent; availability changes per show |
| Where does optimistic lock version live? | In `show_seats` table, version column |
| Where does the HOLD state live? | `show_seats` status + Redis TTL |
| Why index `(show_id, status)` together? | Most common query pattern — "available seats for show X" |
| Why does the DB struggle at scale? | 94% read load, only 6% writes — reads can be cached |

---

## ✅ Concept Check

Can you answer these?
- Why do we need a `show_seats` table instead of just a `status` column in `seats`?
- If Rahul books seats A5 and A6 together, which tables get rows inserted?
- What's the difference between `bookings` and `booking_seats`?
- Why does index column order matter?

---

**Ready for Chapter 4: Caching with Redis?** 🚀

We'll cover *why* caching is needed, *what* to cache vs what not to, how Redis fits in, and the infamous **cache invalidation problem** — one of the hardest problems in computer science.

---

# Chapter 4: Caching with Redis — "The Art of Remembering"

## 🔁 Recap of where we are

Our DB is drowning. 94% of traffic is reads — people browsing movies, checking seat layouts, viewing showtimes. All of it hitting the database directly. The DB is doing the same work **over and over again.**

```
User 1: "Show me all movies in Mumbai today" → DB reads 10,000 rows
User 2: "Show me all movies in Mumbai today" → DB reads 10,000 rows  
User 3: "Show me all movies in Mumbai today" → DB reads 10,000 rows
...
User 50,000: "Show me all movies in Mumbai today" → DB reads 10,000 rows
```

Same question. Same answer. **50,000 identical DB queries.**

An engineer stares at this and says:

> *"What if we just... remembered the answer?"*

---

## The Library Analogy 📚

Imagine a librarian.

Every day, 500 students ask: *"What are the most popular books this month?"*

**Without cache:** The librarian walks to the back room, searches through 10,000 records, compiles the list, comes back. **Every. Single. Time.**

**With cache:** The librarian compiles the list once in the morning, writes it on a **sticky note at the desk.** Next 499 students get the answer in 2 seconds.

That sticky note? **That's your cache.**

Redis is that sticky note — but stored in RAM, accessible in **under 1 millisecond.**

---

## What is Redis, really?

```
Database (PostgreSQL):  Stores data on DISK  → Fast (milliseconds)
Redis:                  Stores data in RAM   → Blazing fast (microseconds)
```

Redis is essentially a **giant hashmap in memory:**

```
Key                          →  Value
"movies:mumbai:2024-01-25"   →  [{movie_id: 101, title: "Pathaan"...}, ...]
"show:5001:seats:available"  →  [4001, 4003, 4007, 4008...]
"movie:101:details"          →  {title: "Pathaan", duration: 146, ...}
```

You give it a key, it gives you the value. No joins. No disk reads. Pure RAM speed.

---

## The Caching Strategy — What to Cache?

Not everything should be cached. This is a critical interview discussion point.

The engineer sat down and categorized all data:

### Category 1 — Cache Aggressively ✅ (Changes rarely)

```
Movie details         → Changes maybe once (typo fix). Cache for 24 hours.
Theatre information   → Changes rarely. Cache for 24 hours.
City list             → Almost never changes. Cache for 7 days.
Screen/seat layout    → Fixed once built. Cache for 7 days.
Show listings         → Added daily, but stable once added. Cache for 1 hour.
```

### Category 2 — Cache Carefully ⚠️ (Changes moderately)

```
Available seat count per show  → Changes as bookings happen.
                                  Cache for 30 seconds. 
                                  "~150 seats left" is good enough for browsing.
```

### Category 3 — Never Cache ❌ (Changes constantly, must be accurate)

```
Exact seat availability (A5 booked or not?)  → NEVER cache this.
                                                Must hit DB for truth.
Active payment status                         → NEVER cache.
User's booking confirmation                   → NEVER cache.
```

This distinction is **crucial.** The seat count shown on the browsing page can be approximate. But when Rahul clicks "Book Seat A5," that must be checked against the real DB.

---

## Building the Caching Layer — Layer by Layer

### Layer 1: The Movie Browsing Cache

User opens BookMyShow and sees:

```
"What's showing in Mumbai today?"
```

**Without cache (before):**
```
App Server → DB Query (joins movies + shows + theatres + cities)
           → 200ms, DB under load
```

**With cache (after):**
```
App Server → Check Redis: "movies:mumbai:2024-01-25" 
           → HIT → Return in 1ms ✅
           
           → MISS → Query DB → Store in Redis with TTL → Return
                    (happens only once per hour)
```

Code flow:

```python
def get_movies_for_city(city, date):
    cache_key = f"movies:{city}:{date}"
    
    # Step 1: Check cache first
    cached = redis.get(cache_key)
    if cached:
        return json.loads(cached)  # Cache HIT — returns in <1ms
    
    # Step 2: Cache MISS — go to DB
    movies = db.query("""
        SELECT m.*, s.show_id, s.start_time 
        FROM movies m
        JOIN shows s ON m.movie_id = s.movie_id
        JOIN screens sc ON s.screen_id = sc.screen_id
        JOIN theatres t ON sc.theatre_id = t.theatre_id
        WHERE t.city_id = :city AND s.show_date = :date
    """, city=city, date=date)
    
    # Step 3: Store in cache with TTL
    redis.setex(cache_key, 3600, json.dumps(movies))  # 1 hour TTL
    
    return movies
```

---

### Layer 2: The Seat Map Cache

User clicks on a show. Sees the seat map. 

```
[A1][A2][A3][A4][A5][A6]...  ← Which are red (booked)? Which are green?
```

This page gets **hammered.** Every user browsing seats for Pathaan's 6 PM show hits it.

```python
def get_seat_map(show_id):
    cache_key = f"seatmap:{show_id}"
    
    cached = redis.get(cache_key)
    if cached:
        return json.loads(cached)
    
    # DB query — gets all seats + their current status
    seats = db.query("""
        SELECT s.seat_id, s.row_label, s.seat_number, 
               s.seat_type, s.base_price, ss.status
        FROM seats s
        JOIN show_seats ss ON s.seat_id = ss.seat_id
        WHERE ss.show_id = :show_id
    """, show_id=show_id)
    
    redis.setex(cache_key, 30, json.dumps(seats))  # 30 second TTL ⚠️
    
    return seats
```

**Why only 30 seconds TTL here?**

Because seats are getting booked constantly. If you cache for 1 hour, a user might see Seat A5 as green (available) on the seat map, click it, and find out it was booked 45 minutes ago. **Stale data.**

30 seconds is a balance:
- Fresh enough that users don't waste clicks on taken seats
- Long enough to absorb thousands of identical requests

---

### Layer 3: Available Seat Count Cache

The homepage shows:

```
Pathaan - 6 PM Show  |  Fast Filling  |  ⚡ 42 seats left
```

This number doesn't need to be exact. "About 42" is fine for the listing page.

```python
# Update this count in Redis whenever a booking happens
def update_available_count(show_id, delta):
    # Atomic decrement — Redis handles concurrency here
    redis.incrby(f"seats:available:{show_id}", delta)
    # delta = -1 when seat booked, +1 when booking cancelled

# Read it for display
def get_available_count(show_id):
    count = redis.get(f"seats:available:{show_id}")
    if count is None:
        # Bootstrap from DB if not in cache
        count = db.query("SELECT COUNT(*) FROM show_seats 
                          WHERE show_id=:id AND status='AVAILABLE'", id=show_id)
        redis.set(f"seats:available:{show_id}", count)
    return count
```

---

## The Cache Invalidation Problem 💣

Here it is. The thing that trips up most engineers.

> *"There are only two hard things in Computer Science: cache invalidation and naming things."*
> — Phil Karlton

### The Problem

```
10:00 AM — Seat map cached: A5=AVAILABLE, A6=AVAILABLE, A7=AVAILABLE
10:01 AM — Rahul books A5 → DB updated → A5=BOOKED
10:01 AM — Priya views seat map → Gets CACHED version → Sees A5=AVAILABLE 😱
10:01:30 AM — Cache expires (30s TTL) → Fresh data loaded → A5=BOOKED ✅
```

For 30 seconds, Priya sees wrong data. Is that okay?

**For the seat map browsing page? Yes.** Priya sees A5 as available, clicks it, the booking service checks the real DB, finds it's taken, and says "Sorry, choose another seat." Minor inconvenience.

**For the booking confirmation page? Absolutely not.** Never serve booking data from cache.

This is the key insight:

> *"Stale data is okay for display. Never okay for transactions."*

### Strategy 1 — TTL Based Invalidation (Lazy)

```
Set cache with TTL = 30 seconds.
After 30 seconds, cache auto-expires.
Next request rebuilds it from DB.
```

Simple. Works for most browsing data. But you get a **stale window.**

### Strategy 2 — Active Invalidation (Eager)

```
When a seat is booked:
  1. Update DB ✅
  2. Immediately DELETE the cached seat map for that show
  
Next user who requests the seat map:
  → Cache MISS → Fresh data from DB → Store in cache again
```

```python
def book_seat(show_id, seat_id, user_id):
    # 1. Update DB
    db.update_seat_status(show_id, seat_id, 'BOOKED')
    
    # 2. Immediately invalidate the cache
    redis.delete(f"seatmap:{show_id}")         # Force fresh load next time
    redis.delete(f"seats:available:{show_id}") # Invalidate count too
```

Faster consistency. But now every booking causes a cache miss spike.

### Strategy 3 — Write Through Cache

```
When a seat is booked:
  1. Update DB ✅
  2. ALSO update the cache directly with new value ✅
  
Cache is always in sync. No misses. No stale windows.
```

```python
def book_seat(show_id, seat_id, user_id):
    # 1. Update DB
    db.update_seat_status(show_id, seat_id, 'BOOKED')
    
    # 2. Update cache directly  
    seat_map = json.loads(redis.get(f"seatmap:{show_id}"))
    seat_map[seat_id]['status'] = 'BOOKED'
    redis.setex(f"seatmap:{show_id}", 30, json.dumps(seat_map))
```

Cleanest consistency. But complex to maintain — **every DB write needs a corresponding cache write.**

---

## Cache Stampede — The Hidden Killer 🐘

Here's a scenario nobody thinks about until it blows up production.

```
10:00:00 AM — Pathaan's 6 PM show cache EXPIRES for 10,000 concurrent users
10:00:00 AM — All 10,000 users get a cache MISS simultaneously
10:00:00 AM — All 10,000 users fire the SAME DB query simultaneously
10:00:00 AM — DB receives 10,000 identical heavy queries at once
10:00:00 AM — DB falls over 💀
```

This is called a **Cache Stampede** or **Thundering Herd.**

### Solution 1 — Mutex Lock

```python
def get_seat_map(show_id):
    cache_key = f"seatmap:{show_id}"
    cached = redis.get(cache_key)
    if cached:
        return json.loads(cached)
    
    # Only let ONE request rebuild the cache
    lock_key = f"lock:seatmap:{show_id}"
    if redis.set(lock_key, 1, nx=True, ex=5):  # nx=only if not exists
        # I got the lock — I'll rebuild the cache
        seats = db.query(...)
        redis.setex(cache_key, 30, json.dumps(seats))
        redis.delete(lock_key)
        return seats
    else:
        # Someone else is rebuilding — wait briefly and retry
        time.sleep(0.1)
        return get_seat_map(show_id)  # Retry — will likely hit cache now
```

Only 1 request hits the DB. The other 9,999 wait 100ms and hit the rebuilt cache.

### Solution 2 — Jitter on TTL

Instead of all caches expiring at the exact same time:

```python
# All seats for all shows cached at same time → all expire at same time → stampede
redis.setex(cache_key, 3600, data)  # All expire at T+1hr 💥

# Add random jitter → expiry spread across time → no stampede
import random
ttl = 3600 + random.randint(-300, 300)  # 55-65 minute window
redis.setex(cache_key, ttl, data)  # ✅
```

---

## The Full Caching Architecture

```
                        User Request
                             ↓
                      Load Balancer
                             ↓
                        App Server
                        ↙         ↘
              Is it browsing?    Is it booking?
                    ↓                   ↓
            Check Redis            Skip cache entirely
                ↓    ↓                  ↓
             HIT    MISS           Direct to DB
              ↓       ↓           (source of truth)
           Return   Query DB
           fast     → Store in Redis
                    → Return
                    
Redis stores:
├── movies:city:date          TTL: 1 hour
├── movie:id:details          TTL: 24 hours  
├── seatmap:show_id           TTL: 30 seconds
├── seats:available:show_id   TTL: 60 seconds
├── theatre:id:details        TTL: 24 hours
└── hold:show_id:seat_id      TTL: 600 seconds (the booking hold!)
```

---

## Cache Hit Ratio — How do you know it's working?

In an interview, if asked *"How do you measure cache effectiveness?"*

```
Cache Hit Ratio = Cache Hits / (Cache Hits + Cache Misses)

Good:    > 90% hit ratio  ✅
Okay:    70-90%           ⚠️  
Bad:     < 70%            ❌  Your cache isn't helping much
```

If hit ratio is low:
- TTL might be too short
- Cache keys might be too granular
- You might be caching data nobody actually reads

---

## What Redis Also Does for Us (Beyond Caching)

Redis isn't just a cache. In our system it's doing **three jobs:**

```
Job 1: Cache         → Store movie/show/seat data for fast reads
Job 2: Hold System   → "hold:show5001:seatA5" with 10min TTL (from Ch. 2)
Job 3: Rate Limiting → "ratelimit:user6001" = how many requests this second
```

Job 3 becomes critical in Chapter 5 when Avengers tickets go on sale.

---

## 🎯 Interview Cheat Sheet for This Chapter

| Question | Answer |
|---|---|
| What do you cache in a ticket system? | Movie details, show listings, seat maps (browsing only) |
| What do you NEVER cache? | Actual booking transactions, payment status |
| What TTL for seat maps? | Short — 30 seconds. Seats change frequently |
| What is cache stampede? | Many simultaneous cache misses hitting DB at once |
| How to fix cache stampede? | Mutex lock on rebuild OR TTL jitter |
| Three cache invalidation strategies? | TTL expiry, active delete, write-through |
| What else does Redis do here? | Seat holds (TTL), rate limiting |

---

## ✅ Concept Check

Can you answer these confidently?
- Why is the seat map TTL 30s but movie details TTL 24 hours?
- Rahul books seat A5. Walk me through what happens to the cache.
- What's the difference between cache invalidation and cache stampede?
- Why should you never cache the actual booking transaction check?

---

**Ready for Chapter 5: Scaling Under Load — The Avengers Problem?** 🚀

This is where we talk about what happens when **10 lakh people try to book tickets the moment they go on sale.** We'll cover Load Balancers, Horizontal Scaling, the Queue system, and Rate Limiting. The stuff that separates a good answer from a great one in interviews.

---

# Chapter 5: Scaling Under Load — "The Avengers Problem"

## 🔁 Recap of where we are

We have a clean schema. We have Redis caching reads. The system works beautifully for normal traffic.

Then one day, the announcement drops:

```
🔴 AVENGERS: SECRET WARS — TICKETS ON SALE TOMORROW 10 AM
```

The engineering team gets a Slack message at 9 PM the night before:

> *"Hey, we're expecting 10 lakh concurrent users at 10 AM sharp. 
> All trying to book at the exact same second. 
> Don't let the system die. Thanks."* 🙂

Everyone stares at their screens. Because right now, the architecture looks like this:

```
10 lakh users
      ↓
  1 Load Balancer
      ↓
  3 App Servers
      ↓
  1 Database
```

This will **absolutely, catastrophically fail** at 10 AM.

Let's fix it. Problem by problem.

---

## Problem 1 — One Server Can't Handle This 💀

### The Maths

A single app server can handle roughly **500-1000 requests/second** comfortably.

10 lakh users hitting at 10:00:00 AM = potentially **10 lakh requests in the first second.**

```
Capacity needed:   10,00,000 requests/second
One server gives:      1,000 requests/second
Gap:                 999x short 😱
```

### The Solution — Horizontal Scaling

**Vertical Scaling** = Make the one server bigger (more CPU, more RAM)

```
Before: 1 server, 8 cores, 16GB RAM
After:  1 server, 64 cores, 256GB RAM
```

This helps. But it has a ceiling. You can only make one machine so big. And it's expensive. And if it dies, **everything** dies.

**Horizontal Scaling** = Add more servers

```
Before: 1 server
After:  50 servers, each handling 20,000 requests/second
```

Cheaper. No ceiling. If one dies, 49 remain.

```
10 lakh users
      ↓
  Load Balancer  ← Distributes traffic across servers
   ↙  ↓   ↓  ↘
 S1   S2  S3...S50   ← 50 App Servers
   ↘  ↓   ↓  ↙
     Database Cluster
```

But now a new problem appears.

---

## Problem 2 — The Load Balancer Itself 🤔

### "Who watches the watchman?"

If you have 50 servers but only 1 load balancer, the load balancer is now your single point of failure.

Also — how does the load balancer decide which server to send a request to?

### Load Balancing Algorithms

**Algorithm 1 — Round Robin**
```
Request 1 → Server 1
Request 2 → Server 2
Request 3 → Server 3
Request 4 → Server 1  (back to start)
...
```
Simple. But what if Server 1 is slow and still processing Request 1 when Request 4 arrives? You're piling on a struggling server.

**Algorithm 2 — Least Connections**
```
Server 1: handling 450 requests
Server 2: handling 12 requests  ← Send next request HERE
Server 3: handling 280 requests
```
Smarter. Send to whoever has the most capacity right now.

**Algorithm 3 — IP Hash**
```
Hash(user's IP address) % number_of_servers = which server
```
Same user always goes to the same server. Why? **Session consistency.** If server stores any user state in memory, you want that user to always land on the same server.

*But wait — if servers store user state in memory, what happens when that server dies?*

> The answer: **Don't store state on servers.** Keep servers **stateless.** All state goes in Redis or DB.

Stateless servers = any request can go to any server = true horizontal scaling.

### Making the Load Balancer Itself Resilient

```
        DNS
         ↓
   Virtual IP (Floating IP)
    ↙              ↘
LB Primary      LB Secondary  ← Hot standby
(Active)        (Takes over if primary dies)
    ↓
  Servers
```

Two load balancers. Primary handles traffic. Secondary watches the primary. If primary dies, secondary takes the Virtual IP and becomes primary in seconds. Users never notice.

This is called **Active-Passive HA (High Availability).**

---

## Problem 3 — The Database Is Still One Machine 🔥

Even with 50 app servers, they're all writing to **one database.**

At 10 AM, 10 lakh users are all trying to book seats simultaneously. That means:

```
10 lakh UPDATE queries hitting 1 DB server
        ↓
DB connections maxed out
        ↓
Queries queue up
        ↓
Timeouts start
        ↓
Users see errors
        ↓
System appears down 💀
```

### The Queue — "Form a Line, Please" 🎟️

Remember the real-world queue outside the cinema in Chapter 1?

The insight was: **The cinema didn't stop existing because of the crowd. It just made people wait in an orderly line.**

We do the same thing digitally. Instead of letting 10 lakh requests slam the DB simultaneously, we put them in a **queue** and process them one by one.

```
10 lakh users click "Book" at 10:00:00 AM
              ↓
        Message Queue
     (Kafka / RabbitMQ)
              ↓
    Queue holds all requests
    in order, safely stored
              ↓
    Workers pull from queue
    at a rate DB can handle
    (say, 5000/second)
              ↓
           Database
        (happy, not overwhelmed)
```

### The User Experience with a Queue

```
User clicks "Book Seat A5" at 10:00:00 AM
            ↓
Instantly sees: "⏳ You're in queue! Position: 4,521"
            ↓
Queue updates: "Position: 3,102... 1,847... 412... Processing..."
            ↓
Either: "✅ Booking confirmed! Seat A5 is yours!"
    Or: "😔 Sorry, Seat A5 was taken. Choose another."
```

The user **knows they're waiting.** That's far better than a spinning loader that eventually crashes.

BookMyShow and Ticketmaster literally do this. You've seen it — *"Waiting Room: You're #8,241 in line."*

---

### How the Queue Works Internally

**Producer** = App Server (puts requests into queue)
**Consumer** = Worker Service (pulls from queue and processes)
**Broker** = Kafka / RabbitMQ (stores and manages the queue)

```
App Server (Producer)
      ↓
   "BookingRequest" message:
   {
     user_id: 6001,
     show_id: 5001,
     seat_ids: [4001, 4002],
     timestamp: 10:00:00.423,
     request_id: "req_xyz"
   }
      ↓
  Kafka Topic: "booking-requests"
  [req1][req2][req3][req4]...[req_1000000]
      ↓
  Worker Pools (pull at controlled rate)
  Worker1: processes req1
  Worker2: processes req2
  Worker3: processes req3
      ↓
  Database (manageable load)
```

### Why Kafka specifically?

```
Feature                  Kafka           RabbitMQ
Messages persist?        Yes (days)      Deleted after consumed
Replay messages?         Yes ✅          No ❌
Order guaranteed?        Per partition   Not guaranteed
Scale to millions/sec?   Yes ✅          Harder
```

If the worker crashes mid-processing, Kafka still has the message. The worker can restart and **replay from where it left off.** No booking request is lost.

---

## Problem 4 — Someone Is Sending 10,000 Requests Per Second 🤖

During the Avengers launch, your monitoring shows:

```
IP 203.45.67.89: 8,432 requests in last 10 seconds 🚨
IP 178.23.45.12: 6,219 requests in last 10 seconds 🚨
```

These aren't humans. These are **bots** — scalpers trying to book thousands of tickets programmatically to resell.

This is eating your queue capacity and crowding out real users.

### Rate Limiting — "You've Had Enough"

Rate limiting says:

> *"Each user can make at most X requests per Y seconds. Beyond that, we reject."*

```
Normal user:   "Book seat" → ✅
Normal user:   "Book seat" → ✅ (different seat, 2 min later)
Bot:           1000 requests in 1 second → ❌ ❌ ❌ ❌ Rate Limited
```

### Implementing Rate Limiting with Redis

Redis is perfect for this because it's fast and shared across all app servers.

**Algorithm — Sliding Window Counter:**

```python
def is_rate_limited(user_id):
    key = f"ratelimit:{user_id}"
    current_time = time.time()
    window = 60  # 60 second window
    max_requests = 10  # max 10 booking attempts per minute
    
    pipe = redis.pipeline()
    
    # Remove old entries outside our window
    pipe.zremrangebyscore(key, 0, current_time - window)
    
    # Count current requests in window  
    pipe.zcard(key)
    
    # Add this request
    pipe.zadd(key, {current_time: current_time})
    
    # Expire the key after window
    pipe.expire(key, window)
    
    results = pipe.execute()
    request_count = results[1]
    
    if request_count >= max_requests:
        return True  # Rate limited ❌
    
    return False  # Allow through ✅
```

Every request:
1. Checks how many requests this user made in last 60 seconds
2. If over 10 → reject with HTTP 429 "Too Many Requests"
3. If under 10 → allow through

**Different limits for different actions:**

```
Browse movies:      1000 requests/minute  (just reading, be generous)
View seat map:      100 requests/minute   (moderate)
Initiate booking:   5 requests/minute     (strict — this hits DB)
Payment attempt:    3 requests/minute     (very strict — fraud prevention)
```

---

## Problem 5 — The "Hotspot" Problem 🌡️

Even with 50 servers and a queue, there's still a bottleneck.

Every single booking request for Avengers needs to touch the **same rows** in the DB — the `show_seats` table for that specific show.

```
Show ID: 9999 (Avengers 10 AM show)
   ↓
All 10 lakh users hitting show_seats WHERE show_id = 9999
   ↓
Even with horizontal app servers...
   ↓
All writes converge on the SAME DB partition 🔥
```

This specific row/partition becomes a **hotspot.**

### Solution 1 — Seat Partitioning

Instead of one queue for the whole show, create **multiple queues by seat section:**

```
Section A (rows A-E):   Queue 1 → Worker Pool 1 → DB Shard 1
Section B (rows F-J):   Queue 2 → Worker Pool 2 → DB Shard 2  
Section C (rows K-O):   Queue 3 → Worker Pool 3 → DB Shard 3
```

Users wanting Row A seats compete in Queue 1.
Users wanting Row B seats compete in Queue 2.
No interference between sections.

### Solution 2 — Pre-generate Seat Tokens

Before 10 AM, generate a **token for every available seat** and dump them into a Redis list:

```python
# At 9:55 AM (before sale starts):
for seat_id in all_available_seats:
    redis.lpush("available_tokens:show9999", seat_id)

# Redis list now has 1000 seat tokens:
# [seatA1, seatA2, seatA3, ... seatJ10]
```

When user wants to book:
```python
# Atomically pop one token (Redis LPOP is atomic!)
seat_id = redis.lpop("available_tokens:show9999")

if seat_id:
    # Got a seat! Proceed to payment
    hold_seat(seat_id, user_id)
else:
    # List is empty = houseful
    return "Sorry, sold out"
```

Redis `LPOP` is **atomic** — only one user can pop each token. No two users get the same seat. The DB write happens after, calmly, confirmed by the token.

---

## Putting It All Together — The 10 AM Flow

```
9:55 AM  — Pre-generate seat tokens in Redis
9:59 AM  — Waiting room opens, users queue up
10:00 AM — Booking opens

User clicks Book:
    ↓
Rate Limiter (Redis): "Is this user within limits?" 
    → No  → HTTP 429, try again later
    → Yes → Continue
    ↓
Waiting Room Queue: "Your position: 4,521"
    ↓
Worker picks up request
    ↓
Redis LPOP: "Grab an available seat token"
    → No token → "Sold out"
    → Got token → Continue
    ↓
Redis Hold: SET hold:show9999:seatA5 = user_id, TTL=600s
    ↓
Return to user: "Seat A5 held for you! Pay in 10 mins"
    ↓
User pays (Chapter 7 - Payment)
    ↓
DB Write: UPDATE show_seats SET status='BOOKED'
    ↓
Confirmation sent (Chapter 8 - Notifications)
```

---

## The Architecture Now

```
                    10 Lakh Users
                         ↓
              ┌─── CDN (static content) ───┐
              ↓                            ↓
         Load Balancer (Active-Passive HA)
              ↓
    ┌────────────────────────────┐
    ↓          ↓         ↓      ↓
  App1       App2      App3  ...App50    (Stateless — scales infinitely)
    ↓          ↓         ↓      ↓
    └────────────────────────────┘
              ↓              ↓
         Rate Limiter    Redis Cache
         (Redis)         (Seat maps, 
                          movie data,
                          seat tokens,
                          holds)
              ↓
      Kafka Queue (absorbs spike)
      [req][req][req]...[req x 1M]
              ↓
    ┌──────────────────────┐
    Worker1  Worker2 ...Worker100
    └──────────────────────┘
              ↓
       Database Cluster
       (Next chapter — Sharding)
```

---

## 🎯 Interview Cheat Sheet for This Chapter

| Question | Answer |
|---|---|
| Vertical vs Horizontal scaling? | Vertical = bigger machine (ceiling exists). Horizontal = more machines (no ceiling) |
| Why stateless servers? | Any request can go to any server — true horizontal scale |
| Why use a queue for booking? | Absorbs traffic spike, protects DB, gives ordered processing |
| Why Kafka over RabbitMQ? | Message persistence, replay, massive scale |
| What is a hotspot? | One DB partition overwhelmed because all traffic hits same rows |
| How to prevent scalper bots? | Rate limiting — Redis sliding window counter |
| What is the waiting room pattern? | Queue users before they hit the booking system, show position |
| How to make Load Balancer resilient? | Active-Passive with floating IP |

---

## ✅ Concept Check

Can you answer these?
- If app servers store session state in memory, why does horizontal scaling break?
- Walk me through what happens to a booking request from click to DB write using the queue
- Why is Redis LPOP perfect for distributing seat tokens?
- A user sends 500 booking requests in 10 seconds. What happens at each layer?
- What's the difference between the waiting room queue and the Kafka queue?

---

**Ready for Chapter 6: Database Sharding & Replication?** 🚀

This is where we answer: *"Your single DB is now getting 50,000 writes per second. How do you scale it?"* We'll cover Master-Slave replication, sharding strategies, consistent hashing, and the dreaded cross-shard queries. This is one of the **most frequently asked topics** in system design interviews.

---

# Chapter 6: Database Sharding & Replication — "Divide and Conquer"

## 🔁 Recap of where we are

We have 50 app servers. Redis caching. Kafka queue. Rate limiting. The system handles the Avengers spike beautifully... until the team looks at the DB metrics:

```
DB Server Stats:
├── CPU:              94% 🔴
├── Disk I/O:         89% 🔴  
├── Active queries:   8,400 simultaneous
├── Query latency:    3.2 seconds avg 🔴
├── Storage used:     1.8 TB / 2 TB 🔴
└── Replication lag:  ∞ (no replication exists yet) 🔴
```

The DB is the last single point of failure. Everything else scales. The DB doesn't.

The team asks: **"How do you scale a database?"**

Two answers. Used together:
- **Replication** — Make copies (solves read load + availability)
- **Sharding** — Split the data (solves write load + storage)

Let's understand each deeply.

---

## Part 1 — Replication 📋

### The Story

Imagine a library with one copy of a very popular book.

100 people want to read it. Only 1 can at a time. Everyone else waits.

The librarian says: *"Let's make 5 copies."*

Now 5 people can read simultaneously. Queue shrinks by 5x.

**That's replication.** Multiple copies of your data, each able to serve reads.

---

### Master - Replica Architecture

```
                    All WRITES go here
                           ↓
                    ┌─── MASTER ───┐
                    │   (Primary)  │
                    └──────────────┘
                     ↙      ↓      ↘
              Replication stream (async)
               ↙        ↓          ↘
        ┌────────┐ ┌────────┐ ┌────────┐
        │Replica1│ │Replica2│ │Replica3│
        └────────┘ └────────┘ └────────┘
             ↑          ↑          ↑
         READ queries go to replicas
```

**The rule:**
```
Writes  → Master only  (one source of truth)
Reads   → Any Replica  (spread the load)
```

In our ticket system:
```
"Book this seat"         → Master (write)
"Show me seat map"       → Replica 1, 2, or 3 (read)
"Show me movie listings" → Replica 1, 2, or 3 (read)
"My booking history"     → Replica 1, 2, or 3 (read)
```

Remember — 94% of our traffic is reads. Replicas handle 94% of queries. Master only handles 6%. **Massive relief.**

---

### How Replication Works Internally

Every write on Master generates a log entry called **WAL — Write Ahead Log.**

```
Master WAL (like a diary of every change):
┌─────────────────────────────────────────────────────┐
│ LSN 1001: UPDATE show_seats SET status='BOOKED'     │
│           WHERE show_seat_id=2 (version 2→3)        │
│ LSN 1002: INSERT INTO bookings VALUES (7001, ...)   │
│ LSN 1003: INSERT INTO booking_seats VALUES (...)    │
│ LSN 1004: UPDATE show_seats SET status='HELD'       │
│           WHERE show_seat_id=5                      │
└─────────────────────────────────────────────────────┘
         ↓ Streamed continuously to replicas
Replica applies same changes in same order → identical data
```

Replicas are essentially **replaying the master's diary** in real time.

---

### The Replication Lag Problem ⏱️

Here's the catch. Replication is **asynchronous by default.**

```
10:00:00.000 — Rahul books Seat A5 → Master updated
10:00:00.000 — Priya queries Replica 2 for seat map
10:00:00.050 — Replication lag: 50ms
              → Replica 2 hasn't received the update yet
              → Priya STILL SEES A5 as AVAILABLE 😱
10:00:00.050 — Replication catches up → Replica updated
```

There's a **50ms window** (sometimes up to seconds during high load) where replicas show stale data.

**Is this okay for us?**

```
Priya sees A5 as available on seat map  → Acceptable ✅
  (She'll find out it's taken when she clicks book)
  
Rahul checks his booking confirmation   → NOT acceptable ❌
  (Must read from Master for this)
```

### The Read-Your-Own-Write Problem

Classic scenario:

```
1. Rahul books seat → writes to Master
2. Rahul immediately views "My Bookings" page
3. App reads from Replica (which hasn't synced yet)
4. Rahul sees his booking isn't there 😱
5. "Did my booking go through??" Rahul panics, books again → duplicate
```

### Solutions to Replication Lag

**Solution 1 — Read from Master for critical reads**
```python
def get_my_bookings(user_id, just_booked=False):
    if just_booked:
        # Read from master — user just wrote, needs fresh data
        return master_db.query("SELECT * FROM bookings WHERE user_id=:id", id=user_id)
    else:
        # Read from replica — historical data, lag is fine
        return replica_db.query("SELECT * FROM bookings WHERE user_id=:id", id=user_id)
```

**Solution 2 — Sticky reads after write**
```python
# After a write, store a flag in Redis for 5 seconds
redis.setex(f"read_master:{user_id}", 5, "1")

def route_read(user_id):
    if redis.get(f"read_master:{user_id}"):
        return master_db  # Still in the "fresh read" window
    return replica_db     # Safe to use replica
```

**Solution 3 — Synchronous Replication (for critical data)**
```
Master writes → waits for at least 1 replica to confirm → then acknowledges write

Slower (write waits for replica) but ZERO lag for that replica.
Use for: booking confirmations, payment records
Don't use for: everything (too slow)
```

---

### What Happens When Master Dies? 💀

```
Master dies at 10:00 AM
        ↓
Replica 1 is most up-to-date (least lag)
        ↓
Automatic failover promotes Replica 1 → New Master
        ↓
Replica 2 and 3 now replicate from new Master
        ↓
System back online in ~30 seconds
```

This is called **Automatic Failover** — tools like **Patroni** (PostgreSQL) or **MHA** (MySQL) do this automatically.

```
Before:  Master(dead) → Replica1, Replica2, Replica3
After:   Replica1(new Master) → Replica2, Replica3
```

The 30-second window is still downtime. To handle that, we use **connection pooling** + **retry logic** in the app:

```python
def execute_write(query, retries=3):
    for attempt in range(retries):
        try:
            return master_db.execute(query)
        except MasterUnavailableException:
            time.sleep(2 ** attempt)  # Exponential backoff: 1s, 2s, 4s
            master_db.reconnect()     # Will connect to new master
    raise Exception("DB unavailable after 3 attempts")
```

---

## Part 2 — Sharding 🔪

### The Problem Replication Doesn't Solve

Replication helps with **reads** (multiple replicas) and **availability** (failover).

But it doesn't help with:
- **Write throughput** — All writes still go to ONE master
- **Storage** — Every replica has ALL the data (2TB × 4 copies = 8TB)
- **Write hotspots** — One show's `show_seats` rows still on one machine

After 3 years of growth:

```
DB Storage: 20TB — won't fit on one machine
Write load: 100,000 writes/sec — one machine can't handle
```

You need **Sharding** — splitting data across multiple machines.

> *"Instead of one DB that knows everything, have many DBs each knowing a part."*

---

### The Restaurant Analogy 🍽️

Imagine a restaurant that's getting too crowded.

Option A: **Bigger restaurant** (vertical scaling) — Limited by building size.

Option B: **Open multiple branches** (sharding):
- Branch 1 handles customers with surnames A-H
- Branch 2 handles customers with surnames I-P  
- Branch 3 handles customers with surnames Q-Z

Each branch is smaller, faster, manageable. **That's sharding.**

---

### Sharding Strategy 1 — Range Based Sharding

Split data by a range of values.

```
Shard 1: show_id 1 — 10,000
Shard 2: show_id 10,001 — 20,000
Shard 3: show_id 20,001 — 30,000
```

```
Query: "Get seats for show_id 15,432"
         ↓
Router: "15,432 is in range 10,001-20,000"
         ↓
Goes directly to Shard 2 ✅
```

**Problem — Hotspot:**
```
New shows added → always get high IDs → always go to last shard
Old shows (no bookings) → sit on early shards doing nothing

Shard 3: 95% of all traffic 🔥
Shard 1: 2% of traffic 😴
Shard 2: 3% of traffic 😴
```

Data is split but load isn't. **Uneven shards** — called a **hot shard.**

---

### Sharding Strategy 2 — Hash Based Sharding

Instead of ranges, use a **hash function:**

```
shard_number = hash(show_id) % total_shards

show_id 5001  → hash = 2847561 → 2847561 % 3 = 0 → Shard 0
show_id 5002  → hash = 9182736 → 9182736 % 3 = 1 → Shard 1
show_id 5003  → hash = 4729183 → 4729183 % 3 = 2 → Shard 2
show_id 5004  → hash = 1928374 → 1928374 % 3 = 0 → Shard 0
```

Data distributes **evenly** across shards. No hotspots.

**Problem — Adding a new shard:**
```
Before: 3 shards → hash % 3
After:  4 shards → hash % 4

show_id 5001 → hash % 3 = Shard 0  (was here)
show_id 5001 → hash % 4 = Shard 1  (now here??)
```

**Almost all data needs to move to different shards.** 

Resharding a live production database = 😱

This is where **Consistent Hashing** comes in.

---

### Consistent Hashing — The Elegant Solution 💡

### The Story

Imagine a **clock face.** Numbers 0 to 360 degrees around the circle.

You place your **shards** at positions on the clock:
```
Shard A → position 90°
Shard B → position 180°
Shard C → position 270°
```

For each piece of data, you hash it to a **position on the clock**, then go **clockwise** to find which shard owns it:

```
         0°
         │
    Shard A (90°)
  ╔═══════╗
270°      90°
  Shard C    Shard B
(270°)      (180°)
         │
        180°

show_id 5001 → hashes to 120° → clockwise → hits Shard B at 180° ✅
show_id 5002 → hashes to 200° → clockwise → hits Shard C at 270° ✅
show_id 5003 → hashes to 300° → clockwise → hits Shard A at 90°  ✅
```

**Now add Shard D at 135°:**

```
Before: show_id 5001 → 120° → clockwise → Shard B (180°) ✅
After:  show_id 5001 → 120° → clockwise → Shard D (135°) ← only nearby data moves!

Only data between 90° and 135° moves to Shard D.
Everything else stays exactly where it is.
```

**Adding a shard only moves ~1/N of data, not everything.**

This is why consistent hashing is used in **Cassandra, DynamoDB, Redis Cluster** — anywhere you need to scale shards without full resharding.

---

### What's Our Shard Key?

This is a critical interview question: **"What do you shard on?"**

The shard key determines how data is distributed. Choose wrong and you're back to hot shards.

**Option 1 — Shard by `show_id`**
```
All data for one show lives on one shard:
  Shard 1: Shows 1-10000 (show_seats, bookings for these shows)
  Shard 2: Shows 10001-20000
  Shard 3: Shows 20001-30000
```

✅ All queries for a show hit one shard (fast, no cross-shard joins)  
✅ Seat availability check is local to one shard  
❌ Avengers show → millions of bookings → one shard gets hammered 🔥  

**Option 2 — Shard by `user_id`**
```
All data for one user lives on one shard:
  Shard 1: Users 1-1M (their bookings, preferences)
  Shard 2: Users 1M-2M
  Shard 3: Users 2M-3M
```

✅ "Show me my bookings" = one shard  
✅ Even distribution (users evenly distributed)  
❌ "Show me all bookings for Avengers show" = hits ALL shards 😱  

**Option 3 — Hybrid (What production systems do)**

```
Shard booking/user data by user_id:
  → Even distribution
  → User queries are fast

Shard show/seat data by show_id:
  → All seat availability local to one shard
  → Booking writes go to show's shard

Two separate sharded clusters for different access patterns
```

---

### The Cross-Shard Query Problem 😩

This is the **biggest pain** of sharding. Some queries need data from multiple shards.

```
"Show me all bookings across all shows this week in Mumbai"
          ↓
This requires data from ALL shards
          ↓
Fan out: query all N shards simultaneously
          ↓
Aggregate results in application layer
          ↓
Return combined result
```

```python
def get_all_bookings_this_week(city_id):
    results = []
    
    # Query ALL shards in parallel
    futures = []
    for shard in all_shards:
        future = thread_pool.submit(
            shard.query,
            "SELECT * FROM bookings WHERE city_id=:city AND week=:week",
            city=city_id, week=current_week
        )
        futures.append(future)
    
    # Collect all results
    for future in futures:
        results.extend(future.result())
    
    # Sort and aggregate in application
    return sorted(results, key=lambda x: x.booked_at, reverse=True)
```

This is slow and expensive. 

**The solution for analytics queries:** Don't run them on the sharded OLTP DB. Replicate all data into a **separate analytics DB** (like Redshift, BigQuery) designed for large aggregations.

```
OLTP Sharded DB    → Fast transactional reads/writes
         ↓ (CDC - Change Data Capture)
Analytics DB       → Slow, complex queries for reporting
```

---

### Putting Replication + Sharding Together

The production setup looks like this:

```
                        App Servers
                             ↓
                      Shard Router
                    (consistent hash)
                   ↙       ↓        ↘
            Shard 1     Shard 2    Shard 3
           ┌───────┐   ┌───────┐   ┌───────┐
           │Master │   │Master │   │Master │  ← Writes
           └───────┘   └───────┘   └───────┘
            ↙    ↘      ↙    ↘      ↙    ↘
          R1a   R1b   R2a   R2b   R3a   R3b  ← Reads
          
Each shard has its own Master + 2 Replicas
If one Master dies → its Replica promotes → other shards unaffected
```

**3 shards × (1 master + 2 replicas) = 9 DB nodes total.**

Every shard is independently scalable, independently available.

---

## The Complete Journey of One Write

Let's trace Rahul booking Seat A5 through the full system:

```
1. Rahul clicks "Book A5" 
         ↓
2. Rate Limiter (Redis): ✅ within limits
         ↓
3. Redis: LPOP seat token for A5 ← atomic, no race condition
         ↓
4. Redis: SET hold:show9999:seatA5 TTL=600s
         ↓
5. Shard Router: hash(show_id=9999) → Shard 2
         ↓
6. Shard 2 Master: 
   UPDATE show_seats SET status='HELD', version=version+1
   WHERE show_id=9999 AND seat_id=A5 AND version=7
         ↓
7. WAL entry created → streams to Shard 2 Replicas
         ↓
8. Rahul pays (Chapter 7)
         ↓
9. Shard 2 Master:
   UPDATE show_seats SET status='BOOKED'
   INSERT INTO bookings VALUES (...)
         ↓
10. Cache invalidated: DEL seatmap:9999
         ↓
11. Notification queued in Kafka → Email/SMS sent
```

Every step has been designed. Every failure has a fallback.

---

## 🎯 Interview Cheat Sheet for This Chapter

| Question | Answer |
|---|---|
| Replication vs Sharding? | Replication = copies for reads/availability. Sharding = split for write scale/storage |
| What is replication lag? | Delay between master write and replica receiving it. Solved by reading master for critical data |
| What is a hot shard? | One shard gets disproportionate traffic. Range sharding causes this |
| Why consistent hashing? | Adding/removing shards only moves 1/N data instead of resharding everything |
| What's the best shard key? | One that distributes evenly AND matches your most common query pattern |
| Cross-shard queries? | Fan out to all shards + aggregate in app. Or use separate analytics DB |
| What is WAL? | Write Ahead Log — the diary of DB changes, used for replication |
| What happens when master dies? | Replica with least lag auto-promotes via Patroni/MHA. ~30s failover |

---

## ✅ Concept Check

Can you answer these?
- Your replica is lagging 2 seconds behind master. User books a ticket and immediately views "My Bookings." What do you do?
- You're sharding `show_seats` by `show_id`. Avengers opens — 1 crore bookings for show_id 9999. What problem appears and how do you fix it?
- Why does adding a 4th shard break hash-based sharding but not consistent hashing?
- A query needs bookings for all users in Mumbai this week. How does it run across shards?
- Draw the DB topology for 3 shards with high availability.

---

**Ready for Chapter 7: Payments & The Saga Pattern?** 💳

This is where distributed systems get *really* interesting. What happens if the user's payment succeeds but our DB write fails? Or payment fails but we already marked the seat as booked? We'll cover the **Saga Pattern**, **Idempotency**, **Two-Phase Commit**, and why payment systems are the hardest part of any booking system to get right.

---

# Chapter 7: Payments & The Saga Pattern — "The Hardest Part"

## 🔁 Recap of where we are

Seat is held. User is at the payment screen. 10 minutes on the clock.

This is where most engineers think: *"Just call the payment API, if it works mark as booked, done."*

This thinking has caused **millions of dollars in losses** at real companies.

Let's understand why.

---

## The Nightmare Scenarios 😱

Before writing a single line of code, the senior engineer listed every way this could go wrong:

```
Scenario 1: Payment succeeds. Our DB write fails.
            → User is charged. Gets no ticket. 😱

Scenario 2: DB write succeeds (seat marked BOOKED). Payment fails.
            → Seat is gone. User isn't charged. 
            → They try again, seat is taken. 😱

Scenario 3: Payment API times out after 8 seconds.
            → Did it succeed? Did it fail? WE DON'T KNOW. 😱

Scenario 4: Server crashes after payment succeeds, 
            before we write to DB.
            → User charged. No ticket. No record. 😱

Scenario 5: User clicks "Pay" twice (double-clicked).
            → Charged twice. 😱

Scenario 6: Network drops after user pays.
            → User retries. Gets charged again. 😱
```

Every single one of these has happened at real companies. Real users. Real money lost.

The root cause of all of them is one thing:

> *"A booking spans multiple systems — our DB, the payment gateway, the notification service. Each can succeed or fail independently. There's no single 'commit' that covers all of them."*

In a single database, you have **ACID transactions:**
```sql
BEGIN;
  UPDATE show_seats SET status='BOOKED';
  INSERT INTO bookings VALUES (...);
COMMIT; -- Either both happen, or neither. Atomic.
```

But you can't do a single COMMIT across your DB + Razorpay + your notification service. They're different systems, different machines, different companies.

This is the **distributed transaction problem.**

---

## Attempt 1 — Two Phase Commit (2PC) 🤝

### The Story

Imagine a wedding. The priest asks:

*"Do you take this person to be your spouse?"*

**Phase 1 (Prepare):** Priest asks both people privately: *"Are you ready to commit?"*
- Person A: "Yes, ready"
- Person B: "Yes, ready"

**Phase 2 (Commit):** Only if BOTH say yes, the priest says: *"You're married!"*

If either says no in Phase 1 → wedding called off → both go home.

**That's 2PC:**

```
Phase 1 — PREPARE:
  Coordinator → DB:      "Can you lock seat A5 and prepare to mark BOOKED?"
  Coordinator → Payment: "Can you prepare to charge Rahul ₹450?"
  
  DB:      "Yes, I've locked it. Ready."      → VOTE: YES
  Payment: "Yes, I've reserved the charge."  → VOTE: YES

Phase 2 — COMMIT:
  All voted YES → Coordinator → DB:      "COMMIT"
                             → Payment: "COMMIT"
  
  Both execute. Both succeed. 
```

If anyone votes NO in Phase 1:
```
  Coordinator → DB:      "ROLLBACK"
  Coordinator → Payment: "ROLLBACK"
  
  Neither executes. Clean state.
```

### Why 2PC Fails in Practice ❌

**Problem 1 — The Coordinator Dies**

```
Phase 1: Both DB and Payment vote YES
Phase 2: Coordinator sends COMMIT to DB... then DIES

DB:      Committed ✅ (seat marked BOOKED)
Payment: Waiting for COMMIT forever ⏳ (locked, can't do anything)

System is now STUCK. 
Payment service holds a lock indefinitely.
Nobody knows what to do.
```

This is called the **blocking problem** of 2PC. If the coordinator dies between phases, the whole system freezes.

**Problem 2 — Latency**

```
2PC requires multiple round trips:
  Prepare round:   100ms
  Vote collection: 100ms  
  Commit round:    100ms
  Total:           300ms+ just for coordination overhead

At scale, this is unacceptable.
```

**Problem 3 — External Payment Gateways Don't Support 2PC**

Razorpay, Stripe, PayU — none of them implement 2PC. You can't tell Razorpay *"prepare to charge but don't actually charge yet."*

2PC only works when all participants are internal systems you control. In the real world, you always have external dependencies.

The industry moved on. Enter **The Saga Pattern.**

---

## The Saga Pattern 🎭

### The Story

The word "saga" comes from Norse literature — long stories where things go wrong and characters have to **undo their previous actions** to restore order.

That's exactly what the Saga Pattern does.

> *"Instead of one giant atomic transaction, break the booking into a sequence of small local transactions. If any step fails, run compensating transactions to undo the previous steps."*

```
Step 1: Hold seat         → Can undo: Release hold
Step 2: Create booking    → Can undo: Cancel booking  
Step 3: Charge payment    → Can undo: Refund payment
Step 4: Confirm booking   → Can undo: Cancel + notify user
Step 5: Send notification → No undo needed (best effort)
```

Each step is a small, local, ACID transaction. The "saga" is the sequence of these steps.

---

### Two Types of Saga

**Type 1 — Choreography Saga** (Event-driven)

Each service does its step, then publishes an event. The next service listens for that event and does its step.

```
Booking Service: "Seat held" → publishes event: SeatHeld
        ↓
Payment Service: hears SeatHeld → charges user → publishes: PaymentSuccess
        ↓
Booking Service: hears PaymentSuccess → marks BOOKED → publishes: BookingConfirmed
        ↓
Notification Service: hears BookingConfirmed → sends email/SMS
```

No central coordinator. Services talk to each other via events (Kafka).

✅ Decoupled — services don't know about each other  
✅ No single point of failure  
❌ Hard to track overall saga state — "what step are we on?"  
❌ Hard to debug — events scattered across many services  

**Type 2 — Orchestration Saga** (Central coordinator)

One **Saga Orchestrator** service tells each participant what to do, step by step.

```
                Saga Orchestrator
                      │
        ┌─────────────┼─────────────┐
        ↓             ↓             ↓
  Seat Service  Payment Service  Notification Service
  
Orchestrator: "Seat Service, hold seat A5"
Seat Service: "Done ✅"
Orchestrator: "Payment Service, charge Rahul ₹450"
Payment Service: "Done ✅"
Orchestrator: "Booking Service, mark as BOOKED"
Booking Service: "Done ✅"
Orchestrator: "Notification Service, send confirmation"
Notification Service: "Done ✅"

Saga complete! 🎉
```

✅ Single place to see the full saga state  
✅ Easy to debug and monitor  
✅ Easier to handle failures  
❌ Orchestrator can become a bottleneck  
❌ More coupled — orchestrator knows about all services  

**For a booking system, Orchestration is preferred** — you need clear visibility into where each booking stands.

---

### The Full Saga with Failure Handling

Let's walk through our booking saga with every failure case:

```python
class BookingSaga:
    def execute(self, user_id, show_id, seat_ids, payment_info):
        saga_id = generate_uuid()
        
        # Save saga state — survives server crashes
        self.save_state(saga_id, "STARTED", {
            "user_id": user_id,
            "show_id": show_id, 
            "seat_ids": seat_ids
        })
        
        try:
            # Step 1: Hold seats
            self.save_state(saga_id, "HOLDING_SEATS")
            hold = seat_service.hold_seats(show_id, seat_ids, user_id)
            self.save_state(saga_id, "SEATS_HELD", {"hold_id": hold.id})
            
            # Step 2: Create pending booking record
            self.save_state(saga_id, "CREATING_BOOKING")
            booking = booking_service.create_pending(user_id, show_id, seat_ids)
            self.save_state(saga_id, "BOOKING_PENDING", {"booking_id": booking.id})
            
            # Step 3: Process payment
            self.save_state(saga_id, "PROCESSING_PAYMENT")
            payment = payment_service.charge(user_id, booking.amount, payment_info)
            self.save_state(saga_id, "PAYMENT_SUCCESS", {"payment_id": payment.id})
            
            # Step 4: Confirm booking
            self.save_state(saga_id, "CONFIRMING")
            booking_service.confirm(booking.id, payment.id)
            self.save_state(saga_id, "CONFIRMED")
            
            # Step 5: Notify (best effort — failure here doesn't rollback)
            notification_service.send_confirmation(user_id, booking.id)
            
            self.save_state(saga_id, "COMPLETED")
            return booking
            
        except SeatUnavailableError:
            # Step 1 failed — nothing to undo
            self.save_state(saga_id, "FAILED_NO_SEATS")
            raise
            
        except PaymentFailedError:
            # Step 3 failed — undo steps 1 and 2
            self.save_state(saga_id, "COMPENSATING")
            booking_service.cancel(booking.id)      # Undo step 2
            seat_service.release_hold(hold.id)      # Undo step 1
            self.save_state(saga_id, "COMPENSATED")
            raise
            
        except BookingConfirmError:
            # Step 4 failed — payment went through but booking failed
            # MUST REFUND
            self.save_state(saga_id, "COMPENSATING")
            payment_service.refund(payment.id)      # Undo step 3
            booking_service.cancel(booking.id)      # Undo step 2
            seat_service.release_hold(hold.id)      # Undo step 1
            self.save_state(saga_id, "COMPENSATED")
            raise
```

**The key insight:** Every step has a corresponding **compensating transaction** that undoes it.

---

## Idempotency — Solving the "Did It Work?" Problem 🔑

### The Timeout Nightmare

```
Rahul clicks Pay
      ↓
Request goes to Payment Gateway
      ↓
Payment processes... (3 seconds)
      ↓
Response comes back... 
      ↓
NETWORK DROPS before we receive response 💀

Did the payment go through? WE DON'T KNOW.
```

If we retry → might charge Rahul twice.
If we don't retry → might leave Rahul uncharged but seat held.

This is the **dual write problem** in distributed systems. The operation might have succeeded, but we didn't get confirmation.

### The Solution — Idempotency Keys

> *"An operation is idempotent if doing it multiple times has the same effect as doing it once."*

```
HTTP DELETE is idempotent:   DELETE /booking/123 twice → booking gone (same result)
HTTP GET is idempotent:      GET /movies twice → same movie list
HTTP POST is NOT idempotent: POST /charge twice → charged TWICE 😱
```

We make our payment call idempotent by sending a **unique idempotency key:**

```python
def charge_user(user_id, amount, booking_id):
    idempotency_key = f"charge:{booking_id}"  # Unique per booking
    
    response = payment_gateway.charge(
        user_id=user_id,
        amount=amount,
        idempotency_key=idempotency_key  # ← Magic ingredient
    )
    return response
```

The payment gateway's behaviour:

```
First call with key "charge:booking7001":
  → Processes payment → Charges ₹450 → Returns SUCCESS
  → Stores: {"charge:booking7001": SUCCESS, payment_id: "pay_abc"}

Second call with SAME key "charge:booking7001" (retry):
  → "I've seen this key before!"
  → Returns the STORED result: SUCCESS, payment_id: "pay_abc"
  → Does NOT charge again ✅
```

**Retry as many times as you want. The user is never double-charged.**

Stripe, Razorpay, PayU — all support idempotency keys. It's an industry standard.

---

### Idempotency in Our Own Services Too

Not just payments. Every step in our saga should be idempotent:

```python
def hold_seat(show_id, seat_id, user_id, request_id):
    # Check if we already processed this request
    existing = db.query("""
        SELECT * FROM seat_holds 
        WHERE request_id = :req_id
    """, req_id=request_id)
    
    if existing:
        return existing  # Already done — return same result ✅
    
    # First time — actually process it
    hold = create_new_hold(show_id, seat_id, user_id, request_id)
    return hold
```

The `request_id` travels through every service. If anything retries, it carries the same ID. No duplicate operations.

---

## The Outbox Pattern — Guaranteed Event Publishing 📬

### The Hidden Bug

After successful payment, we need to publish a `BookingConfirmed` event to Kafka so the notification service can send an email.

Here's the naive code:

```python
def confirm_booking(booking_id, payment_id):
    # Step 1: Update DB
    db.execute("UPDATE bookings SET status='CONFIRMED' WHERE id=:id", id=booking_id)
    
    # Step 2: Publish event
    kafka.publish("booking-confirmed", {"booking_id": booking_id})
```

**What if the server crashes between Step 1 and Step 2?**

```
DB: Booking is CONFIRMED ✅
Kafka: Event never published ❌

Result: User gets no email. No SMS. 
        They think booking failed. They try again. Chaos.
```

Two operations. No way to make them atomic together.

### The Solution — Outbox Pattern

Instead of writing to Kafka directly, write to a **local DB table** (the "outbox") in the **same transaction** as the booking update:

```python
def confirm_booking(booking_id, payment_id):
    with db.transaction():  # Single atomic transaction
        # Step 1: Update booking
        db.execute("""
            UPDATE bookings SET status='CONFIRMED', 
            payment_id=:pay_id WHERE id=:id
        """, pay_id=payment_id, id=booking_id)
        
        # Step 2: Write to outbox (SAME transaction, same DB)
        db.execute("""
            INSERT INTO outbox_events 
            (event_type, payload, status, created_at)
            VALUES ('BookingConfirmed', :payload, 'PENDING', NOW())
        """, payload=json.dumps({"booking_id": booking_id}))
        
    # If we crash here — both writes rolled back. Clean state.
    # If we crash after — both committed. Outbox has the event.
```

A separate **Outbox Processor** (background service) reads from the outbox table and publishes to Kafka:

```python
# Runs every second
def process_outbox():
    pending = db.query("""
        SELECT * FROM outbox_events 
        WHERE status='PENDING' 
        ORDER BY created_at 
        LIMIT 100
    """)
    
    for event in pending:
        kafka.publish(event.event_type, event.payload)
        db.execute("""
            UPDATE outbox_events 
            SET status='PUBLISHED' WHERE id=:id
        """, id=event.id)
```

```
DB Transaction commits:
  ✅ bookings.status = CONFIRMED
  ✅ outbox_events row inserted

Outbox Processor:
  → Reads pending event
  → Publishes to Kafka
  → Marks as PUBLISHED

Notification Service:
  → Hears Kafka event
  → Sends email ✅
```

**Booking update and event are always in sync.** Either both happen or neither does.

---

## The Complete Payment Flow

Putting everything together:

```
User clicks "Pay ₹450"
      ↓
Generate idempotency_key = "charge:{booking_id}"
Generate saga_id = uuid()
Save saga state = STARTED
      ↓
┌─────────────── SAGA BEGINS ───────────────┐
│                                           │
│  1. Hold Seat (Redis TTL + DB)            │
│     → Success: saga state = SEATS_HELD   │
│                                           │
│  2. Create PENDING booking in DB          │
│     → Success: saga state = BOOKING_CREATED│
│                                           │
│  3. Call Payment Gateway                  │
│     with idempotency_key                  │
│     → Timeout? → Retry same key (safe)   │
│     → Failure? → Compensate steps 1+2    │
│     → Success: saga state = PAYMENT_DONE │
│                                           │
│  4. DB Transaction:                       │
│     UPDATE bookings SET status=CONFIRMED  │
│     INSERT INTO outbox_events (event)     │
│     ← single atomic transaction           │
│     → Success: saga state = CONFIRMED    │
│                                           │
│  5. Outbox Processor:                     │
│     → Publishes to Kafka                  │
│     → Notification Service sends email   │
│                                           │
└────────────── SAGA COMPLETE ──────────────┘
```

---

## What Happens to Abandoned Sagas? 🧹

Server crashes mid-saga. Saga state is saved in DB.

A **Saga Recovery Service** runs every minute:

```python
def recover_stuck_sagas():
    # Find sagas stuck for more than 5 minutes
    stuck = db.query("""
        SELECT * FROM sagas 
        WHERE status NOT IN ('COMPLETED', 'COMPENSATED', 'FAILED')
        AND updated_at < NOW() - INTERVAL '5 minutes'
    """)
    
    for saga in stuck:
        if saga.status == 'PAYMENT_DONE':
            # Payment succeeded but booking wasn't confirmed
            # Safe to retry confirmation
            retry_confirmation(saga)
            
        elif saga.status in ('HOLDING_SEATS', 'CREATING_BOOKING'):
            # Failed before payment — safe to just compensate
            compensate_saga(saga)
            
        elif saga.status == 'PROCESSING_PAYMENT':
            # Check with payment gateway — did it actually go through?
            result = payment_gateway.check_status(saga.idempotency_key)
            if result == 'SUCCESS':
                # It worked! Continue the saga
                continue_saga_from_payment(saga)
            else:
                # It failed. Compensate.
                compensate_saga(saga)
```

**No saga is ever lost.** The system heals itself.

---

## 🎯 Interview Cheat Sheet for This Chapter

| Question | Answer |
|---|---|
| Why not 2PC for distributed transactions? | Coordinator failure causes blocking. External gateways don't support it. High latency. |
| What is the Saga Pattern? | Break transaction into steps with compensating transactions for rollback |
| Choreography vs Orchestration? | Choreography = event-driven, decoupled. Orchestration = central coordinator, easier to debug |
| What is an idempotency key? | Unique key per operation — retrying with same key returns same result, no duplicate charge |
| What is the Outbox Pattern? | Write event to local DB table in same transaction as data write. Separate processor publishes to Kafka. |
| What if payment times out? | Retry with same idempotency key. Gateway deduplicates. |
| What if server crashes mid-saga? | Recovery service finds stuck sagas, checks each step's state, retries or compensates |

---

## ✅ Concept Check

Can you answer these?
- Payment succeeded but our server crashed before writing to DB. What happens and how do we recover?
- User double-clicks Pay button. Walk through why they're not charged twice.
- Why can't we just do `UPDATE bookings` and then `kafka.publish()` in sequence?
- What's the compensating transaction if payment succeeds but seat confirmation fails?
- Draw the full saga for a successful booking with all 5 steps.

---

**Ready for Chapter 8: Failure Handling & Resilience Patterns?** 🛡️

This is where we ask: *"What happens when things go wrong at scale?"* We'll cover Circuit Breakers, Bulkheads, Timeouts, Retries with exponential backoff, and Graceful Degradation — the patterns that separate a system that survives failures from one that **cascades into total collapse.** This is the chapter that separates senior engineers from everyone else in interviews.

---

# Chapter 8: Failure Handling & Resilience Patterns — "Surviving the Storm"

## 🔁 Recap of where we are

We have a distributed system. Multiple services. Multiple databases. External payment gateways. Kafka queues. Redis clusters.

Here's the uncomfortable truth about distributed systems:

> *"In a system with 10 services, each with 99.9% uptime, the probability that ALL are up simultaneously is 99.9%^10 = **99.0%**. That's 3.6 days of downtime per year."*

**Things will fail.** Servers crash. Networks partition. Payment gateways slow down. Redis runs out of memory. 

The question isn't *"How do we prevent failures?"*

The question is *"How do we design so that one failure doesn't become total collapse?"*

This chapter is about that.

---

## The Cascading Failure — "The Domino Effect" 🁣

### The Story

It's a regular Tuesday. Razorpay (payment gateway) starts responding slowly — 8 seconds per request instead of 300ms.

Watch what happens:

```
10:00 AM — Razorpay slows down
            ↓
Booking requests are now taking 8 seconds each
            ↓
App servers have limited threads (say 200 per server)
All 200 threads are stuck waiting for Razorpay
            ↓
New requests arrive — no threads available
            ↓
App server queues fill up
            ↓
New requests time out waiting in queue
            ↓
Load balancer marks app servers as unhealthy
            ↓
Entire booking system appears DOWN
            ↓
Users can't even BROWSE movies (nothing to do with payment!)
```

**One slow external service took down the entire platform.**

This is a **cascading failure.** One problem in one place amplifies until everything collapses.

The engineer who saw this said:

> *"Our system is only as strong as its weakest external dependency. We need to ISOLATE failures."*

---

## Pattern 1 — Timeouts ⏱️

### "Don't Wait Forever"

The first and simplest fix. Every network call must have a **timeout.**

```python
# WRONG — waits forever
response = payment_gateway.charge(user_id, amount)

# RIGHT — give up after 3 seconds
response = payment_gateway.charge(
    user_id, amount,
    timeout=3.0  # seconds
)
```

But what timeout value? Too short → too many false failures. Too long → threads blocked too long.

### Setting Timeouts — The P99 Rule

Look at your metrics:

```
Payment gateway response times:
  P50 (median):    300ms   ← 50% of calls finish in 300ms
  P95:             800ms   ← 95% of calls finish in 800ms
  P99:             1.2s    ← 99% of calls finish in 1.2s
  P99.9:           4.0s    ← 99.9% of calls finish in 4.0s
```

Set timeout at **2-3x your P99:**

```
Timeout = 1.2s × 2.5 = 3 seconds

Meaning: 99% of calls succeed well within timeout.
         We only cut off the truly slow outliers.
```

### Timeout Hierarchy

Different operations get different timeouts:

```python
TIMEOUTS = {
    "redis_read":        0.1,   # 100ms — RAM operation, should be instant
    "db_read":           0.5,   # 500ms — indexed query
    "db_write":          1.0,   # 1s    — write + replication
    "payment_gateway":   3.0,   # 3s    — external network call
    "notification":      2.0,   # 2s    — email/SMS API
    "total_booking":     10.0   # 10s   — entire booking flow
}
```

The **total booking timeout** is the user-facing one. If the whole saga takes more than 10 seconds, something is wrong — abort and compensate.

---

## Pattern 2 — Retries with Exponential Backoff 🔄

### "Try Again, But Be Smart About It"

When a call fails or times out, the instinct is to retry immediately.

**Immediate retry is usually wrong:**

```
Payment gateway is slow because it's overwhelmed.
You immediately retry → gateway gets even more traffic → even slower.
All clients retry simultaneously → massive traffic spike → gateway dies.
```

This is called a **Retry Storm** — retries making the problem worse.

### Exponential Backoff

Wait longer and longer between retries:

```python
def call_with_retry(func, max_retries=3):
    for attempt in range(max_retries):
        try:
            return func()
            
        except (TimeoutError, ServiceUnavailableError) as e:
            if attempt == max_retries - 1:
                raise  # Last attempt — propagate error
            
            # Exponential backoff: 1s, 2s, 4s
            wait_time = (2 ** attempt)
            
            # Add jitter — prevent all clients retrying simultaneously
            jitter = random.uniform(0, wait_time * 0.1)
            
            time.sleep(wait_time + jitter)
    
# Retry timeline:
# Attempt 1: Fails immediately
# Wait: 1.0s + 0.08s jitter = 1.08s
# Attempt 2: Fails
# Wait: 2.0s + 0.15s jitter = 2.15s  
# Attempt 3: Fails → raise exception
```

### The Jitter Is Critical

Without jitter:
```
1000 clients all fail at T=0
All wait exactly 1 second
All retry at T=1s simultaneously → thundering herd 💥
All fail again
All wait exactly 2 seconds
All retry at T=3s simultaneously → thundering herd again 💥
```

With jitter:
```
1000 clients all fail at T=0
Client 1:   retries at T=1.02s
Client 2:   retries at T=1.08s
Client 3:   retries at T=0.95s
...
Spread across a window → server can handle ✅
```

### What to Retry vs What Not To

```
✅ Retry these:
   Network timeouts
   HTTP 429 (rate limited — wait and retry)
   HTTP 503 (service temporarily unavailable)
   DB connection errors

❌ Never retry these:
   HTTP 400 (bad request — retrying won't fix bad data)
   HTTP 401 (unauthorized — retrying won't fix auth)
   HTTP 422 (invalid data)
   Payment declined — user's card rejected, not a transient error
   Duplicate booking — retrying creates another duplicate
```

The rule: **Only retry transient failures — ones that might resolve on their own.**

---

## Pattern 3 — Circuit Breaker 🔌

### "Stop Hitting a Dead Service"

Retries are good for occasional failures. But what if a service is completely down for 10 minutes?

```
Without Circuit Breaker:
Every user request → tries payment → waits 3s timeout → retries → fails
= Every booking attempt wastes 9+ seconds and burns threads

10,000 users × 9 seconds of waiting = threads exhausted = system down
```

The Circuit Breaker pattern says:

> *"After enough failures, stop calling the broken service entirely. Fail fast. Let it recover."*

Named after electrical circuit breakers — when a circuit overloads, the breaker **trips** and cuts the power to prevent damage.

### The Three States

```
                  ┌─────────────────────────────────────────┐
                  ↓                                         │
         ┌──────────────┐    Too many failures        ┌────────────┐
         │    CLOSED    │ ──────────────────────────► │    OPEN    │
         │  (Normal)    │                             │  (Broken)  │
         └──────────────┘                             └────────────┘
                  ↑                                         │
                  │ All healthy again                       │ After timeout
                  │                                         ↓
                  │                               ┌──────────────────┐
                  └────────── Success ────────────│  HALF-OPEN       │
                                                  │  (Testing)       │
                                                  └──────────────────┘
```

**CLOSED (Normal operation):**
```
Requests flow through normally.
Failure counter tracks recent failures.
If failures > threshold → OPEN
```

**OPEN (Broken):**
```
ALL requests fail immediately without calling the service.
No waiting. No timeouts. Instant failure.
After X seconds (recovery window) → HALF-OPEN
```

**HALF-OPEN (Testing recovery):**
```
Allow ONE test request through.
If it succeeds → back to CLOSED (service recovered ✅)
If it fails → back to OPEN (still broken ❌)
```

### Implementation

```python
class CircuitBreaker:
    def __init__(self, failure_threshold=5, recovery_timeout=60):
        self.failure_count = 0
        self.failure_threshold = failure_threshold  # Open after 5 failures
        self.recovery_timeout = recovery_timeout    # Try again after 60s
        self.state = "CLOSED"
        self.last_failure_time = None
    
    def call(self, func, *args, **kwargs):
        
        if self.state == "OPEN":
            # Check if recovery window has passed
            if time.time() - self.last_failure_time > self.recovery_timeout:
                self.state = "HALF-OPEN"
            else:
                # Fail immediately — don't even try
                raise CircuitOpenError("Payment service is down. Try later.")
        
        try:
            result = func(*args, **kwargs)
            self._on_success()
            return result
            
        except Exception as e:
            self._on_failure()
            raise
    
    def _on_success(self):
        self.failure_count = 0
        self.state = "CLOSED"  # Reset — service is healthy
    
    def _on_failure(self):
        self.failure_count += 1
        self.last_failure_time = time.time()
        
        if self.failure_count >= self.failure_threshold:
            self.state = "OPEN"
            print("⚡ Circuit OPENED — payment service appears down")

# Usage
payment_breaker = CircuitBreaker(failure_threshold=5, recovery_timeout=60)

def charge_user(user_id, amount):
    return payment_breaker.call(
        razorpay.charge, user_id, amount
    )
```

### What the User Sees

```
Normal:       "Processing payment..."  → Succeeds ✅
Circuit OPEN: "Payment service is temporarily unavailable. 
               Your seat is held. Please try again in 2 minutes." 
               → Instant response. No 9-second wait. ✅
```

Far better than a spinning loader that times out.

---

## Pattern 4 — Bulkhead 🚢

### "Don't Let One Thing Sink the Ship"

A ship has **bulkheads** — sealed compartments. If one compartment floods, the others stay dry. The ship doesn't sink.

In software:

> *"Isolate resources (threads, connections) for different operations so one slow operation can't starve others."*

### The Problem Without Bulkheads

```
App Server has 200 threads total.
All 200 threads are shared between:
  - Movie browsing
  - Seat map viewing  
  - Booking/Payment
  - Admin operations

Razorpay slows down.
200 threads get stuck waiting for payment.
ZERO threads left for movie browsing.
Users can't even see the homepage.
```

### The Solution — Separate Thread Pools

```python
# Separate thread pools — isolated from each other
THREAD_POOLS = {
    "browsing":    ThreadPoolExecutor(max_workers=100),  # 100 threads
    "booking":     ThreadPoolExecutor(max_workers=50),   # 50 threads
    "payment":     ThreadPoolExecutor(max_workers=30),   # 30 threads
    "admin":       ThreadPoolExecutor(max_workers=20),   # 20 threads
}

def handle_request(request_type, func):
    pool = THREAD_POOLS[request_type]
    
    try:
        future = pool.submit(func)
        return future.result(timeout=10)
    except Full:
        # This pool is exhausted — but other pools are fine
        raise ServiceBusyError(f"{request_type} is busy, try again")
```

Now when payment threads are stuck:

```
Payment pool:  30/30 threads busy (Razorpay slow) 🔴
Browsing pool: 12/100 threads busy ✅ ← Completely unaffected
Booking pool:  8/50 threads busy  ✅ ← Can still accept new bookings
```

**A payment outage no longer takes down browsing.** The ship doesn't sink.

### Connection Pool Bulkheads

Same pattern applies to DB connections:

```python
DB_POOLS = {
    "reads":    ConnectionPool(max_connections=80),   # Replica reads
    "writes":   ConnectionPool(max_connections=20),   # Master writes
    "analytics":ConnectionPool(max_connections=10),   # Heavy queries
}
```

A runaway analytics query doesn't eat all connections and block user bookings.

---

## Pattern 5 — Graceful Degradation 🪜

### "Do Less, But Keep Going"

When parts of the system fail, **don't fail completely.** Do a reduced version of the job.

This requires asking: *"What's the core thing users MUST be able to do? What's nice-to-have?"*

```
MUST HAVE:        Browse movies, Book seats, Pay
NICE TO HAVE:     Personalized recommendations, Review scores, 
                  "Others also booked" suggestions, Seat heatmaps
```

When under stress, gracefully shed the nice-to-haves:

```python
def get_movie_page(movie_id, user_id):
    # Core data — must have. If this fails, page fails.
    movie = get_movie_details(movie_id)
    shows = get_shows(movie_id)
    
    # Nice-to-have — degrade gracefully if unavailable
    try:
        recommendations = recommendation_service.get(user_id, timeout=0.5)
    except (TimeoutError, ServiceError):
        recommendations = []  # Empty list — page still loads ✅
    
    try:
        reviews = review_service.get(movie_id, timeout=0.5)
    except (TimeoutError, ServiceError):
        reviews = None  # Show "Reviews unavailable" ✅
    
    try:
        seat_heatmap = analytics_service.get_popular_seats(movie_id)
    except (TimeoutError, ServiceError):
        seat_heatmap = None  # Just don't show heatmap ✅
    
    return render_page(movie, shows, recommendations, reviews, seat_heatmap)
```

User gets the movie page. Maybe without recommendations. Maybe without reviews. **But they can still book.**

### Feature Flags for Degradation

In production, use **feature flags** to turn off non-critical features instantly:

```python
def get_movie_page(movie_id, user_id):
    movie = get_movie_details(movie_id)
    
    # Feature flag — can be toggled in real time
    if feature_flags.is_enabled("recommendations"):
        recommendations = recommendation_service.get(user_id)
    else:
        recommendations = []  # Disabled during high load
    
    if feature_flags.is_enabled("reviews"):
        reviews = review_service.get(movie_id)
    else:
        reviews = None
```

During Avengers launch, ops team **pre-disables** non-critical features:

```
⚡ Disabling: recommendations, reviews, seat heatmaps, social features
⚡ Keeping:   movie listings, seat maps, booking, payments
⚡ Reason:    Reduce load for peak traffic window
```

---

## Pattern 6 — Health Checks & Self-Healing 🏥

### "Know When You're Sick"

Every service exposes a `/health` endpoint:

```python
@app.get("/health")
def health_check():
    status = {
        "service": "booking-service",
        "status": "unknown"
    }
    
    # Check DB connectivity
    try:
        db.execute("SELECT 1")
        status["database"] = "healthy"
    except Exception as e:
        status["database"] = f"unhealthy: {e}"
        status["status"] = "degraded"
    
    # Check Redis connectivity
    try:
        redis.ping()
        status["redis"] = "healthy"
    except Exception as e:
        status["redis"] = f"unhealthy: {e}"
        status["status"] = "degraded"
    
    # Check Kafka connectivity
    try:
        kafka.list_topics(timeout=1.0)
        status["kafka"] = "healthy"
    except Exception as e:
        status["kafka"] = f"unhealthy: {e}"
        status["status"] = "degraded"
    
    if status["status"] == "unknown":
        status["status"] = "healthy"
    
    http_status = 200 if status["status"] == "healthy" else 503
    return Response(status, http_status)
```

Load balancer calls `/health` every 10 seconds:

```
Server healthy   → /health returns 200 → Keep sending traffic ✅
Server degraded  → /health returns 503 → Remove from rotation ❌
                                        → Kubernetes restarts it
                                        → Comes back healthy
                                        → Re-added to rotation ✅
```

This is **self-healing infrastructure.** Bad instances are automatically removed and replaced.

---

## Putting It All Together — The Resilience Stack

Every external call in our system is now wrapped with multiple layers:

```python
def charge_payment(user_id, amount, booking_id):
    
    # Layer 1: Rate limiting (protect against our own retry storms)
    if rate_limiter.is_limited("payment_service"):
        raise RateLimitError()
    
    # Layer 2: Circuit breaker (fail fast if service is down)
    if payment_circuit_breaker.state == "OPEN":
        raise CircuitOpenError("Payment service down")
    
    # Layer 3: Bulkhead (don't exhaust all threads)
    with payment_thread_pool.acquire(timeout=1.0):
        
        # Layer 4: Timeout (don't wait forever)
        with timeout(seconds=3.0):
            
            # Layer 5: Retry with exponential backoff + jitter
            return retry_with_backoff(
                func=razorpay.charge,
                args=(user_id, amount),
                kwargs={"idempotency_key": f"charge:{booking_id}"},
                max_retries=3,
                on_failure=payment_circuit_breaker.record_failure
            )
```

Five layers of protection around one external call.

---

## The Resilience Patterns — How They Work Together

```
Request comes in
      ↓
Rate Limiter: "Is this user/service sending too many requests?"
      → Yes: Reject immediately (HTTP 429) — protect the system
      → No: Continue
      ↓
Bulkhead: "Is this service's thread pool full?"  
      → Yes: Reject immediately — don't cascade
      → No: Acquire thread, continue
      ↓
Circuit Breaker: "Is the downstream service known-broken?"
      → OPEN: Fail immediately — don't waste time
      → CLOSED/HALF-OPEN: Continue
      ↓
Timeout: "Did the call take too long?"
      → Yes: Raise TimeoutError → goes to retry logic
      → No: Return result ✅
      ↓
Retry + Backoff: "Was this a transient failure?"
      → Yes (timeout/503): Wait with jitter, retry
      → No (400/card declined): Fail immediately
      ↓
Graceful Degradation: "Can we do a reduced version?"
      → Core feature failed: Return error to user
      → Nice-to-have failed: Return page without that feature ✅
```

---

## The Chaos Engineering Mindset 🔥

Netflix famously runs **Chaos Monkey** — a tool that randomly kills production servers to test resilience.

The philosophy:

> *"If we're not testing our failure handling, we'll discover its flaws at the worst possible time — during a real incident."*

For our system:

```
Chaos Test 1: Kill the payment service
  Expected: Circuit opens, users see friendly error, browsing still works

Chaos Test 2: Kill one DB shard
  Expected: Failover to replica within 30s, shows on other shards unaffected

Chaos Test 3: Fill Redis to 100% memory
  Expected: Cache misses, DB handles load, no data loss

Chaos Test 4: Introduce 5s latency on Razorpay
  Expected: Timeouts trigger, circuit opens, users informed

Chaos Test 5: Kill 30 of 50 app servers
  Expected: Load balancer routes to remaining 20, scales back up via auto-scaling
```

**If these tests pass, you sleep well during Avengers release.**

---

## 🎯 Interview Cheat Sheet for This Chapter

| Pattern | Problem Solved | Key Mechanism |
|---|---|---|
| **Timeout** | Threads blocked forever | Max wait time per call |
| **Retry + Backoff** | Transient failures | Exponential wait + jitter |
| **Circuit Breaker** | Cascading failure from dead service | Open/Closed/Half-Open states |
| **Bulkhead** | One slow operation starves others | Separate thread/connection pools |
| **Graceful Degradation** | Partial failure = total failure | Shed non-critical features |
| **Health Checks** | Bad instances get traffic | Auto-remove + restart unhealthy nodes |

---

## ✅ Concept Check

Can you answer these?
- Razorpay starts timing out. Walk through each resilience layer and what it does.
- What's the difference between a Circuit Breaker and a Retry? When does each help?
- Why is immediate retry after failure often worse than no retry?
- A heavy analytics query is slowing down all user bookings. Which pattern fixes this?
- Your recommendation service is down. Should the movie page fail? What pattern handles this?
- Why does jitter matter in exponential backoff?

---

**Ready for Chapter 9: Notifications & Async Architecture?** 📨

We'll cover how to send millions of emails and SMS messages reliably without slowing down the booking flow — using Kafka consumers, fan-out patterns, delivery guarantees, and handling the tricky problem of **exactly-once delivery.** Plus we'll tie together everything we've built into the final complete architecture diagram.

---

# Chapter 9: Notifications & Async Architecture — "The Art of Letting Go"

## 🔁 Recap of where we are

Booking is confirmed. Payment is done. Saga completed successfully.

Now the system needs to tell Rahul:

```
📧 Email:  "Your booking is confirmed! Seats A5, A6 for Pathaan 6PM"
📱 SMS:    "Booking confirmed! PVR Andheri, Jan 25, 6PM. Ref: BMS7001"  
🔔 Push:   "Your tickets are ready! Tap to view."
📄 PDF:    Ticket with QR code attached to email
```

The naive engineer says: *"Easy. Just send them after booking."*

```python
def confirm_booking(booking_id):
    db.update(booking_id, status="CONFIRMED")
    
    # Just do it right here
    email_service.send(user.email, booking_details)
    sms_service.send(user.phone, booking_details)
    push_service.send(user.device_token, booking_details)
```

The senior engineer stares at this and says:

> *"You just made the booking API depend on three external services. If any one of them is slow, the user waits. If any fails, does the booking fail? You've coupled things that should never be coupled."*

This chapter is about **decoupling** — one of the most important architectural principles.

---

## The Core Principle — "Fire and Forget" 🔫

The booking service has **one job:** confirm the booking.

Sending notifications is **someone else's job.**

The moment booking is confirmed, the booking service should:
1. Write to DB ✅
2. Put a message in Kafka ✅  
3. **Return response to user immediately** ✅
4. Forget about notifications entirely

The notification system picks up from there, **asynchronously**, completely independent.

```
SYNCHRONOUS (Wrong):
User clicks Book
      ↓
Booking Service:
  Write DB          (100ms)
  Send Email        (300ms) ← User waiting for this
  Send SMS          (200ms) ← User waiting for this
  Send Push         (150ms) ← User waiting for this
  Return response   ←─────────────── User waited 750ms extra!
  
ASYNCHRONOUS (Right):  
User clicks Book
      ↓
Booking Service:
  Write DB          (100ms)
  Publish to Kafka  (5ms)
  Return response   ←─────── User gets confirmation in 105ms ✅
  
Meanwhile, independently:
  Email Worker:  picks up event → sends email (300ms) — user doesn't wait
  SMS Worker:    picks up event → sends SMS   (200ms) — user doesn't wait
  Push Worker:   picks up event → sends push  (150ms) — user doesn't wait
```

User experience: **instant confirmation.** Notifications arrive seconds later. Nobody waited for nothing.

---

## The Kafka Fan-Out Pattern 📡

One booking confirmation event needs to trigger **multiple** notification types.

This is called **Fan-Out** — one message, multiple consumers.

```
Kafka Topic: "booking-confirmed"
      │
      │  BookingConfirmedEvent {
      │    booking_id: 7001,
      │    user_id: 6001,
      │    show_id: 5001,
      │    seats: ["A5", "A6"],
      │    amount: 450,
      │    movie: "Pathaan",
      │    theatre: "PVR Andheri",
      │    show_time: "2024-01-25 18:00"
      │  }
      │
      ├──► Email Consumer Group     → Sends email with PDF ticket
      ├──► SMS Consumer Group       → Sends SMS confirmation  
      ├──► Push Consumer Group      → Sends push notification
      ├──► Analytics Consumer Group → Updates booking metrics
      └──► Loyalty Consumer Group   → Awards reward points
```

Each consumer group reads the **same message independently.**

This is Kafka's superpower — one event, five different systems react to it, none aware of each other.

---

### Consumer Groups — The Key Kafka Concept

```
Topic: "booking-confirmed" has messages: [M1, M2, M3, M4, M5...]

Consumer Group "email-service":
  Worker1: reads M1, M2, M3
  Worker2: reads M4, M5, M6
  Each message read by ONE worker in the group ✅
  
Consumer Group "sms-service":
  Worker1: reads M1, M2, M3  ← Same messages! Independent offset
  Worker2: reads M4, M5, M6
  
Consumer Group "analytics":
  Worker1: reads M1, M2, M3, M4, M5, M6 (single worker, all messages)
```

Each consumer group maintains its **own offset** — its own pointer to "how far have I read?"

```
Topic:          [M1][M2][M3][M4][M5][M6][M7][M8]
                                              ↑
Email offset:                              here (processed up to M8)
SMS offset:                    ↑
                            here (processed up to M5, 3 behind — that's fine)
Analytics offset:   ↑
                 here (processing M1 — slow, but email/SMS unaffected)
```

**A slow analytics consumer never blocks email delivery.** Completely independent.

---

## Building the Email Service 📧

### What needs to happen

```
1. Receive BookingConfirmed event from Kafka
2. Fetch full booking details from DB
3. Generate PDF ticket with QR code
4. Render HTML email template
5. Send via email provider (SendGrid/SES)
6. Record that email was sent (for idempotency)
```

```python
class EmailConsumer:
    
    def __init__(self):
        self.kafka = KafkaConsumer(
            topic="booking-confirmed",
            group_id="email-service",
            auto_offset_reset="earliest"   # If we restart, reprocess from last committed offset
        )
    
    def run(self):
        for message in self.kafka:
            self.process_with_guarantee(message)
    
    def process_with_guarantee(self, message):
        event = json.loads(message.value)
        booking_id = event["booking_id"]
        
        # Idempotency check — did we already send this email?
        if self.already_sent(booking_id, "email"):
            self.kafka.commit()  # Mark as processed, skip
            return
        
        try:
            # Fetch full details
            booking = db.get_booking(booking_id)
            user    = db.get_user(booking.user_id)
            
            # Generate PDF ticket
            pdf = ticket_generator.create_pdf(booking)
            
            # Render email
            html = template_engine.render("booking_confirmation.html", {
                "user_name":   user.name,
                "movie":       booking.movie_title,
                "theatre":     booking.theatre_name,
                "show_time":   booking.show_time,
                "seats":       booking.seats,
                "amount":      booking.amount,
                "booking_ref": booking.id
            })
            
            # Send email
            email_provider.send(
                to=user.email,
                subject=f"Booking Confirmed — {booking.movie_title}",
                html=html,
                attachments=[pdf]
            )
            
            # Record success (for idempotency)
            self.record_sent(booking_id, "email")
            
            # Commit offset AFTER successful processing
            self.kafka.commit()
            
        except Exception as e:
            # Don't commit — Kafka will redeliver this message
            logger.error(f"Email failed for booking {booking_id}: {e}")
            raise
```

### The Critical Detail — When to Commit Offset

```
WRONG — Commit before processing:
  kafka.commit()       ← "I've handled this"
  email_service.send() ← Fails! Email never sent.
  Message is LOST — Kafka won't redeliver. 😱

RIGHT — Commit after processing:
  email_service.send() ← Succeeds ✅
  kafka.commit()       ← "Now I've handled this"
  
  If send() fails → no commit → Kafka redelivers → retry ✅
  If server crashes after send() but before commit() → 
    Kafka redelivers → idempotency check → "already sent" → skip ✅
```

**At-least-once delivery** — a message might be processed more than once, but **never zero times.** Idempotency handles the duplicates.

---

## The Delivery Guarantee Spectrum

This is a crucial interview concept:

```
At-most-once delivery:
  Message delivered 0 or 1 times.
  Commit BEFORE processing.
  "Fire and forget."
  Use for: Metrics, logs — losing one is okay
  Risk:    Messages can be LOST ❌

At-least-once delivery:
  Message delivered 1 or more times.
  Commit AFTER processing.
  Use idempotency to handle duplicates.
  Use for: Emails, SMS, payments — can't lose, handle duplicates
  Risk:    Duplicate processing (handled by idempotency) ✅

Exactly-once delivery:
  Message delivered exactly 1 time.
  Kafka supports this with transactions.
  Very expensive. High latency.
  Use for: Financial ledgers, inventory updates
  Risk:    Performance cost ⚠️
```

For our notification system: **At-least-once** is the right choice.

Better to send Rahul two confirmation emails than zero.

---

## Handling Failures — The Dead Letter Queue 💀

What happens when an email consistently fails to send?

```
Booking 7001 email fails
      ↓
Retry 1: fails (user's mailbox full)
      ↓
Retry 2: fails (mailbox still full)  
      ↓
Retry 3: fails
      ↓
If we keep retrying: this message blocks all subsequent emails in partition
```

Enter the **Dead Letter Queue (DLQ):**

```python
def process_with_retry(self, message, max_retries=3):
    event = json.loads(message.value)
    
    for attempt in range(max_retries):
        try:
            self.send_email(event)
            self.kafka.commit()
            return  # Success ✅
            
        except Exception as e:
            wait = 2 ** attempt  # 1s, 2s, 4s
            time.sleep(wait)
    
    # All retries exhausted — send to Dead Letter Queue
    self.kafka.produce(
        topic="booking-confirmed-dlq",  # Dead letter queue
        value=json.dumps({
            "original_event": event,
            "error": str(e),
            "attempts": max_retries,
            "failed_at": datetime.now().isoformat()
        })
    )
    
    # Commit original — move past it, don't block the queue
    self.kafka.commit()
    
    # Alert the on-call engineer
    alerting.send("EMAIL_DELIVERY_FAILED", event["booking_id"])
```

The DLQ is a **quarantine zone:**
- Failed messages go there instead of blocking the queue
- Engineers investigate why they failed
- Once fixed, messages can be **replayed** from DLQ back to the original topic
- Kafka's persistence makes this possible — messages aren't gone, just sidelined

---

## SMS Service — Rate Limits & Batching 📱

SMS providers (Twilio, MSG91) have rate limits:

```
MSG91 rate limit: 100 SMS/second per account
Our booking rate: potentially 500/second during peak
```

We need to **throttle** our SMS sending:

```python
class SMSConsumer:
    
    def __init__(self):
        self.rate_limiter = RateLimiter(
            max_calls=100,
            period=1.0  # 100 calls per second
        )
    
    def send_sms(self, booking):
        # Wait if we'd exceed rate limit
        with self.rate_limiter:
            sms_provider.send(
                to=booking.user_phone,
                message=self.format_sms(booking)
            )
    
    def format_sms(self, booking):
        # Keep it short — SMS is 160 chars
        return (
            f"Confirmed! {booking.movie_title} "
            f"{booking.show_date} {booking.show_time} "
            f"@ {booking.theatre_name}. "
            f"Seats: {', '.join(booking.seats)}. "
            f"Ref: {booking.id}"
        )[:160]
```

**Batching for efficiency:**

Instead of one API call per SMS, batch them:

```python
# Inefficient — one API call per SMS
for booking in bookings:
    sms_provider.send(booking.phone, booking.message)

# Efficient — one API call for 100 SMS
sms_provider.send_batch([
    {"to": b.phone, "message": b.message} 
    for b in bookings
])
```

100x fewer API calls. 100x less overhead.

---

## Push Notifications — The Fan-Out at Scale 🔔

Push is trickier. A user might have:
- iPhone (APNs — Apple Push Notification Service)
- Android (FCM — Firebase Cloud Messaging)
- Multiple devices

```python
class PushConsumer:
    
    def send_push(self, booking):
        user = db.get_user(booking.user_id)
        
        # Get all devices for this user
        devices = db.query("""
            SELECT device_token, platform 
            FROM user_devices 
            WHERE user_id = :uid AND is_active = true
        """, uid=booking.user_id)
        
        for device in devices:
            try:
                if device.platform == "iOS":
                    apns.send(
                        token=device.device_token,
                        title="Booking Confirmed! 🎬",
                        body=f"{booking.movie_title} — {booking.show_time}",
                        data={"booking_id": booking.id}
                    )
                elif device.platform == "Android":
                    fcm.send(
                        token=device.device_token,
                        notification={
                            "title": "Booking Confirmed! 🎬",
                            "body": f"{booking.movie_title} — {booking.show_time}"
                        },
                        data={"booking_id": str(booking.id)}
                    )
                    
            except InvalidTokenError:
                # Device token expired (user uninstalled app)
                db.execute("""
                    UPDATE user_devices 
                    SET is_active = false 
                    WHERE device_token = :token
                """, token=device.device_token)
```

---

## Notification Preferences — "Don't Spam Me" ⚙️

Not every user wants every notification. We need preference management:

```sql
user_notification_preferences
| user_id | channel | event_type        | enabled |
|---------|---------|-------------------|---------|
| 6001    | email   | booking_confirmed | true    |
| 6001    | sms     | booking_confirmed | true    |
| 6001    | push    | promotional       | false   | ← Rahul hates promos
| 6001    | email   | show_reminder     | true    |
| 6002    | sms     | booking_confirmed | false   | ← Priya prefers email only
```

The notification service checks preferences before sending:

```python
def should_send(user_id, channel, event_type):
    pref = db.get_preference(user_id, channel, event_type)
    
    if pref is None:
        return DEFAULT_PREFERENCES[event_type][channel]  # Sensible default
    
    return pref.enabled
```

---

## The Reminder Notification — Scheduled Jobs ⏰

24 hours before a show:

```
"🎬 Reminder: Pathaan is tomorrow at 6 PM at PVR Andheri! 
  Your seats: A5, A6. Don't forget your e-ticket!"
```

This isn't triggered by an event — it's **time-based.**

```python
# Runs every hour
def schedule_reminders():
    tomorrow = datetime.now() + timedelta(hours=24)
    
    # Find all shows happening in ~24 hours
    upcoming_shows = db.query("""
        SELECT b.*, u.email, u.phone
        FROM bookings b
        JOIN users u ON b.user_id = u.user_id
        JOIN shows s ON b.show_id = s.show_id
        WHERE s.show_date = :date
          AND s.start_time BETWEEN :start AND :end
          AND b.status = 'CONFIRMED'
          AND b.reminder_sent = false
    """, date=tomorrow.date(),
         start=tomorrow.time(),
         end=(tomorrow + timedelta(hours=1)).time())
    
    for booking in upcoming_shows:
        # Publish to Kafka — let notification service handle delivery
        kafka.produce("send-reminder", {
            "booking_id": booking.id,
            "reminder_type": "24h_before_show"
        })
        
        # Mark reminder as scheduled (prevent duplicate scheduling)
        db.execute("""
            UPDATE bookings SET reminder_sent = true 
            WHERE booking_id = :id
        """, id=booking.id)
```

This job runs on a **Cron scheduler** (like Kubernetes CronJob). Not on every app server — only one instance runs at a time to prevent duplicate scheduling.

---

## The Complete Async Architecture

```
                    BOOKING CONFIRMED
                           ↓
                    Outbox Processor
                           ↓
              Kafka Topic: "booking-confirmed"
                           │
         ┌─────────────────┼──────────────────────┐
         ↓                 ↓                       ↓                  ↓                    ↓
  Email Consumer    SMS Consumer         Push Consumer      Analytics Consumer    Loyalty Consumer
       │                  │                    │                    │                    │
  Fetch booking      Format SMS           Check devices        Update metrics      Award points
  Generate PDF       Rate limit           Send APNs/FCM        Show sold%          +50 pts
  Render template    Send batch           Handle bad tokens    Revenue tracking    Level up check
  Send via SES       Record sent          Record sent          Kafka → ClickHouse  Update DB
  Record sent        ↓                   ↓                    ↓                   ↓
       ↓          DLQ if fails        DLQ if fails         Always succeeds    DLQ if fails
  DLQ if fails
  
                    All independent. All async. All resumable.
```

---

## Scaling the Notification System

During Avengers release: 100,000 bookings in first minute.

```
100,000 BookingConfirmed events in Kafka
            ↓
Email Consumer Group:
  Partition 0 → Worker 1
  Partition 1 → Worker 2
  ...
  Partition 20 → Worker 20   ← 20 workers in parallel
  = 20x throughput
            ↓
But SES rate limit: 14 emails/second/account
  → Need to request rate limit increase
  → OR: Multiple SES accounts (different regions)
  → OR: Spread delivery over 5 minutes — nobody notices 30s delay
```

Kafka partitions are the **unit of parallelism.** More partitions = more workers = more throughput. But you can't have more workers than partitions for one consumer group.

```
Rule: Workers ≤ Partitions

Topic with 20 partitions:
  20 workers → each handles 1 partition ← optimal
  10 workers → each handles 2 partitions ← fine
  30 workers → 10 workers idle ← wasteful, 20 still max throughput
```

---

## 🎯 Interview Cheat Sheet for This Chapter

| Question | Answer |
|---|---|
| Why async for notifications? | Decouples booking from notification. Booking is instant. Notifications come separately. |
| What is fan-out? | One Kafka event consumed by multiple independent consumer groups |
| At-least-once vs exactly-once? | At-least-once: commit after processing + idempotency. Exactly-once: Kafka transactions, expensive. |
| What is a Dead Letter Queue? | Quarantine for persistently failing messages. Prevents one bad message blocking the queue. |
| When to commit Kafka offset? | AFTER successful processing. Never before. |
| How to scale consumers? | Add more partitions + more workers. Workers ≤ Partitions per group. |
| How to handle push notification token expiry? | Catch InvalidTokenError → mark device inactive in DB |
| How to send 100k SMS without hitting rate limits? | Batch API calls + rate limiter + spread over time |

---

## ✅ Concept Check

Can you answer these?
- Booking confirmed. Server crashes before Kafka message is published. What happens? (Hint: Outbox pattern from Ch. 7)
- Email worker processes a message, sends email successfully, then crashes before committing offset. What happens on restart?
- 5 different teams want to react to a BookingConfirmed event. How do you structure Kafka consumer groups?
- A user has 3 devices. Two tokens are valid, one expired. Walk through the push notification flow.
- Your SMS provider rate limits you at 100/second. You have 10,000 SMS to send. How do you handle this?

---

## 🏛️ The Final Architecture — Everything Together

Let's zoom out and see the complete system we've built over 9 chapters:

```
                            USERS (10M/day)
                                  │
                    ┌─────────────┴──────────────┐
                    ↓                            ↓
                   CDN                      Mobile App
              (Static assets)              (iOS/Android)
                    │                            │
                    └─────────────┬──────────────┘
                                  ↓
                         Load Balancer (HA)
                        Active ←→ Standby
                                  │
              ┌───────────────────┼───────────────────┐
              ↓                   ↓                   ↓
           App1                App2     ...        App50
        (Stateless)          (Stateless)         (Stateless)
              │                   │                   │
              └───────────────────┼───────────────────┘
                                  │
              ┌───────────────────┼───────────────────┐
              ↓                   ↓                   ↓
        Rate Limiter          Redis Cluster        Feature Flags
        (sliding window)    ┌──────────────┐
                            │ Cache        │
                            │ Seat holds   │
                            │ Seat tokens  │
                            │ Rate limits  │
                            └──────────────┘
                                  │
                         Saga Orchestrator
                                  │
              ┌───────────────────┼───────────────────┐
              ↓                   ↓                   ↓
        Seat Service        Payment Service     Booking Service
        (holds, tokens)     (Razorpay + CB)     (saga state)
              │                   │                   │
              └───────────────────┼───────────────────┘
                                  ↓
                           Sharded DB Cluster
                    ┌─────────────┬─────────────┐
                    ↓             ↓             ↓
                 Shard 1       Shard 2       Shard 3
                M + 2R        M + 2R        M + 2R
                    │
              Outbox Table
                    ↓
             Outbox Processor
                    ↓
    ┌───────── Kafka ──────────────────────────┐
    │   booking-confirmed                      │
    │   payment-events                         │
    │   booking-requests (write queue)         │
    └──────────────────────────────────────────┘
              │              │              │
         Email            SMS           Push/Analytics/Loyalty
         Worker           Worker        Workers
              │              │              │
           SES/SG          MSG91         FCM/APNs
              │
         Dead Letter Queues (for all channels)
              │
    ┌─────────────────────┐
    │  Monitoring Stack   │
    │  Prometheus/Grafana │
    │  Alerts + On-call   │
    └─────────────────────┘
```

---

## The Full Request Journey — One Last Time

**Rahul books Seat A5 for Pathaan 6PM:**

```
Ch.5  → Rate limiter: ✅ within limits
Ch.5  → Redis LPOP: grabbed seat token for A5
Ch.2  → Redis SET hold:show5001:A5, TTL=600s
Ch.8  → Circuit breaker: payment service CLOSED ✅
Ch.7  → Saga starts, state saved
Ch.3  → Shard router: show5001 → Shard 2
Ch.3  → Optimistic lock: UPDATE show_seats version 7→8
Ch.7  → Payment: Razorpay charged ₹450, idempotency_key=charge:7001
Ch.7  → DB transaction: booking CONFIRMED + outbox event inserted
Ch.4  → Cache invalidated: DEL seatmap:5001
Ch.9  → Outbox processor: publishes to Kafka
Ch.9  → Email worker: PDF ticket generated, sent via SES
Ch.9  → SMS worker: "Confirmed! Pathaan 6PM..." sent
Ch.9  → Push worker: notification to Rahul's phone
Ch.6  → WAL streamed to Shard 2 replicas
Ch.6  → Replicas serve future reads for this booking
```

**Every chapter contributed one layer. Together: a production-grade system.**

---

## What We've Covered — Your Interview Toolkit

| Chapter | Concept | Key Interview Points |
|---|---|---|
| 1 | Problem Definition | Scope, requirements, estimation |
| 2 | Seat Locking | Race conditions, optimistic/pessimistic locking, Redis holds |
| 3 | Schema Design | Normalization, show_seats table, indexes |
| 4 | Caching | What to cache, TTL, invalidation, stampede |
| 5 | Scale Under Load | Horizontal scaling, queues, rate limiting, hotspots |
| 6 | Sharding & Replication | Consistent hashing, replication lag, failover |
| 7 | Payments | Saga pattern, idempotency, outbox pattern |
| 8 | Resilience | Circuit breaker, bulkhead, graceful degradation |
| 9 | Notifications | Async architecture, fan-out, delivery guarantees, DLQ |

---

**You now have everything needed to answer a ticket booking system design question in depth — from the first requirement to the last notification.**

Every concept explained through *why it was needed*, not just *what it is*. That's what makes the difference in interviews. 🎯
