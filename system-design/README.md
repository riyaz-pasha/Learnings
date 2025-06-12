
# System Design

## Fundamentals of System Design
- **Scalability**
  - Vertical vs. Horizontal Scaling
- **Availability**
  - High Availability
  - Fault Tolerance
  - Disaster Recovery
- **Performance**
  - Latency vs. Throughput
- **CAP Theorem**
  - Consistency vs. Partition Tolerance vs. Availability
- **Data Modeling & Normalization**
- **ACID vs. BASE Properties**
- **Caching Strategies**
  - Write-through
  - Write-back
  - Write-around
  - LRU
  - LFU
- **Load Balancing Techniques**
  - Round Robin
  - Least Connections
  - IP Hashing
- **Rate Limiting Techniques**
  - Token Bucket
  - Leaky Bucket

## Networking and Communication
- **HTTP vs. HTTPS**
- **WebSockets, Server-Sent Events (SSE)**
- **RPC vs. REST vs. GraphQL vs. gRPC**
- **API Gateway & Reverse Proxy**
  - Nginx, Envoy
- **Content Delivery Networks (CDNs)**
- **DNS and How It Works**
- **TLS/SSL & Encryption Basics**
- **Firewalls, VPNs, and Network Security Best Practices**

## Database Design
- **Relational (SQL) vs. NoSQL Databases**
- **Sharding, Partitioning, and Replication**
- **Indexes and Query Optimization**
- **Consistency Models**
  - Eventual Consistency
  - Strong Consistency
- **Replication Strategies**
  - Primary-Replica
  - Multi-Master
- **Database Caching**
  - Redis, Memcached
- **Message Queues & Pub-Sub**
  - Kafka, RabbitMQ, SQS
- **Specialized Databases**
  - Time-Series: InfluxDB
  - Graph: Neo4j
  - Search: Elasticsearch

## Architecture Patterns
- **Monolithic vs. Microservices Architecture**
- **Serverless Architecture**
- **Event-Driven Architecture**
- **CQRS (Command Query Responsibility Segregation)**
- **Saga Pattern for Distributed Transactions**
- **Strangler Fig Pattern (Monolith to Microservices Migration)**
- **Circuit Breaker Pattern (Resilience)**
- **Bulkhead Pattern (Fault Isolation)**
- **Sidecar Pattern (Service Mesh - Istio, Linkerd)**
- **Backpressure Handling in APIs**

## Scalability & Performance Optimization
- **Database Optimization**
  - Read/Write Optimization (Indexes, Caching)
- **Horizontal Scaling using Load Balancers**
- **API Rate Limiting & Throttling**
- **Batch vs. Stream Processing**
- **Data Compression & Optimization**
- **Edge Computing & Latency Reduction**
- **Distributed Logging & Monitoring**
  - ELK Stack, Prometheus, Grafana

## Distributed Systems
- **Distributed Caching**
  - Redis, CDN, Hazelcast
- **Consensus Algorithms**
  - Paxos, Raft, Zookeeper
- **Leader Election**
- **Vector Clocks & Conflict Resolution**
- **Distributed Transactions**
  - 2PC, 3PC, Saga Pattern
- **Eventual Consistency and Quorum Mechanism**
- **Important Papers**
  - DynamoDB Paper
  - Google Spanner Paper
- **Idempotency & Stateless Services**

## Security & Compliance
- **OAuth2, OpenID Connect, JWT**
- **Access Control Models**
  - RBAC (Role-Based)
  - ABAC (Attribute-Based)
- **API Security**
  - HMAC, JWT, API Keys
- **Zero Trust Architecture**
- **DDoS Protection Techniques**
- **Secure Coding Best Practices**
  - OWASP Top 10
- **Data Encryption**
  - At Rest and In-Transit
- **Authentication & Authorization**
- **Compliance Standards**
  - GDPR, HIPAA, etc.

## Cloud & DevOps
- **Cloud Providers**
  - AWS, GCP, Azure
- **Containerization & Orchestration**
  - Docker, Kubernetes
- **Infrastructure as Code**
  - Terraform, CloudFormation
- **Service Mesh & API Gateway**
  - Istio, Envoy, Kong
- **CI/CD Pipelines**
  - Jenkins, GitHub Actions, ArgoCD
- **Chaos Engineering**
  - Netflix Chaos Monkey
- **Observability**
  - Logging, Tracing, Metrics
  - OpenTelemetry, Prometheus, Grafana
- **Feature Flags & Canary Deployments**
- **Deployment Strategies**
  - Blue-Green, Rolling

## Frontend System Design Considerations
- **Rendering Models**
  - Client-Side (CSR) vs. Server-Side (SSR)
- **Micro Frontends**
- **Progressive Web Apps (PWAs)**
- **Performance Optimization**
  - Lazy Loading, Code Splitting, Caching
- **State Management**
  - Redux, Context API, Recoil, MobX
- **SEO & Accessibility**
- **Frontend Security**
  - CSP, CORS, XSS, CSRF Protection

## References & Resources
- 📘 [GeeksforGeeks System Design Bootcamp](https://www.geeksforgeeks.org/system-design-interview-bootcamp-guide/)
- 🎥 [System Design YouTube Video #1](https://www.youtube.com/watch?v=F2FmTdLtb_4)
- 🎥 [System Design YouTube Video #2](https://www.youtube.com/watch?v=m8Icp_Cid5o)
- 📚 [donnemartin/system-design-primer (GitHub)](https://github.com/donnemartin/system-design-primer)
- 📚 [karanpratapsingh/system-design (GitHub)](https://github.com/karanpratapsingh/system-design)
- 📚 [ashishps1/awesome-system-design-resources (GitHub)](https://github.com/ashishps1/awesome-system-design-resources)
