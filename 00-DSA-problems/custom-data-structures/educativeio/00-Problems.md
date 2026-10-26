# Custom Data Structures Pattern: Correct Problem Order

The goal of this pattern is to learn how to design a data structure that supports a specific set of operations efficiently, often in \\(O(1)\\) or \\(O(\log n)\\) time.

The important distinction is that not every problem asking you to design a data structure belongs to the same pattern. Some primarily teach hashing, some require doubly linked lists, some require heaps or balanced trees, and others are really about prefix sums or binary search.

I’ve classified your 16 problems by their standard solutions, not just by their titles or difficulty labels.

## 1. Problems that genuinely fit the Custom Data Structures pattern

I recommend solving these in the following order, moving from basic operation design to increasingly complex combinations of data structures.

| Order | Problem                                                                                                                        | Difficulty | Main concept                                           |
| ----- | ------------------------------------------------------------------------------------------------------------------------------ | ---------- | ------------------------------------------------------ |
| 1     | [Design HashSet](https://leetcode.com/problems/design-hashset/?utm_source=chatgpt.com)                                         | Easy       | Implementing a set, hashing and collision handling     |
| 2     | [Moving Average from Data Stream](https://leetcode.com/problems/moving-average-from-data-stream/?utm_source=chatgpt.com)       | Easy       | Queue, running sum and fixed-size window               |
| 3     | [Two Sum III – Data Structure Design](https://leetcode.com/problems/two-sum-iii-data-structure-design/?utm_source=chatgpt.com) | Easy       | HashMap frequency counting and operation trade-offs    |
| 4     | [Min Stack](https://leetcode.com/problems/min-stack/?utm_source=chatgpt.com)                                                   | Medium     | Augmenting a stack to support minimum in \\(O(1)\\)    |
| 5     | [Implement LRU Cache](https://leetcode.com/problems/lru-cache/?utm_source=chatgpt.com)                                         | Medium     | HashMap + doubly linked list                           |
| 6     | [Insert Delete GetRandom O(1)](https://leetcode.com/problems/insert-delete-getrandom-o1/?utm_source=chatgpt.com)               | Medium     | ArrayList + HashMap + swap-and-remove                  |
| 7     | [Time-Based Key-Value Store](https://leetcode.com/problems/time-based-key-value-store/?utm_source=chatgpt.com)                 | Medium     | HashMap + binary search over timestamps                |
| 8     | [Shortest Word Distance II](https://leetcode.com/problems/shortest-word-distance-ii/?utm_source=chatgpt.com)                   | Medium     | Preprocessed index lists + two pointers                |
| 9     | [Max Stack](https://leetcode.com/problems/max-stack/?utm_source=chatgpt.com)                                                   | Hard       | Stack operations + ordered data structure              |
| 10    | [LFU Cache](https://leetcode.com/problems/lfu-cache/?utm_source=chatgpt.com)                                                   | Hard       | HashMap + frequency lists + doubly linked lists        |
| 11    | [All O\`one Data Structure](https://leetcode.com/problems/all-oone-data-structure/?utm_source=chatgpt.com)                     | Hard       | HashMap + frequency buckets + linked list              |
| 12    | [Range Module](https://leetcode.com/problems/range-module/?utm_source=chatgpt.com)                                             | Hard       | Interval management using an ordered map or tree       |
| 13    | [Finding MK Average](https://leetcode.com/problems/finding-mk-average/?utm_source=chatgpt.com)                                 | Hard       | Sliding window + order-statistics / balanced multisets |

This is a learning order, not a claim that all 13 use the same algorithm. The first six build the core design skills; the remaining problems introduce indexing, ordering, frequency management, or more advanced state maintenance.

## 2. Why this order works

### Phase 1: Foundations — learn to maintain state

These problems teach you to maintain additional information so operations become efficient.

1\. Design HashSet

- Learn to implement `add`, `remove`, and `contains`.
- Understand hashing, collisions, and why a boolean array can work for a bounded domain.
- Foundation for building custom containers.

2\. Moving Average from Data Stream

- Maintain a queue of the most recent `size` elements.
- Maintain a running sum instead of recalculating the sum on every query.
- Key lesson: store only the information needed to answer queries efficiently.

3\. Two Sum III – Data Structure Design

- Support `add(number)` and `find(target)`.
- Compare storing numbers in a list versus storing frequencies in a `HashMap`.
- Key lesson: optimize for the operation that matters most.

4\. Min Stack

- Support `push`, `pop`, `top`, and `getMin` in \\(O(1)\\).
- Learn to maintain auxiliary state, such as a second stack of minimum values.
- Key lesson: make an expensive query cheap by maintaining extra information during updates.

### Phase 2: Core combinations — combine data structures

5\. Implement LRU Cache

The most important foundational cache problem.

- `HashMap` finds a node in \\(O(1)\\) average time.
- Doubly linked list tracks recency and supports \\(O(1)\\) removal and insertion.
- Learn why a singly linked list is insufficient for straightforward \\(O(1)\\) removal of an arbitrary node.

6\. Insert Delete GetRandom O(1)

- Use an array-backed list and a `HashMap` from value to index.
- Remove an element by swapping it with the last element, then removing the last element.
- Key lesson: a data structure's natural strengths can compensate for another's weaknesses.

### Phase 3: Preprocessing and indexing

7\. Time-Based Key-Value Store

- Map each key to a sequence of timestamp-value pairs.
- Use binary search to find the latest timestamp that does not exceed the requested timestamp.
- Key lesson: combine a data structure with an algorithm rather than assuming the entire solution is a single pattern.

8\. Shortest Word Distance II

- Precompute the sorted list of positions for each word.
- For two queried words, use two pointers to find the minimum distance.
- Key lesson: spend time during preprocessing to make repeated queries efficient.

### Phase 4: Advanced ordering and frequency management

9\. Max Stack

- Support ordinary stack operations while also retrieving and removing the maximum.
- Compare a stack plus an ordered multiset with a doubly linked list plus `TreeMap` indexes.
- Key lesson: multiple access orders often require multiple coordinated structures.

10\. LFU Cache

- Maintain key-to-node mappings, frequency buckets, and linked lists within each frequency.
- Track the minimum frequency so eviction can be efficient.
- Key lesson: LRU is not enough when eviction depends on frequency as well as recency.

11\. All O\`one Data Structure

- Maintain a key's frequency and an ordered linked list of frequency buckets.
- Support increment, decrement, and retrieval of a maximum- or minimum-frequency key in \\(O(1)\\).
- Key lesson: maintain an ordered structure through local changes instead of repeatedly sorting.

### Phase 5: Advanced interval and statistical data structures

12\. Range Module

- Support adding, removing, and querying covered intervals.
- Use an ordered map such as Java's `TreeMap` to find and merge overlapping intervals.
- Key lesson: maintaining ranges is different from maintaining individual values.

13\. Finding MK Average

- Maintain a sliding window of the most recent \\(m\\) elements.
- Exclude the smallest \\(k\\) and largest \\(k\\) values when computing the average.
- Requires efficient insertion, removal, and order statistics; a balanced multiset or an appropriately indexed frequency structure is useful.
- Key lesson: combining a sliding window with order statistics is more challenging than ordinary window maintenance.

## 3. Problems to separate or study under their primary algorithm

These problems involve data structure design, but I would not use them as core examples for learning the Custom Data Structures pattern. Study them under their more specific techniques first.

| Problem                                                                                                        | Difficulty | Primary pattern                 | Why separate it?                                                                              |
| -------------------------------------------------------------------------------------------------------------- | ---------- | ------------------------------- | --------------------------------------------------------------------------------------------- |
| [Snapshot Array](https://leetcode.com/problems/snapshot-array/?utm_source=chatgpt.com)                         | Medium     | Binary search + versioned state | Each index stores changes across snapshots; `get` searches for the latest applicable version. |
| [Range Sum Query – Immutable](https://leetcode.com/problems/range-sum-query-immutable/?utm_source=chatgpt.com) | Easy       | Prefix sums                     | A prefix-sum array answers each range-sum query in \\(O(1)\\).                                |
| [Stream of Characters](https://leetcode.com/problems/stream-of-characters/?utm_source=chatgpt.com)             | Hard       | Trie + streaming matching       | A reversed trie supports matching words against the suffix of the characters received so far. |

### Recommended order for these three

1. Range Sum Query – Immutable — learn prefix sums first.
2. Snapshot Array — learn versioned values and binary search.
3. Stream of Characters — learn trie-based matching over a stream.

These are valuable data structure problems, but their main reusable lessons belong to other patterns.

## 4. Borderline problems: keep them, but recognize their primary technique

These two are valid data structure design problems. They are worth studying, but their core algorithm deserves explicit attention.

| Problem                    | Primary technique                  | Recommendation                                                                                            |
| -------------------------- | ---------------------------------- | --------------------------------------------------------------------------------------------------------- |
| Time-Based Key-Value Store | HashMap + binary search            | Keep in the main learning sequence, but classify it primarily under binary search and preprocessing.      |
| Shortest Word Distance II  | Index preprocessing + two pointers | Keep in the main learning sequence, but classify it primarily under preprocessing and query optimization. |

Similarly, Range Module and Finding MK Average should remain in the custom-design curriculum, but approach them only after you understand ordered maps, interval merging, sliding windows, and balanced multisets.

## 5. Final separation list

### A. Core custom data structure design

Prioritize these for learning how to combine and maintain structures.

1. Design HashSet
2. Moving Average from Data Stream
3. Two Sum III – Data Structure Design
4. Min Stack
5. Implement LRU Cache
6. Insert Delete GetRandom O(1)
7. Max Stack
8. LFU Cache
9. All O\`one Data Structure
10. Range Module
11. Finding MK Average

### B. Data structure design with another dominant technique

Study the primary technique alongside the design.

1. Time-Based Key-Value Store — binary search
2. Shortest Word Distance II — preprocessing + two pointers

### C. Better classified under other patterns

Keep these in their respective pattern lists.

1. Range Sum Query – Immutable — prefix sums
2. Snapshot Array — versioned state + binary search
3. Stream of Characters — trie

My recommendation: solve the 11 core problems in the order given in the final separation list. Then solve Time-Based Key-Value Store and Shortest Word Distance II as hybrid problems, and finish with the three problems under other patterns.

One important interview principle: don't memorize each implementation as an isolated solution. For every problem, ask which operations must be fast, which information must be maintained, and which combination of structures provides those guarantees. That reasoning is the real Custom Data Structures pattern.






























-----------------------------------------------------------------------------------------------------------------------
-----------------------------------------------------------------------------------------------------------------------
-----------------------------------------------------------------------------------------------------------------------
-----------------------------------------------------------------------------------------------------------------------



















Since you've already solved those 13 problems, I wouldn't recommend adding dozens of random design problems. Your next step should be to cover the important design techniques that your current list doesn't fully exercise.

For Custom Data Structures, the most valuable missing skills are:

- Designing a structure that supports multiple access patterns efficiently.
- Maintaining ordered data and retrieving medians or percentiles.
- Building a cache with expiration or time-based eviction.
- Maintaining aggregates while elements enter and leave a sliding window.
- Implementing specialized structures such as a deque, randomized collection, or frequency stack.
- Understanding when a standard Java collection is sufficient and when you need to augment it.

I'll distinguish genuinely valuable additions from problems that mostly repeat what you've already learned.

## 1. My recommended additions

I'd add 10 problems, ordered by the new concept each teaches. These are additional problems, not replacements for the 13 you have already completed.

| Order | Problem                                                                                                                                                  | Difficulty | Main concept                                  |
| ----- | -------------------------------------------------------------------------------------------------------------------------------------------------------- | ---------- | --------------------------------------------- |
| 1     | [Design Circular Queue](https://leetcode.com/problems/design-circular-queue/?utm_source=chatgpt.com)                                                     | Medium     | Circular array, index wrapping                |
| 2     | [Design Circular Deque](https://leetcode.com/problems/design-circular-deque/?utm_source=chatgpt.com)                                                     | Medium     | Double-ended queue, circular buffer           |
| 3     | [Design Front Middle Back Queue](https://leetcode.com/problems/design-front-middle-back-queue/?utm_source=chatgpt.com)                                   | Medium     | Two deques and balancing invariants           |
| 4     | [Maximum Frequency Stack](https://leetcode.com/problems/maximum-frequency-stack/?utm_source=chatgpt.com)                                                 | Hard       | Frequency counting + stacks                   |
| 5     | [Insert Delete GetRandom O(1) – Duplicates Allowed](https://leetcode.com/problems/insert-delete-getrandom-o1-duplicates-allowed/?utm_source=chatgpt.com) | Hard       | Duplicate values + index sets                 |
| 6     | [Find Median from Data Stream](https://leetcode.com/problems/find-median-from-data-stream/?utm_source=chatgpt.com)                                       | Hard       | Two heaps, dynamic order statistics           |
| 7     | [Stock Price Fluctuation](https://leetcode.com/problems/stock-price-fluctuation/?utm_source=chatgpt.com)                                                 | Medium     | Corrections, ordered values, lazy deletion    |
| 8     | [Data Stream as Disjoint Intervals](https://leetcode.com/problems/data-stream-as-disjoint-intervals/?utm_source=chatgpt.com)                             | Hard       | Ordered intervals and merging                 |
| 9     | [Design Hit Counter](https://leetcode.com/problems/design-hit-counter/?utm_source=chatgpt.com)                                                           | Medium     | Time windows, queues and aggregation          |
| 10    | [Design In-Memory File System](https://leetcode.com/problems/design-in-memory-file-system/?utm_source=chatgpt.com)                                       | Hard       | Hierarchical state, tree/trie and path lookup |

This selection covers several distinct techniques found in LeetCode's design and data-stream problem families.&#x20;

[image](https://www.google.com/s2/favicons?domain=https://leetcodejavascript.com\&sz=32)

JavaScript Solutions

+2



## 2. Detailed problem-by-problem roadmap

### 1. Design Circular Queue

LeetCode 622 · Medium

Link: [Problem statement](https://leetcode.com/problems/design-circular-queue/?utm_source=chatgpt.com)

Why solve it: Your earlier problems use stacks, linked lists, and queues, but this teaches you to implement a bounded queue directly using an array.

Operations include `enQueue`, `deQueue`, `Front`, `Rear`, `isEmpty`, and `isFull`.

What you will learn:

- Maintain `head`, `tail`, and `size` (or an equivalent set of indices).
- Wrap indices using modulo arithmetic.
- Avoid shifting array elements after removing the front element.
- Handle empty and full states without confusing them.

Target complexity: \\(O(1)\\) per operation, \\(O(k)\\) space for capacity \\(k\\).

### 2. Design Circular Deque

LeetCode 641 · Medium

Link: [Problem statement](https://leetcode.com/problems/design-circular-deque/?utm_source=chatgpt.com)

Why solve it: It extends the circular queue to support insertion and deletion at both ends.

What you will learn:

- Insert at the front or rear in constant time.
- Remove from either end.
- Maintain consistent index wrapping and empty/full conditions.
- Understand how a circular buffer differs from a linked-list deque.

Target complexity: \\(O(1)\\) per operation, \\(O(k)\\) space.

Dependency: Solve Circular Queue first. Circular Deque is a useful implementation exercise, but it is not essential to solve every later problem.

### 3. Design Front Middle Back Queue

LeetCode 1670 · Medium

Link: [Problem statement](https://leetcode.com/problems/design-front-middle-back-queue/?utm_source=chatgpt.com)

Why solve it: This is a particularly good exercise in maintaining an invariant across multiple operations.

The queue supports insertion and removal at the front, middle, and back.

What you will learn:

- Represent a sequence using two deques.
- Keep the left and right halves balanced.
- Define exactly which half owns the middle element.
- Restore the invariant after every insertion or removal.

Target complexity: \\(O(1)\\) per operation with two balanced deques.

The important lesson is not the deque API itself. It is maintaining a representation that makes every required operation efficient.

### 4. Maximum Frequency Stack

LeetCode 895 · Hard

Link: [Problem statement](https://leetcode.com/problems/maximum-frequency-stack/?utm_source=chatgpt.com)

Why solve it: This is one of the strongest additions to your custom data structure preparation.

A normal stack removes the most recently pushed value. A frequency stack removes the value with the highest frequency; when frequencies tie, it removes the most recently pushed among those values.

What you will learn:

- Track each value's frequency using a `HashMap`.
- Maintain a stack of values for each frequency.
- Track the current maximum frequency.
- Combine frequency tracking with recency ordering.

Target complexity: \\(O(1)\\) average time for `push` and `pop`, with \\(O(n)\\) space.

This is different from LFU Cache: the frequency-stack problem is organized around push/pop history rather than key lookup and eviction.

### 5. Insert Delete GetRandom O(1) — Duplicates Allowed

LeetCode 381 · Hard

Link: [Problem statement](https://leetcode.com/problems/insert-delete-getrandom-o1-duplicates-allowed/?utm_source=chatgpt.com)

Why solve it: You already solved the original randomized-set problem. This variant tests whether you can adapt the design when duplicate values are permitted.

Operations:

- `insert(val)` — insert a value, even if it already exists.
- `remove(val)` — remove one occurrence if present.
- `getRandom()` — return a random stored occurrence, so duplicates are proportionally represented.

What you will learn:

- Maintain an `ArrayList<Integer>` of all occurrences.
- Map each value to a set of its current array indices.
- Remove an occurrence using swap-and-remove.
- Update both index sets correctly when swapping elements.

Target complexity: \\(O(1)\\) average time for all operations.

Why it matters: This is an excellent test of whether you genuinely understand the array-plus-hash-map design or have only memorized the original solution.

### 6. Find Median from Data Stream

LeetCode 295 · Hard

Link: [Problem statement](https://leetcode.com/problems/find-median-from-data-stream/?utm_source=chatgpt.com)

Why solve it: Your Finding MK Average problem involves maintaining order statistics over a sliding window. This problem teaches the foundational technique of maintaining the median as values arrive.

What you will learn:

- Use a max-heap for the smaller half of the numbers.
- Use a min-heap for the larger half.
- Maintain the invariant that the two heaps differ in size by at most one.
- Retrieve the median without sorting the entire collection.

Target complexity:

- `addNum`: \\(O(\log n)\\)
- `findMedian`: \\(O(1)\\)
- Space: \\(O(n)\\)

For example, when the stream contains `5, 2, 10, 4`, the sorted order is `[2, 4, 5, 10]`. The median is `(4 + 5) / 2 = 4.5`.

This is a strong bridge between heap problems and custom data structure design.

### 7. Stock Price Fluctuation

LeetCode 2034 · Medium

Link: [Problem statement](https://leetcode.com/problems/stock-price-fluctuation/?utm_source=chatgpt.com)

Why solve it: Unlike a normal stream, this problem allows a previous timestamp's price to be corrected.

Operations include updating a timestamp, retrieving the current price, and finding the maximum and minimum prices.

What you will learn:

- Map each timestamp to its latest price.
- Maintain access to minimum and maximum prices.
- Handle repeated updates to an existing timestamp.
- Use lazy deletion with heaps, or an ordered multiset/map alternative.

Target complexity with the common heap approach:

- `update`: typically \\(O(\log n)\\), amortized over heap operations.
- `current`: \\(O(1)\\).
- `maximum` and `minimum`: \\(O(\log n)\\) in the worst case when stale heap entries must be removed; often faster in practice.
- Space: potentially \\(O(u)\\) for the heaps, where \\(u\\) is the number of updates, because stale entries can accumulate.

Important distinction: This is not simply a priority-queue exercise. The key design challenge is that an earlier value can become invalid without being removed immediately from a heap.

### 8. Data Stream as Disjoint Intervals

LeetCode 352 · Hard

Link: [Problem statement](https://leetcode.com/problems/data-stream-as-disjoint-intervals/?utm_source=chatgpt.com)

Why solve it: This introduces a different type of state: a set of non-overlapping intervals that must remain merged as numbers arrive.

For example, inserting `1`, `3`, `2` should result in the interval `[1, 3]`.

What you will learn:

- Maintain intervals in sorted order.
- Identify adjacent intervals that should be merged.
- Handle insertion into an existing interval, between intervals, or at an interval boundary.
- Use a `TreeMap` to locate neighboring intervals efficiently.

Target complexity: \\(O(\log n)\\) per insertion in a suitable ordered-map implementation, with \\(O(n)\\) space in the number of stored intervals.

This is useful preparation for Range Module, calendar problems, and interval-management design questions.

### 9. Design Hit Counter

LeetCode 362 · Medium · Premium

Link: [Problem statement](https://leetcode.com/problems/design-hit-counter/?utm_source=chatgpt.com)

Why solve it: It teaches you to design a time-based counter that returns the number of hits in a recent time window.

What you will learn:

- Maintain timestamps in a queue when requests arrive in chronological order.
- Remove expired timestamps before answering a query.
- Trade off storing every event against storing aggregated counts in fixed-size time buckets.
- Define window boundaries precisely, especially at the 300-second cutoff.

Target complexity:

- Queue-based implementation: amortized \\(O(1)\\) per operation, with space proportional to hits in the active window.
- Bucket-based implementation: \\(O(1)\\) per operation with fixed-size storage when the assumptions about timestamps are satisfied.

This is a useful extension of Moving Average from Data Stream, where the maintained aggregate changes from a sum to a count of events.

### 10. Design In-Memory File System

LeetCode 588 · Hard · Premium

Link: [Problem statement](https://leetcode.com/problems/design-in-memory-file-system/?utm_source=chatgpt.com)

Why solve it: It combines hierarchical state management with efficient lookup, and is useful for interviews involving API and data structure design.

Typical operations include listing directory contents, creating directories, adding file content, and reading files.

What you will learn:

- Model files and directories as nodes in a tree.
- Use a `HashMap<String, Node>` for child lookup.
- Parse paths into components and traverse the hierarchy.
- Distinguish file nodes from directory nodes.
- Support operations that have different semantics depending on the final path component.

Target complexity: roughly \\(O(L)\\) for traversing a path of length \\(L\\), plus the cost of listing or returning content. Space depends on the total stored paths and file contents.

This problem overlaps with trie-based design, so it is not essential if you have already mastered file-system and trie implementation problems. Still, it is a worthwhile advanced design exercise.

## 3. Optional problems worth considering later

These are valuable, but I would prioritize the ten above before adding them.

| Problem                           | Difficulty | Link                                                                                                    | Why consider it?                                                                                                   |
| --------------------------------- | ---------- | ------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------ |
| Design Twitter                    | Medium     | [LeetCode 355](https://leetcode.com/problems/design-twitter/?utm_source=chatgpt.com)                    | Combines per-user timelines, follow relationships, and k-way merge for a news feed.                                |
| My Calendar III                   | Hard       | [LeetCode 732](https://leetcode.com/problems/my-calendar-iii/?utm_source=chatgpt.com)                   | Maintains booking overlap counts; useful for ordered events and sweep-line techniques.                             |
| Design Search Autocomplete System | Hard       | [LeetCode 642](https://leetcode.com/problems/design-search-autocomplete-system/?utm_source=chatgpt.com) | Combines a trie, frequency tracking, and ranked suggestions.                                                       |
| Design Movie Rental System        | Hard       | [LeetCode 1912](https://leetcode.com/problems/design-movie-rental-system/?utm_source=chatgpt.com)       | Uses multiple indexes and ordered result sets to satisfy different query requirements.                             |
| Design a Number Container System  | Medium     | [LeetCode 2349](https://leetcode.com/problems/design-a-number-container-system/?utm_source=chatgpt.com) | Combines an index-to-number map with ordered sets or heaps to handle updates and find the smallest matching index. |
| Design a Food Rating System       | Medium     | [LeetCode 2353](https://leetcode.com/problems/design-a-food-rating-system/?utm_source=chatgpt.com)      | Maintains category-based rankings with tie-breaking rules and multiple indexes.                                    |

These extend the same general design mindset to more realistic systems with several query types and overlapping indexes.&#x20;

[image](https://www.google.com/s2/favicons?domain=https://leetcodejavascript.com\&sz=32)

JavaScript Solutions

+2



## 4. What I would prioritize for interview preparation

If your goal is to strengthen DSA interviews rather than solve every LeetCode design problem, my priority would be:

| Priority             | Problems                                                                       | Reason                                              |
| -------------------- | ------------------------------------------------------------------------------ | --------------------------------------------------- |
| Must solve           | Maximum Frequency Stack, Find Median from Data Stream, Stock Price Fluctuation | New, highly reusable techniques                     |
| Strongly recommended | Circular Queue, Circular Deque, Front Middle Back Queue                        | Build confidence with implementation and invariants |
| Recommended          | Randomized Collection with Duplicates, Data Stream as Disjoint Intervals       | More demanding variants of existing concepts        |
| Optional             | Design Hit Counter, In-Memory File System                                      | Valuable but more specialized                       |
| Advanced extension   | Design Twitter, My Calendar III, Movie Rental System                           | Broader combinations of data structures             |

## 5. The checklist to use for every design problem

Before coding in Java, answer these six questions:

1. Operations: What operations must the data structure support?
2. Complexities: What time complexity is expected for each operation?
3. Representation: Which standard structures can support those operations?
4. Additional state: What information must be maintained to avoid recomputation?
5. Invariants: What must always remain true after every operation?
6. Edge cases: What happens with empty structures, duplicate values, updates, deletions, ties, and boundary conditions?

For example, in LRU Cache, the invariant is that the doubly linked list represents recency order, while the map points to the corresponding nodes. In LFU Cache, you must additionally maintain frequency buckets and the minimum frequency.

My final recommendation: Start with Circular Queue, then Front Middle Back Queue, Maximum Frequency Stack, Find Median from Data Stream, and Stock Price Fluctuation. These five will broaden your problem-solving toolkit considerably beyond the custom structures you have already implemented.

Use Java 21 or later for your solutions, but prefer standard collections such as `HashMap`, `ArrayDeque`, `PriorityQueue`, and `TreeMap` unless the problem explicitly requires implementing the underlying structure yourself.
































-----------------------------------------------------------------------------------------------------------------------
-----------------------------------------------------------------------------------------------------------------------
-----------------------------------------------------------------------------------------------------------------------
-----------------------------------------------------------------------------------------------------------------------


























# Custom Data Structures: Additional Problems for Google / Meta Interviews

Excluding the 13 problems you've already solved, here is the curated list of additional problems worth solving, ordered by learning value and interview relevance.

I've separated the core additions from optional extensions so you can prioritize effectively for Google L4/L5 and Meta interviews.

## 1. Core additions — solve these first

These cover new techniques or meaningful extensions of the custom data structure concepts you already know.

| # | Problem                                                                                                                                                  | Difficulty | Main concepts                    | Priority  |
| - | -------------------------------------------------------------------------------------------------------------------------------------------------------- | ---------- | -------------------------------- | --------- |
| 1 | [Maximum Frequency Stack](https://leetcode.com/problems/maximum-frequency-stack/?utm_source=chatgpt.com)                                                 | Hard       | HashMap + frequency stacks       | Essential |
| 2 | [Find Median from Data Stream](https://leetcode.com/problems/find-median-from-data-stream/?utm_source=chatgpt.com)                                       | Hard       | Two heaps, running median        | Essential |
| 3 | [Stock Price Fluctuation](https://leetcode.com/problems/stock-price-fluctuation/?utm_source=chatgpt.com)                                                 | Medium     | HashMap + heaps + lazy deletion  | Essential |
| 4 | [Insert Delete GetRandom O(1) — Duplicates Allowed](https://leetcode.com/problems/insert-delete-getrandom-o1-duplicates-allowed/?utm_source=chatgpt.com) | Hard       | ArrayList + HashMap + index sets | High      |
| 5 | [Data Stream as Disjoint Intervals](https://leetcode.com/problems/data-stream-as-disjoint-intervals/?utm_source=chatgpt.com)                             | Hard       | TreeMap + interval merging       | High      |

### What each problem adds

1\. Maximum Frequency Stack

Design a stack that removes the most frequent value. If multiple values have the same frequency, remove the most recently pushed one.

- Maintain a value-to-frequency map.
- Maintain a stack for each frequency.
- Track the maximum frequency.
- Target: \\(O(1)\\) average time for `push` and `pop`.

2\. Find Median from Data Stream

Continuously insert numbers and return the median efficiently.

- Use a max-heap for the lower half and a min-heap for the upper half.
- Balance the heaps after every insertion.
- Target: \\(O(\log n)\\) insertion and \\(O(1)\\) median retrieval.

3\. Stock Price Fluctuation

Support price updates at existing timestamps and queries for the current, minimum, and maximum prices.

- Maintain the latest price per timestamp.
- Use heaps with lazy deletion or ordered maps.
- Learn how to handle stale heap entries when previous values are updated.

4\. Insert Delete GetRandom O(1) — Duplicates Allowed

Extend the randomized-set problem to permit repeated values.

- Store all occurrences in an array-backed list.
- Map each value to its set of indices.
- Use swap-and-remove while updating the index sets correctly.
- Target: \\(O(1)\\) average time per operation.

5\. Data Stream as Disjoint Intervals

Maintain a sorted collection of disjoint intervals as numbers arrive.

- Use an ordered map to locate neighboring intervals.
- Merge adjacent intervals when a new number connects them.
- Learn to maintain an interval-based representation incrementally.

## 2. Strong additions — implementation and invariant practice

| # | Problem                                                                                                                | Difficulty       | Main concepts                     | Priority    |
| - | ---------------------------------------------------------------------------------------------------------------------- | ---------------- | --------------------------------- | ----------- |
| 6 | [Design Circular Queue](https://leetcode.com/problems/design-circular-queue/?utm_source=chatgpt.com)                   | Medium           | Circular array, modulo arithmetic | Recommended |
| 7 | [Design Circular Deque](https://leetcode.com/problems/design-circular-deque/?utm_source=chatgpt.com)                   | Medium           | Double-ended circular buffer      | Recommended |
| 8 | [Design Front Middle Back Queue](https://leetcode.com/problems/design-front-middle-back-queue/?utm_source=chatgpt.com) | Medium           | Two deques, balancing invariants  | Recommended |
| 9 | [Design Hit Counter](https://leetcode.com/problems/design-hit-counter/?utm_source=chatgpt.com)                         | Medium · Premium | Time window, queue, aggregation   | Recommended |

These problems are useful for practising correct implementation under interview pressure.

- Circular Queue: implement enqueue, dequeue, front, rear, and full/empty checks in \\(O(1)\\).
- Circular Deque: extend the design to insert and remove from either end.
- Front Middle Back Queue: maintain two balanced deques to support all three positions efficiently.
- Hit Counter: count events within a rolling time window; compare storing timestamps with fixed-size bucket aggregation.

## 3. Advanced design extensions — solve if time permits

| #  | Problem                                                                                                                      | Difficulty     | Main concepts                                 | Priority |
| -- | ---------------------------------------------------------------------------------------------------------------------------- | -------------- | --------------------------------------------- | -------- |
| 10 | [Design Twitter](https://leetcode.com/problems/design-twitter/?utm_source=chatgpt.com)                                       | Medium         | HashMap, feed aggregation, k-way merge        | Optional |
| 11 | [Design Search Autocomplete System](https://leetcode.com/problems/design-search-autocomplete-system/?utm_source=chatgpt.com) | Hard           | Trie, frequency ranking, top-k                | Optional |
| 12 | [Design Movie Rental System](https://leetcode.com/problems/design-movie-rental-system/?utm_source=chatgpt.com)               | Hard           | Multiple indexes, ordered sets                | Optional |
| 13 | [Design In-Memory File System](https://leetcode.com/problems/design-in-memory-file-system/?utm_source=chatgpt.com)           | Hard · Premium | Tree/trie, path traversal, hierarchical state | Optional |
| 14 | [My Calendar III](https://leetcode.com/problems/my-calendar-iii/?utm_source=chatgpt.com)                                     | Hard           | Interval events, sweep line, ordered maps     | Optional |
| 15 | [Design a Number Container System](https://leetcode.com/problems/design-a-number-container-system/?utm_source=chatgpt.com)   | Medium         | Multiple indexes, lazy deletion               | Optional |
| 16 | [Design a Food Rating System](https://leetcode.com/problems/design-a-food-rating-system/?utm_source=chatgpt.com)             | Medium         | HashMap, ordered ranking, tie-breaking        | Optional |

These are broader design exercises. They are worth attempting after the core problems, but they shouldn't take priority over mastering the fundamental patterns.

## 4. Final recommended list

If you're preparing specifically for Google and Meta, use this sequence:

1. Maximum Frequency Stack
2. Find Median from Data Stream
3. Stock Price Fluctuation
4. Insert Delete GetRandom O(1) — Duplicates Allowed
5. Data Stream as Disjoint Intervals
6. Design Circular Queue
7. Design Circular Deque
8. Design Front Middle Back Queue
9. Design Hit Counter
10. Design Twitter
11. Design Search Autocomplete System
12. Design Movie Rental System
13. Design In-Memory File System
14. My Calendar III
15. Design a Number Container System
16. Design a Food Rating System

My recommendation: Complete the first five thoroughly, including dry runs, edge cases, invariants, and Java implementations. Then move to the next four. Treat the final seven as optional extensions rather than mandatory interview preparation.

Use Java 21 or later, with `HashMap`, `ArrayDeque`, `PriorityQueue`, and `TreeMap` where appropriate. The important skill is choosing and coordinating the structures—not implementing everything from scratch when the problem doesn't require it.










































-----------------------------------------------------------------------------------------------------------------------
-----------------------------------------------------------------------------------------------------------------------
-----------------------------------------------------------------------------------------------------------------------
-----------------------------------------------------------------------------------------------------------------------





































Since you've now solved the original 13 problems **and all 16 additional problems**, you've covered a substantial part of the Custom Data Structures pattern.

At this stage, I would **stop collecting standard design problems and focus on the remaining high-value gaps**. For Google L4/L5 and Meta interviews, the next useful problems should introduce genuinely different techniques, not simply repeat `HashMap + doubly linked list` or `HashMap + heap`.

I'll focus on problems that extend your current preparation into advanced data structure design, efficient querying, and maintaining complex state.

# 1. Additional Custom Data Structure Problems Worth Solving

I've excluded all 29 problems you've already completed.

These are **10 additional problems** I would consider, ranked by the new techniques they introduce and their usefulness for Google/Meta-style interviews.

| # | Problem | Difficulty | Main concept | Priority |
|---:|---|---|---|---|
| 1 | [Design Underground System](https://leetcode.com/problems/design-underground-system/) | Medium | HashMap + aggregation + state management | High |
| 2 | [Design Authentication Manager](https://leetcode.com/problems/design-authentication-manager/) | Medium | Time-based expiration + lazy cleanup | High |
| 3 | [Design Browser History](https://leetcode.com/problems/design-browser-history/) | Medium | Navigation state + stack/list design | High |
| 4 | [Design Log Storage System](https://leetcode.com/problems/design-log-storage-system/) | Medium | Ordered data + range queries | High |
| 5 | [Design Skiplist](https://leetcode.com/problems/design-skiplist/) | Hard | Probabilistic ordered data structure | High |
| 6 | [Excel Sheet Column Sum Formula](https://leetcode.com/problems/excel-sum-formula/) | Hard · Premium | Dependency graph + incremental updates | Medium–High |
| 7 | [Design Phone Directory](https://leetcode.com/problems/design-phone-directory/) | Medium | Efficient allocation and reuse | Medium |
| 8 | [Design Tic-Tac-Toe](https://leetcode.com/problems/design-tic-tac-toe/) | Medium | Incremental state tracking | Medium |
| 9 | [Design Snake Game](https://leetcode.com/problems/design-snake-game/) | Medium | Deque + HashSet + simulation | Medium |
| 10 | [Design Bounded Blocking Queue](https://leetcode.com/problems/design-bounded-blocking-queue/) | Medium · Premium | Thread safety + synchronization | Medium–High |

*Difficulty and priority are preparation recommendations, not predictions of interview frequency.*

## 2. Why these problems are worth considering

### 1. Design Underground System

**Link:** [LeetCode 1396](https://leetcode.com/problems/design-underground-system/)

**What it teaches:** Designing a data structure that tracks active state and computes aggregates from completed events.

You must support operations such as:
- `checkIn(id, stationName, time)`
- `checkOut(id, stationName, time)`
- `getAverageTime(startStation, endStation)`

The key design is to maintain:
- A map of currently checked-in passengers.
- A map of route statistics containing total travel time and trip count.

The average is calculated from the accumulated total and count.

**Why it matters:** It teaches you to distinguish transient state from aggregated historical state. This is useful for analytics, event processing, and backend-oriented interview problems.

### 2. Design Authentication Manager

**Link:** [LeetCode 1797](https://leetcode.com/problems/design-authentication-manager/)

**What it teaches:** Managing expiring entries efficiently.

You must generate tokens, renew them, and count unexpired tokens.

The important questions are:
- How do you identify expired tokens?
- When should expired entries be removed?
- Can you avoid scanning every token on every request?

Explore a `HashMap` combined with expiration timestamps and an ordered expiration queue.

**Why it matters:** This resembles the underlying challenges of session management, token expiration, and time-to-live caches.

### 3. Design Browser History

**Link:** [LeetCode 1472](https://leetcode.com/problems/design-browser-history/)

**What it teaches:** Maintaining state when operations can move both forward and backward.

Operations include:
- Visiting a new URL.
- Moving backward by a specified number of steps.
- Moving forward by a specified number of steps.

The important invariant is that visiting a new URL discards the forward history.

Compare a list with a current-position pointer against two-stack implementations.

**Why it matters:** It reinforces state transitions and the trade-offs between different representations.

### 4. Design Log Storage System

**Link:** [LeetCode 635](https://leetcode.com/problems/design-log-storage-system/)

**What it teaches:** Storing timestamped records and retrieving them at different levels of time granularity.

You must store logs and retrieve their IDs within a specified time range. The query may operate at year, month, day, hour, minute, or second granularity.

Consider:
- A `TreeMap` with ordered timestamps.
- Encoding timestamps into comparable representations.
- Finding the relevant range without scanning every stored log.

**Why it matters:** It builds on your experience with `TreeMap` and interval queries while introducing time-granularity constraints.

### 5. Design Skiplist

**Link:** [LeetCode 1206](https://leetcode.com/problems/design-skiplist/)

**What it teaches:** Implementing an ordered data structure with probabilistic balancing.

You must support:
- `search`
- `add`
- `erase`

A skip list maintains multiple linked-list levels. Higher levels allow searches to skip over large portions of the data.

Expected time complexity is \(O(\log n)\) for search, insertion, and deletion, with \(O(n)\) expected space.

**Why it matters:** You will learn how an ordered structure can achieve efficient operations without implementing a balanced binary search tree.

This is a more specialized addition, but valuable if you want to understand advanced data structure implementation.

### 6. Excel Sheet Column Sum Formula

**Link:** [LeetCode 631](https://leetcode.com/problems/excel-sum-formula/)

**What it teaches:** Maintaining dependencies between values.

A spreadsheet cell may contain a constant or a formula referencing other cells. When a referenced cell changes, dependent cells must reflect the updated result.

You need to think about:
- Representing cell dependencies.
- Evaluating formulas.
- Propagating updates to dependent cells.
- Avoiding incorrect results when dependencies overlap.

This introduces graph-based dependency management rather than ordinary container design.

**Why it matters:** It is a useful bridge between custom data structures, dependency graphs, and incremental computation.

### 7. Design Phone Directory

**Link:** [LeetCode 379](https://leetcode.com/problems/design-phone-directory/)

**What it teaches:** Managing a pool of available resources.

You must efficiently:
- Allocate an available number.
- Check whether a number is available.
- Release a number for reuse.

A queue or set can represent available numbers, while a boolean array can track allocation state.

**Why it matters:** It is a practical resource-allocation problem. However, given your existing experience with sets and maps, it is less novel than the first six problems.

### 8. Design Tic-Tac-Toe

**Link:** [LeetCode 348](https://leetcode.com/problems/design-tic-tac-toe/)

**What it teaches:** Maintaining enough state to answer queries without repeatedly examining the entire board.

Instead of scanning all rows, columns, and diagonals after every move, maintain counters for each row, column, and diagonal.

Each move can then be processed in \(O(1)\) time.

**Why it matters:** This is a simple but important example of replacing repeated computation with incremental state maintenance.

It is particularly useful for learning how to derive an efficient representation from the required operations.

### 9. Design Snake Game

**Link:** [LeetCode 353](https://leetcode.com/problems/design-snake-game/)

**What it teaches:** Coordinating multiple structures to represent a changing simulation.

A typical solution uses:
- A deque to maintain the snake's body.
- A set of occupied coordinates to detect collisions.
- State representing food positions and the current score.

The tricky part is handling movement into the snake's current tail position when the tail is about to move away.

**Why it matters:** It tests whether your data structures correctly represent dynamic state, including subtle update-order edge cases.

### 10. Design Bounded Blocking Queue

**Link:** [LeetCode 1188](https://leetcode.com/problems/design-bounded-blocking-queue/)

**What it teaches:** Implementing a bounded queue used by multiple threads.

The queue has a fixed capacity:
- `enqueue` waits when the queue is full.
- `dequeue` waits when the queue is empty.
- Operations must be thread-safe.

You need to reason about synchronization, waiting and notification, and race conditions.

**Why it matters:** This is the most relevant addition for connecting custom data structure design with concurrent programming.

Given your Java backend background, it is worth understanding, although it is more specialized than ordinary algorithmic design questions.

---

# 3. Which should you actually solve?

I wouldn't automatically add all 10 to your mandatory DSA list.

For your Google L4/L5 and Meta preparation, my recommended order is:

| Order | Problem | Reason |
|---:|---|---|
| 1 | Design Underground System | State management and aggregation |
| 2 | Design Authentication Manager | Expiration and cleanup |
| 3 | Design Log Storage System | Ordered queries |
| 4 | Design Skiplist | Advanced ordered structure |
| 5 | Excel Sheet Column Sum Formula | Dependency management |
| 6 | Design Browser History | Navigation state |
| 7 | Design Bounded Blocking Queue | Concurrency and synchronization |
| 8 | Design Tic-Tac-Toe | Incremental computation |
| 9 | Design Snake Game | Multiple coordinated structures |
| 10 | Design Phone Directory | Resource allocation |

## 4. My final recommendation

You've already solved 29 problems across custom data structure design, caches, frequency tracking, ordered maps, streams, and complex state management.

**I would prioritize the first five additional problems, then move on to other DSA patterns rather than trying to complete every design problem on LeetCode.**

For Google and Meta, broad algorithmic coverage, problem-solving under time constraints, and the ability to explain your reasoning are generally more valuable than accumulating a very large number of problems from one category.

One final distinction: **custom data structure design is not the same as system design.** For senior-level interviews, you'll also want to practise designing larger systems, including APIs, storage, caching, concurrency, reliability, and scalability. Those skills complement your DSA preparation but require a different kind of practice.





























-----------------------------------------------------------------------------------------------------------------------
-----------------------------------------------------------------------------------------------------------------------
-----------------------------------------------------------------------------------------------------------------------
-----------------------------------------------------------------------------------------------------------------------






















You've now completed the original 13 problems, the 16 additional problems, and the 10 further problems. That's **39 problems in the Custom Data Structures category**.

At this point, I recommend changing your strategy: rather than continuing to collect standard design problems, focus on **genuinely new data structure techniques, less common but valuable design challenges, and problems that combine multiple patterns**.

For your Google L4/L5 and Meta interview preparation, I'll exclude all 39 problems you've already solved and identify only additional problems that offer meaningful learning value.

One caveat: more problems do not automatically mean better interview readiness. The goal is to identify gaps in your understanding, not to exhaust every problem tagged *Design* on LeetCode.

## 1. The next 12 problems I'd consider

I reviewed the broader LeetCode design-problem catalog to identify problems that weren't in your previous lists. The most useful remaining candidates involve iterators, mutable range queries, specialized stacks, dynamic rankings, and stateful systems. [LeetCode JavaScript Solutions](https://leetcodejavascript.com/tags/design?utm_source=chatgpt.com)

| # | Problem | Difficulty | Primary concept | Recommendation |
|---:|---|---|---|---|
| 1 | [Design HashMap](https://leetcode.com/problems/design-hashmap/) | Easy | Hashing, collision handling, bucket design | Solve if not implemented from scratch |
| 2 | [Design Linked List](https://leetcode.com/problems/design-linked-list/) | Medium | Node management, insertion, deletion | Solve if not implemented from scratch |
| 3 | [Implement Queue using Stacks](https://leetcode.com/problems/implement-queue-using-stacks/) | Easy | Amortized analysis, two-stack design | Recommended |
| 4 | [Implement Stack using Queues](https://leetcode.com/problems/implement-stack-using-queues/) | Easy | Adapting one interface to another | Optional |
| 5 | [Design a Stack With Increment Operation](https://leetcode.com/problems/design-a-stack-with-increment-operation/) | Medium | Lazy propagation within a stack | Recommended |
| 6 | [Dinner Plate Stacks](https://leetcode.com/problems/dinner-plate-stacks/) | Hard | Multiple bounded stacks, indexed capacity management | High-value challenge |
| 7 | [Range Sum Query – Mutable](https://leetcode.com/problems/range-sum-query-mutable/) | Medium | Fenwick tree or segment tree | Essential if those structures aren't mastered |
| 8 | [Range Sum Query 2D – Mutable](https://leetcode.com/problems/range-sum-query-2d-mutable/) | Hard · Premium | 2D Fenwick tree or 2D segment tree | Advanced extension |
| 9 | [Online Stock Span](https://leetcode.com/problems/online-stock-span/) | Medium | Monotonic stack + streaming state | Recommended |
| 10 | [Online Election](https://leetcode.com/problems/online-election/) | Medium | Preprocessing + binary search | Recommended |
| 11 | [Sequentially Ordinal Rank Tracker](https://leetcode.com/problems/sequentially-ordinal-rank-tracker/) | Hard | Ordered ranking, heaps or balanced sets | High-value challenge |
| 12 | [Count Integers in Intervals](https://leetcode.com/problems/count-integers-in-intervals/) | Hard | Interval merging, ordered maps or segment trees | Advanced extension |

These problems are not all pure custom data structure problems. I've included several because they strengthen closely related skills that matter in algorithmic interviews.

### Which ones are genuinely new?

**Highest-value additions:**

1. **Dinner Plate Stacks:** Maintain many stacks with a fixed capacity and efficiently locate the leftmost stack with available space.
2. **Design a Stack With Increment Operation:** Learn to defer updates instead of modifying every affected element.
3. **Range Sum Query – Mutable:** Move from simple prefix sums to structures that support both updates and queries.
4. **Sequentially Ordinal Rank Tracker:** Maintain a ranking as items are added, while queries request successive ranked results.
5. **Count Integers in Intervals:** Maintain the union of many intervals while supporting insertions and total covered-length queries.

These five offer more meaningful extensions than solving another basic queue or stack implementation.

---

## 2. A separate category: iterator design

Iterator problems are useful because they test how you maintain traversal state without materializing unnecessary data.

| Problem | Difficulty | Link | Main concept |
|---|---|---|---|
| Flatten 2D Vector | Medium | [LeetCode 251](https://leetcode.com/problems/flatten-2d-vector/) | Lazy traversal across nested collections |
| Peeking Iterator | Medium | [LeetCode 284](https://leetcode.com/problems/peeking-iterator/) | Lookahead without consuming the next element |
| Flatten Nested List Iterator | Medium | [LeetCode 341](https://leetcode.com/problems/flatten-nested-list-iterator/) | Stack-based traversal of nested structures |
| Zigzag Iterator | Medium | [LeetCode 281](https://leetcode.com/problems/zigzag-iterator/) | Alternating traversal across multiple iterators |
| Binary Search Tree Iterator | Medium | [LeetCode 173](https://leetcode.com/problems/binary-search-tree-iterator/) | Explicit traversal state using a stack |

**My recommendation:** Solve Peeking Iterator and Flatten Nested List Iterator first. Then study BST Iterator if you haven't already mastered iterative tree traversal.

These are particularly useful for understanding iterators, lazy evaluation, and stateful traversal in Java.

---

## 3. What about the remaining LeetCode design problems?

There are many more, including Design SQL, Design Spreadsheet, Design a Text Editor, Design a Task Manager, and Design an ATM Machine. The catalog is extensive. [LeetCode JavaScript Solutions](https://leetcodejavascript.com/tags/design?utm_source=chatgpt.com)

I wouldn't automatically add all of them to your preparation.

Use this filter:

- **Add it** if it introduces a genuinely new technique.
- **Study it selectively** if it combines familiar techniques in an interesting way.
- **Skip it for now** if it mostly repeats a structure you already understand.

For example, after completing LRU Cache, LFU Cache, Max Stack, and All O\`one Data Structure, another problem involving only a `HashMap` and linked list probably has diminishing returns unless it introduces a new constraint.

---

## 4. My final recommendation for your Google/Meta preparation

Given the 39 problems you've completed, I'd prioritize the following **eight remaining problems**:

| Order | Problem | What you should master |
|---:|---|---|
| 1 | [Implement Queue using Stacks](https://leetcode.com/problems/implement-queue-using-stacks/) | Amortized \(O(1)\) reasoning |
| 2 | [Peeking Iterator](https://leetcode.com/problems/peeking-iterator/) | Stateful iterator design |
| 3 | [Dinner Plate Stacks](https://leetcode.com/problems/dinner-plate-stacks/) | Coordinating multiple data structures |
| 4 | [Design a Stack With Increment Operation](https://leetcode.com/problems/design-a-stack-with-increment-operation/) | Lazy updates |
| 5 | [Online Stock Span](https://leetcode.com/problems/online-stock-span/) | Monotonic stack |
| 6 | [Range Sum Query – Mutable](https://leetcode.com/problems/range-sum-query-mutable/) | Fenwick tree or segment tree |
| 7 | [Sequentially Ordinal Rank Tracker](https://leetcode.com/problems/sequentially-ordinal-rank-tracker/) | Dynamic ranking |
| 8 | [Count Integers in Intervals](https://leetcode.com/problems/count-integers-in-intervals/) | Interval union and advanced querying |

These are my recommendations based on coverage and transferable skills, not a verified ranking of questions asked by Google or Meta.

**One important point:** If you've already mastered Fenwick trees, segment trees, monotonic stacks, and iterator design through other DSA patterns, you don't need to solve all eight here. In that case, Dinner Plate Stacks, Sequentially Ordinal Rank Tracker, and Count Integers in Intervals are the most interesting remaining challenges.

After these, I'd move on rather than continuing to expand the Custom Data Structures list.
