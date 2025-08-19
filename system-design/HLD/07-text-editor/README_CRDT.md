# Collaborative Document Editing System (Google Docs–style)

> **Focus**: Deep, stepwise blueprints for the critical features (real-time co-editing, presence/cursors, comments, suggestions, access control, offline & multi-device).

---

## 1) Functional & Non-Functional Requirements (Brief)

### P0 / P1 / P2 Features

* **P0**

  * Real-time collaborative editing (low-latency text ops)
  * Presence, cursors, selections
  * Comments & mentions
  * Suggestions (“Track changes”) & version history
  * Access control & sharing (links, org roles)
  * Offline editing + conflict-free sync
* **P1**

  * Media embeds (images)
  * Rich text styles, headings, lists, tables
  * Search in doc; find/replace
  * Notifications (email/push)
* **P2**

  * Export (PDF/Docx), templates
  * AI assist (summarize, rewrite)
  * Multi-doc workspaces

### Personas & Goals

* **Author/Editor**: create, edit, share, suggest changes, resolve
* **Viewer/Commenter**: read, comment, mention
* **Admin/Owner**: manage permissions, audit, retention
* **Mobile user**: intermittent connectivity, offline-first

### Complexities (technical/business)

* Real-time convergence with **offline** support → **CRDTs** or **OT**
* Multi-device state reconciliation, idempotency, and **ordering under partitions**
* Security (tenant isolation, link sharing), audit trails & retention
* Cost: hot docs are chatty; fan-out amplification; snapshotting cadence

### NFRs (short)

* **Perf**: <150 ms end-to-end op latency P95; 20k ops/sec/region
* **Scalability**: 1M MAU, 100k CCU; horizontal shard per doc
* **Reliability**: 99.95%; multi-AZ; graceful degradation to offline
* **Consistency**: Eventual convergence for doc text; strong for ACLs
* **Security**: TLS, OAuth/OIDC, row-level tenancy, KMS-encrypted data at rest
* **Maintainability**: typed schemas, migrations, SLO-driven ownership

### Assumptions & Scope

* Web + Mobile; **WebSocket** for real-time; **HTTP/gRPC** for control paths.
* Choose **CRDT (Yjs-like RGA)** for text; snapshots for fast load; Kafka for events.
* Single org multi-tenant; multi-region active-active (CRDT friendly).

---

## 2) Back-of-the-Envelope (Optional)

* **MAU** 1M; **DAU** 250k; **Peak CCU** 100k (avg 5 docs/session → 20k active docs)
* **Ops**: hot doc \~50 ops/sec; 95p doc \~5 ops/sec → region \~200k ops/min
* **Data**:

  * Text per doc avg 100 KB; history 10× = 1 MB; 10M docs → 10 TB logical
  * Ops log: \~100B/op \* 1e9 ops/year \~ 100 GB/year compressed (per region)
* **Caches**: Redis 200 GB for hot docs (presence, last snapshot, cursors)
* **Kafka**: 3× brokers/region; ops topic 100 partitions → headroom 10×

---

## 3) Functional Requirement Deep Dives (P0 + critical P1)

> We recommend a **CRDT-based** approach (Yjs/RGA-style) for offline friendliness and multi-region. For each feature: client→server flow, storage, server→recipient, messaging, edge cases, cursors, perf, observability.

### 3.1 Real-Time Collaborative Editing (Text)

#### 3.1.A Client → Server Flow

```
Client
  └─(WS: join_room(docId, token, deviceId, clientClock, snapshotHash))
     -> Collab Gateway
          └─ AuthZ(ACL) + Route(doc shard by hash(docId))
             -> Collab Worker (Doc Room)
                 └─ Load current CRDT state (Redis/Snapshot+Ops)
                 └─ Ack join {roomStateMeta, serverClock, vectorClock}
Client
  └─ Locally apply edit -> generate CRDT delta (deltaId, deps=vectorClock)
  └─(WS: submit_op{docId, delta, deltaId, deps, clientClock, idemKey})
     -> Gateway -> Worker
        └─ Validate (schema, size, ACL, idemKey dedupe)
        └─ Merge into CRDT; advance vector clock
        └─ Append to Kafka (doc.ops) and durable ops store
        └─ Fanout broadcast (WS) to room peers (excluding origin unless echo)
        └─ Periodic snapshot check (size/time threshold)
  └─ Receive peer ops; merge locally; update UI
```

**Offline & Multi-Device**

* Client queues ops with **monotonic `clientClock`** + **idempotency key**.
* On reconnect: `(WS: resync{sinceVector})` → server computes missing ops, sends as deltas.
* Devices track **vector clock** per doc; server guards **causal delivery**.

#### 3.1.B Data Storage & Retrieval

**Why CRDT over OT**: simpler offline, no central transform, multi-region friendly. Cost: slightly larger metadata and CPU on merge—acceptable.

**Stores**

* **PostgreSQL** (metadata/ACLs)
* **S3** (snapshots)
* **Cassandra/DynamoDB** (append-only ops)
* **Redis** (hot doc state, presence, cursors, rate limits)

**Tables (PostgreSQL)**

| Table           | PK                       | Important Columns                                      | Indexes                           |
| --------------- | ------------------------ | ------------------------------------------------------ | --------------------------------- |
| `documents`     | `(doc_id)`               | tenant\_id, owner\_id, title, created\_at, updated\_at | idx(tenant\_id), idx(updated\_at) |
| `document_acl`  | `(doc_id, principal_id)` | role(enum: owner, editor, commenter, viewer)           | idx(principal\_id)                |
| `document_meta` | `(doc_id)`               | latest\_snapshot\_id, size\_bytes, deleted\_at         |                                   |

**Ops Store (Cassandra/DynamoDB)**

| Table           | PK       | Clustering       | Columns                                                                      | Notes                               |
| --------------- | -------- | ---------------- | ---------------------------------------------------------------------------- | ----------------------------------- |
| `doc_ops`       | `doc_id` | `op_ts_epoch_ms` | `op_id(uuid)`, `device_id`, `vector_clock(json)`, `delta(bytes)`, `idem_key` | TTL=90d (after snapshot compaction) |
| `doc_snapshots` | `doc_id` | `version_num`    | `snapshot_uri`, `vector_clock`, `byte_len`, `created_at`                     | Every N ops or M minutes            |

**Redis Keys**

* `collab:room:{docId}:state` → **CRDT in-memory** (optional serialized Yjs state); TTL none if active; eviction LRU after idle 30m
* `collab:room:{docId}:vc` → vector clock
* `collab:cursors:{docId}` → hash `{userId -> {pos, selStart, selEnd, ts}}` TTL 10s refresh
* `collab:presence:{docId}` → set of `{userId}` TTL 60s heartbeat
* `idem:op:{docId}:{idemKey}` → bool TTL 24h (dedupe)

**Queries (examples)**

```sql
-- ACL check
SELECT role FROM document_acl WHERE doc_id = $1 AND principal_id = $2;

-- Recent snapshots
SELECT version_num, snapshot_uri, vector_clock
FROM doc_snapshots WHERE doc_id = :doc LIMIT 1 ORDER BY version_num DESC;
```

```
// DynamoDB Get ops since vector:
PartitionKey = doc_id
Filter: op_ts > last_seen_ts   (plus vector_compare on server)
Limit: 5_000 ops
```

**Sharding**

* `shardId = murmur3(doc_id) % N` → routes to **Collab Worker**.
* Cross-shard: not needed for single doc; cross-doc queries go to Postgres.

#### 3.1.C Server → Recipient Flow (Fanout)

* **Transport**: WebSocket per client; **room** per doc on the shard.
* **Broadcast**: `op_event{docId, op_id, delta, vector_clock, serverClock}`
* **Backpressure**: per-connection send queue; drop to **catch-up mode** (send compressed state) if lag > threshold.
* **Offline**: missed ops fetched on reconnect via `resync`.

#### 3.1.D Messaging & Eventing

**Kafka Topics**

| Topic             | Partitions | Key         | Retention | Use                                    |
| ----------------- | ---------- | ----------- | --------- | -------------------------------------- |
| `doc.ops`         | 100        | `doc_id`    | 7 days    | async persistence, indexing, analytics |
| `doc.snapshot`    | 50         | `doc_id`    | 30 days   | signals snapshots created              |
| `doc.audit`       | 50         | `tenant_id` | 180 days  | ACL/admin actions                      |
| `presence.events` | 20         | `doc_id`    | 24 h      | join/leave (optional)                  |

**Flow**

```
Collab Worker -> append to Cassandra (sync path)
              -> publish Kafka(doc.ops) (async, retry with DLQ)
              -> Snapshot Worker consumes doc.ops to decide compaction
```

**Idempotency & DLQ**

* Write-ahead: check `idem:op:{docId}:{idemKey}` before merge.
* If Kafka fails → retry with exponential backoff; after N tries → DLQ `doc.ops.dlq`.

#### 3.1.E Edge Cases & Failures

* **Duplicate op**: idemKey hits → ack without re-apply.
* **Out-of-order**: CRDT merge is commutative; vector clock ensures causal gaps are filled via resync.
* **Oversized delta**: reject (413), suggest snapshot; client retries after pulling latest.
* **Split brain WS**: room stickiness via consistent hashing; detect via room epoch; older epoch closes.
* **Cache miss**: load last snapshot + ops tail; warm Redis; serve after warmup with small delay.

#### 3.1.F Pagination & Cursors (Ops / Versions)

Opaque cursor (Base64 JSON):

```json
{"docId":"d123","lastTs":1723880000123,"lastOpId":"c9a1...","ver":1}
```

* Read ops: `SELECT ... WHERE doc_id=:doc AND (op_ts, op_id) > (lastTs, lastOpId) LIMIT 500`
* Merge rule: stable sort by `(op_ts, op_id)`; dedupe by `idemKey`.
* If cursor stale (snapshot advanced), server may **skip to snapshot** then resume ops.

#### 3.1.G Performance & Scalability

* **Hot-room pinning**: single worker owns doc; actor-model mailbox; no locks.
* **Batching**: coalesce small ops into micro-batches for Kafka and fanout (≤20ms).
* **Compression**: deltas gzip; snapshots stored compressed (zstd).
* **Autoscaling**: HPA on `ops/sec`+`WS conns`; shard rebalancing via hash ring move.

#### 3.1.H Instrumentation

* Metrics: `op_latency_ms{p50,p95}`, `merge_cpu_ms`, `room_backlog`, `resync_count`, `redis_hit_ratio`, `kafka_lag`, `ws_disconnects`.
* Tracing: spans for `WS join` → `merge` → `append` → `broadcast`.

---

### 3.2 Presence, Cursors, Selections

#### 3.2.A Client → Server

* **Join**: `(WS) presence.join{docId, userId, color, name}`
* **Heartbeat**: `(WS) presence.heartbeat{docId, ts}`
* **Cursor**: `(WS) cursor.update{docId, pos, selStart, selEnd, ts}` at 250–500 ms throttle

Validation: authZ, doc membership, payload size, per-user rate limit (Redis token bucket `rl:{userId}:{docId}`).

#### 3.2.B Storage

* **Ephemeral** in Redis only.
* Keys:

  * `collab:presence:{docId} -> set(userId)` TTL 60s
  * `collab:cursor:{docId}:{userId} -> {pos, selStart, selEnd, color, ts}` TTL 10s
* No durable DB writes (privacy + cost).

#### 3.2.C Server → Recipient

* Broadcast to room peers:

  ```
  presence.event{type: join|leave, userId, displayName, color}
  cursor.event{userId, pos, selStart, selEnd, ts}
  ```
* Offline: presence times out; on reconnect client re-announces.

#### 3.2.D Messaging

* Optional: publish summarized presence changes to `presence.events` for analytics.

#### 3.2.E Edge Cases

* Zombie cursors: Redis TTL expiry cleans up.
* Multi-device: last-writer-wins by `ts`; deviceId included to disambiguate if needed.

#### 3.2.F Perf & Monitoring

* Cursor throttling client-side; server gate at 10 msgs/sec per user.
* Metrics: `presence_users`, `cursor_msgs_sec`.

---

### 3.3 Comments & Mentions

#### 3.3.A Client → Server Flow

1. `POST /v1/docs/{docId}/comments`

   * Body: `{anchor:{start,end}, text, mentions:[userId], clientId, idemKey}`
2. Server:

   * AuthZ: role ∈ {owner, editor, commenter}
   * Validate anchor vs latest CRDT text (map ranges via CRDT index)
   * Write comment row; index mentions; emit notifications async
3. Response: 201 with `commentId`

**Edits/Replies**

* `POST /comments/{commentId}/replies`
* `PATCH /comments/{commentId}` (resolve, edit)

#### 3.3.B Storage

**PostgreSQL**

| Table              | PK                           | Columns                                                                                        | Indexes                         |
| ------------------ | ---------------------------- | ---------------------------------------------------------------------------------------------- | ------------------------------- |
| `comments`         | `(comment_id)`               | doc\_id, author\_id, anchor\_json, text, status(enum\:open,resolved), created\_at, updated\_at | idx(doc\_id, created\_at)       |
| `comment_replies`  | `(reply_id)`                 | comment\_id, author\_id, text, created\_at                                                     | idx(comment\_id, created\_at)   |
| `comment_mentions` | `(comment_id, mentioned_id)` |                                                                                                | idx(mentioned\_id, created\_at) |

**Queries**

```sql
INSERT INTO comments(...) VALUES(...) ON CONFLICT DO NOTHING; -- idemKey via unique(comment_id) or side table
SELECT * FROM comments WHERE doc_id = $1 ORDER BY created_at ASC LIMIT $2 OFFSET $3;
```

**Cache**

* `cache:comments:{docId}:page:{cursor}` → list of comment summaries; TTL 5m
* Invalidate on new/edited comments via pub/sub.

#### 3.3.C Server → Recipient

* **WS broadcast**: `comment.event{type:created|replied|resolved, payload...}`
* **Notifications**: enqueue to `notify.events` for email/mobile when mentioned.

#### 3.3.D Messaging

* Kafka `notify.events` (key: mentioned\_id), DLQ on template failures.

#### 3.3.E Edge Cases

* Anchor drift after edits: store anchor as **CRDT IDs** or **text range with sticky behavior**; on render, resolve via CRDT index; fallback to nearest position.

#### 3.3.F Pagination & Cursor

Opaque cursor:

```json
{"docId":"d123","kind":"comments","createdAt":1723881000123,"commentId":"c42"}
```

Query:

```sql
... WHERE doc_id=$1 AND (created_at, comment_id) > ($2, $3)
ORDER BY created_at, comment_id LIMIT $4;
```

---

### 3.4 Suggestions (“Track Changes”) & Version History

#### 3.4.A Client → Server

* **Suggest mode** toggled in client UI; ops are tagged `suggestion=true`.
* Submit as normal CRDT delta with **metadata**:

  ```
  { delta, authorId, suggestion: { type: insert|delete|format, rationale? }, idemKey }
  ```
* Server validates role ≥ commenter (if policy allows suggestions), stamps `serverClock`.

#### 3.4.B Storage

* Suggestions are **first-class** rows referencing ops for audit.

**PostgreSQL**

| Table         | PK          | Columns                                                                                                    | Indexes              |
| ------------- | ----------- | ---------------------------------------------------------------------------------------------------------- | -------------------- |
| `suggestions` | `(sugg_id)` | doc\_id, author\_id, op\_id, status(enum\:open,accepted,rejected), created\_at, resolved\_by, resolved\_at | idx(doc\_id, status) |

**Versioning**

* Every snapshot increments `version_num`.
* `version_log(doc_id, version_num, created_by, created_at, snapshot_uri, vector_clock)`

#### 3.4.C Server → Recipient

* WS broadcast `suggestion.event{created|updated}`
* When accepted: apply **no additional text change** (already in CRDT), only status flips and UI switches from highlighted to normal.

#### 3.4.D Messaging

* `doc.snapshot` topic on snapshot create; **Retention** 30d; downstream to archival.

#### 3.4.E Edge Cases

* Accept/reject race: Postgres transaction with `WHERE status='open'`.

```sql
UPDATE suggestions
SET status='accepted', resolved_by=:uid, resolved_at=now()
WHERE sugg_id=:id AND status='open';
```

#### 3.4.F Pagination & Cursor

* Versions: cursor on `(version_num DESC)`; store opaque `{docId, ver:123}`.

---

### 3.5 Access Control & Sharing

#### 3.5.A Client → Server

* **Share Link**:

  * `POST /v1/docs/{docId}/links` body `{role: viewer|commenter|editor, expiresAt?}`
  * Returns `shareToken` (signed, scoped)
* **Invite User**:

  * `PUT /v1/docs/{docId}/acl` body `{principalId, role}`
* **Join with Link**:

  * Client presents `shareToken` in `Authorization` to open doc.

AuthN: OIDC; sessions bound to deviceId; short-lived access tokens + refresh.

#### 3.5.B Storage (Postgres)

| Table          | PK                       | Columns                                              | Indexes                        |
| -------------- | ------------------------ | ---------------------------------------------------- | ------------------------------ |
| `document_acl` | `(doc_id, principal_id)` | role, granted\_by, granted\_at                       | idx(principal\_id)             |
| `share_links`  | `(link_id)`              | doc\_id, role, token\_hash, expires\_at, created\_by | idx(doc\_id), idx(expires\_at) |

**Cache**

* `acl:doc:{docId}` → hash `{principalId -> role}` TTL 5m; bust on ACL change.
* `share:token:{sha256(token)}` → role, docId, exp TTL to expiry.

#### 3.5.C Server → Recipient

* No fanout except *optionally* `presence` of user list changes for UIs.

#### 3.5.D Messaging

* `doc.audit` append on any ACL mutation (immutable audit).

#### 3.5.E Edge Cases

* Link revoked: delete `share_links` row and Redis token; WS connections with link flag get `policy.changed` and are closed (force reauth).
* Tenant isolation: every query filtered by `tenant_id` (RLS or app-level).

---

### 3.6 Offline & Multi-Device Sync

#### 3.6.A Client → Server

* Local store keeps:

  * Last snapshot vector clock
  * Pending ops queue `[ {delta, idemKey, clientClock, deps} ]`
* Reconnect protocol:

  1. `(WS) resync{docId, sinceVector, pendingOps[]}`
  2. Server computes `missingOps = serverOps - sinceVector`
  3. Server merges `pendingOps` in causal order; applies; returns `resyncAck{applied: [...], rejected: [...], newVector}`
  4. Client drops applied from queue; re-bases UI on newVector.

#### 3.6.B Storage/Server

* **Conflict-free by CRDT**, so server can accept ops regardless of interleaving.
* Idempotency via `idemKey`; duplicates ignored.

#### 3.6.C Edge Cases

* **Clock skew**: ignore `clientClock` except for tie-breaks; rely on vector causal deps.
* **Large divergence**: server sends **compressed state vector** or fresh snapshot diff.

---

### 3.7 Notifications (mentions, shares) – P1 Critical

* API enqueues `notify.events {userId, type, docId, commentId?}`.
* Worker fan-outs email/mobile via providers; stores `notifications(userId, docId, status, created_at)` for in-app center.

---

## 3.x Protocol & Payload Examples

### WebSocket Messages (JSON)

```json
// join room
{ "type":"join", "docId":"d123", "token":"...", "deviceId":"devA", "clientClock": 102, "snapshotHash":"sha" }

// submit op
{ "type":"op.submit", "docId":"d123", "delta": "<base64-crdt-delta>",
  "deltaId":"op_7f", "deps":{"d123:us-east":102}, "clientClock":103, "idemKey":"k-103", "suggestion":false }

// op broadcast
{ "type":"op.event", "docId":"d123", "opId":"srv_9a", "delta":"...", "vector":{"d123:us-east":104}, "serverClock": 73209123 }

// cursor
{ "type":"cursor.update", "docId":"d123", "pos": 241, "selStart": 233, "selEnd": 250, "ts": 1723880450123 }
```

### Redis Commands (snippets)

```bash
# Presence join
SADD collab:presence:d123 u_42
EXPIRE collab:presence:d123 60

# Cursor update (atomic)
HSET collab:cursor:d123:u_42 pos 241 selStart 233 selEnd 250 ts 1723880450123
EXPIRE collab:cursor:d123:u_42 10

# Idempotency
SETNX idem:op:d123:k-103 1
EXPIRE idem:op:d123:k-103 86400
```

### Kafka Schemas (Avro/JSON)

```json
// doc.ops value
{
  "docId":"d123","opId":"srv_9a","deviceId":"devA",
  "vector":{"us-east":104,"eu-west":87},
  "delta":"<bytes>","idemKey":"k-103","ts":1723880450123,
  "suggestion":false,"authorId":"u_42"
}
```

---

## 3.y Pseudocode (Server)

### Collab Worker (actor per doc)

```java
class DocRoom {
  CrdtState state;        // in-memory CRDT
  VectorClock vc;
  Set<WsConn> members;
  Redis redis;
  OpsStore ops;
  KafkaProducer producer;

  void onJoin(JoinMsg m) {
    if (!Authz.canView(m.user, m.docId)) deny();
    loadIfCold(); // from Redis or snapshot+ops tail
    members.add(m.conn);
    sendStateMeta(m.conn, vc);
    touchPresence(m.user);
  }

  void onOpSubmit(OpMsg m) {
    if (!Authz.canEdit(m.user, m.docId)) deny();
    if (!redis.setnx("idem:op:"+m.docId+":"+m.idemKey, "1")) { ackDuplicate(m); return; }
    redis.expire(..., 86400);

    MergeResult res = state.merge(m.delta, m.deps);
    vc = vc.advance(localRegion(), 1);

    ops.append(m.docId, res.deltaNormalized(), vc, m.idemKey, m.deviceId, now());
    producer.send("doc.ops", key=m.docId, value=buildRecord(...));

    broadcastExcept(m.conn, new OpEvent(res.deltaNormalized(), vc));
    maybeSnapshot();
  }

  void onResync(ResyncMsg r) {
    List<Op> missing = ops.fetchSinceVector(r.docId, r.sinceVector);
    send(r.conn, new ResyncOps(missing, vc));
    for (Op pending : r.pendingOps) onOpSubmit(convert(pending));
  }
}
```

---

## 3.z Summary Table (P0/P1)

| Feature           | Client→Server                       | Server→DB/Cache                                                             | Server→Recipient                    | Messaging Events           | Notes / Offline                                  |
| ----------------- | ----------------------------------- | --------------------------------------------------------------------------- | ----------------------------------- | -------------------------- | ------------------------------------------------ |
| Real-time editing | WS `join`, `op.submit`, `resync`    | Ops to Cassandra; snapshots to S3; Redis for hot state, vector, idempotency | WS broadcast `op.event`             | `doc.ops`, `doc.snapshot`  | CRDT ensures convergence; offline queue + resync |
| Presence/Cursors  | WS `presence.join`, `cursor.update` | Redis sets/hashes, TTL                                                      | WS `presence.event`, `cursor.event` | optional `presence.events` | Throttled; ephemeral                             |
| Comments          | HTTP create/edit/reply              | Postgres `comments/*`; cache pages in Redis                                 | WS `comment.event`                  | `notify.events`            | Anchors stick to CRDT positions                  |
| Suggestions       | WS/HTTP ops with `suggestion=true`  | Postgres `suggestions`; linked to ops                                       | WS `suggestion.event`               | `doc.snapshot` on cadence  | Accept/reject toggles status only                |
| ACL/Sharing       | HTTP `share links`, `ACL`           | Postgres ACL; Redis cache & token                                           | N/A                                 | `doc.audit`                | RLS/tenancy; token revocation                    |
| Notifications     | HTTP enqueue                        | Postgres `notifications` (status)                                           | Push/email                          | `notify.events`            | Async with DLQ                                   |
| Offline Sync      | WS `resync`, upload pending ops     | Redis idem + ops store                                                      | WS `resyncOps`                      | N/A                        | Vector clocks, snapshots for catch-up            |

---

## 4) Additional Guidance (Design Choices & HA/Security)

### Storage & HA

* **Postgres**: multi-AZ, read replicas; **RLS** for tenant isolation.
* **Cassandra/DynamoDB**: multi-AZ, predictable writes for `doc_ops`.
* **S3**: versioned buckets + lifecycle to Glacier after 90 days.
* **Redis**: clustered; persistence optional (AOF off for presence).

### Multi-Region

* **Active-active** for collab with CRDTs; region tag in vector clocks.
* Doc “affinity” to nearest region; cross-region edits converge via background replication (Kafka MirrorMaker or Dynamo global tables).

### Security

* TLS 1.2+; OAuth2/OIDC; short-lived JWT access tokens (≤60 min) + refresh.
* KMS encrypted at rest; secrets via vault.
* Input limits (payload size, op rate), MIME allowlist for uploads.
* Audit log for ACL and admin operations.

### Cost & Ops

* Snapshot interval tuned (e.g., every 500 ops or 2 minutes on hot docs) to cap resync costs.
* Cold doc eviction from Redis after 30 min idle; reload on demand.
* Tiering: ops retention 7–30 days; long-term in parquet/warehouse.

---

## Appendix: Schemas (DDL snippets)

```sql
CREATE TABLE documents (
  doc_id UUID PRIMARY KEY,
  tenant_id UUID NOT NULL,
  owner_id UUID NOT NULL,
  title TEXT NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE document_acl (
  doc_id UUID NOT NULL,
  principal_id UUID NOT NULL,
  role SMALLINT NOT NULL, -- 0 viewer,1 commenter,2 editor,3 owner
  granted_by UUID NOT NULL,
  granted_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  PRIMARY KEY (doc_id, principal_id)
);

CREATE TABLE comments (
  comment_id UUID PRIMARY KEY,
  doc_id UUID NOT NULL,
  author_id UUID NOT NULL,
  anchor_json JSONB NOT NULL, -- stores CRDT anchors
  text TEXT NOT NULL,
  status SMALLINT NOT NULL DEFAULT 0, -- 0 open, 1 resolved
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX idx_share_token ON share_links(token_hash);
```

---

## Example Cursors

* **Ops list**: `base64('{"docId":"d123","lastTs":1723880000123,"lastOpId":"c9a1"}')`
* **Comments list**: `base64('{"docId":"d123","createdAt":1723881000123,"commentId":"c42"}')`
* **Versions**: `base64('{"docId":"d123","ver":37}')`

**Cursor Build**

```
function buildCursor(obj) { return base64url(JSON.stringify(obj)); }
function parseCursor(c) { return JSON.parse(base64url_decode(c)); }
```

**Read Merge Logic**

1. Parse cursor.
2. Query with `(ts,id) > (lastTs,lastId)` (or version\_num DESC).
3. Return page + nextCursor if rows == limit; else `null`.

---

## Request/Response Examples (HTTP)

**Create Comment**

```
POST /v1/docs/d123/comments
Authorization: Bearer <JWT>
Idempotency-Key: ic-7fe2

{
  "anchor": {"type":"crdtAnchor","ids":[["r1",12],["r1",15]]},
  "text": "Consider rephrasing the intro.",
  "mentions": ["u_9"]
}

201 Created
{ "commentId": "c42", "createdAt": "2025-08-18T12:34:56Z" }
```

**Create Share Link**

```
POST /v1/docs/d123/links
{ "role": "viewer", "expiresAt": "2025-09-01T00:00:00Z" }

201 { "linkId":"Lk_1", "url":"https://app/share/abc...", "expiresAt":"..." }
```

---

## End-to-End Diagram (ASCII)

```
[Browser/Mobile]
   |  WS: join/op/cursor
   v
[Edge WS GW] --(Auth, rate limit)--> [Collab Router]
   |                       |
   | route shard(docId)    v
   |                [Collab Worker (Doc Room)]
   |                    |   \-- Redis (presence, cursors, idem, hot state)
   |                    |-- OpsStore (Cassandra/Dynamo)
   |                    |-- S3 (snapshots)
   |                    \-- Kafka (doc.ops, snapshot, audit)
   |
HTTP Control (ACL, comments)
   v
[API Service] -- Postgres (ACL, comments, meta)
              -- Kafka (notify, audit)
```

---

Awesome — I expanded the **interview-friendly**, story-like explanations for **all core features** (FR1–FR5 + related P1 features). Each section is written so you can *speak directly in an interview*: starts with a short elevator summary, then a stepwise flow (client→server), how data is stored/retrieved, server→recipient behavior, offline/multi-device notes, failure cases, and one-liners you can say to close the topic. Copy-paste ready.

---

# Interview-ready explanations — full set

## FR1 — Real-time Collaborative Editing (P0)

**Elevator pitch:**
“When someone types, we send only the small change (a delta) across a persistent connection; we merge everyone's deltas using a conflict-free technique (CRDT or OT), persist them for durability, and push them to other collaborators in real time.”

### Stepwise: Client → Server (what to say)

1. Client establishes a WebSocket (or gRPC stream) to a collab gateway. The join includes `docId`, `userId`, `deviceId`, and the client’s last-known **version/vector clock**.
2. User types → client creates a **delta** (small operation: insert, delete, format) and attaches `clientOpId` (idempotency key) and `deps` (vector or version).
3. Client sends: `{docId, clientOpId, delta, deps, suggestionFlag?}` over WS.
4. Gateway authenticates (JWT/OAuth) and routes the WS to the correct **collab worker** (shard) based on `hash(docId)`.
5. Worker validates ACLs, checks `SETNX idem:op:{docId}:{clientOpId}` in Redis to dedupe.
6. Worker **merges** the delta into in-memory CRDT state (or OT transform) and advances the server vector/version.
7. Worker persists the op into an append-only ops store and publishes to `doc.ops` topic (Kafka).
8. Worker fans out the resulting op event to all active connected clients in that doc channel.

### Data storage & retrieval (short)

* **Snapshot store**: Postgres / S3 store of compressed snapshots (every N ops).
* **Ops store**: Append-only DB (Cassandra/DynamoDB) + Kafka for real-time propagation.
* **Redis**: hot doc state, idempotency keys, presence, cursors.
* Example SQL to fetch snapshot:

```sql
SELECT content_snapshot, latest_version
FROM documents WHERE doc_id = 'd123';
```

### Server → Recipient (fan-out)

* Worker sends `op.event` to every WS connection subscribed to `doc:{docId}`.
* Each client applies the op locally (CRDT merging ensures deterministic result).

### Multi-device & offline

* Each device retains `pendingOps[]` with `clientOpId`. On reconnect:

  * Client sends `resync{sinceVector, pendingOps}`.
  * Server returns missing ops and merges pending ops (idempotent merge).
* If divergence huge, server may send a snapshot + ops tail.

### Failure & edge cases (how to explain)

* Duplicate ops: deduped by `idemKey` in Redis.
* Out-of-order: CRDT handles commutativity; with OT we transform incoming ops before apply.
* Oversized op: server rejects with `413` and client asks user to perform smaller edit.

### One-line close

“Essentially: small deltas over WebSocket → merge with CRDT/OT → store ops + snapshots → fanout to clients, with offline replays for catch-up.”

---

## FR2 — Presence, Cursor & Selection Sharing (P0)

**Elevator pitch:**
“Users should see who’s online and where their cursor/selection is: light-weight ephemeral state shared via Redis and WS.”

### Stepwise: Client → Server

1. On document open, client sends `presence.join{docId, userId, name, color}` over WS.
2. Server stores `presence` in Redis:

   ```
   SADD collab:presence:{docId} {userId}
   SETEX collab:cursor:{docId}:{userId} '{pos, selStart, selEnd, ts}' 10
   ```
3. Client sends `cursor.update` events at a throttled frequency (e.g., 200–500 ms).

### Storage & TTL

* Use Redis ephemeral keys: TTL \~10s for cursor, 60s heartbeat for presence. TTL expiry acts as disconnect cleanup.

### Server → Recipient

* Broadcast presence events (`join/leave`) and periodic cursor updates via WS channel `doc:{docId}`.
* If a client disconnects unexpectedly, TTL expiry removes them and server broadcasts `leave`.

### Multi-device & offline

* Multi-device: keep deviceId with cursor (so user can have multiple cursors). If multiple devices, UI can show multiple small cursors with device labels.
* Offline: presence removed via TTL; cursor not persisted.

### Edge cases

* High-frequency cursor updates → client throttles; server applies per-user rate limit in Redis token bucket.

### Interview close

“Make presence/cursors ephemeral, store them in Redis with TTL, and broadcast via the same WebSocket channel — light, fast, and simple.”

---

## FR3 — Comments & Mentions (P0)

**Elevator pitch:**
“Comments are regular database-backed entities (durable) with anchors into the document; they are delivered in real time but also queryable via REST for offline users.”

### Stepwise: Client → Server

1. User posts comment via `POST /docs/{docId}/comments` or via WS `comment.create`.
2. Server checks ACL (commenter allowed), stores comment in Postgres:

```sql
INSERT INTO comments (comment_id, doc_id, author_id, anchor_json, text, status)
VALUES (...);
```

3. Server publishes `comment.created` to Kafka `notify.events` for mention notifications and pushes `comment.event` to live WS clients.

### Anchors

* Store anchor as CRDT-aware info: either CRDT node IDs or “sticky” range (start, end + tie-breaker). On heavy edits the anchor is resolved via CRDT APIs to map to the correct position.

### Storage

* **Postgres** for comments and replies (durable).
* **Redis cache** for recent comment pages: `cache:comments:{docId}:v{ver}` TTL 5m; provide pagination cursor for long lists.

### Server → Recipient

* Live WS broadcast to active clients; offline users load comments via REST `GET /docs/{docId}/comments?cursor=...`.

### Mentions & Notifications

* When a mention appears, server enqueues an event in `notify.events`. Notification worker sends in-app, email, or push (with DLQ handling).

### Edge cases

* Anchor invalidation: if anchor text removed, fallback to nearest position and mark comment “orphaned” until user reviews.
* Spam/abuse: rate-limit comment creation per user (Redis token bucket).

### Interview close

“Comments are durable DB objects with CRDT-aware anchors; they are real-time via WS and durable via Postgres so offline and audit queries are easy.”

---

## FR4 — Suggestions / Track Changes & Version History (P0)

**Elevator pitch:**
“Suggestions are edits tagged as proposals; they’re applied in the CRDT but stored with suggestion metadata so UI can show it and accept/reject later. Snapshots let you build a version history.”

### Stepwise: Client → Server

1. Client toggles “Suggest” mode. Edits are sent as normal ops but marked `{suggestion: true, suggestionId}`.
2. Worker merges CRDT but also writes an entry into `suggestions` table:

```sql
INSERT INTO suggestions (sugg_id, doc_id, author_id, op_id, status) VALUES (...);
```

3. UI highlights suggestion in all clients. When reviewer accepts:

```sql
UPDATE suggestions SET status='accepted', resolved_by=..., resolved_at=now()
WHERE sugg_id = ... AND status = 'open';
```

4. If the suggestion is accepted the UI removes highlight — the actual text was already merged at time of creation if you used in-line suggestion approach. If using deferred apply, the accept triggers a CRDT op that applies the delta.

### Versioning & snapshots

* Periodic snapshot strategy: create new snapshot after N ops or M minutes (e.g., 500 ops or 2 minutes).
* Snapshot entry:

```sql
INSERT INTO doc_snapshots (doc_id, version_num, snapshot_uri, vector_clock) VALUES (...);
```

* To show version history, list `doc_snapshots` sorted by `version_num` and let users preview/restore.

### Storage

* Snapshots to S3 (compressed) + metadata in Postgres/Cassandra.
* Ops needed to reconstruct version between snapshots kept in ops store; older ops compacted after snapshot.

### Edge cases

* Accept/reject race: resolve with transactional update on suggestions table (UPDATE ... WHERE status = 'open').
* Revert to older version: create a new snapshot replicating desired version and issue a CRDT state replace op (careful with concurrency).

### Interview close

“Suggestions are tagged ops with metadata and stored durably; version history is snapshots + ops tail — this allows instant preview, restore, and an audit trail.”

---

## FR5 — Access Control, Sharing, Security (P0)

**Elevator pitch:**
“Enforce permissions on every call using short-lived OAuth tokens, store ACLs in Postgres, and provide shareable links that are cryptographically signed and revocable.”

### Stepwise: Client → Server

1. For each request (WS or HTTP), validate token and enforce ACL by checking cached ACL entry `acl:doc:{docId}` in Redis.
2. ACL updates:

   * `PUT /docs/{docId}/acl` updates Postgres and invalidates `acl:doc:{docId}` cache.
3. Share links:

   * Create `share_links` record with `token_hash` and expiry. Return URL containing token.
   * Server validates token by hashing incoming token and matching DB or Redis.

### Data & queries

* `document_acl(doc_id, principal_id, role, granted_by)`
* Query example:

```sql
SELECT role FROM document_acl WHERE doc_id = $1 AND principal_id = $2;
```

* Cache: on read, populate `acl:doc:{docId}` with a small hash map TTL 5 minutes; bust on writes.

### Security Best Practices to mention

* TLS everywhere, short-lived JWTs, refresh tokens, device binding for WS sessions.
* KMS for encryption at rest, field-level encryption for sensitive data.
* Audit logs in a write-once topic `doc.audit`.

### Edge cases

* Share link revoked: delete DB row and Redis key; forcibly close active sessions that used the link and require reauth (send `policy.update` over WS).

### Interview close

“ACL checks on each request, cache for performance, and cryptographically-backed share links that can be revoked — simple, auditable, and secure.”

---

## FR6 — Offline & Multi-Device Sync (P0 — cross-cutting)

**Elevator pitch:**
“Clients buffer ops locally while offline, then replay them on reconnect with idempotency and vector clocks so changes merge cleanly.”

### Stepwise: Client behavior

1. Maintain `pendingOps[]` stored in local DB (IndexedDB on web, SQLite on mobile).
2. Each op: `{clientOpId, delta, deps}`
3. On reconnect:

   * Call `WS resync{lastKnownVector, pendingOps[]}`.
   * Server returns `missingOps[]` and a new `serverVector`.
   * Client replays/adjusts pendingOps based on server confirmation (server returns mapping of `clientOpId -> serverOpId`).

### Server behavior

* Server computes `missingOps = opsAfter(clientVector)`.
* Apply pendingOps in causal order (CRDT merging handles commutativity).
* Respond: `{applied: [serverOpId], rejected: [reasons], newVector}`.

### Conflict resolution

* Prefer CRDT for offline-friendly behavior. If OT used, server does transform sequence during apply.

### Edge cases & strategies

* **Large pending buffer**: ask client to request a fresh snapshot.
* **Clock skew**: use vector clocks, not physical timestamps for ordering.
* **Idempotency**: dedupe with `SETNX idem:op:{docId}:{clientOpId}`.

### Interview close

“Buffer locally, resync with vector clocks, apply idempotent merges — this allows smooth offline edits and conflict-free reconciliation.”

---

## FR7 — Notifications & Activity (P1)

**Elevator pitch:**
“Notifications are derived events (mentions, comments, share invites). We enqueue them and deliver by channels (in-app, email, push) with retries and DLQ.”

### Stepwise

1. App publishes `notify.events` (Kafka) on mention or share.
2. Notification worker consumes, composes messages, writes a `notifications` DB row for in-app, and calls external services for email/push (with retry and DLQ).
3. Clients poll or receive WS `notification.event` for instant in-app alerts.

### Interview close

“Keep notifications asynchronous and idempotent, separate from the critical edit path so we don’t slow down real-time editing.”

---

## FR8 — Media Embeds & Rich Text (P1)

**Elevator pitch:**
“Rich content like images is stored separately (S3) and referenced in the document as small ops; rendering resolves the reference and optionally lazy-loads content.”

### Implementation highlights

* Image upload: user uploads to auth pre-signed S3 URL; server inserts `media_ref` op into doc with `{mediaId, width, height}`.
* Media policy & virus scan asynchronous: mark as `pending` until scanned; UI shows placeholder.
* Table, lists, styles: stored as CRDT structural nodes (not plaintext).

### Interview close

“Treat media as references and offload heavy data to object store; keep ops small so edit path remains fast.”

---

## FR9 — Pagination, Cursors & History reads

**Elevator pitch:**
“Any list operation (ops history, comments, versions) uses opaque cursors containing the last seen `(version, id)` encoded as Base64 to keep the protocol stable.”

### Cursor example

```json
{"docId":"d123","lastVersion":200,"lastOpId":"op_abc"}
```

Base64 encoded: `eyJkb2NJZCI6ImQxMjMiLCJsYXN0VmVyc2lvbiI6MjAwLCJsYXN0T3BJZCI6Im9wX2FiYyJ9`

### Read flow

* Client requests `GET /docs/{id}/ops?cursor={cursor}&limit=100`.
* Server parses cursor, does `SELECT * FROM doc_ops WHERE (version, op_id) > (v, id) ORDER BY version, op_id LIMIT 100`.
* Return results + `nextCursor` if rows == limit.

### Interview close

“Opaque cursor: stable, stateless for server, and easy for client to resume.”

---

## FR10 — Scaling, HA & Operations (how to present)

**Elevator pitch:**
“Scale by sharding documents, pinning a doc to a worker, making the edit path in-memory and durable via append-only stores, and using Kafka for eventing and decoupling.”

### Key points to say

* **Sharding**: `shard = hash(docId) % N` → collab worker group owns doc.
* **Sticky WebSocket routing**: gateway routes to worker owning the shard.
* **Autoscale rules**: scale workers by `ops/sec`, `active docs`, `ws connections`.
* **Hot doc handling**: special backpressure and snapshot cadence to avoid unbounded memory.
* **Multi-region**: CRDTs enable active-active; sync via region-aware vector clocks.

### Monitoring & SLOs

* Track: op latency (p95), Redis hit rate, Kafka consumer lag, snapshot creation rate, error rates.
* Alert on: `kafka_lag > threshold`, `ws_disconnect_rate up`, `resync_rate spike`.

### Interview close

“Design for single-doc affinity + append-only durability + message bus decoupling — this lets you scale horizontally while preserving low-latency edits.”

---

## Short FAQ — Common interviewer follow-ups (short answers you can use)

* **Q: CRDT vs OT — which and why?**
  A: “CRDTs simplify offline/multi-region support because they’re mergeable without a central transform. OT can be more space-efficient but requires a central transform service and is harder for offline.”
* **Q: How to ensure low latency at scale?**
  A: “Keep edits in worker memory, use Redis for hot state & idempotency, batch ops to Kafka, and shard by doc to reduce cross-talk.”
* **Q: How do you prevent data loss?**
  A: “Append ops to durable store (Cassandra/Dynamo) synchronously or with safe-acks, plus periodic snapshots to S3 — and a replay path from Kafka.”
* **Q: How to limit fan-out explosion?**
  A: “Only broadcast to active WS connections for that doc; for large audiences use polling or summarized notifications for viewers rather than full op stream.”
* **Q: How to handle long offline sessions with conflicting edits?**
  A: “Use vector clocks + CRDT merging; if divergence is massive, send snapshot + tail so client can maintain performance.”

---
