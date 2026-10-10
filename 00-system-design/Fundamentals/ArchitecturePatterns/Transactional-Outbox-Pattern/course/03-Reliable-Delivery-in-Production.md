# Chapter 3 — Reliable Delivery in Production

In Chapter 2, we implemented the basic outbox pattern using PostgreSQL, Spring Boot, and Kafka.

Now we need to address the harder production questions:

- What happens when Kafka is unavailable for several minutes?
- How do we run multiple outbox relays without processing the same row unnecessarily?
- What happens if Kafka accepts an event, but the relay crashes before updating PostgreSQL?
- How do we prevent duplicate events from causing duplicate business operations?
- Should we poll the outbox table or use Change Data Capture (CDC) with Debezium?

The central principle for this chapter is:

Reliable messaging requires both reliable publication and duplicate-safe consumption.

## 1. Understand the complete delivery pipeline

There are several independent steps between creating an order and applying its effect in another service.

\#chatgpt-mermaid-\_r_sc\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,"Segoe UI","Helvetica","Apple Color Emoji","Arial",sans-serif,"Segoe UI Emoji","Segoe UI Symbol";font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_sc\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_sc\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_sc\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_sc\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_sc\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_sc\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_sc\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_sc\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_sc\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_sc\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_sc\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_sc\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_sc\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,"Segoe UI","Helvetica","Apple Color Emoji","Arial",sans-serif,"Segoe UI Emoji","Segoe UI Symbol";font-size:16px;}#chatgpt-mermaid-\_r_sc\_ p{margin:0;}#chatgpt-mermaid-\_r_sc\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,"Segoe UI","Helvetica","Apple Color Emoji","Arial",sans-serif,"Segoe UI Emoji","Segoe UI Symbol";color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_sc\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_sc\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_sc\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_sc\_ .label text,#chatgpt-mermaid-\_r_sc\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_sc\_ .node rect,#chatgpt-mermaid-\_r_sc\_ .node circle,#chatgpt-mermaid-\_r_sc\_ .node ellipse,#chatgpt-mermaid-\_r_sc\_ .node polygon,#chatgpt-mermaid-\_r_sc\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_sc\_ .rough-node .label text,#chatgpt-mermaid-\_r_sc\_ .node .label text,#chatgpt-mermaid-\_r_sc\_ .image-shape .label,#chatgpt-mermaid-\_r_sc\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_sc\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_sc\_ .rough-node .label,#chatgpt-mermaid-\_r_sc\_ .node .label,#chatgpt-mermaid-\_r_sc\_ .image-shape .label,#chatgpt-mermaid-\_r_sc\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_sc\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_sc\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_sc\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_sc\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_sc\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_sc\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_sc\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_sc\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_sc\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_sc\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_sc\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_sc\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_sc\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,"Segoe UI","Helvetica","Apple Color Emoji","Arial",sans-serif,"Segoe UI Emoji","Segoe UI Symbol";font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_sc\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_sc\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_sc\_ .icon-shape,#chatgpt-mermaid-\_r_sc\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_sc\_ .icon-shape p,#chatgpt-mermaid-\_r_sc\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_sc\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_sc\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_sc\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_sc\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_sc\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_sc\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_sc\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_sc\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_sc\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_sc\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_sc\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_sc\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_sc\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_sc\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_sc\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_sc\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_sc\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_sc\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_sc\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_sc\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_sc\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_sc\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_sc\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_sc\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_sc\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_sc\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_sc\_ .node rect,#chatgpt-mermaid-\_r_sc\_ .node circle,#chatgpt-mermaid-\_r_sc\_ .node ellipse,#chatgpt-mermaid-\_r_sc\_ .node polygon,#chatgpt-mermaid-\_r_sc\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_sc\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_sc\_ .node.mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_sc\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_sc\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_sc\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,"Segoe UI","Helvetica","Apple Color Emoji","Arial",sans-serif,"Segoe UI Emoji","Segoe UI Symbol";}Order ServicePostgreSQL transactionOrder row + Outbox eventOutbox Relay / CDCConnectorKafka topicInventory ServiceInventory databasetransactionInventory reserved

Each boundary introduces a potential failure.

For example, the relay might successfully publish the event, but the inventory service might crash before committing its database transaction.

Or the inventory service might commit successfully and crash before acknowledging its Kafka message.

Therefore, we must reason about reliability at three distinct boundaries:

1. Persistence: Was the business change and its corresponding event saved atomically?
2. Publication: Did the event reach the broker?
3. Consumption: Was the business effect applied successfully, and can it be safely repeated?

The outbox pattern primarily solves the first problem and helps make the second recoverable. Consumers and their transaction strategies address the third.

## 2. Polling vs. Change Data Capture (CDC)

There are two common approaches for publishing outbox events.

### Approach A: Polling

A continuously running process queries PostgreSQL for unpublished records.

\#chatgpt-mermaid-\_r_sl\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,"Segoe UI","Helvetica","Apple Color Emoji","Arial",sans-serif,"Segoe UI Emoji","Segoe UI Symbol";font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_sl\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_sl\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_sl\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_sl\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_sl\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_sl\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_sl\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_sl\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_sl\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_sl\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_sl\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_sl\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_sl\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,"Segoe UI","Helvetica","Apple Color Emoji","Arial",sans-serif,"Segoe UI Emoji","Segoe UI Symbol";font-size:16px;}#chatgpt-mermaid-\_r_sl\_ p{margin:0;}#chatgpt-mermaid-\_r_sl\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,"Segoe UI","Helvetica","Apple Color Emoji","Arial",sans-serif,"Segoe UI Emoji","Segoe UI Symbol";color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_sl\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_sl\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_sl\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_sl\_ .label text,#chatgpt-mermaid-\_r_sl\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_sl\_ .node rect,#chatgpt-mermaid-\_r_sl\_ .node circle,#chatgpt-mermaid-\_r_sl\_ .node ellipse,#chatgpt-mermaid-\_r_sl\_ .node polygon,#chatgpt-mermaid-\_r_sl\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_sl\_ .rough-node .label text,#chatgpt-mermaid-\_r_sl\_ .node .label text,#chatgpt-mermaid-\_r_sl\_ .image-shape .label,#chatgpt-mermaid-\_r_sl\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_sl\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_sl\_ .rough-node .label,#chatgpt-mermaid-\_r_sl\_ .node .label,#chatgpt-mermaid-\_r_sl\_ .image-shape .label,#chatgpt-mermaid-\_r_sl\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_sl\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_sl\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_sl\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_sl\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_sl\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_sl\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_sl\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_sl\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_sl\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_sl\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_sl\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_sl\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_sl\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,"Segoe UI","Helvetica","Apple Color Emoji","Arial",sans-serif,"Segoe UI Emoji","Segoe UI Symbol";font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_sl\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_sl\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_sl\_ .icon-shape,#chatgpt-mermaid-\_r_sl\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_sl\_ .icon-shape p,#chatgpt-mermaid-\_r_sl\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_sl\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_sl\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_sl\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_sl\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_sl\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_sl\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_sl\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_sl\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_sl\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_sl\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_sl\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_sl\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_sl\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_sl\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_sl\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_sl\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_sl\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_sl\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_sl\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_sl\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_sl\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_sl\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_sl\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_sl\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_sl\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_sl\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_sl\_ .node rect,#chatgpt-mermaid-\_r_sl\_ .node circle,#chatgpt-mermaid-\_r_sl\_ .node ellipse,#chatgpt-mermaid-\_r_sl\_ .node polygon,#chatgpt-mermaid-\_r_sl\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_sl\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_sl\_ .node.mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_sl\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_sl\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_sl\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,"Segoe UI","Helvetica","Apple Color Emoji","Arial",sans-serif,"Segoe UI Emoji","Segoe UI Symbol";}Scheduled relayQuery pending outbox rowsClaim a batchPublish events to KafkaPublication succeeded?Mark rows publishedRetry or record failureNext polling cycleYesNo

For example:

```
SELECT id, aggregate_id, event_type, payload
FROM outbox_events
WHERE published_at IS NULL
ORDER BY created_at, id
LIMIT 100;
```

This query illustrates how pending events are found. It is not sufficient by itself for a multi-instance production relay because two instances could select the same rows.

A production polling implementation needs a concurrency strategy, such as atomic claiming, a lease, or row locking with `FOR UPDATE SKIP LOCKED`. It should claim rows in a short database transaction, publish outside that transaction, and update the outcome afterward.

Advantages

- Relatively straightforward to implement.
- No separate CDC connector is required.
- Easy to inspect and replay pending records.
- Fits applications that already use scheduled jobs and relational databases.

Trade-offs

- Frequent polling adds database load.
- The polling interval introduces publication latency.
- The relay must manage claiming, locking, retries, and concurrency.
- Large outbox tables require good indexing and a retention strategy.

### Approach B: Change Data Capture (CDC)

Instead of repeatedly querying the outbox table, a CDC connector observes committed database changes through the database's change log.

For PostgreSQL, Debezium can read changes from the Write-Ahead Log (WAL). Its outbox event routing transformation can route outbox records to Kafka topics.

\#chatgpt-mermaid-\_r_t5\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,"Segoe UI","Helvetica","Apple Color Emoji","Arial",sans-serif,"Segoe UI Emoji","Segoe UI Symbol";font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_t5\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_t5\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_t5\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_t5\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_t5\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_t5\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_t5\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_t5\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_t5\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_t5\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_t5\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_t5\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_t5\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,"Segoe UI","Helvetica","Apple Color Emoji","Arial",sans-serif,"Segoe UI Emoji","Segoe UI Symbol";font-size:16px;}#chatgpt-mermaid-\_r_t5\_ p{margin:0;}#chatgpt-mermaid-\_r_t5\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,"Segoe UI","Helvetica","Apple Color Emoji","Arial",sans-serif,"Segoe UI Emoji","Segoe UI Symbol";color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_t5\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_t5\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_t5\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_t5\_ .label text,#chatgpt-mermaid-\_r_t5\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_t5\_ .node rect,#chatgpt-mermaid-\_r_t5\_ .node circle,#chatgpt-mermaid-\_r_t5\_ .node ellipse,#chatgpt-mermaid-\_r_t5\_ .node polygon,#chatgpt-mermaid-\_r_t5\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_t5\_ .rough-node .label text,#chatgpt-mermaid-\_r_t5\_ .node .label text,#chatgpt-mermaid-\_r_t5\_ .image-shape .label,#chatgpt-mermaid-\_r_t5\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_t5\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_t5\_ .rough-node .label,#chatgpt-mermaid-\_r_t5\_ .node .label,#chatgpt-mermaid-\_r_t5\_ .image-shape .label,#chatgpt-mermaid-\_r_t5\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_t5\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_t5\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_t5\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_t5\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_t5\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_t5\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_t5\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_t5\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_t5\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_t5\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_t5\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_t5\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_t5\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,"Segoe UI","Helvetica","Apple Color Emoji","Arial",sans-serif,"Segoe UI Emoji","Segoe UI Symbol";font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_t5\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_t5\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_t5\_ .icon-shape,#chatgpt-mermaid-\_r_t5\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_t5\_ .icon-shape p,#chatgpt-mermaid-\_r_t5\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_t5\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_t5\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_t5\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_t5\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_t5\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_t5\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_t5\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_t5\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_t5\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_t5\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_t5\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_t5\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_t5\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_t5\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_t5\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_t5\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_t5\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_t5\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_t5\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_t5\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_t5\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_t5\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_t5\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_t5\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_t5\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_t5\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_t5\_ .node rect,#chatgpt-mermaid-\_r_t5\_ .node circle,#chatgpt-mermaid-\_r_t5\_ .node ellipse,#chatgpt-mermaid-\_r_t5\_ .node polygon,#chatgpt-mermaid-\_r_t5\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_t5\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_t5\_ .node.mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_t5\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_t5\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_t5\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,"Segoe UI","Helvetica","Apple Color Emoji","Arial",sans-serif,"Segoe UI Emoji","Segoe UI Symbol";}Application transactionInsert order + outbox eventPostgreSQL commitsPostgreSQL WALDebezium connectorOutbox event routingKafka topicConsumer service

The application still inserts the event into the outbox table within the same transaction as the business change. CDC changes how the event is relayed, not the fundamental atomicity guarantee.

Advantages

- Avoids repeated SQL polling for pending outbox rows.
- Captures committed changes from the database log.
- Can provide low-latency event publication without a custom polling scheduler.
- Can scale well for event-driven architectures with high message volume.

Trade-offs

- Requires infrastructure and operational knowledge for Kafka Connect, Debezium, and database log access.
- Connector offsets, restarts, replication slots, and WAL retention must be managed.
- Connector restarts or reprocessing can still result in duplicates.
- Outbox cleanup and replay policies must account for connector progress and recovery requirements.

### Which approach should you choose?

| Situation                                                   | Reasonable choice                                          |
| ----------------------------------------------------------- | ---------------------------------------------------------- |
| Small or moderate workload, few moving parts                | Polling                                                    |
| Existing scheduled-job infrastructure                       | Polling                                                    |
| High event volume and an established Kafka Connect platform | CDC                                                        |
| Need to minimize repeated database queries                  | CDC                                                        |
| Team lacks CDC operational expertise                        | Start with polling, if its load and latency are acceptable |

Neither approach removes the need for duplicate-safe consumers.

## 3. Why publication is usually at-least-once

Consider a relay publishing `evt-101`.

PostgreSQLKafkaOutbox RelayPostgreSQLKafkaOutbox Relay#chatgpt-mermaid-\_r_te\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,"Segoe UI","Helvetica","Apple Color Emoji","Arial",sans-serif,"Segoe UI Emoji","Segoe UI Symbol";font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_te\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_te\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_te\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_te\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_te\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_te\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_te\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_te\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_te\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_te\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_te\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_te\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_te\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,"Segoe UI","Helvetica","Apple Color Emoji","Arial",sans-serif,"Segoe UI Emoji","Segoe UI Symbol";font-size:16px;}#chatgpt-mermaid-\_r_te\_ p{margin:0;}#chatgpt-mermaid-\_r_te\_ .actor{stroke:rgb(31, 78, 148);fill:rgb(9, 23, 44);stroke-width:1;}#chatgpt-mermaid-\_r_te\_ rect.actor.outer-path[data-look="neo"]{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_te\_ rect.note[data-look="neo"]{stroke:rgb(58, 132, 63);fill:rgb(48, 48, 48);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_te\_ text.actor>tspan{fill:rgb(237, 237, 237);stroke:none;}#chatgpt-mermaid-\_r_te\_ .actor-line{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_te\_ .innerArc{stroke-width:1.5;stroke-dasharray:none;}#chatgpt-mermaid-\_r_te\_ .messageLine0{stroke-width:1.5;stroke-dasharray:none;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_te\_ .messageLine1{stroke-width:1.5;stroke-dasharray:2,2;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_te\_ [id$="-arrowhead"] path{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_te\_ .sequenceNumber{fill:#505050;}#chatgpt-mermaid-\_r_te\_ [id$="-sequencenumber"]{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_te\_ [id$="-crosshead"] path{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_te\_ .messageText{fill:rgb(237, 237, 237);stroke:none;}#chatgpt-mermaid-\_r_te\_ .labelBox{stroke:rgba(255, 255, 255, 0.15);fill:rgb(0, 0, 0);filter:none;}#chatgpt-mermaid-\_r_te\_ .labelText,#chatgpt-mermaid-\_r_te\_ .labelText>tspan{fill:rgb(237, 237, 237);stroke:none;}#chatgpt-mermaid-\_r_te\_ .loopText,#chatgpt-mermaid-\_r_te\_ .loopText>tspan{fill:rgb(237, 237, 237);stroke:none;}#chatgpt-mermaid-\_r_te\_ .sectionTitle,#chatgpt-mermaid-\_r_te\_ .sectionTitle>tspan{fill:rgb(237, 237, 237);stroke:none;}#chatgpt-mermaid-\_r_te\_ .loopLine{stroke-width:2px;stroke-dasharray:2,2;stroke:rgba(255, 255, 255, 0.15);fill:rgba(255, 255, 255, 0.15);}#chatgpt-mermaid-\_r_te\_ .note{stroke:rgb(58, 132, 63);fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_te\_ .noteText,#chatgpt-mermaid-\_r_te\_ .noteText>tspan{fill:rgb(237, 237, 237);stroke:none;font-weight:normal;}#chatgpt-mermaid-\_r_te\_ .activation0{fill:rgb(48, 48, 48);stroke:hsl(0, 0%, 8.8235294118%);}#chatgpt-mermaid-\_r_te\_ .activation1{fill:rgb(48, 48, 48);stroke:hsl(0, 0%, 8.8235294118%);}#chatgpt-mermaid-\_r_te\_ .activation2{fill:rgb(48, 48, 48);stroke:hsl(0, 0%, 8.8235294118%);}#chatgpt-mermaid-\_r_te\_ .actorPopupMenu{position:absolute;}#chatgpt-mermaid-\_r_te\_ .actorPopupMenuPanel{position:absolute;fill:rgb(9, 23, 44);box-shadow:0px 8px 16px 0px rgba(0,0,0,0.2);filter:drop-shadow(3px 5px 2px rgb(0 0 0 / 0.4));}#chatgpt-mermaid-\_r_te\_ .actor-man circle,#chatgpt-mermaid-\_r_te\_ line{fill:rgb(9, 23, 44);stroke-width:2px;}#chatgpt-mermaid-\_r_te\_ g rect.rect{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_te\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_te\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_te\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_te\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_te\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_te\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_te\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_te\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_te\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_te\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_te\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_te\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_te\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_te\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_te\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_te\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_te\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_te\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,"Segoe UI","Helvetica","Apple Color Emoji","Arial",sans-serif,"Segoe UI Emoji","Segoe UI Symbol";}Relay crashes herePublish evt-101AcknowledgmentProcess restarts and checks statusevt-101 is still pendingPublish evt-101 againAcknowledgmentMark evt-101 published

The database status update and Kafka publication are separate operations.

If the relay marks the event as published before publishing it, a crash can permanently skip the event.

If the relay publishes first and marks it published afterward, a crash between the two steps can cause the event to be published again.

This leaves a fundamental trade-off for a straightforward outbox relay:

- Marking too early risks losing messages.
- Marking after publication permits duplicate publication.

For business-critical events, the usual choice is to favor at-least-once delivery with idempotent consumers rather than risk silently losing an event.

At-least-once means the system makes repeated publication attempts until success or an explicit failure-handling policy intervenes. It does not mean every event is guaranteed to succeed regardless of permanent failures, expired data, or operator decisions.

## 4. Handle retries correctly

A reliable relay must distinguish failures that may recover from failures that require investigation.

### Transient failures

Examples include:

- Kafka is temporarily unavailable.
- A network connection times out.
- A broker leader is temporarily changing.
- PostgreSQL is temporarily unavailable when recording a publication outcome.

These generally call for retrying, subject to appropriate timeouts and a bounded backoff policy.

### Permanent or repeatedly failing events

Examples include:

- The payload is malformed.
- The event schema is incompatible with the configured serializer.
- The event exceeds an enforced message-size limit.
- A configuration or authorization error persists.

Repeatedly retrying a permanent failure every second wastes resources and can delay healthy events.

### Use exponential backoff with jitter

A typical policy increases the delay after each failed attempt, applies a maximum delay, and adds randomness to prevent many relay instances from retrying simultaneously.

For example, a configurable backoff policy might resemble:

| Failed attempt | Illustrative base delay |
| -------------- | ----------------------- |
| 1              | 1 second                |
| 2              | 2 seconds               |
| 3              | 4 seconds               |
| 4              | 8 seconds               |
| 5              | 16 seconds              |

These are illustrative values, not universal production defaults. A real policy should be selected according to the service's latency requirements, failure modes, and retry budget. Jitter is applied around the calculated delays.

A practical policy should also define:

- Maximum attempts or maximum retry age.
- Maximum delay.
- Whether some failures should be retried indefinitely.
- How failed records are surfaced to operators.
- How an event can be safely redriven after remediation.

### What about a dead-letter queue?

A dead-letter queue (DLQ) is a common option for messages that cannot be processed successfully after a defined policy.

However, an important distinction exists for outbox relays: publishing a failed event to a DLQ is itself another publication operation. If Kafka is unavailable, sending the event to a Kafka DLQ may fail too.

For a polling relay, you can keep the original event in PostgreSQL and record attempts, the last error, and an explicit failure state. This preserves the event for inspection and controlled replay.

For CDC, the connector's error-handling, retry, and dead-letter configuration must be designed alongside the source outbox and connector recovery strategy.

Never silently discard a business-critical outbox event just because it has exceeded a retry count. Make the state observable and establish an explicit recovery procedure.

## 5. Idempotent consumers: prevent duplicate business effects

An operation is idempotent if repeating it with the same logical input does not change the final result after the first successful application.

For example, consider an inventory service processing `OrderCreated`.

An unsafe implementation might increment a counter every time the event arrives:

```
inventoryRepository.incrementReservedQuantity(productId, quantity);
```

If Kafka delivers the same event twice, the reservation may be applied twice.

The consumer needs a way to recognize that it has already processed a particular event.

### Step 1: Create a processed-events table

```
CREATE TABLE processed_events (
    consumer_name VARCHAR(100) NOT NULL,
    event_id UUID NOT NULL,
    processed_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    PRIMARY KEY (consumer_name, event_id)
);
```

The composite primary key prevents the same consumer from recording an event more than once.

The `consumer_name` matters because multiple independent consumers may legitimately process the same event. For example, both `inventory-service` and `notification-service` may need to process `OrderCreated`.

### Step 2: Deduplicate and apply the business operation in one transaction

The following example uses Spring's `JdbcTemplate` to atomically insert the deduplication record only if it does not already exist.

```
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryEventHandler {

    private static final String CONSUMER = "inventory-service";

    private final JdbcTemplate jdbcTemplate;
    private final InventoryRepository inventoryRepository;

    public InventoryEventHandler(
            JdbcTemplate jdbcTemplate,
            InventoryRepository inventoryRepository
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.inventoryRepository = inventoryRepository;
    }

    @Transactional
    public void handle(OrderCreatedEvent event) {
        int inserted = jdbcTemplate.update("""
            INSERT INTO processed_events (consumer_name, event_id)
            VALUES (?, ?)
            ON CONFLICT (consumer_name, event_id) DO NOTHING
            """,
            CONSUMER,
            event.eventId()
        );

        if (inserted == 0) {
            // This consumer has already processed this event.
            return;
        }

        // Perform the actual business operation.
        // For example, reserve inventory for this order.
        inventoryRepository.reserveForOrder(event.orderId());
    }
}
```

Assumptions: `InventoryRepository` writes to the same PostgreSQL database and participates in the same transaction. `reserveForOrder` represents your actual inventory business logic.

The essential sequence is:

\#chatgpt-mermaid-\_r_uf\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,"Segoe UI","Helvetica","Apple Color Emoji","Arial",sans-serif,"Segoe UI Emoji","Segoe UI Symbol";font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_uf\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_uf\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_uf\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_uf\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_uf\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_uf\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_uf\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_uf\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_uf\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_uf\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_uf\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_uf\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_uf\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,"Segoe UI","Helvetica","Apple Color Emoji","Arial",sans-serif,"Segoe UI Emoji","Segoe UI Symbol";font-size:16px;}#chatgpt-mermaid-\_r_uf\_ p{margin:0;}#chatgpt-mermaid-\_r_uf\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,"Segoe UI","Helvetica","Apple Color Emoji","Arial",sans-serif,"Segoe UI Emoji","Segoe UI Symbol";color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_uf\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_uf\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_uf\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_uf\_ .label text,#chatgpt-mermaid-\_r_uf\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_uf\_ .node rect,#chatgpt-mermaid-\_r_uf\_ .node circle,#chatgpt-mermaid-\_r_uf\_ .node ellipse,#chatgpt-mermaid-\_r_uf\_ .node polygon,#chatgpt-mermaid-\_r_uf\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_uf\_ .rough-node .label text,#chatgpt-mermaid-\_r_uf\_ .node .label text,#chatgpt-mermaid-\_r_uf\_ .image-shape .label,#chatgpt-mermaid-\_r_uf\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_uf\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_uf\_ .rough-node .label,#chatgpt-mermaid-\_r_uf\_ .node .label,#chatgpt-mermaid-\_r_uf\_ .image-shape .label,#chatgpt-mermaid-\_r_uf\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_uf\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_uf\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_uf\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_uf\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_uf\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_uf\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_uf\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_uf\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_uf\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_uf\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_uf\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_uf\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_uf\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,"Segoe UI","Helvetica","Apple Color Emoji","Arial",sans-serif,"Segoe UI Emoji","Segoe UI Symbol";font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_uf\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_uf\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_uf\_ .icon-shape,#chatgpt-mermaid-\_r_uf\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_uf\_ .icon-shape p,#chatgpt-mermaid-\_r_uf\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_uf\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_uf\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_uf\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_uf\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_uf\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_uf\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_uf\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_uf\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_uf\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_uf\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_uf\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_uf\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_uf\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_uf\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_uf\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_uf\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_uf\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_uf\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_uf\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_uf\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_uf\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_uf\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_uf\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_uf\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_uf\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_uf\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_uf\_ .node rect,#chatgpt-mermaid-\_r_uf\_ .node circle,#chatgpt-mermaid-\_r_uf\_ .node ellipse,#chatgpt-mermaid-\_r_uf\_ .node polygon,#chatgpt-mermaid-\_r_uf\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_uf\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_uf\_ .node.mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_uf\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_uf\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_uf\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,"Segoe UI","Helvetica","Apple Color Emoji","Arial",sans-serif,"Segoe UI Emoji","Segoe UI Symbol";}Receive eventInsert consumer_name +event_idNew event?Skip business operationApply inventory changeBusiness operationsucceeds?Commit dedupe record +inventory changeRoll back both databasechangesNoYesYesNo

### Why must the deduplication record and business change share a transaction?

Consider two possible implementations.

Incorrect: deduplicate in one transaction, then update inventory separately.

```
1. Record event as processed.
2. Commit the deduplication record.
3. Process crashes before inventory update.
4. Kafka redelivers event.
5. Consumer sees event as already processed.
6. Inventory is never updated.
```

The event has effectively been lost from the consumer's business perspective.

Correct: deduplication and inventory update in one transaction.

```
BEGIN

    Insert processed event if not already present.

    If newly inserted:
        Reserve inventory.

COMMIT
```

If inventory processing fails, the transaction rolls back both changes. A subsequent delivery can retry the entire operation.

If the transaction commits but Kafka's offset acknowledgment fails, the event may be delivered again. This time, the deduplication record exists, so the business operation is skipped.

This is the core of a transactional inbox or consumer deduplication pattern.

### Important limitation

This technique gives you effectively-once business effects for operations protected by that same database transaction. It does not automatically make external operations idempotent.

For example, calling a payment provider or sending an email inside this transaction cannot be rolled back by PostgreSQL. Such workflows need additional safeguards, such as provider-supported idempotency keys, another outbox, or a saga-based design.

## 6. Kafka consumer offsets: another important failure window

Kafka tracks consumer progress using offsets. A consumer typically commits its progress after its message-handling logic succeeds.

The order of these operations matters when a consumer updates a database.

### Safe ordering

Consumer DatabaseConsumerKafkaConsumer DatabaseConsumerKafka#chatgpt-mermaid-\_r_v4\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,"Segoe UI","Helvetica","Apple Color Emoji","Arial",sans-serif,"Segoe UI Emoji","Segoe UI Symbol";font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_v4\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_v4\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_v4\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_v4\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_v4\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_v4\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_v4\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_v4\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_v4\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_v4\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_v4\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_v4\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_v4\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,"Segoe UI","Helvetica","Apple Color Emoji","Arial",sans-serif,"Segoe UI Emoji","Segoe UI Symbol";font-size:16px;}#chatgpt-mermaid-\_r_v4\_ p{margin:0;}#chatgpt-mermaid-\_r_v4\_ .actor{stroke:rgb(31, 78, 148);fill:rgb(9, 23, 44);stroke-width:1;}#chatgpt-mermaid-\_r_v4\_ rect.actor.outer-path[data-look="neo"]{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_v4\_ rect.note[data-look="neo"]{stroke:rgb(58, 132, 63);fill:rgb(48, 48, 48);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_v4\_ text.actor>tspan{fill:rgb(237, 237, 237);stroke:none;}#chatgpt-mermaid-\_r_v4\_ .actor-line{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_v4\_ .innerArc{stroke-width:1.5;stroke-dasharray:none;}#chatgpt-mermaid-\_r_v4\_ .messageLine0{stroke-width:1.5;stroke-dasharray:none;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_v4\_ .messageLine1{stroke-width:1.5;stroke-dasharray:2,2;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_v4\_ [id$="-arrowhead"] path{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_v4\_ .sequenceNumber{fill:#505050;}#chatgpt-mermaid-\_r_v4\_ [id$="-sequencenumber"]{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_v4\_ [id$="-crosshead"] path{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_v4\_ .messageText{fill:rgb(237, 237, 237);stroke:none;}#chatgpt-mermaid-\_r_v4\_ .labelBox{stroke:rgba(255, 255, 255, 0.15);fill:rgb(0, 0, 0);filter:none;}#chatgpt-mermaid-\_r_v4\_ .labelText,#chatgpt-mermaid-\_r_v4\_ .labelText>tspan{fill:rgb(237, 237, 237);stroke:none;}#chatgpt-mermaid-\_r_v4\_ .loopText,#chatgpt-mermaid-\_r_v4\_ .loopText>tspan{fill:rgb(237, 237, 237);stroke:none;}#chatgpt-mermaid-\_r_v4\_ .sectionTitle,#chatgpt-mermaid-\_r_v4\_ .sectionTitle>tspan{fill:rgb(237, 237, 237);stroke:none;}#chatgpt-mermaid-\_r_v4\_ .loopLine{stroke-width:2px;stroke-dasharray:2,2;stroke:rgba(255, 255, 255, 0.15);fill:rgba(255, 255, 255, 0.15);}#chatgpt-mermaid-\_r_v4\_ .note{stroke:rgb(58, 132, 63);fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_v4\_ .noteText,#chatgpt-mermaid-\_r_v4\_ .noteText>tspan{fill:rgb(237, 237, 237);stroke:none;font-weight:normal;}#chatgpt-mermaid-\_r_v4\_ .activation0{fill:rgb(48, 48, 48);stroke:hsl(0, 0%, 8.8235294118%);}#chatgpt-mermaid-\_r_v4\_ .activation1{fill:rgb(48, 48, 48);stroke:hsl(0, 0%, 8.8235294118%);}#chatgpt-mermaid-\_r_v4\_ .activation2{fill:rgb(48, 48, 48);stroke:hsl(0, 0%, 8.8235294118%);}#chatgpt-mermaid-\_r_v4\_ .actorPopupMenu{position:absolute;}#chatgpt-mermaid-\_r_v4\_ .actorPopupMenuPanel{position:absolute;fill:rgb(9, 23, 44);box-shadow:0px 8px 16px 0px rgba(0,0,0,0.2);filter:drop-shadow(3px 5px 2px rgb(0 0 0 / 0.4));}#chatgpt-mermaid-\_r_v4\_ .actor-man circle,#chatgpt-mermaid-\_r_v4\_ line{fill:rgb(9, 23, 44);stroke-width:2px;}#chatgpt-mermaid-\_r_v4\_ g rect.rect{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_v4\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_v4\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_v4\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_v4\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_v4\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_v4\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_v4\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_v4\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_v4\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_v4\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_v4\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_v4\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_v4\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_v4\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_v4\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_v4\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_v4\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_v4\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,"Segoe UI","Helvetica","Apple Color Emoji","Arial",sans-serif,"Segoe UI Emoji","Segoe UI Symbol";}Deliver eventBegin transactionDeduplicate + update business stateTransaction committedCommit consumer offset

What if the consumer crashes after the database commits but before Kafka commits the offset?

Kafka can deliver the event again. The consumer's deduplication record makes that replay safe.

This is why the database transaction should finish successfully before the message is acknowledged or its offset is committed.

### Dangerous ordering

Consumer DatabaseConsumerKafkaConsumer DatabaseConsumerKafka#chatgpt-mermaid-\_r_vd\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,"Segoe UI","Helvetica","Apple Color Emoji","Arial",sans-serif,"Segoe UI Emoji","Segoe UI Symbol";font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_vd\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_vd\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_vd\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_vd\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_vd\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_vd\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_vd\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_vd\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_vd\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_vd\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_vd\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_vd\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_vd\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,"Segoe UI","Helvetica","Apple Color Emoji","Arial",sans-serif,"Segoe UI Emoji","Segoe UI Symbol";font-size:16px;}#chatgpt-mermaid-\_r_vd\_ p{margin:0;}#chatgpt-mermaid-\_r_vd\_ .actor{stroke:rgb(31, 78, 148);fill:rgb(9, 23, 44);stroke-width:1;}#chatgpt-mermaid-\_r_vd\_ rect.actor.outer-path[data-look="neo"]{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_vd\_ rect.note[data-look="neo"]{stroke:rgb(58, 132, 63);fill:rgb(48, 48, 48);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_vd\_ text.actor>tspan{fill:rgb(237, 237, 237);stroke:none;}#chatgpt-mermaid-\_r_vd\_ .actor-line{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_vd\_ .innerArc{stroke-width:1.5;stroke-dasharray:none;}#chatgpt-mermaid-\_r_vd\_ .messageLine0{stroke-width:1.5;stroke-dasharray:none;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_vd\_ .messageLine1{stroke-width:1.5;stroke-dasharray:2,2;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_vd\_ [id$="-arrowhead"] path{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_vd\_ .sequenceNumber{fill:#505050;}#chatgpt-mermaid-\_r_vd\_ [id$="-sequencenumber"]{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_vd\_ [id$="-crosshead"] path{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_vd\_ .messageText{fill:rgb(237, 237, 237);stroke:none;}#chatgpt-mermaid-\_r_vd\_ .labelBox{stroke:rgba(255, 255, 255, 0.15);fill:rgb(0, 0, 0);filter:none;}#chatgpt-mermaid-\_r_vd\_ .labelText,#chatgpt-mermaid-\_r_vd\_ .labelText>tspan{fill:rgb(237, 237, 237);stroke:none;}#chatgpt-mermaid-\_r_vd\_ .loopText,#chatgpt-mermaid-\_r_vd\_ .loopText>tspan{fill:rgb(237, 237, 237);stroke:none;}#chatgpt-mermaid-\_r_vd\_ .sectionTitle,#chatgpt-mermaid-\_r_vd\_ .sectionTitle>tspan{fill:rgb(237, 237, 237);stroke:none;}#chatgpt-mermaid-\_r_vd\_ .loopLine{stroke-width:2px;stroke-dasharray:2,2;stroke:rgba(255, 255, 255, 0.15);fill:rgba(255, 255, 255, 0.15);}#chatgpt-mermaid-\_r_vd\_ .note{stroke:rgb(58, 132, 63);fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_vd\_ .noteText,#chatgpt-mermaid-\_r_vd\_ .noteText>tspan{fill:rgb(237, 237, 237);stroke:none;font-weight:normal;}#chatgpt-mermaid-\_r_vd\_ .activation0{fill:rgb(48, 48, 48);stroke:hsl(0, 0%, 8.8235294118%);}#chatgpt-mermaid-\_r_vd\_ .activation1{fill:rgb(48, 48, 48);stroke:hsl(0, 0%, 8.8235294118%);}#chatgpt-mermaid-\_r_vd\_ .activation2{fill:rgb(48, 48, 48);stroke:hsl(0, 0%, 8.8235294118%);}#chatgpt-mermaid-\_r_vd\_ .actorPopupMenu{position:absolute;}#chatgpt-mermaid-\_r_vd\_ .actorPopupMenuPanel{position:absolute;fill:rgb(9, 23, 44);box-shadow:0px 8px 16px 0px rgba(0,0,0,0.2);filter:drop-shadow(3px 5px 2px rgb(0 0 0 / 0.4));}#chatgpt-mermaid-\_r_vd\_ .actor-man circle,#chatgpt-mermaid-\_r_vd\_ line{fill:rgb(9, 23, 44);stroke-width:2px;}#chatgpt-mermaid-\_r_vd\_ g rect.rect{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_vd\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_vd\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_vd\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_vd\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_vd\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_vd\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_vd\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_vd\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_vd\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_vd\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_vd\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_vd\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_vd\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_vd\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_vd\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_vd\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_vd\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_vd\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,"Segoe UI","Helvetica","Apple Color Emoji","Arial",sans-serif,"Segoe UI Emoji","Segoe UI Symbol";}Process crashesDeliver eventCommit offset too earlyStart database operationMessage will not be normally redelivered

The consumer may have advanced past the message even though its business operation never committed.

Rule: Do not acknowledge or commit progress before successfully persisting the consumer's business outcome.

In Spring Kafka, configure acknowledgment and transaction behavior deliberately. If a listener delegates to a separate `@Transactional` handler using the database transaction manager, ensure the handler's transaction has committed before the listener successfully returns to the Kafka container.

Kafka transactions can coordinate Kafka records and Kafka offsets under supported configurations. They do not, by themselves, atomically coordinate a PostgreSQL transaction with a Kafka offset commit.

## 7. Does the outbox pattern guarantee exactly-once delivery?

No—not by itself.

Three concepts are frequently confused in interviews:

| Concept                | Meaning                                                                                    |
| ---------------------- | ------------------------------------------------------------------------------------------ |
| At-most-once delivery  | A message is delivered zero or one times; loss is possible.                                |
| At-least-once delivery | A message may be delivered more than once; retries aim to avoid loss.                      |
| Exactly-once effects   | A logical business operation has one effective application, even if the event is replayed. |

The outbox pattern is commonly paired with at-least-once publication and idempotent consumers to achieve effectively-once business outcomes within the defined transaction boundary.

Kafka's idempotent producer and transaction features help with specific kinds of duplicate production and Kafka-to-Kafka processing. They do not automatically make this entire sequence atomic:

```
PostgreSQL outbox
        ↓
Kafka publication
        ↓
Consumer PostgreSQL transaction
        ↓
External API call
```

Each system boundary still needs an appropriate reliability strategy.

A useful interview answer is:

> The transactional outbox provides atomic persistence of the business change and the intention to publish an event. Publication is commonly at least once because a relay can crash after publishing but before recording success. Consumers use durable deduplication and transactional business updates to prevent repeated delivery from creating repeated database effects. Exactly-once behavior must be defined for a specific boundary rather than assumed for the entire distributed workflow.

## 8. Message ordering: what if two events arrive out of order?

Suppose the Order Service emits two events:

```
OrderCreated     → order-101
OrderCancelled   → order-101
```

If `OrderCancelled` is processed before `OrderCreated`, a consumer that assumes creation always arrives first may behave incorrectly.

Kafka guarantees ordering within a single partition, not across all partitions. A common approach is to use the aggregate ID as the Kafka message key:

```
kafkaTemplate.send(
    "order-events",
    event.aggregateId().toString(),
    serializedEvent
);
```

All events using the same key are routed to the same partition under the normal partitioning strategy, preserving the order in which Kafka appends them to that partition.

However, the Kafka key does not fix out-of-order publication by the relay. If multiple workers publish events for the same aggregate in the wrong order, Kafka will faithfully preserve the wrong order.

For strict per-aggregate ordering, the design may need:

- An aggregate version or sequence number on every event.
- A relay that respects event order for each aggregate.
- Consumer validation for missing or unexpected sequence numbers.
- A recovery strategy for gaps or out-of-order events.

For many systems, per-aggregate ordering is the relevant requirement; global ordering across all business events is usually much more expensive and often unnecessary.

## 9. Failure scenarios: what should the system do?

This table is useful for both production troubleshooting and system design interviews.

| Failure point                                                 | Expected behavior                                      | Required protection                         |
| ------------------------------------------------------------- | ------------------------------------------------------ | ------------------------------------------- |
| Order transaction fails before commit                         | Neither order nor outbox event is committed            | Database transaction                        |
| Order transaction commits, but relay is unavailable           | Event remains persisted                                | Durable outbox                              |
| Kafka is temporarily unavailable                              | Publication is retried                                 | Retry policy and backoff                    |
| Kafka accepts event, but relay crashes before updating status | Event may be published again                           | Consumer idempotency                        |
| Consumer crashes before its database transaction commits      | Transaction rolls back; message can be retried         | Correct offset acknowledgment               |
| Consumer database commits, but offset commit fails            | Message may be delivered again                         | Durable deduplication                       |
| A permanently invalid event keeps failing                     | Event is isolated for investigation and recovery       | Failure state, alerts, and replay procedure |
| Two relay instances select the same event                     | Duplicate publication is possible without coordination | Atomic claiming or equivalent coordination  |

The goal is not to pretend failures cannot happen. It is to make each failure recoverable without losing business data or applying unintended duplicate effects.

## 10. Production monitoring and operations

A working outbox implementation needs operational visibility. At minimum, monitor these metrics.

| Metric                             | Why it matters                                                      |
| ---------------------------------- | ------------------------------------------------------------------- |
| Pending outbox event count         | Shows whether publication is falling behind.                        |
| Age of the oldest pending event    | Identifies stuck events even when the queue is small.               |
| Publication failure and retry rate | Reveals broker issues and persistent event failures.                |
| End-to-end event latency           | Measures the delay from event creation to consumption.              |
| Consumer lag                       | Shows whether downstream services are keeping up.                   |
| CDC connector health and lag       | Detects connector outages or a growing backlog of database changes. |
| Duplicate event count              | Helps diagnose replay, restart, or coordination behavior.           |

### Event retention and cleanup

Outbox rows cannot grow indefinitely without operational consequences.

A retention policy should specify how long successfully published records remain available before cleanup. Retention must account for auditing, debugging, replay requirements, and storage capacity.

For CDC implementations, cleanup must also be coordinated with the connector's recovery and snapshot behavior. Avoid deleting outbox records that are still needed for recovery or historical replay.

Failed events should be retained according to an explicit recovery policy, rather than being cleaned up merely because they are old.

## 11. Interview questions

### Q1. What is the difference between polling and CDC for an outbox?

Polling repeatedly queries the database for pending events and requires a strategy for claiming and updating rows. CDC observes committed database changes through a change log, such as PostgreSQL's WAL, and can publish them through a connector such as Debezium.

Polling is often simpler to operate at small and moderate scale. CDC can reduce polling overhead and scale well where its infrastructure is already supported.

### Q2. Why can an outbox event be published more than once?

Publishing the event and updating the outbox status are separate operations. If the broker accepts the event and the relay crashes before updating the database, the relay cannot safely infer from the database alone that publication occurred. Retrying may publish the event again.

### Q3. How do you prevent duplicate processing?

Give each event a stable ID. In the consumer's database transaction, insert that ID into a deduplication table with a unique constraint. Apply the business operation only when the insert succeeds. Commit the deduplication record and business change together.

### Q4. Is the outbox enough to guarantee exactly-once processing?

No. It ensures that the business change and the event record are persisted atomically. Duplicate publication and consumer redelivery remain possible. Effectively-once business effects require additional protections at the consuming side, and external effects need their own idempotency or coordination strategy.

### Q5. How do you handle an event that fails repeatedly?

Use a defined retry policy with backoff and jitter, track attempts and failure details, alert on persistent failures, and keep the event recoverable. Whether to stop automatic retries or continue retrying depends on the failure type and business requirements.

### Q6. Can multiple outbox relays run simultaneously?

Yes. A production polling implementation needs a safe claiming or coordination mechanism so multiple relays do not unnecessarily process the same record. Even with coordination, crashes and uncertain publication outcomes mean consumers should remain duplicate-safe.

## Chapter 3 — Key takeaways

You should now be able to explain the complete reliability model:

1. Polling and CDC are alternative ways to relay committed outbox events.
2. At-least-once publication favors retries over silently losing events, but can generate duplicates.
3. Idempotent consumers prevent repeated deliveries from duplicating business effects within a protected transaction.
4. Offset acknowledgment must follow successful business processing to avoid losing messages.
5. Ordering is a separate concern; partition keys help preserve order within a Kafka partition but cannot fix incorrect ordering by the publisher.
6. Monitoring and replay procedures are essential for handling persistent failures.

## Next: Chapter 4 — Advanced Outbox Design

We'll go deeper into concurrent relays, PostgreSQL's `FOR UPDATE SKIP LOCKED`, atomic event claiming, leases, strict per-aggregate ordering, retry starvation, retention, and scaling the outbox without creating database bottlenecks.
