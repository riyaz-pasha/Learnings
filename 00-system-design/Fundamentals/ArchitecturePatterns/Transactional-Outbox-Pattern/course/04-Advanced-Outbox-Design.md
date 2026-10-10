# Chapter 4 — Advanced Outbox Design

In the previous chapters, we implemented the basic Transactional Outbox Pattern and explored retries, CDC, and idempotent consumers.

Now we'll focus on the problems that emerge when the system runs in production with multiple relay instances, high message volume, strict ordering requirements, and long-running failures.

We'll continue with Spring Boot, PostgreSQL, and Kafka, using PostgreSQL polling as the main implementation example.

## 1. The multi-instance problem

Suppose your application runs three outbox relay instances for higher availability and throughput.

Each instance executes the same query:

```
SELECT *
FROM outbox_events
WHERE published_at IS NULL
ORDER BY created_at, id
LIMIT 100;
```

Imagine that 500 events are waiting to be published.

\#chatgpt-mermaid-\_r_11s\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,"Segoe UI","Helvetica","Apple Color Emoji","Arial",sans-serif,"Segoe UI Emoji","Segoe UI Symbol";font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_11s\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_11s\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_11s\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_11s\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_11s\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_11s\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_11s\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_11s\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_11s\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_11s\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_11s\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_11s\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_11s\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,"Segoe UI","Helvetica","Apple Color Emoji","Arial",sans-serif,"Segoe UI Emoji","Segoe UI Symbol";font-size:16px;}#chatgpt-mermaid-\_r_11s\_ p{margin:0;}#chatgpt-mermaid-\_r_11s\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,"Segoe UI","Helvetica","Apple Color Emoji","Arial",sans-serif,"Segoe UI Emoji","Segoe UI Symbol";color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_11s\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_11s\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_11s\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_11s\_ .label text,#chatgpt-mermaid-\_r_11s\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_11s\_ .node rect,#chatgpt-mermaid-\_r_11s\_ .node circle,#chatgpt-mermaid-\_r_11s\_ .node ellipse,#chatgpt-mermaid-\_r_11s\_ .node polygon,#chatgpt-mermaid-\_r_11s\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_11s\_ .rough-node .label text,#chatgpt-mermaid-\_r_11s\_ .node .label text,#chatgpt-mermaid-\_r_11s\_ .image-shape .label,#chatgpt-mermaid-\_r_11s\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_11s\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_11s\_ .rough-node .label,#chatgpt-mermaid-\_r_11s\_ .node .label,#chatgpt-mermaid-\_r_11s\_ .image-shape .label,#chatgpt-mermaid-\_r_11s\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_11s\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_11s\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_11s\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_11s\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_11s\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_11s\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_11s\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_11s\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_11s\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_11s\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_11s\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_11s\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_11s\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,"Segoe UI","Helvetica","Apple Color Emoji","Arial",sans-serif,"Segoe UI Emoji","Segoe UI Symbol";font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_11s\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_11s\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_11s\_ .icon-shape,#chatgpt-mermaid-\_r_11s\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_11s\_ .icon-shape p,#chatgpt-mermaid-\_r_11s\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_11s\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_11s\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_11s\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_11s\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_11s\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_11s\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_11s\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_11s\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_11s\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_11s\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_11s\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_11s\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_11s\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_11s\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_11s\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_11s\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_11s\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_11s\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_11s\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_11s\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_11s\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_11s\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_11s\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_11s\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_11s\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_11s\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_11s\_ .node rect,#chatgpt-mermaid-\_r_11s\_ .node circle,#chatgpt-mermaid-\_r_11s\_ .node ellipse,#chatgpt-mermaid-\_r_11s\_ .node polygon,#chatgpt-mermaid-\_r_11s\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_11s\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_11s\_ .node.mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_11s\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_11s\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_11s\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,"Segoe UI","Helvetica","Apple Color Emoji","Arial",sans-serif,"Segoe UI Emoji","Segoe UI Symbol";}500 pending eventsRelay 1Relay 2Relay 3Same query runsconcurrentlyMultiple relays select thesame rowsDuplicate Kafka publications

All three instances can select the same 100 rows because the query doesn't reserve them for a particular worker.

Even if you add `published_at IS NULL`, the problem remains: all three queries can observe those rows before any worker updates them.

This leads to unnecessary duplicate publication, wasted processing, and unpredictable load.

We need a mechanism to claim work atomically.

## 2. Use `FOR UPDATE SKIP LOCKED`

PostgreSQL provides `FOR UPDATE SKIP LOCKED`, which is particularly useful for concurrent job-processing workers.

- `FOR UPDATE` locks the selected rows against conflicting updates.
- `SKIP LOCKED` allows a worker to skip rows already locked by another transaction instead of waiting for them.

For example:

```
BEGIN;

SELECT id
FROM outbox_events
WHERE published_at IS NULL
ORDER BY created_at, id
LIMIT 100
FOR UPDATE SKIP LOCKED;

-- Claim or update the selected rows here.

COMMIT;
```

If Relay 1 locks the first 100 rows, Relay 2 can skip those rows and select other available rows.

However, there is a crucial detail: the row locks are released when the transaction ends.

Therefore, simply selecting rows using `FOR UPDATE SKIP LOCKED` and committing does not permanently claim them.

A production implementation needs to persist the claim or keep the transaction open. Keeping the transaction open while publishing over the network is generally undesirable, so we'll use a persistent claim.

## 3. Implement durable claims with leases

A lease is a temporary ownership assignment that expires automatically.

A relay claims a batch for a limited time. If the relay crashes, another worker can reclaim the events after the lease expires.

### Step 1: Extend the outbox schema

For more advanced coordination, replace the simple `published_at IS NULL` state model with explicit lifecycle fields.

```
ALTER TABLE outbox_events
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    ADD COLUMN claimed_by VARCHAR(100),
    ADD COLUMN claim_token UUID,
    ADD COLUMN lease_until TIMESTAMPTZ,
    ADD COLUMN next_attempt_at TIMESTAMPTZ NOT NULL DEFAULT NOW();

ALTER TABLE outbox_events
    ADD CONSTRAINT chk_outbox_status
    CHECK (status IN ('PENDING', 'PROCESSING', 'PUBLISHED', 'FAILED'));
```

These columns support the following states:

| State        | Meaning                                                                     |
| ------------ | --------------------------------------------------------------------------- |
| `PENDING`    | Ready for publication when its retry time arrives.                          |
| `PROCESSING` | Claimed by a relay instance.                                                |
| `PUBLISHED`  | Publication succeeded and was recorded in the database.                     |
| `FAILED`     | Automatic processing has been stopped pending investigation or remediation. |

The existing `attempts`, `last_error`, and `published_at` columns can remain.

This schema is illustrative: if you use it, migrate existing records and update the relay consistently rather than maintaining two conflicting definitions of whether an event is pending.

### Step 2: Claim events atomically

A common PostgreSQL pattern uses a short transaction with a CTE. The query selects eligible rows, locks them, updates their claim state, and returns the claimed records.

```
WITH candidates AS (
    SELECT id
    FROM outbox_events
    WHERE
        (
            status = 'PENDING'
            AND next_attempt_at <= NOW()
        )
        OR
        (
            status = 'PROCESSING'
            AND lease_until < NOW()
        )
    ORDER BY created_at, id
    LIMIT 100
    FOR UPDATE SKIP LOCKED
)
UPDATE outbox_events AS e
SET
    status = 'PROCESSING',
    claimed_by = :worker_id,
    claim_token = :claim_token,
    lease_until = NOW() + INTERVAL '60 seconds',
    attempts = attempts + 1
FROM candidates AS c
WHERE e.id = c.id
RETURNING e.*;
```

Use a fresh, unique `claim_token` for the batch or each claimed event, according to your implementation.

The caller executes this statement inside a database transaction and commits the claim before publishing to Kafka.

Why does this work?

1. The database locks eligible rows while choosing a batch.
2. Concurrent workers skip rows locked by another transaction.
3. The selected rows are changed to `PROCESSING`.
4. The claim is committed and remains visible after row locks are released.
5. Workers publish outside the claim transaction.

This separates database coordination from network communication.

The SQL illustrates the claiming technique. A production implementation should confirm its query plan, parameter types, locking behavior, and indexes under the chosen PostgreSQL version and workload.

### Step 3: Publish outside the claim transaction

KafkaPostgreSQLRelayKafkaPostgreSQLRelay#chatgpt-mermaid-\_r_12q\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,"Segoe UI","Helvetica","Apple Color Emoji","Arial",sans-serif,"Segoe UI Emoji","Segoe UI Symbol";font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_12q\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_12q\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_12q\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_12q\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_12q\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_12q\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_12q\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_12q\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_12q\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_12q\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_12q\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_12q\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_12q\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,"Segoe UI","Helvetica","Apple Color Emoji","Arial",sans-serif,"Segoe UI Emoji","Segoe UI Symbol";font-size:16px;}#chatgpt-mermaid-\_r_12q\_ p{margin:0;}#chatgpt-mermaid-\_r_12q\_ .actor{stroke:rgb(31, 78, 148);fill:rgb(9, 23, 44);stroke-width:1;}#chatgpt-mermaid-\_r_12q\_ rect.actor.outer-path[data-look="neo"]{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_12q\_ rect.note[data-look="neo"]{stroke:rgb(58, 132, 63);fill:rgb(48, 48, 48);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_12q\_ text.actor>tspan{fill:rgb(237, 237, 237);stroke:none;}#chatgpt-mermaid-\_r_12q\_ .actor-line{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_12q\_ .innerArc{stroke-width:1.5;stroke-dasharray:none;}#chatgpt-mermaid-\_r_12q\_ .messageLine0{stroke-width:1.5;stroke-dasharray:none;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_12q\_ .messageLine1{stroke-width:1.5;stroke-dasharray:2,2;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_12q\_ [id$="-arrowhead"] path{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_12q\_ .sequenceNumber{fill:#505050;}#chatgpt-mermaid-\_r_12q\_ [id$="-sequencenumber"]{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_12q\_ [id$="-crosshead"] path{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_12q\_ .messageText{fill:rgb(237, 237, 237);stroke:none;}#chatgpt-mermaid-\_r_12q\_ .labelBox{stroke:rgba(255, 255, 255, 0.15);fill:rgb(0, 0, 0);filter:none;}#chatgpt-mermaid-\_r_12q\_ .labelText,#chatgpt-mermaid-\_r_12q\_ .labelText>tspan{fill:rgb(237, 237, 237);stroke:none;}#chatgpt-mermaid-\_r_12q\_ .loopText,#chatgpt-mermaid-\_r_12q\_ .loopText>tspan{fill:rgb(237, 237, 237);stroke:none;}#chatgpt-mermaid-\_r_12q\_ .sectionTitle,#chatgpt-mermaid-\_r_12q\_ .sectionTitle>tspan{fill:rgb(237, 237, 237);stroke:none;}#chatgpt-mermaid-\_r_12q\_ .loopLine{stroke-width:2px;stroke-dasharray:2,2;stroke:rgba(255, 255, 255, 0.15);fill:rgba(255, 255, 255, 0.15);}#chatgpt-mermaid-\_r_12q\_ .note{stroke:rgb(58, 132, 63);fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_12q\_ .noteText,#chatgpt-mermaid-\_r_12q\_ .noteText>tspan{fill:rgb(237, 237, 237);stroke:none;font-weight:normal;}#chatgpt-mermaid-\_r_12q\_ .activation0{fill:rgb(48, 48, 48);stroke:hsl(0, 0%, 8.8235294118%);}#chatgpt-mermaid-\_r_12q\_ .activation1{fill:rgb(48, 48, 48);stroke:hsl(0, 0%, 8.8235294118%);}#chatgpt-mermaid-\_r_12q\_ .activation2{fill:rgb(48, 48, 48);stroke:hsl(0, 0%, 8.8235294118%);}#chatgpt-mermaid-\_r_12q\_ .actorPopupMenu{position:absolute;}#chatgpt-mermaid-\_r_12q\_ .actorPopupMenuPanel{position:absolute;fill:rgb(9, 23, 44);box-shadow:0px 8px 16px 0px rgba(0,0,0,0.2);filter:drop-shadow(3px 5px 2px rgb(0 0 0 / 0.4));}#chatgpt-mermaid-\_r_12q\_ .actor-man circle,#chatgpt-mermaid-\_r_12q\_ line{fill:rgb(9, 23, 44);stroke-width:2px;}#chatgpt-mermaid-\_r_12q\_ g rect.rect{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_12q\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_12q\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_12q\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_12q\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_12q\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_12q\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_12q\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_12q\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_12q\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_12q\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_12q\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_12q\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_12q\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_12q\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_12q\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_12q\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_12q\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_12q\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,"Segoe UI","Helvetica","Apple Color Emoji","Arial",sans-serif,"Segoe UI Emoji","Segoe UI Symbol";}No database transaction is held during network I/OClaim batch in transactionClaimed event rowsCommit claimPublish eventAcknowledgmentMark event PUBLISHEDCommit publication status

This design avoids holding database row locks open while waiting for Kafka.

It also lets you run multiple relay instances without having all of them continuously query and publish the same rows.

## 4. What if a relay crashes while holding a lease?

Suppose Relay 1 claims `evt-101` with a 60-second lease.

Relay 2PostgreSQLRelay 1Relay 2PostgreSQLRelay 1#chatgpt-mermaid-\_r_138\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,"Segoe UI","Helvetica","Apple Color Emoji","Arial",sans-serif,"Segoe UI Emoji","Segoe UI Symbol";font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_138\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_138\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_138\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_138\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_138\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_138\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_138\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_138\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_138\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_138\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_138\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_138\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_138\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,"Segoe UI","Helvetica","Apple Color Emoji","Arial",sans-serif,"Segoe UI Emoji","Segoe UI Symbol";font-size:16px;}#chatgpt-mermaid-\_r_138\_ p{margin:0;}#chatgpt-mermaid-\_r_138\_ .actor{stroke:rgb(31, 78, 148);fill:rgb(9, 23, 44);stroke-width:1;}#chatgpt-mermaid-\_r_138\_ rect.actor.outer-path[data-look="neo"]{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_138\_ rect.note[data-look="neo"]{stroke:rgb(58, 132, 63);fill:rgb(48, 48, 48);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_138\_ text.actor>tspan{fill:rgb(237, 237, 237);stroke:none;}#chatgpt-mermaid-\_r_138\_ .actor-line{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_138\_ .innerArc{stroke-width:1.5;stroke-dasharray:none;}#chatgpt-mermaid-\_r_138\_ .messageLine0{stroke-width:1.5;stroke-dasharray:none;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_138\_ .messageLine1{stroke-width:1.5;stroke-dasharray:2,2;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_138\_ [id$="-arrowhead"] path{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_138\_ .sequenceNumber{fill:#505050;}#chatgpt-mermaid-\_r_138\_ [id$="-sequencenumber"]{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_138\_ [id$="-crosshead"] path{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_138\_ .messageText{fill:rgb(237, 237, 237);stroke:none;}#chatgpt-mermaid-\_r_138\_ .labelBox{stroke:rgba(255, 255, 255, 0.15);fill:rgb(0, 0, 0);filter:none;}#chatgpt-mermaid-\_r_138\_ .labelText,#chatgpt-mermaid-\_r_138\_ .labelText>tspan{fill:rgb(237, 237, 237);stroke:none;}#chatgpt-mermaid-\_r_138\_ .loopText,#chatgpt-mermaid-\_r_138\_ .loopText>tspan{fill:rgb(237, 237, 237);stroke:none;}#chatgpt-mermaid-\_r_138\_ .sectionTitle,#chatgpt-mermaid-\_r_138\_ .sectionTitle>tspan{fill:rgb(237, 237, 237);stroke:none;}#chatgpt-mermaid-\_r_138\_ .loopLine{stroke-width:2px;stroke-dasharray:2,2;stroke:rgba(255, 255, 255, 0.15);fill:rgba(255, 255, 255, 0.15);}#chatgpt-mermaid-\_r_138\_ .note{stroke:rgb(58, 132, 63);fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_138\_ .noteText,#chatgpt-mermaid-\_r_138\_ .noteText>tspan{fill:rgb(237, 237, 237);stroke:none;font-weight:normal;}#chatgpt-mermaid-\_r_138\_ .activation0{fill:rgb(48, 48, 48);stroke:hsl(0, 0%, 8.8235294118%);}#chatgpt-mermaid-\_r_138\_ .activation1{fill:rgb(48, 48, 48);stroke:hsl(0, 0%, 8.8235294118%);}#chatgpt-mermaid-\_r_138\_ .activation2{fill:rgb(48, 48, 48);stroke:hsl(0, 0%, 8.8235294118%);}#chatgpt-mermaid-\_r_138\_ .actorPopupMenu{position:absolute;}#chatgpt-mermaid-\_r_138\_ .actorPopupMenuPanel{position:absolute;fill:rgb(9, 23, 44);box-shadow:0px 8px 16px 0px rgba(0,0,0,0.2);filter:drop-shadow(3px 5px 2px rgb(0 0 0 / 0.4));}#chatgpt-mermaid-\_r_138\_ .actor-man circle,#chatgpt-mermaid-\_r_138\_ line{fill:rgb(9, 23, 44);stroke-width:2px;}#chatgpt-mermaid-\_r_138\_ g rect.rect{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_138\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_138\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_138\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_138\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_138\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_138\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_138\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_138\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_138\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_138\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_138\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_138\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_138\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_138\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_138\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_138\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_138\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_138\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,"Segoe UI","Helvetica","Apple Color Emoji","Arial",sans-serif,"Segoe UI Emoji","Segoe UI Symbol";}Crashes before publishingEvent remains PROCESSINGClaim evt-101 until 14:01:00Claim grantedFind expired leases after 14:01:00evt-101 is eligible for reclaimClaim evt-101 with new tokenNew claim grantedPublish event

The lease makes abandoned work recoverable without requiring an operator to manually reset every stuck record.

But leases introduce a new issue: the original worker might still be running when its lease expires.

For example, Relay 1 could experience a long pause, lose its lease, and then resume while Relay 2 is processing the same event.

That is where claim tokens become important.

### Protect status updates with claim tokens

Every worker should condition its final status update on the claim it still owns.

```
UPDATE outbox_events
SET
    status = 'PUBLISHED',
    published_at = NOW(),
    claimed_by = NULL,
    claim_token = NULL,
    lease_until = NULL,
    last_error = NULL
WHERE id = :event_id
  AND status = 'PROCESSING'
  AND claim_token = :claim_token;
```

If a different worker has reclaimed the event with a new token, the old worker's update affects zero rows.

This protects the database status from stale workers.

However, it does not prevent a stale worker from publishing to Kafka after its lease expires. A database claim token does not automatically fence a Kafka send.

Consequently, leases reduce concurrent duplicate work, but duplicate-safe consumers are still required. If strict ownership or sequencing is essential, design stronger coordination around those requirements.

### How long should a lease be?

There is no universal lease duration.

A lease that's too short can expire while a worker is still legitimately publishing. A lease that's too long delays recovery after a crash.

Choose a duration based on observed processing latency and failure recovery objectives. For long-running batches, renew leases before they expire, and make sure the renewal update verifies the worker's current claim token.

## 5. Preserve event ordering for the same aggregate

Suppose an order generates these events:

```
OrderCreated       — order-101
OrderPaid          — order-101
OrderShipped       — order-101
```

The inventory or fulfillment service may depend on this order.

But consider what happens when multiple relay workers operate concurrently:

KafkaRelay 2Relay 1KafkaRelay 2Relay 1#chatgpt-mermaid-\_r_13u\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,"Segoe UI","Helvetica","Apple Color Emoji","Arial",sans-serif,"Segoe UI Emoji","Segoe UI Symbol";font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_13u\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_13u\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_13u\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_13u\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_13u\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_13u\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_13u\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_13u\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_13u\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_13u\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_13u\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_13u\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_13u\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,"Segoe UI","Helvetica","Apple Color Emoji","Arial",sans-serif,"Segoe UI Emoji","Segoe UI Symbol";font-size:16px;}#chatgpt-mermaid-\_r_13u\_ p{margin:0;}#chatgpt-mermaid-\_r_13u\_ .actor{stroke:rgb(31, 78, 148);fill:rgb(9, 23, 44);stroke-width:1;}#chatgpt-mermaid-\_r_13u\_ rect.actor.outer-path[data-look="neo"]{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_13u\_ rect.note[data-look="neo"]{stroke:rgb(58, 132, 63);fill:rgb(48, 48, 48);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_13u\_ text.actor>tspan{fill:rgb(237, 237, 237);stroke:none;}#chatgpt-mermaid-\_r_13u\_ .actor-line{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_13u\_ .innerArc{stroke-width:1.5;stroke-dasharray:none;}#chatgpt-mermaid-\_r_13u\_ .messageLine0{stroke-width:1.5;stroke-dasharray:none;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_13u\_ .messageLine1{stroke-width:1.5;stroke-dasharray:2,2;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_13u\_ [id$="-arrowhead"] path{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_13u\_ .sequenceNumber{fill:#505050;}#chatgpt-mermaid-\_r_13u\_ [id$="-sequencenumber"]{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_13u\_ [id$="-crosshead"] path{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_13u\_ .messageText{fill:rgb(237, 237, 237);stroke:none;}#chatgpt-mermaid-\_r_13u\_ .labelBox{stroke:rgba(255, 255, 255, 0.15);fill:rgb(0, 0, 0);filter:none;}#chatgpt-mermaid-\_r_13u\_ .labelText,#chatgpt-mermaid-\_r_13u\_ .labelText>tspan{fill:rgb(237, 237, 237);stroke:none;}#chatgpt-mermaid-\_r_13u\_ .loopText,#chatgpt-mermaid-\_r_13u\_ .loopText>tspan{fill:rgb(237, 237, 237);stroke:none;}#chatgpt-mermaid-\_r_13u\_ .sectionTitle,#chatgpt-mermaid-\_r_13u\_ .sectionTitle>tspan{fill:rgb(237, 237, 237);stroke:none;}#chatgpt-mermaid-\_r_13u\_ .loopLine{stroke-width:2px;stroke-dasharray:2,2;stroke:rgba(255, 255, 255, 0.15);fill:rgba(255, 255, 255, 0.15);}#chatgpt-mermaid-\_r_13u\_ .note{stroke:rgb(58, 132, 63);fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_13u\_ .noteText,#chatgpt-mermaid-\_r_13u\_ .noteText>tspan{fill:rgb(237, 237, 237);stroke:none;font-weight:normal;}#chatgpt-mermaid-\_r_13u\_ .activation0{fill:rgb(48, 48, 48);stroke:hsl(0, 0%, 8.8235294118%);}#chatgpt-mermaid-\_r_13u\_ .activation1{fill:rgb(48, 48, 48);stroke:hsl(0, 0%, 8.8235294118%);}#chatgpt-mermaid-\_r_13u\_ .activation2{fill:rgb(48, 48, 48);stroke:hsl(0, 0%, 8.8235294118%);}#chatgpt-mermaid-\_r_13u\_ .actorPopupMenu{position:absolute;}#chatgpt-mermaid-\_r_13u\_ .actorPopupMenuPanel{position:absolute;fill:rgb(9, 23, 44);box-shadow:0px 8px 16px 0px rgba(0,0,0,0.2);filter:drop-shadow(3px 5px 2px rgb(0 0 0 / 0.4));}#chatgpt-mermaid-\_r_13u\_ .actor-man circle,#chatgpt-mermaid-\_r_13u\_ line{fill:rgb(9, 23, 44);stroke-width:2px;}#chatgpt-mermaid-\_r_13u\_ g rect.rect{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_13u\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_13u\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_13u\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_13u\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_13u\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_13u\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_13u\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_13u\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_13u\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_13u\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_13u\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_13u\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_13u\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_13u\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_13u\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_13u\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_13u\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_13u\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,"Segoe UI","Helvetica","Apple Color Emoji","Arial",sans-serif,"Segoe UI Emoji","Segoe UI Symbol";}Claims OrderCreatedClaims OrderPaidPublishes OrderPaidAcknowledgmentPublishes OrderCreatedAcknowledgment

Even if the rows were selected in creation order, concurrent workers can publish them in the wrong order.

Using the aggregate ID as the Kafka key ensures that events for the same aggregate go to the same partition under the normal partitioning strategy. Kafka preserves the order in which records are appended to that partition, but it cannot correct events that the relay publishes in the wrong order.

### Step 1: Give each aggregate an event sequence

Add a sequence number or aggregate version to each event.

```
ALTER TABLE outbox_events
    ADD COLUMN aggregate_version BIGINT;
```

For a production implementation, the version must be assigned reliably as part of the business transaction. For example, the application can derive it from the aggregate's version or increment a per-aggregate sequence transactionally.

You can then enforce uniqueness:

```
CREATE UNIQUE INDEX uq_outbox_aggregate_version
    ON outbox_events (
        aggregate_type,
        aggregate_id,
        aggregate_version
    )
    WHERE aggregate_version IS NOT NULL;
```

This prevents two events from claiming the same aggregate version, but it does not assign sequence numbers by itself.

### Step 2: Do not claim later events while an earlier event is unpublished

Conceptually, a polling query should only select the earliest unpublished event for each aggregate.

\#chatgpt-mermaid-\_r_14l\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,"Segoe UI","Helvetica","Apple Color Emoji","Arial",sans-serif,"Segoe UI Emoji","Segoe UI Symbol";font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_14l\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_14l\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_14l\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_14l\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_14l\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_14l\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_14l\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_14l\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_14l\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_14l\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_14l\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_14l\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_14l\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,"Segoe UI","Helvetica","Apple Color Emoji","Arial",sans-serif,"Segoe UI Emoji","Segoe UI Symbol";font-size:16px;}#chatgpt-mermaid-\_r_14l\_ p{margin:0;}#chatgpt-mermaid-\_r_14l\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,"Segoe UI","Helvetica","Apple Color Emoji","Arial",sans-serif,"Segoe UI Emoji","Segoe UI Symbol";color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_14l\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_14l\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_14l\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_14l\_ .label text,#chatgpt-mermaid-\_r_14l\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_14l\_ .node rect,#chatgpt-mermaid-\_r_14l\_ .node circle,#chatgpt-mermaid-\_r_14l\_ .node ellipse,#chatgpt-mermaid-\_r_14l\_ .node polygon,#chatgpt-mermaid-\_r_14l\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_14l\_ .rough-node .label text,#chatgpt-mermaid-\_r_14l\_ .node .label text,#chatgpt-mermaid-\_r_14l\_ .image-shape .label,#chatgpt-mermaid-\_r_14l\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_14l\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_14l\_ .rough-node .label,#chatgpt-mermaid-\_r_14l\_ .node .label,#chatgpt-mermaid-\_r_14l\_ .image-shape .label,#chatgpt-mermaid-\_r_14l\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_14l\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_14l\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_14l\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_14l\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_14l\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_14l\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_14l\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_14l\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_14l\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_14l\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_14l\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_14l\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_14l\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,"Segoe UI","Helvetica","Apple Color Emoji","Arial",sans-serif,"Segoe UI Emoji","Segoe UI Symbol";font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_14l\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_14l\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_14l\_ .icon-shape,#chatgpt-mermaid-\_r_14l\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_14l\_ .icon-shape p,#chatgpt-mermaid-\_r_14l\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_14l\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_14l\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_14l\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_14l\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_14l\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_14l\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_14l\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_14l\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_14l\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_14l\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_14l\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_14l\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_14l\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_14l\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_14l\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_14l\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_14l\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_14l\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_14l\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_14l\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_14l\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_14l\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_14l\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_14l\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_14l\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_14l\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_14l\_ .node rect,#chatgpt-mermaid-\_r_14l\_ .node circle,#chatgpt-mermaid-\_r_14l\_ .node ellipse,#chatgpt-mermaid-\_r_14l\_ .node polygon,#chatgpt-mermaid-\_r_14l\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_14l\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_14l\_ .node.mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_14l\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_14l\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_14l\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,"Segoe UI","Helvetica","Apple Color Emoji","Arial",sans-serif,"Segoe UI Emoji","Segoe UI Symbol";}Order 101: version 1Order 101: version 2Order 101: version 3Version 1 published?Block versions 2 and 3Version 2 becomes eligibleVersion 2 published?Version 3 becomes eligibleKeep version 3 blockedNoYesYesNo

An illustrative eligibility condition is:

```
AND NOT EXISTS (
    SELECT 1
    FROM outbox_events AS earlier
    WHERE earlier.aggregate_type = e.aggregate_type
      AND earlier.aggregate_id = e.aggregate_id
      AND earlier.aggregate_version < e.aggregate_version
      AND earlier.status <> 'PUBLISHED'
)
```

This condition is intended to be combined with the claim query's other filters and aliases.

It prevents a later event from being claimed while an earlier event for the same aggregate remains unpublished, including one currently being processed or awaiting retry.

For correct strict ordering, the full design must also account for missing sequence numbers, concurrent creation of events, and permanently failed messages. A failed earlier event may need to block later events until an explicit recovery or skip decision is made.

### Step 3: Use the aggregate ID as the Kafka key

```
kafkaTemplate.send(
    "order-events",
    event.aggregateId().toString(),
    serializedEvent
);
```

This provides partition affinity for the aggregate.

The key distinction: Event sequencing, relay ordering, and Kafka partition ordering are three related but separate concerns. Correctness requires aligning all three if per-aggregate order is a business requirement.

Not every system needs strict ordering. If events are independent or consumers are designed to handle out-of-order updates, enforcing strict order can add unnecessary complexity and reduce throughput.

## 6. Prevent retry starvation

Consider a table with 10,000 pending events.

The oldest event fails repeatedly because its payload is malformed. If the relay always selects the oldest 100 events, the same failing event might be retried constantly, depending on the query and batch logic.

This causes a problem known as retry starvation: failing messages consume processing capacity and prevent healthy messages from making progress.

### Solution: Schedule the next retry

The `next_attempt_at` column we introduced earlier allows a failed event to become eligible only after its retry delay.

For example, after a transient publication failure:

```
UPDATE outbox_events
SET
    status = 'PENDING',
    claimed_by = NULL,
    claim_token = NULL,
    lease_until = NULL,
    next_attempt_at = NOW() + INTERVAL '30 seconds',
    last_error = :error
WHERE id = :event_id
  AND status = 'PROCESSING'
  AND claim_token = :claim_token;
```

As before, this should be executed in a database transaction, and the application should verify that the update affected the expected row.

The 30-second value is illustrative. A production retry policy should usually calculate an attempt-dependent delay with a cap and jitter.

Now the relay can process other eligible events while the failed event waits.

### What if strict ordering is enabled?

Retry scheduling and strict ordering create an important trade-off.

If version 1 of an order is permanently failing, allowing version 2 through could violate business semantics. Blocking version 2, however, means that this order cannot progress until version 1 is repaired or explicitly handled.

That is not necessarily a system-wide failure: unrelated aggregates can still progress.

A healthy design makes this choice explicit:

- For independent events, allow later work to proceed.
- For ordered aggregates, block the affected aggregate where necessary.
- For poison events, raise alerts and provide a repair or replay workflow.
- Never silently skip an event if doing so violates business invariants.

## 7. Design indexes for the relay workload

As the outbox grows, indexing becomes important for keeping claims efficient.

A basic polling index is:

```
CREATE INDEX idx_outbox_pending
    ON outbox_events (created_at, id)
    WHERE status = 'PENDING';
```

However, this is only a starting point.

Our claiming query also considers:

- The `next_attempt_at` time.
- Events in `PROCESSING` whose leases have expired.
- Potentially earlier unpublished events for the same aggregate.

One index may not efficiently serve all of these conditions. PostgreSQL can benefit from separate partial indexes for distinct eligibility paths.

For example:

```
CREATE INDEX idx_outbox_ready
    ON outbox_events (next_attempt_at, created_at, id)
    WHERE status = 'PENDING';

CREATE INDEX idx_outbox_expired_lease
    ON outbox_events (lease_until, created_at, id)
    WHERE status = 'PROCESSING';
```

The best index depends on data distribution, query plans, update frequency, and the ordering requirements. Validate the actual query with `EXPLAIN (ANALYZE, BUFFERS)` against representative data.

Avoid automatically indexing every outbox column. Each additional index makes inserts and updates more expensive and consumes storage.

### Why avoid indexing the JSON payload?

The relay usually needs to locate rows by status, retry time, lease time, creation time, or aggregate identity. It then retrieves the payload for the selected rows.

An index on a large JSON payload is unnecessary unless you have a separate query requirement that justifies it.

## 8. Clean up published events safely

Outbox tables are write-heavy. Without a retention strategy, they can grow indefinitely, increasing storage consumption and operational overhead.

A basic cleanup might delete records published more than seven days ago:

```
DELETE FROM outbox_events
WHERE status = 'PUBLISHED'
  AND published_at < NOW() - INTERVAL '7 days';
```

For a large table, deleting millions of rows in one transaction can create excessive WAL, hold locks longer than necessary, and increase table bloat. Use bounded batches or a suitable partition-retention strategy.

For example, bounded deletion:

```
WITH old_events AS (
    SELECT id
    FROM outbox_events
    WHERE status = 'PUBLISHED'
      AND published_at < NOW() - INTERVAL '7 days'
    ORDER BY published_at, id
    LIMIT 5000
)
DELETE FROM outbox_events AS e
USING old_events AS old
WHERE e.id = old.id;
```

Run bounded batches under a scheduled cleanup job until the eligible backlog is cleared.

### What should we retain?

Retention should be based on more than storage capacity.

Consider:

- How long operators need events for debugging.
- Whether business audit requirements apply.
- Whether events must be replayable.
- Whether downstream consumers need historical events to recover.
- Whether CDC connector recovery depends on source-table contents.

Do not delete pending or failed records merely because they are old. They may represent unfinished business operations.

Also, CDC introduces a separate recovery concern: source outbox retention and WAL retention are not interchangeable. Connector progress, recovery procedures, and any required replay history must be considered independently.

## 9. Scaling the outbox when traffic grows

An outbox works well at many scales, but the database can become a bottleneck if the event volume grows significantly.

A useful progression is to scale incrementally.

### Stage 1: Optimize basic polling

Start with:

- Correct partial indexes.
- Reasonable batch sizes.
- Short claim transactions.
- A small number of relay workers.
- Retry scheduling and lease recovery.
- Efficient cleanup of published rows.

Avoid adding complexity before measuring the actual bottleneck.

### Stage 2: Increase relay concurrency

Multiple workers can claim separate batches with `FOR UPDATE SKIP LOCKED`.

However, increasing worker count indefinitely can create database contention, too many simultaneous Kafka requests, and heavy status-update traffic.

Control concurrency and batch size using measured throughput and latency.

### Stage 3: Consider table partitioning

If the outbox accumulates a high volume of records, partitioning by a suitable time range may improve operational management and retention.

For instance:

\#chatgpt-mermaid-\_r_16f\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,"Segoe UI","Helvetica","Apple Color Emoji","Arial",sans-serif,"Segoe UI Emoji","Segoe UI Symbol";font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_16f\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_16f\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_16f\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_16f\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_16f\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_16f\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_16f\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_16f\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_16f\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_16f\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_16f\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_16f\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_16f\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,"Segoe UI","Helvetica","Apple Color Emoji","Arial",sans-serif,"Segoe UI Emoji","Segoe UI Symbol";font-size:16px;}#chatgpt-mermaid-\_r_16f\_ p{margin:0;}#chatgpt-mermaid-\_r_16f\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,"Segoe UI","Helvetica","Apple Color Emoji","Arial",sans-serif,"Segoe UI Emoji","Segoe UI Symbol";color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_16f\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_16f\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_16f\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_16f\_ .label text,#chatgpt-mermaid-\_r_16f\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_16f\_ .node rect,#chatgpt-mermaid-\_r_16f\_ .node circle,#chatgpt-mermaid-\_r_16f\_ .node ellipse,#chatgpt-mermaid-\_r_16f\_ .node polygon,#chatgpt-mermaid-\_r_16f\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_16f\_ .rough-node .label text,#chatgpt-mermaid-\_r_16f\_ .node .label text,#chatgpt-mermaid-\_r_16f\_ .image-shape .label,#chatgpt-mermaid-\_r_16f\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_16f\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_16f\_ .rough-node .label,#chatgpt-mermaid-\_r_16f\_ .node .label,#chatgpt-mermaid-\_r_16f\_ .image-shape .label,#chatgpt-mermaid-\_r_16f\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_16f\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_16f\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_16f\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_16f\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_16f\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_16f\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_16f\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_16f\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_16f\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_16f\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_16f\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_16f\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_16f\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,"Segoe UI","Helvetica","Apple Color Emoji","Arial",sans-serif,"Segoe UI Emoji","Segoe UI Symbol";font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_16f\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_16f\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_16f\_ .icon-shape,#chatgpt-mermaid-\_r_16f\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_16f\_ .icon-shape p,#chatgpt-mermaid-\_r_16f\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_16f\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_16f\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_16f\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_16f\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_16f\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_16f\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_16f\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_16f\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_16f\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_16f\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_16f\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_16f\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_16f\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_16f\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_16f\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_16f\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_16f\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_16f\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_16f\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_16f\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_16f\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_16f\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_16f\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_16f\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_16f\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_16f\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_16f\_ .node rect,#chatgpt-mermaid-\_r_16f\_ .node circle,#chatgpt-mermaid-\_r_16f\_ .node ellipse,#chatgpt-mermaid-\_r_16f\_ .node polygon,#chatgpt-mermaid-\_r_16f\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_16f\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_16f\_ .node.mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_16f\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_16f\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_16f\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,"Segoe UI","Helvetica","Apple Color Emoji","Arial",sans-serif,"Segoe UI Emoji","Segoe UI Symbol";}outbox_eventsOctober partitionNovember partitionDecember partitionRetain or remove accordingto policyActive event processing

This is a conceptual example. Partition keys and retention design must account for PostgreSQL's partitioning rules, unique constraints, indexes, and how pending records are found.

A particularly important trade-off is that dropping an entire old partition is efficient, but time-based partitioning is not automatically suitable for every outbox lifecycle. Pending or failed messages may survive far longer than the normal retention period, so they must not be lost when an old partition is removed.

### Stage 4: Consider CDC

When repeated database polling or row-status updates become expensive, CDC with Debezium is worth evaluating.

CDC avoids the need for a custom poll-and-claim loop, although the database, connector, and Kafka infrastructure still incur costs. You must also design for connector recovery, duplicate delivery, WAL retention, and event cleanup.

CDC is not automatically faster or simpler for every workload. Measure the actual bottleneck before migrating.

## 10. Production design: putting the pieces together

Here's an example of how the advanced design fits together.

\#chatgpt-mermaid-\_r_16o\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,"Segoe UI","Helvetica","Apple Color Emoji","Arial",sans-serif,"Segoe UI Emoji","Segoe UI Symbol";font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_16o\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_16o\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_16o\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_16o\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_16o\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_16o\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_16o\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_16o\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_16o\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_16o\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_16o\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_16o\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_16o\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,"Segoe UI","Helvetica","Apple Color Emoji","Arial",sans-serif,"Segoe UI Emoji","Segoe UI Symbol";font-size:16px;}#chatgpt-mermaid-\_r_16o\_ p{margin:0;}#chatgpt-mermaid-\_r_16o\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,"Segoe UI","Helvetica","Apple Color Emoji","Arial",sans-serif,"Segoe UI Emoji","Segoe UI Symbol";color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_16o\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_16o\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_16o\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_16o\_ .label text,#chatgpt-mermaid-\_r_16o\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_16o\_ .node rect,#chatgpt-mermaid-\_r_16o\_ .node circle,#chatgpt-mermaid-\_r_16o\_ .node ellipse,#chatgpt-mermaid-\_r_16o\_ .node polygon,#chatgpt-mermaid-\_r_16o\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_16o\_ .rough-node .label text,#chatgpt-mermaid-\_r_16o\_ .node .label text,#chatgpt-mermaid-\_r_16o\_ .image-shape .label,#chatgpt-mermaid-\_r_16o\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_16o\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_16o\_ .rough-node .label,#chatgpt-mermaid-\_r_16o\_ .node .label,#chatgpt-mermaid-\_r_16o\_ .image-shape .label,#chatgpt-mermaid-\_r_16o\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_16o\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_16o\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_16o\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_16o\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_16o\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_16o\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_16o\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_16o\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_16o\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_16o\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_16o\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_16o\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_16o\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,"Segoe UI","Helvetica","Apple Color Emoji","Arial",sans-serif,"Segoe UI Emoji","Segoe UI Symbol";font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_16o\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_16o\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_16o\_ .icon-shape,#chatgpt-mermaid-\_r_16o\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_16o\_ .icon-shape p,#chatgpt-mermaid-\_r_16o\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_16o\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_16o\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_16o\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_16o\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_16o\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_16o\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_16o\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_16o\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_16o\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_16o\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_16o\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_16o\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_16o\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_16o\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_16o\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_16o\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_16o\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_16o\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_16o\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_16o\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_16o\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_16o\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_16o\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_16o\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_16o\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_16o\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_16o\_ .node rect,#chatgpt-mermaid-\_r_16o\_ .node circle,#chatgpt-mermaid-\_r_16o\_ .node ellipse,#chatgpt-mermaid-\_r_16o\_ .node polygon,#chatgpt-mermaid-\_r_16o\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_16o\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_16o\_ .node.mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_16o\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_16o\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_16o\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,"Segoe UI","Helvetica","Apple Color Emoji","Arial",sans-serif,"Segoe UI Emoji","Segoe UI Symbol";}Order ServiceSingle PostgreSQLtransactionOrder + Outbox event withsequencePending / claimable eventsRelay workers with leasesKafka topic keyed byaggregate IDConsumer deduplicationConsumer databasetransactionBusiness effect appliedRetry scheduling and failurestateMetrics, logs, and alerts

The major protections work together:

| Concern                                 | Protection                                                   |
| --------------------------------------- | ------------------------------------------------------------ |
| Business change without an event record | Shared database transaction                                  |
| Multiple relays selecting the same rows | Atomic claiming and row locking                              |
| Relay crashes                           | Leases and expired-claim recovery                            |
| Stale workers updating current state    | Claim-token validation                                       |
| Duplicate publication                   | Idempotent consumers                                         |
| Incorrect event order                   | Aggregate versioning and ordered relay eligibility           |
| Persistent failures                     | Backoff, explicit failed state, alerts, and replay           |
| Growing storage                         | Retention, batching, and potentially partitioning            |
| High polling load                       | Query optimization, controlled concurrency, and possibly CDC |

No single mechanism provides all these guarantees. Reliability comes from combining the right mechanisms at the relevant system boundaries.

## 11. Common design mistakes

Holding a database transaction open during Kafka network calls. This holds locks and database resources unnecessarily. Claim work in a short transaction, publish outside it, and record the result afterward.

Assuming `SKIP LOCKED` prevents all duplicates. It coordinates concurrent database claims, but an expired lease, an ambiguous send result, or a crash can still lead to duplicate publication.

Using a lease without handling expiration correctly. The old worker may still be running when the new worker claims the event. Validate claim ownership on database updates and retain consumer idempotency.

Using the Kafka key as the only ordering safeguard. It provides partition affinity, but does not prevent concurrent relays from publishing later events first.

Deleting old records solely by age. This can permanently discard pending work or information needed for recovery.

Adding infrastructure before measuring the problem. A carefully implemented polling relay may be sufficient for a long time. CDC and partitioning are options for particular operating requirements, not mandatory components of every outbox implementation.

## 12. Chapter 4 interview questions

### Q1. Why use `FOR UPDATE SKIP LOCKED`?

It lets concurrent PostgreSQL workers select different unlocked rows instead of waiting for one another. Combined with an atomic state update in a short transaction, it is useful for distributing queued work among relay instances.

### Q2. Why use a lease instead of relying only on row locks?

A row lock disappears when its database transaction ends. A persisted lease records temporary ownership across transactions and lets other workers reclaim abandoned work after the lease expires.

### Q3. Can a claim token eliminate duplicate Kafka messages?

No. It prevents an old worker from overwriting the database state associated with a newer claim. It does not automatically stop that old worker from publishing to Kafka. Consumers must still tolerate duplicates.

### Q4. How do you preserve per-order event ordering?

Assign a reliable aggregate sequence or version, prevent later unpublished events from being claimed ahead of earlier ones, and use the aggregate ID as the Kafka key. The relay must publish in sequence because Kafka preserves append order, not intended business order.

### Q5. How can a failed event block unrelated events?

A strict ordering rule may block later events for the same aggregate. A well-designed scheduler lets other aggregates progress while the affected aggregate waits for retry or intervention. This trade-off must be deliberate.

### Q6. When should you move from polling to CDC?

Consider CDC when measured database polling overhead or publication-latency requirements justify the extra operational infrastructure. Evaluate the full cost, including connector management, recovery, WAL retention, and failure handling.

## Chapter 4 — Final takeaways

You should now understand how to make an outbox relay safer under concurrency and failure:

1. Use atomic claiming to coordinate multiple workers.
2. Use leases to recover work abandoned by crashed instances.
3. Validate claim ownership so stale workers cannot overwrite newer database state.
4. Treat ordering as an explicit business requirement, not an automatic consequence of SQL sorting or Kafka partition keys.
5. Use backoff and eligibility scheduling to avoid wasting capacity on repeatedly failing messages.
6. Apply an explicit retention policy and scale based on measured database and relay bottlenecks.

## Next: Chapter 5 — System Design and Interview Preparation

We'll bring the course together into an end-to-end production design, walk through an order-processing failure scenario, compare the outbox pattern with distributed transactions and direct publishing, and practice interview questions about delivery guarantees, sagas, Kafka transactions, and idempotency.
