# HLD System Design Interview Master Guide: Part B
### Advanced Topics: Distributed Systems Depth, Production Engineering, and Extra Problems

**Part A** taught you the 15 pattern families, trade-offs, and the interview framework.
**Part B** fills the gaps that decide senior-level interviews:

- *How does it really work underneath?* (database internals, consistency, consensus, clocks)
- *How do I build the infrastructure itself?* (key-value store, Kafka, cache, matching engine)
- *How does it behave in production?* (overload, security, observability, migrations, multi-region)
- *What other problems and trade-offs will I face?* (recommendations, metering, AI systems, more designs)

> Part A answers "what do I build". Part B answers "why is it correct, what breaks, and how does it evolve".

---

## Table of Contents

**Group 1: Correctness and distributed systems depth**
1. Database internals behind your trade-offs
2. Consistency models, replication, and consensus
3. Time and clocks
4. Event sourcing and CQRS

**Group 2: Infrastructure design problems**
5. Distributed key-value store (Dynamo-style)
6. Distributed cache
7. Message queue / Kafka-like log
8. Metrics monitoring and alerting platform
9. API gateway and service discovery
10. Distributed lock and coordination service
11. Low-latency single-writer systems (matching engine, auction)

**Group 3: Missing pattern families**
12. Recommendation, ranking, and graph systems
13. Usage metering, quotas, and billing
14. AI-era systems (LLM gateway, RAG, agent platform)

**Group 4: Production engineering**
15. Reliability: SLOs, overload, tail latency
16. Security, identity, and authorization
17. Observability and incident response
18. Migrations, re-sharding, and deployment safety
19. Multi-region architecture and disaster recovery
20. API design and network fundamentals
21. Data modeling and partition key design
22. Privacy, cost, and testing

**Group 5: Practice material**
23. More problems as design cards
24. New decision tables (simplest option, what breaks, when to switch)
25. Production-readiness checklist
26. Exercise template and updated study order
27. Updated problem-to-pattern additions
28. Part B revision sheet

---

# GROUP 1: Correctness and distributed systems depth

---

## 1. Database internals behind your trade-offs

In Part A we said "use a transaction" and "use a lock". Interviewers follow up with: *"Which isolation level? What anomaly does it prevent? What still goes wrong?"* Be ready.

### 1.1 Isolation levels and anomalies

| Anomaly | What happens |
|---|---|
| **Dirty read** | You read data another transaction has not committed |
| **Non-repeatable read** | You read a row twice and get different values (someone committed in between) |
| **Phantom read** | You run the same range query twice and new rows appear |
| **Lost update** | Two transactions read, both modify, last write overwrites the first |
| **Write skew** | Two transactions read overlapping data, write *different* rows, and together break an invariant |

| Level | Dirty read | Non-repeatable read | Phantom | Write skew |
|---|---|---|---|---|
| Read Uncommitted | Possible | Possible | Possible | Possible |
| Read Committed | No | Possible | Possible | Possible |
| Repeatable Read (snapshot) | No | No | Depends on DB* | **Possible** |
| Serializable | No | No | No | No |

\* Behavior differs by database. In Postgres, Repeatable Read is snapshot isolation and still allows write skew. Postgres Serializable uses SSI (detects conflicts and aborts one transaction). MySQL InnoDB defaults to Repeatable Read and uses gap locks. Know your DB's actual behavior, do not rely on the textbook table.

**Write skew example (very common probe):**
Rule: at least one doctor must be on call. Alice and Bob are both on call. Both transactions check "count on call >= 2", both see 2, Alice removes herself, Bob removes himself. Both commit. Now zero on call. No single row was updated by both, so row locks did not conflict.

Fixes: Serializable isolation, `SELECT ... FOR UPDATE` on the rows you read, a constraint, or materialize the conflict into one row (a counter row that both must update).

**Why Read Committed can still double book:** two requests read `AVAILABLE`, both then update. Fix with atomic conditional update (`WHERE status='AVAILABLE'`), row lock, or unique constraint. This is why Part A recommends the conditional update.

### 1.2 Locks, deadlocks, and retries

- **Row lock** (`FOR UPDATE`), **gap/range lock** (prevents phantoms), **advisory lock** (app-defined).
- **Deadlock:** T1 holds A wants B, T2 holds B wants A. DBs detect and abort one.
  - Prevent by always locking rows in a **consistent order**.
- **Always be ready to retry** aborted transactions (serialization failure, deadlock). Retry the whole transaction with backoff.
- Keep transactions **short**. Never call external services (payment API) while holding a DB lock.

### 1.3 MVCC (multi-version concurrency control)

- Each write creates a **new version** of the row. Readers see a consistent snapshot and do not block writers.
- Cost: old versions pile up. Postgres needs `VACUUM`. Long-running transactions block cleanup and bloat tables.

### 1.4 Indexes and storage engines

| | **B-tree** (Postgres, MySQL) | **LSM-tree** (Cassandra, RocksDB, ScyllaDB) |
|---|---|---|
| Writes | Update pages in place (random I/O) | Append to memtable + WAL, flush sorted files (sequential I/O) |
| Reads | Fast, predictable (one tree walk) | May check several files; use Bloom filters |
| Background work | Page splits, vacuum | **Compaction** (can cause latency spikes, write amplification) |
| Best for | Read-heavy, OLTP, range queries | Write-heavy ingestion, time-series |

- **Composite index:** order matters (leftmost prefix rule).
- **Covering index:** index contains all needed columns, so no table lookup.
- Every index **speeds reads and slows writes** and costs storage. Index for your access patterns, not every column.

### 1.5 WAL and durability

- **WAL (write-ahead log):** write the change to a sequential log **before** modifying data pages. After a crash, replay the log.
- `fsync` on commit gives durability but costs latency. Group commit batches fsyncs.
- Replication usually ships the WAL to followers.

### Interview points
- Name the **anomaly** your design prevents and the **mechanism** (constraint, conditional update, lock, serializable).
- Say "I would retry on serialization failure" every time you use a strict isolation level.

---

## 2. Consistency models, replication, and consensus

### 2.1 Consistency models (strongest to weakest)

| Model | Meaning | Example need |
|---|---|---|
| **Linearizability** | Every operation appears to happen at one instant in real time, in a single global order | Locks, leader election, unique usernames, balances |
| **Sequential** | All see the same order, but order need not match real time | Rarely asked directly |
| **Causal** | If A caused B, everyone sees A before B | Comments and replies, chat threads |
| **Read-your-writes** | You always see your own writes | Profile edit then reload |
| **Monotonic reads** | You never see time go backward (new value then old value) | Reading from different replicas |
| **Eventual** | Replicas converge if writes stop | Likes, view counts, feeds |

**How to get read-your-writes with replicas:**
- Read from the primary for a short time after a write, or
- Pin a user session to one replica, or
- Pass a version/LSN token and read from a replica that has caught up.

### 2.2 Replication

| Type | How | Good | Bad |
|---|---|---|---|
| **Leader-follower** | One leader takes writes, followers copy | Simple, strong writes | Failover complexity, replica lag |
| **Multi-leader** | Several leaders (often per region) | Local writes in each region | **Write conflicts** need resolution |
| **Leaderless (Dynamo)** | Any node accepts writes, quorum decides | High availability | Conflicts, tuning, stale reads |

- **Sync replication:** commit waits for follower. Zero loss, higher latency, a down follower can block writes.
- **Async replication:** fast, but a leader crash can **lose recent writes** and followers lag.
- **Semi-sync:** wait for at least one follower. A common middle path.
- **Failover risks:** promoting a lagging follower loses data. Two nodes both thinking they are leader = **split-brain**.

### 2.3 Quorums

With **N** replicas, write to **W**, read from **R**:
- `R + W > N` means read and write sets overlap, so a read sees the latest write (strong-ish).
- Common: `N=3, W=2, R=2`.
- Fast writes: `W=1`. Fast reads: `R=1`. Trade-off knob between write latency, read latency, and consistency.
- Quorums alone do **not** give linearizability in all cases (sloppy quorums, concurrent writes, clock issues). Do not oversell.

### 2.4 Consensus (Raft in plain words)

**Problem:** several nodes must agree on one value or one ordered log, even if some crash.

**Raft in short:**
1. Nodes are **follower / candidate / leader**, with a **term** number.
2. If followers hear no heartbeat, one becomes candidate and asks for votes. A **majority** makes it leader for that term.
3. Clients send writes to the leader. The leader appends to its log and replicates it.
4. An entry is **committed** once a **majority** has it.
5. With `2f + 1` nodes, the cluster tolerates `f` failures. (3 nodes tolerate 1, 5 tolerate 2.)

**Used by:** etcd, Consul, ZooKeeper (ZAB, similar), Kafka KRaft, many databases for metadata.

**Rules of thumb**
- Use consensus for **small, critical metadata**: leader election, config, locks, membership. Not for bulk data.
- Odd number of nodes. Cross-region consensus adds latency (every commit waits for a majority across regions).
- A majority partition continues. The minority stops accepting writes (CP behavior).

### 2.5 Split-brain and fencing

- Prevent split-brain with **majority quorum** and **fencing tokens**: a monotonically increasing number issued with each lock/leadership. The resource rejects any request with an older token.
- A Redis `SETNX` lock with TTL is a **best-effort lease**. A consensus-backed lock (etcd/ZooKeeper) plus fencing is **correctness-critical**. State the difference in interviews.

### 2.6 When to use a coordination service?
Use one for leader election, distributed locks that protect correctness, service membership, and shared config. Do **not** put per-request hot-path data in ZooKeeper/etcd.

---

## 3. Time and clocks

| Concept | What to know |
|---|---|
| **Clock skew** | Machine clocks differ (milliseconds to seconds). NTP reduces but does not remove it |
| **Wall clock vs monotonic clock** | Wall clock can jump (NTP, leap seconds). Use **monotonic** clock to measure durations and timeouts |
| **Timestamps do not prove causality** | Event A at 10:00:01 on node 1 and B at 10:00:00 on node 2 does not mean B happened first |
| **Last-write-wins (LWW)** | Uses timestamps, so skew can silently **drop a newer write**. Fine for low-value data, risky for important data |
| **Lamport clock** | Logical counter that preserves "happened-before" ordering |
| **Vector clock** | Per-node counters that detect **concurrent** writes (conflict) vs ordered writes |
| **Hybrid logical clock (HLC)** | Physical time + logical counter. Used by CockroachDB and others |
| **TrueTime (Spanner)** | Exposes clock uncertainty as an interval, then waits it out to give global consistency. Needs special hardware |
| **Event time vs processing time** | In stream processing, use event time with **watermarks** to handle late data |
| **TTL and lease expiry** | Depend on clocks and pauses (GC, VM freeze). A process can believe its lease is valid after it expired. Use fencing |

**Sorting IDs:** Snowflake IDs are roughly time-ordered but not strictly across machines. Do not use them as proof of order for money or inventory.

---

## 4. Event sourcing and CQRS

### 4.1 Event sourcing

**Idea:** Instead of storing the current state, store **every state change as an immutable event**. Current state = replay of events.

```
Account: [Deposited 100] [Withdrew 30] [Deposited 50]  ->  balance 120
```

**Good for:** ledgers, order history, audit trails, undo/replay, debugging, building new read models later.

| Benefit | Cost |
|---|---|
| Full audit history | Harder to query current state (need projections) |
| Replay to rebuild or fix bugs | Event schema evolution is painful |
| Natural fit for async consumers | Eventual consistency in read models |
| Time travel ("state at time T") | Snapshots needed for long streams |
| | Deleting data (GDPR) is hard: use crypto-shredding or tombstones |

- Use **snapshots** every N events to speed up rebuilds.
- Version your events. Never edit past events; add compensating events.

### 4.2 CQRS (Command Query Responsibility Segregation)

**Idea:** Use a **write model** (validates commands, enforces invariants) and one or more separate **read models** (denormalized for queries), kept in sync by events.

```
Command -> Write model (source of truth) -> events -> Projector -> Read model(s) (Redis, Elasticsearch, SQL views)
Query ------------------------------------------------------------> Read model
```

**Use when:** read and write shapes are very different, reads vastly outnumber writes, or you need several specialized views (search, analytics, timeline).
**Avoid when:** simple CRUD. CQRS adds moving parts and eventual consistency for little gain.

**Interview points:** Always mention that read models are **eventually consistent** and how the UI copes (optimistic UI, read from write model right after a write). Combine with the **outbox** for reliable event publishing.

---

# GROUP 2: Infrastructure design problems

These are the "design the building block" questions. They test whether you understand the systems you normally just use.

---

## 5. Distributed key-value store (Dynamo-style)

**Requirements:** `get(key)`, `put(key, value)`, huge scale, high availability, low latency, tunable consistency, easy to add nodes.

**Architecture**

```
Client -> any node (coordinator)
            |
   consistent hash ring (virtual nodes)
            |
   N replicas on next N distinct nodes -> local storage engine (LSM)
```

**Key design pieces**

| Piece | How it works | Why |
|---|---|---|
| **Partitioning** | Consistent hashing with **virtual nodes** | Even spread, minimal data movement on add/remove |
| **Replication** | Each key stored on **N** successive nodes (ideally across racks/AZs) | Durability and availability |
| **Quorum** | Write to W, read from R, `R+W>N` for stronger reads | Tunable consistency |
| **Sloppy quorum + hinted handoff** | If a replica is down, another node holds the write temporarily and hands it back later | Keeps writes available |
| **Read repair** | On read, if replicas disagree, fix stale ones | Converge lazily |
| **Anti-entropy (Merkle trees)** | Replicas compare hash trees to find and sync differences cheaply | Repair data that is rarely read |
| **Conflict resolution** | Vector clocks (app merges), LWW, or CRDTs | Concurrent writes in an AP system |
| **Membership + failure detection** | **Gossip** protocol; accrual failure detector | No central master |
| **Storage engine** | LSM-tree (memtable + WAL + SSTables + compaction) | Fast writes |
| **Rebalancing** | Move virtual-node ranges to new nodes in background | Elastic scaling |

**Trade-offs**

| Choice | Gain | Cost |
|---|---|---|
| AP (Dynamo style) | Always writable | Conflicts, stale reads |
| CP (Raft per partition, like Spanner/etcd-style) | Strong consistency | Unavailable in minority partition, higher write latency |
| Higher W/R | Consistency | Latency, lower availability |
| More vnodes | Better balance | More metadata, slower rebalancing bookkeeping |
| LWW | Simple | Lost updates under clock skew |

**Interviewers probe:** What if a node dies? What if two clients write the same key? How do you add a node? How do you handle hot keys? How do you detect failures without a master?

---

## 6. Distributed cache

**Requirements:** very low latency, huge throughput, memory-bound, tolerates data loss (it is a cache).

**Design pieces**
- **Sharding:** consistent hashing, client-side or proxy-based (Twemproxy, Envoy) or cluster slots (Redis Cluster uses 16384 hash slots).
- **Replication:** primary + replica for availability. Async, so a failover can lose recent writes (acceptable for cache).
- **Eviction:** LRU (recency), LFU (frequency), TTL. Pick based on access pattern.
- **Hot key:** replicate the key across nodes, add a small local cache in the app, split key (`key#1..#k`).
- **Thundering herd / stampede:** coalesce misses, lock per key, serve stale while revalidating, jittered TTLs.
- **Cache warming** after deploy or failover. A cold cache can crash the DB (protect it with rate limiting).
- **Consistency:** cache-aside + delete-on-write is the usual default. Race: reader fetches old value, writer updates DB and deletes cache, reader then writes the old value into cache. Mitigate with short TTL, versioned keys, or delayed double-delete.

**Trade-offs:** Client-side sharding (no extra hop, clients must know topology) vs proxy (simpler clients, extra hop). Replication (availability) vs memory cost. Strong invalidation vs short TTL.

---

## 7. Message queue / Kafka-like log

**Two models:**

| | **Task queue** (SQS, RabbitMQ) | **Distributed log** (Kafka, Pulsar) |
|---|---|---|
| Consumption | Message removed after ack | Messages retained; consumers track an **offset** |
| Replay | No | Yes (until retention) |
| Ordering | Limited | **Per partition** |
| Scaling | Competing consumers | Partitions + consumer groups |

**Kafka design (what to explain)**
- **Topic -> partitions.** Each partition is an **append-only log** split into segment files. Sequential disk I/O and OS page cache make it fast.
- **Replication:** each partition has a **leader** and followers. **ISR** (in-sync replicas) = followers caught up. Writes go to the leader.
- **Producer durability (`acks`):** `0` fire and forget, `1` leader only, `all` all ISR (with `min.insync.replicas`). Use `all` for important data.
- **Partitioning:** key hash -> partition, so same key = same partition = ordered. No global order across partitions.
- **Consumer groups:** each partition is consumed by **one consumer in a group**. Parallelism is capped by partition count. Group rebalances when consumers join/leave.
- **Offsets:** committed after processing. Commit-after-process = at-least-once. Commit-before = at-most-once.
- **Idempotent producer + transactions:** dedupe producer retries; atomic read-process-write within Kafka. This gives exactly-once **inside Kafka**, not for external side effects.
- **Retention and compaction:** delete by time/size, or keep only the latest value per key (changelog topics).
- **Backpressure:** consumers pull, so slow consumers just lag. Monitor **consumer lag**.

**Pitfalls to mention:** hot partition (skewed keys), too many partitions (overhead), poison messages blocking a partition (use DLQ/retry topics), rebalance storms, ordering lost when retries reorder (use idempotent producer / max in-flight limits).

---

## 8. Metrics monitoring and alerting platform

**Requirements:** ingest millions of data points per second, query dashboards fast, alert reliably, retain history cheaply.

```
Agents/SDKs -> Ingestion (Kafka) -> Time-series DB (hot) -> Downsampler -> Long-term store (cold)
                                 -> Alert evaluator -> Notification service
Dashboards -> Query service -> TSDB / cache
```

**Key points**
- **Push vs pull collection:** pull (Prometheus) is simple and detects dead targets; push suits short-lived jobs and firewalls.
- **Data model:** `metric_name + labels + timestamp + value`. **Cardinality** (number of unique label combinations) is the main enemy. Never put user_id in a label.
- **Storage:** time-series DB with delta/gorilla-style compression, partitioned by time and series.
- **Downsampling / rollups:** keep raw for days, 1-min for weeks, 1-hour for a year.
- **Alerting:** evaluate rules on a schedule, **dedupe and group** alerts, support silences, escalation, and routing. Alert on symptoms (error rate, latency), not every cause.
- **Alert system must be more reliable than the thing it monitors:** run it separately, add a "dead man's switch" (alert if no data arrives).
- **Trade-offs:** resolution vs cost, retention vs cost, push vs pull, query freshness vs load.

---

## 9. API gateway and service discovery

**API gateway** (single entry point): routing, authentication, rate limiting, TLS termination, request/response transformation, caching, logging, canary routing.
- Risks: SPOF (run many, stateless), added latency, becoming a "god" component. Keep business logic out.
- **BFF (backend for frontend):** a gateway per client type (mobile, web) that aggregates calls. Handle partial failure and timeouts in aggregation.

**Service discovery**
- **Client-side** (client asks registry, picks instance: Eureka, Consul) vs **server-side** (client calls a load balancer / DNS / service mesh: Kubernetes Services).
- Registry needs **health checks**, TTL/heartbeats, and must tolerate being stale (clients cache, fall back).
- **Service mesh** (Envoy/Istio): sidecars handle retries, timeouts, mTLS, observability. Cost: complexity, latency, resource use.

---

## 10. Distributed lock and coordination service

**Requirements:** mutual exclusion across machines, safe under crashes and pauses.

| Approach | Safety | Notes |
|---|---|---|
| **DB row / unique constraint** | Strong (inside one DB) | Often the best answer |
| **Redis SETNX + TTL** | Best-effort | Fast, can lose lock on failover, clock/pause issues |
| **Redlock** | Debated | Do not rely on it for correctness-critical work |
| **etcd / ZooKeeper (consensus)** | Strong | Use leases + **fencing tokens** |

**Design:**
1. Acquire lock with a unique owner ID and a lease TTL.
2. Keep alive (heartbeat) while working.
3. Release only if you are the owner (compare-and-delete, atomic).
4. Return a **fencing token**. The protected resource rejects stale tokens.

**Key line for interviews:** "A lock with a TTL is a lease. A paused process can wake up after expiry and act. The resource itself must check the fencing token. Better still, avoid the lock with a DB constraint or idempotent operations."

---

## 11. Low-latency single-writer systems

### 11.1 Stock exchange / order matching engine

**Requirements:** microsecond-to-millisecond latency, strict **fairness (price-time priority)**, **deterministic**, no lost orders, auditable.

```
Clients -> Gateway (auth, validation, risk checks)
        -> Sequencer (assigns global order, writes to replicated log)
        -> Matching engine (single thread per symbol, in-memory order book)
        -> Output events: trades, order acks, market data
        -> Persist event log, replicate to hot standby
```

**Key design points**
- **Single-threaded matching per symbol (partition by symbol).** No locks, deterministic, very fast. Scale by sharding symbols across engines.
- **Order book:** per side, price levels sorted (e.g., tree/array), each level a FIFO queue. Operations: add, cancel, match.
- **Event sourcing:** the input log is the source of truth. Replaying gives the exact same state (deterministic). Hot standby replays the same log for fast failover.
- **Pre-trade risk checks** before an order reaches the engine.
- **Market data fan-out:** multicast/UDP or streaming to many subscribers. Slow consumers must not slow the engine.
- **Trade-offs:** consistency and fairness over availability (CP). Single thread per symbol limits per-symbol throughput but gives correctness and speed. Avoid GC pauses (off-heap, pre-allocated memory).
- Pattern references: LMAX Disruptor (ring buffer, single writer).

### 11.2 Online auction / bidding

- Core: **one highest bid per item**, strict deadline. Use Pattern 1: atomic conditional update (`UPDATE ... WHERE current_price < :bid`) or a single-writer per auction.
- **Deadline handling:** server decides time (never trust client clocks). Scheduled job closes auction. Add **anti-sniping** (extend time on late bids).
- **Real-time updates:** WebSocket/SSE push of the current price (Pattern 7). Price display is eventually consistent, bid acceptance is strongly consistent.
- **Hot auction:** shard by auction ID; queue bids per auction to serialize them.
- Payment: hold/authorize on bid or at close, saga for settlement (Pattern 5).

---

# GROUP 3: Missing pattern families

---

## 12. Recommendation, ranking, and graph systems

### 12.1 Recommendation system

**Core challenge:** pick a few good items from millions, in tens of milliseconds, personalized, and improve over time.

**The standard funnel**

```
All items (millions)
  -> Candidate generation  (fast, rough: ~hundreds-thousands)
  -> Ranking               (ML model, richer features: ~tens-hundreds)
  -> Re-ranking / filters  (diversity, business rules, dedupe, already seen)
  -> Top N shown to the user
```

| Stage | Techniques | Trade-off |
|---|---|---|
| **Candidate generation** | Collaborative filtering, item-to-item similarity, **embeddings + ANN search** (two-tower models, FAISS/HNSW), popularity, recent activity | Recall vs latency |
| **Ranking** | Gradient boosted trees or neural nets using user, item, context features | Accuracy vs inference cost |
| **Re-ranking** | Diversity, freshness boost, fairness, ads mix, remove seen items | Business rules vs relevance |

**Supporting pieces**
- **Feature store:** same features for offline training and online serving (avoid training-serving skew).
- **Offline vs online:** train models offline (batch, daily), precompute heavy things offline, compute fresh signals (last click) online.
- **Feedback loop:** log impressions, clicks, watch time, then retrain. Beware popularity bias and filter bubbles.
- **Cold start:** new users (use popularity, onboarding questions, context), new items (content-based features, exploration).
- **Exploration vs exploitation:** bandits / epsilon-greedy to test new items.
- **Evaluation:** offline metrics (precision@k, recall@k, NDCG), then **A/B tests** for real impact.
- **Caching:** cache precomputed recommendations per user, with short TTL; fall back to popular items if the model service is down.

### 12.2 Social graph (follow/friends)

- **Storage:** adjacency lists. `following(user_id -> list)` and `followers(user_id -> list)` stored separately (two directions). Key-value or wide-column store, sharded by user_id.
- **Counts** (followers) are denormalized counters (eventually consistent, sharded for celebrities).
- **Friend-of-friend / mutual friends:** 2-hop traversal. Precompute for popular queries, cap fan-out, use sets intersection (sorted lists or Bloom filters).
- **Graph DB (Neo4j) vs relational/KV:** graph DB for deep traversals; at huge scale most companies use sharded KV/adjacency stores with custom services (e.g., Facebook TAO).
- **Celebrity (supernode) problem:** huge follower lists. Paginate, avoid loading entire list, hybrid fan-out (Part A Pattern 2).
- **Privacy rules** (blocked, private accounts) must be enforced at read time.

---

## 13. Usage metering, quotas, and billing

**Problem:** API/SaaS platform must count usage, enforce limits, and bill correctly.

```
Services emit usage events
   -> Ingestion (Kafka) with unique event_id
   -> Dedup + validate
   -> Aggregation (per customer, per metric, per window)
   -> Usage store (immutable raw events + rolled-up aggregates)
        -> Entitlement/quota service (real-time check, cached counters)
        -> Billing service (invoice at end of period)
```

**Key design points**

| Topic | Design |
|---|---|
| **Event correctness** | Every event has a **unique ID** (idempotent ingestion), customer ID, metric, quantity, event timestamp |
| **Dedup** | Dedup by event_id within a retention window; make aggregation idempotent |
| **Late events** | Use event time, allow a late window, support **corrections/adjustments** (never edit invoices; add adjustment entries) |
| **Quota enforcement** | **Soft quota:** check cached/approximate counters, tolerate small overuse (fast, available). **Hard quota:** atomic counter (Redis `INCR`/DB) per customer, may reduce availability or add latency |
| **Prepaid vs postpaid** | Prepaid: balance decrement per use (needs atomic, ledger-like). Postpaid: aggregate then invoice |
| **Entitlements** | Plan -> limits/features table. Cache with short TTL, invalidate on plan change |
| **Invoicing** | Generate from immutable usage records for a closed period. Idempotent invoice generation, versioned |
| **Reconciliation** | Compare metered usage vs source system and payments. Alert on drift |
| **Audit** | Keep raw events long enough to explain any bill |

**Trade-offs:** exact vs approximate counting (bill on exact, enforce on approximate), real-time vs batch aggregation, soft vs hard quotas (see section 24). Hot customers need sharded counters.

---

## 14. AI-era systems

### 14.1 LLM gateway / inference platform

```
Clients -> Gateway (authn/z, quotas) -> Router -> Model backends (providers / self-hosted GPUs)
              |-> Cache (exact + semantic)
              |-> Usage metering + cost tracking
              |-> Logging (PII-redacted), guardrails
```

**Key points**
- **Token-based rate limiting and budgets**, not just requests per second (cost scales with tokens). Enforce per user/tenant/model. Estimate tokens before, reconcile after.
- **Streaming responses** via SSE/WebSocket so users see output early. Handle client disconnects (cancel generation to save cost).
- **Routing and fallback:** pick model by task/cost/latency, fall back to another model or provider on errors or rate limits. Circuit breakers per provider.
- **Batching** on GPU servers (continuous batching) raises throughput but adds latency. Separate queues by priority.
- **Caching:** exact-match prompt cache; **semantic cache** (embedding similarity) saves cost but risks wrong hits. Prompt-prefix (KV) caching for shared prefixes.
- **Timeouts and long tails:** LLM latency is high and variable. Set per-request deadlines, return partial results where possible.
- **Safety:** input/output guardrails, prompt-injection handling, PII redaction in logs, tenant isolation.
- **Observability:** time-to-first-token, tokens/sec, cost per request, error and refusal rates, quality evals.
- **Trade-offs:** quality vs cost vs latency model choice, caching hit rate vs correctness, self-hosting (control, fixed cost, ops) vs API (flexible, per-token cost).

### 14.2 RAG (retrieval-augmented generation) system

**Ingestion path**
```
Sources -> parse -> chunk -> embed -> index (vector DB + keyword index) with metadata + ACLs
```
**Query path**
```
Query -> (rewrite) -> hybrid retrieval (vector + BM25) -> metadata/ACL filter -> rerank -> build prompt -> LLM -> answer with citations
```

| Topic | Key points |
|---|---|
| **Chunking** | Size/overlap trade-off: small chunks = precise but lose context, large = noisy |
| **Hybrid search** | Vector search for meaning, keyword for exact terms; combine and rerank |
| **Permissions** | Enforce **access control at retrieval time** (filter by user/tenant ACL). Never rely on the LLM to hide data |
| **Freshness** | Incremental re-indexing via CDC/events; handle deletes (remove embeddings) |
| **Evaluation** | Retrieval: recall@k, MRR. Generation: faithfulness/groundedness, answer relevance. Keep a golden test set |
| **Vector DB choice** | ANN index (HNSW/IVF) trade-offs: recall vs latency vs memory; managed vs self-hosted |
| **Failure modes** | Wrong chunks retrieved, stale data, hallucination despite context, prompt injection through documents |
| **Cost/latency** | Cache embeddings and frequent queries, limit context size, rerank only top candidates |

### 14.3 Agent / workflow platform (multi-agent systems)

- **Orchestrator as a durable state machine / graph:** persist state at each step (checkpoints) so a crash resumes instead of restarting.
- **Tool calls are side effects:** make them **idempotent** (idempotency keys), bounded by timeouts, and retried with backoff. Record every call and result for audit/replay.
- **Control loops:** max steps, max cost/tokens, wall-clock deadline to prevent runaway agents.
- **Human-in-the-loop** approval for risky actions. Pause/resume support.
- **Concurrency:** parallel tool calls with partial-failure handling; avoid shared mutable state between agents.
- **Isolation and security:** sandbox code execution, least-privilege credentials per tool, tenant isolation, prompt-injection defenses.
- **Observability:** trace across agent steps and tool calls, log prompts/outputs (with redaction), track cost per run.
- **Trade-offs:** autonomy vs control, one big agent vs several specialists, sync vs async runs (long runs should be jobs with status APIs).

---

# GROUP 4: Production engineering

---

## 15. Reliability: SLOs, overload, and tail latency

### 15.1 SLI, SLO, error budget

- **SLI:** a measured indicator (e.g., % of requests under 300 ms and non-error).
- **SLO:** the target (e.g., 99.9% over 30 days).
- **Error budget:** `100% - SLO`. At 99.9% over 30 days = about **43 minutes** of allowed badness. Spend it on releases and experiments. If it is exhausted, freeze risky changes.

| Availability | Downtime per year | Per month (30 d) |
|---|---|---|
| 99% | 3.65 days | 7.2 hours |
| 99.9% | 8.76 hours | 43.2 min |
| 99.99% | 52.6 min | 4.3 min |
| 99.999% | 5.26 min | 26 sec |

**Dependency math:** services in series multiply availability. Three dependencies at 99.9% each give about `0.999^3 = 99.7%`. Lesson: reduce hard dependencies, add fallbacks, make non-critical calls async/optional.

### 15.2 Latency percentiles and tail latency
- Track **p50, p95, p99** (and p99.9), not just average. A page that calls 10 services is slow whenever **any** call is slow, so tail latency dominates (fan-out amplifies it).
- Fixes: timeouts, **hedged requests** (send a duplicate after a delay, use the first reply), caching, smaller fan-out, fast fallbacks.

### 15.3 Overload and failure behavior

| Problem | What happens | Defense |
|---|---|---|
| **Retry storm** | Failures cause retries, retries add load, system collapses | Exponential backoff + **jitter**, retry budgets, retry only idempotent calls, retry at **one** layer |
| **Thundering herd** | Many clients hit at once (cache expiry, restart) | Jittered TTLs, request coalescing, staggered restarts |
| **Cascading failure** | One slow dependency exhausts threads/connections upstream | **Timeouts**, circuit breakers, bulkheads |
| **Overload** | Queues grow, latency explodes, everything times out | **Load shedding**, admission control, bounded queues |
| **Slow consumer / backlog** | Queue grows forever | Backpressure, limits, scale consumers, drop or defer low priority |

**Techniques**
- **Timeouts everywhere**, with a **deadline propagated** down the call chain (each hop gets the remaining budget).
- **Circuit breaker:** closed -> open (fail fast after threshold) -> half-open (probe) -> closed. Pair with a fallback.
- **Bulkheads:** separate thread/connection pools per dependency or tenant.
- **Load shedding:** reject early and cheaply (HTTP 429/503), prefer dropping low-priority traffic, protect critical paths.
- **Bounded queues:** unbounded queues hide overload until memory dies. Prefer reject-when-full.
- **Graceful degradation:** serve stale cache, simplified results, or disable non-critical features instead of failing.
- **Retries and circuit breakers interact:** retries must stop when the breaker opens.

---

## 16. Security, identity, and authorization

### 16.1 The framework to apply to any design

| Layer | Questions to answer |
|---|---|
| **Identity (AuthN)** | Who is calling? Users, services, devices |
| **Authorization (AuthZ)** | What may they do, on which resource? |
| **Data protection** | Encryption in transit and at rest, PII handling, key management |
| **Abuse prevention** | Rate limits, bot protection, fraud signals |
| **Auditability** | Who did what, when? Tamper-resistant logs |

### 16.2 Authentication

- **Passwords:** salted slow hash (bcrypt/argon2), MFA, lockout/rate limit, never log secrets.
- **OAuth 2.0 / OIDC:** delegated access (OAuth) and login/identity (OIDC). Use **Authorization Code + PKCE** for browser/mobile apps. Short-lived access tokens + refresh tokens.
- **Sessions vs JWT:**

| | Server session (opaque ID) | JWT (self-contained token) |
|---|---|---|
| Revocation | Easy (delete session) | Hard (valid until expiry) |
| Scalability | Needs shared session store | Stateless verification |
| Typical fix | - | Short expiry + refresh + revocation list for critical cases |

- **Service-to-service:** **mTLS** (both sides present certificates), short-lived workload identities (SPIFFE), or signed tokens. Do not trust "internal network" alone (zero trust).
- **API keys:** hashed at rest, scoped, rotatable, rate limited.

### 16.3 Authorization

- **Models:** RBAC (roles), ABAC (attributes/policies), **ReBAC** (relationships, like Google Zanzibar for docs sharing).
- **Resource-level checks on every request.** The most common real bug is **IDOR**: `/orders/123` returns another user's order because only login was checked, not ownership.
- **Centralize policy** (policy engine or authorization service) but cache decisions carefully (stale permissions = security bug). Define how fast revocation must take effect.
- **Multi-tenant:** tenant ID in every query and cache key; enforce at the data layer, not just the API.

### 16.4 Data protection

- **TLS everywhere.** Encrypt at rest. Use **KMS with envelope encryption** (data keys encrypted by a master key). Rotate keys.
- **Secrets:** a secrets manager, not env files in repos. Rotate regularly, least privilege, short-lived credentials.
- **PII:** minimize, tokenize, mask in logs, restrict access, define retention.
- **Payments:** keep card data with a PCI-compliant provider (tokenization) to reduce scope.

### 16.5 Abuse and audit
- Rate limit by IP/user/tenant, CAPTCHA/risk scoring on signup/login, OTP limits per destination.
- **Audit log:** append-only, includes actor, action, resource, time, source; protect from tampering; ship to separate storage.

---

## 17. Observability and incident response

### 17.1 The three pillars

| Pillar | Use | Notes |
|---|---|---|
| **Metrics** | Trends, alerts, dashboards | Cheap, aggregated. Watch cardinality |
| **Logs** | Detailed events for debugging | Structured (JSON), include request/trace ID, redact PII |
| **Traces** | Follow one request across services | Propagate trace ID, sample (e.g., keep errors and slow requests) |

### 17.2 What to measure
- **RED** (for services): **R**ate, **E**rrors, **D**uration.
- **USE** (for resources): **U**tilization, **S**aturation, **E**rrors.
- **Golden signals:** latency, traffic, errors, saturation.
- **System-specific:** queue lag/age of oldest message, replication lag, cache hit ratio, DB connections, consumer lag, DLQ size, job success rate.
- **Business metrics:** orders/min, payment success rate, bookings. They catch problems technical metrics miss.

### 17.3 Alerting
- **Alert on symptoms users feel** (error rate, latency, SLO burn rate), not every internal cause.
- **Burn-rate alerts:** page when the error budget is being consumed too fast (e.g., fast window + slow window).
- Every alert needs: clear meaning, an owner, a **runbook**, and must be actionable. Delete noisy alerts.
- Have **dead man's switch** alerts (alert if metrics stop).

### 17.4 Incident response
1. **Detect** (alert), **triage** severity.
2. **Mitigate first**, root-cause later: rollback, disable feature flag, shed load, fail over, scale out.
3. **Communicate** (status page, incident channel, one incident commander).
4. **Resolve**, verify with metrics.
5. **Blameless postmortem:** timeline, impact, root cause, contributing factors, action items with owners.

**Interview line:** "For every design I would state the SLOs, the dashboards and alerts, and how we roll back."

---

## 18. Migrations, re-sharding, and deployment safety

### 18.1 Zero-downtime schema change: expand and contract

1. **Expand:** add new column/table (nullable, no breaking change). Old code still works.
2. **Dual write:** new code writes both old and new.
3. **Backfill** old data in small batches (throttled, resumable, checkpointed).
4. **Verify:** compare old and new (counts, checksums, sampled reads).
5. **Switch reads** to new (feature flag, gradual).
6. **Contract:** stop writing old, then drop old column later.

Rules: every step must be **backward compatible** with the previous app version (rolling deploys run both versions at once). Never rename or drop in one release. Create indexes **online** (`CREATE INDEX CONCURRENTLY` in Postgres) to avoid long locks.

### 18.2 Migrating to a new datastore or service

- **Dual writes + backfill + shadow reads:** write to both, read from old, also read new and **compare** silently.
- **Change Data Capture (CDC)** to copy changes reliably instead of fragile app-level dual writes.
- **Gradual cutover:** 1% -> 10% -> 50% -> 100% by feature flag. Keep a **rollback path** until confident.
- **Strangler fig:** route one capability at a time from the monolith to a new service, behind a facade, until the old one is empty.
- Define **success metrics** and **stop conditions** before starting.

### 18.3 Re-sharding and changing partition keys

- **Avoid the problem:** use many **logical shards** (e.g., 1024) mapped to few physical nodes. Resharding = moving logical shards, not rehashing everything.
- **Directory/lookup service** maps key (or range) -> shard. Lets you move data gradually.
- **Online move per shard:** copy snapshot, stream changes (CDC), brief write pause or cutover, update mapping, delete old copy.
- **Changing the partition key** is a full migration: build a new table/index keyed differently, dual write, backfill, cut over reads. Plan the key carefully upfront.
- **Hot shard fix:** split that shard, or add salting/sub-keys for hot entities.

### 18.4 Safe deployments

| Technique | Idea |
|---|---|
| **Rolling** | Replace instances gradually. Needs backward compatibility |
| **Blue-green** | Two environments, switch traffic. Instant rollback, double cost |
| **Canary** | Send small % to new version, watch metrics, expand or roll back automatically |
| **Feature flags** | Deploy code dark, enable gradually, kill-switch. Separate deploy from release. Clean up old flags |
| **Backward-compatible APIs/events** | Add fields, do not remove or repurpose. Version when you must break |
| **Database + code order** | Expand schema first, deploy code, contract later |

---

## 19. Multi-region architecture and disaster recovery

### 19.1 Why multi-region?
Lower latency for global users, survive a region outage, meet **data residency** laws. It adds major complexity and cost, so justify it with requirements (availability, RTO/RPO, latency).

### 19.2 Models

| Model | How | Good | Bad |
|---|---|---|---|
| **Single region + backups** | One region, backups elsewhere | Simplest, cheap | Long recovery, region outage = downtime |
| **Active-passive** | Standby region replicates, takes over on failure | Simpler consistency | Idle cost, failover tested rarely, some data loss with async replication |
| **Active-active** | Both regions serve traffic | Low latency, high availability | Hard: write conflicts, data consistency, cost |

### 19.3 Handling data in active-active

| Strategy | Idea | Trade-off |
|---|---|---|
| **Home region per user/entity** | Each user's data has one owning region (writes there); other regions read/replicate | Simple correctness, cross-region users pay latency |
| **Single global leader** | One region takes all writes | Strong consistency, high write latency for far users |
| **Multi-leader + conflict resolution** | Each region accepts writes, merge (LWW, CRDTs, app logic) | Availability, risk of conflicts/lost updates |
| **Globally consistent DB** (Spanner, CockroachDB style) | Consensus across regions | Strong consistency, higher write latency |

> Routing traffic to another region is not enough if the data there is stale, conflicting, or missing. Always explain the **data** story, not only the traffic story.

### 19.4 Traffic routing and failover
- **DNS-based** (latency/geo routing, health checks; TTL delays failover) or **anycast/global load balancer** (faster).
- **Health checks** must avoid false failover (flapping). Decide **automatic vs manual** failover (automatic is fast but risks split-brain and cascading overload on the survivor).
- The surviving region must have **capacity** for the extra load (or shed load).
- **Failback** (returning to the primary) needs data resync and is risky. Plan it.

### 19.5 RPO, RTO, and DR tiers

- **RPO** (Recovery Point Objective): how much data loss is acceptable. **RTO** (Recovery Time Objective): how long until service is back.

| DR strategy | RPO / RTO | Cost |
|---|---|---|
| Backup and restore | Hours / hours-days | Lowest |
| Pilot light (minimal core always running) | Minutes / tens of minutes | Low |
| Warm standby (scaled-down copy running) | Seconds-minutes / minutes | Medium |
| Active-active | Near zero / near zero | Highest |

- **Test it:** run regular DR drills and **restore tests**. An untested backup is not a backup.
- **Data residency:** keep regulated data in required regions (shard by region, avoid global replication of that data).

---

## 20. API design and network fundamentals

### 20.1 API design checklist
- **Style:** REST (resources, HTTP verbs, cacheable), gRPC (typed, fast, streaming, internal services), GraphQL (client chooses fields, risk of expensive queries: add depth/cost limits).
- **Idempotency:** `PUT`/`DELETE` idempotent by definition; for `POST` accept an **`Idempotency-Key`** header and store the result.
- **Pagination:** cursor-based for large/changing data (`?cursor=...&limit=50`). Return a `next_cursor`.
- **Long-running operations:** return `202 Accepted` + an operation/job resource to poll (or a webhook).
- **Errors:** consistent format, correct status codes (400 vs 401 vs 403 vs 404 vs 409 vs 429 vs 503), `Retry-After` for 429/503.
- **Versioning:** URL or header versions, backward-compatible changes (add, do not remove), deprecation policy.
- **Rate limit headers**, request IDs for tracing, filtering/sorting conventions.
- **Webhooks vs polling:** webhooks are efficient but need signatures, retries, replay. Offer both where useful.
- **Concurrency control:** `ETag` / `If-Match` for optimistic updates.

### 20.2 Network fundamentals

| Topic | What to know |
|---|---|
| **DNS** | Name to IP. TTL caching affects failover speed. Used for geo/latency routing |
| **TCP vs UDP** | TCP: reliable, ordered, connection setup, head-of-line blocking. UDP: no guarantees, low latency (video, gaming, QUIC, DNS) |
| **TLS** | Handshake adds round trips (TLS 1.3 reduces). Use session resumption and connection reuse |
| **Connection pooling / keep-alive** | Reuse connections to avoid repeated TCP+TLS setup. Pool sizing matters (DB pools too) |
| **HTTP/1.1 vs HTTP/2 vs HTTP/3** | HTTP/2: multiplexing over one TCP connection (but TCP head-of-line blocking). HTTP/3: QUIC over UDP, removes that blocking, faster on lossy mobile networks |
| **WebSocket / SSE / long polling** | Persistent connections for real-time (Part A Pattern 7) |
| **L4 vs L7 load balancer** | L4 routes by IP/port (fast). L7 routes by HTTP content (paths, headers, auth) |
| **CDN** | Edge caches static/semi-static content, terminates TLS near users, absorbs traffic spikes |
| **Latency numbers** | Cross-continent ~150 ms RTT. Each extra round trip matters on mobile |

---

## 21. Data modeling and partition key design

### 21.1 Access-pattern-first design (especially for NoSQL)
1. List the **queries** (read and write) with frequency and latency needs.
2. Design the **key** so the main queries hit **one partition**.
3. Denormalize to avoid cross-partition joins.
4. Accept duplicated data and keep it in sync (events/CDC).

### 21.2 Good partition keys
- **High cardinality** (many distinct values).
- **Even access distribution** (no hot values).
- Matches the **main query** (get messages by conversation_id -> partition by conversation_id).
- Bad keys: `country`, `date` alone (everyone writes "today"), `status`, a celebrity user_id.

**Hot partition fixes:** add a **salt/suffix** (`key#0..#9`) and fan-in on read, add time bucket (`device_id#2026-10-17`), split heavy tenants, write-sharding for counters.

**Time-series:** partition by time bucket plus entity; use TTL and tiered storage.

**Range vs hash partitioning:** hash = even load, no range scans; range = efficient scans, risk of hotspots (monotonic keys like timestamps).

### 21.3 Schema evolution
- Use **Protobuf/Avro** with a schema registry. **Backward compatible** (new readers read old data) and **forward compatible** (old readers ignore new fields).
- Add optional fields with defaults. **Never reuse or renumber field IDs.** Avoid changing types.
- Events live forever (logs, event stores), so version them.

---

## 22. Privacy, cost, and testing

### 22.1 Privacy and compliance
- **Data minimization and retention:** collect only what you need, set TTLs, archive or delete on schedule.
- **Deletion propagation (right to erasure):** delete across primary DB, caches, search index, analytics, backups, logs, derived datasets. Techniques: deletion events, tombstones, **crypto-shredding** (delete the encryption key), retention-limited backups.
- **Encryption and access control:** least privilege, audit access to PII, mask in non-prod.
- **Data residency:** regional storage and processing for regulated data.
- **Consent and purpose limits** where required (GDPR, DPDP, CCPA).

### 22.2 Cost engineering
- Think **cost per request / per user / per GB**. Big costs: compute, storage, **network egress**, cross-AZ/cross-region traffic, managed service premiums, observability data.
- **Cache economics:** memory is expensive; cache only hot data; measure hit rate.
- **Tiered storage:** hot SSD, warm, cold object storage, archive.
- **Right-size:** autoscaling, spot/preemptible for batch, reserved capacity for baseline.
- **Multi-region doubles cost** (replication, standby capacity). Justify with RTO/RPO and latency goals.
- **Build vs buy:** managed (faster, less ops, per-unit cost, lock-in) vs self-hosted (control, cheaper at scale, operational burden).

### 22.3 Testing distributed systems
- **Load test** (expected peak), **stress test** (find the breaking point), **soak test** (long run for leaks).
- **Fault injection / chaos testing:** kill instances, add latency, drop packets, fail dependencies. Verify timeouts, retries, failover.
- **Race-condition tests:** concurrent requests on the same resource (double-booking test with 1,000 parallel requests).
- **Recovery tests:** restore from backup, replay events, region failover **game days**.
- **Consistency testing** (Jepsen-style) for databases and replication.
- **Contract tests** between services, **canary + automated rollback** in production.

---

# GROUP 5: Practice material

---

## 23. More problems as design cards

Each card: **patterns used, key points, common traps.**

### Food delivery (Swiggy/Zomato/DoorDash)
- **Patterns:** order workflow (5), geospatial (10), resource allocation for delivery partners (1), real-time tracking (7), search (6), notifications (8).
- **Key points:** order state machine (`PLACED -> ACCEPTED -> PREPARING -> PICKED_UP -> DELIVERED`), restaurant accept timeout, rider assignment with offer/timeout, ETA estimation, live location, payment saga with refunds, surge/pricing, menu/inventory availability caching.
- **Traps:** assigning one rider to two orders unintentionally, payment captured but order rejected, stale menu, location update write storm.

### Live streaming (Hotstar/Twitch)
- **Patterns:** media (9), read-heavy at massive scale (2), real-time chat (7), analytics (14).
- **Key points:** ingest (RTMP/WebRTC) -> transcode ladder -> segment (HLS/DASH) -> **CDN** (the whole game for millions of viewers), low-latency vs scale trade-off (more latency = better cache efficiency), live chat/reactions fan-out with sampling and rate limits, concurrent viewer counts (approximate), autoscaling and **pre-warming for known events**.
- **Traps:** origin overload from CDN misses, thundering herd at match start, chat fan-out to millions.

### Video conferencing (Zoom/Meet)
- **Patterns:** real-time (7), geospatial routing of media servers, scheduling (12).
- **Key points:** **WebRTC**; signaling server (setup) vs media path; **SFU** (forwards streams, scales well, common) vs **MCU** (mixes, heavy server cost) vs P2P (small calls); adaptive bitrate/simulcast; TURN/STUN for NAT; assign meeting to a nearby media server; recording as async pipeline.
- **Traps:** assuming WebSocket carries video, no NAT traversal plan.

### Email service
- **Patterns:** async (4), event-driven (8), search (6), file storage (9).
- **Key points:** SMTP ingestion/outgoing queue with retries, spam/virus filtering pipeline, mailbox storage (metadata in DB, bodies/attachments in object store), threading, search index, deliverability (SPF, DKIM, DMARC, IP reputation, bounce handling), per-tenant rate limits.
- **Traps:** losing mail on crash (durable queue before ack), ignoring bounces and reputation.

### Calendar and recurring events
- **Patterns:** scheduling (12), resource allocation (1) for rooms, multi-tenant (13), notifications (8).
- **Key points:** store recurrence as a **rule (RRULE) + exceptions**, not expanded rows; expand lazily for a time window; time zones and DST (store UTC + original zone); conflict detection for rooms/attendees (atomic booking); invitations and RSVP state; reminders via delayed jobs.
- **Traps:** expanding infinite recurrences, DST bugs, double-booking meeting rooms.

### Maps and routing (Google Maps)
- **Patterns:** geospatial (10), read-heavy (2), analytics/streaming (14).
- **Key points:** **map tiles** pre-rendered and served via CDN (zoom levels, vector tiles); road network as a **graph**; routing with Dijkstra/A* is too slow at scale, so use **contraction hierarchies / hierarchical graph partitioning**; live traffic from probe data (streaming aggregation) adjusting edge weights; ETA from historical + live data; geocoding/search.
- **Traps:** running plain Dijkstra on a continental graph per request.

### Subscription and billing platform
- **Patterns:** transactional workflow (5), scheduling (12), metering (13), async (4).
- **Key points:** subscription state machine (`TRIAL -> ACTIVE -> PAST_DUE -> CANCELLED`), recurring charge scheduler (idempotent per billing cycle), retries/dunning on failed payment, proration on plan change, invoices immutable with adjustments, webhooks from payment provider, tax handling, reconciliation.
- **Traps:** charging twice for one cycle (needs idempotency key per subscription+period), timezone/period boundary errors.

### Order fulfillment and multi-warehouse inventory
- **Patterns:** resource allocation (1), saga (5), event-driven (8).
- **Key points:** reserve stock per warehouse (soft reserve -> commit), allocation strategy (nearest warehouse, split shipments), inventory counts as ledger of movements, stock sync with external systems and reconciliation, shipment tracking events, returns flow.
- **Traps:** overselling across channels, trusting cached stock for final allocation.

### Content moderation pipeline
- **Patterns:** async (4), event-driven (8), ML serving.
- **Key points:** staged pipeline: fast cheap filters (hash matching, rules) -> ML classifiers -> **human review queue** with priority; decide pre-publish vs post-publish; appeals and audit trail; thresholds balance precision/recall; reviewer tooling and wellbeing; privacy and legal retention.
- **Traps:** blocking the upload path on slow ML, no human fallback, no feedback loop to retrain.

### Data pipeline / ETL platform
- **Patterns:** analytics (14), async (4), scheduling (12).
- **Key points:** DAG orchestration (Airflow-style), idempotent and re-runnable tasks, backfills, partitioned outputs (overwrite a partition atomically), data quality checks, schema evolution, lineage, late data handling, ingestion via CDC vs batch extracts, data lake + warehouse.
- **Traps:** non-idempotent tasks producing duplicates on retry, no backfill strategy.

### Ad serving and click tracking
- **Patterns:** read-heavy low-latency (2), write-heavy ingestion (3), analytics (14), recommendation/ranking (12).
- **Key points:** real-time **auction** within ~100 ms (candidate ads -> targeting filters -> predicted CTR -> bid ranking), budget pacing with approximate shared counters, click/impression logging via Kafka, **dedup and fraud detection** on clicks, attribution windows, billing reconciliation.
- **Traps:** double-counted clicks, overspending budgets due to counter lag, blocking ad response on logging.

### Online judge (LeetCode)
- **Patterns:** async (4), resource isolation, queues.
- **Key points:** submission -> queue -> sandboxed workers (containers/gVisor, CPU/memory/time limits, no network), results store, per-language worker pools, contest spikes (autoscale, prioritize), plagiarism detection, leaderboard (sorted sets).
- **Traps:** running untrusted code without sandboxing, no resource limits.

### CI/CD system
- **Patterns:** async (4), scheduling (12), file storage (9), multi-tenant (13).
- **Key points:** webhook trigger -> pipeline DAG -> job queue -> ephemeral runners (containers/VMs), caching dependencies/artifacts, logs streamed in real time, secrets handling, concurrency limits per tenant, retries, artifact storage.
- **Traps:** leaking secrets into logs, noisy neighbors, unbounded runner cost.

### Splitwise (expense sharing)
- **Patterns:** transactional (5), ledger.
- **Key points:** store expenses as immutable entries; balances derived (or maintained as a ledger); **debt simplification** (minimize transactions: graph of net balances); idempotent expense creation; currency handling; audit.
- **Traps:** floating-point money (use integer minor units), editing history instead of adjustments.

### Pastebin
- **Patterns:** read-heavy (2), key-value (15), file storage (9).
- **Key points:** short unique key generation, content in object storage + metadata in DB, expiry (TTL + cleanup), CDN for popular pastes, abuse/spam control, privacy (unlisted vs public), size limits.
- **Traps:** key collisions, unbounded storage with no expiry.

---

## 24. New decision tables (simplest option, what breaks, when to switch)

For each decision, learn **three things**: the simplest viable option, what breaks as you grow, and the signal that justifies the more complex option.

| Decision | Simplest option | What breaks as you grow | Signal to switch |
|---|---|---|---|
| **Single vs multi-region** | One region + cross-region backups | Region outage = long downtime; far users have high latency | Availability/RTO target or latency SLO cannot be met; regulatory residency |
| **Strong consistency vs availability in a partition** | Reject conflicting operations (CP) | Users see errors during partitions | Business accepts stale/conflicting data and needs uptime more |
| **Leader-based vs leaderless replication** | Primary + replicas | Write bottleneck, failover pain | Need multi-region writes or always-writable behavior |
| **Sync vs async workflow** | Synchronous call | Timeouts, coupled availability, slow responses | Slow or flaky downstream, spikes, long-running work |
| **Push vs pull (fan-out)** | Fan-out on read | Read latency too high | Reads dominate and follower counts are manageable |
| **Shared vs isolated infrastructure** | Shared DB/queues with tenant ID | Noisy neighbors, security/compliance asks | Large or regulated tenants, SLA per tenant |
| **Exact vs approximate** | Exact counters | Memory/latency cost at huge cardinality | Bounded error acceptable (unique visitors, top-K) |
| **Eager vs lazy computation** | Compute on demand | Repeated expensive reads are slow | Same result requested often, freshness tolerance exists |
| **Hot standby vs cold recovery** | Cold recovery from backups | Recovery too slow for RTO | RTO shrinks, outage cost exceeds idle capacity cost |
| **Centralized vs decentralized coordination** | Single authority (one DB/leader) | SPOF, throughput limits | Need independent operation across partitions or regions |
| **Soft vs hard quotas** | Soft (approximate, fast) | Overuse costs money or harms others | Strict limits needed (paid caps, abuse, safety) |
| **Store-and-forward vs reject** | Fail fast | Lost requests during brief outages | Safe to retain pending work and able to drain backlog |
| **Monolith vs microservices** | Modular monolith | Team/deploy coupling, scaling mismatch | Many teams, independent scaling and release needs |
| **Cache vs no cache** | No cache | DB overload, high latency | Measured hot reads, acceptable staleness |
| **Shard vs single DB** | One DB with replicas + good indexes | Write throughput or storage ceiling | Metrics show hitting limits that tuning cannot fix |
| **Batch vs stream** | Batch (hourly/daily) | Insights too slow | Real-time decisions or alerts required |
| **Build vs buy** | Managed service | Cost or limits at scale, lock-in | Cost at scale exceeds team cost, special requirements |

**More trade-offs to remember**

| Trade-off | One-line version |
|---|---|
| Latency vs throughput | Batching raises throughput but adds delay |
| Cost vs performance | Provisioned vs autoscale vs spot; cache size vs hit rate |
| Scale up vs scale out | Bigger machine is simpler but has a ceiling |
| Availability vs durability | Acking before replication is fast but can lose data |
| Row vs columnar storage | Row for transactions, columnar for analytics scans |
| Precision vs recall | Search/ML tuning: fewer false positives vs fewer misses |
| Short vs long TTL | Freshness vs hit rate and origin load |
| Normalization vs read speed | Consistency vs join cost |
| Safety vs speed in releases | Canary and flags slow releases but cut blast radius |

---

## 25. Production-readiness checklist (use at the end of every design)

A design is not complete when the happy path works. Walk through these 8 points.

| # | Area | Questions to answer |
|---|---|---|
| 1 | **Correctness** | What are the invariants? Transaction boundaries? Idempotency? Ordering scope? Duplicate handling? |
| 2 | **Failure handling** | Timeouts, retries (with backoff + jitter), partial failures, dependency outages, poison messages, recovery path? |
| 3 | **Scale** | Peak load estimate, bottlenecks, partitioning, hot keys, what happens at 10x? |
| 4 | **Data lifecycle** | Retention, archival, deletion (including derived copies), backups, schema changes, restore? |
| 5 | **Security** | Identity, authorization per resource, tenant isolation, encryption, secrets, audit logs? |
| 6 | **Operations** | SLOs, dashboards, actionable alerts, runbooks, capacity thresholds? |
| 7 | **Deployment** | Safe migrations, backward-compatible releases, rollout, rollback, disaster recovery? |
| 8 | **Cost** | Biggest compute, storage, network, replication, and operational costs? |

**Say this at the end of every interview:** "To summarize the main trade-offs, the failure modes I designed for, how we would monitor it, and what I would add next as it grows."

---

## 26. Exercise template and updated study order

### 26.1 For every major design, practice four layers

1. **Baseline:** the simplest correct design that meets today's requirements.
2. **Scale-up (10x):** what breaks first? What do you change (cache, replicas, shard, queue)?
3. **Failure scenario:** pick one (DB failover, region outage, duplicate message, hot key, dependency down). Walk through detection, impact, and recovery.
4. **Trade-off discussion:** name two decisions, the alternatives, and why you chose yours.

### 26.2 Updated study order (Part A + Part B combined)

| Phase | Focus | Topics |
|---|---|---|
| **1. Correctness and distributed systems** | Core theory | Part B sections 1, 2, 3 (isolation, consistency, consensus, clocks) + Part A Patterns 1, 4, 5 |
| **2. Production engineering** | Real-world readiness | Part B sections 15-19 (reliability, security, observability, migrations, multi-region) |
| **3. Missing pattern families** | New designs | Part B sections 5 (KV store), 12 (recommendation), 13 (metering) + Part A Patterns 2, 3, 7 |
| **4. Extra practice problems** | Breadth | Distributed cache, Kafka design, monitoring, recommendation, ad serving, billing (sections 6-8, 12, 23) |
| **5. Interview simulation** | Under pressure | Pick problems, apply the four-layer exercise, time yourself (45-60 min) |
| **Bonus** | Differentiator | Part B section 14 (AI-era), section 4 (event sourcing/CQRS), section 11 (matching engine) |

**If time is short, do these first:** sections 1, 2, 15, 18, and 5.

---

## 27. Updated problem-to-pattern additions

Add these rows to the Part A mapping table.

| Interview problem | Primary pattern | Secondary patterns | Spend the most time on |
|---|---|---|---|
| Distributed key-value store | Part B 5 | Replication, partitioning | Consistent hashing, quorum, hinted handoff, conflict resolution |
| Distributed cache | Part B 6 | Read-heavy (2) | Sharding, eviction, hot keys, stampede |
| Message queue / Kafka | Part B 7 | Write-heavy (3) | Partitions, ISR, offsets, delivery semantics |
| Metrics monitoring | Part B 8 | Write-heavy (3), analytics (14) | Cardinality, downsampling, alerting reliability |
| API gateway / service discovery | Part B 9 | Rate limiting (11) | SPOF avoidance, health checks, staleness |
| Distributed lock service | Part B 10 | Coordination (11) | Leases, fencing tokens, consensus |
| Stock exchange | Part B 11 | Event sourcing (B4) | Single-threaded sequencer, determinism, fairness |
| Online auction | 1 + 7 | Scheduling (12) | Atomic bid, deadline, anti-sniping |
| Recommendation system | Part B 12 | Analytics (14), search (6) | Candidate gen + ranking funnel, feature store |
| Social graph | Part B 12 | Read-heavy (2) | Adjacency storage, celebrities, 2-hop queries |
| Usage metering / quotas | Part B 13 | Write-heavy (3), transactional (5) | Dedup, late events, soft vs hard quotas |
| Subscription billing | 5 + 12 | Metering (B13) | Idempotent charges, proration, dunning |
| LLM gateway | Part B 14 | Rate limiting (11), caching | Token limits, streaming, fallback, cost |
| RAG system | Part B 14 | Search (6) | Chunking, hybrid retrieval, ACLs, evaluation |
| Agent platform | Part B 14 | Async (4), workflows (5) | Durable state, idempotent tools, cost limits |
| Food delivery | 5 + 10 + 1 | Real-time (7) | Order state machine, rider assignment |
| Live streaming | 9 + 2 | Real-time (7) | CDN, latency vs scale, chat fan-out |
| Video conferencing | 7 | Geospatial routing | WebRTC, SFU vs MCU, NAT traversal |
| Email service | 4 + 8 | Search (6), storage (9) | Durable queue, spam pipeline, deliverability |
| Calendar | 12 + 1 | Multi-tenant (13) | Recurrence rules, time zones, conflicts |
| Google Maps | 10 + 2 | Streaming (14) | Tiles + CDN, hierarchical routing, live traffic |
| Content moderation | 4 + 8 | ML serving | Staged pipeline, human review |
| ETL pipeline | 14 + 4 | Scheduling (12) | Idempotent tasks, backfills, quality checks |
| Ad serving | 2 + 3 + 14 | Ranking (B12) | 100 ms auction, click dedup, budget pacing |
| Online judge | 4 | Isolation | Sandboxing, resource limits |
| CI/CD | 4 + 12 | Multi-tenant (13) | Runners, caching, secrets |

---

## 28. Part B revision sheet

**Correctness depth**
- Name the **anomaly** and the **mechanism**: lost update -> atomic update/lock; write skew -> serializable/materialize conflict; double booking -> conditional update/unique constraint.
- **Linearizable** only where truly needed (locks, balances). Eventual elsewhere.
- Quorum: `R + W > N`. Raft: majority, `2f + 1` nodes tolerate `f` failures.
- **Lock with TTL = lease.** Use **fencing tokens**. Prefer constraints and idempotency over locks.
- **Timestamps do not prove causality.** Use logical clocks/versions; LWW can lose writes.
- Event sourcing = events as truth. CQRS = separate read models (eventually consistent).

**Infrastructure designs in one line**
- **KV store:** consistent hashing + vnodes, N/W/R quorum, hinted handoff, read repair, Merkle trees, gossip.
- **Cache:** shard, evict (LRU/LFU), fix hot keys and stampedes, protect DB when cold.
- **Kafka:** partitioned replicated append-only log, ISR, `acks=all`, offsets, consumer groups, per-partition order.
- **Monitoring:** watch cardinality, downsample, alert on symptoms, monitor the monitor.
- **Matching engine:** single thread per symbol, sequencer + event log, deterministic replay.

**Production engineering**
- **SLO math:** 99.9% = 43 min/month. Dependencies in series multiply.
- **Overload:** timeouts + deadlines, backoff + jitter, circuit breakers, bulkheads, load shedding, bounded queues, graceful degradation. Retry at one layer only.
- **Security:** AuthN vs AuthZ, resource-level checks (IDOR), PKCE, short-lived tokens, mTLS between services, envelope encryption, audit logs.
- **Observability:** RED/USE, SLO burn-rate alerts, runbooks, mitigate first then root-cause.
- **Migrations:** expand -> dual write -> backfill -> verify -> switch reads -> contract. CDC, canary, feature flags, rollback.
- **Multi-region:** explain the **data** story (home region, single leader, multi-leader, global consensus), RPO/RTO, failover and failback, test DR.

**Newer topics**
- **Recommendation:** candidate generation -> ranking -> re-ranking; feature store; cold start; A/B tests.
- **Metering:** unique event IDs, dedup, late events, adjustments not edits; soft vs hard quotas.
- **AI systems:** token-based limits, streaming, semantic cache, fallback routing; RAG = chunk + hybrid retrieve + ACL filter + rerank + evaluate; agents = durable state + idempotent tools + cost limits.

**Always finish with:** correctness, failure handling, scale, data lifecycle, security, operations, deployment, cost.

> **The real goal:** recognize the workload, select the right patterns, justify the trade-offs, and explain how the design behaves when components fail or load grows 10x.

