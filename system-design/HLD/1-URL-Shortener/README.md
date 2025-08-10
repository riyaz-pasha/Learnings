# 1. Functional and Non-Functional Requirements

## Functional requirements (core)

* Create short URL for a long URL (auto-generated code).
* Redirect short URL -> original long URL (HTTP 301/302).
* Support custom / vanity aliases (e.g., `short.ly/goTeam`).
* URL expiration and optional TTL.
* Analytics: click count, referrer, geolocation (optional).
* Admin features: blacklist, rate limiting, disable link.
* API + Web UI for creating/manage short URLs.
* Link preview / metadata fetch (optional).

## Non-functional requirements (SLOs, priorities)

* **Latency:** Redirect: <= 20 ms (user-perceived). Create short URL: <= 100–300 ms.
* **Throughput / Scalability:** Support millions of redirects per day; scale horizontally.
* **Availability:** 99.99% across regions (multi-AZ, graceful failover).
* **Durability:** Persistent storage of short→long mapping (no data loss).
* **Consistency:** Strong consistency for alias creation (uniqueness); eventual consistency for analytics counters is acceptable.
* **Security:** Authentication for API, abuse controls, sanitization of target URLs.
* **Cost efficiency:** minimize expensive DB reads for redirects via cache/CDN.

## Clarifying assumptions

* We own a short domain (e.g., `s.ly`).
* We store only mappings + metadata; payload sizes are small (URL, alias, metadata).
* Analytics are "nice-to-have" and can be eventually consistent.
* Multi-region deployment is required (global users).

---

# 2. Back-of-the-Envelope Estimation (with step-by-step math)

We pick a representative scale to size components. You can scale numbers up/down.

### Assumptions

* Daily redirects (`R_day`) = 10,000,000 (ten million clicks/day).
* Daily new short URLs created (`W_day`) = 1,000,000 (one million creations/day).
* Read\:Write ratio \~ 10:1 (redirects >> creates). (Here read/write ≈ 10M:1M = 10:1.)
* Peak factor = 10x average traffic during spikes.

### Average QPS (reads)

* Seconds per day = 86,400.
* Average reads per second:

  * `R_day / 86,400`
  * `10,000,000 / 86,400`
  * Reduce: divide both by 100 -> `100,000 / 864` ≈ `115.7407`
  * So **avg read QPS ≈ 116 QPS**.

### Peak QPS (reads) with 10x spike

* Peak = avg \* 10 ≈ `116 * 10 = 1,160 QPS`.
* Add writes peak concurrency (writes low): creates \~ `1,000,000 / 86,400 ≈ 11.57 QPS` average; peak \~ 116 QPS.
* So combined peak \~ `1,160 + 116 ≈ 1,276 QPS` → round **\~1.3k QPS**.

### Storage for mappings

* Estimate per record size: id/slug + long URL + metadata \~ 200 bytes average.

  * Example: `200 bytes` per mapping.
* For `N = 1,000,000,000` (1 billion) total records:

  * Total bytes = `200 * 1,000,000,000 = 200,000,000,000 bytes`.
  * Convert to GiB: `200,000,000,000 / (1024*1024*1024)`.

    * 1024*1024*1024 = 1,073,741,824.
    * Division ≈ `200,000,000,000 / 1,073,741,824 ≈ 186.26 GiB`.
  * So **\~200 GB (≈186 GiB)** for 1B links. (If only 100M links, \~20 GB.)

### Bandwidth for redirects

* Redirect response size small (HTTP 301 header). Assume \~500 bytes of transfer attributable to our server per redirect.
* Daily bandwidth = `R_day * 500 bytes = 10,000,000 * 500 = 5,000,000,000 bytes`.
* Bytes → GiB: `5,000,000,000 / 1,073,741,824 ≈ 4.66 GiB/day`.
* So **\~5 GB/day** outgoing.

> These are conservative, straightforward numbers you can scale with actual metrics.

---

# 3. High-Level Architecture (block diagram + components)

ASCII block diagram (logical):

```
Clients (browsers, apps)
    |
    v
CDN (edge caching for redirects, caches 301 response & static content)
    |
    v
Global Load Balancer (DNS + Anycast) -> API Gateway / LB
    |
    +--> Auth / Management API Servers (Create URLs, admin)
    |
    +--> Redirect Servers (stateless)  <--- Redis Cache (mapping)
                     |
                     v
                Persistent DB (Primary/Replica or Sharded NoSQL)
                     |
                     v
               Analytics pipeline (Kafka) -> Click store / OLAP
```

## Roles & interactions

* **Clients:** issue redirect HTTP GET requests to short URL domain; or POST to create short URL via REST API.
* **CDN / Edge Cache:** caches frequent redirects (301 responses) to serve directly from edge (reduce origin load). TTL short (e.g., 1–5 minutes) or indefinite for immutable links.
* **Global Load Balancer / Anycast DNS:** route to nearest region; handle failover.
* **API Gateway:** central point for authentication, rate-limiting, routing to create/management endpoints.
* **Redirect Servers (stateless):** accept `GET /{slug}`, look up mapping (prefer Redis), respond with 301 to long URL, and asynchronously publish analytics event.
* **Auth/Management Servers:** handle create, update, custom alias checks, admin UI, must ensure uniqueness (strong consistency).
* **Cache (Redis / Memcached):** hot mapping cache, TTLs.
* **Primary DB:** persistent store for all mappings (SQL or NoSQL); used for writes and cold reads.
* **Analytics Pipeline:** ingest redirect events (Kafka), process to OLAP store (BigQuery/ClickHouse) for reports.
* **Background jobs:** for TTL eviction, migration, re-sharding, batch analytics.

## Protocols

* Public API: REST over HTTPS (HTTP/1.1 or HTTP/2). Could also support gRPC for internal services.
* Inter-service: gRPC or HTTP/2 for speed.
* Event streaming: Kafka or Kinesis.

---

# 4. Component Deep Dive

## Data storage

### DB choice (SQL vs NoSQL)

* **Primary mapping store:** Choose a NoSQL key-value store (e.g., Cassandra, DynamoDB, or sharded MySQL) or relational DB depending on needs.

  * **Why NoSQL (recommended):** extremely simple key→value access pattern (slug → URL + metadata), massive scale and availability, easy horizontal partitioning.
  * **Why SQL (possible):** if you need complex relational queries, strong ACID transactions for alias uniqueness—use a SQL DB with auto-increment or UUID with sharding.
* **Hybrid approach:** Use a relational DB (MySQL/Postgres) for transactional operations (alias creation) + a NoSQL KV store (DynamoDB/Cassandra) for read-heavy redirect mapping. Or use DynamoDB + conditional writes for uniqueness.

### Schema / data model (logical)

A single mapping record (compact):

* **Table / Collection: `url_map`**

  * `slug` (PK, string) — the short code or alias (e.g., `aB9xZ2`)
  * `target_url` (string) — the long original URL
  * `user_id` (optional) — owner
  * `created_at` (timestamp)
  * `expiry_at` (nullable timestamp)
  * `click_count` (int) — (optionally updated asynchronously)
  * `is_active` (bool)
  * `meta` (JSON) — title, tags, etc.

Indexes:

* PK on `slug`
* Unique constraint/index on `slug` for custom/vanity alias uniqueness.

Analytics events stored separately (append-only):

* `click_events`: `slug`, `timestamp`, `ip_hash`, `country`, `referrer`, `user_agent` (hashed/partial).

### Sharding & partitioning

* Partition by `slug` using consistent hashing or range/shard key.
* For NoSQL: choose partition key = `slug` hashed. Ensure even key distribution by using base62 encoded sequential IDs or randomized slug generation.
* For SQL sharding: choose modulo of an auto-increment ID or hash of slug for shards.
* Use a **routing service** to map slug -> shard (or compute shard by hash algorithm in app code).

## Caching strategy

### Where

* **Edge (CDN)**: cache redirect responses (HTTP 301) for hot slugs to serve directly from edge worldwide. Best for extremely hot URLs.
* **Application layer (Redis):** cache slug → target\_url with small TTLs. Primary cache for redirect servers.
* **Client-side:** short TTL caching via browser behavior is limited (user agent). Not relied on.

### Cache pattern & eviction

* Use **cache-aside** (lazy loading):

  * Redirect server checks Redis (cache). If miss, query DB, write to cache, and return redirect.
* Eviction policy: **LRU** in Redis (default for managed Redis) and TTL per key (e.g., 1 hour or longer for immutable links).
* Write strategy for updates/deletes: invalidate cache on write (create/update/delete) — explicitly delete Redis key (cache invalidation).

### Why cache-aside?

* Simple, efficient reads; avoids unnecessary writes to cache on every DB write.
* Analytics updates are done asynchronously (do not block redirect path).

## Load Balancing

### Placement & type

* **Global DNS / Anycast LB** at edge: route users to nearest region.
* **Region-level Load Balancer** (e.g., AWS ALB / GCP LB) in front of web/API servers.
* **Internal load balancers** for service-to-service traffic if needed.
* **Type:** use health-aware LB, with algorithms:

  * For stateless redirect servers: **Round Robin / Least Connections** with health checks.
  * For persistent connections/internal gRPC: **Least Connections** or connection-aware LB.

---

# 5. Scalability and Availability

## Horizontal scalability

* Make services **stateless**: web/redirect servers do not hold state; they read mapping from cache/DB. Can add servers behind LB.
* Database: partition/shard horizontally (NoSQL or sharded SQL) to handle storage and throughput.
* Cache: use Redis cluster (sharded) with replicas.
* Event streaming: Kafka with multiple partitions for parallel consumers.

## High availability / handling SPOFs

* **Multi-AZ and multi-region deployment.**
* DB: replication (leader-follower) and cross-region reads (read replicas) for failover. Use leader election for writes.
* Cache: Redis with primary-replica and failover (Redis Sentinel or managed Redis Cluster).
* LB & DNS: multiple endpoints, health checks and low TTL DNS to enable failover.
* **No single master** for slug generation: use distributed ID generator (Snowflake or UUID + encoding) or per-shard ID services. If a central service used, make it replicated and highly available.
* Backup & restore: periodic snapshots for DB, incremental backups.
* Disaster recovery: cross-region replication and failover procedures.

---

# 6. Potential Bottlenecks, Trade-offs & Future Improvements

## Potential bottlenecks

* **DB writes for creation-heavy workloads:** millions of writes/day could need many write shards or a write-optimized store.
* **Hot keys:** a viral slug can cause a single cache key to be overwhelmed (thundering herd) — mitigate with CDN and rate limiting.
* **Cache miss storms:** if Redis cluster goes down, origin DB may be hammered—careful capacity planning and failover.
* **Uniqueness checks for custom aliases:** require strongly-consistent check; contention possible if many custom alias attempts.

## Trade-offs

* **Consistency vs. Availability:** for alias creation (custom alias) we need strong consistency (unique constraint). This can force a single leader or transactional DB write, compromising availability in leader outage. We prefer strong consistency for alias creation and eventual consistency for analytics.
* **Short slug length vs. collision risk:** Very short slugs are desirable but exhaust namespace fast; either increase alphabet or slug length.
* **Caching TTL vs. update propagation:** longer cache TTL reduces load but slows propagation of updates/deletes — we choose moderate TTLs and immediate cache invalidation on updates.

## Mitigations & strategies

* **Prevent thundering herd:** add per-key rate-limiting, use leaky-bucket for analytics ingestion, and use CDN to offload hot traffic.
* **Custom alias creation flow:**

  * API checks Redis for alias existence, then performs conditional DB insert (e.g., DynamoDB conditional write or SQL `INSERT ... WHERE NOT EXISTS`). If DB insertion fails because alias exists, return 409 Conflict.
  * Immediately write alias->mapping into cache on success.
* **Slug generation strategies:** use a distributed unique ID (Snowflake) to generate compact base62 codes. This avoids collisions and central hotspot.

## Future improvements / extensions

* **Auto-scaling & autoscaling policies** tuned to QPS.
* **Per-user quota & billing** for high-volume or vanity domains.
* **Custom domain support** (users bring their own domain).
* **A/B redirect / feature flags** for marketing.
* **Shorter slugs using pooled namespace** (reclaim deleted short codes after TTL).
* **Rate-limited vanity auctions** (if aliases are monetized).
* **Advanced analytics & BI (ClickHouse, Presto)** for real-time dashboards.
* **Security:** link scanning services (reputation, phishing detection).

---

# API Design (practical examples)

## 1) Create short URL (auto-generated)

**POST** `/api/v1/shorten`
Request JSON:

```json
{
  "url": "https://very.long.url/path?foo=bar",
  "user_id": "12345",          // optional
  "expiry_at": "2026-01-01T00:00:00Z", // optional
  "private": false             // optional
}
```

Response 201:

```json
{
  "slug": "aB9xZ2",
  "short_url": "https://s.ly/aB9xZ2",
  "target_url": "...",
  "created_at": "2025-08-09T12:00:00Z"
}
```

## 2) Create custom alias

**POST** `/api/v1/shorten/custom`

```json
{
  "url": "https://target.com",
  "alias": "goTeam",
  "user_id": "123"
}
```

* Server flow: check alias validity (regex), check cache; then conditional DB insert (atomic).
* Response 201 or 409 Conflict if alias taken.

## 3) Redirect

**GET** `GET /{slug}`

* HTTP 301 Location header -> `target_url`
* Edge/CDN caches this response (if allowed by TTL).

## 4) Get analytics (sample)

**GET** `/api/v1/shorten/{slug}/stats?from=...&to=...`

* Returns aggregate count, top refs, geo distribution (eventually consistent).

## 5) Delete / Disable

**DELETE** `/api/v1/shorten/{slug}` (auth required) — mark `is_active=false`, invalidate cache.

---

# Hashing / Slug Generation Strategies (detailed)

### Option A — Sequential numeric ID + Base62 encoding (recommended)

* Generate a monotonic unique ID (e.g., Snowflake or DB auto-increment per shard).
* Encode numeric ID into Base62 (0-9, a-z, A-Z) to produce short slugs.
* Advantages:

  * No collisions.
  * Deterministic length; easy to reserve ranges for shards or vanity.
* Consideration: predictable slugs can be enumerated — to avoid, you can introduce obfuscation layer (XOR with per-shard random) before encoding.

### Option B — Random hash (MD5/SHA + Base62)

* Compute hash of `URL + salt`, take first N bits, base62.
* Advantages: distributed generation, less predictable mapping from incremental ID.
* Disadvantages: collision probability; must check DB for collisions and retry — adds complexity.

### Option C — Custom alias mode

* For custom alias requests, take the alias string directly; validate and do conditional insert with unique constraint.

### Best practice

* Use **Snowflake-style** or **UUID -> short** approach and Base62 encode. Enforce per-shard ID ranges to generate slugs without central bottleneck.
* For vanity: use DB transactional insert with unique constraint.

---

# Reliability, Observability & Security

* **Monitoring:** Prometheus + Grafana for QPS, latency, cache hit ratio, DB metrics.
* **Logging & Tracing:** Structured logs, OpenTelemetry traces for create & redirect flows.
* **Alerts:** high error rate, cache failover, DB replication lag.
* **Security:** API keys / OAuth for users, rate limits, input sanitization, URL safety scanning (3rd party).
* **Privacy:** hash IPs for analytics or store truncated IPs; conform to privacy laws.

---

# Quick summary / checklist for implementation

1. API servers (stateless) behind LB + API gateway.
2. Redis cluster for hot slug cache + CDN for edge caching of 301 responses.
3. Persistent store: NoSQL KV (Cassandra/DynamoDB) or sharded SQL for mappings.
4. Slug generation: Snowflake IDs + Base62 encode; custom alias handled by DB uniqueness check.
5. Analytics via async event streaming (Kafka) -> OLAP store.
6. Multi-AZ multi-region for HA; backup and DR in place.
7. Rate limit and abuse detection; protect hot keys and avoid DB stampede.
8. Observability and automated scaling.

---


---

## Assumptions (explicit)

* **Traffic (base)**

  * Daily redirects = **10,000,000**
  * Daily creations = **1,000,000**
* **Average per-second**

  * Avg reads = `10,000,000 / 86,400 ≈ 115.74 RPS`
  * Avg writes = `1,000,000 / 86,400 ≈ 11.57 RPS`
* **Peak factor** = **10×** (short spike window)

  * Peak reads = `115.74 × 10 ≈ 1,157.41 RPS`
  * Peak writes = `11.57 × 10 ≈ 115.74 RPS`
  * **Total peak origin events ≈ 1,273.15 RPS**
* **CDN edge cache hit ratio (typical target)** = **80%** for redirects (edge serves hot redirects). (I’ll also show numbers without CDN later.)
* **Per application server capacity (origin)** = **\~500 RPS** (conservative for typical cloud instance running highly optimized redirect code and doing Redis lookups). You can increase/decrease this after load testing.
* **Record size (storage)** = **\~200 bytes** per mapping.

> If you want a different target QPS, tell me and I’ll recalc everything; otherwise I’ll use these.

---

## Calculations (brief)

1. Peak reads = `1,157.41 RPS`. Peak writes = `115.74 RPS`. Combined = `~1,273.15 RPS`.
2. With **80% CDN hit**: origin reads = `1,157.41 × 0.2 = 231.48 RPS`. Add writes: `231.48 + 115.74 ≈ 347.22 RPS` hitting origin services (Redis + DB + app).
3. Use per-server capacity = **500 RPS**, thus `ceil(347.22 / 500) = 1 server` (but need redundancy → choose minimum 3 instances in prod).

---

## Final per-component capacity plan (recommended, conservative, prod-ready)

### 1) Edge / CDN

* **Provider:** Cloud CDN / Anycast (Cloudfront / Cloudflare / Fastly).
* **Config:** Cache redirect responses (301) with short TTL for mutable links; long TTL for immutable links.
* **Why:** Offloads \~80% of read QPS from origin.

---

### 2) Load Balancers & API Gateway

* **Global:** Anycast DNS + Global LB (e.g., Route53 + Global Accelerator / Cloud CDN).
* **Regional:** 2 managed LBs per region (Auto-managed by cloud provider) for HA.
* **Throughput:** LBs handle the full public peak; no capacity limit with managed LB.

---

### 3) Application servers (redirect + creation APIs)

* **Role split:** Separate fleets for `redirect`-optimized path and `api/create/manage` path.
* **Sizing & count (per active region):**

  * **Redirect fleet:** minimum **3 instances** (for HA), scale to **N = ceil(origin\_redirect\_RPS / 500)**. With our numbers: origin\_redirect\_RPS ≈ 231.5 → 1 instance enough, but keep **3** for N-1 tolerance. Autoscale to \~**6** instances under heavy/unexpected load.
  * **API/Write fleet:** API creation is heavier (validations, DB writes) but lower throughput. Peak writes ≈ 115.7 RPS; a single mid-sized instance can handle ≈ 200–500 writes/s depending on DB latency. Use **3 instances** as baseline, autoscale by CPU/latency to cover spikes.
* **Autoscaling:** enable autoscaling groups with min=3, max tuned to peak\*2 for safety.

**Notes:** put redirect servers behind LBs, health checks, and use autoscaling based on request/CPU/latency.

---

### 4) Cache layer (Redis)

* **Workload hitting Redis** (cache-aside):

  * Cache GETs ≈ origin\_reads ≈ **231.5 GET/s** (with CDN)
  * Cache SETs (miss fills + writes) ≈ small fraction; plus writes for creates ≈ **116/s**
  * **Total Redis ops peak ≈ 350 ops/s** (conservative)
* **Provisioning recommendation:**

  * **Redis Cluster with 3 primary shards** and **1 replica per shard** → **6 nodes total**. This gives:

    * Horizontal capacity to scale if cache hit ratio drops.
    * Replica for read failover and HA.
  * For this QPS, even a single primary could handle it, but 3 primaries give room for growth and rebalancing.
* **Instance size:** mid-sized (e.g., `cache.m4.large` or equivalent) — CPU/network bound; adjust by testing.
* **Eviction policy:** LRU + TTL per key.
* **Failover:** Sentinel / managed Redis with automatic failover.

---

### 5) Persistent DB (mapping store)

* **Choice:** NoSQL KV (DynamoDB/Cassandra) or sharded relational (MySQL/Postgres) depending on your preference. Below plan assumes a sharded strategy so you can map onto either.
* **Workload to DB (origin):**

  * With CDN 80% hit: origin reads hitting DB are mostly cache misses. Assume small miss rate (e.g., 5–10% of origin reads). Practically the DB must reliably handle **peak writes ≈ 116 writes/s** + some reads (cold misses).
* **Sharding plan (recommended for growth):**

  * **4 write shards** (hash-based partitioning by slug). Each shard:

    * Primary (writer) + **2 read replicas** for regional read scaling and failover.
    * So total DB instances = **4 primaries + 8 replicas = 12 DB nodes**.
  * **Why 4 shards:** reduces per-shard write load to ≈ `116 / 4 ≈ 29 writes/s` at peak — very small for typical DB instances, giving lots of headroom.
* **Storage sizing:** For 1B records & 200 bytes/record → ≈ **200 GB** total. Per shard ≈ `200 / 4 = 50 GB` + overhead (indexes, metadata). Use disks with IOPS tuned to expected workload (small per-request IOPS).
* **Instance type:** for modest writes, mid-sized instances (e.g., db.m5.large) per primary; larger if you need lower latency or complex queries.
* **Consistency:** transactional uniqueness check on custom alias creation must be enforced on the shard that would own that alias (use conditional insert / unique constraint).
* **If using DynamoDB:** single table with conditional writes removes need to manage shards; provisioned capacity for writes: peak writes 116 WPS → provision \~200–300 WCU to allow headroom.

---

### 6) Message queue / Analytics pipeline (Kafka)

* **Events:** each redirect emits an event for analytics. Peak events ≈ **1,273 events/s** (all redirects + creates).
* **Partitions:** choose partitions to match consumer parallelism. **12 partitions** is a good start.
* **Broker count:** **3 brokers** (min) for Kafka HA; increase capacity if partitions grow.
* **Retention:** short rolling retention for real-time processing; store aggregated results to OLAP (ClickHouse / BigQuery).

---

### 7) CDN + origin bandwidth

* **Estimated daily outgoing bandwidth (for redirects):** earlier est ≈ 5 GB/day — small. With CDN, origin bandwidth is lower.
* **CDN configuration:** short TTL for mutable, long TTL for immutable; set cache key = slug.

---

## HA & Fault Tolerance summary

* **App servers:** min 3 per region (redundancy), autoscale.
* **Redis:** cluster with replicas (3 primaries + replicas) — cross-AZ; automatic failover.
* **DB:** 4 shards × (1 primary + 2 replicas) → 12 nodes total; cross-AZ replication and automated failover.
* **Kafka:** 3 brokers, multi-AZ.
* **CDN:** global edge + origin fallback.
* **DNS / Global LB:** low TTL and multi-region failover configured.

---

## Example capacity table (single-region baseline)

| Component            |           Metric to satisfy |                                   Recommended baseline |
| -------------------- | --------------------------: | -----------------------------------------------------: |
| Redirect app servers | \~347 origin RPS (with CDN) |                       **3 instances** (autoscale to 6) |
| API/write servers    |         \~116 writes/s peak |                       **3 instances** (autoscale to 6) |
| Redis cluster        |            \~350 ops/s peak |                 **3 primaries + 3 replicas = 6 nodes** |
| DB mapping store     | \~116 writes/s + cold reads | **4 shards** → 4 primaries + 8 replicas = **12 nodes** |
| Kafka                |             \~1.3k events/s |                       **3 brokers**, **12 partitions** |
| CDN                  |         offload \~80% reads |                                configure caching rules |

> These are **conservative** production baselines — they prioritize high availability, easy failover, and headroom for growth.

---

## Autoscaling & Monitoring recommendations

* **Autoscale app servers** on latency and request queue length; maintain min 3 instances.
* **Scale Redis** by adding shards if cache hit ratio falls or ops/s grows.
* **DB:** scale horizontally by increasing shards (rehash plan), or use managed scalable NoSQL (DynamoDB/Cassandra) to avoid manual shards.
* **Monitoring:** track QPS, 95/99p latency, Redis hit ratio, DB replication lag, queue lags, error rates.
* **Load testing:** run chaos and load tests to validate capacity and failure modes.

---


Here is a high-level system design for a scalable URL shortening service like TinyURL.

-----

### 1\. Functional and Non-Functional Requirements

#### Functional Requirements:

  * **URL Shortening:** The system must accept a long URL and generate a unique, short alias.
  * **Redirection:** When a user navigates to a short URL, the system must redirect them to the original long URL with low latency.
  * **Custom Aliases:** Users should have the option to specify a custom, human-readable short alias instead of a randomly generated one. The system must validate that the custom alias is unique and not reserved.
  * **URL Expiration:** The system should support an optional expiration date for short URLs, after which they are no longer valid.
  * **API:** A public RESTful API must be provided for programmatic interaction with the service.

#### Non-Functional Requirements:

  * **High Availability:** The service must be highly available with minimal downtime.
  * **Scalability:** The system must be able to scale horizontally to handle a massive number of read and write requests.
  * **Low Latency:** Redirect requests should be handled with extremely low latency. This is a primary driver for the system's design.
  * **Durability:** The stored URL mappings must be durable and not lost in case of component failure.
  * **Fault Tolerance:** The system should be resilient to the failure of individual components (e.g., a single server or database node).

#### Clarifying Assumptions:

  * The read-to-write ratio is assumed to be very high (e.g., 100:1 or more), meaning the system will be heavily optimized for reads.
  * Short URLs will be alphanumeric strings of a fixed length (e.g., 7 characters).
  * Analytics and user authentication are considered secondary features and will not be the primary focus of this core design.

-----

### 2\. Back-of-the-Envelope Estimation

Let's estimate the scale for a popular service.

  * **Writes (URL Creation):** Assume 100 million new short URLs created per month.
      * $100,000,000 / (30 \\text{ days} \\times 24 \\text{ hours} \\times 3600 \\text{ seconds}) \\approx 38.6 \\text{ QPS (Writes)}$
  * **Reads (Redirections):** Assuming a conservative read-to-write ratio of 100:1.
      * $38.6 \\text{ QPS} \\times 100 \\approx 3,860 \\text{ QPS (Reads)}$
  * **Storage:**
      * Each record requires: `short_url_id` (7 bytes), `long_url` (\~500 bytes avg), `creation_date` (\~4 bytes), `expiration_date` (\~4 bytes), etc. Let's estimate an average of **600 bytes** per record.
      * Monthly storage: $100,000,000 \\text{ records} \\times 600 \\text{ bytes/record} \\approx 60 \\text{ GB/month}$
      * Annual storage: $60 \\text{ GB/month} \\times 12 \\text{ months} \\approx 720 \\text{ GB/year}$
  * **Bandwidth:**
      * The primary bandwidth concern is for reads. A redirection response is very small (a few hundred bytes).
      * Bandwidth (Reads): $3,860 \\text{ QPS} \\times 500 \\text{ bytes/redirect} \\approx 1.93 \\text{ MB/s}$, which is easily manageable. The real bandwidth is used *after* the redirect.

-----

### 3\. High-Level Architecture

The system will use a **microservices-based architecture** to separate concerns and allow for independent scaling of components.

\!([https://i.imgur.com/39wUf1U.png](https://www.google.com/search?q=https://i.imgur.com/39wUf1U.png))

1.  **User/Client:** Interacts with the system via a web browser or a REST API.
2.  **DNS Service:** Resolves the short URL domain (e.g., `tinyurl.com`) to the IP address of the load balancer.
3.  **Load Balancer:** Distributes incoming traffic (both new URL requests and redirections) across a fleet of stateless web servers.
4.  **Web Servers (API):** A pool of identical, stateless servers that handle API calls.
      * **Create Service:** A service that generates and stores new URL mappings.
      * **Redirect Service:** A high-throughput service that handles all redirection requests.
5.  **Distributed Cache (Redis):** A fast, in-memory cache layer placed in front of the database to handle the majority of read requests. This is critical for achieving low latency and reducing database load.
6.  **Database:** A distributed NoSQL database for durable storage of the URL mappings.
7.  **Message Queue (Kafka/RabbitMQ):** An optional component for handling the write requests asynchronously. This decouples the write operations from the user request, improving responsiveness.

Communication within the system will use **HTTP/REST** for client-facing APIs and internal service communication.

-----

### 4\. Component Deep Dive

#### Data Storage:

  * **Database Choice:** We'll use a **NoSQL key-value store** like **Apache Cassandra** or **Amazon DynamoDB**.
  * **Justification:** The core operation is a simple key-value lookup: given a short URL, find the long URL. NoSQL databases are purpose-built for this, offering high write throughput, massive scalability, and low latency for lookups. A relational database would struggle with the horizontal scaling required for billions of records.
  * **Schema:** The data model is simple and flat. The `short_url_id` serves as the primary key.
    ```sql
    CREATE TABLE url_mappings (
      short_url_id VARCHAR(10) PRIMARY KEY,
      long_url TEXT,
      created_at TIMESTAMP,
      expires_at TIMESTAMP
    );
    ```
  * **Sharding:** We will **hash the `short_url_id`** to determine which database shard to store the record on. This ensures a uniform distribution of data across all shards and prevents a single shard from becoming a "hot spot" due to sequential IDs.

#### Hashing Strategy:

  * The system needs a method to generate a unique, short `short_url_id`. We'll use a **Base-62 encoding** approach.
  * 1.  A distributed ID generation service (e.g., using a Snowflake-like algorithm) creates a unique 64-bit integer ID.
  * 2.  This integer is converted to a base-62 string. The Base-62 character set includes `a-z`, `A-Z`, and `0-9`, providing 62 possible characters.
  * 3.  A 7-character string in Base-62 can represent $62^7 \\approx 3.5 \\text{ trillion}$ unique IDs, which is more than enough for our scale.

#### Caching Strategy:

  * **Placement:** A distributed cache cluster (**Redis**) is placed in front of the database.
  * **Strategy:** The **Redirect Service** will use a **cache-aside** strategy for lookups.
      * When a redirect request comes in, the service first checks the cache for the `short_url_id`.
      * If the ID is found (**cache hit**), the long URL is returned immediately, achieving very low latency.
      * If the ID is not found (**cache miss**), the service queries the database, retrieves the long URL, updates the cache with the new mapping (and an expiration time), and then performs the redirect.
  * This approach ensures that frequently accessed URLs are served directly from the fast cache, minimizing database load.

#### Load Balancing:

  * **Placement:** A load balancer is placed at the very front of the system to distribute traffic to the Web Servers.
  * **Type:** A smart load balancer using an algorithm like **Least Connections** or **Weighted Round Robin** is ideal. This ensures that traffic is distributed to the least busy server, preventing any single server from being overwhelmed.

-----

### 5\. Scalability and Availability

  * **Horizontal Scalability:**
      * **Stateless Services:** The Web Servers are designed to be **stateless**. They do not store any session or application data locally. This allows us to easily add or remove servers behind the load balancer to scale with demand.
      * **Distributed Databases:** The NoSQL database is inherently distributed and sharded, allowing us to add more machines to increase storage capacity and throughput.
  * **High Availability:**
      * **Redundancy:** All critical components (Load Balancers, Web Servers, Database, Cache) are deployed with redundancy (multiple instances) across different availability zones.
      * **Failover:** If a primary database node fails, a replica can be automatically promoted to take its place. The Load Balancer will automatically detect and reroute traffic around failed Web Servers.

-----

### 6\. Potential Bottlenecks and Trade-offs

  * **Potential Bottlenecks:**

      * **Database Writes:** If the write QPS is extremely high, the database's write throughput could become a bottleneck. Using an asynchronous message queue for URL creation could mitigate this.
      * **Hot-spots:** A viral short URL could lead to a massive number of requests hitting a single cache node and a single database shard.
      * **Cache Misses:** A low cache-hit ratio would force a majority of requests to the slower database, causing a performance degradation.

  * **Trade-offs:**

      * **Consistency vs. Availability:** The design prioritizes **high availability and low latency over strong consistency**. It is acceptable if a new URL mapping takes a few seconds to propagate to all database replicas. This is a suitable trade-off for a URL shortening service.
      * **Memory vs. Latency:** The use of a distributed cache means we are trading off memory usage for significantly lower latency on redirects. This is a worthwhile trade-off given the read-heavy nature of the service.

  * **Future Improvements:**

      * Integrate a **message queue** to make the write path fully asynchronous and more resilient to database load spikes.
      * Add a dedicated **analytics service** to track clickthroughs, user agents, and other metrics without impacting the core redirection performance.
      * Implement **rate limiting** on the URL creation API to prevent abuse.
      * Use a **separate ID generation service** to prevent database bottlenecks during the creation of new URLs.


---

For a URL shortener, the choice between a 301 (Permanent) and a 302 (Temporary) redirect depends on the desired behavior and priorities: 

• 301 Redirect (Permanent): 
	• Behavior: Informs the browser and search engines that the short URL has permanently moved to the long URL. Browsers may cache this redirect, meaning subsequent requests for the short URL might directly access the long URL without contacting the URL shortening service again. 
	• Advantages: Reduces server load on the URL shortening service after the initial redirect, as browsers cache the destination. 
	• Disadvantages: Makes it harder to track clicks and analytics for the short URL after the initial redirect, as subsequent requests bypass the service. Also, if the long URL needs to be changed in the future, cached 301 redirects might lead users to the old destination until the cache expires or is cleared. 

• 302 Redirect (Temporary): 
	• Behavior: Indicates that the short URL is temporarily pointing to the long URL. Browsers are not expected to cache this redirect, so every request for the short URL will typically hit the URL shortening service. [1]  
	• Advantages: Allows for accurate and continuous tracking of clicks, referral sources, and other analytics, as every request passes through the service. Provides flexibility to update the long URL associated with a short URL without breaking cached links. 
	• Disadvantages: Increases server load on the URL shortening service compared to 301 redirects, as every request needs to be processed. 

Conclusion for URL Shorteners: 
Most URL shortening services opt for 302 redirects. This is primarily due to the importance of analytics and click tracking for short URLs. The ability to monitor how many times a link is clicked, where the clicks originate, and other data points is a core feature of most URL shorteners. While 301 redirects can reduce server load, the loss of real-time analytics often outweighs this benefit in the context of a URL shortening service. Additionally, 302 redirects offer greater flexibility for managing and updating the destination URLs. 

AI responses may include mistakes.

---
[system-design-scalable-url-shortener-service-like-tinyurl](https://medium.com/@sandeep4.verma/system-design-scalable-url-shortener-service-like-tinyurl-106f30f23a82)
[systemdesignschool.io](https://systemdesignschool.io/problems/url-shortener/solution)
[designgurus.io](https://www.designgurus.io/blog/url-shortening)
