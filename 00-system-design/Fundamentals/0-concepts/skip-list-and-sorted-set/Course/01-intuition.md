# Course: Skip List — From Zero to Mastery

A complete, intuition-first course covering how skip lists work internally, why they achieve \\(O(\log n)\\) expected operations, how their randomized structure works, how they compare with other data structures, and how Redis uses them in production.

The goal is not just to memorize their time complexities. By the end, you should be able to draw a skip list, trace every pointer during search and insertion, explain the probability behind its performance, implement one, and defend when to choose it in a system design interview.

## Course roadmap

| Chapter | Topics                                                                       |
| ------- | ---------------------------------------------------------------------------- |
| 1       | The intuition behind skip lists and how they work                            |
| 2       | Internal structure: levels, nodes, forward pointers, and invariants          |
| 3       | Search operation, step-by-step                                               |
| 4       | Insertion and deletion, including pointer updates                            |
| 5       | Randomization and the mathematics behind \\(O(\log n)\\)                     |
| 6       | Time complexity, space complexity, and worst-case behavior                   |
| 7       | Skip lists vs. linked lists, arrays, hash tables, and balanced trees         |
| 8       | Redis internals: sorted sets, skip lists, hash tables, and rank queries      |
| 9       | Implementing a skip list in Java, with testing and edge cases                |
| 10      | Real-world use cases, concurrency, interview questions, and design decisions |

# Chapter 1 — Build the intuition

## 1. What problem does a skip list solve?

Imagine that you need to store one million integers in sorted order.

For example:

`10, 20, 30, 40, 50, 60, 70, 80, ...`

You frequently need to:

- Find whether a value exists.
- Insert a new value while maintaining sorted order.
- Delete an existing value.
- Find the first value greater than or equal to a target.
- Traverse the values in sorted order.

These operations are common in databases, leaderboards, ordered indexes, and scheduling systems.

Let's begin with a simple data structure and see why we need something better.

### Approach 1: A sorted linked list

A sorted singly linked list stores each value with a pointer to the next node.

```

HEAD → 10 → 20 → 30 → 40 → 50 → 60 → 70 → 80 → NULL

```

Suppose we want to find `70`.

We start at the head and move forward:

1. Visit `10`.
2. Visit `20`.
3. Visit `30`.
4. Visit `40`.
5. Visit `50`.
6. Visit `60`.
7. Visit `70`.

We needed seven node visits.

In the worst case, finding a value near the end requires traversing almost every node.

For \\(n\\) elements:

| Operation                                             | Sorted linked list                      |
| ----------------------------------------------------- | --------------------------------------- |
| Search                                                | \\(O(n)\\)                              |
| Insert at a known position                            | \\(O(1)\\), after locating the position |
| Insert at an arbitrary sorted position                | \\(O(n)\\)                              |
| Delete a known node when its predecessor is available | \\(O(1)\\)                              |
| Delete an arbitrary value                             | \\(O(n)\\)                              |
| Traverse in sorted order                              | \\(O(n)\\)                              |

The problem isn't that linked lists are incapable of storing sorted data. They can.

The problem is that each node normally provides only one forward step.

Can we keep the linked list but add shortcuts?

That is the central idea behind a skip list.

## 2. The intuition: Add express lanes

Imagine a railway network.

A local train stops at every station:

`Station 10 → 20 → 30 → 40 → 50 → 60 → 70 → 80`

An express train stops only at selected stations:

`Station 10 → 30 → 50 → 70`

An even faster service might stop at just a few major stations:

`Station 10 → 50 → 90`

To reach station 70, you would not travel through every intermediate station. You would use the express service to get close and then switch to a slower service for the final part.

A skip list applies this idea to linked lists.

A skip list is a sorted linked list augmented with multiple levels of forward pointers, allowing searches to skip over many intermediate nodes.

The bottom level contains every node. Higher levels contain progressively fewer nodes.

Consider a skip list with the following values.

```

Level 3:  HEAD ─────────────→ 40 ─────────────→ 70 ──→ NULL

Level 2:  HEAD ──→ 20 ──────→ 40 ──────→ 60 ──→ 70 ──→ NULL

Level 1:  HEAD → 10 → 20 → 30 → 40 → 50 → 60 → 70 → 80 → NULL

```

Read each level separately.

Level 1: Every value is present, so it behaves like a normal linked list.

Level 2: Some nodes have longer-range pointers. We can move between `20`, `40`, `60`, and `70` without visiting every bottom-level node.

Level 3: There are even fewer nodes, so we can cover larger distances with fewer hops.

The top level is not a separate collection of values. It consists of pointers belonging to selected nodes in the same underlying list.

This distinction is important: the same node can participate in several levels.

The diagram is an illustrative arrangement, not a rule that every third or fourth node must appear at a particular level.

## 3. What exactly is stored inside a node?

A normal singly linked-list node might look like this:

```

class Node {
    int value;
    Node next;
}

```

It contains a value and one pointer.

A skip-list node instead has a collection of forward pointers:

```

class SkipListNode {
    int value;
    SkipListNode[] forward;
}

```

Conceptually, `forward` stores one pointer per level supported by that node.

For example, a node with three levels might contain:

| Pointer      | Destination              |
| ------------ | ------------------------ |
| `forward[0]` | The next node at level 1 |
| `forward[1]` | The next node at level 2 |
| `forward[2]` | The next node at level 3 |

We normally use zero-based indexing in code, so `forward[0]` is the bottom-level pointer.

Consider node `40` from our diagram:

- Its level-1 pointer leads to `50`.
- Its level-2 pointer leads to `60`.
- Its level-3 pointer leads to `70`.

It can therefore make short, medium, or long jumps depending on the current level.

Notice that these pointers need not point to nodes at a fixed numerical distance. The pointers connect to the next available node that participates in that level.

For instance, node `40` could jump directly to `70` at level 3 even though the values between them are `50` and `60`.

That flexibility is essential to the design.

## 4. How do we search in a skip list?

Let's search for `65` in our example.

The target does not exist, so this search will also help us find where it would be inserted.

The basic algorithm is:

1. Start at the head on the highest level.
2. Move forward while the next node's value is less than or equal to the target.
3. If the next node is greater than the target, move down one level.
4. Repeat until reaching level 1.
5. Check the next bottom-level node to determine whether the target exists.

We move forward when it is safe, and downward when the next forward step would overshoot the target.

### Step 1: Start at level 3

The highest-level pointer from `HEAD` leads to `40`.

Since \\(40 < 65\\), we can move to `40`.

The next level-3 node is `70`.

Since \\(70 > 65\\), we cannot move to `70`. It would take us past the target.

We descend to level 2 at node `40`.

### Step 2: Continue at level 2

At node `40`, the level-2 pointer leads to `60`.

Because \\(60 < 65\\), we move to `60`.

The next level-2 node is `70`, which is greater than `65`.

We descend to level 1 at node `60`.

### Step 3: Check the bottom level

The level-1 pointer from `60` leads to `70`.

Since \\(70 > 65\\), we stop. The target is not present.

The insertion position would be between `60` and `70`.

Our search path was:

```

Level 3:  HEAD → 40
                   ↓
Level 2:          40 → 60
                       ↓
Level 1:              60 → 70
                            ↑
                    Target 65 belongs here

```

We did not examine `10`, `20`, `30`, or `50`.

A regular linked list would have to visit each of those nodes when searching from the head.

In a larger skip list, these shortcuts help reduce the number of nodes visited dramatically.

### A small but important detail

The comparison depends on the next node, not just the current node.

At each level, we ask:

> Does the next node exist, and is its value no greater than my target?

If yes, move forward. Otherwise, descend.

This ensures the search does not skip over the desired value.

When duplicates are allowed, the comparison rules must be chosen carefully, depending on whether the search should find the first occurrence, the last occurrence, or a specific entry. We will cover ordering and duplicates in the implementation chapters.

## 5. Why is a skip list faster?

Now let's connect the intuition to complexity.

A linked list with \\(n\\) elements can require \\(O(n)\\) node visits for a search.

A skip list introduces progressively sparser levels, enabling it to cover large portions of the list with fewer forward steps.

Suppose we have \\(1{,}024\\) elements, and imagine a perfectly structured arrangement in which each higher level contains approximately half as many nodes as the level below it.

The number of levels would be roughly:

\\[ \log_2(1024)=10 \\]

At a high level, we can make large jumps. At each lower level, we refine our position.

A search through these levels can therefore take approximately logarithmic work rather than visiting all \\(1{,}024\\) nodes.

Compare the growth:

| Number of elements \\(n\\) | Linear work \\(O(n)\\) | Logarithmic work \\(O(\log_2 n)\\) |
| -------------------------- | ---------------------- | ---------------------------------- |
| 1,024                      | 1,024                  | 10                                 |
| 1,000,000                  | 1,000,000              | About 20                           |
| 1,000,000,000              | 1,000,000,000          | About 30                           |

These are illustrative growth comparisons, not exact counts of the node visits made by a real skip list. Actual costs depend on its randomly chosen levels and the position of the target.

The core intuition is that each additional level provides a way to bypass many nodes that would otherwise require individual visits.

### But here is the catch

We cannot simply add a pointer that jumps exactly 100 positions from every node.

Why?

Imagine that every node has a pointer to the node 100 positions ahead. Insert a new element near the beginning, and the positional relationships of all subsequent nodes shift. Maintaining that fixed-distance pattern could require widespread changes.

We need a design that provides shortcuts without requiring us to rebuild them whenever the data changes.

That leads to the defining feature of skip lists: randomized levels.

## 6. How do we create the levels without manually balancing them?

Suppose we insert these values, one by one:

`10, 20, 30, 40, 50, 60, 70, 80`

We do not decide that every fourth node must have a level-2 pointer or that every eighth node must have a level-3 pointer.

Instead, whenever a new node is inserted, we randomly choose how many levels it will participate in.

For example:

| Value | Randomly selected height |
| ----- | ------------------------ |
| 10    | 1                        |
| 20    | 2                        |
| 30    | 1                        |
| 40    | 3                        |
| 50    | 1                        |
| 60    | 2                        |
| 70    | 3                        |
| 80    | 1                        |

Here, height means the number of levels in which a node participates.

A height-1 node has a bottom-level pointer. A height-3 node can have pointers at levels 1, 2, and 3.

In a common configuration, a newly inserted node has:

- A 50% probability of having height 1.
- A 25% probability of having height 2.
- A 12.5% probability of having height 3.
- A 6.25% probability of having height 4.

And so on.

These probabilities come from a simple procedure: start at level 1 and repeatedly flip a probabilistic choice to determine whether to promote the node to another level.

As a result, most nodes remain short, while a small fraction become tall enough to support long jumps.

We achieve balance probabilistically instead of explicitly rearranging the entire structure or rotating nodes as balanced trees do.

The key is that a node's height is normally assigned when it is inserted and remains fixed throughout its lifetime.

We will derive these probabilities and examine why they yield expected \\(O(\log n)\\) performance in Chapter 5.

## 7. Does randomization mean that the elements are randomly ordered?

No. This is a common misconception.

There are two independent things:

The values' order: The bottom level always keeps the elements sorted according to the skip list's ordering rule.

The nodes' heights: The number of levels assigned to each inserted node is chosen randomly.

For example, even if we insert values in ascending order:

`10, 20, 30, 40, 50, 60, 70, 80`

the bottom-level sequence remains sorted.

The random choices determine which nodes can be used as shortcuts, not where the values appear in sorted order.

This means the insertion order does not need to be randomized to obtain the expected performance characteristics.

There can still be unlucky random arrangements that produce poor performance, but the design makes those arrangements unlikely under the usual assumptions. The original skip-list paper explains this probabilistic balancing approach as an alternative to strictly maintained balanced trees.&#x20;

[image](https://www.google.com/s2/favicons?domain=https://g-trees.github.io\&sz=32)

g-trees.github.io



## 8. What makes a skip list different from simply adding shortcuts?

This is the most important conceptual distinction in this chapter.

Imagine that we have a sorted linked list and want to add a new value, `35`.

Our existing bottom-level order is:

`10 → 20 → 30 → 40 → 50`

After insertion:

`10 → 20 → 30 → 35 → 40 → 50`

In a skip list, a new node receives its height, perhaps height 2. We then update the existing predecessor pointers at levels 1 and 2.

We do not need to change pointers at levels 3, 4, or higher if the new node does not participate in those levels.

Only the links affected by the new node need to be updated.

This gives skip lists a useful combination:

- A sorted structure that supports efficient searches.
- Additional pointers that provide shortcuts.
- Local modifications during insertion and deletion.
- No need to maintain strict tree-balancing rules or rotate tree nodes.

The actual insertion algorithm must locate the predecessor at every level the new node will occupy. We will trace those pointer changes in detail in Chapter 4.

## 9. What are the time complexities?

At this stage, we can introduce the expected performance profile.

| Operation             | Skip list complexity                                |
| --------------------- | --------------------------------------------------- |
| Search                | Expected \\(O(\log n)\\)                            |
| Insert                | Expected \\(O(\log n)\\)                            |
| Delete                | Expected \\(O(\log n)\\)                            |
| Find minimum          | \\(O(1)\\), with a direct pointer to the first node |
| Find maximum          | \\(O(1)\\) if a tail pointer is maintained          |
| Traverse all elements | \\(O(n)\\)                                          |
| Space                 | Expected \\(O(n)\\)                                 |

The distinction between expected and worst-case performance is important.

A skip list is not guaranteed to execute every search in \\(O(\log n)\\) time. An unlucky arrangement of node heights can leave the structure close to a regular linked list, giving \\(O(n)\\) search time in the worst case.

Its expected logarithmic complexity depends on the randomized level assignment and the usual assumptions behind the analysis.

We will prove the expected complexity rather than treating it as a fact to memorize.

## 10. Where does Redis use skip lists?

Skip lists are especially useful when an application needs to maintain an ordered collection that changes frequently.

Redis provides a practical example through its sorted sets, commonly called ZSETs.

A Redis sorted set stores unique members, each associated with a numerical score. Members are ordered by score, with lexicographical ordering used to break ties between equal scores. Typical applications include game leaderboards and sliding-window rate limiters.&#x20;

[image](https://www.google.com/s2/favicons?domain=https://redis.io\&sz=32)

Docs



Consider a leaderboard:

| Member  | Score |
| ------- | ----- |
| Alice   | 120   |
| Bob     | 175   |
| Charlie | 150   |
| Diana   | 200   |

A sorted-set ordering is:

`Alice(120) → Charlie(150) → Bob(175) → Diana(200)`

If Bob's score changes from 175 to 220, the ordered structure must reflect the new position. Simply storing all members in an ordinary hash table would not directly provide an ordered traversal.

Redis's skip-list implementation helps maintain score order while supporting operations such as insertion, removal, and range retrieval.

Redis's internal implementation also uses a hash table alongside the skip list in its traditional skip-list-backed representation. The hash table maps members to their associated data, while the skip list provides the ordered view.&#x20;

[image](https://www.google.com/s2/favicons?domain=https://github.com\&sz=32)

GitHub



There are several interesting implementation details to explore later:

- How Redis handles multiple members with the same score.
- Why a skip-list node uses more than a simple `value` and `next` pointer.
- How backward pointers support reverse traversal.
- How spans support efficient rank-related operations.
- Why Redis can combine an ordered structure with a hash table.
- How Redis's compact representation for smaller sorted sets differs from its skip-list-backed representation.

We will examine these internals in Chapter 8.

## 11. What should you remember from Chapter 1?

A skip list is a sorted linked list enhanced with several levels of forward pointers.

Every element appears on the bottom level, while only a subset of elements appears at higher levels. These higher-level pointers allow searches to skip over intermediate nodes.

Instead of assigning levels according to fixed positions, a skip list chooses node heights randomly. This makes it possible to obtain expected logarithmic search, insertion, and deletion performance while updating only the affected links.

The most important mental model is:

> Move forward while you can do so without passing the target. When you cannot move forward safely, move down one level and continue.

This is the foundation for understanding its search algorithm, pointer updates, and complexity analysis.

### Quick self-check

Before moving on, try answering these questions without looking back:

1. Why is searching a regular linked list \\(O(n)\\)?
2. Why does a skip list keep every element on level 1?
3. Why do higher levels improve search performance?
4. Does randomization change the sorted order of the values?
5. Why is a skip list's performance expected \\(O(\log n)\\), rather than guaranteed \\(O(\log n)\\) in every case?
6. Why might an ordered skip list be useful for a Redis leaderboard?

The next chapter covers the internal structure in more detail: node height, level invariants, the head sentinel, forward-pointer relationships, and how to reason about a skip list precisely before implementing its algorithms.

Type `next` to continue to Chapter 2.





