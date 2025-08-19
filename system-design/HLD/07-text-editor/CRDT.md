# Step 1 — What is a CRDT and why we care (short, simple, with a few examples)

## Quick, friendly definition

**CRDT (Conflict-Free Replicated Data Type)** — a data structure you can replicate on many devices (replicas) that can be updated independently, and when replicas sync, they **always converge to the same value** without needing central coordination or complex conflict resolution. That property is essential for collaborative real-time editors: users can edit offline or concurrently and the document will deterministically converge.

Key idea: design data types so that merging independent updates is *commutative, associative, and idempotent*. Those math properties guarantee convergence no matter how updates arrive.

---

## Why CRDTs for real-time text editors?

* No central server required to keep state consistent (helps offline-first and P2P).
* Merges are deterministic; replicas eventually agree even with network partitions.
* Simpler correctness reasoning than some alternatives (but often trade memory/time for simplicity).

Real editors need to handle *concurrent inserts, deletes, cursor positions,* etc. Sequence CRDTs (special CRDTs for ordered collections) are what we use for text.

---

## Two broad CRDT families (high level)

1. **State-based (CvRDT)** — each replica keeps a local state; periodically replicas send their whole state (or *delta*) to others; receiver *merges* states using a deterministic `join` function. Merge must be monotonic and join forms a semilattice (element-wise max, set union, etc.).
2. **Operation-based (CmRDT / op-based)** — replicas send *operations* (e.g., `insert(id,x)`) to others; operations must be delivered reliably (often with causal ordering). The operations themselves are designed to commute.

There are also **delta-CRDTs** — state-based but send compact deltas, reducing bandwidth.

---

## Core properties every CRDT targets

* **Convergence:** all replicas that applied the same set of updates (in any order) reach same state.
* **Intention preservation:** operations should feel intuitive to users (e.g., inserting characters stays near where user expected).
* **Availability:** replicas accept local updates even while partitioned.
* **Monotonicity / idempotence / commutativity:** building blocks of safe merges.

---

## Start with tiny, concrete CRDTs (concepts you’ll reuse)

### Example 1 — G-Counter (grow-only counter, state-based)

Each replica keeps a vector of per-replica counts.
Replica `A` stores `[a, b, c]` (one slot per replica). Increment only increases your slot. Merge = element-wise **max**.

**Example:**

* Replica A (2 replicas total A and B): A increments twice → local state `[2,0]`.
* Replica B increments once → local state `[0,1]`.
* Merge (element-wise max): `[max(2,0), max(0,1)] = [2,1]`.
* Total value = sum = `2 + 1 = 3`.

(Arithmetic shown: A: 2, B:1 → merge \[2,1] → sum 3.)

Why useful: simple, convergent, idempotent (merging same state twice does nothing).

---

### Example 2 — PN-Counter (positive/negative)

Model increments and decrements by two G-counters: `P` and `N`. Value = sum(P) − sum(N). Decrement increments your `N` slot. Merge: element-wise max for both P and N.

**Example:**

* A increments once: `P_A = [1,0]`, `N_A=[0,0]` → value = 1.
* B decrements once: `P_B=[0,0]`, `N_B=[0,1]` → value = −1 locally.
* Merge P: `[1,0]`, N: `[0,1]` → final value = (1+0) − (0+1) = 0.

---

### Example 3 — Grow-only Set (G-Set)

You can only add; merge = set union. Converges trivially.

Limitation: can't remove elements.

---

### Example 4 — Two-Phase Set (2P-Set)

Keep two G-sets: `A` (adds) and `R` (removes). Element in `R` is considered removed; once removed you cannot re-add (because `R` persists). Merge = union on both. Simple but limited (no re-add).

---

### Example 5 — LWW-Element-Set (Last-Write-Wins)

Store for each element a timestamp and whether last op was add/remove. On merge pick the entry with the latest timestamp. Useful sometimes but relies on clocks (or logical clocks) and gives up some intention preservation.

---

## But text editors need **ordered sequences** — that’s where sequence CRDTs come in

Text = ordered sequence of characters or elements. The tricky parts:

* **Preserve order** across replicas
* Handle **concurrent inserts at same “index”**
* Handle **deletes** (and keep replicas convergent)

Two common flavors for sequence CRDTs:

1. **Identifier-based / position-identifier CRDTs** (Logoot, Treedoc, LSEQ): each character has a globally unique *position identifier* that defines total order. Inserts generate identifiers between neighboring identifiers. Order = sort by identifier.
2. **Linked-list style CRDTs** (WOOT, RGA): elements link to neighbors (prev pointer). Concurrent inserts at same previous element are ordered by identifiers (e.g., site ID + counter).

Both approaches assign **stable IDs** to characters so concurrent operations can be merged deterministically.

---

## Short illustrative example for sequences (RGA-style, simplified)

Start: empty doc `[]`.

Two users A and B both insert characters at the start concurrently:

* A inserts `'X'` after head, creates element `idA:1` → doc locally: `head → (idA:1,'X')`.
* B inserts `'Y'` after head concurrently, creates `idB:1` → doc locally: `head → (idB:1,'Y')`.

When they sync, both elements exist with distinct ids. Deterministic order chosen by comparing ids (e.g., compare site IDs or global counters). Suppose compare `(counter, site)` lexicographically: `(1,A) < (1,B)`, so final order `X Y`.

If A had instead inserted at head twice (`'X'` then `'Z'`), each insertion gets a unique id and linked position.

Deletes are implemented as tombstones: mark element as deleted but keep its id so inserts that reference it still work.

---

## Concurrent insert at *same position* — the crux

If two users insert characters at the same logical index, CRDTs avoid ambiguous “index numbers” by using unique position identifiers or tie-breaking rules based on replica ID / counter / timestamp. Result is deterministic and consistent across replicas. The **user intention** (relative proximity) is mostly preserved; exact interleaving depends on the identifier tie-break rule.

---

## Why tombstones? and why they’re a problem

* Many CRDTs keep deleted elements as **tombstones** so merges referencing old ids are still correct.
* Problem: tombstones accumulate (memory blowup). Real systems implement garbage collection that removes tombstones once it’s safe (everyone has seen the removal).
* Safe removal usually requires some **causal stable point** detection (vector clocks, version vectors, or anti-entropy acknowledgements).

---

## Operation-based vs State-based for sequences

* **Op-based** sequence CRDTs send *insert(id, value, prev-id)* and *delete(id)* operations. Efficient bandwidth but requires reliable *causal* delivery or metadata to preserve semantics.
* **State-based** would merge whole structure (or deltas) — simpler to reason about, but may be heavier. Delta-CRDTs are compromise: send small state fragments.

---

## Quick comparison to OT (Operational Transformation)

* **OT** transforms operations against concurrent operations (transform functions), preserving intention. Historically used by Google Docs.
* **CRDT** designs operations/data to commute without transforms.
  Tradeoffs:
* OT can be more bandwidth/space efficient and simpler for low-level sequences, but implementation is delicate (complex transforms, edge cases).
* CRDTs are conceptually simpler and fit decentralised/offline scenarios; sequence CRDTs often have higher metadata overhead (IDs, tombstones).

---

## Small checklist of scenarios we’ll cover next (pick which you want first):

* Basic building blocks (counters, sets) — we already started.
* **Sequence CRDTs deep dive**: Logoot / Treedoc / LSEQ / RGA / WOOT — how each assigns positions and resolves conflicts.
* **Delete semantics** and tombstone GC (how to garbage-collect safely).
* **Causality & clocks**: vector clocks, version vectors, dotted version vectors (for op-ordering).
* **Delta-CRDTs** and network efficiency techniques.
* **Practical engineering**: memory/time complexity, indexing, cursor mapping, undo/redo, integration into an editor.
* **Comparison with OT** (detailed), and real-world libs (Automerge, Yjs, etc.) — how they implement these ideas.
* **Implementation sketch**: simple op-based CRDT for a toy text editor (pseudocode + message flows).

---

### Quick summary of Step 1

* CRDTs let replicas independently update and later converge deterministically.
* Two families: state-based and op-based (and delta).
* For text you need *sequence CRDTs* that give each character a stable, globally orderable identifier.
* Tombstones keep deletes safe but need GC.
* Next steps can dive into *specific sequence algorithms* (Logoot/Treedoc/LSEQ/RGA/WOOT), or into causality, or into a hands-on implementation.

---

# Step 2 — Sequence CRDTs (for ordered text)

A text document = sequence of characters (or tokens).
Problem: Two users can concurrently insert/delete at the *same logical position*. Normal array indices break, because each replica may see a different order.

👉 Solution: Instead of using “index numbers,” CRDTs assign **unique, stable position identifiers** to each character. Order is determined by sorting identifiers, not by insertion time.

---

## Key Idea

* Each inserted character = `(id, value)`.
* `id` is globally unique and totally orderable.
* When replicas merge, they just **sort by id**.
* Deletion = mark `id` as deleted (tombstone).

---

## Example 1 — Logoot (identifier = path of numbers)

Each character gets a **position identifier**, which is a *list of numbers*.

### How insertion works:

* Start doc: `[ ]`.
* Insert "A" at start → assign id `[5]` → doc: `[(id=[5], 'A')]`.
* Insert "B" after "A" → pick number between `[5]` and `[∞]` → e.g. `[7]`.
* Now doc order: `[5:A, 7:B]`.

### Concurrent insert at same position:

* A inserts "X" after "A" → picks `[6]`.
* B inserts "Y" after "A" → also needs an id between `[5]` and `[7]`. Maybe it picks `[5,3]` (a *path extension*).
* Sorting rule: `[5,3] < [6] < [7]`.
* Final merged order: `A Y X B`.
  Deterministic everywhere, no conflict.

---

## Example 2 — Treedoc / LSEQ

* Similar to Logoot, but uses a *binary tree* of positions.
* Each position = path down tree (e.g., left=0, right=1).
* When inserting between two nodes, choose midpoint path.
* If conflict, extend the path (like decimals between 0.5 and 1).

👉 Advantage: prevents infinite growth of identifiers by balancing tree.
👉 Challenge: metadata size can still grow with many edits at the same spot.

---

## Example 3 — RGA (Replicated Growable Array, linked-list style)

Instead of paths, RGA uses *links*:

* Each element stores a pointer to **previous id**.
* Order = traverse from head following links.
* Concurrent inserts at same spot → tie-break using `(counter, siteID)`.

**Example:**

* Doc = `[ ]`.
* A inserts "A" after head → element `(idA1, value='A', prev=head)`.
* B inserts "B" after head → element `(idB1, 'B', prev=head)`.

When merged:

* Both have same `prev=head`.
* Tie-break rule (say compare `(counter, siteID)`): If `(1,A) < (1,B)`, order = `A B`.

If C inserts "C" after A, it references `prev=idA1`.
Traversal yields `A C B` (because C comes after A).
Deterministic and convergent.

---

## Example 4 — WOOT (WithOut Operational Transform)

* Also linked-list style, but keeps **both left and right neighbor IDs** for each element.
* Order decided by recursive rules resolving conflicts when multiple elements claim same neighbors.
* Conceptually neat but heavier in metadata.

---

## Inserts vs Deletes

* **Insert**: generate new `(id, value)` with reference to position or path.
* **Delete**: don’t remove physically — mark id as deleted.
  *Why?* If another replica still references it (e.g., insert after deleted id), order still works.
* Later, garbage collection can physically remove tombstones once all replicas have seen the delete.

---

## Cursor Mapping

CRDTs work on IDs, but users think in *indices*.
So the editor needs:

* A mapping: `index ↔ id`.
* When rendering, show only non-deleted elements sorted by id.
* Cursor positions move by following id neighbors.

---

## Quick Example — Concurrent Inserts & Deletes

Let’s simulate:

1. Doc starts empty: `[]`.
2. A inserts "H" (idA1 after head).
   → `H`
3. B concurrently inserts "X" at start (idB1 after head).
   → `X`
4. Merge: both have prev=head. Tie-break: `(A,B)` order.
   → `H X`
5. C inserts "Y" after H.
   → `H Y X`
6. A deletes "H". Mark idA1 deleted.
   → `[deleted H] Y X`
7. Rendering (ignore tombstones): `Y X`.

All replicas eventually converge to `Y X`.

---

## Summary of Sequence CRDTs

* **Logoot / LSEQ / Treedoc:** position identifiers (like decimals or tree paths).
* **RGA / WOOT:** linked-list with unique IDs and neighbor references.
* All ensure:

  * Concurrent inserts converge.
  * Deletes preserved until safe to garbage-collect.
  * Deterministic order everywhere.

---

Perfect 👌 Let’s move on.

---

# Step 3 — Deletes & Tombstone Garbage Collection (GC)

We’ve seen that in CRDTs for text:

* Inserts add a new `(id, value)`.
* Deletes don’t physically remove the element — they mark it as **tombstoned** (deleted but still remembered).

This is crucial because:

* Other replicas may still need the deleted element’s ID to resolve order (e.g., “insert after id=H” even if H is deleted locally).
* If we removed it immediately, remote inserts could break.

But tombstones grow forever → memory bloat.
So, we need **safe garbage collection (GC)**.

---

## 🔹 Why can’t we just delete immediately?

Imagine:

1. A deletes “X” and removes it from memory.
2. B, who hasn’t seen the delete yet, inserts “Y” after “X”.
3. When A and B sync, A doesn’t know what “insert after X” means → order breaks.

👉 That’s why tombstones are kept until we are *sure all replicas* know about the deletion.

---

## 🔹 How do we know it’s safe to GC?

We need a notion of **causal stability**:

> An operation (like a delete) is stable if *every replica has seen it*.

If stable, the tombstone can be safely removed.

### Ways to track this:

1. **Version vectors (vector clocks)**

   * Each replica tracks “what operations it has seen from every other replica.”
   * If all replicas’ version vectors show they’ve seen `delete(idX)`, we can GC tombstone for `idX`.

2. **Centralized server (practical optimization)**

   * Instead of full peer-to-peer, many systems have a server that tracks “everyone has acknowledged delete.”
   * Once confirmed, server broadcasts a **GC event**.

3. **Time-based approximation (lazy GC)**

   * Some systems (less strict) assume if enough time passes and all peers are likely synced, old tombstones can be dropped.
   * Risk: if a slow replica reconnects after a long time, it might fail.

---

## 🔹 Example Walkthrough

Say we use vector clocks.

* Replicas: A, B, C.
* A deletes `idX` → makes tombstone.
* A’s clock = `{A:5, B:3, C:2}`.
* It sends delete to others.
* B gets it → merges. B’s clock updates → `{A:5, B:4, C:2}`.
* C gets it later → `{A:5, B:3, C:3}`.

Now, A can only GC `idX` when it knows:

* B’s clock ≥ 5 (saw delete).
* C’s clock ≥ 5 (saw delete).

Only then → safe to remove tombstone.

---

## 🔹 Practical Optimizations

* **Automerge (CRDT lib):** keeps tombstones forever (safe but memory heavy).
* **Yjs (CRDT lib):** aggressively compacts tombstones by merging operations once causally stable.
* **Delta-CRDTs:** only keep minimal metadata and prune aggressively.

---

## 🔹 Tradeoffs

* **Correctness vs Memory:** keeping tombstones = safe but heavy; GC = tricky but efficient.
* **Distributed vs Centralized:** decentralized GC is harder (vector clocks, acknowledgements); centralized service can simplify.

---

✅ At this point you understand:

* Why tombstones exist.
* Why we can’t just delete.
* How CRDTs know when to safely GC (causal stability).
* How real-world systems balance safety vs memory.

---

Alright 👍 Let’s go into

---

# Step 4 — Causality & Clocks in CRDTs

We now know inserts/deletes + tombstones + GC.
But to reason safely about **when ops happened**, and whether all replicas have seen them, we need **causal tracking**.

---

## 🔹 What does “causality” mean here?

Two operations can be:

* **Causally related**: one happened before the other (must be applied in that order).
* **Concurrent**: happened independently (can be applied in any order, CRDT ensures they converge).

**Example:**

* A inserts `"A"`. Then deletes it.
  Delete is *causally after* insert — must apply in that order.
* B concurrently inserts `"B"`. That’s concurrent with A’s insert/delete — order doesn’t matter, only final convergence.

---

## 🔹 Vector Clocks (classic tool)

Each replica keeps a vector of counters, one per replica.

* Increment your counter on every local operation.
* Attach vector to each operation.
* When merging, take element-wise max.

This lets you compare two events:

* If all entries of V1 ≤ V2, then event1 → event2 (causal order).
* If neither dominates, events are concurrent.

**Example with replicas A, B:**

* A inserts “X”: VC = `{A:1, B:0}`.
* B inserts “Y”: VC = `{A:0, B:1}`.
* Neither dominates → concurrent.
* Merge order resolved by CRDT tie-breaker rule.

---

## 🔹 Dotted Version Vectors (DVVs)

Problem: vector clocks can be big (O(#replicas)) and don’t point to a single event.

**DVVs = Vector Clock + one “dot” (the new event).**
Example:

* Op: (dot = (A,5), context = {A:4, B:3}).
* Meaning: “this is A’s 5th op, and I know all ops from A up to 4 and from B up to 3.”

Much more compact, and very useful in GC (detecting stability of deletes).

---

## 🔹 Why editors need causality?

1. **To ensure deletes are applied correctly.**
   If delete arrives before its insert, causality tells us to wait until insert is applied.
2. **For GC of tombstones.**
   You can only GC when you know all replicas have seen the delete (vector clocks help confirm this).
3. **For offline edits.**
   If someone edits offline and rejoins, causality ensures ops integrate in the right order.

---

## 🔹 Example: Causal Ordering in Action

Replica A:

1. Insert “H” (op1) → VC `{A:1,B:0}`.
2. Delete “H” (op2) → VC `{A:2,B:0}`.

Replica B:

1. Insert “X” concurrently (op3) → VC `{A:0,B:1}`.

When merged:

* Op1 < Op2 (delete after insert).
* Op3 concurrent with both.
* Final doc = `X` (since H was inserted then deleted, X stays).

---

## 🔹 How real CRDT libs handle it

* **Yjs**: doesn’t explicitly use vector clocks; instead, uses Lamport timestamps + client IDs, and merges causality via a shared “delete set” and structural ordering.
* **Automerge**: uses Lamport timestamps and maintains a log of ops with causal dependencies.
* **Riak CRDTs**: use dotted version vectors (DVVs) for causal tracking in sets/maps/counters.

---

✅ By now, you know:

* What causality means in CRDTs.
* How vector clocks / DVVs track it.
* Why text editors need causal order for deletes, GC, and merges.
* How practical libraries solve it.

---

Perfect, let’s roll up our sleeves 💻

---

# Step 5 — Implementation Sketch: CRDT Text Editor

We’ll sketch a **toy operation-based CRDT** (simplified RGA style).
Enough detail so you can imagine building a prototype, without drowning in edge cases.

---

## 🔹 Data Model

Each character = a **CRDT element**:

```text
Element = {
  id: (siteID, counter),   // globally unique identifier
  value: char,             // the character itself
  prev: id,                // the id of element before it
  deleted: bool            // tombstone flag
}
```

Each replica maintains:

* `document`: a map from `id → Element`.
* `order`: a linked structure (like head → next).
* `localCounter`: to generate new ids.
* `siteID`: unique id of replica (e.g., user/device id).

---

## 🔹 Insert Operation

```pseudocode
function localInsert(char, prevId):
    id = (siteID, localCounter++)
    element = { id, value=char, prev=prevId, deleted=false }
    applyInsert(element)
    broadcast("insert", element)

function applyInsert(element):
    if element.id not in document:
        document[element.id] = element
        // place it in order: after element.prev
        insertAfter(element.prev, element)
```

**Key Idea:**

* Each insert says: “place this character after `prevId`.”
* If multiple inserts happen after same prev → order by `(counter, siteID)`.

---

## 🔹 Delete Operation

```pseudocode
function localDelete(id):
    if id in document and not document[id].deleted:
        document[id].deleted = true
        broadcast("delete", id)

function applyDelete(id):
    if id in document:
        document[id].deleted = true
```

---

## 🔹 Merging Operations

* **Insert**: Add element if not seen; insert into correct position by scanning neighbors.
* **Delete**: Mark element as deleted.
* Idempotent: receiving same op twice has no effect.
* Commutative: order of applying insert/delete doesn’t matter.

---

## 🔹 Rendering Document

```pseudocode
function render():
    result = []
    traverse from head following order:
        if element.deleted == false:
            result.append(element.value)
    return join(result)
```

---

## 🔹 Example Walkthrough

**Start:**
Doc = `[]`.

Replica A:

```
localInsert("H", head) → id=(A,1)  
Doc = H
```

Replica B:

```
localInsert("X", head) → id=(B,1)  
Doc = X
```

Now they sync:
Both ops reference head.
Tie-break `(counter, siteID)`:

* `(A,1)` < `(B,1)` → order = `H X`.

Now A deletes H:

```
localDelete((A,1))
```

Sync:

* (A,1) is tombstoned.
* Rendering ignores it.
* Final doc = `X`.

---

## 🔹 Pseudocode for Sync

```pseudocode
onReceive(operation):
    if operation.type == "insert":
        applyInsert(operation.element)
    if operation.type == "delete":
        applyDelete(operation.id)
```

Merging is simple — just apply operations in any order.

---

## 🔹 Limitations of This Sketch

* Keeps tombstones forever (needs GC with causal tracking).
* Traversal can be slow if doc is large (practical systems use balanced trees or skip lists for efficient indexing).
* Cursor positions not covered (would need mapping index ↔ id).
* Undo/redo not included (requires history ops).

---

✅ Now you have a **barebones CRDT text editor design**:

* Unique ids per char.
* Inserts reference a previous id.
* Deletes tombstone elements.
* Merge = apply ops in any order.
* Render = traverse + filter tombstones.

---
