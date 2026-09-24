# Step 1 — What problem does OT actually solve?

## The setting

* Many users edit **the same text** at the **same time**.
* Edits travel over the network with delays and can arrive **out of order**.
* We want **everyone’s document to end up identical** (convergence) **and** each person’s change should **keep its original intention** (intention preservation).

## The model (simple, practical)

* Think of the document as a sequence of characters.
* We’ll use **0-based indexing**.
* Two primitive operations:

  * **Insert(p, s)** — insert string `s` **before** index `p`.
  * **Delete(p, n)** — delete `n` characters starting at index `p`.

### Quick indexing example

Text: `"HELLO"` (length 5)
Indices: `H(0) E(1) L(2) L(3) O(4)`
Valid insert positions are 0..5:

* Insert(0,"!") → `"!HELLO"`
* Insert(5,"!") → `"HELLO!"` (end)

Deletes:

* Delete(1,2) on `"HELLO"` removes `"EL"` → `"HLO"`

## Why concurrency breaks naive systems

Two users (A and B) start from `"CAT"`.

* A does: **Insert(3,"!")** (append “!”)
* B does: **Delete(1,1)** (remove `"A"`)

If one site applies **A then B**:

1. `"CAT"` → Insert(3,"!") → `"CAT!"`
2. `"CAT!"` → Delete(1,1) → `"CT!"`

If another site applies **B then A**:

1. `"CAT"` → Delete(1,1) → `"CT"`
2. Now try to apply Insert(3,"!") (A’s original indices)

   * But `"CT"` length is 2; index 3 is **invalid**.
   * If you force it to the end, you get `"CT!"`, but that was a **guess**, not a rule.

So, without special handling, different sites may:

* Crash (invalid index),
* Or guess differently → **diverge**.

## The key idea of OT (high level)

When two operations are **concurrent** (created without knowledge of each other), we **transform** one operation **against** the other so that:

* Everyone applies **the same pair** of transformed operations (possibly in different orders),
* The final documents **converge**,
* Each operation still **does what its author meant**, despite shifts caused by the other edit.

You can think of a function:

```
T(opA, opB) -> opA'
```

> “Adjust opA so it has the right coordinates if opB already happened.”

Often we also compute the symmetric transform for the other direction:

```
T(opB, opA) -> opB'
```

The transformed ops `opA'` and `opB'` are what get applied at the opposite sites, ensuring everyone lands on the **same** final text.

## What “intention” means (intuitively)

* If I inserted “!” **at the end** of the text I saw, I still want my “!” to end up at the end **of that version**, even if someone else deleted characters and changed the length before my edit arrives elsewhere.
* If I deleted the **exact range I saw** (say, the word “blue” at positions 10..14), I still want that **same content** gone, even if concurrent inserts shifted indices.

## Minimal mental picture

* Each client sends local ops immediately (for snappy typing).
* The server relays ops to others.
* On receive, a client **transforms** the incoming op against the set of **concurrent** ops it has already executed, so indices line up.
* Result: same final text everywhere.

## Tiny exercise (for intuition)

Starting text: `"ABA"`

* A does: Insert(1,"X")  (intends to put X before the first `B`)
* B does: Delete(0,1)    (intends to remove the first `A`)

**Question:** If B’s delete is applied first, where should A’s insert go after transformation to preserve A’s intention?

*(Think: A wanted `"AXBA"`. After B deletes the first `A`, the text becomes `"BA"`. The position that used to be 1 is now 0. So A’s op should transform to Insert(0,"X"), yielding `"XBA"` — which still places X before the first `B`.)*

---

# Step 2 — The OT pipeline end-to-end (what actually happens in a real editor)

## 2.1 The cast of characters

* **Client (Site)**: each user’s editor tab/app.
* **Server (Relay/Sequencer)**: accepts ops from clients, assigns them a global order, relays to everyone.

> We’ll use the very common **central relay** model (simpler to reason about than pure P2P).

---

## 2.2 The minimal metadata every op must carry

Each operation has:

* `type`: `Insert` or `Delete`
* `position`, `string`/`count`: where/how much
* `siteId`: who created it
* `baseRevision` (or `context`): **the server revision number the author saw** when they created the op

Example op:

```
Insert(pos=3, "!", siteId=A, baseRevision=12)
```

> Means: “I (A) typed this when my document reflected **all** server ops up to revision 12.”

Why we need `baseRevision`:

* It lets others decide whether your op is **concurrent** with theirs, or **comes after** theirs (causal order).
* It tells the server what you knew at send time so it can transform correctly.

---

## 2.3 What the server keeps

* A **log** of accepted ops: `log = [op#1, op#2, ..., op#R]` (R = current revision)
* Optionally, the current **document state** (to apply ops easily)
* For performance: sometimes a cache/index, but conceptually just a list.

---

## 2.4 What each client keeps

* The **document** it shows.
* Its **known server revision** `localRevision` (how far into the server log it has integrated).
* A **buffer of unacknowledged local ops** (aka **outstanding ops**):

  ```
  outstanding = [myOp1, myOp2, ...]
  ```
* (Optional but common) a **shadow doc** mirroring the last-ACKed state.

---

## 2.5 The golden rule: transform *only* against concurrent ops

* **Causally before**: If op X’s revision ≤ your `baseRevision`, you already saw X when you created your op → **do not transform against it**.
* **Causally after**: Ops that came **after** your `baseRevision` are **concurrent** from your perspective → **must transform**.

---

## 2.6 The happy path (single site)

1. You type:

   * Create op with `baseRevision = localRevision`.
   * **Apply locally immediately** (for instant typing).
   * Push to `outstanding`, send to server.

2. Server receives:

   * Transform your op against any ops in its log with index `> baseRevision`.
   * Assign next revision number, append to log.
   * Broadcast the **transformed** (canonical) op to everyone (including you).

3. You (the author) receive the **ack** (the broadcast of your op in canonical form):

   * Pop the matching op from `outstanding`.
   * Update `localRevision++`. (Also apply the server’s version if it differs—rare if server did extra transforms.)

---

## 2.7 The interesting path (two sites, concurrent)

Let’s walk a concrete timeline.

**Initial text**: `"CAT"`
**Server revision**: 0

**Site A** (localRevision=0) does:

```
a1 = Insert(3, "!", baseRevision=0, siteId=A)
```

A applies `a1` locally → shows `"CAT!"`, `outstanding=[a1]`.

**Site B** (localRevision=0) does:

```
b1 = Delete(1, 1, baseRevision=0, siteId=B)   // delete the "A"
```

B applies `b1` locally → shows `"CT"`, `outstanding=[b1]`.

Both ops go to the server (order of arrival can vary). Suppose **server gets b1 first**.

### At the server

* **Receive `b1`**:

  * Transform against ops after rev 0: none.
  * Accept as **rev 1**. Log: `[b1]`. Doc: `"CT"`.
  * Broadcast `b1@rev1`.

* **Receive `a1`**:

  * `a1.baseRevision=0`, server log has `[b1]` after 0 → **concurrent**.
  * Transform `a1` **against** `b1`:

    * Inserting at 3 into `"CAT"` vs deleting index 1 length 1:
    * After delete, the string shrank by 1; an insertion point after the deletion shifts **left** by 1.
    * So `a1' = Insert(2, "!")`.
  * Accept `a1'` as **rev 2**. Log: `[b1, a1']`. Doc: `"CT!"`.
  * Broadcast `a1'@rev2`.

### At Site A (author of a1)

* Receives `b1@rev1` (remote op arrives before ack of own op):

  * A currently shows `"CAT!"` with `outstanding=[a1]`.
  * **Incoming op must be transformed against A’s outstanding ops** to preserve A’s intention locally:

    * Transform `b1` through `a1`: deleting at 1 when a local insert at 3 already applied.
    * Delete before the insert’s position → **no change** to `b1`.
  * Apply transformed `b1` to A’s doc `"CAT!"` → delete the `"A"` → `"CT!"`.
  * Update `localRevision=1`.

* Receives `a1'@rev2` (ack):

  * Matches `a1` in `outstanding`, pop it. `outstanding=[]`.
  * Typically no extra apply needed (doc already consistent).
  * `localRevision=2`. A shows `"CT!"`.

### At Site B (author of b1)

* Receives `b1@rev1` (ack): pop from `outstanding`, doc stays `"CT"`, `localRevision=1`.
* Receives `a1'@rev2`:

  * No outstanding ops, so simply apply `Insert(2,"!")` to `"CT"` → `"CT!"`.
  * `localRevision=2`.

**Everyone converges to `"CT!"`.**

> Notice two places where **transform happens**:
>
> * **Server**: transforms the incoming client op against the server’s unseen suffix (ops after its base).
> * **Clients** (on receive): transform incoming broadcast ops against **their own outstanding** local ops.

---

## 2.8 Where transforms happen (send vs receive)

* **On send (client → server)**:

  * Many systems **don’t transform on the client when sending**; they just attach `baseRevision` and send.
  * The **server** is the source of truth: it transforms the op against the log tail and assigns a revision.

* **On receive (server → clients)**:

  * **Everyone (including the author)** may need to transform the incoming op against **their own outstanding** local ops to preserve local intention.
  * After that, apply.

This two-sided transforming is what keeps both **convergence** and **intention preservation**.

---

## 2.9 Causality and concurrency (the test)

Two ops `x` and `y` are **concurrent** iff:

* `x.baseRevision < rev(y)` **and**
* `y.baseRevision < rev(x)`
  …i.e., neither was created with knowledge of the other.

In practice:

* The **server** knows `rev(y)` when it processes `x` (and vice versa).
* **Clients** use their `localRevision` and the `baseRevision` carried by ops to decide which outstanding ops to transform through.

> You’ll often see “**causality preservation**” listed as a key property: the system must deliver ops in an order that doesn’t violate “happened-before” (Lamport). The server’s sequencing and clients’ revision counters achieve this.

---

## 2.10 A tiny ASCII sequence diagram

```
Site A                     Server                      Site B
------                     ------                      ------
(localRev=0)                                         (localRev=0)
a1=Ins(3,"!",base=0) ->
apply locally -> "CAT!" (outstanding=[a1])
                          <- b1=Del(1,1,base=0) from B
                       accept as rev1; broadcast b1
<- b1@rev1 (from server)
transform b1 through [a1] -> b1 (unchanged)
apply -> "CT!" ; localRev=1
a1 sent earlier --------->
                       transform a1 vs [b1] -> a1'=Ins(2,"!")
                       accept as rev2; broadcast a1'
<- a1'@rev2 (ack)
pop outstanding; localRev=2
                                                    receive b1@rev1 (ack)
                                                    pop; localRev=1
                                                    receive a1'@rev2
                                                    apply -> "CT!" ; localRev=2
```

---

## 2.11 Pseudocode (client)

```pseudo
onLocalEdit(op):
  op.base = localRevision
  apply(doc, op)                 // immediate UX
  outstanding.push(op)
  sendToServer(op)

onServerOp(incoming):            // incoming is canonical + has revision
  // Transform against outstanding local ops
  op = incoming
  for each myOp in outstanding:
      op = T(op, myOp)           // shift incoming to account for my local-but-unacked edits
  apply(doc, op)
  localRevision = incoming.revision

onAck(incoming):                 // same handler as onServerOp
  // When the incoming op corresponds to my own head outstanding op:
  outstanding.popFront()
```

---

## 2.12 Pseudocode (server)

```pseudo
onClientOp(op):
  // op.base says up to which revision the client had seen
  transformed = op
  for i in (op.base+1 .. currentRevision):
      transformed = T(transformed, log[i])  // shift to the log tail
  currentRevision += 1
  transformed.revision = currentRevision
  log.append(transformed)
  apply(serverDoc, transformed)
  broadcast(transformed)
```

---

## 2.13 Common pitfalls the pipeline prevents

* **Invalid indices** on arrival → avoided because we always transform against the right suffix.
* **Divergence** between sites → avoided because all sites apply the **same canonical sequence** after doing the same transforms against their own outstanding set.
* **Intention breakage** (your insertion becomes mis-placed) → minimized because we transform incoming ops through your local outstanding edits before applying.

---

## 2.14 Quick practice (mentally simulate)

Start: `"door"` (indices d0 o1 o2 r3)

* A inserts `"k"` at pos 0 (before `d`): `a1=Ins(0,"k",base=0)` → A shows `"kdoor"`.
* B deletes 1 char at pos 3 (the second `o`): `b1=Del(3,1,base=0)` → B shows `"dor"`.

Server order: `a1` first (rev1), `b1` second (rev2).

* At server, when `b1` arrives: transform delete(3,1) against prior insert(0,"k") → index shifts +1 → delete(4,1). Final doc `"kdor"`.

Now simulate each client receiving, remembering to transform incoming ops through their **own outstanding** list.

---

Step 3: the concrete transform rules. I’ll show you a clear, conventional set of `T(op, other)` rules you can implement, give lots of tiny examples, explain tie-breakers and overlapping deletes, and finish with compact pseudocode you can copy.

I’ll use this notation:

* `Ins(p, s, site)` = insert string `s` before index `p`, created at `site`.
* `Del(p, n, site)` = delete `n` characters starting at index `p`, created at `site`.
* `T(op, other)` means “transform `op` so it is correct *if* `other` has already been applied.”

Key design choices we’ll use (common, simple, and workable):

1. Use 0-based positions.
2. When two Inserts have the **same position**, break ties with a **total ordering** on site IDs (or a timestamp + site). We'll assume `siteId` comparison: smaller `siteId` wins (comes first).
3. When an Insert happens **inside** the range another client intended to Delete concurrently, we will assume the Delete will also remove that inserted text — i.e., the Delete **expands** to include the inserted text. (This is a common, deterministic choice; other choices are possible but must be consistent.)
4. All transforms are position + length arithmetic (no content comparison).

---

# 1) Insert vs Insert — `T(Ins(i,s1), Ins(j,s2))`

Goal: shift `i` if the other insert comes before or is tied-and-prioritized.

Rules:

* If `j < i` then `i' = i + len(s2)` (other insert before yours shifts you right).
* If `j > i` then `i' = i` (other insert after has no effect).
* If `j == i` then use tie-break:

  * If `other.siteId < op.siteId` (other wins) → `i' = i + len(s2)`
  * Else → `i' = i` (your insert stays first)

Examples:

1. `Ins(3,"X")` transformed against `Ins(1,"AB")` → `j=1 < i=3` → `i' = 3 + 2 = 5` → `Ins(5,"X")`.
2. `Ins(2,"x", site=B)` vs `Ins(2,"Y", site=A)` with `A < B`:

   * Since `j == i` and `other.siteId = A < B`, other goes first → `i' = 2 + len("Y") = 3` → your insert becomes `Ins(3,"x")`. Final order at a site that applied other first will be `"Y" then "x"` at that index.

Intuition: an insert before you pushes your index right. If both inserted at same place, the predetermined ordering decides which lands first and which shifts.

---

# 2) Insert vs Delete — `T(Ins(i,s), Del(j,n))`

We transform an **Insert** `ins` against a **Delete** `del` that already removed `n` chars at `j`.

Rules:

* If `j >= i` → deletion starts at/after your insert point ⇒ `i' = i` (delete is to the right; no effect on insertion position).
* If `j < i`:

  * Let `shift = min(n, i - j)` (how many deleted chars are before your insert).
  * `i' = i - shift`.

(If the deletion overlaps where you would have inserted exactly at end of deleted region, the shift removes only the chars before your point.)

Examples:

1. `Ins(5,"A")` vs `Del(2,2)`:

   * `j=2 < i=5`, `shift = min(2, 5-2=3)=2`, so `i' = 5-2 = 3`.
   * Your insert becomes `Ins(3,"A")`.

2. `Ins(2,"z")` vs `Del(2,3)`:

   * `j=2`, `i=2` → `j >= i` so `i' = 2` (no change). An insertion before index 2 or exactly at 2 is considered before the deletion in this transform.

Intuition: deleting some characters before your insert pulls your insert position left by how many characters were deleted before your position.

---

# 3) Delete vs Insert — `T(Del(i,n), Ins(j,s))`

Now we transform a **Delete** `del` against an **Insert** `ins` that has already been applied.

Rules (following choice #3 above — deletes remove concurrent inserts that fall inside their range):

* If `j <= i`:

  * `i' = i + len(s)` (the insert is before the start of your delete, so your delete start shifts right by the inserted length).
  * `n' = n` (delete length unchanged).
* Else if `j >= i + n`:

  * Insert is after the deletion range → `i' = i`, `n' = n`.
* Else (insert falls **inside** \[i, i+n)):

  * The inserted text lies inside the region you intended to delete. We decide to **also delete** that inserted text (consistent deterministic rule).
  * `i' = i` (start unchanged)
  * `n' = n + len(s)` (delete length expands to include the inserted characters)

Examples:

1. `Del(4,3)` transformed against `Ins(2,"xy")`:

   * `j=2 <= i=4` → `i' = 4 + 2 = 6`, `n'=3`. So `Del(6,3)`.

2. `Del(5,4)` vs `Ins(7,"Z")`:

   * `i=5, n=4 => range [5,9)`. `j=7` is inside.
   * `i' = 5`, `n' = 4 + 1 = 5`. You will now delete inserted `"Z"` as well.

3. `Del(3,2)` vs `Ins(6,"abc")` (insert after deletion): no change.

Intuition: if someone inserted inside the area you planned to delete, you likely intended to delete that area regardless — hence you delete the inserted text too. If the insert is before your delete, you need to shift your start right.

---

# 4) Delete vs Delete — `T(Del(i1,n1), Del(i2,n2))`

This is the most subtle case (overlaps, complete containment, non-overlap). We must produce a deterministic result: after `Del2` has already removed chars, how should `Del1` be adjusted?

Algorithmic approach (robust, covers all subcases):

Let:

* `start1 = i1`, `end1 = i1 + n1` (half-open: `[start1, end1)`)
* `start2 = i2`, `end2 = i2 + n2` (for the existing delete)

We consider cases:

1. **Non-overlapping, other before**: if `end2 <= start1`

   * Other completely before `Del1`. Then `Del1` shifts left by `n2`:
   * `i' = i1 - n2`, `n' = n1`.

2. **Non-overlapping, other after**: if `start2 >= end1`

   * Other completely after `Del1`. No effect:
   * `i' = i1`, `n' = n1`.

3. **Overlapping**: otherwise there is some overlap. Compute the overlap length and remaining region.

Procedure for overlap:

* New start:

  * `i' = min(start1, start2)`? No — we want how the original `Del1` maps after `Del2` removed `n2` characters.
  * Calculate `leftTrim = max(0, start2 - start1)` — how many characters of `Del1` are to the left of `Del2`.
  * Calculate `overlap = max(0, min(end1, end2) - max(start1, start2))` — number of characters both attempted to delete.
  * After `Del2`, the left portion (before start2) remains at same relative position (but shifted left by n2 if start2 <= start1). Simpler is to compute resulting range length:

    * The characters remaining that `Del1` still should delete are `n1 - overlap`.
  * Start position after `Del2`:

    * If `start2 <= start1`: `i' = start1 - n2` (because other removed preceding chars)
    * Else (start2 > start1): `i' = start1` (some trailing part removed, but start didn't shift)
  * New length `n' = max(0, n1 - overlap)`

Edge case: If `overlap >= n1` then `Del1` is entirely removed by `Del2` → becomes a no-op (`n' = 0`).

Clearer consolidated rules:

* If `end2 <= start1`:

  * `i' = i1 - n2`, `n' = n1`
* Else if `start2 >= end1`:

  * `i' = i1`, `n' = n1`
* Else: // overlap

  * `overlap = min(end1, end2) - max(start1, start2)` (positive)
  * `n' = n1 - overlap`
  * If `start2 < start1`:

    * `i' = i1 - (min(n2, start1 - start2))` (i.e., shift start left by chars removed before it)
  * Else:

    * `i' = i1` (start unchanged)

Examples (walk through):

1. `Del1 = Del(10, 5)` (\[10,15)), `Del2 = Del(4,3)` (\[4,7)) → `end2=7 <= start1=10` → case 1:

   * `i' = 10 - 3 = 7`, `n' = 5`. (Del1 shifts left)

2. `Del1 = Del(5,4)` (\[5,9)), `Del2 = Del(8,3)` (\[8,11)) → overlap region = \[8,9) length 1:

   * `overlap = 1`, `n' = 4 - 1 = 3`.
   * `start2 = 8 > start1 = 5` → `i' = 5`.
   * Result `Del(5,3)` — you still delete the left piece and the part after overlap got shifted as a result of other deletion.

3. `Del1 = Del(6,4)` (\[6,10)), `Del2 = Del(5,7)` (\[5,12)) → Del2 fully covers Del1:

   * overlap = min(10,12)-max(6,5) = 4 → `n' = 4 - 4 = 0` → no-op. `Del1` disappears.

4. `Del1 = Del(12,6)` (\[12,18)), `Del2 = Del(9,5)` (\[9,14)):

   * overlap = min(18,14)-max(12,9) = 2
   * `n' = 6 - 2 = 4`
   * `start2=9 < start1=12` → shift `i' = 12 - min(n2, start1-start2) = 12 - min(5,3) = 12 - 3 = 9`
   * Result `Del(9,4)`.

Intuition: anything the other delete already took away should not be attempted again; if that other delete removed chars before your start, your start moves left.

---

# 5) Compact pseudocode for transforms

Below is straightforward, implementable pseudocode for each transform case. (I use `len(s)` as `L`.)

```pseudo
// Insert vs Insert
function T_Ins_Ins(op, other):
  // op = Ins(i, s, site)
  // other = Ins(j, t, otherSite)
  if other.j < op.i:
     op.i += len(other.t)
  else if other.j == op.i:
     if other.siteId < op.siteId:
        op.i += len(other.t)
     // else op.i unchanged
  // else other.j > op.i -> nothing
  return op

// Insert vs Delete
function T_Ins_Del(op, other):
  // op = Ins(i, s)
  // other = Del(j, n)
  if other.j < op.i:
     shift = min(other.n, op.i - other.j)
     op.i -= shift
  // else j >= i -> no change
  return op

// Delete vs Insert
function T_Del_Ins(op, other):
  // op = Del(i, n)
  // other = Ins(j, t)
  L = len(other.t)
  if other.j <= op.i:
     op.i += L
     // op.n unchanged
  else if other.j >= op.i + op.n:
     // insert after deletion range -> no change
  else:
     // insert inside deletion range -> delete the inserted text too
     op.n += L
  return op

// Delete vs Delete
function T_Del_Del(op, other):
  // op = Del(i1, n1)
  // other = Del(i2, n2)
  s1 = op.i
  e1 = op.i + op.n
  s2 = other.i
  e2 = other.i + other.n

  if e2 <= s1:
     // other before op
     op.i = op.i - other.n
     // n unchanged
     return op
  if s2 >= e1:
     // other after op -> no change
     return op

  // overlap case:
  overlap = min(e1, e2) - max(s1, s2) // > 0
  op.n = op.n - overlap
  if op.n < 0: op.n = 0
  if s2 < s1:
     shift = min(other.n, s1 - s2)
     op.i = op.i - shift
  // if s2 >= s1, op.i unchanged
  return op
```

Notes:

* After a transform, `op.n == 0` means the delete became a no-op — drop it.
* Always keep operations canonical (no negative positions or counts).

---

# 6) Worked examples — short, concrete walks

A) `Ins` vs `Ins` tie:

* Start `"abc"`
* A: `Ins(1,"X", site=A)`
* B: `Ins(1,"Y", site=B)` with `A < B`
  Server applies A first, when B arrives transform B: `j==i` and `A < B` so `B` shifts to position 2. Final `"a X Y bc"` → `"aXYbc"`.

B) `Ins` vs `Del`:

* Start `"HELLO"`
* A: `Ins(4,"!")` (after L at index 4)
* B: `Del(2,2)` (remove `LL`)
  If server applies B first, then transform A: `j=2 < i=4`, shift = min(2, 4-2=2)=2 → A becomes `Ins(2,"!")`. Final result: delete `LL` then insert `!` at new pos.

C) `Del` vs `Ins` (insert inside delete):

* Start `"abcdefghi"`
* A: `Del(2,4)` intends to remove `"cdef"` (indices 2..5)
* B: `Ins(4,"X")` inserts inside the to-be-deleted region (between `e` and `f`)
  If server applied B first, transform A: `j=4` is inside `[2,6)`, so `n' = 4 + 1 = 5` → A now deletes `"cdeXf"` — the inserted X gets deleted too.

D) `Del` vs `Del` partial overlap:

* Start `"0123456789"`
* A: `Del(3,5)` removes `3,4,5,6,7`
* B: `Del(6,4)` removes `6,7,8,9`
  If B applied first: other=\[6,10) and A=\[3,8)
* overlap = min(8,10)-max(3,6)=8-6=2
* A.n' = 5 - 2 = 3
* since start2=6 > start1=3 → i' = 3
* A' = `Del(3,3)` which deletes `3,4,5` (the remainder). Works out.

---

# 7) Implementation tips & pitfalls

* Be careful to treat ranges as half-open `[start, end)`. It simplifies overlap arithmetic.
* Normalize ops before transforming: ensure `n >= 0`, `i >= 0`.
* When chaining many transforms (incoming op vs many outstanding ops), apply transforms sequentially in the same order others were applied. Order matters.
* Keep a deterministic tie-breaker (siteId, client timestamp with tie-break on siteId, etc.). Make it total and global.
* If you allow multi-character deletes, remember to treat them as a single op (not many single-character deletes). OT works better with coalesced operations.
* When implementing the client-side transform (incoming against outstanding), use the `T` rules with `incoming` as `op` and each `myOp` in outstanding as `other` in chronological order.

---

# 8) Why the Delete-Insert decision matters (small design note)

We chose to expand deletes to remove concurrent inserts falling inside their range. This preserves the intention “remove that region” even if someone inserted into it concurrently. An alternative is to keep deletes from removing concurrent inserts (so inserted text survives). Either approach can be made consistent; just pick one and ensure the server and all clients implement the same rule. I recommend the “delete wins over concurrent insert inside range” approach for simpler intention preservation in many text-editor scenarios.


---

[OT](https://medium.com/coinmonks/operational-transformations-as-an-algorithm-for-automatic-conflict-resolution-3bf8920ea447)
[Demo](https://operational-transformation.github.io/)
