# Chapter 3 — Routing Messages by Audience

This is the most important chapter for understanding how real-time push infrastructure actually routes messages at scale.

In Chapter 1, we established that every connection belongs to a particular push server.

In Chapter 2, we understood how connections are established, maintained, and recovered.

Now we have a new problem:

When an event occurs, how do we determine exactly which push servers should receive it?

Let's build the solution by evolving the architecture for different kinds of audiences.

## 1. First establish the fundamental routing model

Imagine we have this infrastructure:

```
                         Load Balancer
                               |
                  ┌────────────┼────────────┐
                  |            |            |
                  ▼            ▼            ▼
               Push 1       Push 2       Push 3
```

Users are connected as follows:

```
Push Server 1:
    User A
    User B
    User C

Push Server 2:
    User D
    User E
    User F

Push Server 3:
    User G
    User H
    User I
```

Now suppose an event occurs.

We need to answer two independent questions:

Question 1: Who should receive this event?

This is an audience-selection problem.

Question 2: Which push servers currently own those recipients' connections?

This is a connection-routing problem.

These are two different operations.

For example, suppose User A sends a chat message to a group containing:

```
Group G1:
    User A
    User D
    User F
    User H
```

First, we determine the group members.

Then, we determine where their connections live.

```
Group G1
   |
   ├── User A ──► Push 1
   |
   ├── User D ──► Push 2
   |
   ├── User F ──► Push 2
   |
   └── User H ──► Push 3
```

Notice something important:

We have four recipient users, but only three destination push servers.

We should send only one copy of the event to Push Server 2, not two copies.

Push Server 2 can then deliver the event to both User D and User F.

This is the fundamental routing pipeline:

```
Event
  |
  ▼
Determine Audience
  |
  ▼
Resolve Recipients
  |
  ▼
Resolve Active Connections
  |
  ▼
Identify Destination Push Servers
  |
  ▼
Deduplicate Destination Servers
  |
  ▼
Dispatch Event
  |
  ▼
Push Servers Deliver Locally
```

Every routing architecture we discuss is essentially a variation of this pipeline.

## 2. Don't confuse audience membership with connection ownership

Before discussing the different patterns, let's establish another important distinction.

Suppose we have a group chat.

We need to maintain two kinds of information.

A. Group membership

```
Group G1:
    User A
    User D
    User F
    User H
```

This is business/domain information.

It answers:

> Which users are members of Group G1?

It typically belongs to the chat service and its database.

B. Connection ownership

```
User A -> Push 1
User D -> Push 2
User F -> Push 2
User H -> Push 3
```

This is connection information.

It answers:

> Where are the active connections of these users?

It belongs to the push infrastructure and its connection registry.

These must not be treated as the same thing.

For example, a user can belong to a group without currently being connected.

Likewise, a connected user doesn't automatically have permission to receive every group's events.

Our architecture should preserve this separation.

# Part A — Global Broadcast

## 3. Pattern 1: Send the same event to everyone

Imagine we are building a live sports application.

A cricket match is happening.

Whenever a player scores, the application needs to update all connected users watching that match.

For now, assume every connected user is interested in the same match.

We have:

```
Push Server 1:
    User A
    User B
    User C

Push Server 2:
    User D
    User E
    User F

Push Server 3:
    User G
    User H
    User I
```

An event occurs:

```
{
  "eventId": "event-101",
  "type": "SCORE_UPDATE",
  "payload": {
    "team": "India",
    "score": "180/4",
    "overs": "32.5"
  }
}
```

Who needs to receive it?

Everyone.

### 3.1 The naive approach

The sports service could directly call every push server.

```
Sports Service
      |
      ├────────► Push 1
      |
      ├────────► Push 2
      |
      └────────► Push 3
```

This works.

But the sports service now needs to know:

- How many push servers exist.
- Which ones are healthy.
- Their addresses.
- When servers are added or removed.
- How to retry failed requests.
- How to handle server membership changes.

We're introducing infrastructure responsibilities into the business service.

We need something better.

### 3.2 Introduce Pub/Sub

This is a natural use case for Pub/Sub.

The sports service publishes the event to a broadcast channel.

```
                  Sports Service
                        |
                        | Publish
                        ▼
                  Global Broadcast
                     Channel
                        |
              ┌─────────┼─────────┐
              |         |         |
              ▼         ▼         ▼
           Push 1    Push 2    Push 3
              |         |         |
              ▼         ▼         ▼
           Users     Users     Users
```

Each push server subscribes independently to the channel.

Whenever a new event arrives, every subscribed push server receives a copy.

Each server broadcasts it to its locally connected users.

For example:

```
Push Server 1 receives event
    |
    ├── Send to User A
    ├── Send to User B
    └── Send to User C
```

This is exactly the kind of workload Pub/Sub is good at.

### 3.3 What does the push server subscribe to?

We might have a logical channel:

```
global-broadcast
```

All push servers subscribe to it.

Another example:

```
sports-live-updates
```

All push servers interested in sports updates subscribe to it.

The channel is a logical communication destination. It does not need to correspond to a particular user's connection.

### 3.4 What if only some users are interested?

Now let's make the requirement slightly more realistic.

Suppose:

- Some users are watching India vs Australia.
- Some users are watching England vs Pakistan.
- Some users are watching football.

We shouldn't send cricket updates to every user in the world.

We can organize events by category or audience:

```
sports-live-updates
       |
       ├── Cricket events
       ├── Football events
       └── Tennis events
```

But remember that our push servers host users with different interests.

We have two options.

Option A: Every push server receives the event and filters locally.

```
Cricket Event
     |
     ▼
All Push Servers
     |
     ├── Push 1 -> Find interested local users
     ├── Push 2 -> Find interested local users
     └── Push 3 -> Find interested local users
```

Simple, but wastes network and processing resources.

Option B: Route events only to push servers with interested users.

```
Cricket Event
     |
     ▼
Cricket Audience Router
     |
     ├── Push 1
     └── Push 3
```

More efficient, but requires tracking which push servers currently have interested users.

This is essentially group/subscription-based routing, which we'll discuss shortly.

### 3.5 What if the push server crashes?

Suppose Push Server 2 crashes.

The other push servers continue receiving broadcasts.

When Push Server 2 restarts, it subscribes again.

But what about events published while it was down?

A normal Pub/Sub system might not retain them.

That's acceptable for certain applications.

For example, a sports dashboard can retrieve the latest score snapshot after reconnecting.

It doesn't necessarily need every intermediate score update.

We will distinguish these delivery guarantees properly in Chapter 6.

### 3.6 A subtle but important point about Kafka

You may think:

"Why not use Kafka for this? Just create a topic and let all push servers consume it."

You can, but understand consumer group semantics.

Suppose all push servers use the same Kafka consumer group:

```
Kafka Topic
    |
    ▼
Consumer Group: push-servers
    |
    ├── Push Server 1
    ├── Push Server 2
    └── Push Server 3
```

Kafka distributes records among consumers in the same group.

A particular event goes to one consumer in that group, not every consumer.

That is not broadcast semantics.

To make every push server receive every event, each push server would need an independent logical subscription, such as its own consumer group.

Conceptually:

```
Kafka Topic
    |
    ├── Consumer Group: push-server-1 -> Push 1
    |
    ├── Consumer Group: push-server-2 -> Push 2
    |
    └── Consumer Group: push-server-3 -> Push 3
```

Now every group independently reads the event stream.

But this can become expensive as the number of push servers grows.

We will explore this distinction in Chapter 4.

Global broadcast takeaway:

When every push server needs the same event, broadcast-oriented Pub/Sub is a natural starting point.

When only a subset of servers needs it, introduce audience-aware routing rather than blindly broadcasting.

# Part B — Group-Based Delivery

## 4. Pattern 2: Deliver only to members of a group

Now consider group chat.

Suppose we have:

```
Group G1:
    User A
    User D
    User F
    User H

Group G2:
    User B
    User C
    User E

Group G3:
    User G
    User I
```

All users are distributed across push servers:

```
Push Server 1:
    User A
    User B
    User C

Push Server 2:
    User D
    User E
    User F

Push Server 3:
    User G
    User H
    User I
```

User A sends a message to Group G1.

```
User A
  |
  ▼
Chat Service
  |
  ▼
Group G1
  |
  ├── User A
  ├── User D
  ├── User F
  └── User H
```

Which push servers should receive the message?

```
Group G1
  |
  ├── User A ──► Push 1
  ├── User D ──► Push 2
  ├── User F ──► Push 2
  └── User H ──► Push 3
```

Therefore:

```
Destination servers = {Push 1, Push 2, Push 3}
```

Notice Push Server 2 appears only once.

Let's design this.

## 4.1 First approach: Resolve the group members

The chat service has the authoritative group membership data.

When a message is sent:

1. Validate that the sender belongs to the group and can send messages.
2. Persist the message.
3. Determine the intended recipients.
4. Publish a group-message event.

The event might look like:

```
{
  "eventId": "event-201",
  "type": "NEW_GROUP_MESSAGE",
  "groupId": "G1",
  "senderId": "user-A",
  "payload": {
    "messageId": "message-501",
    "text": "Hello everyone!"
  }
}
```

Notice we publish `groupId`, rather than putting every recipient's connection information into the business event.

Why?

Because the chat service should not need to know which push server owns each recipient's connection.

The push infrastructure can resolve that.

### Architecture

```
                   Chat Service
                        |
                        | Message persisted
                        |
                        ▼
                  Group Event Stream
                        |
                        ▼
                  Group Fanout Worker
                        |
                        | Get group members
                        ▼
                  Group Membership DB
                        |
                        ▼
                  User IDs
                        |
                        | Lookup connections
                        ▼
                  Connection Registry
                        |
                        ▼
                Unique Push Server IDs
                        |
              ┌─────────┼─────────┐
              |         |         |
              ▼         ▼         ▼
           Push 1    Push 2    Push 3
              |         |         |
              ▼         ▼         ▼
           Local     Local     Local
          delivery  delivery  delivery
```

Let's examine the two lookups.

Lookup 1: Group ID → User IDs

This is done using group membership data.

For example:

```
SELECT user_id
FROM group_members
WHERE group_id = 'G1'
AND status = 'ACTIVE';
```

Lookup 2: User IDs → Push Server IDs

This is done through our connection registry.

Conceptually:

```
User A -> Push 1
User D -> Push 2
User F -> Push 2
User H -> Push 3
```

We deduplicate the destinations.

Now we dispatch the event to Push 1, Push 2, and Push 3.

Each push server delivers it to its locally connected recipients.

### Why is this architecture good?

Because the group membership service and the push infrastructure remain independent.

The chat service decides who belongs to the group.

The push infrastructure decides where those users are connected.

We can reuse the same push infrastructure for:

- Group chats.
- Shared documents.
- Private communities.
- Team notifications.
- Online gaming rooms.
- Live classes.
- Meeting participants.

The audience-selection logic changes, but the connection-routing logic remains the same.

## 4.2 Do we really need to look up every member?

Not always.

Consider a group with 10 members.

Looking up 10 users is simple.

But consider a group with 10 million members.

Resolving 10 million users and checking every connection for every event may become extremely expensive.

We need different strategies for different scales.

### Small and medium groups: Fanout to members

This is the approach we just designed.

```
Group Event
    |
    ▼
Resolve Members
    |
    ▼
Resolve Active Connections
    |
    ▼
Deduplicate Push Servers
    |
    ▼
Dispatch
```

Good for relatively small groups.

### Large groups: Track which push servers have interested connections

Let's introduce another idea.

Suppose:

- Group G1 has 1 million members.
- Only 20,000 members are currently online.
- Those online members are distributed across 100 push servers.

Do we really need to query the entire group membership table and resolve every user connection?

Maybe not.

We can maintain information about which push servers currently host members of a group.

For example:

```
Group G1
   |
   ├── Push 1
   ├── Push 3
   ├── Push 5
   ├── Push 8
   └── Push 17
```

This is a group-to-push-server interest registry.

The information can be maintained dynamically.

Here's how.

Suppose User A belongs to Group G1 and connects to Push Server 1.

Push Server 1 records:

```
Local subscription:

Group G1 -> User A
```

Then it informs the group-interest tracking system:

```
Push Server 1 is interested in Group G1.
```

When User D also joins Group G1 on Push Server 1, we don't need to register Push Server 1 again.

When the last local member of G1 disconnects from Push Server 1, the server removes its interest.

Now when a Group G1 event arrives:

```
Group G1 Event
       |
       ▼
Group Interest Registry
       |
       ├── Push 1
       ├── Push 3
       ├── Push 5
       └── Push 8
```

Only those servers receive the event.

Each server delivers to its local group members.

This avoids resolving every group member for every live event.

However, there is a tradeoff:

We now need to maintain additional distributed state.

It must handle:

- Server crashes.
- Users disconnecting.
- Users joining and leaving groups.
- Expired registrations.
- Race conditions.
- Server restarts.

For this reason, such a registry is usually an optimization, not the authoritative source of group membership.

If it becomes stale, it must be possible to repair it or fall back to authoritative membership data.

## 4.3 Should we create one Pub/Sub topic per group?

This is one of your most important questions.

Suppose we have:

```
group-G1
group-G2
group-G3
group-G4
...
group-G1000000
```

One topic per group might initially look attractive.

Each push server subscribes only to groups its local users are interested in.

For example:

```
Push Server 1 subscribes to:
    group-G1
    group-G3

Push Server 2 subscribes to:
    group-G1
    group-G2

Push Server 3 subscribes to:
    group-G3
```

This is a possible design with a Pub/Sub system that supports dynamic subscriptions.

But should we make it our default architecture?

Usually, no.

Why?

Imagine:

- 10 million groups.
- Thousands of groups per push server.
- Users joining and leaving groups continuously.

You can end up with enormous numbers of dynamic subscriptions, complex subscription lifecycle management, and broker resource pressure.

And if we use Kafka, topics are even heavier logical infrastructure objects than ordinary Pub/Sub channels.

So instead of blindly creating one Kafka topic per group, we usually partition a smaller number of topics using group identifiers as keys.

For example:

```
Topic: group-events

Partitioning key: groupId
```

Conceptually:

```
group-events
    |
    ├── Partition 0: G1, G5, G9, ...
    |
    ├── Partition 1: G2, G6, G10, ...
    |
    ├── Partition 2: G3, G7, G11, ...
    |
    └── Partition 3: G4, G8, G12, ...
```

Related events for the same group can be routed to the same partition.

But notice something:

A Kafka consumer group still distributes partitions among its consumers.

It does not automatically deliver Group G1 events only to push servers hosting Group G1 members.

We still need a fanout/routing layer.

We'll understand exactly how this works in Chapter 4.

### When is a topic/channel per group appropriate?

It can be appropriate when:

- The number of groups is relatively bounded.
- Groups have independently managed subscriptions.
- The broker supports large numbers of lightweight dynamic subscriptions.
- The operational cost is acceptable.

Examples might include a limited number of active game rooms or a collaboration system with a controlled number of active rooms.

But don't make one topic per user or per group your default assumption.

## 4.4 A special case: Collaborative documents

Consider Google Docs-style collaboration.

A document has multiple active collaborators.

```
Document D1:

User A
User B
User C
```

When User A edits the document, we want the other collaborators to receive the operation.

The event could be:

```
{
  "documentId": "D1",
  "operationId": "OP-101",
  "version": 101,
  "type": "INSERT_TEXT",
  "payload": {
    "position": 10,
    "text": "Hello"
  }
}
```

This is similar to group delivery, but there is an important difference.

In collaboration, ordering and consistency can be extremely important.

Suppose these operations occur:

```
OP-101
OP-102
OP-103
```

Clients may need to process them in the correct logical order.

A common architecture is:

```
                  Document Service
                         |
                         ▼
                 Document Event Stream
                         |
                  Partition by documentId
                         |
                         ▼
                Collaboration Routing
                         |
             ┌───────────┼───────────┐
             |           |           |
             ▼           ▼           ▼
          Push 1      Push 2      Push 3
             |           |           |
             ▼           ▼           ▼
          Clients    Clients    Clients
```

The document identifier is a natural routing and ordering key.

We might also maintain:

```
Document D1 -> {
    Push 1,
    Push 2,
    Push 3
}
```

This is another example of group-to-server interest tracking.

But collaborative editing may require more than push delivery:

- Sequence numbers or versions.
- OT or CRDT-based conflict handling.
- Operation acknowledgements.
- Reconnection and resynchronization.
- Periodic snapshots.

The push layer distributes operations. It should not automatically be treated as the authoritative document state.

# Part C — Individual User Delivery

## 5. Pattern 3: Send a message to one particular user

Now let's consider the simplest targeted delivery requirement.

Suppose Service A wants to notify User A.

```
Notification:
    User A -> "Your order has shipped."
```

We already maintain a connection registry.

For example:

```
User A -> Push 1
User B -> Push 2
User C -> Push 1
User D -> Push 3
```

This is a direct lookup problem.

### 5.1 The routing process

Service A publishes an event:

```
{
  "eventId": "event-301",
  "type": "ORDER_SHIPPED",
  "recipientUserId": "user-A",
  "payload": {
    "orderId": "order-123"
  }
}
```

Our delivery router looks up User A.

```
                 Notification Event
                        |
                        ▼
                  User ID: A
                        |
                        ▼
                Connection Registry
                        |
                        ▼
                  User A -> Push 1
                        |
                        ▼
                    Push 1
                        |
                        ▼
                     User A
```

That's it.

We don't need to broadcast the event to every push server.

### 5.2 What if User A has multiple devices?

Suppose the connection registry contains:

```
User A:
    |
    ├── Browser 1 -> Push 1
    ├── Browser 2 -> Push 2
    └── Mobile App -> Push 3
```

What should happen?

It depends on product requirements.

Policy A: Deliver to all active connections

Useful for applications where all devices should immediately reflect the same state.

Policy B: Deliver to one connection

Useful in certain session-exclusive or device-specific workflows.

Policy C: Deliver to selected device types

For example, send only to the mobile application.

Policy D: Deliver to all connections except the sender

Useful for chat messages or collaborative updates where the sender already knows about the operation.

The recipient policy is a business decision.

The connection registry simply gives us the locations.

### 5.3 How should we dispatch to Push Server 1?

There are multiple possible implementations.

Option A: Direct internal RPC

The routing component knows Push Server 1's address and sends it a delivery request.

```
Delivery Router
       |
       | Internal RPC
       ▼
    Push 1
       |
       ▼
    User A
```

This can use HTTP/gRPC or another internal protocol.

Option B: Server-specific Pub/Sub channel

The routing component publishes to:

```
push-server-1
```

Push Server 1 subscribes to its own delivery channel.

```
Delivery Router
       |
       ▼
Channel: push-server-1
       |
       ▼
    Push 1
       |
       ▼
    User A
```

Option C: A partitioned delivery stream

The routing infrastructure distributes events to a set of delivery workers, which subsequently route them to the correct push servers.

This is useful when we need durable processing and controlled throughput.

We'll compare these approaches in Chapter 4.

### 5.4 What if User A is offline?

This is where we need to ask an important product question:

Does this message need to be received even if the user is offline?

Consider two examples.

Example 1: Typing indicator

```
User A is typing...
```

Do we need to save this notification for later?

No.

When User A disconnects, the typing indicator becomes irrelevant.

We can discard it.

Example 2: New chat message

```
Alice: Hello!
```

Do we discard it because Bob is offline?

Absolutely not.

We should persist the chat message in the chat database.

When Bob reconnects, he can retrieve the missing messages.

We might also maintain an unread notification or inbox entry.

The flow becomes:

```
             Chat Service
                  |
                  ▼
           Persist Message
                  |
                  ▼
           Publish Event
                  |
                  ▼
          Connection Lookup
                  |
          ┌───────┴────────┐
          |                |
       Online            Offline
          |                |
          ▼                ▼
      Push Server      No live push
          |                |
          ▼                ▼
       Deliver       Message remains
                       persisted
                            |
                            ▼
                    Fetch on reconnect
```

Notice that the push message and the actual chat message are different things conceptually.

The durable chat message belongs to the chat system.

The real-time push can simply notify the client that new data is available.

For some applications we send the full event, while for others we send an event identifier or a lightweight hint.

This distinction allows real-time delivery to remain independent of durable business storage.

# Part D — Individual Connection Delivery

## 6. Pattern 4: Send to a specific device or connection

Now consider a user with multiple active sessions.

```
User A:
    |
    ├── Connection 101 -> Desktop
    |
    ├── Connection 102 -> Mobile
    |
    └── Connection 103 -> Browser Tab
```

Suppose the user updates a setting on their desktop.

We might want to notify only the desktop connection.

Or suppose a user starts watching a live video on their phone.

We might want to send playback-control events only to that device.

In these cases, user-level targeting is not precise enough.

We need connection-level targeting.

Our registry should support:

```
connectionId -> {
    userId,
    pushServerId,
    deviceId,
    sessionId
}
```

The routing process becomes:

```
Event
  |
  ▼
Connection ID: 101
  |
  ▼
Connection Registry
  |
  ▼
Push Server 1
  |
  ▼
Connection 101
  |
  ▼
Specific device
```

This is useful for:

- Device-specific commands.
- Remote session management.
- Targeted acknowledgements.
- Device-specific application state.
- Controlling a specific browser or client.

One important rule:

A connection ID should not be treated as permanently valid.

When the connection disconnects and reconnects, a new connection can be created.

The new connection must be registered with its new ownership information.

# Part E — Putting the Routing Data Together

## 7. What information should we maintain, and where?

We've now seen four routing patterns.

Let's identify the data each one needs.

### 7.1 Connection registry

A distributed connection registry might conceptually maintain:

```
user:{userId}:connections
    |
    ├── connectionId
    ├── connectionId
    └── connectionId
```

Each connection record contains:

```
{
  "connectionId": "conn-101",
  "userId": "user-A",
  "serverId": "push-1",
  "deviceId": "desktop-1",
  "sessionId": "session-123",
  "connectedAt": 1794312000,
  "leaseExpiresAt": 1794312060
}
```

The actual implementation can use Redis sets, hashes, sorted sets, or another suitable structure.

This is infrastructure metadata, not business data.

### 7.2 Group membership

The chat or collaboration service maintains:

```
group:{groupId}:members
    |
    ├── User A
    ├── User D
    ├── User F
    └── User H
```

This information should generally have an authoritative database.

### 7.3 Group-to-server interest registry

For optimized group fanout:

```
group:{groupId}:servers
    |
    ├── Push 1
    ├── Push 2
    └── Push 3
```

This is derived, ephemeral infrastructure state.

It tells us which push servers currently have active subscribers for a group.

### 7.4 Local connection map

Every push server maintains its own local map.

For example, Push Server 1:

```
userConnections:

User A:
    ├── Connection 101
    └── Connection 103

User B:
    └── Connection 102

groupConnections:

Group G1:
    ├── Connection 101
    └── Connection 102
```

The local map contains actual connection/session references, not just IDs.

It is maintained in the push server's memory.

### Summary

| Information                        | Typical storage                          | Purpose                        |
| ---------------------------------- | ---------------------------------------- | ------------------------------ |
| User → connection metadata         | Redis / distributed registry             | Find active connections        |
| Connection → push server           | Connection registry                      | Find connection owner          |
| Group → users                      | Business database                        | Authoritative group membership |
| Group → active push servers        | Redis / distributed registry             | Optimize group fanout          |
| Connection → local socket          | Push server memory                       | Actually send messages         |
| Durable chat/notification messages | Business database or durable event store | Offline recovery               |

The important architectural principle:

The business database tells us who is eligible to receive an event. The connection registry tells us where eligible users are connected.

# Part F — How Do We Choose Between These Patterns?

## 8. One comparison of all routing strategies

Let's bring everything together.

| Requirement           | Audience resolution                  | Destination routing             |
| --------------------- | ------------------------------------ | ------------------------------- |
| Global broadcast      | Everyone or all relevant subscribers | All relevant push servers       |
| Group delivery        | Group ID → members                   | Members → unique push servers   |
| Large-group delivery  | Group ID → interested servers        | Group interest registry         |
| Individual user       | User ID                              | User's active connection owners |
| Individual device     | Device/session ID                    | Selected connection's owner     |
| Individual connection | Connection ID                        | Exactly one owning push server  |

Notice how the destination lookup changes based on the audience.

We don't need to redesign the connection ownership model every time.

The connection registry stays largely the same.

What changes is how we determine the recipients and destination servers.

## 9. Do we need Pub/Sub for all these scenarios?

No.

This is a very important point.

Pub/Sub is not the routing strategy itself. It is one way to distribute events between components.

We can use Pub/Sub, Kafka, queues, internal RPC, or combinations of them.

Here is a useful starting point:

| Scenario                    | Possible messaging approach                          |
| --------------------------- | ---------------------------------------------------- |
| Global broadcast            | Pub/Sub fanout                                       |
| Global durable event stream | Kafka with independent subscribers or a fanout layer |
| Group chat                  | Durable event stream + group fanout                  |
| Collaborative document      | Ordered, partitioned event stream + targeted fanout  |
| Individual notification     | Durable queue/stream + connection lookup             |
| Ephemeral user event        | Lightweight Pub/Sub or direct routing                |
| Specific device             | Targeted internal RPC or server-directed messaging   |

We'll understand why these choices differ in Chapter 4.

# Part G — Important Production Considerations

## 10. What happens when group membership changes?

Suppose User A is a member of Group G1.

They are connected to Push Server 1.

Then an administrator removes User A from G1.

But Push Server 1 still has this local mapping:

```
Group G1 -> User A
```

What happens?

The push server might continue delivering group events to User A.

This is a security problem.

We need a membership-change propagation mechanism.

For example:

```
             Group Service
                  |
                  | User A removed from G1
                  ▼
             Membership Event
                  |
                  ▼
          Group Subscription Router
                  |
                  ▼
               Push 1
                  |
                  ▼
       Remove User A's G1 subscription
```

The push server updates its local subscription.

For sensitive information, we should also enforce authorization at appropriate boundaries rather than trusting a potentially stale cache indefinitely.

A membership version can help.

For example:

```
Group G1:
    membershipVersion = 105
```

An event and a subscription can carry the relevant version.

When the version changes, consumers can invalidate stale subscriptions or recheck authorization.

The important thing is to define what happens when a removal races with an event already being dispatched.

For example, if an event was authorized before the user's removal took effect, do we allow it to finish delivery? Or must removal take immediate precedence?

That is a product/security consistency requirement, not something Pub/Sub automatically solves.

## 11. What happens when a push server crashes?

Consider:

```
Group G1 -> Push 1
```

Push Server 1 crashes.

But the group interest registry still contains:

```
Group G1 -> Push 1
```

This is stale routing information.

There are several ways to handle it:

- Server leases and expiration.
- Heartbeats from push servers.
- Conditional cleanup when a server shuts down.
- Registry entries associated with server generations.
- Reconciliation after restart.

For example:

```
Push Server 1:
    serverId = push-1
    generation = 17
```

After restarting:

```
Push Server 1:
    serverId = push-1
    generation = 18
```

Old registrations belonging to generation 17 should not be treated as current.

We will study this more deeply when we design the connection registry.

## 12. What happens when a group becomes extremely large?

Suppose Group G1 has 50 million members.

Every message needs to be delivered to 50 million users.

This is no longer just a routing lookup problem.

It becomes a fanout and throughput problem.

For example:

```
             Group G1 Event
                    |
                    ▼
              Fanout Service
                    |
          ┌─────────┼─────────┐
          |         |         |
          ▼         ▼         ▼
       Batch 1   Batch 2   Batch 3
          |         |         |
          ▼         ▼         ▼
       Workers   Workers   Workers
          |         |         |
          ▼         ▼         ▼
       Push     Push      Push
       Servers  Servers   Servers
```

We need to:

- Divide recipients into batches.
- Deduplicate destination servers.
- Control fanout concurrency.
- Apply backpressure.
- Avoid overloading a single push server.
- Handle retries independently.
- Avoid repeatedly delivering the same message unnecessarily.

Also, if the group receives 1,000 events per second, multiplying that by 50 million recipients can become an enormous workload.

Sometimes the correct product architecture is not to push every individual update to every member.

Instead:

- Push periodic summaries.
- Send state-change notifications.
- Let clients fetch the latest snapshot.
- Coalesce updates.
- Use regional fanout.
- Prioritize important events.

This is an important system-design lesson:

Architecture must account for both audience size and event frequency.

A group of 10 million members receiving one daily announcement is very different from a group of 10 million receiving 100 updates per second.

# 13. The architecture patterns at a glance

\#chatgpt-mermaid-\_r_5gg\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_5gg\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_5gg\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_5gg\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_5gg\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_5gg\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_5gg\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_5gg\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_5gg\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_5gg\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_5gg\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_5gg\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_5gg\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_5gg\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_5gg\_ p{margin:0;}#chatgpt-mermaid-\_r_5gg\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_5gg\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_5gg\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_5gg\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_5gg\_ .label text,#chatgpt-mermaid-\_r_5gg\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_5gg\_ .node rect,#chatgpt-mermaid-\_r_5gg\_ .node circle,#chatgpt-mermaid-\_r_5gg\_ .node ellipse,#chatgpt-mermaid-\_r_5gg\_ .node polygon,#chatgpt-mermaid-\_r_5gg\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_5gg\_ .rough-node .label text,#chatgpt-mermaid-\_r_5gg\_ .node .label text,#chatgpt-mermaid-\_r_5gg\_ .image-shape .label,#chatgpt-mermaid-\_r_5gg\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_5gg\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_5gg\_ .rough-node .label,#chatgpt-mermaid-\_r_5gg\_ .node .label,#chatgpt-mermaid-\_r_5gg\_ .image-shape .label,#chatgpt-mermaid-\_r_5gg\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_5gg\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_5gg\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_5gg\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_5gg\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_5gg\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_5gg\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_5gg\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_5gg\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_5gg\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_5gg\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_5gg\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_5gg\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_5gg\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_5gg\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_5gg\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_5gg\_ .icon-shape,#chatgpt-mermaid-\_r_5gg\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_5gg\_ .icon-shape p,#chatgpt-mermaid-\_r_5gg\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_5gg\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_5gg\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_5gg\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_5gg\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_5gg\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_5gg\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_5gg\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_5gg\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_5gg\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_5gg\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_5gg\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_5gg\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_5gg\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_5gg\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_5gg\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_5gg\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_5gg\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_5gg\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_5gg\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_5gg\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_5gg\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_5gg\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_5gg\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_5gg\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_5gg\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_5gg\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_5gg\_ .node rect,#chatgpt-mermaid-\_r_5gg\_ .node circle,#chatgpt-mermaid-\_r_5gg\_ .node ellipse,#chatgpt-mermaid-\_r_5gg\_ .node polygon,#chatgpt-mermaid-\_r_5gg\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_5gg\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_5gg\_ .mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_5gg\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_5gg\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_5gg\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}Business EventAudience TypeBroadcast ChannelAll Relevant Push ServersGroup Fanout / InterestRouterGroup Membership or ActiveServer LookupOnly Relevant Push ServersConnection Registry LookupServers Owning UserConnectionsConnection ID LookupOne Owning Push ServerLocal Connection LookupClient ConnectionsGlobalGroupIndividual UserSpecific Connection

Notice that all four patterns converge at the same point:

Local connection lookup and delivery.

That is why we can maintain one reusable push server infrastructure while supporting many different applications.

# 14. The decision framework I want you to internalize

Whenever you see a real-time messaging requirement, don't immediately think:

"Should I use Kafka or Redis Pub/Sub?"

That's the wrong first question.

Think in this order:

Question 1: Who is the audience?

- Everyone?
- A group?
- One user?
- One device?
- One connection?

Question 2: How do I identify that audience?

- Global subscription?
- Group membership database?
- User ID?
- Device ID?
- Connection ID?

Question 3: How do I map recipients to connection owners?

Use the connection registry.

Question 4: How should messages reach the destination push servers?

Possible approaches include:

- Broadcast Pub/Sub.
- Group fanout.
- Server-specific Pub/Sub.
- Internal RPC.
- Partitioned message streams.

Question 5: What if recipients are offline?

- Discard ephemeral events.
- Persist important messages.
- Replay or retrieve missed data after reconnecting.

Question 6: How reliable does delivery need to be?

- Best effort?
- At least once?
- Ordered delivery?
- Application acknowledgement?
- Durable retry?

These questions naturally lead us to the correct architecture.

## Chapter 3 — What you should now be able to explain

Let's take one final example.

We have 100 push servers and 10 million connected users.

A service produces three events:

```
Event 1: Global announcement

Event 2: New message in Group G1

Event 3: Personal notification for User A
```

The routing should look like this:

| Event               | Routing                                                                       |
| ------------------- | ----------------------------------------------------------------------------- |
| Global announcement | Broadcast to all relevant push servers                                        |
| Group G1 message    | Resolve G1's audience and dispatch to relevant push servers                   |
| User A notification | Resolve User A's active connections and dispatch to their owning push servers |

We do not automatically broadcast all three events to all 100 servers.

We do not automatically create three separate Kafka topics.

We do not need one Kafka topic per user or per group.

We have separated audience selection from connection routing.

That is the foundation.

# Next: Chapter 4 — Pub/Sub vs Kafka vs Message Queues

Now we'll tackle the biggest architectural question in your original design.

We'll start from the actual messaging requirements and derive why different messaging systems exist.

We'll cover:

1. What Pub/Sub actually means and how subscriptions work.
2. Why a normal message queue behaves differently from Pub/Sub.
3. Kafka topics, partitions, offsets, and consumer groups.
4. Why Kafka consumers in the same group do not all receive the same event.
5. How to broadcast Kafka events to multiple independent push-server instances.
6. How to route individual and group events without creating millions of topics.
7. Whether to use Redis Pub/Sub, Redis Streams, Kafka, RabbitMQ, or a combination.
8. How to combine durable event processing with low-latency push delivery.

The central question we'll solve: How do we choose the messaging backbone based on the delivery guarantees and routing requirements, rather than selecting Kafka or Pub/Sub simply because we need real-time communication?

