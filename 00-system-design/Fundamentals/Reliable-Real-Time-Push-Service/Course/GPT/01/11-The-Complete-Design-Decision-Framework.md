# Chapter 11 — The Complete Design Decision Framework

We have covered the individual building blocks: WebSockets, SSE, connection registries, Redis, Kafka, message routing, delivery guarantees, retries, multi-region architecture, scaling, and different real-world applications.

Now comes the most important part:

How do you look at a completely new problem and derive the right architecture yourself, instead of memorizing architectures for different use cases?

That's what this chapter is about.

The goal is to develop a repeatable thought process that works whether you're designing:

- A chat application.
- A stock market updates platform.
- A multiplayer game.
- A live location tracking system.
- A notification platform.
- A collaborative document editor.
- A real-time analytics dashboard.
- Something you have never encountered before.

# 1. The biggest mistake: starting with technology

Imagine an interviewer asks:

> Design a real-time system that delivers updates to millions of users.

A common response is:

```
Load Balancer
    |
WebSocket Servers
    |
Redis
    |
Kafka
    |
Database
```

But why do we need WebSockets?

Why Redis?

Why Kafka?

What exactly are we storing in the database?

Who is sending messages? Who receives them? What happens when users disconnect?

We haven't answered any of these questions.

We are choosing technologies before understanding the problem.

Instead, use this principle:

First understand the behavior the system must provide. Then derive the components needed to provide that behavior.

For example, consider these three requirements:

1. Show the latest stock price to everyone watching a stock.
2. Deliver a payment confirmation to a particular user, even if they are offline.
3. Synchronize edits to a document among collaborators, preserving their intended order.

All three involve delivering updates to users.

But their requirements are fundamentally different.

| Requirement        | Latest stock price        | Payment confirmation           | Document edits                 |
| ------------------ | ------------------------- | ------------------------------ | ------------------------------ |
| Audience           | Many subscribers          | Specific user                  | Document collaborators         |
| Offline handling   | Fetch latest price        | Must deliver later             | Recover missed operations      |
| Ordering           | Depends on data type      | Usually not strict             | Essential                      |
| Durability         | Latest state is important | Important                      | Operations must be recoverable |
| Duplicate handling | Often tolerable           | Must prevent duplicate effects | Must handle operations safely  |

If we use the same delivery mechanism and reliability policy for all three, we'll likely build an unnecessarily complicated system or an unreliable one.

So let's build a framework.

# 2. Start with the product requirements, not the infrastructure

Before drawing an architecture, understand the behavior the product promises.

I recommend organizing your clarifying questions into eight categories.

## 2.1 Connection and interaction

First, understand how the participants interact.

Ask:

- Are clients continuously connected, or do they periodically request updates?
- Is communication only server-to-client, or bidirectional?
- Does a client send frequent updates to the server?
- Does the server need to initiate communication at any time?
- Are clients browsers, mobile applications, servers, or IoT devices?

Why does this matter?

Because it influences the transport.

| Requirement                                  | Likely option                     |
| -------------------------------------------- | --------------------------------- |
| Server continuously sends updates to browser | SSE or WebSocket                  |
| Client and server continuously communicate   | WebSocket                         |
| Client periodically sends updates            | HTTP                              |
| Very latency-sensitive game communication    | UDP or specialized transport      |
| Peer-to-peer real-time communication         | WebRTC, depending on requirements |

Don't automatically select WebSockets just because someone said "real-time."

For example, a dashboard that only receives server updates might work perfectly well with SSE.

## 2.2 Audience

Next, understand who should receive an event.

Ask:

- Does everyone receive every event?
- Do users subscribe to specific groups?
- Are updates targeted to a particular user?
- Can a user have multiple devices connected?
- Can a user belong to multiple groups?
- Should a message reach all devices or only one?

This determines the audience-resolution architecture.

| Audience                              | Typical routing model      |
| ------------------------------------- | -------------------------- |
| Everyone                              | Broadcast                  |
| All users watching a particular stock | Symbol subscribers         |
| Members of a chat conversation        | Group fanout               |
| A particular user                     | User-to-connections lookup |
| A particular device                   | Connection/device lookup   |
| A particular region or tenant         | Region/tenant routing      |

Remember:

Audience selection and connection routing are two different operations.

For example:

```
Who should receive this?
    |
    v
All members of conversation-123
    |
    v
Which members are online?
    |
    v
Which push servers own their connections?
    |
    v
Send to those push servers
```

## 2.3 Message semantics

This is probably the most important category.

Not all updates have the same meaning.

Ask:

- Can an update be dropped?
- Can the latest update replace previous updates?
- Must every event be delivered?
- Must events be delivered in order?
- Can the client recover from a missed update?
- Does an event represent a change or the latest state?

Let's classify messages.

| Message category             | Example              | What matters                 |
| ---------------------------- | -------------------- | ---------------------------- |
| Ephemeral                    | Typing indicator     | Freshness                    |
| Latest state                 | Current stock price  | Latest value                 |
| Durable history              | Chat message         | No permanent loss            |
| Business obligation          | Payment confirmation | Must eventually be processed |
| Ordered operation            | Document edit        | Correct sequence             |
| Authoritative session update | Game state           | Consistent session ownership |

This classification tells you more about the architecture than the word "real-time."

We'll use these categories throughout the design.

## 2.4 Offline behavior

Ask:

- What happens when the recipient is offline?
- Should missed events be delivered later?
- Can the client fetch the latest state?
- Does the system need an inbox?
- How long must events be retained?
- Does the client need to resume from a cursor?

Different requirements lead to different architectures.

Case A: Only the latest state matters

A stock price changes from ₹100 to ₹101 to ₹102 while the client is offline.

When the client reconnects, fetching ₹102 may be sufficient.

You don't necessarily need to replay ₹100 and ₹101.

Case B: Every event matters

A user receives three payment notifications while offline.

The system must retain those notifications and expose them when the user reconnects.

A latest-state cache is insufficient.

Case C: Every operation matters

A collaborative editor client missed five document operations.

It may need to retrieve those five operations in order, or receive a fresh snapshot.

The recovery strategy is part of the product semantics.

## 2.5 Acknowledgements

Ask:

- Does the system need to know that a client received the message?
- Does it need to know that the client processed it?
- Does it need to know that the user saw it?
- Should delivery be retried until acknowledged?

These are different guarantees.

```
Server sent message
       |
       v
Client received message
       |
       v
Client processed message
       |
       v
Client persisted message
       |
       v
User read message
```

A successful WebSocket write does not prove that any of the later stages happened.

Clarify exactly which acknowledgement matters.

## 2.6 Ordering and consistency

Ask:

- Must events be processed in order?
- What defines the ordering boundary?
- Is ordering needed globally, per user, per group, or per entity?
- Can clients temporarily see stale data?
- Is eventual consistency acceptable?

For example:

A chat conversation may require:

```
Message 1
Message 2
Message 3
```

A dashboard may be happy to show the newest aggregate, even if it skips intermediate values.

A collaborative document may require strict sequencing or a conflict-resolution algorithm such as OT or CRDT.

Don't assume every application needs global ordering.

Usually, ordering is required only within a particular entity or stream.

## 2.7 Scale and traffic patterns

Ask:

- How many concurrent connections?
- How many messages per second?
- How many recipients per event?
- What is the average payload size?
- Are traffic spikes predictable?
- Is traffic uniformly distributed or concentrated around a few popular entities?
- Are there extremely large groups or broadcast audiences?

One million connected users doesn't necessarily mean a difficult system.

One million users receiving one update per hour is very different from one million users receiving 20 updates per second.

Also distinguish:

- Events produced.
- Recipient deliveries.
- Push-server dispatches.
- Network frames sent.

These can have radically different counts.

## 2.8 Regions, security, and isolation

Finally, clarify:

- Does the system operate in multiple regions?
- Must users connect to the nearest region?
- Is user data restricted to a particular region?
- Can users access only specific groups or tenants?
- What happens when permissions change?
- Are messages sensitive or regulated?

These questions influence regional routing, ownership, authentication, authorization, and data placement.

### Which questions should you prioritize in an interview?

You don't need to ask 30 questions before starting.

Ask questions that change the architecture.

A useful priority order:

| Priority          | Questions                                                                             |
| ----------------- | ------------------------------------------------------------------------------------- |
| Critical          | Who sends updates, who receives them, and what must happen if a recipient is offline? |
| Critical          | Can events be dropped? Is ordering required?                                          |
| Critical          | How many connections, messages, and recipients are expected?                          |
| Important         | Does the client need delivery acknowledgements or replay?                             |
| Important         | Are there multiple regions or very large audiences?                                   |
| Important         | Can a user have multiple devices and sessions?                                        |
| Context-dependent | What are the retention, compliance, and exact latency requirements?                   |

The principle is simple:

Ask the minimum questions required to avoid choosing the wrong architecture.

# 3. The most useful mental model: classify the event first

Suppose a business service produces an event.

Before deciding which broker to use, classify that event.

I would use the following decision process.

```
                New event
                    |
                    v
        Is it only a temporary signal?
              /           \
            Yes            No
             |              |
             v              v
        Ephemeral       Does only the
        delivery        latest state matter?
                         /        \
                       Yes         No
                        |           |
                        v           v
                  Latest state   Must every event
                    delivery      be retained?
                                  /       \
                                Yes        No
                                 |          |
                                 v          v
                             Durable    Best-effort
                             history    based on product
                                 |
                                 v
                         Is ordering essential?
                              /      \
                            Yes       No
                             |         |
                             v         v
                        Ordered    Durable,
                        operation  unordered or
                                   independently ordered
```

Let's understand the practical consequences of each category.

## 3.1 Ephemeral events

Examples:

- Typing indicator.
- Cursor movement.
- Online presence.
- Temporary game movement.
- A transient dashboard animation.

Typical design:

```
Producer
   |
   v
Pub/Sub
   |
   v
Push servers
   |
   v
Connected clients
```

The system may drop events when a subscriber is offline.

You might also:

- Coalesce rapid updates.
- Drop stale queued updates.
- Avoid storing every event in a database.
- Fetch current state when the client reconnects.

A durable per-user delivery record for every cursor movement would be wasteful.

## 3.2 Latest-state events

Examples:

- Current stock price.
- Current vehicle location.
- Current CPU utilization.
- Latest temperature reading.

The fundamental requirement is usually:

> Give me the most recent known state.

A typical architecture:

```
Source
  |
  v
Ingestion / Processing
  |
  +-------> Latest-state store
  |
  v
Subscription routing
  |
  v
Push servers
  |
  v
Clients
```

When a client reconnects:

```
Reconnect
   |
   v
Fetch latest snapshot
   |
   v
Resume live updates
```

This avoids replaying thousands of obsolete intermediate updates.

Be careful, though: a stock price update and a stock trade are not interchangeable. The latest price may be a snapshot, while trades can be important historical events that must not be lost.

## 3.3 Durable events

Examples:

- Chat messages.
- Payment status changes.
- Important notifications.
- Order lifecycle changes.

Typical design:

```
Business transaction
       |
       v
Database + Outbox
       |
       v
Durable event stream
       |
       v
Router
       |
       v
Delivery system
       |
       v
Push servers
       |
       v
Clients
```

You need a durable recovery path.

Depending on the product, this might be:

- An inbox.
- A chat history database.
- An event log.
- A durable delivery record.
- A client synchronization cursor.

A message being pushed is not the same as a message being safely retained.

## 3.4 Ordered operations

Examples:

- Collaborative document edits.
- Account balance changes.
- Inventory updates.
- State transitions in a workflow.

Typical design:

```
Client operation
      |
      v
Authoritative processor
      |
      v
Assign sequence number
      |
      v
Persist operation
      |
      v
Publish operation
      |
      v
Push to subscribers
```

Here, order is a property of the application, not something you can blindly delegate to WebSockets or Kafka.

You need an authoritative source of sequence numbers or a well-defined ordering algorithm.

## 3.5 Authoritative session events

Examples:

- Multiplayer game state.
- Live auction session.
- Real-time collaborative session with a single owner.

A specialized server owns the session and validates changes.

```
Clients
   |
   v
Session / Game Server
   |
   +-------> Authoritative state
   |
   +-------> State updates
   |
   v
Subscribers
```

A generic Kafka consumer group is not a replacement for a low-latency authoritative game loop.

# 4. Derive the architecture step by step

Now let's create a repeatable algorithm.

Whenever you receive a real-time system-design problem, go through these steps.

## Step 1: Identify the core use cases

Write down what users actually do.

For a chat application:

1. Send a message.
2. Receive a message.
3. See typing indicators.
4. See online presence.
5. Recover messages after reconnecting.
6. Receive messages on multiple devices.

Don't design all of these as one generic event.

Each use case might have different delivery guarantees.

## Step 2: Classify each event

Build a table.

| Event                | Audience             | Type         | Durability  | Ordering          |
| -------------------- | -------------------- | ------------ | ----------- | ----------------- |
| Chat message         | Conversation members | Durable      | Yes         | Per conversation  |
| Typing               | Conversation members | Ephemeral    | No          | Usually no        |
| Presence             | Interested contacts  | Latest state | Optional    | Usually no        |
| Message read receipt | Conversation members | State change | Usually yes | Context-dependent |

This table begins to define the architecture.

## Step 3: Choose the transport

Now consider how information travels between the client and server.

| Requirement                                              | Transport                              |
| -------------------------------------------------------- | -------------------------------------- |
| Browser mainly receives updates                          | SSE                                    |
| Browser sends and receives frequent updates              | WebSocket                              |
| Mobile app needs bidirectional low-latency communication | Usually WebSocket                      |
| Device periodically sends telemetry                      | HTTP or MQTT, depending on system      |
| Highly latency-sensitive game traffic                    | UDP/custom protocol may be appropriate |
| Browser needs real-time peer-to-peer communication       | WebRTC may be appropriate              |

WebSocket and SSE are client-server transport mechanisms.

They do not solve:

- Message durability.
- Fanout.
- Ordering.
- Offline delivery.
- Exactly-once business processing.

Those are separate architectural concerns.

## Step 4: Design audience resolution

Determine how you identify recipients.

Ask:

1. What identifies the audience?
2. Where does authoritative membership live?
3. How do we identify active connections?
4. How do we find the push servers that own those connections?
5. How do we efficiently dispatch to them?

For a chat group:

```
conversationId
      |
      v
Conversation membership
      |
      v
User IDs
      |
      v
Connection registry
      |
      v
Push server IDs
      |
      v
Local connections
```

For a huge sports audience, resolving millions of individual users for every update may be too expensive. A regional or per-server subscription interest index can be more appropriate.

## Step 5: Identify the authoritative source of truth

For every important kind of state, ask:

> Which component owns the truth?

Example:

| Data                        | Source of truth                   |
| --------------------------- | --------------------------------- |
| User account                | User service database             |
| Group membership            | Group/chat service database       |
| Active socket               | Push server memory                |
| Connection ownership        | Connection registry               |
| Chat message                | Chat database                     |
| Delivery attempt            | Delivery database, if needed      |
| Latest stock price          | Market-data state processor/store |
| Document operation sequence | Document authority/log            |

This avoids the common mistake of making the push gateway responsible for everything.

## Step 6: Choose the messaging pattern

Now select the messaging semantics.

| Need                                              | Pattern                         |
| ------------------------------------------------- | ------------------------------- |
| Every subscriber independently gets a copy        | Pub/Sub                         |
| One worker among several processes each task      | Competing-consumer queue        |
| Retained events and independent consumer progress | Event log/stream                |
| Immediate communication with a known server       | Direct RPC or targeted dispatch |
| Durable retryable work                            | Queue or durable work table     |
| High-volume ordered processing by entity          | Partitioned event stream        |

Remember the subtle but important Kafka distinction:

```
Same consumer group:
    Consumers share partitions and work.

Different consumer groups:
    Each group maintains its own progress
    and can independently receive the events.
```

Never choose a Kafka consumer-group model without knowing whether you need work distribution or broadcast.

## Step 7: Decide the delivery guarantee

Use a deliberate policy for each event type.

| Event                | Typical guarantee                                    |
| -------------------- | ---------------------------------------------------- |
| Typing indicator     | Best effort                                          |
| Cursor movement      | Best effort / coalesced                              |
| Latest stock price   | Recover from snapshot                                |
| Chat message         | Durable history + client synchronization             |
| Payment notification | Durable delivery obligation                          |
| Document edit        | Durable ordered operation                            |
| Game movement        | Freshness-oriented, with authoritative session rules |

For durable events, design:

- Stable event IDs.
- Idempotent consumers.
- Outbox where database and broker publication must stay consistent.
- Retries.
- Client acknowledgements where required.
- Offline recovery.
- Expiry or retention policy.

## Step 8: Add connection and subscription management

Now design the connection registry.

Typical structures:

```
userId -> connectionIds

connectionId -> {
    userId,
    serverId,
    deviceId,
    expiry,
    generation
}

serverId -> connectionIds
```

And, where needed:

```
groupId -> activePushServerIds
```

The push server itself holds the actual socket and local subscription information.

Don't store socket objects in Redis.

## Step 9: Add failure behavior

Do not wait until the end of the interview to think about failures.

For each component, ask:

> What happens if this component disappears right now?

| Failure                 | Expected design response                                                |
| ----------------------- | ----------------------------------------------------------------------- |
| Push server crashes     | Clients reconnect elsewhere; recover missed data                        |
| Redis unavailable       | Existing local sockets can continue; cross-server routing degrades      |
| Kafka unavailable       | Durable publishing waits/retries; ephemeral flow may degrade separately |
| Router crashes          | Durable events remain available for another router                      |
| Client goes offline     | Retain important data; expire or drop ephemeral data                    |
| Client ACK is lost      | Retry with the same event ID; deduplicate                               |
| Delivery worker crashes | Reclaim leased work; prevent unsafe concurrent completion               |
| Region unavailable      | Route to another region if the data ownership model permits it          |

The most important question is not whether a system can fail.

It is whether a failure causes:

- Permanent data loss.
- Duplicate business effects.
- Incorrect ordering.
- Stale routing.
- A temporary delay.
- A complete outage.

These have very different consequences.

## Step 10: Scale the actual bottleneck

Don't scale every component just because traffic grows.

Identify which dimension is overloaded.

| Symptom                              | Likely bottleneck      | Possible solution                               |
| ------------------------------------ | ---------------------- | ----------------------------------------------- |
| Too many concurrent connections      | Push servers / network | More push servers, connection capacity tuning   |
| Registry lookups are expensive       | Redis / routing        | Sharding, batching, better indexes              |
| Event ingestion lag grows            | Broker / ingestion     | More partitions, producers, processing capacity |
| A few groups dominate traffic        | Hot entities           | Dedicated handling, hierarchical fanout         |
| One event has millions of recipients | Fanout                 | Regional and server-level fanout                |
| Network bandwidth saturated          | Egress                 | Coalescing, compression, batching               |
| One region is overloaded             | Regional capacity      | Regional balancing and traffic placement        |

Notice that adding more WebSocket servers does not fix a Kafka hot partition.

Adding more Kafka consumers does not fix a single hot key if that key must remain on one ordered partition.

Scaling must match the bottleneck.

## Step 11: Add multi-region architecture

Only after you understand the single-region design, ask:

- Where does business data live?
- Where do clients connect?
- Where are messages produced?
- How do regions discover active connections?
- Does a user have one home region?
- Can a message be produced in one region and consumed in another?
- What happens during a cross-region network partition?

A useful distinction:

The user's connection region and the user's business-data home region are not necessarily the same.

For instance:

```
Business service in Region A
          |
          v
Durable event stream
          |
          v
Regional distribution
       /       \
      v         v
Region A     Region B
      |         |
      v         v
Push fleet   Push fleet
      |         |
      v         v
 Clients     Clients
```

For sensitive or strongly consistent business operations, establish ownership and conflict-resolution rules rather than assuming that two regions can safely write the same data independently.

## Step 12: Finish with security and operations

Make sure the architecture enforces:

- Authentication when connecting.
- Authorization when subscribing.
- Authorization when publishing.
- Tenant and group isolation.
- Permission revocation.
- Rate limits.
- Message-size limits.
- Connection admission control.
- TLS.
- Origin checks for browser WebSocket connections.
- Audit logs for sensitive actions.

Also monitor:

- Active connections.
- Connection churn.
- Reconnect rate.
- Heartbeat failures.
- Registry errors.
- Broker lag.
- Fanout latency.
- Delivery latency.
- Client ACK rate.
- Retry count.
- Duplicate count.
- Slow-consumer disconnects.

# 5. Technology selection: a practical decision table

Now let's consolidate when to use what.

## 5.1 WebSocket vs SSE vs other transports

| Technology          | Prefer it when                                               | Main limitation                                                          |
| ------------------- | ------------------------------------------------------------ | ------------------------------------------------------------------------ |
| WebSocket           | Continuous bidirectional communication                       | Reconnect and delivery recovery are largely application responsibilities |
| SSE                 | Mostly server-to-client updates                              | One-way transport                                                        |
| HTTP polling        | Updates are infrequent or periodic                           | Repeated request overhead and update delay                               |
| Long polling        | Simpler fallback for server-initiated updates                | Request lifecycle and scaling overhead                                   |
| WebRTC              | Peer-to-peer media/data or specialized low-latency use cases | More complex session setup and connectivity                              |
| UDP/custom protocol | Highly latency-sensitive specialized systems                 | Reliability and transport behavior require more application-level design |

## 5.2 Redis vs database vs Kafka

| Technology       | Use it for                                                           | Don't confuse it with                            |
| ---------------- | -------------------------------------------------------------------- | ------------------------------------------------ |
| Redis            | Connection metadata, presence, transient state, fast routing indexes | Permanent business history                       |
| PostgreSQL/MySQL | Business records, inboxes, delivery state, outbox                    | High-volume transient Pub/Sub                    |
| Kafka            | Durable event streams, replay, partitioned processing                | A user connection registry                       |
| RabbitMQ         | Work queues and flexible message routing                             | A universal replacement for a retained event log |
| Redis Pub/Sub    | Fast ephemeral distribution                                          | Reliable offline delivery                        |
| Redis Streams    | Retained stream processing with consumer groups                      | An automatic end-to-end delivery guarantee       |

The underlying lesson:

Choose storage based on the state you need to preserve and the queries you need to perform. Choose messaging based on the distribution and processing semantics you need.

## 5.3 Direct RPC vs broker

This choice deserves attention.

Suppose Router A knows that Push Server 7 owns the recipient's connection.

Should Router A call Push Server 7 directly, or publish a message to a broker?

| Direct RPC                                | Broker-based dispatch                    |
| ----------------------------------------- | ---------------------------------------- |
| Low latency                               | Adds broker hop and operational overhead |
| Straightforward targeting                 | Decouples sender and receiver            |
| Failure handling must be designed         | Can buffer work if broker is durable     |
| Receiver availability matters immediately | Consumer availability can be delayed     |
| Good for immediate targeted delivery      | Good for asynchronous, retryable work    |

A common architecture combines them:

```
Durable event
    |
    v
Router
    |
    v
Resolve target push servers
    |
    +-------> Direct RPC
    |
    +-------> Per-server Pub/Sub
    |
    +-------> Durable delivery queue
```

The right choice depends on the required failure semantics.

You do not need to put every message through Kafka and Redis just to make the architecture look scalable.

# 6. Five reusable architecture patterns

Most real-time push designs can be understood as combinations of a small number of patterns.

## Pattern A: Ephemeral broadcast

Example: typing indicators, temporary status, live cursor movement.

```
Producer
   |
   v
Pub/Sub
   |
   v
Push Server 1     Push Server 2     Push Server 3
   |                  |                  |
   v                  v                  v
Local clients     Local clients     Local clients
```

Use it when:

- Updates are temporary.
- Missed updates can be tolerated.
- The client can recover through a fresh state or no recovery is needed.

## Pattern B: Durable business event delivery

Example: order status change, chat message, payment status.

```
Business DB
    |
    +------> Outbox
                 |
                 v
              Kafka
                 |
                 v
              Router
                 |
                 v
        Durable delivery records
                 |
                 v
          Delivery workers
                 |
                 v
            Push servers
                 |
                 v
              Clients
```

This pattern decouples business event creation from client delivery.

## Pattern C: Reliable user notification

Example: a payment confirmation that must be available to an offline user.

```
Business Event
      |
      v
Notification Service
      |
      v
Create durable inbox item
      |
      v
Create delivery work
      |
      +-----------> WebSocket
      |
      +-----------> Mobile Push
      |
      +-----------> Email
      |
      v
Client opens application
      |
      v
Fetch durable inbox
```

The inbox is the user's durable notification record.

A WebSocket notification is a prompt to refresh or display the notification. It should not be the only place where the notification exists.

## Pattern D: Ordered entity updates

Example: collaborative editing or a sequence-sensitive workflow.

```
Producers
    |
    v
Entity-specific ordering boundary
    |
    v
Assign sequence
    |
    v
Persist operation
    |
    v
Partitioned event stream
    |
    v
Router / subscribers
    |
    v
Clients
```

The system needs an ordering authority, a replay mechanism, and a way to detect missing sequence numbers.

## Pattern E: Massive fanout

Example: a popular sports final watched by millions of users.

```
                 Event
                   |
                   v
             Regional router
                   |
          +--------+--------+
          |        |        |
          v        v        v
        Region A Region B Region C
          |        |        |
          v        v        v
       Push fleet Push fleet Push fleet
          |        |        |
          v        v        v
        Clients  Clients  Clients
```

Do not automatically create millions of independent delivery records for a broadcast event.

When all subscribers are supposed to receive the same update, hierarchical broadcast can be much more efficient.

# 7. Apply the framework to a new problem

Let's see whether we can derive an architecture without memorizing a named design.

## Problem: Design a real-time IoT monitoring platform

Imagine an organization has:

- 5 million IoT devices.
- Devices deployed across many locations.
- Each device sends temperature, pressure, and health telemetry.
- Operators monitor dashboards.
- Critical alerts must be delivered reliably.
- Users can monitor a device, a building, or an entire site.

How should we design it?

### Step 1: Identify the use cases

There are four:

1. Devices continuously send telemetry.
2. Operators view live telemetry.
3. Operators receive critical alerts.
4. Operators retrieve historical measurements.

Already, we can see that treating everything as one generic WebSocket message is wrong.

### Step 2: Classify the events

| Event                      | Category                       | Required behavior                                          |
| -------------------------- | ------------------------------ | ---------------------------------------------------------- |
| Temperature measurement    | Latest state + historical data | Latest value for dashboard; historical retention if needed |
| Device heartbeat           | Presence / ephemeral state     | Track device health and last-seen time                     |
| Critical temperature alarm | Durable notification           | Must not be lost                                           |
| Device metadata change     | Durable business event         | Retain and synchronize                                     |
| Dashboard display update   | Latest state                   | Coalesce obsolete updates                                  |

### Step 3: Select the device ingestion protocol

Devices are primarily sending data to the platform.

Depending on device capability and requirements, consider MQTT or HTTP.

For this example, let's choose MQTT for telemetry ingestion.

This does not mean MQTT is automatically required for the dashboard clients. Different parts of the system can use different protocols.

### Step 4: Design ingestion and processing

```
IoT Devices
     |
     v
MQTT Broker / Ingestion Layer
     |
     v
Telemetry Processing
     |
     +---------------------> Latest State Store
     |
     +---------------------> Historical Event Stream
     |
     +---------------------> Alert Evaluation
```

The latest state store might hold:

```
deviceId -> {
    temperature,
    pressure,
    status,
    lastUpdatedAt,
    sequenceNumber
}
```

The historical stream or database stores measurements according to the retention and analytics requirements.

Notice the distinction:

- Latest state is optimized for fast retrieval.
- Historical data is optimized for queries and retention.

These are not necessarily the same database or data structure.

### Step 5: Design critical alerts

Suppose a sensor crosses a dangerous temperature threshold.

That is no longer just a latest-state update.

It creates a business obligation.

```
Alert Evaluation
       |
       v
Critical condition detected
       |
       v
Persist alert + outbox
       |
       v
Durable alert event
       |
       v
Notification Service
       |
       v
Create user inbox / delivery work
       |
       v
Push to connected operators
```

If an operator is offline, the alert remains available.

The live push is only one delivery path.

### Step 6: Design audience resolution

Suppose an operator monitors Building 12.

We need:

```
Building 12
     |
     v
Devices in Building 12
     |
     v
Live subscriptions
     |
     v
Push servers that have
subscribers for Building 12
     |
     v
Local dashboard connections
```

For a huge building with many dashboards, maintain a derived index of active push servers interested in that building.

Do not repeatedly resolve every operator's full device membership for every sensor update if the audience is large and long-lived.

Of course, authorization must still be enforced. A subscription index is an optimization, not an authorization authority.

### Step 7: Choose the dashboard transport

Dashboards mostly receive updates.

SSE may be sufficient.

If the dashboard also sends frequent bidirectional control operations or interactive commands, WebSocket may be preferable.

Device ingestion and operator dashboard transport are independent choices.

### Step 8: Handle reconnect

When an operator reconnects:

```
Reconnect
   |
   v
Authenticate and authorize
   |
   v
Load current snapshot
   |
   v
Subscribe to live building updates
   |
   v
Continue receiving updates
```

We need to avoid a race where a new update occurs between loading the snapshot and establishing the subscription.

A common strategy is:

1. Establish or register the live subscription.
2. Obtain a snapshot with a version or sequence marker.
3. Discard live updates older than the snapshot.
4. Apply newer updates in sequence.
5. Fetch missing data or a fresh snapshot if a gap is detected.

The exact protocol depends on how the telemetry state is versioned.

### Step 9: Scale it

Our bottlenecks might include:

- MQTT connection capacity.
- Telemetry ingestion rate.
- Stream processing capacity.
- Hot buildings with many subscribers.
- Historical database write volume.
- Push-server network egress.

Each requires a different scaling strategy.

Adding more WebSocket servers does not necessarily improve telemetry ingestion throughput.

### Step 10: Handle failure

- Device disconnects: mark stale after a defined heartbeat threshold.
- MQTT broker fails: devices reconnect with jitter; broker deployment and persistence policies protect accepted messages.
- Stream processor fails: resume from retained offsets.
- Latest-state store fails: rebuild or restore from durable sources where feasible.
- Push server fails: dashboards reconnect and fetch fresh snapshots.
- Notification delivery fails: retain the alert and retry according to policy.

### Final architecture

```
                   IoT Devices
                        |
                        v
                 MQTT / Ingestion
                        |
                        v
               Telemetry Processing
                        |
           +------------+-------------+
           |            |             |
           v            v             v
      Latest State   Event Stream   Alert Engine
           |            |             |
           |            v             v
           |       Historical DB   Durable Alerts
           |                          |
           |                          v
           |                   Notification Service
           |                          |
           +------------+-------------+
                        |
                        v
                 Subscription Router
                        |
                        v
                  Push Servers
                        |
                        v
                  Operator UI
```

We derived the architecture from the requirements.

We did not start by saying, "IoT requires MQTT, Redis, Kafka, and WebSockets."

That is the habit you want to develop.

# 8. Common design mistakes to avoid

These are mistakes that frequently appear in real-time architecture discussions.

| Mistake                                                     | Why it is problematic                               | Better approach                                    |
| ----------------------------------------------------------- | --------------------------------------------------- | -------------------------------------------------- |
| Treat every update as durable                               | High storage, write, and retry costs                | Classify events by semantics                       |
| Assume WebSocket guarantees delivery                        | Transport delivery is not application processing    | Use durable state, ACKs, and replay where required |
| Use Redis Pub/Sub for important offline notifications       | Disconnected subscribers miss events                | Persist an inbox or durable delivery work          |
| Put all push servers in one Kafka group expecting broadcast | Consumers share work                                | Use a router or independent subscription groups    |
| Create one Kafka topic per user or group                    | Topic/partition metadata and operational complexity | Shared topics with keys and application routing    |
| Store socket objects in Redis                               | Sockets belong to the owning process                | Store routing metadata in Redis                    |
| Treat user online as a single boolean                       | A user may have multiple devices and sessions       | Derive presence from active connections            |
| Use the business database in every push server              | Tight coupling and connection pressure              | Keep business ownership in domain services         |
| Retry indefinitely                                          | Can amplify overload and hide permanent errors      | Retry budgets, backoff, expiry, and DLQ            |
| Assume Kafka ordering is global                             | Ordering is generally partition-scoped              | Define ordering per entity                         |
| Broadcast huge events to every client separately            | Wasteful fanout                                     | Hierarchical or regional fanout                    |
| Build multi-region active-active without ownership rules    | Can create conflicting writes                       | Define home-region or conflict-resolution strategy |
| Use a durable broker for every ephemeral update             | Unnecessary latency and operational cost            | Use best-effort distribution where appropriate     |
| Assume a successful socket write means user saw the message | The client may disconnect before processing it      | Define acknowledgement semantics                   |

# 9. The interview framework: what to actually draw

When asked to design a new real-time system, don't immediately draw the complete architecture.

Build it in layers.

### Layer 1: The basic product flow

Draw:

```
Producers -> System -> Recipients
```

Identify what is being produced and what recipients expect.

### Layer 2: The connection layer

Add:

```
Clients
   |
Load Balancer
   |
Push Servers
```

Explain:

- How connections are established.
- How connections remain associated with their owning server.
- How reconnect works.
- How authentication is handled.

### Layer 3: The routing layer

Add:

```
Audience
   |
   v
Audience Resolution
   |
   v
Connection Registry
   |
   v
Target Push Servers
```

Explain how group membership, user connections, and server ownership are discovered.

### Layer 4: The messaging layer

Add the appropriate pattern:

```
Producer -> Pub/Sub / Queue / Event Stream -> Router
```

Explain why you selected that pattern.

### Layer 5: The reliability layer

Add only the reliability components the requirements demand:

- Outbox.
- Durable event stream.
- Delivery database.
- Retry workers.
- Client ACKs.
- Replay or snapshot recovery.
- Idempotency.

### Layer 6: The scaling layer

Discuss:

- Connection capacity.
- Network egress.
- Fanout.
- Hot partitions.
- Large groups.
- Regional routing.

### Layer 7: The failure and operational layer

Discuss:

- Node crashes.
- Redis and broker outages.
- Reconnection storms.
- Backpressure.
- Monitoring and alerting.
- Deployment and draining.

This gives the interviewer a clear progression: requirements → correctness → architecture → reliability → scale.

# 10. A reusable design checklist

Use this checklist whenever you face a new real-time system design problem.

## Requirements

- Who produces events?
- Who receives them?
- Is communication unidirectional or bidirectional?
- What are the event categories?
- What is the required latency?
- How many concurrent connections?
- How many events and recipient deliveries per second?

## Transport and connection management

- WebSocket, SSE, HTTP, MQTT, or another transport?
- How is a connection authenticated?
- How is a connection registered?
- Can a user have multiple active connections?
- How is liveness detected?
- How does reconnect work?
- What happens during server draining?

## Routing

- Is the audience global, group-based, user-based, or device-based?
- Where does audience membership live?
- How do we map recipients to push servers?
- How do we handle very large groups?
- How do we handle membership and permission changes?

## Messaging and reliability

- Pub/Sub, work queue, event stream, or direct RPC?
- Can messages be dropped?
- Must offline recipients receive missed events?
- Is replay required?
- Is ordering required, and within what boundary?
- Are event IDs stable?
- Are consumers idempotent?
- Are retries bounded?
- What exactly does an ACK mean?

## Storage

- What is the source of truth for each data type?
- What is ephemeral?
- What is durable?
- Is a separate delivery database necessary?
- Is an outbox necessary?
- What is the retention policy?
- How is the connection registry reconstructed?

## Scale and failure

- What is the first bottleneck?
- Can a single key or audience become hot?
- Are we counting events, deliveries, or network frames correctly?
- What happens if each major component fails?
- Can the system recover after losing a region?
- Is traffic isolated by tenant or workload?
- Are there retry storms or cascading failure risks?

You do not need to mechanically recite every item in every interview. Use the checklist to make sure you haven't missed an architectural decision.

# 11. The most important mental model to remember

After all these chapters, I want you to retain one way of thinking.

A real-time push system is not fundamentally a WebSocket system.

It is a system that must answer six questions:

| Question                                        | Architectural responsibility             |
| ----------------------------------------------- | ---------------------------------------- |
| Who is communicating?                           | Identity and authentication              |
| Who should receive an event?                    | Audience resolution                      |
| Where are those recipients connected?           | Connection registry and routing          |
| How should the event be transported?            | WebSocket, SSE, broker, RPC, etc.        |
| What must happen if delivery fails?             | Durability, retry, ACK, replay           |
| How does the system survive growth and failure? | Scaling, redundancy, isolation, recovery |

And the entire design can be summarized as:

```
                    BUSINESS EVENT
                          |
                          v
                  CLASSIFY THE EVENT
                          |
            +-------------+-------------+
            |                           |
            v                           v
     Ephemeral / Latest             Durable /
          State                    Ordered Event
            |                           |
            v                           v
      Best-effort path           Persist / Outbox
            |                           |
            v                           v
            +-----------> ROUTING <-----+
                             |
                             v
                      RESOLVE AUDIENCE
                             |
                             v
                    RESOLVE CONNECTIONS
                             |
                             v
                      TARGET PUSH NODES
                             |
                             v
                       CLIENT DELIVERY
                             |
                             v
                 ACK / REPLAY / SNAPSHOT
                    AS REQUIRED
```

Do not memorize a complete architecture for every application. Learn to classify requirements and derive the necessary components.

Once you can do that, chat, stock updates, games, tracking, dashboards, and notifications stop looking like completely different problems. They become combinations of patterns you already understand.

# Where we go from here

We've completed the conceptual course. The next valuable step is to apply it to unfamiliar interview problems without being given the architecture upfront.

A good exercise would be:

1. I give you a problem statement.
2. You identify the important requirements and ask clarifying questions.
3. You propose the architecture.
4. We challenge every component: why it's needed, what breaks without it, and what alternatives exist.
5. We progressively add scale, reliability, and multi-region constraints.

That is where the concepts turn into actual system-design skill.
