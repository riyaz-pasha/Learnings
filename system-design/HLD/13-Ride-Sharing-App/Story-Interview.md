# Uber System Design (my notes, interview style)

## Questions I'd ask first

* Are we supporting on demand rides or scheduled rides too?
* Different ride types (Bike, Auto, Cab, different car classes)?
* How frequently drivers location updates are captured, every 5 seconds?
* Is this single city, regional, or global?
* Do we need surge pricing in scope?
* Is payment in scope or just the ride lifecycle and matching?

Let's assume: on demand rides, multiple ride types, location every 5 sec, global but each region works independently, surge pricing and payments in scope.

## Functional Requirements

* A rider can request a ride from point A to B. Should show ride types and fares along with ETA.
* A driver enables themselves whether they're online or offline and sends location updates frequently.
* Ride matching: system finds nearby available drivers and notifies them about the ride, they can accept or reject.

## Non Functional Requirements

* Low latency, match should happen in seconds or as early as possible.
* High availability and scalability, should not go down when too many requests come or during specific events like near a stadium or a concert.
* Consistency and durability, single driver should not be assigned to multiple rides.

## Assumptions

* DAU: Riders 5M, Drivers 1M.
* 2 rides per day, so 10M requests/day, roughly 100 req/sec.
* Location updates roughly 1M drivers every 5 sec, so somewhere around 200K updates/sec sustained.

Let me also quickly do the storage math because it tells us which database to pick later. Location data is tiny, 1M drivers times maybe 100 bytes each, that's just 100 MB, so it comfortably fits in memory. But trip data is different, 10M rides a day times roughly 1 KB per trip record is 10 GB a day, so over a year that's a few TB. That number is what tells me the durable store needs to be something that shards well from day one, not something I bolt on later.

---

## High Level Approach

I'll start simple, find the bottleneck, fix it, and keep going. That's basically how I want to walk through this.

---

## v1 — naive storage

The core and primary functionality of this app is finding nearby drivers and matching them, so let's start there.

Drivers keep sending location, lat and long, continuously. Let's just store it:

```
driver_id | status | lat | long
```

This looks fine on a high level but there are a couple of problems with this approach.

* We can't write efficient queries because the data is 2D, so a normal DB index can't really help us with "nearest" style queries.
* Even if we fetch a rough set of drivers, we'd still have to recompute distance against the rider's location on our side, which is expensive if the set is large.

We can solve this by making the 2D data 1D, create a single geohash using lat and long.

## v2 — geohash and picking the right database

We create geohashes by interleaving the bits of longitude and latitude. Now we store:

```
driver_id | status | geohash
```

So when a driver sends lat and long from the mobile app, the server calculates the geohash and saves it.

This structure looks good, but which database should we pick for storing these continuous location updates?

Storing them in a SQL database would be a problem, because at 100 to 200K writes per second, with each driver updating every 5 seconds:

* write amplification and WAL pressure, writing into the WAL, then to disk, then updating indexes is too much work per write
* locking
* query latency on the select side

So we use Redis instead.

* fast reads and writes since it's in memory
* high throughput
* native geospatial support, GEOADD and GEORADIUS
* and honestly it's okay to lose this data, it changes every few seconds anyway and only matters while the driver is online

So we split it, persistent stuff like driver profile and trip history goes in SQL or NoSQL, and location data goes in Redis.

One thing worth mentioning here, geohash has a boundary problem, two points can be right next to each other but fall into completely different prefixes if they're on opposite sides of a cell edge. This is actually why Uber's real system uses something called H3, which is a hexagonal grid instead of a rectangular one, hexagons don't have this edge problem because every neighboring cell is the same distance away. I wouldn't implement H3 in an interview but it's worth mentioning that I know the limitation and the real fix.

## v3 — scaling Redis, and getting the sharding key right

A single Redis node can't hold this much data or this many queries, reads and writes. So we need multiple Redis nodes, a cluster.

Redis cluster has something called hashslots, 16K plus of them, and each node owns a range. Say we have 4 nodes: 0 to 4K goes to node 1, 4K to 8K to node 2, 8K to 12K to node 3, 12K to 16K to node 4.

Now here's something I want to be careful about. My first instinct might be to just hash the driver_id to pick the slot. But that's actually a trap. If I shard by driver_id, drivers who are all sitting in the same city end up scattered randomly across every node, because their IDs hash to random slots. So when a rider in Hyderabad requests a ride, I'd have to query every single node and merge the results just to find drivers who are all physically nearby each other. That defeats the whole point of sharding.

So instead, I shard by geography. I key the hashslot off the region or city, or off a geohash prefix, not the driver_id. So all drivers currently in Hyderabad map to the same node, all drivers in Bangalore map to a different node. Now when a rider in Hyderabad requests a ride, I only touch the Hyderabad node, no fan out, no merge.

If a driver physically moves from one region to another, their record occasionally needs to migrate nodes, but that's rare compared to how often location updates happen within a region, so it's fine.

If a single region gets too hot, I can split it further using a finer geohash prefix within that region. I'll come back to this exact scenario later, because it's basically what happens near a stadium after a concert.

## v4 — replication, so we don't lose the whole node on a crash

Right now if a node dies, we lose that entire slot range until it recovers. So let's replicate.

Each shard, the primary, gets one or two replica nodes. Redis does this asynchronously, the primary streams its writes to the replicas but doesn't wait for them to acknowledge before responding to the client. And that's on purpose, if I made every location write wait on a replica ack, I'd kill the throughput this whole system exists for.

Because it's async there's a small lag window, maybe a few hundred milliseconds, where if the primary crashes right then, we lose the most recent writes. For this specific data that's completely fine, I'd actually say that out loud in the interview, losing a few hundred milliseconds of a driver's location doesn't matter because the driver's app is going to send another update within the next few seconds anyway. Not every piece of data in this system needs the same durability guarantee, and recognizing which data can be lossy is part of the design.

For failover, Redis Cluster nodes gossip with each other about health, and once a majority of primaries agree a node is down, one of its replicas gets promoted automatically, and the slot mapping updates and propagates out to clients.

And separately from replication, I'd also put a short TTL on every location key, like 15 to 30 seconds. If a driver's app crashes or loses signal, their stale location just expires on its own instead of us routing riders toward a driver who's actually gone dark. That also means I don't need a separate cleanup job for this.

One more thing, I would not replicate this location data across regions. A driver sitting in Mumbai is useless to a Redis cluster in Sao Paulo. Each region runs its own independent location cluster, only the durable trip and account data needs cross region treatment, and I'll get to that.

---

## v5 — ride matching flow

Now let's say the rider has requested a ride with source, destination, and ride type, bike, auto, cab.

1. This request comes into the ride matching service.
2. Matching service converts the rider's lat long into a geohash or region key and fetches nearby available drivers from that region's node.
3. Now here's something important, straight line distance alone isn't good enough for ranking. A driver 1km away across a river could actually take longer to arrive than a driver 2km away with a clear road. So what I'd do is take the closest maybe 10 drivers by straight line distance first, and only for those 10, call the routing API to get an actual drive time ETA, then re-rank by that. I'm keeping it to 10 candidates on purpose so I'm not hammering the routing API with every nearby driver, that API is usually the slowest and most expensive external call in this whole flow.
4. I'd also factor in the driver's acceptance rate and rating into the ranking, not just distance and ETA. A driver who's technically closest but rejects rides constantly isn't actually my best first pick, I want to bias slightly toward drivers more likely to say yes.
5. Then we notify the top ranked driver and wait for accept or reject.
6. If accepted, ride is confirmed, driver assigned, rider notified.
7. If rejected, or if it times out, we move to the next driver in the ranked list. If we exhaust the list, we widen the search radius and repeat.

One alternative worth mentioning, instead of notifying drivers one at a time, some systems dispatch to several top candidates at once and take whichever one accepts first, cancelling the rest. That reduces rider wait time, especially in low supply areas, but it means more wasted notifications and I now need to handle multiple accepts racing each other instead of just one. I'd present the sequential version as my baseline and mention parallel dispatch as an optimization if asked how to reduce latency further.

The main problem still unsolved here, how do we make sure a driver never gets assigned to two rides at once.

## v6 — preventing double booking

My first instinct might be to add a status column on the driver, like RESERVED. But the problem with just a status flag is, if the notification goes out and for some reason the driver's response never makes it back to the server, crash, dropped connection, whatever, that flag can get stuck forever with nothing to revert it.

So here's the concrete fix I'd actually use, a Redis lock with a TTL:

```
SET driver:{id}:lock rideRequestId NX EX 15
```

* NX means only set this if the key doesn't already exist, so it's an atomic compare and set. If two matching workers both try to grab the same driver at the same time, only one of them succeeds.
* EX 15 means it expires in 15 seconds automatically. If the notify and accept round trip doesn't finish in that window, the lock releases itself and the driver goes back into the pool, no cleanup job required, though I'd still run a periodic reconciliation job as a safety net for edge cases like clock skew.
* If the driver explicitly rejects, I delete the lock right away instead of waiting for the TTL, so the next candidate isn't delayed.
* If the driver accepts, that reservation turns into an actual row in the durable trip table, the Redis lock is just the fast temporary reservation, the durable store is the real source of truth once it's confirmed.

This is actually the same pattern as inventory reservation during e-commerce checkout, worth mentioning if asked where else I've seen this.

---

## v7 — fare and ETA

User enters current location and destination, sends the request.

* Request lands on the Fare Estimation and ETA service.
* Instead of building our own maps, we call a third party routing API to get distance, route, and ETA, then calculate the fare from that.
* Client sees the ride type options with fare and ETA and picks one.

One optimization here, I'd cache the routing API response for identical or near identical origin destination pairs over a short window, a few seconds. Popular routes, like airport to downtown, get requested constantly, and this API is usually the slowest and most expensive dependency in the whole flow, so caching it saves both latency and cost.

## v8 — making the request async

Problem, ride matching can take longer than we'd want a synchronous request to sit open for, and if the server restarts or the connection drops we don't want to lose the request.

So, user requests a ride, we publish a ride requested event into a queue, Kafka or SQS. Workers pull events off this queue and do the matching, updating ride status as they go. Meanwhile the rider's app polls with the ride id to check if a driver's been assigned, or later gets pushed the update once we have the real time layer.

This scales well because instead of dropping requests under load, the worker fleet just works through a backlog.

One thing I'd add, the client should generate a request id, a UUID, and send it along. If the client retries because the connection was flaky, the worker sees the same request id and just returns the existing job instead of creating a duplicate ride request.

## v9 — real time connection layer

Instead of maintaining an SSE connection inside the matching service itself, I want to pull that out into its own layer. Both SSE and WebSockets are stateful, so whichever server holds a connection is the only one that can push to that driver, and the matching service shouldn't be the thing holding millions of open sockets, that's a completely different scaling problem than matching logic.

So here's how I'd structure it.

1. A fleet of gateway servers, each holding many live WebSocket connections, drivers and optionally riders for live tracking.
2. A connection registry in Redis, mapping driver_id to which gateway instance they're connected to. Every gateway updates this on connect, disconnect, reconnect.
3. Now the tricky part, cross node delivery. Say the matching service decides to notify driver X, but driver X's socket is sitting on a completely different gateway node than the one handling this request. So the matching service looks up driver X's gateway instance in the registry, and publishes the notification to a pub sub channel scoped to that specific gateway instance, could be Redis pub sub or a Kafka topic per gateway. Only that one gateway node, which is subscribed to its own channel, actually delivers the message down the socket it's holding.
4. Load balancer distributes new connections across gateway nodes, sticky routing isn't strictly required since the registry handles lookup either way, but it reduces registry churn if reconnects land back on roughly the same node.
5. Heartbeats, client pings periodically, and if a gateway misses a heartbeat it evicts that connection from the registry so we're not sending into the void.
6. One more thing worth mentioning, phones background the app and the OS kills the socket to save battery, so a missing WebSocket connection doesn't necessarily mean the driver is offline. Before giving up on a candidate I'd fall back to a push notification, APNs or FCM, to wake the app back up, which reconnects and can pick up the pending offer. This meaningfully improves match success without adding latency on the happy path, since it only kicks in when the socket's already gone.

Why keep this separate from matching, matching is stateless and scales on compute, the gateway layer scales on concurrent open sockets, a completely different axis, so bundling them forces me to over-provision one to satisfy the other.

**One thing worth stopping on, since drivers are now sending location over this same WebSocket, should the gateway write straight to Redis, or publish an event first?**

My first instinct might be to just have the gateway server write directly, driver sends a location update, gateway calls `GEOADD` on whichever Redis node owns that region, done, one hop, simplest possible path. And honestly at v1 to v3, when I first introduced Redis, that's exactly what I'd do, there's no reason to add more infrastructure than the problem needs at that point.

But by the time I've got this gateway layer and a production scale system, I'd go through a queue instead, gateway publishes the location update to Kafka, and a separate consumer is the one that writes into the geo sharded Redis cluster. A few reasons I'd make that call here specifically:

* If the gateway writes directly, it now has to know the sharding topology of the Redis cluster, which node owns which region, and hold connections across the whole cluster. That's coupling the gateway, whose only job is supposed to be holding sockets and forwarding bytes, to the internals of how the location store happens to be sharded. I've already gone out of my way to keep the gateway dumb, this would undo that.
* If a Redis node hiccups, a failover moment, a GC pause, a direct write either blocks the gateway or I have to build retry and buffering logic into the gateway itself. Kafka gives me that buffering for free, it just absorbs the burst and the consumer catches up once Redis is healthy again.
* Location data isn't only useful to matching. ETA model training, surge heatmaps, fraud checks like sanity checking reported speed, all want this same stream. If the gateway writes directly to Redis only, every one of those becomes another direct write I'd have to bolt onto the gateway. With a queue, they're just additional consumer groups on the same topic, no change to the gateway or the hot path at all.
* I'd partition the topic by the same region or geohash key I'm already sharding Redis by, so a consumer instance owns a region end to end and writes straight into that region's node, consistent with the sharding decision instead of fighting it.

The cost is one extra hop, maybe 10 to 50ms through Kafka, and against a 5 second update interval that's nothing, it doesn't meaningfully widen how stale the data matching sees is. So the trade is basically free at this scale, which is why I'd make the call to decouple it, same reasoning as making ride requests async back in v8, same pattern, different data.

---

## Let me walk through one actual request end to end, with a concrete example

I think this is worth having ready because it's usually the follow up once I've drawn the boxes, the interviewer wants to see me trace one request through the whole system with actual state changes, not just names of services.

Setup, before anything happens:

* Rider R has an open WebSocket connection to Gateway Server A
* Driver D has an open WebSocket connection to Gateway Server B
* Redis connection registry already has `R → Server A` and `D → Server B`
* Driver D has been streaming location every 5 seconds this whole time, D's app sends it to Server B, Server B forwards it into Kafka, the location consumer picks it up and writes D's current geohash into the region's Redis node

Now the actual request, T = 0.

**Step 1, rider taps book ride.** R's app sends the request over its existing socket to Server A. Payload is basically pickup, drop, ride type.

**Step 2, Server A forwards it, does nothing else.** This is the part I'd emphasize, the gateway server has no business logic in it at all, its only job is holding the connection and forwarding bytes. So Server A just drops a ride requested event onto a Kafka topic, `ride-requests`, and immediately forgets about it. If Server A crashed the instant after this, it wouldn't matter, the event already made it into Kafka.

**Step 3, matching service consumes the event, roughly T = 10 to 50ms.** A matching worker picks the event off `ride-requests`. It converts R's pickup location into a geohash or region key, and pulls nearby available drivers from that region's Redis node, this is the geo sharded cluster from earlier, so it's a single node lookup, not a scatter gather. Say it gets back 15 candidates. It takes the closest 10 by straight line distance, sends those 10 to the routing API for actual drive time ETA, and re-ranks, folding in acceptance rate and rating like I mentioned before. D comes out on top.

**Step 4, acquire the lock, T = roughly 50ms.** Matching service does `SET driver:D:lock R123 NX EX 15` in Redis. Succeeds, D is now reserved for 15 seconds.

**Step 5, this is the part that's easy to get wrong, routing the notification to D specifically.** The matching service needs to get a message to D, but D's actual open socket lives on Server B, not on whatever machine is running this matching worker. So it does a registry lookup, `D → Server B`, and here's the important detail, it does not publish this to some general topic that every gateway server is subscribed to, because then every single gateway server in the fleet would receive this message and have to check whether one of its own connections happens to be D, that's a broadcast to every node for every single ride request, it does not scale once you have hundreds of gateway servers. Instead it publishes to a channel or partition scoped specifically to Server B, something like a Redis pub sub channel named after that instance, or a Kafka topic partitioned by gateway instance id. Server B is the only one subscribed to its own channel, so it's the only one that receives this.

**Step 6, Server B pushes it down the socket, T = roughly 100ms.** Server B receives the message off its own channel and forwards it down the actual live WebSocket connection to D's phone. D's app shows the new ride request, and I'd start a 10 to 15 second accept timer on the app side that roughly matches the TTL on the backend lock.

**Step 7, driver accepts, T = a couple seconds, human dependent.** D taps accept, sends it back up through the same socket to Server B, Server B forwards it into Kafka, `driver-events` topic, or however I've named it.

**Step 8, ride confirmed.** Matching or ride service consumes that acceptance event, marks the trip as DRIVER_ASSIGNED in the durable store, and this is also the point where I'd cancel any other pending notifications if I'd gone with parallel dispatch instead of sequential.

**Step 9, notify the rider, same cross server problem in reverse.** Now we need to tell R, and R's socket is on Server A, not B. Same pattern, registry lookup `R → Server A`, publish to Server A's specific channel, Server A pushes it down R's socket. R sees driver assigned, driver's on the way.

**Step 10, live tracking, this just repeats the location loop continuously.** D keeps streaming location to Server B every 5 seconds, that gets published to R's channel and pushed to Server A, which forwards to R, so R's app shows the car moving on the map. Same pattern, just running on a loop now instead of once.

A few things I'd call out explicitly if asked:

* Gateway servers are dumb pipes on purpose, all they do is hold connections and forward whatever the registry tells them to forward, this is exactly why I kept them decoupled from matching logic earlier.
* Servers never talk to each other directly, A never calls B directly, everything routes through Kafka or Redis pub sub plus the registry, that's what makes this decoupled instead of every gateway needing to know about every other gateway.
* Rough latency budget I'd expect end to end, excluding however long the human driver takes to actually tap accept, is somewhere under 200ms, maybe 20ms for the request to reach the backend, 50ms for matching including the routing API call, another 100ms or so for the notification to make it back down to the driver's phone. I wouldn't claim these numbers as measured, just as a reasonable order of magnitude to have in my head.

**Failure scenarios I'd walk through if pushed:**

* Driver doesn't respond in time, the Redis lock's TTL expires, driver goes back into the pool, matching service moves to the next candidate. Already covered this.
* Server B crashes mid flow, D's connection drops, D's app detects it and reconnects, lands on a different gateway server, that server updates the registry with D's new location, `D → Server C` say. If a notification was in flight during the crash, it's lost, but since D never received it, the accept timer on the backend TTL just expires normally and matching retries with the next candidate, no special handling needed beyond what's already there.
* A message gets lost between Kafka and a consumer, this is why I'd use Kafka in the first place instead of just firing HTTP calls around, Kafka retains the event and consumers track their own offset, so a consumer that crashes and restarts picks up from where it left off instead of the event just vanishing.

**One more scaling point worth mentioning**, matching isn't recomputing everything from scratch on every single request, the geo index in Redis is already continuously maintained by the location update stream, so a match lookup is just a fast read against an index that's already warm, not some expensive on demand computation.

If I wanted to summarize this whole thing in two sentences for an interviewer, I'd say, riders and drivers each hold a persistent connection to one of many gateway servers, and a registry tracks who's on which server. Every cross server hop, rider to driver or driver to rider, goes through Kafka or Redis pub sub plus that registry lookup, never gateway to gateway directly, which is what lets this scale to millions of concurrent connections without every server needing to know about every other server.

---

## v10 — the rest of it

### Trip lifecycle

I'd model this as an explicit state machine, REQUESTED, DRIVER_ASSIGNED, DRIVER_ARRIVING, IN_PROGRESS, COMPLETED, with CANCELLED reachable from most of these states. This matters because a lot of the edge cases, cancellation fees, no shows, refunds, all hang off which state the trip was in when something went wrong.

### Durable data

Trips, users, drivers, payments live outside Redis, in a store that's sharded by region or by a hash of user_id, since almost every query is scoped to one rider or driver anyway.

For the actual technology, I'd consider a distributed SQL store like CockroachDB or Spanner, mainly because payments need real transactional guarantees and these give me that without hand rolling distributed transactions myself. Cassandra is a reasonable alternative for the high write trip data specifically, but then I'm pushing transactional logic for payments up into the application layer myself, more work, but might be worth it if trip write throughput turns out to be the actual bottleneck rather than payment consistency. Either way, I'd keep this completely separate from the Redis location layer, that layer intentionally trades durability for throughput, this layer intentionally does the opposite, and picking one database to do both jobs is usually a sign something's off.

### Payments, cancellations, surge, ratings

* Payment happens after the trip completes, asynchronously, the rider already got the service so this shouldn't block completion. I'd use an idempotency key tied to the trip id on the charge request so a retry never double charges.
* Cancellations, rider cancelling early might trigger a fee after some grace period or once the driver's already traveled toward pickup. Driver no shows auto cancel and bump the rider to the front of the next match attempt.
* Surge, I'd compute a demand to supply ratio per region or geohash cell over a rolling window and apply a multiplier before the rider confirms. This can be eventually consistent, it doesn't need to be split second exact right at a cell boundary.
* Ratings get written after trip completion, and I'd compute the aggregate rating asynchronously since it's not on the critical path of anything user facing.

### Failure handling

* Driver doesn't respond in time, already handled, the TTL lock releases and we move to the next candidate.
* Payment fails after trip completion, trip still stays marked complete, we retry the payment with backoff separately.
* A gateway node crashes, its registry entries go stale, heartbeat timeout evicts them, and the affected apps just reconnect and land on a healthy node.
* Matching service partially goes down, since requests are already sitting in a queue, whatever's left of the worker fleet just works through the backlog once capacity comes back.

### Hot regions, this is basically the stadium example from the requirements

A single geohash cell near a stadium can spike way past what a typical cell handles, that's a hot shard. Two ways to handle it, either I pre-identify known hotspots from historical demand and use a finer grained cell resolution just there, or I salt the key dynamically, split it into suffixed keys like cell:0, cell:1, and fan reads out across all the suffixes when querying. On the matching side, I'd also widen the search radius progressively instead of using one fixed radius, since a hot zone usually has both extra supply and extra demand, so a slightly wider search still resolves fast.

### Cross region

Active-active per region, not one global cluster, a rider in Delhi's request should never physically cross an ocean, latency alone rules that out. Each region runs its own full stack, gateway, matching, location cluster. Only account and trip history data needs to replicate cross region, and even that can be eventually consistent, partitioned by home region rather than fully synced everywhere.

For rolling out a new region, stand up the stack, seed reference data like fare rules and supported ride types, dark launch it so it's accepting traffic but not shown to real users yet, validate, then flip routing over.

For zero downtime deploys, stateless services like matching and fare estimation are easy, rolling deploy behind the load balancer. The gateway layer is the tricky one because it's holding live connections, so I'd drain connections instead of hard killing a node, stop routing new connections to it and let existing ones finish or gracefully reconnect elsewhere.

---

## Things I'd have ready even if not asked

* Metrics I'd watch, match latency p50/p95/p99 since that's literally the core NFR, driver notification success rate, WebSocket connection churn, and queue backlog depth as an early warning that the matching workers are falling behind.
* Fraud, drivers spoofing GPS to look closer, or riders and drivers colluding on fake trips for referral bonuses. A basic sanity check, does the reported movement match a plausible vehicle speed, is enough of an answer if this comes up, I wouldn't try to design a full fraud system live.
* APIs I'd sketch if asked:
  * POST /rides/estimate, source, destination, ride type, returns fare options and ETA
  * POST /rides/request, confirms and enqueues the request, idempotent via the request id
  * GET /rides/{id}/status, polling fallback until the WebSocket push confirms
  * a WebSocket frame or POST /drivers/location for the location stream
  * POST /drivers/{id}/response for accept or reject

## How I'd pace this in a 45 min interview

1. Requirements, clarifying questions, capacity numbers, 5 min
2. High level architecture, all the boxes, hand wavy, 5 min
3. Deep dive on driver location and matching, this is the core of the problem, 15 min
4. Deep dive on the real time gateway, 10 min
5. Trade offs, hot regions, cross region, 10 min
6. Leave 5 min buffer for wherever the interviewer wants to push, they usually have one specific area they care about, and I'd rather follow that than stick rigidly to my own outline.

If I only had time to really nail down one thing before an interview, it'd be the double booking lock, that's the single most common follow up question across basically every matching or booking system design.

---
---
---
Good question — this is exactly the kind of thing that gets asked as a follow-up once you've drawn the gateway box. Let me walk through both options the way I'd reason about it live.

**Option A: WS server writes straight to Redis on every location update.**

This works, technically. Redis's `GEOADD` is fast, and with the cluster sharded by region (from earlier), a single node can comfortably absorb its share of ~200K updates/sec. It's also the simplest possible path, one hop, gateway receives the update and writes it, done.

But there's a real cost to going direct:

* Every gateway server now needs to know which Redis node owns which region and maintain client connections across the whole cluster. That's coupling the gateway layer, whose only job is supposed to be holding sockets, to the internals of how the location store is sharded.
* If a Redis node has a brief hiccup, a GC pause, a failover moment, the write either blocks the gateway server or the gateway has to build its own retry/buffering logic. That's exactly the kind of resilience work a message queue gives you for free.
* Location data has more than one consumer in a real system. Matching needs it fresh in Redis, sure, but you'd also want it for ETA model training, surge/demand heatmaps, fraud detection like GPS-speed sanity checks. If the gateway writes directly to Redis only, every one of those becomes another direct write the gateway has to make, which means the gateway now fans out to N different systems instead of doing one thing.

**Option B: WS server publishes an event, a consumer writes to Redis.**

Gateway's only job stays exactly what it should be, forward whatever comes in. It publishes the location update to a topic, and a separate consumer group is the one responsible for writing into the geo-sharded Redis cluster.

* Kafka absorbs the burst and buffers through any brief Redis slowness, the gateway never blocks on Redis at all.
* Any other system that wants this stream, analytics, fraud detection, ETA training, just adds another consumer group on the same topic, without touching the gateway or adding load on the hot ingestion path.
* I'd partition the topic by the same region/geohash key I'm sharding Redis by, so a given consumer instance owns a region end to end and writes straight to that region's Redis node, no cross-node routing, consistent with the sharding decision from earlier instead of fighting it.

**The one real cost of going through a queue is latency, and here it's basically free.** An extra 10 to 50ms hop through Kafka is nothing against a 5 second update interval, it doesn't meaningfully widen the staleness window for matching.

**What I'd actually say in an interview:** at v1 to v3, when I've just introduced Redis, I'd let the gateway write directly, it's simpler and there's no reason to add infrastructure I don't need yet. But once I get to this point, real production scale with multiple downstream consumers and a real resilience requirement, I'd introduce the queue specifically because the decoupling and fan-out matter more than saving one hop of latency that doesn't affect anything at 5 second granularity. That's basically the same reasoning I used for making ride requests async back in v8, same pattern, different data.

Want me to fold this into the doc, probably right where v3 hands off to v9, since it's the missing link between "location goes into Redis" and "gateway servers exist"?

---
---
---

# Uber System Design — Complete Notes

## 1. Clarifying Questions

- Are we supporting on-demand rides only, or scheduled rides too?
- Different ride types (Bike, Auto, Cab — different car classes)?
- Do we need to support **shared/pooled rides** (multiple riders, one driver)?
- How frequently are driver location updates captured — every 3–5 seconds?
- Single-city, regional, or global scale?
- Do we need **surge/dynamic pricing** in scope?
- What's the **consistency requirement on driver assignment** — is it acceptable for two riders to briefly see the same driver as "available," or must assignment be exactly-once?
- Is **payment processing** in scope, or just trip lifecycle + matching?
- Build our own routing/maps, or integrate a third-party API (Google Maps/Mapbox)?

For this design: on-demand rides, multiple ride types, 5-second location updates, global scale with regional isolation, surge pricing in scope, exactly-once driver assignment required, payments in scope, third-party routing API used.

---

## 2. Functional Requirements

- A rider can request a ride from point A to B, and see ride types, fares, and ETA before confirming.
- A driver toggles online/offline and sends location updates frequently while online.
- **Ride matching:** the system finds nearby available drivers, notifies them, and they can accept or reject.
- A confirmed trip goes through a full lifecycle from assignment to completion, including cancellation handling.
- Post-trip: fare is charged, and both parties can rate each other.

## 3. Non-Functional Requirements

- **Low latency** — matching should happen in seconds or as early as possible.
- **High availability & scalability** — must not degrade under load spikes (e.g., a concert or stadium event ending simultaneously).
- **Consistency & durability for assignment** — a single driver must never be assigned to two rides at once.
- **Fairness** — a driver shouldn't be perpetually skipped by the matching algorithm; a rider in a low-supply area should see visibility into what's happening ("searching, expanding radius") rather than silence.

---

## 4. Capacity Estimation

**Traffic:**
- DAU: 5M riders, 1M drivers.
- ~2 rides/rider/day → 10M ride requests/day → 10×10⁶ / ~10⁵ sec/day ≈ **~100 requests/sec** average (design for several multiples of this at peak).
- Location updates: ~1M drivers reporting every ~5 sec → **~200K updates/sec sustained** (peaks toward 1M/sec in worst case bursts).

**Storage:**
- **Location data (Redis, ephemeral):** 1M drivers × ~100 bytes (driver_id, geohash, status, timestamp) ≈ **~100 MB resident at any time** — confirms this comfortably fits in-memory across a modest cluster, and confirms this data does *not* need durability.
- **Trip data (durable):** 10M rides/day × ~1 KB/record (rider, driver, route, fare, timestamps) ≈ **10 GB/day**, ≈ **~3.6 TB/year** uncompressed — this is what tells you the durable store needs to be a horizontally sharded store from day one, not a single instance you'll "scale later."
- **Bandwidth:** ~200K location updates/sec × ~50 bytes ≈ **~10 MB/sec sustained ingest** (bursting several times higher) — this number is what justifies routing location updates through Redis rather than a normal OLTP database, and why the ingestion path needs to scale independently of the matching service.

---

## 5. High-Level Approach

Start simple, identify the bottleneck, evolve. This mirrors how the design should actually be presented in an interview: naive approach → bottleneck → fix → repeat.

---

## 6. Driver Location: From Naive to Production-Grade

### v1 — Naive storage

The core, primary functionality of this app is finding nearby drivers and matching them — start there. Drivers send `(lat, long)` continuously; store as:

```
driver_id | status | lat | long
```

Two problems:
- Location is 2D data — a normal B-tree index can't efficiently answer "nearest neighbor" queries on two independent columns.
- To find actual nearby drivers you'd need to fetch a broad set and recompute distance against the rider's location client-side — expensive and slow at scale.

**Fix:** collapse 2D into 1D using a **geohash** — interleave the bits of latitude and longitude into a single string, where a shared prefix implies spatial proximity.

### v2 — Geohash + right database choice

```
driver_id | status | geohash
```

When a driver's app sends `(lat, long)`, the server computes the geohash and stores it.

**Which database?** Not SQL, at this write volume (~100–200K writes/sec, every driver re-writing every ~5 sec):
- Write amplification and WAL pressure — writing to a write-ahead log, then to disk, then updating indexes, is too much overhead per write at this rate.
- Row/page locking contention.
- Query latency for geospatial `SELECT`s under this load.

**Use Redis instead:**
- In-memory → fast reads/writes, high throughput.
- Native geospatial support (`GEOADD`, `GEOSEARCH`).
- It's acceptable to lose this data occasionally — it changes every few seconds and is only meaningful while the driver is online, so it doesn't need durability guarantees.

Split the model: **durable data** (driver profile, vehicle info, trip history) lives in a persistent SQL/NoSQL store; **location data** lives in Redis.

**Note on geohash's limitation:** geohash cells are rectangular, so two points that are geographically close but straddle a cell boundary can end up with very different prefixes, under-returning nearby drivers near edges. Uber's actual production system uses **H3** — a hexagonal hierarchical spatial index — instead of geohash, specifically because hexagons have uniform adjacency (6 equidistant neighbors), which avoids this boundary problem. You don't need to implement H3 in an interview, but naming this limitation and the real-world fix signals depth.

### v3 — Scaling the location store: Redis Cluster

A single Redis node can't hold this volume of reads/writes at global scale. Use **Redis Cluster** — but the sharding *key* matters more than the mechanism, and this is a common place to get the design subtly wrong.

**The trap: sharding by `driver_id`.** Redis Cluster partitions data across **16,384 hash slots**, and it's tempting to hash `driver_id` to pick a slot the way you would for any other entity. Don't — if you do, drivers who are all physically in the same city end up scattered across whichever random nodes their IDs happen to hash to. A "find nearby drivers" query then has to fan out to *every* node and merge results, which defeats the entire point of sharding: you've traded one slow node for N slightly-less-slow nodes queried in parallel.

**The fix: shard geographically.** Key the hash slot off the driver's **region/city ID**, or off a coarse **geohash/H3 prefix**, not `driver_id`. Concretely: all drivers currently in Hyderabad map to the same node (or small node set); all drivers in Bangalore map to a different one. A rider in Hyderabad triggers a query against exactly one region's node — no fan-out, no merge step.

- With this keying, a driver moving between regions occasionally needs their record migrated to a different node — acceptable, since this happens rarely relative to normal location updates within a region.
- To scale a single hot region further, split it across multiple nodes by a finer geohash/H3 prefix within that region (see the hot-region handling in §11) rather than reintroducing `driver_id`-based scatter.
- To scale the cluster overall, hash slots are redistributed across a larger node set, migrating only the affected slot ranges — data movement is kept minimal by design.

### v4 — Replication (making location loss acceptable, not just "okay")

Sharding alone means a node failure makes that shard's entire slot range unavailable until it recovers. Production systems replicate:

- Each shard (primary) gets **1–2 replica nodes**. Redis uses **asynchronous replication**: the primary streams its write stream to replicas without waiting for replica ACKs before responding to the client — waiting on every location write would tank the throughput this system exists to provide.
- This introduces a small **replication lag window** (typically milliseconds) where a primary crash can lose the most recent few writes. For this specific data that's an acceptable trade-off worth stating explicitly: **losing a few hundred milliseconds of a driver's location is fine, because the next update — arriving within seconds — supersedes it anyway.** Not all data in the system needs the same durability guarantee, and recognizing that is part of the design.
- **Failover:** Redis Cluster nodes gossip health over a cluster bus; once a majority of primaries agree a node is down, one of its replicas is auto-promoted, and the updated slot-to-node mapping propagates to clients via `MOVED`/`ASK` redirects.
- **TTL as a second safety net, independent of replication:** every driver location key carries a short TTL (e.g., 15–30 sec). If a driver's app crashes or loses connectivity, their stale location expires on its own rather than routing riders to a driver who's gone dark — this also naturally cleans up state without a separate reaper job.
- **Cross-region:** location data should generally *not* replicate across regions — a driver in Mumbai is irrelevant to a Redis cluster in São Paulo. Each region runs its own independent location cluster; only durable account/trip data gets cross-region treatment (see §11).

---

## 7. Ride Matching

### v5 — Basic matching flow

1. Rider requests a ride with source, destination, and ride type.
2. Request hits the ride-matching service.
3. Service converts `(lat, long)` to geohash/H3 cell and fetches nearby available drivers from that region's node (§6, v3).
4. **Straight-line distance is a weak initial filter, not the final ranking.** A driver 1km away across a river or a highway divider can genuinely be farther in drive-time than one 2km away with a clear route. So: take the ~10 closest candidates by straight-line distance, then send just those 10 to the routing API (§8) to get actual drive-time ETAs, and re-rank by that. Doing this for only the top 10 (not every candidate) keeps routing-API cost and latency bounded.
5. Beyond raw ETA, a real ranking also weighs **driver acceptance rate and rating** — a driver who is technically closest but frequently rejects rides or has a poor rating isn't actually the best first choice; deprioritizing them (without excluding them) improves overall match success rate rather than optimizing distance in isolation.
6. The top-ranked driver is notified and given a window to accept/reject.
7. On acceptance: ride confirmed, driver assigned, rider notified.
8. On rejection (or timeout): try the next driver in the ranked list; if the list is exhausted, widen the search radius and repeat.

**Alternative: parallel dispatch.** Instead of notifying drivers one at a time, some designs send the offer to several top-ranked candidates simultaneously and take whichever accepts first (cancelling the offer to the rest). This lowers rider wait time and improves match success in low-supply areas, at the cost of more wasted notifications and a slightly more involved "first accept wins, all others invalidated" flow — the locking mechanism in the next section still applies, it just needs to handle multiple simultaneous accept attempts instead of one. Worth mentioning as a trade-off if asked "how would you reduce match latency further," but sequential dispatch is the simpler baseline to present first.

The core unresolved problem: **how do we prevent a single driver from being assigned to two rides at once (double booking)?**

### v6 — Solving double booking with a concrete locking mechanism

A naive "status flag" approach has a real gap: if a ride request is sent and the driver's response never reaches the server (crash, dropped connection), the flag can get stuck in a pending state forever unless something reverts it.

**Concrete mechanism — atomic claim with auto-expiry, in Redis:**

```
SET driver:{id}:lock rideRequestId NX EX 15
```

- `NX` — only sets the key if it doesn't already exist. This is an atomic compare-and-set, so two matching-service workers racing to claim the same driver can't both succeed.
- `EX 15` — the lock auto-expires in 15 seconds. If the notify → accept round trip doesn't complete in that window, the lock releases itself and the driver becomes matchable again automatically — no separate batch/cleanup job required, though a periodic reconciliation job is a reasonable belt-and-suspenders check against clock skew or missed edge cases.
- On explicit **rejection**, actively `DEL` the lock immediately rather than waiting out the TTL, so the next candidate isn't delayed.
- On **acceptance**, the reservation is converted into a durable trip record (§9) — the Redis lock is the fast, ephemeral "reservation," and the durable store is the source of truth once confirmed.

This is the same pattern used for inventory reservation in e-commerce checkouts — worth mentioning if asked where else this pattern applies.

**Notifying the driver:** handled by the real-time gateway layer (§8) rather than by the matching service directly — matching should stay focused on matching logic, not persistent-connection management.

---

## 8. Fare, ETA, and Real-Time Notification

### v7 — Fare estimation & ETA

- Rider enters current location and destination in the app and requests an estimate.
- Request hits the Fare Estimation & ETA service.
- Rather than building routing/maps in-house, call a third-party routing API (Google Maps/Mapbox) for distance, route, and ETA, then compute fare from that.
- Client sees ride type options with fare + ETA, and chooses one before confirming.
- **Optimization:** cache third-party routing responses for identical/near-identical origin-destination pairs over short windows — popular routes (e.g., airport → downtown) get requested repeatedly, and the routing API is typically the most expensive and highest-latency external dependency in this whole flow.

### v8 — Making the request flow async

Problem: ride matching can take longer than a synchronous HTTP request should hold open, and a server restart or network blip shouldn't lose the request.

**Fix:** make it event-driven.
- The rider's request is published as an event to a queue (Kafka or SQS).
- Worker processes pull events off the queue and perform matching, updating ride status as they go.
- The rider's app polls (or, once §9's gateway exists, gets pushed) status updates by job/ride ID.
- This absorbs load spikes gracefully — a worker fleet scales to work through backlog rather than requests failing outright.
- **Idempotency:** the client should generate a `request_id` (UUID) with the request; if the client retries due to a flaky connection, the worker recognizes the duplicate `request_id` and returns the existing job rather than creating a second ride request.

### v9 — Real-time connection layer (gateway service)

Maintaining a raw SSE connection per rider/driver from within the matching service couples persistent-connection management to core business logic — bad for independent scaling and bad for resilience (a matching-service redeploy shouldn't drop every open connection). Instead:

**Dedicated Gateway/Connection Management layer**, decoupled from ride-matching:

1. **Gateway servers** — a horizontally scaled fleet, each holding many live WebSocket connections (drivers, and optionally riders for live tracking). Stateful by nature: a given driver's live connection lives on exactly one gateway instance at a time.
2. **Connection registry** — a fast lookup table (Redis) mapping `driver_id → gateway_instance_id`. Every gateway server updates this on connect/disconnect/reconnect.
3. **Cross-node message delivery.** The core problem: the matching service decides "notify driver X," but driver X's live socket may be on a *different* gateway node than the one handling this request. Solution: publish to a **pub/sub channel keyed by gateway instance** (Redis Pub/Sub, or a Kafka topic per gateway) — the matching service looks up `gateway_instance_id` in the registry and publishes there; only that one gateway node, subscribed to its own channel, delivers the message down the live socket it's holding.
4. **Load balancer with sticky-ish routing** — new connections distribute across gateway nodes (round robin / least-connections); reconnects don't strictly need to land on the same node (the registry lookup handles routing either way), but doing so reduces registry churn.
5. **Heartbeats** — clients ping periodically; a gateway evicts a connection from the registry if a heartbeat is missed, so stale entries don't cause "successful" sends into the void.
6. **Push notification fallback** — mobile OSes routinely kill backgrounded sockets to save battery, so a driver's WebSocket connection being absent from the registry doesn't necessarily mean they're offline. Before giving up on a candidate, fall back to APNs (iOS) or FCM (Android) to wake the app, which then re-establishes its WebSocket connection and can pick up the pending offer. This materially improves match success rate without adding real latency, since the fallback only fires when the primary channel is already unavailable.

**Why decouple this from matching (worth stating explicitly):** ride-matching is stateless and scales along compute; the gateway layer scales along a completely different axis — concurrent open sockets. Coupling them forces over-provisioning one to satisfy the other.

---

## 9. Trip Lifecycle & Data Model

A trip moves through an explicit state machine:

```
REQUESTED → DRIVER_ASSIGNED → DRIVER_ARRIVING → IN_PROGRESS → COMPLETED
                                                              ↘ CANCELLED (reachable from most states)
```

**Durable data model (SQL or sharded NoSQL, not Redis):**

- `users`: user_id, name, contact info, payment methods, rating.
- `drivers`: driver_id, vehicle info, license/verification status, rating.
- `trips`: trip_id, rider_id, driver_id, origin, destination, ride_type, status, fare, timestamps for each state transition.
- `payments`: payment_id, trip_id, amount, status, idempotency_key.

**Sharding:** shard the durable store by `region_id` or a hash of `user_id`, since almost every query is scoped to a specific rider or driver — a natural, low-fanout shard key. Use read replicas for analytics/reporting so those queries don't compete with the transactional path.

**Technology choice, worth naming explicitly if asked "which database":**
- A **distributed SQL store with cross-region ACID support** (CockroachDB, Google Spanner) is a strong fit specifically because payments need real transactional guarantees, and these systems give you that without hand-rolling distributed transaction logic yourself.
- A **wide-column NoSQL store** (Cassandra) is a reasonable alternative for the high-write trip/event data specifically, if you're willing to push transactional guarantees (e.g., for payments) up into the application layer — more engineering effort, but can pay off if write throughput on trip records is the dominant bottleneck rather than payment consistency.
- Either way: keep this decision separate from the Redis location layer — that data intentionally sacrifices durability for throughput (§6, v4), while this layer intentionally does the opposite. Conflating the two, or picking one database for both, is a common design smell to avoid.

---

## 10. Payments, Cancellations, Surge Pricing, Ratings

- **Payments:** processed asynchronously after trip completion — the rider already received the service, so this shouldn't block trip completion. Use an idempotency key (keyed by `trip_id`) on the charge request to the payment gateway (e.g., Stripe) so a retry never double-charges. Failed payments are retried with backoff; a rider with unpaid balance above a threshold is blocked from requesting new rides.
- **Cancellations:** rider cancellation before pickup may incur a fee after a grace period, or if the driver has already traveled a meaningful distance/time toward pickup. Driver no-shows auto-cancel and give the rider priority in the next match attempt. Rate-limit cancellations per user to deter gaming (e.g., cancel-farming for some incentive).
- **Surge pricing:** compute a demand/supply ratio per geohash/H3 cell over a rolling time window, and apply a fare multiplier shown to the rider before confirmation. This computation can tolerate **eventual consistency** — surge doesn't need to be split-second-exact across a cell boundary.
- **Ratings:** written after trip completion into the durable store; aggregate rating computation can run asynchronously/batched since it isn't on the critical path of any user-facing action.

---

## 11. Failure Handling, Sharding/Replication Recap, Hot Regions, Cross-Region

### Failure handling

- **Driver notification timeout** — handled by the TTL lock in §6; falls through to the next candidate automatically.
- **Payment failure post-trip** — trip stays marked complete; payment retried asynchronously.
- **Gateway node crash** — registry entries for its connections go stale; heartbeat timeout evicts them; affected apps auto-reconnect and rebalance to a healthy node.
- **Partial matching-service outage** — since requests are queued (§8), a healthy remainder of the worker fleet absorbs backlog once capacity returns, rather than requests being dropped.

### Sharding & replication, tied together

- Location data: Redis Cluster, sharded by geohash/H3 prefix, replicated per-shard (§6, v3/v4).
- Durable data: sharded by region_id/user_id hash, with read replicas offloading reporting queries.

### Hot regions (e.g., a stadium or concert letting out)

- A single geohash/H3 cell can spike far beyond typical load, creating a **hot shard** in Redis Cluster.
- Fix: use **finer-grained cell resolution in pre-identified hotspots** (from historical demand data), or dynamically **salt the hot key** — split traffic across suffixed keys (`cell:0`, `cell:1`, …) and fan reads out across all suffixes on query, a standard hot-key mitigation pattern.
- On the matching side, **widen the search radius progressively** rather than using a fixed radius — a hot zone has both elevated supply and demand, so a slightly wider search still resolves quickly.

### Cross-region deployment

- **Active-active per region**, not one global cluster — a rider in Delhi should never have their location data or matching request cross an ocean; latency alone rules out global centralization for the hot path.
- Each region runs its own full stack (gateway, matching, location cluster). Only account/trip-history data needs cross-region replication, and even that can typically be eventually consistent and geo-partitioned by home region rather than fully synchronized everywhere.
- **New region rollout:** stand up the regional stack, seed reference data (fare rules, supported ride types), dark-launch (accept traffic without surfacing to users) to validate, then flip DNS/geo-routing.
- **Zero-downtime deploys:** rolling deploys behind a load balancer are straightforward for stateless services (matching, fare/ETA). The **gateway layer is the tricky one**, since it holds live connections — use connection draining (stop routing new connections to a node, let existing ones finish or gracefully signal clients to reconnect) rather than hard kills.

---

## 12. Observability & Security (brief, but worth having ready)

- **Metrics:** match latency (p50/p95/p99 — this is the core NFR, so be ready to explain how you'd verify it in production, not just assert it), driver notification success rate, WebSocket connection churn, queue backlog depth as an early warning of matching-service capacity issues.
- **Fraud/GPS spoofing:** drivers spoofing location to appear closer, or riders/drivers colluding on fake trips for referral bonuses. A basic sanity check — does reported movement match a plausible vehicle speed? — is a reasonable one-liner if asked; a full solution is out of scope for most interviews but naming the concern shows awareness.

---

## 13. API Sketch

- `POST /rides/estimate` — body: origin, destination, ride_type → returns fare options + ETA (§8, v7 flow).
- `POST /rides/request` — confirms and enqueues the actual request (§8, v8's async entry point). Idempotent via client-supplied `request_id`.
- `GET /rides/{id}/status` — polling fallback, used until WebSocket push (§8, v9) confirms.
- WebSocket frame / `POST /drivers/location` — high-frequency location stream (§6).
- `POST /drivers/{id}/response` — accept/reject a ride offer (§7, v6).

---

## 14. Suggested Interview Pacing (45 min)

1. Requirements + clarifying questions + capacity estimate — **5 min**
2. High-level architecture sketch, all major boxes, hand-wavy — **5 min**
3. Deep dive #1 — driver location + matching (§6–§7) — **~15 min**, this is the heart of the problem and where most signal comes from
4. Deep dive #2 — real-time gateway (§8, v9) — **~10 min**
5. Trade-offs & scaling — consistency choices, hot regions, cross-region (§11) — **~10 min**
6. Buffer — **5 min**, for wherever the interviewer wants to push deeper; they usually have one area in mind, and flexibility there reads better than rigid time-boxing.

If there's only time to deepen one thing before an actual interview: the **matching/double-booking concurrency mechanism** (§6, v6) — it's the single most common follow-up ("what if two requests race?") across ride-share/booking-system design interviews.
