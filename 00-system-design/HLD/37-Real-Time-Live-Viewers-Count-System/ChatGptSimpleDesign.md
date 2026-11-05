# Design a Real-Time Live Viewers Count System

Imagine an IPL match with 10 million people watching simultaneously. The application displays:

LIVE

IPL FINAL

# 10,24,582

Watching now

The number continuously increases when people start watching and decreases when people stop watching or disconnect.

This looks simple in the UI, but designing it for millions of concurrent viewers creates some interesting distributed-systems problems.

We'll design for a large event, not just a small application.

## 1. Requirements and assumptions

First, let's define exactly what we're counting.

Functional requirements

1. A viewer starts watching an event. The count increases.
2. A viewer stops watching. The count decreases.
3. If a viewer disconnects unexpectedly, the system eventually removes them.
4. The count is visible to all viewers watching the same event.
5. The count updates in near real time.

Non-functional requirements

- Support millions of concurrent viewers.
- Handle sudden traffic spikes when an important match starts.
- Highly available and horizontally scalable.
- Low-latency count updates.
- Avoid counting the same viewer multiple times.
- Continue working when clients disconnect or servers fail.

### Important: What does "viewer" mean?

We need to decide this before designing the system.

For this design, let's assume:

- A viewer means an active playback session, not just someone who opened the event page.
- A session is active if the player is playing and has sent a heartbeat recently.
- A person watching on two devices counts as two sessions.
- A paused or ended playback session is not counted.
- The number can be slightly delayed. An accuracy target of approximately one or two seconds for updates is reasonable, but abrupt disconnects can take longer to detect.

Why count playback sessions instead of unique users?

Because counting unique people across multiple devices requires identity-based deduplication. Counting active playback sessions is simpler and more reliable operationally.

## 2. High-level architecture

\#chatgpt-mermaid-\_r_2f6\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_2f6\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_2f6\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_2f6\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_2f6\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2f6\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_2f6\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_2f6\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_2f6\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_2f6\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_2f6\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_2f6\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2f6\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2f6\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_2f6\_ p{margin:0;}#chatgpt-mermaid-\_r_2f6\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2f6\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2f6\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2f6\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_2f6\_ .label text,#chatgpt-mermaid-\_r_2f6\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2f6\_ .node rect,#chatgpt-mermaid-\_r_2f6\_ .node circle,#chatgpt-mermaid-\_r_2f6\_ .node ellipse,#chatgpt-mermaid-\_r_2f6\_ .node polygon,#chatgpt-mermaid-\_r_2f6\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_2f6\_ .rough-node .label text,#chatgpt-mermaid-\_r_2f6\_ .node .label text,#chatgpt-mermaid-\_r_2f6\_ .image-shape .label,#chatgpt-mermaid-\_r_2f6\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_2f6\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_2f6\_ .rough-node .label,#chatgpt-mermaid-\_r_2f6\_ .node .label,#chatgpt-mermaid-\_r_2f6\_ .image-shape .label,#chatgpt-mermaid-\_r_2f6\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_2f6\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_2f6\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2f6\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2f6\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_2f6\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_2f6\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_2f6\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_2f6\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_2f6\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_2f6\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_2f6\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2f6\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2f6\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_2f6\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2f6\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_2f6\_ .icon-shape,#chatgpt-mermaid-\_r_2f6\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_2f6\_ .icon-shape p,#chatgpt-mermaid-\_r_2f6\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_2f6\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_2f6\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_2f6\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_2f6\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_2f6\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_2f6\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_2f6\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_2f6\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_2f6\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2f6\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_2f6\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_2f6\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_2f6\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2f6\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_2f6\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_2f6\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2f6\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_2f6\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_2f6\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2f6\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_2f6\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2f6\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_2f6\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_2f6\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_2f6\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_2f6\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_2f6\_ .node rect,#chatgpt-mermaid-\_r_2f6\_ .node circle,#chatgpt-mermaid-\_r_2f6\_ .node ellipse,#chatgpt-mermaid-\_r_2f6\_ .node polygon,#chatgpt-mermaid-\_r_2f6\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_2f6\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_2f6\_ .mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_2f6\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_2f6\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2f6\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}Viewer 1Regional API / WebSocketGatewayViewer 2Viewer NKafka: Viewer HeartbeatsPresence Stream ProcessorSharded Active SessionCountersCount AggregatorRedis: Latest Event CountRegional Pub/SubWebSocket / SSE GatewaysViewer Count APICDN / Edge Cache

Let's understand each component and why we need it.

| Component               | Responsibility                             |
| ----------------------- | ------------------------------------------ |
| Viewer Player           | Sends heartbeats while playing             |
| API / WebSocket Gateway | Receives viewer presence events            |
| Kafka                   | Buffers and distributes high-volume events |
| Presence Processor      | Determines which sessions are active       |
| Sharded Counters        | Maintains distributed counts               |
| Count Aggregator        | Calculates the total event count           |
| Redis                   | Stores the latest count for fast reads     |
| Pub/Sub                 | Distributes count updates                  |
| WebSocket / SSE         | Delivers updates to viewers                |

The most important part of this design is how we determine whether a viewer is still active.

## 3. How do we know someone is watching?

We cannot simply increment a counter when someone opens the video.

Consider this scenario:

1. A user opens the IPL match.
2. The count increases from 1,000 to 1,001.
3. The user closes their laptop without sending a logout request.
4. The application never receives a request saying that the user has left.

The count would remain incorrect forever if we relied only on join and leave events.

We need a mechanism that detects inactive viewers automatically.

### Heartbeat mechanism

The video player periodically informs the backend that it is still active.

For example:

- Heartbeat interval: 15 seconds.
- Session expiry / inactivity timeout: 45 seconds.

CounterPresence ProcessorGatewayPlayerCounterPresence ProcessorGatewayPlayer#chatgpt-mermaid-\_r_2fd\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_2fd\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_2fd\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_2fd\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_2fd\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2fd\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_2fd\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_2fd\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_2fd\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_2fd\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_2fd\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_2fd\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2fd\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2fd\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_2fd\_ p{margin:0;}#chatgpt-mermaid-\_r_2fd\_ .actor{stroke:rgb(31, 78, 148);fill:rgb(9, 23, 44);stroke-width:1;}#chatgpt-mermaid-\_r_2fd\_ rect.actor.outer-path[data-look="neo"]{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2fd\_ rect.note[data-look="neo"]{stroke:rgb(58, 132, 63);fill:rgb(48, 48, 48);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2fd\_ text.actor>tspan{fill:rgb(237, 237, 237);stroke:none;}#chatgpt-mermaid-\_r_2fd\_ .actor-line{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2fd\_ .innerArc{stroke-width:1.5;stroke-dasharray:none;}#chatgpt-mermaid-\_r_2fd\_ .messageLine0{stroke-width:1.5;stroke-dasharray:none;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2fd\_ .messageLine1{stroke-width:1.5;stroke-dasharray:2,2;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2fd\_ [id$="-arrowhead"] path{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2fd\_ .sequenceNumber{fill:#505050;}#chatgpt-mermaid-\_r_2fd\_ [id$="-sequencenumber"]{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2fd\_ [id$="-crosshead"] path{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2fd\_ .messageText{fill:rgb(237, 237, 237);stroke:none;}#chatgpt-mermaid-\_r_2fd\_ .labelBox{stroke:rgba(255, 255, 255, 0.15);fill:rgb(0, 0, 0);filter:none;}#chatgpt-mermaid-\_r_2fd\_ .labelText,#chatgpt-mermaid-\_r_2fd\_ .labelText>tspan{fill:rgb(237, 237, 237);stroke:none;}#chatgpt-mermaid-\_r_2fd\_ .loopText,#chatgpt-mermaid-\_r_2fd\_ .loopText>tspan{fill:rgb(237, 237, 237);stroke:none;}#chatgpt-mermaid-\_r_2fd\_ .sectionTitle,#chatgpt-mermaid-\_r_2fd\_ .sectionTitle>tspan{fill:rgb(237, 237, 237);stroke:none;}#chatgpt-mermaid-\_r_2fd\_ .loopLine{stroke-width:2px;stroke-dasharray:2,2;stroke:rgba(255, 255, 255, 0.15);fill:rgba(255, 255, 255, 0.15);}#chatgpt-mermaid-\_r_2fd\_ .note{stroke:rgb(58, 132, 63);fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_2fd\_ .noteText,#chatgpt-mermaid-\_r_2fd\_ .noteText>tspan{fill:rgb(237, 237, 237);stroke:none;font-weight:normal;}#chatgpt-mermaid-\_r_2fd\_ .activation0{fill:rgb(48, 48, 48);stroke:hsl(0, 0%, 8.8235294118%);}#chatgpt-mermaid-\_r_2fd\_ .activation1{fill:rgb(48, 48, 48);stroke:hsl(0, 0%, 8.8235294118%);}#chatgpt-mermaid-\_r_2fd\_ .activation2{fill:rgb(48, 48, 48);stroke:hsl(0, 0%, 8.8235294118%);}#chatgpt-mermaid-\_r_2fd\_ .actorPopupMenu{position:absolute;}#chatgpt-mermaid-\_r_2fd\_ .actorPopupMenuPanel{position:absolute;fill:rgb(9, 23, 44);box-shadow:0px 8px 16px 0px rgba(0,0,0,0.2);filter:drop-shadow(3px 5px 2px rgb(0 0 0 / 0.4));}#chatgpt-mermaid-\_r_2fd\_ .actor-man circle,#chatgpt-mermaid-\_r_2fd\_ line{fill:rgb(9, 23, 44);stroke-width:2px;}#chatgpt-mermaid-\_r_2fd\_ g rect.rect{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_2fd\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_2fd\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_2fd\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_2fd\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_2fd\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2fd\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_2fd\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_2fd\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_2fd\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2fd\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_2fd\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_2fd\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2fd\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_2fd\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_2fd\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2fd\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_2fd\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2fd\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}Refresh session expiryloop[Every 15 seconds]No heartbeat for 45 secondsStart watchingJOIN(sessionId)+1HEARTBEAT(sessionId)HEARTBEAT(sessionId)Network disconnects-1

The key idea:

A viewer remains active only while their heartbeat lease is valid.

For example:

| Time     | Heartbeat received? | Status                           |
| -------- | ------------------- | -------------------------------- |
| 10:00:00 | Yes                 | Active                           |
| 10:00:15 | Yes                 | Active                           |
| 10:00:30 | Yes                 | Active                           |
| 10:00:45 | No                  | Still active during grace period |
| 10:01:00 | No                  | Expired, remove from count       |

The timeout is a business rule, not a universal value. We might use a 30–60 second lease depending on network conditions and how quickly we want disconnected viewers removed.

### What happens when someone leaves normally?

The player can send an explicit leave request:

```
POST /events/ipl-final/presence/leave
```

The backend immediately marks the session inactive and decrements the count.

But we must never depend on this request. A device can lose power, a browser can crash, or a network can disappear.

Explicit leave is an optimization. Heartbeat expiry is the correctness mechanism.

## 4. APIs and session management

We need to identify each playback session and make heartbeat requests idempotent.

A possible API design:

1\. Start watching

```
POST /events/{eventId}/presence/join
```

Response:

```
{
  "sessionId": "vs_abc123",
  "heartbeatIntervalSeconds": 15,
  "leaseSeconds": 45
}
```

The server generates a unique session ID and associates it with the event.

2\. Send heartbeat

```
POST /events/{eventId}/presence/heartbeat
```

Request:

```
{
  "sessionId": "vs_abc123",
  "status": "PLAYING",
  "sequence": 12
}
```

The sequence number helps reject duplicate or out-of-order messages where necessary.

3\. Stop watching

```
POST /events/{eventId}/presence/leave
```

Request:

```
{
  "sessionId": "vs_abc123"
}
```

4\. Get the current viewer count

```
GET /events/{eventId}/viewer-count
```

Response:

```
{
  "eventId": "ipl-final",
  "viewerCount": 1024582,
  "updatedAt": "2026-11-05T10:30:15Z",
  "status": "FRESH"
}
```

The API should return the latest materialized count, not calculate it by querying every active session.

### Session data model

Conceptually, each active session has:

```
ViewerSession
-------------
eventId
sessionId
status
lastHeartbeatAt
expiresAt
region
```

The session ID is the primary identity for presence tracking. We don't need to store personal information just to calculate the count.

We should also authenticate the session token so that random clients cannot inflate the count by submitting arbitrary heartbeats.

## 5. How should we store and calculate active viewers?

This is the most important design decision.

There are multiple approaches. Let's compare them.

### Approach A: Redis Sorted Set

This is a relatively straightforward approach.

For every event, maintain a Redis Sorted Set.

Example:

```
Key: event:ipl-final:viewers

Member          Score (last heartbeat)
---------------------------------------
session_101     1793872800
session_102     1793872815
session_103     1793872820
session_104     1793872830
```

The score represents the last heartbeat timestamp.

When a heartbeat arrives:

```
ZADD event:ipl-final:viewers 1793872845 session_101
```

When we want to remove expired sessions:

```
ZREMRANGEBYSCORE event:ipl-final:viewers -inf <expiry_timestamp>
```

Then:

```
ZCARD event:ipl-final:viewers
```

This gives us the number of active members remaining in that set.

The problem: A single Redis key for an IPL event becomes extremely hot.

Millions of viewers updating the same sorted set would create a bottleneck. Cleanup can also become expensive when many sessions expire together.

### Approach B: Sharded Redis Sorted Sets

Instead of one sorted set for an event, create multiple shards.

For example:

```
event:ipl-final:shard:001
event:ipl-final:shard:002
event:ipl-final:shard:003
...
event:ipl-final:shard:128
```

Assign a session to a shard using:

```
int shardId = Math.floorMod(sessionId.hashCode(), 128);
```

Now different sessions update different Redis keys.

\#chatgpt-mermaid-\_r_2ia\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_2ia\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_2ia\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_2ia\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_2ia\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2ia\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_2ia\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_2ia\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_2ia\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_2ia\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_2ia\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_2ia\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2ia\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2ia\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_2ia\_ p{margin:0;}#chatgpt-mermaid-\_r_2ia\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2ia\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2ia\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2ia\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_2ia\_ .label text,#chatgpt-mermaid-\_r_2ia\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2ia\_ .node rect,#chatgpt-mermaid-\_r_2ia\_ .node circle,#chatgpt-mermaid-\_r_2ia\_ .node ellipse,#chatgpt-mermaid-\_r_2ia\_ .node polygon,#chatgpt-mermaid-\_r_2ia\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_2ia\_ .rough-node .label text,#chatgpt-mermaid-\_r_2ia\_ .node .label text,#chatgpt-mermaid-\_r_2ia\_ .image-shape .label,#chatgpt-mermaid-\_r_2ia\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_2ia\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_2ia\_ .rough-node .label,#chatgpt-mermaid-\_r_2ia\_ .node .label,#chatgpt-mermaid-\_r_2ia\_ .image-shape .label,#chatgpt-mermaid-\_r_2ia\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_2ia\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_2ia\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2ia\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2ia\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_2ia\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_2ia\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_2ia\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_2ia\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_2ia\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_2ia\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_2ia\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2ia\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2ia\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_2ia\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2ia\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_2ia\_ .icon-shape,#chatgpt-mermaid-\_r_2ia\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_2ia\_ .icon-shape p,#chatgpt-mermaid-\_r_2ia\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_2ia\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_2ia\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_2ia\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_2ia\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_2ia\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_2ia\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_2ia\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_2ia\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_2ia\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2ia\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_2ia\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_2ia\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_2ia\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2ia\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_2ia\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_2ia\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2ia\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_2ia\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_2ia\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2ia\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_2ia\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2ia\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_2ia\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_2ia\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_2ia\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_2ia\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_2ia\_ .node rect,#chatgpt-mermaid-\_r_2ia\_ .node circle,#chatgpt-mermaid-\_r_2ia\_ .node ellipse,#chatgpt-mermaid-\_r_2ia\_ .node polygon,#chatgpt-mermaid-\_r_2ia\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_2ia\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_2ia\_ .mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_2ia\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_2ia\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2ia\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}Heartbeat EventsHash sessionIdShard 1Shard 2Shard 3...Shard 128Count AggregatorTotal Viewer Count

Each shard maintains its own active sessions and count.

The aggregator calculates:

\\[ TotalViewers = \sum\_{i=1}^{128} ActiveViewers_i \\]

We can aggregate every one or two seconds and store the latest total in Redis.

This distributes the write load and avoids one hot key.

Important implementation detail: Heartbeats must be idempotent. Repeated heartbeats for an existing session should refresh its timestamp, not increment the count again.

Likewise, when removing expired sessions, the removal and decrement must happen atomically. A Lua script or equivalent atomic operation can prevent a heartbeat racing with an expiry cleanup from causing an incorrect count.

### Approach C: Kafka + Stream Processing

For a very large event, this is the architecture I would choose.

Instead of making every heartbeat directly update Redis, introduce Kafka and a stateful stream processor such as Apache Flink or Kafka Streams.

Why?

- Kafka absorbs sudden traffic spikes.
- It decouples ingestion from counting.
- It allows us to distribute processing across multiple consumers.
- Stateful processing can track session activity and expiry.
- We can recover and rebuild state using durable events and checkpoints.

Let's understand the processing.

Step 1: Heartbeats enter Kafka

```
Topic: viewer-heartbeats
```

Example message:

```
{
  "eventId": "ipl-final",
  "sessionId": "vs_abc123",
  "status": "PLAYING",
  "receivedAt": 1793872845,
  "sequence": 12
}
```

Partition the messages using a composite key:

```
hash(eventId + sessionId)
```

This is important.

Do not partition only by `eventId`, because all heartbeats for the IPL final would go to the same Kafka partition. That would defeat horizontal scaling for the biggest event.

Using the session ID distributes viewers across partitions while keeping events for the same session ordered within a partition.

Step 2: Stream processor maintains session state

The processor maintains state for each `(eventId, sessionId)`.

Conceptually:

```
Session ID       Last Seen       Status
---------------------------------------
vs_101           10:30:00        ACTIVE
vs_102           10:30:05        ACTIVE
vs_103           10:29:10        ACTIVE
```

When a heartbeat arrives:

1. Check whether the session is already active.
2. If it is new or previously expired, mark it active and emit `+1`.
3. Update its last heartbeat timestamp.
4. Schedule or refresh an expiry timer.

When the expiry timer fires:

1. Check the latest heartbeat timestamp.
2. If the lease has expired, mark the session inactive.
3. Emit `-1`.
4. Ignore duplicate expiry events for an already inactive session.

When an explicit leave or pause event arrives, mark the session inactive and emit `-1` exactly once.

This is effectively a state machine:

\#chatgpt-mermaid-\_r_2j6\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_2j6\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_2j6\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_2j6\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_2j6\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2j6\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_2j6\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_2j6\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_2j6\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_2j6\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_2j6\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_2j6\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2j6\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2j6\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_2j6\_ p{margin:0;}#chatgpt-mermaid-\_r_2j6\_ defs [id$="-barbEnd"]{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2j6\_ g.stateGroup text{fill:rgb(31, 78, 148);stroke:none;font-size:10px;}#chatgpt-mermaid-\_r_2j6\_ g.stateGroup text{fill:rgb(237, 237, 237);stroke:none;font-size:10px;}#chatgpt-mermaid-\_r_2j6\_ g.stateGroup .state-title{font-weight:bolder;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2j6\_ g.stateGroup rect{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_2j6\_ g.stateGroup line{stroke:rgb(175, 175, 175);stroke-width:1;}#chatgpt-mermaid-\_r_2j6\_ .transition{stroke:rgb(175, 175, 175);stroke-width:1;fill:none;}#chatgpt-mermaid-\_r_2j6\_ .stateGroup .composit{fill:transparent;border-bottom:1px;}#chatgpt-mermaid-\_r_2j6\_ .stateGroup .alt-composit{fill:#e0e0e0;border-bottom:1px;}#chatgpt-mermaid-\_r_2j6\_ .state-note{stroke:rgb(58, 132, 63);fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_2j6\_ .state-note text{fill:rgb(237, 237, 237);stroke:none;font-size:10px;}#chatgpt-mermaid-\_r_2j6\_ .stateLabel .box{stroke:none;stroke-width:0;fill:rgb(9, 23, 44);opacity:0.5;}#chatgpt-mermaid-\_r_2j6\_ .edgeLabel .label rect{fill:rgb(9, 23, 44);opacity:0.5;}#chatgpt-mermaid-\_r_2j6\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_2j6\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_2j6\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_2j6\_ .edgeLabel .label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2j6\_ .label div .edgeLabel{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2j6\_ .stateLabel text{fill:rgb(237, 237, 237);font-size:10px;font-weight:bold;}#chatgpt-mermaid-\_r_2j6\_ .node circle.state-start{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2j6\_ .node .fork-join{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2j6\_ .node circle.state-end{fill:rgb(31, 78, 148);stroke:transparent;stroke-width:1.5;}#chatgpt-mermaid-\_r_2j6\_ .end-state-inner{fill:transparent;stroke-width:1.5;}#chatgpt-mermaid-\_r_2j6\_ .node rect{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_2j6\_ .node polygon{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_2j6\_ [id$="-barbEnd"]{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2j6\_ .statediagram-cluster rect{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_2j6\_ .cluster-label,#chatgpt-mermaid-\_r_2j6\_ .nodeLabel{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2j6\_ .statediagram-cluster rect.outer{rx:5px;ry:5px;}#chatgpt-mermaid-\_r_2j6\_ .statediagram-state .divider{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_2j6\_ .statediagram-state .title-state{rx:5px;ry:5px;}#chatgpt-mermaid-\_r_2j6\_ .statediagram-cluster.statediagram-cluster .inner{fill:transparent;}#chatgpt-mermaid-\_r_2j6\_ .statediagram-cluster.statediagram-cluster-alt .inner{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_2j6\_ .statediagram-cluster .inner{rx:0;ry:0;}#chatgpt-mermaid-\_r_2j6\_ .statediagram-state rect.basic{rx:5px;ry:5px;}#chatgpt-mermaid-\_r_2j6\_ .statediagram-state rect.divider{stroke-dasharray:10,10;fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_2j6\_ .note-edge{stroke-dasharray:5;}#chatgpt-mermaid-\_r_2j6\_ .statediagram-note rect{fill:rgb(48, 48, 48);stroke:rgb(58, 132, 63);stroke-width:1px;rx:0;ry:0;}#chatgpt-mermaid-\_r_2j6\_ .statediagram-note rect{fill:rgb(48, 48, 48);stroke:rgb(58, 132, 63);stroke-width:1px;rx:0;ry:0;}#chatgpt-mermaid-\_r_2j6\_ .statediagram-note text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2j6\_ .statediagram-note .nodeLabel{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2j6\_ .statediagram .edgeLabel{color:red;}#chatgpt-mermaid-\_r_2j6\_ [id$="-dependencyStart"],#chatgpt-mermaid-\_r_2j6\_ [id$="-dependencyEnd"]{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);stroke-width:1;}#chatgpt-mermaid-\_r_2j6\_ .statediagramTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2j6\_ [data-look="neo"].statediagram-cluster rect{fill:rgb(9, 23, 44);stroke:url(#chatgpt-mermaid-\_r_2j6\_-gradient);stroke-width:1;}#chatgpt-mermaid-\_r_2j6\_ [data-look="neo"].statediagram-cluster rect.outer{rx:5px;ry:5px;filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2j6\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_2j6\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_2j6\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_2j6\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_2j6\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2j6\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_2j6\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_2j6\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_2j6\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2j6\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_2j6\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_2j6\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2j6\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_2j6\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_2j6\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2j6\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_2j6\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2j6\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}InactiveActiveJOIN / valid HEARTBEAT(+1)HEARTBEAT / refresh leaseLEAVE / PAUSE / expiry (-1)duplicate LEAVE / expiry

The crucial rule is that a session contributes either zero or one to the count.

A heartbeat does not increment the count every time.

Step 3: Maintain sharded counters

The processor produces count changes:

```
{
  "eventId": "ipl-final",
  "shardId": 42,
  "delta": 1
}
```

Or:

```
{
  "eventId": "ipl-final",
  "shardId": 42,
  "delta": -1
}
```

These changes update distributed counters.

For example:

```
event:ipl-final:count:shard:1    = 81,200
event:ipl-final:count:shard:2    = 79,500
event:ipl-final:count:shard:3    = 83,100
...
event:ipl-final:count:shard:128  = 80,900
```

We can use Redis Cluster or another distributed store for the materialized counters.

For correctness, the counter update pipeline must handle retries and processor recovery. We can use transactional Kafka output and an idempotent projector, or publish versioned absolute shard snapshots instead of applying non-idempotent increments blindly.

Step 4: Aggregate the counters

A count aggregator periodically reads the shard counts:

```
Shard 1   = 81,200
Shard 2   = 79,500
Shard 3   = 83,100
...
```

It computes the total and updates:

```
event:ipl-final:viewer-count = 10,245,820
```

This value is now cheap to retrieve.

The UI does not need to know anything about Kafka, Flink, session expiry, or Redis shards.

## 6. How do we distribute the count to millions of viewers?

Now we have a count in Redis. But how does everyone see it change?

There are two separate problems:

1. Calculate the latest count.
2. Distribute that count to all interested viewers.

We should not broadcast every heartbeat to every viewer. That would be wasteful.

Imagine 10 million viewers sending heartbeats every 15 seconds. We have roughly 667,000 heartbeats per second. We only need to display the aggregated count, perhaps every two seconds.

### Option 1: Polling

The frontend calls:

```
GET /events/ipl-final/viewer-count
```

Every 5–10 seconds.

The API reads the latest value from Redis.

Advantages:

- Very simple.
- Stateless clients.
- Easy to implement.
- Works well with CDN or edge caching.

Disadvantages:

- A lot of unnecessary requests.
- Updates are delayed by the polling interval.
- Synchronized polling can cause traffic spikes.

We can reduce the load using:

- CDN caching with a short TTL.
- Randomized polling intervals.
- `stale-while-revalidate` where appropriate.
- Serving the same cached event count to all viewers.

For example, millions of viewers requesting the same count should not cause millions of reads from the Redis primary.

### Option 2: WebSocket or Server-Sent Events

The server pushes a new aggregated count to connected clients.

Example:

```
{
  "eventId": "ipl-final",
  "viewerCount": 10245820,
  "updatedAt": "10:30:15"
}
```

The frontend updates the UI.

WebSocket is useful when the application already has bidirectional real-time communication, such as chat or live reactions.

SSE is simpler when the server only needs to push updates to the client.

For a live sports platform, I would prefer reusing the existing live-event connection where possible.

### How do we scale the fan-out?

We shouldn't have one backend server pushing updates directly to 10 million users.

Instead:

\#chatgpt-mermaid-\_r_2kk\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_2kk\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_2kk\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_2kk\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_2kk\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2kk\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_2kk\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_2kk\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_2kk\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_2kk\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_2kk\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_2kk\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2kk\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2kk\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_2kk\_ p{margin:0;}#chatgpt-mermaid-\_r_2kk\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2kk\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2kk\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2kk\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_2kk\_ .label text,#chatgpt-mermaid-\_r_2kk\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2kk\_ .node rect,#chatgpt-mermaid-\_r_2kk\_ .node circle,#chatgpt-mermaid-\_r_2kk\_ .node ellipse,#chatgpt-mermaid-\_r_2kk\_ .node polygon,#chatgpt-mermaid-\_r_2kk\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_2kk\_ .rough-node .label text,#chatgpt-mermaid-\_r_2kk\_ .node .label text,#chatgpt-mermaid-\_r_2kk\_ .image-shape .label,#chatgpt-mermaid-\_r_2kk\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_2kk\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_2kk\_ .rough-node .label,#chatgpt-mermaid-\_r_2kk\_ .node .label,#chatgpt-mermaid-\_r_2kk\_ .image-shape .label,#chatgpt-mermaid-\_r_2kk\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_2kk\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_2kk\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2kk\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2kk\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_2kk\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_2kk\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_2kk\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_2kk\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_2kk\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_2kk\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_2kk\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2kk\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2kk\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_2kk\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2kk\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_2kk\_ .icon-shape,#chatgpt-mermaid-\_r_2kk\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_2kk\_ .icon-shape p,#chatgpt-mermaid-\_r_2kk\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_2kk\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_2kk\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_2kk\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_2kk\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_2kk\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_2kk\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_2kk\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_2kk\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_2kk\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2kk\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_2kk\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_2kk\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_2kk\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2kk\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_2kk\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_2kk\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2kk\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_2kk\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_2kk\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2kk\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_2kk\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2kk\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_2kk\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_2kk\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_2kk\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_2kk\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_2kk\_ .node rect,#chatgpt-mermaid-\_r_2kk\_ .node circle,#chatgpt-mermaid-\_r_2kk\_ .node ellipse,#chatgpt-mermaid-\_r_2kk\_ .node polygon,#chatgpt-mermaid-\_r_2kk\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_2kk\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_2kk\_ .mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_2kk\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_2kk\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2kk\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}Count AggregatorLatest CountRegional Pub/SubGateway - Region 1Gateway - Region 2Gateway - Region 3Connected ViewersConnected ViewersConnected Viewers

Each regional gateway manages its own connected viewers.

When the count changes:

1. The aggregator publishes one count snapshot.
2. Regional messaging infrastructure distributes it to the gateways.
3. Each gateway pushes the snapshot to its locally connected clients.

We should also avoid excessive updates. For example, publish a snapshot every 2–5 seconds rather than after every heartbeat.

There is still significant last-mile fan-out traffic, but we avoid sending individual heartbeat events through the entire distribution infrastructure.

A practical choice: Use WebSocket/SSE for platforms that already have real-time connections. For a simple viewer counter, CDN-cached polling may actually be more economical.

## 7. Capacity estimation

Let's do a basic calculation. Interviewers expect this because it helps justify the architecture.

Assume:

| Metric             | Assumption |
| ------------------ | ---------- |
| Concurrent viewers | 10 million |
| Heartbeat interval | 15 seconds |
| Heartbeat payload  | 200 bytes  |
| Lease timeout      | 45 seconds |

### Heartbeat requests per second

\\[ RPS = \frac{10,000,000}{15} \\]

\\[ RPS \approx 666,667 \\]

We need to handle approximately 667K heartbeat events per second.

### Approximate incoming payload

\\[ 666,667 \times 200 = 133,333,400 \text{ bytes/sec} \\]

That's approximately 133 MB/s of payload, before protocol overhead, replication, and internal processing traffic.

This is why a single application server or Redis instance is not sufficient.

### How many shards?

Suppose we start with 128 logical counter shards.

\\[ \frac{666,667}{128} \approx 5,208 \\]

Each shard receives approximately 5,208 heartbeat events per second on average.

The actual number of shards should be determined through load testing, partition distribution, and per-node capacity. We may use more shards if the event is expected to grow.

Also remember: 10 million concurrent viewers means managing 10 million client connections if we use persistent connections. That requires horizontally scaled gateway infrastructure, connection management, and regional distribution.

## 8. Important production challenges and solutions

This is where the design becomes interesting.

| Problem                             | Solution                                                               |
| ----------------------------------- | ---------------------------------------------------------------------- |
| User closes browser without leaving | Heartbeat lease and expiry                                             |
| Duplicate heartbeat                 | Idempotent session state                                               |
| Duplicate leave event               | Decrement only on active-to-inactive transition                        |
| Out-of-order messages               | Sequence numbers and server-side timestamps                            |
| Traffic spike at match start        | Kafka buffering, autoscaling, load testing                             |
| Hot Redis key                       | Sharded session state and distributed counters                         |
| Synchronized heartbeats             | Add randomized heartbeat jitter                                        |
| Sudden mass disconnections          | Expiry processing with sufficient capacity and controlled batching     |
| Kafka consumer lag                  | Monitor lag, scale consumers, preserve backpressure                    |
| Redis or counter-store outage       | Retain durable event/state recovery capability; serve last known count |
| Region failure                      | Regional presence isolation and cross-region aggregation               |
| Fake heartbeats                     | Authentication, rate limiting, session validation                      |

Let's explore a few important ones.

### A. What if 5 million viewers join within a few seconds?

This is a classic traffic spike.

We should:

- Randomize initial heartbeat timing.
- Use Kafka to absorb temporary bursts.
- Scale ingestion and stream-processing consumers horizontally.
- Keep counter processing independent of the frontend request path.
- Avoid making every joining request synchronously wait for the global count to update.

The UI may briefly show a slightly stale count. That's better than making the entire system unavailable.

### B. What if the user sends duplicate heartbeats?

Suppose a client sends the same heartbeat three times because of a network retry.

We must not increment the counter three times.

Our processing rule is:

```
if session is INACTIVE:
    mark ACTIVE
    increment count

if session is ACTIVE:
    refresh lastHeartbeat
    do not increment
```

The same rule applies in reverse when decrementing.

### C. What if heartbeat processing is delayed?

Imagine a heartbeat was generated at 10:00:00 but Kafka processing happens at 10:00:20.

If we blindly trust the client's timestamp, clock differences and delayed events could cause incorrect counts.

Use the server-side ingestion timestamp for the lease, along with sequence/version handling for duplicate or out-of-order messages.

The count should also expose its `updatedAt` timestamp so that clients and monitoring systems can detect stale data.

### D. What happens when Redis goes down?

We shouldn't show zero just because Redis is unavailable.

Instead:

- Continue retaining the durable presence events.
- Preserve processor state through checkpoints and changelogs.
- Recover the materialized counters from the processor state or a durable snapshot.
- Serve the last known count with an indication that it is stale.
- Reconcile counters after recovery.

A temporary stale count is better than a completely misleading zero.

### E. How do we handle multiple regions?

Suppose viewers are watching from India, Australia, Europe, and the US.

We should avoid making every heartbeat travel to one central region.

Instead:

```
India Presence Counter      = 4,000,000
Australia Presence Counter = 1,500,000
Europe Presence Counter    = 2,000,000
US Presence Counter        = 2,500,000
---------------------------------------
Global Count               = 10,000,000
```

Each region manages its own presence state and periodically publishes its count.

A global aggregator calculates the total.

We must handle session migration carefully. If a viewer reconnects in a different region, the old session must expire or be transferred without double-counting. A globally unique session ID and ownership/version mechanism can help.

### F. Can we make the count even cheaper with HyperLogLog?

There is another interesting alternative.

Suppose the product doesn't require a highly precise count. It is acceptable to show an estimate such as:

```
~10.2M watching
```

We can use HyperLogLog (HLL) to estimate distinct active sessions.

The approach:

1. Divide time into small buckets, say 5 seconds.
2. For each heartbeat, add the session ID to that bucket's HLL.
3. Keep the buckets for the active window, for example, the last 45 seconds.
4. Merge the relevant HLL buckets to estimate the number of distinct active sessions.

Conceptually:

```
event:ipl-final:presence:bucket:10:30:00
event:ipl-final:presence:bucket:10:30:05
event:ipl-final:presence:bucket:10:30:10
...
```

HLL automatically deduplicates session IDs within the merged window.

Advantages:

- Very memory-efficient.
- Good for massive numbers of distinct sessions.
- No need to maintain an individual decrement operation for every leaving viewer.

Disadvantages:

- Approximate results.
- Expiry is based on time buckets, so the active window isn't perfectly precise.
- You cannot directly remove a session from an HLL when it leaves.
- It may temporarily count inactive sessions.

This is a useful option when approximate counts are sufficient. I would not choose it when we need reliable session-level join/leave transitions.

## 9. Which approach should we actually choose?

Let's compare the three approaches.

| Criteria               | Redis Sorted Sets                 | Kafka + Stream Processing        | HyperLogLog                  |
| ---------------------- | --------------------------------- | -------------------------------- | ---------------------------- |
| Accuracy               | High with correct expiry handling | High with correct state handling | Approximate                  |
| Session-level tracking | Yes                               | Yes                              | No                           |
| Join/leave support     | Yes                               | Yes                              | No direct deletion           |
| Massive scale          | Requires sharding                 | Horizontally scalable            | Very efficient               |
| Operational complexity | Medium                            | High                             | Medium                       |
| Best use case          | Small/medium system               | Large production event           | Approximate audience metrics |

My recommendation:

Recommended for IPL-scale events

Kafka + Flink (or Kafka Streams) + sharded counters + Redis + regional fan-out.

Why?

- Millions of heartbeat events can be distributed across partitions.
- Stateful stream processing handles session expiry.
- Sharded counters avoid a single hot key.
- Redis makes the latest count cheap to read.
- Regional gateways distribute count updates efficiently.

For an initial product or smaller event, Redis Sorted Sets with sharding are a perfectly reasonable starting point. We don't need Kafka and Flink from day one.

## 10. Important interview discussion: Accuracy vs freshness

One thing worth understanding is that a heartbeat-based count is not a perfect measurement of the physical people watching a video at this exact instant.

Suppose someone loses internet connectivity.

Their session remains active until the lease expires. With a 45-second lease, the count can temporarily include them for up to approximately 45 seconds after their last valid heartbeat.

There is an unavoidable trade-off:

| Strategy                  | Benefit                                   | Cost                                             |
| ------------------------- | ----------------------------------------- | ------------------------------------------------ |
| Short heartbeat interval  | Faster updates                            | More network and processing load                 |
| Short expiry timeout      | Faster removal                            | More false removals during network interruptions |
| Long expiry timeout       | More tolerant of temporary network issues | More stale viewers                               |
| Frequent count broadcasts | Fresher UI                                | Higher fan-out cost                              |

For our example, a 15-second heartbeat interval and a 45-second lease are reasonable starting values. We'd tune them through production measurements.

One correction to the earlier timeline: if the last heartbeat was received at 10:00:30 and the lease is 45 seconds, the session expires at 10:01:15, not 10:01:00.

We can also smooth the number in the UI, but we should never hide stale backend data or pretend the count is more accurate than it is.

## 11. What should we monitor in production?

I would monitor these metrics:

| Metric                            | Why it matters                  |
| --------------------------------- | ------------------------------- |
| Heartbeats received/sec           | Ingestion load                  |
| Active sessions per event         | Presence system health          |
| Kafka consumer lag                | Delayed count updates           |
| Session expiry rate               | Disconnects and expiry behavior |
| Counter reconciliation difference | Counting correctness            |
| Redis latency                     | Count read/write performance    |
| Gateway connection count          | Connection capacity             |
| Count update propagation latency  | Real-time experience            |
| Regional count freshness          | Multi-region health             |

We should also periodically reconcile the sharded counters against the underlying presence state. This helps detect drift caused by failed processing, incorrect retries, or recovery bugs.

## 12. How I would explain this design in an HLD interview

Here is the sequence I would follow rather than jumping straight into Kafka and Redis.

## Interview design flow

1. Define viewer semantics

   Are we counting page visitors, active playback sessions, or unique accounts?
2. Define freshness and accuracy

   How quickly should the number change? Is an approximate count acceptable?
3. Estimate scale

   Concurrent viewers, heartbeat interval, events/sec, connection count, payload throughput.
4. Design presence tracking

   Join, heartbeat, leave, lease expiry, duplicate handling.
5. Design distributed counting

   Session state, Kafka partitioning, sharded counters, aggregation.
6. Design count delivery

   REST polling, CDN caching, WebSocket/SSE, regional fan-out.
7. Discuss failure scenarios

   Disconnects, duplicates, processor failures, hot partitions, regional failures.
8. Discuss trade-offs

   Redis Sorted Sets vs stream processing vs approximate counting.

### Common mistakes to avoid

1. Increment on join and decrement on leave only. This fails when clients disappear unexpectedly.
2. Use one Redis key for the whole event. This creates a hot-key bottleneck.
3. Partition Kafka only by event ID. A massive event can overwhelm one partition.
4. Increment on every heartbeat. You end up counting heartbeat requests instead of viewers.
5. Broadcast every heartbeat. You create unnecessary fan-out traffic.
6. Run SQL `COUNT(*)` for every viewer-count request. This is the wrong read path for a high-traffic live counter.
7. Assume exactly-once processing automatically solves everything. Counter updates and recovery must also be idempotent.

## Final architecture to remember

## Live Viewer Count — Core Design

Player / Client

Heartbeat every \~15 seconds

Regional Gateway

Authentication, validation, ingestion

Kafka

Distributed heartbeat events

Flink / Kafka Streams

Track session state, heartbeat lease and expiry

Sharded Counters

Increment/decrement active sessions

Aggregator + Redis

Materialize latest total count

Regional Pub/Sub + WebSocket / SSE

Distribute aggregated count snapshots

# 10,245,820

Watching now

The central concept: Don't try to maintain a single global counter that every viewer directly modifies. Track individual presence using leases, distribute that work across partitions and shards, aggregate periodically, and broadcast only the resulting count.

That is the fundamental pattern behind scalable real-time presence and online-user counting systems.





















Yes. In fact, HyperLogLog (HLL) becomes a very attractive option when you only need to display rounded viewer counts, such as `1.3K`, `55K`, or `1M`.

It changes the design considerably because we no longer need to maintain an exact count of every active session or explicitly decrement the count whenever someone leaves.

There is an important distinction, though:

- HyperLogLog: Designed to estimate the number of distinct elements. Excellent candidate for this use case.
- Bloom Filter: Designed to answer whether an element might exist in a set. Not a good primary data structure for calculating viewer counts.

Let's redesign the system around approximate counting.

# 1. First, understand the accuracy requirement

Previously, we assumed that we needed something like:

```
10,245,820 viewers
```

Now the UI might display:

```
10.2M viewers
```

Or even:

```
10M viewers
```

We don't need an exact number such as 10,245,820.

This gives us more flexibility:

1. We can use approximate data structures.
2. We don't need immediate decrement operations.
3. We can tolerate some delay before disconnected viewers disappear.
4. We can reduce the amount of state maintained by the backend.
5. We can aggregate and publish counts less frequently.

But remember: rounded display and approximate counting are two different things. We can have an accurate internal count and display a rounded value. Or we can use an approximate internal count and display a rounded value. We can choose the latter here.

# 2. Can we use HyperLogLog?

Yes — recommended

Let's understand why it fits.

Imagine the following viewers send heartbeats:

```
10:00:00

session_101
session_102
session_103
session_104
```

At 10:00:15:

```
session_101
session_102
session_103
session_104
session_105
```

At 10:00:30:

```
session_101
session_103
session_104
session_105
session_106
```

Notice something: sessions appear repeatedly because they send heartbeats.

We want to count distinct active sessions, not heartbeat messages.

HLL is designed precisely for estimating distinct elements.

For example:

```
HLL.add(session_101)
HLL.add(session_102)
HLL.add(session_103)
HLL.add(session_104)

HLL.add(session_101) // Duplicate
HLL.add(session_103) // Duplicate
HLL.add(session_105)
```

The estimated cardinality is approximately 5, not 7.

The most useful properties of HLL are:

- Repeated additions don't increase the distinct count.
- It uses very little memory compared with storing all individual session IDs.
- Multiple HLL sketches can be merged.
- It estimates cardinality with configurable statistical accuracy.

For a standard HLL, the approximate relative standard error is:

\\[ RSE \approx \frac{1.04}{\sqrt{m}} \\]

Where \\(m\\) is the number of internal registers.

For example:

| Registers | Approx. relative standard error |
| --------- | ------------------------------- |
| 4,096     | 1.625%                          |
| 16,384    | 0.8125%                         |
| 32,768    | 0.575%                          |
| 65,536    | 0.406%                          |

These are statistical standard errors, not guaranteed maximum error bounds.

At a million viewers, even an error of around 0.8% may be perfectly acceptable for a rounded display.

However, HLL has one important limitation.

HLL does not support deleting an individual viewer.

If `session_101` leaves, we cannot simply execute:

```
HLL.remove(session_101)
```

This is where we need to modify our design.

## 3. The trick: Time-bucketed HyperLogLog

We need to solve the deletion problem.

Instead of maintaining one HLL for the entire event, maintain separate HLL sketches for small time windows.

For example:

- Heartbeat interval: 30 seconds.
- Active window: 90 seconds.
- Time bucket: 5 seconds.

We create sketches like:

```
event:ipl-final:10:30:00:HLL
event:ipl-final:10:30:05:HLL
event:ipl-final:10:30:10:HLL
event:ipl-final:10:30:15:HLL
event:ipl-final:10:30:20:HLL
...
```

Every heartbeat adds the session ID to the HLL corresponding to its time bucket.

Let's see what happens.

### Example

At 10:30:00:

```
Bucket 10:30:00

session_101
session_102
session_103
```

At 10:30:05:

```
Bucket 10:30:05

session_101
session_102
session_104
```

At 10:30:10:

```
Bucket 10:30:10

session_101
session_105
session_106
```

Notice that `session_101` appears in all three buckets.

We merge the sketches:

```
HLL.merge(
    bucket_10_30_00,
    bucket_10_30_05,
    bucket_10_30_10
)
```

The result estimates:

```
session_101
session_102
session_103
session_104
session_105
session_106
```

Estimated distinct sessions: 6.

Even though the same session appeared in multiple buckets, HLL's union operation deduplicates it.

### Now, what happens when someone leaves?

Suppose `session_101` disconnects at 10:30:12.

We don't send a decrement operation.

We simply stop receiving its heartbeats.

As time advances, the buckets containing its last heartbeats eventually fall outside our 90-second active window.

At that point, the old buckets are excluded from the merge, and `session_101` disappears from the estimated count.

How the active window works

Illustrative timeline. Active window = 90 seconds.

10:30:00

10:31:30

Old buckets

Included in HLL merge

Future

As buckets expire, sessions that have not sent a recent heartbeat are naturally removed from the estimate.

This is called a sliding-window cardinality estimation approach.

There is a trade-off: if someone leaves, they may remain counted until their last heartbeat ages out of the window. With a 90-second window, that can be roughly 90 seconds, plus bucket-boundary granularity.

Also, a session can be temporarily omitted if its heartbeats are missed for longer than the active window.

This is acceptable when the product wants an approximate, rounded audience number rather than exact live presence.

One important rule: use the heartbeat's server-received timestamp for bucket assignment, not the time at which a delayed Kafka message happens to be processed.

## 4. Redesigned architecture using HLL

Now we can simplify the previous architecture.

\#chatgpt-mermaid-\_r_2pf\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_2pf\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_2pf\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_2pf\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_2pf\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2pf\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_2pf\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_2pf\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_2pf\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_2pf\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_2pf\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_2pf\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2pf\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2pf\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_2pf\_ p{margin:0;}#chatgpt-mermaid-\_r_2pf\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2pf\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2pf\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2pf\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_2pf\_ .label text,#chatgpt-mermaid-\_r_2pf\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2pf\_ .node rect,#chatgpt-mermaid-\_r_2pf\_ .node circle,#chatgpt-mermaid-\_r_2pf\_ .node ellipse,#chatgpt-mermaid-\_r_2pf\_ .node polygon,#chatgpt-mermaid-\_r_2pf\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_2pf\_ .rough-node .label text,#chatgpt-mermaid-\_r_2pf\_ .node .label text,#chatgpt-mermaid-\_r_2pf\_ .image-shape .label,#chatgpt-mermaid-\_r_2pf\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_2pf\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_2pf\_ .rough-node .label,#chatgpt-mermaid-\_r_2pf\_ .node .label,#chatgpt-mermaid-\_r_2pf\_ .image-shape .label,#chatgpt-mermaid-\_r_2pf\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_2pf\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_2pf\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2pf\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2pf\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_2pf\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_2pf\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_2pf\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_2pf\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_2pf\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_2pf\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_2pf\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2pf\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2pf\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_2pf\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2pf\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_2pf\_ .icon-shape,#chatgpt-mermaid-\_r_2pf\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_2pf\_ .icon-shape p,#chatgpt-mermaid-\_r_2pf\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_2pf\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_2pf\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_2pf\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_2pf\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_2pf\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_2pf\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_2pf\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_2pf\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_2pf\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2pf\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_2pf\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_2pf\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_2pf\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2pf\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_2pf\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_2pf\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2pf\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_2pf\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_2pf\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2pf\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_2pf\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2pf\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_2pf\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_2pf\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_2pf\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_2pf\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_2pf\_ .node rect,#chatgpt-mermaid-\_r_2pf\_ .node circle,#chatgpt-mermaid-\_r_2pf\_ .node ellipse,#chatgpt-mermaid-\_r_2pf\_ .node polygon,#chatgpt-mermaid-\_r_2pf\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_2pf\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_2pf\_ .mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_2pf\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_2pf\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2pf\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}Millions of Video PlayersRegional API GatewaysKafka / Event StreamHLL Stream ProcessorsRegion 1: Time-bucketedHLLsRegion 2: Time-bucketedHLLsRegion 3: Time-bucketedHLLsMerge HLL SketchesEstimate Distinct ActiveSessionsRound / Format CountRedis: Latest Display CountPub/SubRegional WebSocket / SSEGatewaysViewers

Let's understand what changed.

### Step 1: Client sends heartbeat

```
{
  "eventId": "ipl-final",
  "sessionId": "vs_abc123",
  "status": "PLAYING"
}
```

Heartbeat every 20–30 seconds, with randomized jitter.

### Step 2: Gateway publishes heartbeat

The gateway attaches a server-side receipt timestamp and publishes the event to Kafka.

For a massive event, we still need distributed ingestion. HLL does not eliminate the incoming heartbeat traffic.

### Step 3: Processor updates HLL

The stream processor calculates a shard:

```
int shard = Math.floorMod(sessionId.hashCode(), 128);
```

It adds the session ID to the appropriate HLL:

```
HLL[eventId][region][shard][timeBucket]
    .add(sessionId)
```

A repeated heartbeat simply adds the same identifier again.

No increment operation is necessary.

No decrement operation is necessary.

No per-session expiry timer is necessary.

### Step 4: Merge sketches

Every 5–10 seconds, the aggregation process:

1. Selects the HLL buckets covering the active window.
2. Merges the sketches across shards.
3. Merges sketches across regions.
4. Estimates the number of distinct sessions.

One important detail: all sketches must use compatible HLL precision and the same hash function/seed.

Also, merge regional sketches rather than simply adding regional estimates. That allows the same session appearing in multiple regions to be deduplicated.

### Step 5: Publish rounded number

Suppose HLL estimates:

```
1,326,482
```

Our formatting layer converts this into:

```
1.3M
```

Redis stores the latest result:

```
event:ipl-final:display-count = "1.3M"
```

The frontend receives the new display value through WebSocket/SSE.

We can also use CDN-cached polling if persistent connections are unnecessary.

## 5. Do we still need Kafka, Redis, and Flink?

Some responsibilities disappear, but not all.

| Component        | Previous design                            | HLL design                                    |
| ---------------- | ------------------------------------------ | --------------------------------------------- |
| Gateway          | Receive heartbeats                         | Same                                          |
| Kafka            | Buffer heartbeats                          | Same                                          |
| Flink / Streams  | Track each session's active/inactive state | Maintain and merge time-bucketed HLL state    |
| Redis            | Store sharded exact counters               | Store latest estimate and optionally sketches |
| Count aggregator | Sum exact shard counts                     | Merge HLL sketches and estimate cardinality   |
| WebSocket/SSE    | Broadcast count                            | Same                                          |

What we eliminate:

- Exact active-session state.
- Per-session expiry timers.
- Atomic `+1` and `-1` counter operations.
- Exact distributed counter reconciliation.

We still need to handle event ingestion, buffering, aggregation, and delivery.

Important: HLL reduces the amount of state maintained. It does not automatically reduce heartbeat ingestion RPS. We can separately tune heartbeat frequency because we no longer require highly precise counts.

For example:

| Configuration           | Value        |
| ----------------------- | ------------ |
| Concurrent viewers      | 10 million   |
| Heartbeat interval      | 30 seconds   |
| Heartbeat rate          | \~333K/sec   |
| Active window           | 90 seconds   |
| Bucket size             | 5 seconds    |
| Display update interval | 5–10 seconds |

This is a reasonable starting configuration for testing, not a production capacity guarantee.

# 6. What about Bloom Filters?

Not recommended for primary counting

Bloom Filters answer a different question:

> Have I probably seen this element before?

For example:

```
BloomFilter.add(session_101)

BloomFilter.mightContain(session_101)
    → TRUE

BloomFilter.mightContain(session_999)
    → FALSE
```

A Bloom Filter can return false positives:

- `TRUE`: The element might exist.
- `FALSE`: The element definitely does not exist.

It cannot return false negatives, assuming normal operation and no deletion mistakes.

### Can we use it to estimate the number of viewers?

Technically, yes.

A Bloom Filter uses a bit array. As more distinct elements are inserted, more bits become set.

If we know:

- `m` = total bits
- `k` = number of hash functions
- `X` = number of bits currently set

We can estimate the number of distinct insertions using:

\\[ n \approx -\frac{m}{k}\ln\left(1-\frac{X}{m}\right) \\]

However, this is not its primary purpose.

Why would I choose HLL instead?

| Feature                           | Bloom Filter           | HyperLogLog     |
| --------------------------------- | ---------------------- | --------------- |
| Membership checking               | Excellent              | No              |
| Cardinality estimation            | Possible, but indirect | Designed for it |
| Duplicate handling                | Yes                    | Yes             |
| Merge operations                  | Bitwise OR             | HLL union       |
| Individual deletion               | No (standard BF)       | No              |
| Memory efficiency for cardinality | Good                   | Excellent       |
| Suitable for viewer count         | Not preferred          | Yes             |

A Counting Bloom Filter supports deletion, but introduces additional complexity and collision-related counting problems.

My choice: HLL for viewer counting. Bloom Filter is unnecessary for this particular requirement.

# 7. How do we handle rounded numbers properly?

Let's say HLL gives us:

```
1,326,482
```

We don't need to send that exact number to the frontend.

We can have a formatting service:

```
public class ViewerCountFormatter {

    public static String format(long count) {

        if (count < 1_000) {
            return String.valueOf(count);
        }

        if (count < 10_000) {
            return String.format("%.1fK", count / 1_000.0);
        }

        if (count < 1_000_000) {
            return Math.round(count / 1_000.0) + "K";
        }

        return String.format("%.1fM", count / 1_000_000.0);
    }
}
```

Example:

| Estimated count | Display |
| --------------- | ------- |
| 985             | 985     |
| 1,326           | 1.3K    |
| 5,850           | 5.9K    |
| 55,240          | 55K     |
| 125,800         | 126K    |
| 1,326,482       | 1.3M    |
| 10,245,820      | 10.2M   |

You can change the formatting rules depending on the product.

### One additional optimization: Don't broadcast unnecessary updates

Imagine the estimated count changes:

```
55,210 → 55,240 → 55,190 → 55,310
```

The UI displays `55K` throughout.

There is no reason to broadcast all four updates.

We can publish only when:

1. The formatted display value changes.
2. A maximum refresh interval has elapsed.

We should also use a small hysteresis around display boundaries to prevent values fluctuating around a threshold from making the UI flicker.

This reduces WebSocket traffic substantially, especially when the displayed number is rounded to millions.

# 8. One important architectural limitation

There is something I want you to remember when explaining this in an interview.

HLL does not give us an exact current active-session count. It gives us an estimate of distinct sessions observed within a time window.

These are not exactly the same.

Consider:

```
10:00:00  session_101 sends heartbeat
10:00:05  session_101 disconnects
10:00:10  session_102 sends heartbeat
```

Our HLL still contains `session_101`.

It will remain in the estimated count until its time bucket expires from the active window.

Also, we cannot merge all HLL buckets into one permanent sketch and subtract the expired bucket later. HLL union is not reversible.

Therefore, we retain individual time-bucket sketches and periodically merge only the buckets inside the current window.

This is an important implementation detail.

# 9. Which design should we choose?

I would make the decision based on the product's actual requirements.

Option A: HLL + Time Buckets

My recommendation

# Approximate

Use when the UI displays rounded counts and tolerates delayed removal of disconnected viewers.

No individual session expiry state or decrement operations required.

Option B: Redis Presence + Exact Counters

# More precise

Use when viewers need to disappear quickly after leaving, or when other features depend on exact online presence.

Requires session tracking, expiration, and correct atomic counter updates.

Option C: Hybrid

# Different accuracy for different needs

Use exact presence for business-critical functionality and HLL for large-scale audience metrics and analytics.

For example, a live sports platform might use exact presence for a participant list but HLL for the public viewer-count display.

## 10. My final recommended design

For your IPL example, I would choose:

| Layer           | Technology / approach                         |
| --------------- | --------------------------------------------- |
| Client          | Heartbeat every 20–30 seconds                 |
| Ingestion       | Regional API gateways                         |
| Event streaming | Kafka                                         |
| Processing      | Flink or Kafka Streams                        |
| Counting        | Time-bucketed HyperLogLog                     |
| Distribution    | Merge HLL sketches across shards and regions  |
| Aggregation     | Estimate distinct sessions every 5–10 seconds |
| Storage         | Redis for the latest estimate/display value   |
| Fan-out         | Regional Pub/Sub + WebSocket/SSE              |
| UI              | Rounded count with hysteresis                 |

## Final simplified architecture

10 Million Video Players

Heartbeat every 20–30 seconds

Regional Gateways + Kafka

Distributed event ingestion

Time-bucketed HLL

Add session IDs, retain recent buckets

Merge + Cardinality Estimation

Estimate distinct active sessions

Format + Redis

Publish only meaningful display changes

Regional Fan-out

WebSocket / SSE

# 1.3M

Watching now

The key interview insight:

When exactness is not required, don't build an exact distributed counting system unnecessarily.

Use a probabilistic cardinality data structure, a sliding time window for active presence, and rounded output to reduce processing state and delivery overhead.

One final distinction: HLL helps reduce the state and counting complexity, while rounding helps reduce the number of UI updates. These are two independent optimizations, and together they make this design much more efficient.
