Sure! Here's a detailed **High-Level System Design (HLSD)** plan for a **Unified Notification System** supporting **Email, SMS, and Push Notifications** that handles retries, failure handling, bulk sending, and extensibility.

---

# 1. Functional and Non-Functional Requirements

### Functional Requirements

* **Send notification via multiple channels:** Email, SMS, Push.
* **Bulk notifications:** Support batch sending to many users.
* **Retry mechanism:** Retry sending notifications on failure with configurable policies.
* **Failure handling:** Track and report failures, notify administrators, and provide dashboards.
* **Extensibility:** Easily add new channels (e.g., WhatsApp, voice call) without major system changes.
* **Scheduling:** Support immediate and scheduled notification sending.
* **Personalization:** Support templated messages with variables per user.
* **Delivery status tracking:** Track delivered, failed, pending, and retried statuses.
* **API:** Provide REST/gRPC APIs for clients to send notifications, check status, and manage templates.
* **Rate limiting:** Per-user or per-tenant rate limiting to avoid spamming.
* **Monitoring & alerting:** Real-time metrics on notification success, failure, and throughput.

### Non-Functional Requirements

* **Scalability:** Handle millions of notifications daily, scale horizontally.
* **Low latency:** Notifications should be sent as soon as possible (seconds-level delay acceptable).
* **High availability:** 99.99% uptime.
* **Durability:** Ensure no messages lost; guarantee at-least-once delivery.
* **Consistency:** Eventual consistency acceptable for delivery status.
* **Security:** Authentication, authorization, and secure storage of user data.
* **Throughput:** Support bursts of notifications (e.g., marketing campaigns).

### Assumptions

* System is multi-tenant (multiple clients/companies use it).
* Supports both transactional and bulk marketing notifications.
* Push notifications go to mobile devices via Firebase/APNs or internal push service.
* SMS and Email providers are third-party services (e.g., Twilio, SendGrid).
* Bulk sends can be large (100k+ recipients), so async processing is critical.

---

# 2. Back-of-the-Envelope Estimation

| Metric                    | Estimate & Justification                                     |
| ------------------------- | ------------------------------------------------------------ |
| **Users (Tenants)**       | 1,000 clients                                                |
| **Daily Active Users**    | 10 million recipients per day                                |
| **QPS (Peak)**            | \~1000-2000 QPS (peak) from API for notification requests    |
| **Avg notifications/day** | \~20 million (mix of transactional + bulk)                   |
| **Storage size**          | \~500GB - 1TB/month (logs, statuses, templates)              |
| **Bandwidth**             | Email/SMS \~1MB per 1000 messages; Push <100KB per 1000 msgs |

---

# 3. API Design

### REST/gRPC APIs

| Endpoint                              | Method | Description                                   | Request Example                                                 |
| ------------------------------------- | ------ | --------------------------------------------- | --------------------------------------------------------------- |
| `/sendNotification`                   | POST   | Send one or bulk notifications                | Notification payload with channel info, template ID, recipients |
| `/getStatus/{notificationId}`         | GET    | Get delivery status of a notification         | notificationId                                                  |
| `/createTemplate`                     | POST   | Create a new message template                 | Template content and variables                                  |
| `/updateTemplate/{id}`                | PUT    | Update existing template                      | Template updates                                                |
| `/listTemplates`                      | GET    | List all templates for tenant                 | Tenant ID                                                       |
| `/retryNotification/{notificationId}` | POST   | Manually trigger retry of failed notification | notificationId                                                  |

---

# 4. High-Level Architecture

```
+--------------------+
|  API Gateway / LB  |<-- Load balancer: distributes API requests
+---------+----------+
          |
+---------v----------+
| Notification       |  Stateless web/API servers handling requests, auth, validation
| Service            |
+---------+----------+
          |
+---------v----------+      +--------------+
| Notification Queue |----->| Worker Pool  |-----> External Providers (Email, SMS, Push)
| (Kafka/RabbitMQ)   |      +--------------+
+---------+----------+
          |
          +---------------> +------------------+
                           | Retry Manager     |
                           +------------------+
          |
+---------v----------+
| Notification Store |  SQL/NoSQL DB for storing metadata, status, templates
+--------------------+

+--------------------+
| Monitoring & Alerting System |
+--------------------+
```

### Components & Roles

* **API Gateway / Load Balancer:** Handles authentication, throttling, routes requests.
* **Notification Service:** Receives requests, validates, persists metadata, enqueues jobs.
* **Notification Queue:** Decouples request intake from processing, supports bulk async processing.
* **Worker Pool:** Multiple workers consume queue, send to external providers, update status.
* **Retry Manager:** Listens for failed messages, applies retry policy, re-enqueues jobs.
* **Notification Store:** Durable storage of notifications, templates, delivery status.
* **Monitoring & Alerting:** Collect metrics, health checks, alert ops on failures.

### Communication protocols

* Internal communication: REST or gRPC for efficiency.
* External providers: HTTP APIs (REST), SMTP, SMPP for SMS, Firebase SDK for push.
* Queue: Kafka/RabbitMQ for reliable message passing.

---

# 5. Component Deep Dive

### Data Storage

* **Choice:**

  * SQL (e.g., PostgreSQL) for metadata, templates, transactional consistency.
  * NoSQL (e.g., Cassandra, DynamoDB) for storing delivery logs for write-heavy workloads.
* **Schema:**

| Table: Notifications           |
| ------------------------------ |
| notification\_id (PK)          |
| tenant\_id                     |
| channel (email/sms/push)       |
| recipient                      |
| template\_id                   |
| status (pending, sent, failed) |
| created\_at                    |
| updated\_at                    |
| retry\_count                   |
| scheduled\_time                |

| Table: Templates  |
| ----------------- |
| template\_id (PK) |
| tenant\_id        |
| channel           |
| content           |
| variables         |
| created\_at       |
| updated\_at       |

* **Sharding/Partitioning:**

  * By `tenant_id` and/or `notification_id` hash for horizontal scalability.
  * Time-based partitioning for logs to improve query performance.

### Caching Strategy

* Cache frequently used templates in an in-memory cache (e.g., Redis) at application layer for fast template rendering.
* Use **write-through cache** to keep cache consistent with DB updates.
* CDN caching not applicable directly but consider caching static assets in push notification payloads.

### Load Balancing

* **Type:** Layer 7 HTTP Load Balancer (e.g., AWS ALB, NGINX).
* Balancer placed in front of API Gateway and Worker Pools.
* Load balancer uses **Round Robin** or **Least Connections** algorithm.

---

# 6. Scalability and Availability

* **Horizontal scaling:**

  * Stateless API servers and workers can be scaled out easily.
  * Notification queue supports partitioned topics for concurrent processing.
  * DB scaling via sharding and read replicas.
* **No Single Point of Failure (SPOF):**

  * Multi-zone deployment for all components.
  * Replicated Kafka/RabbitMQ clusters.
  * Multi-AZ DB deployment with failover.
  * Health checks and auto-restart on failures.
* **High Availability:**

  * Use retries with exponential backoff.
  * Dead-letter queue for messages failing all retries.
  * Circuit breaker pattern for flaky external providers.

---

# 7. Potential Bottlenecks and Trade-offs

### Bottlenecks

* **External provider rate limits and outages:** Can slow or block sending.
* **Database write throughput** for status updates at scale.
* **Queue backpressure** if workers cannot keep up during spikes.
* **Bulk sending overhead** - large batches can overwhelm workers if not chunked properly.

### Trade-offs

* **Consistency vs availability:** Eventual consistency on delivery status tracking accepted for better availability.
* **Complexity vs extensibility:** Plugin-based channel adapters increase flexibility but add complexity.
* **Latency vs durability:** Use of queue adds delay but improves reliability and scale.
* **Retries vs duplicate sends:** Idempotency needed to avoid double sends during retries.

### Future Improvements

* Add machine learning for adaptive retry strategies.
* Integrate user preference management for channels.
* Support advanced scheduling (e.g., time zone awareness).
* Add more notification channels (WhatsApp, Voice).
* Support transactional analytics dashboards.

---

Nice — great concrete scenario. Below I’ll walk you through a clear, production-ready **step-by-step** design for a unified notification pipeline that handles both **transactional events** (Order/Payment/Matching services) and **admin-initiated bulk campaigns**, showing how the **event bus**, **notification service**, **workers**, retries, DLQs, idempotency, provider failover, and delivery receipts all play together.

I'll state key assumptions up front, then give (A) transactional flow, (B) admin/bulk flow, and (C) detailed component behaviors (workers, retries, webhooks, data model, throttling, monitoring, security). Wherever helpful I’ll include example payloads, sequence text-diagrams, and pseudocode.

# Assumptions

* You run microservices (Order, Payment, Matching, etc.) that emit domain events.
* You have an event streaming system (preferably **Kafka** for throughput/partitioning) and a task queue (can be Kafka topics + consumer groups, or RabbitMQ for rate-control).
* External channels = Email (SendGrid), SMS (Twilio/SMPP), Push (FCM/APNs).
* Multi-tenant: notifications are tenant-scoped.
* Need **at-least-once** delivery with idempotency to prevent duplicates.
* Campaigns can be large (100k+ recipients), must not block transactional notifications.

---

# Event envelope (canonical)

All producers send a standard event envelope to the event bus:

```json
{
  "event_id": "uuid-v4",
  "event_type": "order.placed",         // semantic event
  "tenant_id": "tenant_123",
  "user_id": "user_456",                // recipient user
  "correlation_id": "req-abc-123",      // traceable through system
  "timestamp": "2025-08-12T12:34:56Z",
  "payload": { /* domain payload e.g. order details */ }
}
```

Use this envelope across transactional and admin flows (admin events include campaign metadata).

---

# A. Transactional flow (Order -> Notification)

Short description: microservice emits event → event bus → notification service consumes → enqueues per-channel jobs → channel workers send → update status and handle retries.

## Step-by-step

1. **Producer emits domain event**
   e.g., Order Service publishes `order.placed` to topic `events.order` with canonical envelope.

2. **Event Bus (Kafka) stores event**

   * Topic `events.*` partitioned by `user_id` (key) to preserve per-user ordering if needed.
   * Event is durable; producers get ACK from Kafka.

3. **Notification Service (consumer) subscribed**

   * A consumer group `notif-ingest` consumes relevant topics or a filtered stream (either directly or via a stream processor like Kafka Streams).
   * The consumer only *ingests* and converts domain events to notification *jobs*; it does not call external providers here.

4. **Event → Notification mapping (ingest logic)**

   * Apply rules: which events produce notification(s), which channel(s) (email/push/sms), which template to use, personalization variables, localization/timezone, suppression rules (user preferences, do-not-disturb), and throttling limits.
   * Example: `order.placed` → Push (order summary) + Email (receipt).

5. **Persist Notification Entity(s)**

   * For each resulting notification job, create a row/document in `notifications` store:

     ```
     notification_id, event_id, tenant_id, user_id, channel, template_id, status=pending, attempt=0, created_at
     ```
   * This is the source of truth for status and retrying.

6. **Enqueue Job to delivery topic/queue**

   * Publish a `notification.job` message to a dedicated Kafka topic `notification.jobs.{channel}` (e.g., `notification.jobs.push`, `notification.jobs.sms`, `notification.jobs.email`) keyed by `user_id` to preserve order per user/channel.
   * For throughput isolation, have separate topics for transactional vs marketing: `notification.jobs.tx.*` and `notification.jobs.marketing.*`.

7. **Worker pool consumes jobs**

   * Channel-specific worker pools (stateless) subscribe to their `notification.jobs.*` topic. Workers are horizontally scalable.
   * Worker takes the job, checks idempotency, renders template (using cached template), and sends to provider API.

8. **Send to external provider**

   * Worker uses a `ProviderAdapter` (abstract interface). Each adapter handles rate-limiting, retries at provider call-layer, and maps provider response to standard status. Use circuit breaker around providers.

9. **Update status & receipts**

   * On success: persist `status=sent`, store `provider_message_id`, timestamp.
   * On transient failure: increment `attempt` and requeue with backoff (exponential + jitter).
   * On permanent failure: set `status=failed`, possibly add to DLQ for manual/automated handling.

10. **Delivery receipts / webhooks**

    * Providers may asynchronously POST delivery reports. Implement a `provider/webhook` endpoint to receive these, validate signature, match by `provider_message_id` → update `status=delivered` / `bounced`, update metrics.

## Simple sequence ASCII

```
Order Service -> Kafka (events.order) -> Notification Ingest Consumer
  -> DB(notification) -> Kafka(notification.jobs.push)
  -> Push Workers -> FCM -> Provider sends -> Webhook -> Update status
```

---

# B. Admin dashboard / Bulk campaign flow

Admin creates campaign (targeting + content). Campaign engine expands recipients and creates notification jobs in a controlled, scalable way.

## Step-by-step

1. **Admin submits campaign**

   * Dashboard calls `Campaign Service` API with campaign definition:

     * channels, template id, tenant\_id, targeting (all users / segment / uploaded list), schedule (now / scheduled time), rate limits, per-user personalization variables.

2. **Campaign Service creates Campaign record**

   * Persist `campaign_id`, metadata, targeting rules, recipient count estimate, status.

3. **Resolve recipients (segmentation)**

   * If targeting is a segment, run a query against user DB or analytics store (OLAP). For huge segments, produce a recipient file in S3 (streaming export). For uploaded lists, store file in S3.

4. **Chunking & fan-out**

   * Campaign Service *streams* recipients in chunks (e.g., 1k users per chunk for email, 100 for SMS) into a bulk ingestion topic `notification.jobs.marketing.{channel}` or into a bulk ingestion worker which publishes `notification.job` messages (one per recipient) to `notification.jobs.marketing.*`.
   * Important: do NOT publish all notifications at once if that would overwhelm downstream (use backpressure).

5. **Backpressure & rate shaping**

   * Use a bulk-worker/ingestor that reads recipient file and publishes at a controlled rate using token-buckets that respect provider rate limits and tenant quotas.

6. **Workers process as in transactional flow**

   * Channel workers consume from `notification.jobs.marketing.*` and send. Because campaign traffic is on separate topics/consumer groups, it does not block transactional topics.

7. **Progress & resume**

   * Persist per-chunk progress; if campaign fails or restarts, resume from last offset. Provide admins progress UI.

8. **Reporting & analytics**

   * Aggregate send/delivery/bounce metrics into time-series DB and per-campaign dashboards.

## Sequence for bulk

```
Admin -> Campaign Service -> store campaign -> export recipients (S3) ->
Bulk Ingest Worker -> notification.jobs.marketing.email (topic)
-> Email workers -> SendGrid -> webhook -> update campaign metrics
```

---

# C. Component deep dives & operational details

## 1) Idempotency & de-duplication

* **Idempotency key**: `idempotency_key = event_id + channel + tenant_id` (or for campaigns `campaign_id + user_id + channel`). Use this in DB and Redis to prevent duplicate sends.
* **Dedup store**: Keep a short-lived dedup cache (Redis SET or DB unique constraint) with TTL. When a worker gets a job, it checks this before sending.
* Also make provider requests idempotent when provider supports it (some allow idempotency headers).

## 2) Ordering

* If ordering per user is important (e.g., "Order Cancelled" must arrive after "Order Placed"): partition topics by `user_id` and ensure only one consumer (or appropriately-ordered consumers) per partition. Kafka preserves order within a partition.

## 3) Retries and Dead Letter Queue (DLQ)

* **Worker retry policy**:

  * Classify errors: transient (5xx, network timeout) vs permanent (4xx, invalid number).
  * For transient: exponential backoff with jitter, e.g. initial 30s, 2^n \* 30s, max attempts 5.
  * For permanent: mark failed, send bounce reasoning to analytics.
* **DLQ**:

  * After max attempts, publish to DLQ topic `notification.dlq` for manual / automated remediation.
* **Retry manager**:

  * Could be built using delayed retries (Kafka + a delayed queue like Kafka + topic with delay handler or use Redis-based sorted set + worker) or use managed task queue supporting delay (e.g., AWS SQS + DLQ).

## 4) Provider failover & circuit breaker

* Use **provider adapter** abstraction that handles:

  * Rate-limits and per-provider quotas.
  * Circuit breaker: after N failures, open circuit for T seconds and route to fallback provider (if configured) or pause sends.
  * Multi-provider strategy: choose primary provider, if rate-limited or circuit-open, switch to secondary.

## 5) Rate limiting & throttling

* **Per-tenant limits** (to enforce SLA): token bucket per tenant for each channel.
* **Per-provider limits**: token bucket per provider to avoid hitting provider rate-limits.
* **Per-recipient pacing**: avoid spamming a single user — enforce per-user min interval for notifications.

## 6) Template rendering & personalization

* Templates stored in DB and cached in Redis. Worker loads cached template and renders using variables from event payload (e.g., name, order\_id, ETA). Cache invalidation on template update (write-through).

## 7) Data model (essential tables/collections)

* `notifications` (single record per send attempt):

  * notification\_id PK, tenant\_id, campaign\_id (nullable), event\_id, user\_id, channel, payload, template\_id, status, attempt, provider\_message\_id, last\_error, created\_at, updated\_at
* `campaigns`: campaign\_id, tenant\_id, template\_id, targeting, total\_recipients, progress, status
* `templates`: template\_id, tenant\_id, channel, content, variables
* `providers`: provider\_id, channel, config, priority
* `delivery_logs` (time-series): notification\_id, provider\_status, provider\_message\_id, timestamp
* Indexing: partition by tenant\_id and created\_at, separate time-series table for logs.

## 8) Provider delivery receipts (callback)

* Providers send callback containing `provider_message_id` and status. Flow:

  1. Provider POST -> `webhook/receipt` (validate signature).
  2. Lookup `notification` by `provider_message_id`. If not found, write to `orphan_webhooks` and retry resolving.
  3. Update `notifications.status` and write to `delivery_logs`.
  4. Emit events to metrics / campaign aggregator.

## 9) Observability & tracing

* Add tracing headers (`correlation_id`) through the pipeline.
* Emit metrics: per-channel QPS, success%, retry rate, DLQ count, provider latency.
* Logs: structured logs with notification\_id and correlation\_id.
* Alerts for queue backlog growth, high DLQ rates, provider circuit opens.

## 10) Security & access

* Mutual TLS or JWT between services. API Gateway enforces tenant auth & rate limits.
* Admin dashboard RBAC and audit logs for campaign activity.
* Encrypt PII at rest; minimize PII in logs.

## 11) Scaling & partitioning recommendations

* Kafka partitions by `tenant_id` or `user_id` depending on ordering needs.
* Separate topics for `transactional` and `marketing` to isolate workloads.
* Worker autoscaling based on `lag` (Kafka consumer lag) and available tokens per provider.
* Database: use write-optimized store (Cassandra / DynamoDB) for high-write logs and relational DB (Postgres) for metadata if strong consistency needed.

---

# Example worker pseudocode (simplified)

```python
def process_job(job):
    idempotency_key = job.idempotency_key
    if dedup_cache.exists(idempotency_key):
        mark_status(job, 'duplicate')
        return

    dedup_cache.set(idempotency_key, ttl=3600)

    notification = db.get_notification(job.notification_id)
    template = cache.get_or_load_template(notification.template_id)
    rendered = render(template, job.payload)

    provider = select_provider(notification.channel)
    try:
        response = provider.send(rendered, job.recipient)
        db.update_notification(notification.id, status='sent',
                                provider_message_id=response.id, attempt+=1)
    except TransientError as e:
        if notification.attempt < MAX_ATTEMPTS:
            schedule_retry(job, backoff(notification.attempt))
            db.increment_attempt(notification.id)
        else:
            db.update_notification(notification.id, status='failed', last_error=str(e))
            publish_to_dlq(job)
    except PermanentError as e:
        db.update_notification(notification.id, status='failed', last_error=str(e))
```

---

# Tuning knobs & recommended defaults

* **Bulk chunk size:** Email = 100–200 per worker push to provider; SMS = 50; Push = 500 (depends on provider).
* **Worker concurrency:** start with 10–50 workers per channel, scale via metrics and lag.
* **Retry policy:** attempts=5, base\_backoff=30s, exponential factor 2, jitter=±10%.
* **Dedup TTL:** 24–72 hours for transactional; keep campaign keys longer if campaign might re-run.

---

# Implementation checklist (practical steps)

1. Define canonical event envelope & producers: ensure all services emit events with `event_type` & `user_id`.
2. Set up Kafka topics:

   * `events.*` (domain events)
   * `notification.jobs.tx.*` (transactional)
   * `notification.jobs.marketing.*` (bulk)
   * `notification.dlq`
3. Build Notification Ingest consumer & mapping rules (config-driven, per-tenant).
4. Build Notification DB schema, templates store, and caching layer.
5. Implement channel Worker services with ProviderAdapter interface.
6. Implement retry manager or delayed requeue mechanism.
7. Implement webhook endpoint for provider receipts.
8. Add monitoring dashboards and alerts.
9. Implement campaign engine with recipient export + backpressure writer.
10. Add rate-limiting, circuit-breaker, and provider failover logic.
11. Run load tests for typical and burst scenarios; tune chunk sizes and concurrency.

---

# Final notes — tradeoffs & pitfalls

* **Throughput vs ordering:** preserving perfect per-user order implies partitioned topic keyed by user and will limit parallelism on hot users. Choose per-use-case.
* **At-least-once vs duplicates:** you’ll get duplicates without exact once; rely on idempotency to dedupe.
* **Marketing vs transactional isolation:** ALWAYS separate topics/queues so a massive campaign doesn’t impact transactional delivery.
* **Provider rate limits & outages:** always plan for multi-provider failover and backpressure upstream.

---
Alright — let’s break this into **three parts**:

1. **Kafka topic and partitioning plan** (with reasoning + consumer group configs)
2. **Sample worker implementation** (Java, but I can also give Node if needed) with **circuit-breaker + retry**
3. **Handling priority notifications** in the same system

---

## 1️⃣ Kafka Topic & Partitioning Plan

We have **two broad categories** of notifications:

* **Transactional** (order placed, payment confirmed, OTP, etc.) → High priority, low latency, strict SLAs.
* **Marketing/Bulk** (promotions, campaigns) → Can be delayed or throttled.

### **Topics**

| Topic Name                          | Purpose                                                    | Partitions             | Retention | Notes                                               |
| ----------------------------------- | ---------------------------------------------------------- | ---------------------- | --------- | --------------------------------------------------- |
| `events.domain`                     | All raw domain events from services (order, payment, etc.) | 48 (or per throughput) | 7 days    | Partition key = `user_id` to keep per-user ordering |
| `notification.jobs.tx.email`        | Transactional email jobs                                   | 24                     | 3 days    | Key = `user_id`                                     |
| `notification.jobs.tx.sms`          | Transactional SMS jobs                                     | 24                     | 3 days    | Key = `user_id`                                     |
| `notification.jobs.tx.push`         | Transactional push jobs                                    | 24                     | 3 days    | Key = `user_id`                                     |
| `notification.jobs.marketing.email` | Bulk email jobs                                            | 48                     | 3 days    | Key = `campaign_id` to spread load evenly           |
| `notification.jobs.marketing.sms`   | Bulk SMS jobs                                              | 48                     | 3 days    | Key = `campaign_id`                                 |
| `notification.jobs.marketing.push`  | Bulk push jobs                                             | 48                     | 3 days    | Key = `campaign_id`                                 |
| `notification.dlq`                  | Dead-letter jobs for all channels                          | 12                     | 14 days   | Key = `notification_id`                             |

---

### **Partition Count Reasoning**

* **Transactional** topics: lower volume but must be fast → fewer partitions (easier ordering guarantees per user).
* **Marketing** topics: higher volume, no strict ordering → more partitions for throughput.
* For each 1,000 messages/sec → \~12 partitions (assuming \~80MB/s broker throughput per partition).
* Size these based on peak expected QPS.

---

### **Consumer Groups**

| Consumer Group Name | Subscribes To                       | Instance Count                      | Notes                                 |
| ------------------- | ----------------------------------- | ----------------------------------- | ------------------------------------- |
| `notif-ingest`      | `events.domain`                     | Scale with services emitting events | Converts domain → notification jobs   |
| `notif-email-tx`    | `notification.jobs.tx.email`        | = partitions                        | One worker per partition for ordering |
| `notif-sms-tx`      | `notification.jobs.tx.sms`          | = partitions                        | Same as above                         |
| `notif-push-tx`     | `notification.jobs.tx.push`         | = partitions                        | Same as above                         |
| `notif-email-mkt`   | `notification.jobs.marketing.email` | Multiple per partition              | Bulk can parallelize                  |
| `notif-sms-mkt`     | `notification.jobs.marketing.sms`   | Multiple per partition              | Bulk can parallelize                  |
| `notif-push-mkt`    | `notification.jobs.marketing.push`  | Multiple per partition              | Bulk can parallelize                  |
| `notif-retry`       | Retry topics                        | Same as tx/mkt workers              | Handles retries separately            |
| `notif-dlq-handler` | `notification.dlq`                  | Few                                 | Manual/auto fixes                     |

---

### **Config Snippets**

**Kafka topic config:**

```bash
kafka-topics.sh --create \
  --topic notification.jobs.tx.email \
  --partitions 24 \
  --replication-factor 3 \
  --config retention.ms=259200000 \
  --config cleanup.policy=delete
```

**Consumer config (Java properties):**

```properties
bootstrap.servers=kafka1:9092,kafka2:9092,kafka3:9092
group.id=notif-email-tx
enable.auto.commit=false
max.poll.records=500
max.poll.interval.ms=300000
fetch.min.bytes=1
fetch.max.wait.ms=500
session.timeout.ms=10000
heartbeat.interval.ms=3000
```

---

## 2️⃣ Java Worker (Email Channel) — with Circuit Breaker + Retry

We’ll use:

* **Resilience4j** for circuit breaker + retry
* **Kafka Consumer API**
* **Template rendering** + **ProviderAdapter** abstraction

```java
public class EmailWorker {
    private static final int MAX_ATTEMPTS = 5;
    private final KafkaConsumer<String, NotificationJob> consumer;
    private final ProviderAdapter provider;
    private final NotificationRepository repo;
    private final CircuitBreaker cb;
    private final Retry retry;

    public EmailWorker(KafkaConsumer<String, NotificationJob> consumer,
                       ProviderAdapter provider,
                       NotificationRepository repo) {
        this.consumer = consumer;
        this.provider = provider;
        this.repo = repo;

        this.cb = CircuitBreaker.ofDefaults("email-provider");
        this.retry = Retry.of("email-retry",
                RetryConfig.custom()
                    .maxAttempts(MAX_ATTEMPTS)
                    .waitDuration(Duration.ofSeconds(30))
                    .intervalFunction(IntervalFunction.ofExponentialBackoff(30_000, 2.0))
                    .build());
    }

    public void start() {
        consumer.subscribe(Collections.singletonList("notification.jobs.tx.email"));
        while (true) {
            ConsumerRecords<String, NotificationJob> records = consumer.poll(Duration.ofMillis(500));
            for (ConsumerRecord<String, NotificationJob> record : records) {
                NotificationJob job = record.value();
                processJob(job);
            }
            consumer.commitSync();
        }
    }

    private void processJob(NotificationJob job) {
        String idempotencyKey = job.getNotificationId() + ":email";

        if (repo.isDuplicate(idempotencyKey)) {
            return; // already sent
        }

        Runnable sendTask = () -> {
            try {
                String rendered = renderTemplate(job);
                String providerMsgId = provider.sendEmail(job.getRecipient(), rendered);
                repo.markSent(job.getNotificationId(), providerMsgId);
            } catch (TransientProviderException e) {
                throw e; // trigger retry
            } catch (PermanentProviderException e) {
                repo.markFailed(job.getNotificationId(), e.getMessage());
            }
        };

        try {
            Retry.decorateRunnable(retry, CircuitBreaker.decorateRunnable(cb, sendTask)).run();
        } catch (Exception e) {
            repo.markFailed(job.getNotificationId(), e.getMessage());
            publishToDlq(job);
        }
    }

    private String renderTemplate(NotificationJob job) {
        Template t = repo.getTemplate(job.getTemplateId());
        return t.render(job.getVariables());
    }

    private void publishToDlq(NotificationJob job) {
        // produce to notification.dlq
    }
}
```

---

## 3️⃣ Handling **Priority Notifications**

We need **priority queues** inside Kafka.

### **Options**

1. **Separate Topics per Priority**

   * `notification.jobs.tx.high` (OTP, critical alerts)
   * `notification.jobs.tx.normal` (receipts)
   * Workers consume `high` first (or on separate dedicated consumer group).
   * This is simplest and guarantees ordering per priority level.

2. **Same Topic, Priority in Message**

   * Store `priority` field in message and use a **priority-aware consumer** (less common in Kafka; requires consuming all partitions and manually reordering).
   * Not ideal at high scale.

3. **Hybrid**

   * Transactional → split into `tx.high` and `tx.standard`.
   * Marketing always low priority in separate topics.

---

### **Recommended Implementation for Food Delivery**

* OTPs, payment confirmations → **`notification.jobs.tx.high.*`** (dedicated workers with SLA target < 2s).
* Order updates → **`notification.jobs.tx.standard.*`** (slightly lower priority workers).
* Marketing → **`notification.jobs.marketing.*`** (bulk pool with throttling).
* Ingest service routes events to correct topic based on rules table.

---

If you want, I can now draw a **priority-aware Kafka flow diagram** showing how `order.placed` vs `otp.requested` travel through the system and how high-priority workers preempt bulk campaigns.

That would make it crystal clear how the **event bus → topic routing → worker pools** interact under load.

Do you want me to prepare that diagram?

