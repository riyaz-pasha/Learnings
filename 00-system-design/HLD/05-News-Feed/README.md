# High-Level System Design: Facebook-style News Feed (Hybrid Fan-out/Pull, Ranking, Dedup, Real-time)

Below is a consolidated HLSD (v2) that merges everything we discussed, with deeper detail on **post creation**, **feed generation**, **hybrid push/pull**, **pagination**, and **cache behavior**.

---

## 1) Functional & Non-Functional Requirements

### Functional

| Feature                  | Actionable tasks                                                                                       | Priority | Complexities                                                |
| ------------------------ | ------------------------------------------------------------------------------------------------------ | -------- | ----------------------------------------------------------- |
| Post creation            | Create text/media post; virus scan; generate thumbnails; persist metadata; publish `PostCreated` event | P0       | Media pipeline latency; idempotency; abuse/malware scanning |
| Follow graph             | Follow/unfollow; visibility/ACLs; soft blocks                                                          | P0       | Graph queries at scale; privacy rules                       |
| Feed generation (hybrid) | Fan-out to followers (< **T** followers); pull celebrity (≥ **T**) on read; merge+rank; paginate       | P0       | Celebrity explosion; rank consistency; hot partitions       |
| Engagement               | Like/comment/share; counters; notifications                                                            | P0       | High write QPS; dedup; partial ordering                     |
| Real-time updates        | WebSocket/Push for new posts & engagement                                                              | P1       | Connection fan-out; backpressure                            |
| Deduplication            | Prevent duplicated items across pages/refresh                                                          | P0       | Cross-source (fan-out + celebrity pull)                     |
| Moderation               | Flagging, takedown, shadow-ban                                                                         | P1       | Legal/compliance; SLA on takedown                           |
| Search/Trending          | Full-text search; trending topics                                                                      | P2       | Freshness vs cost; spam                                     |

**Personas**: Reader (low latency), Creator (reach/analytics), Moderator (policy tools), System (cost, SLOs).

### Non-Functional

* **Perf**: P50 feed ≤ 200 ms, P95 ≤ 400 ms (server); post create ≤ 500 ms (metadata) + async media.
* **Scale**: 100M–1B MAU, 10–30× regional spikes; read\:write ≈ 10:1 to 30:1.
* **Availability**: 99.99% feed read; multi-AZ, multi-region active-active.
* **Consistency**: Eventual for feed; strong for post write & permissions.
* **Security**: OAuth2/OIDC, mTLS service-to-service, row-level ACL checks, encryption at rest/in transit.
* **Compliance**: GDPR/CCPA (erasure, export, consent), COPPA if minors.
* **Maintainability**: Microservices, strong observability, canary/blue-green.

**Assumptions & Scope**

* Scope: Core posts, feed, ranking, dedup, real-time.
* Out of scope: Ads, long-tail ML, stories/reels.
* Constraints: Cloud-first, cost-aware; configurable threshold **T** (default 1k followers) for push vs pull.

---

## 2) Back-of-Envelope Estimation (example targets)

* **Users**: 500M MAU, 120M DAU, peak CCU 8M.
* **Writes**: 15M posts/day → \~175 QPS avg, 5–10k QPS peak; likes/comments 10× posts.
* **Reads**: Feed pulls 1–2B/day → \~23k QPS avg, 300–600k QPS peak.
* **Storage**: Post metadata \~0.8 KB; feed edges in cache up to 500–1000 per user; media in blob store \~PB scale.
* **Network**: Media dominates egress; API layer \~100–200 Gbps peak.
* **Cache memory**: Redis/ZSet for feeds ≈ (avg 800 items/user × 16 B key/score overhead + IDs) → plan multi-shard clusters (1–3 TB).
* **Cost**: Managed Kafka + blob + Redis biggest line items; reduce DB by hot celebrity cache and merged-TTL cache.

(Adjust based on real telemetry.)

---

## 3) API Design (selected)

**Auth**: OAuth2 Bearer; scopes for read/write; HMAC request signing for internal calls.
**Versioning**: `/v1` path + response `X-API-Version`.
**Rate limit**: token bucket per user/app/IP.

**REST**

* `POST /v1/posts`

  * Req: `{ userId, text?, mediaIds?, visibility, clientTs }`
  * Resp: `{ postId, createdAt }`
  * Errors: 400/401/413/429/500
* `GET /v1/feed`

  * Query: `limit(≤50)`, `cursor?` (opaque), `clientCaps?`
  * Resp: `{ items:[{postId,score,seenToken}], nextCursor, newItemsHint }`
* `POST /v1/posts/{id}/like`, `POST /v1/posts/{id}/comment`
* `POST /v1/follow`, `DELETE /v1/follow/{targetId}`
* `GET /v1/notifications` (optional)
* `GET /v1/realtime/token` → WS auth

**Cursor**

* Opaque base64 of `{ts_floor, rank_floor, lastIds[], celebWatermarks{celebId→ts}}`.

---

## 4) High-Level Architecture

**Blocks**

* **Edge**: CDN (media), API Gateway (auth, WAF, rate limit).
* **Services**: PostSvc, MediaSvc, GraphSvc, FeedSvc, RankSvc, EngSvc, RealtimeSvc.
* **Data**:

  * Posts store (NoSQL wide-column, e.g., Cassandra/Scylla; or document DB)
  * Graph store (followers), either sharded SQL or key-value adjacency lists
  * **Feed cache** (Redis cluster; ZSET per user)
  * **Celebrity hot cache** (Redis ZSET per celeb)
  * Search index (OpenSearch/ES)
  * Analytics lake (S3+Spark/BigQuery)
* **Messaging**: Kafka topics (`post.created`, `engagement.*`, `moderation.*`, `graph.*`).
* **Realtime**: WS fan-out tiers / Push gateways.

**Protocols**: HTTP/JSON; gRPC internal; Kafka for events; WS for live updates.

**Data Flow (read)**

1. Client → `/feed?limit&cursor`
2. FeedSvc:

   * Read **fan-out cache** slice via cursor
   * Pull celebrity posts via **hot celeb cache**/**posts DB** with same cursor filters
   * Merge, rank, dedup → build page; issue **nextCursor**
3. Return; notify RealtimeSvc to update unseen counters if needed.

---

## 5) Functional Deep Dives ⭐

### A) Post Creation (Write path)

**Components**: API GW → PostSvc → MediaSvc (async) → PostsDB → Kafka(`post.created`) → FeedWorker → Redis(Feed) / Redis(Celeb)

**Steps**

1. Validate (ACLs, text length), assign `postId`, write metadata (strong consistency).
2. Media processing async (thumbnail/transcode); update post when ready.
3. Emit `post.created` (idempotent key = `postId`).
4. **FeedWorker** consumes:

   * Get author follower count from GraphSvc.
   * **If followerCount < T (e.g., 1000)** → **fan-out**: for each follower shard, `ZADD feed:{uid} score=rankSeed postId` (include packed metadata: authorId, ts, type).
   * **Else (celebrity)** → **skip fan-out**; push into `celeb:{authorId}:posts` ZSET (size-bounded) for hot reads.
5. RealtimeSvc optionally notifies online followers (rate-limited, sampled).

**Complexities**

* Backpressure on fan-out; use batching per shard (e.g., `ZADD` pipeline).
* Idempotency (consumer group replays).
* Privacy: compute visible audience once (block lists, groups).

**Failure handling**

* DLQ for partial fan-out; compensating job to fill gaps.
* Post deletion emits `post.deleted` → remove from caches.

---

### B) Feed Generation (Read path; 20 items/page)

**Components**: FeedSvc, RankSvc, Redis(Feed), Redis(Celeb), PostsDB, GraphSvc

**First page**

1. Read **fan-out cache** slice (`ZRANGE/ZREVRANGEBYSCORE` with cursor).
2. Fetch **celebrityIds** for user from GraphSvc (cached).
3. Pull celebrity posts:

   * Prefer **hot celeb ZSET** per celeb, **bounded** window (e.g., last 5–10 each) and **filter `< cursor.ts`** if provided.
   * If miss, query PostsDB by `(authorId IN celebIds AND createdAt < cursor.ts)` with per-author limit & global cap (e.g., 50).
4. **Merge & Rank**:

   * Merge two lists; **RankSvc** computes score = f(recency, affinity, quality, diversity).
   * **Dedup** by `postId` (set) and by semantic duplicates if needed.
5. **Select top `limit`**, build **nextCursor**:

   * Store watermarks (per-source & per-celebrity last seen `ts`/`postId`).
6. Return; optionally write **temporary merged feed** `merged:{user}` ZSET with TTL 30–60 s to speed scrolling.

**Subsequent pages**

* Use **cursor**:

  * Fan-out: fetch items **older than cursor** from `feed:{user}`.
  * Celebrities: fetch items **older than celeb watermark** from hot cache/DB.
  * Merge, rank, dedup (skip previously seen IDs via `seenToken` bloom/bitset for session), return, update cursor.

**Real-time interleaving**

* If new posts arrive while user scrolls:

  * They won’t disturb **older pages** due to cursor semantics.
  * Client may show “N new posts” banner; on refresh, page 1 re-materializes with latest head.

---

## 6) Component Deep Dive

### Data Storage

* **Posts DB**: NoSQL (Cassandra/Scylla)

  * Partition key: `(authorId)`; clustering: `(createdAt DESC, postId)`
  * Secondary index/search for keyword via Search cluster
* **Graph**: KV lists `followers:{authorId}` and `following:{userId}` (sharded), plus count caches.
* **Feed cache (fan-out)**: Redis ZSET `feed:{userId}`; **score** = composite rank seed (e.g., `ts * α + edgeWeight * β`).

  * Keep **top N=500–1000**; oldest evicted with `ZREMRANGEBYRANK`.
* **Celebrity hot cache**: Redis ZSET `celeb:{authorId}:posts` top 200–500.
* **Merged cache (optional)**: `merged:{userId}` ZSET TTL 30–60 s.

**Sharding**

* Redis: consistent hashing per `userId`/`authorId`; avoid celebrity hotspots by keeping celeb cache separate.
* Kafka: partition by `authorId` to keep order per author; consumer groups by follower-shard.

**Cross-shard reads**

* Fan-out is local per user shard.
* Celebrity pulls aggregate multiple celeb shards → use scatter-gather with **global cap** and timeout budget.

### Caching Strategy

* **Cache-aside** everywhere.
* **Write-around** for posts (DB is source of truth, caches filled by workers).
* **TTL & size bounds**: feed ZSET size-bound; celeb ZSET size-bound; merged ZSET time-bound.
* **Stampede control**: request coalescing per user+cursor key; jittered TTLs.

### Load Balancing

* L7 global → regional L7 → service pods; least-load + outlier ejection.
* Sticky only for WS; APIs stateless.

### Messaging/Eventing

* Kafka topics: `post.created`, `post.deleted`, `engagement.created`, `follow.changed`.
* Retries with exponential backoff; DLQ + compactor job.

---

## 7) Scalability & Availability

* **Horiz scale**: Stateless FeedSvc/RankSvc; Redis clusters scale by shards; Posts DB by ring nodes.
* **HA**: Multi-AZ quorum for Redis/DB; cross-region active-active with per-region write preference; CRDT or async replication for caches.
* **DR**: Hourly snapshots (DB, Redis RDB/AOF), warm standby region; RTO < 30 min.
* **Observability**: RED (rate, errors, duration) on APIs; SLI/SLO per endpoint; distributed tracing across read path; Kafka lag dashboards.
* **Protection**: Circuit breakers to PostsDB; bulkhead celebrity fetch; adaptive rate limits; queue backpressure.

---

## 8) Advanced Considerations

* **Security**: Input sanitation, media AV scan; ABAC for visibility; audit logs; secrets via KMS; PII tokenization.
* **Performance**: Batching ZADD on fan-out; binary post summaries in cache; pre-computed affinity features; SIMD scoring in RankSvc.
* **Ops**: Progressive delivery (canary); config flags for **T**, per-celeb caps, score weights; capacity planning from Kafka lag + Redis memory headroom.

---

## 9) Bottlenecks, Trade-offs, Futures

* **Hotspots**: Mega-celeb new post → DB/caches spike. Mitigate with celeb hot cache + global caps + partial sampling.
* **Trade-offs**:

  * **Fan-out vs pull**: latency/cost vs complexity; hybrid balances both.
  * **Consistency vs freshness**: cursor stabilizes pages; real-time banner for head freshness.
  * **Memory vs QPS**: larger per-user feed cache lowers DB hits but raises Redis cost.
* **Future**:

  * Per-user pre-ranking jobs (near-line) for top-K head.
  * Learned re-ranking on device.
  * Federated graph/features; on-box compression of cache payloads.

---

## 10) Tech Stack Recommendations

* **Lang/Framework**: Java/Kotlin (Spring Boot) or Go for services; Python for Rank experiments.
* **Data**: Cassandra/Scylla (posts), Redis Cluster (feed/celeb/merged), OpenSearch (search), S3 (media), Snowflake/BigQuery (analytics).
* **Messaging**: Kafka (MSK/PubSub).
* **Realtime**: WS gateways (Nginx+uWSGI/Envoy+Go), FCM/APNs for mobile push.
* **CI/CD**: GitHub Actions, ArgoCD, Terraform; Blue-Green + canary.
* **Obs**: Prometheus, Grafana, Tempo/Jaeger, Loki; SLO via Sloth.

---

## Key Parameters (tunable)

* **T** (fan-out threshold): default **1000** followers.
* **Per-user feed cache size**: **500–1000** items.
* **Per-celeb hot cache size**: **200–500** items.
* **Per-request celeb cap**: **20–50** total, **≤5–10** per celeb.
* **Merged cache TTL**: **30–60 s**.
* **Page size**: up to **20–50**.

---

## Algorithmic Summary (Read Path Pseudologic)

```
items = []
fanout = Redis.ZFetch("feed:{user}", cursor, limit*X)  // X≈1.0–1.5
celebIds = GraphSvc.getCelebFollowing(user)

celebCandidates = []
for c in sample(celebIds, maxCelebAuthors):
    celebCandidates += CelebCache.fetch(c, olderThan=cursor.ts, cap=perCelebCap)
    if miss: celebCandidates += PostsDB.fetch(c, olderThan=cursor.ts, cap=perCelebCap)

candidates = dedup(fanout + celebCandidates)
scored = RankSvc.score(candidates, user)
page = topK(scored, limit)

nextCursor = buildCursor(page.tail, watermarks(fanout, perCeleb))
return page, nextCursor
```

---


I am able to provide a detailed breakdown of the post creation and feed generation steps, including key queries and logic. However, I cannot execute code or generate images.

-----

### 1\. Post Creation Flow (Write Path)

The post creation process is a crucial write-heavy operation designed for high availability and to trigger the fan-out mechanism. It is a multi-step process that ensures data consistency and initiates the feed generation for followers.

1.  **Client Request:** A user submits a post request from their client device. The request includes the user's ID, post content (text), and a list of media attachments.

      * **API Call:** `POST /v1/posts`
      * **Request Body:**
        ```json
        {
          "userId": "user_123",
          "text": "My new post!",
          "mediaIds": ["media_A", "media_B"],
          "visibility": "public"
        }
        ```

2.  **Post Service Processing:** The request is routed to the Post Service. This service performs initial validation, assigns a unique post ID, and writes the post's metadata to a highly-scalable, wide-column NoSQL database like Cassandra.

      * **Database Query (Cassandra):**
        ```sql
        INSERT INTO posts (post_id, user_id, text, media_ids, created_at, visibility)
        VALUES ('post_456', 'user_123', 'My new post!', ['media_A', 'media_B'], now(), 'public');
        ```
      * **Asynchronous Media Handling:** The Post Service doesn't wait for media processing. It hands off the media IDs to a Media Service, which handles tasks like virus scanning, thumbnail generation, and storing the media in a blob store (e.g., AWS S3). The post's metadata is updated later when the media is ready.

3.  **Event Publishing (Kafka):** After the metadata is written, the Post Service publishes an event to a Kafka topic named `post.created`. This event acts as the trigger for the fan-out process.

      * **Event Payload:**
        ```json
        {
          "postId": "post_456",
          "userId": "user_123",
          "createdAt": 1678886400,
          "isCelebrityPost": false
        }
        ```

4.  **Fan-out Logic (Hybrid Push/Pull):** A dedicated **Feed Worker** service consumes events from the `post.created` Kafka topic. It determines whether to use the push or pull model based on a configurable follower threshold, **T**.

      * **Push Model (Follower Count \< T):** The worker fetches the list of followers for the post's author from the Graph Service. For each follower, it directly adds the new post to their feed in a Redis cache. This makes feed retrieval instantaneous for these users.
          * **Cache Update Query (Redis):**
            ```redis
            ZADD feed:{follower_id} <timestamp> <postId>
            ```
      * **Pull Model (Follower Count \>= T):** If the author is a celebrity, the worker does not push the post to all followers' feeds. Instead, it adds the post to a separate **Celebrity Cache** for that specific celebrity. This prevents a fan-out storm and massive write amplification.
          * **Cache Update Query (Redis):**
            ```redis
            ZADD celeb:{authorId}:posts <timestamp> <postId>
            ```

-----

### 2\. Feed Generation Flow (Read Path)

The feed generation process prioritizes low latency and personalization. The core logic is to check the cache first and, on a miss, build the feed on-the-fly using the hybrid model.

1.  **Client Request:** A user requests the first page of their feed.

      * **API Call:** `GET /v1/feed?limit=20`

2.  **Feed Service Cache Check:** The Feed Service first checks the Redis cache for a pre-computed feed for the user.

      * **Cache Query (Redis):**
        ```redis
        ZRANGE feed:{userId} 0 19 WITHSCORES
        ```

3.  **Cache Hit:** If the cached feed exists, the Feed Service retrieves the post IDs and their scores, fetches the full post details from the Posts Database, and returns the response. This is the fastest path and handles the majority of requests.

4.  **Cache Miss (On-the-Fly Generation):** If the cache is empty or expired, the Feed Service triggers a complex on-the-fly generation process.

      * **Step 1: Fetch from Sources:**

          * **Push-based Posts:** The service fetches a list of recent posts from the user's pre-computed feed (populated by the fan-out workers).
          * **Pull-based Posts:** It queries the Graph Service to get a list of celebrities the user follows. For each celebrity, it pulls their latest posts from the **Celebrity Cache** or directly from the Posts Database if not found in the cache.

      * **Step 2: Merge and Rank:** The two lists of posts (push and pull) are merged. The combined list is sent to a **Ranking Service**, which scores each post based on factors like recency, engagement, and user affinity.

      * **Step 3: Deduplication:** A deduplication mechanism is applied to ensure a user does not see the same post twice if it was shared by both a regular user and a celebrity.

      * **Step 4: Cache and Respond:** The final, ranked list of post IDs is stored back in the user's Redis feed cache with a TTL (e.g., 5-10 minutes). The first page of the feed is returned to the user.

5.  **Pagination:** Subsequent requests for the next page of the feed (`GET /v1/feed?limit=20&cursor=...`) are handled by simply reading the next slice of posts from the already cached, ranked list, ensuring high performance for scrolling.


---

You’re 95% there. Below is a tightened, **production-grade version** of your two flows with the tricky parts nailed: **where we cache, how we paginate, how we cap celebrity fetches, dedup, and what we never overwrite.**

# 1) Post Creation (Write Path) — Hybrid Fan-out/Pull

**API**

```
POST /v1/posts
{ userId, text?, mediaIds?, visibility, clientTs }
→ { postId, createdAt }
```

**Steps**

1. **Validate & persist metadata** (strong consistency)

   * DB (e.g., Cassandra/Scylla)

   ```sql
   INSERT INTO posts_by_author (author_id, created_at, post_id, visibility, text, media_ids)
   VALUES (:authorId, :createdAt, :postId, :visibility, :text, :mediaIds);
   INSERT INTO posts_by_id (post_id, author_id, created_at, ...) VALUES (...);
   ```

   * Media pipeline async (scan, transcode, thumbnails), then update post.

2. **Publish event** (idempotent; key = postId)

   * Kafka `post.created`:

   ```json
   { "postId":"p456","authorId":"u123","createdAt":1678886400,"visibility":"public" }
   ```

3. **Feed worker consumes** (idempotent consumer)

   * Get follower count (cached) and ACL scope.
   * **If followers < T (e.g., 1000) → Push fan-out:**

     * Batch/pipeline per follower-shard:

     ```redis
     // score = rankSeed (e.g., epoch_ms or blended score)
     ZADD feed:{followerId} <score> p456
     // enforce size bound
     ZREMRANGEBYRANK feed:{followerId} 0 -1001
     ```
   * **Else (celebrity) → Pull path only:**

     ```redis
     ZADD celeb:{authorId}:posts <score> p456
     ZREMRANGEBYRANK celeb:{authorId}:posts 0 -501   // keep last 500
     ```
   * **Never** push celebrity posts to every follower.
   * On delete/edit → emit events; workers remove/update cache entries.

**Notes**

* Batch follower fetches; shard by followerId; apply backpressure.
* Producer idempotence + consumer exactly-once-effect (idempotent upserts to Redis) prevents duplicates.
* Visibility: compute allowed audience once; respect blocks/soft blocks.

# 2) Feed Generation (Read Path) — Merge, Rank, Dedup, Cursor

**Key principle:** Maintain **two base caches** you never overwrite:

* `feed:{userId}` → **fan-out only** (friends/normal users)
* `celeb:{authorId}:posts` → **per-celebrity hot posts**
* Optional **ephemeral merged cache** `merged:{userId}:{cursorHash}` (TTL 30–60s) to speed rapid scrolling. Don’t store long-lived ranked lists; they go stale.

**First page (limit = 20)**

1. **Read fan-out slice** (over-fetch a bit for ranking/diversity; X≈1.5)

   ```redis
   // if no cursor, use +inf as head
   ZREVRANGEBYSCORE feed:{userId} +inf -inf WITHSCORES LIMIT 0 30
   ```

2. **Determine celebrity set** user follows (cached list).
   Pull candidates with **caps** to protect DB:

   * **Global cap per request**: e.g., ≤50 celeb candidates total
   * **Per-celeb cap**: e.g., ≤5–10 posts each
   * Fetch from celeb cache; fallback to DB **per author** (avoid giant `IN`):

   ```redis
   ZREVRANGEBYSCORE celeb:{authorA}:posts +inf -inf LIMIT 0 8
   ZREVRANGEBYSCORE celeb:{authorB}:posts +inf -inf LIMIT 0 8
   ...
   ```

   *(If cache miss, DB query per celeb: `SELECT ... WHERE author_id=? AND created_at < ? ORDER BY created_at DESC LIMIT ?`)*

3. **Merge + rank**

   * Rank service scores by recency, affinity, quality/engagement, diversity (author & media mix).
   * **Dedup**:

     * Exact: `postId` set
     * Optional soft-dedup: canonical URL/content hash to prevent reshare spam

4. **Select top `limit`** and **emit cursor** (opaque):

   ```json
   {
     "tsFloor": 1678886000,
     "rankFloor": 9.42,
     "lastIds": ["p123","p456"],
     "celebWatermarks": { "authorA":1678886200, "authorB":1678886100 }
   }
   ```

   * Optionally write ephemeral `merged:` ZSET with TTL 30–60s so Page-2 is a cheap ZRANGE.

**Subsequent pages**

* Use **cursor** to make older pages stable and avoid dupes:

  * **Fan-out**:

    ```redis
    ZREVRANGEBYSCORE feed:{userId} (tsFloor -inf WITHSCORES LIMIT 0 40
    ```

    *(Note the parenthesis → exclusive of tsFloor)*
  * **Celebrities** (per author using per-author watermarks):

    ```redis
    ZREVRANGEBYSCORE celeb:{authorA}:posts (celebTsA -inf LIMIT 0 8
    ```
* Merge → rank → dedup against **seen set** (session Bloom/bitset) → return 20 → update cursor (watermarks move older).

**Real-time arrivals while paginating**

* Don’t disturb older pages (cursor makes them immutable).
* Client shows “N new posts” banner; on refresh of page-1, we rebuild head with newest items.

## What to Store & How Much

* `feed:{userId}`: **top 500–1000** items (size-bounded, not time-TTL).

  * Remove oldest on insert overflow (`ZREMRANGEBYRANK`).
* `celeb:{authorId}:posts`: **top 200–500** per celeb (size-bounded).
* `merged:{user}:{cursorHash}`: ephemeral ranked page window (TTL 30–60s) for smooth scroll.
* **Post details cache**: `post:{postId}` in Redis/KV (binary/protobuf) so we don’t hammer DB for hydration.

**Do we paginate on cache?**
Yes—**always** via `ZREVRANGEBYSCORE` (score = rankSeed/timestamp). Cursor = min score returned. For ephemeral merged, you can index-paginate (`ZRANGE 0..19`, `20..39`) during its short TTL window.

## Guardrails & Gotchas (important)

* **Never overwrite** the long-lived `feed:{userId}` with merged (fan-out+celeb) results. Keep sources separate.
* **Cap celebrity work** rigorously: global per-request cap and per-author cap; scatter-gather with a strict time budget (e.g., 20–30 ms).
* **Follower threshold T** is dynamic (feature-flagged). Tune by cost/latency.
* **Idempotency everywhere** (Kafka keys, Redis upserts).
* **Deletes/ACL changes**: propagate `post.deleted`/`visibility.changed`; remove from `feed:*` and `celeb:*`.
* **Hot celebs**: cache warms via write path; background refresher maintains top window.
* **DB protections**: No giant `IN` with 1000 celebs—fan out the reads per celeb (parallel + capped).

## Minimal Pseudocode (read path)

```python
def get_feed(user, limit=20, cursor=None):
    ts_floor = cursor.ts if cursor else +inf

    fanout = redis.zrevrangebyscore(f"feed:{user}", ts_floor, -inf, withscores=True, start=0, num=int(limit*1.5))

    celeb_ids = graph.get_celeb_following(user, cap=50)  # global cap
    celeb_candidates = []
    for c in celeb_ids[:maxCelebAuthors]:
        c_ts = cursor.celebWatermarks.get(c, +inf) if cursor else +inf
        rows = redis.zrevrangebyscore(f"celeb:{c}:posts", c_ts, -inf, start=0, num=perCelebCap)
        if not rows:
            rows = db.fetch_celeb_posts(c, older_than=c_ts, limit=perCelebCap)
        celeb_candidates.extend(rows)

    candidates = dedup_ids(fanout + celeb_candidates, seen=session_seen(user))
    scored = rank.score(user, candidates)
    page = topk(scored, limit)
    next_cur = build_cursor(page, fanout, celeb_candidates)
    return hydrate(page), next_cur
```

Alright — here’s a **high-level database design overview** for the entire **hybrid push/pull feed system** we’ve been discussing, including **services**, **tables**, **primary/secondary indexes**, **sharding strategy**, and **database choice reasoning**.

---

## **1. Post Service**

**Purpose:** Stores post metadata and content.

**DB Choice:** **Cassandra** (or DynamoDB)

* **Reasoning:**

  * High write throughput for millions of posts/day.
  * Wide-column store ideal for time-sorted queries.
  * Tunable consistency for balancing latency vs. durability.
  * Easy horizontal scaling with sharding by partition key.

**Table: `posts`**

| Column            | Type           | Notes                             |
| ----------------- | -------------- | --------------------------------- |
| **post\_id** (PK) | UUID           | Unique per post, primary key      |
| user\_id          | UUID           | Author of the post                |
| created\_at       | Timestamp      | Used for ordering                 |
| text              | Text           | Content text                      |
| media\_ids        | List<Text>     | Media references                  |
| visibility        | Enum           | public / private / followers-only |
| engagement\_meta  | Map\<Text,Int> | likes, comments count             |

**Indexes:**

* **Primary Key:** `(post_id)` — unique identifier
* **Secondary Index / Materialized View:** `(user_id, created_at DESC)` → to fetch posts per user quickly.

**Sharding Logic:**

* **Partition Key:** `post_id % N` (or hashed user\_id)
* Rationale: Evenly distributes writes, avoids hotspotting if one user posts a lot.

---

## **2. Graph Service** (Followers/Following)

**Purpose:** Stores social graph relationships.

**DB Choice:** **MySQL / Postgres** (for transactional consistency)

* **Reasoning:**

  * Follower relationships require strong consistency for follow/unfollow actions.
  * Joins aren’t needed much, but indexed lookups are frequent.
  * RDBMS is simpler for many-to-many mapping tables.

**Table: `followers`**

| Column                 | Type      | Notes                     |
| ---------------------- | --------- | ------------------------- |
| **user\_id** (PK1)     | UUID      | The person being followed |
| **follower\_id** (PK2) | UUID      | The person following      |
| followed\_at           | Timestamp |                           |

**Indexes:**

* **Primary Key:** `(user_id, follower_id)`
* **Secondary Index:** `(follower_id)` — to find "who I follow".

**Sharding Logic:**

* Shard by **user\_id** (person being followed).
* Rationale: Keeps all a user’s followers in the same shard for quick fan-out.

---

## **3. Feed Cache (Fan-out Model)**

**Purpose:** Stores precomputed feeds for non-celebrities (push).

**DB Choice:** **Redis Sorted Sets**

* **Reasoning:**

  * Low latency retrieval for timeline.
  * Sorted set keeps posts ordered by timestamp/score.
  * Supports pagination with range queries.

**Key Pattern:**
`feed:{user_id}` → Sorted Set of `(score=created_at, value=post_id)`

**Sharding Logic:**

* Redis Cluster, sharded by hash(`user_id`).
* Rationale: evenly distributes load, keeps a user's feed local to one shard.

---

## **4. Celebrity Cache (Pull Model)**

**Purpose:** Stores latest posts from celebrities.

**DB Choice:** **Redis Sorted Sets** (short TTL)

* **Reasoning:**

  * Hot data (recent celebrity posts) stored for quick pull on feed request.
  * TTL ensures we fetch from primary DB if stale.

**Key Pattern:**
`celeb:{user_id}:posts` → Sorted Set (score = created\_at)

**Sharding Logic:**

* Redis Cluster, shard by celebrity\_id.

---

## **5. Ranking Service Data**

**Purpose:** Stores features & scores for ranking posts.

**DB Choice:** **ElasticSearch** (for searching + scoring) or **Feature Store** (like Redis/Mongo)

* **Reasoning:**

  * Ranking needs quick lookups on engagement & relevance features.
  * ES can do fast aggregation & sorting.

**Table (ES Index) `post_features`**

| Field             | Notes                   |
| ----------------- | ----------------------- |
| post\_id          | ID of post              |
| recency\_score    | Decay score             |
| engagement\_score | Based on likes/comments |
| affinity\_score   | User-post relevance     |

**Primary Key:** post\_id (ES doc ID)
**No sharding logic needed manually** — ES auto-shards index.

---

## **6. Media Service**

**Purpose:** Stores media references (images/videos).

**DB Choice:** **S3 (blob store)** + **Postgres metadata table**

* **Reasoning:**

  * Binary media in S3 for durability.
  * Metadata in Postgres for transactional consistency.

**Table: `media`**

| Column             | Notes       |
| ------------------ | ----------- |
| **media\_id** (PK) | UUID        |
| owner\_id          | UUID        |
| type               | image/video |
| url                | S3 path     |
| created\_at        | Timestamp   |

---

## **End-to-End Sharding Reasoning**

* **Posts DB:** Shard by user\_id or post\_id hash → even write distribution.
* **Followers Table:** Shard by user\_id (keeps follower list local).
* **Redis Feed Cache:** Shard by user\_id → ensures read locality.
* **Celebrity Cache:** Shard by celeb\_id → each celeb’s posts stored together.
* **Ranking ES Index:** Auto-sharded by ES.

---

Yes — in a typical **followers table**, we maintain **both records** explicitly.

If **A follows B**, that’s one row:

```
(user_id = B, follower_id = A)
```

If **B follows A**, that’s another row:

```
(user_id = A, follower_id = B)
```

This is because:

* **Following is a directed relationship**, not symmetric.
* If you only store one row, you’d have to infer mutual follows by checking both directions every time, which is slower and more complex.
* Keeping both records makes “Who follows me?” and “Who do I follow?” queries O(1) index lookups.

---

### **Index Handling**

Given table:

```sql
CREATE TABLE followers (
    user_id UUID NOT NULL,
    follower_id UUID NOT NULL,
    followed_at TIMESTAMP NOT NULL,
    PRIMARY KEY (user_id, follower_id)
);
```

We typically have:

1. **Primary Key Index** → `(user_id, follower_id)`

   * Used for: “Get all followers of X” → `WHERE user_id = ?` (range scan).
2. **Secondary Index** → `(follower_id, user_id)`

   * Used for: “Get all people X follows” → `WHERE follower_id = ?`.

Both indexes handle A→B and B→A cases **independently**.
So if A follows 1M people, all those rows live in the shard for `follower_id = A` in the secondary index.

---

### **Storage & Sharding Impact**

* With both A→B and B→A stored, **storage cost doubles** compared to undirected edges.
* **Sharding**:

  * Primary table is sharded by **user\_id** so all of a user’s followers are in one shard.
  * Secondary index is sharded by **follower\_id** so all of a user’s followings are in one shard.

---
