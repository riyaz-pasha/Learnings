# **Chapter 7 — Building a Distributed Rate Limiter from Scratch**

Now we are moving from understanding individual algorithms to designing a real production system.

This is where we combine everything:

- Rate-limiting algorithms.
- Redis.
- Multiple backend instances.
- Atomic operations.
- Distributed coordination.
- Failure handling.
- Scalability.

We'll build the architecture incrementally rather than jumping directly into a complicated distributed design.

Our running example will be an API platform with multiple users, tenants, and backend instances.

## **1. Start with the simplest possible system**

Imagine we have built a REST API:

```
GET /api/products
```

Our requirement:

```
Maximum 5 requests per 10 seconds per user.
```

We decide to use Token Bucket.

Our configuration:

```
Capacity = 5 tokens
Refill rate = 0.5 tokens/second
Request cost = 1 token
```

Let's initially assume that our entire application runs on one server.

\#chatgpt-mermaid-\_r_2pc\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_2pc\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_2pc\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_2pc\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_2pc\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2pc\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_2pc\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_2pc\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_2pc\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_2pc\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_2pc\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_2pc\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2pc\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2pc\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_2pc\_ p{margin:0;}#chatgpt-mermaid-\_r_2pc\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2pc\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2pc\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2pc\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_2pc\_ .label text,#chatgpt-mermaid-\_r_2pc\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2pc\_ .node rect,#chatgpt-mermaid-\_r_2pc\_ .node circle,#chatgpt-mermaid-\_r_2pc\_ .node ellipse,#chatgpt-mermaid-\_r_2pc\_ .node polygon,#chatgpt-mermaid-\_r_2pc\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_2pc\_ .rough-node .label text,#chatgpt-mermaid-\_r_2pc\_ .node .label text,#chatgpt-mermaid-\_r_2pc\_ .image-shape .label,#chatgpt-mermaid-\_r_2pc\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_2pc\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_2pc\_ .rough-node .label,#chatgpt-mermaid-\_r_2pc\_ .node .label,#chatgpt-mermaid-\_r_2pc\_ .image-shape .label,#chatgpt-mermaid-\_r_2pc\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_2pc\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_2pc\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2pc\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2pc\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_2pc\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_2pc\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_2pc\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_2pc\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_2pc\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_2pc\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_2pc\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2pc\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2pc\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_2pc\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2pc\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_2pc\_ .icon-shape,#chatgpt-mermaid-\_r_2pc\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_2pc\_ .icon-shape p,#chatgpt-mermaid-\_r_2pc\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_2pc\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_2pc\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_2pc\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_2pc\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_2pc\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_2pc\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_2pc\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_2pc\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_2pc\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2pc\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_2pc\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_2pc\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_2pc\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2pc\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_2pc\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_2pc\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2pc\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_2pc\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_2pc\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2pc\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_2pc\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2pc\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_2pc\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_2pc\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_2pc\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_2pc\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_2pc\_ .node rect,#chatgpt-mermaid-\_r_2pc\_ .node circle,#chatgpt-mermaid-\_r_2pc\_ .node ellipse,#chatgpt-mermaid-\_r_2pc\_ .node polygon,#chatgpt-mermaid-\_r_2pc\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_2pc\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_2pc\_ .mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_2pc\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_2pc\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2pc\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}ClientBackend ServerRate LimiterAllowed?Business LogicHTTP 429ResponseYesNo

The Rate Limiter maintains an in-memory state:

```
user_123:

    available_tokens = 3
    last_refill_time = ...
```

Whenever a request arrives:

1. Identify the user.
2. Read the user's token bucket.
3. Calculate newly generated tokens.
4. Check whether sufficient tokens exist.
5. Deduct tokens if allowed.
6. Continue to business logic.

Everything works!

We could implement this using an ordinary Java `ConcurrentHashMap`, a Python dictionary with appropriate synchronization, or another in-memory data structure.

But now let's introduce the first distributed-systems problem.

# **2. Why does an in-memory Rate Limiter fail?**

Our application becomes popular.

We horizontally scale it.

Instead of one backend instance, we now have three.

\#chatgpt-mermaid-\_r_2pn\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_2pn\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_2pn\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_2pn\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_2pn\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2pn\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_2pn\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_2pn\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_2pn\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_2pn\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_2pn\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_2pn\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2pn\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2pn\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_2pn\_ p{margin:0;}#chatgpt-mermaid-\_r_2pn\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2pn\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2pn\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2pn\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_2pn\_ .label text,#chatgpt-mermaid-\_r_2pn\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2pn\_ .node rect,#chatgpt-mermaid-\_r_2pn\_ .node circle,#chatgpt-mermaid-\_r_2pn\_ .node ellipse,#chatgpt-mermaid-\_r_2pn\_ .node polygon,#chatgpt-mermaid-\_r_2pn\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_2pn\_ .rough-node .label text,#chatgpt-mermaid-\_r_2pn\_ .node .label text,#chatgpt-mermaid-\_r_2pn\_ .image-shape .label,#chatgpt-mermaid-\_r_2pn\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_2pn\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_2pn\_ .rough-node .label,#chatgpt-mermaid-\_r_2pn\_ .node .label,#chatgpt-mermaid-\_r_2pn\_ .image-shape .label,#chatgpt-mermaid-\_r_2pn\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_2pn\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_2pn\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2pn\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2pn\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_2pn\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_2pn\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_2pn\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_2pn\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_2pn\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_2pn\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_2pn\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2pn\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2pn\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_2pn\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2pn\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_2pn\_ .icon-shape,#chatgpt-mermaid-\_r_2pn\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_2pn\_ .icon-shape p,#chatgpt-mermaid-\_r_2pn\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_2pn\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_2pn\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_2pn\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_2pn\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_2pn\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_2pn\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_2pn\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_2pn\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_2pn\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2pn\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_2pn\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_2pn\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_2pn\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2pn\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_2pn\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_2pn\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2pn\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_2pn\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_2pn\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2pn\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_2pn\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2pn\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_2pn\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_2pn\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_2pn\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_2pn\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_2pn\_ .node rect,#chatgpt-mermaid-\_r_2pn\_ .node circle,#chatgpt-mermaid-\_r_2pn\_ .node ellipse,#chatgpt-mermaid-\_r_2pn\_ .node polygon,#chatgpt-mermaid-\_r_2pn\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_2pn\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_2pn\_ .mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_2pn\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_2pn\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2pn\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}ClientsLoad BalancerBackend Server 1Backend Server 2Backend Server 3

We still want the same policy:

```
user_123:

Maximum 5 requests per 10 seconds.
```

But what happens to the in-memory state?

Each application server has its own memory.

```
Server 1:
    user_123 = 5 tokens

Server 2:
    user_123 = 5 tokens

Server 3:
    user_123 = 5 tokens
```

These are three independent buckets.

They don't know about each other.

### **2.1 Let's see the problem**

Suppose the user sends six requests.

The load balancer distributes them like this:

```
Request 1 -> Server 1
Request 2 -> Server 2
Request 3 -> Server 3
Request 4 -> Server 1
Request 5 -> Server 2
Request 6 -> Server 3
```

Each server sees only two requests.

Their local buckets become:

```
Server 1:
    5 - 2 = 3 tokens

Server 2:
    5 - 2 = 3 tokens

Server 3:
    5 - 2 = 3 tokens
```

All six requests were allowed.

But our policy permits only five requests in the initial burst.

We unintentionally multiplied our rate limit by three.

In general:

\\[ ActualAllowance \approx N \times ConfiguredAllowance \\]

Where \\(N\\) is the number of independently maintained application instances, assuming requests can consume the allowance on each instance.

For 20 application instances:

```
Desired burst = 100

Potential aggregate burst = 2,000
```

That's a serious correctness problem.

### **2.2 What if we use sticky sessions?**

You might suggest:

> Let's configure the load balancer so that requests from one user always go to the same application server.

This is called session affinity or sticky sessions.

For example:

```
user_123 -> Server 1

user_456 -> Server 2

user_789 -> Server 3
```

Now all requests from `user_123` reach Server 1.

It appears that the problem is solved.

But consider what happens when:

- Server 1 crashes.
- Server 1 is replaced during deployment.
- The load balancer changes its routing.
- The user moves to another region.
- Autoscaling adds or removes instances.

The rate-limit state is still local to Server 1.

When that state disappears, the user might get a fresh allowance.

Sticky sessions don't provide durable, shared rate-limit state.

They are not a reliable solution to this problem.

### **2.3 What if we replicate the state between application servers?**

We could attempt to synchronize the in-memory counters across servers.

But now we need to solve:

- State replication.
- Concurrent updates.
- Conflict resolution.
- Network partitions.
- Server failures.
- Synchronization delays.
- Which server owns a particular bucket.

We're effectively implementing a distributed data store inside our application servers.

That's unnecessary complexity for a rate limiter.

We need a simpler approach.

# **3. Introduce shared state using Redis**

The observation is straightforward:

**Every application instance must consult and update the same rate-limit state.**

Instead of maintaining a separate bucket inside every server, let's introduce a shared datastore.

Redis is a common choice because it provides:

- Very fast key-value access.
- Atomic operations.
- Sorted Sets, Hashes, and other useful data structures.
- Lua scripts for atomic multi-operation logic.
- Expiration using TTL.
- Replication and clustering options.

Our architecture becomes:

\#chatgpt-mermaid-\_r_2qm\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_2qm\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_2qm\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_2qm\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_2qm\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2qm\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_2qm\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_2qm\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_2qm\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_2qm\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_2qm\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_2qm\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2qm\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2qm\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_2qm\_ p{margin:0;}#chatgpt-mermaid-\_r_2qm\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2qm\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2qm\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2qm\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_2qm\_ .label text,#chatgpt-mermaid-\_r_2qm\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2qm\_ .node rect,#chatgpt-mermaid-\_r_2qm\_ .node circle,#chatgpt-mermaid-\_r_2qm\_ .node ellipse,#chatgpt-mermaid-\_r_2qm\_ .node polygon,#chatgpt-mermaid-\_r_2qm\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_2qm\_ .rough-node .label text,#chatgpt-mermaid-\_r_2qm\_ .node .label text,#chatgpt-mermaid-\_r_2qm\_ .image-shape .label,#chatgpt-mermaid-\_r_2qm\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_2qm\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_2qm\_ .rough-node .label,#chatgpt-mermaid-\_r_2qm\_ .node .label,#chatgpt-mermaid-\_r_2qm\_ .image-shape .label,#chatgpt-mermaid-\_r_2qm\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_2qm\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_2qm\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2qm\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2qm\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_2qm\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_2qm\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_2qm\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_2qm\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_2qm\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_2qm\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_2qm\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2qm\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2qm\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_2qm\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2qm\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_2qm\_ .icon-shape,#chatgpt-mermaid-\_r_2qm\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_2qm\_ .icon-shape p,#chatgpt-mermaid-\_r_2qm\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_2qm\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_2qm\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_2qm\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_2qm\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_2qm\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_2qm\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_2qm\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_2qm\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_2qm\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2qm\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_2qm\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_2qm\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_2qm\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2qm\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_2qm\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_2qm\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2qm\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_2qm\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_2qm\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2qm\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_2qm\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2qm\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_2qm\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_2qm\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_2qm\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_2qm\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_2qm\_ .node rect,#chatgpt-mermaid-\_r_2qm\_ .node circle,#chatgpt-mermaid-\_r_2qm\_ .node ellipse,#chatgpt-mermaid-\_r_2qm\_ .node polygon,#chatgpt-mermaid-\_r_2qm\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_2qm\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_2qm\_ .mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_2qm\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_2qm\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2qm\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}ClientsLoad BalancerBackend Server 1Backend Server 2Backend Server 3Shared RedisBusiness Logic

Now every server uses the same Redis state.

For example:

```
Redis:

rate_limit:products:user_123

tokens = 3
last_refill_time = ...
```

Let's follow our six requests again.

Initially:

```
Redis:

user_123 = 5 tokens
```

Request distribution:

```
Request 1 -> Server 1
Request 2 -> Server 2
Request 3 -> Server 3
Request 4 -> Server 1
Request 5 -> Server 2
Request 6 -> Server 3
```

But each server accesses the same Redis key.

The state changes:

```
Request 1:
    5 -> 4

Request 2:
    4 -> 3

Request 3:
    3 -> 2

Request 4:
    2 -> 1

Request 5:
    1 -> 0

Request 6:
    0 -> REJECT
```

Correct!

The rate-limit state is no longer tied to an individual application instance.

This is the foundation of our distributed Rate Limiter.

### **What problem did we solve?**

Previously:

```
Each server had an independent allowance.
```

Now:

```
All servers share one allowance.
```

This also means an application server can be replaced without losing the rate-limit state, provided the Redis state remains available.

But there is another problem we need to address.

We have solved shared storage.

We haven't yet solved concurrent access.

# **4. Where should the Rate Limiter actually live?**

Before continuing, let's clarify an important architectural decision.

There are two common ways to integrate a Rate Limiter.

### **Option A: Rate Limiter inside each backend application**

Each application instance contains a rate-limiting middleware or library.

\#chatgpt-mermaid-\_r_2rl\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_2rl\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_2rl\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_2rl\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_2rl\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2rl\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_2rl\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_2rl\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_2rl\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_2rl\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_2rl\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_2rl\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2rl\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2rl\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_2rl\_ p{margin:0;}#chatgpt-mermaid-\_r_2rl\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2rl\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2rl\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2rl\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_2rl\_ .label text,#chatgpt-mermaid-\_r_2rl\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2rl\_ .node rect,#chatgpt-mermaid-\_r_2rl\_ .node circle,#chatgpt-mermaid-\_r_2rl\_ .node ellipse,#chatgpt-mermaid-\_r_2rl\_ .node polygon,#chatgpt-mermaid-\_r_2rl\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_2rl\_ .rough-node .label text,#chatgpt-mermaid-\_r_2rl\_ .node .label text,#chatgpt-mermaid-\_r_2rl\_ .image-shape .label,#chatgpt-mermaid-\_r_2rl\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_2rl\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_2rl\_ .rough-node .label,#chatgpt-mermaid-\_r_2rl\_ .node .label,#chatgpt-mermaid-\_r_2rl\_ .image-shape .label,#chatgpt-mermaid-\_r_2rl\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_2rl\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_2rl\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2rl\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2rl\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_2rl\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_2rl\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_2rl\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_2rl\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_2rl\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_2rl\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_2rl\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2rl\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2rl\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_2rl\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2rl\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_2rl\_ .icon-shape,#chatgpt-mermaid-\_r_2rl\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_2rl\_ .icon-shape p,#chatgpt-mermaid-\_r_2rl\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_2rl\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_2rl\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_2rl\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_2rl\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_2rl\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_2rl\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_2rl\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_2rl\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_2rl\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2rl\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_2rl\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_2rl\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_2rl\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2rl\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_2rl\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_2rl\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2rl\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_2rl\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_2rl\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2rl\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_2rl\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2rl\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_2rl\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_2rl\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_2rl\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_2rl\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_2rl\_ .node rect,#chatgpt-mermaid-\_r_2rl\_ .node circle,#chatgpt-mermaid-\_r_2rl\_ .node ellipse,#chatgpt-mermaid-\_r_2rl\_ .node polygon,#chatgpt-mermaid-\_r_2rl\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_2rl\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_2rl\_ .mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_2rl\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_2rl\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2rl\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}Load BalancerBackend 1 + LimiterBackend 2 + LimiterBackend 3 + LimiterShared Redis

When a request reaches an application instance:

1. Authentication middleware identifies the caller.
2. Rate-limiting middleware determines the policy.
3. It calls Redis.
4. Redis returns the decision.
5. The application either continues or rejects the request.

For example, in Spring Boot, this might be implemented as:

```
HTTP Request
    |
    v
Spring Security Filter
    |
    v
Rate Limiting Filter
    |
    v
Controller
    |
    v
Service
```

In Node.js, the corresponding integration could be Express or Fastify middleware.

**Advantages:**

- No additional network hop to a separate rate-limiter service.
- Straightforward application-specific policies.
- Easy to integrate with authenticated user and tenant information.
- Shared Redis still provides distributed state.

**Disadvantages:**

- Each application may implement the same rate-limiting logic separately.
- Policy changes may require application deployments unless configuration is externalized.
- Different services may implement slightly different behavior.

This is a good choice for many systems.

### **Option B: Dedicated Rate Limiter service**

Instead of embedding all the logic in every application, we create a separate service.

\#chatgpt-mermaid-\_r_2s0\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_2s0\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_2s0\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_2s0\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_2s0\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2s0\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_2s0\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_2s0\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_2s0\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_2s0\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_2s0\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_2s0\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2s0\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2s0\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_2s0\_ p{margin:0;}#chatgpt-mermaid-\_r_2s0\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2s0\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2s0\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2s0\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_2s0\_ .label text,#chatgpt-mermaid-\_r_2s0\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2s0\_ .node rect,#chatgpt-mermaid-\_r_2s0\_ .node circle,#chatgpt-mermaid-\_r_2s0\_ .node ellipse,#chatgpt-mermaid-\_r_2s0\_ .node polygon,#chatgpt-mermaid-\_r_2s0\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_2s0\_ .rough-node .label text,#chatgpt-mermaid-\_r_2s0\_ .node .label text,#chatgpt-mermaid-\_r_2s0\_ .image-shape .label,#chatgpt-mermaid-\_r_2s0\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_2s0\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_2s0\_ .rough-node .label,#chatgpt-mermaid-\_r_2s0\_ .node .label,#chatgpt-mermaid-\_r_2s0\_ .image-shape .label,#chatgpt-mermaid-\_r_2s0\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_2s0\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_2s0\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2s0\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2s0\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_2s0\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_2s0\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_2s0\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_2s0\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_2s0\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_2s0\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_2s0\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2s0\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2s0\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_2s0\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2s0\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_2s0\_ .icon-shape,#chatgpt-mermaid-\_r_2s0\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_2s0\_ .icon-shape p,#chatgpt-mermaid-\_r_2s0\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_2s0\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_2s0\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_2s0\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_2s0\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_2s0\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_2s0\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_2s0\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_2s0\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_2s0\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2s0\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_2s0\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_2s0\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_2s0\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2s0\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_2s0\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_2s0\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2s0\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_2s0\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_2s0\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2s0\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_2s0\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2s0\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_2s0\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_2s0\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_2s0\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_2s0\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_2s0\_ .node rect,#chatgpt-mermaid-\_r_2s0\_ .node circle,#chatgpt-mermaid-\_r_2s0\_ .node ellipse,#chatgpt-mermaid-\_r_2s0\_ .node polygon,#chatgpt-mermaid-\_r_2s0\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_2s0\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_2s0\_ .mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_2s0\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_2s0\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2s0\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}ClientsLoad BalancerAPI Server 1API Server 2API Server 3Rate Limiter ServiceRedis

The backend calls a dedicated service:

```
POST /internal/rate-limit/check
```

Example request:

```
{
  "policyId": "products-api",
  "identity": "user_123",
  "cost": 1
}
```

Example response:

```
{
  "allowed": true,
  "remaining": 4,
  "retryAfterMs": 0
}
```

The service encapsulates:

- Policy lookup.
- Algorithm implementations.
- Redis operations.
- Atomic validation.
- Decision generation.

**Advantages:**

- Centralized rate-limiting logic.
- Consistent behavior across different applications.
- One place for policy management.
- Easier to support many programming languages and services.

**Disadvantages:**

- An additional network hop for every validation.
- The Rate Limiter service becomes critical infrastructure.
- It must itself scale and remain highly available.
- Backend requests depend on another service.

A dedicated service is useful when rate limiting is a shared platform capability across many applications.

### **Which approach should we choose?**

These architectures can even be combined.

For example:

\#chatgpt-mermaid-\_r_2sl\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_2sl\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_2sl\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_2sl\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_2sl\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2sl\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_2sl\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_2sl\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_2sl\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_2sl\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_2sl\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_2sl\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2sl\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2sl\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_2sl\_ p{margin:0;}#chatgpt-mermaid-\_r_2sl\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2sl\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2sl\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2sl\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_2sl\_ .label text,#chatgpt-mermaid-\_r_2sl\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2sl\_ .node rect,#chatgpt-mermaid-\_r_2sl\_ .node circle,#chatgpt-mermaid-\_r_2sl\_ .node ellipse,#chatgpt-mermaid-\_r_2sl\_ .node polygon,#chatgpt-mermaid-\_r_2sl\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_2sl\_ .rough-node .label text,#chatgpt-mermaid-\_r_2sl\_ .node .label text,#chatgpt-mermaid-\_r_2sl\_ .image-shape .label,#chatgpt-mermaid-\_r_2sl\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_2sl\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_2sl\_ .rough-node .label,#chatgpt-mermaid-\_r_2sl\_ .node .label,#chatgpt-mermaid-\_r_2sl\_ .image-shape .label,#chatgpt-mermaid-\_r_2sl\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_2sl\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_2sl\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2sl\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2sl\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_2sl\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_2sl\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_2sl\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_2sl\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_2sl\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_2sl\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_2sl\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2sl\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2sl\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_2sl\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2sl\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_2sl\_ .icon-shape,#chatgpt-mermaid-\_r_2sl\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_2sl\_ .icon-shape p,#chatgpt-mermaid-\_r_2sl\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_2sl\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_2sl\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_2sl\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_2sl\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_2sl\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_2sl\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_2sl\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_2sl\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_2sl\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2sl\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_2sl\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_2sl\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_2sl\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2sl\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_2sl\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_2sl\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2sl\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_2sl\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_2sl\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2sl\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_2sl\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2sl\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_2sl\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_2sl\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_2sl\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_2sl\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_2sl\_ .node rect,#chatgpt-mermaid-\_r_2sl\_ .node circle,#chatgpt-mermaid-\_r_2sl\_ .node ellipse,#chatgpt-mermaid-\_r_2sl\_ .node polygon,#chatgpt-mermaid-\_r_2sl\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_2sl\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_2sl\_ .mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_2sl\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_2sl\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2sl\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}ClientsAPI GatewayGateway Rate Limit FilterBackend ServicesShared Rate Limit PlatformRedis

The gateway handles broad per-client protection, while internal services apply more specific limits for individual dependencies or business operations.

The important architectural principle is:

**Separate the decision logic, shared state, and business execution responsibilities, even if some of them live in the same deployment.**

# **5. The next problem: Shared Redis doesn't automatically mean correctness**

This is one of the most important concepts in distributed rate-limiter design.

Imagine our shared Redis initially contains:

```
user_123:

tokens = 1
```

Two application servers receive requests at almost exactly the same time.

```
Server 1 receives Request A

Server 2 receives Request B
```

Both access Redis.

A naive implementation might look like this:

```
tokens = GET(key)

if tokens >= 1:

    tokens = tokens - 1

    SET(key, tokens)

    return ALLOW

return REJECT
```

Let's see what can go wrong.

### **5.1 The race condition**

```
Initial Redis state:
    tokens = 1

Request A:
    GET tokens -> 1

Request B:
    GET tokens -> 1

Request A:
    1 >= 1
    ALLOW

Request B:
    1 >= 1
    ALLOW

Request A:
    SET tokens = 0

Request B:
    SET tokens = 0
```

Both requests were allowed.

But only one token existed.

We have accidentally granted permission twice.

Notice that Redis correctly executed every individual command.

The problem is that our *sequence of commands* was not atomic.

### **5.2 What does atomicity mean here?**

We want the following operation to behave as one indivisible unit:

```
1. Read current state.

2. Calculate newly available tokens.

3. Check whether sufficient tokens exist.

4. Deduct the required tokens.

5. Save the updated state.

6. Return the decision.
```

Another request must not interfere between these steps.

The desired execution is:

```
Initial state:
    tokens = 1

Request A executes completely:

    Read = 1
    Deduct = 1
    Save = 0
    ALLOW

Request B executes completely:

    Read = 0
    Insufficient tokens
    REJECT
```

Now the result is correct.

This is called an atomic state transition.

### **5.3 How do we achieve this with Redis?**

There are several approaches:

**1. Single atomic Redis commands**

Some algorithms can be implemented using individual Redis commands.

For example, `INCR` is atomic.

But a sequence such as incrementing, setting an expiration conditionally, and checking the limit requires more than one operation.

**2. Redis Lua scripts**

This is a very common solution.

Redis executes a Lua script atomically with respect to other commands.

We put the complete decision logic inside the script.

```
Application Server
       |
       v
    Redis
       |
       v
   Lua Script
       |
       +-- Read state
       |
       +-- Calculate
       |
       +-- Validate
       |
       +-- Update state
       |
       +-- Return decision
```

Other Redis commands cannot interleave with the script.

**3. Redis transactions / optimistic locking**

We can use `WATCH`, `MULTI`, and `EXEC` for optimistic concurrency control.

A client watches a key, reads its value, prepares an update, and attempts to execute the transaction. If another client modifies the watched key, the transaction is aborted and the client must retry.

This can work, but repeated contention can cause retries.

For a small, self-contained rate-limiter operation, Lua is often simpler.

### **Important distinction**

Atomicity is about the execution of an operation inside Redis.

It does not automatically guarantee that the latest state survives every Redis crash or failover.

Replication and failover durability are separate concerns, which we'll cover later.

# **6. How does the shared Redis implementation differ for each algorithm?**

Now let's connect our five algorithms to the distributed architecture.

The architecture is the same:

```
Multiple Application Instances
              |
              v
         Shared Redis
```

But the Redis data model differs because each algorithm maintains different information.

## **6.1 Fixed Window**

What do we need?

```
One counter for the current fixed window.
```

A suitable representation:

```
Key:
rate_limit:products:user_123

Fields:
    window_id
    request_count
```

For each request, an atomic Lua operation:

1. Gets the current time.
2. Calculates the window ID.
3. Compares it with the stored window ID.
4. Resets the count if necessary.
5. Increments the counter.
6. Checks the limit.
7. Returns the decision.

The important point is that we don't need to physically reset every user's counter at the same time.

A new window ID makes the old count irrelevant.

## **6.2 Sliding Window Log**

What do we need?

```
Every admitted request's timestamp.
```

Redis data structure:

```
Sorted Set
```

Example:

```
Key:
rate_limit:products:user_123

Members and scores:

request_001 -> 1000
request_002 -> 1500
request_003 -> 1800
request_004 -> 2200
```

Atomic Lua operation:

1. Get current time.
2. Remove expired timestamps.
3. Count remaining members.
4. Check the limit.
5. Add the new timestamp if allowed.
6. Set appropriate expiration.

This needs more memory than Fixed Window because every request gets an entry.

## **6.3 Sliding Window Counter**

What do we need?

```
Previous window count
Current window count
Window ID
```

Redis Hash:

```
rate_limit:products:user_123

window_id = 10
previous_count = 8
current_count = 3
```

Atomic Lua operation:

1. Determine the current window.
2. Rotate counters if needed.
3. Calculate the previous-window weight.
4. Calculate the estimated count.
5. Check whether admitting the request would exceed the limit.
6. Increment the current counter if allowed.
7. Save the state.

## **6.4 Leaky Bucket**

Here we need to distinguish two implementations.

**Virtual backlog**

Store something like:

```
rate_limit:user_123

level = 7.5
last_updated = ...
```

The admission decision updates the level atomically.

**Actual queue-based shaping**

Store the jobs in a queue and maintain scheduling information:

```
website_A_queue:
    job_001
    job_002
    job_003

website_A_scheduler:
    next_available_time = ...
```

The queue retains the actual work.

The scheduler determines when it can leave.

This is a more involved distributed workflow than the counter-based algorithms.

## **6.5 Token Bucket**

What do we need?

```
Current token balance
Last refill timestamp
```

Redis Hash:

```
rate_limit:products:user_123

tokens = 6.5
last_refill_ms = ...
```

Atomic Lua operation:

1. Read the previous balance.
2. Calculate elapsed time.
3. Replenish tokens.
4. Cap the balance at capacity.
5. Check request cost.
6. Deduct tokens if allowed.
7. Save the updated balance.

### **Summary of Redis data structures**

| Algorithm              | Redis representation                                          |
| ---------------------- | ------------------------------------------------------------- |
| Fixed Window           | Hash or counter key                                           |
| Sliding Window Log     | Sorted Set                                                    |
| Sliding Window Counter | Hash                                                          |
| Leaky Bucket           | Queue/List/Stream + scheduling state, or a virtual-level Hash |
| Token Bucket           | Hash                                                          |

This is why understanding an algorithm's state requirements is so valuable.

Once you understand what information it needs, choosing the storage structure becomes much easier.

# **7. Design the actual Rate Limiter request flow**

Now let's stop looking at algorithms individually.

We need to design the service that receives a request and decides which algorithm and policy to execute.

Imagine we expose this internal interface:

```
POST /internal/rate-limit/check
```

A request might contain:

```
{
  "resource": "products",
  "identity": "user_123",
  "tenantId": "tenant_456",
  "cost": 1
}
```

The service shouldn't blindly trust everything the caller sends.

In particular, the identity and applicable limits should be derived from authenticated server-side context or trusted gateway metadata.

Otherwise, a malicious caller could change:

```
identity = user_123
```

to:

```
identity = user_456
```

and bypass their own quota.

Let's examine the complete flow.

### **Step 1: Authenticate and identify the caller**

The system determines:

```
userId = user_123
tenantId = tenant_456
```

It should not trust an arbitrary user ID supplied by the public client.

### **Step 2: Determine the applicable policies**

Our policy configuration might contain:

```
products-api:

    Per-user:
        Token Bucket
        Capacity = 100
        Refill = 10/sec

    Per-tenant:
        Token Bucket
        Capacity = 500
        Refill = 50/sec

    Global:
        Token Bucket
        Capacity = 10,000
        Refill = 1,000/sec
```

The Rate Limiter determines which policies apply.

### **Step 3: Derive Redis keys**

For example:

```
rate_limit:products:user:user_123

rate_limit:products:tenant:tenant_456

rate_limit:products:global
```

Each key represents a different budget.

### **Step 4: Validate the policies**

We check all relevant limits.

```
User bucket:
    ALLOW

Tenant bucket:
    ALLOW

Global bucket:
    ALLOW
```

If all policies pass, we admit the request.

If any mandatory policy fails, we reject it.

### **Step 5: Return the decision**

An internal response might be:

```
{
  "allowed": true,
  "remaining": 99,
  "retryAfterMs": 0
}
```

The application can then continue to its business logic.

### **Complete architecture**

\#chatgpt-mermaid-\_r_30n\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_30n\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_30n\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_30n\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_30n\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_30n\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_30n\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_30n\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_30n\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_30n\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_30n\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_30n\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_30n\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_30n\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_30n\_ p{margin:0;}#chatgpt-mermaid-\_r_30n\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_30n\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_30n\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_30n\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_30n\_ .label text,#chatgpt-mermaid-\_r_30n\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_30n\_ .node rect,#chatgpt-mermaid-\_r_30n\_ .node circle,#chatgpt-mermaid-\_r_30n\_ .node ellipse,#chatgpt-mermaid-\_r_30n\_ .node polygon,#chatgpt-mermaid-\_r_30n\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_30n\_ .rough-node .label text,#chatgpt-mermaid-\_r_30n\_ .node .label text,#chatgpt-mermaid-\_r_30n\_ .image-shape .label,#chatgpt-mermaid-\_r_30n\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_30n\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_30n\_ .rough-node .label,#chatgpt-mermaid-\_r_30n\_ .node .label,#chatgpt-mermaid-\_r_30n\_ .image-shape .label,#chatgpt-mermaid-\_r_30n\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_30n\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_30n\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_30n\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_30n\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_30n\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_30n\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_30n\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_30n\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_30n\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_30n\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_30n\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_30n\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_30n\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_30n\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_30n\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_30n\_ .icon-shape,#chatgpt-mermaid-\_r_30n\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_30n\_ .icon-shape p,#chatgpt-mermaid-\_r_30n\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_30n\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_30n\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_30n\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_30n\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_30n\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_30n\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_30n\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_30n\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_30n\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_30n\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_30n\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_30n\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_30n\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_30n\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_30n\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_30n\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_30n\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_30n\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_30n\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_30n\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_30n\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_30n\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_30n\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_30n\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_30n\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_30n\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_30n\_ .node rect,#chatgpt-mermaid-\_r_30n\_ .node circle,#chatgpt-mermaid-\_r_30n\_ .node ellipse,#chatgpt-mermaid-\_r_30n\_ .node polygon,#chatgpt-mermaid-\_r_30n\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_30n\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_30n\_ .mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_30n\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_30n\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_30n\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}ClientAPI GatewayAuthenticationPolicy ResolverRate Limiter Middleware /ServiceRedisAtomic Algorithm ExecutionAllowed?Backend Business LogicHTTP 429ResponseYesNo

This architecture gives us a clean separation:

- Authentication establishes identity.
- Policy resolution determines the applicable limits.
- Redis holds the shared state.
- The algorithm makes the decision.
- The application executes business logic only when permitted.

# **8. Designing Redis keys correctly**

Our rate limiter may have millions of users and thousands of policies.

We need a consistent key design.

A useful general format is:

```
rate_limit:{policy_id}:{scope}:{identity}
```

Examples:

```
rate_limit:products:user:user_123

rate_limit:products:tenant:tenant_456

rate_limit:login:ip:192.0.2.10

rate_limit:payments:api-key:key_123
```

Why include the policy ID?

Because the same user might have different limits for different operations.

Why include the scope?

Because a user-level limit and a tenant-level limit represent different budgets.

### **8.1 Should we put all policy configuration in Redis?**

Not necessarily.

We have two different kinds of information:

**Policy configuration**

```
policy_id
algorithm
capacity
refill_rate
window_duration
limit
```

**Dynamic rate-limit state**

```
tokens
last_refill_time
```

The policy configuration changes relatively infrequently.

The dynamic state changes with requests.

We can keep configuration in a configuration database or a dedicated policy store, and cache it in the Rate Limiter instances.

Redis can hold the rapidly changing state.

For example:

```
Policy configuration:

products-api:
    algorithm = TOKEN_BUCKET
    capacity = 100
    refill_rate = 10
```

```
Redis dynamic state:

rate_limit:products:user:user_123

tokens = 75
last_refill_time = ...
```

This separation is useful because we don't want to fetch policy configuration from a database on every API request.

### **8.2 Should we use the actual user ID in Redis keys?**

It is possible, but we should avoid exposing sensitive identifiers unnecessarily.

We can use an internal identifier or a stable hash:

```
rate_limit:products:user:8f91a2...
```

Also, key cardinality matters.

Imagine attackers can submit unlimited unique API keys or arbitrary identity strings.

They could generate millions of Redis keys.

We need to ensure identities come from authenticated, validated sources and that inactive keys expire.

# **9. How do we apply multiple policies atomically?**

Let's look more closely at an important problem introduced earlier.

Our request must satisfy:

```
Policy A:
    100 requests/second per user.

Policy B:
    1,000 requests/second per tenant.

Policy C:
    10,000 requests/second globally.
```

Suppose the request arrives with:

```
user = user_123
tenant = tenant_456
```

We have three Redis keys:

```
rate_limit:user:user_123

rate_limit:tenant:tenant_456

rate_limit:global
```

Ideally, we want:

```
Check all three budgets.

If all pass:
    Deduct from all three.

If any fails:
    Deduct from none.
```

Why?

Imagine:

```
User bucket:
    50 tokens.

Tenant bucket:
    0 tokens.

Global bucket:
    5,000 tokens.
```

The request is rejected because the tenant has exhausted its quota.

But we don't want to consume a user token or a global token for a request we rejected.

### **9.1 The single-Redis-instance solution**

If all keys are handled by the same Redis primary, we can use one Lua script.

Conceptually:

```
ATOMIC OPERATION:

    Check user bucket.

    Check tenant bucket.

    Check global bucket.

    If any bucket fails:
        Return REJECT.

    Deduct user tokens.
    Deduct tenant tokens.
    Deduct global tokens.

    Return ALLOW.
```

The important thing is that we check all budgets before deducting any of them.

This avoids partial consumption.

### **9.2 The Redis Cluster complication**

Redis Cluster distributes keys across multiple hash slots.

A Lua script operating on multiple keys generally requires those keys to belong to the same hash slot.

Our three keys might be assigned to different slots.

Therefore, we cannot always execute the entire multi-policy operation as one atomic script.

This is a fundamental distributed coordination tradeoff, not something we can solve by simply adding more Lua code.

There are several approaches:

**Approach A: Colocate related keys**

For example:

```
rate_limit:{tenant_456}:user:user_123

rate_limit:{tenant_456}:tenant

rate_limit:{tenant_456}:products
```

The `{tenant_456}` portion is a Redis Cluster hash tag.

All these keys are assigned to the same slot.

A Lua script can operate on them together.

This is useful for coordinating a user's and tenant's budgets.

However, it doesn't automatically solve a global limit shared across all tenants.

**Approach B: Use a centralized global limiter**

We can enforce user and tenant limits in a tenant-scoped Redis slot, while a separate global limiter controls aggregate traffic.

This introduces a potential partial-consumption issue between the two checks.

For many applications, a small amount of quota inefficiency is acceptable, provided we don't violate the hard resource limit.

**Approach C: Use quota leasing**

We can allocate smaller portions of a global budget to different application instances or regions.

For example:

```
Global allowance = 10,000 tokens

Server 1 receives a lease of 1,000.
Server 2 receives a lease of 1,000.
Server 3 receives a lease of 1,000.
```

Each server consumes its locally leased allowance.

This reduces contention on a single global counter.

We'll explore this properly when we discuss local caching and distributed quota allocation.

**Approach D: Use a centralized coordinator**

For limits that require strict all-or-nothing coordination across multiple independent keys or stores, a centralized decision point may be appropriate.

But that introduces a scalability and availability tradeoff.

The important lesson:

**A globally consistent multi-dimensional rate limiter is more difficult than a single-key rate limiter.**

We must decide which guarantees are essential and which tradeoffs are acceptable.

# **10. Should every API request make a Redis call?**

This is the next scaling question.

Imagine:

```
Application traffic = 100,000 requests/second.
```

If every request performs a Redis operation, our Rate Limiter generates a huge amount of Redis traffic.

For a simple Token Bucket, the logical operation includes:

```
Read state
Calculate refill
Validate
Update state
```

Even when this is inside one Lua script, every request still reaches Redis.

Can we reduce these calls?

Yes, but we need to understand the correctness tradeoffs.

## **10.1 Cache policy configuration locally**

First, consider policy configuration.

```
products-api:
    capacity = 100
    refill_rate = 10
```

This information changes infrequently.

We can cache it in each application instance.

\#chatgpt-mermaid-\_r_33c\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_33c\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_33c\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_33c\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_33c\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_33c\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_33c\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_33c\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_33c\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_33c\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_33c\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_33c\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_33c\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_33c\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_33c\_ p{margin:0;}#chatgpt-mermaid-\_r_33c\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_33c\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_33c\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_33c\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_33c\_ .label text,#chatgpt-mermaid-\_r_33c\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_33c\_ .node rect,#chatgpt-mermaid-\_r_33c\_ .node circle,#chatgpt-mermaid-\_r_33c\_ .node ellipse,#chatgpt-mermaid-\_r_33c\_ .node polygon,#chatgpt-mermaid-\_r_33c\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_33c\_ .rough-node .label text,#chatgpt-mermaid-\_r_33c\_ .node .label text,#chatgpt-mermaid-\_r_33c\_ .image-shape .label,#chatgpt-mermaid-\_r_33c\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_33c\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_33c\_ .rough-node .label,#chatgpt-mermaid-\_r_33c\_ .node .label,#chatgpt-mermaid-\_r_33c\_ .image-shape .label,#chatgpt-mermaid-\_r_33c\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_33c\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_33c\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_33c\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_33c\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_33c\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_33c\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_33c\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_33c\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_33c\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_33c\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_33c\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_33c\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_33c\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_33c\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_33c\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_33c\_ .icon-shape,#chatgpt-mermaid-\_r_33c\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_33c\_ .icon-shape p,#chatgpt-mermaid-\_r_33c\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_33c\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_33c\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_33c\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_33c\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_33c\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_33c\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_33c\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_33c\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_33c\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_33c\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_33c\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_33c\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_33c\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_33c\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_33c\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_33c\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_33c\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_33c\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_33c\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_33c\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_33c\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_33c\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_33c\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_33c\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_33c\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_33c\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_33c\_ .node rect,#chatgpt-mermaid-\_r_33c\_ .node circle,#chatgpt-mermaid-\_r_33c\_ .node ellipse,#chatgpt-mermaid-\_r_33c\_ .node polygon,#chatgpt-mermaid-\_r_33c\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_33c\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_33c\_ .mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_33c\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_33c\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_33c\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}Backend Server 1Local Policy CacheBackend Server 2Local Policy CacheBackend Server 3Local Policy CachePolicy Configuration Store

This is generally a good optimization.

We don't need to fetch the policy from a database for every request.

We need a mechanism to invalidate or refresh local policy caches when policies change.

## **10.2 Cache the actual rate-limit state locally**

This is much more complicated.

Imagine we cache:

```
Server 1:
    user_123 = 10 tokens

Server 2:
    user_123 = 10 tokens

Server 3:
    user_123 = 10 tokens
```

We have recreated our original distributed-state problem.

Each instance now has an independent view of the available allowance.

We must not simply duplicate the authoritative token balance.

But there's a more interesting approach.

## **10.3 Lease tokens to application instances**

Instead of every server independently copying the same balance, we can allocate disjoint portions of the global allowance to individual servers.

Suppose:

```
Global available allowance = 100 tokens
```

Redis allocates:

```
Server 1 receives 30 tokens.

Server 2 receives 30 tokens.

Server 3 receives 40 tokens.
```

Each server can consume its own allocation locally.

```
Server 1:
    Local tokens = 30

Server 2:
    Local tokens = 30

Server 3:
    Local tokens = 40
```

The important distinction is that these are not three copies of the same 100 tokens.

They are three distinct reservations from a shared budget.

The total allocation remains:

\\[ 30+30+40=100 \\]

Therefore, they cannot collectively spend more than the leased global allowance, assuming reservations and expiration are correctly managed.

### **What are the advantages?**

- Fewer Redis calls.
- Lower Redis contention.
- Faster local decisions.
- Better ability to handle high request volumes.

### **What are the disadvantages?**

- Some servers might have unused tokens.
- Other servers might run out of their local allocations.
- Capacity can become unevenly distributed.
- Failed servers may temporarily strand leased capacity.
- Replenishment and lease expiration require additional coordination.

Imagine:

```
Server 1:
    Uses only 5 of its 30 tokens.

Server 2:
    Needs 50 tokens but only has 30.
```

The global system might still have unused capacity, but Server 2 cannot spend Server 1's allocation without another coordination operation.

This is the tradeoff.

**Quota leasing improves scalability by exchanging some utilization efficiency for fewer centralized decisions.**

It is useful in high-throughput systems where making a Redis round trip for every request is too expensive.

One important caution: leases must not be double-allocated after expiry or failover. The coordinator needs to track which allocations remain valid and how unused capacity can be safely reclaimed.

# **11. What have we achieved so far?**

Let's look at the evolution of our architecture.

**Stage 1 — Single application instance**

```
Application
    |
    v
In-memory Rate Limiter
```

Problem: State is local to one application instance.

**Stage 2 — Multiple application instances**

```
          Load Balancer
          /     |     \
       App 1   App 2   App 3
```

Problem: Every instance independently maintains its own rate-limit allowance.

**Stage 3 — Shared Redis**

```
          Load Balancer
          /     |     \
       App 1   App 2   App 3
          \     |     /
           Shared Redis
```

Problem: Individual Redis commands are atomic, but our complete read-calculate-check-update operation might not be.

**Stage 4 — Atomic Redis execution**

```
          Load Balancer
          /     |     \
       App 1   App 2   App 3
          \     |     /
           Shared Redis
                |
             Lua Script
                |
          Atomic Decision
```

We now have a distributed rate limiter with shared state and atomic admission decisions.

**Stage 5 — Scaling optimizations**

We introduced:

- Local caching of policy configuration.
- Redis Cluster hash slots.
- Quota leasing to application instances.
- Different coordination strategies for user, tenant, and global limits.

These help us reduce the overhead of centralized state management.

# **12. The mental model to retain**

There are three separate responsibilities in our distributed Rate Limiter.

```
1. POLICY

   What is the limit?
   Who does it apply to?
   Which algorithm should we use?


2. STATE

   What is the current counter,
   timestamp history, token balance,
   or queue state?

   Where is it stored?


3. DECISION

   Can this request proceed?

   Is the state updated atomically?
```

Redis primarily helps us maintain shared state and make atomic decisions.

It does not decide which policy is appropriate.

Our Rate Limiter service or middleware connects these responsibilities.

## **Next: Chapter 7 — Part 2: Scaling and Making the Distributed Rate Limiter Reliable**

We'll now tackle the harder distributed-systems problems:

1. Redis Cluster partitioning and hot keys.
2. What happens when millions of requests hit the same API key?
3. Can we horizontally scale a global rate limiter?
4. How do we enforce global limits across multiple regions?
5. What happens during Redis outages and failovers?
6. Fail-open vs fail-closed design.
7. What happens when Redis loses recent writes during failover?
8. Local rate limiting versus centralized rate limiting.
9. Monitoring, metrics, and operational reliability.

The central question will be:

**How do we preserve the rate-limit guarantees while making the Rate Limiter itself scalable, highly available, and resilient to failures?**











































# **Chapter 7 — Part 2: Scaling and Making a Distributed Rate Limiter Reliable**

In Part 1, we built the foundation:

```
Multiple Backend Instances
          |
          v
     Shared Redis
          |
          v
    Atomic Validation
          |
          v
    ALLOW / REJECT
```

We solved the problem of multiple application instances maintaining independent rate-limit state.

But now imagine our application becomes extremely popular.

We have:

```
100 application instances

100,000 requests/second

10 million active users

Multiple geographical regions
```

Our Rate Limiter itself now becomes a system that needs to scale.

We have to answer some difficult questions:

1. Can one Redis instance handle all rate-limit operations?
2. What happens when millions of requests access the same Redis key?
3. How do we distribute rate-limit state across Redis Cluster?
4. How do we enforce a global limit without creating a central bottleneck?
5. How do we coordinate rate limits across multiple geographical regions?
6. What happens when Redis fails or loses state?

Let's evolve our architecture again.

# **1. Problem: A single Redis instance becomes a bottleneck**

Initially, our architecture looks like this:

\#chatgpt-mermaid-\_r_35u\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_35u\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_35u\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_35u\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_35u\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_35u\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_35u\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_35u\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_35u\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_35u\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_35u\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_35u\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_35u\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_35u\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_35u\_ p{margin:0;}#chatgpt-mermaid-\_r_35u\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_35u\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_35u\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_35u\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_35u\_ .label text,#chatgpt-mermaid-\_r_35u\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_35u\_ .node rect,#chatgpt-mermaid-\_r_35u\_ .node circle,#chatgpt-mermaid-\_r_35u\_ .node ellipse,#chatgpt-mermaid-\_r_35u\_ .node polygon,#chatgpt-mermaid-\_r_35u\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_35u\_ .rough-node .label text,#chatgpt-mermaid-\_r_35u\_ .node .label text,#chatgpt-mermaid-\_r_35u\_ .image-shape .label,#chatgpt-mermaid-\_r_35u\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_35u\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_35u\_ .rough-node .label,#chatgpt-mermaid-\_r_35u\_ .node .label,#chatgpt-mermaid-\_r_35u\_ .image-shape .label,#chatgpt-mermaid-\_r_35u\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_35u\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_35u\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_35u\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_35u\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_35u\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_35u\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_35u\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_35u\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_35u\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_35u\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_35u\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_35u\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_35u\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_35u\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_35u\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_35u\_ .icon-shape,#chatgpt-mermaid-\_r_35u\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_35u\_ .icon-shape p,#chatgpt-mermaid-\_r_35u\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_35u\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_35u\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_35u\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_35u\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_35u\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_35u\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_35u\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_35u\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_35u\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_35u\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_35u\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_35u\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_35u\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_35u\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_35u\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_35u\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_35u\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_35u\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_35u\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_35u\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_35u\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_35u\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_35u\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_35u\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_35u\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_35u\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_35u\_ .node rect,#chatgpt-mermaid-\_r_35u\_ .node circle,#chatgpt-mermaid-\_r_35u\_ .node ellipse,#chatgpt-mermaid-\_r_35u\_ .node polygon,#chatgpt-mermaid-\_r_35u\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_35u\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_35u\_ .mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_35u\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_35u\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_35u\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}ClientsLoad BalancerApplication InstancesSingle Redis Instance

Every request requires a rate-limit decision.

For example:

```
100,000 application requests/second

100,000 Redis rate-limit evaluations/second
```

These evaluations might execute Lua scripts involving:

- Reading counters.
- Calculating refill.
- Updating token balances.
- Inserting timestamps.
- Removing expired records.
- Checking multiple policies.

Redis has to perform all this work.

Even though individual operations are efficient, the aggregate workload can become too large for one Redis primary.

We need to distribute the workload.

## **1.1 Introduce Redis Cluster**

Instead of one Redis instance, we introduce multiple Redis nodes.

\#chatgpt-mermaid-\_r_363\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_363\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_363\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_363\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_363\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_363\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_363\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_363\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_363\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_363\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_363\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_363\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_363\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_363\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_363\_ p{margin:0;}#chatgpt-mermaid-\_r_363\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_363\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_363\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_363\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_363\_ .label text,#chatgpt-mermaid-\_r_363\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_363\_ .node rect,#chatgpt-mermaid-\_r_363\_ .node circle,#chatgpt-mermaid-\_r_363\_ .node ellipse,#chatgpt-mermaid-\_r_363\_ .node polygon,#chatgpt-mermaid-\_r_363\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_363\_ .rough-node .label text,#chatgpt-mermaid-\_r_363\_ .node .label text,#chatgpt-mermaid-\_r_363\_ .image-shape .label,#chatgpt-mermaid-\_r_363\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_363\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_363\_ .rough-node .label,#chatgpt-mermaid-\_r_363\_ .node .label,#chatgpt-mermaid-\_r_363\_ .image-shape .label,#chatgpt-mermaid-\_r_363\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_363\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_363\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_363\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_363\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_363\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_363\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_363\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_363\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_363\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_363\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_363\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_363\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_363\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_363\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_363\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_363\_ .icon-shape,#chatgpt-mermaid-\_r_363\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_363\_ .icon-shape p,#chatgpt-mermaid-\_r_363\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_363\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_363\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_363\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_363\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_363\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_363\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_363\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_363\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_363\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_363\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_363\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_363\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_363\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_363\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_363\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_363\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_363\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_363\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_363\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_363\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_363\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_363\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_363\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_363\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_363\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_363\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_363\_ .node rect,#chatgpt-mermaid-\_r_363\_ .node circle,#chatgpt-mermaid-\_r_363\_ .node ellipse,#chatgpt-mermaid-\_r_363\_ .node polygon,#chatgpt-mermaid-\_r_363\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_363\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_363\_ .mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_363\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_363\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_363\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}Application InstancesRedis ClusterRedis Primary 1Redis Primary 2Redis Primary 3Replica 1Replica 2Replica 3

Each primary owns a portion of the keyspace.

For example:

```
Redis Primary 1:
    user_001 rate-limit keys
    user_004 rate-limit keys
    user_008 rate-limit keys

Redis Primary 2:
    user_002 rate-limit keys
    user_005 rate-limit keys
    user_009 rate-limit keys

Redis Primary 3:
    user_003 rate-limit keys
    user_006 rate-limit keys
    user_007 rate-limit keys
```

Now requests for different users can be processed by different Redis primaries.

This allows us to scale horizontally.

But how does Redis determine which node owns a particular key?

## **1.2 How Redis Cluster distributes keys**

Redis Cluster uses **hash slots**.

There are 16,384 hash slots.

Conceptually:

\\[ slot = HASH(key) \bmod 16384 \\]

Each primary is responsible for a portion of these slots.

For example:

```
16384 hash slots

Primary 1:
    Slots 0 - 5460

Primary 2:
    Slots 5461 - 10922

Primary 3:
    Slots 10923 - 16383
```

When an application accesses:

```
rate_limit:products:user_123
```

Redis determines the corresponding hash slot and routes the operation to the primary responsible for that slot.

We don't manually decide which server stores each user.

Redis handles that mapping.

The useful consequence:

```
Different user keys
       |
       v
Different hash slots
       |
       v
Different Redis primaries
```

This works well when traffic is distributed across many identities.

But there is an important exception.

**Redis Cluster distributes keys, not the individual requests accessing one key.**

Let's understand why that matters.

# **2. Problem: Hot Keys**

Imagine we have an API that receives requests from millions of users.

Most users send a small number of requests.

But one enterprise customer sends:

```
50,000 requests/second
```

Our key design is:

```
rate_limit:enterprise:tenant_123
```

Every request from this tenant accesses the same Redis key.

Redis Cluster assigns that key to one hash slot.

That slot belongs to one primary.

Therefore:

```
50,000 requests/second

             |
             v

rate_limit:tenant_123

             |
             v

Redis Primary 2
```

Even though we have ten Redis primaries, all those operations target the same primary.

Adding more Redis nodes doesn't distribute operations against a single hot key.

This is called the **hot-key problem**.

## **2.1 Why is this a problem?**

Let's imagine our cluster has:

```
Primary 1:
    10,000 operations/second

Primary 2:
    80,000 operations/second

Primary 3:
    12,000 operations/second
```

The average workload might look manageable.

But Primary 2 is handling significantly more work.

It can become the bottleneck.

The problem isn't necessarily the number of keys.

It is the concentration of traffic on one key.

### **Another example: Global rate limit**

Suppose we enforce:

```
Global application limit = 100,000 requests/second
```

We maintain:

```
rate_limit:global
```

Every incoming request updates this single key.

All 100,000 requests/second hit one Redis primary.

We've effectively centralized the entire rate limiter around one hot key.

This defeats much of the benefit of Redis Cluster.

So what should we do?

# **3. Solution 1: Partition the rate-limit budget**

Let's revisit our example.

We want:

```
Global limit = 1,000 requests/second
```

Instead of making every request update one global counter, we distribute portions of the allowance.

Suppose we have four application groups.

We allocate:

```
Application Group 1:
    250 requests/second

Application Group 2:
    250 requests/second

Application Group 3:
    250 requests/second

Application Group 4:
    250 requests/second
```

The total allowance is:

\\[ 250+250+250+250=1000 \\]

Each group maintains its own local or regional limiter.

\#chatgpt-mermaid-\_r_380\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_380\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_380\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_380\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_380\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_380\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_380\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_380\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_380\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_380\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_380\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_380\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_380\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_380\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_380\_ p{margin:0;}#chatgpt-mermaid-\_r_380\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_380\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_380\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_380\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_380\_ .label text,#chatgpt-mermaid-\_r_380\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_380\_ .node rect,#chatgpt-mermaid-\_r_380\_ .node circle,#chatgpt-mermaid-\_r_380\_ .node ellipse,#chatgpt-mermaid-\_r_380\_ .node polygon,#chatgpt-mermaid-\_r_380\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_380\_ .rough-node .label text,#chatgpt-mermaid-\_r_380\_ .node .label text,#chatgpt-mermaid-\_r_380\_ .image-shape .label,#chatgpt-mermaid-\_r_380\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_380\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_380\_ .rough-node .label,#chatgpt-mermaid-\_r_380\_ .node .label,#chatgpt-mermaid-\_r_380\_ .image-shape .label,#chatgpt-mermaid-\_r_380\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_380\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_380\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_380\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_380\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_380\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_380\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_380\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_380\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_380\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_380\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_380\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_380\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_380\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_380\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_380\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_380\_ .icon-shape,#chatgpt-mermaid-\_r_380\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_380\_ .icon-shape p,#chatgpt-mermaid-\_r_380\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_380\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_380\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_380\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_380\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_380\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_380\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_380\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_380\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_380\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_380\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_380\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_380\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_380\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_380\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_380\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_380\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_380\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_380\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_380\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_380\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_380\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_380\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_380\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_380\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_380\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_380\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_380\_ .node rect,#chatgpt-mermaid-\_r_380\_ .node circle,#chatgpt-mermaid-\_r_380\_ .node ellipse,#chatgpt-mermaid-\_r_380\_ .node polygon,#chatgpt-mermaid-\_r_380\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_380\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_380\_ .mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_380\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_380\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_380\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}Incoming RequestsApplication Group 1Application Group 2Application Group 3Application Group 4Limiter 1: 250/secLimiter 2: 250/secLimiter 3: 250/secLimiter 4: 250/sec

We no longer need to update one global counter for every request.

Each group independently enforces its allocated budget.

The aggregate allowance remains bounded by 1,000 requests/second, assuming the allocations are correctly managed.

## **3.1 What is the tradeoff?**

Imagine:

```
Group 1 traffic = 500 requests/second

Group 2 traffic = 100 requests/second

Group 3 traffic = 100 requests/second

Group 4 traffic = 100 requests/second
```

Our allocations are:

```
Group 1 = 250
Group 2 = 250
Group 3 = 250
Group 4 = 250
```

Group 1 rejects 250 requests even though the other groups have unused capacity.

Total accepted traffic:

\\[ 250+100+100+100=550 \\]

We could potentially have accepted more.

We've exchanged utilization efficiency for scalability.

This is an important recurring tradeoff in distributed systems.

## **3.2 What happens when traffic distribution changes?**

We can introduce dynamic budget allocation.

For example:

```
Group 1:
    Initially allocated 250/sec.

Group 2:
    Initially allocated 250/sec.

Group 3:
    Initially allocated 250/sec.

Group 4:
    Initially allocated 250/sec.
```

Suppose Group 1 suddenly needs more capacity.

A central coordinator can allocate additional quota to it.

```
Group 1 = 400/sec

Group 2 = 200/sec

Group 3 = 200/sec

Group 4 = 200/sec
```

The total remains:

\\[ 400+200+200+200=1000 \\]

The application groups consume their allocations locally.

This is called quota allocation or quota leasing, depending on how the allocations are managed.

The important principle:

**We coordinate the budget occasionally rather than coordinating every individual request.**

# **4. Quota leasing — A powerful distributed-systems technique**

Let's understand this more deeply because it is useful beyond rate limiting.

Suppose our global policy is:

```
10,000 requests/second.
```

We have 10 application instances.

If every request contacts Redis, we create 10,000 Redis operations per second just for this limit.

Instead, we can allocate chunks of permission to each application instance.

For example:

```
Global Rate Limiter
        |
        v
Quota Coordinator
        |
        +---- Server 1 gets 1,000 tokens
        |
        +---- Server 2 gets 1,000 tokens
        |
        +---- Server 3 gets 1,000 tokens
        |
        ...
        |
        +---- Server 10 gets 1,000 tokens
```

Each server maintains its allocated tokens locally.

Suppose Server 1 has:

```
Leased quota = 1,000

Used quota = 400

Remaining quota = 600
```

For the next 600 admitted requests, Server 1 doesn't need to contact Redis.

It can make local decisions.

When its local allocation is exhausted, it requests another allocation.

This dramatically reduces centralized coordination.

## **4.1 How do we avoid allocating the same quota twice?**

The coordinator must maintain the global remaining budget.

Consider:

```
Global budget = 10,000 tokens
```

Server 1 requests 1,000.

The coordinator atomically reserves:

```
Remaining global allocation = 9,000
Server 1 lease = 1,000
```

Server 2 requests another 1,000.

```
Remaining global allocation = 8,000
Server 2 lease = 1,000
```

The coordinator must ensure that these reservations don't overlap.

A lease should contain something like:

```
lease_id
owner_instance_id
granted_quota
expiration_time
generation / fencing token
```

## **4.2 What happens if an application instance crashes?**

Suppose Server 1 receives:

```
1,000 quota units
```

It uses only 400 and then crashes.

600 units remain unused.

What should happen?

We cannot immediately allocate those 600 units to another server unless we know Server 1 can no longer spend them.

Otherwise:

```
Server 1 still has permission for 600.

Server 2 receives the same 600.

Both consume them.

Global limit is exceeded.
```

We need to protect against double spending.

One approach is to make leases valid only until a specific expiration time.

The application instance must stop accepting requests using that lease after expiry.

The coordinator must not reissue the unspent quota until it knows the previous lease is no longer valid.

A fencing token or lease generation can help prevent stale owners from continuing to operate after a newer lease has been issued.

Unused quota can be discarded rather than immediately reclaimed. This sacrifices some utilization but simplifies safety.

## **4.3 An important distinction: Rate tokens versus quota chunks**

There are two different things we might lease.

**A fixed quota for a time interval**

Example:

```
Server 1:
    1,000 requests allowed during the next second.
```

**Tokens reserved from a global Token Bucket**

Example:

```
Server 1:
    Receives 1,000 globally reserved tokens.
```

The second model directly transfers a portion of the global token balance to a local bucket.

The central coordinator must ensure that total grants never exceed what the global bucket permits.

Local buckets may then consume those reserved tokens independently.

If leases expire and are discarded, some available capacity can be wasted.

If the system allows them to be reused, it needs careful lease expiry and fencing.

For a strict upper bound, it is safer to discard unused allocations than to risk issuing them twice.

## **4.4 What exactly have we achieved?**

Before leasing:

```
Every request
     |
     v
Central Redis
     |
     v
Atomic validation
```

After leasing:

```
                    Coordinator
                         |
               Allocate permission
                         |
           +-------------+-------------+
           |             |             |
           v             v             v
        Server 1      Server 2      Server 3
        Local         Local         Local
        quota         quota         quota
           |             |             |
           +-------------+-------------+
                         |
                    Requests
```

We have reduced the number of centralized decisions.

This is useful for:

- Global service rate limits.
- Very high-volume API gateways.
- Large tenant quotas.
- Regional traffic budgets.
- Protecting expensive shared dependencies.

However, leasing is not free.

We must manage unused capacity, lease expiration, crashed instances, and the possibility of stale owners.

# **5. Can we distribute one tenant's hot key?**

So far, our quota leasing example distributed a global budget among application instances.

But consider a slightly different problem.

We have:

```
10 million users.

One tenant:
    tenant_123

Traffic from this tenant:
    50,000 requests/second.
```

Our tenant-level policy is:

```
Maximum = 10,000 requests/second.
```

All requests access:

```
rate_limit:tenant:tenant_123
```

This is still one hot key.

Can we split it across multiple Redis keys?

Yes, but we must do it carefully.

## **5.1 Naive partitioning — Incorrect**

Suppose we create four keys:

```
rate_limit:tenant_123:partition_1

rate_limit:tenant_123:partition_2

rate_limit:tenant_123:partition_3

rate_limit:tenant_123:partition_4
```

And we configure every partition with:

```
10,000 requests/second.
```

What happens?

Our aggregate allowance becomes:

\\[ 4 \times 10,000 = 40,000 \\]

We have accidentally multiplied the tenant's allowance by four.

Splitting a key doesn't automatically preserve its original limit.

## **5.2 Correct partitioning — Allocate the budget**

Our actual limit is:

```
10,000 requests/second.
```

We can allocate:

```
Partition 1 = 2,500 requests/second

Partition 2 = 2,500 requests/second

Partition 3 = 2,500 requests/second

Partition 4 = 2,500 requests/second
```

Total:

\\[ 2500 \times 4 = 10000 \\]

Now each partition can independently enforce its allocated budget.

\#chatgpt-mermaid-\_r_3ba\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_3ba\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_3ba\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_3ba\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_3ba\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3ba\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_3ba\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_3ba\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_3ba\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_3ba\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_3ba\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_3ba\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3ba\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3ba\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_3ba\_ p{margin:0;}#chatgpt-mermaid-\_r_3ba\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3ba\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3ba\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3ba\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_3ba\_ .label text,#chatgpt-mermaid-\_r_3ba\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3ba\_ .node rect,#chatgpt-mermaid-\_r_3ba\_ .node circle,#chatgpt-mermaid-\_r_3ba\_ .node ellipse,#chatgpt-mermaid-\_r_3ba\_ .node polygon,#chatgpt-mermaid-\_r_3ba\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_3ba\_ .rough-node .label text,#chatgpt-mermaid-\_r_3ba\_ .node .label text,#chatgpt-mermaid-\_r_3ba\_ .image-shape .label,#chatgpt-mermaid-\_r_3ba\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_3ba\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_3ba\_ .rough-node .label,#chatgpt-mermaid-\_r_3ba\_ .node .label,#chatgpt-mermaid-\_r_3ba\_ .image-shape .label,#chatgpt-mermaid-\_r_3ba\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_3ba\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_3ba\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3ba\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3ba\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_3ba\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_3ba\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_3ba\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_3ba\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_3ba\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_3ba\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_3ba\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3ba\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3ba\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_3ba\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3ba\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_3ba\_ .icon-shape,#chatgpt-mermaid-\_r_3ba\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_3ba\_ .icon-shape p,#chatgpt-mermaid-\_r_3ba\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_3ba\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_3ba\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_3ba\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_3ba\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_3ba\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_3ba\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_3ba\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_3ba\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_3ba\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3ba\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_3ba\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_3ba\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_3ba\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3ba\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_3ba\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_3ba\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3ba\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_3ba\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_3ba\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3ba\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_3ba\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3ba\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_3ba\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_3ba\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_3ba\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_3ba\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_3ba\_ .node rect,#chatgpt-mermaid-\_r_3ba\_ .node circle,#chatgpt-mermaid-\_r_3ba\_ .node ellipse,#chatgpt-mermaid-\_r_3ba\_ .node polygon,#chatgpt-mermaid-\_r_3ba\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_3ba\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_3ba\_ .mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_3ba\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_3ba\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3ba\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}tenant_123 requestsPartition RouterPartition 1: 2500/secPartition 2: 2500/secPartition 3: 2500/secPartition 4: 2500/secBackend Operations

This preserves the aggregate upper bound, assuming each partition correctly enforces its own allocation.

But we have the same utilization problem as before.

Suppose:

```
Partition 1 needs 5,000 requests/second.

Partition 2 needs 1,000 requests/second.

Partition 3 needs 1,000 requests/second.

Partition 4 needs 1,000 requests/second.
```

Partition 1 cannot use the unused capacity of the other partitions without an allocation adjustment.

This is the fundamental tradeoff.

### **When should we use this?**

When:

- One identity has extremely high traffic.
- One Redis key is the bottleneck.
- Some utilization inefficiency is acceptable.
- We can allocate and rebalance independent budgets.

For a less demanding system, a single atomic key might be simpler and perfectly adequate.

**Don't distribute a key merely because Redis Cluster exists. Distribute it when the workload actually requires it.**

# **6. Global rate limiting — A difficult system design problem**

Now let's examine an important scenario.

We have:

```
20 application instances.

100,000 requests/second aggregate traffic.

Global application limit:
    10,000 requests/second.
```

We want to guarantee that the entire application admits no more than its configured allowance.

A straightforward implementation is:

```
Every request
      |
      v
Redis key: rate_limit:global
      |
      v
Atomic increment / token deduction
```

But we have already identified the hot-key problem.

Can we enforce the global limit without sending every request to one central Redis key?

Let's examine three approaches.

## **6.1 Approach A: A single centralized global limiter**

Architecture:

\#chatgpt-mermaid-\_r_3bt\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_3bt\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_3bt\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_3bt\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_3bt\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3bt\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_3bt\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_3bt\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_3bt\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_3bt\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_3bt\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_3bt\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3bt\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3bt\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_3bt\_ p{margin:0;}#chatgpt-mermaid-\_r_3bt\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3bt\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3bt\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3bt\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_3bt\_ .label text,#chatgpt-mermaid-\_r_3bt\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3bt\_ .node rect,#chatgpt-mermaid-\_r_3bt\_ .node circle,#chatgpt-mermaid-\_r_3bt\_ .node ellipse,#chatgpt-mermaid-\_r_3bt\_ .node polygon,#chatgpt-mermaid-\_r_3bt\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_3bt\_ .rough-node .label text,#chatgpt-mermaid-\_r_3bt\_ .node .label text,#chatgpt-mermaid-\_r_3bt\_ .image-shape .label,#chatgpt-mermaid-\_r_3bt\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_3bt\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_3bt\_ .rough-node .label,#chatgpt-mermaid-\_r_3bt\_ .node .label,#chatgpt-mermaid-\_r_3bt\_ .image-shape .label,#chatgpt-mermaid-\_r_3bt\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_3bt\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_3bt\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3bt\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3bt\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_3bt\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_3bt\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_3bt\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_3bt\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_3bt\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_3bt\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_3bt\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3bt\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3bt\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_3bt\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3bt\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_3bt\_ .icon-shape,#chatgpt-mermaid-\_r_3bt\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_3bt\_ .icon-shape p,#chatgpt-mermaid-\_r_3bt\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_3bt\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_3bt\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_3bt\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_3bt\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_3bt\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_3bt\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_3bt\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_3bt\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_3bt\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3bt\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_3bt\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_3bt\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_3bt\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3bt\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_3bt\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_3bt\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3bt\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_3bt\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_3bt\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3bt\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_3bt\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3bt\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_3bt\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_3bt\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_3bt\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_3bt\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_3bt\_ .node rect,#chatgpt-mermaid-\_r_3bt\_ .node circle,#chatgpt-mermaid-\_r_3bt\_ .node ellipse,#chatgpt-mermaid-\_r_3bt\_ .node polygon,#chatgpt-mermaid-\_r_3bt\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_3bt\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_3bt\_ .mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_3bt\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_3bt\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3bt\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}Application ServersGlobal Rate LimiterRedis PrimaryGlobal Decision

Every request asks the central limiter.

**Advantages:**

- Simple global accounting.
- Easy to reason about.
- Strong coordination among requests processed by the same primary.
- No unused distributed quota allocations.

**Disadvantages:**

- One hot key or coordinator can become a bottleneck.
- Every request introduces a network dependency.
- High latency can affect the whole application.
- The coordinator becomes critical infrastructure.

This is useful when traffic is moderate and the correctness requirement justifies centralized coordination.

## **6.2 Approach B: Allocate global capacity to application instances**

Instead of checking every request centrally, the coordinator leases portions of the global allowance.

Example:

```
Global rate = 10,000 requests/second

20 application instances.
```

We might allocate:

```
Server 1 = 500/sec

Server 2 = 500/sec

...

Server 20 = 500/sec
```

Total:

\\[ 20 \times 500 = 10,000 \\]

Each instance enforces its local budget.

Advantages:

- Much lower Redis traffic.
- Fast local checks.
- No single hot counter for every request.
- Aggregate limits remain bounded by the sum of valid allocations.

Disadvantages:

- Traffic might not be distributed evenly.
- Some instances may exhaust their budgets while others have unused capacity.
- Leases and reallocation add complexity.
- Instance crashes can strand unused quota.

This is often a good solution for very high-throughput systems where exact per-request utilization is less important than enforcing a hard aggregate upper bound.

## **6.3 Approach C: Hierarchical rate limiting**

We can combine the two approaches.

For example:

\#chatgpt-mermaid-\_r_3cc\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_3cc\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_3cc\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_3cc\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_3cc\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3cc\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_3cc\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_3cc\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_3cc\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_3cc\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_3cc\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_3cc\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3cc\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3cc\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_3cc\_ p{margin:0;}#chatgpt-mermaid-\_r_3cc\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3cc\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3cc\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3cc\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_3cc\_ .label text,#chatgpt-mermaid-\_r_3cc\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3cc\_ .node rect,#chatgpt-mermaid-\_r_3cc\_ .node circle,#chatgpt-mermaid-\_r_3cc\_ .node ellipse,#chatgpt-mermaid-\_r_3cc\_ .node polygon,#chatgpt-mermaid-\_r_3cc\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_3cc\_ .rough-node .label text,#chatgpt-mermaid-\_r_3cc\_ .node .label text,#chatgpt-mermaid-\_r_3cc\_ .image-shape .label,#chatgpt-mermaid-\_r_3cc\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_3cc\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_3cc\_ .rough-node .label,#chatgpt-mermaid-\_r_3cc\_ .node .label,#chatgpt-mermaid-\_r_3cc\_ .image-shape .label,#chatgpt-mermaid-\_r_3cc\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_3cc\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_3cc\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3cc\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3cc\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_3cc\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_3cc\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_3cc\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_3cc\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_3cc\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_3cc\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_3cc\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3cc\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3cc\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_3cc\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3cc\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_3cc\_ .icon-shape,#chatgpt-mermaid-\_r_3cc\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_3cc\_ .icon-shape p,#chatgpt-mermaid-\_r_3cc\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_3cc\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_3cc\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_3cc\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_3cc\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_3cc\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_3cc\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_3cc\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_3cc\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_3cc\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3cc\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_3cc\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_3cc\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_3cc\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3cc\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_3cc\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_3cc\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3cc\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_3cc\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_3cc\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3cc\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_3cc\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3cc\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_3cc\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_3cc\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_3cc\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_3cc\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_3cc\_ .node rect,#chatgpt-mermaid-\_r_3cc\_ .node circle,#chatgpt-mermaid-\_r_3cc\_ .node ellipse,#chatgpt-mermaid-\_r_3cc\_ .node polygon,#chatgpt-mermaid-\_r_3cc\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_3cc\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_3cc\_ .mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_3cc\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_3cc\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3cc\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}Global Budget CoordinatorRegional BudgetRegional BudgetApplication Group 1Application Group 2Application Group 3Application Group 4Local Token BucketsLocal Token BucketsLocal Token BucketsLocal Token Buckets

The hierarchy becomes:

```
Global quota
    |
    v
Regional quotas
    |
    v
Application instance quotas
    |
    v
Local request decisions
```

For example:

```
Global:
    10,000 requests/second

Region A:
    6,000 requests/second

Region B:
    4,000 requests/second

Region A:

    Group 1 = 3,000

    Group 2 = 3,000

Region B:

    Group 3 = 2,000

    Group 4 = 2,000
```

Our allocations add up correctly.

\\[ 6000+4000=10000 \\]

This architecture distributes the coordination workload.

The important requirement is that each child allocation must be reserved from its parent's available budget.

A parent must never grant more quota to its children than it has available.

Also, a child must stop consuming quota after its lease expires.

That is what preserves the aggregate guarantee.

# **7. Multi-region Rate Limiting**

Now let's introduce the next major distributed-systems challenge.

Imagine our application runs in three geographical regions:

```
Region A: Mumbai

Region B: Singapore

Region C: Frankfurt
```

Each region has:

- Application servers.
- Load balancers.
- A local Redis deployment.

Architecture:

\#chatgpt-mermaid-\_r_3cv\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_3cv\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_3cv\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_3cv\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_3cv\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3cv\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_3cv\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_3cv\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_3cv\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_3cv\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_3cv\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_3cv\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3cv\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3cv\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_3cv\_ p{margin:0;}#chatgpt-mermaid-\_r_3cv\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3cv\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3cv\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3cv\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_3cv\_ .label text,#chatgpt-mermaid-\_r_3cv\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3cv\_ .node rect,#chatgpt-mermaid-\_r_3cv\_ .node circle,#chatgpt-mermaid-\_r_3cv\_ .node ellipse,#chatgpt-mermaid-\_r_3cv\_ .node polygon,#chatgpt-mermaid-\_r_3cv\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_3cv\_ .rough-node .label text,#chatgpt-mermaid-\_r_3cv\_ .node .label text,#chatgpt-mermaid-\_r_3cv\_ .image-shape .label,#chatgpt-mermaid-\_r_3cv\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_3cv\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_3cv\_ .rough-node .label,#chatgpt-mermaid-\_r_3cv\_ .node .label,#chatgpt-mermaid-\_r_3cv\_ .image-shape .label,#chatgpt-mermaid-\_r_3cv\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_3cv\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_3cv\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3cv\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3cv\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_3cv\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_3cv\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_3cv\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_3cv\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_3cv\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_3cv\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_3cv\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3cv\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3cv\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_3cv\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3cv\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_3cv\_ .icon-shape,#chatgpt-mermaid-\_r_3cv\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_3cv\_ .icon-shape p,#chatgpt-mermaid-\_r_3cv\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_3cv\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_3cv\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_3cv\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_3cv\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_3cv\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_3cv\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_3cv\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_3cv\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_3cv\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3cv\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_3cv\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_3cv\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_3cv\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3cv\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_3cv\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_3cv\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3cv\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_3cv\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_3cv\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3cv\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_3cv\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3cv\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_3cv\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_3cv\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_3cv\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_3cv\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_3cv\_ .node rect,#chatgpt-mermaid-\_r_3cv\_ .node circle,#chatgpt-mermaid-\_r_3cv\_ .node ellipse,#chatgpt-mermaid-\_r_3cv\_ .node polygon,#chatgpt-mermaid-\_r_3cv\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_3cv\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_3cv\_ .mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_3cv\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_3cv\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3cv\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}Global ClientsGlobal Traffic RoutingRegion ARegion BRegion CRedis ARedis BRedis C

Now consider this policy:

```
user_123:

Maximum 100 requests per minute.
```

The user can access our application from different regions.

How do we maintain one global allowance?

This is more difficult than it first appears.

## **7.1 The naive regional implementation is incorrect**

Suppose we maintain independent token buckets in each region.

```
Redis A:
    user_123 = 100 tokens

Redis B:
    user_123 = 100 tokens

Redis C:
    user_123 = 100 tokens
```

Each regional rate limiter independently allows 100 requests.

The aggregate allowance becomes:

\\[ 100+100+100=300 \\]

But our policy allows only 100.

We have recreated the multiple-application-instance problem, but at a geographical level.

Adding more regions must not multiply the global quota.

We need a strategy for distributing the allowance.

# **8. Multi-region strategy 1: Route each identity to its home region**

This is often a good approach for per-user or per-account rate limits.

Suppose:

```
user_123:

Home region = Mumbai
```

Whenever this user makes a request, we route rate-limit validation to the Mumbai region.

Even when the user accesses the application from Singapore or Frankfurt, the authoritative rate-limit state remains in Mumbai.

\#chatgpt-mermaid-\_r_3di\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_3di\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_3di\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_3di\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_3di\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3di\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_3di\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_3di\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_3di\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_3di\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_3di\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_3di\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3di\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3di\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_3di\_ p{margin:0;}#chatgpt-mermaid-\_r_3di\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3di\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3di\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3di\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_3di\_ .label text,#chatgpt-mermaid-\_r_3di\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3di\_ .node rect,#chatgpt-mermaid-\_r_3di\_ .node circle,#chatgpt-mermaid-\_r_3di\_ .node ellipse,#chatgpt-mermaid-\_r_3di\_ .node polygon,#chatgpt-mermaid-\_r_3di\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_3di\_ .rough-node .label text,#chatgpt-mermaid-\_r_3di\_ .node .label text,#chatgpt-mermaid-\_r_3di\_ .image-shape .label,#chatgpt-mermaid-\_r_3di\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_3di\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_3di\_ .rough-node .label,#chatgpt-mermaid-\_r_3di\_ .node .label,#chatgpt-mermaid-\_r_3di\_ .image-shape .label,#chatgpt-mermaid-\_r_3di\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_3di\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_3di\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3di\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3di\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_3di\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_3di\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_3di\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_3di\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_3di\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_3di\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_3di\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3di\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3di\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_3di\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3di\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_3di\_ .icon-shape,#chatgpt-mermaid-\_r_3di\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_3di\_ .icon-shape p,#chatgpt-mermaid-\_r_3di\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_3di\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_3di\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_3di\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_3di\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_3di\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_3di\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_3di\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_3di\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_3di\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3di\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_3di\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_3di\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_3di\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3di\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_3di\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_3di\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3di\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_3di\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_3di\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3di\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_3di\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3di\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_3di\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_3di\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_3di\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_3di\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_3di\_ .node rect,#chatgpt-mermaid-\_r_3di\_ .node circle,#chatgpt-mermaid-\_r_3di\_ .node ellipse,#chatgpt-mermaid-\_r_3di\_ .node polygon,#chatgpt-mermaid-\_r_3di\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_3di\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_3di\_ .mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_3di\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_3di\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3di\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}Request from MumbaiRate Limit RoutingRequest from SingaporeRequest from FrankfurtUser Home Region: MumbaiRedis ARate Limit Decision

Let's examine the flow.

The user sends a request to Singapore.

```
Client
  |
  v
Singapore Application Server
  |
  v
Rate Limiter
  |
  v
Mumbai Redis
  |
  v
Token Bucket Decision
```

All requests for `user_123` consult the same authoritative bucket.

Therefore, they share one allowance.

### **Advantages**

- One authoritative state per identity.
- No duplicated token buckets.
- Straightforward per-user quota enforcement.
- No need to synchronize every request across regions.

### **Disadvantages**

- Cross-region network latency.
- The home region becomes important for that user's rate-limit decisions.
- If the home region is unavailable, we need a failover strategy.
- Heavy traffic from many users whose home region is elsewhere can overload one region.

### **When is this suitable?**

When:

- Each user has a natural home region.
- Most user traffic is reasonably region-local.
- A cross-region rate-limit check is acceptable.
- We want stronger consistency for per-user limits without coordinating every region independently.

This is analogous to routing user data to its authoritative home region in a multi-region database architecture.

# **9. Multi-region strategy 2: Allocate separate quotas to regions**

Sometimes, routing every request to the user's home region is too expensive.

We might prefer each region to make local decisions.

Consider:

```
Global rate limit = 1,000 requests/second
```

We distribute the global allowance:

```
Mumbai:
    500 requests/second

Singapore:
    300 requests/second

Frankfurt:
    200 requests/second
```

Total:

\\[ 500+300+200=1000 \\]

Each region maintains its own local token buckets or another suitable limiter.

\#chatgpt-mermaid-\_r_3e5\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_3e5\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_3e5\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_3e5\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_3e5\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3e5\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_3e5\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_3e5\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_3e5\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_3e5\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_3e5\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_3e5\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3e5\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3e5\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_3e5\_ p{margin:0;}#chatgpt-mermaid-\_r_3e5\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3e5\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3e5\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3e5\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_3e5\_ .label text,#chatgpt-mermaid-\_r_3e5\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3e5\_ .node rect,#chatgpt-mermaid-\_r_3e5\_ .node circle,#chatgpt-mermaid-\_r_3e5\_ .node ellipse,#chatgpt-mermaid-\_r_3e5\_ .node polygon,#chatgpt-mermaid-\_r_3e5\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_3e5\_ .rough-node .label text,#chatgpt-mermaid-\_r_3e5\_ .node .label text,#chatgpt-mermaid-\_r_3e5\_ .image-shape .label,#chatgpt-mermaid-\_r_3e5\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_3e5\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_3e5\_ .rough-node .label,#chatgpt-mermaid-\_r_3e5\_ .node .label,#chatgpt-mermaid-\_r_3e5\_ .image-shape .label,#chatgpt-mermaid-\_r_3e5\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_3e5\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_3e5\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3e5\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3e5\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_3e5\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_3e5\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_3e5\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_3e5\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_3e5\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_3e5\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_3e5\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3e5\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3e5\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_3e5\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3e5\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_3e5\_ .icon-shape,#chatgpt-mermaid-\_r_3e5\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_3e5\_ .icon-shape p,#chatgpt-mermaid-\_r_3e5\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_3e5\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_3e5\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_3e5\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_3e5\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_3e5\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_3e5\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_3e5\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_3e5\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_3e5\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3e5\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_3e5\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_3e5\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_3e5\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3e5\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_3e5\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_3e5\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3e5\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_3e5\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_3e5\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3e5\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_3e5\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3e5\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_3e5\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_3e5\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_3e5\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_3e5\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_3e5\_ .node rect,#chatgpt-mermaid-\_r_3e5\_ .node circle,#chatgpt-mermaid-\_r_3e5\_ .node ellipse,#chatgpt-mermaid-\_r_3e5\_ .node polygon,#chatgpt-mermaid-\_r_3e5\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_3e5\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_3e5\_ .mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_3e5\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_3e5\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3e5\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}Global Quota Coordinator500/sec300/sec200/secRedis MumbaiRedis SingaporeRedis FrankfurtLocal ApplicationsLocal ApplicationsLocal Applications

Now every request can be validated locally.

### **Advantages**

- Very low request-decision latency.
- No cross-region call for every request.
- Regional Redis deployments operate independently.
- Better isolation between regions.

### **Disadvantages**

- Capacity might be unevenly utilized.
- Mumbai may reject requests while Singapore has unused quota.
- Reallocating capacity requires coordination.
- Regional failures can strand quota.

### **Example: Uneven traffic**

Suppose actual traffic is:

```
Mumbai traffic = 700 requests/second

Singapore traffic = 100 requests/second

Frankfurt traffic = 100 requests/second
```

But our allocation is:

```
Mumbai = 500

Singapore = 300

Frankfurt = 200
```

The aggregate demand is only 900 requests/second.

Yet Mumbai rejects 200 requests per second because its local allocation is exhausted.

Meanwhile, the other regions have unused capacity.

This is a utilization problem.

We might solve it by dynamically reallocating quotas.

For example:

```
Mumbai = 700

Singapore = 150

Frankfurt = 150
```

The sum remains 1,000.

But dynamic reallocation must be coordinated carefully so that old and new allocations don't overlap.

# **10. Multi-region strategy 3: Hybrid quota leasing**

Let's combine both approaches.

Instead of centrally checking every request, we maintain regional quota allocations.

Each region then leases smaller budgets to its application instances.

For example:

```
Global rate = 10,000/sec

Region A = 5,000/sec
Region B = 3,000/sec
Region C = 2,000/sec
```

Within Region A:

```
Application 1 = 1,000/sec

Application 2 = 1,000/sec

Application 3 = 1,000/sec

Application 4 = 1,000/sec

Application 5 = 1,000/sec
```

Requests are validated locally.

```
Global Coordinator
        |
        v
  Regional Allocations
        |
        +---------------------+
        |                     |
        v                     v
    Region A               Region B
        |                     |
        v                     v
 Application Instances   Application Instances
        |                     |
        v                     v
   Local Buckets           Local Buckets
```

This is a powerful architecture for large distributed systems.

The global coordinator performs relatively infrequent budget allocation.

Regional coordinators distribute the available budget.

Application instances consume their own reserved portions.

The number of globally coordinated operations is therefore much smaller than the number of incoming requests.

# **11. Can asynchronous Redis replication solve multi-region rate limiting?**

You might ask:

> What if we simply replicate Redis state across all three regions?

For example:

```
Redis Mumbai
     |
     +---- Replicate to Singapore
     |
     +---- Replicate to Frankfurt
```

Then all regions can read and update their local copies.

This sounds convenient, but there's a fundamental problem.

**Asynchronous replication does not guarantee that all regions have the latest rate-limit state when they make a decision.**

Let's see why.

Suppose:

```
Global token balance = 1
```

Two regions have copies:

```
Mumbai Redis:
    tokens = 1

Singapore Redis:
    tokens = 1
```

Two requests arrive simultaneously.

```
Mumbai:
    Reads 1 token.
    Allows request.
    Changes local balance to 0.

Singapore:
    Reads 1 token.
    Allows request.
    Changes local balance to 0.
```

Both regions allowed a request.

The same token was effectively spent twice.

Later, replication might synchronize the values to zero, but it's too late.

We already admitted two requests when only one token existed.

### **Why is this important?**

Replication helps us distribute copies of data.

It does not automatically make independent writes globally atomic.

If our requirement is:

```
Strictly never spend more than the global allowance.
```

Independent regional token buckets with asynchronous replication are insufficient.

We need a coordination strategy.

Possible solutions:

- One authoritative home region.
- A global coordinator that allocates disjoint quota leases.
- Regional budget partitioning.
- A strongly coordinated distributed state system.

### **The tradeoff**

We cannot simultaneously assume:

1. Every region can independently make an immediate local decision.
2. Every region can operate completely independently during network partitions.
3. A strict global quota will never be exceeded.

Without some form of coordination or pre-allocated disjoint budgets, these guarantees conflict.

This is one of the most important lessons in distributed rate limiting.

# **12. What happens when a user moves between regions?**

Let's consider a scenario that combines our multi-region concepts.

Suppose:

```
user_123

Home region = Mumbai

Available tokens = 20
```

The user travels and starts sending requests from Singapore.

What happens?

### **Option A: Continue using the home region**

Singapore routes rate-limit checks to Mumbai.

```
Singapore Application
        |
        v
Mumbai Rate Limiter
        |
        v
Mumbai Redis
```

The user's remaining tokens stay authoritative in Mumbai.

This is simple from a correctness perspective, but adds network latency.

### **Option B: Transfer ownership to Singapore**

We decide to move the authoritative bucket.

Initially:

```
Mumbai:
    tokens = 20
```

We need to transfer its state:

```
Singapore:
    tokens = 20
```

But we must not allow both regions to continue accepting requests using those 20 tokens.

A safe ownership migration might look like:

```
1. Stop issuing new decisions from Mumbai.

2. Complete or fence existing operations.

3. Transfer the latest bucket state.

4. Establish Singapore as the new owner.

5. Allow Singapore to process requests.
```

We need ownership generations or fencing tokens so that an old Mumbai owner cannot continue making valid decisions after the transfer.

### **Option C: Allocate independent regional sub-budgets**

Instead of moving one bucket, we can assign:

```
Mumbai:
    10 tokens

Singapore:
    10 tokens
```

The total remains 20.

Each region can operate independently within its allocation.

But now capacity is fragmented.

A user might have exhausted the Singapore allocation while still having 10 tokens available in Mumbai.

We can rebalance the allocations through coordination.

### **What would I choose?**

For ordinary per-user API rate limiting, I would generally prefer:

- A home-region authoritative bucket when stronger per-user consistency is important.
- Regional quota leasing when latency and availability are more important, and some capacity fragmentation is acceptable.

The right choice depends on the guarantees required by the API.

# **13. A very important distinction: Global limits versus regional limits**

These two policies may sound similar but are fundamentally different.

**Policy A: Per-region limit**

```
Mumbai:
    1,000 requests/second

Singapore:
    1,000 requests/second

Frankfurt:
    1,000 requests/second
```

Aggregate possible allowance:

\\[ 1000 \times 3 = 3000 \\]

This is perfectly correct if the requirement is 1,000 per region.

**Policy B: Global limit**

```
All regions combined:
    1,000 requests/second
```

Now the regional allowances must be coordinated or partitioned.

For example:

```
Mumbai = 500

Singapore = 300

Frankfurt = 200
```

Aggregate:

\\[ 500+300+200=1000 \\]

Do not confuse these policies during system design interviews.

An application may legitimately have both:

```
Regional limit:
    1,000 requests/second per region.

Global limit:
    2,000 requests/second across all regions.
```

One protects an individual region.

The other protects the overall system.

# **14. What consistency can we realistically guarantee?**

Let's summarize the tradeoffs.

| Architecture                                       | Latency                      | Consistency characteristics                | Main tradeoff                                |
| -------------------------------------------------- | ---------------------------- | ------------------------------------------ | -------------------------------------------- |
| Single authoritative Redis primary                 | Depends on request distance  | Atomic decisions at that primary           | Central dependency and potential bottleneck  |
| Home-region routing                                | Low locally, higher remotely | One authoritative bucket per identity      | Cross-region calls                           |
| Independent regional buckets with replicated state | Low                          | May over-admit due to replication lag      | High availability, weaker global correctness |
| Static regional quota allocation                   | Low                          | Global allowance bounded by allocation sum | Unused quota can be stranded                 |
| Dynamic quota leasing                              | Low for local checks         | Bounded by correctly managed grants        | Lease and ownership complexity               |
| Strongly coordinated global state                  | Higher coordination cost     | Can provide stronger global guarantees     | Availability, latency, and throughput costs  |

An important point:

The correctness guarantee also depends on what we do during failover.

A Redis primary may process operations atomically, but its replication and failover behavior must be considered separately.

Acknowledged writes can potentially be lost during an asynchronous failover, depending on configuration and failure timing.

For strict financial or contractual quotas, Redis-based best-effort coordination might not be a sufficient source of truth.

We'll examine that in the reliability chapter.

# **15. Which multi-region strategy should we choose?**

Let's make the decision practical.

| Requirement                               | Recommended approach                                       |
| ----------------------------------------- | ---------------------------------------------------------- |
| Per-user limit, user has a home region    | Home-region authoritative bucket                           |
| Per-user limit with low local latency     | Regional quota leases                                      |
| Global aggregate service limit            | Hierarchical quota allocation                              |
| Independent regional protection           | Regional Redis + regional limiter                          |
| Strict global quota                       | Central coordination or disjoint quota allocation          |
| High traffic with very low Redis overhead | Local token quotas leased from a coordinator               |
| Best-effort abuse protection              | Regional or local rate limiting with bounded approximation |

The key is to choose the consistency guarantee before selecting the infrastructure.

Don't start with:

> Let's use Redis in every region.

Start with:

> What does the rate-limit policy guarantee, and what amount of over-admission, latency, or wasted capacity is acceptable?

That determines the architecture.

# **16. What have we learned in Part 2?**

We've evolved the distributed architecture through several stages.

\#chatgpt-mermaid-\_r_3h6\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_3h6\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_3h6\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_3h6\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_3h6\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3h6\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_3h6\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_3h6\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_3h6\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_3h6\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_3h6\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_3h6\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3h6\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3h6\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_3h6\_ p{margin:0;}#chatgpt-mermaid-\_r_3h6\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3h6\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3h6\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3h6\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_3h6\_ .label text,#chatgpt-mermaid-\_r_3h6\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3h6\_ .node rect,#chatgpt-mermaid-\_r_3h6\_ .node circle,#chatgpt-mermaid-\_r_3h6\_ .node ellipse,#chatgpt-mermaid-\_r_3h6\_ .node polygon,#chatgpt-mermaid-\_r_3h6\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_3h6\_ .rough-node .label text,#chatgpt-mermaid-\_r_3h6\_ .node .label text,#chatgpt-mermaid-\_r_3h6\_ .image-shape .label,#chatgpt-mermaid-\_r_3h6\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_3h6\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_3h6\_ .rough-node .label,#chatgpt-mermaid-\_r_3h6\_ .node .label,#chatgpt-mermaid-\_r_3h6\_ .image-shape .label,#chatgpt-mermaid-\_r_3h6\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_3h6\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_3h6\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3h6\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3h6\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_3h6\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_3h6\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_3h6\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_3h6\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_3h6\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_3h6\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_3h6\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3h6\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3h6\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_3h6\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3h6\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_3h6\_ .icon-shape,#chatgpt-mermaid-\_r_3h6\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_3h6\_ .icon-shape p,#chatgpt-mermaid-\_r_3h6\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_3h6\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_3h6\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_3h6\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_3h6\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_3h6\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_3h6\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_3h6\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_3h6\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_3h6\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3h6\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_3h6\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_3h6\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_3h6\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3h6\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_3h6\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_3h6\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3h6\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_3h6\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_3h6\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3h6\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_3h6\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3h6\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_3h6\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_3h6\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_3h6\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_3h6\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_3h6\_ .node rect,#chatgpt-mermaid-\_r_3h6\_ .node circle,#chatgpt-mermaid-\_r_3h6\_ .node ellipse,#chatgpt-mermaid-\_r_3h6\_ .node polygon,#chatgpt-mermaid-\_r_3h6\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_3h6\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_3h6\_ .mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_3h6\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_3h6\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3h6\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}Single Redis InstanceRedis ClusterDistribute Different KeysIdentify Hot KeysPartition or Lease QuotaHierarchical Rate LimitingMulti-region Rate Limiting

The important concepts:

**1. Redis Cluster distributes keys, not requests to one key.**

A hot key can overload one Redis primary regardless of how many other primaries exist.

**2. Global limits are harder than per-user limits.**

A global counter accessed by every request can become a bottleneck.

**3. Quota leasing reduces central coordination.**

We allocate disjoint pieces of a global budget to application instances or regions.

**4. Partitioning requires dividing the actual budget.**

Giving every partition the original full allowance multiplies the aggregate limit.

**5. Multi-region consistency requires deliberate coordination.**

Asynchronous replication alone cannot guarantee that two regions won't spend the same allowance.

**6. Strict global correctness has a cost.**

It may require central coordination, pre-allocated quotas, or a stronger distributed state mechanism.

# **Next Chapter — Chapter 7, Part 3: Failure Handling and Production Reliability**

Now we need to examine what happens when the infrastructure itself fails.

Imagine:

```
Our application is healthy.

The backend is healthy.

The database is healthy.

But Redis is unavailable.
```

Can our application continue?

Should it allow requests or reject them?

What happens if Redis fails over and loses recently acknowledged rate-limit updates?

How do we prevent an attacker from exploiting a fail-open policy?

We'll explore:

1. **Fail-open vs fail-closed** — choosing the right behavior when the Rate Limiter is unavailable.
2. **Redis failover and lost writes** — how replication lag can cause rate-limit violations.
3. **Timeouts and circuit breakers** — preventing rate-limit infrastructure failures from cascading.
4. **Local fallback rate limiting** — protecting the application when shared Redis is unavailable.
5. **Retries and duplicate decisions** — handling uncertain outcomes safely.
6. **Monitoring and observability** — detecting hot keys, excessive rejections, and Redis bottlenecks.
7. **Production deployment architecture** — tying everything together.

The central question will be:

**How do we make the Rate Limiter reliable enough that it doesn't become the very thing that takes down the application it's supposed to protect?**








































# **Chapter 7 — Part 3: Failure Handling and Production Reliability**

So far, we've built a distributed Rate Limiter using:

- Multiple application instances.
- Shared Redis.
- Atomic Lua scripts.
- Redis Cluster.
- Distributed quota allocation.
- Multi-region coordination.

Our architecture looks reasonably good.

But imagine this situation:

```
Client
   |
   v
API Gateway
   |
   v
Backend Application
   |
   v
Rate Limiter
   |
   v
Redis
```

Suddenly, Redis becomes unavailable.

Our backend is healthy.

Our database is healthy.

Our application logic is healthy.

But we cannot determine whether an incoming request should be allowed.

What should happen?

Should we reject every request?

Should we allow everything?

Should we use an in-memory fallback?

What if Redis comes back but has lost some recent rate-limit updates?

These are not minor implementation details.

**A production Rate Limiter must be designed for failure, not just for successful request validation.**

Let's work through these problems one by one.

# **1. What happens when Redis becomes unavailable?**

Consider our policy:

```
Per-user limit:

100 requests/second
```

Our backend performs a Redis call for every request.

```
Application Server
       |
       v
      Redis
       |
       v
Rate Limit Decision
```

Now Redis becomes unavailable.

The application cannot execute its atomic rate-limit operation.

It doesn't know:

- How many requests the user has already made.
- How many tokens remain.
- Whether the current window has expired.
- Whether the user's quota is exhausted.

We have lost the ability to make an authoritative decision.

There are two obvious choices.

## **1.1 Option A: Fail Open**

Fail Open means:

> When the Rate Limiter is unavailable, allow the request.

Our flow becomes:

\#chatgpt-mermaid-\_r_3j0\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_3j0\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_3j0\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_3j0\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_3j0\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3j0\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_3j0\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_3j0\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_3j0\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_3j0\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_3j0\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_3j0\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3j0\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3j0\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_3j0\_ p{margin:0;}#chatgpt-mermaid-\_r_3j0\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3j0\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3j0\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3j0\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_3j0\_ .label text,#chatgpt-mermaid-\_r_3j0\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3j0\_ .node rect,#chatgpt-mermaid-\_r_3j0\_ .node circle,#chatgpt-mermaid-\_r_3j0\_ .node ellipse,#chatgpt-mermaid-\_r_3j0\_ .node polygon,#chatgpt-mermaid-\_r_3j0\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_3j0\_ .rough-node .label text,#chatgpt-mermaid-\_r_3j0\_ .node .label text,#chatgpt-mermaid-\_r_3j0\_ .image-shape .label,#chatgpt-mermaid-\_r_3j0\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_3j0\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_3j0\_ .rough-node .label,#chatgpt-mermaid-\_r_3j0\_ .node .label,#chatgpt-mermaid-\_r_3j0\_ .image-shape .label,#chatgpt-mermaid-\_r_3j0\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_3j0\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_3j0\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3j0\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3j0\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_3j0\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_3j0\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_3j0\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_3j0\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_3j0\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_3j0\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_3j0\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3j0\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3j0\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_3j0\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3j0\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_3j0\_ .icon-shape,#chatgpt-mermaid-\_r_3j0\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_3j0\_ .icon-shape p,#chatgpt-mermaid-\_r_3j0\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_3j0\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_3j0\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_3j0\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_3j0\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_3j0\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_3j0\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_3j0\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_3j0\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_3j0\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3j0\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_3j0\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_3j0\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_3j0\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3j0\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_3j0\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_3j0\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3j0\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_3j0\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_3j0\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3j0\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_3j0\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3j0\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_3j0\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_3j0\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_3j0\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_3j0\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_3j0\_ .node rect,#chatgpt-mermaid-\_r_3j0\_ .node circle,#chatgpt-mermaid-\_r_3j0\_ .node ellipse,#chatgpt-mermaid-\_r_3j0\_ .node polygon,#chatgpt-mermaid-\_r_3j0\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_3j0\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_3j0\_ .mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_3j0\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_3j0\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3j0\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}Incoming RequestRate LimiterRedis Available?Perform Rate Limit CheckAllowed?BackendHTTP 429Allow RequestYesYesNoNo

Imagine Redis is unavailable for 30 seconds.

During those 30 seconds, requests continue reaching the backend.

### **Advantages**

**1. Application availability is preserved.**

A Rate Limiter outage doesn't automatically become an application outage.

**2. Temporary infrastructure problems don't block legitimate customers.**

This can be important for less critical APIs.

**3. The Rate Limiter doesn't become a single point of failure for all application traffic.**

The business service can continue operating.

### **Disadvantages**

**1. Rate-limit policies are temporarily bypassed.**

An attacker could exploit the outage to generate excessive traffic.

**2. Downstream systems may become overloaded.**

The very resource we were trying to protect may now be exposed.

**3. Usage quotas may be exceeded.**

Customers could consume more than their subscription allows.

Consider:

```
Monthly quota = 1,000,000 requests

Redis unavailable

Additional requests are admitted
```

When Redis recovers, we might discover that the customer has consumed substantially more than the quota.

### **When is Fail Open appropriate?**

For example:

```
Non-critical product browsing API
Optional analytics endpoint
Low-risk internal API
```

A temporary bypass may be preferable to making the entire application unavailable.

However, even for these endpoints, we should consider a bounded local fallback rather than automatically allowing unlimited traffic.

## **1.2 Option B: Fail Closed**

Fail Closed means:

> When the Rate Limiter is unavailable, reject the request.

Our flow:

\#chatgpt-mermaid-\_r_3jf\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_3jf\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_3jf\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_3jf\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_3jf\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3jf\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_3jf\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_3jf\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_3jf\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_3jf\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_3jf\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_3jf\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3jf\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3jf\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_3jf\_ p{margin:0;}#chatgpt-mermaid-\_r_3jf\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3jf\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3jf\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3jf\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_3jf\_ .label text,#chatgpt-mermaid-\_r_3jf\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3jf\_ .node rect,#chatgpt-mermaid-\_r_3jf\_ .node circle,#chatgpt-mermaid-\_r_3jf\_ .node ellipse,#chatgpt-mermaid-\_r_3jf\_ .node polygon,#chatgpt-mermaid-\_r_3jf\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_3jf\_ .rough-node .label text,#chatgpt-mermaid-\_r_3jf\_ .node .label text,#chatgpt-mermaid-\_r_3jf\_ .image-shape .label,#chatgpt-mermaid-\_r_3jf\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_3jf\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_3jf\_ .rough-node .label,#chatgpt-mermaid-\_r_3jf\_ .node .label,#chatgpt-mermaid-\_r_3jf\_ .image-shape .label,#chatgpt-mermaid-\_r_3jf\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_3jf\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_3jf\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3jf\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3jf\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_3jf\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_3jf\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_3jf\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_3jf\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_3jf\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_3jf\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_3jf\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3jf\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3jf\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_3jf\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3jf\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_3jf\_ .icon-shape,#chatgpt-mermaid-\_r_3jf\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_3jf\_ .icon-shape p,#chatgpt-mermaid-\_r_3jf\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_3jf\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_3jf\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_3jf\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_3jf\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_3jf\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_3jf\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_3jf\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_3jf\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_3jf\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3jf\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_3jf\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_3jf\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_3jf\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3jf\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_3jf\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_3jf\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3jf\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_3jf\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_3jf\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3jf\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_3jf\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3jf\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_3jf\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_3jf\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_3jf\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_3jf\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_3jf\_ .node rect,#chatgpt-mermaid-\_r_3jf\_ .node circle,#chatgpt-mermaid-\_r_3jf\_ .node ellipse,#chatgpt-mermaid-\_r_3jf\_ .node polygon,#chatgpt-mermaid-\_r_3jf\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_3jf\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_3jf\_ .mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_3jf\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_3jf\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3jf\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}Incoming RequestRate LimiterRedis Available?Perform Rate Limit CheckAllowed?BackendHTTP 429Reject / Service UnavailableYesYesNoNo

Imagine Redis is unavailable.

Instead of bypassing the limiter, we reject requests.

Depending on the API contract, an appropriate response may be:

```
HTTP/1.1 503 Service Unavailable
Retry-After: 2
```

Why 503 instead of 429?

Because the request might not actually have exceeded the user's quota.

Our rate-limit infrastructure is unavailable, so we cannot determine whether the request should be admitted.

We should distinguish:

```
429:
    Request exceeded its rate limit.

503:
    Rate-limit infrastructure is unavailable.
```

### **Advantages**

**1. Protects downstream resources.**

Requests cannot bypass the configured admission controls.

**2. Avoids uncontrolled quota consumption.**

We don't accidentally allow unlimited usage during an outage.

**3. Useful for security-sensitive operations.**

For example, authentication protection, sensitive exports, or expensive operations.

### **Disadvantages**

**1. Application availability becomes dependent on Redis availability.**

Even a perfectly healthy application might reject requests.

**2. Redis outages can become application outages.**

This can create a large customer impact.

**3. Recovery might cause traffic spikes.**

When Redis comes back, all clients may retry simultaneously.

We need retry management and backoff.

## **1.3 Fail Open vs Fail Closed**

Let's compare them.

| Property                          | Fail Open      | Fail Closed                       |
| --------------------------------- | -------------- | --------------------------------- |
| Redis unavailable                 | Allow requests | Reject requests                   |
| Application availability          | Higher         | Lower                             |
| Protection against overload       | Weaker         | Stronger                          |
| Risk of quota violation           | Higher         | Lower                             |
| Risk of blocking legitimate users | Lower          | Higher                            |
| Typical use                       | Low-risk APIs  | Sensitive or expensive operations |

But in production, we don't necessarily need one global strategy.

We can choose the behavior per policy.

For example:

| API / Operation               | Failure behavior                          |
| ----------------------------- | ----------------------------------------- |
| Product browsing              | Bounded fallback or fail open             |
| Authentication attempts       | Fail closed or conservative fallback      |
| Payment processing protection | Conservative fallback                     |
| Expensive report generation   | Fail closed                               |
| Optional analytics            | Fail open                                 |
| Third-party API calls         | Conservative local fallback or defer jobs |

The right decision depends on what happens when the limit is bypassed.

**The most important question is: What is more dangerous for this particular operation — temporarily allowing excess traffic or rejecting legitimate requests?**

# **2. Better solution: Local fallback Rate Limiter**

There is another option between allowing everything and rejecting everything.

When Redis becomes unavailable, each application instance can temporarily use an in-memory Rate Limiter.

This is called a local fallback or emergency limiter.

Imagine:

```
                 Redis
                   |
                   X
               UNAVAILABLE


      +------------+------------+
      |            |            |
      v            v            v
   Server 1     Server 2     Server 3
   Local        Local        Local
   Limiter      Limiter      Limiter
```

Each application instance has a local fallback policy.

For example:

```
Normal distributed policy:

100 requests/second per user.

Emergency fallback:

10 requests/second per application instance.
```

Now even though the shared rate-limit state is unavailable, we still restrict traffic locally.

### **2.1 Why not just use the original full limit locally?**

Suppose:

```
Global user limit = 100 requests/second

Application instances = 10
```

If every application instance independently allows 100 requests/second, our aggregate fallback allowance becomes:

\\[ 10 \times 100 = 1000 \\]

We have multiplied the intended limit by ten.

So we need a conservative fallback allocation.

For example:

```
Normal global limit = 100/sec

Instances = 10

Local emergency allowance = 5/sec
```

The aggregate fallback allowance is approximately:

\\[ 10 \times 5 = 50 \\]

We have intentionally limited traffic to below the normal allowance.

This sacrifices utilization for protection.

However, note that this calculation assumes ten stable instances and that each enforces its local allowance correctly. If autoscaling changes the instance count, the aggregate allowance changes too.

A fixed per-instance fallback cannot guarantee a strict global limit when the number of instances is dynamic.

For critical global limits, you need preallocated regional or instance quotas that aren't multiplied when instances scale out.

### **2.2 What state does the local limiter need?**

It can be a simple in-memory Token Bucket:

```
user_123:

    tokens = 5
    last_refill_time = ...
```

No Redis access is necessary while the fallback is active.

But the fallback should be deliberately limited:

```
Fallback enabled = true

Fallback capacity = 10

Fallback refill rate = 5/sec
```

We should not accidentally grant customers their full global budget independently on every server.

### **2.3 When do we activate the fallback?**

We shouldn't activate it for one failed request.

A reasonable flow is:

```
Request arrives
     |
     v
Try Redis
     |
     v
Redis timeout
     |
     v
Failure threshold reached?
     |
     +---- No ----> Apply normal error handling
     |
     +---- Yes ---> Open circuit breaker
                         |
                         v
                  Activate fallback
```

We can use a circuit breaker to avoid repeatedly calling a failing Redis dependency.

When Redis recovers, we gradually resume normal distributed validation.

One caveat: local fallback state is not the same as Redis state. It cannot reconstruct all requests admitted during the outage. We should treat it as a temporary emergency protection mechanism, not as a continuation of exactly the same global quota.

# **3. What happens when Redis fails over and loses recent writes?**

This is a subtle but extremely important problem.

We previously discussed Redis Cluster with primary and replica nodes.

Let's understand how replication affects rate limiting.

Imagine:

\#chatgpt-mermaid-\_r_3l7\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_3l7\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_3l7\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_3l7\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_3l7\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3l7\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_3l7\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_3l7\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_3l7\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_3l7\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_3l7\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_3l7\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3l7\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3l7\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_3l7\_ p{margin:0;}#chatgpt-mermaid-\_r_3l7\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3l7\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3l7\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3l7\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_3l7\_ .label text,#chatgpt-mermaid-\_r_3l7\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3l7\_ .node rect,#chatgpt-mermaid-\_r_3l7\_ .node circle,#chatgpt-mermaid-\_r_3l7\_ .node ellipse,#chatgpt-mermaid-\_r_3l7\_ .node polygon,#chatgpt-mermaid-\_r_3l7\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_3l7\_ .rough-node .label text,#chatgpt-mermaid-\_r_3l7\_ .node .label text,#chatgpt-mermaid-\_r_3l7\_ .image-shape .label,#chatgpt-mermaid-\_r_3l7\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_3l7\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_3l7\_ .rough-node .label,#chatgpt-mermaid-\_r_3l7\_ .node .label,#chatgpt-mermaid-\_r_3l7\_ .image-shape .label,#chatgpt-mermaid-\_r_3l7\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_3l7\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_3l7\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3l7\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3l7\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_3l7\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_3l7\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_3l7\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_3l7\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_3l7\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_3l7\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_3l7\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3l7\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3l7\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_3l7\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3l7\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_3l7\_ .icon-shape,#chatgpt-mermaid-\_r_3l7\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_3l7\_ .icon-shape p,#chatgpt-mermaid-\_r_3l7\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_3l7\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_3l7\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_3l7\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_3l7\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_3l7\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_3l7\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_3l7\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_3l7\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_3l7\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3l7\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_3l7\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_3l7\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_3l7\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3l7\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_3l7\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_3l7\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3l7\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_3l7\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_3l7\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3l7\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_3l7\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3l7\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_3l7\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_3l7\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_3l7\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_3l7\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_3l7\_ .node rect,#chatgpt-mermaid-\_r_3l7\_ .node circle,#chatgpt-mermaid-\_r_3l7\_ .node ellipse,#chatgpt-mermaid-\_r_3l7\_ .node polygon,#chatgpt-mermaid-\_r_3l7\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_3l7\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_3l7\_ .mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_3l7\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_3l7\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3l7\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}ApplicationRedis PrimaryRedis Replica

Our primary handles rate-limit operations.

The replica receives replicated updates.

When the primary fails, the replica can be promoted.

But there's an important detail:

**Redis replication is asynchronous by default.**

This means the primary can acknowledge a write before the replica has received it.

Let's see the consequences.

## **3.1 An example of lost rate-limit state**

Suppose a Token Bucket contains:

```
tokens = 10
```

A request arrives.

The primary executes the atomic Lua script:

```
tokens = 9
```

The primary responds:

```
ALLOW
```

But before the write reaches the replica, the primary crashes.

Now the replica is promoted.

Its state is:

```
tokens = 10
```

The recently acknowledged decrement has been lost.

The next request arrives.

The new primary sees:

```
tokens = 10
```

It allows another request.

We have effectively granted an extra token.

This is a consequence of the durability and replication model, not an error in the Lua script.

Lua atomicity ensured that the update was indivisible on the old primary.

It did not guarantee that the update had already reached the replica before failover.

## **3.2 Why is this particularly problematic for Token Bucket?**

Imagine:

```
Capacity = 100

Refill rate = 10/second
```

A user consumes all 100 tokens.

The state should be:

```
tokens = 0
```

But suppose a failover occurs and restores an older state:

```
tokens = 80
```

The user suddenly receives 80 tokens that they had already consumed.

That's an unexpected burst.

Similarly, losing updates in Fixed Window can cause the counter to move backwards.

For example:

```
Actual count = 100

Restored count = 80
```

The next 20 requests may be incorrectly allowed again.

For Sliding Window Log, losing recent timestamp entries may cause old request history to disappear.

For Sliding Window Counter, losing recent counter increments may lower the estimated usage.

All five algorithms can be affected by losing acknowledged state.

# **4. How can we reduce the risk of losing Redis writes?**

There are several mechanisms, and they provide different guarantees.

Let's distinguish them.

## **4.1 Redis replication acknowledgement using WAIT**

Redis provides a command:

```
WAIT number_of_replicas timeout
```

It waits until the preceding writes from the current client connection have been acknowledged by the specified number of replicas, or until the timeout expires.

For example:

```
WAIT 1 100
```

This requests acknowledgement from at least one replica, waiting up to 100 milliseconds.

The idea is:

```
Application
    |
    v
Redis Primary
    |
    v
Write state
    |
    v
Wait for replica acknowledgement
    |
    v
Return result
```

This reduces the likelihood that an acknowledged operation will be lost during failover.

But there is a critical qualification.

**WAIT does not turn Redis into a strongly consistent consensus-based database.**

A replica acknowledgement does not mean the write is durably fsynced, nor does it guarantee that the acknowledged replica will necessarily be the one promoted.

There are still failure scenarios in which an acknowledged write can be lost.

For a rate limiter, WAIT may be a useful durability improvement, but it introduces additional latency.

Also, it is not automatically a complete solution to every deployment's failover guarantee.

## **4.2 Redis persistence using AOF**

Redis supports persistence mechanisms such as:

- RDB snapshots.
- Append Only File (AOF).

AOF records write operations so that Redis can reconstruct state after a restart.

A particularly important configuration is:

```
appendfsync everysec
```

This attempts to synchronize AOF data to disk approximately once per second.

There is a potential loss window if the process or machine crashes before the latest data is synchronized.

Another option is:

```
appendfsync always
```

This requests synchronization after each write operation, providing stronger local durability at a greater performance cost.

However, local persistence and replica failover are separate matters.

Making the primary's AOF durable does not automatically make an out-of-date replica safe to promote.

## **4.3 Redis Cluster and failover settings**

Redis offers additional replication and failover controls, but they must be understood within the actual deployment.

For example:

- Replica health.
- Minimum replica configuration.
- Replication acknowledgement.
- Failover eligibility.
- Persistence configuration.
- Network partitions.

These mechanisms can reduce the likelihood of unsafe failovers.

They do not make all Redis configurations strongly consistent.

### **What should we do for a normal API Rate Limiter?**

For an ordinary rate limiter whose purpose is overload protection, occasional quota over-admission might be acceptable.

We can use:

```
Redis replication
+
Appropriate persistence
+
Monitoring
+
Failover recovery
```

For a financially critical quota where exceeding the allowance is unacceptable, we need a stronger accounting design.

Possibilities include:

- Durable transactional counters.
- A strongly coordinated quota service.
- Preallocated disjoint quota leases.
- A database-backed authoritative usage ledger.

Don't assume that Redis Lua atomicity alone guarantees durable global accounting.

# **5. What should happen to the rate-limit state after Redis recovers?**

Suppose Redis was unavailable for 30 seconds.

We used local fallback limiters during that period.

Then Redis recovers.

Can we simply return to normal?

Not quite.

We have two separate states:

```
Redis state:

The state before the outage.


Local fallback state:

The requests admitted during the outage.
```

They are not synchronized.

For example:

```
Before outage:

Redis tokens = 100

During outage:

Local limiter allows 50 requests.

Redis tokens remain unchanged.
```

When Redis recovers:

```
Redis still thinks:

tokens = 100
```

But the user has already received permission to send 50 requests during the outage.

We risk allowing another 100 requests without accounting for those 50.

### **Possible recovery strategies**

**Strategy A: Accept temporary over-admission**

For low-risk traffic, we might simply resume normal Redis enforcement.

This is simple but can exceed the intended quota.

**Strategy B: Account for fallback usage**

If we recorded fallback admissions durably or in a reliably collected local usage log, we could reconcile them.

However, reconciliation is complicated:

- The local instance might crash before reporting usage.
- Usage reports might be duplicated.
- The Redis state may already have replenished tokens.
- The fallback may have covered multiple different policies.

**Strategy C: Reset into a conservative state**

After recovery, we can initialize the bucket with less available capacity.

For example:

```
Normal capacity = 100

Recovery starting tokens = 20
```

This reduces the chance of an immediate burst.

But it doesn't reconstruct the exact quota consumed during the outage.

**Strategy D: Use reserved local quota**

This is stronger.

Instead of letting local fallback instances independently grant extra permission, we preallocate a disjoint emergency quota.

For example:

```
Normal global quota = 1,000

Emergency allocations:

Server 1 = 50
Server 2 = 50
Server 3 = 50
```

These quotas are already reserved and accounted for in the global budget.

If Redis becomes unavailable, each server can consume only its reserved allocation.

This avoids independently multiplying the full global quota.

### **Practical conclusion**

The best recovery design depends on the required consistency.

For ordinary API protection:

```
Local conservative fallback
+
Temporary recovery behavior
+
Monitoring
```

may be perfectly reasonable.

For strict global quotas:

```
Preallocated quota leases
+
Durable accounting
+
Safe recovery
```

are much more appropriate.

# **6. Timeouts and Circuit Breakers**

Now let's examine another failure mode.

Redis is not completely down.

But its response time increases.

For example:

```
Normal Redis latency = 2ms

Current Redis latency = 3 seconds
```

What happens to our backend?

Every incoming request waits for Redis.

Suppose our application receives:

```
10,000 requests/second.
```

If every request waits several seconds, we accumulate a large number of waiting operations.

This can exhaust:

- HTTP request threads.
- Connection pools.
- Event-loop capacity.
- Memory.
- Outbound Redis connections.

Eventually, Redis latency can cause our application to fail too.

This is called a cascading failure.

## **6.1 Always use bounded timeouts**

We should configure reasonable timeouts for rate-limit operations.

For example:

```
Redis connection timeout = 10ms

Redis operation timeout = 20ms
```

These are illustrative values, not universal defaults.

The actual values should be selected based on:

- Normal Redis latency.
- Application latency budget.
- Network conditions.
- Retry behavior.
- Expected traffic.

The important point is that we should not let an incoming HTTP request wait indefinitely for a rate-limit decision.

## **6.2 Introduce a Circuit Breaker**

A circuit breaker monitors failures of a dependency.

It has three common states.

**Closed**

Requests are allowed to call Redis.

```
Application -> Redis
```

Failures are monitored.

**Open**

Redis is considered unhealthy.

We stop sending normal requests to it temporarily.

```
Application -> Local fallback / failure policy
```

This prevents us from continuously waiting for an unhealthy dependency.

**Half Open**

After a cooldown period, we allow a small number of test calls.

If they succeed, we return to Closed.

If they fail, we return to Open.

\#chatgpt-mermaid-\_r_3oq\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_3oq\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_3oq\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_3oq\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_3oq\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3oq\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_3oq\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_3oq\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_3oq\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_3oq\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_3oq\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_3oq\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3oq\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3oq\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_3oq\_ p{margin:0;}#chatgpt-mermaid-\_r_3oq\_ defs [id$="-barbEnd"]{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3oq\_ g.stateGroup text{fill:rgb(31, 78, 148);stroke:none;font-size:10px;}#chatgpt-mermaid-\_r_3oq\_ g.stateGroup text{fill:rgb(237, 237, 237);stroke:none;font-size:10px;}#chatgpt-mermaid-\_r_3oq\_ g.stateGroup .state-title{font-weight:bolder;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3oq\_ g.stateGroup rect{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_3oq\_ g.stateGroup line{stroke:rgb(175, 175, 175);stroke-width:1;}#chatgpt-mermaid-\_r_3oq\_ .transition{stroke:rgb(175, 175, 175);stroke-width:1;fill:none;}#chatgpt-mermaid-\_r_3oq\_ .stateGroup .composit{fill:transparent;border-bottom:1px;}#chatgpt-mermaid-\_r_3oq\_ .stateGroup .alt-composit{fill:#e0e0e0;border-bottom:1px;}#chatgpt-mermaid-\_r_3oq\_ .state-note{stroke:rgb(58, 132, 63);fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_3oq\_ .state-note text{fill:rgb(237, 237, 237);stroke:none;font-size:10px;}#chatgpt-mermaid-\_r_3oq\_ .stateLabel .box{stroke:none;stroke-width:0;fill:rgb(9, 23, 44);opacity:0.5;}#chatgpt-mermaid-\_r_3oq\_ .edgeLabel .label rect{fill:rgb(9, 23, 44);opacity:0.5;}#chatgpt-mermaid-\_r_3oq\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_3oq\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_3oq\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_3oq\_ .edgeLabel .label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3oq\_ .label div .edgeLabel{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3oq\_ .stateLabel text{fill:rgb(237, 237, 237);font-size:10px;font-weight:bold;}#chatgpt-mermaid-\_r_3oq\_ .node circle.state-start{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3oq\_ .node .fork-join{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3oq\_ .node circle.state-end{fill:rgb(31, 78, 148);stroke:transparent;stroke-width:1.5;}#chatgpt-mermaid-\_r_3oq\_ .end-state-inner{fill:transparent;stroke-width:1.5;}#chatgpt-mermaid-\_r_3oq\_ .node rect{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_3oq\_ .node polygon{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_3oq\_ [id$="-barbEnd"]{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3oq\_ .statediagram-cluster rect{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_3oq\_ .cluster-label,#chatgpt-mermaid-\_r_3oq\_ .nodeLabel{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3oq\_ .statediagram-cluster rect.outer{rx:5px;ry:5px;}#chatgpt-mermaid-\_r_3oq\_ .statediagram-state .divider{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_3oq\_ .statediagram-state .title-state{rx:5px;ry:5px;}#chatgpt-mermaid-\_r_3oq\_ .statediagram-cluster.statediagram-cluster .inner{fill:transparent;}#chatgpt-mermaid-\_r_3oq\_ .statediagram-cluster.statediagram-cluster-alt .inner{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_3oq\_ .statediagram-cluster .inner{rx:0;ry:0;}#chatgpt-mermaid-\_r_3oq\_ .statediagram-state rect.basic{rx:5px;ry:5px;}#chatgpt-mermaid-\_r_3oq\_ .statediagram-state rect.divider{stroke-dasharray:10,10;fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_3oq\_ .note-edge{stroke-dasharray:5;}#chatgpt-mermaid-\_r_3oq\_ .statediagram-note rect{fill:rgb(48, 48, 48);stroke:rgb(58, 132, 63);stroke-width:1px;rx:0;ry:0;}#chatgpt-mermaid-\_r_3oq\_ .statediagram-note rect{fill:rgb(48, 48, 48);stroke:rgb(58, 132, 63);stroke-width:1px;rx:0;ry:0;}#chatgpt-mermaid-\_r_3oq\_ .statediagram-note text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3oq\_ .statediagram-note .nodeLabel{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3oq\_ .statediagram .edgeLabel{color:red;}#chatgpt-mermaid-\_r_3oq\_ [id$="-dependencyStart"],#chatgpt-mermaid-\_r_3oq\_ [id$="-dependencyEnd"]{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);stroke-width:1;}#chatgpt-mermaid-\_r_3oq\_ .statediagramTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3oq\_ [data-look="neo"].statediagram-cluster rect{fill:rgb(9, 23, 44);stroke:url(#chatgpt-mermaid-\_r_3oq\_-gradient);stroke-width:1;}#chatgpt-mermaid-\_r_3oq\_ [data-look="neo"].statediagram-cluster rect.outer{rx:5px;ry:5px;filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3oq\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_3oq\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_3oq\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_3oq\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_3oq\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3oq\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_3oq\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_3oq\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_3oq\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3oq\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_3oq\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_3oq\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3oq\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_3oq\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_3oq\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3oq\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_3oq\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3oq\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}ClosedOpenHalfOpenFailure threshold reachedCooldown expiresTest requests succeedTest requests fail

### **What does this achieve?**

Without a circuit breaker:

```
Thousands of requests
        |
        v
Thousands of Redis timeouts
        |
        v
Exhausted application resources
        |
        v
Application failure
```

With a circuit breaker:

```
Redis becomes unhealthy
        |
        v
Circuit opens
        |
        v
Calls to Redis stop temporarily
        |
        v
Application uses bounded fallback
or applies its failure policy
```

The circuit breaker protects the application from repeatedly interacting with an unhealthy dependency.

### **An important implementation detail**

The circuit breaker should generally monitor Redis infrastructure failures, not ordinary rate-limit rejections.

A response such as:

```
User exceeded quota.
```

is not a Redis failure.

Likewise, a successful Redis operation that returns `allowed = false` should not count as a circuit-breaker failure.

# **7. The retry problem: What if we don't know whether Redis executed the operation?**

This is one of the more subtle distributed-systems problems.

Imagine our application sends a Lua script to Redis.

The script:

```
Deduct one token.
Return ALLOW.
```

Redis executes it successfully.

But the network connection breaks before the response reaches the application.

What does the application know?

Nothing definitive.

It received a timeout.

There are two possibilities:

**Possibility A: Redis never executed the script.**

```
Application sends request.

Network failure occurs.

Redis never executes it.
```

**Possibility B: Redis executed the script, but the response was lost.**

```
Application sends request.

Redis executes script.

Token deducted.

Redis returns ALLOW.

Network response gets lost.
```

Both situations look like a timeout to the application.

Now imagine we blindly retry.

```
First execution:
    Token deducted.
    ALLOW.
    Response lost.

Retry:
    Token deducted again.
    ALLOW.
```

We consumed two tokens for one logical admission attempt.

This is a problem of an **ambiguous operation outcome**.

The client cannot tell whether the operation committed.

## **7.1 Should we retry?**

It depends on the operation.

For ordinary HTTP queries, retrying may be safe.

For rate-limit state mutations, blindly repeating the operation may deduct capacity twice.

We need a strategy.

One solution is to give each logical rate-limit decision a unique operation ID.

For example:

```
request_id = req_abc123
```

The Redis script can remember the result for that operation ID.

Conceptually:

```
ATOMIC OPERATION:

    if request_id was already processed:

        return previous result

    calculate token balance

    if sufficient tokens:

        deduct tokens
        decision = ALLOW

    else:

        decision = REJECT

    save result for request_id

    return decision
```

If the application retries with the same request ID, Redis returns the previous decision without deducting tokens a second time.

### **7.2 How do we store the result?**

For example:

```
rate_limit:decision:req_abc123

allowed = true
remaining = 4
```

Give it a short TTL sufficient to cover the retry horizon.

But now another issue appears.

If we store one result key per request, we create additional storage and write overhead.

We also need to ensure that:

- The operation ID is unique.
- Retries use the same ID.
- The decision and deduplication record are written atomically.
- Expiration is long enough for the expected retry period.
- Redis failover doesn't lose one record while retaining the other.

Both the state update and the result record should be part of the same atomic script.

### **7.3 Is this exactly-once execution?**

No.

It provides deduplicated processing of a particular logical operation within the retained state and failure assumptions.

It does not give us universal exactly-once execution across network failures, Redis failover, and all downstream business operations.

Also, the identifier must represent the logical rate-limit decision.

If an application legitimately makes a new HTTP attempt, that should generally have a new rate-limit operation ID.

The distinction is:

```
Same logical operation retried:
    Reuse the same operation ID.

Genuinely new request:
    Generate a new operation ID.
```

For many ordinary API rate limiters, simpler semantics are acceptable:

- If the decision response is lost, the client may retry.
- The retry may consume another token.
- The system accepts this small amount of over-counting.

For strict accounting, deduplication is more important.

**The key lesson: A timeout does not prove that a state-changing operation failed.**

# **8. Avoid retry storms**

Imagine Redis becomes slow.

Our application servers perform retries:

```
Request A -> Redis timeout -> Retry
Request B -> Redis timeout -> Retry
Request C -> Redis timeout -> Retry
...
```

Now the original workload is multiplied by retries.

For example:

```
Original request rate = 10,000/sec

Average attempts per request = 3

Redis receives approximately 30,000 attempts/sec.
```

We are increasing the load on an already unhealthy system.

This can prevent Redis from recovering.

### **Better approach**

Use:

**1. Bounded retries**

Don't retry indefinitely.

**2. Exponential backoff**

Increase the delay between successive retries.

For example:

```
Attempt 1: Immediately

Attempt 2: 100ms

Attempt 3: 200ms

Attempt 4: 400ms
```

**3. Jitter**

Add randomness to retry delays.

Otherwise, all application instances might retry at the same time.

**4. Circuit breaker**

Stop retrying when the dependency is clearly unhealthy.

**5. Request deadline**

Don't continue attempting rate-limit validation after the original API request has already exceeded its useful time budget.

For synchronous API protection, a bounded local fallback or a controlled 503 is often preferable to repeated Redis retries.

# **9. What happens if the Rate Limiter service itself becomes overloaded?**

So far, we have discussed Redis failures.

But imagine our architecture uses a dedicated Rate Limiter service:

```
Backend Applications
         |
         v
 Rate Limiter Service
         |
         v
       Redis
```

Now the Rate Limiter service receives 100,000 requests per second.

It becomes overloaded.

We need to scale it independently.

A reasonable architecture:

\#chatgpt-mermaid-\_r_3qt\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_3qt\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_3qt\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_3qt\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_3qt\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3qt\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_3qt\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_3qt\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_3qt\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_3qt\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_3qt\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_3qt\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3qt\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3qt\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_3qt\_ p{margin:0;}#chatgpt-mermaid-\_r_3qt\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3qt\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3qt\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3qt\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_3qt\_ .label text,#chatgpt-mermaid-\_r_3qt\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3qt\_ .node rect,#chatgpt-mermaid-\_r_3qt\_ .node circle,#chatgpt-mermaid-\_r_3qt\_ .node ellipse,#chatgpt-mermaid-\_r_3qt\_ .node polygon,#chatgpt-mermaid-\_r_3qt\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_3qt\_ .rough-node .label text,#chatgpt-mermaid-\_r_3qt\_ .node .label text,#chatgpt-mermaid-\_r_3qt\_ .image-shape .label,#chatgpt-mermaid-\_r_3qt\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_3qt\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_3qt\_ .rough-node .label,#chatgpt-mermaid-\_r_3qt\_ .node .label,#chatgpt-mermaid-\_r_3qt\_ .image-shape .label,#chatgpt-mermaid-\_r_3qt\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_3qt\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_3qt\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3qt\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3qt\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_3qt\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_3qt\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_3qt\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_3qt\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_3qt\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_3qt\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_3qt\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3qt\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3qt\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_3qt\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3qt\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_3qt\_ .icon-shape,#chatgpt-mermaid-\_r_3qt\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_3qt\_ .icon-shape p,#chatgpt-mermaid-\_r_3qt\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_3qt\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_3qt\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_3qt\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_3qt\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_3qt\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_3qt\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_3qt\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_3qt\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_3qt\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3qt\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_3qt\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_3qt\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_3qt\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3qt\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_3qt\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_3qt\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3qt\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_3qt\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_3qt\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3qt\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_3qt\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3qt\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_3qt\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_3qt\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_3qt\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_3qt\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_3qt\_ .node rect,#chatgpt-mermaid-\_r_3qt\_ .node circle,#chatgpt-mermaid-\_r_3qt\_ .node ellipse,#chatgpt-mermaid-\_r_3qt\_ .node polygon,#chatgpt-mermaid-\_r_3qt\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_3qt\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_3qt\_ .mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_3qt\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_3qt\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3qt\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}Backend ServicesLoad BalancerRate Limiter Instance 1Rate Limiter Instance 2Rate Limiter Instance 3Redis Cluster

The Rate Limiter instances should generally be stateless with respect to authoritative rate-limit state.

They can maintain:

- Local policy caches.
- Connection pools.
- Metrics.
- Short-lived non-authoritative metadata.

But the authoritative counters and token buckets remain in shared storage, unless we use an explicit quota-leasing model.

We can horizontally scale the Rate Limiter service without multiplying the allowance.

### **Additional protections**

We should configure:

- Maximum request body size.
- Authentication for internal calls.
- Connection pool limits.
- Redis connection pooling.
- Bounded application concurrency.
- Request deadlines.
- Load shedding.

Remember, the rate limiter is itself a service that can be attacked or overloaded.

It needs protection too.

# **10. Should the Rate Limiter be in the critical request path?**

Let's examine two architectures.

### **Architecture A: Synchronous rate-limit check**

\#chatgpt-mermaid-\_r_3r4\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_3r4\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_3r4\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_3r4\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_3r4\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3r4\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_3r4\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_3r4\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_3r4\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_3r4\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_3r4\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_3r4\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3r4\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3r4\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_3r4\_ p{margin:0;}#chatgpt-mermaid-\_r_3r4\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3r4\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3r4\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3r4\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_3r4\_ .label text,#chatgpt-mermaid-\_r_3r4\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3r4\_ .node rect,#chatgpt-mermaid-\_r_3r4\_ .node circle,#chatgpt-mermaid-\_r_3r4\_ .node ellipse,#chatgpt-mermaid-\_r_3r4\_ .node polygon,#chatgpt-mermaid-\_r_3r4\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_3r4\_ .rough-node .label text,#chatgpt-mermaid-\_r_3r4\_ .node .label text,#chatgpt-mermaid-\_r_3r4\_ .image-shape .label,#chatgpt-mermaid-\_r_3r4\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_3r4\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_3r4\_ .rough-node .label,#chatgpt-mermaid-\_r_3r4\_ .node .label,#chatgpt-mermaid-\_r_3r4\_ .image-shape .label,#chatgpt-mermaid-\_r_3r4\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_3r4\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_3r4\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3r4\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3r4\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_3r4\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_3r4\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_3r4\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_3r4\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_3r4\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_3r4\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_3r4\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3r4\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3r4\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_3r4\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3r4\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_3r4\_ .icon-shape,#chatgpt-mermaid-\_r_3r4\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_3r4\_ .icon-shape p,#chatgpt-mermaid-\_r_3r4\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_3r4\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_3r4\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_3r4\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_3r4\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_3r4\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_3r4\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_3r4\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_3r4\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_3r4\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3r4\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_3r4\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_3r4\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_3r4\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3r4\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_3r4\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_3r4\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3r4\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_3r4\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_3r4\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3r4\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_3r4\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3r4\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_3r4\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_3r4\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_3r4\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_3r4\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_3r4\_ .node rect,#chatgpt-mermaid-\_r_3r4\_ .node circle,#chatgpt-mermaid-\_r_3r4\_ .node ellipse,#chatgpt-mermaid-\_r_3r4\_ .node polygon,#chatgpt-mermaid-\_r_3r4\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_3r4\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_3r4\_ .mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_3r4\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_3r4\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3r4\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}Incoming RequestApplicationRate LimiterRedisALLOW / REJECTBusiness Logic

The application waits for the rate-limit decision before continuing.

Advantages:

- Immediate enforcement.
- Easy to return HTTP 429.
- Simple to reason about.
- Appropriate for synchronous API admission.

Disadvantages:

- Adds network latency.
- Makes Redis availability important.
- Every request adds rate-limiter workload.

### **Architecture B: Asynchronous quota reporting**

Sometimes, especially for high-volume usage metering, we might batch or asynchronously record usage.

For example:

\#chatgpt-mermaid-\_r_3rb\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_3rb\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_3rb\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_3rb\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_3rb\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3rb\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_3rb\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_3rb\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_3rb\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_3rb\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_3rb\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_3rb\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3rb\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3rb\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_3rb\_ p{margin:0;}#chatgpt-mermaid-\_r_3rb\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3rb\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3rb\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3rb\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_3rb\_ .label text,#chatgpt-mermaid-\_r_3rb\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3rb\_ .node rect,#chatgpt-mermaid-\_r_3rb\_ .node circle,#chatgpt-mermaid-\_r_3rb\_ .node ellipse,#chatgpt-mermaid-\_r_3rb\_ .node polygon,#chatgpt-mermaid-\_r_3rb\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_3rb\_ .rough-node .label text,#chatgpt-mermaid-\_r_3rb\_ .node .label text,#chatgpt-mermaid-\_r_3rb\_ .image-shape .label,#chatgpt-mermaid-\_r_3rb\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_3rb\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_3rb\_ .rough-node .label,#chatgpt-mermaid-\_r_3rb\_ .node .label,#chatgpt-mermaid-\_r_3rb\_ .image-shape .label,#chatgpt-mermaid-\_r_3rb\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_3rb\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_3rb\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3rb\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3rb\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_3rb\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_3rb\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_3rb\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_3rb\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_3rb\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_3rb\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_3rb\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3rb\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3rb\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_3rb\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3rb\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_3rb\_ .icon-shape,#chatgpt-mermaid-\_r_3rb\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_3rb\_ .icon-shape p,#chatgpt-mermaid-\_r_3rb\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_3rb\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_3rb\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_3rb\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_3rb\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_3rb\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_3rb\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_3rb\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_3rb\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_3rb\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3rb\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_3rb\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_3rb\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_3rb\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3rb\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_3rb\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_3rb\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3rb\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_3rb\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_3rb\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3rb\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_3rb\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3rb\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_3rb\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_3rb\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_3rb\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_3rb\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_3rb\_ .node rect,#chatgpt-mermaid-\_r_3rb\_ .node circle,#chatgpt-mermaid-\_r_3rb\_ .node ellipse,#chatgpt-mermaid-\_r_3rb\_ .node polygon,#chatgpt-mermaid-\_r_3rb\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_3rb\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_3rb\_ .mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_3rb\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_3rb\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3rb\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}Incoming RequestLocal AdmissionBackend ProcessingUsage EventMessage QueueUsage AggregatorUsage Database

This can be appropriate for usage analytics or eventual billing reconciliation.

But be careful:

**Asynchronous usage reporting is not a replacement for synchronous admission when we need to enforce a hard rate limit.**

For example, if a user has only 10 tokens remaining, independently admitting requests on ten different servers based on stale local usage can grant too much capacity.

Asynchronous metering and synchronous rate limiting have different correctness requirements.

A production system often uses both:

```
Synchronous limiter:
    Controls admission.

Asynchronous usage pipeline:
    Records and analyzes consumption.
```

# **11. How do we monitor a production Rate Limiter?**

A system can appear healthy while silently making incorrect rate-limit decisions.

For example:

- Redis is becoming slow.
- A hot key is accumulating traffic.
- Too many legitimate requests are being rejected.
- Local fallback is active for too long.
- Quota leases are frequently unused.
- The global limiter is approaching saturation.

We need proper observability.

Let's organize it.

## **11.1 Rate-limit decision metrics**

Track:

```
rate_limit_allowed_total

rate_limit_rejected_total
```

Break them down by useful dimensions such as:

```
policy_id
algorithm
service
environment
region
```

For example:

```
products-api:

Allowed = 1,000,000

Rejected = 50,000
```

This helps us understand how often the limiter is making decisions.

Avoid using individual user IDs, API keys, or arbitrary request IDs as metric labels.

That would create enormous metric cardinality and might expose sensitive information.

## **11.2 Redis metrics**

Track:

- Command latency.
- Lua script execution duration.
- Connection count.
- Memory consumption.
- CPU consumption.
- Evicted keys.
- Cache misses.
- Replication lag.
- Primary failovers.
- Rejected connections.
- Hot-key indicators where available.

For example:

```
Redis p99 latency:

2ms -> Normal

20ms -> Investigate

100ms -> Potential incident
```

Those thresholds are illustrative; actual alerting should follow our latency budget.

## **11.3 Fallback metrics**

This is particularly important.

Track:

```
rate_limiter_fallback_active

rate_limiter_fallback_allowed_total

rate_limiter_fallback_rejected_total

rate_limiter_circuit_open_total
```

Why?

Because a system that is silently failing open could appear perfectly healthy from the perspective of application success rates.

But it may have stopped enforcing rate limits.

## **11.4 Queue metrics for Leaky Bucket**

If our rate limiter is controlling background processing, also track:

```
queue_depth

oldest_job_age

dispatch_rate

retry_rate

dead_letter_count

worker_concurrency
```

An increasing queue depth can indicate that incoming work consistently exceeds the configured dispatch rate.

This might be an expected temporary burst, or a serious capacity problem.

## **11.5 Useful alerts**

Examples:

```
Redis p99 latency exceeds the budget.

Rate Limiter circuit breaker remains open.

Redis failover occurred.

Fallback mode is active.

Rejected requests suddenly increase.

Queue backlog exceeds the permitted threshold.

Oldest pending job age becomes too high.

A single Redis key receives unusually high traffic.
```

The central idea:

**We need to monitor not just whether the Rate Limiter is running, but whether it is correctly enforcing the intended protection.**

# **12. What should the HTTP response look like?**

A production Rate Limiter should communicate its decision clearly.

Consider an allowed request:

```
HTTP/1.1 200 OK
RateLimit-Limit: 100
RateLimit-Remaining: 45
RateLimit-Reset: 12
```

The exact response headers depend on the API's chosen rate-limit header convention.

For a rejected request:

```
HTTP/1.1 429 Too Many Requests
Retry-After: 2
Content-Type: application/json
```

```
{
  "error": "rate_limit_exceeded",
  "message": "Request limit exceeded.",
  "retryAfterSeconds": 2
}
```

A distinction worth maintaining:

| Response | Meaning                                                                           |
| -------- | --------------------------------------------------------------------------------- |
| 429      | The applicable rate limit was exceeded.                                           |
| 503      | The rate-limit service is unavailable and the request cannot be safely evaluated. |

We shouldn't tell a client it exceeded its quota when our Redis infrastructure simply timed out.

Also, don't reveal internal details such as Redis hostnames, key names, or implementation information in public error responses.

# **13. Testing the Rate Limiter properly**

Rate limiters are particularly prone to concurrency and boundary bugs.

A few unit tests are not enough.

Let's identify the important test categories.

### **A. Algorithm correctness tests**

For Fixed Window:

```
Limit = 5

Send 5 requests:
    All allowed.

Send 6th request:
    Rejected.

Advance to next window:
    New requests allowed.
```

For Sliding Window Log:

```
Limit = 5
Window = 10 seconds

Send 5 requests.

Send another at 9 seconds:
    Rejected.

Advance until the oldest request expires.

Send another:
    Allowed.
```

For Sliding Window Counter:

Test:

- Same window.
- Exactly one window transition.
- Multiple skipped windows.
- Beginning of a window.
- End of a window.
- Fractional weighted counts.

For Token Bucket:

```
Capacity = 10
Refill rate = 2/sec

Initial:
    10 tokens

Consume 10:
    All allowed.

Next request:
    Rejected.

Wait 500ms:
    One token becomes available.
```

For Leaky Bucket:

Test:

- Queue capacity.
- FIFO behavior.
- Dispatch timing.
- Queue overflow.
- Worker failures.
- Retry scheduling.

### **B. Concurrent request tests**

This is essential.

Suppose:

```
Capacity = 100

Initial tokens = 100

1,000 requests arrive simultaneously.
```

Expected result:

```
Allowed = 100

Rejected = 900
```

Test this from:

- One application instance.
- Multiple application instances.
- Multiple worker processes.
- Multiple connections to Redis.

A sequential test may pass even when the implementation has a race condition.

### **C. Boundary tests**

For a 10-second Fixed Window:

```
Request at 9.999 seconds.

Request at 10.001 seconds.
```

Verify that Fixed Window behaves according to its documented boundary semantics.

For Sliding Window:

```
Request exactly at the expiry boundary.
```

Make sure the implementation's interval convention is consistent.

For example, whether the window is represented as:

\\[ (T-W,T] \\]

or another convention must be consistent in cleanup and counting.

### **D. Failure tests**

Simulate:

```
Redis becomes unavailable.

Redis becomes slow.

Redis primary crashes.

Replica is promoted.

Redis connection breaks after a script executes.

Application instance crashes during fallback.

Rate Limiter service restarts.
```

Check that the system follows the intended failure policy.

### **E. Load tests**

Test the expected production workload.

For example:

```
100,000 incoming requests/second.

10 million active identities.

100 application instances.
```

Measure:

- Decision latency.
- Redis CPU.
- Redis memory.
- Script execution time.
- Rejection accuracy.
- Connection pool saturation.

Also test skewed traffic, not just uniform traffic.

A million evenly distributed identities behave very differently from one exceptionally hot tenant.

# **14. The complete production architecture**

Let's finally put everything together.

Imagine we are building a shared Rate Limiting Platform for multiple backend services.

Requirements:

```
Multiple API services.

Multiple tenants and users.

Per-user rate limits.

Per-tenant rate limits.

Global protection.

Different algorithms for different policies.

High availability.

Multiple geographical regions.

Burst tolerance.

Graceful degradation.
```

A possible architecture:

\#chatgpt-mermaid-\_r_3u7\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_3u7\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_3u7\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_3u7\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_3u7\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3u7\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_3u7\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_3u7\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_3u7\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_3u7\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_3u7\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_3u7\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3u7\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3u7\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_3u7\_ p{margin:0;}#chatgpt-mermaid-\_r_3u7\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3u7\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3u7\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3u7\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_3u7\_ .label text,#chatgpt-mermaid-\_r_3u7\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3u7\_ .node rect,#chatgpt-mermaid-\_r_3u7\_ .node circle,#chatgpt-mermaid-\_r_3u7\_ .node ellipse,#chatgpt-mermaid-\_r_3u7\_ .node polygon,#chatgpt-mermaid-\_r_3u7\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_3u7\_ .rough-node .label text,#chatgpt-mermaid-\_r_3u7\_ .node .label text,#chatgpt-mermaid-\_r_3u7\_ .image-shape .label,#chatgpt-mermaid-\_r_3u7\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_3u7\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_3u7\_ .rough-node .label,#chatgpt-mermaid-\_r_3u7\_ .node .label,#chatgpt-mermaid-\_r_3u7\_ .image-shape .label,#chatgpt-mermaid-\_r_3u7\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_3u7\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_3u7\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3u7\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3u7\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_3u7\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_3u7\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_3u7\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_3u7\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_3u7\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_3u7\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_3u7\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3u7\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3u7\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_3u7\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3u7\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_3u7\_ .icon-shape,#chatgpt-mermaid-\_r_3u7\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_3u7\_ .icon-shape p,#chatgpt-mermaid-\_r_3u7\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_3u7\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_3u7\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_3u7\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_3u7\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_3u7\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_3u7\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_3u7\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_3u7\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_3u7\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3u7\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_3u7\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_3u7\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_3u7\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3u7\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_3u7\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_3u7\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3u7\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_3u7\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_3u7\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3u7\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_3u7\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3u7\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_3u7\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_3u7\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_3u7\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_3u7\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_3u7\_ .node rect,#chatgpt-mermaid-\_r_3u7\_ .node circle,#chatgpt-mermaid-\_r_3u7\_ .node ellipse,#chatgpt-mermaid-\_r_3u7\_ .node polygon,#chatgpt-mermaid-\_r_3u7\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_3u7\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_3u7\_ .mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_3u7\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_3u7\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3u7\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}ClientsGlobal Load BalancerAPI GatewayAuthentication and IdentityPolicy ResolverRate Limiter Middleware /ServiceLocal Policy CacheRate Limiter Decision EngineRedis Cluster / RegionalCoordinatorAtomic Policy EvaluationAllowed?Backend ServicesHTTP 429Databases / External APIsFallback LimiterMetrics and MonitoringAsync Usage EventsUsage AggregatorUsage DatabaseYesNo

Let's understand the responsibilities.

| Component        | Responsibility                                  |
| ---------------- | ----------------------------------------------- |
| API Gateway      | Initial request entry point                     |
| Authentication   | Establish trusted user and tenant identity      |
| Policy Resolver  | Determine applicable limits                     |
| Policy Cache     | Avoid repeated policy configuration lookups     |
| Decision Engine  | Execute the selected algorithm                  |
| Redis            | Maintain shared dynamic rate-limit state        |
| Fallback Limiter | Provide bounded protection during Redis outages |
| Metrics          | Track decisions and infrastructure health       |
| Usage Aggregator | Record consumption for analytics or billing     |
| Backend Services | Execute business operations after admission     |

The exact architecture can be simpler or more sophisticated depending on traffic and requirements.

For a smaller application, the API gateway or application middleware can contain most of this logic.

We shouldn't introduce a dedicated service, Redis Cluster, regional coordinators, and quota leasing unless the scale and requirements justify them.

# **15. Production design decisions — What would I actually choose?**

Let's imagine we're building a reasonably large SaaS platform.

My initial architecture would be:

```
API Gateway
    |
    v
Authentication
    |
    v
Rate Limiting Middleware
    |
    +---- Policy Cache
    |
    +---- Redis
    |
    +---- Bounded Local Fallback
    |
    v
Backend Application
```

And my initial technology choices:

| Concern                                  | Choice                                                 |
| ---------------------------------------- | ------------------------------------------------------ |
| General API rate limiting                | Token Bucket                                           |
| Simple daily/monthly quotas              | Fixed Window / durable usage metering                  |
| Strict rolling-window quotas             | Sliding Window Log                                     |
| Approximate rolling quotas at high scale | Sliding Window Counter                                 |
| Background job pacing                    | Leaky Bucket-style dispatcher                          |
| Shared rate-limit state                  | Redis                                                  |
| Atomic multi-step decisions              | Redis Lua scripts                                      |
| Redis scaling                            | Redis Cluster when required                            |
| Policy configuration                     | Configuration database + local cache                   |
| Temporary Redis failures                 | Circuit breaker + bounded fallback                     |
| Global quota scaling                     | Quota leasing when necessary                           |
| Per-region independent limits            | Regional Redis and regional policies                   |
| Cross-region global limits               | Home-region authority or hierarchical quota allocation |
| Monitoring                               | Metrics, logs, tracing and alerts                      |

These are starting choices, not mandatory technologies for every system.

The actual design depends on the required correctness, traffic distribution, latency budget, and failure tolerance.

# **16. Final mental model — The complete distributed Rate Limiter**

Let's summarize the entire evolution.

We started with:

```
One application.

One in-memory counter.

One rate-limit decision.
```

Then we discovered that multiple application instances cannot independently maintain the same global quota.

We introduced Redis as shared state.

Then we discovered that sharing state doesn't guarantee atomic updates.

We introduced Lua scripts.

Then we discovered that one Redis key can become a hot key.

We introduced sharding, partitioned budgets, and quota leasing.

Then we introduced multiple regions.

We discovered that asynchronous replication doesn't prevent two regions from spending the same quota independently.

We introduced home-region ownership and hierarchical quota allocation.

Finally, we considered failures:

- Redis downtime.
- Replication lag.
- Lost acknowledged writes.
- Timeouts.
- Duplicate state mutations.
- Circuit breakers.
- Local fallback.
- Monitoring.

The evolution looks like this:

\#chatgpt-mermaid-\_r_3um\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_3um\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_3um\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_3um\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_3um\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3um\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_3um\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_3um\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_3um\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_3um\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_3um\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_3um\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3um\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3um\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_3um\_ p{margin:0;}#chatgpt-mermaid-\_r_3um\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3um\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3um\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3um\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_3um\_ .label text,#chatgpt-mermaid-\_r_3um\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3um\_ .node rect,#chatgpt-mermaid-\_r_3um\_ .node circle,#chatgpt-mermaid-\_r_3um\_ .node ellipse,#chatgpt-mermaid-\_r_3um\_ .node polygon,#chatgpt-mermaid-\_r_3um\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_3um\_ .rough-node .label text,#chatgpt-mermaid-\_r_3um\_ .node .label text,#chatgpt-mermaid-\_r_3um\_ .image-shape .label,#chatgpt-mermaid-\_r_3um\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_3um\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_3um\_ .rough-node .label,#chatgpt-mermaid-\_r_3um\_ .node .label,#chatgpt-mermaid-\_r_3um\_ .image-shape .label,#chatgpt-mermaid-\_r_3um\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_3um\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_3um\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3um\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3um\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_3um\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_3um\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_3um\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_3um\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_3um\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_3um\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_3um\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3um\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3um\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_3um\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3um\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_3um\_ .icon-shape,#chatgpt-mermaid-\_r_3um\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_3um\_ .icon-shape p,#chatgpt-mermaid-\_r_3um\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_3um\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_3um\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_3um\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_3um\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_3um\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_3um\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_3um\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_3um\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_3um\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3um\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_3um\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_3um\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_3um\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3um\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_3um\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_3um\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3um\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_3um\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_3um\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3um\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_3um\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3um\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_3um\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_3um\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_3um\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_3um\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_3um\_ .node rect,#chatgpt-mermaid-\_r_3um\_ .node circle,#chatgpt-mermaid-\_r_3um\_ .node ellipse,#chatgpt-mermaid-\_r_3um\_ .node polygon,#chatgpt-mermaid-\_r_3um\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_3um\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_3um\_ .mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_3um\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_3um\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3um\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}In-memory Rate LimiterShared RedisAtomic Lua ScriptsRedis ClusterHot Key HandlingQuota LeasingMulti-region CoordinationFailure HandlingProduction Rate LimitingPlatform

The most important principle:

**The algorithm determines how we calculate permission. Distributed-systems engineering determines whether that permission is calculated correctly across servers, regions, and failures.**

Understanding both is what makes you capable of designing a production-grade Rate Limiter.

# **17. What you should be able to explain in a system design interview now**

By this point, you should be comfortable answering these questions:

**Algorithm questions**

- Why does Fixed Window allow boundary bursts?
- How does Sliding Window Log eliminate that problem?
- Why is Sliding Window Counter an approximation?
- Why does Leaky Bucket need a queue?
- How does Token Bucket accumulate burst capacity?
- When should we combine multiple algorithms?

**Implementation questions**

- Why Redis?
- What data structure should we use for each algorithm?
- Why do we need Lua scripts?
- What is the difference between atomicity and durability?
- How do we expire old rate-limit state?
- How do we handle concurrent requests?

**Distributed-systems questions**

- Why doesn't in-memory state work across servers?
- How do we distribute rate-limit keys?
- What is a hot key?
- How do we enforce a global limit?
- How does quota leasing work?
- Why doesn't asynchronous replication guarantee global rate-limit correctness?
- How do we handle multi-region rate limiting?

**Reliability questions**

- Fail open vs fail closed?
- What if Redis is unavailable?
- What if Redis loses recent writes?
- What if the application times out after Redis executes a script?
- How do we avoid retry storms?
- How do we monitor incorrect or degraded rate limiting?

These questions cover the most important architectural and implementation concerns.

## **Where we should go next**

We have completed the core algorithm course and the initial distributed-system design.

There are three useful deeper areas worth studying next.

**1. Implement a complete Rate Limiter in Java + Spring Boot + Redis**

Build actual code for:

- Fixed Window.
- Sliding Window Log.
- Sliding Window Counter.
- Token Bucket.
- Redis Lua scripts.
- Spring Boot filters/interceptors.
- Policy configuration.
- HTTP 429 responses.
- Integration and concurrency tests.

**2. Deep dive into distributed quota leasing and multi-region coordination**

Understand how to maintain hard global quotas while minimizing coordination, including lease expiry, fencing tokens, regional allocations, and consistency tradeoffs.

**3. Complete system design interview simulation**

Take a requirement such as:

> Design a globally distributed rate-limiting service supporting millions of users and multiple API gateways.

Then evolve the complete design from requirements to architecture, database choices, Redis key structures, algorithms, APIs, concurrency, failure handling, and scaling.

My recommendation is to do the **Java + Spring Boot + Redis implementation next**. That will connect the mathematical intuition we've developed to the actual code and distributed behavior of a production system.
