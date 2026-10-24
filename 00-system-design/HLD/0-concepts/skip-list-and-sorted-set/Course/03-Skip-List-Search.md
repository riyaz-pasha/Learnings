# Chapter 3 — Skip List Search: How It Works Internally

In the previous chapters, we learned how nodes participate in multiple levels and how forward pointers provide shortcuts.

Now we will study the most fundamental skip-list operation: searching for a key.

By the end of this chapter, you should be able to trace every pointer movement, write the search algorithm in Java, explain why it never skips a matching element, and understand why its expected time complexity is \\(O(\log n)\\).

## 1. First, understand the search strategy

Suppose we have this skip list:

```
Level 4:  HEAD ──────────→ 40 ──────────────→ 90 ──→ NIL

Level 3:  HEAD ──────────→ 40 ──→ 70 ───────→ 90 ──→ NIL

Level 2:  HEAD ──→ 20 ───→ 40 ──→ 60 ──→ 70 → 90 ──→ NIL

Level 1:  HEAD → 10 → 20 → 30 → 40 → 50 → 60 → 70 → 80 → 90 → NIL
```

Assume the target is `65`.

We could search the bottom level:

`10 → 20 → 30 → 40 → 50 → 60 → 70`

But that examines unnecessary nodes.

Instead, a skip list follows this strategy:

1. Start at the highest active level.
2. Look at the next node.
3. Move forward if its key is smaller than the target.
4. If the next node is greater than or equal to the target, descend one level.
5. Repeat until reaching the bottom level.
6. Check whether the next bottom-level node matches the target.

The key intuition is:

Move forward to get closer to the target. Move down to search more precisely.

The comparison with the next node determines whether moving forward is safe.

## 2. Trace a complete search for `65`

Let's follow every step.

### Step 1: Start at the head of level 4

Our current position is `HEAD`.

The next node is `40`.

Since:

\\[ 40 < 65 \\]

we can move to `40`.

Now the next level-4 node is `90`.

Since:

\\[ 90 > 65 \\]

we must not move to `90`. Doing so would pass the target.

We descend to level 3 while remaining at node `40`.

```
Level 4:  HEAD ───────→ [40] ───────────→ 90
                           |
                           ↓
Level 3:  HEAD ─────────→ [40] ──→ 70 ──→ 90
```

The square brackets indicate our current position.

Notice that we do not move back to the head when descending. We retain our current position.

Why is that safe?

Because `40` is already known to be less than `65`, and the levels below contain all the nodes from the higher levels.

### Step 2: Search at level 3

We are at `40`.

The next level-3 node is `70`.

Since:

\\[ 70 > 65 \\]

we descend to level 2, staying at `40`.

```
Level 3:  HEAD ─────────→ [40] ──→ 70 ───→ 90
                           |
                           ↓
Level 2:  HEAD ──→ 20 ───→ [40] ──→ 60 ──→ 70 ──→ 90
```

We have not reached the target, but we are narrowing the search.

### Step 3: Search at level 2

We are still at `40`.

The next level-2 node is `60`.

Since:

\\[ 60 < 65 \\]

we move forward to `60`.

Now the next node is `70`.

Since:

\\[ 70 > 65 \\]

we descend to level 1 at `60`.

```
Level 2:  HEAD → 20 → [40] ──→ 60 ──→ 70 ──→ 90
                            move forward
                                  ↓
Level 1:  HEAD → 10 → 20 → 30 → 40 → 50 → [60] → 70 → 80 → 90
```

The levels have allowed us to jump over `50` during the search.

### Step 4: Search at level 1

We are at `60`.

The next node is `70`.

Since:

\\[ 70 > 65 \\]

there is no need to move forward.

We have reached the bottom level and cannot descend further.

The target is not present.

The insertion position would be between `60` and `70`.

### What did we accomplish?

A normal linked list would visit:

`10, 20, 30, 40, 50, 60, 70`

The skip list's actual path was:

`HEAD → 40 → 60 → descend → inspect 70`

The skip list avoided traversing `10`, `20`, `30`, and `50`.

This is the practical benefit of the additional levels.

## 3. What if the target exists?

Let's search for `70` using the same structure.

At level 4:

- Move from `HEAD` to `40`.
- The next node is `90`, so descend.

At level 3:

- We are at `40`.
- The next node is `70`.
- Since \\(70\\) is not smaller than the target, descend.

At level 2:

- We are still at `40`.
- The next node is `60`.
- Since \\(60 < 70\\), move to `60`.
- The next node is `70`, so descend.

At level 1:

- We are at `60`.
- The next node is `70`.
- The bottom-level candidate matches the target.

The search succeeds.

The movement looks like this:

```
Level 4:  HEAD ───────→ [40] ───────────→ 90
                            |
                            ↓
Level 3:  HEAD ──────────→ [40] ──→ 70 ──→ 90
                            |
                            ↓
Level 2:  HEAD → 20 ─────→ [40] ──→ 60 ──→ 70
                                      |
                                      ↓
Level 1:  ... → 40 → 50 → [60] ─────→ 70
```

The search doesn't necessarily stop at the first level where a node with the target's key is visible. Instead, this version of the algorithm follows a consistent process: find the target's predecessor and inspect the next node at level 1.

That is especially convenient because the same search path can be reused by insertion and deletion.

## 4. The algorithm in Java

Let's implement the search using the node representation introduced in Chapter 2.

Assume each node contains an integer `key` and a `forward` array. The list also maintains a `head` sentinel and a `currentLevel` value representing the highest active level.

```
public boolean contains(int target) {
    Node current = head;

    // Start at the highest active level.
    for (int level = currentLevel - 1; level >= 0; level--) {

        // Move forward while the next key is smaller
        // than the target.
        while (current.forward[level] != null
                && current.forward[level].key < target) {

            current = current.forward[level];
        }
    }

    // At level 0, current is the predecessor of the
    // target's possible position.
    Node candidate = current.forward[0];

    return candidate != null && candidate.key == target;
}
```

This method assumes that `currentLevel` is stored as a count: for example, `currentLevel == 4` means levels 0 through 3 are active.

Let's examine the crucial parts.

### Why start at the highest active level?

Higher levels allow us to cover larger distances.

If we start at the bottom level, we lose the primary advantage of the skip list.

Starting high allows us to skip over large portions of the collection before refining the search at lower levels.

### Why use an outer `for` loop?

The outer loop controls the descent through levels:

```
for (int level = currentLevel - 1; level >= 0; level--)
```

Each iteration processes one level, moving from the highest active level down to level 0.

The loop must never descend below level 0 because that is the bottom level.

### Why use an inner `while` loop?

The inner loop handles horizontal movement:

```
while (current.forward[level] != null
        && current.forward[level].key < target)
```

It keeps moving while the next node exists and has a key smaller than the target.

There might be zero, one, or many horizontal moves at a given level.

### Why check for `null` first?

Java evaluates `&&` from left to right and short-circuits when the left operand is false.

Therefore, if `current.forward[level]` is `null`, Java does not evaluate:

```
current.forward[level].key < target
```

Without the null check, accessing the key would cause a `NullPointerException`.

### Why inspect the bottom-level successor afterward?

The horizontal loop stops at the last node whose key is smaller than the target.

Consequently, the next node is the only possible candidate for an exact match.

```
Node candidate = current.forward[0];

return candidate != null && candidate.key == target;
```

If the candidate matches, the target exists. Otherwise, it does not.

## 5. Why does the algorithm use `< target` rather than `<= target`?

This is a subtle but important implementation decision.

Consider:

```
10 → 20 → 30 → 40 → 50
```

Suppose the target is `30`.

### Version A: Move while the next key is smaller than the target

```
current.forward[level].key < target
```

The algorithm stops at `20`, because the next node is `30`, which is not smaller than the target.

At the bottom level, its successor is `30`.

Therefore, checking that successor finds the target.

This is the version used by our `contains` method.

### Version B: Move while the next key is smaller than or equal to the target

```
current.forward[level].key <= target
```

Now the algorithm moves directly to `30`.

After reaching the bottom level, its successor is `40`.

A final successor check would incorrectly report that `30` is absent.

The two versions are not interchangeable without adjusting the final check.

Rule to remember: If you advance while the next key is strictly smaller than the target, the final successor is the candidate for an exact match.

If duplicate keys are permitted, advancing with `< target` also naturally stops before the first equal key, assuming each equal-key entry is represented in the sorted ordering. Advancing with `<= target` instead positions the traversal after all equal keys it encounters.

That distinction becomes important when we implement duplicate handling and insertion.

## 6. The search loop invariant

Now let's understand why the algorithm is correct, rather than memorizing the code.

A loop invariant is a property that remains true as an algorithm executes.

At any point during our search:

1. Every ordinary node visited horizontally has a key strictly smaller than the target.
2. The current node is a valid starting point at the level currently being processed.
3. The sorted order guarantees that the next node at the current level is the next available candidate on that level.
4. Because lower levels include every node that appears on higher levels, descending from the current position does not discard any possible matching node that lies ahead.

Let's reason about the most important part.

Suppose we are at node `40` and the target is `65`.

The next node at level 3 is `70`.

Could the target `65` appear somewhere between `40` and `70` at a level lower than level 3?

Yes. A node with key `65` could exist at level 1 or 2 even though it does not appear at level 3.

Could it appear between `40` and `70` at level 3 itself?

No. The pointer from `40` leads to the next level-3 participant, `70`. By definition, no other level-3 node occurs between them.

Therefore, we descend rather than move to `70`.

At a lower level, we get a more detailed view of the same sorted range.

This is the logic that prevents us from skipping over a possible match.

### Why is it safe to stop at the bottom?

At level 1, every element is present.

Once we reach the final predecessor and inspect its successor, there are no lower levels left to search. If the successor isn't the target, the target cannot exist in the sorted sequence.

This establishes correctness for exact membership search.

## 7. Why is search expected \\(O(\log n)\\)?

Let's understand the performance before deriving it mathematically in Chapter 5.

Assume each node is independently promoted to the next level with probability \\(p\\).

A common choice is:

\\[ p = \frac12 \\]

With that choice, approximately half of the nodes that appear at one level also appear at the next level.

For example, if a level contains roughly 1,024 nodes, the level above it might contain about 512. The next could contain around 256, then 128, and so on.

This continues until the top levels contain only a few nodes.

The number of halvings needed to reduce \\(n\\) elements to approximately one is:

\\[ \log_2 n \\]

So the expected number of levels grows logarithmically.

However, the search cost is not just the number of levels. We must also account for horizontal moves at each level.

With the common promotion probability \\(p=1/2\\), the expected number of such moves per level remains bounded by a constant under the standard skip-list model.

Therefore, the expected total search work is proportional to the number of levels:

\\[ \boxed{\mathbb{E}[T(n)] = O(\log n)} \\]

This is expected complexity, not a guarantee that every randomly generated skip list has logarithmic height.

A particularly unlucky sequence of random level assignments can still result in a long search path.

We'll analyze the probability distribution and derive the expected height and search cost in Chapter 5.

## 8. How does skip-list search compare with other structures?

Consider an ordered collection of one million elements.

| Data structure       | Typical search complexity            | Key difference                                                             |
| -------------------- | ------------------------------------ | -------------------------------------------------------------------------- |
| Unsorted linked list | \\(O(n)\\)                           | Must potentially inspect every node                                        |
| Sorted linked list   | \\(O(n)\\)                           | Sorted order does not provide random access                                |
| Sorted array         | \\(O(\log n)\\)                      | Binary search is efficient because the middle index is directly accessible |
| Hash table           | Expected \\(O(1)\\) for exact lookup | Does not naturally provide sorted traversal or efficient range queries     |
| Balanced search tree | \\(O(\log n)\\)                      | Maintains balance through structural rules                                 |
| Skip list            | Expected \\(O(\log n)\\)             | Uses randomized node heights and forward pointers                          |

Why is a sorted array able to use binary search while a sorted linked list cannot do so efficiently?

Binary search relies on repeatedly jumping to the middle element. An array can access that position directly.

A linked list cannot efficiently jump to the middle. Even if it knows the desired index, it must follow pointers from another node.

Skip lists solve this problem by maintaining explicit shortcuts at multiple levels.

They are not universally faster than balanced trees. The practical choice depends on memory overhead, workload, concurrency requirements, implementation complexity, and range-query needs.

## 9. A practical search trace to master

Suppose we have this list:

```
Level 3: HEAD ─────────→ 30 ───────────→ 70 ──→ NIL

Level 2: HEAD ──→ 20 ──→ 30 ──→ 50 ───→ 70 ──→ NIL

Level 1: HEAD → 10 → 20 → 30 → 40 → 50 → 60 → 70 → 80 → NIL
```

Search for `55`.

Try predicting the path before reading the answer.

Trace:

1. Start at level 3 and move to `30`, because \\(30 < 55\\).
2. The next level-3 node is `70`. It is too large, so descend.
3. At level 2, move from `30` to `50`, because \\(50 < 55\\).
4. The next level-2 node is `70`. It is too large, so descend.
5. At level 1, the next node after `50` is `60`. It is greater than `55`.
6. The bottom-level candidate does not equal `55`, so the key is absent.

The visited path is:

```
HEAD → 30 → 50 → inspect 60
```

We did not need to visit `10`, `20`, or `40`.

Now search for `50`.

The algorithm reaches `30` at the upper levels, then advances to `50` at level 2. It descends and checks the bottom-level successor, which is `40` or `50` depending on the exact descent position.

Let's carefully trace it with the algorithm's `< target` rule:

- At level 3, move to `30`.
- At level 2, from `30`, the next node is `50`. Because \\(50 < 50\\) is false, do not move.
- Descend to level 1 at `30`.
- Move from `30` to `40`, since \\(40 < 50\\).
- The next node is `50`.
- Inspect `50` and return `true`.

The important lesson is that the final candidate is checked only after every level has been processed.

This makes the algorithm easy to reason about and reuse for insertion and deletion.

## 10. Interview-ready explanation

If an interviewer asks, “How does searching work in a skip list?”, a strong answer is:

> A skip list is a sorted, multi-level linked list. Search begins at the highest active level and moves forward while the next node's key is smaller than the target. When the next node would overshoot the target, the algorithm descends one level without changing its current position. It continues until the bottom level, where it checks the next node for an exact match. Because higher levels provide shortcuts and the number of levels is logarithmic in expectation, search takes expected \\(O(\log n)\\) time, although the worst case is \\(O(n)\\).

This answer covers the mechanism, the reason for the performance, and the worst-case qualification.

## Chapter 3 — Final takeaways

The core search loop is:

```
for (int level = currentLevel - 1; level >= 0; level--) {
    while (current.forward[level] != null
            && current.forward[level].key < target) {
        current = current.forward[level];
    }
}
```

Remember three rules:

1. Move forward when the next key is strictly smaller than the target.
2. Move down when moving forward would overshoot it.
3. Check the bottom-level successor to determine whether the target exists.

Once you understand this search path, insertion and deletion become natural extensions: they first locate the relevant predecessors and then adjust the pointers.

Next: Chapter 4 — Insertion and deletion, step by step. We'll use a predecessor-update array to show exactly which pointers change, how a new node's height affects insertion, and why ordinary insertions and deletions do not require rebuilding the skip list.

