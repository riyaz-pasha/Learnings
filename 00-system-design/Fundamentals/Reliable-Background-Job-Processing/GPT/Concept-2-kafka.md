# Reliable Background Job Processing with Apache Kafka

Let's take the exact background-processing system we designed with SQS and replace SQS with Apache Kafka.

We will not simply replace the queue name. We need to understand how the entire architecture changes because Kafka has a fundamentally different consumption model from SQS.

The most important difference is:

- SQS: A worker receives a message, processes it, and deletes the message when finished.
- Kafka: A consumer reads a record from a partition and advances a committed offset to record how far it has processed.

This one difference changes how we handle acknowledgements, retries, long-running tasks, ordering, parallelism, worker failures and recovery.

We'll build the architecture from first principles, using an order-processing system as our running example.

By the end, you should understand both how Kafka works internally and how to build a reliable production-grade task-processing system on top of it.

## 1. Our running example

Imagine an e-commerce platform.

A customer places an order.

The system needs to perform several operations:

1. Validate the order.
2. Reserve inventory.
3. Authorize payment.
4. Generate an invoice.
5. Notify the customer.
6. Update the order status.

Some of these operations are asynchronous.

More importantly, the operations associated with one order must happen in the correct order.

For example:

```
Order 101:

ORDER_CREATED
    |
    v
INVENTORY_RESERVED
    |
    v
PAYMENT_AUTHORIZED
    |
    v
INVOICE_GENERATED
    |
    v
ORDER_CONFIRMED
    |
    v
NOTIFICATION_SENT
```

We cannot authorize payment before creating the order.

We cannot generate an invoice before the payment is authorized.

We cannot notify the customer that the order is confirmed before the order is actually confirmed.

At the same time, we want to process thousands of different orders concurrently.

For example:

```
Order 101: Processing
Order 102: Processing
Order 103: Processing
Order 104: Processing
Order 105: Processing
```

This gives us two requirements:

Requirement A: Ordering

Events belonging to the same order must be processed in the correct order.

Requirement B: Parallelism

Different orders should be processed concurrently.

This is exactly where Kafka's topic partitioning and consumer groups become important.

# 2. First understand Kafka's mental model

Before designing the worker architecture, we must understand what Kafka actually stores.

Kafka is not simply a queue containing messages waiting to be deleted.

Kafka is a distributed, persistent, partitioned append-only log.

That distinction is fundamental.

## 2.1 Topic

A topic is a logical category of events.

For example:

```
order-events
payment-events
inventory-events
notification-events
```

Imagine our `order-events` topic contains:

```
ORDER_CREATED
ORDER_UPDATED
ORDER_CANCELLED
ORDER_CONFIRMED
```

A producer writes events to a topic.

Consumers read events from the topic.

But unlike SQS, consuming a message doesn't normally remove it from Kafka.

The event remains available according to the topic's retention policy.

For example:

```
Topic: order-events

Retention: 7 days
```

Consumers can read the same event multiple times, including from different consumer groups.

This is extremely powerful.

For example:

```
                    order-events
                          |
             +------------+------------+
             |            |            |
             v            v            v
        Order Service  Analytics    Notification
         Consumer       Consumer      Consumer
          Group A       Group B        Group C
```

Each consumer group has its own independent position in the topic.

The analytics system can read an event without affecting the order service.

The notification service can independently read the same event.

This is one reason Kafka is particularly useful for event-driven architectures.

Official Kafka documentation describes topics as multi-producer, multi-subscriber streams whose records remain available according to their retention configuration.&#x20;

[image](https://www.google.com/s2/favicons?domain=https://kafka.apache.org\&sz=32)

Apache Kafka+1



## 2.2 Partition

Now let's introduce the most important concept: Partition.

A Kafka topic is divided into multiple partitions.

For example:

```
Topic: order-events

Partition 0
Partition 1
Partition 2
Partition 3
```

Each partition is an independently ordered log.

Imagine the records are:

```
Partition 0:

Offset 0: Event A
Offset 1: Event B
Offset 2: Event C
Offset 3: Event D
Offset 4: Event E
```

Another partition:

```
Partition 1:

Offset 0: Event F
Offset 1: Event G
Offset 2: Event H
```

Another:

```
Partition 2:

Offset 0: Event I
Offset 1: Event J
```

Another:

```
Partition 3:

Offset 0: Event K
Offset 1: Event L
```

An offset is the position of a record within its partition.

It identifies where a record exists in that partition's log.

For example:

```
Topic: order-events
Partition: 2
Offset: 105
```

This identifies a particular position in the Kafka log.

Note that offsets are partition-specific. Offset 105 in partition 2 has no ordering relationship with offset 105 in partition 3.

## 2.3 Ordering guarantee

Kafka guarantees ordering within a partition, not across all partitions.

Consider:

```
Partition 0:

Order 101 - ORDER_CREATED
Order 101 - INVENTORY_RESERVED
Order 101 - PAYMENT_AUTHORIZED

Partition 1:

Order 102 - ORDER_CREATED
Order 102 - INVENTORY_RESERVED
Order 102 - PAYMENT_AUTHORIZED
```

Kafka preserves the record order in each partition.

But Kafka does not promise that an event in partition 0 will be processed before an event in partition 1.

There is no global ordering across the topic.

This is intentional.

If Kafka tried to globally order every event, it would severely limit parallel processing.

We therefore need to decide which events must be ordered together.

That brings us to partition keys.

# 3. Partition key: How do we guarantee per-entity ordering?

Suppose we want all events belonging to the same order to go to the same partition.

We publish messages with the order ID as the Kafka key.

For example:

```
{
  "key": "order-101",
  "value": {
    "eventType": "ORDER_CREATED",
    "orderId": "order-101"
  }
}
```

Another event:

```
{
  "key": "order-101",
  "value": {
    "eventType": "INVENTORY_RESERVED",
    "orderId": "order-101"
  }
}
```

Another:

```
{
  "key": "order-101",
  "value": {
    "eventType": "PAYMENT_AUTHORIZED",
    "orderId": "order-101"
  }
}
```

Kafka's default keyed partitioning maps the same key to the same partition.

Conceptually:

\\[ partition = hash(key) \mod numberOfPartitions \\]

The exact partitioning implementation has details, but the important outcome is:

```
order-101 -> Partition 2
order-101 -> Partition 2
order-101 -> Partition 2

order-102 -> Partition 0
order-102 -> Partition 0

order-103 -> Partition 3
order-103 -> Partition 3
```

Our topic now looks like this:

```
                    order-events
                         |
       +-----------------+-----------------+
       |                 |                 |
       v                 v                 v

   Partition 0       Partition 2       Partition 3

   order-102         order-101         order-103
   CREATED           CREATED           CREATED
   RESERVED          RESERVED          RESERVED
   PAYMENT           PAYMENT           PAYMENT
   CONFIRMED         CONFIRMED         CONFIRMED
```

Now we have something very powerful:

- Events for `order-101` are ordered.
- Events for `order-102` are ordered.
- Events for `order-103` are ordered.
- These three orders can be processed independently and concurrently.

This is called key-based partitioning or semantic partitioning.&#x20;

[image](https://www.google.com/s2/favicons?domain=https://kafka.apache.org\&sz=32)

Apache Kafka+2



## 3.1 Which key should we choose?

This is an architectural decision.

It should represent the entity whose operations require ordering.

| Use case                | Partition key                            |
| ----------------------- | ---------------------------------------- |
| Order processing        | `orderId`                                |
| User activity           | `userId`                                 |
| Account balance updates | `accountId`                              |
| Inventory updates       | `productId` or `warehouseId + productId` |
| Payment lifecycle       | `paymentId`                              |
| Device telemetry        | `deviceId`                               |
| Shipment updates        | `shipmentId`                             |

For example, consider a banking system:

```
Account 1001:

DEPOSIT 500
WITHDRAW 200
DEPOSIT 300
```

These operations must be applied in order.

Using `accountId` as the key ensures they are routed to the same partition.

But deposits and withdrawals for another account can be processed in parallel.

The partition key is essentially your ordering boundary.

## 3.2 An important limitation: Ordering is not the same as business correctness

Suppose two independent services publish events for the same order:

```
Service A:
    ORDER_CREATED

Service B:
    PAYMENT_AUTHORIZED
```

Both use `order-101` as the key.

But Service B might publish its event before Service A's event reaches Kafka.

Kafka cannot automatically know that `ORDER_CREATED` is logically supposed to come first.

Kafka preserves the order in which records are appended to a partition. It does not infer the business sequence across independent producers.

Therefore, for strict business ordering:

- Establish a clear event-producing authority.
- Serialize event publication when necessary.
- Include event sequence numbers or entity versions.
- Validate expected transitions in consumers.

Example:

```
{
  "eventId": "event-102",
  "orderId": "order-101",
  "sequenceNumber": 2,
  "eventType": "PAYMENT_AUTHORIZED"
}
```

A consumer can reject, defer or quarantine an unexpected sequence.

This distinction will become very important when we discuss multiple producers and retries.

# 4. Consumer groups: How do multiple workers process partitions?

Now that we understand partitions, let's introduce consumer groups.

Imagine:

```
Topic: order-events

Partitions:
    P0
    P1
    P2
    P3
```

We create three worker instances:

```
Worker 1
Worker 2
Worker 3
```

All three belong to the same consumer group:

```
Consumer Group:
    order-processing-workers
```

Kafka assigns partitions to consumers within the group.

For example:

\#chatgpt-mermaid-\_r_7mv\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_7mv\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_7mv\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_7mv\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_7mv\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_7mv\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_7mv\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_7mv\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_7mv\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_7mv\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_7mv\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_7mv\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_7mv\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_7mv\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_7mv\_ p{margin:0;}#chatgpt-mermaid-\_r_7mv\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_7mv\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_7mv\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_7mv\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_7mv\_ .label text,#chatgpt-mermaid-\_r_7mv\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_7mv\_ .node rect,#chatgpt-mermaid-\_r_7mv\_ .node circle,#chatgpt-mermaid-\_r_7mv\_ .node ellipse,#chatgpt-mermaid-\_r_7mv\_ .node polygon,#chatgpt-mermaid-\_r_7mv\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_7mv\_ .rough-node .label text,#chatgpt-mermaid-\_r_7mv\_ .node .label text,#chatgpt-mermaid-\_r_7mv\_ .image-shape .label,#chatgpt-mermaid-\_r_7mv\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_7mv\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_7mv\_ .rough-node .label,#chatgpt-mermaid-\_r_7mv\_ .node .label,#chatgpt-mermaid-\_r_7mv\_ .image-shape .label,#chatgpt-mermaid-\_r_7mv\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_7mv\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_7mv\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_7mv\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_7mv\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_7mv\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_7mv\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_7mv\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_7mv\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_7mv\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_7mv\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_7mv\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_7mv\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_7mv\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_7mv\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_7mv\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_7mv\_ .icon-shape,#chatgpt-mermaid-\_r_7mv\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_7mv\_ .icon-shape p,#chatgpt-mermaid-\_r_7mv\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_7mv\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_7mv\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_7mv\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_7mv\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_7mv\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_7mv\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_7mv\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_7mv\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_7mv\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_7mv\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_7mv\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_7mv\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_7mv\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_7mv\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_7mv\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_7mv\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_7mv\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_7mv\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_7mv\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_7mv\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_7mv\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_7mv\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_7mv\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_7mv\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_7mv\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_7mv\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_7mv\_ .node rect,#chatgpt-mermaid-\_r_7mv\_ .node circle,#chatgpt-mermaid-\_r_7mv\_ .node ellipse,#chatgpt-mermaid-\_r_7mv\_ .node polygon,#chatgpt-mermaid-\_r_7mv\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_7mv\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_7mv\_ .mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_7mv\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_7mv\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_7mv\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}Kafka Topic: order-eventsPartition 0Partition 1Partition 2Partition 3Worker 1Worker 2Worker 3

Worker 1 receives partitions 0 and 1.

Worker 2 receives partition 2.

Worker 3 receives partition 3.

Within a traditional Kafka consumer group, a partition is assigned to at most one active consumer at a time.&#x20;

[image](https://www.google.com/s2/favicons?domain=https://kafka.incubator.apache.org\&sz=32)

Apache Kafka+2



Why is this important?

Because the assigned consumer controls the reading position for that partition.

Kafka can therefore preserve partition ordering without needing different consumers to compete for individual records.

## 4.1 What happens when we add more workers?

Suppose we have four partitions and four workers.

```
P0 -> W1
P1 -> W2
P2 -> W3
P3 -> W4
```

Each worker gets one partition.

Now add a fifth worker.

```
P0 -> W1
P1 -> W2
P2 -> W3
P3 -> W4

W5 -> No partition
```

The fifth worker cannot increase the partition-level parallelism.

Why?

Because there are only four partitions.

This leads to a fundamental scalability rule:

\\[ Maximum\ active\ consumers\ per\ group \leq Number\ of\ assigned\ partitions \\]

For example:

| Partitions | Workers | Maximum active consumers |
| ---------- | ------- | ------------------------ |
| 4          | 2       | 2                        |
| 4          | 4       | 4                        |
| 4          | 8       | 4                        |
| 12         | 6       | 6                        |
| 12         | 20      | 12                       |

This is a critical difference from a traditional SQS Standard queue, where adding consumers can increase concurrency without having to preconfigure a fixed number of partitions.

## 4.2 Can one worker process multiple partitions?

Absolutely.

For example:

```
Worker 1:
    Partition 0
    Partition 1
    Partition 2

Worker 2:
    Partition 3
    Partition 4
    Partition 5
```

This is normal.

However, each partition has its own reading position.

Worker 1 needs to maintain separate offsets for partitions 0, 1 and 2.

It must also preserve the correct processing and commit order independently for each partition.

# 5. Offset management: Kafka's equivalent of acknowledging a message

This is the most important Kafka concept for designing reliable workers.

With SQS:

```
Receive message
    |
    v
Process message
    |
    v
Delete message
```

With Kafka:

```
Read record
    |
    v
Process record
    |
    v
Commit offset
```

There is no normal per-record deletion after processing.

Instead, the consumer maintains a position in the partition.

## 5.1 Understand consumer position versus committed offset

Suppose partition 0 contains:

```
Offset 100: ORDER_CREATED
Offset 101: INVENTORY_RESERVED
Offset 102: PAYMENT_AUTHORIZED
Offset 103: ORDER_CONFIRMED
Offset 104: NOTIFICATION_SENT
```

A consumer starts at offset 100.

It reads record 100.

Then reads 101.

Then reads 102.

Kafka tracks the consumer's current position, but that is not necessarily the same as the last successfully committed position.

There are two different concepts:

Current consumer position

Where the consumer will read next.

Committed offset

Where the consumer should resume after restarting or being assigned the partition again.

For example:

```
Current position:
    103

Committed offset:
    100
```

This means the consumer has fetched records up to offset 102, but has not yet durably acknowledged that processing has completed.

Kafka stores committed consumer-group offsets separately from the actual topic data, using its internal offset-management mechanism.&#x20;

[image](https://www.google.com/s2/favicons?domain=https://kafka.apache.org\&sz=32)

Apache Kafka+1



## 5.2 What does committing offset 103 actually mean?

Suppose you successfully processed:

```
Offset 100
Offset 101
Offset 102
```

You commit:

```
Partition 0 -> Offset 103
```

Notice that you commit 103, not 102.

Why?

Because the committed offset represents the next record to consume, not the last record processed.

Therefore:

```
Committed offset = 103

Next record to process = 103
```

This is an important detail when implementing manual offset commits.

## 5.3 Why do we commit only after processing?

Let's consider two possibilities.

Wrong approach: Commit before processing.

```
1. Read offset 100.
2. Commit offset 101.
3. Start processing.
4. Worker crashes.
```

When another worker takes over, it starts from offset 101.

Offset 100 will not normally be processed again by this group.

The task is lost.

This is at-most-once processing.

Correct approach: Process before committing.

```
1. Read offset 100.
2. Process successfully.
3. Persist business result.
4. Commit offset 101.
```

Now suppose the worker crashes after step 3 but before step 4.

Another worker starts from offset 100.

It processes the same record again.

This is at-least-once processing.

We might execute the logic twice, but we haven't lost the event.

This is why idempotency is required.

Apache Kafka explicitly describes these alternatives: committing before processing can lose work, while committing after processing can cause previously completed work to be repeated.&#x20;

[image](https://www.google.com/s2/favicons?domain=https://apache.googlesource.com\&sz=32)

Git at Google+1



# 6. Now design our Kafka-based task-processing architecture

We can retain most of the previous architecture:

- Task creation API.
- Database task table.
- Transactional outbox.
- Task attempts.
- Heartbeats.
- Retry handling.
- Idempotency.
- Fencing tokens.
- Recovery scheduler.

But there is one major change:

Kafka's primary unit of consumption is a partition position, not an individual message acknowledgement.

Let's construct the architecture.

## 6.1 Task creation

The API receives:

```
POST /orders/101/process
```

It creates a task and an outbox event in the same PostgreSQL transaction.

Database:

```
tasks

task_id: task-101
order_id: order-101
status: QUEUED
attempt_count: 0
```

Outbox:

```
event_id: event-101
task_id: task-101
event_type: ORDER_PROCESSING_REQUESTED
status: PENDING
```

The outbox publisher publishes to Kafka.

Example message:

```
{
  "eventId": "event-101",
  "taskId": "task-101",
  "orderId": "order-101",
  "eventType": "ORDER_PROCESSING_REQUESTED"
}
```

Kafka key:

```
order-101
```

This ensures messages for the same order are routed consistently to the same partition.

## 6.2 Kafka topic architecture

Suppose we configure:

```
Topic:
    order-processing

Partitions:
    8

Replication factor:
    3
```

Conceptually:

```
                   order-processing
                           |
       +-------------------+-------------------+
       |                   |                   |
       v                   v                   v

   Partition 0         Partition 1         Partition 2
   Partition 3         Partition 4         Partition 5
   Partition 6         Partition 7
```

All order events are distributed across these partitions based on their keys.

## 6.3 Worker instances

We deploy eight worker instances.

All use:

```
group.id = order-processing-workers
```

Kafka assigns partitions:

```
Partition 0 -> Worker 1
Partition 1 -> Worker 2
Partition 2 -> Worker 3
Partition 3 -> Worker 4
Partition 4 -> Worker 5
Partition 5 -> Worker 6
Partition 6 -> Worker 7
Partition 7 -> Worker 8
```

Worker 1 processes messages from partition 0.

Worker 2 processes messages from partition 1.

And so on.

Within each partition, the consumer reads records in log order.

# 7. The critical difference: Kafka does not have an individual message visibility timeout

Let's compare the ownership mechanism.

With SQS:

```
Worker receives message.

SQS:
    Makes message invisible for 120 seconds.

Worker:
    Extends visibility periodically.

Worker:
    Deletes message after completion.
```

Kafka works differently.

There is no equivalent of extending the visibility timeout for one individual Kafka record.

Why?

Because Kafka does not hand out individual records using the same visibility-and-delete model.

Instead, the consumer group manages ownership of partitions.

For example:

```
order-processing-workers

Worker 1 owns:
    Partition 0
    Partition 1
```

As long as Worker 1 remains a valid member of the group, it continues owning its assigned partitions.

Kafka tracks consumer-group membership through heartbeats and session/liveness mechanisms.

When a consumer leaves the group, fails or stops making progress, Kafka can rebalance the partitions and assign them to other consumers.

Kafka's assignment is partition-level ownership, not task-level ownership.

This is a very important distinction.

## 7.1 Kafka heartbeat

Kafka consumers periodically maintain communication with the group coordination mechanism.

A consumer must also continue calling `poll()` within the allowed interval.

There are two related concepts:

Session timeout / heartbeat

Helps Kafka detect a consumer that is no longer participating in the group.

`max.poll.interval.ms`

Defines the maximum allowed delay between consecutive calls to `poll()`.

This second setting is especially important when processing records takes a long time.

Kafka's Java consumer documentation explains that if the consumer doesn't call `poll()` within the maximum interval, it can leave the group, allowing its partitions to be reassigned. It also recommends separating long-running processing from the poll loop when processing duration is unpredictable.&#x20;

[image](https://www.google.com/s2/favicons?domain=https://kafka.apache.org\&sz=32)

Apache Kafka+1



## 7.2 A very important scenario

Imagine:

```
Kafka consumer:
    Worker 1

Assigned partition:
    Partition 0

Task:
    Generate a report.

Processing duration:
    30 minutes.

max.poll.interval.ms:
    5 minutes.
```

Worker 1 receives a record.

It starts processing synchronously.

It doesn't call `poll()` again for 30 minutes.

What happens?

After the maximum poll interval is exceeded, Kafka can consider the consumer unhealthy for group participation.

The partition can be reassigned.

Suppose Worker 2 now gets partition 0.

But Worker 1 might still be executing the report!

Now we have:

```
Worker 1:
    Still processing old task.

Worker 2:
    Owns partition 0.
    May process the same task again.
```

This is extremely important.

A Kafka consumer being removed from a consumer group does not automatically terminate its business-processing thread.

Just as with our SQS architecture, we have to protect ourselves from stale execution.

# 8. How should we implement long-running Kafka consumers?

There are several approaches.

## Approach A: Process synchronously inside the poll loop

```
while (true) {

    ConsumerRecords<String, TaskMessage> records =
        consumer.poll(Duration.ofMillis(1000));

    for (ConsumerRecord<String, TaskMessage> record : records) {

        process(record);

    }

    consumer.commitSync();
}
```

This is simple.

It works well when:

- Processing duration is short.
- Processing duration is predictable.
- The poll interval can accommodate the processing time.
- You don't need a large amount of concurrency within each partition.

But it has limitations.

Suppose:

```
poll() returns 100 records.

Each record takes 10 seconds.

Total processing duration = 1,000 seconds.
```

The consumer might exceed its poll interval.

You could reduce `max.poll.records`, increase `max.poll.interval.ms`, or process records concurrently, depending on the required ordering guarantees.

For long-running tasks, I would generally avoid this simple synchronous model.

## Approach B: Separate the consumer thread from processing threads

This is a common production approach.

We have:

```
Kafka Consumer Thread
          |
          v
     Poll records
          |
          v
    Processing Queue
          |
     +----+----+
     |         |
     v         v
 Worker 1   Worker 2
     |         |
     +----+----+
          |
          v
      Database
```

The consumer thread keeps polling Kafka.

Processing happens in separate worker threads.

This prevents long-running business processing from blocking the poll loop.

However, there is a critical rule:

Never commit offsets for records that haven't finished processing.

Consider:

```
Partition 0:

Offset 100
Offset 101
Offset 102
```

We dispatch these to three processing threads.

```
Offset 100 -> Worker 1
Offset 101 -> Worker 2
Offset 102 -> Worker 3
```

Now imagine:

```
Offset 100: Still processing.
Offset 101: Completed.
Offset 102: Completed.
```

Can we commit offset 103?

No.

Why?

Because offset 100 is not completed.

If we commit 103 and crash, Kafka will resume from 103.

Offset 100 will be skipped even though it never completed.

We must commit only the highest contiguous completed offset.

For example:

| Offset | Processing result |
| ------ | ----------------- |
| 100    | Running           |
| 101    | Completed         |
| 102    | Completed         |
| 103    | Not started       |

Safe commit position:

```
100
```

That means resume from offset 100.

Once offset 100 completes:

```
100: Completed
101: Completed
102: Completed
```

Now we can safely commit:

```
103
```

This is called a contiguous commit frontier.

It is a fundamental concept when parallelizing Kafka processing.

### But what about ordering?

Even if offset commits are managed correctly, processing offset 101 before offset 100 can violate business ordering.

So we need to distinguish:

- Parallel processing.
- Ordered processing.
- Safe offset advancement.

They are three separate things.

If strict ordering matters within a partition, the simplest design is to process records sequentially within that partition.

We can still process multiple partitions concurrently.

If we need concurrency for different keys that share a partition, we can build per-key sequencing and a contiguous completion tracker, but that increases complexity significantly.

Kafka's consumer API itself is not thread-safe, so the consumer should generally remain controlled by one thread. The official Java client documentation describes this separation and the use of pause/resume for concurrent processing.&#x20;

[image](https://www.google.com/s2/favicons?domain=https://kafka.apache.org\&sz=32)

Apache Kafka+2



## Approach C: Pause a partition while its task is running

This is especially useful for your long-running task-processing scenario.

Imagine Worker 1 owns:

```
Partition 0
Partition 1
```

It receives a long-running task from partition 0.

Instead of continuing to dispatch records from partition 0, it pauses that partition.

```
consumer.pause(
    Collections.singleton(new TopicPartition("order-processing", 0))
);
```

The consumer continues polling.

It can still:

- Maintain group participation.
- Receive records from partition 1.
- Detect rebalance events.
- Continue processing other partitions.

After the task completes:

1. Persist the result.
2. Commit the correct next offset for partition 0.
3. Resume partition 0.

```
consumer.resume(
    Collections.singleton(new TopicPartition("order-processing", 0))
);
```

A paused partition remains assigned, but subsequent poll calls don't return new records from that partition until it is resumed.&#x20;

[image](https://www.google.com/s2/favicons?domain=https://kafka.apache.org\&sz=32)

Apache Kafka+1



For strict ordering, this is a useful design:

```
Partition 0:
    Task A running.
    Task B waiting.
    Task C waiting.

Partition 1:
    Task D running.
    Task E waiting.

Partition 2:
    Task F running.
```

Different partitions can progress concurrently.

Within each partition, processing remains sequential.

This is a good starting architecture when per-key ordering is important and tasks can take a long time.

There is a limitation: if a task takes several hours, the worker must remain a healthy consumer-group member for the duration, handle rebalances, and manage shutdown correctly. For such workloads, I would also consider the more decoupled architecture described later.

# 9. Consumer rebalance: What happens when a worker dies?

This is the Kafka equivalent of losing ownership and allowing another worker to take over.

Suppose we have:

```
Topic:
    order-processing

Partitions:
    4

Workers:
    4
```

Initial assignment:

```
P0 -> W1
P1 -> W2
P2 -> W3
P3 -> W4
```

Worker 2 crashes.

Kafka detects that the consumer is no longer an active group member.

A rebalance happens.

New assignment:

```
P0 -> W1
P1 -> W1 or W3
P2 -> W3
P3 -> W4
```

The exact assignment depends on the configured assignment strategy.

Worker 1 or Worker 3 now owns partition 1.

But where does it start reading?

From the last committed offset for that consumer group and partition.

For example:

```
Partition 1:

Offset 100: Completed and committed.
Offset 101: Completed, but not committed.
Offset 102: Not processed.
```

The last committed position might be:

```
101
```

The new owner resumes from 101.

Therefore, offset 101 can be processed again.

This is normal at-least-once behavior.

## 9.1 What if Worker 2 was still processing when partition ownership changed?

This is the dangerous case.

```
Worker 2:
    Processing offset 101.

Worker 2:
    Stops polling and loses group membership.

Worker 3:
    Gets partition 1.

Worker 3:
    Starts from the last committed offset.

Worker 3:
    Processes offset 101.
```

Meanwhile, Worker 2 might still be processing its old task.

Now we might have duplicate concurrent execution.

This is why we still need:

- Idempotency.
- Database ownership checks.
- Fencing tokens.
- Cancellation on partition revocation.

Kafka protects consumer-group offset ownership, but that doesn't automatically fence arbitrary external operations.

## 9.2 Kafka's group generation versus our DB fencing token

These concepts are related but different.

| Concept                               | Protects                                                |
| ------------------------------------- | ------------------------------------------------------- |
| Kafka group generation / member epoch | Consumer-group ownership and offset commits             |
| Database lease token                  | Application-level task ownership                        |
| External fencing token                | Downstream resources that enforce ownership generations |
| Idempotency key                       | Repeated business operations                            |

Kafka will reject offset commits from a consumer that no longer has authority to commit for the relevant assignment.

But it cannot automatically stop a stale worker from uploading an S3 file or calling a payment API.

That is our responsibility.

## 9.3 Partition revocation handling

A production consumer should implement rebalance callbacks.

The important sequence is:

```
Partition revoked
       |
       v
Stop dispatching new work
       |
       v
Finish in-flight work if possible
       |
       v
Persist completed work
       |
       v
Commit only safe offsets while still authorized
       |
       v
Release partition-specific resources
```

If work cannot finish within the rebalance window:

- Cancel it cooperatively where possible.
- Do not commit its offset.
- Release or allow the database lease to expire.
- Let the new consumer recover it.

Never assume an asynchronous processing thread automatically stops when a partition is revoked.

# 10. How do DB leases and heartbeats fit into Kafka?

You might be wondering:

> In Kafka, a consumer group already assigns a partition to one consumer. Do we still need the database lease that we designed for SQS?

The answer is: Kafka partition ownership and database task ownership solve different problems.

Let's understand this carefully.

## 10.1 Kafka ownership

Suppose:

```
Partition 0 -> Worker 1
```

Kafka considers Worker 1 the active consumer for partition 0 within this group.

Another consumer in the same group will not normally be assigned that same partition concurrently.

This provides partition-level exclusivity.

## 10.2 Database ownership

Now consider:

```
Worker 1:
    Reads task-101 from partition 0.

Worker 1:
    Starts processing.

Worker 1:
    Loses Kafka group membership.

Worker 2:
    Takes over partition 0.
    Replays task-101.
```

Worker 1 might still be executing its old processing thread.

Kafka can prevent Worker 1 from committing offsets after losing group membership, but it cannot automatically prevent Worker 1 from making an external database or API request.

Therefore, we can keep our database lease mechanism.

For example:

```
UPDATE tasks
SET
    status = 'RUNNING',

    attempt_count = attempt_count + 1,

    lease_owner = :worker_id,

    lease_token = lease_token + 1,

    lease_expires_at =
        clock_timestamp() + INTERVAL '90 seconds',

    heartbeat_at = clock_timestamp()

WHERE id = :task_id

AND (
    status IN ('QUEUED', 'RETRY_WAIT')
    OR (
        status = 'RUNNING'
        AND lease_expires_at < clock_timestamp()
    )
)

RETURNING *;
```

The worker can process the task only if it successfully acquires the database lease.

Every state-changing query must validate the fencing token.

## 10.3 What happens during a rebalance?

Let's walk through it.

```
Initial:

Worker 1:
    Kafka partition 0 owner.

    Task task-101:
        lease_token = 41
```

Worker 1 starts processing.

Then a rebalance occurs.

```
Worker 2:
    Receives partition 0.

    Replays task-101.

    Attempts DB claim.
```

But the database still contains:

```
status = RUNNING

lease_owner = worker-1

lease_token = 41

lease_expires_at = future time
```

Worker 2 cannot immediately claim the task.

It must wait for the old lease to expire, or for the old worker to release it safely.

If Worker 1 finishes its processing and successfully completes the task while its database lease is valid, Worker 2 can subsequently observe the terminal database state and acknowledge the Kafka record.

If Worker 1 cannot finish, its lease eventually expires, and Worker 2 can reclaim it with token 42.

```
Worker 1:
    Token 41.

Worker 2:
    Token 42.
```

Worker 1 becomes a stale owner.

Its later updates are rejected.

This is exactly the fencing mechanism we developed earlier.

### Important additional rule

When Kafka tells a consumer that a partition has been revoked, its application should stop starting new work for that partition and cooperatively cancel or safely finish existing work.

It should also stop renewing task leases for work it no longer has authority to continue.

The DB lease remains a second layer of protection, not permission for an old consumer to keep executing indefinitely.

## 10.4 What does the heartbeat look like now?

There are two completely different heartbeat mechanisms.

| Heartbeat                                     | Purpose                                                                                                 |
| --------------------------------------------- | ------------------------------------------------------------------------------------------------------- |
| Kafka consumer heartbeat / poll participation | Proves that the consumer is participating in its consumer group and helps maintain partition ownership. |
| Application DB heartbeat                      | Proves that the worker still owns a particular business task.                                           |

With Kafka, we no longer need:

```
ChangeMessageVisibility()
```

That is specific to SQS.

Our heartbeat model becomes:

DatabaseKafkaWorkerDatabaseKafkaWorker#chatgpt-mermaid-\_r_815\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_815\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_815\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_815\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_815\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_815\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_815\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_815\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_815\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_815\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_815\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_815\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_815\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_815\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_815\_ p{margin:0;}#chatgpt-mermaid-\_r_815\_ .actor{stroke:rgb(31, 78, 148);fill:rgb(9, 23, 44);stroke-width:1;}#chatgpt-mermaid-\_r_815\_ rect.actor.outer-path[data-look="neo"]{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_815\_ rect.note[data-look="neo"]{stroke:rgb(58, 132, 63);fill:rgb(48, 48, 48);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_815\_ text.actor>tspan{fill:rgb(237, 237, 237);stroke:none;}#chatgpt-mermaid-\_r_815\_ .actor-line{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_815\_ .innerArc{stroke-width:1.5;stroke-dasharray:none;}#chatgpt-mermaid-\_r_815\_ .messageLine0{stroke-width:1.5;stroke-dasharray:none;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_815\_ .messageLine1{stroke-width:1.5;stroke-dasharray:2,2;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_815\_ [id$="-arrowhead"] path{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_815\_ .sequenceNumber{fill:#505050;}#chatgpt-mermaid-\_r_815\_ [id$="-sequencenumber"]{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_815\_ [id$="-crosshead"] path{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_815\_ .messageText{fill:rgb(237, 237, 237);stroke:none;}#chatgpt-mermaid-\_r_815\_ .labelBox{stroke:rgba(255, 255, 255, 0.15);fill:rgb(0, 0, 0);filter:none;}#chatgpt-mermaid-\_r_815\_ .labelText,#chatgpt-mermaid-\_r_815\_ .labelText>tspan{fill:rgb(237, 237, 237);stroke:none;}#chatgpt-mermaid-\_r_815\_ .loopText,#chatgpt-mermaid-\_r_815\_ .loopText>tspan{fill:rgb(237, 237, 237);stroke:none;}#chatgpt-mermaid-\_r_815\_ .sectionTitle,#chatgpt-mermaid-\_r_815\_ .sectionTitle>tspan{fill:rgb(237, 237, 237);stroke:none;}#chatgpt-mermaid-\_r_815\_ .loopLine{stroke-width:2px;stroke-dasharray:2,2;stroke:rgba(255, 255, 255, 0.15);fill:rgba(255, 255, 255, 0.15);}#chatgpt-mermaid-\_r_815\_ .note{stroke:rgb(58, 132, 63);fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_815\_ .noteText,#chatgpt-mermaid-\_r_815\_ .noteText>tspan{fill:rgb(237, 237, 237);stroke:none;font-weight:normal;}#chatgpt-mermaid-\_r_815\_ .activation0{fill:rgb(48, 48, 48);stroke:hsl(0, 0%, 8.8235294118%);}#chatgpt-mermaid-\_r_815\_ .activation1{fill:rgb(48, 48, 48);stroke:hsl(0, 0%, 8.8235294118%);}#chatgpt-mermaid-\_r_815\_ .activation2{fill:rgb(48, 48, 48);stroke:hsl(0, 0%, 8.8235294118%);}#chatgpt-mermaid-\_r_815\_ .actorPopupMenu{position:absolute;}#chatgpt-mermaid-\_r_815\_ .actorPopupMenuPanel{position:absolute;fill:rgb(9, 23, 44);box-shadow:0px 8px 16px 0px rgba(0,0,0,0.2);filter:drop-shadow(3px 5px 2px rgb(0 0 0 / 0.4));}#chatgpt-mermaid-\_r_815\_ .actor-man circle,#chatgpt-mermaid-\_r_815\_ line{fill:rgb(9, 23, 44);stroke-width:2px;}#chatgpt-mermaid-\_r_815\_ g rect.rect{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_815\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_815\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_815\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_815\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_815\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_815\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_815\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_815\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_815\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_815\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_815\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_815\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_815\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_815\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_815\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_815\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_815\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_815\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}loop[While processing]poll()Records from assigned partitionClaim taskLease acquired, token 41Continue poll / maintain group participationRenew task lease using token 41Lease renewedComplete task using token 41SUCCESSCommit next offsetOffset committed

There is an important implementation detail: if a background processing thread is running, the main Kafka consumer thread should continue polling. The worker thread should communicate its completion back to the consumer thread, which manages offset commits.

Do not call Kafka consumer APIs concurrently from arbitrary processing threads. The standard Java `KafkaConsumer` is not thread-safe.&#x20;

[image](https://www.google.com/s2/favicons?domain=https://kafka.apache.org\&sz=32)

Apache Kafka+1



# 11. Exactly what happens after a worker completes a task?

Let's follow a specific record.

```
Topic:
    order-processing

Partition:
    2

Offset:
    105

Task:
    task-101
```

Worker 1 receives the record.

It successfully claims the task.

The database contains:

```
task-101:

status = RUNNING
lease_owner = worker-1
lease_token = 41
```

Worker 1 processes the task.

On success:

### Step 1: Persist business output

For example, save the generated invoice in S3 and its metadata in PostgreSQL.

### Step 2: Update the database task state

```
BEGIN;

UPDATE tasks
SET
    status = 'SUCCEEDED',

    completed_at = clock_timestamp(),

    lease_owner = NULL,

    lease_expires_at = NULL

WHERE id = :task_id

AND status = 'RUNNING'

AND lease_owner = :worker_id

AND lease_token = :lease_token

AND lease_expires_at > clock_timestamp();

-- Verify one task row was updated.

UPDATE task_attempts
SET
    status = 'SUCCEEDED',
    finished_at = clock_timestamp()
WHERE task_id = :task_id
AND lease_token = :lease_token
AND status = 'RUNNING';

COMMIT;
```

### Step 3: Commit the Kafka offset

Our record was at offset 105.

We commit:

```
Partition 2 -> Offset 106
```

This says:

```
Everything before offset 106 has been processed
and the next record to process is 106.
```

### Step 4: What if offset commit fails?

This is an important failure scenario.

```
1. Business processing succeeds.
2. Database is updated to SUCCEEDED.
3. Kafka offset commit fails.
4. Worker crashes.
5. Another consumer takes over.
6. Kafka replays offset 105.
```

The new consumer checks the database:

```
task-101:

status = SUCCEEDED
```

It does not execute the task again.

It simply advances the committed offset.

This is how we make repeated Kafka delivery harmless.

The ordering of operations is: business effect → durable database state → Kafka offset commit.

Never commit the offset before the business result is durably recorded unless you explicitly accept possible message loss.

# 12. Database schema additions for Kafka

We can reuse almost everything from the SQS design.

However, I would make a few additions.

## 12.1 Tasks table

```
CREATE TABLE tasks (
    id UUID PRIMARY KEY,

    task_type VARCHAR(100) NOT NULL,

    aggregate_id VARCHAR(255),

    sequence_number BIGINT,

    payload JSONB NOT NULL,

    status VARCHAR(30) NOT NULL,

    attempt_count INTEGER NOT NULL DEFAULT 0,

    max_attempts INTEGER NOT NULL DEFAULT 5,

    next_attempt_at TIMESTAMPTZ,

    lease_owner VARCHAR(150),

    lease_token BIGINT NOT NULL DEFAULT 0,

    lease_expires_at TIMESTAMPTZ,

    heartbeat_at TIMESTAMPTZ,

    progress_at TIMESTAMPTZ,

    progress JSONB,

    source_topic VARCHAR(255),

    source_partition INTEGER,

    source_offset BIGINT,

    started_at TIMESTAMPTZ,

    completed_at TIMESTAMPTZ,

    last_error_code VARCHAR(100),

    last_error_message TEXT,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
```

The new fields:

| Field              | Purpose                                           |
| ------------------ | ------------------------------------------------- |
| `aggregate_id`     | Identifies the entity whose events need ordering. |
| `sequence_number`  | Identifies the business sequence of an event.     |
| `source_topic`     | Original Kafka topic.                             |
| `source_partition` | Original Kafka partition.                         |
| `source_offset`    | Original Kafka offset.                            |

These fields are helpful for debugging, tracing, replay and deduplication.

## 12.2 Consumer inbox table

This is especially useful.

Remember that an event might be processed multiple times.

We should maintain a unique record of which logical events have been durably admitted by our consumer.

```
CREATE TABLE consumer_inbox (
    event_id UUID PRIMARY KEY,

    consumer_name VARCHAR(150) NOT NULL,

    task_id UUID,

    source_topic VARCHAR(255) NOT NULL,

    source_partition INTEGER NOT NULL,

    source_offset BIGINT NOT NULL,

    processed_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
```

Suppose the consumer reads:

```
event-101
```

It performs the following database transaction:

```
1. Insert event-101 into consumer_inbox.
2. Create or update the corresponding task.
3. Commit.
```

If the same Kafka message is replayed, the unique constraint prevents it from being inserted a second time.

The consumer recognizes that this event has already been durably recorded.

It can then safely advance its Kafka offset.

This is the Inbox Pattern, and it complements the Outbox Pattern.

Together:

```
Producer side:
    Transactional Outbox

Consumer side:
    Idempotent Inbox
```

They are extremely useful when integrating databases and Kafka.

# 13. Retry management in Kafka: This is where things get interesting

With SQS, retrying a message is relatively straightforward:

```
Don't delete message.

Visibility timeout expires.

Message becomes available again.
```

Kafka doesn't work like that.

A consumer controls its progress through offsets.

When a record fails, we have to decide what happens to that partition's position.

There are three main approaches.

## Approach A: Don't commit the failed record's offset

Suppose:

```
Partition 0:

Offset 100: Task A
Offset 101: Task B
Offset 102: Task C
```

Worker processes:

```
Task A -> SUCCESS
Task B -> FAILURE
Task C -> Not processed
```

The committed offset is:

```
101
```

Why 101?

Because offset 100 succeeded, and 101 is the next record that still needs processing.

We don't commit 102.

When processing resumes, Kafka delivers offset 101 again.

This is the simplest retry mechanism.

### But there is a major problem

Suppose Task B is failing due to an external API being unavailable.

Task C might be completely unrelated to Task B.

Yet Task C cannot progress within the same partition while we refuse to advance the offset.

This is called head-of-line blocking.

```
Partition 0:

Task A -> SUCCESS

Task B -> FAILING REPEATEDLY
             |
             v
         BLOCKED

Task C -> WAITING
Task D -> WAITING
Task E -> WAITING
```

Remember, even different order IDs may share one partition.

So a failure in one key can block unrelated keys that happen to be assigned to the same partition.

This approach is acceptable when:

- Strict partition ordering is required.
- Failure delays are short.
- The queue volume is manageable.
- You deliberately want later events to wait behind earlier events.

For long retry delays, this becomes expensive.

You should not sleep inside the consumer thread for five minutes while waiting to retry. The consumer must continue its poll lifecycle, and the partition must be paused appropriately.

## Approach B: Retry topics

This is a widely used Kafka retry pattern.

Instead of blocking the original topic, we publish failed records into another topic.

For example:

```
order-processing
       |
       v
  Processing fails
       |
       v
order-retry-30s
       |
       v
  Retry consumer
       |
       v
  Processing again
```

We can create multiple retry topics:

```
order-processing

order-retry-10s
order-retry-1m
order-retry-5m

order-dlq
```

The retry policy might be:

| Attempt | Destination | Delay             |
| ------- | ----------- | ----------------- |
| 1       | Main topic  | Immediate         |
| 2       | Retry-10s   | 10 seconds        |
| 3       | Retry-1m    | 1 minute          |
| 4       | Retry-5m    | 5 minutes         |
| 5       | DLQ         | Permanent failure |

### How does a delay topic work?

Kafka itself does not provide the same per-message visibility-timeout mechanism as SQS.

A retry topic can be implemented with a scheduler or a consumer that checks the retry timestamp.

The retry message contains:

```
{
  "eventId": "event-101",
  "taskId": "task-101",
  "originalTopic": "order-processing",
  "originalPartition": 2,
  "originalOffset": 105,
  "attempt": 2,
  "nextAttemptAt": "2026-11-05T22:30:00Z",
  "payload": {
    "orderId": "order-101"
  }
}
```

The scheduler releases it when the scheduled time arrives.

A retry topic by itself does not make a message wait until its retry time. You need a delay mechanism.

### The major ordering problem with retry topics

Suppose our original partition contains:

```
Offset 100:
    ORDER_CREATED

Offset 101:
    INVENTORY_RESERVED

Offset 102:
    PAYMENT_AUTHORIZED

Offset 103:
    ORDER_CONFIRMED
```

Assume `INVENTORY_RESERVED` fails and is sent to a retry topic.

The main topic can now continue:

```
ORDER_CREATED
PAYMENT_AUTHORIZED
ORDER_CONFIRMED
```

Later, the inventory event is retried.

We have broken the intended business order!

```
ORDER_CREATED
PAYMENT_AUTHORIZED
ORDER_CONFIRMED
INVENTORY_RESERVED
```

That is unacceptable for a strict order-processing workflow.

Retry topics improve throughput, but they can break the original ordering guarantee.

This is one of the most important Kafka design trade-offs.

### How do we solve this?

There are multiple choices.

Option 1: Preserve strict ordering by blocking the partition.

Do not advance the original offset until the failed event succeeds.

This preserves order but blocks the partition.

Option 2: Use an entity-level sequence gate.

Maintain a database record:

```
order-101

last_completed_sequence = 2
```

Suppose:

```
Sequence 3:
    INVENTORY_RESERVED

Sequence 4:
    PAYMENT_AUTHORIZED

Sequence 5:
    ORDER_CONFIRMED
```

Sequence 3 fails and enters retry.

When sequence 4 arrives, the worker checks:

```
Expected next sequence = 3

Received sequence = 4
```

It does not execute sequence 4.

It keeps it pending.

When sequence 3 eventually succeeds:

```
last_completed_sequence = 3
```

Now sequence 4 can execute.

This permits unrelated orders in the same partition to continue, while preserving per-order business ordering.

The database acts as the final business sequence authority.

Option 3: Model the entire order lifecycle as a state machine.

Instead of blindly processing incoming events, the system validates each transition:

```
ORDER_CREATED
    |
    v
INVENTORY_RESERVED
    |
    v
PAYMENT_AUTHORIZED
```

An event that arrives too early is stored as pending or rejected for later reconciliation.

This is often a better design for complex business workflows.

## Approach C: Application-managed DB retries

For our long-running background task system, this is my preferred approach.

We already have a task table.

When a worker fails:

```
task-101

attempt = 2

status = RETRY_WAIT

next_attempt_at = now + 30 seconds
```

We persist this information in PostgreSQL.

The current Kafka record can be acknowledged by committing its offset only after the failure and retry decision have been durably recorded.

A retry scheduler later publishes a new Kafka event when the task becomes eligible.

The flow:

\#chatgpt-mermaid-\_r_858\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_858\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_858\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_858\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_858\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_858\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_858\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_858\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_858\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_858\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_858\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_858\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_858\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_858\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_858\_ p{margin:0;}#chatgpt-mermaid-\_r_858\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_858\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_858\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_858\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_858\_ .label text,#chatgpt-mermaid-\_r_858\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_858\_ .node rect,#chatgpt-mermaid-\_r_858\_ .node circle,#chatgpt-mermaid-\_r_858\_ .node ellipse,#chatgpt-mermaid-\_r_858\_ .node polygon,#chatgpt-mermaid-\_r_858\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_858\_ .rough-node .label text,#chatgpt-mermaid-\_r_858\_ .node .label text,#chatgpt-mermaid-\_r_858\_ .image-shape .label,#chatgpt-mermaid-\_r_858\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_858\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_858\_ .rough-node .label,#chatgpt-mermaid-\_r_858\_ .node .label,#chatgpt-mermaid-\_r_858\_ .image-shape .label,#chatgpt-mermaid-\_r_858\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_858\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_858\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_858\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_858\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_858\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_858\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_858\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_858\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_858\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_858\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_858\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_858\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_858\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_858\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_858\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_858\_ .icon-shape,#chatgpt-mermaid-\_r_858\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_858\_ .icon-shape p,#chatgpt-mermaid-\_r_858\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_858\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_858\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_858\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_858\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_858\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_858\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_858\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_858\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_858\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_858\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_858\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_858\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_858\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_858\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_858\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_858\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_858\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_858\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_858\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_858\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_858\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_858\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_858\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_858\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_858\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_858\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_858\_ .node rect,#chatgpt-mermaid-\_r_858\_ .node circle,#chatgpt-mermaid-\_r_858\_ .node ellipse,#chatgpt-mermaid-\_r_858\_ .node polygon,#chatgpt-mermaid-\_r_858\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_858\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_858\_ .mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_858\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_858\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_858\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}Kafka ConsumerRead Task EventWorker Claims DB TaskProcess TaskResultDB: SUCCEEDEDDB: RETRY_WAITDB: FAILEDCommit Kafka OffsetRetry SchedulerOutbox EventKafka Main TopicSuccessRetryable failurePermanent failure

This gives us:

- Durable retry state.
- Configurable backoff.
- Retry limits.
- Error classification.
- Attempt history.
- Scheduling.
- Monitoring.

But remember the ordering discussion.

If the retry is delayed and the original Kafka partition continues processing later events, we need an entity-level sequence gate whenever those events depend on the failed event.

# 14. Dead Letter Queue with Kafka

Kafka commonly implements a DLQ using a separate topic.

For example:

```
order-processing
       |
       v
   Processing
       |
       v
   Retry 1
       |
       v
   Retry 2
       |
       v
   Retry 3
       |
       v
order-processing-dlq
```

A DLQ event should preserve enough information for investigation:

```
{
  "eventId": "event-101",
  "taskId": "task-101",
  "originalTopic": "order-processing",
  "originalPartition": 2,
  "originalOffset": 105,
  "attemptCount": 5,
  "errorCode": "INVALID_VIDEO_FORMAT",
  "errorMessage": "Unsupported codec",
  "failedAt": "2026-11-05T22:40:00Z",
  "payload": {
    "videoId": "video-123"
  }
}
```

You can replay DLQ records after fixing the problem.

But there is a very important issue.

Suppose:

```
1. Consumer reads source record.
2. Consumer publishes record to DLQ.
3. DLQ publish succeeds.
4. Consumer crashes before committing source offset.
```

The source record is delivered again.

The consumer might publish it to DLQ again.

We have a duplicate DLQ record.

How can we prevent that?

When both the source and DLQ are Kafka topics, we can use a Kafka transaction:

1. Begin a transaction.
2. Publish the failed record to the DLQ.
3. Include the source consumer offset in the transaction.
4. Commit the transaction.

Either the DLQ record and the offset commit both succeed, or neither becomes visible as committed.

This is Kafka's consume-transform-produce transactional pattern.&#x20;

[image](https://www.google.com/s2/favicons?domain=https://kafka.apache.org\&sz=32)

Apache Kafka+1



An important limitation: the Kafka transaction doesn't automatically include a PostgreSQL update or an external payment API call.

If those operations are also involved, use an outbox/inbox or another coordinated idempotency mechanism.

# 15. Kafka's Exactly-Once Semantics: What does it actually mean?

Kafka is often associated with exactly-once processing.

We should understand what that guarantee covers.

There are two important configurations/mechanisms.

## 15.1 Idempotent producer

Imagine our producer sends:

```
event-101
```

The broker stores it successfully.

However, the response gets lost.

The producer cannot tell whether the broker stored it.

It retries.

Without producer idempotency, Kafka might store:

```
event-101
event-101
```

With idempotent producer support, Kafka uses producer identity and sequence information to prevent duplicates caused by producer retries.

For a reliable producer, the usual configuration includes:

```
acks=all
enable.idempotence=true
```

Kafka 4.2 documentation specifies that idempotence requires acknowledgements from all in-sync replicas, retries enabled and a compatible maximum number of in-flight requests.&#x20;

[image](https://www.google.com/s2/favicons?domain=https://kafka.apache.org\&sz=32)

Apache Kafka+1



But there is an important limitation.

Suppose our outbox publisher publishes the same logical business event again after restarting.

The new publishing session may publish it as a new event.

The producer's idempotence does not replace our stable `eventId` and consumer-side deduplication.

## 15.2 Kafka transactions

Suppose we want:

```
Consume event A
    |
    v
Transform event A
    |
    v
Publish event B
```

We want both:

- Event B published.
- Consumer offset for event A advanced.

To happen atomically.

Kafka supports this with producer transactions.

Conceptually:

```
BEGIN TRANSACTION

    Publish output event B.

    Commit input offset for event A
    as part of the transaction.

COMMIT TRANSACTION
```

If the transaction aborts:

```
Output event B:
    Not visible to read_committed consumers.

Input offset:
    Not advanced by that transaction.

Input event A:
    Can be processed again.
```

This is excellent for Kafka-to-Kafka processing.

For example:

```
orders topic
     |
     v
Order Processor
     |
     v
payments-requested topic
```

Kafka can atomically coordinate the produced records and the consumed offsets.

## 15.3 Does Kafka transaction make database processing exactly once?

No.

Suppose:

```
1. Consume Kafka event.
2. Update PostgreSQL.
3. Commit Kafka offset.
```

These are still two independent transactional systems.

If PostgreSQL succeeds and Kafka offset commit fails, the event can be replayed.

If Kafka offset commits first and PostgreSQL fails, the business update can be lost.

Therefore, with PostgreSQL, use this pattern:

```
BEGIN DB TRANSACTION

    INSERT INTO consumer_inbox(event_id)
    -- Unique event ID

    Apply business state changes.

    Update task state.

COMMIT DB TRANSACTION

Commit Kafka offset.
```

On replay:

```
INSERT consumer_inbox(event_id)
```

The unique constraint detects the existing event.

The worker skips the duplicate business operation.

It can then commit the offset.

This gives us effectively-once database effects, provided the inbox record and business update are in the same DB transaction.

For external side effects, use the external service's idempotency support, or a transactional outbox where applicable.

Kafka's own documentation makes this distinction: exactly-once processing is supported when consuming and producing Kafka records transactionally, while external destinations generally require cooperation from those systems.&#x20;

[image](https://www.google.com/s2/favicons?domain=https://kafka.apache.org\&sz=32)

Apache Kafka+1



# 16. Kafka durability and replication

So far, we've discussed consumers.

Now let's understand what happens if Kafka brokers fail.

Kafka is not just a collection of partitions on one machine.

Partitions are replicated across brokers.

Imagine:

```
Kafka Cluster:

Broker 1
Broker 2
Broker 3
```

A topic has:

```
Replication Factor = 3
```

Partition 0:

```
Leader:
    Broker 1

Follower:
    Broker 2

Follower:
    Broker 3
```

The producer normally sends records to the partition leader.

The followers replicate the partition's log.

If Broker 1 fails, Kafka can elect another eligible replica as leader.

This makes the data resilient to broker failures.

## 16.1 Recommended durability configuration

For a production cluster, a common starting point is:

```
Replication Factor = 3

acks = all

enable.idempotence = true

min.insync.replicas = 2
```

Let's understand the relationship.

Replication factor = 3

We keep three replicas of a partition.

acks = all

The producer waits for acknowledgement from the current in-sync replicas.

min.insync.replicas = 2

A write is rejected when fewer than two in-sync replicas are available.

This creates a durability-versus-availability trade-off.

For example:

```
3 replicas healthy:
    Writes accepted.

1 replica unavailable:
    Two replicas remain in sync.
    Writes can continue.

2 replicas unavailable:
    Only one remains in sync.
    Writes fail under min.insync.replicas = 2.
```

This prefers not accepting potentially unsafe writes over continuing with an insufficient number of replicas.

Kafka documents `acks=all` as its strongest producer acknowledgement setting, with the guarantee dependent on at least one in-sync replica remaining alive.&#x20;

[image](https://www.google.com/s2/favicons?domain=https://kafka.apache.org\&sz=32)

Apache Kafka+1



## 16.2 What happens to consumer offsets if a worker crashes?

Remember, consumer offsets are stored by Kafka as group metadata.

Suppose:

```
Partition 0

Committed offset:
    500
```

Worker 1 crashes.

Worker 2 is assigned partition 0.

It starts from offset 500.

The previous consumer's in-memory position is irrelevant.

Only the committed offset determines the normal restart position.

That is why manual offset management matters so much.

# 17. Partition design: How many partitions should we create?

This deserves special attention.

Partitions determine:

1. Parallelism.
2. Distribution across brokers.
3. Per-partition ordering boundaries.
4. Consumer scalability.
5. Operational overhead.

Suppose:

```
Topic:
    order-processing

Partitions:
    8

Workers:
    8
```

Maximum active consumer parallelism within that group is eight.

What if processing demand increases?

We might increase the partition count to 32.

Then we can have up to 32 active consumers, assuming enough workload and capacity.

But choosing the partition count is not as simple as choosing a number.

## 17.1 Too few partitions

Imagine:

```
Partitions:
    2

Workers:
    20
```

Only two workers can actively consume partitions in that group.

The other 18 cannot increase processing parallelism.

The result can be:

- High queue lag.
- Slow processing.
- Uneven workloads.
- Lower throughput than expected.

## 17.2 Too many partitions

Now consider:

```
Partitions:
    10,000
```

Too many partitions create additional overhead:

- More partition metadata.
- More file handles and log segments.
- More leader and replica management.
- More consumer assignment overhead.
- More recovery and monitoring work.
- More storage and broker resource usage.

There is no universally correct partition count.

It depends on expected throughput, message size, processing duration, ordering requirements, broker capacity and future growth.

## 17.3 Hot partition problem

This is another important design problem.

Suppose we partition by `orderId`.

Our system has:

```
Order 101:
    100 messages/sec

Order 102:
    5 messages/sec

Order 103:
    3 messages/sec
```

All `order-101` messages go to one partition.

If one order produces an extremely high event volume, its partition could become a bottleneck.

This is called a hot partition.

Adding more partitions does not necessarily solve it because all events with the same key still map to the same partition.

Possible solutions:

- Reduce unnecessary event volume.
- Split the entity into independently orderable sub-entities.
- Partition by a more granular key if the business allows it.
- Use a dedicated topic or processing path for particularly hot entities.
- Redesign the workload so that global per-entity sequencing is not required for every operation.

But don't split a key just to increase throughput if doing so breaks required ordering.

## 17.4 Very important: Increasing partition count can change key placement

Suppose initially:

```
Number of partitions = 8

order-101 -> Partition 2
```

After increasing the count:

```
Number of partitions = 16

order-101 -> Partition 10
```

The existing events remain in partition 2.

New events might go to partition 10.

Now the same order has events in two different partitions.

Because there is no ordering across those partitions, we may violate business ordering.

Therefore:

Changing the partition count is a migration and ordering decision, not just a capacity adjustment.

Plan it carefully.

For strict per-entity ordering, possibilities include:

- Creating a new versioned topic and migrating producers and consumers in a controlled way.
- Draining old partitions before switching.
- Using a stable custom partitioning scheme and migration strategy.
- Using an application-level sequence number and database gate.

# 18. Kafka consumer groups: Another important architectural benefit

Remember that different consumer groups maintain independent offsets.

This makes Kafka useful beyond task processing.

Imagine:

```
                    order-events
                         |
          +--------------+--------------+
          |              |              |
          v              v              v

      Order Group    Analytics Group  Fraud Group
          |              |              |
          v              v              v
      Update DB      Data Warehouse  Fraud Detection
```

All three groups receive the full event stream independently.

Suppose Analytics is down for four hours.

The Order Service can continue processing.

When Analytics returns, it resumes from its committed offsets, provided the required records are still retained.

You can also deliberately reset consumer offsets to replay records.

This is a very different model from treating a queue as a single collection of pending messages to be deleted after consumption.

Kafka is particularly useful when:

- Multiple independent services need the same events.
- New consumers may be introduced later.
- You need historical replay.
- Analytics and operational processing must consume the same source.
- You want event-driven integration across multiple bounded contexts.

# 19. The strongest architecture for your long-running task scenario

Now let's answer a particularly important architectural question.

We originally designed:

```
API
  |
  v
DB
  |
  v
SQS
  |
  v
Worker
  |
  v
DB
```

Should we do the exact same thing with Kafka?

We can, but there is a better way for long-running jobs when ordering, retries and task-level visibility are important.

I would consider two architectural models.

## Model A: Kafka consumer directly executes the task

\#chatgpt-mermaid-\_r_8c0\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_8c0\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_8c0\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_8c0\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_8c0\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_8c0\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_8c0\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_8c0\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_8c0\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_8c0\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_8c0\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_8c0\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_8c0\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_8c0\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_8c0\_ p{margin:0;}#chatgpt-mermaid-\_r_8c0\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_8c0\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_8c0\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_8c0\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_8c0\_ .label text,#chatgpt-mermaid-\_r_8c0\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_8c0\_ .node rect,#chatgpt-mermaid-\_r_8c0\_ .node circle,#chatgpt-mermaid-\_r_8c0\_ .node ellipse,#chatgpt-mermaid-\_r_8c0\_ .node polygon,#chatgpt-mermaid-\_r_8c0\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_8c0\_ .rough-node .label text,#chatgpt-mermaid-\_r_8c0\_ .node .label text,#chatgpt-mermaid-\_r_8c0\_ .image-shape .label,#chatgpt-mermaid-\_r_8c0\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_8c0\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_8c0\_ .rough-node .label,#chatgpt-mermaid-\_r_8c0\_ .node .label,#chatgpt-mermaid-\_r_8c0\_ .image-shape .label,#chatgpt-mermaid-\_r_8c0\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_8c0\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_8c0\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_8c0\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_8c0\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_8c0\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_8c0\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_8c0\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_8c0\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_8c0\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_8c0\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_8c0\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_8c0\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_8c0\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_8c0\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_8c0\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_8c0\_ .icon-shape,#chatgpt-mermaid-\_r_8c0\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_8c0\_ .icon-shape p,#chatgpt-mermaid-\_r_8c0\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_8c0\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_8c0\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_8c0\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_8c0\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_8c0\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_8c0\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_8c0\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_8c0\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_8c0\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_8c0\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_8c0\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_8c0\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_8c0\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_8c0\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_8c0\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_8c0\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_8c0\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_8c0\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_8c0\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_8c0\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_8c0\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_8c0\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_8c0\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_8c0\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_8c0\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_8c0\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_8c0\_ .node rect,#chatgpt-mermaid-\_r_8c0\_ .node circle,#chatgpt-mermaid-\_r_8c0\_ .node ellipse,#chatgpt-mermaid-\_r_8c0\_ .node polygon,#chatgpt-mermaid-\_r_8c0\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_8c0\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_8c0\_ .mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_8c0\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_8c0\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_8c0\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}Task Creation APIDatabaseOutboxKafka PublisherKafka TopicKafka ConsumerTask Claim and DB LeaseBusiness ProcessingPersist ResultCommit Kafka Offset

Here:

- Kafka consumer receives the task.
- It claims it in the database.
- It performs the task.
- It updates the database.
- It commits the offset.

This is suitable when:

- Processing is relatively short.
- Per-partition sequencing is straightforward.
- You can maintain polling while tasks execute.
- Rebalance handling is implemented carefully.
- You don't need a separate executor with independent scheduling.

For strict ordering, process sequentially within a partition.

This is conceptually simple, but the consumer's lifecycle and business execution lifecycle are tightly coupled.

## Model B: Kafka ingestion + database-backed task execution

This is the architecture I would lean toward for long-running, retry-heavy background tasks.

The main idea is to separate two responsibilities:

1. Consume events reliably from Kafka.
2. Execute business tasks reliably using a database-backed worker system.

Kafka becomes the ordered event-delivery and distribution layer.

The database becomes the execution ledger.

Let's build it.

### Step 1: API creates the task

```
tasks:

task-101
status = QUEUED
order_id = order-101
sequence_number = 1
```

Outbox event:

```
event-101
task_id = task-101
status = PENDING
```

### Step 2: Publish to Kafka

The publisher sends the event using:

```
Kafka key = order-101
```

All events for order 101 go to the same partition.

### Step 3: Kafka ingestion consumer receives the event

This consumer does not perform the expensive business processing.

Its responsibility is to durably register the event/task.

It executes one short database transaction:

```
BEGIN

1. Insert event ID into consumer_inbox.

2. Validate or update the task record.

3. Record its Kafka origin.

COMMIT
```

### Step 4: Commit the Kafka offset

Only after the database transaction commits.

Now the consumer can move on to the next event.

If the consumer crashes before committing, Kafka replays the event, and the inbox deduplication mechanism prevents duplicate registration.

### Step 5: Independent task workers execute tasks

We now have a separate pool:

```
Database
    |
    v
Task Scheduler / Worker Coordinator
    |
    +----------+----------+
    |          |          |
    v          v          v
 Worker 1   Worker 2   Worker 3
```

These workers claim tasks using database leases.

They perform:

- Task ownership.
- Lease renewal.
- Heartbeats.
- Fencing.
- Checkpointing.
- Retry management.
- Task completion.
- Failure handling.

The execution no longer depends on a Kafka consumer staying active for 30 minutes or several hours.

This eliminates the tight coupling between long-running business execution and Kafka's `max.poll.interval.ms`.

### Step 6: Preserve ordering using a sequence gate

This is critical.

Although the Kafka ingestion layer registers events in partition order, the independent workers might execute them in another order.

So maintain an entity-level sequence.

For example:

```
order-101

Event 1:
    ORDER_CREATED

Event 2:
    INVENTORY_RESERVED

Event 3:
    PAYMENT_AUTHORIZED

Event 4:
    ORDER_CONFIRMED
```

Database:

```
aggregate_processing_state

aggregate_id = order-101

last_completed_sequence = 1
```

A worker can claim sequence 2.

But sequence 3 must not be claimed until sequence 2 is completed.

Conceptually:

```
UPDATE tasks t
SET
    status = 'RUNNING',
    lease_owner = :worker_id,
    lease_token = lease_token + 1,
    lease_expires_at =
        clock_timestamp() + INTERVAL '90 seconds'

WHERE t.id = :task_id

AND t.status = 'QUEUED'

AND EXISTS (
    SELECT 1
    FROM aggregate_processing_state a
    WHERE a.aggregate_id = t.aggregate_id
    AND t.sequence_number = a.last_completed_sequence + 1
);
```

In production, this should be implemented in a short transaction with proper row locking on the aggregate state, lease assignment and fencing token management.

When the task completes, update:

```
last_completed_sequence = 2
```

The next event becomes eligible.

The result:

```
Order 101:
    Sequence 1 -> Completed
    Sequence 2 -> Running
    Sequence 3 -> Waiting

Order 102:
    Sequence 1 -> Running

Order 103:
    Sequence 1 -> Running
```

Order 102 and 103 can continue even when order 101 is waiting for a retry.

This gives you:

- Ordered processing within one business entity.
- Parallel processing across different entities.
- Independent long-running task workers.
- Database-controlled retries.
- Strong recovery capabilities.
- Kafka consumer groups that do not need to hold open long-running business operations.

### Complete architecture

\#chatgpt-mermaid-\_r_8dg\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_8dg\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_8dg\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_8dg\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_8dg\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_8dg\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_8dg\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_8dg\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_8dg\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_8dg\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_8dg\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_8dg\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_8dg\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_8dg\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_8dg\_ p{margin:0;}#chatgpt-mermaid-\_r_8dg\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_8dg\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_8dg\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_8dg\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_8dg\_ .label text,#chatgpt-mermaid-\_r_8dg\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_8dg\_ .node rect,#chatgpt-mermaid-\_r_8dg\_ .node circle,#chatgpt-mermaid-\_r_8dg\_ .node ellipse,#chatgpt-mermaid-\_r_8dg\_ .node polygon,#chatgpt-mermaid-\_r_8dg\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_8dg\_ .rough-node .label text,#chatgpt-mermaid-\_r_8dg\_ .node .label text,#chatgpt-mermaid-\_r_8dg\_ .image-shape .label,#chatgpt-mermaid-\_r_8dg\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_8dg\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_8dg\_ .rough-node .label,#chatgpt-mermaid-\_r_8dg\_ .node .label,#chatgpt-mermaid-\_r_8dg\_ .image-shape .label,#chatgpt-mermaid-\_r_8dg\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_8dg\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_8dg\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_8dg\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_8dg\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_8dg\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_8dg\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_8dg\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_8dg\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_8dg\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_8dg\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_8dg\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_8dg\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_8dg\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_8dg\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_8dg\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_8dg\_ .icon-shape,#chatgpt-mermaid-\_r_8dg\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_8dg\_ .icon-shape p,#chatgpt-mermaid-\_r_8dg\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_8dg\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_8dg\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_8dg\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_8dg\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_8dg\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_8dg\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_8dg\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_8dg\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_8dg\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_8dg\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_8dg\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_8dg\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_8dg\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_8dg\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_8dg\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_8dg\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_8dg\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_8dg\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_8dg\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_8dg\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_8dg\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_8dg\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_8dg\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_8dg\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_8dg\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_8dg\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_8dg\_ .node rect,#chatgpt-mermaid-\_r_8dg\_ .node circle,#chatgpt-mermaid-\_r_8dg\_ .node ellipse,#chatgpt-mermaid-\_r_8dg\_ .node polygon,#chatgpt-mermaid-\_r_8dg\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_8dg\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_8dg\_ .mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_8dg\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_8dg\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_8dg\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}ClientTask Creation APIPostgreSQLTasksOutboxKafka PublisherKafka TopicKafka Ingestion Consumer 1Kafka Ingestion Consumer 2Consumer InboxTask CoordinatorExecution Worker 1Execution Worker 2Execution Worker NExternal APIs / S3 / ServicesRetry SchedulerLease Recovery Scheduler

One important detail: Kafka and the database must agree on the stage they are acknowledging.

In this architecture, the Kafka consumer acknowledges durable admission of the event into the task system, not completion of the actual business operation.

That distinction is what makes the separation work.

It also means that the task database needs its own independent monitoring and recovery. Kafka retention is not a substitute for a durable execution ledger.

# 20. Direct Kafka processing versus decoupled execution

Let's compare the two approaches.

| Concern                            | Direct Kafka worker                        | Kafka + DB-backed executor     |
| ---------------------------------- | ------------------------------------------ | ------------------------------ |
| Kafka consumer executes task       | Yes                                        | No                             |
| Long-running processing            | More complex                               | Easier                         |
| Offset commit                      | After task completion                      | After durable task admission   |
| `max.poll.interval.ms` sensitivity | High                                       | Lower                          |
| Task-level lease                   | Useful                                     | Essential                      |
| Retry management                   | Kafka offset/retry topics                  | DB scheduler                   |
| Per-entity ordering                | Partition order, plus sequencing if needed | Explicit DB sequence gate      |
| Worker scaling                     | Limited by partitions                      | Independent execution pool     |
| Task visibility and history        | Requires DB                                | Naturally DB-centric           |
| Rebalance impact                   | Can interrupt business work                | Mainly affects ingestion       |
| Operational complexity             | Lower initially                            | Higher, but clearer separation |

Neither is universally better.

For short event handlers, direct Kafka consumption is often the simplest.

For hours-long tasks with retries, checkpoints, external operations and detailed task status, the decoupled approach is usually easier to operate reliably.

# 21. Important Kafka concepts that affect production design

There are a few more concepts worth understanding before comparing Kafka and SQS.

## 21.1 Kafka is a log, not a traditional work queue

Consider:

```
Topic: video-processing

Offset 100: Process video A
Offset 101: Process video B
Offset 102: Process video C
```

Worker Group A processes all three records.

After completion, its committed offset becomes 103.

The records themselves still exist in Kafka.

Now another consumer group starts:

```
Worker Group B:

Starts from offset 100.
Processes A, B and C independently.
```

This is fundamentally different from SQS:

```
SQS:

Worker 1 receives message A.
Worker 1 processes A.
Worker 1 deletes A.

Message A is no longer available
for normal consumption by another consumer.
```

Kafka's retention and replay model is especially valuable for event-driven systems.

## 21.2 Consumer lag

Kafka gives us an excellent way to understand how far behind our consumers are.

Imagine:

```
Partition 0:

Latest offset:
    10,000

Consumer committed offset:
    9,000
```

Consumer lag is approximately:

\\[ Lag = LatestOffset - CommittedOffset \\]

Here:

```
Consumer lag = 1,000 records
```

This means the consumer has approximately 1,000 records remaining to catch up, depending on the log's offsets and end position.

Monitoring lag is extremely important.

For example:

```
Producer:
    1,000 records/second

Consumer:
    700 records/second

Backlog growth:
    300 records/second
```

Eventually, the consumer will become significantly behind.

You can scale consumers, increase partition count where appropriate, optimize processing or apply backpressure to producers.

In SQS, the equivalent operational indicators include queue depth and age of the oldest message.

## 21.3 Multiple consumer groups versus multiple workers

This distinction is worth remembering.

Suppose you have:

```
Topic: order-events

Partitions: 8
```

Within one consumer group:

```
Group: order-service

Worker 1
Worker 2
Worker 3
```

The three workers share the eight partitions.

Each record is normally processed by one active member of this group.

Now introduce another consumer group:

```
Group: analytics-service

Worker 1
Worker 2
```

This second group independently consumes the same eight partitions.

Therefore:

```
Same consumer group:
    Distributes the work.

Different consumer groups:
    Each gets its own independent stream position
    and can consume the same events.
```

This distinction is crucial in microservice architectures.

## 21.4 What is consumer rebalance?

Rebalancing means redistributing partition ownership among consumers in a group.

It can happen when:

- A consumer instance starts.
- A consumer instance stops.
- A consumer becomes unavailable.
- A topic gains partitions.
- A consumer group subscription changes.
- Group membership or assignment changes.

For example:

Before:

```
P0 -> W1
P1 -> W2
P2 -> W3
P3 -> W4
```

Worker 4 dies.

After rebalance:

```
P0 -> W1
P1 -> W2
P2 -> W3
P3 -> W1
```

Rebalances can interrupt processing and affect throughput.

Kafka 4.0 introduced a generally available next-generation consumer rebalance protocol with incremental assignment, reducing dependence on a global synchronization barrier. The exact protocol and configuration depend on broker and client versions.&#x20;

[image](https://www.google.com/s2/favicons?domain=https://kafka.apache.org\&sz=32)

Apache Kafka



For production:

- Prefer stable consumers and graceful shutdown.
- Avoid repeatedly starting and stopping consumers.
- Handle partition revocation and assignment.
- Avoid excessively long processing in the poll thread.
- Monitor rebalance frequency and duration.
- Use appropriate assignment strategies.

# 22. Producer-side reliability: Don't forget ordering while publishing

We have spent a lot of time discussing consumers.

But ordering can also be affected at the producer side.

Suppose two producers send:

```
Producer A:
    ORDER_CREATED

Producer B:
    PAYMENT_AUTHORIZED
```

Both use:

```
key = order-101
```

Kafka sends both records to the same partition.

However, because they are independent producers, Kafka can only preserve the order in which the records are appended to the partition.

It cannot infer which one should logically come first.

If `PAYMENT_AUTHORIZED` arrives first, that is the order consumers will observe.

For strict business ordering, use:

- A clear event-producing authority.
- Controlled sequencing.
- An aggregate version or sequence number.
- State-transition validation in consumers.

Also configure a reliable Kafka producer:

```
acks=all
enable.idempotence=true
retries=2147483647
```

The exact timeout and batching configuration should be chosen for your latency and throughput requirements.

A reliable producer configuration helps with transient broker failures, duplicate writes caused by producer retries and ordering during retries.

But it does not provide cross-service business sequencing automatically.&#x20;

[image](https://www.google.com/s2/favicons?domain=https://kafka.apache.org\&sz=32)

Apache Kafka+2



## 22.1 The Outbox Publisher

We should retain the transactional outbox pattern.

\#chatgpt-mermaid-\_r_8jp\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_8jp\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_8jp\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_8jp\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_8jp\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_8jp\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_8jp\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_8jp\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_8jp\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_8jp\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_8jp\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_8jp\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_8jp\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_8jp\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_8jp\_ p{margin:0;}#chatgpt-mermaid-\_r_8jp\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_8jp\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_8jp\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_8jp\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_8jp\_ .label text,#chatgpt-mermaid-\_r_8jp\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_8jp\_ .node rect,#chatgpt-mermaid-\_r_8jp\_ .node circle,#chatgpt-mermaid-\_r_8jp\_ .node ellipse,#chatgpt-mermaid-\_r_8jp\_ .node polygon,#chatgpt-mermaid-\_r_8jp\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_8jp\_ .rough-node .label text,#chatgpt-mermaid-\_r_8jp\_ .node .label text,#chatgpt-mermaid-\_r_8jp\_ .image-shape .label,#chatgpt-mermaid-\_r_8jp\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_8jp\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_8jp\_ .rough-node .label,#chatgpt-mermaid-\_r_8jp\_ .node .label,#chatgpt-mermaid-\_r_8jp\_ .image-shape .label,#chatgpt-mermaid-\_r_8jp\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_8jp\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_8jp\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_8jp\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_8jp\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_8jp\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_8jp\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_8jp\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_8jp\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_8jp\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_8jp\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_8jp\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_8jp\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_8jp\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_8jp\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_8jp\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_8jp\_ .icon-shape,#chatgpt-mermaid-\_r_8jp\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_8jp\_ .icon-shape p,#chatgpt-mermaid-\_r_8jp\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_8jp\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_8jp\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_8jp\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_8jp\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_8jp\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_8jp\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_8jp\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_8jp\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_8jp\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_8jp\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_8jp\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_8jp\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_8jp\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_8jp\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_8jp\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_8jp\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_8jp\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_8jp\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_8jp\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_8jp\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_8jp\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_8jp\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_8jp\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_8jp\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_8jp\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_8jp\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_8jp\_ .node rect,#chatgpt-mermaid-\_r_8jp\_ .node circle,#chatgpt-mermaid-\_r_8jp\_ .node ellipse,#chatgpt-mermaid-\_r_8jp\_ .node polygon,#chatgpt-mermaid-\_r_8jp\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_8jp\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_8jp\_ .mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_8jp\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_8jp\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_8jp\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}APIDatabase TransactionTasks TableOutbox TableOutbox PublisherKafka

The publisher should:

1. Read pending outbox events.
2. Publish the event with its stable ID.
3. Use the correct partition key.
4. Wait for Kafka acknowledgement.
5. Mark the outbox event as published.

Suppose Kafka publication succeeds, but the publisher crashes before updating the outbox.

It publishes again after restarting.

This creates a possible duplicate logical event.

Our consumer inbox and unique event ID protect against this.

This is a very important combination:

```
Database producer:
    Transactional Outbox

Kafka producer:
    Idempotent Producer

Kafka consumer:
    Consumer Inbox

Business processing:
    Idempotent operations
```

Each mechanism addresses a different failure scenario.

# 23. Kafka versus SQS: Complete comparison

Now let's directly answer the important architectural question:

When should we choose Kafka, and when should we choose SQS?

First, remember that SQS has two important queue types:

- Standard.
- FIFO.

We should not compare Kafka only against SQS Standard when ordering matters. SQS FIFO already provides ordered message groups.

## 23.1 Fundamental differences

| Dimension                      | Kafka                                                     | SQS Standard                                    | SQS FIFO                                           |
| ------------------------------ | --------------------------------------------------------- | ----------------------------------------------- | -------------------------------------------------- |
| Fundamental model              | Distributed event log                                     | Managed message queue                           | Managed ordered message queue                      |
| Message storage                | Persistent log with configurable retention                | Messages retained until deleted or expired      | Messages retained until deleted or expired         |
| Consumption                    | Offset-based                                              | Receive and delete                              | Receive and delete                                 |
| Ordering                       | Per partition                                             | Best effort, not guaranteed                     | Strict within message group                        |
| Parallelism                    | Partitions and consumer groups                            | Multiple consumers                              | Multiple message groups                            |
| Concurrency limit              | Number of partitions per consumer group                   | Scales independently of fixed partition count   | Parallelism across message groups                  |
| Duplicate handling             | Idempotent producer, consumer deduplication, transactions | At-least-once delivery; application idempotency | Producer deduplication and application idempotency |
| Acknowledgement                | Commit offset                                             | Delete message                                  | Delete message                                     |
| Retry                          | Offset replay, retry topics, application-managed retry    | Visibility timeout and redelivery               | Visibility timeout and redelivery                  |
| Delay scheduling               | Requires additional mechanism                             | Delay queues/message timers                     | Queue-level delay; no individual message timers    |
| Replay                         | Native while records are retained                         | No normal replay after deletion                 | No normal replay after deletion                    |
| Multiple independent consumers | Native consumer groups                                    | Fan-out using SNS or separate queues            | Fan-out using SNS or separate queues               |
| Long-running work              | Needs careful poll and rebalance management               | Visibility extension, up to 12 hours            | Visibility extension, up to 12 hours               |
| Infrastructure                 | Kafka cluster or managed Kafka service                    | Fully managed                                   | Fully managed                                      |
| Operational complexity         | Higher                                                    | Lower                                           | Lower to moderate                                  |

A key observation:

You do not need Kafka just because you need ordered messages.

SQS FIFO can give you ordering within a message group while allowing other groups to be processed concurrently.

AWS explicitly documents that messages with the same FIFO `MessageGroupId` are processed sequentially, while different groups can progress independently.&#x20;

[image](https://www.google.com/s2/favicons?domain=https://docs.aws.amazon.com\&sz=32)

Amazon Simple Queue Service+1



## 23.2 Understand ordering: Kafka versus SQS FIFO

Let's use the same business example.

We have:

```
Order 101:

1. ORDER_CREATED
2. INVENTORY_RESERVED
3. PAYMENT_AUTHORIZED
4. ORDER_CONFIRMED
```

### Kafka

Publish all records using:

```
Kafka key = order-101
```

They map to the same partition.

```
Partition 2:

ORDER_CREATED
INVENTORY_RESERVED
PAYMENT_AUTHORIZED
ORDER_CONFIRMED
```

Kafka guarantees their partition log order.

Different orders can be routed to different partitions.

### SQS FIFO

Publish all records using:

```
MessageGroupId = order-101
```

SQS FIFO maintains sequential processing for that message group.

Different orders use different groups:

```
Group: order-101

ORDER_CREATED
INVENTORY_RESERVED
PAYMENT_AUTHORIZED
ORDER_CONFIRMED


Group: order-102

ORDER_CREATED
INVENTORY_RESERVED
PAYMENT_AUTHORIZED
ORDER_CONFIRMED
```

SQS FIFO can process different groups concurrently while maintaining ordering within each group.

So in many applications:

```
Kafka key = orderId

SQS FIFO MessageGroupId = orderId
```

These solve similar per-entity ordering requirements.

However, they have different execution and retention models.

SQS FIFO also supports producer deduplication IDs, with a five-minute deduplication interval. This does not eliminate the need for application-level protection against duplicate business effects.&#x20;

[image](https://www.google.com/s2/favicons?domain=https://docs.aws.amazon.com\&sz=32)

Amazon Simple Queue Service+1



## 23.3 Throughput and scaling

Let's compare how you scale them.

SQS Standard

You create a queue and add consumers.

AWS handles the underlying queue infrastructure and scales the service without requiring you to configure partitions.

This is excellent for independently processable background jobs.

AWS describes Standard queues as supporting at-least-once delivery, high throughput and independent message processing.&#x20;

[image](https://www.google.com/s2/favicons?domain=https://docs.aws.amazon.com\&sz=32)

Amazon Simple Queue Service+1



SQS FIFO

You increase concurrency by distributing workload across many message groups.

For example:

```
Group 1 -> Worker 1
Group 2 -> Worker 2
Group 3 -> Worker 3
Group 4 -> Worker 4
```

But messages within one group remain sequential.

Kafka

You increase parallelism by increasing topic partitions and adding consumer instances.

For example:

```
8 partitions:

W1 -> P0
W2 -> P1
W3 -> P2
W4 -> P3
W5 -> P4
W6 -> P5
W7 -> P6
W8 -> P7
```

If you have 100 consumers and 8 partitions, only 8 can consume partitions concurrently within that traditional consumer group.

Kafka gives you fine control over distributed partitioning and stream parallelism, but it requires planning.

# 24. When should you choose Kafka?

Choose Kafka when your architecture has the following characteristics.

## Scenario 1: Multiple independent services consume the same event

Imagine an order system.

When an order is created:

```
ORDER_CREATED
```

Several services need the event:

\#chatgpt-mermaid-\_r_8nk\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_8nk\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_8nk\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_8nk\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_8nk\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_8nk\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_8nk\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_8nk\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_8nk\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_8nk\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_8nk\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_8nk\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_8nk\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_8nk\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_8nk\_ p{margin:0;}#chatgpt-mermaid-\_r_8nk\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_8nk\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_8nk\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_8nk\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_8nk\_ .label text,#chatgpt-mermaid-\_r_8nk\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_8nk\_ .node rect,#chatgpt-mermaid-\_r_8nk\_ .node circle,#chatgpt-mermaid-\_r_8nk\_ .node ellipse,#chatgpt-mermaid-\_r_8nk\_ .node polygon,#chatgpt-mermaid-\_r_8nk\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_8nk\_ .rough-node .label text,#chatgpt-mermaid-\_r_8nk\_ .node .label text,#chatgpt-mermaid-\_r_8nk\_ .image-shape .label,#chatgpt-mermaid-\_r_8nk\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_8nk\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_8nk\_ .rough-node .label,#chatgpt-mermaid-\_r_8nk\_ .node .label,#chatgpt-mermaid-\_r_8nk\_ .image-shape .label,#chatgpt-mermaid-\_r_8nk\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_8nk\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_8nk\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_8nk\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_8nk\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_8nk\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_8nk\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_8nk\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_8nk\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_8nk\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_8nk\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_8nk\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_8nk\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_8nk\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_8nk\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_8nk\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_8nk\_ .icon-shape,#chatgpt-mermaid-\_r_8nk\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_8nk\_ .icon-shape p,#chatgpt-mermaid-\_r_8nk\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_8nk\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_8nk\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_8nk\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_8nk\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_8nk\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_8nk\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_8nk\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_8nk\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_8nk\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_8nk\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_8nk\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_8nk\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_8nk\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_8nk\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_8nk\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_8nk\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_8nk\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_8nk\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_8nk\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_8nk\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_8nk\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_8nk\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_8nk\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_8nk\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_8nk\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_8nk\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_8nk\_ .node rect,#chatgpt-mermaid-\_r_8nk\_ .node circle,#chatgpt-mermaid-\_r_8nk\_ .node ellipse,#chatgpt-mermaid-\_r_8nk\_ .node polygon,#chatgpt-mermaid-\_r_8nk\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_8nk\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_8nk\_ .mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_8nk\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_8nk\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_8nk\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}Order Service ProducerKafka: order-eventsOrder ManagementInventoryAnalyticsFraud DetectionNotifications

All services have independent consumer groups.

Each can consume at its own pace.

You don't have to publish the same event separately to five unrelated queues just to give each service its own consumption state.

Kafka is a very natural fit here.

For comparison, you can achieve a similar fan-out using SNS and multiple SQS queues, but the retention and replay semantics are different.

## Scenario 2: You need to replay historical events

Suppose you introduce a new Fraud Detection service.

You want to analyze the last three days of order events.

With Kafka, you can create a new consumer group and start from an appropriate retained offset.

You don't need to ask the original producer to publish everything again.

This is one of Kafka's biggest advantages.

## Scenario 3: High-volume event streaming

Imagine:

```
Millions of customer activity events.

Millions of telemetry records.

Continuous transaction events.

Real-time analytics.
```

Kafka's distributed partitioned log architecture is very suitable for high-throughput event pipelines and stream processing.

It can support:

- Real-time analytics.
- Stream transformations.
- Event aggregation.
- Data synchronization.
- Event-driven microservices.
- CDC pipelines.
- Monitoring and telemetry.

## Scenario 4: You need per-entity ordering at scale

For example:

```
Bank account transactions.

Stock inventory updates.

Order state transitions.

User activity streams.

Shipment lifecycle events.
```

Using an entity ID as the partition key provides ordered processing within that entity's partition.

Different entities can be processed in parallel.

This makes Kafka particularly powerful for high-volume ordered event streams.

## Scenario 5: Event sourcing

Suppose the authoritative event history for an order is:

```
ORDER_CREATED
ADDRESS_UPDATED
ITEM_ADDED
PAYMENT_AUTHORIZED
ORDER_CONFIRMED
```

The current state can be derived from the event history.

Kafka can act as an event distribution and retention backbone in such an architecture.

However, Kafka retention and compaction configuration must be designed according to your replay requirements. Kafka is not automatically a replacement for a database designed to answer arbitrary transactional queries.

## Scenario 6: Many downstream data systems need the same stream

For example:

```
Application events
      |
      v
    Kafka
      |
      +------> PostgreSQL projection
      |
      +------> Elasticsearch
      |
      +------> Data warehouse
      |
      +------> Real-time dashboard
      |
      +------> Fraud model
```

Kafka is a good integration backbone here.

# 25. When should you choose SQS?

SQS is particularly attractive for a different kind of workload.

## Scenario 1: Simple background jobs

For example:

```
Generate PDF
Resize image
Send email
Generate report
Process uploaded file
Generate thumbnail
```

Each task is generally independent.

A worker receives a task, processes it, and deletes it.

You don't need persistent event replay or multiple independent consumer groups.

SQS Standard is usually a simpler fit.

## Scenario 2: Long-running individual tasks

Suppose a task takes:

```
5 minutes
20 minutes
1 hour
```

With SQS:

1. Receive the task.
2. Extend visibility timeout periodically.
3. Process the task.
4. Update the database.
5. Delete the message.

This is a very natural model.

There is no consumer-group partition reassignment or offset-frontier management to build around every task.

However, SQS visibility has a 12-hour maximum measured from the original receive request. For work exceeding that boundary, break the work into independently recoverable steps or use an orchestration service.&#x20;

[image](https://www.google.com/s2/favicons?domain=https://docs.aws.amazon.com\&sz=32)

Amazon Simple Queue Service+1



## Scenario 3: Independent, uneven workloads

Imagine:

```
Task A: 2 seconds

Task B: 30 minutes

Task C: 10 seconds

Task D: 2 hours
```

With SQS Standard, multiple workers can independently take available messages.

You don't need to preserve sequential order across a partition.

This is useful for heterogeneous task processing.

With Kafka, partitioning and ordered processing can introduce head-of-line blocking if a slow task holds up subsequent records in the same partition.

## Scenario 4: AWS-native architectures

Suppose your application is primarily:

```
API Gateway
Lambda
S3
DynamoDB
RDS
SQS
EventBridge
```

SQS integrates naturally with these services.

You don't have to operate a Kafka cluster or introduce a managed Kafka platform if you simply need reliable background execution.

This can reduce infrastructure and operational complexity.

## Scenario 5: Controlled retries and DLQ

SQS provides native:

- Visibility timeout.
- Message redelivery.
- Redrive policy.
- Dead Letter Queue.
- Delay queues.
- Long polling.

For relatively simple asynchronous execution, this reduces the amount of custom messaging infrastructure you need to implement.

Remember, SQS Standard is at-least-once and can deliver records out of order, so application idempotency is still essential.&#x20;

[image](https://www.google.com/s2/favicons?domain=https://docs.aws.amazon.com\&sz=32)

Amazon Simple Queue Service+1



# 26. A very important scenario: What if you need ordering but don't need Kafka?

Let's say you are developing a movie ticketing platform.

The system needs to process all booking-related operations for one booking in order.

For example:

```
Booking 101:

BOOKING_CREATED
SEATS_RESERVED
PAYMENT_INITIATED
PAYMENT_CONFIRMED
TICKETS_GENERATED
```

Bookings must be independent.

Booking 101 should not block Booking 102.

What would you choose?

SQS FIFO might be sufficient.

Use:

```
MessageGroupId = bookingId
```

This gives sequential processing for each booking group.

Different bookings can proceed concurrently.

You do not automatically need Kafka's partitioned log, consumer-group management, retention policies and replay capabilities.

But imagine a different requirement:

```
Booking events must be consumed by:

1. Booking service.
2. Payment analytics.
3. Fraud detection.
4. Customer activity service.
5. Data warehouse.

Each service must independently consume,
retain offsets and replay historical events.
```

Now Kafka becomes much more attractive.

The important lesson:

Ordering alone does not justify Kafka. Ordering plus distributed event streaming, independent subscribers, replay and high-volume processing is a much stronger reason to choose Kafka.

# 27. Decision matrix: Kafka or SQS?

Here is the decision matrix I would actually use during architecture design.

| Requirement                                         | My preference                                                 |
| --------------------------------------------------- | ------------------------------------------------------------- |
| Independent background jobs                         | SQS Standard                                                  |
| Independent long-running tasks                      | SQS Standard                                                  |
| Strict per-entity queue ordering                    | SQS FIFO or Kafka                                             |
| Ordered high-volume event streams                   | Kafka                                                         |
| Multiple independent consumers                      | Kafka or SNS + multiple SQS queues                            |
| Historical event replay                             | Kafka                                                         |
| Event sourcing architecture                         | Kafka, usually with a durable event-store strategy            |
| Simple AWS-native asynchronous processing           | SQS                                                           |
| Complex stream processing                           | Kafka                                                         |
| Real-time analytics pipelines                       | Kafka                                                         |
| Worker autoscaling without fixed partition planning | SQS                                                           |
| High-volume partition-based parallelism             | Kafka                                                         |
| Simple delayed retries                              | SQS for short delays; application scheduler for longer delays |
| Sophisticated retry workflows                       | Either, but Kafka needs additional retry infrastructure       |
| Long-running workflows with checkpoints             | DB-backed tasks or Step Functions; Kafka can carry events     |
| Multiple event subscribers at different speeds      | Kafka                                                         |
| Strong ordering within independent entity groups    | Both Kafka and SQS FIFO                                       |

There is also a useful hybrid architecture.

## Kafka + SQS together

You do not need to select only one.

For example:

\#chatgpt-mermaid-\_r_8pf\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_8pf\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_8pf\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_8pf\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_8pf\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_8pf\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_8pf\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_8pf\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_8pf\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_8pf\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_8pf\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_8pf\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_8pf\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_8pf\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_8pf\_ p{margin:0;}#chatgpt-mermaid-\_r_8pf\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_8pf\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_8pf\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_8pf\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_8pf\_ .label text,#chatgpt-mermaid-\_r_8pf\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_8pf\_ .node rect,#chatgpt-mermaid-\_r_8pf\_ .node circle,#chatgpt-mermaid-\_r_8pf\_ .node ellipse,#chatgpt-mermaid-\_r_8pf\_ .node polygon,#chatgpt-mermaid-\_r_8pf\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_8pf\_ .rough-node .label text,#chatgpt-mermaid-\_r_8pf\_ .node .label text,#chatgpt-mermaid-\_r_8pf\_ .image-shape .label,#chatgpt-mermaid-\_r_8pf\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_8pf\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_8pf\_ .rough-node .label,#chatgpt-mermaid-\_r_8pf\_ .node .label,#chatgpt-mermaid-\_r_8pf\_ .image-shape .label,#chatgpt-mermaid-\_r_8pf\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_8pf\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_8pf\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_8pf\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_8pf\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_8pf\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_8pf\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_8pf\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_8pf\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_8pf\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_8pf\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_8pf\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_8pf\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_8pf\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_8pf\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_8pf\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_8pf\_ .icon-shape,#chatgpt-mermaid-\_r_8pf\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_8pf\_ .icon-shape p,#chatgpt-mermaid-\_r_8pf\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_8pf\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_8pf\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_8pf\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_8pf\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_8pf\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_8pf\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_8pf\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_8pf\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_8pf\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_8pf\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_8pf\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_8pf\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_8pf\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_8pf\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_8pf\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_8pf\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_8pf\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_8pf\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_8pf\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_8pf\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_8pf\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_8pf\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_8pf\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_8pf\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_8pf\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_8pf\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_8pf\_ .node rect,#chatgpt-mermaid-\_r_8pf\_ .node circle,#chatgpt-mermaid-\_r_8pf\_ .node ellipse,#chatgpt-mermaid-\_r_8pf\_ .node polygon,#chatgpt-mermaid-\_r_8pf\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_8pf\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_8pf\_ .mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_8pf\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_8pf\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_8pf\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}Order ServiceKafka: order-eventsInventory ConsumerPayment ConsumerAnalytics ConsumerVideo / Document ProcessingConsumerSQS: processing-jobsWorker 1Worker 2Worker 3Task Database

Kafka distributes domain events to independent services.

One service decides that a heavyweight task needs to be executed asynchronously.

It creates a task and publishes it to SQS.

SQS distributes that work to execution workers.

This separation can be very useful:

- Kafka handles event distribution and replay.
- SQS handles individual task delivery.
- PostgreSQL handles task state, retries, ownership and results.

You get the advantages of both without trying to force one technology to handle every responsibility.

# 28. Common mistakes to avoid

These are important in real-world designs and interviews.

| Mistake                                                                | Why it is wrong                                                        |
| ---------------------------------------------------------------------- | ---------------------------------------------------------------------- |
| Assuming Kafka guarantees ordering across the entire topic             | Kafka only guarantees ordering within individual partitions.           |
| Using a random key when per-entity ordering is required                | Events may go to different partitions.                                 |
| Assuming more workers always means higher parallelism                  | Traditional consumer-group parallelism is limited by partition count.  |
| Committing offsets before processing                                   | Can lose business operations.                                          |
| Committing the latest offset after parallel processing                 | Can skip earlier unfinished records.                                   |
| Processing long-running jobs without continuing `poll()`               | Can cause consumer group membership loss.                              |
| Assuming rebalance kills the old processing thread                     | The old worker might continue external operations.                     |
| Assuming Kafka exactly-once guarantees external API calls exactly once | Kafka transactions do not automatically cover external systems.        |
| Sending failed events to retry topics without considering ordering     | Later records might overtake earlier failed records.                   |
| Increasing partitions without considering key remapping                | Can break per-entity ordering during migration.                        |
| Assuming Kafka retention is the same as task completion tracking       | Retained records are not an execution status or task ownership ledger. |
| Assuming SQS FIFO eliminates all business-level duplication            | External side effects still need proper idempotency.                   |
| Choosing Kafka only because it is popular                              | It introduces unnecessary operational complexity for simple jobs.      |

# 29. My final recommended design for different situations

Let's bring this back to actual system-design decisions.

### Case A: Image / video / PDF processing platform

Requirements:

- Long-running tasks.
- Independent processing.
- Retries.
- Multiple worker instances.
- Task progress.
- Task status API.
- Idempotency.
- Failure recovery.

Recommendation: SQS Standard + PostgreSQL task ledger + transactional outbox + database leases.

Use SQS FIFO only when ordering between messages in the same business group is genuinely necessary.

### Case B: Payment transaction event processing

Requirements:

- Account-level ordering.
- Multiple downstream consumers.
- Persistent event history.
- Replay.
- High-volume transaction streams.

Recommendation: Kafka with the account ID as partition key.

Use transactional or database-backed idempotency for business state changes.

### Case C: Order management system

Requirements:

- Per-order ordering.
- Multiple independent services.
- Event-driven architecture.
- Retryable operations.
- Historical replay.
- Analytics and auditing.

Recommendation: Kafka as the domain-event backbone.

For heavyweight tasks, introduce a separate execution mechanism, potentially SQS, or a DB-backed task executor.

### Case D: Email, notifications and simple report generation

Requirements:

- Independent tasks.
- No strict ordering.
- Simple redelivery.
- Worker scaling.
- Retry and DLQ.

Recommendation: SQS Standard.

Kafka would usually be unnecessary unless the surrounding architecture already uses Kafka and has a strong reason to keep the work in its event-streaming ecosystem.

### Case E: Banking ledger or strict account operations

Requirements:

- Sequential operations per account.
- Strong transactional guarantees.
- Protection against duplicate debits.
- High concurrency across accounts.

Recommendation: Kafka keyed by account ID can provide ordered event ingestion and distribution, but the database transaction and account-level concurrency control must enforce the actual financial invariants.

Never rely solely on Kafka partition ordering for financial correctness.

# 30. Interview-ready explanation

Suppose the interviewer asks:

"How would you design a reliable, ordered background-processing system using Kafka?"

I would answer:

"I would first identify the business entity for which ordering is required, such as an order, account or customer. I would use that entity's ID as the Kafka message key so that its events consistently go to the same partition. Kafka guarantees ordering within a partition, while different partitions can be processed concurrently.

I would create a topic with a suitable number of partitions based on expected throughput and parallelism. Multiple worker instances would belong to the same consumer group, and Kafka would distribute partitions among them. Different consumer groups could independently consume the same events.

For reliability, I would disable automatic offset commits and commit offsets only after processing has been durably recorded. This provides at-least-once processing, so I would use event IDs, an inbox table, database constraints and external idempotency keys to prevent duplicate business effects.

For long-running processing, I would not block the Kafka poll loop. I would continue polling and use pause/resume and a separate execution thread while carefully managing the contiguous offset commit position. I would also handle partition revocation and consumer rebalances.

For long-running, retry-heavy business tasks, I would preferably separate Kafka ingestion from task execution. The Kafka consumer would durably register events in the database and commit offsets after that transaction. An independent worker pool would claim tasks through renewable database leases, use fencing tokens, maintain heartbeats, retry failures with exponential backoff and recover abandoned tasks.

For strict per-entity ordering across retries, I would maintain an aggregate sequence number and prevent later operations from executing before earlier ones complete.

Finally, I would use Kafka's producer idempotence and transactions where appropriate, but I would not assume Kafka transactions provide exactly-once effects across PostgreSQL and external APIs."

That's a strong answer because it covers the architecture, ordering, scalability, failures and correctness.

# 31. The most important concepts to remember

Let's finish with one consolidated comparison.

| Concept                                     | SQS                                        | Kafka                                          |
| ------------------------------------------- | ------------------------------------------ | ---------------------------------------------- |
| Unit of ownership                           | Individual message                         | Partition within a consumer group              |
| Ordering boundary                           | FIFO message group                         | Topic partition                                |
| How to establish ordering                   | `MessageGroupId`                           | Message key and partitioner                    |
| Consumer acknowledgement                    | `DeleteMessage`                            | Commit next offset                             |
| How to prevent redelivery during processing | Visibility timeout                         | Consumer-group assignment                      |
| Consumer liveness                           | Consumer process and visibility management | Consumer heartbeats and poll lifecycle         |
| Long-running task protection                | Extend visibility timeout                  | Continue polling; manage rebalances            |
| Business task ownership                     | DB lease                                   | DB lease, when required                        |
| Stale-worker protection                     | Fencing token                              | Fencing token                                  |
| Retry                                       | Redelivery or application scheduling       | Replay, retry topics or application scheduling |
| Delayed retry                               | Delay queue / message timer                | External scheduler or retry-topic mechanism    |
| Historical replay                           | Not native after deletion                  | Native within retention                        |
| Independent subscribers                     | SNS + multiple queues                      | Multiple consumer groups                       |
| Scaling                                     | Add competing consumers                    | Add partitions and consumers                   |

There are three distinctions I especially want you to internalize:

1\. Kafka partition ownership is not the same as task ownership.

Kafka decides which consumer reads a partition. Your application decides which worker is authorized to execute a particular business task.

2\. Kafka offset commits are not the same as SQS message deletion.

An offset commit advances the position of an entire partition. It does not delete one individual message. Therefore, offset management becomes especially important with concurrent processing.

3\. Kafka partition ordering is not the same as guaranteed business ordering.

Kafka preserves the order records enter a partition. Retry topics, multiple producers, partition-count changes and asynchronous execution can still violate your business workflow's intended sequence.

## My overall recommendation

For the background-processing system we've been designing, I would use the following rule:

- SQS: When the primary requirement is to distribute individual tasks reliably among workers, manage retries and process each task independently.
- Kafka: When the primary requirement is to distribute an ordered stream of events to multiple independent consumers, retain those events and support replay and high-throughput parallel stream processing.
- Kafka + DB-backed executor: When you need Kafka's event ordering and distribution, but your actual business jobs are long-running, retry-heavy and require independent task-level ownership.
- Kafka + SQS: When you want Kafka for the event-driven microservice architecture and SQS for heavyweight background execution.

The biggest architectural insight is this:

Don't confuse event delivery with task execution.

Kafka is exceptionally good at distributing durable, ordered event streams. SQS is exceptionally convenient for distributing independent units of background work. PostgreSQL, leases, fencing tokens and idempotency provide the business-level execution guarantees that neither messaging system can automatically give you.

Once you separate these responsibilities, designing reliable asynchronous systems becomes much easier.

### Official documentation worth keeping as references

- [Apache Kafka 4.2 – Introduction and core concepts](https://kafka.apache.org/42/documentation/?utm_source=chatgpt.com).
- [Kafka Java Consumer API – Offsets, poll lifecycle and multithreaded processing](https://kafka.apache.org/42/javadoc/org/apache/kafka/clients/consumer/KafkaConsumer.html?utm_source=chatgpt.com).
- [Kafka Producer Configuration – Acknowledgements and idempotency](https://kafka.apache.org/42/configuration/producer-configs/?utm_source=chatgpt.com).
- [Kafka Design – Message delivery semantics and transactions](https://kafka.apache.org/41/design/design/?source=post_page-----dcb8a544efd1-----------------------------------------\&utm_source=chatgpt.com).
- [Amazon SQS FIFO – Ordering and message groups](https://docs.aws.amazon.com/en_en/AWSSimpleQueueService/latest/SQSDeveloperGuide/FIFO-queues-understanding-logic.html?utm_source=chatgpt.com).
- [Amazon SQS Standard – Delivery semantics and use cases](https://docs.aws.amazon.com/en_gb/AWSSimpleQueueService/latest/SQSDeveloperGuide/standard-queues.html?utm_source=chatgpt.com).
