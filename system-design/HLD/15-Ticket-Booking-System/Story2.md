Perfect. This is **exactly the right way** to learn HLD for product-company interviews (Google / Uber / Amazon style). Since you're preparing for **system design interviews**, we’ll structure this like an **interview conversation + engineering story**.

We will **build the system step-by-step like engineers at BookMyShow did over time**.

At every step we will discuss:

• Problem that appeared
• Why the current design breaks
• What engineers tried
• New architecture
• Tradeoffs
• Interview talking points

And we **won't jump ahead** until this step is fully clear.

---

# Story: How Ticket Booking Systems Were Built

Imagine the year is **2005**.

Multiplex theatres are increasing in India.

Before platforms like **BookMyShow**, ticket booking worked like this:

People physically went to theatres.

```
User → Ticket Counter → Staff checks seats → prints ticket
```

Problems:

• Long queues
• Tickets sold out before reaching counter
• No way to check availability remotely

So companies decided:

> Let's build a **website where people can see shows and book seats online**.

That was the **first version of BookMyShow**.

---

# Step 1 — The Simplest Possible System

Let's start with the **most naive architecture**.

User flow:

```
User opens website
↓
Search movie
↓
Select theatre
↓
Select showtime
↓
See available seats
↓
Select seats
↓
Pay
↓
Ticket booked
```

---

# Basic Architecture (Version 1)

```
Users
  ↓
Load Balancer
  ↓
Web Servers
  ↓
Application Server
  ↓
Database
```

Database tables:

### Movies

```
movie_id
name
duration
language
```

---

### Theatres

```
theatre_id
name
city
```

---

### Screens

```
screen_id
theatre_id
capacity
```

---

### Shows

```
show_id
movie_id
screen_id
start_time
```

---

### Seats

```
seat_id
screen_id
row
number
```

---

### Bookings

```
booking_id
user_id
show_id
seat_id
status
```

---

# How Seat Booking Works (Naive Version)

User chooses seat **A10**.

Application runs:

```
SELECT * FROM bookings
WHERE show_id = 101
AND seat_id = A10
```

If **no record exists** → seat available.

Then insert booking.

```
INSERT INTO bookings
(show_id, seat_id, status)
VALUES (101, 'A10', 'CONFIRMED')
```

---

# Example

User1 tries to book:

```
Avengers
Inorbit Mall
7:00 PM
Seat A10
```

System checks DB.

Seat not booked → confirm.

---

# Works Fine For Small Traffic

For **100 users** this system works.

But then **a huge movie releases**.

Example:

```
RRR
Baahubali
Avengers
KGF
Pushpa
```

Now imagine:

**50,000 users trying to book SAME SHOW**

This causes a **serious problem**.

---

# The First Big Disaster

Two users click **A10 at the same time**.

```
User A checks DB → A10 available
User B checks DB → A10 available
```

Both insert booking.

Now:

```
User A → confirmed
User B → confirmed
```

But only **1 seat exists**.

This is called:

# Double Booking Problem

Very famous **distributed systems problem**.

Ticketing systems **live or die** based on solving this.

Even **Ticketmaster had outages** because of this.

---

# Why This Happens

Because operations are:

```
Check seat
Then book seat
```

Two separate steps.

Between them **race condition occurs**.

---

# Race Condition Timeline

```
User A → check seat → available
User B → check seat → available

User A → insert booking
User B → insert booking
```

Result:

```
2 bookings for same seat
```

---

# This is the FIRST HLD challenge interviewers expect.

They will ask:

> "How will you prevent double booking?"

---

# Before We Solve It

Let's think like engineers.

Possible solutions:

1️⃣ Lock the seat
2️⃣ Database transactions
3️⃣ Distributed locking
4️⃣ Temporary seat reservation
5️⃣ Queue based booking
6️⃣ Optimistic concurrency control

Real systems use **multiple of these together**.

---

# Before moving forward

First confirm you clearly understand:

1️⃣ **Why double booking happens**

2️⃣ **Why simple DB check fails**

3️⃣ **What race condition means**

4️⃣ **Why ticket systems are hard**

---

Then in the **next step** we will implement the **FIRST REAL SOLUTION**:

# Seat Locking System

This introduces:

• distributed locking
• temporary reservation
• seat hold expiry
• payment timeout

This is **the core of ticket booking architecture**.

And it’s **one of the most asked HLD topics in interviews**.

---

Reply **"continue"** when ready.

Next we will build:

**Seat Locking + Reservation System (Real BookMyShow Design)**.

And this is where the system starts becoming **interesting and interview-level**.

---

Great. Now we reach the **first real engineering solution** used in ticket booking systems.

This is the **core concept interviewers expect** when you design **BookMyShow / Ticketmaster / District systems**.

---

# Step 2 — Seat Locking (Temporary Reservation)

Engineers realized the root problem:

> Multiple users can attempt to book the same seat at the same time.

So they introduced a new concept:

**Seats are not directly booked.
They are first temporarily locked.**

This is called **Seat Reservation / Seat Hold**.

---

# Real Life Analogy

When you book tickets on BookMyShow:

You select seats.

Then you see a message:

```
You have 5 minutes to complete payment
```

During this time:

```
No other user can select those seats
```

That means:

The seats are **temporarily locked**.

---

# New Booking Flow

Instead of:

```
Check seat → Book seat
```

The system becomes:

```
Check seat
↓
Lock seat temporarily
↓
User completes payment
↓
Seat becomes permanently booked
```

---

# New Booking States

Seats now have **three states**.

```
AVAILABLE
LOCKED
BOOKED
```

Example:

| Seat | Status    |
| ---- | --------- |
| A1   | AVAILABLE |
| A2   | LOCKED    |
| A3   | BOOKED    |

---

# Database Design Change

We add a **seat_lock table**.

### Seat Locks

```
seat_lock
---------
lock_id
seat_id
show_id
user_id
lock_time
expiry_time
```

Example:

```
seat_id : A10
show_id : 101
user_id : U1
expiry_time : 10:05 PM
```

Meaning:

```
Seat A10 is locked by user U1 until 10:05
```

---

# Updated Booking Flow

### Step 1 — User selects seat

User clicks **A10**.

Backend tries to lock the seat.

```
INSERT INTO seat_lock
(seat_id, show_id, user_id, expiry_time)
VALUES (A10, 101, U1, NOW()+5min)
```

If insert succeeds → seat locked.

---

### Step 2 — Seat becomes invisible to others

Now when another user checks seat availability:

System checks:

```
seat_lock table
```

If seat exists and **not expired**, seat is unavailable.

---

### Step 3 — User pays

User completes payment.

Now system creates booking.

```
INSERT INTO bookings
(show_id, seat_id, user_id)
VALUES (101, A10, U1)
```

Then remove lock.

```
DELETE FROM seat_lock
WHERE seat_id = A10
```

Seat status:

```
BOOKED
```

---

# What If User Doesn't Pay?

Very common scenario.

User selects seats but **abandons payment**.

Without handling this:

Seats would remain locked forever.

So we add:

# Lock Expiry

Example:

```
lock_time = 10:00
expiry_time = 10:05
```

If payment not completed:

Seat becomes **available again**.

---

# How Expired Locks Are Cleared

Two approaches exist.

---

## Approach 1 — Background Cleaner Job

Every few seconds run:

```
DELETE FROM seat_lock
WHERE expiry_time < NOW()
```

This frees expired seats.

---

## Approach 2 — Check Expiry During Seat Lookup

When checking seat:

```
SELECT *
FROM seat_lock
WHERE seat_id = A10
AND expiry_time > NOW()
```

If expired → treat as available.

---

# But Another Problem Appears

Imagine:

```
RRR ticket launch
10:00 AM
```

100,000 users click seats simultaneously.

Every click does:

```
INSERT INTO seat_lock
```

Database becomes a **hotspot**.

---

# Database Hotspot Problem

All users trying to lock seats for the same show.

Example:

```
Show 101
Theatre: IMAX
Seats: 300
Users trying: 50,000
```

Massive contention on database rows.

This causes:

• DB CPU spike
• Lock contention
• Slow queries

---

# Real Systems Don't Use DB Locks

Instead they use **in-memory locking systems**.

Why?

Because:

```
Database writes are slow
Memory operations are fast
```

Example technologies:

```
Redis
Memcached
Aerospike
```

---

# New Architecture (Seat Locking)

```
Users
 ↓
Load Balancer
 ↓
Booking Service
 ↓
Redis (seat locks)
 ↓
Database (final bookings)
```

Redis handles **temporary locks**.

Database stores **final booking records**.

---

# Example Redis Lock

```
key   : show101:A10
value : user123
TTL   : 5 minutes
```

Redis command:

```
SET show101:A10 user123 NX EX 300
```

Meaning:

```
Set key only if not exists
Expire in 300 seconds
```

This ensures **atomic locking**.

---

# Why Redis is Perfect Here

Redis operations are:

```
atomic
single-threaded
extremely fast
```

Lock creation takes:

```
< 1 millisecond
```

Databases cannot match that.

---

# Updated Booking Flow (Production Style)

### Step 1

User selects seat.

Booking service calls Redis:

```
SET seat_lock NX
```

If success → lock acquired.

---

### Step 2

User goes to payment.

---

### Step 3

Payment successful.

Booking service writes:

```
booking table
```

---

### Step 4

Redis lock deleted.

---

# Important Interview Point

Seat locking is:

```
strong consistency problem
```

But we allow:

```
eventual consistency for seat display
```

Meaning:

Seat UI may lag slightly.

But **booking must be correct**.

---

# But A New Problem Appears Again

Imagine this scenario:

User locks seats.

Payment gateway takes **10 seconds**.

During this time:

```
lock expires
```

Another user books the seat.

Then first user's payment succeeds.

Now what?

This causes **payment-success but booking-failed problem**.

This is a **real production issue**.

---

# Next Step (Very Important)

In the next step we will introduce:

# Booking Workflow System

This includes:

• Payment service
• Booking service
• distributed transactions
• idempotency
• retry logic

This is where systems start looking like **real-world architecture**.

---

Before we move forward, confirm you understand:

1️⃣ Why **seat locking is needed**

2️⃣ Why **database locking doesn't scale**

3️⃣ Why **Redis is used**

4️⃣ How **TTL locks work**

5️⃣ What happens if payment takes longer

---

Reply **"continue"** once clear.

Next we will design:

**Payment + Booking workflow (most interviewers stop here if you explain well).**

---

Excellent. Now we enter the **most important part of a ticket booking system**.

Many candidates stop at **Redis seat locking**, but **senior-level interviews (6+ years like yours)** expect you to understand the **full booking workflow**, including:

* payment reliability
* distributed transactions
* idempotency
* failure recovery

This is where systems become **real production systems**.

---

# Step 3 — The Payment & Booking Workflow Problem

Let's revisit our current system.

Current flow:

```
User selects seat
↓
Seat locked in Redis (5 min)
↓
User goes to payment gateway
↓
Payment successful
↓
Booking written to DB
```

Looks fine.

But real systems fail in **many edge cases**.

---

# Failure Scenario 1 — Payment Success but Booking Fails

Timeline:

```
User locks seat
↓
User pays successfully
↓
Booking service crashes
↓
Seat not recorded as booked
```

Now we have:

```
User money deducted
BUT no ticket
```

This is **unacceptable**.

Users will flood customer support.

---

# Failure Scenario 2 — Payment Gateway Timeout

```
User pays
↓
Gateway responds slowly
↓
System times out
↓
User retries payment
```

Now we may get:

```
2 payment confirmations
```

This causes **duplicate bookings**.

---

# Failure Scenario 3 — Lock Expiry During Payment

Example timeline:

```
10:00:00 seat locked
10:04:59 user paying
10:05:00 lock expires
10:05:01 another user books seat
10:05:05 payment success arrives
```

Now:

```
Two users paid
Only one seat exists
```

Very dangerous scenario.

---

# Engineers Realized

Booking is not a **single operation**.

It is a **workflow** involving multiple systems.

```
Seat Lock
Payment
Booking Confirmation
Notification
```

These systems must be **coordinated safely**.

---

# Solution — Booking Workflow Orchestration

Instead of letting services act independently, we introduce a **Booking Service** that orchestrates the process.

New architecture:

```
Users
↓
API Gateway
↓
Booking Service
↓
Redis (seat locks)
↓
Payment Service
↓
Booking DB
```

The **Booking Service controls the entire flow**.

---

# Booking State Machine

We introduce booking states.

```
INITIATED
SEATS_LOCKED
PAYMENT_PENDING
PAYMENT_SUCCESS
BOOKED
FAILED
EXPIRED
```

Example record:

| booking_id | seat | status          |
| ---------- | ---- | --------------- |
| B123       | A10  | PAYMENT_PENDING |

---

# Updated Booking Flow

### Step 1 — Create Booking

User selects seats.

System creates booking:

```
booking_id = B123
status = INITIATED
```

---

### Step 2 — Lock Seats

Booking service locks seats in Redis.

If successful:

```
status = SEATS_LOCKED
```

---

### Step 3 — Start Payment

User redirected to payment gateway.

```
status = PAYMENT_PENDING
```

---

### Step 4 — Payment Callback

Payment gateway sends webhook.

Example:

```
POST /payment/callback
{
 payment_id: P456
 booking_id: B123
 status: SUCCESS
}
```

---

### Step 5 — Confirm Booking

Booking service verifies:

```
Are seats still locked?
```

If yes:

```
Write final booking
status = BOOKED
```

Then remove Redis lock.

---

# Why Booking ID is Important

Booking ID acts as a **single source of truth**.

Everything references:

```
booking_id
```

Instead of seat + user combinations.

This simplifies:

* retries
* reconciliation
* auditing

---

# Idempotency (Extremely Important)

Payment systems often send **duplicate callbacks**.

Example:

```
Payment webhook called twice
```

Without protection:

```
Booking created twice
```

Solution:

Make booking confirmation **idempotent**.

Example logic:

```
if booking.status == BOOKED
   ignore request
```

Or enforce DB constraint.

---

# Database Protection

Add unique constraint:

```
UNIQUE(show_id, seat_id)
```

Now database itself prevents double booking.

Even if application fails.

---

# Example Table

Bookings table:

```
booking_id
user_id
show_id
seat_id
status
payment_id
created_at
```

Constraint:

```
UNIQUE(show_id, seat_id)
```

This is **final safety net**.

---

# Handling Lock Expiry During Payment

If lock expired before payment confirmation:

Booking service detects:

```
seat not locked anymore
```

Now system must:

```
refund payment
```

So booking becomes:

```
status = FAILED
```

This is why **payment reconciliation jobs exist**.

---

# Payment Reconciliation Job

Every few minutes:

System scans:

```
payments with success
but booking not confirmed
```

Then tries to fix.

Either:

```
confirm booking
or refund
```

This protects system from **partial failures**.

---

# Final Booking Workflow

Full flow now looks like this:

```
User selects seats
↓
Booking created
↓
Seats locked (Redis)
↓
Payment started
↓
Payment gateway
↓
Webhook received
↓
Booking confirmed in DB
↓
Lock released
↓
Ticket issued
```

This design ensures:

```
No double booking
Payment safety
Failure recovery
```

---

# Important Interview Takeaway

If interviewer asks:

**"How do you handle payment and booking consistency?"**

You answer:

> Use a **booking workflow with states**, **idempotent payment callbacks**, and **DB constraints as final protection**.

That answer alone signals **strong system design knowledge**.

---

# But Another Problem Appears

Everything works fine.

Until a **massive movie release** happens.

Example:

```
Avengers Endgame
Baahubali
RRR
```

Traffic spike:

```
2 million users
within 30 seconds
```

Now system faces **new bottlenecks**:

* seat search queries overload DB
* show listings slow
* seat map requests explode
* booking service overwhelmed

This introduces the **next major HLD topic**:

# Scaling the Read Traffic

We must scale:

```
Search movies
List theatres
Show seat maps
```

These are **read-heavy workloads**.

Solutions include:

* caching
* CDN
* read replicas
* precomputation

---

Before we move to scaling, confirm you understand these concepts clearly:

1. **Why booking workflow is needed**
2. **Why idempotency is critical**
3. **Why DB unique constraints are final protection**
4. **Why reconciliation jobs exist**

---

Reply **"continue"** and we will move to the **next major HLD concept**:

# Scaling to Millions of Users (Caching + Read Scaling)

This is where systems start looking like **Netflix/Uber level architectures**.

---

Excellent. Now we enter a **very important HLD section** that interviewers expect when traffic becomes huge.

So far we solved:

* ✅ Double booking
* ✅ Seat locking
* ✅ Payment consistency

But the system **still cannot handle millions of users**.

Why?

Because **most traffic is READ traffic**.

---

# Step 4 — Scaling Read Traffic

In real ticket systems:

```text
~95–99% of traffic = READS
~1–5% of traffic = BOOKINGS
```

Example:

For a blockbuster release:

```text
Users opening app        → 2,000,000
Users viewing shows      → 1,500,000
Users opening seat map   → 800,000
Users actually booking   → 30,000
```

So the problem is **not booking**, it's **information retrieval**.

---

# What Are The Main Read APIs?

Typical APIs in BookMyShow:

```text
GET /movies
GET /theatres?city=Hyderabad
GET /shows?movie=Avengers
GET /seats?show=101
```

If every request hits the database:

```text
DB will crash
```

So engineers introduce **caching**.

---

# First Optimization — CDN for Static Content

Many resources are **static**.

Example:

* movie posters
* movie metadata
* theatre information
* trailers

Instead of serving from backend:

```text
User → CDN → Static Content
```

Architecture:

```text
Users
 ↓
CDN (Cloudflare/Akamai)
 ↓
Backend
```

Benefits:

* Reduces backend load
* Reduces latency globally

Example:

```text
Movie posters
Trailer thumbnails
Static JSON files
```

Interview tip:

> Always mention **CDN for static assets**.

---

# Second Optimization — Application Cache

Now we cache **frequently requested data**.

Example queries:

```sql
SELECT * FROM movies WHERE city='Hyderabad'
SELECT * FROM shows WHERE movie_id=101
```

These are **repeated thousands of times**.

So we cache them.

---

# New Architecture

```text
Users
 ↓
Load Balancer
 ↓
API Servers
 ↓
Redis Cache
 ↓
Database
```

Request flow:

```text
User requests shows
↓
Check Redis
↓
Cache hit → return immediately
↓
Cache miss → query DB → store in Redis
```

This is called:

# Cache-Aside Pattern

Flow:

```text
Read → Cache
If miss → DB
Write to Cache
```

---

# Example Cache Keys

```text
movies:hyderabad
shows:movie_101
theatres:hyderabad
```

Redis entry example:

```json
Key: shows:movie_101

Value:
[
 {show_id:101, time:7PM},
 {show_id:102, time:10PM}
]
```

---

# Why Redis Again?

Because Redis is:

```text
In-memory
Extremely fast (~1 ms)
Supports TTL
Handles millions of ops/sec
```

Perfect for **read-heavy systems**.

---

# Third Optimization — Seat Map Precomputation

Seat map queries are expensive.

Example seat layout:

```text
Screen 1
A1 A2 A3 A4 A5
B1 B2 B3 B4 B5
C1 C2 C3 C4 C5
```

For each seat we must check:

```sql
SELECT seat
FROM bookings
WHERE show_id=101
```

If 800k users request seat maps:

```text
DB meltdown
```

So we **precompute seat maps**.

---

# Precomputed Seat Map

Instead of computing live:

We store seat availability in **Redis**.

Example:

```json
show:101:seats

{
 A1: available
 A2: booked
 A3: locked
}
```

Now seat map request becomes:

```text
Redis lookup
```

Latency:

```text
< 1 ms
```

Instead of expensive DB joins.

---

# Updating Seat Map

When seat locked:

```text
Redis update
```

When seat booked:

```text
Redis update
```

So Redis becomes **real-time seat availability store**.

Database remains **source of truth**.

---

# Fourth Optimization — Read Replicas

Even with caching, some queries hit DB.

Example:

```text
Booking history
Payment status
User bookings
```

If DB handles both reads and writes:

```text
Write performance drops
```

So we separate:

```text
Primary DB → writes
Replica DB → reads
```

Architecture:

```text
               ┌──────────┐
               │ Primary  │
               │ Database │
               └─────┬────┘
                     │ replication
         ┌───────────┴───────────┐
         │                       │
   Read Replica 1          Read Replica 2
```

Application routes:

```text
Writes → Primary
Reads → Replicas
```

Benefits:

* write performance preserved
* read scalability increases

---

# But Replication Introduces A New Issue

Replication is usually **asynchronous**.

Meaning:

```text
Primary updated
Replica updated later
```

This causes:

# Read After Write Inconsistency

Example:

```text
User books ticket
Immediately checks booking history
Replica doesn't have latest data
```

User sees:

```text
No booking found
```

Even though booking succeeded.

---

# How Real Systems Handle This

Solutions include:

### 1️⃣ Read From Primary After Write

For critical reads.

Example:

```text
after booking confirmation
```

---

### 2️⃣ Sticky Sessions

User temporarily reads from primary.

---

### 3️⃣ Short TTL Caching

Booking confirmation cached for user.

---

# Current System Architecture

At this stage the architecture becomes:

```text
Users
 ↓
CDN
 ↓
Load Balancer
 ↓
API Servers
 ↓
Redis Cache
 ↓
Booking Service
 ↓
Redis Seat Locks
 ↓
Primary DB
 ↓
Read Replicas
```

Now system can handle **millions of users**.

---

# Interview Tip

If interviewer asks:

**"How do you scale read traffic?"**

Good answer includes:

* CDN
* Redis caching
* precomputed seat maps
* read replicas

That signals **mature system design thinking**.

---

# But The System Still Has A Major Problem

Now imagine **Avengers ticket release at 9 AM**.

Millions of users try to book **same show**.

This causes:

```text
Extreme contention
on same seat keys
```

Example:

```text
show101:A10
```

Millions of lock attempts.

Redis itself can become **hotspot**.

This introduces the next **advanced distributed systems concept**:

# Partitioning / Sharding

We must distribute load across:

```text
multiple booking services
multiple Redis nodes
multiple databases
```

---

Next we will design:

# Global Scale Architecture

Including:

* Sharding strategy
* Seat partitioning
* Consistent hashing
* Distributed booking services

This is where systems become **large-scale distributed systems**.

---

If you're ready, reply **"continue"**.

The next section will explain **how BookMyShow handles millions of users during ticket releases**.

---

Excellent. Now we move into **true large-scale distributed system design**, which is what interviewers expect from **senior engineers**.

So far we solved:

✅ Seat locking
✅ Payment workflow
✅ Read scaling with cache
✅ Read replicas

But during **blockbuster releases**, another problem appears.

---

# Step 5 — The Hotspot Problem

Imagine this scenario.

Movie release:

```
RRR Ticket Launch
10:00 AM
```

Traffic spike:

```
2 million users
within 10 seconds
```

Everyone is trying to book the **same show**.

Example show:

```
show_id = 101
IMAX Screen
300 seats
```

Now look at the Redis keys we created earlier:

```
show101:A1
show101:A2
show101:A3
...
show101:A300
```

Millions of users are trying to lock **these 300 keys**.

---

# What Happens Internally

Requests arriving:

```
User1 → lock A10
User2 → lock A10
User3 → lock A10
User4 → lock A10
...
```

Redis node receives **massive concurrent traffic**.

Result:

```
CPU spike
network saturation
slow responses
```

Even Redis can become a **hotspot**.

---

# Engineers Realized

We must **spread the load across many machines**.

This concept is called:

# Sharding

Sharding means:

```
Split data across multiple servers
```

Instead of one Redis node:

```
Redis-1
Redis-2
Redis-3
Redis-4
```

Each server handles **subset of data**.

---

# Sharding Example

Seats distributed across Redis nodes.

```
Redis-1 → seats A1–A75
Redis-2 → seats A76–A150
Redis-3 → seats A151–A225
Redis-4 → seats A226–A300
```

Now lock attempts distribute across machines.

Result:

```
Load distributed
Higher throughput
Better scalability
```

---

# But Now A New Problem Appears

How does the system know:

```
Which Redis node contains seat A10?
```

We need a **routing strategy**.

Two common solutions exist.

---

# Solution 1 — Range Based Sharding

Seats divided by range.

Example:

```
Seat numbers 1–75   → Redis-1
Seat numbers 76–150 → Redis-2
Seat numbers 151–225 → Redis-3
Seat numbers 226–300 → Redis-4
```

Seat lookup:

```
Seat A50 → Redis-1
Seat A120 → Redis-2
```

Problem:

```
Uneven traffic distribution
```

Example:

Front seats are more popular.

---

# Solution 2 — Hash Based Sharding

Instead of ranges we compute:

```
hash(show_id + seat_id)
```

Then:

```
hash % number_of_nodes
```

Example:

```
hash(show101:A10) % 4 = Redis-3
```

Advantages:

```
Uniform distribution
Avoids hotspots
```

This is the **most common approach**.

---

# But Another Problem Appears Again

Suppose we add a new Redis node.

Before:

```
4 Redis nodes
```

After scaling:

```
5 Redis nodes
```

Now hash changes:

```
hash % 4 ≠ hash % 5
```

This means **all seat mappings change**.

This causes:

```
Massive cache reshuffling
```

Very bad.

---

# Solution — Consistent Hashing

Consistent hashing minimizes data movement when nodes change.

Idea:

Imagine a **hash ring**.

```
0 -------------------- 360 degrees
```

Redis nodes placed on ring:

```
Redis1
Redis2
Redis3
Redis4
```

Seat keys also mapped on ring.

Example:

```
show101:A10 → hash position
```

Seat assigned to **next node clockwise**.

---

# Visual Example

```
         Redis1
           |
           |
Seat A10 --- ring --- Redis2
           |
           |
         Redis3
```

If Redis2 fails:

```
A10 automatically goes to Redis3
```

Minimal data movement.

---

# Benefits Of Consistent Hashing

```
Horizontal scaling
Minimal reshuffling
High availability
```

Used by systems like:

```
Cassandra
DynamoDB
Kafka
Redis Cluster
```

---

# Next Scaling Layer — Sharding Database

Until now we scaled Redis.

But bookings table will also grow huge.

Example after few months:

```
Bookings
--------
500 million rows
```

Single DB becomes bottleneck.

So we shard database.

---

# Database Sharding Strategy

We partition bookings by:

```
show_id
or
theatre_id
or
city
```

Example:

```
Shard1 → Hyderabad shows
Shard2 → Bangalore shows
Shard3 → Mumbai shows
```

Now writes distribute across shards.

---

# Updated Large Scale Architecture

At this stage system becomes:

```
Users
 ↓
CDN
 ↓
Load Balancer
 ↓
API Gateway
 ↓
Booking Services (many instances)
 ↓
Redis Cluster (seat locks)
 ↓
Database Shards
 ↓
Read Replicas
```

Now system can handle:

```
millions of users
thousands of bookings/sec
```

---

# Another Real Problem

When a blockbuster releases, users repeatedly refresh seat maps.

Example:

```
User opens seat page
Refresh
Refresh
Refresh
```

Millions of requests hitting server.

This introduces **another optimization**.

Next concept:

# Virtual Waiting Queue

Used by:

```
BookMyShow
Ticketmaster
IRCTC
```

Instead of letting millions hit backend simultaneously.

System creates **queue of users**.

---

Example:

```
You are in queue
Position: 12,345
Estimated wait: 2 minutes
```

This smooths traffic spikes.

---

# Next Step

In the next section we will design the **final production architecture** including:

• virtual waiting rooms
• queue systems (Kafka / SQS)
• event-driven booking
• failure recovery
• monitoring

This will complete the **full interview-level design**.

---

If you're ready, reply **"continue"**.

The next part will show **how large ticket systems survive massive launches like Avengers / IPL / concerts**.

---

Great — now we reach the **final stage of the architecture**.
This is where systems like **large ticketing platforms** survive **extreme traffic spikes** during big launches (concerts, IPL finals, blockbuster releases).

So far we solved:

✅ Seat locking
✅ Payment workflow
✅ Caching & read scaling
✅ Redis sharding
✅ Database sharding

But **one huge problem remains**.

---

# Step 6 — The Traffic Tsunami Problem

Imagine a big event release.

Example:

```text
IPL Final Tickets
Release Time: 10:00 AM
```

Traffic pattern:

```text
9:59:50 → 200K users waiting
10:00:00 → 3 million users refresh
10:00:02 → 8 million API requests
```

If all requests reach backend:

```text
Load balancer overloaded
API servers overloaded
Redis overloaded
Database overloaded
```

The entire system crashes.

This actually happened multiple times with **large ticket platforms**.

---

# The Key Insight

We **must not allow all users to access the booking system simultaneously**.

Instead we **control the flow of users**.

Solution:

# Virtual Waiting Queue

When traffic spike happens, users first enter a **waiting room**.

Flow becomes:

```text
Users
 ↓
Waiting Room Service
 ↓
Queue
 ↓
Booking System
```

---

# What Users See

Instead of seat map:

```text
You are in queue
Position: 15432
Estimated wait: 3 minutes
```

Users enter the system **in batches**.

Example:

```text
Every second allow 500 users
```

Now backend receives **controlled traffic**.

---

# How Queue System Works

When ticket sale begins:

Users hit:

```text
GET /enter-sale
```

Waiting service generates:

```text
queue_token
```

Example token:

```text
token_928374982
position = 15234
```

User now polls:

```text
GET /queue-status?token=928374982
```

Once user reaches front:

```text
Access granted
```

Then redirected to **booking system**.

---

# Queue Implementation

Queue can be built using:

```text
Redis sorted sets
Kafka
SQS
RabbitMQ
```

Example Redis structure:

```text
ZADD queue timestamp user_id
```

Users ordered by **arrival time**.

---

# Releasing Users Gradually

Booking system capacity example:

```text
500 bookings/sec
```

So waiting service releases:

```text
500 users/sec
```

This keeps system stable.

---

# Architecture With Waiting Queue

System now becomes:

```text
Users
 ↓
CDN
 ↓
Load Balancer
 ↓
Waiting Room Service
 ↓
Queue System
 ↓
API Gateway
 ↓
Booking Services
 ↓
Redis Cluster (seat locks)
 ↓
Database Shards
```

This prevents **traffic spikes from crashing system**.

---

# Next Optimization — Event Driven Architecture

Currently booking flow is synchronous.

Example:

```text
Payment success
↓
Booking confirmation
↓
Ticket generation
↓
Email notification
```

This increases latency.

Instead we move to **event-driven architecture**.

---

# Event Driven Booking Flow

When booking confirmed:

System publishes event:

```text
BookingConfirmed
```

Event stream example:

```text
Kafka Topic: booking-events
```

Consumers listen.

---

# Example Consumers

### Ticket Service

Generates ticket.

```text
BookingConfirmed → generate QR code ticket
```

---

### Notification Service

Sends email / SMS.

```text
BookingConfirmed → send confirmation message
```

---

### Analytics Service

Tracks sales.

```text
BookingConfirmed → update metrics
```

Benefits:

```text
Loose coupling
Better scalability
Independent services
```

---

# Example Event Flow

```text
Booking Service
   ↓
Kafka Topic
   ↓
 ┌───────────────┐
 │ Ticket Service │
 │ Notification   │
 │ Analytics      │
 └───────────────┘
```

This pattern is **very common in large systems**.

---

# Handling System Failures

Production systems must handle failures.

Example failures:

```text
Redis crash
DB shard down
Payment service timeout
```

Solutions:

---

### Redis Replication

Redis cluster:

```text
Primary
Replica
Replica
```

Failover happens automatically.

---

### Database Replication

Each shard has:

```text
Primary
Read replicas
```

If primary fails → promote replica.

---

### Retry Mechanisms

Example payment callback retry.

```text
retry 3 times
exponential backoff
```

---

### Circuit Breakers

If payment gateway fails:

```text
Stop sending requests temporarily
```

Prevents cascading failures.

---

# Monitoring & Observability

Large systems must monitor:

```text
booking success rate
payment failures
seat lock conflicts
queue wait time
```

Tools used:

```text
Prometheus
Grafana
ELK stack
Datadog
```

Alerts trigger when:

```text
error rate > threshold
latency spikes
```

---

# Final Production Architecture

Now the system looks like this:

```text
Users
 ↓
CDN
 ↓
Load Balancer
 ↓
Waiting Room Service
 ↓
Queue System
 ↓
API Gateway
 ↓
Booking Service Cluster
 ↓
Redis Cluster (seat locks + caching)
 ↓
Database Shards (primary + replicas)
 ↓
Event Streaming (Kafka)
 ↓
Consumers
   ├ Ticket Service
   ├ Notification Service
   └ Analytics Service
```

This architecture can handle:

```text
Millions of users
Thousands of bookings per second
Massive launch traffic
```

---

# What Interviewers Expect

If you explain:

1️⃣ Seat locking with Redis
2️⃣ Booking workflow with states
3️⃣ Payment idempotency
4️⃣ Caching + read replicas
5️⃣ Sharding
6️⃣ Waiting room for spikes
7️⃣ Event-driven architecture

You demonstrate **strong system design ability**.

---

✅ This completes the **core BookMyShow HLD**.

But there are still **very interesting deep interview topics** we can explore if you want.

For example:

• **How seat selection UI updates in real time**
• **How to prevent seat hoarding by bots**
• **How to handle celebrity concert demand (Taylor Swift level)**
• **Geo-distributed ticket systems**
• **Exactly-once booking guarantees**

These are **advanced interview differentiators**.

If you'd like, I can also show you:

**The “perfect whiteboard answer” structure for this question** so you can explain it **clearly in 20 minutes in interviews**.

---
