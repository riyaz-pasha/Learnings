# Chapter 5 — Randomization and the Mathematics Behind \\(O(\log n)\\)

This is one of the most important chapters in the skip-list course.

So far, we have seen that higher levels provide shortcuts and that search, insertion, and deletion use those shortcuts. But we have not yet proved why a randomly constructed skip list usually has enough shortcuts to make those operations efficient.

We will answer five fundamental questions:

1. How does random promotion create the different levels?
2. What is the probability that a node reaches a particular height?
3. Why does the number of levels grow logarithmically?
4. Why does search take expected \\(O(\log n)\\) time?
5. How do promotion probability and maximum height affect memory and performance?

## 1. First, understand the random promotion process

Suppose we insert a new node into a skip list.

Every node starts at height 1. We then repeatedly decide whether to promote it to the next level.

Let \\(p\\) be the probability of promotion.

For our initial analysis, assume:

\\[ p=\frac12 \\]

The process is:

\#chatgpt-mermaid-\_r_vc\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_vc\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_vc\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_vc\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_vc\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_vc\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_vc\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_vc\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_vc\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_vc\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_vc\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_vc\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_vc\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_vc\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_vc\_ p{margin:0;}#chatgpt-mermaid-\_r_vc\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_vc\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_vc\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_vc\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_vc\_ .label text,#chatgpt-mermaid-\_r_vc\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_vc\_ .node rect,#chatgpt-mermaid-\_r_vc\_ .node circle,#chatgpt-mermaid-\_r_vc\_ .node ellipse,#chatgpt-mermaid-\_r_vc\_ .node polygon,#chatgpt-mermaid-\_r_vc\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_vc\_ .rough-node .label text,#chatgpt-mermaid-\_r_vc\_ .node .label text,#chatgpt-mermaid-\_r_vc\_ .image-shape .label,#chatgpt-mermaid-\_r_vc\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_vc\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_vc\_ .rough-node .label,#chatgpt-mermaid-\_r_vc\_ .node .label,#chatgpt-mermaid-\_r_vc\_ .image-shape .label,#chatgpt-mermaid-\_r_vc\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_vc\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_vc\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_vc\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_vc\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_vc\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_vc\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_vc\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_vc\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_vc\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_vc\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_vc\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_vc\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_vc\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_vc\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_vc\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_vc\_ .icon-shape,#chatgpt-mermaid-\_r_vc\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_vc\_ .icon-shape p,#chatgpt-mermaid-\_r_vc\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_vc\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_vc\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_vc\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_vc\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_vc\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_vc\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_vc\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_vc\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_vc\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_vc\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_vc\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_vc\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_vc\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_vc\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_vc\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_vc\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_vc\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_vc\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_vc\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_vc\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_vc\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_vc\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_vc\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_vc\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_vc\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_vc\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_vc\_ .node rect,#chatgpt-mermaid-\_r_vc\_ .node circle,#chatgpt-mermaid-\_r_vc\_ .node ellipse,#chatgpt-mermaid-\_r_vc\_ .node polygon,#chatgpt-mermaid-\_r_vc\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_vc\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_vc\_ .node.mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_vc\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_vc\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_vc\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}Create node at height 1Promote with probability p?Increase height by 1Reached maximum height?StopYesNoYesNo

For example, a particular node might experience these random outcomes:

```
Promotion decisions:  Yes → Yes → No
Resulting height:       3
```

Another node might experience:

```
Promotion decisions:  No
Resulting height:       1
```

A third might experience:

```
Promotion decisions:  Yes → No
Resulting height:       2
```

The important detail is that we stop promoting a node as soon as one promotion attempt fails.

Once its height has been chosen, that height stays fixed during the node's lifetime.

Assume these random choices are independent across nodes. Under this model, we can derive the probabilities that give skip lists their useful structure.

## 2. What is the probability of each node height?

Consider a node with maximum height not yet reached.

For a node to have height 1, its first promotion attempt must fail.

Therefore:

\\[ P(H=1)=1-p \\]

For a node to have height 2, the first attempt must succeed and the second must fail.

\\[ P(H=2)=p(1-p) \\]

For a node to have height 3, the first two attempts must succeed and the third must fail.

\\[ P(H=3)=p^2(1-p) \\]

Generally, for height \\(h\\), assuming the maximum-height cap has not been reached:

\\[ \boxed{P(H=h)=p^{h-1}(1-p)} \\]

Here, \\(H\\) represents the node's height.

### Example with \\(p=0.5\\)

| Height | Required outcome               | Probability |
| ------ | ------------------------------ | ----------- |
| 1      | Fail immediately               | 50%         |
| 2      | Succeed, then fail             | 25%         |
| 3      | Succeed twice, then fail       | 12.5%       |
| 4      | Succeed three times, then fail | 6.25%       |
| 5      | Succeed four times, then fail  | 3.125%      |

Notice the pattern: each successive height is half as likely as the previous one.

This is a geometric probability distribution.

### What does this tell us intuitively?

Most nodes are short. Some nodes are moderately tall. Very few nodes are extremely tall.

This is exactly what we want.

If every node were tall, we would spend considerable memory storing pointers at higher levels without gaining much from their sparsity.

If every node were short, we would have almost no shortcuts.

Random promotion gives us a mixture of both.

## 3. A crucial probability: What is the chance a node reaches level \\(k\\)?

There is a useful distinction between these two questions:

- What is the probability that a node has exactly height \\(k\\)?
- What is the probability that a node has height at least \\(k\\)?

For skip lists, the second question is especially important because it tells us how many nodes participate in each level.

To reach height 1, a node needs no successful promotions.

To reach height 2, it needs one successful promotion.

To reach height 3, it needs two consecutive successful promotions.

To reach height \\(k\\), it needs \\(k-1\\) consecutive successful promotions.

Therefore:

\\[ \boxed{P(H\ge k)=p^{k-1}} \\]

For \\(p=0.5\\):

| Level \\(k\\) | Probability a node participates |
| ------------- | ------------------------------- |
| 1             | \\(1\\)                         |
| 2             | \\(1/2\\)                       |
| 3             | \\(1/4\\)                       |
| 4             | \\(1/8\\)                       |
| 5             | \\(1/16\\)                      |
| 6             | \\(1/32\\)                      |

Every level is progressively sparser.

Be careful with the indexing: these formulas use one-based levels, where height 1 means participation only in the bottom level.

In Java, the bottom level is often indexed as `0`, so the same probability at Java index `i` is \\(p^i\\).

## 4. How many nodes should we expect at each level?

Now imagine that the skip list contains \\(n\\) nodes.

Each node independently has probability \\(p^{k-1}\\) of participating in level \\(k\\).

By linearity of expectation, the expected number of nodes at level \\(k\\) is:

\\[ \boxed{\mathbb{E}[N_k]=np^{k-1}} \\]

This is an expected count, not a guarantee that every level has exactly that number of nodes.

Let's use one million nodes with \\(p=0.5\\).

| Level | Expected number of nodes |
| ----- | ------------------------ |
| 1     | 1,000,000                |
| 2     | 500,000                  |
| 3     | 250,000                  |
| 4     | 125,000                  |
| 5     | 62,500                   |
| 10    | About 1,953              |
| 15    | About 61                 |
| 19    | About 4                  |
| 20    | About 2                  |
| 21    | About 1                  |

This table reveals an important property.

The bottom level contains all one million nodes, but the top levels contain only a handful.

A search can use the upper levels to skip large portions of the collection before it descends to the bottom.

### How far do we go before the expected count falls to approximately one?

Set the expected number of nodes at level \\(k\\) equal to one:

\\[ np^{k-1}\approx 1 \\]

Rearranging:

\\[ p^{k-1}\approx \frac1n \\]

Taking logarithms:

\\[ k-1\approx \log\_{1/p}n \\]

Therefore, the height of the skip list is roughly:

\\[ \boxed{O(\log\_{1/p}n)} \\]

When \\(p=0.5\\), this becomes:

\\[ \boxed{O(\log_2 n)} \\]

For one million nodes:

\\[ \log_2(1{,}000{,}000)\approx 19.93 \\]

So we expect roughly 20 levels, with small variations due to randomness.

This is our first major result.

The hierarchy has logarithmic height because the expected number of nodes shrinks geometrically from one level to the next.

## 5. Why does the logarithmic height make searching efficient?

A logarithmic number of levels is necessary, but we must also consider horizontal movement within each level.

Suppose \\(p=0.5\\).

A node on a lower level has a 50% chance of also appearing on the next higher level.

Think about searching backward from the target's position. At a particular level, we generally move backward until we encounter a node that participates in the next higher level. That is the point from which the previous, higher-level part of the search can continue.

Because each node independently has promotion probability \\(p\\), the distance to such a promoted node follows a geometric pattern.

Under the usual model, the expected horizontal search work per level is bounded by a constant proportional to \\(1/p\\).

For intuition, take \\(p=0.5\\). Before encountering a node promoted to the next level, we typically cross only a small number of nodes. The expected work per level does not grow proportionally with the total collection size.

We therefore have two pieces:

- Expected number of levels: \\(O(\log\_{1/p}n)\\).
- Expected horizontal work per level: \\(O(1/p)\\).

Combining them gives the standard expected search bound:

\\[ \boxed{ \mathbb{E}[T(n)] = O\left(\frac{1}{p}\log\_{1/p}n\right) } \\]

For any fixed promotion probability \\(0\<p<1\\), the factor \\(1/p\\) is constant. Therefore:

\\[ \boxed{\mathbb{E}[T(n)]=O(\log n)} \\]

The same reasoning applies to insertion and deletion because they first find the appropriate predecessor positions, then perform a bounded number of pointer updates per node level.

### A clarification about the formula

The expression above describes asymptotic expected work under the standard randomized model, with logarithmic height available as the collection grows. It is not an exact count of comparisons for every implementation or every randomly constructed list.

Real implementations add details such as a maximum level, comparisons, memory allocation, cache behavior, and language-specific pointer costs.

We will explore how the maximum-level cap affects this analysis shortly.

## 6. Derive the expected height of a single node

We know that most nodes are short, and taller nodes are progressively less common.

But how many forward pointers does a node require on average?

Let \\(H\\) be a node's height.

The expected height can be calculated by summing the probabilities that the node reaches each level:

\\[ \mathbb{E}[H] = \sum\_{k=1}^{\infty}P(H\ge k) \\]

Using our earlier result:

\\[ P(H\ge k)=p^{k-1} \\]

Therefore:

\\[ \mathbb{E}[H] = 1+p+p^2+p^3+\cdots \\]

This is an infinite geometric series. For \\(0\<p<1\\):

\\[ 1+p+p^2+\cdots=\frac{1}{1-p} \\]

So:

\\[ \boxed{\mathbb{E}[H]=\frac{1}{1-p}} \\]

This is the expected number of forward pointers stored per node in the uncapped model.

### Example 1: \\(p=0.5\\)

\\[ \mathbb{E}[H]=\frac{1}{1-0.5}=2 \\]

A node has two forward pointers on average.

For one million nodes, that corresponds to approximately two million forward-pointer slots in expectation.

### Example 2: \\(p=0.25\\)

\\[ \mathbb{E}[H]=\frac{1}{1-0.25} =\frac43 \approx 1.33 \\]

The expected number of pointer slots is lower.

### Example 3: \\(p=0.75\\)

\\[ \mathbb{E}[H]=\frac{1}{1-0.75}=4 \\]

A node has four forward pointers on average.

We can summarize the basic memory trade-off:

| Promotion probability \\(p\\) | Expected node height | Expected pointer slots across \\(n\\) nodes |
| ----------------------------- | -------------------- | ------------------------------------------- |
| 0.25                          | 1.33                 | \\(1.33n\\)                                 |
| 0.50                          | 2                    | \\(2n\\)                                    |
| 0.75                          | 4                    | \\(4n\\)                                    |

These calculations exclude the node objects themselves, their key values, array-object overhead, and other implementation-specific memory costs.

The important conclusion is:

The promotion probability controls not only the density of the upper levels but also the average number of forward pointers stored per node.

## 7. How does the promotion probability affect search performance?

Let's compare two skip lists with the same number of elements but different promotion probabilities.

### Skip list A: \\(p=0.5\\)

Each level has approximately half as many nodes as the one below.

```
Level 4:  •────────────•────────────•
Level 3:  •──────•──────•──────•──────•
Level 2:  •───•───•───•───•───•───•───•
Level 1:  • • • • • • • • • • • • • • •
```

This produces relatively dense upper levels and a relatively large average node height.

### Skip list B: \\(p=0.25\\)

Each level has approximately one quarter as many nodes as the one below.

```
Level 4:  •────────────────────────•
Level 3:  •────────────•────────────•
Level 2:  •────•────•────•────•────•
Level 1:  • • • • • • • • • • • • • •
```

This produces sparser upper levels and fewer forward pointers per node on average.

There is a trade-off:

- A higher promotion probability creates more tall nodes and more pointers, but reduces the number of levels and the expected horizontal movement per level.
- A lower promotion probability reduces pointer storage but tends to require more horizontal movement within each level.

We can get a useful intuition from the leading factor in the expected search-cost estimate:

\\[ f(p)=\frac{1}{p\ln(1/p)} \\]

This follows from the identity:

\\[ \log\_{1/p}n=\frac{\ln n}{\ln(1/p)} \\]

Under this simplified model, the leading factor is minimized near:

\\[ p=\frac1e\approx 0.368 \\]

The difference from \\(p=0.5\\) is modest in this idealized calculation.

Do not treat \\(1/e\\) as a universal optimal implementation setting. Actual performance depends on the implementation language, memory allocation, pointer layout, processor caches, workload, and maximum-height policy.

For many practical implementations, simplicity and lower memory overhead can matter more than a small theoretical difference in expected search work.

## 8. Why is the worst case still \\(O(n)\\)?

This is a common interview question.

If a skip list has expected logarithmic search time, why isn't its worst-case search also logarithmic?

Because randomization provides a probability distribution over structures. It does not force every possible structure to be balanced.

Imagine that all \\(n\\) nodes receive height 1.

The resulting structure is just a regular linked list:

```
Level 1: HEAD → 10 → 20 → 30 → 40 → ... → NIL
```

There are no higher-level shortcuts.

A search near the end can require visiting nearly every node:

\\[ T(n)=O(n) \\]

For a finite collection, a particularly poor height assignment is possible even though it may be unlikely.

More generally, an unfortunate arrangement can leave the upper levels insufficiently useful, causing linear search work.

### How unlikely is an all-height-1 list?

In the uncapped model, a single node has probability \\(1-p\\) of having height 1.

Assuming independent height choices, all \\(n\\) nodes have height 1 with probability:

\\[ \boxed{(1-p)^n} \\]

For \\(p=0.5\\) and \\(n=1{,}000\\):

\\[ P(\text{all heights}=1)=\left(\frac12\right)^{1000} \\]

That particular event is extraordinarily unlikely.

However, we should not confuse this calculation with the probability of every possible bad structure. There are many ways to obtain poor search behavior besides the specific case where every node has height 1.

The important distinction is:

- Expected complexity describes the average performance over the randomized structure under the model's assumptions.
- Worst-case complexity describes the maximum work possible for a valid structure.

For an ordinary skip list, the expected search, insertion, and deletion time is \\(O(\log n)\\), while the worst-case time is \\(O(n)\\).

A properly maintained balanced search tree, such as an AVL tree or red-black tree, offers deterministic \\(O(\log n)\\) search, insertion, and deletion under its standard assumptions.

## 9. Can we say the height is logarithmic with high probability?

Yes, under the standard independent random-level model.

Let \\(H\_{\max}\\) represent the maximum height among all \\(n\\) nodes.

The probability that any particular node reaches height \\(k\\) is:

\\[ P(H\ge k)=p^{k-1} \\]

By applying the union bound across all \\(n\\) nodes:

\\[ P(H\_{\max}\ge k) \le n p^{k-1} \\]

Now choose a level approximately:

\\[ k=1+\log\_{1/p}n+c \\]

where \\(c\\) is a positive constant.

Then:

\\[ \begin{aligned} P(H\_{\max}\ge k) &\le np^{k-1}\\\ &\approx np^{\log\_{1/p}n+c}\\\ &=p^c \end{aligned} \\]

Therefore:

\\[ \boxed{P(H\_{\max}\ge k)\lesssim p^c} \\]

As \\(c\\) increases, the upper bound decreases exponentially.

This tells us that the maximum level is logarithmic in the collection size with high probability: by allowing a modest constant amount of extra height, the probability of exceeding that height can be made very small.

This probability argument is an upper bound, not an exact distribution for the maximum height.

### Why does this matter?

It explains why skip lists work well despite lacking deterministic balancing rules.

Their structure is random, but very tall nodes become exponentially less likely as the height increases. As a result, the maximum height is logarithmic with high probability under the model.

## 10. The maximum-height cap: an important implementation detail

In Java, we commonly see something like:

```
private static final int MAX_LEVEL = 16;
```

And our `randomLevel()` implementation refuses to exceed that maximum.

Why do we impose a cap?

To bound the node height and the size of the head's pointer array.

But the cap has a mathematical consequence.

The idealized logarithmic-height analysis assumes that the number of available levels can grow as the collection grows. If we impose a fixed maximum height \\(M\\) forever, then for sufficiently large \\(n\\), the highest level can contain many nodes.

The expected number of nodes at the capped top level is approximately:

\\[ np^{M-1} \\]

This quantity grows linearly with \\(n\\) if \\(M\\) and \\(p\\) remain fixed.

Eventually, searching across that top level can itself require substantial horizontal work.

Therefore:

A fixed maximum height cannot guarantee asymptotic expected \\(O(\log n)\\) search for an arbitrarily unbounded collection.

In practice, implementations select a maximum level suitable for their expected capacity. The maximum may be large enough that exceeding its useful operating range is not a realistic concern for the deployment.

For a theoretical skip list intended to support arbitrarily growing \\(n\\), the available height should scale appropriately with \\(n\\), or the implementation should have a policy for expanding its maximum level.

This is an important qualification to the standard expected-complexity claim.

## 11. Expected total memory usage

Let's bring the mathematics together.

Suppose there are \\(n\\) data nodes and each node has height \\(H\\).

The total number of forward-pointer slots is:

\\[ S=\sum\_{i=1}^{n}H_i \\]

Using linearity of expectation:

\\[ \mathbb{E}[S] = \sum\_{i=1}^{n}\mathbb{E}[H_i] \\]

Assuming the node heights follow the same uncapped promotion distribution:

\\[ \mathbb{E}[H_i]=\frac{1}{1-p} \\]

Therefore:

\\[ \boxed{\mathbb{E}[S]=\frac{n}{1-p}} \\]

Since \\(p\\) is fixed, expected pointer storage is:

\\[ \boxed{O(n)} \\]

This is a useful result: the skip list maintains logarithmic expected search time while using linear expected storage.

With a finite maximum height \\(M\\), the tail of the distribution is capped. The expected node height becomes:

\\[ \mathbb{E}[H] = 1+p+p^2+\cdots+p^{M-1} \\]

Hence:

\\[ \boxed{\mathbb{E}[H]=\frac{1-p^M}{1-p}} \\]

The expected total pointer storage then becomes:

\\[ \boxed{\mathbb{E}[S]=n\frac{1-p^M}{1-p}} \\]

These formulas count pointer slots, not complete object memory.

## 12. Put everything together with one example

Let's build a skip list of 1,000,000 elements with promotion probability \\(p=0.5\\).

The approximate theoretical characteristics are:

| Property                                 | Expected or approximate result |
| ---------------------------------------- | ------------------------------ |
| Number of data nodes                     | 1,000,000                      |
| Nodes at level 1                         | 1,000,000                      |
| Nodes at level 2                         | 500,000                        |
| Nodes at level 3                         | 250,000                        |
| Expected node height                     | 2                              |
| Expected total pointer slots             | 2,000,000                      |
| Approximate maximum height               | 20–21 levels                   |
| Expected search complexity               | \\(O(\log n)\\)                |
| Worst-case search complexity             | \\(O(n)\\)                     |
| Expected insertion complexity            | \\(O(\log n)\\)                |
| Expected deletion complexity             | \\(O(\log n)\\)                |
| Worst-case insertion/deletion complexity | \\(O(n)\\)                     |

The node counts and maximum height are approximations derived from the randomized model. The exact structure varies with the random choices and the maximum-height policy.

There is one especially useful comparison here.

For one million nodes:

\\[ \log_2(1{,}000{,}000)\approx20 \\]

A well-shaped skip list has around 20 levels, rather than one million levels or one million search steps.

The number of levels grows very slowly compared with the size of the collection.

That is the mathematical foundation of the data structure.

## 13. Interview questions and answers

### Q1. How does a skip list achieve expected \\(O(\log n)\\) search time?

Each node is promoted to the next level with probability \\(p\\). Consequently, the expected number of nodes at each higher level decreases geometrically, producing a hierarchy with \\(O(\log n)\\) levels in expectation. Search performs a bounded expected amount of horizontal work per level, leading to expected logarithmic search time when the promotion probability is fixed and the height limit is suitable for \\(n\\).

### Q2. Why do most nodes have height 1?

A node must pass every promotion attempt required to reach a higher level. Since the probability of each promotion is \\(p<1\\), the probability of reaching height \\(h\\) decreases geometrically as \\(p^{h-1}\\). Most nodes stop at height 1.

### Q3. What is the probability that a node participates in level 5 when \\(p=0.5\\)?

A node must pass four promotion attempts:

\\[ P(H\ge5)=0.5^4=\frac1{16}=6.25\\% \\]

### Q4. What is the expected height when \\(p=0.25\\)?

\\[ \mathbb{E}[H]=\frac{1}{1-0.25}=\frac43 \\]

The average node has approximately 1.33 forward-pointer slots in the uncapped model.

### Q5. Why isn't skip-list search guaranteed \\(O(\log n)\\)?

Because randomization does not ensure every node-height assignment is balanced. A valid structure can have too few useful shortcuts and require linear traversal. Expected logarithmic performance does not imply a deterministic logarithmic worst-case guarantee.

### Q6. Can we set the promotion probability to 1 to make every node tall?

No. If \\(p=1\\), every promotion succeeds until the maximum height is reached. The levels are no longer sparse, and each node consumes the maximum number of pointer slots. The usual geometric analysis assumes \\(0\<p<1\\).

### Q7. If the maximum height is fixed at 16, is search always asymptotically \\(O(\log n)\\)?

No. With an indefinitely fixed height cap, the number of nodes at the highest level grows proportionally to \\(np^{15}\\). For arbitrarily large \\(n\\), that level can itself become expensive to traverse. The maximum-height policy must be appropriate for the supported collection size.

## Chapter 5 — What you should master

The complete mathematical story is:

1. Each node begins at height 1 and is randomly promoted with probability \\(p\\).
2. The probability of participating in level \\(k\\) is \\(p^{k-1}\\).
3. With \\(n\\) nodes, the expected population of level \\(k\\) is \\(np^{k-1}\\).
4. Because the population shrinks geometrically, the maximum height grows logarithmically in \\(n\\) under the standard model.
5. Search requires expected constant work per level, giving expected \\(O(\log n)\\) search time for a fixed promotion probability and a suitable height policy.
6. The expected node height is \\(1/(1-p)\\), giving linear expected total pointer storage.
7. An ordinary skip list still has \\(O(n)\\) worst-case search, insertion, and deletion time.

The most important insight is that randomization provides a good balance on average without requiring explicit rotations or other balancing operations.

That balance is probabilistic, not deterministic.

Next: Chapter 6 — Time complexity, space complexity, and worst-case behavior.

We will analyze each operation individually, compare expected and amortized complexity, examine common complexity-analysis mistakes, and determine when a skip list's practical performance is likely to be attractive relative to a balanced search tree.

