# Chapter 2: Multi-Region Architecture Patterns

In this chapter, we will learn how to choose the right architecture when expanding an application across multiple geographic regions.

The most important decision is how each region serves requests and where authoritative writes happen. That decision influences data consistency, failover, latency, cost, and operational complexity.

We will use an e-commerce application as our running example. Customers can browse products, place orders, make payments, and track deliveries from different parts of the world.

## 2.1 The four main architectural choices

Consider these four designs:

| Pattern                                        | How it works                                                                        | Main advantage                                                                         | Main trade-off                                                                |
| ---------------------------------------------- | ----------------------------------------------------------------------------------- | -------------------------------------------------------------------------------------- | ----------------------------------------------------------------------------- |
| Active-passive                                 | One region serves production traffic; another is a standby                          | Relatively simple write ownership and recovery                                         | Failover takes time; standby costs money                                      |
| Active-active application, single write region | Multiple regions serve requests, but writes go to one authoritative database region | Better global request latency without distributed writes                               | Write latency for remote users; central write dependency                      |
| Active-active with regional write ownership    | Each region handles writes for its own users or data partition                      | Local writes and regional autonomy                                                     | Requires careful ownership, routing, and synchronization                      |
| Multi-region distributed database              | Multiple regions can potentially accept writes to shared data                       | Strong geographic availability and local-write capabilities, depending on the database | Greater complexity, cost, and sometimes consistency or transaction trade-offs |

These names are used somewhat differently across cloud providers and architectures. In particular, active-active application servers do not necessarily mean active-active database writes.

That distinction is essential.

## 2.2 Pattern 1: Active-passive architecture

This is often the most sensible first multi-region architecture when your primary objective is disaster recovery.

Region A is active. Region B is prepared to take over if Region A fails.

During normal operation, users write to the database in Region A. A copy of the data is maintained in Region B using an appropriate replication or backup mechanism.

\#chatgpt-mermaid-\_r_2fe\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_2fe\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_2fe\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_2fe\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_2fe\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2fe\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_2fe\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_2fe\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_2fe\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_2fe\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_2fe\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_2fe\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2fe\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2fe\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_2fe\_ p{margin:0;}#chatgpt-mermaid-\_r_2fe\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2fe\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2fe\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2fe\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_2fe\_ .label text,#chatgpt-mermaid-\_r_2fe\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2fe\_ .node rect,#chatgpt-mermaid-\_r_2fe\_ .node circle,#chatgpt-mermaid-\_r_2fe\_ .node ellipse,#chatgpt-mermaid-\_r_2fe\_ .node polygon,#chatgpt-mermaid-\_r_2fe\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_2fe\_ .rough-node .label text,#chatgpt-mermaid-\_r_2fe\_ .node .label text,#chatgpt-mermaid-\_r_2fe\_ .image-shape .label,#chatgpt-mermaid-\_r_2fe\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_2fe\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_2fe\_ .rough-node .label,#chatgpt-mermaid-\_r_2fe\_ .node .label,#chatgpt-mermaid-\_r_2fe\_ .image-shape .label,#chatgpt-mermaid-\_r_2fe\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_2fe\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_2fe\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2fe\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2fe\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_2fe\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_2fe\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_2fe\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_2fe\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_2fe\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_2fe\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_2fe\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2fe\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2fe\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_2fe\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2fe\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_2fe\_ .icon-shape,#chatgpt-mermaid-\_r_2fe\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_2fe\_ .icon-shape p,#chatgpt-mermaid-\_r_2fe\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_2fe\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_2fe\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_2fe\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_2fe\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_2fe\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_2fe\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_2fe\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_2fe\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_2fe\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2fe\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_2fe\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_2fe\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_2fe\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2fe\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_2fe\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_2fe\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2fe\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_2fe\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_2fe\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2fe\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_2fe\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2fe\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_2fe\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_2fe\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_2fe\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_2fe\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_2fe\_ .node rect,#chatgpt-mermaid-\_r_2fe\_ .node circle,#chatgpt-mermaid-\_r_2fe\_ .node ellipse,#chatgpt-mermaid-\_r_2fe\_ .node polygon,#chatgpt-mermaid-\_r_2fe\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_2fe\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_2fe\_ .node.mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_2fe\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_2fe\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2fe\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}UsersGlobal Traffic ManagerRegion A: Active ApplicationRegion B: StandbyApplicationRegion A: Primary DatabaseRegion B: Standby DatabaseFailover when neededCross-region replication

The diagram shows the intended normal operating arrangement. The secondary application may be running already, or it may need to be started during recovery.

### Normal operation

1. Users connect to Region A.
2. Application instances in Region A process requests.
3. Writes go to the primary database in Region A.
4. Database changes are replicated to Region B.
5. Region B remains ready for recovery.

Because Region B does not normally serve the main production workload, you can often run fewer application instances there, reducing costs.

### What happens when Region A fails?

A recovery procedure must coordinate several actions:

1. Detect that Region A is unavailable or no longer safe to use.
2. Determine whether Region B has sufficiently up-to-date data.
3. Promote the standby database if required.
4. Ensure the old primary cannot continue accepting conflicting writes.
5. Make Region B ready to serve production traffic.
6. Redirect traffic and validate that critical operations work.

The exact sequence depends on the database and cloud services. A database's automatic failover within an availability zone configuration does not necessarily perform cross-region promotion or application traffic failover.

### Four levels of standby readiness

Active-passive systems can be configured with different levels of readiness.

| Strategy     | Secondary environment                                                                                       | Recovery characteristics                       | Cost                |
| ------------ | ----------------------------------------------------------------------------------------------------------- | ---------------------------------------------- | ------------------- |
| Cold standby | Infrastructure is mostly absent; recovery may require provisioning and restoring                            | Slower recovery                                | Lowest ongoing cost |
| Pilot light  | Core recovery infrastructure and replicated data are maintained; application capacity may need provisioning | Faster than rebuilding everything from scratch | Low to moderate     |
| Warm standby | A reduced but functional application environment is running                                                 | Faster recovery with some scale-up time        | Moderate            |
| Hot standby  | A fully provisioned secondary environment is ready                                                          | Potentially very fast recovery                 | High                |

These are common industry descriptions rather than strict universal definitions. The actual RTO and RPO depend on your implementation and testing. AWS's disaster recovery guidance describes pilot light, warm standby, and active-active as distinct strategies with different cost and recovery characteristics.&#x20;

[image](https://www.google.com/s2/favicons?domain=https://docs.aws.amazon.com\&sz=32)

AWS Well-Architected Framework

+1



### Advantages

Active-passive is attractive when:

- You primarily need regional disaster recovery.
- Most writes need a single authoritative database.
- The application does not require low-latency writes for users on every continent.
- You want to avoid distributed write conflicts.
- You can tolerate a planned recovery period during a regional outage.

### Disadvantages

The standby region can be underutilized during normal operation. Recovery also depends on correctly promoting data, redirecting requests, and restoring all dependencies.

Asynchronous replication creates another limitation: the secondary database may not contain every transaction committed by the primary before the outage.

For example, a customer places an order at 10:00:00. The primary commits it, but the transaction has not yet reached the secondary when the region becomes unavailable. The recovered database might not contain that order.

That is a data recovery problem, not merely a traffic-routing problem.

Best fit: applications that need credible regional disaster recovery and can tolerate a controlled failover process.

## 2.3 Pattern 2: Active-active application servers with one write region

This pattern is often an excellent intermediate step when serving users in multiple geographic regions.

Both regions run the application continuously, but a single region remains authoritative for database writes.

\#chatgpt-mermaid-\_r_2g7\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_2g7\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_2g7\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_2g7\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_2g7\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2g7\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_2g7\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_2g7\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_2g7\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_2g7\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_2g7\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_2g7\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2g7\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2g7\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_2g7\_ p{margin:0;}#chatgpt-mermaid-\_r_2g7\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2g7\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2g7\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2g7\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_2g7\_ .label text,#chatgpt-mermaid-\_r_2g7\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2g7\_ .node rect,#chatgpt-mermaid-\_r_2g7\_ .node circle,#chatgpt-mermaid-\_r_2g7\_ .node ellipse,#chatgpt-mermaid-\_r_2g7\_ .node polygon,#chatgpt-mermaid-\_r_2g7\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_2g7\_ .rough-node .label text,#chatgpt-mermaid-\_r_2g7\_ .node .label text,#chatgpt-mermaid-\_r_2g7\_ .image-shape .label,#chatgpt-mermaid-\_r_2g7\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_2g7\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_2g7\_ .rough-node .label,#chatgpt-mermaid-\_r_2g7\_ .node .label,#chatgpt-mermaid-\_r_2g7\_ .image-shape .label,#chatgpt-mermaid-\_r_2g7\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_2g7\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_2g7\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2g7\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2g7\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_2g7\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_2g7\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_2g7\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_2g7\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_2g7\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_2g7\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_2g7\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2g7\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2g7\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_2g7\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2g7\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_2g7\_ .icon-shape,#chatgpt-mermaid-\_r_2g7\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_2g7\_ .icon-shape p,#chatgpt-mermaid-\_r_2g7\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_2g7\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_2g7\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_2g7\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_2g7\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_2g7\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_2g7\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_2g7\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_2g7\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_2g7\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2g7\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_2g7\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_2g7\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_2g7\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2g7\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_2g7\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_2g7\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2g7\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_2g7\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_2g7\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2g7\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_2g7\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2g7\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_2g7\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_2g7\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_2g7\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_2g7\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_2g7\_ .node rect,#chatgpt-mermaid-\_r_2g7\_ .node circle,#chatgpt-mermaid-\_r_2g7\_ .node ellipse,#chatgpt-mermaid-\_r_2g7\_ .node polygon,#chatgpt-mermaid-\_r_2g7\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_2g7\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_2g7\_ .node.mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_2g7\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_2g7\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2g7\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}Global UsersGlobal Traffic ManagerApplication: Region AApplication: Region BRegion A: Primary DatabaseRegion B: Read ReplicaOptional read replication

Notice what changed from the first design: Region B is no longer merely a standby. It can actively handle normal user traffic.

For example, users in Europe might be routed to the European application servers, while users in India are routed to the Indian application servers.

However, both regions write to the same authoritative database in Region A.

### How a request works

Suppose a customer in Germany updates their delivery address.

1. The global traffic manager routes the request to the application in Europe.
2. The European application validates the request.
3. The application sends the write to the authoritative database in Region A.
4. The database commits the transaction.
5. The result returns to the European application and then to the user.

This improves the network path between the European user and the application. But the database interaction still crosses regions.

If the request executes several sequential database queries, each remote interaction may add substantial latency.

### Why this pattern is useful

Many applications have a large proportion of read traffic.

Product catalogs, documentation, public profiles, and search interfaces are examples of workloads that can benefit from regional application instances and carefully designed read paths.

A regional read replica can reduce read latency, but it introduces possible replication lag.

For example:

- The user updates a profile in Region A.
- The user immediately requests the updated profile through Region B.
- The regional replica has not caught up.
- The response contains the old profile.

Possible solutions include routing reads following a write to the authoritative database, using a version or consistency token, or temporarily retaining the authoritative response. These techniques will be discussed in Chapter 4.

### The key failure mode

If Region A hosts the authoritative database and Region A fails, having active application servers in Region B does not automatically make the service fully functional.

The system still needs a database recovery or promotion procedure.

### Advantages

- Regional application instances can reduce request latency.
- Application deployment is relatively straightforward.
- A single write authority simplifies many database invariants.
- You can use both regions' compute capacity during normal operations.
- It creates a manageable path toward more sophisticated global architecture.

### Disadvantages

- Database writes may remain geographically distant.
- The write region remains a critical dependency.
- Read replicas may serve stale data.
- Regional application instances still need compatible configuration and access to required dependencies.

Best fit: global applications that benefit from regional request processing but can retain a centralized write authority.

## 2.4 Pattern 3: Active-active with regional write ownership

Now consider a different design.

Instead of sending every write to Region A, each region becomes authoritative for a designated portion of the data.

For example:

- Users assigned to India have their authoritative profiles in Region A.
- Users assigned to Europe have their authoritative profiles in Region B.
- Each region handles writes for the data it owns.
- Other regions can access owned data through an explicitly designed API or synchronized representation.

The crucial feature is that ownership is partitioned, rather than allowing every region to update the same records independently.

\#chatgpt-mermaid-\_r_2gg\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_2gg\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_2gg\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_2gg\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_2gg\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2gg\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_2gg\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_2gg\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_2gg\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_2gg\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_2gg\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_2gg\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2gg\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2gg\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_2gg\_ p{margin:0;}#chatgpt-mermaid-\_r_2gg\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2gg\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2gg\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2gg\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_2gg\_ .label text,#chatgpt-mermaid-\_r_2gg\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2gg\_ .node rect,#chatgpt-mermaid-\_r_2gg\_ .node circle,#chatgpt-mermaid-\_r_2gg\_ .node ellipse,#chatgpt-mermaid-\_r_2gg\_ .node polygon,#chatgpt-mermaid-\_r_2gg\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_2gg\_ .rough-node .label text,#chatgpt-mermaid-\_r_2gg\_ .node .label text,#chatgpt-mermaid-\_r_2gg\_ .image-shape .label,#chatgpt-mermaid-\_r_2gg\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_2gg\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_2gg\_ .rough-node .label,#chatgpt-mermaid-\_r_2gg\_ .node .label,#chatgpt-mermaid-\_r_2gg\_ .image-shape .label,#chatgpt-mermaid-\_r_2gg\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_2gg\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_2gg\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2gg\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2gg\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_2gg\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_2gg\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_2gg\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_2gg\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_2gg\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_2gg\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_2gg\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2gg\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2gg\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_2gg\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2gg\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_2gg\_ .icon-shape,#chatgpt-mermaid-\_r_2gg\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_2gg\_ .icon-shape p,#chatgpt-mermaid-\_r_2gg\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_2gg\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_2gg\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_2gg\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_2gg\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_2gg\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_2gg\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_2gg\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_2gg\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_2gg\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2gg\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_2gg\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_2gg\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_2gg\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2gg\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_2gg\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_2gg\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2gg\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_2gg\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_2gg\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2gg\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_2gg\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2gg\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_2gg\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_2gg\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_2gg\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_2gg\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_2gg\_ .node rect,#chatgpt-mermaid-\_r_2gg\_ .node circle,#chatgpt-mermaid-\_r_2gg\_ .node ellipse,#chatgpt-mermaid-\_r_2gg\_ .node polygon,#chatgpt-mermaid-\_r_2gg\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_2gg\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_2gg\_ .node.mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_2gg\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_2gg\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2gg\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}Global UsersGlobal Routing andOwnership DirectoryRegion A: ApplicationRegion B: ApplicationRegion A: Authoritative DataRegion B: Authoritative DataReplicated events or selecteddata

### Example: regional customer ownership

Suppose a customer creates an account while living in India.

Their profile is assigned to Region A, which remains its authoritative owner. Their profile updates are written there.

If that customer travels to Europe, their requests can be served by European application servers, but the profile remains owned by Region A unless a formal migration changes its ownership.

This distinction matters because the region serving a request does not have to be the region that owns the data.

You need a routing mechanism that can answer questions such as:

- Which region owns this customer?
- Is this write allowed in the receiving region?
- Where should the application send the write?
- Which data may be cached or replicated elsewhere?
- What happens if the owning region becomes unavailable?

### How should ownership be assigned?

Several strategies are possible.

By tenant: all authoritative data for a customer organization belongs to a designated region.

By user: individual users have a home region.

By geographic jurisdiction: data is assigned to a region according to residency or legal requirements.

By business partition: orders, inventory, or other entities are assigned to the region responsible for that particular domain.

Tenant or business-partition ownership is often easier to reason about than assigning ownership independently to arbitrary records. Related records can be kept together, reducing cross-region transactions.

### What if a user moves between regions?

Suppose a user whose data is owned by Region A travels to Europe.

Changing their traffic route does not change their data ownership. The application can continue serving reads and forwarding authoritative writes to Region A.

Later, the business might decide to migrate the user's data to Region B because their permanent residence has changed.

That migration needs a controlled ownership-transfer process:

1. Determine which records and dependencies must move.
2. Copy the relevant data.
3. Track changes made while the copy is in progress.
4. Synchronize the remaining changes.
5. Establish a clear point at which write ownership transfers.
6. Prevent the old region from accepting new writes for the migrated data.
7. Verify the new owner and update routing.
8. Retain or delete the old copy according to the relevant retention and compliance requirements.

Simply copying the records and changing a routing table can create a period in which both regions accept writes, resulting in conflicting data.

### Advantages

- Writes for owned data can be processed locally.
- Data can be isolated by tenant or jurisdiction.
- Regions can have more independent failure boundaries.
- Cross-region synchronization can be limited to data that genuinely needs to be shared.

### Disadvantages

- Routing and ownership become important system-level concerns.
- Global queries become more difficult.
- Moving users or tenants requires a migration protocol.
- Cross-region workflows may need asynchronous coordination.
- A region's outage may prevent access to the data it owns unless ownership can be safely transferred or recovered.

Best fit: multi-tenant platforms, geographically partitioned workloads, and applications with requirements for regional data ownership.

## 2.5 Pattern 4: Multi-region database with writes in multiple regions

The previous pattern partitions data ownership. But suppose a business requirement says that multiple regions must be able to update the same logical dataset, and potentially the same records.

For example, users anywhere in the world might update a shared project, modify a collaborative document, or reserve globally shared inventory.

You may consider a database architecture that supports writes in multiple regions.

There are two important approaches, and they should not be confused.

### Approach A: Multi-writer replication

Multiple database nodes accept writes, then replicate changes between themselves.

\#chatgpt-mermaid-\_r_2gp\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_2gp\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_2gp\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_2gp\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_2gp\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2gp\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_2gp\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_2gp\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_2gp\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_2gp\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_2gp\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_2gp\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2gp\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2gp\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_2gp\_ p{margin:0;}#chatgpt-mermaid-\_r_2gp\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2gp\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2gp\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2gp\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_2gp\_ .label text,#chatgpt-mermaid-\_r_2gp\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2gp\_ .node rect,#chatgpt-mermaid-\_r_2gp\_ .node circle,#chatgpt-mermaid-\_r_2gp\_ .node ellipse,#chatgpt-mermaid-\_r_2gp\_ .node polygon,#chatgpt-mermaid-\_r_2gp\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_2gp\_ .rough-node .label text,#chatgpt-mermaid-\_r_2gp\_ .node .label text,#chatgpt-mermaid-\_r_2gp\_ .image-shape .label,#chatgpt-mermaid-\_r_2gp\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_2gp\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_2gp\_ .rough-node .label,#chatgpt-mermaid-\_r_2gp\_ .node .label,#chatgpt-mermaid-\_r_2gp\_ .image-shape .label,#chatgpt-mermaid-\_r_2gp\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_2gp\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_2gp\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2gp\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2gp\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_2gp\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_2gp\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_2gp\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_2gp\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_2gp\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_2gp\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_2gp\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2gp\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2gp\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_2gp\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2gp\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_2gp\_ .icon-shape,#chatgpt-mermaid-\_r_2gp\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_2gp\_ .icon-shape p,#chatgpt-mermaid-\_r_2gp\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_2gp\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_2gp\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_2gp\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_2gp\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_2gp\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_2gp\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_2gp\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_2gp\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_2gp\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2gp\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_2gp\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_2gp\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_2gp\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2gp\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_2gp\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_2gp\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2gp\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_2gp\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_2gp\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2gp\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_2gp\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2gp\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_2gp\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_2gp\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_2gp\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_2gp\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_2gp\_ .node rect,#chatgpt-mermaid-\_r_2gp\_ .node circle,#chatgpt-mermaid-\_r_2gp\_ .node ellipse,#chatgpt-mermaid-\_r_2gp\_ .node polygon,#chatgpt-mermaid-\_r_2gp\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_2gp\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_2gp\_ .node.mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_2gp\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_2gp\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2gp\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}Application: Region AWritable Database AApplication: Region BWritable Database BBidirectional replication

Imagine two users concurrently update the same customer record.

Region A receives:

```
{
  "customerId": "C100",
  "address": "Mumbai"
}
```

Region B receives:

```
{
  "customerId": "C100",
  "address": "Berlin"
}
```

Both regions might initially accept their updates. When the changes synchronize, the system must decide which value becomes authoritative.

Possible conflict-resolution strategies include:

- Rejecting one conflicting update.
- Applying a domain-specific merge rule.
- Using versions or optimistic concurrency control.
- Assigning records to a single writer despite multiple writable regions.
- Selecting a winner according to a documented conflict-resolution policy.

A simple last-write-wins rule can resolve certain conflicts technically, but it is not necessarily correct for business data. Overwriting a customer's latest address, inventory reservation, or financial transaction merely because one timestamp is later can violate business expectations.

Some databases provide built-in conflict handling. That does not eliminate the need to choose conflict semantics that make sense for the application.

### Approach B: Strongly consistent distributed database

Instead of allowing conflicting updates to independently commit and then reconciling them, a distributed database can coordinate writes using mechanisms such as consensus, replication quorums, and transaction protocols.

Conceptually:

\#chatgpt-mermaid-\_r_2he\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_2he\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_2he\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_2he\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_2he\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2he\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_2he\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_2he\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_2he\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_2he\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_2he\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_2he\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2he\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2he\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_2he\_ p{margin:0;}#chatgpt-mermaid-\_r_2he\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2he\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2he\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2he\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_2he\_ .label text,#chatgpt-mermaid-\_r_2he\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2he\_ .node rect,#chatgpt-mermaid-\_r_2he\_ .node circle,#chatgpt-mermaid-\_r_2he\_ .node ellipse,#chatgpt-mermaid-\_r_2he\_ .node polygon,#chatgpt-mermaid-\_r_2he\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_2he\_ .rough-node .label text,#chatgpt-mermaid-\_r_2he\_ .node .label text,#chatgpt-mermaid-\_r_2he\_ .image-shape .label,#chatgpt-mermaid-\_r_2he\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_2he\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_2he\_ .rough-node .label,#chatgpt-mermaid-\_r_2he\_ .node .label,#chatgpt-mermaid-\_r_2he\_ .image-shape .label,#chatgpt-mermaid-\_r_2he\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_2he\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_2he\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2he\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2he\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_2he\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_2he\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_2he\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_2he\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_2he\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_2he\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_2he\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2he\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2he\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_2he\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2he\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_2he\_ .icon-shape,#chatgpt-mermaid-\_r_2he\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_2he\_ .icon-shape p,#chatgpt-mermaid-\_r_2he\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_2he\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_2he\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_2he\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_2he\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_2he\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_2he\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_2he\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_2he\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_2he\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2he\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_2he\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_2he\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_2he\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2he\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_2he\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_2he\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2he\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_2he\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_2he\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2he\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_2he\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2he\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_2he\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_2he\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_2he\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_2he\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_2he\_ .node rect,#chatgpt-mermaid-\_r_2he\_ .node circle,#chatgpt-mermaid-\_r_2he\_ .node ellipse,#chatgpt-mermaid-\_r_2he\_ .node polygon,#chatgpt-mermaid-\_r_2he\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_2he\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_2he\_ .node.mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_2he\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_2he\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2he\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}User in Region ADistributed DatabaseUser in Region BDatabase Node: Region ADatabase Node: Region BDatabase Node: Region C

The diagram is conceptual: actual database replication and quorum topologies differ.

With an appropriately configured strongly consistent database, concurrent transactions can be ordered so that clients observe a consistent logical history. However, the exact guarantees depend on the database's transaction model and configuration.

There is a trade-off.

To establish sufficiently strong guarantees, a write may have to wait for coordination across the network. If the network is slow or a necessary quorum is unavailable, writes may become slower or temporarily unavailable.

Google Cloud's database and disaster recovery guidance discusses the relationship between cross-region replication, latency, and consistency.&#x20;

[image](https://www.google.com/s2/favicons?domain=https://docs.cloud.google.com\&sz=32)

Google Cloud Documentation

+1



### Why this is difficult for a PostgreSQL application

Suppose your existing application uses PostgreSQL transactions to create an order:

```
BEGIN

1. Insert order
2. Reserve inventory
3. Insert payment record
4. Commit

COMMIT
```

In your current single-region application, these operations might participate in one database transaction.

Once the authoritative data is distributed across independent regional databases, a single local transaction can no longer automatically provide atomicity across both databases.

You may need distributed transactions, data co-location, workflow orchestration, or a saga pattern, depending on the requirements.

This is why adding a second writable PostgreSQL instance is not, by itself, a safe way to create global multi-writer consistency.

### When should you use this pattern?

Consider it when the business genuinely requires global writes or geographically distributed transactional behavior and the supported guarantees fit your use case.

Do not use it merely because a multi-writer database sounds more highly available.

Key rule: first decide what happens when two regions make conflicting changes. Only then choose a database technology.

## 2.6 Pattern 5: Regional cells or isolated regional stacks

A cell-based architecture divides a system into relatively independent units, each capable of serving a subset of the workload.

A cell might consist of an application deployment, a database or database partition, regional caches, background workers, and supporting services.

For a multi-region system, you can run cells in different regions.

\#chatgpt-mermaid-\_r_2i4\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_2i4\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_2i4\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_2i4\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_2i4\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2i4\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_2i4\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_2i4\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_2i4\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_2i4\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_2i4\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_2i4\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2i4\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2i4\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_2i4\_ p{margin:0;}#chatgpt-mermaid-\_r_2i4\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2i4\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2i4\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2i4\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_2i4\_ .label text,#chatgpt-mermaid-\_r_2i4\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2i4\_ .node rect,#chatgpt-mermaid-\_r_2i4\_ .node circle,#chatgpt-mermaid-\_r_2i4\_ .node ellipse,#chatgpt-mermaid-\_r_2i4\_ .node polygon,#chatgpt-mermaid-\_r_2i4\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_2i4\_ .rough-node .label text,#chatgpt-mermaid-\_r_2i4\_ .node .label text,#chatgpt-mermaid-\_r_2i4\_ .image-shape .label,#chatgpt-mermaid-\_r_2i4\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_2i4\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_2i4\_ .rough-node .label,#chatgpt-mermaid-\_r_2i4\_ .node .label,#chatgpt-mermaid-\_r_2i4\_ .image-shape .label,#chatgpt-mermaid-\_r_2i4\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_2i4\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_2i4\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2i4\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2i4\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_2i4\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_2i4\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_2i4\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_2i4\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_2i4\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_2i4\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_2i4\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2i4\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2i4\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_2i4\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2i4\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_2i4\_ .icon-shape,#chatgpt-mermaid-\_r_2i4\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_2i4\_ .icon-shape p,#chatgpt-mermaid-\_r_2i4\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_2i4\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_2i4\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_2i4\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_2i4\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_2i4\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_2i4\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_2i4\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_2i4\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_2i4\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2i4\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_2i4\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_2i4\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_2i4\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2i4\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_2i4\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_2i4\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2i4\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_2i4\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_2i4\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2i4\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_2i4\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2i4\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_2i4\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_2i4\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_2i4\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_2i4\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_2i4\_ .node rect,#chatgpt-mermaid-\_r_2i4\_ .node circle,#chatgpt-mermaid-\_r_2i4\_ .node ellipse,#chatgpt-mermaid-\_r_2i4\_ .node polygon,#chatgpt-mermaid-\_r_2i4\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_2i4\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_2i4\_ .node.mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_2i4\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_2i4\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2i4\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}Region CRegion BRegion ACell CCell C DatabaseCell BCell B DatabaseCell ACell A DatabaseUsersGlobal Router and CellDirectory

This diagram represents logical isolation; a cell does not necessarily need its own entire physical database cluster. Some implementations use separate databases, while others partition a shared service.

### Why use cells?

Suppose your application hosts 10,000 customer organizations.

Instead of every tenant sharing the same application deployment and database failure domain, you assign groups of tenants to different cells.

If Cell A becomes overloaded or encounters a serious software defect, only its assigned workload should be affected, assuming dependencies and routing respect the cell boundaries.

Similarly, placing cells in different regions can limit the impact of a regional outage.

### Cell-based versus regional write ownership

These approaches are related, but not identical.

- Regional write ownership determines which region is authoritative for particular data.
- Cell-based architecture determines how workload and failure domains are partitioned.

You can use both together: each tenant belongs to a cell, and each cell has an authoritative data store in its designated region.

### Advantages

- Limits the blast radius of many failures.
- Supports gradual scaling by adding cells.
- Can simplify capacity management for individual customer groups.
- Can support data isolation and residency requirements.
- Reduces the scope of some migrations and operational incidents.

### Disadvantages

- Routing needs to identify the correct cell.
- Global reporting and cross-tenant queries become harder.
- Schema migrations and deployments must work across multiple cells.
- Shared dependencies can undermine isolation if they remain centralized.
- Moving a tenant between cells requires a migration procedure.

Best fit: high-scale SaaS platforms, systems serving many independent tenants, and applications that need stronger fault isolation.

## 2.7 Pattern 6: Hybrid architecture

In practice, mature applications frequently combine multiple patterns rather than forcing every component into the same architecture.

Imagine an e-commerce platform.

Product images can be distributed globally. Product details can be cached or read from regional replicas. Orders and payments need clear transactional authority. Background jobs need durable processing and deduplication. Customer data might have regional residency constraints.

A sensible architecture could look like this:

\#chatgpt-mermaid-\_r_2id\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_2id\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_2id\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_2id\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_2id\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2id\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_2id\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_2id\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_2id\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_2id\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_2id\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_2id\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2id\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2id\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_2id\_ p{margin:0;}#chatgpt-mermaid-\_r_2id\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2id\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2id\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2id\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_2id\_ .label text,#chatgpt-mermaid-\_r_2id\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2id\_ .node rect,#chatgpt-mermaid-\_r_2id\_ .node circle,#chatgpt-mermaid-\_r_2id\_ .node ellipse,#chatgpt-mermaid-\_r_2id\_ .node polygon,#chatgpt-mermaid-\_r_2id\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_2id\_ .rough-node .label text,#chatgpt-mermaid-\_r_2id\_ .node .label text,#chatgpt-mermaid-\_r_2id\_ .image-shape .label,#chatgpt-mermaid-\_r_2id\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_2id\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_2id\_ .rough-node .label,#chatgpt-mermaid-\_r_2id\_ .node .label,#chatgpt-mermaid-\_r_2id\_ .image-shape .label,#chatgpt-mermaid-\_r_2id\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_2id\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_2id\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2id\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2id\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_2id\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_2id\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_2id\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_2id\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_2id\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_2id\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_2id\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2id\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2id\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_2id\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2id\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_2id\_ .icon-shape,#chatgpt-mermaid-\_r_2id\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_2id\_ .icon-shape p,#chatgpt-mermaid-\_r_2id\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_2id\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_2id\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_2id\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_2id\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_2id\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_2id\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_2id\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_2id\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_2id\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2id\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_2id\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_2id\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_2id\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2id\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_2id\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_2id\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2id\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_2id\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_2id\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2id\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_2id\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2id\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_2id\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_2id\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_2id\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_2id\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_2id\_ .node rect,#chatgpt-mermaid-\_r_2id\_ .node circle,#chatgpt-mermaid-\_r_2id\_ .node ellipse,#chatgpt-mermaid-\_r_2id\_ .node polygon,#chatgpt-mermaid-\_r_2id\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_2id\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_2id\_ .node.mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_2id\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_2id\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2id\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}Global UsersGlobal CDNGlobal Traffic ManagerRegion A ApplicationRegion B ApplicationAuthoritative TransactionDatabaseRegional Read ReplicaRegional Work Queue ARegional Work Queue BWorkers AWorkers BReplicated Static AssetsReplicated data

This is an illustrative hybrid, not a prescription to put every component in a shared write region.

For example, a production implementation might instead use separate authoritative databases for different data jurisdictions or tenant groups. That choice depends on the application's transaction requirements and compliance constraints.

The key is to choose each component's operating model according to what it needs.

| Component                      | Possible design                                                                      |
| ------------------------------ | ------------------------------------------------------------------------------------ |
| Static assets and images       | CDN with suitable origin replication                                                 |
| Read-heavy product information | Regional caching and/or replicated reads                                             |
| Orders and payments            | One authoritative write path per transaction domain, with clearly defined invariants |
| Customer data                  | Regional ownership if residency requirements demand it                               |
| Background processing          | Regional queues and workers, with idempotency and controlled job ownership           |
| Disaster recovery              | Replication or backup strategy suited to each data store                             |
| Analytics                      | Regional ingestion and controlled aggregation, subject to data-transfer rules        |

### An important detail about queues

A regional message queue is not automatically a globally replicated queue.

For example, if an application uses regional SQS queues, deploying workers and queues in two regions does not automatically synchronize their messages or ensure that a job is executed only once globally.

You must define how work is assigned, how a job is recovered after a regional outage, and whether duplicate execution is possible.

For critical processing, idempotency and durable business records are essential.

### The main lesson

Do not require every component to follow one global topology.

The CDN, read model, transaction database, message queue, and customer records may all have different resilience, consistency, latency, and residency requirements.

## 2.8 Compare the patterns side by side

Use this table when deciding which architecture fits your application.

| Dimension              | Active-passive                             | Active-active app, single write region                     | Regional write ownership            | Multi-writer database                              | Regional cells                           |
| ---------------------- | ------------------------------------------ | ---------------------------------------------------------- | ----------------------------------- | -------------------------------------------------- | ---------------------------------------- |
| Regional traffic       | Normally one region                        | Multiple regions                                           | Multiple regions                    | Multiple regions                                   | Multiple cells                           |
| Write authority        | One active primary                         | One primary database                                       | Assigned per data partition         | Multiple writers or coordinated distributed writes | Depends on the cell's data design        |
| Local write latency    | Good in primary region                     | Potentially poor outside primary                           | Good for locally owned data         | Depends on database coordination                   | Good when data is owned locally          |
| Failover complexity    | Moderate                                   | Moderate                                                   | High                                | High                                               | Depends on ownership and dependencies    |
| Data consistency       | Relatively straightforward with one writer | Straightforward at the primary; replica reads may be stale | Manageable if ownership is enforced | Depends on the database and conflict model         | Depends on the cell's data design        |
| Operational complexity | Low to moderate                            | Moderate                                                   | High                                | Very high in many implementations                  | Moderate to high                         |
| Typical use case       | Regional disaster recovery                 | Global reads and regional application serving              | Regionally partitioned data         | Shared multi-region writes                         | Large SaaS platforms and fault isolation |

These are relative tendencies, not guarantees. For example, a highly automated active-passive deployment can be operationally simpler than a poorly designed single-region application.

## 2.9 How to choose the right pattern

Rather than starting with a particular database or cloud service, work through these decisions in order.

### Decision 1: What is the main business requirement?

If the requirement is primarily regional disaster recovery, start by considering active-passive.

If it is primarily global request latency, consider regional application servers and caching, while keeping writes centralized if that meets the requirements.

If you need regional write performance and data isolation, consider regional ownership.

If multiple regions genuinely need to modify the same logical data concurrently, evaluate multi-writer or strongly consistent distributed database designs.

If your main concern is isolating large numbers of independent tenants, consider cells.

### Decision 2: Can the application tolerate a remote database write?

Suppose the application servers are deployed in both India and Europe, while the authoritative PostgreSQL database lives in India.

Ask how many database round trips occur during a typical request.

A request that performs one remote query may be manageable. A request with numerous sequential database queries, each waiting on the previous one, can accumulate significant latency.

Before introducing distributed writes, consider reducing unnecessary round trips, improving queries and connection management, using suitable caching, and redesigning the request path.

### Decision 3: What happens if the owning region disappears?

For each proposed design, ask:

- Where does the authoritative data live?
- Is another usable copy available?
- Can a replacement region safely accept writes?
- How much data might be missing?
- Can the old primary return and accept writes at the same time?
- Can the application recover without a human manually changing multiple services?

An architecture is not production-ready merely because it has replicas. It must have a tested recovery procedure.

### Decision 4: Does data residency restrict replication?

Suppose customer records belonging to certain European residents must remain within an approved geographic boundary under the organization's applicable legal obligations.

Replicating those records indiscriminately into a region outside that boundary may be incompatible with the organization's legal basis, transfer arrangements, or other requirements.

You might instead use separate regional databases, regional cells, or a design that transfers only permitted information between regions.

This decision can constrain which architecture patterns are viable before you even evaluate their performance.

### Decision 5: Can your team operate the architecture?

An active-active system requires more than two deployments.

Your team must be able to monitor replication, investigate divergent state, manage regional releases, handle partial network failures, recover jobs, test outages, and understand the behavior of every critical dependency.

If the team cannot operate those capabilities reliably, a simpler active-passive design may deliver a better real-world availability outcome.

AWS's Well-Architected guidance similarly treats multi-region active-active as a substantially more operationally complex strategy that should be justified by business requirements.&#x20;

[image](https://www.google.com/s2/favicons?domain=https://docs.aws.amazon.com\&sz=32)

AWS Well-Architected Framework

+1



## 2.10 Common design mistakes

### Mistake 1: Treating active-active application instances as active-active data

Two regions serving requests do not make their databases globally writable.

Write ownership must be explicitly defined.

### Mistake 2: Automatically routing users to the nearest region

The geographically nearest region might not own the user's data, have the correct permissions, or be authorized to process that data.

Geographic proximity is only one routing input.

### Mistake 3: Assuming replicas eliminate data loss

Asynchronous replicas can lag behind the primary. Replication is useful for availability and synchronization, but it is not a substitute for recoverable backups.

AWS explicitly distinguishes replicated data from point-in-time recovery and warns that replication alone does not protect against all corruption or destruction scenarios.&#x20;

[image](https://www.google.com/s2/favicons?domain=https://docs.aws.amazon.com\&sz=32)

AWS Well-Architected Framework



### Mistake 4: Ignoring regional dependencies

Your application may be duplicated while authentication, secrets, encryption keys, queues, file storage, or third-party integrations remain unavailable during a regional incident.

Document the complete dependency graph.

### Mistake 5: Allowing two primaries to accept writes accidentally

Suppose Region A fails and Region B is promoted. Then Region A starts recovering, but its database is still writable.

Now both regions may accept writes to the same logical data.

This is a split-brain risk. A safe recovery procedure must establish authoritative write ownership and prevent the old primary from accepting conflicting writes before it rejoins.

### Mistake 6: Deploying the same capacity in each region without testing a regional failure

If each region handles 50% of normal traffic, the surviving region must be capable of handling the full load when its peer fails—or you must deliberately define which functions will be degraded.

A design that works under normal load may collapse immediately after failover.

### Mistake 7: Assuming failback is simply the reverse of failover

After Region B becomes primary, Region A may contain stale data. Its databases, queues, and other stateful resources need to be reconciled before it can safely resume service.

Failback is a separate recovery operation that must be designed and tested.

## 2.11 A practical recommendation for an existing Spring Boot and PostgreSQL application

Suppose you already run a production system using Spring Boot, PostgreSQL, Redis, background workers, and object storage in one region.

You want to support users in another geographic region and withstand a regional outage.

A sensible initial target could be:

1. Keep PostgreSQL writes in one authoritative region.
2. Run the application in a second region, either as a standby or as an active regional deployment depending on the latency requirement.
3. Configure an appropriate cross-region database replication or recovery strategy.
4. Configure regional cache instances and define how they are invalidated or rebuilt.
5. Configure object storage replication and verify that object access works independently of the original region.
6. Design queue recovery, job ownership, retries, and idempotency explicitly.
7. Introduce global traffic routing with health checks and tested failover behavior.
8. Automate infrastructure and application deployment into both regions.
9. Test regional outage, recovery, and failback scenarios.

This achieves meaningful progress without immediately requiring multi-writer databases, global transactions, or complex conflict-resolution systems.

If regional data residency later becomes a hard requirement, you can introduce regional ownership or separate cells for the relevant data instead of redesigning every component indiscriminately.

## 2.12 Chapter 2: Final mental model

Remember these distinctions:

- Active-passive: one region serves the primary workload; another is prepared to recover it.
- Active-active application, centralized writes: multiple regions serve traffic, but a designated database remains authoritative for writes.
- Regional write ownership: multiple regions accept writes for the data they own.
- Multi-writer or distributed database: multiple regions can write, either through conflict-handling replication or coordinated consistency mechanisms.
- Regional cells: workloads are divided into isolated units to limit failures and improve scale.
- Hybrid architecture: different components use different patterns according to their requirements.

The most important takeaway is that you do not need globally writable databases merely to serve global users.

Start with the least complex architecture that satisfies the business requirements. Add regional write ownership or distributed database coordination only when the latency, availability, or data isolation requirements justify that complexity.

In Chapter 3, we will look closely at database replication: primary databases, read replicas, synchronous versus asynchronous replication, cross-region PostgreSQL replication options, database promotion, and why replication and backups solve different problems.

