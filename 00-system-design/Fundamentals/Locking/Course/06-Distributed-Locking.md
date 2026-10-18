# Chapter 6: Distributed Locking — From Basics to Advanced

In Chapter 5, we learned how to implement Redis locks using `SET NX PX`, ownership tokens, safe release, lease renewal, and fencing tokens.

Now let's step back and understand the bigger system design problem:

How do we coordinate multiple machines when failures, network delays, and partial communication are possible?

This is where distributed locking becomes more than just setting a key in Redis.

## 1. What is distributed locking?

A distributed lock is a coordination mechanism that allows multiple independent processes or machines to agree on who may perform a particular operation at a given time.

Imagine an application deployed across three servers:

\#chatgpt-mermaid-\_r_3ta\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_3ta\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_3ta\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_3ta\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_3ta\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3ta\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_3ta\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_3ta\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_3ta\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_3ta\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_3ta\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_3ta\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3ta\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3ta\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_3ta\_ p{margin:0;}#chatgpt-mermaid-\_r_3ta\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3ta\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3ta\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3ta\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_3ta\_ .label text,#chatgpt-mermaid-\_r_3ta\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3ta\_ .node rect,#chatgpt-mermaid-\_r_3ta\_ .node circle,#chatgpt-mermaid-\_r_3ta\_ .node ellipse,#chatgpt-mermaid-\_r_3ta\_ .node polygon,#chatgpt-mermaid-\_r_3ta\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_3ta\_ .rough-node .label text,#chatgpt-mermaid-\_r_3ta\_ .node .label text,#chatgpt-mermaid-\_r_3ta\_ .image-shape .label,#chatgpt-mermaid-\_r_3ta\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_3ta\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_3ta\_ .rough-node .label,#chatgpt-mermaid-\_r_3ta\_ .node .label,#chatgpt-mermaid-\_r_3ta\_ .image-shape .label,#chatgpt-mermaid-\_r_3ta\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_3ta\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_3ta\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3ta\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3ta\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_3ta\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_3ta\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_3ta\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_3ta\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_3ta\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_3ta\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_3ta\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3ta\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3ta\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_3ta\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3ta\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_3ta\_ .icon-shape,#chatgpt-mermaid-\_r_3ta\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_3ta\_ .icon-shape p,#chatgpt-mermaid-\_r_3ta\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_3ta\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_3ta\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_3ta\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_3ta\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_3ta\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_3ta\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_3ta\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_3ta\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_3ta\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3ta\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_3ta\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_3ta\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_3ta\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3ta\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_3ta\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_3ta\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3ta\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_3ta\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_3ta\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3ta\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_3ta\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3ta\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_3ta\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_3ta\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_3ta\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_3ta\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_3ta\_ .node rect,#chatgpt-mermaid-\_r_3ta\_ .node circle,#chatgpt-mermaid-\_r_3ta\_ .node ellipse,#chatgpt-mermaid-\_r_3ta\_ .node polygon,#chatgpt-mermaid-\_r_3ta\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_3ta\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_3ta\_ .node.mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_3ta\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_3ta\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3ta\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}Application Server AShared coordination serviceApplication Server BApplication Server CProtected resource

All three servers might try to update the same resource. A distributed lock attempts to grant ownership to one contender so the others wait, retry, or skip the operation.

Examples include:

- Only one worker should run a particular scheduled task.
- Only one process should perform a database migration.
- Only one instance should initiate a cache rebuild.
- Multiple services need to coordinate work on a shared resource.
- One service instance should act as a leader for a particular responsibility.

### Why isn't a local lock enough?

Consider Java's `synchronized`:

```
public synchronized void refreshCache() {
    // Refresh cache
}
```

This synchronizes threads that use the same monitor in the same JVM. It does not coordinate independent JVMs.

If three application instances each have their own copy of this code, all three can enter their respective synchronized methods simultaneously.

A distributed lock requires shared coordination outside any single process's memory.

### The important distinction

A distributed lock coordinates ownership, but the protected resource must still enforce the rules required for correctness.

For example, preventing two workers from trying to book a seat is not as strong as ensuring the database cannot accept two bookings for the same seat.

Keep that distinction in mind throughout this chapter.

## 2. Why distributed locking is difficult

In a single process, a thread can acquire a lock and release it when finished. Other threads can observe the same lock state through shared memory and synchronization primitives.

In a distributed system, clients communicate over a network. A client might be alive but unreachable, paused, delayed, or unaware that its ownership has expired.

Consider this scenario:

Protected resourceClient BLock serviceClient AProtected resourceClient BLock serviceClient A#chatgpt-mermaid-\_r_3tq\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_3tq\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_3tq\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_3tq\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_3tq\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3tq\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_3tq\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_3tq\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_3tq\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_3tq\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_3tq\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_3tq\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3tq\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3tq\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_3tq\_ p{margin:0;}#chatgpt-mermaid-\_r_3tq\_ .actor{stroke:rgb(31, 78, 148);fill:rgb(9, 23, 44);stroke-width:1;}#chatgpt-mermaid-\_r_3tq\_ rect.actor.outer-path[data-look="neo"]{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3tq\_ rect.note[data-look="neo"]{stroke:rgb(58, 132, 63);fill:rgb(48, 48, 48);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3tq\_ text.actor>tspan{fill:rgb(237, 237, 237);stroke:none;}#chatgpt-mermaid-\_r_3tq\_ .actor-line{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3tq\_ .innerArc{stroke-width:1.5;stroke-dasharray:none;}#chatgpt-mermaid-\_r_3tq\_ .messageLine0{stroke-width:1.5;stroke-dasharray:none;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3tq\_ .messageLine1{stroke-width:1.5;stroke-dasharray:2,2;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3tq\_ [id$="-arrowhead"] path{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3tq\_ .sequenceNumber{fill:#505050;}#chatgpt-mermaid-\_r_3tq\_ [id$="-sequencenumber"]{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3tq\_ [id$="-crosshead"] path{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3tq\_ .messageText{fill:rgb(237, 237, 237);stroke:none;}#chatgpt-mermaid-\_r_3tq\_ .labelBox{stroke:rgba(255, 255, 255, 0.15);fill:rgb(0, 0, 0);filter:none;}#chatgpt-mermaid-\_r_3tq\_ .labelText,#chatgpt-mermaid-\_r_3tq\_ .labelText>tspan{fill:rgb(237, 237, 237);stroke:none;}#chatgpt-mermaid-\_r_3tq\_ .loopText,#chatgpt-mermaid-\_r_3tq\_ .loopText>tspan{fill:rgb(237, 237, 237);stroke:none;}#chatgpt-mermaid-\_r_3tq\_ .sectionTitle,#chatgpt-mermaid-\_r_3tq\_ .sectionTitle>tspan{fill:rgb(237, 237, 237);stroke:none;}#chatgpt-mermaid-\_r_3tq\_ .loopLine{stroke-width:2px;stroke-dasharray:2,2;stroke:rgba(255, 255, 255, 0.15);fill:rgba(255, 255, 255, 0.15);}#chatgpt-mermaid-\_r_3tq\_ .note{stroke:rgb(58, 132, 63);fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_3tq\_ .noteText,#chatgpt-mermaid-\_r_3tq\_ .noteText>tspan{fill:rgb(237, 237, 237);stroke:none;font-weight:normal;}#chatgpt-mermaid-\_r_3tq\_ .activation0{fill:rgb(48, 48, 48);stroke:hsl(0, 0%, 8.8235294118%);}#chatgpt-mermaid-\_r_3tq\_ .activation1{fill:rgb(48, 48, 48);stroke:hsl(0, 0%, 8.8235294118%);}#chatgpt-mermaid-\_r_3tq\_ .activation2{fill:rgb(48, 48, 48);stroke:hsl(0, 0%, 8.8235294118%);}#chatgpt-mermaid-\_r_3tq\_ .actorPopupMenu{position:absolute;}#chatgpt-mermaid-\_r_3tq\_ .actorPopupMenuPanel{position:absolute;fill:rgb(9, 23, 44);box-shadow:0px 8px 16px 0px rgba(0,0,0,0.2);filter:drop-shadow(3px 5px 2px rgb(0 0 0 / 0.4));}#chatgpt-mermaid-\_r_3tq\_ .actor-man circle,#chatgpt-mermaid-\_r_3tq\_ line{fill:rgb(9, 23, 44);stroke-width:2px;}#chatgpt-mermaid-\_r_3tq\_ g rect.rect{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_3tq\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_3tq\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_3tq\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_3tq\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_3tq\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3tq\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_3tq\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_3tq\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_3tq\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3tq\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_3tq\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_3tq\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3tq\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_3tq\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_3tq\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3tq\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_3tq\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3tq\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}A pauses for a long timeA's lease expiresA resumesAcquire lockGrantedAcquire lockGrantedPerform operationPerform operation using stale ownership

Both A and B may now perform operations on the protected resource.

The lock service may have behaved exactly as designed: A's lease expired, and B acquired a new lease. The problem is that A did not stop executing when its lease expired.

### Why can a client pause?

Several reasons are possible:

- Long garbage collection pauses.
- CPU starvation or operating-system scheduling delays.
- Network outages or high latency.
- A slow downstream service.
- A process suspension or virtual machine pause.
- A client waiting too long for a response.

The lock service cannot necessarily distinguish a dead client from a temporarily unresponsive one.

This leads to a central distributed-systems principle:

Failure to receive a response does not prove that the other process has failed.

A timeout tells you that a response did not arrive within a chosen period. It does not reveal with certainty what happened to the remote process.

## 3. Locks, leases, and failure detection

These three concepts are related, but they are not interchangeable.

### 3.1 Lock

A lock represents exclusive ownership of a resource under a coordination protocol.

For example:

```
Resource: monthly-report:2026-10

Owner: Instance A
```

Other clients should not be granted conflicting ownership while A's ownership is valid.

### 3.2 Lease

A lease grants ownership for a limited period.

```
A acquires lease at 10:00:00
Lease duration: 30 seconds

Lease expires at approximately 10:00:30
```

If A fails to renew the lease, the coordination service can make the resource available to another client.

Leases help with recovery from crashes because ownership does not have to be released explicitly by a crashed process.

However, a lease is not a mechanism for forcibly terminating a process or canceling all its outstanding operations.

### 3.3 Failure detection

Failure detection is the process of deciding whether a process is likely unavailable based on observations such as missed heartbeats or timeouts.

For example:

```
Worker A sends heartbeats to coordinator.

No heartbeat received within the timeout.

Coordinator suspects A is unavailable.
```

The coordinator may then allow another worker to take over.

But A might merely be experiencing a long pause or network partition.

This is why failure detectors generally provide suspicions rather than perfect knowledge of remote failures in an asynchronous network.

### How these concepts fit together

\#chatgpt-mermaid-\_r_3ul\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_3ul\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_3ul\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_3ul\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_3ul\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3ul\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_3ul\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_3ul\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_3ul\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_3ul\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_3ul\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_3ul\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3ul\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3ul\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_3ul\_ p{margin:0;}#chatgpt-mermaid-\_r_3ul\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3ul\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3ul\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3ul\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_3ul\_ .label text,#chatgpt-mermaid-\_r_3ul\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3ul\_ .node rect,#chatgpt-mermaid-\_r_3ul\_ .node circle,#chatgpt-mermaid-\_r_3ul\_ .node ellipse,#chatgpt-mermaid-\_r_3ul\_ .node polygon,#chatgpt-mermaid-\_r_3ul\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_3ul\_ .rough-node .label text,#chatgpt-mermaid-\_r_3ul\_ .node .label text,#chatgpt-mermaid-\_r_3ul\_ .image-shape .label,#chatgpt-mermaid-\_r_3ul\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_3ul\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_3ul\_ .rough-node .label,#chatgpt-mermaid-\_r_3ul\_ .node .label,#chatgpt-mermaid-\_r_3ul\_ .image-shape .label,#chatgpt-mermaid-\_r_3ul\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_3ul\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_3ul\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3ul\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3ul\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_3ul\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_3ul\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_3ul\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_3ul\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_3ul\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_3ul\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_3ul\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3ul\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3ul\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_3ul\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3ul\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_3ul\_ .icon-shape,#chatgpt-mermaid-\_r_3ul\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_3ul\_ .icon-shape p,#chatgpt-mermaid-\_r_3ul\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_3ul\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_3ul\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_3ul\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_3ul\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_3ul\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_3ul\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_3ul\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_3ul\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_3ul\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3ul\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_3ul\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_3ul\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_3ul\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3ul\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_3ul\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_3ul\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3ul\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_3ul\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_3ul\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3ul\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_3ul\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3ul\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_3ul\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_3ul\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_3ul\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_3ul\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_3ul\_ .node rect,#chatgpt-mermaid-\_r_3ul\_ .node circle,#chatgpt-mermaid-\_r_3ul\_ .node ellipse,#chatgpt-mermaid-\_r_3ul\_ .node polygon,#chatgpt-mermaid-\_r_3ul\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_3ul\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_3ul\_ .node.mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_3ul\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_3ul\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3ul\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}Client requests ownershipCoordinator grants leaseClient performs workLease renewed?Lease expiresAnother client may acquireownershipOld client might still berunningResource-level protectionmay be requiredYesNo

A production design must handle the final step. It is not enough to decide that a lease expired; the system must also ensure that stale work cannot violate important invariants.

## 4. The split-brain problem

Split brain occurs when two processes or groups behave as though they independently hold authority over the same logical resource.

For example, suppose two service instances believe they are the leader:

```
Instance A: "I am the leader."
Instance B: "I am the leader."
```

Both may begin processing the same events, updating shared state, or issuing commands.

This can happen because of:

- A network partition.
- Incorrect failover behavior.
- Lock state lost during failover.
- Lease expiration while an old owner continues running.
- Multiple coordinators independently granting ownership.

### Example: Leader election

Imagine three application instances manage a background processing service. Normally, only the leader should schedule a particular operation.

\#chatgpt-mermaid-\_r_3v4\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_3v4\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_3v4\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_3v4\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_3v4\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3v4\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_3v4\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_3v4\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_3v4\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_3v4\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_3v4\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_3v4\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3v4\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3v4\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_3v4\_ p{margin:0;}#chatgpt-mermaid-\_r_3v4\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3v4\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3v4\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3v4\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_3v4\_ .label text,#chatgpt-mermaid-\_r_3v4\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3v4\_ .node rect,#chatgpt-mermaid-\_r_3v4\_ .node circle,#chatgpt-mermaid-\_r_3v4\_ .node ellipse,#chatgpt-mermaid-\_r_3v4\_ .node polygon,#chatgpt-mermaid-\_r_3v4\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_3v4\_ .rough-node .label text,#chatgpt-mermaid-\_r_3v4\_ .node .label text,#chatgpt-mermaid-\_r_3v4\_ .image-shape .label,#chatgpt-mermaid-\_r_3v4\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_3v4\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_3v4\_ .rough-node .label,#chatgpt-mermaid-\_r_3v4\_ .node .label,#chatgpt-mermaid-\_r_3v4\_ .image-shape .label,#chatgpt-mermaid-\_r_3v4\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_3v4\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_3v4\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3v4\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3v4\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_3v4\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_3v4\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_3v4\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_3v4\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_3v4\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_3v4\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_3v4\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3v4\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3v4\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_3v4\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3v4\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_3v4\_ .icon-shape,#chatgpt-mermaid-\_r_3v4\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_3v4\_ .icon-shape p,#chatgpt-mermaid-\_r_3v4\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_3v4\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_3v4\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_3v4\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_3v4\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_3v4\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_3v4\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_3v4\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_3v4\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_3v4\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3v4\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_3v4\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_3v4\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_3v4\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3v4\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_3v4\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_3v4\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3v4\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_3v4\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_3v4\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3v4\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_3v4\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3v4\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_3v4\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_3v4\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_3v4\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_3v4\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_3v4\_ .node rect,#chatgpt-mermaid-\_r_3v4\_ .node circle,#chatgpt-mermaid-\_r_3v4\_ .node ellipse,#chatgpt-mermaid-\_r_3v4\_ .node polygon,#chatgpt-mermaid-\_r_3v4\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_3v4\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_3v4\_ .node.mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_3v4\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_3v4\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3v4\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}Coordination serviceInstance AInstance BInstance CShared resource

At first, A is the leader. Then A becomes temporarily disconnected from the coordination service, and B is elected leader.

If A continues working, both A and B may perform leader-only actions.

The solution is not simply to elect B. The system must also protect against A's stale authority.

Possible safeguards include:

1. Fencing tokens: The resource rejects operations from older leadership epochs.
2. Conditional state transitions: The resource accepts a change only if its current state permits it.
3. Database constraints and transactions: The database enforces business invariants.
4. A well-designed consensus-based coordinator: The coordinator maintains authoritative ownership state under its defined failure model.

These approaches address different parts of the problem and can be combined.

### Is leader election the same as distributed locking?

No.

Leader election chooses a process to act as leader. Distributed locking grants ownership of a resource or critical section.

They are closely related because both coordinate ownership, but the application responsibilities differ.

A leader may need to perform many operations over time. If a former leader can continue sending commands after losing leadership, fencing or equivalent resource-level checks may be necessary.

## 5. Why consensus matters: Redis vs etcd vs ZooKeeper

A key system design question is:

How does the coordination service ensure that competing clients agree on ownership, even when machines fail?

Different systems provide different guarantees.

### 5.1 Redis

Redis can provide fast, practical coordination using atomic commands such as:

```
SET lock:report token-A NX PX 30000
```

This is often suitable for best-effort coordination, provided the application understands expiration and failure behavior.

However, ordinary Redis replication is asynchronous. A lock acknowledged by one primary may not have reached a replica before failover. Consequently, the new primary may not contain that lock.

For strong correctness, do not assume that a successful Redis lock acquisition is equivalent to a consensus-backed ownership decision.

### 5.2 etcd

etcd is a distributed key-value store commonly used for configuration, service coordination, leases, and leader election.

It uses the Raft consensus algorithm to replicate its state across a cluster.

Conceptually, a three-member cluster works like this:

\#chatgpt-mermaid-\_r_3vk\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_3vk\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_3vk\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_3vk\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_3vk\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3vk\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_3vk\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_3vk\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_3vk\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_3vk\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_3vk\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_3vk\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3vk\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3vk\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_3vk\_ p{margin:0;}#chatgpt-mermaid-\_r_3vk\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3vk\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3vk\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3vk\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_3vk\_ .label text,#chatgpt-mermaid-\_r_3vk\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3vk\_ .node rect,#chatgpt-mermaid-\_r_3vk\_ .node circle,#chatgpt-mermaid-\_r_3vk\_ .node ellipse,#chatgpt-mermaid-\_r_3vk\_ .node polygon,#chatgpt-mermaid-\_r_3vk\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_3vk\_ .rough-node .label text,#chatgpt-mermaid-\_r_3vk\_ .node .label text,#chatgpt-mermaid-\_r_3vk\_ .image-shape .label,#chatgpt-mermaid-\_r_3vk\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_3vk\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_3vk\_ .rough-node .label,#chatgpt-mermaid-\_r_3vk\_ .node .label,#chatgpt-mermaid-\_r_3vk\_ .image-shape .label,#chatgpt-mermaid-\_r_3vk\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_3vk\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_3vk\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3vk\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3vk\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_3vk\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_3vk\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_3vk\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_3vk\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_3vk\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_3vk\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_3vk\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3vk\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3vk\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_3vk\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3vk\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_3vk\_ .icon-shape,#chatgpt-mermaid-\_r_3vk\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_3vk\_ .icon-shape p,#chatgpt-mermaid-\_r_3vk\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_3vk\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_3vk\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_3vk\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_3vk\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_3vk\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_3vk\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_3vk\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_3vk\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_3vk\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3vk\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_3vk\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_3vk\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_3vk\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3vk\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_3vk\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_3vk\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3vk\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_3vk\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_3vk\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3vk\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_3vk\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3vk\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_3vk\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_3vk\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_3vk\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_3vk\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_3vk\_ .node rect,#chatgpt-mermaid-\_r_3vk\_ .node circle,#chatgpt-mermaid-\_r_3vk\_ .node ellipse,#chatgpt-mermaid-\_r_3vk\_ .node polygon,#chatgpt-mermaid-\_r_3vk\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_3vk\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_3vk\_ .node.mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_3vk\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_3vk\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3vk\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}Client requestCurrent etcd leaderReplicate operationMember 1Member 2Member 3Quorum agreementOperation committed

The diagram is conceptual: Raft replication and commitment follow specific protocol rules, and not every replica must acknowledge every operation for it to commit.

With three healthy members, a majority is two. If one member fails, the other two can generally continue committing operations. If the cluster loses its majority, it cannot continue committing new consensus-backed updates.

This trade-off is intentional: the system sacrifices availability for certain operations rather than allowing disconnected groups to independently commit conflicting authoritative state.

etcd also provides leases and concurrency primitives that can be used to build locks.

### 5.3 ZooKeeper

Apache ZooKeeper is another established coordination system.

It is used for tasks such as:

- Leader election.
- Distributed synchronization.
- Configuration management.
- Coordination among distributed services.

One common locking approach uses ephemeral sequential nodes. Clients create sequential nodes under a designated lock path and acquire the lock according to their sequence order. A client can watch the node immediately preceding its own rather than waking every contender whenever the lock changes.

ZooKeeper's session and ephemeral-node mechanisms help clean up ownership associated with expired sessions.

However, as with other coordination systems, a client that loses its session may still be running application code. The protected resource may need fencing or other safeguards against stale operations.

### 5.4 Comparison

| Dimension                 | Redis lock                                                                                    | etcd                                                                                      | ZooKeeper                                                    |
| ------------------------- | --------------------------------------------------------------------------------------------- | ----------------------------------------------------------------------------------------- | ------------------------------------------------------------ |
| Main strength             | Fast, simple coordination                                                                     | Strongly consistent coordination and leases                                               | Mature distributed coordination primitives                   |
| Coordination approach     | Atomic commands and TTLs                                                                      | Raft consensus, leases, transactions                                                      | Replicated coordination state and sessions                   |
| Typical use               | Job coordination, cache refresh                                                               | Leader election, locks, authoritative coordination                                        | Leader election, distributed synchronization                 |
| Important limitation      | Lock state can be lost during certain failovers; lease expiry can leave stale clients running | Quorum loss prevents consensus-backed writes; clients still need safe resource operations | Session expiry does not forcibly stop stale application code |
| Resource-level protection | Often necessary                                                                               | May still be necessary                                                                    | May still be necessary                                       |

The correct choice depends on the required guarantees, deployment complexity, latency, operational expertise, and the consequences of conflicting operations.

Important: A consensus-backed coordinator improves the correctness of the ownership decision. It does not automatically make an external database, payment service, or object store reject commands from an old owner.

## 6. Understanding quorum and majority

Let's understand why consensus-based systems use a majority.

Imagine a cluster with five voting members.

\\[ \text{Quorum} = \left\lfloor \frac{N}{2} \right\rfloor + 1 \\]

For \\(N=5\\):

\\[ \text{Quorum} = \left\lfloor \frac{5}{2} \right\rfloor + 1 = 3 \\]

So three members constitute a majority.

Why is this useful?

Any two majorities in the same five-member voting configuration must overlap by at least one member.

For example:

- Majority A: members 1, 2, 3.
- Majority B: members 3, 4, 5.

Both majorities include member 3.

This overlap helps prevent two independent majorities from committing conflicting histories under a correctly implemented consensus protocol.

### What if the cluster splits into two groups?

Suppose a five-member cluster splits into groups of three and two.

\#chatgpt-mermaid-\_r_401\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_401\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_401\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_401\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_401\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_401\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_401\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_401\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_401\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_401\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_401\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_401\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_401\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_401\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_401\_ p{margin:0;}#chatgpt-mermaid-\_r_401\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_401\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_401\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_401\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_401\_ .label text,#chatgpt-mermaid-\_r_401\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_401\_ .node rect,#chatgpt-mermaid-\_r_401\_ .node circle,#chatgpt-mermaid-\_r_401\_ .node ellipse,#chatgpt-mermaid-\_r_401\_ .node polygon,#chatgpt-mermaid-\_r_401\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_401\_ .rough-node .label text,#chatgpt-mermaid-\_r_401\_ .node .label text,#chatgpt-mermaid-\_r_401\_ .image-shape .label,#chatgpt-mermaid-\_r_401\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_401\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_401\_ .rough-node .label,#chatgpt-mermaid-\_r_401\_ .node .label,#chatgpt-mermaid-\_r_401\_ .image-shape .label,#chatgpt-mermaid-\_r_401\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_401\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_401\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_401\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_401\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_401\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_401\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_401\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_401\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_401\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_401\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_401\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_401\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_401\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_401\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_401\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_401\_ .icon-shape,#chatgpt-mermaid-\_r_401\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_401\_ .icon-shape p,#chatgpt-mermaid-\_r_401\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_401\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_401\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_401\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_401\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_401\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_401\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_401\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_401\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_401\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_401\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_401\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_401\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_401\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_401\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_401\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_401\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_401\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_401\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_401\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_401\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_401\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_401\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_401\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_401\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_401\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_401\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_401\_ .node rect,#chatgpt-mermaid-\_r_401\_ .node circle,#chatgpt-mermaid-\_r_401\_ .node ellipse,#chatgpt-mermaid-\_r_401\_ .node polygon,#chatgpt-mermaid-\_r_401\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_401\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_401\_ .node.mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_401\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_401\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_401\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}Five-member clusterPartition A: 3 membersPartition B: 2 membersHas majorityNo majorityMay commit new operationsCannot independentlycommit consensus-backedwrites

The group of three can potentially continue making progress. The group of two cannot form a majority.

This is a central distributed-systems trade-off: during certain network failures, a strongly consistent system cannot safely accept authoritative writes in every disconnected partition.

### A subtle but important distinction

Quorum is not, by itself, a complete distributed locking algorithm.

A production protocol must also define:

- How ownership is assigned.
- How concurrent requests are ordered.
- How a leader is elected.
- How leases expire.
- How clients learn that ownership has changed.
- How stale clients are prevented from corrupting protected state.

Quorum is one component of the consistency mechanism, not a replacement for the whole protocol.

## 7. Fencing tokens and leadership epochs

Let's revisit fencing, now in the context of distributed coordination.

Suppose a coordinator elects leaders with monotonically increasing terms, also called epochs.

```
Epoch 10 → Instance A is leader
Epoch 11 → Instance B becomes leader
Epoch 12 → Instance C becomes leader
```

Every leader receives a newer epoch than its predecessors.

When an instance performs a protected write, it includes its epoch:

```
Instance A writes with epoch 10
Instance B writes with epoch 11
```

The protected resource remembers the highest accepted epoch and rejects older epochs once a newer one has been accepted.

### Example: A database enforcing a fencing token

Assume the current fencing token for a resource is `11`.

```
UPDATE shared_resource
SET value = :new_value,
    fencing_token = :token
WHERE resource_id = :resource_id
  AND fencing_token < :token
RETURNING resource_id;
```

A write with token `10` will be rejected if the database has already accepted token `11`.

However, this example permits only strictly newer tokens. If a legitimate owner needs to perform multiple writes using the same token, this exact query would reject its second write. In that case, design a protocol that validates the ownership epoch while allowing multiple authorized operations within it—for example, by checking the epoch against authoritative state and using a separate operation sequence or transaction-level validation.

The right implementation depends on the protected resource and the number of operations allowed per ownership epoch.

### What makes a fencing token reliable?

A reliable fencing token must have these properties:

1. New ownership epochs receive monotonically increasing tokens.
2. Tokens do not accidentally move backward after failover or recovery.
3. The protected resource actually validates them.
4. Stale operations cannot bypass that validation.

A UUID is useful for identifying an owner, but it cannot tell the resource which of two owners is newer.

### When are fencing tokens especially valuable?

- Distributed workers writing to shared state.
- Leader-based services.
- Long-running tasks that can outlive their leases.
- Systems where an old owner must never overwrite newer results.

If the operation is inherently idempotent and duplicate execution is harmless, fencing may not be necessary. Choose the mechanism according to the invariant you need to preserve.

## 8. Distributed locking vs alternative approaches

A distributed lock is only one way to coordinate work. Often, a different primitive is simpler and safer.

| Problem                                    | Preferred starting point                                              | Why                                                                    |
| ------------------------------------------ | --------------------------------------------------------------------- | ---------------------------------------------------------------------- |
| Prevent duplicate seat bookings            | Unique constraint in PostgreSQL                                       | The database enforces the invariant                                    |
| Update a record only if unchanged          | Optimistic locking                                                    | Detects stale updates without holding a lock throughout the read phase |
| Claim work from a PostgreSQL queue         | `FOR UPDATE SKIP LOCKED` with durable job state                       | Workers can claim different jobs concurrently                          |
| Avoid duplicate payment requests           | Idempotency key and durable transaction state                         | Duplicate requests can be recognized safely                            |
| Allow one scheduled job runner             | Distributed lease or leader election                                  | Provides shared coordination across instances                          |
| Avoid duplicate cache refreshes            | Redis lock or single-flight mechanism                                 | Usually reduces redundant work                                         |
| Serialize commands for one entity          | Partitioned queue or single-writer design                             | Commands can be processed in a controlled order                        |
| Maintain a critical cross-system invariant | Resource-level validation, transactions, and appropriate coordination | A lock alone cannot make multiple independent systems atomic           |

### Example: Use a queue instead of a global lock

Suppose you have ten workers and ten thousand jobs.

A global lock around the entire queue would force workers to compete for one critical section, reducing throughput.

Instead, let workers claim distinct jobs:

\#chatgpt-mermaid-\_r_40t\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_40t\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_40t\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_40t\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_40t\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_40t\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_40t\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_40t\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_40t\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_40t\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_40t\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_40t\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_40t\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_40t\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_40t\_ p{margin:0;}#chatgpt-mermaid-\_r_40t\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_40t\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_40t\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_40t\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_40t\_ .label text,#chatgpt-mermaid-\_r_40t\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_40t\_ .node rect,#chatgpt-mermaid-\_r_40t\_ .node circle,#chatgpt-mermaid-\_r_40t\_ .node ellipse,#chatgpt-mermaid-\_r_40t\_ .node polygon,#chatgpt-mermaid-\_r_40t\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_40t\_ .rough-node .label text,#chatgpt-mermaid-\_r_40t\_ .node .label text,#chatgpt-mermaid-\_r_40t\_ .image-shape .label,#chatgpt-mermaid-\_r_40t\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_40t\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_40t\_ .rough-node .label,#chatgpt-mermaid-\_r_40t\_ .node .label,#chatgpt-mermaid-\_r_40t\_ .image-shape .label,#chatgpt-mermaid-\_r_40t\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_40t\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_40t\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_40t\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_40t\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_40t\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_40t\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_40t\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_40t\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_40t\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_40t\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_40t\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_40t\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_40t\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_40t\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_40t\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_40t\_ .icon-shape,#chatgpt-mermaid-\_r_40t\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_40t\_ .icon-shape p,#chatgpt-mermaid-\_r_40t\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_40t\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_40t\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_40t\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_40t\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_40t\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_40t\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_40t\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_40t\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_40t\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_40t\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_40t\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_40t\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_40t\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_40t\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_40t\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_40t\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_40t\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_40t\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_40t\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_40t\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_40t\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_40t\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_40t\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_40t\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_40t\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_40t\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_40t\_ .node rect,#chatgpt-mermaid-\_r_40t\_ .node circle,#chatgpt-mermaid-\_r_40t\_ .node ellipse,#chatgpt-mermaid-\_r_40t\_ .node polygon,#chatgpt-mermaid-\_r_40t\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_40t\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_40t\_ .node.mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_40t\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_40t\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_40t\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}Durable job queueWorker 1Worker 2Worker 3Job AJob BJob C

For a PostgreSQL-backed queue, `FOR UPDATE SKIP LOCKED` can let concurrent workers claim different rows without waiting for rows already locked by another worker.

For a message broker, the broker's delivery and acknowledgment model may provide the coordination mechanism instead.

This often scales better than making every worker contend for a single distributed lock.

## 9. A practical decision framework for system design interviews

When an interviewer asks, "How would you implement distributed locking?", avoid jumping straight to Redis.

Ask yourself these questions.

### Question 1: What invariant are we protecting?

For example:

- Only one worker should perform an expensive calculation.
- A seat must never be booked twice.
- Only the current leader may update authoritative state.

The required guarantee differs between these examples.

### Question 2: What happens if the work executes twice?

If duplicate work is harmless, a best-effort lock may be sufficient.

If duplicate execution can cause financial loss or corrupt state, use stronger safeguards such as transactions, unique constraints, idempotency, and resource-level fencing.

### Question 3: Can the protected resource enforce the rule itself?

If the resource is PostgreSQL, start with its transactional primitives.

If it is an external system, determine whether that system supports conditional writes, version checks, idempotency keys, or fencing tokens.

### Question 4: What happens if the owner crashes or pauses?

Define:

- How ownership expires.
- How another worker takes over.
- What the old owner must do when it resumes.
- How stale operations are rejected.
- Whether the job needs recovery or idempotent re-execution.

### Question 5: What coordination guarantees are required?

Choose the mechanism based on the failure model:

- PostgreSQL: database transactions, row locks, constraints.
- Redis: fast, simple distributed coordination when its lease and failover semantics are acceptable.
- etcd or ZooKeeper: dedicated coordination primitives when stronger ownership coordination is required.
- Queue or single-writer architecture: often preferable when work can be partitioned and serialized naturally.

## 10. Common interview questions and answers

### Q1. Why is distributed locking harder than local locking?

Local threads can coordinate through shared memory and synchronization primitives. Distributed clients communicate over networks, where delays, partitions, and partial failures make it impossible to infer remote failure with certainty from a timeout alone.

### Q2. What is a lease?

A lease grants ownership for a limited period. It enables recovery when a client crashes, but an expired client may still be running and must not be trusted to perform unrestricted stale operations.

### Q3. What is split brain?

Split brain occurs when multiple processes or groups independently believe they have authority over the same resource. It can cause conflicting writes or duplicate processing.

### Q4. How does consensus help distributed locking?

Consensus protocols such as Raft help participating nodes agree on an authoritative sequence of state changes. They prevent independent partitions from both committing conflicting authoritative histories under the protocol's assumptions.

### Q5. Does consensus eliminate stale-owner problems?

No. A client may continue running after it loses leadership or its lease expires. The protected resource may still need fencing tokens or equivalent validation.

### Q6. Why do consensus systems require a quorum?

A majority allows overlapping voting sets, helping prevent conflicting decisions. When the cluster loses its majority, it generally cannot commit new consensus-backed writes.

### Q7. When would you choose etcd over Redis?

I would consider etcd when the system requires its consensus-backed coordination semantics, such as authoritative leader election or strongly consistent coordination state. I would choose Redis when its simpler coordination model is sufficient and its operational trade-offs are acceptable.

### Q8. Is a distributed lock sufficient for exactly-once processing?

No. A lock can reduce concurrent execution, but crashes, retries, and uncertain outcomes can still produce duplicate effects. Exactly-once effects generally require idempotency, durable state transitions, transactional mechanisms, or deduplication at the resource performing the effect.

### Q9. How do you prevent an old leader from writing after a new leader is elected?

Use fencing tokens or leadership epochs and have the protected resource validate them. The token must be generated reliably, and the resource must reject stale operations.

### Q10. Would you use a distributed lock for inventory management?

I would start with an atomic conditional update or a database transaction and constraints. I would introduce a distributed lock only if there were an additional coordination requirement that database-native mechanisms could not adequately address.

## Chapter 6 summary

The most important lessons are:

1. A distributed lock coordinates ownership; it does not stop a stale process from executing.
2. Leases enable recovery but introduce expiration and renewal problems.
3. Split brain occurs when multiple processes believe they have authority.
4. Consensus-backed systems provide stronger coordination guarantees, but they do not automatically protect every downstream resource.
5. Fencing tokens let a protected resource reject stale owners.
6. For many problems, a database constraint, atomic update, queue, or idempotency mechanism is simpler than distributed locking.

### Next chapter — Chapter 7: Alternatives to locking

We'll explore compare-and-swap (CAS), atomic updates, optimistic concurrency, database constraints, idempotency keys, queue-based serialization, and single-writer designs—plus how to choose among them for high-level system design.

Type `next` to continue.

