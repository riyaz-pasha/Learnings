# Chapter 8 — Redis Internals: How Sorted Sets Use Skip Lists

This is the chapter where skip lists become a real production-engineering concept rather than just a data-structures interview topic.

We will explore how Redis uses skip lists to implement sorted sets, how it handles score updates, how it calculates a member's rank efficiently, and why it combines a skip list with a hash table.

We'll also examine a particularly important optimization: not every Redis sorted set uses a skip list. Small sorted sets can use a compact representation to reduce memory overhead.&#x20;

[image](https://www.google.com/s2/favicons?domain=https://redis.io\&sz=32)

Docs

+1



By the end of this chapter, you should understand not only why Redis uses a skip list, but also how the details of that implementation enable useful commands such as `ZADD`, `ZRANGE`, `ZRANK`, and `ZREM`.

## 1. First, understand the problem Redis needs to solve

Imagine you are building a gaming platform with ten million players.

Each player has a score, and your application needs to support these operations:

- Add a player to the leaderboard.
- Update a player's score.
- Look up a player's score.
- Return the top 10 players.
- Return all players whose scores are between 1,000 and 2,000.
- Find a player's rank.
- Remove a player from the leaderboard.

A basic hash table solves only part of this problem.

It can map a player ID to a score efficiently, but it does not naturally maintain an ordered ranking.

For example, suppose the hash table contains:

```
alice  → 120
bob    → 175
charlie → 150
diana  → 200
```

The entries are not stored in score order.

To retrieve the top 10 players, you would need some additional mechanism to determine which scores are highest.

One option is to sort the players every time you need a leaderboard. But repeatedly sorting a large collection is expensive.

Another option is to maintain a sorted structure as scores change.

This is where the skip list becomes useful.

## 2. What is a Redis sorted set?

A Redis sorted set, commonly called a ZSET, is a collection of unique members, each associated with a numerical score.

For example:

```
ZADD leaderboard 120 alice
ZADD leaderboard 175 bob
ZADD leaderboard 150 charlie
ZADD leaderboard 200 diana
```

Conceptually, our data is:

| Member  | Score |
| ------- | ----- |
| alice   | 120   |
| bob     | 175   |
| charlie | 150   |
| diana   | 200   |

Redis maintains these members in ascending score order.

The logical ordered sequence is:

```
alice(120)
    ↓
charlie(150)
    ↓
bob(175)
    ↓
diana(200)
```

We can retrieve that sequence using:

```
ZRANGE leaderboard 0 -1 WITHSCORES
```

The `0` means the first element, and `-1` means the last element.

To retrieve the highest-scoring members first, we can use a reverse range operation, such as:

```
ZRANGE leaderboard 0 -1 REV WITHSCORES
```

The essential benefit is that Redis maintains the ordering as members are added or changed. It does not have to sort the entire collection from scratch for every leaderboard query.&#x20;

[image](https://www.google.com/s2/favicons?domain=https://redis.io\&sz=32)

Docs

+1



But how does Redis maintain both fast member lookup and sorted order?

That leads to the central design decision.

## 3. Why Redis combines a hash table with a skip list

A sorted set needs two different capabilities:

Capability A: Find a member quickly.

Given `bob`, retrieve his current score or locate his entry.

Capability B: Navigate the collection in sorted order.

Given a score threshold, find where the relevant range begins and efficiently traverse the results.

A hash table is excellent for the first capability.

A skip list is excellent for the second.

Redis's skip-list-backed sorted-set representation combines both structures.&#x20;

[image](https://www.google.com/s2/favicons?domain=https://redis.io\&sz=32)

Docs

+1



Conceptually, the internal design looks like this:

\#chatgpt-mermaid-\_r_1dh\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_1dh\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_1dh\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_1dh\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_1dh\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_1dh\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_1dh\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_1dh\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_1dh\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_1dh\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_1dh\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_1dh\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_1dh\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_1dh\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_1dh\_ p{margin:0;}#chatgpt-mermaid-\_r_1dh\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_1dh\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_1dh\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_1dh\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_1dh\_ .label text,#chatgpt-mermaid-\_r_1dh\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_1dh\_ .node rect,#chatgpt-mermaid-\_r_1dh\_ .node circle,#chatgpt-mermaid-\_r_1dh\_ .node ellipse,#chatgpt-mermaid-\_r_1dh\_ .node polygon,#chatgpt-mermaid-\_r_1dh\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_1dh\_ .rough-node .label text,#chatgpt-mermaid-\_r_1dh\_ .node .label text,#chatgpt-mermaid-\_r_1dh\_ .image-shape .label,#chatgpt-mermaid-\_r_1dh\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_1dh\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_1dh\_ .rough-node .label,#chatgpt-mermaid-\_r_1dh\_ .node .label,#chatgpt-mermaid-\_r_1dh\_ .image-shape .label,#chatgpt-mermaid-\_r_1dh\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_1dh\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_1dh\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_1dh\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_1dh\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_1dh\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_1dh\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_1dh\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_1dh\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_1dh\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_1dh\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_1dh\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_1dh\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_1dh\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_1dh\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_1dh\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_1dh\_ .icon-shape,#chatgpt-mermaid-\_r_1dh\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_1dh\_ .icon-shape p,#chatgpt-mermaid-\_r_1dh\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_1dh\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_1dh\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_1dh\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_1dh\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_1dh\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_1dh\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_1dh\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_1dh\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_1dh\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_1dh\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_1dh\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_1dh\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_1dh\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_1dh\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_1dh\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_1dh\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_1dh\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_1dh\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_1dh\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_1dh\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_1dh\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_1dh\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_1dh\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_1dh\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_1dh\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_1dh\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_1dh\_ .node rect,#chatgpt-mermaid-\_r_1dh\_ .node circle,#chatgpt-mermaid-\_r_1dh\_ .node ellipse,#chatgpt-mermaid-\_r_1dh\_ .node polygon,#chatgpt-mermaid-\_r_1dh\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_1dh\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_1dh\_ .node.mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_1dh\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_1dh\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_1dh\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}Redis ZSETHash tableSkip listFast member lookupOrdered members by scoreRange navigationRank and position

The two structures represent the same logical collection but organize it differently.

### The hash table

Conceptually, the hash table provides a lookup path such as:

```
alice → score 120
bob   → score 175
charlie → score 150
diana → score 200
```

The exact internal representation of dictionary keys, values, and node references can differ by Redis version. The important design principle is that the dictionary supports efficient member-based lookup.

Expected hash-table lookup is \\(O(1)\\), subject to ordinary hashing assumptions.

### The skip list

The skip list maintains entries in score order.

```
Level 3: HEAD ─────────→ bob(175) ─────────→ diana(200)

Level 2: HEAD ──→ charlie(150) ────────────→ diana(200)

Level 1: HEAD → alice(120) → charlie(150) → bob(175) → diana(200)
```

This is an illustrative level arrangement, not Redis's actual height assignment for these particular members.

The skip list provides expected \\(O(\log n)\\) positioning for ordered operations under its random-level model.

### Why not use only the hash table?

Because a hash table does not maintain score order.

### Why not use only the skip list?

Because finding an arbitrary member by name would require searching through the ordered structure unless another lookup mechanism were available.

Using both structures gives Redis a useful combination: fast member lookup and efficient sorted navigation.

Interview takeaway: Redis uses different structures for different access patterns. The hash table serves member-oriented lookup, while the skip list serves score-oriented ordering and navigation.

## 4. How does Redis order members with the same score?

This is an important detail that many developers overlook.

Suppose two players have the same score:

```
ZADD leaderboard 175 bob
ZADD leaderboard 175 zoe
```

Which player comes first?

Redis orders members first by score. When scores are equal, it uses lexicographical ordering of the member strings.&#x20;

[image](https://www.google.com/s2/favicons?domain=https://redis.io\&sz=32)

Docs

+1



Our sorted set now looks like this:

| Ascending rank | Member  | Score |
| -------------- | ------- | ----- |
| 0              | alice   | 120   |
| 1              | charlie | 150   |
| 2              | bob     | 175   |
| 3              | zoe     | 175   |
| 4              | diana   | 200   |

The ordering key is effectively the pair:

\\[ \boxed{(\text{score},\text{member})} \\]

The score is the primary comparison. The member string breaks ties.

Why is this useful?

Because a skip list needs a consistent ordering rule to decide whether one node comes before another.

If two nodes have equal scores, comparing scores alone cannot determine which one should be next.

The member comparison provides the tie-breaker.

### Important consequences

First, members are unique within a sorted set, but scores do not have to be unique.

Second, inserting a member with the same score as another member does not replace that member. They remain separate entries.

Third, if you retrieve equal-scoring members, their order is deterministic according to the member comparison, not according to insertion time.

For example:

```
ZADD leaderboard 175 zoe
ZADD leaderboard 175 bob
```

Even though `zoe` was inserted first, `bob` sorts before `zoe`.

This makes pagination and repeated ordered queries predictable, provided the collection itself has not changed.

## 5. What does a Redis skip-list node contain?

Let's revisit the basic node structure from Chapter 2.

A textbook skip-list node might contain:

```
Node
 ├── member
 ├── score
 ├── backward
 └── levels[]
       ├── forward pointer
       └── span
```

This is a conceptual representation of the skip-list-backed sorted set.

Redis's current source implements skip-list nodes with a variable number of levels, forward links, spans, and a backward link. The node also carries its score and member data. Some allocation and metadata details are implementation optimizations that can change between Redis versions.&#x20;

[image](https://www.google.com/s2/favicons?domain=https://github.com\&sz=32)

GitHub

+1



Let's examine the important fields.

### `member`

The member identifies the item in the sorted set.

Examples:

```
alice
bob
user-1001
order-456
```

### `score`

The score determines the primary ordering.

For a leaderboard, this is the player's score.

For a scheduling system, it might represent a timestamp.

For a priority-like collection, it might represent a numerical priority.

Redis sorted-set scores are double-precision floating-point numbers.&#x20;

[image](https://www.google.com/s2/favicons?domain=https://redis.io\&sz=32)

Docs



### `forward[]`

These are the multi-level forward pointers we studied earlier.

They let Redis search for a score/member position without traversing every member.

### `backward`

Redis's skip-list-backed representation maintains a backward pointer for reverse traversal.

This makes it easy to move through the bottom-level list in the opposite direction after reaching an appropriate starting point.

### `span`

A span records how many rank positions a forward link crosses.

This field is particularly interesting because it lets Redis do something more powerful than ordinary skip-list search: find a member's rank without traversing the entire collection.

Let's understand that next.

## 6. Redis spans: the trick behind efficient rank queries

Imagine a sorted set containing one million members.

You want the rank of `bob`.

A naive implementation could find `bob` and count every preceding member.

Depending on where Bob appears, that could require examining a large fraction of the collection.

Redis can do better by maintaining span information on the skip-list links. These spans allow it to calculate rank while traversing the levels. The implementation updates spans when nodes are inserted or removed.&#x20;

[image](https://www.google.com/s2/favicons?domain=https://github.com\&sz=32)

GitHub

+1



### First, understand a span with a small example

Let's use this sorted collection:

```
Rank 0: alice    score 120
Rank 1: charlie  score 150
Rank 2: bob      score 175
Rank 3: zoe      score 175
Rank 4: diana    score 200
```

For clarity, the following diagram uses one-based rank positions internally, while Redis's public `ZRANK` command returns zero-based ranks.

```
Level 3: HEAD ─────(3)────→ bob ─────(2)────→ diana

Level 2: HEAD ─(2)→ charlie ─(1)→ bob ─(1)→ zoe ─(1)→ diana

Level 1: HEAD → alice → charlie → bob → zoe → diana
```

The numbers in parentheses represent illustrative span values.

For example:

- The level-3 pointer from `HEAD` to `bob` crosses three member positions.
- The level-3 pointer from `bob` to `diana` crosses two member positions.
- The bottom-level links each move forward by one member position.

The top-level pointer can therefore move several rank positions in a single hop.

This is exactly what a skip list already does for searching. Spans add information about how far those links move in the ordered sequence.

### How does Redis calculate rank?

Suppose we want Bob's rank.

Start at `HEAD` with a running rank of zero.

At level 3:

- Follow the pointer from `HEAD` to `bob`.
- Add the span of 3.
- We have reached Bob's internal one-based rank of 3.

In Redis's internal implementation, rank accumulation is performed as the traversal compares score/member pairs and moves forward. Public `ZRANK` then returns a zero-based index.&#x20;

[image](https://www.google.com/s2/favicons?domain=https://github.com\&sz=32)

GitHub

+1



The general idea is:

```
runningRank = 0

At each level, while the next node does not pass the target:
    runningRank += span of current forward pointer
    move to the next node

Return the target's rank
```

This is conceptual pseudocode, not a replacement for Redis's actual implementation.

The crucial point is that we count positions using spans instead of counting one member per step.

Because rank lookup follows the skip-list search path, it takes expected \\(O(\log n)\\) time.

### Why can't an ordinary skip list do this automatically?

An ordinary skip list stores forward pointers but does not necessarily store the number of elements crossed by each pointer.

Without spans, finding an element by its position may require linear traversal.

Spans augment the structure with the extra information required to answer rank queries efficiently.

This is a general engineering principle: additional metadata can turn an expensive query into an efficient one, but updates must maintain that metadata correctly.

## 7. How Redis handles insertion and span updates

Suppose we execute:

```
ZADD leaderboard 160 eve
```

Eve's score places her between Charlie and Bob:

```
alice(120)
    ↓
charlie(150)
    ↓
eve(160)
    ↓
bob(175)
    ↓
zoe(175)
    ↓
diana(200)
```

Redis must maintain more than the bottom-level ordering.

It needs to:

1. Locate Eve's correct score/member position.
2. Generate a height for the new skip-list node.
3. Link Eve into every level she occupies.
4. Update the affected forward pointers.
5. Update the relevant spans so rank calculations remain correct.
6. Maintain backward links and other skip-list metadata.

The insertion logic calculates the rank already traversed at each level. These values help determine the spans for the new node and its predecessors. Higher levels the new node does not occupy may still need span adjustments because they now cross one additional member.&#x20;

[image](https://www.google.com/s2/favicons?domain=https://github.com\&sz=32)

GitHub



That last point is important.

An ordinary skip-list insertion updates the pointers at levels the new node participates in.

A span-augmented skip list has additional responsibilities: links that remain structurally unchanged may still need updated span values.

### What if Redis didn't update spans?

The collection could remain correctly sorted, and some key searches could still work.

However, rank queries could return incorrect results because the stored distances would no longer reflect the positions of members in the collection.

This is why span updates are part of the correctness of the augmented data structure, not merely an optimization.

## 8. How Redis handles deletion

Consider removing Charlie:

```
ZREM leaderboard charlie
```

Redis must remove Charlie from the member lookup structure and unlink the corresponding entry from the skip list.

When the node is unlinked:

- Its predecessors bypass it.
- The affected spans are adjusted.
- The next node's backward pointer is corrected.
- The tail is changed if the removed node was last.
- The active level is reduced if its highest levels become empty.

The implementation handles span changes for links that directly point to the deleted node and for higher-level links that cross the position formerly occupied by that node.&#x20;

[image](https://www.google.com/s2/favicons?domain=https://github.com\&sz=32)

GitHub



Again, maintaining sorted order is only part of the job. Redis must also preserve every other property required for efficient operations, including the ability to calculate rank.

## 9. Backward pointers and reverse traversal

We have focused mostly on moving forward through skip-list levels.

Redis also maintains backward navigation in its skip-list-backed sorted sets.

Conceptually:

```
HEAD → alice ⇄ charlie ⇄ bob ⇄ zoe ⇄ diana
                                              ↑
                                             TAIL
```

This diagram shows the bottom-level links. In a doubly linked bottom level, each node can move to its successor or predecessor.

The backward pointer allows Redis to navigate toward lower scores without creating a second reverse skip list. A tail pointer provides access to the last element.&#x20;

[image](https://www.google.com/s2/favicons?domain=https://github.com\&sz=32)

GitHub

+1



### Why is this useful?

It makes reverse ordered operations convenient.

For example:

```
ZRANGE leaderboard 0 -1 REV WITHSCORES
```

returns the collection in descending order.

This is useful for top-player leaderboards, newest-by-timestamp queries when timestamps are scores, and other workloads where the highest-scoring entries should appear first.

Maintaining reverse traversal is also valuable when a score range needs to be scanned from its upper end.

## 10. Walk through the most important Redis commands

Let's connect the internal structure to the commands you will use in applications.

For the following examples, assume the `leaderboard` sorted set contains:

| Member  | Score |
| ------- | ----- |
| alice   | 120   |
| charlie | 150   |
| bob     | 175   |
| zoe     | 175   |
| diana   | 200   |

### A. `ZADD` — Insert or update a member

```
ZADD leaderboard 180 bob
```

Bob already exists with a score of 175.

Redis updates his score to 180 and places him in the appropriate new position.

The member remains unique; Redis does not create a second Bob entry. The command's basic time complexity is \\(O(\log N)\\) per item added or updated in the sorted-set representation's documented complexity model.&#x20;

[image](https://www.google.com/s2/favicons?domain=https://redis.io\&sz=32)

Docs



When a score changes, the internal implementation can update the node in place if its new ordering position remains valid. Otherwise, it unlinks and reinserts the node at the correct position.&#x20;

[image](https://www.google.com/s2/favicons?domain=https://github.com\&sz=32)

GitHub



### B. `ZSCORE` — Retrieve a member's score

```
ZSCORE leaderboard bob
```

This returns Bob's score.

The dictionary-backed member lookup is the natural path for this operation, rather than scanning the skip list from the beginning.

### C. `ZRANGE` — Retrieve an ordered range

```
ZRANGE leaderboard 0 2 WITHSCORES
```

This returns the first three members in ascending score order.

With the index-based range form, Redis uses rank-navigation logic to locate the beginning of the requested range, then traverses the ordered entries to produce the results.

The documented complexity is:

\\[ \boxed{O(\log N+M)} \\]

where \\(M\\) is the number of elements returned.&#x20;

[image](https://www.google.com/s2/favicons?domain=https://redis.io\&sz=32)

Docs



The logarithmic part locates the starting position; the \\(M\\) part accounts for emitting the requested members.

### D. `ZRANGEBYSCORE` — Query a score range

For example:

```
ZRANGE leaderboard 150 180 BYSCORE WITHSCORES
```

This returns the members whose scores fall within the specified score range, using the applicable inclusive/exclusive boundaries.

Redis can search for the range's starting position and then walk through the matching members.

Its documented complexity is \\(O(\log N+M)\\), where \\(M\\) is the number of returned members.&#x20;

[image](https://www.google.com/s2/favicons?domain=https://redis.io\&sz=32)

Docs

+1



This is particularly useful for sliding windows, time-based indexing, and score thresholds.

### E. `ZRANK` — Find a member's ascending rank

```
ZRANK leaderboard bob
```

The answer is:

```
2
```

Why? The ascending zero-based ranks are:

```
alice    → 0
charlie  → 1
bob      → 2
zoe      → 3
diana    → 4
```

Redis uses span information to calculate the rank efficiently. The documented complexity is \\(O(\log N)\\).&#x20;

[image](https://www.google.com/s2/favicons?domain=https://redis.io\&sz=32)

Docs



### F. `ZREVRANK` — Find descending rank

```
ZREVRANK leaderboard bob
```

The answer is:

```
2
```

Because there are five entries, Diana has descending rank 0, Zoe rank 1, Bob rank 2, Charlie rank 3, and Alice rank 4.

In this example Bob is in the middle, so his rank happens to be 2 in both directions.

### G. `ZREM` — Remove a member

```
ZREM leaderboard charlie
```

Redis removes Charlie from the sorted set.

For each specified member, the documented complexity is \\(O(\log N)\\).&#x20;

[image](https://www.google.com/s2/favicons?domain=https://redis.io\&sz=32)

Docs



The skip list links and spans are updated, and the member is removed from the dictionary-backed lookup structure.

## 11. Redis does not always use a skip list

This is one of the most important practical details in this chapter.

A skip list is useful for large, frequently updated ordered collections, but maintaining a hash table, node objects, and multiple forward pointers has a memory cost.

For a sorted set containing only three or four short members, that overhead can be disproportionate.

Redis therefore supports a compact sorted-set representation using a listpack.

A listpack stores the entries compactly in a contiguous sequence rather than using a full skip list plus hash table.

Redis can use that representation when the collection is small enough and the member-size constraints are satisfied. When the configured limits are exceeded, it can convert the sorted set to the skip-list-backed representation.&#x20;

[image](https://www.google.com/s2/favicons?domain=https://redis.io\&sz=32)

Docs

+1



Conceptually:

\#chatgpt-mermaid-\_r_1lf\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_1lf\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_1lf\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_1lf\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_1lf\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_1lf\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_1lf\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_1lf\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_1lf\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_1lf\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_1lf\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_1lf\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_1lf\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_1lf\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_1lf\_ p{margin:0;}#chatgpt-mermaid-\_r_1lf\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_1lf\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_1lf\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_1lf\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_1lf\_ .label text,#chatgpt-mermaid-\_r_1lf\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_1lf\_ .node rect,#chatgpt-mermaid-\_r_1lf\_ .node circle,#chatgpt-mermaid-\_r_1lf\_ .node ellipse,#chatgpt-mermaid-\_r_1lf\_ .node polygon,#chatgpt-mermaid-\_r_1lf\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_1lf\_ .rough-node .label text,#chatgpt-mermaid-\_r_1lf\_ .node .label text,#chatgpt-mermaid-\_r_1lf\_ .image-shape .label,#chatgpt-mermaid-\_r_1lf\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_1lf\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_1lf\_ .rough-node .label,#chatgpt-mermaid-\_r_1lf\_ .node .label,#chatgpt-mermaid-\_r_1lf\_ .image-shape .label,#chatgpt-mermaid-\_r_1lf\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_1lf\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_1lf\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_1lf\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_1lf\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_1lf\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_1lf\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_1lf\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_1lf\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_1lf\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_1lf\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_1lf\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_1lf\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_1lf\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_1lf\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_1lf\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_1lf\_ .icon-shape,#chatgpt-mermaid-\_r_1lf\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_1lf\_ .icon-shape p,#chatgpt-mermaid-\_r_1lf\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_1lf\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_1lf\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_1lf\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_1lf\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_1lf\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_1lf\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_1lf\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_1lf\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_1lf\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_1lf\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_1lf\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_1lf\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_1lf\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_1lf\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_1lf\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_1lf\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_1lf\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_1lf\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_1lf\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_1lf\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_1lf\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_1lf\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_1lf\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_1lf\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_1lf\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_1lf\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_1lf\_ .node rect,#chatgpt-mermaid-\_r_1lf\_ .node circle,#chatgpt-mermaid-\_r_1lf\_ .node ellipse,#chatgpt-mermaid-\_r_1lf\_ .node polygon,#chatgpt-mermaid-\_r_1lf\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_1lf\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_1lf\_ .node.mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_1lf\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_1lf\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_1lf\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}Create a Redis sorted setFits compact encodinglimits?Listpack representationSkip list + hash tableEntries or member sizesexceed limits?Ordered operations usingskip listMember lookup usingdictionaryYesNoNoYes

### What are the default compact-encoding thresholds?

The Redis memory-optimization documentation lists the following default configuration values:

| Setting                     | Documented default |
| --------------------------- | ------------------ |
| `zset-max-listpack-entries` | 128                |
| `zset-max-listpack-value`   | 64 bytes           |

These settings control the entry-count and member-size limits for compact sorted-set encoding. Values can be changed in configuration, and the exact behavior should be checked for the Redis version you deploy.&#x20;

[image](https://www.google.com/s2/favicons?domain=https://redis.io\&sz=32)

Docs



### Why use a listpack for small sets?

Because it can avoid the per-node and multiple-pointer overhead of the full skip-list-backed representation.

A small collection can be stored and scanned compactly, often with better memory locality.

The trade-off is that operations on a compact listpack may require scanning entries, rather than using skip-list navigation. Since the compact representation is deliberately limited in size, that work can be reasonable.

This demonstrates a useful data-structure principle:

The best representation for a small collection may differ from the best representation for a large collection.

Redis chooses its internal representation according to the collection's size and configured limits rather than using skip lists indiscriminately.

## 12. How do score updates work internally?

Let's trace a realistic leaderboard update.

Initially:

```
alice    120
charlie  150
bob      175
zoe      175
diana    200
```

Now Bob's score increases:

```
ZADD leaderboard 220 bob
```

The resulting order is:

```
alice    120
charlie  150
zoe      175
diana    200
bob      220
```

The member remains `bob`, but its location changes because its score is now greater than all the other scores in this example.

Conceptually, Redis must:

1. Locate Bob's current entry.
2. Determine the new score and the new ordering position.
3. Update the skip-list structure if the new position differs.
4. Update any relevant spans and links.
5. Preserve the member lookup structure.

There are no duplicate Bob entries.

The operation modifies the existing logical member rather than adding a second copy.

In the current skip-list source, the score-update logic explicitly distinguishes between changes that preserve the node's ordering position and changes that require unlinking and reinserting it.&#x20;

[image](https://www.google.com/s2/favicons?domain=https://github.com\&sz=32)

GitHub



This is exactly the kind of operation skip lists are designed to handle efficiently: the sorted order changes without requiring the entire collection to be sorted again.

## 13. What is the complexity of Redis sorted-set operations?

Here is a useful interview reference table.

| Redis command        | Purpose                         | Documented complexity               |
| -------------------- | ------------------------------- | ----------------------------------- |
| `ZADD`               | Add or update members           | \\(O(\log N)\\) per member          |
| `ZSCORE`             | Find a member's score           | Typically \\(O(1)\\) average lookup |
| `ZRANGE`             | Retrieve an index or rank range | \\(O(\log N+M)\\)                   |
| `ZRANGE ... BYSCORE` | Retrieve a score range          | \\(O(\log N+M)\\)                   |
| `ZRANK`              | Find ascending rank             | \\(O(\log N)\\)                     |
| `ZREVRANK`           | Find descending rank            | \\(O(\log N)\\)                     |
| `ZREM`               | Remove members                  | \\(O(\log N)\\) per member          |

Here, \\(N\\) is the number of members in the sorted set, and \\(M\\) is the number of results returned for a range query.

The official Redis command documentation gives these complexities for the corresponding sorted-set commands.&#x20;

[image](https://www.google.com/s2/favicons?domain=https://redis.io\&sz=32)

Docs

+3



One subtlety: these are the documented command-complexity models, not a claim that every operation executes the exact same internal algorithm for every possible encoding. Small listpack-backed sorted sets use their compact representation, while larger skip-list-backed sets can take advantage of the multi-level navigation and spans.

## 14. What should you learn from Redis's design?

Let's summarize the engineering decisions.

### Decision 1: Keep a hash table and a skip list

Reason: the hash table is effective for member lookup, while the skip list provides ordered navigation.

### Decision 2: Maintain the ordering during updates

Reason: repeated sorting during reads would be expensive for frequently queried leaderboards and ranges.

### Decision 3: Store spans

Reason: spans allow rank queries to skip over many members while keeping track of positions.

### Decision 4: Maintain backward traversal

Reason: reverse iteration and access to high-score entries become convenient.

### Decision 5: Use compact encoding for small sorted sets

Reason: avoiding the overhead of a full skip list and dictionary can save memory when collections are small.

These are general lessons for data-structure design: choose structures according to access patterns, augment them with metadata for important operations, and consider representation overhead at different scales.

## 15. Real-world use cases

Redis sorted sets are useful anywhere you need an ordered collection of unique members with scores.

Leaderboards. Store player IDs as members and scores as numerical values. Retrieve top-ranked players and individual ranks.

Time-based indexing. Store event identifiers as members and timestamps as scores, then retrieve events within a timestamp range.

Priority-like scheduling. Store task IDs as members and numerical priorities or ordering values as scores.

Sliding-window queries. Store identifiers with timestamp-based scores and query or remove entries outside a desired window. Depending on the requirements, sorted sets may be combined with expiration or another data structure.

Ranking and analytics. Retrieve members within a score interval or determine their relative positions efficiently.

The same underlying ordered structure can support all of these workloads, as long as the score and member semantics fit the application.

## 16. Interview questions and answers

### Q1. Why does Redis use both a hash table and a skip list for sorted sets?

The hash table supports efficient member-based lookup, while the skip list maintains score order and supports efficient navigation, range queries, and rank calculations. The two structures provide complementary access paths to the same logical collection.

### Q2. Why can't Redis simply use a hash table?

A hash table does not inherently maintain sorted order. Operations such as retrieving the top players, selecting a score range, or finding a rank would need additional work or another data structure.

### Q3. What is a span in Redis's skip list?

A span records the number of member positions crossed by a forward pointer. Redis uses spans to calculate ranks and navigate by position without traversing every element.

### Q4. How does Redis calculate `ZRANK` efficiently?

It traverses the skip list using score/member comparisons while accumulating span values. This allows it to determine the member's position in expected logarithmic time. The public rank is zero-based.&#x20;

[image](https://www.google.com/s2/favicons?domain=https://redis.io\&sz=32)

Docs

+1



### Q5. What happens when a member's score changes?

Redis updates the member's score and maintains the ordering. If the new score changes the member's position, the corresponding skip-list entry is unlinked and reinserted at the correct location, with relevant pointers and spans maintained.&#x20;

[image](https://www.google.com/s2/favicons?domain=https://github.com\&sz=32)

GitHub

+1



### Q6. Why doesn't Redis use a skip list for every sorted set?

A skip list and dictionary have additional memory overhead. Redis can use a compact listpack representation for small sets and transition to a skip-list-backed representation when its configured limits are exceeded.&#x20;

[image](https://www.google.com/s2/favicons?domain=https://redis.io\&sz=32)

Docs

+1



### Q7. What is the complexity of retrieving a score range?

The documented complexity of a score-range query is \\(O(\log N+M)\\), where \\(N\\) is the number of members and \\(M\\) is the number returned. The logarithmic component finds the start of the range; the remaining work emits the results.&#x20;

[image](https://www.google.com/s2/favicons?domain=https://redis.io\&sz=32)

Docs



### Q8. Why are backward pointers useful?

They make reverse traversal convenient. Redis can move through the ordered bottom-level list in the opposite direction, supporting operations that return higher-scoring members first.

## Chapter 8 — Final mental model

Redis's sorted sets demonstrate why skip lists are more than a theoretical data structure.

A sorted set needs efficient lookup by member, ordered traversal, range queries, and ranking. A hash table and skip list complement each other: one supports lookup, while the other maintains sorted order. Span metadata makes rank queries efficient, and backward pointers support reverse traversal.

At the same time, Redis uses a compact representation when appropriate rather than paying the full skip-list overhead for every small collection.

The key insight is:

Redis does not use a skip list merely because it is fast at search. It uses an ordered structure because the application needs many different operations over a dynamically changing sorted collection. The accompanying metadata makes those operations efficient.

## Next: Chapter 9 — Implement a Skip List in Java

We'll now put the theory into practice by implementing a complete skip list, including randomized level generation, insertion, search, deletion, range traversal, edge cases, and tests.

We'll also revisit the predecessor-update array and demonstrate how to verify that every forward pointer and level invariant remains correct after a sequence of updates.

Type `next` to continue.

