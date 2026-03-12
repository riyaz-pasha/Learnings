In **System Design interviews (especially Google L5)**, interviewers **do NOT expect long back-of-the-envelope calculations**. What they want is:

* Do you understand **scale**?
* Do you know **where bottlenecks will happen**?

So spending **2–3 minutes to estimate a few key numbers is enough**.

You only need **4–5 metrics** to guide the entire design.

---

# The 5 Key Metrics to Gather (2–3 min version)

Whenever a design question starts, quickly ask or assume these:

```
1. Traffic (QPS)
2. Read vs Write ratio
3. Data size
4. Latency requirement
5. Growth / Peak factor
```

These **five metrics drive almost every architecture decision**.

---

# 1️⃣ Traffic (QPS) — Most Important

Ask:

> How many requests per second?

Example:

```
Daily Active Users = 100M
Requests per user per day = 20

Total requests = 2B/day
QPS ≈ 23k
```

You **don't need exact numbers**. Just approximate.

Example answers in interview:

```
Assume ~50k requests/sec
```

Why this matters:

| QPS  | Implication        |
| ---- | ------------------ |
| <1k  | single DB possible |
| 10k  | caching required   |
| 100k | sharding required  |
| 1M   | distributed system |

---

# 2️⃣ Read vs Write Ratio

Ask:

```
Is the system read-heavy or write-heavy?
```

Example systems:

| System           | Read/Write  |
| ---------------- | ----------- |
| Twitter timeline | 100:1       |
| YouTube watch    | 1000:1      |
| Ride booking     | balanced    |
| Chat system      | write heavy |

Why it matters:

| Pattern     | Architecture         |
| ----------- | -------------------- |
| Read heavy  | Cache + CDN          |
| Write heavy | Queue + partitioning |
| Balanced    | scale both           |

Example statement:

```
Assume reads are 100x writes.
```

---

# 3️⃣ Data Size (Storage)

Ask:

```
How much data are we storing?
```

Example:

```
10M users
each user stores 1MB
= 10TB
```

Why this matters:

| Data size | Decision            |
| --------- | ------------------- |
| <1TB      | single DB possible  |
| 10TB      | sharding            |
| 100TB+    | distributed storage |

Example interview answer:

```
Assume total data size is around 20TB.
```

---

# 4️⃣ Latency Requirement

Ask:

```
Is this real-time or eventual consistency acceptable?
```

Examples:

| System        | Latency    |
| ------------- | ---------- |
| Google search | <200ms     |
| Chat          | <100ms     |
| Analytics     | minutes OK |

Why it matters:

| Latency | Architecture     |
| ------- | ---------------- |
| <100ms  | cache            |
| seconds | async processing |
| minutes | batch            |

Example interview line:

```
Users expect response within ~200ms.
```

---

# 5️⃣ Peak Traffic / Growth

Traffic **is never constant**.

Ask:

```
What is peak traffic?
```

Example:

```
Average = 20k QPS
Peak = 5x

Peak QPS = 100k
```

Examples:

| System         | Peak multiplier |
| -------------- | --------------- |
| Ecommerce sale | 10x             |
| Sports events  | 20x             |
| Normal apps    | 3x              |

Example interview statement:

```
We should design for 5x peak traffic.
```

---

# The 2-Minute Script (What Top Candidates Say)

In interviews you can say:

```
Let me estimate some quick numbers.

Assume:
• ~50M daily active users
• ~20 actions per user per day

That gives roughly 1B requests/day (~12k QPS).

Assume read-heavy workload (~100:1).

Total data may grow to ~20TB.

Users expect responses within ~200ms.

We should design for 5x peak traffic.
```

Done.

Takes **less than 1 minute**.

And now interviewer knows:

✔ You understand scale
✔ You understand bottlenecks
✔ You are thinking production-grade

---

# Why These 5 Metrics Are Enough

Because they answer the **big architectural questions**:

| Question                   | Metric       |
| -------------------------- | ------------ |
| Can 1 DB handle it?        | QPS          |
| Do we need cache?          | Read ratio   |
| Do we need sharding?       | Data size    |
| Do we need async?          | Latency      |
| Can system survive spikes? | Peak traffic |

Everything else derives from these.

---

# What NOT to Waste Time On

Avoid calculating:

❌ exact storage bytes
❌ exact CPU cores
❌ exact network bandwidth
❌ exact disk IOPS

Interviewers **don't care**.

---

# Pro Tip (What Google Interviewers Love)

After metrics say this:

```
Given this scale (~100k peak QPS),
a single database won't scale,
so we will design a horizontally scalable architecture.
```

This shows **system thinking immediately**.

---

# One More Thing (Very Powerful)

Many candidates miss **one critical metric**.

The **6th metric (optional but impressive)**:

```
Object size
```

Example:

| System  | Object          |
| ------- | --------------- |
| Twitter | tweet size      |
| YouTube | video size      |
| Email   | attachment size |

This determines **bandwidth + storage design**.

---

✅ **Summary**

The only metrics you need:

```
1. QPS (traffic)
2. Read vs Write ratio
3. Data size
4. Latency requirement
5. Peak traffic
```

Spend **2 minutes here max**.

---

If you'd like, I can also show you the **Google-level trick that reduces these 5 metrics to just 3 questions** (many senior candidates use this to move extremely fast in interviews).

---

Great. The trick senior candidates use is to **compress the 5 metrics into just 3 questions**.

This keeps the **metrics discussion under 1 minute**, which is ideal in interviews like **Google L5 system design**.

---

# The 3 Questions Trick (Senior Candidate Style)

Instead of asking many numbers, ask just **three questions**:

```
1. What is the scale of the system?
2. Is the workload read-heavy or write-heavy?
3. Are there strict latency or consistency requirements?
```

From these **three questions**, you can derive everything.

---

# 1️⃣ Question: “What is the scale of the system?”

This single question gives you **3 metrics at once**:

* Users
* QPS
* Data size

Example:

```
What scale should we design for?
For example:
• number of users
• requests per second
• total data size
```

Typical assumption:

```
100M users
~10k–50k QPS
~10–50TB data
```

Now you already know:

* Need **horizontal scaling**
* Need **sharding**
* Need **distributed storage**

All from **one question**.

---

# 2️⃣ Question: “Is the system read-heavy or write-heavy?”

This tells you:

* Cache strategy
* DB scaling
* Replication strategy

Example follow-up:

```
Is this mostly reads or writes?
```

Typical cases:

| System            | Ratio       |
| ----------------- | ----------- |
| Social media feed | 100:1       |
| Search            | 1000:1      |
| Chat              | 1:1         |
| Analytics         | write-heavy |

Immediate design implications:

| Pattern     | Architecture         |
| ----------- | -------------------- |
| Read heavy  | Cache + CDN          |
| Write heavy | Queue + partitioning |
| Balanced    | Scale DB + cache     |

---

# 3️⃣ Question: “Are there strict latency or consistency requirements?”

This gives you **two more critical things**:

* Latency expectation
* Consistency model

Example:

```
Do we need strong consistency or is eventual consistency acceptable?
What latency should we aim for?
```

Example answers:

| Requirement             | Architecture       |
| ----------------------- | ------------------ |
| <100ms                  | aggressive caching |
| real-time               | WebSockets         |
| eventual consistency OK | async processing   |
| strong consistency      | transactions       |

---

# Why This Works

These **3 questions indirectly capture the 5 key metrics**.

| Hidden Metric    | Derived From |
| ---------------- | ------------ |
| QPS              | scale        |
| Data size        | scale        |
| Peak traffic     | scale        |
| Read/write ratio | workload     |
| Latency          | requirement  |

So instead of asking **5 metrics**, you ask **3 smart questions**.

---

# The 30-Second Interview Script

You can say this at the start:

```
Before diving into the design, I'd like to clarify a few assumptions.

1. What scale should we design for?
   (users, QPS, data size)

2. Is the workload mostly read-heavy or write-heavy?

3. Are there strict latency or consistency requirements?
```

Then assume reasonable values if interviewer doesn't provide them.

Example:

```
Let's assume:

• ~50M daily users
• ~20k QPS average
• read-heavy workload (~100:1)
• ~200ms latency expectation
• design for 5x peak traffic
```

Done.

Move to architecture.

---

# Why Interviewers Like This

Because it shows:

* **Structured thinking**
* **Prioritization**
* **Understanding of system constraints**

Instead of wasting **10 minutes doing math**.

---

# One Bonus Question (Only if Relevant)

Sometimes add this **4th question** if system stores large objects:

```
What is the typical object size?
```

Examples:

| System  | Object     |
| ------- | ---------- |
| YouTube | video size |
| Gmail   | attachment |
| Dropbox | file size  |

This determines:

* bandwidth
* storage
* CDN usage

---

# Final Cheat Sheet

Use this in interviews:

```
Before we start designing, I want to clarify:

1. What is the scale? (users, QPS, data size)
2. Is the workload read-heavy or write-heavy?
3. Are there strict latency or consistency requirements?
```

Everything else follows from these.

---

If you want, I can also show you something **extremely powerful for interviews**:

**The 6 system design signals that immediately tell you what architecture to choose** (cache, queue, sharding, streaming, etc.).

Top candidates use these to **jump to the right architecture in seconds.**

---
This is one of the **most powerful shortcuts in System Design interviews**.
Senior engineers don’t randomly choose components like **Redis, Kafka, sharding, CDN**.

Instead they look for **signals in the problem statement**.

When you hear certain **keywords or patterns**, it should immediately trigger a **specific architecture decision**.

Think of them as **“architecture triggers.”**

---

# The 6 System Design Signals (Architecture Triggers)

```text
1. Read-heavy → Cache
2. Write spikes / async tasks → Queue
3. Large scale data → Sharding
4. Real-time updates → Streaming / WebSockets
5. Large files / static content → CDN + Object storage
6. High availability → Replication
```

These **6 signals cover ~90% of system design decisions**.

---

# 1️⃣ Signal: Read Heavy System → Use Cache

When you hear:

```text
Millions of users reading data
Timeline / feed / profile views
Search queries
Leaderboards
```

Immediate thought:

```text
Cache (Redis / Memcached)
```

Example systems:

| System              | Read Pattern         |
| ------------------- | -------------------- |
| Twitter timeline    | very read-heavy      |
| YouTube video views | extremely read-heavy |
| Product pages       | read-heavy           |

Architecture reaction:

```text
User → API → Cache → DB
```

Goal:

```text
Reduce DB load
Reduce latency
```

---

# 2️⃣ Signal: Write Spikes / Background Tasks → Queue

When you hear:

```text
Email sending
Notifications
Video processing
Image processing
Heavy computation
Retries needed
```

Immediate thought:

```text
Queue (Kafka / SQS / RabbitMQ)
```

Architecture:

```text
User → API → Queue → Workers
```

Example:

**Uber receipts**

```text
Ride completed → Queue → email worker
```

Why queue?

| Benefit               |
| --------------------- |
| Smooth traffic spikes |
| Retry on failure      |
| Async processing      |

---

# 3️⃣ Signal: Data Too Large → Sharding

When you hear:

```text
100M users
TBs of data
single DB can't handle load
```

Immediate thought:

```text
Database sharding
```

Example:

```text
user_id % N → choose DB shard
```

Architecture:

```text
App → Shard router → DB shards
```

Common shard keys:

| System       | Shard key       |
| ------------ | --------------- |
| Social media | user_id         |
| Ecommerce    | user_id         |
| Messaging    | conversation_id |

---

# 4️⃣ Signal: Real-Time Updates → Streaming / WebSockets

When you hear:

```text
Live updates
Real-time notifications
Chat
Location tracking
Live leaderboard
```

Immediate thought:

```text
WebSockets or Streaming
```

Architecture:

```text
Server → WebSocket → Client
```

Examples:

| System            | Real-time        |
| ----------------- | ---------------- |
| Chat              | instant messages |
| Uber              | driver location  |
| Stock trading     | price updates    |
| Multiplayer games | live events      |

Polling would be **too expensive**.

---

# 5️⃣ Signal: Large Files → CDN + Object Storage

When you hear:

```text
Images
Videos
Attachments
Files
Media uploads
```

Immediate thought:

```text
Object Storage + CDN
```

Architecture:

```text
Upload → Object storage (S3)
Download → CDN
```

Example:

| System    | Storage     |
| --------- | ----------- |
| YouTube   | videos      |
| Instagram | images      |
| Dropbox   | files       |
| Gmail     | attachments |

Benefits:

```text
cheap storage
global distribution
reduced latency
```

---

# 6️⃣ Signal: System Must Not Go Down → Replication

When you hear:

```text
High availability
Global system
Zero downtime
Fault tolerant
```

Immediate thought:

```text
Replication
```

Example architecture:

```text
Primary DB
    ↓
Read replicas
```

Benefits:

| Benefit           |
| ----------------- |
| failover          |
| read scaling      |
| disaster recovery |

---

# The Instant Architecture Map

This is the **mental shortcut**:

```text
Read heavy → Cache
Write spikes → Queue
Large data → Sharding
Real-time → WebSockets
Large files → CDN
High availability → Replication
```

These **six decisions appear in almost every system design interview**.

---

# Example: Interview Question

### Design Twitter

Signals detected:

```text
Timeline reads → Cache
Millions of users → Sharding
Tweets fanout → Queue
Images → CDN
Real-time notifications → WebSockets
```

You instantly know the architecture pieces.

---

# Example: Design Uber

Signals:

```text
Real-time driver location → Streaming
Write-heavy location updates → Queue
Millions of users → Sharding
High availability → Replication
```

---

# Example: Design YouTube

Signals:

```text
Huge videos → Object storage
Global users → CDN
Read-heavy → Cache
Video processing → Queue
```

---

# Why This Is Powerful in Interviews

Instead of saying:

> “Maybe we can use Redis…”

You say:

```text
Since this system is highly read-heavy,
we should introduce a distributed cache
like Redis to reduce database load.
```

This shows **clear reasoning**.

Interviewers love this.

---

# The Mental Model Senior Engineers Use

When reading a problem statement they immediately scan for:

```text
READ → cache
WRITE → queue
DATA → shard
REALTIME → streaming
FILES → CDN
AVAILABILITY → replication
```

This becomes **automatic with practice**.

---

# Ultra-Short Cheat Sheet (Memorize This)

```text
Read heavy → Cache
Write spikes → Queue
Large scale → Sharding
Real-time → WebSockets
Large files → CDN
High availability → Replication
```

These **6 signals appear in almost every design**.

---

If you'd like, I can also show you the **“Golden System Design Interview Flow”** that **Google and Meta candidates follow step-by-step** (it keeps you structured and prevents getting stuck). It’s extremely useful for your **Google L5 interview next week.**

---
