# Reliable Location Tracking & Real-Time Matching
## Interview-Ready System Design Revision Guide

A practical revision guide for designing Uber/Ola/Rapido-style ride matching, Swiggy/Zomato-style delivery assignment, logistics tracking, and other location-based marketplaces.

---

# 1. Core Mental Model

The most important thing is to recognize that this is not one problem. It is a collection of independent problems with different performance and consistency requirements.

| Problem | What it answers | Primary responsibility |
|---|---|---|
| Location ingestion | Where is the driver? | Receive location updates |
| Location storage | Where is the latest location maintained? | Fast live-location index |
| Discovery | Who is geographically nearby? | Spatial queries |
| Eligibility | Who can accept this request? | Business filtering |
| Matching | Who is the best candidate? | Ranking and optimization |
| Dispatch | Whom should we contact and when? | Offer orchestration |
| Reservation | Who temporarily owns this opportunity? | Prevent conflicting offers |
| Assignment | Who is officially assigned? | Atomic business transaction |
| Delivery | How do we communicate the decision? | WebSockets, push, queues |
| Liveness | Is the driver's information trustworthy? | Heartbeats and freshness |

**Fundamental distinction:**

- Location can be overwritten.
- Search results can be recomputed.
- Connections can be recreated.
- Offers can be retried.
- Assignments must be durably and correctly recorded.

Never apply the same reliability guarantee to all these operations.

## 1.1 Separate data according to its purpose

| Data | Example | Required characteristics | Suggested storage |
|---|---|---|---|
| Latest location | Driver's current coordinates | Fast, frequently overwritten | Redis |
| Spatial index | Drivers in geographical cells | Fast geographical search | Redis GEO / H3-based index |
| Connection state | Driver connected to Gateway A | Temporary, expires | Gateway memory + Redis registry |
| Driver availability | Available, busy, reserved | Correct state transitions | Transactional database |
| Offer | Ride offered to D101 | Durable while valid | Database |
| Assignment | R501 assigned to D101 | Atomic and durable | Transactional database |
| Location history | Previous trip coordinates | Durability as required | Kafka + historical storage |

**Mental model:** Redis maintains the current operational view. Kafka, when justified, preserves event history. The transactional database owns business-critical state.

---

# 2. Requirements and Capacity Estimation

Before designing, clarify the workload.

### Important questions

**Location**
- How often does each driver send GPS updates?
- How fresh must a location be?
- Can intermediate updates be discarded?
- Is historical route reconstruction required?

**Matching**
- Do we need the nearest driver or fastest-to-arrive driver?
- What is the initial and maximum search radius?
- Can multiple drivers receive an offer?
- Is radius expansion required?

**Assignment**
- Can one driver have multiple active tasks?
- Must a ride have exactly one successful assignment?
- What happens when an offer expires?
- Is reassignment supported?

**Reliability**
- Which data can be lost?
- Which messages need retries?
- What happens when Redis or the assignment database fails?
- What recovery time and data-loss limits are acceptable?

### Example scale assumptions

```text
Registered drivers              10 million
Online drivers                   1 million
Location interval                3 seconds
Concurrent ride requests         10,000
Connected WebSocket clients      800,000
Cities                           100+
Regions                          Multiple
```

Location write throughput:

\[
WPS = \frac{1,000,000}{3}
\approx 333,333 \text{ writes/sec}
\]

If each location payload averages 200 bytes:

\[
333,333 \times 200
\approx 66.7 \text{ MB/sec}
\]

Approximately 5.76 TB of raw payload per day.

This excludes network overhead, replication, indexing, and historical storage overhead.

**Conclusion:** Persisting every idle GPS update permanently in an ordinary relational table is generally unnecessary.

---

# 3. Location Ingestion Architecture

## 3.1 Communication choices

| Technology | Communication | Appropriate use |
|---|---|---|
| HTTP | Request/response | Periodic location updates |
| WebSocket | Bidirectional, persistent | Real-time offers, commands, location streaming |
| SSE | Server to client | Server-to-client event streams |
| Push notifications | Best-effort server-to-device | Background notifications |
| gRPC streaming | Bidirectional/unidirectional streams | Efficient streaming communication |
| MQTT | Broker-based publish/subscribe | Lightweight IoT/device messaging |

HTTP doesn't necessarily create a new TCP connection for every update. Keep-alive, HTTP/2, and connection reuse make periodic HTTP ingestion perfectly reasonable.

**Good starting design:**

- HTTP for location ingestion.
- WebSockets for real-time offers and communication.
- Push notifications as a background fallback.
- Acceptance through HTTP or WebSocket commands.

Communication and storage are separate responsibilities.

## 3.2 Ingestion flow

```text
Driver App
    |
    v
Regional Load Balancer
    |
    v
Stateless Ingestion Servers
    |
    v
Authentication and Validation
    |
    v
Session / Sequence Validation
    |
    v
Atomic Location Update
    |
    v
Redis GEO + Metadata
    |
    v
ACK
```

Each ingestion server can process any driver because the shared storage contains the authoritative latest operational projection.

HTTP requests don't need sticky sessions. Established WebSocket connections naturally remain attached to their owning gateway.

## 3.3 Prevent out-of-order updates

Example:

```text
Update A: sequence = 101
Update B: sequence = 102
```

Network delays cause B to arrive first, then A.

Without protection, A might overwrite B.

Use:

```text
(driverId, sessionEpoch, sequenceNumber)
```

Accept an update only when its version is newer than the stored version.

Conceptually:

```text
ATOMIC UPDATE:

if incomingEpoch < storedEpoch:
    reject

if incomingEpoch == storedEpoch
   and incomingSequence <= storedSequence:
    reject

otherwise:
    update location
    update GEO index
    update freshness metadata
```

The comparison and update must happen atomically, for example with a Redis Lua script operating on co-located keys.

Session epochs prevent messages from an old application session overwriting new-session data.

Don't trust device wall-clock timestamps alone for event ordering. Device clocks can drift.

## 3.4 Reduce unnecessary writes

Mobile-side sampling can reduce traffic.

Example:

```text
Send an update when:

Distance moved > 10 metres

OR

Time since previous update > 5 seconds
```

The time condition ensures stationary drivers still report liveness.

Reporting frequency can adapt to movement, battery usage, and whether the driver is idle or carrying a customer/order.

The server must still validate all updates.

## 3.5 Batching and coalescing

For live location, after a network outage:

```text
D101:
    Location 101
    Location 102
    Location 103
    Location 104
```

Usually only Location 104 matters.

We can discard obsolete pending location updates.

This is called **coalescing/latest-value compression**.

For historical route reconstruction, however, intermediate events may need to be retained.

Do not apply live-location dropping to data whose history is contractually important.

## 3.6 Acknowledgement boundary

| ACK point | Guarantee |
|---|---|
| Immediately upon receiving request | Request reached server; storage not guaranteed |
| After Redis succeeds | Accepted into live operational index |
| After Kafka durably acknowledges | Accepted into durable event stream according to configuration |

Always define what an ACK means.

---

# 4. Redis, Kafka and High-Throughput Storage

## 4.1 Redis GEO

Redis GEO provides:

- `GEOADD`
- `GEOSEARCH`
- `GEOPOS`
- `GEODIST`
- `GEOHASH`

Example:

```redis
GEOADD geo:{p3} 78.4867 17.3850 D101
```

Important: longitude comes before latitude.

Nearby search:

```redis
GEOSEARCH geo:{p3}
    FROMLONLAT 78.4867 17.3850
    BYRADIUS 3 km
    WITHDIST
    ASC
```

Redis GEO is implemented using sorted-set-based geospatial indexing with encoded geographical values.

It is a useful geographic search abstraction, not a navigation engine.

## 4.2 Why one huge GEO key is a bottleneck

Redis Cluster distributes keys across hash slots, not members within a single key.

This:

```text
drivers:locations
    D101
    D102
    D103
    ...
    D1,000,000
```

remains one key on one primary.

Adding more Redis Cluster nodes doesn't split that GEO key.

Solution: partition the GEO index into multiple keys.

## 4.3 Partitioning strategies

### A. Driver-ID partitioning

```text
partition = hash(driverId) % N
```

Example:

```text
D101 -> P3
D102 -> P8
D103 -> P3
```

```text
geo:{p0}
geo:{p1}
geo:{p2}
...
geo:{pN}
```

Keys sharing a Redis Cluster hash tag, such as `{p3}`, are colocated.

This allows related driver metadata and GEO operations to share an atomicity boundary.

**Advantages**
- Distributes writes.
- Stable per-driver ownership.
- Easier atomic version checking.

**Disadvantages**
- Nearby searches may need to query many partitions.
- Search fan-out increases with partition count.

### B. Geographic partitioning

Use geographical cells:

```text
Cell A
Cell B
Cell C
Cell D
...
```

Each cell holds the drivers currently within that geographical region.

A query in Cell B searches only the cells intersecting its search radius.

**Advantages**
- Geographic locality.
- Less unnecessary search fan-out.
- Smaller candidate sets.

**Disadvantages**
- Drivers move between cells.
- Cross-node cell updates cannot automatically be one Redis transaction.
- Requires versioning, retries, and reconciliation.

A practical move protocol:

1. Accept the new location under the driver's current version.
2. Insert into the new cell.
3. Make the new membership searchable.
4. Remove from the old cell.
5. Reconcile incomplete movements.

A temporary duplicate is usually safer than temporary absence, but candidates must be validated against the latest accepted location.

### C. Hybrid partitioning

```text
Region
  |
  +---- Geographic Cell
            |
            +---- Partition 0
            +---- Partition 1
            +---- Partition 2
```

Use when individual geographical cells become too large.

**Evolution:** Start simple, introduce driver-ID partitioning for write distribution, geographical partitioning for search efficiency, and a hybrid only when required.

## 4.4 Redis metadata and stale-location cleanup

Redis GEO doesn't provide independent TTLs for individual GEO members.

Don't put an expiry on a shared GEO key to expire one driver.

Maintain:

```text
geo:{p3}                 -> Geospatial index
metadata:{p3}:D101       -> Location/version metadata
lastseen:{p3}            -> Sorted set of last-received timestamps
```

Use `ZADD` to refresh the last-seen timestamp.

A cleanup worker:

1. Finds members older than the freshness cutoff.
2. Rechecks their current timestamp/version atomically.
3. Removes only drivers that are still stale.
4. Deletes their metadata and GEO membership.

Matching must independently reject stale locations. It must not depend solely on cleanup workers.

## 4.5 Direct Redis vs Kafka-first

| Architecture | Advantages | Disadvantages | Choose when |
|---|---|---|---|
| Direct Redis | Lowest live-update latency, simple path | Redis directly affects ingestion availability | Latest location is the main requirement |
| Kafka-first | Durable events, replay, independent consumers | Consumer lag makes live projection stale | Every required location event must be preserved |
| Redis-first + Kafka | Fast live tracking plus asynchronous history | Dual-write inconsistency | History is best-effort |
| Durable stream-first | Clear durable acceptance boundary | Additional latency and infrastructure | Complete history is mandatory |

### Kafka-first flow

```text
Ingestion
    |
    v
Kafka
    |
    +----> Redis Projection
    |
    +----> Historical Storage
    |
    +----> Analytics
```

Use driver ID as Kafka message key for per-driver partition ordering.

Consumer flow:

```text
Read Kafka event
    |
    v
Idempotently update Redis
    |
    v
Commit Kafka offset
```

If the consumer crashes after Redis succeeds but before offset commit, the event is replayed. Version-aware Redis updates make that safe.

Kafka ordering alone does not eliminate duplicates or old-session events.

### Dual-write warning

```text
Redis SUCCESS
Kafka FAILURE
```

Or:

```text
Kafka SUCCESS
Redis FAILURE
```

Two independent writes are not atomic.

When historical completeness is mandatory, stream-first ingestion is often easier to reason about than independent Redis and Kafka writes.

## 4.6 Redis throughput optimization

- Multiple GEO partitions.
- Pipeline independent commands to reduce network round trips.
- Use GEO batch operations where compatible with consistency requirements.
- Coalesce obsolete live updates.
- Use compact payloads.
- Bound connection pools and in-flight requests.
- Apply backpressure.
- Benchmark actual Lua/GEO/metadata operations, not just plain `SET`.

Redis capacity must be measured using realistic concurrent read/write workloads, replication, memory, network, and latency targets.

Redis replication is generally asynchronous. Failover can lose recently acknowledged writes. Persistence and `WAIT` can change recovery/replication behavior but don't automatically provide a strong transactional durability guarantee.

For live locations, rebuilding from fresh updates is often acceptable.

---

# 5. Geospatial Algorithms

## 5.1 Haversine distance

Latitude and longitude are angular coordinates, not ordinary X/Y distances.

One degree of latitude is approximately 111 km. Longitude distance varies with latitude.

For two geographic points, Haversine calculates approximate great-circle distance:

\[
a=\sin^2(\Delta\phi/2)+
\cos(\phi_1)\cos(\phi_2)\sin^2(\Delta\lambda/2)
\]

\[
c=2\operatorname{atan2}(\sqrt a,\sqrt{1-a})
\]

\[
d=R \times c
\]

Where:
- Angles are in radians.
- \(R\) is approximately 6,371 km.
- \(d\) is geographical distance.

For high-precision geodesic calculations, use an ellipsoidal Earth model.

## 5.2 Bounding boxes

A circle is harder to use for initial geometric filtering than a rectangle.

Create a conservative bounding box around the requested radius, retrieve possible candidates, then validate exact distance.

For short urban distances, approximate angular bounds are:

\[
\delta = r/R
\]

\[
\Delta latitude \approx \delta
\]

\[
\Delta longitude \approx \delta/\cos(\phi)
\]

Carefully handle poles and the International Date Line.

**Purpose:** Reduce the number of candidates before expensive exact calculations.

## 5.3 Geohash

Geohash converts coordinates into hierarchical Base32 strings.

It repeatedly subdivides longitude and latitude intervals and interleaves the binary decisions.

```text
Longer geohash -> Smaller cell
Shorter prefix  -> Larger containing region
```

Advantages:
- Simple.
- Hierarchical.
- Useful for geographical partition routing.

Limitations:
- Rectangular cells.
- Cell dimensions vary by latitude.
- Neighbouring-cell boundary problems.

**Never search only the customer's own geohash.** Include every cell intersecting the requested search region, then filter exact distance.

## 5.4 Quadtree

Recursively splits geographical space into four quadrants.

Dense areas get smaller cells; sparse areas can remain larger.

Advantages:
- Adaptive spatial subdivision.
- Useful for uneven geographic density.

Disadvantages:
- Complicated movement and rebalancing.
- More complex distributed ownership.

## 5.5 H3

Uber-developed hierarchical spatial indexing system using mostly hexagonal cells, with 12 pentagons at each resolution.

Features:
- Hierarchical cell IDs.
- Neighbour operations.
- Cell-to-location mapping.
- Geographic region covering.

`gridDisk(cell, k)` means grid distance, not a fixed radius in kilometres.

For correct radius search, use conservative polygon covering or a sufficiently large neighbourhood, then exact-distance filter.

## 5.6 S2

Google's spherical geometry indexing system.

Conceptually:
- Project Earth onto six cube faces.
- Recursively subdivide.
- Generate hierarchical cell IDs.
- Support spherical region covering and spatial queries.

## 5.7 Spatial index comparison

| Structure | Cell model | Useful when |
|---|---|---|
| Redis GEO | Geospatial sorted-set index | Convenient live radius search |
| Geohash | Hierarchical rectangles | Simple cell identification |
| Quadtree | Adaptive rectangles | Uneven geographical density |
| H3 | Hierarchical hexagonal grid | Neighbourhood-based geographic partitioning |
| S2 | Hierarchical spherical quadrilaterals | Spherical region covering |
| R-tree | Bounding-rectangle hierarchy | Complex spatial objects and range queries |

Don't combine multiple spatial indexes without a clear reason. Each additional projection creates write and consistency overhead.

## 5.8 Geographic distance vs road distance vs ETA

```text
Geographic distance:
    Straight line between coordinates.

Road distance:
    Distance through available roads.

ETA:
    Estimated travel time considering route,
    road restrictions and traffic.
```

The nearest driver may not be the fastest.

Use cheap spatial filtering first, shortlist candidates, then perform batched ETA calculations.

---

# 6. Nearby Driver Discovery and Matching

## 6.1 Three independent operations

```text
DISCOVERY:
    Who is geographically nearby?

ELIGIBILITY:
    Who can perform this request?

DISPATCH:
    Whom should we contact and when?
```

Don't put all three into one giant query.

## 6.2 Complete search algorithm

Input:

```text
Pickup = (17.3850, 78.4867)
Radius = 3 km
Vehicle = Bike
Required candidates = 10
```

Flow:

1. Determine the search region.
2. Identify intersecting cells/Redis partitions.
3. Query relevant indexes in parallel.
4. Merge and deduplicate driver IDs.
5. Retrieve current operational state in batches.
6. Reject stale locations.
7. Reject unavailable or incompatible drivers.
8. Recalculate exact geographic distance.
9. Shortlist candidates.
10. Calculate road ETA.
11. Rank candidates.
12. Return results to dispatch.

## 6.3 Top-K and overfetch

Suppose we want ten eligible drivers.

We fetch the ten geographically nearest drivers.

But eight might be busy or stale.

We need more candidates.

Use overfetch:

```text
Required eligible = 10
Initial geographical candidates = 50
Eligible after filtering = 14
Return best 10
```

Or iterative fetching until enough eligible candidates are found.

Important mathematical distinction:

Fetching top K from every partition is sufficient to determine the global geographic top K after merging.

It does not guarantee the global top K eligible drivers because filtering might eliminate many candidates.

Use `ASC` ordering when nearest-first order is required. Avoid Redis GEO's `ANY` optimization when exact nearest-result ordering matters.

## 6.4 Radius expansion

```text
2 km -> 3 km -> 5 km -> 8 km
```

Expand when insufficient suitable drivers are available.

Avoid querying the same candidates repeatedly. Maintain a set of discovered driver IDs.

Alternative: Search a bounded maximum radius once, divide results into distance bands, and progressively use farther candidates.

| Progressive search | Search once at maximum radius |
|---|---|
| Less initial work | Fewer repeated queries |
| More query operations | More initial candidates |
| Good in dense areas | Useful when searches are expensive |

Impose maximum radius, candidate count, latency, and ETA budgets.

## 6.5 Eligibility predicate

A typical ride-hailing predicate:

```text
Eligible =
    Authenticated
    AND Online Intent
    AND Location Fresh
    AND Business Available
    AND No Active Reservation
    AND No Active Assignment
    AND Vehicle Compatible
    AND Service Compatible
```

Connection state may be a separate dispatch requirement if an alternative notification channel exists.

## 6.6 Candidate ranking

Possible factors:

- Road ETA.
- Straight-line distance.
- Location age and accuracy.
- Vehicle compatibility.
- Acceptance probability.
- Driver reliability.
- Workload and fairness.
- Service restrictions.

Start with hard eligibility filtering, then deterministic ETA ranking.

More advanced systems can optimize the probability of successful pickup or expected total wait.

Do not let a predictive model become responsible for assignment correctness.

## 6.7 Search performance

Useful optimizations:
- Parallel partition queries.
- Geographic query routing.
- Candidate ID caching.
- Bulk metadata retrieval.
- Batched ETA calculation.
- Bounded concurrency and query deadlines.

Cache candidate IDs, not authoritative availability or assignment decisions.

A cached candidate might have accepted another ride one millisecond ago.

---

# 7. Real-Time Dispatch and Offer Delivery

## 7.1 An offer is not an assignment

```text
Ride R501
    |
    +---- O101 -> D101 -> EXPIRED
    |
    +---- O102 -> D103 -> REJECTED
    |
    +---- O103 -> D105 -> ACCEPTED
```

A driver receiving an offer does not mean the ride is confirmed.

The authoritative assignment workflow decides the winner.

## 7.2 Dispatch strategies

| Strategy | Advantage | Disadvantage |
|---|---|---|
| Sequential | Low notification volume and contention | Higher latency |
| Parallel | Fast responses | More conflicting accepts |
| Small batches | Balances latency and contention | Requires coordination |
| Staggered | Reduces unnecessary notifications | More orchestration complexity |

**Good starting choice:** Small-batch or staggered parallel dispatch.

## 7.3 Temporary driver reservation

Before offering a driver a ride, reserve them for that specific offer.

Example:

```redis
SET driver:D101:reservation O101 NX PX 15000
```

`NX` ensures the key is created only if absent. `PX` supplies the lease duration.

However, Redis reservations alone are not the final correctness guarantee. Failover or lease expiry can result in stale ownership.

For stronger business correctness, maintain the authoritative reservation in the transactional database.

Release a reservation conditionally:

```text
Delete only if stored reservation == O101.
```

An old cleanup task must never delete a newer offer's reservation.

## 7.4 WebSocket gateway architecture

```text
Load Balancer
     |
     +---- Gateway A -> D101, D104
     |
     +---- Gateway B -> D103
     |
     +---- Gateway C -> D102, D105
```

Each gateway owns only its local sockets.

A distributed connection registry stores:

```text
driverId
gatewayId
connectionId
sessionEpoch
leaseExpiry
```

Dispatch flow:

```text
Dispatch
    |
    v
Connection Registry
    |
    v
Find owning gateway
    |
    v
Internal RPC / Gateway-specific messaging
    |
    v
WebSocket Gateway
    |
    v
Driver
```

Redis Pub/Sub can be a low-latency notification mechanism but is ephemeral; messages can be lost when subscribers are unavailable.

A durable offer record must exist separately.

## 7.5 Message delivery states

```text
OFFER_CREATED
OFFER_QUEUED
SENT_TO_GATEWAY
DEVICE_ACKNOWLEDGED
ACCEPTED / REJECTED
```

Important:

`Sent to gateway != Received by device`

And:

`Received by application != Driver accepted`

Use stable offer and event IDs. The application should deduplicate repeated messages.

## 7.6 Reliable offer delivery

Use:

```text
Database transaction:
    Create offer
    Reserve driver
    Insert outbox event

Commit

Outbox publisher:
    Publish delivery event

Gateway router:
    Route to owning gateway

Driver:
    Receive and acknowledge
```

If publishing succeeds but the publisher crashes before marking the outbox record published, the event can be published again.

Idempotent consumers make this safe.

Use a queue for retryable delivery work or internal RPC where adequate. Kafka is not mandatory simply to send a WebSocket message.

## 7.7 Offer expiration

Each offer has an expiry time.

The timeout worker performs a conditional transition:

```sql
UPDATE driver_offers
SET status = 'EXPIRED'
WHERE offer_id = :offerId
  AND status = 'PENDING'
  AND expires_at <= clock_timestamp();
```

The authoritative acceptance path independently checks the expiry.

The timeout worker is cleanup and workflow progression, not the authority on validity.

If another driver wins:
- Mark remaining offers cancelled.
- Release their reservations conditionally.
- Publish cancellation events.
- Reject late acceptances based on durable state.

---

# 8. Reliable Assignment and Concurrency

This is the most important correctness section.

## 8.1 Two separate invariants

Ride-hailing:

```text
A ride has at most one active assignment.

A driver has at most one active ride.
```

Protecting only the ride is insufficient.

Two different rides could otherwise assign the same driver.

## 8.2 Why in-memory locks are insufficient

` synchronized ` or local JVM locks protect only one process.

Server A and Server B have independent memory.

Both can acquire their own local lock for R501.

Distributed correctness must be enforced by shared authoritative state.

## 8.3 Why Redis locks alone are insufficient

Redis:

```redis
SET ride:R501:lock token NX PX 10000
```

Can reduce contention, but:
- Lock may expire while an old holder continues.
- Asynchronous replication/failover can lose lock state.
- A stale owner can accidentally delete a newer lock unless release is conditional.

Use unique tokens and, where appropriate, fencing tokens.

Never depend on Redis lock ownership as the final assignment guarantee.

## 8.4 Database conditional update

This is the fundamental compare-and-set operation:

```sql
UPDATE rides
SET status = 'ASSIGNED',
    assigned_driver_id = 'D101'
WHERE ride_id = 'R501'
  AND status = 'SEARCHING'
  AND assigned_driver_id IS NULL;
```

Check affected rows:

- `1`: assignment claim succeeded.
- `0`: ride is no longer assignable.

Concurrent requests contend on the same row. Only one can successfully perform the transition.

## 8.5 Protect the driver too

```sql
UPDATE drivers
SET availability_status = 'BUSY',
    active_ride_id = 'R501',
    reserved_offer_id = NULL
WHERE driver_id = 'D101'
  AND availability_status = 'RESERVED_FOR_OFFER'
  AND reserved_offer_id = 'O101'
  AND active_ride_id IS NULL;
```

This prevents two different rides from claiming the same driver.

## 8.6 Perform assignment in one transaction

```text
BEGIN

1. Lock ride.
2. Lock offer.
3. Lock driver.
4. Validate offer ownership and expiry.
5. Validate ride is SEARCHING.
6. Validate driver reservation.
7. Claim driver.
8. Claim ride.
9. Mark offer ACCEPTED.
10. Cancel other pending offers.
11. Create assignment history.
12. Insert outbox events.

COMMIT
```

If any required step fails, rollback.

Use consistent lock ordering across acceptance, cancellation, and timeout workflows to reduce deadlocks.

Conditional updates and unique constraints provide additional protection.

PostgreSQL-style defense-in-depth constraints:

```sql
CREATE UNIQUE INDEX one_active_assignment_per_ride
ON ride_assignments(ride_id)
WHERE status IN ('ASSIGNED', 'IN_PROGRESS');

CREATE UNIQUE INDEX one_active_assignment_per_driver
ON ride_assignments(driver_id)
WHERE status IN ('ASSIGNED', 'IN_PROGRESS');
```

These support historical reassignment while preventing multiple active assignments.

## 8.7 Acceptance vs expiration race

Both operations compete to transition the same offer:

```text
PENDING -> ACCEPTED

OR

PENDING -> EXPIRED
```

Only one can win.

Use the authoritative database clock. In PostgreSQL, `clock_timestamp()` reflects current time when evaluated, unlike transaction-start `CURRENT_TIMESTAMP`.

## 8.8 Duplicate acceptance

Driver sends:

```json
{
  "requestId": "ACCEPT-991",
  "offerId": "O101"
}
```

All retries use the same request ID.

Store the result under a unique idempotency key.

If the original transaction committed but the response was lost, return its previous result.

Don't create a new assignment.

## 8.9 Transactional outbox

Business state and publication intent are written together:

```sql
BEGIN;

UPDATE rides ...;
UPDATE drivers ...;
UPDATE driver_offers ...;

INSERT INTO outbox_events (...);

COMMIT;
```

Publisher sends events asynchronously.

If publishing succeeds but marking the event as published fails, it may publish again.

Consumers must be idempotent.

## 8.10 Inbox pattern

Use a unique `(consumer_name, event_id)` record.

In the same database transaction:

```text
Insert processed-event record.

If duplicate:
    Skip.

Otherwise:
    Apply consumer's business changes.

Commit.
```

For external side effects, such as calling a push provider, account for ambiguous outcomes and the provider's own idempotency capabilities. Inbox processing doesn't create an atomic transaction with an external system.

**The practical guarantee is exactly-once business effect, not exactly-once network delivery.**

---

# 9. Driver Availability and Liveness

Never model everything using one `online` boolean.

## 9.1 Separate state dimensions

| Dimension | Example states | What it means |
|---|---|---|
| Connection | Connected, disconnected, unknown | Is communication channel responsive? |
| Location | Fresh, stale, unknown | Is the position recent enough? |
| Business availability | Offline, available, reserved, busy | Can the driver take work? |
| Assignment | None, assigned, in progress | Is there a current business obligation? |

A driver can be connected but have a stale GPS location.

A driver can be disconnected but still have a fresh recently received location.

A busy driver can continue reporting location.

These states must remain independent.

## 9.2 Three liveness signals

**Connection heartbeat**

WebSocket Ping/Pong detects an unresponsive connection.

**Application heartbeat**

Indicates that the application is communicating.

**Location update**

Confirms a recent location observation.

A location update may also serve as evidence of application activity, avoiding unnecessary separate heartbeats.

## 9.3 Failure detection is probabilistic

A missing heartbeat doesn't prove a device is dead.

It might be:
- Network delay.
- Packet loss.
- Mobile network switching.
- Application suspension.
- Gateway overload.

Use suspicion, timeouts, and grace periods rather than treating one missed heartbeat as definitive failure.

Example policies, which must be tuned:
- Location reports every 3 seconds; stale threshold 12–15 seconds.
- WebSocket pings every 20 seconds; connection timeout 40–60 seconds.
- Application heartbeat every 15 seconds; longer timeout.

## 9.4 Redis liveness

Use per-driver connection leases with TTL.

```text
connection:D101:
    gatewayId
    connectionId
    sessionEpoch
    TTL
```

A gateway periodically refreshes its lease.

If it crashes and stops refreshing, the lease expires.

Don't put TTL on a shared GEO key to expire individual drivers.

## 9.5 Driver freshness cleanup

Maintain a sorted set:

```text
lastseen:{partition}
    D101 -> timestamp
    D102 -> timestamp
    D103 -> timestamp
```

Cleanup worker:

1. Finds timestamps older than the cutoff.
2. Rechecks the latest value/version atomically.
3. Removes still-stale GEO membership and metadata.

Matching must independently reject stale drivers before cleanup executes.

## 9.6 Session epochs

When a driver reconnects:

```text
Old session = 10
New session = 11
```

Old heartbeats and location updates must not overwrite new state.

Delayed disconnect events must remove a registry entry only if the stored gateway, connection ID, and epoch still match the old connection.

## 9.7 Matchability

A useful predicate:

```text
Eligible =
    Authenticated
    AND Online Intent
    AND Location Fresh
    AND Business Available
    AND No Active Reservation
    AND No Active Assignment
    AND Service Compatible
```

Communication availability is evaluated separately according to whether we require an active WebSocket or permit push fallback.

Keep explicit online intent separate from effective matchability.

Loss of location does not imply a busy driver has completed or cancelled their assignment.

---

# 10. Failure Handling and Recovery

## 10.1 Delivery semantics

| Model | Meaning |
|---|---|
| At-most-once | Zero or one processing; possible loss |
| At-least-once | Retries; duplicates possible |
| Exactly-once effect | Repeated delivery produces one logical business effect |

Use at-least-once attempts plus idempotent processing for most reliable messaging.

## 10.2 Exponential backoff and jitter

\[
Delay = Random(0,\min(MaxDelay,BaseDelay \times 2^{attempt}))
\]

Why jitter?

Without it, millions of clients can retry simultaneously after an outage.

Also implement:
- Bounded attempts.
- Overall deadlines.
- Retry budgets.
- `Retry-After` handling.
- Clear retry ownership across service layers.

Don't retry permanent errors such as invalid input, expired offers, or already-assigned rides.

## 10.3 Failure matrix

| Failure | Recovery |
|---|---|
| Location ACK lost | Retry same session/sequence; Redis rejects duplicate |
| Out-of-order location | Reject older version |
| Redis outage | Retry latest location; rebuild index from fresh updates/events |
| Kafka outage | Follow durability contract and apply backpressure |
| Kafka consumer crash | Replay message; idempotent Redis update |
| Consumer lag | Scale consumers or coalesce only where allowed |
| WebSocket gateway crash | Clients reconnect; connection lease expires |
| Lost offer notification | Retry while offer valid |
| Duplicate offer | Deduplicate by offer/event ID |
| Lost acceptance response | Retry same idempotency key |
| Assignment DB outage | Do not confirm assignment until durable result is established |
| Timeout worker duplication | Conditional state transition |
| Outbox publisher crash | Retry pending records |
| Stale Redis availability | Validate database state; reconcile index |
| Regional partition | Fence old writer; maintain authoritative ownership |

## 10.4 Kafka offset ordering

For a non-transactional Redis sink:

```text
Read Kafka message
    |
    v
Idempotently update Redis
    |
    v
Commit offset
```

If Redis succeeds and the consumer crashes before committing the offset, the message is replayed safely.

Committing the offset first could permanently lose the Redis projection update.

Kafka transactions can provide exactly-once semantics for certain Kafka-to-Kafka workflows, but don't automatically make Redis or external database side effects exactly once.

## 10.5 Dead-letter queues

Repeatedly failing messages can move to a DLQ after bounded retries.

Preserve:
- Original event.
- Failure reason.
- Attempt count.
- Timestamp.

Alert and provide controlled replay.

An expired offer isn't necessarily a poison message; its delivery can simply be obsolete.

## 10.6 Circuit breakers and bulkheads

Circuit breaker:

```text
CLOSED -> OPEN -> HALF_OPEN -> CLOSED
```

Stops repeated calls to an unhealthy dependency and tests recovery.

Bulkheads isolate failure domains:
- Per-region worker pools.
- Per-partition limits.
- Independent connection pools.

Load shedding protects the system from unlimited work accumulation.

## 10.7 Reconciliation

Examples:

```text
Redis says D101 AVAILABLE
DB says D101 BUSY
```

or:

```text
Redis reservation O101 exists
DB says O101 CANCELLED
```

Reconciliation compares operational projections against authoritative state and repairs stale entries.

It is a safety net, not a replacement for transactional correctness.

---

# 11. Scaling and Multi-Region Architecture

## 11.1 Scale components independently

| Component | Dominant workload | Scaling method |
|---|---|---|
| Ingestion | High write throughput | Stateless horizontal scaling |
| Redis | GEO reads/writes and memory | Logical partitions and shards |
| Matching | Geographical queries and CPU | Regional workers, candidate caching |
| Routing | ETA calculations | Regional routing clusters, batching |
| WebSocket gateways | Persistent connections | Connection-aware horizontal scaling |
| Dispatch | Workflow processing | Partitioned orchestration and workers |
| Assignment DB | Transaction contention | Indexing, short transactions, market-level sharding |
| Kafka | Durable event ingestion | Partitioning and consumer groups |

## 11.2 Hot geographical cells

A stadium or airport may contain far more drivers than surrounding cells.

Solutions:
- Subdivide dense cells.
- Split a hot logical cell across shards.
- Use adaptive subdivision.
- Rebalance through a controlled migration protocol.

Don't create one new hotspot while trying to solve another.

## 11.3 WebSocket scaling

Each gateway owns its active sockets in local memory.

Connection registry maps driver ID to gateway ownership.

Important operational metrics:
- Active connections.
- Connections per gateway.
- Connection establishment rate.
- Outbound message rate.
- Reconnection rate.
- Memory per connection.
- Event-loop lag.
- Failed sends.

Use graceful draining when deploying gateways:
1. Stop accepting new connections.
2. Notify clients.
3. Allow a bounded drain period.
4. Close remaining sockets.
5. Reconnect clients elsewhere.

## 11.4 Matching scaling

Use region routing, stateless workers, and relevant-cell lookup.

Cache geographical candidate IDs when many requests overlap.

Always revalidate freshness and availability.

Route-matrix batching and ETA caching can reduce expensive routing operations.

## 11.5 Kafka scaling

Partition by driver ID for location events.

Partition by ride ID for ride-workflow events when per-ride ordering is needed.

Each consumer group maintains independent progress.

Consumer count cannot increase useful parallelism beyond the available partitions in its group.

Monitor lag and partition skew.

## 11.6 Database scaling

Don't shard the assignment database merely because Redis needs sharding.

Assignments are a much lower-volume workload than raw GPS ingestion.

Start with:
- Appropriate indexes.
- Short transactions.
- Connection pooling.
- Read replicas for noncritical reporting.
- Historical-table partitioning and archival.

If sharding is required, recognize the central problem:

Ride records and driver records may end up on different database shards, making the assignment transaction cross-shard.

Prefer a market-level transactional authority when possible.

Otherwise, use an explicit durable driver-reservation protocol or distributed workflow. Don't pretend independent databases provide one ACID transaction.

## 11.7 Multi-region architecture

Use regional infrastructure for:
- Location ingestion.
- Live spatial indexes.
- Matching.
- WebSocket connections.

Use regional projections and federated nearby searches for requests near service-region boundaries.

A driver may have:
- An authoritative location owner.
- One or more geographical search projections.
- A connection gateway in another region.

Use a single-writer-per-driver/session policy or an equivalent versioned ownership mechanism.

For assignment, define one authoritative writer per consistency domain.

### Active-active vs active-passive

| Model | Advantages | Trade-offs |
|---|---|---|
| Active-active | Regional capacity and lower latency | Requires clear ownership and write coordination |
| Active-passive | Simpler authoritative ownership | Failover delay and replication lag |
| Hybrid | Active-active location, controlled assignment authority | More nuanced architecture |

During region failure, fence the old assignment leader before promoting a new one.

Use monotonically increasing leadership/ownership epochs.

If a safe new writer cannot be established, temporarily stop authoritative assignment writes rather than allowing split brain.

## 11.8 RPO and RTO

- **RPO:** How much accepted data can be lost?
- **RTO:** How quickly must service recover?

Different data has different objectives:

| Data | Typical priority |
|---|---|
| Idle latest location | Some loss may be acceptable |
| Historical events | Depends on durable acceptance promise |
| WebSocket connection | Recreate through reconnection |
| Active assignment | Must survive acknowledged commit |
| Driver reservation | Must remain valid while its workflow is active |

---

# 12. Business-Specific Matching Patterns

The infrastructure is reusable. Matching and assignment rules vary by product.

## 12.1 Ride-hailing

Objective: Minimize successful pickup time.

Pipeline:

```text
Nearby Discovery
    |
    v
Availability and Vehicle Filter
    |
    v
ETA Ranking
    |
    v
Small-Batch Dispatch
    |
    v
Single-ride Atomic Assignment
```

One active ride per driver.

## 12.2 Food delivery

Additional factors:
- Restaurant preparation time.
- Partner ETA to restaurant.
- Current order workload.
- Delivery deadline.
- Multi-order compatibility.

If food is ready at 10:20 and partner ETA is 8 minutes, dispatching at 10:00 may cause excessive waiting.

A simplified timing objective:

\[
T_{offer}+T_{accept}+T_{arrival}
\approx T_{ready}
\]

This leads to deferred dispatch.

### Multi-order batching

Use route insertion heuristics:

1. Take the existing partner route.
2. Insert new pickup and delivery stops at feasible positions.
3. Respect pickup-before-delivery constraints.
4. Check vehicle capacity and deadlines.
5. Calculate extra route cost.
6. Reject excessive detours.
7. Select the best feasible route.

For delivery partners, assignment invariant becomes:

```text
Total assigned workload <= Capacity
```

Use atomic capacity reservations and route-plan version checks.

Don't blindly reuse the one-active-ride constraint.

## 12.3 Logistics

Hard constraints:
- Vehicle capacity.
- Cargo type.
- Dimensions.
- Driver eligibility.
- Delivery window.
- Route accessibility.

Soft ranking:
- Delivery ETA.
- Cost.
- Fuel.
- Fleet utilization.
- Empty return distance.

Historical tracking and audit requirements are often stronger.

## 12.4 Home services

Hard constraints:
- Required skill.
- Certification.
- Service area.
- Appointment slot.

This requires future time-slot reservation, not just immediate driver availability.

Use atomic interval/slot conflict prevention.

## 12.5 Geofencing

For a warehouse geofence:

```text
Previously OUTSIDE
New observation INSIDE
    |
    v
Emit ENTERED event
```

Use hysteresis to avoid GPS boundary flapping.

Example:
- Enter when within 450 m.
- Exit when beyond 550 m.

Between those distances, retain previous state.

## 12.6 Driver repositioning

Estimate supply and demand by geographical cell:

\[
Pressure = \frac{ExpectedDemand}{AvailableSupply}
\]

Recommend movement toward underserved regions, accounting for repositioning cost, travel time, demand uncertainty, and driver willingness.

This is proactive supply optimization, not ordinary ride matching.

---

# 13. Common Interview Mistakes

| Mistake | Why it's wrong | Better reasoning |
|---|---|---|
| One global Redis GEO key | One hot Redis Cluster key | Partition GEO indexes |
| More Redis nodes automatically solve hotspots | Cluster distributes keys, not members inside one key | Distribute the actual hot keys |
| Search only the customer's geohash | Misses neighbours across boundaries | Search all intersecting cells |
| Treat cell distance as kilometre radius | Grid distance is not physical distance | Conservative region covering + exact filtering |
| Calculate ETA for every driver | Expensive | Cheap shortlist first |
| Select nearest driver unconditionally | Road access and traffic differ | Rank by ETA and business suitability |
| Use Redis lock as assignment authority | Lease expiry/failover limitations | Transactional conditional updates |
| Only protect ride row | Same driver may be claimed by another ride | Protect ride and driver invariants |
| Trust cached availability | Cache may be stale | Atomic authoritative reservation |
| Treat WebSocket send as device delivery | Gateway write doesn't prove device receipt | Application-level ACK |
| Use push notifications as guaranteed delivery | Push can be delayed or suppressed | Durable offer + retry + backend validation |
| Persist all GPS updates in SQL | Huge unnecessary volume | Latest-value index and separate history path |
| Put every event through Kafka | Unnecessary latency and infrastructure | Use based on durability/replay needs |
| Use one `isOnline` boolean | Connection, location, and business state differ | Independent state dimensions |
| Let cleanup define freshness | Cleanup may be delayed | Query-time freshness validation |
| Assume one ACID transaction spans shards | Independent databases don't share ordinary transaction guarantees | Keep a consistency domain or use coordinated workflow |
| Make all regions active-active for assignments | Split-brain can create conflicting assignments | Single authoritative writer + fencing |
| Retry indefinitely | Can create retry storms | Deadlines, backoff, jitter, bounded retries |
| Assume exactly-once network delivery | Network outcomes are ambiguous | Idempotent business effects |

---

# 14. Likely Interview Follow-Up Questions

These are the questions worth preparing to explain, not just memorize answers to.

| Interviewer asks | Reasoning to explain |
|---|---|
| Why Redis instead of PostgreSQL for locations? | High-frequency latest-value workload and GEO support; durable business records remain in SQL |
| Can Redis Cluster distribute one GEO key? | No; keys map to hash slots, so create independently distributed GEO indexes |
| Why not use Kafka for every location update? | Durability/replay benefits versus latency, lag, infrastructure, and history requirements |
| What if Redis loses everything? | Rebuild from fresh driver updates or retained location events; keep restored stale points unmatchable |
| Why H3 when Redis GEO exists? | H3 provides geographical cell routing; Redis GEO performs direct spatial lookup; they're complementary but not always both needed |
| How do you avoid missing drivers near cell boundaries? | Search a conservative cover of intersecting cells and exact-filter candidates |
| How many candidates do you fetch? | Top-K plus overfetch/iterative fetching because eligibility filters eliminate candidates |
| What if all nearby drivers are busy? | Expand radius, search another region where eligible, or mark request unfulfilled within policy limits |
| Why WebSockets? | Server-initiated offers over bidirectional persistent connections; HTTP can still handle location |
| What if WebSocket gateway crashes? | Connection registry lease expires, client reconnects, offers recovered from durable state |
| Is an ACK proof the driver saw the offer? | No; distinguish gateway write, application receipt, and driver response |
| Why not assign using Redis `SET NX`? | Useful coordination, but durable transaction and uniqueness constraints enforce final ownership |
| What if two drivers accept simultaneously? | Both contend on the ride row; one conditional transaction wins |
| What if two rides select the same driver? | Driver reservation/row transition permits only one conflicting claim |
| What if acceptance response is lost? | Retry same idempotency key; retrieve committed result |
| What if expiration and acceptance race? | Conditional transitions on same offer row; exactly one succeeds |
| Why outbox? | Avoid database/broker dual-write loss |
| Why inbox? | Prevent duplicate consumer effects |
| What if Kafka consumer updates Redis then crashes? | Event replays; version-aware Redis update is idempotent |
| Why not one database transaction across everything? | Location writes, notifications, Redis, and independent brokers aren't one ordinary ACID boundary |
| How do you scale hot cells? | Subdivide or shard, migrate with controlled routing, deduplicate projections |
| How do you scale WebSockets? | Gateway fleets, connection ownership registry, connection-aware admission and graceful draining |
| How do you handle region failure? | Reconnect/rebuild live projections; fence assignment authority before promotion |
| How does delivery batching differ from ride matching? | Capacity, route compatibility, pickup/delivery windows, and multi-order constraints |
| What if driver can carry multiple orders? | Replace one-active-assignment rule with atomic capacity reservation and route-plan validation |

---

# 15. Final Quick-Recall Checklist

Before finishing a system design interview, mentally walk through these.

### Requirements
- [ ] What updates are continuous?
- [ ] How fresh must locations be?
- [ ] What data must be durable?
- [ ] What is the scale?
- [ ] What are latency and availability targets?

### Location ingestion
- [ ] HTTP vs WebSockets justified?
- [ ] Stateless ingestion?
- [ ] Authentication and validation?
- [ ] Session epochs and sequence numbers?
- [ ] Out-of-order and duplicate protection?
- [ ] Client sampling and backpressure?

### Storage and spatial indexing
- [ ] Latest location separated from history?
- [ ] Redis GEO limitations understood?
- [ ] GEO key partitioning?
- [ ] Geographic locality considered?
- [ ] Boundary-safe cell search?
- [ ] Stale-location cleanup?
- [ ] Redis recovery strategy?

### Discovery and matching
- [ ] Discovery separated from eligibility?
- [ ] Top-K/overfetch?
- [ ] Exact geographical distance?
- [ ] Location freshness?
- [ ] Radius expansion?
- [ ] ETA rather than distance alone?
- [ ] Partial partition failures handled?

### Dispatch
- [ ] Sequential, parallel, or batch strategy?
- [ ] Temporary driver reservation?
- [ ] Durable offer state?
- [ ] WebSocket connection registry?
- [ ] Device acknowledgement?
- [ ] Offer expiry and cancellation?
- [ ] Push fallback?

### Assignment
- [ ] One active assignment per ride?
- [ ] One active assignment per driver?
- [ ] Conditional database updates?
- [ ] Row locking and transaction boundaries?
- [ ] Unique constraints?
- [ ] Acceptance idempotency?
- [ ] Expiration vs acceptance race?
- [ ] Reassignment history?

### Reliability
- [ ] Bounded retries?
- [ ] Exponential backoff and jitter?
- [ ] Outbox and inbox?
- [ ] Duplicate-safe consumers?
- [ ] DLQ and replay?
- [ ] Redis/Kafka/database outages?
- [ ] Reconciliation?
- [ ] Backpressure and circuit breakers?

### Scaling
- [ ] Ingestion scales independently?
- [ ] Redis hotspots addressed?
- [ ] Matching and routing independently scaled?
- [ ] WebSocket connection capacity?
- [ ] Kafka partitioning and consumer lag?
- [ ] Multi-region ownership?
- [ ] Fencing and split-brain protection?

---

# 16. The One-Minute Architecture Explanation

When you need to explain the entire architecture quickly in an interview:

> I would separate location tracking, nearby discovery, dispatch, and assignment because they have different throughput and correctness requirements.
>
> Drivers continuously send locations through horizontally scaled ingestion services. I'd maintain the latest location in partitioned Redis GEO indexes, using session epochs and sequence numbers to reject duplicates and out-of-order updates. Kafka is optional for historical durability and replay, not mandatory for every live update.
>
> For nearby discovery, I'd use spatial indexing with Redis GEO initially, introducing H3/geographical partition routing when search fan-out becomes expensive. The search returns candidates, which we filter by location freshness, availability, service compatibility, and exact geographic distance. We then rank a shortlist using road ETA.
>
> A dispatch orchestrator selects a small batch of candidates, obtains temporary authoritative reservations, creates durable offers, and delivers them through WebSocket gateways. A connection registry maps drivers to their owning gateways. Push notifications provide a best-effort background fallback.
>
> When drivers accept, a transactional assignment service validates the offer and expiry, atomically claims both the ride and the driver, and records the assignment. Conditional updates, database constraints, transactions, and idempotency prevent double booking. A transactional outbox reliably publishes assignment and cancellation events.
>
> Finally, I would scale ingestion, Redis, matching, WebSocket gateways, routing, and assignment independently. For multiple regions, location projections can be eventually consistent, but assignment decisions need a clearly defined authoritative owner.

---

# Final Mental Model

The entire system can be reduced to these questions:

```text
LOCATION
    Where is the provider?

DISCOVERY
    Who is nearby?

ELIGIBILITY
    Who can do the job?

MATCHING
    Who is the best candidate?

DISPATCH
    Whom should we contact?

RESERVATION
    Who temporarily owns the opportunity?

ASSIGNMENT
    Who officially owns the job?

DELIVERY
    How do we communicate the result reliably?

RECOVERY
    How do we restore the system when something fails?
```

**Don't memorize one architecture. Understand the workload, identify the required consistency guarantee, select the appropriate mechanism, and explain the trade-off.**

That reasoning is what makes the design reusable across ride-hailing, food delivery, logistics, fleet management, and other real-time location-based platforms.
