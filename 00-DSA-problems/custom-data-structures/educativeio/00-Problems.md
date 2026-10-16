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

