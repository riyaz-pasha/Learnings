# Reliable Location Tracking & Real-Time Matching

We'll approach this as a system that evolves step by step, just as we would design a real production system.

The goal is not to memorize an architecture containing Redis, Kafka, WebSockets, databases, and distributed locks.

The goal is to understand why each component exists, which problem it solves, what happens when it fails, and when we should or should not use it.

We'll build one core architecture and then modify it for different business requirements:

- Uber/Ola/Rapido: ride-hailing.
- Swiggy/Zomato: food delivery.
- Logistics: fleet tracking and shipment assignment.
- Hyperlocal services: plumbers, technicians, home services, etc.

These systems share many concepts, but their matching and reliability requirements can differ substantially.

# Chapter roadmap

| Chapter                                | What we'll understand                                                                                                       |
| -------------------------------------- | --------------------------------------------------------------------------------------------------------------------------- |
| 1. Problem decomposition               | What exactly are we building? What data needs to be stored, and what guarantees do we need?                                 |
| 2. Location ingestion                  | How millions of devices continuously send location updates. HTTP vs WebSockets vs gRPC, batching, gateways, validation.     |
| 3. High-throughput storage             | Redis, Redis GEO, databases, Kafka, queues, write optimization, partitioning, and durability.                               |
| 4. Geospatial indexing                 | Latitude/longitude, geohash, quadtrees, S2, H3, spatial partitioning, and radius search.                                    |
| 5. Nearby driver discovery             | How to efficiently find nearby drivers at scale, handle radius expansion, filtering, stale positions, and ranking.          |
| 6. Real-time dispatch                  | How to send requests to selected drivers, offer timeouts, fan-out strategies, and concurrent dispatch.                      |
| 7. Reliable acceptance                 | How to guarantee that only one driver accepts a request, using database constraints, atomic operations, and state machines. |
| 8. Driver availability and liveness    | Heartbeats, stale locations, offline detection, false positives, reconnects, and network failures.                          |
| 9. Reliability engineering             | Retries, duplicates, idempotency, ordering, message delivery guarantees, recovery, and durability.                          |
| 10. Scaling the architecture           | Redis Cluster, sharding, hot regions, partition ownership, multi-region architecture, and backpressure.                     |
| 11. Real-world architecture variations | Uber-style matching vs Swiggy-style assignment vs logistics and fleet tracking.                                             |
| 12. Putting everything together        | Complete production architecture, end-to-end flows, trade-offs, and system design interview approach.                       |

Let's start with the most important chapter.

# Chapter 1 — Understand the problem before choosing technologies

Imagine we're building a ride-hailing application.

There are:

- 1 million registered drivers.
- 200,000 drivers currently online.
- Each online driver sends their location every 3 seconds.
- Customers request rides.
- We need to find suitable nearby drivers.
- We need to send ride offers.
- Only one driver must win a particular ride request.

Your initial thought might be:

> Let's receive the location updates and put them into Redis. Redis is in-memory, so writes and geospatial queries will be fast.

That's a reasonable starting point.

But before choosing Redis, we need to understand that we're actually solving three different problems.

## 1.1 Three separate problems hiding inside this system

### Problem A: Location tracking

We need to continuously know where each driver is.

For example:

```
Driver-101
    10:00:00 -> (17.3850, 78.4867)
    10:00:03 -> (17.3852, 78.4869)
    10:00:06 -> (17.3855, 78.4871)
    10:00:09 -> (17.3858, 78.4873)
```

The latest location is what matters most for immediate matching.

If we lose the location at 10:00:06, is that a disaster?

Usually not.

The driver will send another update at 10:00:09.

This tells us something important:

The latest driver location is generally ephemeral, frequently overwritten data.

We don't necessarily need to preserve every update.

### Problem B: Nearby driver discovery

Suppose a customer requests a ride at:

```
Customer location:
Latitude: 17.3850
Longitude: 78.4867

Search radius: 3 km
```

We need to find drivers who are:

1. Within the relevant geographical area.
2. Online.
3. Available for a new ride.
4. Sending sufficiently fresh location updates.
5. Eligible for this particular ride.

This is a geospatial query combined with business filtering.

Notice that simply knowing a driver's coordinates isn't enough.

A driver can be nearby but:

- Already carrying a passenger.
- Offline.
- Not responding.
- Using an inaccurate GPS location.
- Temporarily unavailable.
- Already reserved for another request.

So nearby discovery is not the same as driver matching.

### Problem C: Reliable assignment

Suppose three drivers receive the same ride offer.

```
Ride-501

Driver-A ---- accepts
Driver-B ---- accepts
Driver-C ---- accepts
```

Their requests might reach our servers almost simultaneously.

We must guarantee:

```
Ride-501 -> Exactly one successful assignment
```

We cannot allow:

```
Ride-501 -> Driver-A
Ride-501 -> Driver-B
```

This is fundamentally a concurrency and consistency problem.

And here's the important distinction:

Losing a location update is acceptable. Losing control over who owns a ride is not.

These two kinds of data need different reliability guarantees.

## 1.2 The most important architectural decision: Separate data by purpose

Let's classify the data we have.

There are four different categories.

| Data                | Example                       | What matters most?                    |
| ------------------- | ----------------------------- | ------------------------------------- |
| Latest location     | Driver's current coordinates  | Freshness and speed                   |
| Driver availability | Online, available, busy       | Correctness and freshness             |
| Ride assignment     | Ride assigned to Driver-A     | Strong correctness and durability     |
| Location history    | Driver's previous coordinates | Durability, depending on requirements |

These categories should not necessarily use the same storage system.

Let's understand each.

### A. Latest location — Optimize for freshness

Suppose a driver sends:

```
{
  "driverId": "D101",
  "latitude": 17.3850,
  "longitude": 78.4867,
  "timestamp": 1794378600
}
```

Three seconds later:

```
{
  "driverId": "D101",
  "latitude": 17.3855,
  "longitude": 78.4871,
  "timestamp": 1794378603
}
```

We usually don't need both records in our live matching index.

We need:

```
D101 -> (17.3855, 78.4871)
```

The previous location can be overwritten.

This is a classic example of a latest-value workload.

Redis is an excellent candidate for this role because it provides fast in-memory operations and supports geospatial indexing.

But remember:

Redis is not automatically our permanent source of truth.

If its live location data disappears, drivers can repopulate it by sending new location updates.

That's a natural recovery mechanism.

### B. Driver availability — Optimize for correct state transitions

Consider these states:

```
OFFLINE
   |
   v
ONLINE_AVAILABLE
   |
   v
RESERVED_FOR_OFFER
   |
   v
BUSY
   |
   v
COMPLETED
   |
   v
ONLINE_AVAILABLE
```

The exact states depend on the business.

For example, receiving a location update should not automatically mean that the driver is available for a ride.

A driver could still be:

```
Location: Fresh
Availability: BUSY
```

Or:

```
Location: Stale
Availability: ONLINE
```

These are two different dimensions.

We should maintain them independently.

Location tells us where the driver appears to be. Availability tells us whether we may assign work to that driver.

We will explore this distinction in depth when we design the liveness and dispatch flows.

### C. Ride assignment — Optimize for correctness and durability

Consider:

```
Ride-501:
    status = REQUESTED
    assignedDriverId = null
```

After a successful assignment:

```
Ride-501:
    status = ACCEPTED
    assignedDriverId = D101
```

This transition is business-critical.

We need to survive:

- Application crashes.
- Redis restarts.
- Server restarts.
- Network failures.
- Duplicate acceptance requests.
- Multiple drivers accepting simultaneously.

We cannot simply store the successful assignment in a temporary Redis key and assume everything is safe.

We need an authoritative, durable transactional record.

Typically, that means a relational database or another datastore capable of enforcing the required atomic state transition.

For example:

```
UPDATE rides
SET
    status = 'ACCEPTED',
    assigned_driver_id = 'D101'
WHERE
    id = 'RIDE-501'
    AND status = 'REQUESTED'
    AND assigned_driver_id IS NULL;
```

Then check the affected row count.

- `1 row updated`: this request won.
- `0 rows updated`: someone else already won, or the ride is no longer eligible.

This gives us an important foundation for later:

The database decides who owns the ride. Redis can help coordinate the process, but it must not be the final authority for assignment correctness.

We'll improve this further when we discuss reservations, conditional updates, unique constraints, and transaction boundaries.

### D. Location history — Optimize according to product requirements

Now imagine the customer wants to see the driver's route after the ride.

Or the business needs:

- Route reconstruction.
- Billing verification.
- Dispute resolution.
- Fleet analytics.
- Driver behavior analysis.

Now the old location points matter.

Overwriting them in Redis would not satisfy this requirement.

We might store selected location events in a durable event stream and asynchronously process them into historical storage.

For example:

```
Location Updates
       |
       v
   Kafka
       |
       +------> Historical Location Store
       |
       +------> Analytics
       |
       +------> Fraud Detection
```

But we don't necessarily need to send every location update through Kafka.

We'll discuss when that is useful and when it is an unnecessary expense in Chapter 3.

# 1.3 Our first architectural principle

We should separate the live operational view from the durable business record.

Think about it this way:

```
                DRIVER MOBILE APP
                       |
                       |
                 Location Updates
                       |
                       v
              Location Ingestion
                       |
                       v
              LIVE LOCATION STORE
                   (Redis)
                       |
                       |
              Nearby Driver Search
                       |
                       v
                DISPATCH SERVICE
                       |
                       |
               Durable Assignment
                       |
                       v
                DATABASE
```

The two storage systems have different responsibilities.

| Redis                                      | Database                        |
| ------------------------------------------ | ------------------------------- |
| Latest driver location                     | Ride and assignment records     |
| Geospatial index                           | Authoritative driver/ride state |
| Temporary reservations (where appropriate) | Durable assignment history      |
| Frequently changing operational data       | Business-critical transactions  |

This does not mean that Redis cannot provide persistence or that a database cannot serve geospatial queries.

It means we're choosing each storage system according to the workload and correctness requirements.

# 1.4 Let's calculate the write throughput

This is where the architecture starts becoming interesting.

Assume:

```
Online drivers = 200,000
Location interval = 3 seconds
```

Each driver sends one update every 3 seconds.

Therefore:

\\[ \text{Writes per second} = \frac{200,000}{3} \\]

\\[ \boxed{\approx 66,667 \text{ writes/second}} \\]

Now imagine:

```
Online drivers = 1,000,000
Location interval = 3 seconds
```

\\[ \text{Writes per second} = \frac{1,000,000}{3} \\]

\\[ \boxed{\approx 333,333 \text{ writes/second}} \\]

And that's just location updates.

We haven't accounted for:

- Heartbeats.
- Nearby searches.
- Ride requests.
- Driver status changes.
- Assignment requests.
- WebSocket connections.
- Historical data processing.

Also, real traffic is not perfectly uniform. Drivers can reconnect together after a network outage, for example, creating sudden bursts.

### Why directly writing every update into a relational database can become expensive

Imagine persisting every location update permanently.

At approximately 333,333 updates per second:

\\[ 333,333 \times 86,400 \approx 28.8 \text{ billion updates/day} \\]

That's an enormous volume of records.

Most of these records may never be useful for the immediate matching operation.

We should ask:

> Do we really need to permanently store every location update, or do we mostly need the latest location?

For immediate matching, we mostly need the latest location.

This is why an in-memory latest-value store is attractive.

But there is one more important consideration:

A high-throughput system is not necessarily solved by selecting a fast database. We must also reduce unnecessary writes, distribute work, and avoid creating bottlenecks around shared data.

We'll explore all of these in the upcoming chapters.

# 1.5 Is Redis actually a good choice?

Yes, Redis is a strong candidate for live location tracking.

It provides:

- In-memory data access.
- Geospatial commands such as `GEOADD` and `GEOSEARCH`.
- Expiration using TTL.
- Atomic individual commands.
- Replication and clustering capabilities.
- Simple data structures for temporary operational state.

For example, conceptually:

```
GEOADD drivers:locations 78.4867 17.3850 D101
```

This adds the driver's geographical position.

We can then search:

```
GEOSEARCH drivers:locations
    FROMLONLAT 78.4867 17.3850
    BYRADIUS 3 km
    WITHDIST
```

This asks Redis to return members near the given coordinates.

We will explore the exact query behavior and its limitations later.

However, there are a few things you should understand now.

### Redis is not infinitely scalable just because it is in-memory

Suppose we put every driver in one geospatial index:

```
drivers:locations
    |
    +---- D1
    +---- D2
    +---- D3
    +---- ...
    +---- D1,000,000
```

As traffic grows, this key can become a hotspot.

Redis Cluster distributes keys across hash slots, not individual members inside a single key.

So a single huge geospatial key does not automatically become distributed across all Redis nodes.

This is a critical design consideration.

We will eventually look at:

- Partitioning by geographic cells.
- Redis Cluster.
- Regional indexes.
- Hotspot handling.
- Search across neighboring partitions.

For now, remember:

Redis GEO is a useful indexing mechanism, not a complete distributed location architecture.

### Redis data loss: Why it may be acceptable

Suppose Redis loses all live location data.

Initially:

```
D101 -> Location
D102 -> Location
D103 -> Location
```

After a restart:

```
D101 -> Missing
D102 -> Missing
D103 -> Missing
```

The drivers continue sending location updates:

```
D101 -> New Location
D102 -> New Location
D103 -> New Location
```

The live index gradually repopulates.

That's acceptable for many systems because the data is continuously refreshed.

However, there is a subtle issue.

Until the index is repopulated, we must not assume missing drivers are available for matching.

A missing location should mean:

```
Location Unknown
```

Not:

```
Location = Previous Location
```

And certainly not:

```
Driver = Available
```

This is one reason freshness and availability must be modeled explicitly.

# 1.6 Our fundamental data model

Before moving forward, let's define the logical entities.

These are not necessarily separate database tables. They represent different concepts in our system.

Driver

```
Driver
------
driverId
vehicleType
serviceRegion
accountStatus
```

Driver's latest location

```
DriverLocation
--------------
driverId
latitude
longitude
accuracy
speed
heading
deviceTimestamp
serverReceivedTimestamp
sequenceNumber
```

Driver operational state

```
DriverAvailability
------------------
driverId
onlineStatus
availabilityStatus
currentAssignmentId
lastHeartbeatTime
```

Ride request

```
Ride
----
rideId
customerId
pickupLatitude
pickupLongitude
status
assignedDriverId
createdAt
```

Driver offer

```
DriverOffer
-----------
offerId
rideId
driverId
status
expiresAt
createdAt
```

Notice that we've separated the ride from the driver offer.

Why?

Because one ride can be offered to multiple drivers over its lifetime.

For example:

```
Ride-501
   |
   +---- Offer to D101 -> EXPIRED
   |
   +---- Offer to D102 -> REJECTED
   |
   +---- Offer to D103 -> ACCEPTED
```

The offer is a temporary interaction.

The ride assignment is the final business decision.

This distinction will become extremely important when we design retries and concurrent acceptance.

# 1.7 Important guarantees: What must be reliable and what need not be?

This table is worth understanding deeply.

| Operation                | Required guarantee                                  |
| ------------------------ | --------------------------------------------------- |
| Receive location update  | Fast, duplicates tolerated                          |
| Update latest location   | Eventually reflects the freshest valid observation  |
| Nearby driver search     | Fast, sufficiently fresh results                    |
| Push offer to driver     | Retryable delivery                                  |
| Record driver acceptance | Durable and atomic                                  |
| Assign ride              | Exactly one successful winner                       |
| Store location history   | Depends on business requirements                    |
| Detect offline driver    | Eventual detection, with controlled false positives |

One particularly important distinction:

### We don't necessarily need exactly-once message delivery.

Suppose Driver D101 accepts an offer.

The driver's application sends:

```
Accept Offer-501
```

Our server processes it, but the response gets lost due to a network failure.

The driver retries.

Now our system receives the same acceptance request twice.

We need to make sure the second request doesn't cause another assignment or corrupt the ride state.

We solve this using:

- Idempotency keys.
- Durable state transitions.
- Conditional database updates.
- Well-defined state machines.

We do not need to magically guarantee that every message travels through the network exactly once.

Instead, we design operations so that repeated delivery is safe.

We'll revisit this carefully.

# 1.8 The architecture we're going to evolve

Here is our initial mental model.

\#chatgpt-mermaid-\_r_37p\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_37p\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_37p\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_37p\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_37p\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_37p\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_37p\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_37p\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_37p\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_37p\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_37p\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_37p\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_37p\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_37p\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_37p\_ p{margin:0;}#chatgpt-mermaid-\_r_37p\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_37p\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_37p\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_37p\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_37p\_ .label text,#chatgpt-mermaid-\_r_37p\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_37p\_ .node rect,#chatgpt-mermaid-\_r_37p\_ .node circle,#chatgpt-mermaid-\_r_37p\_ .node ellipse,#chatgpt-mermaid-\_r_37p\_ .node polygon,#chatgpt-mermaid-\_r_37p\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_37p\_ .rough-node .label text,#chatgpt-mermaid-\_r_37p\_ .node .label text,#chatgpt-mermaid-\_r_37p\_ .image-shape .label,#chatgpt-mermaid-\_r_37p\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_37p\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_37p\_ .rough-node .label,#chatgpt-mermaid-\_r_37p\_ .node .label,#chatgpt-mermaid-\_r_37p\_ .image-shape .label,#chatgpt-mermaid-\_r_37p\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_37p\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_37p\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_37p\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_37p\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_37p\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_37p\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_37p\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_37p\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_37p\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_37p\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_37p\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_37p\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_37p\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_37p\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_37p\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_37p\_ .icon-shape,#chatgpt-mermaid-\_r_37p\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_37p\_ .icon-shape p,#chatgpt-mermaid-\_r_37p\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_37p\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_37p\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_37p\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_37p\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_37p\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_37p\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_37p\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_37p\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_37p\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_37p\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_37p\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_37p\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_37p\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_37p\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_37p\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_37p\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_37p\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_37p\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_37p\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_37p\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_37p\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_37p\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_37p\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_37p\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_37p\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_37p\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_37p\_ .node rect,#chatgpt-mermaid-\_r_37p\_ .node circle,#chatgpt-mermaid-\_r_37p\_ .node ellipse,#chatgpt-mermaid-\_r_37p\_ .node polygon,#chatgpt-mermaid-\_r_37p\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_37p\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_37p\_ .mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_37p\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_37p\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_37p\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}Driver Mobile AppLocation IngestionLive Location Index - RedisKafkaHistorical Location StorageNearby Driver DiscoveryCustomer Ride RequestDispatch ServiceDriver Offer DeliveryAssignment ServiceTransactional DatabaseLocation UpdatesOptional durable event streamWebSocket / Push NotificationAccept OfferAssignment Result

This is not yet the final architecture.

For example:

- We haven't explained how to ingest hundreds of thousands of updates per second.
- We haven't explained how to partition Redis.
- We haven't explained how to query neighboring geographical areas.
- We haven't explained how to reserve a driver.
- We haven't explained how to handle duplicate acceptance.
- We haven't explained how to determine that a driver is offline.
- We haven't explained whether Kafka belongs in the critical location ingestion path.

Those are exactly the problems we'll solve next.

## Chapter 1 — What you should understand

There are three independent problems:

1. Tracking: Know the latest location of each driver.
2. Discovery: Find eligible drivers near a customer.
3. Assignment: Correctly assign a request to exactly one driver.

And there are two fundamental categories of data:

- Fast-changing operational data: Latest locations, temporary offers, live availability.
- Durable business data: Ride assignments, completed trips, payments, and other business-critical records.

Redis is a strong candidate for the live location index. A transactional database is appropriate for authoritative assignments. Kafka is optional and depends on whether we need a durable event stream for history, replay, and downstream processing.

Getting this separation right makes every later architectural decision easier.

Next: Chapter 2 — Location ingestion architecture.

We'll start with a driver sending a GPS update every few seconds and progressively build the ingestion pipeline. We'll answer:

- Should the mobile app use HTTP, WebSockets, MQTT, or gRPC?
- Should every update go directly to Redis?
- What does the load balancer do?
- Why do we need multiple ingestion servers?
- How do we prevent old or out-of-order GPS updates from overwriting newer ones?
- Should we batch location updates?
- How do we handle reconnects and traffic bursts?
- How do we acknowledge an update safely?

Say next to continue.

