# Chapter 5 — Nearby Driver Discovery and Matching Algorithms

We've now built the location ingestion system, understood how Redis stores driver locations, and explored geographical indexing.

Now we're going to build the component that actually decides:

Which drivers should receive a particular ride or delivery request?

This component is commonly called the Matching Engine or Dispatch Engine.

Let's begin with a simple scenario.

A customer requests a ride.

```
Ride ID: R501

Pickup:
    Latitude: 17.3850
    Longitude: 78.4867

Vehicle Type:
    Bike

Customer Search Radius:
    3 km
```

Our system currently has:

```
D101 -> 0.3 km -> Available
D102 -> 0.5 km -> Busy
D103 -> 0.8 km -> Available
D104 -> 1.0 km -> Location Stale
D105 -> 1.2 km -> Available
D106 -> 1.5 km -> Reserved for another ride
D107 -> 2.0 km -> Available
D108 -> 2.5 km -> Available
D109 -> 3.5 km -> Available
```

Which drivers should we select?

Obviously:

- D102 is busy.
- D104 has a stale location.
- D106 is already reserved.
- D109 is outside our radius.

The eligible drivers are:

```
D101
D103
D105
D107
D108
```

But there is a deeper problem.

Imagine that our system has one million online drivers and 10,000 ride requests arriving simultaneously.

We cannot fetch every nearby driver, perform expensive calculations for everyone, and send notifications to everyone.

We need an efficient algorithm.

Before designing it, let's establish three separate responsibilities.

## 5.1 Discovery, eligibility, and dispatch are different operations

This distinction is fundamental.

### A. Discovery — Who is geographically nearby?

Input:

```
Pickup coordinates
Search radius
```

Output:

```
D101
D102
D103
D104
D105
...
```

Discovery uses geographical indexes.

It doesn't necessarily know whether every driver is currently available.

Its job is to produce a geographically relevant candidate set.

### B. Eligibility — Who can actually accept this request?

Now inspect the candidates.

```
D101 -> Available
D102 -> Busy
D103 -> Available
D104 -> Offline
D105 -> Reserved
```

We eliminate drivers who cannot serve this request.

Eligibility involves:

- Availability.
- Location freshness.
- Vehicle type.
- Service area.
- Driver account status.
- Existing assignment.
- Temporary reservations.
- Delivery capabilities.
- Other business constraints.

### C. Dispatch — Whom should we contact?

Suppose we have:

```
D101 -> ETA 2 minutes
D103 -> ETA 4 minutes
D105 -> ETA 5 minutes
D107 -> ETA 6 minutes
```

We must decide whether to:

- Contact D101 first.
- Contact D101 and D103 simultaneously.
- Contact the nearest five drivers.
- Contact drivers in batches.
- Expand the radius if nobody accepts.

That's dispatch policy.

It is not a geographical search problem.

Our architecture should separate these responsibilities:

```
                 Ride Request
                      |
                      v
              Driver Discovery
                      |
                      v
               Candidate IDs
                      |
                      v
              Eligibility Check
                      |
                      v
              Eligible Drivers
                      |
                      v
               Driver Ranking
                      |
                      v
              Dispatch Strategy
                      |
                      v
                Driver Offers
```

Why does this separation matter?

Because we may change our dispatch policy without changing our spatial indexing implementation.

For example, we might initially contact the nearest driver and later introduce parallel offers to improve acceptance latency.

The discovery algorithm remains largely the same.

# 5.2 Building the first nearby-driver query

Let's use the architecture from the previous chapters.

Assume Redis contains a GEO index.

```
geo:{p0}
geo:{p1}
geo:{p2}
geo:{p3}
```

These are our four driver-ID partitions.

A ride request arrives.

The matching service has:

```
Pickup: (17.3850, 78.4867)
Radius: 3 km
Required vehicle: Bike
```

What do we do?

### Step 1: Search the relevant Redis GEO partitions

For each partition, issue a geographic query.

Conceptually:

```
GEOSEARCH geo:{p0}
    FROMLONLAT 78.4867 17.3850
    BYRADIUS 3 km
    WITHDIST
    ASC
```

Repeat for the other partitions.

We should execute these queries in parallel, not sequentially.

```
                    Matching Service
                           |
                 +---------+---------+
                 |         |         |
                 v         v         v
              Redis P0  Redis P1  Redis P2
                 |         |         |
                 +---------+---------+
                           |
                           v
                     Merge Results
```

If there are 16 partitions, we don't want:

```
P0 -> Wait
P1 -> Wait
P2 -> Wait
...
P15 -> Wait
```

We want concurrent queries with an overall deadline.

For example:

```
Search deadline: 50 ms
```

The 50 ms is illustrative; actual limits should come from latency requirements and measured infrastructure performance.

### Step 2: Merge candidates

Suppose the partitions return:

```
P0:
    D101 -> 0.3 km
    D103 -> 0.8 km

P1:
    D102 -> 0.5 km
    D105 -> 1.2 km

P2:
    D104 -> 1.0 km
    D107 -> 2.0 km

P3:
    D108 -> 2.5 km
```

Merge them:

```
D101 -> 0.3 km
D102 -> 0.5 km
D103 -> 0.8 km
D104 -> 1.0 km
D105 -> 1.2 km
D107 -> 2.0 km
D108 -> 2.5 km
```

Sort by distance.

Now we have a geographically ordered candidate list.

### Step 3: Apply eligibility filters

Retrieve the latest operational state.

```
D101 -> Available
D102 -> Busy
D103 -> Available
D104 -> Stale
D105 -> Available
D107 -> Available
D108 -> Available
```

Filter:

```
D101
D103
D105
D107
D108
```

Now we have eligible candidates.

But wait.

There is a subtle problem with this approach.

# 5.3 The Top-K problem — How many drivers should we fetch?

Imagine our search radius contains 10,000 drivers.

Should we retrieve all 10,000?

Usually not.

We might only need five or ten good candidates.

Suppose our requirement is:

```
Return the 10 nearest eligible drivers.
```

We might initially query:

```
GEOSEARCH geo:{p0} ... COUNT 10 ASC
```

And similarly for other partitions.

But consider:

```
Nearest 10 drivers:

D101 -> Busy
D102 -> Busy
D103 -> Busy
D104 -> Stale
D105 -> Busy
D106 -> Reserved
D107 -> Busy
D108 -> Busy
D109 -> Available
D110 -> Available
```

We retrieved 10 drivers, but only two were eligible.

We need eight more.

If we stop after fetching ten geographical candidates, we might unnecessarily conclude that driver supply is insufficient.

This is the Top-K after filtering problem.

## Solution A: Overfetch

Instead of fetching ten candidates, request more.

For example:

```
Required eligible candidates = 10

Initial geographical candidates = 50
```

After filtering:

```
Eligible = 14
```

We can select the ten best.

Pseudocode:

```
List<Driver> candidates = geoSearch(
    pickup,
    radius,
    50
);

List<Driver> eligible = filterEligible(candidates);

return eligible.stream()
        .limit(10)
        .toList();
```

This is a simple and practical optimization.

But it doesn't guarantee that we'll always find ten eligible drivers.

What if all 50 are unavailable?

We need another strategy.

## Solution B: Iterative fetching

We can progressively fetch more candidates until we have enough eligible drivers or exhaust the search region.

```
Fetch 20
   |
   v
Filter
   |
   v
Enough eligible?
   |
   +---- Yes -> Return
   |
   +---- No
           |
           v
        Fetch more
           |
           v
        Filter again
```

This can work with paginated or progressively expanded queries.

One implementation detail matters here:

For every Redis partition, maintain its own search progress. Merge results using a min-heap or sorted merge so that we can progressively fetch the next nearest candidates without repeatedly downloading everything.

Also, don't assume that `COUNT 10` guarantees the ten nearest geographically relevant members in every Redis query mode. In Redis GEO, use explicit ordering (`ASC`), and avoid the `ANY` optimization when exact nearest-result ordering is required.

### A subtle mathematical point

Suppose we query four Redis partitions.

If every partition returns its ten nearest candidates, then merging these results and selecting the global ten nearest candidates is sufficient to identify the global top ten geographically.

Why?

Any candidate ranked 11th or later in its own partition cannot be among the global ten nearest, because that partition alone already contains at least ten candidates closer to the pickup.

However, this guarantee applies to the geographical top ten.

It does not automatically give us the ten nearest eligible drivers.

If nine of the first ten candidates are unavailable, we need to fetch additional candidates.

That's why overfetching or iterative fetching is necessary.

## Solution C: Maintain availability-aware spatial indexes

We can also maintain a separate GEO index containing only available drivers.

Conceptually:

```
geo:all-online-drivers
geo:available-drivers
```

When a driver becomes busy:

```
Remove from geo:available-drivers
```

When a driver becomes available again:

```
Add to geo:available-drivers
```

Now a nearby search can directly query the available-driver index.

This reduces the number of unavailable candidates returned.

However, there are trade-offs:

1. Every availability transition must update the spatial index.
2. A driver changing cells may require additional index maintenance.
3. Redis availability indexes can temporarily be stale relative to authoritative state.
4. Concurrent reservations still require atomic coordination.

Therefore, the availability GEO index is an optimization, not the authority.

Even if a driver appears in the available-driver index, we must still reserve that driver atomically before offering or assigning work.

We'll implement that guarantee in Chapter 7.

# 5.4 Filtering drivers efficiently

We need to apply several filters, but not all filters have the same cost.

Imagine we have 500 geographical candidates.

We could retrieve all their details and perform every check individually.

But that might create unnecessary database calls.

Instead, use a filtering pipeline.

### Filter 1: Location freshness

A driver might have disappeared from the network.

For example:

```
D101 -> Last update 2 seconds ago
D102 -> Last update 4 seconds ago
D103 -> Last update 35 seconds ago
```

Suppose our freshness threshold is 15 seconds.

D103 is eliminated.

We should use the server's last successful update-received time, not blindly trust the device clock.

### Filter 2: Online and availability state

```
D101 -> ONLINE_AVAILABLE
D102 -> BUSY
D103 -> ONLINE_AVAILABLE
D104 -> RESERVED
```

Only eligible availability states remain.

### Filter 3: Vehicle and service compatibility

Suppose the request is for a bike.

```
D101 -> Bike
D102 -> Car
D103 -> Bike
D104 -> Scooter
```

D102 and D104 can be eliminated if they are not compatible with the requested service.

For a delivery platform, filters might include:

- Two-wheeler vs three-wheeler.
- Food delivery vs grocery delivery.
- Carrying capacity.
- Temperature-controlled vehicle.
- Delivery-partner eligibility.
- Current order capacity.

### Filter 4: Exact distance

Recalculate distance using the latest accepted coordinates.

Why?

Because the candidate might have moved after our initial geographical search.

Suppose:

```
Initial location:
D101 -> 2.8 km from customer

Latest location:
D101 -> 3.4 km from customer
```

The driver is no longer within our 3 km radius.

Discard the candidate.

### Filter 5: Business restrictions

Depending on the application:

- Driver is not suspended.
- Driver serves this region.
- Driver supports the requested product/service.
- Driver is not in an incompatible operational state.

The important design principle:

Use cheap filters first, expensive calculations later.

For example, we shouldn't calculate road ETA for a driver who is already busy.

# 5.5 A tricky problem — Drivers move while searches are happening

Let's consider a real-time scenario.

At 10:00:00:

```
D101 -> Customer is 2.0 km away
```

A customer requests a ride.

Our matching service searches Redis and finds D101.

But before the matching service sends an offer:

```
10:00:01 -> D101 moves
10:00:02 -> D101 sends new location
10:00:03 -> Matching service processes candidate
```

Now D101 might be 3.5 km away.

Should we still offer the ride?

Not necessarily.

The initial location was only a snapshot.

This is unavoidable in a continuously moving system.

We cannot freeze the world while searching.

### Solution: Treat discovery results as provisional

The flow should be:

```
Redis GEO Search
       |
       v
Candidate D101
       |
       v
Fetch Latest Driver State
       |
       v
Check Freshness and Availability
       |
       v
Recalculate Distance / ETA
       |
       v
Reserve Driver
       |
       v
Send Offer
```

We revalidate the driver close to the time we intend to dispatch the request.

However, there is no need to prevent the driver from sending new location updates while the matching request is being processed.

Location tracking and availability are independent.

For example:

```
D101:

Location:
    Continuously changing

Availability:
    ONLINE_AVAILABLE
         |
         v
    RESERVED_FOR_OFFER
```

Even while reserved, the driver can continue sending GPS updates.

The matching engine simply treats that driver as temporarily unavailable for other conflicting assignments.

### Do we need a globally consistent snapshot of every driver's location?

Usually not.

That would be extremely expensive and unnecessary.

We can accept a slightly inconsistent candidate snapshot because:

1. Location changes continuously anyway.
2. We validate the candidate again before dispatch.
3. Reservation protects against concurrent assignment.
4. The driver can accept or reject based on the actual offer.

This is an example of choosing the right consistency guarantee for the workload.

# 5.6 Radius expansion — What if nearby drivers are insufficient?

This is one of the most important matching algorithms.

Suppose a customer requests a ride.

Initial radius:

```
2 km
```

We search.

```
Candidates = 2

Eligible = 1
```

But we need five candidates.

What should happen?

We gradually expand the search radius.

```
2 km
  |
  v
3 km
  |
  v
5 km
  |
  v
8 km
```

For example:

| Radius | Geographical candidates | Eligible candidates |
| ------ | ----------------------- | ------------------- |
| 2 km   | 8                       | 1                   |
| 3 km   | 25                      | 5                   |
| 5 km   | 70                      | 18                  |
| 8 km   | 180                     | 45                  |

We now have enough candidates at 3 km.

We don't need to search 5 km or 8 km.

### How do we implement this?

A straightforward algorithm:

```
int[] radii = {2, 3, 5, 8};

for (int radius : radii) {

    List<Driver> candidates =
        discoverNearbyDrivers(pickup, radius);

    List<Driver> eligible =
        filterEligible(candidates);

    if (eligible.size() >= requiredCandidates) {
        return rank(eligible);
    }
}

return handleInsufficientSupply();
```

This is illustrative pseudocode.

A production implementation should also impose:

- A maximum search latency.
- A maximum radius.
- A maximum candidate count.
- A maximum routing/ETA budget.

### Avoid querying the same drivers repeatedly

Suppose we search 2 km and find:

```
D101
D102
D103
```

Then we expand to 3 km.

We should avoid processing those three drivers again.

We can maintain a set of already discovered candidate IDs.

```
Already discovered:
D101, D102, D103

Newly discovered at 3 km:
D104, D105, D106
```

We only process the new candidates.

### Another approach: Search once using a maximum radius

Suppose our maximum search radius is 8 km.

We can query all candidates within 8 km once.

Then divide them into distance bands.

```
0-2 km   -> Ring 1
2-3 km   -> Ring 2
3-5 km   -> Ring 3
5-8 km   -> Ring 4
```

The dispatch service can start with Ring 1.

If insufficient candidates are available, it expands into Ring 2, and so on.

Advantages:

- One geographical search rather than repeated searches.
- Candidates are already available.
- Less repeated Redis query overhead.

Disadvantages:

- We retrieve more data initially.
- More memory and filtering work.
- Large queries can become expensive in dense areas.

### Which approach should we choose?

| Situation                         | Suitable approach                               |
| --------------------------------- | ----------------------------------------------- |
| Dense urban locations             | Smaller initial search, expand only when needed |
| Search queries are cheap          | Progressive radius expansion                    |
| Redis search latency is expensive | Search a bounded maximum area once              |
| Very large candidate populations  | Use cell-based region expansion                 |
| Rural locations                   | Larger initial radius may be appropriate        |

The expansion policy can also depend on the region.

For example:

```
Dense City:
    1 km -> 2 km -> 3 km -> 5 km

Suburban:
    2 km -> 4 km -> 6 km -> 10 km

Rural:
    5 km -> 10 km -> 15 km -> 25 km
```

These are illustrative values, not recommended universal production settings.

The objective is not to find a fixed number of drivers at any cost. It is to find enough suitable drivers within an acceptable pickup-time budget.

# 5.7 Ranking — The nearest driver is not necessarily the best driver

Suppose we have three eligible drivers.

```
D101 -> Straight distance: 0.5 km
D102 -> Straight distance: 1.0 km
D103 -> Straight distance: 1.5 km
```

A naive matching algorithm selects D101.

But consider:

| Driver | Straight-line distance | Estimated road ETA |
| ------ | ---------------------- | ------------------ |
| D101   | 0.5 km                 | 12 minutes         |
| D102   | 1.0 km                 | 4 minutes          |
| D103   | 1.5 km                 | 5 minutes          |

D102 is the best candidate.

Why?

Maybe D101 is separated from the customer by a river or restricted road.

This is why ranking should use more than straight-line distance.

## Possible ranking factors

| Factor                 | Purpose                                   |
| ---------------------- | ----------------------------------------- |
| ETA                    | How quickly can the driver reach pickup?  |
| Road distance          | Actual route distance                     |
| Availability           | Is the driver genuinely free?             |
| Location freshness     | How trustworthy is the location?          |
| Vehicle compatibility  | Can the driver fulfil the request?        |
| Acceptance probability | How likely is the driver to accept?       |
| Driver reliability     | Historical operational reliability        |
| Fairness               | Avoid repeatedly starving certain drivers |
| Current workload       | Avoid overloading certain drivers         |

These factors are not all equally important.

A possible ranking model:

\\[ Score = w_1(ETA) + w_2(LocationAge) + w_3(Workload) + w_4(OtherFactors) \\]

Lower scores could indicate better candidates, depending on how each feature is normalized.

We can also build a predictive model that estimates:

\\[ P(\text{Driver accepts the request}) \\]

And:

\\[ P(\text{Driver reaches pickup successfully}) \\]

But don't jump directly into machine learning.

Start with a deterministic ranking model based on operationally meaningful factors.

# 5.8 How do we calculate ETA efficiently?

Suppose spatial discovery returns 500 drivers.

Should we call a routing service 500 times?

Usually not.

That could add significant latency and cost.

Instead, use a two-stage ranking strategy.

### Stage 1: Cheap geographical ranking

Use straight-line distance to select an initial shortlist.

```
500 drivers
    |
    v
Filter and rank by distance
    |
    v
Top 30 drivers
```

### Stage 2: Calculate ETA for shortlisted drivers

Now send the shortlisted coordinates to a routing engine.

Conceptually:

```
             Routing Engine

Customer Pickup
       |
       +---- D101 -> 3 minutes
       |
       +---- D102 -> 5 minutes
       |
       +---- D103 -> 4 minutes
       |
       +---- D104 -> 7 minutes
```

We can use a route-matrix operation that estimates travel information from one pickup location to multiple driver locations.

The final result might be:

```
D101 -> 3 minutes
D103 -> 4 minutes
D102 -> 5 minutes
```

Now dispatch can use ETA-based ranking.

### Important optimizations

- Use a local routing engine or suitable routing provider.
- Batch route-matrix requests.
- Reuse short-lived route estimates where appropriate.
- Apply request deadlines.
- Fall back to straight-line distance when the routing service is unavailable.
- Do not block the entire dispatch pipeline indefinitely waiting for ETA.

Also, estimates should account for which side of the road a driver is on, road access, traffic, and route restrictions where supported.

Geographical distance is a candidate-discovery tool. ETA is a dispatch-quality tool.

# 5.9 Nearby query performance at very large scale

Now imagine:

```
Online drivers = 1,000,000
Concurrent ride requests = 10,000
```

Each ride request performs nearby discovery.

Even if Redis handles location ingestion efficiently, nearby searches can become expensive.

We need to optimize this workload separately.

### Optimization A: Parallel partition queries

We already discussed this.

Query the relevant Redis partitions concurrently.

Use bounded concurrency and request deadlines.

Don't let one slow partition hold up the entire matching operation.

### Optimization B: Geographic routing

If we're using H3 or geohash-based partitions, determine which cells intersect the pickup search region.

Search only those cells.

Instead of searching 100 Redis partitions, perhaps only five geographical regions are relevant.

The actual savings depend on how storage is partitioned.

### Optimization C: Cell-level candidate caching

Imagine 500 customers request rides from the same neighbourhood within a few seconds.

Their pickup locations are close.

We might maintain a short-lived cache:

```
Cell A:
    Nearby candidate IDs

Cell B:
    Nearby candidate IDs

Cell C:
    Nearby candidate IDs
```

A new request can use the cached candidate IDs as an initial shortlist.

But this cache must not be treated as authoritative.

For every request, validate:

- Current driver location.
- Location freshness.
- Current availability.
- Existing reservation.
- Current eligibility.

Why?

Because a cached candidate list may contain a driver who has already accepted another ride.

Therefore:

Cache discovery results, not assignment decisions.

A cache TTL might be a fraction of a second or a few seconds, depending on movement, traffic, and latency requirements.

It must be measured, not guessed.

### Optimization D: Keep frequently used metadata close to matching

Repeatedly performing individual database queries for every candidate is expensive.

We can maintain a fast operational driver-state store containing:

```
driverId
availability
vehicleType
serviceEligibility
lastLocationReceivedAt
currentReservation
```

This could be in Redis or another suitable low-latency state store.

The authoritative business state remains in the database.

The matching service uses this operational state to shortlist candidates, and the assignment workflow validates the final state atomically.

### Optimization E: Limit expensive routing operations

Don't send all geographical candidates to the routing system.

Only send a shortlist.

Use an explicit ETA budget per matching request.

### Optimization F: Admission control

If the matching service is overloaded, we must not queue an unlimited number of searches.

Use:

- Request deadlines.
- Bounded work queues.
- Per-region concurrency limits.
- Priority handling.
- Backpressure.
- Controlled overload rejection or fallback policies.

This protects the rest of the system.

# 5.10 What happens when many customers request rides simultaneously?

This brings us to a very important distinction between discovery and reservation.

Imagine:

```
Customer A -> Ride A
Customer B -> Ride B
Customer C -> Ride C
```

All three customers search the same geographical area.

Their search results might contain:

```
D101
D102
D103
D104
```

All three matching requests might discover D101 as the nearest available driver.

Is this necessarily a problem?

No.

Discovery only identifies possible candidates.

It does not grant ownership.

However, we need to avoid sending conflicting offers and assignments.

One approach is a temporary driver reservation.

For example:

```
D101:
    ONLINE_AVAILABLE
          |
          v
    RESERVED_FOR_RIDE_A
```

When Ride B tries to reserve D101, it must fail.

The reservation operation must be atomic.

Conceptually:

```
Reserve D101 for Ride A

Success:
    D101 is reserved.

Reserve D101 for Ride B

Failure:
    D101 is already reserved.
```

This is where a conditional update, compare-and-set operation, or carefully designed lease can help.

But we must be careful with the semantics.

A temporary offer reservation is not the same as a permanent ride assignment.

The reservation may expire if the driver doesn't respond.

Also, if an offer expires while the driver is accepting, we must define which operation wins.

We'll design these transitions and failure scenarios in Chapters 6 and 7.

For now, remember:

Finding a driver is a read operation. Acquiring the right to offer or assign work is a concurrency-controlled state transition.

Don't solve concurrency by locking every driver during geographical discovery.

That would create unnecessary contention.

# 5.11 Different matching strategies for different products

Now let's adapt the same discovery system to different businesses.

## Pattern A — Uber/Ola/Rapido-style ride matching

Typical goal:

Find a driver who can reach the pickup quickly and complete the trip.

Important ranking factors:

- Pickup ETA.
- Vehicle category.
- Driver availability.
- Acceptance probability.
- Regional supply and demand.

Possible flow:

```
Ride Request
     |
     v
Nearby Driver Discovery
     |
     v
Filter Available Drivers
     |
     v
ETA Calculation
     |
     v
Rank Drivers
     |
     v
Dispatch Offers
```

Ride requests are often location-sensitive and time-sensitive.

Fast assignment matters.

## Pattern B — Swiggy/Zomato-style delivery partner matching

Delivery matching can have additional constraints.

For example:

- Restaurant pickup location.
- Food preparation time.
- Customer delivery location.
- Delivery partner capacity.
- Vehicle type.
- Current orders.
- Multi-order batching.
- Maximum detour.
- Pickup deadlines.

Consider:

```
Restaurant -> R1
Customer   -> C1
Partner    -> D101
```

The nearest delivery partner might not be the best choice.

Why?

A partner may already have another order whose route is compatible with this one.

We may want:

```
D101:
    Existing order -> Restaurant A -> Customer A

New order:
    Restaurant B -> Customer B
```

If both routes are geographically compatible, the platform might assign both orders to one partner.

Now matching becomes a constrained optimization problem.

We need to consider:

- Route compatibility.
- Combined ETA.
- Order preparation time.
- Delivery deadlines.
- Partner capacity.
- Maximum acceptable detour.

This is more complex than simply finding the nearest free driver.

The spatial discovery algorithm is still useful, but the ranking and assignment logic changes.

## Pattern C — Logistics and fleet dispatch

Suppose a company needs to assign a truck to a shipment.

The matching requirements might be:

- Truck capacity.
- Vehicle dimensions.
- Current route.
- Destination.
- Delivery deadline.
- Fuel or battery range.
- Driver operating hours.
- Geographic proximity.

Here, the nearest truck might not be suitable at all.

Matching is a combination of spatial search and constraint satisfaction.

## Pattern D — Home services

Imagine matching a technician to a customer.

The system may need:

- Technician location.
- Service skill.
- Availability slot.
- Certification.
- Appointment time.
- Expected travel time.

Again, nearby discovery is only one stage.

The same core architecture works, but the eligibility and ranking criteria change.

# 5.12 Complete nearby discovery architecture

Let's assemble everything.

\#chatgpt-mermaid-\_r_4gu\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_4gu\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_4gu\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_4gu\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_4gu\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4gu\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_4gu\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_4gu\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_4gu\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_4gu\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_4gu\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_4gu\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4gu\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4gu\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_4gu\_ p{margin:0;}#chatgpt-mermaid-\_r_4gu\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4gu\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4gu\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4gu\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_4gu\_ .label text,#chatgpt-mermaid-\_r_4gu\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4gu\_ .node rect,#chatgpt-mermaid-\_r_4gu\_ .node circle,#chatgpt-mermaid-\_r_4gu\_ .node ellipse,#chatgpt-mermaid-\_r_4gu\_ .node polygon,#chatgpt-mermaid-\_r_4gu\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_4gu\_ .rough-node .label text,#chatgpt-mermaid-\_r_4gu\_ .node .label text,#chatgpt-mermaid-\_r_4gu\_ .image-shape .label,#chatgpt-mermaid-\_r_4gu\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_4gu\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_4gu\_ .rough-node .label,#chatgpt-mermaid-\_r_4gu\_ .node .label,#chatgpt-mermaid-\_r_4gu\_ .image-shape .label,#chatgpt-mermaid-\_r_4gu\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_4gu\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_4gu\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4gu\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4gu\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_4gu\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_4gu\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_4gu\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_4gu\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_4gu\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_4gu\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_4gu\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4gu\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4gu\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_4gu\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4gu\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_4gu\_ .icon-shape,#chatgpt-mermaid-\_r_4gu\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_4gu\_ .icon-shape p,#chatgpt-mermaid-\_r_4gu\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_4gu\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_4gu\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_4gu\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_4gu\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_4gu\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_4gu\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_4gu\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_4gu\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_4gu\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4gu\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_4gu\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_4gu\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_4gu\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4gu\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_4gu\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_4gu\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4gu\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_4gu\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_4gu\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4gu\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_4gu\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4gu\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_4gu\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_4gu\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_4gu\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_4gu\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_4gu\_ .node rect,#chatgpt-mermaid-\_r_4gu\_ .node circle,#chatgpt-mermaid-\_r_4gu\_ .node ellipse,#chatgpt-mermaid-\_r_4gu\_ .node polygon,#chatgpt-mermaid-\_r_4gu\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_4gu\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_4gu\_ .mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_4gu\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_4gu\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4gu\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}Customer RequestExtract Pickup andRequirementsSelect Initial Search RadiusSpatial RouterQuery Relevant Redis GEOPartitionsMerge and DeduplicateCandidatesFetch Latest DriverOperational StateFresh and Available?DiscardVehicle and ServiceCompatible?Recalculate GeographicDistanceWithin Radius?Shortlist CandidatesCalculate Road ETARank Eligible DriversEnough Suitable Candidates?Expand RadiusPass Ranked Candidates toDispatchNoYesNoYesNoYesNoYes

The complete responsibility of the discovery service is:

```
Input:
    Pickup location
    Search constraints
    Required number of candidates

Output:
    Ranked, eligible candidate list
```

It should not independently declare a ride permanently assigned to a driver.

That decision belongs to the reliable assignment workflow.

# 5.13 A practical implementation outline

Here is a simplified Java-style structure.

```
public List<DriverCandidate> discoverDrivers(
        RideRequest request) {

    int[] radii = {2, 3, 5, 8};

    int requiredCandidates = 10;

    Set<String> seenDriverIds = new HashSet<>();

    List<DriverCandidate> eligibleCandidates =
            new ArrayList<>();

    for (int radius : radii) {

        // 1. Determine relevant spatial partitions.
        List<PartitionId> partitions =
                spatialRouter.getRelevantPartitions(
                        request.pickup(),
                        radius
                );

        // 2. Query partitions concurrently.
        List<GeoCandidate> geoCandidates =
                geoSearchService.searchParallel(
                        partitions,
                        request.pickup(),
                        radius
                );

        // 3. Remove duplicates.
        List<GeoCandidate> newCandidates =
                geoCandidates.stream()
                        .filter(candidate ->
                                seenDriverIds.add(candidate.driverId()))
                        .toList();

        // 4. Load current operational state.
        List<DriverState> states =
                driverStateService.getBulk(
                        newCandidates.stream()
                                .map(GeoCandidate::driverId)
                                .toList()
                );

        // 5. Apply cheap eligibility checks.
        List<DriverCandidate> eligible =
                eligibilityService.filter(
                        request,
                        newCandidates,
                        states
                );

        eligibleCandidates.addAll(eligible);

        // 6. Stop if sufficient candidates exist.
        if (eligibleCandidates.size() >= requiredCandidates) {
            break;
        }
    }

    // 7. Calculate ETA for a limited shortlist.
    List<DriverCandidate> shortlisted =
            candidateSelector.shortlist(
                    eligibleCandidates,
                    30
            );

    List<DriverCandidate> withEta =
            etaService.calculateBatch(
                    request.pickup(),
                    shortlisted
            );

    // 8. Rank candidates.
    return rankingService.rank(request, withEta);
}
```

This is architectural pseudocode. Production implementations should additionally handle query deadlines, partial partition failures, stale index entries, duplicate driver state, and iterative candidate pagination.

One important improvement: if a partition query fails, the result is incomplete. We must distinguish:

```
Found 0 candidates
```

from:

```
Could not search one or more partitions
```

These mean very different things.

We should not confidently report that no nearby drivers exist when part of the search infrastructure was unavailable.

# 5.14 Chapter 5 — Important mental models

| Concept              | Remember it this way                                                       |
| -------------------- | -------------------------------------------------------------------------- |
| Discovery            | Who is geographically nearby?                                              |
| Eligibility          | Who can actually accept this request?                                      |
| Dispatch             | Whom should we contact?                                                    |
| Top-K                | Don't fetch every candidate unnecessarily                                  |
| Overfetch            | Retrieve extra candidates because some will fail filtering                 |
| Radius expansion     | Search a larger area when local supply is insufficient                     |
| Freshness validation | A stored coordinate isn't necessarily current                              |
| ETA ranking          | Nearest geographically doesn't mean fastest by road                        |
| Candidate caching    | Cache potential candidates, never trust cached availability for assignment |
| Atomic reservation   | Discovery doesn't give a driver to a request                               |

The biggest lesson from this chapter:

Nearby matching is not one query. It's a pipeline that progressively narrows a large geographical population into a small, fresh, eligible, ranked candidate set.

# Next: Chapter 6 — Real-Time Dispatch and Driver Offer Delivery

Now we have a ranked list of suitable drivers.

The next question is:

How do we actually deliver a ride or delivery offer to drivers in real time?

We'll evolve the dispatch mechanism from a simple notification into a reliable distributed workflow.

We'll cover:

1. Sequential dispatch: offer one driver at a time.
2. Parallel dispatch: offer multiple drivers simultaneously.
3. Batch dispatch: offer small groups of drivers.
4. WebSockets vs SSE vs push notifications.
5. How to route messages to drivers connected to different gateway servers.
6. Why persistent WebSocket connections need a connection registry.
7. What happens when a driver is disconnected.
8. Offer timeouts and expiration.
9. Driver acceptance, rejection, and cancellation events.
10. Why a push notification is not proof that a driver received or saw the offer.
11. How to prevent stale offers from being accepted.
12. When a durable queue is needed and when best-effort delivery is appropriate.

We'll also establish an important distinction:

A ride offer is a temporary opportunity, not a confirmed assignment.

That distinction will make the next chapter's reliability and concurrency design much easier to understand.

Say next to continue.
