# Chapter 5 — Connection Registry and Routing Infrastructure

This chapter is where we turn our conceptual architecture into something implementable.

So far, we know:

- A user connects to a push server.
- The push server owns the actual WebSocket/SSE connection.
- Other services publish events.
- A routing component determines the audience.
- The routing component needs to locate the push servers that own the recipient connections.

Now let's answer the practical question:

How do we maintain the mapping between users, connections, and push servers reliably, even when millions of connections are continuously created, destroyed, and moved between servers?

We'll build this from scratch.

# Part A — Understand What We Are Actually Storing

## 1. Start with the simplest data structure

Imagine we have three push servers.

```
Push Server 1:
    User A
    User B

Push Server 2:
    User C
    User D

Push Server 3:
    User E
    User F
```

A service wants to send a message to User D.

We need to find Push Server 2.

The simplest mapping is:

```
userId -> pushServerId
```

For example:

```
User A -> Push 1
User B -> Push 1
User C -> Push 2
User D -> Push 2
User E -> Push 3
User F -> Push 3
```

This looks straightforward.

But it immediately breaks when users have multiple connections.

## 2. Why `userId -> serverId` is insufficient

Consider User A.

They have:

- A desktop browser.
- A mobile application.
- Another browser tab.

These connections might be distributed across multiple servers.

```
User A
  |
  ├── Desktop connection -> Push 1
  |
  ├── Mobile connection -> Push 2
  |
  └── Browser tab        -> Push 3
```

Now suppose User A receives a new chat message.

Should we send it only to Push Server 1?

No. We probably want all three active connections to receive it.

Therefore, the mapping must be:

```
userId -> Multiple Connections
```

And each connection must identify its owning server.

A better representation:

```
User A
  |
  ├── Connection 101 -> Push 1
  |
  ├── Connection 102 -> Push 2
  |
  └── Connection 103 -> Push 3
```

This is our first fundamental data model.

One user can have multiple connections, and those connections can belong to different push servers.

## 3. Introduce a Connection ID

We need to uniquely identify every established connection.

Let's define:

```
connectionId
userId
pushServerId
deviceId
sessionId
connectedAt
```

For example:

```
{
  "connectionId": "conn-101",
  "userId": "user-A",
  "pushServerId": "push-1",
  "deviceId": "desktop-1",
  "sessionId": "session-123",
  "connectedAt": "2026-11-10T10:00:00Z"
}
```

Why do we need a separate connection ID?

Because the same user may have several connections.

Also, when one connection disconnects, we should be able to remove exactly that connection without affecting the others.

For example:

```
User A:
    conn-101 -> Push 1
    conn-102 -> Push 2
    conn-103 -> Push 3
```

If `conn-102` disconnects, we remove only `conn-102`.

We must not delete the entire User A entry.

# Part B — Understand Where Connection Information Lives

## 4. Two levels of connection storage

We actually need two different representations of connections.

### A. Local connection storage

Every push server maintains its actual open connections in its own memory.

For example:

Push Server 1

```
Local Connection Map:

conn-101 -> WebSocketSession
conn-104 -> WebSocketSession
conn-105 -> WebSocketSession
```

The `WebSocketSession` is an actual in-memory object representing the open connection.

The server can directly write to it.

For example:

```
session.sendMessage(message);
```

Another push server cannot access this object.

It belongs to Push Server 1's process memory.

### B. Distributed connection registry

Now we maintain a distributed directory.

```
User A -> conn-101 -> Push 1
User A -> conn-102 -> Push 2
User A -> conn-103 -> Push 3
```

This information is available to routing components and other push servers.

It tells them where connections live.

It does not contain the actual socket objects.

### The difference

| Local connection map                      | Distributed connection registry        |
| ----------------------------------------- | -------------------------------------- |
| Exists inside each push server            | Shared infrastructure                  |
| Contains actual socket/session references | Contains connection metadata           |
| Used to send data                         | Used to locate connection owners       |
| Lost when process crashes                 | Expires or is cleaned up independently |
| Usually in process memory                 | Redis or another distributed store     |

This separation is essential.

Never try to store actual WebSocket objects in Redis or a database.

We store references and routing metadata, not live network connections.

## 5. Which database should we use for the connection registry?

We need a storage system with:

- Very low-latency lookups.
- Frequent insertions and deletions.
- Fast updates.
- Support for multiple connections per user.
- Expiring records.
- High availability.
- Horizontal scalability.

A common choice is Redis.

Why Redis?

Because connection information is mostly ephemeral.

We don't need to retain every connection forever.

We need to know which connections are currently active.

For example:

```
User A connected at 10:00.

User A disconnected at 10:30.

The connection entry should disappear.
```

Redis provides useful data structures and expiration mechanisms for this.

Other distributed key-value stores can also work, but Redis is a good starting point for understanding the design.

Let's design the data model.

# Part C — Design the Redis Data Structures

## 6. Our first Redis schema

Let's maintain three logical structures.

### Structure 1: User → Connection IDs

```
user:user-A:connections

    conn-101
    conn-102
    conn-103
```

This gives us all connections belonging to a user.

A Redis Set is a natural fit.

Conceptually:

```
SADD user:user-A:connections conn-101 conn-102 conn-103
```

To retrieve the connection IDs:

```
SMEMBERS user:user-A:connections
```

### Structure 2: Connection → Metadata

Each connection has its own metadata.

```
connection:conn-101
```

Containing:

```
userId        = user-A
serverId      = push-1
deviceId      = desktop-1
sessionId     = session-123
connectedAt   = ...
```

We can represent this using a Redis Hash.

Conceptually:

```
HSET connection:conn-101 \
    userId user-A \
    serverId push-1 \
    deviceId desktop-1 \
    sessionId session-123
```

To retrieve it:

```
HGETALL connection:conn-101
```

### Structure 3: Push Server → Connection IDs

Now suppose Push Server 1 crashes.

We want to know all connections previously owned by it.

Therefore, we maintain a reverse index.

```
server:push-1:connections

    conn-101
    conn-104
    conn-105
```

This is another Redis Set.

Conceptually:

```
SADD server:push-1:connections conn-101 conn-104 conn-105
```

Why is this useful?

Because when a push server shuts down gracefully, we can clean up its connections.

If it crashes, we have an index to help with cleanup after detecting the failure.

### Our complete logical schema

```
1. USER -> CONNECTIONS

user:user-A:connections
    -> conn-101
    -> conn-102
    -> conn-103


2. CONNECTION -> METADATA

connection:conn-101
    -> user-A
    -> push-1
    -> desktop-1

connection:conn-102
    -> user-A
    -> push-2
    -> mobile-1


3. SERVER -> CONNECTIONS

server:push-1:connections
    -> conn-101

server:push-2:connections
    -> conn-102
```

This is our starting point.

But we still have a serious problem.

What happens when a push server crashes without cleaning up its Redis entries?

We need leases and expiration.

# Part D — Leases and Expiration

## 7. Why ordinary connection records are dangerous

Suppose we have:

```
User A -> conn-101 -> Push Server 1
```

Now Push Server 1 suddenly crashes.

It never gets an opportunity to execute:

```
DEL connection:conn-101
```

The Redis record remains.

Now imagine a service sends a notification to User A.

The registry says:

```
User A -> Push Server 1
```

But Push Server 1 no longer exists.

Our registry has stale information.

We need a way to distinguish active ownership from outdated ownership.

This is where leases come in.

## 8. What is a lease?

A lease is a temporary ownership record that remains valid only while it is periodically renewed.

Imagine Push Server 1 registers a connection.

```
Connection: conn-101

Owner: Push Server 1

Lease expiry: 10:01:00
```

The push server must periodically renew the lease.

For example:

```
10:00:00 -> Register connection
10:00:15 -> Renew
10:00:30 -> Renew
10:00:45 -> Renew
```

Each renewal moves the expiration time forward.

Suppose the push server crashes at 10:00:46.

It cannot renew anymore.

The lease expires at 10:01:00.

Now the stale connection is no longer considered active.

```
Push Server 1
      |
      ▼
   Crashes
      |
      ▼
No more renewals
      |
      ▼
Lease expires
      |
      ▼
Registry stops treating connection as active
```

This is a much safer mechanism than relying only on explicit disconnect callbacks.

### Why is this called a lease rather than simply TTL?

Because the important concept is not merely expiration.

It is temporary ownership.

The owner must continuously demonstrate that it is still alive and responsible for the resource.

A TTL can implement the lease.

## 9. How should a connection lease work?

Let's define an example configuration:

```
Lease duration: 60 seconds

Renewal interval: 15 seconds
```

These numbers are illustrative. Production values depend on connection count, failure-detection requirements, network conditions, and Redis capacity.

The lifecycle:

```
10:00:00 -> Register
10:00:15 -> Renew
10:00:30 -> Renew
10:00:45 -> Renew
10:01:00 -> Renew
```

When the connection is healthy, the lease keeps moving forward.

When the connection becomes unhealthy, the push server stops renewing it.

Eventually, the registry expires it.

### Important: Who renews the lease?

The push server.

The push server owns the actual connection and is responsible for determining whether that connection is still healthy.

The distributed registry should not independently assume that a connection is alive just because its metadata exists.

The push server renews the lease only for connections that are still locally active and healthy according to its connection lifecycle rules.

## 10. How does this translate into Redis?

We can maintain a separate TTL-enabled key for every connection.

Conceptually:

```
HSET connection:conn-101 \
    userId user-A \
    serverId push-1 \
    deviceId desktop-1

EXPIRE connection:conn-101 60
```

Every 15 seconds, Push Server 1 refreshes the expiration:

```
EXPIRE connection:conn-101 60
```

If the server crashes, no further refresh happens.

Redis eventually expires the connection record.

However, notice a problem.

Our user Set still contains:

```
user:user-A:connections

    conn-101
    conn-102
```

Even if `connection:conn-101` expires, Redis does not automatically remove `conn-101` from the Set.

TTL expiration applies to individual Redis keys, not individual members of a Set.

This is an important detail.

We need an additional cleanup mechanism.

### Better approach: Track lease expiry in a Sorted Set

Instead of using only a Set, we can use a Sorted Set for the user connections.

Conceptually:

```
Key: user:user-A:connections

Member       Score (expiry timestamp)
--------------------------------------
conn-101     1794312060
conn-102     1794312090
conn-103     1794312120
```

The score represents the lease expiration time.

We can update it with `ZADD`.

```
ZADD user:user-A:connections \
    1794312060 conn-101
```

When renewing the lease, update the score.

When retrieving connections, remove expired entries:

```
ZREMRANGEBYSCORE user:user-A:connections -inf 1794312000
```

Then obtain active connection IDs:

```
ZRANGEBYSCORE user:user-A:connections \
    (1794312000 +inf
```

We can also maintain separate metadata keys for each connection.

This gives us:

- Multiple connections per user.
- Per-connection expiration.
- Efficient retrieval of currently leased connections.
- Automatic cleanup of stale members during lookup or scheduled maintenance.

In production, we should also consider periodic cleanup so stale entries don't accumulate for users who never reconnect.

Important implementation detail: Use a consistent time source for lease calculations. Redis-side time inside atomic scripts can help avoid clock differences between many application servers.

# Part E — Registering, Renewing, and Removing Connections

## 11. The complete connection registration flow

Let's follow a client connecting to Push Server 1.

```
Client
  |
  | 1. Connect
  ▼
Load Balancer
  |
  ▼
Push Server 1
  |
  | 2. Authenticate user
  |
  | 3. Generate connection ID
  |
  | 4. Establish local connection
  |
  ▼
Local Connection Map
  |
  | 5. Register metadata in Redis
  ▼
Connection Registry
  |
  | 6. Registration successful
  ▼
Connection becomes routable
```

Let's look at each operation.

### Step 1: Authenticate the user

We identify:

```
userId = user-A
deviceId = desktop-1
```

### Step 2: Generate a unique connection ID

```
connectionId = conn-101
```

The ID should identify this specific connection, not just the user.

### Step 3: Identify the owning push server

For example:

```
serverId = push-1
```

We'll improve this identity in the next section.

### Step 4: Maintain the local connection

Push Server 1 adds:

```
conn-101 -> WebSocketSession
```

to its local connection map.

### Step 5: Register it in Redis

Conceptually:

```
HSET connection:conn-101 \
    userId user-A \
    serverId push-1 \
    deviceId desktop-1

ZADD user:user-A:connections \
    1794312060 conn-101

SADD server:push-1:connections conn-101

EXPIRE connection:conn-101 60
```

Now other components can find User A's connection.

### Step 6: Make the connection available for routing

Once registration succeeds, the connection is considered routable.

One subtlety: local socket registration and distributed registry registration are separate operations. A delivery that arrives during this transition should be handled safely. For example, the local connection can be placed in a registering state, and a premature delivery request can be retried rather than silently discarded.

If the registry is unavailable, we need an explicit policy. For a system requiring cross-server routing, we generally shouldn't report a new connection as fully active until its registration succeeds.

## 12. How should heartbeats renew Redis entries?

Suppose Push Server 1 owns 100,000 connections.

Every 15 seconds, it needs to refresh the relevant leases.

Conceptually:

```
Push Server 1:

Active connections:
    conn-101
    conn-102
    conn-103
    ...
    conn-100000

Every renewal interval:
    Refresh lease information.
```

We should avoid making thousands of unnecessary individual network round trips.

We can use:

- Redis pipelining.
- Batched updates.
- Lua scripts for atomic conditional updates.
- Carefully designed per-server lease tracking.

For example, multiple updates can be sent in one pipeline:

```
EXPIRE connection:conn-101 60
EXPIRE connection:conn-102 60
EXPIRE connection:conn-103 60
```

The operations still have to be executed, but network round trips are reduced.

### A useful production optimization: Server-level leases

Renewing a separate Redis TTL for every connection can become expensive at very large connection counts.

Consider one million active connections with a 15-second renewal interval.

That is roughly:

\\[ \frac{1,000,000}{15} \approx 66,667 \\]

connection renewals per second.

Instead, we can introduce another layer.

Maintain a lease for each push server instance:

```
server:push-1:lease
    -> expires in 30 seconds
```

Push Server 1 periodically renews this one server-level lease.

Each connection record contains its owning server instance:

```
conn-101 -> push-1
conn-102 -> push-1
conn-103 -> push-1
```

When Push Server 1's lease expires, all its connections should be considered unroutable, even if their individual metadata records haven't yet been cleaned up.

This gives us two choices:

| Approach                                         | Benefits                                                  | Tradeoff                                                |
| ------------------------------------------------ | --------------------------------------------------------- | ------------------------------------------------------- |
| Per-connection leases                            | Independently tracks each connection's registry freshness | Many renewal operations                                 |
| Server-level lease + explicit connection cleanup | Much cheaper renewal workload                             | Requires accurate local connection tracking and cleanup |

A hybrid is also possible.

For example:

- Renew a server lease frequently.
- Explicitly remove individually disconnected connections.
- Refresh individual connection records less frequently or in batches.
- Reconcile all local connections periodically.

This is a particularly useful pattern for very large push fleets.

Important: A server-level lease proves that the server process is alive, not that every client connection is alive. The push server still needs its connection-level heartbeat and cleanup mechanism from Chapter 2.

## 13. What happens when the client disconnects normally?

Suppose User A closes their browser.

Push Server 1 detects the connection close.

It removes the connection from local memory:

```
REMOVE conn-101 from local connection map
```

Then it removes the corresponding registry information:

```
ZREM user:user-A:connections conn-101

SREM server:push-1:connections conn-101

DEL connection:conn-101
```

The remaining User A connections are untouched.

```
Before:

User A:
    conn-101 -> Push 1
    conn-102 -> Push 2
    conn-103 -> Push 3


After conn-101 disconnects:

User A:
    conn-102 -> Push 2
    conn-103 -> Push 3
```

If no connections remain, User A has no active connections in the registry.

We can then update the user's presence state, if the application needs it.

### What if the connection crashes unexpectedly?

The explicit disconnect callback might never happen.

In that case:

1. The push server detects the unhealthy connection through heartbeat failure or write failure.
2. It removes the connection locally.
3. It removes its registry information.
4. If the push server itself crashes, its lease eventually expires.

These mechanisms complement each other.

We should never depend exclusively on the client sending a graceful disconnect.

# Part F — The Hardest Problem: Stale Ownership and Race Conditions

## 14. What happens when an old connection tries to delete a new connection?

Let's examine a subtle distributed-systems problem.

User A has this connection:

```
conn-101 -> Push 1
```

User A loses connectivity.

Before Push Server 1 detects the disconnection, User A reconnects.

The new connection goes to Push Server 3:

```
conn-202 -> Push 3
```

Now imagine Push Server 1 finally detects that `conn-101` is dead.

It executes:

```
Delete all connections for User A.
```

What happens?

It accidentally removes the new connection too!

This is a race condition.

### 14.1 The correct solution: Conditional cleanup

Always remove the exact connection that your server owns.

```
Delete conn-101 only if:

connectionId = conn-101
AND serverId = push-1
AND ownership token matches
```

Never blindly delete all connection information for a user when one connection disconnects.

This is why connection IDs are so important.

### 14.2 Introduce ownership tokens

We can associate each connection with an ownership token or generation.

For example:

```
conn-101:
    serverId = push-1
    generation = 17

conn-202:
    serverId = push-3
    generation = 42
```

Any cleanup or renewal operation must match the expected owner and generation.

Conceptually:

```
DELETE connection
WHERE connectionId = conn-101
AND owner = push-1
AND generation = 17
```

If the record has already changed, the cleanup must not modify it.

This is a form of compare-and-set behavior.

In Redis, we can implement these conditional operations using Lua scripts or transactions, depending on the key layout.

### 14.3 Why do we need server generations?

Consider a push server deployed on a machine or container named `push-1`.

It crashes.

It restarts.

It is again called `push-1`.

Now a delayed heartbeat from its old process might accidentally modify records owned by the new process.

To prevent this, don't identify ownership solely by a reusable logical name.

Use a unique process-instance identifier:

```
push-1:boot-17
```

After restart:

```
push-1:boot-18
```

Now the two processes have different identities.

An old process cannot accidentally renew or delete the new process's ownership records when operations compare the instance identity.

This is related to the distributed-systems concept of fencing tokens.

A fencing token is a monotonically increasing ownership version that allows downstream components to reject operations from an older owner.

For connection management, unique instance IDs plus conditional updates are a good foundation. For stronger ownership transfer guarantees, use monotonically increasing fencing generations.

## 15. What should happen if a routing request reaches the wrong server?

Imagine the registry contains:

```
User A -> conn-101 -> Push 1
```

The routing component dispatches an event to Push Server 1.

But Push Server 1 no longer has `conn-101` in its local connection map.

Perhaps the connection disconnected a moment ago, and Redis cleanup hasn't completed.

Push Server 1 responds:

```
{
  "status": "CONNECTION_NOT_FOUND",
  "connectionId": "conn-101"
}
```

What should the router do?

1. Treat the registry entry as potentially stale.
2. Remove it conditionally if it still belongs to the expected server and generation.
3. Recheck the user's other active connections if the delivery must reach all devices.
4. Retry to the remaining valid destinations where appropriate.

We should distinguish two cases.

Case A: Push server says the connection doesn't exist.

This is a good reason to clean up that exact connection entry.

Case B: Push server is temporarily unreachable.

This does not prove its connections are invalid.

It could be a network partition or temporary RPC failure.

Don't immediately delete every connection owned by that server.

Instead, retry according to the delivery policy, check server health, and let lease expiration or authoritative failure detection establish stale ownership.

This prevents temporary network problems from incorrectly destroying connection metadata.

# Part G — The Actual Message Routing Algorithm

## 16. Let's implement individual-user routing conceptually

Suppose Service A publishes:

```
{
  "eventId": "event-901",
  "userId": "user-A",
  "payload": {
    "message": "Your order shipped"
  }
}
```

The router performs the following steps.

### Step 1: Find User A's connections

Read:

```
user:user-A:connections
```

Suppose we get:

```
conn-101
conn-102
conn-103
```

### Step 2: Get connection metadata

```
conn-101 -> Push 1
conn-102 -> Push 2
conn-103 -> Push 1
```

### Step 3: Validate active ownership

Filter out:

- Expired connection leases.
- Connections owned by expired server instances.
- Connections already known to be invalid.

### Step 4: Deduplicate destination servers

We have:

```
Push 1
Push 2
Push 1
```

So the destination set becomes:

```
{Push 1, Push 2}
```

### Step 5: Dispatch the message

We send one internal request to each destination server.

For example:

```
{
  "eventId": "event-901",
  "recipients": [
    "conn-101",
    "conn-103"
  ],
  "payload": {
    "message": "Your order shipped"
  }
}
```

Push Server 1 handles its two local connections.

Push Server 2 handles its one local connection.

### Step 6: Local delivery

Each push server checks its local connection map:

```
for (String connectionId : recipientConnectionIds) {
    WebSocketSession session = localConnections.get(connectionId);

    if (session != null && session.isOpen()) {
        session.sendMessage(message);
    }
}
```

This is conceptual Java code; a production implementation must also serialize concurrent writes, handle asynchronous send completion, enforce backpressure, and clean up failed sessions.

### The final routing algorithm

```
Input: Event(userId, payload)

1. Get user's connection IDs.

2. Resolve connection metadata.

3. Remove expired/stale connections.

4. Group connections by push server.

5. Dispatch one request per destination server.

6. Each server sends to its local connections.

7. Handle stale connections and delivery failures.
```

Notice that the routing component doesn't need to know anything about actual WebSocket objects.

It only knows connection IDs and server addresses.

# Part H — Routing Group Messages Efficiently

## 17. Small groups: Resolve members and batch connection lookups

Suppose Group G1 contains:

```
User A
User B
User C
User D
User E
```

The group service returns the members.

```
G1 -> [A, B, C, D, E]
```

We then look up their connections.

```
User A -> Push 1
User B -> Push 1
User C -> Push 2
User D -> Push 3
User E -> Push 2
```

The destination set becomes:

```
Push 1
Push 2
Push 3
```

We don't want to make five independent network round trips to Redis if we can avoid it.

We can use pipelining or batched lookups.

Conceptually:

```
Group Fanout Worker
        |
        | Fetch members
        ▼
    [A, B, C, D, E]
        |
        | Batch connection lookup
        ▼
    Connection Registry
        |
        ▼
    Connection metadata
        |
        ▼
    Group by Push Server
        |
        ▼
    [Push 1, Push 2, Push 3]
```

The number of recipients and the number of destination servers are not necessarily the same.

This is an important optimization.

## 18. Large groups: Maintain a group-to-server interest registry

Now consider a group containing 10 million users.

We don't want to enumerate all 10 million members for every event.

Instead, let's maintain another derived index.

```
Group G1 -> Active Push Servers
```

For example:

```
group:G1:servers

    push-1
    push-3
    push-8
    push-12
```

This tells us which servers currently host connections interested in G1.

### How do we maintain this?

Suppose User A joins Group G1 while connected to Push Server 1.

Push Server 1 updates its local subscription map:

```
Local group subscriptions:

G1:
    User A
```

It then registers its interest:

```
SADD group:G1:servers push-1
```

Now User B joins the same group on Push Server 1.

Local map:

```
G1:
    User A
    User B
```

We don't need to register Push Server 1 again.

When the last local member leaves G1, Push Server 1 removes its interest:

```
SREM group:G1:servers push-1
```

Now the router can do:

```
Group G1 event
       |
       ▼
group:G1:servers
       |
       ├── Push 1
       ├── Push 3
       ├── Push 8
       └── Push 12
```

It dispatches one copy to each of these servers.

Each server sends to its local members.

### What if a push server crashes?

We have the same stale-entry problem as before.

The group-to-server registration should be associated with a server lease or expiration mechanism.

For example, use a Sorted Set:

```
group:G1:servers

Member      Expiry
-------------------------
push-1      10:01:00
push-3      10:01:00
push-8      10:01:15
```

The push servers periodically renew their interest while they still have at least one local subscriber.

If Push Server 1 crashes, its entry eventually becomes expired.

The router ignores expired entries.

### Important: This registry is derived data

The authoritative group membership is still owned by the chat or collaboration service.

The group-to-server interest registry is simply an optimization for live routing.

If we lose it, we should be able to reconstruct it from active connections and subscriptions.

It must not become the only source of authorization.

## 19. What about global broadcasts?

For global broadcasts, we generally don't need to examine individual user records.

We need a list of healthy push-server instances.

Conceptually:

```
Active Push Servers:

push-1
push-2
push-3
push-4
```

This membership can come from service discovery, orchestration, or a dedicated server registry.

A broadcast router sends the event to all relevant nodes.

Alternatively, each node can subscribe to an appropriate broadcast channel.

These are different implementations of the same goal.

For a global announcement:

```
                    Global Event
                         |
                         ▼
                   Broadcast Router
                         |
              ┌──────────┼──────────┐
              |          |          |
              ▼          ▼          ▼
           Push 1     Push 2     Push 3
              |          |          |
              ▼          ▼          ▼
           Local      Local      Local
           users      users      users
```

For a category-specific broadcast, such as a particular sports match, we can use the group-to-server interest pattern.

This avoids broadcasting match events to servers without interested clients.

# Part I — Redis Cluster and Registry Scaling

## 20. What happens when the registry itself becomes huge?

Let's assume:

- 10 million concurrent connections.
- 100 push servers.
- Frequent connection creation and removal.
- High-volume routing lookups.

A single Redis instance might become a bottleneck.

We can partition the registry.

Redis Cluster allows us to distribute keys across multiple Redis nodes.

Conceptually:

```
                Connection Registry
                         |
             ┌───────────┼───────────┐
             |           |           |
             ▼           ▼           ▼
          Redis 1     Redis 2     Redis 3
          Shard A     Shard B     Shard C
             |           |           |
          Users A-F   Users G-M   Users N-Z
```

We can distribute user connection metadata based on user IDs.

For example:

```
user-A -> Redis 1
user-B -> Redis 3
user-C -> Redis 2
```

The exact key placement is handled by the Redis Cluster hashing mechanism.

### A useful design principle

Partition by the entity we most frequently look up.

For individual delivery, this is usually `userId`.

For group interest, it might be `groupId`.

These are two different access patterns.

They may live in different key spaces or even different infrastructure components.

### Important Redis Cluster constraint

Redis Cluster uses hash slots.

A Lua script or multi-key transaction generally requires the involved keys to be in the same hash slot.

This matters for our registration operations.

For example, we may want to atomically update:

```
user:user-A:connections

connection:conn-101

server:push-1:connections
```

These keys might live on different Redis shards.

We cannot assume that one Lua script can atomically update all three in a Redis Cluster.

A practical strategy is to colocate per-user connection metadata using a hash tag:

```
user:{user-A}:connections

user:{user-A}:connection:conn-101
```

The `{user-A}` portion ensures these related keys share the same hash slot.

We can then perform the user-level registration and cleanup atomically.

The server reverse index might be stored on a different shard.

We can maintain that index separately and treat it as an eventually consistent auxiliary structure.

This is a good example of why logical data modeling and physical storage partitioning must be designed together.

## 21. Avoid unnecessary Redis calls

A routing system can accidentally generate a huge number of registry operations.

Consider group delivery:

```
Group size = 100,000
```

A naive implementation could perform:

```
100,000 separate user lookups
```

That is expensive.

Better approaches include:

- Batch/pipeline recipient lookups.
- Deduplicate users and destination servers.
- Use group-to-active-server indexes for large groups.
- Use bounded caches with invalidation or short validity windows.
- Avoid repeatedly resolving the same audience when the subscription membership hasn't changed.

For individual-user delivery, retrieving a small set of connections is generally cheap.

For large-group delivery, optimize at the server level rather than repeatedly enumerating every user.

### What about local caches?

Suppose the router frequently sends messages to User A.

It could cache:

```
User A -> Push Server 1, Push Server 2
```

This reduces Redis lookups.

But consider what happens if User A reconnects to Push Server 3.

The cached mapping becomes stale.

Therefore, any cache of connection ownership must have a clear invalidation or freshness strategy.

For most implementations, I would first build the correct Redis-backed lookup and measure it before introducing additional caches.

# Part J — What Happens When Redis Goes Down?

## 22. Do all WebSocket connections immediately stop working?

No.

Remember:

The actual connections live inside push servers.

Redis only stores distributed metadata.

Suppose Redis becomes unavailable.

```
                Redis Registry
                     X
                   DOWN
```

The existing push servers might still have:

```
Push Server 1:
    User A
    User B

Push Server 2:
    User C
    User D
```

They can continue writing to those local sockets.

But cross-server routing becomes problematic.

A routing worker can no longer reliably discover where a user is connected.

### What should happen to new connections?

There are several possible policies.

Policy A: Reject or delay new connections.

If the application requires reliable distributed routing, don't claim a connection is fully registered until Redis becomes available.

The client retries.

Policy B: Allow local-only connections temporarily.

Existing users can continue using their connections, but targeted cross-server delivery is degraded.

This is appropriate only when the application has a recovery mechanism.

Policy C: Use a fallback routing mechanism.

For example, a service directory or another healthy registry can temporarily take over.

This adds complexity and must be designed in advance.

There is no universally correct choice.

The important thing is not to silently assume everything still works.

### What about events published during the outage?

For durable systems, they should remain in Kafka or another reliable delivery system.

Once registry connectivity returns, routing can resume.

For ephemeral events, temporary loss may be acceptable.

## 23. What happens if Redis loses its data after a failover?

Because connection metadata is ephemeral, we don't necessarily need to persist it in a relational database.

But we need to be able to reconstruct it.

Suppose Redis fails and returns with an empty registry.

Push servers still have their local connection maps.

They can re-register their active connections.

```
Push Server 1
    |
    | Enumerate local active connections
    |
    ▼
Register them again in Redis
```

Similarly:

```
Push Server 2
    |
    ▼
Re-register local active connections
```

This gives us a recovery process.

A robust implementation needs:

1. Server instances to retain their local connection maps.
2. Re-registration after registry recovery.
3. Idempotent registration operations.
4. A server generation to distinguish old and new ownership.
5. A mechanism preventing incomplete re-registration from being mistaken for a complete view.

One useful technique is to mark a server as `RECOVERING`.

Only after it has re-registered its active connections should the server become fully routable again.

During recovery, the routing layer can avoid sending traffic to it or retry deliveries after registration completes.

This demonstrates another important design principle: the registry is reconstructible infrastructure state, not your only copy of business data.

# Part K — Should We Store Connection Information in a Relational Database?

## 24. Why not PostgreSQL or MySQL?

Suppose every connection produces:

```
INSERT connection
UPDATE lastHeartbeat
DELETE connection
```

With millions of connections, frequent writes would create unnecessary database workload.

Also, current connection ownership is short-lived and frequently changing.

A relational database can support this workload, but it is usually not the most efficient choice for the live registry.

A better division is:

| Data                         | Storage                              |
| ---------------------------- | ------------------------------------ |
| Active connection ownership  | Redis or distributed KV              |
| Actual open socket           | Push server memory                   |
| User account                 | Relational database                  |
| Device/session authorization | Identity/session system              |
| Group membership             | Business database                    |
| Chat messages                | Durable chat database                |
| Notification inbox           | Durable database                     |
| Event history                | Kafka or application database        |
| Delivery tracking            | Durable delivery store when required |

The connection registry is usually a soft-state system.

If it disappears, the application should be able to reconstruct it from active push-server processes.

The business data must not be lost when Redis disappears.

# Part L — Presence: Is the User Online?

## 25. Should presence be another database table?

Suppose a user is connected on three devices:

```
User A:

conn-101 -> Push 1
conn-102 -> Push 2
conn-103 -> Push 3
```

Now conn-101 disconnects.

Should User A become offline?

No.

The user still has two active connections.

Presence should be derived from active connections, not from the status of a single connection.

Conceptually:

```
User is ONLINE if:

At least one valid active connection exists.
```

And:

```
User is OFFLINE if:

No valid active connections exist.
```

We might maintain a separate presence state for efficient queries:

```
presence:user-A = ONLINE
```

But this is derived information.

It should be updated when the number of active connections changes from:

```
0 -> 1 : ONLINE
1 -> 0 : OFFLINE
```

A transition from 3 connections to 2 should not make the user offline.

### What about stale presence?

Suppose the push server crashes.

The user may still be marked online for a while.

Once the server lease expires and its connections are removed or treated as invalid, the presence system should recalculate the user's state.

Presence is therefore a form of eventually consistent state in many applications.

For applications where precise online status matters, we may need stronger coordination and more frequent lease updates.

# Part M — Handling Multiple Devices and Exclusive Sessions

## 26. What if only one connection per device is allowed?

Some applications allow multiple devices.

Others don't.

For example, a banking application may permit only one active session per device or may explicitly limit concurrent sessions.

Consider:

```
User A:
    Device 1 -> conn-101
    Device 2 -> conn-102
```

Now Device 1 reconnects and creates:

```
conn-103
```

Should conn-101 remain active?

It depends on the session policy.

### Policy A: Multiple connections per device are allowed

We simply add conn-103.

```
Device 1:
    conn-101
    conn-103
```

Useful when multiple browser tabs or application windows should remain connected.

### Policy B: One active connection per device

We maintain an additional mapping:

```
user-A:device-1 -> conn-101
```

When conn-103 is established, the system atomically replaces the device ownership mapping.

```
Before:

user-A:device-1 -> conn-101

After:

user-A:device-1 -> conn-103
```

The old connection is then notified or closed.

We should use conditional ownership/version checks so that a delayed operation from conn-101 cannot overwrite conn-103.

### Policy C: One active connection per user

We can similarly maintain:

```
user-A -> conn-101
```

But this is less common for applications where users need simultaneous web and mobile sessions.

The important point:

The connection registry supports the application's session policy; it should not dictate that policy.

# Part N — A More Complete Registry Design

## 27. Bring the pieces together

Let's assemble the logical schema we've developed.

```

CONNECTION REGISTRY

1. USER -> CONNECTIONS

user:{userId}:connections

    connectionId -> leaseExpiration


2. CONNECTION -> METADATA

user:{userId}:connection:{connectionId}

    userId
    connectionId
    serverInstanceId
    serverGeneration
    deviceId
    sessionId
    connectedAt
    connectionState


3. SERVER -> CONNECTIONS

server:{serverInstanceId}:connections

    connectionId
    connectionId
    connectionId


4. SERVER -> LEASE

server:{serverInstanceId}:lease

    expiration


5. GROUP -> ACTIVE SERVERS

group:{groupId}:servers

    serverInstanceId -> leaseExpiration


6. SERVER -> LOCAL GROUPS

server:{serverInstanceId}:groups

    groupId
    groupId
    groupId


LOCAL PUSH SERVER MEMORY

1. connectionId -> WebSocket/SSE session

2. userId -> Set<connectionId>

3. groupId -> Set<connectionId>

4. serverInstanceId -> server lifecycle information

```

Some of these structures are optional.

For example, the server-to-connection reverse index is useful for bulk cleanup and reconciliation, but it can be maintained asynchronously if your Redis Cluster key layout prevents atomic cross-shard updates.

Likewise, we don't necessarily need a separate user-to-connection index if we choose a different representation for the connection metadata.

The point is to support the important access patterns.

## 28. The important Redis operations

Let's define the operations our registry abstraction should expose.

We don't want every application service to know the details of Redis keys.

We want a clear interface.

```
interface ConnectionRegistry {

    // Register a newly established connection.
    void register(ConnectionMetadata connection);

    // Refresh an existing connection's ownership lease.
    boolean renew(
        String connectionId,
        String serverInstanceId,
        long generation
    );

    // Remove one specific connection.
    boolean unregister(
        String connectionId,
        String serverInstanceId,
        long generation
    );

    // Get all valid connections for a user.
    List<ConnectionMetadata> getUserConnections(
        String userId
    );

    // Find active server instances for a group.
    List<String> getGroupServers(
        String groupId
    );

    // Register a server's interest in a group.
    void registerGroupInterest(
        String groupId,
        String serverInstanceId
    );

    // Remove a server's interest in a group.
    void removeGroupInterest(
        String groupId,
        String serverInstanceId
    );
}
```

These are conceptual method signatures.

The actual implementation needs atomic conditional operations, batched access, and clear handling for Redis errors and expired records.

### Why is this abstraction useful?

Because routing shouldn't depend on whether we use:

- Redis.
- Another distributed key-value store.
- A custom sharded connection registry.

We can change the storage implementation without rewriting the entire business routing system.

# Part O — The Complete Connection Lifecycle With Failure Handling

## 29. Let's walk through one connection from beginning to end

Suppose User A connects.

Initial state:

```
No active connection for User A.
```

### Step 1: Connection establishment

```
Client A
   |
   ▼
Load Balancer
   |
   ▼
Push Server 1
```

Push Server 1 authenticates the user.

### Step 2: Create local ownership

```
conn-101 -> WebSocketSession
```

It generates a unique connection identifier.

### Step 3: Register in Redis

```
User A -> conn-101 -> Push 1
```

The server and connection ownership records are registered.

### Step 4: Renew the lease

Push Server 1 periodically refreshes its ownership lease.

### Step 5: Receive an event

A routing worker receives:

```
{
  "userId": "user-A",
  "type": "NEW_MESSAGE",
  "messageId": "msg-101"
}
```

It queries Redis.

```
User A -> conn-101 -> Push 1
```

The router dispatches to Push Server 1.

### Step 6: Local delivery

Push Server 1 finds `conn-101` in its local map and writes to the socket.

### Step 7: User disconnects

Push Server 1 detects the disconnect.

It removes the connection from:

- Its local map.
- The user's connection index.
- The server's reverse index.
- The connection metadata.

### Step 8: User reconnects elsewhere

User A reconnects.

The load balancer chooses Push Server 3.

A new connection is established:

```
User A -> conn-202 -> Push 3
```

No special socket migration is needed.

### Step 9: Old Push Server 1 cleanup arrives late

Push Server 1 tries to remove `conn-101`.

The conditional deletion only affects `conn-101` if its ownership generation still matches.

It cannot delete `conn-202`.

This protects us against stale operations.

## 30. Complete architecture

Here is the architecture we have arrived at.

\#chatgpt-mermaid-\_r_6g5\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_6g5\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_6g5\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_6g5\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_6g5\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_6g5\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_6g5\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_6g5\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_6g5\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_6g5\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_6g5\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_6g5\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_6g5\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_6g5\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_6g5\_ p{margin:0;}#chatgpt-mermaid-\_r_6g5\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_6g5\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_6g5\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_6g5\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_6g5\_ .label text,#chatgpt-mermaid-\_r_6g5\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_6g5\_ .node rect,#chatgpt-mermaid-\_r_6g5\_ .node circle,#chatgpt-mermaid-\_r_6g5\_ .node ellipse,#chatgpt-mermaid-\_r_6g5\_ .node polygon,#chatgpt-mermaid-\_r_6g5\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_6g5\_ .rough-node .label text,#chatgpt-mermaid-\_r_6g5\_ .node .label text,#chatgpt-mermaid-\_r_6g5\_ .image-shape .label,#chatgpt-mermaid-\_r_6g5\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_6g5\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_6g5\_ .rough-node .label,#chatgpt-mermaid-\_r_6g5\_ .node .label,#chatgpt-mermaid-\_r_6g5\_ .image-shape .label,#chatgpt-mermaid-\_r_6g5\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_6g5\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_6g5\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_6g5\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_6g5\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_6g5\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_6g5\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_6g5\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_6g5\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_6g5\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_6g5\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_6g5\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_6g5\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_6g5\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_6g5\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_6g5\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_6g5\_ .icon-shape,#chatgpt-mermaid-\_r_6g5\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_6g5\_ .icon-shape p,#chatgpt-mermaid-\_r_6g5\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_6g5\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_6g5\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_6g5\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_6g5\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_6g5\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_6g5\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_6g5\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_6g5\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_6g5\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_6g5\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_6g5\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_6g5\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_6g5\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_6g5\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_6g5\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_6g5\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_6g5\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_6g5\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_6g5\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_6g5\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_6g5\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_6g5\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_6g5\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_6g5\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_6g5\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_6g5\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_6g5\_ .node rect,#chatgpt-mermaid-\_r_6g5\_ .node circle,#chatgpt-mermaid-\_r_6g5\_ .node ellipse,#chatgpt-mermaid-\_r_6g5\_ .node polygon,#chatgpt-mermaid-\_r_6g5\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_6g5\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_6g5\_ .mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_6g5\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_6g5\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_6g5\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}LocalStatePush Server 1Push Server 2Push Server 3Local Socket MapLocal Socket MapLocal Socket MapClientsLoad BalancerDistributed ConnectionRegistryBusiness Events / Kafka /QueueRouting WorkersRegister / Renew / RemoveRegister / Renew / RemoveRegister / Renew / RemoveTargeted dispatchTargeted dispatchTargeted dispatchLocal socket deliveryLocal socket deliveryLocal socket delivery

The architecture now has a very clean separation:

- The load balancer establishes connections.
- Push servers own actual connections.
- The registry provides distributed ownership metadata.
- Routing workers resolve the audience and destination servers.
- Push servers deliver locally.

# Part P — Production Design Decisions and Tradeoffs

## 31. How I would design the registry for a large system

Let's consider a system with:

```
10 million concurrent connections
500 push-server instances
Multiple devices per user
Global, group, and individual events
```

I would start with these decisions:

| Concern                 | Design                                              |
| ----------------------- | --------------------------------------------------- |
| Actual sockets          | Local push-server memory                            |
| Connection lookup       | Distributed Redis/KV registry                       |
| User connection mapping | User ID → multiple connection IDs                   |
| Connection metadata     | Connection ID → server instance and session details |
| Ownership               | Unique push-server instance ID + generation         |
| Expiration              | Leases and server-level health                      |
| Normal disconnect       | Conditional connection cleanup                      |
| Server crash            | Lease expiry and stale-entry cleanup                |
| Individual routing      | User lookup → unique destination servers            |
| Group routing           | Membership lookup or group-to-server interest index |
| Global routing          | Active server discovery or broadcast channel        |
| Registry loss           | Re-register active local connections                |
| High-volume lookups     | Batching, pipelining, sharding                      |
| Presence                | Derived from active connections                     |
| Durable messages        | Separate business database or delivery store        |

These decisions are independent of whether the transport is WebSocket or SSE.

The registry works for both.

## 32. Important tradeoff: Centralized lookup vs distributed routing

There is another architectural choice worth understanding.

Should every message require the routing worker to query Redis?

Or should push servers discover each other's connections directly?

### Design A: Centralized registry lookup

```
Router
  |
  ▼
Redis Registry
  |
  ▼
Destination Push Server
```

Advantages:

- Simple routing logic.
- Centralized ownership metadata.
- Easy to support multiple destination types.
- Easier to reason about connection ownership.

Disadvantages:

- Registry becomes a critical dependency.
- High lookup volume.
- Need to manage stale metadata.

This is a good default architecture.

### Design B: Distributed local subscription routing

Each push server maintains local subscriptions.

Servers subscribe to appropriate event channels, and events are routed using those subscriptions.

```
                 Message Broker
                       |
              ┌────────┼────────┐
              |        |        |
              ▼        ▼        ▼
            Push 1   Push 2   Push 3
              |        |        |
          Local G1  Local G2  Local G1
```

Advantages:

- Some lookups can be avoided.
- Efficient for active group subscriptions.
- Natural for room-based collaboration.

Disadvantages:

- Subscription lifecycle becomes more complicated.
- Dynamic membership must be maintained.
- Stale subscriptions are possible.
- Large numbers of subscriptions can create broker overhead.

### My recommendation

Start with a distributed connection registry.

Introduce group-to-server interest tracking when large-group fanout makes individual recipient lookup too expensive.

Don't introduce a sophisticated distributed subscription architecture prematurely.

# 33. What I would test before calling the registry production-ready

A correct-looking Redis schema is not enough.

We need to test its failure behavior.

| Test                                        | Expected behavior                                        |
| ------------------------------------------- | -------------------------------------------------------- |
| Client disconnects normally                 | Exact connection entry removed                           |
| Client loses network silently               | Connection eventually removed                            |
| User reconnects to another server           | New connection becomes routable                          |
| Old server performs delayed cleanup         | New connection remains untouched                         |
| Push server crashes                         | Its connections eventually become invalid                |
| Push server restarts with same logical name | New instance generation is used                          |
| Redis becomes unavailable                   | Connections don't falsely appear fully registered        |
| Redis loses registry data                   | Active servers re-register connections                   |
| Redis returns stale metadata                | Router detects and repairs it                            |
| User has several active devices             | All intended devices receive delivery                    |
| Group membership changes                    | Removed users stop receiving future authorized events    |
| A group becomes very large                  | Routing avoids enumerating every member for every update |
| Redis shard becomes unavailable             | Routing follows an explicit failure and retry policy     |

These tests are important because most registry failures are race conditions, not simple lookup failures.

# 34. The Five Concepts I Want You to Internalize

Let's reduce this chapter to the underlying reasoning.

1\. A user is not a connection.

One user can have multiple active connections on multiple push servers.

Therefore:

```
userId -> Set<connectionId>
```

2\. A connection is not a socket in Redis.

The actual socket lives in the owning push server's local memory.

The distributed registry stores ownership metadata.

3\. Connection ownership is temporary.

A push server must periodically demonstrate that it still owns the connection or server instance through leases.

4\. Cleanup must be conditional.

An old connection must never be allowed to delete a newer connection.

Use unique connection IDs and ownership generations.

5\. The registry is reconstructible soft state.

If Redis loses its connection metadata, push servers can re-register their active local connections.

Business messages and notification history must be stored separately.

## 35. A final mental exercise

Suppose this is our registry:

```
User A:
    conn-101 -> Push 1
    conn-102 -> Push 2

User B:
    conn-103 -> Push 2

User C:
    conn-104 -> Push 3
```

Now these things happen:

1. Push Server 1 crashes.
2. User A reconnects to Push Server 3.
3. Push Server 2 detects that conn-102 is unhealthy.
4. Push Server 1's old cleanup request arrives late.
5. A message arrives for User A.

What should happen?

Let's reason it out.

After Push Server 1 crashes:

Its server lease eventually expires. conn-101 is no longer considered routable.

User A reconnects:

A new connection is registered:

```
conn-105 -> Push 3
```

Push Server 2 detects conn-102 is unhealthy:

It removes only conn-102.

Push Server 1's delayed cleanup arrives:

It attempts to remove conn-101 with its old generation. It must not affect conn-105.

The message arrives for User A:

The registry returns the currently valid connection:

```
conn-105 -> Push 3
```

Push Server 3 delivers it.

This is exactly how we want the system to behave.

# Chapter 5 — Final Architecture Summary

We now have a proper connection ownership system.

```
                 BUSINESS EVENT
                       |
                       ▼
                ROUTING WORKER
                       |
                       ▼
              CONNECTION REGISTRY
                       |
             ┌─────────┼─────────┐
             |         |         |
             ▼         ▼         ▼
          Push 1    Push 2    Push 3
             |         |         |
          Local     Local     Local
          Sockets   Sockets   Sockets
             |         |         |
             ▼         ▼         ▼
          Clients   Clients   Clients
```

The connection registry solves one specific problem:

Finding where an active connection lives.

It doesn't guarantee that:

- The message was successfully dispatched.
- The push server received it.
- The socket write succeeded.
- The client received it.
- The client processed it.
- The client persisted it.

Those are separate reliability problems.

And that brings us to the next chapter.

# Next: Chapter 6 — Reliable Message Delivery

This is arguably the most important chapter for building a genuinely reliable push system.

We'll start with a simple scenario:

```
Service A generates event 101.

Kafka receives event 101.

Router consumes event 101.

Router dispatches it to Push Server 1.

Push Server 1 writes it to User A's WebSocket.
```

Now we introduce failures at every possible point.

- What if Kafka receives the event but the router crashes?
- What if the router dispatches the event but crashes before committing its offset?
- What if Push Server 1 receives the event but crashes before writing to the socket?
- What if the socket write succeeds but the client never processes the message?
- What if the client processes the message but its ACK is lost?
- What if retries produce duplicate messages?
- What if messages arrive out of order?
- What if the user is offline for several hours?

We'll progressively build:

1. Best-effort vs at-most-once vs at-least-once delivery.
2. Why exactly-once delivery is difficult.
3. Producer, broker, consumer, push-server, and client acknowledgements.
4. Transactional Outbox pattern.
5. Durable notification inbox and delivery records.
6. Retry strategies, exponential backoff, and dead-letter queues.
7. Idempotency and duplicate detection.
8. Ordering guarantees and sequence numbers.
9. Client acknowledgements and replay cursors.
10. Offline message recovery.
11. How to combine Kafka, Redis, databases, and WebSockets without losing important events.

The central question: What does it actually mean for a message to be delivered reliably, and how do we guarantee that failures at any intermediate stage don't silently lose important messages?

