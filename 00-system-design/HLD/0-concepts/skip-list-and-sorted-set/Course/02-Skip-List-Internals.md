# Chapter 2 — Skip List Internals: Nodes, Levels and Pointer Invariants

In Chapter 1, we learned that a skip list is a sorted linked list with additional forward pointers that act as shortcuts.

Now let's examine what actually exists inside the data structure.

By the end of this chapter, you should be able to look at any skip-list diagram and answer:

- Which pointers belong to each node?
- Why does a node participate in multiple levels?
- What exactly does a pointer at level 3 point to?
- How is the highest level determined?
- What properties must always remain true for a skip list to work correctly?

These concepts are essential before learning insertion and deletion.

## 1. Start with one important distinction

Consider a node containing the value `40`.

It might have four forward pointers.

Does that mean it contains four copies of the value `40`?

No.

A skip-list node represents one element, but it can participate in multiple levels of the linked structure.

Each level gives that node another forward pointer.

For example:

```
Node 40

┌────────────────────────┐
│ value = 40             │
├────────────────────────┤
│ forward[0] ───────────────→ 50
│ forward[1] ───────────────→ 60
│ forward[2] ───────────────→ 70
│ forward[3] ───────────────→ 90
└────────────────────────┘
```

This is a conceptual example. The array indices are zero-based, so:

- `forward[0]` is the bottom-level pointer.
- `forward[1]` is the second-level pointer.
- `forward[2]` is the third-level pointer.
- `forward[3]` is the fourth-level pointer.

Each pointer follows the next available node at its corresponding level.

The critical point is that these pointers can have different destinations.

Node `40` does not need to point to `50` at every level. At higher levels, it can skip over intermediate nodes.

## 2. Understand node height precisely

Let's define a term that we will use throughout this course.

Node height is the number of levels in which a node participates.

Suppose a node has height 1.

It participates only in the bottom level:

```
Level 1:  40 ──→ 50
```

A node with height 2 participates in two levels:

```
Level 2:  40 ─────────→ 60

Level 1:  40 ──→ 50 ──→ 60
```

A node with height 3 participates in three:

```
Level 3:  40 ─────────────────→ 70

Level 2:  40 ─────────→ 60 ──→ 70

Level 1:  40 ──→ 50 ──→ 60 ──→ 70
```

In each example, the same logical node `40` is represented at every level in which it participates. It is not being duplicated in memory.

A node of height 3 owns three forward pointers.

A node of height 1 owns one forward pointer.

A node's height is generally selected randomly during insertion, and it remains fixed while that node exists.

### A subtle but important point

If a node participates in level 3, it must also participate in levels 2 and 1.

It cannot have a level-3 pointer while missing its level-2 or level-1 pointer.

This is how the levels remain nested.

We will formalize this invariant shortly.

## 3. One complete skip list, examined level by level

Let's construct a specific example.

Assume the following values and assigned heights:

| Value | Node height |
| ----- | ----------- |
| 10    | 1           |
| 20    | 2           |
| 30    | 1           |
| 40    | 4           |
| 50    | 1           |
| 60    | 2           |
| 70    | 3           |
| 80    | 1           |
| 90    | 4           |

These heights are illustrative; we are choosing them manually to understand the structure. In a real skip list, the height is generated according to the random-level policy.

The structure looks like this:

```
Level 4:  HEAD ─────────→ 40 ─────────→ 90 ──→ NIL

Level 3:  HEAD ─────────→ 40 ──→ 70 ──→ 90 ──→ NIL

Level 2:  HEAD ──→ 20 ──→ 40 ──→ 60 ──→ 70 ──→ 90 ──→ NIL

Level 1:  HEAD → 10 → 20 → 30 → 40 → 50 → 60 → 70 → 80 → 90 → NIL
```

Here, `HEAD` is a special starting node, and `NIL` represents the end of the list.

Let's understand each level.

### Level 1: Every element

All nine values appear at the bottom level.

This is the complete sorted linked list:

`10 → 20 → 30 → 40 → 50 → 60 → 70 → 80 → 90`

Therefore, even if every higher-level shortcut is useless or unavailable, the underlying collection remains a valid sorted linked list.

### Level 2: Nodes with height at least 2

The nodes with height at least 2 are:

`20, 40, 60, 70, 90`

Their level-2 pointers form another sorted linked list.

Notice that `20` points to `40`, not to `30`.

Why?

Node `30` has height 1, so it does not participate in level 2.

A level-2 pointer must point to the next node that also participates in level 2.

### Level 3: Nodes with height at least 3

The nodes are:

`40, 70, 90`

Node `40` points directly to `70`, skipping `50` and `60`.

Node `70` points to `90`, skipping `80`.

These pointers let a search cover larger distances.

### Level 4: Nodes with height at least 4

Only these nodes participate:

`40, 90`

At this level, `40` can jump straight to `90`.

The top level has fewer nodes, but its links potentially cover greater distances.

Key observation: Every higher level is a subset of the level directly below it. Nodes are not independently generated for each level; their selected heights determine which levels they participate in.

## 4. What does a forward pointer actually mean?

This is one of the most important internal details.

Suppose we are standing at node `40`.

Its forward pointers might be:

| Pointer      | Destination | Why?                             |
| ------------ | ----------- | -------------------------------- |
| `forward[0]` | `50`        | `50` is the next node at level 1 |
| `forward[1]` | `60`        | `60` is the next node at level 2 |
| `forward[2]` | `70`        | `70` is the next node at level 3 |
| `forward[3]` | `90`        | `90` is the next node at level 4 |

The destination is determined by the level, not by a fixed number of positions to skip.

For example, `forward[2]` does not mean "jump exactly two nodes."

It means:

> Follow the pointer to the next node that participates in level 3.

This is why the distances covered by forward pointers can vary.

One pointer might skip one node; another might skip ten or a thousand.

The data structure does not store an explicit distance for every pointer. It stores the link to the next node at that level.

### Why this matters for insertion

Suppose we insert `45`.

The bottom-level neighbors will be `40` and `50`.

If the new node has height 1, only its bottom-level pointer needs to be added.

If it has height 3, it must be linked into all three levels. The appropriate predecessor pointers must be updated at each level.

This is what allows insertions to remain local rather than rebuilding the entire index.

## 5. The internal representation

Let's translate what we have learned into a Java-oriented structure.

```
class Node {
    int key;
    Node[] forward;

    Node(int key, int height) {
        this.key = key;
        this.forward = new Node[height];
    }

    int height() {
        return forward.length;
    }
}
```

This is the basic representation for an integer-keyed skip list with unique keys.

`key` identifies the element.

`forward` contains the node's forward pointers.

`forward.length` determines the node's height.

For example:

```
Node node = new Node(40, 4);
```

This creates a node with four pointer slots:

```
node.key = 40

node.forward[0]
node.forward[1]
node.forward[2]
node.forward[3]
```

Initially, these pointers are `null` until the node is linked into the structure.

A complete implementation also needs a head node, a representation of the current maximum active level, a configured maximum height, and methods for searching, insertion, and deletion.

The head node is usually created with enough pointer slots for the configured maximum height.

The sample class above is a building block rather than a complete implementation.

## 6. Why do we need a HEAD sentinel?

Consider a search for the smallest key in the list.

If the list is empty, there is no ordinary data node from which to begin.

If the list is nonempty, we need an easy, consistent starting point.

The skip list therefore maintains a special head sentinel, often called `header`.

It is not an ordinary data element. It serves as the entry point to every level.

Conceptually:

```
HEAD
 ├── forward[0] → first node at level 1
 ├── forward[1] → first node at level 2
 ├── forward[2] → first node at level 3
 └── forward[3] → first node at level 4
```

When a level has no participating data nodes, the corresponding head pointer is `null` or points to a designated end sentinel, depending on the implementation.

### Why this simplifies the algorithms

Without a head sentinel, inserting a new smallest element would require special handling because the list's starting reference would need to change.

With a sentinel, the head's pointers can be updated just like any other predecessor's pointers.

The head is effectively the predecessor of the first node at every level.

This simplifies search, insertion, and deletion.

### Is HEAD the same as the first element?

No.

Suppose the smallest key is `10`.

Then:

```
HEAD → 10 → 20 → 30 → ...
```

`HEAD` has no normal data key and isn't counted among the actual elements.

If the skip list contains \\(n\\) elements, it still contains \\(n\\) data nodes, even though there is an additional head sentinel.

Some implementations also maintain a tail pointer or use a `NIL` sentinel at the end. These are implementation choices rather than essential properties of every skip list.

## 7. The most important structural invariants

An invariant is a property that must remain true for the data structure to be correct.

You should know these before writing a skip-list insertion or deletion algorithm.

### Invariant 1: Level 1 contains every data node

The bottom level is the complete collection.

If a key exists in the skip list, its node must appear at level 1.

Higher levels are shortcuts; they do not replace the underlying collection.

### Invariant 2: Every level is sorted

Every forward pointer must lead to a node with a key no smaller than the current key.

For unique keys:

\\[ x.key < x.forward[i].key \\]

whenever `x.forward[i]` points to an ordinary next node.

The `NIL` or null case must be handled separately.

This means a search can safely move forward when the next key is less than the target and descend when it would overshoot.

### Invariant 3: A node appears at every level up to its height

If a node has height \\(h\\), it participates in levels \\(1, 2, \ldots, h\\).

It cannot participate in level 3 without participating in levels 1 and 2.

This is the nesting property.

### Invariant 4: A level-\\(i\\) pointer targets the next node present at level \\(i\\)

A pointer must not skip over an intermediate node that also participates in the same level.

For example, if the level-2 nodes are:

`20 → 40 → 60 → 70`

then node `20` must point to `40` at level 2, not to `60`.

Node `30` cannot interfere with that pointer because it does not participate in level 2.

### Invariant 5: Node heights remain fixed during ordinary updates

Once a node is assigned a height, inserting or removing another node does not ordinarily change its height.

The node's forward pointers may be updated because its neighbors change, but the node does not need to be promoted or demoted when unrelated nodes are inserted or deleted.

This property is a major reason skip lists are easier to maintain than structures that require explicit balancing operations.

These invariants and the randomized-level approach form the basis of the classic skip-list algorithms described by William Pugh. [Read the original paper, Skip Lists: A Probabilistic Alternative to Balanced Trees.](https://15721.courses.cs.cmu.edu/spring2018/papers/08-oltpindexes1/pugh-skiplists-cacm1990.pdf?utm_source=chatgpt.com)

## 8. Configured maximum height vs. current height

These two ideas are easy to confuse.

Suppose our implementation allows at most 16 levels.

We create a head sentinel with 16 pointer slots.

That value is the configured maximum height.

However, assume the tallest node currently in the list has height 4.

The current active level is 4.

The distinction can be represented as:

| Property                        | Value in this example |
| ------------------------------- | --------------------- |
| Configured maximum level        | 16                    |
| Current highest active level    | 4                     |
| Number of pointer slots in HEAD | 16                    |

If a node with height 6 is later inserted, the current active level can increase to 6.

The configured maximum remains 16.

Conversely, if the last node at the current highest level is deleted, the current active level can decrease.

The head sentinel can still retain its 16 pointer slots. The unused higher-level pointers simply remain empty.

### Why don't we always search all configured levels?

Suppose we support 32 levels but the tallest node has height 4.

Starting from level 32 would mean examining many empty head pointers before useful traversal could begin.

A common implementation tracks the current active level and starts searching there.

Whenever the active level is reduced or increased, the algorithm updates this metadata accordingly.

This is not just an optimization. Correctly maintaining the current level keeps the implementation easy to reason about and avoids unnecessary work.

## 9. What happens when the list is empty?

Let's initialize a skip list with a configured maximum height of 4.

The head sentinel has four forward pointers, but there are no data nodes.

```
Current active level: 1

HEAD
 ├── forward[0] → null
 ├── forward[1] → null
 ├── forward[2] → null
 └── forward[3] → null
```

An implementation might use zero or one as its initial current-level convention; the algorithms must use that convention consistently. The important property is that every level is initially empty.

Now insert `10` with height 1.

```
Level 1: HEAD → 10 → null

Level 2: HEAD → null
Level 3: HEAD → null
Level 4: HEAD → null
```

The current active level remains 1.

Next, insert `20` with height 3.

```
Level 3: HEAD ─────→ 20 → null

Level 2: HEAD ─────→ 20 → null

Level 1: HEAD → 10 → 20 → null
```

The current active level becomes 3.

Notice that `10` remains at level 1 only. We did not have to modify its height.

We only had to connect `20` into the levels it was assigned.

This is the underlying mechanism behind incremental skip-list updates.

## 10. Quick knowledge check

Answer these without looking back.

Question 1: A node has height 4. How many forward-pointer slots does it need in the basic representation?

Answer: Four, one per level.

Question 2: If a node appears at level 3, must it also appear at level 1?

Answer: Yes. Every higher-level node participates in all lower levels.

Question 3: If node `40` has `forward[2]` pointing to `70`, what does that pointer represent?

Answer: Assuming zero-based indexing, `forward[2]` is the level-3 pointer. It points to the next node after `40` that participates in level 3, which is `70` in this example.

Question 4: Is the HEAD sentinel an ordinary data element?

Answer: No. It is a special starting node that provides an entry point to the levels.

Question 5: If the configured maximum level is 16 but the tallest node has height 4, what is the current active level?

Answer: Four.

Question 6: When inserting a node with height 2, must we modify every level in the skip list?

Answer: No. We need to update the links at levels 1 and 2 that are affected by the insertion. Higher-level links do not need changes merely because a shorter node was inserted.

## What you should take away

The skip list is not a collection of independent linked lists. It is one sorted collection whose nodes participate in a hierarchy of linked levels.

Each node's height determines which levels it belongs to. Each forward pointer connects to the next node participating in that same level. The head sentinel provides an entry point, and the current active level identifies where searching should begin.

Once these structural rules are clear, the algorithms become much easier to understand.

Next: Chapter 3 — Search in depth. We will trace the exact pointer movements, establish the search loop invariant, understand why the algorithm never skips a matching key, and analyze the cost of searching from the top level down to the bottom.



