# Consistent Hashing in a Multi-Region Architecture

The most important thing to understand is this:

Consistent hashing helps decide where data or requests should go. It does not, by itself, solve multi-region replication, consistency, low latency, failover, or GDPR compliance.

In a multi-region architecture, you need to combine consistent hashing with geographic routing, data ownership, replication policies, and residency rules.

Let's build this from the fundamentals to a production-ready design.

## 1. First, understand what consistent hashing solves

Imagine you have an application running in three regions:

- `ap-south-1` — Mumbai
- `eu-west-1` — Ireland
- `us-east-1` — Virginia

Your application stores millions of customer records and uses Redis to cache them.

A naive approach might distribute keys using:

\\[ \text{server} = \text{hash(key)} \bmod N \\]

For example:

```
customer:101 → Server 1
customer:102 → Server 3
customer:103 → Server 2
```

The problem appears when you add or remove a server.

If \\(N\\) changes from 3 to 4, the modulo result changes for many keys. You may need to remap a large portion of the cache, causing cache misses, increased database load, and higher latency.

Consistent hashing reduces this remapping.

But in a multi-region setup, there is a more important question:

Should a customer's data be routed to a server near the user, or to the region where that customer's data is legally allowed to reside?

Those two goals can conflict. We must solve that conflict before choosing the hashing strategy.

## 2. How consistent hashing works internally

Consistent hashing maps both data keys and servers onto a logical ring.

Imagine a hash function produces values in the range from 0 to \\(2^{32}-1\\).

```
                     0
                 .-------.
              .-'         '-.
            /                 \
           |    Node A         |
           |                   |
           |          Node B   |
            \                 /
              '-.         .-'
                 '-------'
                   Node C
```

The ring wraps around: after the largest hash value, it returns to zero.

### Step 1: Hash each server

```
hash("server-A") = 100
hash("server-B") = 400
hash("server-C") = 700
```

These numbers represent positions on the ring.

### Step 2: Hash each data key

```
hash("customer:101") = 250
hash("customer:102") = 650
hash("customer:103") = 900
```

### Step 3: Walk clockwise to find the owner

Each key belongs to the first server encountered clockwise from the key's position, wrapping around if necessary.

| Data key       | Hash position | Owner                           |
| -------------- | ------------- | ------------------------------- |
| `customer:101` | 250           | Server B at 400                 |
| `customer:102` | 650           | Server C at 700                 |
| `customer:103` | 900           | Server A at 100, after wrapping |

If Server B disappears, only the keys that depended on Server B need to move to their next eligible owner.

This is the key advantage: adding or removing a node generally remaps only a fraction of the keys, rather than nearly all of them.

### Virtual nodes

Real systems often place multiple virtual nodes (vnodes) on the ring for each physical server.

```
Server A → A1, A2, A3, A4
Server B → B1, B2, B3, B4
Server C → C1, C2, C3, C4
```

This improves distribution and helps account for servers with different capacities.

However, virtual nodes don't make every distribution perfectly uniform, and they don't solve cross-region consistency.

## 3. The critical distinction: a global ring versus regional rings

This is where multi-region designs often go wrong.

There are three different ways you might apply consistent hashing.

### Approach A: One global ring

All servers from all regions participate in the same hash ring.

\#chatgpt-mermaid-\_r_34d\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_34d\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_34d\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_34d\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_34d\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_34d\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_34d\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_34d\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_34d\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_34d\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_34d\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_34d\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_34d\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_34d\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_34d\_ p{margin:0;}#chatgpt-mermaid-\_r_34d\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_34d\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_34d\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_34d\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_34d\_ .label text,#chatgpt-mermaid-\_r_34d\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_34d\_ .node rect,#chatgpt-mermaid-\_r_34d\_ .node circle,#chatgpt-mermaid-\_r_34d\_ .node ellipse,#chatgpt-mermaid-\_r_34d\_ .node polygon,#chatgpt-mermaid-\_r_34d\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_34d\_ .rough-node .label text,#chatgpt-mermaid-\_r_34d\_ .node .label text,#chatgpt-mermaid-\_r_34d\_ .image-shape .label,#chatgpt-mermaid-\_r_34d\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_34d\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_34d\_ .rough-node .label,#chatgpt-mermaid-\_r_34d\_ .node .label,#chatgpt-mermaid-\_r_34d\_ .image-shape .label,#chatgpt-mermaid-\_r_34d\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_34d\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_34d\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_34d\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_34d\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_34d\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_34d\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_34d\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_34d\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_34d\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_34d\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_34d\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_34d\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_34d\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_34d\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_34d\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_34d\_ .icon-shape,#chatgpt-mermaid-\_r_34d\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_34d\_ .icon-shape p,#chatgpt-mermaid-\_r_34d\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_34d\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_34d\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_34d\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_34d\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_34d\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_34d\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_34d\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_34d\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_34d\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_34d\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_34d\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_34d\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_34d\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_34d\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_34d\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_34d\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_34d\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_34d\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_34d\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_34d\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_34d\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_34d\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_34d\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_34d\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_34d\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_34d\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_34d\_ .node rect,#chatgpt-mermaid-\_r_34d\_ .node circle,#chatgpt-mermaid-\_r_34d\_ .node ellipse,#chatgpt-mermaid-\_r_34d\_ .node polygon,#chatgpt-mermaid-\_r_34d\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_34d\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_34d\_ .node.mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_34d\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_34d\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_34d\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}User in EuropeHash customer keyGlobal hash ringServer in IrelandServer in MumbaiServer in Virginia

Suppose `customer:101` hashes to a server in Mumbai.

A request from Europe might therefore travel to Mumbai, even if the user is much closer to Ireland.

Advantages

- Global distribution of keys.
- Efficient redistribution when nodes are added or removed.
- Useful when the entire dataset can be accessed from every region.

Disadvantages

- The hash ring does not understand network distance.
- It does not guarantee that requests go to the closest region.
- It can violate data residency requirements if EU customer records are placed outside the EU.
- A regional outage can affect keys whose owners are in that region.

A global ring can work for globally accessible, non-sensitive cache entries, but it is usually not the right top-level routing strategy for residency-constrained customer data.

### Approach B: Select a region first, then hash within that region

This is the recommended default for applications with both low-latency and data-residency requirements.

The routing process becomes:

1. Identify the customer's permitted data region.
2. Route the request to an application deployment in that region.
3. Use consistent hashing within the selected region to choose a cache node, partition, or shard.
4. Read or write the authoritative datastore according to its consistency policy.

\#chatgpt-mermaid-\_r_34m\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_34m\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_34m\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_34m\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_34m\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_34m\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_34m\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_34m\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_34m\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_34m\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_34m\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_34m\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_34m\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_34m\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_34m\_ p{margin:0;}#chatgpt-mermaid-\_r_34m\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_34m\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_34m\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_34m\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_34m\_ .label text,#chatgpt-mermaid-\_r_34m\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_34m\_ .node rect,#chatgpt-mermaid-\_r_34m\_ .node circle,#chatgpt-mermaid-\_r_34m\_ .node ellipse,#chatgpt-mermaid-\_r_34m\_ .node polygon,#chatgpt-mermaid-\_r_34m\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_34m\_ .rough-node .label text,#chatgpt-mermaid-\_r_34m\_ .node .label text,#chatgpt-mermaid-\_r_34m\_ .image-shape .label,#chatgpt-mermaid-\_r_34m\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_34m\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_34m\_ .rough-node .label,#chatgpt-mermaid-\_r_34m\_ .node .label,#chatgpt-mermaid-\_r_34m\_ .image-shape .label,#chatgpt-mermaid-\_r_34m\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_34m\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_34m\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_34m\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_34m\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_34m\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_34m\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_34m\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_34m\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_34m\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_34m\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_34m\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_34m\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_34m\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_34m\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_34m\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_34m\_ .icon-shape,#chatgpt-mermaid-\_r_34m\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_34m\_ .icon-shape p,#chatgpt-mermaid-\_r_34m\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_34m\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_34m\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_34m\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_34m\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_34m\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_34m\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_34m\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_34m\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_34m\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_34m\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_34m\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_34m\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_34m\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_34m\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_34m\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_34m\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_34m\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_34m\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_34m\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_34m\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_34m\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_34m\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_34m\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_34m\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_34m\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_34m\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_34m\_ .node rect,#chatgpt-mermaid-\_r_34m\_ .node circle,#chatgpt-mermaid-\_r_34m\_ .node ellipse,#chatgpt-mermaid-\_r_34m\_ .node polygon,#chatgpt-mermaid-\_r_34m\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_34m\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_34m\_ .node.mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_34m\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_34m\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_34m\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}Incoming requestIdentify tenant and residencypolicyPermitted regionEU deploymentIndia deploymentUS deploymentRegional hash ringRegional hash ringRegional hash ringEU cache and dataIndia cache and dataUS cache and data

Now, the hash ring only distributes work among nodes that are already permitted to handle the data.

For example:

```
Tenant A
  Residency: EU
  Application region: EU
  Cache ring: EU-only nodes
  Authoritative database: EU

Tenant B
  Residency: India
  Application region: India
  Cache ring: India-only nodes
  Authoritative database: India
```

The important point is that residency is a hard constraint, not a preference that consistent hashing can override.

### Approach C: A primary region per tenant, with local read replicas

Another common design assigns each tenant or data partition a home region.

\#chatgpt-mermaid-\_r_354\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_354\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_354\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_354\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_354\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_354\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_354\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_354\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_354\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_354\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_354\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_354\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_354\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_354\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_354\_ p{margin:0;}#chatgpt-mermaid-\_r_354\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_354\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_354\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_354\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_354\_ .label text,#chatgpt-mermaid-\_r_354\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_354\_ .node rect,#chatgpt-mermaid-\_r_354\_ .node circle,#chatgpt-mermaid-\_r_354\_ .node ellipse,#chatgpt-mermaid-\_r_354\_ .node polygon,#chatgpt-mermaid-\_r_354\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_354\_ .rough-node .label text,#chatgpt-mermaid-\_r_354\_ .node .label text,#chatgpt-mermaid-\_r_354\_ .image-shape .label,#chatgpt-mermaid-\_r_354\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_354\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_354\_ .rough-node .label,#chatgpt-mermaid-\_r_354\_ .node .label,#chatgpt-mermaid-\_r_354\_ .image-shape .label,#chatgpt-mermaid-\_r_354\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_354\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_354\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_354\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_354\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_354\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_354\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_354\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_354\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_354\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_354\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_354\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_354\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_354\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_354\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_354\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_354\_ .icon-shape,#chatgpt-mermaid-\_r_354\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_354\_ .icon-shape p,#chatgpt-mermaid-\_r_354\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_354\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_354\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_354\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_354\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_354\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_354\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_354\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_354\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_354\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_354\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_354\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_354\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_354\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_354\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_354\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_354\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_354\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_354\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_354\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_354\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_354\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_354\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_354\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_354\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_354\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_354\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_354\_ .node rect,#chatgpt-mermaid-\_r_354\_ .node circle,#chatgpt-mermaid-\_r_354\_ .node ellipse,#chatgpt-mermaid-\_r_354\_ .node polygon,#chatgpt-mermaid-\_r_354\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_354\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_354\_ .node.mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_354\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_354\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_354\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}EU userEU applicationEU user elsewhereEU primary databasePermitted regional readreplicaEU local cacheAsync replication

The primary region owns writes. Other permitted regions may serve replicated reads.

This can simplify conflict handling and maintain a single writer per tenant, but it adds latency whenever a request must reach the primary from another region.

It also does not automatically solve GDPR: a replica is another copy of personal data, and its location and access rules must be allowed by the applicable policy.

## 4. How to configure routing correctly

Don't make the hash function responsible for every routing decision. Separate the decisions into layers.

| Layer                  | Responsibility                           | Recommended rule                               |
| ---------------------- | ---------------------------------------- | ---------------------------------------------- |
| Residency policy       | Where data may be stored and processed   | Enforce permitted regions                      |
| Global traffic routing | Where the request enters the system      | Choose a healthy, low-latency permitted region |
| Tenant routing         | Which region owns the tenant             | Use an authoritative tenant-to-region mapping  |
| Consistent hashing     | Which node or partition handles the key  | Hash within the eligible regional pool         |
| Database routing       | Which datastore handles reads and writes | Follow ownership and consistency rules         |
| Failover               | What happens when a region fails         | Select only an eligible recovery region        |

### A subtle but important point about user location

Suppose a German customer travels to India.

The application can serve static content or non-sensitive computation from India, provided your policies allow it. But the customer's residency-constrained personal data must still be accessed and processed according to the applicable rules.

Do not automatically change the tenant's home region just because the user's IP address changes.

Likewise, do not use IP geolocation as the authoritative source for residency. Use a trusted tenant or account policy, backed by controlled administrative changes.

## 5. GDPR and data residency: what must be enforced?

There is an important distinction between GDPR compliance and strict data residency.

GDPR does not universally require every EU resident's personal data to remain physically inside the EU. Transfers outside the EEA are subject to additional legal requirements, which can include an adequacy decision or appropriate safeguards. Other laws, sector-specific rules, and customer contracts may impose stricter residency requirements.&#x20;

[image](https://www.google.com/s2/favicons?domain=https://commission.europa.eu\&sz=32)

European Commission

+2



For your architecture, treat residency as a policy that the system enforces technically.

### Recommended tenant metadata

Maintain an authoritative tenant registry with information similar to this:

```
{
  "tenantId": "tenant-123",
  "homeRegion": "eu-west-1",
  "allowedRegions": ["eu-west-1", "eu-central-1"],
  "residencyPolicy": "EU_ONLY",
  "policyVersion": 12
}
```

This is illustrative configuration, not a complete legal policy.

The registry should be protected from unauthorized changes, and changes to residency rules should be audited.

### Enforce the policy at multiple layers

| Component                  | Required control                                                            |
| -------------------------- | --------------------------------------------------------------------------- |
| API gateway                | Route requests to an eligible regional deployment                           |
| Application                | Validate tenant region and processing permissions                           |
| Consistent hashing         | Select nodes only from the permitted regional pool                          |
| Primary database           | Store authoritative records in an approved location                         |
| Replication                | Replicate only to approved destinations                                     |
| Cache                      | Keep personal data within permitted regions                                 |
| Backups                    | Ensure backup locations and recovery destinations are permitted             |
| Logs and traces            | Avoid leaking personal data into globally centralized observability systems |
| Encryption keys            | Apply appropriate key-access and regional controls                          |
| Support and administration | Control remote access to personal data, including access across borders     |

A critical detail: keeping the database in Europe does not automatically mean the data stays in Europe. Logs, backups, support access, analytics pipelines, and replicated caches can introduce additional data flows. Remote access may also count as a transfer in relevant circumstances.&#x20;

[image](https://www.google.com/s2/favicons?domain=https://www.edps.europa.eu\&sz=32)

European Data Protection Supervisor

+1



For example, don't put complete customer records into globally replicated Redis merely because your database is regionally restricted.

Instead, use regional caches:

```
EU tenant
  ├── EU application
  ├── EU Redis
  ├── EU database
  ├── EU backups
  └── EU-approved replication destinations
```

If the EU region fails, do not automatically fail over to India or the US unless the relevant policy permits that destination and the associated processing and transfers are lawful.

You may need to sacrifice availability for a tenant if every permitted region is unavailable. That is a deliberate compliance-versus-availability decision.

## 6. How to achieve low latency without breaking residency

The key is to optimize latency within the set of permitted destinations.

Use the following order of operations:

1. Determine the tenant's authoritative residency policy.
2. Determine which regions are allowed and healthy.
3. Route to the lowest-latency eligible region.
4. Route within that region using consistent hashing.
5. Access the local cache and database whenever possible.
6. Fall back to another region only if it is permitted and meets the required consistency guarantees.

This means you optimize for:

\\[ \min\_{r \in R\_{\text{eligible}}} \operatorname{Latency}(u,r) \\]

where \\(R\_{\text{eligible}}\\) contains only regions permitted by the tenant's policy and currently eligible for the requested operation.

This is more correct than simply choosing the geographically closest region.

### What about users moving between regions?

Suppose a user normally accesses your application from Germany but travels to Singapore.

- The user can connect to a nearby application endpoint if that endpoint is permitted to process the request.
- The application can route permitted operations to the user's home region.
- It should not silently move the user's records to Singapore.
- If the home region is unavailable, the application should select an approved recovery region or return a controlled error.

If a user's tenant changes residency, migrate the data through a controlled workflow: copy and validate the records, establish the new authoritative owner, update routing metadata, revoke old access, and handle old copies and backups according to the applicable retention and deletion requirements.

## 7. Consistent hashing is not database replication

This is another important distinction for system design interviews.

Consider these responsibilities:

| Mechanism                             | What it solves                                             |
| ------------------------------------- | ---------------------------------------------------------- |
| Consistent hashing                    | Assigns keys to nodes or shards                            |
| Geographic routing                    | Chooses a suitable entry region                            |
| Database replication                  | Copies changes between database replicas                   |
| Consensus or distributed coordination | Coordinates decisions and ownership when required          |
| Conflict resolution                   | Handles concurrent modifications                           |
| Failover orchestration                | Moves traffic and possibly write ownership                 |
| Residency enforcement                 | Restricts where data can be stored, processed, or accessed |

You need to design these mechanisms separately.

For example, AWS DynamoDB Global Tables provides managed cross-region replication. Its multi-region eventual consistency mode allows low-latency local operations but can expose stale data in another region and resolves concurrent conflicting updates using last-writer-wins. Its multi-region strong consistency mode has additional regional and operational constraints and can introduce cross-region latency.&#x20;

[image](https://www.google.com/s2/favicons?domain=https://docs.aws.amazon.com\&sz=32)

Amazon DynamoDB

+1



If you're using PostgreSQL, Redis, or another datastore, don't assume that adding a consistent-hashing library provides any of these replication guarantees.

## 8. What happens during a regional outage?

A production design needs explicit behavior for failure.

Suppose the EU application region becomes unavailable.

\#chatgpt-mermaid-\_r_37e\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_37e\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_37e\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_37e\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_37e\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_37e\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_37e\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_37e\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_37e\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_37e\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_37e\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_37e\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_37e\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_37e\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_37e\_ p{margin:0;}#chatgpt-mermaid-\_r_37e\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_37e\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_37e\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_37e\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_37e\_ .label text,#chatgpt-mermaid-\_r_37e\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_37e\_ .node rect,#chatgpt-mermaid-\_r_37e\_ .node circle,#chatgpt-mermaid-\_r_37e\_ .node ellipse,#chatgpt-mermaid-\_r_37e\_ .node polygon,#chatgpt-mermaid-\_r_37e\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_37e\_ .rough-node .label text,#chatgpt-mermaid-\_r_37e\_ .node .label text,#chatgpt-mermaid-\_r_37e\_ .image-shape .label,#chatgpt-mermaid-\_r_37e\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_37e\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_37e\_ .rough-node .label,#chatgpt-mermaid-\_r_37e\_ .node .label,#chatgpt-mermaid-\_r_37e\_ .image-shape .label,#chatgpt-mermaid-\_r_37e\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_37e\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_37e\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_37e\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_37e\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_37e\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_37e\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_37e\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_37e\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_37e\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_37e\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_37e\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_37e\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_37e\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_37e\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_37e\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_37e\_ .icon-shape,#chatgpt-mermaid-\_r_37e\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_37e\_ .icon-shape p,#chatgpt-mermaid-\_r_37e\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_37e\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_37e\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_37e\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_37e\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_37e\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_37e\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_37e\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_37e\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_37e\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_37e\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_37e\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_37e\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_37e\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_37e\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_37e\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_37e\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_37e\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_37e\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_37e\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_37e\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_37e\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_37e\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_37e\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_37e\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_37e\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_37e\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_37e\_ .node rect,#chatgpt-mermaid-\_r_37e\_ .node circle,#chatgpt-mermaid-\_r_37e\_ .node ellipse,#chatgpt-mermaid-\_r_37e\_ .node polygon,#chatgpt-mermaid-\_r_37e\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_37e\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_37e\_ .node.mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_37e\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_37e\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_37e\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}EU requestEU region healthy?Use EU application and dataApproved recovery regionavailable?Activate approved recoverypathControlled failure ordegraded serviceValidate write ownership anddata freshnessServe according toconsistency policyYesNoYesNo

The recovery procedure should:

- Detect regional failures and distinguish them from temporary network problems.
- Prevent two regions from independently claiming exclusive write ownership.
- Ensure the recovery region has the required data and is legally permitted.
- Change routing and write ownership in a controlled manner.
- Reconcile or replay outstanding changes where necessary.
- Prevent stale cache entries from being mistaken for authoritative data.
- Monitor replication lag, failover time, error rates, and recovery objectives.

For a single-writer database, you may need a fencing mechanism or other reliable ownership protocol to prevent the old primary from accepting writes after failover.

Consistent hashing can redistribute keys when nodes change, but it cannot determine whether the old region is truly dead or whether its data is safe to replace.

## 9. A practical production architecture

For a multi-tenant SaaS platform running on AWS, I would start with this design:

\#chatgpt-mermaid-\_r_37n\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_37n\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_37n\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_37n\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_37n\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_37n\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_37n\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_37n\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_37n\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_37n\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_37n\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_37n\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_37n\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_37n\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_37n\_ p{margin:0;}#chatgpt-mermaid-\_r_37n\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_37n\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_37n\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_37n\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_37n\_ .label text,#chatgpt-mermaid-\_r_37n\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_37n\_ .node rect,#chatgpt-mermaid-\_r_37n\_ .node circle,#chatgpt-mermaid-\_r_37n\_ .node ellipse,#chatgpt-mermaid-\_r_37n\_ .node polygon,#chatgpt-mermaid-\_r_37n\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_37n\_ .rough-node .label text,#chatgpt-mermaid-\_r_37n\_ .node .label text,#chatgpt-mermaid-\_r_37n\_ .image-shape .label,#chatgpt-mermaid-\_r_37n\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_37n\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_37n\_ .rough-node .label,#chatgpt-mermaid-\_r_37n\_ .node .label,#chatgpt-mermaid-\_r_37n\_ .image-shape .label,#chatgpt-mermaid-\_r_37n\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_37n\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_37n\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_37n\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_37n\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_37n\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_37n\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_37n\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_37n\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_37n\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_37n\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_37n\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_37n\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_37n\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_37n\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_37n\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_37n\_ .icon-shape,#chatgpt-mermaid-\_r_37n\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_37n\_ .icon-shape p,#chatgpt-mermaid-\_r_37n\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_37n\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_37n\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_37n\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_37n\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_37n\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_37n\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_37n\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_37n\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_37n\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_37n\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_37n\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_37n\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_37n\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_37n\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_37n\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_37n\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_37n\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_37n\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_37n\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_37n\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_37n\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_37n\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_37n\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_37n\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_37n\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_37n\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_37n\_ .node rect,#chatgpt-mermaid-\_r_37n\_ .node circle,#chatgpt-mermaid-\_r_37n\_ .node ellipse,#chatgpt-mermaid-\_r_37n\_ .node polygon,#chatgpt-mermaid-\_r_37n\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_37n\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_37n\_ .node.mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_37n\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_37n\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_37n\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}Global usersGlobal traffic routingTenant registry and policyEligible regionEU application clusterIndia application clusterUS application clusterEU consistent hash ringIndia consistent hash ringUS consistent hash ringEU cache and datastoreIndia cache and datastoreUS cache and datastoreEU recovery replicaApproved replication only

### Recommended configuration principles

1\. Use separate regional hash rings.\
Never let a global ring accidentally select a node outside the permitted data region.

2\. Use stable, well-defined keys.\
For example, `tenantId:customerId` or another domain-specific partition key. Don't include a changing IP address or user location in the key.

3\. Use virtual nodes or a suitable weighted hashing algorithm.\
This helps distribute load across unequal nodes. For highly dynamic systems, evaluate rendezvous hashing as an alternative.

4\. Keep authoritative ownership separate from cache placement.\
Moving a cache key to another node should not silently change the authoritative database owner.

5\. Choose replication based on the consistency requirement.\
Eventual consistency may work for cached product catalogs or dashboards. Financial transactions, inventory reservations, and other sensitive workflows may require stronger coordination or single-writer ownership.

6\. Treat regional recovery as an explicit policy.\
A region is eligible for failover only when it satisfies both technical and regulatory requirements.

7\. Use infrastructure as code and automated policy checks.\
Validate regional resources, backup destinations, replication configuration, encryption keys, and network paths in CI/CD. Fail deployments that violate residency restrictions.

8\. Test the failure cases.\
Simulate a node joining or leaving the ring, a region becoming isolated, replication lag increasing, a tenant changing residency, and a recovery region becoming unavailable.

## 10. How I would decide what to use

| Requirement                                      | Recommended starting point                                                                                          |
| ------------------------------------------------ | ------------------------------------------------------------------------------------------------------------------- |
| Global CDN or static content                     | Geographic routing and edge caching                                                                                 |
| Regional Redis cache                             | Separate consistent-hashing ring per region                                                                         |
| Multi-tenant SaaS with strict residency          | Tenant-to-region registry plus regional data planes                                                                 |
| PostgreSQL with one authoritative writer         | Regional primary, local replicas where permitted, controlled failover                                               |
| Globally distributed key-value workload          | Evaluate managed multi-region databases such as DynamoDB Global Tables                                              |
| Strongly consistent cross-region updates         | Evaluate consensus-based or managed strong-consistency options, accepting their latency and availability trade-offs |
| Strict residency with regional disaster recovery | Region-specific backups and an approved recovery-region allowlist                                                   |

AWS's multi-region guidance emphasizes that geographic distance introduces replication latency and that consistency requirements must drive the architecture, rather than treating multi-region deployment as a simple infrastructure change.&#x20;

[image](https://www.google.com/s2/favicons?domain=https://docs.aws.amazon.com\&sz=32)

AWS Prescriptive Guidance

+1



## The final mental model

Think of the architecture as four independent decisions:

- Policy: Where is this data allowed to live?
- Routing: Which eligible region should handle this request?
- Hashing: Which node inside that region should handle the key?
- Consistency: How do writes, replicas, and failures behave?

If you get these boundaries right, consistent hashing becomes a useful component of a multi-region design instead of a mechanism that accidentally creates latency, data leaks, or split-brain failures.

The best default for a residency-sensitive multi-region SaaS application is policy-first regional routing, regional consistent hashing, local data access, and explicitly controlled cross-region replication and failover.
