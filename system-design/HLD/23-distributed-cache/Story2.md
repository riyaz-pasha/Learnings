Perfect 👍 This **story-based approach** is exactly how system design interviews expect you to think. Since you're preparing for **Google L5 HLD**, we’ll go **deep but structured** so every concept becomes interview-ready.

We will design **Distributed Cache (like Redis / Memcached)** step-by-step as an **evolution story**.

Rules we'll follow:

* Each step solves a **real problem**
* Then we discover **new issues**
* Then we **improve the architecture**
* We will cover **all HLD aspects**: scaling, sharding, replication, consistency, failure handling, eviction, etc.

We will **not jump ahead**.
You will **think like the engineer who built the system**.

---

# Step 0 — The Problem (Why Distributed Cache Exists)

Imagine we are building **a large product like Amazon / Instagram / Google Docs**.

Example request:

```
User opens homepage
→ backend needs user profile
→ backend queries database
```

DB query:

```
SELECT * FROM users WHERE id = 123
```

Now imagine:

```
10M users
100K requests/sec
```

Every request hitting DB.

### What happens?

Database becomes the **bottleneck**.

Example DB performance:

```
DB can handle: 10K queries/sec
Traffic:       100K queries/sec
```

DB will crash.

---

# Step 1 — First Idea: Cache in Application Memory

Engineers think:

> Why query DB every time if data rarely changes?

Example:

User profile rarely changes.

So we cache it.

```
Application Server

if(userId in memoryCache)
    return memoryCache[userId]
else
    data = DB.get(userId)
    memoryCache[userId] = data
    return data
```

Architecture:

```
Client
   ↓
App Server
   ↓
Memory Cache (inside app)
   ↓
Database
```

### Example

Request 1:

```
GET /user/123
```

Flow:

```
App → cache miss
App → DB
App → store in memory
```

Next request:

```
GET /user/123
```

Flow:

```
App → cache hit
```

Latency improvement:

```
DB query = 10ms
Memory = 0.1ms
```

Huge win.

---

# Problem Appears

Now our system grows.

Architecture:

```
        ┌──────────┐
        │ App 1    │
        │ Cache A  │
        └──────────┘
             │
        ┌──────────┐
        │ App 2    │
        │ Cache B  │
        └──────────┘
             │
        ┌──────────┐
        │ App 3    │
        │ Cache C  │
        └──────────┘
             │
           Database
```

Each server has **its own cache**.

Example traffic:

```
User 123 → App 1
User 123 → App 2
User 123 → App 3
```

Each server loads from DB.

```
DB hit ×3
```

Cache is **not shared**.

---

# Bigger Problem — Cache Inconsistency

Suppose user updates profile.

```
UPDATE user name
```

Now:

```
App1 cache = old data
App2 cache = old data
App3 cache = new data
```

Different users see different results.

Very bad.

---

# Scaling Problem

As we scale:

```
1000 app servers
1000 caches
```

Memory waste.

Also:

```
Cache hit ratio becomes poor
```

---

# Engineers Realize

Caching should be:

```
Centralized
Shared
Fast
Scalable
```

So they create:

# Step 2 — Centralized Cache Server

Architecture becomes:

```
            ┌──────────┐
            │  App 1   │
            └────┬─────┘
                 │
            ┌────▼─────┐
            │  Cache   │
            │ Server   │
            └────┬─────┘
                 │
             Database
```

Now:

```
App1
App2
App3
```

All share same cache.

Flow:

```
App → Cache
       ↓
    (miss)
       ↓
       DB
```

Example:

```
GET user:123
```

First request:

```
cache miss
DB hit
store in cache
```

Next request:

```
cache hit
```

DB load reduces massively.

---

# Why Dedicated Cache Server?

Because we need:

```
very fast memory
optimized key lookup
TTL eviction
LRU eviction
network access
```

Examples:

* **Redis**
* **Memcached**

---

# Major Win

Database load reduces:

```
100K req/sec → 5K req/sec
```

---

# But a New Problem Appears

Our architecture now:

```
          ┌────────────┐
          │ Cache Node │
          └─────┬──────┘
                │
              Database
```

Single cache server.

Now traffic increases.

```
1M requests/sec
```

Problems:

### 1) Cache becomes bottleneck

CPU overloaded.

### 2) Memory limit

Example:

```
Server memory = 64GB
Cache needed = 2TB
```

Impossible.

### 3) Single point of failure

If cache dies:

```
All requests hit DB
DB crashes
Entire system down
```

---

# Engineers Realize

We need:

```
Multiple cache servers
```

This leads to the core idea of:

# **Distributed Cache**

But the next big question becomes:

> If we have 100 cache servers
> **Which server should store which key?**

This introduces the most important concept:

# **Sharding**

---

Before we move forward, let me ask you the **interview thinking question**.

### Suppose we have 4 cache servers

```
Cache1
Cache2
Cache3
Cache4
```

And keys:

```
user:1
user:2
user:3
user:4
```

How should we decide:

```
which key → which cache server
```

Common answers engineers first try:

A)

```
random server
```

B)

```
hash(key) % number_of_servers
```

C)

```
range based sharding
```

Which approach would you choose first and why?

---

Good choice 👍
This is exactly the **first approach most engineers try**, and it actually existed in early distributed cache systems.

Let’s continue the story.

---

# Step 3 — Sharding Using `hash(key) % N`

We now have multiple cache servers.

```
Cache1
Cache2
Cache3
Cache4
```

We distribute keys using:

```
server = hash(key) % number_of_servers
```

Example:

```
number_of_servers = 4
```

Keys:

```
user:1
user:2
user:3
user:4
user:5
```

Compute hash:

```
hash("user:1") % 4 = 1 → Cache1
hash("user:2") % 4 = 3 → Cache3
hash("user:3") % 4 = 0 → Cache0
hash("user:4") % 4 = 2 → Cache2
```

Architecture now:

```
              ┌───────────┐
Client ─────► │ App Server│
              └─────┬─────┘
                    │
      hash(key)%4   │
        routing     ▼
      ┌──────┬──────┬──────┬──────┐
      │Cache1│Cache2│Cache3│Cache4│
      └──────┴──────┴──────┴──────┘
                    │
                 Database
```

App server decides **which cache node to talk to**.

---

# Example Request Flow

Request:

```
GET user:42
```

Step 1 — App calculates:

```
hash("user:42") % 4 = 2
```

Step 2

```
App → Cache2
```

If hit:

```
return value
```

If miss:

```
Cache2 → DB
Cache2 stores result
```

---

# Why This Works Well

### Even Distribution

Hash functions distribute keys evenly.

Example:

```
10M keys
4 servers
```

Each server roughly gets:

```
2.5M keys
```

Balanced load.

---

### Horizontal Scaling

If traffic grows:

```
Add more cache servers
```

Example:

```
Cache1
Cache2
Cache3
Cache4
Cache5
Cache6
```

Now:

```
hash(key) % 6
```

Simple.

---

# But a Huge Problem Appears 🚨

Let’s say we initially had:

```
4 servers
```

Distribution:

```
hash(key) % 4
```

Now we add **one more cache server**.

```
5 servers
```

Now the formula becomes:

```
hash(key) % 5
```

---

## Look What Happens

Key:

```
user:42
```

Before:

```
hash("user:42") % 4 = 2 → Cache2
```

After scaling:

```
hash("user:42") % 5 = 4 → Cache4
```

Key moved.

---

Now imagine:

```
100 million cache keys
```

After adding **one server**:

```
Almost ALL keys move to different servers
```

Which means:

```
100M cache misses
```

Suddenly:

```
Cache useless
```

All requests go to DB.

```
DB overload
System meltdown
```

---

# This is Called the **Rehashing Problem**

Small cluster change → massive key movement.

```
4 servers → 5 servers
```

Almost entire cache invalidated.

---

# Real Production Impact

Let’s say:

```
Netflix
Twitter
Amazon
```

Have:

```
500GB cache
```

Adding one node causes:

```
500GB cache miss
```

This creates **traffic spike to database**.

Production outage.

---

# Engineers Needed a Better Solution

Requirements:

When adding or removing cache nodes:

```
Minimal key movement
```

Goal:

```
Adding 1 node should only move ~1/N keys
```

Instead of:

```
Moving almost everything
```

---

This led to one of the **most important algorithms in distributed systems**:

# **Consistent Hashing**

Used by:

* Redis Cluster
* DynamoDB
* Cassandra
* Akamai CDN
* Load balancers
* Distributed caches everywhere

---

Before I explain it, interviewers usually ask this thinking question:

Imagine keys placed on a **circular ring** like a clock.

```
0 → 1 → 2 → 3 → ... → 360
```

Both:

```
servers
keys
```

are placed on this ring using hash.

Then a key is assigned to the **next server clockwise**.

Question:

Why does this reduce the **rehashing problem** when adding a new server?

Think about it and tell me your intuition.
Even a rough guess is fine.


---
Great — let's continue the **engineering story**.

You suggested earlier:

```
hash(key) % N
```

But we saw the **rehashing disaster** when adding servers.

So engineers invented a smarter idea.

---

# Step 4 — Consistent Hashing

Engineers asked:

> What if adding a server **does NOT change the mapping of most keys**?

They came up with **consistent hashing**.

The idea:

Instead of mapping keys **directly to servers**, we map both **keys and servers onto a hash ring**.

---

# The Hash Ring Concept

Imagine the hash space as a **circle**.

```
0 → 2^32 - 1
```

Visually:

```
        (0)
         |
         |
         |
 270 ----+---- 90
         |
         |
         |
        180
```

Now we hash **servers**.

Example:

```
hash(Cache1) = 50
hash(Cache2) = 150
hash(Cache3) = 250
```

Placed on the ring:

```
           Cache3(250)
               *
              /
             /
            /
     *-----(0)------*
Cache1(50)       Cache2(150)
```

---

# Now We Hash Keys

Example keys:

```
user:1
user:2
user:3
```

Example hashes:

```
user:1 → 60
user:2 → 120
user:3 → 260
```

Rule:

> A key is stored in the **first server clockwise**.

---

### Example

Key:

```
user:1 → 60
```

Clockwise server:

```
Cache2 (150)
```

So:

```
user:1 → Cache2
```

---

Key:

```
user:2 → 120
```

Clockwise:

```
Cache2
```

---

Key:

```
user:3 → 260
```

Clockwise:

```
Cache1 (wrap around)
```

---

# Why This Solves Rehashing

Now imagine we add a new server.

```
Cache4 → hash = 100
```

Ring becomes:

```
           Cache3(250)
               *
              /
             /
             *
        Cache4(100)
           /
     *-----(0)------*
Cache1(50)       Cache2(150)
```

Now what changes?

Only the keys between:

```
50 → 100
```

move to the new server.

Everything else stays same.

---

# Compare with Previous Approach

### Modulo Hashing

Adding server:

```
ALL keys move
```

---

### Consistent Hashing

Adding server:

```
Only ~1/N keys move
```

Example:

```
10 cache servers
Add 1 server
```

Only:

```
~10% keys move
```

Huge improvement.

---

# But Engineers Found Another Problem

Look at this ring:

```
Cache1 → 10
Cache2 → 20
Cache3 → 300
```

Distribution becomes uneven.

```
Cache3 holds huge range
```

Example:

```
20 → 300
```

That server stores **most keys**.

This creates:

```
Hotspot server
Memory imbalance
CPU overload
```

---

# Engineers Solve This Using **Virtual Nodes**

Instead of placing **one position per server**, we place **many positions**.

Example:

Instead of:

```
Cache1 → 1 node
```

We create:

```
Cache1-1
Cache1-2
Cache1-3
Cache1-4
```

Each hashed separately.

Example:

```
Cache1-1 → 30
Cache1-2 → 120
Cache1-3 → 200
Cache1-4 → 330
```

Now distribution becomes balanced.

Example ring:

```
Cache1-1
Cache2-1
Cache3-1
Cache1-2
Cache2-2
Cache3-2
...
```

Now each physical server owns **many small ranges** instead of one large range.

This gives:

```
Better load balancing
Better fault tolerance
```

---

# Why Virtual Nodes Also Help During Failure

Suppose:

```
Cache2 fails
```

With normal consistent hashing:

```
Huge range shifts to one server
```

With virtual nodes:

```
Load spreads across many servers
```

Much more stable.

---

# So Our Distributed Cache Now Looks Like This

```
              App Servers
                  │
                  │
          Consistent Hashing
                  │
     ┌───────────┬───────────┬───────────┐
     │           │           │           │
   Cache1      Cache2      Cache3      Cache4
 (many vnodes)(many vnodes)(many vnodes)(many vnodes)
     │           │           │           │
                   DB
```

---

# But a Big Reliability Problem Still Exists

Currently each key lives in **only one cache node**.

Example:

```
user:123 → Cache3
```

If:

```
Cache3 crashes
```

Then:

```
All those keys lost
```

Now:

```
Cache miss storm
DB overload
```

---

Real production caches **never rely on a single copy**.

So engineers introduce the next concept:

# **Replication**

Before I explain the industry solution, let me ask you the next interview thinking question.

Suppose we replicate data.

For each key we store:

```
Primary node
Replica node
```

Question:

When a client reads a key, should it read from:

A)

```
Primary only
```

B)

```
Replica only
```

C)

```
Either primary or replica
```

Which would you choose and why?

---
Good answer 👍
Most distributed caches **do allow reads from either primary or replica**, but the reasoning and trade-offs are important in interviews. Let's walk through it like engineers discovering the solution.

---

# Step 5 — Replication (Making Cache Fault Tolerant)

Until now we had:

```
Key → Single cache node
```

Example:

```
user:123 → Cache3
```

Problem:

```
Cache3 crashes
```

Result:

```
user:123 data lost
```

Then every request becomes:

```
Cache miss → DB
```

If many keys were on Cache3:

```
Millions of DB queries suddenly
```

This is called a **cache miss storm**.

Real systems must avoid this.

---

# Solution — Replicate Data

We store **multiple copies** of each key.

Example:

```
user:123
```

Stored on:

```
Primary  → Cache3
Replica  → Cache7
```

Architecture:

```
              App Servers
                   │
                   │
            Consistent Hashing
                   │
      ┌────────────┬────────────┬────────────┐
      │            │            │            │
   Cache1       Cache2       Cache3       Cache4
                                │
                                │ replication
                                ▼
                             Cache7
```

---

# Write Flow

When storing data:

```
SET user:123 = profile_data
```

Flow:

```
App → Primary (Cache3)
Primary → Replica (Cache7)
```

Now we have **two copies**.

---

# What Happens if Primary Fails?

Example:

```
Cache3 crashes
```

Replica takes over.

```
Cache7 becomes primary
```

Requests continue normally.

DB protected.

---

# Read Strategy Options

Now we reach the question I asked you.

When reading:

```
GET user:123
```

We have options.

---

# Option A — Read From Primary Only

```
Client → Primary
```

Advantages:

```
Strong consistency
```

You always read latest value.

Problem:

```
Primary overloaded
Replica unused
```

Waste of resources.

---

# Option B — Read From Replica Only

```
Client → Replica
```

Advantages:

```
Load on primary reduced
```

But problem:

Replication is **asynchronous**.

Example timeline:

```
t1: SET user:123 = A
t2: stored in primary
t3: replication in progress
t4: client reads from replica
```

Replica might still have **old value**.

This causes:

```
Stale reads
```

---

# Option C — Read From Either Primary or Replica (Most Common)

Reads distributed across both.

Example:

```
Client → Load balancer
           ↓
     Primary or Replica
```

Benefits:

```
Higher read throughput
Better CPU usage
Lower latency
```

Tradeoff:

```
Possible stale reads
```

But for cache systems this is usually acceptable.

Because cache data is **already temporary**.

---

# Real Systems Do This

Examples:

* **Redis Cluster**
* **Memcached with replicas**
* **Amazon DynamoDB DAX**
* **Facebook TAO cache**

They often allow **replica reads**.

---

# But Replication Introduces a New Problem

Imagine we replicate to **3 nodes**.

```
Primary → Replica1 → Replica2
```

Now when writing:

```
SET user:123
```

Question:

Should we consider the write successful when:

A)

```
Primary updated
```

B)

```
Majority replicas updated
```

C)

```
All replicas updated
```

This introduces the important concept of:

```
Consistency vs Availability
```

(Which leads to the **CAP tradeoff**).

---

Before we go deeper, let me ask you the **interview reasoning question**.

For a **distributed cache system**, which write strategy would you choose?

A)

```
Primary only
```

B)

```
Majority replicas
```

C)

```
All replicas
```

Think like a system designer protecting the **database from overload**.

---
Good thinking 👍 — **majority replicas** is a very reasonable answer in many distributed systems (like quorum-based systems such as Cassandra). But in **distributed cache systems**, engineers usually make a **different choice**, and understanding *why* is very important for HLD interviews.

Let’s continue the **engineering story**.

---

# Step 6 — Write Consistency in Distributed Cache

We now have replication:

```
Primary → Replica1 → Replica2
```

For each key:

```
user:123
Primary: Cache3
Replica1: Cache7
Replica2: Cache9
```

When we write:

```
SET user:123 = profile_data
```

We must decide **when the write is considered successful**.

Possible strategies:

```
A) Primary updated
B) Majority replicas updated
C) All replicas updated
```

---

# Option C — Wait For All Replicas

```
Primary → Replica1 → Replica2
```

Write success only after:

```
Primary + Replica1 + Replica2 updated
```

### Problem

Slow.

Example latency:

```
Primary write      = 1 ms
Replica1 write     = 2 ms
Replica2 write     = 4 ms
```

Total latency:

```
4 ms
```

For a **cache system**, this is bad.

Remember:

```
Cache exists to be extremely fast
```

Also:

If one replica is down:

```
Write fails
```

System becomes unavailable.

This violates **high availability**.

---

# Option B — Majority Replicas (Quorum)

Write success after:

```
2 out of 3 nodes updated
```

This gives:

```
Better consistency
```

But still introduces:

```
extra network calls
extra latency
```

Also:

Cache systems usually don't need **strong consistency guarantees**.

Why?

Because:

```
Cache = optimization layer
```

The **source of truth is the database**.

---

# What Real Systems Usually Do

Most distributed caches choose:

# Option A — Write To Primary Only

```
Client → Primary
Primary → async replicate to replicas
```

Write completes immediately after primary update.

Replication happens **asynchronously**.

---

# Why This Is Acceptable For Cache

Because cache data is:

```
Temporary
Rebuildable
Non-authoritative
```

If replicas lag:

Worst case:

```
stale data for few milliseconds
```

Not a big problem.

But latency improvement is huge.

Example:

```
Primary-only write = 1ms
Majority write     = 3–5ms
```

At scale (millions of requests):

This difference matters.

---

# Real Industry Example

**Redis replication model**

```
Client → Primary
Primary → async replication
```

Reads can happen from replicas.

This is called:

```
Eventual Consistency
```

---

# Our Distributed Cache Now

We have built:

```
Consistent hashing
Virtual nodes
Replication
Async writes
Replica reads
```

Architecture now:

```
                  App Servers
                       │
                 Consistent Hash
                       │
       ┌───────────────┬───────────────┬───────────────┐
       │               │               │               │
     Cache1          Cache2          Cache3          Cache4
       │               │               │               │
       ▼               ▼               ▼               ▼
    Replica          Replica         Replica         Replica
```

System is now:

```
Scalable
Fault tolerant
Low latency
```

---

# But Another Problem Appears 🚨

Imagine this scenario.

Cache entry:

```
user:123 → name="Riyaz"
```

Stored in cache.

Now user updates name:

```
name="Riyaz Pasha"
```

Database updates successfully.

But cache still has:

```
name="Riyaz"
```

Now system returns **wrong data**.

This is called:

# Cache Invalidation Problem

One of the hardest problems in distributed systems.

Engineers joke:

```
There are only two hard things in CS:
1) Cache invalidation
2) Naming things
```

---

# Why This Happens

Example flow:

```
Client → Update Profile
       → DB updated
```

But cache still holds old value.

Future reads:

```
App → Cache → stale data
```

Wrong result.

---

# Engineers Invented Multiple Strategies

Three famous patterns:

### 1️⃣ Cache Aside (Lazy Loading)

### 2️⃣ Write Through

### 3️⃣ Write Behind (Write Back)

These are **very important in system design interviews**.

---

Before I explain them, let me ask you:

When updating data, which approach sounds better to you?

A)

```
Update DB
Delete cache
```

B)

```
Update cache
Update DB
```

C)

```
Update cache only
Later update DB
```

Which one would you choose first and why?

---
Good answer 👍 — **update cache → update DB** is actually one of the classic strategies called **Write-Through Cache**. But engineers discovered some interesting trade-offs with it. Let’s continue the story carefully.

---

# Step 7 — The Cache Invalidation Problem

Our system now:

* Consistent hashing
* Virtual nodes
* Replication
* Async replication
* Distributed cache cluster

Architecture:

```
Client
   ↓
App Servers
   ↓
Distributed Cache
   ↓
Database (source of truth)
```

Now imagine user updates profile.

```
UPDATE name = "Riyaz Pasha"
```

But cache still has:

```
user:123 → "Riyaz"
```

This is **stale data**.

We must ensure **cache and DB remain reasonably consistent**.

Engineers tried several strategies.

---

# Strategy 1 — Cache Aside (Lazy Loading)  ⭐ Most Common

This is used by many companies because it is **simple and reliable**.

## Read Flow

```
1) App → Cache
2) If miss → DB
3) Store result in cache
```

Example:

```
GET user:123
```

Flow:

```
Cache miss
↓
DB query
↓
Store in cache
↓
Return result
```

---

## Write Flow

When updating data:

```
1) Update DB
2) Delete cache entry
```

Example:

```
UPDATE user:123
```

Flow:

```
App → DB update
App → DELETE cache[user:123]
```

Next read:

```
Cache miss
↓
DB read
↓
Cache updated
```

---

## Why Engineers Like Cache Aside

Benefits:

```
✔ Cache only stores frequently accessed data
✔ Simpler logic
✔ DB remains source of truth
✔ Easy to scale
```

This is used in:

* Facebook
* Instagram
* Twitter
* Many microservices

---

## Problem: Race Condition

Example timeline:

```
T1: Client1 reads
T2: Cache miss
T3: Client2 updates DB
T4: Cache still empty
T5: Client1 stores OLD value
```

Cache now has stale data again.

This is called a **race condition**.

But in practice:

```
Short TTLs reduce this risk
```

---

# Strategy 2 — Write Through Cache (Your Answer)

Here:

```
Client writes to cache
Cache writes to DB
```

Architecture:

```
Client
   ↓
Cache
   ↓
Database
```

Flow:

```
SET user:123 = "Riyaz Pasha"
```

Steps:

```
1) Cache updated
2) Cache synchronously updates DB
```

---

## Advantages

Consistency improves.

Because:

```
Cache always updated first
```

No stale reads.

---

## Problems

### Higher Write Latency

Every write must go through cache + DB.

Example:

```
Cache write = 1 ms
DB write = 8 ms
```

Total latency:

```
9 ms
```

For systems with many writes this hurts.

---

### Cache Stores Too Much Data

Even rarely accessed data ends up cached.

Example:

```
User updated profile once
but never read again
```

Cache memory wasted.

---

# Strategy 3 — Write Behind (Write Back)

Here:

```
Client → Cache
Cache → DB later (async)
```

Flow:

```
SET user:123
```

Steps:

```
1) Update cache
2) Put write into queue
3) Background worker writes to DB
```

Architecture:

```
Client
   ↓
Cache
   ↓
Write Queue
   ↓
Database
```

---

## Advantages

Very fast writes.

Example:

```
Cache write = 1 ms
```

No DB wait.

---

## Problems

If cache crashes before DB write:

```
Data loss
```

This is risky unless system guarantees durability.

---

# What Most Distributed Caches Use

Most large systems use:

```
Cache Aside Pattern
```

Because:

```
Simple
Safe
Flexible
DB remains source of truth
```

Example systems:

* Redis caching
* Memcached caching
* CDN caches

---

# Our Distributed Cache So Far

We have covered many core HLD topics:

```
✔ Why cache exists
✔ Centralized cache
✔ Distributed cache
✔ Sharding
✔ Consistent hashing
✔ Virtual nodes
✔ Replication
✔ Read strategies
✔ Write consistency
✔ Cache invalidation
```

But an interview-ready distributed cache still needs to cover several **critical topics**.

Important ones left:

1️⃣ **Cache Eviction Policies**
2️⃣ **TTL (Expiration)**
3️⃣ **Hot Key Problem**
4️⃣ **Cache Stampede**
5️⃣ **Failure Handling**
6️⃣ **Cluster Rebalancing**
7️⃣ **Monitoring & Metrics**

These are often **where interviewers dig deeper**.

---

Before we move forward, let me ask the next **system design interview question**.

Cache memory is limited.

Example:

```
Cache memory = 64GB
Incoming data = 200GB
```

We cannot store everything.

So we must decide:

```
Which keys should be evicted?
```

Which eviction policy do you think works best for a distributed cache?

A)

```
FIFO
```

B)

```
LRU (Least Recently Used)
```

C)

```
LFU (Least Frequently Used)
```

D)

```
TTL based eviction
```

What would you choose first and why?

---
Good choice 👍
**LRU (Least Recently Used)** is indeed one of the **most widely used eviction policies** in distributed caches (e.g., Redis supports it). But like every engineering decision, it exists because engineers **tried simpler things first and hit problems**.

Let’s continue the **story evolution**.

---

# Step 8 — Cache Memory Is Limited

Our distributed cache now stores millions of keys.

Example:

```
Cache cluster memory = 64 GB
Incoming working data = 200 GB
```

We cannot store everything.

So when cache becomes full, we must decide:

```
Which key should be removed?
```

This is called **Cache Eviction Policy**.

---

# First Attempt Engineers Tried — FIFO

**FIFO = First In First Out**

Idea:

```
Remove the oldest key inserted into cache
```

Example:

```
Cache order:

A → B → C → D
```

New key arrives:

```
E
```

Eviction:

```
Remove A
```

Cache becomes:

```
B → C → D → E
```

---

## Why FIFO Fails

Imagine this situation:

```
A = homepage config (used constantly)
B = random user profile
C = random product
```

FIFO may remove **A**, even though it is heavily used.

Result:

```
Frequently accessed data gets evicted
```

Cache efficiency drops.

Engineers realized:

```
Access patterns matter
```

---

# Next Attempt — LRU (Your Answer)

LRU removes the key that **has not been used for the longest time**.

Idea:

```
Recent data is likely to be accessed again.
```

This concept comes from **temporal locality**.

---

## Example

Cache:

```
[A, B, C, D]
```

Requests happen:

```
Access A
Access B
Access A
Access C
```

Recent order becomes:

```
D (oldest)
B
C
A (newest)
```

If new key **E** arrives:

```
Evict D
```

Because it was least recently used.

---

# Why LRU Works Well

In most real systems:

```
Recently accessed data is likely to be accessed again.
```

Examples:

```
Trending posts
Popular product pages
User sessions
Hot API responses
```

So LRU keeps **hot data** in memory.

This dramatically improves:

```
Cache hit ratio
```

---

# Implementation Challenge

In interviews they often ask:

> How do we implement LRU efficiently?

Requirements:

```
GET → O(1)
PUT → O(1)
```

Classic solution:

```
HashMap + Doubly Linked List
```

HashMap:

```
key → node
```

Doubly linked list:

```
maintains access order
```

Head:

```
Most recently used
```

Tail:

```
Least recently used
```

Eviction removes **tail**.

---

# But Engineers Discovered Another Problem

LRU works well **most of the time**, but sometimes it behaves badly.

Example scenario:

```
Analytics job scanning millions of records
```

Requests:

```
A
B
C
D
E
F
G
H
```

Each accessed once.

LRU thinks:

```
These are recent → keep them
```

But they are actually **one-time accesses**.

Result:

```
Real hot data gets evicted
```

This is called **cache pollution**.

---

# Engineers Tried Another Policy — LFU

LFU = **Least Frequently Used**

Idea:

```
Evict keys with lowest access count
```

Example:

```
Key A accessed 1000 times
Key B accessed 3 times
Key C accessed 1 time
```

Evict:

```
C
```

Better for:

```
Long-term popular data
```

---

# Why LFU Also Has Problems

Example:

```
Old hot key accessed 1000 times last week
But no one uses it today
```

LFU still keeps it.

Because:

```
Frequency counter is high
```

So stale items remain forever.

---

# What Real Systems Do

Modern caches often use **hybrid policies**.

Examples:

Redis supports:

```
LRU approximation
LFU approximation
TTL eviction
```

Many large-scale caches use **LRU + TTL together**.

---

# TTL (Time To Live)

Instead of waiting for eviction, we attach **expiration time**.

Example:

```
SET user:123 TTL=10 minutes
```

After 10 minutes:

```
Key automatically removed
```

Benefits:

```
Prevents stale data
Controls memory usage
```

Example use cases:

```
Session cache → TTL 30 min
Feature flags → TTL 1 min
Product catalog → TTL 5 min
```

---

# How Redis Handles Expiration

Two mechanisms:

### Passive expiration

```
When key accessed → check TTL
```

If expired:

```
delete
```

---

### Active expiration

Background job scans keys periodically and removes expired ones.

This prevents:

```
Expired keys occupying memory
```

---

# Our Distributed Cache Now Supports

```
✔ Consistent hashing
✔ Virtual nodes
✔ Replication
✔ Async replication
✔ Cache invalidation
✔ Eviction policies
✔ TTL expiration
```

This already covers **70% of distributed cache HLD**.

But two **very important production problems** remain.

These are asked frequently in **FAANG system design interviews**.

---

# Problem 1 — Cache Stampede

Imagine this key:

```
homepage_feed
```

Millions of users request it.

Now TTL expires.

Suddenly:

```
Cache miss
```

Millions of requests go to DB.

```
DB crashes
```

This is called **cache stampede**.

---

# Problem 2 — Hot Key Problem

Suppose this key:

```
celebrity_post
```

Is accessed:

```
1M times/sec
```

But it exists on **one cache shard**.

That shard becomes overloaded.

---

These problems require **advanced distributed cache techniques**.

Before moving forward, I want to ask you the next **interview reasoning question**.

To prevent **cache stampede**, one idea engineers use is:

```
Only ONE request fetches data from DB
Others wait for it
```

What mechanism could implement this?

A)

```
Distributed lock
```

B)

```
Queue requests
```

C)

```
Return stale cache temporarily
```

Which approach would you try first?

---

Excellent 👍
**Distributed lock** is one of the most common techniques to solve **cache stampede**, and interviewers love when candidates mention it. But like everything in distributed systems, engineers discovered some **trade-offs and improvements** over time. Let's continue the story.

---

# Step 9 — Cache Stampede

Imagine we cache a very popular key.

```
homepage_feed
```

Traffic:

```
500K requests/sec
```

Cache entry:

```
homepage_feed → cached result
TTL = 5 minutes
```

Everything works well.

---

## The Moment TTL Expires

At time **T = 5 min**

Cache entry expires.

Now **all incoming requests miss the cache**.

Example:

```
500K requests/sec
```

Flow becomes:

```
500K requests → Database
```

Database gets overwhelmed and crashes.

This is called **Cache Stampede** or **Thundering Herd Problem**.

---

# Solution 1 — Distributed Lock (Your Answer)

Idea:

```
Only ONE request should rebuild the cache.
```

Example flow:

Request 1:

```
Cache miss
↓
Acquire distributed lock
↓
Fetch data from DB
↓
Store in cache
↓
Release lock
```

Other requests:

```
Cache miss
↓
See lock is taken
↓
Wait / retry later
```

Architecture:

```
Client Requests
      │
      ▼
App Server
      │
      ▼
Distributed Cache
      │
      ▼
Distributed Lock System (Redis/Zookeeper)
      │
      ▼
Database
```

---

## Example Timeline

```
T1  Cache expires
T2  Request A gets lock
T3  Request B waits
T4  Request C waits
T5  A fetches DB data
T6  Cache rebuilt
T7  Lock released
T8  B & C read from cache
```

Now DB receives **only one request** instead of thousands.

---

# How Distributed Lock Works

Example using Redis:

```
SET lock_key unique_value NX EX 5
```

Meaning:

```
NX → set only if key not exists
EX → expire in 5 seconds
```

This prevents deadlocks.

---

# Problem With Locks

Locks can introduce:

```
Latency
Waiting threads
Potential bottlenecks
```

Example:

```
1M requests waiting
```

System might stall.

So engineers developed **better strategies**.

---

# Solution 2 — Stale Cache (Serve Old Data)

Instead of blocking requests, we allow **temporary stale reads**.

Idea:

```
If cache expired
→ serve old data
→ refresh cache in background
```

Example:

```
TTL = 5 min
Soft TTL = 4 min
```

Flow:

```
Request arrives at 4.5 min
↓
Return cached data
↓
Background refresh starts
```

Users still get fast responses.

DB protected.

This is called:

```
Stale While Revalidate
```

Used in:

* CDN caches
* HTTP caching
* Many large distributed systems

---

# Solution 3 — Request Coalescing

Idea:

Instead of many requests fetching the same data:

```
Combine them
```

Example:

```
100 requests for same key
```

System merges them into:

```
1 DB request
```

All responses share the result.

Many API gateways and caches implement this.

---

# Now Let's Talk About Another Huge Production Problem

# Hot Key Problem

Imagine a viral event.

Example:

```
World Cup final score
```

Key:

```
match_score
```

Traffic:

```
3M requests/sec
```

But consistent hashing placed this key on:

```
Cache Server 7
```

Now:

```
Cache7 CPU = 100%
```

Other cache nodes idle.

This is **hot key problem**.

---

# Solution 1 — Key Replication

Instead of one copy:

```
match_score_1
match_score_2
match_score_3
match_score_4
```

Stored across multiple nodes.

Reads randomly distributed.

Example:

```
Client → random replica
```

Load spreads across cluster.

---

# Solution 2 — Local Cache (L1 + L2)

Large systems often use **two-layer caching**.

Architecture:

```
        ┌──────────────┐
        │   L1 Cache   │ (inside app server)
        └──────┬───────┘
               │
               ▼
        ┌──────────────┐
        │ Distributed  │
        │ Cache (L2)   │
        └──────┬───────┘
               │
               ▼
            Database
```

Example flow:

```
App → L1 cache
     ↓ miss
App → L2 cache
     ↓ miss
DB
```

Hot keys get cached in **many app servers** automatically.

This drastically reduces load.

Used by:

* Facebook TAO
* LinkedIn
* Twitter

---

# Now Our Distributed Cache System Is Nearly Complete

We have covered most **HLD interview topics**:

```
✔ Why caching exists
✔ Centralized cache
✔ Distributed cache
✔ Sharding
✔ Consistent hashing
✔ Virtual nodes
✔ Replication
✔ Read/write strategies
✔ Cache invalidation
✔ Eviction policies
✔ TTL
✔ Cache stampede
✔ Hot key problem
✔ Multi-layer caching
```

But interviewers often still ask about **failure scenarios**.

For example:

```
What happens if a cache node dies?
```

or

```
How do we rebalance the cluster when nodes are added/removed?
```

These topics include:

```
Cluster membership
Failure detection
Data rebalancing
Consistent hashing movement
Replication recovery
```

---

Before we continue, here's the **next interview reasoning question**.

Suppose a cache node crashes.

Example:

```
Cache3 goes down
```

Keys assigned to it are now unavailable.

Which strategy would you use to recover quickly?

A)

```
Fetch missing keys from DB
```

B)

```
Use replica nodes immediately
```

C)

```
Rebuild cache lazily when requests arrive
```

Which approach would you choose **first** in a distributed cache system and why?

---

Excellent 👍
That’s exactly what **most production distributed caches do** — **use replica nodes immediately** when a primary node fails.

Let’s continue the engineering story.

---

# Step 10 — Cache Node Failure

Our distributed cache cluster currently looks like this:

```
          App Servers
               │
        Consistent Hashing
               │
    ┌──────────┬──────────┬──────────┐
    │          │          │          │
 Cache1      Cache2     Cache3     Cache4
    │          │          │          │
 Replica    Replica     Replica    Replica
```

Example key:

```
user:123 → Primary: Cache3
           Replica: Cache7
```

Now imagine a failure.

```
Cache3 crashes
```

Without replication:

```
All keys on Cache3 lost
↓
Massive cache misses
↓
DB overload
```

System could crash.

---

# Solution — Replica Promotion

Because we already stored a replica:

```
Cache7
```

We simply **promote the replica to primary**.

New mapping becomes:

```
user:123 → Primary: Cache7
```

Requests continue normally.

---

# How The System Detects Failure

In distributed systems we use **heartbeat monitoring**.

Each cache node periodically sends:

```
"I am alive"
```

to a **cluster manager**.

Example architecture:

```
           Cluster Manager
                │
     ┌──────────┼──────────┐
     │          │          │
  Cache1     Cache2     Cache3
     │
  Replica nodes
```

If heartbeat stops:

```
Node marked DEAD
```

Failover triggered.

---

# Example Timeline

```
T1  Cache3 crashes
T2  Heartbeat timeout detected
T3  Cluster manager marks node down
T4  Replica Cache7 promoted
T5  Routing table updated
T6  Traffic redirected
```

Downtime:

```
Few milliseconds to seconds
```

---

# But There Is Another Challenge

After failover we now have:

```
Primary = Cache7
Replica = missing
```

Replication factor reduced.

Originally:

```
Replication factor = 2
```

Now:

```
Replication factor = 1
```

Which increases risk.

So system must **recreate missing replicas**.

---

# Replica Rebuilding

Example cluster:

```
Cache1
Cache2
Cache3 (dead)
Cache4
Cache5
```

Keys from Cache3 must now be replicated elsewhere.

Using consistent hashing:

```
New node selected
```

Example:

```
Replica rebuilt on Cache4
```

Flow:

```
Cache7 (primary)
↓
Copy data
↓
Cache4 (new replica)
```

Cluster returns to safe state.

---

# Next Problem — Cluster Rebalancing

Now suppose we **add a new cache node**.

Example:

```
Cache1
Cache2
Cache3
Cache4
Cache5  ← new
```

Consistent hashing ensures only **small portion of keys move**.

Example:

```
~1/N keys migrate
```

Migration flow:

```
Old node
↓
Transfer keys
↓
New node
```

This happens **gradually** to avoid overload.

Large systems do:

```
background streaming migration
```

---

# Handling Traffic During Rebalancing

During migration:

```
Some keys exist on two nodes
```

System ensures:

```
Reads succeed from either node
```

Writes go to:

```
new owner node
```

Eventually:

```
old copy deleted
```

---

# Production Features Added

Real distributed cache systems include:

```
✔ Automatic failover
✔ Replica promotion
✔ Data rebalancing
✔ Background replication recovery
✔ Consistent hashing migration
```

Examples:

* Redis Cluster
* Amazon ElastiCache
* Facebook TAO cache
* Memcached clusters

---

# Monitoring & Observability

In production we must monitor:

### Cache Hit Ratio

```
cache_hits / total_requests
```

Example:

```
95% hit ratio
```

Meaning:

```
Only 5% requests hit DB
```

Very good.

---

### Latency

```
p50
p95
p99
```

Typical:

```
p99 < 5ms
```

---

### Memory Usage

Monitor:

```
eviction rate
```

High eviction rate means:

```
cache too small
```

---

### Hot Keys

Monitor keys with abnormal traffic.

Example:

```
top 100 keys by QPS
```

---

# Final Distributed Cache Architecture

Putting everything together:

```
                      Clients
                         │
                         ▼
                    Load Balancer
                         │
                         ▼
                     App Servers
                         │
                 ┌───────┴────────┐
                 │ Consistent Hash │
                 └───────┬────────┘
                         │
     ┌──────────┬────────┬────────┬────────┐
     │          │        │        │        │
  Cache1     Cache2   Cache3   Cache4   Cache5
     │          │        │        │        │
  Replica    Replica   Replica   Replica   Replica
     │
  LRU / TTL eviction
     │
  Distributed Lock
     │
  Hot key replication
     │
      ▼
   Database (source of truth)
```

---

# What You Should Say In Interviews

When asked **Design Distributed Cache**, the interviewer expects topics like:

### Core

```
Central cache
Sharding
Consistent hashing
Virtual nodes
Replication
```

### Scaling

```
Horizontal scaling
Cluster rebalancing
Hot key mitigation
```

### Reliability

```
Replication
Failover
Replica promotion
```

### Performance

```
Eviction policy
TTL
L1 + L2 cache
```

### Edge Cases

```
Cache stampede
Hot keys
Cache invalidation
```

---

# One Last Question (Very Common in FAANG Interviews)

Suppose **cache cluster completely crashes**.

Example:

```
Entire Redis cluster down
```

Suddenly:

```
All traffic goes to DB
```

DB might crash.

Which protection strategy would you use?

A)

```
Rate limit requests
```

B)

```
Graceful degradation
```

C)

```
Serve stale data from local cache
```

D)

```
All of the above
```

What would you answer in an interview? 😄

