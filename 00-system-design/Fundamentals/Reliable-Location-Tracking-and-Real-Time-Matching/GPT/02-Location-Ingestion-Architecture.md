# Chapter 2 — Location Ingestion Architecture

In Chapter 1, we established that the primary requirement for live tracking is:

Continuously receive location updates, maintain the latest valid location, and make it available for matching with minimum latency.

Now let's build the ingestion architecture from scratch.

We'll start with one driver and progressively scale it to hundreds of thousands or millions of drivers.

# 2.1 Start with the simplest possible system

Imagine one driver opens our application.

Every 3 seconds, the mobile application obtains the driver's GPS coordinates.

```
Driver Mobile App
       |
       | Location update every 3 seconds
       |
       v
Backend Server
       |
       v
Redis
```

The application sends:

```
{
  "driverId": "D101",
  "latitude": 17.3850,
  "longitude": 78.4867,
  "accuracy": 8.5,
  "speed": 12.4,
  "heading": 180,
  "timestamp": 1794378600,
  "sequenceNumber": 101
}
```

The backend receives this information and updates the driver's latest location.

Three seconds later, another update arrives.

We overwrite the previous location.

That's it.

But this simple architecture raises several questions:

1. Which communication protocol should the mobile application use?
2. Should the connection remain open?
3. Should every update create a new HTTP request?
4. What happens if the mobile network disconnects?
5. What if updates arrive out of order?
6. How do we distribute incoming requests across multiple servers?
7. Should we introduce Kafka or another queue?
8. When should we acknowledge the update?

Let's solve these one by one.

# 2.2 Choosing the communication protocol

There are several possibilities.

| Technology         | Communication                             | Typical use                            |
| ------------------ | ----------------------------------------- | -------------------------------------- |
| HTTP               | Request/response                          | Periodic location updates              |
| WebSocket          | Bidirectional, persistent                 | Real-time updates and commands         |
| gRPC streaming     | Bidirectional or unidirectional streaming | Efficient service/client communication |
| MQTT               | Publish/subscribe                         | Lightweight device messaging           |
| SSE                | Server to client                          | Server-to-client event streaming       |
| Push notifications | Server to device via platform provider    | Notifications and background alerts    |

The important thing is that these technologies solve slightly different problems.

Let's understand them.

## Option A: HTTP polling

The mobile app sends an HTTP request every 3 seconds.

```
Mobile App
    |
    | POST /drivers/location
    |
    v
Backend
    |
    | 200 OK
    |
    v
Mobile App

    Wait 3 seconds

    POST /drivers/location
    |
    v
Backend
```

For example:

```
POST /drivers/location
Authorization: Bearer <token>
Content-Type: application/json
```

```
{
  "latitude": 17.3850,
  "longitude": 78.4867,
  "sequenceNumber": 101
}
```

The backend processes the update and returns:

```
HTTP/1.1 200 OK
```

### Is HTTP inefficient because it creates a new connection every time?

Not necessarily.

Modern HTTP clients reuse connections through keep-alive.

HTTP/2 can multiplex multiple requests over one connection. HTTP/3 also provides efficient transport over QUIC.

So don't assume that every location update requires a new TCP connection and a fresh TLS handshake.

### Advantages

- Simple to implement.
- Easy to load balance.
- No need to maintain long-lived application connections.
- Each request can be handled independently.
- Easy to retry after network failures.
- Easy to authenticate and rate-limit.

### Disadvantages

- Repeated request/response overhead.
- Not naturally bidirectional.
- Less convenient when we also want to send real-time commands to the driver.
- Can have additional latency compared with an already-established bidirectional connection.

Verdict: HTTP is perfectly reasonable for periodic location ingestion.

We don't need WebSockets merely because the system is real-time.

## Option B: WebSockets

Now imagine that the driver application establishes a persistent WebSocket connection.

```
Driver App
    |
    | WebSocket connection
    |
    v
Backend WebSocket Gateway
```

Once connected, the driver can send multiple location updates over the same connection.

```
Driver App                 Backend
    |                         |
    |------ Connect --------->|
    |<----- Connected --------|
    |                         |
    |------ Location 1 ------>|
    |------ Location 2 ------>|
    |------ Location 3 ------>|
    |                         |
    |<----- Ride Offer -------|
    |                         |
    |------ Accept ---------->|
    |                         |
```

Notice something important.

WebSockets are bidirectional.

The backend can also send:

- Ride offers.
- Delivery assignments.
- Cancellation events.
- Navigation updates.
- Driver status requests.
- Other real-time commands.

### Why might WebSockets be better for our system?

Consider a ride request.

We find five nearby drivers.

We want to send offers immediately.

If we already maintain WebSocket connections, the dispatch service can route messages to their connected gateway servers.

We don't have to wait for the drivers to poll the backend.

### But WebSockets introduce a new problem

Persistent connections consume server resources.

Suppose we have:

```
500,000 simultaneously connected drivers
```

We need servers capable of maintaining those connections.

We also need to manage:

- Connection ownership.
- Disconnect detection.
- Reconnection.
- Server restarts.
- Heartbeats.
- Message routing across servers.

We'll revisit connection management in the real-time dispatch chapter.

### Important: WebSockets don't automatically solve location storage

Consider:

```
Driver App
    |
    | WebSocket
    |
    v
WebSocket Gateway
    |
    v
Redis
```

WebSocket is just the transport mechanism.

Redis is still responsible for the live location index.

They solve different problems.

## Option C: gRPC streaming

gRPC supports persistent streaming communication.

For example, the client can continuously send location updates through a bidirectional stream.

Conceptually:

```
Driver App
    |
    | gRPC Stream
    |
    v
Location Ingestion Server

    Location 1
    Location 2
    Location 3
    Location 4
```

It offers:

- Protocol Buffers for compact messages.
- HTTP/2-based transport.
- Strongly defined service contracts.
- Streaming support.
- Efficient communication.

It's particularly attractive for controlled environments and service-to-service communication.

However, mobile network changes, connection lifecycle management, and application-platform constraints still need to be handled.

It doesn't eliminate the need for reconnection or failure handling.

## Option D: MQTT

MQTT is a lightweight publish/subscribe protocol.

A driver publishes location updates:

```
Topic: drivers/D101/location
```

Conceptually:

```
Driver App
    |
    | MQTT Publish
    |
    v
MQTT Broker
    |
    v
Location Consumers
```

MQTT is particularly useful in IoT-style environments where devices communicate with brokers using lightweight messaging.

But it introduces broker infrastructure and additional operational considerations.

Also, MQTT delivery QoS should not be confused with exactly-once business processing.

For a typical ride-hailing mobile application, we shouldn't introduce MQTT automatically just because the data arrives continuously.

## What about SSE and push notifications?

These are important distinctions.

SSE (Server-Sent Events) provides a persistent server-to-client event stream.

It is useful when the client mainly needs to receive server events.

However, it doesn't provide the same bidirectional communication model as WebSockets.

For example:

```
Server --------> Driver App
       Ride Offer
       Cancellation
       Status Update
```

The driver still needs another mechanism, such as HTTP, to submit location updates and acceptance requests.

Push notifications (such as FCM/APNs) are different.

They are useful for notifying drivers when the application isn't actively connected or is in the background.

But they are not a guaranteed low-latency transport for critical dispatch commands.

A notification can be delayed, suppressed, or not delivered.

Therefore:

- WebSocket: Real-time communication while connected.
- SSE: Server-to-client event streaming.
- Push notification: Best-effort notification and wake-up mechanism.
- HTTP: Periodic updates and request/response operations.
- gRPC streaming: Efficient persistent streaming communication.
- MQTT: Lightweight broker-based messaging.

### My starting recommendation

For our initial architecture, I would choose:

- HTTP for straightforward location ingestion.
- WebSockets for real-time offers and responses.
- Push notifications as a fallback notification mechanism where appropriate.

We can also use a bidirectional WebSocket for both location ingestion and dispatch.

There is no requirement that the uplink and downlink use the same protocol.

# 2.3 What does the backend architecture look like?

Let's introduce the load balancer.

Suppose 200,000 drivers are sending location updates.

We cannot handle everything with one backend server.

We'll deploy multiple ingestion servers.

```
                 DRIVER APPLICATIONS
                         |
                         |
                         v
                 LOAD BALANCER
                         |
              +----------+----------+
              |          |          |
              v          v          v
           Server A   Server B   Server C
              |          |          |
              +----------+----------+
                         |
                         v
                    REDIS CLUSTER
```

The load balancer distributes incoming traffic across the available servers.

For HTTP, requests can be routed independently.

For WebSockets, the connection must remain associated with the server that owns it.

Let's understand why.

### HTTP connection

Suppose Driver D101 sends three requests.

```
Request 1 -> Server A
Request 2 -> Server B
Request 3 -> Server C
```

This is completely fine.

Each request contains the driver identity, and every server can update the same shared location store.

We don't need the same server to handle every location update.

This is one of the reasons stateless HTTP ingestion scales well.

### WebSocket connection

Now suppose D101 establishes a persistent connection with Server A.

```
D101
  |
  v
WebSocket Gateway A
```

Its connection cannot simply move to Server B during the session.

Server A owns that TCP/WebSocket connection.

If Server A crashes, the client must reconnect.

It may reconnect to Server B.

Therefore, WebSocket gateways need connection management and routing.

Important distinction: Connection affinity is needed for an established WebSocket connection, but it does not mean the driver must always reconnect to the same server.

### Do we need sticky sessions?

For HTTP location updates, generally no.

For WebSockets, the established connection naturally remains attached to its gateway.

We don't need sticky sessions to preserve driver state if that state is stored in shared infrastructure such as Redis.

# 2.4 Does every ingestion server write directly to Redis?

This is an important architectural decision.

Let's consider two possible designs.

### Design 1: Direct write

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
Redis Cluster
```

A server receives a location update and immediately updates Redis.

For example:

```
D101 -> (17.3850, 78.4867)
```

Redis stores the latest location.

The next update overwrites it.

### Why is this attractive?

Remember our requirement:

We want the newest location to be available as quickly as possible.

Direct writes provide a relatively short processing path.

```
Receive -> Validate -> Redis Update -> Acknowledge
```

There is no queue waiting period.

There is no consumer lag.

There is no additional message-processing infrastructure.

For an ephemeral latest-location workload, this is often an excellent starting design.

### But what happens if Redis becomes slow?

Suppose:

```
Incoming traffic = 100,000 updates/sec
Redis capacity temporarily drops
```

Our ingestion servers may start accumulating pending requests.

Eventually:

- Request latency increases.
- Connection pools become saturated.
- Memory usage increases.
- Timeouts occur.
- Retries increase traffic further.

We need backpressure and load shedding.

We'll discuss the mechanisms shortly.

# 2.5 Should we introduce a queue between ingestion and Redis?

Let's understand when queues help.

Suppose location updates arrive in bursts.

```
10:00:00 -> 50,000 updates/sec
10:00:01 -> 180,000 updates/sec
10:00:02 -> 200,000 updates/sec
10:00:03 -> 50,000 updates/sec
```

Our Redis system might comfortably process 100,000 updates/sec.

But during the burst, incoming traffic is higher than our processing capacity.

A queue can absorb some of this temporary difference.

```
Driver Apps
    |
    v
Ingestion Servers
    |
    v
Message Queue
    |
    v
Consumers
    |
    v
Redis
```

The queue buffers incoming messages.

Consumers process them at a sustainable rate.

However, something important happens here.

Queuing location updates can make the live location index stale.

Imagine a driver's updates:

```
Queue:

Location 1: 10:00:00
Location 2: 10:00:03
Location 3: 10:00:06
Location 4: 10:00:09
```

Suppose the consumer is delayed.

While it is still processing Location 1, the actual driver has already moved.

The matching service may receive outdated coordinates.

This creates a new requirement: we must ensure that old queued messages don't overwrite newer ones.

Also, buffering every update might be wasteful because most of the queued messages will be superseded by newer locations.

Let's look at three queueing strategies.

### Strategy A: Queue every location update

```
Location Ingestion
       |
       v
     Kafka
       |
       +------> Live Location Consumer
       |
       +------> Historical Storage Consumer
```

This is useful when every location event needs to be retained or replayed.

For example:

- Route reconstruction.
- Fleet auditing.
- Historical analytics.
- Downstream event processing.

But it introduces consumer lag and an extra step before the live index is updated.

We will revisit this architecture in Chapter 3.

### Strategy B: Queue only the latest update per driver

Imagine the system receives:

```
D101 -> Location 1
D101 -> Location 2
D101 -> Location 3
```

Instead of keeping all three pending messages, we retain only:

```
D101 -> Location 3
```

This is called coalescing or latest-value compression.

It is useful when the only objective is to update the current position.

However, implementing this requires a carefully designed buffering/coalescing mechanism. An ordinary FIFO queue won't automatically do it.

### Strategy C: Direct Redis for live state, separate asynchronous processing for history

```
                    Location Ingestion
                           |
                 +---------+---------+
                 |                   |
                 v                   v
            Redis GEO           Event Stream
                 |                   |
                 v                   v
          Live Matching       Historical Storage
```

This is attractive because the live matching path doesn't wait for historical storage.

But there is an important caveat: writing to both systems creates a dual-write problem.

For example:

1. Redis update succeeds.
2. Event-stream publication fails.

Now live location and historical history disagree.

Or the reverse can happen.

If historical completeness is important, we need a deliberate reliability strategy. Simply attempting two independent writes is not an atomic operation.

For requirements where every event must be preserved, a durable stream-first ingestion design can be more appropriate.

We'll compare these designs thoroughly in Chapter 3.

# 2.6 Preventing out-of-order location updates

This is one of the most important implementation details.

Imagine the driver sends:

```
Update A:
Timestamp = 10:00:01
Location = A

Update B:
Timestamp = 10:00:02
Location = B
```

But due to network delays:

```
Update B reaches the server first.
Update A reaches the server second.
```

Our Redis updates might become:

```
10:00:02 -> Location B
10:00:01 -> Location A
```

Now we have overwritten a newer location with an older location.

That is bad.

A simple `GEOADD` isn't enough to protect against this.

We need ordering metadata.

## Solution: Sequence numbers

The mobile application includes a sequence number.

```
Update A:
sequenceNumber = 101

Update B:
sequenceNumber = 102
```

Our Redis state stores:

```
D101:
    latitude = ...
    longitude = ...
    sequenceNumber = 102
```

When Update A arrives later, we compare:

```
Incoming sequence = 101
Stored sequence   = 102
```

Since the incoming sequence is older, we reject it.

Conceptually:

```
if (incoming.sequenceNumber() > stored.sequenceNumber()) {
    updateLocation(incoming);
} else {
    ignoreUpdate(incoming);
}
```

But there is a catch.

This comparison must be atomic.

Consider two ingestion servers:

```
Server A receives sequence 101
Server B receives sequence 102
```

Both read the current sequence number as 100.

Server A decides 101 is newer.

Server B decides 102 is newer.

Depending on execution order, they may overwrite each other.

We cannot safely perform the comparison and update as two independent Redis commands.

We need an atomic operation, such as a Redis Lua script, that:

1. Reads the currently stored version.
2. Compares the incoming version.
3. Updates location metadata.
4. Updates the geospatial index.
5. Returns whether the update was accepted.

All of this must happen as one atomic Redis operation.

Conceptually:

```
ATOMIC UPDATE:

if incomingVersion > storedVersion:
    update latest location
    update GEO index
    update lastReceivedTime
    return ACCEPTED
else:
    return IGNORED
```

This solves the concurrent update problem for that driver's record, assuming all related keys are accessed within the same Redis atomicity boundary.

### What about the mobile device restarting?

Suppose the app uses an in-memory counter.

```
Before restart:
Sequence = 500

After restart:
Sequence = 1
```

The new updates would incorrectly appear older.

We need a session generation or epoch.

For example:

```
Driver D101

Session 10:
    Sequence 500

New session after reconnect:
    Session 11:
    Sequence 1
```

We compare:

```
(sessionEpoch, sequenceNumber)
```

Lexicographically:

```
(11, 1) > (10, 500)
```

Therefore, the new session wins.

The session epoch should be managed by the backend when the driver starts a new valid session.

This also prevents delayed messages from a previous session from overwriting locations submitted by the current session.

Do not blindly trust the device's wall-clock timestamp for ordering. Device clocks can drift, jump, or be incorrect.

Use device timestamps for diagnostics and location-age estimation, and use controlled versions/session epochs for update ordering.

# 2.7 What location data should we validate?

We should never blindly trust every coordinate received from a mobile client.

Some possible problems:

- GPS inaccuracies.
- Duplicate updates.
- Old buffered updates.
- Impossible coordinates.
- Abnormal movement.
- Clock drift.
- Spoofed GPS locations.
- Invalid or unauthorized drivers.

Our ingestion service should validate the update before indexing it.

For example:

| Field         | Validation                                                            |
| ------------- | --------------------------------------------------------------------- |
| driverId      | Derived from authenticated identity, not blindly trusted from payload |
| Latitude      | Between -90 and +90                                                   |
| Longitude     | Between -180 and +180                                                 |
| Accuracy      | Within an acceptable range                                            |
| Sequence      | Newer than previously accepted version                                |
| Session       | Current valid driver session                                          |
| Timestamp     | Not implausibly old or in the future                                  |
| Speed         | Check for physically implausible changes                              |
| Driver status | Driver is authorized to report location                               |

### GPS accuracy deserves special attention

Imagine two drivers:

```
Driver A:
Distance = 100 metres
GPS accuracy = 5 metres

Driver B:
Distance = 100 metres
GPS accuracy = 300 metres
```

Are they equally suitable?

Definitely not.

Driver B's reported location is much less precise.

We should carry GPS accuracy as metadata and consider it during matching.

Also, don't simply discard every inaccurate GPS point. Urban areas, underground locations, and weak GPS conditions can produce poor accuracy.

The right policy depends on the business requirements.

# 2.8 Do we need to send every GPS update?

Not necessarily.

Imagine the driver is parked.

```
10:00:00 -> (17.3850, 78.4867)
10:00:03 -> (17.3850, 78.4867)
10:00:06 -> (17.3850, 78.4867)
10:00:09 -> (17.3850, 78.4867)
```

We are repeatedly sending nearly identical coordinates.

This consumes:

- Mobile network bandwidth.
- Backend request capacity.
- Redis write capacity.
- Battery power.

We can optimize client-side sampling.

### Adaptive location reporting

Instead of a fixed interval, the mobile application can adjust its reporting behavior.

For example:

| Driver situation          | Reporting behavior                      |
| ------------------------- | --------------------------------------- |
| Moving quickly            | Frequent updates                        |
| Moving slowly             | Moderate updates                        |
| Stationary                | Less frequent updates                   |
| Trip in progress          | Frequent updates                        |
| Application in background | Platform-appropriate reporting strategy |

The actual intervals must be tuned based on matching latency, battery consumption, network costs, and platform restrictions.

Another optimization is distance-based reporting.

For example:

```
Send an update if:

Distance moved > 10 metres

OR

Time since last update > 5 seconds
```

The time condition is important.

Otherwise, a stationary driver might never send another update, and we would incorrectly consider the location stale.

You can also suppress updates when the movement is insignificant and GPS accuracy is poor.

### Should the server also filter locations?

Yes.

Client-side filtering reduces unnecessary traffic.

Server-side validation protects correctness and security.

Never rely entirely on the mobile client to enforce ingestion rules.

# 2.9 Should we batch location updates?

There are two different situations.

### Situation A: Normal real-time operation

The app sends one latest location update every few seconds.

```
D101 -> Location 101
D101 -> Location 102
D101 -> Location 103
```

Batching is usually unnecessary.

We want the newest location immediately.

### Situation B: Temporary network disconnection

Suppose the driver loses connectivity for 30 seconds.

The app locally collects:

```
10:00:00 -> Location A
10:00:03 -> Location B
10:00:06 -> Location C
10:00:09 -> Location D
...
10:00:30 -> Location K
```

When connectivity returns, should we upload all 11 updates?

For live matching, usually not.

The backend primarily needs the latest location.

We can upload:

```
Latest Location -> K
```

This avoids flooding the backend with obsolete data.

But what if the business needs complete trip history?

Now we have two different delivery policies:

```
LIVE LOCATION:
Only the latest valid update matters.

HISTORICAL LOCATION:
Every required location event may matter.
```

That distinction drives the choice of queueing and durability strategy.

# 2.10 How should we acknowledge a location update?

This is often overlooked.

Consider:

```
Driver App
    |
    | Location Update
    v
Ingestion Server
    |
    v
Redis
```

When should the server return success?

Consider three possible acknowledgements.

Option 1: Acknowledge immediately upon receiving the request.

Bad idea.

The server might crash before persisting the location into Redis.

The application believes its update was accepted, but the update is lost.

Option 2: Acknowledge after successful Redis update.

For our live-location design, this is a reasonable choice.

```
Receive
   |
Validate
   |
Atomic Redis Update
   |
Success
   |
ACK
```

It means the update was accepted by our live storage system.

It does not mean the update is permanently durable.

Option 3: Acknowledge after durable event-stream publication.

This is appropriate when the system promises that the location event has been durably accepted for historical or downstream processing.

For example:

```
Receive
   |
Validate
   |
Append to Kafka
   |
Durable acknowledgement
   |
ACK
```

Consumers can update Redis asynchronously.

However, now our live location may be temporarily behind.

We must accept and manage that trade-off.

This is one of the most important differences between direct-write and stream-first architectures.

# 2.11 Handling retries and duplicate updates

Suppose:

```
Driver App -> Server
             Location Update 101
```

The server writes to Redis successfully.

But the response gets lost.

The app doesn't know whether the update was processed.

It retries.

Now the server receives the same update twice.

Is this a problem?

Not if our processing is idempotent.

For a location update, the same version can simply be ignored.

```
Stored version = 101
Incoming version = 101

Result = Duplicate / Ignored
```

The client can use an exponential backoff with jitter when retrying after transient failures.

For example:

```
Retry 1 -> 1 second
Retry 2 -> 2 seconds
Retry 3 -> 4 seconds
Retry 4 -> 8 seconds
```

The exact limits are configurable.

But there is another critical optimization.

Don't build an infinite backlog of obsolete location updates.

If the driver is currently at Location 120, retrying Location 105 has almost no value for live matching.

The client should prioritize the latest valid location.

For durable historical events, the retry policy is different. We must retain and retry the events that haven't been durably accepted.

# 2.12 What happens during traffic spikes?

Let's return to our example.

```
Online drivers = 1,000,000
Update interval = 3 seconds

Incoming rate ≈ 333,333 updates/sec
```

There might also be sudden bursts when:

- Network connectivity returns after an outage.
- Many drivers log in together.
- A city experiences a large event.
- Applications reconnect after an infrastructure restart.

We need mechanisms beyond adding more servers.

### A. Horizontal scaling

Deploy multiple ingestion servers.

```
                 Load Balancer
                       |
          +------------+------------+
          |            |            |
          v            v            v
       Server A     Server B     Server C
          |            |            |
          +------------+------------+
                       |
                    Redis
```

### B. Backpressure

If Redis starts slowing down, ingestion servers should not keep accepting unlimited pending work.

They should:

- Limit concurrent Redis operations.
- Bound internal queues.
- Apply request deadlines.
- Stop accepting more work when overloaded.
- Return retryable failures where appropriate.
- Protect healthy traffic from unhealthy dependencies.

Otherwise, overload becomes a feedback loop:

```
Redis slows down
      |
      v
Requests accumulate
      |
      v
Latency increases
      |
      v
Clients time out
      |
      v
Clients retry
      |
      v
Even more traffic
      |
      +--------> Redis slows down further
```

This is called a retry storm.

Exponential backoff, jitter, bounded retries, and load shedding help prevent it.

### C. Regional isolation

Suppose a city experiences a sudden traffic burst.

We shouldn't necessarily let it overload every other city.

We can separate live location infrastructure by region:

```
India
 |
 +---- Hyderabad Location Infrastructure
 |
 +---- Bangalore Location Infrastructure
 |
 +---- Mumbai Location Infrastructure
 |
 +---- Delhi Location Infrastructure
```

This is also useful for latency and data locality.

We will explore geographic partitioning and multi-region architecture later.

### D. Avoid unnecessary work

This is frequently more valuable than increasing infrastructure capacity.

Examples:

- Send locations only when necessary.
- Avoid writing identical coordinates repeatedly.
- Avoid persisting every GPS sample if history isn't required.
- Avoid expensive synchronous downstream processing.
- Use compact payloads.
- Limit updates from duplicate or obsolete sessions.

# 2.13 Complete location ingestion architecture

We can now assemble what we've learned.

\#chatgpt-mermaid-\_r_3h4\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_3h4\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_3h4\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_3h4\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_3h4\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3h4\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_3h4\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_3h4\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_3h4\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_3h4\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_3h4\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_3h4\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3h4\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3h4\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_3h4\_ p{margin:0;}#chatgpt-mermaid-\_r_3h4\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3h4\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3h4\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3h4\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_3h4\_ .label text,#chatgpt-mermaid-\_r_3h4\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3h4\_ .node rect,#chatgpt-mermaid-\_r_3h4\_ .node circle,#chatgpt-mermaid-\_r_3h4\_ .node ellipse,#chatgpt-mermaid-\_r_3h4\_ .node polygon,#chatgpt-mermaid-\_r_3h4\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_3h4\_ .rough-node .label text,#chatgpt-mermaid-\_r_3h4\_ .node .label text,#chatgpt-mermaid-\_r_3h4\_ .image-shape .label,#chatgpt-mermaid-\_r_3h4\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_3h4\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_3h4\_ .rough-node .label,#chatgpt-mermaid-\_r_3h4\_ .node .label,#chatgpt-mermaid-\_r_3h4\_ .image-shape .label,#chatgpt-mermaid-\_r_3h4\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_3h4\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_3h4\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3h4\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3h4\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_3h4\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_3h4\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_3h4\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_3h4\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_3h4\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_3h4\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_3h4\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3h4\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3h4\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_3h4\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3h4\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_3h4\_ .icon-shape,#chatgpt-mermaid-\_r_3h4\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_3h4\_ .icon-shape p,#chatgpt-mermaid-\_r_3h4\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_3h4\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_3h4\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_3h4\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_3h4\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_3h4\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_3h4\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_3h4\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_3h4\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_3h4\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3h4\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_3h4\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_3h4\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_3h4\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3h4\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_3h4\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_3h4\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3h4\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_3h4\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_3h4\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3h4\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_3h4\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3h4\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_3h4\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_3h4\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_3h4\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_3h4\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_3h4\_ .node rect,#chatgpt-mermaid-\_r_3h4\_ .node circle,#chatgpt-mermaid-\_r_3h4\_ .node ellipse,#chatgpt-mermaid-\_r_3h4\_ .node polygon,#chatgpt-mermaid-\_r_3h4\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_3h4\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_3h4\_ .mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_3h4\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_3h4\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3h4\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}Driver Mobile AppGPS Sampling and FilteringHTTP or WebSocketLoad BalancerLocation Ingestion ServersAuthentication and ValidationSession and SequenceValidationAccepted Update?Ignore SafelyAtomic Latest LocationUpdateRedis Live Location IndexAcknowledge UpdateOptional Durable EventStreamHistorical LocationProcessingNo: Stale or DuplicateYes

Let's describe the complete process.

1. Driver application obtains GPS coordinates.
2. It decides whether a new update is necessary.
3. It sends the update through HTTP or an existing WebSocket.
4. The load balancer routes the request to an ingestion server.
5. The ingestion server authenticates the driver.
6. It validates the coordinate and session information.
7. It verifies that the update is newer than the stored version.
8. It atomically updates the driver's latest location and geospatial index.
9. The server acknowledges the update.
10. Historical processing happens separately if the business requires it.

# 2.14 A practical implementation example

Let's put the ingestion logic into a Java-style example.

This is simplified application-level logic, not a complete production implementation.

```
public LocationUpdateResult processLocation(
        AuthenticatedDriver driver,
        LocationUpdate update) {

    // 1. Driver identity comes from authentication.
    String driverId = driver.id();

    // 2. Validate coordinates.
    if (update.latitude() < -90 || update.latitude() > 90) {
        throw new InvalidLocationException();
    }

    if (update.longitude() < -180 || update.longitude() > 180) {
        throw new InvalidLocationException();
    }

    // 3. Validate current session.
    if (!sessionService.isCurrentSession(
            driverId,
            update.sessionEpoch())) {
        return LocationUpdateResult.STALE_SESSION;
    }

    // 4. Atomically compare versions and update:
    //
    // - Latest latitude/longitude
    // - Sequence number
    // - Session epoch
    // - Server received timestamp
    // - Redis GEO index
    //
    // This must not be implemented as separate
    // non-atomic read and write operations.

    boolean updated = locationStore.updateIfNewer(
            driverId,
            update
    );

    if (!updated) {
        return LocationUpdateResult.DUPLICATE_OR_STALE;
    }

    return LocationUpdateResult.ACCEPTED;
}
```

Notice that the actual implementation delegates the ordering and update to `locationStore.updateIfNewer()`.

That operation must be atomic.

Also, session validation and location update must be designed so that a session change cannot race with a stale request. In production, the authoritative session epoch should be checked as part of the same atomic storage operation that performs the location update, or protected by an equivalent consistency mechanism.

We haven't yet implemented:

- Redis GEO storage.
- Partition routing.
- Driver liveness expiration.
- Availability validation.
- Historical location persistence.

Those belong to later chapters.

# 2.15 Chapter 2 — Key architectural decisions

Let's consolidate the important choices.

| Problem                               | Recommended starting approach                            |
| ------------------------------------- | -------------------------------------------------------- |
| Periodic location reporting           | HTTP                                                     |
| Bidirectional real-time communication | WebSockets                                               |
| Mobile background notification        | Push notifications                                       |
| Location ingestion                    | Horizontally scaled service                              |
| Live location persistence             | Direct Redis update                                      |
| Historical location storage           | Optional durable stream                                  |
| Out-of-order updates                  | Session epoch + sequence number                          |
| Duplicate updates                     | Idempotent processing                                    |
| Too many identical updates            | Client-side sampling                                     |
| Sudden traffic spikes                 | Backpressure and bounded buffering                       |
| Network disconnection                 | Retry latest location, not obsolete backlog              |
| Update acknowledgement                | After successful write to the chosen acceptance boundary |

## The most important thing to take away

Don't confuse communication, ingestion, buffering, and storage.

They are four different responsibilities:

```
Communication:
How does the driver send data?
HTTP / WebSocket / gRPC / MQTT

Ingestion:
How do we authenticate, validate, and process it?
Location Ingestion Service

Buffering:
Do we need to absorb bursts or durably retain events?
Queue / Kafka (when justified)

Storage:
Where does the latest location live?
Redis / Geospatial Index
```

You don't need all four layers in every architecture.

For the simplest reliable live-tracking system, direct ingestion into Redis is a perfectly sensible starting point.

# Next: Chapter 3 — High-throughput location storage

This chapter is especially important because it addresses your original Redis question.

We'll investigate:

1. How Redis GEO actually stores and queries locations.
2. Why a single Redis GEO key can become a bottleneck even with Redis Cluster.
3. Redis Cluster vs multiple independent Redis partitions.
4. Whether to partition by driver ID or geographic region.
5. Direct Redis writes vs Kafka-first ingestion.
6. When Kafka is useful and when it is unnecessary.
7. How to handle Redis failures without losing the ability to rebuild the live index.
8. How to optimize hundreds of thousands of location writes per second.
9. How to maintain location metadata and geospatial indexes consistently.

The central question we'll answer:

Should every location update go directly into Redis, or should we first put it into Kafka and let consumers update Redis? What exactly do we gain and what do we sacrifice with each design?

Say next to continue.
