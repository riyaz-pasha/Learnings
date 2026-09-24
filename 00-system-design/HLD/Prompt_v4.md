# Interview-Style HLSD Prompt

**Task:** Explain the design of `[SYSTEM_NAME / FEATURE]` as if I’m answering an interviewer’s question.

**Instructions for LLM:**
Respond in a way that feels like **I’m walking the interviewer through the flow step-by-step**.
For each functional requirement, explain it in the following style:

---

### 1. Request Initiation

* Who triggers the request (user, service, system event).
* Example endpoint (`POST /api/...`) with a simple request body.

### 2. Entry Point / First Component

* Which component (API Gateway, Load Balancer, Auth Service).
* Role of this component for this requirement.

### 3. Server-Side Processing

* Which service handles the request.
* Any **algorithm or logic** applied (e.g., hashing, ranking, deduplication) and **why it’s needed**.
* Security checks, validations, transformations.

### 4. Database Interaction

* Which DB it talks to (SQL/NoSQL choice + why).
* Minimal schema of relevant tables (table name, columns, PK/indexes).
* Example queries (read/write).
* Performance optimizations (indexing, sharding, partitioning, denormalization).

### 5. Caching Layer

* Whether cache is used here, and if so:

  * Cache key format
  * TTL / eviction policy
  * Cache-aside / write-through / write-behind reasoning

### 6. Messaging / Async Processing (if needed)

* Does the request produce an event?
* Which `[MESSAGING_SYSTEM]` topic?
* Example payload.
* Consumer responsibilities (update DB, send notifications, update cache).

### 7. Response to Client

* How the response is constructed and sent back.
* Latency considerations.

### 8. Edge Cases & Failures

* What if DB write fails?
* What if cache is stale?
* What if recipient is offline?

### 9. Scaling & Improvements

* How this flow scales with 10x traffic.
* Where bottlenecks might appear.
* Specific optimizations: DB indexing, cache layers, horizontal scaling, partitioning.

---
