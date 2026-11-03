# Chapter 4 — Skip List Insertion and Deletion: Pointer Updates Internally

In Chapter 3, we learned how a skip list searches for a key by moving horizontally and vertically through its levels.

Now we will build on that knowledge to understand how a skip list changes when elements are inserted or deleted.

The most important idea in this chapter is:

Insertion and deletion do not rebuild the skip list. They update only the forward pointers affected by the change.

We'll trace the algorithms visually, understand the `update[]` array, implement both operations in Java, and examine the edge cases that matter in interviews and production code.

## 1. Start with an existing skip list

Consider this skip list. Every node has a height chosen when it was inserted.

```
Level 3: HEAD ─────→ 20 ─────────────────→ 60 ──→ NIL

Level 2: HEAD ─────→ 20 ─────→ 40 ───────→ 60 ──→ NIL

Level 1: HEAD → 10 → 20 → 30 → 40 → 50 → 60 → NIL
```

For this example:

| Node | Height |
| ---- | ------ |
| 10   | 1      |
| 20   | 3      |
| 30   | 1      |
| 40   | 2      |
| 50   | 1      |
| 60   | 3      |

The nodes are sorted by key, and the bottom level contains every element.

We will first insert `35`, then delete `40`.

# Part A — Insertion

## 2. What must happen when we insert a new value?

Suppose we want to insert `35`.

The bottom level currently contains:

```
10 → 20 → 30 → 40 → 50 → 60
```

The new value belongs between `30` and `40`.

A normal linked-list insertion would update one pointer:

```
Before:  30 ─────→ 40

After:   30 ──→ 35 ──→ 40
```

A skip list must also handle the higher levels.

The new node might participate in level 1 only, or in levels 1 and 2, or even in more levels.

So the first question is:

Where should the new node be inserted at each level it participates in?

We need to find its predecessor at every relevant level.

A predecessor is simply the node immediately before the insertion position at that level.

For our example:

- At level 1, the predecessor is `30`.
- At level 2, the predecessor is `20`.
- At level 3, the predecessor is also `20`, although our new node may not participate in level 3.

We can discover these predecessors using the search algorithm from Chapter 3.

However, instead of storing only the final predecessor, we remember the predecessor found at every level.

That brings us to one of the most important concepts in skip-list insertion.

## 3. The `update[]` array

During insertion, we maintain an array commonly named `update`.

Its purpose is to store the predecessor of the new key at each active level.

Assume our levels use zero-based indices in Java:

- Index `0`: bottom level.
- Index `1`: second level.
- Index `2`: third level.

For the insertion of `35`, the array will contain:

| Java index  | Level   | Predecessor stored in `update[]` |
| ----------- | ------- | -------------------------------- |
| `update[0]` | Level 1 | Node `30`                        |
| `update[1]` | Level 2 | Node `20`                        |
| `update[2]` | Level 3 | Node `20`                        |

Even though the new node might not occupy level 3, recording the predecessor there is useful because we need to handle any height changes and maintain consistent insertion logic.

The array is temporary. It is used while inserting one node and can be discarded afterward.

Think of it as a map of the exact locations where pointer updates will be necessary.

### How do we populate it?

We combine the search algorithm with predecessor recording.

```
Node[] update = new Node[MAX_LEVEL];
Node current = head;

for (int i = currentLevel - 1; i >= 0; i--) {
    while (current.forward[i] != null
            && current.forward[i].key < key) {
        current = current.forward[i];
    }

    update[i] = current;
}
```

Let's understand the two loops.

The outer loop moves from the highest active level down to the bottom.

The inner loop moves horizontally while the next key is smaller than the value we want to insert.

When the inner loop stops, `current` is the predecessor at that level. We save it in `update[i]`.

Then we descend and continue finding the predecessor at the next level.

This is similar to search, except that we retain the predecessor at every level instead of simply checking whether the key exists.

## 4. Inserting `35` step by step

Our original skip list is:

```
Level 3: HEAD ─────→ 20 ─────────────────→ 60 ──→ NIL

Level 2: HEAD ─────→ 20 ─────→ 40 ───────→ 60 ──→ NIL

Level 1: HEAD → 10 → 20 → 30 → 40 → 50 → 60 → NIL
```

Assume that random-level generation gives the new node `35` a height of 2.

That means `35` will participate in levels 1 and 2, but not level 3.

### Step 1: Find the predecessor at level 3

Starting at `HEAD`, we move to `20` because \\(20 < 35\\).

The next node is `60`, and \\(60 > 35\\).

We stop.

Therefore:

```
update[2] = 20
```

We have not modified any pointer yet. We are only locating the insertion positions.

### Step 2: Find the predecessor at level 2

We descend from `20` to level 2, keeping our current position.

The next node is `40`.

Since \\(40 > 35\\), we stop.

Therefore:

```
update[1] = 20
```

### Step 3: Find the predecessor at level 1

At level 1, the next node after `20` is `30`.

Because \\(30 < 35\\), move to `30`.

The next node is `40`. Because \\(40 > 35\\), stop.

Therefore:

```
update[0] = 30
```

Our predecessor array is now:

```
update[2] = 20
update[1] = 20
update[0] = 30
```

Now we know exactly which links to change.

### Step 4: Create the new node

We create a node with key `35` and height `2`.

Conceptually:

```
Node 35
 ├── forward[0]
 └── forward[1]
```

It has no pointer for level 3.

### Step 5: Update the pointers at level 1

Before insertion:

```
30 ─────────→ 40
```

The new node must be inserted between them.

First, give the new node the successor that its predecessor currently points to:

```
newNode.forward[0] = update[0].forward[0];
```

Since `update[0]` is `30`, this sets:

```
35.forward[0] = 40
```

Then redirect the predecessor's pointer to the new node:

```
update[0].forward[0] = newNode;
```

Now:

```
30 ──→ 35 ──→ 40
```

The ordering is preserved.

Always connect the new node to the existing successor before redirecting the predecessor's pointer. This order is easy to reason about and preserves the original link while the insertion is being constructed.

### Step 6: Update the pointers at level 2

The predecessor at level 2 is `20`, and its current successor at that level is `40`.

We perform the same two updates:

```
newNode.forward[1] = update[1].forward[1];
update[1].forward[1] = newNode;
```

Now level 2 becomes:

```
20 ──→ 35 ──→ 40
```

Notice that level 2 has a different predecessor from level 1.

This is why we need `update[]`.

### Step 7: Leave level 3 unchanged

Node `35` has height 2.

Therefore, it does not participate in level 3, and the level-3 pointer from `20` to `60` remains untouched.

The final skip list is:

```
Level 3: HEAD ─────→ 20 ─────────────────→ 60 ──→ NIL

Level 2: HEAD ─────→ 20 ─────→ 35 ──→ 40 → 60 ──→ NIL

Level 1: HEAD → 10 → 20 → 30 → 35 → 40 → 50 → 60 → NIL
```

The list is sorted, all links are correct, and we modified only the levels in which the new node participates.

That is the central mechanism behind skip-list insertion.

## 5. What if the new node has more levels than the current list?

So far, the tallest existing node has height 3.

Suppose the new node has height 5, but the current active level is only 3. Also, assume the configured maximum height is at least 5.

The new node would introduce two previously unused levels.

We need to handle this before linking it.

For every newly activated level, the head sentinel is the predecessor because there are no ordinary nodes at that level yet.

In Java:

```
for (int i = currentLevel; i < nodeHeight; i++) {
    update[i] = head;
}

currentLevel = Math.max(currentLevel, nodeHeight);
```

We initialize the missing predecessors to `head`, then increase the current active level.

Afterward, the normal pointer-insertion loop can connect the new node.

For example, if the new node is `80` and its height is 5, the new top levels initially look like:

```
Level 5: HEAD ──→ 80 ──→ NIL

Level 4: HEAD ──→ 80 ──→ NIL
```

The new node is the first participating node in each of those levels.

Two distinctions are worth remembering:

- Configured maximum height is the highest height allowed by the implementation.
- Current active level is the highest level currently occupied by the structure.

The active level can grow or shrink over time without changing the configured maximum.

## 6. Complete Java insertion algorithm

We can now put the pieces together.

This code assumes:

- Each node stores an integer key and a `forward` array.
- Keys are unique.
- `head.forward` has `MAX_LEVEL` pointer slots.
- `currentLevel` is a count of active levels, initially `1`.
- `MAX_LEVEL` is a positive constant.
- `randomLevel()` returns a height between `1` and `MAX_LEVEL`.
- `size` stores the number of data nodes, excluding the head sentinel.

```
public boolean add(int key) {
    Node[] update = new Node[MAX_LEVEL];
    Node current = head;

    // 1. Find the predecessor at every active level.
    for (int i = currentLevel - 1; i >= 0; i--) {
        while (current.forward[i] != null
                && current.forward[i].key < key) {
            current = current.forward[i];
        }

        update[i] = current;
    }

    // 2. Reject duplicate keys.
    Node candidate = current.forward[0];

    if (candidate != null && candidate.key == key) {
        return false;
    }

    // 3. Choose the new node's height.
    int nodeHeight = randomLevel();

    // 4. Initialize predecessors for newly activated levels.
    for (int i = currentLevel; i < nodeHeight; i++) {
        update[i] = head;
    }

    // 5. Update the current active level.
    currentLevel = Math.max(currentLevel, nodeHeight);

    // 6. Create and link the new node.
    Node newNode = new Node(key, nodeHeight);

    for (int i = 0; i < nodeHeight; i++) {
        newNode.forward[i] = update[i].forward[i];
        update[i].forward[i] = newNode;
    }

    size++;
    return true;
}
```

### Why do we check for duplicates before choosing the height?

In this implementation, keys are unique.

If the key already exists, the insertion does nothing and returns `false`.

There is no reason to allocate a new node or generate its height.

Another implementation could update the existing node or allow multiple entries with equal keys, but those are different API and ordering decisions.

The important part is to define duplicate behavior and implement it consistently.

### Why does the insertion loop stop at `nodeHeight`?

```
for (int i = 0; i < nodeHeight; i++)
```

The new node only participates in levels `0` through `nodeHeight - 1`.

There is no need to modify its pointers at higher levels because those pointers do not exist in the new node.

### Why is the insertion expected \\(O(\log n)\\)?

Finding the predecessors requires the same expected logarithmic search as searching for an existing key.

Generating the height takes expected constant time under the usual geometric promotion scheme with a fixed maximum level, and linking the node takes \\(O(h)\\), where \\(h\\) is the selected node height.

The expected height is bounded by a constant for fixed promotion probability, so the overall expected insertion cost is:

\\[ \boxed{O(\log n)} \\]

The configured maximum height also bounds the pointer-update work. The search still has a worst-case \\(O(n)\\) cost for an unlucky skip-list structure.

# Part B — Deletion

## 7. Why is deletion more than removing a node from level 1?

Let's return to our skip list after inserting `35`.

```
Level 3: HEAD ─────→ 20 ─────────────────→ 60 ──→ NIL

Level 2: HEAD ─────→ 20 ─────→ 35 ──→ 40 → 60 ──→ NIL

Level 1: HEAD → 10 → 20 → 30 → 35 → 40 → 50 → 60 → NIL
```

Suppose we want to delete `40`.

If we remove it only from level 1, the higher-level pointer from `35` to `40` at level 2 would remain.

That would leave a pointer to a node that is supposed to have been deleted.

So deletion must remove the target from every level in which it participates.

But how do we know which nodes must be updated?

Exactly as with insertion, we find the predecessors at each level.

The difference is that we use those predecessors to bypass the existing node rather than link a new one.

## 8. Delete `40` step by step

### Step 1: Find the predecessor at every active level

Search for `40`, moving forward only while the next key is smaller than `40`.

The predecessor array becomes:

| Level   | Predecessor | Relevant pointer |
| ------- | ----------- | ---------------- |
| Level 3 | `20`        | `20 → 60`        |
| Level 2 | `35`        | `35 → 40`        |
| Level 1 | `35`        | `35 → 40`        |

Why is the level-3 predecessor `20`?

At level 3, the next node after `20` is `60`. Since `40` is absent at level 3, we stop before `60`.

Why is the predecessor `35` at level 1?

Because `35 < 40`, so the search advances to `35`, then stops when the next node is `40`.

### Step 2: Identify the target

At the end of predecessor discovery, inspect the next bottom-level node:

```
Node target = current.forward[0];
```

If `target` is `null` or its key does not match `40`, there is nothing to delete.

Otherwise, we have found the target.

### Step 3: Bypass the target at level 1

Before:

```
35 ──→ 40 ──→ 50
```

We redirect the predecessor:

```
update[0].forward[0] = target.forward[0];
```

Now:

```
35 ─────────→ 50
```

The bottom-level chain no longer includes `40`.

### Step 4: Bypass the target at level 2

Before:

```
35 ──→ 40 ──→ 60
```

We update the level-2 predecessor's pointer:

```
35 ─────────→ 60
```

Level 3 remains unchanged because `40` did not participate in that level.

### Step 5: Remove the target from all its levels

The final structure is:

```
Level 3: HEAD ─────→ 20 ─────────────────→ 60 ──→ NIL

Level 2: HEAD ─────→ 20 ─────→ 35 ───────→ 60 ──→ NIL

Level 1: HEAD → 10 → 20 → 30 → 35 → 50 → 60 → NIL
```

The node no longer appears in the skip list.

Other nodes retain their existing heights. We do not need to re-randomize or rebalance the remaining structure.

## 9. The deletion algorithm in Java

Here is the implementation for deleting a unique integer key.

```
public boolean remove(int key) {
    Node[] update = new Node[MAX_LEVEL];
    Node current = head;

    // 1. Find the predecessor at every active level.
    for (int i = currentLevel - 1; i >= 0; i--) {
        while (current.forward[i] != null
                && current.forward[i].key < key) {
            current = current.forward[i];
        }

        update[i] = current;
    }

    // 2. Find the target at the bottom level.
    Node target = current.forward[0];

    if (target == null || target.key != key) {
        return false;
    }

    // 3. Bypass the target wherever it participates.
    for (int i = 0; i < target.forward.length; i++) {
        if (update[i].forward[i] == target) {
            update[i].forward[i] = target.forward[i];
        }
    }

    // 4. Remove unused top levels.
    while (currentLevel > 1
            && head.forward[currentLevel - 1] == null) {
        currentLevel--;
    }

    size--;
    return true;
}
```

Notice that we iterate up to `target.forward.length`, the number of levels in the target node.

The target cannot be a member of a level at or above its height. Therefore, it only needs to be bypassed at the levels it participates in.

### Why check whether the predecessor points to the target?

```
if (update[i].forward[i] == target)
```

This verifies that the target is actually present in the current level before modifying that pointer.

It also ensures we do not access `target.forward[i]` for a level that the target does not have.

### Why reduce `currentLevel` after deletion?

Suppose the tallest node in the structure had height 5, and we delete it.

If no other nodes participate in level 5, that level becomes empty.

The `while` loop reduces `currentLevel` until the highest active level is nonempty, or the structure reaches its minimum active level of 1.

This is a metadata update, not a rebalancing operation.

## 10. Why is deletion expected \\(O(\log n)\\)?

Deletion first finds the target's predecessors, using the same search procedure as membership lookup.

That requires expected \\(O(\log n)\\) time.

Once the predecessors are known, bypassing the target takes \\(O(h)\\), where \\(h\\) is its height. Under the standard randomized model, this contributes only expected constant work when the promotion probability and configured maximum height are fixed.

Reducing the active level also takes no more than the configured maximum number of iterations.

So the expected deletion complexity is:

\\[ \boxed{O(\log n)} \\]

As with search and insertion, the worst case is \\(O(n)\\) under an unlucky level arrangement.

# Part C — Random height generation

## 11. How does `randomLevel()` work?

The insertion method uses a helper that chooses a node's height.

A standard implementation uses repeated random promotion.

```
private static final int MAX_LEVEL = 16;
private static final double P = 0.5;

private final Random random = new Random();

private int randomLevel() {
    int height = 1;

    while (height < MAX_LEVEL
            && random.nextDouble() < P) {
        height++;
    }

    return height;
}
```

The algorithm begins at height 1.

For each iteration:

- If the random value is smaller than `P`, the node is promoted by one level.
- Otherwise, promotion stops.
- Promotion also stops when `MAX_LEVEL` is reached.

With \\(P = 0.5\\), the approximate probabilities before reaching the maximum-height cap are:

| Height | Probability |
| ------ | ----------- |
| 1      | 50%         |
| 2      | 25%         |
| 3      | 12.5%       |
| 4      | 6.25%       |
| 5      | 3.125%      |

Most nodes have short heights, and only a small fraction participate in high levels.

This pattern is what produces the sparse upper levels that make the skip list efficient.

For the code examples above, `MAX_LEVEL = 16` is simply a demonstration setting. Production implementations should choose a maximum height appropriate for the expected collection size and the details of their implementation.

We will derive the probability distribution and analyze the expected height more carefully in Chapter 5.

# Part D — What insertion and deletion teach us

## 12. Why skip lists are relatively easy to maintain

Let's compare the structural work.

A skip list insertion:

1. Finds the predecessor at each active level.
2. Chooses a height for the new node.
3. Updates the predecessor pointers at levels where the new node participates.
4. Updates the current active level if needed.

A skip list deletion:

1. Finds the predecessor at each active level.
2. Verifies the target exists.
3. Bypasses the target at every level where it participates.
4. Reduces the current active level if its top levels become empty.

Neither operation needs to rotate nodes, maintain balance factors, or restore a strict tree shape.

The trade-off is that each node may hold several pointers, and the probabilistic balancing method does not provide the same deterministic worst-case logarithmic guarantee as a properly maintained balanced search tree.

## 13. Common implementation mistakes

These are particularly important when implementing a skip list yourself.

Mistake 1: Updating only the bottom level.

This leaves the higher-level links incorrect. An inserted or deleted node must be linked or bypassed at every level in which it participates.

Mistake 2: Forgetting the predecessor array.

Without storing predecessors, the algorithm may need to repeat searches to find the locations where pointers should change. The `update[]` array records these locations during the initial traversal.

Mistake 3: Accessing a pointer level that the node does not have.

A height-2 node has pointer indices `0` and `1`, not `2`. Always respect the lengths of the node's forward array.

Mistake 4: Increasing the current active level without initializing the new predecessors.

When a new node introduces higher levels, those levels must be linked from the head sentinel before the pointer-update loop is executed.

Mistake 5: Forgetting to reduce the active level after deletion.

Empty top levels do not necessarily break correctness if handled consistently, but leaving stale active-level metadata wastes search work and complicates the implementation.

Mistake 6: Using inconsistent comparison rules for duplicates.

The search logic shown so far assumes unique integer keys. If duplicates are allowed, the definition of the insertion position must be adjusted consistently across search, insertion, and deletion.

Mistake 7: Assuming the algorithms are thread-safe.

The Java examples are sequential, unsynchronized algorithms. Concurrent inserts, deletes, and searches require additional synchronization or a carefully designed concurrent skip-list implementation.

## Chapter 4 — Knowledge check

Try these without looking back.

1\. Why is an `update[]` array needed during insertion?

It stores the predecessor of the new key at every active level so we can update the correct links without searching again.

2\. If a node has height 2, which levels must be updated during insertion?

Levels 1 and 2 in one-based terminology, corresponding to indices 0 and 1 in our Java arrays.

3\. If a new node has a height greater than the current active level, what must happen?

The predecessor entries for the newly activated levels are initialized to `head`, and the current active level is increased.

4\. When deleting a node with height 3, how many levels must be checked for bypassing it?

Three levels.

5\. Does deleting the tallest node require the remaining nodes to be re-randomized?

No. The remaining node heights stay unchanged. The active level is reduced if the highest levels become empty.

6\. What is the expected complexity of insertion and deletion?

Both are \\(O(\log n)\\) under the standard randomized skip-list model, while their worst-case complexity is \\(O(n)\\).

## The key takeaway

Insertion and deletion are both extensions of skip-list search.

Search identifies where a key belongs. Insertion uses the recorded predecessors to splice a new node into the levels specified by its random height. Deletion uses the same predecessors to bypass an existing node at every level it occupies.

The `update[]` array is the bridge between searching and modifying the structure.

Once you understand why those predecessor pointers are needed, the implementation becomes much easier to reason about.

Next: Chapter 5 — Randomization and the mathematics behind \\(O(\log n)\\). We will derive the probability of each height, estimate the number of levels and horizontal movements, understand how the promotion probability affects memory and performance, and explain why randomized balancing gives expected logarithmic operations without guaranteeing logarithmic worst-case behavior.


