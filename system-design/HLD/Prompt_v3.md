# High-Level System Design (HLSD)

**Task:** Design a scalable system for `[Design a scalable chat system like WhatsApp. Include message delivery, real-time sync, storage, groups, and end-to-end encryption]`.

**Output format required:** Plain text / Markdown (easy to copy-paste). No images. Provide compact ASCII diagrams where helpful. Provide `[LANGUAGE]` pseudocode for critical flows. Show example DB queries and `[MESSAGING_SYSTEM]`/`[CACHE_SYSTEM]` commands. Use tables for schema definitions.

---

## Required deliverables (produce all sections below)

### 0. Executive summary (1–2 paragraphs)

* Short problem statement, recommended architecture (one-line), and why it fits the requirements.

---

## 1. Functional & Non-Functional Requirements

* List all core features as **actionable tasks**. Mark priority P0/P1/P2.
* Define user personas and their primary needs.
* For each feature, list **technical & business complexities** that must be considered.

Include:

* SLOs and SLAs (latency targets, availability percentages).
* Tunable parameters (e.g., `[PARAMETER_NAME] = [VALUE]`, `[PARAMETER_NAME] = [VALUE]`).

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
* Short-term MVP vs long-term evolution (`[FUTURE_FEATURE_1]`, `[FUTURE_FEATURE_2]`, `[INTEGRATION_OPPORTUNITIES]`).

---

## 12. Technology Recommendations

* Suggested languages, frameworks (mention `[PREFERRED_LANGUAGE]` option for core services and show one `[PREFERRED_LANGUAGE]` snippet in deep dives), databases, cache, messaging, cloud infra.
* DevOps: CI/CD, infra-as-code, monitoring stack.
* Third-party integrations (`[EXTERNAL_SERVICE_1]`, `[EXTERNAL_SERVICE_2]`, `[EXTERNAL_SERVICE_3]`).

---

## 13. Deliverables checklist (final section)

* Provide a compact checklist of the produced artifacts (schemas, queries, pseudocode, diagrams, cost estimates, monitoring plan).

---

## Additional instructions for the assistant

* **Be explicit**: when giving numbers or thresholds, state assumptions.
* **Prefer examples**: show concrete `[CACHE_SYSTEM]`/`[MESSAGING_SYSTEM]` commands, CQL/SQL statements, and one or two `[PREFERRED_LANGUAGE]`-style pseudocode functions for critical parts.
* **Make pagination/cursor examples concrete** (show a sample cursor payload and how the next page is computed).
* **Explain idempotency & deduplication** strategies in detail (keys, bloom filters, seen-sets).
* **If multiple valid approaches exist**, compare them (pros/cons) and pick a recommended approach with reasoning.
* **Keep the answer structured** so an engineer can copy-paste schema snippets and `[CACHE_SYSTEM]`/`[MESSAGING_SYSTEM]` commands into a scratch implementation.
* **Limit verbosity**: keep each section focused and use bullet lists/tables for clarity. But be thorough — produce depth where it matters (DB schemas, queries, sharding, flows).

---

## Template Variables to Replace Before Using:

### System-Specific Variables:
- `[INSERT YOUR SYSTEM REQUIREMENTS HERE]` - The specific system you want to design
- `[DOMAIN_SPECIFIC_METRICS]` - Metrics relevant to your domain (posts/day, orders/hour, etc.)
- `[PRIMARY_WRITE_OPERATION]` - Main write operation (Post creation, Order placement, etc.)
- `[PRIMARY_READ_OPERATION]` - Main read operation (Feed generation, Search, etc.)
- `[CRITICAL_WRITE_OPERATION]` - Most critical write flow to detail
- `[CRITICAL_READ_OPERATION]` - Most critical read flow to detail
- `[CRITICAL_DATA_STRUCTURES]` - Key data relationships to detail
- `[RELATIONSHIP_TYPE]` - Type of relationships in your system
- `[HOTSPOT_SCENARIO]` - Common hotspot scenario for your domain

### Technology Variables:
- `[LANGUAGE]` - Preferred programming language (Java, Python, Go, etc.)
- `[PREFERRED_LANGUAGE]` - Same as above for consistency
- `[MESSAGING_SYSTEM]` - Kafka, RabbitMQ, SQS, etc.
- `[CACHE_SYSTEM]` - Redis, Memcached, etc.
- `[EVENT_NAME]` - Specific event names for your domain

### Service Variables:
- `[SERVICE_1]`, `[SERVICE_2]`, `[SERVICE_3]` - Your specific services
- `[SERVICE_TYPE]` - Type of services in your architecture
- `[FEATURE_1]`, `[FEATURE_2]`, `[FEATURE_3]` - Your core features

### Parameter Variables:
- `[PARAMETER_NAME]` - Tunable parameters specific to your system
- `[VALUE]` - Corresponding values
- `[CACHE_SIZE_1]`, `[CACHE_SIZE_2]` - Cache size parameters
- `[TIME_VALUE]` - TTL or time-based values

### Compliance & Integration Variables:
- `[COMPLIANCE_REQUIREMENTS]` - GDPR, HIPAA, PCI-DSS, etc.
- `[SECURITY_SCANNING_NEEDS]` - AV scanning, content moderation, etc.
- `[EXTERNAL_SERVICE_1]`, `[EXTERNAL_SERVICE_2]`, `[EXTERNAL_SERVICE_3]` - Third-party services
- `[FUTURE_FEATURE_1]`, `[FUTURE_FEATURE_2]` - Planned future features
- `[INTEGRATION_OPPORTUNITIES]` - Future integration possibilities

### Usage Instructions:

1. Copy this template
2. Replace all `[BRACKETED_VARIABLES]` with your specific requirements
3. Remove this variables section before using
4. Provide the customized prompt to get a comprehensive system design

### Example Customization:
For a social media feed system, you might replace:
- `[INSERT YOUR SYSTEM REQUIREMENTS HERE]` → "a social media news feed system"
- `[LANGUAGE]` → "Java"
- `[MESSAGING_SYSTEM]` → "Kafka"
- `[CACHE_SYSTEM]` → "Redis"
- `[PRIMARY_WRITE_OPERATION]` → "Post creation"
- `[PRIMARY_READ_OPERATION]` → "Feed generation"
