**Task:** Design a scalable system for `[SYSTEM_NAME / PROBLEM_STATEMENT]`.

**Instructions for LLM:**

* Produce fully structured **Markdown output**.
* Focus **heavily on Functional Requirement Deep Dives (point 5)**.
* Include **stepwise flows, DB schemas, cache commands, messaging events, pseudocode, cursors, and multi-device/offline handling** for every critical requirement.
* Integrate **NFRs, DB/storage, caching, messaging, scalability, HA, security** within the deep dive where relevant.
* Use ASCII diagrams, tables, and examples.
* Make output **copy-paste ready**.

---

## 1. Functional & Non-Functional Requirements (Brief)

* List core features, prioritized P0/P1/P2.
* Define user personas & goals.
* Highlight technical/business complexities.
* Include short NFRs summary: performance, scalability, reliability, consistency, security, maintainability.
* Assumptions & scope (brief).

---

## 2. Back-of-the-Envelope Estimation (Optional)

* Scenario: MAU, DAU, CCU, data size, QPS.
* Rough cache/messaging sizing & cost estimation.

---

## 3. Functional Requirement Deep Dives (P0 + critical P1) — **MAIN FOCUS**

For **each feature**, produce a **full, stepwise implementation blueprint**:

### 3.1 Client → Server Flow

* Exact request flow (HTTP/REST/gRPC/WebSocket).
* Step-by-step server handling (validation, auth, routing).
* Multi-device scenarios & offline handling.

### 3.2 Data Storage & Retrieval

* DB choice & justification (SQL vs NoSQL).
* Tables/collections with PKs, indexes, secondary indexes.
* Example read/write queries (SQL/CQL/pseudocode).
* Cache keys, TTLs, structure, eviction policy.
* Sharding/partitioning and cross-shard query strategy.

### 3.3 Server → Recipient Flow

* Push data to recipients (WebSocket, push notifications, polling).
* Multi-device, offline, group messaging.
* Optional features: presence, typing indicators, read receipts.

### 3.4 Messaging & Eventing

* `[MESSAGING_SYSTEM]` topics, partition keys, retention, schema.
* Event flow: API → DB → Messaging → Worker → Cache → Clients.
* Async vs sync handling, retries, DLQ, idempotency.

### 3.5 Edge Cases & Failure Handling

* Offline users, partial failures, cache misses, DB errors.
* Reconciliation, retries, deduplication strategies (Bloom filters, seen sets).

### 3.6 Pagination & Cursor Handling

* Cursor-building algorithm, opaque JSON/base64 example.
* Step-by-step read/merge logic from DB + cache.

### 3.7 Performance & Scalability Considerations

* Bottlenecks, autoscaling, multi-AZ/multi-region replication.
* Cache hit/miss ratios, messaging throughput, worker scaling.

### 3.8 Instrumentation & Monitoring

* Metrics: RED, cache hit ratio, messaging lag, latency per endpoint.
* Logging & tracing points in the data path.

### 3.9 Summary Table

\| Feature | Client→Server | Server→DB/Cache | Server→Recipient | Messaging Events | Notes / Multi-device / Offline Handling |

---

## 4. Additional Guidance for LLM Output

* **Prioritize depth over breadth:** every P0/P1 feature should have full blueprint.
* Integrate **DB, cache, messaging, HA, security, scalability considerations** inside the deep dive.
* Use **tables, ASCII diagrams, pseudocode, cache commands, DB queries, messaging payloads**.
* Show **multiple approaches when relevant** and recommend one with reasoning.
* Include **cursor examples** for paginated reads, **idempotency keys**, **deduplication strategies**.
* Optional sections like cost, back-of-envelope estimation, minor NFRs can be summarized briefly.
