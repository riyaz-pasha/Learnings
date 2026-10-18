# Chapter 5: Redis Locking — From Basics to Production

In the previous chapters, we covered pessimistic locking, optimistic locking, and PostgreSQL concurrency internals. Now let's look at Redis-based locking, which is commonly used when multiple application instances need to coordinate work.

We'll cover the basic implementation first, then work through the failure scenarios that matter in production and system design interviews.

## 1. Why do we need Redis locks?

Imagine you have three instances of a backend service running behind a load balancer:

\#chatgpt-mermaid-\_r_3m8\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_3m8\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_3m8\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_3m8\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_3m8\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3m8\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_3m8\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_3m8\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_3m8\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_3m8\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_3m8\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_3m8\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3m8\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3m8\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_3m8\_ p{margin:0;}#chatgpt-mermaid-\_r_3m8\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3m8\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3m8\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3m8\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_3m8\_ .label text,#chatgpt-mermaid-\_r_3m8\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3m8\_ .node rect,#chatgpt-mermaid-\_r_3m8\_ .node circle,#chatgpt-mermaid-\_r_3m8\_ .node ellipse,#chatgpt-mermaid-\_r_3m8\_ .node polygon,#chatgpt-mermaid-\_r_3m8\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_3m8\_ .rough-node .label text,#chatgpt-mermaid-\_r_3m8\_ .node .label text,#chatgpt-mermaid-\_r_3m8\_ .image-shape .label,#chatgpt-mermaid-\_r_3m8\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_3m8\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_3m8\_ .rough-node .label,#chatgpt-mermaid-\_r_3m8\_ .node .label,#chatgpt-mermaid-\_r_3m8\_ .image-shape .label,#chatgpt-mermaid-\_r_3m8\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_3m8\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_3m8\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3m8\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3m8\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_3m8\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_3m8\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_3m8\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_3m8\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_3m8\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_3m8\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_3m8\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3m8\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3m8\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_3m8\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3m8\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_3m8\_ .icon-shape,#chatgpt-mermaid-\_r_3m8\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_3m8\_ .icon-shape p,#chatgpt-mermaid-\_r_3m8\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_3m8\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_3m8\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_3m8\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_3m8\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_3m8\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_3m8\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_3m8\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_3m8\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_3m8\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3m8\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_3m8\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_3m8\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_3m8\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3m8\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_3m8\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_3m8\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3m8\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_3m8\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_3m8\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3m8\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_3m8\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3m8\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_3m8\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_3m8\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_3m8\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_3m8\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_3m8\_ .node rect,#chatgpt-mermaid-\_r_3m8\_ .node circle,#chatgpt-mermaid-\_r_3m8\_ .node ellipse,#chatgpt-mermaid-\_r_3m8\_ .node polygon,#chatgpt-mermaid-\_r_3m8\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_3m8\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_3m8\_ .node.mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_3m8\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_3m8\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3m8\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}Incoming requestsLoad balancerApplication instance AApplication instance BApplication instance CShared Redis

Suppose your application needs to generate a monthly report. You want only one instance to generate that report at a time.

Without coordination, all three instances might start the same expensive job:

```
Instance A: Start report generation
Instance B: Start report generation
Instance C: Start report generation

Result: Same report generated three times
```

You might encounter similar requirements in:

- Preventing duplicate execution of scheduled jobs.
- Coordinating a resource shared across multiple application instances.
- Avoiding redundant cache refreshes.
- Ensuring only one worker attempts a particular operation at a time.
- Coordinating maintenance or batch-processing tasks.

A Java `synchronized` block cannot solve this problem across separate JVMs. Each JVM has its own memory and monitor locks.

A Redis lock gives multiple application instances a shared coordination mechanism.

Important: A Redis lock is not automatically a correctness guarantee. It coordinates cooperating clients, but failures and expiration can allow two clients to believe they own the same work.

## 2. The basic Redis lock

Redis provides an atomic command that is ideal for acquiring a simple lock:

```
SET lock:monthly-report unique-token-123 NX PX 30000
```

Let's break it down.

| Part                  | Meaning                                                  |
| --------------------- | -------------------------------------------------------- |
| `lock:monthly-report` | The key representing the lock                            |
| `unique-token-123`    | A unique ownership token for this acquisition            |
| `NX`                  | Set the key only if it does not already exist            |
| `PX 30000`            | Set the expiration to 30,000 milliseconds, or 30 seconds |

The command combines lock acquisition and expiration in one atomic Redis operation.

### What happens when two instances compete?

Instance BRedisInstance AInstance BRedisInstance A#chatgpt-mermaid-\_r_3mt\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_3mt\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_3mt\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_3mt\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_3mt\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3mt\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_3mt\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_3mt\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_3mt\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_3mt\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_3mt\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_3mt\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3mt\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3mt\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_3mt\_ p{margin:0;}#chatgpt-mermaid-\_r_3mt\_ .actor{stroke:rgb(31, 78, 148);fill:rgb(9, 23, 44);stroke-width:1;}#chatgpt-mermaid-\_r_3mt\_ rect.actor.outer-path[data-look="neo"]{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3mt\_ rect.note[data-look="neo"]{stroke:rgb(58, 132, 63);fill:rgb(48, 48, 48);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3mt\_ text.actor>tspan{fill:rgb(237, 237, 237);stroke:none;}#chatgpt-mermaid-\_r_3mt\_ .actor-line{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3mt\_ .innerArc{stroke-width:1.5;stroke-dasharray:none;}#chatgpt-mermaid-\_r_3mt\_ .messageLine0{stroke-width:1.5;stroke-dasharray:none;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3mt\_ .messageLine1{stroke-width:1.5;stroke-dasharray:2,2;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3mt\_ [id$="-arrowhead"] path{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3mt\_ .sequenceNumber{fill:#505050;}#chatgpt-mermaid-\_r_3mt\_ [id$="-sequencenumber"]{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3mt\_ [id$="-crosshead"] path{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3mt\_ .messageText{fill:rgb(237, 237, 237);stroke:none;}#chatgpt-mermaid-\_r_3mt\_ .labelBox{stroke:rgba(255, 255, 255, 0.15);fill:rgb(0, 0, 0);filter:none;}#chatgpt-mermaid-\_r_3mt\_ .labelText,#chatgpt-mermaid-\_r_3mt\_ .labelText>tspan{fill:rgb(237, 237, 237);stroke:none;}#chatgpt-mermaid-\_r_3mt\_ .loopText,#chatgpt-mermaid-\_r_3mt\_ .loopText>tspan{fill:rgb(237, 237, 237);stroke:none;}#chatgpt-mermaid-\_r_3mt\_ .sectionTitle,#chatgpt-mermaid-\_r_3mt\_ .sectionTitle>tspan{fill:rgb(237, 237, 237);stroke:none;}#chatgpt-mermaid-\_r_3mt\_ .loopLine{stroke-width:2px;stroke-dasharray:2,2;stroke:rgba(255, 255, 255, 0.15);fill:rgba(255, 255, 255, 0.15);}#chatgpt-mermaid-\_r_3mt\_ .note{stroke:rgb(58, 132, 63);fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_3mt\_ .noteText,#chatgpt-mermaid-\_r_3mt\_ .noteText>tspan{fill:rgb(237, 237, 237);stroke:none;font-weight:normal;}#chatgpt-mermaid-\_r_3mt\_ .activation0{fill:rgb(48, 48, 48);stroke:hsl(0, 0%, 8.8235294118%);}#chatgpt-mermaid-\_r_3mt\_ .activation1{fill:rgb(48, 48, 48);stroke:hsl(0, 0%, 8.8235294118%);}#chatgpt-mermaid-\_r_3mt\_ .activation2{fill:rgb(48, 48, 48);stroke:hsl(0, 0%, 8.8235294118%);}#chatgpt-mermaid-\_r_3mt\_ .actorPopupMenu{position:absolute;}#chatgpt-mermaid-\_r_3mt\_ .actorPopupMenuPanel{position:absolute;fill:rgb(9, 23, 44);box-shadow:0px 8px 16px 0px rgba(0,0,0,0.2);filter:drop-shadow(3px 5px 2px rgb(0 0 0 / 0.4));}#chatgpt-mermaid-\_r_3mt\_ .actor-man circle,#chatgpt-mermaid-\_r_3mt\_ line{fill:rgb(9, 23, 44);stroke-width:2px;}#chatgpt-mermaid-\_r_3mt\_ g rect.rect{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_3mt\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_3mt\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_3mt\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_3mt\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_3mt\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3mt\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_3mt\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_3mt\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_3mt\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3mt\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_3mt\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_3mt\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3mt\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_3mt\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_3mt\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3mt\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_3mt\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3mt\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}SET lock:report token-A NX PX 30000OKSET lock:report token-B NX PX 30000nil (not acquired)Execute report

Redis processes the competing commands in a serialized order. If A acquires the key first, B cannot acquire it while the key exists.

The result is typically:

- `OK`: The lock was acquired.
- A null/nil response: The lock was not acquired because the key already exists.

The exact representation of the response depends on your Redis client library.

### Why must acquisition be atomic?

Consider this incorrect approach:

```
1. Check whether the lock key exists.
2. If it does not exist, set the lock key.
```

Two clients could interleave their operations:

```
Instance A: GET lock → key does not exist
Instance B: GET lock → key does not exist
Instance A: SET lock
Instance B: SET lock
```

Both clients might conclude that they acquired the lock.

Using `SET ... NX PX ...` avoids this check-then-set race.

### Why do we need a TTL?

Suppose an instance acquires a lock and then crashes:

```
Instance A acquires lock
        |
        v
Instance A crashes
        |
        v
Lock remains in Redis
        |
        v
Other instances cannot acquire it
```

Without expiration, the lock could remain forever unless something explicitly removes it.

With a TTL, Redis automatically expires the key after the lease period, allowing another instance to attempt acquisition.

But this introduces an important problem: what if the original owner is still working when the lock expires?

We'll examine that shortly.

## 3. Why `DEL lock_key` is dangerous

This is one of the most important Redis locking interview questions.

Suppose Instance A acquires a lock with a 10-second TTL.

```
T = 0 seconds
A acquires the lock

T = 10 seconds
A's lock expires

T = 11 seconds
B acquires the same lock

T = 12 seconds
A finishes its work and executes DEL lock
```

What happens?

A deletes B's lock!

A no longer owns the lock, but it has deleted the lock belonging to the new owner.

The result is that Instance C might now acquire the lock while B is still working.

Instance CInstance BRedisInstance AInstance CInstance BRedisInstance A#chatgpt-mermaid-\_r_3nu\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_3nu\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_3nu\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_3nu\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_3nu\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3nu\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_3nu\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_3nu\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_3nu\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_3nu\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_3nu\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_3nu\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3nu\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3nu\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_3nu\_ p{margin:0;}#chatgpt-mermaid-\_r_3nu\_ .actor{stroke:rgb(31, 78, 148);fill:rgb(9, 23, 44);stroke-width:1;}#chatgpt-mermaid-\_r_3nu\_ rect.actor.outer-path[data-look="neo"]{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3nu\_ rect.note[data-look="neo"]{stroke:rgb(58, 132, 63);fill:rgb(48, 48, 48);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3nu\_ text.actor>tspan{fill:rgb(237, 237, 237);stroke:none;}#chatgpt-mermaid-\_r_3nu\_ .actor-line{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3nu\_ .innerArc{stroke-width:1.5;stroke-dasharray:none;}#chatgpt-mermaid-\_r_3nu\_ .messageLine0{stroke-width:1.5;stroke-dasharray:none;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3nu\_ .messageLine1{stroke-width:1.5;stroke-dasharray:2,2;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3nu\_ [id$="-arrowhead"] path{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3nu\_ .sequenceNumber{fill:#505050;}#chatgpt-mermaid-\_r_3nu\_ [id$="-sequencenumber"]{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3nu\_ [id$="-crosshead"] path{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3nu\_ .messageText{fill:rgb(237, 237, 237);stroke:none;}#chatgpt-mermaid-\_r_3nu\_ .labelBox{stroke:rgba(255, 255, 255, 0.15);fill:rgb(0, 0, 0);filter:none;}#chatgpt-mermaid-\_r_3nu\_ .labelText,#chatgpt-mermaid-\_r_3nu\_ .labelText>tspan{fill:rgb(237, 237, 237);stroke:none;}#chatgpt-mermaid-\_r_3nu\_ .loopText,#chatgpt-mermaid-\_r_3nu\_ .loopText>tspan{fill:rgb(237, 237, 237);stroke:none;}#chatgpt-mermaid-\_r_3nu\_ .sectionTitle,#chatgpt-mermaid-\_r_3nu\_ .sectionTitle>tspan{fill:rgb(237, 237, 237);stroke:none;}#chatgpt-mermaid-\_r_3nu\_ .loopLine{stroke-width:2px;stroke-dasharray:2,2;stroke:rgba(255, 255, 255, 0.15);fill:rgba(255, 255, 255, 0.15);}#chatgpt-mermaid-\_r_3nu\_ .note{stroke:rgb(58, 132, 63);fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_3nu\_ .noteText,#chatgpt-mermaid-\_r_3nu\_ .noteText>tspan{fill:rgb(237, 237, 237);stroke:none;font-weight:normal;}#chatgpt-mermaid-\_r_3nu\_ .activation0{fill:rgb(48, 48, 48);stroke:hsl(0, 0%, 8.8235294118%);}#chatgpt-mermaid-\_r_3nu\_ .activation1{fill:rgb(48, 48, 48);stroke:hsl(0, 0%, 8.8235294118%);}#chatgpt-mermaid-\_r_3nu\_ .activation2{fill:rgb(48, 48, 48);stroke:hsl(0, 0%, 8.8235294118%);}#chatgpt-mermaid-\_r_3nu\_ .actorPopupMenu{position:absolute;}#chatgpt-mermaid-\_r_3nu\_ .actorPopupMenuPanel{position:absolute;fill:rgb(9, 23, 44);box-shadow:0px 8px 16px 0px rgba(0,0,0,0.2);filter:drop-shadow(3px 5px 2px rgb(0 0 0 / 0.4));}#chatgpt-mermaid-\_r_3nu\_ .actor-man circle,#chatgpt-mermaid-\_r_3nu\_ line{fill:rgb(9, 23, 44);stroke-width:2px;}#chatgpt-mermaid-\_r_3nu\_ g rect.rect{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_3nu\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_3nu\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_3nu\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_3nu\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_3nu\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3nu\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_3nu\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_3nu\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_3nu\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3nu\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_3nu\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_3nu\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3nu\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_3nu\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_3nu\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3nu\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_3nu\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3nu\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}A's TTL expiresB's lock is deleted!Acquire lock (token-A)AcquiredAcquire lock (token-B)AcquiredDEL lockAcquire lock (token-C)Acquired

The root cause is that the lock key identifies the resource, but a plain `DEL` does not check who owns the current lock.

### Solution: Use a unique ownership token

Every acquisition should have a unique token, such as a UUID:

```
Instance A acquires: lock:report = token-A
Instance B acquires: lock:report = token-B
```

When A wants to release its lock, it must verify that the token stored in Redis still matches `token-A`.

Conceptually:

```
IF current lock token == my token:
    delete the lock
ELSE:
    do nothing
```

However, implementing this as two separate Redis commands is still unsafe.

For example:

```
A: GET lock → token-A
A: pauses

Lock expires; B acquires lock with token-B

A: DEL lock
```

A checked the value correctly, but the lock changed before its deletion.

The comparison and deletion must happen atomically.

### Safe release using a Lua script

Redis executes a Lua script atomically with respect to other commands on that Redis server.

```
if redis.call("GET", KEYS[1]) == ARGV[1] then
    return redis.call("DEL", KEYS[1])
else
    return 0
end
```

Example invocation using `EVAL`:

```
EVAL "if redis.call('GET', KEYS[1]) == ARGV[1] then return redis.call('DEL', KEYS[1]) else return 0 end" 1 lock:monthly-report token-A
```

Here:

- `KEYS[1]` is the lock key.
- `ARGV[1]` is the token belonging to the client attempting release.
- A return value of `1` means the lock was deleted.
- A return value of `0` means the token didn't match, so nothing was deleted.

In production, use your Redis client's Lua scripting API rather than manually constructing command strings.

Interview takeaway: Use a unique token per lock acquisition and atomically compare-and-delete during release. Never blindly delete a Redis lock key.

## 4. The TTL problem: A lock can expire while its owner is working

A TTL prevents a crashed client from holding a lock forever, but it doesn't guarantee that the client's work finishes within the TTL.

Consider this example:

```
Lock TTL = 30 seconds

Instance A:
  0s  → Acquires lock
  5s  → Starts processing
  20s → JVM pauses due to a long garbage collection
  30s → Lock expires in Redis
  35s → Instance B acquires the lock
  40s → Instance A resumes processing
```

Now A and B may both be executing the protected operation.

A still believes it owns the lock, but Redis has already allowed B to acquire a new lease.

This is a fundamental distinction:

- A Redis TTL controls how long the lock key exists.
- It does not forcibly stop the original application from executing.
- It does not automatically cancel requests already sent to a database or another service.

A network delay, process pause, long-running operation, or unexpected slowdown can cause the same problem.

### 4.1 What does a Redis lock actually guarantee?

Under normal operation, a successful acquisition ensures that only one client can hold the corresponding unexpired lock key at a time on that Redis primary.

It does not, by itself, guarantee that only one client can continue executing the protected work.

This difference matters when the protected operation has serious consequences, such as:

- Transferring money.
- Booking a seat.
- Updating inventory.
- Processing a payment.
- Writing to a shared resource where stale updates must be rejected.

For these operations, you need to protect the actual resource, not just the lock key.

### 4.2 Can we increase the TTL?

You could set the TTL to several minutes, but this is a trade-off:

- A longer TTL reduces the chance of normal work outlasting the lease.
- If the client crashes, other clients must wait longer before the lock expires.
- A sufficiently long pause or delay can still exceed any finite TTL.

Increasing the TTL alone does not eliminate the correctness problem.

## 5. Lock renewal and lease management

If a job can take longer than the initial TTL, you may need to renew the lease while processing.

For example:

```
Initial TTL: 30 seconds

Time 0s:  Acquire lock
Time 10s: Renew lock
Time 20s: Renew lock
Time 30s: Renew lock
Time 40s: Finish work
Time 40s: Release lock
```

Renewal should happen well before expiration, leaving enough time for network delays and Redis response processing.

### 5.1 Renew only if you still own the lock

Never blindly execute:

```
PEXPIRE lock:monthly-report 30000
```

Imagine A's lock expires and B acquires the same key. If A resumes and executes `PEXPIRE`, it could extend B's lease.

Instead, compare the ownership token and update the TTL atomically:

```
if redis.call("GET", KEYS[1]) == ARGV[1] then
    return redis.call("PEXPIRE", KEYS[1], ARGV[2])
else
    return 0
end
```

For example, pass:

- `KEYS[1]`: `lock:monthly-report`
- `ARGV[1]`: `token-A`
- `ARGV[2]`: `30000`

If the token matches, the TTL is extended. Otherwise, the script does nothing.

### 5.2 What if renewal fails?

Suppose A cannot renew its lease because of a network outage.

A must not simply assume it still owns the lock.

A robust application should:

1. Treat failed or uncertain renewal as potential loss of ownership.
2. Stop initiating further protected operations where possible.
3. Avoid publishing results or committing changes based solely on the old lock.
4. Ensure the actual resource can reject stale operations if correctness requires it.

There is an important limitation: stopping the application is not enough if an operation is already in flight or the process is paused. That is why stronger protection often requires fencing tokens or database-enforced invariants.

Lease renewal is a liveness mechanism, not a complete correctness mechanism.

## 6. Fencing tokens: Protecting the actual resource

Fencing tokens are a crucial concept in distributed locking.

The idea is simple: every new lock owner receives a monotonically increasing number. The resource being protected rejects operations from an older owner after it has accepted a newer owner's token.

Suppose the fencing tokens are:

- Instance A: token `41`
- Instance B: token `42`

A acquires the lock first, but its lease expires while A is paused. B acquires the lock and receives the newer token.

Invalid or unsupported diagram

The resource—not merely Redis—must enforce the token rule.

### 6.1 A PostgreSQL example

Suppose a database row stores the highest fencing token it has accepted:

```
CREATE TABLE report_state (
    report_id BIGINT PRIMARY KEY,
    content TEXT NOT NULL,
    last_fencing_token BIGINT NOT NULL DEFAULT 0
);
```

A write can be guarded so that a token older than the stored token is rejected:

```
UPDATE report_state
SET content = :content,
    last_fencing_token = :token
WHERE report_id = :report_id
  AND last_fencing_token < :token
RETURNING report_id;
```

If the update returns a row, the token was newer than the stored token. If it returns no row, the operation was rejected—either because the token was stale or because the report row did not exist.

The database's atomic update protects the comparison and write against concurrent updates to that row.

### 6.2 An important subtlety

Suppose A has token `41`, B gets token `42`, but B has not yet written to the database. A's write with token `41` might still be accepted if the database's stored token is only `40`.

This is not necessarily a violation of fencing. The database has not yet observed token `42`.

Once the resource accepts token `42`, subsequent writes with token `41` will be rejected.

This means fencing guarantees that an older token cannot overwrite a newer token that the resource has already accepted. It does not automatically stop every older operation the instant a new lock is acquired.

If your business invariant requires stronger behavior, design the database transaction and state transitions accordingly.

### 6.3 Where should fencing tokens come from?

They need to be monotonically increasing and reliably associated with new ownership epochs.

A naive implementation might use:

```
INCR lock:fencing-counter
```

This is atomic on the Redis server executing the command. However, if the counter's latest value is lost during failover or recovery, a future owner could receive a token that is not greater than previously issued tokens.

Therefore, for strong correctness, the token generator must have failure and recovery semantics that preserve monotonicity. A database sequence, a suitable consensus-backed coordination service, or another carefully designed authoritative mechanism may be appropriate, depending on the system.

Key principle: A random UUID identifies an owner, but a fencing token establishes an ordering between owners. They solve different problems.

## 7. What is Redlock?

Redis has a distributed locking algorithm called Redlock. It was designed to acquire a lock across multiple independent Redis masters rather than relying on a single master.

Why consider this?

With a single Redis primary and asynchronous replication, this sequence can happen:

1. A acquires a lock on the primary.
2. The primary fails before replicating that lock.
3. A replica is promoted to primary.
4. B acquires the same lock on the new primary.

Both A and B may believe they acquired the lock.

This is possible because Redis replication is not generally a synchronous, consensus-based guarantee that every acknowledged lock write survives failover.

### 7.1 How Redlock works conceptually

Redlock uses multiple independent Redis masters.

For five masters:

\#chatgpt-mermaid-\_r_3qs\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_3qs\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_3qs\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_3qs\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_3qs\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3qs\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_3qs\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_3qs\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_3qs\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_3qs\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_3qs\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_3qs\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3qs\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3qs\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_3qs\_ p{margin:0;}#chatgpt-mermaid-\_r_3qs\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3qs\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3qs\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3qs\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_3qs\_ .label text,#chatgpt-mermaid-\_r_3qs\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3qs\_ .node rect,#chatgpt-mermaid-\_r_3qs\_ .node circle,#chatgpt-mermaid-\_r_3qs\_ .node ellipse,#chatgpt-mermaid-\_r_3qs\_ .node polygon,#chatgpt-mermaid-\_r_3qs\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_3qs\_ .rough-node .label text,#chatgpt-mermaid-\_r_3qs\_ .node .label text,#chatgpt-mermaid-\_r_3qs\_ .image-shape .label,#chatgpt-mermaid-\_r_3qs\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_3qs\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_3qs\_ .rough-node .label,#chatgpt-mermaid-\_r_3qs\_ .node .label,#chatgpt-mermaid-\_r_3qs\_ .image-shape .label,#chatgpt-mermaid-\_r_3qs\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_3qs\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_3qs\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3qs\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3qs\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_3qs\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_3qs\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_3qs\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_3qs\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_3qs\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_3qs\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_3qs\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3qs\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3qs\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_3qs\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_3qs\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_3qs\_ .icon-shape,#chatgpt-mermaid-\_r_3qs\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_3qs\_ .icon-shape p,#chatgpt-mermaid-\_r_3qs\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_3qs\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_3qs\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_3qs\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_3qs\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_3qs\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_3qs\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_3qs\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_3qs\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_3qs\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3qs\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_3qs\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_3qs\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_3qs\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3qs\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_3qs\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_3qs\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3qs\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_3qs\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_3qs\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3qs\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_3qs\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_3qs\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_3qs\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_3qs\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_3qs\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_3qs\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_3qs\_ .node rect,#chatgpt-mermaid-\_r_3qs\_ .node circle,#chatgpt-mermaid-\_r_3qs\_ .node ellipse,#chatgpt-mermaid-\_r_3qs\_ .node polygon,#chatgpt-mermaid-\_r_3qs\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_3qs\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_3qs\_ .node.mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_3qs\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_3qs\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_3qs\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}Lock clientRedis master 1Redis master 2Redis master 3Redis master 4Redis master 5

The algorithm broadly works as follows:

1. Generate a unique random lock token.
2. Attempt to acquire the same lock on multiple independent masters, using a TTL.
3. Measure the elapsed time.
4. Require a majority of masters to grant the lock.
5. Check that sufficient validity time remains after accounting for elapsed time and clock-drift assumptions.
6. If acquisition fails, release the lock from the masters where the client acquired it.
7. On release, remove the lock only when the token matches.

For five masters, a majority means at least three.

### 7.2 Does Redlock guarantee correctness in every failure scenario?

No distributed locking design should be treated as universally safe without examining its assumptions.

Redlock's guarantees and limitations have been debated. Its safety depends on assumptions about timing, independent failures, expiration, and the behavior of the participating nodes.

Even if a client successfully acquires a Redlock lease, a process pause can outlast the lease. The old client can then resume and continue executing unless the protected resource has its own safeguards.

Using multiple Redis masters is not the same thing as making the protected business operation transactional.

For correctness-sensitive writes, consider:

- Database transactions and constraints.
- Conditional updates or compare-and-swap operations.
- Fencing tokens enforced by the protected resource.
- A coordination system whose guarantees fit the required failure model.

Interview answer: Redlock aims to improve lock acquisition safety across multiple independent Redis masters, but it is not a substitute for resource-level correctness checks or a guarantee against every stale-owner scenario.

## 8. Redis locks vs PostgreSQL locks

Both coordinate concurrent operations, but they operate at different layers.

| Dimension                       | PostgreSQL row lock                          | Redis distributed lock                                 |
| ------------------------------- | -------------------------------------------- | ------------------------------------------------------ |
| Main purpose                    | Coordinate database transactions             | Coordinate application instances                       |
| Scope                           | Rows and database resources                  | A shared logical resource identified by a key          |
| Typical mechanism               | `SELECT ... FOR UPDATE`                      | `SET key token NX PX ttl`                              |
| Lock lifetime                   | Until transaction ends                       | Until explicit release or TTL expiration               |
| Crash handling                  | Database rolls back an abandoned transaction | Lease eventually expires if not renewed                |
| Protection against stale owners | Database transaction and locking semantics   | Requires careful lease handling; fencing may be needed |
| Atomic with a database update?  | Yes, when performed in the same transaction  | No, not automatically                                  |
| Typical use                     | Inventory, payments, booking records         | Coordinating jobs or expensive work across instances   |

### Example: Booking a seat

Suppose two application instances try to book the last available seat.

You might be tempted to do this:

```
Acquire Redis lock
Check seat availability
Insert booking
Release Redis lock
```

But if Redis loses the lock during failover or the lease expires while a client is paused, the application could have overlapping owners.

For a database-backed booking system, prefer making PostgreSQL enforce the invariant:

```
CREATE TABLE bookings (
    booking_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    show_id BIGINT NOT NULL,
    seat_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    UNIQUE (show_id, seat_id)
);
```

The unique constraint prevents two successful bookings of the same seat for the same show, even if two application instances race.

If the seat is represented by an inventory row, you can also use an atomic conditional update:

```
UPDATE seats
SET status = 'BOOKED'
WHERE show_id = :show_id
  AND seat_id = :seat_id
  AND status = 'AVAILABLE'
RETURNING seat_id;
```

If a row is returned, the transition succeeded. If no row is returned, the seat was not available or did not exist.

The booking insert and seat update should be in the same database transaction when both are required for correctness.

Lesson: Use Redis to coordinate work when it helps, but let the database enforce database invariants.

## 9. Production implementation checklist

When implementing a Redis lock, think beyond the happy path.

### Lock acquisition

- Use `SET key unique-token NX PX ttl` as one atomic command.
- Generate a new, unpredictable token for every acquisition attempt.
- Handle acquisition failure explicitly; don't execute the protected work without the lock if the lock is required.
- Use a bounded wait or retry policy rather than retrying indefinitely.

### Lock ownership and release

- Release only if the stored token matches the caller's token.
- Use an atomic Lua script for compare-and-delete.
- Do not use a blind `DEL`.
- Ensure every acquisition has a corresponding cleanup path, including exception handling.

### Expiration and long-running tasks

- Choose the TTL based on expected operation duration and acceptable recovery time.
- Renew the lease safely if work may exceed the TTL.
- Treat failed or uncertain renewal as potential loss of ownership.
- Use fencing or resource-level conditional writes when stale operations could cause harm.

### Failure handling

Consider what happens if:

- Redis becomes unavailable during acquisition.
- The network fails after Redis acquires the lock but before the client receives the response.
- The application crashes before releasing the lock.
- A lease expires while the application is paused.
- Redis fails over and loses a recently acknowledged lock write.
- A downstream database operation succeeds, but the application crashes before acknowledging success.

The network-failure case is especially important: a client may not know whether an acquisition succeeded. It should not assume ownership merely because it sent the command. A later retry must use a safe protocol, and any cleanup must be ownership-checked.

Also remember that a lock is not exactly-once execution. If a job must not produce duplicate side effects, use idempotency keys, unique constraints, durable job state, or other appropriate mechanisms alongside locking.

## 10. When should you use Redis locking?

### Good candidates

1\. Avoiding duplicate expensive work

Several application instances notice that the same cache entry is missing. A lock can coordinate a cache refresh so that usually one instance performs the expensive computation.

2\. Coordinating scheduled jobs

Multiple replicas run the same scheduler, but only one should start a particular maintenance task at a time.

3\. Distributed coordination

Multiple workers need to coordinate access to a shared resource that cannot be protected directly by a single database transaction.

For these cases, consider whether occasional duplicate work is tolerable or whether the lock must provide stronger correctness guarantees.

### When a Redis lock is unnecessary

Prefer a simpler mechanism when it can express the invariant directly.

| Requirement                                   | Often a better starting point               |
| --------------------------------------------- | ------------------------------------------- |
| Decrement inventory only if stock is positive | Atomic conditional SQL update               |
| Prevent duplicate booking of a seat           | Database unique constraint                  |
| Update only if the record has not changed     | Optimistic locking with a version           |
| Claim an available job from PostgreSQL        | Transaction with `FOR UPDATE SKIP LOCKED`   |
| Increment a Redis counter                     | Atomic `INCR`                               |
| Prevent duplicate payment processing          | Idempotency key plus durable database state |

A distributed lock introduces operational and failure-mode complexity. Don't add it unless it solves a coordination problem that a simpler primitive cannot adequately handle.

## 11. Common Redis locking interview questions

### Q1. Why do we use `NX`?

`NX` means "set only if the key does not exist." It prevents a client from overwriting an existing lock during acquisition.

### Q2. Why should we use `PX` or `EX`?

They set the lock's expiration at acquisition time, reducing the risk of a lock remaining forever after a client crashes.

`PX` uses milliseconds; `EX` uses seconds.

### Q3. Why is a unique token required?

It identifies the specific acquisition. Without it, an old owner could accidentally delete a lock acquired by a newer owner.

### Q4. Why can't we use `GET` followed by `DEL`?

Another client can acquire the lock between the two commands. The check and deletion must be atomic, such as through a Lua script.

### Q5. What happens if a lock expires while the owner is still working?

Another client can acquire the lock while the original owner continues executing. This can cause overlapping work. A TTL alone does not prevent stale operations.

### Q6. What is the difference between a lease and a lock?

A lease grants ownership for a limited period. Redis locks implemented with TTLs are effectively leased locks. When the lease expires, the system may grant ownership to someone else, even if the previous owner has not stopped working.

### Q7. What are fencing tokens?

Monotonically increasing tokens associated with successive owners. The protected resource checks the token and rejects stale operations after it has accepted a newer token.

### Q8. Does Redlock guarantee that two clients can never execute the same task simultaneously?

No. Redlock is designed to coordinate acquisition across multiple independent Redis masters under particular assumptions. It does not forcibly stop an expired owner from executing or eliminate every stale-operation risk.

### Q9. Should I use Redis locks for financial transactions?

Not as the sole correctness mechanism. Financial operations generally require durable database state, transactions, constraints, and idempotency. A Redis lock may provide additional coordination, but it should not replace those safeguards.

### Q10. What should I choose: Redis lock or PostgreSQL row lock?

If the critical operation is a database state transition, start with a database transaction, conditional update, constraint, or row lock. If the coordination spans application instances or external work, a Redis lock may be useful, but analyze lease expiry and stale-owner behavior.

## 12. A strong interview answer

Question: How would you implement distributed locking using Redis?

A good answer would be:

> I would acquire a lock using Redis `SET` with `NX` and a TTL, for example `SET lock:resource unique-token NX PX 30000`. The command atomically checks whether the lock exists, creates it if available, and sets its expiration.
>
> Each acquisition gets a unique token. On release, I would use a Lua script to compare the stored token with my token and delete the key only if they match. I would never blindly delete the lock because the original lease might have expired and another client might have acquired the key.
>
> For long-running work, I would consider safe lease renewal, but I would not assume that renewal or a TTL alone prevents overlapping execution. A paused client can resume after its lease expires. If stale writes are dangerous, I would use fencing tokens or enforce the invariant at the protected resource, such as with a database transaction, conditional update, or unique constraint.
>
> Finally, I would use Redis locking only when shared coordination is actually required. For database invariants, I would prefer database-native mechanisms whenever possible.

## Chapter 5 summary

Remember these five principles:

1. Acquire atomically: `SET key token NX PX ttl`.
2. Release safely: compare the token and delete atomically.
3. Understand expiration: the lock can expire while the owner is still working.
4. Protect the resource: use fencing tokens or resource-level invariants when stale operations are dangerous.
5. Choose the simplest correct primitive: Redis locks are useful, but they are not a replacement for transactions, constraints, or idempotency.

Next chapter — Chapter 6: Distributed locking

We'll connect these concepts to distributed systems more broadly, including leases, leader election, failure detection, split-brain scenarios, consensus, and how to choose between Redis, PostgreSQL, and coordination systems such as ZooKeeper or etcd.

Type `next` when you're ready.

