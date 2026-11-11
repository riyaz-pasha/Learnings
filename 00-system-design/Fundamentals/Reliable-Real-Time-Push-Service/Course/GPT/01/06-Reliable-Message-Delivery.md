# Chapter 6 — Reliable Message Delivery

This is arguably the most important chapter for building a production-grade real-time push system.

So far, we've built:

- WebSocket/SSE connections.
- Multiple push servers.
- Audience-based routing.
- Kafka/Pub-Sub/message queues.
- A distributed connection registry.
- Group and individual routing.

But there's one fundamental problem we haven't solved.

How do we guarantee that an important message doesn't silently disappear when something fails?

Let's start with a very simple example.

## 1. The problem: What does successful delivery actually mean?

Imagine Alice sends Bob a chat message.

```
Alice: Hello Bob!
```

Our system follows this flow:

```
Alice
  |
  ▼
Chat Service
  |
  ▼
Chat Database
  |
  ▼
Kafka
  |
  ▼
Routing Worker
  |
  ▼
Connection Registry
  |
  ▼
Push Server 1
  |
  ▼
WebSocket
  |
  ▼
Bob's Browser
```

Looks straightforward.

But let's introduce failures.

### Failure 1: Chat service crashes after saving the message

The message is saved in the database.

But the service crashes before publishing the event to Kafka.

Result:

```
Database: Message exists.

Kafka: No event.

Bob: No real-time notification.
```

### Failure 2: Router crashes after consuming the Kafka event

Kafka contains the event.

The router consumes it, but crashes before committing its offset.

Kafka may deliver the event again.

Result:

```
First attempt: Event processed partially.

Second attempt: Same event processed again.
```

We might send duplicates.

### Failure 3: Push server crashes before writing to the socket

The router dispatches the message to Push Server 1.

Push Server 1 crashes.

Result:

```
Kafka: Event exists.

Router: Dispatch attempted.

Push Server: Did not deliver.
```

### Failure 4: Socket write succeeds, but Bob never processes the message

The push server writes the message.

Immediately afterward, Bob loses internet connectivity.

The message might have reached the operating system's network buffers but never reached Bob's application.

Result:

```
Push Server: Write succeeded.

Bob's application: Did not process the message.
```

### Failure 5: Bob processes the message, but the ACK gets lost

Bob receives the message and processes it.

He sends an acknowledgement.

But the ACK never reaches the server.

The server doesn't know that Bob processed it.

It retries.

Bob receives the same message again.

Result:

```
Bob processes message 101.

ACK is lost.

Server retries message 101.

Bob receives message 101 again.
```

These failures tell us something important.

Reliable message delivery cannot be achieved by simply adding retries to WebSocket sends.

We need to design reliability across the complete pipeline.

# Part A — Understand Delivery Guarantees

## 2. Three fundamental delivery semantics

Before building the architecture, we need to understand three terms.

### A. At-most-once delivery

A message is delivered zero or one times.

```
Send message
    |
    ▼
Attempt delivery
    |
    ├── Success -> Done
    |
    └── Failure -> Discard
```

We never retry after a failure.

Therefore:

- No duplicates caused by retries.
- Messages can be lost.

Example:

A typing indicator.

```
Alice is typing...
```

If Bob misses it, there is no reason to deliver it later.

At-most-once is acceptable for temporary events.

### B. At-least-once delivery

We keep retrying until we receive the required acknowledgement or reach a defined terminal policy.

```
Send message
    |
    ▼
Attempt delivery
    |
    ├── ACK received -> Done
    |
    └── No ACK -> Retry
                    |
                    ▼
                 Attempt again
```

This gives us:

- Better protection against message loss.
- Possible duplicate delivery.

Example:

A chat message.

Suppose the server doesn't receive an ACK.

It cannot confidently conclude that the client never received the message.

Therefore, it retries.

The client might see the same message more than once unless duplicate detection is implemented.

### C. Exactly-once processing

This is more subtle.

The desired behavior is:

> Regardless of retries, the logical effect of processing a message happens only once.

For example, Bob should not create two copies of the same chat message just because the server retried it.

This is achievable for many applications through:

- Unique event IDs.
- Idempotent operations.
- Durable deduplication records.
- Atomic processing and acknowledgement.

But we must be careful with the phrase "exactly once."

There is no general guarantee that an arbitrary message travels through the network exactly once, reaches the client exactly once, and is processed exactly once without duplicates or loss.

In distributed systems, the practical approach is:

At-least-once delivery + idempotent processing = effectively-once business effects.

This distinction is fundamental.

## 3. Compare the three guarantees

| Property                | At-most-once                | At-least-once                            | Effectively-once processing                   |
| ----------------------- | --------------------------- | ---------------------------------------- | --------------------------------------------- |
| Can lose messages?      | Yes                         | Not while retry/recovery guarantees hold | Not while retry/recovery guarantees hold      |
| Can deliver duplicates? | No retry-induced duplicates | Yes                                      | Duplicate inputs are tolerated                |
| Needs acknowledgement?  | Not necessarily             | Usually                                  | Usually                                       |
| Needs idempotency?      | Not necessarily             | Yes, where duplicates matter             | Essential                                     |
| Complexity              | Low                         | Medium                                   | Higher                                        |
| Typical use             | Typing indicators           | Durable notifications                    | Payments, chat processing, business workflows |

An important qualification:

At-least-once is not a magic guarantee either.

It depends on retaining the message, retrying correctly, not deleting it prematurely, and defining what happens after retry exhaustion.

We will design those mechanisms.

# Part B — First Separate the Different Messages

## 4. Not every message in the pipeline is the same thing

Consider this chat application.

Alice sends:

```
Hello Bob!
```

We have several distinct objects.

1\. Business message

This is the actual chat message.

```
{
  "messageId": "msg-101",
  "conversationId": "conv-10",
  "senderId": "alice",
  "text": "Hello Bob!"
}
```

It must be saved in the chat database.

2\. Domain event

This announces that a chat message was created.

```
{
  "eventId": "evt-101",
  "type": "MESSAGE_CREATED",
  "messageId": "msg-101",
  "conversationId": "conv-10"
}
```

It is published to the event stream.

3\. Delivery request

This asks the push infrastructure to notify a particular recipient.

```
{
  "deliveryId": "del-101",
  "recipientId": "bob",
  "messageId": "msg-101",
  "eventId": "evt-101"
}
```

4\. Transport frame

This is what travels through the WebSocket.

```
{
  "type": "NEW_MESSAGE",
  "messageId": "msg-101",
  "conversationId": "conv-10",
  "text": "Hello Bob!"
}
```

5\. Client acknowledgement

This says that Bob's application processed a message.

```
{
  "type": "MESSAGE_ACK",
  "messageId": "msg-101",
  "status": "PROCESSED"
}
```

These objects solve different problems.

| Object           | Purpose                             | Authority                |
| ---------------- | ----------------------------------- | ------------------------ |
| Business message | Actual application data             | Chat database            |
| Domain event     | Communicate that something happened | Event stream             |
| Delivery request | Track an intended delivery          | Delivery system          |
| Transport frame  | Move data over WebSocket/SSE        | Push infrastructure      |
| Client ACK       | Confirm client-side processing      | Client delivery protocol |

A WebSocket frame should not be your only durable copy of an important business message.

That is the first major design principle of this chapter.

# Part C — Prevent Losing Events Between the Database and Kafka

## 5. The dual-write problem

Let's revisit Failure 1.

Alice sends a chat message.

Our chat service performs two operations:

```
1. Save message in database.

2. Publish event to Kafka.
```

Naive implementation:

```
public void sendMessage(ChatMessage message) {

    chatRepository.save(message);

    kafkaProducer.publish(
        new MessageCreatedEvent(message)
    );
}
```

There is a dangerous gap between these two operations.

Consider two failure scenarios.

### Scenario A: Database succeeds, Kafka fails

```
Save to Database       SUCCESS
Publish to Kafka       FAILURE
```

The message exists, but downstream systems never learn about it.

### Scenario B: Kafka succeeds, database fails

```
Save to Database       FAILURE
Publish to Kafka       SUCCESS
```

Now consumers receive an event for a message that doesn't exist in the database.

We have inconsistent state.

Can we solve this by doing:

```
@Transactional
public void sendMessage(...) {

    chatRepository.save(...);

    kafkaProducer.publish(...);
}
```

Not reliably.

A normal database transaction cannot automatically atomically commit a separate Kafka transaction.

They are separate systems with separate commit protocols.

We need a solution.

## 6. Introduce the Transactional Outbox Pattern

This pattern solves the dual-write problem without requiring a distributed transaction between the database and Kafka.

The key idea:

Instead of immediately publishing to Kafka, save the event in the same database transaction as the business data.

Let's redesign.

### Step 1: Create an outbox table

Our chat database has:

```
messages
outbox_events
```

Example:

messages

| message_id | conversation_id | sender | text       |
| ---------- | --------------- | ------ | ---------- |
| msg-101    | conv-10         | Alice  | Hello Bob! |

outbox_events

| event_id | event_type      | payload | status  |
| -------- | --------------- | ------- | ------- |
| evt-101  | MESSAGE_CREATED |         | PENDING |

### Step 2: Save both in one database transaction

```
@Transactional
public void sendMessage(ChatMessage message) {

    chatRepository.save(message);

    outboxRepository.save(
        OutboxEvent.messageCreated(message)
    );
}
```

Now the database transaction ensures:

Either both records are committed, or neither is.

```
            DB Transaction
                  |
          ┌───────┴────────┐
          |                |
          ▼                ▼
      Save Message    Save Outbox Event
          |                |
          └───────┬────────┘
                  |
             COMMIT
```

We have eliminated the inconsistent state between the business message and the event record within the same database.

### Step 3: Introduce an Outbox Publisher

A separate process continuously reads pending outbox events.

```
                Chat Database
                      |
                      ▼
                 Outbox Table
                      |
                      ▼
                Outbox Publisher
                      |
                      ▼
                    Kafka
```

Conceptually:

```
while (true) {

    List<OutboxEvent> events =
        outboxRepository.findPendingEvents();

    for (OutboxEvent event : events) {

        kafkaProducer.publish(event);

        outboxRepository.markPublished(event.id());
    }
}
```

This is simplified pseudocode. A production implementation needs safe claiming, concurrent worker coordination, appropriate Kafka producer settings, and recovery from ambiguous publication outcomes.

### Step 4: What happens if the publisher crashes?

Let's examine the dangerous situation.

```
1. Publisher reads evt-101.

2. Publisher publishes evt-101 to Kafka.

3. Kafka successfully stores evt-101.

4. Publisher crashes before marking it PUBLISHED.
```

When the publisher restarts:

```
evt-101 is still PENDING.

Publisher reads it again.

Publisher publishes it again.
```

We get a duplicate Kafka event.

Is that a problem?

Not necessarily.

We deliberately accept possible duplicates in exchange for avoiding silent event loss.

Every outbox event has a stable `eventId`.

Consumers can use that ID for idempotency.

This gives us a powerful pattern:

```
Business DB Transaction
        |
        ▼
Business Data + Outbox Event
        |
        ▼
Outbox Publisher
        |
        ▼
Kafka
        |
        ▼
Idempotent Consumers
```

### What about CDC?

Instead of polling the outbox table, we can use Change Data Capture (CDC).

A CDC mechanism reads committed database changes from the database's transaction log and publishes the outbox records to Kafka.

For example, a Debezium-based architecture can capture outbox records.

This avoids repeatedly polling the database table.

However, the underlying principle remains the same:

The business record and outbox event are committed atomically in the same database transaction.

### When should we use the Transactional Outbox?

Use it when:

- Losing an event after a successful business transaction is unacceptable.
- A database update must reliably lead to downstream event processing.
- A distributed transaction across the database and message broker is undesirable.

Examples:

- Chat message creation.
- Order placement.
- Payment status changes.
- Notification generation.
- Document operation persistence.

For purely ephemeral events, such as typing indicators, an outbox is usually unnecessary.

# Part D — What Happens After Kafka Receives the Event?

## 7. The consumer processing problem

Now the event is safely in Kafka.

Our router consumes it.

```
Kafka
  |
  ▼
Router Worker
  |
  ▼
Connection Registry
  |
  ▼
Push Server
```

We need to decide when the router commits its Kafka offset.

Consider this sequence:

```
1. Router consumes event-101.

2. Router resolves User A.

3. Router sends request to Push Server 1.

4. Push Server 1 accepts the request.

5. Router crashes before committing Kafka offset.
```

Kafka will eventually redeliver event-101.

The router processes it again.

Push Server 1 may receive a duplicate delivery request.

This is normal in at-least-once processing.

We need stable delivery identifiers and idempotency.

### What should the router acknowledge?

There are two broad designs.

Design A: Fire-and-forget handoff

The router sends a message to Push Server 1 and commits the Kafka offset.

This can be acceptable for best-effort events.

But the router should not confuse a successful network request with durable downstream storage.

Design B: Durable handoff

The router creates a durable delivery request before committing the Kafka offset.

For example:

```
Kafka Event
    |
    ▼
Router
    |
    ▼
Delivery Store
    |
    ▼
Commit Kafka Offset
```

Now the delivery system owns the responsibility of completing the work.

If the push server crashes, the delivery request still exists.

This is the design we should prefer for important messages.

### One important detail

Suppose the router does:

```
1. Persist delivery request.
2. Crash before committing Kafka offset.
```

Kafka sends the event again.

The router attempts to create the same delivery request again.

So we need a unique constraint or idempotent insert.

For example:

```
UNIQUE(eventId, recipientId, deliveryChannel)
```

Repeated processing should resolve to the existing delivery request rather than create another independent logical delivery.

# Part E — Introduce the Durable Delivery Store

## 8. Why do we need delivery records?

Let's say Bob is offline.

The chat message must remain available.

We already have the `messages` table.

Why might we additionally need a delivery store?

Because the chat message and the delivery workflow have different responsibilities.

The chat database answers:

> What messages belong to this conversation?

The delivery store answers:

> Which recipient/device needs this notification, and what is its delivery state?

For applications where the business database already provides the required recipient inbox and recovery semantics, a separate delivery database may not be necessary.

But when we need reliable notifications with retries, delivery tracking, multiple channels, or per-device acknowledgements, it can be useful.

### Example schema

```
delivery_records
```

| Column            | Purpose                             |
| ----------------- | ----------------------------------- |
| delivery_id       | Unique delivery identifier          |
| event_id          | Original event                      |
| recipient_id      | Intended user                       |
| connection_scope  | User/device/session, as required    |
| payload_reference | Reference to the message or payload |
| status            | Current delivery status             |
| attempt_count     | Number of delivery attempts         |
| next_attempt_at   | Retry scheduling time               |
| created_at        | Creation time                       |
| acknowledged_at   | Client ACK time                     |

Example record:

```
{
  "deliveryId": "del-101",
  "eventId": "evt-101",
  "recipientId": "bob",
  "payloadReference": "msg-101",
  "status": "PENDING",
  "attemptCount": 0
}
```

### Why store a payload reference instead of the entire message?

For large business objects, we don't necessarily want to copy the full payload into every delivery record.

For example, a chat message might already exist in:

```
messages
```

The delivery record can point to:

```
messageId = msg-101
```

The push layer can send either:

- The actual message payload.
- A lightweight notification containing the message ID.
- A hint that tells the client to synchronize its conversation.

This choice depends on the product and payload size.

## 9. The complete reliable architecture so far

We now have:

```
                  Chat Service
                       |
              ┌────────┴────────┐
              |                 |
              ▼                 ▼
         Message DB       Outbox Table
              |                 |
              |                 ▼
              |           Outbox Publisher
              |                 |
              |                 ▼
              |               Kafka
              |                 |
              |                 ▼
              |            Router Workers
              |                 |
              |                 ▼
              |          Delivery Store
              |                 |
              |                 ▼
              |          Delivery Workers
              |                 |
              |                 ▼
              |          Connection Registry
              |                 |
              |                 ▼
              |           Push Servers
              |                 |
              |                 ▼
              └──────────────► Client
```

The business message is durable.

The event publication is recoverable.

The delivery request is durable.

The push connection is only a temporary transport.

This is the architecture we will refine.

# Part F — Delivery State Machine

## 10. What states should a delivery record have?

Let's design the lifecycle of one delivery.

Suppose we need to deliver `del-101` to Bob.

Initially:

```
PENDING
```

A delivery worker claims it:

```
PENDING -> IN_PROGRESS
```

It finds Bob's active connection and sends the message.

Now we have multiple possible outcomes.

### Case A: Client acknowledges successfully

```
PENDING
   |
   ▼
IN_PROGRESS
   |
   ▼
SENT
   |
   ▼
ACKNOWLEDGED
```

The delivery is complete according to our defined ACK contract.

### Case B: Push server is unavailable

```
PENDING
   |
   ▼
IN_PROGRESS
   |
   ▼
FAILED_TEMPORARILY
   |
   ▼
PENDING (scheduled retry)
```

We retry later.

### Case C: User is offline

We cannot deliver through a live connection.

Depending on our application:

```
PENDING
   |
   ▼
WAITING_FOR_RECIPIENT
```

Or we simply leave the durable notification pending until the next connection or synchronization.

### Case D: Message is no longer relevant

For example, a typing indicator or an expired temporary alert.

```
PENDING
   |
   ▼
EXPIRED
```

We don't retry indefinitely.

### Case E: Permanent failure

For example:

- Recipient account deleted.
- Event invalid.
- Authorization permanently denied.
- Payload cannot be processed.

```
PENDING
   |
   ▼
FAILED_PERMANENTLY
```

We don't automatically retry a condition that will never recover.

### Suggested state machine

\#chatgpt-mermaid-\_r_96p\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_96p\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_96p\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_96p\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_96p\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_96p\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_96p\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_96p\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_96p\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_96p\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_96p\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_96p\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_96p\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_96p\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_96p\_ p{margin:0;}#chatgpt-mermaid-\_r_96p\_ defs [id$="-barbEnd"]{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_96p\_ g.stateGroup text{fill:rgb(31, 78, 148);stroke:none;font-size:10px;}#chatgpt-mermaid-\_r_96p\_ g.stateGroup text{fill:rgb(237, 237, 237);stroke:none;font-size:10px;}#chatgpt-mermaid-\_r_96p\_ g.stateGroup .state-title{font-weight:bolder;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_96p\_ g.stateGroup rect{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_96p\_ g.stateGroup line{stroke:rgb(175, 175, 175);stroke-width:1;}#chatgpt-mermaid-\_r_96p\_ .transition{stroke:rgb(175, 175, 175);stroke-width:1;fill:none;}#chatgpt-mermaid-\_r_96p\_ .stateGroup .composit{fill:transparent;border-bottom:1px;}#chatgpt-mermaid-\_r_96p\_ .stateGroup .alt-composit{fill:#e0e0e0;border-bottom:1px;}#chatgpt-mermaid-\_r_96p\_ .state-note{stroke:rgb(58, 132, 63);fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_96p\_ .state-note text{fill:rgb(237, 237, 237);stroke:none;font-size:10px;}#chatgpt-mermaid-\_r_96p\_ .stateLabel .box{stroke:none;stroke-width:0;fill:rgb(9, 23, 44);opacity:0.5;}#chatgpt-mermaid-\_r_96p\_ .edgeLabel .label rect{fill:rgb(9, 23, 44);opacity:0.5;}#chatgpt-mermaid-\_r_96p\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_96p\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_96p\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_96p\_ .edgeLabel .label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_96p\_ .label div .edgeLabel{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_96p\_ .stateLabel text{fill:rgb(237, 237, 237);font-size:10px;font-weight:bold;}#chatgpt-mermaid-\_r_96p\_ .node circle.state-start{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_96p\_ .node .fork-join{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_96p\_ .node circle.state-end{fill:rgb(31, 78, 148);stroke:transparent;stroke-width:1.5;}#chatgpt-mermaid-\_r_96p\_ .end-state-inner{fill:transparent;stroke-width:1.5;}#chatgpt-mermaid-\_r_96p\_ .node rect{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_96p\_ .node polygon{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_96p\_ [id$="-barbEnd"]{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_96p\_ .statediagram-cluster rect{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_96p\_ .cluster-label,#chatgpt-mermaid-\_r_96p\_ .nodeLabel{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_96p\_ .statediagram-cluster rect.outer{rx:5px;ry:5px;}#chatgpt-mermaid-\_r_96p\_ .statediagram-state .divider{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_96p\_ .statediagram-state .title-state{rx:5px;ry:5px;}#chatgpt-mermaid-\_r_96p\_ .statediagram-cluster.statediagram-cluster .inner{fill:transparent;}#chatgpt-mermaid-\_r_96p\_ .statediagram-cluster.statediagram-cluster-alt .inner{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_96p\_ .statediagram-cluster .inner{rx:0;ry:0;}#chatgpt-mermaid-\_r_96p\_ .statediagram-state rect.basic{rx:5px;ry:5px;}#chatgpt-mermaid-\_r_96p\_ .statediagram-state rect.divider{stroke-dasharray:10,10;fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_96p\_ .note-edge{stroke-dasharray:5;}#chatgpt-mermaid-\_r_96p\_ .statediagram-note rect{fill:rgb(48, 48, 48);stroke:rgb(58, 132, 63);stroke-width:1px;rx:0;ry:0;}#chatgpt-mermaid-\_r_96p\_ .statediagram-note rect{fill:rgb(48, 48, 48);stroke:rgb(58, 132, 63);stroke-width:1px;rx:0;ry:0;}#chatgpt-mermaid-\_r_96p\_ .statediagram-note text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_96p\_ .statediagram-note .nodeLabel{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_96p\_ .statediagram .edgeLabel{color:red;}#chatgpt-mermaid-\_r_96p\_ [id$="-dependencyStart"],#chatgpt-mermaid-\_r_96p\_ [id$="-dependencyEnd"]{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);stroke-width:1;}#chatgpt-mermaid-\_r_96p\_ .statediagramTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_96p\_ [data-look="neo"].statediagram-cluster rect{fill:rgb(9, 23, 44);stroke:url(#chatgpt-mermaid-\_r_96p\_-gradient);stroke-width:1;}#chatgpt-mermaid-\_r_96p\_ [data-look="neo"].statediagram-cluster rect.outer{rx:5px;ry:5px;filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_96p\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_96p\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_96p\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_96p\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_96p\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_96p\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_96p\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_96p\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_96p\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_96p\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_96p\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_96p\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_96p\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_96p\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_96p\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_96p\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_96p\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_96p\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}PENDINGIN_PROGRESSSENTWAITINGRETRY_SCHEDULEDFAILEDACKNOWLEDGEDEXPIREDACK timeoutRecipient reconnects

One important distinction:

`SENT` and `ACKNOWLEDGED` are not the same.

`SENT` might only mean that the push server successfully completed its transport write.

`ACKNOWLEDGED` means the client fulfilled the agreed acknowledgement contract.

We should define the exact meaning of each status in the application.

# Part G — Client Acknowledgements

## 11. Why do we need application-level ACKs?

Imagine Push Server 1 sends:

```
{
  "eventId": "evt-101",
  "deliveryId": "del-101",
  "type": "NEW_MESSAGE",
  "messageId": "msg-101",
  "payload": {
    "text": "Hello Bob!"
  }
}
```

The message reaches Bob's application.

How does the server know that?

It doesn't.

The only reliable way to learn that the application processed the message is to establish an application-level acknowledgement protocol.

Bob sends:

```
{
  "type": "ACK",
  "eventId": "evt-101",
  "deliveryId": "del-101",
  "status": "PROCESSED"
}
```

The server receives the ACK.

It updates:

```
del-101 -> ACKNOWLEDGED
```

This is useful for reliable messaging.

But we must understand a subtle problem.

## 11.1 What if the client ACK is lost?

Consider:

```
1. Server sends message 101.

2. Bob receives message 101.

3. Bob processes it successfully.

4. Bob sends ACK.

5. ACK is lost.
```

The server doesn't know that Bob processed the message.

The delivery record still looks unacknowledged.

Eventually, the server retries.

Bob receives message 101 again.

This is why at-least-once delivery can produce duplicate messages.

The client must be capable of recognizing that it has already processed an event.

## 12. Idempotency: How do we prevent duplicate processing?

Every logical event needs a stable unique identifier.

For example:

```
eventId = evt-101
```

When Bob receives it, the client checks whether it has already processed it.

Conceptually:

```
async function processEvent(event) {

    if (await isProcessed(event.eventId)) {
        sendAck(event.eventId);
        return;
    }

    await applyEvent(event);

    await markProcessed(event.eventId);

    sendAck(event.eventId);
}
```

This is illustrative. There is an important implementation requirement here:

The business effect and the processed-event marker should be persisted atomically whenever possible.

Consider the naive implementation:

```
1. Apply business effect.
2. Crash.
3. Never save processed marker.
```

After reconnecting, the client applies the event again.

Or:

```
1. Save processed marker.
2. Crash.
3. Never apply business effect.
```

Now the event is incorrectly considered processed.

We need the two operations to be atomic or naturally idempotent.

For a client backed by IndexedDB, for example, both the application-state update and the processed-event record can be written in one transaction.

For many clients, an even simpler design is to make state updates idempotent using versions and sequence numbers.

### Example

Suppose the client maintains:

```
conversationVersion = 100
```

It receives:

```
{
  "eventId": "evt-101",
  "conversationVersion": 101,
  "messageId": "msg-101"
}
```

After applying it, the client updates:

```
conversationVersion = 101
```

If the same event arrives again, the client recognizes that it has already applied version 101.

For some applications, checking the message ID against a locally persisted message table is sufficient.

### Idempotency is not just a client concern

We need it at multiple boundaries.

| Layer             | Idempotency key                      |
| ----------------- | ------------------------------------ |
| Business API      | Client-supplied idempotency key      |
| Outbox publisher  | eventId                              |
| Kafka consumer    | eventId + consumer identity          |
| Delivery creation | eventId + recipient + delivery scope |
| Push dispatch     | deliveryId                           |
| Client processing | eventId or sequence number           |
| ACK processing    | deliveryId + recipient/device        |

Each layer protects against a different kind of duplicate.

## 13. What exactly should the client ACK?

There are different acknowledgement levels.

Imagine a notification reaches the client.

We can define:

| ACK type  | Meaning                                         |
| --------- | ----------------------------------------------- |
| RECEIVED  | Client received the event                       |
| PROCESSED | Client processed the event                      |
| PERSISTED | Client successfully persisted the event locally |
| READ      | User actually viewed/read the content           |

These aren't interchangeable.

For example, in a chat application:

- `RECEIVED` means the device received the message.
- `READ` means the user viewed it.

The server might maintain read receipts separately.

For a durable synchronization system, I would generally use a client ACK after the event is safely processed or persisted, rather than merely after a network frame arrives.

For UI notifications, that might not be necessary.

### Important: ACK scope matters

Suppose Bob is logged in on three devices.

```
Bob:
    Desktop
    Mobile
    Tablet
```

Should one ACK mark the message as delivered to Bob?

Maybe.

But suppose the product needs delivery status on each device.

Then the delivery records should be per device:

```
Message 101:
    Desktop -> ACKNOWLEDGED
    Mobile  -> ACKNOWLEDGED
    Tablet  -> PENDING
```

This is why we must decide whether delivery is tracked per:

- User.
- Device.
- Session.
- Connection.

It depends on product requirements.

For chat, the message itself is usually stored once, while device-level delivery/read progress can be maintained independently.

# Part H — The Most Important Offline Delivery Pattern

## 14. Should we keep retrying while the user is offline?

Suppose Bob disconnects for two days.

Should we continuously try to send the message?

No.

That would be wasteful.

The correct approach for many applications is:

Persist the message and notify the user when they become available again.

Let's understand two different patterns.

### Pattern A: Durable notification inbox

Suppose we have an important notification:

```
Your payment was successful.
```

We save it in a notification inbox:

```
notifications

notification_id
user_id
payload
created_at
read_at
```

Bob is offline.

The notification remains there.

When Bob reconnects:

```
Bob reconnects
      |
      ▼
Push Server
      |
      ▼
Notification Service
      |
      ▼
Fetch pending notifications
      |
      ▼
Bob's client
```

Now Bob receives all pending notifications.

The real-time push is simply a fast notification mechanism.

Even if the push event is lost, the inbox remains authoritative.

### Pattern B: Durable chat history with synchronization

For chat, we often don't need a separate delivery record for every message.

The chat message already exists in the conversation history.

Suppose Bob's last successfully processed message ID is:

```
msg-100
```

Bob reconnects.

He requests:

```
GET /conversations/conv-10/messages?after=msg-100
```

The chat service returns:

```
msg-101
msg-102
msg-103
msg-104
```

Bob catches up.

This is message replay through business data synchronization.

The live push is an optimization for low latency.

### Compare these approaches

| Approach                         | Best suited for                                     |
| -------------------------------- | --------------------------------------------------- |
| Durable notification inbox       | Personal notifications, alerts, tasks               |
| Business-history synchronization | Chat, activity feeds, document history              |
| Event-log replay                 | Event-driven consumers and collaboration operations |
| Latest-state snapshot            | Live dashboards, scores, prices                     |

This is an important architectural decision.

Not every application needs a separate durable message-delivery queue for every recipient.

Sometimes its authoritative business database already provides all the recovery guarantees needed.

# Part I — Retries and Failure Recovery

## 15. What should happen when a delivery fails?

Let's say we have:

```
Delivery ID: del-101

Recipient: Bob

Status: PENDING
```

A delivery worker attempts to deliver it.

The push server is temporarily unavailable.

Should we retry immediately?

Usually, no.

Repeated immediate retries can make an outage worse.

Instead, use exponential backoff with jitter.

### 15.1 Exponential backoff

Example retry schedule:

| Attempt | Delay               |
| ------- | ------------------- |
| 1       | 1 second            |
| 2       | 2 seconds           |
| 3       | 4 seconds           |
| 4       | 8 seconds           |
| 5       | 16 seconds          |
| 6       | 32 seconds          |
| 7+      | 60 seconds (capped) |

The formula is:

\\[ delay_n = \min(base \times 2^{n-1}, maxDelay) \\]

Then add random jitter:

\\[ actualDelay = random(0, delay_n) \\]

This spreads retries across time and prevents synchronized retry storms.

The exact values depend on event urgency and service capacity.

### 15.2 Why not retry forever?

Consider:

```
Delivery 101
    |
    ▼
Attempt 1 -> Failure
    |
    ▼
Attempt 2 -> Failure
    |
    ▼
Attempt 3 -> Failure
    |
    ▼
Attempt 4 -> Failure
    |
   ...
```

Some failures are permanent.

For example:

- Invalid recipient.
- Deleted account.
- Invalid payload.
- Authorization permanently revoked.

Retrying these forever wastes resources.

We need a retry policy.

For example:

```
Maximum automatic attempts: 10

After exhaustion:
    Move to a terminal failure state
    or dead-letter handling
```

However, there is an important distinction:

Retry exhaustion for a delivery attempt does not necessarily mean the underlying business message should be deleted.

A notification might remain in the user's inbox even when its live WebSocket delivery fails.

A chat message remains in chat history.

The dead-letter queue is for failed processing work, not necessarily the business message itself.

## 16. Transient vs permanent failures

We need to classify failures correctly.

| Failure                             | Classification                      | Action                              |
| ----------------------------------- | ----------------------------------- | ----------------------------------- |
| Push server temporarily unavailable | Transient                           | Retry                               |
| Redis temporarily unavailable       | Transient                           | Retry                               |
| Kafka temporarily unavailable       | Transient                           | Retry/replay                        |
| Client connection disconnected      | Temporary or expected offline state | Wait for reconnect or sync          |
| Timeout with unknown result         | Ambiguous                           | Retry idempotently                  |
| Invalid recipient                   | Permanent                           | Stop                                |
| Account deleted                     | Usually permanent                   | Stop or apply retention policy      |
| Authorization revoked               | Security-related                    | Stop; don't retry blindly           |
| Malformed message                   | Permanent until corrected           | Dead-letter/quarantine              |
| Client too slow                     | Capacity problem                    | Backpressure/disconnect and recover |

A timeout deserves special attention.

Suppose the router sends a request to Push Server 1.

The request times out.

Did Push Server 1 process it?

We don't know.

The network may have failed:

- Before the server received the request.
- After the server received it.
- After the server processed it but before the response returned.

Therefore, the result is ambiguous.

The safe approach is to retry using the same stable `deliveryId`.

Push Server 1 or the downstream delivery system should recognize duplicates where possible.

## 17. Retry queues vs scheduled retry records

There are two common approaches.

### Option A: Message broker retry queues

With a broker such as RabbitMQ, we can use:

```
Delivery Queue
     |
     ▼
Worker
     |
     ▼
Failure
     |
     ▼
Retry Queue
     |
     | Wait
     ▼
Delivery Queue
```

We might have queues such as:

```
delivery-retry-5s
delivery-retry-30s
delivery-retry-5m
```

Dead-letter exchanges can move expired retry messages back into the main queue.

This is useful for controlled background processing.

### Option B: Retry scheduling in a database

We maintain:

```
delivery_records

deliveryId
status
attemptCount
nextAttemptAt
lastError
```

A worker periodically claims records whose:

```
status = RETRY_SCHEDULED

AND nextAttemptAt <= currentTime
```

Then it retries them.

This is particularly useful when delivery status and operational visibility are important.

At high scale, avoid having every worker continuously scan a huge table.

Use appropriate indexes, partitioning, bounded batch claiming, or a dedicated scheduler.

### Which should we use?

For a general notification delivery system, either can work.

A broker-based retry system is good when we primarily need to execute tasks.

A persistent delivery table is attractive when we also need:

- Per-recipient delivery status.
- Auditability.
- Queryable failures.
- Administrative retries.
- Long-lived pending notifications.

We can combine them:

```
Delivery DB = Source of Truth

Queue = Work Distribution
```

# Part J — Ordering Guarantees

## 18. Why is message ordering a separate problem?

Imagine Alice sends three chat messages:

```
Message 101: Hello
Message 102: How are you?
Message 103: Where are you?
```

Our system might process them through different workers.

```
Message 101 -> Worker 1
Message 102 -> Worker 2
Message 103 -> Worker 3
```

Worker 3 is faster.

Bob receives:

```
103
101
102
```

This is incorrect for a conversation where message ordering matters.

How do we solve this?

First, decide the ordering boundary.

For chat, it is usually:

```
conversationId
```

For collaborative editing:

```
documentId
```

For an order lifecycle:

```
orderId
```

We need ordering per entity, not necessarily across the entire system.

## 18.1 Use Kafka partition keys

For chat:

```
Topic = chat-events

Key = conversationId
```

All events for one conversation are routed to the same partition.

Kafka preserves their order within that partition.

```
Partition 0:

Conversation G1:
    Message 101
    Message 102
    Message 103

Conversation G2:
    Message 201
    Message 202
```

A consumer in one consumer group processes that partition sequentially.

This helps preserve order in the event-processing pipeline.

But there is a limitation:

Kafka partition ordering does not automatically guarantee WebSocket delivery ordering.

Why?

Because:

- Multiple workers might dispatch messages concurrently.
- Network calls may complete out of order.
- Retries may deliver older messages late.
- Clients may reconnect and replay messages.

We need additional protection.

## 19. Introduce sequence numbers

Assign a monotonically increasing sequence number within the ordering boundary.

For example:

```
Conversation G1:

Message 101 -> Sequence 101
Message 102 -> Sequence 102
Message 103 -> Sequence 103
```

The client tracks:

```
lastProcessedSequence = 101
```

It receives:

```
sequence = 103
```

What should happen?

It detects that 102 is missing.

Instead of silently accepting an inconsistent state, it can request replay:

```
GET /conversations/G1/messages?afterSequence=101
```

The service returns:

```
102
103
```

Now the client is consistent.

### What if the same message arrives twice?

The client has already processed sequence 103.

It recognizes the duplicate and ignores the repeated state change.

### What if sequence 102 arrives after 103?

The client can buffer 103 temporarily and process events in order, or simply request replay and resynchronization.

The right strategy depends on the application.

For chat, fetching the missing messages is often simpler than maintaining a complicated client-side reorder buffer.

For collaborative editing, the protocol may require stricter operation sequencing and synchronization.

### Important: Where do sequence numbers come from?

They need to be assigned by an authoritative component.

For a conversation, a database transaction can allocate a monotonically increasing conversation sequence.

Don't assume wall-clock timestamps will provide a reliable order.

Different servers can have different clocks, and timestamps can collide or arrive out of order.

## 20. What if we want to deliver messages to several devices?

Suppose Bob has:

```
Desktop -> Push 1
Mobile  -> Push 2
Tablet  -> Push 3
```

We should decide which ordering guarantee is required.

Option A: Each device processes its own ordered stream.

The desktop might be at sequence 105, while the tablet is at sequence 101.

That's okay as long as each device can recover missing events independently.

Option B: All devices must receive the same messages.

We still don't need all devices to acknowledge at exactly the same time.

We maintain independent delivery cursors.

For example:

```
User Bob:

Desktop:
    lastProcessed = 105

Mobile:
    lastProcessed = 102

Tablet:
    lastProcessed = 100
```

Each device catches up independently.

This is a very useful mental model for reliable multi-device applications.

# Part K — The Complete Client Recovery Protocol

## 21. Build a reconnect-and-resume mechanism

Let's design a complete protocol.

Bob previously processed:

```
conversation G1 -> sequence 105
```

Bob loses connectivity.

Messages 106–110 arrive while he's offline.

He reconnects to Push Server 3.

### Step 1: Establish a new connection

```
Bob
  |
  ▼
Load Balancer
  |
  ▼
Push Server 3
  |
  ▼
Connection Registry
```

### Step 2: Send the resume cursor

Bob sends:

```
{
  "type": "RESUME",
  "conversationId": "G1",
  "lastProcessedSequence": 105
}
```

### Step 3: Server identifies missing data

The chat service checks the authoritative message history.

```
Last processed: 105

Available:
    106
    107
    108
    109
    110
```

### Step 4: Replay

The server sends the missing messages in order.

```
106
107
108
109
110
```

### Step 5: Client applies messages

After successfully applying the messages, the client advances its cursor.

```
lastProcessedSequence = 110
```

### Step 6: Resume live delivery

Now the client continues receiving new messages over its WebSocket.

This is the core of reliable real-time synchronization.

Message DatabaseResume ServicePush ServerClientMessage DatabaseResume ServicePush ServerClient#chatgpt-mermaid-\_r_9d5\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_9d5\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_9d5\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_9d5\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_9d5\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_9d5\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_9d5\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_9d5\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_9d5\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_9d5\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_9d5\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_9d5\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_9d5\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_9d5\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_9d5\_ p{margin:0;}#chatgpt-mermaid-\_r_9d5\_ .actor{stroke:rgb(31, 78, 148);fill:rgb(9, 23, 44);stroke-width:1;}#chatgpt-mermaid-\_r_9d5\_ rect.actor.outer-path[data-look="neo"]{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_9d5\_ rect.note[data-look="neo"]{stroke:rgb(58, 132, 63);fill:rgb(48, 48, 48);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_9d5\_ text.actor>tspan{fill:rgb(237, 237, 237);stroke:none;}#chatgpt-mermaid-\_r_9d5\_ .actor-line{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_9d5\_ .innerArc{stroke-width:1.5;stroke-dasharray:none;}#chatgpt-mermaid-\_r_9d5\_ .messageLine0{stroke-width:1.5;stroke-dasharray:none;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_9d5\_ .messageLine1{stroke-width:1.5;stroke-dasharray:2,2;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_9d5\_ [id$="-arrowhead"] path{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_9d5\_ .sequenceNumber{fill:#505050;}#chatgpt-mermaid-\_r_9d5\_ [id$="-sequencenumber"]{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_9d5\_ [id$="-crosshead"] path{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_9d5\_ .messageText{fill:rgb(237, 237, 237);stroke:none;}#chatgpt-mermaid-\_r_9d5\_ .labelBox{stroke:rgba(255, 255, 255, 0.15);fill:rgb(0, 0, 0);filter:none;}#chatgpt-mermaid-\_r_9d5\_ .labelText,#chatgpt-mermaid-\_r_9d5\_ .labelText>tspan{fill:rgb(237, 237, 237);stroke:none;}#chatgpt-mermaid-\_r_9d5\_ .loopText,#chatgpt-mermaid-\_r_9d5\_ .loopText>tspan{fill:rgb(237, 237, 237);stroke:none;}#chatgpt-mermaid-\_r_9d5\_ .sectionTitle,#chatgpt-mermaid-\_r_9d5\_ .sectionTitle>tspan{fill:rgb(237, 237, 237);stroke:none;}#chatgpt-mermaid-\_r_9d5\_ .loopLine{stroke-width:2px;stroke-dasharray:2,2;stroke:rgba(255, 255, 255, 0.15);fill:rgba(255, 255, 255, 0.15);}#chatgpt-mermaid-\_r_9d5\_ .note{stroke:rgb(58, 132, 63);fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_9d5\_ .noteText,#chatgpt-mermaid-\_r_9d5\_ .noteText>tspan{fill:rgb(237, 237, 237);stroke:none;font-weight:normal;}#chatgpt-mermaid-\_r_9d5\_ .activation0{fill:rgb(48, 48, 48);stroke:hsl(0, 0%, 8.8235294118%);}#chatgpt-mermaid-\_r_9d5\_ .activation1{fill:rgb(48, 48, 48);stroke:hsl(0, 0%, 8.8235294118%);}#chatgpt-mermaid-\_r_9d5\_ .activation2{fill:rgb(48, 48, 48);stroke:hsl(0, 0%, 8.8235294118%);}#chatgpt-mermaid-\_r_9d5\_ .actorPopupMenu{position:absolute;}#chatgpt-mermaid-\_r_9d5\_ .actorPopupMenuPanel{position:absolute;fill:rgb(9, 23, 44);box-shadow:0px 8px 16px 0px rgba(0,0,0,0.2);filter:drop-shadow(3px 5px 2px rgb(0 0 0 / 0.4));}#chatgpt-mermaid-\_r_9d5\_ .actor-man circle,#chatgpt-mermaid-\_r_9d5\_ line{fill:rgb(9, 23, 44);stroke-width:2px;}#chatgpt-mermaid-\_r_9d5\_ g rect.rect{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_9d5\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_9d5\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_9d5\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_9d5\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_9d5\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_9d5\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_9d5\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_9d5\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_9d5\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_9d5\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_9d5\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_9d5\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_9d5\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_9d5\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_9d5\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_9d5\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_9d5\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_9d5\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}ReconnectConnection establishedRESUME(lastSequence=105)Resume conversation G1 from 105Fetch messages after 105Messages 106-110Replay messages106, 107, 108, 109, 110ACK(sequence=110)Resume live stream

There is one more subtle issue.

What if a new event is produced while we're replaying?

For example:

```
Replay starts at sequence 105.

Replay returns 106-110.

New message 111 arrives.
```

We must avoid losing 111 between replay and live subscription.

A safe design establishes a live buffering/subscription position before taking the replay high-water mark, then replays through that high-water mark and merges later live events using sequence numbers and deduplication.

Alternatively, use a durable stream offset from which replay and live consumption continue without a gap.

The principle is:

Reconnection must not create a gap between historical replay and live delivery.

## 22. How does SSE fit into this?

SSE already provides a mechanism for event IDs and resume requests.

Suppose the server sends:

```
id: 105
event: MESSAGE
data: {"messageId":"msg-105"}
```

The browser tracks the last event ID.

When the connection reconnects, EventSource can send:

```
Last-Event-ID: 105
```

The server uses this cursor to retrieve missed events.

But remember:

- EventSource doesn't persist your business message history.
- Event IDs do not guarantee durability.
- The server must retain or reconstruct events.
- The replay/live handoff must be gap-free.
- The application must decide what acknowledgement constitutes successful processing.

Also, native EventSource tracks the most recently dispatched event ID, not necessarily the event that your business logic has durably processed.

For stronger application-level correctness, maintain an explicit application processing cursor or ACK.

# Part L — Reliable Processing at Every Stage

## 23. We need idempotency inside Kafka consumers too

So far, we've mostly discussed client-side duplicates.

But duplicates can occur much earlier.

Suppose a routing worker consumes:

```
event-101
```

It performs a database operation to create a delivery record.

Then it crashes before committing its Kafka offset.

Kafka sends `event-101` again.

Without protection:

```
First attempt:
    Create delivery del-101

Second attempt:
    Create delivery del-102
```

Now we have two logical deliveries for the same event and recipient.

We should prevent this.

### Introduce an idempotent consumer

We can maintain:

```
processed_events
```

With a unique constraint:

```
UNIQUE(consumer_name, event_id)
```

And process the event in one database transaction:

```
@Transactional
public void processEvent(Event event) {

    if (processedEventRepository.exists(
            "notification-router",
            event.id()
    )) {
        return;
    }

    deliveryRepository.createIfAbsent(
        event.id(),
        event.recipientId()
    );

    processedEventRepository.save(
        "notification-router",
        event.id()
    );
}
```

The existence check and writes must be protected by database constraints or appropriate concurrency control. A simple check followed by insert is not sufficient when multiple workers can race.

Conceptually:

```
                 Kafka Event
                      |
                      ▼
                  Router
                      |
                      ▼
              Database Transaction
                      |
              ┌───────┴────────┐
              |                |
              ▼                ▼
        Create Delivery   Mark Event Processed
              |                |
              └───────┬────────┘
                      |
                    COMMIT
                      |
                      ▼
               Commit Kafka Offset
```

Consider the failure:

```
1. Database transaction commits.

2. Router crashes before committing Kafka offset.

3. Kafka redelivers the event.

4. Router finds the processed-event record.

5. Router skips duplicate business processing.

6. Router commits the offset.
```

This gives us effectively-once processing of that database-side operation.

### What if creating the delivery record and marking the event processed are separate database transactions?

Then we can still have an inconsistency.

Always think about which operations must be atomic.

A strong design uses:

- A unique idempotency constraint.
- One database transaction.
- Offset commit after successful database commit.

The delivery worker subsequently processes the durable delivery record.

## 24. Should we keep Kafka offsets uncommitted until the client ACKs?

This is a common design temptation.

Imagine:

```
Kafka
  |
  ▼
Router
  |
  ▼
Push Server
  |
  ▼
Client ACK
  |
  ▼
Commit Kafka Offset
```

It seems attractive because Kafka won't consider the event complete until the client acknowledges it.

But there are problems.

Suppose one event targets 10,000 users.

Do we keep the Kafka record uncommitted until all 10,000 clients ACK?

What if 9,999 clients ACK, but one user remains offline for three days?

That partition might become blocked behind one delivery.

Additionally:

- Kafka consumer progress becomes tied to client availability.
- A slow recipient can delay unrelated events in the same partition.
- Rebalances become harder to manage.
- One business event can represent many independent delivery lifecycles.

A better architecture is:

```
Kafka
  |
  ▼
Router
  |
  ▼
Persist Recipient Deliveries
  |
  ▼
Commit Kafka Offset
  |
  ▼
Independent Delivery Workers
  |
  ▼
Push Servers
  |
  ▼
Clients ACK
```

Now Kafka tracks event-to-delivery-work creation.

The delivery store tracks recipient delivery completion.

These are separate acknowledgement boundaries.

This separation is extremely important for scaling.

# Part M — The Final Reliable Architecture

## 25. Let's build the complete system

We now have all the necessary pieces.

Assume:

- We have a chat application.
- Millions of users can be connected.
- Users have multiple devices.
- Messages cannot be silently lost.
- Offline users must recover messages.
- Duplicate processing must be prevented.
- Delivery must scale horizontally.

Here is the architecture.

\#chatgpt-mermaid-\_r_9et\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_9et\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_9et\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_9et\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_9et\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_9et\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_9et\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_9et\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_9et\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_9et\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_9et\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_9et\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_9et\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_9et\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_9et\_ p{margin:0;}#chatgpt-mermaid-\_r_9et\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_9et\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_9et\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_9et\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_9et\_ .label text,#chatgpt-mermaid-\_r_9et\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_9et\_ .node rect,#chatgpt-mermaid-\_r_9et\_ .node circle,#chatgpt-mermaid-\_r_9et\_ .node ellipse,#chatgpt-mermaid-\_r_9et\_ .node polygon,#chatgpt-mermaid-\_r_9et\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_9et\_ .rough-node .label text,#chatgpt-mermaid-\_r_9et\_ .node .label text,#chatgpt-mermaid-\_r_9et\_ .image-shape .label,#chatgpt-mermaid-\_r_9et\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_9et\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_9et\_ .rough-node .label,#chatgpt-mermaid-\_r_9et\_ .node .label,#chatgpt-mermaid-\_r_9et\_ .image-shape .label,#chatgpt-mermaid-\_r_9et\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_9et\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_9et\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_9et\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_9et\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_9et\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_9et\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_9et\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_9et\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_9et\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_9et\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_9et\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_9et\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_9et\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_9et\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_9et\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_9et\_ .icon-shape,#chatgpt-mermaid-\_r_9et\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_9et\_ .icon-shape p,#chatgpt-mermaid-\_r_9et\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_9et\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_9et\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_9et\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_9et\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_9et\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_9et\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_9et\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_9et\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_9et\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_9et\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_9et\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_9et\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_9et\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_9et\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_9et\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_9et\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_9et\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_9et\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_9et\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_9et\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_9et\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_9et\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_9et\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_9et\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_9et\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_9et\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_9et\_ .node rect,#chatgpt-mermaid-\_r_9et\_ .node circle,#chatgpt-mermaid-\_r_9et\_ .node ellipse,#chatgpt-mermaid-\_r_9et\_ .node polygon,#chatgpt-mermaid-\_r_9et\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_9et\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_9et\_ .mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_9et\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_9et\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_9et\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}Client sends messageChat ServiceChat DatabaseTransactional OutboxOutbox PublisherKafka: Chat EventsRouting Consumer GroupProcessed Event /Idempotency RecordsDurable Delivery RecordsDelivery Work QueueDelivery WorkersConnection RegistryPush Server 1Push Server 2Push Server 3Connected ClientsConnected ClientsConnected ClientsClient ACK HandlerReconnect / Replay API

Let's understand each stage.

### Stage 1: Business transaction

The chat service saves:

- The actual message.
- The corresponding outbox event.

Both within one database transaction.

### Stage 2: Publish event

The outbox publisher sends the event to Kafka.

It marks the outbox event published after Kafka confirms acceptance.

If the publisher crashes after publishing but before updating the outbox, it may publish again.

Consumers tolerate duplicates.

### Stage 3: Durable routing

The router consumes the event.

It resolves recipients.

It creates one logical delivery record per intended recipient/device, depending on the delivery policy.

It uses idempotency constraints to prevent duplicate records.

It commits its Kafka progress after the durable handoff.

### Stage 4: Delivery processing

Delivery workers claim pending delivery records.

They locate active connections using the connection registry.

They dispatch to the destination push servers.

### Stage 5: Transport delivery

Push servers write messages to locally owned connections.

The client processes the event.

### Stage 6: Client acknowledgement

The client sends an application-level ACK.

The delivery record is updated.

If an ACK never arrives, the system applies the appropriate retry or recovery policy.

### Stage 7: Reconnection and replay

If Bob loses connectivity, his chat messages remain in the chat database.

When he reconnects, he supplies his last processed cursor.

The chat service returns missing messages.

The client catches up and resumes live delivery.

This is the complete model.

# Part N — Do We Always Need All These Components?

## 26. No. Choose the reliability architecture based on the requirement.

The architecture above is deliberately comprehensive.

It is not the minimum architecture for every real-time application.

Let's derive four patterns.

### Pattern 1: Best-effort real-time broadcast

Examples:

- Typing indicators.
- Temporary presence updates.
- Live dashboard values.
- Frequently changing stock prices where latest state can be fetched.

Architecture:

```
Business Service
      |
      ▼
Pub/Sub
      |
      ▼
Push Servers
      |
      ▼
Clients
```

Properties:

- Low latency.
- No durable per-recipient delivery record.
- Missed events are acceptable or recoverable through snapshots.
- Minimal retry machinery.

Use when the individual event isn't important after the moment has passed.

### Pattern 2: Durable business state + best-effort push

Examples:

- Chat messages.
- Order status.
- Account settings.
- Document changes where the authoritative history is available.

Architecture:

```
Business Service
      |
      ▼
Business DB + Outbox
      |
      ▼
Kafka
      |
      ▼
Routing Worker
      |
      ▼
Push Server
      |
      ▼
Client

If disconnected:

Client fetches missing state from Business DB.
```

Properties:

- Business changes are durable.
- Push is primarily a low-latency hint.
- The client recovers missed events through synchronization.
- We don't need to keep retrying every socket delivery indefinitely.

This is a very attractive pattern.

### Pattern 3: Durable per-recipient notification delivery

Examples:

- Important personal notifications.
- Alerts requiring delivery status.
- Tasks requiring an acknowledgement.
- Enterprise notifications.

Architecture:

```
Business Service
      |
      ▼
Transactional Outbox
      |
      ▼
Kafka
      |
      ▼
Notification Router
      |
      ▼
Durable Inbox / Delivery DB
      |
      ▼
Delivery Workers
      |
      ▼
Push Servers
      |
      ▼
Client ACK
```

Properties:

- Durable recipient records.
- Retry scheduling.
- Delivery status.
- Offline recovery.
- Duplicate detection.

### Pattern 4: Reliable ordered collaboration stream

Examples:

- Collaborative editors.
- Shared canvases.
- Distributed whiteboards.
- Multi-user simulations.

Architecture:

```
Collaboration Service
      |
      ▼
Persist Operation + Outbox
      |
      ▼
Kafka (Key = documentId)
      |
      ▼
Ordered Processing
      |
      ▼
Group Interest Router
      |
      ▼
Push Servers
      |
      ▼
Clients
```

Properties:

- Per-document ordering boundary.
- Sequence numbers.
- Replay.
- Client cursors.
- Snapshot/resynchronization.
- Idempotent operation handling.

The four patterns solve different problems.

We should not force them into one architecture.

# Part O — Failure Matrix: Can We Actually Recover?

## 27. Let's verify every stage

A reliable architecture should have an explicit answer for every failure boundary.

| Failure                                              | What could go wrong?                   | Recovery mechanism                           |
| ---------------------------------------------------- | -------------------------------------- | -------------------------------------------- |
| Business service crashes before DB commit            | Message not created                    | Client retries idempotently                  |
| DB commits but event isn't published                 | Downstream never learns about event    | Transactional Outbox                         |
| Outbox publisher crashes                             | Event may be published twice           | Stable event ID + idempotent consumer        |
| Kafka unavailable                                    | Events cannot be published/consumed    | Outbox backlog / Kafka retry                 |
| Router crashes before DB commit                      | Event must be processed again          | Kafka redelivery                             |
| Router crashes after DB commit, before offset commit | Duplicate event processing             | Unique constraint and idempotent consumer    |
| Delivery worker crashes                              | Delivery remains incomplete            | Durable delivery record and work reclamation |
| Push server unavailable                              | Live delivery fails                    | Retry or offline recovery                    |
| Push server writes but crashes before response       | Delivery outcome is ambiguous          | Idempotent retry                             |
| Client disconnects before processing                 | Message might be missed                | Replay/inbox synchronization                 |
| Client processes message but ACK is lost             | Duplicate delivery                     | Client-side deduplication                    |
| ACK is received twice                                | Duplicate status update                | Idempotent ACK processing                    |
| User reconnects to another server                    | Previous connection ownership is stale | New connection registration                  |
| User stays offline for days                          | Live push impossible                   | Durable inbox or business history            |

The architecture is reliable because each failure has a recovery mechanism.

Not because every component is highly available.

That distinction matters.

A system can have highly available Kafka and Redis but still lose notifications because its application logic commits offsets too early.

Reliability comes from the complete processing protocol.

# Part P — A Few Important Details That Are Easy to Miss

## 28. ACK processing must itself be idempotent

Suppose the client sends:

```
{
  "deliveryId": "del-101",
  "status": "ACKNOWLEDGED"
}
```

The server receives the ACK and updates the delivery record.

Then the client reconnects and sends the same ACK again.

The server should safely accept the duplicate.

Conceptually:

```
UPDATE delivery_records
SET status = 'ACKNOWLEDGED',
    acknowledged_at = CURRENT_TIMESTAMP
WHERE delivery_id = 'del-101'
  AND status <> 'ACKNOWLEDGED';
```

The operation should only transition the delivery into the acknowledged state.

It should not produce another notification, another payment, or another business effect.

Also verify that the connection/user/device sending the ACK is authorized to acknowledge that particular delivery.

Never trust arbitrary client-supplied delivery IDs.

## 29. Do not confuse message acknowledgement with read receipts

In a chat application:

```
Message 101
```

There are several different statuses:

```
CREATED
    |
    ▼
PERSISTED
    |
    ▼
DELIVERY ATTEMPTED
    |
    ▼
RECEIVED BY DEVICE
    |
    ▼
READ BY USER
```

They answer different questions.

| Status             | Meaning                                  |
| ------------------ | ---------------------------------------- |
| Persisted          | Message is safely stored in chat history |
| Delivery attempted | Server attempted to send it              |
| Received           | A device acknowledged processing/receipt |
| Read               | User viewed it                           |

A user might receive a message on their mobile but not open it.

We shouldn't mark it as read merely because the mobile application acknowledged receipt.

Similarly, don't let one device's delivery ACK automatically imply that every other device has received the message unless that is the intended product semantics.

## 30. How long should we retain delivery records?

Not all delivery records need to live forever.

For example:

| Record                       | Possible retention strategy                                    |
| ---------------------------- | -------------------------------------------------------------- |
| Typing indicator             | No persistence                                                 |
| Live score update            | Short retention or latest snapshot                             |
| Push delivery attempt        | Keep according to retry/audit requirements                     |
| Acknowledged notification    | Retain according to product requirements                       |
| Chat message                 | Retain according to chat-history policy                        |
| Audit/financial notification | Retain according to applicable business and legal requirements |

The delivery record can eventually expire even though the underlying business message remains in the database.

This is another reason to keep business state and delivery state separate.

## 31. What if the client never ACKs?

Suppose we have a notification that requires acknowledgement.

The user receives it but never sends an ACK.

We need an explicit terminal policy.

Possible options:

- Retry for a bounded period.
- Keep it pending until a later synchronization.
- Show it as delivered but unacknowledged.
- Escalate through another channel.
- Mark it expired after its validity window.

Never let unacknowledged delivery records retry forever without a policy.

For a chat message, we generally don't need to endlessly retransmit the same WebSocket message. The durable message remains in chat history, and reconnect synchronization takes care of recovery.

For a critical operational alert, continuing retries or escalating might be appropriate.

The business requirement determines the policy.

# Part Q — The Decision Framework

## 32. How do you derive the reliability architecture from requirements?

This is the most useful part to take into system design interviews and production architecture discussions.

Don't begin by saying:

"We need Kafka with exactly-once delivery."

Start by asking these questions.

### Question 1: Can we lose individual events?

Examples:

- Typing indicator: Yes.
- Presence update: Usually yes, if current presence can be reconstructed.
- Chat message: No.
- Payment completion: No.

This determines whether ephemeral Pub/Sub is sufficient.

### Question 2: Is the actual business data already durable?

Examples:

- Chat messages exist in a message database.
- Order status exists in an order database.
- Notification exists in a notification inbox.

If yes, a push event can sometimes be a lightweight synchronization hint.

If no, we need a durable storage mechanism for the message itself.

### Question 3: What must be acknowledged?

Do we need confirmation that:

- The broker accepted the event?
- The router processed it?
- The push server accepted it?
- The client received it?
- The client processed and persisted it?
- The user actually read it?

These are different acknowledgement boundaries.

### Question 4: Can we tolerate duplicates?

If yes, at-least-once plus idempotency is a good approach.

If no duplicate business effects are allowed, we need stable IDs, deduplication, atomic processing, and appropriate transactional boundaries.

### Question 5: Do we need ordering?

If yes:

- Identify the ordering boundary.
- Partition by that entity.
- Assign sequence numbers.
- Make downstream processing preserve or reconstruct the order.
- Let clients detect gaps.

### Question 6: What should happen while the user is offline?

Choose one:

- Discard the event.
- Deliver the latest snapshot.
- Maintain a durable inbox.
- Replay business history.
- Replay an ordered event stream.

### Question 7: How long should retries continue?

Define:

- Retryable errors.
- Permanent failures.
- Backoff.
- Maximum attempts or expiry.
- Dead-letter/reconciliation policy.
- Manual intervention requirements, if any.

These seven questions will get you to a much more appropriate design than simply choosing Kafka and enabling retries.

# Chapter 6 — Final Mental Model

There are six ideas I want you to retain.

1\. A persistent connection is not a durable message.

WebSocket/SSE provides a communication channel. It doesn't guarantee message storage, delivery, or processing.

2\. Business data must be persisted independently.

A chat message belongs in the chat database. An important notification belongs in a durable inbox or equivalent storage.

3\. Transactional Outbox prevents the database-to-broker dual-write problem.

Save business data and the event in one database transaction. Publish the outbox event asynchronously.

4\. At-least-once + idempotency is the practical reliability foundation.

Duplicates are expected. Stable identifiers and idempotent processing prevent duplicate business effects.

5\. Client ACK and Kafka ACK are different.

Kafka knows whether its consumer has processed an event. It doesn't know whether a browser processed a WebSocket message.

6\. Reconnection requires synchronization.

The client needs a cursor, sequence number, replay mechanism, or snapshot to recover missing data.

The most important formula:

\\[ \boxed{ \text{Reliable Processing} = \text{Durability} + \text{Retries} + \text{Idempotency} + \text{Recovery} } \\]

And remember that exactly-once network delivery is not the goal. Reliable, effectively-once application processing is the goal.

## The four architectures to remember

| Application                       | Recommended reliability pattern                            |
| --------------------------------- | ---------------------------------------------------------- |
| Live dashboards and sports scores | Best-effort push + latest-state snapshot                   |
| Chat and messaging                | Durable messages + outbox + live push + replay             |
| Critical notifications            | Durable inbox + delivery records + retries + ACK           |
| Collaborative editing             | Ordered operations + sequence numbers + replay + snapshots |

These are different patterns because their requirements are different.

# Next: Chapter 7 — Failure Handling and Fault Tolerance

Now that we've designed the reliability protocol, we'll stress-test the entire infrastructure.

We'll take each component down and understand how the remaining system should behave.

1. What happens when a push server crashes with 100,000 active connections?
2. What happens when Kafka brokers or a partition leader go down?
3. What happens when Redis becomes unavailable?
4. What happens when the routing workers crash or get stuck?
5. What happens when a push server is alive but cannot reach the message broker?
6. How do we handle network partitions and stale connection ownership?
7. How do we prevent cascading failures and retry storms?
8. How do we implement circuit breakers, bulkheads, timeouts, and load shedding?
9. How should dead-letter queues and poison messages be handled?
10. How do we perform graceful deployments without disconnecting everyone unnecessarily?
11. How do we monitor delivery latency, failure rates, queue lag, and connection health?

We'll build a systematic failure-handling strategy rather than treating every failure as just another retry.

The central question: When any component becomes unhealthy, how do we prevent that failure from spreading to the entire real-time delivery system?
