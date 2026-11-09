# Chapter 3: Evolving from a Single Region to a Multi-Region Architecture

## Lesson 1: The architecture layers — and where consistent hashing belongs

The most important principle in multi-region system design is:

Decide which region is allowed to own or serve the data first. Only then use consistent hashing to decide which shard or node inside that region should handle it.

This distinction helps avoid many architectural mistakes involving latency, data residency, replication, and regional failover.

## 1. Start with a single-region architecture

Imagine you're building a multi-tenant SaaS application. Customers use it to manage projects, users, reports, and documents.

Initially, everything runs in one AWS region, such as `ap-south-1` (Mumbai).

\#chatgpt-mermaid-\_r_4gt\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_4gt\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_4gt\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_4gt\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_4gt\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4gt\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_4gt\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_4gt\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_4gt\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_4gt\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_4gt\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_4gt\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4gt\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4gt\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_4gt\_ p{margin:0;}#chatgpt-mermaid-\_r_4gt\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4gt\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4gt\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4gt\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_4gt\_ .label text,#chatgpt-mermaid-\_r_4gt\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4gt\_ .node rect,#chatgpt-mermaid-\_r_4gt\_ .node circle,#chatgpt-mermaid-\_r_4gt\_ .node ellipse,#chatgpt-mermaid-\_r_4gt\_ .node polygon,#chatgpt-mermaid-\_r_4gt\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_4gt\_ .rough-node .label text,#chatgpt-mermaid-\_r_4gt\_ .node .label text,#chatgpt-mermaid-\_r_4gt\_ .image-shape .label,#chatgpt-mermaid-\_r_4gt\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_4gt\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_4gt\_ .rough-node .label,#chatgpt-mermaid-\_r_4gt\_ .node .label,#chatgpt-mermaid-\_r_4gt\_ .image-shape .label,#chatgpt-mermaid-\_r_4gt\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_4gt\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_4gt\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4gt\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4gt\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_4gt\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_4gt\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_4gt\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_4gt\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_4gt\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_4gt\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_4gt\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4gt\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4gt\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_4gt\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4gt\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_4gt\_ .icon-shape,#chatgpt-mermaid-\_r_4gt\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_4gt\_ .icon-shape p,#chatgpt-mermaid-\_r_4gt\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_4gt\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_4gt\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_4gt\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_4gt\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_4gt\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_4gt\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_4gt\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_4gt\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_4gt\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4gt\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_4gt\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_4gt\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_4gt\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4gt\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_4gt\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_4gt\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4gt\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_4gt\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_4gt\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4gt\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_4gt\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4gt\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_4gt\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_4gt\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_4gt\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_4gt\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_4gt\_ .node rect,#chatgpt-mermaid-\_r_4gt\_ .node circle,#chatgpt-mermaid-\_r_4gt\_ .node ellipse,#chatgpt-mermaid-\_r_4gt\_ .node polygon,#chatgpt-mermaid-\_r_4gt\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_4gt\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_4gt\_ .node.mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_4gt\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_4gt\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4gt\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}UsersDNSRegional Load BalancerApplication InstancesRedis CachePostgreSQLMessage QueueBackground WorkersObject Storage

The application may already span multiple Availability Zones, but it still belongs to one AWS region.

For example:

- The API runs across several Availability Zones.
- PostgreSQL stores tenant and application data.
- Redis caches frequently accessed information.
- A queue handles asynchronous report generation.
- Object storage stores uploaded documents and generated reports.

At this stage, your application can tolerate some instance and Availability Zone failures, depending on how the components are deployed. But a major regional outage can still affect the entire application.

### What changes when you add a second region?

Suppose you deploy the same application to Singapore.

A tempting approach is to copy the entire stack, add a global load balancer, and direct users to whichever region is closest.

That solves only part of the problem.

You must also decide:

- Which region owns each customer's data?
- Can both regions write to the same customer's data?
- How quickly are database changes replicated?
- What happens when the owning region becomes unavailable?
- Can a customer legally have their data stored in both regions?
- If a user moves from India to Singapore, should their data move too?

These are placement, ownership, consistency, and failover decisions. Consistent hashing addresses only part of placement.

## 2. Understand the different multi-region architectures

Not every multi-region system should work the same way. The right architecture depends on your availability requirements and how your data is written.

### A. Active-passive

One region serves production traffic, while another is prepared to take over.

\#chatgpt-mermaid-\_r_4h6\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_4h6\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_4h6\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_4h6\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_4h6\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4h6\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_4h6\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_4h6\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_4h6\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_4h6\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_4h6\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_4h6\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4h6\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4h6\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_4h6\_ p{margin:0;}#chatgpt-mermaid-\_r_4h6\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4h6\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4h6\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4h6\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_4h6\_ .label text,#chatgpt-mermaid-\_r_4h6\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4h6\_ .node rect,#chatgpt-mermaid-\_r_4h6\_ .node circle,#chatgpt-mermaid-\_r_4h6\_ .node ellipse,#chatgpt-mermaid-\_r_4h6\_ .node polygon,#chatgpt-mermaid-\_r_4h6\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_4h6\_ .rough-node .label text,#chatgpt-mermaid-\_r_4h6\_ .node .label text,#chatgpt-mermaid-\_r_4h6\_ .image-shape .label,#chatgpt-mermaid-\_r_4h6\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_4h6\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_4h6\_ .rough-node .label,#chatgpt-mermaid-\_r_4h6\_ .node .label,#chatgpt-mermaid-\_r_4h6\_ .image-shape .label,#chatgpt-mermaid-\_r_4h6\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_4h6\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_4h6\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4h6\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4h6\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_4h6\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_4h6\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_4h6\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_4h6\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_4h6\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_4h6\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_4h6\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4h6\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4h6\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_4h6\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4h6\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_4h6\_ .icon-shape,#chatgpt-mermaid-\_r_4h6\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_4h6\_ .icon-shape p,#chatgpt-mermaid-\_r_4h6\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_4h6\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_4h6\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_4h6\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_4h6\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_4h6\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_4h6\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_4h6\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_4h6\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_4h6\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4h6\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_4h6\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_4h6\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_4h6\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4h6\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_4h6\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_4h6\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4h6\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_4h6\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_4h6\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4h6\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_4h6\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4h6\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_4h6\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_4h6\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_4h6\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_4h6\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_4h6\_ .node rect,#chatgpt-mermaid-\_r_4h6\_ .node circle,#chatgpt-mermaid-\_r_4h6\_ .node ellipse,#chatgpt-mermaid-\_r_4h6\_ .node polygon,#chatgpt-mermaid-\_r_4h6\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_4h6\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_4h6\_ .node.mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_4h6\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_4h6\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4h6\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}UsersGlobal Traffic ManagerRegion A: ActiveRegion B: PassivePrimary DatabaseStandby DatabaseNormal trafficStandbyReplication

How it works:

1. Region A handles normal traffic.
2. Database changes replicate to Region B.
3. Region B stays ready for a disaster.
4. If Region A fails, the failover process promotes or activates the standby infrastructure.
5. Traffic is redirected to Region B.

Advantages

- Simpler write ownership.
- Fewer concurrent-write conflicts.
- Easier to reason about data consistency.

Disadvantages

- Standby resources may be underutilized.
- Failover takes time.
- Replication lag can cause data loss if the standby is promoted before receiving all committed changes.

This is a good starting point when disaster recovery matters more than serving writes locally from every region.

### B. Active-active compute with a single-writer data model

Both regions handle requests, but a given data partition has one authoritative writer.

\#chatgpt-mermaid-\_r_4hf\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_4hf\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_4hf\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_4hf\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_4hf\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4hf\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_4hf\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_4hf\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_4hf\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_4hf\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_4hf\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_4hf\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4hf\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4hf\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_4hf\_ p{margin:0;}#chatgpt-mermaid-\_r_4hf\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4hf\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4hf\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4hf\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_4hf\_ .label text,#chatgpt-mermaid-\_r_4hf\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4hf\_ .node rect,#chatgpt-mermaid-\_r_4hf\_ .node circle,#chatgpt-mermaid-\_r_4hf\_ .node ellipse,#chatgpt-mermaid-\_r_4hf\_ .node polygon,#chatgpt-mermaid-\_r_4hf\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_4hf\_ .rough-node .label text,#chatgpt-mermaid-\_r_4hf\_ .node .label text,#chatgpt-mermaid-\_r_4hf\_ .image-shape .label,#chatgpt-mermaid-\_r_4hf\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_4hf\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_4hf\_ .rough-node .label,#chatgpt-mermaid-\_r_4hf\_ .node .label,#chatgpt-mermaid-\_r_4hf\_ .image-shape .label,#chatgpt-mermaid-\_r_4hf\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_4hf\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_4hf\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4hf\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4hf\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_4hf\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_4hf\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_4hf\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_4hf\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_4hf\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_4hf\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_4hf\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4hf\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4hf\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_4hf\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4hf\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_4hf\_ .icon-shape,#chatgpt-mermaid-\_r_4hf\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_4hf\_ .icon-shape p,#chatgpt-mermaid-\_r_4hf\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_4hf\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_4hf\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_4hf\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_4hf\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_4hf\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_4hf\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_4hf\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_4hf\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_4hf\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4hf\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_4hf\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_4hf\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_4hf\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4hf\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_4hf\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_4hf\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4hf\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_4hf\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_4hf\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4hf\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_4hf\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4hf\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_4hf\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_4hf\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_4hf\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_4hf\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_4hf\_ .node rect,#chatgpt-mermaid-\_r_4hf\_ .node circle,#chatgpt-mermaid-\_r_4hf\_ .node ellipse,#chatgpt-mermaid-\_r_4hf\_ .node polygon,#chatgpt-mermaid-\_r_4hf\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_4hf\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_4hf\_ .node.mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_4hf\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_4hf\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4hf\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}UsersGlobal Traffic ManagerRegion A: API and WorkersRegion B: API and WorkersOwnership and RoutingRegion A PrimaryRegion B PrimaryPartition owned by APartition owned by BAsync replication, whereconfigured

This diagram is conceptual: each region may own many partitions, and the databases may be different physical clusters rather than two replicas of one database.

For example:

- Tenant `tenant-101` is owned by Region A.
- Tenant `tenant-202` is owned by Region B.
- Both regions run application servers.
- Requests are routed to the authoritative owner for each tenant's data.

A request arriving in Region B for `tenant-101` might be proxied to Region A or redirected through an appropriate routing layer.

The critical point: Both regions are active, but both do not necessarily write the same partition.

This architecture can improve regional availability and resource utilization while avoiding many multi-writer conflict problems.

### C. Active-active multi-writer data

Both regions can accept writes to the same logical data.

\#chatgpt-mermaid-\_r_4ho\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_4ho\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_4ho\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_4ho\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_4ho\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4ho\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_4ho\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_4ho\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_4ho\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_4ho\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_4ho\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_4ho\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4ho\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4ho\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_4ho\_ p{margin:0;}#chatgpt-mermaid-\_r_4ho\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4ho\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4ho\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4ho\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_4ho\_ .label text,#chatgpt-mermaid-\_r_4ho\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4ho\_ .node rect,#chatgpt-mermaid-\_r_4ho\_ .node circle,#chatgpt-mermaid-\_r_4ho\_ .node ellipse,#chatgpt-mermaid-\_r_4ho\_ .node polygon,#chatgpt-mermaid-\_r_4ho\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_4ho\_ .rough-node .label text,#chatgpt-mermaid-\_r_4ho\_ .node .label text,#chatgpt-mermaid-\_r_4ho\_ .image-shape .label,#chatgpt-mermaid-\_r_4ho\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_4ho\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_4ho\_ .rough-node .label,#chatgpt-mermaid-\_r_4ho\_ .node .label,#chatgpt-mermaid-\_r_4ho\_ .image-shape .label,#chatgpt-mermaid-\_r_4ho\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_4ho\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_4ho\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4ho\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4ho\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_4ho\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_4ho\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_4ho\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_4ho\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_4ho\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_4ho\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_4ho\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4ho\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4ho\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_4ho\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4ho\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_4ho\_ .icon-shape,#chatgpt-mermaid-\_r_4ho\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_4ho\_ .icon-shape p,#chatgpt-mermaid-\_r_4ho\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_4ho\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_4ho\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_4ho\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_4ho\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_4ho\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_4ho\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_4ho\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_4ho\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_4ho\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4ho\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_4ho\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_4ho\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_4ho\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4ho\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_4ho\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_4ho\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4ho\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_4ho\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_4ho\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4ho\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_4ho\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4ho\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_4ho\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_4ho\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_4ho\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_4ho\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_4ho\_ .node rect,#chatgpt-mermaid-\_r_4ho\_ .node circle,#chatgpt-mermaid-\_r_4ho\_ .node ellipse,#chatgpt-mermaid-\_r_4ho\_ .node polygon,#chatgpt-mermaid-\_r_4ho\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_4ho\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_4ho\_ .node.mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_4ho\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_4ho\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4ho\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}Region A ApplicationRegion A DatabaseRegion B ApplicationRegion B DatabaseReplication and ConflictResolution

This model is more complex.

Suppose a user changes an account's billing address in Region A while another request changes it in Region B. Both regions might accept the changes before learning about the other.

The system needs an explicit conflict policy, such as:

- Last-write-wins, with carefully defined timestamp semantics.
- Application-specific conflict resolution.
- Version-based conflict detection.
- CRDTs for data types whose operations can be merged safely.
- Strongly coordinated transactions, where supported and justified.

Not all data is suitable for conflict-free multi-writer replication. Payments, inventory reservations, and account balances often require stronger safeguards than simple preferences or independently editable metadata.

Interview takeaway: Active-active infrastructure does not automatically mean active-active writes to every database.

## 3. The four decisions in data placement

Now let's connect multi-region architecture to consistent hashing.

Imagine the application has 10,000 tenants and multiple database shards across India, Singapore, and Europe.

You need to make four distinct decisions.

\#chatgpt-mermaid-\_r_4i1\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_4i1\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_4i1\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_4i1\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_4i1\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4i1\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_4i1\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_4i1\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_4i1\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_4i1\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_4i1\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_4i1\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4i1\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4i1\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_4i1\_ p{margin:0;}#chatgpt-mermaid-\_r_4i1\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4i1\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4i1\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4i1\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_4i1\_ .label text,#chatgpt-mermaid-\_r_4i1\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4i1\_ .node rect,#chatgpt-mermaid-\_r_4i1\_ .node circle,#chatgpt-mermaid-\_r_4i1\_ .node ellipse,#chatgpt-mermaid-\_r_4i1\_ .node polygon,#chatgpt-mermaid-\_r_4i1\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_4i1\_ .rough-node .label text,#chatgpt-mermaid-\_r_4i1\_ .node .label text,#chatgpt-mermaid-\_r_4i1\_ .image-shape .label,#chatgpt-mermaid-\_r_4i1\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_4i1\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_4i1\_ .rough-node .label,#chatgpt-mermaid-\_r_4i1\_ .node .label,#chatgpt-mermaid-\_r_4i1\_ .image-shape .label,#chatgpt-mermaid-\_r_4i1\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_4i1\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_4i1\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4i1\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4i1\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_4i1\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_4i1\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_4i1\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_4i1\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_4i1\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_4i1\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_4i1\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4i1\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4i1\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_4i1\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4i1\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_4i1\_ .icon-shape,#chatgpt-mermaid-\_r_4i1\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_4i1\_ .icon-shape p,#chatgpt-mermaid-\_r_4i1\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_4i1\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_4i1\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_4i1\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_4i1\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_4i1\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_4i1\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_4i1\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_4i1\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_4i1\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4i1\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_4i1\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_4i1\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_4i1\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4i1\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_4i1\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_4i1\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4i1\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_4i1\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_4i1\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4i1\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_4i1\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4i1\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_4i1\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_4i1\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_4i1\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_4i1\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_4i1\_ .node rect,#chatgpt-mermaid-\_r_4i1\_ .node circle,#chatgpt-mermaid-\_r_4i1\_ .node ellipse,#chatgpt-mermaid-\_r_4i1\_ .node polygon,#chatgpt-mermaid-\_r_4i1\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_4i1\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_4i1\_ .node.mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_4i1\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_4i1\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4i1\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}Incoming Request1. Residency and AccessPolicy2. Select Eligible Region3. Determine Data Owner orPartition4. Select Shard or NodeValidate Ownership andRouteData Store

### Decision 1: Residency and access policy

First determine which regions are permitted to hold or process the relevant data.

For example:

- Tenant A's contract requires data to remain in the EU.
- Tenant B is allowed to store data in India and Singapore.
- Tenant C has a customer-specific contractual restriction.

The actual policy must be derived from applicable law, contractual obligations, and the data classification—not merely from the user's current IP address.

If a tenant's data must remain in the EU, a globally distributed hash ring must not accidentally assign that tenant's primary data to an Indian node.

### Decision 2: Select an eligible region

Among permitted regions, select the region according to the application's policy.

Possible criteria include:

- The tenant's home region.
- Network latency.
- Regional health.
- Data availability.
- Regulatory restrictions.
- Business-specific routing requirements.

These criteria may conflict. For example, the closest region may be unavailable, or the lowest-latency region may not be allowed to store the data.

The system needs a defined priority order.

### Decision 3: Determine the data owner

Determine which partition or authoritative database owns the requested entity.

For a tenant-oriented SaaS system, the ownership key might be:

```
tenant-101
```

For a system partitioned by customer and entity:

```
tenant-101:project-987
```

The choice affects colocation, load distribution, and the complexity of cross-partition operations.

### Decision 4: Select a shard or node

Now consistent hashing becomes useful.

Suppose the chosen region contains these eligible database shards:

```
Region: ap-south-1

Shard A
Shard B
Shard C
Shard D
```

You can apply consistent hashing to determine which shard owns a tenant or partition key.

If Shard C is added, only a subset of keys should move under the usual consistent-hashing assumptions.

The ring is responsible for selecting a shard within the relevant ownership scope. It does not decide whether the data is legally permitted to live in that region.

## 4. Why a single global hash ring is often the wrong design

Consider a global ring containing database nodes from three regions:

```
Global ring:
    Mumbai DB 1
    Mumbai DB 2
    Singapore DB 1
    Frankfurt DB 1
```

You calculate:

```
node = ring.getNode(hash(tenantId));
```

This looks simple, but it introduces several problems.

### Problem 1: Placement may violate residency rules

A tenant restricted to EU storage could hash to a Mumbai database.

The hash function knows nothing about legal or contractual requirements.

### Problem 2: Placement may increase latency

An Indian user accessing a tenant whose database is assigned to Frankfurt may incur cross-region network latency for every database interaction.

That can affect API latency, database connections, and synchronous application workflows.

### Problem 3: A regional outage can leave the selected node unreachable

If the hash ring assigns a tenant to Singapore and Singapore becomes unavailable, calculating the same hash still selects the unavailable node.

Consistent hashing does not inherently know that the region is unhealthy.

### Problem 4: Redirecting the key does not move the data

Suppose you remove the Singapore nodes from the ring and recalculate ownership.

The tenant may now map to Mumbai.

But the tenant's records have not magically moved to Mumbai. They might still exist only in Singapore, or replication may not have delivered the latest writes.

Changing the routing decision is not equivalent to moving or recovering the data.

### Better approach

Filter eligible regions and nodes before applying the placement algorithm.

```
tenantId
    |
    v
Resolve residency and access policy
    |
    v
Select permitted, healthy region
    |
    v
Resolve authoritative partition
    |
    v
Consistent hash among eligible shards
    |
    v
Validate ownership and execute request
```

In some systems, the partition owner is established by a control-plane mapping rather than calculated directly from the hash ring. That is often preferable when ownership changes require a coordinated migration or failover.

## 5. How to evolve an existing single-region application safely

A practical migration is incremental. You do not have to jump straight into active-active multi-writer databases.

| Stage | What you do                                                         | What you gain                                |
| ----- | ------------------------------------------------------------------- | -------------------------------------------- |
| 1     | Make the existing region highly available across Availability Zones | Better protection against localized failures |
| 2     | Externalize application state and make services replaceable         | Easier deployment in another region          |
| 3     | Establish explicit data ownership and residency rules               | Predictable placement                        |
| 4     | Deploy application infrastructure in a second region                | Regional deployment readiness                |
| 5     | Add global traffic management and health checks                     | Regional traffic routing                     |
| 6     | Configure database replication, backups, and recovery procedures    | A path to data recovery                      |
| 7     | Implement controlled failover and ownership fencing                 | Reduced risk of conflicting writers          |
| 8     | Test regional failures and recovery regularly                       | Evidence that the design works               |

### Stage 1: Make one region resilient first

Before expanding globally, ensure that a single-region deployment handles the failures it is designed to survive.

For example:

- Multiple application instances across Availability Zones.
- A database deployment with appropriate high availability.
- Backups and tested restores.
- Durable queues and retry handling.
- Idempotent background jobs.
- Monitoring and alerting.

A multi-region architecture does not compensate for broken recovery procedures or unreliable application behavior.

### Stage 2: Externalize state

Application instances should not depend on local memory or local disk as the only place where important state exists.

For example, if a user starts generating a report on one instance, another instance should be able to continue the workflow using durable state.

Externalize the appropriate state into systems such as databases, object storage, or durable queues. Then determine how each of those systems will behave across regions.

This is also where you decide which data is regional, which is globally accessible, and which can be replicated.

### Stage 3: Add a second region deliberately

Deploying the same application code to another region is not enough.

You also need to answer:

- Does it connect to the existing region's database?
- Does it use a local read replica?
- Can it accept writes?
- What happens when the network between regions fails?
- Which region becomes authoritative after failover?

For an initial implementation, you might run application servers in two regions while maintaining a single authoritative write region. This can provide regional deployment flexibility, but it does not automatically provide independent write availability if that region fails.

### Stage 4: Add routing, replication, and controlled failover

Global traffic routing decides where requests enter the system. Data routing decides where the relevant data lives and which node owns it.

Those decisions must work together, but they are not interchangeable.

A traffic manager might route a user to Singapore because Mumbai is unhealthy. The Singapore application must still determine whether the requested data is available locally, must be accessed remotely, or requires an ownership transfer.

Before allowing Singapore to become the writer, the system must also prevent the old writer from continuing to accept conflicting writes.

We will examine this in detail in later lessons.

## 6. What happens when a user moves between regions?

Suppose a customer creates an account in India and later travels to Singapore.

There are three distinct concepts:

1. User location: Where the user is currently connecting from.
2. Request-serving region: Where the API request is processed.
3. Data ownership region: Where the authoritative data resides.

These do not have to be identical.

For example:

```
User currently in Singapore
          |
          v
Singapore API region
          |
          v
Tenant home region: Mumbai
          |
          v
Mumbai owns the tenant's primary data
```

The Singapore API might access a local replica for eligible reads and route writes to Mumbai. Alternatively, the request may be routed to Mumbai entirely.

A user's physical movement should not automatically trigger a data migration. Moving data is a separate workflow involving policy validation, data transfer, replication catch-up, ownership changes, and verification.

For a tenant whose data is legally restricted to the EU, the application must not transfer restricted data outside the permitted boundary merely because the user has travelled.

## 7. Key principles to remember

- Global traffic routing determines where a request enters the application.
- Residency and access policy determines which locations are permitted.
- Regional routing selects an eligible region.
- Partition ownership determines which partition is authoritative.
- Consistent hashing can select a shard or node within the eligible ownership scope.
- Replication copies data between locations; it does not by itself establish safe ownership.
- Failover changes the serving or ownership arrangement and requires a safe recovery procedure.
- Migration physically or logically moves data; changing a hash ring alone does not do this.

The biggest architectural mistake is treating all of these as one routing problem.

### Interview question

Why shouldn't we simply put all database nodes from all regions into one consistent-hashing ring?

A strong answer:

> A global ring treats all nodes as equally eligible unless additional constraints are applied. That can violate data-residency rules, increase request latency, and route requests to regions that do not have the authoritative data. It also does not solve replication or failover. I would first enforce residency and access policies, choose an eligible region, establish partition ownership, and then use consistent hashing within the eligible shard set. Any ownership change must be coordinated with data migration or replication and a safe cutover process.

Next lesson: We'll examine the major multi-region deployment models in more depth: active-passive, active-active, home-region architectures, and cell-based architectures. We'll compare their availability, latency, consistency, and operational trade-offs, and identify when each is appropriate. Type `next` to continue.






















# Chapter 3: Evolving from a Single Region to a Multi-Region Architecture

## Lesson 2: Active-Passive vs Active-Active vs Home-Region vs Cell-Based Architecture

In the previous lesson, we separated four decisions: residency policy, region selection, partition ownership, and shard selection.

Now we need to answer a more fundamental system-design question:

How should we organize the application across regions so that it remains available, performs well, and protects data correctly when something fails?

There is no single best multi-region architecture. The right choice depends on your availability target, write patterns, consistency requirements, data-residency constraints, and operational complexity.

## 1. Active-passive architecture

In an active-passive design, one region handles production traffic while another is prepared to take over.

\#chatgpt-mermaid-\_r_4ke\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_4ke\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_4ke\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_4ke\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_4ke\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4ke\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_4ke\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_4ke\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_4ke\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_4ke\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_4ke\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_4ke\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4ke\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4ke\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_4ke\_ p{margin:0;}#chatgpt-mermaid-\_r_4ke\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4ke\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4ke\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4ke\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_4ke\_ .label text,#chatgpt-mermaid-\_r_4ke\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4ke\_ .node rect,#chatgpt-mermaid-\_r_4ke\_ .node circle,#chatgpt-mermaid-\_r_4ke\_ .node ellipse,#chatgpt-mermaid-\_r_4ke\_ .node polygon,#chatgpt-mermaid-\_r_4ke\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_4ke\_ .rough-node .label text,#chatgpt-mermaid-\_r_4ke\_ .node .label text,#chatgpt-mermaid-\_r_4ke\_ .image-shape .label,#chatgpt-mermaid-\_r_4ke\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_4ke\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_4ke\_ .rough-node .label,#chatgpt-mermaid-\_r_4ke\_ .node .label,#chatgpt-mermaid-\_r_4ke\_ .image-shape .label,#chatgpt-mermaid-\_r_4ke\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_4ke\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_4ke\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4ke\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4ke\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_4ke\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_4ke\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_4ke\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_4ke\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_4ke\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_4ke\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_4ke\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4ke\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4ke\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_4ke\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4ke\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_4ke\_ .icon-shape,#chatgpt-mermaid-\_r_4ke\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_4ke\_ .icon-shape p,#chatgpt-mermaid-\_r_4ke\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_4ke\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_4ke\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_4ke\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_4ke\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_4ke\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_4ke\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_4ke\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_4ke\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_4ke\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4ke\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_4ke\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_4ke\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_4ke\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4ke\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_4ke\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_4ke\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4ke\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_4ke\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_4ke\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4ke\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_4ke\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4ke\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_4ke\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_4ke\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_4ke\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_4ke\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_4ke\_ .node rect,#chatgpt-mermaid-\_r_4ke\_ .node circle,#chatgpt-mermaid-\_r_4ke\_ .node ellipse,#chatgpt-mermaid-\_r_4ke\_ .node polygon,#chatgpt-mermaid-\_r_4ke\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_4ke\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_4ke\_ .node.mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_4ke\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_4ke\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4ke\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}UsersGlobal Traffic ManagerRegion A: ActiveRegion B: StandbyPrimary DatabaseStandby DatabaseReplication

### How it works

Suppose Region A is in Mumbai and Region B is in Singapore.

Under normal conditions:

1. Users access the application in Mumbai.
2. The application writes to the primary database in Mumbai.
3. Database changes replicate to Singapore.
4. Singapore maintains enough infrastructure and data to take over.

If Mumbai experiences a major outage, the recovery process activates Singapore.

However, changing traffic routing is only one part of failover. The recovery process must also establish whether the standby database can safely become authoritative.

### What happens during a failure?

Consider this sequence:

```
10:00:00  Mumbai commits payment P100.
10:00:01  The commit has not replicated to Singapore.
10:00:02  Mumbai becomes unavailable.
10:00:10  Singapore is promoted.
```

If Singapore's replica is missing the latest transaction, that transaction may be lost when the system fails over.

The actual risk depends on the replication mode and the database's guarantees. Asynchronous replication commonly permits some data loss during disaster recovery; synchronous replication can reduce that risk but may add latency or reduce write availability when the required acknowledgements cannot be obtained.

### Advantages

- Easier to maintain one authoritative writer.
- Lower risk of conflicting writes.
- Relatively straightforward database ownership.
- A useful disaster-recovery design.

### Disadvantages

- Failover may take minutes or longer, depending on automation and recovery procedures.
- Standby resources may be underutilized.
- Replication lag can cause data loss.
- The application may become unavailable while the standby is promoted and validated.

### When to use it

Choose active-passive when:

- Disaster recovery is important, but local writes from every region are not required.
- You want a simpler consistency model.
- You can tolerate some failover downtime.
- You have a defined and acceptable recovery point objective (RPO).

For many business applications, this is a sensible starting point.

## 2. Active-active architecture

In active-active, multiple regions serve production traffic simultaneously.

But there is an important distinction: active-active application servers do not require active-active database writes.

There are several possible designs.

### Model A: Active-active compute, single authoritative writer

Both regions handle requests, but each data partition has one authoritative writer.

\#chatgpt-mermaid-\_r_4ks\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_4ks\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_4ks\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_4ks\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_4ks\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4ks\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_4ks\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_4ks\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_4ks\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_4ks\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_4ks\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_4ks\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4ks\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4ks\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_4ks\_ p{margin:0;}#chatgpt-mermaid-\_r_4ks\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4ks\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4ks\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4ks\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_4ks\_ .label text,#chatgpt-mermaid-\_r_4ks\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4ks\_ .node rect,#chatgpt-mermaid-\_r_4ks\_ .node circle,#chatgpt-mermaid-\_r_4ks\_ .node ellipse,#chatgpt-mermaid-\_r_4ks\_ .node polygon,#chatgpt-mermaid-\_r_4ks\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_4ks\_ .rough-node .label text,#chatgpt-mermaid-\_r_4ks\_ .node .label text,#chatgpt-mermaid-\_r_4ks\_ .image-shape .label,#chatgpt-mermaid-\_r_4ks\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_4ks\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_4ks\_ .rough-node .label,#chatgpt-mermaid-\_r_4ks\_ .node .label,#chatgpt-mermaid-\_r_4ks\_ .image-shape .label,#chatgpt-mermaid-\_r_4ks\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_4ks\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_4ks\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4ks\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4ks\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_4ks\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_4ks\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_4ks\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_4ks\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_4ks\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_4ks\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_4ks\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4ks\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4ks\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_4ks\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4ks\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_4ks\_ .icon-shape,#chatgpt-mermaid-\_r_4ks\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_4ks\_ .icon-shape p,#chatgpt-mermaid-\_r_4ks\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_4ks\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_4ks\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_4ks\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_4ks\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_4ks\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_4ks\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_4ks\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_4ks\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_4ks\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4ks\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_4ks\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_4ks\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_4ks\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4ks\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_4ks\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_4ks\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4ks\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_4ks\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_4ks\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4ks\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_4ks\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4ks\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_4ks\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_4ks\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_4ks\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_4ks\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_4ks\_ .node rect,#chatgpt-mermaid-\_r_4ks\_ .node circle,#chatgpt-mermaid-\_r_4ks\_ .node ellipse,#chatgpt-mermaid-\_r_4ks\_ .node polygon,#chatgpt-mermaid-\_r_4ks\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_4ks\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_4ks\_ .node.mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_4ks\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_4ks\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4ks\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}UsersGlobal Traffic ManagerRegion A: APIRegion B: APIPartition Ownership RoutingWriter AWriter BPartition XPartition YReplication if configured

For example:

- Region A owns tenants 1–500.
- Region B owns tenants 501–1000.
- Both regions handle incoming requests.
- A request can enter either region, but writes are routed to the authoritative owner.

This design can distribute workloads across regions without allowing arbitrary concurrent writes to the same partition.

The partition allocation does not have to be a fixed tenant range. It can be maintained in an ownership directory or calculated using consistent hashing, provided ownership transitions are coordinated.

### Model B: Active-active multi-writer database

Multiple regions accept writes to the same logical data.

\#chatgpt-mermaid-\_r_4l5\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_4l5\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_4l5\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_4l5\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_4l5\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4l5\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_4l5\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_4l5\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_4l5\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_4l5\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_4l5\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_4l5\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4l5\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4l5\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_4l5\_ p{margin:0;}#chatgpt-mermaid-\_r_4l5\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4l5\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4l5\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4l5\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_4l5\_ .label text,#chatgpt-mermaid-\_r_4l5\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4l5\_ .node rect,#chatgpt-mermaid-\_r_4l5\_ .node circle,#chatgpt-mermaid-\_r_4l5\_ .node ellipse,#chatgpt-mermaid-\_r_4l5\_ .node polygon,#chatgpt-mermaid-\_r_4l5\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_4l5\_ .rough-node .label text,#chatgpt-mermaid-\_r_4l5\_ .node .label text,#chatgpt-mermaid-\_r_4l5\_ .image-shape .label,#chatgpt-mermaid-\_r_4l5\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_4l5\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_4l5\_ .rough-node .label,#chatgpt-mermaid-\_r_4l5\_ .node .label,#chatgpt-mermaid-\_r_4l5\_ .image-shape .label,#chatgpt-mermaid-\_r_4l5\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_4l5\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_4l5\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4l5\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4l5\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_4l5\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_4l5\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_4l5\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_4l5\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_4l5\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_4l5\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_4l5\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4l5\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4l5\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_4l5\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4l5\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_4l5\_ .icon-shape,#chatgpt-mermaid-\_r_4l5\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_4l5\_ .icon-shape p,#chatgpt-mermaid-\_r_4l5\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_4l5\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_4l5\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_4l5\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_4l5\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_4l5\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_4l5\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_4l5\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_4l5\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_4l5\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4l5\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_4l5\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_4l5\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_4l5\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4l5\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_4l5\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_4l5\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4l5\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_4l5\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_4l5\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4l5\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_4l5\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4l5\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_4l5\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_4l5\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_4l5\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_4l5\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_4l5\_ .node rect,#chatgpt-mermaid-\_r_4l5\_ .node circle,#chatgpt-mermaid-\_r_4l5\_ .node ellipse,#chatgpt-mermaid-\_r_4l5\_ .node polygon,#chatgpt-mermaid-\_r_4l5\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_4l5\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_4l5\_ .node.mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_4l5\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_4l5\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4l5\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}Region A ApplicationRegion A DataRegion B ApplicationRegion B DataCross-Region Replication

This design can reduce write-routing latency, but it introduces additional complexity.

Imagine two regions update the same inventory item:

```
Initial inventory = 10

Region A reserves 7 units.
Region B reserves 6 units.

Each region initially sees inventory = 10.
```

If both accept the reservation independently, the system may allocate 13 units from an inventory of 10.

Replication alone does not prevent this. By the time each region receives the other's write, both reservations may already have been accepted.

Possible solutions include:

- Assigning a single writer for inventory ownership.
- Coordinating reservations through a strongly consistent system.
- Partitioning inventory ownership where the business rules permit it.
- Using a suitable escrow or allocation mechanism that safely divides available capacity.

The key is to choose a consistency strategy that preserves the business invariant.

### Advantages

- Multiple regions serve traffic concurrently.
- Can reduce latency for region-local workloads.
- Can improve utilization of regional infrastructure.
- May allow continued service after a regional failure, depending on data availability and ownership.

### Disadvantages

- Cross-region coordination can increase latency.
- Conflicts and concurrent writes need explicit handling.
- Failover may require ownership changes and fencing.
- Debugging and operational testing become more complicated.

### When to use it

Active-active is useful when the business needs multiple regions to serve production traffic concurrently.

Before choosing multi-writer data, ask whether the same entity genuinely needs to be writable in multiple regions. Many systems can obtain most of the benefits with active-active compute and single-writer data ownership.

## 3. Home-region architecture

A home-region architecture assigns each tenant, customer, or account a designated region that owns its primary data.

For example:

```
Tenant A → Mumbai
Tenant B → Singapore
Tenant C → Frankfurt
Tenant D → Mumbai
```

A tenant's home region is an ownership decision, not necessarily the region nearest to the user.

### Architecture

\#chatgpt-mermaid-\_r_4lo\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_4lo\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_4lo\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_4lo\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_4lo\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4lo\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_4lo\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_4lo\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_4lo\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_4lo\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_4lo\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_4lo\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4lo\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4lo\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_4lo\_ p{margin:0;}#chatgpt-mermaid-\_r_4lo\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4lo\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4lo\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4lo\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_4lo\_ .label text,#chatgpt-mermaid-\_r_4lo\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4lo\_ .node rect,#chatgpt-mermaid-\_r_4lo\_ .node circle,#chatgpt-mermaid-\_r_4lo\_ .node ellipse,#chatgpt-mermaid-\_r_4lo\_ .node polygon,#chatgpt-mermaid-\_r_4lo\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_4lo\_ .rough-node .label text,#chatgpt-mermaid-\_r_4lo\_ .node .label text,#chatgpt-mermaid-\_r_4lo\_ .image-shape .label,#chatgpt-mermaid-\_r_4lo\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_4lo\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_4lo\_ .rough-node .label,#chatgpt-mermaid-\_r_4lo\_ .node .label,#chatgpt-mermaid-\_r_4lo\_ .image-shape .label,#chatgpt-mermaid-\_r_4lo\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_4lo\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_4lo\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4lo\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4lo\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_4lo\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_4lo\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_4lo\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_4lo\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_4lo\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_4lo\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_4lo\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4lo\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4lo\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_4lo\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4lo\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_4lo\_ .icon-shape,#chatgpt-mermaid-\_r_4lo\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_4lo\_ .icon-shape p,#chatgpt-mermaid-\_r_4lo\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_4lo\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_4lo\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_4lo\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_4lo\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_4lo\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_4lo\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_4lo\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_4lo\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_4lo\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4lo\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_4lo\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_4lo\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_4lo\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4lo\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_4lo\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_4lo\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4lo\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_4lo\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_4lo\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4lo\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_4lo\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4lo\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_4lo\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_4lo\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_4lo\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_4lo\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_4lo\_ .node rect,#chatgpt-mermaid-\_r_4lo\_ .node circle,#chatgpt-mermaid-\_r_4lo\_ .node ellipse,#chatgpt-mermaid-\_r_4lo\_ .node polygon,#chatgpt-mermaid-\_r_4lo\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_4lo\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_4lo\_ .node.mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_4lo\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_4lo\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4lo\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}UserGlobal Entry PointRegional APIResolve Tenant Policy andHome RegionOwnership RouterRegion A: Tenant DataRegion B: Tenant DataRegion C: Tenant Data

Suppose Tenant A belongs to Mumbai.

A user from Singapore submits an update for Tenant A:

1. The request enters through Singapore.
2. The application identifies Tenant A.
3. The ownership service resolves Mumbai as its home region.
4. The update is routed to Mumbai.
5. Mumbai commits the update.

The request may be served by a Singapore API, but the authoritative write still happens in Mumbai.

### Where consistent hashing fits

A home-region architecture can use consistent hashing to place tenants across shards within their assigned region.

For example:

```
Tenant A
   |
   v
Home region = Mumbai
   |
   v
Mumbai shard ring
   |
   v
Shard M-17
```

If a shard is added to Mumbai, consistent hashing can reduce the number of tenant keys whose assigned shards change.

But it does not decide whether Tenant A belongs in Mumbai or Singapore. That decision comes from the tenant's ownership and residency policy.

### What if the user moves?

Suppose a user created their account in India and later moves to Germany.

Their current location does not automatically change the tenant's home region. A region change may require a deliberate migration, taking into account:

- Data-residency and contractual rules.
- Data transfer and replication.
- Cache invalidation.
- Background jobs and queued messages.
- File and object storage.
- Active sessions and routing metadata.
- Cutover and rollback procedures.

### Advantages

- Clear ownership of data.
- Easier reasoning about writes and transactions.
- Useful for tenant isolation and data residency.
- Consistent hashing can distribute tenants among regional shards.

### Disadvantages

- Users far from their tenant's home region may experience higher write latency.
- A home-region outage can make that tenant unavailable unless recovery is designed.
- Cross-region reads and service-to-service calls need careful handling.

### When to use it

Home-region architecture is particularly useful for multi-tenant SaaS platforms where tenant ownership, residency, and predictable data access are important.

## 4. Cell-based architecture

A cell-based architecture divides an application into relatively independent units called cells.

Each cell serves a subset of customers or workloads and contains the components needed to operate that subset.

A cell may include:

- API and worker instances.
- A database or set of database shards.
- Cache.
- Queues.
- Storage and supporting services.

Cells can be deployed in one region or distributed across regions.

### Example

Suppose you have 100,000 tenants.

Instead of placing all tenants on one massive logical platform, you assign tenants to multiple cells.

\#chatgpt-mermaid-\_r_4m6\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_4m6\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_4m6\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_4m6\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_4m6\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4m6\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_4m6\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_4m6\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_4m6\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_4m6\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_4m6\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_4m6\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4m6\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4m6\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_4m6\_ p{margin:0;}#chatgpt-mermaid-\_r_4m6\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4m6\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4m6\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4m6\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_4m6\_ .label text,#chatgpt-mermaid-\_r_4m6\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4m6\_ .node rect,#chatgpt-mermaid-\_r_4m6\_ .node circle,#chatgpt-mermaid-\_r_4m6\_ .node ellipse,#chatgpt-mermaid-\_r_4m6\_ .node polygon,#chatgpt-mermaid-\_r_4m6\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_4m6\_ .rough-node .label text,#chatgpt-mermaid-\_r_4m6\_ .node .label text,#chatgpt-mermaid-\_r_4m6\_ .image-shape .label,#chatgpt-mermaid-\_r_4m6\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_4m6\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_4m6\_ .rough-node .label,#chatgpt-mermaid-\_r_4m6\_ .node .label,#chatgpt-mermaid-\_r_4m6\_ .image-shape .label,#chatgpt-mermaid-\_r_4m6\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_4m6\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_4m6\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4m6\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4m6\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_4m6\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_4m6\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_4m6\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_4m6\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_4m6\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_4m6\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_4m6\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4m6\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4m6\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_4m6\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4m6\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_4m6\_ .icon-shape,#chatgpt-mermaid-\_r_4m6\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_4m6\_ .icon-shape p,#chatgpt-mermaid-\_r_4m6\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_4m6\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_4m6\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_4m6\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_4m6\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_4m6\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_4m6\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_4m6\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_4m6\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_4m6\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4m6\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_4m6\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_4m6\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_4m6\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4m6\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_4m6\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_4m6\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4m6\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_4m6\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_4m6\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4m6\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_4m6\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4m6\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_4m6\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_4m6\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_4m6\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_4m6\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_4m6\_ .node rect,#chatgpt-mermaid-\_r_4m6\_ .node circle,#chatgpt-mermaid-\_r_4m6\_ .node ellipse,#chatgpt-mermaid-\_r_4m6\_ .node polygon,#chatgpt-mermaid-\_r_4m6\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_4m6\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_4m6\_ .node.mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_4m6\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_4m6\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4m6\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}UsersGlobal Traffic ManagerCell Routing LayerCell 1Cell 2Cell 3Cell 1 DatabaseCell 2 DatabaseCell 3 Database

Each cell has its own capacity and failure boundary.

If Cell 2 experiences a problem, Cell 1 and Cell 3 may continue operating normally.

This limits the blast radius of some failures. It does not eliminate shared dependencies: if every cell depends on the same broken identity provider, control plane, or global routing service, that shared dependency can still affect the entire system.

### How cells relate to regions

Consider a larger deployment:

\#chatgpt-mermaid-\_r_4mf\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_4mf\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_4mf\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_4mf\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_4mf\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4mf\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_4mf\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_4mf\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_4mf\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_4mf\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_4mf\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_4mf\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4mf\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4mf\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_4mf\_ p{margin:0;}#chatgpt-mermaid-\_r_4mf\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4mf\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4mf\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4mf\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_4mf\_ .label text,#chatgpt-mermaid-\_r_4mf\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4mf\_ .node rect,#chatgpt-mermaid-\_r_4mf\_ .node circle,#chatgpt-mermaid-\_r_4mf\_ .node ellipse,#chatgpt-mermaid-\_r_4mf\_ .node polygon,#chatgpt-mermaid-\_r_4mf\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_4mf\_ .rough-node .label text,#chatgpt-mermaid-\_r_4mf\_ .node .label text,#chatgpt-mermaid-\_r_4mf\_ .image-shape .label,#chatgpt-mermaid-\_r_4mf\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_4mf\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_4mf\_ .rough-node .label,#chatgpt-mermaid-\_r_4mf\_ .node .label,#chatgpt-mermaid-\_r_4mf\_ .image-shape .label,#chatgpt-mermaid-\_r_4mf\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_4mf\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_4mf\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4mf\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4mf\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_4mf\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_4mf\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_4mf\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_4mf\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_4mf\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_4mf\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_4mf\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4mf\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4mf\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_4mf\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4mf\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_4mf\_ .icon-shape,#chatgpt-mermaid-\_r_4mf\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_4mf\_ .icon-shape p,#chatgpt-mermaid-\_r_4mf\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_4mf\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_4mf\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_4mf\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_4mf\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_4mf\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_4mf\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_4mf\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_4mf\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_4mf\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4mf\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_4mf\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_4mf\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_4mf\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4mf\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_4mf\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_4mf\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4mf\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_4mf\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_4mf\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4mf\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_4mf\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4mf\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_4mf\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_4mf\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_4mf\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_4mf\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_4mf\_ .node rect,#chatgpt-mermaid-\_r_4mf\_ .node circle,#chatgpt-mermaid-\_r_4mf\_ .node ellipse,#chatgpt-mermaid-\_r_4mf\_ .node polygon,#chatgpt-mermaid-\_r_4mf\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_4mf\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_4mf\_ .node.mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_4mf\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_4mf\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4mf\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}Global RoutingRegion ARegion BCell A1Cell A2Cell B1Cell B2

You can place several cells in each region.

You can also have recovery capacity in another region for a cell's data and workloads. However, a cell does not automatically become highly available across regions. You still need a replication, recovery, and ownership strategy.

### Where consistent hashing fits

Consistent hashing can help assign tenant keys to cells or shards.

For example:

```
tenant-101 → Cell A1
tenant-202 → Cell B2
tenant-303 → Cell A2
```

You might use consistent hashing to distribute tenants across eligible cells based on capacity. Alternatively, you might use an explicit placement directory when tenant migration, residency, or business constraints require more control.

If a cell is removed, the hash ring can calculate new placements, but the system must still migrate or recover the data before safely switching ownership.

### Advantages

- Limits the impact of some failures.
- Supports incremental scaling.
- Helps isolate large or noisy tenants.
- Allows independent deployment and operational boundaries.

### Disadvantages

- More infrastructure and deployment complexity.
- Requires a cell-placement and tenant-routing strategy.
- Moving tenants between cells requires migration procedures.
- Shared services can still create system-wide failure modes.

### When to use it

Cell-based architecture is useful for large SaaS platforms, high-scale services, and systems where limiting blast radius is a major goal.

## 5. Compare the four architectures

| Property                   | Active-passive                             | Active-active compute, single writer           | Home-region                           | Cell-based                              |
| -------------------------- | ------------------------------------------ | ---------------------------------------------- | ------------------------------------- | --------------------------------------- |
| Main purpose               | Disaster recovery                          | Concurrent regional serving                    | Explicit regional ownership           | Fault and workload isolation            |
| Production traffic         | Primarily one region                       | Multiple regions                               | Multiple regions may serve requests   | Multiple cells, possibly across regions |
| Write ownership            | Usually one primary                        | One owner per partition                        | Assigned home region                  | Depends on the cell's data model        |
| Cross-region write latency | Usually not needed during normal operation | Possible when requests reach non-owner regions | Possible for non-home-region requests | Depends on placement                    |
| Failure handling           | Promote standby                            | Reroute and possibly transfer ownership        | Recover or transfer tenant ownership  | Isolate failed cell and recover it      |
| Consistency complexity     | Lower with a single writer                 | Moderate to high                               | Often moderate                        | Depends on the data model               |
| Operational complexity     | Lower                                      | Higher                                         | Moderate                              | Higher                                  |

These are architectural patterns, not mutually exclusive choices. A production platform might combine them:

- Home-region ownership for tenants.
- Multiple cells within each region.
- Active-active API deployments.
- Single-writer data partitions.
- Active-passive database recovery for individual cells.

That combination can be easier to reason about than a globally writable database shared by every region.

## 6. How to choose an architecture: a practical example

Suppose you are designing a SaaS platform that stores customer documents, generates reports, and supports subscription billing.

You have these requirements:

- Users worldwide should be able to access the API.
- Customer data has residency restrictions.
- Report generation can be asynchronous.
- Billing must not process the same payment twice.
- A regional outage must be recoverable.

A reasonable starting design could be:

1. Regional API deployments to serve users close to their locations.
2. Home-region tenant ownership to enforce data placement.
3. Consistent hashing within each region to distribute tenants or partitions across shards.
4. Durable queues and idempotent workers for report generation.
5. A single authoritative billing-write path with appropriate transaction and idempotency guarantees.
6. Cross-region backups and replication, with recovery procedures appropriate to the database.
7. Explicit failover and fencing to prevent two regions from simultaneously acting as the authoritative writer for the same partition.

As the system grows, you might introduce cells to reduce the blast radius of failures or isolate particularly large tenants.

Notice that consistent hashing is only one component of the design. It helps distribute ownership; it does not supply the rest of the guarantees.

## 7. Interview questions

Q1. Is active-active always more available than active-passive?

Not necessarily. Active-active can keep serving traffic during some regional failures, but the system may still depend on unavailable data, shared services, or ownership coordination. Active-passive can provide strong recovery guarantees if promotion and data recovery are designed correctly. Availability depends on the full system, not just the number of active regions.

Q2. Why use home-region ownership instead of routing every user to the nearest region?

The nearest region is not necessarily the correct owner of the user's data. Home-region ownership supports predictable writes, data-residency constraints, and simpler consistency rules. A nearby region can still serve eligible reads or forward requests to the authoritative region.

Q3. Does consistent hashing help with cell-based architecture?

Yes. It can distribute tenants among eligible cells or shards while reducing remapping when membership changes. But the system still needs to migrate data, update routing metadata, and coordinate ownership changes safely.

Q4. Can I combine home-region and active-active architecture?

Yes. Multiple regions can serve application requests concurrently while each tenant has one authoritative home region. This provides regional entry points without requiring every tenant's data to accept writes everywhere.

Q5. Which design would you choose first for a multi-tenant SaaS application?

I would begin with highly available regional infrastructure, explicit tenant ownership, and a clear single-writer data model. I would add a second region with tested recovery procedures, then introduce active-active serving or cells when latency, availability, scale, or isolation requirements justify the added complexity.

### Key takeaway

Think of multi-region design as a set of related but distinct choices:

- Active-passive answers how to recover from a regional failure.
- Active-active answers how multiple regions serve production traffic.
- Home-region ownership answers which region is authoritative for a tenant.
- Cell-based architecture answers how to isolate workloads and limit blast radius.
- Consistent hashing helps distribute keys across eligible nodes or partitions.

Next lesson: We'll examine the global traffic-routing layer in detail: DNS-based routing, latency-based routing, health checks, global load balancing, sticky sessions, and what happens when users move between regions. We'll also distinguish routing a user to an API region from routing their request to the data owner. Type `next` to continue.



















# Chapter 3: Evolving from a Single Region to a Multi-Region Architecture

## Lesson 3: Global Traffic Routing — DNS, Latency, Health Checks, and Data Ownership

Imagine your application runs in Mumbai, Singapore, and Frankfurt. A user in India sends a request, and a user in Germany sends another.

Which region should receive each request?

At first, the answer sounds simple: send each user to the nearest region.

But a production system must consider more than geographic distance. The nearest region might be unhealthy, the user's data might belong to another region, or a region might be prohibited from processing certain data.

The key distinction is that routing a request to an application region is not the same as routing it to the region that owns the requested data.

Let's build the routing architecture step by step.

## 1. Understand the three routing decisions

Consider a customer named `tenant-101`, whose authoritative data is stored in Mumbai.

The customer travels to Singapore and opens your application.

There are three separate decisions:

1. Entry-point routing: Where should the user's connection enter the application?
2. Application routing: Which API instance or service should process the request?
3. Data routing: Which region and database shard own the requested data?

\#chatgpt-mermaid-\_r_4nt\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_4nt\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_4nt\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_4nt\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_4nt\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4nt\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_4nt\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_4nt\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_4nt\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_4nt\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_4nt\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_4nt\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4nt\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4nt\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_4nt\_ p{margin:0;}#chatgpt-mermaid-\_r_4nt\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4nt\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4nt\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4nt\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_4nt\_ .label text,#chatgpt-mermaid-\_r_4nt\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4nt\_ .node rect,#chatgpt-mermaid-\_r_4nt\_ .node circle,#chatgpt-mermaid-\_r_4nt\_ .node ellipse,#chatgpt-mermaid-\_r_4nt\_ .node polygon,#chatgpt-mermaid-\_r_4nt\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_4nt\_ .rough-node .label text,#chatgpt-mermaid-\_r_4nt\_ .node .label text,#chatgpt-mermaid-\_r_4nt\_ .image-shape .label,#chatgpt-mermaid-\_r_4nt\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_4nt\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_4nt\_ .rough-node .label,#chatgpt-mermaid-\_r_4nt\_ .node .label,#chatgpt-mermaid-\_r_4nt\_ .image-shape .label,#chatgpt-mermaid-\_r_4nt\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_4nt\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_4nt\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4nt\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4nt\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_4nt\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_4nt\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_4nt\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_4nt\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_4nt\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_4nt\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_4nt\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4nt\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4nt\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_4nt\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4nt\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_4nt\_ .icon-shape,#chatgpt-mermaid-\_r_4nt\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_4nt\_ .icon-shape p,#chatgpt-mermaid-\_r_4nt\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_4nt\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_4nt\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_4nt\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_4nt\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_4nt\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_4nt\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_4nt\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_4nt\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_4nt\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4nt\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_4nt\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_4nt\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_4nt\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4nt\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_4nt\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_4nt\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4nt\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_4nt\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_4nt\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4nt\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_4nt\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4nt\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_4nt\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_4nt\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_4nt\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_4nt\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_4nt\_ .node rect,#chatgpt-mermaid-\_r_4nt\_ .node circle,#chatgpt-mermaid-\_r_4nt\_ .node ellipse,#chatgpt-mermaid-\_r_4nt\_ .node polygon,#chatgpt-mermaid-\_r_4nt\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_4nt\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_4nt\_ .node.mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_4nt\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_4nt\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4nt\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}User in SingaporeGlobal Traffic RouterSingapore APIIdentify TenantResolve Residency andOwnershipMumbai Data OwnerAuthoritative Database

The user enters through Singapore, but the request is routed to Mumbai for authoritative data access.

This architecture can still be useful because the Singapore API may handle TLS termination, authentication, request validation, and other operations locally. However, whether that improves latency depends on how much synchronous work still crosses regions.

For a read-only request, the system might instead use a suitable local replica if the consistency requirements allow it.

## 2. DNS-based routing

DNS is often the first routing layer users encounter.

When a client requests `api.example.com`, DNS resolves the hostname to an IP address or another hostname that leads to the application.

A basic flow looks like this:

\#chatgpt-mermaid-\_r_4o6\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_4o6\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_4o6\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_4o6\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_4o6\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4o6\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_4o6\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_4o6\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_4o6\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_4o6\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_4o6\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_4o6\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4o6\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4o6\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_4o6\_ p{margin:0;}#chatgpt-mermaid-\_r_4o6\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4o6\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4o6\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4o6\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_4o6\_ .label text,#chatgpt-mermaid-\_r_4o6\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4o6\_ .node rect,#chatgpt-mermaid-\_r_4o6\_ .node circle,#chatgpt-mermaid-\_r_4o6\_ .node ellipse,#chatgpt-mermaid-\_r_4o6\_ .node polygon,#chatgpt-mermaid-\_r_4o6\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_4o6\_ .rough-node .label text,#chatgpt-mermaid-\_r_4o6\_ .node .label text,#chatgpt-mermaid-\_r_4o6\_ .image-shape .label,#chatgpt-mermaid-\_r_4o6\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_4o6\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_4o6\_ .rough-node .label,#chatgpt-mermaid-\_r_4o6\_ .node .label,#chatgpt-mermaid-\_r_4o6\_ .image-shape .label,#chatgpt-mermaid-\_r_4o6\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_4o6\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_4o6\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4o6\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4o6\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_4o6\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_4o6\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_4o6\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_4o6\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_4o6\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_4o6\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_4o6\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4o6\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4o6\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_4o6\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4o6\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_4o6\_ .icon-shape,#chatgpt-mermaid-\_r_4o6\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_4o6\_ .icon-shape p,#chatgpt-mermaid-\_r_4o6\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_4o6\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_4o6\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_4o6\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_4o6\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_4o6\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_4o6\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_4o6\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_4o6\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_4o6\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4o6\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_4o6\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_4o6\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_4o6\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4o6\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_4o6\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_4o6\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4o6\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_4o6\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_4o6\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4o6\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_4o6\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4o6\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_4o6\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_4o6\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_4o6\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_4o6\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_4o6\_ .node rect,#chatgpt-mermaid-\_r_4o6\_ .node circle,#chatgpt-mermaid-\_r_4o6\_ .node ellipse,#chatgpt-mermaid-\_r_4o6\_ .node polygon,#chatgpt-mermaid-\_r_4o6\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_4o6\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_4o6\_ .node.mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_4o6\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_4o6\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4o6\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}ClientDNS ResolverResolved EndpointRegional Load BalancerApplication

In a multi-region deployment, a DNS routing service can return different endpoints based on configured policies.

For example:

```
api.example.com
    |
    +-- India traffic     → Mumbai endpoint
    +-- Southeast Asia    → Singapore endpoint
    +-- European traffic  → Frankfurt endpoint
```

This is a simplified illustration; the actual decision depends on the DNS provider, routing policy, resolver behavior, and health configuration.

### A. Geolocation routing

Geolocation routing uses the apparent geographic origin of the DNS query or its associated resolver information to choose an endpoint.

Advantages

- Can direct traffic toward a suitable geographic region.
- Can support region-specific entry points.
- Can help align traffic with broad residency or service-placement policies when combined with appropriate enforcement.

Limitations

- The DNS resolver's location may differ from the user's location.
- A user may travel without immediately receiving a new DNS answer.
- Geographic proximity does not guarantee the lowest network latency.
- Geolocation routing does not enforce data ownership by itself.

Do not treat geolocation-based DNS as a substitute for authorization or data-residency enforcement inside the application.

### B. Latency-based routing

Latency-based routing chooses an endpoint based on measured or estimated network performance between clients or resolvers and available endpoints, depending on the routing provider.

For example:

```
Client query
    |
    +-- Mumbai endpoint:     estimated 35 ms
    +-- Singapore endpoint:  estimated 60 ms
    +-- Frankfurt endpoint:  estimated 180 ms
    |
    v
Choose a suitable low-latency endpoint
```

These numbers are illustrative, not guaranteed measurements.

Latency-based routing can be useful when the goal is to improve the user experience. But it does not necessarily select the correct database region, and it may not reflect the latency of the full application request.

For instance, an API request might enter Mumbai in 35 ms but then access a database in Frankfurt, adding another network round trip.

Optimize the full request path, not just the first network hop.

### C. Failover routing with health checks

Suppose Mumbai becomes unavailable.

A global routing service can stop directing new traffic to the Mumbai endpoint and direct traffic to Singapore or another eligible region.

\#chatgpt-mermaid-\_r_4op\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_4op\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_4op\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_4op\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_4op\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4op\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_4op\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_4op\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_4op\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_4op\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_4op\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_4op\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4op\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4op\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_4op\_ p{margin:0;}#chatgpt-mermaid-\_r_4op\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4op\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4op\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4op\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_4op\_ .label text,#chatgpt-mermaid-\_r_4op\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4op\_ .node rect,#chatgpt-mermaid-\_r_4op\_ .node circle,#chatgpt-mermaid-\_r_4op\_ .node ellipse,#chatgpt-mermaid-\_r_4op\_ .node polygon,#chatgpt-mermaid-\_r_4op\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_4op\_ .rough-node .label text,#chatgpt-mermaid-\_r_4op\_ .node .label text,#chatgpt-mermaid-\_r_4op\_ .image-shape .label,#chatgpt-mermaid-\_r_4op\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_4op\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_4op\_ .rough-node .label,#chatgpt-mermaid-\_r_4op\_ .node .label,#chatgpt-mermaid-\_r_4op\_ .image-shape .label,#chatgpt-mermaid-\_r_4op\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_4op\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_4op\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4op\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4op\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_4op\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_4op\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_4op\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_4op\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_4op\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_4op\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_4op\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4op\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4op\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_4op\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4op\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_4op\_ .icon-shape,#chatgpt-mermaid-\_r_4op\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_4op\_ .icon-shape p,#chatgpt-mermaid-\_r_4op\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_4op\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_4op\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_4op\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_4op\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_4op\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_4op\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_4op\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_4op\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_4op\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4op\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_4op\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_4op\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_4op\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4op\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_4op\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_4op\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4op\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_4op\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_4op\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4op\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_4op\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4op\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_4op\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_4op\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_4op\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_4op\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_4op\_ .node rect,#chatgpt-mermaid-\_r_4op\_ .node circle,#chatgpt-mermaid-\_r_4op\_ .node ellipse,#chatgpt-mermaid-\_r_4op\_ .node polygon,#chatgpt-mermaid-\_r_4op\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_4op\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_4op\_ .node.mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_4op\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_4op\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4op\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}UsersGlobal Traffic RouterMumbai healthy?Mumbai EndpointSingapore EndpointYesNo

The important detail is that a health check must reflect the kind of failure you intend to detect.

A process-level check might confirm that an API server responds to HTTP requests. But that does not prove the application can successfully access its database or perform critical operations.

Common health-check levels include:

- Liveness: Is the process alive and functioning?
- Readiness: Is the instance ready to receive traffic?
- Dependency health: Are critical dependencies available?
- Regional health: Can the region serve the required workload?
- Data-plane readiness: Is the region able to access the correct authoritative data?

Be careful about making readiness depend on every downstream service. If a nonessential dependency fails, marking every API instance unhealthy could unnecessarily remove the whole region from service.

Likewise, a region might be healthy for reading cached data but unable to accept authoritative writes.

A useful design therefore distinguishes between serving traffic, serving reads, and accepting writes when those capabilities can fail independently.

### Why health checks can cause problems

Imagine that the database in Mumbai is temporarily slow.

Every API health check starts failing. The global router sends traffic to Singapore. Singapore then overloads because it suddenly receives all requests.

If the health check recovers briefly, traffic switches back to Mumbai. The system repeats the cycle.

This is called flapping.

Ways to reduce this risk include:

- Use failure thresholds and consecutive-success thresholds.
- Add recovery delays or hysteresis.
- Apply capacity-aware traffic shifting.
- Avoid reacting to transient failures too aggressively.
- Test failover and failback separately.
- Make the health check reflect whether the endpoint can safely serve its intended workload.

DNS failover is not instantaneous in every client. Cached answers, resolver behavior, connection reuse, and application retry policies can delay the effect of a DNS change.

## 3. DNS versus global application load balancing

DNS is not the only way to distribute traffic.

A global traffic-management layer may direct clients to a regional endpoint using DNS, anycast networking, a global proxy, or a combination of mechanisms.

The exact behavior depends on the service and architecture.

| Capability                 | DNS-based routing                             | Global proxy/load balancing                                             |
| -------------------------- | --------------------------------------------- | ----------------------------------------------------------------------- |
| How it directs traffic     | Returns DNS answers                           | Proxies or directs connections/requests                                 |
| Health-based routing       | Possible, subject to DNS behavior and caching | Possible, subject to product and configuration                          |
| Changes visible to clients | Can be delayed by DNS caching                 | Can often change routing without changing the client's resolved address |
| Connection-level control   | Limited after DNS resolution                  | Potentially more direct control                                         |
| Application awareness      | Usually limited                               | Can be deeper, depending on the product                                 |
| Operational complexity     | Often simpler                                 | Can require more infrastructure and careful design                      |

A global proxy may be able to direct each new request based on current routing information. But established connections may still be tied to the endpoint they originally reached.

Neither approach automatically transfers application state or database ownership.

### Example: AWS architecture

A common AWS design might use:

- Amazon Route 53 for DNS and health-based routing.
- Regional Application Load Balancers for regional ingress.
- Amazon ECS or another compute service for application instances.
- Regional databases and caches.
- A controlled mechanism for ownership and failover.

Another design might use AWS Global Accelerator for a stable anycast entry point and routing toward healthy regional endpoints.

These services help with traffic management. They do not independently guarantee that a database replica contains every committed transaction or that two regions cannot both accept conflicting writes.

## 4. Sticky sessions and users moving between regions

Some applications use sticky sessions, also called session affinity.

This means that requests from a client are preferentially sent to the same application instance or endpoint.

Why might a system do this?

Consider a legacy application that stores session state in local process memory:

```
Request 1 → Instance A → Session stored in A's memory
Request 2 → Instance A → Session still available
Request 3 → Instance B → Session missing
```

If the session is not shared or otherwise recoverable, the user might be logged out or lose workflow state when the request reaches another instance.

Sticky sessions can reduce that problem, but they are not a complete solution to multi-region routing.

### Better approach: externalize session state

Store important session state in a suitable shared or durable store, or use an appropriate stateless authentication design.

For example:

\#chatgpt-mermaid-\_r_4p7\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_4p7\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_4p7\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_4p7\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_4p7\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4p7\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_4p7\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_4p7\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_4p7\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_4p7\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_4p7\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_4p7\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4p7\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4p7\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_4p7\_ p{margin:0;}#chatgpt-mermaid-\_r_4p7\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4p7\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4p7\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4p7\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_4p7\_ .label text,#chatgpt-mermaid-\_r_4p7\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4p7\_ .node rect,#chatgpt-mermaid-\_r_4p7\_ .node circle,#chatgpt-mermaid-\_r_4p7\_ .node ellipse,#chatgpt-mermaid-\_r_4p7\_ .node polygon,#chatgpt-mermaid-\_r_4p7\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_4p7\_ .rough-node .label text,#chatgpt-mermaid-\_r_4p7\_ .node .label text,#chatgpt-mermaid-\_r_4p7\_ .image-shape .label,#chatgpt-mermaid-\_r_4p7\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_4p7\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_4p7\_ .rough-node .label,#chatgpt-mermaid-\_r_4p7\_ .node .label,#chatgpt-mermaid-\_r_4p7\_ .image-shape .label,#chatgpt-mermaid-\_r_4p7\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_4p7\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_4p7\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4p7\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4p7\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_4p7\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_4p7\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_4p7\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_4p7\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_4p7\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_4p7\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_4p7\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4p7\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4p7\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_4p7\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4p7\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_4p7\_ .icon-shape,#chatgpt-mermaid-\_r_4p7\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_4p7\_ .icon-shape p,#chatgpt-mermaid-\_r_4p7\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_4p7\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_4p7\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_4p7\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_4p7\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_4p7\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_4p7\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_4p7\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_4p7\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_4p7\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4p7\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_4p7\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_4p7\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_4p7\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4p7\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_4p7\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_4p7\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4p7\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_4p7\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_4p7\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4p7\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_4p7\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4p7\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_4p7\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_4p7\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_4p7\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_4p7\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_4p7\_ .node rect,#chatgpt-mermaid-\_r_4p7\_ .node circle,#chatgpt-mermaid-\_r_4p7\_ .node ellipse,#chatgpt-mermaid-\_r_4p7\_ .node polygon,#chatgpt-mermaid-\_r_4p7\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_4p7\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_4p7\_ .node.mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_4p7\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_4p7\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4p7\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}UserGlobal RouterAPI Instance AAPI Instance BShared Session Store

In a multi-region architecture, the phrase shared session store needs careful interpretation. A single regional Redis instance is not automatically globally shared, highly available, or consistent across regions. You must choose whether session state is replicated, stored centrally, or managed through region-aware session ownership.

### What happens when the user moves?

Suppose a user opens the application in Mumbai, then travels to Singapore.

The routing system may start directing new connections to Singapore, but several things can remain unchanged:

- The user's authentication token.
- Their tenant's home region.
- The database partition that owns their data.
- The location of an existing session.
- The state of an in-flight request.

These must be handled by application and data architecture, not assumed to change automatically with DNS.

User location is a routing signal, not a source of truth for data ownership.

## 5. The hard problem: regional routing versus data ownership

Let's walk through a realistic failure.

Your application has two regions:

- Mumbai owns Tenant A's data.
- Singapore owns Tenant B's data.
- Both regions accept API traffic.

Under normal operation:

```
Tenant A → Mumbai API → Mumbai database
Tenant B → Singapore API → Singapore database
```

Now Mumbai becomes unavailable.

The global router sends users to Singapore.

What happens when a user requests Tenant A's data?

A naive implementation might do this:

```
// Dangerous if this simply changes the destination
// without checking data ownership or replication state.
Database database = singaporeDatabase;
return database.findTenant(tenantId);
```

This is unsafe because Singapore may not contain Tenant A's latest data, or may not be authorized to become its writer.

Instead, the application needs an ownership-aware routing decision.

Conceptually:

```
TenantOwnership ownership = ownershipService.getOwnership(tenantId);

if (!ownership.isAvailable()) {
    throw new ServiceUnavailableException(
        "Tenant data is temporarily unavailable"
    );
}

if (ownership.isWritableInCurrentRegion()) {
    return localRepository.update(tenantId, request);
}

return remoteOwnerClient.update(
    ownership.authoritativeRegion(),
    tenantId,
    request
);
```

This is illustrative Java, not a complete production implementation. A real system must handle stale ownership metadata, timeouts, authorization, retries, and changes in ownership between lookup and write.

The central idea is that the routing layer must respect the authoritative owner.

### What if Singapore must take over?

A safe failover workflow typically involves:

1. Detecting that Mumbai cannot serve the required workload.
2. Determining the recoverable database state and replication lag.
3. Establishing that the old writer cannot continue accepting writes.
4. Promoting or activating the recovery data store.
5. Updating authoritative ownership information.
6. Directing requests to the new owner.
7. Monitoring correctness, backlog, and latency.

The old region must not continue accepting conflicting writes after Singapore becomes authoritative.

A fencing mechanism can help enforce this. For example, a monotonically increasing ownership epoch can identify the current owner. Components that support fencing reject writes from an older epoch.

Simply updating a routing table is insufficient if an isolated old writer can continue processing requests.

## 6. How consistent hashing fits into the routing layer

So far, we have discussed global routing and data ownership. Where does consistent hashing fit?

Suppose Tenant A is assigned to Mumbai and Mumbai has four database shards.

\#chatgpt-mermaid-\_r_4q1\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_4q1\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_4q1\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_4q1\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_4q1\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4q1\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_4q1\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_4q1\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_4q1\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_4q1\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_4q1\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_4q1\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4q1\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4q1\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_4q1\_ p{margin:0;}#chatgpt-mermaid-\_r_4q1\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4q1\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4q1\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4q1\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_4q1\_ .label text,#chatgpt-mermaid-\_r_4q1\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4q1\_ .node rect,#chatgpt-mermaid-\_r_4q1\_ .node circle,#chatgpt-mermaid-\_r_4q1\_ .node ellipse,#chatgpt-mermaid-\_r_4q1\_ .node polygon,#chatgpt-mermaid-\_r_4q1\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_4q1\_ .rough-node .label text,#chatgpt-mermaid-\_r_4q1\_ .node .label text,#chatgpt-mermaid-\_r_4q1\_ .image-shape .label,#chatgpt-mermaid-\_r_4q1\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_4q1\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_4q1\_ .rough-node .label,#chatgpt-mermaid-\_r_4q1\_ .node .label,#chatgpt-mermaid-\_r_4q1\_ .image-shape .label,#chatgpt-mermaid-\_r_4q1\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_4q1\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_4q1\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4q1\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4q1\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_4q1\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_4q1\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_4q1\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_4q1\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_4q1\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_4q1\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_4q1\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4q1\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4q1\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_4q1\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4q1\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_4q1\_ .icon-shape,#chatgpt-mermaid-\_r_4q1\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_4q1\_ .icon-shape p,#chatgpt-mermaid-\_r_4q1\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_4q1\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_4q1\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_4q1\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_4q1\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_4q1\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_4q1\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_4q1\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_4q1\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_4q1\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4q1\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_4q1\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_4q1\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_4q1\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4q1\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_4q1\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_4q1\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4q1\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_4q1\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_4q1\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4q1\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_4q1\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4q1\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_4q1\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_4q1\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_4q1\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_4q1\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_4q1\_ .node rect,#chatgpt-mermaid-\_r_4q1\_ .node circle,#chatgpt-mermaid-\_r_4q1\_ .node ellipse,#chatgpt-mermaid-\_r_4q1\_ .node polygon,#chatgpt-mermaid-\_r_4q1\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_4q1\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_4q1\_ .node.mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_4q1\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_4q1\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4q1\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}Incoming RequestResidency and OwnershipPolicyMumbai RegionConsistent Hash RingShard 1Shard 2Shard 3Shard 4

Consistent hashing selects the shard associated with the tenant or partition key.

If you add Shard 5, the ring calculates which keys should change ownership. The application must then move the corresponding data and safely switch routing.

Now imagine Mumbai becomes unavailable. You should not simply add Singapore's shards to the same global ring and assume the problem is solved.

Instead, the system must first determine whether Singapore is an eligible and safe owner. Only then should its eligible shards participate in the placement decision.

There are two common approaches:

Approach A: Separate rings per region

```
Mumbai ring:    M1, M2, M3, M4
Singapore ring: S1, S2, S3, S4
Frankfurt ring: F1, F2, F3, F4
```

A policy or ownership layer selects the region, then that region's ring selects the shard.

Approach B: Explicit partition ownership

A control plane maintains the authoritative owner for each partition. The selected owner then uses its local partition or shard map.

This is useful when tenant placement must be controlled precisely or when failover and migration require explicit state transitions.

In either case, ownership changes must be coordinated with the data layer.

## 7. Common production failures to plan for

| Failure                                     | Why it happens                                      | Design response                                                                |
| ------------------------------------------- | --------------------------------------------------- | ------------------------------------------------------------------------------ |
| DNS points to an unhealthy region           | Health checks or DNS caching delay the change       | Use appropriate health checks, bounded caching, retries, and tested failover   |
| Traffic shifts too quickly                  | One region fails and the remaining region overloads | Maintain recovery capacity and apply controlled traffic shifting               |
| User sees missing data after failover       | Replica is behind or lacks required data            | Define RPO and recovery procedures; validate recoverable state                 |
| Requests reach the wrong tenant owner       | Routing metadata is stale                           | Version ownership metadata and validate writes against authoritative ownership |
| Two regions accept conflicting writes       | Old owner continues after failover                  | Use fencing and a controlled ownership transition                              |
| Users are logged out after changing regions | Session state exists only in one instance or region | Externalize session state or use a suitable stateless/session-recovery design  |
| Requests become slower despite nearby APIs  | Data access still crosses regions                   | Measure end-to-end latency and design locality around the data access pattern  |

## 8. Interview-ready summary

If asked, "How would you route users in a multi-region system?", a strong answer is:

> I would separate global traffic routing from data ownership. A global routing layer, using DNS-based routing or a global load balancer, directs users to a healthy regional entry point based on latency, geography, health, and policy. The application then identifies the tenant or partition and resolves its authoritative owner. Consistent hashing can select a shard within the eligible region, while replication and failover are handled separately. During a regional outage, I would verify data recoverability, fence the old writer, establish the new owner, and then redirect traffic. I would also account for DNS caching, session state, regional capacity, and data-residency restrictions.

The key distinction to remember:

Traffic routing answers, "Where should this request enter?" Data ownership answers, "Where can this request safely read or write the data?"

They are related, but they are not the same decision.

### Next lesson

We'll move to Chapter 3, Lesson 4: Multi-region data replication and database topology.

We'll cover primary databases, read replicas, cross-region replication, synchronous versus asynchronous replication, replication lag, and why a healthy regional endpoint does not necessarily mean its data is safe to serve or promote. Type `next` to continue.





















# Chapter 3: Evolving from a Single Region to a Multi-Region Architecture

## Lesson 4: Database Topology and Replication Across Regions

This is one of the most important topics in multi-region system design.

You can deploy your application in Mumbai, Singapore, and Frankfurt. You can configure global load balancing and use consistent hashing to distribute tenants across database shards.

But if your database architecture is not designed correctly, you can still lose committed data, serve stale reads, or accept conflicting writes during a regional failure.

The central principle: replication copies data; it does not automatically guarantee consistency, safe failover, or correct ownership.

Let's build the concepts from the ground up.

## 1. Start with a primary database and read replicas

Consider a SaaS application running in Mumbai.

You have one primary PostgreSQL database and two read replicas.

\#chatgpt-mermaid-\_r_4rf\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_4rf\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_4rf\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_4rf\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_4rf\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4rf\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_4rf\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_4rf\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_4rf\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_4rf\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_4rf\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_4rf\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4rf\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4rf\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_4rf\_ p{margin:0;}#chatgpt-mermaid-\_r_4rf\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4rf\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4rf\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4rf\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_4rf\_ .label text,#chatgpt-mermaid-\_r_4rf\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4rf\_ .node rect,#chatgpt-mermaid-\_r_4rf\_ .node circle,#chatgpt-mermaid-\_r_4rf\_ .node ellipse,#chatgpt-mermaid-\_r_4rf\_ .node polygon,#chatgpt-mermaid-\_r_4rf\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_4rf\_ .rough-node .label text,#chatgpt-mermaid-\_r_4rf\_ .node .label text,#chatgpt-mermaid-\_r_4rf\_ .image-shape .label,#chatgpt-mermaid-\_r_4rf\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_4rf\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_4rf\_ .rough-node .label,#chatgpt-mermaid-\_r_4rf\_ .node .label,#chatgpt-mermaid-\_r_4rf\_ .image-shape .label,#chatgpt-mermaid-\_r_4rf\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_4rf\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_4rf\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4rf\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4rf\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_4rf\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_4rf\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_4rf\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_4rf\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_4rf\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_4rf\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_4rf\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4rf\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4rf\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_4rf\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4rf\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_4rf\_ .icon-shape,#chatgpt-mermaid-\_r_4rf\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_4rf\_ .icon-shape p,#chatgpt-mermaid-\_r_4rf\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_4rf\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_4rf\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_4rf\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_4rf\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_4rf\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_4rf\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_4rf\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_4rf\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_4rf\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4rf\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_4rf\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_4rf\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_4rf\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4rf\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_4rf\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_4rf\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4rf\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_4rf\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_4rf\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4rf\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_4rf\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4rf\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_4rf\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_4rf\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_4rf\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_4rf\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_4rf\_ .node rect,#chatgpt-mermaid-\_r_4rf\_ .node circle,#chatgpt-mermaid-\_r_4rf\_ .node ellipse,#chatgpt-mermaid-\_r_4rf\_ .node polygon,#chatgpt-mermaid-\_r_4rf\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_4rf\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_4rf\_ .node.mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_4rf\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_4rf\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4rf\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}ApplicationPrimary DatabaseRead Replica 1Read Replica 2

The primary accepts writes. The read replicas receive changes from the primary and serve eligible reads.

For example:

- `POST /payments` writes to the primary.
- `GET /dashboard` might read from a replica.
- `GET /reports/123` might read from a replica if some staleness is acceptable.

The exact behavior depends on the database, replication mode, and application configuration.

### Why use read replicas?

Suppose your workload is:

- 20,000 reads per second.
- 1,000 writes per second.

The primary handles all writes and potentially all reads. If you move suitable read traffic to replicas, you can distribute read load.

However, replicas are not free additional capacity for every workload. They introduce replication overhead, consume resources, and may lag behind the primary.

Also, adding replicas does not automatically distribute writes. In a primary-replica design, the primary remains the write authority.

## 2. Local replication versus cross-region replication

Now deploy the database architecture across two AWS regions.

Region A is Mumbai, and Region B is Singapore.

A common topology is:

\#chatgpt-mermaid-\_r_4ro\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_4ro\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_4ro\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_4ro\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_4ro\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4ro\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_4ro\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_4ro\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_4ro\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_4ro\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_4ro\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_4ro\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4ro\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4ro\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_4ro\_ p{margin:0;}#chatgpt-mermaid-\_r_4ro\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4ro\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4ro\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4ro\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_4ro\_ .label text,#chatgpt-mermaid-\_r_4ro\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4ro\_ .node rect,#chatgpt-mermaid-\_r_4ro\_ .node circle,#chatgpt-mermaid-\_r_4ro\_ .node ellipse,#chatgpt-mermaid-\_r_4ro\_ .node polygon,#chatgpt-mermaid-\_r_4ro\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_4ro\_ .rough-node .label text,#chatgpt-mermaid-\_r_4ro\_ .node .label text,#chatgpt-mermaid-\_r_4ro\_ .image-shape .label,#chatgpt-mermaid-\_r_4ro\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_4ro\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_4ro\_ .rough-node .label,#chatgpt-mermaid-\_r_4ro\_ .node .label,#chatgpt-mermaid-\_r_4ro\_ .image-shape .label,#chatgpt-mermaid-\_r_4ro\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_4ro\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_4ro\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4ro\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4ro\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_4ro\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_4ro\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_4ro\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_4ro\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_4ro\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_4ro\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_4ro\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4ro\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4ro\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_4ro\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4ro\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_4ro\_ .icon-shape,#chatgpt-mermaid-\_r_4ro\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_4ro\_ .icon-shape p,#chatgpt-mermaid-\_r_4ro\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_4ro\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_4ro\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_4ro\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_4ro\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_4ro\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_4ro\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_4ro\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_4ro\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_4ro\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4ro\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_4ro\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_4ro\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_4ro\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4ro\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_4ro\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_4ro\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4ro\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_4ro\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_4ro\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4ro\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_4ro\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4ro\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_4ro\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_4ro\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_4ro\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_4ro\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_4ro\_ .node rect,#chatgpt-mermaid-\_r_4ro\_ .node circle,#chatgpt-mermaid-\_r_4ro\_ .node ellipse,#chatgpt-mermaid-\_r_4ro\_ .node polygon,#chatgpt-mermaid-\_r_4ro\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_4ro\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_4ro\_ .node.mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_4ro\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_4ro\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4ro\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}Region B: SingaporeRegion A: MumbaiApplication BCross-Region Read ReplicaApplication APrimary DatabaseLocal Read ReplicaCross-region replication

The primary in Mumbai replicates data to Singapore.

This provides a recovery option and may support read traffic from Singapore, depending on the data's residency and consistency requirements.

But a replica in Singapore is not automatically an independent writable primary.

If Mumbai fails, Singapore might have the data needed to recover—or it might be missing recent transactions.

### Three important terms

Primary database: The database currently authorized to accept authoritative writes for the relevant data.

Read replica: A database instance that receives changes from another database and is generally used for reads. Whether it can be promoted depends on the database and its configuration.

Cross-region replica: A replica located in another region. It can provide geographic separation for disaster recovery or regional reads, but its existence alone does not guarantee zero data loss.

## 3. Synchronous versus asynchronous replication

This distinction determines an important trade-off between write latency, availability, and the risk of losing recent commits during failover.

### A. Asynchronous replication

The primary commits a transaction without waiting for a remote replica to confirm that it has received or persisted the change.

Remote ReplicaPrimaryApplicationRemote ReplicaPrimaryApplication#chatgpt-mermaid-\_r_4s1\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_4s1\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_4s1\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_4s1\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_4s1\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4s1\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_4s1\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_4s1\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_4s1\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_4s1\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_4s1\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_4s1\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4s1\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4s1\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_4s1\_ p{margin:0;}#chatgpt-mermaid-\_r_4s1\_ .actor{stroke:rgb(31, 78, 148);fill:rgb(9, 23, 44);stroke-width:1;}#chatgpt-mermaid-\_r_4s1\_ rect.actor.outer-path[data-look="neo"]{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4s1\_ rect.note[data-look="neo"]{stroke:rgb(58, 132, 63);fill:rgb(48, 48, 48);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4s1\_ text.actor>tspan{fill:rgb(237, 237, 237);stroke:none;}#chatgpt-mermaid-\_r_4s1\_ .actor-line{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4s1\_ .innerArc{stroke-width:1.5;stroke-dasharray:none;}#chatgpt-mermaid-\_r_4s1\_ .messageLine0{stroke-width:1.5;stroke-dasharray:none;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4s1\_ .messageLine1{stroke-width:1.5;stroke-dasharray:2,2;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4s1\_ [id$="-arrowhead"] path{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4s1\_ .sequenceNumber{fill:#505050;}#chatgpt-mermaid-\_r_4s1\_ [id$="-sequencenumber"]{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4s1\_ [id$="-crosshead"] path{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4s1\_ .messageText{fill:rgb(237, 237, 237);stroke:none;}#chatgpt-mermaid-\_r_4s1\_ .labelBox{stroke:rgba(255, 255, 255, 0.15);fill:rgb(0, 0, 0);filter:none;}#chatgpt-mermaid-\_r_4s1\_ .labelText,#chatgpt-mermaid-\_r_4s1\_ .labelText>tspan{fill:rgb(237, 237, 237);stroke:none;}#chatgpt-mermaid-\_r_4s1\_ .loopText,#chatgpt-mermaid-\_r_4s1\_ .loopText>tspan{fill:rgb(237, 237, 237);stroke:none;}#chatgpt-mermaid-\_r_4s1\_ .sectionTitle,#chatgpt-mermaid-\_r_4s1\_ .sectionTitle>tspan{fill:rgb(237, 237, 237);stroke:none;}#chatgpt-mermaid-\_r_4s1\_ .loopLine{stroke-width:2px;stroke-dasharray:2,2;stroke:rgba(255, 255, 255, 0.15);fill:rgba(255, 255, 255, 0.15);}#chatgpt-mermaid-\_r_4s1\_ .note{stroke:rgb(58, 132, 63);fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_4s1\_ .noteText,#chatgpt-mermaid-\_r_4s1\_ .noteText>tspan{fill:rgb(237, 237, 237);stroke:none;font-weight:normal;}#chatgpt-mermaid-\_r_4s1\_ .activation0{fill:rgb(48, 48, 48);stroke:hsl(0, 0%, 8.8235294118%);}#chatgpt-mermaid-\_r_4s1\_ .activation1{fill:rgb(48, 48, 48);stroke:hsl(0, 0%, 8.8235294118%);}#chatgpt-mermaid-\_r_4s1\_ .activation2{fill:rgb(48, 48, 48);stroke:hsl(0, 0%, 8.8235294118%);}#chatgpt-mermaid-\_r_4s1\_ .actorPopupMenu{position:absolute;}#chatgpt-mermaid-\_r_4s1\_ .actorPopupMenuPanel{position:absolute;fill:rgb(9, 23, 44);box-shadow:0px 8px 16px 0px rgba(0,0,0,0.2);filter:drop-shadow(3px 5px 2px rgb(0 0 0 / 0.4));}#chatgpt-mermaid-\_r_4s1\_ .actor-man circle,#chatgpt-mermaid-\_r_4s1\_ line{fill:rgb(9, 23, 44);stroke-width:2px;}#chatgpt-mermaid-\_r_4s1\_ g rect.rect{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_4s1\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_4s1\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_4s1\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_4s1\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_4s1\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4s1\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_4s1\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_4s1\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_4s1\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4s1\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_4s1\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_4s1\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4s1\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_4s1\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_4s1\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4s1\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_4s1\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4s1\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}Write transactionCommit transactionSuccessReplicate changeApply change

The application receives success before the remote replica necessarily has the transaction.

Suppose:

```
10:00:00.000  Primary commits transaction T1.
10:00:00.010  Application receives success.
10:00:00.030  Region fails before T1 reaches the replica.
```

If the recovery process promotes the lagging replica, T1 might be missing.

This is the fundamental risk of asynchronous replication during failover.

Advantages

- Lower write latency because the application does not wait for remote confirmation.
- Remote network interruptions need not immediately block every write.
- Often suitable for cross-region disaster-recovery replicas.

Disadvantages

- The replica may be stale.
- Recent acknowledged writes may be lost during failover.
- Replica lag can vary significantly with network conditions and workload.

Asynchronous replication is often a sensible choice when low write latency matters and the business accepts a defined recovery point objective.

### B. Synchronous replication

With synchronous replication, the primary waits for the required acknowledgement before completing the operation according to the configured commit semantics.

Conceptually:

ReplicaPrimaryApplicationReplicaPrimaryApplication#chatgpt-mermaid-\_r_4sf\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_4sf\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_4sf\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_4sf\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_4sf\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4sf\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_4sf\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_4sf\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_4sf\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_4sf\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_4sf\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_4sf\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4sf\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4sf\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_4sf\_ p{margin:0;}#chatgpt-mermaid-\_r_4sf\_ .actor{stroke:rgb(31, 78, 148);fill:rgb(9, 23, 44);stroke-width:1;}#chatgpt-mermaid-\_r_4sf\_ rect.actor.outer-path[data-look="neo"]{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4sf\_ rect.note[data-look="neo"]{stroke:rgb(58, 132, 63);fill:rgb(48, 48, 48);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4sf\_ text.actor>tspan{fill:rgb(237, 237, 237);stroke:none;}#chatgpt-mermaid-\_r_4sf\_ .actor-line{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4sf\_ .innerArc{stroke-width:1.5;stroke-dasharray:none;}#chatgpt-mermaid-\_r_4sf\_ .messageLine0{stroke-width:1.5;stroke-dasharray:none;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4sf\_ .messageLine1{stroke-width:1.5;stroke-dasharray:2,2;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4sf\_ [id$="-arrowhead"] path{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4sf\_ .sequenceNumber{fill:#505050;}#chatgpt-mermaid-\_r_4sf\_ [id$="-sequencenumber"]{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4sf\_ [id$="-crosshead"] path{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4sf\_ .messageText{fill:rgb(237, 237, 237);stroke:none;}#chatgpt-mermaid-\_r_4sf\_ .labelBox{stroke:rgba(255, 255, 255, 0.15);fill:rgb(0, 0, 0);filter:none;}#chatgpt-mermaid-\_r_4sf\_ .labelText,#chatgpt-mermaid-\_r_4sf\_ .labelText>tspan{fill:rgb(237, 237, 237);stroke:none;}#chatgpt-mermaid-\_r_4sf\_ .loopText,#chatgpt-mermaid-\_r_4sf\_ .loopText>tspan{fill:rgb(237, 237, 237);stroke:none;}#chatgpt-mermaid-\_r_4sf\_ .sectionTitle,#chatgpt-mermaid-\_r_4sf\_ .sectionTitle>tspan{fill:rgb(237, 237, 237);stroke:none;}#chatgpt-mermaid-\_r_4sf\_ .loopLine{stroke-width:2px;stroke-dasharray:2,2;stroke:rgba(255, 255, 255, 0.15);fill:rgba(255, 255, 255, 0.15);}#chatgpt-mermaid-\_r_4sf\_ .note{stroke:rgb(58, 132, 63);fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_4sf\_ .noteText,#chatgpt-mermaid-\_r_4sf\_ .noteText>tspan{fill:rgb(237, 237, 237);stroke:none;font-weight:normal;}#chatgpt-mermaid-\_r_4sf\_ .activation0{fill:rgb(48, 48, 48);stroke:hsl(0, 0%, 8.8235294118%);}#chatgpt-mermaid-\_r_4sf\_ .activation1{fill:rgb(48, 48, 48);stroke:hsl(0, 0%, 8.8235294118%);}#chatgpt-mermaid-\_r_4sf\_ .activation2{fill:rgb(48, 48, 48);stroke:hsl(0, 0%, 8.8235294118%);}#chatgpt-mermaid-\_r_4sf\_ .actorPopupMenu{position:absolute;}#chatgpt-mermaid-\_r_4sf\_ .actorPopupMenuPanel{position:absolute;fill:rgb(9, 23, 44);box-shadow:0px 8px 16px 0px rgba(0,0,0,0.2);filter:drop-shadow(3px 5px 2px rgb(0 0 0 / 0.4));}#chatgpt-mermaid-\_r_4sf\_ .actor-man circle,#chatgpt-mermaid-\_r_4sf\_ line{fill:rgb(9, 23, 44);stroke-width:2px;}#chatgpt-mermaid-\_r_4sf\_ g rect.rect{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_4sf\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_4sf\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_4sf\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_4sf\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_4sf\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4sf\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_4sf\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_4sf\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_4sf\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4sf\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_4sf\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_4sf\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4sf\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_4sf\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_4sf\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4sf\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_4sf\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4sf\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}Write transactionSend replicated changePersist or process changeRequired acknowledgementComplete commitSuccess

The exact acknowledgement guarantees depend on the database and configuration. An acknowledgement may mean that a change has reached a replica, been written to a log, or been flushed to durable storage. It does not necessarily mean every replica has applied the change and can immediately serve it.

Advantages

- Can reduce the risk of losing acknowledged writes when a suitable replica is promoted.
- Can provide stronger durability guarantees when configured and operated correctly.

Disadvantages

- Adds latency, especially across regions.
- Writes may stall or fail when required replicas cannot acknowledge.
- The system must carefully define what happens during network partitions.

### Why synchronous cross-region replication can be slow

Imagine a transaction requires a remote acknowledgement from Singapore.

If the network round trip between Mumbai and Singapore takes 40 ms, the synchronous acknowledgement can add substantial latency compared with a local commit.

The actual additional latency depends on the database protocol, network conditions, and commit configuration. It is not always exactly one round trip.

For a high-throughput application, that extra delay may affect every transaction.

This is why synchronous replication is often easier to justify within a low-latency local topology than between geographically distant regions.

## 4. Does synchronous replication guarantee zero data loss?

Not automatically.

To achieve zero data loss for acknowledged transactions during a particular failure, the system must ensure that the promoted recovery target contains every transaction that was acknowledged under the promised durability contract.

That requires more than simply enabling a setting called "synchronous replication."

You must understand:

- Which replicas participate in the commit acknowledgement.
- What exactly the acknowledgement guarantees.
- Whether the replica has durably stored the transaction.
- Which failures the replication protocol tolerates.
- How promotion selects the recovery target.
- Whether another region can still accept writes.
- Whether the application can safely resume after promotion.

For example, a database might synchronously acknowledge a transaction after a standby has persisted its log record, but the standby may not yet have applied the transaction for queries. Durability and immediate read visibility are related but distinct properties.

The design must match the exact guarantees your application requires.

## 5. Replication lag: the hidden problem

Replication lag is the delay between a change becoming committed at the source and that change becoming available at the replica.

Consider a user updating their profile.

```
1. User updates profile in Mumbai.
2. Primary commits the update.
3. Application returns success.
4. User requests the profile through Singapore.
5. Singapore replica has not caught up.
6. User sees the old profile.
```

This is a read-after-write consistency problem.

The update succeeded, but the next read did not observe it.

### Common ways to handle it

Option 1: Read from the primary

For critical read-after-write paths, direct the request to the authoritative database.

This is simple but may increase latency for remote users.

Option 2: Use a consistency token or replication position

The application records a version, log position, or other database-supported marker associated with the committed change.

A subsequent read can wait until the replica has reached the required position, if the database supports that mechanism.

This is database-specific; a generic timestamp is not automatically a reliable replication barrier.

Option 3: Keep the user on the authoritative owner

After a write, route subsequent operations to the same authoritative owner until the required consistency condition is met.

This is useful in home-region architectures.

Option 4: Accept bounded staleness

For some data, stale reads are acceptable.

Examples might include analytics dashboards or aggregated reporting, provided the product requirements explicitly allow the delay.

The right choice depends on the business operation.

## 6. Not all data has the same consistency requirements

Imagine your application stores:

- User preferences.
- Product catalog entries.
- Inventory.
- Payment records.
- Report-generation status.

It is usually a mistake to apply the same consistency policy to all five.

| Data             | Typical concern                                     | Possible strategy                                                             |
| ---------------- | --------------------------------------------------- | ----------------------------------------------------------------------------- |
| User preferences | Minor delay may be acceptable                       | Asynchronous replication with suitable read behavior                          |
| Product catalog  | Updates need eventual propagation                   | Replicas with explicit staleness expectations                                 |
| Inventory        | Prevent overselling                                 | Single authoritative writer or strong coordination                            |
| Payment records  | Prevent duplicate or conflicting financial outcomes | Transactional writes, idempotency, and a clearly defined authority            |
| Report status    | UI may briefly show an older state                  | Eventual consistency, with stronger handling for terminal states where needed |

These are illustrative defaults, not universal rules.

For example, a product catalog might require immediate consistency for price changes at checkout. A report status may need stronger guarantees when a user downloads a supposedly completed report.

The important task is to identify the business invariants first.

## 7. How read replicas interact with consistent hashing

Suppose Mumbai has four primary shards, and each shard has a local read replica.

\#chatgpt-mermaid-\_r_4st\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_4st\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_4st\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_4st\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_4st\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4st\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_4st\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_4st\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_4st\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_4st\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_4st\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_4st\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4st\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4st\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_4st\_ p{margin:0;}#chatgpt-mermaid-\_r_4st\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4st\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4st\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4st\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_4st\_ .label text,#chatgpt-mermaid-\_r_4st\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4st\_ .node rect,#chatgpt-mermaid-\_r_4st\_ .node circle,#chatgpt-mermaid-\_r_4st\_ .node ellipse,#chatgpt-mermaid-\_r_4st\_ .node polygon,#chatgpt-mermaid-\_r_4st\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_4st\_ .rough-node .label text,#chatgpt-mermaid-\_r_4st\_ .node .label text,#chatgpt-mermaid-\_r_4st\_ .image-shape .label,#chatgpt-mermaid-\_r_4st\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_4st\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_4st\_ .rough-node .label,#chatgpt-mermaid-\_r_4st\_ .node .label,#chatgpt-mermaid-\_r_4st\_ .image-shape .label,#chatgpt-mermaid-\_r_4st\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_4st\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_4st\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4st\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4st\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_4st\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_4st\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_4st\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_4st\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_4st\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_4st\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_4st\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4st\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4st\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_4st\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4st\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_4st\_ .icon-shape,#chatgpt-mermaid-\_r_4st\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_4st\_ .icon-shape p,#chatgpt-mermaid-\_r_4st\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_4st\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_4st\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_4st\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_4st\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_4st\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_4st\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_4st\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_4st\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_4st\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4st\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_4st\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_4st\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_4st\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4st\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_4st\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_4st\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4st\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_4st\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_4st\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4st\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_4st\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4st\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_4st\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_4st\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_4st\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_4st\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_4st\_ .node rect,#chatgpt-mermaid-\_r_4st\_ .node circle,#chatgpt-mermaid-\_r_4st\_ .node ellipse,#chatgpt-mermaid-\_r_4st\_ .node polygon,#chatgpt-mermaid-\_r_4st\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_4st\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_4st\_ .node.mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_4st\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_4st\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4st\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}Tenant IDConsistent Hash RingPrimary Shard 2PrimaryRead ReplicaReplication

Consistent hashing can select the primary shard for a tenant.

Once the authoritative shard is known, the application can choose a suitable replica for reads if the consistency requirements permit it.

This is a two-stage decision:

1. Determine the authoritative shard.
2. Choose an eligible read endpoint for the operation.

You should not independently hash the tenant across all primary and replica nodes as if they were interchangeable owners. That could send writes to a replica or cause different requests to treat different databases as authoritative.

### A better conceptual model

```
tenant-101
    |
    v
Resolve tenant's region and partition owner
    |
    v
Consistent hash → Primary shard 2
    |
    +-- Write → Primary shard 2
    |
    +-- Read  → Primary shard 2 or an eligible replica
```

If the system supports stronger read guarantees, the replica-selection step must honor them. A replica may be geographically close but too far behind to satisfy the request.

## 8. What happens when the primary region fails?

Let's walk through a disaster-recovery scenario.

Mumbai is the primary region. Singapore contains an asynchronous replica.

```
Normal operation:
Mumbai primary → Singapore replica

Disaster:
Mumbai unavailable
Singapore replica may be behind
```

A safe recovery process should not simply route every request to Singapore immediately.

Instead:

1. Detect the regional failure.
2. Determine whether the primary is truly unavailable or merely isolated from the rest of the system.
3. Assess the recovery replica's state and the potential amount of missing data.
4. Prevent the old primary from accepting writes if it could resume or remain reachable.
5. Promote the selected recovery database when it is safe to do so.
6. Update authoritative ownership and routing information.
7. Resume writes and monitor correctness.

If the application resumes writes in Singapore while the old primary can still accept writes, the two regions may diverge. When connectivity returns, blindly merging their histories can create conflicts or data loss.

This is why failover needs ownership control and fencing, not just health checks.

We will examine the mechanics of that process in a later lesson.

## 9. Define RPO and RTO before selecting a replication strategy

Two measurements guide disaster-recovery design.

### Recovery Point Objective (RPO)

RPO describes how much data loss, measured in time, the business can tolerate after a failure.

For example:

- RPO of 0: no acknowledged data loss is acceptable under the specified failure model.
- RPO of 5 minutes: losing up to approximately five minutes of recent updates may be acceptable.

The precise guarantee must specify which failures and acknowledged transactions are covered.

### Recovery Time Objective (RTO)

RTO describes how quickly the service must be restored after an outage.

For example:

- RTO of 30 seconds: recovery must be completed within 30 seconds.
- RTO of 15 minutes: recovery may take longer if service returns within that window.

These objectives are related, but they are not interchangeable.

| Design characteristic             | Typical impact                                                                                                   |
| --------------------------------- | ---------------------------------------------------------------------------------------------------------------- |
| Asynchronous replication          | Lower write latency, but potential loss of recent writes                                                         |
| Synchronous replication           | Stronger durability possibilities, but potentially higher latency and reduced write availability during failures |
| Automated promotion               | Can reduce RTO, but must preserve safety                                                                         |
| Manual recovery                   | Can improve operator control, but may increase RTO                                                               |
| Pre-provisioned recovery capacity | Can reduce recovery time, but increases ongoing cost                                                             |

A system with zero RPO is not necessarily available during a partition. Depending on its consistency and failure model, it may need to reject or block writes rather than risk acknowledging transactions that cannot be preserved.

## 10. A practical design for a multi-region SaaS application

Suppose you have a tenant-oriented application with a PostgreSQL database, Redis, asynchronous report workers, and object storage.

A reasonable initial design might be:

- A primary database per authoritative tenant partition.
- Local read replicas for suitable read-heavy workloads.
- Cross-region replicas or backups for disaster recovery.
- Explicit RPO and RTO targets for each data category.
- Tenant-aware routing and a consistent-hashing layer within eligible shards.
- Idempotent processing for asynchronous jobs and payment-related operations.
- A controlled promotion process with fencing.
- Monitoring for replication lag, recovery status, write errors, and failover duration.

Do not assume that replicating PostgreSQL also replicates everything the application depends on. Redis state, queued messages, object storage, secrets, identity configuration, and background-job state each need their own availability and recovery strategy.

For example, a report's database record might be replicated to Singapore while the generated PDF still exists only in an unavailable Mumbai storage location. The application would have recovered the record but not the actual document.

## 11. Interview questions

Q1. Why isn't a read replica automatically safe to promote?

Because it may not contain every transaction committed by the primary, especially with asynchronous replication. Promotion must account for replication state, potential data loss, and preventing the old primary from continuing to write.

Q2. Why not use synchronous replication across every region?

It can add substantial network latency and may reduce write availability when required acknowledgements cannot be obtained. Whether it is appropriate depends on the database's guarantees, the required RPO, and the application's latency and availability requirements.

Q3. How do you solve read-after-write consistency across regions?

Possible strategies include reading from the authoritative primary, using a supported replication-position barrier, routing subsequent operations to the owner, or accepting bounded staleness where appropriate.

Q4. Does consistent hashing solve database failover?

No. It can determine shard placement, but failover requires a safe data-recovery and ownership-transition procedure. Changing the hash ring does not copy missing data or fence an old writer.

Q5. What should you monitor in a multi-region database architecture?

At minimum: replication lag, replication health, write and read latency, database availability, failover duration, recovery-point status, error rates, and whether the designated owner is accepting writes correctly.

## Key takeaway

Keep these four concepts separate:

- Replication: Copies changes between database instances.
- Consistency: Defines what readers can observe and what ordering guarantees writes receive.
- Failover: Restores service using a safe recovery target.
- Consistent hashing: Distributes keys across eligible shards or nodes.

A robust multi-region architecture defines how these mechanisms work together instead of assuming that any one of them provides all the others.

Next lesson: We'll go deeper into replication topologies: single-primary, regional primaries, sharded databases, and multi-writer designs. We'll examine which topologies fit home-region ownership, how cross-region data synchronization works, and why globally distributed writes are more difficult than globally distributed reads.

Type `next` to continue.



















# Chapter 3: Evolving from a Single Region to a Multi-Region Architecture

## Lesson 5: Database Topologies — Single Primary, Regional Primaries, Sharding, and Multi-Writer Systems

In the previous lesson, we covered synchronous and asynchronous replication, replication lag, read replicas, RPO, and RTO.

Now let's answer a bigger architectural question:

How should we organize databases when an application operates in multiple regions and serves millions of users?

The answer depends on whether you need to distribute reads, distribute writes, isolate tenants, or survive regional failures. These are different goals, and they lead to different database topologies.

We'll examine five common designs and see where consistent hashing fits into each.

## 1. Topology A: One global primary with cross-region replicas

This is often the simplest multi-region database architecture.

One database is the authoritative writer. Other regions maintain replicas.

\#chatgpt-mermaid-\_r_4ug\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_4ug\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_4ug\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_4ug\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_4ug\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4ug\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_4ug\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_4ug\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_4ug\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_4ug\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_4ug\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_4ug\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4ug\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4ug\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_4ug\_ p{margin:0;}#chatgpt-mermaid-\_r_4ug\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4ug\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4ug\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4ug\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_4ug\_ .label text,#chatgpt-mermaid-\_r_4ug\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4ug\_ .node rect,#chatgpt-mermaid-\_r_4ug\_ .node circle,#chatgpt-mermaid-\_r_4ug\_ .node ellipse,#chatgpt-mermaid-\_r_4ug\_ .node polygon,#chatgpt-mermaid-\_r_4ug\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_4ug\_ .rough-node .label text,#chatgpt-mermaid-\_r_4ug\_ .node .label text,#chatgpt-mermaid-\_r_4ug\_ .image-shape .label,#chatgpt-mermaid-\_r_4ug\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_4ug\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_4ug\_ .rough-node .label,#chatgpt-mermaid-\_r_4ug\_ .node .label,#chatgpt-mermaid-\_r_4ug\_ .image-shape .label,#chatgpt-mermaid-\_r_4ug\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_4ug\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_4ug\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4ug\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4ug\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_4ug\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_4ug\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_4ug\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_4ug\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_4ug\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_4ug\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_4ug\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4ug\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4ug\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_4ug\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4ug\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_4ug\_ .icon-shape,#chatgpt-mermaid-\_r_4ug\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_4ug\_ .icon-shape p,#chatgpt-mermaid-\_r_4ug\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_4ug\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_4ug\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_4ug\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_4ug\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_4ug\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_4ug\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_4ug\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_4ug\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_4ug\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4ug\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_4ug\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_4ug\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_4ug\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4ug\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_4ug\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_4ug\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4ug\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_4ug\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_4ug\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4ug\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_4ug\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4ug\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_4ug\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_4ug\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_4ug\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_4ug\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_4ug\_ .node rect,#chatgpt-mermaid-\_r_4ug\_ .node circle,#chatgpt-mermaid-\_r_4ug\_ .node ellipse,#chatgpt-mermaid-\_r_4ug\_ .node polygon,#chatgpt-mermaid-\_r_4ug\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_4ug\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_4ug\_ .node.mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_4ug\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_4ug\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4ug\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}Region CRegion BRegion AApplication CRead ReplicaApplication BRead ReplicaApplication APrimary DatabaseReplicationReplication

### How it works

Suppose Region A is Mumbai, Region B is Singapore, and Region C is Frankfurt.

- Mumbai accepts writes.
- Singapore and Frankfurt receive replicated changes.
- Applications may read from regional replicas if the consistency requirements allow it.
- Writes from Singapore and Frankfurt must reach the authoritative primary.

### Example

A user in Frankfurt updates their profile.

```
Frankfurt API
     |
     v
Mumbai Primary
     |
     v
Commit transaction
     |
     v
Replicate to Frankfurt
```

This introduces a cross-region write round trip.

The user may experience noticeable latency if the application waits for the primary to commit before returning success.

### Where this design works well

- Applications with moderate write volume.
- Read-heavy workloads.
- Systems that can tolerate remote write latency.
- Applications that prefer a simple, single-writer consistency model.

### Limitations

The primary region can become a bottleneck or a critical dependency.

If Mumbai becomes unavailable, the application needs a recovery strategy. A read replica in Frankfurt does not automatically become the primary or guarantee that every acknowledged transaction is present.

### Where consistent hashing fits

You can still shard the primary database into multiple primary shards, each with its own replicas.

For example:

```
Tenant ID
    |
    v
Consistent hash ring
    |
    +--> Shard 1 primary --> Regional replicas
    |
    +--> Shard 2 primary --> Regional replicas
    |
    +--> Shard 3 primary --> Regional replicas
```

The topology can therefore combine sharding with replication. Consistent hashing determines which shard owns a key; replication maintains additional copies of that shard's data.

## 2. Topology B: Multiple regional primaries with partition ownership

This design distributes write ownership across regions.

Instead of every write going to one global primary, each region owns particular partitions.

For example:

```
Tenant A → Mumbai primary
Tenant B → Singapore primary
Tenant C → Frankfurt primary
Tenant D → Mumbai primary
```

Each partition has an authoritative writer, but different partitions can have different writers.

\#chatgpt-mermaid-\_r_4v8\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_4v8\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_4v8\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_4v8\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_4v8\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4v8\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_4v8\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_4v8\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_4v8\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_4v8\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_4v8\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_4v8\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4v8\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4v8\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_4v8\_ p{margin:0;}#chatgpt-mermaid-\_r_4v8\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4v8\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4v8\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4v8\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_4v8\_ .label text,#chatgpt-mermaid-\_r_4v8\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4v8\_ .node rect,#chatgpt-mermaid-\_r_4v8\_ .node circle,#chatgpt-mermaid-\_r_4v8\_ .node ellipse,#chatgpt-mermaid-\_r_4v8\_ .node polygon,#chatgpt-mermaid-\_r_4v8\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_4v8\_ .rough-node .label text,#chatgpt-mermaid-\_r_4v8\_ .node .label text,#chatgpt-mermaid-\_r_4v8\_ .image-shape .label,#chatgpt-mermaid-\_r_4v8\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_4v8\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_4v8\_ .rough-node .label,#chatgpt-mermaid-\_r_4v8\_ .node .label,#chatgpt-mermaid-\_r_4v8\_ .image-shape .label,#chatgpt-mermaid-\_r_4v8\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_4v8\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_4v8\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4v8\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4v8\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_4v8\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_4v8\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_4v8\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_4v8\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_4v8\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_4v8\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_4v8\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4v8\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4v8\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_4v8\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4v8\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_4v8\_ .icon-shape,#chatgpt-mermaid-\_r_4v8\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_4v8\_ .icon-shape p,#chatgpt-mermaid-\_r_4v8\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_4v8\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_4v8\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_4v8\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_4v8\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_4v8\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_4v8\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_4v8\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_4v8\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_4v8\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4v8\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_4v8\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_4v8\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_4v8\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4v8\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_4v8\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_4v8\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4v8\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_4v8\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_4v8\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4v8\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_4v8\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4v8\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_4v8\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_4v8\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_4v8\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_4v8\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_4v8\_ .node rect,#chatgpt-mermaid-\_r_4v8\_ .node circle,#chatgpt-mermaid-\_r_4v8\_ .node ellipse,#chatgpt-mermaid-\_r_4v8\_ .node polygon,#chatgpt-mermaid-\_r_4v8\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_4v8\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_4v8\_ .node.mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_4v8\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_4v8\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4v8\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}Global Request RouterPartition Ownership ServiceRegion ARegion BRegion CPrimary for A-ownedpartitionsPrimary for B-ownedpartitionsPrimary for C-ownedpartitionsOptional replicationOptional replicationOptional replication

The replication arrows are illustrative. A real system may replicate each partition only to designated recovery regions rather than replicate every database to every other database.

### How it works

Suppose Tenant A belongs to Mumbai.

1. A request enters through Singapore.
2. The routing layer identifies Tenant A.
3. The ownership service resolves Mumbai as the authoritative region.
4. The write is forwarded to Mumbai.
5. Mumbai commits the transaction.

Tenant B can follow a similar flow with Singapore as its owner.

This architecture allows the system to distribute write load across regions without allowing arbitrary concurrent writes to the same partition.

### Important distinction

Multiple regional primaries do not necessarily mean multi-writer data.

If Mumbai owns Tenant A and Singapore owns Tenant B, each region is authoritative for its own partitions. Both regions can accept writes, but they are not independently writing the same partition.

This is often easier to reason about than allowing every region to write to every tenant's data.

### Where consistent hashing fits

Suppose Mumbai owns 100,000 tenants but has 20 database shards.

You can use consistent hashing to distribute those tenants among the eligible Mumbai shards.

```
Tenant
  |
  v
Resolve home region: Mumbai
  |
  v
Mumbai consistent-hashing ring
  |
  v
Select shard
  |
  v
Write to that shard's primary
```

The region is selected first. Consistent hashing then distributes keys among eligible shards within that region.

### The migration problem

Suppose Tenant A needs to move from Mumbai to Singapore.

Changing its home-region metadata is not enough.

You need to:

1. Copy its data to Singapore.
2. Capture changes made during the copy.
3. Synchronize the target until it reaches the required state.
4. Prevent concurrent writes from being accepted by the old owner after cutover.
5. Update authoritative ownership.
6. Direct new requests to Singapore.
7. Verify that the new owner is serving the correct data.

If the migration process skips coordination, requests may write to both regions or read from an incomplete copy.

## 3. Topology C: Sharded databases with regional replicas

Now suppose your application has grown beyond what a single database can handle.

You partition the data into multiple shards, and each shard has its own primary and replicas.

\#chatgpt-mermaid-\_r_4vm\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_4vm\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_4vm\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_4vm\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_4vm\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4vm\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_4vm\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_4vm\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_4vm\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_4vm\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_4vm\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_4vm\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4vm\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4vm\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_4vm\_ p{margin:0;}#chatgpt-mermaid-\_r_4vm\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4vm\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4vm\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4vm\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_4vm\_ .label text,#chatgpt-mermaid-\_r_4vm\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4vm\_ .node rect,#chatgpt-mermaid-\_r_4vm\_ .node circle,#chatgpt-mermaid-\_r_4vm\_ .node ellipse,#chatgpt-mermaid-\_r_4vm\_ .node polygon,#chatgpt-mermaid-\_r_4vm\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_4vm\_ .rough-node .label text,#chatgpt-mermaid-\_r_4vm\_ .node .label text,#chatgpt-mermaid-\_r_4vm\_ .image-shape .label,#chatgpt-mermaid-\_r_4vm\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_4vm\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_4vm\_ .rough-node .label,#chatgpt-mermaid-\_r_4vm\_ .node .label,#chatgpt-mermaid-\_r_4vm\_ .image-shape .label,#chatgpt-mermaid-\_r_4vm\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_4vm\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_4vm\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4vm\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4vm\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_4vm\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_4vm\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_4vm\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_4vm\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_4vm\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_4vm\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_4vm\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4vm\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4vm\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_4vm\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4vm\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_4vm\_ .icon-shape,#chatgpt-mermaid-\_r_4vm\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_4vm\_ .icon-shape p,#chatgpt-mermaid-\_r_4vm\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_4vm\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_4vm\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_4vm\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_4vm\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_4vm\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_4vm\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_4vm\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_4vm\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_4vm\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4vm\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_4vm\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_4vm\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_4vm\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4vm\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_4vm\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_4vm\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4vm\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_4vm\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_4vm\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4vm\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_4vm\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4vm\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_4vm\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_4vm\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_4vm\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_4vm\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_4vm\_ .node rect,#chatgpt-mermaid-\_r_4vm\_ .node circle,#chatgpt-mermaid-\_r_4vm\_ .node ellipse,#chatgpt-mermaid-\_r_4vm\_ .node polygon,#chatgpt-mermaid-\_r_4vm\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_4vm\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_4vm\_ .node.mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_4vm\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_4vm\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4vm\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}ApplicationConsistent Hash RingShard 1Shard 2Shard 3Primary 1Primary 2Primary 3Replica 1Replica 2Replica 3

This diagram represents logical shard ownership followed by primary-replica relationships. In a real deployment, each shard's replicas can be placed according to its durability, latency, and disaster-recovery requirements.

### Why shard?

Sharding distributes data and workload across multiple database instances.

For example, rather than one database handling all 10 million tenants, you might distribute them across 20 shards.

Each shard stores a subset of the data and handles a subset of the requests.

### What consistent hashing contributes

Without a suitable partitioning strategy, adding or removing a shard can cause many keys to be reassigned.

Consistent hashing reduces unnecessary remapping when shard membership changes.

However, it does not automatically:

- Create new databases.
- Copy records to the new shard.
- Preserve transactions across shards.
- Replicate the data to another region.
- Prevent hot tenants from overwhelming a shard.

These concerns require separate mechanisms.

### A subtle issue: replication placement

Suppose the primary for Shard 1 is in Mumbai and its only replica is also in Mumbai.

This protects against some instance failures, but not necessarily a complete Mumbai regional outage.

For regional disaster recovery, you need an appropriate cross-region recovery copy, such as a replica or backup, depending on your recovery requirements.

Replication placement should therefore reflect the failure domains you need to survive.

## 4. Topology D: Multi-writer databases

In a multi-writer topology, multiple regions can accept writes to the same logical data.

\#chatgpt-mermaid-\_r_4vv\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_4vv\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_4vv\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_4vv\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_4vv\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4vv\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_4vv\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_4vv\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_4vv\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_4vv\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_4vv\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_4vv\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4vv\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4vv\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_4vv\_ p{margin:0;}#chatgpt-mermaid-\_r_4vv\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4vv\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4vv\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4vv\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_4vv\_ .label text,#chatgpt-mermaid-\_r_4vv\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4vv\_ .node rect,#chatgpt-mermaid-\_r_4vv\_ .node circle,#chatgpt-mermaid-\_r_4vv\_ .node ellipse,#chatgpt-mermaid-\_r_4vv\_ .node polygon,#chatgpt-mermaid-\_r_4vv\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_4vv\_ .rough-node .label text,#chatgpt-mermaid-\_r_4vv\_ .node .label text,#chatgpt-mermaid-\_r_4vv\_ .image-shape .label,#chatgpt-mermaid-\_r_4vv\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_4vv\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_4vv\_ .rough-node .label,#chatgpt-mermaid-\_r_4vv\_ .node .label,#chatgpt-mermaid-\_r_4vv\_ .image-shape .label,#chatgpt-mermaid-\_r_4vv\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_4vv\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_4vv\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4vv\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4vv\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_4vv\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_4vv\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_4vv\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_4vv\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_4vv\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_4vv\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_4vv\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4vv\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4vv\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_4vv\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4vv\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_4vv\_ .icon-shape,#chatgpt-mermaid-\_r_4vv\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_4vv\_ .icon-shape p,#chatgpt-mermaid-\_r_4vv\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_4vv\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_4vv\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_4vv\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_4vv\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_4vv\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_4vv\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_4vv\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_4vv\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_4vv\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4vv\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_4vv\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_4vv\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_4vv\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4vv\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_4vv\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_4vv\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4vv\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_4vv\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_4vv\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4vv\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_4vv\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4vv\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_4vv\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_4vv\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_4vv\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_4vv\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_4vv\_ .node rect,#chatgpt-mermaid-\_r_4vv\_ .node circle,#chatgpt-mermaid-\_r_4vv\_ .node ellipse,#chatgpt-mermaid-\_r_4vv\_ .node polygon,#chatgpt-mermaid-\_r_4vv\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_4vv\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_4vv\_ .node.mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_4vv\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_4vv\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4vv\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}Region A ApplicationRegion A DataRegion B ApplicationRegion B DataRegion C ApplicationRegion C DataReplication and ConflictResolution

This is attractive because users can potentially write locally in multiple regions.

But the hard problem is not sending changes between databases. The hard problem is preserving the meaning of concurrent updates.

### Example: concurrent updates

Suppose a user's account has:

```
Account status = ACTIVE
```

Two regions receive different requests:

```
Mumbai:    Set status to SUSPENDED
Singapore: Set status to ACTIVE
```

Both regions may accept their local updates before receiving the other region's change.

What should the final state be?

The answer depends on the application's rules.

A generic last-write-wins policy might select one value using timestamps, but timestamps alone do not necessarily represent the correct business order. Clock skew and concurrent operations can complicate the decision.

For a simple preference, a last-write-wins policy may be acceptable. For account suspension, payment processing, or inventory reservations, a generic conflict policy may be unsafe.

### Common strategies

- Single-writer ownership: Route writes for each entity to one authoritative region.
- Optimistic concurrency: Reject conflicting updates using versions or conditional writes.
- Conflict resolution: Apply explicit application-specific rules.
- CRDTs: Use data types and operations designed to merge concurrent updates safely.
- Coordination: Require a suitable strongly consistent protocol for operations that must preserve strict invariants.

Each approach has constraints. CRDTs, for example, are not a universal replacement for transactions or coordination.

### Where consistent hashing fits

Consistent hashing can help assign keys to nodes in a distributed system. But it does not solve conflicts when multiple writers independently update the same key.

A ring may tell you which node normally owns a key. It does not automatically stop another region from accepting a conflicting write during a network partition.

That requires a consistency protocol, an ownership mechanism, or another explicit safeguard.

## 5. Topology E: Globally distributed database with built-in replication

Some distributed database systems provide replication, partition placement, and consistency protocols as part of the database architecture.

Conceptually:

\#chatgpt-mermaid-\_r_50i\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_50i\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_50i\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_50i\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_50i\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_50i\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_50i\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_50i\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_50i\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_50i\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_50i\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_50i\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_50i\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_50i\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_50i\_ p{margin:0;}#chatgpt-mermaid-\_r_50i\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_50i\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_50i\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_50i\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_50i\_ .label text,#chatgpt-mermaid-\_r_50i\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_50i\_ .node rect,#chatgpt-mermaid-\_r_50i\_ .node circle,#chatgpt-mermaid-\_r_50i\_ .node ellipse,#chatgpt-mermaid-\_r_50i\_ .node polygon,#chatgpt-mermaid-\_r_50i\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_50i\_ .rough-node .label text,#chatgpt-mermaid-\_r_50i\_ .node .label text,#chatgpt-mermaid-\_r_50i\_ .image-shape .label,#chatgpt-mermaid-\_r_50i\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_50i\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_50i\_ .rough-node .label,#chatgpt-mermaid-\_r_50i\_ .node .label,#chatgpt-mermaid-\_r_50i\_ .image-shape .label,#chatgpt-mermaid-\_r_50i\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_50i\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_50i\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_50i\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_50i\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_50i\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_50i\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_50i\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_50i\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_50i\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_50i\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_50i\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_50i\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_50i\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_50i\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_50i\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_50i\_ .icon-shape,#chatgpt-mermaid-\_r_50i\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_50i\_ .icon-shape p,#chatgpt-mermaid-\_r_50i\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_50i\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_50i\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_50i\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_50i\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_50i\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_50i\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_50i\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_50i\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_50i\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_50i\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_50i\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_50i\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_50i\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_50i\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_50i\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_50i\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_50i\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_50i\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_50i\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_50i\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_50i\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_50i\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_50i\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_50i\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_50i\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_50i\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_50i\_ .node rect,#chatgpt-mermaid-\_r_50i\_ .node circle,#chatgpt-mermaid-\_r_50i\_ .node ellipse,#chatgpt-mermaid-\_r_50i\_ .node polygon,#chatgpt-mermaid-\_r_50i\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_50i\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_50i\_ .node.mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_50i\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_50i\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_50i\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}Application Region ADistributed DatabaseApplication Region BApplication Region CPartition PlacementReplicationConsistency and ConsensusProtocol

The database may distribute partitions across regions, replicate them, and coordinate writes according to its configured consistency model.

This can reduce the amount of replication and ownership logic your application needs to implement itself.

However, the application still needs to understand the database's actual guarantees.

Questions to investigate include:

- What consistency levels are supported?
- How are writes acknowledged?
- Which failures can the database tolerate?
- What happens to writes during a network partition?
- How is data placed across regions?
- Can placement be restricted to meet residency requirements?
- What are the latency and cost implications?
- How are partition splits and rebalancing handled?

A globally distributed database does not necessarily mean every write is low-latency in every region. Strongly consistent operations may require coordination across multiple nodes or regions.

Similarly, a database may provide globally distributed storage without supporting arbitrary, low-latency multi-region writes with the consistency guarantees your business needs.

## 6. Compare the topologies

| Topology                                    | Write model                                       | Main advantage                            | Main trade-off                                     |
| ------------------------------------------- | ------------------------------------------------- | ----------------------------------------- | -------------------------------------------------- |
| Single primary with regional replicas       | One authoritative primary                         | Simple ownership and consistency model    | Remote write latency and primary-region dependency |
| Regional primaries with partition ownership | One writer per partition                          | Distributes write workload across regions | Ownership and migration complexity                 |
| Sharded databases with replicas             | One writer per shard, usually                     | Horizontal data and workload scaling      | Shard management and cross-shard operations        |
| Multi-writer database                       | Multiple writers may update the same logical data | Local writes across regions               | Conflict resolution and consistency complexity     |
| Globally distributed database               | Depends on the database's consistency model       | Integrated distribution and replication   | Cost, latency, and system-specific constraints     |

These topologies are not mutually exclusive.

For example, a SaaS platform might use regional primaries for tenant ownership, shard each region's data, replicate each shard to a recovery region, and use consistent hashing to distribute tenants among shards.

That is a combination of several patterns rather than one isolated architecture.

## 7. A worked design: Multi-region SaaS with PostgreSQL

Let's design a system that stores customer records, generates reports, and processes subscriptions.

Assume:

- Customers have home regions.
- Each region has multiple PostgreSQL shards.
- The application runs in multiple regions.
- Reports can be generated asynchronously.
- Billing operations require strict safeguards against duplicate processing.
- Regional disaster recovery is required.

A possible architecture is:

\#chatgpt-mermaid-\_r_50r\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_50r\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_50r\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_50r\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_50r\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_50r\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_50r\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_50r\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_50r\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_50r\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_50r\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_50r\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_50r\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_50r\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_50r\_ p{margin:0;}#chatgpt-mermaid-\_r_50r\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_50r\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_50r\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_50r\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_50r\_ .label text,#chatgpt-mermaid-\_r_50r\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_50r\_ .node rect,#chatgpt-mermaid-\_r_50r\_ .node circle,#chatgpt-mermaid-\_r_50r\_ .node ellipse,#chatgpt-mermaid-\_r_50r\_ .node polygon,#chatgpt-mermaid-\_r_50r\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_50r\_ .rough-node .label text,#chatgpt-mermaid-\_r_50r\_ .node .label text,#chatgpt-mermaid-\_r_50r\_ .image-shape .label,#chatgpt-mermaid-\_r_50r\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_50r\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_50r\_ .rough-node .label,#chatgpt-mermaid-\_r_50r\_ .node .label,#chatgpt-mermaid-\_r_50r\_ .image-shape .label,#chatgpt-mermaid-\_r_50r\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_50r\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_50r\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_50r\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_50r\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_50r\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_50r\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_50r\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_50r\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_50r\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_50r\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_50r\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_50r\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_50r\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_50r\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_50r\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_50r\_ .icon-shape,#chatgpt-mermaid-\_r_50r\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_50r\_ .icon-shape p,#chatgpt-mermaid-\_r_50r\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_50r\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_50r\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_50r\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_50r\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_50r\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_50r\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_50r\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_50r\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_50r\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_50r\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_50r\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_50r\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_50r\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_50r\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_50r\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_50r\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_50r\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_50r\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_50r\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_50r\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_50r\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_50r\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_50r\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_50r\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_50r\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_50r\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_50r\_ .node rect,#chatgpt-mermaid-\_r_50r\_ .node circle,#chatgpt-mermaid-\_r_50r\_ .node ellipse,#chatgpt-mermaid-\_r_50r\_ .node polygon,#chatgpt-mermaid-\_r_50r\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_50r\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_50r\_ .node.mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_50r\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_50r\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_50r\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}UsersGlobal Traffic RoutingRegional ApplicationResolve Tenant OwnerConsistent Hashing WithinRegionRegion A ShardRegion B ShardRecovery Copy for Region ARecovery Copy for Region BDurable QueueBackground Workers

The diagram simplifies the ownership relationships; each region has its own shard set, and each shard's recovery copy must be configured according to the actual database technology and recovery objectives.

### Request flow

For a write request:

1. The global routing layer sends the request to a healthy application region.
2. The application authenticates and authorizes the caller.
3. The tenant policy identifies the allowed region and authoritative owner.
4. The regional placement layer identifies the correct shard.
5. The application writes to that shard's primary.
6. Any associated asynchronous work is recorded durably and processed using suitable idempotency guarantees.

### What happens if a shard is added?

The consistent-hashing placement may change for a subset of tenant keys.

The system must then migrate those tenants' records and associated data safely. The old owner remains authoritative until the migration and cutover procedure says otherwise.

### What happens if a region fails?

The system must recover the affected partitions using their recovery copies or another supported recovery mechanism. It must establish the new owner, fence the old owner where necessary, and update routing metadata.

Consistent hashing can help choose the destination shards after the new ownership arrangement is established. It does not perform the recovery itself.

## 8. How to choose the right topology

Use this sequence when making a system-design decision.

Question 1: Do you need to distribute writes?

If not, a primary with regional read replicas may be sufficient.

Question 2: Do different tenants need different authoritative regions?

If yes, consider home-region ownership with regional primaries.

Question 3: Is a single database per region insufficient for scale?

If yes, consider sharding within each region. Consistent hashing can be one of the placement mechanisms.

Question 4: Must the same entity accept writes independently in multiple regions?

If yes, examine multi-writer designs carefully. First determine whether the business invariants can tolerate concurrent updates and what coordination or conflict-resolution strategy is required.

Question 5: Can the database provide the required distribution and consistency guarantees natively?

If yes, a distributed database may simplify the architecture. Evaluate its actual behavior under regional failures, its latency characteristics, and its residency capabilities before committing to it.

## 9. Interview questions

Q1. What is the difference between sharding and replication?

Sharding divides the dataset into separate partitions, distributing storage and workload. Replication maintains additional copies of data for availability, read scaling, or recovery. A system can use both.

Q2. Can consistent hashing replace a database replication strategy?

No. It helps determine key placement across nodes. Replication determines how data is copied and maintained across instances. The two solve different problems.

Q3. Why is single-writer-per-partition often a good multi-region design?

It provides clear write ownership and avoids many concurrent-write conflicts. Different partitions can still have different regional writers, allowing write workloads to be distributed without permitting arbitrary multi-writer access to the same partition.

Q4. Why is multi-writer replication difficult?

Different regions may accept conflicting updates before learning about each other. The system needs a defined consistency and conflict-resolution strategy that preserves its business invariants.

Q5. Would you always use consistent hashing for database sharding?

No. Consistent hashing is useful when membership changes and limited remapping are important. Range partitioning, directory-based placement, and other partitioning strategies can be better for range queries, controlled tenant placement, or particular data-access patterns.

## Key takeaway

The database topology determines how writes, replicas, and partitions behave. Consistent hashing is a placement tool within that broader design.

A robust multi-region architecture explicitly defines:

- Which database or partition is authoritative.
- Where replicas are placed.
- What replication guarantees apply.
- How much data loss and downtime are acceptable.
- How ownership changes during migration and failover.
- Whether concurrent writes are permitted and how conflicts are resolved.

Next lesson: We'll examine how data is synchronized across regions in practice, including replication streams, change data capture (CDC), transactional outbox patterns, event-driven synchronization, ordering, duplicate delivery, and recovery after a replication pipeline falls behind.

Type `next` to continue.



















# Chapter 3: Evolving from Single Region to Multi-Region Architecture

## Lesson 6: Cross-Region Data Synchronization in Practice

In the previous lesson, we explored different database topologies: global primary, regional primaries, sharded databases with replicas, multi-writer databases, and globally distributed databases.

Now we need to answer a practical question:

When data changes in one region, how does that change reach the other regions reliably?

Suppose your SaaS application runs in India and Europe. A customer updates their profile in the India region. Your European region may need that update for local reads, analytics, search, or disaster recovery.

Simply deploying the same application in both regions does not synchronize data. You need a deliberate synchronization mechanism with well-defined guarantees.

## 1. Understand the basic synchronization flow

Imagine this architecture:

\#chatgpt-mermaid-\_r_524\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_524\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_524\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_524\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_524\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_524\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_524\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_524\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_524\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_524\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_524\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_524\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_524\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_524\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_524\_ p{margin:0;}#chatgpt-mermaid-\_r_524\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_524\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_524\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_524\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_524\_ .label text,#chatgpt-mermaid-\_r_524\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_524\_ .node rect,#chatgpt-mermaid-\_r_524\_ .node circle,#chatgpt-mermaid-\_r_524\_ .node ellipse,#chatgpt-mermaid-\_r_524\_ .node polygon,#chatgpt-mermaid-\_r_524\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_524\_ .rough-node .label text,#chatgpt-mermaid-\_r_524\_ .node .label text,#chatgpt-mermaid-\_r_524\_ .image-shape .label,#chatgpt-mermaid-\_r_524\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_524\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_524\_ .rough-node .label,#chatgpt-mermaid-\_r_524\_ .node .label,#chatgpt-mermaid-\_r_524\_ .image-shape .label,#chatgpt-mermaid-\_r_524\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_524\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_524\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_524\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_524\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_524\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_524\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_524\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_524\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_524\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_524\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_524\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_524\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_524\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_524\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_524\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_524\_ .icon-shape,#chatgpt-mermaid-\_r_524\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_524\_ .icon-shape p,#chatgpt-mermaid-\_r_524\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_524\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_524\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_524\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_524\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_524\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_524\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_524\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_524\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_524\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_524\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_524\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_524\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_524\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_524\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_524\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_524\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_524\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_524\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_524\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_524\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_524\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_524\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_524\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_524\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_524\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_524\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_524\_ .node rect,#chatgpt-mermaid-\_r_524\_ .node circle,#chatgpt-mermaid-\_r_524\_ .node ellipse,#chatgpt-mermaid-\_r_524\_ .node polygon,#chatgpt-mermaid-\_r_524\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_524\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_524\_ .node.mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_524\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_524\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_524\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}Europe RegionIndia RegionReplication ConsumerRegional Database / ReplicaApplication APIPrimary DatabaseChange Log / OutboxReplication / CDC Pipeline

The basic process is:

1. An application request changes data in India.
2. The change is committed to the source database.
3. A synchronization mechanism detects the committed change.
4. The change travels to Europe.
5. Europe applies the change to its database or replica.
6. The European copy eventually reflects the update.

Notice the word eventually. With asynchronous synchronization, the two regions can temporarily contain different data.

For example:

| Time     | India database     | Europe database    |
| -------- | ------------------ | ------------------ |
| 10:00:00 | Profile version 10 | Profile version 10 |
| 10:00:01 | Profile version 11 | Profile version 10 |
| 10:00:02 | Profile version 11 | Profile version 10 |
| 10:00:03 | Profile version 11 | Profile version 11 |

During the synchronization delay, a European read could return stale data.

That is not necessarily a bug. It depends on the consistency guarantee your application promises.

The real engineering challenge is ensuring that synchronization is reliable, observable, recoverable, and compatible with your data ownership rules.

## 2. Four common synchronization approaches

There are four major approaches worth distinguishing.

| Approach                                | How changes travel                                                           | Common use                                            |
| --------------------------------------- | ---------------------------------------------------------------------------- | ----------------------------------------------------- |
| Database-native replication             | The database streams its own replication records                             | Read replicas, database recovery                      |
| Change Data Capture (CDC)               | A process reads committed database changes and publishes them                | Replication pipelines, search indexes, data platforms |
| Transactional outbox                    | The application commits business data and an event record in one transaction | Reliable business-event delivery                      |
| Application-level event synchronization | Services publish and consume domain events                                   | Cross-service workflows and derived data              |

These approaches are related, but they are not interchangeable.

### Approach A: Database-native replication

A database records changes in its own transaction or replication log. A replica consumes that stream and applies the changes.

For example, PostgreSQL uses its write-ahead log (WAL). Physical streaming replication transfers WAL records so a standby can reproduce the source database's changes.

\#chatgpt-mermaid-\_r_52d\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_52d\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_52d\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_52d\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_52d\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_52d\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_52d\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_52d\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_52d\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_52d\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_52d\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_52d\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_52d\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_52d\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_52d\_ p{margin:0;}#chatgpt-mermaid-\_r_52d\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_52d\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_52d\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_52d\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_52d\_ .label text,#chatgpt-mermaid-\_r_52d\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_52d\_ .node rect,#chatgpt-mermaid-\_r_52d\_ .node circle,#chatgpt-mermaid-\_r_52d\_ .node ellipse,#chatgpt-mermaid-\_r_52d\_ .node polygon,#chatgpt-mermaid-\_r_52d\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_52d\_ .rough-node .label text,#chatgpt-mermaid-\_r_52d\_ .node .label text,#chatgpt-mermaid-\_r_52d\_ .image-shape .label,#chatgpt-mermaid-\_r_52d\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_52d\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_52d\_ .rough-node .label,#chatgpt-mermaid-\_r_52d\_ .node .label,#chatgpt-mermaid-\_r_52d\_ .image-shape .label,#chatgpt-mermaid-\_r_52d\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_52d\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_52d\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_52d\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_52d\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_52d\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_52d\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_52d\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_52d\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_52d\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_52d\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_52d\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_52d\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_52d\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_52d\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_52d\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_52d\_ .icon-shape,#chatgpt-mermaid-\_r_52d\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_52d\_ .icon-shape p,#chatgpt-mermaid-\_r_52d\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_52d\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_52d\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_52d\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_52d\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_52d\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_52d\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_52d\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_52d\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_52d\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_52d\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_52d\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_52d\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_52d\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_52d\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_52d\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_52d\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_52d\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_52d\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_52d\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_52d\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_52d\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_52d\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_52d\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_52d\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_52d\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_52d\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_52d\_ .node rect,#chatgpt-mermaid-\_r_52d\_ .node circle,#chatgpt-mermaid-\_r_52d\_ .node ellipse,#chatgpt-mermaid-\_r_52d\_ .node polygon,#chatgpt-mermaid-\_r_52d\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_52d\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_52d\_ .node.mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_52d\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_52d\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_52d\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}ApplicationPrimary PostgreSQLWAL StreamStandby PostgreSQL

The application normally does not publish a separate event for every database update. The database replication mechanism handles the transfer.

Advantages

- Designed around database durability and recovery.
- Can replicate changes without application code publishing each one.
- Well suited to read replicas and disaster recovery.

Limitations

- Replication semantics depend on the database and replication configuration.
- A standby is generally a copy of a database or a defined subset of its data, not automatically an independently writable business system.
- Physical replication often couples the replica to the source's database format and version constraints.
- Database replication alone does not reliably trigger every external business action, such as sending a notification or updating an external search service.

When to use it: When you need database-level replication, read replicas, or standby recovery.

### Approach B: Change Data Capture (CDC)

CDC captures committed database changes and exposes them to downstream consumers.

A CDC pipeline might read the database log, convert changes into structured records, and publish them to a stream such as Kafka.

\#chatgpt-mermaid-\_r_52m\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_52m\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_52m\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_52m\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_52m\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_52m\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_52m\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_52m\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_52m\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_52m\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_52m\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_52m\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_52m\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_52m\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_52m\_ p{margin:0;}#chatgpt-mermaid-\_r_52m\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_52m\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_52m\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_52m\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_52m\_ .label text,#chatgpt-mermaid-\_r_52m\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_52m\_ .node rect,#chatgpt-mermaid-\_r_52m\_ .node circle,#chatgpt-mermaid-\_r_52m\_ .node ellipse,#chatgpt-mermaid-\_r_52m\_ .node polygon,#chatgpt-mermaid-\_r_52m\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_52m\_ .rough-node .label text,#chatgpt-mermaid-\_r_52m\_ .node .label text,#chatgpt-mermaid-\_r_52m\_ .image-shape .label,#chatgpt-mermaid-\_r_52m\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_52m\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_52m\_ .rough-node .label,#chatgpt-mermaid-\_r_52m\_ .node .label,#chatgpt-mermaid-\_r_52m\_ .image-shape .label,#chatgpt-mermaid-\_r_52m\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_52m\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_52m\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_52m\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_52m\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_52m\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_52m\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_52m\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_52m\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_52m\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_52m\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_52m\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_52m\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_52m\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_52m\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_52m\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_52m\_ .icon-shape,#chatgpt-mermaid-\_r_52m\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_52m\_ .icon-shape p,#chatgpt-mermaid-\_r_52m\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_52m\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_52m\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_52m\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_52m\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_52m\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_52m\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_52m\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_52m\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_52m\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_52m\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_52m\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_52m\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_52m\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_52m\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_52m\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_52m\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_52m\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_52m\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_52m\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_52m\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_52m\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_52m\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_52m\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_52m\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_52m\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_52m\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_52m\_ .node rect,#chatgpt-mermaid-\_r_52m\_ .node circle,#chatgpt-mermaid-\_r_52m\_ .node ellipse,#chatgpt-mermaid-\_r_52m\_ .node polygon,#chatgpt-mermaid-\_r_52m\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_52m\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_52m\_ .node.mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_52m\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_52m\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_52m\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}ApplicationSource DatabaseDatabase Change LogCDC ConnectorEvent StreamSearch IndexAnalytics StoreRegional Data Store

A captured change could conceptually look like this:

```
{
  "operation": "UPDATE",
  "table": "customer_profile",
  "primary_key": {
    "customer_id": "cust-123"
  },
  "version": 11,
  "after": {
    "name": "Asha",
    "country": "IN"
  }
}
```

This is an illustrative CDC record, not a universal CDC format.

The CDC connector reads changes from the database's log rather than relying on an application to remember to publish an event after every update.

That distinction is important. If five different services can update the same table, an application-level publisher can easily miss one of those update paths. A correctly configured CDC pipeline can capture changes from all relevant transactions regardless of which service issued them.

Advantages

- Captures changes made through multiple database write paths.
- Can feed multiple independent consumers.
- Can support regional synchronization, search indexing, analytics, and migration.

Limitations

- Capturing a row change does not automatically explain its business meaning.
- Consumers must handle duplicates, ordering, schema changes, and replay.
- Log retention and replication-slot management matter. A stalled consumer can cause the source to retain large amounts of log data.
- CDC must be configured with deliberate rules for sensitive columns and region-specific data.

When to use it: When you need a reliable stream of committed database changes for multiple downstream systems.

### Approach C: Transactional outbox pattern

Suppose a payment service performs two actions:

1. Marks an order as paid in PostgreSQL.
2. Publishes an `OrderPaid` event to a message broker.

A naive implementation looks like this:

```
@Transactional
public void markOrderPaid(String orderId) {
    orderRepository.markPaid(orderId);
    eventPublisher.publish(new OrderPaid(orderId));
}
```

There is a potential failure between the two operations.

If the database commits successfully but publishing fails, the order is paid, yet no event is delivered. If publishing succeeds but the database transaction rolls back, consumers may receive an event describing a change that never committed.

A database transaction does not ordinarily include an unrelated broker publish as one atomic operation.

The transactional outbox addresses this by writing the business change and an event record into the same database transaction.

\#chatgpt-mermaid-\_r_53b\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_53b\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_53b\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_53b\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_53b\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_53b\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_53b\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_53b\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_53b\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_53b\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_53b\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_53b\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_53b\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_53b\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_53b\_ p{margin:0;}#chatgpt-mermaid-\_r_53b\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_53b\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_53b\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_53b\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_53b\_ .label text,#chatgpt-mermaid-\_r_53b\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_53b\_ .node rect,#chatgpt-mermaid-\_r_53b\_ .node circle,#chatgpt-mermaid-\_r_53b\_ .node ellipse,#chatgpt-mermaid-\_r_53b\_ .node polygon,#chatgpt-mermaid-\_r_53b\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_53b\_ .rough-node .label text,#chatgpt-mermaid-\_r_53b\_ .node .label text,#chatgpt-mermaid-\_r_53b\_ .image-shape .label,#chatgpt-mermaid-\_r_53b\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_53b\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_53b\_ .rough-node .label,#chatgpt-mermaid-\_r_53b\_ .node .label,#chatgpt-mermaid-\_r_53b\_ .image-shape .label,#chatgpt-mermaid-\_r_53b\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_53b\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_53b\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_53b\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_53b\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_53b\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_53b\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_53b\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_53b\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_53b\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_53b\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_53b\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_53b\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_53b\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_53b\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_53b\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_53b\_ .icon-shape,#chatgpt-mermaid-\_r_53b\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_53b\_ .icon-shape p,#chatgpt-mermaid-\_r_53b\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_53b\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_53b\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_53b\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_53b\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_53b\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_53b\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_53b\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_53b\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_53b\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_53b\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_53b\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_53b\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_53b\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_53b\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_53b\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_53b\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_53b\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_53b\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_53b\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_53b\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_53b\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_53b\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_53b\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_53b\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_53b\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_53b\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_53b\_ .node rect,#chatgpt-mermaid-\_r_53b\_ .node circle,#chatgpt-mermaid-\_r_53b\_ .node ellipse,#chatgpt-mermaid-\_r_53b\_ .node polygon,#chatgpt-mermaid-\_r_53b\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_53b\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_53b\_ .node.mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_53b\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_53b\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_53b\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}Mark Order as PaidSingle Database TransactionUpdate OrderInsert Outbox EventCommit TransactionOutbox Publisher / CDCMessage BrokerRegional ConsumerApply Change

For example, the transaction writes:

Orders table

| order_id | status | version |
| -------- | ------ | ------- |
| order-42 | PAID   | 8       |

Outbox table

| event_id | aggregate_id | event_type | version |
| -------- | ------------ | ---------- | ------- |
| evt-901  | order-42     | OrderPaid  | 8       |

Both records commit together. A separate publisher or CDC connector forwards the outbox event.

If the transaction rolls back, neither the order update nor its outbox event becomes visible as a committed change.

If the publisher crashes after sending an event but before recording that it was sent, it may send the same event again after restarting. Therefore, the outbox prevents the database-change versus event-publication gap, but does not automatically provide exactly-once delivery to every consumer.

Consumers still need idempotency.

When to use it: When publishing business events reliably is important, especially for payments, order processing, notifications, and cross-service workflows.

One important distinction:

- CDC of business tables captures database-level changes.
- CDC of an outbox table transports application-defined business events.
- Database-native replication copies database changes according to database replication semantics.

You can combine these approaches. For example, an application writes an outbox event, CDC transports it to a stream, and a regional consumer applies it to a destination database.

### Approach D: Application-level event synchronization

In this approach, a service publishes domain events such as:

- `CustomerProfileUpdated`
- `OrderPaid`
- `InventoryReserved`
- `SubscriptionCancelled`

Other services consume the events and update their own data.

This is useful when each service owns its own data and downstream services need to maintain derived views or react to business activity.

However, an event-driven architecture does not automatically make every regional database a complete, synchronized copy of the source database. You must define which events are published, what each consumer stores, and how missing events are recovered.

For example, a regional search index may only need a subset of profile fields, while a disaster-recovery database may need a much broader set of records.

Important design rule: Choose synchronization based on the data's purpose and the guarantees you need, rather than using one mechanism indiscriminately for every data type.

## 3. The hardest problem: duplicates and idempotency

Distributed systems frequently deliver messages more than once.

Imagine this sequence:

1. India publishes event `evt-901`.
2. Europe receives it and updates the order.
3. Europe crashes before acknowledging the message.
4. The message is delivered again after recovery.

The consumer cannot safely assume that every message is delivered exactly once.

The standard solution is to make processing idempotent.

An operation is idempotent when repeating it produces the same intended final result as performing it once.

### A. Make updates idempotent

Consider an event that says:

```
{
  "event_id": "evt-901",
  "order_id": "order-42",
  "version": 8,
  "status": "PAID"
}
```

A consumer can treat this as a statement of the desired state: order `order-42` should be at version 8 with status `PAID`.

Applying the same versioned update twice need not change the result after the first successful application.

For a simple versioned record, the destination can reject older or already-applied versions:

```
INSERT INTO orders_replica (
    order_id,
    status,
    version
)
VALUES (
    'order-42',
    'PAID',
    8
)
ON CONFLICT (order_id)
DO UPDATE
SET
    status = EXCLUDED.status,
    version = EXCLUDED.version
WHERE orders_replica.version < EXCLUDED.version;
```

This is an illustrative PostgreSQL pattern. It assumes the destination version is authoritative and monotonically increases for each record. Concurrent writers or more complex version semantics need additional safeguards.

If version 8 arrives again, it does not update the record again. If version 7 arrives after version 8, it cannot overwrite the newer state.

But consider a different event:

```
{
  "event_id": "evt-902",
  "order_id": "order-42",
  "operation": "INCREMENT_REWARD_POINTS",
  "amount": 100
}
```

If you apply the event twice, the customer might receive 200 points instead of 100.

For such events, you need a deduplication mechanism, such as an inbox table that records processed event IDs in the same transaction as the business update.

### B. Use an inbox table

```
CREATE TABLE processed_events (
    event_id TEXT PRIMARY KEY,
    processed_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
```

Conceptually, the consumer performs the following in one database transaction:

1. Insert the event ID into `processed_events`.
2. If the ID already exists, treat the event as a duplicate.
3. Otherwise, apply the business update.
4. Commit both the deduplication record and the business update together.

If the transaction fails, both changes roll back. If the transaction commits and the consumer later crashes before acknowledging the message, the redelivery is recognized as a duplicate.

This only works when the deduplication record and the associated business update share the same transactional boundary. If the side effect happens in an external system, that system needs its own idempotency or deduplication strategy.

Interview takeaway: At-least-once delivery plus idempotent processing is a common and practical reliability strategy. Do not claim that a message broker alone guarantees exactly-once business effects across arbitrary systems.

## 4. Event ordering: what if changes arrive out of order?

Consider a customer profile that changes twice in India:

- Version 10: `city = Hyderabad`
- Version 11: `city = Bengaluru`

The events are generated in that order. But the network, retries, or parallel consumers can cause Europe to receive version 11 before version 10.

Without a version check, the older event might overwrite the newer value.

EuropeEvent StreamIndiaEuropeEvent StreamIndia#chatgpt-mermaid-\_r_54c\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_54c\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_54c\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_54c\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_54c\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_54c\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_54c\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_54c\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_54c\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_54c\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_54c\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_54c\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_54c\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_54c\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_54c\_ p{margin:0;}#chatgpt-mermaid-\_r_54c\_ .actor{stroke:rgb(31, 78, 148);fill:rgb(9, 23, 44);stroke-width:1;}#chatgpt-mermaid-\_r_54c\_ rect.actor.outer-path[data-look="neo"]{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_54c\_ rect.note[data-look="neo"]{stroke:rgb(58, 132, 63);fill:rgb(48, 48, 48);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_54c\_ text.actor>tspan{fill:rgb(237, 237, 237);stroke:none;}#chatgpt-mermaid-\_r_54c\_ .actor-line{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_54c\_ .innerArc{stroke-width:1.5;stroke-dasharray:none;}#chatgpt-mermaid-\_r_54c\_ .messageLine0{stroke-width:1.5;stroke-dasharray:none;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_54c\_ .messageLine1{stroke-width:1.5;stroke-dasharray:2,2;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_54c\_ [id$="-arrowhead"] path{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_54c\_ .sequenceNumber{fill:#505050;}#chatgpt-mermaid-\_r_54c\_ [id$="-sequencenumber"]{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_54c\_ [id$="-crosshead"] path{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_54c\_ .messageText{fill:rgb(237, 237, 237);stroke:none;}#chatgpt-mermaid-\_r_54c\_ .labelBox{stroke:rgba(255, 255, 255, 0.15);fill:rgb(0, 0, 0);filter:none;}#chatgpt-mermaid-\_r_54c\_ .labelText,#chatgpt-mermaid-\_r_54c\_ .labelText>tspan{fill:rgb(237, 237, 237);stroke:none;}#chatgpt-mermaid-\_r_54c\_ .loopText,#chatgpt-mermaid-\_r_54c\_ .loopText>tspan{fill:rgb(237, 237, 237);stroke:none;}#chatgpt-mermaid-\_r_54c\_ .sectionTitle,#chatgpt-mermaid-\_r_54c\_ .sectionTitle>tspan{fill:rgb(237, 237, 237);stroke:none;}#chatgpt-mermaid-\_r_54c\_ .loopLine{stroke-width:2px;stroke-dasharray:2,2;stroke:rgba(255, 255, 255, 0.15);fill:rgba(255, 255, 255, 0.15);}#chatgpt-mermaid-\_r_54c\_ .note{stroke:rgb(58, 132, 63);fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_54c\_ .noteText,#chatgpt-mermaid-\_r_54c\_ .noteText>tspan{fill:rgb(237, 237, 237);stroke:none;font-weight:normal;}#chatgpt-mermaid-\_r_54c\_ .activation0{fill:rgb(48, 48, 48);stroke:hsl(0, 0%, 8.8235294118%);}#chatgpt-mermaid-\_r_54c\_ .activation1{fill:rgb(48, 48, 48);stroke:hsl(0, 0%, 8.8235294118%);}#chatgpt-mermaid-\_r_54c\_ .activation2{fill:rgb(48, 48, 48);stroke:hsl(0, 0%, 8.8235294118%);}#chatgpt-mermaid-\_r_54c\_ .actorPopupMenu{position:absolute;}#chatgpt-mermaid-\_r_54c\_ .actorPopupMenuPanel{position:absolute;fill:rgb(9, 23, 44);box-shadow:0px 8px 16px 0px rgba(0,0,0,0.2);filter:drop-shadow(3px 5px 2px rgb(0 0 0 / 0.4));}#chatgpt-mermaid-\_r_54c\_ .actor-man circle,#chatgpt-mermaid-\_r_54c\_ line{fill:rgb(9, 23, 44);stroke-width:2px;}#chatgpt-mermaid-\_r_54c\_ g rect.rect{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_54c\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_54c\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_54c\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_54c\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_54c\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_54c\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_54c\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_54c\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_54c\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_54c\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_54c\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_54c\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_54c\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_54c\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_54c\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_54c\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_54c\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_54c\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}Reject version 10 because version 11 is already appliedVersion 10: HyderabadVersion 11: BengaluruVersion 11: BengaluruVersion 10: Hyderabad

The solution depends on what kind of ordering your application requires.

### Strategy 1: Per-record version numbers

Each update carries a monotonically increasing version for its record.

The destination only applies an event if its version is newer than the stored version.

This works well when each record has a clear authoritative writer and version sequence.

### Strategy 2: Partition by entity key

Publish all events for the same entity to the same ordered stream partition.

For example:

```
partitionKey = customerId
```

Events for customer `cust-123` are routed to the same partition, where the broker can preserve their order according to its guarantees.

This can simplify processing, but there are caveats:

- Ordering is generally scoped to a partition, not the entire stream.
- Retries and multiple producers still require attention to the broker's exact ordering guarantees.
- A very active customer can create a hot partition.
- Events for different customers may be processed concurrently.

### Strategy 3: Enforce single-writer ownership

Assign each entity or shard one authoritative writer at a time.

If India owns customer `cust-123`, Europe does not independently issue conflicting updates to that same customer. Europe can serve reads or apply replicated changes.

This reduces conflict complexity significantly.

However, a single-writer design requires a safe ownership-transfer protocol. If ownership moves from India to Europe, you must prevent the old owner from continuing to accept writes.

### Strategy 4: Use conflict resolution for genuine multi-writer systems

Sometimes different regions must accept concurrent writes to the same logical data.

For example, a globally distributed collaboration tool may accept edits in multiple regions during network interruptions.

You then need an explicit conflict model:

- Last-write-wins, with a carefully defined timestamp or ordering rule.
- Optimistic concurrency and conflict rejection.
- Field-level or domain-specific merging.
- CRDTs for data types whose operations can be merged safely.
- Coordination or consensus when the business requires a stronger ordering guarantee.

A timestamp alone is not always sufficient: clocks can differ, and a later wall-clock timestamp does not necessarily mean a causally newer update.

For payments, inventory, or financial balances, blindly using last-write-wins can silently discard meaningful business operations.

Rule of thumb: Prefer single-writer ownership where it meets your requirements. Use multi-writer conflict resolution only when its complexity is justified.

## 5. Replication lag: how far behind is the destination?

In asynchronous synchronization, the destination can be behind the source.

This difference is called replication lag or synchronization lag. It can be measured in different ways depending on the mechanism.

For example:

- Database replication: time or log position between the source and replica.
- CDC pipeline: age of the oldest unprocessed change or source-to-consumer delay.
- Event-driven consumers: time between event creation and successful application.

Imagine that the India region generates 5,000 changes per second, but the European consumer applies only 3,000 changes per second.

The backlog grows by approximately 2,000 changes per second while those rates remain constant.

A large backlog means Europe is increasingly stale and may take a long time to catch up after an outage.

### What should you monitor?

| Metric                            | What it tells you                                         |
| --------------------------------- | --------------------------------------------------------- |
| Replication lag                   | How far behind the destination is                         |
| Consumer backlog                  | How much work remains                                     |
| Oldest event age                  | How long the oldest pending change has waited             |
| Apply throughput                  | How quickly changes are being applied                     |
| Error and retry rate              | Whether processing is failing                             |
| Dead-letter count                 | Whether events are being diverted after repeated failures |
| Source log retention / disk usage | Whether a stalled consumer threatens source storage       |

Use alerts based on business requirements, not arbitrary numbers alone.

For example, a search index might tolerate a few seconds of delay, while a payment status used to authorize a shipment may require a much stronger read-after-write guarantee.

### What happens when synchronization falls behind?

A useful recovery sequence is:

1. Identify whether the source is producing changes faster than the destination can apply them.
2. Determine whether the problem is network throughput, consumer capacity, a poison message, database contention, or a schema error.
3. Restore the failed component or increase safe processing capacity.
4. Resume from the last durable checkpoint.
5. Process the backlog without violating ordering or idempotency guarantees.
6. Verify that the destination has caught up and that no required changes were skipped.

Adding more consumers is not always the right fix. If all events for one entity must be applied in order, you cannot arbitrarily parallelize those events without preserving that ordering.

Likewise, increasing throughput can overload the destination database. Recovery capacity must be designed and tested, not assumed.

## 6. Deletions, tombstones, and the danger of resurrecting data

Synchronization is not only about inserting and updating records. Deletions are equally important.

Suppose customer `cust-123` is deleted in India. Europe must learn that the record is no longer valid.

A common mistake is to physically delete the record in the source and assume every destination will somehow know about it. A destination that misses the deletion may retain the old record indefinitely.

### Use a deletion event or tombstone

A tombstone is a marker indicating that a record has been deleted.

For example:

```
{
  "event_id": "evt-950",
  "customer_id": "cust-123",
  "version": 12,
  "deleted": true
}
```

The destination records the deletion and its version.

Why keep a deletion marker instead of simply removing the row?

Imagine the destination processes these events out of order:

1. Version 12: delete customer.
2. Version 11: update customer profile.

If the destination physically removed the row without retaining any version information, it might accept version 11 and recreate the deleted record.

A tombstone preserves enough information to reject that stale update.

\#chatgpt-mermaid-\_r_550\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_550\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_550\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_550\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_550\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_550\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_550\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_550\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_550\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_550\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_550\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_550\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_550\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_550\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_550\_ p{margin:0;}#chatgpt-mermaid-\_r_550\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_550\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_550\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_550\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_550\_ .label text,#chatgpt-mermaid-\_r_550\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_550\_ .node rect,#chatgpt-mermaid-\_r_550\_ .node circle,#chatgpt-mermaid-\_r_550\_ .node ellipse,#chatgpt-mermaid-\_r_550\_ .node polygon,#chatgpt-mermaid-\_r_550\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_550\_ .rough-node .label text,#chatgpt-mermaid-\_r_550\_ .node .label text,#chatgpt-mermaid-\_r_550\_ .image-shape .label,#chatgpt-mermaid-\_r_550\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_550\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_550\_ .rough-node .label,#chatgpt-mermaid-\_r_550\_ .node .label,#chatgpt-mermaid-\_r_550\_ .image-shape .label,#chatgpt-mermaid-\_r_550\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_550\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_550\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_550\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_550\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_550\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_550\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_550\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_550\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_550\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_550\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_550\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_550\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_550\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_550\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_550\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_550\_ .icon-shape,#chatgpt-mermaid-\_r_550\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_550\_ .icon-shape p,#chatgpt-mermaid-\_r_550\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_550\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_550\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_550\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_550\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_550\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_550\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_550\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_550\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_550\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_550\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_550\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_550\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_550\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_550\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_550\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_550\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_550\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_550\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_550\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_550\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_550\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_550\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_550\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_550\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_550\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_550\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_550\_ .node rect,#chatgpt-mermaid-\_r_550\_ .node circle,#chatgpt-mermaid-\_r_550\_ .node ellipse,#chatgpt-mermaid-\_r_550\_ .node polygon,#chatgpt-mermaid-\_r_550\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_550\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_550\_ .node.mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_550\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_550\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_550\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}Version 12: Delete CustomerStore Deletion MarkerVersion 11 Update ArrivesIs incoming version newer?Reject Stale UpdateApply According to DeletionPolicyNoYes

Tombstones cannot necessarily be retained forever. You need a safe cleanup policy based on replication progress, retention guarantees, and the possibility of delayed messages or historical replay.

For privacy-related deletion requests, also consider copies in backups, caches, search indexes, analytics systems, and downstream services. A tombstone in one database does not automatically erase every other copy.

## 7. Schema evolution: what if the data format changes?

Cross-region synchronization creates a compatibility problem when the application or database schema changes.

Suppose the original event is:

```
{
  "customer_id": "cust-123",
  "name": "Asha"
}
```

A new application version adds a required `country_code` field.

If the India region starts producing events with that field before the European consumer understands it, synchronization may fail.

The reverse can also happen: a newer consumer might expect a field that older producers do not yet provide.

### Use an expand-and-contract migration

A safer rollout usually follows this sequence:

1. Expand: Add the new field or schema capability in a backward-compatible way.
2. Deploy compatible consumers: Make consumers tolerate both old and new event formats.
3. Deploy producers: Start producing the new field.
4. Backfill if needed: Populate the field for existing records.
5. Validate: Confirm that consumers and data stores have caught up.
6. Contract: Remove obsolete fields or behavior only when all relevant producers and consumers no longer depend on them.

This matters even more during regional deployments. India might be running application version 12 while Europe is temporarily running version 11.

Assume that the two regions will sometimes run different software versions during rolling deployments, outages, and recovery. Design event schemas accordingly.

For sensitive or regulated data, schema compatibility also needs to preserve data-classification and residency rules. A technically compatible event is not necessarily permitted to cross a regional boundary.

## 8. Avoid replication loops

A subtle failure can occur when data is replicated in both directions.

Suppose India sends a change to Europe. Europe applies it to its database, and its own CDC pipeline detects that update and sends it back to India.

India receives it, applies it, and publishes it again.

Without loop prevention, the same change can circulate indefinitely.

\#chatgpt-mermaid-\_r_55f\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_55f\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_55f\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_55f\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_55f\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_55f\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_55f\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_55f\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_55f\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_55f\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_55f\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_55f\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_55f\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_55f\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_55f\_ p{margin:0;}#chatgpt-mermaid-\_r_55f\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_55f\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_55f\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_55f\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_55f\_ .label text,#chatgpt-mermaid-\_r_55f\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_55f\_ .node rect,#chatgpt-mermaid-\_r_55f\_ .node circle,#chatgpt-mermaid-\_r_55f\_ .node ellipse,#chatgpt-mermaid-\_r_55f\_ .node polygon,#chatgpt-mermaid-\_r_55f\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_55f\_ .rough-node .label text,#chatgpt-mermaid-\_r_55f\_ .node .label text,#chatgpt-mermaid-\_r_55f\_ .image-shape .label,#chatgpt-mermaid-\_r_55f\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_55f\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_55f\_ .rough-node .label,#chatgpt-mermaid-\_r_55f\_ .node .label,#chatgpt-mermaid-\_r_55f\_ .image-shape .label,#chatgpt-mermaid-\_r_55f\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_55f\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_55f\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_55f\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_55f\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_55f\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_55f\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_55f\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_55f\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_55f\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_55f\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_55f\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_55f\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_55f\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_55f\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_55f\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_55f\_ .icon-shape,#chatgpt-mermaid-\_r_55f\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_55f\_ .icon-shape p,#chatgpt-mermaid-\_r_55f\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_55f\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_55f\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_55f\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_55f\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_55f\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_55f\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_55f\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_55f\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_55f\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_55f\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_55f\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_55f\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_55f\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_55f\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_55f\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_55f\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_55f\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_55f\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_55f\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_55f\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_55f\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_55f\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_55f\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_55f\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_55f\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_55f\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_55f\_ .node rect,#chatgpt-mermaid-\_r_55f\_ .node circle,#chatgpt-mermaid-\_r_55f\_ .node ellipse,#chatgpt-mermaid-\_r_55f\_ .node polygon,#chatgpt-mermaid-\_r_55f\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_55f\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_55f\_ .node.mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_55f\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_55f\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_55f\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}India DatabaseEurope DatabaseChangeReplicated Change

Possible solutions include:

- Using database-native replication mechanisms designed for the intended topology.
- Tracking the origin of each change and preventing an applied change from being republished as a new local change.
- Using event IDs, source identifiers, and versions to recognize already-applied changes.
- Defining explicit source-to-destination replication rules.

For example, an event might contain:

```
{
  "event_id": "evt-951",
  "origin_region": "IN",
  "entity_id": "cust-123",
  "version": 13
}
```

Origin metadata can help detect loops, but it is not a complete conflict-resolution strategy. If Europe legitimately accepts an independent update to the same entity, the system must distinguish that new change from a replicated copy.

For a single-writer architecture, the simplest policy is usually to replicate authoritative changes outward and avoid treating replicas as independent writers.

## 9. Recovering after a regional outage

Now combine these ideas in a realistic failure.

India is the authoritative region for a set of customers. Europe holds replicated data for recovery.

India becomes unavailable for 20 minutes. During the outage, the European copy is behind the last committed changes.

What happens when India recovers?

A reliable recovery process looks like this:

\#chatgpt-mermaid-\_r_55u\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_55u\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_55u\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_55u\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_55u\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_55u\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_55u\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_55u\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_55u\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_55u\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_55u\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_55u\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_55u\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_55u\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_55u\_ p{margin:0;}#chatgpt-mermaid-\_r_55u\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_55u\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_55u\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_55u\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_55u\_ .label text,#chatgpt-mermaid-\_r_55u\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_55u\_ .node rect,#chatgpt-mermaid-\_r_55u\_ .node circle,#chatgpt-mermaid-\_r_55u\_ .node ellipse,#chatgpt-mermaid-\_r_55u\_ .node polygon,#chatgpt-mermaid-\_r_55u\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_55u\_ .rough-node .label text,#chatgpt-mermaid-\_r_55u\_ .node .label text,#chatgpt-mermaid-\_r_55u\_ .image-shape .label,#chatgpt-mermaid-\_r_55u\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_55u\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_55u\_ .rough-node .label,#chatgpt-mermaid-\_r_55u\_ .node .label,#chatgpt-mermaid-\_r_55u\_ .image-shape .label,#chatgpt-mermaid-\_r_55u\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_55u\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_55u\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_55u\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_55u\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_55u\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_55u\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_55u\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_55u\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_55u\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_55u\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_55u\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_55u\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_55u\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_55u\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_55u\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_55u\_ .icon-shape,#chatgpt-mermaid-\_r_55u\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_55u\_ .icon-shape p,#chatgpt-mermaid-\_r_55u\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_55u\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_55u\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_55u\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_55u\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_55u\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_55u\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_55u\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_55u\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_55u\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_55u\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_55u\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_55u\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_55u\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_55u\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_55u\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_55u\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_55u\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_55u\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_55u\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_55u\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_55u\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_55u\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_55u\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_55u\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_55u\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_55u\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_55u\_ .node rect,#chatgpt-mermaid-\_r_55u\_ .node circle,#chatgpt-mermaid-\_r_55u\_ .node ellipse,#chatgpt-mermaid-\_r_55u\_ .node polygon,#chatgpt-mermaid-\_r_55u\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_55u\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_55u\_ .node.mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_55u\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_55u\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_55u\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}Detect Regional OutageFence or Confirm SourceWriter StateRestore Source andReplication ConnectivityResume from DurableCheckpointReplay Missing ChangesHandle Duplicates andOrderingMeasure Lag and ValidateDataCaught Up and Consistent?Restore Normal RoutingNoYes

The details depend on whether the original source remained intact, whether another region was promoted, and whether any writes were accepted during the outage.

Consider two cases.

Case A: India remained the only writer, but Europe temporarily lost connectivity.

India continues accepting writes. Once connectivity returns, Europe can replay missing changes from the retained replication log or event stream.

This is relatively straightforward as long as the source has retained the required history and the destination can catch up.

Case B: Europe was promoted and accepted writes while India was unavailable.

This is much harder. India and Europe might now contain different valid writes.

Before returning to the normal topology, you need to reconcile the changes, establish the authoritative state, and safely transfer ownership. You cannot blindly replay both directions and hope that the databases converge correctly.

You must also account for the possibility that the old writer is still alive but isolated. Fencing prevents it from accepting writes after its authority has been revoked.

The key lesson is that restoring connectivity is not the same as restoring consistency.

## 10. Where consistent hashing fits into synchronization

Let's return to the central topic of this course.

Suppose you have a multi-region application with thousands of tenants and several database shards per region.

A typical routing and synchronization flow is:

\#chatgpt-mermaid-\_r_56a\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_56a\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_56a\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_56a\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_56a\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_56a\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_56a\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_56a\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_56a\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_56a\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_56a\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_56a\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_56a\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_56a\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_56a\_ p{margin:0;}#chatgpt-mermaid-\_r_56a\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_56a\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_56a\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_56a\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_56a\_ .label text,#chatgpt-mermaid-\_r_56a\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_56a\_ .node rect,#chatgpt-mermaid-\_r_56a\_ .node circle,#chatgpt-mermaid-\_r_56a\_ .node ellipse,#chatgpt-mermaid-\_r_56a\_ .node polygon,#chatgpt-mermaid-\_r_56a\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_56a\_ .rough-node .label text,#chatgpt-mermaid-\_r_56a\_ .node .label text,#chatgpt-mermaid-\_r_56a\_ .image-shape .label,#chatgpt-mermaid-\_r_56a\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_56a\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_56a\_ .rough-node .label,#chatgpt-mermaid-\_r_56a\_ .node .label,#chatgpt-mermaid-\_r_56a\_ .image-shape .label,#chatgpt-mermaid-\_r_56a\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_56a\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_56a\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_56a\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_56a\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_56a\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_56a\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_56a\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_56a\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_56a\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_56a\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_56a\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_56a\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_56a\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_56a\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_56a\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_56a\_ .icon-shape,#chatgpt-mermaid-\_r_56a\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_56a\_ .icon-shape p,#chatgpt-mermaid-\_r_56a\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_56a\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_56a\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_56a\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_56a\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_56a\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_56a\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_56a\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_56a\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_56a\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_56a\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_56a\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_56a\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_56a\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_56a\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_56a\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_56a\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_56a\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_56a\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_56a\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_56a\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_56a\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_56a\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_56a\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_56a\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_56a\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_56a\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_56a\_ .node rect,#chatgpt-mermaid-\_r_56a\_ .node circle,#chatgpt-mermaid-\_r_56a\_ .node ellipse,#chatgpt-mermaid-\_r_56a\_ .node polygon,#chatgpt-mermaid-\_r_56a\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_56a\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_56a\_ .node.mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_56a\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_56a\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_56a\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}Incoming RequestResolve Residency andOwnership PolicyChoose Authoritative RegionConsistent Hashing WithinEligible ShardsIdentify Authoritative ShardCommit Business ChangeReplication / CDC / OutboxSynchronize EligibleDestinationsApply Idempotently andMonitor Lag

Consistent hashing answers:

> Which eligible shard should own this key?

The synchronization mechanism answers:

> How do committed changes reach other systems or regions?

The consistency model answers:

> Which reads and writes are allowed while copies disagree or communication fails?

The failover protocol answers:

> How do we transfer authority safely when the current owner becomes unavailable?

These are separate responsibilities. A robust design specifies each one explicitly.

For example, if a tenant is assigned to an India shard, hashing the tenant ID to a European shard does not make that European shard authoritative. Nor does adding a new shard automatically copy the tenant's existing data to it.

A shard migration still needs a controlled process: copy data, synchronize ongoing changes, catch up, fence or stop writes to the old owner, update ownership metadata, and validate the cutover.

## 11. Interview-ready summary

If asked, “How would you synchronize data across regions reliably?”, a strong answer would be:

> I would first define the source of truth and the consistency requirements for each data type. For database replicas and disaster recovery, I would consider database-native replication. For downstream systems and regional data pipelines, I would consider CDC, and for reliable business-event publication I would use a transactional outbox.
>
> Because asynchronous delivery can introduce duplicates, delays, and out-of-order events, I would use idempotent consumers, event IDs, version checks, and appropriate ordering guarantees. I would handle deletions with tombstones or equivalent durable deletion metadata, evolve event schemas compatibly, and prevent replication loops.
>
> Operationally, I would monitor replication lag, backlog, failures, and source log retention. I would test recovery from checkpoints and validate that replicas have caught up before declaring recovery complete. Finally, I would enforce data residency rules before deciding which regions are eligible to receive a copy, and keep replication separate from ownership and failover decisions.

### Key takeaways

- Database replication, CDC, outbox, and application events solve related but different problems.
- Assume messages can be delivered more than once.
- Use versioning and explicit ordering guarantees where stale updates would be dangerous.
- Model deletions, schema changes, and replay as first-class concerns.
- Monitor lag and test recovery under realistic load.
- Keep data ownership, regional eligibility, replication, and consistent hashing as distinct architectural responsibilities.

Next lesson: We'll examine how to route user requests with low latency while respecting home-region ownership, read-after-write consistency, regional health, and data residency. Type `next` to continue.




