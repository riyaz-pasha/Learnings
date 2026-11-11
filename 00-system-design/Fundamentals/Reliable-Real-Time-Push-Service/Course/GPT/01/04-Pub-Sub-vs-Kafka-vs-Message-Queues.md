# Chapter 4 — Pub/Sub vs Kafka vs Message Queues

This chapter answers one of the biggest questions in designing a real-time push system:

When an event is generated, how should it travel through the backend and reach the correct push servers?

We've already established the four routing patterns:

1. Global broadcast.
2. Group-based delivery.
3. Individual-user delivery.
4. Individual-connection delivery.

Now we need to choose the messaging infrastructure for each.

Before choosing Redis, Kafka, RabbitMQ, or another technology, let's understand the underlying communication models.

## 1. First, understand the three fundamental messaging models

Imagine Service A generates an event:

```
{
  "eventId": "event-101",
  "type": "ORDER_SHIPPED",
  "userId": "user-A"
}
```

It wants to send this event to another part of the system.

There are three fundamentally different things it might want to achieve.

### Model A: One-to-one work distribution

Suppose we have three workers:

```
                    Producer
                       |
                       | Task
                       ▼
                     Queue
                       |
                 ┌─────┼─────┐
                 |     |     |
                 ▼     ▼     ▼
                W1    W2    W3
```

The producer puts a task into a queue.

One of the available workers receives it.

Suppose W2 receives the task.

```
Task 101 -> W2
```

W1 and W3 do not receive that task.

Why?

Because these workers are competing to process the same work.

This is the competing consumers / work queue model.

Examples:

- Process a notification.
- Generate a report.
- Resize an image.
- Send an email.
- Process one payment task.
- Execute one delivery job.

The fundamental requirement is:

> This work needs to be processed by one worker, not every worker.

### Model B: One-to-many broadcast

Now suppose we want to send the same event to all interested subscribers.

```
                     Producer
                        |
                        | Publish
                        ▼
                     Channel
                        |
              ┌─────────┼─────────┐
              |         |         |
              ▼         ▼         ▼
           Service A Service B Service C
```

All three independent subscribers receive the event.

This is the Publish/Subscribe (Pub/Sub) model.

Examples:

- Broadcast a global announcement.
- Notify all interested push servers about a live score.
- Notify multiple services that an order has shipped.
- Distribute cache invalidation events.
- Notify all active subscribers of a particular collaboration room.

The fundamental requirement is:

> Every interested subscriber should receive its own copy of the event.

### Model C: Retained event stream

Now let's introduce another requirement.

Suppose a consumer is unavailable for 30 minutes.

When it returns, we want it to continue processing events that were published during that period.

Additionally, we might want to replay old events.

```
Producer
   |
   ▼
Event Log
   |
   ├── Event 101
   ├── Event 102
   ├── Event 103
   ├── Event 104
   ├── Event 105
   |
   ▼
Consumer
```

Instead of treating each message as a temporary delivery to one subscriber, we retain an ordered history of events.

Each consumer maintains its own progress.

For example:

```
Consumer A processed through Event 103.

Consumer B processed through Event 101.
```

Consumer A and Consumer B can consume independently.

This is the durable event-stream model.

Examples:

- Order event processing.
- Financial event processing.
- Chat history pipelines.
- Activity streams.
- Audit events.
- Reliable notification pipelines.

Kafka is a particularly important implementation of this model.

## 2. The difference between Pub/Sub, queues, and event streams

Let's consolidate the fundamental differences.

| Property                               | Pub/Sub                   | Work queue                              | Event stream                             |
| -------------------------------------- | ------------------------- | --------------------------------------- | ---------------------------------------- |
| Main purpose                           | Broadcast                 | Distribute work                         | Retain and process events                |
| Multiple consumers receive same event? | Yes, per subscription     | Not within one competing-consumer group | Yes, through independent consumer groups |
| Durable storage                        | Depends on implementation | Depends on implementation               | Core characteristic                      |
| Offline consumer recovery              | Depends on implementation | Depends on implementation               | Supported through retained offsets       |
| Message replay                         | Depends on implementation | Usually not the primary model           | Yes                                      |
| Consumer progress                      | Subscription-dependent    | Acknowledgement-dependent               | Offset per consumer group                |
| Typical example                        | Redis Pub/Sub             | RabbitMQ queue                          | Kafka                                    |

One subtle point:

These are messaging patterns, not mutually exclusive product categories.

A broker can implement more than one pattern.

For example:

- RabbitMQ supports queues and different exchange types, including fanout and topic routing.
- Redis has ephemeral Pub/Sub and the more durable Redis Streams.
- NATS supports Core Pub/Sub and durable messaging through JetStream.
- Kafka provides retained event streams and can also be used for competing-consumer work distribution.

So we shouldn't think:

```
Pub/Sub = Redis
Queue = RabbitMQ
Event Stream = Kafka
```

That is an oversimplification.

Instead, first determine the required semantics, then choose an implementation.

# Part A — Understand Pub/Sub Properly

## 3. What exactly is a Pub/Sub channel?

Suppose we have a sports service and three push servers.

The sports service publishes:

```
{
  "eventId": "101",
  "type": "SCORE_UPDATE",
  "score": "180/4"
}
```

To a logical channel:

```
sports-live
```

Three push servers subscribe to this channel.

```
             Sports Service
                    |
                    | PUBLISH
                    ▼
             sports-live
              (Channel)
                    |
          ┌─────────┼─────────┐
          |         |         |
       SUB 1     SUB 2     SUB 3
          |         |         |
          ▼         ▼         ▼
        Push 1    Push 2    Push 3
```

Each subscriber receives a copy.

Notice that the push servers are not competing for the event.

They all need it.

This is the defining characteristic of Pub/Sub.

### What does subscription mean?

A subscriber says:

```
I am interested in events published to sports-live.
```

The publisher doesn't need to know who the subscribers are.

It simply publishes the event.

This provides loose coupling.

The sports service doesn't need to know whether there are 10 push servers or 10,000 push servers.

It publishes to the channel, and the messaging system distributes the event to the subscribers.

## 3.1 What happens when a subscriber is disconnected?

Consider Redis Pub/Sub.

```
sports-live
     |
     ├── Push 1 (Connected)
     ├── Push 2 (Disconnected)
     └── Push 3 (Connected)
```

The event is published.

Push 1 and Push 3 receive it.

Push 2 misses it.

When Push 2 reconnects, it doesn't automatically receive missed events.

This is because Redis Pub/Sub is an ephemeral messaging mechanism.

There is no durable message history for disconnected subscribers to replay.

This is an extremely important property.

It makes Redis Pub/Sub attractive for low-latency fanout, but unsuitable as the sole reliable storage mechanism for messages that cannot be lost.

## 3.2 When should we use ephemeral Pub/Sub?

Good examples:

Live stock prices

We can deliver the latest price to connected users. If some intermediate updates are missed, the client can retrieve the current snapshot.

Typing indicators

If the event is missed, there's little value in replaying it later.

Presence changes

Some applications can reconstruct or refresh presence after reconnecting.

Global live dashboards

We can push changes as they happen, and the client can fetch a snapshot if it reconnects.

Internal push-server notifications

We can use a low-latency channel to tell a particular push server that it has new work, provided durable recovery exists elsewhere when required.

The key requirement is:

> Losing an individual live event is acceptable, or the latest state can be reconstructed from an authoritative source.

# Part B — Understand Message Queues Properly

## 4. What exactly is a work queue?

Now suppose we have an application generating notifications.

We have 10 delivery workers:

```
Notification Service
         |
         ▼
      Queue
         |
   ┌─────┼─────┐
   |     |     |
   ▼     ▼     ▼
  W1    W2    W3 ...
```

The service publishes a task:

```
{
  "taskId": "task-101",
  "type": "DELIVER_NOTIFICATION",
  "userId": "user-A",
  "messageId": "message-501"
}
```

One worker receives it.

Suppose W2 gets the task.

W2 processes the task and acknowledges successful processing.

The queue can then consider that task complete.

Other workers don't need to process the same task.

### Why do we need this model?

Suppose we have 1 million notification delivery jobs.

We don't want every worker processing every job.

We want to distribute the work.

For example:

```
1 million tasks
      |
      ▼
   Queue
      |
      ├── Worker 1 -> 100,000 tasks
      ├── Worker 2 -> 100,000 tasks
      ├── Worker 3 -> 100,000 tasks
      └── ...
```

In practice, work distribution depends on worker capacity, prefetch, scheduling, and broker implementation. The point is that workers compete to process tasks.

## 4.1 What happens if a worker crashes?

Let's say W2 receives Task 101.

```
Queue -> W2 -> Task 101
```

W2 crashes before acknowledging the task.

With a suitable acknowledgement and redelivery configuration, the broker can make that task available to another worker.

```
Task 101
    |
    ▼
W2 receives it
    |
    ▼
W2 crashes
    |
    ▼
Task becomes eligible for redelivery
    |
    ▼
W3 receives it
```

This is useful for reliable background processing.

But remember:

Redelivery can produce duplicate processing.

For example, W2 might have completed its business operation but crashed before acknowledging it.

Then W3 receives the same task.

Therefore, workers need idempotency.

We'll cover this in the reliability chapter.

## 4.2 Can a queue broadcast to multiple subscribers?

A simple competing-consumer queue does not.

But some messaging systems support a fanout exchange that copies a published message into multiple independent queues.

For example:

```
                  Producer
                     |
                     ▼
                Fanout Exchange
                 /     |     \
                /      |      \
               ▼       ▼       ▼
            Queue 1  Queue 2  Queue 3
               |       |       |
               ▼       ▼       ▼
            Service A Service B Service C
```

Each independent queue gets a copy.

This is Pub/Sub behavior implemented through an exchange and multiple queues.

It is useful to understand because some systems combine work queues and broadcast routing through the same broker.

# Part C — Understand Kafka Properly

## 5. Why does Kafka exist?

Now imagine we have a large system.

Many services generate events:

```
Order Service
Payment Service
Chat Service
User Service
Document Service
Notification Service
```

We want these events to be:

- Durable.
- Retained for a configurable period.
- Processable asynchronously.
- Replayable.
- Scalable across many consumers.
- Ordered where required.

A normal ephemeral Pub/Sub channel is insufficient.

A simple work queue is also not the best abstraction when several independent services need to maintain their own processing progress over the same event history.

This leads us to Kafka.

Kafka is a distributed, partitioned event log.

Think of it as a collection of durable, ordered sequences of events.

### A simple Kafka architecture

```
                  Producers
                      |
                      ▼
                Kafka Cluster
                      |
                 Topic: orders
                      |
             ┌────────┼────────┐
             |        |        |
             ▼        ▼        ▼
         Partition 0 Partition 1 Partition 2
```

A producer writes events to a topic.

Kafka stores records in partitions.

Consumers read those records.

The most important concepts to understand are:

1. Topic.
2. Partition.
3. Offset.
4. Consumer group.

Let's derive them.

## 6. Kafka Topic — What is it?

A topic is a named logical stream of events.

For example:

```
order-events
payment-events
chat-events
document-events
notification-events
```

When an order is created, the order service publishes an event to:

```
order-events
```

When a payment succeeds, the payment service publishes an event to:

```
payment-events
```

A topic represents a category or stream of events.

It does not necessarily represent a particular user or group.

For example, this is generally a reasonable topic:

```
chat-events
```

This is generally not a good default:

```
chat-group-123456
chat-group-123457
chat-group-123458
...
```

Why?

Because we might have millions of groups.

We'll understand this design decision in detail later.

## 7. Kafka Partition — How does Kafka scale?

Suppose we create a topic:

```
chat-events
```

If the entire topic were handled as one sequential stream, it could become a bottleneck.

So Kafka divides a topic into partitions.

```
Topic: chat-events

    Partition 0
    Partition 1
    Partition 2
    Partition 3
```

Each partition is an ordered sequence of records.

Conceptually:

```
Partition 0:
    Event 1
    Event 2
    Event 3
    Event 4

Partition 1:
    Event 5
    Event 6
    Event 7

Partition 2:
    Event 8
    Event 9
    Event 10
```

Kafka can distribute partitions across brokers.

This enables:

- Parallel reading.
- Parallel writing.
- Horizontal scaling.
- Replication across brokers.

### What determines the partition?

A producer can choose a partition directly or provide a key.

For example:

```
topic = chat-events
key = groupId
```

Kafka's partitioning logic maps the key to a partition.

Conceptually:

```
partition = hash(groupId) % partitionCount
```

This means events with the same group ID are normally routed to the same partition, assuming the partitioning configuration remains compatible.

For example:

```
G1 -> Partition 0
G2 -> Partition 2
G3 -> Partition 1
G4 -> Partition 0
```

Now all events for G1 go through the same partition.

That gives us an important benefit:

Events with the same key can be processed in partition order by a consumer group.

For a chat system, this could preserve the ordering of messages within a conversation.

For collaborative editing, this can be useful for ordering document operations.

There is one qualification: changing a topic's partition count can change key-to-partition mapping, so don't casually increase partitions when strict per-key ordering across the change is important.

### Does Kafka provide global ordering?

No.

Kafka provides ordering within an individual partition.

If events are stored in different partitions, there is no automatic global ordering between them.

For example:

```
Partition 0: A1 -> A2 -> A3

Partition 1: B1 -> B2 -> B3
```

A consumer might process A2 before B1, or B1 before A2.

There is no guaranteed ordering between the two partitions.

This is why choosing the right partition key matters.

## 8. Kafka Offset — How does a consumer remember progress?

Suppose a consumer reads:

```
Partition 0:

Offset 0 -> Event A
Offset 1 -> Event B
Offset 2 -> Event C
Offset 3 -> Event D
Offset 4 -> Event E
```

The offset identifies a record's position in a partition.

Suppose Consumer A has processed through offset 2.

It can commit its progress:

```
Consumer A:
    Partition 0 -> Offset 3
```

The committed offset is the position from which it should resume.

So if Consumer A crashes and restarts, it can continue from its committed progress.

Meanwhile, another consumer might have its own independent progress.

```
Consumer A:
    Partition 0 -> Offset 3

Consumer B:
    Partition 0 -> Offset 1
```

Both are reading the same underlying event history.

This is one of Kafka's most powerful features.

The event is retained independently of whether one consumer has processed it.

Within the configured retention period, another consumer can replay the event.

Kafka retention is based on time, size, or configuration, not on whether every consumer acknowledged every record.

# Part D — Consumer Groups: The Most Important Kafka Concept

## 9. Why do consumer groups exist?

Imagine we have one Kafka topic with 3 partitions:

```
Topic: order-events

Partition 0
Partition 1
Partition 2
```

We have three workers.

We want to distribute processing across them.

We put them into the same consumer group:

```
Consumer Group: order-processors

    Worker 1
    Worker 2
    Worker 3
```

Kafka assigns partitions to consumers.

```
Partition 0 -> Worker 1

Partition 1 -> Worker 2

Partition 2 -> Worker 3
```

Each record is processed by one consumer in this group.

This is competing-consumer behavior.

Now let's address a very common confusion.

## 9.1 Do all consumers in the same consumer group receive the same message?

No.

They share the work.

Suppose an event is written to Partition 0.

Only the consumer assigned Partition 0 in that group processes it.

The other consumers in that group don't each receive another copy.

Let's make it visual.

```
                 Kafka Topic
                      |
             ┌────────┼────────┐
             |        |        |
             ▼        ▼        ▼
           P0       P1       P2
             |        |        |
             ▼        ▼        ▼
           W1       W2       W3

             Same Consumer Group
```

This is excellent when we want to scale one processing pipeline.

But now consider a different requirement.

## 10. What if multiple independent services need every event?

Suppose an order event must be processed by:

- Inventory Service.
- Analytics Service.
- Notification Service.

Each service needs its own copy.

We create separate consumer groups.

```
                      Kafka
                        |
                  order-events
                        |
             ┌──────────┼──────────┐
             |          |          |
             ▼          ▼          ▼
          Group A    Group B    Group C
             |          |          |
             ▼          ▼          ▼
         Inventory  Analytics  Notification
```

Each group maintains its own offsets.

The same record can be read independently by all three groups.

Within each group, consumers share partition work.

This gives us a useful distinction:

| Kafka concept              | Meaning                                |
| -------------------------- | -------------------------------------- |
| Topic                      | Logical event stream                   |
| Partition                  | Ordered shard of a topic               |
| Offset                     | Position within a partition            |
| Consumer group             | Independent processing subscription    |
| Consumers within one group | Compete to process assigned partitions |
| Different consumer groups  | Independently consume the same records |

Let's apply this directly to push servers.

# Part E — The Kafka Trap in Push Architectures

## 11. Why subscribing all push servers to one consumer group is wrong for broadcast

We have three push servers:

```
Push 1
Push 2
Push 3
```

We create a Kafka topic:

```
global-events
```

All push servers subscribe using the same consumer group:

```
Consumer Group: push-servers

    Push 1
    Push 2
    Push 3
```

An event arrives:

```
{
  "type": "GLOBAL_ANNOUNCEMENT",
  "message": "Maintenance at 10 PM"
}
```

Kafka delivers it to one of the assigned consumers.

Suppose Push Server 2 receives it.

```
global-events
     |
     ▼
Consumer Group: push-servers
     |
     ├── Push 1
     ├── Push 2  <── Event delivered here
     └── Push 3
```

Push Server 2 broadcasts it to its local users.

But Users on Push 1 and Push 3 never receive it.

This is incorrect if the intention was to deliver the announcement to everyone.

## 11.1 How do we make all push servers receive every event?

We can give every push server its own consumer group.

```
                   Kafka
                      |
                global-events
                      |
          ┌───────────┼───────────┐
          |           |           |
          ▼           ▼           ▼
       Group 1      Group 2      Group 3
          |           |           |
          ▼           ▼           ▼
        Push 1      Push 2      Push 3
```

Now each push server independently receives the event.

This gives us broadcast-like behavior.

But let's look at the problem.

Imagine we have 2,000 push servers.

We now have 2,000 independent consumer groups for this one broadcast stream.

Each group maintains independent offsets and requires consumer-group coordination and partition assignments.

We have also made every push server consume every event, even when the event is relevant to only one user.

This might work for some bounded, low-volume workloads.

But it is often an inefficient architecture for a large push infrastructure.

We need a smarter routing layer.

# Part F — The Correct Architecture: Separate Durable Event Processing From Push Fanout

## 12. Introduce a fanout/router layer

Let's revisit the original requirement.

A business service produces an event.

```
{
  "eventId": "event-501",
  "type": "ORDER_SHIPPED",
  "userId": "user-A",
  "payload": {
    "orderId": "order-123"
  }
}
```

We want:

- Reliable processing.
- A durable event history.
- Scaling across workers.
- Targeted delivery.
- No unnecessary broadcast to every push server.

We can build this architecture:

```
                Business Service
                       |
                       ▼
                 Durable Event Log
                      Kafka
                       |
                       ▼
              Fanout / Routing Workers
                       |
                       ▼
              Connection Registry
                       |
                       ▼
                Target Push Server
                       |
                       ▼
                  User Connection
```

Now the responsibilities are separated.

### Kafka handles:

- Retaining events.
- Distributing event processing work.
- Tracking consumer progress.
- Replaying events.
- Absorbing bursts in production.

### Fanout/router workers handle:

- Interpreting event audience information.
- Looking up group members if needed.
- Looking up user connections.
- Finding destination push servers.
- Deduplicating destination servers.
- Dispatching delivery requests.

### Push servers handle:

- Receiving targeted delivery requests.
- Finding local connections.
- Sending data through sockets.

This is the architecture I would use as a general starting point for a reliable, large-scale push system.

Let's examine the different routing scenarios through it.

## 13. Individual-user messages through Kafka

Suppose we have:

```
Service A wants to notify User A.
```

It publishes:

```
{
  "eventId": "event-601",
  "type": "USER_NOTIFICATION",
  "userId": "user-A",
  "payload": {
    "message": "Your order has shipped"
  }
}
```

To Kafka:

```
Topic: user-events
Key: user-A
```

We have multiple router workers in one consumer group.

```
                Kafka: user-events
                        |
                  Consumer Group
                    push-router
                        |
               ┌────────┼────────┐
               |        |        |
               ▼        ▼        ▼
             W1       W2       W3
```

Only one worker in that group processes this particular record.

Suppose W2 receives it.

W2 performs:

```
1. Read userId = user-A.

2. Lookup user-A in connection registry.

3. Find:
       conn-101 -> Push 1
       conn-102 -> Push 3

4. Deduplicate destinations.

5. Dispatch to Push 1 and Push 3.
```

The result:

```
                 Router W2
                    |
             ┌──────┴──────┐
             |             |
             ▼             ▼
          Push 1        Push 3
             |             |
             ▼             ▼
          Conn 101      Conn 102
```

We needed one Kafka record, one router processing operation, and two destination deliveries.

We did not need every push server to consume the same event.

This is the key optimization.

## 14. Group messages through Kafka

Suppose a message is published to Group G1.

We publish:

```
Topic: group-events

Key: group-G1
```

Why use the group ID as the key?

Because we may want events for the same group to remain ordered within one partition.

A router worker consumes the record.

```
Kafka
  |
  ▼
group-events
  |
  ▼
Partition selected by G1
  |
  ▼
Router Worker
  |
  ▼
Resolve G1 audience
  |
  ▼
Find relevant push servers
  |
  ▼
Dispatch
```

The worker might use:

- The authoritative group membership database for relatively small groups.
- A group-to-push-server interest registry for large active groups.

The important point is that Kafka is responsible for delivering the group event to the routing worker.

The routing worker is responsible for distributing the event to the actual push servers.

The Kafka topic is not automatically a subscription channel for each individual group.

## 15. Global broadcasts through Kafka

Now suppose we really do need to broadcast an event to all push servers.

There are two choices.

### Choice A: Every push server has an independent consumer group

We discussed this.

It is straightforward, but can be costly for a large number of servers.

### Choice B: One consumer group of broadcast workers

Instead, create:

```
Kafka
  |
  ▼
global-events
  |
  ▼
Broadcast Router Consumer Group
  |
  ├── Worker 1
  ├── Worker 2
  └── Worker 3
```

A worker receives the global event.

It then fans out the event to all active push servers.

```
                Broadcast Worker
                       |
             ┌─────────┼─────────┐
             |         |         |
             ▼         ▼         ▼
          Push 1    Push 2    Push 3
```

Now Kafka has only one logical subscription for the broadcast processing pipeline.

We still need to distribute the event to all relevant push servers, but that happens at the application fanout layer.

This is generally easier to scale and operate than maintaining thousands of independent Kafka consumer groups.

However, there is an important operational detail:

The broadcast worker itself must handle the fanout reliably, especially when thousands of destinations are involved.

For extremely large deployments, we'd divide push servers into regions, racks, or fanout partitions rather than creating a single worker that performs one enormous synchronous broadcast.

# Part G — Topic Design: How Many Topics Do We Actually Need?

## 16. The most common mistake: Creating one topic per user or group

Let's say we have:

- 10 million users.
- 5 million chat groups.
- 2 million active documents.

A naive design might create:

```
user-1
user-2
user-3
...
user-10000000
```

And:

```
group-1
group-2
group-3
...
group-5000000
```

And:

```
document-1
document-2
...
```

This is generally a poor Kafka architecture.

Why?

A Kafka topic isn't just a string identifier.

Topics have operational infrastructure associated with them, such as:

- Partition metadata.
- Replication information.
- Broker-side logs.
- Leader/follower assignment.
- Consumer metadata and coordination implications.

Millions of tiny topics are generally much harder to operate than a smaller number of appropriately partitioned topics.

More importantly, we don't need individual topics to distinguish individual recipients.

We already have identifiers inside events. We can use those identifiers for routing and partitioning.

## 16.1 The better approach: Topics represent logical streams

Start with a small set of domain-oriented topics.

For example:

```
order-events
payment-events
chat-events
document-operations
user-notifications
global-announcements
```

Now consider chat.

We publish every group message to:

```
chat-events
```

But use the group ID as the message key.

```
{
  "key": "group-G1",
  "value": {
    "eventId": "event-701",
    "groupId": "G1",
    "senderId": "user-A",
    "message": "Hello"
  }
}
```

Another message:

```
{
  "key": "group-G2",
  "value": {
    "eventId": "event-702",
    "groupId": "G2",
    "senderId": "user-B",
    "message": "Hey"
  }
}
```

Both records belong to the same topic.

But their keys can cause them to go to different partitions.

```
Topic: chat-events

Partition 0:
    G1 -> Event 701
    G1 -> Event 703
    G4 -> Event 707

Partition 1:
    G2 -> Event 702
    G2 -> Event 704

Partition 2:
    G3 -> Event 705
    G3 -> Event 706
```

We can have millions of distinct group IDs without creating millions of Kafka topics.

This is the critical distinction:

| Concept     | Represents                                          |
| ----------- | --------------------------------------------------- |
| Topic       | A logical stream/category of events                 |
| Partition   | A scalable ordered shard of that stream             |
| Message key | An entity or routing key used to select a partition |
| Payload     | The actual event and its attributes                 |

### A practical topic design

For a real-time platform, an initial design might look like this:

| Topic                 | Example key              | Purpose                              |
| --------------------- | ------------------------ | ------------------------------------ |
| `chat-events`         | conversationId           | Chat message events                  |
| `document-operations` | documentId               | Collaborative editing operations     |
| `user-notifications`  | userId                   | Individual notification events       |
| `global-events`       | eventCategory or matchId | Global or category-level events      |
| `presence-events`     | userId                   | User presence changes                |
| `delivery-events`     | recipientId              | Delivery lifecycle events, if needed |

These are examples, not a mandatory topic list.

We create separate topics when streams need different:

- Retention policies.
- Throughput and partition configurations.
- Access controls.
- Schemas and ownership.
- Ordering boundaries.
- Consumer lifecycles.

We shouldn't create a new topic just because a new user or group appears.

## 17. How does one topic support multiple audiences?

Let's take `chat-events`.

It contains events for many conversations:

```
chat-events:

    G1 -> Message A
    G2 -> Message B
    G3 -> Message C
    G1 -> Message D
    G4 -> Message E
    G2 -> Message F
```

A fanout router consumer group subscribes to the topic.

```
                   chat-events
                        |
                        ▼
               Consumer Group:
                 chat-fanout
                        |
                  ┌─────┴─────┐
                  |           |
                  ▼           ▼
               Router 1    Router 2
```

The routers consume the events assigned to their partitions.

Each router examines the event's group ID and performs routing.

```
Event G1
   |
   ▼
Find G1's audience
   |
   ▼
Find active push servers
   |
   ▼
Deliver to those servers
```

The routing decision happens in application logic.

We don't need a Kafka topic per group.

### Important distinction: Partitioning is not routing to individual push servers

Suppose:

```
G1 -> Partition 0
```

This means events for G1 are stored in Partition 0.

It does not mean that only Push Server 0 receives them.

Kafka partitions distribute event processing across the router consumer group.

The router then determines the destination push servers.

This distinction is fundamental.

## 18. How do we choose the partition key?

This is one of the most important Kafka design decisions.

The partition key determines which records should share a partition.

Choose it based on the ordering boundary you need.

| Application        | Possible key   | Ordering objective                           |
| ------------------ | -------------- | -------------------------------------------- |
| Chat               | conversationId | Preserve message order within a conversation |
| Collaboration      | documentId     | Preserve operation order within a document   |
| User notifications | userId         | Preserve notification order per user         |
| Order processing   | orderId        | Preserve order lifecycle events              |
| Payment processing | paymentId      | Preserve events for one payment              |
| Live sports        | matchId        | Preserve events within one match             |

For example:

```
Key = conversationId
```

All events for that conversation normally go to the same partition.

That means the consumer can process them in partition order.

But consider this:

```
G1 generates 100,000 messages/second.

G2 generates 2 messages/second.

G3 generates 5 messages/second.
```

If G1 is assigned to one partition, that partition may become a bottleneck.

This is called a hot partition.

You cannot solve it simply by adding more consumers to the same consumer group, because a partition is assigned to only one consumer in that group at a time.

Possible solutions include:

- More partitions, to distribute different groups across more partitions.
- Separating exceptionally hot groups into dedicated streams.
- Using more complex subpartitioning if strict group-wide ordering can be relaxed.
- Applying batching, coalescing, or aggregation.
- Designing an application-level sequence/reordering mechanism.

There is always a tradeoff between parallelism and ordering.

# Part H — Which Technology Should We Actually Use?

## 19. Redis Pub/Sub vs Redis Streams vs Kafka vs RabbitMQ

Now that the semantics are clear, let's compare practical technologies.

### A. Redis Pub/Sub

Redis Pub/Sub provides lightweight, low-latency message distribution.

```
Publisher
   |
   ▼
Redis Channel
   |
   ├── Subscriber 1
   ├── Subscriber 2
   └── Subscriber 3
```

Characteristics:

- Very low-latency distribution.
- Every active subscriber to a channel receives the message.
- No retained message history.
- Disconnected subscribers miss messages.
- No normal replay mechanism.
- No durable consumer offset.

Suitable for:

- Ephemeral events.
- Live presence hints.
- Typing indicators.
- Live price updates.
- Push-server notifications where missed signals can be recovered.

Not suitable as the sole delivery mechanism for important durable notifications.

### B. Redis Streams

Redis Streams provide a persistent stream data structure.

```
Producer
   |
   ▼
Redis Stream
   |
   ├── Consumer Group A
   |
   └── Consumer Group B
```

They support:

- Retained records.
- Consumer groups.
- Consumer progress.
- Acknowledgements.
- Pending entries.
- Replay.
- Redelivery workflows.

For example:

```
Stream: notification-events

Consumer group: delivery-workers

    Worker 1
    Worker 2
    Worker 3
```

The workers share processing.

A consumer that has not acknowledged a message leaves it in the pending entries list, where recovery logic can eventually reclaim it.

Suitable for:

- Moderate-scale durable task processing.
- Smaller event pipelines.
- Applications already using Redis heavily.
- Workflows where stream retention and throughput fit the system's capacity.

But remember that Redis has different memory, persistence, replication, trimming, and operational characteristics from Kafka.

Redis Streams are not automatically a drop-in replacement for Kafka at every scale.

### C. RabbitMQ

RabbitMQ is particularly useful for task queues and flexible message routing.

A typical topology:

```
Producer
   |
   ▼
Exchange
   |
   ├── Queue A
   |
   ├── Queue B
   |
   └── Queue C
```

Different exchange types give us different routing behavior:

| Exchange type | Behavior                       |
| ------------- | ------------------------------ |
| Direct        | Route by exact routing key     |
| Topic         | Route using wildcard patterns  |
| Fanout        | Copy to all bound queues       |
| Headers       | Route based on message headers |

Queues can provide:

- Message persistence with appropriate configuration.
- Consumer acknowledgements.
- Redelivery.
- Dead-letter routing.
- Competing consumers.
- Prefetch controls.

A useful scenario:

We need to execute notification-delivery jobs with retries and controlled worker concurrency.

```
Notification Exchange
       |
       ▼
Delivery Queue
       |
   ┌───┼────┐
   |   |    |
   ▼   ▼    ▼
  W1  W2   W3
```

This is a natural RabbitMQ-style workload.

### D. Apache Kafka

Apache Kafka is particularly useful for retained, high-throughput event streams.

```
Producer
   |
   ▼
Kafka Topic
   |
   ├── Partition 0
   ├── Partition 1
   ├── Partition 2
   └── Partition 3
```

It provides:

- Persistent retained logs.
- Partition-level ordering.
- Horizontal scaling.
- Consumer groups.
- Independent consumer progress.
- Replay within retention.
- Replication across brokers.
- High-throughput batch processing.

Suitable for:

- High-volume event pipelines.
- Chat event history pipelines.
- Durable business-event processing.
- Large-scale fanout workers.
- Event-driven architectures.
- Analytics pipelines.
- Multiple independent downstream consumers.

Kafka is not automatically the best choice for every message that needs to travel to a connected user.

Its strengths are event retention, replay, throughput, and scalable consumption.

## 20. Technology selection table

Here is how I'd approach the decision.

| Requirement                                       | Starting choice                                          |
| ------------------------------------------------- | -------------------------------------------------------- |
| Very low-latency, ephemeral broadcast             | Redis Pub/Sub or NATS Core                               |
| Durable stream with replay                        | Kafka                                                    |
| Background jobs with retries and acknowledgements | RabbitMQ or Redis Streams                                |
| High-volume distributed event processing          | Kafka                                                    |
| Multiple independent consumers of event history   | Kafka                                                    |
| Dynamic routing using exchange patterns           | RabbitMQ                                                 |
| Lightweight internal push-server notifications    | Redis Pub/Sub, NATS, or RPC                              |
| Reliable offline user notifications               | Durable application inbox + event pipeline               |
| Short-lived typing/presence updates               | Pub/Sub                                                  |
| Collaborative operations requiring ordering       | Partitioned Kafka stream, or another ordered durable log |

The technologies overlap. These are starting choices, not rigid rules.

# Part I — Combining Kafka and Pub/Sub

## 21. Why would we need both?

Consider a system where:

- We cannot lose important notification events.
- We need high-throughput processing.
- We need low-latency delivery to connected clients.
- We need to avoid every push server consuming every event.

Kafka is good at durable event processing.

Redis Pub/Sub is good at lightweight broadcast to active subscribers.

We can combine them.

### Architecture

```
                Business Service
                       |
                       ▼
                  Kafka Topic
                       |
                       ▼
              Fanout / Router Workers
                       |
                Connection Registry
                       |
                       ▼
               Target Push Server
                       |
                       ▼
                 Redis Pub/Sub
                       |
                       ▼
                 Local Push Server
                       |
                       ▼
                    Client
```

There are two different responsibilities:

Kafka:

The durable source of events.

Redis Pub/Sub:

A fast delivery mechanism to an active push server.

For example:

```
Kafka event:

user-A needs notification 501
```

The router looks up:

```
user-A -> Push Server 2
```

It publishes to a node-specific channel:

```
push-node:2
```

Push Server 2 subscribes to its own channel.

```
             Router Worker
                   |
                   ▼
          Redis Channel:
             push-node:2
                   |
                   ▼
               Push 2
                   |
                   ▼
                User A
```

This avoids delivering the event to all push servers.

### But there's a major catch.

What if Push Server 2 is disconnected from Redis when the router publishes the event?

Redis Pub/Sub does not retain the message.

The push server misses it.

Therefore, the Kafka record and routing workflow need a recovery strategy.

For example:

- Replay the event from Kafka when appropriate.
- Persist a durable recipient delivery record.
- Use a durable per-server delivery queue.
- Let the client fetch missed data from its authoritative application inbox.

For important messages, I strongly prefer the last two layers of protection:

Persist the business notification or recipient delivery record, then use Pub/Sub as a low-latency wake-up signal.

This is often a much better design than expecting Pub/Sub to guarantee client delivery.

## 22. Another common architecture: Kafka + RabbitMQ

We can also combine Kafka and RabbitMQ.

For example:

```
                 Business Service
                        |
                        ▼
                       Kafka
                        |
                        ▼
                 Notification Router
                        |
                        ▼
                    RabbitMQ
                        |
                 Delivery Queues
                        |
                ┌───────┼───────┐
                |       |       |
                ▼       ▼       ▼
               W1      W2      W3
```

Here:

Kafka retains the business events.

RabbitMQ distributes concrete delivery tasks to competing workers.

For example:

```
Kafka event:
    Group G1 received message 501.
```

The router expands the event into delivery jobs.

```
Delivery jobs:

    Deliver message 501 to User A
    Deliver message 501 to User B
    Deliver message 501 to User C
```

These jobs enter a queue.

Workers process them and route them to the appropriate push servers.

This architecture is particularly useful when delivery tasks need:

- Independent retries.
- Per-task acknowledgements.
- Concurrency control.
- Dead-letter queues.
- Operational isolation from the event-processing pipeline.

But adding both Kafka and RabbitMQ increases operational complexity.

Don't introduce both unless their distinct capabilities solve a real requirement.

# Part J — The Most Important Reliability Distinction

## 23. An acknowledgement can mean four different things

Let's say Kafka contains:

```
{
  "eventId": "event-801",
  "userId": "user-A",
  "type": "PAYMENT_COMPLETED"
}
```

The router consumes it.

The router locates Push Server 1.

Push Server 1 writes to the WebSocket connection.

What should we acknowledge, and when?

Let's distinguish four different levels.

### Level 1: Producer acknowledgement

```
Business Service -> Kafka
```

Kafka confirms that the record was accepted according to the configured producer and broker durability settings.

This does not mean any consumer has processed it.

### Level 2: Consumer acknowledgement / offset commit

```
Kafka -> Routing Worker
```

The routing worker processes the event and commits its progress.

This means the worker considers that Kafka record processed.

It does not mean a client received it.

### Level 3: Push-server acknowledgement

```
Routing Worker -> Push Server 1
```

Push Server 1 confirms that it accepted the delivery request.

Even this does not necessarily mean that a socket write completed or the client received anything.

### Level 4: Client application acknowledgement

```
Push Server 1 -> Client
Client -> Push Server 1
```

The client acknowledges a specific event after processing it.

For example:

```
{
  "type": "DELIVERY_ACK",
  "eventId": "event-801",
  "status": "PROCESSED"
}
```

This is the strongest of these four signals, although we still need to define exactly what "processed" means.

For example, did the client:

- Receive it?
- Parse it?
- Update the UI?
- Persist it locally?

The application must define the acknowledgement contract.

### Keep these separate

| Acknowledgement        | What it proves                                                  |
| ---------------------- | --------------------------------------------------------------- |
| Producer ACK           | Broker accepted/persisted the record according to configuration |
| Kafka consumer offset  | Router considers the event processed                            |
| Push-server ACK        | Destination server accepted the delivery request                |
| Client application ACK | Client claims the event was processed                           |

A Kafka acknowledgement is not a WebSocket delivery acknowledgement.

This is probably the most important distinction in the entire chapter.

## 24. When should the Kafka consumer commit its offset?

Consider this sequence:

```
1. Router consumes Kafka event.

2. Router finds Push Server 1.

3. Router publishes to Redis Pub/Sub.

4. Router commits Kafka offset.

5. Push Server 1 was disconnected from Redis.

6. Event is lost at the push-server delivery layer.
```

Kafka won't automatically replay the committed record.

The router already acknowledged its processing progress.

The underlying issue is that the router treated publishing to an ephemeral channel as successful durable delivery.

We need a better approach for important messages.

### Option A: Durable handoff

```
Kafka
  |
  ▼
Router
  |
  ▼
Durable Delivery Store / Queue
  |
  ▼
Push Server
  |
  ▼
Client
```

The router commits Kafka progress only after the event has been safely handed off to the durable downstream processing system.

### Option B: Durable recipient inbox

```
Business Service
      |
      ▼
Persist Notification
      |
      ▼
Publish Wake-up Event
      |
      ▼
Push Server
      |
      ▼
Client fetches / receives notification
```

Even if the push signal is lost, the notification remains persisted.

The client can recover it later.

For many notification systems, this is an excellent approach.

### Option C: Best-effort real-time push

For ephemeral updates, the router can send over Pub/Sub and accept that some events may be missed.

The client obtains the latest state through another mechanism.

The key is to select the appropriate reliability level rather than making everything durable unnecessarily.

# Part K — A Practical Architecture for Each Audience

## 25. Let's build the final architecture matrix

We now understand enough to map messaging technology to routing patterns.

| Scenario                | Event transport                         | Fanout / routing                        | Offline behavior                    |
| ----------------------- | --------------------------------------- | --------------------------------------- | ----------------------------------- |
| Global live scores      | Kafka if retained, Pub/Sub if ephemeral | Broadcast to relevant push servers      | Fetch latest snapshot               |
| Global announcement     | Kafka or durable notification pipeline  | Fanout to all push servers              | Persist announcement/inbox          |
| Small group chat        | Kafka or durable chat pipeline          | Resolve members and destination servers | Fetch messages from chat DB         |
| Large group chat        | Kafka                                   | Group-to-server interest registry       | Fetch messages from chat DB         |
| Collaborative document  | Ordered partitioned stream              | Document-aware fanout                   | Replay operations or fetch snapshot |
| Individual notification | Queue/stream                            | User connection lookup                  | Durable inbox                       |
| Typing indicator        | Pub/Sub                                 | Group/user routing                      | Discard                             |
| Presence update         | Pub/Sub or durable state-change event   | Interested servers                      | Reconstruct current presence        |
| Device-specific command | Queue/stream if important               | Connection ID lookup                    | Retry or mark expired               |

These are starting architectures. For example, a chat system can use Kafka for its domain-event pipeline but still persist messages in a relational or wide-column database.

The important thing is to distinguish the authoritative data from the transport.

# 26. The architecture I would start with for a general-purpose reliable push platform

Let's assume the requirements are:

- Millions of connected users.
- Multiple push servers.
- Global, group, and individual-user messages.
- Durable events where required.
- Low-latency delivery.
- Reconnect and recovery support.
- Horizontal scalability.

A reasonable reference design is:

\#chatgpt-mermaid-\_r_5ub\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_5ub\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_5ub\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_5ub\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_5ub\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_5ub\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_5ub\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_5ub\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_5ub\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_5ub\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_5ub\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_5ub\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_5ub\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_5ub\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_5ub\_ p{margin:0;}#chatgpt-mermaid-\_r_5ub\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_5ub\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_5ub\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_5ub\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_5ub\_ .label text,#chatgpt-mermaid-\_r_5ub\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_5ub\_ .node rect,#chatgpt-mermaid-\_r_5ub\_ .node circle,#chatgpt-mermaid-\_r_5ub\_ .node ellipse,#chatgpt-mermaid-\_r_5ub\_ .node polygon,#chatgpt-mermaid-\_r_5ub\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_5ub\_ .rough-node .label text,#chatgpt-mermaid-\_r_5ub\_ .node .label text,#chatgpt-mermaid-\_r_5ub\_ .image-shape .label,#chatgpt-mermaid-\_r_5ub\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_5ub\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_5ub\_ .rough-node .label,#chatgpt-mermaid-\_r_5ub\_ .node .label,#chatgpt-mermaid-\_r_5ub\_ .image-shape .label,#chatgpt-mermaid-\_r_5ub\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_5ub\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_5ub\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_5ub\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_5ub\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_5ub\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_5ub\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_5ub\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_5ub\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_5ub\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_5ub\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_5ub\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_5ub\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_5ub\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_5ub\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_5ub\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_5ub\_ .icon-shape,#chatgpt-mermaid-\_r_5ub\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_5ub\_ .icon-shape p,#chatgpt-mermaid-\_r_5ub\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_5ub\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_5ub\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_5ub\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_5ub\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_5ub\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_5ub\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_5ub\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_5ub\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_5ub\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_5ub\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_5ub\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_5ub\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_5ub\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_5ub\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_5ub\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_5ub\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_5ub\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_5ub\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_5ub\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_5ub\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_5ub\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_5ub\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_5ub\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_5ub\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_5ub\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_5ub\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_5ub\_ .node rect,#chatgpt-mermaid-\_r_5ub\_ .node circle,#chatgpt-mermaid-\_r_5ub\_ .node ellipse,#chatgpt-mermaid-\_r_5ub\_ .node polygon,#chatgpt-mermaid-\_r_5ub\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_5ub\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_5ub\_ .mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_5ub\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_5ub\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_5ub\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}ProducersBusiness Service ABusiness Service BBusiness Service CTransactional OutboxKafka: Domain Event TopicsFanout / Routing ConsumerGroupAudience ResolverMembership / Business DataConnection RegistryDurable Delivery / InboxStoreLow-Latency Node DispatchPush Server 1Push Server 2Push Server 3Connected ClientsConnected ClientsConnected ClientsRecovery / Replay

Let's understand why each part exists.

Transactional Outbox

Prevents a business database transaction from succeeding while its corresponding event publication is silently lost.

Kafka

Maintains a durable event stream and supports scalable event processing.

Fanout/router workers

Determine the intended recipients and destination push servers.

Audience resolver

Looks up group members, users, or active subscriptions.

Connection registry

Identifies active connection owners.

Durable delivery/inbox store

Protects important messages against push-server downtime, disconnected clients, and transient routing failures.

Low-latency node dispatch

Gets the event to the correct push server without making every push server consume every business event.

Push servers

Deliver through their locally owned connections.

Recovery/replay

Ensures clients can obtain messages missed during disconnections.

Not every application needs every component. For example, a live score system can use a much smaller version with a snapshot store and ephemeral broadcast.

# Part L — Common Architectural Mistakes

## 27. Mistakes I would specifically avoid

| Mistake                                                        | Why it is problematic                                      | Better approach                                                    |
| -------------------------------------------------------------- | ---------------------------------------------------------- | ------------------------------------------------------------------ |
| All push servers share one Kafka consumer group for broadcasts | Only one server in the group receives each record          | Broadcast through a fanout layer                                   |
| One Kafka topic per user                                       | Millions of topics create unnecessary operational overhead | One logical topic with user ID as key                              |
| One Kafka topic per group                                      | Same topic explosion problem                               | Shared topic with group ID as key                                  |
| Redis Pub/Sub as the only storage for important messages       | Disconnected subscribers miss messages                     | Durable event/inbox plus Pub/Sub wake-up                           |
| Push server directly queries every business database           | Tight coupling and duplicated business logic               | Domain service owns business data; routing uses defined interfaces |
| Treat successful socket write as client delivery               | Client might never process the message                     | Application ACK when required                                      |
| Commit Kafka offset before a reliable handoff                  | Events can disappear between processing stages             | Durable handoff or recoverable downstream state                    |
| Unlimited per-connection buffers                               | Slow clients can exhaust server memory                     | Bounded buffers and backpressure                                   |
| Kafka for every heartbeat or typing event                      | Often unnecessary persistence and operational overhead     | Lightweight ephemeral messaging                                    |

## 28. A simple set of rules to remember

When choosing the messaging infrastructure, think about these questions.

1\. Does everybody need the event?

Use broadcast/Pub/Sub semantics.

2\. Does exactly one worker need to process the task?

Use a competing-consumer queue or a Kafka consumer group.

3\. Do we need durable history and independent consumers?

Use a retained event stream such as Kafka.

4\. Is the event important when the user is offline?

Persist it in an authoritative business store or durable delivery store.

5\. Does the push server need the event immediately, but losing the signal is recoverable?

Use lightweight dispatch such as Pub/Sub or internal RPC.

6\. Do we need retries and dead-letter handling for individual delivery tasks?

Consider a durable queue or delivery-workflow system.

# 29. Final mental model: Don't think in terms of technologies; think in terms of responsibilities

Let's return to your original system.

A business service wants to send something to a user.

What should happen?

```
              BUSINESS SERVICE
                     |
                     | Generate event
                     ▼
               EVENT STREAM
                  Kafka
                     |
                     | Distribute processing
                     ▼
               ROUTING WORKERS
                     |
                     | Determine audience
                     ▼
              AUDIENCE RESOLUTION
                     |
                     | Find connection owners
                     ▼
             CONNECTION REGISTRY
                     |
                     | Identify destinations
                     ▼
              NODE DISPATCH LAYER
                     |
             ┌───────┼────────┐
             |       |        |
             ▼       ▼        ▼
          PUSH 1   PUSH 2   PUSH 3
             |       |        |
             ▼       ▼        ▼
           CLIENT  CLIENT   CLIENT
```

Each component solves a different problem.

| Component           | Problem solved                                       |
| ------------------- | ---------------------------------------------------- |
| Kafka               | Durable event processing and replay                  |
| Message queue       | Distribute processing tasks among workers            |
| Pub/Sub             | Distribute the same event to independent subscribers |
| Router              | Determine where an event should go                   |
| Connection registry | Locate active connection owners                      |
| Push server         | Deliver through locally owned connections            |
| Durable inbox       | Recover important messages when clients are offline  |

One important qualification: an actual application might remove Kafka, use RabbitMQ instead, use Redis Streams, or combine fewer components. This is a reference architecture, not a mandatory stack.

# 30. What I recommend as starting architectures

To make the technology decisions concrete, here are four configurations.

### Configuration A — Simple real-time dashboard

Requirements:

- Mostly server-to-client.
- Intermediate updates can be lost.
- Latest state is available from a database or cache.

```
Business Service
      |
      ▼
Redis Pub/Sub
      |
      ▼
Push Servers
      |
      ▼
SSE Clients
```

No Kafka is necessarily required.

### Configuration B — Reliable chat application

Requirements:

- Messages must not disappear when users are offline.
- Messages should be ordered within conversations.
- Group delivery is required.

```
Chat Service
     |
     ├── Persist Message
     |
     ▼
Transactional Outbox
     |
     ▼
Kafka: chat-events
     |
     ▼
Chat Fanout Workers
     |
     ▼
Connection Registry
     |
     ▼
Target Push Servers
     |
     ▼
WebSocket Clients
```

The chat database is authoritative. WebSocket delivery is the low-latency optimization; clients can recover from chat history.

### Configuration C — Enterprise notification system

Requirements:

- Individual notifications.
- Durable inbox.
- Retry policies.
- Delivery tracking.

```
Business Service
      |
      ▼
Outbox / Kafka
      |
      ▼
Notification Workers
      |
      ▼
Notification Inbox DB
      |
      ▼
Delivery Workers
      |
      ▼
Push Infrastructure
      |
      ▼
Client
```

The notification remains available even if the user is offline.

### Configuration D — Large-scale collaboration system

Requirements:

- Many active documents.
- Multiple participants.
- Ordered document operations.
- Fast fanout to connected collaborators.

```
Collaboration Gateway
       |
       ▼
Document Event Stream
       |
       | Key = documentId
       ▼
Kafka Partitions
       |
       ▼
Collaboration Router
       |
       ▼
Document -> Active Push Servers
       |
       ▼
Push Servers
       |
       ▼
Collaborators
```

The document's authoritative state, operation ordering, and recovery mechanism are separate concerns from socket delivery.

## Chapter 4 — Final understanding

There are three concepts I particularly want you to retain:

1\. Pub/Sub, queues, and event streams are different delivery semantics.

Pub/Sub broadcasts to subscribers. A competing-consumer queue distributes work. Kafka retains an event log that independent consumer groups can consume.

2\. Kafka topics represent streams, not individual recipients.

Use identifiers such as `userId`, `groupId`, `documentId`, or `orderId` as message keys when they are appropriate ordering boundaries. Avoid creating a topic for every user or group.

3\. Durable event processing and real-time socket delivery are separate problems.

Kafka can reliably retain and distribute events, but it cannot guarantee that a browser processed a WebSocket message. That requires a separate delivery and acknowledgement design.

# Next: Chapter 5 — Connection Registry and Routing Infrastructure

Now we'll go deeper into the infrastructure that makes everything work.

We'll design the actual data structures and operations:

1. How to maintain `userId → connections → pushServerId`.
2. Redis data structures: Hashes, Sets, Sorted Sets, and TTLs.
3. Why a simple `userId → serverId` mapping is insufficient.
4. How multiple devices and browser tabs are handled.
5. How push servers register, refresh, and remove connections.
6. Heartbeat leases and stale connection cleanup.
7. How to prevent an old connection from deleting a newer connection.
8. How to efficiently resolve thousands of group members into a smaller set of destination servers.
9. How to handle push-server crashes and restart generations.
10. How to scale the registry itself and avoid making Redis a bottleneck.

The central question: How can any push server or routing worker locate the correct connection owner quickly and safely, even when thousands of servers and millions of connections are continuously connecting and disconnecting?
