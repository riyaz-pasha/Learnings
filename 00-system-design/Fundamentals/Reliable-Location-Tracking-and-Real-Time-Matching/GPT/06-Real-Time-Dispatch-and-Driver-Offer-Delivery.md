# Chapter 6 — Real-Time Dispatch and Driver Offer Delivery

In Chapter 5, we built a matching engine that can find nearby drivers, validate their eligibility, calculate ETA, and rank them.

Now we have something like:

```
Ride R501

Ranked candidates:

1. D101 -> ETA 2 minutes
2. D103 -> ETA 3 minutes
3. D105 -> ETA 4 minutes
4. D107 -> ETA 5 minutes
5. D108 -> ETA 6 minutes
```

The next question is:

How do we deliver the ride request to these drivers, get their responses, handle timeouts, and move to the next driver without creating conflicting assignments?

This is the dispatch problem.

We'll build it incrementally.

## 6.1 First understand what we're sending

A very important distinction:

A driver offer is not a confirmed assignment.

Suppose we send this to D101:

```
{
  "eventType": "RIDE_OFFER",
  "offerId": "OFFER-101",
  "rideId": "R501",
  "pickup": {
    "latitude": 17.3850,
    "longitude": 78.4867
  },
  "expiresAt": "2026-11-12T10:15:30Z"
}
```

What does this mean?

It means:

> We are giving D101 an opportunity to accept Ride R501 until the specified expiration time.

It does NOT mean:

```
R501 is permanently assigned to D101.
```

The assignment happens only after the acceptance goes through our authoritative assignment workflow.

Why is this distinction so important?

Imagine:

```
10:00:00 -> Send offer to D101
10:00:03 -> D101 accepts
10:00:03 -> D101's network response is delayed
10:00:04 -> Our dispatch timeout occurs
10:00:04 -> We send offer to D103
10:00:05 -> D103 accepts
10:00:06 -> D101's acceptance reaches our server
```

Now two drivers have accepted the same ride.

We need to distinguish between:

- An offer being created.
- An offer being delivered.
- An offer being accepted.
- A ride being assigned.

We'll handle the final concurrency guarantee in Chapter 7.

For now, our dispatch workflow must maintain an independent state for every offer.

### The offer state machine

```
CREATED
   |
   v
DISPATCH_PENDING
   |
   v
SENT
   |
   +-----------> ACCEPTED
   |
   +-----------> REJECTED
   |
   +-----------> EXPIRED
   |
   +-----------> CANCELLED
```

There may also be intermediate delivery states such as `DELIVERED` and `ACKNOWLEDGED`.

A ride can have multiple offers:

```
Ride R501
    |
    +---- Offer O101 -> D101 -> EXPIRED
    |
    +---- Offer O102 -> D103 -> REJECTED
    |
    +---- Offer O103 -> D105 -> ACCEPTED
```

This is much better than trying to store all dispatch information as one `driverId` field on the ride.

The ride represents the business request. The offer represents an individual attempt to get a driver to accept it.

# 6.2 First dispatch strategy — Sequential dispatch

Let's start with the simplest strategy.

We have:

```
D101 -> ETA 2 min
D103 -> ETA 3 min
D105 -> ETA 4 min
D107 -> ETA 5 min
```

We contact only the best-ranked driver.

```
Matching Engine
       |
       v
Offer to D101
       |
       v
Wait for response
       |
       +---- Accepted --> Assignment Workflow
       |
       +---- Rejected --> Offer D103
       |
       +---- Timeout ---> Offer D103
```

Let's simulate it.

```
10:00:00 -> Offer D101
10:00:02 -> D101 doesn't respond
10:00:05 -> Offer expires
10:00:05 -> Offer D103
10:00:07 -> D103 rejects
10:00:07 -> Offer D105
10:00:08 -> D105 accepts
```

This is sequential dispatch.

### Why might we choose this?

It offers several advantages.

1. Only one driver receives the offer at a time.
2. We avoid unnecessarily disturbing many drivers.
3. Less offer contention.
4. Less notification traffic.
5. Fewer temporary driver reservations.
6. Easier operational reasoning.

This can be particularly useful when drivers must not receive multiple competing offers.

### But what is the disadvantage?

Latency.

Suppose each driver gets five seconds to respond.

```
D101 -> 5 seconds
D103 -> 5 seconds
D105 -> 5 seconds
D107 -> 5 seconds
```

We might need 20 seconds before reaching D107.

For a real-time ride-hailing system, this may be too slow.

What if the first driver is offline but hasn't yet been detected as offline?

We waste the response window.

We need a faster strategy.

# 6.3 Second strategy — Parallel dispatch

Instead of contacting one driver, what if we contact multiple drivers simultaneously?

For example:

```
              Ride R501
                   |
                   v
              Dispatch Service
                   |
          +--------+--------+
          |        |        |
          v        v        v
        D101     D103     D105
        Offer    Offer    Offer
```

All three drivers receive an offer simultaneously.

Possible responses:

```
D101 -> No response
D103 -> Accepts
D105 -> Accepts
```

Now two drivers have accepted.

Which one wins?

The authoritative assignment workflow must atomically select one winner.

The other acceptance is rejected.

We will implement this in detail in Chapter 7.

### Advantages

- Lower assignment latency.
- More resilience when drivers don't respond.
- Better chance of obtaining an acceptance quickly.
- Less time wasted waiting for individual drivers.

### Disadvantages

- Multiple drivers may accept.
- More notifications.
- More temporary reservations.
- Losing drivers need cancellation messages.
- Increased contention around the same ride.
- Poor user experience if drivers frequently receive offers that are immediately cancelled.

Imagine thousands of ride requests broadcasting offers to hundreds of drivers.

You could end up creating a huge amount of unnecessary notification traffic.

Therefore, contacting every nearby driver is generally not a good default.

# 6.4 Third strategy — Batch or staggered dispatch

This is a useful middle ground.

Instead of contacting one driver or everyone, contact a small batch.

For example:

```
Ranked candidates:

D101
D103
D105
D107
D108
D110
```

We create the first batch:

```
Batch 1:
D101
D103
```

Both receive offers.

If somebody accepts, stop dispatching new offers and finalize the assignment.

If nobody accepts within our response window, create the next batch:

```
Batch 2:
D105
D107
```

Then:

```
Batch 3:
D108
D110
```

Flow:

\#chatgpt-mermaid-\_r_4kq\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_4kq\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_4kq\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_4kq\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_4kq\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4kq\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_4kq\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_4kq\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_4kq\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_4kq\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_4kq\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_4kq\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4kq\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4kq\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_4kq\_ p{margin:0;}#chatgpt-mermaid-\_r_4kq\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4kq\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4kq\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4kq\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_4kq\_ .label text,#chatgpt-mermaid-\_r_4kq\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4kq\_ .node rect,#chatgpt-mermaid-\_r_4kq\_ .node circle,#chatgpt-mermaid-\_r_4kq\_ .node ellipse,#chatgpt-mermaid-\_r_4kq\_ .node polygon,#chatgpt-mermaid-\_r_4kq\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_4kq\_ .rough-node .label text,#chatgpt-mermaid-\_r_4kq\_ .node .label text,#chatgpt-mermaid-\_r_4kq\_ .image-shape .label,#chatgpt-mermaid-\_r_4kq\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_4kq\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_4kq\_ .rough-node .label,#chatgpt-mermaid-\_r_4kq\_ .node .label,#chatgpt-mermaid-\_r_4kq\_ .image-shape .label,#chatgpt-mermaid-\_r_4kq\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_4kq\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_4kq\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4kq\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4kq\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_4kq\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_4kq\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_4kq\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_4kq\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_4kq\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_4kq\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_4kq\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4kq\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4kq\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_4kq\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4kq\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_4kq\_ .icon-shape,#chatgpt-mermaid-\_r_4kq\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_4kq\_ .icon-shape p,#chatgpt-mermaid-\_r_4kq\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_4kq\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_4kq\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_4kq\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_4kq\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_4kq\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_4kq\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_4kq\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_4kq\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_4kq\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4kq\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_4kq\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_4kq\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_4kq\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4kq\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_4kq\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_4kq\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4kq\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_4kq\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_4kq\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4kq\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_4kq\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4kq\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_4kq\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_4kq\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_4kq\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_4kq\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_4kq\_ .node rect,#chatgpt-mermaid-\_r_4kq\_ .node circle,#chatgpt-mermaid-\_r_4kq\_ .node ellipse,#chatgpt-mermaid-\_r_4kq\_ .node polygon,#chatgpt-mermaid-\_r_4kq\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_4kq\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_4kq\_ .mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_4kq\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_4kq\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4kq\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}Rank Eligible DriversSelect Small BatchCreate OffersSend Offers in ParallelWait for Acceptance /Rejection / TimeoutSuccessful Assignment?Stop Dispatch and CancelRemaining OffersMore Candidates?Expand Search Radius orMark UnfulfilledYesNoYesNo

This gives us:

- Better latency than strict sequential dispatch.
- Less notification volume than broadcasting to everyone.
- Controlled competition among drivers.
- The ability to expand progressively.

### Another improvement — Staggered parallel dispatch

We don't even need to send the next batch at exactly the same time.

Example:

```
t = 0 ms:
    Send to D101

t = 200 ms:
    Send to D103

t = 500 ms:
    Send to D105 if necessary

t = 1000 ms:
    Expand to D107 if nobody has accepted
```

The exact timing depends on the platform's latency and acceptance characteristics.

This technique can reduce unnecessary offers while retaining much of the speed benefit of parallel dispatch.

It is useful to think of dispatch strategies as a spectrum:

| Strategy           | Latency         | Notifications | Contention |
| ------------------ | --------------- | ------------- | ---------- |
| Sequential         | Higher          | Low           | Low        |
| Parallel to all    | Lower           | Very high     | High       |
| Small batches      | Moderate to low | Controlled    | Moderate   |
| Staggered parallel | Low             | Controlled    | Moderate   |

My default starting point for a large-scale ride-hailing system would be small-batch or staggered parallel dispatch, with the exact policy tuned to business requirements.

# 6.5 Do we reserve a driver before sending an offer?

Now let's introduce a concept that will be essential for reliable dispatch.

Imagine two customers request rides almost simultaneously.

```
Customer A -> Ride A
Customer B -> Ride B
```

Both searches find:

```
D101 -> Nearest and available
```

If both dispatch services send offers to D101, we have created an unnecessary conflict.

We should temporarily reserve a driver when we decide to offer work to them.

### What does temporary reservation mean?

Initially:

```
D101:
    ONLINE_AVAILABLE
```

We select D101 for Ride A.

Now:

```
D101:
    RESERVED_FOR_OFFER
    offerId = O101
    rideId = RIDE_A
    expiresAt = 10:00:15
```

When Customer B's matching request tries to reserve D101, it sees that D101 already has an active reservation.

It must select another driver.

Notice that the reservation is temporary.

It is not a permanent assignment.

### What happens after an offer response?

Consider four scenarios.

Scenario A: Driver accepts

```
RESERVED_FOR_OFFER
        |
        v
ASSIGNMENT_PENDING
        |
        v
BUSY
```

The authoritative assignment workflow confirms the assignment.

Scenario B: Driver rejects

```
RESERVED_FOR_OFFER
        |
        v
ONLINE_AVAILABLE
```

The reservation is released.

Scenario C: Driver doesn't respond

```
RESERVED_FOR_OFFER
        |
        v
OFFER EXPIRED
        |
        v
ONLINE_AVAILABLE
```

Scenario D: Customer cancels the ride

```
RESERVED_FOR_OFFER
        |
        v
OFFER CANCELLED
        |
        v
ONLINE_AVAILABLE
```

There are important race conditions around these transitions. We'll handle them in Chapter 7.

### Where should we maintain the reservation?

We can use Redis as a fast temporary reservation mechanism.

For example:

```
SET driver:D101:reservation O101 NX PX 15000
```

Meaning:

- Reserve D101 for offer O101.
- Only create the key if it doesn't already exist.
- Automatically expire it after 15 seconds.

This is an atomic conditional operation on Redis.

But we need to understand its guarantee.

Redis reservations are useful for fast coordination, but asynchronous replication and failover can cause recently acquired locks to be lost or inconsistently observed.

Therefore, a Redis lock alone must not be our final protection against double assignment.

The authoritative assignment workflow must still validate the current state using durable, atomic state transitions.

We'll explore this carefully in Chapter 7.

### Should we reserve every nearby driver during discovery?

No.

This is important.

Suppose the search returns 100 drivers.

We shouldn't lock all 100.

We should:

```
Discover 100 candidates
        |
        v
Rank candidates
        |
        v
Select next small batch
        |
        v
Attempt temporary reservation
        |
        v
Send offers to successfully reserved drivers
```

This minimizes unnecessary contention.

# 6.6 How do we actually deliver offers to drivers?

Now let's focus on the communication architecture.

We know how to identify drivers.

But how does the backend send an offer to a driver's mobile application immediately?

Let's evaluate our options in the actual dispatch context.

## Option A — Driver polls the backend

The driver application periodically asks:

```
GET /drivers/offers
```

For example, every two seconds.

```
Driver App
    |
    | GET /drivers/offers
    |
    v
Backend
    |
    | No offer
    |
    v
Driver App

Wait 2 seconds

GET /drivers/offers
```

If an offer arrives just after a poll, the driver might not receive it until the next poll.

Polling creates unnecessary requests even when there are no offers.

For a small system, it is workable.

For a large real-time dispatch system, we generally want something better.

## Option B — WebSockets

A WebSocket gives us a persistent, bidirectional connection.

```
Driver App
     |
     | Establish WebSocket
     |
     v
WebSocket Gateway
```

Once the connection is established:

```
Backend -> Driver: RIDE_OFFER
Driver -> Backend: ACCEPT_OFFER
Driver -> Backend: REJECT_OFFER
Backend -> Driver: OFFER_CANCELLED
```

The server can send an offer without waiting for the client to ask.

That's exactly what we need for real-time dispatch.

For example:

```
10:00:00.000 -> Ride request created
10:00:00.050 -> Matching completed
10:00:00.060 -> Offer created
10:00:00.070 -> WebSocket send
10:00:00.090 -> Driver application receives offer
```

These timings are illustrative, not guaranteed network latencies.

### Why not use WebSockets for everything?

We can, but we don't have to.

Location updates can still use HTTP.

Offer acceptance can use HTTP.

WebSockets may be used only for server-to-client offer delivery.

For example:

```
Driver App
    |
    +---- HTTP POST /location
    |
    +---- WebSocket connection
    |          |
    |          +---- Receive ride offers
    |          +---- Receive cancellations
    |
    +---- HTTP POST /offers/{id}/accept
```

This is a perfectly reasonable architecture.

Or we can use WebSockets in both directions:

```
Location Updates <------> WebSocket
Ride Offers      <------> WebSocket
Acceptance       <------> WebSocket
```

The choice depends on mobile networking requirements, infrastructure capabilities, and how we want to manage connections.

## Option C — SSE

Server-Sent Events provide a persistent server-to-client event stream.

Conceptually:

```
Backend --------> Driver App
       Ride Offer
       Cancellation
       Status Update
```

SSE is unidirectional.

The driver can receive an offer over SSE and send acceptance using a separate HTTP request.

For example:

```
SSE:
    Receive RIDE_OFFER

HTTP:
    POST /offers/O101/accept
```

This is also a valid design.

But because our system naturally has communication in both directions, WebSockets are often a convenient choice.

## Option D — Push notifications

What happens when the driver application is in the background?

A persistent WebSocket connection might not be available.

Mobile operating systems may suspend background networking or terminate application processes.

We can use platform push services such as FCM and APNs.

For example:

```
Backend
   |
   v
Push Notification Provider
   |
   v
Driver Mobile App
```

The notification might say:

```
New ride request available.
Open the app to view details.
```

However, push notifications are not a reliable low-latency delivery mechanism for a critical assignment command.

They can be:

- Delayed.
- Suppressed.
- Dropped.
- Delivered after the offer expires.

Therefore, push notification should be treated as a best-effort notification or wake-up mechanism.

The application should fetch the actual current offer from the backend.

### Communication choices compared

| Technology        | Real-time delivery | Bidirectional        | Suitable role                     |
| ----------------- | ------------------ | -------------------- | --------------------------------- |
| HTTP polling      | Periodic           | No persistent stream | Simple fallback                   |
| WebSocket         | Yes                | Yes                  | Active driver communication       |
| SSE               | Yes                | No                   | Server-to-client offers           |
| Push notification | Best effort        | No                   | Background notification           |
| gRPC streaming    | Yes                | Yes                  | Controlled streaming environments |
| MQTT              | Yes                | Publish/subscribe    | Broker-based device messaging     |

### My recommendation

For an Uber/Ola/Rapido-style system:

```
Foreground driver application:
    WebSocket

Location reporting:
    HTTP or WebSocket

Offer acceptance:
    HTTP or WebSocket command

Background driver application:
    Push notification as fallback

Authoritative offer/assignment state:
    Backend database
```

Do not confuse the transport protocol with the assignment workflow.

A WebSocket message saying `ACCEPT` is only a command received by the backend. The ride is not assigned until the assignment workflow succeeds.

# 6.7 A major scaling problem — Which WebSocket server owns the driver?

Now imagine we have 500,000 connected drivers.

We deploy multiple WebSocket gateway servers.

```
                    Load Balancer
                          |
             +------------+------------+
             |            |            |
             v            v            v
          Gateway A    Gateway B    Gateway C
```

When D101 connects, the load balancer routes the connection to Gateway A.

```
D101
  |
  v
Gateway A
```

D102 connects to Gateway C.

```
D102
  |
  v
Gateway C
```

D103 connects to Gateway B.

```
D103
  |
  v
Gateway B
```

Now suppose our dispatch service wants to send an offer to D101.

Can it directly send a message through Gateway C?

No.

Gateway C doesn't own D101's WebSocket connection.

Each WebSocket gateway owns only its locally connected clients.

We need a way for dispatch servers to identify the gateway responsible for each driver.

This leads us to the connection registry.

## Connection registry

Whenever a driver establishes a WebSocket connection, the gateway registers it.

For example:

```
D101 -> Gateway A
D102 -> Gateway C
D103 -> Gateway B
D104 -> Gateway A
```

We can maintain this information in Redis.

Conceptually:

```
driver:connection:D101
    gatewayId = Gateway-A
    connectionId = Conn-101
    connectionEpoch = 42
    expiresAt = ...
```

The actual registry also needs a mechanism to keep its entries fresh.

For example, each gateway periodically refreshes the connection lease while the connection remains alive.

### How does message routing work?

Suppose Ride R501 needs to be offered to D101.

Our dispatch service does the following:

```
Dispatch Service
      |
      | Lookup D101
      v
Connection Registry
      |
      | D101 -> Gateway A
      v
Message Router
      |
      | Send to Gateway A
      v
WebSocket Gateway A
      |
      v
D101 Mobile App
```

Gateway A has the actual local socket reference.

It looks up D101 in its local connection map and writes the message to the socket.

This solves our distributed connection ownership problem.

## A practical connection registry

We can store:

```
driverId
gatewayId
connectionId
connectionEpoch
lastHeartbeat
expiresAt
```

Why do we need `connectionEpoch`?

Consider this situation:

```
10:00:00 -> D101 connects to Gateway A
10:00:10 -> D101 disconnects
10:00:11 -> D101 reconnects to Gateway B
10:00:12 -> Gateway A receives an old disconnect callback
```

If Gateway A blindly deletes:

```
driver:connection:D101
```

It might delete the new connection registered by Gateway B.

That's wrong.

Instead, Gateway A must delete the registry entry only if the entry still belongs to its original connection epoch or connection ID.

Conceptually:

```
DELETE CONNECTION IF:

stored.gatewayId == Gateway-A
AND stored.connectionId == Conn-101
AND stored.epoch == 42
```

This is an atomic compare-and-delete operation.

Now the old disconnect cannot remove the new connection.

### What if a gateway crashes?

Suppose Gateway A crashes without unregistering its connections.

The registry may still contain:

```
D101 -> Gateway A
D104 -> Gateway A
D109 -> Gateway A
```

These entries become stale.

The registry lease should expire automatically unless the gateway continues refreshing it.

After expiry:

```
D101 -> No active gateway
```

The application must reconnect.

Once reconnected, it registers its new connection.

This is why the registry should use leases or TTLs rather than assuming gateway disconnect callbacks always execute.

# 6.8 How does the dispatch service communicate with a different WebSocket gateway?

We have this information:

```
D101 -> Gateway A
D102 -> Gateway C
D103 -> Gateway B
```

Now our dispatch service needs to send an offer to D101.

There are several ways to route it.

### Option A: Direct internal RPC

```
Dispatch Service
      |
      | RPC: sendToDriver(D101, OFFER)
      v
Gateway A
      |
      v
Driver D101
```

The dispatch service looks up the gateway and calls it over an internal RPC mechanism.

For example, we could expose an internal gRPC service.

Advantages:

- Low latency.
- Simple routing.
- No additional message broker required.

Disadvantages:

- Dispatch service depends on gateway availability.
- It must handle RPC failures and retries.
- Gateway discovery and connection ownership must be accurate.

This is reasonable for many systems.

### Option B: Gateway-specific messaging channels

We can create logical channels for gateway servers.

```
gateway-A
gateway-B
gateway-C
```

The dispatch service publishes:

```
{
  "gatewayId": "gateway-A",
  "driverId": "D101",
  "offerId": "O101",
  "eventType": "RIDE_OFFER"
}
```

Gateway A consumes messages addressed to it and writes them to the appropriate local connection.

Conceptually:

```
                   Message Router
                         |
              +----------+----------+
              |          |          |
              v          v          v
          Channel A  Channel B  Channel C
              |          |          |
              v          v          v
          Gateway A  Gateway B  Gateway C
              |          |          |
              v          v          v
          Drivers     Drivers     Drivers
```

We could implement this using Redis Pub/Sub, a message broker, or another internal messaging system.

However, there's an important distinction.

Redis Pub/Sub is ephemeral.

If Gateway A is disconnected or not listening when the message is published, that message is lost.

Therefore, Redis Pub/Sub is suitable for fast notification routing, but it should not be the only place where a critical offer exists.

### Option C: A durable message broker

We could also publish offer-delivery events to a broker.

For example:

```
Offer Service
     |
     v
Message Broker
     |
     v
Gateway Router
     |
     v
Correct WebSocket Gateway
     |
     v
Driver
```

This is useful when we need durable buffering and retryable delivery between services.

But we must route messages to the correct gateway.

A generic competing-consumer queue with all gateways in the same consumer group isn't automatically correct.

Imagine:

```
Offer for D101 -> Gateway A owns D101

Message Broker
      |
      +---- Gateway A
      +---- Gateway B
      +---- Gateway C
```

If Gateway C consumes the message, it cannot write to D101's local WebSocket connection.

So we need either:

- Gateway-addressed queues or topics.
- Routing through an intermediate service.
- A broker partitioning strategy that respects gateway ownership.
- Direct RPC to the gateway.

### Which should we choose?

For a typical implementation:

```
Dispatch Service
       |
       v
Connection Registry
       |
       v
Gateway Router
       |
       v
Internal RPC / Gateway-specific PubSub
       |
       v
WebSocket Gateway
       |
       v
Driver
```

The offer's durable state should exist separately.

The gateway channel is a delivery mechanism, not the source of truth.

Important: Kafka is not required just to send an offer through WebSockets. A lightweight internal routing mechanism is often sufficient.

We will discuss how to introduce durable messaging correctly in the next sections.

# 6.9 What happens when an offer is sent but the driver doesn't receive it?

This is one of the most important reliability problems.

Imagine:

```
Dispatch Service
      |
      v
Gateway A
      |
      v
WebSocket Send
      |
      X
Network disconnect
```

What do we know?

Almost nothing about the driver's actual experience.

A successful WebSocket write does not prove that:

- The mobile device received the message.
- The application processed it.
- The driver saw it.
- The driver accepted or rejected it.

We need to distinguish message-delivery stages.

## Five different delivery states

Consider this lifecycle:

```
1. OFFER CREATED
2. OFFER QUEUED
3. SENT TO GATEWAY
4. RECEIVED BY APPLICATION
5. DRIVER ACCEPTS / REJECTS
```

Let's understand them.

| Stage               | What it actually means                |
| ------------------- | ------------------------------------- |
| Created             | Offer exists in our business system   |
| Queued              | Delivery is waiting to be attempted   |
| Sent to gateway     | Gateway accepted the outbound message |
| Device acknowledged | Application confirmed receipt         |
| Accepted/rejected   | Driver responded to the offer         |

Notice:

```
Sent to gateway != Received by device
```

And:

```
Received by device != Driver saw it
```

We should never interpret a successful socket write as proof that the driver received the offer.

## Application-level acknowledgement

When the mobile application receives an offer, it should send an acknowledgement.

For example:

```
{
  "eventType": "OFFER_RECEIVED",
  "offerId": "O101",
  "deviceSessionId": "SESSION-88"
}
```

The backend can now mark the offer as acknowledged.

This still doesn't mean the driver accepted it.

The driver might:

- See the offer and reject it.
- See it and ignore it.
- Never interact with it.
- Lose connectivity immediately afterward.

The acknowledgement only confirms that the application received and processed the delivery event.

### Why include offerId in every message?

Imagine the delivery service retries the same message.

The application might receive:

```
OFFER O101
OFFER O101
OFFER O101
```

Without deduplication, the app could show three identical ride offers.

Instead, the app maintains a small set of recently processed offer IDs.

```
Received O101 -> Display
Received O101 -> Ignore duplicate
Received O101 -> Ignore duplicate
```

The server also processes acknowledgements idempotently.

Transport delivery can be at-least-once while business processing remains idempotent.

That is a much more practical reliability strategy than attempting to guarantee exactly-once network delivery.

# 6.10 Should every offer be durable?

This is where we need to distinguish two different things.

The notification message and the business offer.

They are not the same.

Consider:

```
Business Offer:
    OFFER-101
    Ride R501
    Driver D101
    Status = PENDING
    ExpiresAt = 10:00:15
```

This represents a business interaction.

The notification is simply a way to communicate that interaction to the driver's device.

We can send the same notification multiple times.

We can even lose a notification.

But we should still know:

- Whether the offer was created.
- Whether it is still valid.
- Whether it expired.
- Whether it was rejected.
- Whether it was accepted.
- Whether another driver already won the ride.

These facts belong to our offer state.

### Where should offer state be stored?

For a reliable dispatch workflow, I would generally maintain a durable offer record in a transactional database.

For example:

```
CREATE TABLE driver_offers (
    offer_id VARCHAR(100) PRIMARY KEY,
    ride_id VARCHAR(100) NOT NULL,
    driver_id VARCHAR(100) NOT NULL,
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP NOT NULL
);
```

We may also maintain an operational Redis representation for low-latency lookups and temporary leases.

But the database represents the durable workflow state.

Why?

Imagine the dispatch server crashes after creating the offer.

We must be able to recover and determine what happened.

If the offer exists only as an in-memory object inside the dispatch server, the workflow can disappear.

### What about message delivery?

We can create a transactional outbox.

This is an important pattern.

Suppose we need to:

1. Save the offer.
2. Publish an event indicating that the offer should be delivered.

If we perform these as separate operations, we face a dual-write problem.

For example:

```
Insert offer into database -> SUCCESS

Publish offer message -> FAILURE
```

The offer exists, but no delivery event was published.

Or:

```
Publish offer message -> SUCCESS

Database transaction -> FAILURE
```

Now a driver might receive an offer that doesn't exist in our durable business state.

### Transactional outbox solves this

Within one database transaction:

```
BEGIN TRANSACTION

INSERT INTO driver_offers (...);

INSERT INTO outbox_events (
    event_id,
    event_type,
    aggregate_id,
    payload
);

COMMIT;
```

Now both records exist or neither exists.

A separate outbox publisher continuously reads unpublished events.

```
                   Database
                       |
             +---------+---------+
             |                   |
             v                   v
        driver_offers       outbox_events
                                  |
                                  v
                            Outbox Publisher
                                  |
                                  v
                           Delivery Router
                                  |
                                  v
                           WebSocket Gateway
                                  |
                                  v
                               Driver
```

If the publisher crashes:

- The outbox event is still present.
- Another publisher can retry it.

What if it publishes successfully but crashes before marking the event as published?

It may publish the same event again.

That's okay because we use `eventId` and `offerId` to make processing idempotent.

This gives us:

- Durable offer creation.
- Reliable delivery retries.
- Recovery after dispatch-service crashes.
- No need for exactly-once message delivery.

### Do we need Kafka here?

Not automatically.

For an offer-delivery workflow, a durable work queue or broker is often sufficient.

For example:

- RabbitMQ-style queues for work delivery.
- SQS-style managed queues for retryable tasks.
- Kafka for high-volume event streams, replay, multiple independent consumers, and partitioned event processing.

The outbox pattern is independent of the broker we eventually choose.

The key is that the business record and the event requesting delivery are created atomically.

# 6.11 Offer timeouts — What happens when a driver doesn't respond?

Every offer should have an expiration time.

For example:

```
Offer O101

CreatedAt:
    10:00:00

ExpiresAt:
    10:00:15

Response Window:
    15 seconds
```

Why do we need this?

Because we cannot wait indefinitely for a driver.

If D101 doesn't respond, we want to release the temporary reservation and move to another candidate.

### How should the timeout work?

One approach is a background timeout scheduler.

```
Offer Created
     |
     v
Set expiresAt
     |
     v
Timeout Scheduler
     |
     v
Wait until expiration
     |
     v
Check current offer state
     |
     v
Expire if still pending
     |
     v
Release temporary reservation
     |
     v
Notify Dispatch Orchestrator
```

A simple database query might look like:

```
SELECT *
FROM driver_offers
WHERE status IN ('SENT', 'DELIVERED', 'ACKNOWLEDGED')
  AND expires_at <= CURRENT_TIMESTAMP;
```

The timeout processor then attempts a conditional state transition:

```
UPDATE driver_offers
SET status = 'EXPIRED'
WHERE offer_id = 'O101'
  AND status IN ('SENT', 'DELIVERED', 'ACKNOWLEDGED')
  AND expires_at <= CURRENT_TIMESTAMP;
```

If one row is updated, the expiration succeeded.

If zero rows are updated, something else already changed the offer state.

For example:

- The driver accepted.
- The driver rejected.
- Another operation already expired it.
- The offer was cancelled.

The expiration processor must be idempotent.

### Should we create one thread per offer and sleep until timeout?

No.

Imagine we have 100,000 simultaneous offers.

Creating 100,000 sleeping threads is a wasteful approach.

Better choices include:

- A delayed-message mechanism.
- A scheduled job that scans indexed expiration times.
- A timer wheel.
- A distributed scheduler.
- An indexed sorted set in Redis for operational timers, combined with durable state checks.

The exact implementation depends on scale and the latency precision required.

### Very important: The timeout scheduler is not the authority

Consider:

```
Offer expiresAt = 10:00:15

Timeout worker is delayed.

Actual time = 10:00:20
```

The offer has already expired even though the worker hasn't processed it.

What happens if a driver sends acceptance at 10:00:20?

We must reject it based on the authoritative offer expiration check.

We cannot wait for the timeout worker to update the status.

The offer's deadline determines validity.

The timeout worker merely performs cleanup and advances the dispatch workflow.

We'll implement the acceptance-versus-expiration race in Chapter 7.

# 6.12 A tricky situation — A driver accepts an old offer

Consider this sequence:

```
10:00:00 -> D101 receives Offer O101
10:00:15 -> O101 expires
10:00:16 -> Dispatch sends Offer O102 to D103
10:00:18 -> D101's delayed acceptance for O101 arrives
```

Should we accept D101?

No.

The driver is responding to an expired offer.

Every acceptance must contain the original offer ID.

For example:

```
{
  "eventType": "ACCEPT_OFFER",
  "offerId": "O101",
  "requestId": "ACCEPT-REQUEST-991"
}
```

The backend checks:

```
O101 exists?
O101 belongs to D101?
O101 is still valid?
O101 hasn't been superseded or cancelled?
Ride R501 still needs a driver?
```

If any required condition fails, the acceptance is rejected.

The mobile application should display a useful response:

```
This ride offer has expired.
Please wait for another request.
```

### What if the offer notification itself arrives late?

Imagine:

```
10:00:00 -> Offer created
10:00:15 -> Offer expires
10:00:18 -> Old WebSocket message finally arrives
```

The mobile application should inspect `expiresAt`.

If the offer is expired, it should not present it as a valid opportunity.

But the mobile application's clock might be incorrect.

Therefore, the server remains authoritative.

When the driver taps Accept, the server validates the offer again.

Client-side expiration is a user-experience optimization. Server-side expiration is a correctness requirement.

# 6.13 What happens when one driver accepts and other drivers still have offers?

Let's say we used parallel dispatch.

```
Ride R501

D101 -> Offer O101
D103 -> Offer O102
D105 -> Offer O103
```

Now:

```
D103 -> Accepts
```

The assignment workflow successfully assigns R501 to D103.

But O101 and O103 are still pending.

What should happen?

We must cancel them.

```
Ride R501 -> ASSIGNED TO D103

O101 -> CANCELLED
O102 -> ACCEPTED
O103 -> CANCELLED
```

The dispatch service should publish cancellation events:

```
D101 -> RIDE_OFFER_CANCELLED
D105 -> RIDE_OFFER_CANCELLED
```

What if D101 is disconnected and never receives the cancellation?

That's okay, provided the backend doesn't accept the stale offer.

The offer's durable state is already cancelled.

If D101 reconnects, the application fetches active offers and discovers that O101 is no longer valid.

If D101 sends a delayed acceptance, the server rejects it.

This gives us an important principle:

Cancellation delivery is best effort. Cancellation state must be authoritative.

The mobile UI should never be the authority for whether an offer remains valid.

# 6.14 Reconnection — What if the driver disconnects during an offer?

This is a common mobile-network scenario.

Suppose:

```
D101 is connected to Gateway A.

10:00:00 -> Offer O101 created
10:00:01 -> Gateway A sends offer
10:00:02 -> Driver loses network
10:00:05 -> Driver reconnects to Gateway B
```

What happens?

The WebSocket connection to Gateway A is gone.

The new connection is Gateway B.

The connection registry is updated:

```
Before:
D101 -> Gateway A

After:
D101 -> Gateway B
```

But there is another problem.

What happened to O101?

We should not rely on Gateway A to remember it.

The offer exists in our durable offer store.

When D101 reconnects, the application should ask:

```
GET /drivers/offers/active
```

The backend returns currently valid offers.

For example:

```
{
  "offers": [
    {
      "offerId": "O101",
      "rideId": "R501",
      "expiresAt": "2026-11-12T10:00:15Z"
    }
  ]
}
```

Only valid pending offers should be returned.

If O101 has expired or been cancelled, it should not appear.

This recovery mechanism is important because messages can be lost while the client is disconnected.

### Should we replay every notification after reconnect?

No.

We replay the current business state, not blindly replay every historical notification.

Suppose the driver was disconnected for 30 seconds.

During that time:

```
O101 -> Expired
O102 -> Cancelled
O103 -> Expired
O104 -> Newly created and still valid
```

The application only needs O104.

It doesn't need three obsolete offers.

This is another example of the difference between:

- An event stream containing historical events.
- The current state of a business workflow.

For offer recovery, current state is usually what matters.

# 6.15 Do we need Kafka for real-time offer delivery?

Let's answer this explicitly.

There are two different data flows.

### Flow A — Location updates

```
Driver
   |
   v
Location Ingestion
   |
   v
Redis / Optional Kafka
```

Here, Kafka may be justified when we need a durable event stream for historical tracking, analytics, replay, and independent consumers.

### Flow B — Driver offer delivery

```
Dispatch Service
      |
      v
Offer Delivery Router
      |
      v
WebSocket Gateway
      |
      v
Driver
```

For this flow, an ordinary low-latency internal routing mechanism may be sufficient.

The offer itself is durable in the offer state store.

The delivery mechanism retries while the offer remains valid.

We don't need to put every WebSocket message into Kafka simply because Kafka is a scalable technology.

### Queue vs Kafka in this specific context

| Requirement                            | Queue                           | Kafka                                     |
| -------------------------------------- | ------------------------------- | ----------------------------------------- |
| Send offer-delivery task to a worker   | Very suitable                   | Possible                                  |
| Retry failed delivery work             | Commonly supported              | Requires application-managed retry design |
| Expiring short-lived delivery tasks    | Often convenient                | Requires application-managed expiration   |
| Ordered per-key event history          | Depends on queue                | Native partition ordering                 |
| Replay a large historical event stream | Usually not the primary purpose | Excellent fit                             |
| Multiple independent stream consumers  | Depends on system               | Excellent fit                             |
| Durable offer business state           | No, maintain it separately      | No, maintain it separately                |

A subtle point: neither a queue nor Kafka automatically makes the offer business transaction correct.

They deliver work or events.

The offer record and assignment state still need an authoritative store.

### A reasonable architecture

```
              Transactional Database
                       |
                       v
                 Outbox Publisher
                       |
                       v
             Lightweight Message Queue
                       |
                       v
                Gateway Router
                       |
                 +-----+-----+
                 |           |
                 v           v
              Gateway A   Gateway B
                 |           |
                 v           v
              Drivers     Drivers
```

Alternatively, direct internal RPC with a reliable outbox retry mechanism may be sufficient.

Choose the broker according to the delivery workload, not by default.

# 6.16 Connection heartbeat vs location heartbeat

One subtle topic deserves attention here.

Suppose D101 has an established WebSocket connection.

The gateway regularly exchanges ping/pong messages.

```
Gateway -> PING
Driver  -> PONG
```

This tells us that the connection is responsive.

But does it mean that the driver's location is fresh?

No.

Imagine:

```
WebSocket connection: Alive
Location update:      Last received 40 seconds ago
```

The driver might have:

- Disabled location permissions.
- Stopped reporting GPS updates.
- Encountered a GPS issue.
- A malfunctioning location-reporting component.

These are different signals.

We need to distinguish:

| Signal                | What it tells us                        |
| --------------------- | --------------------------------------- |
| WebSocket ping/pong   | Connection responsiveness               |
| Application heartbeat | Application is responsive               |
| GPS location update   | A new location observation was reported |
| Availability state    | Driver's operational status             |

A driver might be connected but not reporting location.

A driver might also report location using HTTP while the WebSocket connection is disconnected.

We should not infer one signal from another.

This will be particularly important in Chapter 8.

# 6.17 A detail that prevents many distributed-lock bugs

Let's revisit the temporary Redis reservation.

Suppose:

```
D101 reserved for O101
TTL = 15 seconds
```

The reservation expires.

Another request now reserves D101:

```
D101 reserved for O202
```

Now imagine a delayed cleanup operation from O101 executes.

If it blindly runs:

```
DEL driver:D101:reservation
```

It accidentally deletes O202's reservation.

This is a distributed coordination bug.

### Solution: Compare before deleting

Store the offer ID as the reservation value:

```
driver:D101:reservation = O101
```

When releasing it, delete only if the current value still matches O101.

Conceptually:

```
IF stored reservation == O101:
    DELETE reservation
ELSE:
    DO NOTHING
```

This must be atomic, for example using a Redis Lua script.

The same idea applies to refreshing a lease.

An old offer must never refresh or delete a newer offer's reservation.

This is why we use unique reservation tokens or fencing tokens rather than treating a lock as just a boolean.

One more important rule:

An expiration timestamp and a TTL are not substitutes for checking the business state.

The Redis reservation may expire, but the offer database still determines whether an offer is valid.

# 6.18 Failure scenarios — Let's test the dispatch system

A reliable design should explain what happens when individual components fail.

Let's simulate real scenarios.

| Failure                                         | What should happen?                                                               |
| ----------------------------------------------- | --------------------------------------------------------------------------------- |
| Driver is offline before offer                  | No active WebSocket connection; skip or attempt push fallback according to policy |
| Gateway crashes                                 | Connection disappears; registry lease expires; client reconnects elsewhere        |
| Connection registry has stale entry             | Router detects failed delivery and refreshes the connection lookup                |
| Gateway receives duplicate offer message        | Gateway or client deduplicates using event/offer ID                               |
| WebSocket write succeeds but client doesn't ACK | Retry the same offer while valid                                                  |
| Driver disconnects after receiving offer        | Offer remains valid until expiry or cancellation; reconnect fetches current state |
| Push notification is delayed                    | Client checks backend state before presenting the offer as valid                  |
| Outbox publisher crashes                        | Unpublished outbox records are retried                                            |
| Queue delivers the same event twice             | Idempotent processing prevents duplicate business effects                         |
| Dispatch service crashes                        | Recover pending offers and dispatch state from durable records                    |
| Timeout event is delivered twice                | Conditional expiration transition ensures only one successful state change        |
| Driver accepts after expiration                 | Reject the acceptance                                                             |
| Another driver wins                             | Cancel remaining offers and reject their late acceptances                         |

Let's examine a few of these more carefully.

### Scenario A: Gateway is down

```
Dispatch Service
      |
      v
Gateway A
      |
      X
Gateway A unavailable
```

We shouldn't lose the business offer.

Our delivery event remains retryable.

The router can:

1. Check the latest connection registry.
2. Determine whether the driver reconnected to another gateway.
3. Retry through the new gateway if the offer remains valid.
4. Attempt push notification if appropriate.
5. Stop delivery attempts when the offer expires.

We shouldn't retry indefinitely.

A ride offer is time-sensitive.

### Scenario B: The application received the offer, but the ACK was lost

```
Gateway -> Driver: O101

Driver receives O101

Driver -> Gateway: ACK

ACK gets lost
```

The delivery service thinks the application might not have received it.

It retries O101.

The application receives it again.

Because it deduplicates by `offerId`, it doesn't display a duplicate offer.

It sends the ACK again.

This is safe.

### Scenario C: The driver accepted, but the response was lost

```
Driver -> Backend: ACCEPT O101

Backend processes acceptance.

Response gets lost.

Driver still doesn't know whether acceptance succeeded.
```

The application retries the same acceptance command using the same request identifier.

The backend must recognize that this is a retry of the same logical operation.

It must not create a new assignment attempt just because the network request was repeated.

This is where idempotent acceptance processing becomes critical.

We'll implement it in Chapter 7.

### Scenario D: Customer cancels while offers are being delivered

Suppose:

```
O101 -> D101
O102 -> D103
```

Customer cancels the ride.

The dispatch workflow changes the ride to its cancelled state and cancels the pending offers.

Cancellation messages are sent to drivers.

However, if a driver taps Accept after cancellation, the backend rejects it.

Again, the backend state is authoritative.

# 6.19 One more architectural improvement — Separate offer state from delivery state

This is a particularly useful modeling decision.

Don't mix business lifecycle and transport lifecycle into one status field.

Consider:

```
Offer O101
```

Business state:

```
PENDING
ACCEPTED
REJECTED
EXPIRED
CANCELLED
```

Delivery state:

```
NOT_QUEUED
QUEUED
SENT_TO_GATEWAY
DEVICE_ACKNOWLEDGED
DELIVERY_FAILED
```

Why separate them?

Imagine:

```
Offer status:
    PENDING

Delivery status:
    DEVICE_ACKNOWLEDGED
```

The driver received the offer, but has not responded.

Another situation:

```
Offer status:
    EXPIRED

Delivery status:
    SENT_TO_GATEWAY
```

The gateway accepted the message, but the device might never have received it before expiration.

Another:

```
Offer status:
    CANCELLED

Delivery status:
    DEVICE_ACKNOWLEDGED
```

The driver received the offer, but it was subsequently cancelled.

These are all perfectly valid states.

A business offer can be valid or invalid independently of its transport-delivery status.

We might maintain:

Offer table

```
offerId
rideId
driverId
status
createdAt
expiresAt
```

Offer delivery table or event history

```
eventId
offerId
gatewayId
deliveryStatus
attemptCount
lastAttemptAt
```

We don't necessarily need to persist every delivery transition forever.

But the logical separation is important.

# 6.20 Complete real-time dispatch architecture

Let's assemble all the pieces.

\#chatgpt-mermaid-\_r_520\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_520\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_520\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_520\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_520\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_520\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_520\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_520\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_520\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_520\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_520\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_520\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_520\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_520\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_520\_ p{margin:0;}#chatgpt-mermaid-\_r_520\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_520\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_520\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_520\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_520\_ .label text,#chatgpt-mermaid-\_r_520\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_520\_ .node rect,#chatgpt-mermaid-\_r_520\_ .node circle,#chatgpt-mermaid-\_r_520\_ .node ellipse,#chatgpt-mermaid-\_r_520\_ .node polygon,#chatgpt-mermaid-\_r_520\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_520\_ .rough-node .label text,#chatgpt-mermaid-\_r_520\_ .node .label text,#chatgpt-mermaid-\_r_520\_ .image-shape .label,#chatgpt-mermaid-\_r_520\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_520\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_520\_ .rough-node .label,#chatgpt-mermaid-\_r_520\_ .node .label,#chatgpt-mermaid-\_r_520\_ .image-shape .label,#chatgpt-mermaid-\_r_520\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_520\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_520\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_520\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_520\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_520\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_520\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_520\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_520\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_520\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_520\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_520\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_520\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_520\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_520\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_520\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_520\_ .icon-shape,#chatgpt-mermaid-\_r_520\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_520\_ .icon-shape p,#chatgpt-mermaid-\_r_520\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_520\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_520\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_520\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_520\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_520\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_520\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_520\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_520\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_520\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_520\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_520\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_520\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_520\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_520\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_520\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_520\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_520\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_520\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_520\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_520\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_520\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_520\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_520\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_520\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_520\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_520\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_520\_ .node rect,#chatgpt-mermaid-\_r_520\_ .node circle,#chatgpt-mermaid-\_r_520\_ .node ellipse,#chatgpt-mermaid-\_r_520\_ .node polygon,#chatgpt-mermaid-\_r_520\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_520\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_520\_ .mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_520\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_520\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_520\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}Customer Ride RequestMatching EngineRanked Driver CandidatesDispatch OrchestratorSelect Small BatchTemporary DriverReservationReservation Successful?Create Durable Offer andOutbox EventOutbox PublisherOffer Delivery RouterConnection RegistryIdentify WebSocket GatewayWebSocket GatewayDriver Mobile AppApplication ACKAccept / RejectAssignment WorkflowTimeout SchedulerOffer Still Pending?Expire Offer and ReleaseReservationNo ActionCancel Remaining OffersNoYesYesNo

Let's walk through the complete flow.

### Step 1 — Customer requests a ride

```
Ride R501
Status = REQUESTED
```

We persist the ride request.

### Step 2 — Matching engine discovers candidates

```
D101
D103
D105
D107
D108
```

They are geographically suitable, fresh, and eligible according to the initial matching snapshot.

### Step 3 — Dispatch chooses a small batch

For example:

```
Batch 1:
D101
D103
```

### Step 4 — Temporarily reserve selected drivers

Try to acquire reservations:

```
D101 -> Reserved for O101
D103 -> Reserved for O102
```

Any failed reservation means that driver is skipped.

### Step 5 — Persist offer state and delivery events

Create durable offer records and corresponding outbox events.

```
O101 -> R501 -> D101 -> PENDING
O102 -> R501 -> D103 -> PENDING
```

### Step 6 — Publish delivery events

The outbox publisher forwards the messages to the delivery router.

### Step 7 — Find the correct WebSocket gateway

```
D101 -> Gateway A
D103 -> Gateway C
```

Messages are routed to the appropriate gateways.

### Step 8 — Deliver to mobile applications

```
Gateway A -> D101
Gateway C -> D103
```

Applications acknowledge receipt.

### Step 9 — Receive driver responses

Possible outcomes:

```
D101 -> Rejects
D103 -> Accepts
```

Acceptance goes to the assignment workflow.

### Step 10 — Resolve the assignment

The assignment workflow determines the winner.

If D103 successfully wins:

```
Ride R501 -> D103
```

All other offers are cancelled.

### Step 11 — Handle timeouts

If nobody accepts:

```
Expire pending offers
Release temporary reservations
Select next batch
```

If we run out of candidates:

```
Expand search radius
     |
     v
Find more drivers
     |
     v
Continue dispatch
```

Eventually, if no suitable driver can be found, the ride enters an appropriate unfulfilled state.

# 6.21 How different businesses should configure dispatch

The underlying infrastructure can be shared, but the dispatch policy should be configurable.

| Requirement                  | Ride hailing                  | Food delivery                               | Logistics                               |
| ---------------------------- | ----------------------------- | ------------------------------------------- | --------------------------------------- |
| Candidate selection          | Nearby drivers                | Nearby eligible partners                    | Suitable vehicles in service region     |
| Primary ranking              | Pickup ETA                    | Pickup/delivery feasibility                 | Route and shipment compatibility        |
| Dispatch strategy            | Sequential or small batches   | Small batches or optimized assignment       | Often controlled/centralized assignment |
| Multiple simultaneous offers | Business-dependent            | Business-dependent                          | Often more restrictive                  |
| Temporary reservation        | Yes                           | Yes                                         | Yes                                     |
| Offer expiration             | Short, time-sensitive         | Based on preparation and delivery deadlines | Based on operational deadlines          |
| Multi-task assignment        | Generally constrained by ride | Important for order batching                | May be essential                        |
| Historical tracking          | Trip tracking                 | Order tracking                              | Shipment/fleet tracking                 |

For example, food delivery can have a more complex dispatch process:

```
Restaurant accepts order
       |
       v
Estimate food preparation time
       |
       v
Find suitable delivery partners
       |
       v
Evaluate current partner workload
       |
       v
Consider existing delivery routes
       |
       v
Dispatch delivery assignment
```

The system might deliberately delay dispatch until the restaurant is closer to preparing the food.

This is different from ride-hailing, where the pickup request generally becomes actionable immediately.

The location infrastructure is shared, but the orchestration strategy differs.

# 6.22 Chapter 6 — What you should take away

| Concept               | Fundamental idea                                                          |
| --------------------- | ------------------------------------------------------------------------- |
| Offer                 | Temporary opportunity, not a confirmed assignment                         |
| Sequential dispatch   | One driver at a time                                                      |
| Parallel dispatch     | Multiple drivers simultaneously                                           |
| Batch dispatch        | Contact a small group at a time                                           |
| Staggered dispatch    | Gradually send offers without waiting for an entire batch timeout         |
| Temporary reservation | Prevent one driver receiving conflicting assignments                      |
| WebSocket gateway     | Owns actual connected client sockets                                      |
| Connection registry   | Maps driver IDs to gateway ownership                                      |
| Internal routing      | Delivers messages to the correct gateway                                  |
| Application ACK       | Confirms application receipt, not driver acceptance                       |
| Transactional outbox  | Makes offer creation and delivery-event publication recoverable           |
| Offer timeout         | Limits how long an offer remains valid                                    |
| Idempotency           | Makes duplicate delivery and responses safe                               |
| Push notification     | Best-effort background notification                                       |
| Reconnection recovery | Fetch currently valid offers rather than replaying every old notification |

Three especially important distinctions:

1\. Offer vs assignment

Receiving an offer does not mean a driver has been assigned.

2\. Delivery vs acceptance

Sending a WebSocket message does not prove that the driver received or accepted it.

3\. Connection liveness vs location freshness

An active WebSocket connection does not prove that GPS updates are being received.

# Next: Chapter 7 — Reliable Assignment and Preventing Double Booking

This is arguably the most important correctness chapter in the entire design.

We've reached the point where two or more drivers might accept the same ride simultaneously.

We'll design the assignment system to guarantee:

One ride can have at most one successful driver assignment, even when requests are concurrent, duplicated, delayed, or retried.

We'll examine:

1. Why application-level `if (available)` checks are unsafe.
2. Why in-memory locks don't work across multiple backend servers.
3. Redis `SET NX` locks and their limitations.
4. Database conditional updates and compare-and-set.
5. Unique constraints and transaction isolation.
6. Driver reservation vs permanent assignment.
7. What happens when two drivers accept simultaneously.
8. What happens when two rides attempt to reserve the same driver.
9. Acceptance requests that time out and get retried.
10. Driver acceptance racing with offer expiration.
11. Crashes between Redis and database updates.
12. How to design a proper assignment state machine.
13. How to recover when the assignment succeeds but the response is lost.

We'll build the correctness guarantee from first principles rather than jumping straight to distributed locks.

The key question we'll answer is:

> What is the single authoritative operation that determines the winner, and how do we guarantee that every backend server agrees on that winner?

Say next to continue.
