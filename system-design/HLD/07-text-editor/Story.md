# HLD: Building Google Docs — A Story 📖

Let's start from the very beginning. Before we talk about any system design, let's understand **what problem we're even trying to solve.**

---

## Chapter 1: The Problem — "Why can't we just use Microsoft Word?"

It's 2005. You and your teammate Priya are working on a project report.

You write half of it, email her the `.doc` file. She edits it, emails it back. You edit it again. She edits it again.

Now you have:
```
report_final.doc
report_final_v2.doc
report_priya_edits.doc
report_FINAL_USE_THIS.doc
report_FINAL_FINAL.doc  ← everyone has been here
```

**The pain points were clear:**
- ❌ No real-time collaboration
- ❌ Only one person can edit at a time
- ❌ Version conflicts are a nightmare
- ❌ File lives on someone's local machine — what if it crashes?

So the question became: **"What if the document lived on the internet, and multiple people could edit it at the same time?"**

Sounds simple. But it opens up a **Pandora's box of problems.**

---

## The First Naive Idea 💡

> *"Easy! Just store the document on a server. Everyone reads from and writes to the server."*

Here's what that looks like:

```
User A types "Hello"  →  saves to server  →  Document = "Hello"
User B types "World"  →  saves to server  →  Document = "World"  ← A's work is GONE!
```

This is called a **write conflict** — and it's the core problem of collaborative editing.

**The server doesn't know who to trust.** Both A and B sent valid writes. Last write simply wins, and the other person's work vanishes.

This is why Google Docs is actually a **very hard engineering problem** disguised as a simple text box.

---

## So what do we actually need to build?

Before jumping into solutions, let's list what a real collaborative editor needs:

| Requirement | What it means |
|---|---|
| **Real-time sync** | When A types, B sees it within milliseconds |
| **No data loss** | Both A and B's edits must survive |
| **Conflict resolution** | When A and B edit the same word simultaneously, we need a sensible result |
| **Offline support** | You should be able to type even without internet, and sync later |
| **History/Versioning** | "Show me what this doc looked like yesterday" |
| **Permissions** | Owner, Editor, Viewer roles |
| **Scalability** | Millions of docs, thousands of concurrent users per doc |

---

## Where do we start?

The **single hardest problem** here — the one everything else is built around — is:

> ### How do two people edit the same document at the same time without destroying each other's work?

This is what we'll tackle **first**, because every other design decision flows from this answer.

---

### Before I move to the solution — make sure this is clear:

**Quick Check ✅**

Imagine User A and User B are both looking at the text: `"Hello"`

- User A adds `" World"` at the end → wants: `"Hello World"`
- User B adds `" There"` at the end → wants: `"Hello There"`

Both edits happen at the **exact same millisecond**.

**What should the final document say?** And how do we get there without losing either person's edit?

Take a moment to think about it — then tell me when you're ready and we'll dive into **Operational Transformation (OT)**, which is exactly how Google Docs solves this. It's a beautiful idea. 🚀
---
# Chapter 2: Operational Transformation (OT) — The Heart of Google Docs 🧠

## The Story Begins with a Simple Observation

The engineers thought: *"What if instead of sending the entire document back and forth, we just send the **operations** (the changes)?"*

An operation is simply: **what changed, where, and what.**

```
Instead of sending: "Hello World"
Send: "Insert ' World' at position 5"
```

This is called an **Operation**. And the whole system is built around passing these operations around.

---

## Let's Define Operations First

There are only **3 fundamental operations** in a text editor:

```
1. INSERT  → Insert character(s) at position X
2. DELETE  → Delete character(s) at position X  
3. RETAIN  → Keep characters at position X (no change)
```

**Example:**

Document = `"Hello"`

User A types a space and "World" after "Hello":
```
Operation A = INSERT(" World", at position 5)
```

Document becomes = `"Hello World"`

Simple enough. Now let's break it. 💥

---

## The Conflict Problem — In Exact Detail

Both users start with the **same document**: `"Hello"`

```
User A sees: "Hello"   (positions: H=0, e=1, l=2, l=3, o=4)
User B sees: "Hello"   (positions: H=0, e=1, l=2, l=3, o=4)
```

**At the same moment:**
- User A deletes `"H"` at position 0 → Op_A = `DELETE(position 0)`
- User B inserts `"!"` at position 5 → Op_B = `INSERT("!", position 5)`

**Each user applies their own operation locally (for speed):**
```
A's screen shows: "ello"      ← A deleted H
B's screen shows: "Hello!"    ← B inserted !
```

Now they **send their ops to each other** via the server.

**The Naive approach — just apply the op as-is:**

A receives Op_B = `INSERT("!", at position 5)`
```
A's current doc = "ello"  (only 4 chars!)
Position 5 doesn't even exist!  💥 CRASH or wrong result
```

B receives Op_A = `DELETE(position 0)`
```
B's current doc = "Hello!"
DELETE position 0 = deletes "H" ✓ → "ello!"
```

**But A's doc = "ello" and B's doc = "ello!" — they're OUT OF SYNC!**

This is the core problem. **Operations assume a document state that no longer exists.**

---

## The Insight: Transform the Operation Before Applying It ✨

The engineers realized: *"Before A applies B's operation, we need to **adjust** it to account for what A already did."*

This adjustment is called **Transformation**.

```
transform(Op_B, Op_A) → Op_B'   ← a new, adjusted operation
```

Let's work through it:

```
Original doc:  "Hello"
Op_A = DELETE(position 0)    ← A deleted "H"
Op_B = INSERT("!", position 5) ← B inserted "!" at end
```

A already deleted position 0. So every position **after 0** shifts left by 1.

Position 5 in the original → becomes **position 4** in A's new doc.

```
transform(Op_B, against Op_A) = INSERT("!", position 4)  ← adjusted!
```

Now A applies the transformed op:
```
A's doc = "ello"
Apply INSERT("!", position 4) → "ello!"  ✓
```

And B applies A's op (no transformation needed here — deleting position 0 doesn't affect position 5):
```
B's doc = "Hello!"
Apply DELETE(position 0) → "ello!"  ✓
```

**Both users now have `"ello!"` — they're in sync!** 🎉

---

## The Transformation Rules (The Brain of OT)

You need rules for every combination of operations:

### Rule 1: INSERT vs INSERT
```
Doc = "AB"
Op_A = INSERT("X", position 1)  → "AXB"
Op_B = INSERT("Y", position 1)  → "AYB"

When B receives Op_A:
  Op_A inserts at position 1, which is ≤ B's position (1)
  So shift B's position right by 1
  transform(Op_B, Op_A) = INSERT("Y", position 2)

Final for both: "AXYB"  ✓
```

### Rule 2: DELETE vs INSERT
```
Doc = "ABC"
Op_A = DELETE(position 1)   → "AC"  (deleted B)
Op_B = INSERT("X", position 2)  → "ABXC"

When A receives Op_B:
  Op_B inserts after position 1 (the deleted position)
  A's doc is already "AC", so position 2 in original = position 1 in A's doc
  transform(Op_B, Op_A) = INSERT("X", position 1)

A applies: "AC" → "AXC"
B applies Op_A on "ABXC" = deletes position 1 (B) = "AXC"  ✓
```

### Rule 3: DELETE vs DELETE (Tricky!)
```
Doc = "ABC"
Op_A = DELETE(position 1)  → deleted "B"
Op_B = DELETE(position 1)  → also wants to delete "B"

When A receives Op_B:
  Op_B wants to delete position 1, but A already deleted it!
  transform(Op_B, Op_A) = NO-OP (do nothing, it's already gone)

Both end up with "AC"  ✓
```

---

## How OT Works End-to-End with a Server

Now let's zoom out and see the full picture:

```
                    ┌─────────────────┐
                    │     SERVER      │
                    │  (source of     │
                    │    truth)       │
                    │                 │
                    │ Maintains:      │
                    │ - Document state│
                    │ - Op history    │
                    │ - Version num   │
                    └────────┬────────┘
                             │
              ┌──────────────┴──────────────┐
              │                             │
     ┌────────▼────────┐          ┌─────────▼───────┐
     │     USER A      │          │     USER B      │
     │                 │          │                 │
     │ Local doc copy  │          │ Local doc copy  │
     │ + pending ops   │          │ + pending ops   │
     └─────────────────┘          └─────────────────┘
```

**The flow:**

```
Step 1: User A types → generates Op_A (version 3)
        A applies Op_A locally immediately (for speed)
        A sends Op_A to server

Step 2: Server receives Op_A
        Server transforms Op_A against any ops it received 
        since version 3
        Server applies transformed op → updates doc to version 4
        Server broadcasts to all other clients

Step 3: User B receives transformed Op_A from server
        B applies it to their local doc

Step 4: Everything stays in sync ✓
```

**The key insight: The server is the referee.** It decides the official order of operations.

---

## The Version Number — Why It's Critical

Every operation carries a **version number** (also called a revision):

```
Op_A = { type: INSERT, char: "X", position: 3, version: 7 }
```

This tells the server: *"When I made this edit, the document was at version 7."*

If the server is now at version 9, it knows it needs to **transform Op_A against versions 8 and 9** before applying it.

```
transform(Op_A, Op_at_v8) → Op_A'
transform(Op_A', Op_at_v9) → Op_A''
Apply Op_A'' → document is now at version 10
```

---

## Why Not Just Lock the Document?

You might wonder: *"Why not just let one person edit at a time?"*

```
User A starts editing → document is LOCKED
User B tries to edit → "Waiting for User A..." 
```

This is called **Pessimistic Locking** and it's terrible for UX. Imagine waiting every time someone else types a single character. Google Docs has 0 locking. OT is what makes that possible.

---

## OT's Limitation — The Diamond Problem 💎

OT works great for simple cases. But here's where it gets ugly:

```
What if THREE people edit simultaneously?

Op_A, Op_B, Op_C all arrive at the server at the same time.

You need:
transform(Op_B, Op_A) → Op_B'
transform(Op_C, Op_A) → Op_C'
transform(Op_C', Op_B') → Op_C''

The number of transforms grows EXPONENTIALLY with users.
```

For 3 users it's manageable. For 100 simultaneous users? It becomes a nightmare to implement correctly.

This led to the birth of a **competing approach** — which we'll cover next.

---

## Summary So Far 🗺️

```
Problem: Concurrent edits destroy each other's work
─────────────────────────────────────────────────────
Naive Fix: Last write wins → ❌ Data loss

OT Solution:
  1. Send operations, not full document
  2. Each op has a version number
  3. Server transforms conflicting ops before applying
  4. Clients apply transforms to stay in sync
  5. Server is the single source of truth
─────────────────────────────────────────────────────
OT is used by: Google Docs, Apache Wave
```

---

## Quick Check Before We Move On ✅

Make sure these are clear:

1. **Why do we send operations instead of the full document?**
2. **What does "transform" actually do to an operation?**
3. **Why does every operation need a version number?**

When you're confident on these, we move to **Chapter 3: CRDTs** — the modern alternative to OT, used by Figma, Notion, and others. It solves the diamond problem elegantly but comes with its own tradeoffs. Ready? 🚀
---

# Chapter 3: CRDTs — "What if we never needed a referee?" 🎯

## The Story: OT's Frustration

It's 2008. Engineers at various companies are implementing OT and they're pulling their hair out.

The problem isn't the *idea* of OT — it's the **implementation**.

> *"For OT to work correctly, every single transformation rule must be perfect. Miss one edge case, and two users silently end up with different documents — and nobody knows."*

In fact, the original Google Wave (Google's first attempt at collaborative editing) had **over 10,000 lines of just transformation logic.** And it still had bugs.

A researcher named **Marc Shapiro** looked at this and thought:

> *"What if we designed the data structure itself in a way that conflicts are mathematically impossible?"*

This gave birth to **CRDTs**.

---

## What is a CRDT?

**CRDT = Conflict-free Replicated Data Type**

The name says it all:
- **Conflict-free** → merging two versions never causes a conflict
- **Replicated** → every user has their own full copy
- **Data Type** → it's a specially designed data structure

The core promise:

```
If User A and User B both start from the same state
and make any edits independently,
then merging their edits will ALWAYS produce
the same result — regardless of order.
```

This property is called **Strong Eventual Consistency (SEC)**.

---

## The Key Insight: Give Every Character a Unique ID 🔑

Remember OT's problem? It used **positions** to identify characters.

```
Op_A = DELETE(position 1)  ← "position 1" changes as other edits happen!
```

Positions are **unstable**. They shift when other characters are inserted or deleted.

CRDT's insight: **What if every character had a globally unique ID that never changes?**

```
Instead of: DELETE(position 1)
Use:        DELETE(characterID: "char_A3x9")
```

Now it doesn't matter what else happened to the document — `"char_A3x9"` always refers to the exact same character. Forever.

---

## Building a CRDT Text Editor Step by Step

### Step 1: Assign a Unique ID to Every Character

When a user types a character, we assign it:
- A **unique ID** (userID + timestamp + counter)
- A **reference to the character it comes after**

```
User A types "H":
  char = { id: "A:1", value: "H", after: START }

User A types "e":
  char = { id: "A:2", value: "e", after: "A:1" }

User A types "l":
  char = { id: "A:3", value: "l", after: "A:2" }
```

The document isn't an array anymore. It's a **linked list** where each node knows what it comes after:

```
START → [A:1, "H"] → [A:2, "e"] → [A:3, "l"] → END
```

---

### Step 2: Deletion — Just Mark It Dead

Here's a clever trick. We **never actually delete** a character.

Instead, we mark it as a **tombstone** 🪦:

```
DELETE "e" (char A:2):
  char = { id: "A:2", value: "e", after: "A:1", deleted: true }
```

The character still exists in the data structure, it's just invisible to the user.

**Why?** Because if we truly deleted it, and someone else was referencing it as an anchor point, their insertion would have nowhere to go.

```
Document: H → e → l → l → o
               ↑
        User B is inserting "X" after "e"
        
If we truly deleted "e" before B's insert arrives...
Where does X go? Nobody knows! 💥
```

With tombstones, "e" still exists, so X inserts after it correctly, then "e" is hidden from display.

---

### Step 3: The Concurrent Edit — No Conflict! 

Let's replay our earlier nightmare scenario, but with CRDTs:

```
Starting document: "Hi"
  START → [A:1, "H"] → [A:2, "i"] → END
```

**User A** inserts "!" after "i":
```
Op_A = { id: "A:3", value: "!", after: "A:2" }
```

**User B** simultaneously inserts "?" after "i":
```
Op_B = { id: "B:1", value: "?", after: "A:2" }
```

Both refer to `after: "A:2"` — same anchor point. Conflict?

**In OT:** 💥 We'd need transformation rules.

**In CRDT:** We just need a **tiebreaker rule**.

> *"When two characters have the same anchor, sort by user ID alphabetically."*

So: `A:3` comes before `B:1` (A < B alphabetically)

**Both users independently arrive at the same result:**
```
START → [A:1,"H"] → [A:2,"i"] → [A:3,"!"] → [B:1,"?"] → END
Display: "Hi!?"
```

No server coordination needed. No transformation. **Pure math.** ✨

---

## The CRDT Superpower: No Server Required for Merging

This is the **fundamental difference** from OT:

```
OT:
  Client A ──→ Server (transforms) ──→ Client B
  Client B ──→ Server (transforms) ──→ Client A
  Server is REQUIRED for correctness

CRDT:
  Client A ──→ Client B (merge directly)
  Client B ──→ Client A (merge directly)
  Server is optional — just for delivery!
```

This means:

```
User A edits offline for 2 hours on a train ✈️
User B edits the same doc offline for 2 hours

They reconnect.
Their changes are merged automatically, correctly,
without any server-side transformation logic.
```

This is why CRDTs are used in:
- **Figma** — design collaboration
- **Notion** — document editing
- **Apple Notes** — syncs across devices
- **Git** (conceptually similar)

---

## Visualizing the Full CRDT Document Structure

Let's say we're building the word "CAT":

```
User A types C, A, T:
┌─────────────────────────────────────────┐
│ id:"A:1" value:"C" after:START          │
│ id:"A:2" value:"A" after:"A:1"          │
│ id:"A:3" value:"T" after:"A:2"          │
└─────────────────────────────────────────┘

Document (linked list):
START → C(A:1) → A(A:2) → T(A:3) → END

Display: "CAT"
```

User B (offline) deletes "A" and inserts "U":
```
┌─────────────────────────────────────────┐
│ id:"A:2" deleted: true   ← tombstone   │
│ id:"B:1" value:"U" after:"A:1"         │
└─────────────────────────────────────────┘
```

User A (offline) inserts "H" at the start:
```
┌─────────────────────────────────────────┐
│ id:"A:4" value:"H" after:START          │
└─────────────────────────────────────────┘
```

**They sync. Merge is automatic:**

```
START → H(A:4) → C(A:1) → ~~A(A:2)~~ → U(B:1) → T(A:3) → END
                            (tombstone, hidden)

Display: "HCUT"
```

Both users see `"HCUT"` — **no server transformation, no conflicts.** ✅

---

## CRDT vs OT — The Real Tradeoff

```
┌─────────────────────┬──────────────────────┬──────────────────────┐
│                     │        OT            │       CRDT           │
├─────────────────────┼──────────────────────┼──────────────────────┤
│ Conflict resolution │ Server transforms ops│ Math (unique IDs)    │
│ Server dependency   │ Required             │ Optional             │
│ Offline support     │ Hard                 │ Natural              │
│ Memory usage        │ Low                  │ High (tombstones!)   │
│ Implementation      │ Very complex         │ Moderate complexity  │
│ Used by             │ Google Docs          │ Figma, Notion        │
└─────────────────────┴──────────────────────┴──────────────────────┘
```

---

## The CRDT Problem: Memory Bloat 💾

Nothing is free. CRDTs have a serious issue:

> **Tombstones never die.**

Every deleted character stays in memory forever (just marked deleted).

```
User types 10,000 characters
User deletes 9,000 of them

CRDT still stores all 10,000 character objects in memory.
Document looks like 1,000 chars but internally holds 10,000.
```

Over time, documents become **massive** in memory.

**Solutions:**
1. **Garbage Collection** — periodically, when all users are online, actually purge tombstones
2. **Compaction** — create a clean snapshot and rebuild the linked list
3. **Cap the history** — only keep tombstones for N days

This is an active area of research. No perfect solution yet.

---

## So Which Does Google Docs Use?

**Google Docs uses OT**, not CRDT. Here's why:

1. Google built Docs in 2006 — CRDTs weren't mature yet
2. OT works great with a central server (which Google has)
3. Google Docs requires a server connection anyway (no true offline mode)
4. Rewriting a system used by billions is extremely risky

Modern systems (Notion, Figma) built after ~2015 tend to use CRDTs because the research matured and offline-first design became important.

---

## Story So Far 🗺️

```
Chapter 1: Problem → Concurrent edits = data loss
Chapter 2: OT → Transform operations, server is referee
Chapter 3: CRDT → Unique IDs, math prevents conflicts, no referee needed
```

---

## Quick Check ✅

Before we move on, make sure these click:

1. **Why do CRDTs use unique IDs instead of positions?**
2. **Why do we use tombstones instead of truly deleting characters?**
3. **What's the fundamental difference in how OT and CRDT handle conflicts?**
4. **Why does CRDT naturally support offline editing but OT doesn't?**

Once these are clear, we move to **Chapter 4: The Network Layer** — *"How do edits actually travel from your keyboard to your collaborator's screen in milliseconds?"*

This is where we talk about **WebSockets vs HTTP, the real-time connection architecture, and what happens when your internet drops mid-edit.** 🚀

---

# Chapter 4: The Network Layer — "How does your keystroke reach the world?" 🌐

## The Story: The Naive First Attempt

The year is 2006. The team has figured out OT. They're excited. Now they need to actually *send* these operations between users.

A junior engineer suggests:

> *"Easy! Every time someone types a character, we make an HTTP request to the server with the operation. The server saves it and other clients poll every second to check for updates."*

This is called **Polling**. Let's see why it's terrible.

---

## Approach 1: Polling 🔄 (The Bad Way)

```
Every client asks the server every 1 second:
"Hey, any new changes?"

Server: "Nope"
Client: "Hey, any new changes?"
Server: "Nope"
Client: "Hey, any new changes?"
Server: "Yes! Here's op #47"
Client: "Hey, any new changes?"
Server: "Nope"
```

**The math is brutal:**

```
1 document × 10 users × 1 request/second
= 10 requests/second per document

Google Docs has ~1 billion documents
Even if only 0.001% are active simultaneously = 1,000,000 active docs

1,000,000 × 10 requests/second
= 10,000,000 HTTP requests/second  💀
```

And most of those requests return **"Nope"** — pure waste.

**Problems:**
- ❌ Massive server load
- ❌ 1 second delay feels awful — not "real-time"
- ❌ Each HTTP request has ~800 bytes of headers — huge overhead
- ❌ Server is constantly hammered for no reason

---

## Approach 2: Long Polling 🔄 (Slightly Better)

Someone smarter says: *"What if instead of the client asking every second, it asks once — but the server **holds the request open** until it has something to say?"*

```
Client: "Hey, any new changes?" 
Server: *holds the connection open silently*
Server: *waits...*
Server: *waits...*
User B types something!
Server: "YES! Here's op #47"
Client receives it, immediately asks again: "Any new changes?"
Server: *holds open again*
```

**Better!** But still has issues:

```
Problems:
❌ Each "held" request still consumes a server thread
❌ After every response, a new HTTP connection must be established
   (TCP handshake costs ~100-300ms each time)
❌ Still fundamentally request-response — server can't push freely
❌ HTTP headers sent every single time (wasteful)
```

---

## Approach 3: WebSockets 🔌 (The Real Solution)

A new engineer says: *"What if we establish ONE persistent connection and both sides can talk freely at any time?"*

This is **WebSockets** — and it changed real-time applications forever.

### How WebSockets Work

```
Step 1: Client sends an HTTP "Upgrade" request:
  GET /collaborate HTTP/1.1
  Upgrade: websocket
  Connection: Upgrade

Step 2: Server agrees:
  HTTP/1.1 101 Switching Protocols
  Upgrade: websocket

Step 3: HTTP connection is UPGRADED to a WebSocket.
  Now it's a raw TCP connection — both sides can 
  send messages to each other ANY TIME.
```

Visualized:

```
HTTP (old way):                    WebSocket (new way):
                                   
Client ──req──→ Server             Client ←───────→ Server
Client ←──res── Server                    persistent
Client ──req──→ Server                    connection
Client ←──res── Server             
Client ──req──→ Server             Any time either side
Client ←──res── Server             wants to talk — they just talk.
(open, close, open, close...)      (one connection, lives forever)
```

### The Numbers Comparison

```
HTTP Request overhead:
  Headers alone: ~800 bytes per request
  If you type 10 chars/second = 8,000 bytes/sec of just headers!

WebSocket message overhead:
  Frame header: 2-14 bytes
  Actual operation payload: ~50 bytes
  Total: ~64 bytes per keystroke

WebSocket is 12x more efficient. 🚀
```

---

## The Full Real-Time Architecture

Now let's zoom out and design the actual system:

```
┌──────────────────────────────────────────────────────────┐
│                    BROWSER (User A)                       │
│                                                           │
│  ┌─────────────┐    ┌──────────────┐   ┌─────────────┐  │
│  │  Text       │    │  OT/CRDT     │   │  WebSocket  │  │
│  │  Editor UI  │───▶│  Engine      │──▶│  Client     │  │
│  │  (cursor,   │    │  (transforms │   │  (sends &   │  │
│  │   typing)   │◀───│   ops)       │◀──│  receives)  │  │
│  └─────────────┘    └──────────────┘   └──────┬──────┘  │
└──────────────────────────────────────────────-─┼─────────┘
                                                  │ WebSocket
                                                  │ (persistent)
                              ┌───────────────────┼──────────────┐
                              │   COLLAB SERVER   │              │
                              │                   ▼              │
                              │  ┌─────────────────────────┐     │
                              │  │  Connection Manager     │     │
                              │  │  (tracks all active     │     │
                              │  │   WebSocket sessions)   │     │
                              │  └────────────┬────────────┘     │
                              │               │                   │
                              │  ┌────────────▼────────────┐     │
                              │  │  OT Engine              │     │
                              │  │  (transforms ops,       │     │
                              │  │   assigns versions)     │     │
                              │  └────────────┬────────────┘     │
                              │               │                   │
                              │  ┌────────────▼────────────┐     │
                              │  │  Op Queue               │     │
                              │  │  (orders concurrent ops)│     │
                              │  └────────────┬────────────┘     │
                              └───────────────┼──────────────────┘
                                              │
                              ┌───────────────┼──────────────────┐
                              │   STORAGE     │                  │
                              │  ┌────────────▼────────────┐     │
                              │  │  Op Log (every op ever) │     │
                              │  │  + Document Snapshots   │     │
                              └──└─────────────────────────┘─────┘
```

---

## What Happens When You Type a Character — Exact Flow

Let's trace a single keystroke end to end:

```
You type "G" in a Google Doc:

1. [0ms]   Browser captures keypress event
           OT engine creates operation:
           Op = { type: INSERT, char: "G", pos: 42, version: 156 }

2. [0ms]   Op applied to LOCAL doc immediately
           (You see "G" appear instantly — no waiting for server!)

3. [1ms]   Op serialized to JSON, sent over WebSocket
           Payload: {"t":"ins","c":"G","p":42,"v":156,"user":"A"}
           Size: ~50 bytes

4. [20ms]  Server receives op (20ms = network latency)
           Server checks: is doc still at version 156?
           
           Case A: YES → Apply op, doc becomes version 157
                         Broadcast to all other users
           
           Case B: NO  → Transform op against newer ops
                         Apply transformed op
                         Broadcast transformed op

5. [21ms]  Server broadcasts to User B, C, D...
           Each client receives op and applies to their local doc

6. [41ms]  User B's screen updates (20ms return trip)
           Total: ~41ms from your keystroke to their screen ✨
```

**41 milliseconds.** That's why Google Docs feels instantaneous.

---

## The Acknowledgment Protocol — Handling the "In-Flight" Problem

Here's a subtle but important issue:

```
User A types rapidly: G, O, O, G, L, E

Op1 sent to server (version 5)
[Op1 is "in-flight" — not yet acknowledged]
Op2 sent to server (version 5?? or 6??)
Op3 sent to server
...
```

**What version do you stamp on Op2 if Op1 hasn't been confirmed yet?**

Google Docs uses a **buffer + acknowledgment** pattern:

```
┌─────────────────────────────────────────────┐
│           CLIENT STATE MACHINE              │
│                                             │
│  COMMITTED  │  IN-FLIGHT  │  BUFFER        │
│  (server    │  (sent, not │  (waiting to   │
│   knows)    │  acked yet) │   be sent)     │
│             │             │                │
│  "Helo"     │   Op: "l"   │  Op: "l"       │
│             │  v:4, sent  │  Op: "o"       │
│             │  to server  │  (waiting for  │
│             │             │   Op1 ack)     │
└─────────────────────────────────────────────┘

Rule: Only ONE op can be in-flight at a time.
Buffer all others until you get an acknowledgment.

Server acks Op1 → Op2 leaves buffer, becomes in-flight
Server acks Op2 → Op3 leaves buffer...
```

This prevents version stampede and makes the OT math clean.

---

## What Happens When Internet Drops? 📡

This is the **offline/reconnection problem**:

```
User A is editing. Internet drops at 2:00 PM.
User A keeps typing offline. (5 ops buffered locally)
Internet returns at 2:05 PM.

What happens?
```

**The reconnection flow:**

```
Step 1: Client detects WebSocket disconnect
        Starts showing "Trying to reconnect..." indicator
        Keeps buffering ops locally

Step 2: Connection restored
        Client sends: "I was at version 203, here are my 5 buffered ops"

Step 3: Server checks what happened between v203 and now
        Server has 12 new ops from other users since v203

Step 4: Server transforms client's 5 ops against those 12 ops
        Sends back: 12 ops + acknowledgment for client's 5

Step 5: Client applies the 12 ops (transformed)
        Document is now in sync ✓
```

This is why Google Docs shows **"Reconnecting..."** and then seamlessly catches up — it's this exact flow.

---

## Presence — "See Where Others Are Typing" 👥

You've seen colored cursors in Google Docs. How do those work?

These are called **Presence** updates and they're handled separately from operations:

```
Every 50ms (while typing):
  Client sends: { user: "Priya", cursor: position 47, selection: [47,52] }

These are NOT operations — they don't go through OT.
They're just broadcast to all users as-is.

Why not through OT?
  → Cursor positions are ephemeral (we don't care about history)
  → They update so frequently that perfect accuracy isn't critical
  → If a cursor position is slightly off by 1 character, nobody cares
```

**Presence data is treated as "best effort"** — fast and loose, not perfectly consistent.

---

## The Heartbeat — Keeping Connections Alive 💓

WebSocket connections can silently die (NAT timeouts, proxy issues, etc.)

```
Solution: Heartbeat (also called ping/pong)

Every 30 seconds:
  Client → Server: PING
  Server → Client: PONG

If server misses 3 pings:
  Client assumes connection is dead
  Reconnects automatically

If client misses 3 pongs:
  Server closes connection, frees resources
  Removes user from presence list
```

---

## Summary of Chapter 4 🗺️

```
Evolution of Real-time Communication:
─────────────────────────────────────────────────────────
Polling       → Client begs server every second   ❌
Long Polling  → Client waits, server holds         ⚠️
WebSocket     → Persistent bidirectional pipe      ✅
─────────────────────────────────────────────────────────

Key Concepts:
• WebSocket = one TCP connection, both sides talk freely
• Operations are tiny (~50 bytes), sent per keystroke
• Only ONE op in-flight at a time (buffer the rest)
• Offline edits buffer locally, transform on reconnect
• Cursor presence = separate fast/lossy channel
• Heartbeat = keep the connection alive
─────────────────────────────────────────────────────────
```

---

## Quick Check ✅

1. **Why is polling terrible for real-time collaboration?**
2. **What makes WebSocket fundamentally different from HTTP?**
3. **Why do we only allow ONE in-flight operation at a time?**
4. **Why are cursor positions handled differently from text operations?**
5. **What happens to your edits when your internet drops and comes back?**

Once these are solid, we move to **Chapter 5: Storage Architecture** — *"Where does the document actually live? How do we store billions of documents, never lose a single character, and still retrieve any doc in milliseconds?"*

This is where **sharding, replication, and the Op Log pattern** come in. 🚀

---
# Chapter 5: Storage Architecture — "Where does a billion documents live?" 🗄️

## The Story: The First Storage Mistake

The first version of the storage system was simple:

```sql
TABLE documents (
  doc_id    VARCHAR PRIMARY KEY,
  content   TEXT,
  updatedAt TIMESTAMP
)
```

Every time someone typed a character, you'd update the `content` column with the full new document text.

Seemed fine. Then three problems appeared:

```
Problem 1: User asks "What did this doc look like yesterday?"
           → Impossible. You only stored the LATEST version.

Problem 2: Two users save simultaneously.
           → One overwrites the other. Data loss.

Problem 3: Document is 500KB of text.
           → Every single keystroke rewrites 500KB to disk.
           → At 10 users typing 5 chars/sec = 25MB/sec per doc!
```

The team realized: **storing the document as a blob is fundamentally wrong.**

The answer came from an unexpected place — **accounting**.

---

## The Ledger Insight 💡

In accounting, you never erase old entries. You only **append new ones**.

```
Bank account — Wrong way (blob):
  Balance: $1000  → $800 → $1200 → $950
  (history is gone forever)

Bank account — Right way (ledger):
  +$1000  (deposit)
  -$200   (withdrawal)
  +$400   (deposit)
  -$250   (withdrawal)
  Current balance = sum of all entries = $950
  AND you have full history!
```

Applied to documents:

```
Document — Wrong way (blob):
  Content: "H" → "He" → "Hel" → "Hell" → "Hello"

Document — Right way (op log):
  INSERT("H", pos 0)
  INSERT("e", pos 1)
  INSERT("l", pos 2)
  INSERT("l", pos 3)
  INSERT("o", pos 4)
  Current document = replay all ops from beginning
  AND you have full version history!
```

This is called the **Operation Log** (Op Log) pattern. It is the foundation of Google Docs storage.

---

## The Two-Layer Storage System

Google Docs uses **two complementary storage structures**:

```
┌─────────────────────────────────────────────────────────┐
│                    STORAGE SYSTEM                        │
│                                                          │
│   LAYER 1: Op Log (the truth)                           │
│   ┌──────────────────────────────────────────────────┐  │
│   │ op_id │ doc_id │ user │ operation  │ version │ ts │  │
│   │  001  │ doc_A  │  A   │ INS("H",0) │    1    │ .. │  │
│   │  002  │ doc_A  │  A   │ INS("e",1) │    2    │ .. │  │
│   │  003  │ doc_A  │  B   │ INS("!",5) │    3    │ .. │  │
│   │  004  │ doc_A  │  A   │ DEL(2)     │    4    │ .. │  │
│   │  ...  │  ...   │ ...  │    ...     │   ...   │ .. │  │
│   └──────────────────────────────────────────────────┘  │
│   → Append-only. Never update. Never delete.             │
│   → Source of truth for version history                  │
│                                                          │
│   LAYER 2: Snapshots (for speed)                        │
│   ┌──────────────────────────────────────────────────┐  │
│   │ doc_id │ content           │ version │ timestamp  │  │
│   │ doc_A  │ "Hello World"     │  1000   │ 2hr ago    │  │
│   │ doc_A  │ "Hello World!!"   │  1500   │ 1hr ago    │  │
│   └──────────────────────────────────────────────────┘  │
│   → Periodic frozen copy of the document                 │
│   → Used to avoid replaying ALL ops from the beginning   │
└─────────────────────────────────────────────────────────┘
```

---

## Why Snapshots? The Replay Problem

Imagine a document that's been edited for 2 years:

```
Op Log has 2,000,000 operations.

User opens the document.
Without snapshots:
  Server must replay all 2,000,000 ops to reconstruct document.
  At 1M ops/sec → 2 seconds just to open a doc! ❌

With snapshots:
  Find nearest snapshot (say, version 1,998,000)
  Replay only the last 2,000 ops
  Document reconstructed in milliseconds ✅
```

**Snapshot strategy:**

```
Take a snapshot every:
  - 1,000 operations, OR
  - 1 hour of inactivity, OR
  - When a user explicitly saves

Opening a document:
  1. Find latest snapshot (e.g., version 1,998,000)
  2. Fetch all ops AFTER that version from op log
  3. Replay those ops on top of snapshot
  4. Serve to user
```

---

## Now the Real Challenge: Scale 📈

So far so good for ONE document. But Google Docs has:

```
~1 billion documents
~1 billion users
~50 million active documents per day
Peak: ~1 million concurrent editors
```

A single database server can handle maybe ~10,000 writes/second and store ~10TB.

**One server is nowhere near enough.**

We need to split the data. This is **Sharding**.

---

## Sharding — Splitting Data Across Many Machines

**The core idea:** Instead of one giant database, use many smaller ones. Each holds a *slice* of the data.

```
Without sharding:            With sharding:
                             
┌──────────────┐             ┌──────────┐ ┌──────────┐ ┌──────────┐
│  ONE giant   │             │ Shard 1  │ │ Shard 2  │ │ Shard 3  │
│   database   │             │ docs A-F │ │ docs G-M │ │ docs N-Z │
│ ALL docs     │             │          │ │          │ │          │
│ 😰           │             │ 😊       │ │ 😊       │ │ 😊       │
└──────────────┘             └──────────┘ └──────────┘ └──────────┘
```

### How do we decide which shard a document goes to?

**Option 1: Range-Based Sharding**

```
Shard 1: doc_id 1        → 1,000,000
Shard 2: doc_id 1,000,001 → 2,000,000
Shard 3: doc_id 2,000,001 → 3,000,000
```

Simple. But **dangerous**:

```
Problem: Hot spots! 🔥

New documents get IDs like 9,999,991, 9,999,992, 9,999,993...
They ALL land on the LAST shard!

Shard 1: 😴 (old docs, nobody editing)
Shard 2: 😴 (old docs, nobody editing)
Shard N: 🔥🔥🔥 (ALL new activity!)
```

**Option 2: Hash-Based Sharding** ✅

```
shard_number = hash(doc_id) % number_of_shards

doc_id "abc123" → hash = 8291 → 8291 % 10 = Shard 1
doc_id "xyz789" → hash = 4471 → 4471 % 10 = Shard 1  
doc_id "doc456" → hash = 7823 → 7823 % 10 = Shard 3
```

**Documents are distributed uniformly. No hot spots.** ✅

```
Shard 1: 10% of docs  😊
Shard 2: 10% of docs  😊
Shard 3: 10% of docs  😊
...
Shard 10: 10% of docs 😊
```

---

## The Sharding Problem: Resharding 😰

Here's where sharding gets painful. You start with 10 shards:

```
hash(doc_id) % 10
```

A year later, traffic triples. You add 5 more shards:

```
hash(doc_id) % 15  ← changed the formula!
```

Now:

```
doc "abc123" was on Shard 1 (8291 % 10 = 1)
doc "abc123" is now on Shard 11 (8291 % 15 = 11)

ALL documents are on the wrong shard! 😱
You'd have to move ALL data around.
```

**Solution: Consistent Hashing** 🔄

```
Imagine a ring of 1,000,000 virtual positions (0 to 999,999):

             0
        900k   100k
      800k       200k
        700k   300k
             500k

Each shard owns a range of the ring:
  Shard 1: 0      → 333,333
  Shard 2: 333,334 → 666,666  
  Shard 3: 666,667 → 999,999

doc "abc123" → hash = 450,000 → falls in Shard 2's range
```

**Adding a new Shard 4:**

```
Shard 4 takes over part of one range:
  Shard 1: 0       → 333,333  (unchanged)
  Shard 2: 333,334 → 500,000  (gave away some)
  Shard 4: 500,001 → 666,666  (NEW — took from Shard 2)
  Shard 3: 666,667 → 999,999  (unchanged)

Only Shard 2's excess data moves to Shard 4.
Everything else stays put! ✅
```

---

## Replication — "What if a Shard Dies?" 💀

Sharding distributes load. But what if a machine **crashes**?

```
Shard 3 holds 10% of all documents.
Shard 3's hard drive dies at 3 AM.
10% of all Google Docs are GONE. 😱
```

**Solution: Replication** — every shard has multiple copies.

```
┌─────────────────────────────────────────────┐
│              SHARD 3 REPLICA SET            │
│                                             │
│  ┌──────────────┐      ┌──────────────┐     │
│  │   PRIMARY    │─────▶│  SECONDARY 1 │     │
│  │  (handles    │      │  (exact copy)│     │
│  │   all reads  │      └──────────────┘     │
│  │   & writes)  │                           │
│  │              │─────▶┌──────────────┐     │
│  └──────────────┘      │  SECONDARY 2 │     │
│                        │  (exact copy)│     │
│                        └──────────────┘     │
└─────────────────────────────────────────────┘
```

**Write flow with replication:**

```
User types "G" →
  1. Write goes to PRIMARY
  2. PRIMARY writes to its log
  3. PRIMARY replicates to SECONDARY 1 and 2
  4. Once majority confirm (2 out of 3) → acknowledge to user
```

This is called the **Raft consensus protocol** (used by Google internally, and by CockroachDB, etcd, etc.)

```
Why majority (not all)?

If we wait for ALL 3:
  One slow secondary = everything slows down ❌

If we wait for MAJORITY (2 of 3):
  One secondary can be slow or dead = still works ✅
  And we still have 2 copies = safe ✅
```

**What happens when PRIMARY dies?**

```
SECONDARY 1 and 2 detect: "Primary hasn't sent heartbeat in 5 seconds"

They hold an ELECTION:
  SECONDARY 1: "I should be primary, vote for me! My log is up to date."
  SECONDARY 2: "I also vote for Secondary 1 — its log matches mine."
  
  Secondary 1 wins election, becomes new PRIMARY.
  Total downtime: ~5-10 seconds ✅ (not hours)

When old PRIMARY comes back online:
  It sees there's a new primary
  It becomes a secondary and syncs up
```

---

## Putting It All Together: The Full Storage Picture

```
WRITE PATH (user types a character):

Browser
  │
  ▼
Collab Server (OT transform)
  │
  ▼
┌──────────────────┐
│   Router         │  hash(doc_id) % N → which shard?
└──────┬───────────┘
       │
       ▼
┌──────────────────┐
│  Shard Primary   │  Writes op to op log
└──────┬───────────┘
       │ replicates
       ├──────────────▶ Secondary 1
       └──────────────▶ Secondary 2
       
READ PATH (user opens a document):

Browser requests doc_id "abc123"
  │
  ▼
Router → hash("abc123") → Shard 2
  │
  ▼
Shard 2 Primary (or any Secondary for reads):
  1. Find latest snapshot for doc "abc123"
  2. Fetch all ops after that snapshot version
  3. Reconstruct document
  4. Return to browser
```

---

## What Database Does Google Actually Use?

Google built a custom database called **Spanner** for this. Key properties:

```
┌─────────────────────────────────────────────────────┐
│                    SPANNER                           │
│                                                      │
│  ✅ Globally distributed (data centers worldwide)   │
│  ✅ Strong consistency (no stale reads)              │
│  ✅ Automatic sharding + resharding                  │
│  ✅ Synchronous replication across data centers      │
│  ✅ External consistency (like a single giant DB)    │
│                                                      │
│  Uses atomic clocks + GPS for global time sync      │
│  (called TrueTime API)                               │
└─────────────────────────────────────────────────────┘
```

For an interview, you don't need to say "Spanner" — you can say:
> *"A globally distributed SQL database with automatic sharding and synchronous replication — similar to Google Spanner or CockroachDB."*

---

## Garbage Collection — Cleaning Up the Op Log

Op logs grow forever. A 5-year-old document might have 50 million ops.

```
Garbage Collection Strategy:

1. Keep full op log for last 30 days (for fine-grained history)
2. Beyond 30 days: keep only daily snapshots
3. Beyond 1 year: keep only monthly snapshots
4. Tombstoned CRDT chars: purge when ALL active users have 
   synced past that version

This is why Google Docs only shows "version history" 
for recent edits, not every single keystroke from 3 years ago.
```

---

## Summary of Chapter 5 🗺️

```
Storage Architecture:
─────────────────────────────────────────────────────────────
Layer 1: Op Log     → append-only, never delete, source of truth
Layer 2: Snapshots  → periodic full copies, fast reconstruction
─────────────────────────────────────────────────────────────
Sharding:
  Range-based     → simple but creates hot spots ❌
  Hash-based      → uniform distribution ✅
  Consistent Hash → handles resharding gracefully ✅
─────────────────────────────────────────────────────────────
Replication:
  1 Primary + 2 Secondaries per shard
  Write to majority (2/3) before acknowledging
  Automatic leader election on primary failure
  Failover in ~5-10 seconds
─────────────────────────────────────────────────────────────
```

---

## Quick Check ✅

1. **Why do we store operations instead of the full document content?**
2. **Why do we need snapshots if we have the full op log?**
3. **Why is range-based sharding dangerous? What problem does hash-based solve?**
4. **Why do we wait for majority (not all) replicas before acknowledging a write?**
5. **What happens in the 5-10 seconds after a primary shard dies?**

Once these are clear, we move to **Chapter 6: The Server Architecture** — *"How do we handle millions of WebSocket connections simultaneously? How do we route User A's keystroke to the right server that's holding User B's connection?"*

This is where **load balancing, the session affinity problem, and pub/sub messaging** come in. 🚀

---

# Chapter 6: Server Architecture — "How do we handle millions of live connections?" ⚡

## The Story: The Single Server Illusion

The team has OT working. WebSockets working. Storage working. They deploy to a single server.

Day 1: 100 users. Perfect.
Day 7: 10,000 users. Server is sweating.
Day 30: 1,000,000 users. Server is dead. 💀

The naive fix: *"Just get a bigger server!"*

This is called **Vertical Scaling** — making one machine stronger.

```
Start:    4 CPU,  16GB RAM  → handles ~10K connections
Upgrade:  16 CPU, 64GB RAM  → handles ~40K connections
Upgrade:  64 CPU, 256GB RAM → handles ~150K connections
Maximum:  ??? CPU, ???GB RAM → physical limits of hardware
```

You hit a wall. There's no single machine powerful enough.

The real answer is **Horizontal Scaling** — more machines, not bigger machines.

```
Instead of:          Use:
┌──────────────┐     ┌────────┐ ┌────────┐ ┌────────┐
│  1 MASSIVE   │     │Server 1│ │Server 2│ │Server 3│
│    server    │     │ 10K    │ │ 10K    │ │ 10K    │
│   150K conn  │     │ conns  │ │ conns  │ │ conns  │
└──────────────┘     └────────┘ └────────┘ └────────┘
                     Add more servers as needed → infinite scale
```

But horizontal scaling introduces a **brutal new problem**.

---

## The Core Problem: Two Users, Different Servers 😱

```
Google Docs doc "abc123" has 3 users editing:

User A → connected to Server 1
User B → connected to Server 2  
User C → connected to Server 1
```

User A types "G":

```
Op from A arrives at Server 1.
Server 1 knows about User C (also on Server 1).
Server 1 sends update to User C ✅

But User B is on Server 2!
Server 1 has NO connection to User B.
User B never receives the update. ❌
```

The servers are **islands**. They don't know about each other's connections.

This is the **Session Affinity Problem** — and solving it is the central challenge of real-time server architecture.

---

## Approach 1: Sticky Sessions 🍯

*"What if all users of the same document always connect to the same server?"*

```
Load Balancer rule:
  All requests for doc "abc123" → always go to Server 3
  All requests for doc "xyz789" → always go to Server 7
  
Now Server 3 has ALL of abc123's users.
A types → Server 3 has B and C's connections → broadcasts to both ✅
```

Seems to work! But:

```
Problem 1: SERVER 3 DIES 💀
  All users of every doc on Server 3 lose connection.
  Load balancer must reroute them.
  But now they land on different servers again! Back to square one.

Problem 2: VIRAL DOCUMENT 🔥
  A company-wide doc gets shared. 50,000 people open it.
  They ALL go to Server 3 (sticky rule).
  Server 3: 🔥🔥🔥 Dead.
  All other servers: 😴 Idle.
  
Problem 3: Can't scale a single hot document.
  One document can never use more than one server. Hard ceiling.
```

Sticky sessions are a **partial fix** that breaks under pressure.

---

## Approach 2: Pub/Sub Messaging — The Real Solution 📨

The insight: *"Instead of servers talking directly to each other, have them talk through a shared message bus."*

This is the **Publish/Subscribe pattern**:

```
Publisher  → publishes message to a TOPIC
Subscriber → subscribes to a TOPIC, receives all messages on it

The publisher and subscriber never talk directly.
The message bus is the middleman.
```

Applied to Google Docs:

```
Each document gets its own TOPIC: "doc:abc123"

Server 1 (has Users A, C):
  → SUBSCRIBES to topic "doc:abc123"

Server 2 (has User B):
  → SUBSCRIBES to topic "doc:abc123"

User A types "G" → arrives at Server 1:
  Server 1 PUBLISHES op to topic "doc:abc123"
  
Message bus delivers to ALL subscribers:
  → Server 1 receives it, sends to User C ✅
  → Server 2 receives it, sends to User B ✅
  
User B sees the update! Problem solved! 🎉
```

Visualized:

```
                    ┌─────────────────────┐
                    │    MESSAGE BUS      │
                    │   (Redis Pub/Sub    │
                    │    or Kafka)        │
                    │                     │
                    │  Topic: "doc:abc"   │
                    │  ┌───────────────┐  │
                    │  │  Op from A    │  │
                    │  │  Op from B    │  │
                    │  │  Op from C    │  │
                    │  └───────────────┘  │
                    └──────────┬──────────┘
                               │
              ┌────────────────┼────────────────┐
              │ subscribe      │ subscribe       │ subscribe
              ▼                ▼                 ▼
        ┌──────────┐    ┌──────────┐      ┌──────────┐
        │ Server 1 │    │ Server 2 │      │ Server 3 │
        │          │    │          │      │          │
        │ User A ✓ │    │ User B ✓ │      │ User D ✓ │
        │ User C ✓ │    │          │      │ User E ✓ │
        └──────────┘    └──────────┘      └──────────┘
```

---

## The Full Server Architecture — Layer by Layer

Now let's build the complete picture:

```
                         INTERNET
                            │
                            ▼
┌───────────────────────────────────────────────────────┐
│                    LAYER 1: DNS                        │
│                                                        │
│  docs.google.com → resolves to Load Balancer IP       │
│  (GeoDNS: users in India → Indian data center,        │
│   users in US → US data center)                       │
└───────────────────────────┬───────────────────────────┘
                            │
                            ▼
┌───────────────────────────────────────────────────────┐
│                 LAYER 2: LOAD BALANCER                 │
│                                                        │
│  - Terminates SSL/TLS                                  │
│  - Distributes connections across collab servers       │
│  - Health checks (removes dead servers)                │
│  - Algorithm: Least-connections                        │
│    (send new user to server with fewest active conns)  │
└───────────────────────────┬───────────────────────────┘
                            │
          ┌─────────────────┼──────────────────┐
          ▼                 ▼                  ▼
┌──────────────┐   ┌──────────────┐   ┌──────────────┐
│  COLLAB      │   │  COLLAB      │   │  COLLAB      │
│  SERVER 1    │   │  SERVER 2    │   │  SERVER 3    │
│              │   │              │   │              │
│ • WS handler │   │ • WS handler │   │ • WS handler │
│ • OT engine  │   │ • OT engine  │   │ • OT engine  │
│ • Pub/Sub    │   │ • Pub/Sub    │   │ • Pub/Sub    │
│   client     │   │   client     │   │   client     │
└──────┬───────┘   └──────┬───────┘   └──────┬───────┘
       │                  │                  │
       └──────────────────┼──────────────────┘
                          │ publish/subscribe
                          ▼
┌───────────────────────────────────────────────────────┐
│              LAYER 3: MESSAGE BUS (Redis)              │
│                                                        │
│  Topic per document: "doc:{doc_id}"                    │
│  All collab servers subscribe to topics of their docs  │
└───────────────────────────┬───────────────────────────┘
                            │
          ┌─────────────────┼──────────────────┐
          ▼                 ▼                  ▼
┌──────────────┐   ┌──────────────┐   ┌──────────────┐
│  STORAGE     │   │  CACHE       │   │  AUTH        │
│  LAYER       │   │  (Redis)     │   │  SERVICE     │
│              │   │              │   │              │
│ • Op Log     │   │ • Hot docs   │   │ • Permissions│
│ • Snapshots  │   │ • Sessions   │   │ • Tokens     │
│ • Metadata   │   │ • Presence   │   │ • Roles      │
└──────────────┘   └──────────────┘   └──────────────┘
```

---

## The Collab Server — What it Actually Does

Each collab server is responsible for:

```
When a USER CONNECTS:
  1. Validate auth token (call Auth service)
  2. Check permissions for doc (can they read/write?)
  3. Fetch current doc state from Storage (or Cache)
  4. Register WebSocket connection in memory
  5. Subscribe to Redis topic "doc:{doc_id}"
  6. Send current doc state + version to client
  7. Broadcast presence update to topic ("User X joined")

When an OPERATION ARRIVES from a client:
  1. Validate op (is it well-formed?)
  2. Check version — does it need transformation?
  3. Apply OT transform if needed
  4. Write op to Storage (Op Log)
  5. Publish transformed op to Redis topic
  6. (All servers, including this one, receive it from Redis)
  7. Each server forwards to their connected clients

When a USER DISCONNECTS:
  1. Remove WebSocket from memory
  2. If no more users on this server for this doc:
     → Unsubscribe from Redis topic (save memory)
  3. Broadcast presence update ("User X left")
```

---

## The Cache Layer — Redis as the Hot Doc Store 🔥

Fetching a document from the database every time someone opens it is expensive. The solution: **Cache the hot documents in memory**.

```
┌─────────────────────────────────────────────────────┐
│                 REDIS CACHE                          │
│                                                      │
│  "doc:abc123:state"    → current doc content        │
│  "doc:abc123:version"  → current version number     │
│  "doc:abc123:presence" → { userA: pos42, userB: pos7}│
│  "session:token_xyz"   → { userId, docId, perms }   │
└─────────────────────────────────────────────────────┘

Cache hit rate target: >95%
(95% of requests served from memory, not disk)

Cache eviction policy: LRU (Least Recently Used)
  → Docs not opened in last 1 hour get evicted
  → When evicted, collab server fetches fresh from DB
```

**What gets cached vs stored:**

```
CACHE (Redis — fast, temporary):        STORAGE (DB — slow, permanent):
  • Current doc state                     • Full op log
  • Active user sessions                  • All snapshots
  • Presence data                         • Document metadata
  • Recent ops (last 100)                 • Permissions
  • Version number                        • Billing/account data
```

---

## Autoscaling — Handling Traffic Spikes 📈

Monday morning, 9 AM. Millions of people open Google Docs simultaneously.

```
8:59 AM: 50,000 active WebSocket connections
9:00 AM: 500,000 active WebSocket connections  ← 10x spike in 60 seconds!
```

**Manual scaling is impossible** — no human can react fast enough.

**Autoscaling** watches metrics and adds/removes servers automatically:

```
Metric watched: WebSocket connections per server

Rule:
  IF avg_connections_per_server > 8,000
    → Spin up 20% more servers
    
  IF avg_connections_per_server < 2,000 (for 10 mins)
    → Remove 20% of servers (save cost)

Spin-up time: ~90 seconds (boot server, start process, warm up cache)
```

**The Thundering Herd Problem:**

```
All servers spin up at 9:00 AM simultaneously.
All try to warm their cache simultaneously.
All hit the database simultaneously.
Database gets 500 servers all asking for the same popular docs.
Database dies. 💥

Solution: Cache warming with jitter
  Each new server waits: random(0, 30) seconds before warming cache
  Spreads the load across 30 seconds instead of instant spike
```

---

## What Happens When a Collab Server Dies Mid-Session?

```
Timeline:
  2:00:00 PM  User A connected to Server 7, editing doc "abc"
  2:00:00 PM  User B connected to Server 7, editing doc "abc"
  2:03:00 PM  Server 7 crashes 💀
  2:03:01 PM  Load balancer detects Server 7 is dead (health check fails)
  2:03:02 PM  Load balancer stops sending new connections to Server 7
  
  User A's WebSocket disconnects.
  User A's client detects disconnect (heartbeat missed).
  User A's client: "Reconnecting..." (automatic)
  
  2:03:05 PM  User A reconnects → Load balancer sends to Server 12
  2:03:05 PM  User B reconnects → Load balancer sends to Server 4
  
  Server 12: Fetches doc "abc" state from Storage
             Fetches User A's buffered ops (stored in Redis)
             Applies buffered ops
             User A is back, no data lost ✅
```

**The key:** Collab servers are **stateless** — they hold WebSocket connections in memory but all important state (ops, doc content) is in Redis and Storage. Any server can pick up any user.

```
Stateless servers = easy recovery from crashes ✅
Stateful servers = crash = data loss ❌
```

---

## Microservices Breakdown

At Google's scale, the collab server itself splits into specialized services:

```
┌─────────────────────────────────────────────────────────┐
│                  MICROSERVICES                           │
│                                                          │
│  ┌─────────────────┐   Handles WebSocket connections    │
│  │ Gateway Service │   Auth, rate limiting, routing     │
│  └────────┬────────┘                                    │
│           │                                             │
│  ┌────────▼────────┐   OT/CRDT transforms               │
│  │ Collab Service  │   Op ordering, version management  │
│  └────────┬────────┘                                    │
│           │                                             │
│  ┌────────▼────────┐   Who's online, cursor positions   │
│  │Presence Service │   Lightweight, lossy is fine       │
│  └────────┬────────┘                                    │
│           │                                             │
│  ┌────────▼────────┐   Save, version history            │
│  │ Storage Service │   Snapshots, op log writes         │
│  └────────┬────────┘                                    │
│           │                                             │
│  ┌────────▼────────┐   Comments, suggestions,           │
│  │Comments Service │   resolved/unresolved threads      │
│  └─────────────────┘                                    │
└─────────────────────────────────────────────────────────┘
```

Each service scales **independently**:

```
Monday morning spike:
  Gateway Service: scale 10x (lots of new connections)
  Collab Service: scale 5x (lots of edits)
  Presence Service: scale 8x (lots of cursor updates)
  Storage Service: scale 2x (writes are batched anyway)
  Comments Service: scale 1x (nobody reads comments at 9 AM 😄)
```

---

## Summary of Chapter 6 🗺️

```
Server Architecture:
─────────────────────────────────────────────────────────────
Problem: Users on different servers can't see each other's edits
─────────────────────────────────────────────────────────────
Sticky Sessions → simple but creates hot spots + failure issues ❌
Pub/Sub (Redis) → servers publish ops to shared topic,
                  all servers with that doc's users receive it ✅
─────────────────────────────────────────────────────────────
Stack:
  DNS (GeoDNS)  → route to nearest data center
  Load Balancer → distribute WebSocket connections
  Collab Server → stateless, handles OT + WebSocket + pub/sub
  Redis         → pub/sub bus + hot cache
  Storage       → op log + snapshots (permanent)
─────────────────────────────────────────────────────────────
Resilience:
  Stateless servers   → crash recovery in ~5 seconds
  Autoscaling         → handles 10x traffic spikes
  Cache warming jitter → prevents thundering herd
─────────────────────────────────────────────────────────────
```

---

## Quick Check ✅

1. **Why do sticky sessions fail under a viral document or server crash?**
2. **How does Pub/Sub solve the cross-server broadcast problem?**
3. **Why must collab servers be stateless? What would happen if they were stateful?**
4. **What is the thundering herd problem and how does jitter solve it?**
5. **Why do different microservices need to scale independently?**

Once these click, we move to **Chapter 7: Permissions, Sharing & Security** — *"How does Google know you can edit but your friend can only view? What stops a malicious user from deleting everyone's documents? How does the sharing link system work?"* 🔐

---

# Chapter 7: Permissions, Sharing & Security — "Who gets to do what?" 🔐

## The Story: The Accidental Deletion

Imagine it's 2007. Google Docs just launched public sharing.

A lawyer shares a confidential client document with a colleague. She meant to give **view-only** access. Instead, she accidentally clicks **"Anyone with the link can edit."**

A random person finds the link. Deletes everything. Document is gone.

Three problems exposed in one incident:

```
1. Permissions weren't checked on every operation
2. Anonymous edits were allowed
3. "Undo" didn't exist at the document level
```

This is why permissions aren't an afterthought — they're baked into **every single layer** of the system.

---

## The Permission Model — Building it From Scratch

Let's design permissions the way Google did, starting simple and adding complexity.

### Level 1: Basic Roles

```
Three fundamental roles:

OWNER    → created the document
           can do everything
           can delete the document
           can transfer ownership

EDITOR   → can read and write content
           can add/remove other editors/viewers
           cannot delete the document

VIEWER   → can only read
           cannot type, comment, or share
           (can make a copy for themselves)
```

### Level 2: The ACL Table

**ACL = Access Control List** — a table that maps users to their permissions on a document:

```sql
TABLE document_permissions (
  doc_id      VARCHAR,
  principal   VARCHAR,    ← could be a user, group, or "anyone"
  role        ENUM('owner', 'editor', 'viewer', 'commenter'),
  granted_by  VARCHAR,    ← who gave this permission
  granted_at  TIMESTAMP,
  expires_at  TIMESTAMP   ← optional: temporary access
  
  PRIMARY KEY (doc_id, principal)
)
```

Example rows:

```
doc_id    │ principal              │ role    │ granted_by
──────────┼────────────────────────┼─────────┼───────────
doc_abc   │ alice@gmail.com        │ owner   │ (self)
doc_abc   │ bob@gmail.com          │ editor  │ alice
doc_abc   │ priya@company.com      │ viewer  │ alice
doc_abc   │ group:engineering-team │ editor  │ alice
doc_abc   │ anyone                 │ viewer  │ alice  ← public link
```

The `principal` can be:
- A specific user email
- A Google Group (thousands of users in one row!)
- `"anyone"` — public link access

---

## The Sharing Link System — How It Works Internally

When Alice clicks **"Get shareable link"**:

```
Step 1: Server generates a cryptographically random token
        token = random_bytes(32) → base64 encode
        = "aB3xK9mNpQ2rS5vW..."  (48 chars, unguessable)

Step 2: Store in database:
        TABLE share_links (
          token      VARCHAR PRIMARY KEY,
          doc_id     VARCHAR,
          role       ENUM,
          created_by VARCHAR,
          created_at TIMESTAMP,
          expires_at TIMESTAMP,  ← optional expiry
          revoked    BOOLEAN      ← can be killed instantly
        )

Step 3: Return link to Alice:
        https://docs.google.com/document/d/aB3xK9mNpQ2rS5vW.../edit

Step 4: When Bob opens the link:
        Server extracts token from URL
        Looks up token in share_links table
        Finds: doc_id="abc", role="editor"
        Creates a session for Bob with editor permissions
```

**Why random tokens instead of putting the doc_id directly in URL?**

```
BAD (doc_id in URL):
  https://docs.google.com/d/1001/edit
  https://docs.google.com/d/1002/edit  ← just increment! Anyone can guess!

GOOD (random token):
  https://docs.google.com/d/aB3xK9mNpQ2rS5vW.../edit
  2^256 possible tokens → impossible to guess ✅
```

---

## Permission Checking — The Authentication Flow

Every operation goes through **two checks**:

```
CHECK 1: AUTHENTICATION — "Who are you?"
  Is this a valid logged-in user?
  Is their session token valid and not expired?

CHECK 2: AUTHORIZATION — "What can you do?"
  Does this user have permission to do THIS ACTION on THIS DOC?
```

Here's the full flow for every WebSocket operation:

```
User types "G" → Operation arrives at server:

┌─────────────────────────────────────────────────┐
│           PERMISSION MIDDLEWARE                  │
│                                                  │
│  1. Extract session token from WebSocket         │
│     header                                       │
│                                                  │
│  2. Validate token with Auth Service             │
│     → Is it expired?                            │
│     → Has it been revoked?                      │
│     → Who does it belong to?                    │
│                                                  │
│  3. Look up ACL for (user, doc_id)              │
│     Check: document_permissions table           │
│     (cached in Redis for speed)                 │
│                                                  │
│  4. Check: does their role allow this action?   │
│     INSERT/DELETE op → needs 'editor' role      │
│     READ → needs 'viewer' role                  │
│                                                  │
│  5a. PASS → forward op to OT engine             │
│  5b. FAIL → reject op, send error to client     │
│             "You don't have edit access"        │
└─────────────────────────────────────────────────┘
```

**This check happens on EVERY operation, not just when opening the doc.**

Why? Because Alice might revoke Bob's access while Bob is actively editing:

```
2:00 PM  Bob opens doc, has editor access ✅
2:05 PM  Alice revokes Bob's access
2:06 PM  Bob types "Hello" → op sent to server
         Server checks ACL → Bob is now a viewer
         Server REJECTS the op ✅
         Bob's client shows: "You no longer have edit access"
```

---

## Caching Permissions — The Stale Cache Problem

ACL checks happen on every keystroke. You can't hit the database every time:

```
10 users typing 5 chars/second each = 50 ACL checks/second per doc
1 million active docs = 50,000,000 ACL checks/second

Database can't handle this. Cache it in Redis!
```

But caching creates a problem:

```
2:00 PM  Cache: Bob has editor access (TTL: 5 minutes)
2:01 PM  Alice revokes Bob's access in database
2:05 PM  Cache STILL says Bob has editor access ← stale!
2:04 PM  Bob edits freely for 3 minutes he shouldn't ❌
2:05 PM  Cache expires, Bob is finally blocked
```

**Solutions:**

```
Option 1: Short TTL (30 seconds)
  → Max 30 seconds of stale permissions
  → Still 30 extra seconds of unauthorized access ⚠️

Option 2: Active invalidation ✅ (what Google does)
  When Alice revokes Bob's access:
    1. Update database
    2. ALSO publish "invalidate:bob:doc_abc" to Redis pub/sub
    3. All collab servers subscribe to invalidation events
    4. They immediately clear Bob's cached permissions
    5. Next operation from Bob → fresh ACL check → rejected

Revocation is now near-instant (~100ms). ✅
```

---

## The Commenter Role — A Subtle Permission Design

Google Docs has a 4th role: **Commenter**. Can't edit text, but can leave comments.

This seems simple but has interesting design implications:

```
Commenter can:
  ✅ Read document content
  ✅ Add comments (anchored to text ranges)
  ✅ Reply to comments
  ✅ Resolve their own comments

Commenter cannot:
  ❌ Insert/delete text
  ❌ Change formatting
  ❌ Resolve other people's comments
  ❌ Share the document
```

**How comments are stored:**

```sql
TABLE comments (
  comment_id  VARCHAR PRIMARY KEY,
  doc_id      VARCHAR,
  author      VARCHAR,
  content     TEXT,
  
  -- Anchor: which text is this comment attached to?
  anchor_start_char_id  VARCHAR,   ← CRDT char ID!
  anchor_end_char_id    VARCHAR,   ← CRDT char ID!
  
  resolved    BOOLEAN,
  created_at  TIMESTAMP
)
```

**Why use CRDT char IDs as anchors?**

```
Without CRDT IDs (position-based):
  Comment anchored to position 42.
  User inserts 10 chars before position 42.
  Comment now points to position 42 → wrong text! ❌

With CRDT char IDs:
  Comment anchored to char_id "A:42".
  User inserts chars before it.
  "A:42" still points to the same character forever. ✅
  Comment stays correctly attached to its text.
```

---

## Offline Permissions — The Edge Case

What if Bob loses internet, edits offline for 20 minutes, then reconnects — but Alice revoked his access while he was offline?

```
Timeline:
  2:00 PM  Bob opens doc, downloads content, caches permissions locally
  2:01 PM  Bob's internet drops
  2:02 PM  Alice revokes Bob's access (Bob doesn't know)
  2:20 PM  Bob reconnects, has 47 buffered operations

What happens to Bob's 47 ops?

Server receives ops from Bob.
Server checks ACL: Bob is no longer an editor.
Server REJECTS all 47 ops.
Client shows: "Your recent changes couldn't be saved. 
               You no longer have edit access."
```

Bob loses his offline work. This is unfortunate but **correct behavior** — you can't retroactively grant yourself permissions.

This is why Google Drive shows a warning when you open a doc in offline mode: *"Changes made offline may not be saved if your access changes."*

---

## Security: Protecting Against Malicious Ops

Beyond permissions, what if a malicious user sends crafted operations?

### Attack 1: Op Injection

```
Malicious user sends:
  Op = { type: DELETE, position: 0, length: 999999 }
  
Trying to delete the ENTIRE document!
```

**Defense: Op Validation Layer**

```
Before processing ANY op:

1. SIZE CHECK: Is the op within reasonable bounds?
   DELETE length > 10,000? → Reject (suspicious)
   
2. POSITION CHECK: Is position within document bounds?
   position > doc.length? → Reject
   
3. RATE LIMITING: Is this user sending ops too fast?
   > 100 ops/second? → Throttle (no human types this fast)
   
4. PERMISSION CHECK: Does op type match role?
   DELETE op from a viewer? → Reject immediately
```

### Attack 2: Version Manipulation

```
Malicious user sends op with fake version:
  Op = { ..., version: 1 }  ← pretending they're at version 1
                               but doc is at version 1,000,000

This would force the server to transform against 999,999 ops!
→ CPU exhaustion / DoS attack
```

**Defense:**

```
Rule: version claimed by client cannot be more than 
      N versions behind the current server version.
      
If client claims version < (current - 1000):
  → Force client to reload the full document
  → Don't process the op
```

### Attack 3: The Invisible Character Attack

```
Malicious user inserts a zero-width Unicode character
at position 0 of every paragraph.

Visually invisible but:
  → Corrupts copy-paste behavior
  → Breaks screen readers
  → Can carry hidden data
```

**Defense:**

```
Character whitelist validation:
  Before accepting any INSERT op:
  Check character against allowed Unicode ranges
  Reject zero-width joiners, bidi override chars,
  and other invisible/dangerous Unicode
```

---

## Rate Limiting — Preventing Abuse

```
Without rate limiting:
  A bot opens 1,000,000 documents simultaneously.
  Each holds a WebSocket connection.
  Server: 💀

Rate limiting rules:

Per user:
  Max 10 document opens per minute
  Max 100 ops per second
  Max 50 share link creations per day

Per IP:
  Max 1,000 requests per minute
  Max 100 new WebSocket connections per minute

Per document:
  Max 1,000 simultaneous editors
  (beyond this, extra users become view-only)
```

Rate limiting is enforced at the **Load Balancer** level — before requests even reach collab servers.

---

## The Audit Log — "Who Did What, When?"

For enterprise customers (Google Workspace), every action is logged:

```sql
TABLE audit_log (
  event_id    VARCHAR PRIMARY KEY,
  doc_id      VARCHAR,
  user_id     VARCHAR,
  action      ENUM('view','edit','share','download','delete',...),
  ip_address  VARCHAR,
  user_agent  VARCHAR,
  timestamp   TIMESTAMP,
  metadata    JSON        ← e.g., { shared_with: "bob@co.com", role: "editor" }
)
```

This answers:
- Who shared this document with whom and when?
- Who downloaded this confidential file?
- Did an employee access documents before resigning?

Audit logs are **write-once, never editable** — even by admins.

---

## Putting It All Together: The Permission Architecture

```
┌──────────────────────────────────────────────────────────┐
│                  PERMISSION SYSTEM                        │
│                                                           │
│  ┌─────────────────────────────────────────────────┐     │
│  │              AUTH SERVICE                        │     │
│  │  • Issues & validates JWT tokens                │     │
│  │  • Session management                           │     │
│  │  • OAuth (Sign in with Google)                  │     │
│  └────────────────────┬────────────────────────────┘     │
│                       │                                   │
│  ┌────────────────────▼────────────────────────────┐     │
│  │            AUTHORIZATION SERVICE                 │     │
│  │  • ACL lookups (Redis cache + DB fallback)      │     │
│  │  • Role resolution (user → group → everyone)   │     │
│  │  • Real-time revocation via pub/sub             │     │
│  └────────────────────┬────────────────────────────┘     │
│                       │                                   │
│  ┌────────────────────▼────────────────────────────┐     │
│  │              OP VALIDATOR                        │     │
│  │  • Size/position bounds checking                │     │
│  │  • Character whitelist                          │     │
│  │  • Rate limiting                                │     │
│  │  • Version range validation                     │     │
│  └────────────────────┬────────────────────────────┘     │
│                       │                                   │
│            Only valid, authorized ops reach               │
│                  the OT Engine below                      │
└──────────────────────────────────────────────────────────┘
```

---

## Summary of Chapter 7 🗺️

```
Permission System:
─────────────────────────────────────────────────────────────
Roles: Owner > Editor > Commenter > Viewer
ACL Table: (doc_id, principal, role) — principal can be
           user, group, or "anyone"
─────────────────────────────────────────────────────────────
Share Links:
  Random 256-bit token → unguessable
  Stored in DB with role + expiry + revocation flag
─────────────────────────────────────────────────────────────
Permission Checking:
  Every op → Auth check → ACL check → Op validation
  Cached in Redis (TTL + active invalidation on revoke)
  Revocation propagated via pub/sub → near-instant
─────────────────────────────────────────────────────────────
Security:
  Op validation  → bounds, character whitelist, rate limit
  Version check  → prevent DoS via fake old versions
  Audit log      → write-once, who did what, when
─────────────────────────────────────────────────────────────
```

---

## Quick Check ✅

1. **Why use random tokens for share links instead of doc IDs?**
2. **Why must permission checks happen on every operation, not just on open?**
3. **How does active cache invalidation make revocation near-instant?**
4. **Why are comment anchors stored as CRDT char IDs instead of positions?**
5. **What happens to Bob's offline edits if his access was revoked while offline?**

Once these click, we move to **Chapter 8: Version History & Recovery** — *"How does Google Docs let you go back in time to any version? How do we handle accidental deletions? What's the story behind autosave?"* 🕰️

---

# Chapter 8: Version History & Recovery — "Time Travel for Documents" 🕰️

## The Story: The 3 AM Panic

It's 3 AM. A startup founder has been working on his investor pitch deck all week. His intern, trying to help, accidentally selects all text and hits delete. Then closes the tab.

```
5 years of company data.
47 slides of content.
Gone in 2 keystrokes.
```

He frantically googles: *"Google Docs recover deleted content"*

He finds: **File → Version History → See Version History**

He goes back 10 minutes. Everything is there. He restores it.

Crisis averted. But how does this actually work under the hood?

---

## The Foundation: The Op Log IS Version History

Remember our Op Log from Chapter 5?

```
op_id │ doc_id  │ user  │ operation      │ version │ timestamp
──────┼─────────┼───────┼────────────────┼─────────┼──────────
001   │ doc_A   │ Alice │ INS("H", 0)    │    1    │ 09:00:01
002   │ doc_A   │ Alice │ INS("e", 1)    │    2    │ 09:00:01
003   │ doc_A   │ Bob   │ INS("!", 5)    │    3    │ 09:00:02
004   │ doc_A   │ Alice │ DEL(2)         │    4    │ 09:00:03
...   │  ...    │  ...  │   ...          │   ...   │   ...
9999  │ doc_A   │ Intern│ DEL(0, 99999)  │  9999   │ 03:00:00
```

The Op Log already contains **every single change ever made**. Version history is essentially free — it's just replaying the log up to a certain point.

```
"Show me the document at 2:55 AM":
  Find all ops where timestamp ≤ 2:55 AM
  Replay them in order
  That's the document at 2:55 AM ✅
```

But replaying 9,999 ops every time someone opens version history is slow. This is where the snapshot strategy becomes critical.

---

## The Snapshot Strategy — Revisited With Purpose

We introduced snapshots in Chapter 5 for fast document loading. But they serve a second purpose: **making version history fast**.

```
SNAPSHOT TRIGGERS:

1. Time-based:    Every 1 hour of activity
2. Op-based:      Every 500 operations  
3. Session-based: When ALL users close the document
4. Manual:        User clicks "Name this version"
5. Daily:         Midnight snapshot regardless of activity
```

Visualized on a timeline:

```
Time ──────────────────────────────────────────────────────────▶

09:00  [SNAPSHOT v0]
09:00 - 10:00: 847 ops
10:00  [SNAPSHOT v847]
10:00 - 11:00: 1,203 ops
11:00  [SNAPSHOT v2050]
...
02:00  [SNAPSHOT v9100]
02:00 - 03:00: 312 ops including THE DELETION at 03:00
03:00  [SNAPSHOT v9412]

To show doc at 2:55 AM:
  1. Load SNAPSHOT v9100  (closest snapshot before 2:55)
  2. Replay only ops from 9100 → timestamp 2:55 AM (~200 ops)
  3. Done in milliseconds ✅

Without snapshots:
  Replay ALL 9,412 ops from the beginning → slow ❌
```

---

## Named Versions — The Bookmark System

Not all versions are equal. Google Docs lets you **name important versions**:

```
"Before investor review"  ← named version at v4,521
"After legal edits"       ← named version at v7,803
"Final submitted"         ← named version at v9,100
```

These are stored separately:

```sql
TABLE named_versions (
  version_id    VARCHAR PRIMARY KEY,
  doc_id        VARCHAR,
  op_version    INTEGER,      ← points to a version in op log
  snapshot_id   VARCHAR,      ← forced snapshot at this point
  name          VARCHAR,      ← "Before investor review"
  created_by    VARCHAR,
  created_at    TIMESTAMP
)
```

When you create a named version:
```
1. Force-create a snapshot at current op version
2. Insert row into named_versions
3. This snapshot is PINNED — never garbage collected
   (regular snapshots get cleaned up after 30 days,
    named versions live forever or until manually deleted)
```

---

## How Autosave Works — The Invisible Safety Net

There's a common misconception: *"I need to hit Ctrl+S to save."*

Google Docs autosaves. But what does that actually mean?

```
Every op is written to the Op Log immediately.
The document is ALWAYS saved at the op level.

"Autosave" in Google Docs is actually:

1. Local → Op Log sync (real-time, per keystroke)
2. Op Log → Snapshot sync (periodic, every ~30 seconds)
3. "Saving..." indicator = snapshot being written

The indicator lifecycle:
  User types  → "Saving..."  (snapshot write in progress)
  Snapshot done → "All changes saved" ✅
  No internet → "Trying to save..." (ops buffered locally)
  Reconnect → "All changes saved" (buffered ops flushed)
```

Even if the browser crashes **mid-keystroke**, the last confirmed op from the server is safe. You might lose the last 1-2 characters (not yet sent), but never more.

---

## The Diff System — Showing What Changed

When you open version history in Google Docs, you see highlighted changes:

```
Version 9,100 → Version 9,412:

[RED]   The quick brown fox jumped over the lazy dog.
[GREEN] The quick brown fox leaped over the sleeping dog.
```

How is this diff computed?

```
Step 1: Reconstruct doc at version 9,100 (from snapshot)
Step 2: Reconstruct doc at version 9,412 (from snapshot)
Step 3: Run diff algorithm (Myers diff algorithm)
        → Finds minimum edit distance between two strings
        → Produces list of: unchanged, added, removed sections
Step 4: Render with red/green highlighting
```

**The Myers Diff Algorithm in simple terms:**

```
Old: "The CAT sat"
New: "The DOG sat"

Find longest common subsequence: "The " + " sat"
Diff result:
  KEEP   "The "
  REMOVE "CAT"
  ADD    "DOG"
  KEEP   " sat"
```

This is the same algorithm used by **Git** for showing file diffs. 

---

## Granularity of Version History — A Design Decision

Here's an interesting product question: **How granular should version history be?**

```
Option A: Every single keystroke is a version
  ✅ Perfect granularity
  ❌ Overwhelms users ("1,847,293 versions" is useless)
  ❌ Huge storage cost

Option B: Versions grouped by time/session  ← Google's approach
  Group ops into "edit sessions":
    If gap between ops > 30 minutes → new session
    If different user → new session
  
  Result: "Alice edited at 2:00 PM - 3:45 PM"
          "Bob edited at 4:00 PM - 4:20 PM"
  ✅ Human-readable
  ✅ Manageable number of versions
  ✅ Can still reconstruct any minute within a session
```

**Grouping logic:**

```
Op stream:
  09:00 Alice types → session "Alice:morning"
  09:01 Alice types → same session
  ...
  10:47 Alice types → same session
  
  [30 minute gap]
  
  11:18 Alice types → NEW session "Alice:afternoon"
  11:18 Bob types   → NEW session "Bob:afternoon"  ← different user
```

---

## The Restore Flow — Going Back in Time

User clicks "Restore this version" on version from 2:55 AM:

```
NAIVE approach (WRONG ❌):
  Delete all ops after 2:55 AM.
  
  Problem: What if the user changes their mind?
  Problem: Other users may have made edits after 2:55 AM
  Problem: Violates the append-only principle

CORRECT approach ✅:
  Generate a NEW operation that transforms 
  the current document INTO the old version.
  
  current doc (after deletion) = ""  (empty)
  target doc  (at 2:55 AM)    = "Hello World..." (full content)
  
  Diff: everything in target but not in current = INSERT it all
  
  Create op: INSERT("Hello World...", position 0, version: 9,413)
  This op is applied normally through the OT engine.
  It becomes version 9,413 in the op log.
  
  The history is PRESERVED. The restore itself is just another op.
```

This means:
```
You can restore a restore.
You can see WHEN someone restored.
You can undo a restore.
The op log is never mutated. ✅
```

---

## Storage Lifecycle — How Long is History Kept?

Storing every op forever is expensive. Google uses a **tiered retention policy**:

```
AGE OF HISTORY       WHAT'S KEPT              GRANULARITY
─────────────────────────────────────────────────────────────
0 - 24 hours    →   Every op                  Per keystroke
24h - 30 days   →   Hourly snapshots          Per hour  
30d - 1 year    →   Daily snapshots           Per day
1 year+         →   Weekly snapshots          Per week
Forever         →   Named versions only       Explicit only

EXCEPTION: Google Workspace (enterprise)
  Admins can configure "retain everything forever"
  Used for legal/compliance requirements (litigation hold)
```

**The garbage collection process:**

```
Nightly GC job runs:

For each doc:
  1. Find all ops older than 30 days
  2. Check: is there a snapshot for each hour? 
     If not, create one from the ops.
  3. Delete the raw ops (keep only hourly snapshots)
  
  4. Find all hourly snapshots older than 1 year
  5. Keep only daily snapshots, delete hourly ones
  
  6. Never delete named versions (pinned forever)
```

---

## Conflict With Simultaneous Restore

What if two people try to restore different versions simultaneously?

```
Alice: "Restore to 2:55 AM"  → generates restore op R1
Bob:   "Restore to 1:00 PM"  → generates restore op R2

Both submitted at the same time!
```

This goes through the **OT engine** like any other operation:

```
R1 and R2 are just ops — massive INSERT/DELETE ops, but ops.
OT transforms them against each other.
One wins (whichever arrives at server first gets applied first).
The other is transformed against it and applied second.

Result: Bob's restore "wins" (was second, so applied last).
        Alice sees: "Your restore was overridden by Bob's restore."
        Alice can then choose to restore again.
```

The system handles this automatically — it's not a special case.

---

## The "Suggesting" Mode — Track Changes

Google Docs has a **Suggesting mode** (like Track Changes in Word). This is a permission + versioning feature:

```
Editor mode:   Your changes apply immediately to the doc
Suggesting mode: Your changes appear as SUGGESTIONS
                 Owner/Editor must Accept or Reject

How it works internally:

Suggestion = special op type with metadata:
  {
    type: "suggestion",
    op: INSERT("Hello", position 5),
    suggested_by: "bob@email.com",
    status: "pending"  ← pending | accepted | rejected
  }

Suggestions are stored in a separate table:
  TABLE suggestions (
    suggestion_id  VARCHAR PRIMARY KEY,
    doc_id         VARCHAR,
    op_data        JSON,        ← the actual operation
    suggested_by   VARCHAR,
    status         ENUM,
    resolved_by    VARCHAR,
    resolved_at    TIMESTAMP
  )

When suggestion is ACCEPTED:
  The op is extracted from suggestions table
  Applied to the doc as a regular op
  Suggestion marked as "accepted"

When suggestion is REJECTED:
  Op is discarded
  Suggestion marked as "rejected"
  (The suggested text disappears from the doc)
```

---

## Version History API — For Enterprise

Google Workspace exposes version history via API:

```
GET /documents/{docId}/revisions
→ Returns list of all revisions (grouped sessions)

GET /documents/{docId}/revisions/{revisionId}
→ Returns snapshot of doc at that revision

POST /documents/{docId}/revisions/{revisionId}/restore  
→ Restores to that revision
```

This lets enterprises build:
```
• Compliance auditing dashboards
• "Who wrote this paragraph?" tools
• Automated backup systems
• Content policy enforcement
  ("Alert if anyone adds content matching pattern X")
```

---

## Putting It All Together: Version History Architecture

```
┌──────────────────────────────────────────────────────────────┐
│                VERSION HISTORY SYSTEM                         │
│                                                               │
│  WRITE PATH (every op):                                       │
│    Op → Op Log (immediate)                                    │
│       → Snapshot trigger check                                │
│         → If triggered: write Snapshot                        │
│         → If named: pin Snapshot forever                      │
│                                                               │
│  READ PATH (open version history):                            │
│    User picks a time T                                        │
│    → Find nearest snapshot S before T                         │
│    → Replay ops from S to T                                   │
│    → Compute diff vs current version                          │
│    → Render with highlights                                   │
│                                                               │
│  RESTORE PATH:                                                │
│    Find doc state at time T                                   │
│    → Diff against current state                               │
│    → Generate inverse ops                                     │
│    → Apply as new ops (append-only preserved)                 │
│                                                               │
│  GC PATH (nightly job):                                       │
│    Compact old ops → hourly snapshots                         │
│    Compact old hourly → daily snapshots                       │
│    Never touch named/pinned versions                          │
└──────────────────────────────────────────────────────────────┘
```

---

## Summary of Chapter 8 🗺️

```
Version History:
─────────────────────────────────────────────────────────────
Foundation:  Op Log = built-in version history (free!)
Snapshots:   Periodic frozen copies for fast reconstruction
Named vers:  Pinned snapshots, never garbage collected
─────────────────────────────────────────────────────────────
Key Flows:
  Autosave  → every op hits op log immediately
  View hist → snapshot + replay ops to target time
  Restore   → diff current vs target → new ops (append-only)
  Diff view → Myers algorithm on two reconstructed docs
─────────────────────────────────────────────────────────────
Retention Policy:
  0-24h    → every op (per keystroke)
  24h-30d  → hourly snapshots
  30d-1yr  → daily snapshots
  1yr+     → weekly snapshots
  Forever  → named versions only
─────────────────────────────────────────────────────────────
Suggesting Mode:
  Ops stored separately with status (pending/accepted/rejected)
  Accepted → becomes regular op in doc
  Rejected → discarded, never touches doc
─────────────────────────────────────────────────────────────
```

---

## Quick Check ✅

1. **Why is restoring a version done by creating NEW ops rather than deleting old ones?**
2. **What are the two purposes snapshots serve?**
3. **Why does Google group keystrokes into sessions instead of showing every op in version history?**
4. **How does the Myers diff algorithm produce the red/green highlights?**
5. **What happens to version history when two users simultaneously try to restore different versions?**

Once these click, we move to **Chapter 9: Scaling to Google's Scale** — *"We've designed a great system for thousands of users. Now how do we handle a billion? GeoDNS, CDNs, global data centers, and the CAP theorem — the final boss of distributed systems."* 🌍

---

# Chapter 9: Scaling to Google's Scale — "From Thousands to a Billion" 🌍

## The Story: The Geography Problem

It's 2010. Google Docs is growing fast. A user in Mumbai opens a document.

```
Her request travels:
  Mumbai → undersea cable → London → transatlantic cable → 
  New York → Google's data center in Virginia → back

Total distance: ~14,000 km
Speed of light in fiber: ~200,000 km/second
Minimum possible latency: ~70ms ONE WAY
Round trip: ~140ms minimum

She types a character.
Waits 140ms to see it confirmed.
Types another.
Waits 140ms.

Feels like typing through mud. ❌
```

The problem isn't the code. It's **physics**. Light has a speed limit.

The only solution: **bring the servers closer to the users**.

---

## Global Data Centers — The First Step

Google builds data centers on every continent:

```
                    🌍 GOOGLE'S GLOBAL NETWORK

         ┌─────────────────────────────────────────────┐
         │                                             │
    [US-WEST]    [US-EAST]    [EUROPE]    [ASIA]      │
    Los Angeles  Virginia     London      Singapore   │
    Oregon       Iowa         Frankfurt   Tokyo       │
                              Amsterdam   Mumbai      │
                                          Sydney      │
         │                                             │
         └────── All connected via Google's private ───┘
                  fiber backbone (faster than public
                  internet — no congestion)
```

Now Mumbai user hits the **Singapore** data center:

```
Mumbai → Singapore: ~2,500 km
Round trip latency: ~25ms

From 140ms → 25ms.
6x improvement just from geography. ✅
```

But how does Mumbai's browser know to go to Singapore instead of Virginia?

---

## GeoDNS — Routing by Location

When you type `docs.google.com`, your browser asks a DNS server:
*"What IP address is docs.google.com?"*

**Regular DNS:** Always returns the same IP.

**GeoDNS:** Returns a *different IP* based on where you're asking from.

```
DNS query from Mumbai:
  "What is docs.google.com?"
  GeoDNS checks: requester IP is in India
  Returns: 74.125.200.x  ← Singapore data center IP

DNS query from London:
  "What is docs.google.com?"
  GeoDNS checks: requester IP is in UK
  Returns: 216.58.213.x  ← London data center IP

DNS query from New York:
  "What is docs.google.com?"
  GeoDNS checks: requester IP is in US East
  Returns: 172.217.14.x  ← Virginia data center IP
```

**The routing decision tree:**

```
Incoming DNS query
       │
       ▼
  What region is this IP from?
       │
  ┌────┴──────┬──────────┬──────────┐
  ▼           ▼          ▼          ▼
Asia-Pac    Europe    Americas    Africa
  │           │          │          │
Singapore  Frankfurt  Virginia   Johannesburg
  │
  ▼
Is Singapore healthy? (< 80% capacity?)
  YES → return Singapore IP
  NO  → return Tokyo IP  (failover to next nearest)
```

---

## The CDN Layer — Static Assets at the Edge

GeoDNS routes users to the right **compute** (servers). But Google Docs also loads:

```
• JavaScript bundle (~2MB)
• CSS files
• Icons and images  
• Font files
• Spell-check dictionaries
```

These files **never change** (or change rarely). It's wasteful to serve them from Virginia to every user in the world.

**CDN = Content Delivery Network**

```
WITHOUT CDN:
  Mumbai user loads docs.google.com
  Fetches 2MB JS bundle from Virginia
  ~140ms latency × many round trips = 3-4 second load time ❌

WITH CDN:
  CDN has 200+ "edge nodes" worldwide
  Mumbai → nearest edge node (maybe in Chennai, 300km away)
  Edge node has the JS bundle CACHED
  Serves it in ~5ms ✅
```

**How CDN caching works:**

```
First user in Mumbai opens Google Docs:
  1. Browser requests JS bundle
  2. CDN edge (Chennai) doesn't have it → CACHE MISS
  3. Chennai edge fetches from origin (Virginia)
  4. Chennai edge stores a copy
  5. Serves to Mumbai user

Every subsequent user in India:
  1. Browser requests JS bundle
  2. CDN edge (Chennai) has it → CACHE HIT
  3. Served instantly from Chennai ✅
  4. Virginia never involved again

Cache TTL: 1 year for versioned assets
  (filename includes content hash: "main.a3f9b2c.js")
  When code changes → new filename → automatic cache bust
```

---

## Multi-Region Architecture — The Full Picture

Each regional data center is a **complete, independent copy** of the system:

```
┌────────────────────────────────────────────────────┐
│              SINGAPORE DATA CENTER                  │
│                                                     │
│  Load Balancer                                      │
│       │                                             │
│  ┌────┴──────────────────────────┐                 │
│  │     Collab Server Pool        │                 │
│  │  (100s of servers)            │                 │
│  └────────────┬──────────────────┘                 │
│               │                                     │
│  ┌────────────▼──────────────────┐                 │
│  │     Redis Cluster             │                 │
│  │  (pub/sub + cache)            │                 │
│  └────────────┬──────────────────┘                 │
│               │                                     │
│  ┌────────────▼──────────────────┐                 │
│  │     Spanner Region            │                 │
│  │  (sharded + replicated DB)    │                 │
│  └───────────────────────────────┘                 │
└────────────────────────────────────────────────────┘
         │ Google private fiber backbone │
┌────────┴──────────────────────────────▼────────────┐
│              VIRGINIA DATA CENTER                   │
│         (identical structure)                       │
└────────────────────────────────────────────────────┘
```

But this raises the **hardest problem** in distributed systems:

> *"What if users in Mumbai and users in London are editing the same document simultaneously — on different data centers?"*

---

## The CAP Theorem — The Fundamental Tradeoff

This is the **most important theoretical concept** in distributed systems for interviews.

**CAP Theorem** states: A distributed system can only guarantee 2 of these 3 properties simultaneously:

```
C — CONSISTENCY
    Every read sees the most recent write.
    All nodes see the same data at the same time.
    "Mumbai and London always see identical documents"

A — AVAILABILITY  
    Every request gets a response (not an error).
    System is always up, always responds.
    "Users can always read/write, never get errors"

P — PARTITION TOLERANCE
    System keeps working even if network between
    nodes breaks (network partition).
    "Singapore and Virginia can't talk to each other
     — system still works"
```

**The brutal truth:** Network partitions WILL happen. Cables cut. Data centers lose connectivity. P is not optional.

So the real choice is: **CP or AP?**

```
CP (Consistency + Partition tolerance):
  During a partition → refuse to serve stale data
  System returns errors until partition heals
  "Document may be temporarily unavailable"
  
  Used by: Banking systems, Google Spanner

AP (Availability + Partition tolerance):  
  During a partition → serve potentially stale data
  System keeps working with possibly old information
  "Document is always available, may briefly be out of sync"
  
  Used by: DNS, Shopping carts, Cassandra
```

**Where does Google Docs sit?**

```
Google Docs makes a NUANCED choice:

For document READS:    AP  (always serve something, 
                             might be slightly stale)

For document WRITES:   CP  (never lose an op,
                             might queue/delay writes
                             during partition)

For presence/cursors:  AP  (best effort, lossy is fine)
```

This is called **"tunable consistency"** — different guarantees for different data types.

---

## The Cross-Region Problem — Same Doc, Two Continents

The hardest scenario:

```
Document "abc123" — Alice in Mumbai, Bob in London
Alice connects to Singapore DC
Bob connects to Frankfurt DC

Alice types "Hello" at 10:00:00.000
Bob types "World" at 10:00:00.001

Both ops need to be ordered and transformed against each other.
But Singapore and Frankfurt are ~8,000 km apart.
Light-speed latency: ~40ms between them.
```

**Solution: One Region Owns Each Document**

```
Every document has a PRIMARY REGION — one data center
that "owns" the document and runs OT for it.

doc "abc123" primary region: SINGAPORE (where it was created)

Alice (Mumbai→Singapore): 25ms latency to primary ✅ (fast)
Bob (London→Frankfurt→Singapore): 
  Frankfurt handles Bob's WebSocket connection
  But forwards ops to Singapore for OT processing
  Singapore transforms, broadcasts back
  Bob receives result via Frankfurt
  Total: Frankfurt↔Singapore adds ~80ms ⚠️ (acceptable)
```

**The tradeoff:**

```
Bob's latency:
  Local to Frankfurt: ~10ms (WebSocket)
  Frankfurt to Singapore (op processing): ~80ms
  Singapore back to Frankfurt: ~80ms
  Frankfurt to Bob: ~10ms
  
  Total: ~180ms for Bob's op to be confirmed

Is 180ms acceptable?
  → For typing, you apply op locally immediately (OT optimistic)
  → Confirmation latency doesn't block typing
  → Bob sees his own keystrokes instantly
  → He just waits 180ms for official "version number" assignment
  → Invisible to Bob in practice ✅
```

---

## Document Migration — Moving the Primary Region

What if a document is created in Singapore but the team moves to London?

```
Initial state: "abc123" primary = Singapore
Observation: 98% of ops for "abc123" come from Europe
             Only 2% from Asia

Migration trigger:
  IF majority_of_ops_from_region(X) 
     AND current_primary != X
     AND primary_has_been_stable > 24 hours
  THEN migrate primary to X

Migration process:
  1. Singapore primary pauses accepting new ops (briefly)
  2. Syncs final state to Frankfurt
  3. Frankfurt becomes new primary
  4. Singapore becomes secondary (receives updates)
  5. GeoDNS updated: ops for "abc123" now route to Frankfurt
  
  Pause duration: ~200ms (users see "Saving..." briefly)
  Then everything is faster for the European team ✅
```

---

## Eventual Consistency in Practice — The Replica Lag

When Singapore processes an op and replicates to Frankfurt:

```
10:00:00.000  Op applied in Singapore (primary)
10:00:00.040  Op replicated to Frankfurt (40ms lag)
10:00:00.045  Op replicated to Virginia (45ms lag)
10:00:00.060  Op replicated to Tokyo (60ms lag)

During those 40-60ms:
  A user reading from Frankfurt sees the OLD version
  A user reading from Singapore sees the NEW version
  
  They're temporarily inconsistent → "eventual consistency"
  
After 60ms: All regions have the update → consistent again
```

**Is this acceptable for Google Docs?**

```
For active editing: NO — you route to primary region
For reading an old document: YES — 60ms stale is fine
For version history: YES — reading 5-minute-old snapshot anyway
```

---

## The Global Load Balancing Algorithm

When a new connection comes in, Google uses a sophisticated routing decision:

```
New connection from Mumbai:

Step 1: GeoDNS → Singapore DC

Step 2: Singapore Load Balancer checks:
  - Singapore capacity: 73% (healthy)
  - Singapore latency to user: 25ms
  → Route to Singapore ✅

Step 3: If Singapore was overloaded (>90%):
  Compare alternatives:
  - Tokyo:     35ms latency, 45% capacity
  - Mumbai DC: 8ms latency,  88% capacity (too full)
  - Tokyo wins (capacity × latency score)
  → Route to Tokyo

Step 4: Within Singapore, which collab server?
  Least-connections algorithm:
  Server 1: 8,241 connections
  Server 2: 8,187 connections  ← lowest
  Server 3: 8,309 connections
  → Route to Server 2
```

---

## Hotspot Documents — The Viral Problem 🔥

A document goes viral. 500,000 people open it simultaneously.

```
Normal document:   10-50 concurrent editors
Viral document:    500,000 concurrent viewers

Problems:
1. One primary region handles ALL ops → overwhelmed
2. Redis topic "doc:viral" has 500K subscribers → 
   every op broadcast to 500K connections simultaneously
3. A single server can't hold 500K WebSocket connections
```

**Solution: Read Replicas + Tiered Broadcasting**

```
For documents with >10,000 simultaneous users:

TIER 1: Primary region handles WRITES only
         (actual editing ops → OT processing)

TIER 2: Read replicas handle READ-ONLY viewers
         Viewers connect to nearest replica
         Replica streams ops from primary
         Replica broadcasts to its local viewer pool

TIER 3: CDN-level streaming for huge audiences
         (like a live document broadcast)

Structure:
  Primary (Singapore): handles 1,000 active editors
       │ streams ops
  ┌────┴──────────────────────────┐
  ▼                               ▼
Replica (Frankfurt)          Replica (Virginia)
10,000 viewers               50,000 viewers
       │                          │
  ┌────┴────┐               ┌────┴────┐
  ▼         ▼               ▼         ▼
10K users  10K users    25K users  25K users
```

---

## The Numbers — What Google Actually Handles

```
Google Docs at scale (estimated):

Documents:           ~2 billion total
Daily active docs:   ~50 million
Peak concurrent:     ~5 million simultaneous editors
Ops per second:      ~50 million ops/sec at peak
Storage:             ~10 exabytes (10 million terabytes)
Data centers:        ~35 worldwide
Collab servers:      ~100,000+ instances globally

WebSocket connections at peak:
  5M users × 1 connection = 5 million connections
  Per server max: ~10,000 connections
  Servers needed: ~500 minimum (with headroom: ~2,000)

Redis pub/sub throughput:
  50M ops/sec broadcast across active docs
  Each op to avg 3 users = 150M messages/sec through Redis
  → Redis cluster with 1,000+ nodes
```

---

## Observability — How Do You Know It's All Working?

At this scale, things break constantly. You need to **know before users do**.

```
THREE PILLARS OF OBSERVABILITY:

1. METRICS (numbers over time)
   • Ops/second per region
   • WebSocket connection count
   • Cache hit rate
   • DB write latency (p50, p95, p99)
   • Error rate per service
   
   Tool: Google's internal Monarch (like Prometheus)
   Dashboard: Shows all metrics in real-time
   Alert: "p99 latency > 500ms for 5 minutes → page oncall engineer"

2. LOGS (what happened)
   • Every op processed: { op_id, doc_id, latency, status }
   • Every error: stack trace, context, user impact
   • Every deployment: what changed, when, who
   
   Tool: Google's Dapper (like ELK stack)
   Usage: "Why did ops fail at 3:47 PM?" → search logs

3. TRACES (follow one request end-to-end)
   • One op traced through every service it touches
   • WebSocket receive → OT engine → Redis → DB → broadcast
   • Shows exactly where latency lives
   
   Example trace for one op:
   WebSocket receive:    2ms
   Auth check (cache):   1ms
   OT transform:         3ms
   DB write:            12ms  ← bottleneck!
   Redis publish:        2ms
   Total:               20ms
```

---

## Chaos Engineering — Breaking Things on Purpose

At Google's scale, you can't wait for failures to happen. You **simulate them**:

```
CHAOS ENGINEERING (inspired by Netflix's Chaos Monkey):

Every week, randomly:
  • Kill 5% of collab servers mid-session
    → Verify: users reconnect within 10 seconds
    
  • Simulate Singapore→Frankfurt network partition
    → Verify: Frankfurt falls back to Virginia
    
  • Fill a DB shard to 95% capacity
    → Verify: automatic resharding triggers
    
  • Inject 500ms latency into Redis
    → Verify: system degrades gracefully (not crashes)

Goal: Find weaknesses BEFORE real disasters do.
"If it hurts, do it more often — until it doesn't hurt."
```

---

## Summary of Chapter 9 🗺️

```
Global Scale Architecture:
─────────────────────────────────────────────────────────────
Geography:
  GeoDNS     → route users to nearest data center
  CDN        → static assets at the edge (sub-10ms)
  Private backbone → faster than public internet between DCs

CAP Theorem:
  Can't have C + A + P simultaneously
  Google Docs: CP for writes, AP for reads
  Tunable consistency: different guarantees per data type

Cross-Region:
  One primary region owns each document's OT processing
  Other regions forward ops to primary, receive results
  Document migration when team location shifts

Hotspot Handling:
  Viral docs → tiered read replica broadcasting
  Primary handles writes, replicas serve viewers

Scale Numbers:
  5M concurrent connections, 50M ops/sec at peak
  35 DCs, 100K+ servers, 10 exabytes storage

Observability:
  Metrics → know WHAT is broken
  Logs    → know WHAT HAPPENED
  Traces  → know WHERE in the stack

Chaos Engineering:
  Break things intentionally to find weaknesses
─────────────────────────────────────────────────────────────
```

---

## Quick Check ✅

1. **Why can't we just have one giant data center? What's the fundamental limit?**
2. **In CAP theorem, why is P (partition tolerance) non-negotiable?**
3. **How does Google Docs handle Alice in Mumbai and Bob in London editing the same doc?**
4. **What changes architecturally when a document goes viral with 500K viewers?**
5. **What's the difference between metrics, logs, and traces — when do you use each?**

---

Once these click, we move to the **Final Chapter 10: The Complete Picture — Putting It All Together** 🗺️

*We'll draw the complete end-to-end architecture, walk through every interview question you might face, discuss what to say first in an interview, how to handle follow-up questions, and the key tradeoffs you should always mention.*

**This is the chapter that turns everything you learned into interview-ready answers.** 🚀

---

# Chapter 10: The Complete Picture — Interview Ready 🎯

## How to Approach This in an Interview

Before drawing a single box, say this:

```
"Before I jump into the design, let me clarify 
 the requirements and constraints — because every 
 design decision flows from these."
```

Interviewers LOVE this. It shows senior engineering thinking.

---

## Step 1: Requirements Clarification (First 5 Minutes)

### Functional Requirements
```
Ask the interviewer:

1. "Should we support real-time collaboration 
    or just single-user editing?"
   → Collaborative (this changes everything)

2. "Do we need version history / undo?"
   → Yes

3. "What kind of content? Plain text only, 
    or rich text (bold, tables, images)?"
   → Rich text (more complex ops)

4. "Do we need offline support?"
   → Yes (edits should persist when reconnecting)

5. "What sharing model? Private, link-based, 
    or public?"
   → All three (like Google Docs)
```

### Non-Functional Requirements
```
Ask about scale:

"How many users are we designing for?"
→ 1 billion users, 50M active docs/day

"What's acceptable latency for seeing 
 a collaborator's edit?"
→ Under 100ms ideally

"What's our consistency requirement?"
→ No data loss, eventual consistency acceptable 
  for reads

"Any geographic requirements?"
→ Global users, need low latency worldwide
```

Write these on the board:

```
FUNCTIONAL:                    NON-FUNCTIONAL:
✅ Real-time collaboration     ✅ < 100ms edit propagation
✅ Rich text editing           ✅ 99.99% availability
✅ Version history             ✅ Zero data loss
✅ Offline support             ✅ 1B users, global
✅ Permissions & sharing       ✅ 50M active docs/day
✅ Comments & suggestions      ✅ 5M concurrent editors
```

---

## Step 2: The High-Level Architecture (Next 10 Minutes)

Draw this on the board TOP to BOTTOM:

```
                        USERS (browser/mobile)
                               │
                        ┌──────▼──────┐
                        │   GeoDNS    │  Routes to nearest DC
                        └──────┬──────┘
                               │
                        ┌──────▼──────┐
                        │     CDN     │  Static assets (JS/CSS)
                        └──────┬──────┘
                               │
                        ┌──────▼──────┐
                        │    Load     │  Distributes WebSocket
                        │  Balancer   │  connections
                        └──────┬──────┘
                               │
              ┌────────────────┼────────────────┐
              ▼                ▼                ▼
        ┌──────────┐    ┌──────────┐    ┌──────────┐
        │  Collab  │    │  Collab  │    │  Collab  │
        │ Server 1 │    │ Server 2 │    │ Server 3 │
        │(stateless│    │(stateless│    │(stateless│
        └────┬─────┘    └────┬─────┘    └────┬─────┘
             │               │               │
             └───────────────┼───────────────┘
                             │ pub/sub
                        ┌────▼─────┐
                        │  Redis   │  Pub/Sub + Hot Cache
                        │ Cluster  │
                        └────┬─────┘
                             │
              ┌──────────────┼──────────────┐
              ▼              ▼              ▼
        ┌──────────┐  ┌──────────┐  ┌──────────┐
        │  Op Log  │  │Snapshots │  │  Auth &  │
        │    DB    │  │    DB    │  │  Perms   │
        │(sharded) │  │(sharded) │  │    DB    │
        └──────────┘  └──────────┘  └──────────┘
```

Say out loud as you draw:
> *"I'll use WebSockets for real-time communication, stateless collab servers so any server can handle any user, Redis pub/sub so cross-server users see each other's edits, and a two-layer storage system — op log for truth and snapshots for speed."*

---

## Step 3: Deep Dive Each Component

The interviewer will say *"tell me more about X"*. Here's your answer for each:

---

### "How does real-time collaboration work?"

```
Walk through OT in 60 seconds:

"Every edit becomes an Operation — INSERT or DELETE 
 with a position and version number.

When two users edit simultaneously, the server 
transforms conflicting ops using Operational 
Transformation before applying them.

The key insight: operations carry version numbers.
If User A's op was made at version 5 but the server
is now at version 7, we transform A's op against 
versions 6 and 7 before applying.

This guarantees all users converge to the same 
document state. Google Docs uses OT. 
Newer systems like Figma use CRDTs — which assign 
unique IDs to each character instead of positions,
making conflicts mathematically impossible."

Mention the tradeoff:
"OT requires a central server as referee.
 CRDTs work peer-to-peer but use more memory 
 (tombstones for deleted chars never truly deleted)."
```

---

### "How does storage work?"

```
"Two layers:

LAYER 1 — Op Log (append-only):
  Every operation ever made, stored forever.
  Never updated, never deleted.
  Source of truth for version history.
  Like an accounting ledger.

LAYER 2 — Snapshots:
  Periodic frozen copies of the full document.
  Taken every ~500 ops or hourly.
  Avoids replaying millions of ops on every open.

Opening a doc:
  Find nearest snapshot + replay ops since it.

This is sharded by hash(doc_id) across DB nodes
with 3-replica replication using Raft consensus.
Primary handles writes, secondaries can serve reads.
Failover in ~5-10 seconds if primary dies."
```

---

### "How does sharding work?"

```
"I'd use consistent hashing on doc_id.

Why not range-based?
  Creates hot spots — all new documents land on 
  the last shard.

Why consistent hashing over simple modulo?
  hash(doc_id) % N breaks when N changes.
  With consistent hashing, adding a new shard 
  only moves ~1/N of the data, not everything.

Each shard:
  1 Primary + 2 Secondaries
  Writes go to primary, replicated to secondaries
  Acknowledged after majority (2/3) confirm
  (Raft consensus protocol)"
```

---

### "How do you handle offline editing?"

```
"The client buffers ops locally when disconnected.

Key rule: only ONE op can be in-flight to the server
at a time. Others queue in a buffer.

On reconnect:
  Client sends: 'I was at version 203, here are 
  my 12 buffered ops'
  
  Server transforms client's ops against all ops
  that happened server-side since version 203.
  
  Client receives those server ops, applies them.
  Everyone converges. No data lost.

This is why you see 'Reconnecting...' then 
'All changes saved' — that's the buffer flushing."
```

---

### "How does version history work?"

```
"Version history is essentially free because 
 the Op Log already stores everything.

To reconstruct doc at time T:
  1. Find nearest snapshot before T
  2. Replay ops from snapshot to T

Restore is done by generating NEW ops that 
transform current state back to target state.
The op log is never mutated — append-only.

Retention policy:
  0-30 days:  every op (per-keystroke granularity)
  30d-1yr:    hourly snapshots
  1yr+:       daily snapshots
  Forever:    named versions (pinned by user)"
```

---

### "How do permissions work?"

```
"ACL table: (doc_id, principal, role)
  Principal = user, group, or 'anyone'
  Role = owner, editor, commenter, viewer

Share links use random 256-bit tokens — 
not sequential IDs that can be guessed.

Permission check on EVERY operation:
  Auth → 'who are you?' (JWT validation)
  Authz → 'what can you do?' (ACL lookup)
  
Cached in Redis with active invalidation:
  When access revoked → pub/sub event →
  all servers immediately clear cached permissions
  Revocation is near-instant (~100ms)."
```

---

### "How do you scale globally?"

```
"Three layers:

1. GeoDNS → route users to nearest data center
   Mumbai → Singapore (~25ms vs ~140ms to Virginia)

2. Each DC is a full independent copy of the system
   
3. Each document has ONE primary region for OT.
   Other regions forward ops to primary.
   Document migrates primary region if usage shifts.

CAP theorem tradeoff:
  Writes → CP (never lose ops, may queue during partition)
  Reads  → AP (serve slightly stale data, always available)
  Presence (cursors) → AP (best effort, lossy fine)

For viral documents (500K viewers):
  Tiered read replicas — primary handles editors,
  replicas fan out to viewers."
```

---

## Step 4: The Key Tradeoffs to Always Mention

Interviewers specifically listen for these. Say them proactively:

```
TRADEOFF 1: OT vs CRDT
  OT:   simpler memory, needs central server
  CRDT: works offline/P2P, memory bloat from tombstones
  Choice depends on: offline requirements + team topology

TRADEOFF 2: Consistency vs Availability (CAP)
  We chose AP for reads (always serve, may be stale)
  We chose CP for writes (never lose data, may queue)
  Different data types get different guarantees

TRADEOFF 3: Op Log vs Snapshots
  Op Log alone: perfect history, slow to reconstruct
  Snapshots alone: fast open, no history
  Both together: fast open + full history ✅
  Cost: storage is duplicated

TRADEOFF 4: Sticky Sessions vs Pub/Sub
  Sticky: simple, breaks on server crash + hot docs
  Pub/Sub: complex, scales infinitely, handles failures
  
TRADEOFF 5: Monolith vs Microservices
  Monolith: simpler ops, harder to scale parts independently
  Microservices: each component scales separately
  (Presence service scales 8x, Storage only 2x on Monday morning)
```

---

## Step 5: The Numbers You Should Know

Memorize these for interviews — they show you think at scale:

```
LATENCY TARGETS:
  Keystroke to collaborator screen: < 100ms
  Document open time:               < 1 second
  Permission check (cached):        < 5ms
  Cross-region op propagation:      < 80ms

SCALE TARGETS:
  Concurrent WebSocket connections: 5 million
  Ops per second at peak:          50 million
  Active documents per day:        50 million
  Total documents:                 ~2 billion

INFRASTRUCTURE:
  Collab servers needed at peak:   ~2,000
  Max connections per server:      ~10,000
  Redis pub/sub nodes:             ~1,000
  DB shards:                       ~500+
  
STORAGE:
  Op per keystroke:                ~50 bytes
  10 ops/second × 50M active docs  = 25GB/second writes
  With snapshots + replication:    ~75GB/second total I/O
```

---

## Step 6: The Complete Component Summary

The full system, every component and its purpose:

```
COMPONENT          PURPOSE                    TECH
──────────────────────────────────────────────────────────
GeoDNS             Route to nearest DC        Google Cloud DNS
CDN                Serve static assets fast   Cloudflare/Google CDN
Load Balancer      Distribute WebSockets      Google Cloud LB
Collab Server      OT engine + WS handler     Stateless Node.js
Redis Pub/Sub      Cross-server broadcasting  Redis Cluster
Redis Cache        Hot doc state + sessions   Redis Cluster
Op Log DB          Source of truth (history)  Google Spanner
Snapshot DB        Fast doc reconstruction    Google Spanner
Auth Service       JWT validation             Custom + OAuth
ACL Service        Permission checks          Custom + Redis cache
Presence Service   Cursor broadcasting        Lightweight, lossy
Comments Service   Comments + suggestions     Separate microservice
Audit Log          Who did what when          Write-once store
Chaos System       Intentional fault testing  Custom
Monitoring         Metrics + logs + traces    Monarch/Dapper/Trace
```

---

## Common Follow-Up Questions & Answers

```
Q: "What if the OT server becomes a bottleneck?"
A: "Each document has its own primary region.
    Documents are independent — one hot doc doesn't 
    affect others. For truly viral docs (500K users),
    we separate read replicas from the write path."

Q: "How do you handle a data center going down?"
A: "GeoDNS detects unhealthy DC (health checks fail).
    Stops routing new users there.
    Existing users get WebSocket disconnect.
    Client reconnects → GeoDNS sends to next nearest DC.
    New DC fetches doc state from global Spanner.
    Recovery: ~30-60 seconds for most users."

Q: "What about documents with images/videos?"
A: "Binary content (images) stored separately in 
    object storage (like Google Cloud Storage).
    Doc stores a reference (content-addressed hash).
    Image ops = INSERT reference to image, not the bytes.
    CDN serves images globally. 
    Doc ops remain tiny even with media."

Q: "How do you prevent someone from spamming ops?"
A: "Rate limiting at load balancer:
    Max 100 ops/second per user.
    Max 10,000 ops/hour per user.
    Suspicious patterns (bot-like regularity) → CAPTCHA.
    Op size limits: INSERT can't add > 10,000 chars at once."

Q: "How does spell check work at scale?"
A: "Spell check runs CLIENT-SIDE in the browser.
    Dictionary downloaded once (cached by CDN).
    Spell check = pure JS, zero server load.
    Grammar check (Grammarly-style) uses background
    API calls — low frequency, not per-keystroke."
```

---

## What Separates a Good Answer from a Great Answer

```
GOOD candidate:
  Mentions WebSockets, OT, sharding, replication.
  Draws the basic architecture.
  Answers questions correctly.

GREAT candidate:
  1. Starts with requirements, not solutions.
  
  2. Mentions tradeoffs proactively:
     "I chose OT over CRDT because we have a central 
      server — if offline-first was required I'd 
      reconsider."
     
  3. Quantifies decisions:
     "10,000 connections per server × 500 servers 
      = 5M concurrent users — matches our requirement."
      
  4. Identifies failure modes unprompted:
     "The risk here is a viral document overwhelming 
      one primary region — here's how I'd handle it..."
      
  5. Shows awareness of evolution:
     "I'd start with a simpler single-region design
      and add geo-distribution once we hit 10M users.
      Premature optimization at smaller scale adds 
      complexity without benefit."
```

---

## The Story Arc — How All 9 Chapters Connect

```
Chapter 1: PROBLEM
  Email attachments → version chaos → need real-time collab

Chapter 2: OT (the HOW of real-time editing)
  Operations not blobs → transform conflicts → server is referee

Chapter 3: CRDT (the alternative)  
  Unique IDs not positions → tombstones → offline-first
  
Chapter 4: NETWORK (how ops travel)
  Polling → Long polling → WebSockets → one persistent pipe
  One op in-flight → buffer → reconnection sync

Chapter 5: STORAGE (where docs live)
  Op Log (append-only ledger) + Snapshots (speed)
  Sharding (consistent hashing) + Replication (Raft)

Chapter 6: SERVERS (handling millions of connections)
  Stateless collab servers + Redis pub/sub
  Autoscaling + thundering herd prevention

Chapter 7: PERMISSIONS (who can do what)
  ACL table + random share tokens + active invalidation
  Per-op auth checks + audit logs

Chapter 8: VERSION HISTORY (time travel)
  Op Log = free version history
  Restore = new ops, never mutate history
  Tiered retention policy

Chapter 9: GLOBAL SCALE (billions of users)
  GeoDNS + CDN + multi-region
  CAP theorem: CP for writes, AP for reads
  Viral doc → tiered read replicas

Chapter 10: THIS CHAPTER
  Requirements first → architecture → tradeoffs → numbers
```

---

## Final Interview Checklist ✅

Before you walk into the interview, make sure you can explain:

```
CORE CONCEPTS:
□ Why operations instead of full document saves
□ How OT transforms conflicting ops (with example)
□ Why CRDTs use unique IDs instead of positions
□ Why WebSockets over HTTP polling
□ Why only one op in-flight at a time

STORAGE:
□ Op Log vs Snapshot — purpose of each
□ Hash-based sharding vs range-based
□ Why consistent hashing for resharding
□ Raft consensus — why majority acknowledgment

SCALE:
□ CAP theorem — what it means, what Google Docs picks
□ GeoDNS — how it routes users geographically
□ Why pub/sub beats sticky sessions
□ Thundering herd — what it is and how to prevent it

FEATURES:
□ How share links work (random tokens, why)
□ Why permission checks happen per-op not just on open
□ How restore works without mutating history
□ Why comment anchors use char IDs not positions

TRADEOFFS (always mention):
□ OT vs CRDT
□ Consistency vs Availability per data type
□ Op Log vs Snapshots (both needed, why)
□ Monolith vs Microservices (scale independently)
```

---

## You're Ready. 🚀

You now understand Google Docs not as a magic black box but as a collection of **elegant solutions to hard problems**:

```
Hard Problem              Elegant Solution
────────────────────────────────────────────────
Concurrent edits          Operational Transformation
Offline sync              CRDT / buffered ops
Real-time at scale        WebSockets + pub/sub
Billion docs, fast        Consistent hash sharding
Never lose data           Append-only op log
Instant revocation        Active cache invalidation
Any version, any time     Snapshots + op replay
Global low latency        GeoDNS + private backbone
Viral documents           Tiered read replicas
```

Every design decision tells a story. In your interview, **tell that story** — not just the answer, but why you got there.

That's what makes an answer unforgettable. 🎯
