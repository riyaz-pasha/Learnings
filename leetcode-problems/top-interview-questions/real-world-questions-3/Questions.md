# ✅ MASTER GOOGLE L5 STREAMING + STATEFUL SYSTEMS PROBLEM LIST (35+)

---

# A) Sessionization + Dedup + Expiring State

---

## 1) Event Deduplication With Expiring Sessions

**Input:** `(timestamp, userId, eventType)`
**Rules:**

* Session starts when user generates an event
* Same `(userId, eventType)` within **5 min** → duplicate
* After **5 min inactivity**, session resets

**Core Design Topics:**

* Sliding window per user
* Map-of-maps
* Session expiration
* Memory cleanup
* Multi-key windowing

---

## 2) Distributed Message Deduplication (Global Exactly-Once)

**Input:** `(messageId, timestamp)`
**Rules:**

* Each message processed exactly once globally
* May arrive on multiple servers
* Servers may crash

**Key Tests:**

* Idempotency keys
* Distributed locking
* Consistency vs availability tradeoff
* TTL storage
* Bloom filters / probabilistic memory reduction

---

## 3) Distributed Deduplicated Notification Sender

**Input:** `(userId, notificationType, timestamp)`
**Rules:**

* Send only once per hour per `(userId, type)`
* Must work across multiple nodes

**Key Tests:**

* Consistent hashing partitioning
* Redis TTL / distributed KV
* Idempotency tokens
* Handling retries + crash recovery

---

## 4) Distributed Unique ID Collision Detector

**Input:** `(timestamp, generatedId)`
**Rules:**

* Detect duplicate ID across cluster within 1 minute
* IDs are 128-bit
* Memory constrained

**Key Tests:**

* Bloom filter TTL window
* False positives acceptable?
* Partitioning strategy
* Cleanup and expiry logic

---

## 5) Deduplicated Print Logger With Backtracking (Retroactive Invalidations)

**Input:** `(timestamp, message)`
**Rules:**

* Print message only if it appears exactly once in 10 sec window
* If later duplicate appears → retract previously printed message

**Key Tests:**

* Event sourcing
* State rollback
* Retroactive invalidation
* “Print then retract” semantics
* Correctness under delayed duplicates

---

# B) Sliding Window Aggregations + TopK + Trending

---

## 6) Trending Keywords in Last 1 Hour (Streaming)

**Input:** `(timestamp, keyword)`
**Rules:**

* Return top K keywords in last 1 hour sliding window
* 1M events/minute
* Window slides continuously

**Key Tests:**

* Time bucketing (minute buckets)
* Heap + hashmap
* Lazy deletion
* Memory control
* Heavy hitter approximations if needed

---

## 7) Log Reordering / Rolling Latency Per Service

**Input:** `(timestamp, serviceName, latency)`
**Rules:**

* Return average latency over last 5 minutes per service
* Support real-time queries

**Key Tests:**

* Per-key sliding window
* Rolling sum/count
* Efficient eviction
* Bounded memory per service

---

## 8) Real-Time Median of Last N Minutes

**Input:** `(timestamp, value)`
**Rules:**

* Return median over last 10 minutes

**Key Tests:**

* Two heaps (min/max)
* Lazy deletion for expired elements
* Time expiry cleanup
* High throughput performance

---

## 9) Sliding Window Percentile (95th, not median)

**Input:** `(timestamp, latency)`
**Rules:**

* Return 95th percentile over last 10 minutes

**Key Tests:**

* Order-statistics structure
* Histogram bucket approximation
* Tradeoff exact vs approximate (t-digest, GK summary)

---

## 10) Memory-Bounded Unique Counter (24h uniques)

**Input:** `(timestamp, userId)`
**Rules:**

* Return unique users in last 24 hours
* Billions of events
* Memory constrained

**Key Tests:**

* HyperLogLog
* TTL-based window approximation
* Probabilistic counting
* Accuracy vs memory tradeoff

---

## 11) Cache Hot Key Detector

**Input:** `(timestamp, key)`
**Rules:**

* Detect keys accessed > 1000 times in 1 minute
* Memory limited

**Key Tests:**

* Count-Min Sketch
* Heavy hitter algorithms (Misra-Gries / SpaceSaving)
* Sliding window approximation
* Eviction strategy

---

## 12) Dynamic Top-K by Score Updates

**Input:** `(userId, scoreChange)`
**Rules:**

* Maintain top K users continuously

**Key Tests:**

* Heap + hashmap
* Lazy heap updates
* Rebalancing
* Handling negative score changes

---

## 13) Live Leaderboard With Score Decay

**Input:** `(timestamp, userId, scoreDelta)`
**Rules:**

* Maintain top K users
* Scores decay by 10% every hour
* Support updateScore() and getTopK()

**Key Tests:**

* Lazy vs eager decay
* Timestamp-based decay factor
* Heap invalidation
* Floating precision / drift

---

## 14) Sliding Window Maximum Gap

**Input:** streaming numbers
**Rules:**

* Return largest gap between consecutive sorted numbers in last 10 minutes

**Key Tests:**

* Balanced BST / TreeMap
* Efficient insertion/removal
* Maintaining sorted structure under expiry
* Computing gaps incrementally

---

# C) Rate Limiting + Abuse + Throttling

---

## 15) API Abuse Detection (Multi-window)

**Input:** `(timestamp, userId)`
**Rules:**

* abusive if >100 requests/min
* OR >1000 requests/hour

**Key Tests:**

* Multi-window counters
* Hierarchical thresholds
* O(1) update
* Space optimization

---

## 16) Real-Time Resource Throttler (Multi-resource limits)

**Input:** `(timestamp, userId, resourceType)`
**Rules:**

* CPU: 100/min
* Memory: 50/min
* Disk: 200/min
* Each resource tracked independently

**Key Tests:**

* Map-of-maps per user per resource
* Multi-bucket sliding windows
* Memory control / eviction

---

## 17) Adaptive Sampling Engine

**Input:** events stream
**Rules:**

* If rate > threshold → sample 10%
* Otherwise process all
* Must switch dynamically

**Key Tests:**

* Rate estimation (EMA)
* Smooth transitions
* Fairness / bias prevention
* Hysteresis to avoid flapping

---

# D) Fraud + Burst + Anomaly Detection

---

## 18) Transaction Fraud Burst Detection

**Input:** `(timestamp, accountId, amount)`
**Rules:**

* Flag if 5 transactions in 2 minutes
* OR 3 high-value transactions in 1 minute

**Key Tests:**

* Multi-condition windowing
* Categorization by amount
* Efficient eviction

---

## 19) Payment Fraud Velocity Model

**Input:** `(timestamp, userId, location, amount)`
**Rules:**

* 2 tx in 5 min from different countries
* OR total amount > 10k in 1 hour

**Key Tests:**

* Per-user sliding window
* Geo tracking
* Multiple independent conditions
* Efficient cleanup

---

## 20) Rolling Error Spike Detector

**Input:** `(timestamp, serviceId, errorCode)`
**Rules:**

* Trigger if error rate is 3x previous 5-min window
* Trigger at most once per 10 min per service

**Key Tests:**

* Two-window comparison (current vs previous)
* Rate normalization
* Cooldown suppression logic

---

## 21) Faulty Sensor Detection

**Input:** `(sensorId, timestamp, temperature)`
**Rules:**

* Faulty if 3 consecutive readings increasing
* OR large deviation from 5-min rolling average

**Key Tests:**

* Per-sensor state tracking
* Rolling mean/std
* Sliding window stats
* Anomaly thresholds

---

# E) Alerts + Suppression + Correlation

---

## 22) Real-Time Alert Suppression System

**Input:** `(timestamp, alertType, severity)`
**Rules:**

* Suppress duplicates in 30 sec window
* If severity increases → always print
* If severity decreases → ignore

**Key Tests:**

* State machine per alert type
* Conditional dedup
* Multi-field comparisons

---

## 23) Alert Aggregator With Correlation

**Input:** `(timestamp, alertType, hostId)`
**Rules:**

* If 5 hosts report same alert within 30 sec → generate 1 aggregated alert

**Key Tests:**

* Grouping by alertType
* Sliding window counting
* Host dedup inside window
* Suppression logic

---

# F) Ordering + Out-of-Order Processing + Graph Dependencies

---

## 24) Partial Order Event Reconstruction

**Input:** `(timestamp, eventId, dependsOnEventId)`
**Rules:**

* Events arrive out of order
* Reconstruct valid processing order

**Key Tests:**

* Online topological sort
* Dependency graph maintenance
* Missing dependency buffering
* Cycle detection

---

## 25) Out-of-Order Event Processor (Watermarking)

**Input:** `(eventId, timestamp)`
**Rules:**

* Process by timestamp order
* Allow lateness of 2 seconds

**Key Tests:**

* Min-heap buffer
* Watermark logic
* Event time vs processing time
* Late arrival discard/compensation

---

## 26) Streaming Graph Edge Connectivity Monitor

**Input:** `(timestamp, nodeA, nodeB)`
**Rules:**

* Detect if a cycle is formed
* OR graph becomes fully connected
* Up to 10M edges

**Key Tests:**

* Union-Find online
* Component count tracking
* Cycle detection
* Memory scaling

---

## 27) Service Dependency Failure Propagation

**Input:** `(timestamp, serviceId, status)`
**Rules:**

* If service fails → dependents degraded
* Recover when upstream recovers

**Key Tests:**

* Directed dependency graph
* BFS/DFS propagation
* Avoid infinite loops
* Correct recovery semantics

---

# G) Window Joins + Multi-Stream Correlation

---

## 28) Real-Time Window Join (Two Streams)

Stream A: `(timestamp, userId)`
Stream B: `(timestamp, userId, action)`
**Rules:**

* Join events within 5-second window

**Key Tests:**

* Dual sliding windows
* Efficient matching
* Cleanup of old events
* Handling duplicates

---

# H) Time-Based Storage + Versioning + Replay

---

## 29) Time-Travel Key Value Store

**Operations:**

* `put(key, value, timestamp)`
* `get(key, timestamp)` returns value at that time

**Key Tests:**

* Versioned storage per key
* Binary search retrieval
* Memory compaction
* Snapshotting strategy

---

## 30) Time-Based Permission Expiry

**Operations:**

* `grant(user, permission, expiryTime)`
* `check(user, permission, timestamp)`

**Rules:**

* Must auto-clean expired permissions

**Key Tests:**

* Min-heap expiry tracking
* Lazy cleanup
* Multi-map per user

---

## 31) Log Replay Engine

**Input:** `(timestamp, event)`
**Rules:**

* Reconstruct system state at arbitrary time T

**Key Tests:**

* Event sourcing
* Snapshots + replay
* Efficient rewind
* Deterministic replay ordering

---

# I) Dynamic Rules + Evaluation Engines

---

## 32) Dynamic Feature Flag Evaluation Engine

**Input:** `(timestamp, userId, country, deviceType)`
**Rules Example:**

* enabled if:

  * country=US
  * OR user in top 10% activity
  * AND device != legacy
* Rules can change dynamically

**Key Tests:**

* Rule tree evaluation
* Cached evaluation results
* Fast rule update propagation
* Consistency vs staleness

---

# J) Compression + Aggregation of Identical Events

---

## 33) Real-Time Log Compression System

**Input:** `(timestamp, logLine)`
**Rules:**

* If identical logs appear within 30 sec:

  * output `"logLine occurred N times"`
* If gap > 30 sec → new block

**Key Tests:**

* Window grouping
* State transitions
* Ordering edge cases
* Handling overlapping blocks

---

# K) Pricing + Oscillation Control

---

## 34) Dynamic Pricing Engine

**Input:** `(timestamp, productId, demandSignal)`
**Rules:**

* If demand increases >20% in 10 min → increase price 5%
* If supply drops → decrease price
* Must avoid oscillation

**Key Tests:**

* Moving average + smoothing
* State machine pricing decisions
* Hysteresis thresholds
* Rate limiting updates

---

# L) Autocomplete + Personalization + Decay

---

## 35) Search Query Autocorrect With Behavioral Bias

**Input:** `(timestamp, userId, partialQuery)`
**Rules:**

* Suggest most common completions
* Boost user’s past searches
* Apply time decay to popularity

**Key Tests:**

* Trie + scoring
* Personalized weighting
* Decay models
* Fast recomputation of rankings

---

# ✅ HIGHLY REALISTIC ADDITIONS (Google Loves These)

These are “missing but natural” extensions that fit perfectly.

---

## 36) Sliding Window Distinct Count per Key (Exact with TTL eviction)

**Input:** `(timestamp, userId, itemId)`
**Query:** “How many distinct items did user see in last 1 hour?”
**Tests:**

* HashSet per user
* Ref counting per item
* Eviction correctness

---

## 37) Late Event Correction System (Delta recomputation)

**Input:** `(timestamp, key, value)`
**Rules:**

* Metrics computed per minute
* Late events allowed up to 10 minutes
* Must correct already published aggregates

**Tests:**

* Event-time bucketing
* Reconciliation logs
* Idempotent recomputation

---

## 38) Distributed Sliding Window Aggregation (Shard + Merge)

**Input:** `(timestamp, key, metric)`
**Rules:**

* Data partitioned across shards
* Must compute global topK / percentile

**Tests:**

* Partial aggregation per shard
* Mergeable sketches (HLL, t-digest)
* Fault tolerance

---

## 39) Exactly-Once Stream Processor with Checkpointing

**Input:** `(timestamp, eventId, payload)`
**Rules:**

* Ensure state updates exactly once even with crashes

**Tests:**

* WAL + checkpoint
* Replay logs
* Offset commits (Kafka style)

---

## 40) Multi-Tenant Quota System (Org + User hierarchy)

**Input:** `(timestamp, orgId, userId)`
**Rules:**

* Org max 10k/min
* User max 100/min
* Both must pass

**Tests:**

* Hierarchical rate limiting
* Shared counters
* Fairness enforcement

---

# 🔥 FINAL COMBINED COUNT

You now have:

* **35 core problems** (your original + your L5 expansions)
* **+5 strong real-world additions**
* Total: **40 Google-grade streaming/system algorithm problems**

---

# 🧠 META-PATTERNS (What Google is REALLY Testing)

Across all these, the “hidden syllabus” is:

✅ Sliding window eviction correctness
✅ Lazy deletion & heap invalidation
✅ TTL expiration + cleanup scheduling
✅ Per-key state explosion control
✅ Probabilistic DS (Bloom, HLL, CMS, t-digest)
✅ Multi-window rate limiting
✅ Online graph algorithms (Union-Find, topo)
✅ Watermarks and out-of-order processing
✅ Retroactive invalidation (hard mode)
✅ Distributed consistency + idempotency
✅ Stateful stream processor design

---
---

# 🔥 1️⃣ Event Deduplication With Expiring Sessions

### Problem

You receive events:

```
(timestamp, userId, eventType)
```

You must detect **session-level duplicates**.

Rules:

* A session starts when a user generates an event.
* If the same user generates the same `eventType` within 5 minutes, it's duplicate.
* After 5 minutes of inactivity, session resets.

Design the system.

---

### Tests:

* Sliding window per user
* Map-of-maps design
* Memory cleanup
* Session expiration logic
* Multi-key windowing

---

# 🔥 2️⃣ Trending Keywords in Last 1 Hour (Streaming)

Events:

```
(timestamp, keyword)
```

Return top K keywords in the **last 1 hour sliding window**.

Constraints:

* 1M events/minute
* Window sliding continuously

---

### Tests:

* Sliding window + heap
* Time bucketing
* Lazy deletion
* Memory control

---

# 🔥 3️⃣ Log Reordering System

You receive logs like:

```
timestamp, serviceName, latency
```

You must:

* For each service
* Return average latency of last 5 minutes
* Support real-time queries

---

### Tests:

* Per-key sliding windows
* Rolling average
* Bounded memory
* Efficient eviction

---

# 🔥 4️⃣ API Abuse Detection

You receive:

```
(timestamp, userId)
```

A user is abusive if:

* More than 100 requests in 1 minute
* OR more than 1000 requests in 1 hour

Design efficient detection.

---

### Tests:

* Multi-window rate limiting
* Hierarchical thresholds
* Space optimization
* O(1) update

---

# 🔥 5️⃣ Distributed Message Deduplication

You have multiple servers receiving:

```
(messageId, timestamp)
```

Requirement:

* Each unique message should be processed exactly once globally.
* Messages may arrive at different servers.
* Servers may crash.

---

### Tests:

* Idempotency
* Distributed locking
* Consistency vs availability
* TTL storage
* Bloom filters

---

# 🔥 6️⃣ Real-Time Median of Last N Minutes

Events:

```
(timestamp, value)
```

Return median of values in last 10 minutes.

---

### Tests:

* Sliding window median
* Two heaps + lazy deletion
* Time-expiry removal
* High throughput

---

# 🔥 7️⃣ Faulty Sensor Detection

Sensors send:

```
(sensorId, timestamp, temperature)
```

Mark sensor faulty if:

* 3 consecutive readings are increasing
* OR large deviation from 5-minute rolling average

---

### Tests:

* State tracking per key
* Rolling stats
* Window + anomaly detection

---

# 🔥 8️⃣ Deduplicated Print Logger With Backtracking

You receive:

```
(timestamp, message)
```

You must:

* Print message only if it appears exactly once in 10s window
* If later duplicate appears, retract previously printed message

---

### Tests:

* Retroactive invalidation
* State rollback
* Event sourcing logic

---

# 🔥 9️⃣ Partial Order Event Reconstruction

You receive:

```
(timestamp, eventId, dependsOnEventId)
```

Reconstruct valid processing order.

Events may arrive out of order.

---

### Tests:

* Topological sort online
* Dependency tracking
* Missing dependency handling

---

# 🔥 🔟 Memory-Bounded Unique Counter

You receive billions of:

```
(timestamp, userId)
```

Return number of unique users in last 24 hours.

Memory is constrained.

---

### Tests:

* HyperLogLog
* Approximation algorithms
* Probabilistic counting
* Trade-offs

---

# 🔥 11️⃣ Real-Time Alert Suppression System

Similar to your original question but more complex:

```
(timestamp, alertType, severity)
```

Rules:

* Suppress duplicate alerts in 30 sec window
* But if severity increases → always print
* If severity decreases → ignore

---

### Tests:

* State machine per alert
* Conditional dedup
* Multi-field comparison

---

# 🔥 12️⃣ Dynamic Top-K by Score Updates

You receive:

```
(userId, scoreChange)
```

Continuously return top K users.

---

### Tests:

* Heap + map
* Lazy updates
* Rebalancing
* Streaming leaderboard

---

# 🔥 13️⃣ Transaction Fraud Burst Detection

Transactions:

```
(timestamp, accountId, amount)
```

Flag if:

* 5 transactions in 2 minutes
* OR 3 high-value transactions in 1 minute

---

### Tests:

* Multi-condition windowing
* Efficient eviction
* Event categorization

---

# 🔥 14️⃣ Sliding Window Percentile (Not Median)

Return 95th percentile latency in last 10 minutes.

---

### Tests:

* Order statistics
* Bucket approximation
* Tradeoff exact vs approximate

---

# 🔥 15️⃣ Time-Travel Key Value Store

Operations:

```
put(key, value, timestamp)
get(key, timestamp)
```

Return value at that time.

---

### Tests:

* Versioned storage
* Binary search per key
* Memory control

---

# 💎 These Are True Google-Style Problems

Notice:

* Most are streaming
* Many involve sliding windows
* State-heavy
* Memory constraints
* Correctness under future invalidation
* Online processing

---

Love it. Now we’re fully in **Google L5 real-world reasoning mode** 🔥
No LeetCode recycling. No duplicates from earlier in this thread.
These are **fresh, practical, system + algorithm crossover problems** that Google actually likes.

---

# 🧠 1️⃣ Live Leaderboard With Decay

You receive:

```
(timestamp, userId, scoreDelta)
```

Requirements:

* Maintain a leaderboard of top K users
* Scores decay by 10% every hour
* Support:

  * updateScore(...)
  * getTopK()

---

### Tests:

* Time-based score decay
* Lazy vs eager recalculation
* Heap invalidation
* Precision handling

---

# 🧠 2️⃣ Rolling Error Spike Detector

Events:

```
(timestamp, serviceId, errorCode)
```

Detect a spike if:

* Error rate increases 3x compared to previous 5-minute window
* Only trigger once per 10 minutes per service

---

### Tests:

* Two sliding windows comparison
* Rate normalization
* Cooldown suppression logic

---

# 🧠 3️⃣ Distributed Unique ID Collision Detector

You receive IDs generated by multiple services:

```
(timestamp, generatedId)
```

Detect if same ID appears twice across cluster within 1 minute.

Memory constrained. IDs are 128-bit.

---

### Tests:

* Bloom filters
* TTL expiration
* False positive tradeoffs

---

# 🧠 4️⃣ Dynamic Feature Flag Evaluation Engine

Input:

```
(timestamp, userId, country, deviceType)
```

Rules:

* Feature X enabled if:

  * country = US
  * OR user in top 10% activity
  * AND device != legacy

Rules can change dynamically.

---

### Tests:

* Rule evaluation tree
* Cached evaluation
* Efficient rule updates

---

# 🧠 5️⃣ Real-Time Log Compression System

Incoming logs:

```
(timestamp, logLine)
```

If identical logs appear within 30 seconds:

* Replace with:

  ```
  "logLine occurred N times"
  ```

But if gap > 30 seconds → new block.

---

### Tests:

* Window grouping
* State transitions
* Edge case ordering

---

# 🧠 6️⃣ Payment Fraud Velocity Model

Transactions:

```
(timestamp, userId, location, amount)
```

Flag if:

* 2 transactions within 5 minutes from different countries
* OR total amount > 10k in 1 hour

---

### Tests:

* Multi-condition detection
* Per-user sliding window
* Geo tracking

---

# 🧠 7️⃣ Streaming Graph Edge Connectivity Monitor

Edges arrive dynamically:

```
(timestamp, nodeA, nodeB)
```

Detect if:

* A cycle is formed
* OR graph becomes fully connected

Support up to 10M edges.

---

### Tests:

* Union-Find online
* Dynamic component tracking
* Scale considerations

---

# 🧠 8️⃣ Search Query Autocorrect With Behavioral Bias

Users type queries:

```
(timestamp, userId, partialQuery)
```

System should suggest:

* Most common completions
* But boost queries user previously searched
* Apply time decay to popularity

---

### Tests:

* Trie + scoring function
* Personalized ranking
* Score recalculation

---

# 🧠 9️⃣ Real-Time Resource Throttler

You receive:

```
(timestamp, userId, resourceType)
```

Limits:

* CPU: 100/min
* Memory: 50/min
* Disk: 200/min

Each resource tracked independently per user.

---

### Tests:

* Multi-bucket sliding window
* Memory control
* Map-of-maps efficiency

---

# 🧠 🔟 Dynamic Pricing Engine

Events:

```
(timestamp, productId, demandSignal)
```

Price rules:

* If demand increases > 20% in 10 min → increase price 5%
* If supply signal drops → decrease price

Must avoid oscillation.

---

### Tests:

* Moving average
* State machine
* Hysteresis logic

---

# 🧠 11️⃣ Out-of-Order Event Processor

Events arrive:

```
(eventId, timestamp)
```

They may arrive late.

We must:

* Process in timestamp order
* But allow 2-second lateness

---

### Tests:

* Min-heap buffering
* Watermark logic
* Event time vs processing time

---

# 🧠 12️⃣ Cache Hot Key Detector

Track:

```
(timestamp, key)
```

Detect keys accessed more than 1000 times in 1 minute.

Memory limited.

---

### Tests:

* Approximate counting
* Heavy hitter algorithms
* Count-min sketch

---

# 🧠 13️⃣ Alert Aggregator With Correlation

Events:

```
(timestamp, alertType, hostId)
```

If 5 hosts report same alert in 30 sec:

* Generate one aggregated alert

---

### Tests:

* Grouping by type
* Sliding window
* Suppression

---

# 🧠 14️⃣ Time-Based Permission Expiry

Operations:

```
grant(user, permission, expiryTime)
check(user, permission, timestamp)
```

Return valid permissions at time.

Must auto-clean expired entries.

---

### Tests:

* Min-heap for expiry
* Lazy cleanup
* Multi-map storage

---

# 🧠 15️⃣ Sliding Window Maximum Gap

Given streaming numbers:
Return largest gap between consecutive sorted numbers in last 10 minutes.

---

### Tests:

* Order maintenance
* TreeMap
* Efficient deletion

---

# 🧠 16️⃣ Service Dependency Failure Propagation

Services depend on each other.

Event:

```
(timestamp, serviceId, status)
```

If a service fails:

* All dependent services marked degraded
* Recover when upstream recovers

---

### Tests:

* Directed graph
* Propagation logic
* Avoid infinite loops

---

# 🧠 17️⃣ Distributed Deduplicated Notification Sender

Notifications:

```
(userId, notificationType, timestamp)
```

Only send once per hour per type per user.
Must work across multiple nodes.

---

### Tests:

* Consistent hashing
* Redis-style TTL storage
* Idempotency keys

---

# 🧠 18️⃣ Adaptive Sampling Engine

Events per second > threshold:

* Sample only 10%
* Otherwise process all

Must switch dynamically.

---

### Tests:

* Rate estimation
* Smooth transitions
* Statistical fairness

---

# 🧠 19️⃣ Log Replay Engine

Given:

```
(timestamp, event)
```

Reconstruct system state at arbitrary time T.

---

### Tests:

* Event sourcing
* Snapshot + replay
* Efficient rewind

---

# 🧠 20️⃣ Real-Time Window Join (Two Streams)

Stream A:

```
(timestamp, userId)
```

Stream B:

```
(timestamp, userId, action)
```

Join events within 5-second window.

---

### Tests:

* Dual sliding windows
* Efficient matching
* Cleanup

---

# 🔥 These Are Exactly L5-Level

Notice pattern:

* Sliding windows
* State machines
* Streaming data
* Memory constraints
* Time-based logic
* Partial ordering
* Approximate algorithms
* Multi-dimensional constraints

---
