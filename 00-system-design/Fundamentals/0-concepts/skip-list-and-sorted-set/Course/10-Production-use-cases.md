# Chapter 10 — Production Use Cases, Concurrency and Interview Mastery

We have covered the skip list from its basic intuition through pointer manipulation, probability, complexity, alternative data structures, Redis internals, and a Java implementation.

Now we will connect all those concepts to production engineering.

The key topics are:

1. Where skip lists are used in real systems.
2. Why concurrent skip lists require a different design.
3. How Java's `ConcurrentSkipListMap` works conceptually.
4. How databases such as RocksDB use skip lists.
5. What the most important production pitfalls are.
6. How to answer skip-list interview questions confidently.
7. How to decide whether a skip list is appropriate for a new system.

## 1. Real-world use case: In-memory ordered maps

Suppose you are implementing an application that maintains tasks ordered by their scheduled execution time.

Each task has:

```
taskId
scheduledTime
taskStatus
```

For example:

| Task | Scheduled time |
| ---- | -------------- |
| A    | 10:00:05       |
| B    | 10:00:10       |
| C    | 10:00:12       |
| D    | 10:00:20       |

The application repeatedly performs these operations:

- Insert a newly scheduled task.
- Find the next scheduled task.
- Cancel a task.
- Retrieve all tasks scheduled before a deadline.
- Reschedule a task when its execution time changes.

A skip list can maintain the collection in timestamp order.

### Why is it useful?

Suppose the application needs all tasks scheduled before `10:00:15`.

It can search for the beginning of the range and then traverse the matching entries.

The expected complexity is:

\\[ O(\log n+k) \\]

where \\(n\\) is the number of tasks and \\(k\\) is the number returned.

This is more efficient than repeatedly scanning an unsorted collection when ordered range queries are frequent.

However, a skip list is not necessarily the best scheduler structure. If tasks only need to be inserted and removed at the earliest deadline, a priority queue may be sufficient. If tasks must also be cancelled by arbitrary ID, a combination of structures may be more appropriate.

The lesson is to choose a skip list when arbitrary ordered updates and range navigation are important—not merely because the data contains timestamps.

## 2. Real-world use case: Database memtables

One of the more interesting applications of skip lists is inside storage engines.

Consider RocksDB.

RocksDB uses an in-memory structure called a memtable to hold recent writes before they are flushed to persistent sorted files.

Its architecture documentation describes a skip list as the default memtable representation. The ordered structure is particularly useful for workloads in which writes are interleaved with range scans. RocksDB also supports alternative representations, including vector and prefix-hash memtables, for workloads with different characteristics.&#x20;

[image](https://www.google.com/s2/favicons?domain=https://github.com\&sz=32)

GitHub

+1



### Understand the write path

A simplified write path looks like this:

\#chatgpt-mermaid-\_r_1ud\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_1ud\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_1ud\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_1ud\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_1ud\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_1ud\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_1ud\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_1ud\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_1ud\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_1ud\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_1ud\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_1ud\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_1ud\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_1ud\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_1ud\_ p{margin:0;}#chatgpt-mermaid-\_r_1ud\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_1ud\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_1ud\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_1ud\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_1ud\_ .label text,#chatgpt-mermaid-\_r_1ud\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_1ud\_ .node rect,#chatgpt-mermaid-\_r_1ud\_ .node circle,#chatgpt-mermaid-\_r_1ud\_ .node ellipse,#chatgpt-mermaid-\_r_1ud\_ .node polygon,#chatgpt-mermaid-\_r_1ud\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_1ud\_ .rough-node .label text,#chatgpt-mermaid-\_r_1ud\_ .node .label text,#chatgpt-mermaid-\_r_1ud\_ .image-shape .label,#chatgpt-mermaid-\_r_1ud\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_1ud\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_1ud\_ .rough-node .label,#chatgpt-mermaid-\_r_1ud\_ .node .label,#chatgpt-mermaid-\_r_1ud\_ .image-shape .label,#chatgpt-mermaid-\_r_1ud\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_1ud\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_1ud\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_1ud\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_1ud\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_1ud\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_1ud\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_1ud\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_1ud\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_1ud\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_1ud\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_1ud\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_1ud\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_1ud\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_1ud\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_1ud\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_1ud\_ .icon-shape,#chatgpt-mermaid-\_r_1ud\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_1ud\_ .icon-shape p,#chatgpt-mermaid-\_r_1ud\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_1ud\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_1ud\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_1ud\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_1ud\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_1ud\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_1ud\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_1ud\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_1ud\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_1ud\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_1ud\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_1ud\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_1ud\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_1ud\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_1ud\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_1ud\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_1ud\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_1ud\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_1ud\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_1ud\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_1ud\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_1ud\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_1ud\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_1ud\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_1ud\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_1ud\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_1ud\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_1ud\_ .node rect,#chatgpt-mermaid-\_r_1ud\_ .node circle,#chatgpt-mermaid-\_r_1ud\_ .node ellipse,#chatgpt-mermaid-\_r_1ud\_ .node polygon,#chatgpt-mermaid-\_r_1ud\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_1ud\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_1ud\_ .node.mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_1ud\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_1ud\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_1ud\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}Application writes akey-value pairWrite-ahead logInsert into in-memorymemtableSkip list maintains key orderServe reads and range scansFlush sorted contents topersistent files

This is a conceptual storage-engine flow; exact write ordering and persistence behavior depend on configuration and implementation details.

### Why does a memtable need ordering?

Suppose the keys are:

```
user:100
user:200
user:300
user:400
user:500
```

The database might receive writes in this order:

```
user:400
user:100
user:500
user:200
user:300
```

A skip list allows the memtable to maintain sorted key order even though the writes arrive in a different order.

A range query such as:

```
Return all keys from user:200 through user:400
```

can find the starting position and iterate through the relevant keys.

Without an ordered structure, the memtable might need to scan all its entries to find keys in the requested range.

### Why not use a sorted array?

A memtable receives ongoing insertions. A sorted array may need to shift many elements to maintain ordering after each insertion.

A skip list can place a new key by searching for its predecessors and updating the relevant links.

This makes it attractive for a dynamic, in-memory ordered structure.

### An important implementation detail

The skip list used by a storage engine need not look exactly like our Java implementation.

For example, RocksDB's `InlineSkipList` uses specialized memory allocation and supports concurrent insertion through an implementation-specific algorithm. The source documents that its nodes are retained until the skip list is destroyed, with node contents other than links remaining immutable after publication.&#x20;

[image](https://www.google.com/s2/favicons?domain=https://github.com\&sz=32)

GitHub



That illustrates a broader lesson: production data structures often adapt the textbook design to meet memory-management, concurrency, and workload requirements.

# 3. The biggest production misconception: A skip list is not automatically thread-safe

Consider our Java implementation from Chapter 9.

Its insertion algorithm performs multiple steps:

1. Search for the predecessor at each level.
2. Create a node.
3. Initialize the node's forward pointers.
4. Update predecessor pointers.
5. Update the active height and size.

What happens if two threads execute these steps simultaneously?

Let's examine a concrete example.

Suppose the bottom-level structure is:

```
10 → 20 → 40
```

Thread A wants to insert `30`.

Thread B wants to insert `35`.

Both threads might locate `20` as their predecessor.

Thread A executes:

```
newNode30.forward[0] = node20.forward[0];
node20.forward[0] = newNode30;
```

Thread B independently executes:

```
newNode35.forward[0] = node20.forward[0];
node20.forward[0] = newNode35;
```

Imagine that both threads read the original successor, `40`, before either publishes its new node.

Thread A updates `20.forward[0]` to `30`.

Thread B then updates `20.forward[0]` to `35`.

The resulting chain might be:

```
10 → 20 → 35 → 40
```

Node `30` has become unreachable from the list.

This is a lost update.

The problem is not specific to skip lists. It is a general consequence of concurrently modifying shared pointer structures without coordination.

The extra levels make the problem more complicated because the structure can become inconsistent if different links are updated at different times.

## What can we do?

There are several approaches.

Approach A: External locking.

Protect modifications with a lock. A simple design could use a single write lock, or a read/write lock if the implementation and locking protocol support concurrent readers safely.

Approach B: Fine-grained locking.

Use locks around specific nodes or regions, carefully coordinating operations that affect overlapping pointer paths.

Approach C: Atomic pointer operations.

Use compare-and-set (CAS) operations to publish changes only if the observed pointer still has the expected value. The thread must retry or reconcile its operation when that assumption fails.

Approach D: A purpose-built concurrent skip list.

Use an algorithm designed to handle concurrent insertion, deletion, and traversal, including publication, logical deletion, pointer updates, and memory reclamation.

These approaches are not interchangeable implementation details. They provide different synchronization and progress guarantees.

## 4. Understand CAS using a skip-list insertion

CAS stands for compare-and-set or compare-and-swap.

Conceptually, it performs this operation atomically:

```
compareAndSet(expectedValue, newValue)
```

It means:

> If the current value still equals the expected value, replace it with the new value. Otherwise, do nothing and report failure.

Imagine that node `20` initially points to `40`.

Thread A wants to insert `30`.

It first prepares:

```
30.forward → 40
```

Then Thread A attempts:

```
CAS(20.forward, expected = 40, new = 30)
```

If `20.forward` still points to `40`, the operation succeeds:

```
20 → 30 → 40
```

Now suppose Thread B has already changed `20.forward` to point to `35`.

Thread A's CAS expects `40`, but the actual pointer is now `35`.

The CAS fails rather than blindly overwriting Thread B's update.

Thread A must then retry its search or otherwise reconcile the new structure.

This solves the lost-update problem at that particular link. It does not, by itself, make an entire multi-level insertion atomic.

A concurrent skip-list algorithm must coordinate all the steps needed to preserve its invariants, not merely replace ordinary pointers with atomic pointers.

# 5. Lock-free vs. lock-based skip lists

Now let's discuss a more advanced distinction.

A concurrent data structure can provide different progress guarantees.

| Guarantee   | Meaning                                                                         |
| ----------- | ------------------------------------------------------------------------------- |
| Thread-safe | Concurrent operations preserve the specified correctness properties             |
| Lock-based  | Operations coordinate using locks                                               |
| Lock-free   | The system as a whole guarantees that some operation continues to make progress |
| Wait-free   | Every individual operation completes in a bounded number of its own steps       |

A data structure can be thread-safe without being lock-free.

Likewise, using CAS does not automatically make an entire data structure lock-free or wait-free.

These properties depend on the complete algorithm.

### Why does this matter for skip lists?

Imagine Thread A begins inserting a node and is then paused by the operating system.

In a naive locking design, other threads may have to wait for a lock held by Thread A.

A carefully designed non-blocking skip list can allow other threads to continue, potentially completing or helping with certain structural changes.

But that behavior must be provided by the algorithm; it does not come for free with the skip-list representation.

For formal background, William Pugh's work on concurrent skip lists describes how skip-list structures can be extended to support concurrent access and updates.&#x20;

[image](https://www.google.com/s2/favicons?domain=https://drum.lib.umd.edu\&sz=32)

DRUM

+1



# 6. How Java's `ConcurrentSkipListMap` works

Java provides a production-ready concurrent sorted map:

ConcurrentSkipListMap.

It implements `ConcurrentNavigableMap` and maintains its keys in sorted order according to their natural ordering or a supplied comparator.

The Java API documents expected average \\(O(\log n)\\) costs for operations such as `get`, `put`, `remove`, and `containsKey`. Updates and access operations safely execute concurrently.&#x20;

[image](https://www.google.com/s2/favicons?domain=https://docs.oracle.com\&sz=32)

Oracle Docs



Let's understand what makes it different from our implementation.

## 6.1 It does not simply reuse our node representation

Our educational implementation stores an array of forward pointers directly inside every data node:

```
class Node {
    int key;
    Node[] forward;
}
```

OpenJDK's implementation uses a different arrangement: base nodes contain the data, while separate index nodes form the higher-level navigation structure. The source explains that this separation helps reduce the complexity and overhead of maintaining the more frequently traversed index lists.&#x20;

[image](https://www.google.com/s2/favicons?domain=https://github.com\&sz=32)

GitHub



Conceptually:

\#chatgpt-mermaid-\_r_22t\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_22t\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_22t\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_22t\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_22t\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_22t\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_22t\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_22t\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_22t\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_22t\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_22t\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_22t\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_22t\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_22t\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_22t\_ p{margin:0;}#chatgpt-mermaid-\_r_22t\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_22t\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_22t\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_22t\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_22t\_ .label text,#chatgpt-mermaid-\_r_22t\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_22t\_ .node rect,#chatgpt-mermaid-\_r_22t\_ .node circle,#chatgpt-mermaid-\_r_22t\_ .node ellipse,#chatgpt-mermaid-\_r_22t\_ .node polygon,#chatgpt-mermaid-\_r_22t\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_22t\_ .rough-node .label text,#chatgpt-mermaid-\_r_22t\_ .node .label text,#chatgpt-mermaid-\_r_22t\_ .image-shape .label,#chatgpt-mermaid-\_r_22t\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_22t\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_22t\_ .rough-node .label,#chatgpt-mermaid-\_r_22t\_ .node .label,#chatgpt-mermaid-\_r_22t\_ .image-shape .label,#chatgpt-mermaid-\_r_22t\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_22t\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_22t\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_22t\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_22t\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_22t\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_22t\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_22t\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_22t\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_22t\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_22t\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_22t\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_22t\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_22t\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_22t\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_22t\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_22t\_ .icon-shape,#chatgpt-mermaid-\_r_22t\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_22t\_ .icon-shape p,#chatgpt-mermaid-\_r_22t\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_22t\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_22t\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_22t\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_22t\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_22t\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_22t\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_22t\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_22t\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_22t\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_22t\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_22t\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_22t\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_22t\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_22t\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_22t\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_22t\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_22t\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_22t\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_22t\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_22t\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_22t\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_22t\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_22t\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_22t\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_22t\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_22t\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_22t\_ .node rect,#chatgpt-mermaid-\_r_22t\_ .node circle,#chatgpt-mermaid-\_r_22t\_ .node ellipse,#chatgpt-mermaid-\_r_22t\_ .node polygon,#chatgpt-mermaid-\_r_22t\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_22t\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_22t\_ .node.mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_22t\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_22t\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_22t\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}Index headIndex nodeIndex nodeBase-level headBase node referenceBase node referenceBase node ABase node BBase node C

The diagram illustrates the general separation between navigation indexes and the ordered base list. Exact links and fields differ from this simplified drawing.

The important distinction is that the upper levels can be managed separately from the base-level nodes.

## 6.2 Why does that help?

The bottom-level list is the fundamental ordered collection.

Higher-level index nodes accelerate navigation, but they are auxiliary structures.

A concurrent implementation can perform a successful base-list insertion and then install indexing nodes separately. If an auxiliary indexing step fails or needs to be retried, the sorted base list can still represent the collection correctly, though traversal may become less efficient.

OpenJDK's implementation uses CAS-based techniques for index updates, and its deletion logic includes mechanisms for safely unlinking nodes while concurrent operations are in progress.&#x20;

[image](https://www.google.com/s2/favicons?domain=https://github.com\&sz=32)

GitHub



This is a major difference from the straightforward sequential implementation we built earlier.

In our implementation, we update a node's links one level at a time with no synchronization. In a production concurrent implementation, the order and coordination of these updates are part of the correctness proof.

## 6.3 What does weakly consistent iteration mean?

Consider:

```
ConcurrentSkipListMap<Integer, String> map =
    new ConcurrentSkipListMap<>();
```

One thread iterates over the map while another inserts and removes entries.

The iterator is weakly consistent.

It can safely operate while the map is modified, but it does not promise a frozen snapshot of every mapping as of a single instant. It may reflect some concurrent changes without reflecting others.

The Java documentation also says that bulk operations such as `putAll`, `clear`, and `toArray` are not guaranteed to be atomic as a whole.&#x20;

[image](https://www.google.com/s2/favicons?domain=https://docs.oracle.com\&sz=32)

Oracle Docs



This matters in real applications.

Suppose you need to read a collection and calculate a financial total from a consistent snapshot while other threads update it.

Simply iterating over a `ConcurrentSkipListMap` does not automatically give you that snapshot guarantee. You must choose a stronger coordination strategy or a different representation appropriate to the requirement.

### Example: When it is a good fit

```
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;

public class ScheduledTasks {
    private final ConcurrentNavigableMap<Long, String> tasks =
        new ConcurrentSkipListMap<>();

    public void schedule(long timestamp, String taskId) {
        tasks.put(timestamp, taskId);
    }

    public String nextTask(long currentTime) {
        var entry = tasks.ceilingEntry(currentTime);
        return entry == null ? null : entry.getValue();
    }

    public ConcurrentNavigableMap<Long, String> between(
            long from,
            long to) {
        return tasks.subMap(from, true, to, true);
    }
}
```

This is a useful demonstration of ordered concurrent access.

However, this particular model permits only one task per timestamp, because the timestamp is the map key. If multiple tasks can have the same timestamp, use a compound key, a collection of tasks per timestamp, or another explicit tie-breaking scheme.

Also, `ceilingEntry()` is an individual map operation. A subsequent `remove()` is not automatically atomic with it. If claiming the next task must happen exactly once across all workers, you need a suitable atomic claiming protocol as well.

# 7. Why memory management becomes difficult in concurrent skip lists

In our simple Java implementation, deleting a node means bypassing it through predecessor pointers.

After nothing references that node, the garbage collector can eventually reclaim it.

In a low-level language such as C++, that is not automatically safe.

Suppose Thread A is traversing a skip list and holds a pointer to node `40`.

Thread B removes node `40` and frees its memory.

Thread A may still try to read that memory.

This can result in a use-after-free bug.

### Common solutions

Concurrent data structures can use mechanisms such as:

- Garbage collection.
- Hazard pointers.
- Epoch-based reclamation.
- Read-copy-update (RCU) or related reclamation techniques.
- Designs that defer individual node reclamation until the entire structure is destroyed.

The correct choice depends on the programming language, lifetime model, concurrency algorithm, and latency requirements.

### Why is the RocksDB example interesting?

RocksDB's skip-list implementation retains allocated nodes until the skip list is destroyed, and its source documents that the non-link contents of published nodes are immutable. This simplifies the individual node-lifetime problem compared with a structure that immediately frees each deleted node.&#x20;

[image](https://www.google.com/s2/favicons?domain=https://github.com\&sz=32)

GitHub



This comes with a trade-off: memory remains occupied until the structure is destroyed, so the owner must manage the overall lifetime and size of the structure.

The important lesson is that correct concurrent pointer updates are only half the problem; safe node lifetime is the other half.

# 8. Production pitfalls you should recognize

Here are common situations in which an otherwise correct skip-list implementation can fail or perform poorly.

| Pitfall                                           | Why it matters                                                                       | Typical mitigation                                                          |
| ------------------------------------------------- | ------------------------------------------------------------------------------------ | --------------------------------------------------------------------------- |
| Unsynchronized updates                            | Concurrent changes can lose links or corrupt invariants                              | Use a lock or a correct concurrent algorithm                                |
| Unbounded or incorrectly configured height policy | A height cap that is too small can weaken search performance as the collection grows | Choose an appropriate maximum level                                         |
| Too many high-level pointers                      | Memory overhead increases                                                            | Tune promotion probability and node representation                          |
| Poor memory locality                              | Pointer chasing can produce cache misses                                             | Benchmark; consider compact or specialized representations                  |
| Incorrect range comparisons                       | Entries may be omitted or included incorrectly                                       | Define inclusive/exclusive boundaries explicitly                            |
| Duplicate-key ambiguity                           | Equal values can be inserted or removed incorrectly                                  | Define unique-key or duplicate-entry semantics                              |
| Incorrect span maintenance                        | Rank queries return the wrong positions                                              | Test pointer distances after every insertion and deletion                   |
| Unsafe reclamation                                | A reader can access freed memory                                                     | Use the runtime or memory-reclamation strategy appropriate to the algorithm |

One more subtle point: the random-level generator is normally a performance mechanism, not a cryptographic security feature. Do not assume that randomized heights provide strong resistance to adversarial workloads or a deterministic worst-case guarantee.

If a strict upper bound is required, a standard skip list's expected complexity is not enough by itself.

# 9. Advanced skip-list variants

Once you understand the basic skip list, you can recognize several important extensions.

## 9.1 Indexable skip lists

An indexable skip list augments forward pointers with spans or other rank information.

This supports operations such as:

- Find the rank of a key.
- Find the element at rank \\(k\\).
- Navigate to an index without traversing the whole bottom level.

The extra metadata increases the complexity of updates because spans must remain correct after insertions and deletions.

Redis's rank-supporting skip list is an example of this design.

## 9.2 Concurrent skip lists

These use specialized algorithms for safe concurrent access.

They may employ CAS, logical deletion, marker nodes, separate index structures, locks, or other coordination mechanisms.

The key distinction is that the entire algorithm is designed for concurrent updates. Simply adding atomic pointer variables to the sequential implementation is not sufficient.

Java's `ConcurrentSkipListMap` is a practical example.

## 9.3 Storage-engine skip lists

Storage engines adapt skip lists to their memory and indexing requirements.

For example, RocksDB's memtable skip list uses specialized allocation and supports concurrency and ordered traversal. Its node lifecycle and insertion semantics differ from the educational Java implementation.&#x20;

[image](https://www.google.com/s2/favicons?domain=https://github.com\&sz=32)

GitHub

+1



These variants illustrate that skip lists are a family of design techniques, not a single fixed node layout.

# 10. Interview mastery: 12 important questions

These are the questions I would prioritize for a senior backend engineering interview.

### Q1. What is a skip list?

A skip list is a sorted linked-list-based data structure augmented with multiple levels of forward pointers. The bottom level contains every element, while higher levels contain selected nodes that provide shortcuts. Randomized node heights allow expected logarithmic search, insertion, and deletion.

### Q2. Why does a skip list achieve expected \\(O(\log n)\\) search?

Each node is promoted to the next level with probability \\(p\\). The expected number of nodes at level \\(k\\) decreases geometrically as \\(np^{k-1}\\). This creates a hierarchy with logarithmic expected height and bounded expected horizontal work per level.

### Q3. What is the worst-case search complexity?

\\(O(n)\\). A valid skip list can have insufficiently useful upper-level shortcuts, producing a search path that resembles a normal linked list.

### Q4. Why use a skip list instead of a balanced tree?

Both support efficient ordered operations. Skip lists are comparatively straightforward to implement because balancing is probabilistic and does not require rotations. Balanced trees provide deterministic worst-case logarithmic guarantees, so the correct choice depends on the requirements.

### Q5. Why use `update[]` during insertion?

It stores the predecessor at each level. Once the new node's height is chosen, those predecessors identify the links that must change. The array allows the implementation to avoid repeating searches.

### Q6. Does a skip list support \\(O(\log n)\\) rank queries automatically?

No. A basic skip list does not necessarily know how many bottom-level elements each forward pointer crosses. Spans or equivalent metadata can provide expected logarithmic rank operations.

### Q7. How does Redis use a skip list?

Redis's skip-list-backed sorted sets combine an ordered structure with a dictionary for member lookup. The skip list supports score ordering and range traversal, while span metadata supports efficient rank calculations.

### Q8. Why isn't every Redis sorted set stored as a skip list?

A full skip list and dictionary have additional memory overhead. Redis supports a compact listpack representation for small sorted sets and can transition to a skip-list-backed representation when configured size constraints are exceeded.

### Q9. Why is a regular skip list not thread-safe?

Insertion and deletion change shared forward pointers across several levels. Concurrent operations can overwrite each other's updates or observe intermediate structural changes. Safe concurrency requires a coordinated algorithm or synchronization.

### Q10. What is the difference between `SkipList` and `ConcurrentSkipListMap` in Java?

Our educational implementation uses ordinary mutable pointer arrays without synchronization. `ConcurrentSkipListMap` is a production concurrent navigable map with algorithms designed to support concurrent access and updates, expected average logarithmic operations, and weakly consistent iterators.&#x20;

[image](https://www.google.com/s2/favicons?domain=https://docs.oracle.com\&sz=32)

Oracle Docs

+1



### Q11. Where do storage engines use skip lists?

A skip list can serve as an in-memory ordered index or memtable, supporting interleaved writes and range scans. RocksDB documents a skip-list-based default memtable and alternative implementations for other workloads.&#x20;

[image](https://www.google.com/s2/favicons?domain=https://github.com\&sz=32)

GitHub



### Q12. When would you avoid a skip list?

Avoid choosing a standard skip list solely on the assumption that it is always faster or always \\(O(\log n)\\). A hash table is more natural for exact-key lookups without ordering. A sorted array can work well for static, read-heavy datasets. A priority queue suits repeated minimum/maximum extraction. A balanced tree is appropriate when deterministic worst-case logarithmic bounds are required, and a B+ tree is commonly suitable for page-oriented database indexes.

# 11. Final decision framework

When choosing a skip list for a production system, work through these questions.

Is the data ordered? If not, a skip list's main advantage may not be useful.

Do you need arbitrary ordered insertion and deletion? If the collection changes frequently, a skip list can be more attractive than a sorted array.

Do you need range queries? A skip list naturally supports finding a starting position and traversing the results.

Do you need rank queries? Consider an indexable skip list with spans or another rank-augmented structure.

Will many threads update the structure? Use a concurrent implementation with documented guarantees, or select an explicit locking strategy. Do not share the basic sequential implementation across threads.

Does the collection live in memory or on disk? Memory layout and page-access patterns may strongly favor different structures.

What worst-case behavior can your system tolerate? A standard skip list provides expected logarithmic performance, not a deterministic logarithmic worst-case guarantee.

These questions help you choose based on the workload rather than the data structure's name.

# 12. The complete skip-list mental model

Let's bring the course together.

A skip list begins with a sorted linked list. Each node receives a randomly chosen height and participates in that many levels. The upper levels provide shortcuts, while the bottom level remains the complete ordered collection.

During a search, we move forward while the next key does not exceed the target's safe search boundary. When a forward step would overshoot the target, we descend a level. The expected number of levels and the expected horizontal work produce expected \\(O(\log n)\\) searches.

Insertion and deletion first discover predecessor nodes. A temporary `update[]` array stores those predecessors, letting the algorithm modify only the necessary pointers. A standard skip list uses expected \\(O(\log n)\\) time for search, insertion, and deletion, and expected \\(O(n)\\) space when the promotion probability is fixed and the height policy scales appropriately.

Production systems extend the basic structure for particular requirements. Redis adds a dictionary, backward links, and spans for sorted-set operations. Java's concurrent skip list uses a specialized concurrency algorithm. RocksDB adapts skip lists to in-memory ordered storage and range scans.

The most important principle is this:

A skip list is a probabilistically balanced ordered index. Its value comes from combining efficient search with local pointer updates, and its suitability depends on the workload, memory layout, concurrency model, and guarantees the system requires.

## Final mastery checklist

You have mastered the core concepts when you can do all of the following without referring to the code:

- Draw a skip list and explain how node heights determine level membership.
- Trace a search and explain why descending a level is safe.
- Insert and delete a node while updating all relevant forward pointers.
- Explain the `update[]` array and the purpose of the head sentinel.
- Derive \\(P(H\ge k)=p^{k-1}\\) and explain expected \\(O(\log n)\\) operations.
- Distinguish expected, worst-case, and amortized complexity.
- Compare skip lists with arrays, hash tables, trees, heaps, and B+ trees.
- Explain Redis's hash table, skip list, spans, and compact representation.
- Explain why a sequential skip list is not thread-safe and why concurrent variants need specialized algorithms.
- Select a suitable data structure based on a production workload.

That completes the 10-chapter course. The next best exercise is to implement a skip list independently without looking at Chapter 9, then test it against `TreeSet` using randomized insertions, deletions, searches, and range queries.
