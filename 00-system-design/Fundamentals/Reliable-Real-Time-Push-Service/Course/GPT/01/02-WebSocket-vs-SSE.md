# Chapter 2 — WebSocket vs SSE: How Connections Actually Work

In Chapter 1, we established that a push server owns the connection and maintains a reference to it.

Now let's understand what that connection actually is.

We need to answer some fundamental questions:

- How is a connection established?
- What happens after the connection is established?
- How does the server know the client is still alive?
- What if the client suddenly loses internet connectivity?
- What if the client stops responding but the TCP connection hasn't closed?
- How does reconnection work?
- What happens when a push server is restarted?
- How do we prevent slow clients from consuming all our server memory?

These questions are more important than simply choosing WebSocket or SSE.

Let's build the understanding from first principles.

# 1. First understand the communication requirement

Imagine a typical application.

The client and server need to communicate.

There are three possible communication models.

### Model A: Request/Response (HTTP)

```
Client                         Server
  |                               |
  |-------- Request -------------->|
  |                               |
  |<------- Response --------------|
  |                               |
  |         Connection ends       |
```

The client initiates communication.

The server responds.

If the server wants to notify the client later, it needs another request from the client.

This is not ideal for real-time delivery.

### Model B: Polling

We can make the client repeatedly request updates.

```
Client                         Server
  |                               |
  |-------- Any update? ---------->|
  |<------- No --------------------|
  |                               |
  |-------- Any update? ---------->|
  |<------- No --------------------|
  |                               |
  |-------- Any update? ---------->|
  |<------- New event -------------|
```

This works, but creates unnecessary requests and delivery latency.

### Model C: Persistent connection

Instead, establish a connection and keep it open.

```
Client                         Server
  |                               |
  |------ Establish connection --->|
  |                               |
  |<====== Connection Open =======>|
  |                               |
  |<--------- Event 1 -------------|
  |                               |
  |<--------- Event 2 -------------|
  |                               |
  |<--------- Event 3 -------------|
  |                               |
```

Now the server can send information whenever an event arrives.

This is the fundamental concept behind WebSockets and SSE.

Both maintain long-lived HTTP-based connections, but they provide different communication capabilities.

# 2. Understand WebSockets

A WebSocket provides full-duplex communication.

This means both sides can send messages independently over the same established connection.

```
                 WebSocket Connection

Client  <==============================>  Server

        Client can send messages
        Server can send messages
        Both can send independently
```

For example, consider a chat application.

```
Alice                                  Server
  |                                       |
  |------ Connect WebSocket ------------->|
  |                                       |
  |<===== Connection Established ========>|
  |                                       |
  |------ Send chat message ------------->|
  |                                       |
  |<----- Receive another message --------|
  |                                       |
  |------ Typing indicator -------------->|
  |                                       |
  |<----- New message notification -------|
```

The server doesn't need to wait for the client to make another HTTP request.

Similarly, the client doesn't need to establish a new HTTP connection whenever it sends something.

### How is a WebSocket established?

A typical browser connection starts with an HTTP request.

```
GET /ws HTTP/1.1
Host: example.com
Upgrade: websocket
Connection: Upgrade
Sec-WebSocket-Key: ...
Sec-WebSocket-Version: 13
```

The server accepts the upgrade with a response similar to:

```
HTTP/1.1 101 Switching Protocols
Upgrade: websocket
Connection: Upgrade
Sec-WebSocket-Accept: ...
```

The connection is now upgraded to the WebSocket protocol.

The HTTP handshake is complete, but the underlying connection remains open.

The client and server can exchange WebSocket frames over it.

One important distinction:

A WebSocket is not simply an HTTP request that remains open. It becomes a different application protocol after the handshake.

The standard mechanism uses an HTTP Upgrade handshake over HTTP/1.1. HTTP/2 also supports WebSockets through extended CONNECT.

### What does this mean for our architecture?

Nothing changes about connection ownership.

```
                         Load Balancer
                               |
                               |
                         WebSocket Handshake
                               |
                               ▼
                         Push Server 1
                               |
                         Establish socket
                               |
                               ▼
                       Local Connection Map
```

The selected push server owns the resulting connection.

The server maintains a reference to the active WebSocket session.

For example:

```
Map<String, WebSocketSession> connections;
```

But remember that a user might have multiple sessions, so a more appropriate conceptual structure is:

```
Map<String, Set<WebSocketSession>> userConnections;
```

The actual implementation will depend on the WebSocket framework.

# 3. Understand SSE (Server-Sent Events)

Now consider another requirement.

Suppose we're building:

- A live notification system.
- An order tracking application.
- A live sports score dashboard.
- An application showing continuously changing server-side status.
- A monitoring dashboard.

The client mostly receives information.

It doesn't need to send frequent messages over the same connection.

Do we really need full-duplex communication?

Not necessarily.

This is where SSE comes in.

SSE provides a persistent, one-way stream from server to client over HTTP.

```
Client                         Server
  |                               |
  |-------- GET /stream ---------->|
  |                               |
  |<========= Stream Open =========|
  |                               |
  |<----------- Event 1 -----------|
  |                               |
  |<----------- Event 2 -----------|
  |                               |
  |<----------- Event 3 -----------|
  |                               |
```

The connection stays open, and the server continuously writes events.

Unlike WebSockets, SSE does not provide a bidirectional application messaging channel.

If the client needs to send data, it uses ordinary HTTP requests or another communication mechanism.

For example:

```
Client                         Server
  |                               |
  |-------- POST /action --------->|
  |<------- HTTP response ---------|
  |                               |
  |<======= SSE connection =======>|
  |                               |
  |<--------- Event 1 -------------|
  |<--------- Event 2 -------------|
```

Notice that we can use both HTTP and SSE in the same application.

### How does SSE work?

The client establishes an ordinary HTTP GET request:

```
GET /stream HTTP/1.1
Accept: text/event-stream
```

The server responds:

```
HTTP/1.1 200 OK
Content-Type: text/event-stream
Cache-Control: no-cache
```

The response body remains open.

The server writes events in a specific text format:

```
id: 101
event: ORDER_STATUS
data: {"orderId":"123","status":"SHIPPED"}

id: 102
event: ORDER_STATUS
data: {"orderId":"123","status":"DELIVERED"}
```

Each event is separated by a blank line.

The browser's `EventSource` API receives these events.

Example:

```
const eventSource = new EventSource("/stream");

eventSource.onmessage = (event) => {
    console.log(event.data);
};

eventSource.addEventListener("ORDER_STATUS", (event) => {
    console.log(JSON.parse(event.data));
});
```

One useful feature:

SSE has built-in reconnection behavior in the browser's native EventSource API.

The server can suggest a reconnection interval:

```
retry: 3000
```

This tells the client to wait approximately three seconds before reconnecting.

SSE also supports event IDs.

For example:

```
id: 105
data: {"message":"New notification"}
```

If the connection breaks and the browser reconnects, it can send:

```
Last-Event-ID: 105
```

This gives the server an opportunity to resume from that event.

But there's an important condition:

The server must have access to the events after ID 105.

The `Last-Event-ID` header does not magically make SSE durable.

We'll return to this when we study reliability.

# 4. WebSocket vs SSE — When should we choose which?

Let's compare them.

| Feature                                           | WebSocket                   | SSE                                       |
| ------------------------------------------------- | --------------------------- | ----------------------------------------- |
| Communication                                     | Bidirectional               | Server to client                          |
| Transport                                         | WebSocket frames            | HTTP response stream                      |
| Connection                                        | Persistent                  | Persistent                                |
| Client can send messages over the same connection | Yes                         | No                                        |
| Automatic browser reconnection                    | No                          | Yes, with EventSource                     |
| Built-in event IDs                                | No                          | Yes                                       |
| Built-in replay                                   | No                          | No, requires server-side support          |
| Binary data                                       | Yes                         | No, text-based                            |
| Browser support                                   | Broad                       | Broad in modern browsers                  |
| Typical use                                       | Chat, collaboration, gaming | Notifications, dashboards, status updates |

### My decision-making rule

Use SSE when:

- Communication is predominantly server-to-client.
- Client messages can use ordinary HTTP.
- You want simpler server-push semantics.
- Built-in EventSource reconnection is useful.

Use WebSockets when:

- Both parties need to communicate continuously.
- You have frequent client-to-server messages.
- You need interactive collaboration.
- You need real-time chat or multiplayer communication.

Don't select WebSockets merely because an application needs real-time updates.

SSE is frequently sufficient.

And don't assume SSE is automatically more scalable. Both approaches require long-lived connections and consume server resources.

# 5. What actually happens inside a push server?

This is particularly important for system design.

Imagine Push Server 1 has 100,000 connected clients.

Does it create 100,000 application threads?

No. A production push server normally uses asynchronous, non-blocking I/O.

Conceptually:

```
                  Push Server 1
                        |
            ┌───────────┴────────────┐
            |                        |
       Connection Manager       Event Loop(s)
            |                        |
       ┌────┼────┐                   |
       |    |    |                   |
      WS1  WS2  SSE3                 |
       |    |    |                   |
       └────┼────┴───────────────────┘
            |
       Other connections...
```

An event loop monitors network sockets and processes readiness events.

It doesn't need a dedicated thread blocked on every connection.

For example, with an event-loop-based Java framework such as Netty or a reactive WebSocket implementation, one thread can manage many connections.

### What does each connection consume?

Even without one thread per connection, a connection isn't free.

It requires resources such as:

- A file descriptor.
- Kernel socket buffers.
- Connection metadata.
- TLS state, if using TLS.
- Application-level connection state.
- Potentially an outbound message buffer.

Therefore, our capacity planning must consider more than CPU.

For example:

```
Push Server 1

Active connections:       100,000
Connection metadata:      X MB
Outbound buffers:         Y MB
Socket resources:         Z MB
```

The biggest danger is often not the connections themselves but unbounded buffers and excessive message fanout.

We'll discuss that shortly.

# 6. The most important operational concept: Heartbeats

Now let's consider a problem.

User A establishes a WebSocket connection with Push Server 1.

The connection is registered:

```
User A -> Connection 101 -> Push Server 1
```

Then User A's laptop goes to sleep.

What happens?

Does Push Server 1 immediately know that the user is gone?

Not necessarily.

The TCP connection might remain apparently established for some time.

This is called a half-open connection or a connection whose failure has not yet been detected.

Other situations produce the same problem:

- The user loses internet connectivity.
- Wi-Fi disconnects.
- A mobile device changes networks.
- A NAT gateway expires its mapping.
- A network silently drops packets.
- The client application becomes unresponsive.

The server needs a way to determine whether the connection is still healthy.

This is why we use heartbeats.

But there are different kinds of heartbeats.

## 6.1 WebSocket protocol-level ping/pong

WebSocket defines control frames called:

- Ping.
- Pong.
- Close.

A server can periodically send a ping frame:

```
Push Server                         Client
     |                                |
     |---------- PING --------------->|
     |                                |
     |<--------- PONG ----------------|
     |                                |
```

The client must respond to a WebSocket ping with a pong according to the protocol.

This lets the push server check whether the remote WebSocket endpoint is responsive.

For example:

```
Every 30 seconds:

Server sends PING.

If PONG received:
    Connection appears alive.

If PONG not received within timeout:
    Connection considered unhealthy.
```

The interval and timeout are configurable. Thirty seconds is an illustrative starting point, not a universal requirement.

Important browser detail: The native browser WebSocket API does not expose methods for sending WebSocket protocol ping frames or handling pong frames directly. The browser's WebSocket implementation handles protocol-level pong responses. JavaScript applications can implement their own application-level heartbeat messages.

For example:

```
socket.send(JSON.stringify({
    type: "HEARTBEAT"
}));
```

The server can respond:

```
{
    "type": "HEARTBEAT_ACK"
}
```

These are ordinary application messages, not WebSocket control frames.

## 6.2 Who should initiate the heartbeat?

For a push infrastructure, a common design is:

The server initiates WebSocket pings.

Why?

Because the server owns the responsibility of managing its connections.

It needs to determine whether the connection is still healthy without depending on the client voluntarily sending heartbeats.

For example:

```
Push Server:

Every 25 seconds:
    Send PING to active WebSocket connections.

If no PONG within 10 seconds:
    Mark connection unhealthy.
    Close connection.
    Remove local connection metadata.
```

Again, the exact interval and deadline must be chosen based on network conditions and intermediary timeouts.

However, browser applications may additionally need application-level heartbeat messages to verify that their own application logic is functioning.

A protocol-level pong does not prove that the client has processed your business events.

# 7. How do we handle heartbeats for SSE?

SSE is different.

There is no WebSocket ping/pong mechanism because SSE is an HTTP response stream.

The server can periodically send a comment:

```
: heartbeat
```

The browser ignores the comment as an application event.

But the periodic bytes help keep intermediaries from treating the connection as idle.

For example:

```
Push Server                         Client
     |                                |
     |------ SSE heartbeat ---------->|
     |                                |
     |------ SSE heartbeat ---------->|
     |                                |
     |------ Actual event ------------>|
     |                                |
```

This is useful for:

- Maintaining activity through proxies.
- Preventing idle timeouts.
- Detecting certain broken connections when a write fails.

But there's an important limitation:

An SSE server cannot obtain an application-level acknowledgement simply by sending a heartbeat comment.

The server might successfully write bytes into its local socket buffer without knowing whether the browser application processed them.

If actual client acknowledgement matters, use a separate HTTP endpoint.

For example:

```
POST /events/ack
{
    "consumerId": "device-123",
    "eventId": "105"
}
```

Or use a WebSocket when bidirectional application messaging is important.

# 8. Heartbeats vs TCP keepalive vs application acknowledgements

These are three different concepts.

| Mechanism               | What it checks                                                   |
| ----------------------- | ---------------------------------------------------------------- |
| TCP keepalive           | Whether a TCP peer/path appears reachable                        |
| WebSocket ping/pong     | Whether the WebSocket endpoint responds to protocol-level ping   |
| Application heartbeat   | Whether application-level heartbeat messages are processed       |
| Message acknowledgement | Whether a specific application message was processed or accepted |

Let's see why this distinction matters.

Imagine the server sends:

```
{
  "eventId": "100",
  "type": "PAYMENT_COMPLETED"
}
```

The WebSocket client responds to the protocol ping.

Does that mean the client received and processed `PAYMENT_COMPLETED`?

No!

The client might have received the ping and ignored the business event due to an application bug.

Or it might have received the event but crashed before saving it.

Therefore:

Connection health and message delivery guarantees are completely different things.

We'll develop this distinction in the reliability chapter.

# 9. What happens when the client doesn't respond?

Let's evolve our connection management.

Suppose:

```
User A -> Connection 101 -> Push Server 1
```

Push Server 1 sends a ping.

No response arrives.

What should happen?

A sensible lifecycle is:

```
ACTIVE
  |
  | Heartbeat timeout
  ▼
SUSPECTED
  |
  | Still no response after deadline
  ▼
UNHEALTHY
  |
  | Close connection
  ▼
REMOVED
```

### What does the server do?

1. Stop treating the connection as healthy.
2. Close the socket if it hasn't already closed.
3. Remove the socket from local connection maps.
4. Remove or expire the corresponding connection registry entry.
5. Update user presence if the application needs it.

The last step needs some care.

A user can have multiple connections.

If User A has three active devices and one disconnects, the user isn't necessarily offline.

Presence should generally be derived from all the user's active connections.

### What happens to the registry?

Recall:

```
User A -> Connection 101 -> Push Server 1
```

We must not let an old connection delete a newer connection.

Imagine:

1. Connection 101 disconnects.
2. User A reconnects.
3. Connection 202 gets registered.
4. A delayed cleanup operation for Connection 101 executes.

If the cleanup blindly deletes `user-A`, it might accidentally remove Connection 202.

A safer deletion is conditional:

```
Remove connection only if:

userId = User A
AND connectionId = Connection 101
AND owningServer = Push Server 1
```

This is an example of a general distributed-systems principle:

Cleanup should identify the exact resource it owns, not blindly delete whatever currently exists for the same user.

We'll explore leases, TTLs, generations, and stale ownership in Chapter 5.

# 10. What happens when the client reconnects?

Consider this sequence:

```
1. User A connected to Push Server 1.

2. User A loses internet.

3. Connection 101 becomes unhealthy.

4. User A restores internet.

5. User A reconnects.
```

The new connection might land on Push Server 3.

```
Before:

User A -> Connection 101 -> Push Server 1

After:

User A -> Connection 202 -> Push Server 3
```

This is completely normal.

Remember:

The client owns the responsibility for establishing a new connection after an unexpected disconnect.

The push server cannot transfer the broken TCP connection to another server.

### WebSocket reconnection

The browser's native WebSocket API does not automatically reconnect.

Your application needs reconnection logic.

Conceptually:

```
function reconnect(attempt) {

    const delay = Math.min(
        1000 * Math.pow(2, attempt),
        30000
    );

    const jitteredDelay = Math.random() * delay;

    setTimeout(() => {
        connectWebSocket();
    }, jitteredDelay);
}
```

The actual implementation should also:

- Reset the retry count after a stable connection.
- Stop retrying on permanent authentication failures.
- Refresh expired credentials.
- Avoid reconnecting after the user logs out.
- Avoid unlimited attempts where they make no sense.

### Why exponential backoff?

Imagine a push server crashes.

It had 100,000 connected clients.

All clients immediately attempt to reconnect.

```
Server crashes
     |
     ▼
100,000 clients disconnect
     |
     ▼
100,000 reconnect requests
     |
     ▼
Load balancer / surviving servers overloaded
     |
     ▼
More failures
```

This is a reconnection storm.

Exponential backoff with jitter spreads reconnection attempts over time.

For example:

| Attempt | Maximum backoff |
| ------- | --------------- |
| 1       | 1 second        |
| 2       | 2 seconds       |
| 3       | 4 seconds       |
| 4       | 8 seconds       |
| 5       | 16 seconds      |
| 6+      | 30 seconds      |

Random jitter ensures clients don't all retry at precisely the same moment.

### SSE reconnection

The native EventSource API automatically attempts reconnection when its connection breaks.

The application can listen for connection state changes:

```
const source = new EventSource("/stream");

source.onopen = () => {
    console.log("Connected");
};

source.onerror = () => {
    console.log("Disconnected or reconnecting");
};
```

The application can also close and recreate the connection when needed, but it doesn't have to implement all ordinary reconnection behavior itself.

# 11. A subtle but important problem: Reconnection does not mean message recovery

Suppose the client was disconnected for 30 seconds.

During that period, it missed:

```
Event 101
Event 102
Event 103
Event 104
```

After reconnecting, it is connected again.

But how does it receive those missing messages?

There are two possibilities.

### Option A: We don't care about missed events

For example, live stock prices or continuously updated dashboard values.

We might simply send the latest snapshot:

```
{
  "symbol": "ABC",
  "price": 125.50
}
```

We don't need to replay every price change that occurred while the user was offline.

### Option B: We need to recover missed events

For example:

- Chat messages.
- Important notifications.
- Transaction status updates.
- Collaboration changes where every operation matters.

We need a durable event history or a recoverable business-state mechanism.

For SSE, the browser can send `Last-Event-ID`.

For WebSocket, the client can send its last processed sequence number after reconnecting.

For example:

```
{
  "type": "RESUME",
  "lastProcessedEventId": "100"
}
```

The server can then replay events after 100, provided those events are retained somewhere.

This gives us an important principle:

> A persistent connection solves live transport. It does not solve offline message storage or replay.

We will develop this in Chapters 4 and 6.

# 12. What happens when the push server itself goes down?

Now let's move from client failures to server failures.

Suppose Push Server 1 owns 100,000 connections.

It crashes.

```
                  Load Balancer
                  /      |      \
                 ▼       ▼       ▼
              Push 1   Push 2   Push 3
                 X
              CRASHED
```

What happens?

All 100,000 connections terminate.

The clients discover the disconnection and reconnect.

The load balancer distributes new connections across healthy servers.

But there are some important operational details.

### A. Remove the unhealthy server from new traffic

The load balancer's health checks should stop directing new connection requests to Push Server 1.

### B. Clients reconnect elsewhere

They may reconnect to Push Server 2 or Push Server 3.

### C. Rebuild the connection registry

Old connections associated with Push Server 1 must eventually disappear.

If the server crashed without cleanup, the registry may contain stale records.

This is one reason why connection registry entries should generally use leases or TTLs.

### D. Re-establish subscriptions

If the push server consumes messaging infrastructure events, its new instance must establish the required subscriptions and continue processing.

### E. Recover messages that might have been missed

This depends on our delivery architecture.

A live-only broadcast system might accept some loss.

A durable notification system must recover from a persistent event source or delivery store.

We'll design both.

# 13. Graceful shutdown: Don't crash the server unnecessarily

A server restart doesn't always have to cause sudden disconnections.

Imagine we want to deploy a new version of Push Server 1.

Instead of killing it immediately:

```
Push Server 1
      |
      ▼
1. Mark server as DRAINING
      |
      ▼
2. Stop accepting new connections
      |
      ▼
3. Load balancer stops routing new connections here
      |
      ▼
4. Notify existing clients to reconnect
      |
      ▼
5. Allow connections to drain
      |
      ▼
6. Close remaining connections
      |
      ▼
7. Terminate server
```

An important detail: draining doesn't mean transferring sockets.

Clients must establish new connections to another server.

During this process:

- Existing sockets are still owned by Push Server 1.
- It must continue servicing them until they're closed.
- Registry entries must be removed safely.
- Messaging subscriptions must be closed or transferred according to the deployment strategy.

For large systems, gradual draining significantly reduces unnecessary reconnection spikes.

# 14. Another major problem: Slow clients and backpressure

This is something many basic WebSocket designs miss.

Imagine a client is connected, but its network is extremely slow.

The server continuously generates outgoing events.

```
Producer rate:  1,000 messages/second
Client capacity: 100 messages/second
```

The server generates messages faster than the client can receive them.

What happens?

An outbound buffer begins growing.

```
Push Server
    |
    ▼
Outbound Buffer
    |
    ├── Message 1
    ├── Message 2
    ├── Message 3
    ├── Message 4
    ├── Message 5
    ├── ...
    └── Message 1000000
            |
            ▼
        Slow Client
```

If we allow unlimited buffering, eventually the server can run out of memory.

This can affect every other user connected to that server.

We need backpressure.

Backpressure means controlling message production, buffering, or delivery when a consumer cannot keep up.

### Different applications need different strategies.

Scenario A: Live stock price

Suppose we receive 1,000 price updates for the same stock.

A client is slow.

Do we really need to queue all 1,000 updates?

Probably not.

We might retain only the latest price.

```
Old price updates -> Discard/coalesce

Latest price -> 125.50
```

This is called coalescing.

Scenario B: Chat messages

We cannot simply drop arbitrary messages.

We need to preserve them somewhere durable.

If the client is too slow:

1. Apply a bounded outbound buffer.
2. Stop adding unlimited messages to memory.
3. Disconnect the slow client if necessary.
4. Let it reconnect.
5. Replay the missing messages from durable storage.

Scenario C: Collaborative editing

Dropping changes arbitrarily could corrupt the client's view.

We may need operation sequencing, version checks, acknowledgements, or state synchronization.

The client may need to request a fresh snapshot if it falls too far behind.

### Production rule

Every connection should have a bounded outbound buffer.

When it reaches a threshold, the system must take an explicit action.

Possible strategies:

| Strategy                   | Suitable for                    |
| -------------------------- | ------------------------------- |
| Drop old updates           | Ephemeral telemetry             |
| Keep only latest value     | Live prices, dashboards         |
| Coalesce updates           | Frequent state changes          |
| Pause upstream consumption | When flow control is supported  |
| Persist and replay         | Chat, notifications             |
| Disconnect slow consumer   | When it cannot keep up safely   |
| Request snapshot/resync    | Collaborative state, dashboards |

Never allow a slow client to create an unlimited memory queue on a push server.

This is one of the most important production safeguards.

# 15. A critical distinction: What does "send succeeded" mean?

Let's say the push server executes:

```
session.sendMessage(message);
```

Does this mean the client received the message?

Not necessarily.

There are multiple stages:

```
Business Service
      |
      ▼
Message reaches Push Server
      |
      ▼
Push Server writes to socket
      |
      ▼
Operating system accepts bytes
      |
      ▼
Network delivers bytes
      |
      ▼
Client receives the WebSocket frame
      |
      ▼
Client application processes the message
      |
      ▼
Client persists/processes successfully
      |
      ▼
Client sends application ACK
```

These are different guarantees.

For example:

| Stage                             | What we know                                      |
| --------------------------------- | ------------------------------------------------- |
| Message accepted by push server   | Push server received it                           |
| Socket write completed            | Local transport accepted the write                |
| TCP acknowledgement               | Peer TCP stack acknowledged bytes                 |
| Client application received event | Client application obtained the event             |
| Application ACK received          | Client claims it successfully processed the event |

TCP acknowledgements are not application-level delivery acknowledgements.

This distinction will become central when we discuss durable delivery.

# 16. Authentication and authorization of persistent connections

We shouldn't allow just anyone to establish a connection and receive data.

When the client connects, we need to authenticate it.

A typical flow:

```
Client
  |
  | Connect with credentials
  ▼
Load Balancer
  |
  ▼
Push Server
  |
  | Validate authentication
  ▼
Authentication / Identity Service
  |
  | User identity + claims
  ▼
Push Server
  |
  | Establish authorized connection
  ▼
Connection Registry
```

There are a few important considerations.

### Authentication happens at connection establishment

The push server needs to know:

```
userId = user-123
tenantId = tenant-10
sessionId = session-456
```

It associates this identity with the connection.

### Authorization still matters after connection establishment

Suppose a user joins a group chat.

We should not assume that an authenticated connection is automatically authorized to receive every group's messages.

The application must authorize subscription or membership.

Similarly, if a user is removed from a document, their active subscription should be revoked or updated.

### Token expiration

A long-lived connection might outlive its original authentication token.

The system needs a policy:

- Close connections when authentication expires.
- Revalidate periodically.
- Send a controlled reauthentication message.
- Refresh credentials through an appropriate HTTP flow.

### Browser-specific consideration

Native browser WebSocket and EventSource APIs have limitations around setting arbitrary custom headers.

Common approaches include:

- Secure cookies.
- Short-lived connection tickets.
- WebSocket subprotocols where appropriate.
- Fetch-based streaming for SSE when custom headers are required.

Avoid long-lived sensitive bearer tokens in query parameters because URLs can appear in logs and monitoring systems.

Always use TLS (`wss://` and HTTPS).

Also validate the WebSocket Origin where applicable.

# 17. What changes in our push-server architecture?

Let's update the architecture based on everything we've learned.

\#chatgpt-mermaid-\_r_56k\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_56k\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_56k\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_56k\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_56k\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_56k\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_56k\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_56k\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_56k\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_56k\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_56k\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_56k\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_56k\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_56k\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_56k\_ p{margin:0;}#chatgpt-mermaid-\_r_56k\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_56k\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_56k\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_56k\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_56k\_ .label text,#chatgpt-mermaid-\_r_56k\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_56k\_ .node rect,#chatgpt-mermaid-\_r_56k\_ .node circle,#chatgpt-mermaid-\_r_56k\_ .node ellipse,#chatgpt-mermaid-\_r_56k\_ .node polygon,#chatgpt-mermaid-\_r_56k\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_56k\_ .rough-node .label text,#chatgpt-mermaid-\_r_56k\_ .node .label text,#chatgpt-mermaid-\_r_56k\_ .image-shape .label,#chatgpt-mermaid-\_r_56k\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_56k\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_56k\_ .rough-node .label,#chatgpt-mermaid-\_r_56k\_ .node .label,#chatgpt-mermaid-\_r_56k\_ .image-shape .label,#chatgpt-mermaid-\_r_56k\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_56k\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_56k\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_56k\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_56k\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_56k\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_56k\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_56k\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_56k\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_56k\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_56k\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_56k\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_56k\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_56k\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_56k\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_56k\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_56k\_ .icon-shape,#chatgpt-mermaid-\_r_56k\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_56k\_ .icon-shape p,#chatgpt-mermaid-\_r_56k\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_56k\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_56k\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_56k\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_56k\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_56k\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_56k\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_56k\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_56k\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_56k\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_56k\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_56k\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_56k\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_56k\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_56k\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_56k\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_56k\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_56k\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_56k\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_56k\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_56k\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_56k\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_56k\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_56k\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_56k\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_56k\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_56k\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_56k\_ .node rect,#chatgpt-mermaid-\_r_56k\_ .node circle,#chatgpt-mermaid-\_r_56k\_ .node ellipse,#chatgpt-mermaid-\_r_56k\_ .node polygon,#chatgpt-mermaid-\_r_56k\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_56k\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_56k\_ .mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_56k\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_56k\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_56k\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}Push ServerConnection ManagerHeartbeat ManagerBackpressure / BoundedBuffersAuthentication andAuthorizationClient: WebSocket or SSELoad BalancerPush ServerDistributed ConnectionRegistryClose and CleanupUnhealthy connectionNew connectionReconnect with backoff

Our push server now has several responsibilities.

| Component                  | Responsibility                               |
| -------------------------- | -------------------------------------------- |
| Connection Manager         | Maintain local active connections            |
| Heartbeat Manager          | Detect unhealthy connections                 |
| Authentication             | Identify the connecting user                 |
| Authorization              | Determine what the user may access           |
| Connection Registry Client | Register and refresh connection ownership    |
| Outbound Buffer Manager    | Control slow-consumer queues                 |
| Reconnection Support       | Provide appropriate close/reconnect behavior |

These are logical responsibilities. They don't necessarily need to be separate classes or microservices.

# 18. Final architecture: The connection lifecycle

Let's put the lifecycle together.

```
              CLIENT
                 |
                 | 1. Connect
                 ▼
            LOAD BALANCER
                 |
                 | 2. Route to a healthy server
                 ▼
            PUSH SERVER
                 |
                 | 3. Authenticate
                 |
                 | 4. Establish connection
                 |
                 | 5. Add to local connection map
                 |
                 | 6. Register ownership
                 ▼
         CONNECTION REGISTRY
                 |
                 ▼
          CONNECTION ACTIVE
                 |
          ┌──────┴───────┐
          |              |
      Heartbeat       Outgoing Event
          |              |
          ▼              ▼
       Healthy        Bounded Buffer
          |              |
          |              ▼
          |            Client
          |              |
          └──────┬───────┘
                 |
          Connection breaks?
                 |
          ┌──────┴──────┐
          |             |
          No            Yes
          |             |
       Continue      Cleanup / Expire
                        |
                        ▼
                  Client Reconnects
                        |
                        ▼
                  Load Balancer
                        |
                        ▼
                  New Push Server
```

# 19. Decision framework: What should we choose?

By now, you should be able to make the first set of architectural decisions.

| Requirement                                 | Design choice                                            |
| ------------------------------------------- | -------------------------------------------------------- |
| Server-to-client updates only               | SSE                                                      |
| Frequent bidirectional communication        | WebSocket                                                |
| User presence                               | Connection registry + heartbeat/leases                   |
| Detect dead WebSocket connections           | Server-side ping/pong                                    |
| Keep SSE streams active through proxies     | Periodic SSE comments                                    |
| Detect application-level message processing | Application ACK                                          |
| Handle reconnecting WebSocket clients       | Client-side exponential backoff + jitter                 |
| Handle reconnecting SSE clients             | EventSource reconnection                                 |
| Recover missed messages                     | Durable history + replay/cursor                          |
| Handle server crashes                       | Reconnect, registry expiry, message recovery as required |
| Handle slow clients                         | Bounded buffers + backpressure policy                    |
| Deploy push servers safely                  | Connection draining                                      |
| Multiple devices per user                   | Multiple connection records                              |

## Key ideas to retain

There are four especially important distinctions:

1\. Connection vs user

A user can own multiple connections. A connection belongs to one push server.

2\. Connection health vs user presence

One unhealthy connection doesn't necessarily mean the user is offline. Other devices or sessions may still be connected.

3\. Connection delivery vs reliable message delivery

A successful socket write does not mean the client application processed the event.

4\. Reconnection vs recovery

Re-establishing a connection doesn't recover missed events. Recovery requires a separate replay, snapshot, or durable-message mechanism.

## Checkpoint: Can you reason about these scenarios?

Before proceeding, try to answer these using the mental models we've established.

Scenario 1: User A is connected to Push Server 1 and User A's mobile device is also connected to Push Server 3. Push Server 1 detects a heartbeat timeout. Should User A become offline?

Scenario 2: A client receives a WebSocket ping and returns a pong, but the application is stuck and doesn't process incoming messages. Is the connection healthy?

Scenario 3: Push Server 1 successfully writes a message to its socket. Immediately afterward, the server crashes. Can we guarantee that the client processed that message?

Scenario 4: A slow client receives 10,000 updates per second, but can process only 100. Should we keep all 10,000 updates in memory?

The answers follow directly from the concepts above:

1. No. Presence must account for all active connections.
2. The WebSocket endpoint is responding, but application health is not established.
3. No. A socket write is not a client application acknowledgement.
4. No. Use bounded buffers and an appropriate coalescing, persistence, or disconnection strategy.

# Next: Chapter 3 — Routing Messages by Audience

This is where we'll tackle the core of your original question.

We'll build four different routing architectures from the same foundation:

1. Global broadcast: All relevant users receive the same event (live scores, global announcements).
2. Group-based delivery: Only members of a particular group receive events (group chats, shared documents, collaboration).
3. Individual-user delivery: A particular user receives an event across their connected devices.
4. Individual-connection delivery: A particular browser tab or device receives an event.

For each, we'll understand exactly how to route to the correct push servers, what to store, whether Pub/Sub is appropriate, and how to avoid broadcasting everything to every push server.

Most importantly, we'll understand why the correct routing strategy depends on the audience of an event, not merely on whether the transport is WebSocket or SSE.
