# Chapter 12 — Complete End-to-End Architecture and System Design Walkthrough

We've reached the final chapter.

So far, we've learned the individual concepts and patterns:

- High-throughput location ingestion.
- Redis GEO and geographical partitioning.
- Kafka and durable event processing.
- Geohash, H3, S2, and Quadtrees.
- Nearby-driver discovery.
- Radius expansion and candidate ranking.
- WebSockets and real-time dispatch.
- Driver reservations and atomic assignment.
- Heartbeats and failure detection.
- Idempotency, retries, outbox, and recovery.
- Horizontal scaling and multi-region architecture.
- Different matching patterns for ride-hailing, food delivery, logistics, and home services.

Now we'll approach this as if an interviewer asks:

> Design a reliable, real-time location tracking and driver-matching system like Uber.

We'll start with an empty system and build the architecture in the order we should think about it in a real interview.

We'll make decisions, question them, identify bottlenecks, and evolve the design.

The important thing is to understand why each component enters the architecture, rather than starting with a diagram containing every technology we've discussed.

# 12.1 Step 1 — Clarify the requirements

Before drawing anything, establish what the system must accomplish.

We shouldn't immediately say:

```
Redis + Kafka + WebSockets + PostgreSQL
```

Those are implementation choices.

First, establish the requirements that drive those choices.

I like to group clarifying questions into six categories.

## A. Product requirements

What are we actually building?

For our reference design, assume:

1. Drivers can go online and offline.
2. Drivers continuously report their location.
3. Customers can request rides.
4. We find nearby eligible drivers.
5. We send ride offers in real time.
6. Drivers can accept or reject offers.
7. Only one driver can win a particular ride.
8. A driver cannot be assigned conflicting rides.
9. Customers can track their assigned drivers.
10. We support cancellation, expiration, and reassignment.

## B. Location requirements

We need to know:

- How frequently do drivers report locations?
- How fresh must a location be to participate in matching?
- Do we need historical route tracking?
- Should every location update be retained?
- How accurate should geographical search be?
- What is the maximum search radius?

For our example:

```
Location update interval: 3 seconds
Maximum acceptable location age: 15 seconds
Initial search radius: 2 km
Maximum search radius: 10 km
```

These are illustrative business settings, not universal values.

## C. Matching requirements

Ask:

- Do we select the nearest driver or fastest-to-arrive driver?
- Do we contact one driver at a time or multiple drivers?
- How many drivers can receive an offer simultaneously?
- Should the radius expand automatically?
- How should we handle unavailable drivers?
- Is there a fairness requirement?

Our reference design will use ETA-based ranking with small-batch dispatch and progressive radius expansion.

## D. Reliability requirements

Ask:

- Can we lose location updates?
- Can we lose ride assignments?
- What happens if the database crashes?
- Should offer delivery be retried?
- Do we need durable location history?
- Can a customer be temporarily unable to request a ride during an assignment-system outage?

Our assumptions:

| Data                        | Reliability requirement             |
| --------------------------- | ----------------------------------- |
| Latest idle-driver location | Some recent data loss is acceptable |
| Location freshness          | Must be checked before matching     |
| Historical trip locations   | Configurable durability             |
| Driver reservations         | Durable while valid                 |
| Ride assignment             | Must be durable and atomic          |
| Offer delivery              | Retryable, duplicates tolerated     |
| WebSocket connection        | Reconnectable, not durable          |

## E. Scale requirements

Assume:

```
Registered drivers:                 10 million
Simultaneously online drivers:       1 million
Location interval:                   3 seconds
Concurrent ride requests:            10,000
Connected driver WebSockets:          800,000
Number of cities:                     100+
Deployment regions:                   Multiple
```

These figures are hypothetical design inputs.

## F. Performance requirements

For illustration:

| Operation                                | Initial target              |
| ---------------------------------------- | --------------------------- |
| Location ingestion                       | p99 under 150 ms            |
| Nearby discovery                         | p99 under 200 ms            |
| Offer delivery through active connection | p99 under 300 ms            |
| Assignment confirmation                  | p99 under 200 ms            |
| Location freshness                       | Typically within 15 seconds |

These are example engineering objectives, not measured guarantees.

The important point is that we need measurable targets.

A system designed for 10-second matching can look very different from one designed for sub-second matching.

# 12.2 Step 2 — Estimate the scale

Now let's perform basic capacity estimation.

This helps us identify the likely bottlenecks.

### Location write throughput

We have:

```
Online drivers = 1,000,000

Location interval = 3 seconds
```

Therefore:

\\[ WritesPerSecond = \frac{1,000,000}{3} \\]

\\[ \boxed{\approx 333,333 \text{ updates/sec}} \\]

That's our approximate average ingestion rate.

We should also account for bursts caused by:

- Application reconnections.
- Mobile network recovery.
- Login surges.
- Major city events.
- Regional infrastructure recovery.

### Daily location volume

\\[ 333,333 \times 86,400 \\]

\\[ \boxed{\approx 28.8 \text{ billion updates/day}} \\]

Now let's assume each serialized location payload is 200 bytes.

\\[ 28.8B \times 200 \\]

Approximately 5.76 TB of raw payload per day.

That's before protocol overhead, replication, indexes, and storage overhead.

This immediately tells us something important:

We shouldn't permanently persist every location update into a conventional relational table without a very specific reason.

The latest location belongs in a fast operational store.

Historical events need a separate retention strategy.

### WebSocket connections

```
Connected drivers = 800,000
```

These connections need dedicated gateway capacity.

We cannot treat them as ordinary short-lived HTTP requests.

### Nearby searches

Assume:

```
Ride requests = 10,000/sec
```

At this scale, spatial query fan-out matters.

If every request queries 16 Redis partitions:

\\[ 10,000 \times 16 = 160,000 \\]

That's 160,000 spatial queries per second.

If we can route geographically and query only four relevant partitions on average:

\\[ 10,000 \times 4 = 40,000 \\]

We've reduced the spatial-search workload substantially.

This is an illustrative comparison; the actual search cost also depends on candidate density, result size, and the geographic distribution of requests.

### What does the estimation tell us?

We already know that different parts of the system need independent scaling:

```
Location ingestion:
    Extremely write-heavy.

Spatial discovery:
    Read-heavy and geographically selective.

WebSocket:
    Connection-heavy.

Assignment:
    Transaction-heavy.

Historical storage:
    Volume and retention-heavy.
```

This is the foundation of our architecture.

# 12.3 Step 3 — Identify the core entities and invariants

Before designing services, let's define what must remain true.

Our important entities are:

Driver

```
driverId
accountStatus
vehicleType
serviceEligibility
```

Driver location

```
driverId
latitude
longitude
accuracy
speed
heading
sessionEpoch
sequenceNumber
lastReceivedAt
```

Driver operational state

```
driverId
onlineIntent
availabilityStatus
reservedOfferId
activeRideId
```

Ride

```
rideId
customerId
pickupLocation
destination
status
assignedDriverId
createdAt
```

Offer

```
offerId
rideId
driverId
status
createdAt
expiresAt
```

Assignment

```
assignmentId
rideId
driverId
assignedAt
status
```

Now establish the invariants.

These are more important than the individual table definitions.

### Invariant 1

A ride can have at most one active driver assignment.

### Invariant 2

A driver can have at most one active ride assignment in our ride-hailing model.

### Invariant 3

A stale location must never qualify a driver for matching.

### Invariant 4

An older location update must never overwrite a newer accepted update.

### Invariant 5

An expired or cancelled offer cannot be accepted.

### Invariant 6

A driver reservation must belong to a specific offer.

### Invariant 7

A duplicate acceptance request must not create a second assignment.

### Invariant 8

Redis availability is not authoritative for assignment.

These invariants determine our storage and concurrency decisions.

# 12.4 Step 4 — Design the high-level architecture

Now we can introduce components.

Here's the reference architecture we'll evolve.

\#chatgpt-mermaid-\_r_5ge\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_5ge\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_5ge\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_5ge\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_5ge\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_5ge\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_5ge\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_5ge\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_5ge\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_5ge\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_5ge\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_5ge\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_5ge\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_5ge\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_5ge\_ p{margin:0;}#chatgpt-mermaid-\_r_5ge\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_5ge\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_5ge\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_5ge\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_5ge\_ .label text,#chatgpt-mermaid-\_r_5ge\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_5ge\_ .node rect,#chatgpt-mermaid-\_r_5ge\_ .node circle,#chatgpt-mermaid-\_r_5ge\_ .node ellipse,#chatgpt-mermaid-\_r_5ge\_ .node polygon,#chatgpt-mermaid-\_r_5ge\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_5ge\_ .rough-node .label text,#chatgpt-mermaid-\_r_5ge\_ .node .label text,#chatgpt-mermaid-\_r_5ge\_ .image-shape .label,#chatgpt-mermaid-\_r_5ge\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_5ge\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_5ge\_ .rough-node .label,#chatgpt-mermaid-\_r_5ge\_ .node .label,#chatgpt-mermaid-\_r_5ge\_ .image-shape .label,#chatgpt-mermaid-\_r_5ge\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_5ge\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_5ge\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_5ge\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_5ge\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_5ge\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_5ge\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_5ge\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_5ge\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_5ge\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_5ge\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_5ge\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_5ge\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_5ge\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_5ge\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_5ge\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_5ge\_ .icon-shape,#chatgpt-mermaid-\_r_5ge\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_5ge\_ .icon-shape p,#chatgpt-mermaid-\_r_5ge\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_5ge\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_5ge\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_5ge\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_5ge\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_5ge\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_5ge\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_5ge\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_5ge\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_5ge\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_5ge\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_5ge\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_5ge\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_5ge\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_5ge\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_5ge\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_5ge\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_5ge\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_5ge\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_5ge\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_5ge\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_5ge\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_5ge\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_5ge\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_5ge\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_5ge\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_5ge\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_5ge\_ .node rect,#chatgpt-mermaid-\_r_5ge\_ .node circle,#chatgpt-mermaid-\_r_5ge\_ .node ellipse,#chatgpt-mermaid-\_r_5ge\_ .node polygon,#chatgpt-mermaid-\_r_5ge\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_5ge\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_5ge\_ .mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_5ge\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_5ge\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_5ge\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}Authoritative Business InfrastructureMatching and DispatchLocation InfrastructureRegional EdgeMobile ApplicationsAssignment ServiceTransactional DatabaseTransactional OutboxDelivery Queue / InternalRouterNearby DiscoveryMatching and RankingDispatch OrchestratorETA / Routing ServiceLocation IngestionDriver Location OwnerSpatial Index RouterRedis GEO PartitionsOptional Kafka LocationStreamHistorical Location StoreLoad BalancerAPI GatewayWebSocket GatewaysConnection RegistryDriver AppCustomer App

This diagram represents logical components.

We don't need to deploy every box as a separate microservice immediately.

For example, a small implementation might combine:

- Location ingestion and driver-session management.
- Matching and dispatch orchestration.
- Offer processing and assignment APIs.

We split them when their scaling, ownership, or deployment requirements justify it.

Now let's walk through the architecture in the same order as a real request.

# 12.5 Step 5 — Trace one location update end to end

D101 starts working.

The application authenticates and establishes a valid session.

```
Driver D101

Session epoch = 11
Sequence number = 100
Location = (17.3850, 78.4867)
```

The application sends a location update.

### Flow

```
Driver App
    |
    v
Regional Load Balancer
    |
    v
Location Ingestion
    |
    v
Authentication and Validation
    |
    v
Session / Sequence Check
    |
    v
Driver Location Owner
    |
    v
Redis GEO + Metadata
```

### What happens inside ingestion?

1. Authenticate the driver.
2. Validate the coordinate.
3. Validate session epoch.
4. Compare sequence number.
5. Reject duplicate or out-of-order updates.
6. Update latest location.
7. Update the GEO index.
8. Update last-received timestamp.
9. Acknowledge the accepted update.

The critical atomic operation is:

```
IF incoming version > stored version:

    Update latest location.
    Update GEO membership.
    Update freshness metadata.

ELSE:

    Ignore.
```

The actual implementation needs to respect the storage partition's atomicity boundary.

### Where does Kafka fit?

Not necessarily in the live path.

If the product only needs the latest live location:

```
Ingestion -> Redis
```

If historical completeness is required:

```
Ingestion -> Kafka -> Historical Consumers
```

Or use a suitable durable stream-first architecture when preserving every accepted event is mandatory.

We must explicitly define the acceptance boundary.

### What about Redis failure?

```
Redis fails
    |
    v
Ingestion cannot confirm live indexing
    |
    v
Return retryable failure
    |
    v
Driver retries latest location
```

Redis can be rebuilt using fresh driver updates, or retained event history where the business requires it.

We don't need to permanently preserve every idle GPS point merely to rebuild an ephemeral live-location index.

# 12.6 Step 6 — Trace one nearby-driver search end to end

Now a customer requests a ride.

```
Ride R501

Pickup:
    (17.3850, 78.4867)

Destination:
    (17.4100, 78.5000)

Vehicle:
    Bike
```

The customer request reaches the API gateway.

### Step A — Persist the ride request

We create:

```
R501:
    status = SEARCHING
    assignedDriverId = NULL
```

Why persist first?

Because the ride request is a durable business entity.

Even if the matching service crashes, we must know that the customer requested a ride.

We can also create a durable dispatch-start event using the outbox pattern.

### Step B — Determine the search region

Our matching engine receives:

```
Pickup = (17.3850, 78.4867)

Initial radius = 2 km
```

The spatial router determines which geographical indexes intersect the search area.

For example:

```
Relevant geographical cells:

H3-A
H3-B
H3-C
H3-D
```

### Step C — Query Redis

Each relevant partition is queried in parallel.

Conceptually:

```
GEOSEARCH geo:{cell-A}
    FROMLONLAT 78.4867 17.3850
    BYRADIUS 2 km
    WITHDIST ASC
```

Repeat for the relevant indexes.

### Step D — Merge candidate IDs

Suppose:

```
D101 -> 0.4 km
D102 -> 0.7 km
D103 -> 1.2 km
D104 -> 1.5 km
D105 -> 1.8 km
```

We merge and deduplicate.

### Step E — Fetch operational state in batches

We retrieve the latest driver state.

```
D101 -> AVAILABLE, Fresh
D102 -> BUSY, Fresh
D103 -> AVAILABLE, Fresh
D104 -> AVAILABLE, Stale
D105 -> RESERVED, Fresh
```

Only D101 and D103 survive.

### Step F — Validate exact distance

Recalculate using the latest accepted coordinates.

Remember:

A spatial index gives us a candidate set.

We still validate the exact geographic radius using the latest location.

### Step G — Calculate ETA

We shortlist the eligible drivers.

```
D101 -> ETA 3 min
D103 -> ETA 4 min
```

For a larger result set, use batched route-matrix calculations rather than calling a routing provider for every candidate independently.

### Step H — Rank

We might produce:

```
1. D101
2. D103
```

Our matching engine returns the ranked candidates to the dispatch orchestrator.

### What if we don't find enough drivers?

Progressively expand:

```
2 km -> 3 km -> 5 km -> 8 km
```

We keep the previously discovered candidate IDs to avoid unnecessary duplicate processing.

We also impose a matching deadline and maximum candidate count.

### What if one Redis partition fails?

We distinguish:

```
Search returned no drivers.
```

from:

```
Search was incomplete because one partition failed.
```

If the search is incomplete, we apply a defined degraded-search policy.

We don't falsely claim that there are no nearby drivers.

The result now looks something like:

```
Discovery Result:

Candidates:
    D101
    D103

Coverage:
    Complete

Radius:
    2 km

Search latency:
    85 ms
```

We now have two good candidates.

# 12.7 Step 7 — Trace dispatch and temporary driver reservation

The dispatch orchestrator receives:

```
R501:
    D101
    D103
```

Our strategy is small-batch dispatch.

We want to offer the ride to both drivers simultaneously.

But we must first reserve them.

### Step A — Select a batch

```
Batch 1:

D101
D103
```

### Step B — Reserve each driver authoritatively

We use conditional database updates.

For D101:

```
UPDATE drivers
SET
    availability_status = 'RESERVED_FOR_OFFER',
    reserved_offer_id = 'O101',
    reservation_expires_at = :expiry
WHERE
    driver_id = 'D101'
    AND availability_status = 'AVAILABLE'
    AND active_ride_id IS NULL;
```

If one row is updated, the reservation succeeds.

If zero rows are updated, another operation already changed the driver's state.

Repeat for D103.

### Step C — Create offer records

Within the appropriate transaction, we create:

```
O101:
    rideId = R501
    driverId = D101
    status = PENDING
    expiresAt = 10:00:15

O102:
    rideId = R501
    driverId = D103
    status = PENDING
    expiresAt = 10:00:15
```

We also write the corresponding delivery events into the outbox.

Why in the same transaction?

Because we must not reserve a driver without creating the corresponding durable offer, or create an offer without successfully reserving the driver.

### Step D — Commit

Our database now has:

```
D101 -> RESERVED_FOR_OFFER O101
D103 -> RESERVED_FOR_OFFER O102

O101 -> PENDING
O102 -> PENDING
```

The next step is delivery.

# 12.8 Step 8 — Trace a ride offer through WebSockets

We have 800,000 connected drivers distributed across WebSocket gateways.

The connection registry contains:

```
D101 -> Gateway-A
D103 -> Gateway-C
```

Our outbox publisher publishes:

```
SEND_OFFER O101 TO D101
SEND_OFFER O102 TO D103
```

The delivery router looks up the gateway ownership.

```
                    Offer Router
                         |
             +-----------+-----------+
             |                       |
             v                       v
        Gateway A               Gateway C
             |                       |
             v                       v
         Driver D101             Driver D103
```

The gateway writes the message to the local WebSocket connection.

Example:

```
{
  "eventId": "EVT-101",
  "eventType": "RIDE_OFFER",
  "offerId": "O101",
  "rideId": "R501",
  "expiresAt": "2026-11-12T10:00:15Z"
}
```

The mobile application receives it and responds:

```
{
  "eventType": "OFFER_RECEIVED",
  "offerId": "O101"
}
```

The acknowledgement confirms application receipt.

It does not mean the driver accepts the ride.

### What if the gateway is unavailable?

The offer remains durable.

We retry delivery while the offer remains valid.

If the driver reconnects to another gateway, the router discovers the new connection ownership.

If the offer expires before delivery, the delivery operation stops.

### What if the driver is disconnected?

Depending on our policy:

- Wait through a short connection grace period.
- Attempt push notification.
- Cancel the offer when its business deadline or cancellation policy requires it.

The authoritative offer state determines whether an acceptance is still valid.

# 12.9 Step 9 — Trace simultaneous driver acceptance

Now comes the most important operation.

Suppose both drivers accept:

```
D101 -> Accept O101
D103 -> Accept O102
```

The requests reach different assignment-service instances.

```
Server A:
    D101 accepts R501

Server B:
    D103 accepts R501
```

Both begin assignment transactions.

We acquire the relevant database row locks in a consistent order.

Both transactions must validate:

```
Offer exists.
Offer belongs to this driver.
Offer is pending.
Offer hasn't expired.
Ride is still searching.
Driver still holds the reservation.
Driver has no conflicting assignment.
```

Then both attempt to claim R501.

### Transaction A wins

```
R501:
    SEARCHING -> ASSIGNED
    assignedDriver = D101
```

It also transitions D101:

```
RESERVED_FOR_OFFER -> BUSY
activeRide = R501
```

And marks O101 accepted.

### Transaction B loses

Server B now finds:

```
R501:
    status = ASSIGNED
    assignedDriver = D101
```

Its assignment operation fails.

It cannot assign D103.

The database transaction rolls back any partial changes.

Final state:

```
R501 -> D101

D101 -> BUSY
D103 -> AVAILABLE / reservation released

O101 -> ACCEPTED
O102 -> CANCELLED
```

The transaction also writes events into the outbox.

### Why is this reliable?

Because the database is the authority.

The following are not the source of truth:

- Which WebSocket delivered the offer first.
- Which application server received the request first.
- Which Redis lock was acquired first.
- Which driver is geographically closest.
- Which driver tapped the button first on their device.

The successful authoritative database transition determines the winner.

We have achieved one successful assignment.

# 12.10 Step 10 — Notify everybody about the result

After the assignment transaction commits, outbox publishers process the events.

We need to communicate:

```
D101:
    Your ride is confirmed.

D103:
    This ride has been assigned to another driver.

Customer:
    Your driver has been assigned.
```

These notifications might travel through:

- WebSocket.
- Push notification.
- HTTP response.
- Customer-facing WebSocket or SSE.
- Other internal events.

We do not send these notifications inside the database transaction.

Why?

Because external network failures shouldn't hold database locks open.

### What if notification delivery fails?

No problem for business correctness.

The database already records:

```
R501 -> D101
```

A publisher can retry.

If the event is delivered twice, consumers deduplicate.

If the customer reconnects, they fetch the current ride state.

If D103 reconnects, the application fetches the current offer state and sees that O102 has been cancelled.

The business state survives communication failures.

# 12.11 Step 11 — Trace the complete ride lifecycle

Once a driver is assigned, location tracking continues.

The driver periodically sends location updates.

```
D101:
    10:00:00 -> Location A
    10:00:03 -> Location B
    10:00:06 -> Location C
```

Redis updates the latest position.

The customer-facing tracking service can fetch the driver's latest location.

We can also push location events to the customer's application through WebSockets or SSE.

The ride then transitions:

```
ASSIGNED
    |
    v
DRIVER_ARRIVING
    |
    v
DRIVER_ARRIVED
    |
    v
IN_PROGRESS
    |
    v
COMPLETED
```

These transitions must be authorized and persisted through the business workflow.

When the ride completes:

```
Ride R501:
    status = COMPLETED

Driver D101:
    availabilityStatus = AVAILABLE
    activeRideId = NULL
```

This transition should occur in one durable transaction.

We must also update the operational Redis availability index so that D101 can be discovered again.

If the Redis update fails:

- The database transition remains committed.
- The event/outbox mechanism retries the projection update.
- Reconciliation repairs the operational index if necessary.

This completes the normal end-to-end flow.

# 12.12 Step 12 — Now let's break the architecture

A good system design isn't complete until we can explain what happens when components fail.

Let's walk through our major failure scenarios.

| Failure                                | Expected behavior                                             |
| -------------------------------------- | ------------------------------------------------------------- |
| Driver loses network                   | Location becomes stale after threshold; reconnect with jitter |
| Old location update arrives            | Reject by session epoch and sequence                          |
| Redis loses data                       | Repopulate from fresh locations or retained events            |
| Redis partition fails                  | Partial search policy or temporary matching degradation       |
| Kafka is unavailable                   | Follow the chosen durability contract; apply backpressure     |
| WebSocket gateway crashes              | Drivers reconnect to another gateway                          |
| Offer message is duplicated            | Deduplicate by offer/event ID                                 |
| Offer message is lost                  | Retry while offer is valid                                    |
| Driver accepts twice                   | Idempotency returns the previous result                       |
| Two drivers accept simultaneously      | Database transaction selects one winner                       |
| Two rides select the same driver       | Driver reservation allows only one conflicting claim          |
| Timeout races with acceptance          | Conditional state transition determines winner                |
| Outbox publisher crashes               | Retry unpublished records                                     |
| Assignment server crashes after commit | Retry returns the committed result                            |
| Assignment database is unavailable     | Don't declare success; retry the same operation               |
| Region fails                           | New ownership generation and safe authority failover          |

The most important failure-handling principle:

Different failure types should have different recovery policies.

We don't need to preserve a WebSocket connection.

We do need to preserve an assignment.

We don't necessarily need every intermediate idle GPS coordinate.

We might need every required active-delivery location event.

The durability and recovery strategy must reflect that.

# 12.13 Step 13 — Make every major architectural decision explicitly

This is particularly useful during system design interviews.

The interviewer might challenge individual choices.

Instead of saying:

> We use Redis because it's fast.

You should explain the workload, alternatives, and reason for the decision.

Let's build the final decision matrix.

## A. Communication choices

| Option             | When I'd choose it                             | Why                                                        |
| ------------------ | ---------------------------------------------- | ---------------------------------------------------------- |
| HTTP               | Periodic location updates                      | Simple, stateless, easy to scale                           |
| WebSockets         | Bidirectional real-time communication          | Push offers and receive events over persistent connections |
| SSE                | Primarily server-to-client streaming           | Simpler one-way event delivery                             |
| Push notifications | Background notification                        | Useful when the app isn't actively connected               |
| gRPC streaming     | Efficient streaming in controlled environments | Strong contracts and compact messages                      |
| MQTT               | Broker-based lightweight device communication  | Useful in IoT-style environments                           |

My default: HTTP location ingestion + WebSockets for active real-time communication + push notification fallback.

There's no requirement that all traffic use the same protocol.

## B. Storage choices

| Requirement                     | Choice                            | Reason                           |
| ------------------------------- | --------------------------------- | -------------------------------- |
| Latest location                 | Redis                             | Fast in-memory operational view  |
| Nearby search                   | Redis GEO                         | Convenient radius search         |
| Very large spatial partitioning | H3/S2/geohash + distributed index | Geographic locality              |
| Durable location history        | Kafka + historical storage        | Event preservation and replay    |
| Driver profile                  | Relational database               | Durable business data            |
| Driver reservation              | Transactional database            | Atomic state transitions         |
| Ride assignment                 | Transactional database            | Correctness and durability       |
| Connection registry             | Redis or equivalent               | Fast, temporary ownership lookup |

My default: Redis for live state, a transactional database for business state, and Kafka only when durable event streaming is justified.

## C. Partitioning choices

| Strategy                     | Advantage                  | Disadvantage                               |
| ---------------------------- | -------------------------- | ------------------------------------------ |
| Single GEO index             | Very simple                | Single-key hotspot                         |
| Driver-ID partitioning       | Distributes writes         | Search fan-out                             |
| Geographic cell partitioning | Efficient nearby discovery | Driver movement requires index maintenance |
| Hybrid partitioning          | Distributes both workloads | More complexity                            |

My evolution: Start with partitioned Redis GEO, then introduce cell-based search routing when geographically selective queries become a bottleneck.

## D. Dispatch choices

| Strategy             | Best suited for                       |
| -------------------- | ------------------------------------- |
| Sequential           | Low contention and notification cost  |
| Parallel             | Very low response latency             |
| Small batches        | Balance speed and contention          |
| Staggered            | Reduce unnecessary offers             |
| Deferred             | Restaurant preparation and scheduling |
| Multi-order batching | Delivery and logistics                |

My default for ride-hailing: Small-batch or staggered dispatch.

## E. Concurrency choices

| Mechanism                   | Usage                                 |
| --------------------------- | ------------------------------------- |
| In-memory lock              | Local optimization only               |
| Redis `SET NX`              | Fast temporary coordination           |
| Database conditional update | Atomic business state transitions     |
| Database row lock           | Multi-record transaction coordination |
| Unique constraint           | Enforce critical invariants           |
| Idempotency key             | Safe retries                          |
| Outbox                      | Reliable event publication            |

My default for assignment: Database-backed reservations and transactions. Use Redis only as an optional coordination optimization.

# 12.14 Step 14 — How do we adapt the architecture when requirements change?

Let's consider some changes an interviewer might introduce halfway through the discussion.

This is where you should demonstrate architectural reasoning instead of defending your first design blindly.

### Change A: "We must retain every location update during an active trip."

Previously:

```
Driver -> Redis
```

Now:

```
Driver
   |
   v
Location Ingestion
   |
   v
Kafka
   |
   +----> Redis live projection
   |
   +----> Historical location store
```

Why?

We now have a durable historical event requirement.

We need to acknowledge after the durable ingestion boundary.

We also need consumer lag monitoring and idempotent projection updates.

### Change B: "Redis search has become too expensive."

Previously:

```
Driver-ID partitioning
     |
     v
Parallel query every partition
```

Now:

```
Customer pickup
       |
       v
H3 / Geohash region covering
       |
       v
Identify relevant cell partitions
       |
       v
Query only relevant partitions
```

Why?

Our read workload needs geographical locality.

### Change C: "We have too many drivers in airport and stadium locations."

Introduce:

- Smaller subcells.
- Cell-level sharding.
- Dynamic capacity allocation.
- Hotspot-aware search routing.

Why?

The distribution of drivers is uneven.

Uniform partitioning assumptions are no longer sufficient.

### Change D: "Drivers can carry multiple delivery orders."

Modify:

```
One driver -> One active assignment
```

to:

```
One driver -> Bounded compatible workload
```

Introduce:

- Capacity reservation.
- Route-plan versions.
- Insertion heuristics.
- Delivery deadlines.
- Multi-order assignment state.

Why?

The business invariant has changed.

### Change E: "A ride can be requested near a regional boundary."

Introduce:

- Federated regional search.
- Cross-region candidate merging.
- Service-boundary validation.
- An authoritative assignment owner.

Why?

Operational service coverage does not always align with infrastructure boundaries.

### Change F: "The assignment database must survive an entire regional outage."

Define:

- Replication mode.
- RPO and RTO.
- Primary/standby authority.
- Leader-election policy.
- Fencing epochs.
- Safe promotion.

Don't simply point both regions at independent databases and allow writes in both.

Why?

We need to preserve the assignment uniqueness invariant during failover.

### Change G: "The customer wants to track the driver in real time."

Introduce a customer-facing streaming path:

```
Driver Location Update
       |
       v
Redis / Location Event
       |
       v
Tracking Service
       |
       v
Customer WebSocket / SSE
       |
       v
Customer App
```

We should not expose the driver's raw location ingestion endpoint directly to customers.

The tracking service applies:

- Authorization.
- Ride ownership validation.
- Location precision policy.
- Update throttling.
- Privacy rules.
- Active-ride checks.

It can publish selected location changes to the customer.

# 12.15 Step 15 — What APIs might we expose?

An interview architecture becomes more concrete when we define a few important APIs.

These are illustrative endpoints, not a mandatory API design.

## Driver APIs

Go online

```
POST /drivers/me/online
```

Go offline

```
POST /drivers/me/offline
```

Send location

```
POST /drivers/me/location
```

Payload:

```
{
  "sessionEpoch": 11,
  "sequenceNumber": 101,
  "latitude": 17.3850,
  "longitude": 78.4867,
  "accuracy": 8.5,
  "speed": 12.4,
  "heading": 180,
  "deviceTimestamp": "2026-11-12T10:00:00Z"
}
```

The driver identity comes from authentication.

We should not trust an arbitrary `driverId` supplied in the request body.

Heartbeat

```
POST /drivers/me/heartbeat
```

Get current offers

```
GET /drivers/me/offers/active
```

Accept an offer

```
POST /offers/O101/accept
Idempotency-Key: ACCEPT-991
```

Reject an offer

```
POST /offers/O101/reject
Idempotency-Key: REJECT-991
```

Get active assignment

```
GET /drivers/me/active-assignment
```

## Customer APIs

Request a ride

```
POST /rides
```

Example:

```
{
  "pickup": {
    "latitude": 17.3850,
    "longitude": 78.4867
  },
  "destination": {
    "latitude": 17.4100,
    "longitude": 78.5000
  },
  "vehicleType": "BIKE",
  "idempotencyKey": "RIDE-REQUEST-101"
}
```

Get ride state

```
GET /rides/R501
```

Cancel ride

```
POST /rides/R501/cancel
Idempotency-Key: CANCEL-501
```

Track driver

```
GET /rides/R501/driver-location
```

Or receive real-time changes via WebSocket/SSE.

### Why are idempotency keys important on customer ride creation?

Suppose a customer taps Request Ride.

The request reaches the backend and the ride is created.

But the response is lost.

The application retries.

Without idempotency, we might create two separate rides.

The idempotency key protects the customer request itself, just as the acceptance idempotency key protects driver assignment.

# 12.16 Step 16 — Build the final state transition model

Let's consolidate the central business states.

### Ride state

```
CREATED
   |
   v
SEARCHING
   |
   +-----------> CANCELLED
   |
   +-----------> UNFULFILLED
   |
   v
ASSIGNED
   |
   v
DRIVER_ARRIVING
   |
   v
DRIVER_ARRIVED
   |
   v
IN_PROGRESS
   |
   v
COMPLETED
```

A reassignment can happen through a controlled transition when the current assignment is cancelled or otherwise invalidated.

### Driver state

```
OFFLINE
   |
   v
ACTIVATING
   |
   v
AVAILABLE
   |
   v
RESERVED_FOR_OFFER
   |
   +---- REJECTED / EXPIRED / CANCELLED
   |              |
   |              v
   |          AVAILABLE
   |
   v
BUSY
   |
   v
AVAILABLE
```

### Offer state

```
CREATED
   |
   v
PENDING
   |
   +---- ACCEPTED
   |
   +---- REJECTED
   |
   +---- EXPIRED
   |
   +---- CANCELLED
```

We must enforce legal transitions in the assignment and dispatch workflows.

For example, a delayed timeout event cannot change an ACCEPTED offer to EXPIRED.

A duplicate acceptance cannot create another assignment.

A cancelled offer cannot be revived by a delayed WebSocket message.

### One important distinction

Driver availability and location freshness remain independent.

For example:

```
Driver:
    Availability = AVAILABLE
    Location = STALE
```

This driver is not matchable.

After a fresh location arrives:

```
Driver:
    Availability = AVAILABLE
    Location = FRESH
```

Now they may become matchable.

Never let an update to one state machine accidentally reset another.

# 12.17 Step 17 — Operational design and observability

Now imagine the system has been deployed.

How do we know that it is working correctly?

We need to measure not only infrastructure health but also business health.

### Location ingestion SLOs

```
Location updates accepted/sec

Location processing p50/p95/p99

Percentage of online-intent drivers
with fresh locations

Out-of-order and duplicate update rates

Redis update failures

Location index projection lag
```

One especially valuable metric:

```
Percentage of eligible drivers
whose last location is older than 15 seconds
```

This tells us whether the live supply index is healthy.

### Matching SLOs

```
Nearby-search latency

Candidates returned per search

Eligible candidates per search

Average geographic radius required

Percentage of partial spatial searches

ETA calculation latency

No-driver-found rate
```

Also monitor false-positive candidates.

For example:

```
GEO candidate:
    Found in Redis.

Latest state:
    Actually busy or stale.
```

A high proportion of such candidates might indicate that our operational index is not being maintained efficiently.

### Dispatch SLOs

```
Time from ride creation to first offer

Time from offer creation to device acknowledgement

Time from offer to driver acceptance

Offer expiration percentage

Driver acceptance percentage

Offer cancellation percentage
```

### Assignment SLOs

```
Assignment transaction latency

Concurrent acceptance conflicts

Driver reservation failures

Database deadlocks

Idempotent retries

Duplicate active assignment violations
```

The last metric should always be zero.

If it's not, we have a serious correctness issue.

### WebSocket SLOs

```
Active connections

Connection failures

Reconnect rate

Gateway saturation

Offer delivery failures

Heartbeat timeouts

Outbound queue size
```

### Alert on symptoms, not just hardware

For example:

```
CPU = 70%
```

might not be a problem.

But:

```
Location freshness p99 = 45 seconds
```

is a user-facing problem.

Similarly:

```
Redis memory = 75%
```

might be fine.

But:

```
One GEO partition has 10x the write load
```

could indicate a hotspot.

Observability should be connected to the system's actual business guarantees.

# 12.18 Step 18 — Security and trustworthiness of location data

One important area we haven't given enough attention to is security.

Our system is processing extremely sensitive information:

- Driver's live location.
- Customer pickup and destination.
- Driver identity.
- Trip history.
- Active assignments.
- Communication sessions.

An attacker manipulating location data can directly affect matching and business operations.

For example:

```
D101 reports:

Actual location:
    Hyderabad

Spoofed location:
    Near a high-demand airport
```

Now the matching engine believes D101 is nearby.

That can lead to bad dispatch decisions or fraudulent behavior.

We need multiple security controls.

### A. Authenticate every driver

Every location update must belong to an authenticated driver.

Use:

- Short-lived access tokens.
- Secure refresh-token handling.
- Device/session validation.
- Token revocation.
- TLS.
- Rate limiting.

Never trust the `driverId` in the request body as proof of identity.

Derive it from the authenticated principal.

### B. Protect session ownership

A driver should have a controlled active session.

Use:

```
driverId
sessionEpoch
device/session identity
```

Reject location updates and acceptance commands from invalid or superseded sessions.

### C. Detect suspicious GPS behavior

Possible checks:

- Impossible speed between consecutive positions.
- Large unexplained jumps.
- GPS accuracy anomalies.
- Repeatedly identical coordinates despite expected movement.
- Location mismatch with trip progress.
- Suspicious device or application behavior.
- Inconsistent server-received timing.

For example:

```
Previous location:
    A

Next location:
    50 km away

Elapsed time:
    2 seconds
```

This is physically suspicious.

We should flag or reject the update according to a risk policy.

Don't blindly reject every jump, however. GPS errors and network buffering can produce unusual observations.

We can use confidence scoring and anomaly detection.

### D. Protect access to location data

The customer tracking service should only expose a driver's location to an authorized customer with an active relationship to that ride.

For example:

```
Customer A requests R501.

R501 assigned to D101.

Customer A can access the authorized
location projection of D101 for R501.
```

Customer B should not be able to retrieve D101's location simply by guessing a driver ID.

### E. Minimize retention

Don't store precise location history indefinitely unless required.

Define:

- Retention duration.
- Access controls.
- Encryption.
- Deletion policies.
- Auditing.
- Data residency requirements.

Location data should be treated as sensitive operational information.

# 12.19 Step 19 — A few subtle problems worth mentioning in advanced designs

These are not necessarily requirements for every interview, but knowing them helps you recognize where a supposedly simple matching architecture can fail.

## A. Location accuracy and uncertainty

Suppose:

```
D101:
    Reported distance = 2.9 km
    GPS accuracy = 100 m
```

Our matching radius is:

```
3 km
```

Can we confidently say D101 is inside the radius?

Not necessarily.

The reported position has uncertainty.

A system can:

- Prefer higher-accuracy candidates.
- Use a configurable uncertainty margin.
- Request another location update.
- Include uncertainty in ranking.
- Avoid rigidly rejecting a borderline candidate when a more reliable measurement is likely soon.

But we should not simply add GPS accuracy to the distance and assume the resulting value is exact.

Accuracy values are estimates of location uncertainty, not hard guaranteed boundaries.

## B. Heading and speed

Two drivers might be equally distant.

```
D101:
    Distance = 1 km
    Heading = Away from customer

D102:
    Distance = 1 km
    Heading = Toward customer
```

D102 might be a better candidate.

Similarly, speed can help estimate when a moving driver will reach the pickup.

But heading and speed are noisy.

Use them as ranking hints, not as authoritative routing facts.

## C. Map matching

GPS coordinates might place a driver slightly off the road.

Map matching attempts to align noisy GPS points with a plausible road segment.

For example:

```
Raw GPS:
    Slightly outside the road.

Map-matched position:
    Closest plausible road location.
```

This can improve route prediction, although matching to the wrong road is also possible.

For highly accurate navigation and fleet tracking, road-network context can be valuable.

## D. GPS spoofing and fraud

Location-based platforms should consider malicious or manipulated location reports.

Possible techniques:

- Device integrity checks.
- Motion consistency analysis.
- Server-side anomaly detection.
- Historical behavior analysis.
- Cross-checks against trip and geofence events.
- Risk scoring.

We shouldn't assume that a GPS coordinate received from a mobile device is guaranteed to represent the device's actual physical position.

## E. Privacy-aware customer tracking

Customers generally don't need unlimited access to a driver's complete location history.

We can expose:

```
Active ride:
    Current approximate driver location
    Expected pickup ETA
    Relevant trip status
```

We don't need to expose:

- Other customer trips.
- Driver history unrelated to the ride.
- Unnecessary precise location information after the trip ends.

This is an architectural decision, not just a UI decision.

# 12.20 Final architecture decision framework

Now let's create the framework you should use when facing a completely different system design problem.

Instead of remembering the Uber architecture, ask the following questions in order.

## Question 1: What is the primary data workload?

```
Continuous updates?
    -> Ingestion and write optimization.

Frequent nearby searches?
    -> Spatial indexing.

Historical reconstruction?
    -> Durable event stream/storage.

Persistent connections?
    -> Connection gateway architecture.
```

## Question 2: Which information can be lost?

```
Can lose latest location?
    -> Ephemeral live index with recovery.

Must preserve every event?
    -> Durable ingestion boundary.

Must preserve assignment?
    -> Transactional database.

Can recreate connection?
    -> No need for durable socket state.
```

## Question 3: How geographically distributed is the data?

```
Small region?
    -> One spatial index may work.

Large region?
    -> Partition spatial data.

High-density areas?
    -> Smaller cells / subsharding.

Multiple regions?
    -> Regional ownership and federated search.
```

## Question 4: How do we find candidates?

```
Small data set?
    -> Direct search.

Moderate scale?
    -> Redis GEO.

Large geographical search?
    -> H3 / geohash / S2 partition routing.

Uneven geographic density?
    -> Adaptive subdivision or hot-cell splitting.
```

## Question 5: What makes a candidate eligible?

```
Location freshness
Business availability
Current reservations
Service compatibility
Vehicle/skill/capacity
ETA
Time windows
```

The rules depend on the business.

## Question 6: How does the assignment happen?

```
One winner?
    -> Atomic conditional assignment.

One driver, multiple compatible tasks?
    -> Capacity reservation.

Future appointments?
    -> Time-slot reservation.

Multiple authoritative databases?
    -> Durable coordination protocol.
```

## Question 7: How should messages be delivered?

```
Client to server:
    HTTP / streaming

Server to active client:
    WebSocket / SSE

Background notification:
    Push

Durable work:
    Queue

Durable event history:
    Kafka
```

## Question 8: How does the system recover?

```
Duplicate?
    -> Idempotency.

Lost notification?
    -> Retry from durable offer state.

Server crash?
    -> Recover from database.

Redis failure?
    -> Rebuild live projection.

Database failure?
    -> Don't confirm assignment.

Regional split?
    -> Preserve authoritative ownership.
```

This framework is more reusable than any particular architecture diagram.

# 12.21 How I would explain this architecture in a system design interview

Imagine the interviewer asks:

> Design a real-time driver location tracking and matching system like Uber.

Here's how I would structure my answer.

### 1. Start with requirements

"I need to support continuous driver location updates, nearby driver discovery, real-time offers, and reliable assignment.

I'll treat location freshness and assignment correctness as different requirements.

Recent location loss is acceptable, but we must never assign the same ride to multiple drivers."

This establishes the most important architectural distinction.

### 2. Estimate scale

"I'll assume one million online drivers, sending locations every three seconds.

That gives approximately 333,000 location updates per second.

I'll separately account for concurrent WebSocket connections and nearby searches."

This justifies horizontal scaling.

### 3. Design location ingestion

"I'll use horizontally scaled ingestion servers.

Drivers can submit locations over HTTP or an existing WebSocket connection.

The ingestion layer authenticates drivers and validates coordinates, session epochs, and sequence numbers.

For live tracking, I'll write the latest locations into partitioned Redis GEO indexes.

I'll introduce Kafka only if historical location-event durability or multiple downstream consumers justify it."

Now you've explained Redis and Kafka based on requirements rather than technology preference.

### 4. Design nearby discovery

"I'll use Redis GEO initially.

As search volume grows, I'll partition the geographical indexes and use H3 or geohash to route searches to relevant cells.

The discovery service will retrieve candidate drivers, validate location freshness, fetch current availability, filter eligibility, and rank using ETA."

This establishes the search algorithm.

### 5. Design dispatch

"I'll use small-batch or staggered dispatch.

The orchestrator selects a few ranked drivers, reserves them, creates durable offers, and delivers them through WebSocket gateways.

A connection registry maps driver IDs to their owning gateways.

Push notifications can serve as a background fallback."

This covers real-time communication.

### 6. Design reliable assignment

"Driver acceptance is not automatically a successful assignment.

I'll use a transactional database with conditional state transitions and uniqueness constraints.

The transaction must atomically claim both the ride and the driver.

Idempotency keys make repeated acceptance requests safe.

A transactional outbox ensures that assignment and cancellation notifications can be retried."

This establishes correctness.

### 7. Handle failures

"Redis can lose recent live locations and recover through fresh driver updates or event replay.

WebSocket connections can reconnect.

Pending offers can recover from durable offer state.

Assignment transactions must remain durable.

I'll use bounded retries, exponential backoff, jitter, idempotent processing, and reconciliation."

### 8. Scale independently

"I'll scale ingestion, spatial indexes, matching, routing, WebSocket gateways, and assignment independently.

For multiple regions, location search can be regional and eventually consistent, while each assignment domain has a clearly defined authoritative transactional owner."

This demonstrates that the architecture can evolve.

# 12.22 Final complete architecture — Reference design

Here is the complete logical architecture I would use as the reference design for a large ride-hailing system.

\#chatgpt-mermaid-\_r_5sq\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_5sq\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_5sq\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_5sq\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_5sq\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_5sq\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_5sq\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_5sq\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_5sq\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_5sq\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_5sq\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_5sq\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_5sq\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_5sq\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_5sq\_ p{margin:0;}#chatgpt-mermaid-\_r_5sq\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_5sq\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_5sq\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_5sq\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_5sq\_ .label text,#chatgpt-mermaid-\_r_5sq\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_5sq\_ .node rect,#chatgpt-mermaid-\_r_5sq\_ .node circle,#chatgpt-mermaid-\_r_5sq\_ .node ellipse,#chatgpt-mermaid-\_r_5sq\_ .node polygon,#chatgpt-mermaid-\_r_5sq\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_5sq\_ .rough-node .label text,#chatgpt-mermaid-\_r_5sq\_ .node .label text,#chatgpt-mermaid-\_r_5sq\_ .image-shape .label,#chatgpt-mermaid-\_r_5sq\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_5sq\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_5sq\_ .rough-node .label,#chatgpt-mermaid-\_r_5sq\_ .node .label,#chatgpt-mermaid-\_r_5sq\_ .image-shape .label,#chatgpt-mermaid-\_r_5sq\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_5sq\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_5sq\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_5sq\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_5sq\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_5sq\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_5sq\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_5sq\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_5sq\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_5sq\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_5sq\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_5sq\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_5sq\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_5sq\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_5sq\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_5sq\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_5sq\_ .icon-shape,#chatgpt-mermaid-\_r_5sq\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_5sq\_ .icon-shape p,#chatgpt-mermaid-\_r_5sq\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_5sq\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_5sq\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_5sq\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_5sq\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_5sq\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_5sq\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_5sq\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_5sq\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_5sq\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_5sq\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_5sq\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_5sq\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_5sq\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_5sq\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_5sq\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_5sq\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_5sq\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_5sq\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_5sq\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_5sq\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_5sq\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_5sq\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_5sq\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_5sq\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_5sq\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_5sq\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_5sq\_ .node rect,#chatgpt-mermaid-\_r_5sq\_ .node circle,#chatgpt-mermaid-\_r_5sq\_ .node ellipse,#chatgpt-mermaid-\_r_5sq\_ .node polygon,#chatgpt-mermaid-\_r_5sq\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_5sq\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_5sq\_ .mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_5sq\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_5sq\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_5sq\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}Optional Historical ProcessingReliable MessagingDurable AssignmentDispatchDiscovery and MatchingLive Location InfrastructureLocation IngestionAPI and Communication LayerMobile ApplicationsKafka Location StreamHistorical Location StoreAnalytics / AuditingDelivery Queue / InternalRouterCancellation and NotificationWorkersAssignment ServiceTransactional DatabaseTransactional OutboxDispatch OrchestratorOffer and ReservationServiceOffer Timeout WorkersNearby Driver DiscoveryEligibility FilteringETA and RoutingCandidate RankingGeographic Partition RouterRedis GEO PartitionsOperational Driver StateFreshness and LivenessWorkersLocation Ingestion ServiceAuthentication / Validation /Sequence CheckDriver Location OwnerRegional Load BalancerAPI GatewayWebSocket Gateway FleetConnection RegistryDriver AppCustomer App

This architecture separates six major responsibilities:

1. Ingestion: Receive and validate continuous location updates.
2. Live location: Maintain fresh geographical state.
3. Matching: Identify and rank suitable drivers.
4. Dispatch: Manage offers, reservations, and timeouts.
5. Assignment: Enforce durable business correctness.
6. Delivery: Communicate decisions and recover from notification failures.

Historical processing, analytics, and customer tracking are additional capabilities that can be introduced according to requirements.

# 12.23 Final technology selection — What would I actually use?

Here's my practical starting recommendation.

| Layer                          | Starting technology                          | Reason                                    |
| ------------------------------ | -------------------------------------------- | ----------------------------------------- |
| Driver API                     | HTTP                                         | Simple location ingestion                 |
| Real-time communication        | WebSocket                                    | Bidirectional events                      |
| Background notification        | FCM/APNs                                     | Reach drivers when not actively connected |
| Ingestion                      | Stateless Java services                      | Horizontal scaling                        |
| Live location                  | Redis GEO                                    | Fast latest-value and spatial operations  |
| Spatial partitioning           | H3 or geohash when needed                    | Geographic locality                       |
| Driver liveness                | Redis leases + timestamps                    | Fast operational state                    |
| Historical events              | Kafka, when required                         | Durable event stream and replay           |
| Historical storage             | Suitable time-series/geospatial storage      | Retention and historical queries          |
| Matching                       | Stateless Java services                      | Independent scaling                       |
| ETA                            | Regional routing engine/provider             | Road-based travel estimates               |
| Dispatch                       | Java orchestration service                   | Offer lifecycle management                |
| Temporary business reservation | Transactional database                       | Authoritative reservation                 |
| Assignment                     | PostgreSQL or another transactional database | Atomic assignment                         |
| Event publication              | Transactional outbox                         | Recoverable event delivery                |
| Background delivery            | Queue or internal messaging                  | Asynchronous processing                   |
| Connection routing             | Redis registry + gateway RPC                 | Locate connected driver                   |
| Scheduling                     | Distributed scheduler/delayed queue          | Offer expiration and cleanup              |

These are reasonable defaults, not mandatory technologies.

The architecture should survive technology substitutions as long as the underlying guarantees remain intact.

# 12.24 The ultimate decision matrix — Never memorize one architecture

This is the most useful condensed reference from the entire series.

When you encounter a new system design question, identify the problem and choose the relevant pattern.

| Problem                                  | Pattern / concept                          |
| ---------------------------------------- | ------------------------------------------ |
| Continuous high-throughput updates       | Stateless ingestion + latest-value storage |
| Need to preserve every update            | Durable stream-first ingestion             |
| Fast nearby searches                     | Spatial indexing                           |
| One Redis GEO key becoming hot           | Partitioned GEO indexes                    |
| Search fan-out becoming expensive        | Geographic partitioning                    |
| Dense areas becoming hotspots            | Dynamic subdivision / subsharding          |
| Out-of-order updates                     | Versioning + atomic conditional writes     |
| Old driver locations                     | Freshness thresholds                       |
| Driver disconnects                       | Heartbeats + leases                        |
| Driver is connected but GPS is stale     | Independent liveness dimensions            |
| Find enough suitable candidates          | Overfetch + iterative fetching             |
| No nearby drivers                        | Progressive radius expansion               |
| Nearest driver isn't fastest             | ETA-based ranking                          |
| Deliver offers in real time              | WebSocket gateways                         |
| Driver is disconnected                   | Push fallback + state reconciliation       |
| Need reliable offer delivery             | Durable offer + outbox + retry             |
| Driver receives duplicate messages       | Event IDs + idempotent handling            |
| Two drivers accept the same ride         | Atomic ride claim                          |
| One driver is selected by multiple rides | Atomic driver reservation                  |
| Expiration races with acceptance         | Conditional state transitions              |
| Assignment succeeds but response is lost | Idempotency lookup                         |
| Redis and DB disagree                    | Reconciliation against authoritative state |
| One driver carries multiple orders       | Capacity reservation + route optimization  |
| Restaurant preparation affects dispatch  | Deferred dispatch                          |
| Driver supply is uneven                  | Supply-demand prediction                   |
| Multiple cities                          | Regional infrastructure                    |
| Regional search boundaries overlap       | Federated search                           |
| Multi-region assignment                  | Single authoritative owner + fencing       |
| Historical route reconstruction          | Durable events + historical storage        |

# 12.25 The ten principles I want you to internalize

These are the real takeaways. Understanding these will allow you to derive the architecture again without memorizing this document.

1\. Different data deserves different storage guarantees.

The latest location is replaceable. An assignment is not.

2\. High throughput is solved through workload isolation and partitioning, not merely by selecting an in-memory database.

A single Redis GEO key can still become a hotspot.

3\. Optimize writes and reads independently.

Driver-ID partitioning can make writes efficient, while geographic partitioning can make spatial searches efficient.

4\. A spatial index produces candidates, not guaranteed eligible drivers.

Always validate freshness, exact distance, availability, and business constraints.

5\. Discovery and assignment are fundamentally different operations.

Discovery finds potential candidates. Assignment gives them exclusive ownership.

6\. A persistent connection is not a durable communication guarantee.

WebSockets provide a live channel. Durable offer state, retries, acknowledgements, and reconciliation make the workflow reliable.

7\. Exactly-once business effects are built using idempotency and atomic state transitions.

Don't try to make an unreliable network magically deliver every message exactly once.

8\. The transactional database must enforce critical business invariants.

Redis locks can help coordination, but final assignment correctness should not depend on an ephemeral lock.

9\. Multi-region architecture requires explicit ownership.

Eventual consistency works well for geographical projections. Conflicting assignment decisions require authoritative coordination.

10\. Build architecture according to the bottleneck and business requirement.

Don't introduce Kafka, H3, distributed locks, or complex multi-region coordination unless they solve a real problem.

# Final conclusion

We've effectively built a reusable location-based marketplace platform.

The same core foundation can power:

```
Uber / Ola / Rapido
    -> Driver location + ride matching

Swiggy / Zomato
    -> Partner location + delivery assignment

Logistics
    -> Fleet tracking + vehicle allocation

Home services
    -> Technician location + skill-based scheduling

Fleet monitoring
    -> Continuous tracking + geofence events
```

The components change very little.

What changes are the eligibility rules, ranking objectives, reservation constraints, dispatch policies, and durability requirements.

The most important architectural separation is:

```
LOCATION
    Where is the provider?

DISCOVERY
    Who is geographically nearby?

ELIGIBILITY
    Who can perform this task?

MATCHING
    Who is the best candidate?

DISPATCH
    Whom should we contact and when?

RESERVATION
    Who temporarily owns this opportunity?

ASSIGNMENT
    Who has been authoritatively assigned?

DELIVERY
    How do we reliably communicate the result?
```

Once you understand these independently, you can combine them according to the requirements of almost any real-time location-based marketplace.

You don't need to remember the complete architecture. You need to recognize which problem you're solving, what guarantee it requires, and which mechanism provides that guarantee.

That's the foundation of designing these systems confidently.
