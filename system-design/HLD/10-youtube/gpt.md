# High-Level System Design — Video Streaming Service (YouTube-like)

Below is a compact yet end-to-end design you can copy/paste and iterate on. It’s opinionated: **PostgreSQL + Redis + Kafka + S3 (object store) + OpenSearch + CDN**, ABR/HLS playback, and workers using FFmpeg.

---

## 1) Functional & Non-Functional Requirements

### Functional Requirements (prioritized, with complexities)

**Personas**

* **Viewer (P0):** browse, search, play, like, comment, subscribe, manage playlists, watch history, resume playback.
* **Creator (P0):** upload videos, set title/desc/tags/visibility/thumbnail, track basic stats.
* **Moderator/Trust & Safety (P1):** flag/review content, takedown, geo/age restrictions.
* **Advertiser/Partner (P2):** campaign targeting, ad insertion, revenue reporting.

**Features**

* **P0 Upload & Resumable Transfer** — initiate upload, multipart/resumable, integrity check, virus scan.
  *Complexities:* large files, flaky networks, idempotency, storage costs.
* **P0 Transcoding Pipeline** — multi-rendition (240p…4K), HLS/DASH packaging, thumbnails, captions ingestion.
  *Complexities:* compute heavy, queueing, retries, per-region capacity, cost.
* **P0 Metadata & Indexing** — titles, tags, categories, visibility (public/unlisted/private), language, content rating; searchable.
  *Complexities:* search relevance, spam/SEO gaming, updates propagation to index.
* **P0 Playback** — ABR playback with CDN, manifest and segment delivery, geo/age checks, resume from position.
  *Complexities:* DRM (optional), tokenized URLs, cache keys, multi-CDN routing.
* **P0 Engagement** — likes, comments (threads), views counting (exact-once-ish), subscriptions, playlists, notifications.
  *Complexities:* counters at scale, abuse detection, hot keys (“viral”).
* **P0 Search & Discovery** — keyword search, filters, sort, suggested videos, channel pages.
  *Complexities:* freshness vs. relevance, index latency, query spikes.
* **P1 Live Streaming (toggleable)** — ingest (RTMP/SRT), live transcode, low-latency HLS, live chat.
  *Complexities:* end-to-end latency, scale up/down.
* **P1 Analytics** — creator dashboards (views, watch time, CTR), per-video & per-channel aggregates.
  *Complexities:* high-volume event processing, late/duplicate events.
* **P1 Moderation & Policy** — reporting, takedowns, age/geo restrictions, hash-matching.
  *Complexities:* legal SLAs, accuracy, auditability.
* **P2 Ads & Monetization** — preroll/midroll, targeting, frequency capping, reporting.
  *Complexities:* user privacy, latency budgets.

### Non-Functional Requirements

* **Performance**

  * API p50 < 80 ms, p95 < 250 ms (metadata/search read paths).
  * Playback manifest p95 < 120 ms at edge.
  * Upload: sustained 20–100 Mbps per client (best effort).
* **Scalability**

  * Target example (see §2): 2 M DAU, \~200 k CCU streaming; 50 k uploads/day.
* **Reliability/Availability**

  * User-facing APIs 99.95%; manifest service 99.99%; pipeline once-and-only-once semantics via idempotency keys.
* **Consistency**

  * Strong: upload state, ownership/ACLs, video publish state.
  * Eventual: counters (views/likes), search index, recommendation caches.
* **Security**

  * OAuth2/OIDC (PKCE) for clients; mTLS between services; KMS-managed keys; presigned URLs for upload; tokenized CDN URLs.
* **Compliance**

  * GDPR/CCPA deletion/export; DMCA; COPPA (age gating); data residency if needed.
* **Maintainability**

  * Infra as code; golden dashboards; structured logs; SLOs + error budgets; feature flags.

**Assumptions & Scope**

* Scope covers VOD upload→publish→playback, basic engagement, search, notifications. Ads, live streaming, and full moderation: P1/P2.
* Cloud-native; single “home” region + multi-region CDN; budget moderate; team can run Kafka, OpenSearch.

**Out of scope (initial)**

* DRM/widevine/fairplay, payments, full ad stack, live chat, creator payouts.

---

## 2) Back-of-the-Envelope Estimation (explicit numbers)

**Scale Assumptions**

* **MAU:** 10 M; **DAU:** 2 M; **Peak CCU streaming:** 200 k.
* **Uploads/day:** 50 k (avg 200 MB raw).
* **Daily views:** 60 M. Avg watch time/user/day: 30 min. Avg delivered bitrate (ABR across network): 2 Mbps.

**Traffic / QPS**

* **Manifest fetches** ≈ views = 60 M/day → `~694 QPS`.
* **Metadata reads per view** (video+channel+recs ≈ 3 calls): `~2080 QPS`.
* **Search queries**: assume 10% of sessions → `~70 QPS` avg, peaks 5× → `350 QPS`.
* **Likes**: 2% of views → `~14 QPS`.
* **Comments**: 0.2% of views → `~1.4 QPS`.
* **Uploads**: 50 k/day → `~0.58 QPS` create sessions; bandwidth is the concern, not QPS.

**Storage**

* **Raw uploads**: 50k × 200 MB = **10 TB/day**. Keep originals 30 days → **\~300 TB** rolling.
* **Transcoded renditions**: 6 ladder renditions totaling \~0.8× original size → **8 TB/day**, **\~240 TB/month** new.
* **Thumbnails/captions/metadata**: small (<< 5 TB total initially).
* **Watch history & events (hot store)**: tens of TB (Cassandra/Bigtable).

**CDN Egress**

* 2 M DAU × 30 min × 2 Mbps ≈ 2 M × 450 MB ≈ **900 TB/day** → **\~27 PB/month**.

**\[CACHE\_SYSTEM] memory estimate (Redis)**

* Hot video metadata cache: top 100 k videos × \~1.5 KB each ≈ **150 MB**.
* Engagement counters cache (views/likes): + \~500 MB.
* Feed/search result caches: \~1–2 GB.
* **Start with 4–8 GB** per shard, scale horizontally.

**\[MESSAGING\_SYSTEM] throughput (Kafka)**

* View events: 60 M/day → **\~694 events/s** avg; bursts ×5 → **\~3.5 k/s**.
* Pipeline (transcode/publish) events: << 100/s.

**Rough Monthly Cloud Costs (very coarse)**

* **CDN egress 27 PB** @ \~\$0.02/GB → **\~\$540k**.
* **Object storage 600 TB** @ \$0.023/GB → **\~\$13.8k**.
* **Compute (API/Workers/DB/Search)** → **\$50–150k** depending on region/commit.
* **OpenSearch/Kafka/Redis managed** → **\$30–80k**.
* **Total ballpark:** **\~\$650–800k/mo** at this scale (egress dominates).

---

## 3) API Design (REST external, gRPC internal)

**Auth:** OAuth2/OIDC Bearer; rate-limits via API-GW (e.g., 100 req/min/user; burst 50).

### Upload & Publish

* `POST /v1/uploads` → init upload (presigned multipart URLs)
  **Req:** `{ "filename","size_bytes","mime","sha256","idempotency_key" }`
  **Res:** `{ "upload_id","video_id","parts":[{part_no,url,expires_at}], "resume_url" }`
  **Codes:** 201, 409 (idempotency), 413 (too large)
* `POST /v1/uploads/{upload_id}/complete`
  **Req:** `{ "parts":[{part_no,etag}], "checksum":"sha256" }`
  **Res:** `{ "video_id","status":"PROCESSING" }`
* `PATCH /v1/videos/{video_id}` (metadata)
  **Req:** `{ "title","description","tags", "visibility", "category_id", "language", "age_restriction" }`
* `POST /v1/videos/{video_id}/publish` → triggers index + CDN token warmup.

### Playback

* `GET /v1/videos/{id}` → metadata (cacheable)
* `GET /v1/videos/{id}/manifest.m3u8?token=...` → short-lived signed manifest URL
* `GET /v1/videos/{id}/thumbnails/{size}` → CDN
* `POST /v1/views` → `{ "video_id","position_sec","event":"start|heartbeat|complete","client_ts" }` (async accept 202)

### Search & Discovery

* `GET /v1/search?q=&filters=&cursor=&limit=` → OpenSearch backed
  **Cursor (opaque)**: base64 of `{"last_score":float,"last_id":"uuid"}`
  **Example:** `eyJsYXN0X3Njb3JlIjoxLjIzLCJsYXN0X2lkIjoiYjJi...In0=`

### Engagement

* `POST /v1/videos/{id}/like` (idempotent)
* `DELETE /v1/videos/{id}/like`
* `POST /v1/videos/{id}/comments` → `{ "text","parent_id?" }`
* `GET /v1/videos/{id}/comments?cursor=&limit=` (cursor = base64 `{"created_at":ts,"id":uuid}`)
* `POST /v1/channels/{id}/subscribe` / `DELETE .../subscribe`
* `POST /v1/playlists` / `POST /v1/playlists/{id}/items` / `GET /v1/playlists/{id}`

**Errors:** JSON `{code,message,details?}`; **Idempotency-Key** header supported for mutating endpoints.

---

## 4) High-Level Architecture

```
[Client Apps]
   | HTTPS
   v
[API Gateway/Edge] --(Auth/Rate limit)--+
   |                                     \
   v                                      \  WebSocket/SSE for notif
[API Service (REST/gRPC)] <----> [Redis Cluster]
   |            |   \                ^
   |            |    \               |
   |            |     +--> [OpenSearch]
   |            v
   |         [PostgreSQL (meta)  <->  Read replicas]
   |
   +--> [Presigned URLs] --> [Object Storage (S3)] --> [AV Scanner]
   |
   +--> [Kafka Topics: upload, transcode.*, publish, events.*]
             |                         |
             v                         v
       [Transcode Orchestrator]   [Indexing Workers]
             |                         |
       [FFmpeg Workers]         [OpenSearch Updater]
             |
          [Renditions + HLS manifests to S3] --> [CDN]
```

**Primary write (upload→publish):**

* Client → API (init) → S3 multipart → API complete → Postgres `videos.status=PROCESSING` → Kafka `transcode.requested` → Workers → renditions+manifest to S3 → Kafka `video.transcoded` → Orchestrator validates → Postgres update `PUBLISHED` → Kafka `video.published` → Indexer → OpenSearch & Redis warmup.

**Primary read (playback):**

* Client → API `GET /videos/{id}` (Redis→Postgres) → auth checks → signed manifest URL → Client pulls manifest/segments from **CDN**.

**Realtime notifications:**

* Events to Kafka → Notification service → push to Redis streams → fan-out via WebSocket/SSE; offline users get APNs/FCM.

**Resiliency points:**

* Retries with backoff (idempotency keys), DLQs per topic, circuit breakers from API→DB/Search, multi-AZ for DB/Redis, object store versioning, CDN multi-provider failover.

---

## 5) Functional Requirement Deep Dives (P0 + key P1)

> For each: **client→server flow**, **storage**, **delivery**, **edge cases**, **optional features**. Examples include Redis/Kafka/SQL/pseudocode.

### A) **Upload & Resumable Transfer** (P0) — **\[CRITICAL\_WRITE\_OPERATION]**

**Flow (plain English)**

1. Client asks API to **init upload** (filename/size/mime/sha256/idempotency\_key).
2. API creates `video_id`, `upload_id`, stores row in `uploads` & `videos(status=INIT)`, returns **presigned multipart URLs** (e.g., 5–15 MB parts).
3. Client uploads parts directly to S3; on flaky network, it **lists completed parts** and resumes missing ones.
4. Client calls **complete**; API verifies ETags & checksum, updates `videos.status=PROCESSING`, emits `kafka:transcode.requested`.
5. Orchestrator assigns worker; worker downloads original, runs **FFmpeg** ladder → uploads renditions + `master.m3u8` to S3; emits `video.transcoded`.
6. Orchestrator marks `PUBLISHED`, generates signed manifest base path, warms Redis, emits `video.published` (indexing).

**Data storage**

* `uploads(upload_id PK, user_id, video_id, state, parts_json, created_at, expires_at)`
* `videos(video_id PK, owner_id, status, title, visibility, original_key, duration_sec?, created_at, published_at?)`
* `video_renditions(video_id+rendition_id PK, resolution, bitrate, codec, object_key, size_bytes, status)`
* Indexes: `videos(owner_id)`, `videos(status)`, `video_renditions(video_id)`.
* **Redis**: `video:{id}` (hash: `title,owner,visibility,status,duration,...`, TTL 6h).

**Delivery to others**

* After publish, **Notification** to subscribers (Kafka→Notif→WS/Push).
* Search index updates make content discoverable.

**Edge cases**

* Client crash mid-upload → re-init with same `idempotency_key` → 409 returns existing `upload_id`.
* Checksum mismatch → mark upload `FAILED`, surface 422.
* Transcode failure → retry N times; after N, `videos.status=FAILED`, notify creator.

**Optional**

* Pre-upload quota checks, virus scan, content hash de-dup (server-side copy if duplicate).

**Concrete bits**

* **Kafka topics**

  * `transcode.requested` (partitions by `video_id`), retention 7 d
  * `video.transcoded`, `video.published`
  * **Payload (JSON)**

    ```json
    { "event":"transcode.requested","video_id":"uuid","original_key":"s3://bucket/o/..","owner_id":"u123","submitted_at":169... }
    ```
* **Redis**

  * `HSET video:{id} status PROCESSING` (upon complete)
  * Post-publish: `HSET video:{id} status PUBLISHED manifest s3://...`
* **Java worker (consumer) – concise**

  ```java
  @KafkaListener(topics="transcode.requested", groupId="transcode-workers")
  void handle(TranscodeReq msg) {
    if (alreadyProcessed(msg.videoId)) return; // idempotent
    MediaSpec[] ladder = Ladder.defaultSet(); // 240p..4k
    for (MediaSpec spec: ladder) {
      ffmpeg.transcode(msg.originalKey, tmpPath(spec));
      s3.putObject(renditionKey(msg.videoId, spec), tmpPath(spec));
    }
    String manifestKey = hls.pack(msg.videoId, ladder); // writes master + variants
    kafka.send("video.transcoded", new Transcoded(msg.videoId, manifestKey));
  }
  ```
* **Sequence (exact)**

  * API → Postgres `videos` INSERT → **Kafka** `transcode.requested` → Worker (FFmpeg) → S3 writes → **Kafka** `video.transcoded` → Orchestrator updates Postgres `PUBLISHED` → **Redis** warm (`video:{id}`) → **Kafka** `video.published` (indexer).

---

### B) **Playback** (P0) — **\[CRITICAL\_READ\_OPERATION]**

**Flow**

1. App requests `/videos/{id}` → API checks ACL/visibility/geo/age, returns metadata + **signed** manifest base URL.
2. Player fetches `master.m3u8` from CDN → segments (`.ts/.m4s`) follow; ABR selects rendition.
3. Client emits `views` events: `start` → periodic `heartbeat` → `complete` (202 accepted).

**Storage & retrieval**

* Redis cache-aside for `video:{id}`; fallback to Postgres.
* Manifest and segments in S3 behind **CDN** (edge cache).
* **Counters**: Redis `INCR` `veng:{id}:views:today` and Kafka event for durable aggregation.

**Delivery**

* CDN handles scale; token includes `video_id`,`exp`,`geo` claims.

**Edge cases**

* Token expired → 401; player retries manifest URL refresh.
* Region block → 451.
* Missing rendition → fallback next lower.

**Concrete**

* **Read-and-merge service (Java)** merges metadata + counters:

  ```java
  VideoView getVideo(String id) {
    Map<String,String> meta = redis.hgetAll("video:"+id);
    if (meta.isEmpty()) meta = db.fetchVideo(id); // then cache
    long views = parse(redis.getOrDefault("veng:"+id+":views:total","0"));
    return new VideoView(meta, views);
  }
  ```
* **Cursor for “related videos”**: base64(JSON) of `{"score": 12.345, "vid":"uuid"}`; next page uses `search_after`.

---

### C) **Search & Indexing** (P0)

**Flow**

* On `video.published`, an indexer reads Postgres → normalizes fields → **OpenSearch** `PUT /videos/_doc/{video_id}` with title, tags, language, channel, recency boost.
* Query: `GET /search?q=..&filters..&cursor..` using `search_after`.

**Storage**

* OpenSearch index: analyzers for languages; fields: `title^3,tags^2,description,channel,category,created_at,views_count`.
* Postgres remains SoR.

**Edge cases**

* Updates (title/visibility) publish `video.updated` → partial reindex.
* Deletes → `video.deleted` → remove doc.

---

### D) **Engagement: Likes, Comments, Views** (P0)

**Flow**

* **Like**: client POST; API writes **idempotently** to Postgres `video_likes(user_id,video_id)` (ON CONFLICT DO NOTHING), increments Redis counter; emits `like.event`.
* **Comment**: API inserts into Postgres `comments`; increments counters; emits `comment.event`; fan-out notification to owner.
* **View**: API accepts events (202) → Kafka `view.events`; stream processor dedups by `(user_id,video_id,day,session_id)` and updates aggregates.

**Storage**

* Postgres tables + Redis counters; long-term per-video aggregates (daily buckets) in Postgres or Cassandra.
* Comments pagination by `created_at DESC, id DESC` with opaque cursor.

**Edge cases**

* Double-click like → dedup by unique index.
* Comment spam → rate-limit (per user/ip) + moderation flags.

---

### E) **Subscriptions, Playlists, Notifications** (P0)

**Subscriptions**

* `subscriptions(follower_id, channel_id, created_at)` unique index;
* **Feed generation:** on publish, push `video_id` into **Redis ZSET** `feed:{follower}` with score=`published_at`. Lazy-fill on demand for cold users.

**Playlists**

* Postgres `playlists` + `playlist_items` with `position` and index for pagination.

**Notifications**

* Kafka `video.published`, `comment.event`, `reply.event` → Notif service → WebSocket/SSE for online; APNs/FCM for offline.

**Edge cases**

* Fan-out explosion: batch ZADD with pipelining, backpressure; fallback to **pull** (compute on read) for mega-channels.

---

### F) **(P1) Live (toggle)**

* RTMP ingest → Transcoder → Low-latency HLS (LL-HLS) → CDN; live chat via Redis Streams & WebSocket. (Details omitted to keep VOD focus concise.)

---

### Summary Table (flows)

| Requirement       | Client → Server                      | Server → DB/Cache                                                 | Server → Recipient                              | Notes / Offline                                    |
| ----------------- | ------------------------------------ | ----------------------------------------------------------------- | ----------------------------------------------- | -------------------------------------------------- |
| Upload            | init → presigned → upload → complete | Postgres `uploads/videos`; Redis `video:{id}` status              | Kafka `transcode.*` → workers                   | Resume via multipart listParts; idempotency by key |
| Transcode/Publish | n/a (async)                          | S3 renditions + manifests; Postgres status= PUBLISHED; Redis warm | Kafka `video.published` → indexer/notifications | DLQ on persistent failures                         |
| Playback          | GET metadata, signed manifest        | Redis cache-aside; Postgres fallback                              | CDN manifests+segments                          | Token refresh; geo/age blocks                      |
| Search            | GET /search                          | OpenSearch index; cursor search\_after                            | n/a                                             | Reindex on updates/deletes                         |
| Likes             | POST like/unlike                     | Postgres unique row; Redis counters                               | Owner notif                                     | Idempotent; anti-abuse limits                      |
| Comments          | POST comment                         | Postgres insert; Redis counters                                   | Owner notif; thread fetch                       | Cursor by (created\_at,id)                         |
| Views             | POST view events                     | Kafka `view.events`; Redis real-time counters                     | n/a                                             | Dedup by session/day                               |

---

## 6) Component — Database & Storage Deep Dive (MANDATORY)

Below, main services that **persist** data.

### 6.1 **Video Metadata Service** (SoR)

**DB Choice:** **PostgreSQL** (row-level consistency, relational joins, strong constraints). Use partitioning by `created_at` (monthly) and hash-sharding by `video_id` if needed (Citus/pg\_shard).

**Tables & Indexes**

* **videos**

  * `video_id UUID PK`, `owner_id UUID`, `status ENUM('INIT','PROCESSING','PUBLISHED','FAILED')`, `visibility ENUM('PUBLIC','UNLISTED','PRIVATE')`, `title TEXT`, `description TEXT`, `tags JSONB`, `category_id INT`, `language TEXT`, `duration_sec INT`, `original_key TEXT`, `manifest_key TEXT`, `published_at TIMESTAMPTZ`, `created_at TIMESTAMPTZ DEFAULT now()`
  * IDX: `(owner_id)`, `(status)`, `(published_at DESC)`, GIN on `tags`
* **video\_renditions**

  * `video_id UUID`, `rendition_id TEXT`, `width INT`, `height INT`, `bitrate_kbps INT`, `codec TEXT`, `object_key TEXT`, `size_bytes BIGINT`, `status TEXT`, **PK** `(video_id, rendition_id)`
* **uploads**

  * `upload_id UUID PK`, `video_id UUID`, `user_id UUID`, `state TEXT`, `parts JSONB`, `checksum TEXT`, `created_at`, `expires_at`
* **thumbnails**

  * `video_id UUID`, `size TEXT`, `key TEXT`, `width INT`, `height INT`, **PK** `(video_id,size)`

**Retention/TTLs**

* Keep originals 30 days then lifecycle to infrequent access or delete; DB rows retained.

**Queries**

```sql
-- Fetch for playback
SELECT v.*, t.key AS thumb_key
FROM videos v LEFT JOIN thumbnails t ON t.video_id=v.video_id AND t.size='hq'
WHERE v.video_id=$1 AND v.visibility IN ('PUBLIC','UNLISTED');

-- Creator's recent
SELECT video_id,title,status,published_at FROM videos
WHERE owner_id=$1 ORDER BY created_at DESC LIMIT 50;

-- Update publish
UPDATE videos SET status='PUBLISHED', manifest_key=$2, duration_sec=$3, published_at=now()
WHERE video_id=$1;
```

**Redis Cache Keys**

* `video:{id}` → Hash `{title,owner,visibility,duration,manifest,thumb,...}` TTL 6h
* `veng:{id}:views:total` → String (counter); `veng:{id}:likes:total`.

**Sharding/Partitioning**

* Time partition `videos_<YYYY_MM>`; optionally Citus distribute by `video_id`.
* Cross-shard queries: scatter-gather via coordinator; keep most lookups by `video_id`.

**Trade-offs**

* Postgres strong constraints, but heavy comment threads may push to NoSQL; we keep comments in Postgres with partitioning (below), or move high-volume to Cassandra.

---

### 6.2 **Engagement Service (Likes/Comments/Counters)**

**DB Choice:** **PostgreSQL** for likes/comments (strict uniqueness, transactions); **Redis** for hot counters; **Cassandra** (or Timescale) for long-term per-video daily aggregates if needed.

**Tables**

* **video\_likes**: `user_id UUID`, `video_id UUID`, `created_at` → **PK** `(video_id,user_id)`; Unique ensures idempotency.
  IDX: `(user_id, created_at DESC)`.
* **comments**:
  `comment_id UUID PK`, `video_id UUID`, `user_id UUID`, `parent_id UUID NULL`, `text TEXT`, `status ENUM('VISIBLE','HIDDEN')`, `like_count INT`, `created_at TIMESTAMPTZ`, `path LTREE` (optional for thread).
  IDX: `(video_id, created_at DESC)`, `(parent_id, created_at DESC)`, GIN on `path` if using ltree.

**TTL/Retention**

* None for SoR; offload deleted/hidden to archive.

**Queries**

```sql
-- Like (idempotent)
INSERT INTO video_likes(video_id,user_id,created_at)
VALUES($1,$2,now())
ON CONFLICT (video_id,user_id) DO NOTHING;

-- Comments page (top-level)
SELECT * FROM comments
WHERE video_id=$1 AND parent_id IS NULL
  AND (created_at, comment_id) < ($2,$3)
ORDER BY created_at DESC, comment_id DESC LIMIT $4;
```

**Redis**

* `ZINCRBY veng:top:day <score> <video_id>` (leaderboards)
* `INCRBY veng:{id}:views:today 1`, `INCRBY veng:{id}:likes:total 1`
* Comment pages cache: `cmt:{video_id}:p:{cursor}` JSON TTL 60–120 s.

**Sharding**

* Postgres partition `comments` by `video_id` hash or by month.

---

### 6.3 **Search Service**

**Store:** **OpenSearch**
**Index mapping (simplified)**

* `title (text, analyzer=language)`, `description (text)`, `tags (keyword)`, `channel_id (keyword)`, `category_id (int)`, `created_at (date)`, `views_count (long)`, `language (keyword)`, `visibility (keyword)`.

**Queries**

* Multi-match on `title^3, tags^2, description`, with filter `visibility=PUBLIC`, recency decay, and `search_after` pagination.

**Cursor**

* Payload: `{"last_score": <float>, "last_id": "<uuid>"}` → base64.

---

### 6.4 **Subscriptions/Feed & Notifications**

**DB:** Postgres for SoR; Redis for feed fan-out.

**Tables**

* `subscriptions(follower_id, channel_id, created_at, PRIMARY KEY(follower_id, channel_id))`
* `notifications(user_id, notif_id, type, payload_json, created_at, read_at NULL, PK(user_id, notif_id))`

**Redis**

* `ZADD feed:{user} <published_at> <video_id>`; keep last 5–10k per user.
* Eviction: trim with `ZREMRANGEBYRANK`.

---

### 6.5 **Watch History**

**DB:** **Cassandra** (or Bigtable) for large scale, time-series per user.

**Tables**

* `watch_history (user_id PARTITION KEY, watched_at CLUSTER KEY DESC, video_id, last_position_sec)`; TTL 2 years.

**Queries**

* `SELECT ... WHERE user_id=? LIMIT 100;`
* Update last position: upsert with new `watched_at`.

**Cache**

* `wh:{user}:recent` list of video\_ids TTL 1 h.

---

### 6.6 **Object Storage & CDN**

* **S3 buckets**

  * `raw-uploads/` (lifecycle 30d), `transcoded/{video_id}/...`, `manifests/{video_id}/master.m3u8`, `thumbs/{video_id}/...`
* **CDN** with tokenized paths (JWT in query or header), cache keys include `video_id`, `rendition`, `segment`.

---

## 7) Caching Strategy

* **Browser**: HTTP caching for static assets; `Cache-Control` for thumbnails.
* **CDN**: manifest TTL short (10–30 s) to allow quick takedowns; segments TTL 1–6 h.
* **Edge**: signed URLs prevent abuse; use stale-while-revalidate for manifests.
* **App-level Redis (cache-aside)**:

  * `video:{id}` TTL **6 h**; stampede protection via **mutex key** `mx:video:{id}` (10 s).
  * Engagement counters: write-behind to DB every **1–5 min** via aggregator, with **Lua** to batch INCRs.
* **DB-level**: Postgres prepared statements, connection pool, read replicas.

**Eviction**: Redis `volatile-lru`, key sizes < 4 KB. Start with **8 GB**/shard, add shards as needed.

---

## 8) Messaging & Eventing

**Kafka Topics**

* `transcode.requested` (key=`video_id`, partitions=64, retention=7d)
* `video.transcoded` (key=`video_id`, partitions=32, ret=7d)
* `video.published` (key=`video_id`, partitions=32, ret=7d)
* `events.views` (key=`video_id` or `user_id`, partitions=128, ret=3d)
* `events.likes`, `events.comments` (ret=7d)
* `notif.events` (fan-out)

**Schemas (JSON/Avro)** include `event_id`, `ts`, `idempotency_key`.

**Async vs Sync**

* Sync: user-blocking writes to SoR (videos row, like/comment insert).
* Async: transcode, indexing, counters aggregation, notifications.

**Backpressure & DLQ**

* Consumer groups per service; max poll interval tuned; exponential backoff; **DLQ** per topic with structured error payload and auto-replay tooling.

**Idempotency**

* Mutation APIs require `Idempotency-Key`; workers keep a **seen set** (`SETNX` in Redis or key-value store with TTL 24–48 h).
* Event processors dedup by `(event_id)` Bloom filter + Redis `SET` for hot dedup window.

---

## 9) Scalability, HA & Operations

* **Autoscaling**

  * API: CPU+latency targets; Workers: queue lag + CPU; FFmpeg pools scale by queue depth.
* **Multi-AZ** for API/Redis/Postgres; **Postgres**: primary + 2 replicas; `pgBouncer`.
* **Multi-region (later)**: active/active for read (CDN + caches), active/passive for writes; per-region transcode; async replication of metadata; object store cross-region replication.
* **Backups/DR**

  * Postgres PITR (WAL), daily snapshots; Redis RDB/AOF; OpenSearch snapshots; object store versioning. DR RTO 4–8 h, RPO 15 min.
* **Monitoring & Tracing**

  * RED (Rate/Errors/Duration) for APIs; Kafka lag per CG; Redis hit ratio; DB p95; CDN hit ratio; transcoding success rate; queue times.
  * Alerts: API 5xx > 1% 5 min; Kafka lag > 100k msgs 10 min; Redis hit < 80% 15 min; Transcode failure rate > 2% 10 min.
* **Runbooks**

  * Redis partition loss → switch to DB fallback, throttle traffic, rebuild keys.
  * Kafka lag spike → scale consumers, pause producers (quota), investigate slow partitions.
  * Hot video hotspot → enable request coalescing; raise CDN TTL; optionally **pre-warm** edges.

---

## 10) Security, Compliance & Privacy

* **AuthN/Z**: OAuth2/OIDC + scopes; service-to-service **mTLS**; short-lived tokens.
* **Data**: PII encrypted at rest (KMS) & in transit; presigned uploads with least privilege.
* **ACLs**: row-level checks on `videos.visibility`, `owner_id`; field redaction for private videos.
* **Compliance**: GDPR delete/export APIs; audit log of moderation actions; DMCA takedown workflow; age gating (COPPA).
* **Sanitization**: HTML escaping for comments; media scanning (malware); **WAF**; rate-limit & abuse detection; dependency & container scanning in CI.

---

## 11) Cost, Trade-offs & Roadmap

* **Cost drivers:** CDN egress ≫ compute ≫ storage.
* **Levers:** Multi-CDN negotiation, segment size tuning, origin shield, tiered caching, lifecycle policies for originals, spot instances for FFmpeg, auto-pause workers when idle.
* **Trade-offs:**

  * **Consistency vs Freshness:** counters/eventual; publish strong.
  * **Fan-out vs Pull:** fan-out for small/medium channels; pull for mega-channels.
  * **SQL vs NoSQL:** Postgres for integrity; Cassandra for massive histories.
* **Roadmap:**

  1. VOD core; 2) Recommendations (offline model + feature store); 3) Live/LL-HLS; 4) Ads insertion; 5) DRM; 6) Multi-region active/active.

---

## CRITICAL DETAILS (explicit)

### Exact Events & Cache Updates

* **Upload→Publish**

  * API INSERT videos → `kafka:transcode.requested`
  * Worker outputs → `kafka:video.transcoded`
  * Orchestrator: `UPDATE videos SET status='PUBLISHED', manifest_key=...`
  * **Redis**: `HSET video:{id} status PUBLISHED manifest s3://...`
  * Emit `kafka:video.published` → indexer updates OpenSearch; Notif fan-out.

* **Playback Read**

  * API: try `HGETALL video:{id}`; miss → `SELECT ... FROM videos WHERE video_id=?` → `HSET video:{id} ... EX 21600`
  * Merge counters: `GET veng:{id}:views:total` (if null, backfill from DB aggregate).
  * Sign and return manifest URL.

### Sample Kafka Payloads

```json
// video.published
{
  "event_id":"e-7c1",
  "event":"video.published",
  "ts":"2025-08-26T12:34:56Z",
  "video_id":"2c1d-a...f",
  "owner_id":"u-123",
  "manifest_key":"s3://media/manifests/2c1d/master.m3u8",
  "duration_sec":615,
  "visibility":"PUBLIC",
  "published_at":"2025-08-26T12:34:50Z"
}
```

### Redis Commands (pagination/counters)

* Home feed: `ZREVRANGEBYSCORE feed:{user} +inf (cursor_score] LIMIT 0 50`
  Next cursor = base64(`{"score": <last_score>, "vid":"<last_id>"}`)
* Counters: `INCRBY veng:{video}:views:today 1`; hourly flush worker aggregates to DB.

### Cursor Building Example

Payload JSON: `{"created_at":"2025-08-26T12:00:00Z","id":"b5e3..."}` → base64 **`eyJjcmVhdGVkX2F0IjoiMjAyNS0wOC0yNlQxMjowMDowMFoiLCJpZCI6ImI1ZTN...In0=`**.
On next page, query `WHERE (created_at,id) < (cursor.created_at, cursor.id)` ordered DESC.

---

## Appendix — Approaches & Recommendations

**Upload Resumability**

* **Approach 1:** S3 multipart presigned (recommended: server generates part URLs; client tracks etags).
* **Approach 2:** TUS protocol server (more control, more ops).

**Comments Storage**

* **Approach 1:** Postgres with `(video_id, created_at DESC)` + `parent_id` (simple, consistent).
* **Approach 2:** Cassandra (very large threads, linear writes) but weaker transactional semantics.
* **Recommended:** Start with Postgres + ltree or `path` string; move hot videos to cache/fan-out.

**Search**

* **Approach 1:** OpenSearch (managed, fast iteration).
* **Approach 2:** PostgreSQL full-text (simpler, less scalable).
* **Recommended:** OpenSearch.

**Transcoding**

* **Approach 1:** Self-host FFmpeg on autoscaled workers (spot-friendly).
* **Approach 2:** Cloud media services (faster to market; higher \$).
* **Recommended:** Start managed if team is small; move to FFmpeg when cost matters.

