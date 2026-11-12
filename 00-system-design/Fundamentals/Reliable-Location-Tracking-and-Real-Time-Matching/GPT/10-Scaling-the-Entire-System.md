# Chapter 10 — Scaling the Entire System

We've built the complete functional architecture.

Now let's imagine our system has become successful.

We're no longer operating in one city with a few thousand drivers.

We're operating across multiple cities and countries.

Consider this hypothetical scale:

```
Registered drivers:             10 million
Simultaneously online drivers:   1 million
Location update interval:        3 seconds
Concurrent WebSocket clients:    800,000
Concurrent ride requests:        10,000
Cities:                           100+
Regions:                          Multiple
```

Our approximate location ingestion rate:

\\[ \frac{1,000,000}{3} \approx 333,333 \\]

That's approximately 333,333 location updates per second.

Our previous architecture can still work conceptually, but we need to distribute ownership and workloads carefully.

The biggest mistake would be trying to scale the entire architecture as one giant application.

Instead, we should scale each responsibility independently.

## 10.1 The fundamental principle — Scale according to workload

Let's identify our major workloads.

| Component             | Primary workload              | Main scaling requirement                  |
| --------------------- | ----------------------------- | ----------------------------------------- |
| Location ingestion    | Continuous high-volume writes | Throughput                                |
| Redis location index  | GEO writes and reads          | Partitioning and memory                   |
| Nearby discovery      | Spatial queries               | Geographic locality and query parallelism |
| Matching engine       | Filtering, ranking, ETA       | CPU and concurrency                       |
| Dispatch orchestrator | Offer state transitions       | Workflow throughput                       |
| WebSocket gateways    | Persistent connections        | Connection capacity                       |
| Message delivery      | Notifications and events      | Delivery throughput                       |
| Assignment database   | Reservations and assignments  | Transactional correctness                 |
| Historical tracking   | Location event ingestion      | Durable write throughput                  |
| Heartbeat processing  | Frequent liveness signals     | Lightweight state updates                 |

Notice that these workloads have different characteristics.

For example:

- Location ingestion is write-heavy.
- Nearby discovery is geographically selective and read-heavy.
- WebSocket gateways are connection-heavy.
- Assignment is transaction-heavy but generally much lower volume than location updates.
- Historical tracking is storage-heavy.

We should not scale them all in the same way.

### Our new architecture

```
                    DRIVER APPLICATIONS
                            |
                +-----------+-----------+
                |                       |
                v                       v
         Location Ingestion       WebSocket Gateways
                |                       |
                v                       v
           Redis GEO               Connection Registry
                |                       |
                v                       |
         Matching Engine                 |
                |                       |
                v                       |
        Dispatch Orchestrator             |
                |                       |
                v                       |
        Assignment Service               |
                |                       |
                v                       |
        Transactional Database           |
                |                       |
                v                       |
          Transactional Outbox ----------+
```

The services can scale independently.

For example:

```
Location Ingestion:
    40 instances

Matching:
    20 instances

WebSocket Gateways:
    60 instances

Assignment:
    10 instances

Outbox Publishers:
    8 instances
```

These numbers are purely illustrative.

The important idea is that we can add capacity to the component experiencing the bottleneck without scaling every other component.

# 10.2 Scaling location ingestion independently

Let's start with our biggest write workload.

```
1 million online drivers
Location interval = 3 seconds

~333,333 updates/sec
```

We should horizontally scale the ingestion servers.

```
                     LOAD BALANCER
                           |
           +---------------+---------------+
           |               |               |
           v               v               v
        Ingest A        Ingest B        Ingest C
           |               |               |
           +---------------+---------------+
                           |
                           v
                    LOCATION ROUTER
                           |
          +----------------+----------------+
          |                |                |
          v                v                v
       Redis P0         Redis P1         Redis P2
```

The ingestion servers should ideally be stateless.

They don't need to remember which server handled a driver previously.

Each request contains:

```
driverId
sessionEpoch
sequenceNumber
latitude
longitude
```

The ingestion service determines the appropriate storage partition.

### Why stateless ingestion matters

Suppose Ingest A crashes.

Its requests can be routed to Ingest B or C.

We don't need to recover Ingest A's local business state.

The shared storage and authoritative session information determine which updates are valid.

This makes horizontal scaling and deployment significantly easier.

### Partition routing

Suppose we have 256 logical driver partitions.

```
partition = hash(driverId) % 256
```

For example:

```
D101 -> Partition 17
D102 -> Partition 91
D103 -> Partition 17
D104 -> Partition 220
```

The ingestion service routes the update to the correct storage owner.

Why use logical partitions instead of hardcoding the physical Redis node?

Because physical infrastructure will change.

For example:

```
Logical Partition 17 -> Redis Node A
Logical Partition 91 -> Redis Node B
Logical Partition 220 -> Redis Node C
```

Later, we might move Partition 17 to Node D without changing the logical identity.

This separation is extremely useful for rebalancing.

# 10.3 Partitioning Redis properly

Let's revisit our Redis design.

We learned that Redis Cluster distributes keys across hash slots, not individual members inside one GEO key.

So one key containing every driver is not enough.

We need multiple independently distributable GEO indexes.

### Design A — Driver-ID partitioning

```
Redis Cluster
    |
    +---- geo:{p0}
    +---- geo:{p1}
    +---- geo:{p2}
    +---- geo:{p3}
    +---- ...
    +---- geo:{p255}
```

Each GEO key contains only drivers assigned to its logical partition.

For example:

```
geo:{p17}
    D101
    D103
    D110
    D115
```

Writes distribute across partitions.

### How many Redis nodes do we need?

We cannot determine this from driver count alone.

We need to know:

- Number of updates per second.
- GEO index size.
- Metadata size.
- Lua script cost.
- Nearby query workload.
- Memory per driver.
- Replication overhead.
- Latency targets.
- Peak load.

Suppose our benchmark shows one primary can handle 40,000 location updates per second under our actual production-like workload.

Then:

\\[ \frac{333,333}{40,000} \approx 8.34 \\]

A theoretical minimum is nine primary shards.

But we need operational headroom, uneven load tolerance, and capacity for reads.

We might provision more.

The number must come from load testing, not from a universal Redis throughput assumption.

### The remaining problem: Search fan-out

If we have 256 GEO partitions, a nearby request might need to query all 256.

That can become expensive.

For example:

```
Ride Request
     |
     +---- Query P0
     +---- Query P1
     +---- Query P2
     ...
     +---- Query P255
```

Even in parallel, we're creating 256 operations per request.

With 10,000 ride requests per second:

\\[ 10,000 \times 256 = 2,560,000 \\]

That's 2.56 million Redis search operations per second.

Clearly, driver-ID partitioning is not ideal for geographical discovery at this scale.

This brings us to a fundamental architectural trade-off:

A partitioning strategy that is excellent for writes may be expensive for spatial reads.

We need to evolve our architecture.

# 10.4 Geographical partitioning — Optimize nearby searches

Now let's revisit the problem we discovered.

Driver-ID partitioning distributes writes efficiently, but every nearby search may need to query many partitions.

We need to optimize geographical reads.

Instead of partitioning the spatial index randomly by driver ID, we divide the world into geographical cells.

For example:

```
Hyderabad
    |
    +---- Cell A
    |
    +---- Cell B
    |
    +---- Cell C
    |
    +---- Cell D
    |
    +---- Cell E
```

We can use H3, geohash, or S2 to identify these cells.

Now suppose a customer requests a ride in Cell B.

Instead of querying all 256 driver partitions, we determine which geographical cells intersect the pickup radius.

```
Customer Request
       |
       v
Determine Relevant Cells
       |
       +---- Cell A
       +---- Cell B
       +---- Cell C
       |
       v
Query Only Those Cell Indexes
```

This reduces search fan-out.

### How should we physically distribute cells?

We can assign cells to regional Redis shards.

```
Redis Cluster
    |
    +---- Node A
    |       +---- Cell A
    |       +---- Cell B
    |       +---- Cell C
    |
    +---- Node B
    |       +---- Cell D
    |       +---- Cell E
    |       +---- Cell F
    |
    +---- Node C
            +---- Cell G
            +---- Cell H
            +---- Cell I
```

The matching service has a spatial routing table:

```
Cell A -> Redis Node A
Cell B -> Redis Node A
Cell C -> Redis Node A
Cell D -> Redis Node B
...
```

The important point:

Geographic cells are logical partitions. Physical Redis nodes are storage owners. These two concepts should remain separate.

We might move Cell C from Node A to Node D without changing its geographical identity.

This makes rebalancing easier.

### But what happens when a driver moves?

Suppose:

```
D101:
    Old position -> Cell B
    New position -> Cell C
```

We need to update the geographical index.

A safe movement protocol is:

```
1. Validate the new location using the driver's version.
2. Determine the new geographical cell.
3. Insert the driver into the new cell.
4. Ensure the new membership is queryable.
5. Remove the old membership.
6. Reconcile incomplete movement operations.
```

Why insert first?

Because we prefer a temporary duplicate candidate over a period in which the driver is absent from both cells.

But this introduces an important distributed consistency issue.

Cell B and Cell C may reside on different Redis nodes.

We cannot assume one Redis Lua script can atomically modify both.

We solve this using versioned, idempotent index updates and a clearly defined location owner.

For example:

```
Driver D101

Latest accepted version = (session 11, sequence 102)

Cell B:
    D101 -> version 101

Cell C:
    D101 -> version 102
```

A query might temporarily find D101 in both cells.

The matching service checks the latest driver location and version before using the candidate.

An asynchronous cleanup process removes the obsolete entry from Cell B.

### A more mature architecture — Separate location ownership from spatial indexing

At very large scale, consider two logical layers:

```
              DRIVER LOCATION UPDATE
                        |
                        v
                Driver Location Owner
                        |
                 Validate version
                        |
                        v
              Latest Location State
                        |
                        v
                Spatial Index Projector
                        |
             +----------+----------+
             |                     |
             v                     v
         Cell B Index          Cell C Index
```

The driver-location owner partitions by driver ID.

It is responsible for accepting the newest valid location.

The spatial index is a geographical projection that supports efficient queries.

This is useful because:

- One component controls per-driver update ordering.
- Another component optimizes geographical searches.
- Index updates can be retried.
- Stale cell memberships can be reconciled.

However, the projection must be maintained within an explicit freshness and consistency budget.

For latency-sensitive matching, a location update should not be considered geographically searchable until the required index update is available.

A common implementation is to synchronously update the new cell index before acknowledging the location as indexed, then remove the old membership asynchronously.

If we use a fully asynchronous projector, matching must account for projection lag and possible false negatives.

This is an important trade-off: a faster asynchronous projection can temporarily hide a newly moved driver from the new cell.

# 10.5 Hot geographical cells — What if one area becomes extremely popular?

Let's imagine a major event in Hyderabad.

A stadium releases thousands of people.

Many drivers move into the same geographical area.

Our spatial partitioning might look like:

```
Cell A -> 500 drivers
Cell B -> 700 drivers
Cell C -> 80,000 drivers
Cell D -> 600 drivers
```

Cell C becomes a hotspot.

Why?

Because the geographical distribution of drivers is rarely uniform.

A popular city center, airport, railway station, stadium, or shopping district can contain far more drivers than surrounding cells.

If Cell C is stored as one Redis key, that key can become a bottleneck again.

We have simply moved the hotspot from one giant city-wide GEO key to one giant cell key.

### Solution 1: Dynamically subdivide dense cells

Suppose Cell C is an H3 cell.

We can subdivide the operational area further using:

- A higher H3 resolution.
- Geohash subcells.
- Quadtree subdivision.
- Custom secondary partitions.

Conceptually:

```
Cell C
  |
  +---- C1
  |
  +---- C2
  |
  +---- C3
  |
  +---- C4
```

Now:

```
C1 -> 20,000 drivers
C2 -> 18,000 drivers
C3 -> 22,000 drivers
C4 -> 20,000 drivers
```

The load becomes more manageable.

### Solution 2: Split a hot cell across multiple storage shards

Sometimes we want to retain the same logical geographical cell but distribute its members.

For example:

```
Cell C
   |
   +---- Shard 0
   +---- Shard 1
   +---- Shard 2
   +---- Shard 3
```

This introduces search fan-out within Cell C.

We query all of its subshards and merge results.

### Which should we choose?

| Strategy                     | Best when                                              |
| ---------------------------- | ------------------------------------------------------ |
| Subdivide geographical cells | Search density varies geographically                   |
| Split one cell across shards | A logical cell itself is too large                     |
| Combine both                 | Extremely dense areas with high write and read traffic |

### Dynamic splitting requires migration

Suppose Cell C was previously stored on Node A.

We decide to split it.

```
Old:
Cell C -> Node A

New:
C1 -> Node B
C2 -> Node C
C3 -> Node D
C4 -> Node E
```

We need to migrate memberships.

A controlled process might be:

1. Create the new subcell indexes.
2. Begin dual-writing accepted location changes to the old and new representations.
3. Backfill existing drivers into their new subcells.
4. Verify membership counts and versions.
5. Switch query routing to the new representation.
6. Stop old writes.
7. Remove the old index.

During migration, reads should avoid treating the old and new indexes as independent sources of additional drivers. Deduplicate results using driver ID and version.

Don't split hot cells by simply moving data and hoping queries will find the new location. Rebalancing needs a coordinated routing and migration protocol.

# 10.6 Scaling the WebSocket infrastructure independently

Now let's scale the communication layer.

Suppose:

```
Connected drivers = 800,000
```

These are persistent connections.

We deploy multiple WebSocket gateway servers.

```
                  L4 Load Balancer
                          |
           +--------------+--------------+
           |              |              |
           v              v              v
        Gateway A      Gateway B      Gateway C
           |              |              |
        10k sockets     10k sockets     10k sockets
```

The numbers are illustrative.

The correct gateway capacity depends on:

- Active connection count.
- Memory per connection.
- File descriptor limits.
- TCP buffers.
- TLS overhead.
- Ping/Pong frequency.
- Outbound message rate.
- Network bandwidth.
- Message serialization cost.
- CPU utilization.

### Why use an L4 load balancer?

WebSocket connections are long-lived TCP connections.

An L4 load balancer can distribute new connections across gateway instances without interpreting every application message.

An L7 load balancer can also support WebSockets, terminate TLS, and apply HTTP-level routing policies.

The choice depends on the infrastructure.

The important thing is that once a WebSocket is established, its connection remains attached to the gateway that accepted it.

### Should we use round-robin load balancing?

Round-robin might distribute connection attempts reasonably well initially.

But it doesn't always distribute active load evenly.

Imagine:

```
Gateway A:
    30,000 connections

Gateway B:
    10,000 connections

Gateway C:
    15,000 connections
```

The gateways have unequal connection counts.

We may use:

- Least-connections routing.
- Capacity-aware routing.
- Connection-count monitoring.
- CPU and network-aware scaling.
- Regional gateway pools.

However, connection count alone is not a perfect load metric.

One client might generate far more outbound messages than another.

### Autoscaling signals

For WebSocket gateways, monitor:

| Metric                | Why it matters                                  |
| --------------------- | ----------------------------------------------- |
| Active connections    | Measures connection capacity                    |
| New connections/sec   | Detects reconnect bursts                        |
| Messages/sec          | Measures actual workload                        |
| Outbound bandwidth    | Can become the bottleneck                       |
| CPU usage             | Detects processing pressure                     |
| Memory per connection | Helps determine capacity                        |
| Event-loop lag        | Detects overloaded gateway processes            |
| Failed sends          | Detects connection and network problems         |
| Reconnect rate        | Detects network instability or gateway failures |

When a gateway approaches its capacity, scale out before it reaches saturation.

### What happens when adding gateways?

Suppose:

```
Gateway A
Gateway B
Gateway C
```

We add:

```
Gateway D
Gateway E
```

Existing WebSocket connections don't automatically migrate.

New connections can be routed to D and E.

Existing drivers remain connected to their original gateways until they disconnect or are deliberately migrated through a reconnect protocol.

This is why gateway deployment needs graceful draining.

### Graceful gateway shutdown

Suppose we want to deploy a new version of Gateway A.

Instead of killing it immediately:

```
1. Mark Gateway A as draining.
2. Stop accepting new connections.
3. Notify clients that reconnect may be required.
4. Allow a bounded drain period.
5. Close remaining connections.
6. Clients reconnect to healthy gateways.
7. Remove Gateway A's connection registrations.
```

Because the business offer state is durable, connections can be recreated without losing assignments or active offers.

# 10.7 The connection registry becomes a major distributed system

With 800,000 simultaneous connections, the connection registry is not just a small lookup table anymore.

We have:

```
D101 -> Gateway A
D102 -> Gateway B
D103 -> Gateway C
...
D800000 -> Gateway Z
```

We need to distribute this registry.

A natural approach is to partition it by driver ID.

```
Connection Registry
       |
       +---- Partition 0
       +---- Partition 1
       +---- Partition 2
       +---- ...
       +---- Partition N
```

Each entry has a lease.

```
driverId
gatewayId
connectionId
sessionEpoch
leaseExpiry
```

Gateway heartbeats refresh the lease.

### Why not keep one registry in one Redis key?

Because the registry should be independently distributable.

Use individual keys or partitioned structures.

For example:

```
connection:{p0}:D101
connection:{p1}:D102
connection:{p2}:D103
```

The exact structure depends on how the registry is queried and maintained.

### What happens during massive disconnects?

Suppose one gateway crashes with 30,000 connected drivers.

We might receive:

```
30,000 reconnect attempts
```

almost simultaneously.

We should:

- Use client reconnect jitter.
- Expire old connection leases.
- Avoid flooding the registry with repeated writes.
- Route new connections to gateways with capacity.
- Re-register drivers using fresh session information.
- Recover active offer state from durable storage.

The connection registry must never be mistaken for the assignment database.

A gateway crash doesn't mean that 30,000 drivers' active rides should be cancelled.

# 10.8 Scaling the matching engine independently

Now let's return to nearby discovery.

Suppose:

```
Ride requests/sec = 10,000
```

We deploy multiple matching service instances.

```
Customer Requests
        |
        v
 Matching Load Balancer
        |
        +------------+------------+
        |            |            |
        v            v            v
   Matcher A    Matcher B    Matcher C
        |            |            |
        +------------+------------+
                     |
                     v
              Regional Spatial Index
```

The matching services should generally be stateless.

They receive:

```
rideId
pickupCoordinates
searchRadius
requirements
```

They retrieve candidate data, rank candidates, and return a result to the dispatch orchestrator.

### What should we partition the matching workload by?

A few useful options:

Option A: Ride ID

```
hash(rideId) -> matching worker
```

Distributes independent ride requests.

Option B: Geographic region

```
Hyderabad -> Matcher Pool A
Bangalore -> Matcher Pool B
Mumbai -> Matcher Pool C
```

Improves data locality and regional isolation.

Option C: Combination

First route to the region, then distribute ride requests across workers using ride ID.

This is a good general design.

```
             Customer Request
                     |
                     v
               Region Router
                     |
             +-------+-------+
             |       |       |
             v       v       v
          Hyderabad Bangalore Mumbai
             |       |       |
             v       v       v
          Matcher Matcher Matcher
```

### Do we need one matching worker per geographical cell?

Usually not.

That would introduce unnecessary operational complexity.

A matching service can process requests from multiple cells while the spatial index determines which data partitions to query.

We can introduce finer-grained work ownership only when a particular region becomes too hot.

# 10.9 Handling thousands of nearby searches in the same neighbourhood

Imagine a concert ends.

At the same time:

```
5,000 customers request rides
```

Many of them are standing within a relatively small geographical area.

Their queries might look like:

```
Request 1 -> Pickup near Cell A
Request 2 -> Pickup near Cell A
Request 3 -> Pickup near Cell A
Request 4 -> Pickup near Cell B
Request 5 -> Pickup near Cell A
...
```

We might repeatedly query nearly identical geographical areas.

This is a read hotspot.

Our location write architecture might be healthy, but nearby discovery can become the bottleneck.

Let's explore optimizations.

## A. Short-lived candidate caching

Suppose we maintain a cache:

```
Cell A:
    D101
    D102
    D103
    D104
    D105
```

A new ride request in Cell A can use this list as an initial candidate shortlist.

For example:

```
Request 1 -> Cell A -> Cache miss -> Build list

Request 2 -> Cell A -> Cache hit

Request 3 -> Cell A -> Cache hit

Request 4 -> Cell A -> Cache hit
```

This reduces repeated spatial lookups.

But remember:

The cached list is not the authoritative availability state.

A driver could have accepted another ride one millisecond ago.

So the flow is:

```
Cached candidate IDs
        |
        v
Latest operational state
        |
        v
Freshness validation
        |
        v
Atomic reservation
```

Only the candidate discovery result is cached.

Never cache a conclusion such as:

```
D101 is available for every request for the next 5 seconds.
```

Availability and reservation can change immediately.

### How long should we cache?

It depends on:

- Location update frequency.
- Driver speed.
- Candidate density.
- Matching latency targets.
- Request volume.

A short TTL may help reduce repeated queries.

But the actual value must be measured.

## B. Cache nearby cell membership, not individual ride matches

A better abstraction is sometimes:

```
Cell A:
    Recently discovered candidates
```

Then multiple ride requests can reuse the candidate IDs.

Why not cache the final ranked list?

Because ETA, availability, service type, pickup requirements, and current driver workload may differ between requests.

We can cache the expensive geographical discovery stage and rerun request-specific filtering.

## C. Batch candidate state retrieval

Suppose GEO search returns:

```
D101
D102
D103
D104
...
D150
```

Don't call Redis once per driver:

```
GET D101
GET D102
GET D103
...
```

That's the N+1 query problem.

Instead, perform bulk metadata retrieval using pipelining or an appropriately partitioned multi-get.

We can reduce round trips substantially.

Also, group the requests by the Redis shard that owns each driver's metadata.

## D. Batch ETA calculations

If we have 30 shortlisted drivers, use a route-matrix operation rather than issuing 30 independent routing requests where the routing provider or engine supports batching.

## E. Request coalescing

Sometimes many requests ask for a similar expensive computation.

We can share the geographical candidate-discovery result across these requests while keeping the final eligibility and assignment decisions request-specific.

This can significantly reduce redundant search work.

But be careful:

Two customers requesting a ride in the same cell do not get to share a driver reservation.

Discovery can be shared.

Business ownership cannot.

# 10.10 Scaling dispatch orchestration

The matching engine returns:

```
Ride R501:

D101
D103
D105
D107
```

The dispatch orchestrator now needs to create offers and manage their lifecycles.

Suppose 10,000 ride requests arrive per second.

We could have many dispatch orchestrator instances.

```
Dispatch Requests
       |
       v
Dispatch Router
       |
       +----> Orchestrator A
       |
       +----> Orchestrator B
       |
       +----> Orchestrator C
```

A useful design is to assign a logical owner to each ride workflow.

For example:

```
hash(rideId) % N
```

This can help serialize orchestration commands for a ride.

But don't confuse this with assignment correctness.

Even if one orchestrator owns a ride, the durable database still enforces the assignment invariant.

Why?

Because:

- The orchestrator might crash.
- Its lease might expire.
- A duplicate worker might execute.
- A delayed command might arrive.
- A failover might temporarily produce two owners.

The owner assignment is an optimization for orderly workflow execution.

The database transaction is the correctness boundary.

### How should pending dispatch workflows recover?

Suppose Orchestrator A owns R501.

It crashes.

The dispatch state is durable:

```
R501:
    SEARCHING

O101:
    PENDING

O102:
    PENDING
```

A replacement orchestrator can recover the workflow.

It checks:

- Whether the ride is still searchable.
- Which offers are still valid.
- Whether any driver accepted.
- Whether any offers expired.
- Whether the next batch should be dispatched.

It should not blindly restart every offer.

It resumes from the durable state.

# 10.11 Scaling the location event stream

Now suppose we need durable location history.

We use Kafka.

```
Location Ingestion
       |
       v
   Kafka Topic
       |
       +------> Redis Projection
       |
       +------> Historical Store
       |
       +------> Analytics
```

How should we scale Kafka?

### A. Partition by driver ID

Use:

```
Kafka message key = driverId
```

Updates from the same driver normally go to the same partition.

For example:

```
Partition 0:
    D101
    D105
    D110

Partition 1:
    D102
    D106

Partition 2:
    D103
    D107
```

This gives us useful per-driver ordering.

### B. Partition count controls parallelism

Suppose a topic has eight partitions.

A consumer group can have up to eight active consumers processing those partitions concurrently.

Adding 20 consumers doesn't create 20-way parallelism for a topic with only eight partitions.

Additional consumers will be idle for that group.

So we choose partition count based on:

- Throughput.
- Desired consumer parallelism.
- Number of drivers.
- Ordering requirements.
- Future expansion.
- Broker capacity.

But excessively large partition counts also have operational costs.

We need a measured balance.

### C. Consumer groups

Consider:

```
LOCATION_TOPIC
       |
       +------> Consumer Group A: Redis Updater
       |
       +------> Consumer Group B: Historical Writer
       |
       +------> Consumer Group C: Analytics
```

Each consumer group tracks its own progress.

A Redis updater falling behind doesn't automatically prevent the historical writer from making progress.

This is useful isolation.

### D. Avoid putting every event type into one giant topic by default

Location data, assignment events, offer events, and financial events have different:

- Retention requirements.
- Ordering requirements.
- Throughput profiles.
- Consumer groups.
- Security requirements.

Separate topics or streams can make operational ownership clearer.

For example:

```
driver-location-events
ride-assignment-events
driver-offer-events
driver-session-events
```

We don't necessarily need one topic per event type.

Choose boundaries based on shared retention and processing requirements.

### E. Kafka ordering is per partition

This is important.

Kafka does not provide global ordering across all partitions.

If we need ordering for a driver, key by driver ID.

If we need ordering for a ride workflow, key by ride ID.

Different ordering requirements may require different event streams or processing strategies.

For example:

```
Driver location events:
    Key by driverId.

Ride assignment events:
    Key by rideId.
```

The same event might be republished into a downstream topic using a different key when a different ordering boundary is required.

# 10.12 Scaling the ETA and routing service

One overlooked bottleneck in location matching is the routing engine.

Suppose:

```
Ride requests/sec = 10,000

Drivers evaluated for ETA per request = 30
```

We potentially need:

\\[ 10,000 \times 30 = 300,000 \\]

driver-to-pickup ETA evaluations per second.

That's a separate heavy workload.

We should not assume the routing provider can serve this demand for free.

### Strategy 1: Use cheap spatial ranking first

```
1,000 geographical candidates
        |
        v
Cheap filtering
        |
        v
50 candidates
        |
        v
Straight-line shortlist
        |
        v
10-20 ETA candidates
```

The exact shortlist size depends on ranking quality and route complexity.

### Strategy 2: Regional routing engines

Instead of sending every request to one central routing system:

```
Hyderabad -> Routing Cluster A
Bangalore -> Routing Cluster B
Mumbai -> Routing Cluster C
```

This improves geographic locality.

### Strategy 3: Route matrix batching

Calculate several candidate ETAs in one request when supported.

### Strategy 4: Short-lived ETA caching

Cache route estimates for repeated combinations of:

```
pickup geographical cell
driver geographical cell
time bucket
```

But be careful.

Traffic conditions and road accessibility can change.

An ETA cache is an approximation, not a permanent fact.

### Strategy 5: Graceful fallback

If routing is temporarily unavailable:

- Use straight-line distance for a preliminary ranking.
- Use historical travel-time estimates where appropriate.
- Continue with a reduced-quality dispatch policy if business requirements permit.

Don't block all assignments indefinitely just because the routing engine is unavailable.

# 10.13 Scaling the assignment database

Now comes an important distinction.

Our location data volume is enormous.

But assignment volume is usually much lower.

We shouldn't automatically shard the transactional database just because Redis is sharded.

Start with one authoritative relational database deployment per operational market or region, depending on scale and consistency requirements.

```
Assignment Service
       |
       v
Transactional Database
       |
       +---- Rides
       +---- Drivers
       +---- Offers
       +---- Assignments
       +---- Outbox
```

Scale initially through:

- Proper indexes.
- Connection pooling.
- Short transactions.
- Read replicas for noncritical reporting.
- Partitioning large history tables.
- Archiving.
- Vertical scaling where appropriate.

Assignment correctness reads must go to the authoritative writer, not a potentially lagging replica.

### When do we need database sharding?

Suppose one market becomes too large for one transactional database.

We can divide it into multiple consistency domains.

For example:

```
Market A
    |
    +---- Assignment Shard 0
    +---- Assignment Shard 1
    +---- Assignment Shard 2
```

But there is a subtle problem.

Remember our two invariants:

```
One ride -> At most one active driver

One driver -> At most one active ride
```

Suppose rides are sharded by `rideId`.

And drivers are sharded by `driverId`.

The ride and driver could reside on different database shards.

Now a transaction attempting to reserve the driver and assign the ride would cross shards.

A normal relational database transaction cannot atomically cover arbitrary independent database shards.

This is a real architectural trade-off.

### Three approaches

Approach A: Keep both records inside one transactional consistency domain.

For example, partition assignment data by operational market or dispatch region.

Every assignment and driver reservation for that market happens in the same transactional database.

This is usually the simplest choice.

Approach B: Use a single assignment authority per driver/market.

Route all competing assignments for a driver through their authoritative owner.

That owner serializes the driver's reservation.

The ride assignment is then coordinated with the driver's reservation using a durable workflow.

This introduces distributed state transitions and recovery logic.

Approach C: Introduce a distributed transaction protocol.

This is possible in certain environments but adds substantial complexity, availability implications, and operational requirements.

For most ride-hailing systems, we should avoid introducing distributed transactions simply to scale assignment throughput.

### My recommendation

Keep assignment within a regional or market-level transactional consistency boundary for as long as practical.

Location ingestion and spatial indexing can be scaled far more aggressively and independently.

The assignment database is unlikely to need the same massive write throughput as the raw GPS ingestion system.

# 10.14 Multi-region architecture — Now things get interesting

So far, we've mostly discussed a single-region system.

But a ride-hailing application can operate across multiple regions.

For example:

```
India
  |
  +---- Hyderabad
  |
  +---- Bangalore
  |
  +---- Mumbai
  |
  +---- Delhi

Other markets
  |
  +---- Singapore
  |
  +---- London
  |
  +---- New York
```

Why do we need multiple regions?

- Lower latency.
- Geographic locality.
- Regional failure isolation.
- Data residency requirements.
- Independent scaling.
- Disaster recovery.

But multi-region architecture introduces a new question:

Where should location data, driver state, ride state, and assignment ownership live?

We must not assume that all these things need the same replication strategy.

## 10.15 First distinguish four different regions

This distinction will make multi-region architecture much easier to understand.

Consider D101.

The driver is currently working near Hyderabad.

There are four possible regional concepts:

| Region concept              | Meaning                                                                        |
| --------------------------- | ------------------------------------------------------------------------------ |
| Ingestion region            | Where the mobile request first reaches the backend                             |
| Location storage region     | Where the latest location is maintained                                        |
| Search region               | Which geographical infrastructure answers nearby searches                      |
| Assignment authority region | Which transactional system decides whether the driver and ride can be assigned |

These regions can be the same, but they don't have to be.

For example:

```
D101's mobile application
        |
        v
Nearest API Gateway: Mumbai
        |
        v
Location Owner: Hyderabad
        |
        v
Hyderabad Redis Index
        |
        v
Nearby Matching: Hyderabad
        |
        v
Assignment Authority: Hyderabad Market
```

Why might the API gateway and location owner differ?

The mobile application may connect to a nearby network edge, but the driver's current operational data belongs to a different regional partition.

The edge gateway can forward the update to its authoritative owner.

Alternatively, we can route the driver session to the appropriate region at connection establishment.

The important thing is that we establish a clear ownership model.

# 10.16 A good starting design — Regional location infrastructure

Let's divide the infrastructure by geography.

```
                       DRIVER APPS
                            |
                     Global Load Balancer
                            |
              +-------------+-------------+
              |             |             |
              v             v             v
          Hyderabad      Bangalore       Mumbai
           Region         Region         Region
              |             |              |
              v             v              v
          Location       Location        Location
          Ingestion      Ingestion       Ingestion
              |             |              |
              v             v              v
          Redis GEO      Redis GEO      Redis GEO
              |             |              |
              v             v              v
          Matching       Matching        Matching
```

Now Hyderabad traffic primarily stays in Hyderabad.

Bangalore traffic primarily stays in Bangalore.

This gives us:

- Lower latency.
- Less cross-region communication.
- Better regional fault isolation.
- Easier capacity planning.

We can also partition individual regions internally by geographical cells.

For example:

```
Hyderabad Region
     |
     +---- Cell Group A
     |        +---- Redis Shard 0
     |        +---- Redis Shard 1
     |
     +---- Cell Group B
     |        +---- Redis Shard 2
     |        +---- Redis Shard 3
     |
     +---- Cell Group C
              +---- Redis Shard 4
              +---- Redis Shard 5
```

This is our regional geographical indexing architecture.

But a driver can move.

What if D101 travels from Hyderabad into another operational region?

We need to handle ownership changes.

# 10.17 How should a driver moving between regions work?

Imagine:

```
D101 initially:

Region = Hyderabad
Cell = H-A
Location = Hyderabad
```

Later:

```
D101 moves:

Region = Bangalore
Cell = B-C
Location = Bangalore
```

We need to move the driver's live location ownership.

There are several possible designs.

### Strategy A: Always keep a home-region owner

Assign each driver a logical home region.

For example:

```
D101 -> Hyderabad Home Region
```

Even when the driver travels to Bangalore, the authoritative per-driver location state may remain in Hyderabad.

The Hyderabad location owner receives updates and maintains the latest location.

The geographical index can place the driver in Bangalore's spatial search projection.

Advantages:

- Driver identity has stable ownership.
- Sequence validation has one authoritative location owner.
- No need to change the driver's authoritative owner for every boundary crossing.

Disadvantages:

- Long-distance roaming creates cross-region writes.
- Location updates may travel farther than necessary.
- Nearby queries in Bangalore may need to access a remote location owner.

This is simple conceptually but may not provide ideal geographical locality.

### Strategy B: Transfer location ownership when the driver moves regions

Initially:

```
D101 -> Hyderabad Owner
```

Now the driver enters Bangalore.

We transfer ownership:

```
D101 -> Bangalore Owner
```

The transfer must be controlled.

A safe conceptual process:

```
1. Detect regional movement.
2. Establish new ownership generation.
3. Stop accepting writes under old ownership.
4. Transfer latest accepted location and session metadata.
5. Register the new location owner.
6. Update the geographical index.
7. Reject stale writes from the old owner.
```

This requires fencing.

Suppose:

```
Old owner epoch = 10
New owner epoch = 11
```

The new owner receives epoch 11.

Any delayed operation from epoch 10 must be rejected.

Otherwise, the old region might continue writing while the new region is also accepting updates.

### Which approach should we choose?

For most systems, a stable home identity owner combined with geographical search projections is a reasonable starting model.

Transfer physical ownership only when the operational benefit justifies the complexity.

Also, don't transfer ownership just because one GPS point crossed a geographical boundary.

GPS accuracy and boundary oscillation can cause drivers to move between cells or regions repeatedly.

Use a stable region or ownership policy with hysteresis.

For example, require the driver to remain in a new operational region for a defined period before transferring location ownership.

This prevents unnecessary migration.

# 10.18 Cross-region nearby searches

Now suppose a customer requests a ride near the Hyderabad-Bangalore service boundary.

The customer's pickup is in Hyderabad.

But a suitable driver is physically located in the nearby Bangalore region.

Should we only search Hyderabad?

Not necessarily.

Service boundaries and geographical ownership boundaries are not always the same.

We can perform a federated nearby search.

```
              Ride Request: Hyderabad
                          |
                          v
                    Search Router
                          |
                 +--------+--------+
                 |                 |
                 v                 v
           Hyderabad Search   Bangalore Search
                 |                 |
                 v                 v
           Local Candidates   Local Candidates
                 |                 |
                 +--------+--------+
                          |
                          v
                    Merge Results
                          |
                          v
                  Exact Validation
                          |
                          v
                     Ranking
```

The router identifies which regions intersect the requested geographical search area.

It queries only those regions.

It does not search the entire country or world.

### How do we merge the results?

Suppose:

```
Hyderabad:
    D101 -> 1.2 km
    D102 -> 2.0 km

Bangalore:
    D103 -> 1.5 km
    D104 -> 4.0 km
```

Our customer is within 3 km of:

```
D101
D102
D103
```

D104 is eliminated after exact distance validation.

We then apply:

- Freshness.
- Availability.
- Service compatibility.
- Reservation checks.
- ETA ranking.

### What happens if Bangalore is unavailable?

We must distinguish:

```
Bangalore Search = 0 candidates
```

from:

```
Bangalore Search = Failed
```

If Bangalore failed, our search coverage is incomplete.

In a dense region, the Hyderabad candidates might be sufficient.

Near a border with sparse supply, failure to search Bangalore might materially reduce matching quality.

We should track the completeness of the federated search and apply an appropriate degraded-service policy.

### Important: Don't query every region for every ride

Use geographical coverage to select only regions whose search areas intersect the requested radius.

This is the same spatial pruning principle we learned in Chapter 4, applied at the regional level.

# 10.19 Cross-region location replication — Do we need every driver in every region?

No.

This is a common misconception.

Suppose we have:

```
Hyderabad Region
Bangalore Region
Mumbai Region
Delhi Region
```

Does every region need the latest location of every driver in the country?

Definitely not.

A Hyderabad ride request has no reason to query every Mumbai driver.

We should replicate location information selectively.

For example:

```
Driver D101:
    Authoritative location owner = Hyderabad

    Hyderabad:
        Full live location state

    Bangalore:
        Relevant geographical search projection, if needed

    Mumbai:
        No reason to replicate
```

### What should be replicated across regional boundaries?

Depending on the search architecture:

- Driver ID.
- Latest coordinates.
- Location version.
- Location received timestamp.
- Service eligibility metadata.
- Possibly availability as a non-authoritative hint.

Avoid replicating unnecessary full driver profiles or sensitive information.

The destination region should still validate the latest authoritative state before dispatch.

### Should availability be synchronously replicated?

Usually not for discovery.

A regional spatial index can use an eventually consistent availability hint.

But the actual reservation and assignment must go through the authoritative assignment mechanism.

For example:

```
Bangalore projection:

D101 -> Available
```

The driver might already have been reserved in the authoritative system.

The projection is stale.

The reservation transaction rejects the attempt.

This is why a cross-region spatial index is for finding potential candidates, not granting ownership.

# 10.20 The hardest multi-region question — Who owns the assignment?

This is where we must be especially careful.

Imagine:

```
Ride R501:
    Pickup in Hyderabad

Driver D101:
    Home location owner in Bangalore

Driver D102:
    Home location owner in Hyderabad
```

The ride might find both drivers.

Now both are candidates.

Where do we perform the final assignment?

We need one authoritative consistency boundary that can enforce:

```
Ride R501 -> At most one driver

Driver D101 -> At most one active assignment

Driver D102 -> At most one active assignment
```

There are a few approaches.

### Pattern A: Market-level transactional assignment authority

Define an operational market that owns the assignment workflow.

For example:

```
Hyderabad Service Market
        |
        v
Assignment Authority
        |
        +---- Ride records
        +---- Driver reservations
        +---- Active assignments
        +---- Assignment history
```

All rides and drivers eligible within that market are coordinated through the same transactional authority.

Even if a driver's location is ingested or indexed in another region, the market's assignment database is responsible for reserving and assigning them.

This is often the simplest approach.

The location infrastructure can be geographically distributed, while the transactional assignment domain remains logically centralized for that market.

### Pattern B: Driver-owner reservation protocol

Now imagine assignment volume has become large enough that driver state is partitioned across different authoritative owners.

We might have:

```
Ride Authority:
    Owns R501

Driver Authority:
    Owns D101
```

These are separate transactional systems.

We can no longer assume one ACID transaction can update both.

We need a coordinated workflow.

One possible protocol:

```
1. Dispatch coordinator requests a durable reservation
   from D101's authoritative owner.

2. Driver owner atomically reserves D101 using a unique token.

3. Coordinator claims R501 in its authoritative database.

4. Coordinator commits the assignment decision.

5. Driver owner confirms D101's reservation as a BUSY assignment.

6. If any step fails, coordinator runs recovery or compensation.
```

The important part is that the reservation is durable and exclusive.

If two rides try to reserve D101, only one driver-owner transaction succeeds.

If the coordinator crashes halfway through, the reservation remains identifiable using its token and can be recovered.

This is effectively a distributed workflow or saga.

We need states such as:

```
DRIVER_RESERVATION_REQUESTED
DRIVER_RESERVED
RIDE_CLAIMED
ASSIGNMENT_CONFIRMED
```

The system must not expose a final assignment until the required workflow has completed.

This is more complex than a single database transaction.

It introduces:

- Cross-service coordination.
- Durable workflow state.
- Timeouts.
- Compensation.
- Recovery.
- Reconciliation.

### Pattern C: Global assignment service

We can also maintain one logically authoritative assignment service for a broader market or fleet.

It owns the durable relationship:

```
Driver -> Active Assignment
Ride -> Assigned Driver
```

This can simplify correctness but introduces a scaling and availability boundary.

It might use multiple replicas, but writes for the same assignment domain must still be coordinated by a consistent authority.

### Which approach should we choose?

My recommendation:

| Scale                                       | Recommended assignment model                                                             |
| ------------------------------------------- | ---------------------------------------------------------------------------------------- |
| Initial implementation                      | Single transactional database                                                            |
| Large city                                  | Regional/market assignment database                                                      |
| Multiple cities with independent operations | One assignment authority per market                                                      |
| Very large, distributed assignment domain   | Driver ownership + durable reservation protocol or another explicitly coordinated design |

Do not geographically shard the assignment database without understanding how cross-shard driver/ride ownership affects atomicity.

It is relatively easy to distribute location reads and writes.

It is much harder to preserve global assignment invariants across independent transactional databases.

# 10.21 Active-active vs active-passive — Which architecture should we use?

Now let's discuss two common multi-region deployment patterns.

## A. Active-active

Multiple regions actively serve traffic.

```
              Global Load Balancer
                      |
             +--------+--------+
             |                 |
             v                 v
         Region A          Region B
             |                 |
             v                 v
         Ingestion          Ingestion
             |                 |
             v                 v
         Redis GEO          Redis GEO
             |                 |
             v                 v
          Matching          Matching
```

Both regions handle live traffic.

Advantages:

- Lower latency for regional clients.
- Better regional fault isolation.
- Capacity distributed across regions.
- Regional traffic can continue without routing every operation to one distant region.

But there is an important issue.

Suppose D101 has a session in Region A.

Due to a routing problem, Region B also accepts a new session.

Now:

```
Region A:
    D101 location = A
    sessionEpoch = 10

Region B:
    D101 location = B
    sessionEpoch = 11
```

Which update should win?

We need a single authoritative owner for D101's current session or a globally coordinated versioning mechanism.

Simply allowing both regions to accept independent writes creates conflicting state.

For live location, a single-writer-per-driver policy is a very useful starting point.

Both regions can serve different drivers simultaneously.

But one driver should have only one current authoritative session generation.

### What about the spatial projections?

They can be replicated asynchronously.

For example:

```
Driver location owner:
    Region A

Primary location index:
    Region A

Search projection:
    Region B
```

Region B can discover the driver, but final dispatch revalidates current state through the authoritative owner.

This preserves regional read scalability without pretending that all replicas are strongly consistent.

## B. Active-passive

One region handles normal traffic.

Another region stands by for disaster recovery.

```
              Traffic Router
                    |
                    v
              Primary Region
                    |
                    v
               All Writes

              Backup Region
                    |
                    v
               Standby
```

If the primary fails, we promote the standby.

Advantages:

- Simpler write ownership.
- Easier consistency reasoning.
- Fewer simultaneous writers.
- More predictable database authority.

Disadvantages:

- Failover can take longer.
- Backup resources may be underutilized.
- Clients might need to reconnect.
- Replication lag can affect recovery-point objectives.

### What is the better choice?

It depends on workload.

For location ingestion, active-active can work well with regional ownership and versioned replication.

For business assignment, we need a clearly defined authoritative writer.

We shouldn't blindly make every part of the system active-active.

A hybrid architecture is often more sensible.

# 10.22 The split-brain problem — The most dangerous multi-region failure

Imagine:

```
Region A:
    Assignment database primary

Region B:
    Assignment database replica
```

The network between regions fails.

Both regions believe they should accept assignments.

Now:

```
Region A assigns R501 to D101.

Region B assigns R501 to D102.
```

We have two conflicting assignments.

This is called split brain.

It violates our fundamental correctness invariant.

How do we prevent it?

### Use a single authoritative writer

Only one region should be able to commit assignments for a given consistency domain at a time.

The other region can be:

- A standby.
- A read replica.
- A secondary that waits for safe promotion.

For automatic failover, the system needs a reliable leader-election or consensus mechanism.

### Why is a simple lease insufficient?

Suppose Region A's leadership lease expires.

Region B becomes the new leader.

But Region A is still running and doesn't know its lease has expired.

It might continue writing.

We need fencing.

For example:

```
Region A:
    Leadership epoch = 10

Region B:
    Leadership epoch = 11
```

Every authoritative write carries the current fencing epoch.

The storage system rejects writes from old epochs.

The old leader cannot continue making authoritative changes.

The mechanism that grants leadership and the storage system enforcing the fence must be designed consistently. Merely attaching an arbitrary timestamp to a request isn't sufficient.

### What if we cannot safely establish a new leader?

Stop authoritative assignment writes temporarily.

This is an intentional availability trade-off.

We may continue serving cached or noncritical reads, tracking locations, and accepting retryable requests.

But we must not permit two competing assignment authorities.

For ride assignment, consistency is more important than accepting writes independently in two disconnected regions.

# 10.23 Multi-region location ingestion during a regional outage

Now let's distinguish location ingestion from assignment.

Suppose Region A is unavailable.

Drivers that were connected to Region A reconnect to Region B.

What happens?

```
Before:

D101 -> Region A
D102 -> Region A

After outage:

D101 -> Region B
D102 -> Region B
```

We should:

1. Authenticate the reconnect.
2. Establish a new authoritative session or ownership epoch.
3. Fence stale Region A sessions.
4. Accept fresh location observations.
5. Update the Region B geographical index.
6. Replicate appropriate location projections.
7. Mark drivers eligible only after fresh location and business-state checks.

What about previously active rides?

They remain owned by the assignment authority.

A location-region failure should not automatically cancel them.

If the assignment authority is also in Region A, then its failover follows the stronger transactional failover procedure described above.

These are independent failure domains.

### Recovery objectives

We should define:

RTO — Recovery Time Objective

How quickly must the service recover?

RPO — Recovery Point Objective

How much accepted data can we afford to lose?

For example:

```
Live location:
    RPO may be a few seconds.

Ride assignment:
    RPO should be zero for acknowledged committed assignments,
    according to the chosen database durability contract.

WebSocket connection:
    No need to preserve the connection.
    Reconnect instead.

Location history:
    Depends on whether every event was promised durable acceptance.
```

This is why different components may have different disaster-recovery strategies.

# 10.24 Capacity planning — Calculate before scaling

Let's make the scale concrete.

Assume:

```
Online drivers = 1,000,000

Update frequency = 3 seconds

Location updates = 333,333/sec
```

Now let's consider different workloads.

### A. Location ingress

If the average payload is 200 bytes:

\\[ 333,333 \times 200 \approx 66.7 \text{ MB/sec} \\]

This is approximately 5.76 TB of raw payload per day.

This is only a rough payload calculation.

Actual network consumption will be higher because of protocol overhead, headers, authentication, metadata, retries, and serialization.

Historical event storage can grow substantially once replication and indexes are included.

This is another reason not to store every GPS update permanently unless the business requires it.

### B. WebSocket connections

Assume:

```
Connected drivers = 800,000

Gateway capacity target = 20,000 active connections
```

A simple calculation gives:

\\[ \frac{800,000}{20,000}=40 \\]

We would need at least 40 gateways under that illustrative connection target.

But we also need headroom for uneven load and failures.

For example, if we need to survive one gateway failure without overloading the others, we provision additional capacity.

The real benchmark must include:

- Connection memory.
- Heartbeat frequency.
- TLS.
- Incoming messages.
- Outgoing offers.
- Reconnection bursts.
- Network bandwidth.

### C. Redis shards

Assume a realistic benchmark gives us:

```
35,000 location updates/sec per Redis primary
```

Then:

\\[ \frac{333,333}{35,000}\approx9.52 \\]

A theoretical minimum is 10 primaries.

But we need additional capacity for:

- GEO searches.
- Metadata updates.
- Lua execution.
- Replication.
- Cleanup.
- Failure recovery.
- Traffic spikes.

We might provision considerably more after load testing.

### D. Matching throughput

Assume:

```
Ride requests = 10,000/sec
```

Suppose each request searches six relevant geographical partitions.

Then:

\\[ 10,000 \times 6 = 60,000 \\]

Approximately 60,000 geographical index queries per second.

If each request then performs 20 ETA calculations:

\\[ 10,000 \times 20 = 200,000 \\]

Potential ETA evaluations per second.

This shows why routing needs its own scaling strategy.

### E. Assignment throughput

Suppose each ride attempt creates:

```
2 initial offers
```

And there are 10,000 ride requests per second.

We might have:

```
20,000 offer creation attempts/sec
```

But actual database transaction throughput depends on:

- Reservation frequency.
- Acceptance success rate.
- Timeout and cancellation frequency.
- Index design.
- Transaction duration.
- Lock contention.
- Number of independent database consistency domains.

We should benchmark the actual workflow.

The important observation:

The assignment database has a different workload from the location database. Do not assume it needs to process hundreds of thousands of GPS updates per second.

# 10.25 How do we know which component needs scaling?

This is an important production engineering question.

Suppose the system becomes slow.

Simply adding more application servers may not help.

We need to identify the bottleneck.

Let's map symptoms to likely causes.

| Symptom                                     | Possible bottleneck                | What to investigate                              |
| ------------------------------------------- | ---------------------------------- | ------------------------------------------------ |
| Location ingestion latency increasing       | Ingestion or Redis saturation      | Request queueing, Redis latency, CPU, network    |
| Redis writes are fast but matching is slow  | Spatial search bottleneck          | Fan-out, result size, partition hotspots         |
| Only one city is slow                       | Geographical hotspot               | Cell occupancy and regional capacity             |
| Driver offers are delayed                   | Gateway routing or queue backlog   | Outbox age, broker lag, gateway throughput       |
| WebSocket disconnects increasing            | Gateway saturation                 | Memory, file descriptors, event-loop lag         |
| ETA calculations are slow                   | Routing service                    | Matrix throughput, route cache, provider latency |
| Assignment failures increasing              | Database contention                | Row locks, deadlocks, transaction latency        |
| Some drivers appear twice in GEO            | Index movement or cleanup problem  | Membership versions and reconciliation           |
| Nearby candidates frequently stale          | Location ingestion or consumer lag | Freshness distribution and Kafka lag             |
| Regional failover causes duplicate sessions | Ownership fencing problem          | Session epoch and leader ownership               |

We should not monitor only CPU and memory.

We need workload-specific metrics.

## Metrics I would monitor

### Location tracking

```
location_updates_received_per_second
location_updates_accepted_per_second
location_updates_rejected_per_second
location_update_processing_latency
location_age_p50
location_age_p95
location_age_p99
stale_driver_count
out_of_order_update_count
duplicate_update_count
```

### Redis

```
redis_command_latency
redis_cpu_usage
redis_memory_usage
redis_network_throughput
redis_evicted_keys
redis_replication_lag
redis_failovers
geo_search_latency
partition_request_distribution
```

### Matching

```
matching_requests_per_second
candidate_count_per_search
eligible_candidate_count
search_radius_expansion_count
geo_partition_fanout
partial_search_count
matching_latency
eta_latency
no_driver_found_rate
```

### Dispatch

```
offers_created_per_second
offer_delivery_latency
offer_acknowledgement_rate
offer_acceptance_rate
offer_expiration_rate
offer_rejection_rate
pending_offer_count
oldest_pending_offer_age
```

### Assignment

```
assignment_success_rate
assignment_conflict_rate
assignment_transaction_latency
database_lock_wait_time
deadlock_count
idempotency_replay_count
active_assignment_invariant_violations
```

### WebSocket infrastructure

```
active_connections
connections_per_gateway
connection_establishment_rate
reconnection_rate
outbound_message_rate
message_delivery_failure_rate
heartbeat_timeout_rate
```

### Kafka, if used

```
consumer_lag
producer_error_rate
broker_disk_usage
partition_distribution
under_replicated_partitions
oldest_unprocessed_event_age
```

The most important principle:

Scale based on the actual workload and its latency/freshness SLOs, not simply on the total number of users.

# 10.26 A better way to think about scaling — Independent consistency domains

Let's consolidate our architecture around ownership.

Every kind of data should have a clear owner.

| Data                         | Logical owner            | Replication strategy                                    |
| ---------------------------- | ------------------------ | ------------------------------------------------------- |
| Driver's current location    | Location owner/region    | Asynchronous geographical projections where appropriate |
| Spatial cell index           | Cell/partition owner     | Rebuildable projection                                  |
| WebSocket connection         | Gateway instance         | Temporary connection lease                              |
| Driver online intent         | Business state authority | Durable database replication                            |
| Temporary driver reservation | Assignment authority     | Strong enough to prevent conflicting claims             |
| Ride assignment              | Assignment authority     | Durable, authoritative                                  |
| Historical location events   | Event stream/storage     | Durable replication according to retention contract     |
| Matching cache               | Matching region          | Rebuildable, short-lived                                |

This leads to a useful rule:

Distribute the data freely where eventual consistency is acceptable. Establish explicit ownership where competing writes could violate business invariants.

For example:

- A driver location can be copied into several regional search indexes.
- A driver's active assignment cannot independently be decided by several regions.
- A WebSocket connection can move between gateways.
- The ride's assignment cannot move between competing authorities without a controlled ownership transfer.

This distinction is extremely valuable when designing multi-region systems.

# 10.27 The complete large-scale architecture

Let's combine everything we've learned.

This is an architecture for a large multi-region ride-hailing or delivery platform.

\#chatgpt-mermaid-\_r_51h\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_51h\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_51h\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_51h\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_51h\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_51h\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_51h\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_51h\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_51h\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_51h\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_51h\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_51h\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_51h\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_51h\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_51h\_ p{margin:0;}#chatgpt-mermaid-\_r_51h\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_51h\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_51h\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_51h\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_51h\_ .label text,#chatgpt-mermaid-\_r_51h\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_51h\_ .node rect,#chatgpt-mermaid-\_r_51h\_ .node circle,#chatgpt-mermaid-\_r_51h\_ .node ellipse,#chatgpt-mermaid-\_r_51h\_ .node polygon,#chatgpt-mermaid-\_r_51h\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_51h\_ .rough-node .label text,#chatgpt-mermaid-\_r_51h\_ .node .label text,#chatgpt-mermaid-\_r_51h\_ .image-shape .label,#chatgpt-mermaid-\_r_51h\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_51h\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_51h\_ .rough-node .label,#chatgpt-mermaid-\_r_51h\_ .node .label,#chatgpt-mermaid-\_r_51h\_ .image-shape .label,#chatgpt-mermaid-\_r_51h\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_51h\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_51h\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_51h\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_51h\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_51h\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_51h\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_51h\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_51h\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_51h\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_51h\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_51h\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_51h\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_51h\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_51h\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_51h\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_51h\_ .icon-shape,#chatgpt-mermaid-\_r_51h\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_51h\_ .icon-shape p,#chatgpt-mermaid-\_r_51h\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_51h\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_51h\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_51h\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_51h\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_51h\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_51h\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_51h\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_51h\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_51h\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_51h\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_51h\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_51h\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_51h\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_51h\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_51h\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_51h\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_51h\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_51h\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_51h\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_51h\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_51h\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_51h\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_51h\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_51h\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_51h\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_51h\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_51h\_ .node rect,#chatgpt-mermaid-\_r_51h\_ .node circle,#chatgpt-mermaid-\_r_51h\_ .node ellipse,#chatgpt-mermaid-\_r_51h\_ .node polygon,#chatgpt-mermaid-\_r_51h\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_51h\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_51h\_ .mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_51h\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_51h\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_51h\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}Optional Durable TrackingAuthoritative Business StateMatching and DispatchLive Location InfrastructureRegional Ingestion InfrastructureMobile ApplicationsKafka Location StreamHistorical Location StoreAnalytics ConsumersTransactional AssignmentDatabaseTransactional OutboxDelivery QueueMatching EngineRouting and ETA ServiceDispatch OrchestratorDriver Location OwnersGeographical Index RouterRedis Cell ShardsRedis Cell ShardsRedis Cell ShardsRegional Load BalancerLocation Ingestion FleetWebSocket Gateway FleetConnection RegistryDriver AppsCustomer Apps

Let's understand the responsibilities one final time.

### Location layer

Handles:

- High-throughput location ingestion.
- Session and sequence validation.
- Latest location storage.
- Geographical indexing.
- Location freshness.
- Regional location ownership.

### Matching layer

Handles:

- Spatial candidate discovery.
- Eligibility filtering.
- Radius expansion.
- ETA calculations.
- Candidate ranking.

### Dispatch layer

Handles:

- Candidate selection.
- Temporary reservations.
- Offer creation.
- Offer expiration.
- Workflow recovery.

### Real-time communication layer

Handles:

- WebSocket connections.
- Connection registry.
- Message routing.
- Application acknowledgements.
- Reconnection.
- Push notification fallback.

### Assignment layer

Handles:

- Durable reservations.
- Offer acceptance.
- Conditional state transitions.
- Assignment uniqueness.
- Reassignment history.
- Transactional outbox.

### Historical data layer

Handles:

- Durable location events when required.
- Route reconstruction.
- Auditing.
- Analytics.
- Historical queries.

These are logical responsibilities. They don't necessarily need to become separate microservices on day one.

# 10.28 How I would evolve this architecture in real life

Let's build the architecture incrementally rather than starting with every sophisticated component.

| Stage | Architecture                                | Why we introduce it                           |
| ----- | ------------------------------------------- | --------------------------------------------- |
| 1     | Ingestion + single Redis GEO                | Establish live tracking                       |
| 2     | Multiple ingestion servers                  | Handle more incoming connections and requests |
| 3     | Redis GEO partitioning                      | Distribute writes                             |
| 4     | Spatial cells and regional indexing         | Reduce nearby-search fan-out                  |
| 5     | Separate matching and dispatch services     | Isolate discovery from business workflows     |
| 6     | WebSocket gateway fleet                     | Scale real-time communication                 |
| 7     | Connection registry                         | Route messages to the correct gateway         |
| 8     | Database reservations and atomic assignment | Prevent double booking                        |
| 9     | Outbox and message broker                   | Recover notifications and downstream events   |
| 10    | Kafka for required event history            | Preserve and replay location events           |
| 11    | Regional infrastructure                     | Reduce latency and isolate failures           |
| 12    | Multi-region ownership and failover         | Support global operations                     |

Notice the order.

We introduce complexity when a concrete bottleneck or requirement appears.

For example:

- Don't use Kafka simply because write traffic is high.
- Don't introduce H3 if Redis GEO already satisfies the search workload.
- Don't add distributed locks if conditional database updates are sufficient.
- Don't introduce multi-region active-active assignments without a clear consistency model.
- Don't persist every GPS update if only the latest location matters.

# 10.29 Important design decisions by scale

Let's make the trade-offs explicit.

| Question              | Initial answer                            | Large-scale evolution                                                 |
| --------------------- | ----------------------------------------- | --------------------------------------------------------------------- |
| Location transport    | HTTP or WebSocket                         | Regional ingestion fleets                                             |
| Live location storage | Redis GEO                                 | Partitioned regional/cell indexes                                     |
| Location durability   | Fresh updates can rebuild state           | Kafka when historical completeness is needed                          |
| Spatial indexing      | Redis GEO                                 | H3/S2/geohash partition routing when needed                           |
| Nearby searches       | Parallel GEO queries                      | Geographic cell routing, candidate caching                            |
| Matching              | One service                               | Regionally scaled stateless workers                                   |
| Dispatch              | Small batches                             | Durable distributed orchestration                                     |
| Driver communication  | WebSockets                                | Connection registry and gateway-specific routing                      |
| Reservations          | Database conditional updates              | Partitioned ownership or coordinated reservation protocol             |
| Assignment            | Single transactional DB                   | Market-level assignment authorities or carefully coordinated sharding |
| Driver liveness       | Location timestamp + connection heartbeat | Distributed leases and regional monitoring                            |
| Event publication     | Transactional outbox                      | Partitioned publishers and broker consumers                           |
| Historical location   | Optional                                  | Dedicated event stream and storage pipeline                           |
| Disaster recovery     | Reconnect and rebuild                     | Regional failover, fencing, RPO/RTO policies                          |

# 10.30 Common scaling mistakes to avoid

These are mistakes I would specifically look out for in a system design interview.

1\. Adding Redis nodes without partitioning keys.

One enormous GEO key remains on one Redis Cluster primary.

2\. Partitioning everything by driver ID.

Write distribution improves, but nearby queries might fan out to every partition.

3\. Partitioning everything geographically without handling movement.

Drivers cross geographical cells. Index updates need versioning, retries, and reconciliation.

4\. Putting Kafka between every component.

Kafka is useful for durable event streams, not automatically necessary for every internal communication.

5\. Making every service active-active.

Location ingestion can often operate with regional ownership and asynchronous projections. Assignment requires a single authoritative decision for each consistency domain.

6\. Sharding rides and drivers independently without a cross-shard assignment protocol.

The assignment transaction must enforce both ride uniqueness and driver uniqueness.

7\. Treating cached availability as authoritative.

A cached driver can become busy immediately after discovery. Reserve through the authoritative mechanism.

8\. Scaling using CPU alone.

A WebSocket gateway might be limited by file descriptors or network bandwidth, while Redis might be limited by memory or command latency.

9\. Replicating every driver's full state everywhere.

Replicate only the information required by the receiving region.

10\. Assuming a region can immediately become the new primary after a network failure.

Old writers must be fenced before a new authoritative writer takes over.

# Chapter 10 — Final mental model

We can now think about the system in five independent scaling dimensions.

```
                  LARGE-SCALE SYSTEM
                          |
       +------------------+------------------+
       |                  |                  |
       v                  v                  v
   WRITE SCALE        READ SCALE        CONNECTION SCALE
       |                  |                  |
       v                  v                  v
  Ingestion Fleet    Geo Partitions     WS Gateways
  Redis Shards       Spatial Routing    Connection Registry
  Kafka (optional)   Candidate Cache    Message Routing

                          |
                 +--------+--------+
                 |                 |
                 v                 v
           WORKFLOW SCALE     DURABILITY SCALE
                 |                 |
                 v                 v
           Dispatch Fleet      Transactional DB
           Offer Workers      Outbox / Inbox
           Timeout Workers    Kafka / History
```

And across regions:

```
               GLOBAL PLATFORM
                     |
          +----------+----------+
          |          |          |
          v          v          v
        Region A   Region B   Region C
          |          |          |
          v          v          v
       Local      Local      Local
       Location   Location   Location
       Index      Index      Index
          |          |          |
          +----------+----------+
                     |
              Federated Search

       Assignment Authorities:
          Clearly owned transactional
          consistency domains
```

The biggest insight from this chapter:

Location tracking, nearby discovery, WebSocket connections, dispatch, and assignment must scale independently because they have different bottlenecks and different consistency requirements.

# Next: Chapter 11 — Real-World Architecture Patterns and Variations

Now we'll compare the actual system designs for different products.

So far, we've built a generic architecture. But Uber, Swiggy, Zomato, logistics platforms, and home-service platforms don't necessarily use the same matching strategy.

We'll take the same core building blocks and modify them according to business requirements.

| Pattern                                | Main focus                                                                       |
| -------------------------------------- | -------------------------------------------------------------------------------- |
| 11.1 Uber/Ola/Rapido                   | Nearest suitable driver, fast pickup, low assignment latency                     |
| 11.2 Swiggy/Zomato                     | Restaurant preparation time, delivery partner availability, batching             |
| 11.3 Multi-order delivery batching     | Assigning compatible orders to one delivery partner                              |
| 11.4 Logistics/fleet management        | Vehicle capacity, routes, delivery windows, long-distance tracking               |
| 11.5 Home services                     | Skills, service areas, appointment scheduling                                    |
| 11.6 Driver repositioning              | Predicting demand and suggesting where drivers should move                       |
| 11.7 Supply-demand balancing           | Avoiding overcrowding and improving regional availability                        |
| 11.8 Marketplace matching              | Ranking based on ETA, acceptance probability, fairness, and business constraints |
| 11.9 Different durability requirements | Which location events must be retained for each product?                         |
| 11.10 Different dispatch strategies    | Sequential, parallel, batch, deferred, and optimized assignment                  |

We'll also address some important concepts that haven't been explored deeply yet:

- Why delivery partner batching is an optimization problem.
- How to avoid assigning orders that produce excessive detours.
- How to predict available supply in a region.
- How to handle drivers who reject many offers.
- Whether to optimize for nearest driver, fastest pickup, or highest successful-completion probability.
- How assignment policies change when a driver can carry multiple orders.

The objective is to understand which parts of our core architecture remain unchanged and which parts must evolve with the business.

Say next to continue.
