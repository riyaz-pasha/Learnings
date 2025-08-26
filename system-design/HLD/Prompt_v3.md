# High-Level System Design (HLSD)

**Task:** Design a scalable system for `[Design a video streaming service like YouTube. Include upload, transcoding, CDN usage, metadata indexing, and playback.]`.

---

## Required deliverables (produce all sections below)


## 1. Functional & Non-Functional Requirements

### Functional Requirements:

* List all core features of the system.
* Define each feature as a specific, actionable task.
* Prioritize requirements (P0/P1/P2) if applicable.
* Define user personas and their needs.
* For each feature, highlight potential **technical or business complexities**.

### Non-Functional Requirements:

* **Performance:** Latency targets, throughput, peak vs. average load.
* **Scalability:** Expected growth patterns, max concurrent users, QPS.
* **Reliability & Availability:** Uptime targets (e.g., 99.9% or 99.99%), fault tolerance.
* **Consistency:** Strong vs. eventual consistency requirements.
* **Security:** Authentication, authorization, encryption, data protection.
* **Compliance:** Regulatory or industry standards.
* **Maintainability:** Ease of updates, monitoring, and debugging.

### Assumptions & Scope:

* Clarify assumptions (scope, user base, data model).
* Define what is explicitly **out of scope**.
* Mention constraints (budget, timeline, existing infrastructure).

---

## 2. Back-of-the-Envelope Estimation

* Provide a concrete example scenario (set explicit numbers: MAU, DAU, CCU, `[DOMAIN_SPECIFIC_METRICS]`).
* Compute read/write QPS (avg & peak), storage needs, `[CACHE_SYSTEM]` memory estimate, `[MESSAGING_SYSTEM]` throughput.
* Show formulas and assumptions used.
* Give a rough monthly cloud cost ballpark (major items only).

---

## 3. API Design

* List REST/gRPC/GraphQL endpoints with request/response examples, auth, rate-limiting scheme, status codes.
* Map each endpoint to the functional requirement it implements.
* Show pagination parameters and the exact **opaque cursor format** (example JSON/base64).

---

## 4. High-Level Architecture

* ASCII block diagram of components (edge, API GW, services, DBs, caches, queues, storage).
* Component roles and communication protocols (HTTP/gRPC/`[MESSAGING_SYSTEM]`/WS).
* Data flows for:
  * `[PRIMARY_WRITE_OPERATION]` (write path)
  * `[PRIMARY_READ_OPERATION]` (read path)
  * Real-time notifications/updates
* For each flow, highlight potential bottlenecks and where to place resiliency (DLQ, retries, circuit breakers).

---

## 5. Functional Requirement Deep Dives (for every P0 and important P1)

For each requirement (e.g., `[FEATURE_1]`, `[FEATURE_2]`, `[FEATURE_3]`, etc.):

**Instruction:**

> I want a detailed implementation of all functional requirements. For each functional requirement, explain:
>
> 1. **Request flow from client to server**: how the request is initiated, what the server does step by step. Include WebSocket/HTTP/REST/GRPC details where relevant.
> 2. **Data storage and retrieval**: how data is persisted in DB, what tables/collections/primary keys/indexes are used, cache keys, TTLs, and structure.
> 3. **Delivery to other clients / devices**: how the system pushes data to recipients, handles multi-device scenarios, offline users, and group scenarios.
> 4. **Edge cases and failure handling**: what happens if a recipient is offline, connection is lost, or data fails to persist.
> 5. **Optional features**: presence, typing indicators, push notifications, read receipts.
>
> Present the results in **plain English words first** describing the flows, and optionally include **example Redis / Kafka commands, DB queries, or pseudo code**.
>
> Finally, provide a **summary table** listing:
>
> * Functional requirement
> * Client → Server flow
> * Server → DB/Cache flow
> * Server → Recipient flow
> * Notes / multi-device / offline handling

* **Components involved**
* **Step-by-step data flow**
* **Implementation approaches** (Approach 1, Approach 2, recommended)
* **Complexities and trade-offs**
* **Edge cases and error handling**
* **Performance & scalability considerations**
* **Instrumentation points** (what to monitor)

Specifically for **`[CRITICAL_WRITE_OPERATION]`** and **`[CRITICAL_READ_OPERATION]`**, include:

* Exact sequence of events (API → DB write → `[MESSAGING_SYSTEM]` `[EVENT_NAME]` → Worker → `[CACHE_SYSTEM]` updates).
* `[MESSAGING_SYSTEM]` topic names and sample event payloads.
* `[CACHE_SYSTEM]` commands for data writes and pagination.
* `[LANGUAGE]` pseudocode for the worker consumer and service read-and-merge logic (concise, readable).
* Cursor-building algorithm and sample opaque cursor (JSON example and base64-encoding).

---

## 6. Component — Database & Storage Deep Dive (this is mandatory)

For **every service** that persists data (`[SERVICE_1]`, `[SERVICE_2]`, `[SERVICE_3]`, etc.), provide:

1. **DB choice** and justification (SQL vs NoSQL; named product recommendation).
2. **Tables / collections / indexes** — for each table provide:
   * Table name
   * Column list with types
   * Primary key (PK) and sort key (if any)
   * Secondary indexes (what queries they support)
   * TTLs / retention policy
   * Example read and write queries (SQL / CQL / pseudo)
   * Example `[CACHE_SYSTEM]` keys and data shape (for caches)
3. **Sharding / partitioning strategy**
   * Partition key(s), rationale, hotspot mitigation strategies
   * How cross-shard queries are handled (scatter-gather, fan-in limits)
4. **Data model trade-offs**
   * Strong consistency vs eventual trade-offs
   * Denormalization vs normalization reasons
5. **Sample schema table** (markdown table) for each DB

**Important:** For `[CRITICAL_DATA_STRUCTURES]`:

* Show the exact schema and explain how `[RELATIONSHIP_TYPE]` is stored and indexed.
* Provide the cache key pattern and explain how pagination is implemented with data structure scores and cursor semantics.
* Provide cache eviction policies.

---

## 7. Caching Strategy

* Layers: browser, CDN, edge, app-level (`[CACHE_SYSTEM]`), DB-level.
* Caching patterns (cache-aside, write-through, write-behind) — which to use where and why.
* Eviction policy, item sizes, TTLs, stampede protection (mutex/coalesce).
* Concrete values to start with (e.g., `[CACHE_SIZE_1]` = `[VALUE]`, `[CACHE_SIZE_2]` = `[VALUE]`, TTL = `[TIME_VALUE]`).

---

## 8. Messaging & Eventing

* `[MESSAGING_SYSTEM]` topics (name, partition key, retention), message schemas.
* Which operations are async vs sync.
* Consumer group patterns for fan-out workers (sharding strategy).
* Backpressure, retry policy, DLQ handling, idempotency keys.

---

## 9. Scalability, HA & Operations

* Autoscaling strategy for API / worker / `[SERVICE_TYPE]` services.
* Multi-AZ / multi-region deployment & replication strategy.
* Data backup / restore and DR plan.
* Monitoring & tracing: metrics to track (RED, `[MESSAGING_SYSTEM]` lag, `[CACHE_SYSTEM]` hit ratio, SLOs), example alert thresholds.
* Runbooks for common failures (e.g., `[CACHE_SYSTEM]` partition fail, `[MESSAGING_SYSTEM]` lag spike, `[HOTSPOT_SCENARIO]`).

---

## 10. Security, Compliance & Privacy

* Auth (OAuth2/OIDC), service-to-service auth (mTLS), secrets management.
* Row/field-level ACLs for data visibility.
* `[COMPLIANCE_REQUIREMENTS]` considerations: deletion, export, consent.
* Input sanitization, `[SECURITY_SCANNING_NEEDS]`, rate-limit anti-abuse.

---

## 11. Cost, Trade-offs & Future Roadmap

* Identify main cost drivers and suggest cost-optimization levers.
* Trade-offs (consistency vs freshness, memory vs latency, fan-out vs pull).

---

## Additional instructions for the assistant

* **Be explicit**: when giving numbers or thresholds, state assumptions.
* **Prefer examples**: show concrete `[CACHE_SYSTEM]`/`[MESSAGING_SYSTEM]` commands, CQL/SQL statements, and one or two `[PREFERRED_LANGUAGE]`-style pseudocode functions for critical parts.
* **Make pagination/cursor examples concrete** (show a sample cursor payload and how the next page is computed).
* **Explain idempotency & deduplication** strategies in detail (keys, bloom filters, seen-sets).
* **If multiple valid approaches exist**, compare them (pros/cons) and pick a recommended approach with reasoning.
* **Keep the answer structured** so an engineer can copy-paste schema snippets and `[CACHE_SYSTEM]`/`[MESSAGING_SYSTEM]` commands into a scratch implementation.
* **Limit verbosity**: keep each section focused and use bullet lists/tables for clarity. But be thorough — produce depth where it matters (DB schemas, queries, sharding, flows).
