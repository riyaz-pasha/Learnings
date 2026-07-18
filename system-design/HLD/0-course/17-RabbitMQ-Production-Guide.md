# RabbitMQ — Production Deep-Dive Guide

Companion to `17-Distributed-Messaging-Queue-FAANG-Guide.md`. That guide covers the general theory; this one covers **RabbitMQ specifically**: exchanges/routing, how to fake "partitioning" (RabbitMQ has none natively), reliable delivery, and multi-region deployment.

**The one thing to internalize before anything else:** Kafka partitions a *topic*; RabbitMQ routes messages through an **exchange** into one or more **queues** based on a **routing key** — there is no built-in concept of a partition. Every "how do I shard/parallelize" question in RabbitMQ is really "how do I route across multiple queues," which is a routing-topology problem, not a config flag.

---

## 1. Architecture Recap

```mermaid
flowchart LR
    Pub[Publisher] -->|routing key| EX{{Exchange}}
    EX -->|binding: orders.us.*| Q1[(Queue: us-orders)]
    EX -->|binding: orders.eu.*| Q2[(Queue: eu-orders)]
    EX -->|binding: orders.#| Q3[(Queue: audit-log)]
    Q1 --> C1[Consumer]
    Q2 --> C2[Consumer]
    Q3 --> C3[Consumer]
```

| Concept | What it means |
|---|---|
| **Virtual host (vhost)** | A namespace inside a broker — separate exchanges/queues/permissions per app or environment |
| **Exchange** | Receives published messages, routes them to queues based on type + routing key. **No storage** — pure routing logic. |
| **Queue** | Where messages actually sit until consumed. This is RabbitMQ's unit of storage and parallelism. |
| **Binding** | The rule connecting an exchange to a queue (with a routing key pattern, for topic exchanges) |
| **Routing key** | A string the publisher attaches to a message (e.g., `"orders.us.created"`) — how it's matched depends on exchange type |

---

## 2. Quick Local Broker

```yaml
# docker-compose.yml
services:
  rabbitmq:
    image: rabbitmq:3.13-management
    ports:
      - "5672:5672"   # AMQP
      - "15672:15672" # management UI
```

```bash
docker compose up -d
# UI at http://localhost:15672 (guest/guest, local only)
```

---

## 3. Exchange Types — Pick the Right One

```mermaid
flowchart TD
    Q1{Does every consumer need<br/>every message, no filtering?} -->|Yes| FANOUT["fanout exchange
    ignores routing key,
    broadcasts to all bound queues"]
    Q1 -->|No, needs filtering| Q2{Is the filter an exact match<br/>on one routing key?}
    Q2 -->|Yes| DIRECT["direct exchange
    exact routing-key match"]
    Q2 -->|No, need wildcard/hierarchical match| Q3{Filtering on the routing<br/>key string itself?}
    Q3 -->|Yes| TOPIC["topic exchange
    wildcard patterns: * and #"]
    Q3 -->|No, filtering on message metadata| HEADERS["headers exchange
    matches on header key/values,
    ignores routing key entirely"]
```

| Exchange type | Routing logic | Example use case |
|---|---|---|
| **direct** | Exact routing-key match to binding key | `"payment.failed"` → only the queue bound with that exact key |
| **fanout** | Broadcast to every bound queue, key ignored | Cache-invalidation event that every service instance must see |
| **topic** | Wildcard match: `*` = exactly one word, `#` = zero or more words | `"orders.*.created"` matches `"orders.us.created"` but not `"orders.us.eu.created"`; `"orders.#"` matches any depth |
| **headers** | Matches on message header key/value pairs, not the routing key | Routing by multiple independent attributes (e.g., `region=us AND priority=high`) where a single string key is awkward |

**Declaring exchange, queue, and binding (Python / pika):**
```python
import pika

connection = pika.BlockingConnection(pika.ConnectionParameters("localhost"))
channel = connection.channel()

channel.exchange_declare(exchange="orders", exchange_type="topic", durable=True)
channel.queue_declare(queue="us-orders", durable=True, arguments={"x-queue-type": "quorum"})
channel.queue_bind(queue="us-orders", exchange="orders", routing_key="orders.us.*")

channel.basic_publish(
    exchange="orders",
    routing_key="orders.us.created",
    body=order_json,
    properties=pika.BasicProperties(delivery_mode=2)  # persistent
)
```

**CLI equivalent (rabbitmqadmin):**
```bash
rabbitmqadmin declare exchange name=orders type=topic durable=true
rabbitmqadmin declare queue name=us-orders durable=true arguments='{"x-queue-type":"quorum"}'
rabbitmqadmin declare binding source=orders destination=us-orders routing_key="orders.us.*"
```

**Memory hook:** *direct = exact match, like a mail slot with one label; fanout = a megaphone, everyone hears it; topic = a mail slot that accepts a pattern of labels; headers = sorting mail by the stamps on it, not the address.*

---

## 4. "Sharding" in RabbitMQ — There's No Partition Key, So Fake One

RabbitMQ has no native equivalent to a Kafka partition. If you need to parallelize processing of a logical stream across multiple consumers while preserving some ordering, you build it out of routing:

### Option A — Consistent Hash Exchange plugin (closest thing to Kafka partitioning)

```bash
rabbitmq-plugins enable rabbitmq_consistent_hash_exchange
```

```python
channel.exchange_declare(exchange="orders-hashed", exchange_type="x-consistent-hash", durable=True)

# Bind 8 queues, each queue gets 10 "hash buckets" (weight) — controls its share of the ring
for i in range(8):
    channel.queue_declare(queue=f"orders-shard-{i}", durable=True, arguments={"x-queue-type": "quorum"})
    channel.queue_bind(queue=f"orders-shard-{i}", exchange="orders-hashed", routing_key="10")

# Publish with the field you want to shard/order by AS the routing key
channel.basic_publish(exchange="orders-hashed", routing_key=order.customer_id, body=order_json)
```

```mermaid
flowchart LR
    Pub[Publisher] -->|routing_key = customer_id| EX{{x-consistent-hash exchange}}
    EX -->|hash bucket| Q0[(shard-0)]
    EX -->|hash bucket| Q1[(shard-1)]
    EX -->|hash bucket| Q2[(shard-2)]
    Q0 --> C0[Consumer 0]
    Q1 --> C1[Consumer 1]
    Q2 --> C2[Consumer 2]
```

This gives you: same `customer_id` always hashes to the same queue (per-customer order preserved), and N queues to parallelize across — functionally the same shape as Kafka partitions, hand-built from RabbitMQ primitives.

### Option B — application-level sharding (no plugin)

Publish directly to a routing key like `orders.shard-{hash(customer_id) % N}`, with N pre-declared queues bound to each exact key via a **direct** exchange. Simpler to reason about, less flexible than the consistent-hash plugin if N needs to change later (same repartitioning pain as Kafka — plan capacity up front).

**Golden rule:** *if a design conversation calls for "partitions" in RabbitMQ, the honest answer is "RabbitMQ doesn't have that primitive — here's how I'd build the equivalent with N queues and a hash-based routing key," not silence or a wrong claim that queues auto-shard.*

---

## 5. Reliable Delivery — Publisher Confirms & Consumer Acks

Unreliable-by-default is the single biggest way naive RabbitMQ usage loses messages. Two independent mechanisms, both required for true at-least-once:

```mermaid
sequenceDiagram
    participant Pub as Publisher
    participant Broker
    participant Consumer

    rect rgb(40,60,40)
    Note over Pub,Broker: Publisher confirms — did the broker actually have it?
    Pub->>Broker: publish (persistent message)
    Broker-->>Pub: ack (message safely in the queue / on disk)
    end

    rect rgb(40,40,80)
    Note over Broker,Consumer: Consumer acks — did the consumer actually finish it?
    Broker->>Consumer: deliver message
    Consumer->>Consumer: process
    Consumer->>Broker: basic.ack
    Note over Broker: only now is the message removed
    end
```

```python
# Publisher confirms
channel.confirm_delivery()
if not channel.basic_publish(exchange="orders", routing_key="orders.us.created",
                              body=order_json,
                              properties=pika.BasicProperties(delivery_mode=2)):
    raise Exception("Broker did not confirm the publish — retry or alert")

# Consumer: manual ack, not auto-ack
def callback(ch, method, properties, body):
    process(body)                              # must be idempotent
    ch.basic_ack(delivery_tag=method.delivery_tag)

channel.basic_qos(prefetch_count=20)            # see prefetch tuning below
channel.basic_consume(queue="us-orders", on_message_callback=callback, auto_ack=False)
```

### Prefetch (QoS) — the throughput/fairness dial

`prefetch_count` caps how many unacked messages the broker will push to a consumer at once.

| `prefetch_count` | Effect |
|---|---|
| `0` (unlimited) | **Danger** — one greedy/slow consumer can be handed the entire queue while siblings sit idle |
| `1` | Maximum fairness across consumers, but a round trip per message caps throughput |
| `20–100` (typical) | Balances throughput (batched delivery) against fairness (no single consumer hoards the queue) |

**Memory hook:** *publisher confirms answer "did the broker get it"; consumer acks answer "did the consumer finish it." You need both — confirming the publish says nothing about whether processing later crashed.*

---

## 6. Queue Types — Pick Quorum Queues for Production

| Type | Replication | Status | Use when |
|---|---|---|---|
| **Classic queue** | None (single node) unless using deprecated classic mirroring | Simple, fast, not HA by default | Non-critical, single-node dev/test only |
| **Classic mirrored queue** | Mirrors to other nodes | **Deprecated** — known split-brain and data-loss edge cases during network partitions | Avoid in new designs |
| **Quorum queue** | Raft-based replication across a cluster | **Recommended default for production** | Anything that needs durability + HA |
| **Stream** | Kafka-like append-only log, replayable | Newer feature | When you need replay/multiple independent consumers reading the same data, closer to Kafka's model |

```python
channel.queue_declare(queue="orders", durable=True, arguments={"x-queue-type": "quorum"})
```

```mermaid
graph TD
    subgraph "Quorum Queue (Raft group across 3 nodes)"
    L[Node 1 — Raft leader] --> F1[Node 2 — follower]
    L --> F2[Node 3 — follower]
    end
```

**Golden rule:** *if you're designing a new RabbitMQ system today and the interviewer doesn't specify, say "quorum queues" — classic mirrored queues are a legacy trap that even RabbitMQ's own docs steer people away from.*

---

## 7. Dead-Lettering & TTL

```python
channel.queue_declare(
    queue="orders",
    durable=True,
    arguments={
        "x-queue-type": "quorum",
        "x-dead-letter-exchange": "orders-dlx",   # where rejected/expired messages go
        "x-message-ttl": 86400000,                 # 1 day, in ms
        "x-max-length": 1000000,                   # cap queue depth — avoid unbounded growth
    }
)
```

A message is dead-lettered when: it's `basic.nack`'d / rejected with `requeue=false`, its TTL expires, or the queue hits `x-max-length`. **Without a max retry count enforced at the application layer, a `requeue=true` nack loop is an infinite redelivery loop** — always pair rejection with either `requeue=false` (straight to DLQ) or an app-side attempt counter that eventually stops requeuing.

---

## 8. Multi-Region Deployment

### Do NOT cluster across regions

RabbitMQ clustering (the built-in multi-node cluster mechanism) assumes a **low-latency, reliable LAN** between nodes — it uses Erlang distribution under the hood, which does not tolerate WAN-grade latency or partitions well. Clustering brokers across regions is the same trap as stretching a Kafka cluster across a WAN, but RabbitMQ is *more* fragile about it — expect partition-handling chaos (`pause_minority`/`autoheal` policies fighting each other), not just slow writes.

### The real answer: Federation or Shovel between independent per-region clusters

```mermaid
flowchart LR
    subgraph "Region A (own cluster)"
    EA{{Exchange A}}
    end
    subgraph "Region B (own cluster)"
    EB{{Exchange B}}
    QB[(Queue B)]
    end
    EA -->|Federation link<br/>async, reconnects on its own| EB
    EB --> QB
```

| Mechanism | Model | Use when |
|---|---|---|
| **Federation** | Loosely-coupled link between exchanges/queues in independent brokers; survives disconnects and catches up | Geo-distributed setups where each region has its own broker and you want selective, resilient replication of specific exchanges/queues |
| **Shovel** | A configured "worker" that reliably moves messages from a source queue to a destination queue/exchange, like a dedicated consumer+producer pair | Simpler point-to-point transfer, e.g., forwarding everything from a regional queue into a central aggregation queue |

```bash
# Federation: declare an upstream (Region A) from Region B's broker
rabbitmqctl set_parameter federation-upstream region-a \
  '{"uri":"amqp://user:pass@region-a-broker","expires":3600000}'

rabbitmqctl set_policy --apply-to exchanges federate-orders "^orders$" \
  '{"federation-upstream-set":"all"}'
```

**Golden rule:** *"cluster" is a same-datacenter HA mechanism; "federation/shovel" is the cross-region mechanism. Confusing the two is the single most common RabbitMQ multi-region mistake.*

---

## 9. Monitoring & Operations

| Signal | Why it matters | Where |
|---|---|---|
| Queue depth / consumer utilization | Rising depth with low utilization = consumers can't keep up | Management UI, `rabbitmq_prometheus` plugin |
| Memory/disk alarms | RabbitMQ **blocks publishers** (flow control) when it hits configured memory/disk watermarks — a silent-looking stall, not a crash | `rabbitmqctl status`, alert on `mem_alarm`/`disk_alarm` events |
| Unacked message count | Growing unacked count = consumers pulling messages but not acking — often a `prefetch_count` set too high combined with a slow/stuck consumer | Management UI per-queue view |
| Connection/channel churn | Frequent reconnects usually means client-side connection-per-message anti-pattern instead of a long-lived connection with multiple channels | Broker connection metrics |

---

## 10. Common Mistakes to Avoid

| Mistake | Why it hurts | Fix |
|---|---|---|
| **No publisher confirms** | A publish can be lost between client and broker with no signal to the application | `channel.confirm_delivery()` + check the return value before considering a message "sent" |
| **Auto-ack consumers (`auto_ack=True`)** | Broker deletes the message the instant it's delivered — a consumer crash mid-processing loses it silently | Manual ack after processing completes, matching the "never delete on read" rule from the main guide |
| **Classic mirrored queues in production** | Deprecated; known data-loss/split-brain behavior under network partitions | Use quorum queues |
| **`prefetch_count=0` (unlimited)** | One slow or stuck consumer can be handed the whole queue while others starve | Set an explicit, tuned prefetch count (start around 20–100, measure) |
| **Assuming a single queue "auto-shards"** | RabbitMQ queues are not partitioned — one queue is one ordered stream regardless of how many consumers you attach | Use the Consistent Hash Exchange plugin or application-level routing-key sharding for real parallelism |
| **Clustering across regions/WAN** | Erlang distribution + RabbitMQ's partition-handling policies are not designed for WAN latency/instability — expect split-brain and node ejection storms | Independent per-region clusters + Federation or Shovel |
| **No TTL / no `x-max-length`** | An unbounded queue eventually exhausts broker memory/disk, triggering flow control that silently blocks every publisher | Always set a retention bound appropriate to the business need |
| **`nack` with `requeue=true` and no attempt limit** | A message that always fails processing creates an infinite redelivery loop — the RabbitMQ version of a poison-pill outage | Dead-letter after N attempts (`x-dead-letter-exchange` + an app-side attempt counter) |
| **Using the default exchange for everything** | Ties routing key directly to queue name with no flexibility — every future routing change requires touching every publisher | Declare named exchanges (topic/direct/fanout) deliberately, even for simple cases |
| **Opening a new connection per message** | Connection setup is expensive (TCP + AMQP handshake); doing it per-message throttles throughput and can exhaust broker file descriptors | Long-lived connection, one channel per logical worker/thread |

---

## RabbitMQ Cheat Sheet

- **No native partitions** — "sharding" means N queues + a hash-based routing key (Consistent Hash Exchange plugin, or hand-rolled).
- **Exchange choice:** direct = exact match, fanout = broadcast, topic = wildcard pattern, headers = match on metadata not key.
- **Reliability pair:** publisher confirms (did the broker get it) + manual consumer acks (did processing finish) — need both.
- **Prefetch (`x-qos`)** balances throughput vs. fairness; never leave it unlimited.
- **Queue type:** quorum queues for production, classic mirrored queues are deprecated — avoid.
- **Region strategy:** cluster only within a datacenter/LAN; Federation or Shovel between independent regional clusters, never a WAN-spanning cluster.
- **Always bound:** TTL + `x-max-length` on every production queue, and a dead-letter exchange with an attempt cap to stop infinite requeue loops.
