# RabbitMQ — Production Deep-Dive Guide

> **Enhancement notes:** this pass added content marked with 🆕 — everything else is the original guide, untouched in structure and order.
> - Filled gaps: durability's throughput cost (Section 5), a headers-exchange `x-match` binding example (Section 3), a TTL+DLX retry-with-backoff ladder since RabbitMQ has no native delay/visibility-timeout (Section 7), and a classic-mirrored-vs-quorum replication comparison explaining *why* quorum is safer, not just that it is (Section 6).
> - New diagrams: a full publish→ack→process→nack→dead-letter sequence diagram, a retry-ladder flowchart, a retry-decision flowchart, and two side-by-side sequence diagrams contrasting classic mirroring's async replication with quorum's Raft-majority replication.
> - New Section 11: a RabbitMQ vs. Kafka quick-recall table, for the "why this and not that" interview question.
> - Tightened prefetch tuning with a worked numeric example, and added an explicit auto-ack data-loss callout in Section 5.
> - Cheat sheet got a few new bullets reflecting the additions above; existing bullets are untouched.
> - Left as-is (already solid): the AMQP architecture recap, exchange-type flowchart and table, the "fake sharding" section, quorum queue basics, and the multi-region federation/shovel content.

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

#### 🆕 Headers exchange — the one people forget how to bind

Headers exchanges don't use a routing key at all — the binding carries an `x-match` argument that says whether **all** header keys must match (`x-match: all`, the AND case) or **any single one** is enough (`x-match: any`, the OR case):

```python
channel.exchange_declare(exchange="orders-headers", exchange_type="headers", durable=True)
channel.queue_bind(
    queue="us-high-priority",
    exchange="orders-headers",
    arguments={"x-match": "all", "region": "us", "priority": "high"},
)
channel.basic_publish(
    exchange="orders-headers",
    routing_key="",  # ignored by headers exchanges
    body=order_json,
    properties=pika.BasicProperties(headers={"region": "us", "priority": "high"}),
)
```

Headers exchanges are rarely the first choice — slower to match than direct/topic (the broker compares a dict, not a single string) — but they're the honest answer when a filter genuinely needs "AND across two independent attributes" and forcing that into one routing-key string (`"us.high"`) would just reinvent headers matching by hand.

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

`auto_ack=True` is the trap: the broker deletes the message the instant it hands it to the socket, before your callback even runs. If the consumer process dies mid-`process(body)`, that message is gone forever — no crash, no error, just silently lost. Manual ack after the work is actually done is what makes "at-least-once" true instead of aspirational.

### Prefetch (QoS) — the throughput/fairness dial

`prefetch_count` caps how many **unacked** messages the broker will push to a consumer before it stops sending more and waits for acks to free up room.

**Worked example:** say average processing time is 50ms per message (~20 msg/sec per consumer thread) and network round-trip to the broker is 2ms.
- `prefetch_count=1` — consumer processes one, acks, waits for the next: throughput per consumer stays near the theoretical ~20 msg/sec, and no other consumer can ever be starved by this one holding extra messages.
- `prefetch_count=20` — broker hands over up to 20 messages up front; the consumer works through its local backlog without waiting on a network round trip between every single message, pushing realized throughput closer to what the processing loop alone can sustain.
- `prefetch_count=0` (unlimited) — if this consumer stalls (GC pause, downstream call hangs), the broker can keep handing it messages up to the queue's entire depth, while sibling consumers on the same queue sit idle waiting for work that will never come.

| `prefetch_count` | Effect |
|---|---|
| `0` (unlimited) | **Danger** — one greedy/slow consumer can be handed the entire queue while siblings sit idle |
| `1` | Maximum fairness across consumers, but a round trip per message caps throughput |
| `20–100` (typical) | Balances throughput (batched delivery) against fairness (no single consumer hoards the queue) |

**Memory hook:** *publisher confirms answer "did the broker get it"; consumer acks answer "did the consumer finish it." You need both — confirming the publish says nothing about whether processing later crashed.*

#### 🆕 Durability's price tag — persistent messages + durable queues aren't free

Every durability knob in this guide (`durable=True` queue, `delivery_mode=2` persistent message, quorum replication, publisher confirms) trades throughput for the guarantee that a message survives a broker restart or crash. Roughly, moving up that ladder:

| Configuration | Survives broker restart? | Relative throughput (illustrative, not a benchmark) |
|---|---|---|
| Transient message, non-durable queue, no confirms | No | Fastest — everything stays in memory, no fsync |
| Persistent message (`delivery_mode=2`), durable queue, no confirms | Message is on disk, but you have no signal it got there | Noticeably slower — every message triggers a disk write |
| Persistent + durable + publisher confirms + quorum queue | Yes — the message survived an fsync and a Raft-majority replication | Slowest of the options — write is not "done" until disk + majority of replicas agree |

The exact numbers depend entirely on hardware (disk type, batching, network between quorum nodes) — don't quote a specific msg/sec figure in an interview unless you've benchmarked it yourself. The point to make out loud is qualitative and directional: **every durability guarantee you add costs a disk write, a network round trip, or both — durability is a dial you turn deliberately per queue, not a default you leave on everywhere "to be safe."** A queue full of low-value, replaceable cache-invalidation pings doesn't need the same durability spend as a queue of payment events.

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

#### 🆕 Why quorum beats classic mirrored — what actually happens on a publish

The failure mode that killed classic mirroring wasn't theoretical — it's *why the message data-loss risk shows up during partitions specifically*:

```mermaid
sequenceDiagram
    participant P as Publisher
    participant Q as Classic Mirrored Queue
    participant M as Mirror node

    P->>Q: publish (persistent)
    Q-->>P: ack (as soon as the primary has it)
    Q--)M: async mirror push (best-effort, no confirmation required)
    Note over Q,M: If the primary crashes before the mirror<br/>catches up, the "acked" message never arrives — silent loss
```

```mermaid
sequenceDiagram
    participant P as Publisher
    participant L as Quorum leader
    participant F1 as Follower 1
    participant F2 as Follower 2

    P->>L: publish (persistent)
    L->>F1: replicate (Raft log entry)
    L->>F2: replicate (Raft log entry)
    F1-->>L: ack
    Note over L: majority reached (2 of 3) — leader alone isn't enough
    L-->>P: ack (only now — message is durable on a majority)
```

Classic mirroring acks the publisher once the **primary alone** has the message, then copies it to mirrors on a best-effort basis — a primary crash in that window loses an already-acked message. Quorum queues use Raft: the publisher isn't acked until a **majority** of replicas have the entry in their log, so any single node (including the leader) can die without losing an acked message. A 3-node quorum queue tolerates 1 node failure; a 5-node quorum tolerates 2 — the trade-off is more nodes replicating every write, which costs write latency and disk, not "free" HA.

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

#### 🆕 The full flow: publish → ack → process → nack → dead-letter

The two reliability mechanisms from Section 5 and dead-lettering aren't separate features — they compose into one flow. Here's what happens when processing fails:

```mermaid
sequenceDiagram
    participant Pub as Publisher
    participant Broker
    participant Consumer
    participant DLX as orders-dlx (exchange)
    participant DLQ as orders-dlq (queue)

    Pub->>Broker: publish (persistent, confirm requested)
    Broker-->>Pub: publisher confirm (message durably queued)
    Broker->>Consumer: deliver message (attempt 1)
    Consumer->>Consumer: process() throws
    Consumer->>Broker: basic.nack(requeue=false)
    Broker->>DLX: route dead-lettered message
    DLX->>DLQ: bind matches, message lands in DLQ
    Note over DLQ: on-call alerts on DLQ depth > 0 — never a silent drop
```

#### 🆕 RabbitMQ has no built-in delay — the TTL + DLX retry ladder

Unlike SQS (which has a native visibility timeout you can just bump), RabbitMQ has no "retry this in 30 seconds" primitive. The standard way to get delayed, backed-off retries out of RabbitMQ primitives is a **ladder of retry queues**, each with a longer TTL, that dead-letter back into the real work queue once their TTL expires:

```mermaid
flowchart LR
    Q[orders queue] -->|nack, requeue=false| DLX1{{retry-dlx}}
    DLX1 --> R1["retry-10s queue
    TTL=10s, no consumer,
    DLX points back to orders exchange"]
    R1 -->|TTL expires| Q
    Q -->|fails again| DLX1
    DLX1 --> R2["retry-60s queue
    TTL=60s"]
    R2 -->|TTL expires| Q
    Q -->|fails a 3rd time| Final[[final-dlq
    dead — human inspects/replays]]
```

```python
# Retry queue: no consumer ever reads this directly — messages just sit
# until their TTL expires, then get dead-lettered back to the real queue.
channel.queue_declare(
    queue="orders-retry-10s",
    durable=True,
    arguments={
        "x-message-ttl": 10_000,                    # 10s backoff
        "x-dead-letter-exchange": "orders",          # bounces back to the live exchange
        "x-dead-letter-routing-key": "orders.us.created",
    },
)
```

Each hop through the ladder increments a retry-count header (`x-death` is populated automatically by RabbitMQ on every dead-letter hop — read `len(properties.headers["x-death"])` in the consumer to decide "have I already retried twice? route straight to `final-dlq` instead of the ladder again."). This gives exponential-feeling backoff (10s, 60s, 5m, ...) without a scheduler process, entirely out of TTL + dead-lettering.

#### 🆕 Retry decision logic

```mermaid
flowchart TD
    A[Message delivered to consumer] --> B{process() succeeds?}
    B -->|Yes| C[basic.ack — done]
    B -->|No| D{Retry count from<br/>x-death header < max attempts?}
    D -->|Yes| E["nack, requeue=false
    → next rung of retry ladder
    (TTL backoff before it comes back)"]
    D -->|No, exhausted| F["nack, requeue=false
    → final DLQ
    alert on-call, no more auto-retry"]
```

**Memory hook:** *ack = done, nack+requeue=false = "not now, try later or give up" — never nack+requeue=true in a loop without a counter, that's just a spin loop with extra steps.*

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

## 🆕 11. RabbitMQ vs Kafka — Quick Recall Table

The two get conflated in interviews because both are "the message broker." They're built on different mental models — use this to answer "why RabbitMQ here and not Kafka" (or vice versa) fast:

| Dimension | RabbitMQ | Kafka |
|---|---|---|
| Core model | Broker routes messages through exchanges/bindings into queues, then deletes them once acked | Append-only log per partition; consumers track their own offset, message stays until retention expires |
| "Partitioning" | None natively — fake it with N queues + a hash-based routing key (Section 4) | Native — topic is split into partitions, the unit of parallelism and ordering |
| Delivery to multiple independent consumer groups | Needs a fanout exchange to N queues, one per consumer group | Native — any number of consumer groups read the same log independently, at their own pace |
| Replay old messages | Not really — once acked and deleted, it's gone (unless using Streams) | Yes — reset a consumer group's offset and re-read history within retention |
| Routing flexibility | Rich — direct/topic/fanout/headers exchanges, per-message routing decisions | Minimal — producer picks a partition (usually by key hash), no per-message routing logic |
| Ordering guarantee | Per-queue FIFO | Per-partition FIFO only — no global ordering across partitions |
| Typical use case | Task queues, RPC-style work distribution, complex routing topologies, priority/delay patterns | Event streaming, high-throughput logs, replay/reprocessing, multiple teams reading the same event stream |

**Memory hook:** *Kafka is a log you replay; RabbitMQ is a router that deletes on delivery. If the interview needs "replay history" or "many independent readers of the same stream," that's a Kafka-shaped need even if the rest of the design leans RabbitMQ.*

---

## RabbitMQ Cheat Sheet

- **No native partitions** — "sharding" means N queues + a hash-based routing key (Consistent Hash Exchange plugin, or hand-rolled).
- **Exchange choice:** direct = exact match, fanout = broadcast, topic = wildcard pattern, headers = match on metadata not key (`x-match: all/any`).
- **Reliability pair:** publisher confirms (did the broker get it) + manual consumer acks (did processing finish) — need both. `auto_ack=True` silently loses messages on a consumer crash.
- **Prefetch (`x-qos`)** balances throughput vs. fairness; never leave it unlimited. Low prefetch = fair but slower, high prefetch = faster but one stuck consumer can hoard messages.
- **Durability costs something** — persistent messages + durable queues + confirms + quorum replication all trade throughput for "survives a crash." Turn the dial per queue, not globally.
- **Queue type:** quorum queues for production, classic mirrored queues are deprecated — avoid. Quorum only acks after a Raft majority, so no single node's crash loses an acked message.
- **Retry without a scheduler:** RabbitMQ has no native delay — build backoff from a ladder of TTL queues that dead-letter back to the real queue, then a final DLQ after N attempts.
- **Region strategy:** cluster only within a datacenter/LAN; Federation or Shovel between independent regional clusters, never a WAN-spanning cluster.
- **Always bound:** TTL + `x-max-length` on every production queue, and a dead-letter exchange with an attempt cap to stop infinite requeue loops.
- **vs. Kafka:** RabbitMQ routes-and-deletes, Kafka logs-and-replays — reach for Kafka when the design needs replay or many independent consumer groups over the same stream.
