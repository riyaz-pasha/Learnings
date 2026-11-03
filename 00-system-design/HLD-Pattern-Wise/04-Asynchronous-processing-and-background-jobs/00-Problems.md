# Asynchronous Processing & Background Jobs — HLD System Design Problem Roadmap

This is one of the most important HLD system design patterns for backend and distributed systems interviews at Google, Meta, Amazon, Uber, Netflix, Stripe, LinkedIn, etc.

The central idea is:

> Move expensive, time-consuming, unreliable, or independently executable work out of the synchronous request-response lifecycle and process it asynchronously using queues, workers, schedulers, and distributed processing infrastructure.

You should be able to design systems where:

- An API accepts a request quickly without waiting for the actual work to finish.
- Jobs are persisted and processed asynchronously.
- Workers scale independently based on workload.
- Failed jobs are retried without duplicating side effects.
- Processing survives worker crashes and infrastructure failures.
- Users can track job progress and receive completion notifications.
- The system handles bursts, backpressure, prioritization, scheduling, and ordering.

One important distinction: Asynchronous processing is not a single system design problem. It is a reusable architectural pattern appearing in many different products.

For interview preparation, I recommend learning the shared architecture first, then solving problems that introduce different challenges rather than designing ten nearly identical queue-and-worker systems.

# 1. Complete problem catalogue

I've grouped the problems into 10 categories. Each problem includes its functional scope, the asynchronous processing requirement, and the important HLD concepts it tests.

## Category A: File processing and media processing

These are among the strongest problems for understanding background workers, distributed processing, resource-intensive workloads, and pipeline orchestration.

### 1. Video Transcoding Platform



Must solve

High priority



Problem: Design a system like YouTube's video processing pipeline.

Users upload videos in different formats and resolutions. The system processes them into multiple formats and resolutions so they can be streamed on different devices and network conditions.

Functional requirements:

- Upload large video files.
- Support multipart and resumable uploads.
- Generate multiple resolutions (360p, 480p, 720p, 1080p, 4K).
- Generate thumbnails and video metadata.
- Track processing status.
- Notify users when processing finishes.

Asynchronous workflow:

1. User uploads a video to object storage.
2. API creates a video-processing job.
3. Job enters a queue.
4. Workers pick up transcoding tasks.
5. Different encoding profiles execute.
6. Processed files are stored in object storage.
7. Metadata is updated.
8. User is notified.

Important design challenges:

- Large file uploads and multipart uploads.
- CPU/GPU-intensive workers.
- Multiple processing stages.
- Parallel transcoding.
- Job prioritization.
- Worker failures and retries.
- Processing progress tracking.
- Temporary storage management.
- Idempotency and duplicate execution.

Concepts to master: S3/object storage, queues, worker pools, fan-out/fan-in, workflow orchestration, dead-letter queues, autoscaling.

### 2. Image Processing and Resizing Service

Must solve

Problem: Design an image processing service like the image infrastructure used by Instagram or an e-commerce platform.

Users upload images, and the system generates optimized versions for different devices and use cases.

Functional requirements:

- Upload images.
- Generate thumbnails.
- Resize images into multiple dimensions.
- Convert formats (JPEG, PNG, WebP, AVIF).
- Compress images.
- Apply watermarks.
- Serve processed images through a CDN.

Asynchronous workflow:

Upload → Storage → Processing Queue → Image Workers → Multiple Variants → Object Storage → CDN.

Important design challenges:

- Processing thousands of images simultaneously.
- Different image transformations.
- Deduplicating identical processing requests.
- Handling malformed or extremely large images.
- Avoiding repeated transformations.
- Cache invalidation.
- Prioritizing thumbnail generation over less important transformations.

Concepts to master: Event-driven architecture, object storage notifications, worker pools, CDN, idempotency, caching.

### 3. Document Processing and Conversion Platform

Must solve

Problem: Design a document processing service like Google Docs document conversion or an enterprise document processing platform.

Users upload documents in different formats. The system extracts content, converts formats, generates previews, and makes documents searchable.

Functional requirements:

- Upload PDF, DOCX, XLSX, PPTX and other formats.
- Generate PDF versions.
- Extract text and metadata.
- Generate document previews.
- Perform OCR on scanned documents.
- Generate thumbnails.
- Track processing status.

Important design challenges:

- Different processing engines for different file formats.
- CPU/memory-intensive OCR.
- Multi-stage processing pipelines.
- Corrupt document handling.
- Large document processing.
- Processing timeouts.
- Retry policies.
- Document versioning.

Concepts to master: Workflow orchestration, queues, specialized workers, object storage, state machines.

### 4. Audio Processing and Transcription Service

Must solve

Problem: Design an audio processing platform like a podcast processing or speech-to-text service.

Users upload audio recordings. The system transcribes audio, generates summaries, extracts metadata, and creates different audio formats.

Functional requirements:

- Upload audio files.
- Convert audio formats.
- Normalize audio.
- Generate transcripts.
- Identify speakers.
- Generate summaries.
- Store transcription results.
- Notify users when processing completes.

Important design challenges:

- Long-running jobs.
- GPU-intensive speech recognition.
- Parallel processing of audio segments.
- Combining segment results in the correct order.
- Managing external AI inference APIs.
- Handling model failures and rate limits.

Concepts to master: Pipeline orchestration, parallel processing, fan-out/fan-in, rate limiting, external service integration.

### 5. Video Thumbnail and Preview Generation

Problem: Design a service that automatically generates video thumbnails, preview clips, and keyframes when users upload videos.

Important design challenges:

- Extracting frames at different timestamps.
- Generating multiple thumbnail candidates.
- Processing priorities.
- Separating lightweight metadata jobs from expensive video jobs.
- Handling failed thumbnail generation independently of transcoding.

Concepts to master: Job dependencies, task decomposition, independent queues, priority scheduling.

## Category B: Report generation and document generation

These are particularly valuable because they test long-running jobs, database-heavy workloads, large result sets, and downloadable artifacts.

### 6. Report Generation System



Must solve

High priority



Problem: Design a reporting platform like the reporting systems used by Stripe, Salesforce, or enterprise analytics applications.

Users request reports involving large datasets. The system generates reports in the background and allows users to download them.

Functional requirements:

- Request reports through an API.
- Support CSV, Excel and PDF.
- Support filters and custom date ranges.
- Generate reports containing millions of records.
- Track report generation progress.
- Notify users upon completion.
- Allow downloading generated reports.
- Automatically expire old reports.

Important design challenges:

- Large database queries.
- Avoiding long-running HTTP requests.
- Database read replicas.
- Pagination and cursor-based scanning.
- Memory-efficient streaming generation.
- Snapshot consistency.
- Report expiration and cleanup.
- Multiple concurrent report requests.

Concepts to master: Asynchronous jobs, database optimization, streaming, object storage, job status tracking, resource isolation.

### 7. PDF Generation Service

Problem: Design a centralized PDF generation platform used by multiple applications.

Applications submit data or templates, and the service generates PDFs asynchronously.

Examples:

- Invoices.
- Receipts.
- Financial statements.
- Certificates.
- Legal agreements.
- Account statements.

Important design challenges:

- Template management.
- Dynamic data injection.
- Rendering engines.
- Fonts and localization.
- Large PDF generation.
- Template versioning.
- Retries without duplicate documents.
- Secure document storage.

Concepts to master: Job queues, template engines, worker pools, object storage, idempotency.

### 8. Bulk Data Export System

Must solve

Problem: Design a system that allows users to export large amounts of data from an application.

For example, a user wants to export all transactions, customers, orders, or audit logs.

Functional requirements:

- Request data export.
- Support CSV, JSON and Excel.
- Export millions of records.
- Support filters and date ranges.
- Generate downloadable files.
- Notify users upon completion.
- Support export expiration.

Important design challenges:

- Efficient database scanning.
- Streaming instead of loading everything into memory.
- Database load isolation.
- Consistent exports while records are changing.
- Large file generation.
- Download security.
- Concurrent export limits.

Concepts to master: Cursor pagination, database snapshots, streaming, queues, object storage, rate limiting.

### 9. Invoice and Financial Statement Generation

Problem: Design a system that generates invoices, monthly statements, and financial documents for customers.

Functional requirements:

- Generate invoices after transactions.
- Generate periodic statements.
- Support multiple currencies.
- Generate PDF documents.
- Send documents through email.
- Maintain document history.
- Regenerate documents when required.

Important design challenges:

- Financial correctness.
- Exactly-once business effects.
- Idempotent invoice generation.
- Transaction consistency.
- Document versioning.
- Scheduled monthly processing.
- Auditability.

Concepts to master: Transactional outbox, idempotency, scheduled jobs, workflow orchestration, database consistency.

## Category C: Notifications and message delivery

These are extremely important for distributed systems interviews because they introduce retries, delivery guarantees, provider failures, prioritization, and rate limiting.

### 10. Email Delivery System



Must solve

High priority



Problem: Design an email delivery platform like SendGrid, Amazon SES, or an internal enterprise email service.

Applications submit emails, and the platform delivers them asynchronously.

Functional requirements:

- Send transactional emails.
- Send bulk marketing emails.
- Support templates.
- Support attachments.
- Track delivery status.
- Handle bounces and complaints.
- Support scheduling.
- Provide delivery analytics.

Important design challenges:

- Large email volumes.
- SMTP/provider rate limits.
- Retry policies.
- Duplicate email prevention.
- Provider failures.
- Email prioritization.
- Bounce handling.
- Dead-letter queues.
- Suppression lists.

Concepts to master: Message queues, competing consumers, retry mechanisms, exponential backoff, rate limiting, provider abstraction.

### 11. SMS Delivery Platform

Must solve

Problem: Design an SMS notification service like Twilio.

Applications submit SMS messages, and the system delivers them using external telecom providers.

Functional requirements:

- Send OTPs.
- Send transactional notifications.
- Send bulk SMS.
- Support multiple countries.
- Support multiple SMS providers.
- Track delivery status.
- Handle delivery failures.

Important design challenges:

- External provider rate limits.
- OTP delivery latency.
- Regional provider selection.
- Provider failover.
- Retry and duplicate SMS prevention.
- Message prioritization.
- Delivery receipts.

Concepts to master: Priority queues, provider routing, circuit breakers, retries, rate limiting, delivery tracking.

### 12. Push Notification Service

Must solve

Problem: Design a push notification platform like Firebase Cloud Messaging infrastructure.

Applications send notifications to mobile devices and browsers.

Functional requirements:

- Register device tokens.
- Send notifications to individual users.
- Send notifications to millions of users.
- Support topics and user segments.
- Support scheduled notifications.
- Track delivery results where supported.

Important design challenges:

- Massive fan-out.
- Token expiration.
- Dead device cleanup.
- Notification prioritization.
- Provider throttling.
- Delivery analytics.
- User preferences.

Concepts to master: Fan-out, batching, partitioning, queue scaling, provider integration.

### 13. Multi-Channel Notification System

Problem: Design a unified notification service that supports email, SMS, push notifications, and in-app notifications.

Applications send a notification request, and the platform selects the appropriate delivery channels.

Functional requirements:

- Support multiple channels.
- User notification preferences.
- Notification templates.
- Delivery scheduling.
- Retry failed deliveries.
- Track delivery status.
- Support fallback channels.

Example:

An important payment failure occurs:

1. Send push notification.
2. If push delivery fails or times out according to policy, send SMS.
3. Send an email containing additional information.
4. Track the complete delivery workflow.

Important design challenges:

- Multi-step workflows.
- Channel-specific queues.
- User preferences.
- Channel fallback.
- Duplicate suppression.
- Delivery guarantees.
- Provider abstraction.

Concepts to master: Workflow orchestration, state machines, fan-out, retries, idempotency.

### 14. OTP Delivery System

Problem: Design a highly reliable OTP generation and delivery service.

Functional requirements:

- Generate OTPs.
- Send via SMS or email.
- Validate OTPs.
- Expire OTPs.
- Enforce resend limits.
- Prevent brute-force attempts.

Important design challenges:

- Low delivery latency.
- High availability.
- Rate limiting.
- Provider failover.
- Duplicate requests.
- OTP expiration and consistency.
- Abuse prevention.

Concepts to master: Caching, TTL, priority queues, rate limiting, synchronous validation with asynchronous delivery.

## Category D: Bulk processing and data pipelines

These problems introduce large-scale processing, partitioning, parallelism, checkpoints, and recovery.

### 15. Bulk Import System



Must solve

High priority



Problem: Design a bulk data import platform that allows users to upload CSV or Excel files containing millions of records.

Examples:

- Import customers.
- Import products.
- Import employee records.
- Import financial transactions.
- Import inventory.

Functional requirements:

- Upload large files.
- Validate schemas.
- Validate individual rows.
- Process valid records.
- Generate error reports.
- Track import progress.
- Support retrying failed records.

Important design challenges:

- Millions of rows.
- Batch processing.
- Partial success.
- Invalid records.
- Duplicate detection.
- Database write throughput.
- Checkpointing.
- Restarting failed imports.

Concepts to master: Chunking, batch processing, partitioning, bulk database inserts, idempotency, checkpointing.

### 16. Bulk Data Update System

Problem: Design a platform that updates millions of existing records based on user-defined operations.

Examples:

- Update product prices.
- Change customer attributes.
- Update employee information.
- Apply discounts to selected products.
- Change inventory values.

Important design challenges:

- Large-scale database writes.
- Transaction boundaries.
- Partial failures.
- Concurrent updates.
- Optimistic locking.
- Rate limiting.
- Rollback and compensation.

Concepts to master: Batch workers, write partitioning, transactions, checkpoints, optimistic concurrency.

### 17. Search Indexing System

Must solve

Problem: Design an asynchronous indexing platform that keeps Elasticsearch/OpenSearch synchronized with the primary database.

For example, an e-commerce application needs to index products whenever they are created or updated.

Functional requirements:

- Index newly created records.
- Update existing documents.
- Delete removed documents.
- Support bulk reindexing.
- Recover from indexing failures.
- Track indexing lag.

Important design challenges:

- Database-to-search synchronization.
- Event ordering.
- Duplicate events.
- Out-of-order updates.
- Bulk indexing.
- Reindexing without downtime.
- Eventual consistency.

Concepts to master: CDC, transactional outbox, Kafka, partitioning, idempotent consumers, version-based updates.

### 18. Data Synchronization Platform

Problem: Design a platform that synchronizes data between multiple systems.

Examples:

- CRM to ERP.
- E-commerce platform to warehouse.
- Payment provider to accounting system.
- Internal database to external SaaS application.

Functional requirements:

- Synchronize create/update/delete operations.
- Support scheduled synchronization.
- Support incremental synchronization.
- Track sync status.
- Retry failed records.
- Resolve conflicts.

Important design challenges:

- Source-of-truth selection.
- Change detection.
- Conflict resolution.
- Out-of-order updates.
- Duplicate events.
- External API limitations.
- Incremental checkpoints.

Concepts to master: CDC, event-driven architecture, idempotency, reconciliation, retry policies.

### 19. Database Migration and Backfill System

Problem: Design a system to migrate or backfill millions or billions of records between databases or data stores.

Examples:

- Migrate MySQL to PostgreSQL.
- Move data between database clusters.
- Populate a newly introduced database column.
- Backfill historical data.
- Migrate documents into a new search index.

Important design challenges:

- Large-scale scanning.
- Checkpointing.
- Consistency during migration.
- Dual writes or CDC.
- Throttling to protect production.
- Restarting failed partitions.
- Validation and reconciliation.

Concepts to master: Batch processing, CDC, partitioning, checkpoints, consistency, throttling.

### 20. ETL / Data Processing Pipeline

Problem: Design a distributed data processing system that extracts data from multiple sources, transforms it, and loads it into a destination.

Functional requirements:

- Ingest data from multiple sources.
- Transform records.
- Validate data.
- Load into warehouses.
- Support scheduled pipelines.
- Track pipeline execution.
- Retry failed stages.

Important design challenges:

- Large datasets.
- Parallel execution.
- Stage dependencies.
- Data quality.
- Pipeline failures.
- Checkpointing.
- Backpressure.
- Reprocessing historical data.

Concepts to master: DAG execution, workflow orchestration, batch processing, stream processing, distributed workers.

## Category E: Webhooks and event delivery

These are excellent interview problems because they test delivery guarantees, ordering, retries, customer isolation, and failure recovery.

### 21. Webhook Delivery Platform



Must solve

High priority



Problem: Design a webhook delivery platform like Stripe's webhook infrastructure.

When an event occurs in a platform, the system sends an HTTP request to a registered customer endpoint.

Functional requirements:

- Register webhook endpoints.
- Subscribe to specific event types.
- Deliver events asynchronously.
- Retry failed deliveries.
- Track delivery attempts.
- Support replaying events.
- Allow customers to inspect delivery history.

Example:

A payment succeeds:

1. Payment service publishes `payment.succeeded`.
2. Webhook service receives the event.
3. Event is persisted.
4. Delivery job enters a queue.
5. Worker sends an HTTP POST to the customer's endpoint.
6. Delivery result is recorded.
7. Failed deliveries are retried.

Important design challenges:

- Customer endpoint timeouts.
- HTTP 5xx failures.
- Exponential backoff.
- Duplicate webhook delivery.
- Event ordering.
- Customer-specific rate limits.
- Endpoint isolation.
- Signature verification.
- Replay and dead-letter handling.

Concepts to master: At-least-once delivery, idempotency, retries, exponential backoff, tenant isolation, event persistence.

### 22. Webhook Event Processing and Replay System

Problem: Design a system that allows customers to replay previously generated webhook events.

For example, a customer had an outage and missed webhook events for the last 24 hours. They want to replay them.

Important design challenges:

- Event retention.
- Replay windows.
- Large-scale replay.
- Duplicate delivery.
- Ordering during replay.
- Replay throttling.
- Separating live traffic from replay traffic.
- Preventing replay from overwhelming customer endpoints.

Concepts to master: Event logs, replayable queues, consumer offsets, rate limiting, idempotency.

### 23. Event Notification and Fan-out System

Problem: Design a service that distributes an event to thousands or millions of subscribers.

Examples:

- A creator publishes a post and followers receive notifications.
- A product becomes available and interested customers are notified.
- A stock price alert triggers notifications.
- A new video is published and subscribers receive updates.

Important design challenges:

- Large fan-out.
- Fan-out-on-write versus fan-out-on-read.
- Hot users and celebrity accounts.
- Batching.
- Queue partitioning.
- Subscriber management.
- Notification deduplication.

Concepts to master: Fan-out, partitioning, distributed queues, batching, load balancing.

## Category F: Scheduled jobs and recurring background tasks

These problems are especially useful for understanding distributed schedulers, delayed execution, recurring jobs, and coordination across multiple workers.

### 24. Distributed Job Scheduler



Must solve

High priority



Problem: Design a distributed job scheduling platform like Quartz Scheduler or a simplified version of Temporal's scheduling capabilities.

Users and applications can schedule jobs to execute at specific times or recurring intervals.

Functional requirements:

- Schedule one-time jobs.
- Schedule recurring jobs.
- Support cron expressions.
- Cancel or modify jobs.
- Track execution history.
- Retry failed jobs.
- Support millions of scheduled jobs.

Important design challenges:

- Accurate execution time.
- Scheduler high availability.
- Distributed locking or partition ownership.
- Duplicate execution prevention.
- Time zones and daylight saving.
- Handling missed schedules.
- Scheduler failover.
- Large numbers of jobs with the same execution time.

Concepts to master: Time wheels, delayed queues, cron scheduling, distributed coordination, leases, idempotency.

### 25. Recurring Billing and Subscription Processing

Problem: Design a system that processes recurring payments for millions of subscriptions.

Examples:

- Monthly subscriptions.
- Annual subscriptions.
- SaaS billing.
- Membership renewals.

Functional requirements:

- Schedule billing dates.
- Generate payment jobs.
- Process payments asynchronously.
- Retry failed payments.
- Handle payment-provider failures.
- Send renewal notifications.
- Maintain billing history.

Important design challenges:

- Millions of subscriptions due simultaneously.
- Scheduling accuracy.
- Duplicate payment prevention.
- Payment idempotency.
- Retry policies.
- Grace periods.
- Payment reconciliation.
- Time-zone handling.

Concepts to master: Distributed scheduling, idempotency, workflow orchestration, transactional outbox, payment state machines.

### 26. Reminder and Notification Scheduler

Problem: Design a service that schedules reminders and delivers them at a future time.

Examples:

- Appointment reminders.
- Meeting reminders.
- Medication reminders.
- Subscription renewal reminders.
- Payment due reminders.

Functional requirements:

- Create reminders.
- Schedule delivery.
- Support recurring reminders.
- Cancel reminders.
- Reschedule reminders.
- Deliver through multiple channels.

Important design challenges:

- Large delayed-job volumes.
- Accurate scheduling.
- Cancellation races.
- Time-zone handling.
- Recurring job calculation.
- Delivery retries.

Concepts to master: Delayed queues, time wheels, scheduling, distributed workers, state management.

### 27. Periodic Cleanup and Maintenance System

Problem: Design a background processing system that performs recurring maintenance operations.

Examples:

- Delete expired sessions.
- Remove expired files.
- Archive old records.
- Clean temporary uploads.
- Delete expired reports.
- Purge old logs.
- Remove inactive device tokens.

Important design challenges:

- Large-scale deletion.
- Avoiding database load spikes.
- Batch deletion.
- Safe retries.
- Retention policies.
- Resource throttling.
- Concurrent cleanup jobs.

Concepts to master: Scheduled jobs, batch processing, TTL, partitioned deletion, rate limiting.

## Category G: Business workflows and long-running transactions

These problems are particularly valuable for senior-level interviews because they move beyond simple queue consumption into multi-stage business processes.

### 28. Order Fulfillment and Processing System

Must solve

Problem: Design an asynchronous order processing system for an e-commerce platform.

After a customer places an order, multiple independent operations need to happen.

Functional requirements:

- Accept orders.
- Reserve inventory.
- Process payment.
- Generate invoices.
- Notify customers.
- Arrange shipping.
- Track order status.

Example workflow:

Order Created → Inventory Reservation → Payment Processing → Invoice Generation → Shipping Request → Notification.

Important design challenges:

- Multiple independent services.
- Workflow state management.
- Partial failures.
- Inventory reservation expiration.
- Payment failures.
- Compensation actions.
- Duplicate events.
- Long-running workflows.

Concepts to master: Saga pattern, orchestration versus choreography, transactional outbox, state machines, idempotency.

### 29. Payment Processing and Reconciliation System

Problem: Design an asynchronous payment processing and reconciliation platform.

Functional requirements:

- Accept payment requests.
- Communicate with payment providers.
- Process payment status updates.
- Handle delayed provider responses.
- Reconcile transactions.
- Generate settlement reports.
- Notify customers.

Important design challenges:

- External provider timeouts.
- Unknown payment outcomes.
- Duplicate payment prevention.
- Delayed callbacks.
- Eventual consistency.
- Periodic reconciliation.
- Audit trails.
- Retry safety.

Concepts to master: Idempotency keys, state machines, outbox/inbox patterns, reconciliation, retry policies.

### 30. Account Onboarding and KYC Processing

Problem: Design an onboarding platform that processes user registration and identity verification asynchronously.

Functional requirements:

- Accept registration.
- Upload identity documents.
- Perform OCR.
- Verify documents with external providers.
- Run risk checks.
- Approve or reject applications.
- Notify applicants.

Important design challenges:

- Multiple dependent processing stages.
- External API latency.
- Human review stages.
- Job status tracking.
- Sensitive document storage.
- Retry and escalation.
- Workflow timeout handling.

Concepts to master: Workflow orchestration, state machines, human-in-the-loop workflows, external service integration.

### 31. Insurance Claims Processing System

Problem: Design a platform that processes insurance claims asynchronously.

Functional requirements:

- Accept claims.
- Upload supporting documents.
- Extract document information.
- Run eligibility checks.
- Assess claims.
- Route complex cases for manual review.
- Approve or reject claims.
- Generate settlement documents.

Important design challenges:

- Long-running workflows.
- Parallel validation.
- External data providers.
- Manual approval stages.
- Auditability.
- Workflow recovery.
- Deadline and SLA tracking.

Concepts to master: Durable workflows, DAG execution, state machines, retries, human approval steps.

### 32. E-commerce Product Catalog Processing

Problem: Design a system that processes product catalog updates from sellers.

Functional requirements:

- Accept bulk product uploads.
- Validate product information.
- Process product images.
- Generate search indexes.
- Update inventory.
- Publish product listings.
- Report errors to sellers.

Important design challenges:

- Multiple dependent background jobs.
- Bulk imports.
- Parallel image processing.
- Search indexing.
- Partial success.
- Product versioning.
- Event ordering.

Concepts to master: Event-driven pipelines, fan-out/fan-in, workflow orchestration, bulk processing.

## Category H: AI/ML and computational background jobs

These are increasingly useful in modern system design interviews, especially for platforms with expensive model inference or data processing.

### 33. AI Document Analysis Platform

Problem: Design a system where users upload documents and receive AI-generated analysis.

Examples:

- Contract analysis.
- Procurement bid analysis.
- Resume screening.
- Financial document analysis.
- Compliance analysis.

Functional requirements:

- Upload documents.
- Extract text and tables.
- Chunk documents.
- Generate embeddings.
- Store vectors.
- Run AI analysis.
- Generate structured results.
- Notify users.

Important design challenges:

- Multi-stage asynchronous processing.
- Expensive model inference.
- Token limits.
- Model rate limits.
- Parallel document processing.
- Retry failed model calls.
- Tracking progress across stages.
- Reprocessing with different models.

Concepts to master: Workflow orchestration, queues, specialized workers, vector databases, rate limiting, fan-out/fan-in.

### 34. Distributed Machine Learning Inference Platform

Problem: Design a platform that accepts ML inference requests and processes them asynchronously.

Functional requirements:

- Accept inference requests.
- Batch requests.
- Route to available GPU workers.
- Track processing status.
- Return results asynchronously.
- Support multiple models.

Important design challenges:

- GPU resource scheduling.
- Dynamic batching.
- Model loading and unloading.
- Queue latency.
- Worker utilization.
- Model-specific routing.
- Priority scheduling.
- Resource isolation.

Concepts to master: Workload scheduling, batching, autoscaling, resource-aware queues, backpressure.

### 35. Video/Audio AI Processing Pipeline

Problem: Design a system that generates subtitles, summaries, translations, and searchable transcripts for uploaded media.

Functional requirements:

- Upload media.
- Extract audio.
- Transcribe speech.
- Translate transcripts.
- Generate summaries.
- Generate subtitles.
- Store searchable metadata.

Important design challenges:

- Multi-stage DAGs.
- Parallel processing.
- Model inference cost.
- External inference limits.
- Long-running jobs.
- Partial stage failures.
- Reprocessing.

Concepts to master: Workflow orchestration, task dependencies, parallel workers, checkpoints, model inference queues.

### 36. Content Moderation Pipeline

Problem: Design a moderation system that asynchronously analyzes user-uploaded images, videos, text, and audio.

Functional requirements:

- Accept content.
- Run automated moderation.
- Identify policy violations.
- Route uncertain cases for human review.
- Publish or block content.
- Notify users.

Important design challenges:

- High throughput.
- Low-latency moderation for content publication.
- Parallel analysis.
- Human review queues.
- Priority handling.
- Model failures.
- Audit history.

Concepts to master: Multi-stage workflows, priority queues, human-in-the-loop processing, event-driven architecture.

## Category I: Crawling, indexing, and large-scale processing

### 37. Web Crawler

Must solve

Problem: Design a distributed web crawler that discovers URLs and downloads web pages for indexing.

Functional requirements:

- Accept seed URLs.
- Maintain a URL frontier.
- Fetch pages.
- Extract links.
- Deduplicate URLs.
- Respect robots.txt and crawl policies.
- Store page content.
- Schedule recrawls.

Important design challenges:

- Distributed URL queues.
- URL deduplication.
- Domain-level rate limiting.
- Crawl prioritization.
- Retry policies.
- Politeness and robots.txt.
- Handling slow and failed websites.
- Avoiding duplicate crawling.

Concepts to master: Distributed queues, partitioning, deduplication, scheduling, rate limiting, distributed workers.

### 38. Search Engine Indexing and Reindexing Platform

Problem: Design a system that builds and maintains search indexes for billions of records.

This goes beyond the incremental indexing problem in #17.

Functional requirements:

- Initial full indexing.
- Incremental indexing.
- Bulk reindexing.
- Support multiple index versions.
- Track indexing progress.
- Switch to a new index without downtime.

Important design challenges:

- Massive backfills.
- Index versioning.
- Dual indexing.
- Atomic alias switching.
- CDC.
- Indexing lag.
- Recovery from partial failures.

Concepts to master: Distributed batch processing, CDC, blue-green indexing, event replay, checkpoints.

### 39. Log Processing and Aggregation System

Problem: Design a platform that ingests application logs and processes them asynchronously for analytics and search.

Functional requirements:

- Collect logs from multiple services.
- Buffer incoming logs.
- Parse and enrich records.
- Aggregate metrics.
- Store processed logs.
- Support querying and alerting.

Important design challenges:

- High ingestion throughput.
- Burst handling.
- Backpressure.
- Data partitioning.
- Retention policies.
- Late-arriving events.
- Exactly-once versus at-least-once processing.

Concepts to master: Kafka, stream processing, batching, partitioning, backpressure, consumer lag.

### 40. Notification Digest and Aggregation System

Problem: Design a system that aggregates multiple events into periodic notification digests.

Examples:

- Daily email summaries.
- Weekly activity reports.
- Social media notification digests.
- Daily sales summaries.

Functional requirements:

- Collect events.
- Group by user.
- Aggregate events.
- Schedule digest generation.
- Deliver notifications.
- Support user preferences.

Important design challenges:

- High-cardinality user aggregation.
- Time-zone boundaries.
- Deduplication.
- Scheduled fan-out.
- Large numbers of users.
- Delivery throttling.

Concepts to master: Stream aggregation, scheduled jobs, batching, fan-out, distributed state.

## Category J: Other important background processing problems

These are useful extensions. Some are more specialized, but they expose additional design trade-offs.

| #  | Problem                              | What the system does                                         | Key challenges                                             |
| -- | ------------------------------------ | ------------------------------------------------------------ | ---------------------------------------------------------- |
| 41 | Data Archival System                 | Moves old data to cheaper storage                            | Retention, batch migration, consistency                    |
| 42 | Cache Warming System                 | Preloads frequently accessed data into caches                | Prioritization, traffic spikes, invalidation               |
| 43 | Fraud Detection Pipeline             | Asynchronously analyzes transactions for suspicious activity | Low latency, parallel checks, false positives              |
| 44 | Inventory Reconciliation             | Compares inventory across warehouses and databases           | Consistency, batch processing, conflict resolution         |
| 45 | Price Calculation Engine             | Calculates prices and discounts for large product catalogs   | Parallelism, versioning, deterministic results             |
| 46 | Recommendation Generation            | Periodically generates recommendations for users             | Large-scale computation, model freshness                   |
| 47 | Data Retention and GDPR Deletion     | Deletes or anonymizes user data across services              | Distributed deletion, auditability, retries                |
| 48 | Video Upload Processing              | Validates uploads and generates metadata and assets          | Large files, multipart uploads, pipeline recovery          |
| 49 | Email Campaign System                | Sends marketing campaigns to millions of users               | Fan-out, batching, suppression, provider limits            |
| 50 | Scheduled Database Snapshot System   | Creates and stores periodic database snapshots               | Scheduling, consistency, storage, recovery                 |
| 51 | Geospatial Batch Processing          | Processes large volumes of location data                     | Spatial partitioning, computational workloads              |
| 52 | Large-Scale Notification Fan-out     | Delivers an event to millions of recipients                  | Hot partitions, batching, consumer scaling                 |
| 53 | Certificate Generation System        | Generates certificates for thousands of users                | Template rendering, batch processing, duplicate prevention |
| 54 | Data Quality and Validation Pipeline | Validates large datasets and produces quality reports        | Rule execution, parallelism, partial failures              |
| 55 | Background Fraud Investigation       | Collects evidence and creates investigation cases            | Multi-stage processing, manual review, SLA                 |

# 2. Which problems are truly different, and which are variations?

This distinction is very important.

You don't need to design 55 completely different systems. Many share the same underlying architecture but introduce different distributed systems challenges.

Here is how I would classify them.

| Architectural challenge   | Problems that test it                                      |
| ------------------------- | ---------------------------------------------------------- |
| Simple queue + worker     | Image resizing, PDF generation, email delivery             |
| Long-running jobs         | Video transcoding, AI document analysis, report generation |
| Multi-stage pipelines     | Video processing, document processing, ETL, AI analysis    |
| Fan-out / fan-in          | Video transcoding, notifications, media processing         |
| Massive batch processing  | Bulk import, export, database migration                    |
| External API delivery     | Email, SMS, webhooks, payment processing                   |
| Distributed scheduling    | Job scheduler, recurring billing, reminders                |
| Workflow orchestration    | Order fulfillment, insurance claims, KYC                   |
| Event-driven processing   | Search indexing, data synchronization, webhook delivery    |
| High-throughput streaming | Log processing, fraud detection, event aggregation         |
| Resource-aware scheduling | GPU inference, video transcoding                           |
| Replay and recovery       | Webhooks, search indexing, ETL, data migration             |

My recommendation: Master these architectural challenges rather than treating every application as an independent problem.

# 3. The core architecture you must master

Before solving the individual problems, become comfortable with this architecture.

\#chatgpt-mermaid-\_r_ei\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,"Segoe UI",Helvetica,"Apple Color Emoji",Arial,sans-serif,"Segoe UI Emoji","Segoe UI Symbol";font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_ei\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_ei\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_ei\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_ei\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_ei\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_ei\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_ei\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_ei\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_ei\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_ei\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_ei\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_ei\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_ei\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,"Segoe UI",Helvetica,"Apple Color Emoji",Arial,sans-serif,"Segoe UI Emoji","Segoe UI Symbol";font-size:16px;}#chatgpt-mermaid-\_r_ei\_ p{margin:0;}#chatgpt-mermaid-\_r_ei\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,"Segoe UI",Helvetica,"Apple Color Emoji",Arial,sans-serif,"Segoe UI Emoji","Segoe UI Symbol";color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_ei\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_ei\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_ei\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_ei\_ .label text,#chatgpt-mermaid-\_r_ei\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_ei\_ .node rect,#chatgpt-mermaid-\_r_ei\_ .node circle,#chatgpt-mermaid-\_r_ei\_ .node ellipse,#chatgpt-mermaid-\_r_ei\_ .node polygon,#chatgpt-mermaid-\_r_ei\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_ei\_ .rough-node .label text,#chatgpt-mermaid-\_r_ei\_ .node .label text,#chatgpt-mermaid-\_r_ei\_ .image-shape .label,#chatgpt-mermaid-\_r_ei\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_ei\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_ei\_ .rough-node .label,#chatgpt-mermaid-\_r_ei\_ .node .label,#chatgpt-mermaid-\_r_ei\_ .image-shape .label,#chatgpt-mermaid-\_r_ei\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_ei\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_ei\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_ei\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_ei\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_ei\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_ei\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_ei\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_ei\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_ei\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_ei\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_ei\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_ei\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_ei\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,"Segoe UI",Helvetica,"Apple Color Emoji",Arial,sans-serif,"Segoe UI Emoji","Segoe UI Symbol";font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_ei\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_ei\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_ei\_ .icon-shape,#chatgpt-mermaid-\_r_ei\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_ei\_ .icon-shape p,#chatgpt-mermaid-\_r_ei\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_ei\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_ei\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_ei\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_ei\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_ei\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_ei\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_ei\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_ei\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_ei\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_ei\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_ei\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_ei\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_ei\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_ei\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_ei\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_ei\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_ei\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_ei\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_ei\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_ei\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_ei\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_ei\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_ei\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_ei\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_ei\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_ei\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_ei\_ .node rect,#chatgpt-mermaid-\_r_ei\_ .node circle,#chatgpt-mermaid-\_r_ei\_ .node ellipse,#chatgpt-mermaid-\_r_ei\_ .node polygon,#chatgpt-mermaid-\_r_ei\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_ei\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_ei\_ .mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2,2;}#chatgpt-mermaid-\_r_ei\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_ei\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_ei\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,"Segoe UI",Helvetica,"Apple Color Emoji",Arial,sans-serif,"Segoe UI Emoji","Segoe UI Symbol";}ClientAPI Gateway / Application APIJob DatabaseTransactional OutboxMessage QueueWorker Pool 1Worker Pool 2Worker Pool 3Object StorageJob Status APINotification ServiceDead Letter Queue

Understand each component and, more importantly, why it exists.

## A. Job submission and persistence

Questions to answer:

- How does the API accept a job?
- Do we persist the job before publishing the message?
- How do we avoid losing jobs if the API crashes?
- How do we handle duplicate requests?
- How do we generate job IDs?
- How do we validate requests?

Important concepts:

- Job table.
- Transactional outbox.
- Idempotency keys.
- Job metadata.
- Job status.

## B. Queue and message broker

Questions to answer:

- Kafka vs RabbitMQ vs SQS?
- FIFO vs standard queues?
- At-most-once vs at-least-once delivery?
- Message visibility timeout?
- Delayed messages?
- Priority queues?
- Queue partitioning?
- Message retention?
- Dead-letter queues?

Understand when to use each technology rather than memorizing a particular choice.

## C. Worker architecture

Questions to answer:

- How do workers claim jobs?
- How many jobs can a worker process concurrently?
- How do we scale workers?
- What happens if a worker crashes?
- How do we prevent duplicate processing?
- How do we handle different workloads?

Important concepts:

- Competing consumers.
- Worker pools.
- Horizontal autoscaling.
- Worker heartbeats.
- Leases.
- Visibility timeout.
- Graceful shutdown.
- Resource isolation.

## D. Job lifecycle and state management

A typical job lifecycle:

\#chatgpt-mermaid-\_r_ep\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,"Segoe UI",Helvetica,"Apple Color Emoji",Arial,sans-serif,"Segoe UI Emoji","Segoe UI Symbol";font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_ep\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_ep\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_ep\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_ep\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_ep\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_ep\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_ep\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_ep\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_ep\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_ep\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_ep\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_ep\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_ep\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,"Segoe UI",Helvetica,"Apple Color Emoji",Arial,sans-serif,"Segoe UI Emoji","Segoe UI Symbol";font-size:16px;}#chatgpt-mermaid-\_r_ep\_ p{margin:0;}#chatgpt-mermaid-\_r_ep\_ defs [id$="-barbEnd"]{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_ep\_ g.stateGroup text{fill:rgb(31, 78, 148);stroke:none;font-size:10px;}#chatgpt-mermaid-\_r_ep\_ g.stateGroup text{fill:rgb(237, 237, 237);stroke:none;font-size:10px;}#chatgpt-mermaid-\_r_ep\_ g.stateGroup .state-title{font-weight:bolder;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_ep\_ g.stateGroup rect{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_ep\_ g.stateGroup line{stroke:rgb(175, 175, 175);stroke-width:1;}#chatgpt-mermaid-\_r_ep\_ .transition{stroke:rgb(175, 175, 175);stroke-width:1;fill:none;}#chatgpt-mermaid-\_r_ep\_ .stateGroup .composit{fill:transparent;border-bottom:1px;}#chatgpt-mermaid-\_r_ep\_ .stateGroup .alt-composit{fill:#e0e0e0;border-bottom:1px;}#chatgpt-mermaid-\_r_ep\_ .state-note{stroke:rgb(58, 132, 63);fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_ep\_ .state-note text{fill:rgb(237, 237, 237);stroke:none;font-size:10px;}#chatgpt-mermaid-\_r_ep\_ .stateLabel .box{stroke:none;stroke-width:0;fill:rgb(9, 23, 44);opacity:0.5;}#chatgpt-mermaid-\_r_ep\_ .edgeLabel .label rect{fill:rgb(9, 23, 44);opacity:0.5;}#chatgpt-mermaid-\_r_ep\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_ep\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_ep\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_ep\_ .edgeLabel .label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_ep\_ .label div .edgeLabel{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_ep\_ .stateLabel text{fill:rgb(237, 237, 237);font-size:10px;font-weight:bold;}#chatgpt-mermaid-\_r_ep\_ .node circle.state-start{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_ep\_ .node .fork-join{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_ep\_ .node circle.state-end{fill:rgb(31, 78, 148);stroke:transparent;stroke-width:1.5;}#chatgpt-mermaid-\_r_ep\_ .end-state-inner{fill:transparent;stroke-width:1.5;}#chatgpt-mermaid-\_r_ep\_ .node rect{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_ep\_ .node polygon{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_ep\_ [id$="-barbEnd"]{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_ep\_ .statediagram-cluster rect{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_ep\_ .cluster-label,#chatgpt-mermaid-\_r_ep\_ .nodeLabel{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_ep\_ .statediagram-cluster rect.outer{rx:5px;ry:5px;}#chatgpt-mermaid-\_r_ep\_ .statediagram-state .divider{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_ep\_ .statediagram-state .title-state{rx:5px;ry:5px;}#chatgpt-mermaid-\_r_ep\_ .statediagram-cluster.statediagram-cluster .inner{fill:transparent;}#chatgpt-mermaid-\_r_ep\_ .statediagram-cluster.statediagram-cluster-alt .inner{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_ep\_ .statediagram-cluster .inner{rx:0;ry:0;}#chatgpt-mermaid-\_r_ep\_ .statediagram-state rect.basic{rx:5px;ry:5px;}#chatgpt-mermaid-\_r_ep\_ .statediagram-state rect.divider{stroke-dasharray:10,10;fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_ep\_ .note-edge{stroke-dasharray:5;}#chatgpt-mermaid-\_r_ep\_ .statediagram-note rect{fill:rgb(48, 48, 48);stroke:rgb(58, 132, 63);stroke-width:1px;rx:0;ry:0;}#chatgpt-mermaid-\_r_ep\_ .statediagram-note rect{fill:rgb(48, 48, 48);stroke:rgb(58, 132, 63);stroke-width:1px;rx:0;ry:0;}#chatgpt-mermaid-\_r_ep\_ .statediagram-note text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_ep\_ .statediagram-note .nodeLabel{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_ep\_ .statediagram .edgeLabel{color:red;}#chatgpt-mermaid-\_r_ep\_ [id$="-dependencyStart"],#chatgpt-mermaid-\_r_ep\_ [id$="-dependencyEnd"]{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);stroke-width:1;}#chatgpt-mermaid-\_r_ep\_ .statediagramTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_ep\_ [data-look="neo"].statediagram-cluster rect{fill:rgb(9, 23, 44);stroke:url(#chatgpt-mermaid-\_r_ep\_-gradient);stroke-width:1;}#chatgpt-mermaid-\_r_ep\_ [data-look="neo"].statediagram-cluster rect.outer{rx:5px;ry:5px;filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_ep\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_ep\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_ep\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_ep\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_ep\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_ep\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_ep\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_ep\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_ep\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_ep\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_ep\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_ep\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_ep\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_ep\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_ep\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_ep\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_ep\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_ep\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,"Segoe UI",Helvetica,"Apple Color Emoji",Arial,sans-serif,"Segoe UI Emoji","Segoe UI Symbol";}PENDINGQUEUEDRUNNINGCOMPLETEDRETRY_WAITFAILEDCANCELLED

Understand:

- Job state transitions.
- Progress tracking.
- Retry counters.
- Execution history.
- Cancellation.
- Timeouts.
- Stuck job detection.
- Job expiration.

## E. Failure handling and reliability

This is where many interview candidates struggle.

You should understand:

1. Worker crashes after receiving a message.
2. Worker crashes after completing work but before acknowledging the message.
3. Queue publishes duplicate messages.
4. Database is temporarily unavailable.
5. External provider times out.
6. Job continuously fails.
7. A poison message blocks processing.
8. A downstream service becomes unavailable.
9. A queue accumulates millions of messages.
10. A deployment terminates workers during processing.

Important concepts:

- At-least-once delivery.
- Idempotent consumers.
- Exponential backoff.
- Dead-letter queues.
- Retry limits.
- Circuit breakers.
- Transactional outbox.
- Inbox/deduplication pattern.
- Reconciliation jobs.

Remember: a message broker generally does not guarantee that your business operation executes exactly once. Design business operations to be idempotent.

## F. Scaling and performance

Understand:

- Queue depth-based autoscaling.
- Consumer lag.
- Throughput estimation.
- Batch size optimization.
- Horizontal scaling.
- Partitioning.
- Backpressure.
- Rate limiting.
- Priority scheduling.
- Noisy-neighbor isolation.

## G. Monitoring and observability

Understand how to monitor:

- Queue depth.
- Oldest message age.
- Job processing latency.
- Job success/failure rates.
- Retry rates.
- Worker utilization.
- Dead-letter queue size.
- Processing throughput.
- End-to-end completion time.

A particularly important metric is queue age, not just queue depth. A large queue of recent messages may be acceptable, whereas a small queue containing very old jobs may indicate a serious problem.

# 4. My recommended problem-solving order

If your goal is to become strong at HLD interviews, I would not solve these problems in their numerical order.

I recommend the following sequence. It progresses from simple background execution to distributed workflow orchestration.

Recommended preparation roadmap

| Order | Problem                            | Difficulty  | Why solve it?                                   |
| ----- | ---------------------------------- | ----------- | ----------------------------------------------- |
| 1     | Image Processing Service           | Medium      | Fundamental queue-worker architecture           |
| 2     | Email Delivery System              | Medium      | Retries, provider failures, rate limits         |
| 3     | Report Generation System           | Medium      | Long-running jobs, large datasets               |
| 4     | Bulk Import System                 | Medium–Hard | Batching, checkpoints, partial failures         |
| 5     | Video Transcoding Platform         | Hard        | Resource-intensive distributed pipelines        |
| 6     | Webhook Delivery Platform          | Hard        | Delivery guarantees, ordering, retries          |
| 7     | Distributed Job Scheduler          | Hard        | Delayed jobs, recurring schedules, coordination |
| 8     | Search Indexing System             | Hard        | CDC, event ordering, eventual consistency       |
| 9     | Bulk Data Export System            | Medium–Hard | Streaming, snapshots, database protection       |
| 10    | Multi-Channel Notification System  | Hard        | Multi-stage workflows and fallback              |
| 11    | ETL Processing Pipeline            | Hard        | DAGs, parallel processing, checkpoints          |
| 12    | Order Fulfillment System           | Hard        | Saga, distributed transactions                  |
| 13    | Payment Reconciliation System      | Hard        | Financial consistency and recovery              |
| 14    | Data Migration and Backfill System | Hard        | Large-scale recovery and consistency            |
| 15    | AI Document Analysis Platform      | Hard        | Modern AI pipelines and expensive inference     |
| 16    | Web Crawler                        | Hard        | Distributed queues, deduplication, scheduling   |
| 17    | Distributed ML Inference Platform  | Hard        | GPU scheduling and dynamic batching             |
| 18    | Log Processing System              | Hard        | High-throughput streaming and backpressure      |

These 18 provide a strong foundation across the major challenges.

# 5. Which problems are particularly important for Google, Meta, Amazon and similar interviews?

I would prioritize them as follows.

| Problem                    | Interview relevance | Main reason                                    |
| -------------------------- | ------------------- | ---------------------------------------------- |
| Distributed Job Scheduler  | Very high           | Distributed coordination and scheduling        |
| Webhook Delivery           | Very high           | Reliability and delivery guarantees            |
| Email/Notification Service | Very high           | Queue architecture and scale                   |
| Video Processing           | Very high           | Distributed processing and resource management |
| Bulk Import/Export         | Very high           | Large-scale data processing                    |
| Search Indexing            | Very high           | Eventual consistency and event processing      |
| Report Generation          | High                | Backend architecture and database scalability  |
| ETL Pipeline               | High                | Data processing and orchestration              |
| Order Fulfillment          | Very high           | Distributed transactions and Saga              |
| Payment Processing         | Very high           | Reliability and correctness                    |
| Web Crawler                | Very high           | Distributed architecture and scheduling        |
| AI Document Processing     | High                | Modern workload orchestration                  |

These are my preparation priorities, not a claim that every company regularly asks these exact questions. Interview frequency varies by team and level.

# 6. Advanced concepts that separate senior candidates

Once you complete the foundational problems, make sure you can explain these topics in depth.

| Concept                | What you should be able to explain                                           |
| ---------------------- | ---------------------------------------------------------------------------- |
| Transactional Outbox   | How to atomically persist business changes and events                        |
| Inbox Pattern          | How to deduplicate incoming messages                                         |
| Idempotency            | How to prevent duplicate business effects                                    |
| At-least-once Delivery | Why duplicate processing is possible                                         |
| Exactly-once Semantics | Difference between broker guarantees and business-level exactly-once effects |
| Delayed Queues         | How to schedule future execution                                             |
| Priority Queues        | How urgent jobs overtake normal workloads                                    |
| Fan-out/Fan-in         | How to split work and combine results                                        |
| Workflow Orchestration | How to coordinate dependent tasks                                            |
| Saga Pattern           | How to handle distributed business transactions                              |
| Checkpointing          | How to resume partially completed jobs                                       |
| Backpressure           | How to protect overloaded downstream services                                |
| Dead Letter Queue      | How to isolate permanently failing jobs                                      |
| Retry with Backoff     | How to prevent retry storms                                                  |
| Distributed Leases     | How to manage worker ownership                                               |
| Job Cancellation       | How to safely stop running work                                              |
| Job Deduplication      | How to avoid scheduling the same logical work repeatedly                     |
| Queue Partitioning     | How to scale processing horizontally                                         |
| Reconciliation         | How to detect and repair missing or inconsistent work                        |
| Multi-tenancy          | How to prevent one customer from monopolizing workers                        |

# 7. A reusable HLD interview checklist

For every problem in this pattern, make sure you cover these areas.

| Area             | Questions you must answer                               |
| ---------------- | ------------------------------------------------------- |
| Requirements     | What is synchronous and what is asynchronous?           |
| APIs             | How are jobs submitted, queried, cancelled and retried? |
| Data model       | How are jobs, attempts, results and events stored?      |
| Job submission   | How do we guarantee that accepted jobs are not lost?    |
| Queue            | Which broker and delivery semantics do we need?         |
| Workers          | How are jobs claimed and executed?                      |
| State management | How is progress and job status maintained?              |
| Failure handling | What happens after crashes and timeouts?                |
| Idempotency      | How do we prevent duplicate side effects?               |
| Scaling          | How do we scale producers and consumers independently?  |
| Backpressure     | How do we handle bursts and downstream overload?        |
| Ordering         | Does processing require FIFO or per-entity ordering?    |
| Scheduling       | Are delayed or recurring jobs required?                 |
| Monitoring       | How do we detect stuck jobs and processing lag?         |
| Security         | How do we isolate tenants and protect uploaded data?    |

# 8. Final recommendation: What I would actually solve

Don't try to solve all 55 problems independently. That would lead to repetition without necessarily improving your system design skills.

I would divide preparation into three levels:

Level 1 — Core architecture (8 problems)

1. Image Processing
2. Email Delivery
3. Report Generation
4. Bulk Import
5. Video Transcoding
6. Webhook Delivery
7. Distributed Job Scheduler
8. Search Indexing

Level 2 — Advanced distributed processing (6 problems)

9. Bulk Export
10. Multi-Channel Notifications
11. ETL Pipeline
12. Order Fulfillment
13. Payment Reconciliation
14. Database Migration and Backfill

Level 3 — Specialized systems (4 problems)

15. AI Document Processing
16. Web Crawler
17. Distributed ML Inference
18. Log Processing



18 problems

&#x20;A strong preparation set covering the major architectural variations.



My most important advice: For each problem, don't just draw `API → Queue → Worker → Database`. That is the easy part.

Your real interview preparation should focus on answering:

- What happens when the worker crashes halfway through?
- What if the same job is delivered five times?
- What if the queue grows faster than workers can process it?
- What if an external provider is down for two hours?
- What if a job depends on five other jobs?
- What if the user cancels a running job?
- What if millions of jobs become eligible at the same time?
- How do we recover jobs that were accepted but never completed?

If you can confidently answer those questions across the 18 problems, you will have a solid command of asynchronous processing and background job architectures for senior backend HLD interviews.

