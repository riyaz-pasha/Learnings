# Designing a Reliable Real-Time Push Service

Let's build this architecture incrementally, starting with the simplest possible implementation and evolving it until it can handle millions of connections, multiple push servers, Kafka, failures, retries, duplicate messages, and reconnects.

The most important design principle is:

> The push server owns the live connection, but it should not be the system of record for the message.

A live connection is temporary. A user can disconnect, a server can crash, or a network can fail at any moment. Therefore, reliable message delivery must not depend on a particular push server or a particular connection remaining alive.

We will separate three responsibilities:

1. Message ownership: Store the message durably so it can be recovered and delivered later.
2. Connection ownership: Know which push server currently owns each live client connection.
3. Message delivery: Route the message to the correct push server and write it to the appropriate connection.

These are different problems. Keeping them separate makes the architecture easier to scale and recover.

# 1. First, understand what we are actually building

Suppose we have an application where users need to receive events in real time.

Examples:

- A chat application delivers new messages.
- A payment application notifies a user when a payment succeeds.
- A task management application notifies a user when a task is assigned.
- A trading application streams price updates.
- A video processing application notifies a user when a file has finished processing.
- A collaboration application notifies users when another user edits a document.

A backend service produces an event, and a connected client should receive it without continuously polling the backend.

There are two relevant mechanisms, but they are not interchangeable.

## 1.1 SSE: Server-Sent Events

SSE is useful when the client establishes a persistent HTTP connection and the server continuously sends events over it.

The flow looks like this:

```
Browser / Mobile Client
        |
        | GET /stream
        |
        v
Push Server
        |
        | HTTP response remains open
        |
        | event 1
        | event 2
        | event 3
        |
        v
Browser / Mobile Client
```

The client opens a connection:

```
GET /stream HTTP/1.1
Host: api.example.com
Accept: text/event-stream
Authorization: Bearer <token>
```

The server responds:

```
HTTP/1.1 200 OK
Content-Type: text/event-stream
Cache-Control: no-cache
Connection: keep-alive
```

It can continuously write events:

```
id: 1001
event: payment.completed
data: {"paymentId":"p-101","status":"COMPLETED"}

id: 1002
event: task.assigned
data: {"taskId":"t-202","assignedBy":"user-10"}
```

The HTTP response remains open.

The client does not need to make a new request for every event.

Important: SSE is primarily one-way communication: server to client. The client can acknowledge receiving an event using a separate HTTP API.

## 1.2 Webhooks

Webhooks solve a different problem.

A webhook usually means that your service sends an HTTP request to another service's registered endpoint.

For example:

```
Your Application
       |
       | POST https://customer.example.com/webhook
       |
       v
Customer's Backend
```

The receiving backend does not need to maintain a persistent connection to your service.

Your service makes an outbound HTTP request whenever an event needs to be delivered.

This means the architecture needs two distinct delivery adapters:

| Concern                 | SSE                                      | Webhooks                                                     |
| ----------------------- | ---------------------------------------- | ------------------------------------------------------------ |
| Connection              | Long-lived client connection             | Individual HTTP requests                                     |
| Who initiates delivery? | Push server writes to an existing stream | Your service sends an HTTP POST                              |
| Connection ownership    | Push server                              | No persistent connection to own                              |
| Offline recipient       | Reconnect and replay later               | Retry delivery later                                         |
| Delivery acknowledgment | Application-level client ACK, if needed  | HTTP response, but not necessarily business-level processing |
| Routing                 | User/device to push-server mapping       | Subscription ID to endpoint mapping                          |

They can share the same durable event infrastructure, but their delivery mechanisms are different.

For the main architecture, we will focus on SSE. Later, we will evolve the same design to support webhooks.

# 2. Evolution 1: The simplest SSE implementation

Let's start with one push server.

A user opens `/stream`. The server accepts the request and stores the connection in its memory.

```
                 +-------------------+
                 |    Push Server    |
                 |                   |
Client --------->| GET /stream       |
                 |                   |
                 | Active Connections|
                 |                   |
                 | user-101 -> C1    |
                 | user-102 -> C2    |
                 | user-103 -> C3    |
                 +-------------------+
```

Conceptually, our server has an in-memory structure:

```
Map<UserId, List<SseConnection>> connections;
```

When a connection is established:

```
connections[user-101].add(connection-1)
```

When a message needs to be sent:

```
connection = connections[user-101]

connection.send(message)
```

This is simple and efficient.

The server already has the live TCP connection. It does not need to open another HTTP connection or go through the load balancer to reach the user.

### But what happens when we add more push servers?

Suppose we have three servers behind a load balancer.

```
                  Load Balancer
                       |
             +---------+---------+
             |         |         |
             v         v         v
           Push-1    Push-2    Push-3
             |         |         |
           User A    User B    User C
```

The load balancer selects a server when the client opens `/stream`.

For example:

```
User A -> Push-1
User B -> Push-2
User C -> Push-3
User D -> Push-1
```

Each server stores its own connections in local memory.

Now imagine Service A wants to notify User B.

It cannot simply invoke `connections[userB]` on Push-1.

Why?

Because User B's connection is stored in Push-2.

This introduces our first major problem:

How does the system find the push server that currently owns a user's connection?

That is the connection-routing problem.

# 3. Evolution 2: Introduce a connection registry

We need a central place that tells us which push server owns each active connection.

Let's introduce a Connection Registry.

Its responsibility is to maintain temporary information about active connections.

For example:

| User     | Connection ID | Push Server | Device |
| -------- | ------------- | ----------- | ------ |
| user-101 | conn-1        | push-1      | laptop |
| user-101 | conn-2        | push-2      | mobile |
| user-102 | conn-3        | push-2      | laptop |
| user-103 | conn-4        | push-3      | mobile |

Notice something important: one user can have multiple connections.

A user might be logged in through a browser, a mobile application, and another browser tab.

Therefore, a mapping of:

```
user_id -> push_server
```

is insufficient.

We need:

```
user_id -> multiple active connections -> push servers
```

A connection registry could look conceptually like this:

```
                    Connection Registry
                    -------------------

user-101:
    conn-1 -> push-1
    conn-2 -> push-2

user-102:
    conn-3 -> push-2

user-103:
    conn-4 -> push-3
```

Now, whenever a user establishes a connection, the push server registers it.

### 3.1 Connection establishment flow

Suppose User A opens `/stream`.

```
Client A
   |
   | GET /stream
   |
   v
Load Balancer
   |
   | Selects Push-2
   |
   v
Push-2
```

Push-2 performs the following operations:

Step 1: Authenticate the client

Validate the access token and identify:

```
user_id   = user-101
tenant_id = tenant-10
device_id = device-20
```

The server must also authorize the user to receive the relevant events. Authentication alone does not establish authorization to subscribe to every topic or resource.

Step 2: Create a connection identifier

Generate a unique connection ID:

```
connection_id = conn-123
```

Step 3: Store the connection locally

Push-2 maintains the actual HTTP response or connection handle in memory:

```
localConnections = {
    "conn-123": {
        "userId": "user-101",
        "response": <live HTTP response>
    }
}
```

This is the actual connection that allows the server to write events to the client.

Step 4: Register the connection centrally

Push-2 registers its ownership:

```
user-101 -> conn-123 -> push-2
```

Step 5: Start sending heartbeats

The connection remains active while the client and server can communicate.

The resulting architecture is:

```
                       Load Balancer
                             |
                +------------+------------+
                |            |            |
                v            v            v
              Push-1       Push-2       Push-3
                |            |            |
                |            |            |
                +------------+------------+
                             |
                             v
                    Connection Registry
```

### 3.2 Where should we store the registry?

For this particular responsibility, Redis is a natural choice.

Why?

Connection ownership is ephemeral information:

- A connection may appear at any moment.
- It may disappear at any moment.
- We frequently update its last-seen time.
- We need fast lookups by user ID.
- We need automatic expiration when a server disappears.

This fits Redis very well.

We do not need a traditional relational transaction every time a client sends a heartbeat.

For example, we can maintain keys conceptually like these:

```
connection:conn-123
    user_id       = user-101
    tenant_id     = tenant-10
    device_id     = device-20
    push_server   = push-2
    connected_at  = ...
    generation    = 5

user:tenant-10:user-101:connections
    conn-123
    conn-456
```

The connection record should have a TTL.

For example, suppose the lease duration is 60 seconds.

```
connection:conn-123
    TTL = 60 seconds
```

The push server periodically refreshes the TTL.

If the server crashes and stops refreshing it, Redis eventually removes the connection record.

This prevents dead connections from remaining in the registry indefinitely.

Important distinction: Redis tells us where a connection is supposed to exist. It does not contain the actual TCP connection.

The actual connection handle exists only inside the owning push server's memory.

This separation is fundamental:

```
Redis:
    conn-123 -> push-2

Push-2 memory:
    conn-123 -> actual HTTP response / connection handle
```

Redis cannot transfer an HTTP connection from one server to another.

# 4. Evolution 3: How do we route a message to the correct push server?

We have solved connection registration.

Now let's solve message delivery.

Suppose Service A needs to send a message to User A.

Service A should not know:

- Which push server owns User A.
- Whether User A has one or multiple connections.
- Whether User A is currently connected.
- How the actual SSE connection is maintained.

That is the push infrastructure's responsibility.

Service A should express its intent:

```
Deliver this event to user-101.
```

It should not express:

```
Send this event through push-2 using connection conn-123.
```

This avoids coupling business services to the push-server topology.

## 4.1 Introduce a message broker

Service A publishes the event into a messaging system.

For now, assume Kafka.

```
Service A
    |
    | Publish event for user-101
    v
Kafka
```

The event might look like:

```
{
  "eventId": "evt-1001",
  "tenantId": "tenant-10",
  "recipient": {
    "type": "USER",
    "id": "user-101"
  },
  "eventType": "payment.completed",
  "payload": {
    "paymentId": "payment-500",
    "status": "COMPLETED"
  },
  "createdAt": "2026-11-10T10:30:00Z"
}
```

Now we need something to consume this event and route it to the correct push server.

There are two possible approaches. Understanding their difference is especially important when using Kafka.

## 4.2 Approach A: Every push server independently subscribes to the event stream

We could configure each push server to receive every event.

```
                    Kafka
                      |
          +-----------+-----------+
          |           |           |
          v           v           v
        Push-1      Push-2      Push-3
          |           |           |
          v           v           v
       Check       Check       Check
       local       local       local
       users       users       users
```

Each push server checks whether it owns the target user's connection.

For example:

```
Event: Deliver to user-101

Push-1: Do I own user-101? No.
Push-2: Do I own user-101? Yes. Deliver.
Push-3: Do I own user-101? No.
```

This is easy to understand, but there is a serious Kafka trap.

You cannot put all three push servers in the same Kafka consumer group and expect each one to receive every event.

Within a Kafka consumer group, a record from a partition is consumed by one member of that group, not every member.

If Push-1, Push-2, and Push-3 share the same consumer group, an event might be consumed only by Push-1. That does not help when the user's connection belongs to Push-2.

You could give each push server a separate consumer group. Then each server independently receives every event.

However, that means every event is processed by every push server.

With 100 push servers, one event may cause 100 separate processing operations, even though only one server owns the recipient's connection.

This approach can be acceptable for a small cluster, but it is inefficient at scale.

## 4.3 Approach B: Introduce a dedicated message router

This is the architecture I would generally recommend for a reliable, scalable system.

Introduce a component called the Push Router or Delivery Dispatcher.

Its job is to:

1. Consume events from the durable event stream.
2. Find the currently active connections for the recipient.
3. Identify the push servers owning those connections.
4. Dispatch the event to those push servers.
5. Handle routing failures and delivery acknowledgments according to the chosen reliability contract.

The architecture becomes:

```
                    +----------------+
                    |   Service A    |
                    +----------------+
                            |
                            | Publish
                            v
                    +----------------+
                    |     Kafka      |
                    +----------------+
                            |
                            | Consume
                            v
                    +----------------+
                    |  Push Router   |
                    +----------------+
                            |
                            | Lookup active connections
                            v
                    +----------------+
                    | Redis Registry |
                    +----------------+
                            |
                  +---------+---------+
                  |                   |
                  v                   v
              Push-1                Push-2
                  |                   |
                  |                   |
                  v                   v
              Clients             Clients
```

Let's walk through an actual delivery.

User-101 has two active connections:

```
conn-1 -> push-1
conn-2 -> push-2
```

Service A publishes:

```
Deliver event evt-1001 to user-101.
```

Kafka stores the event.

The Push Router consumes it and queries Redis:

```
Find all active connections for user-101.
```

Redis returns:

```
conn-1 -> push-1
conn-2 -> push-2
```

The router groups the destinations by server:

```
push-1:
    conn-1

push-2:
    conn-2
```

It sends an internal delivery command to Push-1 and Push-2.

Both servers locate the corresponding connection in their local memory and write the SSE event.

There is no load balancer involved in this delivery path.

The load balancer was involved when the client initially established its stream. Once the connection exists, the delivery infrastructure routes directly to its current owner.

### 4.4 How does the router communicate with the push servers?

There are several ways.

For example, use an internal message bus:

```
Push Router
     |
     +----> internal topic/channel for push-1
     |
     +----> internal topic/channel for push-2
```

Redis Pub/Sub, NATS, or another low-latency internal messaging system can be used.

The push servers subscribe to their own delivery channels.

Conceptually:

```
channel: push-server-1
channel: push-server-2
channel: push-server-3
```

The router publishes:

```
{
  "deliveryId": "delivery-1001",
  "eventId": "evt-1001",
  "connectionId": "conn-1",
  "generation": 5,
  "payload": {
    "paymentId": "payment-500",
    "status": "COMPLETED"
  }
}
```

Push-1 receives the message, verifies that the connection still exists locally, and writes it to the client.

The `generation` field will become important when we discuss stale connection registrations and server failures.

Redis Pub/Sub is suitable for low-latency internal dispatch, but it is not a durable message store. If a push server is offline or subscribed too late, it can miss a Pub/Sub message.

That is acceptable only when we have another way to recover the message. We will introduce that shortly.

# 5. The recommended architecture so far

Let's consolidate what we have built.

```
                        CLIENT SIDE
                             |
                             | GET /stream
                             v
                      +-------------+
                      | Load Balancer|
                      +-------------+
                             |
                 +-----------+-----------+
                 |           |           |
                 v           v           v
              Push-1      Push-2      Push-3
                 |           |           |
                 |           |           |
                 +-----------+-----------+
                             |
                             v
                    Connection Registry
                         (Redis)


                        EVENT SIDE

                    +----------------+
                    |   Service A    |
                    +----------------+
                            |
                            | Publish event
                            v
                    +----------------+
                    |     Kafka      |
                    +----------------+
                            |
                            v
                    +----------------+
                    |  Push Router   |
                    +----------------+
                            |
                            | Resolve connection owners
                            v
                    Connection Registry
                            |
                            | Internal dispatch
               +------------+------------+
               |            |            |
               v            v            v
            Push-1       Push-2       Push-3
               |            |            |
               v            v            v
            Client       Client       Client
```

This solves the routing problem.

But we still haven't solved the most important reliability problem:

What happens when the message is accepted by the event infrastructure, but the client is offline, the push server crashes, or the network breaks during delivery?

Simply putting Kafka before the push servers does not automatically guarantee that the client receives the message.

To understand why, we need to separate event production, message persistence, server delivery, and client acknowledgment.

# 6. Evolution 4: Make delivery reliable even when users are offline

Imagine this situation.

1. Service A publishes a payment-completed event.
2. Kafka stores the event.
3. The Push Router consumes it.
4. The router finds that User-101 is connected to Push-2.
5. The router dispatches the message to Push-2.
6. Push-2 crashes immediately before writing the message to the client's connection.

What happens?

The message may already have been consumed from Kafka.

The router may have already committed its Kafka offset.

Redis Pub/Sub may have successfully accepted the internal dispatch before Push-2 crashed.

But the user has not received the event.

This is why we should not treat the push connection as the source of truth.

## 6.1 Introduce a durable notification inbox

Let's introduce another component: a User Notification Store, also called a user inbox.

Whenever a service wants to generate an important notification, we first persist it.

For example:

```
notification_id = notification-1001
user_id         = user-101
event_type      = payment.completed
status          = PENDING
```

The notification remains available even if:

- The user is offline.
- The push server crashes.
- The router restarts.
- The internal Pub/Sub message is lost.
- The user reconnects from another device.

The architecture now has two distinct stores:

| Store                      | Responsibility                        | Expected lifetime                          |
| -------------------------- | ------------------------------------- | ------------------------------------------ |
| Redis connection registry  | Where is the active connection?       | Short-lived                                |
| Durable notification store | Which messages must the user receive? | Until retention or deletion policy expires |

This distinction is extremely important.

Redis stores the temporary location of a connection.

The durable database stores the actual notification and its delivery identity.

The database is the recovery mechanism.

## 6.2 Where should the notification be persisted?

There are two common approaches.

### Approach A: Service A writes directly to the notification database

Service A performs:

```
1. Execute business operation.
2. Store business result.
3. Store notification.
4. Publish event to Kafka.
```

But this creates a distributed consistency problem.

Imagine:

```
Business transaction committed successfully.

Notification committed successfully.

Kafka publication failed.
```

Now we have a notification in the database, but no event telling the push infrastructure to deliver it.

Or consider the opposite:

```
Kafka publication succeeded.

Database transaction rolled back.
```

Now the push infrastructure might receive an event referring to something that was never committed.

We need to solve this properly.

### Approach B: Transactional Outbox Pattern

This is generally the stronger approach.

Service A performs its business operation and inserts an outbox record in the same database transaction.

For example:

```
BEGIN;

UPDATE payments
SET status = 'COMPLETED'
WHERE id = 'payment-500';

INSERT INTO outbox_events (
    id,
    event_type,
    aggregate_id,
    payload,
    created_at
)
VALUES (
    'evt-1001',
    'payment.completed',
    'payment-500',
    '{"userId":"user-101","paymentId":"payment-500"}',
    NOW()
);

COMMIT;
```

The business change and event creation either both commit or both roll back.

A separate Outbox Publisher reads unpublished records and publishes them to Kafka.

```
                    Service A
                        |
                        v
                 +-------------+
                 | PostgreSQL  |
                 |             |
                 | payments    |
                 | outbox      |
                 +-------------+
                        |
                        | Outbox Publisher
                        v
                     Kafka
```

The publisher should tolerate duplicate publication. It might publish an event and crash before recording that it was published. On restart, it can publish the same event again.

Therefore, consumers need idempotency.

The transactional outbox solves the database-to-message-broker atomicity problem. It does not, by itself, guarantee that the user receives the message.

## 6.3 Who creates the durable notification?

There are two reasonable ownership models.

Model 1: Service A owns the notification data.

Service A creates an outbox event. A downstream Notification Service consumes it and stores a user notification.

```
Service A
   |
   v
Business DB + Outbox
   |
   v
Kafka
   |
   v
Notification Service
   |
   v
Notification DB
   |
   v
Push Router
```

This is useful when notification creation is a separate business capability.

For example, the Notification Service may transform one business event into several user-facing notifications, apply preferences, or create notifications for multiple recipients.

Model 2: Service A writes to a shared notification/inbox service as part of its workflow.

This can be reasonable in a simpler system, but it introduces tighter coupling and potentially a cross-service transaction problem.

For a reusable push platform supporting many independent services, I would generally prefer Model 1.

The Notification Service owns durable notification records. The Push Service owns active connections and delivery.

# 7. Evolution 5: Treat Kafka as a delivery trigger, not as the user's inbox

We now have a durable notification database and Kafka.

What should happen when the Push Router consumes an event?

There are two useful ways to design this.

## 7.1 Pattern A: Store notification first, then publish a delivery trigger

The Notification Service receives the domain event and creates a durable notification.

For example:

```
Notification DB:

notification_id = n-1001
recipient_id    = user-101
sequence        = 501
event_type      = payment.completed
status          = PENDING
```

It also creates a delivery outbox record.

A publisher sends a small event to Kafka:

```
{
  "notificationId": "n-1001",
  "recipientId": "user-101",
  "sequence": 501
}
```

The Push Router consumes this event.

It does not need to treat the Kafka payload as the only copy of the message. It knows the notification is already durable.

It can load the notification or use a compact delivery envelope containing enough information to dispatch it.

The notification database remains the recovery source.

This separation is useful because Kafka is excellent at distributing events, but it is not automatically a convenient user-facing inbox.

## 7.2 Why isn't Kafka alone enough?

Kafka gives us:

- Durable event logs, subject to configured replication and retention.
- Consumer groups and partition-based parallelism.
- Message replay within the retained log.
- Ordering within an individual partition.
- Backpressure through consumer lag.

But it is not naturally designed for queries like:

```
Give me all unread notifications for user-101.

Give me notifications after sequence 500.

Give me all notifications for user-101 from the last 7 days.

Mark notification n-1001 as read.
```

We could build an entire user inbox around Kafka, but we would be fighting its primary data-access model.

For a typical application, a database is better for the durable user-facing inbox, while Kafka acts as the event transport.

## 7.3 What if we use Pub/Sub instead of Kafka?

The same distinction applies.

Think of the components this way:

| Technology                        | Main purpose in this design                                              |
| --------------------------------- | ------------------------------------------------------------------------ |
| PostgreSQL / DynamoDB             | Durable notifications and user inbox                                     |
| Kafka                             | Durable event distribution, replay, parallel consumption                 |
| Redis Pub/Sub                     | Very low-latency internal notification to a specific push node           |
| NATS or similar messaging systems | Low-latency internal routing, with durability depending on configuration |
| Redis connection registry         | Current connection ownership and presence                                |

You do not need all of these technologies.

A system can use:

```
PostgreSQL + Redis + Kafka
```

or:

```
DynamoDB + Redis + Kafka
```

or, for a less demanding system:

```
PostgreSQL + Redis Pub/Sub
```

The right choice depends on the durability, throughput, replay, and ordering requirements.

One important rule: Do not use an ephemeral Pub/Sub message as the only copy of a notification that must eventually reach the client.

# 8. Evolution 6: What exactly does "delivered" mean?

This is a subtle but essential part of the design.

Suppose Push-2 executes:

```
response.write(event);
response.flush();
```

Can we now mark the message as successfully delivered?

Not necessarily.

There are several distinct milestones:

| Milestone    | What it proves                                        |
| ------------ | ----------------------------------------------------- |
| Accepted     | The notification was durably stored                   |
| Published    | The notification event reached Kafka                  |
| Dispatched   | The router sent the message to the owning push server |
| Written      | The push server wrote the bytes to its HTTP response  |
| Received     | The client's networking stack received the bytes      |
| Processed    | The client's application handled the event            |
| Acknowledged | The client explicitly confirmed processing            |
| Read         | The user actually viewed the notification             |

These are not equivalent.

For a critical workflow, we should not use the word "delivered" without defining exactly which milestone we mean.

For example, a push server successfully writing to a socket does not prove that the browser application processed the event.

The connection could break during transmission. The browser could receive the bytes but crash before saving its state.

## 8.1 Introduce an application-level acknowledgment

For important notifications, give every notification a stable ID:

```
notification_id = n-1001
```

The server sends:

```
id: n-1001
event: payment.completed
data: {"paymentId":"payment-500","status":"COMPLETED"}
```

The client processes the event and calls a normal API:

```
POST /notifications/n-1001/ack
Authorization: Bearer <token>
Content-Type: application/json
```

The server verifies that this notification belongs to the authenticated user and records its acknowledgment.

Conceptually:

```
notification_id = n-1001
user_id         = user-101
acknowledged_at = ...
```

The acknowledgment endpoint must be idempotent.

Calling it twice should not create two acknowledgment records or cause an error that breaks recovery.

### What do we gain?

Consider the failure:

```
Push server writes event
        |
        v
Client receives event
        |
        v
Client processes event
        |
        X
Client crashes before ACK
```

We cannot know whether the client processed it.

The system may send it again after reconnect.

The client should therefore also deduplicate using `notification_id`.

This leads to the most practical guarantee for real-world push systems:

> At-least-once delivery attempts, combined with idempotent client processing.

Exactly-once processing across a network, server, and client application is not something we can generally guarantee just by using Kafka, Redis, or SSE.

We can make duplicate processing harmless.

# 9. Evolution 7: Handle reconnects and replay missed notifications

Now let's solve a realistic scenario.

User-101 receives notifications while connected to Push-2.

Then the user's internet connection disappears.

The SSE stream closes.

The user reconnects a few seconds later. The load balancer chooses Push-3 this time.

```
Before disconnect:

User-101 -> Push-2 -> Client


After reconnect:

User-101 -> Push-3 -> Client
```

This is perfectly normal.

We should not try to force the user back to Push-2.

Push servers are interchangeable connection hosts. A reconnect can land on any healthy server.

The new push server establishes a new connection and registers it.

But how does it know which notifications the user has missed?

## 9.1 Use notification IDs and sequence numbers

Every user's notification stream should have a monotonically increasing sequence number, or an equivalent durable cursor.

For example:

```
user-101:

sequence 501 -> payment.completed
sequence 502 -> task.assigned
sequence 503 -> report.completed
sequence 504 -> comment.created
```

The client has successfully processed notifications up to sequence 501.

It reconnects with:

```
GET /stream?after=501
```

Or it can use SSE's built-in `Last-Event-ID` mechanism.

The server then retrieves notifications after sequence 501:

```
SELECT *
FROM notifications
WHERE tenant_id = 'tenant-10'
  AND user_id = 'user-101'
  AND sequence > 501
ORDER BY sequence ASC
LIMIT 100;
```

It sends sequences 502, 503, and 504.

This is the replay mechanism.

The new push server does not need the previous push server's memory. The durable notification store supplies the missing events.

## 9.2 SSE already provides a reconnect mechanism

The browser's `EventSource` API automatically reconnects when a stream closes.

SSE also supports event IDs and the `Last-Event-ID` request header on reconnection.

For example:

```
id: 502
event: task.assigned
data: {"taskId":"t-202"}
```

If the browser reconnects after receiving that event, it may send:

```
Last-Event-ID: 502
```

The server can use this cursor to resume delivery.

However, there is an important distinction:

`Last-Event-ID` is not proof that the application's business logic successfully processed the event.

It is a transport-level reconnection cursor.

For applications requiring stronger guarantees, use an application-level acknowledgment cursor maintained by the backend.

For example:

```
last_transport_event_id = 502
last_processed_sequence = 501
```

These can differ.

The application-level cursor is the reliable signal for replay.

## 9.3 Avoid an unbounded replay operation

Imagine a user returns after being offline for three months.

Do not necessarily send every historical event through the live stream.

Define:

- Maximum replay window.
- Maximum batch size.
- Notification retention period.
- Expiration policy.
- Whether old notifications are fetched through a paginated REST API.
- Whether the client needs a fresh state snapshot.

For example:

```
GET /notifications?after=501&limit=100
```

The SSE endpoint can then switch to live delivery after replay catches up.

A common design is:

1. Client opens the stream with its last processed cursor.
2. Server queries outstanding notifications.
3. Server sends them in order.
4. Server transitions to live events.
5. If new messages arrive during replay, they are buffered or fetched through a coordinated cursor-based mechanism.

This last point matters: we need to avoid a race where a new notification arrives between the database replay query and the switch to live consumption.

One robust approach is to use the durable stream as the source for both replay and live tailing, with a cursor that defines the boundary. Another is to establish live delivery first, buffer incoming events, replay from the durable store up to a known high-watermark, and then drain the buffered events in sequence while discarding duplicates.

The important guarantee is that the transition from replay to live delivery must not introduce a gap.

# 10. Evolution 8: Heartbeats, dead connections, and stale connection registrations

Now let's understand what happens when a client stops responding.

First, we need to distinguish three things:

1. Whether the network connection is still alive.
2. Whether the client application is still receiving events.
3. Whether the client application is actually processing events.

A heartbeat can help detect connectivity problems, but it cannot prove all three.

## 10.1 Who should send heartbeats?

For SSE, the server should periodically send a heartbeat to the client.

For example, every 15–25 seconds:

```
: heartbeat
```

This is a valid SSE comment. The browser ignores it as an application event.

Why send it?

- It keeps idle connections active through intermediaries.
- It helps prevent some proxies and load balancers from closing idle connections.
- It gives the server opportunities to discover broken connections when writing.

A simplified timeline:

```
Push Server                         Client
     |                                 |
     | -------- heartbeat ------------>|
     |                                 |
     | -------- heartbeat ------------>|
     |                                 |
     | -------- heartbeat ------------>|
     |                                 |
```

A successful write does not necessarily prove that the browser application processed the heartbeat. It may only mean that the server's local networking stack accepted the bytes.

If a client disappears without a clean TCP close, the server might not immediately know.

For example, a mobile device may lose connectivity while the TCP connection is still considered open by the server.

## 10.2 Should the client also send heartbeats?

SSE itself is one-way.

The browser cannot send an application-level heartbeat message through the same SSE response.

You have three choices:

Option A: Rely on server heartbeats and TCP connection detection.

This is often sufficient for ordinary SSE applications.

The server writes heartbeat comments. A broken connection is eventually detected through a failed write or socket error.

Option B: Use a separate client heartbeat endpoint.

For example:

```
POST /stream/heartbeat
```

The client periodically calls it.

This lets the server verify that the client application is still actively communicating, rather than relying only on server-to-client writes.

This adds traffic and usually requires additional client-side timer and lifecycle management.

Option C: Use a different bidirectional protocol.

WebSocket is worth considering when application-level bidirectional heartbeats and continuous client-to-server messages are central to the use case.

You do not need WebSockets simply because the system must be reliable.

SSE can be reliable when combined with durable messages, reconnects, replay, and acknowledgments.

## 10.3 How should Redis connection TTL work?

Let's assume:

```
Heartbeat interval = 20 seconds
Connection lease   = 60 seconds
```

When Push-2 establishes a connection, it registers it with a 60-second TTL.

Every 20 seconds, it refreshes the lease.

```
Time 0s:  Register connection, TTL = 60s
Time 20s: Refresh TTL = 60s
Time 40s: Refresh TTL = 60s
Time 60s: Refresh TTL = 60s
```

Now imagine Push-2 crashes at time 65 seconds.

It cannot refresh the lease anymore.

Redis removes the connection record after its remaining TTL expires.

The router no longer sees the connection as active.

This is a lease-based ownership model.

The lease is useful even when we also have explicit disconnect handling, because explicit cleanup is never guaranteed to happen.

For example, a server might crash without executing its cleanup code.

## 10.4 Do not refresh Redis for every tiny heartbeat

We should avoid excessive writes.

For example, suppose:

- 1 million active connections.
- Each connection refreshes its lease every 10 seconds.

That gives:

```
1,000,000 / 10 = 100,000 Redis updates per second
```

This is a substantial workload.

We should tune the heartbeat interval and lease duration based on acceptable stale-connection duration, Redis capacity, failure-detection requirements, and network conditions.

We should also avoid one Redis operation per connection per heartbeat where practical.

For large fleets, the design may batch or pipeline lease refreshes while still ensuring that individual connections expire correctly.

The lease duration should be sufficiently longer than the refresh interval to tolerate scheduling delays and temporary Redis/network interruptions.

## 10.5 What happens when the client disconnects cleanly?

Suppose the browser closes its tab.

The push server detects a connection close.

It should immediately:

1. Remove the connection from local memory.
2. Remove or invalidate its connection registry entry.
3. Update any local connection counters.
4. Release associated resources.

The Redis removal is an optimization for prompt cleanup. The TTL remains the fallback.

The same process applies when a connection is explicitly terminated because of token expiry, authorization revocation, or server draining.

# 11. Evolution 9: Handle stale connection registrations and ownership races

Now let's examine a subtle distributed systems problem.

Consider this sequence:

```
1. User-101 connects to Push-1.
2. Redis stores conn-1 -> Push-1.
3. Push-1's connection breaks.
4. User-101 reconnects to Push-2.
5. Redis stores conn-2 -> Push-2.
6. A delayed cleanup from Push-1 runs.
```

What if the cleanup incorrectly removes the whole user entry?

It could accidentally remove Push-2's valid connection.

This is why connection records should be managed by connection ID, not by casually deleting an entire user mapping.

There is another problem: stale messages.

The router might have queried Redis just before a connection closed. It may dispatch a message to Push-1 after the client has already reconnected to Push-2.

The router's view is not guaranteed to be perfectly current.

We need to make stale routing harmless.

## 11.1 Use connection IDs and ownership generations

Each connection should have:

```
connection_id = conn-123
owner_node    = push-2
generation    = 7
```

The generation is a monotonically increasing ownership version for the connection lifecycle or ownership slot.

When a new connection replaces an older one, use a new connection ID and, where useful, an incremented generation.

The router includes the generation in its internal delivery command:

```
{
  "deliveryId": "delivery-1001",
  "connectionId": "conn-123",
  "expectedOwner": "push-2",
  "generation": 7,
  "notificationId": "n-1001"
}
```

Push-2 verifies:

```
Does conn-123 exist locally?
Does this server still own it?
Does the generation match?
```

If the connection does not exist, the server returns a result such as:

```
STALE_CONNECTION
```

The router can then refresh the registry and retry routing.

It must not blindly assume that the old ownership record is still valid.

## 11.2 Use compare-and-delete semantics

Suppose Push-1 is cleaning up `conn-123`.

It should delete the registry record only if the record still belongs to its expected ownership generation.

Conceptually:

```
Delete connection:conn-123
only if:
    owner_node == push-1
    AND generation == 6
```

If the record has already been replaced by generation 7, the cleanup must not delete it.

This should be an atomic operation, for example using a Redis Lua script or an equivalent transactional operation.

This pattern prevents delayed cleanup from deleting newer state.

## 11.3 Why use fencing tokens?

Leases alone can have subtle races.

Imagine a server temporarily pauses for a long time. Its lease expires, but the process later resumes and attempts to act as though it still owns the old connection.

A monotonically increasing fencing token lets downstream components reject commands from an older ownership epoch.

For a simple SSE gateway, verifying the locally registered connection ID and ownership generation is usually enough.

For systems with more complicated reassignment or shared ownership, fencing tokens become increasingly useful.

General rule: Connection ownership is temporary and can become stale. Every delivery command should be validated against the current connection state before writing.

# 12. Evolution 10: What if a push server goes down?

Let's consider the most important infrastructure failure.

Push-2 crashes.

Before the crash, it owns:

```
conn-1 -> user-101
conn-2 -> user-102
conn-3 -> user-103
```

The consequences:

- The TCP connections close or become unusable.
- All three clients need to reconnect.
- Redis may still temporarily contain the old connection records.
- In-flight delivery commands may fail.
- Push-2's local connection memory is lost.

How does the system recover?

## 12.1 Client-side recovery

The browser's SSE implementation automatically attempts to reconnect when the stream closes.

It reconnects through the load balancer:

```
User-101
   |
   v
Load Balancer
   |
   v
Push-3
```

Push-3 authenticates the user, establishes a new stream, and creates a new connection registration.

The user does not need to know that Push-2 failed.

For mobile clients or custom SSE clients, implement reconnection with exponential backoff and jitter.

For example:

```
Attempt 1: wait 1 second
Attempt 2: wait 2 seconds
Attempt 3: wait 4 seconds
Attempt 4: wait 8 seconds
Attempt 5: wait 16 seconds
```

Cap the delay and add random jitter to prevent thousands of clients from reconnecting simultaneously after a regional or server-wide failure.

For native browser `EventSource`, the reconnection behavior is built in, although application-level control and authentication renewal may require a wrapper or a new `EventSource` instance.

## 12.2 Connection registry recovery

The old Redis entries disappear in one of two ways:

- Push-2 performs cleanup before exiting, if it has an opportunity to do so.
- The lease TTL expires because Push-2 cannot refresh it.

The new connection on Push-3 registers separately.

We should not depend on the old connection registration being removed before allowing the new connection.

A user can temporarily have a stale old registration and a valid new registration.

The router must validate connection ownership and handle stale records safely.

## 12.3 In-flight message recovery

Suppose a notification is sent while Push-2 is crashing.

The Push Router tries to dispatch it.

It may see:

```
conn-1 -> push-2
```

But Push-2 is unavailable.

The internal dispatch fails or times out.

The router should not silently discard the notification.

Instead:

1. Keep the notification in durable storage.
2. Mark or record the dispatch attempt.
3. Retry after refreshing the active connection registry.
4. If there is no live connection, leave the notification available for reconnect replay.
5. When the client reconnects, replay from its last processed cursor.

Notice how the durable inbox solves the problem without requiring the old connection to survive.

## 12.4 Do we need to migrate connections between push servers?

Generally, no.

An established TCP connection cannot simply be moved from Push-2's process memory to Push-3.

When Push-2 fails, clients establish new connections.

That is a fundamental property of this architecture.

Trying to transfer the live connection is unnecessary complexity. Build reconnect and replay correctly instead.

# 13. Evolution 11: Design the database correctly

Now let's focus specifically on the databases, considering only this push-delivery workflow.

We have two different data domains, and they should not be confused.

## 13.1 Database A: Connection Registry

Recommendation: Redis.

Why?

The data is ephemeral and optimized for:

- Frequent connection registration.
- Frequent lease refreshes.
- Fast user-to-connection lookups.
- Fast connection-to-server lookups.
- Automatic expiration.

An example logical data model:

```
Connection record

connection_id
tenant_id
user_id
device_id
push_server_id
region
generation
connected_at
last_seen_at
lease_expiry
```

In Redis, a simple starting point is:

```
connection:{connection_id}
```

With fields such as:

```
user_id
tenant_id
device_id
push_server_id
generation
```

And an index of connection IDs for the user:

```
user:{tenant_id}:{user_id}:connections
```

Use Redis TTL for individual connection records.

The user-to-connections index must also be maintained carefully. Expired connection records do not automatically remove their IDs from arbitrary Redis sets.

You have several options:

- Explicitly remove IDs during disconnect.
- Use a sorted set with expiry timestamps and periodically clean stale members.
- Periodically reconcile the index against the individual connection records.
- Maintain an index structure with carefully designed expiry semantics.

Do not assume that expiring a connection key automatically cleans up every secondary index containing that key.

Also consider Redis Cluster key placement. Operations involving a user index and individual connection keys may require deliberate hash-slot design or separate atomic updates.

For a high-throughput system, design the data layout and cleanup strategy before relying on complex multi-key transactions.

### Why not PostgreSQL for live connection ownership?

You can use PostgreSQL for connection metadata, but it is not my first choice for high-frequency heartbeat updates.

Imagine:

```
1 million connected clients
Heartbeat every 20 seconds
```

That can generate around 50,000 heartbeat updates per second.

Updating a relational row for every heartbeat creates significant write amplification, index maintenance, WAL volume, and database pressure.

You can optimize the heartbeat strategy, but using Redis for this ephemeral registry is simpler.

### Should the push server also keep local state?

Absolutely.

We need both:

```
Local memory:
    Actual connection handle.

Redis:
    Cross-server connection ownership information.
```

Local memory is necessary for writing to the established connection.

Redis enables other components to discover which server owns it.

Neither replaces the other.

## 13.2 Database B: Durable Notification Store

Here we need to choose according to the application's access patterns and scale.

My default recommendation for a general notification system is PostgreSQL.

A possible schema:

```
CREATE TABLE notifications (
    id              UUID PRIMARY KEY,
    tenant_id       UUID NOT NULL,
    user_id         UUID NOT NULL,
    sequence        BIGINT NOT NULL,
    event_type      VARCHAR(150) NOT NULL,
    payload         JSONB NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL,
    expires_at      TIMESTAMPTZ,
    UNIQUE (tenant_id, user_id, sequence)
);
```

An index for replay:

```
CREATE INDEX idx_notifications_user_sequence
ON notifications (tenant_id, user_id, sequence);
```

A separate acknowledgment table can be useful:

```
CREATE TABLE notification_acknowledgements (
    tenant_id       UUID NOT NULL,
    user_id         UUID NOT NULL,
    notification_id UUID NOT NULL,
    acknowledged_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (tenant_id, user_id, notification_id)
);
```

You could instead maintain a last-processed sequence per user or subscription, depending on the semantics.

For example:

```
CREATE TABLE notification_cursors (
    tenant_id       UUID NOT NULL,
    user_id         UUID NOT NULL,
    stream_name     VARCHAR(100) NOT NULL,
    last_processed  BIGINT NOT NULL,
    updated_at      TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (tenant_id, user_id, stream_name)
);
```

The cursor approach works especially well for ordered streams where the client acknowledges processing all notifications up to a particular sequence.

Be careful: a single cursor assumes the client processes events in sequence. When parallel processing or out-of-order handling is allowed, individual acknowledgments or a more sophisticated offset model may be necessary.

### Why PostgreSQL?

It gives us:

- ACID transactions.
- Unique constraints for idempotency.
- Flexible queries.
- Useful indexes.
- JSONB for event payloads.
- Straightforward pagination.
- Reliable acknowledgment updates.
- A familiar operational model.

For many applications, it is a good starting point and can scale significantly using partitioning, read replicas, connection pooling, and carefully designed queries.

### When would I choose DynamoDB?

Choose DynamoDB when the workload has very high scale and predictable access patterns, especially when the main operations are:

```
Write notification for user.

Get notifications for user after sequence X.

Update acknowledgment state.

Fetch a notification by ID.
```

A possible key design:

```
Partition Key:
    TENANT#tenant-10#USER#user-101

Sort Key:
    NOTIFICATION#000000000501
```

Or use a time-based sort key if your primary read patterns are time-oriented.

Advantages:

- Horizontal partitioned throughput.
- Managed scaling.
- High write throughput.
- TTL support.
- Good fit for user-scoped queries.
- Integration with DynamoDB Streams.

Tradeoffs:

- Requires careful partition-key distribution.
- Hot users or hot tenants can create hot partitions.
- Query patterns must be designed up front.
- Some relational and reporting queries are less convenient.
- Multi-item transactional requirements need careful consideration.

### What about Cassandra?

Cassandra can be a good fit when you have massive append-heavy workloads with predictable partition-based reads and strong operational expertise.

For a typical application notification inbox, I would not introduce Cassandra by default.

It adds complexity that PostgreSQL or DynamoDB may avoid.

### What about Redis?

Do not make Redis the sole durable notification database just because it is already used for connection management.

Redis persistence and replication configurations can improve durability, but its usual role in this design is ephemeral connection state and low-latency routing.

The notification must survive:

- Redis flushes.
- Redis failovers.
- Temporary outages.
- Push-server restarts.
- Pub/Sub message loss.

Keep it in a durable source of truth.

## 13.3 My recommended database selection

| Data                               | Preferred technology                                       | Reason                                            |
| ---------------------------------- | ---------------------------------------------------------- | ------------------------------------------------- |
| Live connection registry           | Redis Cluster                                              | Fast lookups, TTLs, ephemeral state               |
| Push server local connections      | Process memory                                             | Actual socket/HTTP response handles               |
| User notifications                 | PostgreSQL                                                 | Durable, transactional user inbox                 |
| Very high-scale notification inbox | DynamoDB                                                   | Predictable key-based access and horizontal scale |
| Event distribution                 | Kafka                                                      | Durable log, partitioning, consumer groups        |
| Internal ephemeral routing         | Redis Pub/Sub or NATS                                      | Low-latency node-specific dispatch                |
| Outbox                             | Same DB as the owning business or notification transaction | Atomic state change and event creation            |

These components have different responsibilities. There is no need to select one database to do all of them.

# 14. Evolution 12: The actual push-server responsibilities

Now let's answer an important architectural question:

Should push servers handle business operations, or should they only handle the push part?

My recommendation is to make them focused connection and delivery gateways.

A push server should be responsible for:

1. Accepting SSE connections.
2. Authenticating the connection.
3. Authorizing subscriptions.
4. Registering and renewing connection ownership.
5. Maintaining local connection handles.
6. Sending heartbeat comments.
7. Receiving internal delivery commands.
8. Writing events to the correct connections.
9. Handling slow clients and broken connections.
10. Cleaning up disconnected connections.
11. Enforcing connection limits and payload limits.

It should generally not be responsible for:

- Executing payment business logic.
- Updating order state.
- Orchestrating distributed transactions.
- Making domain-specific business decisions.
- Running complex notification fan-out rules.
- Managing durable notification creation for unrelated domain services.

Those responsibilities belong in the relevant domain or notification services.

Why make the push server so focused?

Imagine that a notification requires:

```
Check payment.
Update account.
Generate invoice.
Send email.
Notify user.
```

If we put all these operations inside the push server, it becomes a domain service as well as a connection server.

Now its workload can be affected by database latency, business logic, third-party API failures, and other operations that have nothing to do with maintaining SSE connections.

That is undesirable.

The push server should ideally be able to do its core job:

```
Receive delivery envelope
        |
        v
Find local connection
        |
        v
Write event
```

It can perform authentication, authorization, and delivery-protocol concerns without owning the underlying business workflow.

### Should the push server connect to a database?

It depends on which database and which operation.

For the main delivery path:

- It may communicate with Redis to register or validate connection ownership.
- It may communicate with an internal delivery bus.
- It may retrieve a durable notification during replay, either directly from the notification store or through a dedicated notification API.

It should not execute an SQL write for every heartbeat or every SSE write.

A useful separation is:

```
Push Server:
    Connection handling
    Local connection memory
    Redis registry
    Internal delivery bus
    Optional replay read API

Notification Service:
    Durable notification creation
    Notification reads
    Acknowledgments
    Retention
    User-facing notification state
```

For strong service boundaries, the push server should call the Notification Service's replay API rather than directly owning notification tables.

A direct read connection can still be reasonable for performance-sensitive deployments, but it should not make the push server the owner of the notification data model.

# 15. Evolution 13: Make routing and delivery robust with Kafka

Let's design the Kafka portion properly.

We have:

```
Notification Service
        |
        v
Notification DB + Outbox
        |
        v
Kafka
        |
        v
Push Router
        |
        v
Push Servers
```

There are two important design questions:

1. How do we scale Kafka consumers?
2. How do we preserve ordering for a user's notifications?

## 15.1 What should the Kafka message key be?

A sensible starting point is:

```
Kafka key = tenant_id + user_id
```

For example:

```
tenant-10:user-101
```

Why?

Kafka uses the message key to determine partition placement when using its standard keyed partitioning strategy.

All records with the same key are routed to the same partition, assuming the partitioning configuration remains consistent.

This gives us ordering within that partition.

Example:

```
Partition 0:
    user-101 -> notification 501
    user-101 -> notification 502
    user-101 -> notification 503

Partition 1:
    user-102 -> notification 301
    user-102 -> notification 302

Partition 2:
    user-103 -> notification 201
```

Now several Push Router instances can consume the topic through one consumer group.

```
                 Kafka: user-notifications
                            |
                +-----------+-----------+
                |                       |
                v                       v
           Router-1                 Router-2
           P0, P1                    P2, P3
```

The actual partition assignments depend on the consumer group and number of partitions.

This gives us parallel processing across users while retaining partition order for each user.

It does not guarantee global ordering across all users, and we don't need that.

## 15.2 Does this guarantee ordering at the client?

Not by itself.

Suppose Router-1 consumes:

```
user-101: sequence 501
user-101: sequence 502
```

It dispatches sequence 501 to Push-1.

Then Push-1 becomes slow.

The router might attempt to dispatch sequence 502 before the first delivery has been completed.

Kafka ordered the records, but our downstream dispatch may still introduce concurrency or reordering.

Therefore, decide where ordering is required.

For a typical user notification stream, a good design is:

- Assign a sequence number when the notification is durably created.
- Preserve message order in the Kafka partition.
- Serialize delivery per user or per relevant stream where strict ordering is required.
- Include the sequence number in every notification.
- Let the client discard duplicates.
- Let the client detect sequence gaps and request replay.

This gives us a way to recover even if retries and reconnections change delivery order.

For example, the client expects:

```
501 -> 502 -> 503
```

But receives:

```
501 -> 503
```

It notices that 502 is missing and fetches the missing data.

Strict ordering at every stage is expensive. Sequence-based recovery is often a better approach.

## 15.3 Important: Do not make every push server consume the same Kafka group

Let's make the earlier Kafka rule explicit.

Suppose we have:

```
Kafka
  |
  +--> Push-1
  +--> Push-2
  +--> Push-3
```

If these servers share one consumer group, Kafka distributes partitions among them.

It does not broadcast each record to all three.

For the recommended design, keep the Kafka consumer group at the router/dispatcher layer.

The router resolves the destination push servers and sends targeted internal delivery messages.

This separates durable event consumption from connection ownership.

## 15.4 What happens when the router cannot reach a push server?

Consider:

```
Router -> Redis lookup -> Push-2
```

Router tries to dispatch a notification to Push-2.

But Push-2 is unavailable.

The router must not interpret a successful Redis lookup as proof that delivery succeeded.

Its next actions should be:

```
1. Attempt internal dispatch.
2. Detect timeout or connection failure.
3. Refresh the connection registry.
4. Check whether the connection has moved or disappeared.
5. Retry against a valid destination, when one exists.
6. Otherwise leave the notification pending for replay.
```

A router should distinguish outcomes:

```
ACCEPTED_BY_GATEWAY
STALE_CONNECTION
GATEWAY_UNAVAILABLE
NO_ACTIVE_CONNECTION
INVALID_RECIPIENT
```

These outcomes should have different retry behavior.

For example:

| Outcome              | Appropriate action                                   |
| -------------------- | ---------------------------------------------------- |
| Gateway accepted     | Complete the dispatch attempt                        |
| Stale connection     | Refresh ownership and reroute                        |
| Gateway unavailable  | Retry with backoff                                   |
| No active connection | Keep notification durable; wait for reconnect/replay |
| Invalid recipient    | Record permanent failure or reject upstream          |

We should also put a unique `delivery_id` in the dispatch message. If the router retries, the gateway can avoid writing the same delivery repeatedly where practical.

However, gateway-level deduplication is only an optimization. The durable notification ID and client-level deduplication remain necessary.

# 16. Evolution 14: Retry strategies and duplicate messages

Distributed systems naturally produce duplicate attempts.

Suppose:

```
Router sends notification to Push-2.
Push-2 receives it.
Push-2 writes it to the connection.
Router times out before receiving the acknowledgment.
```

The router does not know whether the write succeeded.

Should it retry?

For a reliable system, usually yes.

But the user might receive the same notification twice.

This is why our design must be idempotent.

## 16.1 Assign stable IDs

Every logical notification gets one stable ID:

```
notification_id = n-1001
```

Every dispatch attempt can get its own attempt ID:

```
notification_id = n-1001
delivery_attempt = 1
```

Retry:

```
notification_id = n-1001
delivery_attempt = 2
```

The notification ID must not change across retries.

The client keeps track of processed IDs or the highest contiguous processed sequence.

For example:

```
async function handleNotification(notification) {
    if (await alreadyProcessed(notification.id)) {
        return;
    }

    await processBusinessEvent(notification);

    await saveProcessedNotification(notification.id);

    await acknowledgeOnServer(notification.id);
}
```

This is conceptual pseudocode. In production, the client should ensure that business-state changes and its local deduplication record are atomic where possible.

For example, a mobile app can use a local database transaction to update its state and insert the processed notification ID.

The browser can use IndexedDB for durable client-side tracking.

## 16.2 Where should the retry happen?

There are different retry layers.

| Layer               | What it retries                                    |
| ------------------- | -------------------------------------------------- |
| Outbox publisher    | Publication to Kafka                               |
| Kafka consumer      | Processing of an event after failure               |
| Push Router         | Dispatch to the owning push server                 |
| Push Server         | Local connection write, within safe bounded limits |
| Client              | Reconnecting and fetching missed notifications     |
| Client ACK workflow | Reconciliation when an acknowledgment is missing   |

Avoid having every layer blindly retry forever.

Define one clear owner for each retry responsibility.

For example:

- Outbox publisher retries broker publication.
- Router retries infrastructure dispatch.
- Durable notification store preserves pending messages.
- Client reconnect/replay handles offline delivery.
- Client retries acknowledgment requests.

## 16.3 Exponential backoff with jitter

For transient failures:

```
Retry 1: 1 second
Retry 2: 2 seconds
Retry 3: 4 seconds
Retry 4: 8 seconds
Retry 5: 16 seconds
```

In practice, add jitter:

```
actual_delay = random value within an allowed range
```

This prevents synchronized retry storms.

Also define:

- Maximum retry count or maximum retry age.
- A dead-letter policy for repeatedly failing records.
- Monitoring for retries and dead-letter volume.
- A way to redrive recoverable failures.

A no-active-connection result is different from a gateway crash. Do not continuously retry an offline user's live connection delivery. Preserve the notification and replay it when the user returns.

## 16.4 What about Kafka offsets?

A particularly important detail:

Do not commit a Kafka offset merely because the router received a record.

Commit only after the router has completed the work that its reliability contract considers successful.

But even this needs careful wording.

Suppose the router dispatches to Push-2, Push-2 writes to the HTTP connection, and the router commits the offset.

That does not prove the client processed the event.

For the recommended durable-inbox architecture, the router's job is to initiate delivery, not to guarantee application-level processing.

Once the notification is safely persisted, the router can process a delivery trigger and dispatch it. It can commit its Kafka offset after dispatch has been accepted or after a durable routing task has been recorded.

If stronger downstream dispatch guarantees are required, use a durable dispatch task/outbox and idempotent delivery workers, rather than treating a transient internal Pub/Sub publish as durable acceptance.

This means there are two different guarantees:

Infrastructure delivery guarantee: The event will be routed or retried until it reaches a gateway or is deferred.

User processing guarantee: The client processes the notification and acknowledges it.

The second guarantee depends on the durable inbox and application-level acknowledgment, not merely the Kafka consumer offset.

## 16.5 Should we add a delivery-status database?

Only when the product needs that level of tracking.

For example:

```
notification_id = n-1001
user_id         = user-101

created_at       = ...
dispatched_at    = ...
acknowledged_at  = ...
read_at          = ...
```

For a multi-device application, be careful about what "acknowledged" means.

Does it mean:

- At least one device received it?
- Every active device received it?
- The user's account processed it?
- The user read it?

These are different business semantics.

A notification inbox can track a user's notification state while a separate delivery table tracks individual device or webhook attempts, if the application actually needs that information.

Do not create a row for every heartbeat or every socket write merely to have a complete audit trail. That can create unnecessary write amplification.

# 17. Evolution 15: Handle slow clients and backpressure

Let's say a client has a connection to Push-2.

Push-2 receives 100 notifications per second for that client.

But the client can consume only 10 per second.

What happens?

The server's output buffer keeps growing.

If we do not control this, one slow client can consume large amounts of memory and eventually harm the entire push server.

This is a classic backpressure problem.

## 17.1 Use bounded per-connection queues

Each connection can have a small bounded outgoing queue.

```
Push-2
  |
  +-- Connection A -> Queue A
  |
  +-- Connection B -> Queue B
  |
  +-- Connection C -> Queue C
```

Suppose:

```
Maximum pending messages = 100
```

If the queue reaches its limit, we need a policy.

Possible choices:

- Disconnect the slow client and let it reconnect and replay.
- Drop replaceable updates, such as old presence or price snapshots.
- Coalesce updates where only the latest value matters.
- Reject lower-priority messages when capacity is exhausted.

For durable notifications, disconnecting and replaying is usually safer than silently dropping the messages.

For replaceable real-time updates, such as a frequently refreshed status, keeping only the latest value may be more efficient.

## 17.2 Apply limits at several levels

Useful limits include:

```
Maximum active connections per server
Maximum connections per user
Maximum payload size
Maximum queue size per connection
Maximum queued bytes per server
Maximum event rate per user
Maximum event rate per tenant
Maximum concurrent reconnects
```

Connection count alone is not a sufficient scaling metric.

A server with 100,000 idle SSE connections might be healthy, while one with 30,000 connections and extremely high outgoing throughput might be overloaded.

Monitor:

- Active connections.
- Outbound messages per second.
- Outbound bytes per second.
- Per-connection queue size.
- Memory usage.
- Event-loop or worker saturation.
- Failed writes.
- Slow-client disconnects.
- Kafka consumer lag.
- Router dispatch latency.

# 18. Evolution 16: Handle multiple connections, devices, and subscription types

We previously used:

```
user_id -> connections
```

That is enough for basic delivery, but real applications need more precise routing.

Consider this user:

```
user-101
    |
    +-- Laptop
    |
    +-- Mobile
    |
    +-- Tablet
```

Maybe the application wants to send:

- A payment notification to all devices.
- A security notification to all active sessions.
- A chat typing event only to the current conversation.
- A device-specific message to one mobile device.
- A real-time dashboard update to users authorized for a particular project.

So connection ownership should be separate from subscription authorization.

An expanded connection record might look like:

```
connection_id
tenant_id
user_id
device_id
push_server_id
region
connected_at
generation
```

Subscriptions might be maintained separately:

```
connection_id -> subscriptions

conn-101:
    user:user-101
    project:project-10
    conversation:conversation-20
```

A message can identify a target using a routing type:

```
{
  "target": {
    "type": "USER",
    "id": "user-101"
  }
}
```

Or:

```
{
  "target": {
    "type": "DEVICE",
    "id": "device-20"
  }
}
```

Or:

```
{
  "target": {
    "type": "CHANNEL",
    "id": "project-10"
  }
}
```

There is an important authorization rule here:

The client must not be allowed to subscribe to arbitrary channels just by supplying a channel name.

When a connection requests a subscription, the server must verify that the user is authorized for that resource.

For large-scale channel fan-out, we might add a separate subscription directory or channel-to-node index.

For example:

```
project-10
    |
    +-- push-1: conn-1, conn-3
    |
    +-- push-2: conn-7
    |
    +-- push-5: conn-20, conn-21
```

Then the router can dispatch once per owning push server instead of once per individual connection.

For a user-targeted message, routing by user is sufficient. For a room or project broadcast, channel-level indexing is more efficient.

Do not use the user-connection registry as a substitute for a subscription directory when the application has large group broadcasts.

# 19. Evolution 17: Scale the complete system

Let's now look at how this architecture behaves as the application grows.

Suppose we start with:

```
10,000 active SSE connections
```

Then we grow to:

```
100,000
1 million
10 million
```

We need to understand what actually scales.

## 19.1 Scale the push servers horizontally

Push servers are horizontally scalable.

```
                  Load Balancer
                       |
          +------------+------------+
          |            |            |
          v            v            v
        Push-1       Push-2       Push-3

              ... additional nodes ...
```

The load balancer distributes new connections among healthy push servers.

Existing SSE connections remain associated with their original servers.

This is important: when you add Push-4, the load balancer does not automatically move existing connections from Push-1, Push-2, or Push-3.

New connections can be routed to Push-4, while existing connections remain where they are.

You should scale based on active connection count, resource usage, outbound traffic, and latency.

## 19.2 Why shouldn't we rely only on sticky sessions?

Sticky sessions can help a load balancer consistently route requests belonging to a particular client to the same server.

But they are not the solution to message routing.

The system still needs to know which server owns each existing connection.

Also, sticky sessions do not help when that server crashes.

A reconnect must be able to land on another healthy server.

Use the connection registry for ownership discovery.

Treat load-balancer affinity as an optional routing optimization, not as the correctness mechanism.

## 19.3 Load-balancer considerations

Long-lived SSE connections need specific infrastructure configuration.

For example:

- Idle timeout must exceed the heartbeat interval.
- Response buffering must be disabled.
- Proxy behavior must support streaming.
- HTTP response compression should be considered carefully because compression buffers can introduce latency.
- Connection limits must be large enough.
- Draining behavior must allow existing connections to close gracefully.
- HTTP/2 behavior and stream limits must be understood.
- TLS termination and authentication behavior must be consistent.

For example, if the load balancer has a 30-second idle timeout but the server sends a heartbeat only every 60 seconds, the connection may be closed even though the application believes it is healthy.

Heartbeat timing and intermediary timeouts must be designed together.

## 19.4 Push-server draining during deployments

Suppose you need to deploy a new version.

Do not immediately kill all push servers.

A better process:

```
1. Mark Push-2 as draining.
2. Stop accepting new connections.
3. Mark it unavailable for new connection ownership.
4. Continue servicing existing connections temporarily.
5. Send a reconnect instruction/event where appropriate.
6. Close existing connections gradually.
7. Allow clients to reconnect elsewhere.
8. Terminate Push-2.
```

During draining, prevent new registry ownership from being created on the draining server.

Clients reconnect using a cursor, so they can resume without losing durable notifications.

This is much safer than abruptly restarting all nodes.

## 19.5 Scale Kafka and the router separately

The push server has a connection-heavy workload.

The router has a message-processing and fan-out workload.

They need different scaling policies.

For example:

| Component             | Main scaling signals                                           |
| --------------------- | -------------------------------------------------------------- |
| Push servers          | Active connections, memory, egress bandwidth, write latency    |
| Kafka                 | Partition count, throughput, storage, consumer lag             |
| Push routers          | Records per second, dispatch latency, CPU, downstream pressure |
| Redis registry        | Operations per second, memory, network, latency                |
| Notification database | Write throughput, read throughput, storage, query latency      |

You should not scale push servers merely because Kafka lag is increasing.

That may mean the router is overloaded, the database is slow, or internal dispatch is failing.

Each layer needs independent observability.

## 19.6 A rough capacity example

Assume:

```
1 million active connections
Average idle connection memory = 20 KB
```

The connection-associated memory alone is approximately:

```
1,000,000 × 20 KB
= 20 GB
```

This is only an illustrative calculation. Actual memory per connection depends heavily on the networking stack, framework, TLS, buffers, application objects, and runtime.

If we run 20 push servers, the theoretical average would be:

```
50,000 connections per server
```

But do not simply divide the total by the number of servers and assume the system is safe.

Load may be uneven. Some tenants may be much more active. Some connections may produce much more traffic.

Run realistic load tests, include failure headroom, and measure memory and network use.

For this design, the outgoing data rate can become more important than the number of connections.

# 20. Evolution 18: Multi-region delivery

Let's extend the architecture to multiple regions.

Suppose we have:

```
                 Global Load Balancer
                    /           \
                   v             v
              Region A         Region B
                |                |
          Push Servers      Push Servers
```

A user connects to a nearby region or their assigned home region, depending on latency, data residency, and consistency requirements.

The connection registry should contain regional ownership information:

```
user-101
    conn-1 -> region-a / push-1
    conn-2 -> region-b / push-5
```

The router needs to know the region and owner node for every active connection.

There are two broad designs:

Regional routing with a global ownership directory

The router consults a globally accessible or replicated connection directory and dispatches to the owning region.

Regional routing with regional brokers and directory services

Events are distributed to the appropriate regions, and each region performs local connection resolution.

The second is often better when we want to isolate regional failures and keep most connection traffic within a region.

For example:

```
                   Notification Event
                          |
                          v
                   Regional Router
                          |
                 +--------+--------+
                 |                 |
                 v                 v
              Region A          Region B
                 |                 |
                 v                 v
             Push-1,2           Push-3,4
                 |                 |
                 v                 v
              Clients           Clients
```

Some considerations:

- Do not assume Redis replication provides strongly consistent global connection ownership.
- Use a clear ownership model for each active connection.
- Store durable notifications according to data-residency requirements.
- Define what happens when the home region fails.
- Avoid delivering the same event independently through multiple regions unless deduplication is guaranteed.
- Allow clients to reconnect to another region and replay durable notifications.

For many systems, the user's connection belongs to one active regional owner at a time, while the durable notification record can be replicated according to the application's data requirements.

The connection registry is a routing directory, not a globally consistent business database.

# 21. The complete recommended SSE architecture

We now have enough building blocks to assemble the full design.

Here is the architecture I would use as a strong general-purpose starting point.

```
                         BUSINESS SERVICES

                    +----------------------+
                    |      Service A       |
                    +----------------------+
                               |
                               | Business transaction
                               v
                    +----------------------+
                    |    Business DB       |
                    |  Business + Outbox   |
                    +----------------------+
                               |
                               | Outbox Publisher
                               v
                    +----------------------+
                    |        Kafka         |
                    |   Domain Events      |
                    +----------------------+
                               |
                               v
                    +----------------------+
                    | Notification Service |
                    +----------------------+
                               |
                  +------------+------------+
                  |                         |
                  v                         v
          +---------------+         +----------------+
          | Notification  |         | Delivery Outbox|
          |      DB       |         +----------------+
          +---------------+                  |
                  |                          v
                  |                     Kafka / Stream
                  |                          |
                  |                          v
                  |                   +--------------+
                  |                   | Push Router  |
                  |                   +--------------+
                  |                          |
                  |                  Lookup connection
                  |                       owners
                  |                          |
                  v                          v
             Replay API              +---------------+
                  ^                  | Redis Registry|
                  |                  +---------------+
                  |                          |
                  |               Dispatch to owning nodes
                  |                          |
                  |              +-----------+-----------+
                  |              |           |           |
                  |              v           v           v
                  |            Push-1      Push-2      Push-3
                  |              |           |           |
                  |              |           |           |
                  +--------------+-----------+-----------+
                                             |
                                             v
                                          CLIENTS


                         CONNECTION ESTABLISHMENT

          Client
             |
             | GET /stream
             v
        Load Balancer
             |
             v
       Available Push Server
             |
             +---- Authenticate
             |
             +---- Create local connection
             |
             +---- Register connection in Redis
             |
             +---- Start heartbeat
             |
             +---- Replay missed notifications
             |
             +---- Receive internal delivery commands
             |
             +---- Stream events to client
```

The push servers do not need to consume every domain event from Kafka.

They receive only the delivery commands intended for them.

The notification database holds the recoverable message state.

Redis holds active connection ownership.

The router performs the mapping between the two worlds.

# 22. One subtle problem: How do we avoid missing events during reconnect and replay?

We touched on replay earlier. Let's make this completely clear because it is a common source of production bugs.

Imagine:

```
Client last processed sequence = 100

Client reconnects.

Push server queries the database:
    sequence > 100

It finds:
    101
    102

While this query is happening, a new event is created:
    103
```

What if the server finishes replaying 101 and 102, then starts consuming live events from a point after event 103 has already passed?

The client may never receive 103.

This is a race between the historical replay path and the live delivery path.

## 22.1 A robust replay-to-live strategy

One approach is to use a durable event log with a well-defined cursor.

The process is:

Step 1: Establish the live event subscription and begin buffering new events for this connection.

Step 2: Read a high-watermark from the durable notification stream.

For example:

```
High watermark = 105
```

Step 3: Load all unacknowledged events after the client's cursor and up to 105.

```
101
102
103
104
105
```

Step 4: Send these events in sequence.

Step 5: Process the buffered live events.

Discard any events at or below 105 because they have already been included in replay.

Send events above 105 in order.

For example:

```
Buffered live events:
    104
    105
    106
    107

Already replayed:
    101 ... 105

Send from buffer:
    106
    107
```

Step 6: Continue normal live delivery.

This is a general pattern for switching from historical replay to live tailing.

The same principle works if your implementation uses a database sequence, Kafka offsets, or a durable per-user event cursor. The important thing is that the replay boundary and live subscription have a coordinated relationship.

An alternative is to use a single durable stream as the source for both replay and live consumption, with the same cursor semantics.

You should choose one strategy and implement it explicitly rather than simply performing a database query and then starting a live subscription.

## 22.2 Another practical consideration: reconnect storms

Imagine a push-server crash disconnects 100,000 users.

They all reconnect at approximately the same time.

The reconnect process can create:

- A sudden authentication spike.
- A Redis registration spike.
- A notification database read spike.
- A replay spike.
- A sudden increase in outbound network traffic.

Mitigations include:

- Client exponential backoff with jitter.
- Connection admission limits.
- Rate limiting.
- Paginated replay.
- Replay concurrency limits.
- Database read replicas where appropriate.
- Caching carefully chosen non-sensitive data.
- Gradual server recovery and draining.

The reconnection path is part of the scalability design, not just an error-handling detail.

# 23. What should the internal delivery message look like?

It is worth designing a stable envelope that all internal components understand.

For example:

```
{
  "deliveryId": "delivery-1001",
  "notificationId": "notification-1001",
  "tenantId": "tenant-10",
  "recipient": {
    "type": "USER",
    "id": "user-101"
  },
  "connectionId": "conn-123",
  "expectedOwner": "push-2",
  "generation": 7,
  "sequence": 501,
  "eventType": "payment.completed",
  "schemaVersion": 1,
  "payload": {
    "paymentId": "payment-500",
    "status": "COMPLETED"
  },
  "createdAt": "2026-11-10T10:30:00Z",
  "expiresAt": "2026-11-11T10:30:00Z",
  "traceId": "trace-abc"
}
```

Some of these fields are for routing; others are for message identity, ordering, security, observability, and retention.

| Field            | Why it matters                              |
| ---------------- | ------------------------------------------- |
| `deliveryId`     | Identifies one dispatch operation           |
| `notificationId` | Stable identity across retries              |
| `tenantId`       | Tenant isolation                            |
| `recipient`      | User, device, or channel target             |
| `connectionId`   | Specific live connection                    |
| `expectedOwner`  | Push server expected to own that connection |
| `generation`     | Prevents stale ownership operations         |
| `sequence`       | Ordering and gap detection                  |
| `eventType`      | Client event handling                       |
| `schemaVersion`  | Payload evolution                           |
| `payload`        | Event content                               |
| `createdAt`      | Auditing and latency measurement            |
| `expiresAt`      | Avoid delivering obsolete events            |
| `traceId`        | End-to-end debugging                        |

For sensitive applications, avoid putting sensitive business data into the message payload unnecessarily.

A notification might contain only:

```
{
  "notificationId": "notification-1001",
  "eventType": "payment.updated",
  "resourceId": "payment-500"
}
```

The client can then fetch the current payment state through its normal authenticated API.

This reduces the amount of sensitive data traveling through the push infrastructure and makes authorization easier to enforce at the resource API.

# 24. What APIs should the system expose?

A practical SSE push platform might expose the following endpoints.

| Endpoint                            | Purpose                                        |
| ----------------------------------- | ---------------------------------------------- |
| `GET /stream`                       | Establish SSE connection                       |
| `POST /notifications/{id}/ack`      | Acknowledge processing                         |
| `GET /notifications`                | Fetch notifications and replay history         |
| `GET /notifications/unread`         | Fetch unread notifications                     |
| `POST /notifications/{id}/read`     | Mark a notification as read                    |
| `POST /stream/heartbeat`            | Optional application-level client heartbeat    |
| `POST /stream/subscriptions`        | Subscribe to an authorized resource or channel |
| `DELETE /stream/subscriptions/{id}` | Remove a subscription                          |

Not every application needs every endpoint.

For example, a simple notification center might need only `/stream`, `/notifications`, and `/notifications/{id}/read`.

## Example: Establishing a stream

```
GET /stream
Authorization: Bearer <access-token>
Last-Event-ID: 500
Accept: text/event-stream
```

The response remains open and streams notifications.

## Example: Acknowledging a notification

```
POST /notifications/notification-1001/ack
Authorization: Bearer <access-token>
Content-Type: application/json
```

The server must verify that the authenticated user owns that notification.

Do not trust a client-supplied `user_id` for authorization.

## Example: Fetching missed notifications

```
GET /notifications?after=500&limit=100
Authorization: Bearer <access-token>
```

This API is particularly valuable for mobile clients and applications that may not always have a live stream connection.

# 25. What if the message must be delivered to a webhook instead?

Let's now extend the same platform to webhooks.

Suppose a customer registers:

```
https://customer.example.com/webhooks/payment
```

When a payment is completed, the system must POST the event to that endpoint.

We should not try to use the SSE connection registry.

There is no persistent client connection to find.

Instead, introduce a Webhook Delivery Worker.

```
                 Domain Service
                       |
                       v
                     Kafka
                       |
                       v
               Notification/Event
                    Service
                       |
                       v
                Durable Event Store
                       |
                       v
               Webhook Dispatcher
                       |
                       v
               Webhook Delivery Queue
                       |
              +--------+--------+
              |        |        |
              v        v        v
           Worker-1 Worker-2 Worker-3
              |        |        |
              v        v        v
          Customer's Registered Webhook
```

## 25.1 Store webhook subscriptions separately

For example:

```
webhook_subscriptions

id
tenant_id
endpoint_url
secret_reference
subscribed_event_types
status
created_at
```

The actual secret should be stored in a secrets manager or similarly protected storage, not in a casually accessible subscription record.

When a matching event arrives, create a durable delivery task.

```
webhook_deliveries

id
subscription_id
event_id
status
attempt_count
next_attempt_at
last_http_status
created_at
completed_at
```

The worker sends:

```
POST /webhooks/payment HTTP/1.1
Content-Type: application/json
X-Webhook-ID: delivery-1001
X-Event-ID: evt-1001
X-Webhook-Signature: ...
```

The receiver returns a 2xx response.

The worker records the attempt result.

## 25.2 How should webhook failures work?

| Result          | Typical behavior                                                                   |
| --------------- | ---------------------------------------------------------------------------------- |
| 2xx             | Mark HTTP delivery successful                                                      |
| 3xx             | Apply an explicit redirect policy; do not blindly follow arbitrary redirects       |
| 400 / 401 / 403 | Usually permanent/configuration failure; alert or disable according to policy      |
| 404 / 410       | Endpoint unavailable; commonly disable or suspend subscription after policy checks |
| 408 / timeout   | Retry                                                                              |
| 429             | Retry, respecting `Retry-After`                                                    |
| 5xx             | Retry with exponential backoff and jitter                                          |

An HTTP 2xx response proves the endpoint returned success, but it does not necessarily prove that the customer's business application completed its processing.

So webhook consumers should also deduplicate by event or delivery ID.

## 25.3 Important webhook security concerns

Since we are calling customer-provided URLs, we must defend against:

- Server-side request forgery (SSRF).
- Access to private or link-local IP addresses.
- DNS rebinding.
- Unsafe redirects.
- Untrusted TLS configurations.
- Excessive response bodies.
- Very slow endpoints.
- Unauthorized endpoint modifications.

Use request signing, TLS, bounded timeouts, endpoint validation, and careful network egress controls.

The useful architectural conclusion is that SSE and webhooks can share the durable event and notification infrastructure, but should have independent delivery workers and failure policies.

# 26. Failure scenarios: What happens in the real world?

Let's consolidate the architecture through a failure table.

| Failure                                  | What happens                                            | Recovery                                                                                                   |
| ---------------------------------------- | ------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------- |
| Client closes browser                    | SSE connection closes                                   | Push server cleans up; TTL is fallback                                                                     |
| Client loses network without clean close | Server may not detect it immediately                    | Heartbeat/write failures; Redis lease eventually expires                                                   |
| Push server crashes                      | Its local connection handles are lost                   | Client reconnects to another node; replay from durable store                                               |
| Redis becomes temporarily unavailable    | Registry operations may fail or be delayed              | Existing local connections may continue; do not accept unsafe ownership changes; retry registry operations |
| Redis entry is stale                     | Router may select a dead connection                     | Push server rejects stale connection; router refreshes registry                                            |
| Kafka is unavailable                     | Events cannot be published/consumed normally            | Outbox retains unpublished events; consumers resume later                                                  |
| Router crashes                           | Some delivery attempts stop                             | Kafka reassigns partitions; idempotent processing resumes                                                  |
| Internal Pub/Sub loses a message         | Gateway might not receive an attempted dispatch         | Durable notification remains; replay/reconciliation recovers                                               |
| Push server is overloaded                | Queues and write latency grow                           | Apply backpressure, bounded queues, and controlled disconnections                                          |
| Client receives duplicate notification   | Same logical event is delivered again                   | Stable notification ID and client deduplication                                                            |
| Client misses a notification             | Sequence gap or cursor mismatch                         | Fetch from durable inbox                                                                                   |
| Client ACK is lost                       | Server still considers it unacknowledged                | Retry ACK or replay; duplicate processing is harmless                                                      |
| Notification database is unavailable     | Cannot reliably query or persist new notification state | Fail or defer processing; don't pretend that message was durably accepted                                  |
| Database retention expires               | Old notification is no longer replayable                | Return an explicit expired-cursor response and provide a state refresh/snapshot                            |
| Regional outage                          | Live connections in that region fail                    | Reconnect to healthy region; retrieve durable messages according to replication and residency policy       |

There is one especially important rule here:

Do not claim that the user has received a notification simply because a router, broker, or push server accepted it.

The system should represent the actual delivery stage.

# 27. Operational and security requirements

A push platform is not only a connection-routing problem. It is also a security and operational system.

## 27.1 Security

At a minimum:

- Authenticate users before establishing a stream.
- Authorize every subscription.
- Associate every connection with a tenant and authenticated user.
- Do not trust client-supplied recipient IDs.
- Validate incoming event envelopes.
- Enforce per-user and per-tenant rate limits.
- Encrypt network traffic.
- Avoid leaking internal server IDs and routing metadata.
- Protect the internal delivery bus.
- Revoke connections when required by security policy.
- Handle access-token expiry explicitly.
- Prevent one tenant from consuming another tenant's notifications.

One SSE-specific issue: browser-native `EventSource` does not offer a general way to attach arbitrary authorization headers.

Common solutions include:

- Secure same-site cookies.
- An appropriate fetch-based streaming client.
- A short-lived, carefully scoped connection token.

Avoid putting long-lived access tokens in URL query parameters because URLs are frequently logged.

## 27.2 Observability

I would monitor at least these metrics.

| Component       | Important metrics                                                   |
| --------------- | ------------------------------------------------------------------- |
| Push servers    | Active connections, disconnects, reconnects, memory, outbound bytes |
| Connections     | Registration failures, lease-expiry count, stale ownership attempts |
| Router          | Dispatch rate, success rate, routing latency, retry rate            |
| Kafka           | Consumer lag, rebalance frequency, publish failures                 |
| Redis           | Memory, operations per second, latency, connection errors           |
| Notification DB | Write latency, replay-query latency, storage, connection-pool usage |
| Clients         | ACK latency, replay frequency, sequence gaps                        |
| Overall         | Time from event creation to client acknowledgment                   |

For reliability investigations, correlate events using:

```
event_id
notification_id
delivery_id
connection_id
push_server_id
trace_id
```

Without these identifiers, tracing an event through a distributed delivery pipeline becomes difficult.

## 27.3 Avoid excessive persistence

We want reliability, but we do not need to persist every low-level operation.

A reasonable persistence model is:

```
Persist:
    Business event/outbox
    Notification record
    Required delivery attempt state
    Client acknowledgment/read state

Keep ephemeral:
    Actual connection handles
    Current connection ownership
    Heartbeat comments
    Temporary per-connection write queues
```

This helps keep database traffic predictable.

# 28. Which architecture should we actually implement?

I would choose the architecture based on the required reliability level.

| Requirement                                       | Suitable starting architecture                                                    |
| ------------------------------------------------- | --------------------------------------------------------------------------------- |
| Simple live dashboard, dropped updates acceptable | SSE gateways + Redis connection registry + Pub/Sub                                |
| Important user notifications                      | Durable notification DB + Kafka + router + SSE gateways                           |
| Chat or ordered activity stream                   | Durable messages/events + per-conversation or per-user ordering + cursor replay   |
| High-volume real-time market updates              | SSE/WebSocket gateways + partitioned streaming + latest-value/coalescing strategy |
| Multiple devices per user                         | User-to-multiple-connections registry                                             |
| Large project/room broadcasts                     | Channel subscription directory + per-node fan-out                                 |
| Customer webhook platform                         | Durable webhook deliveries + retry workers + signatures                           |
| Multi-region product                              | Regional gateways + regional routing + durable replication and replay policy      |

For a general-purpose, reusable enterprise push platform, I would start with:

```
PostgreSQL
    Durable notification inbox
    Notification ACK/cursor
    Notification outbox

Kafka
    Durable event transport
    Partition by tenant_id + user_id

Redis Cluster
    Ephemeral connection registry
    Lease-based ownership
    Fast connection lookup

Push Router
    Consumes notification delivery triggers
    Resolves active owners
    Routes to the correct push nodes

Redis Pub/Sub or NATS
    Low-latency internal dispatch

Push Servers
    Maintain SSE connections
    Write events
    Handle heartbeats and slow clients

Clients
    Reconnect
    Replay from cursor
    Deduplicate
    Acknowledge processed notifications
```

At a smaller scale, PostgreSQL and Redis may be enough initially. Kafka should be introduced when durable, independently scalable event distribution and replay justify its operational cost.

Also, Redis Pub/Sub should remain a delivery accelerator in this design, not the only durable path.

# 29. An important distinction: Not every real-time event needs durable storage

So far, we have designed a reliable notification system where every important notification is persisted.

But real-world applications also have events that are useful only while they are happening.

For example:

| Event                       | Usually needs durable storage? | Why                                    |
| --------------------------- | ------------------------------ | -------------------------------------- |
| Payment completed           | Yes                            | The user must eventually know          |
| Report generation completed | Yes                            | Important application state            |
| New chat message            | Yes                            | Must not disappear                     |
| Task assigned               | Yes                            | Important business notification        |
| User typing                 | Usually no                     | Old typing state is irrelevant         |
| User online/offline         | Depends                        | Can often be reconstructed             |
| Live stock price update     | Depends                        | A newer price may replace an older one |
| Cursor movement             | Usually no                     | High-frequency, temporary state        |
| Dashboard progress update   | Depends                        | Latest state may be sufficient         |

This suggests that we should support two delivery modes.

## 29.1 Durable delivery mode

For important events:

```
Producer
    |
    v
Durable Notification Store
    |
    v
Delivery Trigger
    |
    v
Push Router
    |
    v
Push Server
    |
    v
Client
```

Properties:

- Notification is persisted.
- It can be replayed.
- It has a stable ID.
- It can be acknowledged.
- Duplicate attempts are safe.
- It has retention and expiration policies.

## 29.2 Ephemeral delivery mode

For temporary updates:

```
Producer
    |
    v
Pub/Sub
    |
    v
Push Router
    |
    v
Push Server
    |
    v
Client
```

Properties:

- No durable inbox is required.
- Messages may be dropped when the recipient is offline.
- Old updates may be coalesced or discarded.
- The latest state can be retrieved from a normal API if necessary.

This is useful because persisting every cursor movement or typing event would be unnecessarily expensive.

The push platform can support both, using a normalized internal event format and different reliability policies.

### 29.3 A practical event envelope

For example:

```
{
  "eventId": "evt-1001",
  "tenantId": "tenant-10",
  "recipientId": "user-101",
  "eventType": "dashboard.progress",
  "deliveryMode": "EPHEMERAL",
  "orderingKey": "dashboard-20",
  "payload": {
    "progress": 75
  }
}
```

For an important notification:

```
{
  "eventId": "evt-1002",
  "tenantId": "tenant-10",
  "recipientId": "user-101",
  "eventType": "report.completed",
  "deliveryMode": "DURABLE",
  "orderingKey": "user-101",
  "payload": {
    "reportId": "report-500"
  }
}
```

A source adapter can consume messages from Kafka, Redis Pub/Sub, or another broker and convert them to this internal envelope.

The downstream routing infrastructure then does not need to know which upstream broker originally produced the event.

This is particularly useful when your company has many applications using different messaging technologies.

# 30. The implementation algorithm: What exactly happens in code?

Let's now reduce the whole design into the operations each component actually performs.

This is the part that should make the architecture straightforward to implement.

## 30.1 Push server: Establish a connection

Conceptual pseudocode:

```
function handleStreamRequest(request):

    user = authenticate(request)

    authorizeStreamAccess(user)

    connectionId = generateUniqueId()

    generation = generateOwnershipGeneration()

    connection = acceptSseConnection(request)

    localConnections.add(
        connectionId,
        connection
    )

    connectionRegistry.register(
        connectionId,
        user.id,
        currentPushServer.id,
        generation,
        leaseDuration
    )

    startLeaseRefresh(
        connectionId,
        generation
    )

    startHeartbeat(connection)

    startReplayAndLiveDelivery(
        user,
        connection,
        request.lastEventId
    )
```

In a real implementation, registration and connection creation need failure handling.

For example, if Redis registration fails, do not leave an accepted connection in a state where the rest of the system believes it is available.

You can close the connection and allow the client to retry, or use a specifically designed degraded-mode strategy.

The key invariant is:

A connection should not be advertised as deliverable until the owning server is prepared to accept deliveries for it.

## 30.2 Push server: Lease refresh

```
function refreshConnectionLease(connectionId, generation):

    if localConnections.contains(connectionId):

        registry.refreshLease(
            connectionId,
            expectedOwner = currentPushServer.id,
            expectedGeneration = generation,
            leaseDuration
        )

    else:

        stopLeaseRefresh()
```

The registry update must verify ownership atomically.

A server must not refresh a lease that has already been reassigned to a newer owner.

## 30.3 Push server: Receive a delivery command

```
function handleDelivery(delivery):

    connection = localConnections.get(
        delivery.connectionId
    )

    if connection is missing:
        return STALE_CONNECTION

    if delivery.generation != connection.generation:
        return STALE_CONNECTION

    if connection.isClosed():
        removeConnection()
        return STALE_CONNECTION

    if connection.queueIsFull():
        disconnectSlowClient()
        return RETRY_OR_REPLAY

    connection.write(
        delivery.notification
    )

    return ACCEPTED_BY_GATEWAY
```

The response `ACCEPTED_BY_GATEWAY` means the gateway accepted the operation according to its implementation.

It does not mean the client processed it.

The client ACK is separate.

Also, the write operation should usually feed a bounded per-connection output queue rather than blocking a delivery consumer indefinitely.

## 30.4 Push Router: Route a notification

```
function routeNotification(notification):

    connections = registry.getConnections(
        notification.tenantId,
        notification.userId
    )

    if connections.isEmpty():
        markAsDeferredOrNoActiveConnection()
        return

    destinations = groupByPushServer(connections)

    for each destination in destinations:

        dispatch(
            destination.pushServer,
            notification,
            destination.connections
        )
```

For high fan-out, use asynchronous dispatch with bounded concurrency.

Do not launch an unbounded number of requests or tasks for a large channel broadcast.

When a dispatch fails, refresh ownership, retry within a bounded policy, or leave the notification pending for replay.

## 30.5 Client: Process notifications

```
function handleSseEvent(notification):

    if alreadyProcessed(notification.id):
        return

    processNotification(notification)

    saveProcessedState(notification.id)

    acknowledge(notification.id)
```

For an ordered stream, track a cursor and do not advance it past an unprocessed gap.

For example:

```
Processed: 101, 102, 103
Expected:  104

Received: 105
```

Do not advance the contiguous processed cursor to 105 until 104 is recovered or an explicit application policy allows the gap.

# 31. The five architectural invariants I would protect

The whole design can be understood through five rules.

Rule 1: A live connection is owned by exactly one active push server at a time.

A connection handle cannot be shared arbitrarily across servers. Redis provides the temporary ownership directory, while the actual handle stays local.

Rule 2: The connection registry is not the source of truth for notifications.

Connection ownership can expire or be lost. Durable notifications must remain recoverable independently.

Rule 3: A durable notification must have a stable identity.

Every retry, replay, and redelivery refers to the same notification ID. This makes deduplication possible.

Rule 4: A transport write is not an application acknowledgment.

A successful HTTP write does not prove the client processed the message. Important workflows need explicit acknowledgment and replay.

Rule 5: Every connection and delivery operation must tolerate stale state.

A router can read outdated registry information. A connection can disappear between lookup and dispatch. A retry can produce duplicates. None of these should corrupt the notification or business state.

These five rules are more important than choosing a particular broker or framework.

# 32. Putting it all together: One complete real-world story

Let's walk through one complete scenario.

We have:

```
User: user-101

Push Servers:
    push-1
    push-2
    push-3

Databases:
    PostgreSQL
    Redis

Messaging:
    Kafka
    Redis Pub/Sub
```

### Phase A: User establishes a connection

User-101 opens:

```
GET /stream
```

The load balancer routes the request to Push-2.

Push-2:

1. Authenticates User-101.
2. Creates connection `conn-123`.
3. Saves the actual SSE response in local memory.
4. Registers `conn-123 -> push-2` in Redis.
5. Starts refreshing its lease.
6. Starts sending SSE heartbeats.
7. Replays missed durable notifications.

Redis contains:

```
user-101:
    conn-123 -> push-2
```

### Phase B: A business event occurs

User-101 completes a payment.

Service A performs its business transaction:

```
Payment status = COMPLETED
Outbox event = payment.completed
```

Both are committed together.

The Outbox Publisher sends the event to Kafka.

### Phase C: Notification is created

The Notification Service consumes the event.

It evaluates notification rules and creates:

```
notification_id = n-1001
user_id         = user-101
sequence        = 501
status          = PENDING
```

It commits the notification and a delivery-outbox record.

A publisher emits a delivery trigger.

### Phase D: The router finds the connection

Push Router consumes the delivery trigger.

It queries Redis:

```
user-101 -> conn-123 -> push-2
```

It creates an internal dispatch command:

```
delivery-1001
notification n-1001
connection conn-123
expected owner push-2
generation 7
```

It sends the command through Push-2's internal delivery channel.

### Phase E: Push-2 writes the event

Push-2:

1. Finds `conn-123` in local memory.
2. Checks the ownership generation.
3. Adds the event to the bounded connection queue.
4. Writes the SSE event.

The client receives:

```
id: 501
event: payment.completed
data: {"notificationId":"n-1001","paymentId":"payment-500"}
```

### Phase F: The client acknowledges

The client processes the payment notification.

It sends:

```
POST /notifications/n-1001/ack
```

The Notification Service records the acknowledgment.

Now the system knows the client application acknowledged the notification.

### Phase G: Push-2 crashes unexpectedly

Later, Push-2 goes down.

The user's SSE connection breaks.

Redis might temporarily still contain:

```
conn-123 -> push-2
```

But its lease stops refreshing and eventually expires.

The browser automatically reconnects.

The load balancer sends it to Push-3.

Push-3:

1. Authenticates User-101.
2. Creates `conn-456`.
3. Stores its connection locally.
4. Registers `conn-456 -> push-3`.
5. Receives the client's last processed cursor.
6. Queries for notifications after that cursor.
7. Sends any missing notifications.
8. Continues live delivery.

The old push server does not need to recover its memory or transfer its connections.

### Phase H: A dispatch is duplicated

Suppose the router had already dispatched `n-1001` but did not receive its acknowledgment before Push-2 crashed.

The router retries.

The client may receive `n-1001` again after reconnect.

But it has already processed that notification ID.

It ignores the duplicate and acknowledges it again if necessary.

No duplicate business operation occurs.

That is the core of a reliable push architecture.

# 33. My final recommendation: How I would build this in production

I would split the solution into four independently scalable responsibilities.

| Responsibility          | Component            | Owns what?                                                                |
| ----------------------- | -------------------- | ------------------------------------------------------------------------- |
| Notification management | Notification Service | Durable notification records, sequences, acknowledgments, replay          |
| Connection management   | Push Gateway         | Live SSE connections, heartbeats, subscriptions, local connection handles |
| Connection discovery    | Redis Registry       | Mapping of active connections to push-server owners                       |
| Delivery orchestration  | Push Router          | Consuming delivery events, finding owners, dispatching to gateways        |

The architecture would look like this:

\#chatgpt-mermaid-\_r_a96\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_a96\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_a96\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_a96\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_a96\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_a96\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_a96\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_a96\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_a96\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_a96\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_a96\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_a96\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_a96\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_a96\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_a96\_ p{margin:0;}#chatgpt-mermaid-\_r_a96\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_a96\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_a96\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_a96\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_a96\_ .label text,#chatgpt-mermaid-\_r_a96\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_a96\_ .node rect,#chatgpt-mermaid-\_r_a96\_ .node circle,#chatgpt-mermaid-\_r_a96\_ .node ellipse,#chatgpt-mermaid-\_r_a96\_ .node polygon,#chatgpt-mermaid-\_r_a96\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_a96\_ .rough-node .label text,#chatgpt-mermaid-\_r_a96\_ .node .label text,#chatgpt-mermaid-\_r_a96\_ .image-shape .label,#chatgpt-mermaid-\_r_a96\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_a96\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_a96\_ .rough-node .label,#chatgpt-mermaid-\_r_a96\_ .node .label,#chatgpt-mermaid-\_r_a96\_ .image-shape .label,#chatgpt-mermaid-\_r_a96\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_a96\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_a96\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_a96\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_a96\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_a96\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_a96\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_a96\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_a96\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_a96\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_a96\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_a96\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_a96\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_a96\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_a96\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_a96\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_a96\_ .icon-shape,#chatgpt-mermaid-\_r_a96\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_a96\_ .icon-shape p,#chatgpt-mermaid-\_r_a96\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_a96\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_a96\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_a96\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_a96\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_a96\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_a96\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_a96\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_a96\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_a96\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_a96\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_a96\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_a96\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_a96\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_a96\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_a96\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_a96\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_a96\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_a96\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_a96\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_a96\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_a96\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_a96\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_a96\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_a96\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_a96\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_a96\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_a96\_ .node rect,#chatgpt-mermaid-\_r_a96\_ .node circle,#chatgpt-mermaid-\_r_a96\_ .node ellipse,#chatgpt-mermaid-\_r_a96\_ .node polygon,#chatgpt-mermaid-\_r_a96\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_a96\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_a96\_ .mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_a96\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_a96\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_a96\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}Business ServicesBusiness DB + OutboxKafkaNotification ServiceNotification DBDelivery OutboxKafka Delivery TriggersPush RouterRedis Connection RegistryInternal Node MessagingPush Gateway 1Push Gateway 2Push Gateway 3Connected ClientsConnected ClientsConnected ClientsLoad BalancerNotification ACK API

The load balancer handles connection establishment.

Redis handles connection discovery.

The Push Router handles message routing.

Push Gateways handle actual delivery over live connections.

The Notification Service handles durable notification state and recovery.

Kafka handles durable event distribution.

For internal node delivery, I would initially use Redis Pub/Sub or NATS if the durable notification store and replay process already provide recovery. If the gateway dispatch itself needs durable queuing independently of notification replay, introduce a durable internal delivery mechanism instead.

## The stack I would choose

For a general-purpose enterprise system:

```
SSE:
    HTTP streaming endpoints

Push gateways:
    Horizontally scalable application instances
    In-memory connection maps
    Bounded per-connection queues

Connection registry:
    Redis Cluster
    TTL-based leases
    Ownership generations

Durable messages:
    PostgreSQL
    Transactional outbox
    Notification sequence and ACK state

Event transport:
    Kafka
    Partition by tenant_id + user_id

Routing:
    Push Router consumer group
    Redis Registry lookup
    Node-specific internal dispatch

Client:
    Automatic reconnect
    Cursor-based replay
    Stable notification IDs
    Idempotent processing
    Application-level ACK
```

## What I would build first

I would implement the system incrementally in this order:

1. One SSE gateway with an in-memory connection map.
2. Multiple gateways behind a load balancer.
3. Redis connection registry with TTL-based ownership.
4. A router that resolves user connections and dispatches to the owning gateway.
5. Durable notifications in PostgreSQL.
6. Transactional outbox and Kafka integration.
7. Client reconnect, replay, deduplication, and ACK.
8. Bounded queues, heartbeat handling, retries, and server draining.
9. Multiple devices, channel subscriptions, and fan-out.
10. Multi-region routing and webhook delivery as separate extensions.

This avoids introducing Kafka, Redis, multiple services, and delivery-state tables before understanding why each is needed.

One last conceptual distinction worth remembering:

A message broker answers:

> Where should this event be distributed for processing?

The connection registry answers:

> Which server currently owns the client's live connection?

The push gateway answers:

> How do I write bytes to that live connection?

The durable notification store answers:

> How do I recover the message if any of the above fails?

And the client acknowledgment answers:

> Has the application actually processed the message?

Once these five responsibilities are clear, you can design a reliable push system for chat, notifications, dashboards, payment updates, collaboration, background jobs, and many other real-world applications without tying the entire architecture to one particular broker or database.
