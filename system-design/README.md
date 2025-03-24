# System Design

**Fundamentals of System Design**
    * Scalability (Vertical vs. Horizontal Scaling)
    * Availability (High Availability, Fault Tolerance, Disaster Recovery)
    * Performance (Latency vs. Throughput)
    * Consistency vs. Partition Tolerance vs. Availability (CAP Theorem)
    * Data Modeling & Normalization
    * ACID vs. BASE Properties
    * Caching Strategies (Write-through, Write-back, Write-around, LRU, LFU)
    * Load Balancing Techniques (Round Robin, Least Connections, IP Hashing, etc.)
    * Rate Limiting Techniques (Token Bucket, Leaky Bucket)

**Networking and Communication**
    * HTTP vs. HTTPS
    * WebSockets, SSE (Server-Sent Events)
    * RPC vs. REST vs. GraphQL vs. gRPC
    * API Gateway & Reverse Proxy (e.g., Nginx, Envoy)
    * CDN (Content Delivery Networks)
    * DNS and How It Works
    * TLS/SSL & Encryption Basics
    * Firewalls, VPNs, and Network Security Best Practices

**Database Design**
    * Relational (SQL) vs. NoSQL Databases
    * Sharding, Partitioning, and Replication
    * Indexes and Query Optimization
    * Eventual Consistency & Strong Consistency
    * Primary-Replica & Multi-Master Replication
    * Database Caching (Redis, Memcached)
    * Message Queues & Pub-Sub (Kafka, RabbitMQ, SQS)
    * Time-Series, Graph, and Search Databases (Elasticsearch, Neo4j, InfluxDB)

**Architecture Patterns**
    * Monolithic vs. Microservices Architecture
    * Serverless Architecture
    * Event-Driven Architecture
    * CQRS (Command Query Responsibility Segregation)
    * Saga Pattern for Distributed Transactions
    * Strangler Fig Pattern (For Migrating Monolith to Microservices)
    * Circuit Breaker Pattern (Resilience in Distributed Systems)
    * Bulkhead Pattern (Fault Isolation)
    * Sidecar Pattern (Service Mesh Concepts - Istio, Linkerd)
    * Backpressure Handling in APIs
  
**Scalability & Performance Optimization**
    * Database Read/Write Optimization (Indexes, Query Caching, etc.)
    * Horizontal Scaling using Load Balancers
    * API Rate Limiting & Throttling
    * Batch Processing vs. Streaming Processing
    * Data Compression & Optimization Techniques
    * Edge Computing & Latency Reduction
    * Distributed Logging & Monitoring (ELK Stack, Prometheus, Grafana)

**Distributed Systems**
    * Distributed Caching (Redis, CDN, Hazelcast)
    * Consensus Algorithms (Paxos, Raft, Zookeeper)
    * Leader Election in Distributed Systems
    * Vector Clocks & Conflict Resolution in Distributed Databases
    * Distributed Transactions (2PC, 3PC, Saga Pattern)
    * Eventual Consistency and Quorum Mechanism
    * DynamoDB Paper & Google Spanner Paper Concepts
    * Idempotency & Stateless Services

**Security & Compliance**
    * OAuth2, OpenID Connect, JWT Tokens
    * Role-Based Access Control (RBAC) vs. Attribute-Based Access Control (ABAC)
    * API Security (HMAC, JWT, API Keys)
    * Zero Trust Architecture
    * DDOS Protection Techniques
    * Secure Coding Best Practices (OWASP Top 10)
    * Data Encryption at Rest and In-Transit
    * Secure Authentication & Authorization Mechanisms
    * GDPR, HIPAA, and Other Compliance Requirements

**Cloud & DevOps**
    * Cloud Providers (AWS, GCP, Azure)
    * Containerization & Orchestration (Docker, Kubernetes)
    * Infrastructure as Code (Terraform, CloudFormation)
    * Service Mesh & API Gateway (Istio, Envoy, Kong)
    * CI/CD Pipelines (Jenkins, GitHub Actions, ArgoCD)
    * Chaos Engineering (Resilience Testing - Netflix Chaos Monkey)
    * Observability (Logging, Tracing, Metrics - OpenTelemetry, Prometheus, Grafana)
    * Feature Flags & Canary Deployments
    * Blue-Green & Rolling Deployments

**Frontend System Design Considerations**
    * Client-Side vs. Server-Side Rendering (CSR vs. SSR)
    * Micro Frontends
    * Progressive Web Apps (PWAs)
    * Performance Optimization (Lazy Loading, Code Splitting, Caching)
    * State Management Strategies (Redux, Context API, Recoil, MobX)
    * SEO & Accessibility in Web Applications
    * Frontend Security (CSP, CORS, XSS, CSRF Protection)

* https://www.geeksforgeeks.org/system-design-interview-bootcamp-guide/
* https://www.youtube.com/watch?v=F2FmTdLtb_4
* https://www.youtube.com/watch?v=m8Icp_Cid5o
* https://github.com/donnemartin/system-design-primer
* https://github.com/karanpratapsingh/system-design
* https://github.com/ashishps1/awesome-system-design-resources
