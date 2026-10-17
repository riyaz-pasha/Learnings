# HLD System Design Interview Master Guide
### Patterns, Problems, Trade-offs, and Interview Talking Points

You do not need to solve 100 different system design problems.
You need to master about **15 reusable patterns**, know the **core challenge** of each, and learn how to **combine** them.

Example: Movie booking, hotel booking, flight booking, and ride allocation look different. But all four are "many users fight for one scarce resource". Same pattern, same core answer.

Example: Uber = geospatial (10) + resource allocation (1) + real-time (7) + write-heavy location updates (3).

---

## Table of Contents

1. How to use this guide
2. The interview framework
3. Master classification (15 patterns)
4. The 15 patterns in detail
5. Reusable sub-patterns (building blocks of answers)
6. Cross-cutting trade-off matrix
7. Problem to pattern mapping cheat sheet
8. Core components you must know
9. Estimation numbers
10. Study plan and how to practice
11. Common mistakes
12. One-page revision sheet

---

## 1. How to use this guide

- First learn the **pattern**, not the product.
- For each pattern, remember: **core challenge, 3 key moves, 3 trade-offs, 2 failure scenarios, interviewer probes**.
- Priority: patterns **1-5 first**, then **6-10**, then **11-15**.
- Technologies (Redis, Kafka, Postgres, DynamoDB, Elasticsearch, S3) are **choices you make after** you identify the pattern.

---

## 2. The interview framework (use it every time)

| Step | Time | What to do |
|---|---|---|
| 1. Requirements | 3-5 min | Functional, non-functional (scale, latency, availability vs consistency), out of scope |
| 2. Estimation | 3-5 min | QPS, read:write ratio, storage, bandwidth, peak |
| 3. API + data model | 5-10 min | Main entities, keys, indexes, constraints, state transitions |
| 4. High-level design | 10 min | Client, LB, services, DB, cache, queue, storage, CDN |
| 5. Deep dive | 10-15 min | The hardest part (the pattern's core challenge) |
| 6. Failure + recovery | 5 min | Timeouts, retries, duplicates, partial failure, stale cache, failover |
| 7. Trade-offs + evolution | 5 min | What you gave up, bottleneck, what you'd add next and when |

### The 8 questions to ask before designing

1. **What must never go wrong?** Double booking? Lost payment? Data leak? Missed event?
2. **Read/write ratio?** Where is the bottleneck?
3. **Latency target?** Tens of ms, or can we return a job ID?
4. **Freshness needed?** Is a few seconds of staleness OK?
5. **Failure contract?** Can work be retried? Can the user see "pending"? Can data be rebuilt?
6. **Scale?** Peak RPS, object size, retention, concurrency, hot keys.
7. **Ordering scope?** Global, per user, per order, per conversation, or none?
8. **Operational constraints?** Availability, cost, RPO/RTO, regions, security, team size.

> **Golden rule:** Say your trade-offs out loud. Interviewers score the **"why"**, not the **"what"**.
> **Second rule:** Start with the simplest design that preserves correctness. Add a component only when you can name the requirement it solves.

---

## 3. Master classification (15 patterns)

| # | Pattern | Tag | Example problems |
|---|---|---|---|
| 1 | Scarce resource allocation | Double booking | Movie tickets, hotel, flight, ride allocation, appointments, flash sale |
| 2 | Read-heavy serving | Read-optimized | Product catalog, news feed, URL shortener redirects, profile service |
| 3 | Write-heavy ingestion | Write-optimized | Metrics, logs, clickstream, IoT, GPS updates |
| 4 | Async background processing | Background work | Report export, video transcoding, email, image resize |
| 5 | Transactional workflows | Consistency | Payments, orders, wallet, checkout, booking + payment |
| 6 | Search and discovery | Indexing | Autocomplete, e-commerce search, job search |
| 7 | Real-time communication | Low latency | Chat, presence, live comments, collaborative editing |
| 8 | Event-driven distribution | Fan-out | Notification platform, webhooks, CDC, activity streams |
| 9 | File and media delivery | Large objects | Dropbox, Instagram upload, YouTube/Netflix |
| 10 | Geospatial | Location | Uber, nearby friends/places, delivery tracking |
| 11 | Coordination primitives and rate limiting | Traffic control | Rate limiter, ID generator, distributed cache, leader election |
| 12 | Distributed scheduling | Time and leases | Cron service, delayed jobs, reminders, web crawler |
| 13 | Multi-tenant SaaS | Isolation | CRM, project tools, enterprise analytics |
| 14 | Analytics and aggregation | Batch + stream | Top-K, view counts, dashboards, ad click aggregation |
| 15 | Highly available lookup services | Resilience | URL shortener, config service, feature flags, DNS-like |

---

## 4. The 15 patterns in detail

---

### Pattern 1: Scarce resource allocation (double booking)

**Problems:** Movie ticket booking (BookMyShow), hotel reservation, flight booking, ride allocation, doctor appointment, restaurant table, flash sale, parking, meeting room.

| Problem | Resource | Special challenge |
|---|---|---|
| Movie tickets | Individual seat | Seat map, concurrent holds |
| Hotel | Room inventory by date | Date-range overlap |
| Flight | Seat / fare class | Multi-leg, fare buckets |
| Ride allocation | Available driver | Location, offers, driver acceptance |
| Appointment | Time slot | Slot uniqueness, cancellation |
| Flash sale | Limited stock | Extreme contention, overselling |

**Core challenge:** Two or more users want the same limited resource at nearly the same time. Only one can win. Correctness comes first.

**Architecture**

```
Client -> API Gateway -> Booking Service
                              |
                  DB transaction + concurrency control   (source of truth)
                              |
          +-------------------+--------------------+
          |                                        |
   Expiration worker                       Events (async)
   (release expired holds)                 (email, analytics, search)
```

**Key design moves**

- **Atomic conditional update** (simple and strong):
  ```sql
  UPDATE seats
  SET status='HELD', hold_expires_at=:exp, reservation_id=:rid
  WHERE seat_id=:sid AND status='AVAILABLE';
  -- check affected rows == 1, else someone else won
  ```
- **Unique constraint:** `UNIQUE(show_id, seat_id)` on confirmed bookings. The DB enforces the invariant itself.
- **Pessimistic lock:** `SELECT ... FOR UPDATE`. Safe, but lower throughput under contention.
- **Optimistic lock:** version column, `UPDATE ... WHERE version = X`. Good when conflicts are rare.
- **Temporary hold with expiry:** `AVAILABLE -> HELD (5-10 min) -> BOOKED`. Persist `expires_at`. A worker releases expired holds. Redis TTL alone is not enough.
- **Distributed lock (Redis/ZooKeeper):** only when the resource is not inside one DB. Use fencing tokens.
- **Idempotency key** on the booking request so client retries do not double book.
- **Virtual waiting room / queue** for flash sales so the DB does not melt.
- **Hotel date ranges:** one row per `(room_type, date)` with a count, or an exclusion constraint on date ranges (Postgres `EXCLUDE USING gist`).

**The dangerous bug to explain**

1. Read seat as `AVAILABLE`.
2. Check in app code.
3. Update to `BOOKED`.

Two requests can both read `AVAILABLE` before either writes. Fix with the atomic conditional write above.

**Trade-offs**

| Decision | Choose | Why | Cost |
|---|---|---|---|
| Database | Relational | Constraints + transactions make correctness easy | Harder to scale writes (shard by show/hotel) |
| Locking | Optimistic if low contention, pessimistic if high | Match contention level | Retries vs lower throughput |
| Cache | For showing availability only, never as final authority | Cached availability can be stale | UI may show a seat that is gone |
| Sync vs async | Reserve **sync**, side effects **async** | User needs a definite answer now | Eventual consistency for emails/analytics |
| Expiration | Persist `expires_at` + cleanup worker | TTL alone cannot guarantee correct state | Extra worker |
| Partitioning | By `show_id` / `hotel_id` | Keeps contention inside one shard | Hot shard for a blockbuster show |
| Redis lock vs DB constraint | DB constraint when possible | Lock is not a substitute for a constraint | - |

**Ride allocation is slightly different:** one driver can be *offered* to several riders depending on strategy, but only one offer can become an active assignment. Needs geo index, offer timeout, and a driver state machine (`AVAILABLE -> OFFERED -> ASSIGNED`).

**What interviewers probe**

- How do you prevent double booking?
- Payment succeeds but booking confirmation fails. What now?
- Hold expires while payment is in progress?
- Can you use Redis locks instead of DB transactions?
- 100,000 users, 1,000 tickets. How?
- Expiration worker processes the same event twice?

**Interview points:** CP over AP here. DB is the source of truth. Explain the state machine and idempotency. Mention reconciliation for the payment gap (see Pattern 5).

---

### Pattern 2: Read-heavy, read-optimized systems

**Problems:** URL shortener, news feed / timeline reads, product catalog, product detail pages, news website, profile service, config/feature flags, leaderboard reads, reference data.

**Core challenge:** Reads are 100x or more than writes. Serve reads with low latency, keep data reasonably fresh, control cost.

**Architecture**

```
Client -> CDN / edge cache -> API + Redis cache -> Read replicas -> Primary DB
```
Use only the layers that fit the freshness, cost, and consistency needs.

**Key design moves**

- **Layered caching:** browser -> CDN -> app cache (Redis/Memcached) -> DB.
- **Cache patterns:** cache-aside (default), read-through, write-through, write-behind.
- **Read replicas** for DB read scale.
- **Denormalization** and precomputed views.
- **Cache failure modes:**
  - *Stampede* (many misses at once): request coalescing, locks, jittered TTL.
  - *Penetration* (keys that don't exist): cache nulls, Bloom filter.
  - *Hot key*: local in-process cache, key replication.
- **Cursor pagination** for large changing lists.
- **Sharding by key** when data outgrows one node.

**Trade-offs**

| Decision | Choose when | Cost |
|---|---|---|
| CDN | Public cacheable content, global users | Invalidation; unsafe for private data without correct cache keys |
| Redis | Repeated reads overload DB | Memory, invalidation, staleness |
| Read replicas | Reads exceed one primary | Replication lag (stale reads) |
| Precompute | Same expensive result requested again and again | Extra storage and update work |
| Denormalize | Read latency matters more | Write amplification, consistency work |
| Eventual consistency | Small staleness OK | Stale results |
| Cursor vs offset pagination | Big, changing data | More complex cursor semantics |
| Cache-aside vs write-through | Cache-aside is flexible | Misses and invalidation races remain |

**Feed generation (classic discussion)**

| Approach | How | Good when | Downside |
|---|---|---|---|
| **Fan-out on write (push)** | Precompute follower feeds at post time | Reads are frequent, followers manageable | Celebrity = huge write amplification |
| **Fan-out on read (pull)** | Build feed when user opens app | Writes expensive, few follows | Slow, heavy reads |
| **Hybrid** | Push for normal users, pull and merge celebrities at read time | Large social networks | More complex |

**URL shortener extras:** key generation (counter + base62, pre-generated key ranges, or hash with collision check), 301 vs 302 redirect (301 is cached by browsers, 302 lets you count clicks), hot-key handling, expiry. Redirect = read-heavy, creation = write path. Two traffic patterns in one problem.

**What interviewers probe:** Cache invalidation strategy. What happens when cache is down? Celebrity problem. Replica lag and read-your-own-writes.

**Interview points:** Default to **AP / eventual consistency** unless told otherwise. Cache means a second copy of data, so you must discuss consistency.

---

### Pattern 3: Write-heavy systems and high-volume ingestion

**Problems:** Metrics/time-series, logs/observability, clickstream, ad impressions, IoT telemetry, audit logs, GPS updates, high-volume likes/views/counters, market data feeds.

**Core challenge:** Events arrive faster than storage can absorb. Absorb bursts, distribute writes, decide what must be durable now vs processed later.

**Architecture**

```
Producers / SDKs
   -> Ingestion API (validate, auth, quotas)
   -> Durable log / partitioned stream (Kafka)
        -> Raw storage (object store / data lake)
        -> Stream processors (aggregates, alerts)
        -> Serving store (LSM DB / time-series / columnar)
```

**Key design moves**

- **Buffer with a durable log (Kafka)** between producers and storage. It absorbs spikes.
- **LSM-tree databases** (Cassandra, ScyllaDB, HBase, RocksDB): append-only, fast writes.
- **Batching and compression** of writes.
- **Partition by key** (device_id, user_id) or by time.
- **Append-only** design, avoid in-place updates.
- **Pre-aggregate** in a stream processor before storing.
- **Tiered storage:** hot (SSD) -> warm -> cold (S3), with retention/TTL.
- **Sampling** where exact data is not needed.
- **Hot partition fix:** salting the key, sub-partitioning.

**Trade-offs**

| Decision | Gain | Cost |
|---|---|---|
| Queue/log vs direct DB write | Absorbs bursts | Extra component, processing delay |
| Ack after queue accepts | Fast response | "Accepted" is not "fully processed" |
| LSM DB | Fast writes | Slower reads, compaction cost |
| Batching | Efficiency | Buffer latency, larger retry batches |
| Exact vs approximate counters | Speed, memory | Inaccuracy, reconciliation |
| Short retention | Cost | Old queries become expensive |
| Columnar + compression | Cheap analytics | CPU, format complexity |

**Critical questions to answer**

1. Can ingestion absorb a 10x spike?
2. What if consumers are slower than producers (backpressure, queue growth)?
3. Can events arrive twice? (Yes, so dedupe or make idempotent.)
4. What ordering is needed and at what scope?
5. What data loss is acceptable during a crash?
6. How do you replay events and rebuild aggregates?

> A queue does not automatically make writes reliable. Durability depends on ack settings, replication, producer retries, and storage config. Consumers must handle redelivery.

**Interview points:** "We can lose a few metrics, but never lose a payment." Use that to justify weaker guarantees here.

---

### Pattern 4: Asynchronous processing and background jobs

**Problems:** Report generation, video transcoding, image resizing, email/SMS delivery, bulk import, document generation, search indexing, data export, webhook delivery.

**Core challenge:** The work is slow, expensive, or retryable, and does not need to finish before the API responds. The hard part is not "add a queue". It is **job state, retries, duplicates, failures, and eventual completion**.

**Architecture**

```
Client -> API -> persist job record + enqueue durable work
                          |
                       Queue / broker
                          |
                       Workers -> output storage
                          |
        Job status API / completion notification (poll or push)
```

**Job states:** `PENDING -> RUNNING -> SUCCEEDED`, plus `RETRY_WAIT`, `FAILED`, `CANCELLED`.

**Key design moves**

- Return **202 Accepted + job ID**. Client polls or gets a push.
- **At-least-once delivery + idempotent workers** = effectively-once.
- **Retries with exponential backoff + jitter**, bounded, then **DLQ**.
- **Visibility timeout / lease** so a crashed worker's job is picked up again.
- **Chunk big jobs** into bounded units (retryable, parallel).
- **Separate worker pools** so CPU-heavy jobs do not starve latency-sensitive ones.

**The DB + queue problem (very common probe)**

- DB first, then publish: DB commits, broker is down. Order exists, nobody hears about it.
- Publish first, then DB: event published, DB fails. Consumers process a non-existent order.

**Fix: Transactional Outbox**

1. In **one DB transaction**, write the business row **and** an outbox row.
2. A relay reads committed outbox rows and publishes them.
3. Mark published only after successful delivery (retry-safe).
4. Make consumers idempotent.

This gives reliable eventual processing, not magical exactly-once.

**Trade-offs**

| Decision | Recommended | Why | Cost |
|---|---|---|---|
| Sync vs async | Async for slow/non-essential work | Avoids timeouts, decouples scaling | Pending state, complexity |
| At-most vs at-least once | At-least-once + idempotent | Never lose work | Duplicates possible |
| Retry vs DLQ | Bounded retries then DLQ | Poison messages cannot loop forever | Need DLQ handling |
| Poll vs push for status | Poll (simple), push (timely) | - | Wasted requests vs connection complexity |
| One big job vs chunks | Chunks | Retryability, parallelism | Coordination |

**Interview points:** Export, transcoding, notifications, bulk work -> think queue, job state, retries, idempotency, DLQ, observability.

---

### Pattern 5: Transactional workflows and financial correctness

**Problems:** Payments, e-commerce checkout, wallet transfer, bank transfer, subscription billing, refunds/chargebacks, loyalty points, travel booking (flight + hotel + cab), double-entry ledger.

**Core challenge:** Keep **business invariants** across multiple state changes or services, even when any step fails. You usually cannot use one ACID transaction across services.

Booking asks: "Who gets this resource?"
Workflow asks: "How do all related operations reach a valid state if any step fails?"

**Architecture**

```
Order Service -> Workflow / Saga coordinator
                    |-> Inventory: reserve stock
                    |-> Payments: authorize / capture
                    |-> Shipping: create shipment
                    |-> Notifications: confirm
Each service commits a local transaction. Events + explicit states connect them.
```

**Key design moves**

- **Saga:** series of local transactions with **compensating actions** (undo steps). Two styles: orchestration (central coordinator) and choreography (event reactions).
- **Outbox pattern** (see Pattern 4).
- **Idempotency keys** at every API and consumer. Enforce uniqueness at the right boundary.
- **State machine** per entity: `CREATED -> PAID -> SHIPPED ...`
- **Double-entry ledger:** every debit has a matching credit, entries are immutable.
- **Reconciliation jobs** against banks / payment providers.
- **Webhooks + status polling** to resolve unknown payment outcomes.
- **2PC** exists but is rarely preferred (blocking, coordinator is a SPOF, poor scale).
- If both account entries live in **one DB**, one transaction is enough. You do not automatically need Kafka or microservices for a wallet transfer.

**Five rules for payments**

1. A **timeout does not mean failure**. The provider may have processed it.
2. Use a stable **idempotency key** per logical payment.
3. Model uncertain states explicitly (`PENDING`, `UNKNOWN`).
4. Use **webhooks and reconciliation** to resolve unclear outcomes.
5. Refunds and reversals are **new operations**, not erasing history.

**Trade-offs**

| Decision | Choose based on | Consequence |
|---|---|---|
| Single DB txn vs Saga | One DB boundary or many services | Saga needs compensation and recovery logic |
| 2PC vs eventual consistency | Need atomicity vs availability | 2PC blocks; Saga has no global atomicity |
| Orchestration vs choreography | Visibility vs loose coupling | Choreography is hard to trace |
| Auth vs capture | Can fulfillment fail after payment? | Different reversal semantics |
| "Exactly once" claim vs idempotency | Real-world effects must not duplicate | Use stable IDs + provider idempotency + reconciliation |
| Ledger vs mutable balance | Auditability | Ledger lets you rebuild and reconcile |
| Immediate vs eventual consistency | Which invariant must hold now | Never defer a safety-critical invariant just for latency |

**What interviewers probe:** "Payment succeeded but response timed out?" Answer: check status by idempotency key, use webhook, reconcile. Also: partial failure in the middle of a saga, duplicate webhook, refund after partial shipment.

---

### Pattern 6: Search, autocomplete, and discovery

**Problems:** E-commerce search, job search, restaurant discovery, autocomplete/typeahead, document search, log search, product filters.

**Core challenge:** Transactional DBs are not ideal for full-text relevance, typo tolerance, facets, and ranking.

**Architecture**

```
Client -> Search API -> query/ranking -> Search index -> doc IDs -> DB (source of truth)
Primary DB --(CDC / Kafka)--> indexer --> Search index
```

**Key design moves**

- **Inverted index** (term -> list of docs): Elasticsearch / Lucene / Solr.
- Search index is a **derived copy**. The primary DB stays the source of truth. Sync via CDC or events.
- **Autocomplete:** trie or prefix index, **top-K precomputed per prefix**, cache popular prefixes, client-side debounce.
- **Ranking:** BM25/TF-IDF plus popularity, freshness, personalization.
- **Sharding:** by document (common) vs by term.
- **Deep pagination:** use search-after / cursor.

**Trade-offs**

| Decision | Trade-off |
|---|---|
| Postgres full-text vs dedicated search engine | Simplicity + transactional freshness vs rich search + scale |
| Sync vs async indexing | Immediate visibility vs lower write latency (eventual consistency) |
| Prefix index vs trie vs search engine | Simplicity vs specialized speed |
| Exact vs fuzzy matching | Speed/precision vs typo tolerance |
| Precomputed candidates vs live compute | Predictable latency vs freshness |
| Index every field vs selective | Flexible queries vs storage + write amplification |

> Discovery is not commitment. Finding an available driver or product does **not** reserve it.

---

### Pattern 7: Real-time communication, fan-out, and collaboration

**Problems:** Chat (WhatsApp/Slack), presence ("online"), live comments, live scores, stock streaming, multiplayer state, Google Docs, Figma.

**Core challenge:** One event must reach many users quickly. Connections are long-lived and **stateful**.

**Key design moves**

- **Protocol:** WebSocket (bi-directional), SSE (server -> client), long polling (fallback).
- **Connection registry:** user -> gateway server (Redis).
- **Pub/Sub or Kafka** to route messages between gateway servers.
- **Durable message store.** WebSocket delivery alone is not storage.
- **Offline users:** store, push via APNs/FCM, deliver on reconnect with ack + sequence numbers.
- **Ordering:** per-conversation sequence IDs (global ordering is too expensive).
- **Delivery states:** *Sent* (server accepted) -> *Delivered* (device received) -> *Read* (user viewed). Not interchangeable.
- **Group chat:** fan-out limits, large groups handled differently.
- **Collaboration:** OT (operational transform) or CRDTs, version vectors, periodic snapshots + operation log.

**Trade-offs**

| Decision | Trade-off |
|---|---|
| WebSocket vs polling | Real-time, bi-directional vs simple stateless infra |
| SSE vs WebSocket | Simple one-way streaming vs two-way |
| Presence in Redis vs DB | Fast ephemeral vs durable/queryable |
| Per-conversation vs global order | Scalable vs expensive coordination |
| Durable storage vs ephemeral delivery | History + recovery vs lower cost/latency |
| Fan-out on write vs read | Fast delivery vs write amplification |
| OT vs CRDT | Needs central server, mature vs decentralized, more metadata |
| Last-write-wins vs merge | Simple but lossy vs complex but safe |

**What interviewers probe:** Reconnection storms, sticky routing and load balancing for WebSockets, message ordering, duplicate delivery, offline messages, multi-device sync.

---

### Pattern 8: Event-driven distribution and notification systems

**Problems:** Notification platform, email/SMS/push delivery, webhook service, audit event distribution, CDC, activity streams.

**Core challenge:** Reliably deliver an event to the right consumers, who run at different speeds and fail in different ways.

**Key design moves**

- **Queue vs pub/sub:** competing consumers for work vs many independent subscribers.
- **Kafka-style log:** replay + ordered partitions. **Task queue (SQS/RabbitMQ):** simple ack/retry.
- **At-least-once + dedup** at consumers.
- **Per-key ordering** (partition by entity ID) instead of global ordering.
- **Separate topics** for isolation and independent retention.
- **DLQ** for poison messages, **rate limiting** per channel/provider.
- **User preferences, templates, dedup window, quiet hours** for notifications.

**Webhook system (reusable example)**

1. Persist the event / delivery intent.
2. Select eligible subscribers.
3. Create durable delivery tasks.
4. Deliver with timeouts and bounded retries (exponential backoff).
5. Sign payloads (HMAC) so receivers can verify.
6. Store attempt history and status.
7. Provide replay, DLQ inspection, per-subscriber rate limits.

> Never let a slow webhook receiver block your core business transaction.

**Trade-offs**

| Decision | Trade-off |
|---|---|
| Queue vs pub/sub | Work distribution vs multi-subscriber delivery |
| Kafka vs task queue | Replay + ordering vs simpler retry model |
| Push vs pull consumers | Low latency vs consumer control |
| At-most vs at-least once | No duplicates vs no loss |
| Shared vs separate topics | Simplicity vs isolation |
| Inline retry vs DLQ | Fast recovery vs protecting the main pipeline |

---

### Pattern 9: File storage, media processing, and CDN delivery

**Problems:** Dropbox/Google Drive, Instagram/photo upload, YouTube/Netflix, WhatsApp media, document upload, Zoom recordings.

**Core challenge:** Big binary data. Bandwidth and storage dominate. Do not push big blobs through your app servers.

**Architecture**

```
Client -> API (authz + metadata) -> pre-signed URL
Client -> Object storage (S3) directly
Upload event -> Queue -> Processing workers (transcode/resize) -> Object storage
Client -> CDN -> cached media
```

**Key design moves**

- **Object storage (S3)** for blobs, **metadata in a DB**.
- **Pre-signed URLs**: direct client upload/download.
- **Multipart / chunked / resumable upload.** Validate size, checksum, object state at completion.
- **Dedup by content hash** (Dropbox style).
- **Async pipeline:** upload -> queue -> transcode into multiple resolutions -> store -> CDN.
- **Adaptive bitrate streaming (HLS/DASH):** segments, manifests, transcoding ladder.
- **File sync:** sync only changed blocks, versioning, conflict resolution.
- **Immutable versions** make caching safe.

**Trade-offs**

| Decision | Trade-off |
|---|---|
| App proxy vs pre-signed upload | Central control vs lower bandwidth/server load |
| Object storage vs DB BLOBs | Scale + cost vs transactional simplicity |
| CDN vs origin only | Low latency + origin relief vs cache cost + invalidation |
| Multipart vs single upload | Resumable, high throughput vs more upload state |
| Sync vs async transcoding | Immediate vs responsive upload + scalable workers |
| Eager vs lazy transformation | Fast delivery vs storage/processing cost |
| Metadata in SQL vs only in object store | Rich queries vs simplicity |

**Interview traps**

- Pre-signed URL must not give access to the wrong object.
- CDN must not leak private content across users/tenants.
- Processing must be idempotent (upload events can be redelivered).
- Large downloads need range requests, cache headers, access control.

---

### Pattern 10: Geospatial systems and location tracking

**Problems:** Uber/Ola, food delivery partner assignment, nearby friends/places (Yelp), driver tracking, fleet management, geofencing, Tinder.

**Core challenge:** A rapidly changing spatial dataset. "Which eligible drivers are close to this pickup *right now*?"

**Architecture**

```
Driver devices -> Location ingestion -> Geospatial index (in-memory)
Rider request -> Candidate discovery -> Matching service -> Atomic assignment
```

**Key design moves**

- **Spatial index:** Geohash, Quadtree, S2 (Google), H3 (Uber). Pick one and explain it.
- **In-memory store** (Redis GEO or custom) for the **latest** location only. Keep history separately only if required.
- **Location updates = write-heavy** (Pattern 3). Accept slightly stale data. Throttle updates.
- **Matching:** nearby cells -> filter eligibility -> rank by ETA/rating -> send offer -> timeout -> next driver.
- **Atomic assignment** uses Pattern 1 (driver state change).
- **Stale location detection**, driver availability transitions, fallback when nobody accepts.

**Trade-offs**

| Decision | Trade-off |
|---|---|
| Geohash vs H3 vs R-tree | Different query and partition properties |
| Update frequency | Fresh locations vs battery/network/server load |
| In-memory index vs DB | Latency vs durability + ops complexity |
| Push vs poll locations | Timeliness vs traffic |
| Exact vs approximate nearest | Precision vs bounded cost |
| History retention | Analytics/audit vs storage + privacy |
| Cell size | Precision vs number of cells to search |

> Proximity is **candidate selection**, not proof the driver is still available.

---

### Pattern 11: Coordination primitives and rate limiting

**Problems:** Rate limiter, API gateway throttling, login/OTP abuse protection, tenant quotas, unique ID generator (Snowflake), distributed lock service, leader election, distributed cache, consistent hashing, circuit breaker.

**Core challenge:** Build the building blocks other systems rely on.

**Rate limiting algorithms**

| Algorithm | Idea | Note |
|---|---|---|
| Fixed window | Count per time window | Simple, but burst at window boundary |
| Sliding window log | Store timestamps | Precise, more memory |
| Sliding window counter | Weighted counts of two windows | Good approximation |
| Token bucket | Bucket capacity B, refill rate r | Allows bursts up to B, sustained rate about r per second |
| Leaky bucket | Constant outflow | Smooths traffic |

- Store counters in **Redis** with atomic ops or Lua scripts.
- **Local counters:** fast and available but approximate. **Central counter:** accurate but adds latency and a dependency.
- **Fail-open vs fail-closed** when the limiter is down. Public API: usually fail-open. Auth/security: fail-closed.
- Key by **IP vs user vs tenant**. Authenticated keys are fairer. For OTP: per-destination quota + cooldown + abuse detection.

**Other primitives**

- **Consistent hashing + virtual nodes:** minimal data movement when nodes change.
- **Snowflake ID:** timestamp + machine ID + sequence. Roughly sortable, no coordination.
- **Leader election:** ZooKeeper / etcd / Raft.
- **Cache design:** eviction (LRU/LFU), replication, hot keys.

**Trade-offs:** Accuracy vs memory vs latency. Centralized vs local counters. Fail-open vs fail-closed.

---

### Pattern 12: Distributed scheduling, delayed jobs, and leases

**Problems:** Distributed cron, reminders, delayed messages, workflow timers (Airflow), recurring billing, scheduled exports, delayed payment retries, web crawler.

**Core challenge:** Run work at the right time, at-least-once (or close to exactly-once), and survive crashes.

**Key design moves**

- **Persist the schedule.** Do not rely on in-memory timers alone.
- **Time-bucketed storage** indexed by next run time. Poll upcoming buckets, or use a delay queue / Redis ZSET.
- **Worker claims a job with a lease.** If it dies, the lease expires and another worker picks it up.
- **Idempotent jobs.** At-least-once is easier than exactly-once.
- **Priority queues, retries, DLQ.**
- **Crawler:** URL frontier, per-host politeness, dedup (Bloom filter / simhash), robots.txt.

**Lock vs lease (important)**

Worker A takes a lease, pauses (GC, network), the lease expires. Worker B takes the job and runs it. Worker A wakes up and also runs it. Fix: **fencing tokens** (monotonic version checked at the resource), plus idempotency.

**Trade-offs**

| Decision | Trade-off |
|---|---|
| Poll DB vs delayed queue | Simple + durable vs specialized semantics |
| Distributed lock vs DB claim | Abstraction vs transactional state + fencing |
| At-most vs at-least once | Fewer duplicates vs crash recovery |
| Central vs partitioned scheduler | Simplicity vs scale + fault isolation |
| Lease expiry vs explicit release | Recovery vs stale owner risk |
| Time wheel vs priority queue | Efficient large-scale timers vs complexity |

---

### Pattern 13: Multi-tenant SaaS architecture

**Problems:** CRM, project management tool, enterprise analytics, shared reporting platform, multi-organization APIs.

**Core challenge:** Many customers share the platform. Isolation, fairness, and security matter.

**Trade-offs**

| Decision | Trade-off |
|---|---|
| Shared DB + tables vs DB per tenant | Low cost vs strong isolation |
| Shared schema vs schema per tenant | Ops simplicity vs per-tenant migration/isolation |
| Tenant ID in partition key vs dedicated partitions | Distribution vs hot-tenant risk |
| Shared cache vs tenant-scoped keys | Reuse vs leak risk |
| Shared worker pool vs per-tenant pools | Utilization vs noisy neighbor |
| Global vs per-tenant quotas | Simplicity vs fairness |
| Shared vs per-tenant encryption keys | Easy management vs stronger isolation |

**Non-negotiables**

- Enforce tenant authorization at **every** data access boundary.
- Tenant scope in **cache keys and query filters**.
- No cross-tenant leakage in search indexes, object storage, exports, logs.
- Quotas and noisy-neighbor protection (bulkheads).
- Tenant-aware routing, migrations, backups, audit logs.

> When you hear "enterprise", "organizations", "tenants", "customer isolation": tenant ID is **not just another column**. It affects authorization, partitioning, caching, observability, and operations.

---

### Pattern 14: Analytics, aggregation, and counting at scale

**Problems:** Top-K (trending hashtags, top songs), view counts, ad click aggregation, leaderboard, real-time dashboards, unique visitors, fraud analytics, BI reporting.

**Core challenge:** Aggregate huge event streams, sometimes in real time, without hurting the transactional system.

**Architecture**

```
App events / CDC -> durable stream or batch ingestion -> processing -> analytical store -> dashboard API
```

**Key design moves**

- Keep **transactional path** and **analytical path** separate. A big dashboard query should not compete with checkout on the same DB.
- **OLTP vs OLAP:** use a columnar/warehouse store for scans and aggregations.
- **Lambda** (batch + stream) vs **Kappa** (stream only).
- **Stream processing** (Flink/Spark) with tumbling / sliding windows.
- **Approximate algorithms:** Count-Min Sketch (frequency), HyperLogLog (unique counts), Bloom filter (membership).
- **Leaderboard:** Redis sorted sets. Think about ties and rank consistency.
- **Sharded counters** to avoid hot rows. Batch writes.
- **Late and duplicate events:** event time vs processing time, watermarks, dedup.
- **Backfills and rebuilds** of derived tables.

**Trade-offs**

| Decision | Trade-off |
|---|---|
| Batch vs stream | Simple/cheap vs low latency |
| Precomputed vs query-time aggregation | Fast dashboards vs freshness + update cost |
| Exact vs approximate | Accuracy vs memory/compute |
| Event-time vs processing-time windows | Correct late data vs buffering complexity |
| Raw retention vs rollups | Flexibility vs cost |
| CDC vs app events | Captures committed changes vs less business context |

---

### Pattern 15: Highly available lookup, configuration, and key-value services

**Problems:** URL shortener redirect path, feature flags, service configuration, DNS-like lookup, global reference data.

**Core challenge:** Very fast, always-available reads with safe propagation of changes.

**Reason about**

- Availability target, **RTO** (time to recover), **RPO** (acceptable data loss).
- Replication lag and failover behavior.
- Idempotent writes and conflict resolution.
- Backups and **restore testing**.
- Hot keys, partitioning, regional routing.
- **Graceful degradation** when dependencies fail (serve stale, safe defaults).

**Trade-offs**

| Decision | Trade-off |
|---|---|
| Single primary vs multi-region | Simple consistency vs regional resilience + local latency |
| Active-passive vs active-active | Easy conflicts vs availability + write flexibility |
| Strong vs eventual consistency | Global agreement vs latency + partition tolerance |
| SQL vs key-value | Flexible queries vs simple high throughput |
| Replication vs sharding | Availability/read scale vs write scale + routing complexity |
| Cache vs authoritative lookup | Low latency vs staleness |
| Automatic vs manual failover | Fast recovery vs split-brain / false failover risk |

**Consistency in replication:** quorum `R + W > N` gives strong reads in leaderless systems. Know leader-follower, multi-leader, leaderless.

---

## 5. Reusable sub-patterns (building blocks of answers)

Learn these once. Reuse them everywhere.

| Sub-pattern | Used in | Must-know points |
|---|---|---|
| **Idempotency** | Payments, booking, order creation | Key scope, retention window, concurrent duplicate requests, return the same result |
| **Transactional outbox** | Any "DB write + publish event" | Same txn for row + event, relay publishes, consumers idempotent |
| **Saga** | Cross-service workflows | Compensation, orchestration vs choreography |
| **Hot-key handling** | Viral posts, counters, flash sales | Salting/sharding, local cache, batching, aggregation |
| **Fan-out** | Feeds, notifications | Write vs read vs hybrid, backpressure |
| **Backpressure** | Streaming, queues, ingestion | Queue limits, admission control, load shedding |
| **Deduplication** | Events, imports, webhooks | Stable IDs, retention, idempotent side effects |
| **Distributed lock / lease** | Schedulers, singleton jobs | Lease expiry, fencing tokens, crash recovery |
| **Optimistic concurrency** | Docs, inventory, APIs | Version column, ETags, conflict resolution |
| **CDC** | Search indexing, analytics | Ordering, duplicates, schema changes |
| **Circuit breaker** | Service dependencies | Thresholds, half-open recovery, fallback |
| **Bulkhead** | Multi-tenant, worker pools | Resource pools, noisy-neighbor isolation |
| **Retry with backoff + jitter** | Everything with remote calls | Bounded retries, avoid retry storms |
| **Reconciliation** | Payments, inventory, workflows | Source of truth, periodic scans, repair actions |
| **Leaderboard** | Games, rankings | Sorted sets, ties, hot keys |
| **API aggregation (BFF)** | Dashboards | Parallel calls, timeouts, partial failure |
| **Multi-region failover** | Global services | Replication, routing, split-brain prevention |
| **Tenant isolation** | SaaS | Authz, quotas, data and cache separation |

> Example: a payment system = transactional workflow + async processing + idempotency + outbox + reconciliation + rate limiting.

---

## 6. Cross-cutting trade-off matrix

### A. Data and database

| Trade-off | Prefer A when | Prefer B when |
|---|---|---|
| **SQL vs NoSQL** | Relations, transactions, constraints, flexible queries | Access patterns known, huge scale, write-heavy, key-based |
| **Normalize vs denormalize** | Consistency, write simplicity | Read latency, avoid joins |
| **Single DB vs multiple DBs** | One system handles workloads | Independent scaling/isolation justified |
| **Primary reads vs replicas** | Need read-your-own-writes | High read throughput, lag OK |
| **Shard vs don't shard** | One DB can't meet capacity/isolation | One DB still meets needs (simpler ops) |
| **Hash vs range sharding** | Even distribution | Range scans, time-series |
| **Offset vs cursor pagination** | Small data, jump to page N | Large data, stable traversal |
| **Soft vs hard delete** | Recovery, audit | Immediate removal, privacy |

> Start with the simplest DB that meets requirements. Do not shard or go polyglot just because the system is "large scale".

### B. Consistency and transactions

| Trade-off | Prefer A when | Prefer B when |
|---|---|---|
| **Strong vs eventual** | Overselling, balances, ownership | Feeds, counts, analytics, search |
| **ACID vs Saga** | One DB boundary | Workflow spans services |
| **Optimistic vs pessimistic lock** | Conflicts rare | Contention high |
| **Unique constraint vs app check** | Invariant must hold vs concurrent writers | Validation is only advisory |
| **Idempotency vs blind retry** | Duplicates cause duplicate effects | Operation is naturally safe to repeat |
| **Sync validation vs async reconciliation** | Decision needed before ack | Outcome can be resolved later |

> A cache, queue, or distributed lock is **not** a substitute for a DB constraint when the DB can enforce the invariant.

**CAP / PACELC in one line:** During a partition, choose consistency or availability. Else, choose latency or consistency.

### C. Caching and delivery

| Choice | Use when | Avoid when |
|---|---|---|
| Redis cache | Repeated reads justify memory cost | Rarely reused data, or every read must see the latest commit |
| CDN | Public cacheable, or safely partitioned by auth context | User-specific/sensitive data without safe cache controls |
| Cache-aside | Flexible, demand-driven | Cold-start latency or invalidation races violate requirements |
| Write-through | Cache must be updated with the write | Cache availability shouldn't gate every write |
| Write-behind | Deferred persistence acceptable, loss semantics explicit | Write must be durable before success |
| No cache | Cheap to fetch, freshness dominates | Expensive repeated reads overwhelm origin |

### D. Sync vs async

| Dimension | Synchronous | Asynchronous |
|---|---|---|
| User feedback | Immediate result | "Accepted", result later |
| Coupling | Depends on downstream availability | Queue decouples |
| Latency | Higher with downstream calls | Short request, longer completion |
| Failure handling | Errors propagate | Retries, DLQ, reconciliation |
| Consistency | Easier | Intermediate states |
| Examples | Reserve seat, authorize payment | Email, transcode, report, index update |

> Strong designs use **sync for the core decision**, **async for side effects**. The boundary depends on the business contract, not a universal rule.

### E. Everything else (quick table)

| Decision | Option A | Option B | Choose A when | Choose B when |
|---|---|---|---|---|
| Delivery guarantee | At-least-once (+ idempotency) | At-most-once | Can't lose data | Loss OK, duplicates not |
| Replication | Synchronous | Asynchronous | Zero data loss | Lower latency, higher throughput |
| Push vs pull (feeds) | Push (fan-out on write) | Pull (on read) | Few followers, read-heavy | Celebrities, write-heavy |
| WebSocket vs polling | WebSocket | Polling/SSE | True real-time, two-way | Simple, infrequent updates |
| Batch vs stream | Batch | Stream | Accuracy, cost | Low-latency insight |
| Exact vs approximate | Exact | Approximate | Billing, correctness | Analytics, huge cardinality |
| Fail-open vs fail-closed | Open | Closed | Availability matters more | Security matters more |
| Monolith vs microservices | Monolith | Microservices | Small team, early stage | Large org, independent scaling |
| Queue vs direct call | Queue | Direct | Need buffering/retry/decoupling | Need immediate answer, simple flow |
| Eager vs lazy compute | Eager (precompute) | Lazy (on demand) | Read-heavy, repeated | Rare or unpredictable use |

---

## 7. Problem to pattern mapping cheat sheet

| Interview problem | Primary pattern | Secondary patterns | Spend the most time on |
|---|---|---|---|
| Movie ticket booking | 1 Resource allocation | Transactions, caching, async | Seat holds, double booking, expiry |
| Hotel booking | 1 | Search, transactions | Date-range inventory, overlap |
| Flight booking | 1 | Workflows | Inventory, fare classes, payment failure |
| Ride hailing (Uber) | 1 + 10 | Real-time, write-heavy | Driver discovery, offer expiry, atomic assignment |
| Flash sale | 1 | Rate limiting, queue | Overselling, waiting room |
| E-commerce checkout | 5 Transactional | Read-heavy, async | Checkout, stock, payment, order state |
| Payment system | 5 | Event-driven, reconciliation | Idempotency, uncertain outcomes, ledger |
| Digital wallet | 5 | Audit, security | Atomic debit/credit, overdraft prevention |
| News feed | 2 Read-heavy | Fan-out, ranking | Push vs pull vs hybrid, hot users |
| Instagram | 2 + 9 | Async, CDN | Upload processing, feed, media delivery |
| URL shortener | 15 Lookup + 2 | Analytics | Key generation, redirect, cache, hot keys |
| Notification service | 8 Event-driven | Async, rate limiting | Preferences, retries, dedup |
| WhatsApp-like chat | 7 Real-time | Durable messaging | Delivery semantics, ordering, offline users |
| Google Docs | 7 | Versioning | OT/CRDT, conflict resolution |
| Google Drive / Dropbox | 9 File storage | Sync, versioning | Upload, metadata, access control, conflicts |
| YouTube / Netflix | 9 | Async, CDN | Transcoding, manifests, edge caching |
| Search engine | 6 Search | Write ingestion | Indexing, relevance, freshness |
| Autocomplete | 6 | Caching | Prefix match, top-K, low latency |
| Logging / metrics system | 3 Write-heavy | Analytics | Backpressure, partitioning, retention |
| Report export | 4 Async | File storage | Job state, chunking, retries, download |
| Webhook platform | 8 | Async | Delivery attempts, signatures, replay |
| Distributed cron / job scheduler | 12 Scheduling | Queues, leases | Duplicate execution, missed schedules |
| Web crawler | 12 | Dedup, rate limiting | URL frontier, politeness, dedup |
| Rate limiter | 11 | Distributed state | Algorithm, shared quotas, fail-open |
| Unique ID generator | 11 | - | Snowflake, clock issues |
| Leaderboard | 14 / 2 | Aggregation | Sorted sets, hot keys, ties |
| Top-K / trending | 14 Analytics | Streaming | Count-Min Sketch, windows |
| Ad click aggregation | 14 + 3 | Dedup | Late events, exactly-once effect |
| Multi-tenant SaaS | 13 | Partitioning | Authorization, quotas, noisy neighbors |
| Real-time analytics | 14 | Write-heavy | Windows, late events, aggregation |
| Config / feature flag service | 15 | Caching, replication | Fast reads, propagation, safe defaults |
| Nearby friends / places | 10 | Search | Spatial index, update frequency |

**How to use:** For each problem, master the primary pattern and its correctness guarantee. Study secondary patterns enough to integrate. You do not need a new design for every row.

---

## 8. Core components you must know

For each, answer three questions: **What problem does it solve? Why is it better than the simpler alternative here? What new failure modes or costs does it add?** (The third question separates senior answers from checklists.)

| Component | Know this |
|---|---|
| **Relational DB (Postgres/MySQL)** | Transactions, isolation levels, constraints, indexes, row locks, optimistic concurrency, query plans, connection pools, replication, partitioning |
| **Redis** | Cache-aside, TTL, eviction, atomic ops, sorted sets, counters, rate limiting, pub/sub, geo, hot keys, behavior when cache fails |
| **Kafka / brokers** | Topics, partitions, ordering per partition, consumer groups, ack, redelivery, backpressure, replay, retention, DLQ |
| **Object storage + CDN** | Pre-signed URLs, multipart upload, lifecycle policies, cache keys, invalidation, range requests, private content |
| **Search engine** | Inverted index, analyzers, relevance, indexing lag, facets, reindexing |
| **Load balancer / API gateway** | L4 vs L7, algorithms (round robin, least connections, consistent hashing), health checks, auth, throttling |
| **Consistent hashing** | Minimal data movement on node changes, virtual nodes |
| **Replication** | Leader-follower, multi-leader, leaderless quorum (R+W>N) |
| **Sharding** | Hot shards, resharding, cross-shard queries |
| **Bloom filter** | "Definitely not present / maybe present" |
| **Resilience patterns** | Circuit breaker, bulkhead, retry + backoff + jitter, timeouts |
| **Observability** | Metrics, logs, traces, SLOs, queue lag, saturation, alerts |

---

## 9. Estimation numbers

- 1 day = 86,400 s, about 10^5 s. So **1M requests/day is about 12 QPS**.
- Peak is roughly **2-5x** average.
- One DB node: a few thousand to about 10K simple QPS. Redis: about 100K ops/s per node.
- Latency: memory ~100 ns, SSD ~100 us, same-DC network round trip ~0.5 ms, cross-continent ~150 ms.
- Storage: text post ~1 KB, image ~200 KB-2 MB, video ~ several MB per minute.
- Always compute: QPS (avg + peak), storage per day and per year, bandwidth, cache size (often 20% of data serves 80% of reads).

---

## 10. Study plan and how to practice

### Learn these 8 first (best coverage)

1. **Movie ticket booking** - concurrency, double booking
2. **Payment / order processing** - transactions, idempotency, Saga
3. **News feed** - read optimization, fan-out, hot users
4. **Metrics / log ingestion** - write throughput, partitioning, backpressure
5. **Report generation** - async processing, job recovery
6. **Notification / webhook platform** - delivery guarantees, retries, DLQ
7. **Chat** - real-time delivery, persistence, ordering
8. **Ride hailing** - geospatial discovery + resource allocation

Then add: URL shortener, rate limiter, typeahead, YouTube/Dropbox, job scheduler, Top-K, multi-tenant SaaS.

### 4-week plan

| Week | Focus | Problems |
|---|---|---|
| 1 | Correctness and transactions | Movie booking, hotel/appointment, payment + wallet, e-commerce checkout. Learn idempotency, isolation levels, outbox, Saga |
| 2 | Read/write scaling | URL shortener, news feed, product catalog + search, metrics ingestion. Learn cache, replication, partitioning, hot keys |
| 3 | Async and real-time | Report generation, notification + webhook, chat, video/image processing. Learn retries, DLQ, delivery guarantees, backpressure |
| 4 | Advanced distributed systems | Ride hailing, job scheduler, rate limiter, multi-tenant SaaS, analytics, multi-region. Do mock interviews |

### How to practice one problem (45-60 min)

- 5-10 min: requirements, estimation, **identify invariants, classify patterns**
- 10 min: APIs, data model, boundaries
- 15-20 min: architecture, trace the most important request end to end
- 10-15 min: failures, consistency, 2-3 major trade-offs
- 5 min: bottlenecks, scaling, monitoring, what you'd defer

For each pattern, write a one-page note: **core challenge, 3 key moves, 3 trade-offs, 2 failure scenarios, interviewer probes.**

When a new problem comes up, ask: *What is the hardest part? Which pattern does it match? Which patterns do I need to combine?*

---

## 11. Common mistakes that cost interviews

1. **Adding every technology.** Kafka, Redis, Elasticsearch should each solve a named problem.
2. **"Use a cache" without invalidation.** Discuss freshness, keys, stampede, cache outage.
3. **"Exactly once" without defining the boundary.** Separate delivery, processing, DB commit, and external side effects. Use idempotency.
4. **Async for everything.** Some decisions must be authoritative before success is returned.
5. **Sync calls for every dependency.** Long latency chains, amplified failures.
6. **Treating a distributed lock as full correctness.** Think lease expiry, stale owners, fencing.
7. **Ignoring recovery.** Explain how incomplete work is detected and repaired.
8. **Sharding before measuring.** Start with good schema, indexes, capacity numbers.
9. **Confusing availability with correctness.** A fast success response for an invalid operation is useless.
10. **Ignoring security and tenant isolation.** Mention authn, authz, quotas, sensitive data.
11. **Skipping the read/write ratio.** It drives the whole design.
12. **Not stating trade-offs.** Always say what you gave up and why.

---

## 12. One-page revision sheet

**First 2 minutes of any problem:**
1. What is the invariant that must never break?
2. Read-heavy, write-heavy, or both?
3. Does it need strong or eventual consistency?
4. Is any work slow enough to go async?
5. Which patterns match? (Use the table in section 7.)

**Pattern in one line:**

| # | Pattern | Remember |
|---|---|---|
| 1 | Resource allocation | Atomic conditional update + hold with expiry + idempotency; DB is the source of truth |
| 2 | Read-heavy | CDN + cache + replicas; discuss invalidation; hybrid fan-out for feeds |
| 3 | Write-heavy | Kafka buffer + LSM DB + partitioning + batching; absorb bursts |
| 4 | Async jobs | Job state + at-least-once + idempotent + backoff + DLQ + outbox |
| 5 | Transactional workflow | Saga + idempotency + ledger + reconciliation; timeout is not failure |
| 6 | Search | Inverted index as derived copy via CDC; autocomplete = precomputed top-K per prefix |
| 7 | Real-time | WebSocket + registry + pub/sub + durable store + per-conversation order |
| 8 | Event distribution | Queue vs pub/sub, per-key order, DLQ, signed webhooks |
| 9 | Media | Pre-signed URL + object store + async transcode + CDN |
| 10 | Geospatial | Geohash/H3 index, latest location in memory, discovery then atomic assignment |
| 11 | Primitives / rate limit | Token bucket, Redis atomic ops, consistent hashing, fail-open vs closed |
| 12 | Scheduling | Persisted schedule, leases + fencing, idempotent jobs |
| 13 | Multi-tenant | Tenant scope everywhere, quotas, noisy-neighbor protection |
| 14 | Analytics | Separate OLAP path, windows + watermarks, HLL / Count-Min |
| 15 | HA lookup | Replication, RTO/RPO, stale-OK degradation, safe defaults |

**Last 5 minutes of any interview, always cover:**
- Bottlenecks and how you would scale next
- Failure scenarios and recovery
- Monitoring (queue lag, error rate, latency percentiles, saturation)
- Security and tenant isolation
- What you deliberately **deferred** and the trigger to add it

> **The core skill:** recognize the invariant, classify the workload, choose the simplest architecture that preserves correctness, and justify every extra component with a concrete requirement.

