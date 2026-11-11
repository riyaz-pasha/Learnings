# Chapter 8 — Database and Storage Architecture

So far, we've introduced several storage systems:

- Local memory inside push servers.
- Redis for connection ownership and routing.
- Kafka for durable event processing.
- A business database for authoritative application data.
- A delivery database or notification inbox for reliable delivery.

Now let's answer an important architectural question:

Does a push server need its own database? What should it store, what should it read, and which database should it connect to?

We'll derive the answer from the actual responsibilities of the push infrastructure.

## 1. First understand that we have different kinds of data

Consider our real-time push system.

We have:

```
                    Business Services
                           |
                           ▼
                     Kafka / Queue
                           |
                           ▼
                    Routing Workers
                           |
                           ▼
                    Push Infrastructure
                           |
                           ▼
                         Clients
```

Different components need different information.

For example:

- Push Server 1 needs to know which WebSocket connection belongs to User A.
- The routing worker needs to know which push server owns User A's connection.
- The chat service needs to know which messages belong to Conversation G1.
- The delivery worker needs to know whether Notification 101 has been acknowledged.
- Kafka needs to retain events for independent consumers.

Should all this data go into one database?

No.

Each type of data has a different access pattern, consistency requirement, lifetime, and owner.

Let's categorize them before choosing any database.

## 2. Five types of data in our architecture

| Data             | Example                                     | Lifetime                   | Primary owner                  |
| ---------------- | ------------------------------------------- | -------------------------- | ------------------------------ |
| Connection state | User A's socket belongs to Push 1           | Connection lifetime        | Push infrastructure            |
| Routing metadata | User A has connections on Push 1 and Push 2 | Short-lived                | Connection registry            |
| Business state   | Chat message 101                            | Business-defined retention | Chat service                   |
| Delivery state   | Notification 101 is pending acknowledgement | Until completion/retention | Delivery system                |
| Event history    | Order-created event 101                     | Configured retention       | Event streaming infrastructure |

These are not interchangeable.

Let's understand each one.

# Part A — What Should Live Inside Push Server Memory?

## 3. Actual WebSocket/SSE connections belong in local memory

Suppose:

```
Push Server 1:

User A -> conn-101
User B -> conn-102
User C -> conn-103
```

The actual connection references must remain inside Push Server 1.

For example:

```
Map<String, WebSocketSession> connections;
```

This gives the server direct access to its locally owned connections.

Why not Redis?

Because Redis cannot hold a live Java `WebSocketSession` object that another process can directly use to write to the underlying TCP socket.

The socket belongs to the operating system and process that established it.

Therefore:

Actual connections are local process state.

When Push Server 1 crashes, this state disappears.

That's expected.

We recreate it when clients reconnect.

### What else belongs in local memory?

Besides socket references, we may keep:

```
Local Connection State:

connectionId -> socket/session reference

userId -> Set<connectionId>

groupId -> Set<connectionId>

connectionId -> {
    authenticatedPrincipal,
    authorizationContext,
    deviceId,
    connectionState,
    lastHeartbeat
}
```

We can also maintain:

- Small temporary outbound buffers.
- Subscription metadata.
- Per-connection sequence/cursor state.
- Rate-limit counters.
- Protocol state.
- Compression/session state.

But there is a rule:

Don't use push-server memory as the only durable location for important information.

For example, if a client is slow and the push server has queued an important notification in memory, a process crash will destroy it.

Important messages must already exist in durable storage or have a recoverable source.

## 4. Should local connection information be replicated to another push server?

Generally, no.

Consider:

```
Push Server 1 owns Connection 101.
```

Should it continuously replicate the actual socket object or its local connection map to Push Server 2?

No.

There is no useful way for Push Server 2 to assume ownership of that same TCP connection after Push Server 1 crashes.

Instead:

1. Push Server 1 owns the actual connection.
2. A distributed registry advertises its ownership.
3. If Push Server 1 dies, the ownership becomes invalid.
4. The client reconnects to another server.
5. The new server creates a new connection.

We replicate metadata when necessary, not the live connection itself.

This is different from database replication.

# Part B — What Should Live in Redis?

## 5. Redis stores distributed, ephemeral connection metadata

Let's recall our mapping:

```
User A:
    conn-101 -> Push Server 1
    conn-102 -> Push Server 2

User B:
    conn-103 -> Push Server 2
```

A routing worker needs to find these connections.

It cannot inspect the local memory of every push server.

So we maintain a shared connection registry.

A typical Redis data model might logically represent:

```
USER -> CONNECTIONS

user:{userId}:connections


CONNECTION -> METADATA

user:{userId}:connection:{connectionId}


SERVER -> LEASE

server:{serverInstanceId}:lease


GROUP -> ACTIVE SERVERS

group:{groupId}:servers
```

We already discussed how these structures work in Chapter 5.

The important thing now is to understand the storage decision.

### Why Redis rather than PostgreSQL?

Let's compare the workloads.

Suppose a connection remains active for 30 minutes.

During that time, we might:

- Register it once.
- Refresh ownership periodically.
- Update subscription information.
- Remove it upon disconnection.

These are frequent, short-lived, operational updates.

A relational database can handle such writes, but its transaction, indexing, storage, and cleanup machinery may be unnecessary for this ephemeral workload.

Redis provides:

- Fast key-value access.
- Sets, hashes, and sorted sets.
- Atomic operations.
- Expiration.
- In-memory access.
- Clustering and replication options.

It is a natural choice for a distributed, temporary registry.

### Does Redis need persistence?

This is an important question.

The connection registry is reconstructible.

If Redis loses its data, push servers can re-register their active connections.

Therefore, we don't need to treat every connection registration as critical business data that must survive every restart.

However, we still need high availability and reasonable persistence/recovery configuration because losing the registry temporarily disrupts routing.

A practical approach:

- Use Redis replication and failover.
- Use an appropriate persistence policy for operational recovery.
- Make all registration operations idempotent.
- Reconstruct active registrations from push-server memory.
- Mark recovering servers as not fully routable until reconciliation completes.

Redis is an operational dependency, but it is not the authoritative source of business data.

# Part C — What Belongs in the Business Database?

## 6. Push servers should not own business data

Suppose we're building a chat application.

Alice sends a message to Bob.

The chat message is:

```
{
  "messageId": "msg-101",
  "conversationId": "conv-10",
  "senderId": "alice",
  "text": "Hello Bob!"
}
```

Where should this be stored?

In the chat service's database.

Not in Redis.

Not in the push server's local memory.

Not necessarily in the delivery database.

Why?

Because the chat service owns the meaning and lifecycle of the message.

It decides:

- Whether Alice is allowed to send it.
- Whether Bob belongs to the conversation.
- Whether the message should be persisted.
- Whether the message can be edited or deleted.
- How message history is queried.
- What retention rules apply.

The push infrastructure doesn't need to understand all these business rules.

It simply needs to deliver a notification or message payload to the connected client.

### Consider another example: Order tracking

An order service owns:

```
Order 123 -> SHIPPED
```

The push server should not independently maintain:

```
Order 123 -> SHIPPED
```

as an authoritative record.

Otherwise, we'd have two systems claiming ownership of the same business state.

That creates consistency problems.

Instead:

```
Order Service
      |
      ▼
Order Database
      |
      ▼
Outbox / Kafka
      |
      ▼
Push Infrastructure
      |
      ▼
Client
```

The push system delivers the update.

The order service remains responsible for authoritative order state.

### The general rule

| Business information     | Owner                            |
| ------------------------ | -------------------------------- |
| Chat messages            | Chat service                     |
| Orders and order status  | Order service                    |
| Payment transactions     | Payment service                  |
| Document contents        | Document service                 |
| Group membership         | Group/chat/collaboration service |
| User profiles            | User service                     |
| Notification preferences | Notification service             |

This is why a push service should generally not directly query every business database.

It creates unnecessary coupling and makes the push infrastructure harder to reuse.

# Part D — What About the Delivery Database?

## 7. Why do we sometimes need another database?

We already have a business database.

We already have Redis.

We already have Kafka.

Why introduce a delivery database?

Because some applications need to maintain a separate lifecycle for message delivery.

Consider a notification application.

A business service produces:

```
Your payment was successful.
```

The intended recipient is User A.

User A might have:

- A desktop browser.
- A mobile application.
- A tablet.

We need to know:

- Was the notification created?
- Which user should receive it?
- Is the notification still pending?
- Has it been delivered to the mobile application?
- Has the desktop acknowledged it?
- Should we retry?
- Has it expired?
- Has the user read it?

These are delivery-related concerns.

They don't necessarily belong in the payment service's database.

We can introduce a notification/delivery database.

### Example schema

Let's start with two tables.

Table 1: notifications

```
CREATE TABLE notifications (
    notification_id VARCHAR(100) PRIMARY KEY,
    user_id         VARCHAR(100) NOT NULL,
    event_id        VARCHAR(100) NOT NULL,
    notification_type VARCHAR(100) NOT NULL,
    payload         JSON,
    created_at      TIMESTAMP NOT NULL
);
```

This represents the actual notification in the user's inbox.

Table 2: notification_deliveries

```
CREATE TABLE notification_deliveries (
    delivery_id      VARCHAR(100) PRIMARY KEY,
    notification_id  VARCHAR(100) NOT NULL,
    recipient_id     VARCHAR(100) NOT NULL,
    device_id        VARCHAR(100),
    status           VARCHAR(30) NOT NULL,
    attempt_count    INTEGER DEFAULT 0,
    next_attempt_at  TIMESTAMP,
    acknowledged_at  TIMESTAMP
);
```

This tracks delivery attempts and acknowledgement.

For example:

notifications

| notification_id | user_id | type            |
| --------------- | ------- | --------------- |
| N101            | User A  | PAYMENT_SUCCESS |

notification_deliveries

| delivery_id | notification_id | device  | status       |
| ----------- | --------------- | ------- | ------------ |
| D101        | N101            | Desktop | ACKNOWLEDGED |
| D102        | N101            | Mobile  | PENDING      |

Now we can distinguish:

- The notification exists.
- Desktop received it.
- Mobile hasn't acknowledged it yet.

### Do we always need both tables?

No.

For many applications, a single notification table with read/delivery metadata is sufficient.

For large systems with independent per-device delivery, retries, and audit requirements, separate delivery records are useful.

Don't create separate tables merely to make the architecture look sophisticated.

## 8. Does a chat application need a separate delivery database?

Not necessarily.

This is an important distinction.

Suppose Alice sends Bob a chat message.

The chat message is stored:

```
messages:

msg-101
conversationId = conv-10
sender = Alice
text = Hello Bob!
```

When Bob reconnects, he can retrieve messages after his last processed sequence.

We don't necessarily need:

```
delivery_records:

msg-101 -> Bob
msg-102 -> Bob
msg-103 -> Bob
```

for every chat message.

Instead, we might maintain a per-device cursor:

```
device-sync-state:

User B / Device 1:
    conversation G1 -> sequence 105

User B / Device 2:
    conversation G1 -> sequence 101
```

When a device reconnects, it retrieves the missing messages.

This can be much more efficient than tracking millions of individual delivery records.

### When is a separate delivery record worthwhile?

When the product explicitly requires:

- Delivery acknowledgement.
- Per-device delivery status.
- Individual retry scheduling.
- Auditability.
- Escalation when a notification remains unacknowledged.
- Delivery through several channels.

For example, enterprise notifications might need WebSocket, mobile push, email, and escalation workflows.

Then a dedicated delivery system makes sense.

# Part E — Should Push Servers Connect Directly to a Database?

## 9. The two possible designs

This is a decision worth understanding carefully.

### Design A: Push server connects directly to business databases

Imagine:

```
                 Push Server
                      |
          ┌───────────┼───────────┐
          |           |           |
          ▼           ▼           ▼
       Chat DB     Order DB    Payment DB
```

The push server queries different business databases to obtain message payloads.

For example:

```
Notification arrives:

userId = user-A
messageId = msg-101
orderId = order-123
```

The push server:

1. Queries the chat database for `msg-101`.
2. Queries the order database for `order-123`.
3. Constructs the payload.
4. Sends it over WebSocket.

What problems does this create?

Problem 1: Tight coupling

The push server needs to understand different business domains.

Problem 2: Database connection explosion

Suppose:

- 100 push servers.
- Each opens 50 connections to the chat DB.
- Each also opens 50 connections to the order DB.

That is potentially:

```
100 × 50 = 5,000 connections per database
```

The push infrastructure can overwhelm business databases.

Problem 3: Business logic duplication

The push server might need to understand authorization, visibility, filtering, and data transformations.

These belong in business services.

Problem 4: Failure coupling

A chat database outage could affect the push server even when it is delivering unrelated order notifications.

This is undesirable.

### Design B: Business services prepare events; push servers deliver

Instead:

```
                  Chat Service
                       |
                       ▼
                   Chat DB
                       |
                       ▼
                 Chat Event
                       |
                       ▼
                    Kafka
                       |
                       ▼
                 Routing Layer
                       |
                       ▼
                  Push Server
                       |
                       ▼
                    Client
```

The event contains the information needed for delivery or a reference that can be resolved through a designated service.

For example:

```
{
  "eventId": "evt-101",
  "type": "NEW_CHAT_MESSAGE",
  "recipientId": "user-A",
  "payload": {
    "messageId": "msg-101",
    "conversationId": "conv-10",
    "text": "Hello Bob!"
  }
}
```

The push server doesn't need to query the chat database.

It receives an already prepared delivery payload.

### What if the payload is too large?

We have two options.

Option 1: Send a lightweight reference

```
{
  "type": "NEW_MESSAGE",
  "messageId": "msg-101",
  "conversationId": "conv-10"
}
```

The client fetches the message from the chat API.

Option 2: Let an application-owned payload service resolve it

The routing or delivery layer can obtain the required payload through a defined interface, rather than giving every push server direct database access.

For example:

```
Chat Event
    |
    ▼
Delivery Worker
    |
    ▼
Chat API
    |
    ▼
Prepared Payload
    |
    ▼
Push Server
```

The choice depends on payload size, latency, and the application's authorization model.

### My recommendation

For a general-purpose push platform:

Push servers should normally not connect directly to domain databases.

Keep them focused on:

- Connection lifecycle.
- Authentication context.
- Subscription management.
- Local routing.
- Serialization/transport.
- Heartbeats.
- Backpressure.
- Delivery acknowledgements.

They may connect to infrastructure stores such as Redis or a node-dispatch service.

But business persistence and domain queries should remain with the owning services.

# Part F — Which Database Is Suitable?

## 10. Database selection should follow the access pattern

Let's compare databases specifically for this workflow.

We are not choosing a database for the entire application.

We are choosing for:

1. Connection registry.
2. Delivery state.
3. Notification inbox.
4. Chat/event history.
5. Session and device metadata.

### A. Redis — Connection registry

Redis is a strong choice for:

- User-to-connection mappings.
- Server ownership metadata.
- Connection leases.
- Group-to-active-server indexes.
- Temporary presence information.
- Short-lived routing caches.

Why?

Its data structures fit the required operations.

For example:

```
user-A -> Set<connectionId>

connectionId -> Metadata

serverId -> Set<connectionId>
```

We need high-speed lookup, updates, and expiration.

Redis is a natural fit.

Don't use it as the only durable storage for important business messages.

### B. PostgreSQL / MySQL — Delivery records and inbox

PostgreSQL and MySQL are good starting choices for:

- Notification inbox.
- Delivery records.
- Retry scheduling.
- Acknowledgement status.
- Outbox tables.
- Device synchronization cursors.
- Audit information.

Why?

These workflows frequently need:

- Transactions.
- Unique constraints.
- Conditional updates.
- Indexes.
- Relationships.
- Queries by recipient and status.

For example:

```
SELECT *
FROM notification_deliveries
WHERE recipient_id = 'user-A'
  AND status = 'PENDING'
ORDER BY next_attempt_at
LIMIT 100;
```

We can implement safe work claiming with transactions and row locking.

For example, PostgreSQL supports `FOR UPDATE SKIP LOCKED`, which is useful when multiple delivery workers claim work concurrently.

Conceptually:

```
BEGIN;

SELECT delivery_id
FROM notification_deliveries
WHERE status = 'PENDING'
  AND next_attempt_at <= NOW()
ORDER BY next_attempt_at
LIMIT 100
FOR UPDATE SKIP LOCKED;

-- Mark selected records IN_PROGRESS.

COMMIT;
```

This lets multiple workers claim different records without intentionally claiming the same locked rows.

In production, use a lease/claim expiration and conditional completion so crashes don't permanently strand records.

### C. DynamoDB — Large-scale key-value delivery state

Amazon DynamoDB can be attractive when:

- Traffic is very high.
- Access patterns are predictable.
- We primarily retrieve records by partition key.
- We need managed horizontal scaling.
- We can design around partition-key access.

For example:

```
PK = USER#user-A

SK = NOTIFICATION#notification-101
```

Or:

```
PK = DELIVERY#del-101
```

But remember:

Database design depends on access patterns.

If we need:

```
Find all pending deliveries due for retry
```

we should design an appropriate index or a separate work-discovery mechanism.

A key-value database doesn't automatically make arbitrary filtering efficient.

### D. Cassandra / ScyllaDB — High-volume history and wide-column workloads

Apache Cassandra and ScyllaDB may be suitable for very large, write-heavy histories.

Examples:

- Chat history.
- Append-heavy event records.
- Per-user timelines.
- High-volume device synchronization state.

But you need to model queries in advance.

For example:

```
Partition key: conversationId

Clustering key: messageSequence
```

This supports retrieving a conversation's messages in sequence.

It is less attractive when you need arbitrary relational joins or complex cross-entity transactional updates.

### E. Kafka — Event history, not a replacement for a business database

Kafka retains event records for configured periods and allows consumers to replay them.

It is useful for:

- Domain events.
- Delivery workflows.
- Event processing.
- Multiple independent consumer groups.

But it isn't automatically your notification inbox or chat database.

The retention policy may remove older records.

It also doesn't naturally support the same arbitrary point queries as a database.

### Comparison

| Requirement                     | Good starting choice                                       |
| ------------------------------- | ---------------------------------------------------------- |
| Active connection registry      | Redis                                                      |
| Presence cache                  | Redis                                                      |
| Notification inbox              | PostgreSQL/MySQL or DynamoDB                               |
| Delivery status and retry state | PostgreSQL/MySQL or DynamoDB                               |
| Transactional Outbox            | Same relational DB as business transaction                 |
| High-volume chat history        | PostgreSQL initially; Cassandra/ScyllaDB at suitable scale |
| Durable event pipeline          | Kafka                                                      |
| Temporary socket/session state  | Push-server memory                                         |

Don't introduce five databases on day one.

For many systems, a perfectly reasonable initial stack is:

```
PostgreSQL + Redis + Kafka
```

And even Kafka may not be necessary in a smaller application.

# Part G — How Should Push Servers Perform Database Operations?

## 11. Minimize the number of operations on the connection hot path

Remember that push servers may maintain hundreds of thousands of active connections.

We should be careful about what happens for each connection.

Imagine a user connects.

A naive implementation might do:

```
1. Authenticate user by querying database.

2. Insert connection row.

3. Update user presence.

4. Insert device activity record.

5. Update session last seen.

6. Update several subscription records.

7. Query business permissions.

8. Establish WebSocket.
```

Now multiply this by thousands of simultaneous connections.

The connection establishment path becomes database-heavy.

Instead, think in terms of essential versus non-essential operations.

### Essential operations

To establish a valid connection, we generally need:

- Authentication.
- Required authorization.
- Local connection creation.
- Connection ownership registration.

### Operations that can often be asynchronous

Depending on requirements:

- Analytics events.
- Connection history.
- Non-critical last-seen timestamps.
- Usage metrics.
- Audit enrichment.
- Presence notifications.

For example:

```
              Connection Establishment
                        |
             ┌──────────┴──────────┐
             |                     |
             ▼                     ▼
        Essential Path       Asynchronous Events
             |                     |
             ▼                     ▼
       Local Connection       Event Stream
             |                     |
             ▼                     ▼
       Redis Registry         Analytics / Audit
```

We don't need to synchronously update an analytics database for every connection heartbeat.

### A useful optimization

Don't perform a database write every time a heartbeat arrives.

A 30-second connection heartbeat from one million connections could generate a very large number of writes.

Keep connection heartbeat state in the appropriate ephemeral infrastructure.

Persist only the information the application actually needs.

For example, updating a user's `last_seen_at` every 30 seconds is usually unnecessary.

We might update it only when:

- The user disconnects.
- A periodic aggregation interval passes.
- An important session transition occurs.

## 12. Connection registration must be safe under concurrency

Let's revisit one specific operation.

Suppose User A establishes a connection.

We need to update:

```
User A -> conn-101

Connection conn-101 -> Push 1
```

What if the push server crashes after updating only one of these structures?

For example:

```
Step 1: Add conn-101 to User A's connection set.

Step 2: Crash before creating conn-101 metadata.
```

Now the registry contains a dangling connection ID.

Or:

```
Step 1: Create conn-101 metadata.

Step 2: Crash before adding it to User A's connection set.
```

Now the record exists but cannot be found through the user index.

We need to think about atomicity.

### Approach A: Atomic registration in Redis

When the related keys share the same Redis Cluster hash slot, use a Lua script or transaction to update them atomically.

Conceptually:

```
REGISTER CONNECTION:

1. Validate server generation.

2. Create connection metadata.

3. Add connection ID to user's connection index.

4. Update appropriate lease.

5. Return success.
```

If one operation fails, the script should not leave the logical registration half-complete.

### Approach B: Treat indexes as rebuildable

Some indexes may live on different Redis Cluster shards.

In that case, we might not be able to update every structure atomically.

We can:

1. Write authoritative connection metadata first.
2. Update secondary reverse indexes asynchronously.
3. Periodically reconcile those indexes.
4. Make reads verify the actual connection metadata and server lease.

The exact choice depends on which queries need strong consistency.

### A useful principle

Not every index has to be perfectly synchronized at every instant.

But we must identify:

- Which structure is authoritative.
- Which structures are derived indexes.
- What happens if an index is temporarily stale.
- How the index is rebuilt.

For example, a stale server-to-connections index might cause extra cleanup work, but should not cause an unrelated user's connection to be deleted.

# Part H — Designing Delivery Database Operations Correctly

## 13. A delivery worker must safely claim work

Suppose we have 10 delivery workers.

They all query:

```
SELECT *
FROM notification_deliveries
WHERE status = 'PENDING';
```

Imagine there are 100 pending records.

All 10 workers might retrieve the same records.

Now the same notification could be dispatched multiple times unnecessarily.

We need a safe claiming mechanism.

### Use atomic work claiming

A worker should claim records inside a database transaction.

For PostgreSQL, a common pattern is:

```
BEGIN;

WITH claimed AS (
    SELECT delivery_id
    FROM notification_deliveries
    WHERE status = 'PENDING'
      AND next_attempt_at <= NOW()
    ORDER BY next_attempt_at
    LIMIT 100
    FOR UPDATE SKIP LOCKED
)
UPDATE notification_deliveries d
SET status = 'IN_PROGRESS',
    worker_id = 'worker-1',
    lease_expires_at = NOW() + INTERVAL '60 seconds',
    attempt_count = attempt_count + 1
FROM claimed
WHERE d.delivery_id = claimed.delivery_id
RETURNING d.*;

COMMIT;
```

What happens?

Imagine Worker 1 claims the first 100 records.

It locks those rows while claiming them.

Worker 2 skips the locked records and claims another batch.

```
100 pending deliveries

Worker 1 -> D1 ... D100

Worker 2 -> D101 ... D200
```

They don't intentionally process the same claimed work.

### What happens when Worker 1 crashes?

Its records remain `IN_PROGRESS`.

We need a recovery process:

```
SELECT delivery_id
FROM notification_deliveries
WHERE status = 'IN_PROGRESS'
  AND lease_expires_at < NOW();
```

Expired claims can be made eligible for another attempt.

But remember our old ownership principle.

The old worker must not be allowed to complete a delivery after another worker has reclaimed it.

So increment a claim generation or use a unique claim token.

For example:

```
deliveryId = D101

workerId = worker-1
generation = 5
```

After reclamation:

```
deliveryId = D101

workerId = worker-2
generation = 6
```

Worker 1's delayed completion must be rejected.

Conceptually:

```
UPDATE notification_deliveries
SET status = 'ACKNOWLEDGED'
WHERE delivery_id = 'D101'
  AND worker_id = 'worker-2'
  AND claim_generation = 6;
```

A more complete implementation would also validate the allowed state transition and ownership token.

### Why is this important?

The same pattern applies to:

- Notification delivery.
- Background event processing.
- Scheduled retries.
- Fanout jobs.
- Any distributed worker system.

A database record shouldn't become permanently stuck because its original worker crashed.

## 14. How should delivery status be updated?

Let's say a worker claims:

```
D101 -> IN_PROGRESS
```

It dispatches to Push Server 1.

The push server acknowledges that it accepted the request.

We update:

```
D101 -> SENT
```

Later, the client sends an ACK.

We update:

```
D101 -> ACKNOWLEDGED
```

These transitions should be conditional and idempotent.

For example:

```
UPDATE notification_deliveries
SET status = 'SENT',
    sent_at = NOW()
WHERE delivery_id = 'D101'
  AND status = 'IN_PROGRESS';
```

And:

```
UPDATE notification_deliveries
SET status = 'ACKNOWLEDGED',
    acknowledged_at = NOW()
WHERE delivery_id = 'D101'
  AND status IN ('SENT', 'IN_PROGRESS');
```

The exact permitted transitions depend on your workflow.

For example, a late ACK may arrive after a retry has already started.

We should generally allow a valid ACK to complete the logical delivery even if another attempt is underway, while making sure stale workers cannot overwrite the final result.

### Avoid unnecessary row updates

Don't update a delivery record every time a message is written to a socket or every time a heartbeat occurs.

Persist meaningful lifecycle transitions.

For very high throughput, maintain aggregate metrics separately rather than generating excessive per-event updates.

# Part I — Indexing and Partitioning the Delivery Database

## 15. How do we query pending work efficiently?

Let's imagine:

```
notification_deliveries
```

contains 500 million historical delivery records.

A query like this becomes problematic without appropriate indexing:

```
SELECT *
FROM notification_deliveries
WHERE status = 'PENDING';
```

We need to design indexes based on access patterns.

Common queries:

Query A: Find pending deliveries ready for retry

```
SELECT *
FROM notification_deliveries
WHERE status = 'PENDING'
AND next_attempt_at <= NOW();
```

Query B: Find deliveries for one recipient

```
SELECT *
FROM notification_deliveries
WHERE recipient_id = 'user-A'
ORDER BY created_at DESC;
```

Query C: Find all device deliveries for one notification

```
SELECT *
FROM notification_deliveries
WHERE notification_id = 'N101';
```

Possible indexes:

```
CREATE INDEX idx_delivery_retry
ON notification_deliveries (
    next_attempt_at,
    delivery_id
)
WHERE status = 'PENDING';
```

```
CREATE INDEX idx_delivery_recipient
ON notification_deliveries (
    recipient_id,
    created_at DESC
);
```

```
CREATE INDEX idx_delivery_notification
ON notification_deliveries (
    notification_id
);
```

For PostgreSQL, the first example is a partial index that contains only pending deliveries.

This can reduce index size if the pending population is much smaller than the total historical population.

The exact indexes should be validated against actual query plans and workload measurements.

## 16. When do we need database partitioning?

Suppose a notification platform stores:

```
500 million delivery records.
```

Eventually, a single table may become difficult to manage.

We might partition by time:

```
notification_deliveries:

    Partition 2026-10
    Partition 2026-11
    Partition 2026-12
```

This is useful when:

- Records have predictable retention.
- We frequently remove old records.
- Queries are time-bounded.
- Historical data is accessed less frequently.

For example, deleting an old partition can be much cheaper than deleting millions of rows individually.

Alternatively, we can partition based on recipient identifiers when the main access pattern is per-user lookup.

The choice depends on:

- Query patterns.
- Write distribution.
- Retention.
- Database capabilities.
- Whether one partition key becomes hot.

Don't partition merely because the table is large.

Partitioning is useful when it improves a specific operational or query problem.

### What about chat history?

A typical chat history query is:

```
Get messages for conversation G1
after sequence 105
```

A suitable relational index might be:

```
CREATE INDEX idx_messages_conversation_sequence
ON messages (conversation_id, sequence_number);
```

This allows efficient ordered retrieval.

At sufficiently large scale, a partitioned relational database or wide-column store may be more appropriate.

But we don't need to start with a distributed database just because the application uses WebSockets.

# Part J — What Should We Store: Full Payload or Reference?

## 17. This decision affects the entire architecture

Suppose a message contains:

```
{
  "messageId": "msg-101",
  "sender": "Alice",
  "text": "Hello Bob!",
  "attachments": [
    "image-1",
    "image-2"
  ],
  "metadata": {
    "conversationId": "conv-10",
    "createdAt": "..."
  }
}
```

Should Kafka contain the complete message?

Should the delivery database contain it again?

Should every push server fetch it?

There are several approaches.

### Option A: Full event payload

The event contains all information required for delivery.

```
{
  "eventId": "evt-101",
  "userId": "bob",
  "type": "NEW_MESSAGE",
  "payload": {
    "messageId": "msg-101",
    "text": "Hello Bob!"
  }
}
```

Flow:

```
Business Service
      |
      ▼
Kafka (Full Payload)
      |
      ▼
Router
      |
      ▼
Push Server
      |
      ▼
Client
```

Advantages:

- Fewer database lookups.
- Lower delivery latency.
- Consumers can operate independently.

Disadvantages:

- Larger events.
- Payload duplication.
- Sensitive data may be replicated into more systems.
- Schema changes affect consumers.

Good when event payloads are reasonably small and self-contained.

### Option B: Event reference

Kafka stores:

```
{
  "eventId": "evt-101",
  "messageId": "msg-101",
  "userId": "bob",
  "type": "NEW_MESSAGE"
}
```

A designated service retrieves the actual message when needed.

```
Kafka
  |
  ▼
Router / Payload Resolver
  |
  ▼
Chat API / Database
  |
  ▼
Prepared Payload
  |
  ▼
Push Server
```

Advantages:

- Smaller events.
- Less duplicated data.
- Easier to keep large business objects in their authoritative storage.

Disadvantages:

- Additional network calls.
- Extra latency.
- Payload lookup can become a bottleneck.
- The referenced record must remain available for as long as the event is needed.

### Option C: Hybrid

This is often a sensible choice.

Kafka contains:

- Event ID.
- Recipient or audience identifier.
- Event type.
- Sequence number.
- Small delivery payload.
- Reference to larger business data.

For example:

```
{
  "eventId": "evt-101",
  "eventType": "NEW_MESSAGE",
  "recipientId": "bob",
  "conversationId": "conv-10",
  "sequence": 101,
  "payload": {
    "messageId": "msg-101",
    "preview": "Hello Bob!"
  }
}
```

The client receives the preview and can fetch attachments or larger content from the chat API.

### My recommendation

Don't blindly copy full business objects into every intermediate system.

Keep event payloads as small as reasonably practical while still avoiding unnecessary synchronous lookups.

Also consider data sensitivity: every additional copy of a message increases the systems that must be secured, retained, and governed.

# Part K — What If the Push Server Needs Some Persistent State?

## 18. Is a completely stateless push server always correct?

Not necessarily.

We should distinguish business state from transient operational state.

A push server might maintain:

```
Local subscriptions:

Connection 101 -> Group G1
Connection 102 -> Group G2

Connection 103 -> Document D1
```

This is useful local state.

If the server crashes, it can reconstruct its subscriptions when clients reconnect.

But what about a system where clients remain connected for a long time and subscriptions are expensive to reconstruct?

We may persist a subscription record externally.

For example:

```
connectionId -> {
    userId,
    subscribedGroups,
    authorizedResources
}
```

However, the push server still needs to revalidate that the connection exists and that the authorization is current.

It must not trust old subscription records indefinitely.

### Another example: Real-time collaboration

A collaboration gateway may maintain:

- Current document subscriptions.
- Temporary operational buffers.
- Per-document sequence tracking.
- In-memory fanout state.
- Short-lived synchronization sessions.

These are legitimate responsibilities.

But document contents and durable operations still belong to the collaboration service or its authoritative storage.

This brings us to the distinction between two gateway designs.

# Part L — Thin Gateway vs Stateful Gateway

## 19. Pattern A: Thin Push Gateway

A thin gateway primarily manages transport.

```
             Business Services
                    |
                    ▼
                Kafka
                    |
                    ▼
              Routing Workers
                    |
                    ▼
               Push Servers
                    |
                    ▼
                  Clients
```

Its responsibilities:

- Accept connections.
- Authenticate connection requests.
- Maintain local sockets.
- Maintain connection registry metadata.
- Manage subscriptions where required.
- Send and receive transport messages.
- Handle heartbeats.
- Manage bounded outbound buffers.
- Dispatch acknowledgements.
- Handle disconnections.

It does not own business data.

### Advantages

- Easier to scale horizontally.
- Easy to replace failed instances.
- Independent of business-domain database schemas.
- Reusable by multiple business services.
- Easier to reason about operational failures.

### Disadvantages

- Some routing and domain-specific processing must happen elsewhere.
- Additional service-to-service communication may be required.

This is my default recommendation for a general-purpose push platform.

## 20. Pattern B: Stateful application-specific gateway

Imagine a collaborative editing platform.

We might have:

```
          Collaboration Gateway
                    |
          ┌─────────┼─────────┐
          |         |         |
          ▼         ▼         ▼
       Document   Document  Document
        State      State     State
          1          2         3
```

A gateway might handle some collaboration-specific transient work.

For example:

- Maintaining active editing sessions.
- Tracking temporary operation buffers.
- Applying protocol-specific sequencing.
- Maintaining ephemeral presence.
- Batching operations.
- Performing collaboration fanout.

It is still not automatically the authoritative database for the document.

A durable document state might be persisted by the document service.

### Advantages

- Lower latency for application-specific processing.
- Reduced network hops.
- Natural locality for collaborative sessions.

### Disadvantages

- More complex ownership.
- Larger process memory requirements.
- More difficult recovery.
- Tighter application-specific coupling.
- More complicated deployments.

### Comparison

| Dimension              | Thin gateway                   | Stateful gateway                                     |
| ---------------------- | ------------------------------ | ---------------------------------------------------- |
| Primary responsibility | Connection transport           | Transport + selected application logic               |
| Business DB access     | Usually none                   | Sometimes through explicit service interfaces        |
| Local state            | Connections and subscriptions  | Connections plus transient application state         |
| Horizontal scaling     | Relatively simple              | Requires more careful state distribution             |
| Crash recovery         | Reconnect and restore metadata | May require restoring session/application state      |
| Suitable for           | Generic push platforms         | Collaboration, gaming, specialized real-time systems |

An important rule:

Stateful doesn't mean that every server should own a database.

It means the process maintains additional application-specific state, which may still be stored externally or reconstructed from durable sources.

# Part M — Three Complete Storage Architectures

## 21. Architecture 1: Simple live-update system

Requirements:

- Users receive live updates.
- Missing intermediate events is acceptable.
- Latest application state is available elsewhere.
- No delivery acknowledgement is required.

Example: live dashboard.

```
                  Dashboard Service
                         |
                         ▼
                    State Store
                         |
                         ▼
                    Redis Pub/Sub
                         |
              ┌──────────┼──────────┐
              |          |          |
              ▼          ▼          ▼
           Push 1     Push 2     Push 3
              |          |          |
              ▼          ▼          ▼
            Clients    Clients    Clients
```

Storage choices:

| Data                     | Storage              |
| ------------------------ | -------------------- |
| Actual connections       | Push-server memory   |
| Connection directory     | Redis                |
| Latest dashboard state   | Redis or business DB |
| Live update events       | Redis Pub/Sub        |
| Delivery acknowledgement | Not required         |

If a client misses updates, it retrieves the latest state.

No dedicated delivery database is necessary.

## 22. Architecture 2: Reliable chat system

Requirements:

- Messages must be durable.
- Group conversations.
- Ordered messages.
- Multiple devices.
- Offline recovery.

```
                  Chat Service
                       |
               ┌───────┴────────┐
               |                |
               ▼                ▼
          Chat Database   Outbox Table
               |                |
               |                ▼
               |          Outbox Publisher
               |                |
               |                ▼
               |              Kafka
               |                |
               |                ▼
               |         Chat Routing Workers
               |                |
               |                ▼
               |         Connection Registry
               |                |
               |                ▼
               |          Push Servers
               |                |
               ▼                ▼
          Chat History       WebSocket
                                 |
                                 ▼
                               Client
```

Storage choices:

| Data                   | Storage                            |
| ---------------------- | ---------------------------------- |
| Actual connection      | Push-server memory                 |
| Connection directory   | Redis                              |
| Group membership       | Chat database                      |
| Actual chat messages   | Chat database                      |
| Domain events          | Kafka                              |
| Per-device sync cursor | Chat database or suitable KV store |
| Presence               | Redis                              |
| Separate delivery DB   | Optional                           |

When a user reconnects, the client fetches missing messages using its last processed sequence.

We do not have to maintain a separate durable delivery attempt for every message if the chat history and synchronization cursor already meet the requirements.

## 23. Architecture 3: Enterprise notification system

Requirements:

- Individual notifications.
- Multiple devices.
- Delivery acknowledgement.
- Retry policies.
- Notification history.
- Offline recovery.

```
                Business Services
                       |
                       ▼
                  Business DB
                       |
                       ▼
                  Outbox Events
                       |
                       ▼
                     Kafka
                       |
                       ▼
               Notification Router
                       |
                       ▼
               Notification / Inbox DB
                       |
                       ▼
                 Delivery Work Queue
                       |
                       ▼
                 Delivery Workers
                       |
                       ▼
                Connection Registry
                       |
                       ▼
                  Push Servers
                       |
                       ▼
                     Clients
                       |
                       ▼
                   ACK Handler
                       |
                       ▼
               Delivery / Inbox DB
```

Storage choices:

| Data                        | Storage                     |
| --------------------------- | --------------------------- |
| Connection information      | Redis                       |
| Business state              | Domain database             |
| Event history               | Kafka                       |
| Notification inbox          | PostgreSQL/DynamoDB         |
| Per-device delivery records | PostgreSQL/DynamoDB         |
| Retry work                  | Queue or delivery scheduler |
| Actual connections          | Push-server memory          |

Here, a separate delivery store is justified because we have independent notification and delivery lifecycles.

# Part N — How Should We Scale Database Access?

## 24. Database connection pools

Imagine we have:

```
200 Push Servers
```

Each server creates a connection pool of:

```
50 DB connections
```

Then:

\\[ 200 \times 50 = 10,000 \\]

database connections.

That's potentially unnecessary, especially when push servers are not supposed to perform heavy domain queries.

This is another reason to separate the connection gateway from business services.

### Recommended approach

- Push servers maintain a small Redis client pool appropriate to their workload.
- Routing workers use bounded database and Redis pools.
- Delivery workers use bounded database connections.
- Business services own their database connection pools.
- Heavy read operations happen in purpose-built services.

Don't let a large number of push servers independently create massive pools against one business database.

## 25. Batch operations instead of N+1 operations

Suppose we need to route a group message to 10,000 users.

Bad approach:

```
For every user:
    Query Redis.
    Get connection metadata.
    Find push server.
```

That's 10,000 separate lookups.

Better:

```
1. Resolve member IDs.

2. Batch connection lookups.

3. Filter invalid connections.

4. Group connections by push server.

5. Dispatch once per server.
```

If the group is enormous, use a group-to-active-server index instead.

Similarly, when fetching pending notifications, retrieve them in bounded batches instead of querying one record at a time.

## 26. Caching database reads

Not every lookup needs a database round trip.

For example, group membership might be cached:

```
group:G1 -> [User A, User B, User C]
```

But group membership changes.

Therefore, we need:

- Invalidation events.
- Membership versions.
- Appropriate cache expiry.
- Revalidation for sensitive operations.

For connection ownership, Redis itself already acts as the low-latency distributed lookup layer.

Adding another cache on top should be justified by measured traffic, not assumed to be beneficial.

# Part O — Database Failure and Data Recovery

## 27. What if the delivery database becomes unavailable?

Suppose notification records are stored in PostgreSQL.

The database becomes unavailable.

What should happen?

```
Notification Service
       |
       ▼
Kafka
       |
       ▼
Router
       |
       ▼
Delivery Database
       X
    UNAVAILABLE
```

The router should not commit a Kafka offset before the delivery record is durably created.

Otherwise, we lose the downstream work.

A sensible flow:

```
Kafka event
    |
    ▼
Router attempts DB write
    |
    ├── Success -> Commit offset
    |
    └── Failure -> Do not commit offset
                         |
                         ▼
                    Retry / pause
```

If the outage lasts for several minutes, Kafka consumer lag increases.

Once the database recovers, workers resume.

### But what happens if the database is slow?

Use:

- Bounded connection pools.
- Query timeouts.
- Circuit breakers.
- Backpressure on consumers.
- Controlled retry.
- Appropriate indexes.

If workers are unable to create durable delivery records, they should stop accepting more processing work than they can safely handle.

## 28. What if Redis is lost but PostgreSQL and Kafka are healthy?

This is easier from a business-data perspective.

Existing push servers still own their local connections.

The registry can be rebuilt.

```
Push Server 1 -> Local connection map
Push Server 2 -> Local connection map
Push Server 3 -> Local connection map
```

They re-register their active connections.

Meanwhile:

- Kafka retains events.
- PostgreSQL retains business messages.
- The notification inbox retains pending notifications.

Once Redis is rebuilt, routing resumes.

This shows why our storage responsibilities were separated.

Loss of the connection registry should disrupt live routing, not destroy business messages or delivery history.

## 29. Should we store messages in both Kafka and the database?

Often, yes.

But we need to understand why.

Suppose a chat message is stored in PostgreSQL and its creation event is published to Kafka.

```
PostgreSQL:
    The authoritative chat message.

Kafka:
    The event stream indicating that a message was created.
```

These are different purposes.

The database supports:

- Conversation history.
- Fetching specific messages.
- Editing/deleting according to business rules.
- Queries by conversation and sequence.

Kafka supports:

- Event processing.
- Routing.
- Analytics consumers.
- Other independent subscriptions.
- Event replay within its retention period.

We aren't necessarily duplicating the same storage responsibility.

We're maintaining the same business occurrence in two representations for different access patterns.

The Outbox makes the transfer reliable.

Do we need infinite retention in both?

No.

For example:

```
Chat DB:
    Retain messages according to chat-history policy.

Kafka:
    Retain chat events for 7 days.

Delivery DB:
    Retain pending/failed records according to notification policy.
```

These are illustrative policies.

Each store has its own retention requirements.

# Part P — The Complete Storage Architecture

## 30. Where does every piece of information live?

Let's create one final reference table.

| Information                   | Where it lives                   | Who reads/writes it              |
| ----------------------------- | -------------------------------- | -------------------------------- |
| Actual WebSocket connection   | Push-server memory               | Owning push server               |
| Connection → server mapping   | Redis                            | Push servers and routing workers |
| User → connections            | Redis                            | Push servers and routing workers |
| Server lease                  | Redis                            | Push servers and routing workers |
| Group membership              | Business DB                      | Group/chat service               |
| Group → active push servers   | Redis                            | Push servers and routing workers |
| User profile                  | User DB                          | User service                     |
| Chat message                  | Chat DB                          | Chat service                     |
| Order status                  | Order DB                         | Order service                    |
| Notification inbox            | Notification DB                  | Notification service             |
| Delivery status               | Delivery DB                      | Delivery workers and ACK handler |
| Event history                 | Kafka                            | Producers and consumers          |
| Retry schedule                | Delivery DB or queue             | Delivery workers                 |
| Actual delivery payload       | Event/DB/reference               | Delivery pipeline                |
| Client synchronization cursor | Business DB or suitable KV store | Client sync service              |
| Presence                      | Redis / presence service         | Presence system                  |

## 31. The correct database access flow

Let's say an order service wants to notify User A.

This is how data should flow:

```
                     Order Service
                          |
                          ▼
                      Order DB
                          |
                          ▼
                    Outbox Event
                          |
                          ▼
                        Kafka
                          |
                          ▼
                     Router Worker
                          |
                          | Who should receive?
                          ▼
                    Connection Registry
                        (Redis)
                          |
                          | Where are they?
                          ▼
                     Delivery Worker
                          |
                          ▼
                      Push Server
                          |
                          | Local connection lookup
                          ▼
                        WebSocket
                          |
                          ▼
                          User
```

Notice:

- The order service accesses the order database.
- The routing worker accesses the connection registry.
- The push server accesses its local connection map.
- The delivery system accesses delivery state when required.

There is no reason for the push server to independently query the order database.

If the user is offline, the notification remains in its durable inbox or the client can retrieve the authoritative order state.

# 32. My recommended default architecture

For a general-purpose real-time push platform, I'd start with:

```
                  BUSINESS SERVICES
                          |
                          ▼
                  Business Databases
                          |
                    Transactional Outbox
                          |
                          ▼
                         Kafka
                          |
                          ▼
                  Routing Workers
                          |
             ┌────────────┴────────────┐
             |                         |
             ▼                         ▼
      Connection Registry       Delivery Store
          (Redis)             (Optional, based
             |                  on requirements)
             |                         |
             └────────────┬────────────┘
                          |
                          ▼
                   Node Dispatch
                          |
             ┌────────────┼────────────┐
             |            |            |
             ▼            ▼            ▼
          Push 1       Push 2       Push 3
             |            |            |
          Local        Local        Local
          Memory       Memory       Memory
             |            |            |
             ▼            ▼            ▼
          Clients      Clients      Clients
```

Initial technology choices:

| Component              | Technology to consider                   |
| ---------------------- | ---------------------------------------- |
| Business storage       | PostgreSQL/MySQL                         |
| Connection registry    | Redis                                    |
| Event streaming        | Kafka                                    |
| Delivery tracking      | PostgreSQL initially, when required      |
| Actual connections     | Local push-server memory                 |
| Internal node dispatch | gRPC, HTTP, or server-directed messaging |

For smaller applications, you can simplify this considerably.

For example:

```
PostgreSQL + Redis + WebSocket
```

may be sufficient initially.

Introduce Kafka and a dedicated delivery subsystem when the volume, independent consumers, durability requirements, or processing complexity justify them.

# 33. The decision framework I want you to retain

Whenever you ask, "Where should this data live?", ask these questions:

1\. Is it an actual live socket?

Push-server memory.

2\. Is it temporary metadata used to find a socket?

Redis or another distributed key-value registry.

3\. Is it authoritative business information?

The database owned by the corresponding business service.

4\. Is it a durable event that several downstream services need to process?

Kafka or another suitable durable messaging system.

5\. Is it an independently tracked delivery obligation?

A delivery database, queue, or durable inbox, depending on the required semantics.

6\. Is it temporary work performed by a push server?

Local memory, with a recovery mechanism when that work matters.

This framework will prevent the most common architectural mistake: putting everything in Redis or making every push server another business service.

## Chapter 8 — What you should now understand

There are five important conclusions.

1. The actual socket lives only in the owning push server's memory.
2. Redis maintains reconstructible connection-routing metadata.
3. Business databases remain authoritative for business state.
4. A separate delivery database is needed only when the application requires independent delivery tracking, retries, or inbox semantics.
5. Push servers should generally avoid direct connections to domain databases.

The push infrastructure should own connection and transport state, not become the owner of every application's data.

# Next: Chapter 9 — Scaling the Entire Real-Time Push System

Now that we understand connections, routing, messaging, reliability, failures, and storage, we'll scale the complete architecture.

We'll cover:

1. How to estimate connections per push server.
2. CPU, memory, network, file descriptors, and TLS overhead.
3. How to scale connection registries.
4. How to partition Kafka topics and routing workloads.
5. How to handle millions of users receiving global broadcasts.
6. Fanout-on-write vs fanout-on-read.
7. How to handle hot users, hot groups, and hot topics.
8. Multi-region push architecture.
9. Regional routing and cross-region message delivery.
10. Data locality and regional failure recovery.
11. How to scale without increasing the load on business databases.
12. Capacity planning and production deployment considerations.

We'll also compare how the architecture changes at:

```
10,000 concurrent connections

1 million concurrent connections

10 million concurrent connections

100 million concurrent connections
```

The central question: How do we increase the number of users, events, and push servers without creating a bottleneck in the connection registry, messaging backbone, fanout workers, or network?
