# Chapter 2: Implementing Consistent Hashing

## Lesson 1: Building the Ring Data Structure from Scratch

In Chapter 1, we learned the intuition behind consistent hashing. Now let's implement it in Java and understand exactly what happens internally.

We'll build the implementation incrementally so you understand the design decisions rather than just memorizing code.

## 1. Define the problem

Imagine we have a distributed cache with three physical nodes:

```
Cache Node A
Cache Node B
Cache Node C
```

We want a function that takes a key and returns its owner.

```
String findNode(String key);
```

For example:

```
findNode("user:101"); // Node B
findNode("user:202"); // Node A
findNode("user:303"); // Node C
```

Our implementation should satisfy these requirements:

1. The same key and ring configuration should produce the same owner.
2. Adding a node should move only a subset of keys.
3. Removing a node should move only keys previously owned by that node, assuming the remaining ring configuration is unchanged.
4. Keys should be distributed reasonably evenly.
5. The implementation should support efficient lookup.

We'll use a sorted map to represent the ring.

## 2. Representing the ring

Recall that a consistent-hashing ring contains positions for nodes and keys.

For implementation, we don't need to create a literal circular data structure. We can represent the ring as a sorted sequence of hash positions.

For example:

```
Hash position    Node
-------------    ----
100              Node A
250              Node B
500              Node C
750              Node A
900              Node B
```

A Java `TreeMap` is a natural fit:

```
NavigableMap<Long, String> ring = new TreeMap<>();
```

Here:

- `Long` is the hash position.
- `String` is the node identifier.
- `TreeMap` keeps the positions sorted.
- `NavigableMap` gives us operations such as `ceilingEntry()` and `firstEntry()`.

Why do we need sorting? Because the ownership rule is:

> Find the first node position at or clockwise after the key's hash. If there is no such position, wrap around to the first position on the ring.

Suppose the ring contains these positions:

```
100 → Node A
250 → Node B
500 → Node C
750 → Node A
900 → Node B
```

A key hashes to position `300`.

The first position greater than or equal to `300` is `500`, so Node C owns the key.

If a key hashes to `950`, there is no position after it. We wrap around to position `100`, so Node A owns it.

\#chatgpt-mermaid-\_r_428\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_428\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_428\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_428\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_428\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_428\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_428\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_428\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_428\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_428\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_428\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_428\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_428\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_428\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_428\_ p{margin:0;}#chatgpt-mermaid-\_r_428\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_428\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_428\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_428\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_428\_ .label text,#chatgpt-mermaid-\_r_428\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_428\_ .node rect,#chatgpt-mermaid-\_r_428\_ .node circle,#chatgpt-mermaid-\_r_428\_ .node ellipse,#chatgpt-mermaid-\_r_428\_ .node polygon,#chatgpt-mermaid-\_r_428\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_428\_ .rough-node .label text,#chatgpt-mermaid-\_r_428\_ .node .label text,#chatgpt-mermaid-\_r_428\_ .image-shape .label,#chatgpt-mermaid-\_r_428\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_428\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_428\_ .rough-node .label,#chatgpt-mermaid-\_r_428\_ .node .label,#chatgpt-mermaid-\_r_428\_ .image-shape .label,#chatgpt-mermaid-\_r_428\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_428\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_428\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_428\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_428\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_428\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_428\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_428\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_428\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_428\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_428\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_428\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_428\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_428\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_428\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_428\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_428\_ .icon-shape,#chatgpt-mermaid-\_r_428\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_428\_ .icon-shape p,#chatgpt-mermaid-\_r_428\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_428\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_428\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_428\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_428\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_428\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_428\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_428\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_428\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_428\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_428\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_428\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_428\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_428\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_428\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_428\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_428\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_428\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_428\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_428\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_428\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_428\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_428\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_428\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_428\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_428\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_428\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_428\_ .node rect,#chatgpt-mermaid-\_r_428\_ .node circle,#chatgpt-mermaid-\_r_428\_ .node ellipse,#chatgpt-mermaid-\_r_428\_ .node polygon,#chatgpt-mermaid-\_r_428\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_428\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_428\_ .node.mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_428\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_428\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_428\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}Hash keyFind first ring position ≥ keyhashPosition found?Return node at that positionWrap around to first ringpositionReturn first nodeYesNo

This is the core lookup algorithm.

## 3. Why `TreeMap`?

A `TreeMap` is backed by a balanced search tree, typically a red-black tree in Java.

For \\(M\\) positions on the ring:

| Operation                                  | Typical time complexity                                       |
| ------------------------------------------ | ------------------------------------------------------------- |
| Insert a ring position                     | \\(O(\log M)\\)                                               |
| Remove a ring position                     | \\(O(\log M)\\)                                               |
| Find the first position at or after a hash | \\(O(\log M)\\)                                               |
| Find the first ring position               | \\(O(\log M)\\) or better depending on implementation details |

The important operation is:

```
ring.ceilingEntry(keyHash);
```

It returns the entry with the smallest key greater than or equal to `keyHash`.

If no such entry exists, it returns `null`, and we wrap around.

Without a sorted data structure, a straightforward implementation might scan every node position, requiring \\(O(M)\\) lookup time. A sorted ring reduces lookup to \\(O(\log M)\\).

Here, \\(M\\) means the total number of virtual-node positions, not just physical machines.

## 4. Implement a basic ring

Let's begin with a simplified implementation.

```

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

public class ConsistentHashRing {

    private final NavigableMap<Long, String> ring = new TreeMap<>();

    public void addNode(String nodeId) {
        long position = hash(nodeId);
        ring.put(position, nodeId);
    }

    public String findNode(String key) {
        if (ring.isEmpty()) {
            throw new IllegalStateException("No nodes are available");
        }

        long keyHash = hash(key);

        Map.Entry<Long, String> entry = ring.ceilingEntry(keyHash);

        if (entry == null) {
            entry = ring.firstEntry();
        }

        return entry.getValue();
    }

    private long hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(
                value.getBytes(StandardCharsets.UTF_8)
            );

            long result = 0;

            for (int i = 0; i < 8; i++) {
                result = (result << 8) | (hashBytes[i] & 0xffL);
            }

            // Use a non-negative 63-bit position.
            return result & Long.MAX_VALUE;

        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }

    public static void main(String[] args) {
        ConsistentHashRing ring = new ConsistentHashRing();

        ring.addNode("node-A");
        ring.addNode("node-B");
        ring.addNode("node-C");

        System.out.println(ring.findNode("user:101"));
        System.out.println(ring.findNode("user:202"));
        System.out.println(ring.findNode("user:303"));
    }
}

```

The output will depend on the hash positions, but repeated runs with the same implementation and nodes will produce the same mapping.

### Understand the code

Step 1: Add a node

```
ring.put(position, nodeId);
```

We hash the node identifier and use that position to represent the node on the ring.

Step 2: Hash the requested key

```
long keyHash = hash(key);
```

The key is mapped into the same hash space as the node positions.

Step 3: Find the clockwise owner

```
Map.Entry<Long, String> entry = ring.ceilingEntry(keyHash);
```

This finds the first node position at or after the key's position.

Step 4: Wrap around if necessary

```
if (entry == null) {
    entry = ring.firstEntry();
}
```

The ring is logically circular, even though `TreeMap` is linear and sorted.

### An important limitation

This implementation hashes each physical node only once. That can lead to uneven distribution, so production-style consistent hashing usually uses virtual nodes.

It also does not yet implement node removal, collision handling, or safe data migration. We'll improve it throughout this chapter.

## 5. One subtle but important detail: collision handling

Notice the type:

```
NavigableMap<Long, String>
```

A map can store only one value for each hash position.

If two nodes hash to the same position, this line:

```
ring.put(position, nodeId);
```

can overwrite the previous node.

A production implementation must explicitly handle that possibility, for example by using a structure that supports multiple node entries at the same position, or by defining a deterministic composite ordering of hash position and node identity.

Using SHA-256 makes accidental collisions very unlikely, but low probability is not the same as a defined collision-handling strategy.

## 6. What we have built

Our current implementation has the following behavior:

| Requirement                 | Current implementation                    |
| --------------------------- | ----------------------------------------- |
| Deterministic key placement | Yes, for the same hash algorithm and ring |
| Efficient owner lookup      | Yes, \\(O(\log M)\\)                      |
| Wrap-around                 | Yes                                       |
| Multiple positions per node | Not yet                                   |
| Node removal                | Not yet                                   |
| Collision handling          | Not yet                                   |
| Automatic data migration    | No                                        |
| Replication and failover    | No                                        |

The last two rows are especially important. A ring determines placement; it does not transfer records or guarantee that an owner is healthy.

### Check your understanding

Suppose the ring contains positions `100`, `300`, and `700`.

- A key hashes to `250`. Which position owns it?
- A key hashes to `800`. Which position owns it?
- If you add a node at position `400`, which existing key ranges can change owners?

Try answering these before moving on.

Next lesson: We'll introduce virtual nodes into the implementation, examine why one position per physical node is insufficient, and build a more balanced ring with proper node registration and removal.

Type `next` to continue.


















# Chapter 2: Implementing Consistent Hashing

## Lesson 2: Virtual Nodes — Implementation, Distribution, and Rebalancing

In the previous lesson, we built a basic consistent-hashing ring using Java's `TreeMap`.

That implementation has a significant weakness: each physical node gets only one position on the ring.

Let's understand why that causes problems and how virtual nodes improve the design.

## 1. The problem with one position per node

Suppose we have four physical nodes:

```
Node A
Node B
Node C
Node D
```

We hash each node ID once and place it on the ring.

Imagine the resulting positions are:

```
100 → Node A
200 → Node B
800 → Node C
900 → Node D
```

The ring looks like this:

\#chatgpt-mermaid-\_r_45b\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_45b\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_45b\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_45b\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_45b\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_45b\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_45b\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_45b\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_45b\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_45b\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_45b\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_45b\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_45b\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_45b\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_45b\_ p{margin:0;}#chatgpt-mermaid-\_r_45b\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_45b\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_45b\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_45b\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_45b\_ .label text,#chatgpt-mermaid-\_r_45b\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_45b\_ .node rect,#chatgpt-mermaid-\_r_45b\_ .node circle,#chatgpt-mermaid-\_r_45b\_ .node ellipse,#chatgpt-mermaid-\_r_45b\_ .node polygon,#chatgpt-mermaid-\_r_45b\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_45b\_ .rough-node .label text,#chatgpt-mermaid-\_r_45b\_ .node .label text,#chatgpt-mermaid-\_r_45b\_ .image-shape .label,#chatgpt-mermaid-\_r_45b\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_45b\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_45b\_ .rough-node .label,#chatgpt-mermaid-\_r_45b\_ .node .label,#chatgpt-mermaid-\_r_45b\_ .image-shape .label,#chatgpt-mermaid-\_r_45b\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_45b\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_45b\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_45b\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_45b\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_45b\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_45b\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_45b\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_45b\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_45b\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_45b\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_45b\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_45b\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_45b\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_45b\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_45b\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_45b\_ .icon-shape,#chatgpt-mermaid-\_r_45b\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_45b\_ .icon-shape p,#chatgpt-mermaid-\_r_45b\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_45b\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_45b\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_45b\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_45b\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_45b\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_45b\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_45b\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_45b\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_45b\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_45b\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_45b\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_45b\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_45b\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_45b\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_45b\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_45b\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_45b\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_45b\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_45b\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_45b\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_45b\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_45b\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_45b\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_45b\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_45b\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_45b\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_45b\_ .node rect,#chatgpt-mermaid-\_r_45b\_ .node circle,#chatgpt-mermaid-\_r_45b\_ .node ellipse,#chatgpt-mermaid-\_r_45b\_ .node polygon,#chatgpt-mermaid-\_r_45b\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_45b\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_45b\_ .node.mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_45b\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_45b\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_45b\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}Node A at 100Node B at 200Node C at 800Node D at 900

Remember that each node owns the interval after the previous node's position, up to and including its own position.

Therefore, the ownership ranges are approximately:

| Node | Owned hash range           | Approximate share |
| ---- | -------------------------- | ----------------- |
| A    | 900 → 100, wrapping around | 20%               |
| B    | 100 → 200                  | 10%               |
| C    | 200 → 800                  | 60%               |
| D    | 800 → 900                  | 10%               |

Node C owns approximately 60% of the hash space, while Node B owns only 10%.

This is a simplified illustration, but it demonstrates the problem: a node's position is random, and a single position can produce a large ownership range.

If these ranges correspond to millions of keys, Node C might hold far more data than the other nodes.

Consistent hashing limits remapping when membership changes; it does not guarantee balanced ownership when each node has only one position.

## 2. The intuition behind virtual nodes

Instead of placing each physical node at one position, place it at many positions.

For example, Node A becomes:

```
Node A
 ├── A#0
 ├── A#1
 ├── A#2
 ├── A#3
 └── A#4
```

Each virtual node has its own hash position on the ring, but all virtual nodes point to the same physical node.

\#chatgpt-mermaid-\_r_45s\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_45s\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_45s\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_45s\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_45s\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_45s\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_45s\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_45s\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_45s\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_45s\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_45s\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_45s\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_45s\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_45s\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_45s\_ p{margin:0;}#chatgpt-mermaid-\_r_45s\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_45s\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_45s\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_45s\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_45s\_ .label text,#chatgpt-mermaid-\_r_45s\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_45s\_ .node rect,#chatgpt-mermaid-\_r_45s\_ .node circle,#chatgpt-mermaid-\_r_45s\_ .node ellipse,#chatgpt-mermaid-\_r_45s\_ .node polygon,#chatgpt-mermaid-\_r_45s\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_45s\_ .rough-node .label text,#chatgpt-mermaid-\_r_45s\_ .node .label text,#chatgpt-mermaid-\_r_45s\_ .image-shape .label,#chatgpt-mermaid-\_r_45s\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_45s\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_45s\_ .rough-node .label,#chatgpt-mermaid-\_r_45s\_ .node .label,#chatgpt-mermaid-\_r_45s\_ .image-shape .label,#chatgpt-mermaid-\_r_45s\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_45s\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_45s\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_45s\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_45s\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_45s\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_45s\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_45s\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_45s\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_45s\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_45s\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_45s\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_45s\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_45s\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_45s\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_45s\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_45s\_ .icon-shape,#chatgpt-mermaid-\_r_45s\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_45s\_ .icon-shape p,#chatgpt-mermaid-\_r_45s\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_45s\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_45s\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_45s\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_45s\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_45s\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_45s\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_45s\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_45s\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_45s\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_45s\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_45s\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_45s\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_45s\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_45s\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_45s\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_45s\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_45s\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_45s\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_45s\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_45s\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_45s\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_45s\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_45s\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_45s\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_45s\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_45s\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_45s\_ .node rect,#chatgpt-mermaid-\_r_45s\_ .node circle,#chatgpt-mermaid-\_r_45s\_ .node ellipse,#chatgpt-mermaid-\_r_45s\_ .node polygon,#chatgpt-mermaid-\_r_45s\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_45s\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_45s\_ .node.mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_45s\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_45s\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_45s\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}Physical Node AA#0A#1A#2A#3A#4

With four physical nodes and five virtual nodes per physical node, the ring contains 20 positions.

```
Hash ring
────────────────────────────────────────────────
A#2 → C#1 → B#4 → D#0 → A#0 → C#3 → ...
────────────────────────────────────────────────
```

The positions are interleaved because each virtual node is hashed independently.

This tends to break one large ownership range into many smaller ranges.

### The key idea

Virtual nodes do not create additional physical servers.

They create additional logical positions on the ring, giving each physical server multiple opportunities to own different ranges.

## 3. How virtual nodes improve distribution

Suppose there are \\(N\\) physical nodes, each with \\(V\\) virtual nodes.

The total number of positions is:

\\[ M = N \times V \\]

For example:

- 10 physical nodes
- 100 virtual nodes per physical node
- 1,000 total ring positions

Each node owns multiple small ranges rather than one large range.

If the virtual-node positions are distributed uniformly, a node's total ownership is the sum of many smaller intervals. This tends to reduce variation in the amount of hash space owned by each physical node.

The distinction matters:

- One position per node: ownership depends heavily on a single random interval.
- Many positions per node: ownership is the sum of many intervals, so the relative imbalance tends to decrease as the number of virtual nodes increases.

Virtual nodes improve statistical balance, but they don't guarantee that every node owns exactly the same amount of data.

Also, balanced hash space does not guarantee balanced traffic. A hot key still routes to its owner unless you add caching, replication, or another strategy.

## 4. Implement virtual nodes in Java

Let's improve our previous implementation.

We'll introduce a configurable virtual-node count and generate a unique identifier for each virtual node.

For example:

```
node-A#0
node-A#1
node-A#2
...
node-A#99
```

Each identifier is hashed separately.

```

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

public class ConsistentHashRing {

    private final NavigableMap<Long, String> ring = new TreeMap<>();
    private final int virtualNodesPerPhysicalNode;

    public ConsistentHashRing(int virtualNodesPerPhysicalNode) {
        if (virtualNodesPerPhysicalNode <= 0) {
            throw new IllegalArgumentException(
                "Virtual-node count must be positive"
            );
        }

        this.virtualNodesPerPhysicalNode = virtualNodesPerPhysicalNode;
    }

    public void addNode(String nodeId) {
        for (int i = 0; i < virtualNodesPerPhysicalNode; i++) {
            String virtualNodeId = nodeId + "#" + i;
            long position = hash(virtualNodeId);

            ring.put(position, nodeId);
        }
    }

    public String findNode(String key) {
        if (ring.isEmpty()) {
            throw new IllegalStateException("No nodes are available");
        }

        long keyHash = hash(key);

        Map.Entry<Long, String> entry = ring.ceilingEntry(keyHash);

        if (entry == null) {
            entry = ring.firstEntry();
        }

        return entry.getValue();
    }

    private long hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            byte[] bytes = digest.digest(
                value.getBytes(StandardCharsets.UTF_8)
            );

            long result = 0;

            for (int i = 0; i < 8; i++) {
                result = (result << 8) | (bytes[i] & 0xffL);
            }

            return result & Long.MAX_VALUE;

        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(
                "SHA-256 is unavailable",
                e
            );
        }
    }

    public static void main(String[] args) {
        ConsistentHashRing ring = new ConsistentHashRing(100);

        ring.addNode("node-A");
        ring.addNode("node-B");
        ring.addNode("node-C");

        System.out.println(ring.findNode("customer:101"));
        System.out.println(ring.findNode("customer:202"));
        System.out.println(ring.findNode("customer:303"));
    }
}

```

### What changed?

Previously, each node was inserted once:

```
ring.put(hash(nodeId), nodeId);
```

Now, each physical node is inserted multiple times:

```
for (int i = 0; i < virtualNodesPerPhysicalNode; i++) {
    String virtualNodeId = nodeId + "#" + i;
    ring.put(hash(virtualNodeId), nodeId);
}
```

The ring stores the physical node ID as the value. Therefore, regardless of which virtual node owns the key's range, the lookup returns the physical server that should handle the request.

### Important limitations of this sample

This is a learning implementation, not a complete production library:

- Adding the same node twice can create duplicate registrations.
- Removing a node is not implemented.
- Hash-position collisions can overwrite entries.
- It assumes all application instances share the same membership and hashing configuration.
- It doesn't move data or provide replication.

We'll address the core implementation concerns incrementally.

## 5. Why does the virtual-node identifier matter?

Consider these two physical nodes:

```
node-A
node-B
```

We generate virtual nodes using:

```
node-A#0
node-A#1
node-A#2

node-B#0
node-B#1
node-B#2
```

Each identifier produces a different hash position with high probability.

But suppose we accidentally generate virtual-node IDs incorrectly:

```
node-A#0
node-A#0
node-A#0
```

We haven't created three useful independent positions. We're repeatedly hashing the same identifier.

The result is either repeated positions or collisions that collapse entries in the map.

Virtual-node IDs must therefore be deterministic and unique for each physical node and virtual-node index.

This also matters across application instances. If one instance names a virtual node `node-A#2` while another names it `node-A-2`, they can build different rings.

## 6. How many virtual nodes should we use?

There is no universally optimal number.

A small number reduces metadata and membership-update overhead, but may leave substantial imbalance. A larger number generally improves statistical distribution at the cost of more positions to maintain.

| Virtual nodes per physical node | General tradeoff                                      |
| ------------------------------- | ----------------------------------------------------- |
| 1                               | Minimal overhead, potentially significant imbalance   |
| 10                              | Better distribution, but still potentially uneven     |
| 100                             | Often a reasonable starting point for experiments     |
| 1,000                           | More positions and metadata; improvement may diminish |

These are illustrative values, not universal production recommendations. Actual behavior depends on the hashing scheme, cluster size, workload, and implementation.

You should measure:

- The percentage of hash space owned by each node.
- Actual key counts per node.
- Requests and CPU per node.
- Storage usage and migration volume.
- The impact of adding or removing a node.

A ring can look balanced in terms of hash space but remain unbalanced in real workload.

## 7. Heterogeneous nodes and weighted virtual nodes

Now imagine a cluster with three servers:

```
Node A: 8 CPU cores, 32 GB RAM
Node B: 8 CPU cores, 32 GB RAM
Node C: 32 CPU cores, 128 GB RAM
```

Giving every node the same number of virtual nodes treats them as if they had equal capacity.

That may waste Node C's capacity or overload the smaller nodes.

A weighted approach might allocate virtual nodes in proportion to effective capacity:

```
Node A → 100 virtual nodes
Node B → 100 virtual nodes
Node C → 400 virtual nodes
```

This approximates a 1:1:4 capacity ratio.

But be careful: CPU and memory aren't always interchangeable capacity measures. For a storage-heavy workload, available disk and I/O may matter more. For a latency-sensitive service, the bottleneck may be CPU, memory, or network throughput.

Weighted placement is useful when node capacities differ, but it should be guided by observed workload and capacity planning.

## 8. What happens when we add a physical node?

Suppose our cluster contains Nodes A, B, and C, each with 100 virtual nodes.

We add Node D, also with 100 virtual nodes.

\#chatgpt-mermaid-\_r_47q\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_47q\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_47q\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_47q\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_47q\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_47q\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_47q\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_47q\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_47q\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_47q\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_47q\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_47q\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_47q\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_47q\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_47q\_ p{margin:0;}#chatgpt-mermaid-\_r_47q\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_47q\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_47q\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_47q\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_47q\_ .label text,#chatgpt-mermaid-\_r_47q\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_47q\_ .node rect,#chatgpt-mermaid-\_r_47q\_ .node circle,#chatgpt-mermaid-\_r_47q\_ .node ellipse,#chatgpt-mermaid-\_r_47q\_ .node polygon,#chatgpt-mermaid-\_r_47q\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_47q\_ .rough-node .label text,#chatgpt-mermaid-\_r_47q\_ .node .label text,#chatgpt-mermaid-\_r_47q\_ .image-shape .label,#chatgpt-mermaid-\_r_47q\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_47q\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_47q\_ .rough-node .label,#chatgpt-mermaid-\_r_47q\_ .node .label,#chatgpt-mermaid-\_r_47q\_ .image-shape .label,#chatgpt-mermaid-\_r_47q\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_47q\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_47q\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_47q\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_47q\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_47q\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_47q\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_47q\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_47q\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_47q\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_47q\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_47q\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_47q\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_47q\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_47q\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_47q\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_47q\_ .icon-shape,#chatgpt-mermaid-\_r_47q\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_47q\_ .icon-shape p,#chatgpt-mermaid-\_r_47q\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_47q\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_47q\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_47q\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_47q\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_47q\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_47q\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_47q\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_47q\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_47q\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_47q\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_47q\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_47q\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_47q\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_47q\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_47q\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_47q\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_47q\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_47q\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_47q\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_47q\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_47q\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_47q\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_47q\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_47q\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_47q\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_47q\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_47q\_ .node rect,#chatgpt-mermaid-\_r_47q\_ .node circle,#chatgpt-mermaid-\_r_47q\_ .node ellipse,#chatgpt-mermaid-\_r_47q\_ .node polygon,#chatgpt-mermaid-\_r_47q\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_47q\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_47q\_ .node.mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_47q\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_47q\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_47q\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}Ring has A, B, CAdd D's 100 virtual positionsRecalculate ownershipOnly affected ranges changeowner

Each new virtual-node position captures a range that previously belonged to its clockwise successor.

Only keys in those affected ranges should change owner, assuming the hash function, ring rules, and other node positions remain unchanged.

The new node does not automatically receive the existing data in those ranges. That still requires a data-migration protocol.

### What if we remove Node D?

Removing D's virtual-node positions causes the keys in D's ownership ranges to move to the next eligible positions on the ring.

The rest of the ring remains unchanged.

This is why consistent hashing is useful for clusters whose membership changes frequently: ownership can be adjusted without remapping nearly every key.

## 9. Test your understanding

Suppose we have three physical nodes and 100 virtual nodes per physical node.

1. How many ring positions should exist?
2. If a key maps to virtual node `node-B#37`, which physical node handles the request?
3. Why does increasing the virtual-node count generally improve balance?
4. Does adding Node D automatically migrate the data that Node D should now own?
5. If a single key receives 40% of all requests, will virtual nodes alone solve the hotspot?

Answers:

1. \\(3 \times 100 = 300\\) positions, assuming no collisions or duplicate registrations.
2. Node B.
3. Each physical node owns many smaller intervals, reducing the effect of any one unusually large interval.
4. No. Placement and data migration are separate operations.
5. No. A hot key can still overload its owner.

## Key takeaways

- A virtual node is a logical position, not a physical machine.
- Multiple virtual nodes help distribute ownership more evenly.
- `TreeMap` supports efficient ring lookups with `ceilingEntry()`.
- The number of virtual nodes is a tuning parameter, not a magic constant.
- Weighted virtual nodes can account for different server capacities.
- Node membership changes still require safe migration and ownership coordination.

Next lesson: We'll implement safe node addition and removal, including duplicate-registration prevention, removal of all virtual-node positions, collision-aware ring design, and tests to verify that adding a node doesn't remap more keys than necessary.

Type `next` to continue.

















# Chapter 2: Implementing Consistent Hashing

## Lesson 3: Adding and Removing Nodes Safely

In the previous lesson, we introduced virtual nodes. Now we'll make the implementation more robust by supporting node registration and removal.

This lesson is important because real distributed systems don't have a fixed set of servers forever. Nodes are added during scaling, removed during maintenance, and replaced when hardware or instances fail.

Our goal is to make membership changes predictable.

## 1. What must happen when we add a node?

Suppose the ring currently contains three physical nodes:

```
Node A → 100 virtual nodes
Node B → 100 virtual nodes
Node C → 100 virtual nodes
```

We want to add Node D.

The correct procedure is to:

1. Validate the node identifier.
2. Check whether the node is already registered.
3. Calculate all of its virtual-node positions.
4. Insert those positions into the ring.
5. Update the registered-node metadata.

After insertion, some key ranges will belong to D instead of their previous owners.

### Why check for duplicate registration?

Consider this code:

```
ring.addNode("node-A");
ring.addNode("node-A");
```

Without duplicate protection, the implementation might register the same node twice, corrupt metadata, or later remove positions that are still supposed to exist.

Even if the ring map happens to overwrite identical positions, that behavior is not a safe membership protocol.

A node should have one logical registration, with a clearly defined set of virtual-node positions.

## 2. What must happen when we remove a node?

Suppose Node B has 100 virtual nodes:

```
node-B#0
node-B#1
node-B#2
...
node-B#99
```

Removing only one position is not enough.

We must remove every position belonging to Node B.

Otherwise, the ring might still route some keys to a node that we believe has been removed.

A useful structure is:

```
Map<String, List<Long>> nodePositions;
```

This maps each physical node ID to the ring positions registered for that node.

For example:

```
node-A → [120, 850, 930, ...]
node-B → [250, 420, 780, ...]
node-C → [100, 510, 670, ...]
```

When removing Node B, we use its recorded positions to remove every corresponding ring entry.

This is more reliable than attempting to reconstruct the positions from assumptions about how the node was originally registered.

## 3. A safer Java implementation

We'll improve the ring with four changes:

- Track registered physical nodes.
- Track every virtual-node position for each physical node.
- Support adding and removing nodes.
- Handle ring-position collisions deterministically instead of silently overwriting entries.

We'll also synchronize membership operations and lookups. This protects the in-memory data structures from concurrent modification within one Java process. It does not coordinate membership across multiple application instances.

```

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;

public final class ConsistentHashRing {

    private final int virtualNodesPerPhysicalNode;

    // Every unique ring position maps to its physical node.
    private final NavigableMap<RingPosition, String> ring =
            new TreeMap<>();

    // Tracks the exact positions registered for each physical node.
    private final Map<String, List<RingPosition>> nodePositions =
            new HashMap<>();

    public ConsistentHashRing(int virtualNodesPerPhysicalNode) {
        if (virtualNodesPerPhysicalNode <= 0) {
            throw new IllegalArgumentException(
                    "Virtual-node count must be positive"
            );
        }

        this.virtualNodesPerPhysicalNode = virtualNodesPerPhysicalNode;
    }

    public synchronized void addNode(String nodeId) {
        validateNodeId(nodeId);

        if (nodePositions.containsKey(nodeId)) {
            throw new IllegalArgumentException(
                    "Node already registered: " + nodeId
            );
        }

        List<RingPosition> positions = new ArrayList<>();

        for (int i = 0; i < virtualNodesPerPhysicalNode; i++) {
            RingPosition position = new RingPosition(
                    hash(nodeId + "#" + i),
                    nodeId,
                    i
            );

            positions.add(position);
        }

        // Prepare positions first; then publish the registration.
        for (RingPosition position : positions) {
            ring.put(position, nodeId);
        }

        nodePositions.put(nodeId, positions);
    }

    public synchronized boolean removeNode(String nodeId) {
        List<RingPosition> positions = nodePositions.remove(nodeId);

        if (positions == null) {
            return false;
        }

        for (RingPosition position : positions) {
            ring.remove(position);
        }

        return true;
    }

    public synchronized String findNode(String key) {
        if (key == null) {
            throw new IllegalArgumentException("Key cannot be null");
        }

        if (ring.isEmpty()) {
            throw new IllegalStateException("No nodes are available");
        }

        long keyHash = hash(key);

        // Empty node ID sorts before every valid node ID.
        RingPosition searchPosition =
                new RingPosition(keyHash, "", -1);

        Map.Entry<RingPosition, String> entry =
                ring.ceilingEntry(searchPosition);

        if (entry == null) {
            entry = ring.firstEntry();
        }

        return entry.getValue();
    }

    public synchronized Set<String> getNodes() {
        return Set.copyOf(nodePositions.keySet());
    }

    private static void validateNodeId(String nodeId) {
        if (nodeId == null || nodeId.isBlank()) {
            throw new IllegalArgumentException(
                    "Node ID must not be blank"
            );
        }
    }

    private static long hash(String value) {
        try {
            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            byte[] bytes = digest.digest(
                    value.getBytes(StandardCharsets.UTF_8)
            );

            long result = 0;

            for (int i = 0; i < 8; i++) {
                result = (result << 8) | (bytes[i] & 0xffL);
            }

            return result & Long.MAX_VALUE;

        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(
                    "SHA-256 is unavailable",
                    e
            );
        }
    }

    private record RingPosition(
            long hash,
            String nodeId,
            int virtualNodeIndex
    ) implements Comparable<RingPosition> {

        @Override
        public int compareTo(RingPosition other) {
            int result = Long.compare(hash, other.hash);

            if (result != 0) {
                return result;
            }

            result = nodeId.compareTo(other.nodeId);

            if (result != 0) {
                return result;
            }

            return Integer.compare(
                    virtualNodeIndex,
                    other.virtualNodeIndex
            );
        }
    }
}

```

### Why use a composite ring position?

Instead of using only a `long` as the map key, we use:

```
RingPosition(hash, nodeId, virtualNodeIndex)
```

The comparison order is:

1. Compare the hash.
2. If the hashes match, compare the node IDs.
3. If those also match, compare the virtual-node indexes.

This gives every registered virtual node a deterministic position, even if two virtual nodes have the same hash value.

For lookup, we construct a search position with the key's hash and an empty node ID. Since we reject blank node IDs, this position sorts before all valid node positions with the same hash. Consequently, `ceilingEntry()` finds the first eligible position at that hash or later.

This is one valid collision-handling strategy. Other implementations might store a bucket of virtual nodes at each hash value instead.

### What does `removeNode()` do?

This line retrieves the positions belonging to the physical node:

```
List<RingPosition> positions = nodePositions.remove(nodeId);
```

Then:

```
for (RingPosition position : positions) {
    ring.remove(position);
}
```

Every virtual-node position is removed, so the ring no longer assigns any ownership range to that physical node.

The method returns `false` if the node wasn't registered and `true` if it was removed.

### A subtle implementation detail

The example builds the positions before inserting them, but its synchronization is only local to one Java object. A production implementation should also validate that the registration is published atomically to readers, handle failures during updates, and coordinate membership versions across the cluster.

For larger systems, immutable ring snapshots swapped atomically into service instances can be a better approach than modifying a shared ring in place.

## 4. What happens to keys when a node is removed?

Let's visualize a simplified ring:

```
Before removal:

100 → A
250 → B
500 → C
750 → A
900 → B
```

Suppose B is removed. Its positions at 250 and 900 disappear.

```
After removal:

100 → A
500 → C
750 → A
```

Consider the keys:

| Key hash | Before removal | After removal |
| -------- | -------------- | ------------- |
| 120      | B              | C             |
| 300      | C              | C             |
| 600      | A              | A             |
| 800      | B              | A             |
| 950      | A              | A             |

The first and fourth keys change owners. The others remain assigned to their previous owners.

The example is deliberately small to make the ownership rule visible. A real ring with virtual nodes contains many more positions, and removing a physical node can affect multiple separate ranges.

Removing a node changes the calculated owner. It does not move the underlying data from the old node to the new one.

If this ring routes database records, data migration must be coordinated separately.

## 5. How do we prove that consistent hashing reduces remapping?

We can test it empirically.

Suppose we generate 100,000 keys and record their owners across a cluster of three nodes. Then we add a fourth node and count how many keys changed owners.

The measurement is:

\\[ \text{Remapping rate} = \frac{\text{Keys with changed owners}} {\text{Total keys}} \times 100\\% \\]

For an idealized consistent-hashing ring with equally capable nodes and a well-distributed hash space, adding one node to \\(N\\) existing nodes is expected to move approximately:

\\[ \frac{1}{N+1} \\]

of the keys.

For three existing nodes:

\\[ \frac{1}{3+1} = 25\\% \\]

This is an expectation, not a guarantee for a particular finite ring. The actual result depends on virtual-node count and placement.

With ordinary modulo hashing, changing from three to four nodes would be expected to remap roughly \\(75\\%\\) of keys under a uniform distribution, because only about one quarter of keys retain the same remainder-based owner.

### Important distinction

A consistent-hashing test should measure changed owners, not just the number of ring positions inserted or removed.

Adding 100 virtual nodes does not mean exactly 100 keys move. Each virtual-node position represents a range containing potentially many keys.

## 6. What this implementation still doesn't solve

Our implementation is safer, but it remains an in-memory placement library.

| Concern                                    | Status                    |
| ------------------------------------------ | ------------------------- |
| Deterministic key lookup                   | Supported                 |
| Multiple virtual nodes                     | Supported                 |
| Duplicate physical-node registration       | Rejected                  |
| Node removal                               | Supported                 |
| Ring-position collisions                   | Deterministically ordered |
| Concurrent access within one instance      | Synchronized              |
| Atomic membership changes across instances | Not supported             |
| Data migration                             | Not supported             |
| Node health detection                      | Not supported             |
| Replication and failover                   | Not supported             |
| Ownership fencing                          | Not supported             |

In a distributed system, each application instance might hold its own ring. You must ensure those instances agree on the active membership configuration, especially during scaling and failure recovery.

Also, the sample's SHA-256 hashing implementation creates a new `MessageDigest` for each call. That's easy to understand and correct for this example, but a high-throughput implementation may need optimized hashing and benchmarking.

## 7. A production scenario: replacing a failed node

Suppose Node B fails.

A dangerous approach is:

1. Remove B from the ring immediately.
2. Route all its keys to new owners.
3. Assume those owners have the data.

That can produce missing reads or lost writes.

A safer process separates placement from recovery:

\#chatgpt-mermaid-\_r_4b3\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_4b3\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_4b3\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_4b3\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_4b3\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4b3\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_4b3\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_4b3\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_4b3\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_4b3\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_4b3\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_4b3\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4b3\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4b3\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_4b3\_ p{margin:0;}#chatgpt-mermaid-\_r_4b3\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4b3\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4b3\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4b3\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_4b3\_ .label text,#chatgpt-mermaid-\_r_4b3\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4b3\_ .node rect,#chatgpt-mermaid-\_r_4b3\_ .node circle,#chatgpt-mermaid-\_r_4b3\_ .node ellipse,#chatgpt-mermaid-\_r_4b3\_ .node polygon,#chatgpt-mermaid-\_r_4b3\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_4b3\_ .rough-node .label text,#chatgpt-mermaid-\_r_4b3\_ .node .label text,#chatgpt-mermaid-\_r_4b3\_ .image-shape .label,#chatgpt-mermaid-\_r_4b3\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_4b3\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_4b3\_ .rough-node .label,#chatgpt-mermaid-\_r_4b3\_ .node .label,#chatgpt-mermaid-\_r_4b3\_ .image-shape .label,#chatgpt-mermaid-\_r_4b3\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_4b3\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_4b3\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4b3\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4b3\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_4b3\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_4b3\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_4b3\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_4b3\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_4b3\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_4b3\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_4b3\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4b3\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4b3\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_4b3\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4b3\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_4b3\_ .icon-shape,#chatgpt-mermaid-\_r_4b3\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_4b3\_ .icon-shape p,#chatgpt-mermaid-\_r_4b3\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_4b3\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_4b3\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_4b3\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_4b3\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_4b3\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_4b3\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_4b3\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_4b3\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_4b3\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4b3\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_4b3\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_4b3\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_4b3\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4b3\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_4b3\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_4b3\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4b3\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_4b3\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_4b3\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4b3\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_4b3\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4b3\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_4b3\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_4b3\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_4b3\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_4b3\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_4b3\_ .node rect,#chatgpt-mermaid-\_r_4b3\_ .node circle,#chatgpt-mermaid-\_r_4b3\_ .node ellipse,#chatgpt-mermaid-\_r_4b3\_ .node polygon,#chatgpt-mermaid-\_r_4b3\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_4b3\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_4b3\_ .node.mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_4b3\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_4b3\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4b3\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}Detect Node B failureDetermine whether B is trulyunavailableChoose replacement orrecovery nodesRestore or replicate affecteddataVerify data and ownershipreadinessPublish a new membershipversionRoute requests using thenew versionRetire old ownership safely

The actual sequence depends on the storage system. A replicated database may promote an eligible replica; a cache may simply rebuild data; a partitioned database may require a controlled shard-recovery procedure.

The central lesson is that membership changes, data availability, and authoritative ownership are separate concerns that must be coordinated.

## 8. Interview-ready explanation

If asked how you would implement consistent hashing with node addition and removal, you can say:

“I would maintain a sorted ring of virtual-node positions, mapping each position to its physical node. To add a node, I would generate deterministic virtual-node identifiers and insert their positions. To remove a node, I would remove every position registered for that physical node. A lookup uses a ceiling search to find the first position at or after the key hash, wrapping to the beginning if necessary. I would handle collisions explicitly, reject duplicate registration, and test remapping behavior. In production, I would also version membership configuration and coordinate data migration, ownership changes, and failover separately.”

That's a strong answer because it covers both the algorithm and the operational boundary around it.

## Next lesson: Testing and measuring consistent hashing

We'll build a repeatable test harness to compare modulo hashing with consistent hashing, measure key distribution and remapping, examine virtual-node counts, and understand how to interpret the results without being misled by a small sample.

Type `next` to continue.















# Chapter 2: Implementing Consistent Hashing

## Lesson 4: Testing Distribution, Remapping, and Performance

We've now implemented a ring with virtual nodes and support for adding and removing physical nodes.

But how do we know whether the implementation actually works well?

In production, we shouldn't choose a virtual-node count or a hashing strategy based on intuition alone. We should measure the behavior we care about.

We'll examine three important properties:

1. Distribution: How evenly are keys assigned to nodes?
2. Remapping: How many keys change owners when membership changes?
3. Performance: How quickly can the system calculate an owner?

These metrics answer different questions. A system can have excellent lookup performance but poor distribution, or balanced distribution but excessive remapping.

## 1. Metric one: distribution

Suppose we have four nodes and 100,000 keys.

An ideally balanced distribution would look like this:

| Node | Expected keys |
| ---- | ------------- |
| A    | 25,000        |
| B    | 25,000        |
| C    | 25,000        |
| D    | 25,000        |

But real hashing results won't necessarily be exactly equal.

You might observe:

| Node | Actual keys |
| ---- | ----------- |
| A    | 23,900      |
| B    | 26,100      |
| C    | 24,700      |
| D    | 25,300      |

This is reasonably balanced for many workloads, although whether it is acceptable depends on the system.

Now imagine:

| Node | Actual keys |
| ---- | ----------- |
| A    | 10,000      |
| B    | 15,000      |
| C    | 25,000      |
| D    | 50,000      |

Node D holds half the keys. That might cause serious problems if storage consumption and request volume correlate with key count.

### Measure the maximum-to-average ratio

One useful metric is:

\\[ \text{Imbalance ratio} = \frac{\text{Maximum keys on any node}} {\text{Average keys per node}} \\]

For the second distribution:

\\[ \text{Average} = \frac{100000}{4}=25000 \\]

\\[ \text{Imbalance ratio} = \frac{50000}{25000}=2 \\]

The most heavily loaded node owns twice the average number of keys.

An imbalance ratio of 1 means perfect balance by key count. Higher values indicate greater imbalance.

However, this metric measures key count, not actual workload. If one key generates a million requests and another generates one request, key counts alone can be misleading.

## 2. Metric two: remapping

Suppose we have four nodes and 100,000 keys.

We record each key's owner, add a fifth node, and calculate ownership again.

For every key:

```
Old owner: Node A
New owner: Node A
→ Not remapped

Old owner: Node B
New owner: Node E
→ Remapped
```

The remapping rate is:

\\[ R = \frac{\text{Number of keys whose owners changed}} {\text{Total number of keys}} \\]

For example, if 20,500 keys move:

\\[ R = \frac{20500}{100000} = 20.5\\% \\]

For a well-distributed consistent-hashing ring with five equally capable nodes after the addition, we'd expect approximately 20% of keys to move in the idealized model.

Why? The new node should acquire roughly one-fifth of the key space.

By contrast, modulo hashing with five nodes after changing from four typically remaps about 80% of keys, assuming uniformly distributed keys and the usual `hash(key) % nodeCount` mapping.

This is one of the most important advantages of consistent hashing.

Don't confuse remapping with data movement. The remapping metric tells us how many keys have new calculated owners. It does not measure the bytes copied, the time taken to migrate data, or the impact on live traffic.

## 3. Build a simple measurement harness

Let's create a small Java program to measure owner distribution and remapping.

The code below uses a simple consistent-hashing implementation with virtual nodes. It is intended for measurement and learning, not production use.

```

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

public class ConsistentHashExperiment {

    private static final int VIRTUAL_NODES = 100;
    private static final int KEY_COUNT = 100_000;

    static class Ring {

        private final NavigableMap<Long, String> ring =
                new TreeMap<>();

        void addNode(String nodeId) {
            for (int i = 0; i < VIRTUAL_NODES; i++) {
                long position = hash(nodeId + "#" + i);
                ring.put(position, nodeId);
            }
        }

        String findNode(String key) {
            if (ring.isEmpty()) {
                throw new IllegalStateException("Empty ring");
            }

            long keyHash = hash(key);

            Map.Entry<Long, String> entry =
                    ring.ceilingEntry(keyHash);

            return entry != null
                    ? entry.getValue()
                    : ring.firstEntry().getValue();
        }
    }

    static long hash(String value) {
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));

            long result = 0;

            for (int i = 0; i < 8; i++) {
                result = (result << 8) | (bytes[i] & 0xffL);
            }

            return result & Long.MAX_VALUE;

        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    public static void main(String[] args) {

        Ring ring = new Ring();

        ring.addNode("A");
        ring.addNode("B");
        ring.addNode("C");
        ring.addNode("D");

        Map<String, String> oldOwners = new HashMap<>();
        Map<String, Integer> counts = new HashMap<>();

        for (int i = 0; i < KEY_COUNT; i++) {
            String key = "customer:" + i;
            String owner = ring.findNode(key);

            oldOwners.put(key, owner);
            counts.merge(owner, 1, Integer::sum);
        }

        System.out.println("Distribution before adding E:");
        counts.forEach((node, count) ->
                System.out.println(node + ": " + count));

        ring.addNode("E");

        int remapped = 0;

        for (int i = 0; i < KEY_COUNT; i++) {
            String key = "customer:" + i;
            String newOwner = ring.findNode(key);

            if (!oldOwners.get(key).equals(newOwner)) {
                remapped++;
            }
        }

        System.out.printf(
                "Remapped keys: %d / %d (%.2f%%)%n",
                remapped,
                KEY_COUNT,
                100.0 * remapped / KEY_COUNT
        );
    }
}

```

### What should you observe?

Run the experiment and inspect:

- The number of keys assigned to each original node.
- The percentage of keys remapped after adding Node E.
- Whether the remapping percentage is reasonably close to the expected value.

Don't expect exact equality. A finite number of virtual nodes creates a finite approximation of the ideal ring.

Also, this simplified harness has the same collision limitation as our earlier basic implementation: ring positions are stored under a single `Long` key, so a hash-position collision can overwrite an entry. A rigorous implementation should reuse the collision-aware position representation from the previous lesson.

## 4. Why virtual-node count affects distribution

Let's repeat the experiment with different virtual-node counts.

| Virtual nodes per node | General expectation                                       |
| ---------------------- | --------------------------------------------------------- |
| 1                      | Potentially large imbalance                               |
| 10                     | More ownership ranges, often improved balance             |
| 100                    | More opportunities for ranges to average out              |
| 1,000                  | More metadata and diminishing returns may become relevant |

The important statistical intuition is that each physical node owns multiple intervals. As the number of independently distributed positions increases, the sum of those intervals tends to become more stable relative to its average.

But there is a tradeoff.

More virtual nodes mean:

- More ring entries.
- More metadata to distribute.
- More work when changing membership.
- Potentially more ownership ranges to migrate.

The correct number should be chosen using experiments representative of the expected cluster size and workload.

### A subtle point about the experiment

Our harness counts keys generated as `"customer:0"`, `"customer:1"`, and so on.

That is a deterministic dataset, not a mathematical proof of uniform distribution. The hash function may distribute this dataset well, but real identifiers and workload patterns can differ.

Test representative keys, and test actual request traffic separately.

## 5. Metric three: lookup performance

The ring's lookup operation is approximately:

\\[ O(\log M) \\]

where \\(M\\) is the total number of virtual-node positions.

If you have 100 physical nodes with 100 virtual nodes each:

\\[ M = 100 \times 100 = 10{,}000 \\]

A lookup searches a sorted structure with 10,000 positions.

The lookup cost grows logarithmically with the number of positions rather than linearly.

However, measuring only the `TreeMap` search is insufficient. In our sample, hashing also creates a SHA-256 digest and allocates objects.

A realistic benchmark should distinguish:

1. Hash calculation time.
2. Ring lookup time.
3. End-to-end routing overhead.
4. Membership-update time.
5. Memory consumed by ring metadata.

For Java, use a benchmarking tool such as JMH for reliable microbenchmarks rather than relying on a single `System.nanoTime()` measurement.

Warm-up, JIT compilation, garbage collection, and CPU scheduling can all distort naive measurements.

## 6. What should you measure in production?

A distributed cache or database needs more than a key-count experiment.

Consider collecting these metrics:

| Metric                                | What it reveals           |
| ------------------------------------- | ------------------------- |
| Keys per node                         | Ownership distribution    |
| Bytes per node                        | Storage imbalance         |
| Requests per node                     | Traffic imbalance         |
| CPU and memory per node               | Resource pressure         |
| P95 and P99 latency per node          | Uneven performance        |
| Ring version per application instance | Membership disagreement   |
| Data migration backlog                | Rebalancing progress      |
| Migration failures                    | Data-movement problems    |
| Rejected stale-owner requests         | Routing metadata problems |
| Hot-key request counts                | Workload concentration    |

This is especially important in a multi-region system.

A region may have a perfectly balanced ring but still be overloaded because it serves more users, receives more traffic, or hosts a few unusually hot tenants.

Consistent hashing balances placement within the eligible node set. It does not automatically balance traffic across regions or enforce residency policy.

## 7. Common testing mistakes

Mistake 1: Testing only one cluster size.

An algorithm that looks balanced with four nodes might behave differently with 100 nodes. Test the range of cluster sizes you expect to support.

Mistake 2: Measuring only key count.

Equal key counts do not imply equal storage or traffic. Measure workload characteristics.

Mistake 3: Measuring remapping but not migration.

A 20% remapping rate might still mean moving several terabytes of data. Estimate bytes moved and the impact on live traffic.

Mistake 4: Assuming one experiment proves correctness.

Use automated tests for ring lookup, wrap-around, membership changes, collisions, and deterministic behavior.

Mistake 5: Ignoring concurrent membership changes.

A local test can pass while multiple application instances use different ring versions. Distributed membership needs its own tests.

## 8. Interview-ready summary

If an interviewer asks, “How would you validate a consistent-hashing implementation?”, answer:

“I would measure distribution, remapping, and lookup performance separately. For distribution, I'd inspect keys and bytes per node, not just average key count. For remapping, I'd record ownership before and after a node change and calculate the percentage of keys that changed owners. I'd compare the result with the expected behavior, then benchmark hashing and lookup separately. In production, I'd also monitor traffic imbalance, hot keys, ring-version disagreement, and migration backlog because consistent hashing alone doesn't guarantee balanced workload or safe data movement.”

## Next lesson

We'll examine how consistent hashing compares with rendezvous hashing in implementation terms, including lookup complexity, membership changes, weighted placement, and when you might choose one algorithm over the other in a real backend system.

Type `next` to continue.




















# Chapter 2: Implementing Consistent Hashing

## Lesson 5: Consistent Hashing vs. Rendezvous Hashing — Implementation and Design Trade-offs

In Chapter 1, we learned the conceptual difference between consistent hashing and rendezvous hashing. Now let's examine how they behave when you implement them in a real distributed system.

Both algorithms solve a similar problem:

> Given a key and a set of eligible nodes, determine which node should own that key while minimizing unnecessary remapping when membership changes.

But they solve it differently.

- Consistent hashing: Place nodes on a ring and find the first eligible position clockwise from the key.
- Rendezvous hashing: Score each eligible node for the key and select the node with the highest score.

Let's work through both approaches.

## 1. Consistent hashing: ring-based lookup

Suppose we have four cache nodes:

```
Node A
Node B
Node C
Node D
```

We hash the nodes, or their virtual-node identifiers, to positions on a ring.

For a given key:

1. Hash the key.
2. Find the first ring position at or after the key's hash.
3. Wrap around if necessary.
4. Return the physical node associated with that position.

\#chatgpt-mermaid-\_r_4ds\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_4ds\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_4ds\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_4ds\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_4ds\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4ds\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_4ds\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_4ds\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_4ds\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_4ds\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_4ds\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_4ds\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4ds\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4ds\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_4ds\_ p{margin:0;}#chatgpt-mermaid-\_r_4ds\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4ds\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4ds\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4ds\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_4ds\_ .label text,#chatgpt-mermaid-\_r_4ds\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4ds\_ .node rect,#chatgpt-mermaid-\_r_4ds\_ .node circle,#chatgpt-mermaid-\_r_4ds\_ .node ellipse,#chatgpt-mermaid-\_r_4ds\_ .node polygon,#chatgpt-mermaid-\_r_4ds\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_4ds\_ .rough-node .label text,#chatgpt-mermaid-\_r_4ds\_ .node .label text,#chatgpt-mermaid-\_r_4ds\_ .image-shape .label,#chatgpt-mermaid-\_r_4ds\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_4ds\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_4ds\_ .rough-node .label,#chatgpt-mermaid-\_r_4ds\_ .node .label,#chatgpt-mermaid-\_r_4ds\_ .image-shape .label,#chatgpt-mermaid-\_r_4ds\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_4ds\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_4ds\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4ds\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4ds\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_4ds\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_4ds\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_4ds\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_4ds\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_4ds\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_4ds\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_4ds\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4ds\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4ds\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_4ds\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4ds\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_4ds\_ .icon-shape,#chatgpt-mermaid-\_r_4ds\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_4ds\_ .icon-shape p,#chatgpt-mermaid-\_r_4ds\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_4ds\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_4ds\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_4ds\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_4ds\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_4ds\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_4ds\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_4ds\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_4ds\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_4ds\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4ds\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_4ds\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_4ds\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_4ds\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4ds\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_4ds\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_4ds\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4ds\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_4ds\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_4ds\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4ds\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_4ds\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4ds\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_4ds\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_4ds\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_4ds\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_4ds\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_4ds\_ .node rect,#chatgpt-mermaid-\_r_4ds\_ .node circle,#chatgpt-mermaid-\_r_4ds\_ .node ellipse,#chatgpt-mermaid-\_r_4ds\_ .node polygon,#chatgpt-mermaid-\_r_4ds\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_4ds\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_4ds\_ .node.mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_4ds\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_4ds\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4ds\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}Input keyHash keyBinary search on sorted ringFind clockwise successorReturn physical owner

If the ring contains \\(M\\) virtual-node positions, lookup using a balanced sorted structure such as Java's `TreeMap` takes approximately:

\\[ O(\log M) \\]

Adding a physical node requires generating its virtual-node positions and inserting them into the ring.

This is particularly useful when the system has a large number of keys and the node membership changes relatively infrequently.

## 2. Rendezvous hashing: score every eligible node

Rendezvous hashing is also called highest-random-weight hashing.

Instead of building a ring, we calculate a score for each key-node pair.

For a key `customer:101` and three nodes, the conceptual process is:

```
score("customer:101", "node-A") = 0.28
score("customer:101", "node-B") = 0.91
score("customer:101", "node-C") = 0.47
```

The highest score belongs to Node B, so Node B owns the key.

These numbers are illustrative, not actual hash outputs.

The algorithm is straightforward:

\#chatgpt-mermaid-\_r_4ea\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_4ea\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_4ea\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_4ea\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_4ea\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4ea\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_4ea\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_4ea\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_4ea\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_4ea\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_4ea\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_4ea\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4ea\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4ea\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_4ea\_ p{margin:0;}#chatgpt-mermaid-\_r_4ea\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4ea\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4ea\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4ea\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_4ea\_ .label text,#chatgpt-mermaid-\_r_4ea\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4ea\_ .node rect,#chatgpt-mermaid-\_r_4ea\_ .node circle,#chatgpt-mermaid-\_r_4ea\_ .node ellipse,#chatgpt-mermaid-\_r_4ea\_ .node polygon,#chatgpt-mermaid-\_r_4ea\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_4ea\_ .rough-node .label text,#chatgpt-mermaid-\_r_4ea\_ .node .label text,#chatgpt-mermaid-\_r_4ea\_ .image-shape .label,#chatgpt-mermaid-\_r_4ea\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_4ea\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_4ea\_ .rough-node .label,#chatgpt-mermaid-\_r_4ea\_ .node .label,#chatgpt-mermaid-\_r_4ea\_ .image-shape .label,#chatgpt-mermaid-\_r_4ea\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_4ea\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_4ea\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4ea\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4ea\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_4ea\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_4ea\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_4ea\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_4ea\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_4ea\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_4ea\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_4ea\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4ea\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4ea\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_4ea\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4ea\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_4ea\_ .icon-shape,#chatgpt-mermaid-\_r_4ea\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_4ea\_ .icon-shape p,#chatgpt-mermaid-\_r_4ea\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_4ea\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_4ea\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_4ea\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_4ea\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_4ea\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_4ea\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_4ea\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_4ea\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_4ea\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4ea\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_4ea\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_4ea\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_4ea\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4ea\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_4ea\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_4ea\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4ea\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_4ea\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_4ea\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4ea\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_4ea\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4ea\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_4ea\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_4ea\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_4ea\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_4ea\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_4ea\_ .node rect,#chatgpt-mermaid-\_r_4ea\_ .node circle,#chatgpt-mermaid-\_r_4ea\_ .node ellipse,#chatgpt-mermaid-\_r_4ea\_ .node polygon,#chatgpt-mermaid-\_r_4ea\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_4ea\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_4ea\_ .node.mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_4ea\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_4ea\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4ea\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}Input keyEnumerate eligible nodesCalculate score for eachkey-node pairSelect node with highestscoreReturn owner

For \\(N\\) eligible nodes, a straightforward implementation takes:

\\[ O(N) \\]

per lookup, because it scores every node.

That can be perfectly reasonable when the node set is small. With a large cluster and extremely high request rates, scoring every node on every lookup may be expensive.

## 3. Implement rendezvous hashing in Java

Here is a basic implementation using SHA-256 to produce deterministic scores.

```

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;

public final class RendezvousHashing {

    private RendezvousHashing() {}

    public static String findNode(
            String key,
            Collection<String> nodeIds
    ) {
        if (key == null) {
            throw new IllegalArgumentException(
                    "Key cannot be null"
            );
        }

        if (nodeIds == null || nodeIds.isEmpty()) {
            throw new IllegalArgumentException(
                    "At least one node is required"
            );
        }

        String bestNode = null;
        long bestScore = 0;

        for (String nodeId : nodeIds) {
            if (nodeId == null || nodeId.isBlank()) {
                throw new IllegalArgumentException(
                        "Node IDs must not be blank"
                );
            }

            long score = score(key, nodeId);

            if (bestNode == null
                    || Long.compareUnsigned(score, bestScore) > 0) {
                bestNode = nodeId;
                bestScore = score;
            }
        }

        return bestNode;
    }

    private static long score(String key, String nodeId) {
        try {
            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            digest.update(
                    key.getBytes(StandardCharsets.UTF_8)
            );
            digest.update((byte) 0);
            digest.update(
                    nodeId.getBytes(StandardCharsets.UTF_8)
            );

            byte[] bytes = digest.digest();

            return ByteBuffer.wrap(bytes).getLong();

        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(
                    "SHA-256 is unavailable",
                    e
            );
        }
    }

    public static void main(String[] args) {
        var nodes = java.util.List.of(
                "node-A",
                "node-B",
                "node-C"
        );

        System.out.println(
                findNode("customer:101", nodes)
        );
    }
}

```

### Understand the implementation

For each node, we calculate:

```
score(key, nodeId)
```

The key and node ID are both included in the input. This matters because we want different key-node pairs to produce different scores.

The separator byte prevents simple concatenation ambiguities such as:

```
key = "ab", node = "c"
key = "a",  node = "bc"
```

Without an unambiguous encoding, both pairs could produce the same concatenated byte sequence.

We then compare scores as unsigned 64-bit values:

```
Long.compareUnsigned(score, bestScore)
```

This treats the 64-bit result as a value from \\(0\\) to \\(2^{64}-1\\), rather than interpreting half the values as negative signed Java `long` numbers.

### Is this production-ready?

It is a useful learning implementation, but it needs more engineering before production use:

- Membership should be represented by a stable, versioned node set.
- The hash function and input encoding must be identical across all instances.
- Duplicate node IDs should be rejected or normalized.
- The implementation should avoid creating a new `MessageDigest` for every node if performance measurements show that overhead matters.
- A benchmark should compare its full lookup cost with the ring implementation.

For applications where placement affects durable data, node membership and ownership transitions also need a separate coordination and migration protocol.

## 4. Why rendezvous hashing has an interesting property

Suppose we have three nodes:

```
A, B, C
```

For each key, we calculate the score for all three and select the highest.

Now add Node D.

What changes?

For a given key, the scores for A, B, and C remain unchanged, assuming the hashing algorithm and node identifiers remain unchanged.

The only new candidate is D.

Therefore:

- If D's score is not the highest, the owner remains unchanged.
- If D's score is higher than all the existing scores, the key moves to D.

This makes the remapping behavior intuitive.

With \\(N\\) equally weighted nodes and uniformly distributed independent scores, the new node wins approximately:

\\[ \frac{1}{N+1} \\]

of keys after being added to a cluster of \\(N\\) nodes.

That is the same idealized fraction we expect to move when adding one equally capable node to a well-balanced consistent-hashing ring.

However, neither algorithm guarantees that an arbitrary finite dataset will achieve the exact expected percentage.

## 5. Side-by-side comparison

| Property                    | Consistent hashing                       | Rendezvous hashing                                |
| --------------------------- | ---------------------------------------- | ------------------------------------------------- |
| Basic mechanism             | Sorted ring                              | Score every eligible node                         |
| Straightforward lookup cost | \\(O(\log M)\\)                          | \\(O(N)\\)                                        |
| Requires virtual nodes      | Commonly used for balance                | No                                                |
| Node removal                | Remove its ring positions                | Exclude it from candidates                        |
| Adding a node               | Some ring intervals change owner         | New node wins for keys where its score is highest |
| Weighting                   | Multiple positions or weighted placement | Weighted scoring methods                          |
| Memory per routing instance | Ring metadata                            | Node list and any scoring metadata                |
| Natural fit                 | Large keyspaces and many lookups         | Small or moderate node sets                       |
| Data migration              | Must be implemented separately           | Must be implemented separately                    |
| Replication and consistency | Not provided by the algorithm            | Not provided by the algorithm                     |

Here, \\(M\\) is the number of virtual-node positions, and \\(N\\) is the number of eligible physical nodes.

Do not compare the complexity figures without considering constants and workload. A ring with thousands of virtual-node positions has a larger search structure, while rendezvous hashing repeatedly calculates scores for the eligible nodes.

The actual winner depends on the cluster size, hash implementation, caching, request rate, and membership-update frequency.

## 6. Which should you choose for a backend system?

Let's compare three scenarios.

### Scenario A: Distributed cache with hundreds of nodes

You need to route millions of cache requests per second across a large cluster.

A ring-based approach with virtual nodes can be a good fit because owner lookup requires a search through the ring rather than scoring every physical node.

You still need to manage membership updates, cache misses during rebalancing, and node health.

### Scenario B: Service with ten storage workers

You have a small, relatively stable set of workers, and routing simplicity is important.

Rendezvous hashing may be attractive because the implementation is straightforward and doesn't require maintaining a ring of virtual positions.

Benchmark it before choosing it for a very high-throughput path.

### Scenario C: Multi-region application

Your application operates in India, Europe, and the United States.

A user requests data that must remain within a particular residency boundary.

Neither algorithm should blindly choose from every node worldwide.

Instead:

\#chatgpt-mermaid-\_r_4ff\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_4ff\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_4ff\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_4ff\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_4ff\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4ff\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_4ff\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_4ff\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_4ff\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_4ff\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_4ff\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_4ff\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4ff\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4ff\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_4ff\_ p{margin:0;}#chatgpt-mermaid-\_r_4ff\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4ff\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4ff\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4ff\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_4ff\_ .label text,#chatgpt-mermaid-\_r_4ff\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4ff\_ .node rect,#chatgpt-mermaid-\_r_4ff\_ .node circle,#chatgpt-mermaid-\_r_4ff\_ .node ellipse,#chatgpt-mermaid-\_r_4ff\_ .node polygon,#chatgpt-mermaid-\_r_4ff\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_4ff\_ .rough-node .label text,#chatgpt-mermaid-\_r_4ff\_ .node .label text,#chatgpt-mermaid-\_r_4ff\_ .image-shape .label,#chatgpt-mermaid-\_r_4ff\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_4ff\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_4ff\_ .rough-node .label,#chatgpt-mermaid-\_r_4ff\_ .node .label,#chatgpt-mermaid-\_r_4ff\_ .image-shape .label,#chatgpt-mermaid-\_r_4ff\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_4ff\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_4ff\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4ff\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4ff\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_4ff\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_4ff\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_4ff\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_4ff\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_4ff\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_4ff\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_4ff\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4ff\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4ff\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_4ff\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4ff\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_4ff\_ .icon-shape,#chatgpt-mermaid-\_r_4ff\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_4ff\_ .icon-shape p,#chatgpt-mermaid-\_r_4ff\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_4ff\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_4ff\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_4ff\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_4ff\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_4ff\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_4ff\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_4ff\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_4ff\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_4ff\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4ff\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_4ff\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_4ff\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_4ff\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4ff\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_4ff\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_4ff\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4ff\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_4ff\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_4ff\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4ff\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_4ff\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4ff\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_4ff\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_4ff\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_4ff\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_4ff\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_4ff\_ .node rect,#chatgpt-mermaid-\_r_4ff\_ .node circle,#chatgpt-mermaid-\_r_4ff\_ .node ellipse,#chatgpt-mermaid-\_r_4ff\_ .node polygon,#chatgpt-mermaid-\_r_4ff\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_4ff\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_4ff\_ .node.mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_4ff\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_4ff\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4ff\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}Incoming requestResolve tenant anddata-residency policyDetermine eligible regionsChoose an appropriateregionSelect eligible nodes withinthat regionUse consistent hashing orrendezvous hashingValidate ownership andexecute request

You can use either algorithm within a region.

If a region fails, choosing another region is not merely a routing change. You must establish whether the destination has the necessary data, whether it is permitted to serve the tenant, and whether it is authorized to accept writes.

## 7. Advanced consideration: weighted rendezvous hashing

What if the nodes have different capacities?

Suppose:

```
Node A: capacity 1
Node B: capacity 1
Node C: capacity 4
```

A naive rendezvous algorithm gives each node approximately the same probability of winning, so Node C receives about one-third of the keys, not four times as many as either smaller node.

Weighted rendezvous methods modify the scoring procedure so that a node's capacity influences its probability of selection.

A common mathematical approach uses a uniform value \\(u\\) derived from the key-node hash and an exponential-race score:

\\[ s = \frac{-\ln(u)}{w} \\]

where \\(w\\) is the positive node weight. Choose the node with the smallest score.

Equivalently, choose the node with the largest value of:

\\[ \frac{w}{-\ln(u)} \\]

These formulations describe the same ordering.

With suitable independent uniform values and positive weights, a node's probability of winning is proportional to its weight.

This is an important distinction from merely multiplying an arbitrary hash value by the node's capacity, which does not generally produce the desired weighted distribution.

As always, weighted ownership is not equivalent to weighted request load: a few hot keys can still dominate a node's traffic.

## 8. Interview questions and answers

Q1. Why does rendezvous hashing minimize remapping when a node is added?

Because each existing key-node score stays the same. The new node can become the owner only if its score beats the existing winner.

Q2. Why might you choose consistent hashing over rendezvous hashing?

For a large cluster with a very high lookup rate, ring-based lookup can be more efficient because it avoids scoring every node for every key. Virtual nodes can also provide fine-grained control over ownership distribution.

Q3. Why might rendezvous hashing be simpler?

It doesn't require constructing and maintaining a sorted ring or generating multiple virtual-node positions. Given the same key and membership list, it simply selects the highest-scoring eligible node.

Q4. Does rendezvous hashing solve replication?

No. It identifies an owner. Replication requires a separate mechanism to choose replica nodes, maintain copies, and define consistency and failure behavior.

Q5. Can either algorithm enforce GDPR or data residency?

No. Policy enforcement must determine which regions and nodes are eligible before placement. Hashing is not a compliance control.

## Key takeaways

- Consistent hashing selects an owner using a sorted ring.
- Rendezvous hashing selects an owner by comparing key-node scores.
- Both can minimize remapping when membership changes.
- Consistent hashing often offers efficient lookup for large clusters.
- Rendezvous hashing can be simpler for small or moderate node sets.
- Neither algorithm automatically migrates data, replicates records, guarantees consistency, or handles regional failover.

Next lesson: We'll move from the implementation details into Chapter 3: evolving from a single-region application to a multi-region architecture. We'll explore where regional routing fits, why hashing globally is often the wrong first step, and how to separate region selection from node selection.

Type `next` to continue.

