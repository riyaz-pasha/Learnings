# Chapter 9 — Scaling the Entire Real-Time Push System

Now we have designed the individual components:

- WebSocket/SSE connections.
- Push servers and connection ownership.
- Global, group, and individual routing.
- Pub/Sub, Kafka, and message queues.
- Connection registry.
- Durable message delivery.
- Failure handling.
- Database and storage architecture.

Now let's put all of them under significant load.

Imagine the application grows from 10,000 users to 10 million users.

What changes?

Do we simply add more push servers?

Not quite.

Because the push servers are only one part of the system.

As the application grows, different components become bottlenecks at different times:

- Push servers run out of memory, file descriptors, or network capacity.
- Redis becomes overwhelmed with connection registration and lookups.
- Kafka partitions become hot.
- Routing workers struggle with audience resolution.
- Large groups create enormous fanout workloads.
- Network bandwidth becomes the actual bottleneck.
- Cross-region communication creates latency and cost.

The central principle of this chapter is:

> Scaling a real-time push system means independently scaling connection capacity, event processing, audience resolution, fanout, delivery throughput, and network bandwidth.

Let's build this incrementally.

# Part A — Understand What We Are Actually Scaling

## 1. There are six independent scaling dimensions

Consider our architecture:

\#chatgpt-mermaid-\_r_ami\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_ami\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_ami\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_ami\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_ami\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_ami\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_ami\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_ami\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_ami\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_ami\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_ami\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_ami\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_ami\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_ami\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_ami\_ p{margin:0;}#chatgpt-mermaid-\_r_ami\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_ami\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_ami\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_ami\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_ami\_ .label text,#chatgpt-mermaid-\_r_ami\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_ami\_ .node rect,#chatgpt-mermaid-\_r_ami\_ .node circle,#chatgpt-mermaid-\_r_ami\_ .node ellipse,#chatgpt-mermaid-\_r_ami\_ .node polygon,#chatgpt-mermaid-\_r_ami\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_ami\_ .rough-node .label text,#chatgpt-mermaid-\_r_ami\_ .node .label text,#chatgpt-mermaid-\_r_ami\_ .image-shape .label,#chatgpt-mermaid-\_r_ami\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_ami\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_ami\_ .rough-node .label,#chatgpt-mermaid-\_r_ami\_ .node .label,#chatgpt-mermaid-\_r_ami\_ .image-shape .label,#chatgpt-mermaid-\_r_ami\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_ami\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_ami\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_ami\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_ami\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_ami\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_ami\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_ami\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_ami\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_ami\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_ami\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_ami\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_ami\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_ami\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_ami\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_ami\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_ami\_ .icon-shape,#chatgpt-mermaid-\_r_ami\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_ami\_ .icon-shape p,#chatgpt-mermaid-\_r_ami\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_ami\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_ami\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_ami\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_ami\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_ami\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_ami\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_ami\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_ami\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_ami\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_ami\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_ami\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_ami\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_ami\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_ami\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_ami\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_ami\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_ami\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_ami\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_ami\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_ami\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_ami\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_ami\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_ami\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_ami\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_ami\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_ami\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_ami\_ .node rect,#chatgpt-mermaid-\_r_ami\_ .node circle,#chatgpt-mermaid-\_r_ami\_ .node ellipse,#chatgpt-mermaid-\_r_ami\_ .node polygon,#chatgpt-mermaid-\_r_ami\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_ami\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_ami\_ .mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_ami\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_ami\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_ami\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}ClientsLoad BalancerPush Server FleetConnection RegistryBusiness ServicesKafkaRouting WorkersDelivery StoreDelivery Workers

We have six distinct workloads.

| Workload              | What determines its scale?                          |
| --------------------- | --------------------------------------------------- |
| Connection management | Number of concurrent connections                    |
| Connection registry   | Number of connections and ownership updates/lookups |
| Event ingestion       | Number of business events produced                  |
| Audience resolution   | Number of recipients per event                      |
| Fanout/delivery       | Event frequency × recipient count                   |
| Network delivery      | Payload size × messages per second                  |

These dimensions can grow independently.

For example:

Application A: Live stock dashboard

- 10 million concurrent connections.
- 10 events per second per stock.
- Each user watches only a few stocks.
- Most updates are small.

This primarily stresses connection management and targeted fanout.

Application B: Group chat

- 1 million concurrent connections.
- Many small conversations.
- Moderate event volume.
- Individual groups have variable sizes.

This stresses group routing and delivery.

Application C: Global sports broadcast

- 10 million concurrent connections.
- One score update is delivered to everyone.
- Updates arrive every few seconds.

This stresses fanout and outbound network bandwidth.

They all use WebSockets or SSE, but their scaling bottlenecks are different.

This is why we shouldn't scale every component equally.

## 2. Understand the most important capacity equation

Let's start with the push servers.

Suppose we have:

```
Total concurrent connections = 1,000,000

Maximum tested connections per push server = 100,000
```

Then the theoretical minimum number of servers is:

\\[ N = \frac{1,000,000}{100,000} = 10 \\]

But this is not a production sizing decision yet.

Why?

Because we haven't accounted for:

- CPU usage.
- Memory per connection.
- TLS overhead.
- Outbound bandwidth.
- Message processing rate.
- Connection churn.
- Failure reserve.

We need capacity headroom.

Suppose we decide to operate each server at no more than 70% of its tested safe capacity.

Then:

\\[ N = \frac{1,000,000}{100,000 \times 0.7} \\]

\\[ N \approx 15 \\]

We would start capacity planning around 15 servers, then validate through load testing and failure testing.

These numbers are illustrative, not universal server capacities.

The important insight: Don't size push servers using concurrent connections alone.

A server with 100,000 mostly idle connections has a very different workload from a server with 100,000 connections receiving 50 messages per second each.

# Part B — Scaling Push Servers

## 3. What limits the number of connections per push server?

A push server is a long-lived network connection manager.

It doesn't necessarily need one thread per connection, but each connection consumes resources.

Let's examine them.

### 3.1 Memory

Each active connection consumes memory in several places:

```
For each connection:

Application metadata
+ Socket state
+ TCP buffers
+ TLS state (if enabled)
+ Protocol/session state
+ Outbound message buffers
```

Suppose a hypothetical load test measures:

```
Average additional memory per connection = 20 KB
```

For 100,000 connections:

\\[ 100,000 \times 20 KB = 2 GB \\]

That's already 2 GB just for the estimated connection-associated memory.

This excludes:

- Application heap overhead.
- Runtime overhead.
- Message payloads.
- Shared caches.
- Framework memory.
- JVM or garbage collector overhead.

And 20 KB is merely an illustrative measurement. Actual memory usage can vary significantly depending on implementation, TLS, operating system settings, and buffering.

Production lesson: Measure actual memory per connection under realistic conditions.

Don't assume that every WebSocket connection consumes a fixed universal amount of memory.

### 3.2 File descriptors

Every network socket generally consumes a file descriptor.

If we want 200,000 concurrent connections, the server and its runtime must allow enough open file descriptors.

For example:

```
Active connections:        100,000
Other open descriptors:      2,000

Required descriptors:      102,000+
```

We need to check:

- Operating system file descriptor limits.
- Process limits.
- Container limits.
- Framework limits.
- Available memory for socket buffers.

If the process reaches its descriptor limit, new connections fail even when CPU usage is low.

### 3.3 CPU

CPU consumption depends on message activity.

Operations include:

- TLS encryption/decryption.
- WebSocket frame processing.
- JSON serialization/deserialization.
- Authentication.
- Authorization.
- Compression.
- Heartbeats.
- Routing.
- Application callbacks.

A server with one million idle connections across a fleet might use relatively little CPU.

The same fleet with a high message rate might become CPU-bound.

### 3.4 Network bandwidth

This is frequently overlooked.

Suppose:

```
100,000 clients
Each receives 1 message/sec
Average payload = 1 KB
```

Then:

\\[ 100,000 \times 1 KB \times 1 = 100 MB/s \\]

That's approximately:

\\[ 800 Mbps \\]

of raw payload throughput for that server.

Actual network consumption is higher after accounting for protocol and transport overhead.

If each client receives 10 messages per second, the raw payload throughput becomes approximately 8 Gbps.

Therefore, connection capacity and message throughput must be benchmarked separately.

## 4. How should we horizontally scale push servers?

The simplest architecture remains:

```
                      Load Balancer
                           |
             ┌─────────────┼─────────────┐
             |             |             |
             ▼             ▼             ▼
          Push 1        Push 2        Push 3
             |             |             |
          100K          100K          100K
        Connections   Connections   Connections
```

When load increases, add more servers.

```
                      Load Balancer
                           |
           ┌───────────────┼────────────────┐
           |               |                |
           ▼               ▼                ▼
       Push 1..3        Push 4..6         Push 7..9
```

The load balancer distributes new connection requests.

Once a connection is established, its selected push server owns it.

We don't need to use the connection registry to make the initial assignment. The load balancer handles that.

The registry is for subsequent routing.

### Should we use sticky sessions?

Not necessarily.

For WebSockets, the established connection naturally remains associated with its selected backend through the lifetime of that proxied connection.

For SSE, the HTTP response remains associated with the backend handling that stream.

When the client reconnects, it can land on another server.

So persistent session affinity across reconnects is not mandatory if the system has a shared connection registry and proper recovery.

There are cases where affinity is useful, but don't assume that every reconnect must reach the previous push server.

### Should we use L4 or L7 load balancing?

Both can work.

L4 load balancing

- Operates at the transport layer.
- Suitable for distributing TCP connections.
- Doesn't need to understand HTTP routes or application payloads.
- Often useful for high-volume persistent connections.

L7 load balancing

- Understands HTTP and application-level routing information.
- Can route based on hostname, URL path, headers, or other HTTP attributes.
- Useful for distinguishing endpoints such as `/ws` and `/stream`.
- Can provide application-level health and routing features.

The correct choice depends on the infrastructure and operational requirements.

For SSE, also make sure proxy buffering is configured appropriately. A proxy that buffers the response can defeat the intended low-latency streaming behavior.

For both protocols, configure idle timeouts, connection limits, and health checks appropriately.

## 5. Use connection draining when adding or removing servers

Suppose we add Push Server 10.

Existing connections stay where they are.

New connections are distributed across the updated fleet.

Suppose we remove Push Server 3.

Don't immediately kill it.

Use the draining workflow:

```
Push Server 3
      |
      ▼
Mark DRAINING
      |
      ▼
Stop receiving new connections
      |
      ▼
Notify existing clients
      |
      ▼
Clients reconnect elsewhere
      |
      ▼
Connection registry updates
      |
      ▼
Terminate old instance
```

This is also important for autoscaling.

An autoscaler should not terminate push servers as aggressively as it might terminate ordinary short-lived HTTP application servers.

Long-lived connections make termination disruptive.

A useful autoscaling policy should account for:

- Connection count.
- CPU and memory.
- Network throughput.
- New connection rate.
- Outbound queue depth.
- Message-processing load.
- Time required to drain connections.

Don't scale push servers based only on CPU utilization.

A server can have low CPU and still be close to its file descriptor, memory, or network limits.

# Part B — Scaling the Connection Registry

## 6. Redis becomes a bottleneck at large connection counts

Let's assume:

```
Concurrent connections = 10 million

Push servers = 100

Average connections per server = 100,000
```

Every push server needs to register its connections in Redis.

Additionally, it periodically refreshes ownership information.

Routing workers continuously perform lookups.

Let's estimate one workload.

Suppose:

```
10 million active connections

Lease refresh interval = 15 seconds
```

If we refresh every connection separately:

\\[ \frac{10,000,000}{15} \approx 666,667 \\]

That is approximately 667,000 connection-renewal operations per second.

That's substantial.

We need to optimize how registry operations are performed.

## 7. Strategy 1: Use server-level leases where appropriate

We discussed this in Chapter 5.

Instead of renewing every connection independently, each push-server instance can periodically renew its own server lease.

For example:

```
Push Server 1:

server:push-1:lease
    expires in 30 seconds
```

Every 10 seconds, it renews the lease.

Its local connections are associated with that server instance:

```
conn-101 -> push-1
conn-102 -> push-1
conn-103 -> push-1
```

A connection is routable only when:

- Its connection metadata is valid.
- Its owning server instance is valid.
- The connection is known to be locally active.

This changes our lease workload from one renewal per connection to one renewal per server instance, plus any individual cleanup or refresh operations we choose to perform.

However, this doesn't eliminate all connection-level registry work.

We still need to:

- Register new connections.
- Remove disconnected connections.
- Maintain user-to-connection indexes.
- Reconcile local connections after Redis recovery.

And individual connection health must still be checked by the push server.

## 8. Strategy 2: Shard the connection registry

One Redis instance should not be expected to handle unlimited traffic.

We can partition the registry across multiple Redis nodes.

For example:

```
                  Connection Registry
                           |
              ┌────────────┼────────────┐
              |            |            |
              ▼            ▼            ▼
          Redis 1       Redis 2       Redis 3
          Shard A       Shard B       Shard C
```

We can distribute users using a deterministic hash:

```
shard = hash(userId) % numberOfShards
```

Conceptually:

```
User A -> Redis 1
User B -> Redis 3
User C -> Redis 2
User D -> Redis 1
```

A routing worker knows which shard contains a particular user's connection metadata.

Redis Cluster provides an implementation of hash-slot-based distribution.

### Why partition by user ID?

Our common individual routing lookup is:

```
userId -> all active connections
```

If all connection records belonging to a user are colocated, the lookup is efficient.

For example:

```
user:{user-A}:connections

user:{user-A}:connection:conn-101

user:{user-A}:connection:conn-102
```

Related keys can use Redis Cluster hash tags to land in the same slot.

This also helps us perform atomic user-level registration and removal operations.

### But what about server-to-connections indexes?

Consider:

```
server:push-1:connections
```

This may contain connections belonging to users distributed across many Redis shards.

Updating the user index and server reverse index might require modifying keys on different shards.

Redis Cluster cannot atomically execute an arbitrary multi-key operation across different hash slots.

This creates a design choice:

- Make the user-centric index authoritative and maintain the server reverse index asynchronously.
- Partition indexes according to their access pattern.
- Use per-server/per-shard structures.
- Reconcile derived indexes periodically.

Don't assume that a schema that works on a standalone Redis instance automatically works atomically in Redis Cluster.

### Another important issue: Hot users

A popular account might have hundreds of thousands of connections or subscriptions.

If we store everything under one user key, that key can become a hotspot.

For ordinary applications, this is unlikely to be a problem.

For extremely large fanout or shared system accounts, we might partition a single entity's state into buckets.

We should introduce this complexity only when measurements show a real hotspot.

## 9. Strategy 3: Avoid unnecessary registry lookups

Suppose a group contains 100,000 connected users across 50 push servers.

A naive router performs:

```
100,000 user lookups
```

But it only needs 50 destination servers.

We can maintain:

```
Group G1 -> Active Push Servers
```

For example:

```
G1:
    Push 1
    Push 2
    Push 3
    ...
    Push 50
```

Now routing requires a group-to-server lookup rather than resolving every individual user.

Each push server already knows its local connections subscribed to G1.

The fanout becomes:

```
Group G1 Event
       |
       ▼
Group Interest Registry
       |
       ▼
50 Active Push Servers
       |
       ▼
Local Subscribers
```

This is one of the most valuable scaling optimizations for group-based real-time systems.

### When should we use which approach?

| Group size / situation  | Approach                                   |
| ----------------------- | ------------------------------------------ |
| Very small group        | Resolve members and connection owners      |
| Medium group            | Batch user lookups and deduplicate servers |
| Large active group      | Group-to-active-server index               |
| Massive global audience | Hierarchical regional/server fanout        |

The exact thresholds should come from your workload.

# Part C — Scaling Kafka and Routing Workers

## 10. Kafka throughput and routing throughput are different

Suppose Kafka can deliver 100,000 records per second to our consumer group.

Does that mean our push system can deliver 100,000 records per second?

Not necessarily.

One record might target one user.

Another record might target 10 million users.

These are very different workloads.

For example:

```
Event A:
    User A -> Push Server 1

Event B:
    Group G1 -> 5 users

Event C:
    Global announcement -> 10 million users
```

Kafka sees three records.

But the fanout work might be:

```
Event A -> 1 recipient

Event B -> 5 recipients

Event C -> 10,000,000 recipients
```

Therefore:

We must measure routing and fanout work, not just Kafka records per second.

## 11. Scale Kafka consumers using partitions

Suppose our topic has:

```
Topic: chat-events

Partitions:
    P0
    P1
    P2
    P3
```

Our router consumer group has:

```
Router 1
Router 2
Router 3
Router 4
```

Kafka assigns one partition to each worker.

```
P0 -> Router 1
P1 -> Router 2
P2 -> Router 3
P3 -> Router 4
```

Now we need more routing capacity.

We can increase partitions and consumers, subject to Kafka's topic/partition design.

```
8 partitions
8 router workers
```

This increases the potential processing parallelism.

But remember:

A partition can be assigned to only one consumer in a consumer group at a time.

Therefore:

\\[ Maximum\ useful\ consumers \leq Number\ of\ partitions \\]

A consumer may be assigned multiple partitions, but adding more consumers than partitions doesn't increase parallel consumption.

### How many partitions should we create?

There isn't one universally correct number.

Consider:

- Expected throughput.
- Desired consumer parallelism.
- Broker capacity.
- Message size.
- Ordering requirements.
- Future growth.
- Consumer processing time.

Avoid having too few partitions if you need high processing parallelism.

Also avoid creating an excessive number of partitions without considering broker metadata, recovery time, and operational overhead.

## 12. Hot partitions

Now suppose we partition `chat-events` using:

```
key = conversationId
```

This is useful for ordering.

But imagine one conversation is extraordinarily active.

```
Conversation G1 -> 100,000 events/sec

Conversation G2 -> 10 events/sec

Conversation G3 -> 5 events/sec
```

All G1 events go to the same partition.

That partition becomes hot.

Adding more consumers doesn't solve the problem because all G1 events still belong to one partition.

We have a tradeoff:

```
Strict ordering within one conversation
                 |
                 ▼
One partition owns that conversation
                 |
                 ▼
Limited parallelism for that conversation
```

Possible solutions:

A. Increase the overall partition count.

This helps distribute different conversations across more partitions.

It doesn't split one extremely hot conversation.

B. Separate unusually hot entities.

A very large public chat group could have a dedicated topic or processing path.

This is one situation where a dedicated topic can be justified.

C. Relax the ordering requirement.

If independent message categories can be processed separately, we can distribute them using different routing keys.

But this loses global ordering across those categories.

D. Introduce subpartitioning with sequence coordination.

For very hot entities, we might divide processing across several shards while maintaining an application-level ordering mechanism.

This increases complexity significantly.

The important decision is:

Don't sacrifice ordering blindly just to increase throughput. First establish which ordering guarantee the application actually needs.

## 13. Don't make Kafka workers perform enormous synchronous fanout

Imagine a router consumes a global announcement targeting 10 million users.

A naive implementation:

```
for (String userId : allUsers) {
    deliverTo(userId);
}
```

This is dangerous.

The worker might spend minutes processing one event.

During that time, its Kafka partition makes little progress.

Other events assigned to the same partition get delayed.

We need to separate event consumption from large fanout execution.

### Introduce fanout jobs

Instead:

```
Kafka Event
    |
    ▼
Router
    |
    ▼
Create Fanout Job
    |
    ▼
Fanout Workers
    |
    ▼
Recipient Batches
    |
    ▼
Destination Push Servers
```

For example:

```
Global announcement:

10 million recipients

Divide into 10,000 batches.

Each batch contains 1,000 recipients.
```

These are illustrative batch sizes.

Each batch can be processed independently.

A durable fanout job might track:

```
{
  "fanoutId": "fanout-101",
  "audienceId": "global-users",
  "totalRecipients": 10000000,
  "completedBatches": 0,
  "status": "PROCESSING"
}
```

The fanout coordinator creates and tracks batches.

Workers process them with bounded concurrency.

This keeps one very large audience from blocking unrelated event-processing work.

### One more optimization: Batch by destination server

Suppose 1,000 recipient IDs map to:

```
Push 1 -> 300 connections

Push 2 -> 400 connections

Push 3 -> 200 connections

Push 4 -> 100 connections
```

Instead of sending 1,000 separate dispatch requests, send four destination batches.

```
Fanout Worker
     |
     ├── Push 1: 300 recipients
     ├── Push 2: 400 recipients
     ├── Push 3: 200 recipients
     └── Push 4: 100 recipients
```

This reduces internal network calls and routing overhead.

However, each push server must still use bounded processing and outbound buffers.

# Part D — Fanout-on-Write vs Fanout-on-Read

## 14. This is a fundamental scaling decision

Let's introduce two ways of distributing data to a large audience.

Imagine we're building a social application.

A user publishes a post.

They have 1 million followers.

We want all followers to see the post.

We have two approaches.

### Pattern A: Fanout-on-Write

When the post is created, we immediately determine the recipients and create individual feed entries or delivery work.

```
                  New Post
                     |
                     ▼
                Fanout Worker
                     |
           ┌─────────┼─────────┐
           |         |         |
           ▼         ▼         ▼
        User A     User B     User C
         Inbox      Inbox      Inbox
```

We perform the fanout when the event occurs.

Advantages:

- Fast reads.
- Recipient-specific delivery tracking.
- Easy to identify pending recipients.
- Useful when recipients must receive individual notifications.

Disadvantages:

- Expensive writes for enormous audiences.
- High storage amplification.
- More work during event creation.

### Pattern B: Fanout-on-Read

Instead of copying the post into every follower's inbox, we store the post once.

When a user opens their feed, we retrieve relevant posts from the accounts they follow.

```
New Post
   |
   ▼
Store Once
   |
   ▼
User Opens Feed
   |
   ▼
Read Relevant Posts
```

Advantages:

- One write instead of millions of feed writes.
- More efficient for exceptionally large audiences.
- Less storage amplification.

Disadvantages:

- More expensive reads.
- More complicated query aggregation.
- Potentially higher read latency.

### Pattern C: Hybrid fanout

Very large systems often combine the two.

For ordinary accounts:

```
Fanout-on-Write
```

For exceptionally large accounts:

```
Fanout-on-Read
```

This is known as a hybrid fanout approach.

### How does this apply to real-time push?

The same reasoning applies.

Suppose a celebrity account has 20 million followers.

When it publishes a live update, should we create 20 million independent delivery records immediately?

Not necessarily.

Instead:

1. Store the announcement once.
2. Publish a lightweight event.
3. Broadcast it to relevant regions or push-server nodes.
4. Let connected clients receive the live update.
5. Let offline clients retrieve announcements from their inbox/feed.

For a smaller group where individual delivery acknowledgements matter, fanout-on-write may still be better.

Important: Fanout-on-read is primarily a data retrieval strategy. It doesn't mean the server cannot send live notifications. We can store one authoritative event and use live fanout as a lightweight wake-up mechanism.

# Part E — The Real Bottleneck: Network Fanout

## 15. Why network bandwidth becomes expensive

Let's imagine:

```
Connected users = 1 million

Average event payload = 500 bytes

Events per user = 1 per second
```

Total outgoing payload:

\\[ 1,000,000 \times 500 \times 1 = 500,000,000\ bytes/sec \\]

That's approximately:

```
500 MB/sec
4 Gbps
```

This is raw payload alone.

Actual network traffic will be higher because of:

- TCP/IP overhead.
- TLS records.
- WebSocket framing.
- Retransmissions.
- Application metadata.
- Internal router-to-push-server traffic.

Now increase the number of users:

```
10 million users
500-byte event
1 event/second
```

The raw payload becomes:

```
5 GB/sec
40 Gbps
```

This is a substantial network workload.

And remember, this assumes every user receives every event.

Many systems have much lower fanout because each user subscribes to only a subset of events.

The key calculation is:

\\[ OutboundBandwidth = ConnectedRecipients \times EventsPerSecondPerRecipient \times AveragePayloadSize \\]

For multiple event categories:

\\[ TotalBandwidth = \sum_i (R_i \times F_i \times S_i) \\]

Where:

- \\(R_i\\) = recipients of event category \\(i\\).
- \\(F_i\\) = update frequency.
- \\(S_i\\) = average payload size.

This equation is extremely useful for real-world capacity planning.

## 16. How do we reduce network consumption?

There are several techniques.

### A. Send state changes instead of full snapshots

Suppose a dashboard contains:

```
{
  "price": 100,
  "volume": 5000,
  "high": 105,
  "low": 95,
  "change": 2
}
```

Every time the price changes, we could send the entire object.

But instead, send:

```
{
  "price": 101
}
```

The client updates its local state.

We have reduced payload size.

However, the client must have a correct base state and a resynchronization mechanism.

### B. Coalesce frequent updates

Suppose a stock price changes 100 times in one second.

We don't necessarily need to send every update to every user.

We can aggregate:

```
100 updates
     |
     ▼
Latest price per short time window
     |
     ▼
One outgoing update
```

Suitable for dashboards and latest-state applications.

Not suitable for chat messages or ordered collaborative operations where every event matters.

### C. Batch messages

Suppose one push server has 1,000 outgoing notifications.

Instead of making 1,000 separate internal dispatch calls, it can receive a batch.

```
{
  "type": "DELIVERY_BATCH",
  "events": [
    {"eventId": "101"},
    {"eventId": "102"},
    {"eventId": "103"}
  ]
}
```

The push server can then process the batch with controlled concurrency.

On the client transport, batching can also reduce per-message overhead, although it adds a small amount of latency.

### D. Compress large payloads

WebSocket compression can reduce repetitive textual data.

But compression consumes CPU and may introduce additional security considerations when compressing secret and attacker-controlled content together.

Use it based on actual payload characteristics and benchmarks.

For small messages, compression can cost more CPU than it saves in bandwidth.

### E. Send references instead of huge payloads

For example:

```
{
  "type": "DOCUMENT_UPDATED",
  "documentId": "D1",
  "version": 101
}
```

The client fetches the current document state or missing operations through the application API.

This can be especially beneficial for large documents, images, and other media.

## 17. Internal network fanout: Don't accidentally send the same message everywhere

Suppose we have:

```
10 million connected users.

100 push servers.
```

A global event is relevant to everyone.

Sending the event to all 100 push servers is necessary.

But suppose an event is relevant to just one user.

Broadcasting to all 100 servers is wasteful.

The system should preserve this distinction:

| Audience            | Internal distribution              |
| ------------------- | ---------------------------------- |
| All users           | All relevant nodes                 |
| Large group         | Only nodes hosting group members   |
| Individual user     | Only nodes owning user connections |
| Specific connection | Exactly its owning node            |

Another useful optimization is to avoid sending the actual payload multiple times unnecessarily.

For example, a large broadcast payload can be stored once in a regional cache or durable event store.

The push servers can receive a reference and retrieve the payload.

But that adds another storage dependency.

For small payloads, directly distributing them is usually simpler.

Always compare:

```
Extra network bytes
```

against:

```
Additional cache lookups + operational complexity
```

# Part F — Scaling Large Groups and Hot Entities

## 18. What happens when one group has 10 million members?

Let's say we have a very large public group.

```
Group G1:

10 million members

1 million currently online
```

A new event occurs.

We need to deliver it to one million active users.

There are several scaling challenges:

1. Finding active push servers.
2. Sending the event to all those servers.
3. Distributing the workload across push servers.
4. Preventing a single fanout worker from becoming a bottleneck.
5. Handling slow users.
6. Managing retries and offline recovery.

Let's progressively improve the architecture.

### Design A: One fanout worker

```
Group Event
     |
     ▼
Fanout Worker
     |
     ├── Push 1
     ├── Push 2
     ├── Push 3
     ├── ...
     └── Push 100
```

This is simple.

But one worker must coordinate all destinations.

For a very large audience, it could become a bottleneck.

### Design B: Multiple fanout workers

```
                  Group Event
                       |
                       ▼
                Fanout Coordinator
                       |
              ┌────────┼────────┐
              |        |        |
              ▼        ▼        ▼
          Fanout 1  Fanout 2  Fanout 3
              |        |        |
              ▼        ▼        ▼
           Pushes   Pushes   Pushes
```

The coordinator divides destination push servers into batches.

Each worker handles one batch.

We need a durable job/checkpoint mechanism if a fanout operation must survive worker failures.

### Design C: Hierarchical fanout

For very large deployments, introduce regional or node-level fanout layers.

```
                     Group Event
                          |
                          ▼
                    Global Router
                          |
              ┌───────────┼───────────┐
              |           |           |
              ▼           ▼           ▼
           Region A    Region B    Region C
              |           |           |
              ▼           ▼           ▼
          Regional     Regional     Regional
          Fanout       Fanout       Fanout
              |           |           |
              ▼           ▼           ▼
          Push Fleet   Push Fleet   Push Fleet
```

The global router doesn't need to talk to 10,000 push servers individually.

It sends the event to regional fanout coordinators.

Each region distributes the event locally.

This reduces cross-region coordination and makes the system more scalable.

### What if Group G1 is mostly offline?

We should not send millions of live delivery requests to disconnected users.

The durable group message stays in the authoritative data store.

The live fanout targets active interested servers.

Offline users recover through synchronization.

This distinction reduces unnecessary work dramatically.

## 19. Hot groups: Why one group can overwhelm the system

Consider:

```
Group G1 -> 1 million active users

Group G2 -> 10 members

Group G3 -> 20 members
```

An event in G1 produces vastly more delivery work.

If we process everything identically, G1 might starve G2 and G3.

We need workload isolation.

Possible approaches:

- Dedicated large-group fanout workers.
- Per-group concurrency limits.
- Regional fanout.
- Per-tenant quotas.
- Coalescing.
- Separate durable fanout jobs.
- Priority classes.

For example:

```
                  Event Processing
                         |
              ┌──────────┴──────────┐
              |                     |
              ▼                     ▼
        Normal Groups           Large Groups
              |                     |
              ▼                     ▼
        Normal Workers         Fanout Workers
              |                     |
              ▼                     ▼
         Push Servers           Regional Relays
```

A large group's workload is isolated from ordinary messaging.

This is especially useful in collaboration platforms, live events, public channels, and broadcast applications.

# Part G — Multi-Region Architecture

## 20. Why do we need multiple regions?

So far, we've assumed one geographical region.

Imagine we have users in:

- India.
- Europe.
- North America.
- Southeast Asia.

If every user connects to one region, users far away experience:

- Higher network latency.
- More packet loss on long-distance paths.
- Greater dependence on one region.
- More expensive cross-region bandwidth.

We should distribute push servers geographically.

For example:

```
                     Global DNS / Traffic Manager
                                |
                 ┌──────────────┼──────────────┐
                 |              |              |
                 ▼              ▼              ▼
             India Region   Europe Region   US Region
                 |              |              |
                 ▼              ▼              ▼
             Push Fleet     Push Fleet     Push Fleet
                 |              |              |
                 ▼              ▼              ▼
              Clients        Clients        Clients
```

Users should generally connect to an appropriate nearby healthy region.

The traffic manager might use:

- GeoDNS.
- Anycast.
- Global load balancing.
- Latency-based routing.
- Health-based failover.

The appropriate mechanism depends on the networking infrastructure.

### Important: The connection is owned regionally

Suppose User A connects in India.

```
User A -> India Region -> Push 1
```

The actual connection belongs to Push Server 1 in India.

A service operating in Europe cannot directly write to that socket.

It needs to route the event to India's push infrastructure.

This is exactly the same connection ownership principle we established in Chapter 1, now applied across regions.

## 21. Introduce regional connection registries

Should all push servers in all regions access one global Redis cluster?

We could attempt this, but it introduces unnecessary cross-region traffic and latency.

Instead, maintain region-local connection registries.

```
                  Global Routing Directory
                            |
              ┌─────────────┼─────────────┐
              |             |             |
              ▼             ▼             ▼
         India Region  Europe Region   US Region
              |             |             |
              ▼             ▼             ▼
         Local Redis    Local Redis    Local Redis
              |             |             |
              ▼             ▼             ▼
         Push Fleet     Push Fleet     Push Fleet
```

Each region maintains:

```
userId -> local connection IDs -> local push servers
```

For example:

```
India Redis:

User A:
    conn-101 -> India Push 1

Europe Redis:

User A:
    conn-202 -> Europe Push 3
```

This is useful because the same user can be connected from multiple devices in different regions.

Now we need another capability.

How does a routing worker know which regions currently have connections for User A?

## 22. Introduce a global user-to-region directory

We can maintain a higher-level routing directory.

Conceptually:

```
User A
   |
   ├── India Region
   |
   └── Europe Region
```

The global directory only tells us which regions have active connections.

It doesn't need to know every connection ID or push-server instance.

Then the routing flow becomes:

```
               Event for User A
                      |
                      ▼
              Global Region Directory
                      |
              ┌───────┴────────┐
              |                |
              ▼                ▼
          India Region    Europe Region
              |                |
              ▼                ▼
          Local Redis     Local Redis
              |                |
              ▼                ▼
          Push Server    Push Server
              |                |
              ▼                ▼
          Connection     Connection
```

This is a hierarchical registry.

It avoids requiring every global router to maintain detailed connection metadata for every user.

### What if User A is connected in only one region?

Then only one regional dispatch is necessary.

### What if User A has several devices in different regions?

We route to all applicable regions.

### What if the global directory is temporarily stale?

The regional routing request can return:

```
NO_ACTIVE_CONNECTION
```

The router can invalidate the stale region entry conditionally and consult durable delivery/recovery state.

For critical messages, the business message should remain recoverable even if the global directory temporarily fails.

The global directory is a routing optimization, not the authoritative source of user or message data.

## 23. Where should Kafka live in a multi-region architecture?

Now we have another question.

Should all regions publish to one Kafka cluster?

Or should every region have a separate Kafka cluster?

There are several possible designs.

### Design A: One central Kafka cluster

```
India Services
       |
       ▼
       |
Europe Services ─────► Central Kafka
       |
       ▼
US Services
```

All regions publish to and consume from the same cluster.

This is relatively simple conceptually, but can introduce:

- Cross-region network latency.
- Higher inter-region bandwidth.
- A central failure domain.
- Data residency complications.
- Dependence on cross-region network connectivity.

It may be suitable for smaller deployments or certain global event pipelines.

But it is not my default choice for a large geographically distributed real-time push system.

### Design B: Regional Kafka clusters

Each region has its own Kafka cluster.

```
India Region:
    Business Services
          |
          ▼
    India Kafka
          |
          ▼
    India Routers
          |
          ▼
    India Push Fleet


Europe Region:
    Business Services
          |
          ▼
    Europe Kafka
          |
          ▼
    Europe Routers
          |
          ▼
    Europe Push Fleet


US Region:
    Business Services
          |
          ▼
    US Kafka
          |
          ▼
    US Routers
          |
          ▼
    US Push Fleet
```

Advantages:

- Low-latency regional processing.
- Failure isolation.
- Regional scaling.
- Better data locality.
- Reduced continuous cross-region communication.

But global events still need to reach multiple regions.

For example:

```
Global Announcement
       |
       ▼
Origin Region
       |
       ├── India Kafka
       ├── Europe Kafka
       └── US Kafka
```

We can replicate or forward relevant events to regional clusters.

Technologies such as Kafka MirrorMaker 2 can replicate Kafka topics between clusters, but replication topology, offsets, ordering, retention, and duplicate handling must be designed deliberately.

Don't assume cross-cluster replication gives us one globally ordered stream.

### Design C: Hybrid

A large application might have:

```
Regional Kafka clusters
        +
Selected global event streams
        +
Regional fanout services
```

For most real-time user delivery, this is a more scalable direction than sending every event through one central cluster.

## 24. Cross-region delivery: A practical example

Suppose Alice is in India.

Bob is in Europe.

They are members of the same conversation.

Alice sends:

```
Hello Bob!
```

What happens?

### Step 1: Alice submits the message in India

```
Alice
  |
  ▼
India Chat Service
  |
  ▼
India Chat DB
  |
  ▼
India Outbox
```

The message is persisted in its authoritative location.

### Step 2: Generate a domain event

```
{
  "eventId": "evt-101",
  "conversationId": "conv-10",
  "recipientId": "bob",
  "payload": {
    "messageId": "msg-101",
    "text": "Hello Bob!"
  }
}
```

### Step 3: Determine Bob's active regions

The global directory tells us:

```
Bob -> Europe
```

### Step 4: Forward the event to Europe

```
India Kafka
     |
     ▼
Cross-region Event Forwarder
     |
     ▼
Europe Kafka
```

For an important event, the forwarding mechanism needs durable handoff, retry, and idempotency.

### Step 5: Route locally in Europe

```
Europe Kafka
     |
     ▼
Europe Router
     |
     ▼
Europe Connection Registry
     |
     ▼
Push Server 3
     |
     ▼
Bob's Device
```

Notice something important:

The cross-region system delivers the event to Bob's region, and then the regional system takes over local routing.

We don't need to send the event to every push server globally.

### What if Bob is offline?

The chat message remains in the authoritative chat history.

The cross-region live notification might be skipped or marked pending according to the delivery model.

When Bob reconnects, the synchronization service retrieves the missing message.

The cross-region delivery path is not the only source of truth.

## 25. What about groups whose members are spread across regions?

Consider:

```
Group G1:

India:
    User A
    User B

Europe:
    User C
    User D
    User E

US:
    User F
```

When User A sends a group message, we shouldn't enumerate every push server globally.

Instead:

```
                 Group G1 Event
                        |
                        ▼
                 Global Group Router
                        |
            ┌───────────┼───────────┐
            |           |           |
            ▼           ▼           ▼
        India Region Europe Region US Region
            |           |           |
            ▼           ▼           ▼
         Local       Local       Local
         Interest    Interest    Interest
         Registry    Registry    Registry
            |           |           |
            ▼           ▼           ▼
         Push Fleet  Push Fleet  Push Fleet
```

The global group router identifies regions with interested users.

Each region resolves its own active push servers.

This is hierarchical fanout.

### Why is this better?

Because:

- The global layer handles region-level distribution.
- Each regional layer handles local server-level distribution.
- Individual push servers handle their own local connections.

We have divided one massive operation into smaller independent operations.

### One more consideration: Group authorization

Membership data may be globally replicated or regionally partitioned.

We need a clear authoritative membership service and a defined propagation model for membership changes.

For sensitive collaboration data, a stale group subscription must not grant someone continued access after they have been removed.

The routing cache is not the final authorization authority.

# Part H — Multi-Region Consistency and Failover

## 26. What happens when an entire region goes down?

Suppose Europe Region goes offline.

```
India Region       Europe Region       US Region
     UP                DOWN                 UP
```

All connections in Europe are lost.

What should happen?

1. Global traffic management stops sending new connections to Europe.
2. Clients reconnect to an available region where permitted.
3. Their previous connections become stale.
4. The active connection directory is updated.
5. They replay missed events from durable application history.
6. New regional routing becomes active.

But there's an important distinction.

Failing over push connections is easier than failing over authoritative business data.

If a user reconnects to India, that doesn't automatically mean the India region can safely accept writes to the user's business data.

We must separately design database failover and write ownership.

For example:

```
User's business data:
    Home region = Europe

Temporary push connection:
    Can move to India during Europe outage
```

The user can establish a real-time connection in India while their business operations continue through an appropriate data failover or recovery strategy.

If the business database is unavailable and no safe failover is configured, the push server should not invent a successful business result.

## 27. Home region vs connection region

This distinction is extremely useful.

A user might have:

```
User A:

Home region:
    India

Active desktop:
    Europe

Active mobile:
    India
```

The home region is where their primary business data may be owned.

The connection region is where a particular live socket currently exists.

These need not be the same.

This allows us to design:

```
Home Region:
    Business state
    Authoritative writes
    Business transactions

Connection Region:
    Local sockets
    Connection registry
    Real-time delivery
```

However, not every application needs a home region per user.

Some applications partition business data differently, for example by tenant, document, account, or geographic residency boundary.

The deeper principle is:

Business data placement and connection placement are separate architecture decisions.

## 28. Should we use active-active multi-region architecture?

It depends.

Active-active means multiple regions can serve traffic simultaneously.

For push connections, this is often quite natural:

```
India:
    Accept connections.

Europe:
    Accept connections.

US:
    Accept connections.
```

Each region manages its own sockets.

But business writes require more care.

Suppose two regions independently update the same conversation.

```
India:
    Message sequence 101

Europe:
    Message sequence 101
```

Now we have conflicting sequence numbers.

Similarly, two regions might modify the same account balance or document version.

We need an ownership or conflict-resolution model.

Possible approaches:

- Home-region writes.
- Partition ownership.
- Single-leader writes per entity.
- Globally coordinated transactions.
- CRDTs for suitable collaborative data.
- Explicit application conflict resolution.

Don't assume that because push connections are active-active, the business database can safely become multi-writer without additional consistency design.

For many systems, a good compromise is:

Active-active connection infrastructure, with clearly defined business-data write ownership.

This provides low-latency connections without making every business transaction globally distributed.

# Part I — How the Architecture Evolves at Different Scales

## 29. Stage 1: 10,000 concurrent connections

At this stage, avoid overengineering.

A reasonable architecture:

```
                 Load Balancer
                       |
              ┌────────┴────────┐
              |                |
              ▼                ▼
           Push 1           Push 2
              |                |
              └───────┬────────┘
                      |
                    Redis
                      |
                Application
                      |
                  PostgreSQL
```

Possible choices:

- Two or more push servers.
- Redis for distributed connection ownership.
- PostgreSQL/MySQL for business data.
- A lightweight message broker or even direct internal routing.
- SSE or WebSocket depending on application behavior.

We might not need Kafka.

We might not need a separate delivery database.

We might not need regional routing.

The most important things are:

- Correct connection ownership.
- Reconnection handling.
- Bounded buffers.
- Basic monitoring.
- Recovery from server restarts.

## 30. Stage 2: 1 million concurrent connections

Now we need stronger horizontal scaling.

```
                    Load Balancer
                         |
         ┌───────────────┼───────────────┐
         |               |               |
         ▼               ▼               ▼
      Push Fleet     Push Fleet     Push Fleet
        A               B               C
         |               |               |
         └───────────────┼───────────────┘
                         |
                  Redis Cluster
                         |
                    Router Fleet
                         |
                    Message Broker
                         |
                    Business Services
```

We now introduce:

- Multiple Redis shards or a suitable cluster.
- Kafka or another scalable event pipeline, if event throughput justifies it.
- Dedicated routing workers.
- Separate delivery workers where needed.
- Proper admission control.
- Connection draining.
- Per-connection backpressure.

At this stage, I would start measuring fanout and outbound bandwidth very carefully.

A million connections alone doesn't tell us how expensive the system is.

## 31. Stage 3: 10 million concurrent connections

Now the system needs more deliberate workload separation.

```
                       Global Entry
                            |
                            ▼
                     Traffic Manager
                            |
             ┌──────────────┼──────────────┐
             |              |              |
             ▼              ▼              ▼
          Push Region A  Push Region B  Push Region C
             |              |              |
             ▼              ▼              ▼
         Redis Shard    Redis Shard    Redis Shard
             |              |              |
             └──────────────┼──────────────┘
                            |
                      Regional Routers
                            |
                     Kafka Clusters
                            |
                       Fanout Jobs
                            |
                     Delivery Workers
                            |
                    Durable Delivery Store
```

Now consider:

- Regional connection ownership.
- Regional event processing.
- Regional group interest registries.
- Hierarchical fanout.
- Independent capacity planning for each workload.
- Dedicated pools for massive broadcasts.
- Automated recovery and reconciliation.
- Multi-region operational monitoring.

You should not assume one global Redis deployment and one global message broker must handle everything.

The infrastructure should have locality.

## 32. Stage 4: 100 million concurrent connections

At this scale, we're designing a distributed real-time infrastructure rather than merely scaling a WebSocket server.

Conceptually:

```
                         GLOBAL PLATFORM
                                |
                   ┌────────────┼────────────┐
                   |            |            |
                   ▼            ▼            ▼
                REGION 1     REGION 2     REGION 3
                   |            |            |
           ┌───────┼──────┐     ...     ┌─────┼──────┐
           |       |      |             |     |      |
           ▼       ▼      ▼             ▼     ▼      ▼
         Zone A  Zone B  Zone C       Zone A Zone B Zone C
           |       |      |             |     |      |
           ▼       ▼      ▼             ▼     ▼      ▼
        Push Fleets and Regional Connection Registries
                   |                     |
                   ▼                     ▼
             Regional Routing       Regional Routing
                   |                     |
                   ▼                     ▼
             Regional Kafka        Regional Kafka
                   |                     |
                   └──────────┬──────────┘
                              |
                      Global Event Routing
```

At this level:

- Each region has local connection infrastructure.
- Each region has a local connection registry.
- Routing is hierarchical.
- Event ingestion is partitioned.
- Fanout is distributed and checkpointed.
- Business data has explicit ownership and replication rules.
- Failure domains are isolated.
- No single component must maintain all global connection details.

A single massive flat Pub/Sub topology is generally not the architecture I'd choose.

The system needs hierarchy and locality.

### Important qualification

These four stages are architectural maturity examples, not fixed thresholds.

A system with 100,000 connections and enormous broadcast frequency might need sophisticated fanout infrastructure sooner than a system with 10 million mostly idle connections.

Traffic shape matters more than the connection count alone.

# Part J — A Practical Capacity Planning Example

## 33. Let's perform a small end-to-end estimation

Suppose we're building a real-time notification platform.

Requirements:

```
Concurrent connections: 2 million

Average connections per push server:
    Target 50,000

Average outgoing message:
    400 bytes

Average delivery frequency:
    0.2 messages/sec per connected user
```

These are hypothetical values for learning the estimation process.

### Step 1: Number of push servers

Assume 50,000 connections per server is a tested safe operating target.

\\[ N = \frac{2,000,000}{50,000} \\]

\\[ N = 40 \\]

Now add capacity headroom.

Suppose we want the system to tolerate losing one availability zone or a meaningful subset of instances.

We might deploy more than 40 instances.

For example, 48–60, depending on failure distribution and measured capacity.

### Step 2: Total event delivery rate

\\[ 2,000,000 \times 0.2 \\]

\\[ = 400,000 \text{ messages/sec} \\]

This is the total message delivery rate under our assumed uniform distribution.

### Step 3: Outbound payload bandwidth

\\[ 400,000 \times 400 = 160,000,000 \text{ bytes/sec} \\]

Approximately:

```
160 MB/sec

1.28 Gbps
```

before protocol and transport overhead.

### Step 4: Average messages per push server

With 40 servers:

\\[ \frac{400,000}{40} = 10,000 \\]

Each server handles approximately 10,000 outgoing messages per second on average.

But this assumes an even distribution.

A hot group might cause one server to receive much more traffic than others.

Therefore, we need to measure skew.

### Step 5: Router capacity

The router doesn't necessarily process all 400,000 socket messages individually.

It may receive:

```
200,000 business events/sec
```

Some events might have multiple recipients.

Others might be routed to several connections.

We need separate measurements for:

- Domain events per second.
- Recipient deliveries per second.
- Push-server dispatch requests per second.
- WebSocket frames per second.

These are four different throughput numbers.

### Step 6: Redis operations

Suppose we perform:

```
Connection creation/deletion:
    20,000 operations/sec

Connection lookups:
    100,000 lookups/sec

Lease refresh:
    Batched server-level leases
```

We can estimate the Redis requirement from the actual operation mix and test the cluster accordingly.

Don't simply divide total traffic by the number of Redis nodes and assume uniform distribution.

Key skew and large recipient sets can create hotspots.

### Step 7: Test failure capacity

Suppose 48 push servers operate normally.

Can the remaining servers handle the workload if one availability zone fails?

Calculate the remaining capacity under the failure scenario.

For example, with three equally sized availability zones and 16 servers each:

```
Normal:
    16 + 16 + 16 = 48

One zone down:
    16 + 16 = 32
```

If those 32 servers cannot handle the expected connection and message load, we don't have sufficient zone-failure capacity.

We must either:

- Add more reserve capacity.
- Reduce the expected failover load.
- Apply admission control.
- Define a degraded mode.
- Use an appropriate combination.

Capacity planning must include failure conditions, not only normal operation.

# Part K — Common Scaling Mistakes

## 34. Things I would avoid

| Mistake                                       | Why it becomes a problem                              | Better approach                                       |
| --------------------------------------------- | ----------------------------------------------------- | ----------------------------------------------------- |
| Scaling push servers only by CPU              | Memory, sockets, and network might be exhausted first | Multi-dimensional capacity metrics                    |
| One global Redis cluster for everything       | Cross-region latency and shared failure domain        | Regional registries                                   |
| Broadcasting every event to every push server | Wastes network and processing                         | Audience-aware routing                                |
| One topic per group/user                      | Excessive operational overhead                        | Shared topics with partition keys                     |
| One Kafka partition for everything            | Limits parallelism                                    | Partition by ordering boundary                        |
| Increasing partitions to fix a hot key        | A single hot key can still overload one partition     | Special handling for hot entities                     |
| One worker handles massive broadcasts         | Blocks smaller workloads                              | Hierarchical fanout and batching                      |
| Unbounded fanout                              | Can overload downstream infrastructure                | Rate limits, checkpoints, backpressure                |
| Scaling using connection count alone          | Ignores event frequency and payload size              | Measure connections and message throughput separately |
| Centralizing every region's live traffic      | Adds cross-region latency and cost                    | Regional processing and hierarchical routing          |
| Retrying every failure immediately            | Creates retry storms                                  | Backoff, jitter, retry budgets                        |
| Running servers at maximum capacity           | No resilience during failures                         | Reserve capacity and admission control                |

# Part L — The Architecture Decision Framework

## 35. How should you approach scaling a real-world problem?

When someone gives you a requirement such as:

> Design a real-time messaging system for 50 million users.

Don't immediately start drawing 500 WebSocket servers and Kafka.

Start with these questions.

### 1. Connection workload

- How many registered users?
- How many concurrent users?
- How many devices per user?
- How many simultaneous connections?
- How frequently do users connect and disconnect?
- What is the expected connection lifetime?

This determines push-server capacity.

### 2. Event workload

- How many events per second?
- Average payload size?
- What percentage of events are global?
- What percentage are group-based?
- What percentage are individual?
- How many recipients does each event have?

This determines the messaging and fanout workload.

### 3. Delivery semantics

- Can events be lost?
- Must offline messages be recovered?
- Do clients need to ACK?
- Is ordering required?
- How long should messages be retained?

This determines the durable delivery infrastructure.

### 4. Geographic requirements

- Where are users located?
- Does the application need regional availability?
- Does data need to stay within a particular region?
- Can users reconnect in another region?
- What is the cross-region latency budget?

This determines regional architecture.

### 5. Failure requirements

- Should one server failure be transparent?
- How many availability zones must the system survive?
- Should a region failure cause an outage or a degraded experience?
- How quickly must the system recover?

This determines reserve capacity, replication, failover, and recovery mechanisms.

## 36. The final architecture we have built

Let's bring the entire system together.

\#chatgpt-mermaid-\_r_b38\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_b38\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_b38\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_b38\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_b38\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_b38\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_b38\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_b38\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_b38\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_b38\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_b38\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_b38\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_b38\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_b38\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_b38\_ p{margin:0;}#chatgpt-mermaid-\_r_b38\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_b38\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_b38\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_b38\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_b38\_ .label text,#chatgpt-mermaid-\_r_b38\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_b38\_ .node rect,#chatgpt-mermaid-\_r_b38\_ .node circle,#chatgpt-mermaid-\_r_b38\_ .node ellipse,#chatgpt-mermaid-\_r_b38\_ .node polygon,#chatgpt-mermaid-\_r_b38\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_b38\_ .rough-node .label text,#chatgpt-mermaid-\_r_b38\_ .node .label text,#chatgpt-mermaid-\_r_b38\_ .image-shape .label,#chatgpt-mermaid-\_r_b38\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_b38\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_b38\_ .rough-node .label,#chatgpt-mermaid-\_r_b38\_ .node .label,#chatgpt-mermaid-\_r_b38\_ .image-shape .label,#chatgpt-mermaid-\_r_b38\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_b38\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_b38\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_b38\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_b38\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_b38\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_b38\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_b38\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_b38\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_b38\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_b38\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_b38\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_b38\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_b38\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_b38\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_b38\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_b38\_ .icon-shape,#chatgpt-mermaid-\_r_b38\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_b38\_ .icon-shape p,#chatgpt-mermaid-\_r_b38\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_b38\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_b38\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_b38\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_b38\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_b38\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_b38\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_b38\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_b38\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_b38\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_b38\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_b38\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_b38\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_b38\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_b38\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_b38\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_b38\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_b38\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_b38\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_b38\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_b38\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_b38\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_b38\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_b38\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_b38\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_b38\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_b38\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_b38\_ .node rect,#chatgpt-mermaid-\_r_b38\_ .node circle,#chatgpt-mermaid-\_r_b38\_ .node ellipse,#chatgpt-mermaid-\_r_b38\_ .node polygon,#chatgpt-mermaid-\_r_b38\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_b38\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_b38\_ .mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_b38\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_b38\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_b38\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}USEuropeIndiaUS RegionRegional Load BalancerPush FleetRegional Connection RegistryLocal ClientsRegional KafkaRegional RoutersDelivery StoreDelivery WorkersEurope RegionRegional Load BalancerPush FleetRegional Connection RegistryLocal ClientsRegional KafkaRegional RoutersDelivery StoreDelivery WorkersIndia RegionRegional Load BalancerPush FleetRegional Connection RegistryLocal ClientsRegional KafkaRegional RoutersDelivery StoreDelivery WorkersGlobal ClientsGlobal Traffic ManagerBusiness ServicesAuthoritative BusinessDatabasesTransactional OutboxesRegional / Global EventRouting

This is a reference architecture for a large multi-region deployment.

In an actual system, we may combine services, use fewer databases, or choose a different regional event-distribution mechanism.

The architectural principles remain the same.

# 37. The Most Important Scaling Principles

Let's summarize the concepts that matter most.

1\. Scale each workload independently.

Connections, event processing, recipient resolution, fanout, and delivery are different workloads.

2\. Scale by actual traffic, not just user count.

Two systems with one million connections can have completely different resource requirements.

3\. Keep routing hierarchical.

For large systems:

```
Global routing
    |
    ▼
Regional routing
    |
    ▼
Push-server routing
    |
    ▼
Local connection
```

4\. Minimize unnecessary fanout.

Send messages only to relevant users or push servers.

5\. Partition according to ordering requirements.

A conversation ID, document ID, or order ID can be a useful partition key.

6\. Isolate large workloads.

A massive global broadcast should not block an individual payment notification.

7\. Design capacity for failures.

Servers must have enough headroom to absorb expected failure scenarios.

8\. Keep business data close to its authoritative owner.

Regional push infrastructure doesn't automatically require globally distributed business databases.

9\. Prefer regional locality.

Most connections, lookups, and fanout operations should remain inside their region when possible.

10\. Avoid unnecessary complexity.

A system with 10,000 connections probably doesn't need the same infrastructure as one with 100 million connections.

# Chapter 9 — Final Mental Model

The biggest conceptual shift is this:

Initially, we thought the scaling problem was:

> How do we handle millions of WebSocket connections?

Now we understand that the actual problem is much broader.

We need to scale:

\\[ \begin{aligned} &\text{Concurrent Connections}\\\ +&\text{Connection Registry}\\\ +&\text{Event Ingestion}\\\ +&\text{Audience Resolution}\\\ +&\text{Fanout Throughput}\\\ +&\text{Network Bandwidth}\\\ +&\text{Durable Delivery} \end{aligned} \\]

And each has its own scaling strategy.

The most useful principle to remember:

Keep connections local, business data authoritative, routing hierarchical, fanout bounded, and delivery independently recoverable.

# Next: Chapter 10 — Complete Production Architectures for Real-World Applications

Now let's apply everything we've learned.

Instead of continuing to study isolated concepts, we'll take actual application requirements and derive their architectures.

We'll design:

1. Group chat application: Messages, multiple devices, delivery receipts, read receipts, offline synchronization.
2. Collaborative document editor: Active document rooms, ordered operations, OT/CRDT considerations, snapshots, reconnect recovery.
3. Live sports application: Match-specific subscriptions, massive broadcast, snapshots, coalescing, regional fanout.
4. Stock market application: High-frequency updates, subscription filtering, slow consumers, latest-state delivery.
5. Notification platform: User/device targeting, durable inbox, retry policies, ACKs, multiple channels.
6. Online gaming: Low-latency bidirectional communication, room ownership, state synchronization, regional routing.
7. Live tracking application: Location updates, user-specific subscriptions, geo-based routing, update frequency control.
8. Global dashboard: Tenant-specific subscriptions, selective fanout, aggregation and backpressure.

For every application, we'll answer:

- Why this architecture?
- Which parts are reused?
- What changes compared to our core architecture?
- Where do Kafka and Pub/Sub fit?
- What should be persisted?
- Which events can be dropped?
- What needs retries and acknowledgements?
- How do we scale it?
- What are the tradeoffs?

The goal of the next chapter is to move from understanding components to being able to design the entire architecture independently from the requirements.
