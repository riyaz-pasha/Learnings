# Chapter 9 — Implement a Skip List in Java

Now let's implement a complete skip list from scratch.

The objective is to understand every operation, not just copy code. We will build an implementation, visualize the levels, test pointer updates, verify the structural invariants, and compare our skip list with Java's `TreeSet`.

We'll implement a skip list that stores unique integer keys in sorted order.

Our implementation will support:

- `add(key)` — insert a key.
- `contains(key)` — check whether a key exists.
- `remove(key)` — delete a key.
- `ceiling(key)` — find the smallest key greater than or equal to the target.
- `range(from, to)` — retrieve keys within an inclusive range.
- `toList()` — retrieve all keys in sorted order.
- `printLevels()` — visualize the internal structure.
- `validateInvariants()` — verify that all levels and pointers are correct.

## 1. Start with the node

Recall that a regular linked-list node has a single next pointer.

A skip-list node needs a variable number of forward pointers.

```java
private static final class Node {
    final int key;
    final Node[] forward;

    Node(int key, int height) {
        this.key = key;
        this.forward = new Node[height];
    }
}
```

Suppose we create these nodes:

```
new Node(10, 1);
new Node(20, 3);
new Node(30, 2);
```

Their pointer arrays have different sizes:

```
Node 10, height 1:
    forward[0]

Node 20, height 3:
    forward[0]
    forward[1]
    forward[2]

Node 30, height 2:
    forward[0]
    forward[1]
```

Remember that Java arrays use zero-based indexing.

`forward[0]` is always the bottom-level pointer.

A node of height 3 participates in levels 1, 2, and 3, using indices 0, 1, and 2 respectively.

The node's height is selected at insertion time and does not change afterward.

## 2. The complete implementation

Save the following code in a file named `SkipListDemo.java`. It uses only the Java standard library.

The implementation includes a configurable maximum height, deterministic random seeds for reproducible testing, and a test harness that compares its behavior with `TreeSet`.

```java

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;
import java.util.Random;
import java.util.TreeSet;

public final class SkipListDemo {

    public static void main(String[] args) {
        demonstrateOperations();
        runRandomizedTests();

        System.out.println("\nAll tests passed.");
    }

    private static void demonstrateOperations() {
        SkipList list = new SkipList(16, 42L);

        int[] initialKeys = {10, 20, 30, 40, 50, 60, 70, 80, 90};

        for (int key : initialKeys) {
            check(list.add(key), "Initial insertion failed: " + key);
        }

        System.out.println("Initial skip list:");
        list.printLevels();

        check(list.contains(40), "40 should exist");
        check(!list.contains(45), "45 should not exist");

        check(
            list.ceiling(41).orElseThrow() == 50,
            "Ceiling of 41 should be 50"
        );

        check(
            list.ceiling(100).isEmpty(),
            "There should be no ceiling for 100"
        );

        check(
            list.range(25, 65).equals(List.of(30, 40, 50, 60)),
            "Incorrect inclusive range"
        );

        // Insert a new node.
        check(list.add(35), "35 should be inserted");

        // Duplicate keys are not allowed.
        check(!list.add(35), "Duplicate insertion should fail");

        // Delete an existing node.
        check(list.remove(40), "40 should be removed");

        // Deleting a missing key should return false.
        check(!list.remove(999), "Missing key should not be removed");

        List<Integer> expected =
            List.of(10, 20, 30, 35, 50, 60, 70, 80, 90);

        check(list.toList().equals(expected), "Incorrect final ordering");

        list.validateInvariants();

        System.out.println("\nAfter inserting 35 and removing 40:");
        list.printLevels();

        System.out.println("\nSorted contents: " + list.toList());
        System.out.println("Size: " + list.size());
    }

    private static void runRandomizedTests() {
        SkipList actual = new SkipList(16, 123L);
        TreeSet<Integer> expected = new TreeSet<>();
        Random operations = new Random(7L);

        // Repeatedly compare the skip list with a trusted sorted set.
        for (int i = 0; i < 2_000; i++) {
            int key = operations.nextInt(500);

            if (operations.nextBoolean()) {
                boolean actualResult = actual.add(key);
                boolean expectedResult = expected.add(key);

                check(
                    actualResult == expectedResult,
                    "Insertion result mismatch at key " + key
                );
            } else {
                boolean actualResult = actual.remove(key);
                boolean expectedResult = expected.remove(key);

                check(
                    actualResult == expectedResult,
                    "Deletion result mismatch at key " + key
                );
            }

            check(
                actual.toList().equals(new ArrayList<>(expected)),
                "Collection contents mismatch after operation " + i
            );

            actual.validateInvariants();
        }

        check(
            actual.size() == expected.size(),
            "Final size mismatch"
        );
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    // A skip list with unique integer keys.
    //
    // This implementation is NOT thread-safe.
    private static final class SkipList {

        private static final double PROMOTION_PROBABILITY = 0.5;
        private static final int DEFAULT_MAX_LEVEL = 32;

        private final int maxLevel;
        private final Random random;
        private final Node head;

        // Number of active levels, not the number of data nodes.
        // The bottom level is always active.
        private int currentLevel = 1;

        private int size;

        private static final class Node {
            final int key;
            final Node[] forward;

            Node(int key, int height) {
                this.key = key;
                this.forward = new Node[height];
            }
        }

        public SkipList() {
            this(DEFAULT_MAX_LEVEL, new Random());
        }

        public SkipList(long seed) {
            this(DEFAULT_MAX_LEVEL, new Random(seed));
        }

        public SkipList(int maxLevel, long seed) {
            this(maxLevel, new Random(seed));
        }

        private SkipList(int maxLevel, Random random) {
            if (maxLevel < 1) {
                throw new IllegalArgumentException(
                    "Maximum level must be at least 1"
                );
            }

            this.maxLevel = maxLevel;
            this.random = random;
            this.head = new Node(0, maxLevel);
        }

        public int size() {
            return size;
        }

        public boolean isEmpty() {
            return size == 0;
        }

        // Returns true only when a new key is inserted.
        public boolean add(int key) {
            Node[] update = new Node[maxLevel];
            Node current = head;

            // Phase 1: Find the predecessor at each active level.
            for (int level = currentLevel - 1; level >= 0; level--) {
                while (current.forward[level] != null
                        && current.forward[level].key < key) {
                    current = current.forward[level];
                }

                update[level] = current;
            }

            // The bottom-level successor is the insertion candidate.
            Node candidate = current.forward[0];

            if (candidate != null && candidate.key == key) {
                return false;
            }

            // Phase 2: Choose the height for the new node.
            int nodeHeight = randomLevel();

            // Phase 3: Initialize predecessors for newly active levels.
            for (int level = currentLevel; level < nodeHeight; level++) {
                update[level] = head;
            }

            // Phase 4: Update the active height of the skip list.
            currentLevel = Math.max(currentLevel, nodeHeight);

            Node newNode = new Node(key, nodeHeight);

            // Phase 5: Connect the new node at every level it occupies.
            for (int level = 0; level < nodeHeight; level++) {
                newNode.forward[level] = update[level].forward[level];
                update[level].forward[level] = newNode;
            }

            size++;
            return true;
        }

        public boolean contains(int key) {
            Node current = head;

            // Search from the highest active level to the bottom.
            for (int level = currentLevel - 1; level >= 0; level--) {
                while (current.forward[level] != null
                        && current.forward[level].key < key) {
                    current = current.forward[level];
                }
            }

            // The successor of the final predecessor is the candidate.
            Node candidate = current.forward[0];

            return candidate != null && candidate.key == key;
        }

        // Returns true only when an existing key is removed.
        public boolean remove(int key) {
            Node[] update = new Node[maxLevel];
            Node current = head;

            // Phase 1: Find predecessors at every active level.
            for (int level = currentLevel - 1; level >= 0; level--) {
                while (current.forward[level] != null
                        && current.forward[level].key < key) {
                    current = current.forward[level];
                }

                update[level] = current;
            }

            // Phase 2: Find the target on the bottom level.
            Node target = current.forward[0];

            if (target == null || target.key != key) {
                return false;
            }

            // Phase 3: Bypass the target at every level it occupies.
            for (int level = 0; level < target.forward.length; level++) {
                if (update[level].forward[level] == target) {
                    update[level].forward[level] = target.forward[level];
                }
            }

            // Phase 4: Remove empty top levels.
            while (currentLevel > 1
                    && head.forward[currentLevel - 1] == null) {
                currentLevel--;
            }

            size--;
            return true;
        }

        // Returns the smallest key >= target, or empty if none exists.
        public OptionalInt ceiling(int target) {
            Node current = head;

            for (int level = currentLevel - 1; level >= 0; level--) {
                while (current.forward[level] != null
                        && current.forward[level].key < target) {
                    current = current.forward[level];
                }
            }

            Node candidate = current.forward[0];

            return candidate == null
                ? OptionalInt.empty()
                : OptionalInt.of(candidate.key);
        }

        // Returns all keys in the inclusive interval [from, to].
        public List<Integer> range(int from, int to) {
            List<Integer> result = new ArrayList<>();

            if (from > to) {
                return result;
            }

            Node current = head;

            // Locate the predecessor of the lower bound.
            for (int level = currentLevel - 1; level >= 0; level--) {
                while (current.forward[level] != null
                        && current.forward[level].key < from) {
                    current = current.forward[level];
                }
            }

            // Traverse only the matching bottom-level entries.
            current = current.forward[0];

            while (current != null && current.key <= to) {
                result.add(current.key);
                current = current.forward[0];
            }

            return result;
        }

        public List<Integer> toList() {
            List<Integer> result = new ArrayList<>();
            Node current = head.forward[0];

            while (current != null) {
                result.add(current.key);
                current = current.forward[0];
            }

            return result;
        }

        public void printLevels() {
            for (int level = currentLevel - 1; level >= 0; level--) {
                StringBuilder line = new StringBuilder();
                line.append("Level ")
                    .append(level + 1)
                    .append(": HEAD");

                Node current = head.forward[level];

                while (current != null) {
                    line.append(" -> ").append(current.key);
                    current = current.forward[level];
                }

                line.append(" -> NIL");
                System.out.println(line);
            }
        }

        // Verifies sorted order, node heights, level membership,
        // immediate-next-node pointers, size, and active height.
        public void validateInvariants() {
            List<Node> bottomNodes = new ArrayList<>();
            Node current = head.forward[0];

            // Collect the complete bottom-level sequence.
            while (current != null) {
                if (bottomNodes.size() >= size) {
                    throw new IllegalStateException(
                        "Cycle or incorrect size in bottom level"
                    );
                }

                if (!bottomNodes.isEmpty()
                        && bottomNodes.get(bottomNodes.size() - 1).key
                            >= current.key) {
                    throw new IllegalStateException(
                        "Bottom level is not strictly sorted"
                    );
                }

                bottomNodes.add(current);
                current = current.forward[0];
            }

            if (bottomNodes.size() != size) {
                throw new IllegalStateException("Incorrect stored size");
            }

            int maximumObservedHeight = 1;

            for (Node node : bottomNodes) {
                if (node.forward.length < 1
                        || node.forward.length > maxLevel) {
                    throw new IllegalStateException(
                        "Invalid node height"
                    );
                }

                maximumObservedHeight = Math.max(
                    maximumObservedHeight,
                    node.forward.length
                );
            }

            if (currentLevel != maximumObservedHeight) {
                throw new IllegalStateException(
                    "Incorrect active level"
                );
            }

            // Every level must contain precisely the bottom-level nodes
            // whose heights reach that level, in the same sorted order.
            for (int level = 0; level < currentLevel; level++) {
                List<Node> expected = new ArrayList<>();

                for (Node node : bottomNodes) {
                    if (node.forward.length > level) {
                        expected.add(node);
                    }
                }

                List<Node> actual = new ArrayList<>();
                current = head.forward[level];

                while (current != null) {
                    if (actual.size() >= size) {
                        throw new IllegalStateException(
                            "Cycle detected at level " + (level + 1)
                        );
                    }

                    if (current.forward.length <= level) {
                        throw new IllegalStateException(
                            "Node appears above its assigned height"
                        );
                    }

                    actual.add(current);
                    current = current.forward[level];
                }

                if (actual.size() != expected.size()) {
                    throw new IllegalStateException(
                        "Incorrect number of nodes at level "
                            + (level + 1)
                    );
                }

                for (int i = 0; i < expected.size(); i++) {
                    if (actual.get(i) != expected.get(i)) {
                        throw new IllegalStateException(
                            "Incorrect forward link at level "
                                + (level + 1)
                        );
                    }
                }
            }
        }

        private int randomLevel() {
            int height = 1;

            while (height < maxLevel
                    && random.nextDouble() < PROMOTION_PROBABILITY) {
                height++;
            }

            return height;
        }
    }
}

```

Run it with:

```
javac SkipListDemo.java
java SkipListDemo
```

The random level choices are seeded for the demonstration, so the example is reproducible. Your own implementation can use the no-argument constructor, which uses a normally seeded `Random`.

## 3. Understand insertion: the `update[]` array

Let's return to the most important part of insertion.

Suppose the list contains:

```
Level 3: HEAD ─────→ 20 ──────────────→ 60

Level 2: HEAD ─────→ 20 ─→ 40 ────────→ 60

Level 1: HEAD → 10 → 20 → 30 → 40 → 50 → 60
```

We want to insert `35`.

Assume its randomly assigned height is 2.

### Step 1: Search and remember the predecessors

Our traversal finds:

```
update[2] = 20
update[1] = 20
update[0] = 30
```

In this example, `update[]` stores pointers to the predecessor at each level. The new node might not participate in every level where a predecessor was discovered.

### Step 2: Connect the new node to its successor

At level 1:

```
newNode.forward[0] = update[0].forward[0];
```

This sets the new node's successor to `40`.

Then:

```
update[0].forward[0] = newNode;
```

This changes node `30` to point to the new node.

The result is:

```
30 → 35 → 40
```

### Step 3: Repeat at level 2

At level 2, the predecessor is `20`.

The same two assignments create:

```
20 → 35 → 40
```

Level 3 is not modified because the new node does not participate in it.

That is the entire insertion mechanism.

Find predecessors first, then update only the levels occupied by the new node.

## 4. Understand deletion

Suppose we now remove `40`.

The deletion algorithm first finds the predecessor at every active level, just as insertion does.

It then redirects the predecessor's pointer to the node after `40`.

Before:

```
30 → 35 → 40 → 50
```

After:

```
30 → 35 → 50
```

The same bypass occurs at every level where node `40` participates.

The implementation uses this loop:

```java
for (int level = 0; level < target.forward.length; level++) {
    if (update[level].forward[level] == target) {
        update[level].forward[level] = target.forward[level];
    }
}
```

The condition verifies that the predecessor really points to the target at that level before redirecting it.

Once the target has been removed, the implementation decreases the active level if the highest levels have become empty.

Other nodes keep their assigned heights. No tree rotations or explicit balancing procedure is required.

## 5. Why is the code structured this way?

Let's look at the choices that make the implementation easier to reason about.

### Why use `currentLevel`?

`maxLevel` is the configured upper bound. `currentLevel` is the highest level currently populated.

If the tallest node has height 4, we begin searching at level index 3, not at unused higher levels.

### Why store the predecessor rather than just the matching node?

Because insertion and deletion modify the link before a node.

For example, to insert `35` between `30` and `40`, we must change `30`'s forward pointer.

The predecessor array preserves the location needed for this modification at each level.

### Why make `key` and `forward` final?

A node's key and height should not change after construction.

The `forward` array itself remains mutable because its entries must change during insertion and deletion. Declaring the array reference `final` prevents replacing the array, not updating its contents.

This makes the node's structural properties easier to maintain.

### Why return a boolean from `add()` and `remove()`?

The collection represents unique keys.

- `add(key)` returns `true` only if the key was newly inserted.
- `remove(key)` returns `true` only if the key existed and was deleted.

This gives callers a simple way to detect duplicate insertion attempts and unsuccessful deletions.

### Why does `ceiling()` return `OptionalInt`?

The smallest key greater than or equal to a target might not exist.

`OptionalInt` represents both cases without reserving a valid integer value as a special sentinel. For example, `Integer.MIN_VALUE` can still be a legitimate key.

## 6. Complexity of our implementation

Let \\(n\\) be the number of keys, and assume that the configured maximum level is appropriate for the supported collection size.

| Method            | Expected time     | Worst case   |
| ----------------- | ----------------- | ------------ |
| `add()`           | \\(O(\log n)\\)   | \\(O(n)\\)   |
| `contains()`      | \\(O(\log n)\\)   | \\(O(n)\\)   |
| `remove()`        | \\(O(\log n)\\)   | \\(O(n)\\)   |
| `ceiling()`       | \\(O(\log n)\\)   | \\(O(n)\\)   |
| `range(from, to)` | \\(O(\log n+k)\\) | \\(O(n+k)\\) |
| `toList()`        | \\(O(n)\\)        | \\(O(n)\\)   |
| `size()`          | \\(O(1)\\)        | \\(O(1)\\)   |
| `isEmpty()`       | \\(O(1)\\)        | \\(O(1)\\)   |

Here, \\(k\\) is the number of elements returned by the range query.

The expected bounds follow from randomized levels, while worst-case operations can become linear when the structure has insufficiently useful shortcuts.

One important implementation qualification: this code uses a finite, configurable maximum height. In a real system, select that limit for the expected collection size. The asymptotic logarithmic analysis assumes the available height can grow appropriately with the collection rather than remaining too small forever.

Space usage is expected \\(O(n)\\) for a fixed promotion probability, including the forward pointers, excluding temporary arrays and the output lists created by methods such as `toList()`.

## 7. The value of `validateInvariants()`

This method is worth studying carefully.

Skip-list bugs often don't appear immediately. For example, an insertion might update the bottom-level pointer correctly while accidentally skipping a node on level 2.

The displayed sorted result might look correct, but future searches could miss keys.

Our validation method checks that:

1. The bottom-level list is sorted.
2. The number of bottom-level nodes equals `size`.
3. Every node has a valid height.
4. The current active level matches the tallest node.
5. Each higher level contains exactly the bottom-level nodes whose heights reach that level.
6. Each level preserves the order of the bottom-level nodes.

That fifth invariant is particularly important.

For every level \\(i\\), the nodes participating in that level must be exactly the nodes whose heights are at least \\(i+1\\), in their original sorted order.

This verifies that a pointer doesn't accidentally skip a node that should participate in that level.

The checker is intentionally expensive and intended for testing, not for execution after every operation in production.

## 8. Why compare against `TreeSet`?

A good data-structure implementation should be tested against a trusted reference implementation.

Our demonstration creates two collections:

```
SkipList actual = new SkipList(16, 123L);
TreeSet<Integer> expected = new TreeSet<>();
```

It generates random insertion and deletion operations and applies them to both.

After each operation, it checks:

- Whether both collections report the same insertion or deletion result.
- Whether both contain the same keys in the same sorted order.
- Whether all the skip-list invariants still hold.

This is a form of differential testing.

Instead of manually determining the correct output for every random operation, we compare our implementation against a reference with well-understood sorted-set behavior.

You should use this technique when implementing complex data structures. It is excellent at exposing pointer-update bugs, especially when operations occur in different orders.

For more exhaustive testing, extend the harness to include repeated `contains()` calls, ranges, empty-list cases, and boundary values such as `Integer.MIN_VALUE` and `Integer.MAX_VALUE`.

## 9. Important limitations of this implementation

This is a complete educational implementation, but it has several deliberate limits.

It is not thread-safe. Concurrent reads and writes require synchronization or a dedicated concurrent algorithm.

It accepts unique integer keys. A production implementation that stores arbitrary objects would typically use a comparator and a separate representation for associated values.

It is not a complete Redis-style sorted set. Redis uses compound ordering by score and member, a dictionary for member lookup, backward links, and spans for rank calculations. Our implementation focuses on understanding the core skip-list mechanics.

Its maximum height must be chosen appropriately. The configurable height bound should suit the intended collection size.

Understanding these limitations helps distinguish a useful learning implementation from a production-ready collection for a particular workload.

## 10. Test your understanding

Before continuing, see whether you can answer these without looking at the code.

Question 1: Why does insertion need `update[]`?

It remembers the predecessor at each level so the algorithm can modify the correct forward links without searching again.

Question 2: Why does a new node's height remain fixed?

The node's height is selected when it is inserted. Other insertions and deletions update links but do not ordinarily change the heights of existing nodes.

Question 3: Why does deletion iterate up to `target.forward.length`?

A node participates only in the levels represented by its forward-pointer array, so it only needs to be bypassed at those levels.

Question 4: What is the difference between `maxLevel` and `currentLevel`?

`maxLevel` is the configured maximum supported height. `currentLevel` is the highest level currently occupied by at least one data node, with level 1 retained as the baseline for an empty list.

Question 5: What is the most important invariant to verify after insertion?

Every level must contain exactly the nodes whose assigned heights reach that level, in sorted order, with each forward pointer leading to the next participating node.

## Chapter 9 — The main lesson

A skip list becomes much easier to implement once you separate its responsibilities.

The node stores a key and its forward pointers. Random promotion chooses the node's height. Search discovers the predecessors. Insertion links the new node into the levels it occupies. Deletion bypasses the target. The invariants ensure that the sorted order and level relationships remain correct.

The `update[]` array is the most important implementation pattern to remember because it connects the search algorithm to both insertion and deletion.

You should now be able to implement a standard skip list without memorizing the code line by line.

Next: Chapter 10 — Production use cases, concurrency, limitations, advanced variants, and interview mastery. We'll tie the full course together and examine what changes when the structure is used in concurrent applications, databases, and high-performance systems.

