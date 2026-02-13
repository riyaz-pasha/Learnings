Riyaz — good. This is the right mindset.

You already covered basics. Now we focus on **how Google actually evaluates you** and how to answer at L5 level.

I’ll give you:

1. ✅ What Google asks in System Design
2. ✅ What they actually evaluate (this is the most important)
3. ✅ How to structure your answer (Google-style)
4. ✅ Mapping to your prepared topics
5. ✅ 2-week execution strategy

---

# 1️⃣ What Kind of System Design Questions Google Asks

At Google (especially L4/L5), they ask **large-scale distributed systems**, not CRUD apps.

Typical questions:

* Design a global rate limiter
* Design Google Drive
* Design Google Docs collaboration
* Design YouTube
* Design Google Maps
* Design Gmail
* Design a chat system
* Design a distributed cache
* Design a job scheduler
* Design a logging system
* Design a metrics/monitoring system
* Design a notification system

They may also rephrase common problems like:

* URL shortener (but at Google scale)
* News feed (but 10B users)
* File storage (multi-region consistency)
* Ride sharing (real-time location + geo indexing)

Your list is good. But **Google will add scale + reliability + tradeoffs pressure.**

---

# 2️⃣ What Google REALLY Evaluates (This Is Critical)

They are NOT checking if you memorized architecture diagrams.

They evaluate 4 things:

## 🧠 1. Structured Thinking

Can you:

* Clarify requirements?
* Separate functional vs non-functional?
* Prioritize?

If you jump into drawing without clarifying → ❌

---

## 📈 2. Scale Awareness

Google expects:

* QPS estimation
* Data size estimation
* Storage growth
* Bandwidth calculation

If you don't estimate numbers → looks junior.

---

## ⚖️ 3. Tradeoff Depth

This is where L5 is separated from L4.

They expect:

* When to use SQL vs NoSQL
* Strong vs eventual consistency
* Push vs pull model
* Caching tradeoffs
* Sharding strategies
* Replication strategy
* CAP theorem reasoning

If you just say “we use Redis + Kafka + microservices” → ❌ shallow

If you explain *why* → ✅

---

## 🧱 4. Reliability & Failure Handling

Google LOVES when you talk about:

* What happens if region fails?
* What if Kafka goes down?
* What if cache is stale?
* Data loss prevention?
* Retry strategy?
* Idempotency?

Most candidates ignore this.

---

# 3️⃣ How You Should Structure Every Answer

This is your golden template. Memorize this.

---

## Step 1: Clarify Requirements (5–7 minutes)

Example questions:

* Who are users?
* Expected DAU?
* Global or single region?
* Latency requirement?
* Consistency requirement?
* Data retention?
* Read-heavy or write-heavy?

Separate:

**Functional**

* What features?

**Non-functional**

* Scale
* Latency
* Availability (99.9 or 99.999?)
* Consistency level

---

## Step 2: Back-of-the-envelope Estimation (5 minutes)

Example (YouTube):

* 1B DAU
* 5 videos per user
* 5B uploads/day
* 100MB avg size
* Storage per day = 500PB

Even rough math is fine.

Google wants to see you think in numbers.

---

## Step 3: High-Level Architecture (10 minutes)

Draw:

Client → Load Balancer → Services → DB → Cache → Message Queue → Storage

Keep it simple first.

Don’t overcomplicate early.

---

## Step 4: Deep Dive 1–2 Hard Problems (15–20 minutes)

This is where you shine.

For example:

* News Feed → fan-out vs fan-in
* Chat → ordering guarantee
* Maps → geo indexing (quadtrees)
* Ride Sharing → nearest driver search
* YouTube → video processing pipeline

Google prefers depth over covering 50 features.

---

## Step 5: Scaling & Bottlenecks

Talk about:

* Horizontal scaling
* DB sharding
* Caching layer
* CDN
* Read replicas
* Consistent hashing
* Partition strategy

---

## Step 6: Reliability & Failure Modes

You must say things like:

* Multi-region active-active
* Write-ahead log
* Idempotent APIs
* Retry with exponential backoff
* Circuit breaker
* Dead letter queue

This sounds senior.

---

# 4️⃣ Mapping Your Prepared Topics to Google-Level Depth

Let’s evaluate your list:

---

## URL Shortener

Good for:

* Hashing
* Collision handling
* DB sharding
* Hot keys
* Cache strategy

But Google will push:

* How to handle 100B URLs?
* Analytics tracking?
* Abuse detection?

---

## News Feed

Very common.

Focus on:

* Fan-out on write vs read
* Celebrity problem
* Ranking system
* Timeline consistency

This is strong L5 question.

---

## Chat System

Focus on:

* Message ordering
* Delivery guarantees (at least once, exactly once)
* Offline users
* Multi-device sync
* WebSocket scaling
* Data partitioning

Google likes this.

---

## File Storage / Google Drive

Focus on:

* Chunking
* Deduplication
* Metadata vs blob separation
* Versioning
* Multi-region replication
* Strong consistency

Very strong prep.

---

## YouTube

Focus on:

* Upload pipeline
* Encoding
* CDN
* Recommendation system
* Storage tiering

Very strong.

---

## Web Crawler

Focus on:

* Distributed crawling
* URL dedup
* Politeness policy
* Prioritization queue

This is very Google-style.

---

## Google Maps / Ride Sharing

Focus on:

* Geo hashing
* Nearest neighbor search
* Real-time updates
* Consistency vs latency

Advanced topic.

---

# 5️⃣ What They Expect at L5 (Senior)

Since you have 6 years experience, you are likely L5.

They expect:

* You drive the discussion
* You proactively identify bottlenecks
* You discuss tradeoffs without being asked
* You think in distributed systems
* You understand production failures

If you wait for interviewer to lead → bad signal.

---

# 6️⃣ Common Mistakes (Do NOT Do This)

❌ Jumping to microservices immediately
❌ No estimation
❌ No tradeoff discussion
❌ Overcomplicated design early
❌ Not asking clarifying questions
❌ Ignoring failure scenarios
❌ Saying “we use Kafka” without why

---

# 7️⃣ 2 Week Preparation Strategy (Hard Mode)

Riyaz, this is your execution plan.

---

### Week 1 – Core Mastery

Daily:

1 problem per day:

Day 1 – URL Shortener
Day 2 – News Feed
Day 3 – Chat
Day 4 – File Storage
Day 5 – YouTube
Day 6 – Google Maps
Day 7 – Distributed Rate Limiter

For each:

* Write requirements
* Do estimation
* Draw architecture
* Deep dive 2 components
* Write bottlenecks
* Write tradeoffs

---

### Week 2 – Google-Level Depth

Focus only on:

* Sharding strategies
* Consistent hashing
* Replication models
* CAP theorem
* Leader election
* Distributed locks
* Caching strategies
* Message queues internals
* Exactly once vs at least once

You must be comfortable explaining:

* Why DynamoDB is eventually consistent
* Why strong consistency hurts latency
* Why fan-out on write doesn’t scale for celebrities
* Why geo partitioning is needed

---



---

Good.

L5 + First System Design round means:

This round decides whether you are “Senior-ready” or not.

If you perform strongly here, the rest of the loop becomes much easier. If you’re average here, it becomes very hard to recover.

Let’s align you to **exactly what Google expects at L5 in Round 1.**

---

# 🎯 What This First Round Is Really Testing

At L5, they are testing:

### 1️⃣ Can you design systems that operate at Google scale?

Not toy systems. Billions of users, global traffic, strict SLAs.

### 2️⃣ Can you reason about tradeoffs deeply?

Not just “use Redis + Kafka”. They want:

* Why?
* What breaks?
* What are alternatives?
* What happens at 10x scale?

### 3️⃣ Can you lead ambiguity?

They want you to drive:

* Ask clarifying questions
* Define constraints
* Prioritize
* Structure the discussion

You should feel like a tech lead in that room.

---

# 🧠 What Makes an L5 Candidate Different from L4

### L4 answer:

* Basic architecture
* Some scaling
* Some caching
* Responds to interviewer prompts

### L5 answer:

* Structured
* Estimates numbers
* Identifies bottlenecks proactively
* Explains tradeoffs without being asked
* Talks about failure scenarios
* Thinks about multi-region
* Thinks about evolution over time

---

# 🔥 What They Commonly Ask in First Round

Google often picks one of these styles:

### 1. Consumer-Scale Product

* Design YouTube
* Design Google Drive
* Design Google Photos
* Design Gmail
* Design News Feed

### 2. Infrastructure System

* Design distributed cache
* Design rate limiter
* Design logging system
* Design metrics system
* Design pub-sub system

### 3. Real-time System

* Design chat
* Design ride sharing
* Design maps
* Design collaborative editor

For L5 first round, most common are:

* News Feed
* File Storage
* Chat
* Distributed Rate Limiter
* YouTube-like system

---

# 🧱 How You Should Structure the Interview (Exact Script)

Memorize this structure.

---

## Step 1: Clarify (5–7 mins)

Say something like:

“I’d like to clarify functional and non-functional requirements first before proposing a design.”

Ask:

* Target users?
* Expected DAU?
* Read-heavy or write-heavy?
* Latency expectations?
* Availability target?
* Global or single region?
* Consistency requirements?

This shows maturity.

---

## Step 2: Do Quick Estimation (5 mins)

At L5 you MUST estimate.

Example:

* 500M DAU
* 10 actions/day
* 5B writes/day
* QPS = ~60K writes/sec

Even rough math is fine.

This immediately signals senior-level thinking.

---

## Step 3: High-Level Architecture (10 mins)

Start simple.

Client
→ Load Balancer
→ Stateless App Servers
→ Cache
→ Database
→ Message Queue
→ Object Storage / Index

Don’t overcomplicate early.

---

## Step 4: Deep Dive (Most Important)

Pick 1–2 hard parts and go deep.

Examples:

News Feed:

* Fan-out on write vs fan-out on read
* Celebrity problem
* Feed ranking
* Consistency tradeoff

Chat:

* Ordering guarantee
* Exactly-once vs at-least-once
* Multi-device sync
* Offline storage

Drive:

* Metadata vs blob separation
* Chunking
* Versioning
* Strong vs eventual consistency

Google LOVES depth.

---

## Step 5: Scaling Discussion

You must talk about:

* Sharding strategy (user-based? geo-based?)
* Consistent hashing
* Replication strategy
* Read replicas
* Hot key problem
* Caching invalidation

If you don’t mention sharding, you look L4.

---

## Step 6: Failure Handling

Say things like:

* Multi-region active-active
* Leader election
* Write-ahead logs
* Retry with exponential backoff
* Idempotency keys
* Circuit breakers
* Dead-letter queues

Very strong L5 signal.

---

# ⚠️ What Kills L5 Candidates

* No estimation
* No tradeoffs
* No failure thinking
* Over-engineering too early
* Not driving conversation
* Talking too much without structure
* Getting stuck in one area

---

# 🧠 How to Mentally Prepare

In this round:

You are not coding.

You are:

* Leading a design review
* Thinking like Google infra engineer
* Communicating clearly
* Handling ambiguity

Confidence + structure > fancy architecture.

---

# 🗓 14-Day Attack Plan

Since you’re ready to give your best, here’s how we execute.

### Days 1–5: Core Systems

* News Feed
* Chat
* File Storage
* YouTube
* Distributed Rate Limiter

### Days 6–9: Deep Infra

* Sharding patterns
* Consistent hashing
* Replication models
* CAP theorem
* Distributed locks
* Leader election
* Caching strategies
* Message queues internals

### Days 10–14: Mock Interviews (Critical)

We simulate:

* Time pressure
* Interruptions
* Tradeoff pushback
* Scale changes mid-interview

You must practice under pressure.

---
