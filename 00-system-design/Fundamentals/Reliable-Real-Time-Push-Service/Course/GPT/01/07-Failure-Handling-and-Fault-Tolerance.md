# Chapter 7 — Failure Handling and Fault Tolerance

So far, we've designed a system that can deliver messages reliably under normal operating conditions.

Now let's assume things start going wrong.

- A push server suddenly crashes with 100,000 active connections.
- Redis becomes unavailable.
- Kafka loses a broker.
- Routing workers get stuck.
- A push server is healthy but cannot communicate with Redis.
- One user receives messages much slower than everyone else.
- Retries overwhelm the infrastructure.
- A network partition causes different components to disagree about which server owns a connection.

How do we prevent one failure from bringing down the entire system?

This chapter is about designing the system so that failures remain isolated, recoverable, and don't cascade into other components.

The most important mindset:

> Production systems are not designed with the assumption that components won't fail. They're designed with the assumption that components will fail, and that the rest of the system must continue behaving predictably.

We'll examine failures component by component and then build a unified fault-tolerance strategy.

## 1. First understand the different failure domains

Let's revisit our architecture.

\#chatgpt-mermaid-\_r_9r1\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_9r1\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_9r1\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_9r1\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_9r1\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_9r1\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_9r1\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_9r1\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_9r1\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_9r1\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_9r1\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_9r1\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_9r1\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_9r1\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_9r1\_ p{margin:0;}#chatgpt-mermaid-\_r_9r1\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_9r1\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_9r1\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_9r1\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_9r1\_ .label text,#chatgpt-mermaid-\_r_9r1\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_9r1\_ .node rect,#chatgpt-mermaid-\_r_9r1\_ .node circle,#chatgpt-mermaid-\_r_9r1\_ .node ellipse,#chatgpt-mermaid-\_r_9r1\_ .node polygon,#chatgpt-mermaid-\_r_9r1\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_9r1\_ .rough-node .label text,#chatgpt-mermaid-\_r_9r1\_ .node .label text,#chatgpt-mermaid-\_r_9r1\_ .image-shape .label,#chatgpt-mermaid-\_r_9r1\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_9r1\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_9r1\_ .rough-node .label,#chatgpt-mermaid-\_r_9r1\_ .node .label,#chatgpt-mermaid-\_r_9r1\_ .image-shape .label,#chatgpt-mermaid-\_r_9r1\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_9r1\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_9r1\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_9r1\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_9r1\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_9r1\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_9r1\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_9r1\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_9r1\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_9r1\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_9r1\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_9r1\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_9r1\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_9r1\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_9r1\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_9r1\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_9r1\_ .icon-shape,#chatgpt-mermaid-\_r_9r1\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_9r1\_ .icon-shape p,#chatgpt-mermaid-\_r_9r1\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_9r1\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_9r1\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_9r1\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_9r1\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_9r1\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_9r1\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_9r1\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_9r1\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_9r1\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_9r1\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_9r1\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_9r1\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_9r1\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_9r1\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_9r1\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_9r1\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_9r1\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_9r1\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_9r1\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_9r1\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_9r1\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_9r1\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_9r1\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_9r1\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_9r1\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_9r1\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_9r1\_ .node rect,#chatgpt-mermaid-\_r_9r1\_ .node circle,#chatgpt-mermaid-\_r_9r1\_ .node ellipse,#chatgpt-mermaid-\_r_9r1\_ .node polygon,#chatgpt-mermaid-\_r_9r1\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_9r1\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_9r1\_ .mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_9r1\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_9r1\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_9r1\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}ClientsLoad BalancerPush Server 1Push Server 2Push Server 3Redis Connection RegistryBusiness ServicesTransactional OutboxKafkaRouting WorkersDelivery StoreDelivery Workers

We have several independent failure domains.

| Component         | Its responsibility                 | What breaks when it fails?        |
| ----------------- | ---------------------------------- | --------------------------------- |
| Load balancer     | Establish new connections          | New connection establishment      |
| Push server       | Maintain and deliver over sockets  | Connections owned by that server  |
| Redis             | Distributed connection metadata    | Cross-server connection lookup    |
| Kafka             | Durable event distribution         | Event publication and consumption |
| Routing workers   | Resolve audiences and destinations | New event routing                 |
| Delivery workers  | Process pending deliveries         | Delivery throughput               |
| Delivery database | Persist delivery state             | Durable delivery processing       |
| Business database | Store authoritative data           | Business operations and recovery  |

Notice something important.

Not every component failing should cause the entire application to become unavailable.

For example:

- If Redis fails, existing WebSocket connections may continue working.
- If Kafka fails, already-connected clients may continue communicating, but new durable events may accumulate in the outbox.
- If one push server fails, clients connected to other servers should remain unaffected.
- If one delivery worker fails, other workers should continue processing.

This is called fault isolation.

## 2. Introduce the three states of a component

When something goes wrong, don't think only in binary terms:

```
UP / DOWN
```

A component can be:

Healthy

It performs its intended operations successfully.

Degraded

It is still operating, but some functionality or performance is impaired.

Unavailable

It cannot provide the required functionality.

For example, imagine Redis is unavailable.

Our push servers may still have their local connection maps.

```
Push Server 1:
    User A -> Local connection
    User B -> Local connection
```

It can still send messages received through an already-established local processing path.

But it cannot reliably look up connections belonging to other servers.

Therefore:

```
Local socket delivery -> Potentially healthy

Cross-server routing -> Degraded

New connection registration -> Potentially unavailable
```

This is a much better way to reason about failures than declaring the whole push service down.

# Part A — Push Server Failures

## 3. Scenario 1: One push server suddenly crashes

Suppose we have:

```
                  Load Balancer
                 /      |      \
                ▼       ▼       ▼
             Push 1   Push 2   Push 3
```

Connection distribution:

```
Push 1:
    100,000 users

Push 2:
    100,000 users

Push 3:
    100,000 users
```

Now Push Server 1 crashes.

What happens?

### Step 1: Its connections terminate

All 100,000 TCP connections owned by Push Server 1 are lost.

There is no way to transfer those existing TCP connections to Push Server 2 or Push Server 3.

Remember Chapter 2:

Connection ownership is tied to the actual network socket.

When the owning server dies, that connection dies.

### Step 2: The load balancer detects the failure

The load balancer stops directing new connections to Push Server 1.

Health checks and infrastructure orchestration help identify the failed instance.

### Step 3: The connection registry contains stale information

Redis might still contain:

```
User A -> conn-101 -> Push 1
User B -> conn-102 -> Push 1
User C -> conn-103 -> Push 1
```

But Push Server 1 no longer exists.

We cannot depend on graceful cleanup because the process crashed.

This is why Chapter 5 introduced leases.

When the server-instance lease expires, its connections must be considered unavailable for routing.

We can also clean up the corresponding reverse indexes asynchronously.

### Step 4: Clients reconnect

The clients discover that their connections have been terminated.

They reconnect through the load balancer.

```
Before:

User A -> Push 1

After:

User A -> Push 2
```

Or perhaps Push Server 3.

### Step 5: Recover missed messages

This is where Chapter 6 becomes important.

If the client missed important messages while disconnected, it resumes from its last processed cursor.

For example:

```
Last processed sequence = 105
```

It retrieves:

```
106
107
108
109
```

from the authoritative business history or durable event store.

### Step 6: Continue normal delivery

Once the new connection is registered, the routing system can locate it again.

### Complete recovery flow

\#chatgpt-mermaid-\_r_9sc\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_9sc\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_9sc\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_9sc\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_9sc\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_9sc\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_9sc\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_9sc\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_9sc\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_9sc\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_9sc\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_9sc\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_9sc\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_9sc\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_9sc\_ p{margin:0;}#chatgpt-mermaid-\_r_9sc\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_9sc\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_9sc\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_9sc\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_9sc\_ .label text,#chatgpt-mermaid-\_r_9sc\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_9sc\_ .node rect,#chatgpt-mermaid-\_r_9sc\_ .node circle,#chatgpt-mermaid-\_r_9sc\_ .node ellipse,#chatgpt-mermaid-\_r_9sc\_ .node polygon,#chatgpt-mermaid-\_r_9sc\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_9sc\_ .rough-node .label text,#chatgpt-mermaid-\_r_9sc\_ .node .label text,#chatgpt-mermaid-\_r_9sc\_ .image-shape .label,#chatgpt-mermaid-\_r_9sc\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_9sc\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_9sc\_ .rough-node .label,#chatgpt-mermaid-\_r_9sc\_ .node .label,#chatgpt-mermaid-\_r_9sc\_ .image-shape .label,#chatgpt-mermaid-\_r_9sc\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_9sc\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_9sc\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_9sc\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_9sc\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_9sc\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_9sc\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_9sc\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_9sc\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_9sc\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_9sc\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_9sc\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_9sc\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_9sc\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_9sc\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_9sc\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_9sc\_ .icon-shape,#chatgpt-mermaid-\_r_9sc\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_9sc\_ .icon-shape p,#chatgpt-mermaid-\_r_9sc\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_9sc\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_9sc\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_9sc\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_9sc\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_9sc\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_9sc\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_9sc\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_9sc\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_9sc\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_9sc\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_9sc\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_9sc\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_9sc\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_9sc\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_9sc\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_9sc\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_9sc\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_9sc\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_9sc\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_9sc\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_9sc\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_9sc\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_9sc\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_9sc\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_9sc\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_9sc\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_9sc\_ .node rect,#chatgpt-mermaid-\_r_9sc\_ .node circle,#chatgpt-mermaid-\_r_9sc\_ .node ellipse,#chatgpt-mermaid-\_r_9sc\_ .node polygon,#chatgpt-mermaid-\_r_9sc\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_9sc\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_9sc\_ .mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_9sc\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_9sc\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_9sc\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}Push Server 1 CrashesExisting Connections LostLoad Balancer Stops NewTrafficServer Lease ExpiresOld Connection OwnershipBecomes InvalidClients ReconnectLoad Balancer SelectsHealthy ServersNew Connection RegistrationReplay Missing EventsNormal Delivery Resumes

### What if 100,000 clients reconnect simultaneously?

Now we have another problem.

The server crash itself might be isolated, but the resulting reconnect storm can take down the surviving servers.

We need to address this separately.

## 4. Prevent reconnect storms

Imagine three servers normally handle 300,000 connections.

One fails.

The other two now have to absorb the reconnecting clients.

If every client reconnects immediately:

```
Push 1 crashes
      |
      ▼
100,000 clients disconnect
      |
      ▼
100,000 clients reconnect immediately
      |
      ▼
Push 2 and Push 3 overloaded
      |
      ▼
More connection failures
      |
      ▼
More reconnects
```

This creates a cascading failure.

### Solution 1: Exponential backoff with jitter

We've already discussed this in Chapter 2.

A client progressively increases its retry delay.

For example:

```
Attempt 1 -> Random delay up to 1s
Attempt 2 -> Random delay up to 2s
Attempt 3 -> Random delay up to 4s
Attempt 4 -> Random delay up to 8s
Attempt 5 -> Random delay up to 16s
```

The delay should have a cap.

The jitter ensures clients don't reconnect in synchronized waves.

### Solution 2: Connection admission control

Suppose the remaining servers have capacity for only 50,000 additional connections.

We shouldn't blindly accept 100,000 new connections at once.

We can enforce:

- Maximum connection count per server.
- Maximum concurrent handshakes.
- Maximum authentication requests per second.
- Rate limits on reconnect attempts.
- Admission queues or temporary rejection.

For example:

```
Push Server 2:

Current connections: 100,000
Maximum connections: 130,000

Available capacity: 30,000
```

Once capacity is exhausted, new connections should be rejected or delayed in a controlled way.

The clients retry using backoff.

### Solution 3: Keep spare capacity

Don't run every push server at its absolute connection limit.

Suppose three servers have a combined capacity of 300,000 connections.

Instead of running at 100,000 each, we might deliberately operate at a lower level.

This gives the cluster some ability to absorb a failure.

The actual reserve depends on:

- Failure scenarios you intend to tolerate.
- Connection resource usage.
- Message fanout.
- Traffic burstiness.
- Regional capacity.

### Solution 4: Protect authentication services

Reconnection can also create an authentication storm.

Every reconnect might trigger:

- Token validation.
- Session lookup.
- Authorization.
- Database access.

If our identity service cannot handle the burst, connections may fail even though the push servers have enough capacity.

Use appropriate local verification of signed credentials, cached public keys, rate limits, and controlled authentication-service dependencies.

Do not perform expensive database lookups on every heartbeat.

### Key lesson

The system must have enough capacity and controlled admission to survive the failure of a component, not merely recover after that component restarts.

## 5. Scenario 2: Graceful deployment or planned restart

A server crash is not the only reason connections terminate.

We also need to deploy new versions.

Suppose Push Server 1 needs restarting.

We should use connection draining.

```
Push Server 1
      |
      ▼
Mark as DRAINING
      |
      ▼
Stop accepting new connections
      |
      ▼
Load balancer removes it from new traffic
      |
      ▼
Ask existing clients to reconnect
      |
      ▼
Wait for connections to drain
      |
      ▼
Close remaining connections
      |
      ▼
Shutdown
```

There are a few subtleties.

Don't remove it from every routing destination immediately.

It still owns existing connections and may need to deliver pending messages while clients reconnect.

Don't stop processing every event immediately.

You need a defined policy for what happens to work already assigned to the server.

Don't shut down before cleaning up its registry ownership.

The cleanup must be conditional on its exact server instance/generation.

Use a termination grace period.

It should allow sufficient time for clients to reconnect and for local connection cleanup, while still enforcing a hard shutdown deadline.

A rolling deployment usually repeats this process for one batch of servers at a time.

This avoids disconnecting the entire user population simultaneously.

# Part B — Redis / Connection Registry Failures

## 6. Scenario 3: Redis suddenly becomes unavailable

Let's assume Redis stores:

```
User A -> conn-101 -> Push 1
User B -> conn-102 -> Push 2
User C -> conn-103 -> Push 3
```

Redis suddenly goes down.

What happens to the existing connections?

They don't automatically disconnect.

Remember:

- Actual sockets are in push-server memory.
- Redis only stores distributed connection metadata.

Therefore, existing push servers can potentially continue communicating with their locally connected users.

But cross-server routing becomes difficult.

Let's examine the different operations.

| Operation                                                | Redis unavailable                             |
| -------------------------------------------------------- | --------------------------------------------- |
| Send to an already-known local connection                | Can continue                                  |
| Register a new distributed connection                    | Cannot reliably register                      |
| Find another server's connection                         | Cannot reliably look up                       |
| Renew Redis-based connection leases                      | Cannot renew                                  |
| Remove connection metadata                               | Cannot guarantee cleanup                      |
| Deliver through a local subscription already established | May continue, depending on the messaging path |

### Should we disconnect every user immediately?

No.

That would turn a Redis outage into a complete push-service outage unnecessarily.

Instead, define a degraded mode.

For example:

```
Redis DOWN

Existing local connections:
    Continue serving where possible.

New connections:
    Temporarily reject or hold in a registration-pending state.

Individual cross-server routing:
    Retry or use a predesigned fallback.

Group/global delivery:
    Continue only where the required routing path is healthy.

Durable events:
    Remain in Kafka / delivery store.
```

This avoids unnecessarily destroying healthy connections.

However, there's a security and consistency consideration.

If we cannot verify ownership or authorization because Redis is unavailable, we should not blindly deliver to potentially stale or unauthorized destinations.

For sensitive messages, failing safely is better than broadcasting to every server as a fallback.

## 7. What if Redis is slow instead of completely down?

This is another failure scenario.

Imagine Redis is responding, but its latency increases from 2 ms to 2 seconds.

Our router performs:

```
Lookup User A -> 2 seconds
Lookup User B -> 2 seconds
Lookup User C -> 2 seconds
```

Messages start accumulating.

Delivery workers become blocked waiting for Redis.

More workers are created.

Those workers also wait for Redis.

Now we've increased resource consumption without increasing throughput.

Eventually, the delivery system becomes overloaded.

This is a classic cascading failure.

### Introduce timeouts

Every Redis operation should have a bounded timeout.

For example:

```
Connection lookup timeout = 100 ms
```

This is illustrative; actual values should come from measured latency distributions and the end-to-end delivery latency budget.

We should not allow a worker to wait indefinitely.

### Introduce a circuit breaker

A circuit breaker prevents continuous calls to a dependency that is known to be unhealthy.

It has three conceptual states:

```
             CLOSED
               |
               | Failure threshold reached
               ▼
              OPEN
               |
               | Wait for recovery interval
               ▼
           HALF-OPEN
               |
       ┌───────┴────────┐
       |                |
    Success           Failure
       |                |
       ▼                ▼
    CLOSED             OPEN
```

CLOSED: Requests flow normally.

OPEN: Requests are rejected quickly without continuously calling the unhealthy dependency.

HALF-OPEN: A small number of requests test whether the dependency has recovered.

For our registry:

```
Redis healthy:
    Lookup connections normally.

Redis failing repeatedly:
    Open circuit.
    Stop flooding Redis.

Redis recovery interval:
    Allow limited test requests.

Redis recovered:
    Close circuit.
```

What do we do with rejected requests?

That depends on the message.

For important notifications, retain them in the durable delivery store and retry routing later.

For ephemeral updates, discard or coalesce them.

A circuit breaker doesn't itself recover messages. It protects the system from making an unhealthy dependency worse.

## 8. What if Redis fails over and loses some connection metadata?

Suppose Redis recovers, but some recently registered connection metadata is missing.

Push Server 1 still has:

```
conn-101 -> WebSocketSession
conn-102 -> WebSocketSession
```

But Redis doesn't know about these connections.

We need registry reconciliation.

```
Redis recovers
      |
      ▼
Push servers detect recovery
      |
      ▼
Enumerate local active connections
      |
      ▼
Re-register ownership metadata
      |
      ▼
Mark server as fully routable
```

During re-registration, the server should not be treated as fully ready for cross-server routing.

Also, the router should distinguish:

```
SERVER RECOVERING
```

from:

```
SERVER HEALTHY AND ROUTABLE
```

This prevents messages from being dispatched to a server before its ownership information has been restored.

### Should we keep local connection ownership caches?

Yes, local ownership is already maintained by the push server.

But using local caches as a global fallback is much harder.

For example, Push Server 1 cannot query Push Server 2's process memory directly.

We could introduce a server-discovery or broadcast fallback, but blindly broadcasting every individual message during a Redis outage can cause enormous load.

I'd rather:

- Keep existing local connections alive.
- Preserve important events durably.
- Temporarily degrade cross-server routing.
- Restore the registry.
- Replay pending delivery work.

The objective is graceful degradation, not pretending every operation can continue when its required dependency is unavailable.

# Part C — Kafka Failures

## 9. Scenario 4: One Kafka broker crashes

Suppose we have a Kafka cluster with three brokers:

```
              Kafka Cluster

        ┌──────────┬──────────┐
        |          |          |
        ▼          ▼          ▼
      Broker 1  Broker 2  Broker 3
```

Our topic has replicated partitions.

For example:

```
Partition 0:
    Leader: Broker 1
    Follower: Broker 2
    Follower: Broker 3
```

Now Broker 1 crashes.

What happens?

Kafka can elect an eligible in-sync replica as the new partition leader.

For example:

```
Partition 0:

Before:
    Leader: Broker 1
    Follower: Broker 2
    Follower: Broker 3

After:
    Leader: Broker 2
    Follower: Broker 3
    Broker 1: Down
```

Producers and consumers refresh metadata and continue communicating with the new leader.

This works because Kafka maintains replicas.

But we need to understand the durability configuration.

### 9.1 Replication factor

Suppose we configure:

```
Replication Factor = 3
```

Each partition has three replicas distributed across brokers.

If one broker fails, other replicas may still have the partition data.

But replication factor alone does not automatically guarantee that every recently acknowledged event is safe.

The producer acknowledgement and minimum in-sync replica settings matter.

### 9.2 Producer acknowledgements

Kafka producers can use different acknowledgement settings.

| Setting    | Meaning                                                       |
| ---------- | ------------------------------------------------------------- |
| `acks=0`   | Producer doesn't wait for broker acknowledgement              |
| `acks=1`   | Leader acknowledges the write                                 |
| `acks=all` | Leader waits for the required in-sync replicas to acknowledge |

For important event streams, a common starting configuration is:

```
replication.factor = 3

acks = all

min.insync.replicas = 2

enable.idempotence = true
```

Let's understand this.

With replication factor 3 and minimum in-sync replicas 2:

- The partition has three configured replicas.
- At least two in-sync replicas must be available to accept writes with `acks=all`.
- If only one eligible in-sync replica remains, the system should reject writes rather than silently weaken the configured durability requirement.

This creates a conscious availability-versus-durability tradeoff.

We prefer refusing a write temporarily rather than acknowledging a write without the required replication.

### 9.3 What happens if two brokers are down?

Suppose:

```
Broker 1: DOWN
Broker 2: DOWN
Broker 3: UP
```

Only one replica remains.

With `min.insync.replicas=2`, writes requiring `acks=all` fail.

The business service's outbox publisher should not mark the event as published.

It should retain it for retry.

```
Kafka unavailable for required durability
               |
               ▼
Outbox event remains PENDING
               |
               ▼
Retry when Kafka recovers
```

This is exactly why the Transactional Outbox is useful.

The event is not lost just because Kafka is temporarily unavailable.

### Important distinction

Kafka replication protects the event log against broker failures.

It does not replace:

- The business database.
- Transactional Outbox.
- Consumer idempotency.
- Delivery records.
- Client acknowledgement.

Each protects a different failure boundary.

## 10. What happens when a Kafka consumer crashes?

Suppose our router consumer group looks like this:

```
Topic: chat-events

Partition 0 -> Router 1
Partition 1 -> Router 2
Partition 2 -> Router 3
```

Router 2 crashes.

Kafka detects that the consumer has left the group and triggers a rebalance.

Its assigned partitions are reassigned to surviving consumers.

For example:

```
Before:

P0 -> Router 1
P1 -> Router 2
P2 -> Router 3

After:

P0 -> Router 1
P1 -> Router 3
P2 -> Router 3
```

Now Router 3 processes Partition 1.

But what if Router 2 was halfway through an event?

For example:

```
1. Router 2 consumes Event 101.

2. Router 2 persists two delivery records.

3. Router 2 crashes before committing its offset.
```

The new consumer may receive Event 101 again.

This is expected.

Our idempotent processing logic ensures it doesn't create duplicate logical deliveries.

### A practical rule

For Kafka consumers that write to an external database:

```
1. Consume record.

2. Perform idempotent database transaction.

3. Commit database transaction.

4. Commit Kafka offset.
```

Do not commit the Kafka offset before the durable processing work is complete.

Also, don't commit offsets beyond records that have actually completed when processing records concurrently.

Kafka consumers have more complexity around partition assignment and offset commits than a simple queue worker. In production, use a framework that manages partition lifecycle and offset discipline deliberately.

### What about Kafka consumer lag?

Suppose events are produced at:

```
10,000 events/second
```

But our routing workers process:

```
7,000 events/second
```

Backlog grows at:

```
3,000 events/second
```

Kafka consumer lag increases.

This means events are waiting to be processed.

It doesn't necessarily mean events are lost.

We need to monitor lag and add capacity or reduce incoming work before retention or latency objectives are breached.

But be careful:

Adding more consumers only helps if there are enough partitions and the workload can be parallelized.

If one hot key dominates a partition, adding more consumers won't make that single partition process faster.

## 11. What if a push server cannot reach Kafka?

This is an important architectural question.

Should every push server directly consume Kafka?

Sometimes yes, but it introduces coupling.

Imagine:

```
Kafka
  |
  ├── Push Server 1
  ├── Push Server 2
  └── Push Server 3
```

Push Server 2 loses Kafka connectivity.

Its WebSocket connections are still alive.

But it might not receive new events.

Now the push server is simultaneously responsible for:

- Connection management.
- Kafka consumption.
- Consumer group lifecycle.
- Event routing.
- Socket delivery.

This makes its responsibilities more complicated.

A cleaner architecture often looks like:

```
Kafka
  |
  ▼
Routing Workers
  |
  ▼
Target Dispatch Layer
  |
  ├── Push Server 1
  ├── Push Server 2
  └── Push Server 3
```

Push servers don't need to know about Kafka.

They receive targeted delivery requests through a simpler internal mechanism.

Advantages:

- Kafka consumer failures are isolated from connection management.
- Push servers can restart independently.
- Router workers can scale separately.
- The delivery layer can introduce retries and rate limits.
- We can use different transport mechanisms between routers and push servers.

The tradeoff is another routing/dispatch component to operate.

For a large reliable platform, this separation is generally worth it.

# Part D — Routing and Delivery Worker Failures

## 12. Scenario 5: All routing workers crash

Suppose Kafka is healthy.

Events are continuously arriving:

```
Kafka:

evt-101
evt-102
evt-103
evt-104
evt-105
...
```

But every routing worker crashes.

What happens?

The events remain in Kafka.

The consumer group stops advancing its offsets.

Consumer lag increases.

```
Business Services
       |
       ▼
     Kafka
       |
       ▼
  Router Workers
       X
    ALL DOWN
```

This is actually a relatively recoverable failure because Kafka retains the events.

When routing workers restart:

1. They join the consumer group.
2. Kafka assigns partitions.
3. They resume from committed offsets.
4. Previously unprocessed events are consumed.
5. Idempotency prevents duplicate logical deliveries.

### Should incoming business requests stop?

Not necessarily.

Suppose the chat service is healthy and Kafka can still accept events.

The business service can continue storing messages and publishing events.

The router backlog grows temporarily.

However, we must monitor:

- Kafka consumer lag.
- Age of the oldest unprocessed event.
- Remaining Kafka retention.
- Outbox backlog.
- Available delivery-store capacity.

If the outage continues for too long, we may need admission control or backpressure on business producers.

For example, a chat system might continue accepting messages while warning of delayed notifications.

A payment system might require stricter downstream processing guarantees before acknowledging certain operations.

The product semantics determine the response.

## 13. Scenario 6: A delivery worker crashes halfway through delivery

Suppose we have:

```
Delivery Worker 1:
    del-101
    del-102
    del-103
```

It claims `del-101`.

Then:

```
1. Worker claims delivery.

2. Worker resolves User A.

3. Worker dispatches to Push Server 1.

4. Worker crashes before recording completion.
```

What happens to `del-101`?

It must become available for recovery.

This is why delivery records need a claim/lease mechanism.

### Introduce delivery leases

A worker claims a delivery temporarily:

```
deliveryId = del-101

status = IN_PROGRESS

workerId = delivery-worker-1

leaseExpiresAt = 10:05:00
```

The worker must finish before the lease expires or renew it.

If the worker crashes:

```
Lease expires
      |
      ▼
Another worker can reclaim del-101
```

This is similar to connection ownership leases, but the purpose is different.

- Connection lease: Who owns a live connection?
- Delivery lease: Who currently owns the processing attempt?

We should distinguish them.

### The duplicate problem

Suppose Worker 1 actually succeeded in dispatching the message but crashed before recording success.

Worker 2 reclaims it.

It might dispatch the same message again.

Therefore, every delivery uses a stable identifier:

```
deliveryId = del-101
```

The downstream system should tolerate repeated dispatch requests.

For example, Push Server 1 can maintain a short-lived deduplication cache for delivery IDs it has already accepted.

But this cache is only an optimization.

For strong delivery guarantees, the durable delivery record and client-side deduplication remain important.

### Do not hold a delivery lease forever

If a worker crashes, a delivery record should not remain permanently stuck in `IN_PROGRESS`.

Use:

- Lease expiration.
- Periodic recovery of expired claims.
- Conditional completion based on worker ownership.
- Bounded processing time.

For example:

```
Complete del-101 only if:

status = IN_PROGRESS
AND workerId = delivery-worker-1
AND claimGeneration = 7
```

This prevents an old worker from completing or modifying a delivery after another worker has reclaimed it.

It is the same ownership principle we used in the connection registry.

## 14. Scenario 7: One routing worker becomes extremely slow

Suppose:

```
Router 1 -> Processes 1,000 events/sec
Router 2 -> Processes 1,000 events/sec
Router 3 -> Processes 10 events/sec
```

Router 3 might be:

- Blocked by Redis.
- Experiencing garbage collection pauses.
- Performing expensive group membership lookups.
- Processing an unusually large audience.
- Waiting for an unresponsive push server.

We need to prevent a single slow worker from dragging down the whole pipeline.

### Solution 1: Bounded work batches

Don't let a worker fetch an unlimited number of records and accumulate them in memory.

Use bounded polling and batch sizes.

### Solution 2: Timeouts on external calls

Every call to:

- Redis.
- Group membership service.
- Delivery database.
- Push dispatch endpoint.

Should have a bounded timeout.

### Solution 3: Separate expensive fanout work

Consider two events:

```
Event A: Individual notification for User A.

Event B: Broadcast to 10 million users.
```

If the same workers process both synchronously, the large broadcast can monopolize processing capacity.

We can isolate them:

```
Kafka
  |
  ├── Individual Delivery Router
  |
  └── Large Fanout Router
```

Or have the router create a separate fanout job that is processed by another worker pool.

This is called workload isolation.

### Solution 4: Per-audience rate limiting

A single extremely active group should not overwhelm every other group.

We can enforce:

- Maximum concurrent fanout jobs per group.
- Per-tenant throughput limits.
- Per-user delivery rates.
- Maximum destinations per dispatch batch.

This makes the system more predictable.

# Part E — Network Partitions and Split Brain

## 15. Scenario 8: Push Server is alive but isolated from the network

This is more dangerous than an ordinary crash.

Imagine Push Server 1 is healthy from a CPU and memory perspective.

It still has 100,000 local sockets.

But it cannot communicate with Redis.

```
              Redis
                |
                X
                |
            Push 1
                |
             Clients
```

Meanwhile, Push Server 2 can reach Redis.

What does the system believe?

There are two possibilities.

Push Server 1 believes:

```
I'm alive.
I own these 100,000 connections.
```

The registry might eventually believe:

```
Push Server 1 lease expired.
Its connections are no longer routable.
```

This is a network partition.

The server and the registry disagree about ownership.

### What happens if User A reconnects to Push Server 2?

Suppose User A's old connection to Push Server 1 is still locally open.

The new connection registers on Push Server 2.

Now:

```
Push 1:
    User A -> conn-101

Push 2:
    User A -> conn-202
```

We might temporarily have two active connections for the same device or session.

This can cause:

- Duplicate delivery.
- Duplicate presence.
- Conflicting connection ownership.
- Incorrect device-level session behavior.

This is why connection IDs and ownership generations are important.

But there is an even more important rule.

A push server must not continue acting as an authorized global owner merely because its local socket map says it owns a connection.

Its ownership must be validated against the distributed system's current state when required.

### How should Push Server 1 behave?

It can continue serving existing local connections for some purposes.

But it should recognize that it cannot guarantee distributed ownership or routing correctness.

For sensitive workflows, it may need to:

- Stop accepting new connections.
- Stop renewing its server lease.
- Stop receiving new targeted dispatch.
- Notify clients of a degraded state.
- Begin controlled reconnection.
- Shut down or isolate itself after an appropriate failure threshold.

This is a deliberate availability-versus-consistency decision.

For a casual dashboard, temporary local delivery might be acceptable.

For financial commands or exclusive sessions, a disconnected owner should not continue accepting authoritative operations.

## 16. Introduce fencing tokens

Let's understand the problem with an example.

Push Server 1 originally owns a particular connection.

Its generation is:

```
generation = 17
```

Its lease expires.

A new ownership is established with:

```
generation = 18
```

Now Push Server 1 reconnects to Redis and tries to perform an operation using generation 17.

The system rejects it because the current generation is 18.

This is a fencing mechanism.

Conceptually:

```
Ownership:

User A / Device 1

Old owner:
    Push 1
    Generation 17

New owner:
    Push 2
    Generation 18
```

An operation from generation 17 is stale.

The destination system accepts only generation 18.

This protects against delayed operations from old owners.

### Where should we enforce fencing?

Wherever stale ownership could cause incorrect behavior:

- Connection registry updates.
- Device-exclusive session management.
- Presence transitions.
- Server ownership claims.
- Delivery state updates.

Unique instance IDs alone prevent accidental identity reuse.

Monotonically increasing fencing generations give stronger protection when ownership can move between competing owners.

A critical nuance:

A lease can expire because of a false suspicion, such as a long network pause.

The old server might still be alive.

Therefore, fencing is stronger than relying on a lease alone.

But it only protects operations that actually validate the fencing token. It cannot magically terminate a TCP connection on the old server.

## 17. Avoid split-brain delivery decisions

Imagine a device should have only one active connection.

Two servers independently believe they own it.

```
                 Device A

             ┌─────┴─────┐
             |           |
             ▼           ▼
          Push 1      Push 2
          Believes    Believes
          owner       owner
```

Both may deliver commands.

We need an authoritative ownership transition.

One possible sequence:

```
1. New connection requests ownership.

2. Registry performs an atomic ownership update.

3. New generation is assigned.

4. Previous generation becomes invalid.

5. Old push server receives an invalidation message.

6. Old connection is closed or restricted.
```

The ownership transition must be serialized where exclusivity is required.

For example, Redis Lua scripts or a transactional database update can perform an atomic compare-and-set against the current generation, depending on the storage design.

For ordinary multi-device delivery, we don't need exclusive ownership. Multiple connections are legitimate.

This is another example of why session policy must be separated from connection routing.

# Part F — Prevent Cascading Failures

## 18. What is a cascading failure?

Let's examine a realistic failure.

Suppose Redis becomes slow.

```
Redis latency increases
        |
        ▼
Routing workers wait longer
        |
        ▼
Routing throughput decreases
        |
        ▼
Kafka consumer lag increases
        |
        ▼
Delivery latency increases
        |
        ▼
Application retries begin
        |
        ▼
More requests reach Redis
        |
        ▼
Redis becomes even slower
        |
        ▼
Everything gets worse
```

The original Redis slowdown now affects Kafka consumers, delivery workers, and potentially the business services.

This is a cascading failure.

We need several mechanisms to prevent this.

## 19. Mechanism 1: Timeouts

Every operation that depends on another component must have a timeout.

For example:

```
Router Worker
    |
    | Redis lookup
    |
    ├── Success within deadline
    |
    └── Timeout
```

Without timeouts, workers can remain blocked indefinitely.

We should define timeouts for:

- Database operations.
- Redis lookups.
- Kafka producer operations.
- Internal RPC.
- WebSocket delivery.
- External authentication calls.

### How should we choose timeout values?

Start with an overall latency budget.

For example:

```
End-to-end delivery target: 500 ms

Audience resolution:         50 ms
Connection lookup:           50 ms
Node dispatch:              100 ms
Socket delivery processing: 100 ms
Remaining allowance:        200 ms
```

These are illustrative allocations.

The actual budget needs to account for measured latency, retries, network variability, and the required delivery percentile.

Don't give every downstream operation a 10-second timeout when the entire system is expected to deliver within 500 ms.

A timeout is not merely an error-handling setting.

It is a way to bound how long one failed dependency can consume system resources.

## 20. Mechanism 2: Circuit breakers

We introduced these for Redis.

They are useful for any downstream dependency.

Suppose Push Server 1 becomes unavailable.

Instead of every worker continuously making requests to it, the circuit breaker can open.

```
Delivery Worker
      |
      ▼
Push Server 1
      |
      X
   Failing
      |
      ▼
Circuit Breaker OPEN
      |
      ▼
Fail fast
      |
      ▼
Retry later or choose another valid destination
```

But there's one important caveat.

If Push Server 1 owns a connection, we cannot arbitrarily deliver that same socket message through Push Server 2.

We must first resolve a valid current connection owner.

A retry should recheck ownership rather than continually sending to a failed node.

For a user with another active connection, the system may route there according to its delivery policy.

### Circuit breakers have three states

| State     | Behavior                     |
| --------- | ---------------------------- |
| CLOSED    | Allow requests               |
| OPEN      | Reject requests immediately  |
| HALF-OPEN | Permit limited test requests |

Use them to protect against repeatedly calling known-unhealthy dependencies.

Don't use an open circuit as a reason to delete connection ownership without evidence.

A temporarily unreachable push server is not necessarily dead.

## 21. Mechanism 3: Bulkheads

Imagine one delivery worker pool processes everything:

```
Delivery Workers:

    Individual Notifications
    Group Messages
    Global Broadcasts
    Collaborative Operations
```

A global broadcast targeting 10 million users arrives.

It consumes all available workers.

Now a personal payment notification waits behind the broadcast.

This is a failure of workload isolation.

### Introduce bulkheads

Bulkheads separate resources so that one workload cannot consume everything.

For example:

```
                    Delivery Infrastructure
                            |
             ┌──────────────┼──────────────┐
             |              |              |
             ▼              ▼              ▼
       Individual Pool  Group Pool    Broadcast Pool
             |              |              |
             ▼              ▼              ▼
        User Events     Group Events    Global Events
```

We can isolate:

- Worker concurrency.
- Queue capacity.
- Rate limits.
- CPU/memory budgets.
- Connection pools.
- Retry budgets.

### Why is this useful?

Suppose the broadcast worker pool becomes overloaded.

Individual notifications can continue operating.

If one tenant sends an enormous number of events, it shouldn't consume all delivery resources for every other tenant.

This is particularly important for multi-tenant platforms.

The principle: One traffic category should not be able to exhaust the resources required by unrelated traffic categories.

## 22. Mechanism 4: Backpressure

Let's revisit a scenario from Chapter 2.

A push server has a slow client.

```
Producer:
    1,000 messages/sec

Client:
    100 messages/sec
```

The outbound queue grows.

But now imagine the entire push server is overloaded.

Its incoming delivery rate is higher than its socket delivery capacity.

We need backpressure at multiple levels.

### A. Per-connection backpressure

Each connection has a bounded outbound buffer.

When it fills:

- Coalesce messages.
- Drop permitted ephemeral updates.
- Disconnect the slow client.
- Persist important messages for replay.

### B. Per-server backpressure

Each push server has limits on:

- Incoming delivery requests.
- Concurrent serialization.
- Outbound bandwidth.
- Pending connection writes.

When overloaded, it should reject or defer new work rather than accepting an unlimited amount.

### C. Pipeline backpressure

If delivery workers cannot keep up, they should avoid fetching enormous numbers of additional tasks.

For Kafka consumers, this might involve reducing polling, pausing partitions, and controlling how much work is in flight.

For a queue, it might involve controlling prefetch or consumer concurrency.

For a database-backed delivery scheduler, it means bounded claim batches.

### Important distinction

Backpressure is not the same as dropping all messages.

It means controlling the flow of work based on downstream capacity.

The appropriate action depends on whether the event is ephemeral or durable.

## 23. Mechanism 5: Load shedding

Sometimes the system cannot handle all incoming traffic.

We have to decide which work to reject or discard.

This is called load shedding.

Imagine our sports application is overloaded.

It receives:

```
Global score updates:       100/sec
Typing indicators:        10,000/sec
Personal alerts:            500/sec
Critical notifications:     50/sec
```

What should we preserve?

A reasonable priority system might be:

| Priority | Event type                     | Overload behavior          |
| -------- | ------------------------------ | -------------------------- |
| Critical | Payment/security alerts        | Preserve and durably queue |
| High     | Chat messages                  | Persist and replay         |
| Medium   | Status updates                 | Batch or coalesce          |
| Low      | Typing indicators              | Drop                       |
| Low      | Intermediate dashboard updates | Keep latest state          |

This is much better than randomly dropping events.

### Coalescing example

Suppose a user is watching a stock price.

We generate:

```
100.1
100.2
100.3
100.4
100.5
```

A slow client has not received any of these yet.

Instead of queuing all five, we can replace them with:

```
100.5
```

The application only needs the latest value.

For chat messages, this would be incorrect because intermediate messages matter.

This is why the event type should carry information about its delivery semantics.

A useful event metadata model might include:

```
{
  "eventId": "evt-101",
  "priority": "HIGH",
  "deliveryMode": "DURABLE",
  "orderingKey": "conversation-10",
  "expiresAt": null
}
```

The push infrastructure can use these policies without understanding every domain-specific business rule.

## 24. Mechanism 6: Retry budgets

Retries can make failures worse.

Imagine 10,000 requests fail because Redis is unavailable.

Every worker retries immediately.

Redis receives another 10,000 requests.

This is a retry storm.

Instead, introduce retry budgets.

A retry budget limits how much additional traffic retries can create.

For example:

```
Maximum retry traffic:
    20% of normal traffic over a defined window
```

This is just an illustrative policy.

Other useful techniques:

- Exponential backoff.
- Random jitter.
- Bounded attempts.
- Per-destination retry limits.
- Global retry limits.
- Circuit breakers.
- Retry only transient failures.

### Don't retry at every layer independently without coordination

Consider this:

```
Business Service: 3 retries

Kafka Producer: 3 retries

Router: 3 retries

Delivery Worker: 3 retries

Push Server: 3 retries
```

The number of attempts can multiply.

In a badly designed pipeline, one logical operation might trigger far more requests than expected.

Assign retry responsibility to clear boundaries.

For example:

- Outbox publisher retries Kafka publication.
- Router retries durable handoff.
- Delivery subsystem retries destination delivery.
- Client reconnect logic handles connection restoration.
- Client replay handles missed business events.

Each layer should know what it owns and avoid uncontrolled nested retries.

# Part G — Poison Messages and Dead-Letter Queues

## 25. Scenario 9: One event repeatedly crashes a worker

Suppose Kafka contains:

```
{
  "eventId": "evt-999",
  "type": "DOCUMENT_UPDATE",
  "payload": "MALFORMED_PAYLOAD"
}
```

Every time a routing worker consumes it, deserialization fails.

The worker restarts or retries.

It receives the same event again.

And fails again.

This is called a poison message.

If the system keeps retrying it forever, it can block or repeatedly consume resources.

We need a policy.

### Step 1: Classify the error

Is it transient?

For example:

```
Redis temporarily unavailable
```

Retry.

Or permanent?

```
Invalid JSON
Unsupported schema
Invalid event type
```

Don't retry indefinitely.

### Step 2: Move poison messages to a Dead-Letter Queue

A conceptual flow:

```
Main Queue
    |
    ▼
Worker
    |
    ▼
Processing Failure
    |
    ▼
Retry Policy
    |
    ├── Transient -> Retry
    |
    └── Permanent / Exhausted
               |
               ▼
           Dead-Letter Queue
```

The dead-letter record should include:

```
{
  "eventId": "evt-999",
  "originalTopic": "document-events",
  "error": "Invalid document payload",
  "attemptCount": 5,
  "failedAt": "2026-11-10T10:00:00Z"
}
```

### What do we do with DLQ messages?

A DLQ is not a trash can.

It is an operational recovery mechanism.

We should:

1. Alert on DLQ growth.
2. Inspect the failure reason.
3. Identify whether a code bug or data problem caused it.
4. Correct the issue.
5. Replay the event safely if appropriate.
6. Preserve its original event ID for idempotency.

For Kafka, dead-letter handling is often implemented by publishing the failed record to a separate topic rather than expecting Kafka itself to automatically move it there.

The consumer then commits the original record only after the dead-letter handoff succeeds.

If the handoff is retried, the DLQ consumer and producer need to tolerate duplicates.

### Important: Don't dead-letter a temporary network timeout immediately

An ordinary Redis timeout does not mean the event is malformed.

Use an appropriate retry policy first.

# Part H — Graceful Recovery and Deployments

## 26. Scenario 10: Deploy a new version without disrupting everyone

Suppose we have:

```
Push 1
Push 2
Push 3
Push 4
```

We want to deploy version 2.

A bad deployment:

```
Stop all servers
Deploy new version
Start all servers
```

This disconnects everyone.

It also creates a massive reconnection storm.

Instead, use a rolling deployment.

### Step 1: Deploy a new instance

```
Push 1 (v1)
Push 2 (v1)
Push 3 (v1)
Push 4 (v1)

Deploy:
Push 5 (v2)
```

Check that Push 5 is healthy.

### Step 2: Stop new connections to Push 1

Mark Push 1 as:

```
DRAINING
```

The load balancer no longer sends new connections to it.

### Step 3: Let existing connections migrate

Notify its clients:

```
{
  "type": "SERVER_RESTART",
  "reconnectAfterMs": 2000
}
```

The client reconnects with jitter.

The load balancer selects a healthy server.

### Step 4: Finish draining

Push 1 waits until:

```
Active connection count = 0
```

Or until a configured deadline.

Remaining connections can then be closed.

### Step 5: Replace the instance

Push 1 is terminated and replaced with Push 1 (v2).

Repeat for Push 2, Push 3, and Push 4.

### What about Kafka consumers?

If our push servers also consume Kafka, graceful shutdown additionally requires:

- Stop fetching new records.
- Finish or safely hand off in-flight records.
- Commit only completed processing.
- Leave the consumer group cleanly.
- Allow partitions to be reassigned.

This is one reason why separating Kafka routing consumers from push servers can make deployment and lifecycle management simpler.

### What about database migrations?

An additional production concern:

A new push-server version might expect a different event schema.

During rolling deployment, old and new versions coexist.

Use backward-compatible schema changes:

```
Version 1 consumer understands fields A and B.

Version 2 producer emits A, B, and C.

Version 1 can safely ignore C.
```

Prefer additive schema changes first, then remove obsolete fields after all consumers are upgraded.

Never assume every service will be upgraded simultaneously.

# Part I — Monitoring and Observability

## 27. How do we know the system is healthy?

A major mistake is monitoring only:

```
CPU
Memory
Number of servers
```

Those metrics are useful, but insufficient.

Imagine:

```
All servers: UP

CPU: 30%

Memory: 40%

Kafka: UP

Redis: UP
```

Everything looks healthy.

But users are receiving messages 20 seconds late.

The actual system is unhealthy even though the infrastructure looks healthy.

We need metrics based on user-visible delivery behavior.

## 27.1 Connection metrics

For each push server, monitor:

| Metric                           | What it tells us                       |
| -------------------------------- | -------------------------------------- |
| Active connections               | Current load                           |
| New connections/sec              | Connection establishment rate          |
| Disconnects/sec                  | Connection churn                       |
| Reconnect rate                   | Possible network or server instability |
| Heartbeat timeout count          | Unhealthy connections                  |
| Average connection duration      | Connection stability                   |
| Authentication failures          | Auth or security problems              |
| Connections per server           | Load distribution                      |
| Connection registration failures | Registry availability                  |

Pay special attention to reconnect rates.

A sudden increase can be an early indication of:

- Network issues.
- Load balancer problems.
- Push server failures.
- Mobile network instability.

## 27.2 Messaging metrics

For Kafka:

| Metric                       | What it tells us                  |
| ---------------------------- | --------------------------------- |
| Consumer lag                 | Events waiting for processing     |
| Oldest unprocessed event age | How delayed processing has become |
| Produce failures             | Event publication problems        |
| Broker availability          | Broker health                     |
| Under-replicated partitions  | Replication health                |
| ISR shrink/expansion         | Replica stability                 |
| Consumer rebalances          | Consumer group instability        |

For RabbitMQ or another queue:

- Queue depth.
- Unacknowledged messages.
- Redelivery count.
- Consumer utilization.
- Dead-letter count.
- Oldest message age.

For Redis:

- Command latency.
- Connection count.
- Memory utilization.
- Evictions.
- Rejected connections.
- Replication/failover status.
- Client timeouts.

## 27.3 Delivery metrics

These are particularly important.

| Metric                       | What it tells us                        |
| ---------------------------- | --------------------------------------- |
| Delivery attempts            | Amount of delivery work                 |
| Successful push writes       | Transport-level success                 |
| Client ACK rate              | Application acknowledgement success     |
| Retry rate                   | Delivery instability                    |
| Duplicate rate               | Idempotency/retry behavior              |
| Offline recipient count      | Number of undeliverable live recipients |
| Expired deliveries           | Events exceeding their validity window  |
| Pending delivery count       | Backlog                                 |
| P50/P95/P99 delivery latency | End-to-end delivery performance         |

### A very important metric: End-to-end delivery latency

Measure:

```
Event creation time
       |
       ▼
Client ACK time
```

Then:

\\[ DeliveryLatency = ClientAckTime - EventCreationTime \\]

We can measure different parts separately:

```
Business event created
    -> Kafka published

Kafka published
    -> Router consumed

Router consumed
    -> Delivery record created

Delivery record created
    -> Push server accepted

Push server accepted
    -> Client ACK
```

This lets us determine exactly where the delay occurs.

For example:

```
Kafka processing:       20 ms
Audience resolution:    30 ms
Node dispatch:          10 ms
Client acknowledgement: 200 ms
```

Without stage-level metrics, we might incorrectly blame Kafka for slow client processing.

## 28. Logs, metrics, and distributed tracing

These three serve different purposes.

### Metrics

Tell us whether something is wrong.

Example:

```
P99 delivery latency = 4 seconds
```

### Logs

Tell us what happened in a specific execution.

Example:

```
deliveryId=del-101
recipientId=user-A
serverId=push-2
error=CONNECTION_NOT_FOUND
```

### Distributed traces

Show the complete execution path across services.

For example:

```
Chat Service
    |
    ▼
Outbox Publisher
    |
    ▼
Kafka Consumer
    |
    ▼
Router Worker
    |
    ▼
Redis Lookup
    |
    ▼
Push Server
    |
    ▼
Client ACK
```

A trace can help us determine which component introduced latency or failure.

### Correlation IDs

Every event should have a stable identifier.

For example:

```
eventId = evt-101
deliveryId = del-101
```

Carry these identifiers throughout the system.

Then we can search logs across:

- Business services.
- Kafka consumers.
- Routing workers.
- Delivery workers.
- Push servers.
- ACK handlers.

Don't use `userId` as the only correlation ID.

One user can generate millions of events, and one event can target millions of recipients.

Use identifiers that represent the actual event and delivery operation.

## 29. Define Service Level Objectives (SLOs)

We should define what the user actually expects.

For example:

```
Connection availability:
    99.9%

Connected-user delivery latency:
    P99 under 500 ms

Critical notification processing:
    99.99% within defined delivery window

Message durability:
    No acknowledged business message silently lost
```

These are illustrative targets, not universal values.

We should distinguish two different delivery populations.

Online delivery SLO

Measures how quickly we deliver to connected, healthy clients.

Offline recovery SLO

Measures how quickly a user receives or can retrieve messages after reconnecting.

Combining the two into one number hides important problems.

We should also distinguish the application's promise from the push infrastructure's promise.

For example, a push service can promise:

> For connected, authorized clients, 99% of eligible events are delivered within a specified latency.

It cannot promise that every client will always be connected or that a user will immediately read every notification.

# Part J — Failure Matrix for the Entire System

## 30. One consolidated production failure matrix

Let's put all the scenarios together.

| Failure                            | Immediate impact                        | Detection                               | Recovery                                 |
| ---------------------------------- | --------------------------------------- | --------------------------------------- | ---------------------------------------- |
| One push server crashes            | Its connections terminate               | Health check / heartbeat / server lease | Client reconnect + replay                |
| All push servers crash             | Real-time delivery unavailable          | Infrastructure health checks            | Restart fleet + reconnect control        |
| Redis unavailable                  | Cross-server routing impaired           | Redis timeouts and health               | Preserve local sockets; restore registry |
| Redis loses metadata               | Connection ownership unknown            | Registry reconciliation                 | Re-register active sockets               |
| Kafka broker crashes               | Some partitions temporarily unavailable | Broker/ISR monitoring                   | Leader election and retries              |
| Kafka cluster unavailable          | Durable publishing/consumption stops    | Producer failures                       | Outbox backlog and recovery              |
| Routing workers crash              | Events accumulate in Kafka              | Consumer lag                            | Restart and resume                       |
| Delivery workers crash             | Delivery tasks remain pending           | Expired worker leases                   | Reclaim and retry                        |
| Push server isolated from Redis    | Stale ownership risk                    | Lease failure                           | Restrict global ownership; recover       |
| Push server isolated from dispatch | Targeted delivery fails                 | RPC timeouts/circuit breaker            | Retry/re-resolve destination             |
| Client becomes unresponsive        | Delivery buffer may grow                | Heartbeat / write timeout               | Disconnect and replay                    |
| Large global broadcast             | Resource saturation                     | Fanout latency and queue depth          | Bulkheads, batching, backpressure        |
| Poison event                       | Repeated processing failure             | Failure counters                        | DLQ and investigation                    |
| Retry storm                        | Infrastructure overload                 | Retry volume and error rates            | Retry budgets, jitter, circuit breakers  |
| Rolling deployment                 | Temporary connection churn              | Deployment and reconnect metrics        | Graceful draining                        |

This is a good checklist for a system design interview.

Instead of saying:

"We'll handle failures using retries."

You can explain exactly which failure is detected, what state is affected, what gets retried, and how recovery occurs.

# Part K — The Fault-Tolerant Architecture

## 31. Let's design the system to survive failures

Combining everything, this is the architecture I'd aim for in a large reliable push platform.

\#chatgpt-mermaid-\_r_a7g\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_a7g\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_a7g\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_a7g\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_a7g\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_a7g\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_a7g\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_a7g\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_a7g\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_a7g\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_a7g\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_a7g\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_a7g\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_a7g\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_a7g\_ p{margin:0;}#chatgpt-mermaid-\_r_a7g\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_a7g\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_a7g\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_a7g\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_a7g\_ .label text,#chatgpt-mermaid-\_r_a7g\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_a7g\_ .node rect,#chatgpt-mermaid-\_r_a7g\_ .node circle,#chatgpt-mermaid-\_r_a7g\_ .node ellipse,#chatgpt-mermaid-\_r_a7g\_ .node polygon,#chatgpt-mermaid-\_r_a7g\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_a7g\_ .rough-node .label text,#chatgpt-mermaid-\_r_a7g\_ .node .label text,#chatgpt-mermaid-\_r_a7g\_ .image-shape .label,#chatgpt-mermaid-\_r_a7g\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_a7g\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_a7g\_ .rough-node .label,#chatgpt-mermaid-\_r_a7g\_ .node .label,#chatgpt-mermaid-\_r_a7g\_ .image-shape .label,#chatgpt-mermaid-\_r_a7g\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_a7g\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_a7g\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_a7g\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_a7g\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_a7g\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_a7g\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_a7g\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_a7g\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_a7g\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_a7g\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_a7g\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_a7g\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_a7g\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_a7g\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_a7g\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_a7g\_ .icon-shape,#chatgpt-mermaid-\_r_a7g\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_a7g\_ .icon-shape p,#chatgpt-mermaid-\_r_a7g\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_a7g\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_a7g\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_a7g\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_a7g\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_a7g\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_a7g\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_a7g\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_a7g\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_a7g\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_a7g\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_a7g\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_a7g\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_a7g\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_a7g\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_a7g\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_a7g\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_a7g\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_a7g\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_a7g\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_a7g\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_a7g\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_a7g\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_a7g\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_a7g\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_a7g\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_a7g\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_a7g\_ .node rect,#chatgpt-mermaid-\_r_a7g\_ .node circle,#chatgpt-mermaid-\_r_a7g\_ .node ellipse,#chatgpt-mermaid-\_r_a7g\_ .node polygon,#chatgpt-mermaid-\_r_a7g\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_a7g\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_a7g\_ .mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_a7g\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_a7g\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_a7g\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}Push InfrastructureRoutingBusinessPush Server 1Push Server 2Push Server 3Local Connection MapLocal Connection MapLocal Connection MapRouting Consumer GroupAudience ResolverRedis Connection RegistryDurable Delivery StoreBusiness ServicesBusiness DatabaseTransactional OutboxKafka ClusterDelivery Worker PoolCircuit Breaker / Rate LimiterNode DispatchClientsClientsClientsACK / Recovery Service

The system now has several independent protection mechanisms.

| Protection               | What it prevents                                    |
| ------------------------ | --------------------------------------------------- |
| Transactional Outbox     | Lost event publication                              |
| Kafka replication        | Broker-level data loss within configured guarantees |
| Idempotent consumers     | Duplicate event processing                          |
| Durable delivery records | Lost delivery work                                  |
| Delivery leases          | Permanently stuck worker assignments                |
| Retry scheduler          | Temporary delivery failures                         |
| Dead-letter handling     | Infinite poison-message retries                     |
| Connection leases        | Stale connection ownership                          |
| Fencing generations      | Operations from outdated owners                     |
| Circuit breakers         | Repeated calls to unhealthy dependencies            |
| Bulkheads                | One workload exhausting all processing capacity     |
| Backpressure             | Unlimited memory and processing queues              |
| Reconnection jitter      | Reconnect storms                                    |
| Replay cursors           | Lost events during client disconnections            |
| Monitoring and SLOs      | Undetected degradation                              |

## 32. The most important design principle: Don't make one failure everyone's problem

Consider these scenarios.

Redis fails:

Existing local connections may continue working. Important new delivery requests remain durable until routing recovers.

Kafka fails:

Business transactions can continue according to the application's policy, while outbox events accumulate for later publication.

Push Server 1 fails:

Its clients reconnect. Other push servers continue operating. Pending messages remain recoverable.

A broadcast becomes extremely large:

Isolate it in a separate workload pool so individual notifications continue working.

A client becomes unresponsive:

Disconnect the client instead of buffering unlimited messages. Durable data can be recovered later.

A worker crashes:

Its claimed delivery records eventually become available for another worker. Idempotency protects against duplicate processing.

These are examples of graceful degradation.

The system doesn't necessarily maintain full functionality during every failure, but it preserves the important guarantees and avoids spreading the failure.

# 33. A practical rulebook for production design

I would use these rules when implementing the system.

1. Never depend on graceful socket disconnects for cleanup. Always have expiry/reconciliation.
2. Never allow an old connection cleanup operation to delete a newer connection.
3. Never hold a database or Redis request indefinitely.
4. Never retry a transient failure immediately and without limits.
5. Never let one large broadcast monopolize all delivery workers.
6. Never allow unbounded outbound buffers per connection.
7. Never assume a successful socket write means the client processed a message.
8. Never commit a Kafka offset before the corresponding durable processing boundary completes.
9. Never automatically retry permanent errors forever.
10. Never use a DLQ as a substitute for investigating failed processing.
11. Never treat a network timeout as proof that a remote operation did not happen.
12. Never make Redis connection metadata the authoritative source of business data.

The underlying theme is that every asynchronous operation needs:

- An owner.
- A bounded lifetime.
- A failure policy.
- A recovery mechanism.
- A way to recognize duplicate work.

## Chapter 7 — What you should now understand

You should now be able to reason about the system's failure behavior without relying on a list of technologies.

The five most important concepts are:

1\. Failure isolation: A failed push server shouldn't bring down the other push servers.

2\. Graceful degradation: Redis being unavailable doesn't necessarily mean existing sockets must disconnect.

3\. Controlled recovery: Reconnects, retries, and reprocessing must be spread over time and bounded by capacity.

4\. Ownership correctness: Leases alone aren't enough for all ownership races. Conditional operations and fencing generations protect against stale owners.

5\. Observability: A system can have every infrastructure component marked healthy while actual message delivery is failing. Measure end-to-end delivery behavior.

# Next: Chapter 8 — Database and Storage Architecture

We've introduced several different storage components:

- Redis for connection ownership.
- Kafka for event history.
- A business database for authoritative application data.
- A delivery database or inbox for reliable notification processing.
- Local memory for actual socket references.

Now let's answer a particularly important question:

Does the push server itself need a database?

We'll design the storage architecture from first principles.

We'll cover:

1. Which data belongs in the push server's memory.
2. Which data belongs in Redis.
3. Which data belongs in PostgreSQL/MySQL.
4. When a separate delivery database is justified.
5. Whether push servers should directly access business databases.
6. How to perform database operations without creating bottlenecks.
7. How to handle connection registration atomically.
8. Redis versus relational databases versus DynamoDB/Cassandra-style storage.
9. How to design delivery tables, indexes, partitioning, and retention.
10. How to recover when Redis or the delivery database loses data.

We'll also compare two important architectures:

- A thin push gateway that only handles transport and connection routing.
- A stateful push gateway that maintains additional application-specific transient state.

The central question: What data must the push infrastructure own, what data should it merely access, and how do we avoid turning every push server into another business application server?
