# Key-Value Store — High Level Design

## Requirements Clarification

Before diving in, it's worth scoping the system:

**Functional:** `get(key)`, `put(key, value)`, `delete(key)`. Optional: TTL/expiry, versioning, transactions.

**Non-Functional (assumptions):**
- High availability over strong consistency (AP system)
- Low latency reads and writes (P99 < 10ms)
- Horizontal scalability (petabytes of data)
- Eventual consistency acceptable (tunable)

---

## Core Architecture

```
Client → Load Balancer → Coordinator Nodes → Storage Nodes
                                    ↓
                            Gossip / Membership
```

The design draws inspiration from **Dynamo** (Amazon) and **Cassandra**.

---

## 1. Data Partitioning — Consistent Hashing

Use a **consistent hash ring** to distribute keys across nodes.

- Each node owns a range of the ring. `hash(key) % ring_size` determines placement.
- Use **virtual nodes (vnodes)** — each physical node gets ~150 tokens — to handle heterogeneous hardware and smooth out rebalancing when nodes join/leave.
- When a node is added/removed, only its neighboring keys migrate, not the whole dataset.

---

## 2. Replication

- Replicate each key to **N nodes** (typically N=3) — the coordinator writes to the first N healthy nodes clockwise on the ring.
- **Replication factor** is configurable per keyspace.
- Replicas are spread across **availability zones** to tolerate rack/AZ failures.

---

## 3. Consistency Model — Quorum

Use tunable consistency via quorum reads/writes:

| Parameter | Meaning |
|---|---|
| N | Replication factor (e.g., 3) |
| W | Write quorum (e.g., 2) |
| R | Read quorum (e.g., 2) |

**W + R > N** guarantees strong consistency. For high availability, use W=1, R=1 (eventual). For read-heavy workloads, W=3, R=1.

Conflict resolution uses **vector clocks** to track causality. On conflict (concurrent writes), use last-write-wins (LWW) via timestamps, or surface to the application.

---

## 4. Storage Engine — LSM Tree

For write-heavy workloads, use a **Log-Structured Merge Tree (LSM)**:

```
Write Path:
  Client Write → WAL (durability) → MemTable (in-memory) 
      → Flush to SSTable on disk → Background Compaction

Read Path:
  MemTable → Bloom Filter check → SSTable (L0 → L1 → L2...)
```

- **WAL (Write-Ahead Log):** ensures durability before in-memory write
- **MemTable:** sorted in-memory structure (e.g., Red-Black Tree or Skiplist)
- **SSTables:** immutable sorted files on disk
- **Bloom filters:** probabilistic check to skip SSTables that don't contain a key (reduces I/O dramatically)
- **Compaction:** merges SSTables, removes tombstones (deletes), reclaims space

For read-heavy workloads, a **B-Tree** engine (like RocksDB or InnoDB) is an alternative.

---

## 5. Handling Failures

**Hinted Handoff:** If a replica node is temporarily down, the coordinator stores the write as a "hint" and forwards it when the node recovers.

**Anti-Entropy with Merkle Trees:** Each node maintains a Merkle tree of its data. Nodes periodically compare trees to detect and repair inconsistencies — only divergent subtrees need syncing, making it efficient.

**Gossip Protocol:** Nodes share membership state (who is up/down) in a peer-to-peer fashion every second. No single point of failure for cluster state.

---

## 6. Coordinator / Request Routing

Clients can talk to **any node** (all nodes are peers — no master). The receiving node acts as the **coordinator** for that request:

- Identifies the N replica nodes via the hash ring
- Sends parallel reads/writes to them
- Waits for W or R acknowledgments
- Returns response to client

---

## 7. Caching Layer

Add an in-memory cache (e.g., a local LRU cache per node, or a distributed layer like Redis) in front of the storage engine to serve hot keys with sub-millisecond latency.

---

## 8. Additional Features

**TTL / Expiry:** Store expiry timestamp alongside the value. A background thread scans and tombstones expired keys. SSTables handle deletion during compaction.

**Compression:** Compress SSTable blocks (Snappy/LZ4) to reduce storage footprint.

**Batching & Pipelining:** Clients can batch multiple puts/gets in one RPC to reduce network overhead.

---

## Data Flow Summary

```
PUT(k, v):
  1. Client → any node (coordinator)
  2. Coordinator hashes key → finds N replicas
  3. Writes to WAL
  4. Writes to MemTable
  5. Sends async replication to N-1 other replicas
  6. Returns success after W acks

GET(k):
  1. Client → coordinator
  2. Coordinator sends parallel reads to R replicas
  3. Returns value with latest vector clock
  4. Background read-repair if replicas disagree
```

---

## Trade-offs at a Glance

| Decision | Choice | Rationale |
|---|---|---|
| CAP | AP (availability + partition tolerance) | KV stores favor availability |
| Consistency | Tunable quorum | Flexible per use case |
| Storage engine | LSM Tree | Optimized for write-heavy workloads |
| Partitioning | Consistent hashing + vnodes | Minimal reshuffling on topology change |
| Failure detection | Gossip protocol | Decentralized, no SPOF |
| Conflict resolution | Vector clocks + LWW | Practical and widely adopted |

---

This design covers the core of systems like **DynamoDB, Cassandra, and Riak**. Happy to go deeper on any component — the storage engine, consistency semantics, compaction strategies, or a specific trade-off.


---

# Data Partitioning — Deep Dive

## The Core Problem

You have **more data than one machine can hold**. You need a strategy to decide: *"Which node is responsible for key K?"*

The strategy must be:
- **Deterministic** — any node can compute the answer independently
- **Balanced** — data spreads evenly across nodes
- **Minimal disruption** — adding/removing nodes shouldn't reshuffle everything

---

## Approach 1: Naive Modulo Hashing

```
node = hash(key) % N
```

Simple, but **fatally flawed** for distributed systems.

When N changes (node added/removed), almost every key maps to a different node. If you had 10 nodes and add 1, ~91% of keys need to move. This causes a **massive, thundering migration storm** — exactly what you don't want.

---

## Approach 2: Consistent Hashing

### The Ring

Map both **keys and nodes** onto the same circular hash space — a ring from `0` to `2^128 - 1` (using MD5 or SHA-1).

```
        0
      /    \
  Node A   Node B
    |         |
  Node D   Node C
      \    /
      2^128-1
```

A key is owned by the **first node clockwise** from its position on the ring.

```
hash("user:123") → position 450 → falls between Node A (400) and Node B (600) → Node B owns it
```

### What Changes When a Node Joins or Leaves?

**Node E joins** between Node B and Node C:
- Only keys that were between Node B and Node E's position migrate to Node E
- Everything else is completely unaffected
- **Only 1/N of keys move on average** — a massive improvement over modulo hashing

**Node B fails:**
- Its keys are now owned by Node C (next clockwise)
- All other nodes are unaffected

---

## Problem with Basic Consistent Hashing: Hotspots

With only N physical nodes on the ring, two problems emerge:

**1. Non-uniform distribution.** Node positions on the ring are determined by `hash(node_id)`, which is random. You could get unlucky and have nodes clustered together, leaving one node responsible for 40% of the ring and another for 5%.

**2. Heterogeneous hardware.** If you add a beefier node, it should handle more load — but basic consistent hashing gives every node an equal share regardless.

---

## Approach 3: Virtual Nodes (Vnodes) — The Real Solution

Instead of placing each physical node **once** on the ring, place it **many times** using multiple tokens.

```
Physical Node A → tokens at positions: 15, 87, 203, 445, 612, 789...  (150 positions)
Physical Node B → tokens at positions: 32, 110, 267, 398, 590, 801...  (150 positions)
Physical Node C → tokens at positions: 55, 145, 312, 501, 670, 900...  (150 positions)
```

The ring now looks like:

```
0---[A]--[B]--[C]--[A]--[C]--[B]--[A]--[B]--[C]--- 2^128
```

Each segment is a **vnode**, and a physical node owns many vnodes scattered across the ring.

### Why This Works

**Balanced distribution:** With 150 vnodes per node, the law of large numbers kicks in — each node ends up owning roughly `1/N` of the total key space, regardless of hash clustering.

**Heterogeneous hardware:** A powerful node gets 300 vnodes. A weaker node gets 75. Load scales proportionally with token count.

**Faster recovery:** When a node fails, its vnodes are spread across the ring, so **many nodes each take on a small slice** rather than one neighbor absorbing all the load.

**Smoother scaling:** Adding a new node — it takes a few vnodes from *every* existing node. Data migration is parallelized across the cluster.

### The Cost

You now need a **token metadata table** — each node must know which tokens map to which physical nodes. This is typically gossiped across the cluster and stored in a replicated metadata store.

---

## Replication with Partitioning

Once you know which node *owns* a key (the primary), you replicate to the next N-1 **distinct physical nodes** clockwise. The key word is *distinct* — you skip vnodes that belong to the same physical node.

```
Key K → primary is Node A (vnode at 450)
Next clockwise vnodes: Node A (skip, same physical), Node B, Node C
→ Replicas on: Node A, Node B, Node C  ✓
```

For **rack/AZ awareness**, you add a placement policy: replicas must land on nodes in different racks or availability zones. This means you skip vnodes until you reach a node in a new AZ.

---

## Partition Metadata & Routing

Every node stores a local copy of the **partition map** — a table of `(token_range → physical_node)`. This is how any node can act as a coordinator without a central router.

```
Token Range          →   Primary Node    AZ
[0, 850)             →   Node-A          us-east-1a
[850, 1700)          →   Node-D          us-east-1b
[1700, 2550)         →   Node-B          us-east-1c
...
```

When the cluster topology changes, this map is propagated via **gossip** — nodes exchange their view of the ring every second until convergence.

---

## Hotspot Problem — Beyond Partitioning

Even with great partitioning, **skewed access patterns** cause hotspots. A celebrity tweet, a viral product — one key gets hammered regardless of which node owns it.

Mitigations:

**Key salting / scatter:** Append a random suffix to the key (`tweet:123_0`, `tweet:123_1`...) and spread writes across multiple partitions. Reads must fan out and merge — trades write throughput for read complexity.

**Dedicated cache:** Hot keys get promoted to an in-memory cache layer (Redis, local LRU) that sits in front of the storage node, absorbing the read volume.

**Adaptive load shedding:** The storage node detects a hot key and temporarily replicates it to additional nodes beyond the normal replication factor.

---

## Summary

```
Naive Modulo          → Simple, catastrophic on rebalancing
Consistent Hashing    → Great on rebalancing, poor balance with few nodes
Consistent Hashing    → Solves balance + heterogeneity + 
  + Virtual Nodes        smooth scaling; standard in production
```

The vnode approach is what **Cassandra, DynamoDB, and Riak** all use in practice. The number of vnodes per node (Cassandra defaults to 256) is a tunable that trades routing table size against distribution quality.

---

Want to go deeper on **replication strategies**, **request routing**, or how **rebalancing actually works** when a node joins mid-traffic?

---

# Replication — Deep Dive

## Why Replication?

Replication serves three distinct goals — and the tension between them drives most design decisions:

- **Durability** — data survives node/disk failures
- **Availability** — reads and writes succeed even when nodes are down
- **Performance** — serve reads from the nearest replica, reducing latency

---

## The Fundamental Problem

You have N replicas of a key. A write comes in. You must decide:

1. **How many replicas must acknowledge before you tell the client "success"?**
2. **What do you do when replicas disagree on the value?**
3. **How do you recover a replica that fell behind?**

Every replication design is essentially an answer to these three questions.

---

## Replication Factor (N)

N is the number of copies of each key across the cluster. Stored in cluster metadata, configurable per keyspace.

```
N=1 → No redundancy. One node dies, data is gone.
N=2 → Survives 1 failure, but tight — no room for maintenance.
N=3 → Industry standard. Survives 1 failure comfortably, 2 failures with degraded consistency.
N=5 → Used for critical data. Higher storage cost.
```

Rule of thumb: **N=3 is the sweet spot** for most production systems. The storage cost (3x) is acceptable, and you can lose one node entirely without impacting availability.

---

## Replication Strategies

### Strategy 1: Single-Leader (Master-Slave)

One replica is the **leader** — all writes go through it. The leader propagates changes to **followers** asynchronously or synchronously.

```
Client Write
     ↓
  [ Leader ]  ──── sync/async ────→  [ Follower 1 ]
                                 └──────────────→  [ Follower 2 ]
```

**Reads** can go to followers (eventual consistency) or leader only (strong consistency).

**Pros:** Simple conflict resolution — leader is always authoritative. Easy to reason about ordering.

**Cons:** Leader is a **single point of bottleneck** for writes. Leader failure requires an election (downtime window). Not suitable for multi-datacenter writes.

Used by: **MySQL replication, PostgreSQL streaming replication, Redis (primary mode).**

---

### Strategy 2: Multi-Leader (Multi-Master)

Multiple nodes accept writes simultaneously. Each leader replicates to others.

```
DC-1: [ Leader A ] ←──────────────→ [ Leader B ] :DC-2
         ↓                                ↓
    Follower A1                      Follower B1
```

Primarily used for **multi-datacenter deployments** — each DC has its own leader, accepting local writes with low latency. Cross-DC replication happens asynchronously.

**The hard problem:** Two leaders can accept conflicting writes to the same key concurrently. You **must** have a conflict resolution strategy.

Common strategies:
- **Last Write Wins (LWW):** Keep the write with the higher timestamp. Simple, but clocks aren't perfectly synchronized — you can lose a write silently.
- **Application-level merge:** Surface the conflict to the application and let it decide (e.g., shopping carts merge item sets).
- **CRDTs (Conflict-free Replicated Data Types):** Mathematically designed data structures that always merge deterministically. Great for counters, sets, flags.

Used by: **CouchDB, some DynamoDB configurations, cross-region setups.**

---

### Strategy 3: Leaderless Replication — The Dynamo Model

**No leader at all.** Any node can accept any write. Clients (or a coordinator) send writes to multiple replicas directly and use **quorum** to determine success.

```
Client Write
     ↓
Coordinator
  ├──→ Node A  ✓
  ├──→ Node B  ✓
  └──→ Node C  ✗ (slow/down)

W=2 achieved → return success to client
```

This is the model used by **Cassandra, DynamoDB, and Riak** — the most common model for large-scale KV stores. We'll go deep on this.

---

## Quorum in Depth

With N replicas, define:

- **W** = number of replicas that must confirm a write
- **R** = number of replicas that must respond to a read

The magic rule: **W + R > N** guarantees at least one node in every read set overlaps with every write set — ensuring you always read the latest write.

```
N=3, W=2, R=2 → W+R=4 > 3 ✓  Strong consistency
N=3, W=1, R=1 → W+R=2 < 3 ✗  Eventual consistency (but very fast)
N=3, W=3, R=1 → W+R=4 > 3 ✓  Strong consistency, fast reads, slow writes
N=3, W=1, R=3 → W+R=4 > 3 ✓  Strong consistency, fast writes, slow reads
```

### The Latency vs. Consistency Trade-off

```
Higher W → More write durability, higher write latency
Higher R → More read freshness, higher read latency
Lower W,R → Lower latency, risk of stale reads
```

In practice, most systems expose this as a **consistency level per-request**, not a global setting. A financial transaction uses W=3, R=3. A social media "like count" uses W=1, R=1.

### Sloppy Quorum & Hinted Handoff

What if the nodes that *should* own a key are all down? A **strict quorum** would fail the write.

A **sloppy quorum** says: write to any W available nodes, even if they're not the natural replicas. Store a **hint** — "this data belongs to Node B, deliver it when Node B comes back."

```
Node B is down.
Write goes to Node E (a "bystander") with hint: {intended_for: Node B}
Node B recovers → Node E delivers the hinted write → Node E discards its copy
```

This dramatically improves **write availability** at the cost of temporary inconsistency. DynamoDB and Cassandra both use this.

---

## Conflict Resolution — The Hard Part

In leaderless systems, two clients can write to the same key simultaneously on different replicas. Now you have divergent versions. How do you resolve them?

### Vector Clocks

A vector clock is a map of `{node_id → sequence_number}` that tracks the **causal history** of a value.

```
Initial:   key="x", value="A", vclock={Node1: 1}

Client 1 reads from Node1, gets {Node1:1}, updates → writes "B" to Node1 and Node2
           key="x", value="B", vclock={Node1: 2}

Client 2 (concurrently) reads from Node3 (stale), gets {Node1:1}, updates → writes "C" to Node3
           key="x", value="C", vclock={Node1: 1, Node3: 1}

Now Node2 has "B" {Node1:2} and Node3 has "C" {Node1:1, Node3:1}
→ Neither descends from the other → TRUE CONFLICT
→ Must be resolved
```

Vector clocks let you distinguish between:
- **Causal ordering** (one write happened-after another → safe to discard the older one)
- **True concurrency** (writes happened independently → genuine conflict, needs resolution)

### Last Write Wins (LWW)

Simpler but lossy. Attach a timestamp to every write. On conflict, the higher timestamp wins.

**The problem:** Clocks drift. NTP synchronization has millisecond-level error. Two concurrent writes can have the same timestamp. **Silent data loss is possible** — a write succeeds from the client's perspective but gets discarded.

Cassandra uses LWW by default. DynamoDB also uses it. It's acceptable when **losing occasional writes is tolerable** (e.g., user profile updates).

### CRDTs

Mathematically designed data structures that can always be merged without conflicts:

- **G-Counter:** Grow-only counter. Each node tracks its own increment. Total = sum of all. Always mergeable.
- **PN-Counter:** Positive-negative counter. Two G-counters, one for increments, one for decrements.
- **OR-Set (Observed-Remove Set):** A set where concurrent add and remove of the same element resolves deterministically.
- **LWW-Register:** Single value with timestamp — basically LWW formalized.

CRDTs are the gold standard for **eventually consistent data structures** because they guarantee convergence without coordination. Used heavily in collaborative apps (Google Docs-style editing).

---

## Replication Lag & Read-Your-Writes

In async replication, a follower can lag behind the leader by seconds or more. This creates subtle problems:

**Problem 1 — Read-your-writes:** User writes a post, immediately reads it — but hits a lagging replica that doesn't have the write yet. Appears as if the write was lost.

*Solution:* After a write, read from the **same node you wrote to** for a short window (e.g., 1 minute). Or track the write timestamp and only read from replicas that have caught up to at least that timestamp.

**Problem 2 — Monotonic reads:** User makes two reads. First hits an up-to-date replica, second hits a stale one — appears to go backward in time.

*Solution:* Route each user's reads to the **same replica** consistently (e.g., hash user ID to a replica).

**Problem 3 — Consistent prefix reads:** User reads a sequence of writes but sees them out of order (e.g., sees a reply before the original message).

*Solution:* Causally related writes go to the **same partition**, ensuring they're replicated in order.

---

## Replica Repair — Keeping Replicas in Sync

Even with quorum, replicas drift over time due to failures, network partitions, and hinted handoffs. Two mechanisms bring them back in sync:

### Read Repair

When a coordinator reads from R replicas and notices they return different values, it immediately **writes the latest value back** to the stale replicas.

```
Read from Node A: value="B", vclock={Node1:2}  ← newer
Read from Node B: value="A", vclock={Node1:1}  ← stale

Coordinator → writes "B" back to Node B (background)
```

This is **reactive** — repairs only happen on keys that are read. Cold keys that are never read can drift indefinitely.

### Anti-Entropy with Merkle Trees

A background process proactively compares replicas and repairs divergence without reading every key.

Each node builds a **Merkle tree** over its key space:
- Leaf nodes = hash of individual key-value pairs
- Internal nodes = hash of children's hashes
- Root = a single hash representing the entire dataset

```
         [Root Hash]
        /            \
  [Hash L]          [Hash R]
   /     \           /     \
[h1]   [h2]       [h3]   [h4]
 k1,v1  k2,v2     k3,v3  k4,v4
```

To compare two replicas: **exchange root hashes first**. If they match — done, no repair needed. If they differ, walk down the tree, comparing child hashes until you find exactly which leaf (key) diverges. Only those keys need to be exchanged.

This makes anti-entropy **extremely efficient** — you don't need to compare every key pair directly. Cassandra runs this as a periodic `nodetool repair`.

---

## Multi-Datacenter Replication

For geographic distribution, replication crosses datacenter boundaries:

```
        DC-1 (us-east)                    DC-2 (eu-west)
  ┌─────────────────────┐         ┌─────────────────────┐
  │  Node A  Node B  Node C  ───→  Node D  Node E  Node F │
  └─────────────────────┘         └─────────────────────┘
       Local quorum W=2                Local quorum W=2
            ↑
         Client write returns here (low latency)
         Cross-DC replication is async
```

**Local quorum** within a DC satisfies the write immediately. Cross-DC replication happens asynchronously in the background, keeping the remote DC eventually consistent.

This means a DC can fail completely and the other continues operating — no data loss for locally-acknowledged writes, possible loss of in-flight cross-DC replication.

---

## Summary

```
Single-Leader    → Simple, one write bottleneck, common for SQL DBs
Multi-Leader     → Multi-DC writes, complex conflict resolution
Leaderless       → Highest availability, tunable consistency via quorum

Conflict Resolution:
  LWW            → Simple, possible silent data loss
  Vector Clocks  → Accurate causality, complex
  CRDTs          → Convergence guaranteed, limited data types

Replica Repair:
  Read Repair    → Reactive, only fixes hot keys
  Merkle Trees   → Proactive, efficient, catches all divergence
```

---

Want to go deeper on **consistency models (eventual vs. strong vs. causal)**, **leader election**, or how **quorum interacts with failures** in real failure scenarios?

---
# Failure Detection & Handling — Deep Dive

## The Core Challenge

In a distributed system, you can never *know* with certainty that a node has failed. A node that's slow looks identical to a node that's dead from the outside. A network partition makes a perfectly healthy node appear gone. Every failure detection system is really answering: **"Has this node failed, or is it just slow?"**

The answer determines everything — declare failure too eagerly and you trigger unnecessary recovery work; too conservatively and you serve stale or unavailable data.

---

## Part 1: Failure Detection

### Naive Approach — Heartbeats with Fixed Timeout

Every node sends a heartbeat every T seconds. If a node misses K consecutive heartbeats, it's declared dead.

```
Node A → heartbeat every 1s → Node B
Node B: "Haven't heard from A in 5s → A is dead"
```

**The problem:** Fixed timeouts are fundamentally fragile. In a network with variable latency (GC pauses, TCP retransmission, congestion), a healthy node can easily miss a heartbeat window. You get **false positives** — triggering expensive recovery for a node that comes back 2 seconds later. Increase the timeout to reduce false positives and you get **slow failure detection** — your system is unavailable for longer during real failures.

---

### Better: The Phi Accrual Failure Detector

Used by **Cassandra and Akka**. Instead of a binary "alive/dead" decision, it outputs a continuous value **φ (phi)** representing the *suspicion level* of a node.

The key insight: rather than a fixed timeout, model the **distribution of inter-arrival times** of heartbeats from that node. If heartbeats normally arrive every 1s ± 0.1s, a 3s gap is very suspicious. If they normally arrive every 1s ± 0.5s (high jitter), a 3s gap is less alarming.

```
φ = -log₁₀(probability that heartbeat is just late given historical pattern)

φ < 1   → Very likely alive
φ = 1   → 10% chance it's failed
φ = 2   → 1% chance it's failed  
φ = 8   → 99.999999% chance it's failed → declare dead
```

The threshold φ is **tunable per environment**:
- Low-latency datacenter network → φ threshold = 5
- Cross-DC with high jitter → φ threshold = 10

This eliminates the binary decision. The system can start **routing around a suspicious node** before fully declaring it dead — a graceful degradation rather than a hard cutover.

---

### Gossip-Based Membership — Detecting Failures at Scale

Direct heartbeating doesn't scale. If every node heartbeats every other node, that's O(N²) messages. With 1000 nodes heartbeating every second, you have 1,000,000 messages/second just for failure detection.

The solution: **gossip protocol** (epidemic protocol).

```
Every T seconds, each node:
  1. Increments its own heartbeat counter
  2. Picks K random nodes (K=3 typically)
  3. Sends its full membership table to those nodes
  4. Receives their tables, merges by taking max heartbeat counter per node
```

```
Node A's membership table:
  Node A: counter=145, timestamp=T
  Node B: counter=203, timestamp=T
  Node C: counter=98,  timestamp=T-15  ← hasn't been updated in 15s → suspicious
  Node D: counter=312, timestamp=T
```

Each entry has a heartbeat counter and the local timestamp of the last update. If a node's counter hasn't increased in `failure_timeout` seconds, it's marked suspect, then dead.

**Convergence is fast:** Information about a failure propagates to all N nodes in `O(log N)` gossip rounds. With 1000 nodes and 1-second gossip intervals, the whole cluster knows about a failure in ~10 seconds.

**Messages scale as O(N log N)** — each node sends K messages, each containing the full table, but the table is small and gossip is periodic.

---

### SWIM Protocol — A More Efficient Alternative

Used by **HashiCorp Consul, memberlist**. Separates failure detection from membership dissemination.

Instead of gossip heartbeating, SWIM uses **indirect probing:**

```
Step 1: Node A sends direct ping to Node C
Step 2: Node C doesn't respond within timeout
Step 3: Node A asks K other nodes to ping C on its behalf (indirect ping)
         → These nodes probe C from different network paths
Step 4: If none get a response → C is declared suspect
Step 5: After a suspect timeout with no rebuttal from C → C declared dead
```

The indirect probe step is the key innovation — it distinguishes between **"Node A can't reach C"** (network partition between A and C) and **"Nobody can reach C"** (C is actually dead). This dramatically reduces false positives from partial network partitions.

---

## Part 2: Handling Temporary Failures

Temporary failures are: node restarts, short network partitions, GC pauses, slow disks — the node comes back, and you need to reconcile what it missed.

### Hinted Handoff

When a write's target replica is down, a **coordinator node** accepts the write on its behalf and stores it with a **hint** — metadata saying "this write belongs to Node B."

```
Normal write (Node B healthy):
  Coordinator → Node A ✓
             → Node B ✓  
             → Node C ✓

Node B is temporarily down:
  Coordinator → Node A ✓
             → Node B ✗ (down)
             → Node E ✓ (stores write + hint: {target: Node B, key: k, value: v})
```

When Node B recovers, Node E detects it (via gossip) and **delivers the hinted writes** to B, then discards its local copy.

**Important limits:**
- Hints are stored in a separate local hints file/table, not in the main data path
- Hints expire after a configurable window (e.g., 3 hours). If Node B is down longer than that, hints are discarded — you now need full repair
- Hinted handoff maintains **write availability** during temporary outages but does **not count toward quorum** in strict consistency modes — it's a best-effort durability mechanism

**The problem with hinted handoff:** The hint-holding node can itself fail before delivering hints. This is why it's paired with anti-entropy repair as a safety net.

---

### Read Repair

When a read coordinator queries R replicas and gets back different values, it:

1. Returns the **most recent value** to the client immediately
2. In the background (or synchronously in strong consistency mode), writes the latest value back to stale replicas

```
Read quorum R=2 from 3 replicas:
  Node A → value="B", vclock={n1:2}  ← newer
  Node B → value="A", vclock={n1:1}  ← stale
  Node C → (not queried this time)

Coordinator returns "B" to client
Background: writes "B" to Node B → Node B is repaired
```

**Limitation:** Read repair only fixes keys that are actively read. Cold keys that are never accessed can remain divergent indefinitely. This is fine for caches but not for durable storage — which is why anti-entropy is also needed.

---

### Handling GC Pauses — A Subtle Temporary Failure

JVM-based systems (Cassandra, HBase) suffer **stop-the-world GC pauses** that can last hundreds of milliseconds to several seconds. During a pause, the node doesn't respond — looks like a failure to everyone else.

Mitigations:
- **G1/ZGC collectors** dramatically reduce pause times (< 10ms typically)
- **Lease-based leadership:** A leader holds a lease for T seconds. Other nodes won't take over until the lease expires — giving the GC-paused node time to resume
- **Coordinator timeout tuning:** Set read/write timeouts longer than worst-case GC pause to avoid false failure escalation

---

## Part 3: Handling Permanent Failures

Permanent failures are: dead disks, hardware failures, decommissioned nodes. The node is not coming back (or comes back with an empty disk).

### Step 1 — Detection & Marking Dead

Once the failure detector (φ threshold or SWIM suspect timeout) marks a node as permanently dead, the cluster must:

1. Update the gossip membership table — mark node as removed
2. Notify all nodes to update their partition maps
3. Redistribute responsibility for its token ranges to remaining nodes

The partition map now has gaps — the dead node's vnodes need new owners.

---

### Step 2 — Data Re-Replication

The dead node owned some token ranges. The data that was on it exists on its other replicas (because replication factor N=3, so two other copies exist). But now you're **under-replicated** — you have N-1 copies instead of N.

You need to bring replication back to N by copying data to a new replica.

```
Before Node B failure (N=3):
  Key K → Node A ✓, Node B ✓, Node C ✓

After Node B failure:
  Key K → Node A ✓, Node B ✗, Node C ✓  ← only 2 copies, under-replicated

Re-replication:
  Key K → Node A ✓, Node C ✓, Node D ✓  ← Node D takes over Node B's ranges
```

This re-replication is **throttled** — you don't want recovery traffic to saturate production I/O. Cassandra uses a streaming rate limiter (default 200 MB/s) to balance recovery speed against impact to live traffic.

---

### Step 3 — Adding a Replacement Node

When you add Node X to replace the dead Node B:

```
1. Node X bootstraps → announces itself to the cluster via gossip
2. Cluster assigns it Node B's token ranges (or a new set of vnodes)
3. Node X streams data from existing replicas for those ranges
4. Node X enters a "joining" state — reads are served by old replicas
5. Once streaming is complete → Node X transitions to "normal" state
6. Partition map updated cluster-wide via gossip
```

The **streaming phase** is the expensive part — potentially terabytes of data. During this window, the system operates with reduced redundancy. Most operators monitor `pending ranges` as a health metric during node replacement.

---

### Full Repair with Merkle Trees

After a node comes back from a long outage (or a new node finishes streaming), you can't be sure it has 100% of its data. You run a **full anti-entropy repair** using Merkle trees.

```
Repair between Node A and Node D for a token range:

1. Both nodes build a Merkle tree over the key range
2. Exchange root hashes
3. If roots match → done (ranges are identical)
4. If roots differ → exchange child hashes recursively
5. Find the differing leaf nodes → only those keys need syncing
6. Sync just the divergent keys
```

This is far more efficient than a full key scan. A range with 100M keys where only 1000 differ will converge by exchanging ~1000 key-value pairs, not 100M.

**The operational gotcha:** Full repair is expensive even with Merkle trees — building the tree requires reading all SSTables. In Cassandra, `nodetool repair` is a scheduled maintenance operation, typically run weekly. Running it too frequently impacts production I/O; running it too rarely risks long-term divergence.

---

## Part 4: Network Partitions — The Hardest Case

A network partition is when nodes are alive and healthy but **can't communicate with each other**. This is the "P" in CAP theorem and the hardest failure mode to handle.

```
       PARTITION
          |
DC-1: [A, B, C]  |  [D, E, F] :DC-2
          |
    (cannot communicate)
```

Both sides think the other side is dead. Both sides can still accept writes. Now you have **split-brain** — two independent clusters diverging.

### Option 1 — Prioritize Availability (AP)

Both partitions keep accepting reads and writes. When the partition heals, you have conflicting versions that must be resolved via vector clocks, LWW, or CRDTs.

This is the **Dynamo/Cassandra choice.** You never reject a write, but you might have to reconcile conflicts later. Data loss is possible (LWW) but the system stays up.

### Option 2 — Prioritize Consistency (CP)

The minority partition stops accepting writes. Only the majority partition (> N/2 nodes) continues operating.

```
N=5 nodes. Partition: 3 nodes vs 2 nodes.
Majority side (3 nodes) → continues operating
Minority side (2 nodes) → rejects writes, returns error to clients
```

This requires a **leader election** on the majority side (Raft or Paxos) to confirm it's the authoritative partition. No conflicting writes, but the minority side is unavailable for the partition duration.

This is the **ZooKeeper/etcd/HBase choice.**

### Handling Partition Heal

When the partition heals, nodes reconnect and exchange gossip. The system must:

1. Detect that nodes have reconnected (gossip counters resume advancing)
2. Identify divergent data via Merkle tree comparison
3. Apply conflict resolution and repair

---

## Summary — Decision Tree

```
Failure Detected?
        |
   How long down?
   /              \
Short              Long (> hint expiry)
(temporary)         (permanent)
   |                    |
Hinted Handoff     Re-replication to
+ Read Repair      new/replacement node
+ Anti-entropy     + Full Merkle repair

Network Partition?
        |
  CAP choice made at design time
  /              \
AP (Dynamo)     CP (Raft/Paxos)
Accept writes   Reject minority
both sides      partition writes
Reconcile on    No conflicts,
heal            but unavailable
```

---

## Key Operational Metrics to Monitor

| Metric | What it signals |
|---|---|
| Gossip convergence time | How fast failures propagate |
| Pending ranges | Under-replicated data, node in recovery |
| Hint queue depth | How much catch-up a recovering node needs |
| Repair progress | Anti-entropy health |
| φ score distribution | Whether nodes are close to failure threshold |

---

Want to go deeper on **Raft/Paxos for leader election**, **CRDTs for conflict-free resolution**, or the **read/write path under failure scenarios** with concrete quorum examples?

---
# Raft, Paxos, CRDTs & Quorum Failure Scenarios — Deep Dive

---

# Part 1: Paxos & Raft for Leader Election

## Why Do We Need Leader Election?

In a CP system, you need **one authoritative node** to coordinate writes. Without a single leader, two nodes can simultaneously accept conflicting writes believing they're in charge — split-brain. Leader election is the mechanism that ensures the cluster agrees on exactly one leader at any point, even during failures.

The core challenge: nodes can fail mid-election, messages can be lost, and the network can partition. The algorithm must guarantee **safety** (never two leaders at once) even if it temporarily sacrifices **liveness** (the system might stall briefly during election).

---

## Paxos — The Original Algorithm

Paxos guarantees consensus among N nodes despite failures. It's notoriously hard to understand and even harder to implement correctly — Leslie Lamport's original paper was famously rejected twice.

### The Roles

- **Proposer** — initiates the consensus round, proposes a value
- **Acceptor** — votes on proposals, stores accepted values
- **Learner** — learns the final agreed value

In practice, every node plays all three roles.

### Phase 1 — Prepare (Leader Election)

A node that wants to become leader sends a **Prepare(n)** message to a majority of nodes, where `n` is a unique, monotonically increasing proposal number (ballot number).

```
Candidate Node A picks n=5 (higher than any it's seen before)
A → Prepare(n=5) → [Node B, Node C, Node D, Node E]
```

Each acceptor responds with **Promise(n)** if `n` is the highest proposal number it has seen. It promises never to accept proposals with a lower number. It also returns the **highest value it has already accepted**, if any.

```
Node B → Promise(n=5, accepted_value=null)   ✓
Node C → Promise(n=5, accepted_value=null)   ✓
Node D → Promise(n=5, accepted_value=null)   ✓
Node E → (no response — down)

Majority (3/5) promised → Phase 1 succeeds
```

If a node has already seen a higher proposal number (e.g., from another concurrent candidate), it rejects:

```
Node B already promised n=7 to Node F
Node B → Reject(n=5, saw_higher=7) to Node A
Node A → must restart with n > 7
```

### Phase 2 — Accept (Commit)

Node A now has a majority of promises. It sends **Accept(n=5, value=A)** — proposing itself as leader. If any acceptor returned a previously accepted value in Phase 1, Paxos **forces A to propose that value instead** (this is the key safety mechanism — it prevents overwriting an already-committed decision).

```
Node A → Accept(n=5, value="A is leader") → [B, C, D]

Node B → Accepted(n=5) ✓
Node C → Accepted(n=5) ✓
Node D → Accepted(n=5) ✓

Majority accepted → "A is leader" is committed
Learners are notified → all nodes know A is leader
```

### The Fundamental Problem with Paxos — Dueling Proposers

```
Node A: Prepare(n=5) → majority promises
Node B (concurrent): Prepare(n=6) → majority promises (preempts A)
Node A: sees rejection, retries with n=7 → preempts B
Node B: retries with n=8 → preempts A
...
```

Two nodes can livelock forever, each preempting the other before committing. This is the **dueling proposers problem.** Multi-Paxos solves this by electing a **distinguished proposer** who handles all proposals, but this essentially re-introduces a leader — which is what you were trying to elect in the first place. The recursiveness is why Paxos is hard to engineer into a complete system.

---

## Raft — Paxos Made Understandable

Raft was explicitly designed to be **easier to understand and implement** than Paxos, while providing the same safety guarantees. It's the algorithm behind etcd, CockroachDB, TiKV, and Consul.

Raft decomposes consensus into three sub-problems: **leader election**, **log replication**, and **safety**.

### Terms — The Logical Clock

Raft divides time into **terms** — monotonically increasing integers. Each term begins with an election. If a leader is elected, it leads for the rest of that term. If the election fails (split vote), a new term begins immediately.

```
Term 1: Node A elected leader
Term 2: Node A crashes → election → Node B elected leader
Term 3: Network partition → split vote → no leader elected → Term 4
Term 4: Node C elected leader
```

Terms are the fundamental mechanism for detecting stale leaders. If Node A comes back from a crash still thinking it's the leader of Term 1, any node will reject its messages because they're operating in Term 4.

### Leader Election — Step by Step

**Initial State:** All nodes start as **Followers**. Every follower has an election timeout — a random timer between 150ms and 300ms (randomization is key to avoiding split votes).

**Step 1 — Timeout:** A follower's timer fires (no heartbeat from leader received). It transitions to **Candidate**, increments its term counter, votes for itself, and sends **RequestVote(term, candidateId, lastLogIndex, lastLogTerm)** to all other nodes.

```
Node C's timer fires first (it had the shortest timeout — say 152ms)
Node C: term=4, state=Candidate
Node C → RequestVote(term=4, candidate=C, lastLogIndex=42, lastLogTerm=3)
         → [Node A, Node B, Node D, Node E]
```

**Step 2 — Voting:** Each node grants a vote to the first candidate it hears from in a term, subject to one critical condition: **the candidate's log must be at least as up-to-date as the voter's log.** This prevents a node with a stale log from becoming leader and overwriting committed entries.

```
Log up-to-date check:
  If candidate's lastLogTerm > voter's lastLogTerm → candidate wins
  If equal terms → candidate with longer log wins
  Otherwise → reject
```

```
Node A → grants vote to C (C's log is up-to-date) ✓
Node B → grants vote to C ✓
Node D → grants vote to C ✓
Node E → (down, no response)

Node C has 4 votes (including its own) out of 5 → majority → Node C becomes Leader
```

**Step 3 — Heartbeats:** Node C immediately starts sending **AppendEntries(heartbeat)** to all followers to assert leadership and reset their election timers. As long as heartbeats arrive, followers stay passive.

### What If There's a Split Vote?

Random timeouts make this rare, but it can happen:

```
5 nodes, two time out almost simultaneously:
Node B (155ms): RequestVote(term=4) 
Node D (158ms): RequestVote(term=4)

Node A → votes for B (heard B first)
Node E → votes for D (heard D first)
Node C → votes for B

Node B: 3 votes → wins
Node D: 2 votes → loses, reverts to follower, resets timer
```

If it were a true tie (2-2 with 4 nodes, 1 missing):

```
Both B and D get 2 votes → neither reaches majority → election fails
Both increment term to 5 and start new election with new random timeouts
The randomness virtually guarantees one fires first in term 5
```

### Log Replication & Safety

Once a leader is elected, it handles all writes by appending them to its **log** and replicating to followers via **AppendEntries** RPC:

```
Client → write("x=5") → Leader C
Leader C: appends {term=4, index=43, cmd="x=5"} to its log
Leader C → AppendEntries(entries=[{43, "x=5"}]) → [A, B, D, E]

Majority ack (3/5) → entry committed
Leader C → applies "x=5" to state machine → responds to client
Leader C → notifies followers of commit index → they apply too
```

**Safety Guarantee:** A leader will never overwrite a committed entry. If a candidate's log is missing committed entries, it cannot win the election (the log up-to-date check). This ensures committed data is never lost across leader transitions.

### Raft vs Paxos — The Practical Difference

Paxos is a single-decree consensus algorithm — it decides one value. Building a replicated log (multi-decree Paxos) requires significant additional engineering that Paxos doesn't specify. Raft is built from the ground up as a **replicated log protocol**, making it directly implementable.

```
Paxos: Theoretically minimal, notoriously hard to extend to full systems
Raft:  Slightly more prescriptive, much easier to implement correctly
       Handles log replication, membership changes, and snapshots natively
```

---

# Part 2: CRDTs — Conflict-Free Replicated Data Types

## The Core Idea

In an AP system (Dynamo-style), you allow concurrent writes to the same key on different replicas. Eventually those replicas must merge. The question is: can you design your data structures so that **merging is always deterministic and correct** — no matter what order updates arrive in, no matter how many times you apply an update?

CRDTs guarantee this mathematically. They rely on one key property: the merge function must form a **join-semilattice** — it must be commutative (order doesn't matter), associative (grouping doesn't matter), and idempotent (applying the same update twice gives the same result as applying it once).

```
merge(A, B) = merge(B, A)           ← commutative
merge(merge(A,B), C) = merge(A, merge(B,C))  ← associative
merge(A, A) = A                     ← idempotent
```

If your data structure satisfies these, **eventual consistency is guaranteed** — all replicas will converge to the same value regardless of network delays, reorderings, or partitions.

---

## CRDT Type 1: G-Counter (Grow-Only Counter)

**Problem:** Distributed like counter. Two users simultaneously like a post on different replicas. LWW would drop one like. How do you count correctly?

**Design:** Each node maintains its own counter slot. The total is the sum of all slots.

```
State on each node: vector of per-node counts
{Node-A: 0, Node-B: 0, Node-C: 0}

User on Node A likes the post:
  Node A state: {Node-A: 1, Node-B: 0, Node-C: 0}

Concurrently, user on Node C likes the post:
  Node C state: {Node-A: 0, Node-B: 0, Node-C: 1}

Merge (take element-wise max):
  Merged: {Node-A: 1, Node-B: 0, Node-C: 1}
  Total = 1 + 0 + 1 = 2  ✓ Both likes counted
```

**Merge function:** `max(a[i], b[i])` per slot. Always idempotent — merging the same update twice gives the same result because `max(1,1) = 1`.

**Limitation:** Counters can only grow. You can't decrement.

---

## CRDT Type 2: PN-Counter (Positive-Negative Counter)

**Problem:** You need both increments and decrements — e.g., inventory count, balance.

**Design:** Two G-Counters, one for increments (P) and one for decrements (N). Value = sum(P) - sum(N).

```
Initial: P={A:0, B:0}, N={A:0, B:0}, value=0

Node A: user adds 3 items to cart
  P={A:3, B:0}, N={A:0, B:0}, value=3

Node B concurrently: user removes 1 item
  P={A:0, B:0}, N={A:0, B:1}, value=-1

Merge:
  P={A:3, B:0}  (element-wise max)
  N={A:0, B:1}  (element-wise max)
  value = (3+0) - (0+1) = 2  ✓
```

The key insight: **you never actually subtract**. You track decrements separately as their own growing counter. Subtraction happens only at read time.

---

## CRDT Type 3: LWW-Register (Last Write Wins Register)

**Problem:** A single mutable value (e.g., user's display name). You want the latest write to win but need this to be deterministic across replicas.

**Design:** Store the value alongside a timestamp (or Lamport clock). Merge always picks the higher timestamp.

```
Node A: name="Alice", ts=100
Node B: name="Alicia", ts=105  ← concurrent update

Merge: pick higher ts → "Alicia", ts=105  ✓ Deterministic
```

**The danger:** Clocks can drift. If timestamps are wall-clock time, Node A's clock being 10ms ahead can silently overwrite a genuinely newer write from Node B. For correctness, use **Lamport clocks or hybrid logical clocks (HLC)** instead of wall time.

---

## CRDT Type 4: OR-Set (Observed-Remove Set)

**Problem:** A set where elements can be added and removed. The classic problem:

```
Concurrent operations:
  Node A: remove("apple")
  Node B: add("apple")

What should the final state be?
```

Naïve sets can't resolve this deterministically. The OR-Set (Observed-Remove Set) solves it elegantly.

**Design:** Every `add` operation tags the element with a **unique token** (UUID). A `remove` operation removes only the specific tokens it has observed. New concurrent adds have new tokens and survive.

```
Initial: {}

Node A adds "apple" with token t1:
  Node A: {(apple, t1)}

Node A removes "apple" — removes all tokens it knows about:
  Node A: {}  removes {t1}

Concurrently, Node B adds "apple" with token t2:
  Node B: {(apple, t2)}

Merge:
  "apple" with t1 → removed (t1 was in remove set)
  "apple" with t2 → NOT removed (t2 was not in Node A's remove set when it issued remove)
  Final: {(apple, t2)} → apple is present ✓
```

**Add-wins semantics:** concurrent add and remove → add wins. This is usually the right behavior for shopping carts (don't lose items), collaborative editing, etc. Remove-wins variants exist for other semantics.

---

## CRDT Type 5: RGA (Replicated Growable Array) — for Collaborative Text

**Problem:** Google Docs-style concurrent text editing. Two users insert characters at the same position simultaneously.

**Design:** Each character gets a globally unique identifier (based on node ID + Lamport timestamp). Characters are ordered by their identifiers, not absolute positions. Insertions use the identifier of the character to their left as an anchor.

```
Document: "AC"
           [id1:A][id3:C]

Node X inserts "B" after id1:A at ts=5:
  Insert(id5:B, after=id1)
  Document: [id1:A][id5:B][id3:C] → "ABC"

Concurrently, Node Y inserts "X" after id1:A at ts=5:
  Insert(id6:X, after=id1)
  
Merge — both inserted after id1. Tie-break by id (id6 > id5):
  Document: [id1:A][id6:X][id5:B][id3:C] → "AXBC"
  
Both nodes converge to "AXBC" — deterministic, no conflict ✓
```

This is the foundation of **operational transformation** alternatives used in modern collaborative editors. Automerge and Y.js implement RGA-based CRDTs for real-time collaboration.

---

## When Not to Use CRDTs

CRDTs have costs. The state grows unboundedly (tombstones for removes, tokens for OR-Set). You need **garbage collection** to prune obsolete metadata, which requires coordination — a form of the very thing CRDTs were trying to avoid. They're excellent for specific data types but shouldn't be applied universally.

```
Good fit:         Counters, sets, flags, text, key-value maps
Poor fit:         Financial transactions (need serializability)
                  Unique constraints (e.g., unique username registration)
                  Complex relational invariants
```

---

# Part 3: Read/Write Path Under Failure — Concrete Quorum Examples

Setup for all scenarios: **N=3, W=2, R=2**, three replicas for every key: **Node A, Node B, Node C**.

---

## Scenario 1: Normal Operation (No Failures)

```
PUT(key="user:1", value="Alice")

Coordinator receives write:
  → Sends write to Node A, Node B, Node C in parallel

Timeline:
  t=0ms:  Coordinator sends to A, B, C
  t=2ms:  Node A acks ✓
  t=3ms:  Node B acks ✓  ← W=2 reached → return success to client
  t=8ms:  Node C acks ✓  (arrives late but still applied)

Client receives: SUCCESS at t=3ms

GET(key="user:1")

Coordinator:
  → Sends read to Node A, Node B in parallel (only needs R=2)
  t=0ms:  Read sent
  t=2ms:  Node A → "Alice", vclock={A:1}
  t=3ms:  Node B → "Alice", vclock={A:1}  ← R=2 reached, values agree
  
Client receives: "Alice" at t=3ms
```

---

## Scenario 2: One Replica Down During Write

```
Node C is DOWN. N=3, W=2, R=2.

PUT(key="user:1", value="Bob")

  t=0ms:  Coordinator sends to Node A, Node B, Node C
  t=2ms:  Node A acks ✓
  t=3ms:  Node B acks ✓  ← W=2 reached → SUCCESS
  t=timeout: Node C — no response (down)

Coordinator stores hint: {target: Node C, key: "user:1", value: "Bob"}

Client receives: SUCCESS ✓

State:
  Node A: "Bob"  ✓
  Node B: "Bob"  ✓
  Node C: "???"  (stale or missing)  ← under-replicated
```

Now a read:

```
GET(key="user:1")  [Node C still down]

Coordinator sends to Node A, Node B (C is known down, skipped)
  Node A → "Bob" ✓
  Node B → "Bob" ✓  ← R=2 reached, consistent

Client receives: "Bob" ✓ — Correct, system degraded but functional
```

When Node C recovers: coordinator delivers hint, Node C is repaired.

---

## Scenario 3: One Replica Down During Read — Stale Data Risk

```
Sequence of events:
  1. PUT("user:1", "Alice") → W=2, written to A and B. C missed it (was slow).
  2. Node A goes DOWN.
  3. GET("user:1") arrives.

State:
  Node A: "Alice" ← DOWN
  Node B: "Alice" ✓
  Node C: [old value or missing] ← missed the write

GET attempts R=2 replicas:
  Coordinator sends to Node B and Node C (A is down)
  Node B → "Alice", vclock={coordinator:1}
  Node C → null or stale value

Coordinator sees disagreement!
  → Returns the value with the highest vclock: "Alice"
  → Triggers read repair: writes "Alice" to Node C in background

Client receives: "Alice" ✓
Node C repaired to "Alice" ✓
```

---

## Scenario 4: Network Partition — The Dangerous Case

```
N=3, W=2, R=2
Partition occurs: [Node A, Node B] | [Node C]

Client 1 (on left side): PUT("x", "value_1")
  → A acks ✓, B acks ✓  → W=2 SUCCESS
  C gets nothing (partitioned)

Client 2 (on right side, with weaker consistency W=1):
  PUT("x", "value_2")
  → C acks ✓  → W=1 SUCCESS (sloppy quorum)
  A, B get nothing

State after partition:
  Node A: "value_1"
  Node B: "value_1"
  Node C: "value_2"  ← CONFLICT
```

**Partition heals.** Now what?

```
Read with R=2:
  Coordinator reads from A and C (or B and C)
  Node A → "value_1", vclock={left:1}
  Node C → "value_2", vclock={right:1}
  
  Vclocks are concurrent — neither descends from the other
  → TRUE CONFLICT. Coordinator must resolve:

  Option 1 (LWW): Compare timestamps → pick higher → "value_2" wins (or "value_1")
                  The losing write is SILENTLY DROPPED
  Option 2 (Vector clock): Surface both values to application → app merges
  Option 3 (CRDT): If the value is a PN-Counter or OR-Set → auto-merge deterministically
```

This is the **fundamental AP trade-off** — you kept accepting writes during the partition, and now you have to pay the reconciliation cost.

---

## Scenario 5: W=3 (Strong Durability) — Write Failure Behavior

```
N=3, W=3 (all replicas must ack)

Node C is DOWN.

PUT("user:1", "Alice"):
  Node A acks ✓
  Node B acks ✓
  Node C → timeout ✗

W=3 not reached → WRITE FAILS → client gets error

Client receives: ERROR — write not durable enough
```

This is the CP trade-off. You sacrifice availability (write rejected) to guarantee that when a write succeeds, it's on all N replicas — maximally durable.

```
Read after a successful W=3 write with R=1:
  Only need 1 replica to respond
  Any single replica is guaranteed to have the latest write (W+R = 3+1 = 4 > N=3)
  
  → Fastest possible read, still strongly consistent ✓
```

---

## Scenario 6: Quorum Race — Classic Consistency Anomaly

This illustrates why W+R > N matters:

```
N=3, W=1, R=1 (weak consistency — W+R=2, NOT > N=3)

PUT("x", "new_value"):
  Only written to Node A (W=1 satisfied)
  Node B, C still have old_value

GET("x"):
  Coordinator picks Node B or C randomly
  → Returns "old_value" even though write succeeded!
  
Client: "I just wrote new_value, why am I reading old_value?!"
→ This is a stale read — expected with W+R ≤ N
```

Now with W=2, R=2 (W+R=4 > N=3):

```
PUT("x", "new_value"):
  Written to Node A, Node B (W=2)

GET("x"):
  Reads from any 2 nodes. Possibilities:
  - A+B → both have new_value ✓
  - A+C → A has new, C has old → coordinator picks new (higher vclock) ✓
  - B+C → B has new, C has old → coordinator picks new ✓

In ALL cases, at least one replica in the read set (R=2) overlaps 
with the write set (W=2). The latest value is always returned. ✓
```

The overlap guarantee: at least `W + R - N = 2 + 2 - 3 = 1` replica is guaranteed to have seen the write. That one replica has the latest vclock and wins the comparison.

---

## Scenario 7: Coordinator Failure Mid-Write

```
PUT("user:1", "Alice"):
  Coordinator sends to A, B, C
  Node A acks ✓
  Node B acks ✓  ← W=2 reached, coordinator prepares to reply
  COORDINATOR CRASHES before replying to client

Client: Did the write succeed or not? No response received.
```

This is the **indeterminate state** problem — the write IS durable (2 replicas have it) but the client doesn't know. The client must:

1. **Retry the write** — idempotent writes (with a client-generated request ID) allow safe retry. The replicas detect the duplicate via the request ID and return success without double-applying.
2. **Read-your-write check** — client reads back the value to confirm before proceeding.

This is why well-designed KV store clients always use **idempotency keys** and implement retry with deduplication on the server side.

---

## Summary — Quorum Decision Matrix

```
Scenario                W   R   Outcome
─────────────────────────────────────────────────────
Strong consistency      3   1   Never stale reads, writes fail if any node down
Balanced                2   2   Survives 1 failure, reads always fresh  ← most common
Write-optimized         1   3   Fast writes, slow reads, reads always fresh
Eventual (fast)         1   1   Fastest, stale reads possible
─────────────────────────────────────────────────────
Rule:  W + R > N → strong consistency guarantee
       W + R ≤ N → possible stale reads (eventual consistency)
```

---

## Putting It All Together

```
Leader Election (Raft):
  Needed for CP systems that require a single authoritative writer
  Raft guarantees one leader per term via log-completeness voting
  Terms detect and reject stale leaders automatically

CRDTs:
  Needed for AP systems to merge concurrent writes without coordination
  Choose the right type: G-Counter, PN-Counter, OR-Set, LWW-Register, RGA
  Mathematically guaranteed convergence, but state growth requires GC

Quorum under failure:
  W + R > N is your consistency guarantee — verify this holds under your failure scenarios
  Sloppy quorum + hinted handoff preserves write availability at consistency cost
  Read repair + Merkle anti-entropy bring replicas back in sync over time
  Coordinator failures require idempotent retries with deduplication
```

Want to continue with **storage engine internals (LSM Trees, compaction strategies)**, **distributed transactions (2PC, Sagas)**, or **how systems like DynamoDB and Cassandra implement these in practice**?
