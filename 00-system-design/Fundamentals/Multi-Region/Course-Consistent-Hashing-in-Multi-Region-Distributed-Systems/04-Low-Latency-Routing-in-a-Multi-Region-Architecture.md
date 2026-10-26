# Chapter 4: Low-Latency Routing in a Multi-Region Architecture

## Lesson 1: Route Users to the Nearest Region Without Breaking Data Ownership

Imagine your SaaS application runs in three regions:

- India
- Europe
- North America

A user in India sends a request. The nearest healthy application server is in India, so routing the request there seems obvious.

But there is a catch: what if that user's data is owned by a shard in Europe?

This is the central challenge of multi-region routing:

The nearest region is not necessarily the region that owns the data.

You need to balance four goals:

1. Low latency: Serve users from a nearby region where possible.
2. Correctness: Route reads and writes according to data ownership and consistency guarantees.
3. Availability: Avoid unhealthy or unavailable regions.
4. Compliance: Keep data and processing within permitted geographic boundaries.

Let's build the routing model step by step.

## 1. Separate the three routing decisions

A common architectural mistake is treating routing as one decision. In practice, there are three separate decisions.

\#chatgpt-mermaid-\_r_57j\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_57j\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_57j\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_57j\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_57j\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_57j\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_57j\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_57j\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_57j\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_57j\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_57j\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_57j\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_57j\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_57j\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_57j\_ p{margin:0;}#chatgpt-mermaid-\_r_57j\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_57j\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_57j\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_57j\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_57j\_ .label text,#chatgpt-mermaid-\_r_57j\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_57j\_ .node rect,#chatgpt-mermaid-\_r_57j\_ .node circle,#chatgpt-mermaid-\_r_57j\_ .node ellipse,#chatgpt-mermaid-\_r_57j\_ .node polygon,#chatgpt-mermaid-\_r_57j\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_57j\_ .rough-node .label text,#chatgpt-mermaid-\_r_57j\_ .node .label text,#chatgpt-mermaid-\_r_57j\_ .image-shape .label,#chatgpt-mermaid-\_r_57j\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_57j\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_57j\_ .rough-node .label,#chatgpt-mermaid-\_r_57j\_ .node .label,#chatgpt-mermaid-\_r_57j\_ .image-shape .label,#chatgpt-mermaid-\_r_57j\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_57j\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_57j\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_57j\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_57j\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_57j\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_57j\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_57j\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_57j\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_57j\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_57j\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_57j\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_57j\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_57j\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_57j\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_57j\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_57j\_ .icon-shape,#chatgpt-mermaid-\_r_57j\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_57j\_ .icon-shape p,#chatgpt-mermaid-\_r_57j\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_57j\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_57j\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_57j\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_57j\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_57j\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_57j\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_57j\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_57j\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_57j\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_57j\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_57j\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_57j\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_57j\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_57j\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_57j\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_57j\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_57j\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_57j\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_57j\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_57j\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_57j\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_57j\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_57j\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_57j\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_57j\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_57j\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_57j\_ .node rect,#chatgpt-mermaid-\_r_57j\_ .node circle,#chatgpt-mermaid-\_r_57j\_ .node ellipse,#chatgpt-mermaid-\_r_57j\_ .node polygon,#chatgpt-mermaid-\_r_57j\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_57j\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_57j\_ .node.mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_57j\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_57j\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_57j\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}User Request1. Global Entry RoutingSelected Application Region2. Application InstanceRouting3. Data Ownership andResidency LookupEligible Region andAuthoritative ShardDatabase / Read Replica

### Decision 1: Which region receives the request?

This is the global traffic-routing problem.

Possible inputs include:

- User's approximate geographic location.
- Network latency.
- Regional health.
- Capacity and traffic load.
- Residency restrictions.
- Whether the request is for a user with an established home region.

Technologies such as DNS-based routing, global load balancing, and anycast-based entry points can help with this decision.

### Decision 2: Which application instance handles the request?

Once the request reaches a region, a regional load balancer routes it to a healthy application instance.

For example, an AWS Application Load Balancer might distribute traffic across healthy targets within a region.

This is a regional compute-routing decision. It does not decide which database shard owns the user's data.

### Decision 3: Where does the authoritative data live?

The application determines the eligible data region, resolves the shard or partition, and chooses an appropriate database endpoint.

This is where consistent hashing can help.

For example:

```
Request: GET /customers/cust-123

1. Resolve customer residency policy.
2. Determine the customer's authoritative region.
3. Resolve the shard within that region.
4. Select a permitted read endpoint.
5. Execute the query.
```

If the customer belongs to the India region, a global hash ring must not send the request to an arbitrary European shard simply because that shard's hash position happens to come first.

The policy and ownership constraints come before hashing.

## 2. Why routing every user to the nearest region fails

Consider a user based in India whose tenant data is owned by Europe.

A naive system might do this:

\#chatgpt-mermaid-\_r_581\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_581\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_581\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_581\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_581\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_581\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_581\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_581\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_581\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_581\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_581\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_581\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_581\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_581\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_581\_ p{margin:0;}#chatgpt-mermaid-\_r_581\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_581\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_581\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_581\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_581\_ .label text,#chatgpt-mermaid-\_r_581\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_581\_ .node rect,#chatgpt-mermaid-\_r_581\_ .node circle,#chatgpt-mermaid-\_r_581\_ .node ellipse,#chatgpt-mermaid-\_r_581\_ .node polygon,#chatgpt-mermaid-\_r_581\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_581\_ .rough-node .label text,#chatgpt-mermaid-\_r_581\_ .node .label text,#chatgpt-mermaid-\_r_581\_ .image-shape .label,#chatgpt-mermaid-\_r_581\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_581\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_581\_ .rough-node .label,#chatgpt-mermaid-\_r_581\_ .node .label,#chatgpt-mermaid-\_r_581\_ .image-shape .label,#chatgpt-mermaid-\_r_581\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_581\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_581\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_581\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_581\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_581\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_581\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_581\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_581\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_581\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_581\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_581\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_581\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_581\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_581\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_581\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_581\_ .icon-shape,#chatgpt-mermaid-\_r_581\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_581\_ .icon-shape p,#chatgpt-mermaid-\_r_581\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_581\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_581\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_581\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_581\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_581\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_581\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_581\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_581\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_581\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_581\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_581\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_581\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_581\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_581\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_581\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_581\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_581\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_581\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_581\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_581\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_581\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_581\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_581\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_581\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_581\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_581\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_581\_ .node rect,#chatgpt-mermaid-\_r_581\_ .node circle,#chatgpt-mermaid-\_r_581\_ .node ellipse,#chatgpt-mermaid-\_r_581\_ .node polygon,#chatgpt-mermaid-\_r_581\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_581\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_581\_ .node.mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_581\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_581\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_581\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}User in IndiaIndia ApplicationIndia DatabaseIncorrect ResultTenant data absent or stale

The India database might not contain the authoritative record. It might have a stale replica, or it might not be allowed to store that customer's data at all.

The request can fail, return stale information, or violate a residency policy.

One alternative is to route the entire request to Europe:

\#chatgpt-mermaid-\_r_58a\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_58a\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_58a\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_58a\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_58a\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_58a\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_58a\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_58a\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_58a\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_58a\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_58a\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_58a\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_58a\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_58a\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_58a\_ p{margin:0;}#chatgpt-mermaid-\_r_58a\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_58a\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_58a\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_58a\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_58a\_ .label text,#chatgpt-mermaid-\_r_58a\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_58a\_ .node rect,#chatgpt-mermaid-\_r_58a\_ .node circle,#chatgpt-mermaid-\_r_58a\_ .node ellipse,#chatgpt-mermaid-\_r_58a\_ .node polygon,#chatgpt-mermaid-\_r_58a\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_58a\_ .rough-node .label text,#chatgpt-mermaid-\_r_58a\_ .node .label text,#chatgpt-mermaid-\_r_58a\_ .image-shape .label,#chatgpt-mermaid-\_r_58a\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_58a\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_58a\_ .rough-node .label,#chatgpt-mermaid-\_r_58a\_ .node .label,#chatgpt-mermaid-\_r_58a\_ .image-shape .label,#chatgpt-mermaid-\_r_58a\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_58a\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_58a\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_58a\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_58a\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_58a\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_58a\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_58a\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_58a\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_58a\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_58a\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_58a\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_58a\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_58a\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_58a\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_58a\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_58a\_ .icon-shape,#chatgpt-mermaid-\_r_58a\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_58a\_ .icon-shape p,#chatgpt-mermaid-\_r_58a\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_58a\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_58a\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_58a\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_58a\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_58a\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_58a\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_58a\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_58a\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_58a\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_58a\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_58a\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_58a\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_58a\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_58a\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_58a\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_58a\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_58a\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_58a\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_58a\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_58a\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_58a\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_58a\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_58a\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_58a\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_58a\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_58a\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_58a\_ .node rect,#chatgpt-mermaid-\_r_58a\_ .node circle,#chatgpt-mermaid-\_r_58a\_ .node ellipse,#chatgpt-mermaid-\_r_58a\_ .node polygon,#chatgpt-mermaid-\_r_58a\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_58a\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_58a\_ .node.mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_58a\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_58a\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_58a\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}User in IndiaIndia Entry PointEurope ApplicationEurope AuthoritativeDatabase

This preserves ownership, but the request now pays the network cost of reaching Europe.

If the user makes multiple database calls during one request, the additional latency can accumulate.

A third option is to execute the application logic in India and access the authoritative data in Europe. This can reduce some compute-routing overhead, but it does not eliminate the cross-region network latency. It also introduces security, connectivity, and failure-handling requirements.

The correct choice depends on the application and its data policy.

Principle: Route compute for performance, but route data operations according to ownership, consistency, and residency rules.

## 3. Four routing strategies

There isn't one routing strategy that works for every application. Let's compare the main options.

### Strategy A: Nearest healthy region

Send users to the region with the best network latency, provided it is healthy and eligible to serve them.

\#chatgpt-mermaid-\_r_58j\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_58j\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_58j\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_58j\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_58j\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_58j\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_58j\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_58j\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_58j\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_58j\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_58j\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_58j\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_58j\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_58j\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_58j\_ p{margin:0;}#chatgpt-mermaid-\_r_58j\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_58j\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_58j\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_58j\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_58j\_ .label text,#chatgpt-mermaid-\_r_58j\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_58j\_ .node rect,#chatgpt-mermaid-\_r_58j\_ .node circle,#chatgpt-mermaid-\_r_58j\_ .node ellipse,#chatgpt-mermaid-\_r_58j\_ .node polygon,#chatgpt-mermaid-\_r_58j\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_58j\_ .rough-node .label text,#chatgpt-mermaid-\_r_58j\_ .node .label text,#chatgpt-mermaid-\_r_58j\_ .image-shape .label,#chatgpt-mermaid-\_r_58j\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_58j\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_58j\_ .rough-node .label,#chatgpt-mermaid-\_r_58j\_ .node .label,#chatgpt-mermaid-\_r_58j\_ .image-shape .label,#chatgpt-mermaid-\_r_58j\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_58j\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_58j\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_58j\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_58j\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_58j\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_58j\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_58j\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_58j\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_58j\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_58j\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_58j\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_58j\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_58j\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_58j\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_58j\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_58j\_ .icon-shape,#chatgpt-mermaid-\_r_58j\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_58j\_ .icon-shape p,#chatgpt-mermaid-\_r_58j\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_58j\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_58j\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_58j\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_58j\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_58j\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_58j\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_58j\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_58j\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_58j\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_58j\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_58j\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_58j\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_58j\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_58j\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_58j\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_58j\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_58j\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_58j\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_58j\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_58j\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_58j\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_58j\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_58j\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_58j\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_58j\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_58j\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_58j\_ .node rect,#chatgpt-mermaid-\_r_58j\_ .node circle,#chatgpt-mermaid-\_r_58j\_ .node ellipse,#chatgpt-mermaid-\_r_58j\_ .node polygon,#chatgpt-mermaid-\_r_58j\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_58j\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_58j\_ .node.mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_58j\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_58j\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_58j\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}User RequestCheck Eligible RegionsFilter Unhealthy RegionsChoose Low-Latency RegionServe Request

Best for:

- Public websites.
- Static content.
- Read-heavy APIs backed by sufficiently fresh regional replicas.
- Services that do not require all operations to execute in the data owner's region.

Risks:

- The nearest region may have stale data.
- A regional application may need to call a remote database.
- A latency-based decision may conflict with a user's established session or data ownership.

The critical word is eligible. Do not select the lowest-latency region from a set of regions that should never have been considered for that request.

### Strategy B: Home-region routing

Assign each tenant or customer a home region. Route its data operations to that region even if the user is currently elsewhere.

For example:

| Tenant   | Home region   | User's current location |
| -------- | ------------- | ----------------------- |
| Tenant A | India         | India                   |
| Tenant B | Europe        | India                   |
| Tenant C | North America | Europe                  |

A user can travel, change networks, or connect through a different region without changing the tenant's data ownership.

\#chatgpt-mermaid-\_r_58s\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_58s\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_58s\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_58s\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_58s\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_58s\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_58s\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_58s\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_58s\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_58s\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_58s\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_58s\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_58s\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_58s\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_58s\_ p{margin:0;}#chatgpt-mermaid-\_r_58s\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_58s\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_58s\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_58s\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_58s\_ .label text,#chatgpt-mermaid-\_r_58s\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_58s\_ .node rect,#chatgpt-mermaid-\_r_58s\_ .node circle,#chatgpt-mermaid-\_r_58s\_ .node ellipse,#chatgpt-mermaid-\_r_58s\_ .node polygon,#chatgpt-mermaid-\_r_58s\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_58s\_ .rough-node .label text,#chatgpt-mermaid-\_r_58s\_ .node .label text,#chatgpt-mermaid-\_r_58s\_ .image-shape .label,#chatgpt-mermaid-\_r_58s\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_58s\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_58s\_ .rough-node .label,#chatgpt-mermaid-\_r_58s\_ .node .label,#chatgpt-mermaid-\_r_58s\_ .image-shape .label,#chatgpt-mermaid-\_r_58s\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_58s\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_58s\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_58s\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_58s\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_58s\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_58s\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_58s\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_58s\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_58s\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_58s\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_58s\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_58s\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_58s\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_58s\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_58s\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_58s\_ .icon-shape,#chatgpt-mermaid-\_r_58s\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_58s\_ .icon-shape p,#chatgpt-mermaid-\_r_58s\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_58s\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_58s\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_58s\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_58s\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_58s\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_58s\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_58s\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_58s\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_58s\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_58s\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_58s\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_58s\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_58s\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_58s\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_58s\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_58s\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_58s\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_58s\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_58s\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_58s\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_58s\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_58s\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_58s\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_58s\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_58s\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_58s\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_58s\_ .node rect,#chatgpt-mermaid-\_r_58s\_ .node circle,#chatgpt-mermaid-\_r_58s\_ .node ellipse,#chatgpt-mermaid-\_r_58s\_ .node polygon,#chatgpt-mermaid-\_r_58s\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_58s\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_58s\_ .node.mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_58s\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_58s\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_58s\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}Request for Tenant BNearest Entry: IndiaResolve Home Region:EuropeEurope Data OwnerReturn Result

This approach simplifies ownership and often provides a clear consistency model.

The tradeoff is latency: requests can cross regions even when a nearby region is healthy.

Best for: Multi-tenant SaaS applications where tenants have clear ownership boundaries and cross-region writes should be avoided.

### Strategy C: Nearest application, remote authoritative database

The request is handled by a nearby application server, but the application accesses the authoritative database in another region.

For example:

\#chatgpt-mermaid-\_r_595\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_595\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_595\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_595\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_595\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_595\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_595\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_595\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_595\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_595\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_595\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_595\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_595\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_595\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_595\_ p{margin:0;}#chatgpt-mermaid-\_r_595\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_595\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_595\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_595\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_595\_ .label text,#chatgpt-mermaid-\_r_595\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_595\_ .node rect,#chatgpt-mermaid-\_r_595\_ .node circle,#chatgpt-mermaid-\_r_595\_ .node ellipse,#chatgpt-mermaid-\_r_595\_ .node polygon,#chatgpt-mermaid-\_r_595\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_595\_ .rough-node .label text,#chatgpt-mermaid-\_r_595\_ .node .label text,#chatgpt-mermaid-\_r_595\_ .image-shape .label,#chatgpt-mermaid-\_r_595\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_595\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_595\_ .rough-node .label,#chatgpt-mermaid-\_r_595\_ .node .label,#chatgpt-mermaid-\_r_595\_ .image-shape .label,#chatgpt-mermaid-\_r_595\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_595\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_595\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_595\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_595\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_595\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_595\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_595\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_595\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_595\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_595\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_595\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_595\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_595\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_595\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_595\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_595\_ .icon-shape,#chatgpt-mermaid-\_r_595\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_595\_ .icon-shape p,#chatgpt-mermaid-\_r_595\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_595\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_595\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_595\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_595\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_595\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_595\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_595\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_595\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_595\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_595\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_595\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_595\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_595\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_595\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_595\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_595\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_595\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_595\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_595\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_595\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_595\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_595\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_595\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_595\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_595\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_595\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_595\_ .node rect,#chatgpt-mermaid-\_r_595\_ .node circle,#chatgpt-mermaid-\_r_595\_ .node ellipse,#chatgpt-mermaid-\_r_595\_ .node polygon,#chatgpt-mermaid-\_r_595\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_595\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_595\_ .node.mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_595\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_595\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_595\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}User in IndiaIndia APIEurope Shard

This can be reasonable when application processing is substantial and database access is limited.

However, it can perform poorly when the request makes many sequential database calls.

Suppose a request performs 10 sequential database round trips to Europe, each adding approximately 80 ms of network round-trip time. Those database calls alone could contribute about 800 ms of network latency.

Parallel requests may reduce the total time, but introduce their own complexity. Connection pooling, remote database availability, and network costs also matter.

Best for: Some read-heavy applications or services with relatively few remote database round trips.

### Strategy D: Regional ownership with local replicas

The authoritative data lives in one region, while other regions maintain read replicas.

For example:

\#chatgpt-mermaid-\_r_59e\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_59e\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_59e\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_59e\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_59e\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_59e\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_59e\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_59e\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_59e\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_59e\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_59e\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_59e\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_59e\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_59e\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_59e\_ p{margin:0;}#chatgpt-mermaid-\_r_59e\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_59e\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_59e\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_59e\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_59e\_ .label text,#chatgpt-mermaid-\_r_59e\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_59e\_ .node rect,#chatgpt-mermaid-\_r_59e\_ .node circle,#chatgpt-mermaid-\_r_59e\_ .node ellipse,#chatgpt-mermaid-\_r_59e\_ .node polygon,#chatgpt-mermaid-\_r_59e\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_59e\_ .rough-node .label text,#chatgpt-mermaid-\_r_59e\_ .node .label text,#chatgpt-mermaid-\_r_59e\_ .image-shape .label,#chatgpt-mermaid-\_r_59e\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_59e\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_59e\_ .rough-node .label,#chatgpt-mermaid-\_r_59e\_ .node .label,#chatgpt-mermaid-\_r_59e\_ .image-shape .label,#chatgpt-mermaid-\_r_59e\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_59e\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_59e\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_59e\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_59e\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_59e\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_59e\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_59e\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_59e\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_59e\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_59e\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_59e\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_59e\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_59e\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_59e\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_59e\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_59e\_ .icon-shape,#chatgpt-mermaid-\_r_59e\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_59e\_ .icon-shape p,#chatgpt-mermaid-\_r_59e\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_59e\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_59e\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_59e\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_59e\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_59e\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_59e\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_59e\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_59e\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_59e\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_59e\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_59e\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_59e\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_59e\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_59e\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_59e\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_59e\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_59e\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_59e\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_59e\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_59e\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_59e\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_59e\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_59e\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_59e\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_59e\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_59e\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_59e\_ .node rect,#chatgpt-mermaid-\_r_59e\_ .node circle,#chatgpt-mermaid-\_r_59e\_ .node ellipse,#chatgpt-mermaid-\_r_59e\_ .node polygon,#chatgpt-mermaid-\_r_59e\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_59e\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_59e\_ .node.mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_59e\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_59e\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_59e\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}India: Authoritative ShardEurope: Read ReplicaNorth America: Read ReplicaEuropean UserEurope Read Endpoint

Reads can be fast locally, while writes go to the authoritative owner.

The tradeoff is replication lag. A local replica may not yet contain a recent write.

This strategy needs an explicit read-consistency policy. You cannot assume a local replica is always sufficiently fresh.

Best for: Read-heavy workloads where eventual consistency is acceptable for at least some operations.

These strategies can be combined. For instance, a system might route users to the nearest eligible application region, use home-region ownership for writes, and use local replicas for eventually consistent reads.

## 4. How consistent hashing fits into regional routing

Let's use a concrete example.

Your application has three regions, each containing multiple database shards.

\#chatgpt-mermaid-\_r_59n\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_59n\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_59n\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_59n\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_59n\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_59n\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_59n\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_59n\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_59n\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_59n\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_59n\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_59n\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_59n\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_59n\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_59n\_ p{margin:0;}#chatgpt-mermaid-\_r_59n\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_59n\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_59n\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_59n\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_59n\_ .label text,#chatgpt-mermaid-\_r_59n\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_59n\_ .node rect,#chatgpt-mermaid-\_r_59n\_ .node circle,#chatgpt-mermaid-\_r_59n\_ .node ellipse,#chatgpt-mermaid-\_r_59n\_ .node polygon,#chatgpt-mermaid-\_r_59n\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_59n\_ .rough-node .label text,#chatgpt-mermaid-\_r_59n\_ .node .label text,#chatgpt-mermaid-\_r_59n\_ .image-shape .label,#chatgpt-mermaid-\_r_59n\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_59n\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_59n\_ .rough-node .label,#chatgpt-mermaid-\_r_59n\_ .node .label,#chatgpt-mermaid-\_r_59n\_ .image-shape .label,#chatgpt-mermaid-\_r_59n\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_59n\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_59n\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_59n\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_59n\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_59n\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_59n\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_59n\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_59n\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_59n\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_59n\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_59n\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_59n\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_59n\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_59n\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_59n\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_59n\_ .icon-shape,#chatgpt-mermaid-\_r_59n\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_59n\_ .icon-shape p,#chatgpt-mermaid-\_r_59n\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_59n\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_59n\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_59n\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_59n\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_59n\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_59n\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_59n\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_59n\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_59n\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_59n\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_59n\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_59n\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_59n\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_59n\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_59n\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_59n\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_59n\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_59n\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_59n\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_59n\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_59n\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_59n\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_59n\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_59n\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_59n\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_59n\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_59n\_ .node rect,#chatgpt-mermaid-\_r_59n\_ .node circle,#chatgpt-mermaid-\_r_59n\_ .node ellipse,#chatgpt-mermaid-\_r_59n\_ .node polygon,#chatgpt-mermaid-\_r_59n\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_59n\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_59n\_ .node.mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_59n\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_59n\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_59n\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}Request for Tenant TResolve Residency RulesDetermine Eligible / HomeRegionConsistent Hash WithinRegionSelect Authoritative ShardOperation TypeAuthoritative WriterApply Read-ConsistencyPolicyEligible Read EndpointWriteRead

The key is that the hash ring operates over the eligible shard set, not over every shard worldwide.

Suppose tenant `tenant-42` is assigned to the India region. The application might calculate:

```
region = IN
shard = ring_IN.getNode("tenant-42")
```

The exact API is illustrative, but the design intent is important.

If India has 12 eligible shards, the hash ring chooses among those shards according to the ring's placement rules.

If Europe has 20 shards, those extra European shards should not affect the India tenant's ownership merely because the global system has more capacity in Europe.

### What if a region is added?

Suppose you add a new India shard.

Consistent hashing can reduce the number of keys whose intended shard assignment changes. But the affected data still needs to be migrated, and routing must not switch to the new shard before the data is available there.

### What if the India region fails?

Do not automatically hash the tenant to any available shard in Europe and begin writing.

Instead, a failover policy must determine whether Europe is an authorized recovery destination, whether it has a sufficiently current copy, and whether the previous writer has been fenced.

The consistent hash ring can then help distribute keys among eligible shards in the recovery region if the architecture supports that placement model.

Consistent hashing reduces remapping; it does not establish authority or guarantee that the selected shard has the latest data.

## 5. Users move between regions. Should their data move too?

No—not automatically.

This distinction is fundamental to multi-region design.

Imagine a customer creates an account in India and later travels to Europe.

There are now three different locations to consider:

- User location: Where the user is physically connecting from.
- Request-serving region: Where the application handles the request.
- Data home region: Where the authoritative data belongs.

These locations can differ without any problem.

For example:

\#chatgpt-mermaid-\_r_5a5\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_5a5\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_5a5\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_5a5\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_5a5\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_5a5\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_5a5\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_5a5\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_5a5\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_5a5\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_5a5\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_5a5\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_5a5\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_5a5\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_5a5\_ p{margin:0;}#chatgpt-mermaid-\_r_5a5\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_5a5\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_5a5\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_5a5\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_5a5\_ .label text,#chatgpt-mermaid-\_r_5a5\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_5a5\_ .node rect,#chatgpt-mermaid-\_r_5a5\_ .node circle,#chatgpt-mermaid-\_r_5a5\_ .node ellipse,#chatgpt-mermaid-\_r_5a5\_ .node polygon,#chatgpt-mermaid-\_r_5a5\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_5a5\_ .rough-node .label text,#chatgpt-mermaid-\_r_5a5\_ .node .label text,#chatgpt-mermaid-\_r_5a5\_ .image-shape .label,#chatgpt-mermaid-\_r_5a5\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_5a5\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_5a5\_ .rough-node .label,#chatgpt-mermaid-\_r_5a5\_ .node .label,#chatgpt-mermaid-\_r_5a5\_ .image-shape .label,#chatgpt-mermaid-\_r_5a5\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_5a5\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_5a5\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_5a5\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_5a5\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_5a5\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_5a5\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_5a5\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_5a5\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_5a5\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_5a5\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_5a5\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_5a5\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_5a5\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_5a5\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_5a5\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_5a5\_ .icon-shape,#chatgpt-mermaid-\_r_5a5\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_5a5\_ .icon-shape p,#chatgpt-mermaid-\_r_5a5\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_5a5\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_5a5\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_5a5\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_5a5\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_5a5\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_5a5\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_5a5\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_5a5\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_5a5\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_5a5\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_5a5\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_5a5\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_5a5\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_5a5\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_5a5\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_5a5\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_5a5\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_5a5\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_5a5\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_5a5\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_5a5\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_5a5\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_5a5\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_5a5\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_5a5\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_5a5\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_5a5\_ .node rect,#chatgpt-mermaid-\_r_5a5\_ .node circle,#chatgpt-mermaid-\_r_5a5\_ .node ellipse,#chatgpt-mermaid-\_r_5a5\_ .node polygon,#chatgpt-mermaid-\_r_5a5\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_5a5\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_5a5\_ .node.mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_5a5\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_5a5\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_5a5\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}Customer currently in EuropeEurope Entry PointResolve Tenant OwnershipIndia Data OwnerReturn Response

The customer can continue using the application without transferring the tenant's data to Europe.

### Why not move the data whenever the user moves?

Because user movement is often temporary or ambiguous.

A user might:

- Travel for a week.
- Connect through a corporate VPN.
- Use a mobile network whose traffic exits in another country.
- Log in from a different device.
- Share a tenant with colleagues located in multiple regions.

Moving data based on IP geolocation or the last region visited would create unnecessary migrations and unpredictable ownership changes.

### When should data actually move?

When an explicit business or administrative event changes the ownership or residency policy.

For example:

- A customer requests a tenant-region migration.
- A company changes its contractual hosting region.
- A data residency policy requires relocation.
- An administrator moves a tenant to a different capacity cell.

A tenant migration is a data-management workflow, not a side effect of routing.

A safe migration commonly involves:

1. Validate that the destination is permitted and capable of hosting the tenant.
2. Copy the existing data.
3. Synchronize changes made during the copy.
4. Verify that the destination has caught up.
5. Fence writes to the old owner.
6. Atomically update authoritative ownership metadata.
7. Route new operations to the destination.
8. Verify correctness and retain a rollback or recovery plan appropriate to the migration.

The precise cutover protocol depends on the database and whether writes can be paused. But the invariant is the same: there must be a clear authoritative owner throughout the transition.

## 6. Read-after-write consistency across regions

Now let's examine a common user-facing failure.

A user updates their profile in India:

```
PATCH /profile
{ "displayName": "Asha" }
```

The India primary commits the change and returns success.

Immediately afterward, the user makes another request. This time, global routing sends the request to Europe, where a replica has not yet received the update.

The response returns the old profile.

From the user's perspective, the system appears to have lost the update, even though the write succeeded.

This is a read-after-write consistency problem.

### Option A: Read from the authoritative writer

After a write, route subsequent reads to the authoritative writer.

This is straightforward, but may increase latency for a user far from that writer.

### Option B: Session or tenant affinity

Keep relevant requests associated with the authoritative region, at least for the consistency window required by the application.

For example, a tenant's profile updates and subsequent reads can continue to go to its home region.

This does not require sticky routing to one physical application instance. The important affinity is to the correct data owner or consistency domain.

### Option C: Replication-position tokens

Some databases expose a replication position or consistency token that can help establish whether a replica has caught up to a particular write.

Conceptually:

```
1. Write profile to primary.
2. Receive a commit or replication-position token.
3. Send the next read to an eligible replica.
4. Wait until that replica has applied the required position.
5. Return the result.
```

This only works when the database supports the required token semantics and the application uses them correctly. A generic timestamp is not a substitute for a reliable replication-position guarantee.

### Option D: Accept bounded staleness

For some data, stale reads are acceptable.

For example, a dashboard might tolerate several seconds of delay. A payment authorization decision might not.

The right policy depends on the operation:

| Operation             | Possible consistency policy                                       |
| --------------------- | ----------------------------------------------------------------- |
| User profile display  | Local replica, with read-after-write protection after edits       |
| Product catalog       | Local replica with bounded staleness                              |
| Order status          | Owner read or sufficiently fresh replica                          |
| Inventory reservation | Authoritative transactional write path                            |
| Payment state         | Strongly controlled authoritative state and idempotent processing |
| Analytics dashboard   | Eventual consistency often acceptable                             |

There is no single consistency setting that is optimal for every kind of data.

Design insight: Low latency is not simply minimizing the distance to a database. It is minimizing latency while still satisfying the consistency contract.

## 7. Regional health and failover routing

What should happen when the nearest region becomes unhealthy?

A simple routing system might select the next-nearest region. But a production system needs to distinguish between several kinds of health.

### Layer 1: Entry-point health

Can users reach the region's global or regional endpoint?

### Layer 2: Application health

Can the application instances accept and process requests?

### Layer 3: Dependency health

Can the application reach the databases, queues, caches, and other required services?

### Layer 4: Data readiness

Does the destination have the data and consistency guarantees required for this request?

A region can pass an HTTP health check while still being unable to process an important class of writes.

For example, the API server may be running, but the authoritative database could be unavailable. Treating that region as fully healthy would cause requests to fail after routing.

### Failover example

Suppose India is the primary owner for a set of tenants, and Europe holds a replicated copy.

\#chatgpt-mermaid-\_r_5ao\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_5ao\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_5ao\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_5ao\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_5ao\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_5ao\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_5ao\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_5ao\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_5ao\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_5ao\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_5ao\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_5ao\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_5ao\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_5ao\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_5ao\_ p{margin:0;}#chatgpt-mermaid-\_r_5ao\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_5ao\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_5ao\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_5ao\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_5ao\_ .label text,#chatgpt-mermaid-\_r_5ao\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_5ao\_ .node rect,#chatgpt-mermaid-\_r_5ao\_ .node circle,#chatgpt-mermaid-\_r_5ao\_ .node ellipse,#chatgpt-mermaid-\_r_5ao\_ .node polygon,#chatgpt-mermaid-\_r_5ao\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_5ao\_ .rough-node .label text,#chatgpt-mermaid-\_r_5ao\_ .node .label text,#chatgpt-mermaid-\_r_5ao\_ .image-shape .label,#chatgpt-mermaid-\_r_5ao\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_5ao\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_5ao\_ .rough-node .label,#chatgpt-mermaid-\_r_5ao\_ .node .label,#chatgpt-mermaid-\_r_5ao\_ .image-shape .label,#chatgpt-mermaid-\_r_5ao\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_5ao\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_5ao\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_5ao\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_5ao\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_5ao\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_5ao\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_5ao\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_5ao\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_5ao\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_5ao\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_5ao\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_5ao\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_5ao\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_5ao\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_5ao\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_5ao\_ .icon-shape,#chatgpt-mermaid-\_r_5ao\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_5ao\_ .icon-shape p,#chatgpt-mermaid-\_r_5ao\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_5ao\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_5ao\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_5ao\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_5ao\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_5ao\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_5ao\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_5ao\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_5ao\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_5ao\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_5ao\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_5ao\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_5ao\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_5ao\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_5ao\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_5ao\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_5ao\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_5ao\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_5ao\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_5ao\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_5ao\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_5ao\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_5ao\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_5ao\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_5ao\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_5ao\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_5ao\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_5ao\_ .node rect,#chatgpt-mermaid-\_r_5ao\_ .node circle,#chatgpt-mermaid-\_r_5ao\_ .node ellipse,#chatgpt-mermaid-\_r_5ao\_ .node polygon,#chatgpt-mermaid-\_r_5ao\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_5ao\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_5ao\_ .node.mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_5ao\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_5ao\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_5ao\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}RequestIndia Healthy?Use India OwnerEurope Eligible for Failover?Reject or Degrade SafelyFence Old WriterPromote / Establish NewOwnerUpdate Ownership andRoutingYesNoNoYes

The order matters.

You should not redirect writes to Europe merely because the India health check failed. The system must establish that Europe is an authorized and sufficiently recoverable destination, and that India cannot continue accepting conflicting writes.

Depending on the architecture, that might involve database promotion, a consensus-based ownership mechanism, or a controlled recovery workflow.

### Avoid health-check flapping

If a region alternates between healthy and unhealthy every few seconds, routing can oscillate and worsen the incident.

Common mitigations include:

- Consecutive failure thresholds.
- Recovery thresholds before routing traffic back.
- Hysteresis and cooldown periods.
- Separate checks for routing eligibility and operational alerts.
- Explicit failover state and runbooks.

DNS-based routing has another limitation: DNS caches and client behavior can delay traffic movement even after a record changes.

A global proxy or accelerator can offer different traffic-control behavior, but no routing product removes the need for a safe data failover protocol.

## 8. The routing metadata problem

Suppose the application uses a mapping like this:

```
tenant-42 -> region-IN -> shard-07
tenant-81 -> region-EU -> shard-12
tenant-99 -> region-US -> shard-03
```

Where should this mapping live?

You could derive shard placement from consistent hashing, but the system still needs to know:

- Which regions are eligible for each tenant.
- Which region currently owns the tenant.
- Whether a shard is healthy and writable.
- Which ring or placement version applies.
- Whether a migration or failover is in progress.

This is routing metadata.

### Why routing metadata is critical

Imagine two application instances have different ownership information:

```
Instance A: tenant-42 -> India
Instance B: tenant-42 -> Europe
```

Both instances receive write requests.

If both regions accept writes independently, you may create conflicting updates even if every individual hash calculation is correct.

The problem is not the hash function. The problem is inconsistent ownership information.

### Common design

Use a strongly controlled ownership registry or equivalent authoritative mechanism for ownership transitions.

The application can cache routing information to avoid a remote lookup on every request, but the cache must have a clear invalidation and versioning strategy.

For example:

```
tenantId
homeRegion
shardId
ownershipEpoch
placementVersion
migrationState
```

The fields are illustrative; a real design should include only the information it needs.

- `ownershipEpoch` can identify successive ownership generations.
- `placementVersion` can identify the applicable shard-placement configuration.
- `migrationState` can prevent normal routing while a cutover is in progress.

However, metadata alone is not enough. The old writer must actually be prevented from accepting writes after its authority is revoked.

### Fencing tokens

A fencing token is a monotonically increasing value associated with ownership or lease generations.

For example:

```
India owner: epoch 41
Europe promoted owner: epoch 42
```

A write from the old owner with epoch 41 should be rejected after epoch 42 has become authoritative.

The protection must be enforced at a point that can reliably reject stale writers. Merely attaching an epoch to an application request does not protect the data if the database or write path ignores it.

Fencing is especially important when a network partition makes the old region unreachable to the control plane but leaves it capable of serving some clients.

Important: Consistent hashing can determine a preferred placement, but it does not provide a distributed consensus protocol for ownership changes.

## 9. Caching and low-latency routing

Cross-region routing decisions themselves can add latency.

If every request must query a central ownership service, that service becomes both a latency dependency and a potential bottleneck.

A common solution is to cache routing metadata locally.

For example:

\#chatgpt-mermaid-\_r_5bl\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_5bl\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_5bl\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_5bl\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_5bl\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_5bl\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_5bl\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_5bl\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_5bl\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_5bl\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_5bl\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_5bl\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_5bl\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_5bl\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_5bl\_ p{margin:0;}#chatgpt-mermaid-\_r_5bl\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_5bl\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_5bl\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_5bl\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_5bl\_ .label text,#chatgpt-mermaid-\_r_5bl\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_5bl\_ .node rect,#chatgpt-mermaid-\_r_5bl\_ .node circle,#chatgpt-mermaid-\_r_5bl\_ .node ellipse,#chatgpt-mermaid-\_r_5bl\_ .node polygon,#chatgpt-mermaid-\_r_5bl\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_5bl\_ .rough-node .label text,#chatgpt-mermaid-\_r_5bl\_ .node .label text,#chatgpt-mermaid-\_r_5bl\_ .image-shape .label,#chatgpt-mermaid-\_r_5bl\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_5bl\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_5bl\_ .rough-node .label,#chatgpt-mermaid-\_r_5bl\_ .node .label,#chatgpt-mermaid-\_r_5bl\_ .image-shape .label,#chatgpt-mermaid-\_r_5bl\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_5bl\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_5bl\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_5bl\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_5bl\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_5bl\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_5bl\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_5bl\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_5bl\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_5bl\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_5bl\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_5bl\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_5bl\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_5bl\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_5bl\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_5bl\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_5bl\_ .icon-shape,#chatgpt-mermaid-\_r_5bl\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_5bl\_ .icon-shape p,#chatgpt-mermaid-\_r_5bl\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_5bl\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_5bl\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_5bl\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_5bl\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_5bl\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_5bl\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_5bl\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_5bl\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_5bl\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_5bl\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_5bl\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_5bl\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_5bl\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_5bl\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_5bl\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_5bl\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_5bl\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_5bl\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_5bl\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_5bl\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_5bl\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_5bl\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_5bl\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_5bl\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_5bl\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_5bl\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_5bl\_ .node rect,#chatgpt-mermaid-\_r_5bl\_ .node circle,#chatgpt-mermaid-\_r_5bl\_ .node ellipse,#chatgpt-mermaid-\_r_5bl\_ .node polygon,#chatgpt-mermaid-\_r_5bl\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_5bl\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_5bl\_ .node.mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_5bl\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_5bl\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_5bl\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}Incoming RequestRouting Cache Hit?Validate Version / EligibilityOwnership RegistryUpdate Local CacheRoute to Eligible Owner andShardYesNo

This is effective when routing metadata changes relatively infrequently.

But caching introduces a correctness challenge: a cached route can become stale after a migration, failover, or ownership change.

### How to manage stale routing metadata

Common techniques include:

- Versioned routing metadata.
- Short-lived cache entries where appropriate.
- Explicit invalidation or update notifications.
- Retry logic when a shard reports that ownership has changed.
- Fencing tokens that reject writes from stale owners.
- A fallback to the authoritative registry when the cached route is uncertain.

Do not rely on cache expiration alone for correctness. A stale cache entry can remain in use during its validity period, and clocks or invalidation delivery may not behave as expected during failures.

For writes, ownership enforcement must remain authoritative even when application routing caches are stale.

### Avoid a global lookup on every request

A practical architecture might cache:

```
tenant-42 -> {
    region: "IN",
    shard: "shard-07",
    ownershipEpoch: 42,
    placementVersion: 8
}
```

The application then uses the cached information for normal routing, while ownership changes update the registry and invalidate or supersede older entries.

Consistent hashing can reduce the amount of explicit key-to-shard metadata you need, but you still need authoritative information about eligible regions, ring membership, placement versions, and ownership transitions.

## 10. Putting it all together: a production request flow

Let's design a request path for a multi-region SaaS platform.

Requirements:

- Customers can connect from anywhere.
- Tenants have an assigned home region.
- Each region contains multiple database shards.
- Local replicas can serve selected reads.
- Writes must go to the authoritative owner.
- A regional outage can trigger controlled failover.

A request might follow this sequence:

\#chatgpt-mermaid-\_r_5c3\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_5c3\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_5c3\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_5c3\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_5c3\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_5c3\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_5c3\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_5c3\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_5c3\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_5c3\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_5c3\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_5c3\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_5c3\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_5c3\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_5c3\_ p{margin:0;}#chatgpt-mermaid-\_r_5c3\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_5c3\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_5c3\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_5c3\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_5c3\_ .label text,#chatgpt-mermaid-\_r_5c3\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_5c3\_ .node rect,#chatgpt-mermaid-\_r_5c3\_ .node circle,#chatgpt-mermaid-\_r_5c3\_ .node ellipse,#chatgpt-mermaid-\_r_5c3\_ .node polygon,#chatgpt-mermaid-\_r_5c3\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_5c3\_ .rough-node .label text,#chatgpt-mermaid-\_r_5c3\_ .node .label text,#chatgpt-mermaid-\_r_5c3\_ .image-shape .label,#chatgpt-mermaid-\_r_5c3\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_5c3\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_5c3\_ .rough-node .label,#chatgpt-mermaid-\_r_5c3\_ .node .label,#chatgpt-mermaid-\_r_5c3\_ .image-shape .label,#chatgpt-mermaid-\_r_5c3\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_5c3\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_5c3\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_5c3\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_5c3\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_5c3\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_5c3\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_5c3\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_5c3\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_5c3\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_5c3\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_5c3\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_5c3\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_5c3\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_5c3\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_5c3\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_5c3\_ .icon-shape,#chatgpt-mermaid-\_r_5c3\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_5c3\_ .icon-shape p,#chatgpt-mermaid-\_r_5c3\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_5c3\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_5c3\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_5c3\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_5c3\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_5c3\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_5c3\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_5c3\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_5c3\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_5c3\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_5c3\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_5c3\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_5c3\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_5c3\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_5c3\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_5c3\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_5c3\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_5c3\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_5c3\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_5c3\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_5c3\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_5c3\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_5c3\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_5c3\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_5c3\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_5c3\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_5c3\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_5c3\_ .node rect,#chatgpt-mermaid-\_r_5c3\_ .node circle,#chatgpt-mermaid-\_r_5c3\_ .node ellipse,#chatgpt-mermaid-\_r_5c3\_ .node polygon,#chatgpt-mermaid-\_r_5c3\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_5c3\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_5c3\_ .node.mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_5c3\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_5c3\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_5c3\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}User RequestGlobal Traffic RoutingRegional ApplicationResolve Tenant andResidency PolicyResolve Ownership MetadataSelect Shard Using RegionalPlacementRead or Write?Validate Owner and FencingEpochCommit to AuthoritativeWriterChoose Eligible ReadEndpointApply ConsistencyRequirementsReturn ResponseWriteRead

Notice the ordering:

1. Global routing chooses an entry region.
2. The application identifies the tenant and applicable policy.
3. Ownership metadata identifies the authoritative region and shard.
4. Consistent hashing selects the shard within the eligible placement set, where appropriate.
5. Writes are validated against current ownership.
6. Reads select an endpoint that satisfies the requested consistency.
7. The response is returned.

A real implementation may optimize this flow by caching tenant metadata, combining lookups, or routing some requests directly to the home region. The logical guarantees should remain the same.

## 11. Common design mistakes

| Mistake                                                  | Why it fails                                                 | Better approach                                              |
| -------------------------------------------------------- | ------------------------------------------------------------ | ------------------------------------------------------------ |
| Always choose the nearest region                         | It may not own the data or be allowed to process it          | Filter by policy and ownership first                         |
| Use one global hash ring across every region             | Can violate residency and select remote or ineligible shards | Select the eligible region before hashing                    |
| Move data when the user moves                            | User location does not imply a change in data ownership      | Make tenant migration an explicit workflow                   |
| Read from any local replica                              | The replica may be stale                                     | Apply a defined read-consistency policy                      |
| Redirect writes immediately after a health-check failure | The old writer may still accept writes                       | Fence the old owner and establish a new authoritative owner  |
| Trust cached routing metadata indefinitely               | Ownership may have changed                                   | Version metadata and enforce ownership at the write boundary |
| Assume consistent hashing performs migration             | Placement calculation does not copy existing data            | Use a separate migration and cutover process                 |

## 12. Interview-ready answer

If asked, “How would you design low-latency routing for a multi-region application?”, you could say:

> I would separate global entry routing, application instance routing, and data routing. Global routing would select a healthy, policy-eligible region based on factors such as latency and capacity. Data routing would independently resolve the tenant's authoritative region and shard.
>
> I would use consistent hashing within the eligible shard set to distribute keys and reduce remapping when shard membership changes. For reads, I would select local replicas only when they satisfy the required consistency guarantees. Writes would go to the authoritative owner, with ownership epochs or fencing to prevent stale writers after failover.
>
> I would cache routing metadata for performance, but version it and ensure that stale routing information cannot bypass authoritative write checks. Regional failover and tenant migration would be explicit workflows involving data readiness, synchronization, fencing, and controlled ownership changes.

The core principle is:

Route users toward nearby compute, route data operations toward the correct owner, and use consistent hashing only after eligibility and ownership have been established.

### What comes next?

In the next lesson, we'll examine data placement and replication in greater depth: how to choose shard keys, distribute tenants across regions, place replicas across failure domains, handle hot shards, and decide what to replicate versus what must remain region-local.

Type `next` to continue.




















