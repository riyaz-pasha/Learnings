# Chapter 6 — Skip List Complexity: Expected, Worst Case, Space and Practical Performance

In Chapter 5, we derived why random promotion creates a hierarchy with logarithmic expected height.

Now we will analyze every important operation more carefully and understand what those complexity claims actually mean.

This chapter is particularly important for interviews and system design because saying “skip lists are \\(O(\log n)\\)” is incomplete. You need to explain which complexity guarantee you have, what assumptions it depends on, and when real-world performance might differ from the theoretical result.

## 1. The complete complexity table

Let's begin with the usual skip-list operations.

| Operation                                                            | Expected time                   | Worst-case time |
| -------------------------------------------------------------------- | ------------------------------- | --------------- |
| Search by key                                                        | \\(O(\log n)\\)                 | \\(O(n)\\)      |
| Insert by key                                                        | \\(O(\log n)\\)                 | \\(O(n)\\)      |
| Delete by key                                                        | \\(O(\log n)\\)                 | \\(O(n)\\)      |
| Find minimum                                                         | \\(O(1)\\)                      | \\(O(1)\\)      |
| Find maximum                                                         | \\(O(\log n)\\) normally        | \\(O(n)\\)      |
| Find lower bound                                                     | \\(O(\log n)\\)                 | \\(O(n)\\)      |
| Find upper bound                                                     | \\(O(\log n)\\)                 | \\(O(n)\\)      |
| Iterate through all elements                                         | \\(O(n)\\)                      | \\(O(n)\\)      |
| Retrieve \\(k\\) elements in a sorted range after locating its start | \\(O(\log n+k)\\)               | \\(O(n+k)\\)    |
| Retrieve the element at a given rank                                 | \\(O(n)\\) in a basic skip list | \\(O(n)\\)      |

These results assume the standard randomized skip-list structure and an appropriate maximum-height policy. For range operations, \\(k\\) denotes the number of elements returned.

There are two important qualifications.

First, expected time is not a guarantee that every individual operation takes that amount of time. A poor random structure can require linear work.

Second, some implementations augment the skip list with extra information. For example, storing spans between nodes can support rank queries in expected \\(O(\log n)\\) time. Redis uses this technique in its skip-list-backed sorted sets.

Let's derive these results rather than memorize the table.

## 2. Why is search expected \\(O(\log n)\\)?

We already understand the search algorithm:

1. Start at the highest active level.
2. Move forward while the next node is smaller than the target.
3. Descend when the next node is too large.
4. Repeat until the bottom level.

With promotion probability \\(p\\), the expected number of levels grows logarithmically with the number of nodes.

For fixed \\(0\<p<1\\), the expected search work in the standard model has the asymptotic bound

\\[ \mathbb{E}[T\_{\text{search}}(n)] = O(\log n). \\]

Why doesn't searching each level cost \\(O(n)\\)?

Because we do not traverse all \\(n\\) nodes at every level.

Higher levels are progressively sparser, and a typical search moves through only a small number of nodes at each level before descending.

The expected horizontal work per level is bounded by a constant when the promotion probability is fixed. The number of levels is logarithmic, so total expected work is logarithmic.

The original skip-list analysis describes this probabilistic balancing approach and compares its performance with balanced search trees.&#x20;

[image](https://www.google.com/s2/favicons?domain=https://rtheunissen.github.io\&sz=32)

rtheunissen.github.io

+1



### What about worst-case search?

Imagine this valid structure:

```
Level 1: HEAD → 10 → 20 → 30 → 40 → 50 → 60 → ... → NIL
```

Every data node has height 1.

There are no shortcuts.

Searching for a value near the end requires examining nearly every node:

\\[ T\_{\text{search}}(n)=O(n). \\]

Therefore, the correct statement is:

A standard skip list provides expected logarithmic search time, but linear worst-case search time.

This is different from a properly maintained AVL tree or red-black tree, whose search operations have deterministic \\(O(\log n)\\) worst-case bounds.

## 3. Why is insertion expected \\(O(\log n)\\)?

Remember the insertion algorithm from Chapter 4.

To insert a key, we first find its predecessor at each relevant level.

\#chatgpt-mermaid-\_r_130\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_130\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_130\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_130\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_130\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_130\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_130\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_130\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_130\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_130\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_130\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_130\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_130\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_130\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_130\_ p{margin:0;}#chatgpt-mermaid-\_r_130\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_130\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_130\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_130\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_130\_ .label text,#chatgpt-mermaid-\_r_130\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_130\_ .node rect,#chatgpt-mermaid-\_r_130\_ .node circle,#chatgpt-mermaid-\_r_130\_ .node ellipse,#chatgpt-mermaid-\_r_130\_ .node polygon,#chatgpt-mermaid-\_r_130\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_130\_ .rough-node .label text,#chatgpt-mermaid-\_r_130\_ .node .label text,#chatgpt-mermaid-\_r_130\_ .image-shape .label,#chatgpt-mermaid-\_r_130\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_130\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_130\_ .rough-node .label,#chatgpt-mermaid-\_r_130\_ .node .label,#chatgpt-mermaid-\_r_130\_ .image-shape .label,#chatgpt-mermaid-\_r_130\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_130\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_130\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_130\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_130\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_130\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_130\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_130\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_130\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_130\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_130\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_130\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_130\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_130\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_130\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_130\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_130\_ .icon-shape,#chatgpt-mermaid-\_r_130\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_130\_ .icon-shape p,#chatgpt-mermaid-\_r_130\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_130\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_130\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_130\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_130\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_130\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_130\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_130\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_130\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_130\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_130\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_130\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_130\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_130\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_130\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_130\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_130\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_130\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_130\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_130\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_130\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_130\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_130\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_130\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_130\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_130\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_130\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_130\_ .node rect,#chatgpt-mermaid-\_r_130\_ .node circle,#chatgpt-mermaid-\_r_130\_ .node ellipse,#chatgpt-mermaid-\_r_130\_ .node polygon,#chatgpt-mermaid-\_r_130\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_130\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_130\_ .node.mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_130\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_130\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_130\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}Start at highest active levelFind predecessor at currentlevelMore lower levels?Descend and continueChoose random node heightUpdate affected forwardpointersFinish insertionYesNo

The work consists of three parts.

Part 1: Find the insertion position.

This is the same traversal used for search.

Expected cost:

\\[ O(\log n). \\]

Part 2: Generate the node height.

With fixed promotion probability \\(p\\), expected height is

\\[ \mathbb{E}[H]=\frac{1}{1-p} \\]

in the uncapped model.

For a fixed \\(p\\), this is constant expected work.

Part 3: Update pointers.

If the new node has height \\(h\\), we update pointers at \\(h\\) levels.

The pointer-update cost is \\(O(h)\\). Because expected height is constant for fixed \\(p\\), this contributes constant expected work.

Combining the costs:

\\[ O(\log n)+O(1)+O(1) \\]

Therefore,

\\[ \boxed{\mathbb{E}[T\_{\text{insert}}(n)]=O(\log n)} \\]

### What if the inserted node has height 15?

The insertion will update 15 levels instead of perhaps one or two.

That particular insertion involves more pointer updates.

However, the random-height distribution makes very tall nodes uncommon, and finding the insertion position still accounts for the logarithmic search work.

This is why the expected insertion cost remains logarithmic.

### Worst-case insertion

Suppose the skip list has no useful upper-level shortcuts.

Finding the insertion position takes \\(O(n)\\) time.

Once the position has been found, linking the new node costs \\(O(h)\\), which is bounded by the configured maximum height \\(M\\).

So the general worst-case bound is

\\[ O(n+M). \\]

For an ordinary implementation where \\(M=O(\log n)\\) or is a fixed supported limit, this simplifies to

\\[ \boxed{O(n)}. \\]

The important point is that the search for the insertion position, not the pointer-update loop, dominates the worst case.

## 4. Why is deletion expected \\(O(\log n)\\)?

Deletion follows the same reasoning.

To delete a key, we:

1. Search for its predecessor at each level.
2. Check whether the target exists.
3. Redirect the predecessor pointers to bypass the target.
4. Reduce the active level if the highest levels become empty.

The predecessor search costs expected \\(O(\log n)\\).

If the deleted node has height \\(h\\), the structural update costs \\(O(h)\\). With fixed promotion probability, expected height is constant. Reducing the active level costs at most the configured maximum number of iterations.

Therefore,

\\[ \boxed{\mathbb{E}[T\_{\text{delete}}(n)]=O(\log n)} \\]

The worst-case deletion time is \\(O(n)\\), because finding the target's predecessors can require a linear traversal.

Notice the pattern: search, insertion, and deletion all share the same logarithmic expected search cost. Insertion and deletion then perform comparatively little additional work.

## 5. What does expected complexity actually mean?

This deserves special attention.

Suppose we construct 1,000 different skip lists containing the same 100,000 keys, using independent random height assignments.

One structure might be very well shaped.

Another might have a less useful distribution of shortcuts.

Most should have behavior consistent with the expected logarithmic analysis, but their exact search costs will differ.

The expected cost is the average over the random choices under the mathematical model.

Formally, for a random execution cost \\(T\\),

\\[ \mathbb{E}[T] = \sum_t t \cdot P(T=t) \\]

when the cost is represented by a discrete distribution.

For example, if an operation has these hypothetical costs:

| Cost      | Probability |
| --------- | ----------- |
| 10 steps  | 50%         |
| 20 steps  | 40%         |
| 100 steps | 10%         |

Then its expected cost is

\\[ \mathbb{E}[T] = 10(0.5)+20(0.4)+100(0.1) =23. \\]

The operation costs 23 steps on average under this distribution, but no listed outcome requires it to take exactly 23 steps.

This example is only to explain expectation; it is not a measured skip-list distribution.

### Why is this distinction important in production?

Expected latency does not directly tell you the 99th-percentile latency.

A service might care about:

- Average or expected operation time.
- 95th-percentile latency.
- 99th-percentile latency.
- Maximum allowed latency.
- Behavior during concurrent workloads.

The theoretical expected bound is valuable, but performance-sensitive systems should also benchmark realistic workloads and monitor latency distributions.

## 6. Expected complexity vs. amortized complexity

These two terms are often confused in interviews.

### Expected complexity

Expected complexity accounts for randomness.

A skip list uses randomly assigned node heights. Its expected \\(O(\log n)\\) operation cost is derived from the distribution of those heights.

The expected result does not mean the implementation can never perform a linear-time operation.

### Amortized complexity

Amortized analysis studies the total cost of a sequence of operations and spreads expensive operations across that sequence.

For example, a dynamic array occasionally reallocates and copies its contents. Although one insertion can cost \\(O(n)\\), append operations have amortized \\(O(1)\\) cost under the standard dynamic-array model.

The standard skip-list guarantee is usually expressed as expected logarithmic time, rather than as the classic amortized guarantee used for dynamic arrays.

Consider a sequence of \\(n\\) independent insertions into a skip list.

If insertion \\(i\\) has expected cost \\(O(\log i)\\), then linearity of expectation gives

\\[ \begin{aligned} \mathbb{E}[T\_{\text{total}}] &= \sum\_{i=1}^{n} O(\log i)\\\ &= O(n\log n). \end{aligned} \\]

Therefore, the expected total cost of inserting \\(n\\) elements one at a time is

\\[ \boxed{O(n\log n)}. \\]

This remains true even if the input keys arrive in sorted order, provided the node heights are independently randomized according to the usual policy.

An implementation can also be designed to construct a skip list from already sorted data more directly, but that is a separate construction algorithm.

The main lesson is that expected and amortized analyses answer different questions. Do not use the terms interchangeably.

## 7. What about space complexity?

Every data node must appear at the bottom level.

In addition, a node of height \\(h\\) stores \\(h\\) forward pointers.

The total number of pointer slots is

\\[ S=\sum\_{i=1}^{n}H_i. \\]

With promotion probability \\(p\\), the expected height of a node is

\\[ \mathbb{E}[H_i]=\frac{1}{1-p} \\]

in the uncapped model.

Consequently,

\\[ \begin{aligned} \mathbb{E}[S] &=\sum\_{i=1}^{n}\mathbb{E}[H_i]\\\ &=\frac{n}{1-p}. \end{aligned} \\]

For fixed \\(p\\),

\\[ \boxed{\mathbb{E}[S]=O(n)}. \\]

Thus, a standard skip list uses expected linear space.

### Does every node have exactly two pointers when \\(p=0.5\\)?

No.

The expected node height is two, but individual nodes can have different heights.

For example:

```
Node 10: height 1
Node 20: height 2
Node 30: height 1
Node 40: height 4
Node 50: height 1
```

The average is influenced by the whole distribution, including uncommon tall nodes.

It would be incorrect to claim that each node actually stores exactly two pointer slots.

### What about worst-case space?

If the configured maximum height is \\(M\\), every node has at most \\(M\\) pointer slots.

Therefore, a general upper bound is

\\[ O(nM). \\]

If \\(M\\) is fixed, that is \\(O(n)\\).

If \\(M\\) is allowed to grow with \\(n\\), such as \\(O(\log n)\\), the worst case can be \\(O(n\log n)\\) if many nodes have maximum height.

The expected space remains \\(O(n)\\) under the standard random-height model.

Remember the distinction:

- Expected space: \\(O(n)\\).
- Worst-case space: \\(O(nM)\\), given a maximum per-node height \\(M\\).

## 8. Why are range queries particularly useful?

Suppose you are storing ordered scores:

```
100 → 110 → 120 → 130 → 140 → 150 → 160 → 170
```

You want all scores between `120` and `150`.

A skip list can:

1. Search for the first relevant key.
2. Continue traversing the bottom-level linked list.
3. Stop when the upper bound is exceeded.

The cost of finding the first relevant position is expected \\(O(\log n)\\).

If \\(k\\) elements are returned, traversing them costs \\(O(k)\\).

Therefore,

\\[ \boxed{O(\log n+k)} \\]

in expected time.

This is useful because no data structure can return \\(k\\) individual elements in less than \\(O(k)\\) time if those elements must actually be emitted.

The range-query complexity shows why skip lists are attractive for ordered workloads.

They combine efficient search with natural sequential traversal.

The same principle appears in databases, ordered indexes, in-memory ranking systems, and sorted collections.

### Lower bound and upper bound

A lower-bound operation finds the first element whose key is greater than or equal to a given value.

An upper-bound operation finds the first element whose key is strictly greater than the given value.

Both can use the same top-down traversal as exact search, with slightly different comparison conditions.

For example, if the sorted keys are

`10, 20, 30, 40, 50`

then:

- Lower bound of `35` is `40`.
- Upper bound of `40` is `50`.
- Lower bound of `40` is `40`.

These are expected \\(O(\log n)\\) operations in an ordinary skip list.

## 9. Why is finding the minimum \\(O(1)\\)?

The minimum is the first element in sorted order.

A skip list's head sentinel has a direct pointer to the first node at the bottom level.

Therefore, if the list is nonempty:

```
Node minimum = head.forward[0];
```

This is a direct pointer access.

So finding the minimum takes

\\[ \boxed{O(1)}. \\]

If the list is empty, the pointer is null and the implementation reports that no minimum exists.

### What about the maximum?

The maximum is the last element.

If we maintain a tail pointer, retrieving the maximum can be \\(O(1)\\).

Without a tail pointer, we can find the maximum using the usual top-down traversal: move forward whenever possible and descend when no further node exists at the current level.

That takes expected \\(O(\log n)\\) time and \\(O(n)\\) in the worst case.

Maintaining a tail pointer is a design choice. It improves maximum lookup but adds update logic when inserting or deleting the last node.

## 10. Can a basic skip list find the \\(k\\)-th element in \\(O(\log n)\\)?

Not necessarily.

A standard skip list stores forward pointers, but it does not automatically store how many bottom-level elements each pointer skips.

Suppose you want the element at zero-based index `500,000`.

The key-based search algorithm does not directly answer this question because the target is specified by position, not by key.

A simple implementation may need to traverse the bottom list until reaching that index, taking \\(O(n)\\) time in the worst case.

### How can we improve this?

We can augment each forward pointer with a span that records the number of bottom-level nodes crossed by that pointer.

Then the search can maintain a running rank while moving forward.

If the next pointer's span would take us beyond the desired rank, we descend instead.

This enables expected \\(O(\log n)\\) rank queries and selection by rank when the spans are maintained correctly.

Redis uses spans in its skip-list implementation for sorted sets.

This is a good example of a general data-structure principle:

The complexity of an operation depends not only on the basic structure but also on what additional information the structure maintains.

We will explore this implementation detail in the Redis chapter.

## 11. The fixed maximum-height trap

Our earlier Java example used:

```
private static final int MAX_LEVEL = 16;
```

This is a useful teaching example, but it has a limitation.

If the collection grows indefinitely while the configured maximum stays fixed, the highest available level can eventually contain many nodes.

The expected population of the top level, indexed as one-based level \\(M\\), is approximately

\\[ np^{M-1}. \\]

If \\(M\\) is fixed, this quantity increases with \\(n\\).

Eventually, the top-level linked list itself can become long, weakening the expected logarithmic search behavior for arbitrarily large collections.

Therefore, the standard asymptotic expected \\(O(\log n)\\) result assumes that sufficient levels are available as \\(n\\) grows, or that the selected maximum height is appropriate for the supported capacity.

In practical systems, the configured maximum is chosen to accommodate the expected collection size. Some implementations use a deliberately generous bound rather than dynamically resizing their maximum-level array.

For a theoretical structure that must scale without a fixed upper limit on \\(n\\), the height policy needs to account for growth.

## 12. Skip lists vs. balanced trees: performance guarantees

Let's compare the two data structures more carefully.

| Property                              | Skip list                             | AVL tree / red-black tree                                            |
| ------------------------------------- | ------------------------------------- | -------------------------------------------------------------------- |
| Search                                | Expected \\(O(\log n)\\)              | Worst-case \\(O(\log n)\\)                                           |
| Insertion                             | Expected \\(O(\log n)\\)              | Worst-case \\(O(\log n)\\)                                           |
| Deletion                              | Expected \\(O(\log n)\\)              | Worst-case \\(O(\log n)\\)                                           |
| Balancing mechanism                   | Randomized heights                    | Explicit structural invariants                                       |
| Worst-case linear operation possible? | Yes                                   | Not for the standard operations under valid tree invariants          |
| Local pointer updates                 | Yes                                   | Yes, with rotations/rebalancing where needed                         |
| Sequential ordered traversal          | Natural linked-list traversal         | In-order tree traversal                                              |
| Memory overhead                       | Multiple forward pointers per node    | Child pointers and balancing metadata                                |
| Concurrency                           | Specialized concurrent variants exist | Specialized concurrent variants exist; implementation details matter |

The skip list's advantage is not that it has a better asymptotic worst-case bound. It does not.

Its advantage is that the algorithms can be comparatively simple while giving good expected performance.

Balanced trees are preferable when the deterministic worst-case logarithmic guarantee is important.

Skip lists are attractive when randomized balancing and ordered traversal fit the workload, and when their practical memory and performance characteristics are favorable.

## 13. A practical way to think about the complexity

Suppose your application maintains one million ordered entries.

You search, insert, delete, and retrieve short ranges throughout the day.

A reasonable skip-list model gives:

- Expected logarithmic search.
- Expected logarithmic insertion.
- Expected logarithmic deletion.
- Expected logarithmic location of a range's beginning.
- Linear cost in the number of elements returned by that range query.
- Linear expected space.

The theoretical maximum cost of one of the key-based operations is still linear.

So your design decision should consider both the typical workload and the consequences of unusually poor structures.

If you need deterministic upper bounds, a balanced tree may be the better choice.

If you need fast ordered updates and straightforward algorithms, a skip list can be an excellent choice.

The right data structure is determined by the requirements and trade-offs, not simply by which one has the most attractive average complexity.

## Chapter 6 — Interview questions

### Q1. What is the expected time complexity of skip-list search?

Expected \\(O(\log n)\\) under the standard randomized-level model, assuming an appropriate maximum-height policy.

### Q2. What is the worst-case search complexity?

\\(O(n)\\), because a valid skip list can contain too few useful shortcuts, resulting in a traversal similar to a normal linked list.

### Q3. Why are insertion and deletion expected \\(O(\log n)\\)?

Both operations first search for predecessor positions, which costs expected \\(O(\log n)\\). They then modify pointers only at the levels where the node participates, contributing expected constant additional pointer-update work for fixed promotion probability.

### Q4. What is the expected space complexity?

\\(O(n)\\), because each node has a constant expected height when the promotion probability is fixed.

### Q5. Does a standard skip list support rank queries in expected \\(O(\log n)\\)?

Not necessarily. A basic skip list does not track the number of elements crossed by each pointer, so finding an element by rank may require linear traversal. Augmenting the links with spans allows expected logarithmic rank queries.

### Q6. What is the difference between expected and amortized complexity?

Expected complexity accounts for randomness. Amortized complexity analyzes the total cost over a sequence of operations. The usual skip-list bounds are stated as expected bounds over the random structure, not as the classic amortized guarantees used for dynamic arrays.

### Q7. How does a skip list compare with an AVL tree?

Both typically support logarithmic search, insertion, and deletion. An AVL tree guarantees worst-case \\(O(\log n)\\) time through explicit balancing, while a standard skip list achieves expected \\(O(\log n)\\) time through randomized heights, with an \\(O(n)\\) worst case.

## What you should retain from this chapter

The full complexity picture is:

- Search, insertion and deletion: expected \\(O(\log n)\\), worst case \\(O(n)\\).
- Space: expected \\(O(n)\\), with worst-case pointer storage depending on the maximum allowed node height.
- Range retrieval: expected \\(O(\log n+k)\\) when returning \\(k\\) items.
- Minimum: \\(O(1)\\).
- Maximum: expected \\(O(\log n)\\) without a tail pointer, or \\(O(1)\\) when a tail pointer is maintained.
- Rank queries: require extra information such as spans for expected logarithmic performance.
- Expected is not amortized: they are different kinds of complexity guarantees.

The most important interview distinction is this: skip lists offer probabilistic balancing, while balanced search trees offer deterministic balancing.

Both can provide fast ordered operations, but they make different guarantees.

Next: Chapter 7 — Skip lists vs. linked lists, arrays, hash tables, AVL trees, red-black trees, and B-trees. We'll compare their real trade-offs, memory layout, range queries, cache behavior, and when each one is the better engineering choice.

