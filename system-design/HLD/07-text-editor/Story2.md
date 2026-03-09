Perfect HLD practice question. This is **exactly the kind of system MAANG interviewers love** because it touches many distributed system problems.

Since you want to **deeply understand concepts and answer confidently in interviews**, we'll learn it **as an engineering evolution story**, just like you asked.

We'll move **step-by-step**, and **only continue when you say “next.”**

---

# Story: How Engineers Ended Up Building Google Docs

Imagine it’s **2003**.

People write documents like this:

• Microsoft Word
• Save `.doc` file
• Email it to someone
• They edit it
• Send it back

Soon teams realize something painful.

```
report_final.doc
report_final_v2.doc
report_final_v3_really_final.doc
report_final_FINAL_USE_THIS.doc
```

Nobody knows:

• which file is latest
• who changed what
• work gets overwritten

This creates the **first real problem.**

---

# Problem 1 — Collaboration

Teams want:

• Multiple people editing the **same document**
• Changes visible **instantly**
• No **file versions chaos**

So someone proposes a simple idea.

---

# Version 1 — Shared Document Server

Instead of sending files around:

```
Client (Browser)
      |
      v
Document Server
      |
      v
Database
```

User opens document.

Server loads document from DB.

User edits → sends update → server saves.

Simple.

---

### Example

Document:

```
Hello world
```

User edits:

```
Hello distributed systems
```

Client sends:

```
UPDATE DOCUMENT 123
CONTENT = "Hello distributed systems"
```

Server writes to DB.

---

### Why this works (initially)

Good for:

• Single user editing
• Small teams
• Low traffic

But a **big problem appears immediately.**

---

# Problem 2 — Two Users Editing Same Time

Example.

Document:

```
Hello world
```

Two users open it.

### User A edits

```
Hello distributed systems
```

### User B edits

```
Hello world!!!
```

Both send update.

Server receives:

```
UPDATE 1
UPDATE 2
```

Which one wins?

Possible result:

```
Hello world!!!
```

User A's work is **lost**.

---

This is called:

# ❗ Lost Update Problem

Classic distributed system problem.

---

# First Attempt To Fix It

Engineers say:

"Only allow **one person editing at a time**."

So system becomes:

```
User opens doc
↓
Server locks document
↓
Only that user edits
↓
Others read-only
```

---

### This is called

# Document Locking

---

### Example

User A opens document.

```
LOCK DOC 123
```

Now:

User B sees

```
"This document is being edited by Riyaz"
```

They must wait.

---

### Advantages

Simple.

Easy to implement.

---

### But real users hate it.

Example:

```
10 people writing meeting notes
```

Only **one person can type**.

This defeats the purpose of collaboration.

---

# Problem 3 — True Real-Time Editing

Users want this:

```
User A typing
User B typing
User C typing
```

All changes visible instantly.

Like magic.

---

But this creates a **deep distributed systems challenge**.

Example.

Initial document:

```
Hello world
```

Now:

User A inserts:

```
Hello amazing world
```

User B deletes:

```
Hello
```

Both happen **at same time**.

Server must resolve:

```
Hello amazing world
Hello world
amazing world
world
???
```

Which one is correct?

This becomes a **conflict resolution problem**.

---

This is where the **real system design begins.**

Two famous techniques were invented:

1️⃣ **Operational Transformation (OT)**
2️⃣ **CRDT (Conflict-free Replicated Data Types)**

Google Docs originally used **Operational Transformation**.

---

# Interview Insight

If you say this in interview:

> Google Docs uses Operational Transformation or CRDT to resolve concurrent edits.

Interviewers immediately know you understand the problem.

---

# Where We Go Next

Next step in the story:

We will explore **how engineers invented Operational Transformation** to solve simultaneous editing.

This will cover:

• How edits are represented
• Transforming operations
• Keeping documents consistent across users
• Handling network delays
• Handling offline edits

This is **one of the most important concepts in this system.**

---

# Before we continue

Tell me if you understood:

1️⃣ Why **document locking fails**
2️⃣ Why **lost update happens**
3️⃣ Why **simultaneous editing is hard**

Then say **"next"** and we'll go to:

# The Birth of Operational Transformation (OT)

This is where the **real distributed system engineering starts.**

---
Great. Now we reach the **first truly difficult engineering problem** in collaborative editors.

This part is **very important for HLD interviews** because it demonstrates that you understand **consistency in distributed systems**.

We’ll go slowly.

---

# Step 2 — The Birth of Operational Transformation (OT)

Engineers realized something important:

Instead of sending **entire documents**, send **small editing operations**.

Because sending full documents causes overwrites.

So they changed the system.

---

# Version 2 — Send Operations Instead of Documents

Instead of this:

```text
SAVE DOCUMENT
"Hello distributed systems"
```

Clients send **operations** like:

```text
INSERT(position=6, "distributed ")
```

Example document:

```text
Hello world
```

Operation:

```text
INSERT(6, "distributed ")
```

Result:

```text
Hello distributed world
```

Now the server stores **operations instead of full content updates**.

---

# Why This Helps

Operations are **small and precise**.

Instead of overwriting the document, we apply changes step-by-step.

Example:

Document:

```text
Hello world
```

Operations:

User A:

```text
INSERT(6, "amazing ")
```

User B:

```text
DELETE(6,5)
```

---

# But We Still Have a Problem

Two users edit **at the same time**.

Initial document:

```text
Hello world
```

User A inserts:

```text
INSERT(6, "amazing ")
```

User B deletes:

```text
DELETE(6,5)
```

If server applies operations in **different order**, results differ.

---

### Case 1 — Apply A then B

Step 1

```text
Hello amazing world
```

Step 2

```text
Hello amazing
```

---

### Case 2 — Apply B then A

Step 1

```text
Hello
```

Step 2

```text
Helloamazing
```

---

Now we have **two different final documents**.

This is unacceptable.

All users must see **exact same document**.

This requirement is called:

# Strong Convergence

Every replica must eventually reach **the same state**.

---

# Operational Transformation Idea

Instead of applying operations blindly, we **transform operations against each other**.

Meaning:

When operation arrives late, we adjust its **position** based on previous edits.

---

# Example — Insert vs Insert

Initial document:

```text
Hello world
```

Two users type simultaneously.

User A:

```text
INSERT(6, "amazing ")
```

User B:

```text
INSERT(6, "beautiful ")
```

---

Without transformation:

Both insert at **position 6**.

Result could be:

```text
Hello amazing beautiful world
```

or

```text
Hello beautiful amazing world
```

---

OT ensures **consistent order**.

Suppose we decide:

**Server timestamp order**

User A arrives first.

User B operation must be **transformed**.

Original:

```text
INSERT(6, "beautiful ")
```

But A inserted **8 characters** before it.

So transform B:

```text
INSERT(14, "beautiful ")
```

---

Final document:

```text
Hello amazing beautiful world
```

All clients converge to same state.

---

# Insert vs Delete Example

Initial:

```text
Hello world
```

User A:

```text
DELETE(6,5)
```

User B:

```text
INSERT(6,"amazing ")
```

If delete happens first:

```text
Hello
```

Now insertion position must change.

OT transforms B:

```text
INSERT(6,"amazing ")
```

Final:

```text
Hello amazing
```

---

# Core OT Principle

When an operation arrives:

```text
Transform(operation, previous_operations)
```

Adjust position to maintain consistency.

---

# Simplified Algorithm

Server keeps **operation log**.

When new operation arrives:

```text
for each previous_operation:
    transform(new_operation)
```

Then apply.

---

# Architecture Now

```text
Client A
   |
Client B
   |
Client C
   |
   v
Collaboration Server
   |
Operation Log
   |
Database
```

Flow:

1. Client sends operation
2. Server transforms operation
3. Server applies operation
4. Broadcast to all clients

---

# Example Real-Time Flow

User A types:

```text
INSERT(6,"amazing ")
```

Server applies.

Then broadcasts:

```text
OPERATION 101
INSERT(6,"amazing ")
```

All clients update their document.

---

# Latency Problem

Users expect **instant typing**.

Waiting for server round-trip would feel slow.

Solution:

Clients **apply operations locally first**.

This is called:

# Optimistic UI

---

Flow becomes:

1️⃣ User types
2️⃣ Client applies edit locally
3️⃣ Send operation to server
4️⃣ Server transforms
5️⃣ Server broadcasts authoritative version

Client adjusts if needed.

---

# Important HLD Interview Insight

You should say:

> Google Docs clients perform **local optimistic edits**, while the server maintains a **canonical operation log** and performs **Operational Transformation to ensure convergence**.

That answer signals **strong distributed systems knowledge**.

---

# But OT Has Serious Problems

As systems scaled, engineers discovered OT has issues:

### 1️⃣ Complex transformation rules

Insert vs delete
Delete vs insert
Insert vs insert
Delete vs delete

Many edge cases.

---

### 2️⃣ Hard with offline editing

Users editing offline create **diverging histories**.

---

### 3️⃣ Hard to scale across regions

Multiple servers need consistent transformation order.

---

This led engineers to develop a **better model**.

---

# The Next Big Evolution

Researchers introduced:

# CRDT — Conflict-free Replicated Data Types

Instead of transforming operations,
they designed **data structures that naturally merge changes**.

No central transformation needed.

This is used by:

• Figma
• Notion
• Automerge
• Yjs

---

# Next Step (Very Important)

Next we will cover:

# CRDT for Text Editors

This will explain:

• Character IDs
• Distributed ordering
• How offline edits merge automatically
• Why CRDT scales better globally

This is **one of the most impressive things to mention in interviews**.

---

Before moving forward, confirm you understand:

1️⃣ Why sending **operations is better than documents**
2️⃣ Why **operation order breaks consistency**
3️⃣ What **Operational Transformation does**

Then say **“next”**.

The next part is **mind-blowing distributed system design**.

---

Great. Now we reach one of the **most elegant ideas in distributed systems**.

This is where modern collaborative systems (Notion, Figma, etc.) moved **beyond Operational Transformation**.

---

# Step 3 — CRDT: Conflict-Free Replicated Data Types

Engineers realized something:

> Instead of fixing conflicts **after they happen**, design a system where **conflicts never break the document**.

Meaning:

No matter the order of operations, **all replicas eventually converge**.

This idea is called:

**CRDT (Conflict-free Replicated Data Type)**.

---

# First Principle of CRDT

Every change must satisfy two properties:

**1️⃣ Commutative**

Order of operations should not matter.

```
A then B  =  B then A
```

---

**2️⃣ Idempotent**

Applying an operation twice should not break state.

```
Apply(op)
Apply(op) again
State remains correct
```

---

# The Big Idea

Instead of thinking of a document as a **string**:

```
Hello world
```

CRDT represents a document as **a list of uniquely identified characters**.

Each character has a **globally unique ID**.

Example:

| Character | ID    |
| --------- | ----- |
| H         | (1,A) |
| e         | (2,A) |
| l         | (3,A) |
| l         | (4,A) |
| o         | (5,A) |

Where:

```
(number, userID)
```

Example:

```
(5,A)
```

means:

```
5th character inserted by User A
```

---

# Why Unique IDs Matter

Because two users inserting text **never overwrite each other**.

Each character becomes an **independent object**.

---

# Example — Concurrent Insert

Initial document:

```
Hello world
```

Two users type at the same position.

User A inserts:

```
amazing
```

User B inserts:

```
beautiful
```

---

### With CRDT

Characters are assigned IDs.

User A:

```
(6,A) a
(7,A) m
(8,A) a
...
```

User B:

```
(6,B) b
(7,B) e
...
```

Now ordering rule decides final position.

Example rule:

```
Sort by position
Then by userID
```

Final result might be:

```
Hello amazing beautiful world
```

or

```
Hello beautiful amazing world
```

But **every replica produces the same result**.

No transformation required.

---

# CRDT Insert Operation

Instead of:

```
INSERT(position=6)
```

CRDT operation is:

```
INSERT(after_character_id)
```

Example:

```
INSERT(after=(5,A), value="a", id=(6,A))
```

Meaning:

Insert character **after ID (5,A)**.

---

# Example Document Representation

Instead of storing:

```
Hello
```

We store:

| ID    | Character |
| ----- | --------- |
| (1,A) | H         |
| (2,A) | e         |
| (3,A) | l         |
| (4,A) | l         |
| (5,A) | o         |

---

Now User B inserts after `(2,A)`.

```
INSERT(after=(2,A), id=(3,B), value="X")
```

Document becomes:

| ID    | Character |
| ----- | --------- |
| (1,A) | H         |
| (2,A) | e         |
| (3,B) | X         |
| (3,A) | l         |
| (4,A) | l         |
| (5,A) | o         |

Displayed:

```
HeXllo
```

---

# Delete in CRDT

Deleting characters is tricky.

If we physically delete characters, replicas might disagree.

So CRDT uses **tombstones**.

Instead of removing:

```
l
```

We mark:

```
deleted = true
```

---

Example:

| ID    | Character | Deleted |
| ----- | --------- | ------- |
| (1,A) | H         | false   |
| (2,A) | e         | false   |
| (3,A) | l         | true    |
| (4,A) | l         | false   |
| (5,A) | o         | false   |

Displayed:

```
Helo
```

---

# Why Tombstones?

Because other operations may reference that character.

If removed completely:

```
INSERT(after=(3,A))
```

would fail.

---

# CRDT Advantages

### 1️⃣ Works Offline

User edits offline:

```
Laptop on plane ✈️
```

Later reconnects.

Operations merge automatically.

---

### 2️⃣ No Central Transformation

OT requires server ordering.

CRDT allows **peer merging**.

---

### 3️⃣ Scales Globally

Multiple servers across regions can merge operations.

---

# Real Systems Using CRDT

| System     | Technology          |
| ---------- | ------------------- |
| Figma      | CRDT-like structure |
| Notion     | CRDT                |
| Automerge  | CRDT                |
| Yjs        | CRDT                |
| Redis CRDT | CRDT                |

---

# But CRDT Has a Major Problem

CRDT stores **a lot of metadata**.

Example:

Document:

```
Hello world
```

Instead of:

```
11 bytes
```

CRDT stores:

```
ID + metadata per character
```

Maybe:

```
200+ bytes
```

For large docs:

```
Millions of characters
```

Memory explodes.

---

# Solution Engineers Use

They combine:

**CRDT + Compaction**

Periodically:

```
1. Merge characters
2. Remove tombstones
3. Rebuild document
```

This is called:

**Snapshotting / Garbage Collection**

---

# Our System Architecture Now

```
Clients
   |
WebSocket
   |
Collaboration Servers
   |
Operation Log
   |
Storage
```

Clients exchange CRDT operations.

Server stores:

```
operation log
snapshots
```

---

# Interview Gold Statement

You can say:

> Modern collaborative editors often use **CRDT-based text models where each character has a unique identifier and operations reference other characters instead of positions. This allows concurrent edits to merge without operational transformation.**

Interviewers love hearing that.

---

# But We Haven’t Designed The System Yet

So far we only solved **editing conflicts**.

But real Google Docs also needs:

• Document storage
• Millions of users
• Real-time sync
• Sharding
• Replication
• Version history
• Fault tolerance
• Presence (cursor positions)
• Permissions
• Autosave
• Offline editing

This is where **true system design begins**.

---

# Next Step

Next we will design the **first real architecture**.

We will answer:

### Where is the document stored?

```
Browser
   ↓
Load doc
```

From where?

```
Cache?
Database?
Object store?
```

We'll design:

### Document Storage System

Topics we'll cover:

• Document data model
• Snapshots vs operation logs
• Version history
• Storage choice (SQL vs NoSQL)
• Read/write flows

---

Reply **"next"** and we will start building the **actual Google Docs architecture** step-by-step like a real production system.

This is where **sharding, scaling, and replication start appearing**, which is exactly what interviewers expect.

---

Excellent. Now we move from **editing algorithms → actual system architecture**.

This is the point in an interview where you transition from **theoretical collaboration model (CRDT/OT)** to **distributed system design**.

---

# Step 4 — Where Is the Document Stored?

Let's imagine the first real production version of our editor.

Users expect this:

1️⃣ Open document instantly
2️⃣ Edit in real-time
3️⃣ Changes autosave
4️⃣ Document history exists

So engineers ask:

> **How should we store documents?**

---

# Naive Idea — Store Whole Document

The simplest approach:

```text
documents table
---------------
doc_id
content
updated_at
```

Example:

| doc_id | content     |
| ------ | ----------- |
| 123    | Hello world |

Every edit:

```text
UPDATE documents
SET content="Hello amazing world"
```

---

# Why This Fails Immediately

Imagine:

```
500 users editing a doc
each typing 5 chars/sec
```

That means:

```
2500 updates / second
```

Database writes become insane.

Also:

### Problem 1 — Write Amplification

Typing one letter:

```
INSERT 'a'
```

But database writes:

```
entire document
```

Example:

```
1 MB document
```

Typing **1 character → rewrite 1 MB**.

Extremely inefficient.

---

### Problem 2 — Version History

Google Docs supports:

```
File → Version history
```

If we store only the latest document, history disappears.

---

### Problem 3 — Collaboration

Real-time edits mean **many small operations**.

Saving entire doc each time wastes bandwidth.

---

# Better Idea — Operation Log

Instead of storing document state, store **editing operations**.

Example:

Document:

```
Hello world
```

Operation log:

```text
1 INSERT(0,"Hello")
2 INSERT(5," world")
3 INSERT(6,"amazing ")
4 DELETE(11,5)
```

Final state reconstructed by replaying operations.

---

# Advantages

### Efficient writes

Each keystroke:

```
1 operation
```

Instead of:

```
full document rewrite
```

---

### Version history becomes easy

Just replay operations up to a timestamp.

Example:

```
version 1 → op 1-10
version 2 → op 1-20
version 3 → op 1-50
```

---

### Collaboration friendly

Operations are exactly what we broadcast to clients.

---

# But This Creates A New Problem

Large documents accumulate **millions of operations**.

Example:

```
doc editing for 3 years
```

Operations:

```
50 million ops
```

Loading document would require replaying **50M operations**.

That is too slow.

---

# Solution — Snapshots + Operation Log

Engineers combine **both approaches**.

Store:

```
1️⃣ Document snapshots
2️⃣ Operation logs after snapshot
```

---

# Example

Snapshot:

```
version 1000
document_state
```

Operations after snapshot:

```
op1001
op1002
op1003
...
```

To load document:

```
load snapshot
apply recent operations
```

Very fast.

---

# Example Flow

Document:

```
Hello world
```

Snapshot:

```text
snapshot_v1
"Hello world"
```

Operations:

```text
op1 INSERT(6,"amazing ")
op2 DELETE(11,5)
```

To reconstruct:

```
snapshot
→ apply op1
→ apply op2
```

Result:

```
Hello amazing
```

---

# Storage Architecture

Now we introduce the first real backend architecture.

```
                 +------------------+
                 |  API Gateway     |
                 +------------------+
                          |
                          v
                +-------------------+
                | Collaboration     |
                | Server            |
                +-------------------+
                   |            |
                   v            v
           Operation Log      Snapshot Store
                DB             Object Storage
```

---

# Where Do Snapshots Live?

Snapshots can be large.

Example:

```
10 MB document
```

Storing in database is expensive.

Better option:

```
Object Storage
```

Examples:

• Amazon S3
• Google Cloud Storage

---

# Where Do Operations Live?

Operations are small.

Example:

```
INSERT(6,"a")
```

Perfect for a **database**.

Options:

SQL:

• PostgreSQL
• MySQL

Or NoSQL:

• Apache Cassandra
• Amazon DynamoDB

---

# Example Data Model

### Documents Table

```
documents
---------
doc_id
owner_id
created_at
```

---

### Snapshots Table

```
snapshots
---------
doc_id
snapshot_id
s3_url
version
created_at
```

---

### Operations Table

```
operations
----------
doc_id
operation_id
user_id
operation_data
timestamp
```

---

# Document Load Flow

User opens doc.

```
Browser
   |
   v
API Server
```

Server does:

```
1 get latest snapshot
2 load operations after snapshot
3 reconstruct document
4 send to client
```

---

# Autosave

Instead of manual save, system autosaves.

Client sends operations continuously.

Example:

```
User types: Hello
```

Client sends:

```
INSERT(0,H)
INSERT(1,e)
INSERT(2,l)
INSERT(3,l)
INSERT(4,o)
```

Server writes them to operation log.

---

# Snapshot Creation

Every N operations:

```
1000 ops
```

Server creates snapshot.

```
document_state
```

Uploads to:

```
S3 / object storage
```

Deletes old operations.

---

# Why This Architecture Is Powerful

It gives us:

### Efficient writes

Operations are tiny.

---

### Fast loading

Snapshot avoids replaying millions of ops.

---

### Version history

Operation log is history.

---

### Crash recovery

Replay operations to rebuild state.

---

# Interview Tip

When designing Google Docs storage say:

> The document state is maintained using **snapshots combined with an append-only operation log**. Snapshots are stored in object storage while operations are stored in a database for efficient writes and reconstruction.

This signals **production-level thinking**.

---

# But We Have Not Solved Real-Time Collaboration Yet

We still need to answer:

```
How do 500 users edit a document simultaneously?
```

HTTP polling would be too slow.

Engineers realized they need **persistent connections**.

So the next big invention is:

# Real-Time Collaboration Servers

Using:

```
WebSockets
```

This allows edits to propagate **within milliseconds**.

---

# Next Step

Next we will design the **Real-Time Collaboration Layer**.

This is where topics like:

• WebSockets
• Horizontal scaling
• Sticky sessions
• Pub/Sub systems
• Redis fanout
• Multi-server sync

come in.

This part is **very important for HLD interviews**.

---

Reply **“next”** and we'll build the **real-time collaboration architecture** used in systems like Google Docs.

This is where the system becomes truly **distributed and scalable**.

---

Excellent. Now we move to **the heart of Google Docs architecture** — the **Real-Time Collaboration Layer**.

This is where most **HLD interviews spend time**, because it involves:

* WebSockets
* horizontal scaling
* pub/sub
* consistency between servers
* failure handling

We will again follow the **engineering evolution story**.

---

# Step 5 — Real-Time Collaboration

Users expect this experience in **Google Docs**:

User A types:

```
Hello
```

User B sees it **instantly**.

Latency expectation:

```
< 100 ms
```

If it takes even **1 second**, the experience feels broken.

So engineers must design **low-latency communication**.

---

# Version 1 — HTTP Polling

The first naive idea:

Clients ask server repeatedly:

```
GET /doc/updates
GET /doc/updates
GET /doc/updates
```

Architecture:

```
Client
  |
HTTP
  |
API Server
  |
Database
```

---

### Why this fails

Imagine:

```
100 users
poll every 1 second
```

Requests:

```
100 requests/sec
```

For **1 document**.

Now imagine:

```
1M active docs
```

Servers collapse.

Also latency becomes:

```
0–1 second
```

Too slow.

---

# Version 2 — Long Polling

Instead of polling every second:

Client asks server:

```
GET /doc/updates
```

Server **keeps request open** until update occurs.

Flow:

```
Client -> request
Server waits
User edits
Server responds
Client reconnects
```

Better than polling.

But still problematic:

* HTTP connections constantly reopening
* heavy load on servers
* inefficient

---

# Version 3 — WebSockets (Correct Solution)

Engineers switch to **persistent connections**.

Protocol:

```
WebSocket
```

Connection stays open for entire editing session.

Architecture:

```
Client
   |
WebSocket
   |
Collaboration Server
```

Now communication becomes:

```
User types
↓
Send operation instantly
↓
Server broadcasts
↓
All clients update
```

Latency:

```
~10–50 ms
```

Perfect.

---

# Real-Time Collaboration Architecture

```
           +----------------------+
           |      API Gateway     |
           +----------------------+
                     |
                     v
          +-----------------------+
          | Collaboration Server  |
          +-----------------------+
           |           |          |
           v           v          v
      Client A     Client B    Client C
```

Clients maintain WebSocket connection with collaboration server.

---

# Edit Flow

User A types:

```
INSERT(5,"a")
```

Flow:

```
Client A
   |
WebSocket
   |
Collaboration Server
   |
Broadcast to other clients
```

Clients B and C receive:

```
INSERT(5,"a")
```

Their documents update instantly.

---

# But Now A Big Problem Appears

What if we have:

```
1 million concurrent users
```

A single server cannot handle that many WebSocket connections.

Typical WebSocket server capacity:

```
~50k connections
```

We must scale horizontally.

---

# Horizontal Scaling

Instead of one collaboration server:

```
           Load Balancer
                |
      -------------------------
      |           |           |
Collab S1     Collab S2    Collab S3
```

Clients connect to **different servers**.

---

# Problem — Clients On Different Servers

Example:

```
User A -> Server 1
User B -> Server 2
User C -> Server 3
```

User A edits.

Server 1 knows about it.

But servers 2 and 3 do **not**.

So B and C never see update.

We must sync servers.

---

# Solution — Pub/Sub Layer

Servers communicate using a **publish/subscribe system**.

Architecture becomes:

```
             Load Balancer
                   |
        -------------------------
        |           |           |
      S1           S2          S3
        \           |          /
         \          |         /
           --------Pub/Sub------
```

Now:

```
Server receives edit
↓
Publish event
↓
All servers receive event
↓
Broadcast to their clients
```

---

# Example Flow

User A types.

```
INSERT(5,"a")
```

Flow:

```
Client A
 ↓
Server 1
 ↓
Publish event
 ↓
Pub/Sub system
 ↓
Server 2 + Server 3 receive
 ↓
Broadcast to clients
```

All users update.

---

# Technologies Used

Typical Pub/Sub options:

In-memory:

• Redis Pub/Sub

Streaming systems:

• Apache Kafka
• Google Cloud Pub/Sub

---

# Why Redis Is Often Used

Because collaboration updates must be **extremely fast**.

Redis latency:

```
~1 ms
```

Perfect for broadcasting edits.

---

# Message Example

Published event:

```
{
  doc_id: 123,
  operation: INSERT,
  position: 5,
  char: "a",
  user: 42
}
```

Subscribers:

```
Collab Server 1
Collab Server 2
Collab Server 3
```

Each sends event to connected clients.

---

# Presence (Cursors & User Names)

Another feature users expect:

```
Riyaz is editing...
```

And colored cursors.

These updates also flow via WebSockets.

Example message:

```
{
  type: cursor_update,
  user: "Riyaz",
  position: 25
}
```

These events **do not go to storage**, only real-time layer.

---

# Sticky Sessions

Important question in interviews:

> Why do we need sticky sessions?

Because a user must remain connected to the **same collaboration server**.

Otherwise:

```
operation queues break
```

Load balancer ensures:

```
User session → same server
```

Techniques:

* cookie affinity
* consistent hashing

---

# Failure Scenario

Suppose:

```
Collaboration Server 2 crashes
```

Clients connected to it lose connection.

Recovery:

```
Client reconnects
↓
Load balancer routes to another server
↓
Server syncs document state
```

Since operations are stored in database, state is recoverable.

---

# Final Real-Time Architecture

```
                    +------------------+
                    |   Load Balancer  |
                    +------------------+
                           |
            -----------------------------------
            |                |                |
     +--------------+ +--------------+ +--------------+
     | Collab S1    | | Collab S2    | | Collab S3    |
     +--------------+ +--------------+ +--------------+
           |                 |                 |
           ---------------- Pub/Sub ----------------
                           |
                        Redis/Kafka
                           |
                     Operation Storage
```

---

# Interview Key Points

When asked about real-time collaboration say:

> Clients maintain WebSocket connections with collaboration servers. Edits are sent as operations and broadcast to other clients. Multiple collaboration servers synchronize edits through a pub/sub messaging layer such as Redis or Kafka.

That answer shows **production-level architecture knowledge**.

---

# But We Still Haven’t Solved Huge Problems

We still need to design:

### 1️⃣ Document Sharding

```
Millions of documents
```

How do we distribute them across servers?

---

### 2️⃣ Database Scaling

```
Billions of operations
```

How do we shard the operation log?

---

### 3️⃣ Replication

Prevent data loss.

---

### 4️⃣ Global Collaboration

Users editing from:

```
India
US
Europe
```

---

### 5️⃣ Offline Editing

Users reconnect and merge edits.

---

These topics are **very important in senior HLD interviews**.

---

# Next Step

Next we will design:

# Document Sharding & Scaling Strategy

This will answer:

* how documents are distributed across servers
* how to avoid hot documents
* how to scale to millions of active documents

This is where **consistent hashing, partitioning, and replication** appear.

---

Reply **“next”** and we’ll go into the **deep distributed system scaling part** (the part most candidates struggle with in interviews).

---

Great. Now we enter the **scaling part** of the system design.
This is where **senior interviews usually go deeper**, because the interviewer wants to see if you can think about **millions of documents, billions of edits, and hot documents**.

We'll again follow the **engineering evolution story**.

---

# Step 6 — Document Sharding (Scaling the System)

Let’s imagine our system becomes successful like **Google Docs**.

Traffic numbers might look like this:

```text
500M documents
20M daily active users
1M concurrent editing sessions
```

Our current architecture:

```
Collaboration Servers
        |
        v
Operation Database
```

If we keep **all documents in one database**, it will fail quickly.

---

# Problem 1 — Database Size

Suppose each document generates:

```text
100K operations
```

If we have:

```text
500M documents
```

Operations become:

```text
50 trillion operations
```

A single database cannot handle this.

We must **split data across many databases**.

This is called:

# Sharding

---

# What is Sharding?

Sharding means:

```
Split data across multiple servers
```

Instead of:

```
Database 1
  |
  |-- All documents
```

We create:

```
DB1   DB2   DB3   DB4
 |     |     |     |
docs docs docs docs
```

Each database stores **a subset of documents**.

---

# First Idea — Shard by User

Example:

```
user_id % 4
```

Distribution:

```
Shard 0 → user 0,4,8
Shard 1 → user 1,5,9
Shard 2 → user 2,6,10
Shard 3 → user 3,7,11
```

This works for many systems.

But it **fails for collaborative editors**.

---

# Why Sharding by User Fails

Consider this document:

```
Team design doc
```

Editors:

```
10 users
```

If we shard by user:

```
User A → DB1
User B → DB2
User C → DB3
```

Now the **same document exists across multiple shards**.

This creates:

```
cross-shard coordination
```

Extremely complicated.

---

# Better Strategy — Shard by Document ID

Instead of user:

```
doc_id % N
```

Example:

```
doc_123 → shard 3
doc_124 → shard 0
doc_125 → shard 1
```

Now **all operations for a document live on the same shard**.

This is very important.

---

# Why This Works

Editing operations are **document-specific**.

Example:

```
INSERT(5,"a")
```

Belongs only to:

```
doc_123
```

So we guarantee:

```
All operations for doc_123 → same shard
```

This removes cross-shard complexity.

---

# Sharded Architecture

```
                API Gateway
                     |
                     v
            Collaboration Servers
                     |
          -------------------------
          |           |           |
        Shard1      Shard2      Shard3
          |           |           |
     Operation DB Operation DB Operation DB
```

Each shard stores:

```
documents
operations
snapshots
```

---

# How Servers Know Which Shard To Use

We use a **routing service**.

Flow:

```
Client opens doc_123
↓
API server
↓
Shard lookup
↓
Shard 3
```

The lookup can be done using:

```
doc_id % N
```

or using a **metadata service**.

---

# But Another Big Problem Appears

Some documents become extremely popular.

Example:

```
Company handbook
```

Editors:

```
10,000 users
```

All edits go to:

```
one shard
```

This creates a **hot shard problem**.

---

# Hot Document Problem

Example:

```
doc_999
```

Huge team editing.

All operations:

```
Shard 4
```

That shard becomes overloaded.

CPU spikes.

Latency increases.

---

# How Real Systems Handle Hot Documents

Instead of scaling the **database**, they scale the **collaboration layer**.

Meaning:

```
Many collaboration servers
handling the same document
```

Example:

```
Collab Server 1
Collab Server 2
Collab Server 3
```

All handle clients editing the same doc.

Database still stores operations sequentially.

This distributes **connection load**, which is the biggest cost.

---

# Another Optimization — In-Memory Document State

Rebuilding document by replaying operations repeatedly is expensive.

So collaboration servers keep **document state in memory**.

Example:

```
doc_123
```

Loaded into server memory.

When operation arrives:

```
apply to memory
broadcast to clients
append to DB
```

This reduces database reads dramatically.

---

# What Happens When Server Crashes?

Memory state disappears.

Recovery process:

```
1 load snapshot
2 replay recent operations
3 rebuild document
```

Then continue serving users.

---

# Distributed Cache Layer

Often systems add cache:

```
             Collaboration Servers
                     |
                 Cache Layer
                     |
                   Database
```

Common technology:

• Redis

Cache can store:

```
recent document snapshots
hot documents
```

This prevents constant DB reads.

---

# Scaling Numbers (Interview Style)

Example capacity planning:

Assume:

```
1M concurrent editors
```

If each server supports:

```
50k WebSocket connections
```

Servers required:

```
1,000,000 / 50,000 = 20 servers
```

Add redundancy:

```
~40 servers
```

Interviewers love seeing this kind of reasoning.

---

# Final Scaled Architecture

```
                        Load Balancer
                             |
                ---------------------------------
                |               |               |
         Collab Server     Collab Server    Collab Server
                |               |               |
                ----------- Pub/Sub Layer -----------
                             |
                         Redis/Kafka
                             |
                ---------------------------------
                |               |               |
            DB Shard 1      DB Shard 2      DB Shard 3
                |               |               |
             Snapshots        Snapshots       Snapshots
           (Object Store)   (Object Store)  (Object Store)
```

Snapshots stored in object storage like:

• Amazon S3

---

# Interview Gold Summary

You can say:

> Documents are partitioned by document ID so all operations for a document live in the same shard. Collaboration servers maintain in-memory document state and broadcast operations via WebSockets. Shards store operation logs and snapshots, while object storage stores document snapshots.

That answer shows **strong system design ability**.

---

# But Two Critical Pieces Are Still Missing

To finish a **complete HLD answer**, we still need:

### 1️⃣ Replication & Fault Tolerance

What happens if:

```
database shard dies
```

or

```
data center outage
```

---

### 2️⃣ Global Collaboration (Multi-Region)

Users editing from:

```
India
US
Europe
```

How do we keep latency low globally?

---

These two topics are **often the final part of senior system design interviews**.

---

If you want, next we will cover:

# Replication, Fault Tolerance, and Multi-Region Design

This part will connect ideas you were exploring earlier like:

* replication
* consensus
* distributed logs
* disaster recovery

Reply **“next”** and we’ll finish the system like a **real production-grade Google Docs architecture**.

---

Great. Now we reach the **final and most “senior-level” part** of the system design: **Replication, Fault Tolerance, and Multi-Region Architecture**. This is where interviewers often push deeper to see if you understand **data durability, availability, and global latency**.

We’ll continue the **engineering evolution story**.

---

# Step 7 — Replication & Fault Tolerance

So far we built:

* Collaboration servers (WebSocket)
* Pub/Sub for real-time updates
* Sharded databases for operations
* Snapshots stored in object storage
* In-memory document state

But now imagine something bad happens.

Example:

```text
DB Shard 2 crashes
```

What happens?

If that shard stores:

```text
doc_120
doc_121
doc_122
```

Then all those documents are **lost or unavailable**.

Unacceptable for a system like **Google Docs**.

So engineers introduce **replication**.

---

# Database Replication

Each shard is not just one database.

Instead we create **primary + replicas**.

Architecture:

```text
         Primary DB (Shard 1)
             |
     ---------------------
     |                   |
Replica DB           Replica DB
```

Flow:

```text
Writes → Primary
Reads  → Replicas
```

This gives two advantages:

### 1️⃣ Fault tolerance

If primary crashes:

```text
Replica promoted to primary
```

### 2️⃣ Read scaling

Many reads can go to replicas.

---

# Example Operation Flow

User edits document:

```text
INSERT(5,"a")
```

Flow:

```text
Client
 ↓
Collaboration Server
 ↓
Primary DB
 ↓
Replication stream
 ↓
Replica DBs
```

Replication technologies often used:

* PostgreSQL streaming replication
* MySQL binlog replication

---

# But Replication Introduces Another Problem

Replication is usually **asynchronous**.

Meaning:

Primary writes:

```text
operation 101
```

Replica might receive it **milliseconds later**.

If a server reads from replica immediately:

```text
operation missing
```

This is called:

# Replica Lag

For collaborative editing we must ensure:

```text
All edits appear in correct order
```

Solution:

For **active editing sessions**, always read from:

```text
Primary shard
```

Replicas used for:

* history queries
* analytics
* backups

---

# Disaster Recovery (Data Center Failure)

Imagine an entire data center goes down.

Example:

```text
AWS region outage
```

If all shards are there, the service dies.

So companies replicate across **regions**.

Example regions:

```text
US
Europe
Asia
```

Each region has copies of data.

---

# Multi-Region Architecture

```text
              Global Load Balancer
                       |
          ----------------------------------
          |                |               |
       US Region        EU Region       Asia Region
          |                |               |
   Collab Servers    Collab Servers   Collab Servers
          |                |               |
        DB Shards        DB Shards       DB Shards
```

Users connect to **nearest region** to reduce latency.

---

# Latency Example

User in India connecting to US server:

```text
~250 ms latency
```

User connecting to Asia region:

```text
~30 ms latency
```

Huge improvement.

---

# But Now a Hard Problem Appears

Suppose two users edit same document.

User A:

```text
India
```

User B:

```text
US
```

If both edit simultaneously, which region is **source of truth**?

This is a classic distributed systems challenge.

---

# Two Possible Approaches

## Approach 1 — Single Region Per Document

Each document is **owned by one region**.

Example:

```text
doc_123 → US region
```

If a user from India edits it:

```text
request routed to US
```

Advantages:

* simple consistency
* easy ordering of operations

Disadvantage:

```text
higher latency for distant users
```

---

## Approach 2 — Multi-Region CRDT

Because we use **CRDT operations**, different regions can accept edits independently.

Flow:

```text
User edits in India
↓
Asia region applies CRDT op
↓
Replicate operation globally
↓
Other regions merge operations
```

Since CRDT operations are **commutative**, order doesn't break consistency.

This is why CRDT is powerful for **global collaboration**.

---

# Replication via Distributed Log

Operations are often replicated using a **distributed log system**.

Example technologies:

* Apache Kafka
* Google Cloud Pub/Sub

Architecture:

```text
Collaboration Servers
        |
        v
   Event Log (Kafka)
        |
        v
   Database Shards
```

Benefits:

* durable operation history
* replayable events
* easy cross-region replication

---

# Backup Strategy

Even with replication, backups are required.

Typical strategy:

### Snapshot backups

Daily snapshot of database.

Stored in object storage like:

* Amazon S3

### Operation log backups

Since operations are append-only:

```text
backup incremental logs
```

Recovery flow:

```text
restore snapshot
replay operation logs
```

Exactly like we discussed earlier.

---

# Failure Scenarios (Interview Favorite)

You should always discuss a few.

### Collaboration Server Crash

Recovery:

```text
client reconnects
server reloads document state
```

---

### Database Shard Crash

Recovery:

```text
promote replica
```

---

### Pub/Sub Failure

Use persistent messaging like:

* Apache Kafka

so events are not lost.

---

### Network Partition

CRDT operations allow **eventual consistency**.

When connectivity returns:

```text
operations merge automatically
```

---

# Final Production Architecture

```text
                      Global Load Balancer
                              |
              -------------------------------------
              |                |                  |
           US Region        EU Region         Asia Region
              |                |                  |
      ----------------    ----------------   ----------------
      | Collab Servers |   | Collab Servers | | Collab Servers |
      ----------------    ----------------   ----------------
              |                |                  |
             Pub/Sub (Kafka / Redis Streams)
                              |
                       Operation Log
                              |
             --------------------------------
             |              |               |
         DB Shard 1     DB Shard 2      DB Shard 3
             |              |               |
        Primary + Replicas
             |
        Snapshot Storage
        (Amazon S3 / GCS)
```

---

# How to Answer This in an Interview (Short Version)

You could summarize like this:

> A collaborative text editor maintains document state using an operation log and periodic snapshots. Clients maintain WebSocket connections with collaboration servers for low-latency updates. Edits are broadcast through a pub/sub layer to other servers. Documents are sharded by document ID to scale storage. Each shard uses primary-replica replication for fault tolerance. For global users, documents can either be region-owned or replicated across regions using CRDT operations to merge concurrent edits.

That answer usually **scores very highly in HLD interviews**.

---

✅ Now we have covered:

* collaboration algorithms (OT vs CRDT)
* operation logs
* snapshots
* WebSockets
* pub/sub
* sharding
* scaling
* replication
* multi-region architecture

This is essentially a **complete Google Docs HLD**.

---

If you'd like, I can also show you the **next level interview depth**, which most candidates miss:

1️⃣ **Cursor synchronization & presence system**
2️⃣ **Operational ordering & sequence numbers**
3️⃣ **Undo/redo across multiple users**
4️⃣ **Security & permissions model**
5️⃣ **Rate limiting & abuse protection**
6️⃣ **Handling extremely large documents**

These are the **details that differentiate strong senior candidates**.

If you want, say **“deep dive”** and we’ll go into those advanced parts.

---
