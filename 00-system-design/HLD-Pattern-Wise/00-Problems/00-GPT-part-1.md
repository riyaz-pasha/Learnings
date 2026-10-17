# HLD System Design Interview Master Guide

## Reusable patterns, problem families, trade-offs, and interview playbooks

> **The goal is not to memorize 100 system designs.** Learn a compact
> set of reusable patterns, understand the invariant and workload behind
> each one, and practice combining patterns. A new interview problem is
> usually a familiar pattern with a different constraint.

------------------------------------------------------------------------

## Table of contents

1.  [How to use this guide](#1-how-to-use-this-guide)
2.  [The interview framework](#2-the-interview-framework)
3.  [Master classification: 15
    patterns](#3-master-classification-15-patterns)
4.  [The 15 patterns in detail](#4-the-15-patterns-in-detail)
5.  [Reusable sub-patterns](#5-reusable-sub-patterns)
6.  [Cross-cutting trade-off matrix](#6-cross-cutting-trade-off-matrix)
7.  [Problem-to-pattern mapping](#7-problem-to-pattern-mapping)
8.  [Core components to master](#8-core-components-to-master)
9.  [Capacity estimation cheat
    sheet](#9-capacity-estimation-cheat-sheet)
10. [Four-week preparation plan](#10-four-week-preparation-plan)
11. [Common interview mistakes](#11-common-interview-mistakes)
12. [One-page revision sheet](#12-one-page-revision-sheet)

------------------------------------------------------------------------

# 1. How to use this guide

For each pattern, learn five things:

1.  **Core challenge:** What makes this system difficult?
2.  **Invariant:** What must never be violated?
3.  **Three key design moves:** What usually makes the architecture
    work?
4.  **Trade-offs:** What do the design choices improve, and what do they
    cost?
5.  **Failure scenarios:** What happens when requests time out, messages
    repeat, or a dependency fails?

### Suggested priority

-   **First:** Patterns 1--5: resource allocation, read-heavy systems,
    write-heavy systems, async jobs, transactional workflows.
-   **Next:** Patterns 6--10: search, real-time communication, event
    distribution, media, geospatial.
-   **Then:** Patterns 11--15: coordination/rate limiting, scheduling,
    multi-tenancy, analytics, highly available lookup.

Technologies such as PostgreSQL, Redis, Kafka, DynamoDB, Elasticsearch,
and object storage are choices made **after** the workload and
correctness requirements are understood.

### The central principle

> Identify the invariant, classify the workload, choose the simplest
> architecture that preserves correctness, and justify every additional
> component with a requirement.

A system can be read-heavy and still contain a correctness-critical
write path. A ride-hailing platform combines geospatial discovery,
high-volume location ingestion, real-time messaging, and scarce-resource
allocation. Most real designs are combinations of patterns rather than a
single pattern.

------------------------------------------------------------------------

# 2. The interview framework

## A repeatable 45--60 minute structure

  ------------------------------------------------------------------------
  Step                                  Approx. time What to do
  --------------------- ---------------------------- ---------------------
  Requirements                              3--5 min Functional scope,
                                                     non-functional goals,
                                                     constraints, out of
                                                     scope

  Capacity estimates                        3--5 min Average/peak QPS,
                                                     read/write ratio,
                                                     storage, bandwidth,
                                                     concurrency

  API and data model                       5--10 min Main endpoints,
                                                     entities, keys,
                                                     indexes, constraints,
                                                     state transitions

  High-level design                        8--10 min Services, database,
                                                     cache, queue, object
                                                     storage, CDN where
                                                     relevant

  Deep dive                               10--15 min The hardest part:
                                                     consistency, fan-out,
                                                     partitioning, search,
                                                     or media

  Failure and recovery                         5 min Timeouts, retries,
                                                     duplicates, partial
                                                     failures, stale
                                                     cache, failover

  Trade-offs and                            3--5 min Bottlenecks,
  evolution                                          alternatives, what to
                                                     defer and when to add
                                                     it
  ------------------------------------------------------------------------

The exact timing is flexible. If the interviewer spends longer on one
area, adapt rather than mechanically following the schedule.

## Eight questions to ask before designing

1.  **What must never go wrong?** Double booking, lost payment,
    unauthorized data access, or missed event?
2.  **What is the workload?** Read-heavy, write-heavy, bursty,
    streaming, or mixed?
3.  **What is the latency target?** Must the result be immediate, or can
    the API return a job ID?
4.  **How fresh must data be?** Is bounded staleness acceptable? How
    much?
5.  **What is the failure contract?** Can work be retried? Can users see
    `PENDING`? Can derived data be rebuilt?
6.  **What is the scale?** Peak RPS, object size, retention, concurrent
    users, hot keys, and growth?
7.  **What ordering is required?** Global, per account, per order, per
    conversation, or none?
8.  **What are the operational constraints?** Availability, cost,
    RPO/RTO, regions, privacy, security, and team size?

### Say the trade-off out loud

Do not only say, "I will use Redis." Say: "I would add Redis because
repeated reads are expensive and a short period of staleness is
acceptable. It adds invalidation and cache-outage handling, so the
database remains authoritative for booking decisions."

Interviewers are evaluating the reasoning behind the choice, not the
number of boxes in the diagram.

------------------------------------------------------------------------

# 3. Master classification: 15 patterns

  ------------------------------------------------------------------------
                     \# Pattern          Typical workload Example problems
                                         / challenge      
  --------------------- ---------------- ---------------- ----------------
                      1 Scarce resource  Contention and   Movie tickets,
                        allocation       double booking   hotels, flights,
                                                          appointments,
                                                          rides, flash
                                                          sales

                      2 Read-heavy       Low-latency      Product catalog,
                        serving          repeated reads   feeds, profiles,
                                                          URL redirects,
                                                          configuration

                      3 Write-heavy      High write rate  Logs, metrics,
                        ingestion        and bursts       clickstream,
                                                          IoT, GPS updates

                      4 Async background Long-running or  Reports,
                        processing       retryable work   transcoding,
                                                          exports, emails,
                                                          imports

                      5 Transactional    Business         Payments,
                        workflows        invariants       checkout,
                                         across steps     wallet,
                                                          subscriptions,
                                                          refunds

                      6 Search and       Relevance,       E-commerce
                        discovery        indexing,        search, jobs,
                                         filters          autocomplete,
                                                          nearby
                                                          candidates

                      7 Real-time        Low-latency      Chat, presence,
                        communication    delivery and     live comments,
                                         long-lived       collaboration
                                         connections      

                      8 Event-driven     Reliable fan-out Notifications,
                        distribution     to consumers     webhooks, CDC,
                                                          activity streams

                      9 File and media   Large objects    Drive/Dropbox,
                        delivery         and bandwidth    photos, video
                                                          streaming,
                                                          recordings

                     10 Geospatial       Frequently       Ride hailing,
                        systems          changing spatial delivery, fleet
                                         data             tracking, nearby
                                                          places

                     11 Coordination and Shared control   Rate limiter, ID
                        rate limiting    primitives       generator,
                                                          leases, leader
                                                          election

                     12 Distributed      Durable          Cron, reminders,
                        scheduling       execution at a   delayed jobs,
                                         given time       recurring
                                                          billing

                     13 Multi-tenant     Isolation and    CRM, project
                        SaaS             fair sharing     tools,
                                                          enterprise
                                                          analytics

                     14 Analytics and    High-volume      Top-K,
                        aggregation      aggregation      dashboards,
                                                          counts, fraud
                                                          analytics

                     15 Highly available Fast reads and   URL redirect,
                        lookup services  safe propagation feature flags,
                                                          configuration,
                                                          DNS-like lookup
  ------------------------------------------------------------------------

**These categories overlap intentionally.** URL shortener creation and
redirects have different workloads. Ride allocation is both geospatial
discovery and resource allocation. Checkout combines transactions,
scarce inventory, async events, and payment workflows.

------------------------------------------------------------------------

# 4. The 15 patterns in detail

## Pattern 1 --- Scarce resource allocation and double booking

**Problems:** Movie ticket booking, hotel rooms, flights, doctor
appointments, restaurant tables, meeting rooms, parking, ride
assignment, limited-stock flash sales.

**Core challenge:** Multiple requests compete for a limited resource.
Only a valid allocation may succeed.

### Typical architecture

``` text
Client -> API Gateway -> Booking Service
                            |
                   DB transaction + concurrency control
                            |
              +-------------+-------------+
              |                           |
       Expiration worker            Async events
       (release holds)              (email, analytics)
```

### Key design moves

1.  **Make the authoritative allocation atomic.** Use a conditional
    update, unique constraint, row lock, or optimistic version check.
2.  **Model holds explicitly.** For example,
    `AVAILABLE -> HELD -> CONFIRMED`, with `expires_at` and a
    recoverable expiry process.
3.  **Make retries safe.** Use an idempotency key for the logical
    booking operation and enforce relevant uniqueness in the database.
4.  **Keep cache advisory.** Cache availability for display, but
    revalidate against the authoritative store when reserving.
5.  **Control overload.** Flash sales may need a virtual waiting room,
    admission control, or per-inventory queues.

Example conditional update:

``` sql
UPDATE seats
SET status = 'HELD',
    hold_expires_at = :expires_at,
    reservation_id = :reservation_id
WHERE seat_id = :seat_id
  AND status = 'AVAILABLE';
```

Check the affected row count. One updated row means this request
acquired the seat; zero means it did not. The transaction must commit
successfully before success is reported.

A separate uniqueness constraint should protect the final business
invariant where applicable. Application-side "check then update" is
unsafe because concurrent requests can both observe availability.

### Important variations

-   **Movie seat:** individual seat and showtime.
-   **Hotel:** inventory across date ranges. A common model is room-type
    inventory per date; another uses range constraints to prevent
    overlapping reservations.
-   **Flash sale:** extreme contention; queue/admission control can
    protect the database.
-   **Ride allocation:** nearby-driver discovery is separate from
    assignment. Offers can expire; the transition to assigned must be
    atomic.

### Trade-offs

  -----------------------------------------------------------------------
  Decision          Default direction Why               Cost / risk
  ----------------- ----------------- ----------------- -----------------
  SQL vs cache as   SQL or another    Constraints and   Contention and
  authority         transactional     transactions      write limits
                    authority         protect           
                                      invariants        

  Optimistic vs     Optimistic at low Matches expected  Retries vs lock
  pessimistic       conflict;         conflict rate     waits
  locking           pessimistic when                    
                    contention                          
                    warrants it                         

  Sync vs async     Allocation        User needs a      Downstream views
                    synchronously;    definitive        update later
                    side effects      allocation result 
                    asynchronously                      

  Hold expiry       Persist expiry    Expiry must       Worker and
                    time and          survive restarts  state-machine
                    recoverable                         complexity
                    cleanup                             

  Partitioning      By show,          Localizes         Hot partitions
                    property,         contention        for popular
                    inventory scope,                    inventory
                    or similar key                      

  Redis lock vs DB  Prefer DB         A lock is not a   Cross-system
  constraint        enforcement when  replacement for a cases need more
                    the invariant     constraint        care
                    lives in that DB                    
  -----------------------------------------------------------------------

### Failure scenarios and interviewer probes

-   Two users book the same seat simultaneously.
-   A hold expires while payment is in progress.
-   The payment provider succeeds but the booking response times out.
-   The expiry worker runs twice.
-   A blockbuster show receives a sudden traffic spike.
-   The cache says a seat is available after it has been sold.

**Say this:** "The database enforces the allocation invariant. The cache
is for display, holds have durable state and expiry, and downstream
notifications are asynchronous and idempotent."

------------------------------------------------------------------------

## Pattern 2 --- Read-heavy, read-optimized systems

**Problems:** Product catalogs, profiles, news websites, timelines, URL
redirects, reference data, feature flags, leaderboards.

**Core challenge:** Serve repeated reads cheaply and quickly while
defining acceptable staleness.

### Typical architecture

``` text
Client -> CDN / edge cache -> API -> Redis / local cache
                                      |
                               Read replicas / DB
```

Use only layers justified by the workload. A CDN is not automatically
useful for personalized or private responses.

### Key design moves

-   Optimize database queries and indexes before adding layers.
-   Use cache-aside as a common starting point.
-   Add read replicas when read capacity requires them and replica lag
    is acceptable.
-   Precompute expensive results when reuse is high.
-   Use cursor pagination for large, changing collections.
-   Plan for cache stampedes, hot keys, cache misses, and cache outage
    behavior.

### Cache failure modes

-   **Stampede:** many requests miss at once. Consider request
    coalescing, TTL jitter, or controlled refresh.
-   **Penetration:** repeated requests for nonexistent keys. Consider
    short negative caching where safe.
-   **Hot key:** one key dominates traffic. Consider local caching,
    replication, or request shaping.
-   **Stale data:** invalidation and update ordering matter. Define a
    freshness contract.

### Trade-offs

  -----------------------------------------------------------------------
  Decision                Prefer when             Cost / risk
  ----------------------- ----------------------- -----------------------
  CDN                     Content is cacheable    Invalidation, privacy,
                          and broadly reused      and cache-key mistakes

  Redis                   Repeated reads justify  Staleness, eviction,
                          memory and operations   invalidation

  Read replicas           Read traffic exceeds    Replication lag;
                          primary capacity        read-your-write
                                                  behavior

  Precompute              Results are expensive   Extra storage and
                          and repeatedly          update work
                          requested               

  Denormalize             Read latency dominates  Write amplification and
                                                  consistency work

  Eventual consistency    Bounded staleness is    Users may briefly see
                          acceptable              old data

  Cursor vs offset        Large collections and   More complex cursor
  pagination              stable traversal        semantics
  -----------------------------------------------------------------------

### News feed: fan-out choices

  -----------------------------------------------------------------------
  Strategy          How it works      Best fit          Main cost
  ----------------- ----------------- ----------------- -----------------
  Fan-out on write  Add a post        Read-heavy feeds  Huge write
                    reference to      with manageable   amplification for
                    followers' feed   follower counts   celebrity
                    stores when                         accounts
                    published                           

  Fan-out on read   Fetch and merge   Lower write       More work and
                    followed accounts amplification or  latency per read
                    when the user     users with few    
                    opens the feed    follows           

  Hybrid            Precompute normal Large social      More complex
                    accounts; merge   networks          ranking and
                    very                                freshness
                    high-follower                       
                    accounts at read                    
                    time                                
  -----------------------------------------------------------------------

**URL shortener note:** Redirects are read-heavy; creation is a separate
write path. Key generation, collision handling, expiry, hot-key
mitigation, and redirect semantics (such as 301 vs 302) may matter.

**Interviewer probes:** What happens when Redis is down? How do you
invalidate? How stale can a replica be? How do you handle a celebrity
post?

------------------------------------------------------------------------

## Pattern 3 --- Write-heavy ingestion and high-volume events

**Problems:** Logs, metrics, clickstream, IoT telemetry, GPS updates,
audit events, ad impressions, market data.

**Core challenge:** Absorb bursts, distribute writes, and define which
events must be durable before acknowledgment.

### Typical architecture

``` text
Producers -> Ingestion API (auth, validation, quotas)
          -> Durable log / queue
                 |-> Raw object storage / data lake
                 |-> Stream processors (alerts, aggregates)
                 |-> Serving store (time-series / key-value / columnar)
```

### Key design moves

-   Buffer bursts with a durable log when downstream systems cannot
    absorb them directly.
-   Partition by a key that supports parallelism and the required
    ordering.
-   Batch writes and compress payloads where useful.
-   Prefer append-oriented data models for event streams.
-   Separate hot, warm, and cold retention tiers.
-   Aggregate or sample only when product accuracy requirements allow
    it.
-   Monitor consumer lag and apply backpressure, admission control, or
    load shedding.

### Trade-offs

  -----------------------------------------------------------------------
  Decision                Benefit                 Cost / risk
  ----------------------- ----------------------- -----------------------
  Queue/log vs direct DB  Burst absorption,       Extra component and
  write                   decoupling, replay      processing delay

  Acknowledge after       Fast acceptance without Accepted does not mean
  durable enqueue         waiting for every       fully processed
                          consumer                

  Batching                Better throughput       More latency and larger
                                                  retry units

  Partitioning            Parallelism and scale   Hot partitions and
                                                  ordering limits

  Exact vs approximate    Lower compute/memory    Inaccuracy and possible
  counters                                        reconciliation

  Retention / tiering     Cost control            Historical queries can
                                                  become slower

  Columnar compression    Efficient analytical    CPU and processing
                          scans and storage       complexity
  -----------------------------------------------------------------------

### Questions to answer

1.  Can ingestion absorb a 10× spike?
2.  What if consumers are slower than producers?
3.  Can events be delivered more than once?
4.  Is ordering needed, and at what scope?
5.  What is the durability/acknowledgment contract?
6.  Can you replay events and rebuild derived aggregates?
7.  What is the hot-partition mitigation plan?

A queue does not automatically make a system reliable. Reliability
depends on producer behavior, broker durability and replication,
acknowledgment settings, consumer idempotency, and recovery procedures.

------------------------------------------------------------------------

## Pattern 4 --- Asynchronous processing and background jobs

**Problems:** Reports, exports, video transcoding, image resizing, bulk
imports, email/SMS, document generation, indexing, webhooks.

**Core challenge:** Work is long-running, expensive, retryable, or not
required to finish before the API response.

### Typical architecture

``` text
Client -> API -> persist job state + durable work intent
                         |
                       Queue
                         |
                       Workers -> Output storage
                         |
               Status API / completion notification
```

### Key design moves

-   Return `202 Accepted` and a job ID when work is accepted but not
    complete.
-   Persist job state such as `PENDING`, `RUNNING`, `SUCCEEDED`,
    `RETRY_WAIT`, `FAILED`, and `CANCELLED`.
-   Use at-least-once delivery with idempotent workers as a common
    reliability model.
-   Retry with exponential backoff and jitter, with bounded attempts;
    send poison messages to a DLQ.
-   Use visibility timeouts or leases so crashed workers do not hold
    jobs forever.
-   Split large jobs into bounded, resumable chunks.
-   Isolate CPU-heavy and latency-sensitive worker pools.

### The database-plus-queue dual-write problem

Two naive approaches can fail:

-   **DB then publish:** the DB commits, but publishing fails. The
    business record exists without the event.
-   **Publish then DB:** the event is delivered, but the DB transaction
    fails. Consumers process a nonexistent record.

**Transactional outbox:**

1.  In one DB transaction, write the business record and an outbox row.
2.  A relay publishes committed outbox rows.
3.  Record publication progress safely and retry failures.
4.  Make consumers idempotent because delivery can repeat.

This avoids requiring one distributed transaction between the database
and broker. It does not mean the entire system has magical exactly-once
effects.

### Trade-offs

  -----------------------------------------------------------------------
  Decision                Recommended starting    Cost
                          point                   
  ----------------------- ----------------------- -----------------------
  Sync vs async           Async for slow or       Pending states and more
                          nonessential downstream components
                          work                    

  At-most-once vs         At-least-once +         Duplicates must be safe
  at-least-once           idempotency for         
                          important work          

  Inline retries vs DLQ   Bounded retries then    DLQ needs operational
                          DLQ                     ownership

  Poll vs push for status Polling for simplicity; Polling load vs
                          push for timely         connection/delivery
                          completion              complexity

  One big job vs chunks   Chunk large jobs        Coordination and
                                                  aggregation complexity
  -----------------------------------------------------------------------

**Interviewer probes:** Worker crashes after side effect but before
acknowledgment; poison message; queue backlog; duplicate job;
cancellation during execution; output written but status not updated.

------------------------------------------------------------------------

## Pattern 5 --- Transactional workflows and financial correctness

**Problems:** Payments, checkout, wallets, bank transfers,
subscriptions, refunds, chargebacks, loyalty points, multi-service
fulfillment.

**Core challenge:** Maintain business invariants across multiple state
changes, potentially across services that do not share one database
transaction.

### Typical architecture

``` text
Order Service -> Workflow / Saga
                    |-> Inventory: reserve
                    |-> Payment: authorize/capture
                    |-> Shipping: create shipment
                    |-> Notification: confirm

Each service owns local transactions; durable events and workflow state connect them.
```

### Key design moves

-   Use a single ACID transaction when all required changes fit within
    one database boundary.
-   Use a **Saga** for multi-service workflows, with explicit states and
    compensating actions.
-   Choose orchestration for centralized visibility or choreography when
    independent event reactions are a better fit.
-   Use idempotency keys at API and consumer boundaries.
-   Model uncertain states such as `PENDING` or `UNKNOWN`.
-   Use provider webhooks and reconciliation to resolve ambiguous
    outcomes.
-   Use an immutable ledger for financial history; a double-entry model
    records matching debits and credits.
-   Treat refunds and reversals as new operations rather than deleting
    history.

### Payment rules

1.  A timeout does **not** prove the payment failed; the provider may
    have processed it.
2.  Use a stable idempotency key for each logical payment operation.
3.  Query status or retry with the same key according to the provider's
    contract.
4.  Use webhooks and reconciliation to resolve unknown outcomes.
5.  Separate authorization, capture, void, refund, and reversal
    semantics.
6.  Do not claim global exactly-once effects merely because one API is
    idempotent.

### Trade-offs

  ---------------------------------------------------------------------------
  Decision                Choose based on         Cost / risk
  ----------------------- ----------------------- ---------------------------
  Single DB transaction   One transactional       Saga needs compensation and
  vs Saga                 boundary vs multiple    recovery
                          services                

  2PC vs Saga/eventual    Need for coordinated    2PC
  consistency             atomicity vs            coordination/availability
                          independent service     costs; Saga lacks global
                          progress                atomicity

  Orchestration vs        Central visibility vs   Choreography can be hard to
  choreography            decentralized reactions trace

  Authorization vs        Whether later           Different settlement and
  capture                 fulfillment can fail    reversal semantics

  Ledger vs mutable       Auditability and        More records and domain
  balance only            reconstruction          complexity
                          requirements            
  ---------------------------------------------------------------------------

A wallet transfer whose debit and credit fit in one database can often
use one transaction. Do not introduce Kafka and microservices unless
they solve a real requirement.

------------------------------------------------------------------------

## Pattern 6 --- Search, autocomplete, and discovery

**Problems:** E-commerce and job search, restaurant discovery, document
search, filters/facets, autocomplete, log search.

**Core challenge:** Relevance, typo tolerance, faceted filtering,
ranking, and indexing freshness.

### Typical architecture

``` text
Primary DB -- CDC/events --> Indexer --> Search index
                                      ^
Client -> Search API -> query/ranking-+
```

The search index is usually a derived representation; define which
system owns canonical data.

### Key design moves

-   Understand inverted indexes and analyzers.
-   Use PostgreSQL full-text search when it meets the needs; add a
    dedicated search engine when richer relevance or scale justifies it.
-   Treat asynchronous indexing as eventually consistent.
-   For autocomplete, use a trie/prefix index or search engine; consider
    debouncing and popular-prefix caching.
-   Use cursor/search-after for deep pagination.
-   Rebuild or reindex derived data from a reliable source.

### Trade-offs

  -----------------------------------------------------------------------
  Decision                            Trade-off
  ----------------------------------- -----------------------------------
  SQL search vs dedicated engine      Simpler operations and
                                      transactional freshness vs richer
                                      search and indexing scale

  Sync vs async indexing              Immediate visibility vs decoupled
                                      writes and eventual freshness

  Exact vs fuzzy matching             Precision and speed vs typo
                                      tolerance

  Precomputed candidates vs live      Predictable latency vs freshness
  compute                             

  Index every field vs selective      Query flexibility vs storage and
  indexing                            write amplification
  -----------------------------------------------------------------------

**Important:** Discovery is not commitment. Finding an available driver
or product does not reserve it.

------------------------------------------------------------------------

## Pattern 7 --- Real-time communication and collaboration

**Problems:** Chat, presence, live comments, collaborative documents,
live dashboards, multiplayer state.

**Core challenge:** Low-latency delivery over long-lived connections,
while supporting offline users, ordering, and persistence.

### Key design moves

-   **WebSocket:** bidirectional communication.
-   **SSE:** simpler server-to-client streaming.
-   **Polling:** simpler but can add latency and request overhead.
-   Maintain a connection registry if gateways need to route messages
    across servers.
-   Persist messages when history and recovery are required; transport
    delivery is not durable storage.
-   Use a broker/pub-sub layer to route across gateway instances where
    needed.
-   Store offline messages or durable message state and deliver on
    reconnect.
-   Use per-conversation ordering rather than global ordering unless the
    product truly needs global order.
-   For collaborative editing, understand OT and CRDTs at a conceptual
    level.

### Message states

-   **Sent:** server accepted or persisted the message, according to the
    contract.
-   **Delivered:** recipient device/service acknowledged receipt.
-   **Read:** recipient acknowledged viewing it.

These states are not interchangeable.

### Trade-offs

  -----------------------------------------------------------------------
  Decision                            Trade-off
  ----------------------------------- -----------------------------------
  WebSocket vs polling                Lower-latency bidirectional
                                      communication vs simpler
                                      statelessness

  SSE vs WebSocket                    Simple one-way stream vs two-way
                                      messages

  Ephemeral presence vs durable state Fast and cheap vs history and
                                      queryability

  Per-conversation vs global ordering Scalable local order vs expensive
                                      coordination

  Durable storage vs ephemeral        History and recovery vs lower cost
  delivery                            

  Fan-out on write vs read            Fast delivery vs write
                                      amplification

  OT vs CRDT                          Different complexity and
                                      collaboration guarantees
  -----------------------------------------------------------------------

**Interviewer probes:** Reconnection storms, offline delivery, duplicate
messages, multi-device sync, gateway failure, message ordering, large
group fan-out.

------------------------------------------------------------------------

## Pattern 8 --- Event-driven distribution and notification systems

**Problems:** Email/SMS/push platforms, webhooks, audit event
distribution, CDC consumers, activity streams.

**Core challenge:** Deliver events to the right subscribers while
handling variable consumer speed and failure.

### Key design moves

-   Distinguish a **queue** (competing consumers share work) from
    **pub/sub** (multiple independent subscribers receive events).
-   Use a durable log when replay and partition ordering matter; use a
    task queue for simpler work distribution when appropriate.
-   Partition by entity ID if per-entity ordering is required.
-   Make consumers idempotent and plan for redelivery.
-   Use bounded retries, exponential backoff, DLQs, and replay tooling.
-   For notifications, model preferences, templates, deduplication
    windows, quiet hours, provider limits, and delivery status.

### Webhook delivery checklist

1.  Persist the event or delivery intent.
2.  Identify eligible subscribers.
3.  Create durable delivery tasks.
4.  Deliver with timeouts and bounded retries.
5.  Sign payloads so receivers can verify authenticity.
6.  Store attempt history and delivery state.
7.  Provide replay and DLQ inspection.
8.  Rate-limit per subscriber or destination.

Never let a slow webhook receiver block the core business transaction.

### Trade-offs

  -----------------------------------------------------------------------
  Decision                            Trade-off
  ----------------------------------- -----------------------------------
  Queue vs pub/sub                    Work distribution vs independent
                                      subscribers

  Durable log vs task queue           Replay and ordered partitions vs
                                      simpler task semantics

  Push vs pull consumers              Low delivery latency vs
                                      consumer-controlled pace

  At-most-once vs at-least-once       Fewer duplicates vs better
                                      protection against loss

  Shared vs separate topics           Simplicity vs isolation and
                                      independent retention

  Inline retry vs DLQ                 Quick recovery vs preventing
                                      poison-message loops
  -----------------------------------------------------------------------

------------------------------------------------------------------------

## Pattern 9 --- File storage, media processing, and CDN delivery

**Problems:** Drive/Dropbox, photo sharing, video streaming, chat media,
document upload, recordings.

**Core challenge:** Large binary data makes bandwidth, storage cost,
processing, and delivery dominant concerns.

### Typical architecture

``` text
Client -> API (authorization + metadata) -> pre-signed URL
Client -> Object storage directly
Upload event -> Queue -> Workers (resize/transcode) -> Object storage
Client -> CDN -> cached media
```

### Key design moves

-   Store blobs in object storage and searchable metadata in a database.
-   Use short-lived, narrowly scoped pre-signed URLs for direct
    uploads/downloads.
-   Support multipart/resumable uploads for large objects.
-   Validate size, checksum, ownership, and upload completion.
-   Process images/video asynchronously and idempotently.
-   Use immutable object versions or content-addressed names when useful
    for cache safety.
-   For video, understand manifests, segments, HLS/DASH, and adaptive
    bitrate ladders.
-   For sync products, consider versioning, changed-block transfer, and
    conflict resolution.

### Trade-offs

  -----------------------------------------------------------------------
  Decision                            Trade-off
  ----------------------------------- -----------------------------------
  App proxy vs pre-signed upload      Central control vs lower server
                                      bandwidth

  Object storage vs DB BLOB           Scale and cost vs transactional
                                      simplicity

  CDN vs origin-only                  Low latency and origin relief vs
                                      cache cost/invalidation

  Multipart vs single upload          Resumability and throughput vs more
                                      state

  Sync vs async transcoding           Immediate processing vs responsive
                                      upload and worker scaling

  Eager vs lazy transformations       Fast repeated delivery vs
                                      storage/processing cost

  Immutable versions vs overwrite     Safer cache/version behavior vs
                                      storage growth
  -----------------------------------------------------------------------

### Common traps

-   A pre-signed URL must not authorize the wrong object or tenant.
-   CDN cache keys and access rules must not leak private content.
-   Duplicate upload events must not create incorrect duplicate side
    effects.
-   Large downloads may require range requests and appropriate cache
    headers.
-   Deletion, retention, and legal/privacy requirements need explicit
    treatment.

------------------------------------------------------------------------

## Pattern 10 --- Geospatial systems and location tracking

**Problems:** Ride hailing, food delivery, nearby places/friends, fleet
tracking, geofencing.

**Core challenge:** Find eligible candidates near a location using
rapidly changing spatial data.

### Typical architecture

``` text
Driver devices -> Location ingestion -> Geospatial index
Rider request -> Candidate discovery -> Matching -> Atomic assignment
```

### Key design moves

-   Choose a spatial index such as geohash, H3, S2, an R-tree, or a
    database's spatial index based on the queries.
-   Keep latest locations in a fast index; store history separately only
    if required.
-   Throttle updates based on movement, product latency, battery, and
    network constraints.
-   Search nearby cells, filter eligibility, rank by ETA or another
    objective, send an offer, and expire/retry offers.
-   Use the resource-allocation pattern for the final atomic assignment.
-   Detect stale locations and unavailable drivers.

### Trade-offs

  -----------------------------------------------------------------------
  Decision                            Trade-off
  ----------------------------------- -----------------------------------
  Geohash vs H3 vs R-tree             Different spatial query and
                                      partition properties

  Update frequency                    Freshness vs battery, bandwidth,
                                      and write load

  In-memory index vs database         Latency vs durability and
                                      operational complexity

  Exact vs approximate nearest        Precision vs bounded query cost
  neighbor                            

  Location history vs latest only     Analytics/audit vs storage and
                                      privacy costs

  Cell size                           Precision vs number of neighboring
                                      cells to query
  -----------------------------------------------------------------------

Proximity is candidate selection, not proof the driver is still
available.

------------------------------------------------------------------------

## Pattern 11 --- Coordination primitives and rate limiting

**Problems:** API throttling, login/OTP abuse protection, quotas, unique
ID generators, distributed locks, leader election, consistent hashing.

### Rate-limiting algorithms

  -----------------------------------------------------------------------
  Algorithm               Idea                    Main property
  ----------------------- ----------------------- -----------------------
  Fixed window            Count requests in fixed Simple; can burst at
                          time buckets            window boundaries

  Sliding-window log      Store request           Precise but
                          timestamps              memory-intensive

  Sliding-window counter  Approximate using       Lower memory,
                          adjacent windows        approximate

  Token bucket            Tokens refill at rate   Allows bursts up to
                          `r`, capacity `B`       `B`; sustained rate
                                                  about `r`

  Leaky bucket            Drain at a steady rate  Smooths outgoing
                                                  traffic
  -----------------------------------------------------------------------

### Key design moves

-   Use atomic counter updates, transactions, or scripts when concurrent
    requests must not overrun a quota.
-   Local counters are fast and available but only approximate a global
    quota.
-   Centralized counters improve shared accuracy but add latency and
    dependency risk.
-   Choose the identity key deliberately: IP, user, API key, tenant, or
    destination.
-   Decide fail-open vs fail-closed based on risk. Security-sensitive
    operations often need stricter behavior than low-risk public reads.
-   Know the basics of Snowflake-style IDs: timestamp, worker identity,
    and sequence; clock behavior and worker-ID assignment matter.
-   Understand consistent hashing and why virtual nodes reduce movement
    when nodes change.

### Trade-offs

Accuracy vs latency vs memory; centralized vs local state; fail-open vs
fail-closed; simple IDs vs coordination and collision risks.

------------------------------------------------------------------------

## Pattern 12 --- Distributed scheduling, delayed jobs, and leases

**Problems:** Distributed cron, reminders, delayed messages, recurring
billing, scheduled exports, payment retries, crawlers.

**Core challenge:** Execute work at the right time and recover from
worker or scheduler failures.

### Key design moves

-   Persist schedules and next-run times; do not rely only on in-memory
    timers.
-   Index upcoming work by due time, use time buckets, a delay queue, or
    an ordered store.
-   Claim work atomically and use leases so abandoned jobs can be
    recovered.
-   Make scheduled work idempotent because retries and duplicate
    triggers happen.
-   Track attempts, timeouts, next retry, cancellation, and terminal
    state.
-   For crawlers, include a URL frontier, deduplication, per-host
    politeness, and robots policy.

### Lock vs lease

Worker A acquires a lease and pauses. The lease expires; worker B
acquires it and runs. Worker A resumes and may still attempt the
operation. A lease alone does not prevent a stale owner from acting.

Use fencing tokens or equivalent monotonic versions enforced by the
protected resource, plus idempotency where needed.

### Trade-offs

  -----------------------------------------------------------------------
  Decision                            Trade-off
  ----------------------------------- -----------------------------------
  Poll DB vs delayed queue            Simplicity/durability vs
                                      specialized delivery semantics

  DB claim vs distributed lock        Transactional state vs coordination
                                      abstraction

  At-most-once vs at-least-once       Fewer duplicates vs stronger
                                      recovery

  Central vs partitioned scheduler    Simplicity vs scale and isolation

  Lease expiry vs explicit release    Crash recovery vs stale-owner risk

  Time wheel vs priority queue        Efficient timer scale vs
                                      implementation complexity
  -----------------------------------------------------------------------

------------------------------------------------------------------------

## Pattern 13 --- Multi-tenant SaaS

**Problems:** CRM, project management, enterprise reporting,
organization-based APIs.

**Core challenge:** Many customers share infrastructure without leaking
data or allowing one tenant to monopolize resources.

### Trade-offs

  -----------------------------------------------------------------------
  Decision                            Trade-off
  ----------------------------------- -----------------------------------
  Shared tables vs DB per tenant      Lower cost/easier operations vs
                                      stronger isolation

  Shared schema vs schema per tenant  Operational simplicity vs
                                      per-tenant isolation/migration
                                      flexibility

  Shared vs dedicated partitions      Efficient utilization vs hot-tenant
                                      isolation

  Shared cache vs tenant-scoped keys  Reuse vs cross-tenant leak risk

  Shared worker pool vs per-tenant    Utilization vs noisy-neighbor
  pools                               isolation

  Global vs tenant-level quotas       Simplicity vs fairness

  Shared vs tenant-specific           Easier operations vs isolation and
  encryption keys                     key lifecycle complexity
  -----------------------------------------------------------------------

### Non-negotiables

-   Enforce tenant authorization at every data-access boundary.
-   Include tenant scope in query filters, cache keys, search documents,
    and object paths.
-   Prevent cross-tenant leakage in exports, logs, analytics, and
    backups.
-   Add quotas and bulkheads to control noisy neighbors.
-   Plan tenant-aware migrations, routing, auditing, and restore
    procedures.

A tenant ID is not "just another column." It influences authorization,
partitioning, cache design, observability, and operations.

------------------------------------------------------------------------

## Pattern 14 --- Analytics, aggregation, and counting at scale

**Problems:** Top-K/trending, view counts, ad click aggregation,
leaderboards, dashboards, unique visitors, fraud analytics.

**Core challenge:** Aggregate huge event volumes without interfering
with transactional workloads.

### Typical architecture

``` text
Application events / CDC -> Stream or batch ingestion
                          -> Processing / aggregation
                          -> Analytical store
                          -> Dashboard API
```

### Key design moves

-   Separate OLTP transaction workloads from OLAP scans and aggregations
    when scale warrants it.
-   Choose batch for cost-efficient processing when latency permits;
    streaming for lower-latency insights.
-   Use tumbling/sliding windows and distinguish event time from
    processing time.
-   Handle late and duplicate events with watermarks, deduplication, and
    replay strategy.
-   Precompute common aggregates; keep raw data for the retention period
    required.
-   Use approximate algorithms only when product accuracy allows it:
    -   **Bloom filter:** membership, with false positives possible.
    -   **HyperLogLog:** approximate distinct count.
    -   **Count-Min Sketch:** approximate frequency.
-   Use sharded counters or batching to avoid hot rows.
-   Plan backfills and rebuilding derived tables.

### Trade-offs

  -----------------------------------------------------------------------
  Decision                            Trade-off
  ----------------------------------- -----------------------------------
  OLTP vs OLAP store                  Transactional updates vs analytical
                                      scans

  Batch vs stream                     Simplicity/cost vs low latency

  Precompute vs query-time            Fast dashboards vs freshness/update
                                      cost

  Exact vs approximate                Accuracy vs memory/compute

  Event time vs processing time       Correct handling of late data vs
                                      buffering complexity

  Raw retention vs rollups            Flexibility vs storage cost

  CDC vs application events           Captures committed changes vs
                                      richer business meaning in explicit
                                      events
  -----------------------------------------------------------------------

------------------------------------------------------------------------

## Pattern 15 --- Highly available lookup and configuration services

**Problems:** URL redirects, feature flags, configuration, DNS-like
lookup, global reference data.

**Core challenge:** Serve fast reads while safely propagating updates
and surviving failures.

### What to reason about

-   Availability target, recovery time objective (**RTO**) and recovery
    point objective (**RPO**).
-   Replication lag and failover behavior.
-   Idempotent writes and conflict resolution.
-   Backups and restore testing.
-   Hot keys, partitioning, regional routing, and cache behavior.
-   Graceful degradation: stale data or safe defaults only when product
    semantics allow it.

### Trade-offs

  -----------------------------------------------------------------------
  Decision                            Trade-off
  ----------------------------------- -----------------------------------
  Single primary vs multi-region      Simpler consistency vs regional
                                      resilience and local latency

  Active-passive vs active-active     Easier conflict handling vs write
                                      flexibility and availability

  Strong vs eventual consistency      Immediate agreement vs
                                      latency/availability costs

  SQL vs key-value                    Flexible querying vs simple
                                      high-throughput lookups

  Replication vs sharding             Availability/read scale vs write
                                      scale/routing complexity

  Cache vs authoritative lookup       Low latency vs staleness

  Automatic vs manual failover        Faster recovery vs false
                                      failover/split-brain risk
  -----------------------------------------------------------------------

Understand leader-follower, multi-leader, and leaderless replication at
a conceptual level. Quorum conditions such as `R + W > N` are useful
only with the assumptions of the specific replication model; they do not
automatically guarantee every form of strong consistency.

------------------------------------------------------------------------

# 5. Reusable sub-patterns

Learn these once and reuse them across system designs.

  -----------------------------------------------------------------------
  Sub-pattern             Used in                 Must-know points
  ----------------------- ----------------------- -----------------------
  Idempotency             Payments, bookings,     Stable key, scope,
                          orders, jobs            retention, concurrent
                                                  duplicates, replay same
                                                  result

  Transactional outbox    DB write + event        Business row and outbox
                          publication             row in one transaction;
                                                  relay retries; consumer
                                                  idempotency

  Saga                    Multi-service workflows Local transactions,
                                                  compensation, state
                                                  tracking, recovery

  Hot-key handling        Viral posts, counters,  Salting/sharding, local
                          flash sales             cache, batching,
                                                  aggregation

  Fan-out                 Feeds, notifications    Write vs read vs
                                                  hybrid, backpressure

  Backpressure            Streams, queues,        Queue limits, admission
                          ingestion               control, load shedding

  Deduplication           Events, imports,        Stable event IDs,
                          webhooks                retention window,
                                                  idempotent effects

  Lease/fencing           Schedulers, singleton   Expiry, stale owners,
                          jobs                    monotonic fencing

  Optimistic concurrency  APIs, inventory,        Version column, ETag,
                          documents               conflict response

  CDC                     Search, analytics,      Ordering, duplicates,
                          replication             schema evolution

  Circuit breaker         Remote dependencies     Thresholds, half-open
                                                  state, fallback

  Bulkhead                Multi-tenant systems,   Isolated pools and
                          workers                 quotas

  Retry with backoff +    Remote calls, jobs      Bounded retries,
  jitter                                          retryable error
                                                  classification, avoid
                                                  retry storms

  Reconciliation          Payments, inventory,    Source of truth,
                          workflows               periodic comparison,
                                                  repair actions

  API aggregation / BFF   Dashboards, mobile APIs Parallel calls,
                                                  deadlines, partial
                                                  failure

  Multi-region failover   Global systems          Replication, routing,
                                                  split-brain prevention

  Schema evolution        Events and APIs         Backward/forward
                                                  compatibility, staged
                                                  rollout

  Graceful degradation    Lookup/config services  Safe defaults, stale
                                                  reads where allowed,
                                                  dependency isolation
  -----------------------------------------------------------------------

Example: a payment system may combine transactional workflow, async
processing, idempotency, outbox publication, reconciliation, and rate
limiting.

------------------------------------------------------------------------

# 6. Cross-cutting trade-off matrix

## A. Data and database choices

  -----------------------------------------------------------------------
  Trade-off               Prefer A when           Prefer B when
  ----------------------- ----------------------- -----------------------
  SQL vs NoSQL            Relations, constraints, Access patterns are
                          transactions, flexible  known and a specialized
                          queries matter          data model/scale
                                                  dominates

  Normalize vs            Consistency and write   Read latency and
  denormalize             simplicity matter       avoiding joins dominate

  Single DB vs multiple   One system can meet     Independent scaling,
  DBs                     capacity and isolation  ownership, or access
                          needs                   patterns justify
                                                  separation

  Primary reads vs        Read-your-write         Read throughput is high
  replicas                freshness is required   and lag is acceptable

  Shard vs do not shard   One database cannot     A single DB still meets
                          meet capacity/isolation needs and simpler
                          needs                   operations are
                                                  preferable

  Hash vs range sharding  Even distribution and   Range scans/time-based
                          key lookups dominate    access dominate

  Offset vs cursor        Small data and          Large datasets and
  pagination              arbitrary page jumps    stable traversal matter
                          matter                  

  Soft vs hard delete     Recovery/audit is       Immediate deletion or
                          needed                  reduced retention is
                                                  required

  Shared vs separate      Shared ownership and    Isolation or
  databases               workload are manageable independent scaling
                                                  justifies separation
  -----------------------------------------------------------------------

**Default:** Start with the simplest database that meets the
requirements. "Large scale" alone is not a reason to shard.

## B. Consistency and transactions

  -----------------------------------------------------------------------
  Trade-off               Prefer A when           Prefer B when
  ----------------------- ----------------------- -----------------------
  Strong vs eventual      Balances, ownership,    Feeds, counts,
  consistency             inventory invariants    analytics, and search
                          must hold immediately   can tolerate staleness

  ACID transaction vs     Changes fit in one DB   Workflow spans
  Saga                    transaction             independent service
                                                  boundaries

  Optimistic vs           Conflicts are rare and  Contention is high or
  pessimistic lock        retry is affordable     serialization is
                                                  necessary

  DB constraint vs app    Invariant must hold     Check is advisory and
  check                   under concurrency       not the enforcement
                                                  boundary

  Idempotency vs blind    Duplicate effects are   Operation is genuinely
  retry                   dangerous               safe to repeat

  Sync decision vs async  User needs an           Outcome can safely be
  reconciliation          authoritative result    resolved later
                          now                     
  -----------------------------------------------------------------------

A cache, queue, or distributed lock is not a substitute for a database
constraint when the database can enforce the invariant.

**CAP in one line:** During a network partition, a distributed system
cannot guarantee both linearizable consistency and availability for
every request. PACELC adds the latency-versus-consistency trade-off when
there is no partition.

## C. Caching and delivery

  -----------------------------------------------------------------------
  Choice                  Use when                Avoid or limit when
  ----------------------- ----------------------- -----------------------
  Redis cache             Repeated reads justify  Data is rarely reused
                          memory and operations   or must always reflect
                                                  the latest commit

  CDN                     Content is              Private data has unsafe
                          public/cacheable or     cache keys or headers
                          safely partitioned by   
                          authorization context   

  Cache-aside             Flexible, demand-driven Cold misses or
                          caching is desired      invalidation races
                                                  violate the contract

  Write-through           Cache update belongs in Cache availability must
                          the defined write       not gate every write
                          behavior                

  Write-behind            Deferred persistence is Success must mean data
                          explicitly acceptable   is durably persisted

  No cache                Fetch is cheap or       Repeated expensive
                          freshness dominates     reads overload the
                                                  origin
  -----------------------------------------------------------------------

Caching is not free performance. It creates a second representation and
therefore a consistency problem.

## D. Synchronous vs asynchronous

  -----------------------------------------------------------------------
  Dimension               Synchronous             Asynchronous
  ----------------------- ----------------------- -----------------------
  User response           Immediate               Accepted or pending,
                          result/failure          result later

  Coupling                Caller depends on       Queue decouples caller
                          downstream availability and worker

  Latency                 Can include downstream  Short initial request;
                          calls                   completion later

  Failure handling        Errors propagate to     Retry, DLQ,
                          caller                  reconciliation

  State model             Often simpler           Intermediate states
                                                  required

  Examples                Reserve a seat,         Email, transcode,
                          authorize an operation  report, search indexing
  -----------------------------------------------------------------------

A strong design often uses **synchronous core decisions and asynchronous
side effects**. The correct boundary comes from the business contract,
not a universal rule.

## E. Other recurring decisions

  --------------------------------------------------------------------------------------------
  Decision       Option A           Option B        Choose A when          Choose B when
  -------------- ------------------ --------------- ---------------------- -------------------
  Delivery       At-least-once +    At-most-once    Loss is unacceptable   Occasional loss is
                 idempotency                                               acceptable and
                                                                           duplicates are
                                                                           worse

  Replication    Synchronous        Asynchronous    Stronger               Lower write latency
                                                    durability/freshness   and higher
                                                    is required            throughput matter

  Feed fan-out   Push on write      Pull on read    Read-heavy and         Write amplification
                                                    manageable follower    dominates
                                                    count                  

  Real-time      WebSocket          Polling/SSE     Bidirectional,         Simpler or one-way
  protocol                                          low-latency            updates suffice
                                                    interaction            

  Processing     Batch              Stream          Cost and throughput    Low-latency insight
                                                    dominate latency       is required

  Counting       Exact              Approximate     Billing or correctness Analytics tolerates
                                                    requires exactness     bounded error

  Dependency     Fail-open          Fail-closed     Availability dominates Security or
  failure                                           and risk is low        correctness
                                                                           requires blocking

  Architecture   Monolith           Microservices   Small team, cohesive   Independent
                                                    domain, simpler        ownership/scaling
                                                    operations             justify distributed
                                                                           boundaries

  Invocation     Queue              Direct call     Buffering, retry,      Immediate response
                                                    decoupling required    and simple
                                                                           dependency

  Computation    Eager/precompute   Lazy/on demand  Frequent reads of      Rare or
                                                    expensive results      unpredictable
                                                                           demand
  --------------------------------------------------------------------------------------------

------------------------------------------------------------------------

# 7. Problem-to-pattern mapping

  -----------------------------------------------------------------------
  Interview problem Primary pattern   Secondary         Deep dive
                                      patterns          
  ----------------- ----------------- ----------------- -----------------
  Movie ticket      1: Allocation     Transactions,     Holds, double
  booking                             cache, async      booking, expiry

  Hotel booking     1                 Search,           Date-range
                                      transactions      inventory,
                                                        overlap

  Flight booking    1                 Transactional     Fare classes,
                                      workflows         multi-leg,
                                                        payment failure

  Ride hailing      1 + 10            Real-time,        Driver discovery,
                                      write-heavy       offer expiry,
                                                        atomic assignment

  Flash sale        1                 Rate limiting,    Overselling,
                                      queue             waiting room, hot
                                                        inventory

  E-commerce        5                 Read-heavy, async Order, stock,
  checkout                                              payment, state
                                                        transitions

  Payment system    5                 Event-driven,     Idempotency,
                                      reconciliation    uncertain
                                                        outcome, ledger

  Wallet            5                 Audit/security    Atomic
                                                        debit/credit,
                                                        overdraft
                                                        prevention

  News feed         2                 Fan-out, ranking  Push vs pull, hot
                                                        accounts

  Instagram-like    2 + 9             Async, CDN        Upload, feed,
  app                                                   media delivery

  URL shortener     15 + 2            Analytics         Key generation,
                                                        redirects, hot
                                                        keys

  Notification      8                 Async, rate       Preferences,
  service                             limiting          retries, dedup

  Chat              7                 Durable messaging Ordering,
                                                        delivery states,
                                                        offline users

  Collaborative     7                 Versioning        OT/CRDT, conflict
  editor                                                resolution

  Drive/Dropbox     9                 Sync/versioning   Upload, metadata,
                                                        access, conflicts

  YouTube/Netflix   9                 Async, CDN        Transcoding,
                                                        manifests, edge
                                                        caching

  Search engine     6                 Write ingestion   Indexing,
                                                        relevance,
                                                        freshness

  Autocomplete      6                 Cache             Prefix lookup,
                                                        top-K, latency

  Logs/metrics      3                 Analytics         Backpressure,
                                                        partitioning,
                                                        retention

  Report export     4                 File storage      Job state,
                                                        chunks, retries

  Webhook platform  8                 Async             Attempts,
                                                        signatures,
                                                        replay

  Distributed cron  12                Queue, leases     Duplicate
                                                        execution, missed
                                                        runs

  Web crawler       12                Dedup, rate       URL frontier,
                                      limiting          politeness

  Rate limiter      11                Distributed state Algorithm, shared
                                                        quotas, fail-open

  Unique ID         11                Coordination      Snowflake layout,
  generator                                             clock issues

  Leaderboard       14 + 2            Aggregation       Sorted sets,
                                                        ties, hot keys

  Top-K/trending    14                Streaming         Approximation,
                                                        windows

  Ad click          14 + 3            Dedup             Late events,
  aggregation                                           replay,
                                                        effective-once
                                                        effect

  Multi-tenant SaaS 13                Partitioning      Authorization,
                                                        quotas, noisy
                                                        neighbors

  Real-time         14 + 3            Streaming         Windows, late
  analytics                                             events,
                                                        aggregation

  Config/feature    15                Cache,            Fast reads,
  flags                               replication       propagation, safe
                                                        defaults

  Nearby places     10                Search            Spatial index,
                                                        update frequency
  -----------------------------------------------------------------------

For every problem, master the primary pattern and correctness guarantee.
Learn enough of the secondary patterns to combine them.

------------------------------------------------------------------------

# 8. Core components to master

For every component, be ready to answer: **What problem does it solve?
Why is it better than the simpler alternative here? What new costs or
failure modes does it add?**

  -----------------------------------------------------------------------
  Component                           Topics to know
  ----------------------------------- -----------------------------------
  PostgreSQL/MySQL                    Transactions, isolation,
                                      constraints, indexes, row locks,
                                      optimistic concurrency, query
                                      plans, pools, replication,
                                      partitioning

  Redis                               Cache-aside, TTL, eviction, atomic
                                      ops, sorted sets, counters, rate
                                      limiting, geo, hot keys, outage
                                      behavior

  Kafka / brokers                     Topics, partitions, ordering scope,
                                      consumer groups, acknowledgments,
                                      redelivery, backpressure, replay,
                                      retention, DLQ

  Object storage + CDN                Pre-signed URLs, multipart upload,
                                      lifecycle, cache keys,
                                      invalidation, range requests,
                                      private content

  Search engine                       Inverted index, analyzers,
                                      relevance, indexing lag, facets,
                                      reindexing

  Load balancer/API gateway           L4/L7, routing, health checks,
                                      authentication, throttling,
                                      timeouts

  Replication                         Leader-follower, multi-leader,
                                      leaderless quorum, lag, conflict
                                      resolution

  Sharding                            Key choice, hot shards, resharding,
                                      cross-shard queries

  Bloom filter                        Definitely absent vs maybe present;
                                      false positives

  Resilience patterns                 Timeouts, circuit breaker,
                                      bulkhead, retry/backoff/jitter

  Observability                       Metrics, logs, traces, SLOs, queue
                                      lag, saturation, actionable alerts
  -----------------------------------------------------------------------

------------------------------------------------------------------------

# 9. Capacity estimation cheat sheet

Estimates should be internally consistent and tied to assumptions. These
are rough interview aids, not hardware guarantees.

## Basic conversions

-   1 day = 86,400 seconds, approximately (10\^5) seconds.
-   1 million requests/day ≈ 11.6 requests/second on average.
-   Average QPS = daily requests / 86,400.
-   Peak QPS = average QPS × an assumed peak multiplier (often start
    with 2--5×, then justify it).
-   Daily storage = events/day × average stored bytes/event, plus
    indexes, replication, and metadata overhead.
-   Bandwidth = requests/second × average response size.

## Always estimate

1.  Average and peak read/write QPS.
2.  Read/write ratio and burst behavior.
3.  Storage per day and per year.
4.  Inbound and outbound bandwidth.
5.  Retention period and replication overhead.
6.  Cache working set and hit-rate assumptions.
7.  Queue backlog growth if consumers are slower than producers.

Avoid claiming a fixed "QPS per database node" as a universal fact.
Capacity varies widely with schema, indexes, payload, durability
settings, hardware, query complexity, and contention. State assumptions
and propose benchmarking.

------------------------------------------------------------------------

# 10. Four-week preparation plan

## The eight highest-coverage designs

1.  **Movie ticket booking:** concurrency and double booking.
2.  **Payment/order processing:** transactions, idempotency, Saga.
3.  **News feed:** read optimization, fan-out, hot accounts.
4.  **Metrics/log ingestion:** write throughput, partitioning,
    backpressure.
5.  **Report generation:** async processing and recovery.
6.  **Notification/webhook platform:** delivery, retries, DLQ.
7.  **Chat:** real-time delivery, persistence, ordering.
8.  **Ride hailing:** geospatial discovery plus resource allocation.

Then add URL shortener, rate limiter, autocomplete, media storage,
scheduler, Top-K, multi-tenant SaaS, and multi-region failover.

## Four-week plan

  -------------------------------------------------------------------------------
  Week              Focus             Practice problems         Concepts to
                                                                master
  ----------------- ----------------- ------------------------- -----------------
  1                 Correctness and   Movie booking,            Constraints,
                    transactions      hotel/appointment,        isolation,
                                      payment/wallet, checkout  idempotency,
                                                                outbox, Saga

  2                 Read/write        URL shortener, news feed, Cache, replicas,
                    scaling           catalog/search, metrics   partitioning, hot
                                      ingestion                 keys

  3                 Async and         Reports,                  Retry, DLQ,
                    real-time         notifications/webhooks,   delivery
                                      chat, media processing    guarantees,
                                                                backpressure

  4                 Advanced          Ride hailing, scheduler,  Geospatial,
                    distributed       rate limiter,             leases,
                    systems           multi-tenant SaaS,        isolation,
                                      analytics                 recovery, mock
                                                                interviews
  -------------------------------------------------------------------------------

## How to practice one design (45--60 minutes)

-   **5--10 min:** Requirements, estimates, invariants, pattern
    classification.
-   **10 min:** APIs, data model, service boundaries.
-   **15--20 min:** Architecture and one end-to-end request.
-   **10--15 min:** Failures, consistency, and two or three major
    trade-offs.
-   **5 min:** Bottlenecks, scaling, monitoring, and what to defer.

For each pattern, write a one-page note with: core challenge, invariant,
three key moves, three trade-offs, two failure scenarios, and likely
interviewer probes.

------------------------------------------------------------------------

# 11. Common interview mistakes

1.  **Adding every technology.** Kafka, Redis, and Elasticsearch should
    each solve a named problem.
2.  **Saying "use cache" without invalidation.** Discuss freshness,
    keys, stampedes, and cache outages.
3.  **Claiming exactly-once without defining the boundary.** Separate
    delivery, processing, database commits, and external effects.
4.  **Making everything asynchronous.** Some decisions must be
    authoritative before success is returned.
5.  **Making every downstream call synchronous.** Long latency chains
    amplify dependency failures.
6.  **Treating a distributed lock as complete correctness.** Think lease
    expiry, stale owners, and fencing.
7.  **Ignoring recovery.** Explain how incomplete work is detected and
    repaired.
8.  **Sharding before measuring.** Start with schema, indexes, workload
    estimates, and bottlenecks.
9.  **Confusing availability with correctness.** A fast success response
    for an invalid operation is not success.
10. **Ignoring security and tenant isolation.** Include authentication,
    authorization, quotas, and sensitive-data boundaries.
11. **Skipping read/write ratio and ordering scope.** Both can change
    the architecture.
12. **Not stating trade-offs.** Explain what you gain, what you give up,
    and why the choice fits the requirement.
13. **Ignoring overload behavior.** Explain queue growth, timeouts,
    admission control, and graceful degradation.
14. **Treating a queue as a guarantee of durability.** Define the
    acknowledgment and persistence semantics.

------------------------------------------------------------------------

# 12. One-page revision sheet

## First two minutes of any HLD problem

1.  What invariant must never break?
2.  Is it read-heavy, write-heavy, streaming, or mixed?
3.  What consistency and freshness guarantees are required?
4.  Which work must finish before the response?
5.  What is the ordering scope?
6.  Which primary pattern applies, and which secondary patterns must be
    combined?

## Patterns in one line

  ------------------------------------------------------------------------
                            \# Pattern               Remember
  ---------------------------- --------------------- ---------------------
                             1 Resource allocation   Atomic conditional
                                                     write + hold expiry +
                                                     idempotency;
                                                     authoritative source
                                                     enforces invariant

                             2 Read-heavy            Optimize queries,
                                                     cache/replicate as
                                                     needed; define
                                                     invalidation; hybrid
                                                     feed fan-out

                             3 Write-heavy           Durable buffer where
                                                     needed, partitioning,
                                                     batching,
                                                     backpressure, replay

                             4 Async jobs            Persist job state,
                                                     idempotent worker,
                                                     bounded retry, DLQ,
                                                     outbox

                             5 Transactional         Local
                               workflow              transactions/Saga,
                                                     idempotency, ledger
                                                     where appropriate,
                                                     reconciliation

                             6 Search                Derived index,
                                                     CDC/events,
                                                     relevance, freshness,
                                                     reindexing

                             7 Real-time             WebSocket/SSE as
                                                     appropriate, durable
                                                     state,
                                                     per-conversation
                                                     ordering

                             8 Event distribution    Queue vs pub/sub,
                                                     per-key order,
                                                     retry/DLQ, signed
                                                     webhooks

                             9 Media                 Pre-signed upload,
                                                     object storage, async
                                                     processing, CDN

                            10 Geospatial            Spatial index, latest
                                                     location, candidate
                                                     discovery then atomic
                                                     assignment

                            11 Coordination/rate     Token bucket, atomic
                               limit                 counters, quota
                                                     scope, lease/fencing
                                                     concepts

                            12 Scheduling            Durable schedule,
                                                     claim/lease,
                                                     idempotent job,
                                                     stale-owner
                                                     protection

                            13 Multi-tenant          Tenant scope
                                                     everywhere, quotas,
                                                     isolation,
                                                     noisy-neighbor
                                                     controls

                            14 Analytics             Separate analytical
                                                     path, windows, late
                                                     events, approximate
                                                     algorithms when
                                                     acceptable

                            15 HA lookup             Replication, RTO/RPO,
                                                     safe degradation,
                                                     failover and conflict
                                                     strategy
  ------------------------------------------------------------------------

## Last five minutes of the interview

-   Name the likely bottleneck and the next scaling step.
-   Walk through at least two failure scenarios and recovery.
-   State monitoring signals: latency percentiles, error rate,
    saturation, queue lag, stale data.
-   Mention security and tenant isolation where relevant.
-   Explain what you deliberately deferred and the trigger for adding
    it.

------------------------------------------------------------------------

## Final principle

**Recognize the invariant. Classify the workload. Select the simplest
design that preserves correctness. Make trade-offs explicit. Design for
failure and recovery.**

That skill transfers to far more interview questions than memorizing
individual architectures.
















Yes. After auditing the structure of your HLD System Design Interview Master Guide, I’d say it is already a strong foundation, but I would not call it fully comprehensive yet.

The biggest opportunity is not adding dozens of random system-design questions. It is filling a few important gaps in reusable patterns, production architecture, and the problems that force you to combine several patterns.

The existing guide already covers 15 pattern families, reusable subpatterns, trade-off matrices, 30+ problem mappings, capacity estimation, and a study plan.&#x20;

Here is what I would add before treating it as your main interview-preparation reference.

## 1. Important missing or underdeveloped patterns

1\. Distributed key-value stores and databases

High priority

Design a DynamoDB-like store, distributed cache, or simple distributed database.

Learn consistent hashing, partitioning, replication factor, quorum reads/writes, hinted handoff, anti-entropy repair, conflict resolution, rebalancing, and failure detection.

Why it matters: Your guide discusses these concepts individually, but does not give them a dedicated end-to-end design pattern.

2\. Security, identity, and authorization

High priority

Authentication versus authorization, OAuth 2.0/OIDC, service-to-service identity, mTLS, secrets rotation, tenant isolation, resource-level authorization, abuse prevention, and audit logs.

Why it matters: Security appears in individual examples, but deserves a cross-cutting design framework. A system can be scalable and still fail the interview on security.

3\. Observability, SLOs, and incident response

High priority

Availability and latency SLOs, error budgets, RED/USE metrics, distributed tracing, alert design, queue-lag monitoring, capacity saturation, incident mitigation, and post-incident recovery.

Why it matters: The current guide mentions observability components, but needs a reusable approach for designing the operational side of any system.

4\. Distributed coordination and consensus

High priority

Leader election, consensus basics, split-brain prevention, fencing tokens, membership and failure detection, quorum trade-offs, and when a coordination service is justified.

Why it matters: The existing lease and leader-election coverage is useful, but it should explicitly distinguish a best-effort lease from a correctness-critical consensus-backed coordination mechanism.

5\. Data migration, re-sharding, and zero-downtime changes

High priority

Expand/contract migrations, dual reads and writes, backfills, checkpoints, validation, cutover, rollback, online index creation, and changing partition keys without a prolonged outage.

Why it matters: Interviewers often ask how the design evolves after launch, not just how it works on day one.

6\. Multi-region architecture and disaster recovery

Active-active versus active-passive, regional failover, data residency, replication lag, split-brain risks, RPO/RTO, failback, and disaster-recovery drills.

Why it matters: Multi-region failover is mentioned, but it needs its own decision framework. Routing traffic to another region is not enough if the data is stale, conflicting, or unavailable.

7\. Recommendation, ranking, and graph systems

Personalized feeds, friend/follower graphs, candidate generation, ranking pipelines, feature stores, graph traversal, and offline versus online computation.

Why it matters: Feeds and search are covered, but recommendation systems have a distinct pipeline and a different set of latency, freshness, and quality trade-offs.

8\. Quotas, billing, and usage metering

Usage events, aggregation, deduplication, entitlement checks, quota enforcement, prepaid versus postpaid usage, corrections, and invoice reconciliation.

Why it matters: This is a common SaaS and API-platform design challenge, combining correctness with high-volume event processing.

## 2. Important concepts to add to the cross-cutting sections

These are not necessarily separate patterns. They should be concepts you can apply to almost every system-design problem.

| Missing or underdeveloped concept   | What you should be able to explain                                                                                                       |
| ----------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------- |
| SLOs and availability math          | Availability targets, error budgets, latency percentiles (p50/p95/p99), and how dependencies affect end-to-end reliability               |
| Network fundamentals                | DNS, TCP versus UDP, TLS, connection pooling, keep-alive, HTTP/2 versus HTTP/3, WebSockets, and RPC                                      |
| Consistency models                  | Linearizability, sequential consistency, causal consistency, eventual consistency, read-your-writes, and monotonic reads                 |
| Database isolation                  | Dirty reads, non-repeatable reads, phantom reads, serializability, write skew, deadlocks, and retrying aborted transactions              |
| Replication and failover            | Synchronous versus asynchronous replication, replica lag, stale reads, failover promotion, and recovery                                  |
| Backups and recovery                | Point-in-time recovery, backup validation, restore drills, RPO, RTO, and disaster recovery                                               |
| Load shedding and admission control | Rejecting work early, bounded queues, priority queues, tenant quotas, and protecting critical traffic                                    |
| Deployment safety                   | Canary releases, blue-green deployments, feature flags, rollback, backward-compatible changes, and gradual traffic shifting              |
| Cost engineering                    | Cost per request, storage and egress costs, cache economics, compute utilization, and the cost of multi-region redundancy                |
| Privacy and compliance              | Data retention, deletion propagation, encryption, auditability, PII handling, and regional data restrictions                             |
| Testing distributed systems         | Load and soak testing, fault injection, chaos testing, race-condition testing, and recovery testing                                      |
| Time and clock assumptions          | Clock skew, monotonic clocks, timestamp ordering, event time versus processing time, and why timestamps alone do not establish causality |

One particularly important distinction: exactly-once processing is not a universal guarantee you get by selecting a message broker. You need to define the boundary of the guarantee, how duplicate deliveries are handled, and how database effects remain correct during retries and recovery.

## 3. Additional interview problems worth including

I would expand the problem catalog selectively. These problems exercise genuinely different design decisions rather than just renaming existing examples.

Distributed storage and infrastructure

- Distributed key-value store
- Distributed cache
- Distributed lock or coordination service
- Metrics monitoring and alerting platform
- API gateway and service discovery

High priority

Social and personalization

- Social network / follow graph
- Recommendation system
- Ad-serving and ad-click tracking
- Personalized ranking and feature serving

High priority

Communication and collaboration

- Email service
- Calendar and recurring-event scheduling
- Video conferencing
- Live audio/video streaming
- Document sharing and permissions

Medium priority

Business platforms

- Subscription and billing platform
- Inventory management across warehouses
- Order fulfillment and shipment tracking
- Multi-channel webhook platform
- API usage metering and quota enforcement

Medium priority

Data and content systems

- Web-based paste service
- Data pipeline / ETL platform
- Log search and alerting
- Data warehouse ingestion
- Content moderation pipeline

Medium priority

The existing guide already includes chat, video platforms, crawlers, analytics, payments, file storage, and scheduling. Keep those existing problems; the list above identifies additions or useful deeper variants, not a reason to duplicate the catalog.

## 4. Trade-offs that deserve their own decision tables

The guide has a useful trade-off matrix already. I would extend it with these specific decisions because they frequently determine the architecture.

| Decision                                             | Options to compare                                          | The deciding question                                                                      |
| ---------------------------------------------------- | ----------------------------------------------------------- | ------------------------------------------------------------------------------------------ |
| Single-region vs multi-region                        | One region, active-passive, active-active                   | What availability, latency, and recovery objectives justify the added complexity?          |
| Strong consistency vs availability during partitions | Reject conflicting operations, serve potentially stale data | Which business invariants must never be violated?                                          |
| Leader-based vs leaderless replication               | Primary/replicas, quorum-based designs                      | How important are write coordination, conflict handling, and failover behavior?            |
| Synchronous vs asynchronous workflows                | Request waits, job/event processing                         | Does the caller need the result immediately, and what happens if processing fails?         |
| Push vs pull                                         | Fan-out on write, fan-out on read, hybrid                   | How skewed is the workload, and how many recipients does a single update have?             |
| Shared vs isolated infrastructure                    | Shared database/queues, per-tenant resources                | How much isolation is needed relative to operational cost?                                 |
| Exact vs approximate computation                     | Exact counters, probabilistic algorithms                    | Is bounded error acceptable in exchange for lower latency or resource usage?               |
| Eager vs lazy computation                            | Precompute, compute on demand                               | Is freshness or read latency more important than write cost and storage?                   |
| Hot standby vs cold recovery                         | Warm capacity, provision after failure                      | What recovery time is acceptable, and what is the cost of idle capacity?                   |
| Centralized vs decentralized coordination            | Single authority, distributed coordination                  | Does the system need a global decision, or can it operate independently across partitions? |
| Soft vs hard quotas                                  | Best-effort enforcement, strict enforcement                 | Is temporary overuse acceptable, or must the limit be enforced atomically?                 |
| Store-and-forward vs reject                          | Buffer during downstream failure, fail fast                 | Can the system safely retain pending work, and how will it drain the backlog?              |

For every decision, teach three things: the simplest viable option, what breaks as the system grows, and the signal that justifies switching to the more complex option.

## 5. Add a production-readiness checklist to every design

A design should not be considered complete just because its happy-path architecture works.

### Design review checklist

0 of 8

Correctness

Define invariants, transaction boundaries, idempotency, ordering, and duplicate handling.

Failure handling

Cover timeouts, retries, partial failures, dependency outages, poison messages, and recovery.

Scale

Estimate peak load, identify bottlenecks, plan partitioning, and address hot keys.

Data lifecycle

Specify retention, archival, deletion, backups, schema changes, and restoration.

Security

Cover identity, authorization, tenant isolation, encryption, secrets, and audit logs.

Operations

Define SLOs, dashboards, actionable alerts, runbooks, and capacity thresholds.

Deployment

Plan safe migrations, compatible releases, rollout, rollback, and disaster recovery.

Cost

Identify major compute, storage, network, replication, and operational costs.

Reset checklist

## 6. What I would prioritize

Don't turn the guide into an encyclopedia that is impossible to finish. Make the following additions in this order.

1. First: correctness and distributed systems. Strengthen consistency, database isolation, consensus, replication, idempotency, and failure recovery.
2. Second: production engineering. Add security, SLOs, disaster recovery, observability, safe migrations, and cost modeling.
3. Third: missing pattern families. Add distributed key-value storage, recommendations/ranking, and metering/quotas.
4. Fourth: additional practice problems. Add a distributed cache, monitoring platform, recommendation system, ad-serving system, and subscription/billing system.
5. Fifth: practical interview exercises. For each major design, include a baseline solution, a scale-up scenario, a failure scenario, and a trade-off discussion.

My recommendation is to update the existing Markdown with these additions rather than create another separate guide. Keep the 15 current patterns as the foundation, add the missing pattern families, and make security, reliability, migration, and operations mandatory cross-cutting sections.

The real target is not to memorize every system. It is to recognize the workload, select the right patterns, justify the trade-offs, and explain how the design behaves when components fail or the workload grows by 10×. That is what makes the guide useful for unfamiliar HLD interview questions, too.

