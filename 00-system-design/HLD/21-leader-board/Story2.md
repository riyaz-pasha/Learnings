Great question for HLD interviews. This problem appears **very frequently in gaming, competitive platforms, and social apps**, so understanding it deeply will help you answer multiple system design questions.

Since you prefer **story-based incremental learning**, we will do this **step-by-step exactly like an interview discussion**.

We will not jump to the final architecture immediately.
Instead we will **evolve the system step-by-step like engineers at a company would**.

---

# Step 0 — The Story (Why Leaderboards Exist)

Imagine a company building a **global multiplayer gaming platform**.

Examples:

* Battle games
* Coding competitions
* Fitness apps
* Fantasy sports

Every user earns **points**.

Example:

| User  | Score |
| ----- | ----- |
| Alice | 1200  |
| Bob   | 1500  |
| John  | 900   |

Players want to see:

* Top 10 players globally
* Their **own rank**
* Rankings among **friends**
* Rankings **updated instantly**

Example UI:

```
Global Leaderboard

1. Bob      1500
2. Alice    1200
3. John      900
```

---

# Step 1 — Clarify Requirements (Interview MUST DO)

This is **the first thing interviewers expect**.

### Functional Requirements

1. Update player score
2. Get **Top K players**
3. Get **rank of a player**
4. Leaderboard should update **in real time**
5. Possibly **multiple leaderboards**

   * Global
   * Country
   * Friends

---

### Non Functional Requirements

These matter **a lot**.

* Extremely **low latency**
* **Real-time updates**
* **Millions of players**
* **High write throughput**
* **Highly available**

Example scale:

```
Users: 100M
Concurrent users: 5M
Score updates: 200K / sec
Leaderboard reads: 500K / sec
```

---

# Step 2 — The Naive Solution (First Attempt)

Early engineers implemented something simple.

### Database Table

```
Leaderboard

user_id
score
```

Example:

| user_id | score |
| ------- | ----- |
| u1      | 100   |
| u2      | 200   |
| u3      | 150   |

---

### Query Top Players

```
SELECT user_id, score
FROM leaderboard
ORDER BY score DESC
LIMIT 10
```

---

### Query User Rank

```
SELECT COUNT(*)
FROM leaderboard
WHERE score > user_score
```

Rank = count + 1

---

# Problem With This Approach

Looks fine for small systems.

But at scale:

### Problem 1 — Sorting is Expensive

Every query requires:

```
ORDER BY score
```

Sorting **100M users** repeatedly is expensive.

---

### Problem 2 — Rank Query is Expensive

To find rank:

```
COUNT(score > user_score)
```

This scans a huge dataset.

---

### Problem 3 — Too Many Writes

Scores update frequently.

Example:

```
200K score updates/sec
```

Database becomes the bottleneck.

---

### Problem 4 — Not Real-Time

Database queries become slow:

```
1–5 seconds latency
```

Users expect **milliseconds**.

---

### Interview Insight

At this point interviewers expect you to say:

> "Relational databases are not ideal for real-time ranking."

We need a **different data structure**.

---

# Step 3 — Key Insight

Leaderboard problems are basically:

```
Maintain a sorted list that updates frequently
```

Which data structures support this?

Possible answers:

1. Heap
2. Balanced BST
3. Skip list
4. Sorted set

In distributed systems, the most common solution is:

```
Redis Sorted Sets (ZSET)
```

---

# Step 4 — Redis Sorted Set

Redis supports:

```
ZADD leaderboard score user
```

Example:

```
ZADD leaderboard 1200 Alice
ZADD leaderboard 1500 Bob
ZADD leaderboard 900 John
```

---

### Redis Internally Uses

```
Hash Table + Skip List
```

Skip list maintains **sorted order efficiently**.

---

### Get Top Players

```
ZREVRANGE leaderboard 0 9 WITHSCORES
```

Returns:

```
1. Bob    1500
2. Alice  1200
3. John    900
```

Time Complexity:

```
O(log N)
```

Very fast.

---

### Get Player Rank

```
ZREVRANK leaderboard Alice
```

Output:

```
1
```

---

### Update Score

```
ZADD leaderboard 1800 Alice
```

Redis automatically reorders leaderboard.

---

### Performance

| Operation | Complexity   |
| --------- | ------------ |
| Insert    | O(log N)     |
| Update    | O(log N)     |
| Get Rank  | O(log N)     |
| Top K     | O(log N + K) |

Very efficient.

---

# Step 5 — Architecture (First Version)

Simple system.

```
Clients
   |
   v
API Servers
   |
   v
Redis Leaderboard
```

Flow:

```
User scores -> API -> Redis
Leaderboard query -> Redis -> Response
```

---

### Why Redis?

Because it provides:

* In-memory speed
* Sorted data structures
* Atomic operations

Latency:

```
~1ms
```

Perfect for real-time systems.

---

# First Interview Milestone

At this point in an interview you already showed:

✔ Problem understanding
✔ Data structure selection
✔ Basic architecture

But we are **not even 20% done**.

Real systems still have major problems:

1. Redis memory limits
2. Millions of writes
3. Global scale
4. Fault tolerance
5. Sharding
6. Replication
7. Consistency
8. Top K across shards

These are the **actual interview discussion points**.

---

# Before moving further

Let's pause here (as you requested).

Make sure you clearly understand:

1. Why **SQL is bad for leaderboard**
2. Why **Redis Sorted Set works**
3. How **rank queries work**

---

### Small check for you

In interviews they often ask:

**Question 1**

If a player score increases from:

```
1000 -> 1500
```

Do we:

A) Delete and insert
B) Update score using ZADD
C) Rebuild leaderboard

What would you answer and **why**?

After that we will move to the **next big challenge**:

> **Redis memory explosion when leaderboard has 100M users**

This is where **real system design begins.**

---

Correct ✅

**Answer: B) Update score using `ZADD`**

But the **important part (which interviewers care about)** is *why*.

---

# Why `ZADD` Works for Updates

Redis `ZADD` behaves like **UPSERT**.

Meaning:

```text
If member does not exist → INSERT
If member already exists → UPDATE score
```

Example:

Initial leaderboard

```
Alice 1200
Bob   1500
John   900
```

Command:

```
ZADD leaderboard 1800 Alice
```

Redis will:

1. Find **Alice**
2. Update score **1200 → 1800**
3. Reposition Alice in sorted order

New leaderboard:

```
1. Alice 1800
2. Bob   1500
3. John   900
```

---

# What Happens Internally (Important for Interviews)

Redis Sorted Set internally uses:

```
HashMap + SkipList
```

### HashMap

```
member -> score
```

Example:

```
Alice -> 1200
Bob   -> 1500
John  -> 900
```

This allows **O(1)** lookup to find if user exists.

---

### Skip List

Maintains sorted order:

```
900  -> John
1200 -> Alice
1500 -> Bob
```

When score updates:

1. Find member in HashMap
2. Remove node from SkipList
3. Insert new score position

Complexity:

```
O(log N)
```

Very fast even for **millions of players**.

---

# Why Not Delete + Insert?

You could do:

```
ZREM Alice
ZADD leaderboard 1800 Alice
```

But that means **two operations instead of one**.

Also:

```
ZADD already handles update atomically
```

Which is better for **concurrent updates**.

---

# Interview Tip (Important)

A good answer in interviews is:

> "Redis `ZADD` automatically updates the score if the member already exists. Internally Redis removes the old node from the skip list and reinserts it with the new score in **O(log N)** time."

That line alone shows **strong system knowledge**.

---

# Now Let's Continue the Story

Our system currently looks like this:

```
Players
   |
API Servers
   |
Redis Leaderboard
```

Works perfectly for **small scale**.

But then the game becomes **globally popular**.

Example scale:

```
Users: 100M
Score updates: 500K/sec
Leaderboard reads: 1M/sec
```

Now the engineers start seeing **major problems**.

---

# Problem #1 — Redis Memory Explosion

Redis is **in-memory**.

Assume each leaderboard entry takes roughly:

```
~100 bytes
```

If we store:

```
100M players
```

Memory required:

```
100M × 100B = 10GB
```

But Redis also needs:

* replication
* overhead
* skip list nodes
* fragmentation

Actual memory becomes:

```
30–40 GB
```

Single Redis node **cannot handle this safely**.

---

# Problem #2 — Single Node Bottleneck

Current system:

```
Clients
   |
API
   |
Redis (single node)
```

Problems:

1️⃣ CPU bottleneck
2️⃣ Memory bottleneck
3️⃣ Network bottleneck

Worst case:

```
If Redis crashes → leaderboard gone
```

Very bad for production.

---

# Problem #3 — Global Latency

Players from:

```
USA
Europe
India
Japan
```

All hitting **one Redis cluster**.

Latency becomes:

```
200ms+
```

Players complain:

> "Leaderboard updates slowly."

---

# Engineers Realize

We must introduce:

```
Sharding
```

Meaning:

```
Split leaderboard across multiple machines
```

Example:

```
Shard 1 → users 1M–20M
Shard 2 → users 20M–40M
Shard 3 → users 40M–60M
Shard 4 → users 60M–80M
Shard 5 → users 80M–100M
```

Now memory + load distributed.

---

But **this introduces a BIG interview problem**.

If leaderboard is split like this:

```
Shard1
Shard2
Shard3
Shard4
```

How do we compute:

```
GLOBAL TOP 10 ?
```

Because the **top players may exist on any shard**.

This is one of the **most important leaderboard design questions**.

---

# Before we move forward

Quick check question (very common interview question):

Suppose we have **10 Redis shards**.

Each shard stores **10M players**.

How would you compute:

```
Global Top 100
```

Without scanning all **100M users**?

Give your thought process. Then we'll continue the story. This next part introduces **one of the most important distributed system patterns used by companies like Twitch, Riot Games, and Discord.**

---

Good thinking 👍 — you are trying to **reduce the search space by using score ranges**. That’s a common instinct in distributed systems.

But in practice **sharding by score is a bad strategy for leaderboards**. Let’s explore *why*, because this exact discussion happens in real interviews.

---

# Why Sharding by Score Looks Attractive

Your idea:

```text
Shard 1 → score 0–1000
Shard 2 → score 1000–2000
Shard 3 → score 2000–3000
Shard 4 → score 3000–4000
```

If we want **top players**, we query the highest shard first.

Example:

```text
Top players must be in shard 4
```

So it sounds efficient.

---

# Problem 1 — Scores Keep Changing

Leaderboard scores are **dynamic**.

Example:

| User  | Old Score | New Score |
| ----- | --------- | --------- |
| Alice | 950       | 1200      |
| Bob   | 1800      | 2500      |

Now users must **move between shards**.

Example:

```text
Alice moves from shard1 → shard2
Bob moves from shard2 → shard3
```

This means:

* delete from one shard
* insert into another shard

At scale:

```text
500K updates/sec
```

Cross-shard movement becomes extremely expensive.

---

# Problem 2 — Hot Shard Problem

Top players usually cluster near the **highest scores**.

Example distribution:

```text
Shard1 (0–1000) → 80M users
Shard2 (1000–2000) → 15M users
Shard3 (2000–3000) → 4M users
Shard4 (3000+) → 1M users
```

But **all leaderboard reads** go to shard4.

This creates:

```text
HOT SHARD
```

Shard4 becomes overloaded.

---

# Problem 3 — Score Range is Unknown

When the game launches:

```text
Max score = ?
```

Maybe later:

```text
Players reach 1,000,000 points
```

Now you must **reshard everything**.

This is extremely painful.

---

# Industry Solution Instead

Instead of **score-based sharding**, companies shard by:

```text
user_id
```

Example:

```text
Shard1 → users hash(user_id) % 10 = 0
Shard2 → users hash(user_id) % 10 = 1
Shard3 → users hash(user_id) % 10 = 2
...
Shard10
```

Benefits:

✔ Even distribution
✔ No hot shard
✔ No cross-shard movement when score changes

---

# But Now We Have the Big Problem

Each shard has its **own leaderboard**.

Example:

```
Shard1 Top Players
1 Alice  5000
2 Tom    4800
3 Mike   4700
```

```
Shard2 Top Players
1 Bob    5200
2 John   4900
3 Alex   4600
```

But the **global leaderboard** should be:

```
1 Bob    5200
2 Alice  5000
3 John   4900
4 Tom    4800
```

So how do we compute this?

---

# The Key Trick (Used in Production)

Instead of storing **all users globally**, we only store **top players from each shard**.

Example:

Each shard keeps:

```text
Top 1000 players
```

If we have:

```text
10 shards
```

We only merge:

```text
10 × 1000 = 10,000 players
```

Instead of:

```text
100M players
```

---

# Aggregator Service

Architecture evolves to this:

```
                +----------------+
                |  Leaderboard   |
                |   Aggregator   |
                +--------+-------+
                         |
      -------------------------------------------
      |        |        |        |        |
   Redis1   Redis2   Redis3   Redis4   Redis5
   (Shard)  (Shard)  (Shard)  (Shard)  (Shard)
```

Each shard periodically sends:

```
Top 1000 players
```

Aggregator merges them using a **heap**.

Complexity:

```
O(K log S)

K = top players per shard
S = shards
```

Very fast.

---

# Example

Shard outputs:

```
Shard1: 1000 players
Shard2: 1000 players
Shard3: 1000 players
Shard4: 1000 players
Shard5: 1000 players
```

Total candidates:

```
5000 players
```

Aggregator computes:

```
Global Top 100
```

in milliseconds.

---

# This is the **Pattern**

This architecture is known as:

```
Hierarchical Leaderboard Aggregation
```

Used by:

* large gaming companies
* esports platforms
* competitive coding platforms

---

# Updated Architecture

```
Players
   |
API Servers
   |
Shard Router
   |
--------------------------------
|        |        |        |
Redis1  Redis2  Redis3  Redis4
(shard) (shard) (shard) (shard)
   |
Top K export
   |
Leaderboard Aggregator
   |
Global Leaderboard API
```

---

# We Are Now ~40% Through the Real Design

Still remaining (the **most interesting parts**):

1️⃣ **How to compute player rank globally**
2️⃣ **How to keep leaderboard real-time**
3️⃣ **Replication and failover**
4️⃣ **Handling 1M writes/sec**
5️⃣ **Streaming updates using Kafka**
6️⃣ **Region based leaderboards**
7️⃣ **Friend leaderboards**
8️⃣ **Redis cluster architecture**
9️⃣ **Failure handling**
10️⃣ **Consistency tradeoffs**

These are the **exact topics interviewers explore**.

---

# Before moving forward

Let me ask you a **very important interview question**:

Suppose we already know:

```
Global Top 100 players
```

Now a random player asks:

```
"What is my rank?"
```

Example:

```
Player score = 3500
Total players = 100M
```

How would you compute their **rank efficiently**?

(Without scanning all players.)

Think about this — it's a **classic leaderboard design problem**.

---

Great — this is one of the **most important discussions in a leaderboard HLD interview**.
Interviewers ask this because it tests whether you understand **distributed ranking**.

We’ll continue the **story evolution**.

---

# The Problem

A player asks:

```text
"What is my rank?"
```

Example:

| Player | Score |
| ------ | ----- |
| Bob    | 5200  |
| Alice  | 5000  |
| John   | 4900  |
| ...    | ...   |
| You    | 3500  |

Total players:

```text
100,000,000
```

We need to compute:

```text
Rank of player with score 3500
```

---

# First Attempt (Naive)

Engineers tried:

```sql
COUNT(players WHERE score > my_score)
```

Example:

```text
COUNT(score > 3500)
```

Rank:

```text
rank = count + 1
```

---

### Problem

This requires scanning **all players**.

If:

```text
100M users
```

Every query becomes expensive.

Latency becomes:

```text
seconds
```

Not acceptable.

---

# But Remember Our Architecture

We already **sharded the leaderboard by user_id**.

Example:

```
Redis Shards

Shard1
Shard2
Shard3
Shard4
Shard5
```

Each shard stores **subset of users**.

So the player exists in **one shard**.

---

# Key Insight

Redis Sorted Sets already know **rank inside a shard**.

Example:

```
Shard3
```

```text
1  Alice   5000
2  Tom     4800
3  Mike    4700
...
50,000  You 3500
```

Command:

```
ZREVRANK leaderboard user_id
```

Returns:

```
50000
```

Meaning:

```text
Rank inside shard = 50,000
```

But that is **not global rank**.

---

# The Real Trick

To compute global rank we need:

```text
Players with score higher than mine
```

Across **all shards**.

But we don't scan everything.

Instead we ask each shard:

```text
How many players have score > my_score ?
```

Redis supports this.

---

# Redis Command

```
ZCOUNT leaderboard (3500 +inf)
```

Meaning:

```text
Count players with score > 3500
```

Each shard returns a number.

Example:

```
Shard1 → 1200
Shard2 → 900
Shard3 → 700
Shard4 → 400
Shard5 → 200
```

---

# Global Rank Calculation

Add them:

```
1200 + 900 + 700 + 400 + 200 = 3400
```

Rank:

```
rank = 3400 + 1
```

Final rank:

```
3401
```

---

# Architecture Flow

```
Player → API Server
        |
        v
Leaderboard Rank Service
        |
        |-- Shard1 → ZCOUNT
        |-- Shard2 → ZCOUNT
        |-- Shard3 → ZCOUNT
        |-- Shard4 → ZCOUNT
        |-- Shard5 → ZCOUNT
        |
        v
Sum results
        |
Return Rank
```

---

# Complexity

If we have:

```
S = number of shards
```

Query cost:

```
O(S log N)
```

Example:

```
10 shards
```

Only **10 queries**.

Very fast.

---

# Interview Insight

This is called:

```text
Distributed Rank Aggregation
```

Pattern:

```
local computation + global aggregation
```

Used everywhere in distributed systems.

---

# But Engineers Still Found Problems

At scale:

```
Rank queries = 500K/sec
Shards = 20
```

Each rank request triggers:

```
20 Redis queries
```

Total Redis load becomes huge.

---

# Engineers Introduced Optimization

Instead of querying all shards every time, we maintain:

```text
Score Buckets
```

Example histogram:

| Score Range | Players |
| ----------- | ------- |
| 5000+       | 10      |
| 4500–5000   | 200     |
| 4000–4500   | 2000    |
| 3500–4000   | 10000   |
| 3000–3500   | 50000   |

Now rank can be estimated quickly.

This is called:

```text
Rank Approximation
```

Many games use this.

---

# Current System So Far

```
Players
   |
API Servers
   |
Shard Router
   |
--------------------------------
|        |        |        |
Redis1  Redis2  Redis3  Redis4
(shard) (shard) (shard) (shard)
   |
Top-K Export
   |
Leaderboard Aggregator
   |
Global Leaderboard
```

And rank queries use:

```
ZCOUNT across shards
```

---

# But We Haven’t Solved a Major Problem Yet

Real games have **millions of score updates per second**.

Example:

```
Score updates = 1M/sec
```

Directly writing to Redis becomes dangerous.

Problems appear:

1. Redis overload
2. Network spikes
3. Lost updates during failures
4. Write amplification

---

# Real Systems Add a Buffer Layer

Engineers introduce:

```
Kafka / Streaming Queue
```

Architecture evolves to:

```
Players
   |
Game Servers
   |
Kafka (score updates)
   |
Leaderboard Workers
   |
Redis Leaderboard
```

Benefits:

✔ smooth traffic spikes
✔ retries if Redis fails
✔ async processing
✔ event history

---

# Next Step in Our Story (Very Important)

The next challenge engineers faced was:

```
Real-time leaderboard updates for millions of players watching
```

Example:

```
10M players watching leaderboard live
```

If everyone polls the API:

```
API meltdown
```

Companies solved this using:

```
Streaming Leaderboards
(WebSockets / PubSub)
```

This part is **extremely common in Google / Meta / Uber interviews**.

---

Before we continue, quick check question for you:

Suppose **10M users are watching the leaderboard page**.

If each client polls:

```
GET /leaderboard
```

every **2 seconds**.

How many requests per second will the backend receive?
And why is that a **huge problem**?

---
You’re **very close**, but let's calculate it precisely — interviewers often check this quickly.

---

# Calculation

Given:

```text
Users watching leaderboard = 10,000,000
Polling interval = 2 seconds
```

Requests per second:

```text
10,000,000 / 2
```

Result:

```text
5,000,000 requests/sec
```

So your answer:

```text
5M req/sec
```

✅ **Correct**

---

# Why This Is a Huge Problem

Even if the system is well optimized:

```text
5,000,000 requests/sec
```

is massive.

Let’s see where the pressure appears.

---

## 1️⃣ API Servers

If one API server handles roughly:

```text
10K req/sec
```

We would need:

```text
5,000,000 / 10,000 = 500 servers
```

Just for polling.

---

## 2️⃣ Redis Load

Each request might trigger:

```text
ZREVRANGE leaderboard 0 99
```

So Redis receives:

```text
5M reads/sec
```

Even a Redis cluster would struggle.

---

## 3️⃣ Network Explosion

Each response might be:

```text
Top 100 leaderboard
≈ 5KB
```

Network output:

```text
5KB × 5M = 25GB/sec
```

That’s huge bandwidth.

---

# The Key Insight Engineers Realized

Polling is inefficient because:

```text
Most requests return identical data
```

Example:

Leaderboard might update **once per second**, but clients request **5M times per second**.

That means:

```text
Millions of duplicate responses
```

---

# The Better Solution — Push Model

Instead of:

```text
Client → repeatedly asking server
```

We do:

```text
Server → pushes update when leaderboard changes
```

This is done using:

```text
WebSockets
```

or

```text
Pub/Sub streaming
```

---

# New Architecture

```text
Players
   |
Game Servers
   |
Kafka (score updates)
   |
Leaderboard Workers
   |
Redis Leaderboard
   |
Leaderboard Streaming Service
   |
WebSocket Connections
   |
Millions of Clients
```

---

# How It Works

1️⃣ Score update arrives

```
player score updated
```

2️⃣ Redis leaderboard updates

3️⃣ Streaming service publishes change

4️⃣ All connected clients receive update instantly

---

# Result

Instead of:

```text
5M requests/sec
```

Server only sends updates when needed.

Example:

```text
Leaderboard updates 5 times/sec
```

Network:

```text
5 broadcasts/sec
```

Much smaller load.

---

# Technologies Used

Common stack in production:

| Layer     | Technology           |
| --------- | -------------------- |
| Streaming | WebSockets           |
| Messaging | Kafka / Redis PubSub |
| State     | Redis                |
| Fanout    | Pub/Sub servers      |

Large systems sometimes add:

```text
NATS
or
Kafka Streams
```

---

# Real Companies Using Similar Design

Leaderboard systems like this appear in:

* Riot Games
* Twitch
* Steam
* LeetCode
* Codeforces
* PUBG
* Call of Duty

---

# But A Major Scaling Problem Still Exists

Now imagine:

```text
10M concurrent WebSocket connections
```

A single server cannot maintain that many.

So companies introduce:

```text
Connection Gateways
```

Architecture becomes:

```text
                 +------------------+
                 | Leaderboard Core |
                 +---------+--------+
                           |
                     Pub/Sub System
                           |
       ------------------------------------------------
       |             |             |            |
 WebSocket GW1   WebSocket GW2   WebSocket GW3  WebSocket GW4
       |             |             |            |
   2.5M users     2.5M users     2.5M users    2.5M users
```

Each gateway maintains **millions of connections**.

---

# We Have Now Covered

✔ Sorted sets
✔ Sharding
✔ Global top-K aggregation
✔ Distributed rank calculation
✔ Kafka buffering
✔ Polling vs streaming
✔ WebSocket fanout architecture

These are **core leaderboard interview topics**.

---

# Next (Very Important Interview Topic)

Now we must design:

```text
Fault Tolerant Leaderboard
```

Because if Redis crashes:

```text
Leaderboard disappears
```

So engineers introduce:

```
Replication
Persistence
Failover
Multi-region design
```

This is where concepts like:

* Redis replication
* Redis cluster
* leader election
* consistency tradeoffs

appear.

---

Before we move forward, one **very important interview question** for you:

If Redis stores leaderboard **only in memory**, what happens if:

```text
Redis node crashes?
```

How would you **recover the leaderboard**?

(This question leads us into **replication, persistence, and durability**, which are critical for HLD interviews.)

---
You’re **partially correct**, and your thinking is exactly where many engineers start.

Yes, if Redis is **only in memory with no durability**, then:

```text
Redis crash → data lost
```

But in **real production systems**, we design Redis so that this **does NOT happen**. Engineers add **durability and redundancy**.

Let’s continue the story.

---

# What Engineers Initially Faced

Early leaderboard system:

```text
Players
   |
API
   |
Redis (single node)
```

Everything works great.

Until one day:

```text
Redis server crashes
```

What happens?

```
Leaderboard completely disappears
```

Players see:

```
Rank = 0
Leaderboard empty
```

This is a **huge disaster for games**.

---

# Engineers Add Replication

They introduce a **primary–replica architecture**.

```
            +----------------+
            |   Redis Primary |
            +--------+-------+
                     |
          -------------------------
          |                       |
   Redis Replica 1         Redis Replica 2
```

How it works:

1️⃣ Writes go to **Primary**

```
ZADD leaderboard score user
```

2️⃣ Primary replicates data to **replicas**

3️⃣ Replicas act as **backup copies**

---

# If Primary Crashes

Failover happens.

Replica becomes new primary.

```
Old Primary ❌

Replica1 → promoted to Primary
Replica2 → remains replica
```

This can be automated using:

```text
Redis Sentinel
```

or

```text
Redis Cluster
```

Failover time:

```
~ few seconds
```

System continues working.

---

# But Replication Alone Is Not Enough

Imagine this scenario:

```
Primary crashes
Replica also crashes
Machine disk failure
```

Now **all memory data is gone**.

So engineers add **persistence**.

---

# Redis Persistence

Redis can save data to disk.

Two main methods.

---

# 1️⃣ RDB Snapshots

Redis periodically saves full snapshot.

Example:

```
Every 5 minutes
```

Redis writes:

```
dump.rdb
```

If Redis crashes:

```
Restart Redis
Load snapshot
```

Leaderboard restored.

---

### Problem

We lose **recent updates**.

Example:

```
Snapshot time = 12:00
Crash time = 12:04
```

All updates between **12:00–12:04 lost**.

---

# 2️⃣ AOF (Append Only File)

Instead of snapshot, Redis logs every command.

Example:

```
ZADD leaderboard 1200 Alice
ZADD leaderboard 1500 Bob
ZADD leaderboard 1800 Alice
```

Commands stored in:

```
appendonly.aof
```

If Redis crashes:

```
Replay commands
```

Leaderboard restored.

---

### Advantage

Almost **no data loss**.

---

# Real Production Setup

Most companies use:

```
Redis replication + AOF persistence
```

Architecture:

```
                  +----------------+
                  |   Redis Primary |
                  +--------+-------+
                           |
                 ---------------------
                 |                   |
           Redis Replica       Redis Replica
                 |
          AOF persistence
```

---

# But There Is Still One More Problem

Even if Redis crashes and restarts, **rebuilding leaderboard might take time**.

So engineers use another **safety mechanism**.

Remember earlier we introduced:

```
Kafka (score updates)
```

Kafka stores **event history**.

Example:

```
score_update(user=Alice, score=1200)
score_update(user=Bob, score=1500)
score_update(user=Alice, score=1800)
```

If Redis fails completely:

1️⃣ Restart Redis
2️⃣ Replay Kafka events
3️⃣ Rebuild leaderboard

This is called:

```
Event Sourcing Recovery
```

Very common pattern.

---

# Final Reliable Architecture

```
Players
   |
Game Servers
   |
Kafka (durable log)
   |
Leaderboard Workers
   |
Redis Cluster
   |
Replication + Persistence
   |
Leaderboard API
   |
WebSocket Gateways
   |
Clients
```

Now system is:

```
Fault tolerant
Recoverable
Highly available
```

---

# Interview Insight (Very Important)

A strong candidate usually says something like:

> "Redis is used as a fast in-memory ranking store, but durability is ensured using replication and AOF persistence. Additionally, since score updates flow through Kafka, we can replay events to rebuild the leaderboard if Redis state is lost."

That statement shows **senior-level system thinking**.

---

# Where Interviewers Usually Go Next

After this point they ask about:

```
Multi-region global leaderboards
```

Example:

```
US players
Europe players
Asia players
```

If everyone writes to **one region**, latency becomes huge.

So companies introduce:

```
Regional leaderboards
Global aggregation layer
```

This becomes a **very interesting distributed system problem**.

---

If you'd like, next we can go into something **extremely valuable for interviews**:

### How companies design **Global Multi-Region Leaderboards**

This introduces concepts like:

* geo sharding
* regional aggregation
* eventual consistency
* CRDT-like score merges

This is the **kind of depth that impresses Google interviewers**.

---

Great question. This is **one of the hardest parts of leaderboard design**, and interviewers often push here. Let's break it down **slowly with an example**, because once you see it clearly, the concept becomes very intuitive.

---

# 1️⃣ First Understand What “Rank” Actually Means

Rank is simply:

```
rank = number of players with higher score + 1
```

Example leaderboard:

| Player | Score |
| ------ | ----- |
| Bob    | 5200  |
| Alice  | 5000  |
| John   | 4900  |
| Tom    | 4700  |
| You    | 3500  |

Your rank:

```
players with higher score = 4
rank = 4 + 1 = 5
```

So the **real problem is just this**:

```
How many players have score > my_score ?
```

---

# 2️⃣ Now Introduce Sharding

Suppose we have **4 Redis shards**.

Each shard stores **a subset of users**.

```
Shard1 → users 1–25M
Shard2 → users 25–50M
Shard3 → users 50–75M
Shard4 → users 75–100M
```

Each shard keeps a **sorted set**:

```
ZSET leaderboard
```

Example data.

---

### Shard1

| Player | Score |
| ------ | ----- |
| A      | 5000  |
| B      | 4500  |
| C      | 3000  |

---

### Shard2

| Player | Score |
| ------ | ----- |
| D      | 4800  |
| E      | 4200  |
| F      | 3500  |

---

### Shard3

| Player | Score |
| ------ | ----- |
| G      | 5200  |
| H      | 3900  |
| I      | 3200  |

---

### Shard4

| Player | Score |
| ------ | ----- |
| J      | 4700  |
| K      | 4100  |
| L      | 3300  |

---

Now suppose **player F asks their rank**.

```
score = 3500
```

---

# 3️⃣ Step 1 — Find Player Score

First we ask **the shard where the user exists**.

Example:

```
Zscore leaderboard F
```

Result:

```
3500
```

Now we know the player's score.

---

# 4️⃣ Step 2 — Ask Each Shard

We now ask every shard:

```
How many players have score > 3500 ?
```

Redis command:

```
ZCOUNT leaderboard (3500 +inf)
```

Meaning:

```
count scores greater than 3500
```

---

### Query Shard1

Players:

| Player | Score |
| ------ | ----- |
| A      | 5000  |
| B      | 4500  |
| C      | 3000  |

Scores > 3500:

```
A
B
```

Count:

```
2
```

---

### Query Shard2

| Player | Score |
| ------ | ----- |
| D      | 4800  |
| E      | 4200  |
| F      | 3500  |

Scores > 3500:

```
D
E
```

Count:

```
2
```

---

### Query Shard3

| Player | Score |
| ------ | ----- |
| G      | 5200  |
| H      | 3900  |
| I      | 3200  |

Scores > 3500:

```
G
H
```

Count:

```
2
```

---

### Query Shard4

| Player | Score |
| ------ | ----- |
| J      | 4700  |
| K      | 4100  |
| L      | 3300  |

Scores > 3500:

```
J
K
```

Count:

```
2
```

---

# 5️⃣ Step 3 — Aggregate

Now the **Rank Service sums results**.

```
Shard1 = 2
Shard2 = 2
Shard3 = 2
Shard4 = 2
```

Total players with higher score:

```
2 + 2 + 2 + 2 = 8
```

Final rank:

```
rank = 8 + 1 = 9
```

---

# 6️⃣ Architecture Flow

```
Player
  |
Leaderboard API
  |
Rank Service
  |
 -------------------------------
 |      |      |      |
Shard1 Shard2 Shard3 Shard4
(ZCOUNT)(ZCOUNT)(ZCOUNT)(ZCOUNT)
  |
Sum counts
  |
Return rank
```

---

# 7️⃣ Why This Is Fast

Each shard query is:

```
O(log N)
```

If:

```
20 shards
```

Total work:

```
20 fast Redis queries
```

This is extremely fast compared to scanning:

```
100 million players
```

---

# 8️⃣ Real Optimization (Used in Production)

In practice, companies also cache results.

Example:

```
Rank Cache
```

Because many users ask repeatedly:

```
"What is my rank?"
```

So results are cached for:

```
1–5 seconds
```

Which dramatically reduces load.

---

# 9️⃣ Important Interview Insight

This pattern is called:

```
Distributed Count Aggregation
```

You compute:

```
local result → global aggregation
```

This pattern appears in many systems like:

* analytics queries
* distributed SQL
* search ranking

---

# 10️⃣ But There Is One Hidden Problem

If we have:

```
20 shards
Rank requests = 500K/sec
```

Each request needs:

```
20 Redis queries
```

Total Redis queries:

```
10 million queries/sec
```

Which is **too high**.

Companies solve this using **score buckets / histograms**.

That optimization is **very interesting and often asked in interviews**.

---

If you'd like, next I can explain:

**The advanced trick large gaming systems use to compute ranks in ~1 query instead of querying all shards.**

It's a **very clever technique used by large-scale leaderboard systems** and is something **Google interviewers love to hear.**
