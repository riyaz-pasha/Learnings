# Chapter 3 — High-Throughput Location Storage

This is one of the most important chapters because it addresses the central architectural question:

How do we store hundreds of thousands of location updates per second, make nearby searches fast, and avoid Redis becoming a bottleneck?

We'll examine Redis in depth, then introduce partitioning, Kafka, buffering, and failure recovery.

By the end, we'll have a clear understanding of when to choose:

1. Direct ingestion into Redis.
2. Kafka first, followed by Redis.
3. Redis for live tracking plus Kafka for historical tracking.
4. Multiple geographically distributed Redis indexes.

Let's start with a very important distinction.

## 3.1 Redis is our live location index, not necessarily our source of truth

Imagine we have one million drivers.

Each driver periodically reports a location.

We want to maintain something like:

```
Driver ID    Latest Location       Last Updated
-------------------------------------------------
D101         (17.385, 78.486)       10:00:03
D102         (17.386, 78.487)       10:00:04
D103         (17.387, 78.488)       10:00:02
D104         (17.388, 78.489)       10:00:05
```

Our live matching service needs this information.

It doesn't necessarily need every previous location.

For example:

```
D101:
10:00:00 -> Location A
10:00:03 -> Location B
10:00:06 -> Location C
10:00:09 -> Location D
```

For live matching, retaining Location D is generally sufficient.

This is a latest-value workload.

That changes how we should think about storage.

A conventional database workload often focuses on preserving records.

Our live location workload focuses on maintaining a continuously updated view of the present.

This is why Redis is attractive.

But we need to understand exactly how to organize that data.

# 3.2 How does Redis GEO actually work?

Redis provides geospatial commands:

- `GEOADD`
- `GEOSEARCH`
- `GEOPOS`
- `GEODIST`
- `GEOHASH`

Let's understand the underlying idea instead of memorizing commands.

Suppose we have these drivers:

```
D101 -> (17.3850, 78.4867)
D102 -> (17.3860, 78.4870)
D103 -> (17.3900, 78.4900)
D104 -> (17.4000, 78.5000)
```

We create a Redis geospatial index:

```
GEOADD drivers:locations \
    78.4867 17.3850 D101 \
    78.4870 17.3860 D102 \
    78.4900 17.3900 D103 \
    78.5000 17.4000 D104
```

Important: Redis accepts longitude first, then latitude.

Conceptually, the index looks like:

```
drivers:locations
      |
      +---- D101 -> Geographic position
      |
      +---- D102 -> Geographic position
      |
      +---- D103 -> Geographic position
      |
      +---- D104 -> Geographic position
```

Now a customer requests a ride at:

```
Pickup:
Latitude  = 17.3850
Longitude = 78.4867

Radius = 3 km
```

We query:

```
GEOSEARCH drivers:locations
    FROMLONLAT 78.4867 17.3850
    BYRADIUS 3 km
    WITHDIST
    WITHCOORD
    ASC
```

Redis returns drivers within the requested geographical radius, along with their distances and coordinates.

The important point is that Redis doesn't need to calculate the distance between the customer and every driver in the entire dataset.

It uses its geospatial index to narrow down the candidates.

### What is Redis storing internally?

Redis GEO is implemented using a sorted-set-based geospatial index.

The geographical coordinates are encoded into a numerical representation based on geohashing, and the index uses sorted-set machinery to locate relevant geographic ranges.

You don't need to manually calculate those hashes when using Redis GEO.

The abstraction is:

```
GEOADD:
Insert/update geographic position.

GEOSEARCH:
Find geographic members in an area.

GEODIST:
Calculate distance between two indexed members.
```

One important distinction:

Redis GEO is a geographic search index, not a general-purpose navigation engine.

It doesn't automatically understand:

- Road networks.
- Traffic.
- One-way streets.
- Bridges.
- Travel times.
- Restricted roads.
- Driver preferences.

We'll deal with geographical distance versus actual travel distance in Chapter 5.

# 3.3 Our first Redis architecture

Let's start with a single key.

```
Redis
 |
 +---- drivers:locations
          |
          +---- D101
          +---- D102
          +---- D103
          +---- D104
          +---- ...
          +---- D1,000,000
```

All drivers are stored in one GEO index.

This is a reasonable initial design.

For a small or moderate deployment, it can work quite well.

But now let's introduce scale.

Suppose our system handles:

```
Online drivers:       1,000,000
Update interval:      3 seconds
Location write rate:  ~333,333/sec
```

We might think:

> Redis Cluster supports multiple nodes. Let's put this key in Redis Cluster and the cluster will distribute the location data.

Unfortunately, this is where an important Redis Cluster detail matters.

## 3.4 Why one huge GEO key becomes a bottleneck

Redis Cluster distributes keys, not individual members inside a key.

Suppose we have three Redis nodes:

```
Redis Cluster

Node A
Node B
Node C
```

Now we create:

```
drivers:locations
```

That entire key belongs to one hash slot.

Consequently, the entire GEO index lives on one primary node.

It doesn't automatically distribute:

```
D101 -> Node A
D102 -> Node B
D103 -> Node C
```

Instead:

```
Node A:
    drivers:locations
        D101
        D102
        D103
        ...
        D1,000,000

Node B:
    Other keys

Node C:
    Other keys
```

This creates a potential hotspot.

More Redis Cluster nodes won't solve the problem of one huge hot key.

This is a crucial system design interview point.

The solution is to partition the geospatial index into multiple keys.

But how should we partition it?

There are two major strategies.

## 3.5 Partitioning strategy 1 — Partition by Driver ID

This is the easiest way to distribute the workload without losing the ability to update one driver's location atomically.

Imagine we divide drivers into 16 partitions.

```
Driver ID
    |
    v
Hash(driverId) % 16
    |
    +---- Partition 0
    +---- Partition 1
    +---- Partition 2
    ...
    +---- Partition 15
```

For example:

```
D101 -> Partition 3
D102 -> Partition 8
D103 -> Partition 3
D104 -> Partition 12
D105 -> Partition 8
```

Each partition owns a separate GEO index.

```
Redis Cluster
    |
    +---- Partition 0
    |       geo:{p0}
    |       metadata:{p0}
    |       lastseen:{p0}
    |
    +---- Partition 1
    |       geo:{p1}
    |       metadata:{p1}
    |       lastseen:{p1}
    |
    +---- Partition 2
    |       geo:{p2}
    |       metadata:{p2}
    |       lastseen:{p2}
    |
    +---- ...
```

Here, `{p0}`, `{p1}` are Redis Cluster hash tags. Keys with the same hash tag are placed in the same hash slot.

This is useful because we can keep related data together.

For example, for Partition 0:

```
geo:{p0}
metadata:{p0}
lastseen:{p0}
```

All these keys can be accessed together within a Redis Lua script.

We can atomically:

1. Check the driver's session epoch.
2. Compare the sequence number.
3. Update the driver's metadata.
4. Update the GEO index.
5. Update the last-seen timestamp.

Now our previous ordering problem becomes much easier to solve.

### But how do we search nearby drivers?

Here's the trade-off.

Suppose the customer is in Hyderabad.

The nearby drivers might be distributed across all 16 partitions:

```
Partition 0 -> Nearby Driver D101
Partition 1 -> No nearby drivers
Partition 2 -> Nearby Driver D108
Partition 3 -> Nearby Driver D110
...
Partition 15 -> Nearby Driver D130
```

We must search all relevant partitions.

Conceptually:

```
                  Customer Request
                         |
                         v
                  Nearby Search
                         |
             +-----------+-----------+
             |           |           |
             v           v           v
          GEO p0      GEO p1      GEO p2
             |           |           |
             +-----------+-----------+
                         |
                      ...
                         |
                         v
                  Merge Results
                         |
                         v
                 Rank Candidates
```

We can query partitions in parallel and merge their results.

For each partition, we request the nearest K candidates.

For example:

```
Partition 0 -> Top 20
Partition 1 -> Top 20
Partition 2 -> Top 20
...
Partition 15 -> Top 20
```

Then we merge, remove duplicates if necessary, and select the globally best candidates.

Why is requesting a limited number from each partition sufficient?

Because we generally don't need every nearby driver.

We need enough good candidates for the dispatch strategy.

There are some subtleties around ranking and filtering, which we'll cover in Chapter 5.

### Advantages

- Simple and predictable partition assignment.
- Location updates from one driver always go to the same partition.
- Related driver metadata can share the same Redis hash slot.
- Atomic per-driver updates become manageable.
- Writes distribute across partitions.
- A single Redis key no longer becomes the system-wide write bottleneck.

### Disadvantages

- Nearby searches require fan-out.
- A search may contact many Redis partitions.
- The same city-wide driver population is spread across every partition.
- Increasing the number of partitions also increases search fan-out.

This leads to a useful design principle:

Driver-ID partitioning makes writes easier to distribute, but geographical partitioning can make reads more efficient.

Let's understand that second approach.

## 3.6 Partitioning strategy 2 — Partition by geographical area

Instead of distributing drivers randomly across partitions, let's organize them according to where they are.

Imagine splitting a city into geographical cells.

```
+----------+----------+----------+
| Cell A   | Cell B   | Cell C   |
|          |          |          |
+----------+----------+----------+
| Cell D   | Cell E   | Cell F   |
|          |          |          |
+----------+----------+----------+
| Cell G   | Cell H   | Cell I   |
|          |          |          |
+----------+----------+----------+
```

Each driver belongs to a geographical cell based on their current coordinates.

For example:

```
D101 -> Cell E
D102 -> Cell E
D103 -> Cell F
D104 -> Cell H
D105 -> Cell B
```

We can use a geospatial partitioning system such as:

- Geohash.
- H3.
- S2.
- A custom grid.

We'll study their algorithms and differences in Chapter 4.

For now, assume every driver has a current cell.

Our Redis structure could look like:

```
Redis
 |
 +---- geo:cell_A
 |
 +---- geo:cell_B
 |
 +---- geo:cell_C
 |
 +---- geo:cell_D
 |
 +---- geo:cell_E
 |
 +---- geo:cell_F
 |
 +---- geo:cell_G
 |
 +---- geo:cell_H
 |
 +---- geo:cell_I
```

Now suppose a customer requests a ride in Cell E.

We don't need to search every geographical cell in the city.

We search:

```
Cell E
Cell B
Cell D
Cell F
Cell H
```

The exact set depends on the search radius and cell geometry.

We have reduced the search scope considerably.

### Why is this attractive?

Imagine a city with 500,000 online drivers.

A customer requests a ride in one neighbourhood.

With geographical partitioning, we can search the cells intersecting the requested area instead of querying every driver in the city.

This gives us geographical locality.

It can reduce:

- Search fan-out.
- Unnecessary Redis operations.
- Amount of data returned.
- Network traffic between search services and Redis.

But now we have a new problem.

### What happens when a driver moves to another cell?

Suppose:

```
Initially:

D101 -> Cell E
```

The driver moves.

```
Now:

D101 -> Cell F
```

We need to:

```
Remove D101 from Cell E
Add D101 to Cell F
```

Conceptually:

```
Old Cell E                  New Cell F
    |                           |
    |---- Remove D101            |
    |                           |
    |                    Add D101
```

What if Cell E and Cell F are on different Redis Cluster nodes?

We cannot assume that one Redis Lua script can atomically update both.

Redis Cluster restricts multi-key atomic operations to keys in the same hash slot.

If we force all cell keys into one slot to solve this, we defeat the purpose of distributing them.

This is an important distributed systems trade-off.

### How do we handle geographical movement?

A practical approach is to treat the geographical index as an eventually updated projection of the driver's authoritative latest-location state.

Consider this flow:

```
                 Location Update
                       |
                       v
                Driver Owner
                       |
                       v
              Validate Sequence
                       |
                       v
              Current Location State
                       |
                       v
             Determine New Geo Cell
                       |
              +--------+--------+
              |                 |
              v                 v
        Insert into New     Remove from Old
           Cell                Cell
```

We use the following rules.

Rule 1: Serialize or version updates per driver.

Every accepted location update must have a valid session epoch and increasing sequence number.

An older update must never overwrite a newer one.

Rule 2: Insert into the new cell before removing from the old cell.

This avoids creating a period where the driver is absent from both cells during normal movement.

Rule 3: Make index operations idempotent.

Repeated insertion or deletion should not corrupt the index.

Rule 4: Validate candidates against the latest driver state.

A search might temporarily discover a driver from an old cell.

Before offering a ride, the matching service checks the latest accepted location and availability.

Rule 5: Have cleanup and reconciliation.

If a server crashes after inserting into the new cell but before removing from the old one, the driver might temporarily exist in both indexes.

A reconciliation process removes obsolete entries.

There is an important trade-off here:

The new cell should be updated before we acknowledge the movement as available for geographical search. Otherwise, a nearby query can temporarily miss the driver.

Even then, distributed updates cannot be treated as one atomic transaction. We need version checks, retries, and reconciliation.

### A useful observation

A stale extra entry usually creates a false positive:

```
Search Cell E
    |
    v
Find D101
    |
    v
Check latest location
    |
    v
D101 is actually in Cell F
    |
    v
Reject stale candidate
```

A missing new entry creates a false negative:

```
Search Cell F
    |
    v
D101 not found
```

False positives can often be filtered after discovery.

False negatives can temporarily hide eligible drivers.

That's why we insert into the new cell first and remove from the old cell second.

### When should we choose geographical partitioning?

It is attractive when:

- There are very large numbers of drivers in a region.
- Nearby searches are frequent.
- Geographical locality matters.
- Searching every driver partition is too expensive.
- We can tolerate and manage eventual consistency in the spatial index.

It requires more careful engineering than driver-ID partitioning.

## 3.7 Can we combine both partitioning strategies?

Absolutely.

In fact, this is a natural evolution for a very large system.

Consider two levels:

```
                 City / Region
                       |
               Geographical Cells
                       |
              +--------+--------+
              |                 |
            Cell A            Cell B
              |                 |
          Hash Shards       Hash Shards
          |   |   |         |   |   |
          P0  P1  P2        P0  P1  P2
```

For example:

```
Hyderabad
  |
  +---- Cell A
  |       +---- Partition 0
  |       +---- Partition 1
  |
  +---- Cell B
  |       +---- Partition 0
  |       +---- Partition 1
  |
  +---- Cell C
          +---- Partition 0
          +---- Partition 1
```

Now we have both geographical locality and distributed storage within a geographical area.

However, the data placement and movement logic becomes more complicated.

For most interviews, don't jump directly to this architecture.

Start with:

1. One Redis GEO index for a moderate workload.
2. Driver-ID partitioning when write distribution becomes necessary.
3. Geographic partitioning when search fan-out becomes expensive.
4. A combination when individual geographical areas themselves become too large.

This is an example of evolving an architecture according to the bottleneck rather than introducing complexity prematurely.

# 3.8 What exactly should we store in Redis?

Let's define a more practical data model.

For the driver-ID partitioning approach, suppose D101 belongs to partition 3.

We could maintain three structures.

1\. Geospatial index

```
GEOADD geo:{p3} 78.4867 17.3850 D101
```

Purpose:

Find nearby drivers.

2\. Driver metadata

Conceptually:

```
HASH metadata:{p3}:D101

latitude             = 17.3850
longitude            = 78.4867
accuracy             = 8.5
sessionEpoch         = 12
sequenceNumber       = 101
serverReceivedAt     = ...
```

Purpose:

Maintain the latest accepted location version and metadata.

3\. Last-seen index

```
ZADD lastseen:{p3} <serverTimestamp> D101
```

Purpose:

Find drivers whose location updates have become stale.

Why use a separate sorted set for last-seen time?

Because Redis GEO members don't have independent TTLs.

This is a subtle but extremely important implementation detail.

### Why not simply use TTL on the GEO key?

Suppose:

```
geo:{p3}
    |
    +---- D101
    +---- D102
    +---- D103
```

If we apply an expiry to the entire key, all three drivers disappear together.

But D101 might stop sending updates while D102 and D103 continue working.

We need per-driver expiration or cleanup.

Redis doesn't provide a separate TTL for each GEO member.

So we maintain a separate time index.

### How does cleanup work?

Suppose our freshness policy is:

```
Location is considered stale
if no valid update arrives for 15 seconds.
```

A background cleanup worker periodically finds:

```
ZRANGEBYSCORE lastseen:{p3} -inf <cutoffTimestamp>
```

For each stale driver:

1. Check that the driver's last-seen value is still old.
2. Remove the driver from the GEO index.
3. Remove the metadata.
4. Remove the last-seen entry.

Conceptually:

```
Cleanup Worker
      |
      v
Find stale drivers
      |
      +---- D101
      +---- D104
      +---- D108
      |
      v
Remove from GEO index
      |
      v
Remove metadata
      |
      v
Remove last-seen entries
```

The cleanup operation should also be idempotent and protected against races with fresh updates.

For example, imagine the cleanup worker identifies D101 as stale, but D101 sends a new location before the deletion executes.

We must not delete that fresh location.

A safe implementation checks the timestamp/version again as part of the atomic cleanup operation.

Also, cleanup is not our only defense.

Nearby search and matching must independently reject stale locations.

Why?

Because cleanup workers run periodically. A driver could become stale just after the previous cleanup sweep.

We cannot wait for cleanup to finish before recognizing that the driver is stale.

# 3.9 Direct Redis vs Kafka-first ingestion

Now let's answer one of your original questions:

> Should we insert directly into Redis, or put a queue in between?

There is no universal answer.

It depends on whether we need to preserve every location event or only maintain the latest location.

Let's compare both designs.

## Design A — Direct Redis write

```
Driver Apps
     |
     v
Load Balancer
     |
     v
Location Ingestion Servers
     |
     v
Redis GEO Partitions
     |
     v
Nearby Matching
```

The flow is:

```
1. Driver sends location.
2. Server validates it.
3. Server updates Redis.
4. Server acknowledges success.
```

### What happens during a Redis failure?

Suppose Redis is unavailable.

```
Driver App
    |
    v
Ingestion Server
    |
    X
Redis unavailable
```

We cannot truthfully acknowledge that the live index was updated.

We should return a retryable failure.

The driver application can retain the latest location and retry with backoff.

When Redis becomes available again, the driver sends its latest location.

We don't need to replay every intermediate coordinate for live matching.

This is an important advantage of latest-value semantics.

### Advantages

- Lowest processing complexity.
- Short ingestion-to-index latency.
- No consumer lag.
- No Kafka infrastructure required for live tracking.
- Obsolete updates can be safely discarded.
- Natural recovery through subsequent driver updates.

### Disadvantages

- Redis is directly on the ingestion critical path.
- If Redis is unavailable, fresh locations cannot be indexed.
- Sudden bursts must be controlled through backpressure and rate limiting.
- There is no durable event log for replaying lost historical updates.

For a system whose primary requirement is the latest live location, this is my default starting architecture.

But let's examine another requirement.

## Design B — Kafka-first ingestion

Imagine the business now requires that every location update during an active delivery must be preserved.

We need it for:

- Route reconstruction.
- Delivery dispute resolution.
- Historical analytics.
- Fleet monitoring.
- Auditing.

Now losing intermediate location events is not acceptable.

We introduce Kafka.

```
Driver Apps
     |
     v
Load Balancer
     |
     v
Location Ingestion Servers
     |
     v
Kafka
     |
     v
Location Consumers
     |
     v
Redis GEO
     |
     v
Nearby Matching
```

The flow changes:

```
1. Driver sends location.
2. Ingestion server validates it.
3. Server publishes the event to Kafka.
4. Kafka acknowledges the accepted event.
5. Ingestion server acknowledges the driver.
6. Consumers read the event.
7. Consumers update Redis and historical storage.
```

Notice what Kafka does.

It provides a durable, ordered event log within each partition, subject to the configured replication and acknowledgement guarantees.

It also allows multiple independent consumers to process the same location events.

For example:

```
                     Kafka
                       |
             +---------+---------+
             |         |         |
             v         v         v
          Redis     History   Analytics
          Updater   Consumer   Consumer
```

This gives us a lot of flexibility.

### How should we partition Kafka?

Use the driver ID as the message key.

For example:

```
Key: D101

Value:
{
    "latitude": 17.385,
    "longitude": 78.4867,
    "sequenceNumber": 101
}
```

Kafka hashes the key to choose a partition.

Therefore, updates for the same driver normally go to the same Kafka partition.

Conceptually:

```
Kafka

Partition 0:
    D101 -> Location 101
    D104 -> Location 201
    D101 -> Location 102

Partition 1:
    D102 -> Location 101
    D105 -> Location 201

Partition 2:
    D103 -> Location 101
    D102 -> Location 102
```

This allows consumers to process messages partition by partition while preserving the order of records within each partition.

But remember:

Kafka ordering alone doesn't solve every ordering problem.

For example:

- Multiple producers might publish concurrently.
- Old messages might be retried.
- Events from previous sessions might arrive late.

So we still use the session epoch and sequence number validation discussed in Chapter 2.

### What happens if Redis crashes now?

This is where Kafka-first becomes useful.

```
Driver
  |
  v
Ingestion
  |
  v
Kafka       <- Events safely accepted
  |
  v
Redis       <- Temporarily unavailable
```

The Kafka consumer cannot update Redis.

It can retry.

If Redis is unavailable for some time, Kafka retains the events according to its retention configuration.

After Redis recovers, consumers continue processing.

The live index gradually catches up.

We can also rebuild the latest locations by replaying retained events, provided the retained history is sufficient.

### But what is the disadvantage?

Latency.

Suppose:

```
Driver sends at 10:00:00
Kafka accepts at 10:00:00.010
Redis consumer processes at 10:00:00.200
```

Our index is already 200 milliseconds behind.

Under heavy consumer lag:

```
Driver sends at 10:00:00
Redis updates at 10:00:05
```

Now matching is working with five-second-old locations.

That may be unacceptable for fast-moving drivers.

So Kafka-first introduces a real trade-off:

We gain durable event ingestion and replayability, but the live materialized view becomes asynchronous.

We must monitor consumer lag and enforce freshness policies.

Also, Kafka does not magically give exactly-once updates to Redis. If a consumer updates Redis and crashes before committing its Kafka offset, the same event can be processed again.

That's why our Redis consumer must use idempotent, version-aware updates.

## 3.10 Design C — Redis for live tracking + Kafka for history

This is often an attractive architecture when we want both fast live tracking and historical events.

```
                      LOCATION UPDATE
                             |
                             v
                      Ingestion Service
                             |
                  +----------+----------+
                  |                     |
                  v                     v
             Redis GEO                Kafka
                  |                     |
                  v                     +------> History
            Live Matching               |
                                        +------> Analytics
```

Here, we attempt two operations:

1. Update Redis immediately.
2. Publish the location event to Kafka asynchronously.

The live matching service doesn't wait for Kafka.

This gives us low live-location latency.

However, there is an important reliability problem.

### The dual-write problem

Consider this sequence:

```
Redis update succeeds.
Kafka publication fails.
```

Now:

```
Live location = Updated
Location history = Missing event
```

Or the reverse:

```
Kafka publication succeeds.
Redis update fails.
```

Now:

```
Live location = Stale
Location history = Event preserved
```

These writes aren't one atomic transaction.

So we must decide which operation is authoritative.

There are three reasonable approaches:

| Requirement                                        | Appropriate strategy                                               |
| -------------------------------------------------- | ------------------------------------------------------------------ |
| Only latest location matters                       | Direct Redis                                                       |
| Every location event must be preserved             | Kafka-first                                                        |
| Live location matters most, history is best-effort | Redis-first with asynchronous publication                          |
| Both require strict recovery guarantees            | Durable ingestion boundary and carefully designed event processing |

For strict historical completeness, Kafka-first is generally simpler than trying to make two independent writes atomic.

A transactional outbox can also help when events originate from a durable transactional database, but it is usually not the natural solution for every high-frequency ephemeral GPS update.

### One subtle but important optimization

We don't necessarily have to publish every GPS point to Kafka.

Suppose a driver sends 100 updates while travelling.

We might:

- Publish every update during an active trip for route history.
- Sample updates when the driver is idle.
- Send only the latest location to Redis.
- Publish separate significant events for analytics.

The retention and durability requirements can vary by business state.

For example, delivery tracking during an active order may justify more historical detail than a driver merely being online and waiting for work.

# 3.11 Redis write-throughput optimization

Now let's focus on the direct Redis architecture.

We have potentially hundreds of thousands of updates per second.

What can we do to improve throughput?

There are several independent optimizations.

### A. Horizontal partitioning

We've already discussed why a single huge GEO key is problematic.

Partition the index.

```
Ingestion Servers
       |
       v
Partition Router
       |
       +------> Redis Partition 0
       |
       +------> Redis Partition 1
       |
       +------> Redis Partition 2
       |
       +------> Redis Partition N
```

Make sure the routing strategy distributes load appropriately.

Don't merely add more Redis nodes without distributing the keys.

### B. Pipelining

Imagine an ingestion server has received 100 updates.

Without pipelining:

```
Update 1 -> Redis -> Wait
Update 2 -> Redis -> Wait
Update 3 -> Redis -> Wait
...
Update 100 -> Redis -> Wait
```

There are repeated network round trips.

With pipelining:

```
Ingestion Server
       |
       +---- Command 1
       +---- Command 2
       +---- Command 3
       +---- ...
       +---- Command 100
                 |
                 v
              Redis
                 |
                 v
          Batch of responses
```

We send multiple commands without waiting for each response individually.

This reduces network round-trip overhead.

Important:

Pipelining is not the same as a transaction.

It improves network efficiency but does not make all commands atomic.

If multiple commands must update related driver data consistently, use an appropriate atomic operation, such as a Lua script, within one Redis hash slot.

A practical system can pipeline independent driver updates while executing each driver's version-check-and-update operation atomically.

### C. Batch GEO operations

Redis GEO commands support adding multiple members in one operation.

For example:

```
GEOADD geo:{p3} \
    78.4867 17.3850 D101 \
    78.4870 17.3860 D102 \
    78.4900 17.3900 D103
```

This can reduce command overhead.

But if each driver requires conditional session and sequence validation, a plain batch `GEOADD` isn't sufficient.

We must not sacrifice the ordering guarantee just to reduce commands.

### D. Coalesce unnecessary updates

Suppose:

```
D101:
    Location A
    Location B
    Location C
    Location D
```

If the backend hasn't processed these updates yet and only Location D matters, we can coalesce the pending updates.

The system can process:

```
D101 -> Location D
```

instead of all four.

This is particularly useful for transient ingestion buffering.

But remember the distinction:

- Live latest-location storage can discard superseded values.
- Historical event storage must preserve required events.

### E. Avoid unnecessary payloads

The location ingestion service doesn't need a large JSON document for every GPS update.

Prefer a compact schema.

For high-volume systems, a binary representation such as Protocol Buffers can reduce serialization and network overhead.

However, optimization should be guided by actual load testing.

### F. Control the number of in-flight Redis requests

Don't allow every incoming HTTP request to spawn unlimited Redis operations.

Use:

- Bounded connection pools.
- Bounded asynchronous execution.
- Concurrency limits per partition.
- Request deadlines.
- Circuit breakers where appropriate.
- Backpressure.

If one Redis partition is unhealthy, we should not let it consume all the resources of the ingestion service.

# 3.12 How much can Redis actually handle?

Let's address the question directly:

> Can Redis handle 333,000 location writes per second?

It depends on:

- Hardware.
- Redis version and configuration.
- Number of shards.
- Network bandwidth.
- Command complexity.
- Number of GEO members per shard.
- Atomic Lua script complexity.
- Replication overhead.
- Read/write distribution.
- Data size and memory pressure.

There isn't one universal Redis throughput number we can safely assume.

Also, these are different workloads:

```
Workload A:
GEOADD only

Workload B:
GEOADD + metadata update

Workload C:
Lua script + GEOADD + sequence comparison
            + last-seen update

Workload D:
All of the above + replication + nearby searches
```

They won't necessarily have the same throughput.

### How should we size the cluster?

Let's say our target is:

```
Average ingestion rate: 333,333 updates/sec
```

We run performance tests with realistic data and operations.

Suppose our benchmark tells us:

```
One Redis primary can sustain
50,000 updates/sec
under our actual workload and latency target.
```

This is just an illustrative benchmark result, not a Redis guarantee.

A naive calculation says:

\\[ \frac{333,333}{50,000} \approx 6.67 \\]

So we'd need at least seven primary shards to handle that average load.

But we shouldn't deploy exactly seven.

We need headroom for:

- Traffic spikes.
- Uneven partition distribution.
- Nearby searches.
- Replication.
- Failovers.
- Background cleanup.
- Future growth.

We also need to test read and write operations together.

A better sizing process is:

1. Establish realistic location-update payloads.
2. Include session and sequence validation.
3. Include metadata and GEO index updates.
4. Run nearby-search workloads simultaneously.
5. Measure p50, p95, and p99 latency.
6. Measure CPU, memory, network, and replication lag.
7. Introduce burst traffic.
8. Test node failures and failover.
9. Add capacity based on measured performance and operational headroom.

Never size Redis using a benchmark of only plain `SET` operations when your production operation is a multi-step geospatial Lua script.

# 3.13 Redis replication and durability

Now let's talk about what happens when a Redis node crashes.

Suppose:

```
Redis Primary A
      |
      +---- Replica A
```

All location updates go to Primary A.

The replica receives replicated updates.

Now Primary A crashes.

The replica may be promoted.

This provides availability.

But there is a subtle point:

Redis replication is generally asynchronous.

Suppose:

```
10:00:00.000 -> Primary accepts Location A
10:00:00.005 -> Primary crashes
10:00:00.010 -> Replica is promoted
```

The replica might not have received the latest acknowledged write.

So some recent location updates may disappear during failover.

Is that acceptable?

For a live location index, often yes.

The driver will continue reporting its location.

But we must understand the consequence:

```
Redis failover
     |
     v
Some recent locations may be missing
     |
     v
Drivers reconnect / report again
     |
     v
Live index recovers
```

Don't confuse replication with a strong durability guarantee.

### What about Redis persistence?

Redis supports persistence mechanisms such as:

- RDB snapshots.
- AOF (Append Only File).
- Combinations of the two.

They can help recover data after restarts.

But persistence has performance, latency, and recovery implications.

For our ephemeral live location index, we may choose not to make every location update synchronously durable on disk.

We might instead accept losing recent location updates during certain failures and recover through fresh driver reports.

### What about WAIT?

Redis also provides the `WAIT` command, which can wait for replicas to acknowledge a specified number of preceding writes.

This can improve replication acknowledgement guarantees in some scenarios.

However, it isn't equivalent to a strongly consistent, durable database commit.

It does not transform asynchronous Redis replication into a fully synchronous distributed transaction.

For our use case, adding this coordination to every GPS update may also be unnecessary.

The important architectural decision is not to make Redis perfectly durable. It is to make the system recover correctly when the live index loses data.

For assignment correctness, we rely on the durable transactional system, not the live location cache.

# 3.14 What happens if Redis completely disappears?

Let's simulate a major failure.

Before failure:

```
Redis

D101 -> Location A
D102 -> Location B
D103 -> Location C
D104 -> Location D
```

All Redis nodes in the live location cluster become unavailable.

What happens?

### Step 1: Stop trusting the live index

Matching must not use locations simply because they existed before the failure.

The system should recognize that the index is unavailable or recovering.

### Step 2: Driver applications keep sending updates

The ingestion service may temporarily return a retryable response.

Drivers retain the latest location and retry.

We should avoid building an enormous offline backlog.

Only the latest location matters for live matching.

### Step 3: Redis recovers

The cluster starts accepting writes again.

### Step 4: Live locations repopulate

```
D101 -> New Location
D102 -> New Location
D103 -> New Location
D104 -> New Location
```

The index gradually recovers.

### Step 5: Matching resumes for fresh drivers

A driver is eligible only when we have a sufficiently fresh, accepted location and valid availability state.

We shouldn't assume that every previously online driver is immediately matchable after Redis recovery.

### What if many drivers reconnect simultaneously?

This can produce a thundering herd.

We should use:

- Randomized reconnect delays.
- Exponential backoff.
- Rate limiting.
- Capacity-aware ingestion.
- Gradual reopening of matching traffic.

This is particularly important after a regional outage.

# 3.15 A very important consistency rule: Never match from stale locations

Imagine:

```
D101 last location = 10:00:00
Current server time = 10:00:30
```

Our freshness threshold might be 15 seconds.

D101 should not be considered a valid live candidate.

Even if the driver's old coordinates are still present in Redis GEO, they are not trustworthy for dispatch.

We need two separate concepts:

```
Location Exists
        !=
Location Is Fresh
```

And:

```
Location Is Fresh
        !=
Driver Is Available
```

A candidate must satisfy all relevant conditions.

For example:

```
Eligible Driver =
    Authenticated
    AND Online
    AND Location Fresh
    AND Available
    AND Not Already Reserved
    AND Service Compatible
```

We'll formalize the complete matching query in Chapter 5.

# 3.16 Putting Chapter 3 together — Our recommended architectures

We now have enough information to choose an architecture based on requirements.

### Pattern 1: Simple, fast live location system

Use this when the primary requirement is nearby discovery and losing some recent locations is acceptable.

```
Driver Apps
     |
     v
Location Ingestion
     |
     v
Redis GEO
     |
     v
Matching Service
```

Scale Redis using multiple partitions when necessary.

This is the simplest architecture.

### Pattern 2: Reliable historical tracking

Use this when every required location event must be preserved.

```
Driver Apps
     |
     v
Location Ingestion
     |
     v
Kafka
     |
     +------> Redis GEO
     |
     +------> Historical Storage
     |
     +------> Analytics
```

Kafka is the durable event-ingestion boundary.

Redis is a materialized live-location view.

### Pattern 3: Low-latency live tracking plus best-effort history

Use this when immediate matching is the priority and historical data doesn't require complete preservation.

```
                   Location Ingestion
                          |
                +---------+---------+
                |                   |
                v                   v
            Redis GEO           Kafka
                |                   |
                v                   v
           Live Matching       Best-effort History
```

Be explicit about the dual-write failure modes.

### Pattern 4: Very high-scale geographical matching

Use this when a city-wide or regional GEO index becomes too large or nearby queries become too expensive.

```
                Location Ingestion
                       |
                       v
                Regional Router
                       |
               +-------+-------+
               |       |       |
               v       v       v
             Cell A  Cell B  Cell C
               |       |       |
             Redis   Redis   Redis
               |       |       |
               +-------+-------+
                       |
                       v
                Nearby Discovery
```

In a very large deployment, cells can themselves have multiple storage partitions.

## 3.17 Which architecture would I start with?

For a typical ride-hailing system, I would evolve it like this:

Initial production architecture

```
Driver App
    |
    v
Location Ingestion
    |
    v
Redis Cluster
    |
    +---- GEO Partition 0
    +---- GEO Partition 1
    +---- GEO Partition 2
    +---- GEO Partition N
```

Use driver-ID-based partitioning initially.

Why?

- Every driver has a stable partition.
- Updating one driver's location is straightforward.
- We can atomically update location, version, and last-seen metadata within a partition.
- We distribute writes.
- We can query partitions concurrently.

When geographical search becomes expensive:

Introduce a geographical index with cell-based partitioning.

When historical tracking becomes a requirement:

Introduce Kafka with a clearly defined durability contract.

Don't introduce Kafka, geographical sharding, and multiple indexing layers simply because the system is expected to become large someday.

Introduce them when the corresponding workload justifies them.

# Chapter 3 — What you should remember

These are the fundamental ideas:

| Problem                                     | Solution                                           |
| ------------------------------------------- | -------------------------------------------------- |
| One large GEO key becomes hot               | Partition the index                                |
| Redis Cluster doesn't split individual keys | Create multiple GEO keys                           |
| Need atomic per-driver updates              | Co-locate related keys in one hash slot            |
| Nearby queries across driver partitions     | Parallel fan-out and merge                         |
| Nearby queries over a huge area             | Geographical cell partitioning                     |
| Driver moves between cells                  | Versioned, retryable index movement                |
| Old locations remain in GEO                 | Last-seen sorted set + cleanup                     |
| Redis fails                                 | Repopulate using fresh driver updates              |
| Need durable location events                | Kafka-first ingestion                              |
| Need fast live tracking                     | Direct Redis writes                                |
| Need both speed and best-effort history     | Redis live path + asynchronous history             |
| Need high write throughput                  | Partitioning, pipelining, coalescing, backpressure |

The most important conceptual distinction:

Redis maintains the current geographical view. Kafka, when required, preserves the event history. The transactional database owns business-critical assignment decisions.

These are three different responsibilities.

# Next: Chapter 4 — Geospatial Indexing and Nearby Search Algorithms

Now we'll move to the algorithmic part.

This is where we'll understand how the system knows which geographical areas to search.

We'll build the intuition from basic geometry rather than starting with complex algorithms.

Topics:

1. Why searching all drivers is inefficient.
2. Why latitude and longitude cannot be treated like ordinary X/Y coordinates.
3. How radius queries actually work.
4. Bounding boxes versus circular radius searches.
5. Geohash: how coordinates become geographical cells.
6. Neighbouring geohash cells and boundary problems.
7. Quadtree: recursively splitting geographical space.
8. H3 and S2: hierarchical geographical indexing.
9. How to choose cell sizes for different search radii.
10. How Redis GEO, geohash, and H3 fit together.
11. How to avoid missing drivers near cell boundaries.
12. Why geographical distance and road travel distance are different.

We'll also build a concrete example: Given a customer's pickup coordinates and a 3 km search radius, how exactly do we identify the cells, search them, and produce a correct nearby-driver candidate set?

Say next to continue.
