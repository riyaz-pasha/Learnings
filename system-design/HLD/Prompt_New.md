**Task:** Design a scalable system for `[Your HLSD Question Here]`.

Provide a detailed **High-Level System Design (HLSD)** plan, following the structure below:

---

## 1. Functional and Non-Functional Requirements

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

* **User Metrics:** Total users, daily active users, peak concurrent users.
* **Request Metrics:** QPS for reads/writes, seasonal variations, read/write ratios.
* **Data Metrics:** Storage per user, retention policies, expected growth.
* **Network Metrics:** Bandwidth requirements, upload/download patterns.
* **Resource Estimates:** Memory (for caching), CPU, storage, network utilization.
* **Justification:** Explain assumptions behind all estimates.
* **Cost Considerations:** Approximate infrastructure and operational costs.

---

## 3. API Design

* Define RESTful endpoints, GraphQL schema, or gRPC services.
* Specify request/response formats, including data types and error codes.
* Map each API to the **functional requirement** it satisfies.
* Include authentication, rate-limiting, versioning, and error handling strategies.

---

## 4. High-Level Architecture

* **Block Diagram:** Show main components (Load Balancer, API Gateway, Web/Service Servers, Databases, Caches, Queues, Storage).
* **Component Roles:** Explain responsibilities of each component.
* **Communication Protocols:** HTTP/HTTPS, gRPC, WebSockets, message queues.
* **Data Flow:** Trace how requests flow through the system.
* **Functional Requirement Flow:** For each major requirement:

  * Which components are involved
  * How they interact
  * Challenges and design choices
  * Alternative approaches and trade-offs

---

## 5. Functional Requirements Deep Dive ⭐

For **each functional requirement**:

### Requirement: \[Name]

* **Components Involved:** All system components participating.
* **Data Flow:** Step-by-step request-to-response process.
* **Implementation Approaches:**

  * **Approach 1:** Description, pros/cons.
  * **Approach 2:** Alternative method, trade-offs.
  * **Recommended Approach:** Justification.
* **Complexities & Challenges:** Technical, business, integration issues.
* **Edge Cases & Error Handling:** Failure scenarios and mitigation.
* **Performance Considerations:** Latency, throughput, resource usage.
* **Scalability Challenges:** Hotspots, bottlenecks.
* **Variations & Future Extensions:** Alternative designs, future scaling.

---

## 6. Component Deep Dive

### Data Storage:

* SQL vs. NoSQL justification.
* Schema/data model, indexing, relationships.
* Partitioning/sharding strategy and cross-shard query handling.
* Multi-database or polyglot persistence if applicable.

### Caching Strategy:

* Cache layers: Browser, CDN, application, database.
* Cache patterns: Cache-aside, write-through, write-behind.
* Eviction policies, TTL, cache consistency, stampede handling.

### Load Balancing:

* Placement (L4/L7, geographic distribution).
* Algorithms: Round-robin, least connections, consistent hashing.
* Health checks, session affinity.

### Messaging / Eventing:

* When to use queues (Kafka, RabbitMQ).
* Asynchronous vs. synchronous tasks.
* Event-driven patterns for decoupling services.

---

## 7. Scalability & Availability

* **Horizontal Scaling:** Stateless services, auto-scaling, microservices boundaries.
* **High Availability:** Redundancy (active-active/passive), failover, disaster recovery.
* **Monitoring & Observability:** Metrics, logging, distributed tracing, alerts.
* **Circuit Breakers & Rate Limiting:** Prevent cascading failures.

---

## 8. Advanced Considerations

* **Security:** Auth, encryption, network security, input validation.
* **Performance Optimization:** DB queries, network, CPU/memory, caching.
* **Operational Excellence:** Deployment strategies, configuration management, capacity planning.

---

## 9. Bottlenecks, Trade-offs, and Future Considerations

* Identify performance, scalability, and operational bottlenecks.
* Discuss trade-offs: consistency vs. availability, performance vs. cost, complexity vs. maintainability.
* Suggest future improvements, technology evolution, and scaling strategies.

---

## 10. Technology Stack Recommendations

* Programming languages and frameworks.
* Infrastructure: Cloud provider, managed services.
* DevOps tools: CI/CD, monitoring, deployment tools.
* Third-party integrations (payment, notifications, etc.).
