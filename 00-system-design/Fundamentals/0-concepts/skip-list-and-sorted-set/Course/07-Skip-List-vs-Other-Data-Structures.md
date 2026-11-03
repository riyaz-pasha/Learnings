# Chapter 7 — Skip List vs. Other Data Structures

This chapter answers one of the most important engineering questions:

If a skip list provides expected \\(O(\log n)\\) search, insertion, and deletion, why would we choose it instead of a balanced tree, sorted array, or hash table?

The answer is that Big-O complexity is only one part of a data structure's performance. You also need to consider whether the data must remain sorted, how frequently it changes, whether queries operate on ranges, memory overhead, cache locality, concurrency, and worst-case guarantees.

Let's compare each alternative using the same example: maintaining a collection of users ordered by their scores.

## 1. The big-picture comparison

| Data structure       | Exact lookup                                                            | Insert / delete                                         | Ordered traversal             | Main strength                                                  |
| -------------------- | ----------------------------------------------------------------------- | ------------------------------------------------------- | ----------------------------- | -------------------------------------------------------------- |
| Unsorted array       | \\(O(n)\\)                                                              | Append: amortized \\(O(1)\\); arbitrary mutation varies | \\(O(n)\\) to scan            | Simple storage                                                 |
| Sorted array         | \\(O(\log n)\\)                                                         | \\(O(n)\\)                                              | \\(O(n)\\) traversal          | Fast search, excellent locality                                |
| Sorted linked list   | \\(O(n)\\)                                                              | \\(O(n)\\) including position search                    | \\(O(n)\\)                    | Simple pointer-based updates once position is known            |
| Hash table           | Expected \\(O(1)\\)                                                     | Expected \\(O(1)\\)                                     | No inherent sorted order      | Exact-key lookup                                               |
| Balanced search tree | \\(O(\log n)\\) worst case                                              | \\(O(\log n)\\) worst case                              | \\(O(n)\\) traversal          | Deterministic logarithmic operations                           |
| Skip list            | Expected \\(O(\log n)\\)                                                | Expected \\(O(\log n)\\)                                | \\(O(n)\\) traversal          | Ordered operations with relatively simple randomized balancing |
| B-tree / B+ tree     | \\(O(\log n)\\) in tree height; typically \\(O(\log_B n)\\) page visits | Similar tree-height cost                                | Efficient ordered range scans | Page-oriented storage and databases                            |
| Binary heap          | \\(O(n)\\) for arbitrary element lookup                                 | Insert / remove minimum: \\(O(\log n)\\)                | No complete sorted order      | Fast minimum or maximum retrieval                              |

The table describes typical operations and assumes suitable implementations. The precise costs of deletion, insertion, and lookup depend on the data structure's representation and which position or key is already known.

Let's understand the practical differences.

# 2. Skip list vs. sorted linked list

A sorted linked list looks like this:

\#chatgpt-mermaid-\_r_15o\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_15o\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_15o\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_15o\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_15o\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_15o\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_15o\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_15o\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_15o\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_15o\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_15o\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_15o\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_15o\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_15o\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_15o\_ p{margin:0;}#chatgpt-mermaid-\_r_15o\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_15o\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_15o\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_15o\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_15o\_ .label text,#chatgpt-mermaid-\_r_15o\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_15o\_ .node rect,#chatgpt-mermaid-\_r_15o\_ .node circle,#chatgpt-mermaid-\_r_15o\_ .node ellipse,#chatgpt-mermaid-\_r_15o\_ .node polygon,#chatgpt-mermaid-\_r_15o\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_15o\_ .rough-node .label text,#chatgpt-mermaid-\_r_15o\_ .node .label text,#chatgpt-mermaid-\_r_15o\_ .image-shape .label,#chatgpt-mermaid-\_r_15o\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_15o\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_15o\_ .rough-node .label,#chatgpt-mermaid-\_r_15o\_ .node .label,#chatgpt-mermaid-\_r_15o\_ .image-shape .label,#chatgpt-mermaid-\_r_15o\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_15o\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_15o\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_15o\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_15o\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_15o\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_15o\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_15o\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_15o\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_15o\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_15o\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_15o\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_15o\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_15o\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_15o\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_15o\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_15o\_ .icon-shape,#chatgpt-mermaid-\_r_15o\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_15o\_ .icon-shape p,#chatgpt-mermaid-\_r_15o\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_15o\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_15o\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_15o\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_15o\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_15o\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_15o\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_15o\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_15o\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_15o\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_15o\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_15o\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_15o\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_15o\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_15o\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_15o\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_15o\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_15o\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_15o\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_15o\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_15o\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_15o\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_15o\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_15o\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_15o\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_15o\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_15o\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_15o\_ .node rect,#chatgpt-mermaid-\_r_15o\_ .node circle,#chatgpt-mermaid-\_r_15o\_ .node ellipse,#chatgpt-mermaid-\_r_15o\_ .node polygon,#chatgpt-mermaid-\_r_15o\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_15o\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_15o\_ .node.mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_15o\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_15o\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_15o\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}102030405060

A skip list adds shortcut levels:

```
Level 3: HEAD ─────────→ 30 ─────────→ 60

Level 2: HEAD ──→ 20 ──→ 30 ──→ 50 ─→ 60

Level 1: HEAD → 10 → 20 → 30 → 40 → 50 → 60
```

The underlying elements remain sorted. The difference is the extra forward pointers.

### What does the skip list improve?

Consider searching for `50`.

In the sorted linked list, you traverse from the head:

`10 → 20 → 30 → 40 → 50`

In the skip list, you can move through upper levels and bypass intermediate nodes.

The sorted linked list has \\(O(n)\\) search time, whereas the skip list provides expected \\(O(\log n)\\) search time.

### But isn't linked-list insertion already \\(O(1)\\)?

Yes, provided the insertion position is already known and you have the necessary predecessor pointer.

For a singly linked list, inserting `35` after node `30` only requires changing a couple of references.

However, if you are given just the key `35`, you must first locate its sorted position. That search costs \\(O(n)\\) in a regular linked list.

The skip list locates the insertion position in expected \\(O(\log n)\\), then updates the necessary forward pointers.

### When should you choose each?

Use a sorted linked list when the collection is small or you frequently insert and delete around positions that are already known.

Consider a skip list when you need efficient key-based searches as well as frequent ordered updates.

Neither structure supports efficient arbitrary index access like an array does.

# 3. Skip list vs. sorted array

A sorted array is a strong competitor because binary search provides \\(O(\log n)\\) search.

Consider:

```
Index:  0   1   2   3   4   5
Value: 10  20  30  40  50  60
```

To search for `50`, binary search repeatedly examines the middle index and narrows the search interval.

An array supports direct index access, so it can inspect the middle element without traversing all preceding elements.

A linked list cannot do this efficiently.

### The important difference: mutation cost

Suppose we insert `35` into the sorted array.

Before:

```
[10, 20, 30, 40, 50, 60]
```

After:

```
[10, 20, 30, 35, 40, 50, 60]
```

The elements `40`, `50`, and `60` must shift to make room for the new value.

Inserting near the beginning can require shifting nearly the entire array.

Therefore:

- Sorted-array search: \\(O(\log n)\\).
- Sorted-array insertion: \\(O(n)\\) in general.
- Sorted-array deletion: \\(O(n)\\) in general.

A skip list instead updates a limited number of links after finding the insertion or deletion position.

Its expected search, insertion, and deletion costs are all \\(O(\log n)\\).

### The less obvious difference: cache locality

Arrays typically store their elements contiguously in memory.

Modern processors can often access sequential array elements efficiently because they exploit caches and prefetching.

A conventional skip list usually stores nodes as separately allocated objects. Following a pointer can require accessing a different memory location, potentially causing a cache miss.

So an array can outperform a skip list in practice for some read-heavy workloads, even though both offer logarithmic search.

This is an important engineering lesson:

Better asymptotic complexity does not always mean faster execution for every workload.

### When should you choose each?

| Workload                                         | Likely choice                                                     |
| ------------------------------------------------ | ----------------------------------------------------------------- |
| Mostly search, rarely modified                   | Sorted array                                                      |
| Binary search with compact memory layout         | Sorted array                                                      |
| Frequent ordered insertion and deletion          | Skip list or balanced tree                                        |
| Efficient index-based access                     | Array                                                             |
| Frequent sorted range queries with changing data | Skip list, balanced tree, or B+ tree depending on the environment |

A sorted array is often excellent when the dataset is static or changes in batches. A skip list is more attractive when the ordered collection changes continuously.

# 4. Skip list vs. hash table

This comparison is fundamental to backend engineering.

A hash table maps keys to values using a hash function.

For example:

```
"user-101" → User object
"user-102" → User object
"user-103" → User object
```

It is designed for efficient exact-key lookup.

Under typical hash-distribution assumptions, search, insertion, and deletion take expected \\(O(1)\\) time.

Why use a skip list if a hash table can do better for exact lookup?

Because a hash table does not inherently maintain the keys in sorted order.

## Consider a leaderboard

Suppose we store:

| User    | Score |
| ------- | ----- |
| Alice   | 120   |
| Bob     | 175   |
| Charlie | 150   |
| Diana   | 200   |

A hash table can efficiently find Alice's record.

But what if we need to:

- Return the top 10 users.
- Find all users with scores between 150 and 200.
- Find the first score greater than or equal to 175.
- Iterate through users in score order.
- Find a user's position in the leaderboard.

A hash table alone does not provide those ordered operations efficiently.

We would need to extract or scan elements and possibly sort them.

If we repeatedly sort all \\(n\\) elements to answer ordered queries, the sorting work is generally \\(O(n\log n)\\) per full sort.

A skip list maintains the order continuously, allowing expected logarithmic location of a starting position and efficient sequential traversal afterward.

### A common production design: use both

You do not have to choose only one data structure.

You can maintain:

\#chatgpt-mermaid-\_r_16q\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_16q\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_16q\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_16q\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_16q\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_16q\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_16q\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_16q\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_16q\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_16q\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_16q\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_16q\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_16q\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_16q\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_16q\_ p{margin:0;}#chatgpt-mermaid-\_r_16q\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_16q\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_16q\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_16q\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_16q\_ .label text,#chatgpt-mermaid-\_r_16q\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_16q\_ .node rect,#chatgpt-mermaid-\_r_16q\_ .node circle,#chatgpt-mermaid-\_r_16q\_ .node ellipse,#chatgpt-mermaid-\_r_16q\_ .node polygon,#chatgpt-mermaid-\_r_16q\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_16q\_ .rough-node .label text,#chatgpt-mermaid-\_r_16q\_ .node .label text,#chatgpt-mermaid-\_r_16q\_ .image-shape .label,#chatgpt-mermaid-\_r_16q\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_16q\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_16q\_ .rough-node .label,#chatgpt-mermaid-\_r_16q\_ .node .label,#chatgpt-mermaid-\_r_16q\_ .image-shape .label,#chatgpt-mermaid-\_r_16q\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_16q\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_16q\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_16q\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_16q\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_16q\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_16q\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_16q\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_16q\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_16q\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_16q\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_16q\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_16q\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_16q\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_16q\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_16q\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_16q\_ .icon-shape,#chatgpt-mermaid-\_r_16q\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_16q\_ .icon-shape p,#chatgpt-mermaid-\_r_16q\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_16q\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_16q\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_16q\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_16q\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_16q\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_16q\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_16q\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_16q\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_16q\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_16q\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_16q\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_16q\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_16q\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_16q\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_16q\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_16q\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_16q\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_16q\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_16q\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_16q\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_16q\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_16q\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_16q\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_16q\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_16q\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_16q\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_16q\_ .node rect,#chatgpt-mermaid-\_r_16q\_ .node circle,#chatgpt-mermaid-\_r_16q\_ .node ellipse,#chatgpt-mermaid-\_r_16q\_ .node polygon,#chatgpt-mermaid-\_r_16q\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_16q\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_16q\_ .node.mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_16q\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_16q\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_16q\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}Leaderboard requestHash tableSkip listExact user lookupScore ordering and rangequeries

The hash table provides fast member-to-record lookup. The skip list supports the ordered view.

This is precisely the kind of combination used by Redis sorted sets. Redis documentation describes its sorted sets as a structure combining a skip list and a hash table, with the skip list maintaining order and the hash table supporting member lookup.&#x20;

[image](https://www.google.com/s2/favicons?domain=https://redis.io\&sz=32)

Docs



### When should you choose each?

Choose a hash table when exact key lookup dominates and ordering is unnecessary.

Choose a skip list when ordered operations are central to the workload.

Use both when you need fast exact lookup and efficient ordering, provided the additional memory and update work are justified.

# 5. Skip list vs. balanced binary search trees

Now let's compare a skip list with two important tree structures:

- AVL tree.
- Red-black tree.

Both are balanced binary search trees.

A normal binary search tree can become a linked list if elements are inserted in sorted order.

Balanced trees prevent this through structural invariants and rotations.

For example, an AVL tree keeps its left and right subtree heights tightly balanced. A red-black tree uses coloring rules to keep its height logarithmic.

The important difference is how balance is maintained.

## Balanced tree

\#chatgpt-mermaid-\_r_17h\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_17h\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_17h\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_17h\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_17h\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_17h\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_17h\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_17h\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_17h\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_17h\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_17h\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_17h\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_17h\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_17h\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_17h\_ p{margin:0;}#chatgpt-mermaid-\_r_17h\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_17h\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_17h\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_17h\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_17h\_ .label text,#chatgpt-mermaid-\_r_17h\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_17h\_ .node rect,#chatgpt-mermaid-\_r_17h\_ .node circle,#chatgpt-mermaid-\_r_17h\_ .node ellipse,#chatgpt-mermaid-\_r_17h\_ .node polygon,#chatgpt-mermaid-\_r_17h\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_17h\_ .rough-node .label text,#chatgpt-mermaid-\_r_17h\_ .node .label text,#chatgpt-mermaid-\_r_17h\_ .image-shape .label,#chatgpt-mermaid-\_r_17h\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_17h\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_17h\_ .rough-node .label,#chatgpt-mermaid-\_r_17h\_ .node .label,#chatgpt-mermaid-\_r_17h\_ .image-shape .label,#chatgpt-mermaid-\_r_17h\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_17h\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_17h\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_17h\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_17h\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_17h\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_17h\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_17h\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_17h\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_17h\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_17h\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_17h\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_17h\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_17h\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_17h\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_17h\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_17h\_ .icon-shape,#chatgpt-mermaid-\_r_17h\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_17h\_ .icon-shape p,#chatgpt-mermaid-\_r_17h\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_17h\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_17h\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_17h\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_17h\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_17h\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_17h\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_17h\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_17h\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_17h\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_17h\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_17h\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_17h\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_17h\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_17h\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_17h\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_17h\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_17h\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_17h\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_17h\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_17h\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_17h\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_17h\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_17h\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_17h\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_17h\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_17h\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_17h\_ .node rect,#chatgpt-mermaid-\_r_17h\_ .node circle,#chatgpt-mermaid-\_r_17h\_ .node ellipse,#chatgpt-mermaid-\_r_17h\_ .node polygon,#chatgpt-mermaid-\_r_17h\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_17h\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_17h\_ .node.mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_17h\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_17h\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_17h\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}40206010305070

The tree has a hierarchy of child pointers. Searching follows one path from the root.

## Skip list

```
Level 3: HEAD ─────→ 40 ─────→ 70

Level 2: HEAD ─→ 20 → 40 ─→ 60 → 70

Level 1: HEAD → 10 → 20 → 30 → 40 → 50 → 60 → 70
```

The skip list has a hierarchy of forward links. Searching moves horizontally and vertically.

### The most important difference: guarantees

| Property                | Skip list                          | AVL / red-black tree                  |
| ----------------------- | ---------------------------------- | ------------------------------------- |
| Typical search          | Expected \\(O(\log n)\\)           | \\(O(\log n)\\)                       |
| Worst-case search       | \\(O(n)\\)                         | \\(O(\log n)\\)                       |
| Balance mechanism       | Randomized node heights            | Explicit balancing invariants         |
| Insertion               | Link updates at relevant levels    | Node links and potentially rotations  |
| Deletion                | Bypass pointers at relevant levels | Remove node and potentially rebalance |
| Ordered range traversal | Natural bottom-level traversal     | In-order traversal                    |
| Key ordering            | Required                           | Required                              |

The distinction is not that trees are always faster or that skip lists are always simpler in every implementation.

It is that balanced trees give a deterministic worst-case logarithmic guarantee, while ordinary skip lists give expected logarithmic performance with linear worst-case operations.

### Why is a skip list appealing to implement?

In a skip list, insertion typically involves:

1. Finding predecessors.
2. Randomly choosing a height.
3. Updating forward pointers.

Deletion typically involves:

1. Finding predecessors.
2. Bypassing the target at its levels.
3. Adjusting the current active level if necessary.

There are no rotations or explicit subtree-height or color invariants to maintain.

That makes the algorithms comparatively straightforward to understand.

The trade-off is that each node has a variable number of forward pointers and the structure is probabilistically balanced.

### When should you choose each?

A balanced tree is attractive when deterministic worst-case performance is essential.

A skip list is attractive when randomized balancing and ordered updates fit the use case, and its memory and implementation characteristics are favorable.

For example, if you are designing a real-time component with a strict worst-case latency requirement, the ordinary skip-list guarantee alone may not be sufficient.

# 6. Skip list vs. B-tree and B+ tree

This comparison matters when moving from in-memory data structures to databases and storage engines.

A binary search tree stores a small number of child pointers per node.

A B-tree can store many keys and child pointers in each node.

A B+ tree typically stores data entries or references in its leaves, while internal nodes primarily guide searches. Leaf nodes are commonly linked to support ordered range scans.

Conceptually:

\#chatgpt-mermaid-\_r_17v\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_17v\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_17v\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_17v\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_17v\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_17v\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_17v\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_17v\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_17v\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_17v\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_17v\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_17v\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_17v\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_17v\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_17v\_ p{margin:0;}#chatgpt-mermaid-\_r_17v\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_17v\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_17v\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_17v\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_17v\_ .label text,#chatgpt-mermaid-\_r_17v\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_17v\_ .node rect,#chatgpt-mermaid-\_r_17v\_ .node circle,#chatgpt-mermaid-\_r_17v\_ .node ellipse,#chatgpt-mermaid-\_r_17v\_ .node polygon,#chatgpt-mermaid-\_r_17v\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_17v\_ .rough-node .label text,#chatgpt-mermaid-\_r_17v\_ .node .label text,#chatgpt-mermaid-\_r_17v\_ .image-shape .label,#chatgpt-mermaid-\_r_17v\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_17v\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_17v\_ .rough-node .label,#chatgpt-mermaid-\_r_17v\_ .node .label,#chatgpt-mermaid-\_r_17v\_ .image-shape .label,#chatgpt-mermaid-\_r_17v\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_17v\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_17v\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_17v\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_17v\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_17v\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_17v\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_17v\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_17v\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_17v\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_17v\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_17v\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_17v\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_17v\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_17v\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_17v\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_17v\_ .icon-shape,#chatgpt-mermaid-\_r_17v\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_17v\_ .icon-shape p,#chatgpt-mermaid-\_r_17v\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_17v\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_17v\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_17v\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_17v\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_17v\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_17v\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_17v\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_17v\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_17v\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_17v\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_17v\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_17v\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_17v\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_17v\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_17v\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_17v\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_17v\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_17v\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_17v\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_17v\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_17v\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_17v\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_17v\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_17v\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_17v\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_17v\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_17v\_ .node rect,#chatgpt-mermaid-\_r_17v\_ .node circle,#chatgpt-mermaid-\_r_17v\_ .node ellipse,#chatgpt-mermaid-\_r_17v\_ .node polygon,#chatgpt-mermaid-\_r_17v\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_17v\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_17v\_ .node.mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_17v\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_17v\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_17v\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}Root: separator keysInternal nodeInternal nodeLeaf pageLeaf pageLeaf pageLeaf pageNext leafNext leafNext leaf

The exact layout varies by implementation, but the important point is that each tree node can hold multiple keys.

## Why is that useful?

Suppose a database index is stored on disk.

Reading one page from storage can be much more expensive than comparing a few keys in memory.

A B-tree or B+ tree groups many keys into a page. A single page read lets the database inspect multiple keys and choose the next child.

If the branching factor is \\(B\\), the tree height is approximately:

\\[ O(\log_B n) \\]

This is the number of tree levels, corresponding roughly to the number of page visits for a point lookup under a simplified disk-oriented model.

A high branching factor can make the tree shallow, reducing expensive page accesses.

A traditional pointer-based skip list instead moves between individual nodes. It is often attractive in memory, but its pointer-heavy structure is not automatically as well suited to page-oriented storage.

### When should you choose each?

| Use case                                  | Typical candidate                         |
| ----------------------------------------- | ----------------------------------------- |
| In-memory ordered collection              | Skip list or balanced tree                |
| Database index persisted on disk          | B+ tree or another storage-oriented index |
| Efficient database range scan             | B+ tree is a common choice                |
| Large page-oriented index                 | B-tree family                             |
| Frequently modified in-memory ordered set | Skip list or balanced tree                |

This does not mean skip lists cannot be adapted to storage systems. Specialized variants exist. The point is that the physical storage model significantly influences which structure is a good fit.

# 7. Skip list vs. heap

A heap is a different kind of data structure.

A min-heap guarantees that the smallest element appears at the root. It does not guarantee that every element is globally sorted relative to every other element.

For example:

```
        10
       /  \
      20   15
     /  \
    40  30
```

The heap property requires each parent to be no larger than its children. It does not require the entire structure to be sorted from left to right.

### Compare the operations

| Operation                     | Min-heap                                       | Skip list                                |
| ----------------------------- | ---------------------------------------------- | ---------------------------------------- |
| Find minimum                  | \\(O(1)\\)                                     | \\(O(1)\\)                               |
| Insert                        | \\(O(\log n)\\)                                | Expected \\(O(\log n)\\)                 |
| Remove minimum                | \\(O(\log n)\\)                                | Expected \\(O(\log n)\\)                 |
| Find arbitrary key            | \\(O(n)\\)                                     | Expected \\(O(\log n)\\)                 |
| Iterate in fully sorted order | Requires extra work, often repeated extraction | \\(O(n)\\) traversal of the ordered list |
| Find elements in a key range  | Not naturally supported                        | Expected \\(O(\log n+k)\\)               |

A heap is excellent for priority queues where you repeatedly remove the next minimum or maximum element.

A skip list is better suited to maintaining a fully ordered collection with arbitrary key searches and range queries.

Do not choose a skip list just because you need the minimum element quickly. A heap already solves that problem efficiently. Choose the structure according to the full operation mix.

# 8. Cache locality: a hidden factor in real-world performance

Big-O analysis counts operations as the collection grows. It does not directly describe the cost of each memory access.

Consider two implementations:

- A sorted array stores elements in contiguous memory.
- A pointer-based skip list stores nodes with references to other nodes, often allocated at different memory addresses.

The array's sequential accesses are generally cache-friendly.

The skip list may jump between different memory locations, and those pointer accesses can cause cache misses.

This can make pointer-based skip lists slower than expected for some workloads, even though they perform fewer logical comparisons than linear search.

A balanced tree also commonly uses pointer-based nodes, while B-trees mitigate some of this cost by storing multiple keys in each node.

However, there is no universal rule that arrays always win or skip lists always lose. The result depends on the language runtime, allocator, dataset size, data layout, query distribution, and processor.

The practical lesson is:

Choose a suitable asymptotic structure first, then benchmark its actual performance under the intended workload.

# 9. A concurrency advantage: Java's `ConcurrentSkipListMap`

Skip lists are not automatically thread-safe.

The simple Java implementation from our earlier chapters updates multiple forward pointers without coordination. Concurrent insertion, deletion, and search could observe inconsistent intermediate states or overwrite another operation's changes.

Java provides a concurrent implementation:

`ConcurrentSkipListMap<K, V>`

It implements a sorted, concurrent navigable map. The Java API documents expected average logarithmic costs for `get`, `put`, `remove`, and related operations. It also specifies that its iterators are weakly consistent, meaning they can safely be used while updates occur but do not represent a frozen snapshot of the entire map.&#x20;

[image](https://www.google.com/s2/favicons?domain=https://docs.oracle.com\&sz=32)

Oracle Docs



Example:

```
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;

public class Example {
    public static void main(String[] args) {
        ConcurrentNavigableMap<Integer, String> events =
                new ConcurrentSkipListMap<>();

        events.put(300, "Event C");
        events.put(100, "Event A");
        events.put(200, "Event B");

        // Ordered output by key.
        events.forEach((key, value) ->
                System.out.println(key + ": " + value));

        // Find the first key >= 150.
        System.out.println(events.ceilingKey(150)); // 200

        // Retrieve an ordered range of keys.
        System.out.println(events.subMap(100, true, 250, false));
    }
}
```

This is useful for in-memory indexes, ordered registries, scheduling metadata, and other workloads that need concurrent access to sorted keys.

It is not a universal replacement for a `ConcurrentHashMap`.

If you need only exact-key lookup and do not need sorted operations, a hash-based concurrent map is often a more natural choice.

Also, a concurrent collection does not automatically make a group of separate operations atomic. If your business invariant requires multiple map operations to happen as one indivisible unit, you need to design for that separately.

# 10. How should you choose a data structure in a system design interview?

Start with the operations, not the name of the data structure.

Suppose an interviewer asks you to design an in-memory leaderboard with millions of users.

Ask yourself:

Do we need exact user lookup?

If yes, a hash table is a natural fit.

Do we need users ranked by score?

A sorted structure such as a skip list or balanced tree is a natural fit.

Do we need frequent insertions, deletions, and score changes?

That favors a dynamic ordered structure over a sorted array.

Do we need fast rank queries?

The chosen structure must support rank efficiently, potentially through an augmented skip list with spans or a tree augmented with subtree sizes.

Do we need strict worst-case logarithmic guarantees?

That weighs in favor of a correctly maintained balanced tree over an ordinary skip list.

Is the index stored on disk?

A B+ tree or another storage-oriented index may be more appropriate.

A simplified decision flow is:

\#chatgpt-mermaid-\_r_18q\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_18q\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_18q\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_18q\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_18q\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_18q\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_18q\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_18q\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_18q\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_18q\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_18q\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_18q\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_18q\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_18q\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_18q\_ p{margin:0;}#chatgpt-mermaid-\_r_18q\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_18q\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_18q\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_18q\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_18q\_ .label text,#chatgpt-mermaid-\_r_18q\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_18q\_ .node rect,#chatgpt-mermaid-\_r_18q\_ .node circle,#chatgpt-mermaid-\_r_18q\_ .node ellipse,#chatgpt-mermaid-\_r_18q\_ .node polygon,#chatgpt-mermaid-\_r_18q\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_18q\_ .rough-node .label text,#chatgpt-mermaid-\_r_18q\_ .node .label text,#chatgpt-mermaid-\_r_18q\_ .image-shape .label,#chatgpt-mermaid-\_r_18q\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_18q\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_18q\_ .rough-node .label,#chatgpt-mermaid-\_r_18q\_ .node .label,#chatgpt-mermaid-\_r_18q\_ .image-shape .label,#chatgpt-mermaid-\_r_18q\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_18q\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_18q\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_18q\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_18q\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_18q\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_18q\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_18q\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_18q\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_18q\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_18q\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_18q\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_18q\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_18q\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_18q\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_18q\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_18q\_ .icon-shape,#chatgpt-mermaid-\_r_18q\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_18q\_ .icon-shape p,#chatgpt-mermaid-\_r_18q\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_18q\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_18q\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_18q\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_18q\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_18q\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_18q\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_18q\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_18q\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_18q\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_18q\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_18q\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_18q\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_18q\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_18q\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_18q\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_18q\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_18q\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_18q\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_18q\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_18q\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_18q\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_18q\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_18q\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_18q\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_18q\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_18q\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_18q\_ .node rect,#chatgpt-mermaid-\_r_18q\_ .node circle,#chatgpt-mermaid-\_r_18q\_ .node ellipse,#chatgpt-mermaid-\_r_18q\_ .node polygon,#chatgpt-mermaid-\_r_18q\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_18q\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_18q\_ .node.mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_18q\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_18q\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_18q\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}What operations dominate?Exact lookup only?Hash tableNeed sorted or rangequeries?Choose based on otheroperationsMostly static andread-heavy?Sorted arrayNeed deterministicworst-case O(log n)?Balanced search treeConsider skip listFor page-oriented databaseindexing, consider B+ treeYesNoNoYesYesNoYesNo

This is a starting point rather than a rigid rule. Real systems may combine several structures to serve different query patterns.

# 11. Three production scenarios

Let's connect the theory to concrete engineering decisions.

### Scenario A: API rate limiter using a sliding window

Suppose you need to retain timestamps for requests during the last 60 seconds.

You need to insert timestamps, remove timestamps that fall outside the window, and inspect the count or entries for the current window.

An ordered structure is useful because the oldest timestamps are at one end of the collection.

A skip list is one possible implementation, but it is not always necessary. A deque can be simpler and more efficient if timestamps arrive in monotonic order and the workload only needs insertion at the back and expiry from the front.

Lesson: A skip list is appropriate for general ordered operations, but a specialized structure may be better when the workload has stronger constraints.

### Scenario B: Concurrent in-memory ordered registry

Suppose multiple threads update and query records keyed by an ordered numeric identifier. The application needs lower-bound searches, ordered iteration, and navigable views.

A concurrent skip-list map is a candidate because it supports sorted keys and concurrent access.

A hash-based concurrent map would not directly supply the ordered navigation operations.

Lesson: Choose a skip list when ordered navigation is a central part of the concurrent workload, not merely because it is concurrent.

### Scenario C: Database index for a large persistent table

Suppose the data must be indexed on disk and queried by key range.

A B+ tree is a conventional candidate because its high branching factor and page-oriented layout help reduce storage reads.

A conventional in-memory skip list is not automatically the best match for that storage model.

Lesson: The environment in which the structure lives is as important as the abstract operations it supports.

## 12. Final comparison: memorize the decision, not just the table

| Choose           | When                                                                                                              |
| ---------------- | ----------------------------------------------------------------------------------------------------------------- |
| Sorted array     | Reads dominate, the collection changes rarely, and compact memory layout matters                                  |
| Linked list      | You already know positions, and simple sequential updates are sufficient                                          |
| Hash table       | Exact-key lookup dominates and order is unnecessary                                                               |
| Balanced tree    | Ordered operations are needed with deterministic worst-case logarithmic guarantees                                |
| Skip list        | Ordered operations are needed, randomized balancing is acceptable, and its pointer-based design fits the workload |
| B-tree / B+ tree | Page-oriented storage or database indexing is central                                                             |
| Heap             | You primarily need fast access to the minimum or maximum priority                                                 |

If you remember one idea from this chapter, make it this:

A skip list is not a universally faster linked list or a universally better balanced tree. It is a trade-off: expected logarithmic ordered operations, relatively straightforward update algorithms, linear expected space, and a linear worst case.

It is especially valuable when the application needs an ordered collection that changes frequently and benefits from efficient range retrieval.

## Chapter 7 — Interview questions

1\. Why can a skip list be preferable to a sorted array?

Both provide efficient search, but insertion and deletion in a sorted array usually require shifting elements, taking \\(O(n)\\) time. A skip list can perform these operations in expected \\(O(\log n)\\) time by updating pointers.

2\. Why not use a hash table instead?

A hash table is excellent for exact-key lookup, but it does not inherently maintain order. A skip list supports ordered traversal, lower-bound searches, and efficient range queries.

3\. What is the main difference between a skip list and a red-black tree?

A skip list uses randomized heights to achieve expected logarithmic operations. A red-black tree maintains explicit balancing invariants and guarantees logarithmic worst-case operations.

4\. Why can a sorted array outperform a skip list in practice?

Contiguous memory, cache locality, fewer allocations, and lower per-element overhead can make sorted-array operations faster for read-heavy or relatively static workloads.

5\. Why are B+ trees common in databases?

Their high branching factor and page-oriented structure reduce tree height and can reduce expensive storage-page accesses. They also support efficient ordered range scans.

6\. Does `ConcurrentSkipListMap` guarantee a consistent snapshot during iteration?

No. Its iterators are weakly consistent, so they can operate safely alongside updates without guaranteeing a snapshot of all mappings at one instant.&#x20;

[image](https://www.google.com/s2/favicons?domain=https://docs.oracle.com\&sz=32)

Oracle Docs



## What comes next?

We now understand where skip lists fit among the major data structures.

The next chapter focuses on Redis, where the skip list becomes a practical production design rather than just a textbook structure. We will examine how sorted sets combine a hash table with a skip list, how score ordering and ties work, what spans and backward pointers contribute, and how operations such as `ZADD`, `ZRANGE`, and `ZRANK` use the underlying structure.

Type `next` to continue to Chapter 8 — Redis Internals: How Sorted Sets Use Skip Lists.

