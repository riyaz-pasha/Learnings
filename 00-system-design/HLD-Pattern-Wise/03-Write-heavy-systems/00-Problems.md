# HLD System Design: Write-Heavy Systems and High-Volume Ingestion

This is an important system design pattern for interviews at companies like Google, Meta, Amazon, and other large-scale technology companies.

The central challenge is designing systems that **accept enormous volumes of incoming data, ingest it reliably, process it efficiently, and make it available for querying without overwhelming the storage layer**.

Examples include metrics platforms, log aggregation systems, clickstream analytics, IoT telemetry, GPS tracking, financial market feeds, and social media counters.

For HLD interview preparation, I recommend studying these problems as a family rather than treating every application as an independent design. Many share the same ingestion architecture but differ in delivery guarantees, ordering, aggregation, retention, querying, and latency requirements.

---

## 1. What Defines This Pattern?

A system belongs to this pattern when its primary engineering challenge is handling a large volume of incoming writes.

Typical characteristics include:

- **High write throughput:** Thousands to millions of events per second.
- **Continuous ingestion:** Data arrives around the clock, often from millions of clients or devices.
- **Bursty traffic:** A major event, product launch, or outage can cause sudden spikes.
- **Large data volume:** Events accumulate rapidly, potentially reaching terabytes or petabytes.
- **Low-latency ingestion:** Producers need acknowledgements quickly without waiting for expensive downstream processing.
- **Asynchronous processing:** Data may need validation, enrichment, aggregation, filtering, or transformation.
- **High availability:** Temporary failures should not result in lost events.
- **Efficient storage:** Data must be partitioned, compressed, indexed, aggregated, or expired appropriately.
- **Read/write asymmetry:** Some systems have overwhelmingly more writes than reads, while others must support both high-volume ingestion and fast analytical queries.
- **Replay and recovery:** The ability to reprocess historical events after a consumer failure or processing bug may be essential.

### The common architecture

A typical architecture looks like this:

```text
 Producers / Devices / Applications
                 |
                 v
        Load Balancer / API
                 |
                 v
        Ingestion Service
                 |
                 v
       Durable Message Broker
       (Kafka / Pulsar / Kinesis)
                 |
                 v
        Stream Processing
        (Flink / Kafka Streams)
                 |
         +-------+--------+
         |                |
         v                v
   Real-Time Path    Batch Path
         |                |
         v                v
  Fast Query Store   Data Lake / Warehouse
         |
         v
     Dashboards / APIs / Alerts
```

The precise components depend on the requirements. For example, a metrics platform may use a time-series database, a log platform may use an inverted index, and a clickstream platform may use a warehouse or columnar analytical store.

The critical principle is that **the ingestion path should not depend synchronously on every downstream operation completing successfully**.

A durable buffer absorbs bursts, decouples producers from consumers, and enables controlled processing. However, it does not eliminate capacity constraints: sustained ingestion must remain within the system's processing and storage capacity, or the backlog will grow.

---

# 2. Complete Problem Catalog

I would divide this pattern into **12 categories covering 40 system design problems**.

The catalog includes canonical interview problems, closely related variants, and advanced problems that build on the same ingestion fundamentals.

The classifications are:

- **Core:** A high-value, standalone system design problem for this pattern.
- **Variant:** Shares most of its architecture with another problem but introduces a different requirement.
- **Advanced:** Useful for senior-level interviews or when the interviewer explores scalability, correctness, and distributed systems trade-offs.

You do not need to design all 40 from scratch. The goal is to master the shared foundations and understand what changes between problem variants.

## Category A: Metrics and Time-Series Systems

These are among the most important problems in the write-heavy category.

The central challenge is ingesting millions of timestamped measurements, storing them efficiently, and supporting queries over time ranges.

### 1. Metrics Monitoring and Time-Series Database

**Priority:** Core  
**Difficulty:** Hard  
**Interview relevance:** Very high

**Problem statement**

Design a metrics platform that collects infrastructure and application metrics from thousands of servers and services. Users should be able to query historical metrics, visualize trends, and monitor system health.

**Example events**

```json
{
  "metric": "cpu_usage",
  "value": 76.4,
  "timestamp": 1793095200,
  "host": "server-101",
  "region": "ap-southeast-2"
}
```

**Functional requirements**
- Ingest metrics continuously from applications and infrastructure.
- Query metrics over configurable time ranges.
- Aggregate measurements by minute, hour, or day.
- Filter and group metrics by labels such as host, region, and service.
- Support dashboards and alerting.

**Key HLD challenges**
- How do you handle millions of metric samples per second?
- How do you partition time-series data?
- How do you efficiently query the last 15 minutes or 30 days?
- How do you avoid creating too many distinct time series?
- How do you implement retention and downsampling?

**Important design decisions**
- Time-series database versus general-purpose database.
- Append-oriented ingestion versus in-place updates.
- Time-based partitioning and label-based indexing.
- Raw samples versus precomputed aggregates.
- Compression and retention policies.

**What to master:** Time-series storage, high-cardinality labels, time-based partitioning, downsampling, and efficient range queries.

**Related real-world systems:** Prometheus, VictoriaMetrics, and the metrics components of large observability platforms.

---

### 2. Distributed Metrics Collection Platform

**Priority:** Core  
**Difficulty:** Hard

**Problem statement**

Design a distributed metrics collection system that gathers measurements from applications, containers, virtual machines, and physical servers across multiple regions.

Unlike problem 1, the focus is on the **collection and ingestion infrastructure**, not primarily the database or query engine.

**Requirements**
- Collect metrics from a large number of agents.
- Support periodic collection and push-based reporting.
- Buffer measurements during temporary network failures.
- Handle duplicate reports and late-arriving samples.
- Support regional collection and centralized aggregation.

**Key challenges**
- How frequently should agents report?
- How do you avoid a synchronized reporting storm?
- How do you handle agents that reconnect after an outage?
- How do you prevent one malfunctioning agent from overwhelming ingestion?
- How do you balance local buffering against data-loss risk?

**Important concepts**
- Agent-side batching.
- Jittered reporting intervals.
- Regional ingestion gateways.
- Backpressure and rate limiting.
- Local disk buffers.
- Per-tenant quotas.

**Difference from problem 1:** Problem 1 focuses on storing and querying time-series data. This problem focuses on collecting that data reliably and at scale.

---

### 3. Real-Time Monitoring and Alerting System

**Priority:** Core  
**Difficulty:** Hard

**Problem statement**

Design a system that ingests infrastructure or application metrics and detects conditions such as high CPU usage, elevated error rates, or increased latency. It should trigger alerts within seconds.

**Example**

```text
Metric: API error rate
Window: 5 minutes
Condition: Error rate > 5%
Action: Send an alert
```

**Requirements**
- Ingest metrics continuously.
- Evaluate threshold-based and rolling-window rules.
- Support alerts across multiple services and regions.
- Deduplicate repeated alerts.
- Support recovery notifications and alert suppression.

**Key challenges**
- Real-time windowed aggregation.
- Late and out-of-order events.
- Avoiding alert storms.
- Evaluating many alert rules efficiently.
- Handling missing metrics and monitoring-system failures.
- Preventing duplicate notifications during retries.

**Important design decisions**
- Stream processing versus periodic database queries.
- Event time versus processing time.
- Fixed windows versus sliding windows.
- Stateful aggregation versus stateless evaluation.
- At-least-once processing versus deduplicated side effects.

**Difference from problem 1:** This system must turn incoming measurements into timely decisions, not merely retain and visualize them.

---

### 4. Application Performance Monitoring (APM)

**Priority:** Core  
**Difficulty:** Hard

**Problem statement**

Design an APM platform that collects application performance metrics, request traces, errors, and service-level telemetry from distributed applications.

**Requirements**
- Collect request latency, error rates, throughput, and resource utilization.
- Ingest distributed traces and span data.
- Correlate traces with logs and metrics.
- Support service dependency maps.
- Allow users to investigate slow requests and failures.

**Key challenges**
- Handling high-volume span ingestion.
- Managing trace sampling.
- Preserving trace context across services.
- Correlating data without expensive joins.
- Controlling storage costs for high-cardinality dimensions.
- Supporting fast searches over recent telemetry.

**Important concepts**
- Trace and span identifiers.
- Head-based and tail-based sampling.
- Trace ingestion pipelines.
- Columnar and analytical storage.
- Service-level objectives.
- Correlation between metrics, logs, and traces.

**Interview distinction:** APM combines several data types. It is a useful bridge between metrics, log aggregation, and distributed tracing designs.

---

### 5. Custom Business Metrics Platform

**Priority:** Variant  
**Difficulty:** Medium–Hard

**Problem statement**

Design a platform that allows product teams to define, publish, aggregate, and query business metrics such as orders per minute, checkout conversion, active users, or subscription cancellations.

**Requirements**
- Accept custom events from multiple applications.
- Convert events into business metrics.
- Support grouping by product, geography, customer segment, and time.
- Expose dashboards and APIs.
- Allow teams to define custom aggregation rules.

**Key challenges**
- Schema evolution.
- Consistent metric definitions across teams.
- Duplicate business events.
- Aggregation across different dimensions.
- Correct handling of delayed updates and cancellations.

**Important distinction:** Infrastructure metrics generally measure system health; business metrics measure product or business outcomes.

---

## Category B: Logs, Observability, and Distributed Tracing

These problems focus on large volumes of semi-structured or structured events, often with unpredictable fields and complex search requirements.

### 6. Centralized Log Aggregation System

**Priority:** Core  
**Difficulty:** Hard  
**Interview relevance:** Very high

**Problem statement**

Design a centralized logging platform that collects logs from thousands of services and machines, stores them, and allows engineers to search and investigate failures.

**Example log event**

```json
{
  "timestamp": "2026-10-27T04:30:00Z",
  "service": "payment-service",
  "level": "ERROR",
  "trace_id": "abc123",
  "message": "Payment authorization failed",
  "region": "ap-southeast-2"
}
```

**Requirements**
- Collect logs from applications and infrastructure.
- Support high-throughput ingestion.
- Search logs by time, service, severity, and text.
- Handle large log bursts during outages.
- Apply retention and access-control policies.
- Support log export and downstream processing.

**Key challenges**
- Indexing versus indexing-free storage.
- High write throughput and indexing overhead.
- Full-text search at scale.
- Partitioning by time and service.
- Compression and storage costs.
- Multitenancy and noisy-neighbor protection.

**Important design decisions**
- Search-oriented storage versus object storage.
- Synchronous versus asynchronous indexing.
- Hot, warm, and cold storage tiers.
- Index lifecycle management.
- Durable buffering and replay.

**What to master:** The difference between the ingestion path, indexing path, and query path.

---

### 7. Real-Time Log Analytics and Anomaly Detection

**Priority:** Core  
**Difficulty:** Hard

**Problem statement**

Design a system that continuously analyzes incoming logs to detect unusual error patterns, suspicious activity, or unexpected changes in application behavior.

**Example**

```text
Normal:
Payment failures = 20 per minute

Current:
Payment failures = 2,000 per minute

Expected behavior:
Detect anomaly -> Notify on-call engineer
```

**Requirements**
- Ingest and parse logs continuously.
- Aggregate events by service, error code, and time window.
- Detect threshold violations and unusual patterns.
- Trigger alerts and provide supporting log samples.

**Key challenges**
- Processing logs without indexing every field.
- Maintaining rolling counts.
- Distinguishing legitimate traffic spikes from anomalies.
- Avoiding duplicate alerts.
- Handling bursts without dropping critical events.

**Difference from problem 6:** Centralized logging emphasizes collection, storage, and search. This problem emphasizes continuous analysis and detection.

---

### 8. Distributed Tracing Backend

**Priority:** Core  
**Difficulty:** Hard

**Problem statement**

Design a backend that ingests distributed traces from microservices and allows developers to reconstruct a request's journey across multiple services.

**Example**

```text
API Gateway:       20 ms
    |
    v
Order Service:     80 ms
    |
    +----> Inventory Service: 30 ms
    |
    +----> Payment Service:   500 ms
```

The developer should be able to identify the payment service as the main contributor to latency.

**Requirements**
- Ingest spans from distributed services.
- Associate spans with traces.
- Query traces by trace ID, service, duration, and time.
- Support service dependency graphs.
- Retain recent traces and selectively retain older ones.

**Key challenges**
- High-volume span ingestion.
- Trace sampling and retention.
- Out-of-order span arrival.
- Partial traces caused by missing spans.
- Efficient trace-ID lookups.
- Balancing observability coverage against storage costs.

**Important distinction:** A distributed tracing backend is related to APM but can be designed as an independent system focused on span ingestion and trace retrieval.

---

### 9. Audit Logging Platform

**Priority:** Core  
**Difficulty:** Hard

**Problem statement**

Design a tamper-resistant audit logging system that records security-sensitive and business-critical actions.

**Example events**
- An administrator changes a user's role.
- A customer downloads confidential data.
- A service modifies a payment configuration.
- An employee accesses a restricted resource.

**Requirements**
- Record who performed an action, what changed, when it happened, and the relevant context.
- Support queries by actor, resource, action, and time.
- Prevent unauthorized alteration or deletion.
- Support retention and compliance requirements.
- Provide evidence for investigations.

**Key challenges**
- Reliable event capture.
- Tamper evidence and access controls.
- Immutable or write-once storage.
- Ordering and completeness of audit events.
- Data residency and retention policies.
- Handling sensitive information without logging secrets.

**Important design decisions**
- Whether audit events must be committed before a business action succeeds.
- Transactional outbox versus direct asynchronous publishing.
- Append-only storage and cryptographic integrity verification.
- Separation between operational logs and security audit records.

**Critical interview insight:** Audit logging often has stricter correctness requirements than ordinary application logging. An asynchronous pipeline that can silently lose events may be unacceptable.

---

### 10. Security Event Ingestion and SIEM

**Priority:** Advanced  
**Difficulty:** Hard–Very Hard

**Problem statement**

Design a Security Information and Event Management (SIEM) platform that collects security events from endpoints, identity systems, cloud infrastructure, network devices, and applications.

**Requirements**
- Ingest security events from many sources.
- Normalize different event formats.
- Correlate related events.
- Detect suspicious behavior.
- Support incident investigation and alerting.
- Retain historical evidence.

**Example detection**

```text
1. User logs in from an unusual country.
2. The same account accesses sensitive resources.
3. Privileged operations occur shortly afterward.

Expected:
Correlate events and raise a risk alert.
```

**Key challenges**
- Schema normalization.
- High-cardinality filtering.
- Cross-source correlation.
- Late and out-of-order events.
- Detection-rule execution.
- Reliable ingestion of security-critical events.
- Long-term retention and efficient forensic queries.

**Difference from ordinary log analytics:** SIEM focuses on security context, event correlation, threat detection, and investigation.

---

## Category C: Clickstream, Product Analytics, and Web Events

These are particularly useful for understanding high-volume ingestion from web and mobile applications.

### 11. Clickstream Event Collection Platform

**Priority:** Core  
**Difficulty:** Hard  
**Interview relevance:** Very high

**Problem statement**

Design a platform that collects user interaction events from websites and mobile applications, such as page views, clicks, searches, product views, and navigation events.

**Example**

```json
{
  "event_id": "evt-123",
  "user_id": "user-456",
  "event_type": "product_view",
  "product_id": "product-789",
  "timestamp": "2026-10-27T04:30:00Z",
  "session_id": "session-123"
}
```

**Requirements**
- Provide an event collection SDK or API.
- Accept large volumes of events.
- Buffer events before analytical processing.
- Support near-real-time dashboards.
- Store historical events for analysis.
- Handle duplicate, delayed, and malformed events.

**Key challenges**
- Browser or mobile SDK batching.
- High-volume ingestion endpoints.
- Anonymous versus authenticated users.
- Event schema evolution.
- Event deduplication.
- Late-arriving events.
- Privacy and data deletion requirements.

**Important design decisions**
- Direct event ingestion versus regional collection gateways.
- Broker partitioning keys.
- Stream processing versus batch processing.
- Raw event storage versus pre-aggregated data.
- Columnar warehouse versus general-purpose database.

**What to master:** Event collection, schema validation, durable buffering, analytical storage, and real-time versus offline processing.

---

### 12. Product Analytics Platform

**Priority:** Core  
**Difficulty:** Hard

**Problem statement**

Design an analytics platform that enables product teams to answer questions such as:
- How many users signed up today?
- What percentage of users completed onboarding?
- Where do users abandon checkout?
- How many users returned after seven days?
- Which features are used most frequently?

**Requirements**
- Ingest product events.
- Track users and sessions.
- Compute funnels, retention, and conversion.
- Support segmentation by time, geography, platform, and cohort.
- Provide dashboards and ad hoc queries.

**Key challenges**
- Distinguishing users from sessions and devices.
- Identity resolution.
- Counting unique users at scale.
- Recomputing metrics when events arrive late.
- Handling schema changes.
- Supporting interactive analytical queries.

**Important concepts**
- Event-driven analytics.
- Sessionization.
- Cohort analysis.
- Approximate distinct counting.
- OLAP storage.
- Incremental aggregation.

**Difference from problem 11:** Clickstream collection focuses on reliably capturing events. Product analytics focuses on transforming those events into useful metrics and behavioral insights.

---

### 13. User Activity Tracking and Active Users Counter

**Priority:** Core  
**Difficulty:** Medium–Hard

**Problem statement**

Design a system that tracks daily active users (DAU), weekly active users (WAU), and monthly active users (MAU) across a large platform.

**Requirements**
- Ingest activity events.
- Count unique users within configurable windows.
- Segment by product, region, and platform.
- Support historical reporting.
- Update dashboards regularly.

**Key challenges**
- Exact distinct counting at scale.
- Duplicate events.
- Identity merging.
- Window boundaries and time zones.
- Recomputing counts after delayed events.
- Storage cost for user-level activity.

**Important design choices**
- Exact sets versus approximate cardinality structures.
- Pre-aggregation versus query-time computation.
- Event-time processing versus processing-time counting.
- Handling users active across multiple devices.

**Interview insight:** This is a strong problem for discussing the trade-off between exact answers and approximate answers using probabilistic data structures.

---

### 14. User Sessionization Service

**Priority:** Variant  
**Difficulty:** Medium–Hard

**Problem statement**

Design a system that converts raw user events into sessions. A session may end after a specified period of inactivity, such as 30 minutes.

**Example**

```text
10:00 - Page viewed
10:05 - Product clicked
10:12 - Search performed
11:00 - Page viewed

With a 30-minute inactivity threshold:
Session 1: 10:00–10:12
Session 2: 11:00
```

**Requirements**
- Consume clickstream events.
- Group events by user or session identifier.
- Detect session boundaries.
- Handle late-arriving events.
- Produce session summaries for analytics.

**Key challenges**
- Stateful stream processing.
- Event-time windows.
- Watermarks and allowed lateness.
- Session updates after delayed events.
- State expiration and recovery.

**Difference from problem 12:** Sessionization is a specific stateful transformation that feeds product analytics and user behavior analysis.

---

### 15. Real-Time Trending Topics or Popular Products

**Priority:** Core  
**Difficulty:** Hard

**Problem statement**

Design a system that continuously identifies the most popular topics, products, hashtags, or search terms over a recent time window.

**Example**

```text
Top 10 products viewed in the last 5 minutes
Top 100 hashtags in the last hour
Most searched terms in the last 15 minutes
```

**Requirements**
- Ingest large volumes of interaction events.
- Aggregate activity over rolling windows.
- Maintain top-K results.
- Update rankings continuously.
- Support regional or category-specific rankings.

**Key challenges**
- High-cardinality event dimensions.
- Rolling-window aggregation.
- Skewed keys caused by viral items.
- Approximate counting.
- Maintaining top-K results without sorting every item repeatedly.
- Handling traffic spikes.

**Important concepts**
- Count-Min Sketch.
- Heavy-hitter detection.
- Time-windowed counters.
- Top-K algorithms.
- Partitioned aggregation and merging.

**Difference from user activity tracking:** Active-user counting focuses on unique identities. Trending systems focus on event frequency and ranking.

---

### 16. Real-Time Funnel Analytics

**Priority:** Variant  
**Difficulty:** Medium–Hard

**Problem statement**

Design a system that measures how users progress through a sequence of actions, such as:

```text
Visit Website
    |
    v
View Product
    |
    v
Add to Cart
    |
    v
Checkout
    |
    v
Purchase
```

**Requirements**
- Ingest events continuously.
- Track ordered user actions.
- Calculate conversion and abandonment rates.
- Support different funnel definitions.
- Update results in near real time.

**Key challenges**
- Correlating events by user and session.
- Defining conversion windows.
- Handling out-of-order events.
- Repeated actions and multiple funnel attempts.
- Recomputing results after late events.

**Important distinction:** Simple funnels can be computed using SQL over event tables. Complex real-time funnels may require stateful streaming and incremental aggregation.

---

## Category D: Advertising, Impressions, and Event Counting

Advertising systems are especially valuable for senior HLD interviews because they combine enormous event volumes with strict accounting, low-latency reporting, and financial correctness.

### 17. Ad Impression and Click Tracking

**Priority:** Core  
**Difficulty:** Hard  
**Interview relevance:** Very high

**Problem statement**

Design a system that records ad impressions, clicks, and other interactions across websites, applications, and advertising networks.

**Requirements**
- Record impressions and clicks.
- Associate events with campaigns, creatives, publishers, and users.
- Support real-time campaign reporting.
- Detect invalid or fraudulent traffic.
- Provide historical analytics.

**Key challenges**
- Millions of events per second during peak traffic.
- Duplicate events caused by retries.
- Delayed click reports.
- Bot traffic and invalid impressions.
- High-cardinality aggregation dimensions.
- Consistent reporting across ingestion and analytical systems.

**Important design decisions**
- Event identifiers and idempotent ingestion.
- Real-time approximate reporting versus accurate finalized reports.
- Raw event retention.
- Campaign-level pre-aggregation.
- Late-arriving event correction.

**What to master:** The distinction between recording an event and counting it toward a business metric.

---

### 18. Ad Billing and Impression Reconciliation

**Priority:** Core  
**Difficulty:** Very Hard

**Problem statement**

Design a system that calculates billable advertising impressions and clicks, reconciles reports, and produces accurate campaign charges.

This is closely related to impression tracking, but the correctness requirements are substantially different.

**Requirements**
- Ingest impression and click events.
- Validate whether events are billable.
- Deduplicate events.
- Apply campaign-specific billing rules.
- Reconcile preliminary and finalized counts.
- Generate billing reports.

**Key challenges**
- Exactly-once business effects.
- Idempotent event processing.
- Duplicate and fraudulent event detection.
- Late-arriving events and corrections.
- Financial reconciliation.
- Auditable calculation history.

**Important design distinction**

A real-time dashboard may display approximate counts that are corrected later. A finalized billing report generally requires deterministic rules, traceable corrections, and stronger correctness guarantees.

**Interview insight:** Avoid claiming that a message broker alone provides end-to-end exactly-once business processing. You must explain how deduplication, state updates, transactions, and external side effects work together.

---

### 19. Real-Time Ad Campaign Analytics

**Priority:** Variant  
**Difficulty:** Hard

**Problem statement**

Design a system that provides advertisers with near-real-time campaign performance, including impressions, clicks, conversions, spend, and click-through rate.

**Requirements**
- Aggregate events by campaign and time window.
- Provide low-latency dashboards.
- Support geographic and demographic breakdowns where permitted.
- Correct aggregates when delayed events arrive.
- Reconcile preliminary metrics with finalized metrics.

**Key challenges**
- High-cardinality aggregation.
- Skewed campaigns with extremely high traffic.
- Incremental aggregation.
- Late conversion attribution.
- Exact versus approximate metrics.

**Difference from problem 17:** Impression tracking records interactions; campaign analytics turns those interactions into continuously updated performance metrics.

---

### 20. Conversion Attribution System

**Priority:** Advanced  
**Difficulty:** Very Hard

**Problem statement**

Design a system that attributes a conversion, such as a purchase or signup, to one or more earlier advertising interactions.

**Example**

```text
10:00 - User sees an ad
10:05 - User clicks the ad
10:20 - User visits the website
11:00 - User purchases a product
```

The system must determine which advertising interactions receive conversion credit under the selected attribution model.

**Requirements**
- Ingest impressions, clicks, and conversion events.
- Associate events with users or sessions.
- Support configurable attribution windows.
- Recalculate attribution when delayed events arrive.
- Provide campaign-level reporting.

**Key challenges**
- Identity resolution.
- Cross-device attribution.
- Out-of-order events.
- Attribution windows and model changes.
- Reprocessing historical data.
- Privacy restrictions and consent.

**Important concepts:** Event correlation, event-time processing, mutable aggregates, replay, and historical recomputation.

---

## Category E: IoT, Device Telemetry, and GPS

These systems involve large fleets of independent producers, unreliable networks, and data that may arrive late or out of order.

### 21. IoT Telemetry Ingestion Platform

**Priority:** Core  
**Difficulty:** Hard  
**Interview relevance:** Very high

**Problem statement**

Design a platform that receives telemetry from millions of IoT devices, such as sensors, industrial equipment, smart meters, and connected vehicles.

**Example event**

```json
{
  "device_id": "sensor-123",
  "temperature": 37.2,
  "battery_level": 82,
  "timestamp": "2026-10-27T04:30:00Z"
}
```

**Requirements**
- Authenticate devices.
- Ingest high-volume telemetry.
- Handle intermittent connectivity.
- Support device-level and fleet-level queries.
- Detect anomalies.
- Retain historical readings.

**Key challenges**
- Massive device concurrency.
- Uneven traffic distribution.
- Intermittent connectivity and reconnection storms.
- Duplicate or out-of-order telemetry.
- Per-device buffering and sequence numbers.
- Device provisioning and authentication.

**Important design decisions**
- HTTP versus MQTT or another device messaging protocol.
- Regional ingestion gateways.
- Partitioning by device ID or another key.
- Time-series storage versus object storage.
- Device-side versus server-side buffering.

**What to master:** Device fleet ingestion, durable buffering, partitioning, backpressure, and unreliable-network handling.

---

### 22. Smart Meter Reading and Energy Telemetry

**Priority:** Variant  
**Difficulty:** Hard

**Problem statement**

Design a system that collects electricity, gas, or water meter readings from millions of meters.

**Requirements**
- Collect periodic readings.
- Detect missing or implausible measurements.
- Store historical consumption.
- Support billing and usage analytics.
- Handle devices that report late or reconnect after outages.

**Key challenges**
- Periodic traffic spikes when many meters report together.
- Late and missing readings.
- Corrections to previously reported measurements.
- Accurate consumption aggregation.
- Long retention periods.
- Distinguishing duplicate readings from legitimate repeated measurements.

**Difference from generic IoT ingestion:** Smart metering introduces stronger requirements around measurement integrity, billing periods, and corrections.

---

### 23. Fleet GPS Tracking and Live Location Updates

**Priority:** Core  
**Difficulty:** Hard  
**Interview relevance:** Very high

**Problem statement**

Design a platform that receives GPS updates from vehicles, delivery drivers, or mobile assets and exposes their latest locations and movement history.

**Example**

```json
{
  "vehicle_id": "truck-123",
  "latitude": 17.3850,
  "longitude": 78.4867,
  "speed_kmh": 42,
  "timestamp": "2026-10-27T04:30:00Z"
}
```

**Requirements**
- Ingest frequent location updates.
- Show current vehicle locations on a map.
- Query historical routes.
- Support geofencing and location-based alerts.
- Track fleets across regions.

**Key challenges**
- High-frequency updates from large fleets.
- Last-write-wins semantics versus event history.
- Out-of-order GPS messages.
- Geospatial indexing.
- Hotspot queries and spatial partitioning.
- Separating current state from historical telemetry.

**Important design decisions**
- Keep the latest location in a low-latency store.
- Store historical points in a time-series or analytical store.
- Use geospatial indexes for proximity queries.
- Reduce redundant updates through batching or adaptive sampling.

**Critical interview insight:** The latest location and the full location history are different data products. You often need separate storage strategies for them.

---

### 24. Real-Time Ride Tracking

**Priority:** Core  
**Difficulty:** Hard

**Problem statement**

Design the location update and distribution component of a ride-hailing platform. Drivers publish locations, and riders receive relevant updates for their assigned trips.

**Requirements**
- Ingest driver locations.
- Track active drivers.
- Distribute updates to relevant riders.
- Support nearby-driver discovery.
- Expire stale locations.
- Handle driver disconnections.

**Key challenges**
- High-frequency writes.
- Real-time fan-out.
- Geospatial partitioning.
- Stale location detection.
- Connection management.
- Avoiding unnecessary global broadcasts.

**Difference from problem 23:** GPS tracking stores and queries locations. Ride tracking must also distribute the right updates to active users with low latency.

---

### 25. Geofencing and Location-Based Event Detection

**Priority:** Variant  
**Difficulty:** Hard

**Problem statement**

Design a system that detects when a device or vehicle enters or exits a geographic region.

**Example use cases**
- A delivery vehicle enters a warehouse perimeter.
- A shipment leaves a restricted zone.
- A vehicle enters a toll area.
- A mobile asset enters a designated operating region.

**Requirements**
- Ingest location updates.
- Match locations against geographic boundaries.
- Generate entry and exit events.
- Avoid repeated alerts while a device remains inside a region.
- Support many devices and geofences.

**Key challenges**
- Spatial indexing.
- Efficient point-in-polygon queries.
- Maintaining device-to-geofence state.
- GPS noise and boundary oscillation.
- Duplicate and out-of-order location events.
- Large-scale event generation.

**Important concepts:** Geospatial indexes, spatial partitioning, stateful stream processing, and hysteresis to prevent repeated transitions caused by noisy coordinates.

---

### 26. Industrial Equipment Monitoring and Predictive Maintenance

**Priority:** Advanced  
**Difficulty:** Hard–Very Hard

**Problem statement**

Design a system that ingests sensor data from industrial machinery to identify abnormal behavior and predict equipment failures.

**Requirements**
- Ingest high-frequency sensor readings.
- Correlate readings across sensors and devices.
- Detect anomalies in real time.
- Generate alerts and maintenance events.
- Retain historical telemetry for analysis and model training.

**Key challenges**
- High-frequency time-series ingestion.
- Sensor synchronization.
- Missing or noisy measurements.
- Stateful anomaly detection.
- Edge processing versus cloud processing.
- High-resolution retention versus aggregated long-term storage.

**Difference from generic IoT:** The ingestion architecture is similar, but predictive maintenance adds analytical and operational requirements around anomaly detection and failure prediction.

---

## Category F: Social Media, Likes, Views, and Counters

These are some of the best problems for learning the trade-offs between high write throughput, read latency, and exact counts.

### 27. High-Volume Likes and Reactions Counter

**Priority:** Core  
**Difficulty:** Hard  
**Interview relevance:** Very high

**Problem statement**

Design a system that records likes and reactions on posts, videos, photos, or comments for a social media platform.

**Requirements**
- Allow users to like and unlike content.
- Display like counts quickly.
- Prevent duplicate likes by the same user.
- Support high-volume posts.
- Maintain correct counts despite retries and concurrent operations.

**Key challenges**
- Hot keys for viral posts.
- Atomic updates.
- Duplicate requests.
- Concurrent like and unlike operations.
- Count consistency.
- Reconciliation after failures.

**Important design decisions**
- Store user-to-content relationships separately from aggregate counts.
- Use idempotent operations.
- Buffer counter updates where acceptable.
- Shard extremely hot counters.
- Merge partial counts for reads.
- Reconcile counters against the underlying relationship data.

**Critical distinction:** A like is a user-to-content relationship; a like count is an aggregate derived from those relationships. Treating both as the same piece of data creates correctness problems.

---

### 28. Video Views and View Counting System

**Priority:** Core  
**Difficulty:** Hard

**Problem statement**

Design a platform that counts video views accurately enough for product analytics, ranking, creator dashboards, and potentially monetization.

**Requirements**
- Collect view events from web and mobile clients.
- Apply the platform's view eligibility rules.
- Deduplicate events where necessary.
- Update visible counts.
- Support historical analytics.

**Key challenges**
- Defining a valid view.
- Duplicate requests and retries.
- Bot and fraudulent traffic.
- Extremely popular videos.
- Delayed event processing.
- Reconciling approximate real-time counts with finalized counts.

**Important design distinction**

A raw playback-start event is not necessarily a valid view. The system must define and enforce the business rule for counting a view.

For example, the rule may require a minimum playback duration or other eligibility conditions.

**Difference from problem 27:** View counts often derive from a stream of eligible events rather than a simple one-user-to-one-content relationship.

---

### 29. High-Volume Distributed Counter Service

**Priority:** Core  
**Difficulty:** Hard

**Problem statement**

Design a generic counter service that can process millions of increments per second and return current counts with low latency.

**Potential use cases**
- Likes.
- Views.
- Downloads.
- API requests.
- Reactions.
- Notification counts.
- Event totals.

**Requirements**
- Support increment and possibly decrement operations.
- Return current or near-current counts.
- Handle extremely hot counters.
- Recover from processing failures.
- Support periodic reconciliation.

**Key challenges**
- Atomicity versus throughput.
- Hot-key contention.
- Counter sharding.
- Duplicate increments.
- Read amplification.
- Merge consistency.

**Common design**

```text
Counter Update API
        |
        v
  Durable Event Log
        |
        v
 Counter Workers
        |
        v
  Sharded Counters
   /     |      \
Shard A Shard B Shard C
   \     |      /
        Merge
          |
          v
    Counter Read API
```

The exact design depends on whether the system must provide linearizable counts or whether eventual consistency is acceptable.

**What to master:** Sharded counters, idempotency, read-your-writes requirements, atomic updates, and reconciliation.

---

### 30. Viral Content and Hot-Key Management

**Priority:** Advanced  
**Difficulty:** Very Hard

**Problem statement**

Design a write path that remains stable when a small number of posts, videos, or products receive a disproportionate share of all traffic.

**Example**

A celebrity publishes a post that receives millions of likes in a short period. Most requests now target the same post ID.

**Requirements**
- Accept traffic for viral content.
- Avoid overloading a single database partition.
- Preserve acceptable counter accuracy.
- Support low-latency reads.
- Recover correctly after worker or partition failures.

**Key challenges**
- Key skew.
- Partition hotspots.
- Counter sharding.
- Adaptive batching.
- Cache invalidation.
- Read-time aggregation.
- Detecting and splitting hot keys dynamically.

**Important distinction:** A standard counter service explains how to scale writes generally. This problem focuses on the worst-case distribution, where a tiny number of keys dominate the workload.

---

### 31. API Usage Metering and Quota Counters

**Priority:** Core  
**Difficulty:** Hard

**Problem statement**

Design a system that tracks API requests, data usage, tokens consumed, or other billable activities for customers and enforces usage quotas.

**Requirements**
- Record usage by tenant, API key, and time window.
- Expose usage dashboards.
- Support quota checks.
- Generate billing reports.
- Handle concurrent requests.
- Support configurable limits.

**Key challenges**
- Extremely high update rates.
- Tenant isolation.
- Exact billing versus approximate dashboards.
- Atomic quota enforcement.
- Counter resets and time-window boundaries.
- Duplicate usage events.

**Critical design distinction:** Approximate counters can be acceptable for dashboards, but quota enforcement and billing may require stronger guarantees. A design should explicitly separate these use cases when necessary.

---

## Category G: Financial Market Data and High-Frequency Event Feeds

These systems are excellent for senior-level interviews because they emphasize throughput, ordering, replay, latency, and data correctness.

### 32. Financial Market Data Feed Ingestion

**Priority:** Core  
**Difficulty:** Very Hard  
**Interview relevance:** High for trading, fintech, and infrastructure roles

**Problem statement**

Design a platform that ingests market data from exchanges or market-data providers, including prices, trades, quotes, and order-book updates.

**Example events**

```text
10:00:00.001  AAPL  TRADE  225.40  100
10:00:00.002  AAPL  TRADE  225.42   50
10:00:00.003  AAPL  QUOTE  225.41  225.45
```

**Requirements**
- Ingest continuous market data.
- Preserve source ordering where required.
- Distribute data to downstream consumers.
- Support historical replay.
- Detect missing or out-of-sequence events.
- Store historical data for analytics.

**Key challenges**
- Very high event rates.
- Low-latency processing.
- Ordering guarantees.
- Sequence-number tracking.
- Feed gaps and recovery.
- Backpressure.
- Fan-out to multiple independent consumers.

**Important design decisions**
- Partitioning by instrument, exchange, or feed.
- In-memory processing versus durable persistence.
- Event-log retention and replay.
- Gap detection and snapshot recovery.
- Separating the critical low-latency path from analytical processing.

**What to master:** Ordering, sequence numbers, replay, gap recovery, and the trade-offs between durability and latency.

---

### 33. Real-Time Stock Price Aggregation and OHLC Candles

**Priority:** Core  
**Difficulty:** Hard

**Problem statement**

Design a system that processes incoming trade events and generates price aggregates over time intervals.

For example, produce one-minute candles containing:

- Open: First trade price.
- High: Highest trade price.
- Low: Lowest trade price.
- Close: Last trade price.
- Volume: Total traded quantity.

**Requirements**
- Consume trade events.
- Aggregate by instrument and time interval.
- Publish near-real-time results.
- Correct aggregates when late events arrive.
- Support historical queries.

**Key challenges**
- Event-time windowing.
- Ordering and late events.
- Efficient per-instrument aggregation.
- High-cardinality state.
- Corrections to previously emitted candles.
- Exact volume aggregation.

**Important distinction:** Market-data ingestion and OHLC aggregation are separate problems. The first reliably captures and distributes events; the second derives time-windowed values from them.

---

### 34. Financial Transaction Event Ingestion and Ledger Processing

**Priority:** Advanced  
**Difficulty:** Very Hard

**Problem statement**

Design a system that ingests large volumes of financial transaction events and reliably updates transaction records, account balances, or downstream financial reporting.

**Requirements**
- Accept transaction requests.
- Persist accepted transactions durably.
- Process transaction events.
- Prevent duplicate financial effects.
- Support reconciliation and auditability.
- Recover from failures.

**Key challenges**
- Idempotency.
- Transactional consistency.
- Ordering for individual accounts.
- Atomic updates across related records.
- Outbox and change-data-capture patterns.
- Replay without duplicating financial effects.

**Important distinction:** Financial transaction processing is not merely an event ingestion problem. Correctness of the underlying transaction and account state may be more important than raw throughput.

A strong design separates the authoritative transactional ledger from downstream analytics and reporting pipelines.

---

## Category H: Change Data Capture and Database Change Streams

This category is useful because the producers are databases rather than application clients or IoT devices.

### 35. Change Data Capture (CDC) Platform

**Priority:** Core  
**Difficulty:** Hard

**Problem statement**

Design a system that captures database changes and streams them to downstream consumers in near real time.

**Example**

```text
PostgreSQL
    |
    v
Transaction Log / WAL
    |
    v
CDC Connector
    |
    v
Durable Event Stream
    |
    +----> Search Index
    |
    +----> Cache
    |
    +----> Analytics Warehouse
    |
    +----> Data Lake
```

**Requirements**
- Capture inserts, updates, and deletes.
- Preserve change ordering where required.
- Deliver changes to multiple consumers.
- Recover from connector failures.
- Support snapshot initialization and incremental changes.

**Key challenges**
- Database log offsets and checkpoints.
- Snapshot-plus-stream consistency.
- Schema evolution.
- Delete propagation.
- Duplicate delivery and idempotent consumers.
- Backpressure from downstream consumers.

**Important concepts**
- Write-ahead logs.
- Log sequence numbers or equivalent offsets.
- Checkpointing.
- Change events and tombstones.
- Replay and recovery.

**What to master:** CDC is a foundational architecture for synchronizing operational databases with search indexes, caches, warehouses, and other systems.

---

### 36. Database-to-Search Index Synchronization

**Priority:** Variant  
**Difficulty:** Hard

**Problem statement**

Design a system that synchronizes changes from an operational database into a search engine without slowing down the application's transactional workload.

**Example**

```text
Product Database
       |
       v
 CDC / Change Stream
       |
       v
   Event Broker
       |
       v
 Indexing Workers
       |
       v
   Search Engine
```

**Requirements**
- Propagate database changes to the search index.
- Support document creation, updates, and deletion.
- Recover after indexing failures.
- Monitor synchronization lag.
- Rebuild indexes when mappings or schemas change.

**Key challenges**
- Eventual consistency.
- Out-of-order updates.
- Duplicate events.
- Deleted records and tombstones.
- Bulk indexing versus individual writes.
- Full reindexing without excessive load on the source database.

**Difference from generic CDC:** This problem focuses on maintaining a derived search index and managing the consistency gap between the source database and the index.

---

## Category I: Notification and Delivery Event Pipelines

These systems are write-heavy at the event-ingestion stage, even though their ultimate purpose is to deliver messages.

### 37. Notification Event Ingestion and Delivery Pipeline

**Priority:** Core  
**Difficulty:** Hard

**Problem statement**

Design a system that accepts notification requests from applications and delivers push notifications, emails, SMS messages, or in-app notifications to large numbers of users.

**Requirements**
- Accept notification requests.
- Buffer large delivery bursts.
- Prioritize urgent notifications.
- Retry temporary failures.
- Track delivery status.
- Prevent duplicate deliveries where possible.

**Key challenges**
- High-volume fan-out.
- Provider rate limits.
- Retry storms.
- Dead-letter queues.
- Per-user and per-tenant quotas.
- Idempotency across retries.
- Delivery-status ingestion.

**Important distinction:** There are two separate high-volume streams:

1. Notification requests entering the system.
2. Delivery, failure, retry, and engagement events returning from downstream providers.

A complete design may need to ingest both.

---

### 38. Email and SMS Delivery Events Analytics

**Priority:** Variant  
**Difficulty:** Medium–Hard

**Problem statement**

Design a system that ingests delivery events from email or SMS providers and produces reports on sent, delivered, bounced, failed, opened, or clicked messages.

**Requirements**
- Receive webhooks from external providers.
- Validate webhook authenticity.
- Deduplicate repeated callbacks.
- Track message lifecycle state.
- Aggregate delivery statistics.
- Support investigation of failures.

**Key challenges**
- Duplicate and out-of-order webhooks.
- Provider-specific event schemas.
- Idempotent state transitions.
- Large campaign bursts.
- Late delivery receipts.
- Reconciliation between send requests and provider reports.

**Difference from problem 37:** The primary concern is not sending notifications but reliably ingesting and reconciling events generated by external delivery systems.

---

## Category J: Data Ingestion Platforms and Analytical Event Pipelines

These are broader platform-design problems that often appear in senior backend, infrastructure, and data engineering interviews.

### 39. General-Purpose Event Streaming Platform

**Priority:** Core  
**Difficulty:** Very Hard  
**Interview relevance:** Very high for infrastructure roles

**Problem statement**

Design a distributed event streaming platform that accepts events from many producers and allows multiple independent consumers to process them.

Conceptually, this is the class of system represented by Kafka or Apache Pulsar.

**Requirements**
- Accept events from many producers.
- Partition event streams.
- Persist events durably.
- Allow independent consumer groups.
- Support replay from earlier offsets.
- Scale brokers and consumers horizontally.
- Handle broker and consumer failures.

**Key challenges**
- Partitioning and ordering.
- Replication and durability.
- Leader election and failover.
- Consumer offsets.
- Retention and replay.
- Backpressure and consumer lag.
- Rebalancing.
- Handling oversized messages and uneven partitions.

**Important design decisions**
- Partition key selection.
- Replication factor and acknowledgement policy.
- At-least-once delivery and duplicate handling.
- Retention by time versus storage size.
- Consumer group coordination.
- Log compaction versus ordinary retention.

**What to master:** This is one of the most reusable designs in the entire pattern. Many of the other systems listed here can be built on top of a durable event-streaming layer.

---

### 40. Unified Data Ingestion Platform for Analytics

**Priority:** Core  
**Difficulty:** Very Hard

**Problem statement**

Design a platform that ingests data from many different sources, validates and transforms it, and delivers it to analytical storage for downstream reporting and machine learning.

**Potential sources**
- Application events.
- Database changes.
- Logs and metrics.
- IoT devices.
- Third-party APIs.
- Batch file uploads.
- Business transaction systems.

**Requirements**
- Support multiple ingestion protocols.
- Validate schemas and enforce data contracts.
- Route events to appropriate pipelines.
- Handle malformed records.
- Support replay and reprocessing.
- Deliver data to a warehouse, lake, or analytical database.
- Monitor freshness, completeness, and pipeline health.

**Key challenges**
- Schema evolution.
- Data quality.
- Backpressure.
- Multiple ingestion patterns.
- Dead-letter handling.
- Replay without duplicate downstream effects.
- Data lineage and observability.
- Isolation between tenants and workloads.

**Important distinction:** A general-purpose event streaming platform focuses on durable event transport. A unified data ingestion platform focuses on the end-to-end lifecycle from source ingestion through validation, transformation, and delivery to analytical destinations.

---

# 3. How These 40 Problems Relate to Each Other

The most important thing to understand is that these problems are not 40 independent architectures.

They share several reusable design patterns.

| Shared design pattern | Problems where it is especially useful |
|---|---|
| Durable event ingestion | 1–40 |
| Message brokers and consumer groups | 2, 6, 11, 21, 32, 39, 40 |
| Time-series storage | 1–5, 21–26, 33 |
| Event-time windowing and aggregation | 3, 7, 12–16, 19, 33 |
| High-cardinality indexing | 1, 6, 8, 10, 11, 12 |
| Idempotency and deduplication | 9, 17–20, 27–31, 34–38 |
| Sharded counters and hot-key management | 15, 27–31 |
| Geospatial indexing | 23–26 |
| Ordering and replay | 32–36, 39, 40 |
| Schema evolution | 11, 12, 21, 35, 40 |
| Backpressure and load shedding | 1–40 |
| Retention and tiered storage | 1, 6–10, 21, 32–34, 39, 40 |
| Exactly-once business effects | 18, 27–29, 31, 34, 37, 38 |
| Reconciliation and correction | 9, 18–20, 27–34, 36, 38 |

The goal is to recognize which design problem the interviewer is actually testing.

For example, if you already understand a distributed counter service, a likes counter is mostly a question of applying those concepts to a social application. The interesting additional challenges are duplicate likes, user-to-post relationships, and viral content.

Similarly, if you understand clickstream ingestion, product analytics adds sessionization, funnels, identity resolution, and analytical queries rather than requiring you to reinvent the entire ingestion layer.

---

# 4. The Most Important Cross-Cutting HLD Concepts

Regardless of which problem you choose, you should be comfortable discussing the following topics.

## 4.1 Capacity Estimation

Always estimate the incoming event rate before selecting infrastructure.

Suppose a system receives 10 million events per minute.

The average event rate is:

\[
\text{Events per second} = \frac{10,000,000}{60}
\approx 166,667
\]

If each serialized event averages 1 KB:

\[
\text{Ingress bandwidth}
\approx 166,667 \times 1\text{ KB}
\approx 167\text{ MB/s}
\]

The approximate raw data volume per day is:

\[
167\text{ MB/s} \times 86,400
\approx 14.4\text{ TB/day}
\]

This estimate excludes replication, indexes, metadata, compression effects, and additional storage overhead.

In an interview, estimate at least:

- Average and peak events per second.
- Average event size.
- Incoming bandwidth.
- Daily and annual storage requirements.
- Replication and retention overhead.
- Number of partitions and consumers.
- Expected query volume.

**Important:** Never size the system solely for average throughput. Peak load and burst duration can determine the required buffering capacity.

## 4.2 Durable Buffering

A durable buffer separates the producer's availability from the consumer's availability.

For example:

```text
Producers
    |
    v
Ingestion API
    |
    v
Durable Broker
    |
    +------> Consumer A
    |
    +------> Consumer B
    |
    +------> Consumer C
```

If Consumer B is temporarily unavailable, the other consumers can continue independently, provided the broker and their resources remain healthy.

You should understand:

- Message acknowledgements.
- Producer retries.
- Consumer offsets.
- Partition ordering.
- Replication.
- Consumer lag.
- Replay.
- Dead-letter handling.
- Backpressure.

A durable broker is not automatically a guarantee of zero data loss. The actual guarantee depends on when the producer receives acknowledgement, the broker's persistence and replication configuration, and the application's retry behavior.

## 4.3 Partitioning Strategy

Partitioning is one of the most important decisions in write-heavy system design.

| Partition key | Advantages | Potential problems |
|---|---|---|
| Device ID | Groups events from one device | A very active device can become a hot key |
| User ID | Useful for user activity and session processing | Popular users or tenants may create skew |
| Event ID | Can distribute writes well | Does not preserve per-user ordering |
| Timestamp | Useful for time-based organization | Can create hotspots if all current writes target the same partition |
| Campaign ID | Convenient for campaign analytics | Viral campaigns can overload a partition |
| Instrument ID | Useful for market data ordering | Popular instruments can receive disproportionate traffic |
| Geographic region | Supports regional isolation | Regions may have very different traffic volumes |

The best partition key depends on which records need to be processed together and what ordering guarantees are required.

For instance, partitioning by device ID can simplify device-specific processing, but it does not automatically solve the problem of one device producing an unusually large amount of data.

## 4.4 Idempotency and Duplicate Events

Duplicates can arise from producer retries, network failures, consumer restarts, and repeated external webhooks.

A common approach is to assign a stable event identifier:

```json
{
  "event_id": "evt-123",
  "event_type": "video_view",
  "video_id": "video-456",
  "timestamp": "2026-10-27T04:30:00Z"
}
```

The consumer can use the event ID to prevent the same logical event from being applied more than once.

However, deduplication introduces additional questions:

- Where are processed event IDs stored?
- How long must they be retained?
- Is deduplication atomic with the state update?
- What happens when a retry arrives after the deduplication record expires?
- Can the consumer safely replay historical events?

For financial operations, the deduplication record and the business effect often need to be coordinated transactionally.

For less critical analytics, a bounded deduplication window or approximate deduplication may be sufficient.

## 4.5 Event Time Versus Processing Time

These are different concepts.

**Event time** is when the event actually occurred.

**Processing time** is when your system processes it.

Consider GPS data:

```text
10:00:00 - Device records location A
10:00:05 - Device records location B
10:00:10 - Device records location C

Network delay causes arrival order:
Location A
Location C
Location B
```

If the system uses arrival order without accounting for event timestamps or sequence numbers, it may incorrectly conclude that the device moved from C back to B.

The same problem occurs with delayed financial events, clickstream data, and market feeds.

You should understand:

- Event-time windows.
- Watermarks.
- Allowed lateness.
- Out-of-order processing.
- Corrections to emitted aggregates.
- Replay and recomputation.

## 4.6 Exact Versus Approximate Aggregation

Not every metric requires the same level of accuracy.

| Use case | Typical accuracy requirement |
|---|---|
| Live infrastructure dashboard | Near-real-time values may be sufficient |
| Trending hashtags | Approximate counts may be acceptable |
| Daily active users | Depends on reporting requirements |
| Preliminary ad performance | Approximate values may be acceptable |
| Final ad billing | Strong reconciliation and correctness |
| Financial ledger | Strict transactional correctness |
| Video view counter | Depends on business rules and reporting stage |
| API quota enforcement | Depends on quota semantics; overshoot may be unacceptable |

This is an important interview skill: **define the correctness requirement before choosing the storage or aggregation strategy**.

## 4.7 Backpressure and Overload

Suppose producers generate 200,000 events per second, but downstream consumers process only 150,000 events per second.

The backlog grows by:

\[
200,000 - 150,000 = 50,000
\]

events per second.

After one hour, assuming the rates remain constant, the backlog increases by:

\[
50,000 \times 3,600 = 180,000,000
\]

events.

A broker buys time; it does not create infinite processing capacity.

Potential responses include:

- Scaling consumers horizontally.
- Increasing parallelism where the partitioning strategy permits it.
- Batching writes.
- Applying backpressure.
- Rate-limiting producers.
- Dropping or sampling explicitly noncritical data.
- Prioritizing important event types.
- Increasing processing efficiency.
- Temporarily reducing optional enrichment or aggregation work.

Your design should explain what happens when the system cannot keep up.

---

# 5. Recommended Study Order for HLD Interviews

I would not solve all 40 problems independently. That would cause substantial repetition.

Instead, prepare in the following sequence.

## Phase 1: Master the Ingestion Fundamentals

Start here because these concepts appear in nearly every other problem.

| Order | Problem | What you should learn |
|---:|---|---|
| 1 | **39. General-Purpose Event Streaming Platform** | Partitioning, replication, ordering, durability, replay, consumer groups |
| 2 | **40. Unified Data Ingestion Platform** | End-to-end ingestion, schema validation, routing, transformations, failure recovery |
| 3 | **11. Clickstream Event Collection** | Event APIs, batching, event schemas, high-volume ingestion |
| 4 | **6. Centralized Log Aggregation** | Log ingestion, indexing, storage tiers, search |
| 5 | **1. Metrics and Time-Series Database** | Time-series storage, cardinality, downsampling, retention |

**Goal:** Become comfortable designing the common ingestion pipeline and explaining why each component exists.

## Phase 2: Master Streaming and Real-Time Processing

| Order | Problem | What you should learn |
|---:|---|---|
| 6 | **3. Real-Time Monitoring and Alerting** | Windowing, stream processing, state, alert delivery |
| 7 | **15. Trending Topics and Popular Products** | Heavy hitters, top-K, rolling windows, approximate aggregation |
| 8 | **12. Product Analytics Platform** | OLAP, analytical queries, cohorts, incremental aggregation |
| 9 | **14. User Sessionization** | Stateful processing, event-time windows, late events |
| 10 | **8. Distributed Tracing Backend** | Span ingestion, trace reconstruction, sampling |

**Goal:** Learn how to transform raw events into useful information without relying entirely on batch processing.

## Phase 3: Master Hot Keys and Counters

| Order | Problem | What you should learn |
|---:|---|---|
| 11 | **29. Distributed Counter Service** | Atomic updates, sharded counters, aggregation |
| 12 | **27. Likes and Reactions Counter** | Idempotency, relationship data, counter consistency |
| 13 | **30. Viral Content and Hot-Key Management** | Skew, partition hotspots, adaptive sharding |
| 14 | **28. Video Views Counter** | Event eligibility, deduplication, eventual consistency |
| 15 | **31. API Usage Metering and Quota Counters** | Rate limits, usage accounting, consistency requirements |

**Goal:** Understand why a single hot key can become a bottleneck even when the rest of the system scales horizontally.

## Phase 4: Master Reliability and Correctness

| Order | Problem | What you should learn |
|---:|---|---|
| 16 | **9. Audit Logging** | Immutable records, reliable capture, compliance |
| 17 | **18. Ad Billing and Impression Reconciliation** | Deduplication, accounting, corrections |
| 18 | **35. Change Data Capture** | Database logs, checkpoints, snapshot consistency |
| 19 | **36. Database-to-Search Synchronization** | Eventual consistency, indexing failures, reindexing |
| 20 | **34. Financial Transaction Ingestion** | Transactions, idempotency, ledger correctness |

**Goal:** Be able to explain how your system recovers from failures without silently losing or incorrectly duplicating important business events.

## Phase 5: Master Domain-Specific Variants

After the first four phases, study the remaining problems based on the companies and roles you are targeting.

| Domain | Recommended problems |
|---|---|
| Advertising | 17. Impression Tracking, 19. Campaign Analytics, 20. Conversion Attribution |
| IoT | 21. IoT Ingestion, 22. Smart Metering, 26. Predictive Maintenance |
| GPS and location | 23. Fleet GPS, 24. Ride Tracking, 25. Geofencing |
| Analytics | 13. Active Users, 16. Funnel Analytics, 5. Business Metrics |
| Observability | 2. Metrics Collection, 4. APM, 7. Log Anomaly Detection |
| Security | 10. SIEM |
| Market data | 32. Market Data Feed, 33. OHLC Aggregation |
| Notifications | 37. Notification Pipeline, 38. Delivery Event Analytics |

---

# 6. Which Problems Should You Prioritize for Google and Meta Interviews?

For general backend and infrastructure interviews, I would prioritize these **15 problems**.

| Rank | Problem | Primary interview skill |
|---:|---|---|
| 1 | General-Purpose Event Streaming Platform | Distributed systems fundamentals |
| 2 | Clickstream Event Collection | High-volume ingestion |
| 3 | Centralized Log Aggregation | Storage and search architecture |
| 4 | Metrics and Time-Series Database | Time-series design |
| 5 | Real-Time Monitoring and Alerting | Stream processing |
| 6 | Distributed Counter Service | High-write scalability |
| 7 | Likes and Reactions Counter | Consistency and idempotency |
| 8 | Viral Content and Hot-Key Management | Partition skew |
| 9 | Product Analytics Platform | Analytical data pipelines |
| 10 | IoT Telemetry Ingestion | Device-scale ingestion |
| 11 | Fleet GPS Tracking | High-frequency updates and geospatial queries |
| 12 | Distributed Tracing Backend | Observability data modeling |
| 13 | Change Data Capture | Database integration and replay |
| 14 | Real-Time Trending Topics | Streaming aggregation |
| 15 | Audit Logging Platform | Reliability and correctness |

This is a preparation recommendation, not a claim about the frequency of specific questions at any particular company. Actual interviews depend on the team, level, and interview format.

For senior roles, prepare to go beyond the happy path. Interviewers may ask you to handle a tenfold traffic spike, a failing region, a hot partition, a growing consumer backlog, or a requirement to replay events without corrupting downstream state.

---

# 7. A Reusable HLD Checklist for Every Problem

When you practice a system design problem, answer these questions in order.

### A. Requirements

1. Who produces the data?
2. What does one event look like?
3. What is the average and peak event rate?
4. How large is each event?
5. What is the required ingestion latency?
6. What are the read and query requirements?
7. Is event ordering required?
8. Can events be dropped?
9. Must duplicates be prevented?
10. How long must data be retained?

### B. Architecture

1. How do producers authenticate and send data?
2. Where does validation happen?
3. Where is the durable buffer?
4. How are events partitioned?
5. How many consumers are required?
6. How are events transformed?
7. Where is raw data stored?
8. Where are aggregates stored?
9. How are queries served?
10. How are failures detected and recovered?

### C. Scalability

1. What is the partition key?
2. Can a single partition become a hotspot?
3. Can consumers scale horizontally?
4. What happens when the input rate exceeds processing capacity?
5. Can batching improve throughput?
6. How are storage and indexing costs controlled?
7. How is traffic distributed across regions?

### D. Reliability and Correctness

1. When does the producer receive an acknowledgement?
2. What happens if a broker fails?
3. What happens if a consumer crashes after processing but before acknowledging?
4. Can an event be processed twice?
5. How are malformed events handled?
6. Can historical events be replayed?
7. How are late events handled?
8. How are incorrect aggregates repaired?
9. Is eventual consistency acceptable?
10. Are there operations that require stronger transactional guarantees?

### E. Operations

1. How do you monitor ingestion throughput?
2. How do you measure consumer lag?
3. How do you detect data loss or gaps?
4. How do you manage retention?
5. How do you deploy schema changes?
6. How do you handle a regional outage?
7. How do you test recovery and replay?

---

# Final Recommendation

For this pattern, I would focus on mastering **five foundational designs** first:

1. **Event streaming platform** — how to ingest, partition, replicate, and replay events.
2. **Time-series and metrics platform** — how to store and query timestamped data.
3. **Clickstream analytics pipeline** — how to transform raw events into useful insights.
4. **Distributed counter service** — how to handle high-write workloads and hot keys.
5. **Audit logging and reliable event processing** — how to preserve correctness across failures.

Once you understand these five, many other designs become variations rather than completely new problems.

For every design, practice explaining the architecture, estimating capacity, choosing partition keys, handling failures, and identifying the consistency guarantees. Those skills are more valuable than memorizing a particular combination of Kafka, Redis, a database, and a stream-processing framework.
