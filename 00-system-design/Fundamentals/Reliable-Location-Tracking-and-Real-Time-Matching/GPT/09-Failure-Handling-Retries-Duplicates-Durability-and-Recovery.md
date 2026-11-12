# Chapter 9 — Failure Handling, Retries, Duplicates, Durability and Recovery

We've now designed almost the entire system.

We have:

- Location ingestion servers.
- Redis GEO partitions.
- Optional Kafka event streams.
- Geographical indexes.
- Nearby driver discovery.
- Matching and ranking.
- Dispatch orchestration.
- WebSocket gateways.
- Driver reservations.
- Transactional assignment.
- Heartbeat and liveness detection.

But there's one thing we haven't done properly yet:

We've mostly described how everything works when components are healthy. Now let's understand what happens when things fail.

This is what separates a system that works in a demo from one that can operate reliably at scale.

We'll deliberately break our architecture, understand the consequences, and design recovery mechanisms.

# 9.1 First, understand the different types of failures

Not all failures are the same.

Consider these scenarios.

| Scenario                                           | Failure type               |
| -------------------------------------------------- | -------------------------- |
| Driver temporarily loses mobile network            | Transient                  |
| Redis node crashes                                 | Infrastructure failure     |
| Database rejects a constraint violation            | Permanent/business failure |
| Backend request times out but database committed   | Ambiguous outcome          |
| Kafka consumer is slower than producers            | Processing lag             |
| Gateway receives a duplicate event                 | Duplicate delivery         |
| A location update arrives after a newer update     | Out-of-order event         |
| Entire Redis cluster becomes unavailable           | Dependency outage          |
| 100,000 drivers reconnect simultaneously           | Traffic overload           |
| Database is healthy but one region cannot reach it | Network partition          |

Why does this classification matter?

Because we shouldn't handle every failure by blindly retrying.

For example:

```
Database temporarily unavailable
    -> Retry may succeed.

Invalid GPS coordinates
    -> Retrying won't help.

Ride already assigned
    -> Retrying the same assignment won't help.

Request timed out
    -> Outcome may be unknown; check the existing operation.
```

A good failure-handling system first asks:

1. Is the error transient?
2. Is retrying safe?
3. Can the operation have already succeeded?
4. Is the request still useful?
5. Is the dependency healthy enough to accept another attempt?

Only then should it retry.

# 9.2 The most important concept — At-most-once, at-least-once and exactly-once

These terms appear everywhere in distributed systems.

Let's understand them using our location tracking system.

Suppose D101 sends:

```
Location Update 101
```

We need to deliver this update to Redis.

There are three possible approaches.

## A. At-most-once delivery

We send the update once.

```
Driver
   |
   v
Server
   |
   v
Redis
```

If the request fails, we don't retry.

Possible outcomes:

```
Update received -> SUCCESS
Update lost     -> LOST
```

The update is processed zero or one times.

Advantages:

- No duplicate processing.
- Simple.
- Low overhead.

Disadvantages:

\- Some updates may be lost.

Is this acceptable?

For the latest live location, sometimes yes, provided another update will arrive shortly and we don't promise durable acceptance.

However, we still need to protect the index against old updates.

## B. At-least-once delivery

Now suppose the client retries when it doesn't receive an acknowledgement.

```
Attempt 1 -> Timeout
Attempt 2 -> Timeout
Attempt 3 -> Success
```

The server might have actually processed the first attempt, but the response was lost.

So Redis may receive the same logical update multiple times.

The update is delivered one or more times, assuming retries continue and the service eventually becomes available.

Advantages:

- Better chance of successful processing.
- Temporary failures can recover automatically.

Disadvantages:

- Duplicate messages are possible.
- Processing must be idempotent.

This is generally a practical choice for location ingestion and message delivery.

## C. Exactly-once processing

People often assume we must guarantee that a network request is delivered exactly once.

That's not a realistic assumption across unreliable networks.

Consider:

```
Server processes request successfully.
Response gets lost.
Client retries.
```

How can the client know whether the original operation happened?

It cannot know merely from the missing response.

Instead, we use idempotency.

The client may send the same command multiple times, but the business effect occurs only once.

For example:

```
ACCEPT RIDE R501
ACCEPT RIDE R501
ACCEPT RIDE R501
```

The result remains:

```
R501 -> D101
```

This is what we actually want.

Exactly-once business effects are achieved through durable state, idempotency, atomic transactions, and duplicate detection—not by assuming the network delivers a message exactly once.

### Comparison

| Guarantee           | Meaning                                                | Duplicate possible?                 | Lost operation possible?                              |
| ------------------- | ------------------------------------------------------ | ----------------------------------- | ----------------------------------------------------- |
| At-most-once        | Process zero or one times                              | No                                  | Yes                                                   |
| At-least-once       | Retry until processed                                  | Yes                                 | Possible if retries stop or system never recovers     |
| Exactly-once effect | Repeated delivery produces one logical business effect | Transport duplicates still possible | Depends on durable acceptance and recovery guarantees |

For our architecture:

| Operation              | Preferred behavior                                                               |
| ---------------------- | -------------------------------------------------------------------------------- |
| Live location update   | At-least-once attempts + version-based idempotency, with latest-value coalescing |
| Location history event | Durable acceptance when history is required                                      |
| Offer delivery         | At-least-once attempts while the offer is valid                                  |
| Offer acknowledgement  | Idempotent                                                                       |
| Driver acceptance      | Idempotent durable transaction                                                   |
| Ride assignment        | At most one active assignment per ride                                           |
| WebSocket cancellation | Retryable, but backend state remains authoritative                               |
| Heartbeat              | Best effort; next heartbeat refreshes liveness                                   |

# 9.3 Retries — How should we retry failed operations?

Let's begin with a simple example.

D101 sends a location update.

```
Driver -> Ingestion Server -> Redis
```

Redis is temporarily unavailable.

The request fails.

Should we retry immediately?

Repeatedly retrying immediately can make an outage worse.

Suppose Redis is already overloaded.

If 300,000 clients immediately retry, we produce another large traffic spike.

This creates a retry storm.

## Exponential backoff

Instead of retrying at a constant interval, gradually increase the delay.

For example:

```
Attempt 1 -> Immediate
Attempt 2 -> Wait 1 second
Attempt 3 -> Wait 2 seconds
Attempt 4 -> Wait 4 seconds
Attempt 5 -> Wait 8 seconds
```

Conceptually:

\\[ Delay = \min(BaseDelay \times 2^{attempt}, MaxDelay) \\]

But there's one more problem.

Imagine one million clients fail simultaneously.

They all retry after one second.

We still get a massive burst.

### Add jitter

Randomize retry timing.

For example:

```
Base delay = 4 seconds

Client A -> 3.2 seconds
Client B -> 4.5 seconds
Client C -> 2.8 seconds
Client D -> 5.1 seconds
```

Now requests are spread across time.

A common strategy is full jitter:

\\[ Delay = Random(0,\min(MaxDelay,BaseDelay \times 2^{attempt})) \\]

The maximum retry delay is capped.

This is particularly important for mobile applications reconnecting after an outage.

### When should we retry?

| Failure                     | Retry?                                      |
| --------------------------- | ------------------------------------------- |
| Network timeout             | Usually yes                                 |
| HTTP 503                    | Yes, with backoff                           |
| HTTP 429                    | Yes, respect `Retry-After` where provided   |
| Database deadlock           | Usually yes, retry entire transaction       |
| Database connection failure | Yes, bounded                                |
| Invalid payload             | No                                          |
| Authentication failure      | No, until credentials/session are corrected |
| Ride already assigned       | No                                          |
| Offer expired               | No                                          |
| Driver not eligible         | No                                          |
| Unsupported service         | No                                          |

### A critical rule: Retry the entire transaction after a transaction failure

Suppose our assignment transaction locks a ride, updates the driver, then encounters a deadlock.

The database rolls the transaction back.

We should retry the complete logical transaction, not blindly retry only the last SQL statement.

Also, use the same idempotency key for the retried logical acceptance.

## 9.4 Retry budgets — Don't retry forever

Imagine a dispatch service calls Redis.

Redis is unavailable.

We retry:

```
1
2
3
4
5
6
7
...
```

Meanwhile, requests accumulate.

This consumes:

- Threads.
- Connections.
- Memory.
- CPU.
- Network bandwidth.

Retries can actually cause a healthy application to collapse even when the original failure is confined to one dependency.

We need bounded retries.

For example:

```
Maximum attempts = 3

OR

Maximum retry time = 2 seconds
```

Whichever comes first.

The exact values depend on the operation's deadline.

### Every request needs an overall deadline

Imagine a customer requests a ride.

We want matching to finish within a defined time budget.

For example:

```
Total matching budget = 500 ms
```

Within that budget:

```
Redis Search       -> 100 ms
Eligibility Check  -> 100 ms
ETA Calculation    -> 200 ms
Ranking             -> 50 ms
Remaining buffer    -> 50 ms
```

These numbers are illustrative.

The important idea is that individual operations share an overall deadline.

If Redis takes 400 ms, we shouldn't continue waiting indefinitely and then perform another 500 ms ETA operation.

We need deadline propagation.

### Avoid retry multiplication

Suppose:

```
Mobile App retries 3 times.

Gateway retries 3 times.

Ingestion Service retries 3 times.

Redis Client retries 3 times.
```

One logical request might generate:

\\[ 3 \times 3 \times 3 \times 3 = 81 \\]

attempts in the worst case, depending on how each layer counts attempts.

This is dangerous.

Define which layer owns the retry policy.

For example:

```
Mobile App:
    Retry request when the overall request outcome is unknown.

Ingestion Server:
    Small bounded retry for transient Redis errors.

Redis Client:
    No hidden unbounded retries.

Matching Service:
    Deadline-aware retries for individual partitions.
```

Don't let every layer retry independently without a shared budget.

# 9.5 The most important reliability technique — Idempotency

Let's understand why this matters so much.

Suppose D101 reports a location.

```
{
  "driverId": "D101",
  "sessionEpoch": 11,
  "sequenceNumber": 101,
  "latitude": 17.3850,
  "longitude": 78.4867
}
```

The server updates Redis successfully.

But the response is lost.

The mobile app retries.

Now Redis receives the same message again.

Should we update the location again?

We could, but we don't need to.

We can identify the logical operation using:

```
driverId
sessionEpoch
sequenceNumber
```

The unique event identity is effectively:

```
D101:11:101
```

When the same update arrives again:

```
Stored version = (11, 101)
Incoming version = (11, 101)
```

We ignore it.

Now suppose an older update arrives:

```
Incoming version = (11, 100)
Stored version = (11, 101)
```

We ignore that too.

And a newer update arrives:

```
Incoming version = (11, 102)
Stored version = (11, 101)
```

We accept it.

This gives us two protections:

1. Duplicate protection.
2. Out-of-order protection.

### A useful rule for location ingestion

For live location:

```
Accept only if incoming version > stored version.
```

The comparison and update must happen atomically.

We already introduced this in Chapter 2.

The important lesson now is that retries are safe because our write operation is idempotent.

# 9.6 Idempotency across the entire system

Let's build an idempotency strategy for every important operation.

| Operation                 | Idempotency identity                                                       |
| ------------------------- | -------------------------------------------------------------------------- |
| Location update           | Driver ID + session epoch + sequence number                                |
| Driver heartbeat          | Driver ID + session epoch + heartbeat sequence, if deduplication is needed |
| Offer creation            | Offer ID or dispatch-attempt ID                                            |
| Offer delivery            | Event ID + offer ID                                                        |
| Offer acknowledgement     | Offer ID + acknowledgement type                                            |
| Offer rejection           | Offer ID + command/request ID                                              |
| Driver acceptance         | Acceptance request ID + offer ID                                           |
| Ride assignment           | Ride ID + authoritative assignment transition                              |
| Offer cancellation        | Offer ID + cancellation event ID                                           |
| Historical location event | Event ID or driver/session/sequence tuple                                  |

The key is that an idempotency identifier must identify one logical operation, not merely one HTTP request attempt.

For example:

```
First HTTP request:
    requestId = ACCEPT-991

Retry:
    requestId = ACCEPT-991
```

Same logical operation.

But:

```
A new offer acceptance:
    requestId = ACCEPT-992
```

This is a different operation.

### Where should we store idempotency records?

For business-critical operations, keep them in the transactional database.

For example:

```
CREATE TABLE idempotency_records (
    idempotency_key VARCHAR(150) PRIMARY KEY,
    operation_type VARCHAR(50) NOT NULL,
    result_status VARCHAR(50),
    result_payload JSONB,
    created_at TIMESTAMP NOT NULL
);
```

A simplified transaction:

```
BEGIN

Check idempotency key.

If already completed:
    Return stored result.

Otherwise:
    Execute business operation.
    Store result against idempotency key.

COMMIT
```

The uniqueness constraint handles concurrent duplicate requests.

If two servers receive the same request simultaneously, they cannot both create independent completed records for the same key.

### How long should we retain idempotency records?

It depends on the operation.

For location updates, retaining every update identity forever is unnecessary. We can keep only the latest accepted version per driver/session.

For driver acceptance, the offer and assignment history already provide durable evidence of the result.

For payment-like or financial operations, retention and deduplication requirements may be much longer.

Don't create one global idempotency policy for every operation. Match retention to the business operation's lifetime and risk.

# 9.7 The Inbox pattern — Prevent duplicate event processing

We've already introduced the transactional outbox.

Now let's introduce its counterpart: the Inbox pattern.

Imagine:

```
Assignment Service
       |
       v
Outbox Publisher
       |
       v
Message Broker
       |
       v
Notification Service
```

The publisher sends:

```
Event ID: EVT-501
Type: RIDE_ASSIGNED
Ride: R501
Driver: D101
```

The notification service receives it.

It processes the event successfully.

But before acknowledging the message, the consumer crashes.

The broker redelivers the same event.

Now the notification service receives EVT-501 again.

How do we prevent duplicate processing?

We maintain an inbox/deduplication table.

```
CREATE TABLE processed_events (
    consumer_name VARCHAR(100) NOT NULL,
    event_id VARCHAR(150) NOT NULL,
    processed_at TIMESTAMP NOT NULL,

    PRIMARY KEY (consumer_name, event_id)
);
```

The processing transaction becomes:

```
BEGIN

1. Insert event into processed_events.

2. If the event already exists:
       Skip processing.

3. Otherwise:
       Perform consumer's database changes.

COMMIT
```

Now the processing and deduplication record commit together.

### Why must they be in the same transaction?

Imagine:

```
Insert inbox record -> SUCCESS

Process event -> FAILURE
```

If these are separate transactions, the event may be marked processed even though its business effect did not happen.

Or:

```
Process event -> SUCCESS

Insert inbox record -> FAILURE
```

The event is processed, but a redelivery might process it again.

Combining them atomically avoids these partial outcomes when both operations are in the same transactional database.

### Does an inbox solve duplicate WebSocket sends?

Not completely.

It can prevent duplicate business processing, but the actual network delivery can still be repeated.

The mobile application should also deduplicate using the stable `eventId` or `offerId`.

If the event causes an external side effect, such as sending a push notification through a third-party provider, we need to understand that provider's own idempotency capabilities and ambiguous-result behavior.

The inbox does not magically create an atomic transaction with an external provider.

# 9.8 Transactional outbox — Let's understand it deeply

This pattern is important enough to examine with a concrete failure.

Suppose the assignment service executes:

```
1. Assign ride R501 to D101.
2. Publish RIDE_ASSIGNED event.
```

These are two independent systems:

```
Database
Message Broker
```

There is no ordinary local transaction spanning both.

### Failure scenario A

```
Database commit -> SUCCESS

Broker publish -> FAILURE
```

The ride is assigned.

But no notification is sent.

### Failure scenario B

```
Broker publish -> SUCCESS

Database commit -> FAILURE
```

A consumer receives a notification about an assignment that never committed.

Both are dangerous.

### Outbox solution

We create the business state and event record in one database transaction.

```
BEGIN;

UPDATE rides
SET status = 'ASSIGNED',
    assigned_driver_id = 'D101'
WHERE ride_id = 'R501'
  AND status = 'SEARCHING';

INSERT INTO outbox_events (
    event_id,
    event_type,
    aggregate_id,
    payload,
    status
)
VALUES (
    'EVT-501',
    'RIDE_ASSIGNED',
    'R501',
    '{"driverId":"D101"}',
    'PENDING'
);

COMMIT;
```

Now:

```
Ride assigned
AND
Event saved in outbox
```

Either both commit or neither commits.

A separate publisher reads pending records.

```
Outbox Table
     |
     v
Outbox Publisher
     |
     v
Message Broker
     |
     v
Consumers
```

If the publisher crashes, another worker retries the event.

### What if publishing succeeds but marking it published fails?

This is the unavoidable ambiguity:

```
Publisher -> Broker: SUCCESS
Publisher crashes
Outbox status still PENDING
```

The event will be published again.

That's fine because consumers are idempotent.

We therefore get:

```
Transactional creation
+
At-least-once publication attempts
+
Idempotent consumption
```

This is a practical reliable event-delivery architecture.

### How do we scale the outbox publisher?

We can:

- Use indexed pending-event queries.
- Claim batches of events.
- Use `FOR UPDATE SKIP LOCKED` in databases that support it.
- Partition work by aggregate or region.
- Track attempts and last error.
- Monitor oldest pending-event age.
- Use exponential backoff.
- Move poison events into a monitored dead-letter workflow.

Also, if ordering matters, events for one ride should be published and processed in an order consistent with the ride's state transitions.

An outbox publisher that arbitrarily publishes concurrent rows can violate business event ordering unless ordering is explicitly preserved.

For example:

```
RIDE_ASSIGNED
RIDE_CANCELLED
```

should not accidentally reach a consumer as:

```
RIDE_CANCELLED
RIDE_ASSIGNED
```

We can use aggregate version numbers and enforce ordered processing per aggregate.

# 9.9 Queue vs Kafka — Now let's make the decision properly

We've discussed Kafka in Chapter 3, and queues in Chapter 6.

Let's now establish a clear rule for choosing between them.

First, understand that they solve related but different problems.

## A. Traditional work queue

Think of a queue as a system that distributes tasks among workers.

```
                Producer
                   |
                   v
                 Queue
                   |
           +-------+-------+
           |       |       |
           v       v       v
        Worker A Worker B Worker C
```

Suppose we publish:

```
SEND_OFFER_TO_D101
SEND_OFFER_TO_D102
SEND_OFFER_TO_D103
```

Workers consume these tasks and attempt delivery.

Once a task is successfully acknowledged, it is removed from the queue according to the broker's delivery semantics.

If a worker crashes before acknowledging, the task can be redelivered, subject to the queue's configuration.

This is a natural fit for:

- Offer delivery tasks.
- Notification jobs.
- Timeout processing.
- Background work.
- Retryable operations.

Examples include RabbitMQ and Amazon SQS.

## B. Kafka

Kafka is better understood as a durable, partitioned event log.

```
Producer
   |
   v
Kafka Topic
   |
   +------> Consumer A
   |
   +------> Consumer B
   |
   +------> Consumer C
```

The same event can be independently consumed by multiple consumer groups.

For example:

```
LOCATION_TOPIC
      |
      +------> Redis Location Updater
      |
      +------> Historical Storage
      |
      +------> Analytics
      |
      +------> Fraud Detection
```

Kafka retains events according to retention configuration.

Consumers track their own progress using offsets.

This makes Kafka particularly useful for:

- High-volume event ingestion.
- Event replay.
- Multiple downstream consumers.
- Historical event processing.
- Partitioned ordering.
- Stream processing.

## C. Direct calls

Sometimes we don't need either.

For example:

```
Dispatch Service
      |
      | Internal RPC
      v
WebSocket Gateway
```

If we only need to route an ephemeral notification to a connected gateway, an internal RPC can be simpler and faster than introducing a broker.

We still need a retry/recovery mechanism for important offers.

## Decision table

| Requirement                                       | Good starting choice                       |
| ------------------------------------------------- | ------------------------------------------ |
| Update latest driver location                     | Direct Redis                               |
| Preserve all required GPS events                  | Kafka-first                                |
| Send notification jobs to workers                 | Durable work queue                         |
| Publish events to multiple independent consumers  | Kafka                                      |
| Route a message to a known active gateway         | Internal RPC or gateway-specific messaging |
| Process offer expiration tasks                    | Delayed queue or scheduler                 |
| Replay historical location events                 | Kafka                                      |
| Send a simple request to another service          | Direct RPC                                 |
| Persist a business decision and publish its event | Database transaction + outbox              |
| Make an assignment atomic                         | Transactional database                     |

### One important observation

Kafka is not a distributed lock.

A queue is not a transactional database.

WebSockets are not a durable messaging system.

Redis Pub/Sub is not a durable queue.

Each solves a different problem.

# 9.10 Kafka consumer failures — What if Redis updates succeed but offsets don't commit?

Let's revisit our Kafka-first location architecture.

```
Driver
   |
   v
Ingestion
   |
   v
Kafka
   |
   v
Redis Location Consumer
```

Suppose the consumer processes:

```
D101 -> Session 11 -> Sequence 101
```

It successfully updates Redis.

But it crashes before committing its Kafka offset.

After restarting, Kafka delivers the same event again.

What should happen?

Our Redis update operation is version-aware.

```
Stored version = (11, 101)
Incoming version = (11, 101)
```

The update is ignored as a duplicate.

Now the consumer can successfully commit the offset.

This is safe.

### Why shouldn't we commit the offset before updating Redis?

Suppose:

```
1. Commit Kafka offset.
2. Update Redis.
3. Redis operation fails.
```

Kafka believes the message was processed.

The consumer won't normally receive it again.

Our Redis update is permanently lost from this processing path.

So, for a non-transactional Redis sink, the usual order is:

```
1. Read event.
2. Idempotently update Redis.
3. Commit Kafka offset.
```

If the consumer crashes between steps 2 and 3, it processes the same event again.

Idempotency makes this safe.

### What about multiple consumers?

Kafka distributes partitions across consumers within a consumer group.

For driver-location updates, partitioning by driver ID is useful:

```
Kafka Key = D101
```

It preserves per-driver event ordering in a partition under the normal single-key partitioning scheme.

But remember:

- Events from old sessions may still arrive.
- A producer can retry or publish duplicates.
- Different producers can introduce concurrency.
- A session change might occur while older events are still being processed.

The consumer still validates session epochs and sequence numbers.

Kafka ordering is helpful, but our business data should not blindly rely on it.

### What about exactly-once Kafka processing?

Kafka supports idempotent producers and transactional processing for certain Kafka-to-Kafka workflows.

For example:

```
Kafka Topic A
     |
     v
Kafka Consumer
     |
     v
Kafka Topic B
```

Kafka transactions can coordinate consumed offsets and produced Kafka records.

But this does not automatically give exactly-once side effects in Redis or an external database.

For external sinks, we still need idempotent updates or an appropriate transactional integration.

# 9.11 Durability — Which information must survive a crash?

This is where we make one of the most important architectural decisions.

Let's classify our data.

| Information                            | Must survive crash?                   | Recovery strategy                                    |
| -------------------------------------- | ------------------------------------- | ---------------------------------------------------- |
| Latest idle driver's location          | Usually not necessarily               | Driver sends a fresh location                        |
| Latest location during active delivery | Depends on tracking requirements      | Durable stream if required                           |
| Old location samples                   | Business-dependent                    | Historical storage                                   |
| WebSocket connection                   | No                                    | Driver reconnects                                    |
| Connection registry                    | Not permanently                       | Lease expires and reconnects                         |
| Temporary search candidate list        | No                                    | Recompute                                            |
| Temporary Redis availability index     | No                                    | Rebuild from authoritative state and fresh locations |
| Driver reservation                     | Yes, while valid                      | Transactional database                               |
| Ride offer                             | Yes, while valid                      | Durable offer record                                 |
| Offer delivery event                   | Yes, while valid                      | Outbox and retry                                     |
| Ride assignment                        | Absolutely                            | Transactional database                               |
| Assignment history                     | Yes                                   | Durable database                                     |
| Payment/order business events          | Yes, according to business guarantees | Durable transactional/event infrastructure           |

Let's explore a few.

## A. Latest location

For an idle driver, losing the latest location is generally acceptable.

Why?

```
Redis loses D101's location.
       |
       v
D101 sends another update.
       |
       v
Redis repopulates D101.
```

We temporarily lose the driver's visibility.

But we haven't lost a business transaction.

However, until a fresh update arrives, the driver is not matchable.

## B. Historical trip location

Suppose a customer needs to see the delivery partner's route history.

Now the intermediate coordinates matter.

We need an explicit retention and durability policy.

For example:

```
During active delivery:
    Publish selected location events to Kafka.

After delivery:
    Process into historical storage.

For live matching:
    Update Redis independently.
```

If the history must be complete, the system needs a durable acceptance boundary before claiming that every event has been preserved.

## C. Connection state

Losing the WebSocket connection is not a business-data loss.

The driver reconnects.

We recreate the connection registry entry.

We do not need to permanently preserve an old socket.

## D. Assignment

This is different.

Suppose:

```
R501 assigned to D101
```

If we lose that fact, we might assign R501 to D102.

That could result in two drivers physically travelling to the same pickup.

So the assignment must have a durable authoritative record.

This is why different data deserves different durability guarantees.

# 9.12 The three storage systems — Final responsibility separation

Let's consolidate the architecture.

```
                  LOCATION UPDATES
                         |
                         v
                    Ingestion
                         |
              +----------+----------+
              |                     |
              v                     v
          Redis GEO               Kafka
              |                     |
              v                     +------> History
         Live Matching              |
                                    +------> Analytics


                  BUSINESS WORKFLOW
                         |
                         v
                  Dispatch Service
                         |
                         v
                 Transactional DB
                         |
                 +-------+-------+
                 |               |
                 v               v
             Offers         Assignments
                 |
                 v
             Outbox
                 |
                 v
           Message Broker
                 |
                 v
          WebSocket Gateways
                 |
                 v
              Drivers
```

Three key responsibilities:

| System                 | Responsibility                                          |
| ---------------------- | ------------------------------------------------------- |
| Redis                  | Current operational view and fast geographical lookup   |
| Kafka, when required   | Durable high-volume event stream and replay             |
| Transactional database | Authoritative business state and assignment correctness |

The message broker is an additional delivery mechanism. It doesn't replace any of these three responsibilities.

# 9.13 Failure scenario 1 — Redis successfully processes an update, but the ACK is lost

Let's simulate this exactly.

```
10:00:00

Driver sends:
D101, sequence 101

Ingestion Server:
    Receives update.

Redis:
    Updates location successfully.

Ingestion Server:
    Crashes before sending HTTP response.
```

The application sees a timeout.

It retries:

```
D101, sequence 101
```

Redis now contains:

```
D101:
    sessionEpoch = 11
    sequence = 101
```

The atomic version-check operation rejects the duplicate.

The server can return:

```
ALREADY_PROCESSED
```

The driver knows the update has been accepted.

No corruption occurs.

Mechanism: Idempotent version-aware write.

# 9.14 Failure scenario 2 — Redis is unavailable

```
Driver
   |
   v
Ingestion Server
   |
   X
Redis unavailable
```

What should the ingestion service do?

1. Do not claim that the location was successfully stored.
2. Return a retryable response.
3. Apply bounded backoff.
4. Retain only the latest location for live recovery.
5. Resume writing when Redis becomes available.

What happens to nearby matching?

The system must recognize that the live index is unavailable.

It should not serve stale cached coordinates as though they were current.

We have two choices:

Option A: Pause affected matching requests.

Return a temporary service-unavailable or retryable response.

Option B: Use a healthy, independently maintained regional index.

Only do this when its freshness and correctness guarantees are understood.

Don't fall back to an old snapshot without checking its age.

The safe principle is:

During live-location storage failure, temporarily reduce matching availability rather than knowingly matching using untrustworthy coordinates.

# 9.15 Failure scenario 3 — Kafka is unavailable

The behavior depends on our architecture.

### Case A: Redis-first, Kafka is only for best-effort history

```
Location Ingestion
      |
      +----> Redis SUCCESS
      |
      +----> Kafka FAILURE
```

Live matching can continue.

However, we lose or delay historical data.

We need to expose that limitation operationally rather than pretending history is complete.

### Case B: Kafka-first

```
Location Ingestion
      |
      X
Kafka unavailable
```

If Kafka is our durable acceptance boundary, the system must not acknowledge durable acceptance until Kafka confirms it according to the required durability configuration.

Possible actions:

- Temporarily buffer locally only within explicitly bounded limits.
- Allow the mobile client to retry.
- Apply backpressure.
- Reject or degrade requests when the buffer is exhausted.

For live-only latest-value semantics, a temporary latest-location buffer may be sufficient.

For guaranteed complete history, dropping events silently is not acceptable.

### Case C: Redis-first and Kafka both required

If both are mandatory, we need a carefully defined policy.

A common mistake is:

```
Write Redis
Write Kafka
Return success regardless of Kafka result
```

This silently violates the history durability requirement.

Instead, define which system represents the acceptance boundary.

If historical durability is mandatory, a durable event stream should generally be the first committed boundary, with Redis updated as a downstream projection.

# 9.16 Failure scenario 4 — Assignment committed, but the response is lost

This is the most important acceptance failure.

```
D101 -> ACCEPT O101
       |
       v
Assignment Service
       |
       v
Database Transaction
       |
       v
COMMIT SUCCESS
       |
       X
Response lost
```

D101 retries the same acceptance.

The new server checks:

```
O101 = ACCEPTED
R501 = ASSIGNED to D101
```

It returns the previous successful result.

It does not create another assignment.

What if the retry reaches another server?

It doesn't matter.

All servers consult the same authoritative assignment state.

Mechanisms:

- Durable idempotency record.
- Offer state.
- Atomic assignment transaction.
- Database uniqueness constraints.

# 9.17 Failure scenario 5 — Offer created, but dispatch service crashes

Imagine:

```
1. D101 is reserved.
2. Offer O101 is committed to database.
3. Outbox event is committed.
4. Dispatch service crashes.
5. Driver hasn't received the offer.
```

Do we lose the offer?

No.

The durable state remains:

```
Offer O101:
    PENDING

Outbox Event:
    PENDING
```

Another outbox publisher can pick up the event and attempt delivery.

The dispatch orchestrator can also recover pending offers from the database.

It doesn't need the previous JVM's memory.

### What about an offer that was already delivered?

Suppose the gateway sent O101, but the publisher crashed before marking the event delivered.

The message might be sent again.

The mobile application deduplicates using:

```
offerId = O101
```

The backend checks the offer's current state.

If it is still pending, the original offer remains valid.

If it expired or was cancelled, the duplicate notification is ignored.

This is why the offer record must be durable and the notification must not be the source of truth.

# 9.18 Failure scenario 6 — Redis loses data, but the database survives

Let's say our Redis cluster is completely lost.

Initially:

```
Redis:
    D101 -> Location A
    D102 -> Location B
    D103 -> Location C

Database:
    D101 -> AVAILABLE
    D102 -> BUSY
    D103 -> AVAILABLE
```

After the Redis failure:

```
Redis:
    Empty
```

What do we do?

There are two different datasets to recover.

### A. Recover business state

The database still knows:

```
D101 -> AVAILABLE
D102 -> BUSY
D103 -> AVAILABLE
```

No business assignment data has been lost.

### B. Recover live location state

We have options:

Option 1: Drivers repopulate Redis.

As drivers reconnect or continue reporting, the index gradually fills.

This is often sufficient for ephemeral live location.

Option 2: Replay a retained location event stream.

If we have Kafka containing sufficiently recent location events, we can rebuild the latest location projection.

For each driver, process the latest valid event.

Option 3: Restore a snapshot and apply newer events.

We can restore a recent checkpoint and replay events after the checkpoint.

But there is a critical rule:

A restored coordinate is only eligible for matching if it still satisfies the freshness policy.

Do not make an old restored location appear fresh simply because Redis was restarted.

### Rebuild sequence

```
Redis Cluster Recovered
        |
        v
Rebuild Operational Metadata
        |
        v
Restore / Replay Latest Locations
        |
        v
Validate Session and Version
        |
        v
Populate GEO Index
        |
        v
Enable Matching for Fresh Drivers
```

We should gradually enable traffic rather than instantly exposing an incompletely rebuilt index.

### What about availability indexes?

Suppose:

```
geo:available-drivers
```

also disappeared.

We rebuild it by combining:

```
Fresh accepted location
        +
Authoritative business availability
        +
No active reservation
        +
No active assignment
```

A driver should enter the available GEO index only when these requirements are satisfied.

We must not simply populate GEO using every driver whose database status says `AVAILABLE`.

A driver with a week-old location isn't a nearby candidate.

# 9.19 Failure scenario 7 — Assignment database is unavailable

Suppose:

```
Redis = Healthy
WebSocket Gateways = Healthy
Matching = Healthy
Assignment Database = Down
```

D101 accepts an offer.

What should happen?

We cannot confirm the assignment.

Remember:

```
Redis != Assignment Authority
```

We should:

1. Keep the offer's durable state if it already exists.
2. Attempt the assignment transaction within the request deadline.
3. Return an uncertain or retryable response if the outcome cannot be established.
4. Reuse the same idempotency key on retry.
5. Never tell the customer or driver that the ride is assigned until the durable result is confirmed.

If we can still read the database after a timeout, query the existing operation using its idempotency key.

If we cannot establish whether the transaction committed, do not create a second independent assignment attempt.

The next retry must resolve the original logical operation.

### Should we queue the acceptance?

There are two possible API designs.

Synchronous assignment

The backend tries the durable transaction immediately.

If successful:

```
200 ACCEPTED
```

If not:

```
503 / RETRYABLE ERROR
```

The client retries with the same idempotency key.

This is a good default when assignment transactions are quick.

Asynchronous assignment

The backend durably stores an acceptance command and returns:

```
202 ACCEPTANCE_RECEIVED
```

A worker processes it.

But this response means only that the command was durably received.

It does NOT mean the ride was assigned.

The driver application must later receive the result.

This design is useful when assignment involves longer-running workflows, but it introduces additional state and latency.

Never return a successful assignment response merely because an acceptance command entered a queue.

# 9.20 Failure scenario 8 — One Redis partition is slow

Suppose our Redis architecture has 16 partitions.

```
P0 -> Healthy
P1 -> Healthy
P2 -> Slow
P3 -> Healthy
...
P15 -> Healthy
```

A matching request queries all partitions.

If we wait indefinitely for P2, a single slow partition can delay every ride request.

We need bounded parallel fan-out.

Example:

```
Overall search deadline = 100 ms

P0 -> 10 ms
P1 -> 12 ms
P2 -> 130 ms
P3 -> 15 ms
...
```

At 100 ms, we stop waiting for P2.

But we must be careful about the result.

The search is incomplete.

We shouldn't report:

```
Only 4 drivers nearby.
```

as though every partition was searched successfully.

Instead, our matching result should contain both:

```
Candidates found
Search coverage status
```

For example:

```
Candidates:
    D101, D103, D105

Search status:
    PARTIAL

Failed partitions:
    P2
```

Now dispatch can choose an appropriate policy:

- Retry P2 within the remaining deadline.
- Use a healthy secondary index.
- Proceed with partial candidates if business rules permit.
- Delay or fail matching if complete discovery is required.

This is especially important in supply-constrained areas.

A partial result may be good enough in a dense city but not in a rural area with only a few drivers.

### Avoid letting one partition exhaust all resources

Use:

- Per-partition concurrency limits.
- Request deadlines.
- Circuit breakers.
- Independent connection pools.
- Bulkheads.
- Partial-result policies.

A circuit breaker can stop repeatedly calling a dependency that is clearly unhealthy.

But it must also allow controlled recovery probes.

# 9.21 Failure scenario 9 — Outbox backlog grows

Suppose the assignment service continues creating events.

But the notification publisher is unavailable.

```
Database:
    1,000 pending outbox events
       |
       v
Publisher unavailable
```

What happens?

The outbox grows.

Initially this is fine because the events are durable.

But if the publisher stays unavailable:

```
1,000
10,000
100,000
1,000,000
```

Eventually, database storage and query performance can be affected.

We need operational limits.

Monitor:

- Number of unpublished events.
- Age of the oldest unpublished event.
- Publication retry count.
- Failed event count.
- Outbox processing throughput.
- Publisher health.

For time-sensitive offers, an outbox event may no longer be useful after its associated offer expires.

The publisher should check the current business state before attempting late delivery.

For example:

```
Offer O101:
    Status = EXPIRED

Outbox event:
    SEND_OFFER O101
```

The delivery publisher can skip the event because the offer is no longer valid.

This prevents stale work from accumulating in the system.

But don't blindly delete all old outbox records.

Maintain appropriate auditability and retention policies.

# 9.22 Dead-letter queues — What happens when a message repeatedly fails?

Suppose our notification consumer receives:

```
Event EVT-501
```

Every attempt fails due to malformed payload.

We retry:

```
Attempt 1 -> Failure
Attempt 2 -> Failure
Attempt 3 -> Failure
Attempt 4 -> Failure
```

Should we retry forever?

No.

After a configured number of attempts, move the message to a dead-letter queue or equivalent failure store.

Conceptually:

```
Main Queue
    |
    v
Consumer
    |
    v
Processing Failed
    |
    +---- Retry
    |
    +---- Retry
    |
    +---- Retry
    |
    v
Dead Letter Queue
```

But there's a critical distinction.

A dead-lettered message isn't automatically solved.

We need to:

- Alert operators.
- Preserve the original payload.
- Record the failure reason.
- Provide a controlled replay process.
- Check whether the associated business operation is still valid.
- Prevent replay from causing duplicate side effects.

### Should every failed offer go to a DLQ?

Not necessarily.

Suppose:

```
Offer O101 has expired.
```

There is no point retrying its delivery.

That's not a technical poison-message failure.

It's a business-expired task.

We should mark it as obsolete and stop processing.

DLQs are primarily for messages that cannot be processed correctly after reasonable retries and recovery.

# 9.23 Circuit breakers, bulkheads and load shedding

These are important production patterns.

## A. Circuit breaker

Imagine our Redis partition is failing repeatedly.

Without protection:

```
100,000 requests/sec
        |
        v
Repeated Redis timeouts
        |
        v
More connection pressure
        |
        v
More latency
```

A circuit breaker tracks failures.

It has three conceptual states:

```
CLOSED
   |
   | Too many failures
   v
OPEN
   |
   | Wait recovery interval
   v
HALF_OPEN
   |
   +---- Healthy probe ---> CLOSED
   |
   +---- Failed probe ----> OPEN
```

CLOSED: Requests are allowed.

OPEN: Calls are rejected quickly rather than repeatedly hitting the unhealthy dependency.

HALF_OPEN: A limited number of test requests are allowed to check recovery.

This prevents dependency failures from consuming all application resources.

## B. Bulkheads

Imagine our matching service serves:

```
Hyderabad
Bangalore
Mumbai
Delhi
```

If Hyderabad's Redis partitions become slow, Mumbai shouldn't lose all its search capacity.

We can isolate:

- Regional worker pools.
- Per-dependency connection pools.
- Per-partition concurrency.
- Per-region queues.

This is called the bulkhead pattern.

The idea comes from ships: one flooded compartment should not sink the entire ship.

## C. Load shedding

Suppose the location ingestion service receives 500,000 requests/sec but the live-location store can temporarily process only 300,000.

We shouldn't accept unlimited requests and allow memory usage to grow indefinitely.

We can:

- Reject excess requests with retryable responses.
- Coalesce pending updates by driver.
- Prioritize fresh location updates over obsolete ones.
- Apply per-driver and per-region rate limits.
- Protect active-trip tracking if its service requirements are stricter.
- Stop nonessential analytics processing during overload.

This is particularly useful because location data is continuously superseded.

We can often discard obsolete pending location values while preserving the newest accepted value.

### But don't apply location-style dropping to assignment commands

An assignment acceptance is not a superseded location update.

We cannot casually drop it.

The handling policy must depend on the operation's business meaning.

# 9.24 Backpressure — What happens when consumers are slower than producers?

Let's examine an important difference between direct Redis writes and Kafka.

Suppose:

```
Location producers = 300,000 updates/sec
Redis consumers = 200,000 updates/sec
```

If we use Kafka-first ingestion:

```
Kafka incoming rate = 300,000/sec
Kafka processing rate = 200,000/sec
```

The backlog grows.

The difference:

\\[ 300,000 - 200,000 = 100,000 \\]

We accumulate approximately 100,000 unprocessed records every second while the rates remain unchanged.

After one minute:

\\[ 100,000 \times 60 = 6,000,000 \\]

Six million additional events are waiting.

This is consumer lag.

### What can we do?

Option 1: Scale consumers.

Increase consumer capacity, subject to Kafka partition count and downstream Redis capacity.

Option 2: Coalesce latest-value events.

If only the latest position matters, obsolete queued updates for the same driver can be discarded or superseded.

For example:

```
D101:
    101
    102
    103
    104
```

If only sequence 104 is needed for live matching, processing all four isn't necessary.

However, ordinary Kafka consumers don't automatically coalesce messages.

We need an explicit compaction, stream-processing, or buffering strategy.

Option 3: Separate workloads.

Historical tracking may need every event.

Live matching may need only the newest value.

We can use different processing paths and retention policies.

Option 4: Apply admission control.

Slow or reject work when the downstream infrastructure is saturated.

### Important Kafka detail: Log compaction

Kafka supports log-compacted topics, where records with the same key can be compacted so that only the latest value is retained in the cleaned-up log representation.

For example:

```
Key D101:
    Location 101
    Location 102
    Location 103
```

Compaction may eventually retain:

```
D101 -> Location 103
```

But compaction is asynchronous.

Consumers can still observe older records before compaction occurs.

It is not an immediate latest-value queue.

Also, log compaction is inappropriate when every individual location event must be retained for history.

Choose the event retention policy according to whether intermediate locations have business value.

# 9.25 Reconciliation — How do we repair inconsistencies?

Even with transactions and idempotency, operational projections can drift.

For example:

```
Database:
    D101 = BUSY
    activeRide = R501

Redis available GEO:
    Contains D101
```

The database says the driver is busy.

Redis still contains them in the available index.

Or:

```
Database:
    Offer O101 = EXPIRED

Redis:
    Reservation O101 still exists
```

We need a mechanism to find and repair these inconsistencies.

This is called reconciliation.

## Example 1: Driver appears available in Redis but is busy in the database

A reconciliation worker periodically:

1. Reads a batch of drivers from the operational index.
2. Looks up their authoritative availability.
3. Removes drivers who are busy, reserved, offline, or ineligible.
4. Checks whether missing but eligible drivers should be reinserted, provided their locations remain fresh.

## Example 2: Orphaned reservations

Suppose:

```
Redis:
    D101 -> O101

Database:
    O101 = CANCELLED
```

The reservation is obsolete.

The reconciliation worker conditionally deletes it if it still belongs to O101.

It must not delete a newer reservation.

## Example 3: Ride and driver disagree

Suppose:

```
Ride:
    R501 -> D101

Driver:
    D101 -> AVAILABLE
```

This violates our operational invariant.

We should detect it.

But we should not arbitrarily change either side.

We need a repair workflow that reads assignment history and determines the correct state.

For example, the assignment record might show:

```
R501 -> D101
Status = IN_PROGRESS
```

Then the driver should be BUSY.

The reconciliation worker can repair the driver's operational status from that authoritative assignment information.

### Important principle

Reconciliation is not a substitute for transactional correctness.

It is a safety net for:

- Stale projections.
- Missed asynchronous events.
- Operational bugs.
- Infrastructure recovery.
- Manual corrections.

We should design the transactional path to prevent invalid business states in the first place.

# 9.26 Failure matrix — Test the entire architecture

Let's now put the failure-handling rules into one table.

This is especially useful when designing the system in an interview.

| Component             | Failure               | What happens                             | Recovery                                                   |
| --------------------- | --------------------- | ---------------------------------------- | ---------------------------------------------------------- |
| Mobile application    | Network lost          | Location/heartbeat stops                 | Retry latest location, reconnect with jitter               |
| Mobile application    | Duplicate update      | Same event received again                | Version-based idempotency                                  |
| Mobile application    | Old session message   | Delayed event arrives                    | Reject using session epoch                                 |
| Load balancer         | Instance failure      | Traffic can't reach one ingestion server | Health checks and rerouting                                |
| Location ingestion    | Server crash          | In-flight requests may be uncertain      | Client retries with same event identity                    |
| Redis                 | Primary crash         | Some recent updates may be lost          | Failover and refresh from drivers/events                   |
| Redis                 | Full outage           | Live location index unavailable          | Pause affected matching or use a healthy independent index |
| Kafka                 | Broker outage         | Durable event acceptance may fail        | Backpressure and retry                                     |
| Kafka consumer        | Crash                 | Messages redelivered                     | Idempotent consumer and offset recovery                    |
| Kafka consumer        | Lag                   | Live index becomes stale                 | Scale, coalesce where valid, enforce freshness             |
| Spatial index         | Stale entries         | Invalid candidates returned              | Freshness validation and reconciliation                    |
| Matching engine       | Crash                 | Matching attempt interrupted             | Recompute from current state                               |
| Connection registry   | Stale gateway mapping | Offer routed incorrectly                 | Lease expiry and connection refresh                        |
| WebSocket gateway     | Crash                 | Clients disconnect                       | Reconnect and state synchronization                        |
| Offer delivery        | Message lost          | Driver doesn't see offer                 | Retry same valid offer, fallback notification              |
| Dispatch service      | Crash                 | Pending offers remain                    | Recover from durable offer state                           |
| Timeout worker        | Duplicate execution   | Same expiration attempted twice          | Conditional idempotent transition                          |
| Assignment database   | Outage                | Cannot confirm assignment                | Fail closed; retry same acceptance operation               |
| Assignment API        | Response lost         | Client doesn't know result               | Idempotency lookup                                         |
| Outbox publisher      | Crash                 | Events remain pending                    | Resume publication                                         |
| Notification consumer | Duplicate event       | Repeated notification attempt            | Inbox/deduplication and client event ID                    |
| Regional network      | Partition             | Services cannot communicate              | Regional isolation and explicit failover policy            |

# 9.27 End-to-end failure simulations

Let's test three complete scenarios.

## Simulation A — Location loss, then recovery

Initial state:

```
D101:
    Available
    Location fresh
    Connected
```

Then:

```
10:00:00 -> Last location update

10:00:15 -> Location stale

10:00:16 -> Matching excludes D101

10:00:20 -> Cleanup removes stale GEO entry

10:00:25 -> Driver reconnects

10:00:26 -> New location accepted

10:00:26 -> GEO entry restored

10:00:26 -> D101 becomes eligible again
```

Notice:

- We didn't permanently delete the driver.
- We didn't mark them busy or assign them.
- We didn't use the old location.
- Recovery occurred automatically through a new valid update.

## Simulation B — Duplicate dispatch and duplicate acceptance

Initial:

```
R501 = SEARCHING

D101 = RESERVED_FOR_OFFER O101

O101 = PENDING
```

Driver accepts.

```
ACCEPT-991 -> Received
```

Database transaction:

```
R501 -> ASSIGNED to D101
D101 -> BUSY
O101 -> ACCEPTED
```

Response gets lost.

The driver retries:

```
ACCEPT-991 -> Received again
```

Backend checks the idempotency record.

Result:

```
Already processed.

Return previous success.
```

No second assignment is created.

Then the outbox publisher sends:

```
RIDE_ASSIGNED EVT-501
```

It crashes before marking it published.

The publisher retries.

The notification consumer sees the same event ID.

It deduplicates.

Meanwhile, cancellation events are also published for other pending offers.

Result: The driver is assigned once, even though several messages and requests were repeated.

## Simulation C — Redis outage during active service

Initial:

```
Redis:
    Healthy

Database:
    Healthy

Kafka:
    Healthy
```

Redis becomes unavailable.

Actions:

1. Location ingestion returns retryable failures for live indexing.
2. Drivers retain the latest location and retry.
3. Matching requests using the unavailable index are delayed or rejected.
4. Existing ride assignments remain intact in the database.
5. Active dispatch decisions already committed to the database are not lost.
6. When Redis recovers, fresh locations are repopulated.
7. If Kafka retains required location events, the index can also be replayed.
8. Stale locations remain excluded.
9. Matching gradually resumes.

Result: We lose some live matching availability, but we do not lose authoritative assignments.

This is a good example of prioritizing business correctness over pretending the entire service is healthy.

# 9.28 Different durability profiles for different products

Let's adapt our reliability decisions.

| Requirement                  | Uber/Ola/Rapido                | Swiggy/Zomato                | Logistics                 |
| ---------------------------- | ------------------------------ | ---------------------------- | ------------------------- |
| Latest location              | Ephemeral live state           | Ephemeral live state         | Often retained longer     |
| Active-trip location history | May be required                | May be required              | Often important           |
| Idle location history        | Can be sampled or discarded    | Can be sampled or discarded  | Policy-dependent          |
| Driver assignment            | Strong durability              | Strong durability            | Strong durability         |
| Offer delivery               | Retryable, time-limited        | Retryable, time-limited      | Retryable                 |
| Reassignment history         | Important                      | Important                    | Important                 |
| Heartbeat detection          | Aggressive enough for matching | Depends on delivery timeline | Often less aggressive     |
| Offline recovery             | Latest location refresh        | Latest location refresh      | May use delayed reporting |
| Audit trail                  | Business-dependent             | Order/dispute-dependent      | Often important           |

The key is that different business data requires different durability policies.

Do not apply a blanket rule such as:

```
Everything must go through Kafka.
```

Or:

```
Everything can be stored in Redis.
```

Both can create unnecessary complexity or unacceptable data loss.

# 9.29 Production design rules — A practical checklist

When building a production version, I would insist on these rules.

For location ingestion:

- Every location event has a stable identity.
- Session epoch and sequence number are validated atomically.
- The backend acknowledges only after the chosen acceptance boundary.
- Live updates can be superseded.
- Historical events follow a separate durability contract.
- Retry policies include jitter and deadlines.
- Old locations are never used for matching.

For dispatch:

- Every offer has a durable ID.
- Every offer has an expiry.
- Temporary reservations belong to a specific offer.
- Delivery retries are safe.
- A reconnect fetches current valid offers.
- A stale notification cannot revive an expired offer.

For assignment:

- Ride ownership is decided by one authoritative transactional database operation.
- A driver cannot have two active assignments.
- A ride cannot have two active assignments.
- Acceptance is idempotent.
- Expiration and acceptance are atomic competing transitions.
- The database commit happens before declaring assignment success.

For asynchronous processing:

- Use an outbox for reliable event publication.
- Use an inbox or equivalent deduplication for state-changing consumers.
- Expect duplicate events.
- Commit Kafka offsets after the downstream side effect, unless using an appropriate transactional integration.
- Monitor consumer lag.
- Make replay safe.

For operational reliability:

- Bound every queue.
- Bound retries.
- Apply backpressure.
- Isolate regions and dependencies.
- Use circuit breakers where appropriate.
- Monitor reconciliation errors.
- Test primary/replica failover.
- Practice recovery from complete Redis loss.

# 9.30 Final architecture after introducing reliability

Let's see how our architecture looks now.

\#chatgpt-mermaid-\_r_4hn\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_4hn\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_4hn\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_4hn\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_4hn\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4hn\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_4hn\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_4hn\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_4hn\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_4hn\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_4hn\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_4hn\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4hn\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4hn\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_4hn\_ p{margin:0;}#chatgpt-mermaid-\_r_4hn\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4hn\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4hn\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4hn\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_4hn\_ .label text,#chatgpt-mermaid-\_r_4hn\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4hn\_ .node rect,#chatgpt-mermaid-\_r_4hn\_ .node circle,#chatgpt-mermaid-\_r_4hn\_ .node ellipse,#chatgpt-mermaid-\_r_4hn\_ .node polygon,#chatgpt-mermaid-\_r_4hn\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_4hn\_ .rough-node .label text,#chatgpt-mermaid-\_r_4hn\_ .node .label text,#chatgpt-mermaid-\_r_4hn\_ .image-shape .label,#chatgpt-mermaid-\_r_4hn\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_4hn\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_4hn\_ .rough-node .label,#chatgpt-mermaid-\_r_4hn\_ .node .label,#chatgpt-mermaid-\_r_4hn\_ .image-shape .label,#chatgpt-mermaid-\_r_4hn\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_4hn\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_4hn\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4hn\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4hn\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_4hn\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_4hn\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_4hn\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_4hn\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_4hn\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_4hn\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_4hn\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4hn\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4hn\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_4hn\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4hn\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_4hn\_ .icon-shape,#chatgpt-mermaid-\_r_4hn\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_4hn\_ .icon-shape p,#chatgpt-mermaid-\_r_4hn\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_4hn\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_4hn\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_4hn\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_4hn\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_4hn\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_4hn\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_4hn\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_4hn\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_4hn\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4hn\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_4hn\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_4hn\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_4hn\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4hn\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_4hn\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_4hn\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4hn\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_4hn\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_4hn\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4hn\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_4hn\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4hn\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_4hn\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_4hn\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_4hn\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_4hn\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_4hn\_ .node rect,#chatgpt-mermaid-\_r_4hn\_ .node circle,#chatgpt-mermaid-\_r_4hn\_ .node ellipse,#chatgpt-mermaid-\_r_4hn\_ .node polygon,#chatgpt-mermaid-\_r_4hn\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_4hn\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_4hn\_ .mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_4hn\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_4hn\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4hn\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}Driver Mobile AppLocation IngestionRedis GEOKafka - Optional DurableLocation StreamHistorical StorageMatching EngineDispatch OrchestratorTransactional DatabaseOffers and ReservationsAssignmentsOutboxOutbox PublisherMessage Broker / InternalRouterWebSocket GatewaysAcceptance APITimeout WorkersReconciliation WorkersHeartbeat / LivenessOperational State

Here is the important separation:

Fast, replaceable state:

- Latest location.
- Geographical indexes.
- Connection registry.
- Temporary search candidates.

Durable business state:

- Driver reservation.
- Ride offers.
- Successful assignments.
- Assignment history.
- Required event history.

Asynchronous processing:

- Notification delivery.
- Historical location processing.
- Analytics.
- Cleanup.
- Reconciliation.

# Chapter 9 — The most important lessons

I want you to remember these principles rather than memorize technologies.

| Principle                                       | Why it matters                                    |
| ----------------------------------------------- | ------------------------------------------------- |
| Networks produce ambiguous outcomes             | A timeout doesn't mean the operation failed       |
| Retries produce duplicates                      | Make processing idempotent                        |
| Retries can create overload                     | Use backoff, jitter, deadlines, and retry budgets |
| Durable business state needs an authority       | Use transactional storage for assignments         |
| Dual writes can fail partially                  | Use transactional outbox                          |
| Event consumers can receive duplicates          | Use inbox/deduplication                           |
| Latest location is replaceable                  | Coalesce obsolete updates                         |
| Historical events may not be replaceable        | Use durable event ingestion when required         |
| Queues accumulate work                          | Monitor lag and apply backpressure                |
| Redis projections can drift                     | Reconcile against authoritative state             |
| Partial search results are not complete results | Track partition/query coverage                    |
| Stale locations are dangerous                   | Fail closed for location eligibility              |

Three especially important distinctions:

1\. Retry vs idempotency

Retries increase the chance of successful execution. Idempotency ensures repeated execution doesn't create repeated business effects.

2\. Durable event vs durable business state

Kafka can preserve an event, but an event is not automatically an authoritative assignment. The transactionally committed assignment is the business decision.

3\. Availability vs correctness

If Redis fails, we might temporarily stop matching. If the assignment database fails, we must not claim an assignment succeeded without confirming its durable state.

That is how we build a reliable system without demanding that every component be perfectly available.

# Next: Chapter 10 — Scaling the Entire System

We've solved the functional problems. Now we'll tackle what happens when this system grows to millions of drivers and enormous numbers of concurrent requests.

We'll evolve the design beyond one Redis cluster and a few application servers.

Topics:

1. Horizontal scaling of ingestion and WebSocket gateways.
2. Redis Cluster versus custom geographical sharding.
3. Driver-ID partitions versus H3/geographical partitions at scale.
4. Hot cities and overloaded geographical cells.
5. Dynamic partitioning and rebalancing.
6. Load balancing by CPU, connections, and geographic locality.
7. Kafka partition sizing and consumer scaling.
8. Matching-engine concurrency and query fan-out.
9. Preventing hotspots when thousands of customers request rides in one neighbourhood.
10. Regional isolation and multi-region deployments.
11. Cross-region location visibility.
12. Active-active vs active-passive architectures.
13. Database sharding for assignments.
14. Disaster recovery and regional failover.
15. Capacity planning and backpressure at millions of drivers.

We'll also build a complete example with:

```
1 million online drivers
100,000+ location updates/sec
Thousands of concurrent ride requests
Hundreds of thousands of WebSocket connections
Multiple cities
Multiple regions
```

And understand how to distribute ownership, data, connections, searches, and assignment decisions without introducing unnecessary global coordination.

The next important question is:

How do we scale location ingestion, nearby searches, and assignment independently instead of trying to scale the entire system as one giant service?

Say next to continue.
