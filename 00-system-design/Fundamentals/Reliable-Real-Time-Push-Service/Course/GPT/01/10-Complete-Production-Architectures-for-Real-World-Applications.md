# Chapter 10 — Complete Production Architectures for Real-World Applications

Now comes the most important practical part of our learning journey.

We've learned all the individual components and their responsibilities.

Now we'll take actual applications and derive their architectures from their requirements.

The objective is not to memorize eight architectures.

It's to understand why a particular application needs a particular combination of connections, messaging, storage, routing, and reliability mechanisms.

We'll use the same design process for every application:

1. Understand the communication requirements.
2. Identify the audience.
3. Identify which events matter and which can be discarded.
4. Choose WebSocket or SSE.
5. Choose the messaging architecture.
6. Choose the authoritative storage.
7. Design routing and fanout.
8. Define delivery guarantees.
9. Handle failures and scaling.

## 1. First, understand the eight application patterns

Before designing them, let's classify their fundamental requirements.

| Application          | Communication           | Audience               | Primary challenge                     |
| -------------------- | ----------------------- | ---------------------- | ------------------------------------- |
| Chat                 | Bidirectional           | Individual/group       | Durable messages and ordering         |
| Collaborative editor | Bidirectional           | Document participants  | Concurrent operations and consistency |
| Live sports          | Mostly server-to-client | Match subscribers      | Massive broadcast                     |
| Stock market         | Mostly server-to-client | Symbol subscribers     | High-frequency updates                |
| Notifications        | Mostly server-to-client | Individual user/device | Durable delivery and acknowledgement  |
| Online gaming        | Bidirectional           | Players in a room      | Extremely low latency                 |
| Live tracking        | Bidirectional or SSE    | Selected users         | Frequent location updates             |
| Global dashboard     | Mostly server-to-client | Tenant/subscription    | Selective fanout and aggregation      |

Notice something.

All these applications need real-time delivery.

But the underlying problem is different.

We should not design all of them using the same messaging strategy.

Let's start with the more complicated ones.

# Part A — Design a Group Chat Application

## 2. Understand the requirements

Imagine we're designing a WhatsApp/Slack-style chat application.

Users can:

- Send messages to individuals.
- Send messages to groups.
- Have multiple active devices.
- Receive messages in real time.
- View message history.
- Receive messages while offline.
- See delivery/read receipts.
- Reconnect and recover missed messages.

Let's translate these into architectural requirements.

| Requirement                     | Architectural implication                   |
| ------------------------------- | ------------------------------------------- |
| Bidirectional communication     | WebSocket                                   |
| Durable chat history            | Chat database                               |
| Group messages                  | Group-based fanout                          |
| Multiple devices                | User → multiple connections                 |
| Offline users                   | Durable message history and synchronization |
| Message ordering                | Conversation-level sequence numbers         |
| Message publication reliability | Transactional Outbox                        |
| Large groups                    | Group-to-active-server routing              |
| Delivery/read receipts          | Separate receipt semantics                  |

WebSocket is a natural choice because users continuously send and receive messages.

SSE could technically deliver incoming messages, while outgoing messages use HTTP, but WebSocket is generally a more convenient communication model for interactive chat.

## 3. Build the architecture

Let's begin with a small system.

```
               Alice's Client
                     |
                     | WebSocket
                     ▼
                 Push Gateway
                     |
                     ▼
                 Chat Service
                     |
                     ▼
                  Chat DB
```

Should the push gateway directly implement chat functionality?

I'd separate them.

The gateway handles transport.

The chat service handles business logic.

The chat service decides:

- Whether the sender belongs to the conversation.
- Whether the sender is allowed to send.
- What message ID to assign.
- What sequence number to allocate.
- How the message should be persisted.

The gateway doesn't need to know these business rules.

### Our complete architecture

\#chatgpt-mermaid-\_r_b4m\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_b4m\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_b4m\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_b4m\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_b4m\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_b4m\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_b4m\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_b4m\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_b4m\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_b4m\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_b4m\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_b4m\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_b4m\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_b4m\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_b4m\_ p{margin:0;}#chatgpt-mermaid-\_r_b4m\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_b4m\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_b4m\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_b4m\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_b4m\_ .label text,#chatgpt-mermaid-\_r_b4m\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_b4m\_ .node rect,#chatgpt-mermaid-\_r_b4m\_ .node circle,#chatgpt-mermaid-\_r_b4m\_ .node ellipse,#chatgpt-mermaid-\_r_b4m\_ .node polygon,#chatgpt-mermaid-\_r_b4m\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_b4m\_ .rough-node .label text,#chatgpt-mermaid-\_r_b4m\_ .node .label text,#chatgpt-mermaid-\_r_b4m\_ .image-shape .label,#chatgpt-mermaid-\_r_b4m\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_b4m\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_b4m\_ .rough-node .label,#chatgpt-mermaid-\_r_b4m\_ .node .label,#chatgpt-mermaid-\_r_b4m\_ .image-shape .label,#chatgpt-mermaid-\_r_b4m\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_b4m\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_b4m\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_b4m\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_b4m\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_b4m\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_b4m\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_b4m\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_b4m\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_b4m\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_b4m\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_b4m\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_b4m\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_b4m\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_b4m\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_b4m\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_b4m\_ .icon-shape,#chatgpt-mermaid-\_r_b4m\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_b4m\_ .icon-shape p,#chatgpt-mermaid-\_r_b4m\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_b4m\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_b4m\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_b4m\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_b4m\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_b4m\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_b4m\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_b4m\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_b4m\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_b4m\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_b4m\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_b4m\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_b4m\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_b4m\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_b4m\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_b4m\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_b4m\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_b4m\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_b4m\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_b4m\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_b4m\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_b4m\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_b4m\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_b4m\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_b4m\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_b4m\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_b4m\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_b4m\_ .node rect,#chatgpt-mermaid-\_r_b4m\_ .node circle,#chatgpt-mermaid-\_r_b4m\_ .node ellipse,#chatgpt-mermaid-\_r_b4m\_ .node polygon,#chatgpt-mermaid-\_r_b4m\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_b4m\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_b4m\_ .mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_b4m\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_b4m\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_b4m\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}Alice ClientLoad BalancerPush GatewayChat ServiceChat DatabaseTransactional OutboxKafka: chat-eventsChat Fanout RouterConnection RegistryGroup FanoutPush Server 1Push Server 2Push Server 3Bob DesktopBob MobileOther Participants

Let's follow one message.

## 4. Step-by-step: Alice sends a message

Suppose Alice sends:

```
Hello Bob!
```

The client sends:

```
{
  "type": "SEND_MESSAGE",
  "clientMessageId": "alice-msg-101",
  "conversationId": "conv-10",
  "text": "Hello Bob!"
}
```

Notice `clientMessageId`.

Why do we need it?

Suppose Alice sends the message.

The server persists it.

But the connection breaks before Alice receives confirmation.

Alice's application doesn't know whether the message was accepted.

It reconnects and retries.

Without an idempotency key, we could create two messages.

So the chat service uses:

```
UNIQUE(senderId, clientMessageId)
```

The same logical client request cannot create multiple messages.

### Step 1: Authenticate and authorize

The gateway knows:

```
Alice is authenticated.
```

The chat service checks:

```
Alice belongs to conv-10.

Alice is allowed to send messages.

Conversation is active.
```

### Step 2: Allocate message sequence

Suppose:

```
Current conversation sequence = 100
```

The new message receives:

```
sequence = 101
```

The sequence should be assigned by an authoritative mechanism.

For a conversation, one approach is to lock/update a conversation metadata row in the same transaction as message insertion.

For example:

```
UPDATE conversations
SET next_sequence = next_sequence + 1
WHERE conversation_id = 'conv-10'
RETURNING next_sequence;
```

In practice, message insertion and sequence allocation must be part of the same transaction, with the appropriate concurrency control.

This ensures that concurrent message sends don't accidentally receive conflicting conversation sequence numbers.

### Step 3: Persist the message and outbox event

We write:

Messages table

| message_id | conversation_id | sequence | sender | text       |
| ---------- | --------------- | -------- | ------ | ---------- |
| msg-101    | conv-10         | 101      | Alice  | Hello Bob! |

Outbox table

| event_id | event_type      | status  |
| -------- | --------------- | ------- |
| evt-101  | MESSAGE_CREATED | PENDING |

Both are written in one database transaction.

```
Chat DB Transaction
        |
        ├── Insert message
        |
        ├── Allocate sequence 101
        |
        └── Insert outbox event
                 |
               COMMIT
```

Only after successful commit should Alice receive the server's message-accepted acknowledgement.

For example:

```
{
  "type": "MESSAGE_ACCEPTED",
  "clientMessageId": "alice-msg-101",
  "messageId": "msg-101",
  "sequence": 101
}
```

This means the message is durably accepted by the chat service.

It does not mean Bob has received it.

That's an important distinction.

### Step 4: Publish through Kafka

The outbox publisher publishes:

```
{
  "eventId": "evt-101",
  "type": "MESSAGE_CREATED",
  "conversationId": "conv-10",
  "messageId": "msg-101",
  "sequence": 101,
  "senderId": "alice"
}
```

The Kafka configuration:

```
Topic: chat-events

Key: conversationId
```

Why `conversationId`?

Because messages belonging to the same conversation should be processed in their intended order.

Events for different conversations can be processed in parallel.

### Step 5: Resolve recipients

Suppose:

```
Conversation conv-10:

Alice
Bob
Charlie
David
```

The fanout router obtains the eligible recipients.

We might exclude Alice's sending connection or all of Alice's devices, depending on how the sender's UI is updated.

The other recipients are:

```
Bob
Charlie
David
```

### Step 6: Resolve active connections

Our connection registry returns:

```
Bob:
    Desktop -> Push 1
    Mobile  -> Push 2

Charlie:
    Browser -> Push 2

David:
    No active connection
```

Destination servers:

```
Push 1:
    Bob Desktop

Push 2:
    Bob Mobile
    Charlie Browser
```

David is offline.

We don't need to keep trying to send him a live WebSocket message.

His message already exists in the chat database.

### Step 7: Deliver

Push Server 1 receives:

```
{
  "conversationId": "conv-10",
  "messageId": "msg-101",
  "sequence": 101
}
```

It sends the message to Bob's desktop.

Push Server 2 sends it to Bob's mobile and Charlie's browser.

### Step 8: Delivery receipts

Suppose Bob's desktop and mobile acknowledge receipt.

We can track:

```
msg-101:

Bob Desktop -> RECEIVED
Bob Mobile  -> RECEIVED
Charlie     -> PENDING / OFFLINE
```

A chat application's product may define delivery at the user level instead of separately for every device.

If so, one qualifying device acknowledgement may be sufficient.

Read receipts are separate:

```
Bob -> READ

Charlie -> UNREAD
```

The chat service, rather than the push gateway, owns the authoritative receipt semantics.

## 5. What happens when Bob is offline?

This is where chat differs from a live scoreboard.

Suppose Bob is offline.

Alice sends:

```
Message 101
Message 102
Message 103
```

All three messages are persisted.

The push system doesn't need to deliver them immediately.

When Bob reconnects:

```
Bob reconnects
      |
      ▼
Push Gateway
      |
      ▼
Chat Synchronization API
      |
      ▼
Fetch messages after last sequence
      |
      ▼
Message 101
Message 102
Message 103
```

For example:

```
GET /conversations/conv-10/messages?afterSequence=100
```

Response:

```
{
  "messages": [
    {"sequence": 101, "text": "Hello"},
    {"sequence": 102, "text": "How are you?"},
    {"sequence": 103, "text": "Are you there?"}
  ],
  "nextSequence": 103
}
```

The client processes them and updates its cursor:

```
lastProcessedSequence = 103
```

Now it receives new messages live.

### What if messages arrive while synchronization is happening?

Establish the live subscription before the replay high-water mark, or use an equivalent gap-free handoff protocol.

For example:

1. Establish live subscription.
2. Buffer incoming live events.
3. Read the conversation's current high-water sequence.
4. Fetch messages after the client's cursor through that high-water mark.
5. Process the replay.
6. Process buffered live events, deduplicating by sequence.

This prevents messages from disappearing between historical synchronization and live delivery.

## 6. What storage does chat actually need?

| Information                       | Storage                           |
| --------------------------------- | --------------------------------- |
| Actual message                    | Chat DB                           |
| Conversation sequence             | Chat DB                           |
| Group members                     | Chat DB                           |
| Message-created event             | Kafka                             |
| Pending outbox records            | Chat DB                           |
| Active connections                | Redis                             |
| Actual sockets                    | Push-server memory                |
| Presence                          | Redis                             |
| Per-device synchronization cursor | Chat DB or KV                     |
| Delivery/read receipts            | Chat DB or suitable receipt store |

Notice:

We don't necessarily need a separate delivery database for every chat message.

The authoritative chat history provides the recovery mechanism.

We might add delivery records for explicit receipt or operational requirements, but that is a product decision.

### Chat architecture summary

```
             Client
               |
            WebSocket
               |
               ▼
          Push Gateway
               |
               ▼
           Chat Service
               |
          ┌────┴─────┐
          |          |
          ▼          ▼
       Chat DB     Outbox
          |          |
          |          ▼
          |        Kafka
          |          |
          |          ▼
          |      Fanout Router
          |          |
          |          ▼
          |    Connection Registry
          |          |
          |          ▼
          |      Push Servers
          |          |
          ▼          ▼
       History     Live Delivery
          |
          ▼
      Reconnect Sync
```

The important lesson: Chat needs durable business messages and synchronization. WebSockets merely make delivery immediate.

# Part B — Design a Collaborative Document Editor

## 7. Understand the requirements

Now let's build a Google Docs/Figma/whiteboard-style collaborative application.

Suppose three users are simultaneously editing Document D1.

```
Document D1:

Alice
Bob
Charlie
```

When Alice moves an object or inserts text, the other participants need to see the change almost immediately.

But collaboration has additional challenges.

- Multiple users edit simultaneously.
- Operations might arrive out of order.
- Two users might modify the same logical location.
- Clients can disconnect temporarily.
- Users can reconnect with outdated document state.
- Every operation might need to be recoverable.

This is significantly different from chat.

In chat, the message is an independent entry.

In collaboration, an event often changes a shared state that subsequent operations depend on.

For example:

```
Document version 100:

A rectangle exists at position (10, 10).
```

Alice moves it:

```
Operation 101:
    MOVE rectangle to (20, 20)
```

Bob simultaneously moves the same rectangle:

```
Operation 102:
    MOVE rectangle to (30, 30)
```

Which position should be the final one?

Simply broadcasting both operations doesn't answer this.

We need an application-level consistency model.

## 8. First separate the three responsibilities

A collaborative system has three different concerns.

### A. Authoritative document state

What is the latest document content?

```
Document D1:
    version = 1001
    objects = [...]
```

This belongs to the document/collaboration service and its persistence layer.

### B. Collaboration session state

Who is currently connected to the document?

```
Document D1:
    Alice -> Push 1
    Bob -> Push 2
    Charlie -> Push 3
```

This is ephemeral subscription information.

It can be maintained through Redis or an active-server registry.

### C. Operation history

What changes have been applied to the document?

```
D1:

OP-101
OP-102
OP-103
OP-104
```

Depending on the collaboration model, we need to persist operations so we can reconstruct state or recover clients.

These three concerns are related, but they should not be confused.

## 9. Understand why OT and CRDT matter

Before discussing messaging, we need to understand the nature of concurrent edits.

There are two major approaches.

### Approach A: Operational Transformation (OT)

OT systems transform concurrent operations so they remain meaningful after applying them in a different order.

Imagine Alice and Bob both edit the same text document.

Initial content:

```
Hello
```

Alice inserts `A` at position 0.

Bob inserts `B` at position 0.

The operations are concurrent.

If both clients apply these operations independently without coordination, their results might diverge.

OT transforms operations to account for previously applied operations.

Conceptually:

```
Original operation
        |
        ▼
Check current document version
        |
        ▼
Transform against concurrent operations
        |
        ▼
Apply transformed operation
        |
        ▼
Broadcast
```

### Approach B: Conflict-Free Replicated Data Types (CRDTs)

CRDTs use data structures and operation semantics designed so that replicas can converge despite concurrent updates and different delivery orders, subject to the chosen CRDT's rules and eventual exchange of updates.

For example, certain collaborative text structures assign stable identifiers to elements instead of relying only on numeric positions.

This makes concurrent insertions easier to reconcile.

### Which should we choose?

Neither is universally better.

| Consideration         | OT                                                             | CRDT                                         |
| --------------------- | -------------------------------------------------------------- | -------------------------------------------- |
| Conflict handling     | Transforms operations                                          | Uses convergence-aware data structures       |
| Coordination          | Often uses a central authority or ordered operation processing | Can support more decentralized replication   |
| Implementation        | Complex transformation logic                                   | Complex data structure and metadata design   |
| Offline collaboration | Possible, with appropriate transform/reconciliation design     | Often a strong fit                           |
| Storage               | Operation log and snapshots                                    | Operations/state plus metadata and snapshots |

The key decision:

The push infrastructure should transport collaboration operations, but the collaboration algorithm determines how concurrent edits are reconciled.

WebSockets alone cannot solve concurrent document editing.

## 10. Build the collaborative editing architecture

Let's start with a centralized collaboration service.

```
Alice Client
     |
     ▼
Push Gateway
     |
     ▼
Collaboration Service
     |
     ▼
Document State
```

But if several servers are handling different users editing the same document, how do we preserve a coherent operation order?

One practical starting architecture is:

Assign each document to an authoritative collaboration processor or logical owner.

For example:

```
Document D1 -> Collaboration Worker 1

Document D2 -> Collaboration Worker 2

Document D3 -> Collaboration Worker 1
```

This is similar to assigning conversations to ordered processing partitions.

A document's operations pass through its authoritative processing path.

### Complete architecture

\#chatgpt-mermaid-\_r_b9j\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_b9j\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_b9j\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_b9j\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_b9j\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_b9j\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_b9j\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_b9j\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_b9j\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_b9j\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_b9j\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_b9j\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_b9j\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_b9j\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_b9j\_ p{margin:0;}#chatgpt-mermaid-\_r_b9j\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_b9j\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_b9j\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_b9j\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_b9j\_ .label text,#chatgpt-mermaid-\_r_b9j\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_b9j\_ .node rect,#chatgpt-mermaid-\_r_b9j\_ .node circle,#chatgpt-mermaid-\_r_b9j\_ .node ellipse,#chatgpt-mermaid-\_r_b9j\_ .node polygon,#chatgpt-mermaid-\_r_b9j\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_b9j\_ .rough-node .label text,#chatgpt-mermaid-\_r_b9j\_ .node .label text,#chatgpt-mermaid-\_r_b9j\_ .image-shape .label,#chatgpt-mermaid-\_r_b9j\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_b9j\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_b9j\_ .rough-node .label,#chatgpt-mermaid-\_r_b9j\_ .node .label,#chatgpt-mermaid-\_r_b9j\_ .image-shape .label,#chatgpt-mermaid-\_r_b9j\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_b9j\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_b9j\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_b9j\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_b9j\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_b9j\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_b9j\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_b9j\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_b9j\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_b9j\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_b9j\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_b9j\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_b9j\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_b9j\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_b9j\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_b9j\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_b9j\_ .icon-shape,#chatgpt-mermaid-\_r_b9j\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_b9j\_ .icon-shape p,#chatgpt-mermaid-\_r_b9j\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_b9j\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_b9j\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_b9j\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_b9j\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_b9j\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_b9j\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_b9j\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_b9j\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_b9j\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_b9j\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_b9j\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_b9j\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_b9j\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_b9j\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_b9j\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_b9j\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_b9j\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_b9j\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_b9j\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_b9j\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_b9j\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_b9j\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_b9j\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_b9j\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_b9j\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_b9j\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_b9j\_ .node rect,#chatgpt-mermaid-\_r_b9j\_ .node circle,#chatgpt-mermaid-\_r_b9j\_ .node ellipse,#chatgpt-mermaid-\_r_b9j\_ .node polygon,#chatgpt-mermaid-\_r_b9j\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_b9j\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_b9j\_ .mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_b9j\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_b9j\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_b9j\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}AlicePush Gateway 1BobPush Gateway 2CharliePush Gateway 3Collaboration ServiceDocument OperationProcessorDocument DBOperation LogKafka: document-operationsDocument Fanout RouterDocument Active ServerRegistryPush Server 1Push Server 2Push Server 3

Let's follow Alice's edit.

## 11. Step-by-step: Alice edits Document D1

Alice sends:

```
{
  "type": "DOCUMENT_OPERATION",
  "documentId": "D1",
  "operationId": "op-101",
  "baseVersion": 100,
  "operation": {
    "type": "MOVE_OBJECT",
    "objectId": "rectangle-1",
    "x": 20,
    "y": 20
  }
}
```

### Step 1: Authenticate and authorize

The collaboration service checks:

```
Alice has access to D1.

Alice has permission to modify D1.
```

### Step 2: Route to the document owner

We identify:

```
D1 -> Collaboration Worker 1
```

The request is sent to the authoritative document processor.

### Step 3: Reconcile the operation

Suppose the current document version is 100.

Alice's operation is based on version 100.

We can apply it.

If the current version is already 102, the operation needs to be transformed, merged, or rejected/reconciled according to the collaboration protocol.

This is where OT or CRDT logic operates.

### Step 4: Assign a document sequence

The authoritative processor creates:

```
operationId = op-101

documentVersion = 101
```

### Step 5: Persist the operation

We maintain an operation log:

| Operation | Document | Version | Type   |
| --------- | -------- | ------- | ------ |
| OP-99     | D1       | 99      | INSERT |
| OP-100    | D1       | 100     | DELETE |
| OP-101    | D1       | 101     | MOVE   |

We may also persist the resulting document state or periodically create snapshots.

### Step 6: Publish the document operation

We publish:

```
{
  "eventId": "evt-101",
  "documentId": "D1",
  "version": 101,
  "operationId": "op-101",
  "operation": {
    "type": "MOVE_OBJECT",
    "objectId": "rectangle-1",
    "x": 20,
    "y": 20
  }
}
```

Kafka:

```
Topic: document-operations

Key: documentId
```

This keeps document operations within a logical ordered stream.

The exact authoritative sequence assignment should be owned by the document processor, not inferred solely from Kafka arrival order when multiple clients submit concurrent operations.

### Step 7: Route to active participants

The active-server registry contains:

```
D1:
    Push 1
    Push 2
    Push 3
```

The router dispatches the operation to these servers.

Each server delivers to its locally subscribed clients.

### Step 8: Clients apply the operation

Bob receives:

```
{
  "documentId": "D1",
  "version": 101,
  "operationId": "op-101",
  "operation": {
    "type": "MOVE_OBJECT",
    "objectId": "rectangle-1",
    "x": 20,
    "y": 20
  }
}
```

His client applies the operation.

It updates:

```
lastAppliedVersion = 101
```

The document's state now converges according to the chosen collaboration algorithm.

## 12. What if Bob disconnects and reconnects?

Suppose Bob was last at:

```
Document version = 100
```

Alice and Charlie have already produced:

```
101
102
103
104
```

Bob reconnects.

He sends:

```
{
  "type": "RESUME_DOCUMENT",
  "documentId": "D1",
  "lastAppliedVersion": 100
}
```

The collaboration service checks whether the missing operations are available.

### Case A: Operations are available

Replay:

```
101
102
103
104
```

Bob catches up.

### Case B: The operation history has been compacted

Suppose we retain only operations after version 500.

Bob has version 100.

We shouldn't replay thousands of old operations individually.

Instead:

1. Send a current snapshot at version 600.
2. Apply operations 601 onward.
3. Resume live delivery.

This gives us a snapshot-plus-log architecture.

```
Document Snapshot
    |
    ▼
Version 600
    |
    ▼
Operations 601-650
    |
    ▼
Current State
```

This is a common way to keep collaborative systems recoverable without retaining an infinitely long uncompressed operation history.

## 13. What should we persist in a collaborative editor?

| Data                            | Storage                                       |
| ------------------------------- | --------------------------------------------- |
| Document's authoritative state  | Document DB/object storage, depending on size |
| Operation history               | Durable operation log                         |
| Snapshots                       | Document database/object storage              |
| Document membership/permissions | Document service database                     |
| Active participants             | Redis/ephemeral registry                      |
| Document → active push servers  | Redis                                         |
| Live operation events           | Kafka or suitable ordered event stream        |
| Actual connections              | Push-server memory                            |
| User cursor/selection           | Often ephemeral state                         |

Notice another important distinction.

A user's cursor position changes frequently.

We don't necessarily need to persist every cursor movement.

For example:

```
{
  "type": "CURSOR_UPDATE",
  "documentId": "D1",
  "userId": "alice",
  "position": 150
}
```

This can often travel through ephemeral Pub/Sub.

If Bob misses a cursor update while offline, that's fine.

The current cursor position can be refreshed.

But a durable document edit is different.

We need to preserve operations or their resulting state.

### Separate event categories

| Event                       | Delivery model                                                       |
| --------------------------- | -------------------------------------------------------------------- |
| Text insertion              | Durable, ordered                                                     |
| Object movement             | Depends on collaboration semantics; may be coalesced in some systems |
| Cursor movement             | Ephemeral                                                            |
| User joined document        | Presence/subscription update                                         |
| User left document          | Presence/subscription update                                         |
| Document permission revoked | Authorization invalidation                                           |
| Snapshot created            | Durable document state                                               |

This distinction is especially important in collaborative applications.

Not every message exchanged over a collaboration WebSocket deserves a Kafka record.

Persisting every mouse movement would create unnecessary data and processing overhead.

## 14. Do we need Kafka for collaboration?

Not necessarily.

The answer depends on the durability and scale requirements.

### Simpler collaboration system

```
Client
  |
  ▼
Collaboration Service
  |
  ├── Document DB
  |
  └── Redis Pub/Sub
          |
          ▼
      Push Servers
          |
          ▼
       Clients
```

This may be enough when:

- The collaboration engine already owns ordered document state.
- Operation history is persisted directly.
- Pub/Sub is only a live notification mechanism.
- Missing live notifications can be reconstructed.

### Larger durable collaboration system

```
Client
  |
  ▼
Collaboration Service
  |
  ▼
Operation Log + Transactional Outbox
  |
  ▼
Kafka (Key = documentId)
  |
  ▼
Collaboration Router
  |
  ▼
Active Document Servers
  |
  ▼
Push Servers
  |
  ▼
Clients
```

Kafka provides:

- Durable event processing.
- Partitioned processing.
- Independent consumers.
- Replay.
- Scalable distribution.

But Kafka does not replace the collaboration algorithm or authoritative document storage.

The document processor still needs to determine the correct operation semantics.

# Part C — Comparing Chat and Collaborative Editing

This is a useful comparison because both applications use WebSockets and groups, yet their architecture differs.

| Concern               | Chat                         | Collaborative editor                         |
| --------------------- | ---------------------------- | -------------------------------------------- |
| Communication         | WebSocket                    | WebSocket                                    |
| Primary object        | Message                      | Operation                                    |
| Ordering key          | Conversation ID              | Document ID                                  |
| Durable data          | Messages                     | Operations and snapshots                     |
| Concurrent changes    | Usually independent messages | Shared-state conflicts                       |
| Consistency mechanism | Sequence numbers             | OT/CRDT or authoritative operation processor |
| Live routing          | Conversation members         | Active document participants                 |
| Offline recovery      | Fetch missing messages       | Replay operations or snapshot                |
| Typing/cursor events  | Ephemeral                    | Ephemeral                                    |
| Separate delivery DB  | Optional                     | Usually not required for every operation     |

The important lesson:

Chat is primarily a durable message-distribution problem. Collaborative editing is a shared-state consistency problem that also requires message distribution.

We cannot solve the second problem merely by choosing Kafka or WebSockets.

# Part D — Reusable Patterns We Have Identified

## 15. What have we reused from our earlier chapters?

Notice that we didn't invent a new push infrastructure for chat or collaboration.

We reused:

```
Load Balancer
    |
    ▼
Push Servers
    |
    ▼
Local Connection Maps
    |
    ▼
Connection Registry
```

We reused the same messaging principles:

- Kafka for durable event processing when required.
- Pub/Sub for ephemeral fanout.
- Durable database state for recovery.
- Connection registry for targeted routing.

What changed was the domain-specific layer:

```
Chat:
    Conversation membership
    Message persistence
    Conversation sequence


Collaboration:
    Document membership
    OT/CRDT
    Document version
    Operation log
    Snapshot
```

This is exactly how you should approach system design.

Start with reusable infrastructure.

Then introduce only the domain-specific mechanisms required by the problem.

## Chapter 10 — Part 1: What you should retain

We've designed two real-world systems.

Chat application:

```
WebSocket
+ Chat DB
+ Transactional Outbox
+ Kafka
+ Conversation fanout
+ Connection Registry
+ Offline message synchronization
```

Collaborative editor:

```
WebSocket
+ Collaboration Service
+ OT/CRDT
+ Ordered Operations
+ Operation Log
+ Snapshots
+ Document-to-server routing
+ Client replay
```

Both use the same connection infrastructure.

But their consistency and durability models are different.

# Next: Chapter 10 — Part 2

We'll design the applications that introduce very different fanout and throughput challenges:

### 1. Live sports application

How to deliver a cricket score update to 10 million users without unnecessarily overwhelming Kafka, Redis, and push servers.

We'll cover match-specific topics, regional broadcasting, snapshots, and coalescing.

### 2. Stock market application

How to handle thousands of price updates per second, symbol subscriptions, selective fanout, high-frequency data, and slow consumers.

### 3. Notification platform

How to design personal notifications with a durable inbox, multiple devices, push channels, retries, acknowledgements, and offline delivery.

The key comparison will be:

Why a live sports update, a stock price update, and a critical personal notification should not use the same delivery guarantee—even though all three are delivered through the same WebSocket/SSE infrastructure.






# Chapter 10 — Part 2: Live Sports, Stock Markets, and Notification Systems

In Part 1, we designed chat and collaborative editing.

Both required relatively strong durability, but their consistency requirements differed.

Now let's study three systems that introduce different challenges:

1. Live sports: Millions of users receive the same information simultaneously.
2. Stock market: A large number of frequently changing values must be delivered selectively.
3. Notifications: Individual messages must remain available and recoverable, even when users are offline.

All three can use the same underlying push infrastructure.

But their routing, messaging, storage, and delivery guarantees should be different.

# Part E — Design a Live Sports Application

## 1. Understand the requirements

Imagine we're building a live cricket application.

Users can:

- Follow different cricket matches.
- Receive live scores.
- Receive ball-by-ball updates.
- Watch commentary.
- View changing leaderboards.
- Receive important match announcements.

Suppose:

```
Total users: 20 million

Users following India vs Australia: 5 million

Users following England vs Pakistan: 2 million

Users following other matches: 3 million
```

Not every user wants every match's updates.

Our first architectural decision is:

Audience = Users subscribed to a particular match or event category.

For example:

```
India vs Australia
    |
    ├── User A
    ├── User B
    ├── User C
    ├── ...
    └── User 5,000,000
```

This is group-based routing.

But the group is potentially enormous.

We should not enumerate five million user IDs every time a ball is bowled.

## 2. First identify the event types

Not every sports update needs the same treatment.

Consider:

| Event              | Example                 | Delivery requirement             |
| ------------------ | ----------------------- | -------------------------------- |
| Score snapshot     | India 180/4             | Latest state matters             |
| Ball-by-ball event | Batter hits a four      | Event history matters            |
| Commentary         | "Excellent cover drive" | Usually best effort              |
| Match status       | Match interrupted       | Important state change           |
| Leaderboard        | Top 100 players         | Latest snapshot often sufficient |
| Match ended        | India won               | Durable result                   |

This distinction will influence how we process and deliver events.

For example, suppose the score changes five times in one second.

A dashboard displaying the current score might only need the latest state.

But a ball-by-ball commentary feed should preserve every meaningful event.

A single sports application can contain several different delivery semantics.

## 3. Build the basic architecture

Let's assume we have an authoritative sports data provider or our own scoring system.

It produces:

```
{
  "matchId": "match-101",
  "eventId": "evt-501",
  "type": "SCORE_UPDATE",
  "score": {
    "team": "India",
    "runs": 180,
    "wickets": 4,
    "overs": "32.5"
  }
}
```

We shouldn't directly broadcast from the scoring service to millions of WebSocket connections.

Let's introduce an event pipeline.

\#chatgpt-mermaid-\_r_bdq\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_bdq\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_bdq\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_bdq\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_bdq\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_bdq\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_bdq\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_bdq\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_bdq\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_bdq\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_bdq\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_bdq\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_bdq\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_bdq\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_bdq\_ p{margin:0;}#chatgpt-mermaid-\_r_bdq\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_bdq\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_bdq\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_bdq\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_bdq\_ .label text,#chatgpt-mermaid-\_r_bdq\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_bdq\_ .node rect,#chatgpt-mermaid-\_r_bdq\_ .node circle,#chatgpt-mermaid-\_r_bdq\_ .node ellipse,#chatgpt-mermaid-\_r_bdq\_ .node polygon,#chatgpt-mermaid-\_r_bdq\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_bdq\_ .rough-node .label text,#chatgpt-mermaid-\_r_bdq\_ .node .label text,#chatgpt-mermaid-\_r_bdq\_ .image-shape .label,#chatgpt-mermaid-\_r_bdq\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_bdq\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_bdq\_ .rough-node .label,#chatgpt-mermaid-\_r_bdq\_ .node .label,#chatgpt-mermaid-\_r_bdq\_ .image-shape .label,#chatgpt-mermaid-\_r_bdq\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_bdq\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_bdq\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_bdq\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_bdq\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_bdq\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_bdq\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_bdq\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_bdq\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_bdq\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_bdq\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_bdq\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_bdq\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_bdq\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_bdq\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_bdq\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_bdq\_ .icon-shape,#chatgpt-mermaid-\_r_bdq\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_bdq\_ .icon-shape p,#chatgpt-mermaid-\_r_bdq\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_bdq\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_bdq\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_bdq\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_bdq\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_bdq\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_bdq\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_bdq\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_bdq\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_bdq\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_bdq\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_bdq\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_bdq\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_bdq\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_bdq\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_bdq\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_bdq\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_bdq\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_bdq\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_bdq\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_bdq\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_bdq\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_bdq\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_bdq\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_bdq\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_bdq\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_bdq\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_bdq\_ .node rect,#chatgpt-mermaid-\_r_bdq\_ .node circle,#chatgpt-mermaid-\_r_bdq\_ .node ellipse,#chatgpt-mermaid-\_r_bdq\_ .node polygon,#chatgpt-mermaid-\_r_bdq\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_bdq\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_bdq\_ .mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_bdq\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_bdq\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_bdq\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}Scoring Service / DataProviderMatch State DBTransactional OutboxKafka: match-eventsSports Fanout RouterMatch-to-Region DirectoryIndia RegionEurope RegionUS RegionRegional FanoutRegional FanoutRegional FanoutPush FleetPush FleetPush FleetClientsClientsClients

The architecture has four important layers:

1. Scoring service: Owns match information.
2. Kafka: Distributes the durable processing events.
3. Sports fanout router: Determines where interested users are located.
4. Regional push infrastructure: Delivers to local connections.

Let's follow an event.

## 4. Step-by-step: A ball is hit for a four

### Step 1: Scoring system generates an event

```
{
  "eventId": "evt-501",
  "matchId": "match-101",
  "sequence": 501,
  "type": "FOUR_SCORED",
  "payload": {
    "batsman": "Player A",
    "runs": 4,
    "score": "180/4"
  }
}
```

The sequence number matters.

Suppose the previous event was sequence 500.

The new event is 501.

This helps clients detect missing or out-of-order events.

### Step 2: Persist match state and event

The scoring service persists:

- Updated match state.
- The meaningful event.
- An outbox record for publishing.

The outbox prevents a successful scoring transaction from silently losing its corresponding event.

### Step 3: Publish to Kafka

We create a logical topic:

```
match-events
```

The partition key is:

```
matchId
```

For example:

```
match-101 -> Partition 0
match-102 -> Partition 3
match-103 -> Partition 1
```

Why `matchId`?

We want events belonging to one match to be processed in partition order.

We do not need:

```
match-101 topic
match-102 topic
match-103 topic
```

One logical topic with a match identifier as its key is sufficient in the normal case.

### Step 4: Identify interested regions

Suppose users following Match 101 are distributed across:

```
India:
    2.5 million users

Europe:
    1.5 million users

US:
    1 million users
```

Our match-to-region directory might contain:

```
match-101:
    India
    Europe
    US
```

The global router forwards the event to those regions.

### Step 5: Identify interested push servers within each region

In India:

```
match-101:
    Push 1
    Push 2
    Push 3
    ...
    Push 50
```

In Europe:

```
match-101:
    Push 101
    Push 102
    ...
    Push 130
```

Each regional fanout component sends one copy of the event to each relevant push server.

The push server maintains local subscriptions:

```
match-101:
    User A
    User B
    User C
    ...
```

It delivers the event to those local connections.

We have avoided enumerating millions of individual users for every score update.

## 5. How do users subscribe to a match?

Suppose User A opens Match 101.

The client sends:

```
{
  "type": "SUBSCRIBE",
  "matchId": "match-101"
}
```

The push infrastructure must not blindly trust this request.

It checks whether User A is authorized to access the match's information, including any required subscription entitlement.

Then:

```
User A
   |
   ▼
Push Server 1
   |
   ▼
Local Subscription Map

match-101 -> User A's Connection
   |
   ▼
Register Push Server 1
as interested in match-101
```

The local server maintains:

```
matchSubscriptions:

match-101 -> Set<connectionId>
```

The distributed interest registry maintains:

```
match-101 -> Set<pushServerId>
```

Now the sports fanout router knows where to send updates.

### What happens when the user leaves the match?

The subscription is removed.

When a push server has no more local subscribers for that match, it removes its interest.

This is similar to our group-to-server interest registry from Chapter 3.

The authoritative match data and subscription authorization remain owned by the sports application.

The active-interest registry is derived infrastructure state.

## 6. Do we need one Kafka topic per match?

No, not as the default.

Use:

```
Topic: match-events

Key: matchId
```

For example:

```
Partition 0:
    match-101
    match-105

Partition 1:
    match-102
    match-106

Partition 2:
    match-103
    match-107
```

All events for a match are normally routed to the same partition.

This gives us ordering per match while distributing different matches across partitions.

If one particular match becomes extraordinarily hot, we may need a specialized processing path.

But creating a topic for every match from the beginning is unnecessary.

## 7. How should we handle five million users following the same match?

This is the interesting scaling problem.

Suppose:

```
Match 101:

5 million followers

2 million currently connected

100 push servers hosting those connections
```

A match update needs to reach all two million connected users.

There are two different ways to approach it.

### Approach A: Broadcast each update to every interested push server

```
Match Event
    |
    ▼
Regional Fanout
    |
    ├── Push 1
    ├── Push 2
    ├── ...
    └── Push 100
```

Each server broadcasts locally.

This is a good design for the event volume if push-server-level fanout is efficient.

We don't need to create two million durable delivery records per update.

Why?

Because this is generally an ephemeral live-state update.

If a client misses one update, it can retrieve the latest score.

### Approach B: Individual delivery records

We could create:

```
2 million delivery records
```

for every single score update.

This is usually an unnecessarily expensive design.

Imagine 20 updates per minute.

That becomes:

\\[ 2,000,000 \times 20 = 40,000,000 \\]

recipient deliveries per minute.

The delivery database would be doing enormous work even though a scoreboard only needs the latest score.

Use node-level broadcast for massive live audiences. Don't turn every live update into millions of durable individual delivery jobs unless the product truly requires that guarantee.

## 8. What if the client disconnects?

Suppose User A is watching the match.

The client receives:

```
Sequence 501
```

Then disconnects.

Meanwhile:

```
502
503
504
505
```

happen.

When the client reconnects, what do we send?

There are two possible designs.

### Design A: Latest-state recovery

The client fetches:

```
GET /matches/match-101
```

Response:

```
{
  "matchId": "match-101",
  "score": "190/5",
  "overs": "35.2",
  "sequence": 505
}
```

It can resume live updates from that point.

This is sufficient for many scoreboards.

### Design B: Event replay

For commentary or ball-by-ball history, the client might need every missed event.

It sends:

```
{
  "type": "RESUME",
  "matchId": "match-101",
  "lastProcessedSequence": 501
}
```

The sports service replays:

```
502
503
504
505
```

This requires retained event history.

### The better architecture

Support both.

```
Live score:
    Latest snapshot.

Ball-by-ball commentary:
    Ordered historical event replay.
```

Not every UI component needs the same recovery mechanism.

## 9. What about global leaderboards?

Suppose a leaderboard changes frequently.

We have:

```
Top 100 players
```

Do we send each player's individual ranking change as a separate notification to every user?

Usually not.

The leaderboard is a shared materialized state.

A better approach:

```
Leaderboard Processor
       |
       ▼
Compute Latest Top 100
       |
       ▼
Persist Snapshot
       |
       ▼
Publish Leaderboard Update
       |
       ▼
Relevant Push Servers
       |
       ▼
Subscribers
```

A client that misses an update fetches the latest leaderboard snapshot.

This is similar to the live sports score pattern.

### Sports architecture: Final decision table

| Concern                     | Design                                                                 |
| --------------------------- | ---------------------------------------------------------------------- |
| Transport                   | SSE for primarily server-to-client; WebSocket for interactive features |
| Audience                    | Match subscribers                                                      |
| Event topic                 | Shared `match-events`, keyed by match ID                               |
| Routing                     | Match → regions → active push servers                                  |
| Live delivery               | Regional fanout                                                        |
| Score persistence           | Match state database                                                   |
| Commentary history          | Durable event/history store                                            |
| Reconnection                | Snapshot or event replay                                               |
| Missing intermediate scores | Usually acceptable                                                     |
| Missing commentary events   | Recoverable through history                                            |
| Per-user delivery records   | Usually unnecessary for score updates                                  |
| Huge audience               | Hierarchical broadcast                                                 |

The central lesson: Live sports is primarily a selective broadcast and state synchronization problem, not an individual message-delivery problem.

# Part F — Design a Stock Market Real-Time Application

## 10. Understand the requirements

Now imagine building a real-time stock market application.

Users can:

- Subscribe to different stock symbols.
- Receive price updates.
- See live trades.
- View changing order books.
- Set price alerts.
- Follow hundreds of stocks simultaneously.

Suppose:

```
Total users: 5 million

Active connections: 2 million

Available symbols: 10,000

Popular symbol: AAPL

Some users follow 5 symbols.
Others follow 500 symbols.
```

Unlike sports, we don't have just one match-based audience.

We have thousands of independently changing data streams.

The audience for each event depends on the subscribed symbol.

For example:

```
AAPL:
    User A
    User B
    User D

GOOGL:
    User A
    User C

TSLA:
    User B
    User C
    User E
```

This is a subscription-based selective fanout problem.

## 11. First understand the different market-data events

This distinction is extremely important.

A stock market system doesn't produce just one kind of update.

| Event              | Example                           | Can we drop intermediate updates?                       |
| ------------------ | --------------------------------- | ------------------------------------------------------- |
| Latest quote       | Price = $190                      | Often yes, if latest state is sufficient                |
| Bid/ask update     | Best bid/ask changed              | Depends on product                                      |
| Trade event        | 100 shares traded at $190         | Usually not if providing a complete time-and-sales feed |
| Order-book delta   | Quantity at a price level changed | Not arbitrarily; sequence integrity matters             |
| Candlestick update | Current 1-minute candle           | Often can be coalesced                                  |
| Price alert        | User's threshold reached          | No, if alert is an important obligation                 |

Notice that different data types need different guarantees.

For example:

If a user wants to see the latest stock price, missing five intermediate price updates is often acceptable.

But if a user wants the complete list of trades, dropping trades would be incorrect.

If a user wants an order book, dropping an arbitrary delta could make the local order book inconsistent.

So we should not treat every market-data update as a generic WebSocket message.

## 12. Build the basic market-data architecture

Let's begin with the data source.

Suppose our system receives market data from an exchange or licensed market-data provider.

```
          Exchange / Market Data Provider
                         |
                         ▼
                   Feed Handlers
                         |
                         ▼
                Normalize / Validate
                         |
             ┌───────────┴───────────┐
             |                       |
             ▼                       ▼
        Durable Event Log       Latest State
             |                       |
             ▼                       ▼
           Kafka                  Redis
             |                       |
             ▼                       |
       Market Routers              |
             |                       |
             └───────────┬───────────┘
                         |
                         ▼
                 Regional Fanout
                         |
                         ▼
                    Push Servers
                         |
                         ▼
                      Clients
```

Let's understand each component.

### A. Feed handlers

They receive data from the source.

Responsibilities include:

- Maintaining feed connections.
- Parsing incoming messages.
- Detecting sequence gaps.
- Handling reconnects.
- Validating incoming records.

If the upstream source provides sequence numbers, we should use them to identify missing events.

### B. Normalization service

Different market-data sources may represent information differently.

The normalization layer creates a consistent internal event format.

For example:

```
{
  "eventId": "evt-101",
  "symbol": "AAPL",
  "sequence": 10001,
  "type": "QUOTE_UPDATE",
  "payload": {
    "lastPrice": 190.25,
    "bid": 190.20,
    "ask": 190.30
  }
}
```

### C. Latest-state store

We maintain the latest snapshot.

Conceptually:

```
AAPL:
    latest sequence = 10001
    latest price = 190.25

GOOGL:
    latest sequence = 50001
    latest price = 180.10

TSLA:
    latest sequence = 30001
    latest price = 250.00
```

Redis or another suitable low-latency data store can hold these snapshots.

This is useful when a user newly subscribes or reconnects.

They don't need to receive every historical quote update just to obtain the current price.

### D. Durable event stream

Kafka can retain the normalized feed events for:

- Historical processing.
- Analytics.
- Aggregation.
- Market-data consumers.
- Replay and recovery where retention permits.

But this doesn't mean every individual user's outgoing WebSocket update needs a separate Kafka record.

That's an important distinction.

## 13. How do users subscribe to stock symbols?

Suppose User A follows:

```
AAPL
GOOGL
TSLA
```

The client sends:

```
{
  "type": "SUBSCRIBE",
  "symbols": ["AAPL", "GOOGL", "TSLA"]
}
```

The push gateway checks:

- Whether the user is authenticated.
- Whether the symbols are valid.
- Whether the user's market-data subscription permits access.
- How many symbols they are allowed to subscribe to.
- Whether the subscription is subject to any additional access controls.

Then the local connection map becomes:

```
Push Server 1:

AAPL:
    User A
    User B

GOOGL:
    User A
    User C

TSLA:
    User B
    User E
```

We also maintain an interest registry:

```
AAPL -> Push 1, Push 3, Push 8

GOOGL -> Push 1, Push 5

TSLA -> Push 2, Push 3
```

Now a new AAPL update can be sent only to push servers hosting AAPL subscribers.

We don't need to broadcast it to every push server.

### Why is subscription authorization important?

Market data can have licensing and entitlement requirements.

We cannot assume that an authenticated user is permitted to subscribe to all data.

The subscription system must enforce the appropriate authorization and entitlement rules, including limits on concurrent subscriptions.

Also, authorization changes must propagate to existing subscriptions, not just be checked once during initial connection establishment.

## 14. How should market updates be routed?

Suppose a new AAPL update arrives.

```
{
  "symbol": "AAPL",
  "sequence": 10002,
  "type": "QUOTE_UPDATE",
  "payload": {
    "lastPrice": 190.35
  }
}
```

Our router performs:

```
1. Identify symbol = AAPL.

2. Find regions with AAPL subscribers.

3. Find push servers in those regions.

4. Dispatch one copy to each destination server.

5. Each server delivers to its local AAPL subscribers.
```

Architecture:

\#chatgpt-mermaid-\_r_bjg\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_bjg\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_bjg\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_bjg\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_bjg\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_bjg\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_bjg\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_bjg\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_bjg\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_bjg\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_bjg\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_bjg\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_bjg\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_bjg\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_bjg\_ p{margin:0;}#chatgpt-mermaid-\_r_bjg\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_bjg\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_bjg\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_bjg\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_bjg\_ .label text,#chatgpt-mermaid-\_r_bjg\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_bjg\_ .node rect,#chatgpt-mermaid-\_r_bjg\_ .node circle,#chatgpt-mermaid-\_r_bjg\_ .node ellipse,#chatgpt-mermaid-\_r_bjg\_ .node polygon,#chatgpt-mermaid-\_r_bjg\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_bjg\_ .rough-node .label text,#chatgpt-mermaid-\_r_bjg\_ .node .label text,#chatgpt-mermaid-\_r_bjg\_ .image-shape .label,#chatgpt-mermaid-\_r_bjg\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_bjg\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_bjg\_ .rough-node .label,#chatgpt-mermaid-\_r_bjg\_ .node .label,#chatgpt-mermaid-\_r_bjg\_ .image-shape .label,#chatgpt-mermaid-\_r_bjg\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_bjg\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_bjg\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_bjg\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_bjg\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_bjg\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_bjg\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_bjg\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_bjg\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_bjg\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_bjg\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_bjg\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_bjg\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_bjg\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_bjg\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_bjg\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_bjg\_ .icon-shape,#chatgpt-mermaid-\_r_bjg\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_bjg\_ .icon-shape p,#chatgpt-mermaid-\_r_bjg\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_bjg\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_bjg\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_bjg\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_bjg\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_bjg\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_bjg\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_bjg\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_bjg\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_bjg\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_bjg\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_bjg\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_bjg\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_bjg\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_bjg\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_bjg\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_bjg\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_bjg\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_bjg\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_bjg\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_bjg\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_bjg\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_bjg\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_bjg\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_bjg\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_bjg\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_bjg\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_bjg\_ .node rect,#chatgpt-mermaid-\_r_bjg\_ .node circle,#chatgpt-mermaid-\_r_bjg\_ .node ellipse,#chatgpt-mermaid-\_r_bjg\_ .node polygon,#chatgpt-mermaid-\_r_bjg\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_bjg\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_bjg\_ .mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_bjg\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_bjg\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_bjg\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}Normalized Market FeedKafka: market-eventsMarket RouterSymbol Interest RegistryPush Server 1Push Server 2Push Server 3AAPL Local SubscribersAAPL Local SubscribersAAPL Local Subscribers

Kafka topic:

```
market-events
```

Key:

```
symbol
```

This gives us per-symbol ordering within a partition.

But now we have an interesting scaling problem.

## 15. The hot-symbol problem

Suppose:

```
AAPL:

1 million subscribers

GOOGL:

100,000 subscribers

TSLA:

50,000 subscribers
```

AAPL is much more popular.

Its partition might receive a large number of events.

Moreover, a single market router might spend substantial time processing its fanout.

This is a hot-key problem.

We need to distinguish two kinds of work:

Processing the market-data sequence

This needs to preserve the source ordering and validate sequence numbers.

Distributing an already processed update to thousands of push servers

This can be parallelized after the update has been normalized and assigned an authoritative sequence.

For example:

```
                AAPL Ordered Processor
                         |
                         ▼
                 Validated Update 10002
                         |
                         ▼
                 Fanout Coordinator
                         |
               ┌─────────┼─────────┐
               |         |         |
               ▼         ▼         ▼
            Region 1  Region 2  Region 3
               |         |         |
               ▼         ▼         ▼
           Push Fleet Push Fleet Push Fleet
```

The ordered market-data processing remains logically consistent.

The regional distribution happens in parallel.

We should not split the same symbol arbitrarily across Kafka partitions and then assume the updates will remain globally ordered.

If we need more processing capacity for one exceptionally popular symbol, we should parallelize downstream fanout while preserving the authoritative source sequence.

## 16. The most important optimization: Coalescing

Consider a user following AAPL.

The source produces:

```
190.10
190.12
190.15
190.18
190.20
190.25
190.30
```

Suppose the client can process only a few updates per second.

Do we need to queue every quote update?

Often, no.

We can retain only the newest quote snapshot.

For example, over a short delivery interval:

```
Incoming:
    190.10
    190.12
    190.15
    190.18
    190.20
    190.25
    190.30

Outgoing:
    190.30
```

This is coalescing.

It reduces:

- Network bandwidth.
- Push-server outbound buffers.
- Queue growth.
- Client processing load.

The server can maintain a latest-value buffer:

```
latestQuote[AAPL] = 190.30
```

When the client is ready for another update, it receives the latest value.

### But don't apply coalescing to everything

Consider these two clients.

Client A: Casual price dashboard

Needs:

```
Latest price
Latest bid/ask
Daily change
```

Coalescing is appropriate.

Client B: Complete trade feed

Needs:

```
Trade 1
Trade 2
Trade 3
Trade 4
```

Arbitrarily dropping trades is incorrect.

Similarly, order-book deltas require sequence continuity.

If a client misses a delta:

```
Expected sequence = 1005

Received sequence = 1008
```

The client knows it has a gap.

The server should provide missing deltas if retained, or instruct the client to obtain a fresh order-book snapshot.

The correct coalescing policy belongs to the type of market data, not merely to the WebSocket connection.

## 17. What happens when a user reconnects?

Suppose User A was watching AAPL.

Their last processed quote sequence was:

```
10001
```

They reconnect.

For a latest-price screen:

```
GET /market-data/AAPL/snapshot
```

Response:

```
{
  "symbol": "AAPL",
  "sequence": 10500,
  "price": 190.50
}
```

The client has the latest state.

It resumes live updates after the snapshot sequence.

For a full order-book application, the recovery process is stricter:

1. Request a current order-book snapshot.
2. Record its sequence number.
3. Subscribe to subsequent updates.
4. Apply only contiguous updates.
5. Detect missing sequence numbers.
6. Resynchronize when a gap cannot be recovered.

The handoff between snapshot and live updates must be gap-free, as discussed in the previous chapters.

## 18. Does every stock update need durable delivery?

No.

Let's separate the data pipeline.

| Requirement                | Storage/messaging                                                |
| -------------------------- | ---------------------------------------------------------------- |
| Latest quote               | Redis/materialized snapshot                                      |
| Normalized market events   | Kafka, when replay/analytics justify it                          |
| Regulatory/history records | Durable market-data storage according to applicable requirements |
| User symbol subscriptions  | Local push memory + interest registry                            |
| Individual price alerts    | Durable alert records                                            |
| Client connection          | Push-server memory                                               |
| Latest-state recovery      | Snapshot API                                                     |

A retail price dashboard might use best-effort real-time delivery.

A market-data audit or professional trading system may require much stronger completeness and ordering guarantees.

The same data source can feed two separate pipelines:

```
                  Market Data Feed
                         |
                 Normalization
                         |
                ┌────────┴────────┐
                |                 |
                ▼                 ▼
          Real-Time Feed     Durable History
                |                 |
                ▼                 ▼
          Coalescing       Analytics / Audit
                |
                ▼
          Push Servers
                |
                ▼
            Clients
```

Don't mix the requirements of a low-latency dashboard with the requirements of a full historical market-data record.

## 19. Stock market architecture: Final decisions

| Concern              | Design                                                                   |
| -------------------- | ------------------------------------------------------------------------ |
| Transport            | WebSocket is a natural choice; SSE is possible for one-way subscriptions |
| Audience             | Symbol subscribers                                                       |
| Subscription lookup  | Symbol → active push servers                                             |
| Event topic          | Shared `market-events`, keyed by symbol                                  |
| Latest price         | Redis/materialized state                                                 |
| Trade history        | Durable market-data store                                                |
| Order book           | Snapshot + sequenced deltas                                              |
| Quote delivery       | Coalesce latest state where acceptable                                   |
| High-volume symbols  | Dedicated/hierarchical downstream fanout                                 |
| Slow consumers       | Bounded buffers and event-type-specific policies                         |
| Reconnect            | Snapshot or sequence-based replay                                        |
| Critical price alert | Separate durable user notification workflow                              |

The central lesson: A stock market system is primarily a high-frequency selective streaming problem. We should optimize around subscription locality, sequence continuity, coalescing, and network throughput.

# Part G — Design a Reliable Notification Platform

## 20. Understand the requirements

Now let's design a notification platform that can be reused by many business services.

For example:

- Order shipped.
- Payment completed.
- Account security alert.
- New chat message.
- Task assigned.
- Report generated.
- Approval required.

Suppose many business services want to send notifications.

Instead of every service implementing its own delivery logic, we introduce a dedicated notification system.

The requirements:

- Individual user targeting.
- Multiple devices.
- Notification history.
- Users may be offline.
- Some notifications need acknowledgements.
- Retries must be supported.
- Duplicate notifications must be avoided.
- Different channels may be needed.

This system has fundamentally different reliability requirements from live sports or stock quotes.

A notification represents an obligation to inform a recipient, not just a temporary state update.

## 21. First separate notification creation from delivery

Suppose the order service generates:

```
Your order has shipped.
```

Should the order service directly contact the user's WebSocket?

No.

It should publish a business event.

```
{
  "eventId": "evt-801",
  "type": "ORDER_SHIPPED",
  "userId": "user-A",
  "payload": {
    "orderId": "order-123"
  }
}
```

The notification service consumes this event and creates a notification.

For example:

```
{
  "notificationId": "notification-101",
  "userId": "user-A",
  "type": "ORDER_UPDATE",
  "title": "Order shipped",
  "body": "Your order is on its way."
}
```

This is important:

The business service tells us something happened.

The notification service decides how that occurrence should become a user-facing notification.

It may apply:

- Notification preferences.
- Templates.
- Localization.
- Priority.
- Channel preferences.
- Deduplication.
- Expiry rules.

The notification service owns the notification lifecycle.

## 22. Build the notification architecture

\#chatgpt-mermaid-\_r_blu\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_blu\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_blu\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_blu\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_blu\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_blu\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_blu\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_blu\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_blu\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_blu\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_blu\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_blu\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_blu\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_blu\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_blu\_ p{margin:0;}#chatgpt-mermaid-\_r_blu\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_blu\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_blu\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_blu\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_blu\_ .label text,#chatgpt-mermaid-\_r_blu\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_blu\_ .node rect,#chatgpt-mermaid-\_r_blu\_ .node circle,#chatgpt-mermaid-\_r_blu\_ .node ellipse,#chatgpt-mermaid-\_r_blu\_ .node polygon,#chatgpt-mermaid-\_r_blu\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_blu\_ .rough-node .label text,#chatgpt-mermaid-\_r_blu\_ .node .label text,#chatgpt-mermaid-\_r_blu\_ .image-shape .label,#chatgpt-mermaid-\_r_blu\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_blu\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_blu\_ .rough-node .label,#chatgpt-mermaid-\_r_blu\_ .node .label,#chatgpt-mermaid-\_r_blu\_ .image-shape .label,#chatgpt-mermaid-\_r_blu\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_blu\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_blu\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_blu\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_blu\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_blu\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_blu\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_blu\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_blu\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_blu\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_blu\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_blu\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_blu\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_blu\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_blu\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_blu\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_blu\_ .icon-shape,#chatgpt-mermaid-\_r_blu\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_blu\_ .icon-shape p,#chatgpt-mermaid-\_r_blu\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_blu\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_blu\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_blu\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_blu\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_blu\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_blu\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_blu\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_blu\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_blu\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_blu\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_blu\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_blu\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_blu\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_blu\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_blu\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_blu\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_blu\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_blu\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_blu\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_blu\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_blu\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_blu\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_blu\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_blu\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_blu\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_blu\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_blu\_ .node rect,#chatgpt-mermaid-\_r_blu\_ .node circle,#chatgpt-mermaid-\_r_blu\_ .node ellipse,#chatgpt-mermaid-\_r_blu\_ .node polygon,#chatgpt-mermaid-\_r_blu\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_blu\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_blu\_ .mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_blu\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_blu\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_blu\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}Order ServiceKafka: domain-eventsPayment ServiceChat ServiceTask ServiceNotification ServiceNotification Inbox DBNotification OutboxDelivery Work QueueChannel Delivery WorkersWebSocket / SSE GatewayMobile Push ProviderEmail ProviderClientACK / Read Handler

Notice that the push infrastructure is only one delivery channel.

The same notification might also be sent through:

- Mobile push.
- Email.
- SMS.
- In-app inbox.

We need to keep the notification itself separate from channel-specific delivery attempts.

## 23. Introduce two entities: Notification and Delivery

This is one of the most useful database design decisions.

Imagine User A receives:

```
Notification 101:
Your order has shipped.
```

They have three devices:

```
Desktop
Mobile
Tablet
```

And the user has email notifications enabled.

We have one notification, but potentially several delivery operations.

### Entity 1: Notification

```
notificationId = N101

recipient = User A

content = Order shipped
```

This represents the user's notification.

### Entity 2: Delivery attempts

```
N101:

WebSocket Desktop -> ACKNOWLEDGED

Mobile Push        -> ACCEPTED_BY_PROVIDER

Email              -> SENT

WebSocket Tablet   -> PENDING
```

These delivery channels have different acknowledgement semantics.

For example:

- A browser WebSocket client may send an application ACK.
- A mobile push provider may confirm that it accepted a push request, not that the device displayed it.
- An email provider may accept the message for sending without proving that the recipient opened it.

Never treat all channel acknowledgements as equivalent.

### Conceptual schema

notifications

```
notification_id
user_id
type
title
body
payload
priority
created_at
expires_at
read_at
```

notification_deliveries

```
delivery_id
notification_id
device_id
channel
status
attempt_count
next_attempt_at
last_error
accepted_at
acknowledged_at
```

A notification is created once.

Its delivery lifecycle can evolve independently across channels and devices.

## 24. Step-by-step: Sending a durable notification

Suppose the payment service publishes:

```
{
  "eventId": "evt-901",
  "type": "PAYMENT_COMPLETED",
  "userId": "user-A",
  "paymentId": "payment-101"
}
```

### Step 1: Consume the domain event

Notification service receives the event from Kafka.

### Step 2: Apply idempotency

We don't want the same business event to create duplicate notifications.

A useful uniqueness rule:

```
UNIQUE(sourceEventId, recipientId, notificationType)
```

The exact key depends on whether one business event can legitimately generate several notifications of the same type.

### Step 3: Resolve preferences and policy

For example:

```
User A:

In-app: Enabled

WebSocket: Enabled

Mobile Push: Enabled

Email: Disabled
```

We generate only the appropriate delivery work.

### Step 4: Persist the notification

Save:

```
N101 -> User A -> Payment completed
```

And create the corresponding delivery work.

If both are written to the same database, the notification and its initial delivery records can be committed in one transaction.

If they are published through a separate broker, use an outbox or equivalent reliable handoff mechanism.

### Step 5: Deliver through the live push channel

The delivery worker looks up User A's active connections.

```
User A:
    Desktop -> Push 1
    Mobile  -> Push 2
```

It dispatches to the appropriate push servers.

### Step 6: What if User A is offline?

The notification still exists in the inbox.

We don't need to keep hitting a nonexistent WebSocket connection.

When User A reconnects:

```
GET /notifications?status=UNREAD
```

The notification service returns outstanding notifications.

### Step 7: ACK and read status

The client acknowledges according to the notification protocol.

The user opening the notification is a separate read event.

For example:

```
N101:

PERSISTED
    |
    ▼
DELIVERY_ATTEMPTED
    |
    ▼
CLIENT_ACKNOWLEDGED
    |
    ▼
READ
```

Not every notification needs all these states.

The important thing is to define their meanings clearly.

## 25. What if live notification delivery fails?

Suppose the notification exists in the inbox.

A delivery worker attempts the WebSocket dispatch.

Push Server 1 is unavailable.

We have two responsibilities:

Responsibility A: Preserve the notification.

Do not delete it merely because the live delivery failed.

Responsibility B: Decide whether another channel or retry is appropriate.

For example:

```
WebSocket delivery fails
       |
       ▼
Notification still in inbox
       |
       ├── User online elsewhere?
       |       |
       |       └── Try another authorized connection
       |
       ├── User offline?
       |       |
       |       └── Wait for synchronization
       |
       └── Critical alert?
               |
               └── Use configured alternate channel
```

This is very different from a stock quote.

A missed quote can be replaced by the latest quote.

A missed payment notification should remain available.

## 26. How should retries work for multiple channels?

Suppose:

```
Notification N101:

WebSocket -> Failed

Mobile Push -> Accepted by provider

Email -> Disabled
```

We shouldn't treat all three as one combined delivery operation.

Each channel has its own lifecycle.

For example:

```
Notification N101
       |
       ├── WebSocket Delivery D101
       |
       ├── Mobile Push Delivery D102
       |
       └── Email Delivery D103
```

D101 can be retried independently.

D102 might already be accepted by the mobile push provider.

D103 might be skipped because the user disabled email.

If we have a provider-level error, we apply that provider's retry policy.

If the mobile provider accepts the message but the device never displays it, we should not claim that it was read.

### Retry policy should consider notification expiry

For example:

```
Notification:

Priority: HIGH

Expires: In 24 hours
```

The delivery system shouldn't keep retrying an expired notification.

For some notifications, expiry can be very short.

For example:

```
Your meeting starts in 2 minutes.
```

Delivering that message six hours later is not useful.

For others, such as payment confirmation, the notification should remain available in the inbox even after its real-time delivery window has passed.

This is why the business notification and individual transport attempts need separate lifecycles.

## 27. What queue technology should notifications use?

We now have two separate requirements.

Domain-event processing

Many services publish events.

We need independent consumers, replay, and scalable event processing.

Kafka is a reasonable choice.

Delivery-task processing

We need:

- Individual task retries.
- Bounded concurrency.
- Dead-letter handling.
- Provider-specific processing.
- Delivery leases/acknowledgements.

A work queue such as RabbitMQ or Redis Streams can be suitable.

Alternatively, a database-backed delivery scheduler can manage the workflow.

A possible combined design:

```
                 Business Services
                        |
                        ▼
                       Kafka
                        |
                        ▼
               Notification Processor
                        |
                        ▼
                Notification DB
                        |
                        ▼
                Delivery Work Queue
                        |
            ┌───────────┼───────────┐
            |           |           |
            ▼           ▼           ▼
         WebSocket   Mobile Push   Email
          Worker      Worker      Worker
```

We don't need a separate work queue just because we have Kafka.

Kafka consumer groups can also distribute delivery tasks if the requirements fit.

Choose the queue based on retry, scheduling, concurrency, and delivery-state needs.

## 28. Does a notification need exactly-once delivery?

Not necessarily.

We typically want:

```
At-least-once delivery
+
Idempotent client processing
+
Durable inbox
```

Suppose WebSocket delivery is retried.

The client might receive:

```
Notification N101
Notification N101
```

It should recognize the same `notificationId` and avoid adding duplicate notification records.

The notification inbox also acts as a source of truth.

The push system can send a notification ID and lightweight payload.

The client can reconcile with the inbox if necessary.

This provides a robust experience without pretending the underlying network delivers exactly once.

### Notification architecture: Final decisions

| Concern               | Design                                                   |
| --------------------- | -------------------------------------------------------- |
| Transport             | SSE or WebSocket; both may be used alongside mobile push |
| Audience              | Individual user/device                                   |
| Notification creation | Notification service                                     |
| Event ingestion       | Kafka or suitable event bus                              |
| Notification storage  | PostgreSQL/DynamoDB                                      |
| Delivery state        | Per-channel/device delivery records when needed          |
| Work distribution     | Queue or durable delivery scheduler                      |
| Live routing          | User → connections → push servers                        |
| Offline delivery      | Durable inbox                                            |
| Delivery guarantee    | At-least-once attempts with idempotency                  |
| Client ACK            | Explicit application ACK                                 |
| Read receipt          | Separate user action                                     |
| Expiry                | Notification-specific                                    |
| Retries               | Channel-specific and bounded                             |

The central lesson: A reliable notification system is primarily a durable inbox and delivery-workflow problem. WebSocket/SSE provides one of its delivery channels.

# Part H — Compare the Three Applications

## 29. Why are their architectures so different?

All three applications use the same fundamental push infrastructure.

But look at their actual requirements.

| Concern            | Live sports                         | Stock market                     | Notifications                                       |
| ------------------ | ----------------------------------- | -------------------------------- | --------------------------------------------------- |
| Audience           | Match subscribers                   | Symbol subscribers               | Individual users/devices                            |
| Update frequency   | Moderate                            | Very high                        | Event-driven                                        |
| Event volume       | Moderate to high                    | Very high                        | Variable                                            |
| Fanout             | Potentially enormous                | Selective, sometimes enormous    | Usually targeted                                    |
| Durability         | Different by event type             | Different by data type           | Generally important                                 |
| Ordering           | Per match                           | Per symbol/feed                  | Per notification workflow                           |
| Offline recovery   | Latest snapshot/history             | Latest snapshot or market replay | Durable inbox                                       |
| Coalescing         | Often useful for scores             | Very useful for quotes           | Usually inappropriate for independent notifications |
| Delivery ACK       | Usually unnecessary for live scores | Depends on product               | Useful when required                                |
| Primary bottleneck | Broadcast bandwidth                 | Update frequency and fanout      | Durable delivery workflow                           |

## 30. Which messaging technology fits each?

Let's bring back the messaging concepts from Chapter 4.

| Application requirement                         | Messaging approach                     |
| ----------------------------------------------- | -------------------------------------- |
| Broadcast a cricket score to active subscribers | Pub/Sub / regional fanout              |
| Retain ball-by-ball commentary events           | Kafka or durable history               |
| Broadcast latest stock prices                   | Low-latency Pub/Sub/fanout             |
| Process ordered market-data events              | Partitioned event stream               |
| Deliver personal payment notifications          | Durable queue/stream + inbox           |
| Retry failed email/mobile notifications         | Work queue or delivery scheduler       |
| Preserve order within a chat conversation       | Partitioned Kafka stream               |
| Distribute collaborative document operations    | Ordered event stream + targeted fanout |

One observation:

Kafka and Pub/Sub can coexist in all three applications.

For example, a sports system may use Kafka for durable match-event processing and Pub/Sub for local regional broadcast.

A stock system may retain normalized market events in Kafka while using a separate low-latency data distributor for live quote updates.

A notification platform may use Kafka for business-event ingestion and a queue for independently retryable channel delivery work.

But we should only add components when their responsibilities justify the complexity.

# 31. One important concept: The same event can have multiple delivery paths

Consider a cricket match update.

We might need:

1. Live scoreboard updates.
2. Ball-by-ball commentary.
3. Personal alerts for users who selected a particular player.
4. A match history for later retrieval.

All four originate from the same underlying match activity.

But they need different delivery guarantees.

```
                  Match Event
                       |
             ┌─────────┼─────────┐
             |         |         |
             ▼         ▼         ▼
         Scoreboard Commentary Personal Alert
             |         |         |
             ▼         ▼         ▼
          Pub/Sub    Kafka    Notification
             |         |      Delivery System
             ▼         ▼         ▼
          Snapshot  History   Durable Inbox
             |         |         |
             ▼         ▼         ▼
          Clients   Clients   Clients
```

This is a powerful architecture pattern.

Instead of asking:

> Should this event use Kafka or Pub/Sub?

Ask:

> What are the different consumers of this business event, and what guarantee does each consumer need?

The same domain event can feed:

- An ephemeral real-time stream.
- A durable event history.
- A personalized notification pipeline.
- An analytics pipeline.

Each consumer has its own processing and recovery requirements.

# 32. The architecture decision framework

Let's extract the reasoning from these three applications.

When designing any real-time application, identify these five things first.

### A. Audience

Who receives the event?

```
Everyone
A match
A symbol
A group
An individual user
A specific device
```

### B. Event semantics

What does the event represent?

```
Latest state?

An independent historical event?

An incremental state change?

A notification obligation?
```

This determines whether you can discard or coalesce it.

### C. Ordering

Where must order be preserved?

```
Per match?
Per symbol?
Per conversation?
Per document?
Per user?
```

This determines your partitioning and sequencing strategy.

### D. Durability

What happens if the user is offline?

```
Discard.

Send the latest snapshot.

Replay historical events.

Maintain a durable inbox.
```

### E. Delivery acknowledgement

What needs to be acknowledged?

```
Broker accepted?

Push server accepted?

Client processed?

User read?
```

These are separate guarantees.

## Chapter 10 — Part 2: What you should now understand

We have designed three more complete systems.

Live sports

```
Match state
+ Match event stream
+ Match-to-region routing
+ Regional broadcast
+ Snapshot/replay
```

The emphasis is massive broadcast without unnecessary per-user delivery records.

Stock market

```
Market feed
+ Symbol subscriptions
+ Ordered market-data processing
+ Symbol-to-server routing
+ Coalescing
+ Sequence recovery
```

The emphasis is high-frequency selective streaming without overwhelming clients or network capacity.

Notification platform

```
Business event
+ Notification service
+ Durable inbox
+ Channel-specific delivery records
+ Retry workers
+ Client ACK
```

The emphasis is reliable delivery even when the user is offline.

The underlying push infrastructure remains the same.

# Next: Chapter 10 — Part 3: Gaming, Live Tracking, and Global Dashboards

We'll finish our real-world design patterns with three more applications.

### 1. Online multiplayer gaming

We'll understand:

- Why low-latency bidirectional communication is essential.
- Why WebSocket might not always be the best choice.
- Room-based connection management.
- Game-session ownership.
- High-frequency state synchronization.
- Why most game updates should not go through Kafka.
- Tick rates, state snapshots, and client prediction.
- What happens when a game server crashes.
- Why game state is different from ordinary push state.

### 2. Live location tracking

We'll design an Uber-style tracking system:

- Driver location updates.
- Customer-specific subscriptions.
- Geo-based routing.
- Location update frequency.
- Latest-location storage.
- Geospatial indexing.
- Coalescing and stale-update handling.
- Offline and reconnect recovery.

### 3. Multi-tenant global dashboards

We'll design:

- Tenant-specific streams.
- Per-user subscriptions.
- Selective fanout.
- Aggregated updates.
- Cache invalidation.
- High-cardinality subscriptions.
- Backpressure and isolation between tenants.

Finally, we'll compare all eight application patterns and build a reusable architecture decision framework.

The central goal: Given a new real-time system requirement, you should be able to derive its connection model, audience routing, message broker, database, reliability guarantees, and scaling architecture without memorizing a predefined design.









# Chapter 10 — Part 3: Online Gaming, Live Location Tracking, and Global Dashboards

We've now designed six applications:

- Chat.
- Collaborative editing.
- Live sports.
- Stock market.
- Notifications.
- Document collaboration.

Let's finish the remaining three.

These introduce three additional architectural challenges:

1. Online gaming: Extremely low latency and continuous bidirectional communication.
2. Live location tracking: High-frequency location updates with selective geographic and user-based routing.
3. Global dashboards: Large numbers of users receiving aggregated, tenant-specific updates.

An important observation before we start:

Not every real-time system should use Kafka, and not every real-time system should use a WebSocket push gateway as its primary processing engine.

Sometimes we need a dedicated real-time application server, sometimes a streaming pipeline, and sometimes simply SSE with a cached snapshot.

Let's understand why.

# Part I — Design an Online Multiplayer Gaming System

## 1. Understand the requirements

Imagine we're building an online multiplayer game.

Players can:

- Join a game room.
- Move their characters.
- Shoot, jump, or perform actions.
- See other players' movements.
- Receive updated game state.
- Disconnect and reconnect.
- Play with users across different regions.

Suppose:

```
Game Room R1:

Player A
Player B
Player C
Player D
```

Whenever Player A moves, the other players should observe the movement quickly.

This is fundamentally different from a notification application.

A notification might be delivered after 200 ms or even several seconds.

But in a multiplayer game, delays can directly affect gameplay.

We need to optimize for:

- Very low latency.
- High-frequency communication.
- Bidirectional messaging.
- Session consistency.
- Predictable message ordering.
- Fast failure recovery.

## 2. First understand why a normal push architecture isn't enough

Our existing architecture looks like:

```
Client
  |
  ▼
Load Balancer
  |
  ▼
Push Server
  |
  ▼
Message Broker
  |
  ▼
Routing Workers
  |
  ▼
Push Server
  |
  ▼
Client
```

This is good for notifications and ordinary real-time applications.

But imagine a game running at 60 updates per second.

Each player produces movement updates.

```
Player A -> Movement update
Player B -> Movement update
Player C -> Movement update
Player D -> Movement update
```

If every movement update goes through:

```
Kafka
  |
  ▼
Routing Worker
  |
  ▼
Redis Lookup
  |
  ▼
Push Server
```

we introduce unnecessary latency, processing, and coordination.

We need a specialized design.

Game-state communication should generally happen through a dedicated game-session server, not through a generic durable event pipeline for every movement update.

This is an important exception to our earlier general-purpose push architecture.

## 3. Introduce the Game Session Server

Instead of treating the game server as just another push server, give it ownership of an active game session.

For example:

```
Room R1 -> Game Server 1

Room R2 -> Game Server 2

Room R3 -> Game Server 1
```

Each game server manages the authoritative state of its assigned sessions.

For Room R1:

```
Game Server 1:

Players:
    A
    B
    C
    D

Game State:
    Player positions
    Health
    Scores
    Inventory
    Current game phase
    Other game-specific state
```

It continuously processes player actions and generates updated game state.

### Why should the game server own the authoritative state?

Suppose Player A sends:

```
{
  "type": "MOVE",
  "x": 100,
  "y": 200
}
```

Should the server simply broadcast this coordinate to everyone?

No.

A malicious client could send:

```
{
  "type": "MOVE",
  "x": 999999,
  "y": 999999
}
```

Or claim:

```
I moved 100 meters in 1 millisecond.
```

The server should validate the action according to the game's rules.

For example:

- Is the player allowed to move?
- Is the movement distance valid?
- Is the player alive?
- Is the destination reachable?
- Does the action violate game constraints?

The server calculates the authoritative state.

This is called an authoritative server model.

The client sends input or intentions.

The server decides what actually happened.

This prevents clients from becoming trusted sources of game state.

## 4. Understand the game architecture

Let's introduce the components.

```
                    Game Clients
                         |
                         ▼
                   Global Traffic
                      Manager
                         |
                         ▼
                  Game Gateway Fleet
                         |
              ┌──────────┼──────────┐
              |          |          |
              ▼          ▼          ▼
            Room R1    Room R2    Room R3
              |          |          |
              ▼          ▼          ▼
         Game Server  Game Server Game Server
              |          |          |
              ▼          ▼          ▼
         Game State  Game State  Game State
```

There are several responsibilities:

| Component               | Responsibility                                              |
| ----------------------- | ----------------------------------------------------------- |
| Matchmaking service     | Find players and create matches                             |
| Room directory          | Map room IDs to game-server owners                          |
| Game gateway            | Manage client network connections and route game traffic    |
| Game server             | Execute game logic and maintain authoritative session state |
| Presence service        | Track players and session availability                      |
| Persistent game storage | Store results, checkpoints, rankings, and important state   |

A gateway and game server can be deployed in the same process for simpler games.

Separating them becomes more useful when you have many game servers, multiple protocols, or independent connection and compute scaling requirements.

## 5. Step-by-step: Player A moves

Suppose Room R1 belongs to Game Server 1.

Player A connects.

```
Player A
    |
    ▼
Game Gateway 1
    |
    ▼
Room Directory
    |
    ▼
Room R1 -> Game Server 1
```

The gateway routes Player A's input to Game Server 1.

Player A sends:

```
{
  "type": "MOVE",
  "sequence": 101,
  "direction": "RIGHT"
}
```

Game Server 1:

1. Validates the player's action.
2. Applies game rules.
3. Updates authoritative state.
4. Calculates the new player position.
5. Generates the state update.
6. Sends the relevant update to the players in Room R1.

```
                    Game Server 1
                          |
                    Updated State
                          |
              ┌───────────┼───────────┐
              |           |           |
              ▼           ▼           ▼
           Player A    Player B    Player C
```

The game server already knows which players belong to Room R1.

It doesn't need to query Redis for every movement update.

This is a significant difference from ordinary targeted notification delivery.

## 6. What transport should gaming use?

This is where the choice becomes interesting.

We have several possibilities.

### A. WebSocket

WebSocket provides bidirectional communication over a reliable, ordered transport.

Advantages:

- Easy browser integration.
- Client and server can send messages independently.
- Connection remains open.
- Suitable for many multiplayer games.
- Works with existing HTTP infrastructure.

Disadvantages:

- TCP retransmission and head-of-line blocking can increase latency when packets are lost.
- Ordered delivery can delay newer updates behind older data.
- Not always ideal for extremely latency-sensitive real-time movement.

For turn-based games, card games, board games, and many casual multiplayer games, WebSocket is usually a perfectly reasonable choice.

### B. UDP

UDP provides datagram-based communication.

Advantages:

- No built-in retransmission of lost packets.
- No built-in ordering requirement.
- Application can prioritize recent state over old state.
- Useful for time-sensitive game updates.

Disadvantages:

- No built-in reliable delivery.
- No built-in congestion control or connection management comparable to TCP.
- The game protocol needs to implement appropriate reliability, sequencing, and congestion behavior where necessary.
- NAT traversal and security require careful handling.

For fast-paced competitive games, UDP-based protocols are common.

### C. WebRTC Data Channels

WebRTC data channels can provide bidirectional communication.

They support different reliability and ordering configurations over an encrypted SCTP-over-DTLS transport.

They can be useful in browser-based real-time applications, including certain gaming scenarios.

However, they introduce additional signaling, ICE, NAT traversal, and operational complexity.

### Comparison

| Requirement                    | WebSocket                | UDP-based protocol                         | WebRTC Data Channel                    |
| ------------------------------ | ------------------------ | ------------------------------------------ | -------------------------------------- |
| Browser support                | Excellent                | Not directly through standard browser APIs | Excellent                              |
| Bidirectional                  | Yes                      | Yes, with application protocol             | Yes                                    |
| Reliable ordered communication | Yes                      | Must be implemented as needed              | Configurable                           |
| Low-latency state updates      | Good                     | Often preferable for fast-paced games      | Good                                   |
| Ease of implementation         | Relatively easy          | More complex                               | More complex                           |
| Typical use                    | Casual/many online games | Fast-paced competitive games               | Browser peer/data-channel applications |

The key insight:

A real-time gaming system might not use WebSocket at all for its most latency-sensitive traffic.

The general push infrastructure and the real-time game transport are related, but they are not necessarily the same service.

## 7. Understand game ticks and state updates

Let's introduce an important gaming concept: ticks.

Suppose a game server runs at 60 ticks per second.

It processes the current game state approximately every:

\\[ \frac{1}{60} = 0.01667\ seconds \\]

Approximately 16.7 milliseconds per tick.

Conceptually:

```
Tick 1 -> Read player inputs
       -> Validate actions
       -> Update game state
       -> Send relevant snapshots

Tick 2 -> Read player inputs
       -> Validate actions
       -> Update game state
       -> Send relevant snapshots

Tick 3 -> ...
```

This is called the game loop.

The server doesn't necessarily need to send every internal state change to every client.

It can generate state snapshots at an appropriate update rate.

For example:

```
Server state:

Player A -> (100, 200)
Player B -> (300, 400)
Player C -> (500, 600)
```

A snapshot can represent the relevant state of several entities.

This is often more efficient than sending a separate independent message for every small movement.

### What happens if a client misses a snapshot?

For ordinary movement updates, we might simply send the newest state.

The client can update its view.

We don't need to replay every old player position.

This is another case where latest-state delivery is preferable to durable event delivery.

But certain game events are different:

- Match result.
- Item acquisition.
- Currency changes.
- Tournament ranking.
- Payment-related purchase.
- Important inventory changes.

These should be persisted and processed with appropriate business guarantees.

## 8. Client-side prediction and reconciliation

Let's understand another gaming-specific concept.

Network latency means the player doesn't immediately see the effect of their own input.

Suppose Player A presses right.

If the client waits for the server response:

```
Press right
    |
    ▼
Send to server
    |
    ▼
Server validates
    |
    ▼
Server responds
    |
    ▼
Update screen
```

The player sees a delay.

Instead, the client can predict the movement immediately.

```
Press right
    |
    ├── Update local movement immediately
    |
    └── Send input to server
```

Later, the server sends the authoritative result.

If the prediction differs from the actual result, the client reconciles its state.

This is called client-side prediction and server reconciliation.

For certain games, clients also interpolate remote player positions between received snapshots.

The push system transports these updates.

The game protocol determines how to interpret them.

## 9. Why Kafka is usually not used for every game tick

Imagine:

```
10,000 active game rooms.

Each room has 10 players.

Each player sends 30 inputs/sec.
```

Total input rate:

\\[ 10,000 \times 10 \times 30 = 3,000,000 \\]

That is 3 million input events per second.

Putting every movement input into Kafka creates substantial overhead and latency for a workload that is often ephemeral and session-local.

Instead:

```
Clients
   |
   ▼
Game Gateway
   |
   ▼
Authoritative Game Server
   |
   ├── In-memory game processing
   |
   ├── Network state distribution
   |
   └── Persist meaningful checkpoints/results asynchronously
```

Kafka may still be useful for:

- Match-created events.
- Match-completed events.
- Player achievements.
- Analytics.
- Tournament updates.
- Auditing.
- Game-state recovery checkpoints.

But it usually shouldn't sit in the synchronous hot path of every movement tick.

This is an important exception to the general-purpose push architecture.

## 10. What happens when a game server crashes?

Suppose Game Server 1 owns Room R1.

It crashes.

All active players lose the session connection or stop receiving authoritative state.

We need to decide how much of the match can be recovered.

### Option A: Session can be restarted

Suitable for casual games.

```
Game Server 1 crashes
        |
        ▼
Room becomes unavailable
        |
        ▼
Players reconnect
        |
        ▼
New Game Server owns the room
        |
        ▼
Restore checkpoint or restart match
```

### Option B: Restore from checkpoint and event history

For more important game sessions, periodically persist:

- Current game state.
- Player status.
- Match version.
- Critical events.
- Current phase.

After failure:

1. Select a new game-server owner.
2. Restore the latest checkpoint.
3. Replay required events after the checkpoint.
4. Notify clients of the new session epoch.
5. Reconnect players.

The game must explicitly support this recovery model.

A checkpoint alone may not be sufficient if external operations were in progress.

### Room ownership

We might maintain:

```
Room R1 -> {
    gameServerId: game-1,
    generation: 17,
    leaseExpiry: ...
}
```

After a crash:

```
Room R1 -> {
    gameServerId: game-2,
    generation: 18
}
```

The generation protects against stale operations from an old game-server owner.

This is the same ownership and fencing principle we developed for push servers.

However, game sessions have their own state and computation ownership, so the recovery requirements are stricter.

## 11. Gaming architecture: Final decisions

| Concern          | Design                                                                            |
| ---------------- | --------------------------------------------------------------------------------- |
| Transport        | WebSocket for many games; UDP-based protocol for suitable latency-sensitive games |
| Audience         | Game room/session                                                                 |
| Routing          | Room ID → authoritative game-server owner                                         |
| Game state       | Authoritative game-server memory                                                  |
| Durable state    | Database/checkpoints for important state                                          |
| Movement updates | Usually ephemeral                                                                 |
| Game loop        | Tick-based processing                                                             |
| Message ordering | Per game session/input sequence                                                   |
| Broadcast        | Game server sends to room participants                                            |
| Reconnect        | Resume or recover session                                                         |
| Kafka            | Important events, analytics, not every movement tick                              |
| Failure recovery | Server ownership transfer and checkpoint/restart policy                           |

The central lesson: Gaming is primarily a low-latency state synchronization problem. A specialized game server often owns the room and authoritative state, while the generic push architecture is only one possible transport component.

# Part II — Design a Live Location Tracking System

## 12. Understand the requirements

Imagine we're building an Uber-style delivery or ride-tracking application.

A driver moves through a city.

The customer needs to see the driver's location in real time.

Requirements:

- Drivers continuously send location updates.
- Customers can track a particular trip.
- Multiple trips may be active simultaneously.
- The system should show recent location.
- GPS updates may arrive late or out of order.
- Not every location update needs to be persisted forever.
- Offline customers should retrieve the latest location when reconnecting.
- Nearby drivers may need to be discovered.

This system combines two distinct problems:

A. Location ingestion: Where are drivers currently located?

B. Selective distribution: Which customers should receive which driver's location?

We need to separate them.

## 13. First identify the entities

Suppose:

```
Driver A -> Trip T1 -> Customer X

Driver B -> Trip T2 -> Customer Y

Driver C -> Trip T3 -> Customer X
```

A customer may have several active tracking sessions.

The system should not broadcast every driver's location to every customer.

For example:

```
Driver A location update:

Only Customer X tracking Trip T1 needs it.
```

This is individual or trip-based routing.

But there is another use case:

When a customer requests a ride, we may need to find nearby available drivers.

That's a geographic search problem, not just a push-delivery problem.

Let's keep these two use cases separate.

## 14. Build the location ingestion architecture

The driver application sends:

```
{
  "driverId": "driver-101",
  "tripId": "trip-501",
  "sequence": 1001,
  "latitude": 12.9716,
  "longitude": 77.5946,
  "accuracyMeters": 8,
  "recordedAt": "2026-11-10T10:30:00Z"
}
```

This is received by a location ingestion service.

```
Driver App
    |
    ▼
Location Ingestion
    |
    ├── Validate update
    |
    ├── Check sequence/freshness
    |
    ├── Update latest location
    |
    └── Publish location event
```

The service can receive location updates through:

- WebSocket.
- Periodic HTTP requests.
- A combination of these, depending on mobile/network requirements.

For a driver application, WebSocket is useful when there is also bidirectional communication with the backend.

But if location updates are sent periodically and the server primarily needs to receive them, HTTPS requests may be simpler.

The customer-facing tracking stream is a different communication path.

## 15. Where should current location be stored?

We want to answer:

> Where is Driver A currently located?

This is a latest-state lookup.

A simple Redis representation:

```
driver:driver-101:location

latitude
longitude
timestamp
sequence
tripId
```

For example:

```
{
  "latitude": 12.9716,
  "longitude": 77.5946,
  "sequence": 1001,
  "timestamp": 1794316200
}
```

This is suitable for low-latency retrieval.

But what if we also need to find all available drivers within 2 kilometres of a customer's location?

A normal key-value lookup isn't sufficient.

We need geospatial indexing.

Redis supports geospatial operations that can help with radius and nearby-location queries.

For example, conceptually:

```
Find active drivers within 2 km
of (latitude, longitude)
```

We might maintain:

```
geo:active-drivers
```

Each active driver is indexed using its current coordinates.

### But is Redis the only suitable choice?

No.

For large-scale geographic search, we might use:

- Redis geospatial indexes.
- A database with native spatial indexes.
- A dedicated geospatial search service.
- A specialized location-processing system.

The choice depends on query volume, update frequency, retention, and geographic complexity.

## 16. A subtle problem: Out-of-order GPS updates

Suppose Driver A sends three updates.

```
Update 101:
    Time: 10:00:01
    Location: A

Update 102:
    Time: 10:00:02
    Location: B

Update 103:
    Time: 10:00:03
    Location: C
```

But due to network delays, the server receives them in this order:

```
103
101
102
```

If we blindly update Redis every time, the final location becomes B.

That's wrong.

The latest location is C.

We need a sequence number or another reliable version mechanism.

The storage update should be conditional:

```
Accept new location only when:

incomingSequence > storedSequence
```

For example:

```
Stored sequence = 103

Incoming sequence = 101

Reject as stale.
```

We should also consider timestamps and device clock errors.

A server-assigned or session-scoped sequence is generally a stronger ordering signal than trusting client wall-clock time alone.

### What if GPS sends duplicate updates?

The same sequence should not produce a second logical state change.

This is another use for idempotency.

## 17. How often should drivers send location updates?

This is a critical scaling decision.

Suppose:

```
100,000 active drivers

One update every second
```

Then:

\\[ 100,000 \times 1 = 100,000 \\]

location updates per second.

If every update is persisted in a relational database, we may be creating unnecessary write load.

Instead, use different rates for different purposes.

For example:

| Activity                 | Possible update policy                     |
| ------------------------ | ------------------------------------------ |
| Driver stationary        | Send less frequently                       |
| Normal travel            | Periodic updates                           |
| Fast movement            | More frequent updates                      |
| Active customer tracking | Higher freshness requirement               |
| No active trip           | Less frequent or different tracking policy |

The exact intervals are product and operational decisions.

We can also use movement-based updates.

For example:

```
Send location when:

Moved more than 20 meters

OR

5 seconds elapsed
```

These are illustrative thresholds.

This reduces unnecessary updates from stationary devices.

## 18. How does the location reach the customer?

Suppose:

```
Driver A -> Trip T1 -> Customer X
```

Customer X is connected to Push Server 2.

Driver A sends a new location.

The ingestion service publishes:

```
{
  "eventId": "loc-101",
  "tripId": "T1",
  "driverId": "driver-A",
  "sequence": 1002,
  "latitude": 12.9720,
  "longitude": 77.5950
}
```

The location router resolves:

```
Trip T1:

Driver A
Customer X
```

It then looks up Customer X's connections.

```
Customer X -> Push Server 2
```

Delivery:

```
Location Ingestion
       |
       ▼
Latest Location Store
       |
       ▼
Trip Router
       |
       ▼
Connection Registry
       |
       ▼
Push Server 2
       |
       ▼
Customer X
```

We don't need to send the update to every push server.

### What about multiple customers?

Sometimes multiple parties can track the same driver.

For example:

```
Driver A:

Customer X -> Trip T1
Support Agent -> Trip T1
Operations Dashboard -> Trip T1
```

The trip is the natural audience boundary.

We can maintain:

```
trip:T1:subscribers

    Customer X
    Support Agent
    Operations Dashboard
```

Each push server knows which local connections subscribed to Trip T1.

The routing system delivers only to relevant servers.

## 19. Why should we use latest-state delivery here?

Suppose Driver A sends:

```
Location 101
Location 102
Location 103
Location 104
Location 105
```

A customer has a slow connection.

Do they need to receive every intermediate position?

Usually not.

They need the driver's latest position.

We can coalesce updates:

```
Pending updates:
    101
    102
    103
    104
    105

Send:
    105
```

This is appropriate for map-marker updates.

However, we may still want to persist a location history for:

- Trip reconstruction.
- Analytics.
- Fraud detection.
- Operational investigations.
- Route analysis.

That is a separate data pipeline.

```
              Driver Location
                     |
                     ▼
                Ingestion
                     |
             ┌───────┴────────┐
             |                |
             ▼                ▼
        Latest State      Location History
             |                |
             ▼                ▼
          Redis          Kafka / Time-Series
             |            / Historical DB
             ▼
       Real-Time Fanout
             |
             ▼
          Customer
```

Now we have two different storage requirements.

Latest location is optimized for fast current-state queries.

Location history is optimized for durable time-based retrieval and analytics.

We shouldn't overload one store with both workloads without considering the access patterns.

## 20. What if the driver disconnects?

Suppose Driver A stops sending updates.

We should not indefinitely display their last location as if it were current.

We need freshness metadata.

For example:

```
{
  "driverId": "driver-A",
  "latitude": 12.9720,
  "longitude": 77.5950,
  "lastUpdatedAt": 1794316200,
  "status": "STALE"
}
```

Possible state transitions:

```
ACTIVE
   |
   | No updates within expected interval
   ▼
STALE
   |
   | Additional timeout
   ▼
UNAVAILABLE
```

The exact timeouts depend on the application's requirements and mobile network conditions.

The customer can then see:

```
Driver location last updated 30 seconds ago.
```

Instead of displaying a misleading moving marker.

### What about reconnecting?

When the driver reconnects:

1. Authenticate the driver.
2. Identify the active trip.
3. Send current location.
4. Validate sequence/freshness.
5. Update latest state.
6. Resume live distribution.

The customer retrieves the latest snapshot.

No need to replay every stale location update for a map marker.

## 21. Separate nearby-driver search from trip tracking

This is worth emphasizing.

A ride-hailing system has two different real-time workflows.

### Workflow A: Find nearby drivers

```
Customer's location
       |
       ▼
Geospatial Search
       |
       ▼
Nearby available drivers
       |
       ▼
Dispatch / Matching Service
```

This is a geographic query problem.

### Workflow B: Track an already assigned driver

```
Driver location
       |
       ▼
Trip-specific routing
       |
       ▼
Customer's active connection
```

This is an audience-routing and real-time delivery problem.

They might use different storage indexes and processing services.

Trying to solve both with a simple global Pub/Sub channel would be a poor design.

## 22. Live tracking architecture: Final decisions

| Concern                      | Design                                    |
| ---------------------------- | ----------------------------------------- |
| Driver-to-server transport   | HTTPS or WebSocket                        |
| Customer-to-server transport | SSE or WebSocket                          |
| Audience                     | Trip subscribers                          |
| Current location             | Redis/KV/geospatial store                 |
| Location history             | Kafka + historical database when required |
| Routing                      | Trip → active regions/servers             |
| Out-of-order updates         | Sequence validation                       |
| Slow customer                | Coalesce to latest location               |
| Offline customer             | Fetch latest snapshot                     |
| Stale driver                 | Freshness TTL/status                      |
| Nearby-driver search         | Geospatial index                          |
| Critical trip events         | Durable business events                   |

The central lesson: Live tracking is primarily a high-frequency latest-state distribution problem combined with a separate geospatial discovery problem.

# Part III — Design a Multi-Tenant Global Dashboard

## 23. Understand the requirements

Imagine we're building an enterprise monitoring/dashboard platform.

Different companies use the same platform.

```
Tenant A:
    5,000 employees

Tenant B:
    20,000 employees

Tenant C:
    100,000 employees
```

Each tenant has dashboards showing:

- Sales metrics.
- System health.
- Active users.
- Application errors.
- Revenue.
- Processing throughput.
- Operational alerts.

The dashboard continuously updates.

However, Tenant A must never receive Tenant B's information.

Also, not every user within a tenant necessarily sees the same dashboards.

For example:

```
Tenant A:

Dashboard 1:
    Sales Team

Dashboard 2:
    Operations Team

Dashboard 3:
    Administrators
```

Some users might be subscribed to multiple dashboards.

This introduces three requirements:

1. Multi-tenant data isolation.
2. Selective subscriptions.
3. Efficient aggregation and fanout.

This is not simply a global broadcast problem.

It's a combination of tenant-based and subscription-based routing.

## 24. First separate raw events from dashboard updates

Imagine a tenant's application produces:

```
Order Created
Order Shipped
Payment Completed
User Registered
Error Occurred
```

Do we send every raw event to every dashboard client?

No.

Suppose a dashboard displays:

```
Orders today: 10,000

Revenue today: $500,000

Active users: 20,000

Error rate: 0.5%
```

A new order arrives.

The dashboard might only need:

```
{
  "metric": "orders_today",
  "value": 10001
}
```

It doesn't need the complete raw order record.

We need an aggregation layer.

### Introduce a Dashboard Aggregation Service

```
                 Business Services
                        |
                        ▼
                    Kafka Events
                        |
                        ▼
               Stream Processing
                        |
                        ▼
               Dashboard Aggregator
                        |
              ┌─────────┴──────────┐
              |                    |
              ▼                    ▼
        Latest Metrics       Time-Series Data
              |                    |
              ▼                    ▼
            Redis              Analytics DB
              |
              ▼
        Dashboard Updates
              |
              ▼
          Push Servers
              |
              ▼
           Clients
```

The aggregation service processes raw events and updates derived metrics.

It can calculate:

- Counts.
- Totals.
- Rates.
- Percentages.
- Rolling averages.
- Top-N values.
- Time-window aggregations.

The push layer only delivers the resulting dashboard changes.

The push infrastructure should not calculate business metrics by reading millions of raw records.

That computation belongs in the appropriate domain or stream-processing layer.

## 25. How should dashboard subscriptions work?

Suppose User A opens:

```
Tenant A / Sales Dashboard
```

The client establishes an SSE connection:

```
GET /stream
```

After authentication, it subscribes to:

```
tenant-A:sales-dashboard
```

Conceptually:

```
User A
   |
   ▼
Push Server 1
   |
   ▼
Local Subscriptions

tenant-A:sales-dashboard
    |
    ├── User A
    └── User B
```

The distributed interest registry maintains:

```
tenant-A:sales-dashboard
    |
    ├── Push 1
    ├── Push 3
    └── Push 8
```

When the dashboard metrics change, the routing layer sends the update only to those interested push servers.

But we must address one very important concern.

### Subscription authorization

The user must be authorized to view that tenant's dashboard.

Don't assume that knowing a channel name is sufficient permission.

A safe subscription flow:

```
1. Authenticate user.

2. Identify tenant.

3. Verify tenant membership.

4. Verify dashboard permissions.

5. Register authorized subscription.

6. Deliver only permitted metric updates.
```

If the user is removed from the tenant or loses dashboard permission, the subscription must be revoked.

For sensitive multi-tenant systems, authorization must be enforced at the service boundaries, and stale local subscription caches must have an invalidation/revalidation strategy.

## 26. Should we create one topic per tenant?

Suppose we have:

```
Tenant A
Tenant B
Tenant C
...
Tenant 1,000,000
```

Should we create:

```
tenant-A-events
tenant-B-events
tenant-C-events
...
```

Not necessarily.

As we discussed in Chapter 4, we generally don't need a separate Kafka topic for every tenant.

We can use shared logical streams:

```
dashboard-events
```

And partition by an appropriate tenant or dashboard identifier.

For example:

```
Topic: dashboard-events

Key: tenantId
```

An event might contain:

```
{
  "eventId": "evt-101",
  "tenantId": "tenant-A",
  "dashboardId": "sales-dashboard",
  "metric": "orders_today",
  "value": 10001
}
```

The routing layer uses:

```
tenantId
dashboardId
```

to determine the authorized audience and destination push servers.

### Why might we use dashboardId instead of tenantId as the partition key?

Suppose one tenant has:

```
Sales Dashboard
Operations Dashboard
Security Dashboard
```

If every dashboard event for the tenant goes to one partition, a very active tenant might become a hot partition.

If the ordering requirement is per dashboard, then `dashboardId` may be a better key.

Remember:

The partition key should follow the actual ordering boundary.

Don't automatically choose tenant ID just because the application is multi-tenant.

### When is a dedicated tenant topic appropriate?

It might make sense for:

- A small number of very large enterprise tenants.
- Tenants requiring separate retention policies.
- Strong operational isolation.
- Tenant-specific access controls.
- Dedicated throughput guarantees.

But it isn't a default requirement.

## 27. How do we aggregate dashboard updates?

Suppose a tenant has 100,000 users viewing the same dashboard.

The dashboard metric changes 100 times per second.

Do we need to update every connected client 100 times per second?

Probably not.

For most dashboards, a refresh every 1–5 seconds may be sufficient.

The aggregation service can coalesce changes.

Example:

```
Raw events:

Order created
Order created
Payment completed
Order shipped
Order created
```

Instead of sending five events, the aggregator updates:

```
{
  "orders_today": 10005,
  "payments_today": 8200,
  "shipments_today": 7300
}
```

The dashboard receives the latest metrics snapshot or a compact delta.

This significantly reduces:

- Network traffic.
- Push-server work.
- Client rendering work.
- Duplicate metric computation.

### Two delivery approaches

Approach A: Snapshot updates

```
{
  "dashboardId": "sales-dashboard",
  "version": 101,
  "metrics": {
    "ordersToday": 10005,
    "revenueToday": 500100
  }
}
```

The client replaces its current state.

Simple and robust.

Approach B: Delta updates

```
{
  "dashboardId": "sales-dashboard",
  "version": 101,
  "changes": {
    "ordersToday": 1,
    "revenueToday": 100
  }
}
```

The client applies incremental changes.

More bandwidth-efficient, but the client must not miss deltas.

If it detects a sequence gap, it needs to fetch a new snapshot.

For most enterprise dashboards, snapshot updates are often the simpler starting point.

## 28. What happens when a user reconnects?

Suppose User A was viewing Dashboard D1.

They last processed:

```
version = 100
```

They reconnect.

There are two common strategies.

### Strategy A: Always send a fresh snapshot

```
Reconnect
    |
    ▼
Fetch current dashboard snapshot
    |
    ▼
Version 105
    |
    ▼
Continue live updates
```

This is generally simple because dashboards mostly care about current state.

### Strategy B: Replay metric deltas

If the system maintains a durable delta history:

```
101
102
103
104
105
```

The client can replay missed updates.

This is useful when the application needs an audit trail or exact incremental processing.

But for a typical dashboard, the snapshot approach is usually sufficient.

Again, a missed intermediate update isn't necessarily a lost business transaction.

The latest metric is what matters.

## 29. Multi-tenant isolation and workload fairness

Suppose Tenant A is extremely active.

Its dashboard generates 100,000 updates per second.

Tenant B generates only 10 updates per second.

We shouldn't allow Tenant A to consume every available push-server and fanout resource.

We need tenant-level workload isolation.

Possible controls:

- Per-tenant subscription limits.
- Per-tenant update rates.
- Per-dashboard fanout limits.
- Per-tenant bandwidth quotas.
- Dedicated workers for high-volume tenants.
- Separate infrastructure for premium or exceptional workloads where justified.

For example:

```
                 Dashboard Delivery
                         |
              ┌──────────┴──────────┐
              |                     |
              ▼                     ▼
          Tenant A              Other Tenants
              |                     |
              ▼                     ▼
       Dedicated/limited       Shared Worker
       worker capacity          capacity
              |                     |
              ▼                     ▼
          Push Fleet              Push Fleet
```

This is a bulkhead pattern applied to multi-tenant systems.

Also, use tenant-aware authorization and avoid trusting tenant identifiers supplied by clients without validating them against the authenticated identity.

A routing mistake in a multi-tenant system can become a data-isolation/security incident, not just a delivery bug.

## 30. Dashboard architecture: Final decisions

| Concern               | Design                                                                   |
| --------------------- | ------------------------------------------------------------------------ |
| Transport             | SSE is usually a natural starting choice                                 |
| Audience              | Tenant + dashboard + authorized users                                    |
| Raw event ingestion   | Kafka or event pipeline                                                  |
| Metric computation    | Stream processor / aggregation service                                   |
| Latest metric state   | Redis or analytical/materialized store                                   |
| Subscription registry | Local push memory + distributed interest registry                        |
| Topic design          | Shared dashboard events with appropriate keys                            |
| Fanout                | Dashboard-to-region-to-server                                            |
| Delivery              | Snapshot or compact delta                                                |
| Reconnect             | Fresh snapshot or cursor-based replay                                    |
| Durability            | Durable business/analytical state, not necessarily per-client deliveries |
| Tenant protection     | Authorization, isolation, quotas, and bounded fanout                     |

The central lesson: Dashboards are primarily an aggregation-and-selective-fanout problem. Don't push every raw business event directly to every user.

# Part IV — Compare All Eight Real-World Applications

## 31. One comparison that brings everything together

We've now designed eight applications.

Let's put them side by side.

| Application          | Audience                     | Transport                     | Main messaging approach                     | Authoritative storage              |
| -------------------- | ---------------------------- | ----------------------------- | ------------------------------------------- | ---------------------------------- |
| Chat                 | Conversation members         | WebSocket                     | Durable event stream + targeted fanout      | Chat DB                            |
| Collaborative editor | Document participants        | WebSocket                     | Ordered operations + targeted fanout        | Document DB + operation history    |
| Live sports          | Match subscribers            | SSE / WebSocket               | Pub/Sub + event stream where required       | Match DB                           |
| Stock market         | Symbol subscribers           | SSE / WebSocket               | High-frequency streaming + selective fanout | Market-data storage                |
| Notifications        | Individual user/device       | SSE / WebSocket + mobile push | Durable queue/stream + inbox                | Notification DB                    |
| Multiplayer gaming   | Game-room participants       | WebSocket / UDP / WebRTC      | Direct session communication                | Game server state + checkpoints    |
| Live tracking        | Trip subscribers             | SSE / WebSocket               | Latest-state updates + targeted fanout      | Trip/location stores               |
| Global dashboards    | Tenant/dashboard subscribers | SSE                           | Aggregation + selective Pub/Sub             | Business DB + materialized metrics |

Notice that all eight applications have different requirements.

Yet many of their infrastructure components are reusable.

## 32. Which components are reusable?

Let's identify the common infrastructure.

| Component                   | Chat      | Collaboration       | Sports              | Stocks              | Notifications | Gaming                           | Tracking            | Dashboard           |
| --------------------------- | --------- | ------------------- | ------------------- | ------------------- | ------------- | -------------------------------- | ------------------- | ------------------- |
| Connection gateway          | Yes       | Yes                 | Yes                 | Yes                 | Yes           | Sometimes                        | Yes                 | Yes                 |
| Connection registry         | Yes       | Yes                 | Yes                 | Yes                 | Yes           | Sometimes                        | Yes                 | Yes                 |
| Group/subscription routing  | Yes       | Yes                 | Yes                 | Yes                 | Sometimes     | Yes                              | Yes                 | Yes                 |
| Kafka                       | Often     | Often               | Optional            | Often               | Often         | Selectively                      | Optional            | Often               |
| Ephemeral Pub/Sub           | Optional  | Yes                 | Yes                 | Yes                 | Optional      | Yes                              | Yes                 | Yes                 |
| Durable business storage    | Yes       | Yes                 | Yes                 | Yes                 | Yes           | As required                      | Yes                 | Yes                 |
| Per-recipient delivery DB   | Optional  | Usually unnecessary | Usually unnecessary | Usually unnecessary | Often         | Usually unnecessary              | Usually unnecessary | Usually unnecessary |
| Sequence/version tracking   | Yes       | Essential           | Useful              | Important           | Sometimes     | Essential for selected protocols | Useful              | Useful              |
| Snapshot recovery           | Optional  | Yes                 | Yes                 | Yes                 | Inbox         | Game-specific                    | Yes                 | Yes                 |
| Heavy fanout infrastructure | Sometimes | Sometimes           | Often               | Often               | Sometimes     | Usually room-local               | Sometimes           | Sometimes           |

This table is deliberately not a list of mandatory components.

For example, a small chat application can work without Kafka.

A large collaborative system can use Kafka but still rely primarily on its document database for recovery.

A live sports application might work perfectly well with an event source, Redis, and regional fanout without needing a dedicated durable delivery database.

The requirements decide.

# Part V — The Deeper Patterns Hidden Inside These Applications

## 33. We can reduce the eight designs to six fundamental patterns

The applications look different, but their architectures are built from a smaller set of reusable patterns.

### Pattern 1: Individual targeted delivery

Examples:

- Personal notification.
- Order status.
- Account alert.

Mental model:

```
User ID
   |
   ▼
Connection Registry
   |
   ▼
Destination Push Server
   |
   ▼
Local Connection
```

Primary challenge: offline delivery and retries.

### Pattern 2: Group-based fanout

Examples:

- Chat group.
- Shared document.
- Game room.
- Tracking trip.

Mental model:

```
Group ID
   |
   ▼
Group Membership / Interest Registry
   |
   ▼
Active Push Servers
   |
   ▼
Local Subscribers
```

Primary challenge: audience resolution and dynamic membership.

### Pattern 3: Massive broadcast

Examples:

- Live sports.
- Public announcements.
- Global price feeds.

Mental model:

```
Global Event
    |
    ▼
Regional Fanout
    |
    ▼
Push Fleets
    |
    ▼
Local Clients
```

Primary challenge: network bandwidth and fanout throughput.

### Pattern 4: High-frequency latest-state streaming

Examples:

- Stock quotes.
- Driver location.
- Live dashboard metrics.

Mental model:

```
Incoming Updates
       |
       ▼
Latest-State Store
       |
       ▼
Coalescing / Filtering
       |
       ▼
Interested Clients
```

Primary challenge: controlling update volume without delivering stale or inconsistent state.

### Pattern 5: Ordered state synchronization

Examples:

- Collaborative editing.
- Chat history.
- Order lifecycle.

Mental model:

```
Versioned Events
       |
       ▼
Ordered Processing
       |
       ▼
Sequence-aware Clients
       |
       ▼
Gap Detection / Replay
```

Primary challenge: consistency and recovery.

### Pattern 6: Authoritative real-time sessions

Examples:

- Multiplayer gaming.
- Specialized collaboration sessions.

Mental model:

```
Session ID
    |
    ▼
Session Owner
    |
    ▼
Authoritative State
    |
    ▼
Participants
```

Primary challenge: low latency, session ownership, and state recovery.

These six patterns are the real reusable knowledge from our eight application designs.

# Chapter 10 — Final Understanding

## 34. The most important lessons from all eight applications

There are several architectural decisions I want you to become comfortable making.

1\. The same transport does not imply the same architecture.

Chat, sports, stocks, and dashboards can all use WebSockets/SSE.

Their routing and reliability requirements are completely different.

2\. Not every event should be durable.

A typing indicator doesn't need a database record.

A chat message does.

A stock price snapshot may need only latest-state storage.

A financial notification may require a durable inbox and delivery acknowledgement.

3\. Not every event should be broadcast.

Determine the audience first.

Then determine which servers actually host those subscribers.

4\. Not every application needs Kafka.

A game movement update shouldn't automatically go through Kafka.

An ephemeral dashboard update might only need Pub/Sub.

A durable, independently consumed business event may benefit greatly from Kafka.

5\. Not every application needs a delivery database.

If business history already allows complete recovery, a separate per-recipient delivery record may create unnecessary complexity.

6\. Connection infrastructure and application state are different.

A game server may own authoritative session state.

A chat service owns message history.

A document service owns document state.

The push gateway owns live transport connections.

These responsibilities should remain clearly defined.

# Next: Chapter 11 — The Complete Design Decision Framework

This is our final and most important chapter.

Up to now, I've explained individual concepts, components, and architectures.

Now we're going to combine all of them into one systematic approach.

The objective is to make sure that when somebody gives you a new requirement such as:

> Design a reliable real-time notification system for 50 million users.

Or:

> Design a collaborative application with millions of active sessions.

You can derive the architecture yourself.

We'll build a reusable design process:

### 1. Start with clarifying questions

What should you ask before drawing any architecture?

How do you identify which questions actually change the design?

### 2. Determine the communication pattern

WebSocket vs SSE vs HTTP streaming vs UDP vs WebRTC.

### 3. Determine the audience

Global, group, individual user, device, connection, or session.

### 4. Determine event semantics

Ephemeral, latest-state, durable event, incremental operation, or notification obligation.

### 5. Choose the messaging infrastructure

Pub/Sub, Kafka, RabbitMQ, Redis Streams, or direct RPC.

### 6. Design routing

Audience resolution, connection registry, node routing, regional routing, hierarchical fanout.

### 7. Design storage

Business DB, Redis, delivery database, event log, snapshots, and local connection memory.

### 8. Define reliability

At-most-once, at-least-once, idempotency, acknowledgement, retries, replay, ordering.

### 9. Design failure handling

Server crashes, broker failures, registry failures, network partitions, reconnection storms, backpressure.

### 10. Scale the architecture

Estimate connections, messages/sec, recipients/event, bandwidth, partitions, and fanout capacity.

### 11. Put everything together

We'll build a complete architecture from an unseen example requirement, explaining why every component exists and which components we deliberately don't introduce.

The ultimate goal is that you stop thinking in terms of "Which technology should I use?" and start thinking in terms of "What problem does this requirement introduce, and what is the simplest architecture that correctly solves it?"

