# Apache Kafka as Pub-Sub — Deep Dive & Production Guide

> **Enhancement notes:** this pass added the pub-sub internals that were
> thin or missing, marked `🆕` at each new heading. Existing sections,
> voice, and structure are untouched otherwise.
> - Added offset management & delivery semantics (at-most/at-least/exactly-once), with a sequence diagram covering produce → fetch → process → commit.
> - Added the consumer-group rebalancing protocol (JoinGroup/SyncGroup, eager vs. cooperative-sticky, session/poll-interval timeouts) as its own flowchart.
> - Added a topic/partition/broker/replica cluster-layout diagram, an explicit "ordering is per-partition only, never global" section, and a retention-vs-compaction subsection.
> - Added comparison tables (no-key vs. key-based vs. salted keying; at-most/at-least/exactly-once trade-offs) plus quick-recall "if X then Y" lines and one mnemonic for offset-commit timing.
> - Folded the new concepts into the Golden rules and Cheat sheet; left the cross-region, mistakes, and internals (page cache/zero-copy, purgatory, ISR) sections as-is since they were already solid.

Companion deep-dive to `18-Pub-sub-FAANG-Guide.md`. This file goes past "Kafka
has partitions" into the internals, the exact knobs you configure to create
and shard topics correctly, how real companies run Kafka across regions, and
the specific mistakes that show up in production.

**Framing this file cares about**: Kafka is being treated here strictly as a
**pub-sub system** — one write, many independent readers, replay within
retention — not as a work queue. The queue-like behavior (one message, one
worker) only shows up *inside a single consumer group*; the moment you have
two consumer groups reading the same topic, you're looking at true pub-sub
fan-out. Keep that framing in your head through this whole guide.

## Mental model

Kafka is a **DVR for events, rented out as a broadcast service**. Unlike
Redis Pub/Sub's live radio, every message is recorded to disk *first*, then
served. Every subscriber (consumer group) gets its own "remote" — its own
independent position in the recording — so slow subscribers, new
subscribers, and subscribers that crash and restart all just rewind/resume
without affecting anyone else or the producer.

## How it works internally

### 🆕 Cluster layout: topics, partitions, brokers, and replicas

Before the request pipeline and log format below, here's the picture those
details live inside. A **topic** is just a name. A **partition** is the unit
of parallelism and ordering. Each partition has copies (**replicas**) spread
across different **brokers**, and exactly one replica is elected **leader**
per partition at any time.

```mermaid
flowchart TB
    subgraph Broker1["Broker 1"]
        P0L["orders-P0 (Leader)"]
        P1F1["orders-P1 (Follower)"]
        P2F1["orders-P2 (Follower)"]
    end
    subgraph Broker2["Broker 2"]
        P0F1["orders-P0 (Follower)"]
        P1L["orders-P1 (Leader)"]
        P2F2["orders-P2 (Follower)"]
    end
    subgraph Broker3["Broker 3"]
        P0F2["orders-P0 (Follower)"]
        P1F2["orders-P1 (Follower)"]
        P2L["orders-P2 (Leader)"]
    end
```

Concrete example: topic `orders` has 3 partitions, replication factor 3, on a
3-broker cluster. The controller spreads leadership so each broker leads
roughly a third of the partitions and follows the rest — no single broker
carries all the write traffic. Producers and consumers only ever talk to a
partition's **leader**; followers exist purely for durability (see
Replication below).

This is also *why* partitioning buys parallelism: a partition is a single
append-only log, served by one leader at a time. Partition count is therefore
a hard ceiling on both how many consumers in one group can do useful work at
once, and how many brokers can share the write load for one topic. More
partitions → more parallelism, up to however many consumers/brokers you
actually run.

### Broker request pipeline

A Kafka broker doesn't process a request the instant it arrives — there's a
pipeline designed to keep the broker responsive even while some requests
(like a `acks=all` produce waiting on replication, or a fetch waiting for
enough new data) are slow.

```mermaid
flowchart LR
    Client[Producer / Consumer] --> NT["Network threads\n(parse requests)"]
    NT --> RQ["Shared request queue"]
    RQ --> IO["I/O threads\n(read/write the log)"]
    IO --> Purg["Purgatory\n(park requests waiting on\na condition, e.g. acks=all)"]
    Purg -->|condition met| Resp[Response sent back]
    IO -->|condition already met| Resp
```

**Purgatory** is the internal name for where "not-yet-satisfiable" requests
wait — e.g., a producer that asked for `acks=all` is parked until every
in-sync replica has confirmed the write, and a consumer `fetch` with
`fetch.min.bytes` set can be parked until enough data accumulates (fewer,
bigger fetches instead of constant tiny ones).

### The log: segments, indexes, and why reads are fast

A partition on disk isn't one giant file — it's a directory of rotating
**segments**, each with a paired sparse index for O(log n) offset lookup
instead of a linear scan.

```mermaid
flowchart TD
    subgraph "Partition 0 directory"
        S1["00000000000000000000.log\n(segment: messages 0-999)"]
        I1["00000000000000000000.index\n(offset -> byte position)"]
        T1["00000000000000000000.timeindex\n(timestamp -> offset)"]
        S2["00000000000001000000.log\n(segment: messages 1000-1999, active)"]
        I2["00000000000001000000.index"]
        T2["00000000000001000000.timeindex"]
    end
    S1 -.paired with.- I1
    S1 -.paired with.- T1
    S2 -.paired with.- I2
    S2 -.paired with.- T2
```

- Segments roll over based on size (`log.segment.bytes`, default 1GB) or age
  (`log.roll.ms`).
- Retention deletes/compacts **whole segment files**, not individual
  messages — this is why retention cleanup is cheap: no per-message
  bookkeeping.
- Kafka deliberately relies on the **OS page cache**, not JVM heap, for
  message data — writes go to the page cache and are flushed by the OS, and
  reads for recent data are served straight from page cache without touching
  disk. This is the single biggest reason Kafka gets away with disk-backed
  storage at very high throughput.
- Consumer fetches use **zero-copy** (`sendfile()`): data goes straight from
  the page cache to the network socket without being copied into the
  broker's user-space/JVM heap first.

```mermaid
sequenceDiagram
    participant Disk as Page Cache / Disk
    participant B as Broker (JVM)
    participant C as Consumer

    Note over B,C: Naive path (NOT what Kafka does)
    Disk->>B: read into JVM heap buffer
    B->>B: copy to socket buffer
    B->>C: send

    Note over B,C: Kafka's zero-copy path
    Disk->>C: sendfile() — page cache to socket, JVM never touches the bytes
```

#### 🆕 Retention vs. compaction

Retention (`cleanup.policy=delete`, the default) drops whole old segments
once they age out. **Compaction** (`cleanup.policy=compact`) is a different
cleanup strategy entirely: instead of deleting by age, it keeps only the
**latest value per key**, forever — a background log-cleaner thread drops
older values for a key once it compacts that segment.

| `cleanup.policy` | What survives | Use it for |
|---|---|---|
| `delete` (default) | Records within `retention.ms` / `retention.bytes`; the whole segment is dropped after that | Event streams — order-created, click, log events — where you need recent history, not every past version |
| `compact` | Only the newest value per key, kept indefinitely | "Latest state per key" topics — e.g. keyed by `user_id` holding the current profile, backing a KTable or rebuilding a cache from scratch |
| `compact,delete` | Compacted by key **and** still capped by time/size | Compacted topics that also need a hard retention ceiling |

A record with a **null value** for a key is a **tombstone** — after a delay
(`delete.retention.ms`), compaction removes that key entirely. Compaction
only runs over already-closed (non-active) segments, so there's always some
lag before a superseded value is actually reclaimed — don't rely on
compaction for immediate deletion.

### Replication: leader, ISR, and the controller

```mermaid
stateDiagram-v2
    [*] --> Leader: elected by controller quorum (KRaft)
    Leader --> Leader: serves all produce/fetch for this partition
    Leader --> [*]: broker crash detected (session timeout)
    InSyncReplica --> Leader: controller promotes\n(must be in ISR at crash time)
    Leader --> InSyncReplica: was leader, demoted after rejoining as follower
    OutOfSyncReplica --> InSyncReplica: catches up within replica.lag.time.max.ms
    InSyncReplica --> OutOfSyncReplica: falls behind (slow disk, GC pause, network)
```

- **ISR (In-Sync Replica set)**: replicas that have fetched up to (within a
  bounded lag of) the leader's log. Only ISR members are eligible for
  leader promotion — promoting a replica outside the ISR would silently
  drop committed messages.
- **`acks=all` + `min.insync.replicas=2`** (with replication factor 3) is the
  standard durable combo: a write is only acknowledged once the leader *and*
  at least one follower have it, so losing any single broker can't lose data.
  `acks=all` with `min.insync.replicas=1` is a trap — it degrades to
  effectively the same guarantee as `acks=1` the moment the ISR shrinks to
  just the leader.
- **Controller (KRaft)**: since Kafka 3.3 (production-ready) and mandatory
  from Kafka 4.0 onward, a quorum of controller nodes (Raft-based) owns
  cluster metadata and leader election, replacing the old ZooKeeper
  dependency — one less distributed system to run in production.

## Creating topics and choosing your partition key

### Creating a topic

```bash
kafka-topics.sh --bootstrap-server localhost:9092 --create \
  --topic orders \
  --partitions 12 \
  --replication-factor 3 \
  --config retention.ms=604800000 \
  --config min.insync.replicas=2
```

```mermaid
flowchart TD
    A["Decide topic name\n(env.domain.entity.event convention)"] --> B["Decide partition count\n(throughput target ÷ per-partition ceiling,\nand ≥ max planned consumer parallelism)"]
    B --> C["Decide replication factor\n(3 is standard for production)"]
    C --> D["Decide retention\n(time-based vs. compacted)"]
    D --> E["Decide cleanup policy:\ndelete (time/size) vs. compact (keep latest per key)"]
    E --> F[kafka-topics.sh --create]
```

**Naming convention** (borrow this, don't invent one per team):
```
<env>.<domain>.<entity>.<event>
prod.orders.order.created
prod.payments.invoice.paid
staging.notifications.email.sent
```

### Partition key strategy — the decision that determines ordering and hot spots

```mermaid
flowchart TD
    K{"Do you need ordering\nfor a specific entity?"}
    K -->|"No — any order is fine"| NK["Send with no key\n(sticky partitioning batches\nrecords efficiently per batch)"]
    K -->|"Yes, e.g. all events for\none order_id must be in order"| WK["Key by that entity's ID\n(murmur2(key) % partitions)"]
    WK --> Skew{"Is that key's traffic\nheavily skewed?\n(e.g. one viral order/celebrity)"}
    Skew -->|"No"| Done["Good — even spread + per-key order"]
    Skew -->|"Yes"| Salt["Composite/salted key:\norder_id + hash(order_id) % SALT_BUCKETS\n(spreads the hot key, relaxes strict\nordering to 'ordered within a bucket')"]
```

- **No key**: since KIP-480/482, the default partitioner uses **sticky
  partitioning** — it batches all the null-key records arriving in a short
  window into one partition per batch (not pure round-robin per record),
  which produces bigger, more efficient batches without hurting balance over
  time.
- **With a key**: `murmur2(key) % num_partitions` by default. Same key always
  → same partition → strict per-key ordering, at the cost of that key's
  entire load landing on one partition.
- **Hot key mitigation**: salt the key (append a bounded random or
  hash-derived suffix) to spread one overloaded entity across N partitions —
  you trade "strict order for this entity" for "order within each of the N
  buckets," which is usually an acceptable relaxation (e.g., process-then-
  reconcile patterns, or accept bounded reordering for non-financial events).

**Side by side:**

| Strategy | Ordering guarantee | Load balance | Watch out for |
|---|---|---|---|
| No key (sticky partitioning) | None across records | Even, adapts per batch automatically | Fine only when consumers don't care about per-entity order |
| Key-based (`murmur2(key) % N`) | Strict order per key | Even *if* keys are evenly distributed | One hot key (viral order, celebrity account) can overload a single partition |
| Salted/composite key | Order only within a bucket | Even even under a hot key | Relaxes strict per-entity order — needs reconciliation logic downstream |

#### 🆕 Ordering guarantees: per-partition only, never global

Kafka only guarantees order **within a single partition**: records written to
partition P are read back by a consumer of P in exactly the order they were
appended. There is **no ordering guarantee across partitions** of the same
topic — a topic with 12 partitions is 12 independent logs, unordered with
respect to each other.

That's exactly why key-based partitioning matters: keying by `order_id`
doesn't create a magic "ordered stream for this order" — it just forces
every event for that key onto the *same* partition, and then rides that
partition's natural order. Ordering is a side effect of partition placement,
not a separate feature.

Quick recall:
- Need strict order for one entity → key by that entity's ID (forces same partition).
- Need global order across the *entire* topic → use exactly 1 partition (and give up parallelism — one leader, one consumer at a time).
- Don't care about order → no key, let sticky partitioning balance the load.

### Consumer groups — this is where "pub-sub, not queue" actually shows up

```mermaid
flowchart TD
    T["Topic: orders (12 partitions)"]
    subgraph "Consumer Group: billing-service"
        CB1[Consumer 1: P0-P3]
        CB2[Consumer 2: P4-P7]
        CB3[Consumer 3: P8-P11]
    end
    subgraph "Consumer Group: shipping-service"
        CS1[Consumer 1: P0-P5]
        CS2[Consumer 2: P6-P11]
    end
    subgraph "Consumer Group: fraud-detection"
        CF1[Consumer 1: P0-P11 alone]
    end
    T --> CB1
    T --> CB2
    T --> CB3
    T --> CS1
    T --> CS2
    T --> CF1
```

Every one of those three groups gets **every message** on the topic — that's
the pub-sub fan-out. *Within* `billing-service`, the three consumers split
the 12 partitions between them — that's the queue-like load balancing. Same
topic, three independent "queues," each processing the full stream at its
own pace. This diagram is the single best whiteboard artifact for answering
"how is Kafka pub-sub, not just a queue?"

Concrete example: a topic with 50 partitions and a consumer group of 10
consumers → the group coordinator hands each consumer exactly 5 partitions.
Scale down to 5 consumers and each gets 10; add an 11th consumer past 50 and
it sits idle — partition count is still the hard ceiling on parallelism.

**Producer/consumer code (concise, Python + `confluent-kafka`):**

```python
# Producer — key by order_id for per-order ordering
from confluent_kafka import Producer

p = Producer({"bootstrap.servers": "localhost:9092"})
p.produce("prod.orders.order.created", key="order-42", value='{"status":"created"}')
p.flush()

# Consumer — independent group.id = independent full copy of the stream
from confluent_kafka import Consumer

c = Consumer({
    "bootstrap.servers": "localhost:9092",
    "group.id": "billing-service",          # change this -> new independent subscriber
    "auto.offset.reset": "earliest",
    "partition.assignment.strategy": "cooperative-sticky",
})
c.subscribe(["prod.orders.order.created"])

while True:
    msg = c.poll(1.0)
    if msg is None:
        continue
    print(msg.key(), msg.value())
    c.commit(msg)
```

#### 🆕 Consumer-group rebalancing: the protocol

Partition assignment inside a group isn't static — it's recomputed by a
**rebalance** whenever membership or topic metadata changes. Each group has a
**group coordinator** (a broker) that tracks membership via heartbeats.

```mermaid
flowchart TD
    Trigger{"What triggered this?"}
    Trigger -->|"New consumer subscribes and polls"| Join["Consumer sends JoinGroup\nto the Group Coordinator"]
    Trigger -->|"No heartbeat within\nsession.timeout.ms"| Detect["Coordinator marks member dead"]
    Trigger -->|"poll() loop exceeds\nmax.poll.interval.ms\n(stuck processing)"| Detect
    Trigger -->|"Consumer calls close() cleanly"| Leave["Consumer sends LeaveGroup"]
    Trigger -->|"Topic partition count changes"| Meta["Coordinator sees new metadata"]

    Join --> Rebalance["Rebalance starts —\ncoordinator picks a group leader\n(first member to join)"]
    Detect --> Rebalance
    Leave --> Rebalance
    Meta --> Rebalance

    Rebalance --> Strategy{"Assignor configured?"}
    Strategy -->|"range / round-robin\n(eager protocol)"| Eager["ALL members revoke ALL\npartitions first,\nthen everything is reassigned\n-> whole group pauses"]
    Strategy -->|"cooperative-sticky\n(incremental protocol)"| Coop["Only the partitions that must\nmove are revoked;\nunaffected members keep consuming"]

    Eager --> Assign["Group leader computes the\nnew assignment, sends via SyncGroup"]
    Coop --> Assign
    Assign --> Resume["Members resume fetching\nwith their new assignment"]
```

- **Eager (`range`, `round-robin`)**: the historical default — simple, but
  every rebalance is stop-the-world for the *entire* group, which hurts
  during autoscaling or rolling deploys (see Mistake #6 below).
- **`cooperative-sticky`**: incremental — a member gives up only the specific
  partitions that need to move; everyone else keeps processing uninterrupted.
  Prefer this for any group that scales up/down regularly.
- Two knobs govern how fast a dead consumer is detected: `session.timeout.ms`
  (missed heartbeats) and `max.poll.interval.ms` (the caller took too long
  between `poll()` calls — usually slow processing, not a dead process). Set
  `max.poll.interval.ms` above your worst-case processing time per batch, or
  Kafka will treat a slow-but-alive consumer as dead and rebalance away from
  it.

#### 🆕 Offset management and delivery semantics

Kafka doesn't track "has this consumer read this message" inside the data
log itself — each consumer group **commits its offsets** (the position it
has finished reading, per partition) to an internal compacted topic called
`__consumer_offsets` (50 partitions by default). Committing an offset is
just another write to Kafka.

```mermaid
sequenceDiagram
    participant P as Producer
    participant L as Partition Leader
    participant C as Consumer
    participant GC as Group Coordinator (offsets topic)

    P->>L: produce(key="order-42", acks=all)
    L-->>P: ack (offset 10482 written + replicated)

    C->>L: poll() — fetch from last committed offset
    L-->>C: records up through offset 10482
    C->>C: process records
    C->>GC: commitSync(offset=10483)
    GC-->>C: ack — offset persisted

    Note over C,GC: Crash between "process" and "commit"?<br/>Next consumer resumes from the LAST<br/>COMMITTED offset and reprocesses 10482<br/>-> at-least-once, not exactly-once
```

| Semantic | How you get it | What can go wrong | Typical use |
|---|---|---|---|
| At-most-once | Commit offset *before* processing (or a stray auto-commit fires ahead of finishing) | Crash after commit, before processing finishes → message silently skipped | Rarely intentional; tolerable for best-effort metrics |
| At-least-once (default, most common) | `enable.auto.commit=false`, commit *after* processing completes | Crash after processing, before commit → message reprocessed | Default for most pipelines — pair with idempotent downstream writes |
| Exactly-once (Kafka-to-Kafka) | Idempotent producer (`enable.idempotence=true`, on by default since 3.0) + transactions (`transactional.id`, consumer `isolation.level=read_committed`) | Only covers Kafka-to-Kafka; a side effect to an external DB/API in the same step isn't automatically covered | Kafka Streams / ksqlDB, consume-transform-produce pipelines |

Mnemonic: **commit-after = redo risk (at-least-once), commit-before = skip
risk (at-most-once)** — pick the failure mode you can tolerate; "exactly
once" is a real but narrower guarantee (idempotent producer + transactions),
not a magic default you get for free.

### Growing a topic's partitions — the ordering gotcha

```mermaid
flowchart LR
    Before["Before: 6 partitions\norder-42 -> murmur2(order-42) % 6 = P3"]
    Repartition["ALTER: increase to 12 partitions"]
    After["After: order-42 -> murmur2(order-42) % 12 = P9\n(different partition!)"]
    Before --> Repartition --> After
    After --> Warn["New events for order-42 land on P9,\nold ones are still on P3 —\nper-key ordering across the change is broken"]
```

You can only **increase** partition count, never decrease it, and increasing
it changes the `hash % N` result for every existing key — anything relying
on "same key always same partition" breaks across that boundary. Plan
partition count for peak capacity up front; don't casually resize a
production topic that has ordering-sensitive keys.

## Cross-region / multi-datacenter production patterns

### MirrorMaker 2 — the standard replication tool

MirrorMaker 2 (built on Kafka Connect) replicates topics between clusters.
By default it **prefixes replicated topic names with the source cluster
alias** (e.g., `us-west.orders`) specifically to avoid an infinite
replication loop when you wire two clusters to mirror each other.

```mermaid
flowchart LR
    subgraph "us-west cluster"
        UWT["orders"]
    end
    subgraph "eu-central cluster"
        ECT["orders"]
        Mirrored["us-west.orders\n(mirrored copy)"]
    end
    UWT -->|MirrorMaker 2| Mirrored
```

### Active-passive (DR) — the simple, safe default

```mermaid
flowchart LR
    subgraph "Primary region (us-west) — all writes"
        P["Producers"] --> PT["orders"]
    end
    subgraph "DR region (eu-central) — read-only mirror"
        DT["us-west.orders"]
        C["Consumers (only activated on failover)"]
    end
    PT -->|MirrorMaker 2| DT
```

One region writes, the other is a replicated standby promoted only during a
regional failure. Simplest to reason about; the trade-off is the DR region's
compute sits mostly idle.

### Active-active — real fan-out, real complexity

```mermaid
flowchart LR
    subgraph "us-west"
        PW["Producers"] --> TW["orders"]
        TW2["eu-central.orders (mirrored)"]
    end
    subgraph "eu-central"
        PE["Producers"] --> TE["orders"]
        TW3["us-west.orders (mirrored)"]
    end
    TW -->|MirrorMaker 2| TW3
    TE -->|MirrorMaker 2| TW2
```

Both regions accept local writes and mirror to each other — lower write
latency for local producers in each region, but now you must handle: topic
name proliferation (`us-west.orders` vs `eu-central.orders` vs local
`orders` — which do consumers actually read?), **offset translation** (an
offset in the mirrored topic isn't the same offset as the source), and
potential **duplicate processing** if a consumer isn't careful about which
copies of a topic it subscribes to.

### The anti-pattern: stretching one cluster across regions

```mermaid
flowchart LR
    subgraph "One 'cluster' spanning continents — DON'T"
        B1["Broker (us-west)"]
        B2["Broker (eu-central), same cluster"]
        B1 <-->|"ISR replication over\n80-150ms cross-region RTT"| B2
    end
    Producer["Producer, acks=all"] --> B1
    B1 -.->|"blocked waiting for\ncross-region ISR ack"| Slow["Produce latency ≈ cross-region RTT"]
```

Never spread a single Kafka cluster's brokers across regions. ISR
replication assumes low, LAN-grade latency; putting it over a 80-150ms
cross-region link means every `acks=all` produce pays that full round trip,
and transient cross-region network blips look like broker failures, causing
spurious ISR shrinkage and leader elections. **Run one cluster per region,
replicate between clusters with MirrorMaker 2** — never one cluster spanning
regions.

### Aggregate cluster pattern (for global analytics)

```mermaid
flowchart TD
    R1["us-west cluster"] -->|MirrorMaker 2| AGG["Aggregate cluster\n(all regions' topics, prefixed)"]
    R2["eu-central cluster"] -->|MirrorMaker 2| AGG
    R3["ap-south cluster"] -->|MirrorMaker 2| AGG
    AGG --> Analytics["Global analytics / ML consumers\nread everything in one place"]
```

Each region keeps its own local, low-latency cluster for regional producers
and consumers; a separate aggregate cluster collects a mirrored copy of
everything for global consumers (analytics, monitoring) that need the full
picture — without forcing every regional producer to pay cross-region
latency just so a dashboard in one place can see it all.

## Mistakes to avoid

```mermaid
flowchart TD
    A["Kafka in production"] --> M1["Too few partitions\n-> caps consumer parallelism forever"]
    A --> M2["Too many partitions\n-> controller/metadata overhead,\nlonger failover, file handle explosion"]
    A --> M3["acks=all but min.insync.replicas=1\n-> false sense of durability"]
    A --> M4["Growing partition count on a\nkey-ordered topic without realizing\nit remaps existing keys"]
    A --> M5["Ignoring consumer lag monitoring\n-> silent, invisible backlog growth"]
    A --> M6["Stop-the-world (eager) rebalancing\nin a fast-scaling consumer group"]
    A --> M7["Stretching one cluster across regions\ninstead of MirrorMaker between clusters"]
    A --> M8["No retention/disk sizing plan\n-> broker disk fills, crash loop"]
    A --> M9["Reaching for Kafka when a single-process\nqueue or Redis Pub/Sub would do"]
```

1. **Too few partitions.** Partition count is a hard ceiling on how many
   consumers in a single group can work in parallel — you cannot add a 5th
   parallel consumer to a 4-partition topic; it will sit idle. Size for peak
   planned consumer parallelism, not today's.
2. **Too many partitions.** Every partition costs open file handles (segment
   + index + timeindex files ×replication factor), adds controller metadata
   overhead, and lengthens leader-election/failover time during a broker
   restart. More isn't free — size to the throughput math, not "just use a
   big number to be safe."
3. **`acks=all` with `min.insync.replicas=1`.** This combination silently
   degrades to leader-only durability the instant any follower falls out of
   the ISR — always pair `acks=all` with `min.insync.replicas ≥ 2` (for
   replication factor 3) if durability is the actual goal.
4. **Repartitioning a key-ordered topic without accounting for remapping.**
   Increasing partitions changes `hash(key) % N` for every key — anything
   depending on "same key, same partition, therefore ordered" breaks at that
   boundary. Plan capacity up front instead.
5. **No consumer lag monitoring.** Because pub-sub decouples producer and
   consumer speed by design, a consumer silently falling behind produces no
   error anywhere — until retention expires and it starts losing data. Lag
   must be a first-class, alerted metric, not something you check
   reactively.
6. **Default eager rebalancing at scale.** The older eager assignor
   revokes *all* partitions from *all* group members on every join/leave,
   pausing the entire group — painful during autoscaling or rolling
   deploys. Use `cooperative-sticky` so only the specific partitions that
   need to move actually get reassigned.
7. **Stretching a single cluster across regions.** Covered above — this is
   the multi-region mistake senior candidates are expected to call out
   unprompted.
8. **No retention/disk capacity plan.** Retention is a promise about how
   much disk you need, not just a business/compliance policy — undersizing
   broker disk relative to `retention.ms × ingest rate × replication factor`
   causes brokers to hit disk-full and crash-loop under real traffic.
9. **Reaching for Kafka when you don't need it.** Running and tuning a
   distributed log cluster (ZooKeeper/KRaft, ISR tuning, partition
   rebalancing, MirrorMaker) is real, ongoing operational cost. If you don't
   need replay, don't need multiple independent consumer groups, and volume
   is modest, a managed queue (SQS) or even Redis Pub/Sub may be the
   correctly lazy choice — know when *not* to bring Kafka into a design.

## Golden rules

- **Partition count is a capacity decision made once, not a knob you casually
  turn later** — decreasing is impossible, increasing remaps every key.
- **`acks=all` is only as strong as `min.insync.replicas`** — always state
  both together, never one without the other.
- **One cluster per region, MirrorMaker between them** — never one cluster
  stretched across a continent.
- **Multiple consumer groups is what makes Kafka pub-sub instead of a
  queue** — a single group behaves exactly like a queue; that's not a bug,
  it's the mechanism.
- **Order is per-partition, not global** — keying gives you per-key order by
  forcing one partition; never assume order across partitions.
- **Commit-after vs. commit-before offset is your delivery-semantics
  choice** — know which failure mode (duplicate vs. skip) you're accepting.

## Cheat sheet

- Internals: network threads → shared request queue → I/O threads →
  purgatory (parks acks=all / fetch.min.bytes waits) → response. Storage
  relies on OS page cache + zero-copy `sendfile()`, not JVM heap.
- Log structure: partition → rotating segments, each with a `.log` +
  sparse `.index` + `.timeindex` — retention deletes whole segments, cheap.
- Durability knob pair: `acks=all` + `min.insync.replicas ≥ 2` (replication
  factor 3). ISR membership gates leader-election eligibility.
- Controller: KRaft quorum (Kafka 3.3+ production, mandatory 4.0+) — no more
  ZooKeeper dependency.
- Retention vs. compaction: `delete` expires whole segments by age/size;
  `compact` keeps only the latest value per key forever — use compaction for
  "current state per key" topics, not event streams.
- Keying: no key → sticky partitioning (efficient batches); with key →
  `murmur2(key) % partitions` (per-key order, watch for hot keys → salt the
  key to spread).
- Ordering: guaranteed **within a partition only**, never across partitions
  of the same topic. Global order requires exactly 1 partition (no
  parallelism).
- Consumer groups: one group = queue-like load balancing; multiple
  independent groups on one topic = true pub-sub fan-out — this is the
  mechanism, not a workaround.
- Rebalancing: eager assignors (`range`/`round-robin`) pause the whole group
  on every join/leave; `cooperative-sticky` only moves the partitions that
  must move — prefer it.
- Offsets and semantics: committed to `__consumer_offsets`, not the data log.
  Commit-after-processing = at-least-once (default, expect duplicates);
  commit-before = at-most-once (expect gaps); idempotent producer +
  transactions (`read_committed`) = exactly-once, Kafka-to-Kafka only.
- Cross-region: MirrorMaker 2 between **separate regional clusters**
  (active-passive for simplicity, active-active for local write latency,
  aggregate cluster for global analytics) — never stretch one cluster across
  regions.
- Top production mistakes to name unprompted: partition count as a
  one-way door, `acks=all` without `min.insync.replicas`, repartitioning
  breaking key ordering, unmonitored consumer lag, eager rebalancing storms,
  cross-region stretch clusters, disk sizing vs. retention.
