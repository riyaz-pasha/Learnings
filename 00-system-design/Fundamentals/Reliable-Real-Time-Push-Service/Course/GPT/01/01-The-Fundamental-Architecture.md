# Reliable Real-Time Message Delivery — From First Principles to Production Architecture

Let's build this as an evolving system rather than starting with a complicated architecture and memorizing it.

We'll start with the simplest possible real-time communication system. Then, as new requirements appear, we'll introduce the components and patterns that solve them.

The most important thing to understand from the beginning is:

WebSocket/SSE, Pub/Sub, Kafka, message queues, connection registries, and databases solve different problems. They are not interchangeable components.

A reliable real-time delivery system combines them according to the guarantees the application needs.

## Learning roadmap

We'll cover this in chapters, one at a time.

| Chapter                               | What we'll understand                                                                                                    |
| ------------------------------------- | ------------------------------------------------------------------------------------------------------------------------ |
| 1. The fundamental architecture       | What a push service actually does, connection ownership, routing, and the overall mental model                           |
| 2. WebSocket vs SSE                   | How connections work, lifecycle, heartbeats, reconnects, authentication, and backpressure                                |
| 3. Routing messages by audience       | Global broadcasts, groups, individual users, devices, and conversations                                                  |
| 4. Pub/Sub vs Kafka vs Message Queues | Why each exists, when to use them, topic design, partitions, subscriptions, and avoiding topic explosion                 |
| 5. Connection registry and routing    | How to locate users across thousands of push servers, data structures, Redis, sharding, and stale connections            |
| 6. Reliable delivery                  | Durability, acknowledgements, retries, duplicates, ordering, idempotency, and delivery guarantees                        |
| 7. Failure handling                   | Push server crashes, broker failures, disconnected clients, slow consumers, network partitions, and recovery             |
| 8. Storage and database design        | What to persist, what to keep in memory, database choices, and how to manage database operations                         |
| 9. Scaling the system                 | Fanout, partitioning, hot users, hot groups, multi-region architecture, and capacity planning                            |
| 10. Production architecture patterns  | Complete architectures for chat, collaborative editing, notifications, live scores, feeds, dashboards, and other systems |
| 11. Design decision framework         | How to derive the architecture from requirements in a system design interview or real implementation                     |

Let's begin.

# Chapter 1 — The Fundamental Architecture

## 1. First understand the actual problem

Imagine we have an application.

A user opens the application and wants to receive real-time updates.

For example:

- A chat application wants to show new messages.
- A stock trading application wants to show price updates.
- A collaborative editor wants to show another user's changes.
- A delivery application wants to show live delivery status.
- A sports application wants to show live scores.

Without real-time communication, the client would have to repeatedly ask the server:

```
Are there any new messages?

Are there any new messages?

Are there any new messages?
```

This is polling.

Instead, we want the server to notify the client whenever something happens.

That's the responsibility of a push system.

The first important distinction:

The push system is responsible for maintaining communication channels and delivering data through them. It is not necessarily responsible for deciding what the data means or whether the underlying business operation is valid.

Let's understand why.

## 2. Build the simplest possible system

Suppose we have:

- 1,000 users.
- 1 application server.
- WebSocket connections.

Each user establishes a WebSocket connection.

```
User 1  ───── WebSocket ─────┐
User 2  ───── WebSocket ─────┤
User 3  ───── WebSocket ─────┤
                             │
                         Push Server
                             │
User 1000 ── WebSocket ──────┘
```

The push server maintains these connections.

When a new event arrives, the server writes data to the appropriate socket.

For example:

```
connections.get(userId).send(message);
```

Conceptually, this is enough.

But there are several problems.

Problem 1: The application has only one push server.

What happens when 1 million users connect?

One server cannot maintain an unlimited number of connections.

Problem 2: What happens when we add more push servers?

Now we have multiple servers, and a user's connection might be established on any one of them.

Problem 3: How does another service send a message to that user?

A business service cannot simply call a local socket because the socket may belong to another server.

These problems lead us to the architecture.

## 3. Introduce multiple push servers

Let's scale horizontally.

```
                         Load Balancer
                        /      |      \
                       /       |       \
                      ▼        ▼        ▼
                  Push 1    Push 2    Push 3
```

When users establish connections:

```
User A ───────────────► Push Server 1

User B ───────────────► Push Server 2

User C ───────────────► Push Server 1

User D ───────────────► Push Server 3

User E ───────────────► Push Server 2
```

Each push server maintains its own connections.

For example:

Push Server 1

```
User A -> Connection A
User C -> Connection C
```

Push Server 2

```
User B -> Connection B
User E -> Connection E
```

Push Server 3

```
User D -> Connection D
```

This introduces the most fundamental concept in push architectures:

## Connection ownership

A WebSocket/SSE connection belongs to exactly one push server at a time.

Why?

Because the underlying TCP connection is established with a particular server endpoint.

Once the connection is established, that server owns the socket.

Another push server cannot directly write to that socket.

This is true even if both servers have access to the same database.

A database can tell us where a connection lives. It cannot magically transfer ownership of an open TCP connection.

### What does the load balancer do?

The load balancer handles the initial connection request.

```
Client
  |
  | GET /stream
  |
  ▼
Load Balancer
  |
  | Selects Push Server 2
  |
  ▼
Push Server 2
  |
  | Establishes WebSocket connection
  |
  ▼
Connection remains on Push Server 2
```

The important point:

We don't need to send every outgoing message through the load balancer.

Once the connection is established, Push Server 2 can write directly to its locally owned socket.

However, there's one nuance:

If the load balancer or reverse proxy is operating as a TCP/HTTP proxy, it remains in the network path of the established connection. It doesn't need to make a new backend-routing decision for every message.

Also, we generally don't want to move an existing connection from Push Server 2 to Push Server 3. When that connection breaks, the client reconnects, and the load balancer can select a new server.

## 4. How does a business service send a message?

Now imagine Service A wants to send a message to User A.

```
Service A
   |
   | Send message to User A
   |
   ▼
    ?
```

Where should it send the message?

It doesn't know which push server owns User A's connection.

Calling every push server would work, but it is wasteful.

Imagine:

- 100,000 users are connected.
- 100 push servers exist.
- Service A wants to message one user.

Broadcasting that event to all 100 push servers means 99 servers receive an event they cannot deliver.

We need a way to find the connection owner.

This introduces our next component.

## 5. Introduce the Connection Registry

A Connection Registry maintains the mapping between users and their active connections.

Conceptually, it answers:

> Which push server currently owns the connection for this user?

For example:

| User ID | Connection ID | Push Server |
| ------- | ------------- | ----------- |
| User A  | Conn-101      | Push-1      |
| User B  | Conn-102      | Push-2      |
| User C  | Conn-103      | Push-1      |
| User D  | Conn-104      | Push-3      |

Now Service A (or a routing component acting on its behalf) can locate User A's push server.

```
                  Connection Registry
                  /       |        \
                 /        |         \
              User A    User B     User C
                |         |          |
              Push 1    Push 2     Push 1
```

The registry doesn't contain the actual WebSocket connection.

It contains metadata about where the connection lives.

The actual socket remains in the memory of its owning push server.

### What information should we maintain?

At the simplest level:

```
userId
    -> connectionId
    -> pushServerId
```

But there are important details.

A user can have:

- Multiple browser tabs.
- A mobile application and a web application.
- Multiple devices.
- Multiple active sessions.

Therefore, we should not assume:

```
One user = One connection
```

A better representation is:

```
User A
  |
  ├── Connection 101 -> Push Server 1
  |
  ├── Connection 102 -> Push Server 2
  |
  └── Connection 103 -> Push Server 1
```

Our connection registry should support:

```
userId -> Set<ConnectionMetadata>
```

Where connection metadata contains something like:

```
connectionId
pushServerId
deviceId
connectedAt
lastHeartbeatAt
connectionStatus
```

We may also maintain tenant, session, protocol, and other information depending on the application.

### Where should this registry live?

A common choice is Redis or another distributed, low-latency key-value store.

Why?

Because connection information:

- Is frequently updated.
- Is frequently looked up.
- Is ephemeral.
- Must be accessible across all push servers.
- Needs to disappear when a connection becomes stale.

We will examine this properly in Chapter 5.

For now, remember:

The connection registry is a directory of active connections, not the storage for business messages.

This distinction will become extremely important later.

## 6. Our first complete architecture

We now have enough components to establish the basic architecture.

```
                   ┌───────────────────┐
                   │   Load Balancer   │
                   └─────────┬─────────┘
                             │
                  ┌──────────┼──────────┐
                  │          │          │
                  ▼          ▼          ▼
              ┌────────┐ ┌────────┐ ┌────────┐
              │ Push 1 │ │ Push 2 │ │ Push 3 │
              └────────┘ └────────┘ └────────┘
                  │          │          │
                  └──────────┼──────────┘
                             │
                      Connection Registry
```

The flow is:

When a user connects:

1. Client requests `/stream`.
2. Load balancer selects a push server.
3. Push server establishes the connection.
4. Push server records the connection locally.
5. Push server registers its connection metadata in the connection registry.

When a user disconnects:

1. Push server detects the disconnect.
2. Push server removes its local connection.
3. Push server removes or expires the corresponding registry entry.

We now have a way to locate active users.

But we still have a problem.

What happens when Service A wants to send a message?

Let's evolve the architecture.

## 7. Introduce the messaging backbone

Imagine Service A wants to send a message to User A.

Instead of directly communicating with a particular push server, it publishes an event to a messaging backbone.

```
Service A
    |
    | Publish event
    ▼
Messaging Backbone
    |
    ▼
Routing Component
    |
    | Find User A's connection owner
    ▼
Connection Registry
    |
    ▼
Push Server 1
    |
    ▼
User A
```

This introduces another important separation:

| Component           | Responsibility                        |
| ------------------- | ------------------------------------- |
| Business service    | Decides that something should be sent |
| Messaging backbone  | Transfers events between components   |
| Routing component   | Determines where the event should go  |
| Connection registry | Identifies the owning push server     |
| Push server         | Writes the message to the connection  |
| Client              | Receives and processes the message    |

Notice that we have separated business decisions from transport delivery.

For example:

An order service knows that an order has shipped.

It publishes:

```
{
  "eventId": "evt-1001",
  "type": "ORDER_SHIPPED",
  "recipient": {
    "type": "USER",
    "id": "user-A"
  },
  "payload": {
    "orderId": "order-123",
    "status": "SHIPPED"
  }
}
```

The push infrastructure doesn't need to understand the business meaning of `ORDER_SHIPPED`.

It needs to understand:

- Who should receive the event?
- Where is the recipient connected?
- How should the event be delivered?

This makes the push infrastructure reusable across many business domains.

### Important: The routing component doesn't have to be a separate microservice

It can be:

- Logic inside the push servers.
- A separate routing service.
- A set of partitioned message consumers.
- A combination of these.

The right choice depends on scale, reliability requirements, and the messaging infrastructure.

We'll compare the alternatives later.

# 8. A critical distinction: Three different kinds of state

Before moving forward, we need to distinguish three types of state.

This is one of the most important mental models for designing a production-grade system.

### A. Connection state

Answers:

> Which users are currently connected, and which push server owns their connections?

Example:

```
User A -> Push Server 1
User B -> Push Server 2
User C -> Push Server 1
```

This state is:

- Ephemeral.
- Frequently updated.
- Primarily used for routing.
- Usually kept in memory on push servers, with distributed metadata in a registry.

Redis is a common choice for the distributed part.

### B. Business state

Answers:

> What is the actual state of the application?

For example:

```
Order 123 -> SHIPPED

Document 456 -> Current document version 109

Conversation 789 -> Latest message ID 501
```

This belongs to the application's business services and their databases.

The push server should not become the authoritative owner of this information.

### C. Delivery state

Answers:

> Has a particular message been delivered, acknowledged, retried, or expired?

Example:

```
Message 101:
    Status: PENDING

Message 102:
    Status: ACKNOWLEDGED

Message 103:
    Status: RETRYING
```

Whether we need to maintain this state depends on our delivery requirements.

For a live sports score, we might not need to record every delivery.

For a financial transaction notification or chat message, we may need durable storage and recovery.

We'll build this properly in Chapters 6–8.

Here's the distinction:

| State            | Example                        | Typical owner                       |
| ---------------- | ------------------------------ | ----------------------------------- |
| Connection state | User A is on Push-1            | Push infrastructure / registry      |
| Business state   | Order is shipped               | Business service / database         |
| Delivery state   | Message acknowledged by client | Messaging / delivery infrastructure |

Never confuse an active connection with a successfully delivered message.

A user being connected doesn't mean that the user's application has received, processed, or persisted the message.

# 9. What exactly should a push server do?

A natural question is whether the push server should also perform business operations.

Consider this example:

A client sends a message through its WebSocket connection.

Should the push server:

1. Validate the chat message?
2. Check whether the user belongs to the group?
3. Persist the message?
4. Update the conversation's latest message?
5. Notify the other participants?
6. Maintain connections and deliver messages?

There are two possible designs.

Design A: Push server handles everything.

It becomes a combination of:

- API server.
- Business service.
- Database client.
- Message processor.
- Connection manager.
- Delivery engine.

This can work for small applications, but it tightly couples business logic with connection management.

Design B: Push server focuses on connection management and transport.

```
                       Push Server
                            |
                       Incoming request
                            |
                            ▼
                     Business Service
                            |
                       Validate & Persist
                            |
                            ▼
                      Publish event
                            |
                            ▼
                       Push System
```

This creates a cleaner separation.

My default production recommendation is:

Keep the push gateway primarily responsible for connections, transport, routing, and delivery mechanics. Let domain services own business rules and authoritative data.

There are exceptions. For example, a real-time collaborative editing gateway may handle protocol-specific collaboration logic or maintain short-lived session state. But business authorization and durable document state should still have clearly defined owners.

# 10. One important correction to the initial architecture

You mentioned:

> All push servers subscribe to the channel/topic and consume events, then push the message to connected users.

This is correct for certain workloads, but it is not a universal routing strategy.

Let's examine why.

Suppose we have 100 push servers.

Service A publishes an event for User A.

If all 100 push servers subscribe to the same topic:

```
                       Topic
                         |
           ┌─────────────┼─────────────┐
           │             │             │
           ▼             ▼             ▼
        Push 1         Push 2        Push 3
           │             │             │
           ...           ...           ...
           │             │             │
           ▼             ▼             ▼
        Push 100
```

All 100 servers receive the event.

Only Push Server 1 owns User A's connection.

The remaining 99 servers receive useless traffic.

This becomes expensive at scale.

Therefore, the architecture needs to consider the audience of each event.

Let's establish the basic categories now. We'll design each one in detail in Chapter 3.

| Audience                  | Example                         | Fundamental routing idea                              |
| ------------------------- | ------------------------------- | ----------------------------------------------------- |
| Everyone                  | Global live score               | Broadcast to all relevant push servers                |
| A selected group          | Group chat, shared document     | Deliver to servers hosting members of the group       |
| Individual user           | Personal notification           | Find the user's active connections                    |
| Individual device/session | Notification to one browser tab | Find the exact connection                             |
| No active connection      | User is offline                 | Store for later or discard, depending on requirements |

These are different routing problems.

They should not automatically use the same Pub/Sub architecture.

# 11. The architecture we have built so far

Let's bring everything together.

\#chatgpt-mermaid-\_r_4ud\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_4ud\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_4ud\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_4ud\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_4ud\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4ud\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_4ud\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_4ud\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_4ud\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_4ud\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_4ud\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_4ud\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4ud\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4ud\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_4ud\_ p{margin:0;}#chatgpt-mermaid-\_r_4ud\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4ud\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4ud\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4ud\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_4ud\_ .label text,#chatgpt-mermaid-\_r_4ud\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4ud\_ .node rect,#chatgpt-mermaid-\_r_4ud\_ .node circle,#chatgpt-mermaid-\_r_4ud\_ .node ellipse,#chatgpt-mermaid-\_r_4ud\_ .node polygon,#chatgpt-mermaid-\_r_4ud\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_4ud\_ .rough-node .label text,#chatgpt-mermaid-\_r_4ud\_ .node .label text,#chatgpt-mermaid-\_r_4ud\_ .image-shape .label,#chatgpt-mermaid-\_r_4ud\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_4ud\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_4ud\_ .rough-node .label,#chatgpt-mermaid-\_r_4ud\_ .node .label,#chatgpt-mermaid-\_r_4ud\_ .image-shape .label,#chatgpt-mermaid-\_r_4ud\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_4ud\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_4ud\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4ud\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4ud\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_4ud\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_4ud\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_4ud\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_4ud\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_4ud\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_4ud\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_4ud\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4ud\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4ud\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_4ud\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4ud\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_4ud\_ .icon-shape,#chatgpt-mermaid-\_r_4ud\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_4ud\_ .icon-shape p,#chatgpt-mermaid-\_r_4ud\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_4ud\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_4ud\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_4ud\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_4ud\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_4ud\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_4ud\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_4ud\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_4ud\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_4ud\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4ud\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_4ud\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_4ud\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_4ud\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4ud\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_4ud\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_4ud\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4ud\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_4ud\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_4ud\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4ud\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_4ud\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4ud\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_4ud\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_4ud\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_4ud\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_4ud\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_4ud\_ .node rect,#chatgpt-mermaid-\_r_4ud\_ .node circle,#chatgpt-mermaid-\_r_4ud\_ .node ellipse,#chatgpt-mermaid-\_r_4ud\_ .node polygon,#chatgpt-mermaid-\_r_4ud\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_4ud\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_4ud\_ .mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_4ud\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_4ud\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4ud\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}ClientLoad BalancerPush Server 1Push Server 2Push Server 3Connection RegistryBusiness Service AMessaging BackboneRouting ComponentRegister / Refresh / LookupRegister / Refresh / LookupRegister / Refresh / Lookup

This is our foundational architecture.

It establishes the responsibilities and relationships between the components.

But it deliberately leaves several questions unanswered:

- What if a user doesn't respond to heartbeats?
- What if a push server crashes?
- What if a message arrives while a user is reconnecting?
- What if two push servers temporarily believe they own the same user?
- How do we deliver messages to a group without broadcasting them to everybody?
- Should every push server subscribe to Kafka?
- Should we create one Kafka topic per user or group?
- What happens if a push server receives a message but the client never processes it?
- How do we retry without producing duplicate messages?

These are precisely the problems the next chapters will solve.

## Chapter 1 — What you should now understand

You should be able to explain these five ideas without memorizing an architecture:

1. Connections have owners. A particular push server owns an established WebSocket/SSE connection.
2. The load balancer establishes the initial route. The established connection stays associated with the selected server until it disconnects.
3. A connection registry is a directory. It maps users and connections to their owning push servers; it does not store the actual sockets.
4. The messaging backbone and connection registry solve different problems. One transports events; the other tells us where active connections live.
5. The push layer is not automatically the business layer. Business services own business state, while push infrastructure manages real-time transport and delivery.

The most important mental model:

> A push system is a distributed network of connection owners. When an event needs to reach a client, the system must first determine the intended audience, then locate the servers owning those clients' connections, and finally deliver the event through those connections.

Keep this model in mind. Every architecture we build from now on will be an evolution of it.

Next: Chapter 2 — WebSocket vs SSE: How connections actually work.

We'll understand connection establishment, long-lived TCP connections, heartbeats, ping/pong, idle timeouts, reconnects, authentication, multiple devices, backpressure, and what happens when clients stop responding.
